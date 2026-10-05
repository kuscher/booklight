package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Overture
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunGateTest {
    private fun begins(s: State, screenDp: Float = 1200f) = FirstRun.overture(s, motion = true, reader = false, plain = true, screenDp = screenDp)

    /** The order for each panel: the opening is counted, then it is asked what stands and how it begins. */
    @Test fun atTheFourthOpeningNeitherAScreenNorTheOpeningPieceComes() {
        var s = State(Run.NEW)
        repeat(FirstRun.MAX_OPENS) {
            s = FirstRun.opening(s, 1200f, plain = true)
            assertEquals(Screen.K1, FirstRun.stage(s, 1200f))
            assertEquals(Overture.WELCOME, begins(s))
        }
        s = FirstRun.opening(s, 1200f, plain = true)
        assertNull(FirstRun.stage(s, 1200f))
        assertEquals(Overture.NONE, begins(s))
    }

    /** Why the order matters: asked before it is counted, the fourth opening still says the piece plays, and nothing would stand after it. */
    @Test fun askedBeforeItIsCountedTheFourthOpeningWouldStillBegin() {
        val third = State(Run.NEW, opens = FirstRun.MAX_OPENS)
        assertEquals(Overture.WELCOME, begins(third))
        assertEquals(Overture.NONE, begins(FirstRun.opening(third, 1200f, plain = true)))
    }

    @Test fun onAScreenTooLowNothingStandsAndNothingIsCounted() {
        assertNull(FirstRun.stage(State(Run.NEW), 474f))
        assertEquals(State(Run.NEW), FirstRun.opening(State(Run.NEW), 474f, plain = true))
        assertEquals(Screen.K1, FirstRun.stage(State(Run.NEW), 475f))
        assertEquals(1, FirstRun.opening(State(Run.NEW), 475f, plain = true).opens)
    }

    /** A panel that carries another app's text, or that an example is typed into, shows no screen: it uses up none of the run's openings. */
    @Test fun aPanelInWhichNoScreenCanStandIsNotCounted() {
        assertEquals(State(Run.NEW), FirstRun.opening(State(Run.NEW), 1200f, plain = false))
        var s = State(Run.NEW)
        repeat(5) { s = FirstRun.opening(s, 1200f, plain = false) }
        assertEquals(Screen.K1, FirstRun.stage(FirstRun.opening(s, 1200f, plain = true), 1200f))
    }

    @Test fun whereThereIsNoRunThePanelIsNotCounted() {
        assertEquals(State(key = true), FirstRun.opening(State(key = true), 1200f, plain = true))
        assertNull(FirstRun.stage(State(key = true), 1200f))
    }

    /** The key first, then the count: the first press of the user's own key brings a run back that waited for it, and that opening is its first again. */
    @Test fun theKeyThatMadeThePanelIsLandedBeforeTheOpeningIsCounted() {
        val waiting = State(Run.NEW, helper = 1, opens = FirstRun.MAX_OPENS + 1)
        val s = FirstRun.opening(FirstRun.keyLanded(waiting), 1200f, plain = true)
        assertEquals(Screen.K4, FirstRun.stage(s, 1200f))
        assertEquals(1, s.opens)
    }

    /** A replay that had asked for another key and then waited is not brought back by a key that was known: only a first key is news. */
    @Test fun aKeyThatWasKnownDoesNotBringAWaitingRunBack() {
        val waiting = State(Run.REPLAY, done = listOf(FirstRun.SHOWN, FirstRun.CHANGE), helper = 1, opens = FirstRun.MAX_OPENS + 1, key = true)
        val s = FirstRun.keyLanded(waiting)
        assertTrue(FirstRun.parked(s))
        assertTrue(FirstRun.CHANGE !in s.done)
    }
}
