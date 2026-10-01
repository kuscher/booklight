package io.github.kuscher.booklight.core

/**
 * Reads a search engine's suggestion reply, the OpenSearch form every engine in [Engines] answers
 * with: `["typed text", ["first suggestion", "second", …], …]`. Anything else is no suggestions:
 * a reply from the network is never trusted to be well formed.
 */
object Suggest {
    fun parse(body: String, limit: Int = 8): List<String> = try {
        val r = Reader(body)
        r.ws()
        r.expect('[')
        r.ws()
        r.string()                    // the text the engine was asked about
        r.ws()
        r.expect(',')
        r.ws()
        r.expect('[')
        val out = ArrayList<String>()
        r.ws()
        if (r.peek() != ']') {
            while (true) {
                r.ws()
                val s = r.string().trim()
                if (s.isNotEmpty() && s.length <= 120 && out.size < limit) out.add(s)
                r.ws()
                if (r.peek() == ',') { r.next(); continue }
                break
            }
        }
        r.expect(']')
        out
    } catch (_: Exception) {
        emptyList()
    }

    /** When to ask at all: a couple of letters at least, and not something Booklight answers itself. */
    fun worthAsking(text: String): Boolean {
        val t = text.trim()
        return t.length in 2..80 && Calc.answer(t) == null && Web.url(t) == null
    }

    private class Reader(private val s: String) {
        private var i = 0
        fun peek(): Char = if (i < s.length) s[i] else throw IllegalStateException("end")
        fun next(): Char = peek().also { i++ }
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun expect(c: Char) { if (next() != c) throw IllegalStateException("expected $c") }
        fun string(): String {
            expect('"')
            val b = StringBuilder()
            while (true) {
                when (val c = next()) {
                    '"' -> return b.toString()
                    '\\' -> when (val e = next()) {
                        'n' -> b.append('\n'); 't' -> b.append('\t'); 'r' -> b.append('\r')
                        'b' -> b.append('\b'); 'f' -> b.append('\u000C')
                        'u' -> { b.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        else -> b.append(e)
                    }
                    else -> b.append(c)
                }
            }
        }
    }
}
