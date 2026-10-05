package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunOpeningsTest {
    /** A run left half-way: the key and the first lesson are over. */
    private val halfWay = State(Run.NEW, done = listOf("show", "key", "open"), key = true)

    @Test fun aRunLeftHalfWayStandsOnTheSameScreenAtTheNextOpening() {
        var s = halfWay
        repeat(FirstRun.MAX_OPENS) {
            s = FirstRun.opened(s)
            assertEquals(Screen.L3, FirstRun.screen(s))
        }
        assertEquals(FirstRun.MAX_OPENS, s.opens)
    }

    @Test fun afterThreeOpeningsItWaitsAndNothingComes() {
        var s = halfWay
        repeat(FirstRun.MAX_OPENS + 1) { s = FirstRun.opened(s) }
        assertTrue(FirstRun.parked(s))
        assertNull(FirstRun.screen(s))
        // It is counted no further, and what was done is kept.
        assertEquals(s, FirstRun.opened(s))
        assertEquals(halfWay.done, s.done)
    }

    @Test fun whereThereIsNoRunNothingIsCounted() {
        assertEquals(State(), FirstRun.opened(State()))
        assertEquals(State(key = true), FirstRun.resume(State(key = true)))
    }

    @Test fun askedForAgainItGoesOnWhereItStopped() {
        var s = halfWay
        repeat(FirstRun.MAX_OPENS + 1) { s = FirstRun.opened(s) }
        assertEquals(Screen.L3, FirstRun.screen(FirstRun.resume(s)))
        assertEquals(Screen.L3, FirstRun.screen(FirstRun.again(s, overture = true)))
        assertEquals(halfWay.done, FirstRun.again(s, overture = true).done)
    }

    /** A screen the run has stood on in an earlier opening comes back: its parts arrive as rows do, and nothing of it is written again. */
    @Test fun inALaterOpeningAScreenComesBack() {
        var s = FirstRun.opening(halfWay, 1200f, plain = true)
        assertFalse(FirstRun.comesBack(s))
        s = FirstRun.opening(s, 1200f, plain = true)
        assertTrue(FirstRun.comesBack(s))
        // Asked for again, the run's next opening is its first once more; so is the one a first key makes.
        assertFalse(FirstRun.comesBack(FirstRun.opening(FirstRun.again(s, overture = true), 1200f, plain = true)))
        val waiting = State(Run.NEW, helper = 1, opens = FirstRun.MAX_OPENS)
        assertFalse(FirstRun.comesBack(FirstRun.opening(FirstRun.keyLanded(waiting), 1200f, plain = true)))
        // The panel the command is typed in is the run's first opening: only the next one comes back.
        val here = FirstRun.againHere(State(key = true))
        assertFalse(FirstRun.comesBack(here))
        assertTrue(FirstRun.comesBack(FirstRun.opening(here, 1200f, plain = true)))
        // Nothing comes back where nothing stands.
        assertFalse(FirstRun.comesBack(State()))
        assertFalse(FirstRun.comesBack(State(Run.NEW, opens = FirstRun.MAX_OPENS + 1)))
    }

    @Test fun theUsersOwnKeyBringsARunBackThatWaitedForIt() {
        val waiting = State(Run.NEW, helper = 1, opens = FirstRun.MAX_OPENS + 1)
        assertNull(FirstRun.screen(waiting))
        assertEquals(Screen.K4, FirstRun.screen(FirstRun.keyLanded(waiting)))
    }

    @Test fun aReplayTouchesNoSwitchAndKeepsTheKey() {
        val s = State(icon = true, key = true, suggestions = true, mark = true)
        val r = FirstRun.replay(s, overture = false)
        assertEquals(State(Run.REPLAY, done = listOf(FirstRun.SHOWN), icon = true, key = true, suggestions = true, mark = true), r)
        assertEquals(Screen.K4, FirstRun.screen(r))
        assertEquals(emptyList<String>(), FirstRun.replay(s, overture = true).done)
        assertEquals(Screen.K1, FirstRun.screen(FirstRun.replay(State(), overture = false)))
    }

    @Test fun firstStepsAskedForWithNoRunPlaysItAgain() {
        assertEquals(Run.REPLAY, FirstRun.again(State(key = true), overture = false).run)
    }

    @Test fun everyScreenCanBeReachedByName() {
        val s = State(key = true, mark = true, icon = true)
        for (screen in Screen.entries) {
            val to = FirstRun.at(s, screen)!!
            assertEquals(screen, FirstRun.screen(to))
            assertEquals("$screen", listOf(true, false, true, true), listOf(to.key, to.suggestions, to.mark, to.icon))
        }
    }

    @Test fun aScreenThatCannotStandIsNotReached() {
        assertNull(FirstRun.at(State(), Screen.K4))
        assertNull(FirstRun.at(State(key = true, suggestions = true), Screen.Q))
        assertEquals(Screen.K2, FirstRun.screen(FirstRun.at(State(), Screen.K2)!!))
        assertFalse(FirstRun.at(State(), Screen.K1)!!.key)
        // A run that is under way keeps its kind.
        assertEquals(Run.NEW, FirstRun.at(State(Run.NEW), Screen.L3)!!.run)
        assertEquals(Run.REPLAY, FirstRun.at(State(Run.UPDATE), Screen.L3)!!.run)
    }
}
