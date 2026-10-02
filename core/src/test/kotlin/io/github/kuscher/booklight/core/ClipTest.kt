package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // ---- a fresh copy, offered under the empty field

    private fun look(age: Long = 20_000, found: Map<Thing, Float>? = null, private: Boolean = false, own: Boolean = false, text: Boolean = true, looking: Boolean = false) =
        Look(text, age, private, own, found, looking)

    @Test fun aFreshCopyIsOffered() {
        assertEquals(Offer(Age.Seconds(20), emptyList(), false), Clip.offer(look()))
        assertEquals(Offer(Age.JustNow, listOf(Thing.LINK, Thing.DATE), false), Clip.offer(look(3_000, mapOf(Thing.DATE to 1f, Thing.LINK to 0.9f))))
    }

    @Test fun notWhenItIsOldPrivateOurOwnOrSwitchedOff() {
        assertNull(Clip.offer(look(120_001)))
        assertEquals(Age.Minute, Clip.offer(look(120_000))?.age)
        assertNull(Clip.offer(look(private = true)))
        assertNull(Clip.offer(look(own = true)))
        assertNull(Clip.offer(look(text = false)))
        assertNull(Clip.offer(look(), on = false))
    }

    @Test fun aClockSetBack() {
        // the copy's time is the wall clock's: a little in the future is "just now", a lot is not offered
        assertEquals(Age.JustNow, Clip.offer(look(-800))?.age)
        assertNull(Clip.offer(look(-60_000)))
    }

    @Test fun onlyWhatTheSystemIsSureOfAndInTheFixedOrder() {
        val found = mapOf(Thing.MAIL to 0.8f, Thing.PHONE to 0.49f, Thing.LINK to 0.5f, Thing.DATE to 0.99f)
        assertEquals(listOf(Thing.LINK, Thing.DATE, Thing.MAIL), Clip.offer(look(found = found))?.things)
    }

    @Test fun stillLooking() {
        assertEquals(Offer(Age.JustNow, emptyList(), true), Clip.offer(look(500, looking = true)))
    }

    @Test fun theAgeDoesNotTick() {
        assertEquals(Age.JustNow, Clip.age(0))
        assertEquals(Age.JustNow, Clip.age(9_999))
        assertEquals(Age.Seconds(10), Clip.age(10_000))
        assertEquals(Age.Seconds(20), Clip.age(29_999))
        assertEquals(Age.Seconds(50), Clip.age(59_999))
        assertEquals(Age.Minute, Clip.age(60_000))
        assertEquals(Age.Minute, Clip.age(119_000))
    }

    @Test fun twoAreNamedAndTheRestIsMore() {
        assertEquals(emptyList<Thing>() to false, Clip.named(emptyList()))
        assertEquals(listOf(Thing.LINK) to false, Clip.named(listOf(Thing.LINK)))
        assertEquals(listOf(Thing.LINK, Thing.DATE) to false, Clip.named(listOf(Thing.LINK, Thing.DATE)))
        assertEquals(listOf(Thing.LINK, Thing.DATE) to true, Clip.named(listOf(Thing.LINK, Thing.DATE, Thing.PHONE)))
    }

    @Test fun whichWayATranslationGoes() {
        // a text that is not in the app's language comes into it
        assertEquals("en", Clip.target("de", "en"))
        assertEquals("de", Clip.target("fr", "de-DE"))
        assertEquals("en", Clip.target("da", "en-GB"))
        // a text in the app's language goes into the other one: German in an English Booklight, English otherwise
        assertEquals("de", Clip.target("en", "en"))
        assertEquals("de", Clip.target("en-US", "en-GB"))
        assertEquals("en", Clip.target("de", "de"))
        assertEquals("en", Clip.target("fr", "fr"))
        // a language that could not be told: as if it were the app's own
        assertEquals("de", Clip.target(null, "en"))
        assertEquals("en", Clip.target(null, "de"))
    }

    @Test fun aTextInAnotherLanguage() {
        assertTrue(Clip.foreign("de", "en"))
        assertFalse(Clip.foreign("en-US", "en-GB"))
        assertFalse(Clip.foreign(null, "en"))
    }

    @Test fun aPostcodeIsNotThePhoneNumber() {
        assertEquals("+49 30 5550 1234", Clip.pick(Thing.PHONE, listOf("10117", "+49 30 5550 1234")))
        assertEquals(null, Clip.pick(Thing.PHONE, listOf("10117")))
        assertEquals("a.example", Clip.pick(Thing.LINK, listOf("a.example", "b.example")))
        assertEquals(null, Clip.pick(Thing.DATE, emptyList()))
    }
}
