package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SchemesTest {
    @Test fun theSchemeIsWhatStandsBeforeTheFirstColon() {
        assertEquals("https", Schemes.of("https://example.com"))
        assertEquals("spotify", Schemes.of("spotify:track:4uLU6hMCjMI75M1A2tKUQC"))
        assertEquals("spotify", Schemes.of("Spotify:search:{argument}"))
        assertEquals("mailto", Schemes.of("mailto:anna@example.com"))
        assertEquals("x-app.one+two", Schemes.of("x-app.one+two:thing"))
        assertNull(Schemes.of("example.com"))
        assertNull(Schemes.of("example.com/a:b"))
        assertNull(Schemes.of(":3000"))
        assertNull(Schemes.of("12:30"))
        assertNull(Schemes.of("{argument}:x"))
        assertNull(Schemes.of("a b:c"))
        assertNull(Schemes.of(""))
    }

    @Test fun aLinkMayHaveAnyScheme() {
        assertNull(Schemes.refused("https://example.com/browse/{argument}"))
        assertNull(Schemes.refused("http://localhost:3000"))
        assertNull(Schemes.refused("spotify:search:{argument}"))
        assertNull(Schemes.refused("spotify:playlist:37i9dQZF1DX8NTLI2TtZa6"))
        assertNull(Schemes.refused("slack://channel?team=T1&id=C1"))
        assertNull(Schemes.refused("mailto:anna@example.com"))
        assertNull(Schemes.refused("market://search?q=%s&c=apps"))
        assertNull(Schemes.refused("tel:+491712345678"))
    }

    @Test fun someSchemesAreNeverALink() {
        for (address in listOf("javascript:alert(1)", "file:///sdcard/a.txt", "content://media/external/1", "intent:#Intent;action=x;end", "data:text/html,hi", "JavaScript:alert(1)", "FILE:///a"))
            assertEquals(address, Schemes.Why.REFUSED, Schemes.refused(address))
    }

    @Test fun withoutASchemeItIsNoAddress() {
        for (address in listOf("example.com", "www.example.com/a", "{argument}", "%s", "", "//example.com", "spotify search", "12:30", "{argument}:x"))
            assertEquals(address, Schemes.Why.NONE, Schemes.refused(address))
    }

    @Test fun aSchemeAloneIsNotFinished() {
        assertEquals(Schemes.Why.EMPTY, Schemes.refused("https://"))
        assertEquals(Schemes.Why.EMPTY, Schemes.refused("http:"))
        assertEquals(Schemes.Why.EMPTY, Schemes.refused("spotify:"))
        assertNull(Schemes.refused("https://a"))
        assertNull(Schemes.refused("spotify:x"))
    }

    @Test fun theSchemeIsKeptInSmallLetters() {
        assertEquals("spotify:Track:AbC", Schemes.tidy("Spotify:Track:AbC"))
        assertEquals("https://Example.com/A", Schemes.tidy("HTTPS://Example.com/A"))
        assertEquals("no scheme here", Schemes.tidy("no scheme here"))
    }

    @Test fun aTypedAddressWithAnAppsOwnScheme() {
        assertEquals("spotify:track:4uLU6hMCjMI75M1A2tKUQC", Schemes.typed("spotify:track:4uLU6hMCjMI75M1A2tKUQC"))
        assertEquals("spotify:track:4uLU6hMCjMI75M1A2tKUQC", Schemes.typed("  spotify:track:4uLU6hMCjMI75M1A2tKUQC\n"))
        assertEquals("spotify:search:daft%20punk", Schemes.typed("Spotify:search:daft%20punk"))
        assertEquals("mailto:anna@example.com", Schemes.typed("mailto:anna@example.com"))
        assertEquals("tel:+491712345678", Schemes.typed("tel:+491712345678"))
        assertEquals("market://details?id=com.example", Schemes.typed("market://details?id=com.example"))
        assertEquals("slack://channel?team=T1&id=C1", Schemes.typed("slack://channel?team=T1&id=C1"))
    }

    @Test fun wordsWithAColonStayWords() {
        for (text in listOf(
            "note: milk", "note:  milk", "12:30", "c:", "C:", "c:\\Users\\me", "a:b", "todo:", "re: lunch", "std::vector", "::", ":smile:", ":3000",
            "spotify:search:daft punk", "see spotify:track:1", "ratio 16:9", "16:9", "key:\tvalue", "",
        )) assertNull(text, Schemes.typed(text))
    }

    @Test fun theWebsAddressesAreNotAnAppsOwn() {
        for (text in listOf("https://example.com", "http://example.com/a", "HTTPS://example.com", "localhost:3000", "localhost:3000/a", "example.com:8080", "example.com:8080/path"))
            assertNull(text, Schemes.typed(text))
    }

    @Test fun refusedSchemesAreNotOfferedWhenTyped() {
        for (text in listOf("javascript:alert(1)", "file:///a", "content://a/b", "intent:#Intent;end", "data:text/plain,hi", "Intent:x#Intent;end"))
            assertNull(text, Schemes.typed(text))
    }

    @Test fun aVeryLongTextIsNotLookedAt() {
        assertNull(Schemes.typed("app:" + "x".repeat(3000)))
    }
}
