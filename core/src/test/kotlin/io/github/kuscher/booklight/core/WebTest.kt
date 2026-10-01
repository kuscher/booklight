package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebTest {
    @Test fun addresses() {
        assertEquals("https://github.com/kuscher", Web.url("github.com/kuscher"))
        assertEquals("https://googlebook.studio", Web.url(" googlebook.studio "))
        assertEquals("http://localhost:3000/admin", Web.url("localhost:3000/admin"))
        assertEquals("http://example.com/a?b=c", Web.url("http://example.com/a?b=c"))
    }

    @Test fun wordsAndNumbersAreNotAddresses() {
        assertNull(Web.url("chrome"))
        assertNull(Web.url("weather berlin"))
        assertNull(Web.url("1.5"))
        assertNull(Web.url("3.14*2"))
        assertNull(Web.url("readme.m"))
        assertNull(Web.url(""))
    }
}
