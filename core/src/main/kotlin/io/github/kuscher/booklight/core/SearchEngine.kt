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
 * has learned for this exact text. Answers (a sum) go first; the web search comes after every
 * local row, and suggestions from the search engine, when they arrive, go below it.
 *
 * Inside a scope (the chip in the field) only that scope is asked, its rows keep their own order,
 * and the way out to the web for the keyword and the text together keeps the last place.
 */
class SearchEngine(
    private val providers: List<Provider>,
    private val history: History,
    private val clock: () -> Long = System::currentTimeMillis,
    /** A provider that takes longer than this is left out of that keystroke's list. */
    private val budgetMs: Long = 150,
    /** The scopes there are right now (the user can add keyword searches). */
    private val scopes: () -> List<Scope> = { emptyList() },
    /** What a scope's own row says Enter does ("Type…", "Search…"). */
    private val enterLabel: (Scope) -> String = { "Open" },
    /** The rows that end a scope's list: the web search for the keyword and the text together. */
    private val fallback: (String) -> List<Result> = { emptyList() },
) {
    suspend fun search(q: Query, limit: Int = DEFAULT_LIMIT): List<Result> {
        if (q.scope != null) return inScope(q, limit)
        if (q.isEmpty) return zeroState(limit)
        val now = clock()
        val all = ask { it.query(q) } + scopeRows(q.text)
        val ranked = all.distinctBy { it.id }
            .map { it to rank(it, q.text, now) }
            .sortedWith(compareByDescending<Pair<Result, Double>> { it.second }.thenBy { it.first.title.length }.thenBy { it.first.title })
            .map { it.first }
        // The ways out (search the web, ask Gemini) keep the last rows, however many other rows match.
        val out = ranked.filter(::isFallback).take(MAX_FALLBACKS)
        return ranked.filterNot(::isFallback).take(limit - out.size) + out
    }

    private suspend fun inScope(q: Query, limit: Int): List<Result> {
        val s = scope(q.scope ?: return emptyList()) ?: return emptyList()
        val rows = withTimeoutOrNull(budgetMs) { runCatching { s.rows(q.text) }.getOrElse { emptyList() } } ?: emptyList()
        // An ordinary word that happened to be a keyword ("new york weather") is one row away.
        val out = if (q.isEmpty) emptyList() else fallback("${s.keywords.firstOrNull() ?: s.key} ${q.text}").take(1)
        return (rows.take(limit - out.size) + out).distinctBy { it.id }
    }

    fun scope(key: String): Scope? = scopes().firstOrNull { it.key == key }

    /**
     * The scope and its argument when [text] starts with a keyword and a space: "yt lofi" is
     * YouTube and "lofi", "yt " is YouTube and nothing yet. The keyword alone is ordinary text.
     */
    fun scopeFor(text: String): Pair<Scope, String>? {
        val t = text.trimStart()
        val space = t.indexOf(' ')
        if (space <= 0) return null
        val word = t.substring(0, space)
        val s = scopes().firstOrNull { sc -> sc.keywords.any { it.equals(word, ignoreCase = true) } } ?: return null
        return s to t.substring(space + 1)
    }

    /** Scopes as rows of the ordinary list: found by a keyword or by name, entered with Tab or Enter. */
    private fun scopeRows(text: String): List<Result> = scopes().filter { it.listed }.mapNotNull { s ->
        val t = text.lowercase()
        val score = when {
            s.keywords.any { it.equals(text, ignoreCase = true) } -> 1.0
            t.length >= 2 && s.keywords.any { it.lowercase().startsWith(t) } -> 0.85
            else -> Matcher.score(text, s.name) * 0.8
        }
        if (score <= 0) null else Result(
            id = "scope:${s.key}", provider = "scopes", kind = Kind.SCOPE, title = s.name, subtitle = s.about,
            icon = Icon.Symbol(s.symbol), score = score,
            actions = listOf(Action("enter", enterLabel(s), Effect.EnterScope(s.key), keepOpen = true, symbol = "edit")),
        )
    }

    private fun isFallback(r: Result) = r.kind == Kind.WEB && r.score < URL_SCORE

    /**
     * Adds a search engine's suggestions, which arrive after the local rows are on screen, below
     * everything else. They only take rows that are free, so nothing already shown moves or goes.
     */
    fun merge(local: List<Result>, suggestions: List<Result>, limit: Int = DEFAULT_LIMIT): List<Result> {
        val shown = local.map { Matcher.fold(it.title) }.toSet()
        val fresh = suggestions.filter { Matcher.fold(it.title) !in shown }.distinctBy { Matcher.fold(it.title) }
        return local + fresh.take(minOf(MAX_SUGGESTIONS, limit - local.size).coerceAtLeast(0))
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
        Kind.SCOPE -> 0.97                         // "mail" is the Mail scope unless an app is called exactly that
        Kind.CONTROL -> 0.95
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
        const val MAX_SUGGESTIONS = 3
        const val MAX_FALLBACKS = 2
    }
}
