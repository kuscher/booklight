package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Hold
import io.github.kuscher.booklight.core.FirstRun.Held
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Signal
import io.github.kuscher.booklight.core.FirstRun.Start
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunHoldTest {
    /** The signals in their order: the hold at the end, whether the panel was closed on the way, and how often the dialog came. */
    private fun after(vararg signals: Signal): Triple<Hold, Boolean, Int> {
        var hold = Hold.NONE; var closed = false; var came = 0
        for (s in signals) { val h = FirstRun.hold(hold, s); hold = h.hold; closed = closed || h.close; if (h.came) came++ }
        return Triple(hold, closed, came)
    }

    @Test fun withoutAHoldALostFocusClosesThePanelAsEveryDay() {
        assertEquals(Held(Hold.NONE, close = true), FirstRun.hold(Hold.NONE, Signal.FOCUS_LOST))
        for (s in listOf(Signal.FOCUS_BACK, Signal.KEY, Signal.GONE, Signal.NO_DIALOG)) assertEquals(Held(Hold.NONE), FirstRun.hold(Hold.NONE, s))
    }

    /** As the device said it: under the dialog the panel is told one thing, that it lost the focus. */
    @Test fun theDialogComesAndThePanelStays() {
        assertEquals(Triple(Hold.UNDER, false, 1), after(Signal.ASK, Signal.FOCUS_LOST))
        // The second that was allowed for it to come has passed: nothing changes.
        assertEquals(Triple(Hold.UNDER, false, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.NO_DIALOG))
    }

    @Test fun theDialogIsClosedByHandAndTheHoldIsOver() {
        assertEquals(Triple(Hold.NONE, false, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.FOCUS_BACK))
        // From here a lost focus closes, as every day.
        assertEquals(Triple(Hold.NONE, true, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.FOCUS_BACK, Signal.FOCUS_LOST))
    }

    /** The key lands while the dialog is open: its start comes first, the focus 45 ms later. The panel never closes. */
    @Test fun theKeyLandsUnderTheDialog() {
        assertEquals(Triple(Hold.UNDER, false, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.KEY))
        assertEquals(Triple(Hold.NONE, false, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.KEY, Signal.FOCUS_BACK))
    }

    /** A click on another window, another app's shortcut: the front is gone for good, and so is the panel. */
    @Test fun anotherWindowTakesTheFrontForGood() {
        assertEquals(Triple(Hold.NONE, true, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.GONE))
        assertEquals(Triple(Hold.NONE, true, 0), after(Signal.ASK, Signal.GONE))
    }

    @Test fun theSystemShowsNoDialog() {
        assertEquals(Triple(Hold.NONE, false, 0), after(Signal.ASK, Signal.NO_DIALOG))
        assertEquals(Triple(Hold.NONE, true, 0), after(Signal.ASK, Signal.NO_DIALOG, Signal.FOCUS_LOST))
    }

    @Test fun askedTwiceItIsCountedOnce() {
        assertEquals(Triple(Hold.UNDER, false, 1), after(Signal.ASK, Signal.ASK, Signal.FOCUS_LOST, Signal.FOCUS_LOST))
    }

    private val home = "com.example.home"
    /** A keyboard shortcut's start, as it was seen on a device: the launcher activity, sent by the system, nothing else. */
    private val key = Start(launcher = true, referrer = FirstRun.SYSTEM, home = home)

    @Test fun aStartByTheSystemItselfIsTheKey() {
        assertTrue(FirstRun.byKey(key))
        assertFalse(FirstRun.fromIcon(key))
        assertTrue(FirstRun.byKey(Start(launcher = false, assist = true)))
        // Nobody is named: as before, a key.
        assertTrue(FirstRun.byKey(Start(launcher = true, home = home)))
    }

    @Test fun aClickOnTheIconIsNoKey() {
        val withBounds = key.copy(bounds = true)
        val fromHome = Start(launcher = true, referrer = home, home = home)
        for (s in listOf(withBounds, fromHome)) { assertTrue(FirstRun.fromIcon(s)); assertFalse(FirstRun.byKey(s)) }
    }

    @Test fun openInTheStoreAndAnotherAppsStartAreNoKey() {
        for (who in listOf("com.android.vending", "com.android.settings", "com.android.shell", "com.example.other"))
            assertFalse(who, FirstRun.byKey(Start(launcher = true, referrer = who, home = home)))
        // A start that carries something, or does not ask for the launcher activity (the widget, the tile, text handed over).
        assertFalse(FirstRun.byKey(key.copy(extras = true)))
        assertFalse(FirstRun.byKey(Start(launcher = false, referrer = FirstRun.SYSTEM)))
        assertFalse(FirstRun.fromIcon(Start(launcher = false, bounds = true)))
    }

    @Test fun theIconsFirstClickShowsThePanelOnce() {
        val fresh = State(Run.NEW)
        assertTrue(FirstRun.iconShowsPanel(fresh, 1200f))
        assertTrue(FirstRun.iconShowsPanel(fresh.copy(helper = 2), 1200f))
        assertTrue(FirstRun.iconShowsPanel(fresh.copy(opens = FirstRun.MAX_OPENS - 1), 1200f))        // its panel is the third opening
        assertFalse(FirstRun.iconShowsPanel(FirstRun.iconClicked(fresh), 1200f))
    }

    @Test fun everyOtherClickOnTheIconOpensTheWindow() {
        assertFalse(FirstRun.iconShowsPanel(State(), 1200f))                              // no run
        assertFalse(FirstRun.iconShowsPanel(State(Run.UPDATE), 1200f))                    // an installation that was there before knows the window
        assertFalse(FirstRun.iconShowsPanel(State(Run.NEW, key = true), 1200f))           // the key works: nothing to ask for
        // Asked of the state as this opening leaves it: a click that is the fourth opening would show an empty field.
        assertFalse(FirstRun.iconShowsPanel(State(Run.NEW, opens = FirstRun.MAX_OPENS), 1200f))
        assertFalse(FirstRun.iconShowsPanel(State(Run.NEW, opens = FirstRun.MAX_OPENS + 1), 1200f))   // the run waits
        assertFalse(FirstRun.iconShowsPanel(State(Run.NEW), 474f))                        // a screen too low for first run
    }
}
