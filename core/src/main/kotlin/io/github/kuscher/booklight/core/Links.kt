package io.github.kuscher.booklight.core

/** A web address as a row shows it, and without what was added to it to follow whoever opens it. */
object Links {
    /** Parameters that only say where a click came from. `utm_…` is matched by its start. */
    private val TAILS = setOf(
        "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "msclkid", "yclid", "twclid", "ttclid", "igshid", "igsh", "si",
        "mc_cid", "mc_eid", "_hsenc", "_hsmi", "mkt_tok", "srsltid", "ref_src", "ref_url", "vero_id", "oly_anon_id", "oly_enc_id", "s_kwcid", "_ga", "_gl",
    )

    private val SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]*://")

    private fun tail(name: String): Boolean = name.lowercase().let { it.startsWith("utm_") || it in TAILS }

    /**
     * [url] without its tracking parameters, or null if it has none to take off. Everything else
     * stays as it was written: the order of the other parameters, the fragment, the case.
     */
    fun clean(url: String): String? {
        val hash = url.indexOf('#').let { if (it < 0) url.length else it }
        val q = url.indexOf('?')
        if (q < 0 || q > hash) return null
        val params = url.substring(q + 1, hash).split('&')
        val kept = params.filter { it.isNotEmpty() && !tail(it.substringBefore('=')) }
        if (kept.size == params.count { it.isNotEmpty() }) return null
        return url.substring(0, q) + (if (kept.isEmpty()) "" else "?" + kept.joinToString("&")) + url.substring(hash)
    }

    /** The address as a title: without `https://`, `www.` and a lone closing slash, and without its tracking tail. */
    fun shown(url: String): String {
        val u = clean(url) ?: url
        val bare = u.replaceFirst(SCHEME, "").removePrefix("www.")
        return if (bare.count { it == '/' } == 1 && bare.endsWith("/")) bare.dropLast(1) else bare
    }
}
