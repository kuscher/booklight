package io.github.kuscher.booklight.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// The two halves of what When reads: a day and a time of day. Both take lower-case words and
// either understand all of them or return null.

internal sealed interface Day {
    class On(val date: LocalDate) : Day
    /** A weekday. [weak]: a short form that is an ordinary word too ("do", "so", "we"); it needs a time beside it. */
    class Every(val day: DayOfWeek, val weak: Boolean) : Day
}

internal object Days {
    private val WORDS = mapOf("today" to 0, "heute" to 0, "tomorrow" to 1, "morgen" to 1, "ubermorgen" to 2, "uebermorgen" to 2)
    private val WEAK = setOf("do", "so", "we")
    private val WEEKDAYS: Map<String, DayOfWeek> = names(
        "monday mon mo montag", "tuesday tue tues tu dienstag di", "wednesday wed weds we mittwoch mi",
        "thursday thu thur thurs th donnerstag do", "friday fri fr freitag", "saturday sat sa samstag sonnabend",
        "sunday sun su sonntag so",
    ).mapValues { DayOfWeek.of(it.value) }
    private val MONTHS: Map<String, Int> = names(
        "january jan januar", "february feb februar", "march mar marz maerz mrz", "april apr", "may mai",
        "june jun juni", "july jul juli", "august aug", "september sep sept", "october oct oktober okt",
        "november nov", "december dec dezember dez",
    )
    private val AHEAD = mapOf("d" to 1, "day" to 1, "days" to 1, "tag" to 1, "tage" to 1, "tagen" to 1,
        "w" to 7, "wk" to 7, "wks" to 7, "week" to 7, "weeks" to 7, "woche" to 7, "wochen" to 7)
    private val AHEAD_MONTHS = setOf("month", "months", "monat", "monate", "monaten")

    /** Each string lists the names of number 1, 2, 3…, already folded. */
    private fun names(vararg lists: String): Map<String, Int> = buildMap {
        lists.forEachIndexed { i, list -> for (w in list.split(' ')) put(w, i + 1) }
    }

    fun parse(words: List<String>, today: LocalDate): Day? = when {
        words.isEmpty() -> null
        words.size == 1 -> one(words[0], today)
        words[0] == "in" -> ahead(words, today)?.let { Day.On(it) }
        else -> named(words, today)?.let { Day.On(it) }
    }

    private fun one(word: String, today: LocalDate): Day? {
        val f = Matcher.fold(word)
        WORDS[f]?.let { return Day.On(today.plusDays(it.toLong())) }
        WEEKDAYS[f]?.let { return Day.Every(it, f in WEAK) }
        return numeric(word, today)?.let { Day.On(it) }
    }

    /** 3.10. and 3.10.2026 (day first), 10/3 and 10/3/2026 (month first), 2026-10-03. */
    private fun numeric(word: String, today: LocalDate): LocalDate? {
        val p = word.split('.', '/', '-')
        return when {
            p.size < 2 || p.size > 4 -> null
            '.' in word && '/' !in word && '-' !in word -> when {
                p.size == 3 && p[2].isEmpty() -> date(null, p[1], p[0], today)              // "3.10" could be a version: the dot is needed
                p.size == 3 || (p.size == 4 && p[3].isEmpty()) -> date(year(p[2]), p[1], p[0], today, yearGiven = true)
                else -> null
            }
            '/' in word && '.' !in word && '-' !in word -> when (p.size) {
                2 -> date(null, p[0], p[1], today)
                3 -> date(year(p[2]), p[0], p[1], today, yearGiven = true)
                else -> null
            }
            '-' in word && '.' !in word && '/' !in word ->
                if (p.size == 3 && p[0].length == 4) date(year(p[0]), p[1], p[2], today, yearGiven = true) else null
            else -> null
        }
    }

    /** "oct 3", "3 oct", "3. Okt", "October 3rd 2027". */
    private fun named(words: List<String>, today: LocalDate): LocalDate? {
        if (words.size !in 2..3) return null
        val year = if (words.size == 3) year(words[2])?.takeIf { words[2].length == 4 } ?: return null else null
        val (a, b) = words
        val monthFirst = MONTHS[Matcher.fold(a)]
        val month = monthFirst ?: MONTHS[Matcher.fold(b)] ?: return null
        val day = dayNumber(if (monthFirst != null) b else a) ?: return null
        return fixed(year, month, day, today)
    }

    /** "in 3 days", "in 3d", "in 2 Wochen", "in 1 month". */
    private fun ahead(words: List<String>, today: LocalDate): LocalDate? {
        val (number, unit) = when (words.size) {
            2 -> words[1].takeWhile { it in '0'..'9' } to words[1].dropWhile { it in '0'..'9' }
            3 -> words[1] to words[2]
            else -> return null
        }
        val n = digits(number, 4) ?: return null
        val u = unit.trimEnd('.', ',')
        AHEAD[u]?.let { return today.plusDays(n.toLong() * it) }
        return if (u in AHEAD_MONTHS) today.plusMonths(n.toLong()) else null
    }

    private fun date(year: Int?, month: String, day: String, today: LocalDate, yearGiven: Boolean = false): LocalDate? {
        if (yearGiven && year == null) return null
        return fixed(year, digits(month, 2) ?: return null, digits(day, 2) ?: return null, today)
    }

    /** The date itself when the year is given; otherwise the next time that day of that month comes (today counts). */
    private fun fixed(year: Int?, month: Int, day: Int, today: LocalDate): LocalDate? {
        if (month !in 1..12 || day < 1) return null
        if (year != null) return if (day <= YearMonth.of(year, month).lengthOfMonth()) LocalDate.of(year, month, day) else null
        for (y in today.year..today.year + 8) {                                       // 29.2. waits for a leap year
            if (day > YearMonth.of(y, month).lengthOfMonth()) continue
            val d = LocalDate.of(y, month, day)
            if (!d.isBefore(today)) return d
        }
        return null
    }

    private fun year(s: String): Int? = when (s.length) { 4 -> digits(s, 4); 2 -> digits(s, 2)?.plus(2000); else -> null }

    private fun dayNumber(word: String): Int? {
        var t = word.trimEnd(',', '.')
        for (suffix in listOf("st", "nd", "rd", "th")) t = t.removeSuffix(suffix)
        return digits(t, 2)?.takeIf { it in 1..31 }
    }

    private fun digits(s: String, max: Int): Int? =
        if (s.isEmpty() || s.length > max || s.any { it !in '0'..'9' }) null else s.toInt()
}

/** A time of day and, for "9-10", where it ends: minutes from that day's midnight (past 1440 is the next day). */
internal class Clock(val time: LocalTime, val end: Int?)

internal object Times {
    private val MARKS = setOf("am", "pm", "a.m.", "p.m.", "uhr")
    private const val DASHES = "-–—"
    private val NAMED = listOf("midnight" to 0, "mitternacht" to 0, "mittags" to 12, "mittag" to 12, "noon" to 12)

    private class Part(val hour: Int, val minute: Int, val mark: Char, val exact: Boolean)

    /**
     * The time that [words] are: 3pm, "3 pm", 3:30pm, 15:00, "15 Uhr", noon; and spans, 9-9:30, 9am-10am,
     * "9 – 10". A number alone ("7") counts only if [bare]. With [afternoon], an hour from 1 to 7 that
     * has no am, pm or Uhr is 13 to 19 ("at 3", "at 3:30").
     */
    fun parse(words: List<String>, bare: Boolean, afternoon: Boolean = false, span: Boolean = true): Clock? {
        val s = join(words) ?: return null
        val (a, next) = part(s, 0) ?: return null
        if (a.minute > 59) return null
        if (next == s.length) {
            var hour = hour24(a, a.mark) ?: return null
            if (a.mark == ' ' && !a.exact && !bare) return null
            if (a.mark == ' ' && afternoon && hour in 1..7) hour += 12
            return Clock(LocalTime.of(hour, a.minute), null)
        }
        if (!span || s[next] !in DASHES) return null
        val (b, done) = part(s, next + 1) ?: return null
        if (done != s.length || b.minute > 59) return null
        // "9-10am": the start borrows the end's half of the day, unless that puts it after the end ("11-1pm").
        val borrowed = a.mark == ' ' && (b.mark == 'a' || b.mark == 'p')
        var start = (hour24(a, if (borrowed) b.mark else a.mark) ?: return null) * 60 + a.minute
        var end = (hour24(b, b.mark) ?: return null) * 60 + b.minute
        if (borrowed && start >= end) ((start + 720) % 1440).let { if (it < end) start = it }
        // A span that starts at 1 to 7 with no am, pm or Uhr is a meeting after lunch, not at night: "2-3" is 14:00 to 15:00.
        if (a.mark == ' ' && !borrowed && start in 60 until 480) start += 720
        if (end <= start) end += if (b.mark == ' ' && end < 720 && end + 720 > start) 720 else 1440
        return Clock(LocalTime.of(start / 60, start % 60), end)
    }

    /** "3" "pm" → 3pm, "9" "-" "10" → 9-10. Any other second word means these words are not one time. */
    private fun join(words: List<String>): String? {
        if (words.isEmpty()) return null
        val b = StringBuilder(words[0])
        var i = 1
        while (i < words.size) {
            val w = words[i]
            when {
                w in MARKS -> b.append(w)
                w.length == 1 && w[0] in DASHES && i + 1 < words.size -> { b.append('-').append(words[i + 1]); i++ }
                else -> return null
            }
            i++
        }
        return b.toString()
    }

    /** One time starting at [from], and where it stops. Marks: a, p, u (24 hours, said outright), blank. */
    private fun part(s: String, from: Int): Pair<Part, Int>? {
        for ((word, hour) in NAMED) if (s.startsWith(word, from)) return Part(hour, 0, 'u', true) to from + word.length
        var i = from
        while (i < s.length && i - from < 2 && s[i] in '0'..'9') i++
        if (i == from) return null
        val hour = s.substring(from, i).toInt()
        var minute = 0
        var exact = false
        if (i + 2 < s.length && s[i] == ':' && s[i + 1] in '0'..'9' && s[i + 2] in '0'..'9') {
            minute = s.substring(i + 1, i + 3).toInt()
            exact = true
            i += 3
        }
        val mark = when {
            s.startsWith("am", i) -> { i += 2; 'a' }
            s.startsWith("a.m.", i) -> { i += 4; 'a' }
            s.startsWith("pm", i) -> { i += 2; 'p' }
            s.startsWith("p.m.", i) -> { i += 4; 'p' }
            s.startsWith("uhr", i) -> { i += 3; 'u' }
            else -> ' '
        }
        return Part(hour, minute, mark, exact) to i
    }

    private fun hour24(p: Part, mark: Char): Int? = when (mark) {
        'a' -> if (p.hour in 1..12) p.hour % 12 else null
        'p' -> if (p.hour in 1..12) p.hour % 12 + 12 else null
        else -> if (p.hour in 0..23) p.hour else null
    }
}
