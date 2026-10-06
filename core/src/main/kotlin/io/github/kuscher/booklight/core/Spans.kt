package io.github.kuscher.booklight.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * When an event is, as its row says it: "Wed 7 Oct, 7–8 PM", "Wed 14 – Fri 16 Oct, all day".
 *
 * The row never cuts this: what is saved stands on the glass whole, and the title is the slot that gives way.
 * So every form is short, [ROOM] letters at the most in English and German, and where a fuller form would be
 * longer a tighter one is said, in this order:
 *
 * - A time: the weekday, the day, the month, then the times; "3–4 PM" where both ends share their half of the
 *   day. Too long (a year, and minutes at both ends in different halves): without the weekday; then without the
 *   dot some languages end a month's short name with.
 * - All day: the weekday, the day, the month, and "all day" in the reader's words.
 * - A range of days in one month: "Wed 14 – Fri 16 Oct, all day". Too long: without the weekdays ("14 – 16
 *   Oct"). Across months, never the weekdays: "28 Oct – 2 Nov". Then without the room round the dash, then
 *   without the months' dots.
 * - **The year** is said wherever it is not this year's: after the day ("Tue 5 Oct 2027, 3–4 PM"), and in a
 *   range once, after its last day ("30 Dec – 2 Jan 2027"): a range is short, so the first day's year follows
 *   from the last's.
 */
object Spans {
    /**
     * The most letters "When" has: at the size of the row's values a letter is about 8.5 dp, and the row's
     * narrowest text (with Save, Open, Copy and Calendar beside it, about 430 dp) then leaves the title's slot
     * 96 dp after the label "WHEN" and the gap between the two.
     */
    const val ROOM = 31

    /**
     * [e] as its row says it. [today]: which year needs no saying. [locale]: the reader's, for the names of days and
     * months. [h24]: the device shows the 24-hour clock. [allDay]: "all day" with the day before it, in the reader's
     * words ("%s, all day").
     */
    fun say(e: EventDraft, today: LocalDate, locale: Locale, h24: Boolean, allDay: (String) -> String): String {
        fun f(pattern: String, t: LocalDateTime) = t.format(DateTimeFormatter.ofPattern(pattern, locale))
        fun weekday(t: LocalDateTime) = f("EEE", t)
        fun month(t: LocalDateTime, dots: Boolean = true) = f("MMM", t).let { if (dots) it else it.trimEnd('.') }
        fun year(t: LocalDateTime) = if (t.year != today.year) " ${t.year}" else ""
        fun day(t: LocalDateTime, named: Boolean, dots: Boolean = true) = (if (named) weekday(t) + " " else "") + "${f("d", t)} ${month(t, dots)}${year(t)}"
        val forms: List<String> = when {
            !e.allDay -> {
                fun clock(t: LocalDateTime) = f(if (h24) "HH:mm" else if (t.minute == 0) "h a" else "h:mm a", t)
                // "3–4 PM", not "3 PM–4 PM": the half of the day is said once where both ends share it.
                val from = clock(e.start)
                val to = clock(e.end)
                val half = to.substringAfterLast(' ', "")
                val times = (if (half.isNotEmpty() && from.endsWith(" $half")) from.removeSuffix(" $half") else from) + "–" + to
                listOf(day(e.start, named = true), day(e.start, named = false), day(e.start, named = false, dots = false)).map { "$it, $times" }
            }
            // (The end that is kept is the midnight after the last day.)
            !e.end.isAfter(e.start.plusDays(1)) -> listOf(day(e.start, named = true), day(e.start, named = false)).map(allDay)
            else -> {
                val first = e.start
                val last = e.end.minusDays(1)
                val year = if (first.year != today.year || last.year != today.year) " ${last.year}" else ""
                fun d(t: LocalDateTime) = f("d", t)
                val one = first.year == last.year && first.month == last.month
                fun range(dash: String, dots: Boolean) =
                    (if (one) d(first) else "${d(first)} ${month(first, dots)}") + dash + "${d(last)} ${month(last, dots)}$year"
                listOfNotNull("${weekday(first)} ${d(first)} – ${weekday(last)} ${d(last)} ${month(last)}$year".takeIf { one }, range(" – ", true), range("–", true), range("–", false)).map(allDay)
            }
        }
        return forms.firstOrNull { it.length <= ROOM } ?: forms.last()
    }
}
