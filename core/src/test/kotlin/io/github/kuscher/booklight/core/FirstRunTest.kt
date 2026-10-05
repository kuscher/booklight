package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import io.github.kuscher.booklight.core.FirstRun.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunTest {
    private fun done(vararg steps: Step) = steps.map { it.id }
    private val lessons = done(Step.KEY, Step.OPEN, Step.SEARCH, Step.SUM)

    @Test fun noRunNoScreen() {
        assertNull(FirstRun.screen(State()))
        assertNull(FirstRun.step(State(key = true)))
        assertEquals(emptyList<Step>(), FirstRun.steps(State()))
    }

    @Test fun aStoredNumberNobodyKnowsIsNoRun() {
        assertEquals(listOf(Run.NONE, Run.NEW, Run.UPDATE, Run.REPLAY, Run.NONE, Run.NONE), listOf(0, 1, 2, 3, 4, -1).map { Run.of(it) })
        assertEquals(listOf(0, 1, 2, 3), Run.entries.map { it.id })
    }

    @Test fun theKeysStepHasFourScreens() {
        assertEquals(Screen.K1, FirstRun.screen(State(Run.NEW)))
        assertEquals(Screen.K2, FirstRun.screen(State(Run.NEW, helper = 1)))
        assertEquals(Screen.K3, FirstRun.screen(State(Run.NEW, helper = 2)))
        assertEquals(Screen.K3, FirstRun.screen(State(Run.NEW, helper = 7)))
        assertEquals(Screen.K4, FirstRun.screen(State(Run.NEW, helper = 1, key = true)))
    }

    @Test fun aReplayThatChangesTheKeyAsksThoughOneIsKnown() {
        assertEquals(Screen.K1, FirstRun.screen(State(Run.REPLAY, done = listOf(FirstRun.CHANGE), key = true)))
        assertEquals(Screen.K2, FirstRun.screen(State(Run.REPLAY, done = listOf(FirstRun.CHANGE), helper = 1, key = true)))
    }

    @Test fun theStepsStandInTheirOrder() {
        val s = State(Run.NEW, key = true)
        assertEquals(Screen.L2, FirstRun.screen(s.copy(done = done(Step.KEY))))
        assertEquals(Screen.L3, FirstRun.screen(s.copy(done = done(Step.KEY, Step.OPEN))))
        assertEquals(Screen.L4, FirstRun.screen(s.copy(done = done(Step.KEY, Step.OPEN, Step.SEARCH))))
        assertEquals(Screen.Q, FirstRun.screen(s.copy(done = lessons)))
        assertEquals(Screen.C, FirstRun.screen(s.copy(done = lessons + Step.ASK.id)))
        assertNull(FirstRun.step(s.copy(done = lessons + Step.ASK.id + Step.CHOICES.id)))
    }

    @Test fun aStepDoneOutOfTurnIsPassedOver() {
        assertEquals(Screen.L3, FirstRun.screen(State(Run.NEW, done = done(Step.KEY, Step.OPEN, Step.SUM), key = true)))
        assertEquals(Screen.Q, FirstRun.screen(State(Run.NEW, done = done(Step.KEY, Step.OPEN, Step.SUM, Step.SEARCH), key = true)))
    }

    @Test fun theQuestionIsAStepOnlyWhileSuggestionsAreOff() {
        assertEquals(Screen.Q, FirstRun.screen(State(Run.REPLAY, done = lessons, key = true)))
        assertEquals(Screen.C, FirstRun.screen(State(Run.REPLAY, done = lessons, key = true, suggestions = true)))
        // Answered with Agree in this run, it stays a step that is over: the count does not change under the reader.
        assertEquals(6, FirstRun.steps(State(Run.NEW, done = lessons + Step.ASK.id, key = true, suggestions = true)).size)
    }

    /**
     * "Show sums" switched off, as an installation that has been lived in may have it when the run is asked for again:
     * there is no sum to practise, so its lesson is no step. (With the lesson left in, its example found the web's row
     * and the lesson could only be skipped.)
     */
    @Test fun withSumsSwitchedOffTheSumsLessonIsNoStep() {
        val s = State(Run.REPLAY, done = done(Step.KEY, Step.OPEN, Step.SEARCH), key = true, sums = false)
        assertEquals(Screen.Q, FirstRun.screen(s))
        assertEquals(Screen.C, FirstRun.screen(s.copy(suggestions = true)))
        assertFalse(Step.SUM in FirstRun.steps(s))
        // The counter counts the steps this run has: two lessons and the question after the key.
        assertEquals(4 to 4, FirstRun.count(s))
        assertEquals(2 to 4, FirstRun.count(State(Run.REPLAY, done = done(Step.KEY), key = true, sums = false)))
        // Done in this run before the switch went off, it stays a step that is over: the count does not change under the reader.
        assertEquals(6, FirstRun.steps(State(Run.NEW, done = lessons, key = true, sums = false)).size)
        // A new installation's run is no other: whoever switches sums off before the lesson has no lesson.
        assertEquals(Screen.Q, FirstRun.screen(State(Run.NEW, done = done(Step.KEY, Step.OPEN, Step.SEARCH), key = true, sums = false)))
        // The lesson cannot be reached by name either (the debug hooks), and with sums on everything is as it was.
        assertNull(FirstRun.at(State(key = true, sums = false), Screen.L4))
        assertEquals(Screen.L4, FirstRun.screen(s.copy(sums = true)))
    }

    /** A whole run with sums switched off: every screen but the sum's stands once, in its order, and the run ends. */
    @Test fun aRunWithSumsSwitchedOffIsWholeWithoutTheSum() {
        var s = FirstRun.replay(State(key = true, sums = false), overture = false)
        val stood = ArrayList<Screen>()
        while (true) {
            val on = FirstRun.screen(s) ?: break
            stood += on
            s = FirstRun.answer(s, when (on) { Screen.K4 -> FirstRun.Answer.GO_ON; Screen.Q -> FirstRun.Answer.NOT_NOW; Screen.C -> FirstRun.Answer.DONE; else -> FirstRun.Answer.SKIP })
        }
        assertEquals(listOf(Screen.K4, Screen.L2, Screen.L3, Screen.Q, Screen.C), stood)
        assertEquals(Run.NONE, s.run)
        // No switch was touched by any of it.
        assertFalse(s.sums)
        assertFalse(s.suggestions)
    }

    @Test fun theRunOfAnInstallationThatWasThereBeforeIsTheKeyAlone() {
        assertEquals(listOf(Step.KEY), FirstRun.steps(State(Run.UPDATE)))
        assertEquals(Screen.K1, FirstRun.screen(State(Run.UPDATE)))
        assertEquals(Screen.K4, FirstRun.screen(State(Run.UPDATE, key = true)))
        assertNull(FirstRun.screen(State(Run.UPDATE, done = done(Step.KEY), key = true)))
    }

    @Test fun theCounterSaysWhereAScreenIsInItsRun() {
        val s = State(Run.NEW, key = true)
        assertEquals(1 to 5, FirstRun.count(State(Run.NEW)))
        assertEquals(1 to 5, FirstRun.count(State(Run.NEW, helper = 2)))
        assertEquals(1 to 5, FirstRun.count(s))
        assertEquals(2 to 5, FirstRun.count(s.copy(done = done(Step.KEY))))
        assertEquals(3 to 5, FirstRun.count(s.copy(done = done(Step.KEY, Step.OPEN))))
        assertEquals(4 to 5, FirstRun.count(s.copy(done = done(Step.KEY, Step.OPEN, Step.SEARCH))))
        assertEquals(5 to 5, FirstRun.count(s.copy(done = lessons)))
    }

    @Test fun noCounterOnTheLastScreenNorInARunOfOneStep() {
        assertNull(FirstRun.count(State(Run.NEW, done = lessons + Step.ASK.id, key = true)))
        assertNull(FirstRun.count(State(Run.UPDATE)))
        assertNull(FirstRun.count(State()))
        // A replay with suggestions on has no question: four steps are counted.
        assertEquals(1 to 4, FirstRun.count(State(Run.REPLAY, key = true, suggestions = true)))
    }

    @Test fun aRunThatWaitsShowsNothing() {
        assertEquals(Screen.K1, FirstRun.screen(State(Run.NEW, opens = FirstRun.MAX_OPENS)))
        assertNull(FirstRun.screen(State(Run.NEW, opens = FirstRun.MAX_OPENS + 1)))
        assertTrue(FirstRun.parked(State(Run.NEW, opens = FirstRun.MAX_OPENS + 1)))
        assertFalse(FirstRun.parked(State(opens = FirstRun.MAX_OPENS + 1)))
    }

    @Test fun aLowScreenHasNoFirstRunInThePanel() {
        assertFalse(FirstRun.fits(474f))
        assertTrue(FirstRun.fits(475f))
    }

    /** How first run meets the one rule for the place under the empty field: a screen that is due has the place first. */
    @Test fun aScreenThatIsDueHasThePlaceFirst() {
        fun under(s: State, last: Boolean) = Under.choose(untouched = true, stage = FirstRun.screen(s) != null, copy = true, zero = true, seats = 3, tip = true, last = last)
        assertEquals(Under.What.STAGE, under(State(Run.NEW), last = false))
        assertEquals(Under.What.STAGE, under(State(Run.NEW, done = lessons, key = true), last = true))
        // No run, or a run that waits: the place is the others', in their order.
        assertEquals(Under.What.COPY, under(State(), last = true))
        assertEquals(Under.What.COPY, under(State(Run.NEW, opens = FirstRun.MAX_OPENS + 1), last = true))
    }
}
