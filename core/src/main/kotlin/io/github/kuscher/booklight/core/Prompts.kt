package io.github.kuscher.booklight.core

/** A prompt the user keeps: "Fix the spelling and grammar. Reply with only the corrected text: {text}". */
object Prompts {
    const val MARK = "{text}"

    /** Where a ready-made prompt names the language to translate into, and where it takes what the user asked for. */
    const val LANGUAGE = "{language}"
    const val INSTRUCTION = "{instruction}"

    /**
     * The prompt with [text] where `{text}` stands, or after it when it has no such place. The
     * prompt is read once: a `{text}` that arrives inside the text stays text. [more]: other
     * places and what goes there (`{language}`, `{instruction}`), filled in in the same reading.
     */
    fun fill(prompt: String, text: String, more: Map<String, String> = emptyMap()): String {
        val p = prompt.trim()
        if (p.isEmpty()) return text
        val places = more + (MARK to text)
        val out = StringBuilder(p.length + text.length)
        var i = 0
        var placed = false
        while (i < p.length) {
            val hit = if (p[i] == '{') places.keys.firstOrNull { p.startsWith(it, i) } else null
            if (hit != null) { out.append(places.getValue(hit)); i += hit.length; if (hit == MARK) placed = true } else { out.append(p[i]); i++ }
        }
        return if (placed) out.toString() else "$out\n\n$text"
    }
}
