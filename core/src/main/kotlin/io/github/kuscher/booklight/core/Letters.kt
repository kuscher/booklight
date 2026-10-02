package io.github.kuscher.booklight.core

import java.text.Normalizer

/** A letter this keyboard may not have: its glyph and what it is called ("a umlaut"). */
data class Letter(val glyph: String, val name: String)

/**
 * The letters of other languages, to pick and copy: `abc german`, `abc danish`, `abc a`, `abc ss`,
 * `abc umlaut`, `abc o slash`, `abc A`.
 *
 * What is typed is taken a word at a time, and a letter must fit every word: a language it is used
 * in ("german", "dansk"; the start of the name is enough), its plain letter ("a"), the two letters
 * it is written with where there is no such key ("ae", "ss"), or the name of its mark ("umlaut",
 * "acute", "slash"). Small letters come first, then their capitals; a capital typed ("A"), or
 * "capital", gives the capitals alone. The letter itself typed gives it and its other case.
 * Nothing typed: every small letter, in the order of the languages below.
 *
 * With [german] the names are German; the English words are understood as well.
 */
class Letters(private val german: Boolean) {
    private class Entry(val lower: Letter, val upper: Letter?, val base: String, val keys: List<String>)

    private val entries: List<Entry>

    init {
        val list = LinkedHashMap<String, Entry>()
        val langs = HashMap<String, ArrayList<String>>()
        for (l in LANGUAGES) for (g in glyphs(l.letters + l.marks)) langs.getOrPut(g) { ArrayList() }.addAll(l.names)
        // The letters of every language first, then the few marks some of them write with.
        for (g in LANGUAGES.flatMap { glyphs(it.letters) } + LANGUAGES.flatMap { glyphs(it.marks) }) {
            if (g in list) continue
            val special = SPECIAL[g]
            val nfd = Normalizer.normalize(g, Normalizer.Form.NFD)
            val mark = if (special == null && nfd.length == 2) MARKS[nfd[1]] else null
            val base = special?.base ?: nfd.substring(0, 1).lowercase()
            val name = when {
                special != null -> if (german) special.nameDe else special.name
                mark != null -> "${nfd[0]} ${if (german) mark.nameDe else mark.name}"
                else -> g
            }
            val keys = langs.getValue(g) + (special?.keys ?: emptyList()) + (mark?.let { listOf(it.name, it.nameDe) + it.keys } ?: emptyList()) + listOfNotNull(TWO[g])
            val up = if (g in UPPER) UPPER[g] else g.uppercase().takeIf { it != g && it.length == g.length }
            val upper = up?.let { Letter(it, if (mark != null) name.replaceFirstChar(Char::uppercaseChar) else name) }
            list[g] = Entry(Letter(g, name), upper, base, keys.map(Matcher::fold).distinct())
        }
        entries = list.values.toList()
    }

    /** The best [limit] for [text]; nothing typed, [first]. */
    fun find(text: String, limit: Int): List<Letter> {
        val raw = text.trim()
        if (raw.isEmpty()) return first(limit)
        if (limit <= 0) return emptyList()
        // The letter itself: it and its other case.
        entries.firstOrNull { it.lower.glyph == raw }?.let { return listOfNotNull(it.lower, it.upper).take(limit) }
        entries.firstOrNull { it.upper?.glyph == raw }?.let { return listOfNotNull(it.upper, it.lower).take(limit) }

        val words = Matcher.fold(raw).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        // "A", "AE": the capitals. Asked for in words ("capital a"): the same.
        val asked = words.filter { w -> CAPITAL.any { it.startsWith(w) && w.length >= 3 } }
        val rest = words - asked.toSet()
        val capitals = asked.isNotEmpty() || (raw.length <= 2 && raw.all { it.isUpperCase() })
        val fitting = entries.filter { e -> rest.all { w -> fits(e, w) } }
        // A language asked for: its letters in its own order (ą ć ę ł ń ó…), not the order the table met them in.
        val language = LANGUAGES.firstOrNull { l -> rest.any { w -> w.length >= 2 && l.names.any { Matcher.fold(it).startsWith(w) } } }
        val own = language?.let { it.letters + it.marks }
        val found = if (own == null) fitting else fitting.sortedBy { e -> own.indexOf(e.lower.glyph).let { if (it < 0) Int.MAX_VALUE else it } }
        return (if (capitals) found.mapNotNull { it.upper } else found.map { it.lower } + found.mapNotNull { it.upper }).take(limit)
    }

    /** One letter is the plain letter it is made from; more are the start of one of its words ("ae", "ger", "uml", "slash"). */
    private fun fits(e: Entry, word: String): Boolean =
        if (word.length == 1) e.base == word
        else e.keys.any { k -> k.startsWith(word) || (' ' in k && k.split(' ').any { it.startsWith(word) }) }

    /** What the grid shows before anything is typed: the small letters, in the languages' order. */
    fun first(limit: Int): List<Letter> = entries.asSequence().map { it.lower }.take(limit.coerceAtLeast(0)).toList()

    /** The entry for a glyph, small or capital: for showing the ones picked lately again. */
    fun named(glyph: String): Letter? = entries.firstNotNullOfOrNull { e -> e.lower.takeIf { it.glyph == glyph } ?: e.upper?.takeIf { it.glyph == glyph } }

    private fun glyphs(s: String): List<String> = s.map { it.toString() }

    private class Language(val names: List<String>, val letters: String, val marks: String = "")
    private class Mark(val name: String, val nameDe: String, val keys: List<String> = emptyList())
    private class Special(val base: String, val name: String, val nameDe: String, val keys: List<String>)

    private companion object {
        /** In the order the empty grid shows them. A name's start is enough: "ger", "dan". */
        val LANGUAGES = listOf(
            Language(listOf("german", "deutsch"), "äöüß", "„“"),
            Language(listOf("danish", "dänisch", "dansk"), "æøå"),
            Language(listOf("norwegian", "norwegisch", "norsk"), "æøå"),
            Language(listOf("swedish", "schwedisch", "svenska"), "åäö"),
            Language(listOf("french", "französisch", "français"), "éèêëàâçîïôœùûüÿæ", "«»"),
            Language(listOf("spanish", "spanisch", "español"), "áéíóúüñ", "¿¡"),
            Language(listOf("italian", "italienisch", "italiano"), "àèéìòù"),
            Language(listOf("portuguese", "portugiesisch", "português"), "áâãàçéêíóôõú"),
            Language(listOf("polish", "polnisch", "polski"), "ąćęłńóśźż"),
            Language(listOf("czech", "tschechisch", "česky"), "áčďéěíňóřšťúůýž"),
            Language(listOf("turkish", "türkisch", "türkçe"), "çğıİöşü"),
            Language(listOf("dutch", "niederländisch", "nederlands"), "éëïóöü"),
            Language(listOf("finnish", "finnisch", "suomi"), "äöå"),
            Language(listOf("hungarian", "ungarisch", "magyar"), "áéíóöőúüű"),
            Language(listOf("icelandic", "isländisch", "íslenska"), "áðéíóúýþæö"),
            Language(listOf("romanian", "rumänisch", "română"), "ăâîșț"),
        )

        /** The mark a letter carries, by its combining character. */
        val MARKS = mapOf(
            '\u0308' to Mark("umlaut", "Umlaut", listOf("diaeresis", "trema", "dots", "punkte")),
            '\u0301' to Mark("acute", "Akut", listOf("accent", "akzent")),
            '\u0300' to Mark("grave", "Gravis", listOf("accent", "akzent")),
            '\u0302' to Mark("circumflex", "Zirkumflex", listOf("accent", "akzent", "hat", "dach")),
            '\u0303' to Mark("tilde", "Tilde"),
            '\u030A' to Mark("ring", "Ring"),
            '\u0327' to Mark("cedilla", "Cedille"),
            '\u0328' to Mark("ogonek", "Ogonek"),
            '\u030C' to Mark("caron", "Hatschek", listOf("hacek")),
            '\u0307' to Mark("dot", "Punkt"),
            '\u030B' to Mark("double acute", "Doppelakut"),
            '\u0306' to Mark("breve", "Breve"),
            '\u0326' to Mark("comma", "Komma"),
        )

        /** Letters and marks that are not a plain letter with a mark on it. */
        val SPECIAL = mapOf(
            "ß" to Special("s", "sharp s", "scharfes S", listOf("ss", "sz", "eszett", "sharp s", "scharfes s")),
            "æ" to Special("a", "ae", "ae", listOf("ae", "ligature", "ligatur")),
            "ø" to Special("o", "o slash", "o mit Strich", listOf("oe", "slash", "stroke", "strich")),
            "œ" to Special("o", "oe", "oe", listOf("oe", "ligature", "ligatur")),
            "ł" to Special("l", "l stroke", "l mit Strich", listOf("slash", "stroke", "strich")),
            "ð" to Special("d", "eth", "Eth", listOf("dh", "eth")),
            "þ" to Special("t", "thorn", "Thorn", listOf("th", "thorn")),
            "ı" to Special("i", "dotless i", "i ohne Punkt", listOf("dotless", "ohne punkt")),
            "„" to Special("\"", "low quote", "Anführungszeichen unten", listOf("quote", "anführungszeichen", "gänsefüßchen")),
            "“" to Special("\"", "high quote", "Anführungszeichen oben", listOf("quote", "anführungszeichen", "gänsefüßchen")),
            "«" to Special("\"", "left guillemet", "Guillemet links", listOf("quote", "guillemet", "anführungszeichen")),
            "»" to Special("\"", "right guillemet", "Guillemet rechts", listOf("quote", "guillemet", "anführungszeichen")),
            "¿" to Special("?", "inverted question mark", "umgekehrtes Fragezeichen", listOf("question", "fragezeichen", "inverted", "umgekehrt")),
            "¡" to Special("!", "inverted exclamation mark", "umgekehrtes Ausrufezeichen", listOf("exclamation", "ausrufezeichen", "inverted", "umgekehrt")),
        )

        /** What a letter is written with on a keyboard that lacks it. */
        val TWO = mapOf("ä" to "ae", "ö" to "oe", "ü" to "ue", "å" to "aa")

        /** Capitals that `uppercase()` gets wrong: ß has one of its own, and ı's is the plain I. */
        val UPPER = mapOf("ß" to "ẞ", "ı" to null)

        val CAPITAL = listOf("capital", "capitals", "uppercase", "upper", "caps", "groß", "gross", "großbuchstaben")
    }
}
