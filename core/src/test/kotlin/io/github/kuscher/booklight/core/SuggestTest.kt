package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestTest {
    @Test fun readsTheOpenSearchReply() {
        assertEquals(listOf("weather berlin", "weather tomorrow"), Suggest.parse("""["weather",["weather berlin","weather tomorrow"]]"""))
        assertEquals(listOf("a \"quoted\" one", "café"), Suggest.parse("""["q", ["a \"quoted\" one", "café"], [], {"x":1}]"""))
        assertEquals(listOf("a", "b"), Suggest.parse("""["q",["a","b","c"]]""", limit = 2))
    }

    @Test fun anythingElseIsNoSuggestions() {
        assertEquals(emptyList<String>(), Suggest.parse(""))
        assertEquals(emptyList<String>(), Suggest.parse("<html>Sign in</html>"))
        assertEquals(emptyList<String>(), Suggest.parse("""{"error":"quota"}"""))
        assertEquals(emptyList<String>(), Suggest.parse("""["q",["unterminated"""))
        assertEquals(emptyList<String>(), Suggest.parse("""["q",[]]"""))
        assertEquals(emptyList<String>(), Suggest.parse("""["q",[1,2]]"""))
    }

    @Test fun onlyAsksForTextBooklightDoesNotAnswerItself() {
        assertTrue(Suggest.worthAsking("weather"))
        assertFalse(Suggest.worthAsking("w"))
        assertFalse(Suggest.worthAsking("12*3"))
        assertFalse(Suggest.worthAsking("github.com/kuscher"))
        assertFalse(Suggest.worthAsking("x".repeat(100)))
    }
}
