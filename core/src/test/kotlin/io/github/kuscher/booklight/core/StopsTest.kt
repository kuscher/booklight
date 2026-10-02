package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What the arming can rest on: the icons, Window and the arrow, and where a typed line stands. */
class StopsTest {
    private fun a(id: String, danger: Boolean = false, off: Boolean = false) =
        Action(id, id, if (id == AppRow.WINDOW) Effect.OpenList(Behind.WINDOW) else Effect.Internal(id), danger = danger, off = off)

    /** An app's row as the app list makes it: Open · Search · Play · Window · the arrow. */
    private fun app(vararg typed: String, search: Boolean = true, play: Boolean = true): Result {
        val actions = AppRow.arrange(listOfNotNull(a("open"), a("search").takeIf { search }, a("play").takeIf { play }, a(AppRow.WINDOW), a("window")) +
            AppRow.PLACES.map { a(it) } + a("info") + AppRow.PAGES.map { a(it) } + a("uninstall", danger = true))
        return Result("app:x", "apps", Kind.APP, "X", icon = Icon.Symbol("app"), score = 1.0, actions = actions, armed = typed.firstOrNull()?.let { t -> actions.indexOfFirst { it.id == t } } ?: 0)
    }
    private fun ids(r: Result, stops: List<Int> = Stops.of(r)) = stops.map { r.actions.getOrNull(it)?.id ?: "more" }

    @Test fun anAppsRowStopsOnItsIconsThenWindowThenTheArrow() {
        assertEquals(listOf("open", "search", "play", "places", "more"), ids(app()))
        assertEquals(listOf("open", "places", "more"), ids(app(search = false, play = false)))
        assertEquals(listOf("open", "search", "places", "more"), ids(app(play = false)))
    }

    @Test fun aTypedPlaceStandsInWindowsSlot() {
        assertEquals(listOf("open", "search", "play", "left", "more"), ids(app("left")))
        assertEquals(listOf("open", "search", "play", "ptl", "more"), ids(app("ptl")))
        assertEquals(listOf("open", "window", "more"), ids(app("window", search = false, play = false)))     // "chr new"
    }

    @Test fun aTypedPageOrUninstallStandsInTheArrowsSlot() {
        assertEquals(listOf("open", "search", "play", "places", "notifications"), ids(app("notifications")))
        assertEquals(listOf("open", "search", "play", "places", "uninstall"), ids(app("uninstall")))
        assertEquals(listOf("open", "search", "play", "places", "info"), ids(app("info")))
    }

    @Test fun withAListOpenEveryLineIsInItsList() {
        val r = app("left")
        assertEquals(listOf("open", "search", "play", "places", "more"), ids(r, Stops.of(r, open = true)))
    }

    @Test fun anActionThatIsOffIsPassedOver() {
        val r = Result("appsearch:x", "play", Kind.OTHER, "Play", icon = Icon.Symbol("app"), score = 1.0, actions = listOf(a("search"), a("play", off = true), a("which").copy(more = true)))
        assertEquals(listOf("search", "more"), ids(r))
    }

    @Test fun aRowWithNothingBehindAnArrowStopsOnItsActionsOnly() {
        val r = Result("web:search", "web", Kind.WEB, "Search", icon = Icon.Symbol("search"), score = 0.1, actions = listOf(a("search"), a("link")))
        assertEquals(listOf(0, 1), Stops.of(r))
        assertNull(Stops.list(r, 2))
    }

    @Test fun whichListAStopOpens() {
        val r = app()
        assertEquals(Behind.ARROW, Stops.list(r, r.actions.size))
        assertEquals(Behind.WINDOW, Stops.list(r, r.actions.indexOfFirst { it.id == AppRow.WINDOW }))
        assertNull(Stops.list(r, 0))
        assertNull(Stops.list(r, r.actions.indexOfFirst { it.id == "search" }))
        // A typed place in Window's slot runs: it opens nothing.
        val typed = app("left")
        assertNull(Stops.list(typed, typed.armed))
    }

    @Test fun whereTheArmingRestsForAList() {
        val r = app()
        assertEquals(r.actions.size, Stops.at(r, Behind.ARROW))
        assertEquals(r.actions.indexOfFirst { it.id == AppRow.WINDOW }, Stops.at(r, Behind.WINDOW))
        assertEquals(r.actions.size, Stops.at(r, null))
        // Both are stops of the row, so closing a list always lands on one.
        assertEquals(true, Stops.at(r, Behind.WINDOW) in Stops.of(r, open = true))
        assertEquals(true, Stops.at(r, Behind.ARROW) in Stops.of(r, open = true))
    }
}
