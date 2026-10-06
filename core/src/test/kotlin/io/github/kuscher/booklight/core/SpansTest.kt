package io.github.kuscher.booklight.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When an event is, as its row says it ([Spans.say]): every form, with its longest example in English and in German.
 * What is saved must stand on the glass whole, so the row never cuts it: each form is short enough to leave the title
 * its room ([Spans.ROOM] letters). "Today" is Tuesday 6 October 2026.
 */
class SpansTest {
    private val today = LocalDate.of(2026, 10, 6)
    private fun at(day: Int, hour: Int = 0, minute: Int = 0, month: Int = 10, year: Int = 2026) = LocalDateTime.of(year, month, day, hour, minute)
    private fun timed(start: LocalDateTime, end: LocalDateTime) = EventDraft("x", start, end, false, "", dayGiven = true, timeGiven = true)
    private fun days(first: LocalDateTime, last: LocalDateTime = first) = EventDraft("x", first, last.plusDays(1), true, "", dayGiven = true, timeGiven = true)

    private fun en(e: EventDraft, h24: Boolean = false) = Spans.say(e, today, Locale.US, h24) { "$it, all day" }
    private fun de(e: EventDraft, h24: Boolean = true) = Spans.say(e, today, Locale.GERMANY, h24) { "$it, ganztägig" }

    @Test fun aTimeOnADayOfThisYear() {
        assertEquals("Wed 7 Oct, 7–8 PM", en(timed(at(7, 19), at(7, 20))))
        assertEquals("Wed 7 Oct, 11:30 AM–12:30 PM", en(timed(at(7, 11, 30), at(7, 12, 30))))
        assertEquals("Wed 28 Oct, 10:30–11:30 AM", en(timed(at(28, 10, 30), at(28, 11, 30))))
        assertEquals("Wed 28 Oct, 10:30 AM–12:30 PM", en(timed(at(28, 10, 30), at(28, 12, 30))))
        assertEquals("Wed 7 Oct, 11 PM–1 AM", en(timed(at(7, 23), at(8, 1))))
        assertEquals("Wed 7 Oct, 19:00–20:00", en(timed(at(7, 19), at(7, 20)), h24 = true))
        assertEquals("Mi. 7 Okt., 19:00–20:00", de(timed(at(7, 19), at(7, 20))))
        assertEquals("Mi. 28 Okt., 10:30–12:30", de(timed(at(28, 10, 30), at(28, 12, 30))))
        assertEquals("Mi. 28 Okt., 10:30 AM–12:30 PM", de(timed(at(28, 10, 30), at(28, 12, 30)), h24 = false))
    }

    @Test fun theYearIsSaidWhereverItIsNotThisYears() {
        assertEquals("Tue 5 Oct 2027, 3–4 PM", en(timed(at(5, 15, year = 2027), at(5, 16, year = 2027))))
        assertEquals("Fri 15 Jan 2027, 9–10 AM", en(timed(at(15, 9, month = 1, year = 2027), at(15, 10, month = 1, year = 2027))))
        assertEquals("Sat 3 Oct 2020, 3–4 PM", en(timed(at(3, 15, year = 2020), at(3, 16, year = 2020))))
        assertEquals("Thu 15 Apr 2027, all day", en(days(at(15, month = 4, year = 2027))))
        assertEquals("Di. 5 Okt. 2027, 15:00–16:00", de(timed(at(5, 15, year = 2027), at(5, 16, year = 2027))))
        assertEquals("Do. 15 Apr. 2027, ganztägig", de(days(at(15, month = 4, year = 2027))))
        // Where the year would cost the title its room, the weekday gives way: the date and both times stand whole.
        assertEquals("Thu 28 Oct 2027, 10:30–11:30 AM", en(timed(at(28, 10, 30, year = 2027), at(28, 11, 30, year = 2027))))
        assertEquals("28 Oct 2027, 10:30 AM–12:30 PM", en(timed(at(28, 10, 30, year = 2027), at(28, 12, 30, year = 2027))))
        assertEquals("Di. 28 Sept. 2027, 10:30–12:30", de(timed(at(28, 10, 30, month = 9, year = 2027), at(28, 12, 30, month = 9, year = 2027))))
        // (On the 12-hour clock in German the longest month loses its dot too: thirty-one letters.)
        assertEquals("10 Sept 2027, 10:30 AM–12:30 PM", de(timed(at(10, 10, 30, month = 9, year = 2027), at(10, 12, 30, month = 9, year = 2027)), h24 = false))
        assertEquals("10 Jan. 2027, 10:30 AM–12:30 PM", de(timed(at(10, 10, 30, month = 1, year = 2027), at(10, 12, 30, month = 1, year = 2027)), h24 = false))
        // And in a range of days the year is said once, after its last day: the first day's own follows from it.
        assertEquals("14 – 16 Oct 2027, all day", en(days(at(14, year = 2027), at(16, year = 2027))))
        assertEquals("30 Dec – 2 Jan 2027, all day", en(days(at(30, month = 12), at(2, month = 1, year = 2027))))
        assertEquals("30 Dec – 2 Jan 2028, all day", en(days(at(30, month = 12, year = 2027), at(2, month = 1, year = 2028))))
        // This year's dates say none.
        for (e in listOf(timed(at(7, 19), at(7, 20)), days(at(7)), days(at(14), at(16)), days(at(28), at(2, month = 11)))) assertFalse(en(e), "2026" in en(e))
    }

    @Test fun allDayAndARangeOfDays() {
        assertEquals("Wed 7 Oct, all day", en(days(at(7))))
        assertEquals("Mi. 7 Okt., ganztägig", de(days(at(7))))
        // In one month: both weekdays, the month once.
        assertEquals("Wed 14 – Fri 16 Oct, all day", en(days(at(14), at(16))))
        assertEquals("Mi. 14 – Fr. 16 Okt., ganztägig", de(days(at(14), at(16))))
        // Across months: without the weekdays.
        assertEquals("28 Oct – 2 Nov, all day", en(days(at(28), at(2, month = 11))))
        assertEquals("28 Okt. – 2 Nov., ganztägig", de(days(at(28), at(2, month = 11))))
        // Where a long month's name or the year leaves no room, the weekdays go, then the room round the dash, then the months' dots.
        assertEquals("14 – 16 Sept. 2027, ganztägig", de(days(at(14, month = 9, year = 2027), at(16, month = 9, year = 2027))))
        assertEquals("28 Sept–12 Okt 2027, ganztägig", de(days(at(28, month = 9, year = 2027), at(12, month = 10, year = 2027))))
        assertEquals("28 Dez.–12 Jan. 2027, ganztägig", de(days(at(28, month = 12), at(12, month = 1, year = 2027))))
    }

    /** Every form at its longest: every day of three years, the times that are longest to say, in both languages and on both clocks. */
    @Test fun noFormIsLongerThanTheRowHasRoomFor() {
        val longest = HashMap<String, String>()
        fun note(kind: String, said: String) { if (said.length > (longest[kind]?.length ?: 0)) longest[kind] = said }
        var day = LocalDate.of(2026, 1, 1)
        while (day.year < 2029) {
            val d = day.atStartOfDay()
            for ((name, say) in listOf<Pair<String, (EventDraft) -> String>>("en 12" to { en(it) }, "en 24" to { en(it, h24 = true) }, "de 24" to { de(it) }, "de 12" to { de(it, h24 = false) })) {
                note("$name timed", say(timed(d.withHour(10).withMinute(30), d.withHour(12).withMinute(30))))
                note("$name timed", say(timed(d.withHour(22).withMinute(30), d.plusDays(1).withHour(0).withMinute(30))))
                note("$name day", say(days(d)))
                for (n in listOf(1L, 2L, 14L, 45L, 62L)) note("$name days", say(days(d, d.plusDays(n))))
            }
            day = day.plusDays(1)
        }
        assertEquals(12, longest.size)
        for ((kind, said) in longest) assertTrue("$kind: $said (${said.length})", said.length <= Spans.ROOM)
        // The longest of each, as they stand: the first that is as long as any (none is longer than thirty-one letters).
        assertEquals(mapOf(
            "en 12 timed" to "10 Jan 2027, 10:30 AM–12:30 PM", "en 24 timed" to "Sun 10 Jan 2027, 10:30–12:30", "en 12 day" to "Sun 10 Jan 2027, all day", "en 24 day" to "Sun 10 Jan 2027, all day",
            "en 12 days" to "Fri 1 – Sat 2 Jan 2027, all day", "en 24 days" to "Fri 1 – Sat 2 Jan 2027, all day",
            "de 12 timed" to "Do. 10 Sept., 10:30 AM–12:30 PM", "de 24 timed" to "Fr. 10 Sept. 2027, 10:30–12:30", "de 12 day" to "Fr. 10 Sept. 2027, ganztägig", "de 24 day" to "Fr. 10 Sept. 2027, ganztägig",
            "de 12 days" to "Sa. 10 – So. 11 Jan., ganztägig", "de 24 days" to "Sa. 10 – So. 11 Jan., ganztägig",
        ), longest)
    }
}
