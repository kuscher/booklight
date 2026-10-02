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

    /**
     * When to ask the search engine at all. The promise to the user is that sums and addresses
     * are never sent, and that has to hold while they are still being typed: "1500*" is not a sum
     * yet and "192.168.1.1" or "me@" is no address [Web.url] knows, but none of them may leave the
     * device. So only text that reads as words is sent: it has letters, no address marks, and no
     * digit next to a sign of arithmetic. The same holds for an address with an app's own scheme.
     */
    fun worthAsking(text: String): Boolean {
        val t = text.trim()
        if (t.length !in 2..80 || t.none { it.isLetter() }) return false
        if (t.startsWith("=") || "://" in t || '@' in t) return false
        if (Calc.answer(t) != null || Web.url(t) != null) return false
        // An address with an app's own scheme, complete or in the making ("spotify:search:daftpunk", "spotify:"): one word
        // that starts as a scheme does. It is meant for that app, and no sentence starts so ("note: milk" has its space).
        if (t.none { it.isWhitespace() } && Schemes.of(t).let { it != null && it.length >= 2 }) return false
        // An address or a sum in the making: "localhost:3000", "10.0.0.5:8080/admin", "12 * 3.", "sqrt(", "2 p", "10 mod".
        if (t.none { it.isWhitespace() } && ('.' in t || ':' in t || '/' in t) && t.any { it.isDigit() }) return false
        if (t.any { it in MATHS }) {
            if (t.any { it.isDigit() } || t.endsWith("(")) return false
        }
        if (t.first().isDigit() && t.count { it.isLetter() } <= 3 && !t.contains(Regex("[A-Za-z]{4,}"))) return false
        return true
    }

    private const val MATHS = "+-*/^%×÷=()!"

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
