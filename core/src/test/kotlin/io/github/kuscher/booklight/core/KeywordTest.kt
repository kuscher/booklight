package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An event's line under the keyword `event`, and a line typed without it: what the released keyword read, this
 * build reads at least as well; and a line without the keyword that begins like an everyday search is offered
 * with more care and never saved from the list. "Now" is Tuesday 6 October 2026, ten in the morning.
 */
class KeywordTest {
    private val now = LocalDateTime.of(2026, 10, 6, 10, 0)
    private fun at(day: Int, hour: Int = 0, minute: Int = 0, month: Int = 10, year: Int = 2026) = LocalDateTime.of(year, month, day, hour, minute)

    private val sam = Cal(1, "Sam", owner = "sam@example.com", primary = true)
    private val team = Cal(2, "Team")
    private val mine = listOf(sam, team)

    /** Under the keyword `event`. */
    private fun k(text: String) = Sentence.read(text, now, mine, keyword = true)
    /** Typed without it. */
    private fun r(text: String) = Sentence.read(text, now, mine)

    /** A line under the keyword, and what 3.1 read of it: its title, its start and its end. */
    private class Row(val text: String, val title: String, val start: LocalDateTime, val end: LocalDateTime)

    /**
     * 3.1's own readings (its core, run with this "now"): two bare numbers at one end of the line are the time of
     * the day at its other end.
     */
    private val ranges = listOf(
        Row("9-10 standup tomorrow", "standup", at(7, 9), at(7, 10)),
        Row("9-10 standup Friday", "standup", at(9, 9), at(9, 10)),
        Row("9-10 standup on Friday", "standup", at(9, 9), at(9, 10)),
        Row("9-10 Standup morgen", "Standup", at(7, 9), at(7, 10)),
        Row("9-10 standup 14.10.", "standup", at(14, 9), at(14, 10)),
        Row("2-3 meet Friday", "meet", at(9, 14), at(9, 15)),
        Row("tomorrow standup 9-10", "standup", at(7, 9), at(7, 10)),
        Row("Friday standup 9-10", "standup", at(9, 9), at(9, 10)),
        Row("tomorrow lunch 12-1", "lunch", at(7, 12), at(7, 13)),
        Row("morgen Mittagessen 12-13", "Mittagessen", at(7, 12), at(7, 13)),
        // And what it read with the two beside each other, or with nothing else in the line, it still reads.
        Row("standup tomorrow 9-10", "standup", at(7, 9), at(7, 10)),
        Row("standup 9-10 tomorrow", "standup", at(7, 9), at(7, 10)),
        Row("1-2 eggs tomorrow", "eggs", at(7, 13), at(7, 14)),
    )

    @Test fun twoBareNumbersAtAnEndAreTheTimeUnderTheKeyword() {
        for (row in ranges) for (got in listOf(k(row.text).draft, Jot.event(row.text, now))) {
            assertEquals(row.text, row.title, got.title)
            assertEquals(row.text, row.start, got.start)
            assertEquals(row.text, row.end, got.end)
            assertFalse(row.text, got.allDay)
            assertTrue(row.text, got.dayGiven && got.timeGiven)
            // Which half of the day was not said: read as ever, and a guess.
            assertTrue(row.text, got.vague)
            assertFalse(row.text, got.sure)
        }
    }

    @Test fun aDayAWordAndAnHourAreTheHourAsIn31() {
        // 3.1 read the hour of these lines and left the day's words in the title; this build reads the day as well. Neither
        // reads a range of days up to that day of the month.
        for ((text, start) in listOf("Schicht morgen bis 9 Uhr" to at(7, 9), "shift tomorrow until 9 Uhr" to at(7, 9), "Schicht Freitag bis 18 Uhr" to at(9, 18), "shift Friday to 18 Uhr" to at(9, 18),
            "shift tomorrow through 9 Uhr" to at(7, 9), "shift today – 20 Uhr" to at(6, 20), "shift tomorrow till 9 pm" to at(7, 21)))
            for (got in listOf(k(text).draft, Jot.event(text, now))) {
                assertEquals(text, start, got.start)
                assertEquals(text, start.plusHours(1), got.end)
                assertFalse(text, got.allDay)
                assertTrue(text, got.dayGiven && got.timeGiven)
            }
    }

    @Test fun aFullStopAtTheLinesEndIsReadAsWithoutIt() {
        // With the keyword and without: "… at 7pm." is seven in the evening, and saved as the same line without its stop is.
        for (t in listOf("Add dinner tomorrow at 7pm.", "Add dinner with Sam tomorrow at 7pm!", "Trag Zahnarzt morgen um 9 Uhr ein.", "Add dinner tomorrow at 7pm to Team.")) {
            val plain = t.trimEnd('.', '!')
            assertEquals(t, r(plain).draft, r(t).draft)
            assertEquals(t, r(plain).calendar, r(t).calendar)
            assertTrue(t, Cals.saves(r(t), true, Cals.target(r(t).calendar, mine, "")))
        }
        // (And where the line's own words end, they end: the minutes after „Uhr“ have no word after them here.)
        for (t in listOf("Trag Zahnarzt morgen um 15 Uhr 30 ein", "Add Zahnarzt morgen 15 Uhr 30 to Team", "Add Zahnarzt morgen 15 Uhr 30 @ Praxis")) assertTrue(t, r(t).draft.sure)
        assertEquals(k("Dentist tomorrow 3pm").draft, k("Dentist tomorrow 3pm.").draft)
        assertEquals(Jot.event("Dentist tomorrow 3pm", now), Jot.event("Dentist tomorrow 3pm.", now))
        assertEquals(at(7, 15), k("Dentist tomorrow 3pm.").draft.start)
    }

    @Test fun withoutTheKeywordTwoBareNumbersAreNoTime() {
        // "add 1-2 eggs tomorrow" is a line anyone may type: the day is read, and the numbers stay.
        for ((text, title) in listOf("add 1-2 eggs tomorrow" to "1-2 eggs", "add 9-10 standup tomorrow" to "9-10 standup", "add standup tomorrow, room 9-10" to "standup room 9-10", "9-10 standup tomorrow" to "9-10 standup")) {
            val got = r(text).draft
            assertEquals(text, title, got.title)
            assertEquals(text, at(7), got.start)
            assertTrue(text, got.allDay)
            assertFalse(text, got.timeGiven)
        }
        // Beside their day they are its time with the keyword and without, as ever.
        assertEquals(at(7, 9), r("add standup tomorrow 9-10").draft.start)
    }

    @Test fun underTheKeywordNoCueIsDroppedButTheTwoWordOnes() {
        // The line after `event` is the event: its first word is the title's, as it was in 3.1.
        for (row in listOf(Row("put out bins thu 7am", "put out bins", at(8, 7), at(8, 8)), Row("Freitag 9 Uhr Stunden eintragen", "Stunden eintragen", at(9, 9), at(9, 10)),
            Row("Add dinner tomorrow 7pm", "Add dinner", at(7, 19), at(7, 20)), Row("schedule review Friday 3pm", "schedule review", at(9, 15), at(9, 16)),
            Row("Trag Essen morgen um 12 Uhr ein", "Trag Essen ein", at(7, 12), at(7, 13)), Row("book club Friday 7pm", "book club", at(9, 19), at(9, 20)))) {
            val got = k(row.text)
            assertEquals(row.text, row.title, got.draft.title)
            assertEquals(row.text, row.start, got.draft.start)
            assertEquals(row.text, row.end, got.draft.end)
            assertFalse(row.text, got.everyday)
            // With the switch on such a line is saved: the keyword says it is an event. (But „Stunden“, left in a title, is a word
            // of time nobody read: that line opens the editor.)
            assertEquals(row.text, row.title != "Stunden eintragen", got.draft.sure)
            assertEquals(row.text, got.draft.sure, Cals.saves(got, true, sam))
        }
        val deadline = k("Schedule C deadline Apr 15").draft
        assertEquals("Schedule C deadline", deadline.title)
        assertEquals(at(15, month = 4, year = 2027), deadline.start)
        assertTrue(deadline.allDay)
        // "New event" and „Neuer Termin“ say nothing the keyword has not said: dropped there too.
        assertEquals("dinner", k("New event dinner tomorrow 7pm").draft.title)
        assertEquals("Zahnarzt", k("Neuer Termin Zahnarzt morgen 15 Uhr").draft.title)
        // A calendar is read under the keyword as without it, and written into the line the same way.
        assertEquals(team, k("put out bins thu 7am to Team").calendar)
        assertEquals("put out bins", k("put out bins thu 7am to Team").draft.title)
        assertEquals("Trag Essen morgen um 12 Uhr ein to Team", Sentence.put("Trag Essen morgen um 12 Uhr ein", "Team", "to", now, mine, keyword = true))
        assertEquals("Trag Essen morgen um 12 Uhr to Team ein", Sentence.put("Trag Essen morgen um 12 Uhr ein", "Team", "to", now, mine))
    }

    @Test fun aLineThatBeginsLikeAnEverydaySearchNeedsADayAndATime() {
        // Not offered: a day alone, a time alone, a range of days. Each is as often a search.
        for (t in listOf("plane crash today", "plane tickets to denver friday", "book the midnight library", "book of mormon tickets saturday", "put options expiry friday", "schedule nfl sunday",
            "plan trip to rome oct 14 to oct 16", "book flights to boston friday", "schedule 1 drugs tomorrow", "plan b 9am", "Schedule dentist Friday", "Plan offsite Oct 14 to Oct 16", "Plane Offsite am 14.10.",
            "Plan review tomorrow", "book club 9am", "Plane Offsite morgen", "put milk in fridge tomorrow all day", "book hotel Friday all day"))
            assertFalse(t, Sentence.reads(r(t)))
        // „plane“ is German's cue alone: where the day and the time were said in English, it is the thing that flies.
        for (t in listOf("plane landing tomorrow 3pm", "plane spotting Friday at 9am", "plane tickets Oct 14 9am")) { assertFalse(t, Sentence.reads(r(t))); assertFalse(t, r(t).cue) }
        // Still offered: with a day and a time.
        for (t in listOf("Schedule dentist appointment next Tuesday at 3:30pm at Dr. Sam office", "Put yoga class every Monday 6pm in my Team calendar", "plane Treffen morgen um 15 Uhr", "Plane Offsite am 14.10. um 9 Uhr",
            "Book dentist tomorrow 3pm", "book flight to boston friday 9am", "plan review Friday 10am", "put yoga Friday 6pm", "Plan Besprechung morgen um 15 Uhr", "schedule call tomorrow at 7")) {
            assertTrue(t, Sentence.reads(r(t)))
            assertTrue(t, r(t).everyday)
        }
        // "add", "new event" and their German forms need a day or a time, as ever, and are no everyday search.
        for (t in listOf("Add dinner with Sam tomorrow at 7pm", "add dinner 7pm", "add milk tomorrow", "Add lunch on sat", "add call 2-3pm", "add dinner at 7", "add 1-2 eggs tomorrow", "Trag Mittagessen am Freitag um 12 ein",
            "Neuer Termin Zahnarzt morgen", "New event dinner Friday", "add 2 hours to 5pm")) {
            assertTrue(t, Sentence.reads(r(t)))
            assertFalse(t, r(t).everyday)
        }
    }

    @Test fun aLineThatBeginsLikeAnEverydaySearchIsNeverSavedFromTheList() {
        // Sure of its day and its time, and all the same never saved by Enter: typed fast, it was as likely meant for the web.
        for (t in listOf("book flight to boston friday 9am", "Schedule dentist appointment Friday at 3:30pm", "put yoga Friday 6pm to Team", "plan review Friday 10am", "plane Treffen morgen um 15 Uhr")) {
            val got = r(t)
            assertTrue(t, got.draft.sure)
            assertTrue(t, got.everyday)
            assertFalse(t, Cals.saves(got, true, Cals.target(got.calendar, mine, "")))
            // Under the keyword the same words are an event that was asked for.
            assertTrue(t, Cals.saves(k(t), true, sam))
        }
        // "add …" is saved as ever.
        assertTrue(Cals.saves(r("Add dinner with Sam tomorrow at 7pm"), true, sam))
        assertTrue(Cals.saves(r("Neuer Termin Zahnarzt morgen 15 Uhr"), true, sam))
    }

    @Test fun theModelsAnswerNeverMakesSuchALineOneThatIsSaved() {
        val text = "book flight to boston friday 9am with Sam"
        val own = r(text)
        assertTrue(own.everyday && own.loose && own.draft.sure)
        val got = Splits.merge(own, "{\"title\":\"book flight to boston\",\"when\":\"friday 9am\",\"place\":\"\",\"calendar\":\"\"}", text, now, mine)
        assertSame(own, got)
        val split = Splits.merge(own, "{\"title\":\"book flight to boston\",\"when\":\"friday 9am\",\"place\":\"with Sam\",\"calendar\":\"\"}", text, now, mine)
        assertEquals("book flight to boston", split.draft.title)
        assertTrue(split.everyday)
        assertFalse(Cals.saves(split, true, sam))
    }

    @Test fun underTheKeywordTheModelLosesNoWordTheRulesKeep() {
        // "Add" is the title's under the keyword: an answer that leaves it out has lost a word, and the rules' reading stands.
        val text = "Add dinner tomorrow 7pm at Cafe Luna"
        val own = k(text)
        assertEquals("Add dinner at Cafe Luna", own.draft.title)
        val lost = "{\"title\":\"dinner\",\"when\":\"tomorrow 7pm\",\"place\":\"Cafe Luna\",\"calendar\":\"\"}"
        assertSame(own, Splits.merge(own, lost, text, now, mine, keyword = true))
        val kept = Splits.merge(own, "{\"title\":\"Add dinner\",\"when\":\"tomorrow 7pm\",\"place\":\"Cafe Luna\",\"calendar\":\"\"}", text, now, mine, keyword = true)
        assertEquals("Add dinner", kept.draft.title)
        assertEquals("Cafe Luna", kept.draft.place)
        // Without the keyword the same answer is taken, the cue dropped by the rules' own rule.
        val free = Splits.merge(r(text), lost, text, now, mine)
        assertEquals("dinner", free.draft.title)
        assertEquals("Cafe Luna", free.draft.place)
        assertNull(free.calendar)
    }
}
