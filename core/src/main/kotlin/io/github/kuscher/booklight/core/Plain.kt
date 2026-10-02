package io.github.kuscher.booklight.core

/**
 * The device's model writes Markdown unless told not to, and sometimes when told. An answer is
 * shown in a row and pasted into mails: the marks come off, the words and the line breaks stay.
 */
object Plain {
    private val FENCE = Regex("^```[A-Za-z0-9_-]*\\s*$")
    private val HEADING = Regex("^#{1,6}\\s+")
    private val BULLET = Regex("^(\\s*)[*•]\\s+")
    private val BOLD = Regex("(\\*\\*|__)(?=\\S)(.+?)(?<=\\S)\\1")
    private val STAR = Regex("(?<![\\w*])\\*(?=[^\\s*])([^*\\n]+?)(?<=[^\\s*])\\*(?![\\w*])")
    private val TICK = Regex("`([^`\\n]+)`")

    fun of(text: String): String = text.lineSequence().filterNot { FENCE.matches(it.trim()) }.joinToString("\n") { line ->
        var s = HEADING.replace(line, "")
        s = BULLET.replace(s) { it.groupValues[1] + "- " }
        s = BOLD.replace(s) { it.groupValues[2] }
        s = STAR.replace(s) { it.groupValues[1] }
        TICK.replace(s) { it.groupValues[1] }
    }
}
