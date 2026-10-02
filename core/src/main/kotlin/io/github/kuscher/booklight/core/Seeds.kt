package io.github.kuscher.booklight.core

/** A ready-made prompt, as one language's resources give it: `keyword|name|text`. */
data class Seed(val keyword: String, val name: String, val text: String)

/**
 * A kept prompt, as far as [Seeds] needs it. [seed] is the ready-made prompt it still is, by its
 * place in the table; null for one of the user's own, and once the user has changed a ready-made one.
 */
data class Kept(val id: String, val name: String, val keyword: String, val text: String, val seed: Int? = null)

/**
 * The ready-made prompts are kept by reference: the settings remember which one an entry is, and
 * its name, keyword and text come from the resources until the user edits it. So a change of the
 * device's language reaches them, a later version can reword one, and a new one is added once.
 */
object Seeds {
    /** The places of two of them in the table: their rows know more than their text says. */
    const val TRANSLATION = 2
    const val SUMMARY = 3

    fun parse(lines: List<String>): List<Seed> = lines.mapNotNull { line ->
        line.split('|', limit = 3).takeIf { it.size == 3 }?.let { (keyword, name, text) -> Seed(keyword, name, text) }
    }

    /** What a new installation starts with. A keyword that is [taken] (a link's, a recipe's) stays theirs: that prompt comes without one. */
    fun fresh(seeds: List<Seed>, taken: Set<String>): List<Kept> =
        seeds.mapIndexed { i, s -> Kept("p-${s.keyword}", s.name, if (s.keyword.lowercase() in taken) "" else s.keyword, s.text, i) }

    /**
     * Which ready-made prompt [k] still is, word for word, in any language's table: for settings
     * written before prompts were kept by reference. The keyword may have been left out when it
     * was taken; any other change makes it the user's own.
     */
    fun of(k: Kept, tables: List<List<Seed>>): Int? {
        if (!k.id.startsWith("p-")) return null
        for (table in tables) table.forEachIndexed { i, s ->
            if (s.name == k.name && s.text == k.text && (k.keyword.isEmpty() || k.keyword == s.keyword)) return i
        }
        return null
    }

    /**
     * The kept prompts as they are now: each unchanged ready-made one in the words of [seeds] (this
     * language's table), and the ready-made ones from place [known] on added at the end (a version
     * that brings a new one). One that had no keyword stays without; a keyword somebody else has
     * ([taken], or another prompt) is left out.
     */
    fun current(kept: List<Kept>, seeds: List<Seed>, known: Int, taken: Set<String>): List<Kept> {
        val used = taken.mapTo(HashSet()) { it.lowercase() }
        kept.filter { it.seed == null || it.seed !in seeds.indices }.forEach { if (it.keyword.isNotEmpty()) used += it.keyword.lowercase() }
        fun keyword(s: Seed) = s.keyword.takeIf { used.add(it.lowercase()) }.orEmpty()
        val now = kept.map { k ->
            val s = k.seed?.let(seeds::getOrNull) ?: return@map k
            k.copy(name = s.name, text = s.text, keyword = if (k.keyword.isEmpty()) "" else keyword(s))
        }
        val added = seeds.drop(known.coerceAtLeast(0)).mapIndexed { i, s -> Kept("p-${s.keyword}", s.name, keyword(s), s.text, known + i) }
            .filter { a -> now.none { it.id == a.id } }
        return now + added
    }
}
