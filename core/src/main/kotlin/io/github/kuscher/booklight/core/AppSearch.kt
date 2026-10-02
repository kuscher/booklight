package io.github.kuscher.booklight.core

/**
 * A search inside an app: "spotify daft punk" lands on Spotify's results for "daft punk".
 *
 * Where an app's search goes is one of three things, in this order for one app: a keyword of the app's
 * own Booklight file that takes text (docs/EXTENSIONS.md), a line of the bundled table, a search
 * activity the app declares for everyone. This reads the table, chooses, splits what was typed and
 * writes the address. Whether the installed app takes a way is the device's to say (`AppCommands.safe`).
 */
object AppSearch {
    /** Where an app's search was found, in their order of precedence. */
    enum class Source { FILE, TABLE, DECLARED }

    /**
     * One line of the bundled table: [address] is sent to [pkg], with `{argument}` where the typed text
     * goes. [link]: the keyword of the one of Booklight's own links that searches the same place (`yt`).
     */
    data class Line(val pkg: String, val address: String, val link: String? = null)

    /** One way to read a text as an app's name and what to look for there: [name] folded, [text] as typed. */
    data class Typed(val name: String, val text: String)

    const val ARGUMENT = "{argument}"

    /**
     * The table, in its own order: a line is the package, a tab, the address, and for an app one of
     * Booklight's links stands for, a tab and that link's keyword. A line that starts with `#` is a
     * comment. A line that is not all of that is skipped, as is an address that takes no text or
     * could say more than where to go (`intent:`).
     */
    fun table(lines: Sequence<String>): List<Line> = lines.mapNotNull { raw ->
        if (raw.trimStart().startsWith("#")) return@mapNotNull null
        val f = raw.split('\t').map { it.trim() }
        val pkg = f[0]
        val address = f.getOrNull(1).orEmpty()
        if (!PACKAGE.matches(pkg) || ARGUMENT !in address) return@mapNotNull null
        val scheme = SCHEME.find(address)?.groupValues?.get(1)?.lowercase() ?: return@mapNotNull null
        if (scheme in REFUSED) return@mapNotNull null
        Line(pkg, address, f.getOrNull(2)?.takeIf { it.isNotEmpty() })
    }.toList()

    /**
     * The way to use for one app: the first that the installed app takes ([taken] answers with what
     * to keep of it), the app's own file before the table before what it declares, and within one
     * source in the order given. Null when it takes none.
     */
    fun <T, R : Any> choose(ways: List<Pair<Source, T>>, taken: (T) -> R?): Pair<Source, R>? {
        for (source in Source.entries) for ((s, way) in ways) if (s == source) taken(way)?.let { return source to it }
        return null
    }

    /**
     * Every way to read [text] as a name and what to look for: the first words are the name, the rest
     * the text, the longest name first ("youtube music daft punk" is YouTube Music before it is
     * YouTube). A name is [WIDEST] words at most, and something has to follow it. Like [Verbs], this
     * only splits: whether the name is an app's, and whether the whole text is a better name
     * ("google maps"), is the caller's question.
     */
    fun readings(text: String): List<Typed> {
        val words = Words.of(text)
        val n = words.size
        val out = ArrayList<Typed>()
        for (k in minOf(WIDEST, n - 1) downTo 1) {
            // A word with no letters ("-") is no part of a name.
            if ((0 until k).any { Matcher.fold(words[it].text).isEmpty() }) continue
            out.add(Typed(Matcher.fold(Words.cut(text, words, 0, k)), Words.cut(text, words, k, n)))
        }
        return out
    }

    /** [template] with [text] where it says `{argument}`, escaped so that it stays the text wherever it stands. */
    fun address(template: String, text: String): String = template.replace(ARGUMENT, escape(text))

    /**
     * [text] for a place in an address: UTF-8, and everything but letters, digits and `-._*` as `%XX`.
     * A space is `%20`, which is right in a path (`…/search/daft%20punk`) as well as after a `?`,
     * where a plus would do too; `&`, `#`, `/` and `:` cannot end the text's place.
     */
    fun escape(text: String): String = Templates.encode(text)

    /**
     * The package of the app that searches where [site] does, if [site] is one of Booklight's own
     * links as it came (`yt`, `maps`, `store`, `drive`). A link the user changed is theirs: it goes
     * where it says.
     */
    fun owner(site: Site, table: List<Line>): String? {
        if (Sites.defaults.none { it.keyword == site.keyword && it.url == site.url }) return null
        return table.firstOrNull { it.link == site.keyword }?.pkg
    }

    /** An app's name is four words at most: "Google Play Store" and one to spare. */
    private const val WIDEST = 4
    private val PACKAGE = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+""")
    private val SCHEME = Regex("""^([A-Za-z][A-Za-z0-9+.\-]*):""")
    private val REFUSED = setOf("intent", "javascript", "file", "content", "data")
}
