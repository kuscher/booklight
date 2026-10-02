package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LettersTest {
    private val en = Letters(german = false)
    private val de = Letters(german = true)
    private fun f(text: String, l: Letters = en) = l.find(text, 70).joinToString(" ") { it.glyph }

    @Test fun aLanguageGivesItsLettersSmallThenCapital() {
        assertEquals("ä ö ü ß „ “ Ä Ö Ü ẞ", f("german"))
        assertEquals("æ ø å Æ Ø Å", f("danish"))
        assertEquals("å ä ö Å Ä Ö", f("swedish"))
        assertEquals("ą ć ę ł ń ó ś ź ż Ą Ć Ę Ł Ń Ó Ś Ź Ż", f("polish"))
    }

    @Test fun theStartOfANameIsEnoughInEitherLanguage() {
        assertEquals(f("german"), f("ger"))
        assertEquals(f("german"), f("deutsch"))
        assertEquals(f("danish"), f("dansk"))
        assertEquals(f("danish"), f("dänisch"))
        assertEquals(f("french"), f("franz"))
    }

    @Test fun aPlainLetterGivesWhatIsMadeFromIt() {
        assertEquals("ß ś š ş ș ẞ Ś Š Ş Ș", f("s"))
        assertTrue(f("a").startsWith("ä æ å à â á ã ą ă"))
        assertEquals("ñ ń ň Ñ Ń Ň", f("n"))
    }

    @Test fun twoLettersAreWhatItIsWrittenWith() {
        assertEquals("ß ẞ", f("ss"))
        assertEquals("ä æ Ä Æ", f("ae"))
        assertEquals("ö ø œ Ö Ø Œ", f("oe"))
        assertEquals("å Å", f("aa"))
    }

    @Test fun aMarkByName() {
        assertEquals("ä ö ü ë ï ÿ Ä Ö Ü Ë Ï Ÿ", f("umlaut"))
        assertEquals("ø ł Ø Ł", f("slash"))
        assertEquals("ñ ã õ Ñ Ã Õ", f("tilde"))
        assertEquals("ß ẞ", f("eszett"))
    }

    @Test fun everyWordMustFit() {
        assertEquals("ä Ä", f("a umlaut"))
        assertEquals("ø Ø", f("o slash"))
        assertEquals("é É", f("e acute"))
        assertEquals("ö Ö", f("swedish o"))
    }

    @Test fun capitals() {
        assertTrue(f("A").startsWith("Ä Æ Å À"))
        assertEquals("Ä Æ", f("AE"))
        assertEquals("Ä Ö Ü ẞ", f("german capital"))
        assertEquals("Ä Ö Ü ẞ", f("deutsch groß"))
    }

    @Test fun theLetterItselfGivesItAndItsOtherCase() {
        assertEquals("ß ẞ", f("ß"))
        assertEquals("Ø ø", f("Ø"))
        assertEquals("ı", f("ı"))
    }

    @Test fun nothingTypedIsEverySmallLetterGermanFirst() {
        val all = en.first(70).map { it.glyph }
        assertEquals(listOf("ä", "ö", "ü", "ß", "æ", "ø", "å"), all.take(7))
        assertTrue(all.none { it.any(Char::isUpperCase) && it != "İ" })
        assertEquals(all.size, all.distinct().size)
        assertEquals(all, en.find("  ", 70).map { it.glyph })
    }

    @Test fun namesInTheUsersLanguage() {
        assertEquals("a umlaut", en.named("ä")?.name)
        assertEquals("A umlaut", en.named("Ä")?.name)
        assertEquals("a Umlaut", de.named("ä")?.name)
        assertEquals("sharp s", en.named("ß")?.name)
        assertEquals("scharfes S", de.named("ß")?.name)
        assertEquals("o slash", en.named("ø")?.name)
    }

    @Test fun whatIsNotALetterFindsNothing() {
        assertEquals("", f("zzz"))
        assertEquals("", f("?"))
        assertEquals("", f("x"))
    }

    @Test fun marksOfALanguageAreFoundByWhatTheyAre() {
        assertEquals("„ “ « »", f("quote"))
        assertEquals("¿", f("question"))
    }
}
