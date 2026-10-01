package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipTest {
    private fun t(text: String) = Clip.transforms(text).associate { it.id to it.value }
    private fun ids(text: String) = Clip.transforms(text).map { it.id }

    @Test fun everyTransformInItsOrder() {
        val text = "hello  World\nand you"
        assertEquals(listOf("upper", "lower", "title", "line", "count", "url"), ids(text))
        val t = t(text)
        assertEquals("HELLO  WORLD\nAND YOU", t["upper"])
        assertEquals("hello  world\nand you", t["lower"])
        assertEquals("Hello  World\nAnd You", t["title"])
        assertEquals("hello World and you", t["line"])
        assertEquals("4 20", t["count"])
        assertEquals("hello%20%20World%0Aand%20you", t["url"])
    }

    @Test fun nothingForNothing() {
        assertEquals(emptyList<Transform>(), Clip.transforms(""))
        assertEquals(emptyList<Transform>(), Clip.transforms("  \n\t "))
    }

    @Test fun aTransformThatChangesNothingIsLeftOut() {
        assertEquals(listOf("lower", "title", "count"), ids("HELLO"))
        assertEquals(listOf("upper", "title", "count"), ids("hello"))
        assertEquals(listOf("upper", "lower", "count", "url"), ids("Hello World"))
        assertEquals(listOf("count"), ids("42"))
    }

    @Test fun titleCase() {
        assertEquals("Hello World-foo", t("hELLO wORLD-foo")["title"])
        assertEquals("Über Den Wolken", t("über den WOLKEN")["title"])
        assertEquals("1st Place", t("1st place")["title"])
    }

    @Test fun oneLine() {
        assertEquals("two lines here", t("  two\n lines \t here ")["line"])
        assertEquals("a b", t("a\r\nb")["line"])
    }

    @Test fun countIsWordsThenCharacters() {
        assertEquals("3 14", t("one two  three")["count"])
        assertEquals("1 1", t("a")["count"])
        assertEquals("2 4", t("🎉 ok")["count"])                    // an emoji is one character
        assertEquals("10000 49999", t("word ".repeat(10_000).trim())["count"])
    }

    @Test fun otherAlphabets() {
        assertEquals("STRASSE", t("straße")["upper"])
        assertEquals("ελληνικά", t("ΕΛΛΗΝΙΚΆ")["lower"])
        assertEquals("%E6%97%A5%E6%9C%AC", t("日本")["url"])
        assertEquals(listOf("count", "url"), ids("日本"))
    }
}
