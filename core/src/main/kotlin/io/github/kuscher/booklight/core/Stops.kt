package io.github.kuscher.booklight.core

/**
 * What the arming can rest on, on a row. A row shows some of its actions as icons and keeps the
 * others as lines of a list: behind its arrow, and on an app's row also behind Window. Tab and the
 * arrows go along the stops; Enter on a stop that opens a list opens it. The places are indices into
 * the row's actions; the arrow, which is no action, is the number of actions.
 */
object Stops {
    /**
     * The stops of [r] in the order they are drawn: the actions shown as icons (one that is off is
     * passed over), then the arrow if the row keeps lines behind it. A typed verb that named a line
     * ([Result.armed]) stands in that list's stop while the list is closed: a place in Window's
     * ("chrome top left"), a page or Uninstall in the arrow's ("chrome uninstall"). [open]: one of
     * the row's lists is open under it, and every line is in its list.
     */
    fun of(r: Result, open: Boolean = false): List<Int> {
        val typed = r.armed.takeIf { r.actions.getOrNull(it)?.more == true && !open }
        val place = typed?.takeIf { r.actions[it].behind == Behind.WINDOW }
        val shown = r.actions.indices.filter { !r.actions[it].more && !r.actions[it].off }.map { if (place != null && r.actions[it].effect is Effect.OpenList) place else it }
        if (r.actions.none { it.more && it.behind == Behind.ARROW }) return shown
        return shown + (typed?.takeIf { place == null } ?: r.actions.size)
    }

    /** The list the stop at [armed] opens: the arrow's, or the one of a stop made for it (Window). Null for a stop that runs something. */
    fun list(r: Result, armed: Int): Behind? {
        if (armed == r.actions.size) return Behind.ARROW.takeIf { r.actions.any { it.more && it.behind == Behind.ARROW } }
        return (r.actions.getOrNull(armed)?.effect as? Effect.OpenList)?.behind
    }

    /** Where the arming rests on [r] for the list [behind]: the arrow (the number of actions), or the stop that opens that list. */
    fun at(r: Result, behind: Behind?): Int =
        if (behind == null || behind == Behind.ARROW) r.actions.size else r.actions.indexOfFirst { (it.effect as? Effect.OpenList)?.behind == behind }.takeIf { it >= 0 } ?: r.actions.size
}
