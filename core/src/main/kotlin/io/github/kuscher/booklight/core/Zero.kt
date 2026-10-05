package io.github.kuscher.booklight.core

/**
 * The rows under the empty field, "your usual": the two things run most from Booklight, and the one
 * run last. Two or three, never one. Worked out from [History] alone (what was run from Booklight,
 * how often and when), so nothing here needs a permission.
 *
 *  - **Earned**: a thing's faded count is [EARNED] or more. Run twice, it is earned for eleven
 *    days; run three times, for four weeks.
 *  - **Seats one and two**: the two most earned; both must be filled, or there are no rows. What
 *    sat there at the last showing keeps its seat until something else weighs [HOLD] times as
 *    much: two things run every day must not swap seats every day, or "Down, Enter" would mean
 *    something else each time.
 *  - **Seat three**: of the rest, the one run last, if that was within [RECENT_MS] (once is
 *    enough); else the third most earned; else no third row.
 */
object Zero {
    enum class Why { OFTEN, RECENT }
    data class Pick(val id: String, val why: Why, val weight: Double, val last: Long)
    /** [held]: what to keep as the holders of seats one and two; null = keep what was kept (no rows, or a holder was only away for now). */
    data class Seats(val picks: List<Pick>, val held: List<String>?)

    const val MIN = 2
    const val MAX = 3
    const val EARNED = 1.5
    const val RECENT_MS = 8L * 60 * 60 * 1000
    /** A run this far ahead of the clock still counts as now; further ahead, the clock was set back, and it is not "recent". */
    const val AHEAD_MS = 5_000L
    const val HOLD = 1.2

    /** What can be suggested, by the start of its id: things that are opened. Not typed text, sums, notes, controls, keywords or answers. */
    val KINDS = listOf("app:", "setting:", "command:", "cmd:", "link:", "recipe:")

    /**
     * [items]: the history. [live]: whether a thing still exists and may be shown (asked only for
     * candidates). [hidden]: what the user said "Don't suggest" for. [held]: seats one and two of
     * the last showing, in order.
     */
    fun pick(
        items: Map<String, History.Entry>,
        now: Long,
        live: (String) -> Boolean,
        hidden: Set<String> = emptySet(),
        held: List<String> = emptyList(),
    ): Seats {
        // Heaviest first; equal weights: the one run later, then by id, so the order is the same on every call.
        val order = compareByDescending<Pick> { it.weight }.thenByDescending { it.last }.thenBy { it.id }
        val all = items.asSequence()
            .filter { (id, _) -> id !in hidden && KINDS.any(id::startsWith) }
            .map { (id, e) -> Pick(id, Why.OFTEN, History.faded(e, now), e.last) }
            .sortedWith(order)
            .toList()
        val alive = HashMap<String, Boolean>()
        fun ok(p: Pick) = alive.getOrPut(p.id) { live(p.id) }
        val was = held.distinct().take(2)
        // A holder that is earned and not hidden, but not there just now (its app is being updated, a list is still
        // being read): it has no row this time, and its seat is not given away for good.
        val away = was.any { id -> all.any { it.id == id && it.weight >= EARNED && !ok(it) } }
        val earned = all.filter { it.weight >= EARNED && ok(it) }
        if (earned.size < MIN) return Seats(emptyList(), null)

        // Seats one and two: whoever held one keeps it, in the order they sat, unless a newcomer weighs HOLD times as much.
        // The newcomer stands where the one it pushed out stood.
        val seats = was.mapNotNull { id -> earned.firstOrNull { it.id == id } }.toMutableList()
        for (c in earned) {
            if (seats.any { it.id == c.id }) continue
            if (seats.size < 2) { seats += c; continue }
            val weakest = seats.minWithOrNull(compareBy<Pick> { it.weight }.thenBy { it.last })!!
            if (c.weight >= HOLD * weakest.weight) seats[seats.indexOf(weakest)] = c
        }
        // No holder left among the two: heaviest first. Otherwise they swap only when the second weighs HOLD times the first.
        if (seats.none { it.id in was }) seats.sortWith(order)
        else if (seats[1].weight >= HOLD * seats[0].weight) seats.reverse()

        val rest = all.filter { c -> seats.none { it.id == c.id } }
        val recent = rest.filter { now - it.last in -AHEAD_MS..RECENT_MS }.sortedWith(compareByDescending<Pick> { it.last }.thenBy { it.id }).firstOrNull { ok(it) }
        val third = recent?.copy(why = Why.RECENT) ?: rest.firstOrNull { it.weight >= EARNED && ok(it) }
        return Seats((seats + listOfNotNull(third)).take(MAX), if (away) null else seats.map { it.id })
    }

    /**
     * [row] with "Don't suggest" among its actions, in its place: before the first action that
     * removes, else last; behind the arrow where the row keeps actions there. So every other action
     * keeps the place it has on any other row (arrow, Enter, Enter is still "Left third"), and the
     * way down to it never passes one that removes. Never armed by this: an arming that pointed at
     * or past that place moves along. A row that has it already is returned as it is.
     */
    fun offer(row: Result, label: String): Result {
        if (row.actions.any { it.effect is Effect.Unsuggest }) return row
        val at = row.actions.indexOfFirst { it.danger }.takeIf { it >= 0 } ?: row.actions.size
        val behind = row.actions.any { it.more }
        val action = Action("unsuggest", label, Effect.Unsuggest(row.id), keepOpen = true, symbol = "hide", more = behind)
        return row.copy(actions = row.actions.subList(0, at) + action + row.actions.subList(at, row.actions.size), armed = if (row.armed >= at) row.armed + 1 else row.armed)
    }
}

/**
 * What stands under the untouched empty field. At most one thing, chosen in one order: first run's
 * stage (the screen of first run that is due, [FirstRun.stage]), the line for a fresh copy, the
 * usual rows, a tip. The stage comes at once; the others wait for the last moment ([last]: a
 * moment after the panel has opened), except that the usual rows may come before it once it is
 * known that no copy is fresh.
 */
object Under {
    enum class What { STAGE, COPY, USUAL, TIP, NOTHING, WAIT }

    /**
     * [untouched]: nothing typed, no chip, no rows, nothing under the field yet. [stage]: a screen of
     * first run is due. [copy]: a copy is fresh, or null while the system has not said. [zero]: the
     * switch for the usual rows. [seats]: how many of them there are, or null while they are not
     * worked out. [tip]: tips are on and one is left. At the [last] moment nothing waits any longer,
     * and "not known" counts as no.
     */
    fun choose(untouched: Boolean, stage: Boolean, copy: Boolean?, zero: Boolean, seats: Int?, tip: Boolean, last: Boolean): What = when {
        !untouched -> What.NOTHING
        stage -> What.STAGE
        copy == true -> if (last) What.COPY else What.WAIT
        zero && (seats ?: 0) >= Zero.MIN && (copy == false || last) -> What.USUAL
        !last -> What.WAIT
        tip -> What.TIP
        else -> What.NOTHING
    }
}
