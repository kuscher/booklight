package io.github.kuscher.booklight.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToLong

/**
 * A day, a time or both, read off a typed line. [end] only when an end time was typed ("9-9:30").
 * [allDay] when no time was given: [start] is then that day's midnight. [dayGiven] and [timeGiven] say
 * what was typed and what was guessed; [rest] is the line without the expression.
 */
data class Moment(val start: LocalDateTime, val end: LocalDateTime?, val allDay: Boolean,
    val dayGiven: Boolean, val timeGiven: Boolean, val rest: String)

/**
 * Reads when something is, in English and German: "Fri 3pm Dentist", "Standup tomorrow 9-9:30",
 * "Zahnarzt am Freitag um 15 Uhr", "in 20m stretch".
 *
 * The rules, kept few so that the preview never surprises:
 * - The expression is the first or the last words of the line, never the middle. The longest reading
 *   wins; if both ends read equally long, the start. A day and a time may stand in either order.
 * - Days: today, tomorrow, heute, morgen, übermorgen; weekday names and their short forms (mon, mo,
 *   Montag): the next such day, or today when a time is given that is still ahead. Dates: 3.10.
 *   3.10.2026 10/3 2026-10-03 "oct 3" "3 oct" "3. Okt"; without a year, the next time that date comes.
 *   "in 3 days", "in 2 Wochen". "do", "so" and "we" are ordinary words too: days only beside a time.
 * - Times: 3pm 3:30pm 15:00 "15 Uhr" noon midnight Mittag. With a colon and no am or pm, the 24-hour
 *   clock. A number alone is not a time ("3 Dentists"), except in a span and after "at" or "um".
 *   After those two, an hour from 1 to 7 without am, pm or Uhr is the afternoon: "at 3" is 15:00,
 *   "at 3:30" 15:30, "at 8" 08:00.
 * - Spans: 9-9:30, 9am-10am, 9–10. An end that isn't after the start moves 12 hours on when that
 *   helps ("10-2"), else to the next day. A span that starts at 1 to 7 with no am or pm is the
 *   afternoon ("2-3" is 14:00 to 15:00).
 * - A day at one end and a time at the other also work: "Friday Dentist 3pm".
 * - "next", "this", "nächsten", "kommenden" in front of a day are read with it and change nothing.
 * - "in 20m", "in 2h", "in 1h 30m": that long from [now].
 * - Only a time: today if it is still ahead, else tomorrow. Only a day: all day.
 * - "at", "on", "um", "am" in front of the expression belong to it ("Dentist at 3pm" leaves
 *   "Dentist"). "@" never does: it marks a place.
 */
object When {
    /** The most words an expression has: "am 3. Okt 2026 um 9 am - 10 am". */
    private const val SPAN = 10
    private const val YEAR = 366L * 24 * 3600
    /** Words that lead into a day or a time; the two in [HOUR] make a number alone an hour. */
    private val LEADING = setOf("at", "on", "um", "am", "next", "this", "coming", "nächsten", "nächste", "nächster", "kommenden", "diesen", "dieses")
    private val HOUR = setOf("at", "um")

    /** What [text] says about when, or null when neither end of it is a day or a time. */
    fun parse(text: String, now: LocalDateTime): Moment? {
        val hit = find(text, now) ?: return null
        return Moment(hit.start, hit.end, !hit.time, hit.day, hit.time, hit.rest)
    }

    internal fun find(text: String, now: LocalDateTime): Hit? {
        val one = findOne(text, now) ?: return null
        if (one.seconds != null || (one.day && one.time)) return one
        // Only a day, or only a time: the other half may stand at the other end of what is left.
        val two = findOne(one.rest, now)?.takeIf { it.seconds == null && it.day != one.day && it.time != one.time } ?: return one
        val day = if (one.day) one else two
        val time = if (one.time) one else two
        val start = day.start.toLocalDate().atTime(time.start.toLocalTime())
        val end = time.end?.let { start.plusSeconds(java.time.Duration.between(time.start, it).seconds) }
        return Hit(start, end, day = true, time = true).also { it.rest = two.rest }
    }

    /** As [Moment], plus [seconds] when the expression was "in 20m": reminders make a timer of those. */
    internal class Hit(val start: LocalDateTime, val end: LocalDateTime?, val day: Boolean, val time: Boolean, val seconds: Long? = null) {
        var rest = ""
    }

    private fun findOne(text: String, now: LocalDateTime): Hit? {
        val words = Words.of(text)
        val n = words.size
        val low = words.map { it.text.lowercase().trimEnd(',', ';') }
        for (k in minOf(n, SPAN) downTo 1) {
            read(low.subList(0, k), now)?.let { it.rest = Words.cut(text, words, k, n); return it }
            if (k < n) read(low.subList(n - k, n), now)?.let { it.rest = Words.cut(text, words, 0, n - k).trimEnd(',', ';'); return it }
        }
        return null
    }

    /** [w] as one whole expression, or null. */
    private fun read(w: List<String>, now: LocalDateTime): Hit? {
        if (w[0] == "in") relative(w, now)?.let { return it }
        val led = w[0] in LEADING
        val hour = w[0] in HOUR
        val body = if (led) w.subList(1, w.size) else w
        if (body.isEmpty()) return null
        val today = now.toLocalDate()
        Days.parse(body, today)?.let { return resolve(it, null, now) }
        Times.parse(body, bare = hour, afternoon = hour)?.let { return resolve(null, it, now) }
        // A day then a time, or a time then a day. The longer first part first, so "9 am Freitag" is 9 am.
        for (i in body.size - 1 downTo 1) {
            val a = body.subList(0, i)
            var b = body.subList(i, body.size)
            val at = b[0] in HOUR
            if (b[0] in LEADING) b = b.subList(1, b.size)
            if (b.isEmpty()) continue
            val day = Days.parse(a, today)
            if (day != null) Times.parse(b, bare = at, afternoon = at)?.let { return resolve(day, it, now) }
            val clock = Times.parse(a, bare = hour, afternoon = hour)
            if (clock != null) Days.parse(b, today)?.let { return resolve(it, clock, now) }
        }
        return null
    }

    private fun relative(w: List<String>, now: LocalDateTime): Hit? {
        val length = Durations.leading(w.subList(1, w.size), bareMinutes = false) ?: return null
        if (length.used != w.size - 1 || !(length.seconds >= 1 && length.seconds <= YEAR)) return null
        val seconds = length.seconds.roundToLong()
        return Hit(now.plusSeconds(seconds), null, day = false, time = true, seconds = seconds)
    }

    private fun resolve(day: Day?, clock: Clock?, now: LocalDateTime): Hit? {
        val today = now.toLocalDate()
        if (clock == null) {
            val date = when (day) {
                is Day.On -> day.date
                is Day.Every -> if (day.weak) return null else next(today, day.day)
                null -> return null
            }
            return Hit(date.atStartOfDay(), null, day = true, time = false)
        }
        val ahead = clock.time.isAfter(now.toLocalTime())
        val date = when (day) {
            null -> if (ahead) today else today.plusDays(1)
            is Day.On -> day.date
            is Day.Every -> if (today.dayOfWeek == day.day && ahead) today else next(today, day.day)
        }
        val end = clock.end?.let { date.atStartOfDay().plusMinutes(it.toLong()) }
        return Hit(date.atTime(clock.time), end, day = day != null, time = true)
    }

    /** The next [day] after [today], one to seven days on. */
    private fun next(today: LocalDate, day: DayOfWeek): LocalDate {
        val gap = (day.value - today.dayOfWeek.value + 7) % 7
        return today.plusDays(if (gap == 0) 7L else gap.toLong())
    }
}
