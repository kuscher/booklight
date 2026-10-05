package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Answer
import io.github.kuscher.booklight.core.FirstRun.Caption
import io.github.kuscher.booklight.core.FirstRun.Key
import io.github.kuscher.booklight.core.FirstRun.Read
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.State
import io.github.kuscher.booklight.core.FirstRun.Words
import org.junit.Assert.assertEquals
import org.junit.Test

class FirstRunKeyStepTest {
    private val k1 = State(Run.NEW)
    private val k2 = State(Run.NEW, helper = 1)
    private val k3 = State(Run.NEW, helper = 2)
    private val k4 = State(Run.NEW, key = true)
    private val lesson = State(Run.NEW, done = listOf("key"), key = true)

    @Test fun theKeysScreensOfferTheDialogOrNotNow() {
        for (s in listOf(k1, k2, k3)) assertEquals(listOf(Answer.OPEN_HELPER, Answer.NOT_NOW), FirstRun.answers(s))
        assertEquals(listOf(Answer.GO_ON), FirstRun.answers(k4))
        assertEquals(listOf(Answer.GO_ON, Answer.CHANGE_KEY), FirstRun.answers(State(Run.REPLAY, key = true)))
        assertEquals(emptyList<Answer>(), FirstRun.answers(State()))
    }

    @Test fun enterHasAnAnswerButWhereTheKeyIsTheThingToPress() {
        assertEquals(listOf(0, -1, 0, 0), listOf(k1, k2, k3, k4).map { FirstRun.armed(it) })
        assertEquals(-1, FirstRun.armed(State()))
    }

    @Test fun askingForTheDialogChangesNothingThatIsKept() {
        for (s in listOf(k1, k2, k3)) assertEquals(s, FirstRun.answer(s, Answer.OPEN_HELPER))
    }

    @Test fun tabGoesRoundAndTheFirstPressArmsTheFirst() {
        assertEquals(0, FirstRun.tab(-1, 2, back = false))
        assertEquals(0, FirstRun.tab(-1, 2, back = true))
        assertEquals(1, FirstRun.tab(0, 2, back = false))
        assertEquals(0, FirstRun.tab(1, 2, back = false))
        assertEquals(1, FirstRun.tab(0, 2, back = true))
        assertEquals(0, FirstRun.tab(1, 2, back = true))
        // One answer: it stays armed. None: nothing can be.
        assertEquals(0, FirstRun.tab(0, 1, back = false))
        assertEquals(0, FirstRun.tab(0, 1, back = true))
        assertEquals(-1, FirstRun.tab(0, 0, back = false))
        // An arming that is out of range (the screen changed under it) starts at the first.
        assertEquals(0, FirstRun.tab(5, 2, back = false))
    }

    @Test fun onlyQuickInsertNeedsSayingWhereItIs() {
        assertEquals(Caption.WHERE_AND_ANY, FirstRun.caption(k1, Key.QUICK_INSERT))
        assertEquals(Caption.ANY, FirstRun.caption(k1, Key.M))
        assertEquals(Caption.YOURS, FirstRun.caption(k2, Key.QUICK_INSERT))
        assertEquals(Caption.YOURS, FirstRun.caption(k2, Key.M))
    }

    /** A keyboard without Quick Insert was offered Action + M: there is no other key to try after it. */
    @Test fun theOtherKeysToTryAreNamedOnlyAfterQuickInsert() {
        assertEquals(Caption.TRY_OTHER, FirstRun.caption(k3, Key.QUICK_INSERT))
        assertEquals(Caption.ANY, FirstRun.caption(k3, Key.M))
        assertEquals(Caption.NONE, FirstRun.caption(k4, Key.QUICK_INSERT))
        assertEquals(Caption.NONE, FirstRun.caption(lesson, Key.QUICK_INSERT))
    }

    private val ours = Words("Customize", "Set shortcut", "Quick Insert")

    @Test fun theSystemsOwnWordsAreQuotedWhereTheyWereRead() {
        assertEquals(Words("Anpassen", "Speichern", "Schnelles Einfügen"), FirstRun.words(Read("Anpassen", "Speichern", "Schnelles Einfügen"), ours))
        assertEquals(Words("Anpassen", "Set shortcut", "Quick Insert"), FirstRun.words(Read(customize = " Anpassen "), ours))
    }

    /** The system's package cannot be read, a name is gone in another build, a word is empty or is no button's name: Booklight's own word. */
    @Test fun whereAWordCannotBeHadBooklightsOwnStandsIn() {
        assertEquals(ours, FirstRun.words(null, ours))
        assertEquals(ours, FirstRun.words(Read(), ours))
        assertEquals(ours, FirstRun.words(Read("", "   ", "\n"), ours))
        assertEquals(ours, FirstRun.words(Read("x".repeat(FirstRun.MOST_WORD + 1), "two\nlines", null), ours))
        assertEquals("x".repeat(FirstRun.MOST_WORD), FirstRun.words(Read("x".repeat(FirstRun.MOST_WORD)), ours).customize)
    }
}
