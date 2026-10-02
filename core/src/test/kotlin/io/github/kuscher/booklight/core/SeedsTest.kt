package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeedsTest {
    private val en = Seeds.parse(listOf("fix|Fix spelling|Fix it.\n\n{text}", "shorter|Shorter|Shorter.\n\n{text}", "de|In German|Into German.\n\n{text}"))
    private val de = Seeds.parse(listOf("fix|Korrigieren|Korrigiere.\n\n{text}", "kürzer|Kürzer|Kürzer.\n\n{text}", "en|Auf Englisch|Ins Englische.\n\n{text}"))

    @Test fun aLineIsKeywordNameAndText() {
        assertEquals(Seed("fix", "Fix spelling", "Fix it.\n\n{text}"), en[0])
        assertEquals(emptyList<Seed>(), Seeds.parse(listOf("no bars here")))
        // The text may hold a bar of its own.
        assertEquals("a|b", Seeds.parse(listOf("k|n|a|b"))[0].text)
    }

    @Test fun aNewInstallationHasThemAllByReference() {
        val kept = Seeds.fresh(en, taken = setOf("de"))
        assertEquals(listOf(0, 1, 2), kept.map { it.seed })
        assertEquals(listOf("fix", "shorter", ""), kept.map { it.keyword })       // "de" is a link of the user's
        assertEquals("p-de", kept[2].id)
    }

    @Test fun anUnchangedOneFromAnOlderVersionIsRecognisedInEitherLanguage() {
        assertEquals(1, Seeds.of(Kept("p-shorter", "Shorter", "shorter", "Shorter.\n\n{text}"), listOf(en, de)))
        assertEquals(2, Seeds.of(Kept("p-en", "Auf Englisch", "", "Ins Englische.\n\n{text}"), listOf(en, de)))
        // Changed in any way, or the user's own: not by reference.
        assertNull(Seeds.of(Kept("p-shorter", "Shorter", "shorter", "Much shorter.\n\n{text}"), listOf(en, de)))
        assertNull(Seeds.of(Kept("p-shorter", "Shorter", "s", "Shorter.\n\n{text}"), listOf(en, de)))
        assertNull(Seeds.of(Kept("p1700000000000", "Shorter", "shorter", "Shorter.\n\n{text}"), listOf(en, de)))
    }

    @Test fun aChangeOfLanguageReachesTheUnchangedOnes() {
        val kept = Seeds.fresh(en, emptySet()) + Kept("p1", "Mine", "mine", "Do this.\n\n{text}")
        val now = Seeds.current(kept, de, known = 3, taken = emptySet())
        assertEquals(listOf("Korrigieren", "Kürzer", "Auf Englisch", "Mine"), now.map { it.name })
        assertEquals(listOf("fix", "kürzer", "en", "mine"), now.map { it.keyword })
        assertEquals(listOf("p-fix", "p-shorter", "p-de", "p1"), now.map { it.id })     // what was learned for them stays theirs
    }

    @Test fun anEditedOneStaysAsItIs() {
        val kept = Seeds.fresh(en, emptySet()).map { if (it.seed == 1) it.copy(text = "My own shorter.\n\n{text}", seed = null) else it }
        assertEquals("My own shorter.\n\n{text}", Seeds.current(kept, de, 3, emptySet())[1].text)
    }

    @Test fun aKeywordSomebodyElseHasIsLeftOut() {
        val kept = Seeds.fresh(en, emptySet()) + Kept("p1", "Mine", "en", "Do this.\n\n{text}")
        val now = Seeds.current(kept, de, 3, taken = setOf("kürzer"))
        assertEquals(listOf("fix", "", "", "en"), now.map { it.keyword })
        // One that came without a keyword stays without.
        assertEquals("", Seeds.current(Seeds.fresh(en, setOf("fix")), en, 3, emptySet())[0].keyword)
    }

    @Test fun aNewOneIsAddedOnce() {
        val more = en + Seed("formal", "Formal", "Formal.\n\n{text}")
        val kept = Seeds.fresh(en, emptySet()).filter { it.seed != 1 }                 // the user removed Shorter
        val now = Seeds.current(kept, more, known = 3, taken = emptySet())
        assertEquals(listOf("Fix spelling", "In German", "Formal"), now.map { it.name })
        assertEquals(3, now.last().seed)
        // And not again once it is known.
        assertEquals(now, Seeds.current(now, more, known = 4, taken = emptySet()))
    }
}
