package io.github.kuscher.booklight.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Asks every provider, merges what comes back into one ranked list, and learns from picks.
 *
 * One list, best first (Alfred, Raycast, Spotlight since macOS 26), not groups: Enter always
 * runs the first row. Order = the provider's match score × the kind's weight + what [History]
 * has learned for this exact text. Answers (a sum) go first; the web search is always last.
 */
class SearchEngine(
    private val providers: List<Provider>,
    private val history: History,
    private val clock: () -> Long = System::currentTimeMillis,
    /** A provider that takes longer than this is left out of that keystroke's list. */
    private val budgetMs: Long = 150,
) {
    suspend fun search(q: Query, limit: Int = DEFAULT_LIMIT): List<Result> {
        if (q.isEmpty) return zeroState(limit)
        val now = clock()
        val all = ask { it.query(q) }
        return all.distinctBy { it.id }
            .map { it to rank(it, q.text, now) }
            .sortedWith(compareByDescending<Pair<Result, Double>> { it.second }.thenBy { it.first.title.length }.thenBy { it.first.title })
            .take(limit)
            .map { it.first }
    }

    /** Before anything is typed: what you use most, then whatever providers suggest. */
    private suspend fun zeroState(limit: Int): List<Result> {
        val now = clock()
        val all = ask { it.zeroState() }.distinctBy { it.id }
        return all.map { it to (history.weight(it.id, now) + it.score * 0.01) }
            .filter { it.second > 0 }                // nothing used yet and nothing suggested: an empty panel
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private suspend fun ask(call: suspend (Provider) -> List<Result>): List<Result> = coroutineScope {
        providers.map { p ->
            async { withTimeoutOrNull(budgetMs) { runCatching { call(p) }.getOrElse { emptyList() } } ?: emptyList() }
        }.awaitAll().flatten()
    }

    private fun rank(r: Result, text: String, now: Long): Double {
        val base = r.score * weight(r.kind)
        return when (r.kind) {
            Kind.ANSWER -> 10 + base                 // an answer to what you typed is always first
            Kind.WEB -> if (r.score >= URL_SCORE) base else -1 + base   // "search the web" is the last row; a typed address isn't
            else -> base + if (r.learnable) history.boost(text, r.id, now) else 0.0
        }
    }

    /** Apps are what people open most; settings and commands sit just under an equal app match. */
    private fun weight(k: Kind) = when (k) {
        Kind.APP -> 1.0
        Kind.COMMAND -> 0.92
        Kind.SETTING -> 0.9
        else -> 1.0
    }

    /** Call when the user runs a result, so the same text finds it first next time. */
    fun picked(q: Query, r: Result) {
        if (r.learnable) history.record(q.text, r.id, clock())
    }

    companion object {
        const val DEFAULT_LIMIT = 8
        /** A web result scoring this or more is an address the user typed, ranked like a match. */
        const val URL_SCORE = 0.9
    }
}
