package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PromptsTest {
    @Test fun theTextGoesWhereThePromptSays() {
        assertEquals("Translate to German: good morning", Prompts.fill("Translate to German: {text}", "good morning"))
        assertEquals("Say “hi” twice: hi", Prompts.fill("Say “{text}” twice: {text}", "hi"))
    }

    @Test fun orAfterIt() {
        assertEquals("Fix the grammar.\n\ntheir going", Prompts.fill("Fix the grammar.  ", "their going"))
        assertEquals("just this", Prompts.fill("  ", "just this"))
    }

    @Test fun aPlaceholderInsideTheTextStaysText() {
        assertEquals("Fix: write {text} here {text}", Prompts.fill("Fix: {text}", "write {text} here {text}"))
    }

    @Test fun oddText() {
        assertEquals("P: ", Prompts.fill("P: {text}", ""))
        assertEquals("P: 🎉\n$", Prompts.fill("P: {text}", "🎉\n$"))
    }
}
