package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Answer
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirstRunUpdateTest {
    /**
     * What a build from before first run leaves of the settings when it writes them: it keeps what
     * it knows (whether a key is known, the switch, the list the mark is in) and drops the rest.
     */
    private fun older(s: State) = State(key = s.key, suggestions = s.suggestions, mark = s.mark)
    /** The trip: this build's settings, written by an older build, read by this build again. */
    private fun trip(s: State) = FirstRun.forUpdate(older(s))

    @Test fun anUpdateWithAKeyGetsNothing() {
        val s = FirstRun.forUpdate(State(key = true))
        assertEquals(State(key = true), s)
        assertNull(FirstRun.screen(s))
    }

    @Test fun anUpdateWithoutAKeyGetsTheKeysStepOnce() {
        val s = FirstRun.forUpdate(State())
        assertEquals(Run.UPDATE, s.run)
        assertEquals(Screen.K1, FirstRun.screen(s))
        assertNull(FirstRun.count(s))
    }

    @Test fun whatTheUserHadSwitchedOnStaysOn() {
        assertEquals(State(key = true, suggestions = true), FirstRun.forUpdate(State(key = true, suggestions = true)))
        assertEquals(true, FirstRun.forUpdate(State(suggestions = true)).suggestions)
    }

    /** Settings nobody has written since are read anew at every start: the answer must not change. */
    @Test fun theSameSettingsGiveTheSameAnswerEveryTime() {
        for (old in listOf(State(), State(key = true), State(mark = true), State(suggestions = true), State(key = true, mark = true)))
            assertEquals(FirstRun.forUpdate(old), FirstRun.forUpdate(FirstRun.forUpdate(old)))
    }

    @Test fun whoSaidNotNowIsNotAskedAgainAfterTheTrip() {
        val said = FirstRun.answer(FirstRun.forUpdate(State()), Answer.NOT_NOW)
        assertEquals(Run.NONE, trip(said).run)
        assertNull(FirstRun.screen(trip(said)))
    }

    @Test fun whoseKeyWorksIsNotAskedAgainAfterTheTrip() {
        val works = FirstRun.keyLanded(FirstRun.helperCame(FirstRun.forUpdate(State())))
        assertEquals(Screen.K4, FirstRun.screen(works))
        assertNull(FirstRun.screen(trip(works)))
        assertNull(FirstRun.screen(trip(FirstRun.answer(works, Answer.GO_ON))))
    }

    @Test fun aRunHalfDoneComesBackFromTheTripAsTheKeysStepOrAsNone() {
        // Without a key: the key's step from its start.
        assertEquals(Screen.K1, FirstRun.screen(trip(State(Run.UPDATE, helper = 2, opens = 2))))
        assertEquals(Screen.K1, FirstRun.screen(trip(State(Run.NEW, done = listOf("show"), helper = 1))))
        // With a key: no run. The lessons are not offered by themselves again.
        assertNull(FirstRun.screen(trip(State(Run.NEW, done = listOf("show", "key", "open"), key = true, mark = true))))
    }

    @Test fun aRestoredBackupOfAnInstallationWithAKeyShowsNothing() {
        assertNull(FirstRun.screen(FirstRun.forUpdate(State(key = true, suggestions = true))))
    }

    /** A backup made in the middle of a run is restored as it is; where the run cannot go on, it gives up by itself. */
    @Test fun aRestoredRunThatIsNeverGoneOnWithWaitsAfterThreeOpenings() {
        var s = State(Run.NEW, done = listOf("show", "key", "open"), key = true, mark = true)
        repeat(FirstRun.MAX_OPENS) { s = FirstRun.opened(s); assertEquals(Screen.L3, FirstRun.screen(s)) }
        assertNull(FirstRun.screen(FirstRun.opened(s)))
    }

    @Test fun theMarkOutlastsShowTheTipsAgain() {
        assertEquals(listOf(FirstRun.MARK), FirstRun.keepMark(listOf("help", FirstRun.MARK, "places")))
        assertEquals(emptyList<String>(), FirstRun.keepMark(listOf("help", "places")))
    }
}
