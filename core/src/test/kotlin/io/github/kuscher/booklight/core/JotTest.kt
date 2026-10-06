package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JotTest {
    /** Thursday 1 October 2026, ten in the morning. */
    private val now = LocalDateTime.of(2026, 10, 1, 10, 0)
    private fun at(day: Int, hour: Int = 0, minute: Int = 0) = LocalDateTime.of(2026, 10, day, hour, minute)

    // Mail

    @Test fun mailWithSubjectAndMessage() {
        assertEquals(MailDraft(listOf("anna@x.com"), "Lunch?", "See you at 1"), Jot.mail("anna@x.com Lunch? / See you at 1"))
        assertEquals(MailDraft(listOf("anna@x.com"), "Lunch?", ""), Jot.mail("anna@x.com Lunch?"))
        assertEquals(MailDraft(listOf("anna@x.com"), "Hi", "line one\nline two"), Jot.mail("anna@x.com Hi / line one / line two"))
    }

    @Test fun mailToSeveral() {
        val both = listOf("anna@x.com", "ben@x.com")
        assertEquals(MailDraft(both, "Q3 numbers", ""), Jot.mail("anna@x.com, ben@x.com Q3 numbers"))
        assertEquals(MailDraft(both, "Q3", ""), Jot.mail("anna@x.com,ben@x.com Q3"))
        assertEquals(MailDraft(both, "Q3", ""), Jot.mail("anna@x.com , ben@x.com; anna@x.com Q3"))
    }

    @Test fun onlyLeadingWordsAreRecipients() {
        assertEquals(MailDraft(emptyList(), "Ask anna@x.com about lunch", ""), Jot.mail("Ask anna@x.com about lunch"))
        assertEquals(MailDraft(emptyList(), "Lunch tomorrow", "hi"), Jot.mail("Lunch tomorrow / hi"))
    }

    @Test fun mailWhileItIsBeingTyped() {
        assertEquals(MailDraft(emptyList(), "", ""), Jot.mail(""))
        assertEquals(MailDraft(emptyList(), "", ""), Jot.mail("   "))
        assertEquals(MailDraft(listOf("anna@"), "", ""), Jot.mail("anna@"))
        assertEquals(MailDraft(listOf("anna@x.com"), "Hi", ""), Jot.mail("anna@x.com Hi /"))
        assertEquals(MailDraft(emptyList(), "", ""), Jot.mail("@"))
    }

    // Note

    @Test fun aSlashBetweenSpacesBreaksTheLine() {
        assertEquals("buy milk\nand coffee", Jot.note("buy milk / and coffee"))
        assertEquals("Idea: quieter footer\ntry 0.6 ink", Jot.note("  Idea: quieter footer  /  try 0.6 ink "))
        assertEquals("a", Jot.note("a /"))
    }

    @Test fun otherSlashesStay() {
        assertEquals("a/b", Jot.note("a/b"))
        assertEquals("see https://x.com/a", Jot.note("see https://x.com/a"))
        assertEquals("", Jot.note(""))
        assertEquals("", Jot.note("  "))
    }

    // Event

    @Test fun eventWithWhenTitleAndPlace() {
        assertEquals(EventDraft("Dentist", at(2, 15), at(2, 16), false, "Main St", dayGiven = true, timeGiven = true),
            Jot.event("Fri 3pm Dentist @ Main St", now))
        assertEquals(EventDraft("Standup", at(2, 9), at(2, 9, 30), false, "Room 4", dayGiven = true, timeGiven = true, vague = true),
            Jot.event("Standup tomorrow 9-9:30 @ Room 4", now))
    }

    @Test fun eventWithoutADayOrATimeIsTodayAllDay() {
        assertEquals(EventDraft("Dentist", at(1), at(2), true, "", dayGiven = false, timeGiven = false), Jot.event("Dentist", now))
        assertEquals(EventDraft("3 Dentists", at(1), at(2), true, "", dayGiven = false, timeGiven = false), Jot.event("3 Dentists", now))
        assertEquals(EventDraft("", at(1), at(2), true, "", dayGiven = false, timeGiven = false), Jot.event("", now))
    }

    @Test fun eventGuessesWhatIsMissing() {
        assertEquals(EventDraft("Offsite", at(2), at(3), true, "", dayGiven = true, timeGiven = false), Jot.event("tomorrow Offsite", now))
        assertEquals(EventDraft("Call", at(1, 17), at(1, 18), false, "", dayGiven = false, timeGiven = true), Jot.event("5pm Call", now))
        assertEquals(EventDraft("", at(2, 15), at(2, 16), false, "", dayGiven = true, timeGiven = true), Jot.event("Fri 3pm", now))
    }

    @Test fun thePlaceFollowsTheLastAt() {
        assertEquals("c", Jot.event("a @ b @ c", now).place)
        assertEquals("a @ b", Jot.event("a @ b @ c", now).title)
        assertEquals(EventDraft("", at(1), at(2), true, "Home", dayGiven = false, timeGiven = false), Jot.event("@ Home", now))
        assertEquals("Dentist", Jot.event("Dentist Fri 3pm @", now).title)
        assertEquals(at(2, 15), Jot.event("Dentist Fri 3pm @", now).start)
        assertEquals("", Jot.event("mail anna@x.com", now).place)
        assertEquals("mail anna@x.com", Jot.event("mail anna@x.com", now).title)
    }

    @Test fun anEventsDayAndTimeMayStandInTheMiddle() {
        assertEquals(EventDraft("call about it", at(2), at(3), true, "", dayGiven = true, timeGiven = false), Jot.event("call tomorrow about it", now))
        assertEquals(EventDraft("Dinner with Sam at Luigi’s", at(2, 19), at(2, 20), false, "", dayGiven = true, timeGiven = true),
            Jot.event("Dinner with Sam tomorrow at 7pm at Luigi’s", now))
        assertEquals(EventDraft("Dinner with Sam", at(2, 19), at(2, 20), false, "Luigi’s", dayGiven = true, timeGiven = true),
            Jot.event("Dinner with Sam tomorrow at 7pm @ Luigi’s", now))
    }

    @Test fun anEventOverSeveralDaysAndOneThatIsAllDay() {
        // A range of days ends at the midnight after its last day: that is how calendars keep one.
        assertEquals(EventDraft("Team offsite", at(14), at(17), true, "Lisbon", dayGiven = true, timeGiven = true), Jot.event("Team offsite Oct 14 to Oct 16 @ Lisbon", now))
        assertEquals(EventDraft("Birthday party for Sam", LocalDateTime.of(2026, 11, 3, 0, 0), LocalDateTime.of(2026, 11, 4, 0, 0), true, "", dayGiven = true, timeGiven = true),
            Jot.event("Birthday party for Sam on November 3rd all day", now))
    }

    @Test fun whenAnEventIsSure() {
        // Sure: a day and a time that were both typed, the time with its half of the day; a day with "all day"; a range of days.
        for (t in listOf("Call mom Sunday 10am", "Geburtstag von Oma am Samstag ganztägig", "Team offsite Oct 14 to Oct 16", "Standup tomorrow from 9am to 10am", "Standup tomorrow 9-10am",
            "Abendessen morgen um 19 Uhr", "Essen morgen 7 Uhr abends", "Dinner tomorrow at 7 in the evening", "Lunch Friday at noon", "Flight on the 12th at 6:45 in the morning",
            "Review Friday 15:30", "Workshop morgen 9-17 Uhr", "dinner at Main Street tomorrow 7pm", "sun deck party tomorrow 3pm"))
            assertEquals(t, true, Jot.event(t, now).sure)
        // A guess: no day, no time, neither, or a repeat (the day that was read is only the next one).
        for (t in listOf("5pm Call", "Offsite tomorrow", "Dentist", "", "yoga class every Monday 6pm", "Inventory all day"))
            assertEquals(t, false, Jot.event(t, now).sure)
        assertEquals(true, Jot.event("yoga class every Monday 6pm", now).repeats)
    }

    @Test fun aTimeWithoutItsHalfOfTheDayIsAGuess() {
        // "at 7" is read as seven in the evening, and "7:30" as half past seven in the morning: which half was not typed.
        for (t in listOf("Dinner tomorrow at 7", "Dinner tomorrow 7:30", "Essen morgen um 7", "Friseur übermorgen um 8", "Mittagessen am Freitag um 12", "Standup tomorrow from 9 to 10",
            "Standup tomorrow 9-10", "dinner 7 people tomorrow at 8", "Call Friday at 3")) {
            val e = Jot.event(t, now)
            assertTrue(t, e.dayGiven && e.timeGiven && e.vague)
            assertFalse(t, e.sure)
        }
        assertFalse(Jot.event("Dinner tomorrow at 7pm", now).vague)
        // „9 Uhr“ is the 24-hour clock's nine in the morning, and says so. „8 Uhr“ is said for the evening as well: read as eight
        // in the morning, and a guess.
        assertTrue(Jot.event("Friseur übermorgen 9 Uhr", now).sure)
        assertEquals(8, Jot.event("Friseur übermorgen 8 Uhr", now).start.hour)
        assertFalse(Jot.event("Friseur übermorgen 8 Uhr", now).sure)
    }

    @Test fun aWordThatSaysARepeatIsAGuess() {
        for (t in listOf("weekly sync Friday 9am", "daily standup tomorrow 9am", "standup every day at 9am tomorrow", "every other week Friday 3pm", "wöchentlich Freitag 9 Uhr morgens",
            "monthly review on the 12th at 6pm", "Sync jede Woche Freitag 15 Uhr", "Yoga montags morgen 18 Uhr")) {
            val e = Jot.event(t, now)
            assertTrue(t, e.dayGiven && e.timeGiven && e.repeats)
            assertFalse(t, e.sure)
        }
    }

    @Test fun aSecondDayOrTimeLeftInTheTitleIsAGuess() {
        // Two were typed and one was read: which of them was meant is a guess.
        for ((t, title) in listOf("Mon 3pm Tue 4pm" to "Tue 4pm", "lunch Friday 3pm 4pm" to "lunch 4pm", "offsite Oct 14 - Oct 16 6pm" to "offsite 6pm", "lunch tomorrow tomorrow 7pm" to "lunch tomorrow",
            "Meet Friday at 5pm or Saturday at 6pm" to "Meet Friday at 5pm or", "Sprint Oct 5 to Dec 20 9am" to "Sprint Oct 5 to", "tomorrow 3pm all day" to "all day",
            "Dinner tomorrow 7am in the evening" to "Dinner in the evening", "Essen morgen 15 Uhr morgens" to "Essen morgens", "Dinner Friday 7pm at noon" to "Dinner 7pm")) {
            val e = Jot.event(t, now)
            assertEquals(t, title, e.title)
            assertTrue(t, e.dayGiven && e.timeGiven && e.twice)
            assertFalse(t, e.sure)
        }
        // What is far more often something else is no second day: a short weekday that is a word too, two bare numbers, a number after "at" with a word after it.
        for (t in listOf("Sun deck party tomorrow 3pm", "Sat nav update Friday 3pm", "Meeting with May tomorrow 3pm")) {
            val e = Jot.event(t, now)
            assertFalse(t, e.twice)
            assertTrue(t, e.sure)
        }
        // Nor is a number that stands alone in the title one; but what it meant was not read, and that is a guess of its own.
        for (t in listOf("buy 2-3 gifts tomorrow 5pm", "dinner at 5 Main Street tomorrow 7pm", "2nd interview tomorrow 3pm", "Launch Booklight 1.1 tomorrow 3pm")) {
            val e = Jot.event(t, now)
            assertFalse(t, e.twice)
            assertEquals(t, setOf(Doubt.LEFT), e.doubts)
            assertFalse(t, e.sure)
        }
    }

    @Test fun whatIsReadBetweenTheWordsOfAnEvent() {
        // The edges of what the middle of a line is read as, each pinned.
        assertEquals(EventDraft("buy 2 tickets for 7 people", at(2), at(3), true, "", dayGiven = true, timeGiven = false, doubts = setOf(Doubt.LEFT)), Jot.event("buy 2 tickets for 7 people Friday", now))
        for (t in listOf("version 12 notes", "May Smith", "march to the office", "meet Wed Thompson", "we sat on the sun deck", "add 1/2 cup sugar", "dinner at 5 Main Street", "run 10k", "call 555-1234"))
            assertEquals(t, EventDraft(t, at(1), at(2), true, "", dayGiven = false, timeGiven = false), Jot.event(t, now))
        assertEquals("meet Wed Thompson", Jot.event("meet Wed Thompson at noon", now).title)
        // A word that is left where the two halves stood stays in the title.
        assertEquals("lunch at", Jot.event("lunch tomorrow at", now).title)
    }

    // Reminder

    @Test fun inSoLongIsATimer() {
        assertEquals(ReminderPlan.Timer(TimerSpec(1200, "stretch")), Jot.reminder("in 20m stretch", now))
        assertEquals(ReminderPlan.Timer(TimerSpec(90, "x")), Jot.reminder("x in 90s", now))
        assertEquals(ReminderPlan.Timer(TimerSpec(86400, "")), Jot.reminder("in 24h", now))
    }

    @Test fun aTimeInTheNextDayIsAnAlarm() {
        assertEquals(ReminderPlan.Alarm(AlarmSpec(17, 0, "call bank"), at(1, 17)), Jot.reminder("5pm call bank", now))
        assertEquals(ReminderPlan.Alarm(AlarmSpec(9, 0, "standup"), at(2, 9)), Jot.reminder("9am standup", now))
        assertEquals(ReminderPlan.Alarm(AlarmSpec(10, 0, "x"), at(2, 10)), Jot.reminder("tomorrow 10:00 x", now))
    }

    @Test fun laterIsAnEvent() {
        assertEquals(ReminderPlan.Event(EventDraft("call", at(2, 17), at(2, 17, 30), false, "", dayGiven = true, timeGiven = true)),
            Jot.reminder("tomorrow 5pm call", now))
        assertEquals(ReminderPlan.Event(EventDraft("x", at(2, 16), at(2, 16, 30), false, "", dayGiven = false, timeGiven = true)),
            Jot.reminder("in 30h x", now))
    }

    @Test fun aDayWithoutATimeIsNineInTheMorning() {
        assertEquals(ReminderPlan.Event(EventDraft("dentist", at(2, 9), at(2, 9, 30), false, "", dayGiven = true, timeGiven = false)),
            Jot.reminder("Friday dentist", now))
        assertEquals(ReminderPlan.Event(EventDraft("x", at(4, 9), at(4, 9, 30), false, "", dayGiven = true, timeGiven = false)),
            Jot.reminder("in 3 days x", now))
    }

    @Test fun aReminderNeedsAWhen() {
        assertNull(Jot.reminder("call bank", now))
        assertNull(Jot.reminder("", now))
        assertNull(Jot.reminder("3 things", now))
        // "All day" is no time to ring at.
        assertNull(Jot.reminder("all day sale", now))
        assertNull(Jot.reminder("Inventur ganztägig", now))
        // A reminder reads the ends of its line, and a time that does not say its half of the day, as it always has.
        assertEquals(ReminderPlan.Alarm(AlarmSpec(19, 0, "call"), at(1, 19)), Jot.reminder("call at 7", now))
        assertEquals(ReminderPlan.Event(EventDraft("sale", at(2, 9), at(2, 9, 30), false, "", dayGiven = true, timeGiven = false)), Jot.reminder("all day sale tomorrow", now))
    }

    // Timer

    @Test fun timerLengths() {
        assertEquals(TimerSpec(600, ""), Jot.timer("10"))
        assertEquals(TimerSpec(600, "tea"), Jot.timer("10m tea"))
        assertEquals(TimerSpec(90, ""), Jot.timer("90s"))
        assertEquals(TimerSpec(5400, ""), Jot.timer("1h30"))
        assertEquals(TimerSpec(5400, ""), Jot.timer("1h 30m"))
        assertEquals(TimerSpec(5400, ""), Jot.timer("1.5h"))
        assertEquals(TimerSpec(5400, ""), Jot.timer("1,5h"))
        assertEquals(TimerSpec(5420, ""), Jot.timer("1h30m20s"))
        assertEquals(TimerSpec(90, ""), Jot.timer("1m30"))
        assertEquals(TimerSpec(86400, ""), Jot.timer("24h"))
    }

    @Test fun timerUnitsAsWords() {
        assertEquals(TimerSpec(600, ""), Jot.timer("10 min"))
        assertEquals(TimerSpec(7200, ""), Jot.timer("2 std"))
        assertEquals(TimerSpec(7200, ""), Jot.timer("2 Std."))
        assertEquals(TimerSpec(30, ""), Jot.timer("30 sek"))
        assertEquals(TimerSpec(300, ""), Jot.timer("5 minutes"))
        assertEquals(TimerSpec(7200, "deep work"), Jot.timer("2 hours deep work"))
        assertEquals(TimerSpec(5400, "x"), Jot.timer("1 Stunde 30 Minuten x"))
    }

    @Test fun timerLabels() {
        assertEquals(TimerSpec(600, "Tea  Time"), Jot.timer(" 10  Tea  Time "))
        assertEquals(TimerSpec(600, "tea"), Jot.timer("in 10m tea"))
        assertEquals(TimerSpec(600, ""), Jot.timer("in 10"))
        assertEquals(TimerSpec(3600, "30 pushups"), Jot.timer("1h 30 pushups"))     // a number after a space needs its unit
        assertEquals(TimerSpec(1800, "1h"), Jot.timer("30m 1h"))
        assertEquals(TimerSpec(600, "meetings"), Jot.timer("10 meetings"))
    }

    @Test fun noTimer() {
        for (s in listOf("", "  ", "tea", "tea 10m", "0", "0m", "25h", "1441", "10x", "m", "in", "in tea", "1e3", "10:30", "-5",
            "99999999999999999999")) assertNull(s, Jot.timer(s))
    }

    // Alarm

    @Test fun alarmTimes() {
        assertEquals(AlarmSpec(7, 30, "gym"), Jot.alarm("7:30 gym", now))
        assertEquals(AlarmSpec(7, 0, ""), Jot.alarm("7am", now))
        assertEquals(AlarmSpec(19, 0, "x"), Jot.alarm("7 pm x", now))
        assertEquals(AlarmSpec(19, 0, ""), Jot.alarm("19 Uhr", now))
        assertEquals(AlarmSpec(12, 0, "lunch"), Jot.alarm("noon lunch", now))
    }

    @Test fun aNumberAloneIsTheHour() {
        assertEquals(AlarmSpec(7, 0, ""), Jot.alarm("7", now))
        assertEquals(AlarmSpec(7, 0, "gym"), Jot.alarm("7 gym", now))
        assertEquals(AlarmSpec(7, 0, ""), Jot.alarm("at 7", now))
        assertEquals(AlarmSpec(18, 0, "leave"), Jot.alarm("in 8h leave", now))
    }

    @Test fun noAlarm() {
        for (s in listOf("", "  ", "gym", "gym 7", "25", "7:75", "13pm", "9-10", "in", "in 30h", "at")) assertNull(s, Jot.alarm(s, now))
    }

    // New

    @Test fun newWithAKindWord() {
        assertEquals(NewSpec(NewKind.FILE, "ideas.md", true), Jot.new("file ideas.md"))
        assertEquals(NewSpec(NewKind.FOLDER, "Projects/Alpha", true), Jot.new("folder Projects/Alpha"))
        assertEquals(NewSpec(NewKind.DOC, "Title of it", true), Jot.new("doc Title of it"))
        assertEquals(NewSpec(NewKind.SHEET, "Budget", true), Jot.new("sheet Budget"))
        assertEquals(NewSpec(NewKind.SLIDES, "", true), Jot.new("slides"))
        assertEquals(NewSpec(NewKind.FOLDER, "x", true), Jot.new("dir x"))
    }

    @Test fun newInGerman() {
        assertEquals(NewSpec(NewKind.FOLDER, "X", true), Jot.new("Ordner X"))
        assertEquals(NewSpec(NewKind.FILE, "x", true), Jot.new("datei x"))
        assertEquals(NewSpec(NewKind.DOC, "Brief", true), Jot.new("Dokument Brief"))
        assertEquals(NewSpec(NewKind.SHEET, "Budget 2027", true), Jot.new("Tabelle Budget 2027"))
        assertEquals(NewSpec(NewKind.SLIDES, "Q3", true), Jot.new("Präsentation Q3"))
    }

    @Test fun newWithoutAKindIsAFileOrAFolder() {
        assertEquals(NewSpec(NewKind.FILE, "ideas.md", false), Jot.new("ideas.md"))
        assertEquals(NewSpec(NewKind.FILE, "My file", false), Jot.new(" My file "))
        assertEquals(NewSpec(NewKind.FILE, "doc.md", false), Jot.new("doc.md"))
        assertEquals(NewSpec(NewKind.FOLDER, "Projects", false), Jot.new("Projects/"))
        assertEquals(NewSpec(NewKind.FOLDER, "A/B", false), Jot.new("A/B/"))
        assertEquals(NewSpec(NewKind.FILE, "", false), Jot.new(""))
    }

    // A day or a time inside a copied sentence. Today is Thursday 1 October 2026, 10:00.

    private fun copied(text: String, near: String) = Jot.eventIn(text, near, now)?.let { Jot.event(it, now) }

    @Test fun aDateInASentenceIsReadWithItsSentenceAsTheTitle() {
        val e = copied("Dinner with Anna on Friday at 7pm. Call +49 30 5550 1234 or book at https://example.com/table", "Friday at 7pm")!!
        assertEquals(LocalDateTime.of(2026, 10, 2, 19, 0), e.start)
        assertEquals("Dinner with Anna", e.title)
        // The system may have found only a part of it: the whole expression is still read.
        assertEquals(LocalDateTime.of(2026, 10, 2, 19, 0), copied("Dinner with Anna on Friday at 7pm. Call", "Friday")!!.start)
    }

    @Test fun aDateInTheMiddleLeavesTheWordsOnBothSides() {
        val e = copied("The review is on 12 October at 10:30 in room 4", "12 October at 10:30")!!
        assertEquals(LocalDateTime.of(2026, 10, 12, 10, 30), e.start)
        assertEquals("The review is in room 4", e.title)
        assertEquals("Meeting with Sam", copied("Meeting on Oct 12, 2026 at 3 PM with Sam", "Oct 12, 2026 at 3 PM")!!.title)
    }

    @Test fun aDayAndATimeMayStandApart() {
        val e = copied("Am Freitag treffen wir uns um 15 Uhr im Büro.", "Freitag")!!
        assertEquals(LocalDateTime.of(2026, 10, 2, 15, 0), e.start)
        assertEquals("treffen wir uns im Büro", e.title)
        val f = copied("Your order arrives 5 Oct between 9-11", "5 Oct")!!
        assertEquals(LocalDateTime.of(2026, 10, 5, 9, 0), f.start)
        assertEquals("Your order arrives", f.title)
    }

    @Test fun aNumbersDotDoesNotEndTheSentence() {
        val e = copied("Wir sehen uns am 3. Oktober um 18 Uhr. Bis dann", "3. Oktober um 18 Uhr")!!
        assertEquals(LocalDateTime.of(2026, 10, 3, 18, 0), e.start)
        assertEquals("Wir sehen uns", e.title)
    }

    @Test fun aWordLeftHangingIsDropped() {
        assertEquals("Abgabe", copied("Abgabe bis 14.10.2026", "14.10.2026")!!.title)
        assertEquals("See you", copied("See you tomorrow!", "tomorrow")!!.title)
    }

    @Test fun onlyTheSentenceTheSystemPointedAtIsRead() {
        // "sun" is a short form of Sunday: it is not read, because the system's date shares no word with it.
        val e = copied("The sun is out. See you on Sunday", "Sunday")!!
        assertEquals("See you", e.title)
        assertNull(Jot.eventIn("The sun is out. See you later", "Sunday", now))
        // A month's day is no span's first hour, and the number before „Uhr“ no last day of a range: both as 3.1 read them.
        assertEquals("Oct 14 noon Shift to", Jot.eventIn("Shift Oct 14 to noon.", "Oct 14", now))
        assertEquals("Oct 14 18 Uhr Shift", Jot.eventIn("Shift Oct 14 until 18 Uhr.", "Oct 14", now))
        assertEquals("am 14. Oktober 18 Uhr Schicht", Jot.eventIn("Schicht am 14. Oktober bis 18 Uhr.", "14. Oktober", now))
        assertEquals("morgen 9 Uhr Schicht", Jot.eventIn("Schicht morgen bis 9 Uhr.", "morgen", now))
        // What is a range of days by its own words is one, and a span that ends at noon keeps its start.
        assertEquals("Oct 14 - 18 Open", Jot.eventIn("Open Oct 14 - 18.", "Oct 14", now))
        assertEquals("tomorrow 9 to noon Workshop in room 4", Jot.eventIn("Workshop tomorrow 9 to noon in room 4.", "tomorrow", now))
        assertNull(Jot.eventIn("Nothing here", "", now))
    }
}
