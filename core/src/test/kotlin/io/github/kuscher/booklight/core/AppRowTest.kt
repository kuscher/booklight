package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** An app's row: Open · Search · Play · Window · the arrow, and what stands behind the last two. */
class AppRowTest {
    private fun a(id: String, danger: Boolean = false) = Action(id, id, Effect.Internal(id), danger = danger)

    /** What the app list makes for an app, in the order it happens to make it. */
    private fun made(search: Boolean = false, play: Boolean = false, own: Boolean = true, system: Boolean = false, pages: Boolean = true): List<Action> = listOfNotNull(
        a("uninstall", danger = true).takeIf { !system },
        a("info"), a("open"),
        a("play").takeIf { play },
        a("window").takeIf { own },
    ) + AppRow.PLACES.reversed().map { a(it) } + (if (pages) AppRow.PAGES.map { a(it) } else emptyList()) + listOfNotNull(a("search").takeIf { search }, a(AppRow.WINDOW))

    private fun icons(row: List<Action>) = row.filter { !it.more }.map { it.id }
    private fun lines(row: List<Action>, behind: Behind) = row.filter { it.more && it.behind == behind }.map { it.id }

    @Test fun anAppShowsOnlyWhatItHasInOneOrder() {
        assertEquals(listOf("open", "places"), icons(AppRow.arrange(made())))                                           // a calculator
        assertEquals(listOf("open", "search", "places"), icons(AppRow.arrange(made(search = true))))                    // Netflix
        assertEquals(listOf("open", "search", "play", "places"), icons(AppRow.arrange(made(search = true, play = true))))   // Spotify, with a key
        assertEquals(listOf("open", "play", "places"), icons(AppRow.arrange(made(play = true))))                        // a player nobody can search
    }

    @Test fun windowHoldsNewWindowAndThePlaces() {
        val row = AppRow.arrange(made(search = true))
        assertEquals(listOf("window", "full", "left", "right", "p3l", "p3m", "p3r", "p23l", "p23r", "ptl", "ptr", "pbl", "pbr", "pc"), lines(row, Behind.WINDOW))
        assertEquals(14, lines(row, Behind.WINDOW).size)
        // An app of another profile cannot be given a second window from here: the places are all of the list.
        assertEquals(AppRow.PLACES, lines(AppRow.arrange(made(own = false)), Behind.WINDOW))
    }

    @Test fun theArrowHoldsTheRestAndWhatRemovesIsLast() {
        assertEquals(listOf("info", "notifications", "language", "defaults", "battery", "uninstall"), lines(AppRow.arrange(made(play = true)), Behind.ARROW))
        // An app that came with the device has no Uninstall; the Settings app has no pages of its own.
        assertEquals(listOf("info", "notifications", "language", "defaults", "battery"), lines(AppRow.arrange(made(system = true)), Behind.ARROW))
        assertEquals(listOf("info", "uninstall"), lines(AppRow.arrange(made(pages = false)), Behind.ARROW))
        assertTrue(AppRow.arrange(made()).last().danger)
    }

    @Test fun theRowIsItsIconsThenWindowsLinesThenTheArrows() {
        val row = AppRow.arrange(made(search = true, play = true))
        assertEquals(listOf("open", "search", "play", "places") + AppRow.WINDOW_LINES + AppRow.ARROW_LINES, row.map { it.id })
        assertEquals(0, row.indexOfFirst { it.id == "open" })
    }

    @Test fun whichListALineBelongsTo() {
        for (id in AppRow.ICONS) assertNull(id, AppRow.behind(id))
        for (id in AppRow.WINDOW_LINES) assertEquals(id, Behind.WINDOW, AppRow.behind(id))
        for (id in AppRow.ARROW_LINES + "unsuggest") assertEquals(id, Behind.ARROW, AppRow.behind(id))
    }

    @Test fun windowWithNothingBehindItIsNotAStop() {
        assertEquals(listOf("open"), icons(AppRow.arrange(listOf(a("open"), a(AppRow.WINDOW), a("info")))))
    }

    @Test fun anActionTheRowDoesNotKnowIsALineBehindTheArrowBeforeWhatRemoves() {
        val row = AppRow.arrange(made() + a("share"))
        assertEquals(listOf("info", "notifications", "language", "defaults", "battery", "share", "uninstall"), lines(row, Behind.ARROW))
    }

    @Test fun dontSuggestStandsBeforeUninstallBehindTheArrow() {
        val row = Result("app:x", "apps", Kind.APP, "X", icon = Icon.Symbol("app"), score = 1.0, actions = AppRow.arrange(made(search = true)))
        val offered = Zero.offer(row, "Don't suggest")
        assertEquals(listOf("info", "notifications", "language", "defaults", "battery", "unsuggest", "uninstall"), lines(offered.actions, Behind.ARROW))
        assertEquals(AppRow.WINDOW_LINES, lines(offered.actions, Behind.WINDOW))
    }

    // ---- the chip's two actions

    @Test fun aChipIsEnteredWithWhatWasArmedIfTheAppHasIt() {
        val both = listOf(Act.SEARCH, Act.PLAY)
        assertEquals(Act.PLAY, Chips.enter(both, Act.PLAY))
        assertEquals(Act.SEARCH, Chips.enter(both, Act.SEARCH))
        assertEquals(Act.SEARCH, Chips.enter(both, null))
        // `play` with Spotify and no key: the app only searches, and its chip says so.
        assertEquals(Act.SEARCH, Chips.enter(listOf(Act.SEARCH), Act.PLAY))
        assertEquals(Act.PLAY, Chips.enter(listOf(Act.PLAY), null))
        assertNull(Chips.enter(emptyList(), Act.SEARCH))
    }

    @Test fun tabChangesToTheOtherActionWhereThereIsOne() {
        val both = listOf(Act.SEARCH, Act.PLAY)
        assertEquals(Act.PLAY, Chips.other(both, Act.SEARCH))
        assertEquals(Act.SEARCH, Chips.other(both, Act.PLAY))
        assertNull(Chips.other(listOf(Act.SEARCH), Act.SEARCH))
        assertNull(Chips.other(emptyList(), null))
    }

    @Test fun anActionIsKnownByItsId() {
        assertEquals(Act.SEARCH, Act.of("search"))
        assertEquals(Act.PLAY, Act.of("play"))
        assertNull(Act.of("open"))
        assertNull(Act.of(null))
    }

    // ---- what Backspace restores

    private fun row(id: String, armed: Int = 0, vararg actions: Action) = Result(id, "apps", Kind.APP, id, icon = Icon.Symbol("app"), score = 1.0, actions = actions.toList(), armed = armed)

    @Test fun backspaceGoesBackToTheRowAndTheActionEnterWasPressedOn() {
        val rows = listOf(row("app:chrome", 0, a("open"), a("places")), row("app:spotify", 0, a("open"), a("search"), a("play")))
        assertEquals(1 to 2, Origin("spo", "app:spotify", "play").place(rows))
        assertEquals(1 to 1, Origin("spo", "app:spotify", "search").place(rows))
    }

    @Test fun aRowThatIsGoneLeavesRowOneWithItsOwnDefault() {
        val rows = listOf(row("app:chrome", 1, a("open"), a("places")))
        assertEquals(0 to 1, Origin("spo", "app:spotify", "play").place(rows))
        assertEquals(-1 to 0, Origin("spo", "app:spotify", "play").place(emptyList()))
        // Among the usual rows nothing is selected at rest.
        assertEquals(-1 to 0, Origin("", "app:spotify", "play", usual = true).place(rows))
    }

    @Test fun anActionThatIsGoneOrCannotBeRestedOnLeavesTheRowsOwnDefault() {
        // The key was taken out meanwhile: Spotify's row has no Play.
        val rows = listOf(row("app:spotify", 0, a("open"), a("search")))
        assertEquals(0 to 0, Origin("spo", "app:spotify", "play").place(rows))
        val off = listOf(row("app:spotify", 0, a("open"), a("search"), a("play")))
        assertEquals(0 to 0, Origin("spo", "app:spotify", "play").place(off) { listOf(0, 1) })
        assertFalse(Origin("spo", "app:spotify", "play").usual)
    }

    @Test fun oneOfTheUsualRowsIsGoneBackTo() {
        val rows = listOf(row("app:chrome", 0, a("open")), row("app:spotify", 0, a("open"), a("search")))
        assertEquals(1 to 1, Origin("", "app:spotify", "search", usual = true).place(rows))
    }
}
