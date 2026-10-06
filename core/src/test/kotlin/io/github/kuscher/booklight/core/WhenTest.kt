package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhenTest {
    /** Thursday 1 October 2026, ten in the morning. */
    private val now = LocalDateTime.of(2026, 10, 1, 10, 0)
    private fun p(s: String) = When.parse(s, now)
    private fun at(day: Int, hour: Int = 0, minute: Int = 0, month: Int = 10, year: Int = 2026) =
        LocalDateTime.of(year, month, day, hour, minute)
    private fun start(s: String) = p(s)?.start

    @Test fun aDayAndATimeAtTheStart() {
        val m = p("Fri 3pm Dentist")!!
        assertEquals(at(2, 15), m.start)
        assertNull(m.end)
        assertFalse(m.allDay)
        assertTrue(m.dayGiven)
        assertTrue(m.timeGiven)
        assertEquals("Dentist", m.rest)
    }

    @Test fun aDayAndATimeAtTheEnd() {
        val m = p("Standup tomorrow 9-9:30")!!
        assertEquals(at(2, 9), m.start)
        assertEquals(at(2, 9, 30), m.end)
        assertEquals("Standup", m.rest)
    }

    @Test fun dayAndTimeInEitherOrder() {
        assertEquals(at(2, 15), start("3pm Friday"))
        assertEquals(at(2, 15), start("Friday 3pm"))
        assertEquals(Moment(at(2, 15), null, false, true, true, "Call"), p("tomorrow at 3pm Call"))
        assertEquals(Moment(at(2, 15), null, false, true, true, "Call"), p("Call 3pm on Friday"))
        assertEquals(Moment(at(2, 9), at(2, 9, 30), false, true, true, "", vague = true), p("tomorrow 9-9:30"))
    }

    @Test fun german() {
        assertEquals(Moment(at(2, 15), null, false, true, true, ""), p("morgen 15 uhr"))
        assertEquals(Moment(at(2, 15), null, false, true, true, "Zahnarzt"), p("Zahnarzt am Freitag um 15 Uhr"))
        assertEquals(Moment(at(3), null, true, true, false, "Kino"), p("Übermorgen Kino"))
        assertEquals(Moment(at(1, 18, 30), null, false, true, true, "Essen"), p("heute 18:30 Essen"))
        assertEquals(Moment(at(1, 12), null, false, false, true, "Essen"), p("Mittag Essen"))
        assertEquals(at(15), start("in 2 Wochen"))
        assertEquals(at(3, month = 3, year = 2027), start("3. März"))
    }

    @Test fun onlyADayIsAllDay() {
        assertEquals(Moment(at(1), null, true, true, false, "Offsite"), p("today Offsite"))
        assertEquals(Moment(at(2), null, true, true, false, "Offsite"), p("Offsite tomorrow"))
        assertEquals(at(1), start("heute"))
        assertEquals(at(2), start("morgen"))
        assertEquals(at(3), start("uebermorgen"))
    }

    @Test fun aWeekdayIsTheNextSuchDay() {
        assertEquals(at(5), start("mon"))
        assertEquals(at(5), start("Montag"))
        assertEquals(at(7), start("wednesday"))
        assertEquals(at(3), start("sat"))
        // Two-letter forms are names and words too ("Mo Farah", "Di Maria"): days only beside a time.
        assertNull(p("Mo"))
        assertNull(p("Di Maria signing"))
        assertEquals(at(5, 15), start("Mo 15 Uhr"))
        assertEquals(at(6, 9), start("Di 9:00 Zahnarzt"))
        assertEquals(at(4), start("Sonntag"))
        assertEquals(at(8), start("thursday"))          // today is one: the next
    }

    @Test fun aWeekdayIsTodayWhenItsTimeIsStillAhead() {
        assertEquals(at(1, 11), start("Thu 11am"))
        assertEquals(at(8, 9), start("Thu 9am"))
        assertEquals(at(1, 15), start("15:00 Donnerstag"))
    }

    @Test fun twoLettersThatAreAlsoWordsNeedATime() {
        assertNull(p("do"))
        assertNull(p("things to do"))
        assertNull(p("so"))
        assertNull(p("we meet"))
        // (Read beside a time, and a guess all the same: „do“ and „so“ are words.)
        // (And typed on a Thursday: this week's or the next.)
        assertEquals(Moment(at(1, 15), null, false, true, true, "Zahnarzt", doubts = setOf(Doubt.WEAK, Doubt.WEEK)), p("Do 15 Uhr Zahnarzt"))
        assertEquals(Moment(at(2, 15), null, false, true, true, "Zahnarzt", doubts = setOf(Doubt.WEAK)), p("Fr 15 Uhr Zahnarzt"))
        assertEquals(at(4, 10, 30), start("So 10:30"))
        assertEquals(at(8), start("Donnerstag"))
    }

    @Test fun datesInNumbers() {
        assertEquals(at(3), start("3.10."))
        assertEquals(at(3), start("3.10.2026"))
        assertEquals(at(3), start("03.10.26"))
        assertEquals(at(3), start("10/3"))
        assertEquals(at(3, year = 2027), start("10/3/2027"))
        assertEquals(at(3), start("2026-10-03"))
        assertEquals(Moment(at(24, 18, month = 12), null, false, true, true, "Party"), p("24.12. 18:00 Party"))
    }

    @Test fun aVersionNumberIsNoDate() {
        assertNull(p("3.10"))
        assertNull(p("Launch Booklight 1.1"))
        assertNull(p("1.2.3"))
    }

    @Test fun datesWithMonthNames() {
        assertEquals(at(3), start("oct 3"))
        assertEquals(at(3), start("3 oct"))
        assertEquals(at(3), start("3. Okt"))
        assertEquals(at(3), start("October 3rd"))
        assertEquals(at(3, year = 2027), start("3. Oktober 2027"))
        assertEquals(Moment(at(3, year = 2027), null, true, true, false, "Trip"), p("Oct 3, 2027 Trip"))
        assertEquals(Moment(at(24, month = 12), null, true, true, false, "Party"), p("Party Dec 24"))
        assertEquals("Room 3", p("Room 3 oct 5")!!.rest)
    }

    @Test fun aDateWithoutAYearIsTheNextOne() {
        assertEquals(at(30, month = 9, year = 2027), start("30.9."))
        assertEquals(at(1), start("1.10."))
        assertEquals(at(29, month = 2, year = 2028), start("29.2."))
        assertEquals(at(30, month = 9), start("30.9.2026"))        // a year typed is the year meant
    }

    @Test fun datesThatDontExist() {
        for (s in listOf("31.2.", "31.2.2026", "13/1", "0.5.", "32.1.", "2026-13-01", "2026-02-30", "feb 30 2026", "oct 0")) assertNull(s, p(s))
    }

    @Test fun onlyATimeIsTodayIfStillAhead() {
        assertEquals(Moment(at(1, 17), null, false, false, true, "call bank"), p("5pm call bank"))
        assertEquals(at(2, 9), start("9am"))
        assertEquals(at(2, 10), start("10:00"))                   // now is not ahead
        assertEquals(at(1, 10, 1), start("10:01"))
    }

    @Test fun waysToWriteATime() {
        for (s in listOf("3:30pm", "3:30 pm", "15:30", "15:30 Uhr", "3:30 p.m.", "3:30PM")) assertEquals(s, at(1, 15, 30), start(s))
        for (s in listOf("3pm", "3 pm", "15 uhr", "15uhr", "15:00")) assertEquals(s, at(1, 15), start(s))
        assertEquals(at(1, 12), start("noon"))
        assertEquals(at(1, 12), start("12pm"))
        assertEquals(at(2, 0), start("midnight"))
        assertEquals(at(2, 0), start("12am"))
        assertEquals(at(2, 7, 5), start("7:05"))
    }

    @Test fun spans() {
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, ""), p("9am-10am"))
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, "Standup", vague = true), p("9–10 Standup"))
        assertEquals(Moment(at(1, 14), at(1, 15), false, false, true, "Review"), p("Review 2pm - 3pm"))
        assertEquals(Moment(at(1, 15), at(1, 16), false, false, true, ""), p("15-16 Uhr"))
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, ""), p("9-10am"))
        assertEquals(Moment(at(1, 21), at(1, 23), false, false, true, ""), p("9-11pm"))
    }

    @Test fun anEndThatIsNotAfterTheStartMovesOn() {
        assertEquals(at(1, 13), p("11-1pm")!!.end)
        assertEquals(at(1, 11), start("11-1pm"))
        assertEquals(at(2, 14), p("10-2")!!.end)
        assertEquals(at(2, 13), p("9am-1")!!.end)
        assertEquals(at(1, 16), p("3pm-4")!!.end)
        assertEquals(at(2, 2), p("22-2")!!.end)                    // past midnight
        assertEquals(at(1, 22), start("22-2"))
    }

    @Test fun aNumberAloneIsNotATime() {
        for (s in listOf("3 Dentists", "Dentists 3", "Room 15", "3", "15", "event 2026")) assertNull(s, p(s))
    }

    @Test fun exceptAfterAtOrUm() {
        assertEquals(Moment(at(1, 15), null, false, false, true, "Dentist", vague = true), p("Dentist at 3"))
        assertEquals(at(1, 19), start("at 7"))
        assertEquals(at(2, 8), start("at 8"))
        assertEquals(at(1, 12), start("at 12"))
        assertEquals(Moment(at(1, 15), null, false, false, true, "Kaffee"), p("um 15 Kaffee"))
        assertEquals(at(2, 15), start("Friday at 3"))
        assertEquals(at(2, 15), start("at 3 on Friday"))
        // With minutes too; am, pm and Uhr are taken at their word, and so is a time without "at".
        assertEquals(Moment(at(1, 15, 30), null, false, false, true, "Call", vague = true), p("Call at 3:30"))
        assertEquals(at(1, 19, 30), start("um 7:30"))
        assertEquals(at(2, 7, 30), start("at 7:30am"))
        assertEquals(at(2, 7), start("um 7 Uhr"))
        assertEquals(at(2, 3, 30), start("3:30 Call"))
        assertEquals(Moment(at(1, 15), at(1, 16), false, false, true, "Sync", vague = true), p("Sync at 3-4"))
        assertNull(p("at 25"))
        assertNull(p("on 3"))
    }

    @Test fun wordsInFrontOfTheExpressionGoWithIt() {
        assertEquals("Dentist", p("Dentist at 3pm")!!.rest)
        assertEquals("Lunch", p("Lunch on Friday")!!.rest)
        assertEquals("Kino", p("Kino am Samstag")!!.rest)
        assertEquals("Essen", p("Essen um 19 Uhr")!!.rest)
        assertEquals("Dentist", p("at 3pm Dentist")!!.rest)
        // Never "@", and not what follows the expression.
        assertEquals("Dentist @", p("Dentist @ 3pm")!!.rest)
        assertEquals("at Luigi's", p("tomorrow at Luigi's")!!.rest)
        assertEquals("on call", p("on call tomorrow")!!.rest)
    }

    @Test fun amIsMorningAfterANumberAndGermanBeforeADay() {
        assertEquals(at(2, 9), start("9 am Freitag"))
        assertEquals(at(2, 9), start("9 am"))
        assertEquals(Moment(at(2), null, true, true, false, "Zahnarzt"), p("Zahnarzt am Freitag"))
        assertEquals(at(2, 15), start("um 15 am Freitag"))
        assertNull(p("am"))
    }

    @Test fun inSoLong() {
        assertEquals(Moment(at(1, 10, 20), null, false, false, true, "stretch"), p("in 20m stretch"))
        assertEquals(at(1, 12), start("in 2h"))
        assertEquals(at(1, 11, 30), start("in 1h 30m"))
        assertEquals(at(1, 11, 30), start("in 1.5h"))
        assertEquals(at(1, 10, 20), start("call mum in 20 min"))
        assertEquals(now.plusSeconds(90), start("in 90s"))
        assertEquals(Moment(at(4), null, true, true, false, "x"), p("in 3 days x"))
        assertEquals(at(4), start("in 3d"))
        assertEquals(at(1, month = 11), start("in 1 month"))
        assertEquals(at(4, 17), start("in 3 days 5pm"))
    }

    @Test fun inWithoutALengthIsNothing() {
        for (s in listOf("in", "in 20", "in love", "in 0m", "in 20x", "in 99999999999999999999h", "in 99999 months", "in 2 years")) {
            assertNull(s, p(s))
        }
    }

    @Test fun theLongerReadingWinsThenTheStart() {
        assertEquals(Moment(at(2), null, true, true, false, "call friday"), p("tomorrow call friday"))
        assertNull(p("call tomorrow about it"))                    // the middle is not read
    }

    @Test fun aDayAtOneEndAndATimeAtTheOther() {
        assertEquals(Moment(at(2, 13), null, false, true, true, "lunch"), p("tomorrow lunch at 1pm"))
        assertEquals(Moment(at(2, 15), null, false, true, true, "Dentist"), p("Friday Dentist 3pm"))
        assertEquals(Moment(at(2, 15), null, false, true, true, "Dentist"), p("3pm Dentist tomorrow"))
        assertEquals(Moment(at(2, 9), at(2, 9, 30), false, true, true, "Standup", vague = true), p("9-9:30 Standup tomorrow"))
    }

    @Test fun aSpanAfterLunch() {
        assertEquals(Moment(at(2, 14), at(2, 15), false, true, true, "Review", vague = true), p("Fri 2-3 Review"))
        assertEquals(Moment(at(1, 15), at(1, 16), false, false, true, "Sprint", vague = true), p("Sprint 3-4"))
        assertEquals(at(2, 9), start("tomorrow 9-10"))             // mornings stay mornings
        assertEquals(at(2, 3), start("tomorrow 3am-4am"))          // said outright
    }

    @Test fun nextFriday() {
        // The coming one, as ever, and a guess: people mean two different days by "next Friday".
        assertEquals(Moment(at(2), null, true, true, false, "Dentist", doubts = setOf(Doubt.NEXT)), p("Dentist next Friday"))
        assertEquals(Moment(at(2), null, true, true, false, "Dentist", doubts = setOf(Doubt.NEXT)), p("next friday Dentist"))
        assertEquals(Moment(at(2), null, true, true, false, "Zahnarzt", doubts = setOf(Doubt.NEXT)), p("Zahnarzt nächsten Freitag"))
        // Typed on that weekday it is a week on, never today, whatever the time; without "next" it is today while its time is ahead,
        // and a guess of another kind: this week's or the next was not said.
        assertEquals(Moment(at(8, 11), null, false, true, true, "Dentist", doubts = setOf(Doubt.NEXT)), p("Dentist next Thursday 11am"))
        assertEquals(Moment(at(8), null, true, true, false, "Dentist", doubts = setOf(Doubt.NEXT)), p("Dentist next Thursday"))
        assertEquals(Moment(at(1, 11), null, false, true, true, "Dentist", doubts = setOf(Doubt.WEEK)), p("Dentist this Thursday 11am"))
        assertEquals(Moment(at(1, 11), null, false, true, true, "Dentist", doubts = setOf(Doubt.WEEK)), p("Dentist Thursday 11am"))
        assertEquals(Moment(at(8, 9), null, false, true, true, "Dentist", doubts = setOf(Doubt.WEEK)), p("Dentist Thursday 9am"))
        assertEquals(Moment(at(2, 11), null, false, true, true, "Dentist"), p("Dentist Friday 11am"))
    }

    @Test fun theLongestExpression() {
        assertEquals(Moment(at(3, 9, year = 2027), at(3, 10, year = 2027), false, true, true, "Brunch"),
            p("Brunch am 3. Okt 2027 um 9 am - 10 am"))
    }

    @Test fun theRestIsAsTyped() {
        assertEquals("Call  ANNA re: Q3", p("Fri 3pm  Call  ANNA re: Q3")!!.rest)
        assertEquals("Lunch", p("  Lunch  tomorrow ")!!.rest)
        assertEquals(at(2, 15), start("FRI, 3PM"))
        assertEquals("Dentist", p("Dentist, Fri 3pm")!!.rest)
        assertEquals("Dentist, the good one", p("Fri 3pm, Dentist, the good one")!!.rest)
    }

    // What a spoken sentence says that the parser could not read (docs/design/event-sentence/design.md §6). Today is Thursday 1 October 2026, 10:00.

    @Test fun tmrwIsTomorrow() {
        assertEquals(Moment(at(2, 9), at(2, 10), false, true, true, "meeting"), p("meeting tmrw 9-10am"))
        assertEquals(at(2), start("tmrw"))
        assertEquals(at(2, 15), start("tmr 3pm"))
    }

    @Test fun theTwelfth() {
        assertEquals(Moment(at(12), null, true, true, false, "Flight to Frankfurt"), p("Flight to Frankfurt on the 12th"))
        assertEquals(at(12), start("on 12th"))
        assertEquals(at(1), start("on the 1st"))                    // today counts
        assertEquals(at(31), start("on the 31st"))
        assertEquals(at(12, 6, 45), start("on the 12th at 6:45 in the morning"))
        assertEquals(at(12, 15), start("3pm on the 12th"))
        assertEquals(at(3), start("3rd of October"))
        assertEquals(at(3), start("the 3rd of oct"))
        // A month that has no such day: the next one that has.
        assertEquals(LocalDateTime.of(2026, 12, 31, 0, 0), When.parse("on the 31st", LocalDateTime.of(2026, 11, 2, 10, 0))!!.start)
        // In German the number has a dot, and „am“ or „den“ before it says it is a day.
        assertEquals(Moment(at(12), null, true, true, false, "Abgabe"), p("Abgabe am 12."))
        assertEquals(at(12, 18), start("am 12. um 18 Uhr"))
        assertEquals(at(12), start("den 12."))
        assertEquals(at(1, 18), start("um 18 Uhr am 1."))
    }

    @Test fun aNumberWithThAloneNeedsATime() {
        // "2nd interview", "the 3rd floor", "the 1st draft": a number like that is the second of something as often as a day.
        for (s in listOf("12th", "the 12th", "2nd interview", "the 3rd floor", "1st", "the", "the sun is out", "am 12", "12.", "on 12")) assertNull(s, p(s))
        assertEquals(at(12, 9), start("12th 9am"))
        assertEquals(at(12, 9), start("the 12th 9am"))
        // Read beside a time, and a guess all the same ("room 3rd 3pm" is the third room); with "on" it is a day said outright.
        assertEquals(Moment(at(12, 6, 45), null, false, true, true, "Flight", vague = true), p("Flight 12th at 6:45am"))
        assertTrue(p("room 3rd 3pm")!!.vague)
        assertTrue(p("the 12th 9am")!!.vague)
        assertFalse(p("Flight on the 12th at 6:45am")!!.vague)
        assertFalse(p("3pm on the 12th")!!.vague)
        assertFalse(p("am 12. um 18 Uhr")!!.vague)
        assertEquals("2nd interview", p("2nd interview tomorrow")!!.rest)
        assertEquals("the 1st draft review", p("the 1st draft review tomorrow")!!.rest)
    }

    @Test fun theHalfOfTheDayInWords() {
        assertEquals(at(2, 6, 45), start("6:45 in the morning"))
        assertEquals(at(1, 19), start("7 in the evening"))
        assertEquals(at(1, 15), start("3 in the afternoon"))
        assertEquals(at(1, 23), start("11 at night"))
        assertEquals(at(1, 19), start("um 7 abends"))
        assertEquals(at(2, 7), start("7 Uhr morgens"))
        assertEquals(at(2, 7), start("morgen 7 Uhr früh"))
        assertEquals(at(1, 15), start("3 nachmittags"))
        assertEquals(at(1, 19), start("7 am Abend"))
        // A time that says its own half of the day keeps it.
        assertEquals(at(1, 19), start("19 Uhr abends"))
        assertEquals(at(1, 19), start("7pm in the evening"))
        // "at 6:45" alone is the afternoon; the morning said outright is the morning.
        assertEquals(at(1, 18, 45), start("at 6:45"))
        assertEquals(at(2, 6, 45), start("at 6:45 in the morning"))
        assertEquals(Moment(at(2, 19), at(2, 21), false, true, true, "Film"), p("Film tomorrow 7-9 in the evening"))
        assertEquals("Call", p("Call 9 in the morning")!!.rest)
        // The words alone are no time.
        for (s in listOf("in the morning", "abends", "at night", "früh")) assertNull(s, p(s))
    }

    @Test fun theHalfOfTheDayHoldsForEveryFormOfTheHour() {
        // „7 Uhr abends“ is seven in the evening: with the words, an hour up to twelve is on the twelve-hour clock, „Uhr“ or not.
        assertEquals(at(1, 19), start("7 Uhr abends"))
        assertEquals(at(1, 19), start("um 7 Uhr abends"))
        assertEquals(at(1, 15), start("3 Uhr nachmittags"))
        assertEquals(at(1, 20), start("8 Uhr abends"))
        assertEquals(at(1, 18, 30), start("6:30 Uhr abends"))
        assertEquals(at(1, 19, 30), start("7:30 in the evening"))
        assertEquals(Moment(at(2, 20), null, false, true, true, "Treffen"), p("Treffen morgen 8 Uhr abends"))
        assertEquals(at(2, 7), start("7 Uhr morgens"))
        assertEquals(at(2, 9), start("9 Uhr vormittags"))
        // An hour that can only be one half of the day keeps it, where the words agree.
        assertEquals(at(1, 15), start("15 Uhr nachmittags"))
        assertEquals(at(1, 23), start("23 Uhr nachts"))
        assertEquals(at(1, 23), start("11pm at night"))
        assertEquals(at(2, 9), start("9am in the morning"))
        // Noon by its hour: „12 Uhr mittags“, "12 noon", „1 Uhr mittags“.
        assertEquals(at(1, 12), start("12 Uhr mittags"))
        assertEquals(at(1, 12), start("12 noon"))
        assertEquals(at(1, 13), start("1 Uhr mittags"))
        assertEquals(at(1, 12), start("12 in the afternoon"))
        // A time said this way says its half of the day: it is no guess.
        assertFalse(p("Treffen morgen 8 Uhr abends")!!.vague)
        assertFalse(p("7 in the evening")!!.vague)
    }

    @Test fun atNight() {
        // Twelve at night is midnight, one to five are the small hours, six to eleven the evening's.
        assertEquals(at(2, 0), start("12 at night"))
        assertEquals(at(2, 0, 30), start("12:30 at night"))
        assertEquals(at(2, 0), start("12 nachts"))
        assertEquals(at(2, 0), start("12 Uhr nachts"))
        assertEquals(at(2, 1), start("1 at night"))
        assertEquals(at(2, 2), start("2 at night"))
        assertEquals(Moment(at(2, 3), null, false, false, true, "alarm"), p("alarm 3 at night"))
        assertEquals(at(2, 5), start("5 at night"))
        assertEquals(at(2, 3), start("3 Uhr nachts"))
        assertEquals(at(1, 18), start("6 at night"))
        assertEquals(at(1, 23), start("11 at night"))
        assertEquals(at(2, 0), start("12 in the morning"))
        // A span over midnight is the night's own; no other half of the day has one.
        assertEquals(Moment(at(1, 23), at(2, 1), false, false, true, ""), p("11-1 at night"))
        assertNull(Times.parse(listOf("11-1", "in", "the", "evening"), bare = true))
        assertNull(Times.parse(listOf("11-1", "in", "the", "morning"), bare = true))
    }

    @Test fun aHalfOfTheDayThatContradictsItsHourIsNotRead() {
        // Eleven is no afternoon's hour, twelve no evening's, fifteen no morning's: the words are not taken for a time's.
        for (s in listOf("11 in the afternoon", "9 in the afternoon", "12 in the evening", "2 in the evening", "25 in the evening", "0 in the afternoon", "Call at 11 in the afternoon"))
            assertNull(s, p(s))
        for (w in listOf("15 uhr morgens", "12 uhr vormittags", "noon in the morning", "noon at night", "9-10 in the afternoon", "7am in the evening", "7pm in the morning"))
            assertNull(w, Times.parse(w.split(' '), bare = true))
        // A time that can be read without the words is, and the words stay in the line: an event's reading is then a guess (JotTest).
        assertEquals(Moment(at(2, 7), null, false, false, true, "in the evening"), p("7am in the evening"))
        assertEquals(Moment(at(1, 15), null, false, false, true, "morgens"), p("15 Uhr morgens"))
        assertEquals(Moment(at(1, 12), null, false, false, true, "in the morning"), p("noon in the morning"))
        assertEquals(Moment(at(1, 11), at(1, 13), false, false, true, "in the evening", vague = true), p("11-1 in the evening"))
    }

    @Test fun aTimeThatDoesNotSayItsHalfOfTheDayIsAGuess() {
        // An hour from one to twelve with neither am nor pm nor words for the half of the day: which half is the parser's to choose.
        for (s in listOf("at 7", "um 7", "at 7:30", "7:30", "at 12", "12:30", "12:00", "9-10", "from 9 to 10", "von 9 bis 10", "at 3-4", "Fri 2-3", "tomorrow at 8", "9:00-10:30", "7-13"))
            assertTrue(s, p(s)!!.vague)
        // Said: am or pm, an hour past twelve, noon and midnight by name, the half of the day in words, and a span whose other end says it.
        // And by the 24-hour clock's own ways of writing an hour: a zero before it, or „Uhr“ after an hour from nine on.
        for (s in listOf("7pm", "7 pm", "at 7am", "19:30", "19 Uhr", "um 15", "0:30", "noon", "Mittag", "midnight", "7 in the evening", "7 Uhr abends", "9-10am", "9am-1", "3pm-4",
            "15-16 Uhr", "9-17 Uhr", "8-13", "22-2", "tomorrow", "Oct 14 to Oct 16", "in 20m", "Saturday all day",
            "9 Uhr", "um 10 Uhr", "11 Uhr", "12 Uhr", "um 9:30 Uhr", "13 Uhr", "08 Uhr", "um 07 Uhr", "8 Uhr morgens", "von 10 bis 11 Uhr", "9 bis 10 Uhr", "von 9 bis 11 Uhr", "von 10 bis 14 Uhr",
            "8 bis 13 Uhr", "08-12 Uhr", "09:30", "07:30", "at 07:30", "09:00-10:30"))
            assertFalse(s, p(s)!!.vague)
        // „1 Uhr“ to „8 Uhr“ with no zero before them are said for the evening as often as they are written for the morning: read
        // by the 24-hour clock, as ever, and a guess. So is a span that starts at such an hour, unless its end is past twelve.
        for (s in listOf("um 7 Uhr", "8 Uhr", "1 Uhr", "um 3 Uhr", "7:30 Uhr", "von 1 bis 3 Uhr", "2-3 Uhr", "7 bis 9 Uhr", "8 bis 12 Uhr", "8 Uhr bis 10 Uhr"))
            assertTrue(s, p(s)!!.vague)
        // What they read: „7 Uhr“ and "07:30" are the morning also after "at" and „um“, where a bare seven is the evening.
        assertEquals(at(2, 7), start("um 7 Uhr"))
        assertEquals(at(2, 3), start("um 3 Uhr"))
        assertEquals(at(2, 7, 30), start("at 07:30"))
        assertEquals(at(1, 19, 30), start("at 7:30"))
        assertEquals(Moment(at(2, 1), at(2, 3), false, false, true, "", vague = true), p("von 1 bis 3 Uhr"))
        assertEquals(Moment(at(2, 2), at(2, 3), false, false, true, "", vague = true), p("2-3 Uhr"))
        assertEquals(Moment(at(2, 7), at(2, 8), false, false, true, ""), p("07:00-08:00"))
        assertEquals(Moment(at(1, 14), at(1, 15), false, false, true, "", vague = true), p("2-3"))
    }

    @Test fun theMinutesAfterUhr() {
        // „8 Uhr 30“: two digits below sixty directly after „Uhr“ are that hour's minutes.
        assertEquals(at(2, 8, 30), start("um 8 Uhr 30"))
        assertEquals(at(1, 15, 30), start("15 Uhr 30"))
        assertEquals(at(1, 20, 15), start("20 Uhr 15"))
        assertEquals(at(2, 9, 5), start("9 Uhr 05"))
        assertEquals(Moment(at(2, 15, 30), null, false, true, true, "Zahnarzt"), p("Zahnarzt morgen 15 Uhr 30"))
        assertEquals(Moment(at(2, 9), at(2, 10, 30), false, false, true, ""), p("9 bis 10 Uhr 30"))
        assertEquals(at(1, 20, 30), start("8 Uhr 30 abends"))
        // The half of the day is said as for the hour alone.
        assertTrue(p("um 8 Uhr 30")!!.vague)
        assertFalse(p("15 Uhr 30")!!.vague)
        assertFalse(p("9 Uhr 30")!!.vague)
        // Not one digit or three, not sixty or more, and not after an hour that has its minutes already.
        for (w in listOf("15 uhr 5", "15 uhr 300", "15 uhr 75", "15:30 uhr 20", "15 30")) assertNull(w, Times.parse(w.split(' '), bare = true))
    }

    @Test fun aSpanThatEndsAtNoon() {
        // "9 to noon": the end is a time by its name. (The same word says a half of the day after an hour: "12 noon".)
        for (s in listOf("9 to noon", "9 - noon", "9-noon", "9am to noon", "9am - noon", "von 9 bis mittags", "9 bis mittags", "9 bis Mittag"))
            assertEquals(s, Moment(at(2, 9), at(2, 12), false, false, true, ""), p(s))
        assertEquals(Moment(at(2, 10), at(2, 12), false, true, true, "standup"), p("standup tomorrow 10 till noon"))
        assertEquals(Moment(at(1, 12), at(1, 14), false, false, true, ""), p("noon to 2"))
        assertEquals(at(1, 12), start("12 noon"))
        assertEquals(at(1, 12), start("12 Uhr mittags"))
    }

    @Test fun aSpanInWords() {
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, "Standup", vague = true), p("Standup from 9 to 10"))
        assertEquals(Moment(at(1, 14), at(1, 15), false, false, true, "", vague = true), p("from 2 to 3"))                  // after lunch, as "2-3" is
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, "", vague = true), p("von 9 bis 10"))
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, ""), p("von 9 bis 10 Uhr"))                           // „Uhr“ says the 24-hour clock, for both ends
        assertEquals(Moment(at(2, 9), at(2, 10, 30), false, false, true, ""), p("9am to 10:30am"))
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, ""), p("9 to 10am"))
        assertEquals(Moment(at(1, 15), at(1, 16), false, false, true, ""), p("15 bis 16 Uhr"))
        assertEquals(Moment(at(2, 9), at(2, 10), false, true, true, "Sync", vague = true), p("Sync tomorrow from 9 to 10"))
        assertEquals(Moment(at(2, 9), at(2, 10), false, true, true, "Sync", vague = true), p("Sync morgen von 9 bis 10"))
        // Two bare numbers with a word between them are no span, and "from" without an end is no time.
        for (s in listOf("2 to 3 Dentists", "9 to 10", "from 9", "from Berlin to Paris", "von 9", "9 bis")) assertNull(s, p(s))
    }

    @Test fun aRangeOfDays() {
        assertEquals(Moment(at(14), at(17), true, true, true, "Team offsite"), p("Team offsite Oct 14 to Oct 16"))
        assertEquals(Moment(at(14), at(17), true, true, true, ""), p("Oct 14 to 16"))
        assertEquals(Moment(at(14), at(17), true, true, true, ""), p("Oct 14-16"))
        assertEquals(Moment(at(14), at(17), true, true, true, "Team offsite"), p("Team offsite Oct 14-16"))
        assertEquals(Moment(at(14), at(17), true, true, true, "Offsite"), p("Offsite 14.10. bis 16.10."))
        assertEquals(Moment(at(14), at(17), true, true, true, "Offsite"), p("Offsite vom 14. bis 16. Oktober"))
        assertEquals(Moment(at(14), at(17), true, true, true, ""), p("14.-16. Oktober"))
        assertEquals(Moment(at(5), at(8), true, true, true, "Messe"), p("Messe Montag bis Mittwoch"))
        assertEquals(Moment(at(5), at(8), true, true, true, ""), p("from Monday to Wednesday"))
        assertEquals(Moment(at(2), at(4), true, true, true, "Trip"), p("Trip tomorrow to Saturday"))
        // Over the year's end: the last day is the next such day after the first.
        assertEquals(Moment(at(30, month = 12), at(3, month = 1, year = 2027), true, true, true, ""), p("Dec 30 to Jan 2"))
        // No range where the last day is not after the first: the first day is read, and the rest stays.
        assertEquals(Moment(at(16), null, true, true, false, "to 14"), p("Oct 16 to 14"))
        // Nor where the two are months apart: two dates that happen to stand side by side.
        assertEquals(Moment(at(14), null, true, true, false, "to Mar 3"), p("Oct 14 to Mar 3"))
        // And then no shorter range is made of a part of them: "14 to Dec 20" of "Oct 14 to Dec 20" is no range of December's.
        assertEquals(Moment(at(14), null, true, true, false, "to Dec 20"), p("Oct 14 to Dec 20"))
        assertEquals(Moment(at(1, month = 1, year = 2027), null, true, true, false, "to Dec 31"), p("Jan 1 to Dec 31"))
        assertEquals(Moment(at(20, month = 12), null, true, true, false, "Sprint Oct 5 to"), p("Sprint Oct 5 to Dec 20"))
        assertEquals(Moment(at(14), null, true, true, false, "to 20 Dec"), p("Oct 14 to 20 Dec"))
        assertEquals(Moment(at(20, month = 12), null, true, true, false, "Offsite vom 14. Oktober bis"), p("Offsite vom 14. Oktober bis 20. Dezember"))
        for (s in listOf("Oct 14 to Dec 20", "Sprint Oct 5 to Dec 20", "Jan 1 to Dec 31", "Oct 14 to 20 Dec", "vom 14. Oktober bis 20. Dezember")) {
            assertNull(s, p(s)!!.end)
            assertNull(s, When.spot(s, now)!!.moment.end)
        }
        // A range inside the bound is read whole, over a month's end too, and a number beside a month that is its own is still read.
        assertEquals(Moment(at(30), at(3, month = 11), true, true, true, ""), p("Oct 30 to Nov 2"))
        assertEquals(Moment(at(14), at(21, month = 11), true, true, true, "Sprint"), p("Sprint Oct 14 to Nov 20"))
        assertEquals(Moment(at(1, month = 12), at(6, month = 1, year = 2027), true, true, true, ""), p("Dec 1 to 5 Jan"))
    }

    @Test fun allDaySaidOutright() {
        assertEquals(Moment(at(3, month = 11), null, true, true, true, "Birthday party"), p("Birthday party on November 3rd all day"))
        assertEquals(Moment(at(3), null, true, true, true, "Geburtstag"), p("Geburtstag am Samstag ganztägig"))
        assertEquals(Moment(at(3), null, true, true, true, "Messe"), p("ganztägig Messe am Samstag"))           // the day at the other end
        assertEquals(Moment(at(2), null, true, true, true, "Offsite"), p("all day tomorrow Offsite"))
        assertEquals(Moment(at(3), null, true, true, true, "Messe"), p("Messe Samstag den ganzen Tag"))
        assertEquals(Moment(at(2), null, true, true, true, "Offsite"), p("Offsite tomorrow all-day"))
        // A day alone is all day too, and there the time is the guess.
        assertEquals(Moment(at(2), null, true, true, false, "Offsite"), p("Offsite tomorrow"))
    }

    @Test fun allDayWithNoDayBesideItIsNeitherADayNorATime() {
        // "I worked all day" is no event: the words are a day's length only where a day stands beside them or at the line's other end.
        for (s in listOf("Inventory all day", "I worked all day", "what to eat all day", "Alles ganztägig", "Meeting. All day", "Meeting den ganzen Tag", "all day", "ganztägig", "all day sale"))
            assertNull(s, p(s))
        // What else the line says is read as if the words were not there: as it was before they meant anything.
        assertEquals(Moment(at(1, 15), null, false, false, true, "meeting all day"), p("3pm meeting all day"))
        assertEquals(Moment(at(1, 15), null, false, false, true, "all day meeting"), p("all day meeting 3pm"))
        // With a day anywhere at the ends they are that day's.
        assertEquals(Moment(at(2), null, true, true, true, "sale"), p("all day sale tomorrow"))
    }

    @Test fun everyMondayIsReadAsTheNextOneAndMarked() {
        assertEquals(Moment(at(5, 18), null, false, true, true, "yoga class", repeats = true), p("yoga class every Monday 6pm"))
        assertEquals(Moment(at(5, 18), null, false, true, true, "Yoga", repeats = true), p("jeden Montag 18 Uhr Yoga"))
        assertEquals(Moment(at(5, 18), null, false, true, true, "Yoga", repeats = true), p("Yoga 6pm every Monday"))
        assertEquals(Moment(at(5), null, true, true, false, "Yoga", repeats = true), p("Yoga each Monday"))
        assertFalse(p("yoga class Monday 6pm")!!.repeats)
        // Only a weekday repeats that way: before anything else the word stays in the line.
        assertEquals(Moment(at(2), null, true, true, false, "every"), p("every tomorrow"))
        assertNull(p("every"))
        assertNull(p("every other day"))
    }

    @Test fun nothingToRead() {
        for (s in listOf("", "   ", "Dentist", "call the bank", "@", "at", "on", "um", ":", "-", "9-", "-9", "25:00", "13pm",
            "0pm", "9:75", "9:5", "123", "noonish", "3pmm", "9-10-11", "friday's", "🎉", "at at 3pm at")) {
            assertNull(s, p(s))
        }
    }

    @Test fun nothingThrows() {
        val bits = listOf("fri", "3pm", "in", "20m", "at", "um", "am", "on", "-", "–", "9", "25", ":", ".", "/", "@", "uhr",
            "10/3", "3.10.", "99.99.9999", "oct", "3.", "2026-10-03", "9-9:30", "noon", "tomorrow", "x", "🎉", "\u0000",
            "\uD83D", "9".repeat(40), "1e9", "0", "-1", "days", "wochen", "1h30", "٣", "İ", "ß", ",", ";",
            "to", "from", "bis", "von", "vom", "the", "12th", "0th", "99th", "every", "jeden", "all", "day", "ganztägig", "morning", "abends", "den", "14-16", "14.-16.", "-", "of", "tmrw")
        val random = java.util.Random(7)
        repeat(20_000) {
            val text = List(1 + random.nextInt(6)) { bits[random.nextInt(bits.size)] }.joinToString(if (random.nextBoolean()) " " else "  ")
            When.parse(text, now)
            Jot.event(text, now); Jot.reminder(text, now); Jot.timer(text); Jot.alarm(text, now)
        }
        val long = "tomorrow " + "word ".repeat(50_000) + "3pm"
        assertEquals(at(2, 15), start(long))                       // a day at one end, a time at the other
        assertEquals(at(1, 15), start("word ".repeat(50_000) + "at 3pm"))
        assertNull(p("9".repeat(100_000)))
    }
}
