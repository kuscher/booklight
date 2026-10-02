package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinksTest {
    @Test fun theTailComesOff() {
        assertEquals("https://example.com/table", Links.clean("https://example.com/table?utm_source=chat"))
        assertEquals("https://example.com/a?id=7", Links.clean("https://example.com/a?utm_source=x&id=7&utm_medium=mail"))
        assertEquals("https://example.com/a?id=7&b=2", Links.clean("https://example.com/a?id=7&fbclid=AbC&b=2"))
        assertEquals("https://youtu.be/dQw4w9WgXcQ?t=42", Links.clean("https://youtu.be/dQw4w9WgXcQ?si=abc123&t=42"))
        assertEquals("https://shop.example/p/1", Links.clean("https://shop.example/p/1?gclid=x&gbraid=y&srsltid=z"))
        assertEquals("https://example.com/", Links.clean("https://example.com/?UTM_Campaign=Spring"))
        assertEquals("https://example.com/n?page=2", Links.clean("https://example.com/n?page=2&mc_cid=1&mc_eid=2"))
        assertEquals("https://example.com/x", Links.clean("https://example.com/x?_hsenc=a&_hsmi=b&mkt_tok=c"))
        assertEquals("https://example.com/x", Links.clean("https://example.com/x?igshid=1&msclkid=2&twclid=3&ttclid=4&yclid=5&dclid=6&wbraid=7"))
    }

    @Test fun theFragmentStays() {
        assertEquals("https://example.com/doc#part-2", Links.clean("https://example.com/doc?utm_source=a#part-2"))
        assertEquals("https://example.com/doc?v=1#part-2", Links.clean("https://example.com/doc?v=1&utm_term=b#part-2"))
    }

    @Test fun nothingToTakeOff() {
        assertNull(Links.clean("https://example.com/table"))
        assertNull(Links.clean("https://example.com/a?id=7&b=2"))
        assertNull(Links.clean("https://example.com/a#utm_source=not-a-parameter"))
        assertNull(Links.clean("https://example.com/search?q=utm_source"))
        assertNull(Links.clean("https://example.com/a?reference=1&simple=2&site=3"))
        assertNull(Links.clean("not a link"))
        assertNull(Links.clean(""))
    }

    @Test fun oddOnes() {
        assertEquals("https://example.com/a", Links.clean("https://example.com/a?utm_source"))
        assertEquals("https://example.com/a?x=1", Links.clean("https://example.com/a?&x=1&&utm_id=3"))
        assertEquals("https://example.com/a?x=1=2", Links.clean("https://example.com/a?x=1=2&gclid=5"))
    }

    @Test fun asATitle() {
        assertEquals("example.com/table", Links.shown("https://example.com/table?utm_source=chat"))
        assertEquals("example.com", Links.shown("https://www.example.com/"))
        assertEquals("example.com/a/", Links.shown("http://example.com/a/"))
        assertEquals("example.com/a?id=7", Links.shown("https://example.com/a?id=7"))
        assertEquals("example.com", Links.shown("example.com"))
    }

    @Test fun onlyALeadingSchemeIsTakenOff() {
        assertEquals("example.com/login?next=https://example.com/home", Links.shown("https://www.example.com/login?next=https://example.com/home"))
        assertEquals("example.com/login?next=https://other.example/x", Links.shown("www.example.com/login?next=https://other.example/x"))
    }
}
