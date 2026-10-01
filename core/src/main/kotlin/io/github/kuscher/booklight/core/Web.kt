package io.github.kuscher.booklight.core

/** Telling an address from search words. */
object Web {
    private val SCHEME = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)
    private val DOMAIN = Regex("^([a-z0-9-]+\\.)+[a-z]{2,}(:\\d+)?(/\\S*)?$", RegexOption.IGNORE_CASE)
    private val LOCAL = Regex("^localhost(:\\d+)?(/\\S*)?$", RegexOption.IGNORE_CASE)

    /** The address to open if [text] looks like one ("github.com/kuscher", "localhost:3000"), else null. */
    fun url(text: String): String? {
        val t = text.trim()
        return when {
            t.isEmpty() || t.any { it.isWhitespace() } -> null
            SCHEME.matches(t) -> t
            LOCAL.matches(t) -> "http://$t"
            DOMAIN.matches(t) -> "https://$t"
            else -> null
        }
    }
}
