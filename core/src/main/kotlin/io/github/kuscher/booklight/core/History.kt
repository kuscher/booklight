package io.github.kuscher.booklight.core

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min

/**
 * What Booklight has learned from your picks. Two signals, both from Booklight's own use only
 * (no usage-access permission):
 *
 *  - **Latching** (Alfred's idea): the exact text you typed is tied to what you picked for it.
 *    Pick Calendar for "c" a couple of times and "c" means Calendar, while "ca" can still mean
 *    Calculator.
 *  - **Frecency**: things you open often and recently get a smaller lift for every query.
 *    Counts decay with a four-week half-life, so old habits fade.
 *
 * Safe to call from any thread: searches read it off the main thread while a pick is recorded on it.
 */
class History(data: Data = Data()) {
    /** Plain data for saving; the app writes it as JSON. Times are epoch milliseconds. */
    data class Entry(val count: Double, val last: Long)
    data class Data(
        val items: Map<String, Entry> = emptyMap(),
        /** Typed text → result id → picks. */
        val latches: Map<String, Map<String, Entry>> = emptyMap(),
    )

    private val items = HashMap(data.items)
    private val latches = HashMap<String, HashMap<String, Entry>>().apply { for ((k, v) in data.latches) put(k, HashMap(v)) }

    @Synchronized
    fun data(): Data = Data(HashMap(items), latches.mapValues { HashMap(it.value) })

    @Synchronized
    fun record(query: String, id: String, now: Long) {
        items[id] = bump(items[id], now)
        val q = Matcher.fold(query)
        if (q.isEmpty()) return
        val m = latches.getOrPut(q) { HashMap() }
        m[id] = bump(m[id], now)
        if (latches.size > MAX_LATCHES) prune()
    }

    /** What to add to a result's match score for this query, 0..[LATCH_MAX] + [FRECENCY_MAX]. */
    @Synchronized
    fun boost(query: String, id: String, now: Long): Double {
        val latch = latches[Matcher.fold(query)]?.get(id)?.let { decayed(it, now) } ?: 0.0
        return LATCH_MAX * saturate(latch, 2.0) + FRECENCY_MAX * saturate(weight(id, now), 6.0)
    }

    /** How much an item is used, decayed to now: for ordering the empty-query suggestions. */
    @Synchronized
    fun weight(id: String, now: Long): Double = items[id]?.let { decayed(it, now) } ?: 0.0

    /** The most-used ids, best first. */
    @Synchronized
    fun top(n: Int, now: Long): List<String> =
        items.entries.map { it.key to decayed(it.value, now) }.filter { it.second > 0.05 }
            .sortedByDescending { it.second }.take(n).map { it.first }

    @Synchronized
    fun forget(id: String) {
        items.remove(id)
        for (m in latches.values) m.remove(id)
        latches.values.removeAll { it.isEmpty() }
    }

    @Synchronized
    fun clear() { items.clear(); latches.clear() }

    private fun bump(e: Entry?, now: Long) = Entry((e?.let { decayed(it, now) } ?: 0.0) + 1.0, now)

    private fun decayed(e: Entry, now: Long): Double {
        val age = (now - e.last).coerceAtLeast(0)
        return e.count * exp(-LN2 * age / HALF_LIFE_MS)
    }

    /** 0 at 0, about 0.63 at [scale], towards 1 beyond: the first picks matter most. */
    private fun saturate(x: Double, scale: Double) = min(1.0, 1 - exp(-x / scale))

    private fun prune() {
        val keep = latches.entries.sortedByDescending { e -> e.value.values.maxOf { it.last } }.take(MAX_LATCHES * 3 / 4)
        latches.clear()
        for (e in keep) latches[e.key] = e.value
    }

    companion object {
        /** Enough to lift a weak match (0.45) over an exact one (1.0) once latched. */
        const val LATCH_MAX = 0.7
        const val FRECENCY_MAX = 0.15
        const val HALF_LIFE_MS = 28.0 * 24 * 60 * 60 * 1000
        private const val MAX_LATCHES = 4000
        private val LN2 = ln(2.0)
    }
}
