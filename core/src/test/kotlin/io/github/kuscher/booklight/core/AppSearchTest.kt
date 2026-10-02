package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.AppSearch.Line
import io.github.kuscher.booklight.core.AppSearch.Source
import io.github.kuscher.booklight.core.AppSearch.Typed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URI
import java.net.URLDecoder

class AppSearchTest {
    private fun table(vararg lines: String) = AppSearch.table(lines.asSequence())

    @Test fun aLineIsThePackageATabAndTheAddress() {
        assertEquals(listOf(Line("com.spotify.music", "spotify:search:{argument}")), table("com.spotify.music\tspotify:search:{argument}"))
        assertEquals(listOf(Line("com.android.vending", "https://play.google.com/store/search?q={argument}&c=apps", "store")),
            table("com.android.vending\thttps://play.google.com/store/search?q={argument}&c=apps\tstore"))
        // Spaces around a field, and an empty third one, are nothing.
        assertEquals(listOf(Line("a.b", "x:{argument}")), table(" a.b \t x:{argument} \t "))
    }

    @Test fun commentsAndEmptyLinesAreSkipped() {
        assertEquals(listOf(Line("a.b", "x:{argument}"), Line("c.d", "https://example.com/?q={argument}#top")),
            table("# Seen working.", "", "a.b\tx:{argument}", "   # so was this", "\t", "c.d\thttps://example.com/?q={argument}#top"))
    }

    @Test fun aLineThatIsNotOneIsSkipped() {
        assertEquals(emptyList<Line>(), table(
            "com.spotify.music",                                        // no address
            "com.spotify.music spotify:search:{argument}",              // a space is not a tab
            "com.spotify.music\tspotify:search",                        // takes no text
            "com.spotify.music\t{argument}",                            // not an address
            "spotify\tspotify:search:{argument}",                       // not a package
            "com.spotify.music\tintent:#Intent;S.q={argument};end",     // says more than where to go
            "com.spotify.music\tjavascript:alert('{argument}')",
            "com.spotify.music\tFILE:///sdcard/{argument}",
        ))
    }

    @Test fun theOrderOfTheTableIsKept() {
        val t = table("a.b\tone:{argument}", "c.d\tx:{argument}", "a.b\ttwo:{argument}")
        assertEquals(listOf("one:{argument}", "two:{argument}"), t.filter { it.pkg == "a.b" }.map { it.address })
    }

    @Test fun theBundledTableReads() {
        // The asset itself: every line that is not a comment or empty is a line of the table.
        val file = generateSequence(File("").absoluteFile) { it.parentFile }.map { File(it, "app/src/main/assets/appsearch.tsv") }.first { it.exists() }
        val lines = file.readLines()
        val t = AppSearch.table(lines.asSequence())
        assertEquals(lines.count { it.isNotBlank() && !it.trimStart().startsWith("#") }, t.size)
        assertTrue(t.size >= 7)
        // Each of Booklight's own links that has an app names one, once.
        assertEquals(listOf("drive", "maps", "store", "yt"), t.mapNotNull { it.link }.distinct().sorted())
        for (link in t.mapNotNull { it.link }.distinct()) assertEquals(link, 1, t.filter { it.link == link }.map { it.pkg }.distinct().size)
        // Netflix takes the text as a part of the path: after a `?` it opens its search with an empty field.
        assertEquals(listOf("https://www.netflix.com/search/{argument}"), t.filter { it.pkg == "com.netflix.mediaclient" }.map { it.address })
        // Every address is one once the text is in it.
        for (line in t) URI(AppSearch.address(line.address, "daft punk & söhne #1"))
    }

    @Test fun anAppsOwnFileComesFirstThenTheTableThenWhatItDeclares() {
        val all = listOf(Source.DECLARED to "search", Source.TABLE to "link", Source.TABLE to "second link", Source.FILE to "keyword")
        assertEquals(Source.FILE to "KEYWORD", AppSearch.choose(all) { it.uppercase() })
        assertEquals(Source.TABLE to "link", AppSearch.choose(all) { it.takeIf { w -> w != "keyword" } })
        // The next line of the table when the installed app does not take the first.
        assertEquals(Source.TABLE to "second link", AppSearch.choose(all) { it.takeIf { w -> w.startsWith("second") || w == "search" } })
        assertEquals(Source.DECLARED to "search", AppSearch.choose(all) { it.takeIf { w -> w == "search" } })
        assertNull(AppSearch.choose(all) { null as String? })
        assertNull(AppSearch.choose(emptyList<Pair<Source, String>>()) { it })
    }

    @Test fun aNameAndWhatToLookFor() {
        assertEquals(listOf(Typed("spotify daft", "punk"), Typed("spotify", "daft punk")), AppSearch.readings("spotify daft punk"))
        assertEquals(listOf(Typed("netflix", "severance")), AppSearch.readings("netflix severance"))
        // The longest name first: YouTube Music before YouTube.
        assertEquals(listOf(Typed("youtube music daft", "punk"), Typed("youtube music", "daft punk"), Typed("youtube", "music daft punk")), AppSearch.readings("youtube music daft punk"))
    }

    @Test fun aNameAloneIsNoSearch() {
        for (alone in listOf("spotify", "spotify ", "  spotify", "", "   ")) assertEquals(alone, emptyList<Typed>(), AppSearch.readings(alone))
    }

    @Test fun theNameIsFoldedTheTextIsAsTyped() {
        assertEquals(listOf(Typed("spotify", "Daft  Punk")), AppSearch.readings("  Spotify   Daft  Punk ").takeLast(1))
        assertEquals(Typed("yt music", "Björk"), AppSearch.readings("YT Music Björk").first())
        assertEquals(Typed("zdf", "Übermorgen"), AppSearch.readings("ZDF Übermorgen").first())
        // A name is four words at most.
        assertEquals(4, AppSearch.readings("a b c d e f g").size)
        assertEquals(Typed("a b c d", "e f g"), AppSearch.readings("a b c d e f g").first())
        // A word with no letters is no part of a name, but it can be looked for.
        assertEquals(listOf(Typed("spotify", "- daft punk")), AppSearch.readings("spotify - daft punk").filter { it.name == "spotify" })
        assertEquals(listOf(Typed("spotify", "?")), AppSearch.readings("spotify ?"))
    }

    @Test fun theTextIsEscapedForAnAddress() {
        assertEquals("spotify:search:daft%20punk", AppSearch.address("spotify:search:{argument}", "daft punk"))
        assertEquals("https://www.netflix.com/search/m%C3%A4dchen%20%26%20jungs", AppSearch.address("https://www.netflix.com/search/{argument}", "mädchen & jungs"))
        assertEquals("https://play.google.com/store/search?q=c%2B%2B%20%231&c=apps", AppSearch.address("https://play.google.com/store/search?q={argument}&c=apps", "c++ #1"))
        assertEquals("geo:0,0?q=caf%C3%A9%2Fbar%3F", AppSearch.address("geo:0,0?q={argument}", "café/bar?"))
        assertEquals("x:100%25%20%7Bargument%7D", AppSearch.address("x:{argument}", "100% {argument}"))
        assertEquals("x:%F0%9F%8E%B7%20%C3%9F", AppSearch.address("x:{argument}", "🎷 ß"))
        assertEquals("x:a-b_c.d*e", AppSearch.address("x:{argument}", "a-b_c.d*e"))
    }

    @Test fun theTextComesBackOutOfTheAddressAsItWasTyped() {
        for (text in listOf("daft punk", "AC/DC", "tom & jerry", "#1 hits", "50% off?", "a+b=c", "Mötley Crüe", "名探偵コナン", "it's \"so\" <odd>", "a:b;c,d")) {
            val path = URI(AppSearch.address("https://example.com/search/{argument}", text))
            assertEquals(text, "/search/$text", path.path)
            assertNull(text, path.query); assertNull(text, path.fragment)
            val query = URI(AppSearch.address("https://example.com/search?q={argument}&c=apps", text))
            assertEquals(text, "q=${AppSearch.escape(text)}&c=apps", query.rawQuery)
            assertEquals(text, text, URLDecoder.decode(query.rawQuery.removePrefix("q=").removeSuffix("&c=apps"), "UTF-8"))
            assertNull(text, query.fragment)
            // A scheme of the app's own: everything after it is one piece.
            assertEquals(text, "search:$text", URI(AppSearch.address("spotify:search:{argument}", text)).schemeSpecificPart)
        }
    }

    @Test fun aLinkOfBooklightsOwnHasItsApp() {
        val t = table("com.google.android.youtube\thttps://www.youtube.com/results?search_query={argument}\tyt", "com.android.vending\tmarket://search?q={argument}\tstore", "com.spotify.music\tspotify:search:{argument}")
        val yt = Sites.defaults.first { it.keyword == "yt" }
        assertEquals("com.google.android.youtube", AppSearch.owner(yt, t))
        assertEquals("com.android.vending", AppSearch.owner(Sites.defaults.first { it.keyword == "store" }, t))
        // A link the user changed, or made, goes where it says.
        assertNull(AppSearch.owner(yt.copy(url = "https://invidious.example/search?q=%s"), t))
        assertNull(AppSearch.owner(Site("yt2", "YouTube", yt.url), t))
        assertNull(AppSearch.owner(Site("sp", "Spotify", "https://open.spotify.com/search/%s"), t))
        // One of Booklight's own that no line of the table stands for.
        assertNull(AppSearch.owner(Sites.defaults.first { it.keyword == "w" }, t))
    }
}
