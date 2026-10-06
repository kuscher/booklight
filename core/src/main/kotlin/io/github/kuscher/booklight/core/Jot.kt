package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import kotlin.math.roundToInt

/** A mail to hand to the compose window: nothing here sends. */
data class MailDraft(val to: List<String>, val subject: String, val body: String)

/**
 * An event for the calendar's editor. [dayGiven] and [timeGiven] say what was typed; the rest is
 * guessed (today, all day, one hour) and shown dimmer in the preview. "All day" said outright, and a
 * range of days, count as the time given. [repeats]: the line said "every Monday" or "weekly", which
 * this version cannot carry: the day is the next such day, and no more than a guess. [vague]: the time
 * that was typed does not say its half of the day ("at 7", "7:30"): that half is a guess. [twice]: a
 * second day or time is left in the title ("Mon 3pm Tue 4pm"), or words about the time of day that
 * were not read with it ("in the evening", "all day"): which was meant is a guess. [doubts]: what
 * else of the day or the time was chosen by the parser and not typed ([Doubt]): a year, which number is
 * the month, "next Tuesday", a number or a word of time that was left in the title.
 */
data class EventDraft(val title: String, val start: LocalDateTime, val end: LocalDateTime, val allDay: Boolean,
    val place: String, val dayGiven: Boolean, val timeGiven: Boolean, val repeats: Boolean = false, val vague: Boolean = false, val twice: Boolean = false,
    val doubts: Set<Doubt> = emptySet()) {
    /**
     * Nothing of when it is is a guess: the day and the time were both read from what was typed, the time
     * says its half of the day, the line asks for no repeat, no other day or time stands in it, and
     * nothing else of it was the parser's choice ([doubts]). Only such an event is ever saved without the
     * calendar's editor.
     */
    val sure: Boolean get() = dayGiven && timeGiven && !repeats && !vague && !twice && doubts.isEmpty()
}

data class TimerSpec(val seconds: Int, val label: String)
data class AlarmSpec(val hour: Int, val minute: Int, val label: String)

/** What will ring for a reminder: the Clock's timer or alarm when it is soon, a calendar event when it is not. */
sealed interface ReminderPlan {
    data class Timer(val spec: TimerSpec) : ReminderPlan
    data class Alarm(val spec: AlarmSpec, val at: LocalDateTime) : ReminderPlan
    data class Event(val draft: EventDraft) : ReminderPlan
}

enum class NewKind { FILE, FOLDER, DOC, SHEET, SLIDES }

/** [kindGiven] is false when the kind was guessed from the name alone. */
data class NewSpec(val kind: NewKind, val name: String, val kindGiven: Boolean)

/**
 * Reads the one-line arguments of the capture scopes: mail, note, event, remind, timer, alarm, new.
 * Parts of a line are separated by " / ". Nothing here fails: text that isn't understood stays in
 * the title, the subject or the label.
 */
object Jot {
    private const val DAY = 24 * 3600
    private val KINDS: Map<String, NewKind> = buildMap {
        for (w in listOf("file", "datei")) put(w, NewKind.FILE)
        for (w in listOf("folder", "ordner", "directory", "dir")) put(w, NewKind.FOLDER)
        for (w in listOf("doc", "document", "dokument")) put(w, NewKind.DOC)
        for (w in listOf("sheet", "spreadsheet", "tabelle")) put(w, NewKind.SHEET)
        for (w in listOf("slides", "presentation", "prasentation", "praesentation")) put(w, NewKind.SLIDES)
    }

    /** "anna@x.com, ben@x.com Lunch? / See you at 1": the leading words with an @ are recipients, " / " starts the message. */
    fun mail(arg: String): MailDraft {
        val parts = parts(arg)
        val words = Words.of(parts[0])
        val to = ArrayList<String>()
        var i = 0
        while (i < words.size) {
            val w = words[i].text
            if ('@' !in w && w.any { it != ',' && it != ';' }) break
            for (address in w.split(',', ';')) if (address.isNotEmpty() && address != "@") to.add(address)
            i++
        }
        return MailDraft(to.distinct(), Words.cut(parts[0], words, i, words.size), parts.drop(1).joinToString("\n").trim())
    }

    /** The note's text; " / " becomes a line break. */
    fun note(arg: String): String = parts(arg).joinToString("\n").trim()

    /**
     * "Fri 3pm Dentist @ Main St": what follows the last " @ " is the place, a day or time anywhere in
     * the line is when ([When.spot]), the rest is the title. No day: today. No time: all day. No end:
     * one hour. The line of the keyword `event`: two bare numbers at one of its ends are the time of a
     * day at the other ("9-10 standup tomorrow"), as they have always been there.
     */
    fun event(arg: String, now: LocalDateTime): EventDraft {
        val (text, place) = placed(arg)
        return draft(When.spot(text, now, bare = true)?.moment, text, place, now)
    }

    /** [arg] without its place, and the place: what follows the last " @ ". A " @" at the very end is a place about to be typed. */
    internal fun placed(arg: String): Pair<String, String> {
        val text = " " + arg.trim()
        val at = text.lastIndexOf(" @ ")
        return when {
            at >= 0 -> text.substring(0, at).trim() to text.substring(at + 3).trim()
            text.endsWith(" @") -> text.dropLast(2).trim() to ""
            else -> text.trim() to ""
        }
    }

    /** The event that [m] and its rest make; with no day or time read, [text] is the title, today, all day. */
    internal fun draft(m: Moment?, text: String, place: String, now: LocalDateTime, title: String? = null): EventDraft {
        if (m == null) return now.toLocalDate().atStartOfDay().let { EventDraft(title ?: text, it, it.plusDays(1), true, place, dayGiven = false, timeGiven = false) }
        val end = m.end ?: if (m.allDay) m.start.plusDays(1) else m.start.plusHours(1)
        val name = title ?: m.rest
        // (A number or a word of time that is still in the title was typed and not read: a length, "for 2 hours", "2 hour
        // workshop"; the minutes, another week, a zone. The end and the day the row shows are then the parser's own.)
        return EventDraft(name, m.start, end, m.allDay, place, m.dayGiven, m.timeGiven, m.repeats, m.vague, twice = When.again(name, now),
            doubts = if (When.left(name, m.start.toLocalTime().takeIf { !m.allDay })) m.doubts + Doubt.LEFT else m.doubts)
    }

    /**
     * A day or a time that stands somewhere in a copied text ("Dinner with Anna on Friday at 7pm.
     * Call…"), as the line the event row is typed as: when first, then the rest of its sentence
     * ("on Friday at 7pm Dinner with Anna"). Null when it cannot be read. [near] is the date as the
     * system found it: only its sentence is read, and the expression must share a word with it, so
     * "the sun" is never Sunday. A day and a time may stand apart ("Am Freitag treffen wir uns um
     * 15 Uhr"); a word left hanging where the expression stood ("due by") is dropped.
     */
    fun eventIn(text: String, near: String, now: LocalDateTime): String? {
        val hint = near.lowercase().split(SPACE).map { it.trim { c -> !c.isLetterOrDigit() } }.filter { it.isNotEmpty() }.toSet()
        if (hint.isEmpty()) return null
        val sentence = SENTENCE.split(text).firstOrNull { near in it } ?: return null
        val words = sentence.trim().trimEnd('.', '!', '?').split(SPACE).map { it.trim(',', ';', ':', '(', ')', '"', '„', '“', '”') }.filter { it.isNotEmpty() }.take(WORDS)
        /**
         * The longest run of [w] that is all of it a day or a time, and passes [fits]; the earliest of the longest. Not a run
         * that takes a month's day for a span's first hour ("Oct 14 to noon"), nor one that ends in the number of an hour
         * („… bis 18“ before „Uhr“): those are a day and a time, each a run of its own.
         */
        fun run(w: List<String>, fits: (Moment, List<String>) -> Boolean): IntRange? {
            val low = w.map { it.lowercase() }
            for (len in minOf(RUN, w.size) downTo 1) for (i in 0..w.size - len) {
                if (When.dated(low, i, len) || When.halved(low, i + len)) continue
                val part = w.subList(i, i + len)
                val m = When.parse(part.joinToString(" "), now) ?: continue
                if (m.rest.isBlank() && fits(m, part)) return i until i + len
            }
            return null
        }
        val first = run(words) { _, part -> part.any { it.lowercase().trim { c -> !c.isLetterOrDigit() } in hint } } ?: return null
        val one = When.parse(words.slice(first).joinToString(" "), now) ?: return null
        var left = words.subList(0, first.first).dropLastWhile { it.lowercase() in HANGING } + words.subList(first.last + 1, words.size)
        var expression = words.slice(first)
        if (one.dayGiven != one.timeGiven) run(left) { m, _ -> m.dayGiven != one.dayGiven && m.timeGiven != one.timeGiven }?.let { second ->
            expression = expression + left.slice(second)
            left = left.subList(0, second.first).dropLastWhile { it.lowercase() in HANGING } + left.subList(second.last + 1, left.size)
        }
        val line = (expression + left).joinToString(" ")
        return line.takeIf { event(it, now).let { e -> e.dayGiven || e.timeGiven } }
    }

    private val SPACE = Regex("\\s+")
    /** Where a sentence ends: after its mark and a space, but not after a number's dot ("am 3. Oktober"); and at a line break. */
    private val SENTENCE = Regex("(?<![0-9][.])(?<=[.!?])\\s+|\\n+")
    /** A word that led up to the day or the time and means nothing without it. */
    private val HANGING = setOf("on", "at", "by", "until", "till", "for", "from", "between", "this", "next", "am", "um", "den", "bis", "ab", "vom", "von", "zum", "zwischen")
    /** How many words of a sentence are read, and how long a day and a time are at most. */
    private const val WORDS = 60
    private const val RUN = 8

    /**
     * "in 20m stretch" is a timer; a time in the next 24 hours an alarm, labelled with the text;
     * anything later a half-hour event, at 09:00 when only a day was given. No day or time: null.
     */
    fun reminder(arg: String, now: LocalDateTime): ReminderPlan? {
        // ("All day" alone names neither a day nor a time to ring at: [When.find] reads none of it.)
        val hit = When.find(arg, now) ?: return null
        val seconds = hit.seconds
        if (seconds != null && seconds <= DAY) return ReminderPlan.Timer(TimerSpec(seconds.toInt(), hit.rest))
        val at = if (hit.time) hit.start else hit.start.withHour(9)
        if (hit.time && at.isAfter(now) && !at.isAfter(now.plusHours(24))) {
            return ReminderPlan.Alarm(AlarmSpec(at.hour, at.minute, hit.rest), at)
        }
        return ReminderPlan.Event(EventDraft(hit.rest, at, at.plusMinutes(30), false, "", hit.day, hit.time))
    }

    /**
     * "10m tea", "90s", "1h30", "1h 30m", "1.5h", "10 min", "2 Std", with or without "in" in front.
     * A number alone is minutes. What follows is the label. Null without a duration at the start,
     * and for nothing at all or more than 24 hours.
     */
    fun timer(arg: String): TimerSpec? {
        val words = Words.of(arg)
        val from = if (words.size > 1 && words[0].text.equals("in", ignoreCase = true)) 1 else 0
        val length = Durations.leading(words.subList(from, words.size).map { it.text.lowercase() }, bareMinutes = true) ?: return null
        if (!(length.seconds >= 0.5 && length.seconds <= DAY)) return null
        return TimerSpec(length.seconds.roundToInt(), Words.cut(arg, words, from + length.used, words.size))
    }

    /**
     * "7:30 gym", "7am", "7 pm x", "19 Uhr", "at 7"; a number alone is the hour as it stands.
     * "in 8h" is that long from [now]. What follows is the label. Otherwise null.
     */
    fun alarm(arg: String, now: LocalDateTime): AlarmSpec? {
        val words = Words.of(arg)
        val low = words.map { it.text.lowercase() }
        if (low.isEmpty()) return null
        if (low[0] == "in") {
            val length = Durations.leading(low.subList(1, low.size), bareMinutes = false) ?: return null
            if (!(length.seconds >= 1 && length.seconds <= DAY)) return null
            val at = now.plusSeconds(length.seconds.toLong())
            return AlarmSpec(at.hour, at.minute, Words.cut(arg, words, 1 + length.used, words.size))
        }
        val from = if (low.size > 1 && (low[0] == "at" || low[0] == "um")) 1 else 0
        for (k in 2 downTo 1) {
            if (from + k > low.size) continue
            val clock = Times.parse(low.subList(from, from + k), bare = true, span = false) ?: continue
            return AlarmSpec(clock.time.hour, clock.time.minute, Words.cut(arg, words, from + k, words.size))
        }
        return null
    }

    /**
     * "file ideas.md", "folder Projects/Alpha", "doc Title", "Ordner X": a kind word first, then the name.
     * Without one it is a file, or a folder when the name ends with a slash. The name may be empty.
     */
    fun new(arg: String): NewSpec {
        val words = Words.of(arg)
        val kind = words.firstOrNull()?.let { KINDS[Matcher.fold(it.text)] }
        if (kind != null) return NewSpec(kind, Words.cut(arg, words, 1, words.size), kindGiven = true)
        val name = arg.trim()
        return if (name.endsWith("/")) NewSpec(NewKind.FOLDER, name.trimEnd('/').trimEnd(), kindGiven = false)
        else NewSpec(NewKind.FILE, name, kindGiven = false)
    }

    /** The line split at " / ", each part trimmed. A " /" at the very end starts a part that is still empty. */
    private fun parts(arg: String): List<String> {
        val t = arg.trim()
        return (if (t.endsWith(" /")) t.dropLast(2) else t).split(" / ").map { it.trim() }
    }
}
