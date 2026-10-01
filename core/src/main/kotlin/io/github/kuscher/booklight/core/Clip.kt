package io.github.kuscher.booklight.core

/** One thing done to the clipboard's text. [id] names it: upper, lower, title, line, count, url. */
data class Transform(val id: String, val value: String)

/** What `clip` offers for the text the clipboard holds. */
object Clip {
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
