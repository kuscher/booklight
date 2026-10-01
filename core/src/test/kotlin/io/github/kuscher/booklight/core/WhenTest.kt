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
        assertEquals(Moment(at(2, 9), at(2, 9, 30), false, true, true, ""), p("tomorrow 9-9:30"))
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
        assertEquals(Moment(at(1, 15), null, false, true, true, "Zahnarzt"), p("Do 15 Uhr Zahnarzt"))
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
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, "Standup"), p("9–10 Standup"))
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
        assertEquals(Moment(at(1, 15), null, false, false, true, "Dentist"), p("Dentist at 3"))
        assertEquals(at(1, 19), start("at 7"))
        assertEquals(at(2, 8), start("at 8"))
        assertEquals(at(1, 12), start("at 12"))
        assertEquals(Moment(at(1, 15), null, false, false, true, "Kaffee"), p("um 15 Kaffee"))
        assertEquals(at(2, 15), start("Friday at 3"))
        assertEquals(at(2, 15), start("at 3 on Friday"))
        // With minutes too; am, pm and Uhr are taken at their word, and so is a time without "at".
        assertEquals(Moment(at(1, 15, 30), null, false, false, true, "Call"), p("Call at 3:30"))
        assertEquals(at(1, 19, 30), start("um 7:30"))
        assertEquals(at(2, 7, 30), start("at 7:30am"))
        assertEquals(at(2, 7), start("um 7 Uhr"))
        assertEquals(at(2, 3, 30), start("3:30 Call"))
        assertEquals(Moment(at(1, 15), at(1, 16), false, false, true, "Sync"), p("Sync at 3-4"))
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
        assertEquals(Moment(at(2, 9), at(2, 9, 30), false, true, true, "Standup"), p("9-9:30 Standup tomorrow"))
    }

    @Test fun aSpanAfterLunch() {
        assertEquals(Moment(at(2, 14), at(2, 15), false, true, true, "Review"), p("Fri 2-3 Review"))
        assertEquals(Moment(at(1, 15), at(1, 16), false, false, true, "Sprint"), p("Sprint 3-4"))
        assertEquals(at(2, 9), start("tomorrow 9-10"))             // mornings stay mornings
        assertEquals(at(2, 3), start("tomorrow 3am-4am"))          // said outright
    }

    @Test fun nextFriday() {
        assertEquals(Moment(at(2), null, true, true, false, "Dentist"), p("Dentist next Friday"))
        assertEquals(Moment(at(2), null, true, true, false, "Dentist"), p("next friday Dentist"))
        assertEquals(Moment(at(2), null, true, true, false, "Zahnarzt"), p("Zahnarzt nächsten Freitag"))
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

    @Test fun nothingToRead() {
        for (s in listOf("", "   ", "Dentist", "call the bank", "@", "at", "on", "um", ":", "-", "9-", "-9", "25:00", "13pm",
            "0pm", "9:75", "9:5", "123", "noonish", "3pmm", "9-10-11", "friday's", "🎉", "at at 3pm at")) {
            assertNull(s, p(s))
        }
    }

    @Test fun nothingThrows() {
        val bits = listOf("fri", "3pm", "in", "20m", "at", "um", "am", "on", "-", "–", "9", "25", ":", ".", "/", "@", "uhr",
            "10/3", "3.10.", "99.99.9999", "oct", "3.", "2026-10-03", "9-9:30", "noon", "tomorrow", "x", "🎉", "\u0000",
            "\uD83D", "9".repeat(40), "1e9", "0", "-1", "days", "wochen", "1h30", "٣", "İ", "ß", ",", ";")
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
