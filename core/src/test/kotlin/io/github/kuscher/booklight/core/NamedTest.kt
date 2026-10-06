package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A calendar by its name, where the name is an odd one: what the row's list and the completion write into the field
 * must read back as the calendar that was chosen ([Sentence.put], [Cals.marked]), and a calendar is read only where
 * the sentence says it is one. "Now" is Tuesday 6 October 2026, ten in the morning.
 */
class NamedTest {
    private val now = LocalDateTime.of(2026, 10, 6, 10, 0)
    private fun at(day: Int, hour: Int = 0, minute: Int = 0) = LocalDateTime.of(2026, 10, day, hour, minute)

    private val sam = Cal(1, "Sam", owner = "sam@example.com", primary = true)
    private val team = Cal(2, "Team")
    private val trips = Cal(3, "Team trips")
    private val tigers = Cal(4, "The Tigers")
    private val family = Cal(5, "Family Calendar")
    private val familie = Cal(6, "Familienkalender")
    private val workCalendar = Cal(7, "Work calendar")
    private val work = Cal(8, "Work")
    private val weekly = Cal(9, "Weekly sync")
    private val daily = Cal(10, "Daily")
    private val nine = Cal(11, "Plan of all the works in house and garden")
    private val two = Cal(12, "Team 2")
    private val muller = Cal(13, "Müller")
    private val cafe = Cal(14, "Café Luna")
    private val onCall = Cal(15, "On call")
    private val toDo = Cal(16, "To do")
    private val teamAgain = Cal(17, "Team")
    private val signs = Cal(18, "🎉🎉")
    private val friday = Cal(19, "Friday")
    private val placed = Cal(20, "Home @ Sam")
    private val holidays = Cal(21, "Holidays", writable = false)

    /** Every odd kind of name at once: an article first, "Calendar" last, a repeat word, nine words, a digit, an accent, two that begin alike, one of signs alone, two of one name. */
    private val odd = Cals.marked(listOf(sam, team, trips, tigers, family, familie, workCalendar, work, weekly, daily, nine, two, muller, cafe, onCall, toDo, teamAgain, signs, friday, placed, holidays), now)
    private fun of(c: Cal) = odd.first { it.id == c.id }

    private fun r(text: String, calendars: List<Cal> = odd) = Sentence.read(text, now, calendars)

    @Test fun aCalendarWhoseOwnNameEndsInTheWordCalendar() {
        val mine = listOf(sam, family, familie, workCalendar)
        for ((text, cal) in listOf("Add dinner tomorrow 7pm to the Family Calendar" to family, "Add dinner tomorrow 7pm to Family Calendar" to family, "Add dinner tomorrow 7pm to family calendar" to family,
            "Add dinner tomorrow 7pm to the Family Calendar calendar" to family, "Trag Essen morgen um 19 Uhr in den Familienkalender ein" to familie, "Essen morgen um 19 Uhr im Familienkalender" to familie,
            "Add dinner tomorrow 7pm to Work calendar" to workCalendar, "Add dinner tomorrow 7pm to my work calendar" to workCalendar, "Add dinner to the Family Calendar tomorrow 7pm" to family)) {
            val got = r(text, mine)
            assertEquals(text, cal, got.calendar)
            assertEquals(text, if (text.contains("Essen")) "Essen" else "dinner", got.draft.title)
            assertFalse(text, got.named)
            assertNull(text, got.rest)
            assertTrue(text, Cals.saves(got, true, Cals.target(got.calendar, mine, "")))
        }
        // The start of such a name is completed, and what Right then writes reads as that calendar.
        val begun = r("Add dinner tomorrow 7pm to fam", mine)
        assertEquals(family, begun.calendar)
        assertEquals("ily Calendar", begun.rest)
        val taken = Sentence.put("Add dinner tomorrow 7pm to fam", family.name, "to", now, mine)
        assertEquals("Add dinner tomorrow 7pm to Family Calendar", taken)
        assertEquals(family, r(taken, mine).calendar)
        assertNull(r(taken, mine).rest)
        // The name as it was typed comes before the name without the word: with a calendar "Work" and one "Work calendar", each by its own.
        val both = listOf(sam, workCalendar, work)
        assertEquals(workCalendar, r("Add sync tomorrow 9am to Work calendar", both).calendar)
        assertEquals(work, r("Add sync tomorrow 9am to Work", both).calendar)
        assertEquals(work, r("Add sync tomorrow 9am to the Work", both).calendar)
        // While the name's own last word is typed the calendar stays in the row: its start, then the whole name.
        for (n in "Add dinner tomorrow 7pm to Family".length.."Add dinner tomorrow 7pm to Family Calendar".length)
            assertEquals(family, r("Add dinner tomorrow 7pm to Family Calendar".take(n), mine).calendar)
    }

    @Test fun whichCalendarsCanBeNamed() {
        // Not: the second of two with one name, a name of signs alone, a name that reads as a day, a name the place's sign cuts in two.
        assertEquals(setOf(teamAgain.id, signs.id, friday.id, placed.id), odd.filter { !it.nameable }.map { it.id }.toSet())
        // Marking changes nothing else of a calendar, and nothing of the list's order.
        assertEquals(listOf(sam, team, trips), Cals.marked(listOf(sam, team, trips), now))
        assertEquals(odd.map { it.id }, (1L..21L).toList())
        assertEquals(emptyList<Cal>(), Cals.marked(emptyList(), now))
    }

    @Test fun whatIsWrittenIntoTheFieldReadsBackAsThatCalendar() {
        val texts = listOf("Add dinner tomorrow 7pm", "Add dinner tomorrow 7pm @ Cafe Luna", "Add dinner tomorrow 7pm to tea", "dinner", "", "@ Cafe Luna", "Add @ Cafe Luna", "Add dinner tomorrow 7pm to the Team calendar",
            "Add dinner to the Team calendar tomorrow 7pm", "Add dinner tomorrow 7pm @ Cafe Luna to Team", "Add dinner tomorrow 7pm to The Tigers", "Add dinner tomorrow 7pm to Family Calendar",
            "Add dinner tomorrow 7pm to the Garden calendar", "Add dinner to the Garden calendar tomorrow 7pm", "Abendessen morgen um 19 Uhr im Teamkalender", "Abendessen im Teamkalender morgen um 19 Uhr",
            "Abendessen in den Team-Kalender morgen um 19 Uhr", "Add dinner tomorrow 7pm to the", "Add dinner next Friday 7pm to Weekly sync @ Cafe Luna")
        for (c in odd.filter { it.nameable }) {
            for (text in texts) for (keyword in listOf(false, true)) {
                val written = Sentence.put(text, c.name, "to", now, odd, keyword)
                val got = Sentence.read(written, now, odd, keyword)
                assertSame("$text + ${c.name} = $written", c, got.calendar)
                assertNull(written, got.rest)
                assertFalse(written, got.named)
            }
            // In German the lead word is „in“, and it stands before the verb's other half.
            val written = Sentence.put("Trag Essen morgen um 12 Uhr ein", c.name, "in", now, odd)
            assertEquals("Trag Essen morgen um 12 Uhr in ${c.name} ein", written)
            assertSame(written, c, r(written).calendar)
            assertEquals(written, "Essen", r(written).draft.title)
        }
        // One reading of the sentence writes every name as each reading of it would.
        for (text in texts) { val write = Sentence.putter(text, "to", now, odd); for (c in odd) assertEquals("$text + ${c.name}", Sentence.put(text, c.name, "to", now, odd), write(c.name)) }
        // A line that is only a place: the name stands before the place's sign, with a space, and both read back.
        assertEquals("to Team @ Cafe Luna", Sentence.put("@ Cafe Luna", "Team", "to", now, odd, keyword = true))
        assertEquals("Add to Team @ Cafe Luna", Sentence.put("Add @ Cafe Luna", "Team", "to", now, odd))
        assertEquals("Cafe Luna", r("Add to Team @ Cafe Luna").draft.place)
        assertSame(of(team), r("Add to Team @ Cafe Luna").calendar)
    }

    /** Lines with a day, a time, both or neither, many of them in the forms that are read only at a line's end ("at 7", "9-10", "3/4", "sun", "fr 3pm"). */
    private val lines = listOf("Add dinner tomorrow at 7", "Add dinner at 7", "Add dinner tomorrow with Sam at 7", "Add dinner at 7 tomorrow", "Add dinner tomorrow 7pm", "Add dinner tomorrow 7pm to te",
        "Add dinner tomorrow 7pm to Team", "Add dinner to the Team calendar tomorrow 7pm", "Add dinner tomorrow 7pm @ Cafe Luna", "Add dinner tomorrow 7pm @ Cafe Luna to Team", "Add dinner tomorrow", "Add dinner Friday",
        "Add dinner on Friday", "Add standup tomorrow 9", "Add standup tomorrow at 9 sharp", "Add standup tomorrow 9-10", "Add standup 9-10", "Add standup tomorrow 9am", "Add lunch sun", "Add pictures of the sun",
        "Add standup 3/4", "Add offsite Oct 14", "Add offsite Wednesday", "Add offsite tomorrow all day", "Add dinner um 7", "Add dinner morgen um 7", "Trag Abendessen morgen um 7 ein", "Trag Abendessen um 7 ein",
        "Add dinner tomorrow at 19", "Add dinner at 19", "Add meet fr 3pm", "Add meet 3pm fr", "Add standup tomorrow 9am weekly")

    /** Calendars called like ordinary words, days and numbers. */
    private val words = Cals.marked(listOf(sam, team, work, workCalendar, Cal(31, "May"), Cal(32, "Mo"), Cal(33, "Tonight"), Cal(34, "Evening"), Cal(35, "2027"), daily, Cal(36, "Lunch"), Cal(37, "Home"),
        Cal(38, "Main Street"), Cal(39, "Kids"), friday, holidays), now)

    @Test fun aCalendarWrittenIntoALineKeepsItsDayAndItsTime() {
        // What the row's list and the completion write is the same event in another calendar: the day, the time and how sure they
        // are stay what they were. A calendar's phrase after the line's own words is not the line going on.
        assertEquals(33, lines.size)
        // (And the keyword's own line with two bare numbers; and a line with two times, of which the one at the line's end is read.)
        val more = listOf("tomorrow standup 9-10", "Add call noon today 15:00", "Add call Friday 3pm or Saturday 4pm")
        for (list in listOf(odd, words)) for (c in list.filter { it.nameable && it.writable }) for (keyword in listOf(false, true)) for (text in lines + more) {
            val before = Sentence.read(text, now, list, keyword)
            val written = Sentence.put(text, c.name, "to", now, list, keyword)
            val after = Sentence.read(written, now, list, keyword)
            val said = "$text + ${c.name} = $written" + if (keyword) " (keyword)" else ""
            assertSame(said, c, after.calendar)
            assertNull(said, after.rest)
            assertEquals(said, before.draft.start, after.draft.start)
            assertEquals(said, before.draft.end, after.draft.end)
            assertEquals(said, before.draft.allDay, after.draft.allDay)
            assertEquals(said, before.draft.dayGiven to before.draft.timeGiven, after.draft.dayGiven to after.draft.timeGiven)
            assertEquals(said, before.draft.sure, after.draft.sure)
            // And a line that was offered without the keyword still is.
            if (!keyword) assertEquals(said, Sentence.reads(before), Sentence.reads(after))
        }
        // In German the lead word is „in“.
        for (text in listOf("Trag Abendessen um 7 ein", "Trag Abendessen morgen um 7 ein")) {
            val after = r(Sentence.put(text, "Team", "in", now, odd))
            assertEquals(text, r(text).draft.start, after.draft.start)
            assertSame(text, of(team), after.calendar)
        }
    }

    @Test fun aCalendarsPhraseAtTheLinesEndIsNotTheLineGoingOn() {
        val mine = listOf(sam, team, holidays)
        // "at 7" is a time at a line's end, and a house's number before a street: before a calendar's phrase it is the time.
        val got = r("Add dinner at 7 to Team", mine)
        assertEquals(EventDraft("dinner", at(6, 19), at(6, 20), false, "", dayGiven = false, timeGiven = true, vague = true), got.draft)
        assertEquals(team, got.calendar)
        assertTrue(Sentence.reads(got))
        // A guess as ever: the half of the day, and the day.
        assertFalse(Cals.saves(got, true, team))
        // So are the other three things that are read only at a line's end: two bare numbers, two with a slash, a short weekday, a weekday by two letters with its time.
        for ((text, start, allDay) in listOf(Triple("Add dinner tomorrow with Sam at 7 to Team", at(7, 19), false), Triple("Add standup 9-10 to Team", at(7, 9), false), Triple("Add lunch sun to Team", at(11), true),
            Triple("Add meet fr 3pm to Team", at(9, 15), false), Triple("Add dinner at 7 to the Team calendar", at(6, 19), false), Triple("Add dinner at 7 in Team", at(6, 19), false), Triple("Add dinner at 7, add to Team", at(6, 19), false),
            Triple("Add dinner at 7 to te", at(6, 19), false), Triple("Add dinner at 7 to Holidays", at(6, 19), false), Triple("Trag Abendessen um 7 in den Team-Kalender ein", at(6, 19), false),
            Triple("Add dinner at 7 to Team @ Cafe Luna", at(6, 19), false))) {
            val read = r(text, mine)
            assertEquals(text, start, read.draft.start)
            assertEquals(text, allDay, read.draft.allDay)
            assertTrue(text, read.calendar != null)
            assertFalse(text, read.draft.sure)
        }
        assertEquals(LocalDateTime.of(2027, 3, 4, 0, 0), r("Add standup 3/4 to Team", mine).draft.start)
        // An hour past twelve says its half of the day, with a day beside it: what the line says, and sure.
        val nineteen = r("Add dinner tomorrow with Sam at 19 to Team", mine)
        assertEquals(at(7, 19), nineteen.draft.start)
        assertTrue(Cals.saves(nineteen, true, team))
        // Under the keyword too: the line it has always read keeps its time when a calendar is chosen for it.
        for (text in listOf("tomorrow standup 9-10 to Team", "lunch at 12 to Team", "dinner at 7 to Team", "standup 9-10 to the Team calendar")) {
            val read = Sentence.read(text, now, mine, keyword = true)
            assertTrue(text, read.draft.timeGiven)
            assertEquals(text, team, read.calendar)
        }
        assertEquals(at(7, 9), Sentence.read("tomorrow standup 9-10 to Team", now, mine, keyword = true).draft.start)
        // What follows is no calendar of yours: the line goes on, and "at 5" is a house's number as ever.
        for (text in listOf("Add dinner at 5 on Main Street", "Add dinner at 5 to go", "Add dinner at 7 to the Foo calendar", "Add dinner at 7 to", "Add dinner at 7 to t", "Add ship v2 at 5 to launch")) {
            val read = r(text, mine)
            assertFalse(text, read.draft.timeGiven)
            assertFalse(text, Sentence.reads(read))
        }
        // And where the calendar's own words are read as the day, the line is read as it stands: no time out of its middle.
        val day = r("Add dinner at 19 to Friday", listOf(sam, friday))
        assertEquals(at(9), day.draft.start)
        assertFalse(day.draft.timeGiven)
    }

    @Test fun afterOnAndInACalendarIsNamedAsItIsCalled() {
        val mine = listOf(sam, team, tigers)
        // "on Teams" is where the standup is held: after "on" and "in" a calendar is its name as the calendar has it, not the
        // plural or the singular of it. The words stay in the title.
        for ((text, title) in listOf("Add standup tomorrow 9am on Teams" to "standup on Teams", "Add standup tomorrow 9am in teams" to "standup in teams", "Add match tomorrow 7pm in Tiger" to "match in Tiger",
            "Add match tomorrow 7pm on the Tiger" to "match on the Tiger", "Trag Standup morgen um 9 Uhr in Teams ein" to "Standup in Teams")) {
            val got = r(text, mine)
            assertNull(text, got.calendar)
            assertNull(text, got.rest)
            assertFalse(text, got.named)
            assertEquals(text, title, got.draft.title)
        }
        assertEquals("Room 4 in Teams", r("Add standup tomorrow 9am @ Room 4 in Teams", mine).draft.place)
        // The name itself is the calendar after any of them, with an article or without; so is any form of it with the word
        // "calendar", or after "to", "into" and "onto".
        for ((text, cal) in listOf("Add standup tomorrow 9am on Team" to team, "Add standup tomorrow 9am in team" to team, "Add standup tomorrow 9am in the Team" to team, "Add standup tomorrow 9am on the Teams calendar" to team,
            "Add standup tomorrow 9am in my teams calendar" to team, "Add standup tomorrow 9am on Teams calendar" to team, "Add standup tomorrow 9am to Teams" to team, "Add standup tomorrow 9am into Teams" to team,
            "Add standup tomorrow 9am onto teams" to team, "Add standup tomorrow 9am @ Room 4 in Team" to team, "Add standup tomorrow 9am @ Room 4 to Teams" to team, "Add match tomorrow 7pm in Tigers" to tigers,
            "Add match tomorrow 7pm in the Tigers" to tigers, "Add match tomorrow 7pm on The Tigers" to tigers, "Add match tomorrow 7pm to tiger" to tigers, "Add match tomorrow 7pm in the Tiger calendar" to tigers,
            "Trag Standup morgen um 9 Uhr in Team ein" to team, "Standup morgen um 9 Uhr im Teams-Kalender" to team)) {
            val got = r(text, mine)
            assertEquals(text, cal, got.calendar)
            assertNull(text, got.rest)
        }
        // The start of a name is completed after either, as ever, and what is taken reads back.
        val begun = r("Add standup tomorrow 9am in te", mine)
        assertEquals(team, begun.calendar)
        assertEquals("am", begun.rest)
        assertEquals(team, r(Sentence.put("Add standup tomorrow 9am in te", "Team", "in", now, mine), mine).calendar)
        // The model's answer is held to the same: "Teams" after "on" is no calendar of yours, after "to" it is.
        val on = "Add standup with Sam tomorrow 9am on Teams"
        val own = r(on, mine)
        assertSame(own, Splits.merge(own, "{\"title\":\"standup with Sam\",\"when\":\"tomorrow 9am\",\"place\":\"\",\"calendar\":\"Teams\"}", on, now, mine))
        val to = "Add standup with Sam tomorrow 9am at Room 4 to Teams"
        assertEquals(team, Splits.merge(r(to, mine), "{\"title\":\"standup with Sam\",\"when\":\"tomorrow 9am\",\"place\":\"Room 4\",\"calendar\":\"Teams\"}", to, now, mine).calendar)
    }

    @Test fun aNameThatGoesOnIntoTheWordCalendar() {
        // With a calendar "Work" and one "Work calendar": while the second word is typed the row shows the one whose name it
        // begins, a guess until the name is whole. It was "Work", and saved, from "cal" to "calenda".
        for (both in listOf(listOf(sam, workCalendar, work), listOf(sam, work, workCalendar))) {
            val whole = "Add sync tomorrow 9am to Work calendar"
            for (n in "Add sync tomorrow 9am to Work c".length until whole.length) {
                val got = r(whole.take(n), both)
                assertEquals(whole.take(n), workCalendar, got.calendar)
                assertEquals(whole.take(n), "Work calendar".substring(n - "Add sync tomorrow 9am to ".length), got.rest)
                assertFalse(whole.take(n), Cals.saves(got, true, workCalendar))
            }
            assertEquals(workCalendar, r(whole, both).calendar)
            assertNull(r(whole, both).rest)
            assertEquals(work, r("Add sync tomorrow 9am to Work", both).calendar)
            assertNull(r("Add sync tomorrow 9am to Work", both).rest)
        }
        // With no name that goes on, three letters of the word say that the word is on its way, as ever.
        val one = r("Add sync tomorrow 9am to Work cal", listOf(sam, work))
        assertEquals(work, one.calendar)
        assertNull(one.rest)
    }

    @Test fun aNameReadsBackAfterEveryLeadWordItIsWrittenAfter() {
        // The row's list writes "to" in English and „in“ in German, and a name that takes another's place keeps the word that
        // was typed there, which may be "on": a calendar is offered only where its name reads back after all three. "in 2h" is
        // two hours from now, and "at 4 on Mo" a Monday.
        val hours = Cal(40, "2h")
        val mo = Cal(41, "Mo")
        val marked = Cals.marked(listOf(sam, team, hours, mo), now)
        assertEquals(listOf(true, true, false, false), marked.map { it.nameable })
        assertSame(marked[2], Sentence.read("Add sync tomorrow 9am to 2h", now, marked).calendar)
        assertNull(Sentence.read("Add sync in 2h", now, marked).calendar)
        assertSame(marked[3], Sentence.read("Add sync tomorrow 9am to Mo", now, marked).calendar)
        assertNull(Sentence.read("Add sync at 4 on Mo", now, marked).calendar)
        for (c in odd.filter { it.nameable }) for (word in listOf("to", "in", "on")) for (text in listOf("x tomorrow 9am", "x at 4", "x"))
            assertSame("$text $word ${c.name}", c, r(Sentence.put(text, c.name, word, now, odd)).calendar)
    }

    @Test fun aNameThatCannotBeReadBackIsNeverCompleted() {
        // The start of a name completes into a calendar that can be named and takes events, or into none.
        for (c in odd) for (n in 2 until c.name.length) {
            val typed = c.name.take(n)
            Cals.starting(typed, odd)?.let { (cal, rest) ->
                assertTrue("$typed -> ${cal.name}", cal.nameable && cal.writable)
                assertTrue("$typed -> ${cal.name}", cal.name.endsWith(rest))
            }
        }
        assertNull(Cals.starting("Fri", odd))
        assertNull(Cals.starting("Home", odd))
        assertNull(Cals.starting("Hol", odd))
        // And what Right takes reads as the calendar the row showed.
        for (c in odd.filter { it.nameable && it.writable }) for (n in 2 until c.name.length) {
            val text = "Add dinner tomorrow 7pm to " + c.name.take(n)
            if (text.endsWith(" ")) continue
            val begun = r(text)
            if (begun.rest == null) continue
            val taken = Sentence.put(text, begun.calendar!!.name, "to", now, odd)
            assertSame("$text -> $taken", begun.calendar, r(taken).calendar)
            assertNull(taken, r(taken).rest)
        }
    }

    @Test fun aNameIsAsLongAsTheLongestInTheList() {
        // Nine words, with a word in it that leads into a calendar elsewhere: typed whole it is that calendar, with the word "calendar" too.
        assertSame(of(nine), r("Add review tomorrow 9am to Plan of all the works in house and garden").calendar)
        assertSame(of(nine), r("Add review tomorrow 9am to the plan of all the works in house and garden calendar").calendar)
        assertEquals("review", r("Add review tomorrow 9am to Plan of all the works in house and garden").draft.title)
        val begun = r("Add review tomorrow 9am to the plan")
        assertSame(of(nine), begun.calendar)
        assertEquals(" of all the works in house and garden", begun.rest)
        // Without such a calendar the same words are the title's.
        assertNull(r("Add review tomorrow 9am to Plan of all the works in house and garden", listOf(sam, team)).calendar)
    }

    @Test fun aCalendarThatIsNoneOfYoursIsNeverSavedTo() {
        val mine = listOf(sam, team)
        val got = r("Add dinner tomorrow 7pm to the Foo calendar", mine)
        assertTrue(got.named)
        assertNull(got.calendar)
        assertTrue(got.draft.sure)
        assertEquals("dinner to the Foo calendar", got.draft.title)
        // Not into the calendar new events go to: the sentence asked for another.
        assertFalse(Cals.saves(got, true, Cals.target(got.calendar, mine, "")))
        assertFalse(Cals.saves(r("Abendessen morgen um 19 Uhr in den Garten-Kalender", mine), true, sam))
        assertFalse(Cals.saves(r("Add dinner to the Foo calendar tomorrow 7pm", mine), true, sam))
        assertTrue(Cals.saves(r("Add dinner tomorrow 7pm to the Team calendar", mine), true, team))
    }

    @Test fun beforeADayOrATimeACalendarNeedsTheWord() {
        val home = Cal(30, "Home")
        val mine = listOf(sam, team, work, home)
        // "Drive to work" is what the event is called: a name alone is a calendar only as the last thing in the line.
        for ((text, title) in listOf("Add Drive to work tomorrow 8am" to "Drive to work", "Walk to work tomorrow 8am" to "Walk to work", "back to work Monday 9am" to "back to work",
            "Check in Team Friday 3pm" to "Check in Team", "Add handover to team tomorrow 9am" to "handover to team", "dinner to Team tomorrow 7pm" to "dinner to Team",
            "Drive to work tomorrow 8am with Sam" to "Drive to work with Sam")) {
            val got = r(text, mine)
            assertNull(text, got.calendar)
            assertFalse(text, got.named)
            assertEquals(text, title, got.draft.title)
        }
        // With the word after the name it is the calendar wherever it stands; and as the last thing in the line the name alone is.
        for ((text, cal) in listOf("Add standup to the work calendar tomorrow 8am" to work, "Add standup to work calendar tomorrow 8am" to work, "Add standup in den Work-Kalender morgen um 8 Uhr" to work,
            "Add Team bowling night to the Team calendar Saturday 8pm" to team, "Add standup tomorrow 8am to work" to work, "Add standup tomorrow 8am to work @ Room 4" to work,
            "Trag Standup morgen um 8 Uhr in Work ein" to work)) {
            val got = r(text, mine)
            assertEquals(text, cal, got.calendar)
            assertEquals(text, if (cal == team) "Team bowling night" else if (text.startsWith("Trag")) "Standup" else "standup", got.draft.title)
        }
        // The model on the device does not take one out of the title either: before the day and time its answer needs the word too.
        val text = "Drive to work tomorrow 8am with Sam"
        val own = r(text, mine)
        val answer = "{\"title\":\"Drive\",\"when\":\"tomorrow 8am\",\"place\":\"\",\"calendar\":\"work\"}"
        assertSame(own, Splits.merge(own, answer, text, now, mine))
    }

    @Test fun theWordCalendarComingAfterAWholeNameNeedsThreeLetters() {
        val mine = listOf(sam, team)
        // "to Sam K" is a name going on, not the word on its way: one letter says nothing.
        for (typed in listOf("Add tomorrow 9am send book to Sam K", "Add tomorrow 9am send book to Sam Ka", "Add dinner tomorrow 7pm to Team c", "Add dinner tomorrow 7pm to Team ca", "Essen morgen um 19 Uhr im Teamk")) {
            assertNull(typed, r(typed, mine).calendar)
            assertNull(typed, r(typed, mine).rest)
        }
        assertEquals("send book to Sam K", r("Add tomorrow 9am send book to Sam K", mine).draft.title)
        for (typed in listOf("Add tomorrow 9am send book to Sam Kal", "Add dinner tomorrow 7pm to Team cal", "Add dinner tomorrow 7pm to Team calend", "Add dinner tomorrow 7pm to the Team calendar",
            "Essen morgen um 19 Uhr im Teamkal", "Essen morgen um 19 Uhr in den Team-Kalen"))
            assertTrue(typed, r(typed, mine).calendar != null && r(typed, mine).rest == null)
    }

    @Test fun aRepeatWordInsideTheCalendarsOwnNameSaysNoRepeat() {
        for ((text, cal) in listOf("Add sync Friday 9am to Weekly sync" to weekly, "Add standup tomorrow 9am to the Daily calendar" to daily, "Add standup to the Daily calendar tomorrow 9am" to daily,
            "Add sync Friday 9am @ Room 4 to Weekly sync" to weekly)) {
            val got = r(text)
            assertSame(text, of(cal), got.calendar)
            assertFalse(text, got.draft.repeats)
            assertTrue(text, got.draft.sure)
        }
        // A word of the sentence's own still says one, and so does "every Monday".
        for (text in listOf("Add weekly sync Friday 9am to Weekly sync", "Add sync every Friday 9am to Weekly sync", "Add daily standup tomorrow 9am to the Daily calendar", "Add sync Friday 9am weekly to Team"))
            assertTrue(text, r(text).draft.repeats)
        // And the name of a calendar that is none of yours is the sentence's own words.
        assertTrue(r("Add sync Friday 9am to the Monthly calendar").draft.repeats)
    }

    @Test fun aCalendarAfterThePlace() {
        val mine = listOf(sam, team, trips)
        val got = r("Add dinner tomorrow 7pm @ Cafe Luna to Team", mine)
        assertEquals(EventDraft("dinner", at(7, 19), at(7, 20), false, "Cafe Luna", dayGiven = true, timeGiven = true), got.draft)
        assertEquals(team, got.calendar)
        assertNull(got.rest)
        for ((text, cal, place) in listOf(Triple("Add dinner tomorrow 7pm @ Cafe Luna to the Team calendar", team, "Cafe Luna"), Triple("Add dinner tomorrow 7pm @ Cafe Luna, add to Team trips", trips, "Cafe Luna"),
            Triple("Add dinner tomorrow 7pm @ Cafe Luna in Team", team, "Cafe Luna"), Triple("Trag Essen morgen um 19 Uhr @ Café Luna in den Team-Kalender ein", team, "Café Luna"))) {
            assertEquals(text, cal, r(text, mine).calendar)
            assertEquals(text, place, r(text, mine).draft.place)
        }
        // The start of a name there is not completed: the place may go on. Nor is a name after any other word a calendar.
        for (text in listOf("Add dinner tomorrow 7pm @ Cafe Luna to te", "Add dinner tomorrow 7pm @ Cafe Luna to", "Add dinner tomorrow 7pm @ Team", "Add dinner tomorrow 7pm @ Cafe Luna to Team room")) {
            assertNull(text, r(text, mine).calendar)
            assertNull(text, r(text, mine).rest)
            assertEquals(text, text.substringAfter("@ "), r(text, mine).draft.place)
        }
        // A calendar named by the word that is none of yours stays in the place, and is never saved to.
        val unknown = r("Add dinner tomorrow 7pm @ Cafe Luna to the Foo calendar", mine)
        assertTrue(unknown.named)
        assertEquals("Cafe Luna to the Foo calendar", unknown.draft.place)
        assertFalse(Cals.saves(unknown, true, sam))
        // Without the calendars nothing after the place is one.
        assertEquals("Cafe Luna to Team", r("Add dinner tomorrow 7pm @ Cafe Luna to Team", emptyList()).draft.place)
    }

    @Test fun aCalendarCalledLikeADayIsAGuessWhereItIsNamed() {
        // "to Friday" after a day and a time: the calendar of that name, or a second day. The row shows the calendar, and Enter opens the editor.
        val got = r("Add dinner tomorrow 7pm to Friday")
        assertSame(of(friday), got.calendar)
        assertTrue(got.draft.twice)
        assertFalse(Cals.saves(got, true, of(friday)))
        // With no other day in the line the word is the day.
        assertNull(r("Add dinner 7pm to Friday").calendar)
    }
}
