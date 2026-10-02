package io.github.kuscher.booklight.core

/** A language a text can be translated into: its tag, and its name in English, in German and in itself. */
data class Language(val tag: String, val en: String, val de: String, val own: String)

/** The languages `tr` knows, and the words they are asked for by. */
object Languages {
    val all: List<Language> = listOf(
        Language("en", "English", "Englisch", "English"),
        Language("de", "German", "Deutsch", "Deutsch"),
        Language("fr", "French", "Französisch", "français"),
        Language("es", "Spanish", "Spanisch", "español"),
        Language("it", "Italian", "Italienisch", "italiano"),
        Language("pt", "Portuguese", "Portugiesisch", "português"),
        Language("nl", "Dutch", "Niederländisch", "Nederlands"),
        Language("da", "Danish", "Dänisch", "dansk"),
        Language("sv", "Swedish", "Schwedisch", "svenska"),
        Language("nb", "Norwegian", "Norwegisch", "norsk"),
        Language("fi", "Finnish", "Finnisch", "suomi"),
        Language("pl", "Polish", "Polnisch", "polski"),
        Language("cs", "Czech", "Tschechisch", "čeština"),
        Language("hu", "Hungarian", "Ungarisch", "magyar"),
        Language("ro", "Romanian", "Rumänisch", "română"),
        Language("el", "Greek", "Griechisch", "ελληνικά"),
        Language("tr", "Turkish", "Türkisch", "Türkçe"),
        Language("uk", "Ukrainian", "Ukrainisch", "українська"),
        Language("ru", "Russian", "Russisch", "русский"),
        Language("ja", "Japanese", "Japanisch", "日本語"),
        Language("ko", "Korean", "Koreanisch", "한국어"),
        Language("zh", "Chinese", "Chinesisch", "中文"),
        Language("hi", "Hindi", "Hindi", "हिन्दी"),
        Language("ar", "Arabic", "Arabisch", "العربية"),
        Language("he", "Hebrew", "Hebräisch", "עברית"),
    )

    fun of(tag: String): Language? = tag.substringBefore('-').lowercase().let { t -> all.firstOrNull { it.tag == t || (t == "no" && it.tag == "nb") } }

    /**
     * The language [word] asks for: one of its three names in full, whatever the case and with or
     * without its marks ("danish", "Dänisch", "daenisch", "dansk"), the start of one from four
     * letters on ("span"), or its tag ("da"). Null for anything else, and for a start two languages share.
     */
    fun find(word: String): Language? {
        val w = bare(word)
        if (w.isEmpty()) return null
        all.firstOrNull { l -> names(l).any { it == w } || l.tag == w || (w == "no" && l.tag == "nb") }?.let { return it }
        if (w.length < 4) return null
        return all.filter { l -> names(l).any { it.startsWith(w) } }.singleOrNull()
    }

    /** The first word of [text] as a language, and the rest: `danish see you on Saturday`. Null if it does not start with one. */
    fun leading(text: String): Pair<Language, String>? {
        val t = text.trimStart()
        val first = t.substringBefore(' ')
        val l = find(first) ?: return null
        return l to t.substring(first.length).trimStart()
    }

    private fun names(l: Language): List<String> {
        val written = l.de.lowercase().replace("ä", "ae").replace("ö", "oe").replace("ü", "ue")
        // And the German name as it stands after "ins": "ins Englische", "ins Franzoesische".
        return listOf(bare(l.en), bare(l.de), bare(l.own), written, bare(l.de) + "e", written + "e")
    }

    private fun bare(s: String): String = java.text.Normalizer.normalize(s.trim().lowercase(), java.text.Normalizer.Form.NFD).filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
}
