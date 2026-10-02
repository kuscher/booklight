package io.github.kuscher.booklight.core

/** One thing done to the clipboard's text. [id] names it: upper, lower, title, line, count, url. */
data class Transform(val id: String, val value: String)

/** A kind of thing the system can find in a text. Their order here is the order they are named and listed in. */
enum class Thing { LINK, DATE, PHONE, MAIL }

/**
 * What the system says about a copy while its content has not been read: what kind it is, how old,
 * whether whoever copied it marked it private, whether Booklight made it, and what the system
 * found in it ([found]: by kind, a score from 0 to 1; null while it has not looked, or will not).
 */
data class Look(
    val text: Boolean,
    val ageMs: Long,
    val private: Boolean = false,
    val own: Boolean = false,
    val found: Map<Thing, Float>? = null,
    /** The system is still looking at it: [found] may come. */
    val looking: Boolean = false,
)

/** How old a copy is, the way the line says it: it does not tick. */
sealed interface Age {
    data object JustNow : Age
    /** In steps of ten: 10 to 50. */
    data class Seconds(val n: Int) : Age
    data object Minute : Age
}

/** The line under the empty field for a fresh copy: how old it is and the things found in it, in their order. */
data class Offer(val age: Age, val things: List<Thing>, val looking: Boolean)

/** What `clip` offers for the text the clipboard holds, and when a copy is offered unasked. */
object Clip {
    /** A copy is offered for two minutes. */
    const val FRESH_MS = 120_000L
    /** How sure the system must be that a thing is in the text for the line to name it. */
    const val SURE = 0.5f

    /**
     * The line for this copy, or null for none: older than two minutes, marked private, Booklight's
     * own, not a text, or the switch is off. The copy's time is the wall clock's, which can be set:
     * a copy "from the future" by more than a few seconds is not offered.
     */
    fun offer(look: Look, on: Boolean = true): Offer? {
        if (!on || !look.text || look.private || look.own) return null
        if (look.ageMs > FRESH_MS || look.ageMs < -5_000) return null
        val things = look.found.orEmpty().filter { it.value >= SURE }.keys.sortedBy { it.ordinal }
        return Offer(age(look.ageMs), things, look.looking)
    }

    fun age(ms: Long): Age = when {
        ms < 10_000 -> Age.JustNow
        ms < 60_000 -> Age.Seconds((ms / 10_000).toInt() * 10)
        else -> Age.Minute
    }

    /**
     * Which of the [found] things of one kind gets the row: the first. For a phone number, the first
     * with seven digits or more: the system also calls a postcode ("10117 Berlin") a phone number.
     */
    fun pick(kind: Thing, found: List<String>): String? = when (kind) {
        Thing.PHONE -> found.firstOrNull { it.count(Char::isDigit) >= PHONE_DIGITS }
        else -> found.firstOrNull()
    }
    private const val PHONE_DIGITS = 7

    /** The things the line names: two at most, and whether "and more" follows them. */
    fun named(things: List<Thing>): Pair<List<Thing>, Boolean> = things.take(2) to (things.size > 2)

    /**
     * The language a translation of a text goes into, as a tag: a text that is not in the app's
     * language comes into it; one that is goes into the other one, German in an English Booklight
     * and English in any other. [text]: the text's language, null if it could not be told.
     */
    fun target(text: String?, app: String): String {
        val mine = app.substringBefore('-').lowercase()
        val its = text?.substringBefore('-')?.lowercase()
        return if (its != null && its != mine) mine else if (mine == "en") "de" else "en"
    }

    /** True if a text in language [text] is not in the app's own: its translation then comes first. */
    fun foreign(text: String?, app: String): Boolean = text != null && text.substringBefore('-').lowercase() != app.substringBefore('-').lowercase()

    /**
     * The transforms of [text], in a fixed order; none for empty or blank text. One that would change
     * nothing is left out. `count` is always there: its value is the words, a space, the characters
     * ("12 68"), for the app to put into a sentence.
     */
    fun transforms(text: String): List<Transform> {
        if (text.isBlank()) return emptyList()
        val words = Words.of(text)
        return listOf(
            Transform("upper", text.uppercase()),
            Transform("lower", text.lowercase()),
            Transform("title", title(text)),
            Transform("line", words.joinToString(" ") { it.text }),
            Transform("count", "${words.size} ${text.codePointCount(0, text.length)}"),
            Transform("url", Templates.encode(text)),
        ).filter { it.id == "count" || it.value != text }
    }

    /** The first letter of each word upper case, the others lower. Spacing stays. */
    private fun title(text: String): String {
        val b = StringBuilder(text.length)
        var start = true
        for (c in text) {
            b.append(if (start) c.titlecaseChar() else c.lowercaseChar())
            start = c.isWhitespace()
        }
        return b.toString()
    }
}
