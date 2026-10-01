package io.github.kuscher.booklight.core

/**
 * When typed text reads as something to ask rather than something to find. Words are what stands
 * between spaces. Whether a local row matches better is the caller's question.
 */
object Ask {
    private val QUESTION: Set<String> = listOf(
        "how", "what", "why", "when", "who", "where", "which", "can", "could", "should", "is", "are", "does", "do",
        "write", "explain", "summarize", "summarise", "draft", "translate", "compare",
        "wie", "was", "warum", "wieso", "wann", "wer", "wo", "welche", "welcher", "welches", "kann", "soll", "ist", "sind",
        "schreibe", "schreib", "erkläre", "erklär", "fasse", "übersetze", "vergleiche",
    ).map(Matcher::fold).toSet()

    /** Asking comes first: five or more words, or a question mark at the end, or a question word first and three or more words. */
    fun first(text: String): Boolean {
        val words = Words.of(text)
        if (words.isEmpty()) return false
        if (words.size >= 5) return true
        val last = words.last().text
        if ((last.endsWith("?") || last.endsWith("？")) && text.any { it.isLetterOrDigit() }) return true
        // "what's" folds to "what s": the word before the apostrophe is the one that counts.
        return words.size >= 3 && Matcher.fold(words[0].text).substringBefore(' ') in QUESTION
    }

    /** Asking is offered under the web search: from two words on. */
    fun offered(text: String): Boolean = Words.of(text).size >= 2
}
