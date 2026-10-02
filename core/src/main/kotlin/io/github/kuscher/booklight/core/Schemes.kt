package io.github.kuscher.booklight.core

/**
 * An address by its scheme: what stands before its first colon (`https`, `spotify`, `mailto`). A link of
 * the user's own may have any scheme: with an app's own (`spotify:playlist:…`) the link opens in that app
 * and no browser is involved. A few schemes are never taken, and text without one is not an address.
 */
object Schemes {
    /**
     * Never a link: these run a script, read a file of somebody's, or are a whole request to another app
     * (`intent:`), which is only ever put together in the editor of an app command.
     */
    val REFUSED = setOf("javascript", "file", "content", "intent", "data")

    private val WEB = setOf("http", "https")

    /** Why an address cannot be a link. */
    enum class Why {
        /** It has no scheme. */
        NONE,
        /** Its scheme is one of [REFUSED]. */
        REFUSED,
        /** Nothing follows the scheme. */
        EMPTY,
    }

    /**
     * The scheme of [address] in small letters, or null if it has none: a letter, then letters, digits,
     * `+`, `.` and `-`, then a colon.
     */
    fun of(address: String): String? {
        val colon = address.indexOf(':')
        if (colon <= 0 || !letter(address[0])) return null
        for (i in 1 until colon) if (!(letter(address[i]) || address[i] in '0'..'9' || address[i] in "+.-")) return null
        return address.substring(0, colon).lowercase()
    }

    /** Why [address] cannot be saved as a link of the user's own; null if it can. Placeholders may still be in it. */
    fun refused(address: String): Why? {
        val scheme = of(address) ?: return Why.NONE
        if (scheme in REFUSED) return Why.REFUSED
        return if (address.substring(scheme.length + 1).trimStart('/').isEmpty()) Why.EMPTY else null
    }

    /** [address] with its scheme in small letters, as apps declare theirs: `Spotify:track:1` is answered by nobody. */
    fun tidy(address: String): String = of(address)?.let { it + address.substring(it.length) } ?: address

    /**
     * What was typed or pasted, if it is an address with an app's own scheme (`spotify:track:…`,
     * `mailto:anna@example.com`); else null. Strict, because most text with a colon in it is no address:
     * no space anywhere (`note: milk`), a scheme of two letters or more that starts with a letter (`c:`,
     * `12:30`), something after it that does not start with another colon (`std::vector`), not one of
     * [REFUSED], and not a web address, with or without its `https://` (`localhost:3000`). Whether an
     * installed app answers it is the device's to say: without one there is no row.
     */
    fun typed(text: String): String? {
        val t = text.trim()
        if (t.length > MAX || t.any { it.isWhitespace() || it.isISOControl() }) return null
        val scheme = of(t) ?: return null
        if (scheme.length < 2 || scheme in WEB || scheme in REFUSED) return null
        val rest = t.substring(scheme.length + 1)
        if (rest.isEmpty() || rest[0] == ':' || Web.url(t) != null) return null
        return tidy(t)
    }

    private fun letter(c: Char) = c in 'a'..'z' || c in 'A'..'Z'

    /** Longer than any address a person types or copies. */
    private const val MAX = 2000
}
