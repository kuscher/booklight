package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What of a day or a time is a guess, and so never saved without the calendar's editor ([EventDraft.sure], [Doubt]):
 * each reading that was the parser's choice and not what was typed. "Now" is Tuesday 6 October 2026, ten in the morning.
 */
class GuessTest {
    private val now = LocalDateTime.of(2026, 10, 6, 10, 0)
    private fun at(day: Int, hour: Int = 0, minute: Int = 0, month: Int = 10, year: Int = 2026) = LocalDateTime.of(year, month, day, hour, minute)
    private fun e(text: String, at: LocalDateTime = now) = Jot.event(text, at)

    /** Each line is read as a day and a time, and is a guess for [doubt] and for nothing else. */
    private fun guesses(doubt: Doubt, vararg lines: String) {
        for (t in lines) {
            val got = e(t)
            assertTrue(t, got.dayGiven && got.timeGiven)
            assertEquals(t, setOf(doubt), got.doubts)
            assertFalse(t, got.sure)
        }
    }

    private fun sure(vararg lines: String) {
        for (t in lines) { assertEquals(t, emptySet<Doubt>(), e(t).doubts); assertTrue(t, e(t).sure) }
    }

    @Test fun aDateWithoutAYearThatHadPassedIsAGuess() {
        // Yesterday's date, with no year: the next one is a year away, and whether that was meant is a guess.
        val dentist = e("dentist Oct 5 3pm")
        assertEquals(at(5, 15, year = 2027), dentist.start)
        assertEquals("dentist", dentist.title)
        assertEquals(setOf(Doubt.PAST), dentist.doubts)
        assertFalse(dentist.sure)
        guesses(Doubt.PAST, "dentist 5.10. 15:00", "dentist 5 Oct 3pm", "standup Sep 10 9am", "offsite Oct 5 all day", "trip Oct 1 to Oct 3", "Messe vom 1. bis 3. Oktober")
        assertEquals(at(29, 20, month = 2, year = 2028), e("party Feb 29 8pm").start)
        assertEquals(setOf(Doubt.PAST), e("party Feb 29 8pm").doubts)
        // A date that is still ahead, in this year or the next, is the date that was meant: three hundred days at the most.
        assertEquals(at(15, 9, month = 1, year = 2027), e("review Jan 15 9am").start)
        assertEquals(at(2, 9, month = 8, year = 2027), e("review Aug 2 9am").start)
        sure("review Jan 15 9am", "review Aug 2 9am", "dentist Oct 6 3pm", "dentist Oct 7 3pm", "dentist on the 5th at 3pm", "trip Oct 7 to Oct 9", "review Dec 30 to Jan 2")
        assertEquals(setOf(Doubt.PAST), e("review Aug 3 9am").doubts)
        // A year that was typed is as typed, behind us or far ahead.
        assertEquals(at(3, 15, year = 2020), e("review 3.10.2020 15:00").start)
        sure("review 3.10.2020 15:00", "review Oct 5 2027 3pm", "review 2028-10-05 15:00", "review 5 October 2026 3pm")
    }

    @Test fun theDayAfterTomorrow() {
        for (t in listOf("dentist the day after tomorrow 3pm", "dentist day after tomorrow 3pm", "the day after tomorrow 3pm dentist", "dentist 3pm the day after tomorrow", "dentist on the day after tomorrow at 3pm")) {
            assertEquals(t, EventDraft("dentist", at(8, 15), at(8, 16), false, "", dayGiven = true, timeGiven = true), e(t))
        }
        assertEquals(EventDraft("offsite", at(8), at(9), true, "", dayGiven = true, timeGiven = true), e("offsite the day after tomorrow all day"))
    }

    @Test fun aWordBesideTheDayThatChangesItIsAGuess() {
        guesses(Doubt.BESIDE, "call the day before Friday 3pm", "lunch not tomorrow 1pm", "report until Friday 5pm", "report till Friday 5pm",
            "closed since Friday 9am", "open except Friday 9am", "dinner Friday after next 7pm", "dinner 7pm Friday after next", "report before this Friday 5pm", "lunch not on Friday 1pm",
            "Treffen nach Freitag 15 Uhr", "Abgabe vor Freitag 15 Uhr", "Urlaub ab Montag 9 Uhr", "Büro außer Freitag 9 Uhr", "Abgabe bis Freitag 17 Uhr", "Party morgen ab 15 Uhr")
        // "Next week Friday" is not this Friday, nor is "last Friday": the week's own words beside the day say another day too.
        guesses(Doubt.BESIDE, "dinner last Friday 7pm", "Essen letzten Freitag 19 Uhr")
        // (The week's word is left in the title as well, which is a second reason: wherever it stands in the line.)
        for (t in listOf("review a week from tomorrow 3pm", "dinner next week Friday 7pm", "dinner Friday next week 7pm", "dinner Friday week 7pm", "Essen nächste Woche Freitag 19 Uhr",
            "Essen Freitag nächste Woche 19 Uhr", "review in two weeks Friday 3pm")) {
            assertEquals(t, setOf(Doubt.BESIDE, Doubt.LEFT), e(t).doubts)
            assertFalse(t, e(t).sure)
        }
        assertEquals("dinner next week", e("dinner next week Friday 7pm").title)
        // Where the day and the time stand apart, the word beside the day counts; one beside the time alone does not.
        assertEquals(setOf(Doubt.BESIDE), e("not tomorrow lunch 1pm").doubts)
        // A word that only stands near the day, with another between them, changes nothing.
        sure("Party after work Friday 6pm", "Call before lunch tomorrow 1pm", "Drinks Friday 6pm after work", "Standup tomorrow from 9am to 10am", "Trip tomorrow to Saturday",
            "Messe Montag bis Mittwoch", "Workshop Freitag von 9 bis 17 Uhr")
        // The word stays in the title, as it was typed.
        assertEquals("report until", e("report until Friday 5pm").title)
    }

    @Test fun aSecondShortWeekdayBesideTheDayIsAGuess() {
        // "Sat" alone in a title is as often a word ("Sat nav"); directly beside the day that was read it is a second day.
        val got = e("dinner Sat tomorrow 5pm")
        assertEquals(at(7, 17), got.start)
        assertEquals("dinner Sat", got.title)
        assertEquals(setOf(Doubt.BESIDE), got.doubts)
        assertFalse(got.sure)
        assertFalse(e("dinner tomorrow 5pm sun").sure)
        sure("Sat nav update Friday 3pm", "Sun deck party tomorrow 3pm")
    }

    @Test fun twoNumbersAndASlashThatCouldBothBeTheMonthAreAGuess() {
        val got = e("dinner 12/10 7pm")
        assertEquals(at(10, 19, month = 12), got.start)
        assertEquals(setOf(Doubt.MONTH), got.doubts)
        assertFalse(got.sure)
        guesses(Doubt.MONTH, "dinner 12/10/2026 7pm", "dinner on 11/12 at 7pm", "dinner 7pm 12/10")
        assertTrue(Doubt.MONTH in e("dinner 3/4").doubts)
        assertTrue(Doubt.MONTH in e("trip 11/10 to 11/12").doubts)
        // One of the two can only be the day, or both are the same number: nothing to choose.
        sure("dinner 10/25 7pm", "dinner 12/12 7pm", "dinner 10/25/2026 7pm", "dinner 25.10. 19:00", "dinner 2026-12-10 19:00")
    }

    @Test fun nextAndAWeekdayIsNeverTodayAndAlwaysAGuess() {
        // Typed on a Tuesday: "next Tuesday" is a week on, never today, whatever the time; another weekday is the coming one.
        assertEquals(at(13, 19), e("dinner next Tuesday 7pm").start)
        assertEquals(at(13, 15, 30), e("next Tuesday at 3:30pm dentist").start)
        assertEquals(at(13, 19), e("dinner 7pm next Tuesday").start)
        assertEquals(at(13, 19), e("Essen nächsten Dienstag 19 Uhr").start)
        assertEquals(at(13), e("offsite next Tuesday all day").start)
        assertEquals(at(13), e("offsite next Tuesday").start)
        assertEquals(at(9, 19), e("dinner next Friday 7pm").start)
        assertEquals(at(9, 19), e("Essen nächsten Freitag 19 Uhr").start)
        guesses(Doubt.NEXT, "dinner next Tuesday 7pm", "next Tuesday at 3:30pm dentist", "dinner 7pm next Tuesday", "Essen nächsten Dienstag 19 Uhr", "Essen nächsten Freitag 19 Uhr",
            "offsite next Tuesday all day", "dinner next Friday 7pm", "all day next Friday offsite")
        assertEquals(setOf(Doubt.NEXT), e("offsite next Tuesday").doubts)
        // Without "next" a weekday is as it was: today while its time is still ahead. And "this" changes nothing.
        assertEquals(at(6, 19), e("dinner Tuesday 7pm").start)
        assertEquals(at(6, 19), e("dinner this Tuesday 7pm").start)
        sure("dinner Friday 7pm", "Essen kommenden Freitag 19 Uhr", "dinner on Friday 7pm", "dinner this Friday 7pm")
    }

    @Test fun aWeekdayThatIsTodaysIsAGuess() {
        // Typed on a Tuesday: "Tuesday 7pm" is today while seven is ahead, "Tuesday 9am" and "Tuesday all day" a week on. Which
        // week was meant was not said.
        assertEquals(at(6, 19), e("dinner Tuesday 7pm").start)
        assertEquals(at(13, 9), e("dinner Tuesday 9am").start)
        assertEquals(at(13), e("offsite Tuesday all day").start)
        guesses(Doubt.WEEK, "dinner Tuesday 7pm", "dinner Tuesday 9am", "offsite Tuesday all day", "dinner 7pm on Tuesday", "Essen Dienstag 19 Uhr", "dinner this Tuesday 7pm", "all day Tuesday offsite", "Tue 7pm dinner")
        assertEquals(setOf(Doubt.WEEK), e("offsite Tuesday").doubts)
        // Today by its own word, a date, a weekday with its date, and every other weekday say it.
        sure("dinner today 7pm", "Essen heute 19 Uhr", "dinner Oct 6 7pm", "dinner Tuesday 6 October 7pm", "dinner Wednesday 7pm", "dinner Monday 7pm")
        // "Next Tuesday" is a week on, and a guess of its own.
        assertEquals(setOf(Doubt.NEXT), e("dinner next Tuesday 7pm").doubts)
        // On any other day the same line is no guess.
        assertTrue(e("dinner Tuesday 7pm", LocalDateTime.of(2026, 10, 5, 10, 0)).sure)
    }

    @Test fun aNumberOrAWordOfTimeLeftInTheTitleIsAGuess() {
        // The net under every other rule: once the day and the time are read, a number standing alone in the title, or a word
        // of time that nobody read, says that something of the day, the time or the length was typed and not read.
        guesses(Doubt.LEFT,
            // A number: the minutes, a length, an hour.
            "Zahnarzt 30 morgen 15 Uhr", "2 hour workshop tomorrow 2pm", "30 min call tomorrow 2pm", "dinner tomorrow 7pm 2h", "dinner tomorrow 7pm, 90 minutes", "Meeting morgen 9 Uhr 2 Stunden",
            "meeting tomorrow 3pm for 2 hours", "meeting tomorrow 3pm for 1h30", "3rd workshop tomorrow 9am", "Termin morgen 9 Uhr 14.", "standup tomorrow 3pm room 9-10",
            // Another week, another month, a length in words.
            "dinner Friday 7pm next week", "dinner on Friday at 7pm next week", "dinner Friday in two weeks 7pm", "dinner Friday at 7pm in two weeks", "dinner Friday of next week 7pm", "dinner Friday 7pm next month",
            "dinner Friday in a fortnight 7pm", "Abendessen am Freitag um 19 Uhr nächste Woche", "Abendessen heute in einer Woche 19 Uhr", "Treffen Freitag in zwei Wochen 19 Uhr", "Treffen heute in acht Tagen 19 Uhr",
            "dinner Monday in November 7pm", "Stammtisch Freitag im November 19 Uhr", "half day offsite tomorrow 9am", "gym Friday 9am once a week", "gym Friday 9am for 6 weeks", "Sport Freitag 9 Uhr alle zwei Wochen",
            // A half of the day in a word the time's own rules do not know, where it is not the half of the time that was read.
            "Essen Freitag Abend 9 Uhr", "Treffen morgen Nachmittag 10 Uhr", "morning standup tomorrow 3pm", "night shift tomorrow 9am",
            // A zone's letters: the hour is another place's.
            "dinner tomorrow 7pm EST", "call tomorrow 19:00 UTC", "Termin morgen 9 Uhr MEZ", "dinner tomorrow 7pm GMT+2", "call tomorrow 3pm pst")
        // A zone by two letters is a word too ("Mt Fuji", "PT session"): it counts directly after the time, and there alone. So
        // does a place's "time" after it.
        guesses(Doubt.BESIDE, "call tomorrow 3pm PT", "call tomorrow 3pm ET with Sam", "call tomorrow 3pm London time", "call tomorrow 3pm my time", "Termin morgen 15 Uhr Ortszeit", "call 3pm tomorrow PT")
        sure("Mt Fuji hike Saturday 9am", "PT session tomorrow 9am", "story time Friday 10am", "Zeit für Sport morgen 18 Uhr")
        // Its cost, known and accepted: a number that is the title's own.
        guesses(Doubt.LEFT, "Sprint 12 planning tomorrow 9am", "table for 2 tomorrow 7pm", "Tisch für 2 morgen 19 Uhr", "week 42 kickoff Monday 9am")
        // Not that cost: a title's own "night" or "morning" where the time that was read is in that half of the day, and "1:1",
        // which is no time (a time's minutes have two digits).
        sure("movie night Friday 8pm", "dinner Friday night 9pm", "run tomorrow morning 9am", "Team bowling night Saturday 8pm", "Spieleabend Freitag 20 Uhr", "Essen Freitag Abend 19 Uhr",
            "afternoon tea Sunday 3pm", "1:1 with Sam tomorrow 3pm", "Sam 1:1 Friday 10am")
        assertEquals("Sprint 12 planning", e("Sprint 12 planning tomorrow 9am").title)
        // A number inside a word is no number standing alone, nor is one with a sign of its own; and a word that only holds a
        // word of time is another word.
        sure("Q3 review tomorrow 9am", "B12 shot tomorrow 9am", "5k run tomorrow 7am", "meeting room 4B tomorrow 9am", "v2 launch tomorrow 9am", "3d printing workshop tomorrow 3pm", "Issue #12 review tomorrow 3pm",
            "Halbmarathon Sonntag 9 Uhr", "Mittagessen morgen 12 Uhr", "Yearbook photo Friday 10am", "Nightingale concert Friday 8pm", "Birthday party for Sam tomorrow 3pm", "dinner with May Friday 7pm",
            "October review Friday 3pm")
        // The place has numbers of its own, and is not asked.
        sure("dinner tomorrow 7pm @ Room 4", "dinner tomorrow 7pm @ 5 Main Street", "call tomorrow 3pm @ 2 hour parking")
        // With no day and no time read there is nothing to doubt.
        assertEquals(emptySet<Doubt>(), e("Sprint 12 planning").doubts)
    }

    @Test fun moreWordsBesideTheDayThatChangeIt() {
        // Before the day: the following, the other, the first, second or last of them; „nicht“ and „statt“ as "not" is.
        guesses(Doubt.BESIDE, "dinner the following Friday 7pm", "dinner the other Friday 7pm", "Abendessen nicht Freitag 19 Uhr", "Abendessen statt Freitag 19 Uhr", "dinner instead Friday 7pm",
            "meetup first Friday 7pm", "meetup second Friday 7pm", "Stammtisch ersten Freitag 19 Uhr", "Stammtisch zweiten Freitag 19 Uhr", "Stammtisch letzten Freitag 19 Uhr")
        for (t in listOf("dinner first Monday in November 7pm", "dinner second Friday of November 7pm", "Stammtisch ersten Freitag im November 19 Uhr")) {
            assertTrue(t, Doubt.BESIDE in e(t).doubts)
            assertFalse(t, e(t).sure)
        }
        // "Friday after next", wherever the time stands; and "next" between a day and its time.
        guesses(Doubt.BESIDE, "dinner Friday 7pm after next", "dinner Friday at 7pm after next", "dinner Friday next 7pm", "dinner Friday next at 7pm")
        assertEquals(at(9, 19), e("dinner Friday next 7pm").start)
        // The same words anywhere else are the title's.
        sure("first aid course Saturday 9am", "second interview with Sam Friday 3pm", "Friday first thing 9am", "the other meeting Friday 3pm", "next steps Friday 3pm", "Drinks Friday 6pm after work")
    }

    @Test fun aFullStopAtTheEndOfTheLineIsNotPartOfItsLastWord() {
        for (t in listOf("dinner tomorrow at 7pm.", "dinner tomorrow at 7pm!", "dinner tomorrow at 7pm?", "dinner tomorrow 19:30.", "Essen morgen um 19 Uhr.", "dinner tomorrow at noon.", "dinner tomorrow 7pm!!", "dinner 7pm tomorrow."))
            assertEquals(t, e(t.trimEnd('.', '!', '?')), e(t))
        assertEquals(EventDraft("dinner", at(7, 19), at(7, 20), false, "", dayGiven = true, timeGiven = true), e("dinner tomorrow at 7pm."))
        // A date's own dot and an abbreviation's stay: „am 12.“ is the twelfth, "7 p.m." seven in the evening.
        assertEquals(at(12), e("Abgabe am 12.").start)
        assertEquals(at(14), e("Messe 14.10.").start)
        assertEquals(at(7, 19), e("dinner tomorrow 7 p.m.").start)
        // And a word of the title keeps its mark.
        assertEquals("dinner with Dr.", e("dinner tomorrow 7pm with Dr.").title)
    }

    @Test fun aWeekdayAndItsDateInGermansOwnForm() {
        // 16 October 2026 is a Friday.
        for (t in listOf("Essen am Freitag, den 16.10. um 19 Uhr", "Essen Freitag den 16.10. 19 Uhr", "Essen Freitag, den 16. Oktober 19 Uhr", "Essen 19 Uhr am Freitag, den 16.10."))
            assertEquals(t, EventDraft("Essen", at(16, 19), at(16, 20), false, "", dayGiven = true, timeGiven = true), e(t))
        // A weekday that disagrees is a second day there too.
        assertTrue(e("Essen Donnerstag, den 16.10. 19 Uhr").twice)
        // The weekday settles which number is the month, where only that reading falls on it: 8 October 2026 is a Thursday, 10 August 2027 a Tuesday.
        assertEquals(setOf(Doubt.MONTH), e("dinner 10/8 7pm").doubts)
        assertEquals(at(8, 19), e("dinner Thu 10/8 7pm").start)
        sure("dinner Thu 10/8 7pm", "dinner Thursday 10/8 at 7pm")
    }


    @Test fun aWeekdayByTwoLettersIsAGuessWhereverItIsRead() {
        // At an end of the line, with its time, it is the day it has always been: and a guess, because "do" and "Mo" are words and names too.
        assertEquals(EventDraft("things to", at(8, 15), at(8, 16), false, "", dayGiven = true, timeGiven = true, doubts = setOf(Doubt.WEAK)), e("things to do 3pm"))
        assertEquals(EventDraft("dinner with", at(12, 15), at(12, 16), false, "", dayGiven = true, timeGiven = true, doubts = setOf(Doubt.WEAK)), e("dinner with Mo 3pm"))
        assertEquals(EventDraft("Zahnarzt", at(9, 15), at(9, 16), false, "", dayGiven = true, timeGiven = true, doubts = setOf(Doubt.WEAK)), e("Fr 15 Uhr Zahnarzt"))
        guesses(Doubt.WEAK, "Zahnarzt, so 15 Uhr", "dinner so 7pm", "Mi 9:00 Uhr Zahnarzt", "15 Uhr Do Zahnarzt")
        // In the middle of a line it is not read at all: the time is, and the day is today's or tomorrow's guess.
        for ((t, title) in listOf("meet we 3pm with Sam" to "meet we with Sam", "lunch with Mo 1pm at Luigi’s" to "lunch with Mo at Luigi’s")) {
            val got = e(t)
            assertEquals(t, title, got.title)
            assertFalse(t, got.dayGiven)
            assertTrue(t, got.timeGiven)
        }
        // A weekday by its name, or by three letters, is no guess.
        sure("Freitag 15 Uhr Zahnarzt", "Fri 3pm Dentist", "Dentist thu 7am")
    }

    @Test fun midnightBesideADayIsAGuess() {
        assertEquals(at(9), e("party Friday midnight").start)
        guesses(Doubt.NIGHT, "party Friday midnight", "party tomorrow 12 at night", "party Friday 12am", "party tomorrow 1 at night", "Party Freitag Mitternacht", "party midnight on Friday",
            "Party morgen 12 Uhr nachts", "party Friday 12:30am", "party tomorrow 12 in the morning", "party Friday 0:00", "party Friday 00:00", "Party Freitag 0 Uhr")
        // An hour of the night on the 24-hour clock, and every other hour, says which day it is in.
        sure("party Friday 0:30", "party Friday 11pm", "lunch Friday noon", "party Friday 11 at night", "flight Friday 6:45 in the morning", "Party Freitag 23 Uhr nachts")
    }

    @Test fun aLengthLeftInTheTitleIsAGuess() {
        val got = e("meeting tomorrow 3pm for 2 hours")
        assertEquals(at(7, 15), got.start)
        assertEquals(at(7, 16), got.end)
        assertEquals("meeting for 2 hours", got.title)
        // With "for" and without it: how long the event is was typed and not read, and the end the row shows is the parser's own.
        guesses(Doubt.LEFT, "meeting tomorrow 3pm for 2 hours", "Besprechung morgen 15 Uhr für 2 Stunden", "meeting tomorrow 3pm for 90 min", "meeting for 1h30 tomorrow 3pm", "call tomorrow 3pm for 30m with Sam",
            "2 hour workshop tomorrow 2pm", "30 min call tomorrow 2pm", "Besprechung morgen 9 Uhr 2 Stunden", "workshop tomorrow 9am two hours")
        // "For" before a name is a word of the title.
        sure("Birthday party for Sam tomorrow 3pm", "gift for the kids tomorrow 3pm", "Tisch für Sam morgen 19 Uhr")
    }

    @Test fun aWeekdayBesideADateThatAgreesWithItIsPartOfTheDate() {
        // 28 October 2026 is a Wednesday.
        for (t in listOf("standup Wednesday 28 October 10:30am", "standup Wed 28 Oct 10:30am", "standup on Wednesday, 28 October at 10:30am", "Wednesday Oct 28 10:30am standup", "standup 10:30am Wednesday 28.10."))
            assertEquals(t, EventDraft("standup", at(28, 10, 30), at(28, 11, 30), false, "", dayGiven = true, timeGiven = true), e(t))
        assertEquals(EventDraft("Besprechung", at(28, 10, 30), at(28, 11, 30), false, "", dayGiven = true, timeGiven = true), e("Besprechung Mittwoch, 28. Oktober 10:30 Uhr"))
        assertEquals(EventDraft("offsite", at(14), at(17), true, "", dayGiven = true, timeGiven = true), e("offsite Wed 14 Oct to Fri 16 Oct"))
        assertEquals(EventDraft("offsite", at(28), at(29), true, "", dayGiven = true, timeGiven = true), e("offsite Wednesday the 28th all day"))
        // One that disagrees is a second day: it stays in the title, and which was meant is a guess.
        val other = e("standup Thursday 28 October 10:30am")
        assertEquals("standup Thursday", other.title)
        assertEquals(at(28, 10, 30), other.start)
        assertTrue(other.twice)
        assertFalse(other.sure)
    }

    @Test fun uhrAndALeadingZeroSayTheHalfOfTheDay() {
        // "09:30" and „N Uhr“ from nine on are read by the 24-hour clock, and are no guess.
        for ((t, start) in listOf("Friseur morgen 9 Uhr" to at(7, 9), "Treffen morgen um 9:30 Uhr" to at(7, 9, 30), "Mittagessen morgen 12 Uhr" to at(7, 12), "Friseur morgen um 10 Uhr" to at(7, 10),
            "standup tomorrow 09:30" to at(7, 9, 30), "run tomorrow 07:30" to at(7, 7, 30), "run tomorrow at 07:30" to at(7, 7, 30), "Lauf morgen um 07:30" to at(7, 7, 30), "Friseur morgen 11 Uhr" to at(7, 11),
            "Friseur morgen 08 Uhr" to at(7, 8), "Essen morgen 13 Uhr" to at(7, 13), "Friseur morgen 8 Uhr morgens" to at(7, 8), "Essen morgen 7 Uhr abends" to at(7, 19))) {
            assertEquals(t, start, e(t).start)
            assertTrue(t, e(t).sure)
        }
        for ((t, start, end) in listOf(Triple("Workshop morgen von 10 bis 11 Uhr", at(7, 10), at(7, 11)), Triple("Workshop morgen von 9 bis 11 Uhr", at(7, 9), at(7, 11)), Triple("Workshop morgen von 10 bis 14 Uhr", at(7, 10), at(7, 14)),
            Triple("standup tomorrow 09:00-10:30", at(7, 9), at(7, 10, 30)), Triple("run tomorrow 07:00-08:00", at(7, 7), at(7, 8)), Triple("Schicht morgen 22-2 Uhr", at(7, 22), at(8, 2)),
            Triple("Schicht morgen 8 bis 13 Uhr", at(7, 8), at(7, 13)))) {
            assertEquals(t, start, e(t).start)
            assertEquals(t, end, e(t).end)
            assertTrue(t, e(t).sure)
        }
        // „1 Uhr“ to „8 Uhr“ with no zero before them: people say „um 7 Uhr“ for the evening. Read by the 24-hour clock, as
        // ever, and a guess: "Abendessen um 7 Uhr" is never saved at seven in the morning without the editor.
        for ((t, start) in listOf("Abendessen morgen um 7 Uhr" to at(7, 7), "Friseur morgen 8 Uhr" to at(7, 8), "Kaffee morgen um 3 Uhr" to at(7, 3), "Zahnarzt morgen 1 Uhr" to at(7, 1), "Lauf morgen 6:30 Uhr" to at(7, 6, 30))) {
            assertEquals(t, start, e(t).start)
            assertTrue(t, e(t).vague)
            assertFalse(t, e(t).sure)
        }
        // The same for a span that starts at such an hour, unless its end is past twelve and leaves it only the morning.
        for ((t, start, end) in listOf(Triple("Termin morgen von 1 bis 3 Uhr", at(7, 1), at(7, 3)), Triple("Termin morgen 2-3 Uhr", at(7, 2), at(7, 3)), Triple("Abendessen morgen von 7 bis 9 Uhr", at(7, 7), at(7, 9)),
            Triple("Schicht morgen 8 bis 12 Uhr", at(7, 8), at(7, 12)))) {
            assertEquals(t, start, e(t).start)
            assertEquals(t, end, e(t).end)
            assertTrue(t, e(t).vague)
            assertFalse(t, e(t).sure)
        }
        // A bare hour stays a guess: which half of the day was not said.
        for (t in listOf("dinner tomorrow at 7", "run tomorrow 7:30", "Essen morgen um 7", "work tomorrow 9-5", "lunch tomorrow at 12", "lunch tomorrow 12:00", "standup tomorrow 9:30-10:30", "Essen morgen um 12")) {
            assertTrue(t, e(t).vague)
            assertFalse(t, e(t).sure)
        }
        assertEquals(at(7, 19), e("dinner tomorrow at 7").start)
        assertEquals(at(7, 7, 30), e("run tomorrow 7:30").start)
    }

    @Test fun theMinutesAfterUhrAreTheTimes() {
        for ((t, start, title) in listOf(Triple("Zahnarzt morgen um 8 Uhr 30", at(7, 8, 30), "Zahnarzt"), Triple("Zahnarzt morgen 15 Uhr 30", at(7, 15, 30), "Zahnarzt"), Triple("dinner tomorrow 20 Uhr 15", at(7, 20, 15), "dinner"),
            Triple("Essen morgen 8 Uhr 30 abends", at(7, 20, 30), "Essen"))) {
            assertEquals(t, start, e(t).start)
            assertEquals(t, title, e(t).title)
        }
        val span = e("Zahnarzt morgen 9 bis 10 Uhr 30")
        assertEquals(at(7, 9), span.start)
        assertEquals(at(7, 10, 30), span.end)
        assertEquals("Zahnarzt", span.title)
        // Sure as the hour alone is: fifteen and nine say their half of the day, eight does not.
        sure("Zahnarzt morgen 15 Uhr 30", "Zahnarzt morgen 9 Uhr 30", "Zahnarzt morgen 9 bis 10 Uhr 30", "Essen morgen 8 Uhr 30 abends", "15 Uhr 30 morgen Zahnarzt")
        assertFalse(e("Zahnarzt morgen um 8 Uhr 30").sure)
        // A word directly after the minutes may make a count of them („15 Uhr 30 Leute“): read as the minutes, and a guess.
        guesses(Doubt.BESIDE, "Treffen morgen 15 Uhr 30 Leute", "morgen 15 Uhr 30 Zahnarzt", "Treffen morgen 15 Uhr 20 Personen")
        assertEquals(at(7, 15, 30), e("Treffen morgen 15 Uhr 30 Leute").start)
    }

    @Test fun aSpanThatEndsAtNoonKeepsItsStart() {
        for (t in listOf("standup tomorrow 9 to noon", "standup tomorrow 9 - noon", "standup tomorrow 9am to noon", "Standup morgen von 9 bis mittags", "Standup morgen 9 bis mittags")) {
            val got = e(t)
            assertEquals(t, at(7, 9), got.start)
            assertEquals(t, at(7, 12), got.end)
            assertEquals(t, "standup", got.title.lowercase())
            assertTrue(t, got.sure)
        }
        assertEquals(at(7, 10), e("standup tomorrow 10 till noon").start)
    }

    @Test fun aBareNumberEndsARangeOfDaysOnlyAfterADayThatNamesItsMonth() {
        // „morgen bis 9 Uhr“ is tomorrow until nine, never three days: the hour is read, and the word beside the day makes it a guess.
        for ((t, start, title) in listOf(Triple("Schicht morgen bis 9 Uhr", at(7, 9), "Schicht bis"), Triple("Schicht Freitag bis 18 Uhr", at(9, 18), "Schicht bis"), Triple("Schicht heute bis 20 Uhr", at(6, 20), "Schicht bis"),
            Triple("shift tomorrow until 9 pm", at(7, 21), "shift until"), Triple("shift tomorrow till 9 Uhr", at(7, 9), "shift till"))) {
            val got = e(t)
            assertEquals(t, start, got.start)
            assertEquals(t, start.plusHours(1), got.end)
            assertFalse(t, got.allDay)
            assertEquals(t, title, got.title)
            assertFalse(t, got.sure)
        }
        // Without a word for the hour the number is neither: the day alone is read, for all of that day, and is a guess.
        for ((t, day) in listOf("shift tomorrow until 9" to 7, "shift tomorrow to 9" to 7, "shift Friday until 10" to 9, "shift Monday till 14" to 12, "sale today through 10" to 6, "course Friday - 23" to 9, "Schicht morgen bis 9" to 7)) {
            val got = e(t)
            assertEquals(t, at(day), got.start)
            assertEquals(t, at(day + 1), got.end)
            assertTrue(t, got.allDay)
            assertFalse(t, got.timeGiven)
            assertFalse(t, got.sure)
        }
        // After a day that names its month it is the last day, as ever.
        for (t in listOf("offsite Oct 14 to 16", "offsite Oct 14 until 16", "offsite 14.10. bis 16.", "offsite Oct 14-16", "Messe vom 14. bis 16. Oktober", "offsite 14 to 16 Oct")) {
            val got = e(t)
            assertEquals(t, at(14), got.start)
            assertEquals(t, at(17), got.end)
            assertTrue(t, got.sure)
        }
    }

    @Test fun aWordBeforeTheTimeThatChangesItIsAGuess() {
        // "till 5pm" does not start at five, and "before 5pm" is not five: the time is read as ever, and is a guess.
        guesses(Doubt.BESIDE, "workshop till 5pm tomorrow", "dinner until 7pm tomorrow", "call bank before 5pm tomorrow", "shift Oct 14 to 5pm", "Workshop bis 17 Uhr morgen", "drinks after 6pm Friday",
            "Treffen nach 15 Uhr morgen", "lunch not 1pm tomorrow", "shift Friday through 6pm", "shift Friday - 6pm", "shift Friday to 18 Uhr")
        assertEquals(at(7, 17), e("workshop till 5pm tomorrow").start)
        assertEquals("workshop till", e("workshop till 5pm tomorrow").title)
        // "quarter to 8pm" is a quarter to eight, „halb 9 Uhr“ half past eight: not read, and never saved as the hour that follows.
        for ((t, start) in listOf("dinner tomorrow quarter to 8pm" to at(7, 20), "dinner tomorrow half past 7pm" to at(7, 19), "Zahnarzt morgen halb 9 Uhr" to at(7, 9), "Zahnarzt morgen viertel nach 8 Uhr" to at(7, 8),
            "Zahnarzt morgen Viertel vor 9 Uhr" to at(7, 9), "standup tomorrow 8 to 12 noon" to at(7, 12))) {
            assertEquals(t, start, e(t).start)
            assertTrue(t, Doubt.BESIDE in e(t).doubts)
            assertFalse(t, e(t).sure)
        }
        // A word that leads into the time, a span's own words, and a word before the day where the day comes first change nothing.
        sure("dinner at 7pm tomorrow", "Standup tomorrow from 9am to 10am", "standup from 9am to 10am tomorrow", "Party after work Friday 6pm", "Call before lunch tomorrow 1pm", "flight to Boston 9am tomorrow",
            "Workshop morgen von 9 bis 17 Uhr", "Treffen ab 15 Uhr morgen")
    }

    @Test fun aNumberAfterAMonthsNameIsItsDayAndBeginsNoSpan() {
        // "Oct 14 until 18 Uhr" is the fourteenth, at eighteen: never from 14:00 today.
        for ((t, start, title) in listOf(Triple("shift Oct 14 until 18 Uhr", at(14, 18), "shift until"), Triple("shift Oct 14 to 9 Uhr", at(14, 9), "shift to"), Triple("shift Oct 14 to noon", at(14, 12), "shift to"),
            Triple("shift Oct 14 bis 18:00", at(14, 18), "shift bis"), Triple("shift Oct 14 - noon", at(14, 12), "shift -"), Triple("Schicht 14. Oktober bis 18 Uhr", at(14, 18), "Schicht bis"))) {
            val got = e(t)
            assertEquals(t, start, got.start)
            assertEquals(t, start.plusHours(1), got.end)
            assertEquals(t, title, got.title)
            assertFalse(t, got.sure)
        }
        // Without a month before it the number is a span's first hour, as ever; and a time that is whole after the month is one.
        assertEquals(at(7, 14), e("shift tomorrow 14 to 18 Uhr").start)
        assertEquals(at(7, 18), e("shift tomorrow 14 to 18 Uhr").end)
        assertEquals(at(6, 14), e("Sitzung im Oktober 14 Uhr").start)
    }

    @Test fun aSpanThatEndsWhereItStartsIsAGuess() {
        val got = e("standup tomorrow 9am-9am")
        assertEquals(at(7, 9), got.start)
        assertEquals(setOf(Doubt.END), got.doubts)
        assertFalse(got.sure)
        sure("standup tomorrow 9am-10am", "shift tomorrow 10pm-6am")
    }

    @Test fun aSpanOfMoreThanTwelveHoursIntoTheNextDayIsAGuess() {
        // "10pm-9pm" is twenty-three hours as it is typed: as likely a slip.
        val got = e("shift tomorrow 10pm-9pm")
        assertEquals(at(7, 22), got.start)
        assertEquals(at(8, 21), got.end)
        assertEquals(setOf(Doubt.LONG), got.doubts)
        assertFalse(got.sure)
        guesses(Doubt.LONG, "shift tomorrow 9am to midnight", "Schicht morgen 9 bis 5 Uhr", "shift tomorrow 3pm-2pm", "Schicht morgen 10 bis 2 Uhr")
        // Up to twelve hours it is a night's shift, as it was typed; and a long day that ends before midnight is that day.
        sure("shift tomorrow 10pm-6am", "shift tomorrow 8pm-8am", "Schicht morgen 22-2 Uhr", "shift tomorrow noon to midnight", "shift tomorrow 6am-11pm")
        // A start that does not say its half of the day is a guess as ever, whatever its end.
        assertTrue(e("standup tomorrow 10 to midnight").vague)
    }

    @Test fun aRangeOfDaysFromAWeekdayThatIsTodays() {
        // Typed on a Monday, "Monday to Wednesday" begins today, where "Monday" alone is the next one: which was meant is a guess.
        val monday = LocalDateTime.of(2026, 10, 5, 16, 20)
        val got = e("conference Monday to Wednesday", monday)
        assertEquals(at(5), got.start)
        assertEquals(at(8), got.end)
        assertEquals(setOf(Doubt.WEEK), got.doubts)
        assertFalse(got.sure)
        assertTrue(e("conference Tuesday to Thursday", monday).sure)
        assertTrue(e("conference today to Wednesday", monday).sure)
    }

    @Test fun everyDoubtIsCarriedByTheTwoHalvesOfALine() {
        // A day at one end and a time at the other: what was a guess about either is a guess about the whole.
        assertEquals(setOf(Doubt.PAST), e("Oct 5 dentist 3pm").doubts)
        assertEquals(setOf(Doubt.NEXT), e("next Tuesday dentist 3pm").doubts)
        assertEquals(setOf(Doubt.MONTH), e("12/10 dinner 7pm").doubts)
        assertEquals(setOf(Doubt.NIGHT), e("Friday party midnight").doubts)
        assertEquals(setOf(Doubt.NIGHT), e("midnight party Friday").doubts)
        // Midnight with no day beside it has no day at all: that is the guess, and it is said once.
        assertEquals(emptySet<Doubt>(), e("party midnight").doubts)
        assertFalse(e("party midnight").sure)
        // More than one reason at once.
        assertEquals(setOf(Doubt.PAST, Doubt.MONTH), e("dentist 10/5 3pm").doubts)
    }

    @Test fun aReminderReadsWhatItAlwaysRead() {
        // A reminder knows no guess: its day and time are read as an event's are, and it rings.
        assertEquals(ReminderPlan.Alarm(AlarmSpec(19, 0, "call"), at(6, 19)), Jot.reminder("call at 7", now))
        assertEquals(ReminderPlan.Event(EventDraft("call", at(7, 17), at(7, 17, 30), false, "", dayGiven = true, timeGiven = true)), Jot.reminder("tomorrow 5pm call", now))
        assertEquals(ReminderPlan.Event(EventDraft("dentist", at(13, 15), at(13, 15, 30), false, "", dayGiven = true, timeGiven = true)), Jot.reminder("dentist next Tuesday 3pm", now))
        // A month's day is no span's first hour for a reminder either: it rings at noon, and at eighteen, as in 3.1.
        assertEquals(ReminderPlan.Alarm(AlarmSpec(12, 0, "shift Oct 14 to"), at(6, 12)), Jot.reminder("shift Oct 14 to noon", now))
        assertEquals(ReminderPlan.Alarm(AlarmSpec(18, 0, "shift Oct 14 until"), at(6, 18)), Jot.reminder("shift Oct 14 until 18 Uhr", now))
        assertEquals(ReminderPlan.Alarm(AlarmSpec(9, 0, "Schicht morgen bis"), at(7, 9)), Jot.reminder("Schicht morgen bis 9 Uhr", now))
        // (With a dash between them 3.1 took the two for a span, and a reminder still does: it rings as it did.)
        assertEquals(ReminderPlan.Alarm(AlarmSpec(14, 0, "shift Oct"), at(6, 14)), Jot.reminder("shift Oct 14 - 18 Uhr", now))
    }
}
