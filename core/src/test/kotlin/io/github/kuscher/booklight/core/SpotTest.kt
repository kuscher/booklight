package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A day and a time anywhere in a line ([When.spot]): what an event's sentence is read by. */
class SpotTest {
    /** Monday 5 October 2026, twenty past four: the day the sixteen sentences were tried on a device. */
    private val now = LocalDateTime.of(2026, 10, 5, 16, 20)
    private fun s(text: String) = When.spot(text, now)
    private fun at(day: Int, hour: Int = 0, minute: Int = 0) = LocalDateTime.of(2026, 10, day, hour, minute)
    private fun pieces(text: String) = s(text)!!.pieces.map { it.text }

    @Test fun aDayAndATimeInTheMiddle() {
        val x = s("Add dinner with Sam tomorrow at 7pm to Team calendar")!!
        assertEquals(at(6, 19), x.moment.start)
        assertTrue(x.moment.dayGiven && x.moment.timeGiven)
        assertEquals(listOf("Add dinner with Sam", "to Team calendar"), x.pieces.map { it.text })
        // Where each piece starts in the line: what is cut out of one can be put back in its place.
        assertEquals(listOf(0, 36), x.pieces.map { it.start })
        assertEquals("tomorrow at 7pm", x.said)
        assertEquals("Add dinner with Sam to Team calendar", x.moment.rest)
    }

    @Test fun whatTheEndsReadIsReadTheSame() {
        for (t in listOf("Fri 3pm Dentist", "Standup tomorrow 9-9:30", "tomorrow call friday", "Friday Dentist 3pm", "Dentist, Fri 3pm", "in 20m stretch",
            "Brunch am 3. Okt 2027 um 9 am - 10 am", "9-9:30 Standup tomorrow", "Fri 3pm, Dentist, the good one", "  Lunch  tomorrow ", "Team offsite Oct 14 to Oct 16"))
            assertEquals(t, When.parse(t, now), s(t)!!.moment)
        for (t in listOf("", "   ", "Dentist", "call the bank", "3 Dentists", "at", "🎉", "Launch Booklight 1.1")) assertNull(t, s(t))
    }

    @Test fun theLongestRunWinsWhereverItStands() {
        // "Friday" at the start is one word; "tomorrow at 7pm" in the middle is three.
        val x = s("Friday dinner with Sam tomorrow at 7pm to Team")!!
        assertEquals(at(6, 19), x.moment.start)
        assertEquals(listOf("Friday dinner with Sam", "to Team"), x.pieces.map { it.text })
        // As long as each other: the start, then the end, then the leftmost.
        assertEquals(at(6), s("tomorrow call friday")!!.moment.start)
        assertEquals(at(9), s("call tomorrow about friday")!!.moment.start)
        assertEquals(at(6), s("call tomorrow about friday maybe")!!.moment.start)
    }

    @Test fun theOtherHalfIsFoundWhereverItStands() {
        val x = s("Lunch on Friday with Sam at noon in the canteen")!!
        assertEquals(at(9, 12), x.moment.start)
        assertEquals(listOf("Lunch", "with Sam", "in the canteen"), x.pieces.map { it.text })
        assertEquals("on Friday at noon", x.said)
        // The time first and the day later; and a day with "all day" at the line's other end.
        assertEquals(at(9, 12), s("Sam at noon in the canteen on Friday please")!!.moment.start)
        val y = s("ganztägig Messe am Samstag in Halle 4")!!
        assertEquals(Moment(at(10), null, true, true, true, "Messe in Halle 4"), y.moment)
        // Only one of the two is there: the other is the guess, as at the ends.
        assertEquals(Moment(at(6), null, true, true, false, "call about the offer"), s("call tomorrow about the offer")!!.moment)
        assertEquals(Moment(at(5, 17), null, false, false, true, "call about the offer"), s("call at 5pm about the offer")!!.moment)
    }

    @Test fun theProbesSentencesHaveTheirDayAndTime() {
        // ("next Tuesday", typed on a Monday: the coming one, and a guess.)
        assertEquals(Moment(at(6, 15, 30), null, false, true, true, "Schedule dentist appointment at Dr. Sam office", doubts = setOf(Doubt.NEXT)), s("Schedule dentist appointment next Tuesday at 3:30pm at Dr. Sam office")!!.moment)
        assertEquals(Moment(at(9, 12), null, false, true, true, "Lunch with Sam at Cafe Luna, add to Team calendar"), s("Lunch with Sam on Friday at noon at Cafe Luna, add to Team calendar")!!.moment)
        assertEquals(Moment(at(14), at(17), true, true, true, "Team offsite in Lisbon"), s("Team offsite Oct 14 to Oct 16 in Lisbon")!!.moment)
        // (Typed on a Monday: today's, or the next one's, was not said.)
        assertEquals(Moment(at(5, 18), null, false, true, true, "Put yoga class in my Team calendar", repeats = true, doubts = setOf(Doubt.WEEK)), s("Put yoga class every Monday 6pm in my Team calendar")!!.moment)
        assertEquals(Moment(at(12, 6, 45), null, false, true, true, "Flight to Frankfurt"), s("Flight to Frankfurt on the 12th at 6:45 in the morning")!!.moment)
        assertEquals(Moment(at(6, 9), at(6, 10), false, true, true, "meeting with the team about Q3 plan room 4B"), s("meeting with the team about Q3 plan tmrw 9-10am room 4B")!!.moment)
        assertEquals(Moment(at(6, 19), null, false, true, true, "Abendessen mit Sam in den Team-Kalender eintragen"), s("Abendessen mit Sam morgen um 19 Uhr in den Team-Kalender eintragen")!!.moment)
        // („um 12“ does not say which twelve: read as noon, and a guess.)
        assertEquals(Moment(at(9, 12), null, false, true, true, "Trag Mittagessen mit Sam im Café Luna in den Teamkalender ein", vague = true), s("Trag Mittagessen mit Sam am Freitag um 12 im Café Luna in den Teamkalender ein")!!.moment)
        assertEquals(Moment(at(14, 19, 30), null, false, true, true, "Elternabend in der Schule"), s("Elternabend am 14.10. um 19:30 in der Schule")!!.moment)
    }

    @Test fun whatIsFarMoreOftenSomethingElseIsNotReadInTheMiddle() {
        // A weekday's short form that is a word too.
        assertNull(s("the cat sat on the mat"))
        assertEquals("today", s("we sat in the sun today")!!.said)
        // Two bare numbers with a dash.
        assertNull(s("buy 2-3 gifts for Sam"))
        assertEquals("tomorrow", s("buy 2-3 gifts tomorrow")!!.said)
        // At an end both are what they have always been; a whole name, a led one and a marked one are read anywhere.
        assertEquals(at(10), s("sat dinner")!!.moment.start)
        assertEquals(at(6, 9), s("party 9-11")!!.moment.start)
        assertEquals(at(10), s("dinner Saturday with Sam")!!.moment.start)
        assertEquals(at(10), s("dinner on sat with Sam")!!.moment.start)
        assertEquals(at(6, 14), s("call 2-3pm with Sam")!!.moment.start)
    }

    @Test fun aNumberAfterAtInTheMiddleIsATimeOnlyBesideItsDay() {
        // A street's number, a head count: "at 5" in the middle of a line, with a plain word after it, is no time.
        for ((line, title) in listOf("dinner at 5 Main Street tomorrow" to "dinner at 5 Main Street", "lunch at 12 Baker Street on Friday" to "lunch at 12 Baker Street",
            "party at 7 Elm St tomorrow" to "party at 7 Elm St", "we ate at 5 people tomorrow" to "we ate at 5 people", "Essen um 7 Personen morgen" to "Essen um 7 Personen")) {
            val x = s(line)!!
            assertEquals(line, title, x.moment.rest)
            assertTrue(line, x.moment.dayGiven)
            assertFalse(line, x.moment.timeGiven)
        }
        assertNull(s("dinner at 5 Main Street"))
        assertNull(s("buy 2 tickets for 7 people"))
        // Beside its day, in one expression, it is the time: before the day or after it. So is one that says am, pm or „Uhr“, and one at the line's end or start.
        assertEquals(at(6, 17), s("dinner tomorrow at 5 with Sam")!!.moment.start)
        assertEquals("dinner with Sam", s("dinner tomorrow at 5 with Sam")!!.moment.rest)
        assertEquals(at(6, 17), s("dinner at 5 tomorrow with Sam")!!.moment.start)
        assertEquals(at(9, 17), s("dinner at 5 on Friday with Sam")!!.moment.start)
        assertEquals(at(6, 19), s("Essen morgen um 19 Uhr mit Sam")!!.moment.start)
        assertEquals(at(5, 17), s("dinner at 5pm Main Street")!!.moment.start)
        assertEquals(at(5, 17), s("dinner with Sam at 5")!!.moment.start)
        assertEquals(at(5, 17), s("at 5 dinner with Sam")!!.moment.start)
        // And it is a guess wherever it is read: it does not say its half of the day.
        assertTrue(s("dinner tomorrow at 5 with Sam")!!.moment.vague)
        assertTrue(s("dinner 7 people tomorrow at 8")!!.moment.vague)
        assertFalse(s("dinner tomorrow at 5pm with Sam")!!.moment.vague)
    }

    @Test fun aWordThatSaysARepeatAnywhereInTheLine() {
        for (t in listOf("weekly sync Friday 9am", "daily standup tomorrow 9am", "standup every day at 9 tomorrow", "every other week Friday 3pm", "wöchentlich Freitag 9 Uhr",
            "monthly review on the 12th at 6pm", "Yoga jeden Montag 18 Uhr", "Sync jede Woche Freitag 9 Uhr", "Bericht monatlich am 12. um 9 Uhr", "Putzen täglich morgen 8 Uhr",
            "Review yearly Oct 12 9am", "Yoga montags morgen 18 Uhr", "run Mondays tomorrow 6pm", "call each Friday 3pm", "Team sync, weekly, Friday 9am", "Jährlich Inventur am 12.10. um 9 Uhr",
            "recurring sync Friday 9am", "repeating sync Friday 9am", "bi-weekly sync Friday 9am", "nightly backup check tomorrow 9am"))
            assertTrue(t, s(t)!!.moment.repeats)
        // Only a whole word says so.
        for (t in listOf("sync Friday 9am", "everyday carry review Friday 9am", "Jedenfalls Freitag 9 Uhr", "Dailies review Friday 9am"))
            assertFalse(t, s(t)!!.moment.repeats)
    }

    @Test fun whereTheLinesOwnWordsEndIsAnEndOfTheLine() {
        // Words after the line's own (a matched calendar's phrase) are not the line going on: what is read only at an end of the
        // line is read where its own words end.
        fun own(text: String, rest: String) = When.spot(text, now, own = text.indexOf(rest))
        assertNull(s("dinner at 7 to Team"))
        assertEquals(Moment(at(5, 19), null, false, false, true, "dinner to Team", vague = true), own("dinner at 7 to Team", "to Team")!!.moment)
        assertEquals(at(6, 9), own("standup 9-10 to the Team calendar", "to the")!!.moment.start)
        assertEquals(at(11), own("lunch sun to Team", "to Team")!!.moment.start)
        assertEquals(LocalDateTime.of(2027, 3, 4, 0, 0), own("standup 3/4 to Team", "to Team")!!.moment.start)
        assertEquals(at(9, 15), own("meet fr 3pm to Team", "to Team")!!.moment.start)
        assertEquals(listOf("meet", "to Team"), own("meet fr 3pm to Team", "to Team")!!.pieces.map { it.text })
        // In the middle of its own words the line is read as ever.
        assertNull(own("dinner at 5 Main Street to Team", "to Team"))
        assertNull(own("add 2-3 eggs to Team", "to Team"))
        assertEquals("tomorrow", own("we sat in the sun tomorrow to Team", "to Team")!!.said)
    }

    @Test fun aFractionInTheMiddleIsNoDate() {
        assertNull(s("add 1/2 cup sugar"))
        assertNull(s("score 3/4 for the team"))
        assertEquals("tomorrow", s("add 1/2 cup sugar tomorrow")!!.said)
        // After a word that leads into a date it is one; so it is beside a time, and at the line's ends, as it has always been.
        assertEquals(LocalDateTime.of(2027, 3, 4, 0, 0), s("dinner on 3/4 with Sam")!!.moment.start)
        assertEquals(LocalDateTime.of(2027, 4, 3, 0, 0), s("Essen am 3.4. mit Sam")!!.moment.start)
        assertEquals(LocalDateTime.of(2027, 3, 4, 17, 0), s("dinner 3/4 5pm with Sam")!!.moment.start)
        assertEquals(LocalDateTime.of(2027, 3, 4, 0, 0), s("3/4 dinner with Sam")!!.moment.start)
        assertEquals(LocalDateTime.of(2027, 3, 4, 0, 0), s("dinner with Sam 3/4")!!.moment.start)
        // (A date with its dots is no fraction.)
        assertEquals(at(14), s("Abgabe 14.10. Bericht")!!.moment.start)
    }

    @Test fun twoBareNumbersAreNotTheTimeOfADayThatStandsElsewhere() {
        // "1-2 eggs tomorrow": the day is read, the numbers stay.
        assertEquals(Moment(at(6), null, true, true, false, "1-2 eggs"), s("1-2 eggs tomorrow")!!.moment)
        assertEquals(Moment(at(6), null, true, true, false, "lunch 1-2"), s("tomorrow lunch 1-2")!!.moment)
        assertEquals(Moment(at(5, 17), null, false, false, true, "1-2 eggs"), s("1-2 eggs 5pm")!!.moment)
        // Beside the day they are its time, and alone at an end of a line that says nothing else they are one: both as ever, and both a guess at the half of the day.
        assertEquals(Moment(at(6, 13), at(6, 14), false, true, true, "lunch", vague = true), s("lunch tomorrow 1-2")!!.moment)
        assertEquals(Moment(at(6, 9), at(6, 11), false, false, true, "party", vague = true), s("party 9-11")!!.moment)
    }

    @Test fun afterWordsThatWereTakenOffTheStartIsTheMiddle() {
        // What a sentence's cue stood before ("Add …") is not the line's start: what is not read in the middle is not read there.
        fun led(text: String) = When.spot(text, now, led = true)
        assertEquals("tomorrow", led("1-2 eggs tomorrow")!!.said)
        assertNull(led("1-2 eggs"))
        assertNull(led("1/2 cup sugar"))
        assertNull(led("sun cream order"))
        assertEquals(Moment(at(5, 17), null, false, false, true, "sun cream order"), led("sun cream order 5pm")!!.moment)
        assertNull(led("at 5 Main Street"))
        // What is read anywhere is read there too.
        assertEquals(at(6, 19), led("tomorrow 7pm dinner")!!.moment.start)
        assertEquals(at(10), led("Saturday dinner")!!.moment.start)
        assertEquals(at(10), led("on sat dinner")!!.moment.start)
    }

    @Test fun allDayAloneIsNothingInTheMiddleEither() {
        for (t in listOf("I worked all day", "I worked all day on it", "what to eat all day", "Meeting den ganzen Tag im Büro")) assertNull(t, s(t))
        // With a day anywhere in the line it is that day's.
        assertEquals(Moment(at(6), null, true, true, true, "Inventory with Sam"), s("Inventory all day tomorrow with Sam")!!.moment)
        assertEquals(Moment(at(6), null, true, true, true, "Inventory with Sam"), s("Inventory tomorrow with Sam all day")!!.moment)
        // And a time in such a line is read as if the words were not there.
        assertEquals(Moment(at(5, 17), null, false, false, true, "I worked all day"), s("I worked all day 5pm")!!.moment)
    }

    @Test fun aLongLineIsReadAtItsEndsOnly() {
        val filler = "word ".repeat(70)
        assertNull(s(filler + "tomorrow " + filler))
        assertEquals(at(6), s("tomorrow $filler")!!.moment.start)
        assertEquals(at(5, 17), s(filler + "5pm")!!.moment.start)
        assertEquals(at(6, 17), s("tomorrow " + filler + "5pm")!!.moment.start)
    }

    @Test fun nothingThrows() {
        val bits = listOf("fri", "3pm", "in", "20m", "at", "um", "am", "on", "-", "9", "25", ":", "@", "uhr", "10/3", "3.10.", "oct", "9-9:30", "noon", "tomorrow", "x", "🎉",
            "to", "from", "bis", "von", "the", "12th", "every", "jeden", "all", "day", "ganztägig", "morning", "abends", "den", "14-16", "of", "tmrw", "add", "calendar", ",", "sat")
        val random = java.util.Random(11)
        repeat(20_000) {
            val text = List(1 + random.nextInt(9)) { bits[random.nextInt(bits.size)] }.joinToString(if (random.nextBoolean()) " " else "  ")
            val x = When.spot(text, now) ?: return@repeat
            // Every piece is a piece of the line, where it says it is; and the rest is the pieces.
            for (p in x.pieces) { assertFalse(text, p.text.isEmpty()); assertEquals(text, p.text, text.substring(p.start, p.start + p.text.length)) }
            assertEquals(text, x.pieces.joinToString(" ") { it.text }, x.moment.rest)
        }
    }
}
