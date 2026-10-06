package io.github.kuscher.booklight.core

import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * One of the user's calendars, as the system lists it: its [name] and [color], the [id] the system has
 * for it on this device, and [owner], the address a calendar link names it by. [primary]: the account's
 * own calendar. [writable]: events can be added to it. [nameable]: its name, written into a sentence,
 * is read back as this calendar ([Cals.marked]): only such a one is offered in the row's list or
 * completed from its first letters. Nothing of what is in it: Booklight never reads an event.
 */
data class Cal(val id: Long, val name: String, val color: Int = 0, val owner: String = "", val primary: Boolean = false, val writable: Boolean = true, val nameable: Boolean = true)

/**
 * What a sentence says about an event, read by rules ([Sentence.read]): the event ([draft]), the
 * calendar it names ([calendar], one of the user's), and how sure the reading is.
 *
 * [rest]: only the start of that calendar's name is typed, at the very end of the sentence, and this is
 * what is left of it: the field shows it in grey. [named]: the sentence names a calendar by the word
 * ("… to the Team calendar") that is none of the user's, or their calendars are not known: the name
 * stays in the title, and the event is never saved without the editor. [cue]: the sentence begins with
 * a word that says it is an event ("Add …"). [loose]: words stand on more than one side of the day and
 * time, so the title is put together from pieces, and one of them may be a place: the model on the
 * device may split it better. [said]: the day and time as they were typed, empty where none was read.
 * [everyday]: the sentence was typed without the keyword and its cue is a word that as often begins an
 * everyday search ("schedule", "put", "book", "plan"): it is offered only with a day and a time, and
 * never saved without the editor.
 */
data class EventReading(
    val draft: EventDraft, val calendar: Cal? = null, val rest: String? = null, val named: Boolean = false,
    val cue: Boolean = false, val loose: Boolean = false, val said: String = "", val everyday: Boolean = false,
)

/**
 * An event the way it is said: "Add dinner with Sam tomorrow at 7pm to the Team calendar". Read by
 * rules, at every letter, in English and German:
 *
 * 1. A word at the start that says "this is an event" is the cue. "Add", "schedule", "put", "new event"
 *    („trag … ein“, „neuer Termin“) are dropped; "book" and "plan" („plane“) stay in the title, because
 *    "Book dentist" and "Plan review" are what such events are called. „… eintragen“ at the end is dropped.
 *    Under the keyword `event` the line is the event: only the two-word cues are dropped there ("new
 *    event", „neuer Termin“), and "put out bins" keeps its "put".
 * 2. What follows the last " @ " is the place, as under the keyword.
 * 3. The day and time are found wherever they stand ([When.spot]).
 * 4. A calendar is found by its name: the words after "to", "in" or "on" („in den“, „im“) at the end of
 *    the sentence, with or without "calendar" or „-Kalender“, matched against the user's calendars
 *    without regard to case, to "the" and "my", and to a plural s; the name as it was typed first, so a
 *    calendar called "Family Calendar" is named by its own name. A calendar whose own name begins with
 *    an article ("The Tigers") is named with it and without it. Before a day or a time ("… to the Team
 *    calendar tomorrow") the word "calendar" must stand after the name: "Drive to work tomorrow" is
 *    what the event is called. After a place ("… @ Cafe Luna to Team") the whole name is the calendar,
 *    and the place ends before it. At the very end of the sentence the start of a name is enough (of
 *    a calendar that takes events and can be named; never an article alone), and so is a name with
 *    three letters or more of the word "calendar" after it. After "on" and "in" a name without the word
 *    is the calendar only as the calendar has it, not its plural or singular: "on Teams" is where the
 *    standup is held. A calendar's phrase that ends the line is not the line going on: "at 7 to Team"
 *    is a time, as "at 7" is.
 * 5. What is left is the title.
 *
 * Nothing here fails, and nothing is reckoned by anyone but [When].
 */
object Sentence {
    /** A sentence that begins with one of these is an event; the word is dropped. Nobody calls an event "Add dinner". */
    private val DROPPED = setOf("add", "schedule", "put", "trag", "trage")
    /** These begin an event's sentence too, and stay in its title. */
    private val KEPT = setOf("book", "plan", "plane")
    private val TWO = setOf("new event", "neuer termin", "neuen termin")
    /**
     * Cues that as often begin an everyday search ("schedule nfl sunday", "book flights to boston", "plan a
     * trip to rome"). A line that begins with one is an event only where it says a day and a time, and is
     * never saved from the list ([EventReading.everyday]).
     */
    private val EVERYDAY = setOf("schedule", "put", "book", "plan", "plane")
    /** German's „plane“ is English's "plane": a cue only where the day or the time was said in German. */
    private const val PLANE = "plane"
    /** „Trag … ein“: the verb's other half stands at the end. */
    private val SPLIT = setOf("trag", "trage")
    /** A calendar's name follows one of these, and may have one of [THE] before it. */
    internal val INTO = setOf("to", "in", "into", "on", "onto", "im", "ins", "zum", "zu")
    /**
     * After these two a name without the word "calendar" is a calendar only as the calendar has it: they lead into a place
     * as often ("on Teams", "in Boston"), so the plural or the singular of a name there is no calendar.
     */
    internal val EXACT = setOf("on", "in")
    private val THE = Cals.ARTICLES
    /** "…, add to the Team calendar": a second verb before the calendar goes with it. */
    private val ADDS = setOf("add", "put", "trag", "trage")
    /** The most words a calendar's name has, where none of the user's has more. */
    private const val NAME = 5
    /** The word that may follow a calendar's name, folded. */
    private val WORDS = listOf("calendar", "kalender")
    /** How many letters of that word, after a calendar's whole name, say that the word is on its way ("Team cal"). Fewer are as likely a name going on ("Sam K"). */
    private const val COMING = 3

    /**
     * What [text] says, read by the rules above. [calendars]: the user's, where they are known. [keyword]:
     * the text stands under the keyword `event`, where it is an event whatever its first word is.
     */
    fun read(text: String, now: LocalDateTime, calendars: List<Cal> = emptyList(), keyword: Boolean = false): EventReading = parsed(text, now, calendars, keyword).reading

    /**
     * Whether a line typed without the keyword is offered as an event: it begins with a cue, a day or
     * a time was read, and something is left to call it. A cue that stays in the title is no title by
     * itself ("plan tomorrow", „plane morgen“). Not for an expression that is far more often something
     * else: a weekday's short form alone ("add pictures of the sun"), two bare numbers with a dash
     * ("add 2-3 eggs"). A cue that as often begins an everyday search ([EventReading.everyday]) needs a
     * day and a time of day, both: "schedule nfl sunday", "book the midnight library" and "plan trip to
     * rome oct 14 to oct 16" are searches.
     */
    fun reads(r: EventReading): Boolean {
        val e = r.draft
        if (!r.cue || e.title.isBlank() || Matcher.fold(e.title) in KEPT || When.slight(r.said)) return false
        return if (r.everyday) e.dayGiven && e.timeGiven && !e.allDay else e.dayGiven || e.timeGiven
    }

    /** Whether [text] begins with a cue at all. Asked before a line typed without the keyword is read: one that is no event's costs no more than this. */
    fun cued(text: String): Boolean = cue(Words.of(text.trimStart().take(CUE)).map { Matcher.fold(it.text) }, keyword = false).second

    /**
     * How many words at the start of a sentence, given folded, are a cue that is dropped; and whether it begins with a cue
     * at all. With [keyword] only the two-word cues are one: the keyword has said that the line is an event.
     */
    private fun cue(f: List<String>, keyword: Boolean): Pair<Int, Boolean> = when {
        f.size >= 2 && "${f[0]} ${f[1]}" in TWO -> 2 to true
        keyword -> 0 to false
        f.isNotEmpty() && f[0] in DROPPED -> 1 to true
        else -> 0 to (f.isNotEmpty() && f[0] in KEPT)
    }

    /** More letters than any cue's two words have. */
    private const val CUE = 32

    /**
     * [text] with the calendar [name] in it: in place of the calendar it names now (or of the letters a
     * name began with), else after the sentence's own words, led in by [word] ("to", „in“). What a
     * calendar chosen from the row's list writes into the field. Where the calendar it names now stands
     * before a day or a time ("… to the Team calendar tomorrow"), that phrase goes and the name is
     * written at the sentence's end: only there is a name alone a calendar.
     */
    fun put(text: String, name: String, word: String, now: LocalDateTime, calendars: List<Cal>, keyword: Boolean = false): String = putter(text, word, now, calendars, keyword)(name)

    /** [put] for one sentence and many names: the sentence is read once, and each name is written into it. (A row has a line for every calendar, at every key.) */
    fun putter(text: String, word: String, now: LocalDateTime, calendars: List<Cal>, keyword: Boolean = false): (String) -> String {
        val p = parsed(text, now, calendars, keyword)
        return { name ->
            // (Without the phrase of a calendar named before a day or a time, if there is one; and where the sentence's own words then end.)
            val text = p.cut?.let { p.text.substring(0, it.first).trimEnd().trimEnd(',', ';') + p.text.substring(it.last + 1) } ?: p.text
            val end = p.end - (p.text.length - text.length)
            // (Before a place that begins where the name is written, "@ Cafe Luna", a space.)
            if (p.name == null || p.cut != null) (text.substring(0, end).trimEnd() + " $word $name").trimStart() + text.substring(end).let { if (it.isEmpty() || it[0].isWhitespace()) it else " $it" }
            // (A name that begins with an article of its own, "The Tigers", takes the place of the article that was typed before the old one, too.)
            else p.text.substring(0, if (Cals.short(name) != name) p.article ?: p.name.first else p.name.first) + name + p.text.substring(p.name.last + 1)
        }
    }

    /** [title] without a cue that is dropped at its start: the model on the device sometimes leaves the verb in. */
    internal fun uncued(title: String, keyword: Boolean = false): String {
        val words = Words.of(title)
        return Words.cut(title, words, cue(words.map { Matcher.fold(it.text) }, keyword).first, words.size)
    }

    /**
     * How many of a sentence's first words, given folded, are a cue that is dropped ("add", "new event"); and whether its
     * last is the other half of „trag … ein“, or „eintragen“. Under the [keyword] neither verb is dropped.
     */
    internal fun dropped(f: List<String>, keyword: Boolean = false): Pair<Int, Boolean> {
        val from = cue(f, keyword).first
        return from to (!keyword && f.size > from && (f.last() == "eintragen" || (f.last() == "ein" && f[0] in SPLIT)))
    }

    /** A calendar's name as a sentence has it, folded, without the word "calendar" or „-Kalender“; and whether that word stood there. */
    internal fun bare(name: String): Pair<String, Boolean> {
        val f = Matcher.fold(name)
        return when {
            f.endsWith(" calendar") -> f.removeSuffix(" calendar") to true
            f.endsWith(" kalender") -> f.removeSuffix(" kalender") to true
            f.endsWith("kalender") && f.length > "kalender".length -> f.removeSuffix("kalender") to true
            else -> f to false
        }
    }

    /**
     * The calendar of [calendars] that [typed] names: by the name as it was typed, which may itself end in the word
     * ("Family Calendar"), and then by the name without the word "calendar" after it ("Team calendar"). [exact]: without
     * the word, only by the name as the calendar has it ([EXACT]).
     */
    internal fun called(typed: String, calendars: List<Cal>, exact: Boolean = false): Cal? =
        Cals.named(Matcher.fold(typed), calendars, exact) ?: bare(typed).takeIf { it.second }?.let { Cals.named(it.first, calendars) }

    /**
     * The reading, the trimmed text it was read from, where a calendar's name stands in that text ([article]: where the
     * words before it begin that may be an article, "the" of "to the Team"), and where the sentence's own words end
     * (before a place, before „ein“). [cut]: the whole phrase of a calendar that is named before a day or a time ("to
     * the Team calendar" in "… to the Team calendar tomorrow"), where there is one.
     */
    private class Parsed(val reading: EventReading, val text: String, val name: IntRange?, val article: Int?, val end: Int, val cut: IntRange?)

    /** A calendar found at the end of a piece: where its phrase, its name and the words after "to" start in the piece. */
    private class Found(val cal: Cal?, val rest: String?, val phrase: Int, val name: Int, val article: Int)

    private fun parsed(input: String, now: LocalDateTime, calendars: List<Cal>, keyword: Boolean): Parsed {
        val t = input.trim()
        val words = Words.of(t)
        val f = words.map { Matcher.fold(it.text) }
        // 1. The cue, and the German verb's other half.
        val (from, cued) = cue(f, keyword)
        var to = words.size
        if (dropped(f, keyword).second) to--
        val start = if (from < to) words[from].start else t.length
        // 2. The place.
        val whole = if (from < to) t.substring(start, words[to - 1].end) else ""
        val (main, after) = Jot.placed(whole)
        val end = start + main.length
        // (What the calendar's search fills in. A name may be as long as the longest of the user's own.)
        val longest = maxOf(NAME, calendars.maxOfOrNull { Words.of(it.name).size } ?: 0)
        var calendar: Cal? = null
        var rest: String? = null
        var named = false
        var name: IntRange? = null
        var article: Int? = null
        var cut: IntRange? = null
        var known = -1
        var place = after
        // (A calendar's whole name as it was typed: where it reads as a day too and no word "calendar" says which, that is a guess.)
        var plain: String? = null
        // (Where the words that were read for a matched calendar stand in [main]: they say nothing about a repeat.)
        var own: IntRange? = null
        // 4, its first half. A calendar after the place, at the line's end: a lead word and a calendar's whole name. The place
        // ends before it. The start of a name there is not completed: the place may go on.
        if (after.isNotEmpty()) phrase(after, calendars, longest, last = false, tail = true)?.let { found ->
            val at = start + whole.length - after.length
            name = at + found.name until at + after.length
            article = at + found.article
            if (found.cal == null) named = true
            else {
                calendar = found.cal
                place = after.substring(0, found.phrase).trimEnd().trimEnd(',', ';')
                plain = after.substring(found.name)
            }
        }
        // 3. The day and time, and the sentence's pieces around them. (After a cue that was dropped is the middle of the line.)
        // A calendar's phrase that ends the line is not the line going on: "at 7 to Team" is a time, as "at 7" is, and a line
        // keeps its day and its time when a calendar from the row's list is written into it. So the line's own words end where
        // the phrase of a calendar of the user's begins, its name whole or begun. (Not one that is only named by the word.)
        val ends = if (calendar == null) phrase(main, calendars, longest, last = end == t.length, tail = true)?.takeIf { it.cal != null }?.phrase else null
        val spot = if (ends == null) When.spot(main, now, led = from > 0, bare = keyword) else When.spot(main, now, led = from > 0, bare = keyword, own = ends).let { read ->
            // (Unless the day or the time was then read out of that very phrase, "to Friday": such a line is read as it stands.)
            val kept = read?.pieces?.lastOrNull()?.let { it.start <= ends && it.start + it.text.length == main.trimEnd(',', ';').length } == true
            if (read == null || kept) read else When.spot(main, now, led = from > 0, bare = keyword)
        }
        val pieces = (spot?.pieces ?: listOfNotNull(main.takeIf { it.isNotEmpty() }?.let { Piece(it, 0) })).toMutableList()
        // 4. The calendar at the end of a piece, the last piece first. Only the sentence's very end may hold the start of a name,
        // and only its last piece a name without the word "calendar": before a day or a time such words are the title's.
        if (calendar == null) for (i in pieces.indices.reversed()) {
            val p = pieces[i]
            // (The line's last piece: nothing of the day or the time follows it. A comma after it changes nothing of that.)
            val tail = i == pieces.lastIndex && p.start + p.text.length == main.trimEnd(',', ';').length
            val found = phrase(p.text, calendars, longest, last = tail && p.start + p.text.length == main.length && end == t.length, tail = tail) ?: continue
            val at = start + p.start
            val range = at + found.name until at + p.text.length
            // (Its whole phrase, where a day or a time follows it.)
            val stretch = (at + found.phrase until at + p.text.length).takeIf { !tail }
            if (found.cal == null) {
                // Named by the word, and none of the user's: it stays where it is. (The first such from the end counts.)
                if (!named) { named = true; name = range; article = at + found.article; cut = stretch; if (found.phrase == 0) known = i }
                continue
            }
            calendar = found.cal; rest = found.rest; named = false; known = -1
            name = range
            article = at + found.article
            cut = stretch
            own = p.start + found.phrase until p.start + p.text.length
            if (found.rest == null) plain = p.text.substring(found.name)
            pieces[i] = Piece(p.text.substring(0, found.phrase).trimEnd().trimEnd(',', ';'), p.start)
            break
        }
        // 5. The title: what is left, in its order. Pieces on more than one side of the day and time make a loose one.
        val left = pieces.withIndex().filter { it.value.text.isNotEmpty() }
        val title = left.joinToString(" ") { it.value.text }
        val loose = left.count { it.index != known } > 1
        // A word that says a repeat inside the matched calendar's own words says none ("to Weekly sync", "to the Daily
        // calendar"): only the sentence's other words, and the day's own ("every Monday"), do.
        var moment = spot?.moment
        val skip = own
        if (moment != null && spot != null && moment.repeats && skip != null)
            moment = moment.copy(repeats = spot.every || Words.of(main).any { it.start !in skip && Days.repeats(it.text.lowercase().trimEnd(',', ';')) })
        var draft = Jot.draft(moment, main, place, now, title)
        // A calendar whose own name reads as a day or a time ("to Friday", after another day): the calendar, or a second day.
        // The row shows the calendar, and which was meant is a guess.
        if (plain?.let { !bare(it).second && When.again(it, now) } == true) draft = draft.copy(twice = true)
        // „plane“ is German's cue where the day or the time was said in German; else it is the thing that flies, and no cue.
        val plane = !keyword && f.firstOrNull() == PLANE && Words.of(spot?.said.orEmpty()).none { Days.german(it.text) }
        val cue = cued && !plane
        return Parsed(EventReading(draft, calendar, rest, named, cue, loose, spot?.said.orEmpty(), everyday = cue && !keyword && f.firstOrNull() in EVERYDAY), t, name, article, end, cut)
    }

    /**
     * The calendar that [piece] ends with: "… to the Team calendar", "… in Team", „… in den Team-Kalender“,
     * with "add" or "put" before it. A calendar of [calendars] by its whole name: as it was typed, first with
     * the article that stands before it, which may be the calendar's own ("to The Tigers"), then without; then
     * the same without the word "calendar". With [tail] false (a day or a time follows the piece) only where
     * that word stands after the name. After "on" and "in" ([EXACT]) a name without the word is the calendar
     * only as the calendar has it. With [last], also by the start of its name. Named by the word
     * "calendar" and none of them: found, with no calendar. Else null. [longest]: the most words a name has.
     */
    private fun phrase(piece: String, calendars: List<Cal>, longest: Int, last: Boolean, tail: Boolean): Found? {
        val w = Words.of(piece)
        val g = w.map { Matcher.fold(it.text) }
        // (A name, and the word "calendar" after it.)
        val most = longest + 1
        var unknown: Found? = null
        for (j in w.lastIndex - 1 downTo maxOf(0, w.size - most - 3)) {
            if (g[j] !in INTO) continue
            var k = j + 1
            while (k < w.lastIndex && g[k] in THE) k++
            if (w.size - k > most) continue
            // ("…, add to", "put it in": the verb goes with the calendar.)
            val verb = if (j > 0 && g[j - 1] in ADDS) j - 1 else if (j > 1 && g[j - 1] == "it" && g[j - 2] in ADDS) j - 2 else j
            fun found(cal: Cal?, rest: String?, at: Int) = Found(cal, rest, w[verb].start, w[at].start, w[j + 1].start)
            // Where the name may begin: at the article, then after it. The name as it was typed before the name without
            // the word: a calendar's own name may end in "Calendar".
            val starts = listOf(j + 1, k).distinct().map { it to piece.substring(w[it].start) }
            for ((at, typed) in starts) if (tail || bare(typed).second) Cals.named(Matcher.fold(typed), calendars, exact = g[j] in EXACT)?.let { return found(it, null, at) }
            for ((at, typed) in starts) bare(typed).takeIf { it.second }?.let { Cals.named(it.first, calendars) }?.let { return found(it, null, at) }
            val typed = piece.substring(w[k].start)
            val (folded, byWord) = bare(typed)
            if (folded.isEmpty()) continue
            if (byWord) { if (unknown == null) unknown = found(null, null, k); continue }
            if (!last) continue
            // At the very end of what is typed the start of a name is enough: a guess until the name is whole. Else the word
            // "calendar" may be on its way after a whole name ("… to the Team cal"). (The start first: with a calendar "Work"
            // and one "Work calendar", "to Work cal" is on its way to the second, and nothing is saved to the first.)
            Cals.starting(typed, calendars)?.let { (cal, rest) -> return found(cal, rest, k) }
            coming(folded, calendars)?.let { return found(it, null, k) }
        }
        return unknown
    }

    /**
     * The calendar whose whole name [folded] is, with the first letters of the word "calendar" or „Kalender“ after it,
     * [COMING] or more: that word is still being typed.
     */
    private fun coming(folded: String, calendars: List<Cal>): Cal? {
        for (word in WORDS) for (n in COMING until word.length) {
            if (!folded.endsWith(word.take(n))) continue
            Cals.named(folded.dropLast(n).trimEnd(), calendars)?.let { return it }
        }
        return null
    }
}

/**
 * An event as it is written into a calendar: the calendar's [calendar] id on this device, and the
 * event's own values. [start] and [end] are epoch milliseconds; an all-day event runs from midnight to
 * midnight in UTC, whatever the device's zone, and says so in [zone]: that is how calendars keep one.
 */
data class EventWrite(val calendar: Long, val title: String, val start: Long, val end: Long, val allDay: Boolean, val zone: String, val place: String)

/** The user's calendars: which of them a name means, which one a new event goes to, and what is sent or written for an event. */
object Cals {
    /** The web calendar's own address for a new event, filled in: the Calendar app takes it, and honours the calendar it names. */
    const val TEMPLATE = "https://calendar.google.com/calendar/render?action=TEMPLATE"
    /** Words that stand before a calendar's name in a sentence and are not it ("to the Team calendar"), folded. A calendar's own name may begin with one ("The Tigers"). */
    internal val ARTICLES = setOf("the", "my", "our", "den", "dem", "der", "die", "das", "mein", "meine", "meinen", "meinem", "unser", "unsere", "unseren", "unserem")
    private val DAY = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val INSTANT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    /**
     * The calendar a new event is written into when it is saved from the panel: the one the sentence
     * [named]; with none named, the one the user chose ([chosen]: its owner's address, empty for none),
     * else the account's own, else the first that can be written. Null where the named one cannot be
     * written (a calendar that is only subscribed to), and where there is none to write to. Where two
     * calendars have the chosen owner's address, or two are an account's own, the first in [all] is the
     * one: the list's order decides (the system's list is kept in one order: the account's own first,
     * then by name).
     */
    fun target(named: Cal?, all: List<Cal>, chosen: String): Cal? {
        if (named != null) return named.takeIf { it.writable }
        val open = all.filter { it.writable }
        return open.firstOrNull { chosen.isNotEmpty() && it.owner == chosen } ?: open.firstOrNull { it.primary } ?: open.firstOrNull()
    }

    /**
     * Whether Enter saves the event itself: only with the switch [on], a calendar to write to, a
     * title, an end after the start, and a day and a time that were both read from the sentence (or a
     * day with "all day", or a range of days). A guess is never saved ([EventDraft.sure]): not a day
     * that was guessed, not a time, not a time's half of the day, not a repeat, not one of two days.
     * Nor while a calendar's name is still being typed ([EventReading.rest]): the calendar the row
     * shows by its first letters is a guess until the name is whole or taken. Nor where the sentence
     * names a calendar that is none of the user's ([EventReading.named]): it asked for another calendar
     * than the one new events go to. Nor a line typed without the keyword that begins like an everyday
     * search ([EventReading.everyday]): "book flight to boston friday 9am" and Enter must never write.
     */
    fun saves(r: EventReading, on: Boolean, target: Cal?): Boolean =
        on && target != null && target.writable && r.draft.sure && r.rest == null && !r.named && !r.everyday && r.draft.title.isNotBlank() && r.draft.end.isAfter(r.draft.start)

    /**
     * The calendar link for [e], which opens the Calendar app's editor filled in: the title, the start
     * and end in UTC (a moment in [zone], the device's), or for an all-day event its first day and the
     * day after its last, the place, and [cal] as the calendar to put it on. With no calendar none is
     * named, and the Calendar app chooses.
     */
    fun link(e: EventDraft, cal: Cal?, zone: ZoneId): String {
        fun q(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
        val dates = if (e.allDay) "${DAY.format(e.start)}/${DAY.format(e.end)}"
            else listOf(e.start, e.end).joinToString("/") { INSTANT.format(it.atZone(zone).withZoneSameInstant(ZoneOffset.UTC)) }
        return buildString {
            append(TEMPLATE)
            if (e.title.isNotBlank()) append("&text=").append(q(e.title))
            append("&dates=").append(dates)
            if (e.place.isNotBlank()) append("&location=").append(q(e.place))
            if (cal != null && cal.owner.isNotBlank()) append("&src=").append(q(cal.owner))
        }
    }

    /** What is written for [e] into [cal], with the device's [zone]: exactly what the row shows, and nothing else. */
    fun write(e: EventDraft, cal: Cal, zone: ZoneId): EventWrite {
        val at = if (e.allDay) ZoneOffset.UTC else zone
        return EventWrite(cal.id, e.title, e.start.atZone(at).toInstant().toEpochMilli(), e.end.atZone(at).toInstant().toEpochMilli(), e.allDay, if (e.allDay) "UTC" else zone.id, e.place)
    }

    /**
     * The calendar called [folded] (a name as [Matcher.fold] leaves it): the same name, or the name
     * without the article it begins with ("Tigers" for "The Tigers"), or either but for an s at the
     * end ("Teams" for "Team", „Arbeitskalender“ for „Arbeit“). In that order; the first of [all].
     * [exact]: not but for an s: only the name as the calendar has it.
     */
    fun named(folded: String, all: List<Cal>, exact: Boolean = false): Cal? {
        if (folded.isEmpty()) return null
        val names = all.map { Matcher.fold(it.name) }
        val short = names.map(::short)
        fun plural(name: String) = name.isNotEmpty() && (name + "s" == folded || folded + "s" == name)
        val i = names.indexOf(folded).takeIf { it >= 0 } ?: short.indexOf(folded).takeIf { it >= 0 }
            ?: if (exact) -1 else names.indexOfFirst(::plural).takeIf { it >= 0 } ?: short.indexOfFirst(::plural)
        return all.getOrNull(i)
    }

    /** [name] without the article it begins with ("The Tigers", "the tigers"); as it is where it begins with none, or is nothing else. */
    internal fun short(name: String): String {
        val words = Words.of(name)
        return if (words.size > 1 && Matcher.fold(words[0].text) in ARTICLES) name.substring(words[1].start) else name
    }

    /**
     * The calendar whose name starts with [typed] and goes on, and what is left of that name: letter
     * for letter but for case and accents ("mul" begins „Müller“). An article before either is not
     * counted: "ti" and "the ti" both begin "The Tigers", by its T. So a name is never begun by an
     * article alone, nor by fewer than two letters of its own. The first of [all] that takes events and
     * can be named ([Cal.nameable]): a start is never completed into a calendar nothing can be added
     * to, nor into a name that would not read back as its calendar. Null where [typed] is a calendar's
     * whole name, which has nothing left to complete.
     */
    fun starting(typed: String, all: List<Cal>): Pair<Cal, String>? {
        val f = short(Matcher.fold(typed))
        if (f.length < START || f in ARTICLES || named(f, all) != null) return null
        for (c in all) if (c.writable && c.nameable) {
            val name = short(c.name)
            val whole = Matcher.fold(name)
            if (whole.length <= f.length || !whole.startsWith(f)) continue
            // What is left of the name as the calendar has it: after its fewest letters that are what was typed.
            (1 until name.length).firstOrNull { Matcher.fold(name.take(it)) == f }?.let { return c to name.substring(it) }
        }
        return null
    }

    /**
     * [all], each marked with whether it can be named ([Cal.nameable]): written into a sentence as the row's list
     * and the completion write it ([Sentence.put]: after "to" in English and „in“ in German, and in another name's
     * place after the word that was typed there, which may be "on"), its name is read back as this very calendar,
     * whole, after each of them. That does not hold for the second of two calendars with one name (a name means
     * the first), for a name of signs alone, for one that reads as a day or a time ("Friday": after "to" it is the
     * day; "Mo": after "on", beside a time, a Monday), or one the place's sign cuts in two. Such a calendar is
     * still read where its name is typed and matches; it is only never offered and never completed. Worked out
     * once for a list, where it is read: not at a key.
     */
    fun marked(all: List<Cal>, now: LocalDateTime): List<Cal> = all.map { c ->
        val reads = LEADS.all { word -> PROBES.all { text -> Sentence.read(Sentence.put(text, c.name, word, now, all), now, all).let { it.calendar == c && it.rest == null } } }
        if (reads == c.nameable) c else c.copy(nameable = reads)
    }

    /** A sentence with a day and a time of its own, and one with none: a name that reads as a day is the day in the second. */
    private val PROBES = listOf("x tomorrow 9am", "x")

    /** The words a calendar's name is written after: the row's own, English's and German's, and "on", which a name may be typed after. */
    private val LEADS = listOf("to", "in", "on")

    /** The fewest letters that are taken for the start of a calendar's name. */
    private const val START = 2
}
