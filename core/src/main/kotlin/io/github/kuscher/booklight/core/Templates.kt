package io.github.kuscher.booklight.core

import java.net.URLEncoder
import java.time.LocalDate

/**
 * The placeholders of a quicklink: `%s`, `{argument}` and `{query}` are what was typed after the
 * keyword, `{clipboard}` what the clipboard holds, `{date}` today as 2026-10-01.
 */
object Templates {
    private val ARGUMENT = listOf("%s", "{argument}", "{query}")
    private val MARKS = ARGUMENT + listOf("{clipboard}", "{date}")

    /** Whether the link wants text after its keyword. Without a placeholder for it, it is a plain link. */
    fun takesArgument(link: String): Boolean = ARGUMENT.any { it in link }

    /**
     * The link with its placeholders filled in, each value encoded for an address. The link is read
     * once, left to right: a placeholder that arrives inside a value stays text.
     */
    fun fill(link: String, argument: String, clipboard: String, date: LocalDate): String {
        val arg = lazy { encode(argument) }
        val clip = lazy { encode(clipboard) }
        val out = StringBuilder(link.length + 16)
        var i = 0
        while (i < link.length) {
            val mark = if (link[i] == '%' || link[i] == '{') MARKS.firstOrNull { link.startsWith(it, i) } else null
            if (mark == null) { out.append(link[i]); i++; continue }
            out.append(when (mark) { "{clipboard}" -> clip.value; "{date}" -> date.toString(); else -> arg.value })
            i += mark.length
        }
        return out.toString()
    }

    /** For a place in an address: UTF-8 percent-encoding, a space as %20 (a plus only means a space in a query). */
    internal fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
