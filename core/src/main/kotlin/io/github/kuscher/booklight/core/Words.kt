package io.github.kuscher.booklight.core

/** One word of a typed line and where it sits, so what is left over can be cut out as it was typed. */
internal class Word(val text: String, val start: Int, val end: Int)

internal object Words {
    /** The runs of [s] between white space, in order. */
    fun of(s: String): List<Word> {
        val out = ArrayList<Word>()
        var i = 0
        while (i < s.length) {
            while (i < s.length && s[i].isWhitespace()) i++
            val start = i
            while (i < s.length && !s[i].isWhitespace()) i++
            if (i > start) out.add(Word(s.substring(start, i), start, i))
        }
        return out
    }

    /** The line from word [from] up to, not including, word [to]: the user's own casing and spacing. */
    fun cut(s: String, words: List<Word>, from: Int, to: Int): String =
        if (from >= to) "" else s.substring(words[from].start, words[to - 1].end)
}
