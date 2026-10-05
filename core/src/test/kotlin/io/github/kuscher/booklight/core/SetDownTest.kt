package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.SetDown.Comes
import io.github.kuscher.booklight.core.SetDown.THERE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** When each part of first run's stage comes (docs/design/first-run/motion.md §3): the paper's own tables, with the paper's own times. */
class SetDownTest {
    /** The paper's times: a part 22 ms after the one before, a beat of 66, a letter 40. */
    private val pace = SetDown.Pace(
        part = 22, beat = 66, letter = 40, band = 120, turn = 120, afterList = 60, press = 80,
        askText = 66, askNote = 120, askAnswers = 142,
        landsDisc = 80, landsWords = 150, landsCounter = 190, landsArmed = 180, landsBand = 260, rest = 170,
    )
    /** The band's pieces, as how many letters each typed part has (0: a cap or a sign): the suggested keys, "your key", and the three recipes. */
    private val caps = listOf(0, 0, 0)
    private val yours = listOf(0)
    private val l2 = listOf(4, 0)
    private val l3 = listOf(4, 0, 0, 4, 0)
    private val l4 = listOf(9, 0)

    private fun marks(comes: Comes, letters: List<Int> = caps, ask: Boolean = false, caption: Boolean = true, bandAfter: Int = pace.band, bandChanges: Boolean = true, captionChanges: Boolean = true, answersChange: Boolean = true) =
        SetDown.marks(comes, pace, letters, ask, caption, bandAfter, bandChanges, captionChanges, answersChange)

    /** T1: the key's step at the gate of a first opening at Medium, where the band begins 163 ms after the gate. */
    @Test fun theKeysStepIsSetDownAsThePaperHasIt() {
        val m = marks(Comes.SET_DOWN, caps, bandAfter = 163)
        assertEquals(listOf(0, 22, 44), listOf(m.disc, m.words, m.counter))
        // `Action`, "+", `Quick Insert`: B, B + 66, B + 132. The caption at B + 198, the answers at B + 220, at rest B + 390.
        assertEquals(listOf(163, 229, 295), m.pieces)
        assertEquals(163 + 198, m.caption)
        assertEquals(163 + 220, m.answers)
        assertEquals(163 + 390, m.end)
        // The armed answer comes with the others, and that is where the screen's keys begin to count from.
        assertEquals(m.answers, m.armed)
        assertEquals(383, m.lead)
        assertTrue(m.rises)
        // At Fast, and with the opening switched off, the band is 120 ms after the gate.
        assertEquals(listOf(120, 186, 252), marks(Comes.SET_DOWN, caps).pieces)
    }

    /**
     * T5: a recipe is written at one pace: a letter 40 ms after the one before, a cap or a new typed part a beat after the
     * piece before it. Every mark is the paper's own figure. Its "at rest" column is rounded (its rest follows Skip by
     * 171, 168 and 171 ms where one figure, 170, is meant): that column is held to the paper's within 3 ms.
     */
    @Test fun aLessonsRecipeIsWrittenAsThePaperHasIt() {
        fun written(letters: List<Int>) = marks(Comes.SET_DOWN, letters, bandAfter = 0)
        fun rests(paper: Int, m: SetDown.Marks) = assertEquals(paper.toDouble(), m.end.toDouble(), 3.0)
        // `yout` `⏎`: letters 0 to 120, the cap at 186; the caption 252, Skip 274; at rest 445.
        written(l2).let { assertEquals(listOf(0, 186), it.pieces); assertEquals(252, it.caption); assertEquals(274, it.answers); rests(445, it) }
        // `yout` `Tab` `⏎` `lofi` `⏎`: 0 to 120, 186, 252, 318 to 438, 504; 570 and 592; at rest 760.
        written(l3).let { assertEquals(listOf(0, 186, 252, 318, 504), it.pieces); assertEquals(570, it.caption); assertEquals(592, it.answers); rests(760, it) }
        // `150 + 20%` `⏎`: 0 to 320, 386; 452 and 474; at rest 645.
        written(l4).let { assertEquals(listOf(0, 386), it.pieces); assertEquals(452, it.caption); assertEquals(474, it.answers); rests(645, it) }
        // (The code's own rule, which the paper's column rounds: a stage is at rest 170 ms after its last part began.)
        for (letters in listOf(l2, l3, l4)) written(letters).let { assertEquals(it.answers + pace.rest, it.end) }
        // A typed part's letters follow its mark 40 ms apart.
        assertEquals(listOf(318, 358, 398, 438), (0 until 4).map { SetDown.letter(written(l3), pace, 3, it) })
        // A lesson whose example is not worked out yet has no recipe: its caption and Skip do not wait for one.
        written(emptyList()).let { assertEquals(emptyList<Int>(), it.pieces); assertEquals(0, it.caption); assertEquals(22, it.answers) }
    }

    /** "Your key works" in a panel the keys have just opened: the key comes up at B, "Go on" at B + 66. There is no caption. */
    @Test fun aScreenWithoutACaptionHasItsAnswersABeatAfterTheBand() {
        val m = marks(Comes.SET_DOWN, yours, caption = false, bandAfter = 163)
        assertEquals(listOf(163), m.pieces)
        assertEquals(THERE, m.caption)
        assertEquals(163 + 66, m.answers)
    }

    /** A screen come back to is not set down again at that pace: its parts arrive as rows do, 22 ms apart, and nothing is written twice. */
    @Test fun aScreenComeBackToArrivesAsRowsDo() {
        for (letters in listOf(caps, yours, l2, l3, l4)) {
            val m = marks(Comes.BACK, letters, bandAfter = 163)
            assertEquals(listOf(0, 22, 44), listOf(m.disc, m.words, m.counter))
            assertEquals(List(letters.size) { 66 }, m.pieces)
            assertEquals(88, m.caption)
            assertEquals(110, m.answers)
            // Its typed parts stand whole: every letter with its piece.
            for (k in letters.indices) for (i in 0 until letters[k]) assertEquals(66, SetDown.letter(m, pace, k, i))
        }
    }

    /** T5, in the same panel: the seat stays and its words roll; the new band is written from 120 ms; what stays does not move. */
    @Test fun aScreenThatTakesAnothersPlaceMovesOnlyWhatChanges() {
        // "Go on" on "Your key works": lesson 2's recipe is written from 120, then its caption, then Skip in place of "Go on".
        val goOn = marks(Comes.TURN, l2)
        assertEquals(listOf(THERE, THERE, THERE), listOf(goOn.disc, goOn.words, goOn.counter))
        assertEquals(listOf(120, 306), goOn.pieces)
        assertEquals(372, goOn.caption)
        assertEquals(394, goOn.answers)
        // Skip on a lesson: the next lesson's Skip is the same answer, and its caption says the same: neither moves.
        val skip = marks(Comes.TURN, l3, captionChanges = false, answersChange = false)
        assertEquals(THERE, skip.answers)
        assertEquals(THERE, skip.caption)
        assertEquals(0, skip.lead)
        assertEquals(listOf(120, 306, 372, 438, 624), skip.pieces)
        // From lesson 3 to the sum's lesson the caption says something else: it comes once the recipe is written.
        assertEquals(120 + 386 + 66, marks(Comes.TURN, l4, answersChange = false).caption)
        // The key lands with its screen in view: "your key" stays where it is, and "Go on" comes once the old answers have faded.
        val lands = marks(Comes.TURN, yours, caption = false, bandChanges = false)
        assertEquals(listOf(THERE), lands.pieces)
        assertEquals(80, lands.answers)
        assertEquals(80, lands.lead)
        // Back from the system's dialog: the caps give way to "your key", the caption changes after it, the two answers stay.
        val back = marks(Comes.TURN, yours, answersChange = false)
        assertEquals(listOf(120), back.pieces)
        assertEquals(186, back.caption)
        assertEquals(THERE, back.answers)
    }

    /** T8, and a lesson after a lesson's Enter: a stage that follows a list is set down 60 ms after the list gave way. */
    @Test fun afterAListTheStageWaitsForItsRowsToFade() {
        val m = marks(Comes.AFTER_LIST, l3)
        assertEquals(listOf(60, 82, 104), listOf(m.disc, m.words, m.counter))
        assertEquals(126, m.pieces.first())
        assertEquals(marks(Comes.SET_DOWN, l3, bandAfter = 66).pieces.map { it + 60 }, m.pieces)
        assertEquals(marks(Comes.SET_DOWN, l3, bandAfter = 66).answers + 60, m.answers)
    }

    /** The question's parts in reading order (T8: 580, 602, 624, 646, 700 and 722 for a list that gave way at 520). */
    @Test fun theQuestionComesInReadingOrder() {
        val after = marks(Comes.AFTER_LIST, emptyList(), ask = true, caption = false)
        assertEquals(listOf(580, 602, 624, 646, 700, 722), listOf(after.disc, after.words, after.counter, after.pieces[0], after.pieces[1], after.answers).map { it + 520 })
        assertEquals(722 - 520 + 170, after.end)
        // Come back to at a gate: the same offsets from the gate. Nothing of it is armed: its keys count from its answers.
        for (comes in listOf(Comes.SET_DOWN, Comes.BACK)) {
            val m = marks(comes, emptyList(), ask = true, caption = false, bandAfter = 163)
            assertEquals(listOf(0, 22, 44, 66, 120, 142), listOf(m.disc, m.words, m.counter, m.pieces[0], m.pieces[1], m.answers))
            assertEquals(142, m.lead)
        }
        // Where it takes a lesson's place (Skip): its text once the recipe has faded, then as above.
        val turn = marks(Comes.TURN, emptyList(), ask = true, caption = false)
        assertEquals(listOf(THERE, 120, 174, 196), listOf(turn.disc, turn.pieces[0], turn.pieces[1], turn.answers))
        assertEquals(196, turn.lead)
    }

    /** B5: the opening piece lands. The highlight travels into the armed answer, whose words fade in inside it at T + 180; the rest follows. */
    @Test fun atALandingTheArmedAnswerComesInsideTheHighlight() {
        val m = marks(Comes.LANDS, caps)
        assertEquals(listOf(80, 150, 190), listOf(m.disc, m.words, m.counter))
        assertEquals(180, m.armed)
        assertEquals(listOf(260, 326, 392), m.pieces)
        assertEquals(458, m.caption)
        assertEquals(480, m.answers)
        assertEquals(180, m.lead)
        // The answers do not rise there: the highlight is on its way to where they stand.
        assertFalse(m.rises)
        // A replay lands in "Your key works": the key at 260, "Change the key" a beat later.
        val again = marks(Comes.LANDS, yours, caption = false)
        assertEquals(listOf(260), again.pieces)
        assertEquals(326, again.answers)
        assertEquals(180, again.armed)
    }

    /**
     * Enter runs the armed answer, and counts from when that one began to show ([SetDown.Marks.lead]). Tab, the pointer
     * and a click reach any answer: they count from when every answer has begun to show ([SetDown.Marks.all]), and so
     * does Enter once Tab has moved the arming. The two marks differ at a landing alone, where the armed answer's words
     * come inside the highlight 300 ms before the other answer.
     */
    @Test fun aKeyThatCanReachAnyAnswerCountsFromTheLastOfThem() {
        val lands = marks(Comes.LANDS, caps)
        assertEquals(180, lands.lead)
        assertEquals(480, lands.all)
        assertEquals(180, SetDown.since(lands, enter = true, moved = false))
        assertEquals(480, SetDown.since(lands, enter = false, moved = false))
        assertEquals(480, SetDown.since(lands, enter = true, moved = true))
        assertEquals(480, SetDown.since(lands, enter = false, moved = true))
        // A replay lands in "Your key works": "Change the key" comes at 326, and nothing reaches it sooner.
        assertEquals(326, marks(Comes.LANDS, yours, caption = false).all)
        // The question at a landing arms nothing: both answers come at one mark, and every key counts from it.
        marks(Comes.LANDS, emptyList(), ask = true, caption = false).let { assertEquals(it.answers, it.lead); assertEquals(it.answers, it.all) }
        // In every way a screen can come: never sooner than the armed answer's own mark, and never before every answer has begun.
        for (comes in Comes.entries) for (letters in listOf(caps, yours, l2, l3, l4, emptyList())) for (ask in listOf(false, true))
            for (caption in listOf(true, false)) for (answers in listOf(true, false)) {
                val m = marks(comes, letters, ask, caption, answersChange = answers)
                val name = "$comes $letters ask=$ask caption=$caption answers=$answers"
                assertEquals(name, maxOf(0, m.armed, m.answers), m.all)
                assertTrue(name, m.all >= m.lead)
                if (comes != Comes.LANDS) assertEquals(name, m.lead, m.all)
                for (enter in listOf(true, false)) for (moved in listOf(true, false))
                    assertEquals(name, if (enter && !moved) m.lead else m.all, SetDown.since(m, enter, moved))
            }
    }

    /**
     * At the landing of the opening piece the keys of someone hurrying past the show, Tab and then Enter, reach "Not
     * now" only once it has been in view for 350 ms: not 350 ms after the armed answer began to show, which is 50 ms
     * after "Not now" did. Enter alone, on the answer that came armed, counts from that answer's own mark.
     */
    @Test fun tabThenEnterAtALandingReachNothingThatHasNotBeenInView() {
        val k1 = FirstRun.State(FirstRun.Run.NEW)
        val m = marks(Comes.LANDS, caps)
        val came = 10_000L
        fun seen(press: Long, enter: Boolean, moved: Boolean) = FirstRun.inView(press, opened = 0, came = came, lead = SetDown.since(m, enter, moved).toLong())
        // 530 ms in: Enter is the armed answer's; Tab moves nothing, so Enter after it is still the armed answer's.
        val early = came + 180 + 350
        assertEquals(FirstRun.Answer.OPEN_HELPER, FirstRun.entered(k1, FirstRun.armed(k1), seen(early, enter = true, moved = false)))
        assertEquals(FirstRun.armed(k1), FirstRun.tabbed(k1, FirstRun.armed(k1), back = false, arrived = seen(early, enter = false, moved = false)))
        // Until 350 ms after "Not now" began to show (480): Tab arms nothing else.
        assertEquals(0, FirstRun.tabbed(k1, 0, back = false, arrived = seen(came + 480 + 349, enter = false, moved = false)))
        val tabbed = FirstRun.tabbed(k1, 0, back = false, arrived = seen(came + 480 + 350, enter = false, moved = false))
        assertEquals(1, tabbed)
        assertEquals(FirstRun.Answer.NOT_NOW, FirstRun.entered(k1, tabbed, seen(came + 480 + 350, enter = true, moved = true)))
        // And an arming that was moved never counts by the sooner mark, whatever moved it.
        assertNull(FirstRun.entered(k1, 1, seen(came + 480 + 349, enter = true, moved = true)))
    }

    /** A pace that would put a part before its screen came, or the question's answers before its text, is no pace: it is refused where it is made. */
    @Test fun aPaceThatRunsBackwardsIsRefused() {
        fun refused(make: () -> SetDown.Pace) = assertTrue(runCatching(make).exceptionOrNull() is IllegalArgumentException)
        refused { pace.copy(part = -1) }
        refused { pace.copy(rest = -1) }
        // The question's text comes after its seat, where it takes a lesson's place and at a landing: never before the screen came.
        refused { pace.copy(turn = 60) }
        refused { pace.copy(landsBand = 60) }
        // Its parts come in reading order, the answers last.
        refused { pace.copy(askNote = 60) }
        refused { pace.copy(askAnswers = 100) }
        // At a landing the armed answer's words come inside the highlight, before the band.
        refused { pace.copy(landsArmed = 300) }
        // (The paper's own pace is one.)
        assertEquals(pace, pace.copy())
    }

    /** A screen that stands whole (the field emptied again, animations off): nothing comes, everything is there. */
    @Test fun aScreenThatStandsWholeHasNothingToCome() {
        for (letters in listOf(caps, l3, emptyList())) for (ask in listOf(true, false)) {
            val m = marks(Comes.WHOLE, letters, ask = ask)
            assertEquals(SetDown.WHOLE, m)
            assertEquals(0, m.end)
            assertEquals(0, m.lead)
            assertEquals(THERE, SetDown.piece(m, 3))
        }
    }

    /**
     * Whatever comes, in every way a screen can come: the answers are the last of its parts to begin (but for the
     * armed one's words at a landing), nothing begins after the end, and the lead is when the answer Enter would run
     * begins to show, never before the screen came.
     */
    @Test fun theAnswersComeLastAndTheLeadIsTheArmedAnswers() {
        for (comes in Comes.entries) for (letters in listOf(caps, yours, l2, l3, l4, emptyList())) for (ask in listOf(false, true))
            for (caption in listOf(true, false)) for (band in listOf(true, false)) for (answers in listOf(true, false)) for (after in listOf(120, 163, 324)) {
                val m = marks(comes, letters, ask, caption, after, band, captionChanges = band, answersChange = answers)
                val name = "$comes $letters ask=$ask caption=$caption band=$band answers=$answers"
                val parts = listOf(m.disc, m.words, m.counter, m.caption) + m.pieces
                if (m.answers != THERE) assertTrue(name, parts.all { it <= m.answers })
                assertTrue(name, (parts + m.answers + m.armed).all { it == THERE || it in 0..m.end })
                assertEquals(name, maxOf(0, m.armed), m.lead)
                if (comes != Comes.LANDS) assertEquals(name, m.answers, m.armed)
                // A piece asked for that the band does not have is the last one's, never a crash.
                assertEquals(name, m.pieces.lastOrNull() ?: THERE, SetDown.piece(m, 99))
            }
    }

    /** Which screens share a band: the key's last three are all "your key"; every other screen has its own. */
    @Test fun whichScreensShareABand() {
        for (a in Screen.entries) for (b in Screen.entries) {
            val yours = setOf(Screen.K2, Screen.K3, Screen.K4)
            assertEquals("$a $b", a == b || (a in yours && b in yours), SetDown.sameBand(a, b))
        }
        assertFalse(SetDown.sameBand(null, Screen.K1))
    }

    /** Which screens say the same under their band: lessons 2 and 3; and the key's first and third screen where no other key is left to try. */
    @Test fun whichScreensShareACaption() {
        val quick = FirstRun.Key.QUICK_INSERT
        assertTrue(SetDown.sameCaption(Screen.L2, Screen.L3, quick))
        assertFalse(SetDown.sameCaption(Screen.L3, Screen.L4, quick))
        assertFalse(SetDown.sameCaption(Screen.K4, Screen.L2, quick))
        // With Quick Insert suggested every screen of the key's step says something else; "Your key works" and the question say nothing.
        for (a in listOf(Screen.K1, Screen.K2, Screen.K3, Screen.K4)) for (b in listOf(Screen.K1, Screen.K2, Screen.K3, Screen.K4)) assertEquals("$a $b", a == b, SetDown.sameCaption(a, b, quick))
        // With Action + M suggested there is no other key to try: the third screen says what the first said.
        assertTrue(SetDown.sameCaption(Screen.K1, Screen.K3, FirstRun.Key.M))
        assertFalse(SetDown.sameCaption(Screen.K1, Screen.K2, FirstRun.Key.M))
        assertTrue(SetDown.sameCaption(Screen.K4, Screen.Q, quick))
        assertFalse(SetDown.sameCaption(null, Screen.K1, quick))
        assertTrue(SetDown.sameCaption(null, Screen.K4, quick))
    }
}
