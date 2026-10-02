package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTest {
    private val day = 24L * 60 * 60 * 1000

    @Test fun latchingTiesTypedTextToThePick() {
        val h = History()
        h.record("c", "app:calendar", 0)
        h.record("c", "app:calendar", 1)
        assertTrue(h.boost("c", "app:calendar", 2) > h.boost("c", "app:chrome", 2))
        // Another text isn't latched, only lifted a little by general use.
        assertTrue(h.boost("ca", "app:calendar", 2) <= History.FRECENCY_MAX)
        assertTrue(h.boost("c", "app:calendar", 2) > History.FRECENCY_MAX)
    }

    @Test fun latchedWeakMatchBeatsAnExactOne() {
        val h = History()
        repeat(3) { h.record("crm", "app:chrome", it.toLong()) }
        assertTrue(Matcher.SCATTERED + h.boost("crm", "app:chrome", 10) > 1.0)
    }

    @Test fun picksFadeOverWeeks() {
        val h = History()
        h.record("x", "a", 0)
        val fresh = h.boost("x", "a", 0)
        val later = h.boost("x", "a", 56 * day)
        assertTrue(later < fresh / 2)
        assertEquals(0.5, h.weight("a", 28 * day), 1e-9)
    }

    @Test fun topIsMostUsedFirst() {
        val h = History()
        h.record("", "a", 0); h.record("", "b", 0); h.record("", "b", 1)
        assertEquals(listOf("b", "a"), h.top(5, 2))
    }

    @Test fun dataRoundTripsAndForgetRemoves() {
        val h = History()
        h.record("Chr", "a", 5)
        val copy = History(h.data())
        assertEquals(h.boost("chr", "a", 6), copy.boost("chr", "a", 6), 0.0)
        copy.forget("a")
        assertEquals(0.0, copy.boost("chr", "a", 6), 0.0)
    }

    @Test fun itemsAreWhatWasRunWithoutWhatWasTyped() {
        val h = History()
        h.record("chr", "app:chrome", 1_000)
        h.record("", "setting:wifi", 2_000)
        assertEquals(setOf("app:chrome", "setting:wifi"), h.items().keys)
        assertEquals(1.0, h.items().getValue("app:chrome").count, 0.0)
        // A copy: what is recorded later does not change it.
        val before = h.items()
        h.record("x", "app:x", 3_000)
        assertEquals(2, before.size)
    }
}
