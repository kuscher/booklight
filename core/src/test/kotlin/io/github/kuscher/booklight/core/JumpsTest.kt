package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JumpsTest {
    @Test fun aRepository() {
        assertEquals("https://github.com/kuscher/booklight", Jumps.github("kuscher/booklight"))
        assertEquals("https://github.com/kuscher/booklight", Jumps.github("  kuscher/booklight "))
        assertEquals("https://github.com/a.b-c_d/e.f", Jumps.github("a.b-c_d/e.f"))
        assertEquals("https://github.com/Kuscher/BookLight", Jumps.github("Kuscher/BookLight"))
    }

    @Test fun anIssue() {
        assertEquals("https://github.com/kuscher/booklight/issues/12", Jumps.github("kuscher/booklight#12"))
        assertNull(Jumps.github("kuscher/booklight#"))
        assertNull(Jumps.github("kuscher/booklight#abc"))
        assertNull(Jumps.github("kuscher/booklight#12a"))
        assertNull(Jumps.github("kuscher/booklight#" + "9".repeat(50)))
    }

    @Test fun aPathGoesThroughAsTyped() {
        assertEquals("https://github.com/kuscher/booklight/pull/3", Jumps.github("kuscher/booklight/pull/3"))
        assertEquals("https://github.com/kuscher/booklight/issues?q=is:open", Jumps.github("kuscher/booklight/issues?q=is:open"))
        assertEquals("https://github.com/kuscher/booklight/", Jumps.github("kuscher/booklight/"))
    }

    @Test fun notARepository() {
        for (s in listOf("", "  ", "booklight", "booklight#12", "kuscher/", "/booklight", "/", "kuscher//x", "kuscher/book light",
            "ku scher/booklight", "kuscher/b@d", "ku\$her/x", "../..", "./x", "kuscher/..", "kuscher/booklight?x", "kuscher/booklight\npull",
            "https://github.com/kuscher/booklight", "kuscher/bücher", "a/" + "b".repeat(5000))) assertNull(s, Jumps.github(s))
    }

    @Test fun aPort() {
        assertEquals("http://localhost:3000", Jumps.port(":3000"))
        assertEquals("http://localhost:3000/path?x=1", Jumps.port(":3000/path?x=1"))
        assertEquals("http://localhost:3000/", Jumps.port(":3000/"))
        assertEquals("http://localhost:8080", Jumps.port("  :8080 "))
        assertEquals("http://localhost:1", Jumps.port(":1"))
        assertEquals("http://localhost:65535", Jumps.port(":65535"))
        assertEquals("http://localhost:80", Jumps.port(":0080"))
    }

    @Test fun notAPort() {
        for (s in listOf("", ":", ":0", ":00000", ":65536", ":99999", ":123456", ":99999999999999999999", ":abc", "3000", "localhost:3000",
            ":3000 x", ":3000x", ":3000?x=1", ":3000/a b", "::3000", ":-1", ":３０００")) assertNull(s, Jumps.port(s))
    }
}
