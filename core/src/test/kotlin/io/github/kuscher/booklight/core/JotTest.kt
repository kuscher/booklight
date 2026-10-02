package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals(EventDraft("Standup", at(2, 9), at(2, 9, 30), false, "Room 4", dayGiven = true, timeGiven = true),
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
        assertNull(Jot.eventIn("Nothing here", "", now))
    }
}
