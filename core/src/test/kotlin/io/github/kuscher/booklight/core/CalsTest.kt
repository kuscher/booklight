package io.github.kuscher.booklight.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What Enter on an event's row sends or writes: the calendar link, the values for a direct save, the calendar it goes to, and when it is saved at all. */
class CalsTest {
    private val now = LocalDateTime.of(2026, 10, 5, 16, 20)
    private val berlin = ZoneId.of("Europe/Berlin")

    private val sam = Cal(1, "Sam", 0xFF3F51B5.toInt(), "sam@example.com", primary = true)
    private val team = Cal(2, "Team", 0xFF0B8043.toInt(), "team@group.calendar.google.com")
    private val holidays = Cal(4, "Holidays", 0xFF7986CB.toInt(), "holidays@group.v.calendar.google.com", writable = false)
    private val mine = listOf(team, sam, holidays)

    private fun r(text: String) = Sentence.read(text, now, mine)

    @Test fun theLinkCarriesTheTitleTheTimeInUtcAndTheCalendar() {
        val got = r("Add dinner with Sam tomorrow at 7pm to Team calendar")
        // Seven in the evening in Berlin's summer time is five in UTC.
        assertEquals("https://calendar.google.com/calendar/render?action=TEMPLATE&text=dinner%20with%20Sam&dates=20261006T170000Z/20261006T180000Z&src=team%40group.calendar.google.com",
            Cals.link(got.draft, got.calendar, berlin))
        // No calendar named: none is sent, and the Calendar app chooses.
        assertEquals("https://calendar.google.com/calendar/render?action=TEMPLATE&text=Call%20mom&dates=20261011T080000Z/20261011T090000Z", Cals.link(r("Call mom Sunday 10am").draft, null, berlin))
        // A place.
        assertEquals("https://calendar.google.com/calendar/render?action=TEMPLATE&text=Dentist&dates=20261009T130000Z/20261009T140000Z&location=Main%20St%2012",
            Cals.link(r("Fri 3pm Dentist @ Main St 12").draft, null, berlin))
    }

    @Test fun anAllDayEventIsItsFirstDayAndTheDayAfterItsLast() {
        assertEquals("${Cals.TEMPLATE}&text=Birthday%20party%20for%20Sam&dates=20261103/20261104", Cals.link(r("Birthday party for Sam on November 3rd all day").draft, null, berlin))
        assertEquals("${Cals.TEMPLATE}&text=Team%20offsite&dates=20261014/20261017&location=Lisbon", Cals.link(r("Team offsite Oct 14 to Oct 16 @ Lisbon").draft, null, berlin))
        // The days are the days that were typed, wherever the device is: no zone moves an all-day event to the day before.
        for (zone in listOf("America/Los_Angeles", "Pacific/Auckland", "UTC", "Asia/Kolkata"))
            assertEquals(zone, "${Cals.TEMPLATE}&text=Birthday%20party%20for%20Sam&dates=20261103/20261104", Cals.link(r("Birthday party for Sam on November 3rd all day").draft, null, ZoneId.of(zone)))
    }

    @Test fun aMomentIsTheSameMomentInEveryZone() {
        val e = r("Call mom Sunday 10am").draft
        // Ten in the morning in Los Angeles is five in the afternoon in UTC; in Auckland it is nine in the evening of the day before.
        assertTrue(Cals.link(e, null, ZoneId.of("America/Los_Angeles")).endsWith("&dates=20261011T170000Z/20261011T180000Z"))
        assertTrue(Cals.link(e, null, ZoneId.of("Pacific/Auckland")).endsWith("&dates=20261010T210000Z/20261010T220000Z"))
        // Over the night the clocks go back (25 October 2026 in Berlin): each end is its own moment.
        val night = EventDraft("Night shift", LocalDateTime.of(2026, 10, 24, 22, 0), LocalDateTime.of(2026, 10, 25, 3, 30), false, "", dayGiven = true, timeGiven = true)
        assertTrue(Cals.link(night, null, berlin).endsWith("&dates=20261024T200000Z/20261025T023000Z"))
        // And what is written is those same moments.
        val w = Cals.write(night, sam, berlin)
        assertEquals(Instant.parse("2026-10-24T20:00:00Z").toEpochMilli(), w.start)
        assertEquals(Instant.parse("2026-10-25T02:30:00Z").toEpochMilli(), w.end)
        // West and east of UTC, what is written is the moment the link says.
        for ((zone, start) in listOf("America/Los_Angeles" to "2026-10-11T17:00:00Z", "Pacific/Auckland" to "2026-10-10T21:00:00Z", "Asia/Kolkata" to "2026-10-11T04:30:00Z")) {
            val there = Cals.write(e, sam, ZoneId.of(zone))
            assertEquals(zone, Instant.parse(start).toEpochMilli(), there.start)
            assertEquals(zone, Instant.parse(start).plusSeconds(3600).toEpochMilli(), there.end)
            assertEquals(zone, zone, there.zone)
        }
    }

    @Test fun aTimeTheClocksSkipOrShowTwice() {
        // The night the clocks go forward (29 March 2026 in Berlin) has no half past two: it is read as half past three, an hour on,
        // in the link and in what is written alike.
        val gap = EventDraft("Night shift", LocalDateTime.of(2026, 3, 29, 2, 30), LocalDateTime.of(2026, 3, 29, 3, 30), false, "", dayGiven = true, timeGiven = true)
        assertTrue(Cals.link(gap, null, berlin).endsWith("&dates=20260329T013000Z/20260329T013000Z"))
        assertEquals(Instant.parse("2026-03-29T01:30:00Z").toEpochMilli(), Cals.write(gap, sam, berlin).start)
        // The night they go back (25 October 2026) has half past two twice: the first of the two, in both.
        val twice = EventDraft("Night shift", LocalDateTime.of(2026, 10, 25, 2, 30), LocalDateTime.of(2026, 10, 25, 3, 30), false, "", dayGiven = true, timeGiven = true)
        assertTrue(Cals.link(twice, null, berlin).endsWith("&dates=20261025T003000Z/20261025T023000Z"))
        assertEquals(Instant.parse("2026-10-25T00:30:00Z").toEpochMilli(), Cals.write(twice, sam, berlin).start)
        assertEquals(Instant.parse("2026-10-25T02:30:00Z").toEpochMilli(), Cals.write(twice, sam, berlin).end)
    }

    @Test fun whatATitleHoldsIsWrittenIntoTheLinkSafely() {
        val e = EventDraft("Q&A: 50% off? #1 + “more” für Jürgen 🎉", now, now.plusHours(1), false, "A&B / C=D", dayGiven = true, timeGiven = true)
        val link = Cals.link(e, Cal(9, "x", owner = "a+b&c=d@example.com"), berlin)
        assertEquals("${Cals.TEMPLATE}&text=Q%26A%3A%2050%25%20off%3F%20%231%20%2B%20%E2%80%9Cmore%E2%80%9D%20f%C3%BCr%20J%C3%BCrgen%20%F0%9F%8E%89&dates=20261005T142000Z/20261005T152000Z" +
            "&location=A%26B%20%2F%20C%3DD&src=a%2Bb%26c%3Dd%40example.com", link)
        // Nothing typed can add a parameter of its own: after the fixed start there are exactly these four.
        assertEquals(listOf("text", "dates", "location", "src"), link.removePrefix(Cals.TEMPLATE).split('&').filter { it.isNotEmpty() }.map { it.substringBefore('=') })
        // A line break, a tab, and words that look like a parameter: all of it is the title's own text. A calendar's address with a # in it stays one address.
        val sly = Cals.link(e.copy(title = "x&src=a@b.c\nnext\tline", place = "&dates=1"), Cal(9, "x", owner = "en.usa#holiday@group.v.calendar.google.com"), berlin)
        assertEquals("${Cals.TEMPLATE}&text=x%26src%3Da%40b.c%0Anext%09line&dates=20261005T142000Z/20261005T152000Z&location=%26dates%3D1&src=en.usa%23holiday%40group.v.calendar.google.com", sly)
        assertEquals(listOf("text", "dates", "location", "src"), sly.removePrefix(Cals.TEMPLATE).split('&').filter { it.isNotEmpty() }.map { it.substringBefore('=') })
        // No title, no place, no owner: those parameters are not there.
        assertEquals("${Cals.TEMPLATE}&dates=20261005T142000Z/20261005T152000Z", Cals.link(e.copy(title = " ", place = ""), Cal(9, "x"), berlin))
    }

    @Test fun whatIsWrittenIsWhatTheRowShows() {
        val got = r("Add dinner with Sam tomorrow at 7pm to Team calendar @ Cafe Luna")
        assertEquals(EventWrite(2, "dinner with Sam", Instant.parse("2026-10-06T17:00:00Z").toEpochMilli(), Instant.parse("2026-10-06T18:00:00Z").toEpochMilli(), false, "Europe/Berlin", "Cafe Luna"),
            Cals.write(got.draft, got.calendar!!, berlin))
        // All day: midnight to midnight in UTC, said so, whatever the device's zone.
        val day = r("Birthday party for Sam on November 3rd all day").draft
        for (zone in listOf(berlin, ZoneId.of("America/Los_Angeles"), ZoneId.of("Pacific/Auckland")))
            assertEquals(EventWrite(1, "Birthday party for Sam", Instant.parse("2026-11-03T00:00:00Z").toEpochMilli(), Instant.parse("2026-11-04T00:00:00Z").toEpochMilli(), true, "UTC", ""), Cals.write(day, sam, zone))
    }

    @Test fun whereANewEventGoes() {
        // The calendar that was named.
        assertEquals(team, Cals.target(team, mine, ""))
        assertEquals(team, Cals.target(team, mine, sam.owner))
        // None named: the one chosen in the window; until one is chosen, the account's own; without one, the first that can be written.
        assertEquals(team, Cals.target(null, mine, team.owner))
        assertEquals(sam, Cals.target(null, mine, ""))
        assertEquals(team, Cals.target(null, listOf(holidays, team), ""))
        // The chosen one is gone from this device (another device's settings came with a backup): the account's own again.
        assertEquals(sam, Cals.target(null, mine, "gone@group.calendar.google.com"))
        // A calendar that cannot be written is never the one: not when named, not when chosen, not as the only one.
        assertNull(Cals.target(holidays, mine, ""))
        assertEquals(sam, Cals.target(null, mine, holidays.owner))
        assertNull(Cals.target(null, listOf(holidays), ""))
        assertNull(Cals.target(null, emptyList(), ""))
        // Two accounts, each with a calendar of its own: the first in the list is the one, whichever way the list is turned; one that was chosen wins over both.
        val second = Cal(5, "Sam too", 0, "sam.too@example.com", primary = true)
        assertEquals(sam, Cals.target(null, listOf(team, sam, second), ""))
        assertEquals(second, Cals.target(null, listOf(second, team, sam), ""))
        assertEquals(second, Cals.target(null, listOf(team, sam, second), second.owner))
        assertEquals(team, Cals.target(null, listOf(team, sam, second), team.owner))
        // Two calendars with one owner's address (the chosen one): the first of them.
        val twin = Cal(6, "Team again", 0, team.owner)
        assertEquals(team, Cals.target(null, listOf(sam, team, twin), team.owner))
        assertEquals(twin, Cals.target(null, listOf(sam, twin, team), team.owner))
        // An own calendar that cannot be written is passed over for the next own one.
        assertEquals(second, Cals.target(null, listOf(sam.copy(writable = false), team, second), ""))
    }

    @Test fun aGuessIsNeverSaved() {
        fun saves(text: String, on: Boolean = true, target: Cal? = sam) = Cals.saves(r(text), on, target)
        // Saved: a day and a time that were both typed; a day with "all day"; a range of days.
        for (t in listOf("Add dinner with Sam tomorrow at 7pm", "Call mom Sunday 10am", "Birthday party for Sam on November 3rd all day", "Team offsite Oct 14 to Oct 16", "Standup tomorrow from 9am to 10am",
            "Abendessen morgen um 19 Uhr", "Abendessen morgen 7 Uhr abends", "Dinner tomorrow at 7 in the evening", "Lunch on Friday at noon", "Add dinner tomorrow 7pm to Team", "Add dinner tomorrow 7pm to the Team calendar",
            "Friseur übermorgen 9 Uhr", "Friseur morgen um 9:30 Uhr", "Workshop morgen von 10 bis 11 Uhr", "Standup tomorrow 09:30", "dentist the day after tomorrow 3pm", "standup Wednesday 28 October 10:30am"))
            assertTrue(t, saves(t))
        // Never: the day is a guess (only a time was typed), the time is (only a day), both are, or the day is the next of a repeat.
        for (t in listOf("Call mom 10am", "Offsite tomorrow", "Dentist", "Put yoga class every Monday 6pm in my Team calendar", "Inventory all day"))
            assertFalse(t, saves(t))
        // Nor a time that does not say its half of the day: "at 7" was read as the evening, and nobody typed the evening.
        // Nor „8 Uhr“ and „um 7 Uhr“: written for the morning, and said for the evening too.
        for (t in listOf("Add dinner with Sam tomorrow at 7", "Dinner tomorrow 7:30", "Abendessen morgen um 7", "Friseur übermorgen um 8", "Standup tomorrow from 9 to 10", "Standup tomorrow 9-10", "Lunch Friday at 12",
            "Friseur übermorgen 8 Uhr", "Abendessen morgen um 7 Uhr", "Termin morgen 2-3 Uhr"))
            assertFalse(t, saves(t))
        // Nor anything else of the day or the time that was the parser's to choose ([Doubt]): a year, a month, "next", a weekday by two letters,
        // which night, a word beside the day that changes it, a length that was not read, an end that is its start.
        for (t in listOf("dentist Oct 4 3pm", "dinner 12/10 7pm", "dinner next Tuesday 7pm", "Zahnarzt nächsten Dienstag 15:30 Uhr", "things to do 3pm", "Fr 15 Uhr Zahnarzt", "party Friday midnight",
            "party tomorrow 12 at night", "review a week from tomorrow 3pm", "report until Friday 5pm", "dinner Friday after next 7pm", "lunch not tomorrow 1pm", "meeting tomorrow 3pm for 2 hours",
            "dinner Sat tomorrow 5pm", "standup tomorrow 9am-9am", "standup Thursday 28 October 10:30am"))
            assertFalse(t, saves(t))
        // Nor a line that asks for a repeat by any word, one that holds a second day or time, or a day that is only a number beside a time.
        for (t in listOf("weekly sync Friday 9am", "daily standup tomorrow 9am", "Sync wöchentlich Freitag 15 Uhr", "monthly review on the 12th at 6pm", "Mon 3pm Tue 4pm", "Meet Friday at 5pm or Saturday at 6pm",
            "Sprint Oct 5 to Dec 20 9am", "Dinner tomorrow 7am in the evening", "tomorrow 3pm all day", "room 3rd 3pm"))
            assertFalse(t, saves(t))
        // Nor what is no event at all, or one whose only time is a house's number.
        for (t in listOf("I worked all day", "dinner at 5 Main Street tomorrow", "add 1/2 cup sugar", "add 1-2 eggs tomorrow"))
            assertFalse(t, saves(t))
        // Nor while a calendar's name is still being typed: the row shows the calendar by its first letters, and that is a guess until the name is whole.
        val begun = r("Add dinner tomorrow 7pm to te")
        assertEquals(team, begun.calendar)
        assertEquals("am", begun.rest)
        assertTrue(begun.draft.sure)
        assertFalse(Cals.saves(begun, true, team))
        assertTrue(Cals.saves(r("Add dinner tomorrow 7pm to Team"), true, team))
        // Nor an event that does not end after it starts, however it came to be.
        val sure = r("Call mom Sunday 10am")
        assertTrue(Cals.saves(sure, true, sam))
        assertFalse(Cals.saves(sure.copy(draft = sure.draft.copy(end = sure.draft.start)), true, sam))
        assertFalse(Cals.saves(sure.copy(draft = sure.draft.copy(end = sure.draft.start.minusHours(1))), true, sam))
        // Nor into the calendar new events go to where the sentence named another, one that is none of the user's.
        val other = r("Add dinner tomorrow 7pm to the Garden calendar")
        assertTrue(other.named && other.draft.sure)
        assertFalse(Cals.saves(other, true, sam))
        // Nor a line typed without the keyword that begins like an everyday search, however sure its day and time; under the keyword it is an event.
        assertFalse(saves("book flight to boston friday 9am"))
        assertFalse(saves("Schedule dentist Friday 3pm"))
        assertTrue(Cals.saves(Sentence.read("book flight to boston friday 9am", now, mine, keyword = true), true, sam))
        // Never without the switch, without a calendar to write to, into one that cannot be written, or with nothing to call it.
        assertFalse(saves("Call mom Sunday 10am", on = false))
        assertFalse(saves("Call mom Sunday 10am", target = null))
        assertFalse(saves("Call mom Sunday 10am", target = holidays))
        assertFalse(saves("Sunday 10am"))
        assertFalse(saves("Add tomorrow at 7pm"))
    }
}
