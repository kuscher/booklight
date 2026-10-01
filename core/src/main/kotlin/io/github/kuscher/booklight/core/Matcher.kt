package io.github.kuscher.booklight.core

import java.text.Normalizer

/**
 * Scores how well typed text matches a name, 0 (no match) to 1 (the same).
 *
 * The order people expect from a launcher: the whole name, then its start, then the start of a
 * later word ("book" → "Summa Book"), then initials ("gc" → "Google Chrome", "vsc" → "VSCodeBook"),
 * then anywhere inside, then the letters in order with gaps ("chrm"). Every word of the query
 * has to match somewhere.
 */
object Matcher {
    const val EXACT = 1.0
    const val PREFIX = 0.9
    const val WORD_PREFIX = 0.8
    const val INITIALS = 0.75
    const val INSIDE = 0.6
    const val SCATTERED = 0.45

    fun score(query: String, name: String): Double {
        val q = fold(query)
        if (q.isEmpty()) return 0.0
        val words = words(name)
        val n = fold(name)
        if (n.isEmpty()) return 0.0
        val tokens = q.split(' ').filter { it.isNotEmpty() }
        if (tokens.size > 1) {
            // "google ch": the whole thing first (it may be a prefix), else every word on its own.
            val whole = one(q, n, words)
            if (whole > 0) return whole
            var sum = 0.0
            for (t in tokens) {
                val s = one(t, n, words)
                if (s == 0.0) return 0.0
                sum += s
            }
            return sum / tokens.size * 0.95
        }
        return one(q, n, words)
    }

    /**
     * For a hidden keyword ("dark" for Display): only the start of the keyword or of one of its
     * words counts, a little below a match on the name. No initials, no scattered letters:
     * a result found by a word the user can't see has to be an obvious one.
     */
    fun keyword(query: String, word: String): Double {
        val s = score(query, word)
        return if (s >= WORD_PREFIX) s * 0.85 else 0.0
    }

    private fun one(q: String, n: String, words: List<String>): Double {
        if (q == n) return EXACT
        // A shorter name is the closer match for the same typed text: "Chrome" before "Chrome Beta".
        val brevity = 0.05 * q.length / n.length
        if (n.startsWith(q)) return PREFIX + brevity
        if (words.drop(1).any { it.startsWith(q) }) return WORD_PREFIX + brevity
        if (q.length >= 2 && initials(q, words)) return INITIALS + brevity
        if (n.contains(q)) return INSIDE + brevity
        return scattered(q, n)
    }

    /** "gc" for "google chrome", "vsc" for "vs code book": letters taken from the starts of words, in order. */
    private fun initials(q: String, words: List<String>): Boolean {
        if (words.size < 2) return false
        // Each word gives one or more of its leading letters, or is skipped; two words at least.
        fun go(qi: Int, wi: Int, used: Int): Boolean {
            if (qi == q.length) return used >= 2
            if (wi == words.size) return false
            val w = words[wi]
            var k = 0
            while (k < w.length && qi + k < q.length && w[k] == q[qi + k]) {
                k++
                if (go(qi + k, wi + 1, used + 1)) return true
            }
            return go(qi, wi + 1, used)
        }
        return go(0, 0, 0)
    }

    /** The letters in order with gaps. Fewer, smaller gaps score higher; needs 3+ letters. */
    private fun scattered(q: String, n: String): Double {
        if (q.length < 3) return 0.0
        var qi = 0
        var first = -1
        var last = -1
        for (i in n.indices) {
            if (qi < q.length && n[i] == q[qi]) {
                if (first < 0) first = i
                last = i
                qi++
            }
        }
        if (qi < q.length) return 0.0
        val span = last - first + 1
        val tight = q.length.toDouble() / span          // 1 = no gaps
        val early = if (first == 0) 1.0 else 0.6        // starts with the first letter
        return SCATTERED * (0.4 + 0.6 * tight) * early
    }

    /** Lower case, no accents, separators as single spaces. */
    fun fold(s: String): String {
        val d = Normalizer.normalize(s, Normalizer.Form.NFD)
        val b = StringBuilder(d.length)
        var space = false
        for (c in d) {
            when {
                Character.getType(c) == Character.NON_SPACING_MARK.toInt() -> {}
                c.isLetterOrDigit() -> { if (space && b.isNotEmpty()) b.append(' '); space = false; b.append(c.lowercaseChar()) }
                else -> space = true
            }
        }
        return b.toString()
    }

    /** The words of a name, folded: split at separators and at camelCase humps ("VSCodeBook" → vs, code, book). */
    fun words(name: String): List<String> {
        val d = Normalizer.normalize(name, Normalizer.Form.NFD).filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
        val out = ArrayList<String>()
        val cur = StringBuilder()
        fun flush() { if (cur.isNotEmpty()) { out.add(cur.toString().lowercase()); cur.clear() } }
        for (i in d.indices) {
            val c = d[i]
            if (!c.isLetterOrDigit()) { flush(); continue }
            if (cur.isNotEmpty()) {
                val prev = d[i - 1]
                val next = d.getOrNull(i + 1)
                val hump = c.isUpperCase() && (prev.isLowerCase() || (prev.isUpperCase() && next != null && next.isLowerCase()))
                if (hump) flush()
            }
            cur.append(c)
        }
        flush()
        return out
    }
}
