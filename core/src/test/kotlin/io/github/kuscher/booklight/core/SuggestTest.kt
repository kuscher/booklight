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

    @Test fun neverAsksForSumsBeingTypedAddressesOrAnythingWithoutWords() {
        // A sum is not yet a sum while it is being typed, and must still stay on the device.
        for (t in listOf("1500*", "(4+5", "sqrt(", "=12+", "2 p", "12 * 3.", "150 + 20%", "3^", "5!", "10 mod"))
            assertFalse("sent: $t", Suggest.worthAsking(t))
        // Addresses of every kind, complete or not.
        for (t in listOf("192.168.1.1", "10.0.0.5:8080/admin", "alex@gmail.com", "https://exa", "http://", "localhost:3000", "github.com/kuscher", "me@"))
            assertFalse("sent: $t", Suggest.worthAsking(t))
        // No letters: nothing to suggest for.
        for (t in listOf("42", "2026-10-01", "+49 30 1234", "...", "  "))
            assertFalse("sent: $t", Suggest.worthAsking(t))
        // Ordinary words, digits included, are fine.
        for (t in listOf("weather berlin", "iphone 17 review", "c++ tutorial", "top 10 films", "3d printer"))
            assertTrue("not sent: $t", Suggest.worthAsking(t))
    }
}
