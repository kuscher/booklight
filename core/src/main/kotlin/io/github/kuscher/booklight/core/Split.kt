package io.github.kuscher.booklight.core

import java.time.LocalDateTime

/** A sentence as the model on the device split it, each part as the model gave it: nothing here has been checked. */
data class Split(val title: String, val said: String, val place: String, val calendar: String)

/**
 * The model on the device as a splitter: it is asked which words of an event's sentence are the title,
 * the day and time, the place and the calendar, and nothing else. It never reckons a date (that is
 * [When]'s), it is asked only where the rules leave a title in pieces ([asks]), and what it says is
 * taken only where every check of [merge] passes. Whatever fails, the rules' own reading stands.
 */
object Splits {
    /** The longest sentence the model is asked about: a longer one is no event typed into a launcher. */
    const val MAX = 200

    /** What the model is asked, word for word as it was tried on a device (docs/research/event-sentence.md); the sentence follows. */
    private const val INSTRUCTION = "Split the request into its parts. Answer with one line of JSON and nothing else:\n" +
        "{\"title\":\"\",\"when\":\"\",\"place\":\"\",\"calendar\":\"\"}\n" +
        "Rules: copy the words exactly from the request. \"when\" is every word about the day and the time. \"calendar\" is only the name of the calendar. " +
        "\"title\" is what the event is, without add, schedule or put, and without the time, the place and the calendar. Use \"\" for a part that is not there.\n" +
        "Request: "

    /** Words that lead into a place and are not its name: „in der Schule“ is „Schule“. */
    private val AT = setOf("at", "in", "im", "bei", "auf", "an")
    private val ARTICLE = setOf("der", "dem", "den")
    /** Between two parts of a sentence, before the words that lead into a calendar: "…, and put it in the Team calendar". */
    private val AND = setOf("and", "und")
    private val ADDS = setOf("add", "put", "trag", "trage")
    /** The word that may follow a calendar's name. */
    private val CALENDAR = setOf("calendar", "kalender")
    /** Signs at a word's edge that are part of the word ("C++", "#1"); any other mark there is the sentence's (a comma, a bracket). */
    private const val SIGNS = "+#$€%&"
    private val SPACES = Regex("\\s+")

    /** Whether the model is asked about [text] at all: only where the rules left the title in pieces. */
    fun asks(r: EventReading, text: String): Boolean = r.loose && text.trim().length <= MAX

    /**
     * The question for [text]: the instruction, and the sentence on one line, no more of it than [MAX] letters
     * whoever asks. The sentence is the user's own words and follows the instruction as they are: a line
     * that reads like an instruction can change what the model answers, and nothing else, because every
     * answer goes through [merge].
     */
    fun prompt(text: String): String = INSTRUCTION + text.trim().replace(SPACES, " ").take(MAX)

    /**
     * The model's answer, taken apart: one JSON object with a title and a "when", whatever stands
     * around it (it comes in a code fence). An answer whose last value has a quote too many is mended,
     * as two of sixteen had; anything else that is not such an object is no answer.
     */
    fun parse(answer: String): Split? {
        val from = answer.indexOf('{')
        val to = answer.lastIndexOf('}')
        if (from < 0 || to <= from) return null
        val line = answer.substring(from, to + 1)
        val map = read(line) ?: read(MENDS.replace(line, "\"\"}")) ?: return null
        fun part(key: String): String? = when (val v = map[key]) { is String -> v.trim(); null -> ""; else -> null }
        if (map["title"] !is String || map["when"] !is String) return null
        return Split(part("title") ?: return null, part("when") ?: return null, part("place") ?: return null, part("calendar") ?: return null)
    }

    private val MENDS = Regex("\"{3,}\\s*\\}$")
    private fun read(line: String): Map<*, *>? = try { Json.parse(line) as? Map<*, *> } catch (_: Exception) { null }

    /**
     * [own], the rules' reading of [text], with the model's [answer] where it helps. The answer is
     * taken only if all of this holds; else [own] comes back as it is:
     * - it is an answer ([parse]), with a title and a "when";
     * - every part is a piece of the text: its words stand there, whole, side by side, in that order. Each
     *   part is then taken from the text as it was typed, never from the answer;
     * - **the parts do not overlap**: the title, the "when" and the calendar are words no other part has. A
     *   place that is wholly a piece of the title, or the calendar's own words, is no place; any other
     *   overlap drops the answer;
     * - **the "when" is the very words the rules read as the day and time** (with a word before them that
     *   leads into a day, where the model left it out: "on", "the", „am“). So the day, the time, whether it
     *   is all day and how sure all of it is are the rules' own, and the model never chooses between two
     *   dates in a sentence. Only where the rules read no day and no time is the model's "when" read, by
     *   [When], from the text's own words, all of them: and that reading is a guess;
     * - a calendar it names is one of [calendars], the one the rules found if they found one, and **the
     *   sentence says it is a calendar**: the word "calendar" or „Kalender“ stands with it (and is not
     *   the calendar's own last word: "Family Calendar"), or a word that leads into one stands before it
     *   ("to", "in", „im“, with "the" or "my" between) and no day or time follows it, as the rules ask:
     *   "Drive to work tomorrow" names none, and after "on" and "in" only the name as the calendar has it
     *   is one ("on Teams" is no Team calendar). A name that is only a word of the sentence ("Team
     *   offsite") is never a calendar;
     * - a place typed after an @ is the place: the model's is taken only if it is that same place;
     * - **no word of the text is lost, and a word may go only by where it stands**: a cue as the sentence's
     *   first words ("Add …", and the other half of „trag … ein“ as its last; with [keyword], under the
     *   keyword `event`, only "new event": there the rules drop no other), the words that lead into
     *   the place directly before it ("at the …"), those that lead into the calendar directly before it
     *   ("…, and put it in my …") and the word "calendar" directly after it. "Hotel check in" keeps its
     *   "in". Every other word is in a part the row shows.
     * A cue the model left in a title that begins the sentence is dropped by the rules' own rule. A
     * second day or time that the model put into the title or the place makes the reading a guess, as it
     * does in the rules' own. So what comes back is sure only where [own] is ([EventDraft.sure]), and a
     * line that begins like an everyday search stays one ([EventReading.everyday]): the model never makes
     * an event one that is saved without the calendar's editor.
     */
    fun merge(own: EventReading, answer: String, text: String, now: LocalDateTime, calendars: List<Cal>, keyword: Boolean = false): EventReading {
        val s = parse(answer) ?: return own
        if (s.title.isEmpty() || s.said.isEmpty()) return own
        val words = Words.of(text)
        fun key(w: String) = w.lowercase().trim { !it.isLetterOrDigit() && it !in SIGNS }
        // The words that are words (not a comma or an @ alone), and where each stands among all of them.
        val at = words.indices.filter { key(words[it].text).isNotEmpty() }
        val keys = at.map { key(words[it].text) }
        val n = keys.size
        fun keysOf(part: String) = Words.of(part).map { key(it.text) }.filter { it.isNotEmpty() }
        fun typed(r: IntRange) = Words.cut(text, words, at[r.first], at[r.last] + 1).trimEnd(',', ';')
        val o = own.draft
        val taken = HashSet<Int>()
        // The rules' own place, typed after an @: its words are the row's, and no part of the model's can hold one of them.
        val fixed = if (o.place.isEmpty()) null else {
            val mark = words.indexOfLast { it.text == "@" }
            val p = keysOf(o.place)
            val from = at.indexOfFirst { it > mark }
            if (mark < 0 || from < 0 || from + p.size > n || p.indices.any { keys[from + it] != p[it] }) return own
            (from until from + p.size).also { taken += it }
        }
        /** Everywhere [part]'s words stand in the text, as places among [keys]. */
        fun runs(part: String): List<IntRange> {
            val p = keysOf(part)
            if (p.isEmpty() || p.size > n) return emptyList()
            return (0..n - p.size).filter { i -> p.indices.all { keys[i + it] == p[it] } }.map { it until it + p.size }
        }
        fun free(r: IntRange) = r.none { it in taken }
        val title = runs(s.title).firstOrNull(::free) ?: return own
        taken += title
        var said = runs(s.said).firstOrNull(::free) ?: return own
        while (said.first > 0 && said.first - 1 !in taken && When.leads(keys[said.first - 1])) said = said.first - 1..said.last
        taken += said
        // The day and time: where the rules read one, the model's "when" must be the very words they read. It splits a
        // sentence, and never points at another day in it, nor at less or more than was read.
        if (own.said.isNotEmpty() && said !in runs(own.said)) return own
        // Where they read none, the parser reads the model's words, all of them: a day or a time the rules did not read, and a guess.
        val found = if (own.said.isNotEmpty()) null else When.parse(typed(said), now)?.takeIf { it.rest.isBlank() } ?: return own
        // The calendar: one of the user's, no other than the rules' own, and only where the sentence says it is one: after
        // a word that leads into a calendar, or with the word "calendar". The first such place where its words are free.
        var calendar: IntRange? = null
        var cal: Cal? = null
        if (s.calendar.isNotEmpty()) {
            /** [r] without the articles it begins with, where something is left. */
            fun name(r: IntRange): IntRange { var k = r.first; while (k < r.last && keys[k] in Cals.ARTICLES) k++; return k..r.last }
            // (After "on" and "in" with no word "calendar", only the name as the calendar has it is one, as the rules ask.)
            var exact = false
            calendar = runs(s.calendar).filter(::free).firstOrNull { r ->
                var j = r.first - 1
                while (j >= 0 && j !in taken && keys[j] in Cals.ARTICLES) j--
                // The word, as part of what the model named or directly after it; not where it is the name's own last word.
                val whole = Cals.named(Matcher.fold(typed(r)), calendars) != null || Cals.named(Matcher.fold(typed(name(r))), calendars) != null
                val worded = (!whole && Sentence.bare(typed(r)).second) || (r.last + 1 < n && r.last + 1 !in taken && keys[r.last + 1] in CALENDAR)
                // A lead word alone says it only after the day and the time: before them the rules ask for the word, and so is this asked.
                val led = j >= 0 && j !in taken && keys[j] in Sentence.INTO && r.first > said.last
                exact = !worded && led && keys[j] in Sentence.EXACT
                worded || led
            } ?: return own
            // (By its name as the model gave it; with an article before it that is not the calendar's own, without that.)
            cal = Sentence.called(typed(calendar), calendars, exact) ?: Sentence.called(typed(name(calendar)), calendars, exact) ?: return own
            taken += calendar
        }
        // (So a calendar the rules read by the first letters of its name stays the rules': those letters are no calendar's
        // whole name, and no answer can name it. A name that is still being typed is never merged, and never saved.)
        if (own.calendar != null && cal != own.calendar) return own
        // The place: words of its own. One that is wholly a piece of the title, or the calendar's own words, is no place
        // (the model's faults, as they were seen); one that is the rules' own place is that place.
        var place: IntRange? = null
        if (s.place.isNotEmpty()) {
            val all = runs(s.place)
            place = all.firstOrNull(::free)
            if (fixed != null) { if (fixed !in all) return own; place = null }
            else if (place == null && all.none { r -> r.all { it in title } || (calendar != null && r.all { it in calendar }) }) return own
        }
        if (place != null) taken += place
        // No word lost: one that is in no part the row shows may go only by where it stands. The cue and „ein“ exactly where
        // the rules drop them: the sentence's first words, and its last.
        val (cue, verb) = Sentence.dropped(words.map { Matcher.fold(it.text) }, keyword)
        val gone = HashSet<Int>()
        gone += at.indices.filter { at[it] < cue || (verb && at[it] == words.lastIndex) }
        if (place != null) {
            // "at the Cafe": the articles before the place, and one word that leads into it before them.
            var j = place.first - 1
            while (j >= 0 && j !in taken && keys[j] in Cals.ARTICLES) gone += j--
            if (j >= 0 && j !in taken && keys[j] in AT) gone += j
        }
        if (calendar != null) {
            // "…, and put it in my Team calendar": the articles, the word that leads into it, "it", the verb, "and"; and the word "calendar" after it.
            var j = calendar.first - 1
            while (j >= 0 && j !in taken && keys[j] in Cals.ARTICLES) gone += j--
            if (j >= 0 && j !in taken && keys[j] in Sentence.INTO) {
                gone += j--
                if (j >= 1 && keys[j] == "it" && keys[j - 1] in ADDS) gone += j--
                if (j >= 0 && keys[j] in ADDS) {
                    gone += j--
                    if (j >= 0 && keys[j] in AND) gone += j
                }
            }
            if (calendar.last + 1 < n && keys[calendar.last + 1] in CALENDAR) gone += calendar.last + 1
        }
        if (keys.indices.any { it !in taken && it !in gone }) return own
        // (A cue the model left in the title is dropped where the title begins the sentence, as the rules drop it there and nowhere else.)
        val name = typed(title).let { if (at[title.first] == 0) Sentence.uncued(it, keyword) else it }
        if (name.isBlank()) return own
        val where = if (fixed != null) o.place else place?.let { unled(typed(it)) }.orEmpty()
        // When it is: the rules' own reading as it stands, the day, the time and every reason it may be a guess. A second day
        // or time is one more such reason wherever the model put it, in the title or in the place. So this is sure only where
        // the rules' own is.
        val second = o.twice || When.again(name, now) || (place != null && When.again(typed(place), now))
        // (So is a number or a word of time in the title the model made; and one that made the rules' own title a guess stays
        // a reason, wherever the model put it.)
        val draft = if (found == null) o.copy(title = name, place = where, twice = second, doubts = if (When.left(name, o.start.toLocalTime().takeIf { !o.allDay })) o.doubts + Doubt.LEFT else o.doubts)
            else Jot.draft(found.copy(vague = true), "", where, now, name).let { if (second) it.copy(twice = true) else it }
        // (A calendar the sentence named that is none of the user's stays named, wherever the model put its words: such
        // an event is never saved into another calendar.)
        return EventReading(draft, cal, named = cal == null && own.named, cue = own.cue, said = typed(said), everyday = own.everyday)
    }

    /** A place without the words that lead into it: "at Cafe Luna", „in der Schule“. */
    private fun unled(place: String): String {
        val words = Words.of(place)
        var from = 0
        if (words.size > 1 && words[0].text.lowercase() in AT) from = 1
        if (from == 1 && words.size > 2 && words[1].text.lowercase() in ARTICLE) from = 2
        return Words.cut(place, words, from, words.size)
    }
}
