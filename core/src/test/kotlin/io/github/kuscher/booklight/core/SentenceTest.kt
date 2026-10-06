package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An event read from a sentence by rules ([Sentence.read]). The table is the sixteen sentences that were
 * tried on a device (docs/research/event-sentence.md), with the people in them called Sam and the
 * calendars Team.
 */
class SentenceTest {
    /** Monday 5 October 2026, twenty past four: the day those sentences were tried. */
    private val now = LocalDateTime.of(2026, 10, 5, 16, 20)
    private fun at(day: Int, hour: Int = 0, minute: Int = 0, month: Int = 10) = LocalDateTime.of(2026, month, day, hour, minute)

    private val sam = Cal(1, "Sam", 0xFF3F51B5.toInt(), "sam@example.com", primary = true)
    private val team = Cal(2, "Team", 0xFF0B8043.toInt(), "team@group.calendar.google.com")
    private val trips = Cal(3, "Team trips", 0xFFF4511E.toInt(), "trips@group.calendar.google.com")
    private val holidays = Cal(4, "Holidays", 0xFF7986CB.toInt(), "holidays@group.v.calendar.google.com", writable = false)
    private val mine = listOf(sam, team, trips, holidays)

    private fun r(text: String, calendars: List<Cal> = mine) = Sentence.read(text, now, calendars)

    /** One line of the table: the sentence, and what the rules make of it. */
    private class Row(val text: String, val title: String, val start: LocalDateTime, val end: LocalDateTime, val allDay: Boolean = false, val place: String = "",
        val calendar: Cal? = null, val sure: Boolean = true, val loose: Boolean = false, val cue: Boolean = false)

    private val table = listOf(
        Row("Add dinner with Sam tomorrow at 7pm to Team calendar", "dinner with Sam", at(6, 19), at(6, 20), calendar = team, cue = true),
        // A place without an @ stays in the title, and the reading is loose: the model may split it.
        // ("next Tuesday", typed on a Monday: the coming one, and a guess, because people mean two different days by it.)
        Row("Schedule dentist appointment next Tuesday at 3:30pm at Dr. Sam office", "dentist appointment at Dr. Sam office", at(6, 15, 30), at(6, 16, 30), sure = false, loose = true, cue = true),
        Row("Lunch with Sam on Friday at noon at Cafe Luna, add to Team calendar", "Lunch with Sam at Cafe Luna", at(9, 12), at(9, 13), calendar = team, loose = true),
        Row("Team offsite Oct 14 to Oct 16 in Lisbon", "Team offsite in Lisbon", at(14), at(17), allDay = true, loose = true),
        Row("Call mom Sunday 10am", "Call mom", at(11, 10), at(11, 11)),
        // "every Monday": the next one (today is a Monday, and six is still ahead), and a guess.
        Row("Put yoga class every Monday 6pm in my Team calendar", "yoga class", at(5, 18), at(5, 19), calendar = team, sure = false, cue = true),
        Row("Flight to Frankfurt on the 12th at 6:45 in the morning", "Flight to Frankfurt", at(12, 6, 45), at(12, 7, 45)),
        // ("night" is the title's own word here: eight in the evening is in the night it names. Beside a nine in the morning it would be a guess.)
        Row("Add Team bowling night to the Team calendar Saturday 8pm", "Team bowling night", at(10, 20), at(10, 21), calendar = team, cue = true),
        Row("meeting with the team about Q3 plan tmrw 9-10am room 4B", "meeting with the team about Q3 plan room 4B", at(6, 9), at(6, 10), loose = true),
        Row("Birthday party for Sam on November 3rd all day", "Birthday party for Sam", at(3, month = 11), at(4, month = 11), allDay = true),
        Row("Abendessen mit Sam morgen um 19 Uhr in den Team-Kalender eintragen", "Abendessen mit Sam", at(6, 19), at(6, 20), calendar = team),
        Row("Zahnarzt nächsten Dienstag 15:30 Uhr", "Zahnarzt", at(6, 15, 30), at(6, 16, 30), sure = false),
        // („um 12“ does not say its half of the day: read as noon, and a guess. „8 Uhr“ is read as eight in the morning, and is a
        // guess too: an hour up to eight with „Uhr“ is said for the evening as often.)
        Row("Trag Mittagessen mit Sam am Freitag um 12 im Café Luna in den Teamkalender ein", "Mittagessen mit Sam im Café Luna", at(9, 12), at(9, 13), calendar = team, sure = false, loose = true, cue = true),
        Row("Elternabend am 14.10. um 19:30 in der Schule", "Elternabend in der Schule", at(14, 19, 30), at(14, 20, 30), loose = true),
        Row("Übermorgen 8 Uhr Friseur", "Friseur", at(7, 8), at(7, 9), sure = false),
        Row("Geburtstag von Oma am Samstag ganztägig", "Geburtstag von Oma", at(10), at(11), allDay = true),
    )

    @Test fun theSixteenSentences() {
        assertEquals(16, table.size)
        for (row in table) {
            val got = r(row.text)
            assertEquals(row.text, row.title, got.draft.title)
            assertEquals(row.text, row.start, got.draft.start)
            assertEquals(row.text, row.end, got.draft.end)
            assertEquals(row.text, row.allDay, got.draft.allDay)
            assertEquals(row.text, row.place, got.draft.place)
            assertEquals(row.text, row.calendar, got.calendar)
            assertEquals(row.text, row.sure, got.draft.sure)
            assertEquals(row.text, row.loose, got.loose)
            assertEquals(row.text, row.cue, got.cue)
            assertNull(row.text, got.rest)
            assertFalse(row.text, got.named)
        }
        // Ten of the sixteen are right from end to end by the rules alone; the other six have their place in the title.
        assertEquals(10, table.count { !it.loose })
    }

    @Test fun theCueIsDroppedOrKept() {
        assertEquals("dinner", r("Add dinner tomorrow 7pm").draft.title)
        assertEquals("dinner", r("add  dinner tomorrow 7pm").draft.title)
        assertEquals("dentist", r("Schedule dentist Friday 3pm").draft.title)
        assertEquals("yoga", r("Put yoga Friday 6pm").draft.title)
        assertEquals("dinner", r("New event dinner tomorrow 7pm").draft.title)
        assertEquals("Zahnarzt", r("Neuer Termin Zahnarzt morgen 15 Uhr").draft.title)
        assertEquals("Zahnarzt", r("Trage Zahnarzt morgen 15 Uhr ein").draft.title)
        assertEquals("Zahnarzt", r("Zahnarzt morgen 15 Uhr eintragen").draft.title)
        // "Book club" and "Plan review" are what such events are called: these two say it is an event and stay.
        for (t in listOf("Book club Friday 7pm" to "Book club", "Plan review tomorrow 10am" to "Plan review", "book dentist tomorrow 3pm" to "book dentist", "Plane Offsite am 14.10." to "Plane Offsite")) {
            assertEquals(t.first, t.second, r(t.first).draft.title)
            assertTrue(t.first, r(t.first).cue)
        }
        // Only at the start, only as a whole word, and „ein“ only where „trag“ began.
        assertEquals("Add-ons review", r("Add-ons review Friday 3pm").draft.title)
        assertFalse(r("Add-ons review Friday 3pm").cue)
        assertEquals("Addition lesson", r("Addition lesson tomorrow").draft.title)
        assertFalse(r("Addition lesson tomorrow").cue)
        assertEquals("Kaufe ein", r("Kaufe ein morgen 15 Uhr").draft.title)
        // The cue alone, and nothing at all.
        assertEquals("", r("Add").draft.title)
        assertTrue(r("Add").cue)
        assertEquals(EventDraft("", at(5), at(6), true, "", dayGiven = false, timeGiven = false), r("").draft)
    }

    @Test fun thePlaceFollowsAnAt() {
        val got = r("Add dinner with Sam tomorrow at 7pm to Team @ Cafe Luna")
        assertEquals("dinner with Sam", got.draft.title)
        assertEquals("Cafe Luna", got.draft.place)
        assertEquals(team, got.calendar)
        assertFalse(got.loose)
        assertEquals("Cafe Luna", r("Add dinner @ Cafe Luna").draft.place)
    }

    @Test fun aLineTypedWithoutTheKeywordIsOfferedAsAnEvent() {
        for (t in listOf("Add dinner with Sam tomorrow at 7pm", "add dinner 7pm", "Schedule dentist Friday 3pm", "Put yoga class every Monday 6pm in my Team calendar",
            "Book dentist tomorrow 3pm", "Plan offsite Oct 14 9am", "Trag Mittagessen am Freitag um 12 ein", "Neuer Termin Zahnarzt morgen", "Plane Offsite am 14.10. um 9 Uhr",
            "Add lunch on sat", "add call 2-3pm", "Plan review tomorrow 10am", "book club Friday 9am", "Plane Offsite morgen um 9 Uhr", "add milk tomorrow", "add dinner at 7", "add 1-2 eggs tomorrow"))
            assertTrue(t, Sentence.reads(r(t)))
        for (t in listOf(
            // No cue: under the keyword it is an event, without it an ordinary line.
            "Lunch with Sam on Friday at noon", "Call mom Sunday 10am", "dinner tomorrow", "Zahnarzt morgen 15 Uhr",
            // No day and no time.
            "Add dinner with Sam", "add", "book a table", "schedule", "plan b",
            // Nothing left to call it.
            "Add tomorrow", "add 7pm", "schedule friday 3pm",
            // What is far more often something else: a short weekday alone, two bare numbers.
            "add pictures of the sun", "add 2-3 eggs", "book 2-3 nights", "put the cat out, sat",
            // A longer word that only begins like a cue.
            "Additional notes tomorrow", "Booking tomorrow 3pm", "planet tomorrow",
            // A cue that stays in the title is no title by itself.
            "plan tomorrow", "plan friday", "Plan 5pm", "book tomorrow", "book 9am", "plane morgen", "Plane am 14.10. um 19 Uhr",
            // A cue that as often begins an everyday search needs a day and a time, both (KeywordTest has the searches).
            "Schedule dentist Friday", "Plan offsite Oct 14 to Oct 16", "Plane Offsite am 14.10.", "Plan review tomorrow", "book club 9am", "Plane Offsite morgen",
            // After the cue is the middle of the line: a fraction, two bare numbers, a short weekday and a number after "at" are not read there.
            "add 1/2 cup sugar", "add 1-2 eggs", "add sun cream", "add at 5 Main Street dinner", "add sat nav update",
            // "All day" with no day is neither a day nor a time.
            "add inventory all day", "put it off all day", "Trag Inventur ganztägig ein",
        )) assertFalse(t, Sentence.reads(r(t)))
        // What stood after the cue and was not read stays in the title; the day that was read is the day.
        val eggs = r("add 1-2 eggs tomorrow")
        assertEquals("1-2 eggs", eggs.draft.title)
        assertEquals(EventDraft("1-2 eggs", at(6), at(7), true, "", dayGiven = true, timeGiven = false, doubts = setOf(Doubt.LEFT)), eggs.draft)
        assertEquals("sun cream order", r("add sun cream order 5pm").draft.title)
        assertFalse(r("add sun cream order 5pm").draft.dayGiven)
    }

    @Test fun aCalendarIsFoundByItsName() {
        for (t in listOf("dinner tomorrow 7pm to Team", "dinner tomorrow 7pm to the Team calendar", "dinner tomorrow 7pm in team", "dinner tomorrow 7pm on my TEAM calendar",
            "dinner tomorrow 7pm to Teams", "dinner to the Team calendar tomorrow 7pm", "Abendessen morgen 19 Uhr in den Team-Kalender", "Abendessen morgen 19 Uhr im Teamkalender",
            "Abendessen morgen 19 Uhr in meinen Team Kalender", "dinner tomorrow 7pm, add to Team", "dinner tomorrow 7pm, put it in the Team calendar")) {
            assertEquals(t, team, r(t).calendar)
            assertEquals(t, if (t.startsWith("A")) "Abendessen" else "dinner", r(t).draft.title)
            assertFalse(t, r(t).loose)
        }
        // A name of several words; the whole name wins over a name that begins the same.
        assertEquals(trips, r("flight Friday 9am to Team trips").calendar)
        assertEquals("flight", r("flight Friday 9am to Team trips").draft.title)
        assertEquals(trips, r("flight Friday 9am to the team trips calendar").calendar)
        // The account's own calendar, by its name.
        assertEquals(sam, r("dentist Friday 9am in Sam").calendar)
        // Before the day and time a name alone is the title's: there a calendar needs the word after it.
        assertNull(r("dinner to Team tomorrow 7pm").calendar)
        assertEquals("dinner to Team", r("dinner to Team tomorrow 7pm").draft.title)
    }

    @Test fun aCalendarCalledLikeACommonWord() {
        // After "to", "in" or "on" at the end of a piece it is the calendar (the row says so); after any other word it is the title's.
        val home = Cal(20, "Home")
        val work = Cal(21, "Work")
        val friday = Cal(22, "Friday")
        val all = listOf(sam, team, home, work, friday)
        for (t in listOf("dinner at home tomorrow 7pm" to "dinner at home", "Call mom at home tomorrow 5pm" to "Call mom at home", "drive home on Friday 5pm" to "drive home",
            "meeting in Team room on Friday 5pm" to "meeting in Team room", "work lunch tomorrow 1pm" to "work lunch", "home office day tomorrow all day" to "home office day")) {
            assertNull(t.first, r(t.first, all).calendar)
            assertEquals(t.first, t.second, r(t.first, all).draft.title)
        }
        assertEquals(home, r("dinner tomorrow 7pm to Home", all).calendar)
        // "Drive to work" is what the event is called: before the day a name alone is no calendar. As the line's last words it is.
        assertNull(r("Drive to work tomorrow 8am", all).calendar)
        assertEquals("Drive to work", r("Drive to work tomorrow 8am", all).draft.title)
        assertEquals(work, r("Drive tomorrow 8am to work", all).calendar)
        assertEquals("Drive", r("Drive tomorrow 8am to work", all).draft.title)
        // An emoji in a name, and a name of two words that another name begins.
        val party = Cal(23, "Fam 🎉")
        assertEquals(party, r("dinner tomorrow 7pm to Fam 🎉", all + party).calendar)
        assertEquals(trips, r("flight Friday 9am to Team trips", mine).calendar)
    }

    @Test fun wordsThatAreNoCalendarStayInTheTitle() {
        // Not after "to", "in" or "on"; not in the middle of a piece; a word that only begins like the name, anywhere but at the very end.
        assertNull(r("dinner with the Team tomorrow 7pm").calendar)
        assertEquals("dinner with the Team", r("dinner with the Team tomorrow 7pm").draft.title)
        assertNull(r("Team dinner tomorrow 7pm").calendar)
        assertNull(r("send invites to Team members tomorrow 9am").calendar)
        assertEquals("send invites to Team members", r("send invites to Team members tomorrow 9am").draft.title)
        assertNull(r("move to Te tomorrow 9am").calendar)
        assertEquals("Flight to Frankfurt", r("Flight to Frankfurt on the 12th at 6:45 in the morning").draft.title)
    }

    /**
     * A calendar's name is an ordinary word too. After "to", "in" or "on" as the last words of the line it is the calendar, as
     * the design has it: the row says so (its Calendar slot, and the title without the word) before anything is opened or
     * saved. Before a day or a time it is the title's, unless the word "calendar" follows it.
     */
    @Test fun aCalendarsNameThatIsAlsoAnOrdinaryWordIsTheCalendar() {
        val got = r("Add handover tomorrow 9am to team")
        assertEquals(team, got.calendar)
        assertEquals("handover", got.draft.title)
        val before = r("Add handover to team tomorrow 9am")
        assertNull(before.calendar)
        assertEquals("handover to team", before.draft.title)
        assertEquals(team, r("Add handover to the team calendar tomorrow 9am").calendar)
        // Without such a calendar the words stay where they are.
        val none = r("Add handover tomorrow 9am to team", listOf(sam))
        assertNull(none.calendar)
        assertEquals("handover to team", none.draft.title)
    }

    @Test fun aCalendarThatIsNoneOfYoursStaysInTheTitle() {
        // Named by the word: the reading says so, the name stays, and the model is not asked about a piece that is known.
        val unknown = r("Add dinner tomorrow at 7pm to the Garden calendar")
        assertNull(unknown.calendar)
        assertTrue(unknown.named)
        assertEquals("dinner to the Garden calendar", unknown.draft.title)
        assertFalse(unknown.loose)
        // The calendars are not known at all (not allowed): the same, for every calendar.
        val blind = r("Add dinner with Sam tomorrow at 7pm to Team calendar", emptyList())
        assertNull(blind.calendar)
        assertTrue(blind.named)
        assertEquals("dinner with Sam to Team calendar", blind.draft.title)
        assertFalse(blind.loose)
        val german = r("Abendessen mit Sam morgen um 19 Uhr in den Team-Kalender eintragen", emptyList())
        assertTrue(german.named)
        assertEquals("Abendessen mit Sam in den Team-Kalender", german.draft.title)
        // Without the word nothing says it is a calendar: an ordinary piece of the title.
        val plain = r("Add dinner tomorrow at 7pm to Garden")
        assertFalse(plain.named)
        assertTrue(plain.loose)
        // "the calendar" names none.
        assertFalse(r("Add dinner tomorrow at 7pm to the calendar").named)
    }

    @Test fun theStartOfACalendarsNameAtTheVeryEnd() {
        val got = r("Add dinner with Sam tomorrow at 7pm to tea")
        assertEquals(team, got.calendar)
        assertEquals("m", got.rest)
        assertEquals("dinner with Sam", got.draft.title)
        assertEquals("am", r("Add dinner tomorrow 7pm to the Te").rest)
        assertEquals("rips", r("flight Friday 9am to team t").rest)
        assertEquals(trips, r("flight Friday 9am to team t").calendar)
        // Never into a calendar that takes no events: its whole name is read (and shown, and never saved to), its start is not.
        assertNull(r("Add trip tomorrow in ho", listOf(holidays)).rest)
        assertNull(r("Add trip tomorrow in ho", listOf(holidays)).calendar)
        assertEquals(holidays, r("Add trip tomorrow in holidays", listOf(holidays)).calendar)
        assertEquals("me", r("Add trip tomorrow in ho", listOf(holidays, Cal(5, "Home"))).rest)
        // Two letters at least, and the whole name has nothing left to complete.
        assertNull(r("Add dinner tomorrow 7pm to t").rest)
        assertNull(r("Add dinner tomorrow 7pm to t").calendar)
        assertNull(r("Add dinner tomorrow 7pm to Team").rest)
        // Only at the very end of the sentence: not before the day, not before a place, not before „ein“.
        assertNull(r("Add dinner to tea tomorrow 7pm").calendar)
        assertEquals("dinner to tea", r("Add dinner to tea tomorrow 7pm").draft.title)
        assertNull(r("Add dinner tomorrow 7pm to tea @ Cafe Luna").calendar)
        assertNull(r("Trag Essen morgen um 12 in tea ein").calendar)
        // And not with the word after it: "tea calendar" is a calendar that is none of yours.
        assertTrue(r("Add dinner tomorrow 7pm to tea calendar").named)
    }

    /** A calendar whose own name begins with "the", "my" or "our" is named with that word and without it, in any case, with a plural s and without. */
    @Test fun aCalendarWhoseOwnNameBeginsWithAnArticle() {
        val tigers = Cal(7, "The Tigers", 0, "tigers@group.calendar.google.com")
        val family = Cal(8, "Our Family", 0, "family@group.calendar.google.com")
        val stuff = Cal(9, "My Stuff", 0, "stuff@group.calendar.google.com")
        val tiger = Cal(10, "Die Tiger", 0, "tiger@group.calendar.google.com")
        val all = listOf(sam, team, tigers, family, stuff)
        for (t in listOf("to tiger", "to tigers", "to the tigers", "to The Tigers", "to THE TIGERS", "to the Tigers calendar", "to The Tigers calendar", "to my tigers calendar", "in den Tigers-Kalender",
            "im Tigers-Kalender", "on the tigers calendar", ", add to the Tigers calendar")) {
            val got = r("Add match tomorrow 7pm $t", all)
            assertEquals(t, tigers, got.calendar)
            assertEquals(t, "match", got.draft.title)
            assertNull(t, got.rest)
            assertFalse(t, got.named)
        }
        assertEquals(tigers, r("Add match to the Tigers calendar tomorrow 7pm", all).calendar)
        assertEquals("match", r("Add match to the Tigers calendar tomorrow 7pm", all).draft.title)
        assertNull(r("Add match to the Tigers tomorrow 7pm", all).calendar)
        assertEquals(family, r("Add lunch Sunday 1pm to our family", all).calendar)
        assertEquals(family, r("Add lunch Sunday 1pm to family", all).calendar)
        assertEquals(family, r("Add lunch Sunday 1pm to the Family calendar", all).calendar)
        assertEquals(stuff, r("Add laundry Sunday 1pm in my stuff", all).calendar)
        assertEquals(stuff, r("Add laundry Sunday 1pm in stuff", all).calendar)
        assertEquals(tiger, r("Trag Spiel morgen um 19 Uhr in den Tiger-Kalender ein", listOf(sam, tiger)).calendar)
        assertEquals(tiger, r("Trag Spiel morgen um 19 Uhr in die Tiger ein", listOf(sam, tiger)).calendar)
        // At every letter: the article alone names nothing and completes nothing, the name's start completes from two letters of its own, and the whole name stays.
        fun end(typed: String) = r("Add match tomorrow 7pm $typed", all)
        assertNull(end("to the").calendar)
        assertNull(end("to the").rest)
        assertNull(end("to The").calendar)
        assertNull(end("to the t").calendar)
        assertEquals(tigers, end("to the ti").calendar)
        assertEquals("gers", end("to the ti").rest)
        assertEquals("rs", end("to the tige").rest)
        assertEquals(tigers, end("to the Tiger").calendar)
        assertNull(end("to the Tiger").rest)
        assertEquals(tigers, end("to ti").calendar)
        assertEquals("gers", end("to ti").rest)
        assertEquals("mily", end("to our fa").rest)
        // (Nor is the name begun by the first letters of its article: "to th" is on its way to "to the Team".)
        assertNull(end("to th").calendar)
        assertNull(Cals.starting("th", all))
        assertEquals(tigers to "gers", Cals.starting("the ti", all))
        assertNull(end("to our").calendar)
        assertNull(end("to my").calendar)
        for (typed in listOf("to the Tigers", "to the Tigers cal", "to the Tigers calendar", "to The Tigers", "to The Tigers calen")) {
            assertEquals(typed, tigers, end(typed).calendar)
            assertNull(typed, end(typed).rest)
        }
        // A calendar called like a word that begins with an article's letters is not begun by the article: "to the" is not the start of "Theatre".
        val theatre = Cal(11, "Theatre")
        assertNull(r("Add play tomorrow 7pm to the", listOf(team, theatre)).calendar)
        assertEquals("tre", r("Add play tomorrow 7pm to thea", listOf(team, theatre)).rest)
        assertEquals(team, r("Add play tomorrow 7pm to the te", listOf(team, theatre)).calendar)
        // A calendar "Team" and one "The Team": each by its own name.
        val theTeam = Cal(12, "The Team")
        assertEquals(theTeam, r("Add sync tomorrow 9am to the team", listOf(team, theTeam)).calendar)
        assertEquals(team, r("Add sync tomorrow 9am to team", listOf(team, theTeam)).calendar)
        assertEquals(team, r("Add sync tomorrow 9am to my team", listOf(team, theTeam)).calendar)
    }

    @Test fun aNameIsCompletedWhateverItsAccentsAndCase() {
        val muller = Cal(13, "Müller")
        val all = listOf(team, muller)
        assertEquals(muller, r("Add call tomorrow 9am to muller", all).calendar)
        assertEquals("ler", r("Add call tomorrow 9am to mül", all).rest)
        assertEquals("ler", r("Add call tomorrow 9am to mul", all).rest)
        assertEquals("ler", r("Add call tomorrow 9am to MUL", all).rest)
        assertEquals(muller, r("Add call tomorrow 9am to mul", all).calendar)
        assertEquals("am", r("Add call tomorrow 9am to TE", all).rest)
        // Typed whole, a name has nothing left to complete, also where another name goes on from it.
        val travel = Cal(14, "Work Travel")
        val work = Cal(15, "Work")
        assertNull(Cals.starting("Work", listOf(travel, work)))
        assertEquals(travel to "ravel", Cals.starting("Work T", listOf(travel, work)))
        assertNull(Cals.starting("T", listOf(team)))
        assertNull(Cals.starting("", listOf(team)))
        assertNull(Cals.starting("ho", listOf(holidays)))
    }

    /**
     * A calendar's whole name is that calendar; and with three letters or more of the word "calendar" after it the word is
     * on its way, and the row holds the calendar at every letter that follows. One letter or two after the name say nothing
     * yet ("to Sam K" is a name going on): for those two letters the words are the title's.
     */
    @Test fun theCalendarStaysWhileTheWordCalendarIsTyped() {
        class Typed(val whole: String, val name: String, val calendar: Cal, val title: String)
        for (t in listOf(
            Typed("Add dinner with Sam tomorrow at 7pm to the Team calendar", "Team", team, "dinner with Sam"),
            Typed("Abendessen morgen um 19 Uhr in den Team-Kalender", "Team", team, "Abendessen"),
            Typed("Abendessen morgen um 19 Uhr im Teamkalender", "Team", team, "Abendessen"),
            Typed("flight Friday 9am to Team trips calendar", "Team trips", trips, "flight"),
        )) {
            // Every beginning from the name's last letter on. (A space at the end is no letter: a sentence is read trimmed.)
            val whole = t.whole.indexOf(t.name) + t.name.length
            for (n in whole..t.whole.length) {
                val typed = t.whole.take(n)
                // (How many letters of the word stand after the name: a hyphen or a space between them is none.)
                val letters = typed.substring(whole).count { it.isLetter() }
                if (letters in 1..2) { assertNull(typed, r(typed).calendar); continue }
                assertEquals(typed, t.calendar, r(typed).calendar)
                assertEquals(typed, t.title, r(typed).draft.title)
            }
        }
        // Only at the very end, and only the word: "Team c" before the day is no calendar, and "Team x" never.
        assertNull(r("Add dinner to Team c tomorrow 7pm").calendar)
        assertNull(r("Add dinner tomorrow 7pm to Team x").calendar)
    }

    @Test fun everyLetterOfEverySentenceIsRead() {
        // The row is made at every key: no beginning of any of the sixteen sentences throws, and none shows a word that was not typed.
        for (row in table) for (n in 0..row.text.length) {
            val typed = row.text.take(n)
            val got = r(typed)
            Sentence.reads(got)
            // (A whole word of what was typed, or the word that was typed with a comma after it: never a piece of a longer one.)
            val words = typed.split(' ').flatMap { listOf(it, it.trimEnd(',', ';')) }.toSet()
            for (w in (got.draft.title + " " + got.draft.place).split(' ').filter { it.isNotEmpty() }) assertTrue("$typed: $w", w in words)
            // And the row never offers to save what is still being typed as a calendar's name, or what is a guess.
            if (Cals.saves(got, true, got.calendar ?: sam)) { assertTrue(typed, got.draft.sure); assertNull(typed, got.rest) }
        }
    }

    @Test fun aCalendarChosenFromTheListIsWrittenIntoTheSentence() {
        fun put(text: String, name: String = "Team", word: String = "to") = Sentence.put(text, name, word, now, mine)
        // None named: after the sentence's own words.
        assertEquals("Add dinner tomorrow 7pm to Team", put("Add dinner tomorrow 7pm"))
        assertEquals("dinner tomorrow 7pm to Team trips", put("dinner tomorrow 7pm ", "Team trips"))
        // Before a place, and before „ein“.
        assertEquals("Add dinner tomorrow 7pm to Team @ Cafe Luna", put("Add dinner tomorrow 7pm @ Cafe Luna"))
        assertEquals("Trag Essen morgen um 12 in Team ein", put("Trag Essen morgen um 12 ein", word = "in"))
        // One named, or begun: in its place.
        assertEquals("Add dinner tomorrow 7pm to Sam", put("Add dinner tomorrow 7pm to Team", "Sam"))
        assertEquals("Add dinner tomorrow 7pm to the Sam", put("Add dinner tomorrow 7pm to the Team calendar", "Sam"))
        assertEquals("Add dinner tomorrow 7pm to Team", put("Add dinner tomorrow 7pm to tea"))
        // Named before the day and time, by the word: that phrase goes, and the name stands at the sentence's end, where a name alone is a calendar.
        assertEquals("Add dinner tomorrow 7pm to Sam", put("Add dinner to the Team calendar tomorrow 7pm", "Sam"))
        assertEquals("Add dinner tomorrow 7pm to Sam @ Cafe Luna", put("Add dinner, add to the Team calendar tomorrow 7pm @ Cafe Luna", "Sam"))
        assertEquals("Add dinner to Team tomorrow 7pm to Sam", put("Add dinner to Team tomorrow 7pm", "Sam"))
        assertEquals("Add dinner tomorrow 7pm to the Team", put("Add dinner tomorrow 7pm to the Garden calendar"))
        // A name that begins with an article of its own takes the place of the article that was typed too; and the start of a name becomes the name as the calendar has it.
        val tigers = Cal(7, "The Tigers")
        fun tiger(text: String, name: String = "The Tigers") = Sentence.put(text, name, "to", now, mine + tigers)
        assertEquals("Add match tomorrow 7pm to The Tigers", tiger("Add match tomorrow 7pm to the Team calendar"))
        assertEquals("Add match tomorrow 7pm to The Tigers", tiger("Add match tomorrow 7pm to the ti"))
        assertEquals("Add match tomorrow 7pm to The Tigers", tiger("Add match tomorrow 7pm to TI"))
        assertEquals("Add match tomorrow 7pm to The Tigers", tiger("Add match tomorrow 7pm"))
        assertEquals("Add match tomorrow 7pm to Sam", tiger("Add match tomorrow 7pm to The Tigers", "Sam"))
        assertEquals("Add match tomorrow 7pm to Team", tiger("Add match tomorrow 7pm to TE", "Team"))
        assertEquals("Add match tomorrow 7pm to the Team", tiger("Add match tomorrow 7pm to the te", "Team"))
        assertEquals("Add match tomorrow 7pm to The Tigers", tiger("Add match tomorrow 7pm to my ti"))
        assertEquals(tigers, Sentence.read(tiger("Add match tomorrow 7pm to the Team calendar"), now, mine + tigers).calendar)
        // One reading of the sentence writes every name as each reading of it would.
        for (t in listOf("Add dinner tomorrow 7pm", "Add dinner tomorrow 7pm to the Team calendar @ Cafe Luna", "Trag Essen morgen um 12 ein", "Add match tomorrow 7pm to the ti", "")) {
            val all = mine + tigers
            val write = Sentence.putter(t, "to", now, all)
            for (c in all) assertEquals("$t + ${c.name}", Sentence.put(t, c.name, "to", now, all), write(c.name))
        }
        // What it makes is read as that calendar.
        for (t in listOf("Add dinner tomorrow 7pm", "Add dinner tomorrow 7pm @ Cafe Luna", "Add dinner tomorrow 7pm to tea", "dinner", ""))
            for (c in listOf(sam, team, trips)) assertEquals("$t + ${c.name}", c, r(put(t, c.name)).calendar)
    }

    @Test fun nothingThrowsAndNothingIsLost() {
        val bits = listOf("add", "Add", "trag", "ein", "eintragen", "new", "event", "book", "to", "in", "on", "the", "my", "den", "Team", "team", "tea", "te", "trips", "calendar", "Kalender",
            "Team-Kalender", "tomorrow", "7pm", "at", "@", "Cafe", "Luna", ",", "dinner", "Sam", "fri", "9-10", "🎉", "it", "put", "every", "Monday", "all", "day", "x")
        val random = java.util.Random(5)
        repeat(20_000) {
            val text = List(random.nextInt(10)) { bits[random.nextInt(bits.size)] }.joinToString(if (random.nextBoolean()) " " else "  ")
            val got = Sentence.read(text, now, mine)
            Sentence.reads(got)
            // Every word of the title and of the place was typed, as a whole word.
            val words = text.split(' ').flatMap { listOf(it, it.trimEnd(',', ';')) }.toSet()
            for (w in (got.draft.title + " " + got.draft.place).split(' ').filter { it.isNotEmpty() }) assertTrue("$text: $w", w in words)
            // What is left of a calendar's name is the end of that calendar's name.
            got.rest?.let { assertTrue(text, got.calendar!!.name.endsWith(it)) }
            Sentence.put(text, "Team", "to", now, mine)
        }
    }
}
