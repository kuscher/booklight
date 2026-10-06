package io.github.kuscher.booklight.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToLong

/**
 * A day, a time or both, read off a typed line. [end] only when an end was typed: an end time ("9-9:30"),
 * or the last day of a range of days ("Oct 14 to Oct 16"), for which it is the midnight after that day.
 * [allDay] when no time was given: [start] is then that day's midnight. [dayGiven] and [timeGiven] say
 * what was typed and what was guessed: "all day" said outright, and a range of days, count as the time
 * given. [rest] is the line without the expression. [repeats]: the line said "every Monday": the day
 * that was read is the next such day, and no more than a guess at what was meant; an event's line
 * ([When.spot]) also where a word in it says a repeat ("weekly", „täglich“). [vague]: a time was typed
 * that does not say which half of the day it is in ("at 7", "7:30", „um 7“, "9-10"): the half was
 * chosen here, and is a guess. "7pm", "19:30", „19 Uhr“, „9 Uhr“, "7 in the evening" and "noon" say it;
 * „7 Uhr“ does not. Also for a day of the month by its number alone beside a time ("12th 9am", with no
 * "on" before it).
 * [doubts]: what else of the reading was chosen here and not typed ([Doubt]); empty where nothing was.
 */
data class Moment(val start: LocalDateTime, val end: LocalDateTime?, val allDay: Boolean,
    val dayGiven: Boolean, val timeGiven: Boolean, val rest: String, val repeats: Boolean = false, val vague: Boolean = false,
    val doubts: Set<Doubt> = emptySet())

/**
 * Why a day or a time that was read is a guess all the same: something about it was chosen by the parser and
 * not typed. An event with one of these is never saved without the calendar's editor ([EventDraft.sure]).
 */
enum class Doubt {
    /** The date has no year and had passed: it was put a year on, more than three hundred days ahead ("Oct 5" typed on 6 October). */
    PAST,
    /** "next Tuesday", „nächsten Dienstag“: people mean two different days by it. */
    NEXT,
    /** A weekday by two letters that are a word or a name too ("do", "Mo", "so"). */
    WEAK,
    /** Two numbers and a slash, each of which could be the month ("12/10"): which is the month was not said. */
    MONTH,
    /** Midnight, or an hour of the night, beside a day ("Friday midnight", "tomorrow 12 at night"): which night was not said. */
    NIGHT,
    /**
     * A word stands directly beside the day that changes it ("the day before Friday", "not tomorrow", "Friday after next"), or a
     * second, short weekday does ("Sat tomorrow"); or one stands directly before the time that changes it ("till 5pm", "quarter
     * to 8pm", „halb 9 Uhr“), or directly after it ("3pm PT", "3pm London time"; a word after the minutes of „15 Uhr 30“).
     */
    BESIDE,
    /**
     * A number standing alone, or a word of time that nobody read, is left in the title ("Zahnarzt 30", "2 hour workshop",
     * "dinner next week", "7pm EST"): something of the day, the time or the length was typed and not read.
     */
    LEFT,
    /** A span that ends where it starts ("9am-9am"). */
    END,
    /** A span that ends on the next day, more than twelve hours after it starts ("10pm-9pm"): as likely a slip as a day and a night. */
    LONG,
    /** A weekday that is today's, by its name ("Tuesday 7pm" or "Monday to Wednesday", typed on that day): this week's or the next was not said. */
    WEEK,
}

/**
 * A day or a time found anywhere in a line ([When.spot]): what it says ([moment], whose rest is the
 * line without the expression), the line's [pieces] around the expression in their order and as they
 * were typed (none of them empty), and the expression itself as typed ([said]; empty for a line so
 * long that only its ends were read). [every]: the expression itself says a repeat ("every Monday"),
 * whatever the line's other words say.
 */
class Spot(val moment: Moment, val pieces: List<Piece>, val said: String, val every: Boolean = false)

/** A piece of a line, as typed, and where it starts in the line. */
class Piece(val text: String, val start: Int)

/**
 * Reads when something is, in English and German: "Fri 3pm Dentist", "Standup tomorrow 9-9:30",
 * "Zahnarzt am Freitag um 15 Uhr", "in 20m stretch".
 *
 * The rules, kept few so that the preview never surprises:
 * - The expression is the first or the last words of the line, never the middle. The longest reading
 *   wins; if both ends read equally long, the start. A day and a time may stand in either order.
 *   (An event's line is read in its middle too: [spot].)
 * - Days: today, tomorrow, heute, morgen, übermorgen; weekday names and their short forms (mon, mo,
 *   Montag): the next such day, or today when a time is given that is still ahead (typed on that very
 *   weekday it is a guess which of the two was meant: [Doubt.WEEK]). Dates: 3.10.
 *   3.10.2026 10/3 2026-10-03 "oct 3" "3 oct" "3. Okt"; without a year, the next time that date comes.
 *   "in 3 days", "in 2 Wochen". "do", "so" and "we" are ordinary words too: days only beside a time.
 * - Times: 3pm 3:30pm 15:00 "15 Uhr" noon midnight Mittag. With a colon and no am or pm, the 24-hour
 *   clock. „15 Uhr 30“: two digits below sixty directly after „Uhr“ are the minutes. A number alone is
 *   not a time ("3 Dentists"), except in a span and after "at" or "um".
 *   After those two, an hour from 1 to 7 without am, pm or Uhr is the afternoon: "at 3" is 15:00,
 *   "at 3:30" 15:30, "at 8" 08:00.
 * - Spans: 9-9:30, 9am-10am, 9–10, "9 to noon". An end that isn't after the start moves 12 hours on
 *   when that helps ("10-2"), else to the next day; more than twelve hours on, that is a guess
 *   ([Doubt.LONG]). A span that starts at 1 to 7 with no am or pm is the afternoon ("2-3" is 14:00 to
 *   15:00).
 * - A day at one end and a time at the other also work: "Friday Dentist 3pm".
 * - "this", "coming", "kommenden" in front of a day are read with it and change nothing. "next Tuesday",
 *   „nächsten Dienstag“ is never today: typed on a Tuesday it is a week on, on any other day the coming one.
 *   People mean two different days by it, so it is a guess wherever it is read ([Doubt.NEXT]).
 * - "the day after tomorrow" is two days on. A weekday before a date that falls on it ("Wednesday 28 October",
 *   „Freitag, den 16.10.“) is part of that date, and says which number is the month where only one reading of
 *   "10/8" falls on it.
 * - "tmrw" is tomorrow. "on the 12th", "on 12th", „am 12.“: the next day of that number. Without
 *   the "on" ("12th", "the 12th") it is a day only beside a time, and a guess ([Moment.vague]): it is
 *   as often the twelfth of something.
 * - "6:45 in the morning", "7 in the evening", „7 Uhr abends“, "12 at night": the words say which half
 *   of the day, for every form of the hour. Twelve at night is midnight, one to five at night are the
 *   small hours. Words that contradict their hour ("11 in the afternoon") are not the time's.
 * - A time that says its half of the day in none of these ways (an hour up to twelve without am or pm:
 *   "at 7", "7:30", "12:00") is read as above, and marked as a guess ([Moment.vague]). An hour with a zero
 *   before it ("09:30", „08 Uhr“) and „9 Uhr“ to „12 Uhr“ are the 24-hour clock's, and say it. „1 Uhr“ to
 *   „8 Uhr“ are read by that clock too, and are a guess: people say „um 7 Uhr“ for the evening. So is a
 *   span that starts at such an hour, unless its end is past twelve („8 bis 13 Uhr“).
 * - What else was chosen here and not typed is said in [Moment.doubts]: a date without a year that had passed
 *   and was put most of a year ahead, "12/10", a weekday by two letters, midnight beside a day (by its name or
 *   as "0:00"), a word beside the day that changes it ("the day before Friday", "not tomorrow", "the following
 *   Friday", "first Monday in November", "Friday next 7pm"), a word before the time that does ("till 5pm",
 *   "quarter to 8pm", „halb 9 Uhr“) or after it ("3pm PT", "3pm London time", „15 Uhr 30 Leute“).
 * - "from 9 to 10", „von 9 bis 10“, "9am to 10am": a span in words. Two bare numbers need the "from".
 * - "Oct 14 to Oct 16", "Monday to Wednesday", „vom 14. bis 16. Oktober“, "Oct 14-16": a range of
 *   days, all day, from the first to the last. Two days more than two months apart are no range, and
 *   no shorter one is made of a part of them. A number alone is the last day only after a day that
 *   names its month: „morgen bis 9 Uhr“ and "Friday until 10" are no three days. And a number directly
 *   after a month's name is that month's day, no span's first hour ("Oct 14 until 18 Uhr", "Oct 14 to
 *   noon"; with a dash between the two, [parse] still reads the span 3.1 read).
 * - "all day", „ganztägig“ beside a day, or with the day at the line's other end: all day, said
 *   outright. With no day they are neither a day nor a time ("I worked all day").
 * - "every Monday", „jeden Montag“: read as the next such day, and marked as a guess ([Moment.repeats]).
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
    private val LEADING = setOf("at", "on", "um", "am", "next", "this", "coming", "nächsten", "nächste", "nächster", "kommenden", "diesen", "dieses",
        "every", "each", "jeden", "jede", "jedes", "den")
    private val HOUR = setOf("at", "um")
    /** "next Tuesday": never today, and a guess ([Doubt.NEXT]). */
    private val NEXT = setOf("next", "nächsten", "nächste", "nächster")
    /**
     * A word that, directly beside a day, makes another day of it or says the day is not it: "a week from tomorrow", "until
     * Friday", „ab Montag“, „nicht Freitag“; and a week's own words there, which name another week's day: "next week Friday",
     * "Friday next week", "last Friday", „nächste Woche Freitag“. Folded.
     */
    private val SHIFTS = setOf("after", "before", "from", "not", "instead", "until", "till", "since", "except", "nach", "vor", "ab", "außer", "ausser", "bis", "nicht", "statt",
        "week", "weeks", "next", "last", "previous", "woche", "wochen", "nachste", "nachsten", "nachster", "ubernachste", "ubernachsten", "letzte", "letzten", "letzter", "vorige", "vorigen")
    /**
     * A word that makes another day of the day it stands directly before, and is a word of the title anywhere else ("first aid
     * course Saturday"): "the following Friday", "the other Friday", "first Monday in November", „ersten Freitag im Monat“. Folded.
     */
    private val WHICH = setOf("following", "other", "first", "second", "third", "fourth", "fifth", "erste", "ersten", "erster", "zweite", "zweiten", "zweiter",
        "dritte", "dritten", "dritter", "vierte", "vierten", "vierter", "funfte", "funften", "funfter")
    /**
     * A word that, directly before a time, says the event does not begin then, or makes another time of it: "till 5pm", „bis
     * 17 Uhr“, "before 5pm", "quarter to 8pm", "half past 7pm", „halb 9 Uhr“. Folded; and the dashes, which fold to nothing.
     */
    private val LIMITS = setOf("until", "till", "to", "through", "thru", "bis", "before", "after", "vor", "nach", "not", "nicht",
        "half", "quarter", "past", "halb", "viertel", "dreiviertel")
    private const val DASHES = "-–—"
    /** A zone by two letters that are a word or a name too ("PT session", "Mt Fuji"): one only directly after a time. Folded. */
    private val NEAR = setOf("et", "ct", "mt", "pt")
    /** "3pm London time", „15 Uhr Ortszeit“: after a time, the word says whose time it is. Folded. */
    private val ELSEWHERE = setOf("time", "zeit", "ortszeit")
    private val MINUTES = Regex("[0-5][0-9]")
    private const val UHR = "uhr"
    /** Between a first hour and a last, where they stand as words of their own: "14 until 18", and with the dashes "14 - 18". */
    private val TOWARD = setOf("to", "until", "till", "through", "thru", "bis")
    private val UNTIL = TOWARD + setOf("-", "–", "—")
    /** After these a number with a dot is a day of the month: „am 12.“. */
    private val DOTTED = setOf("am", "den")
    /** "every Monday": this version reads the next one and says it is a guess. */
    private val EVERY = setOf("every", "each", "jeden", "jede", "jedes")
    /** "All day", said outright, folded. */
    private val WHOLE = setOf("all day", "allday", "ganztagig", "ganztaegig", "ganztags", "den ganzen tag")
    /** The most words of a line whose middle is read ([spot]): a longer one is read at its ends alone. */
    private const val WORDS = 60
    /** Two numbers and a dash, with nothing that says they are times: "2-3". */
    private val BARE = Regex("[0-9]{1,2}[-–—][0-9]{1,2}")
    /** Two numbers and a slash: a date at a line's end or after "on" ("on 3/4"), and as often a half or a score ("1/2 cup"). */
    private val FRACTION = Regex("[0-9]{1,2}/[0-9]{1,2}")
    /** A number alone: after "at" it is an hour, or the number of a house ("at 5 Main Street"). */
    private val NUMBER = Regex("[0-9]{1,2}")

    /** What [text] says about when, or null when neither end of it is a day or a time. */
    fun parse(text: String, now: LocalDateTime): Moment? = find(text, now)?.let { moment(it, it.rest) }

    private fun moment(hit: Hit, rest: String, repeats: Boolean = false) = Moment(hit.start, hit.end, !hit.time, hit.day, hit.time || hit.whole, rest, hit.repeats || repeats, hit.vague,
        // (Which night a midnight is in is a question only beside a day: without one the day itself is the guess.)
        if (hit.day) hit.doubts else hit.doubts - Doubt.NIGHT)

    /** [alone]: "all day" by itself is read (as a half that still needs its day); without, the line is read as if those words meant nothing. */
    internal fun find(text: String, now: LocalDateTime, alone: Boolean = true): Hit? {
        val one = findOne(text, now, alone) ?: return null
        if (one.seconds != null || (one.day && one.timed)) return one
        // Only a day, or only a time: the other half may stand at the other end of what is left.
        val two = findOne(one.rest, now, alone)?.takeIf { it.seconds == null && it.day != one.day && it.timed != one.timed }
        // "All day" with no day beside it or at the line's other end is neither a day nor a time ("I worked all day"):
        // the line is read again without it.
        if (two == null) return if (one.day || one.time) one else find(text, now, alone = false)
        return both(one, two).also { it.rest = two.rest }
    }

    /** A day from one of the two and a time (or "all day") from the other, as one. */
    private fun both(one: Hit, two: Hit): Hit {
        val day = if (one.day) one else two
        val time = if (one.day) two else one
        if (time.whole) return Hit(day.start, day.end, day = true, time = false, whole = true, repeats = day.repeats, doubts = day.doubts)
        val start = day.start.toLocalDate().atTime(time.start.toLocalTime())
        val end = time.end?.let { start.plusSeconds(java.time.Duration.between(time.start, it).seconds) }
        return Hit(start, end, day = true, time = true, repeats = day.repeats, vague = time.vague, doubts = day.doubts + time.doubts)
    }

    /**
     * As [Moment], plus [seconds] when the expression was "in 20m": reminders make a timer of those.
     * [whole]: "all day" was said, or the expression is a range of days: no time is missing. [repeats]: "every Monday".
     * [vague]: its time does not say its half of the day. [doubts]: what else of it was chosen here ([Doubt]).
     * [opens], [closes]: the day's own words begin the expression, or end it: both where it is a day alone
     * ("Friday", a range of days), neither where it is a time alone; "Friday 3pm" opens with its day, "3pm on Friday"
     * closes with it.
     */
    internal class Hit(val start: LocalDateTime, val end: LocalDateTime?, val day: Boolean, val time: Boolean, val seconds: Long? = null,
        val whole: Boolean = false, val repeats: Boolean = false, val vague: Boolean = false, val doubts: Set<Doubt> = emptySet(),
        val opens: Boolean = day, val closes: Boolean = day) {
        var rest = ""
        /** Nothing about the time of day is left to say. */
        val timed: Boolean get() = time || whole
        /** This, and a guess for [doubt] too. */
        fun with(doubt: Doubt): Hit = Hit(start, end, day, time, seconds, whole, repeats, vague, doubts + doubt, opens, closes).also { it.rest = rest }
    }

    /**
     * A day or a time anywhere in [text], for a line that is an event: the longest run of words that is
     * one expression; where two are as long, the one at the line's start, then the one at its end, then
     * the leftmost. Where that is only a day or only a time, the other half is looked for in what is
     * left, wherever it stands. So whatever [parse] reads is read the same, and "Add dinner tomorrow at
     * 7pm to the Team calendar" is read too.
     *
     * Some things are not read by themselves in the middle of a line, where they are far more often
     * something else: a weekday's short form that is a word as well ("we sat"), two bare numbers with a
     * dash ("2-3 eggs"), two with a slash ("1/2 cup"; "on 3/4" is a date), and a number alone after "at"
     * or „um“ ("at 5 Main Street"): that one is an hour beside its day, in one expression ("tomorrow at
     * 5", "at 5 on Friday"), or at an end of the line. Nor is a weekday by two letters with its time
     * ("to do 3pm"): at an end of the line it is the day it has always been, and a guess. Nor are two bare
     * numbers ever the time of a day that stands elsewhere in the line ("1-2 eggs tomorrow"): the day is
     * read, and they stay. Except with [bare], for a line under the keyword `event`, which has always
     * read them at one end of the line as the time of a day at the other ("9-10 standup tomorrow"): a
     * guess at the half of the day, as ever.
     * [led]: words were taken off before [text] (a sentence's cue: "Add …"), so its first word stands in
     * the middle of the line. [own]: where in [text] the line's own words end, where words follow them that
     * are not the line going on (a calendar's phrase, "… at 7 to Team"): what is read only at an end of the
     * line is read where its own words end, too.
     *
     * A word that says a repeat anywhere in the line ("weekly", „täglich“, "every other week") marks
     * the reading as one ([Moment.repeats]). A word directly beside the day's own words that changes the
     * day, or says it is not the day ("a week from tomorrow", "not tomorrow", "Friday after next", "the
     * following Friday"), a second, short weekday beside the expression ("Sat tomorrow"), and a word
     * directly before the time's own words that changes the time ("till 5pm") make the reading a guess
     * ([Doubt.BESIDE]). A full stop, "!" or "?" that ends the line is not part of its last word. A line of
     * more than [WORDS] words is read at its ends only.
     */
    fun spot(text: String, now: LocalDateTime, led: Boolean = false, bare: Boolean = false, own: Int = Int.MAX_VALUE): Spot? = spot(text, now, led, bare, alone = true, own)

    private fun spot(text: String, now: LocalDateTime, led: Boolean, ends: Boolean, alone: Boolean, own: Int): Spot? {
        val words = Words.of(text)
        val n = words.size
        // (How many of the words are the line's own.)
        val mine = words.count { it.start < own }
        // (A full stop, "!" or "?" that ends the line is not part of its last word: "… at 7pm." is seven in the evening.)
        val low = words.mapIndexed { i, w -> w.text.lowercase().trimEnd(',', ';').let { if (i == n - 1) ended(it) else it } }
        val again = low.any { Days.repeats(it) }
        if (n > WORDS) return find(text, now)?.let { h -> Spot(moment(h, h.rest, again), listOfNotNull(h.rest.takeIf { it.isNotEmpty() }?.let { Piece(it, text.indexOf(it).coerceAtLeast(0)) }), "", h.repeats) }
        class Run(val from: Int, val to: Int, val hit: Hit)
        /** The best run of the words from [a] up to [b] that is one expression and passes [fits]. [bare]: two bare numbers alone may be it. */
        fun best(a: Int, b: Int, bare: Boolean, fits: (Hit) -> Boolean): Run? {
            for (k in minOf(b - a, SPAN) downTo 1) {
                // (The start, the end, then the middle from the left. Where the line's own words end before the end, there first.)
                val end = if (mine in a + k until b) sequenceOf(mine - k, b - k) else sequenceOf(b - k)
                for (i in (sequenceOf(a) + end + (a + 1 until b - k)).distinct()) {
                    // (In the middle of the line: not at its start, and neither at its end nor where its own words end.)
                    val inside = (i > 0 || led) && i + k < (if (i + k <= mine) mine else n)
                    if (k == 1 && ((inside && (Days.slight(low[i]) || FRACTION.matches(low[i]))) || ((inside || !bare) && BARE.matches(low[i])))) continue
                    if (k == 2 && inside && low[i] in HOUR && NUMBER.matches(low[i + 1])) continue
                    if (dated(low, i, k)) continue
                    val h = read(low.subList(i, i + k), now, alone, low.getOrNull(i - 1), low.getOrNull(i + k)) ?: continue
                    // (A weekday by two letters that are a word or a name too, "to do 3pm", "with Mo 1pm": no day in the middle of a line.)
                    if (inside && Doubt.WEAK in h.doubts) continue
                    if (fits(h)) return Run(i, i + k, h)
                }
            }
            return null
        }
        var one = best(0, n, bare = true) { true } ?: return null
        // Two bare numbers at an end of the line are a time only where the line says nothing else about when. (Under the
        // keyword they are the time whatever else it says, as they have always been there: [ends].)
        if (!ends && one.to - one.from == 1 && BARE.matches(low[one.from])) best(0, n, bare = false) { true }?.let { one = it }
        var runs = listOf(one)
        var hit = one.hit
        if (hit.seconds == null && !(hit.day && hit.timed)) {
            // Only a day, or only a time: the other half, before it or after it. The longer of the two; after it, where they are as long.
            val other: (Hit) -> Boolean = { it.seconds == null && it.day != one.hit.day && it.timed != one.hit.timed }
            val before = best(0, one.from, bare = ends, other)
            val after = best(one.to, n, bare = ends, other)
            val two = if (before != null && (after == null || before.to - before.from > after.to - after.from)) before else after
            if (two != null) { hit = both(one.hit, two.hit); runs = listOf(one, two).sortedBy { it.from } }
            // "All day" with no day anywhere in the line is neither a day nor a time: the line is read again without it.
            else if (!hit.day && !hit.time) return spot(text, now, led, ends, alone = false, own)
        }
        // A word directly beside the day's own words that makes another day of it, and a short weekday beside the
        // expression that is a second day: which day was meant is a guess. So is a word directly before the time's own
        // words that says the event does not begin then, or makes another time of it.
        fun shifts(word: String?) = word != null && Matcher.fold(word) in SHIFTS
        fun which(word: String?) = word != null && Matcher.fold(word) in WHICH
        fun second(word: String?) = word != null && Days.slight(word)
        fun limits(word: String?) = word != null && (Matcher.fold(word) in LIMITS || (word.length == 1 && word[0] in DASHES))
        if (runs.any { r ->
            val before = low.getOrNull(r.from - 1)
            val after = low.getOrNull(r.to)
            val next = low.getOrNull(r.to + 1)
            (r.hit.opens && (shifts(before) || which(before))) || (r.hit.closes && shifts(after)) || (r.hit.day && (second(before) || second(after))) ||
                // ("Friday 7pm after next": the two words say another Friday wherever the time stands.)
                (r.hit.day && after == "after" && next == "next") ||
                (r.hit.time && !r.hit.opens && limits(before)) ||
                // After the time: a zone by the two letters that are a word anywhere else ("3pm PT"), or another place's time
                // ("3pm London time"): the hour is not this device's.
                (r.hit.time && after != null && (Matcher.fold(after) in NEAR || Matcher.fold(after) in ELSEWHERE || (next != null && Matcher.fold(next) in ELSEWHERE))) ||
                // And a word of the line's own directly after the minutes of „15 Uhr 30“ may make a count of them („30 Leute“).
                (r.hit.time && r.to < mine && r.to - r.from >= 2 && MINUTES.matches(low[r.to - 1]) && low[r.to - 2].endsWith(UHR))
        }) hit = hit.with(Doubt.BESIDE)
        val pieces = ArrayList<Piece>(3)
        var from = 0
        for (r in runs + Run(n, n, hit)) {
            if (r.from > from) Words.cut(text, words, from, r.from).trimEnd(',', ';').takeIf { it.isNotEmpty() }?.let { pieces.add(Piece(it, words[from].start)) }
            from = r.to
        }
        return Spot(moment(hit, pieces.joinToString(" ") { it.text }, again), pieces, runs.joinToString(" ") { Words.cut(text, words, it.from, it.to) }, runs.any { it.hit.repeats })
    }

    /**
     * A day or a time still stands in [title], the words that were left when an event's day and time were
     * read: a second one ("Mon 3pm Tue 4pm" leaves "Tue 4pm"), "all day" beside a time that was read, or
     * words for a half of the day that no time took ("in the evening"). Which was meant is then a guess.
     * Not for what is far more often something else ([slight]).
     */
    internal fun again(title: String, now: LocalDateTime): Boolean {
        if (title.isBlank()) return false
        val low = Words.of(title).map { it.text.lowercase().trimEnd(',', ';') }
        if (low.indices.any { i -> (1..3).any { k -> i + k <= low.size && whole(low.subList(i, i + k)) } } || Times.halved(low)) return true
        return spot(title, now)?.let { !slight(it.said) } ?: false
    }

    /**
     * A number standing alone, or a word of time that nobody read, still stands in [title], the words that were left when an
     * event's day and time were read: "Zahnarzt 30", "standup 9 to", "2 hour workshop", "Schicht Uhr", "dinner next week", "7pm
     * EST". What it meant for the day, the time or the length was typed and not read, so the reading is a guess ([Doubt.LEFT]):
     * this is the net under every other rule, for what no list of sentences foresees. A number standing alone is digits, with
     * or without a dot, a colon, a dash, a slash, an ordinal's ending or a unit ("9", "14.", "9:30", "3rd", "2h", "1h30"); a
     * number inside a word is none ("Q3", "B12", "5k", "#12"). A month's name counts only after "in" or "of" ("Monday in
     * November"): alone it is as often a name or a word of the title ("May", "October review"). The place is not asked: a
     * place has numbers of its own ("Room 4", "5 Main Street"). A word for a half of the day ("night", "morning", „Abend“) is
     * a title's own word where the time that was read ([at]; null: none, all day) is in that half: "movie night Friday 8pm"
     * is no guess, „Essen Freitag Abend 9 Uhr“ is one. And "1:1" is no number of time: a time's minutes have two digits.
     */
    internal fun left(title: String, at: LocalTime? = null): Boolean {
        val words = Words.of(title).map { it.text.lowercase().trim { c -> c in MARKS } }
        return words.indices.any { i -> alone(words[i]) || timely(words[i]) || halved(words[i], at) || (i > 0 && words[i - 1] in OF && Days.month(words[i])) }
    }

    /** [word] is a number standing alone, with a unit or an ordinal's ending or without. */
    private fun alone(word: String): Boolean =
        (ALONE.matches(word) && !RATIO.matches(word)) || (word.firstOrNull()?.isDigit() == true && Durations.pieces(word)?.all { (_, unit) -> unit.isEmpty() || Durations.unit(unit) != null } == true)

    /** [word] names a half of the day that is not the one [at] is in. */
    private fun halved(word: String, at: LocalTime?): Boolean = HALVES[Matcher.fold(word)]?.let { hours -> at == null || at.hour !in hours } ?: false

    /** [word] is a word of time ([TIMELY]), or a zone's letters ("EST", "GMT+2"). */
    private fun timely(word: String): Boolean = Matcher.fold(word.replace("'", "").replace("’", "")) in TIMELY || ZONE.matches(word)

    /** Signs at a word's edge that are the sentence's, not the word's. */
    private const val MARKS = ".,;:!?()[]\"'„“”‚‘’…"
    private val ALONE = Regex("[0-9]+(?:[.,:/–—-][0-9]+)*(?:st|nd|rd|th|am|pm|uhr)?")
    /** "1:1", "2:1": one digit after the colon is no time. */
    private val RATIO = Regex("[0-9]{1,2}:[0-9]")
    /** A half of the day by a word that titles have too, and the hours that are in it. */
    private val HALVES = mapOf("morning" to 4..11, "vormittag" to 4..11, "afternoon" to 12..17, "nachmittag" to 12..17, "evening" to 17..23, "abend" to 17..23,
        "night" to ((17..23) + (0..4)), "nacht" to ((17..23) + (0..4))).mapValues { it.value.toSet() }
    /**
     * Words of time that the rules read only in their own places, or not at all: left in a title beside a day and a time that
     * were read, each says something about when that was not. Folded. (Midnight, noon and a second day are found by [again].)
     */
    private val TIMELY = setOf("week", "weeks", "weekend", "fortnight", "month", "months", "year", "years", "days", "hour", "hours", "hr", "hrs", "min", "mins", "minute", "minutes",
        "half", "quarter", "past", "noon", "midnight", "tonight", "daily", "oclock",
        "woche", "wochen", "wochenende", "monat", "monate", "monaten", "jahr", "jahre", "jahren", "tage", "tagen", "stunde", "stunden", "minuten", "uhr", "halb", "viertel", "dreiviertel",
        "mittags", "abends", "morgens", "nachts", "heute", "morgen", "ubermorgen", "uebermorgen",
        "utc", "gmt", "est", "edt", "cst", "cdt", "mst", "mdt", "pst", "pdt", "cet", "cest", "mez", "mesz", "bst")
    /** A zone by its distance: "GMT+2", "UTC-5". */
    private val ZONE = Regex("(?:utc|gmt)[+-][0-9]{1,2}(?::[0-9]{2})?")
    /** After these a month's name left in the title says which month a day is in: "Monday in November", „Freitag im November“. */
    private val OF = setOf("in", "of", "im")

    /**
     * [word], the last of a line, without the mark that ends the line: any "!" and "?", and a full stop, unless the dot is the
     * word's own: a number's („am 12.“, „14.10.“) or an abbreviation's ("p.m.").
     */
    private fun ended(word: String): String {
        val w = word.trimEnd('!', '?')
        if (!w.endsWith('.')) return w
        val bare = w.dropLast(1)
        return if (bare.isEmpty() || '.' in bare || bare.all { it in '0'..'9' }) w else bare
    }

    /**
     * The [k] words of [low] (a line's words, lower case) from [i] on would begin a span with a month's day: a number alone
     * directly after a month's name, then a word that leads to a span's end ("Oct 14 until 18 Uhr", "Oct 14 to noon"). That
     * number is the day, and such a run is no time. [dashed]: also where a dash alone leads to the end ("Oct 14 - 18 Uhr").
     */
    internal fun dated(low: List<String>, i: Int, k: Int, dashed: Boolean = true): Boolean =
        k >= 3 && i > 0 && NUMBER.matches(low[i]) && low[i + 1] in (if (dashed) UNTIL else TOWARD) && Days.month(low[i - 1])

    /** The word of [low] before [at] is a number alone and the word at [at] says it is an hour („… bis 18“ before „Uhr“): a run that ends there has cut a time in two. */
    internal fun halved(low: List<String>, at: Int): Boolean = at in 1 until low.size && NUMBER.matches(low[at - 1]) && low[at] in CLOCKS

    /** After a number alone these say it is an hour. (Not "am": in German it leads into a day or a place.) */
    private val CLOCKS = setOf("uhr", "pm", "p.m.")

    /** True for an expression that is far more often something else than a day or a time: a weekday's short form alone ("sun"), two bare numbers with a dash ("2-3"). */
    internal fun slight(said: String): Boolean = Days.slight(said) || BARE.matches(said.lowercase())

    /** True for a word that leads into a day or a time and means nothing without it ("on", "the", "at", „am“). */
    internal fun leads(word: String): Boolean = word.lowercase().let { it in LEADING || it == "the" || it == "from" || it == "von" || it == "vom" }

    private fun findOne(text: String, now: LocalDateTime, alone: Boolean): Hit? {
        val words = Words.of(text)
        val n = words.size
        val low = words.map { it.text.lowercase().trimEnd(',', ';') }
        for (k in minOf(n, SPAN) downTo 1) {
            read(low.subList(0, k), now, alone, null, low.getOrNull(k))?.let { it.rest = Words.cut(text, words, k, n); return it }
            // (A month's day is no span's first hour, "Oct 14 to noon". With a dash between them 3.1 read one, and a line that is
            // read at its ends alone keeps that: a reminder rings as it did.)
            if (k < n && !dated(low, n - k, k, dashed = false)) read(low.subList(n - k, n), now, alone, low[n - k - 1], null)?.let { it.rest = Words.cut(text, words, 0, n - k).trimEnd(',', ';'); return it }
        }
        return null
    }

    /** [w] as one whole expression, or null. [alone]: "all day" by itself is one. [before], [after]: the words beside [w] in its line, where there are any. */
    private fun read(w: List<String>, now: LocalDateTime, alone: Boolean, before: String?, after: String?): Hit? {
        if (w[0] == "in") relative(w, now)?.let { return it }
        // "every Monday 6pm": the next Monday at six, and a guess. Only a weekday repeats that way.
        if (w[0] in EVERY) return if (w.size > 1 && Days.parse(w.subList(1, minOf(w.size, 2)), now.toLocalDate()) is Day.Every) once(w, now, alone, before, after)
            ?.let { Hit(it.start, it.end, it.day, it.time, whole = it.whole, repeats = true, vague = it.vague, doubts = it.doubts, opens = it.opens, closes = it.closes) } else null
        return once(w, now, alone, before, after)
    }

    private fun once(w: List<String>, now: LocalDateTime, alone: Boolean, before: String?, after: String?): Hit? {
        val today = now.toLocalDate()
        // "All day", said outright, with nothing beside it: no day yet and no time, only the half that a day at the
        // line's other end makes whole ([find], [spot]: without that day it is nothing).
        if (alone && whole(w)) return Hit(today.atStartOfDay(), null, day = false, time = false, whole = true)
        val led = w[0] in LEADING
        val hour = w[0] in HOUR
        val dotted = w[0] in DOTTED
        // ("on the 12th", „am 12.“: after these a day of the month by its number alone is a day.)
        val on = dotted || w[0] == "on"
        val next = w[0] in NEXT
        val body = if (led) w.subList(1, w.size) else w
        if (body.isEmpty()) return null
        Days.parse(body, today, dotted, on)?.let { return resolve(it, null, now, next) }
        Times.parse(body, bare = hour, afternoon = hour)?.let { return resolve(null, it, now) }
        Days.range(body, today, if (body === w) before else w[0], after)?.let { r -> return Hit(r.first.atStartOfDay(), r.last.plusDays(1).atStartOfDay(), day = true, time = false, whole = true, doubts = r.doubts) }
        // A day then a time, or a time then a day. The longer first part first, so "9 am Freitag" is 9 am.
        for (i in body.size - 1 downTo 1) {
            val a = body.subList(0, i)
            val rest = body.subList(i, body.size)
            val at = rest[0] in HOUR
            val dot = rest[0] in DOTTED
            val after = dot || rest[0] == "on"
            val later = rest[0] in NEXT
            val b = if (rest[0] in LEADING && rest[0] !in EVERY) rest.subList(1, rest.size) else rest
            if (b.isEmpty()) continue
            val day = Days.parse(a, today, dotted, on)
            if (day != null) {
                // ("Friday next 7pm": the "next" between a day and its time says another Friday, and is a guess.)
                Times.parse(b, bare = at, afternoon = at)?.let { return resolve(day, it, now, next)?.let { h -> if (later) h.with(Doubt.BESIDE) else h } }
                if (whole(rest)) return allDay(day, today, next, first = true)
            }
            val clock = Times.parse(a, bare = hour, afternoon = hour)
            if (clock != null) Days.parse(b, today, dot, after)?.let { return resolve(it, clock, now, later, first = false) }
            if (!led && whole(a)) Days.parse(b, today, dot, after)?.let { return allDay(it, today, later, first = false) }
        }
        return null
    }

    private fun whole(w: List<String>): Boolean = w.size <= 3 && Matcher.fold(w.joinToString(" ")) in WHOLE

    /**
     * What of [day] itself is a guess: its date's own doubts, a weekday by two letters, "next" before a weekday ([next]), and a
     * weekday that is [today]'s: with a time still ahead it is read as today, else as a week on, and which was meant was not said.
     */
    private fun doubts(day: Day?, next: Boolean, today: LocalDate): Set<Doubt> = when (day) {
        is Day.On -> day.doubts
        is Day.Every -> setOfNotNull(Doubt.WEAK.takeIf { day.weak }, Doubt.NEXT.takeIf { next }, Doubt.WEEK.takeIf { !next && day.day == today.dayOfWeek })
        null -> emptySet()
    }

    /** [day], all day, said outright. [next]: "next" stands before it. [first]: the day's words stand before "all day". */
    private fun allDay(day: Day, today: LocalDate, next: Boolean, first: Boolean): Hit {
        val date = when (day) { is Day.On -> day.date; is Day.Every -> next(today, day.day) }
        return Hit(date.atStartOfDay(), null, day = true, time = false, whole = true, doubts = doubts(day, next, today), opens = first, closes = !first)
    }

    private fun relative(w: List<String>, now: LocalDateTime): Hit? {
        val length = Durations.leading(w.subList(1, w.size), bareMinutes = false) ?: return null
        if (length.used != w.size - 1 || !(length.seconds >= 1 && length.seconds <= YEAR)) return null
        val seconds = length.seconds.roundToLong()
        return Hit(now.plusSeconds(seconds), null, day = false, time = true, seconds = seconds)
    }

    /**
     * [next]: "next" stands before [day]: a weekday is then never today, and a guess. [first]: the day's words stand before
     * the time's.
     */
    private fun resolve(day: Day?, clock: Clock?, now: LocalDateTime, next: Boolean = false, first: Boolean = true): Hit? {
        val today = now.toLocalDate()
        if (clock == null) {
            val date = when (day) {
                is Day.On -> if (day.weak) return null else day.date
                is Day.Every -> if (day.weak) return null else next(today, day.day)
                null -> return null
            }
            return Hit(date.atStartOfDay(), null, day = true, time = false, doubts = doubts(day, next, today))
        }
        val ahead = clock.time.isAfter(now.toLocalTime())
        val date = when (day) {
            null -> if (ahead) today else today.plusDays(1)
            is Day.On -> day.date
            // ("next Tuesday", typed on a Tuesday, is a week on whatever the time: never today.)
            is Day.Every -> if (today.dayOfWeek == day.day && ahead && !next) today else next(today, day.day)
        }
        val end = clock.end?.let { date.atStartOfDay().plusMinutes(it.toLong()) }
        // (A day of the month by its number alone, with no "on" before it, is a day only because a time stands beside it:
        // it is as often the third of something, "room 3rd 3pm". Read, and a guess.)
        return Hit(date.atTime(clock.time), end, day = day != null, time = true, vague = clock.vague || (day is Day.On && day.weak),
            doubts = doubts(day, next, today) + clock.doubts, opens = day != null && first, closes = day != null && !first)
    }

    /** The next [day] after [today], one to seven days on. */
    private fun next(today: LocalDate, day: DayOfWeek): LocalDate {
        val gap = (day.value - today.dayOfWeek.value + 7) % 7
        return today.plusDays(if (gap == 0) 7L else gap.toLong())
    }
}
