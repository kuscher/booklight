package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SitesTest {
    @Test fun keywordThenText() {
        val (site, text) = Sites.parse("yt lofi beats")!!
        assertEquals("YouTube", site.name)
        assertEquals("lofi beats", text)
        assertEquals("https://www.youtube.com/results?search_query=lofi+beats", site.search(text))
        assertEquals("GitHub", Sites.parse("GH booklight")!!.first.name)
    }

    @Test fun aKeywordAloneIsOrdinaryText() {
        assertNull(Sites.parse("yt"))
        assertNull(Sites.parse("yt "))
        assertNull(Sites.parse("g"))
        assertNull(Sites.parse("youtube lofi"))
        assertNull(Sites.parse(""))
    }

    @Test fun enginesEncodeTheText() {
        assertEquals("https://www.google.com/search?q=caf%C3%A9+%26+bar", Engines.default.search("café & bar"))
        assertEquals("suggestqueries.google.com", Engines.default.suggestHost)
        assertEquals("google", Engines.byId("no such engine").id)
        assertEquals("duckduckgo.com", Engines.byId("duckduckgo").suggestHost)
    }
}
