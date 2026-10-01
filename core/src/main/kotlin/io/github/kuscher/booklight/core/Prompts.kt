package io.github.kuscher.booklight.core

/** A prompt the user keeps: "Fix the spelling and grammar. Reply with only the corrected text: {text}". */
object Prompts {
    const val MARK = "{text}"

    /**
     * The prompt with [text] where `{text}` stands, or after it when it has no such place. The
     * prompt is read once: a `{text}` that arrives inside the text stays text.
     */
    fun fill(prompt: String, text: String): String {
        val p = prompt.trim()
        if (p.isEmpty()) return text
        if (MARK !in p) return "$p\n\n$text"
        val out = StringBuilder(p.length + text.length)
        var i = 0
        while (i < p.length) {
            if (p.startsWith(MARK, i)) { out.append(text); i += MARK.length } else { out.append(p[i]); i++ }
        }
        return out.toString()
    }
}
