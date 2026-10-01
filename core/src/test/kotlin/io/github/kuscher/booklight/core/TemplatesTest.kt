package io.github.kuscher.booklight.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplatesTest {
    private val date = LocalDate.of(2026, 10, 1)
    private fun fill(link: String, argument: String = "", clipboard: String = "") = Templates.fill(link, argument, clipboard, date)

    @Test fun aLinkTakesAnArgumentWhenItHasAPlaceForOne() {
        assertTrue(Templates.takesArgument("https://www.youtube.com/results?search_query=%s"))
        assertTrue(Templates.takesArgument("https://jira.example.com/browse/{argument}"))
        assertTrue(Templates.takesArgument("https://example.com/?q={query}"))
        assertFalse(Templates.takesArgument("https://example.com/"))
        assertFalse(Templates.takesArgument("https://example.com/?u={clipboard}&d={date}"))
        assertFalse(Templates.takesArgument(""))
    }

    @Test fun theThreeNamesOfTheArgument() {
        assertEquals("https://www.youtube.com/results?search_query=lofi%20beats", fill("https://www.youtube.com/results?search_query=%s", "lofi beats"))
        assertEquals("https://jira.example.com/browse/BOOK-12", fill("https://jira.example.com/browse/{argument}", "BOOK-12"))
        assertEquals("https://example.com/?q=x&again=x", fill("https://example.com/?q={query}&again=%s", "x"))
    }

    @Test fun valuesAreEncodedWithSpacesAsPercent20() {
        assertEquals("q=caf%C3%A9%20%26%20bar", fill("q=%s", "café & bar"))
        assertEquals("q=a%2Bb", fill("q=%s", "a+b"))
        assertEquals("q=%F0%9F%8E%89", fill("q=%s", "🎉"))
        assertEquals("q=a%2Fb%3Fc%3Dd%23e", fill("q=%s", "a/b?c=d#e"))
    }

    @Test fun clipboardAndDate() {
        assertEquals("https://example.com/?u=https%3A%2F%2Fa.b%2Fc%20d&d=2026-10-01",
            fill("https://example.com/?u={clipboard}&d={date}", clipboard = "https://a.b/c d"))
        assertEquals("log/2026-10-01/2026-10-01", fill("log/{date}/{date}"))
    }

    @Test fun aPlaceholderInsideAValueStaysText() {
        assertEquals("q=%7Bclipboard%7D", fill("q=%s", argument = "{clipboard}", clipboard = "secret"))
        assertEquals("q=%25s&c=x", fill("q={clipboard}&c=%s", argument = "x", clipboard = "%s"))
    }

    @Test fun whatIsNotAPlaceholderStays() {
        assertEquals("https://example.com/a%20b?x=100%", fill("https://example.com/a%20b?x=100%", "ignored"))
        assertEquals("{other} {argument {Argument} %S", fill("{other} {argument {Argument} %S", "x"))
        assertEquals("", fill("", "x"))
        assertEquals("q=", fill("q=%s"))
    }
}
