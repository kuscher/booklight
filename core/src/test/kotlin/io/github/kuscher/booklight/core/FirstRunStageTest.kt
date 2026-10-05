package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Answer
import io.github.kuscher.booklight.core.FirstRun.Enter
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunStageTest {
    private val steps = listOf("key", "open", "search", "sum")
    private val k1 = State(Run.NEW)
    private val l2 = State(Run.NEW, done = steps.take(1), key = true)
    private val l3 = State(Run.NEW, done = steps.take(2), key = true)
    private val l4 = State(Run.NEW, done = steps.take(3), key = true)
    private val q = State(Run.NEW, done = steps, key = true)
    private val c = State(Run.NEW, done = steps + "ask", key = true)

    @Test fun theStatesOfThisTestStandOnTheirScreens() {
        assertEquals(listOf(Screen.K1, Screen.L2, Screen.L3, Screen.L4, Screen.Q, Screen.C), listOf(k1, l2, l3, l4, q, c).map { FirstRun.screen(it) })
    }

    @Test fun aLessonOffersSkipAndTheQuestionNotNowThenAgree() {
        for (s in listOf(l2, l3, l4)) assertEquals(listOf(Answer.SKIP), FirstRun.answers(s))
        assertEquals(listOf(Answer.NOT_NOW, Answer.AGREE), FirstRun.answers(q))
        // The choices are a list of their own: no answers stand beside them.
        assertEquals(emptyList<Answer>(), FirstRun.answers(c))
    }

    @Test fun nothingIsArmedInALessonNorOnTheQuestion() {
        for (s in listOf(l2, l3, l4, q, c)) assertEquals(-1, FirstRun.armed(s))
    }

    /** Enter on a lesson's empty field does nothing, as every day. Tab, then Enter skips the lesson. */
    @Test fun tabThenEnterSkipsALesson() {
        assertNull(FirstRun.entered(l2, FirstRun.armed(l2), arrived = true))
        val armed = FirstRun.tabbed(l2, FirstRun.armed(l2), back = false, arrived = true)
        assertEquals(Answer.SKIP, FirstRun.entered(l2, armed, arrived = true))
        assertEquals(Screen.L3, FirstRun.screen(FirstRun.answer(l2, Answer.SKIP)))
    }

    /** The consent. Enter alone gives the question no answer; Tab chooses Not now, then Agree, and round again. */
    @Test fun enterGivesTheQuestionNoAnswerUntilOneIsChosen() {
        assertNull(FirstRun.entered(q, FirstRun.armed(q), arrived = true))
        val first = FirstRun.tabbed(q, FirstRun.armed(q), back = false, arrived = true)
        val second = FirstRun.tabbed(q, first, back = false, arrived = true)
        assertEquals(Answer.NOT_NOW, FirstRun.entered(q, first, arrived = true))
        assertEquals(Answer.AGREE, FirstRun.entered(q, second, arrived = true))
        assertEquals(first, FirstRun.tabbed(q, second, back = false, arrived = true))
    }

    /**
     * Tab, then Enter skips a lesson. Whoever goes on pressing them, lesson after lesson and on through the question,
     * has agreed to nothing: on the question those keys say Not now.
     */
    @Test fun theKeysThatSkipTheLessonsDoNotAgree() {
        var s = l2
        repeat(4) {
            val armed = FirstRun.tabbed(s, FirstRun.armed(s), back = false, arrived = true)
            s = FirstRun.answer(s, FirstRun.entered(s, armed, arrived = true)!!)
        }
        assertEquals(Screen.C, FirstRun.screen(s))
        assertFalse(s.suggestions)
    }

    /** Keys pressed blind, while the panel is still opening and nothing of the stage can be seen, choose nothing and answer nothing. */
    @Test fun keysPressedBeforeTheStageIsThereDoNothing() {
        for (s in listOf(k1, l2, q)) {
            assertEquals(FirstRun.armed(s), FirstRun.tabbed(s, FirstRun.armed(s), back = false, arrived = false))
            for (armed in -1..1) assertNull(FirstRun.entered(s, armed, arrived = false))
        }
        // (Once it is there, a key's screen answers Enter at once: its first answer is armed.)
        assertEquals(Answer.OPEN_HELPER, FirstRun.entered(k1, FirstRun.armed(k1), arrived = true))
    }

    /**
     * A screen takes a key only once it has been in view for a moment, and a screen whose parts are set down one after
     * the other is in view only once its answers have begun to show. A press made while they are still coming answers
     * nothing, however long ago the screen "came".
     */
    @Test fun aScreenThatIsStillArrivingIsNotInViewSooner() {
        val opened = 1_000L
        val came = 5_000L
        // Whole at once: 350 ms after it came.
        assertFalse(FirstRun.inView(press = came + 349, opened = opened, came = came, lead = 0))
        assertTrue(FirstRun.inView(press = came + 350, opened = opened, came = came, lead = 0))
        // Its answers begin to show 383 ms after it: 350 ms after them, not after the screen.
        assertFalse(FirstRun.inView(press = came + 350, opened = opened, came = came, lead = 383))
        assertFalse(FirstRun.inView(press = came + 383 + 349, opened = opened, came = came, lead = 383))
        assertTrue(FirstRun.inView(press = came + 383 + 350, opened = opened, came = came, lead = 383))
        // A screen the panel opens on is in view from the glass's opening, not from the moment the panel was made.
        assertFalse(FirstRun.inView(press = 1_000 + 120 + 349, opened = 1_000, came = 400, lead = 120))
        assertTrue(FirstRun.inView(press = 1_000 + 120 + 350, opened = 1_000, came = 400, lead = 120))
        // Nothing makes a screen count sooner than one that stands whole.
        assertFalse(FirstRun.inView(press = came + 349, opened = opened, came = came, lead = -500))
        assertEquals(350L, FirstRun.SEEN_MS)
    }

    /**
     * A screen of the key's step that came while the system's dialog was over the panel ("Now press your keys" a moment
     * after the dialog came, "Your key works" where the key landed under it) was not in view while it was covered. Its
     * keys count from when the panel was uncovered, or from when the answer began to show where that is later: never
     * from when it came under the dialog. A screen that was never covered counts as it did.
     */
    @Test fun aScreenThatCameUnderTheDialogIsInViewOnceThePanelIsUncovered() {
        val opened = 1_000L
        val came = 5_000L
        // "Now press your keys" came under the dialog, which went four seconds later: a third of a second after that.
        assertFalse(FirstRun.inView(press = 9_000 + 349, opened = opened, came = came, lead = 0, uncovered = 9_000))
        assertTrue(FirstRun.inView(press = 9_000 + 350, opened = opened, came = came, lead = 0, uncovered = 9_000))
        // "Your key works": "Go on" begins to show 80 ms after the key landed, and the dialog has gone 240 ms after it.
        assertFalse(FirstRun.inView(press = came + 80 + 350, opened = opened, came = came, lead = 80, uncovered = came + 240))
        assertFalse(FirstRun.inView(press = came + 240 + 349, opened = opened, came = came, lead = 80, uncovered = came + 240))
        assertTrue(FirstRun.inView(press = came + 240 + 350, opened = opened, came = came, lead = 80, uncovered = came + 240))
        // Uncovered before its answer began to show (a slow set-down): the answer's own moment is the later one.
        assertFalse(FirstRun.inView(press = came + 320 + 349, opened = opened, came = came, lead = 320, uncovered = came + 100))
        assertTrue(FirstRun.inView(press = came + 320 + 350, opened = opened, came = came, lead = 320, uncovered = came + 100))
        // Still covered, the moment is not known: given as now, no press counts, however long ago the screen came.
        assertFalse(FirstRun.inView(press = 60_000, opened = opened, came = came, lead = 0, uncovered = 60_000))
        // Never covered: as it was, to the millisecond.
        for (press in listOf(came + 349, came + 350)) assertEquals(FirstRun.inView(press, opened, came, lead = 0), FirstRun.inView(press, opened, came, lead = 0, uncovered = 0))
    }

    /**
     * What a screen says to a screen reader is its parts, one sentence after another. A part that ends in a mark of its
     * own keeps it and gets no full stop after it: the question's title must not be read as "…as you type?.".
     */
    @Test fun aScreensSentenceEndsEachOfItsPartsOnce() {
        assertEquals(
            "5 of 5. Suggest searches as you type? Off until you agree. Nothing is chosen. Tab for Not now, Tab again for Agree.",
            FirstRun.sentence(listOf("5 of 5", "Suggest searches as you type?", "Off until you agree.", "Nothing is chosen. Tab for Not now, Tab again for Agree.")),
        )
        assertEquals("5 von 5. Beim Tippen Suchen vorschlagen? Tab für „Nicht jetzt“.", FirstRun.sentence(listOf("5 von 5", "Beim Tippen Suchen vorschlagen?", "Tab für „Nicht jetzt“.")))
        assertEquals("1 of 4. Didn’t work? Or try Action + M.", FirstRun.sentence(listOf("1 of 4", "Didn’t work?", "Or try Action + M")))
        assertEquals("It works! Go on.", FirstRun.sentence(listOf("It works!", "Go on")))
        // A part that says nothing is left out, and nothing at all is no sentence.
        assertEquals("Open an app.", FirstRun.sentence(listOf("", "Open an app ", " ")))
        assertEquals("", FirstRun.sentence(emptyList()))
    }

    /**
     * The keys that skip (Tab, then Enter), pressed while a screen is being set down, choose nothing and answer
     * nothing: on the question they do not even reach "Not now", and on the key's screen Enter is not the armed
     * answer's until that answer has been in view.
     */
    @Test fun keysPressedWhileAScreenIsSetDownDoNothing() {
        val came = 10_000L
        val lead = 142L       // the question's answers are the last of its parts
        for (press in listOf(came, came + lead, came + lead + 349)) {
            val seen = FirstRun.inView(press, opened = 0, came = came, lead = lead)
            val armed = FirstRun.tabbed(q, FirstRun.armed(q), back = false, arrived = seen)
            assertEquals(-1, armed)
            assertNull(FirstRun.entered(q, armed, arrived = seen))
        }
        val seen = FirstRun.inView(came + lead + 350, opened = 0, came = came, lead = lead)
        assertEquals(Answer.NOT_NOW, FirstRun.entered(q, FirstRun.tabbed(q, -1, back = false, arrived = seen), arrived = seen))
        assertNull(FirstRun.entered(k1, FirstRun.armed(k1), arrived = FirstRun.inView(came + 383 + 100, opened = 0, came = came, lead = 383)))
        assertEquals(Answer.OPEN_HELPER, FirstRun.entered(k1, FirstRun.armed(k1), arrived = FirstRun.inView(came + 383 + 350, opened = 0, came = came, lead = 383)))
    }

    /** Through a whole run, whatever else is pressed or left undone, suggestions are on only after Agree. */
    @Test fun nothingButAgreeSwitchesSuggestionsOnThroughAWholeRun() {
        var s = FirstRun.answer(FirstRun.keyLanded(State(Run.NEW)), Answer.GO_ON)
        for (enter in listOf(Enter.PRACTICE_OPEN, Enter.PRACTICE_SEARCH, Enter.COPY_STAYS)) s = FirstRun.ran(s, enter)
        assertEquals(Screen.Q, FirstRun.screen(s))
        assertFalse(s.suggestions)
        // Enter at rest, and Tab and Enter pressed blind: no answer at all.
        assertNull(FirstRun.entered(s, FirstRun.armed(s), arrived = true))
        assertNull(FirstRun.entered(s, FirstRun.tabbed(s, FirstRun.armed(s), back = false, arrived = false), arrived = false))
        // An answer the question does not offer changes nothing.
        for (a in Answer.entries - Answer.AGREE - Answer.NOT_NOW) assertEquals(s, FirstRun.answer(s, a))
        // Left unanswered in three openings, the run waits: nothing was answered for the user.
        var left = s
        repeat(FirstRun.MAX_OPENS + 1) { left = FirstRun.opening(left, 1200f, plain = true) }
        assertNull(FirstRun.stage(left, 1200f))
        assertFalse(left.suggestions)
        // Not now, and then the choices left: still off.
        val no = FirstRun.answer(s, Answer.NOT_NOW)
        assertEquals(Screen.C, FirstRun.screen(no))
        assertFalse(FirstRun.answer(no, Answer.DONE).suggestions)
        assertTrue(FirstRun.answer(s, Answer.AGREE).suggestions)
    }

    /** "Agree" is the question's answer alone: given on any other screen, by whatever slip, it switches nothing on. */
    @Test fun agreeOnAScreenThatDoesNotOfferItSwitchesNothingOn() {
        val k4 = FirstRun.keyLanded(k1)
        assertEquals(Screen.K4, FirstRun.screen(k4))
        for (s in listOf(k1, k4, l2, l3, l4, c)) assertFalse(FirstRun.answer(s, Answer.AGREE).suggestions)
    }

    /** Every screen is counted, not only the key's: an unfinished run stands in three openings wherever it stopped, then waits with what was done kept. */
    @Test fun aRunPastTheKeyStandsInThreeOpeningsThenWaits() {
        for (start in listOf(l2, l3, l4, q, c)) {
            var s = start
            repeat(FirstRun.MAX_OPENS) {
                s = FirstRun.opening(s, 1200f, plain = true)
                assertEquals(FirstRun.screen(start), FirstRun.stage(s, 1200f))
            }
            s = FirstRun.opening(s, 1200f, plain = true)
            assertNull(FirstRun.stage(s, 1200f))
            assertEquals(start.done, s.done)
            assertEquals(start.run, s.run)
        }
    }

    /** One opening for the whole of it: the lessons, the question and the choices follow one another in the panel that is open, and none of them is counted again. */
    @Test fun whatFollowsInTheSamePanelIsNotCountedAgain() {
        var s = FirstRun.opening(l2, 1200f, plain = true)
        for (enter in listOf(Enter.PRACTICE_OPEN, Enter.PRACTICE_SEARCH, Enter.COPY_STAYS)) s = FirstRun.ran(s, enter)
        s = FirstRun.answer(s, Answer.NOT_NOW)
        assertEquals(Screen.C, FirstRun.stage(s, 1200f))
        assertEquals(1, s.opens)
        // The choices left, the run is over: nothing of it is kept but that the key's step has ended here.
        assertEquals(State(key = true, mark = true), FirstRun.answer(s, Answer.DONE))
    }
}
