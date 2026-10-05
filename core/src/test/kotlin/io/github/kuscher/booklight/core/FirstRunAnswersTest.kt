package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Answer
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import io.github.kuscher.booklight.core.FirstRun.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunAnswersTest {
    private val lessons = listOf(Step.KEY, Step.OPEN, Step.SEARCH, Step.SUM).map { it.id }
    /** A new installation's run on each of its screens. */
    private val onEvery: Map<Screen, State> = mapOf(
        Screen.K1 to State(Run.NEW),
        Screen.K2 to State(Run.NEW, helper = 1),
        Screen.K3 to State(Run.NEW, helper = 2),
        Screen.K4 to State(Run.NEW, key = true),
        Screen.L2 to State(Run.NEW, done = lessons.take(1), key = true),
        Screen.L3 to State(Run.NEW, done = lessons.take(2), key = true),
        Screen.L4 to State(Run.NEW, done = lessons.take(3), key = true),
        Screen.Q to State(Run.NEW, done = lessons, key = true),
        Screen.C to State(Run.NEW, done = lessons + Step.ASK.id, key = true),
    )

    @Test fun theStatesOfThisTestStandOnTheirScreens() {
        assertEquals(Screen.entries.toSet(), onEvery.keys)
        for ((screen, s) in onEvery) assertEquals(screen, FirstRun.screen(s))
    }

    @Test fun notNowOnTheKeyEndsTheRunAndIsRemembered() {
        for (screen in listOf(Screen.K1, Screen.K2, Screen.K3)) assertEquals(State(mark = true), FirstRun.answer(onEvery.getValue(screen), Answer.NOT_NOW))
    }

    @Test fun goOnBringsTheFirstLesson() {
        val s = FirstRun.answer(onEvery.getValue(Screen.K4), Answer.GO_ON)
        assertEquals(Screen.L2, FirstRun.screen(s))
        assertTrue(s.mark)
    }

    @Test fun forAnInstallationThatWasThereBeforeGoOnIsTheEnd() {
        assertEquals(State(key = true, mark = true), FirstRun.answer(State(Run.UPDATE, helper = 1, key = true), Answer.GO_ON))
    }

    @Test fun skipIsAnAnswer() {
        assertEquals(Screen.L3, FirstRun.screen(FirstRun.answer(onEvery.getValue(Screen.L2), Answer.SKIP)))
        assertEquals(Screen.L4, FirstRun.screen(FirstRun.answer(onEvery.getValue(Screen.L3), Answer.SKIP)))
        assertEquals(Screen.Q, FirstRun.screen(FirstRun.answer(onEvery.getValue(Screen.L4), Answer.SKIP)))
    }

    /** The consent: no screen and no answer but Agree on the question switches suggestions on. */
    @Test fun onlyAgreeOnTheQuestionSwitchesSuggestionsOn() {
        for ((screen, s) in onEvery) for (a in Answer.entries)
            assertEquals("$a on $screen", screen == Screen.Q && a == Answer.AGREE, FirstRun.answer(s, a).suggestions)
    }

    @Test fun bothAnswersToTheQuestionBringTheChoices() {
        val yes = FirstRun.answer(onEvery.getValue(Screen.Q), Answer.AGREE)
        val no = FirstRun.answer(onEvery.getValue(Screen.Q), Answer.NOT_NOW)
        assertEquals(Screen.C, FirstRun.screen(yes))
        assertEquals(Screen.C, FirstRun.screen(no))
        assertFalse(no.suggestions)
    }

    @Test fun leavingTheChoicesEndsTheRun() {
        assertEquals(State(key = true, mark = true), FirstRun.answer(onEvery.getValue(Screen.C), Answer.DONE))
    }

    @Test fun anAnswerTheScreenDoesNotOfferChangesNothing() {
        assertEquals(onEvery.getValue(Screen.K1), FirstRun.answer(onEvery.getValue(Screen.K1), Answer.SKIP))
        assertEquals(onEvery.getValue(Screen.K4), FirstRun.answer(onEvery.getValue(Screen.K4), Answer.NOT_NOW))
        assertEquals(onEvery.getValue(Screen.L2), FirstRun.answer(onEvery.getValue(Screen.L2), Answer.DONE))
        assertEquals(onEvery.getValue(Screen.C), FirstRun.answer(onEvery.getValue(Screen.C), Answer.SKIP))
        assertEquals(State(), FirstRun.answer(State(), Answer.NOT_NOW))
    }

    @Test fun onlyAReplayChangesTheKey() {
        assertEquals(Screen.K1, FirstRun.screen(FirstRun.answer(State(Run.REPLAY, helper = 2, key = true), Answer.CHANGE_KEY)))
        assertEquals(Screen.K4, FirstRun.screen(FirstRun.answer(onEvery.getValue(Screen.K4), Answer.CHANGE_KEY)))
    }

    @Test fun theKeyLands() {
        val s = FirstRun.keyLanded(State(Run.NEW, helper = 2))
        assertTrue(s.key)
        assertEquals(Screen.K4, FirstRun.screen(s))
        assertEquals(0, s.helper)
        // With no run, the key is known and nothing else changes.
        assertEquals(State(key = true), FirstRun.keyLanded(State()))
        assertNull(FirstRun.screen(FirstRun.keyLanded(State())))
    }

    @Test fun aKeyThatWasKnownChangesNothing() {
        val s = State(Run.NEW, done = lessons.take(2), opens = 2, key = true)
        assertEquals(s, FirstRun.keyLanded(s))
    }

    @Test fun theNewKeyOfAReplayLands() {
        val asking = FirstRun.answer(State(Run.REPLAY, key = true), Answer.CHANGE_KEY)
        val s = FirstRun.keyLanded(FirstRun.helperCame(asking))
        assertEquals(Screen.K4, FirstRun.screen(s))
        assertFalse(FirstRun.CHANGE in s.done)
    }

    @Test fun theSystemsDialogIsCountedOnlyWhileTheKeyIsAskedFor() {
        assertEquals(Screen.K2, FirstRun.screen(FirstRun.helperCame(onEvery.getValue(Screen.K1))))
        assertEquals(Screen.K3, FirstRun.screen(FirstRun.helperCame(onEvery.getValue(Screen.K2))))
        assertEquals(Screen.K3, FirstRun.screen(FirstRun.helperCame(onEvery.getValue(Screen.K3))))
        for (screen in listOf(Screen.K4, Screen.L2, Screen.Q, Screen.C)) assertEquals(onEvery.getValue(screen), FirstRun.helperCame(onEvery.getValue(screen)))
        assertEquals(State(), FirstRun.helperCame(State()))
    }
}
