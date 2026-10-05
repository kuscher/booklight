package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Answer
import io.github.kuscher.booklight.core.FirstRun.Overture
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "First steps": the run asked for again, from the Booklight window's row or by its command in the panel; and where the steps wait until then. */
class FirstRunAgainTest {
    /** An installation that has been lived in: a key, suggestions on, the icon clicked, step 1 over long ago, sums switched off. */
    private val lived = State(icon = true, key = true, suggestions = true, mark = true, sums = false)
    /** A run left half-way: the key and the first lesson are over. */
    private val halfWay = State(Run.NEW, done = listOf("show", "key", "open"), opens = 2, key = true)

    private fun begins(s: State) = FirstRun.overture(s, motion = true, reader = false, plain = true, screenDp = 1200f)

    /** The window's row plays the whole of it, the opening piece first, at the speed that is set; it lands in "Your key works". */
    @Test fun theWindowsRowPlaysTheWholeOfItAgainThePieceFirst() {
        val s = FirstRun.again(State(key = true, mark = true), overture = true)
        assertEquals(Run.REPLAY, s.run)
        assertEquals(Overture.WELCOME, begins(FirstRun.opening(s, 1200f, plain = true)))
        assertFalse(FirstRun.slow(s, Overture.WELCOME))
        assertEquals(Screen.K4, FirstRun.screen(s))
        assertEquals(listOf(Answer.GO_ON, Answer.CHANGE_KEY), FirstRun.answers(s))
    }

    /** The typed command does not: the user is at work in the panel. The run starts on "Your key works", in the panel that is open. */
    @Test fun theTypedCommandStartsWithoutThePiece() {
        val s = FirstRun.again(State(key = true, mark = true), overture = false)
        assertEquals(Run.REPLAY, s.run)
        assertEquals(Overture.NONE, begins(s))
        assertEquals(Screen.K4, FirstRun.screen(s))
        assertEquals(0, FirstRun.armed(s))
        // Nor at the next opening, if the panel is closed on it: the piece was not asked for.
        assertEquals(Overture.NONE, begins(FirstRun.opening(s, 1200f, plain = true)))
    }

    /** Asked for by its command, the run stands in the panel that is open: that panel is its first opening, and it has two more. */
    @Test fun thePanelTheCommandIsTypedInIsTheRunsOpening() {
        val here = FirstRun.againHere(State(key = true, mark = true))
        assertEquals(FirstRun.again(State(key = true, mark = true), overture = false).copy(opens = 1), here)
        assertEquals(Screen.K4, FirstRun.screen(here))
        var s = here
        repeat(FirstRun.MAX_OPENS - 1) { s = FirstRun.opening(s, 1200f, plain = true); assertEquals(Screen.K4, FirstRun.screen(s)) }
        assertNull(FirstRun.screen(FirstRun.opening(s, 1200f, plain = true)))
        // An unfinished run asked for by the command: where it stopped, counted anew from this panel.
        assertEquals(halfWay.copy(opens = 1), FirstRun.againHere(halfWay.copy(opens = FirstRun.MAX_OPENS + 1)))
    }

    /** Without a key the run asked for again begins where a new one does: on the key's first screen. */
    @Test fun withoutAKeyItBeginsOnTheKeysFirstScreen() {
        for (overture in listOf(true, false)) {
            val s = FirstRun.again(State(mark = true), overture)
            assertEquals(Screen.K1, FirstRun.screen(s))
            assertEquals(1 to 5, FirstRun.count(s))
        }
    }

    /** A run that is not finished goes on where it stopped, from either place, and the piece does not come with it. */
    @Test fun anUnfinishedRunGoesOnWhereItStopped() {
        var waiting = halfWay
        repeat(FirstRun.MAX_OPENS) { waiting = FirstRun.opened(waiting) }
        assertNull(FirstRun.screen(waiting))
        for (overture in listOf(true, false)) for (s in listOf(halfWay, waiting)) {
            val on = FirstRun.again(s, overture)
            assertEquals(Run.NEW, on.run)
            assertEquals(Screen.L3, FirstRun.screen(on))
            assertEquals(halfWay.done, on.done)
            assertEquals(0, on.opens)
            assertEquals(Overture.NONE, begins(FirstRun.opening(on, 1200f, plain = true)))
        }
        // The run of an installation that was there before, left on its key: the key's step again, and nothing more.
        assertEquals(Screen.K1, FirstRun.screen(FirstRun.again(State(Run.UPDATE, opens = FirstRun.MAX_OPENS + 1), overture = true)))
    }

    /** Asked for again, it changes no switch and forgets no key, whichever way it is asked and wherever it stood. */
    @Test fun askedAgainItTouchesNoSwitchAndNoKey() {
        for (overture in listOf(true, false)) for (s in listOf(lived, lived.copy(run = Run.NEW, done = listOf("show", "key")), State(), halfWay)) {
            val on = FirstRun.again(s, overture)
            assertEquals("$s", listOf(s.key, s.suggestions, s.sums, s.icon, s.mark), listOf(on.key, on.suggestions, on.sums, on.icon, on.mark))
        }
        // With suggestions on the question is not asked again, and with sums off there is no sum to practise.
        assertEquals(listOf(FirstRun.Step.KEY, FirstRun.Step.OPEN, FirstRun.Step.SEARCH, FirstRun.Step.CHOICES), FirstRun.steps(FirstRun.again(lived, overture = true)))
    }

    /** What the window's row says: "First steps"; while a run is unfinished, "Go on with the first steps" and where it stopped. */
    @Test fun theRowSaysGoOnWhileARunIsUnfinished() {
        assertEquals(FirstRun.Offer(goesOn = false, count = null), FirstRun.offer(State()))
        assertEquals(FirstRun.Offer(goesOn = false, count = null), FirstRun.offer(lived))
        assertEquals(FirstRun.Offer(goesOn = true, count = 3 to 5), FirstRun.offer(halfWay))
        // A run that waits says where it stopped, not nothing.
        assertEquals(FirstRun.Offer(goesOn = true, count = 3 to 5), FirstRun.offer(halfWay.copy(opens = FirstRun.MAX_OPENS + 1)))
        // The run of one step has no counter, and the choices have none.
        assertEquals(FirstRun.Offer(goesOn = true, count = null), FirstRun.offer(State(Run.UPDATE)))
        assertEquals(FirstRun.Offer(goesOn = true, count = null), FirstRun.offer(State(Run.NEW, done = listOf("key", "open", "search", "sum", "ask"), key = true)))
    }

    /** "Not now" on the key's step ends the run: the steps wait in the Booklight window, and the bare field says so for that opening. */
    @Test fun notNowOnTheKeyLeavesAWordBehind() {
        for (helper in 0..2) for (run in listOf(Run.NEW, Run.UPDATE)) {
            val before = State(run, helper = helper)
            assertTrue("$run $helper", FirstRun.later(before, FirstRun.answer(before, Answer.NOT_NOW)))
        }
        // A replay that asks for another key, left by "Not now": the same.
        val change = State(Run.REPLAY, done = listOf(FirstRun.SHOWN, FirstRun.CHANGE), key = true)
        assertTrue(FirstRun.later(change, FirstRun.answer(change, Answer.NOT_NOW)))
    }

    /** Nothing else leaves it: a run that goes on, a run that is finished, an answer that changes nothing. */
    @Test fun noOtherAnswerLeavesThatWord() {
        val k1 = State(Run.NEW)
        assertFalse(FirstRun.later(k1, FirstRun.answer(k1, Answer.OPEN_HELPER)))
        assertFalse(FirstRun.later(k1, FirstRun.helperCame(k1)))
        assertFalse(FirstRun.later(k1, FirstRun.keyLanded(k1)))
        // "Go on" in the run of one step ends that run as finished: there is nothing left to wait.
        val k4 = State(Run.UPDATE, key = true)
        assertEquals(Run.NONE, FirstRun.answer(k4, Answer.GO_ON).run)
        assertFalse(FirstRun.later(k4, FirstRun.answer(k4, Answer.GO_ON)))
        // The question's "Not now" is an answer like another, and the choices' end is the run's own end.
        val q = State(Run.NEW, done = listOf("key", "open", "search", "sum"), key = true)
        assertFalse(FirstRun.later(q, FirstRun.answer(q, Answer.NOT_NOW)))
        val c = q.copy(done = q.done + "ask")
        assertEquals(Run.NONE, FirstRun.answer(c, Answer.DONE).run)
        assertFalse(FirstRun.later(c, FirstRun.answer(c, Answer.DONE)))
        // Nobody without a run is told anything.
        assertFalse(FirstRun.later(State(), State()))
        assertFalse(FirstRun.later(lived, FirstRun.opening(lived, 1200f, plain = true)))
    }

    /** An unfinished run stands in three openings. In the fourth nothing stands, and that opening alone says where the steps wait. */
    @Test fun aRunThatUsedUpItsOpeningsLeavesTheSameWordOnce() {
        var s = State(Run.NEW, done = listOf("show", "key", "open"), key = true)
        val said = ArrayList<Boolean>()
        repeat(6) {
            val after = FirstRun.opening(s, 1200f, plain = true)
            said += FirstRun.later(s, after)
            s = after
        }
        assertEquals(listOf(false, false, false, true, false, false), said)
        // A panel that is no opening of the run (it carries another app's text) says nothing, and uses nothing up.
        val third = State(Run.NEW, done = listOf("show", "key"), opens = FirstRun.MAX_OPENS, key = true)
        assertFalse(FirstRun.later(third, FirstRun.opening(third, 1200f, plain = false)))
    }

    /**
     * The command is offered where it was typed for, and nowhere else: someone who never asked for first run must not meet
     * its row for a letter or two ("f", "fi", "w"). Typed for: three letters or more that begin its name, or one of its
     * other words in full, in any case.
     */
    @Test fun theCommandIsOfferedOnlyWhereItWasTypedFor() {
        fun typedFor(typed: String) = FirstRun.typedFor(typed, "First steps", listOf("tour", "welcome", "intro"))
        for (typed in listOf("fir", "firs", "first", "first ", "first s", "first steps", "First Steps", "FIR")) assertTrue(typed, typedFor(typed))
        for (typed in listOf("tour", "welcome", "intro", "Welcome", "TOUR", " intro ")) assertTrue(typed, typedFor(typed))
        // A letter or two; a later word of its name, its initials, letters inside it; one of its other words begun, or with more after it.
        for (typed in listOf("", " ", "f", "fi", "fi ", "s", "ste", "steps", "fs", "irs", "first steps x", "first step s")) assertFalse(typed, typedFor(typed))
        for (typed in listOf("t", "to", "tou", "w", "we", "wel", "welcom", "i", "int", "intr", "tours", "tour x")) assertFalse(typed, typedFor(typed))
        // In German, by that language's name and words.
        fun getippt(typed: String) = FirstRun.typedFor(typed, "Erste Schritte", listOf("tour", "willkommen", "intro", "einführung"))
        for (typed in listOf("ers", "erste", "erste s", "erste schritte", "Erste Schritte", "willkommen", "einführung", "Einführung")) assertTrue(typed, getippt(typed))
        for (typed in listOf("e", "er", "sch", "schritte", "will", "einf", "fir", "welcome")) assertFalse(typed, getippt(typed))
        // A list of words as a resource gives it, with nothing between two commas, offers nothing for nothing typed.
        assertFalse(FirstRun.typedFor("", "First steps", listOf("tour", "")))
        assertFalse(FirstRun.typedFor(" ", "First steps", listOf("tour", " ")))
        assertEquals(3, FirstRun.TYPED_LETTERS)
    }

    /** The command carries nothing the executor knows, and nothing a lesson would take for practice: the panel catches it first. */
    @Test fun theCommandCarriesNothingToRun() {
        assertEquals(Effect.Internal("first"), FirstRun.AGAIN)
        assertFalse(FirstRun.AGAIN == Show.NOTHING)
        val lesson = State(Run.NEW, done = listOf("key"), key = true)
        assertEquals(FirstRun.Enter.AS_ALWAYS, FirstRun.enters(lesson, null, FirstRun.AGAIN, searchable = true))
    }
}
