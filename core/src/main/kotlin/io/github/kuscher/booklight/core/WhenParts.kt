package io.github.kuscher.booklight.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// The two halves of what When reads: a day and a time of day. Both take lower-case words and
// either understand all of them or return null.

internal sealed interface Day {
    /**
     * [weak]: a day of the month by its number alone ("12th", "the 12th"), which is as often the twelfth of something: it needs
     * "on" before it or a time beside it. [doubts]: what of the date was chosen here and not typed: a year it was put into
     * because it had passed ([Doubt.PAST]), which of its two numbers is the month ([Doubt.MONTH]).
     */
    class On(val date: LocalDate, val weak: Boolean = false, val doubts: Set<Doubt> = emptySet()) : Day
    /** A weekday. [weak]: a two-letter form, which is often an ordinary word or a name too ("do", "so", "Mo", "Di"); it needs a time beside it. */
    class Every(val day: DayOfWeek, val weak: Boolean) : Day
}

internal object Days {
    private val WORDS = mapOf("today" to 0, "heute" to 0, "tomorrow" to 1, "tmrw" to 1, "tmr" to 1, "morgen" to 1, "ubermorgen" to 2, "uebermorgen" to 2)
    private val WEAK = setOf("mo", "tu", "we", "th", "fr", "sa", "su", "di", "mi", "do", "so")
    /** A weekday's short form that is an ordinary word as often as a day ("the sun", "we sat"): alone in the middle of a sentence it is not read. */
    private val SLIGHT = setOf("mon", "tue", "tues", "wed", "weds", "thu", "thur", "thurs", "fri", "sat", "sun")
    /** Between two days: "Oct 14 to Oct 16", „14. bis 16. Oktober“. And what may stand before the first. */
    private val TO = setOf("to", "through", "thru", "until", "till", "bis", "-", "–", "—")
    private val FROM = setOf("from", "von", "vom")
    /** After a number these say it is an hour, and no day of the month: „bis 9 Uhr“, "until 9 pm". */
    private val HOURS = setOf("uhr", "am", "pm", "a.m.", "p.m.", "o'clock", "o’clock", "h")
    /** The longest a range of days is: more is two dates that happen to stand side by side. */
    private const val RANGE = 62L
    /** "The day after tomorrow", with its article and without: two days on. */
    private val AFTER = listOf(listOf("the", "day", "after", "tomorrow"), listOf("day", "after", "tomorrow"))
    /**
     * How far ahead a date without a year may be put and still be the date that was meant: "Jan 15", typed in October, is next
     * January. One that had only just passed and lands more than this many days ahead ("Oct 5" on 6 October) is as likely
     * a slip, or this year's: a guess ([Doubt.PAST]).
     */
    private const val LATE = 300L
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

    /** True if [prefix] is how a day's word begins ("tom" of tomorrow, "fre" of Freitag, "ok" of Oktober): a day that is still being typed. */
    fun begins(prefix: String): Boolean {
        val f = Matcher.fold(prefix)
        return f.isNotEmpty() && (WORDS.keys.any { it.startsWith(f) } || WEEKDAYS.keys.any { it.startsWith(f) } || MONTHS.keys.any { it.startsWith(f) })
    }

    /** True for a weekday's short form that is a word too ([SLIGHT]). */
    fun slight(word: String): Boolean = Matcher.fold(word) in SLIGHT

    /** True for a weekday's two letters that are a word or a name too ("do", "Mo"). */
    fun weak(word: String): Boolean = Matcher.fold(word) in WEAK

    /**
     * True for a word that only German says of a day or a time („morgen“, „Freitag“, „Uhr“, „Oktober“, „um“), and for a date
     * with its dots („14.10.“, „12.“). What either language may have written ("am", "Mo", "Sa", "August", "April") does not count.
     */
    fun german(word: String): Boolean = Matcher.fold(word) in GERMAN || DOTS.matches(word.trimEnd(','))

    private val GERMAN = setOf("heute", "morgen", "ubermorgen", "uebermorgen", "montag", "dienstag", "mittwoch", "donnerstag", "freitag", "samstag", "sonnabend", "sonntag", "di", "mi", "do", "so",
        "januar", "februar", "marz", "maerz", "mrz", "mai", "juni", "juli", "oktober", "okt", "dezember", "dez", "uhr", "um", "mittag", "mittags", "mitternacht", "morgens", "vormittag", "vormittags",
        "nachmittag", "nachmittags", "abend", "abends", "nachts", "fruh", "ganztagig", "ganztaegig", "ganztags", "nachsten", "nachste", "nachster", "kommenden", "diesen", "dieses", "jeden", "jede",
        "jedes", "von", "vom", "bis", "den")
    private val DOTS = Regex("[0-9]{1,2}[.]([0-9]{1,2}[.]([0-9]{2,4})?)?")

    /**
     * Words that say an event comes again and again, folded: "weekly", "every", „täglich“, "Mondays", „montags“. This
     * version writes one event and no repeat, so a line with one of them is never more than a guess at what was meant.
     */
    private val REPEATS = setOf("every", "each", "daily", "weekly", "biweekly", "bi weekly", "fortnightly", "monthly", "quarterly", "yearly", "annually", "weekdays", "weekends",
        "nightly", "recurring", "repeating",
        "mondays", "tuesdays", "wednesdays", "thursdays", "fridays", "saturdays", "sundays",
        "jeden", "jede", "jedes", "jedem", "taglich", "taeglich", "wochentlich", "woechentlich", "monatlich", "jahrlich", "jaehrlich", "werktags", "wochentags",
        "montags", "dienstags", "mittwochs", "donnerstags", "freitags", "samstags", "sonnabends", "sonntags")

    /** True for a word that says a repeat ([REPEATS]). */
    fun repeats(word: String): Boolean = Matcher.fold(word) in REPEATS

    /** True for a month's name, whole or short ("October", "Okt"). */
    fun month(word: String?): Boolean = word != null && MONTHS[Matcher.fold(word)] != null

    /**
     * [dotted]: the words follow „am“ or „den“, where a number with a dot is a day of the month („am 12.“).
     * [led]: they follow "on", „am“ or „den“: a day of the month by its number alone is then a day ("on the
     * 12th"); without, it is a weak one. "the" before a number belongs to it ("the 12th", "the 3rd of May").
     */
    fun parse(words: List<String>, today: LocalDate, dotted: Boolean = false, led: Boolean = dotted): Day? = when {
        words.isEmpty() -> null
        words.size == 1 -> one(words[0], today, dotted, led)
        words in AFTER -> Day.On(today.plusDays(2))
        // "Wednesday 28 October", „Mittwoch, 28.10.“, „Freitag, den 16.10.“: a weekday before a date that falls on it is part of
        // that date. Before any other date it is a second day, and no part of this one.
        WEEKDAYS[Matcher.fold(words[0])] != null -> {
            val weekday = WEEKDAYS[Matcher.fold(words[0])]
            val date = if (words.size > 2 && words[1] == "den") words.subList(2, words.size) else words.subList(1, words.size)
            (parse(date, today, dotted || date.size < words.size - 1, led = true) as? Day.On)?.takeIf { it.date.dayOfWeek == weekday }?.let { on ->
                // ("Thu 10/8": where the other reading of the two numbers falls on another weekday, the weekday has said which is the month.)
                val settled = Doubt.MONTH in on.doubts && date.size == 1 && turned(date[0], today)?.date?.dayOfWeek != weekday
                Day.On(on.date, doubts = if (settled) on.doubts - Doubt.MONTH else on.doubts)
            }
        }
        words[0] == "the" -> if (words[1].firstOrNull()?.isDigit() == true) parse(words.subList(1, words.size), today, led = led) else null
        words[0] == "in" -> ahead(words, today)?.let { Day.On(it) }
        else -> named(words, today)
    }

    private fun one(word: String, today: LocalDate, dotted: Boolean = false, led: Boolean = dotted): Day? {
        val f = Matcher.fold(word)
        WORDS[f]?.let { return Day.On(today.plusDays(it.toLong())) }
        WEEKDAYS[f]?.let { return Day.Every(it, f in WEAK) }
        numeric(word, today)?.let { return it }
        return ordinal(word, dotted)?.let { n -> ofMonth(n, today) }?.let { Day.On(it, weak = !led) }
    }

    /** "12th", "1st", "3rd"; with [dotted] also „12.“: the number, 1 to 31. A number alone is never a day. */
    private fun ordinal(word: String, dotted: Boolean): Int? {
        val t = word.trimEnd(',')
        val digits = t.takeWhile { it in '0'..'9' }
        val tail = t.substring(digits.length)
        if (digits.isEmpty() || digits.length > 2) return null
        return if (tail in ORDINALS || (dotted && tail == ".")) digits.toInt().takeIf { it in 1..31 } else null
    }
    private val ORDINALS = setOf("st", "nd", "rd", "th")

    /** The next time the month has a day [n] (today counts): "the 31st" in a month of thirty days is the next month's. */
    private fun ofMonth(n: Int, today: LocalDate): LocalDate? {
        for (m in 0..2L) {
            val month = YearMonth.from(today).plusMonths(m)
            if (n > month.lengthOfMonth()) continue
            val d = month.atDay(n)
            if (!d.isBefore(today)) return d
        }
        return null
    }

    /**
     * A range of days: "oct 14 to oct 16", "monday to wednesday", „14.10. bis 16.10.“, "oct 14 to 16",
     * „vom 14. bis 16. oktober“, "oct 14-16". The first and the last day; null for anything else. One
     * side must be a whole day by itself; the other may be a number that takes its month. The last day
     * is the next such day after the first. A number alone is the last day only after a first day that
     * names its month ("oct 14 to 16", „14.10. bis 16.“): after any other day it is as often an hour
     * („morgen bis 9“, "friday until 10"), and before „Uhr“, "am" or "pm" it is one.
     *
     * [before] and [after]: the words that stand next to these in the line, where there are any. A number
     * that would take the other side's month takes none where a month's name stands beside it there: "14
     * to Dec 20" in "Oct 14 to Dec 20" is no range of December's, and where the two real days are too far
     * apart to be one range, no shorter one is made of a part of them.
     */
    fun range(words: List<String>, today: LocalDate, before: String? = null, after: String? = null): Range? {
        val w = if (words.size > 1 && words[0] in FROM) words.subList(1, words.size) else words
        fun day(d: Day?, from: LocalDate): LocalDate? = when (d) {
            is Day.On -> d.date
            is Day.Every -> if (d.weak) null else from.plusDays(((d.day.value - from.dayOfWeek.value + 6) % 7 + 1).toLong())
            null -> null
        }
        fun number(word: String): Int? = dayNumber(word)
        fun doubts(d: Day?): Set<Doubt> = (d as? Day.On)?.doubts.orEmpty()
        fun checked(a: LocalDate?, b: LocalDate?, doubts: Set<Doubt>): Range? =
            if (a != null && b != null && b.isAfter(a) && !b.isAfter(a.plusDays(RANGE))) Range(a, b, doubts) else null
        // "oct 14-16", "14.-16. oktober": one word holds both numbers, the month stands beside it.
        if (w.size == 2) for ((both, month) in listOf(w[1] to w[0], w[0] to w[1])) {
            val cut = both.indexOfFirst { it in "-–—" }
            if (cut <= 0 || MONTHS[Matcher.fold(month)] == null) continue
            val a = number(both.substring(0, cut)) ?: continue
            val b = number(both.substring(cut + 1)) ?: continue
            val first = named(listOf(month, a.toString()), today) ?: continue
            return checked(first.date, runCatching { first.date.withDayOfMonth(b) }.getOrNull(), first.doubts)
        }
        for (at in 1 until w.size - 1) {
            if (w[at] !in TO) continue
            val left = w.subList(0, at)
            val right = w.subList(at + 1, w.size)
            val from = parse(left, today)
            val first = day(from, today.minusDays(1))
            if (first != null) {
                val to = parse(right, first)
                val last = day(to, first) ?: right.singleOrNull()?.takeIf { !month(after) && after !in HOURS && left.any { month(it) || numeric(it, today) != null } }
                    ?.let(::number)?.let { n -> runCatching { first.withDayOfMonth(n) }.getOrNull() }
                // (A weekday that is today's begins the range today, where the weekday alone is the next one: which week was
                // not said. The last day is reckoned from the first: whether its own year had passed says nothing.)
                val week = if (from is Day.Every && first == today) setOf(Doubt.WEEK) else emptySet()
                checked(first, last, doubts(from) + week + (doubts(to) - Doubt.PAST))?.let { return it }
            } else if (left.size == 1 && !(w === words && month(before))) {
                // „14. bis 16. Oktober“, "14 to 16 oct": the first day takes the month of the last.
                val n = number(left[0]) ?: continue
                val last = parse(right, today) as? Day.On ?: continue
                checked(runCatching { last.date.withDayOfMonth(n) }.getOrNull(), last.date, last.doubts)?.let { return it }
            }
        }
        return null
    }

    /** A range of days from [first] to [last], and what of it was chosen here and not typed. */
    class Range(val first: LocalDate, val last: LocalDate, val doubts: Set<Doubt>)

    /**
     * 3.10. and 3.10.2026 (day first), 10/3 and 10/3/2026 (month first), 2026-10-03. With a slash, where the second number
     * could be the month as well ("12/10"), month first is this parser's choice: [Doubt.MONTH].
     */
    private fun numeric(word: String, today: LocalDate): Day.On? {
        val p = word.split('.', '/', '-')
        return when {
            p.size < 2 || p.size > 4 -> null
            '.' in word && '/' !in word && '-' !in word -> when {
                p.size == 3 && p[2].isEmpty() -> date(null, p[1], p[0], today)              // "3.10" could be a version: the dot is needed
                p.size == 3 || (p.size == 4 && p[3].isEmpty()) -> date(year(p[2]), p[1], p[0], today, yearGiven = true)
                else -> null
            }
            '/' in word && '.' !in word && '-' !in word -> when (p.size) {
                2 -> date(null, p[0], p[1], today, either = true)
                3 -> date(year(p[2]), p[0], p[1], today, yearGiven = true, either = true)
                else -> null
            }
            '-' in word && '.' !in word && '/' !in word ->
                if (p.size == 3 && p[0].length == 4) date(year(p[0]), p[1], p[2], today, yearGiven = true) else null
            else -> null
        }
    }

    /** [word], two numbers and a slash or those and a year, read the other way round: the day first ("10/8" as 10 August). */
    private fun turned(word: String, today: LocalDate): Day.On? {
        val p = word.split('/')
        return when (p.size) {
            2 -> date(null, p[1], p[0], today)
            3 -> date(year(p[2]), p[1], p[0], today, yearGiven = true)
            else -> null
        }
    }

    /** "oct 3", "3 oct", "3. Okt", "October 3rd 2027". */
    private fun named(all: List<String>, today: LocalDate): Day.On? {
        // "3rd of October": the "of" is nothing.
        val words = if (all.size in 3..4 && all[1] == "of") all.filterIndexed { i, _ -> i != 1 } else all
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

    /** [either]: the two numbers stand with a slash between them, where either may be the month if it is twelve or less. */
    private fun date(year: Int?, month: String, day: String, today: LocalDate, yearGiven: Boolean = false, either: Boolean = false): Day.On? {
        if (yearGiven && year == null) return null
        val m = digits(month, 2) ?: return null
        val d = digits(day, 2) ?: return null
        val on = fixed(year, m, d, today) ?: return null
        return if (either && m <= 12 && d <= 12 && m != d) Day.On(on.date, doubts = on.doubts + Doubt.MONTH) else on
    }

    /**
     * The date itself when the year is given; otherwise the next time that day of that month comes (today counts). That
     * next time is a guess where the date had passed and is now most of a year away ([LATE], [Doubt.PAST]).
     */
    private fun fixed(year: Int?, month: Int, day: Int, today: LocalDate): Day.On? {
        if (month !in 1..12 || day < 1) return null
        if (year != null) return if (day <= YearMonth.of(year, month).lengthOfMonth()) Day.On(LocalDate.of(year, month, day)) else null
        for (y in today.year..today.year + 8) {                                       // 29.2. waits for a leap year
            if (day > YearMonth.of(y, month).lengthOfMonth()) continue
            val d = LocalDate.of(y, month, day)
            if (!d.isBefore(today)) return Day.On(d, doubts = if (d.isAfter(today.plusDays(LATE))) setOf(Doubt.PAST) else emptySet())
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

/**
 * A time of day and, for "9-10", where it ends: minutes from that day's midnight (past 1440 is the next day).
 * [vague]: the time does not say which half of the day it is in ("7", "7:30"): that was chosen here. [doubts]: what
 * else of it is a guess: midnight or an hour of the night, which beside a day does not say which night
 * ([Doubt.NIGHT]); a span that ends where it starts ([Doubt.END]).
 */
internal class Clock(val time: LocalTime, val end: Int?, val vague: Boolean = false, val doubts: Set<Doubt> = emptySet())

internal object Times {
    private val MARKS = setOf("am", "pm", "a.m.", "p.m.", "uhr")
    private const val DASHES = "-–—"
    /** Between two times, as a word: "9am to 10am", „9 bis 10 Uhr“. And what may stand before the first: "from 9 to 10". */
    private val TO = setOf("to", "until", "till", "bis")
    private val FROM = setOf("from", "von")
    private val NAMED = listOf("midnight" to 0, "mitternacht" to 0, "mittags" to 12, "mittag" to 12, "noon" to 12)

    /**
     * A part of the day that words after a time can say ("7 in the evening", „7 Uhr abends“, "12 at night"): the
     * words, folded, and which hour of the day each hour of the twelve-hour clock is in it. Null: that hour is not
     * in this part of the day ("11 in the afternoon"), and the words are then not taken for the time's.
     */
    private enum class Half(val phrases: List<List<String>>, val of: (Int) -> Int?) {
        MORNING(listOf(listOf("in", "the", "morning"), listOf("am", "vormittag"), listOf("morgens"), listOf("vormittags"), listOf("fruh")), { if (it == 12) 0 else it }),
        MIDDAY(listOf(listOf("mittags"), listOf("noon")), { when (it) { 11, 12 -> it; 1, 2 -> it + 12; else -> null } }),
        AFTERNOON(listOf(listOf("in", "the", "afternoon"), listOf("am", "nachmittag"), listOf("nachmittags")), { when (it) { 12 -> 12; in 1..8 -> it + 12; else -> null } }),
        EVENING(listOf(listOf("in", "the", "evening"), listOf("am", "abend"), listOf("abends")), { if (it in 4..11) it + 12 else null }),
        // Twelve at night is midnight, one to five are the small hours, six to eleven are the evening's.
        NIGHT(listOf(listOf("at", "night"), listOf("nachts")), { when (it) { 12 -> 0; in 1..5 -> it; else -> it + 12 } });

        /** The hours of the day that are in this part of it: an hour said outright (19, 7pm) must be one of them. */
        val hours: Set<Int> = (1..12).mapNotNull(of).toSet()
    }

    /** [named]: noon or midnight by its name. [zero]: its hour is written with a zero before it ("09:30"), as only the 24-hour clock writes one. */
    private class Part(val hour: Int, val minute: Int, val mark: Char, val exact: Boolean, val named: Boolean = false, val zero: Boolean = false)

    /**
     * [p] leaves its half of the day unsaid: an hour from one to twelve with neither am nor pm ("7", "7:30", "12:00"). Past
     * twelve there is only one; and "09:30" and „9 Uhr“ are the 24-hour clock's way to write the morning, and say it. Not so
     * „1 Uhr“ to „8 Uhr“ with no zero before them ([early]): people say „um 7 Uhr“ for the evening too.
     */
    private fun unsaid(p: Part): Boolean = !p.zero && ((p.mark == ' ' && p.hour in 1..12) || (p.mark == 'u' && early(p)))

    /** An hour from one to eight with no zero before it: with „Uhr“ it is written for the morning and said for the evening. */
    private fun early(p: Part): Boolean = !p.zero && p.hour in 1..8

    /** [p] says its half of the day whatever one makes of „Uhr“: am or pm, noon or midnight by name, a zero before the hour, an hour past twelve. */
    private fun plain(p: Part): Boolean = p.mark == 'a' || p.mark == 'p' || p.named || p.zero || p.hour == 0 || p.hour > 12

    /** [p] is midnight by its name, by its number ("0:00", „0 Uhr“) or as "12am" ([mark]: its own, or the one it takes from its span's end): beside a day it does not say which night. */
    private fun midnight(p: Part, mark: Char = p.mark): Set<Doubt> = if ((p.hour == 0 && p.minute == 0) || (mark == 'a' && p.hour == 12)) setOf(Doubt.NIGHT) else emptySet()

    /**
     * The time that [words] are: 3pm, "3 pm", 3:30pm, 15:00, "15 Uhr", noon; and spans, 9-9:30, 9am-10am,
     * "9 – 10". A number alone ("7") counts only if [bare]. With [afternoon], an hour from 1 to 7 that
     * has no am, pm or Uhr, and no zero before it, is 13 to 19 ("at 3", "at 3:30"; "at 07:30" is the morning).
     */
    fun parse(words: List<String>, bare: Boolean, afternoon: Boolean = false, span: Boolean = true): Clock? {
        // "from 9 to 10", „von 9 bis 10“: the words say it is a span, so a number alone is an hour. Without its end it is nothing.
        if (words.size > 1 && words[0] in FROM) return if (span) parse(words.subList(1, words.size), bare = true, afternoon)?.takeIf { it.end != null } else null
        // "6:45 in the morning", „7 Uhr abends“: the words say the half of the day, whatever form the hour has. (Where what
        // stands before them is no time, they may be a time themselves, and the end of a span: "9 to noon", „9 bis mittags“.)
        half(words)?.let { (time, half) -> said(time, half, span)?.let { return it } }
        val joined = join(words) ?: return null
        val s = joined.text
        val (a, next) = part(s, 0) ?: return null
        if (a.minute > 59) return null
        if (next == s.length) {
            var hour = hour24(a, a.mark) ?: return null
            if (a.mark == ' ' && !a.exact && !bare) return null
            if (a.mark == ' ' && !a.zero && afternoon && hour in 1..7) hour += 12
            return Clock(LocalTime.of(hour, a.minute), null, vague = unsaid(a), doubts = midnight(a))
        }
        if (!span || s[next] !in DASHES) return null
        val (b, done) = part(s, next + 1) ?: return null
        if (done != s.length || b.minute > 59) return null
        // Two numbers with a word between them are a span only where something says they are times: "2 to 3 dentists" is not one.
        if (joined.worded && !bare && a.mark == ' ' && b.mark == ' ' && !a.exact && !b.exact) return null
        // "9-10am": the start borrows the end's half of the day, unless that puts it after the end ("11-1pm").
        val borrowed = a.mark == ' ' && (b.mark == 'a' || b.mark == 'p')
        // „10 bis 11 Uhr“: the end's „Uhr“ is the start's too, and both are on the 24-hour clock.
        val clocked = a.mark == ' ' && b.mark == 'u' && !b.named
        var start = (hour24(a, if (borrowed) b.mark else a.mark) ?: return null) * 60 + a.minute
        var end = (hour24(b, b.mark) ?: return null) * 60 + b.minute
        if (borrowed && start >= end) ((start + 720) % 1440).let { if (it < end) start = it }
        // A span that starts at 1 to 7 with no am, pm or Uhr is a meeting after lunch, not at night: "2-3" is 14:00 to 15:00.
        if (a.mark == ' ' && !borrowed && !clocked && !a.zero && start in 60 until 480) start += 720
        // Which half of the day the start is in was said: by itself, by the end's am or pm, by the end's „Uhr“ where the start
        // is no hour from one to eight („2-3 Uhr“ is as often the afternoon), or by an end after it that can only be one hour
        // of the day and leaves the start only the morning ("8-13", „8 bis 13 Uhr“). An end follows its start, and is no guess
        // of its own.
        val startSaid = borrowed || (if (clocked) !early(a) else !unsaid(a)) || (plain(b) && end > start && start < 720 && start + 720 >= end)
        // ("9am-9am": an end that is its start would be a whole day on, which nobody means by it.)
        val same = if (end == start) setOf(Doubt.END) else emptySet()
        val on = if (end > start) 0 else if (b.mark == ' ' && end < 720 && end + 720 > start) 720 else 1440
        end += on
        // ("10pm-9pm": an end on the next day, more than twelve hours on, is as likely a slip.)
        val long = if (on == 1440 && end - start > 720 && same.isEmpty()) setOf(Doubt.LONG) else emptySet()
        return Clock(LocalTime.of(start / 60, start % 60), end, vague = !startSaid, doubts = same + long + midnight(a, if (borrowed) b.mark else a.mark))
    }

    /**
     * [time] with the [half] of the day that words said after it: an hour up to twelve is read on the twelve-hour clock
     * („7 Uhr abends“ is 19:00, "12 at night" 00:00), and one that says its own half (19, 7pm, noon) must be in that
     * part of the day. Null where the two contradict each other ("11 in the afternoon", „15 Uhr morgens“): the words are
     * then not this time's. A span is in that part of the day at both its ends, and only the night's goes over midnight.
     */
    private fun said(time: List<String>, half: Half, span: Boolean): Clock? {
        val s = join(time)?.text ?: return null
        fun minutes(p: Part): Int? {
            if (p.minute > 59) return null
            val hour = when {
                p.mark == 'a' || p.mark == 'p' -> hour24(p, p.mark)?.takeIf { it in half.hours }
                p.named || p.hour == 0 || p.hour > 12 -> p.hour.takeIf { it in half.hours }
                // (Twelve in the morning is midnight; „12 Uhr vormittags“ is as likely noon, and is not read.)
                else -> half.of(p.hour).takeIf { !(half == Half.MORNING && p.hour == 12 && p.mark == 'u') }
            } ?: return null
            return hour * 60 + p.minute
        }
        val (a, next) = part(s, 0) ?: return null
        val start = minutes(a) ?: return null
        // (Twelve at night, twelve in the morning and the night's small hours: beside a day, which night was not said.)
        val night = if (start < 60 || (half == Half.NIGHT && start < 360)) setOf(Doubt.NIGHT) else emptySet()
        if (next == s.length) return Clock(LocalTime.of(start / 60, start % 60), null, doubts = night)
        if (!span || s[next] !in DASHES) return null
        val (b, done) = part(s, next + 1) ?: return null
        if (done != s.length) return null
        var end = minutes(b) ?: return null
        if (end <= start) { if (half != Half.NIGHT) return null; end += 1440 }
        return Clock(LocalTime.of(start / 60, start % 60), end, doubts = night)
    }

    /** True where [words] hold words for a half of the day ("in the evening", „abends“) anywhere: left in a title, they say a time nobody read. */
    fun halved(words: List<String>): Boolean {
        val f = words.map { Matcher.fold(it) }
        return Half.entries.any { h -> h.phrases.any { p -> p != listOf("noon") && p != listOf("mittags") && (0..f.size - p.size).any { i -> p.indices.all { f[i + it] == p[it] } } } }
    }

    /** The words as one text; [worded]: its two ends were joined by a word ("to"), not by a dash. */
    private class Joined(val text: String, val worded: Boolean)

    /**
     * "3" "pm" → 3pm, "9" "-" "10" → 9-10, "9" "to" "10" → 9-10; and „8“ „Uhr“ „30“ → 8:30uhr: two digits below sixty directly
     * after „Uhr“ are that hour's minutes. Any other second word means these words are not one time.
     */
    private fun join(words: List<String>): Joined? {
        if (words.isEmpty()) return null
        val b = StringBuilder(words[0])
        var worded = false
        var i = 1
        while (i < words.size) {
            val w = words[i]
            when {
                w in MARKS -> b.append(w)
                minutes(w, b) -> { b.setLength(b.length - UHR.length); b.append(':').append(w).append(UHR) }
                w.length == 1 && w[0] in DASHES && i + 1 < words.size -> { b.append('-').append(words[i + 1]); i++ }
                w in TO && i + 1 < words.size -> { b.append('-').append(words[i + 1]); i++; worded = true }
                else -> return null
            }
            i++
        }
        return Joined(b.toString(), worded)
    }

    /** [w] is the minutes of the hour that [text] ends with: two digits below sixty, after an hour that has „Uhr“ and no minutes yet. */
    private fun minutes(w: String, text: CharSequence): Boolean {
        if (w.length != 2 || w.any { it !in '0'..'9' } || w.toInt() > 59 || !text.endsWith(UHR)) return false
        val hour = text.substring(0, text.length - UHR.length).substringAfterLast('-')
        return hour.isNotEmpty() && hour.all { it in '0'..'9' }
    }

    private const val UHR = "uhr"

    /** [words] without the words at their end that say the half of the day, and that half. Null where no such words end them. */
    private fun half(words: List<String>): Pair<List<String>, Half>? {
        for (h in Half.entries) for (p in h.phrases) {
            if (words.size <= p.size) continue
            val from = words.size - p.size
            if (p.indices.all { Matcher.fold(words[from + it]) == p[it] }) return words.subList(0, from) to h
        }
        return null
    }

    /** One time starting at [from], and where it stops. Marks: a, p, u (24 hours, said outright), blank. */
    private fun part(s: String, from: Int): Pair<Part, Int>? {
        for ((word, hour) in NAMED) if (s.startsWith(word, from)) return Part(hour, 0, 'u', true, named = true) to from + word.length
        var i = from
        while (i < s.length && i - from < 2 && s[i] in '0'..'9') i++
        if (i == from) return null
        val zero = i - from == 2 && s[from] == '0'
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
        return Part(hour, minute, mark, exact, zero = zero) to i
    }

    private fun hour24(p: Part, mark: Char): Int? = when (mark) {
        'a' -> if (p.hour in 1..12) p.hour % 12 else null
        'p' -> if (p.hour in 1..12) p.hour % 12 + 12 else null
        else -> if (p.hour in 0..23) p.hour else null
    }
}
