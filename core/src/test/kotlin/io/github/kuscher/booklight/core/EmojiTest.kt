package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmojiTest {
    private val table = """
        # glyph	kind	name	words	name de	words de
        😀	e	grinning face	smile|happy	grinsendes Gesicht	lachen|froh
        🎉	e	party popper	celebration|tada	Konfettibombe	feier|party
        ❤️	e	red heart	love	rotes Herz	liebe
        🥳	e	partying face	celebration|birthday	Partygesicht	feier|geburtstag
        💔	e	broken heart	sad
        →	s	rightwards arrow	arrow|right	Pfeil nach rechts	pfeil|rechts
        #	s	number sign	hash|pound
        #️⃣	e	keycap: #	hash

        a line that is not a row
        x	q	a kind that does not exist
        y	e
        # 😀	e	a row that was commented out
        ©	s	copyright
    """.trimIndent()
    private val en = EmojiIndex(table.lineSequence(), german = false)
    private val de = EmojiIndex(table.lineSequence(), german = true)
    private fun EmojiIndex.glyphs(text: String, symbols: Boolean = false, limit: Int = 70) = search(text, symbols, limit).map { it.glyph }

    @Test fun readsTheRowsAndSkipsTheRest() {
        assertEquals(listOf("😀", "🎉", "❤️", "🥳", "💔", "#️⃣"), en.first(false, 100).map { it.glyph })
        assertEquals(listOf("→", "#", "©"), en.first(true, 100).map { it.glyph })
        assertEquals(Emoji("🎉", false, "party popper", listOf("celebration", "tada")), en.find("🎉"))
        assertEquals(Emoji("©", true, "copyright", emptyList()), en.find("©"))
        assertNull(en.find("x"))
        assertNull(en.find("y"))
        assertNull(en.find("# 😀"))
        assertNull(en.find(""))
    }

    @Test fun nameStartFirstInTableOrder() {
        assertEquals(listOf("🎉", "🥳"), en.glyphs("party"))
        assertEquals(listOf("🎉", "🥳"), en.glyphs("PA"))
        assertEquals(listOf("🥳"), en.glyphs("partying f"))
    }

    @Test fun thenAWordOfTheNameThenAKeywordThenInside() {
        // heart is a later word of two names; happy and hash are keywords; nothing merely contains "h".
        assertEquals(listOf("❤️", "💔", "😀", "#️⃣"), en.glyphs("h"))
        assertEquals(listOf("🎉", "🥳"), en.glyphs("celeb"))
        assertEquals(listOf("🎉", "❤️", "🥳", "💔"), en.glyphs("art"))
        // "r": red (name), rightwards is a symbol, "grinning"/"party"/"broken" contain it.
        assertEquals(listOf("❤️", "😀", "🎉", "🥳", "💔"), en.glyphs("r"))
    }

    @Test fun emojiAndSymbolsAreSearchedApart() {
        assertEquals(listOf("→"), en.glyphs("arrow", symbols = true))
        assertEquals(emptyList<String>(), en.glyphs("arrow", symbols = false))
        assertEquals(listOf("#"), en.glyphs("hash", symbols = true))
        assertEquals(listOf("#️⃣"), en.glyphs("hash", symbols = false))
    }

    @Test fun theLimitHolds() {
        assertEquals(listOf("🎉"), en.glyphs("party", limit = 1))
        assertEquals(listOf("❤️", "💔"), en.glyphs("h", limit = 2))
        assertEquals(emptyList<String>(), en.glyphs("party", limit = 0))
        assertEquals(emptyList<String>(), en.glyphs("party", limit = -1))
        assertEquals(emptyList<Emoji>(), en.first(false, 0))
    }

    @Test fun nothingTypedIsTheStartOfTheTable() {
        assertEquals(listOf("😀", "🎉", "❤️"), en.glyphs("", limit = 3))
        assertEquals(listOf("😀", "🎉", "❤️"), en.glyphs("   ", limit = 3))
        assertEquals(listOf("→", "#"), en.glyphs("", symbols = true, limit = 2))
    }

    @Test fun germanNamesWhereTheTableHasThem() {
        assertEquals(Emoji("🎉", false, "Konfettibombe", listOf("feier", "party")), de.find("🎉"))
        assertEquals(Emoji("💔", false, "broken heart", listOf("sad")), de.find("💔"))
        assertEquals(listOf("🎉"), de.glyphs("konf"))
        assertEquals(listOf("❤️"), de.glyphs("herz"))
        assertEquals(listOf("🎉", "🥳"), de.glyphs("feier"))
        assertEquals(listOf("→"), de.glyphs("pfeil", symbols = true))
        assertEquals(emptyList<String>(), en.glyphs("konf"))
    }

    @Test fun inGermanTheEnglishWordsStillFind() {
        assertEquals(listOf("🎉", "🥳"), de.glyphs("party"))
        assertEquals(listOf("❤️"), de.glyphs("love"))
        assertEquals(listOf("❤️", "💔"), de.glyphs("heart"))
        assertEquals(listOf("😀"), de.glyphs("grin"))
    }

    @Test fun caseAndAccentsDontMatter() {
        assertEquals(listOf("🎉", "🥳"), en.glyphs("  Pârty "))
        assertEquals(listOf("😀"), de.glyphs("GRINSENDES"))
    }

    @Test fun aGlyphFindsItself() {
        assertEquals(listOf("→"), en.glyphs("→", symbols = true))
        assertEquals(emptyList<String>(), en.glyphs("→", symbols = false))
        assertEquals(emptyList<String>(), en.glyphs("!!!"))
    }

    @Test fun oddTables() {
        val empty = EmojiIndex(emptySequence(), german = true)
        assertEquals(emptyList<Emoji>(), empty.search("party", false, 10))
        assertEquals(emptyList<Emoji>(), empty.first(true, 10))
        val windows = EmojiIndex(sequenceOf("﻿🎉\te\tparty popper\ttada\r", "\t\t\t", "🎉\te\tsecond of the same"), german = false)
        assertEquals(listOf(Emoji("🎉", false, "party popper", listOf("tada"))), windows.search("party", false, 10))
        assertEquals("party popper", windows.find("🎉")!!.name)
    }
}
