package io.github.kuscher.booklight.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AskTest {
    @Test fun fiveWordsAreAQuestion() {
        assertTrue(Ask.first("best pizza near the office"))
        assertTrue(Ask.first("  one  two three\tfour five  "))
        assertFalse(Ask.first("google chrome beta now"))
    }

    @Test fun aQuestionMarkAtTheEnd() {
        assertTrue(Ask.first("weather?"))
        assertTrue(Ask.first("weather tomorrow? "))
        assertTrue(Ask.first("天気は？"))
        assertFalse(Ask.first("?"))
        assertFalse(Ask.first("? weather"))
    }

    @Test fun aQuestionWordFirstAndThreeWords() {
        assertTrue(Ask.first("what is booklight"))
        assertTrue(Ask.first("How does it"))
        assertTrue(Ask.first("what's the time"))
        assertTrue(Ask.first("explain monads simply"))
        assertFalse(Ask.first("what is"))
        assertFalse(Ask.first("what"))
        assertFalse(Ask.first("whatever you say"))
        assertFalse(Ask.first("play lofi beats"))
        assertFalse(Ask.first("new york weather"))
    }

    @Test fun germanQuestionWords() {
        assertTrue(Ask.first("wie spät ist"))
        assertTrue(Ask.first("Erkläre mir Quantenphysik"))
        assertTrue(Ask.first("erklare mir das"))
        assertTrue(Ask.first("Übersetze das bitte"))
        assertTrue(Ask.first("warum ist das"))
        assertFalse(Ask.first("wetter in berlin"))
    }

    @Test fun offeredFromTwoWords() {
        assertTrue(Ask.offered("new york"))
        assertTrue(Ask.offered(" a \t b "))
        assertFalse(Ask.offered("chrome"))
        assertFalse(Ask.offered("  chrome  "))
        assertFalse(Ask.offered(""))
    }

    @Test fun nothingAndALot() {
        for (s in listOf("", "   ", "\n")) { assertFalse(Ask.first(s)); assertFalse(Ask.offered(s)) }
        assertTrue(Ask.first("word ".repeat(100_000)))
        assertFalse(Ask.first("w".repeat(100_000)))
    }
}
