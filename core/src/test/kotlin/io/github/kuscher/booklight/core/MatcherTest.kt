package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatcherTest {
    private fun s(q: String, n: String) = Matcher.score(q, n)
    private fun best(q: String, vararg names: String) = names.maxByOrNull { s(q, it) }!!

    @Test fun exactBeatsPrefixBeatsWordBeatsInside() {
        assertEquals(1.0, s("chrome", "Chrome"), 0.0)
        assertTrue(s("chr", "Chrome") > s("chr", "Google Chrome"))
        assertTrue(s("chr", "Google Chrome") > s("hro", "Chrome"))
        assertTrue(s("hro", "Chrome") > s("crm", "Chrome"))
    }

    @Test fun noMatch() {
        assertEquals(0.0, s("xyz", "Chrome"), 0.0)
        assertEquals(0.0, s("", "Chrome"), 0.0)
        assertEquals(0.0, s("chromes", "Chrome"), 0.0)
    }

    @Test fun shorterNameWinsForTheSameText() {
        assertEquals("Chrome", best("chr", "Chrome Beta", "Chrome", "Chrome Canary"))
    }

    @Test fun initials() {
        assertTrue(s("gc", "Google Chrome") >= Matcher.INITIALS)
        assertTrue(s("vsc", "VSCodeBook") >= Matcher.INITIALS)
        assertTrue(s("pt", "PDF Toolbox") >= Matcher.INITIALS)
        assertTrue(s("ds", "Disco Sweeper") >= Matcher.INITIALS)
        // One word can't have initials; letters of a single word are a prefix or nothing.
        assertEquals(0.0, s("cr", "Chrome"), 0.0)
    }

    @Test fun camelCaseAndSeparatorsSplitWords() {
        assertEquals(listOf("vs", "code", "book"), Matcher.words("VSCodeBook"))
        assertEquals(listOf("studio", "snap"), Matcher.words("StudioSnap"))
        assertEquals(listOf("hear", "on", "link"), Matcher.words("HearOn Link"))
        assertEquals(listOf("pdf", "toolbox"), Matcher.words("PDF Toolbox"))
        assertTrue(s("snap", "StudioSnap") >= Matcher.WORD_PREFIX)
    }

    @Test fun accentsAndCaseDontMatter() {
        assertEquals(1.0, s("cafe", "Café"), 0.0)
        assertTrue(s("ubers", "Übersetzer") >= Matcher.PREFIX)
    }

    @Test fun everyWordOfTheQueryMustMatch() {
        assertTrue(s("google ch", "Google Chrome") >= Matcher.PREFIX)
        assertTrue(s("chrome google", "Google Chrome") > 0.0)
        assertEquals(0.0, s("google fire", "Google Chrome"), 0.0)
    }

    @Test fun scatteredLettersNeedThreeAndStayBelowRealMatches() {
        assertTrue(s("chrm", "Chrome") > 0.0)
        assertTrue(s("chrm", "Chrome") < Matcher.INSIDE)
        assertEquals(0.0, s("ce", "Chrome"), 0.0)
    }

    @Test fun keywordsOnlyMatchFromTheStartOfAWord() {
        assertTrue(Matcher.keyword("dark", "dark mode") > 0.0)
        assertTrue(Matcher.keyword("mode", "dark mode") > 0.0)
        assertTrue(Matcher.keyword("dark", "dark mode") < Matcher.score("dark", "Dark theme"))
        assertEquals(0.0, Matcher.keyword("chr", "charge"), 0.0)     // scattered letters
        assertEquals(0.0, Matcher.keyword("ark", "dark mode"), 0.0)  // inside a word
    }
}
