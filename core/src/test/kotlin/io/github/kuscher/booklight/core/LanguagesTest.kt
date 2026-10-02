package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguagesTest {
    private fun tag(word: String) = Languages.find(word)?.tag

    @Test fun byAnyOfItsNames() {
        assertEquals("da", tag("danish"))
        assertEquals("da", tag("Dänisch"))
        assertEquals("da", tag("daenisch"))
        assertEquals("da", tag("danisch"))
        assertEquals("da", tag("dansk"))
        assertEquals("fr", tag("FRENCH"))
        assertEquals("fr", tag("francais"))
        assertEquals("es", tag("español"))
        assertEquals("de", tag("german"))
        assertEquals("de", tag("deutsch"))
        assertEquals("nb", tag("norwegian"))
        assertEquals("ja", tag("日本語"))
    }

    @Test fun byItsTag() {
        assertEquals("da", tag("da"))
        assertEquals("nb", tag("no"))
        assertEquals("nb", tag("nb"))
        assertEquals("en", tag("en"))
    }

    @Test fun byItsStartFromFourLettersOn() {
        assertEquals("es", tag("span"))
        assertEquals("pt", tag("portug"))
        assertNull(tag("spa"))
        assertNull(tag("dan"))
    }

    @Test fun notByAStartTwoShareOrAnotherWord() {
        assertNull(tag(""))
        assertNull(tag("  "))
        assertNull(tag("danger"))
        assertNull(tag("to"))
        assertNull(tag("it is"))
        assertNull(tag("english please"))
    }

    @Test fun theFirstWordOfATextAndTheRest() {
        assertEquals("da" to "see you on Saturday", Languages.leading("danish see you on Saturday")?.let { it.first.tag to it.second })
        assertEquals("fr" to "", Languages.leading("  french")?.let { it.first.tag to it.second })
        assertNull(Languages.leading("see you on Saturday"))
        assertNull(Languages.leading(""))
    }

    @Test fun aLanguageByItsTag() {
        assertEquals("German", Languages.of("de-DE")?.en)
        assertEquals("Norwegian", Languages.of("no")?.en)
        assertNull(Languages.of("xx"))
    }

    @Test fun theGermanNameAfterInsIsFound() {
        assertEquals("en", Languages.find("englische")?.tag)
        assertEquals("fr", Languages.find("französische")?.tag)
        assertEquals("fr", Languages.find("franzoesische")?.tag)
        // An ordinary word that ends in e is not made a language by it.
        assertNull(Languages.find("are"))
        assertNull(Languages.find("rue"))
    }
}
