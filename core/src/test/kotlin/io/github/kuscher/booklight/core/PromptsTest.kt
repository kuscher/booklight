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

    @Test fun theOtherPlaces() {
        val tr = "Translate this text into {language}. Reply with only the translation.\n\n{text}"
        assertEquals("Translate this text into Danish. Reply with only the translation.\n\nsee you", Prompts.fill(tr, "see you", mapOf(Prompts.LANGUAGE to "Danish")))
        val ins = "{instruction}\n\nReply with only the result.\n\n{text}"
        assertEquals("make a list\n\nReply with only the result.\n\nmilk, eggs", Prompts.fill(ins, "milk, eggs", mapOf(Prompts.INSTRUCTION to "make a list")))
    }

    @Test fun aPlaceInsideWhatIsFilledInStaysText() {
        assertEquals("Into {text}: hi", Prompts.fill("Into {language}: {text}", "hi", mapOf(Prompts.LANGUAGE to "{text}")))
        assertEquals("Do {language}: {instruction}", Prompts.fill("Do {instruction}: {text}", "{instruction}", mapOf(Prompts.INSTRUCTION to "{language}")))
    }

    @Test fun anotherPlaceButNoneForTheText() {
        assertEquals("Into Danish.\n\nhi", Prompts.fill("Into {language}.", "hi", mapOf(Prompts.LANGUAGE to "Danish")))
        assertEquals("Braces {like} these stay.\n\nhi", Prompts.fill("Braces {like} these stay.", "hi"))
    }
}
