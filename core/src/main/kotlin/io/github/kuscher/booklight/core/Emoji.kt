package io.github.kuscher.booklight.core

/** One cell of the grid: an emoji or, if [symbol], a plain symbol (→, ©). [name] and [words] are in the user's language. */
data class Emoji(val glyph: String, val symbol: Boolean, val name: String, val words: List<String>)

/**
 * The bundled table of emoji and symbols, searchable by name and keyword.
 *
 * A line is `glyph ⇥ e|s ⇥ name ⇥ words ⇥ German name ⇥ German words`, words separated by `|`; the
 * German columns may be missing. Blank lines and lines starting with `#` are skipped, except the
 * rows of the `#` glyphs themselves (a first column without a space, then e or s).
 *
 * With [german] the names and words shown are the German ones where the table has them, and a
 * search finds the English ones too.
 */
class EmojiIndex(lines: Sequence<String>, german: Boolean) {
    /** [names] and [keys] are folded: what a search compares against. */
    private class Entry(val emoji: Emoji, val names: List<String>, val keys: List<String>)

    private val entries: List<Entry>
    private val byGlyph: Map<String, Emoji>

    init {
        val list = ArrayList<Entry>()
        for (raw in lines) {
            val cols = raw.removePrefix("﻿").split('\t').map { it.trim() }
            if (cols.size < 3 || cols[0].isEmpty() || cols[2].isEmpty()) continue
            if (cols[0].startsWith("#") && cols[0].any { it.isWhitespace() }) continue
            val symbol = when (cols[1]) { "e" -> false; "s" -> true; else -> continue }
            val words = words(cols.getOrNull(3))
            val nameDe = cols.getOrNull(4).orEmpty()
            val wordsDe = words(cols.getOrNull(5))
            val emoji = if (german) Emoji(cols[0], symbol, nameDe.ifEmpty { cols[2] }, wordsDe.ifEmpty { words })
                else Emoji(cols[0], symbol, cols[2], words)
            val names = if (german) listOf(nameDe, cols[2]) else listOf(cols[2])
            val keys = if (german) wordsDe + words else words
            list.add(Entry(emoji, fold(names), fold(keys)))
        }
        entries = list
        byGlyph = HashMap<String, Emoji>().also { map -> for (e in list) map.putIfAbsent(e.emoji.glyph, e.emoji) }
    }

    /**
     * The best [limit] for [text] among the emoji, or among the symbols if [symbols]. Best first: the
     * name starts with the text, a later word of the name does, a keyword does, the name contains it;
     * within each, the table's order. Nothing typed: [first].
     */
    fun search(text: String, symbols: Boolean, limit: Int): List<Emoji> {
        if (text.isBlank()) return first(symbols, limit)
        if (limit <= 0) return emptyList()
        val q = Matcher.fold(text)
        // No letters at all ("→"): the glyph itself, if the table has it.
        if (q.isEmpty()) return listOfNotNull(find(text.trim())?.takeIf { it.symbol == symbols })
        val word = " $q"
        val ranks = List(4) { ArrayList<Emoji>() }
        for (e in entries) {
            if (e.emoji.symbol != symbols) continue
            val rank = when {
                e.names.any { it.startsWith(q) } -> 0
                e.names.any { word in it } -> 1
                e.keys.any { it.startsWith(q) } -> 2
                e.names.any { q in it } -> 3
                else -> continue
            }
            ranks[rank].add(e.emoji)
            if (ranks[0].size >= limit) break            // nothing later in the table can come before these
        }
        return ranks.flatten().take(limit)
    }

    /** The first [limit] of the table: what the grid shows before anything is typed. */
    fun first(symbols: Boolean, limit: Int): List<Emoji> {
        if (limit <= 0) return emptyList()
        return entries.asSequence().filter { it.emoji.symbol == symbols }.map { it.emoji }.take(limit).toList()
    }

    /** The entry for a glyph, for showing recent picks again. */
    fun find(glyph: String): Emoji? = byGlyph[glyph]

    private fun words(column: String?): List<String> =
        column.orEmpty().split('|').map { it.trim() }.filter { it.isNotEmpty() }

    private fun fold(texts: List<String>): List<String> = texts.map(Matcher::fold).filter { it.isNotEmpty() }.distinct()
}
