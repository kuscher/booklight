package io.github.kuscher.booklight.core

/**
 * A typed verb and the action it arms: "uninstall", "info", "new window".
 * [min] is how many letters an abbreviation at the end needs ("chrome inf"); [atStart] is false for
 * verbs that only follow the name ("new window": at the start, "new" is a scope).
 */
data class Verb(val action: String, val words: List<String>, val min: Int = 2, val atStart: Boolean = true)

/** One way to read a text as a name and a verb: [rest] is the name part, as typed. */
data class Reading(val rest: String, val action: String)

/**
 * Reads a text as "name verb" or "verb name". It only splits the text: whether [Reading.rest] names
 * an app, and whether the whole text is a better name ("play store"), is the caller's question.
 */
object Verbs {
    /**
     * Every reading of [text], verbs at the end first (the longest first), none twice.
     *
     * At the end, the last one, two or three words are the start of one of a verb's words, with at least
     * [Verb.min] letters ("chrome inf", "chrome new w"); a whole verb word always counts. At the start
     * the verb must be a whole word, and only if [Verb.atStart]. A verb with nothing beside it is no
     * reading: "info" finds apps called Info. Case and accents don't matter ([Matcher.fold]).
     */
    fun readings(text: String, verbs: List<Verb>): List<Reading> {
        val words = Words.of(text)
        val n = words.size
        if (n < 2 || verbs.isEmpty()) return emptyList()
        val out = LinkedHashSet<Reading>()
        val widest = minOf(WIDEST, n - 1)
        // A whole verb word first, then the start of one: "chrome left" is Left before it is the start of "left third".
        // And the longest verb first: "chrome top left" is Top left before it is Left with a name of "chrome top".
        for (whole in listOf(true, false)) for (k in widest downTo 1) {
            val tail = folded(words, n - k, n) ?: continue
            val letters = tail.count { it != ' ' }
            for (v in verbs) {
                val hit = v.words.any { w -> val f = Matcher.fold(w); if (whole) f == tail else f != tail && letters >= v.min && f.startsWith(tail) }
                if (hit) out.add(Reading(Words.cut(text, words, 0, n - k), v.action))
            }
        }
        for (k in 1..widest) {
            val head = folded(words, 0, k) ?: continue
            for (v in verbs) {
                if (v.atStart && v.words.any { Matcher.fold(it) == head }) out.add(Reading(Words.cut(text, words, k, n), v.action))
            }
        }
        return out.toList()
    }

    /**
     * "chrome t", on the way to "chrome top left": the name part when the last word is a single
     * letter that starts one of the verbs. One letter is not a reading, but it must not make the
     * row vanish for a keystroke either. Null when the text is not of that shape.
     */
    fun dangling(text: String, verbs: List<Verb>): String? {
        val words = Words.of(text)
        val n = words.size
        if (n < 2) return null
        val last = Matcher.fold(words[n - 1].text)
        if (last.length != 1) return null
        if (verbs.none { v -> v.words.any { Matcher.fold(it).startsWith(last) } }) return null
        return Words.cut(text, words, 0, n - 1)
    }

    /** A verb is three words at most: "left two thirds". */
    private const val WIDEST = 3

    /** Words [from] to [to] folded and joined; null when one of them has no letters ("-"), so it can't hide in a verb. */
    private fun folded(words: List<Word>, from: Int, to: Int): String? {
        val parts = ArrayList<String>(to - from)
        for (i in from until to) parts.add(Matcher.fold(words[i].text).ifEmpty { return null })
        return parts.joinToString(" ")
    }
}
