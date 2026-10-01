package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerbsTest {
    private val verbs = listOf(
        Verb("open", listOf("open", "launch", "start", "run", "öffnen", "starten")),
        Verb("window", listOf("new window", "window", "new", "neues fenster", "fenster", "neu"), atStart = false),
        Verb("info", listOf("info", "app info", "app-info", "details")),
        Verb("store", listOf("store", "play", "update", "aktualisieren")),
        Verb("uninstall", listOf("uninstall", "remove", "delete", "deinstallieren", "entfernen", "löschen", "loeschen"), min = 3),
    )
    private fun r(text: String) = Verbs.readings(text, verbs)
    private fun one(rest: String, action: String) = listOf(Reading(rest, action))

    @Test fun aVerbAfterTheName() {
        assertEquals(one("chrome", "uninstall"), r("chrome uninstall"))
        assertEquals(one("chrome", "info"), r("chrome details"))
        assertEquals(one("chrome", "open"), r("chrome launch"))
    }

    @Test fun atTheEndTwoLettersAreEnough() {
        assertEquals(one("chrome", "info"), r("chrome inf"))
        assertEquals(one("chrome", "info"), r("chrome in"))
        assertEquals(emptyList<Reading>(), r("chrome i"))
        assertEquals(emptyList<Reading>(), r("chrome xy"))
    }

    @Test fun uninstallNeedsThree() {
        assertEquals(emptyList<Reading>(), r("chrome un"))
        assertEquals(one("chrome", "uninstall"), r("chrome uni"))
        assertEquals(one("chrome", "uninstall"), r("chrome rem"))
    }

    @Test fun aVerbOfTwoWords() {
        assertEquals(one("chrome", "window"), r("chrome new w"))
        assertEquals(one("chrome", "window"), r("chrome new"))
        assertEquals(one("chrome", "window"), r("chrome neues f"))
        assertEquals(one("chrome", "info"), r("chrome app-info"))
        // "new window" whole: the name may be "chrome" or "chrome new".
        assertEquals(listOf(Reading("chrome", "window"), Reading("chrome new", "window")), r("chrome new window"))
    }

    @Test fun atTheStartTheWholeWord() {
        assertEquals(one("chrome", "uninstall"), r("uninstall chrome"))
        assertEquals(one("ch", "uninstall"), r("uninstall ch"))
        assertEquals(one("chrome", "info"), r("app info chrome"))
        assertEquals(emptyList<Reading>(), r("unin chrome"))
        assertEquals(emptyList<Reading>(), r("uninstalls chrome"))
    }

    @Test fun someVerbsOnlyFollowTheName() {
        assertEquals(emptyList<Reading>(), r("new chrome"))
        assertEquals(emptyList<Reading>(), r("window chrome"))
    }

    @Test fun aVerbNeedsAnObject() {
        for (alone in listOf("info", "uninstall", "inf", "", "   ", "\t")) assertEquals(alone, emptyList<Reading>(), r(alone))
        assertEquals(emptyList<Reading>(), Verbs.readings("chrome info", emptyList()))
    }

    @Test fun caseAndAccentsDontMatter() {
        assertEquals(one("Chrome", "uninstall"), r("Chrome LÖSCHEN"))
        assertEquals(one("chrome", "uninstall"), r("chrome loschen"))
        assertEquals(one("chrome", "uninstall"), r("chrome loeschen"))
        assertEquals(one("chrome", "uninstall"), r("chrome Lösch"))
        assertEquals(one("Chrome", "open"), r("Öffnen Chrome"))
    }

    @Test fun everyReadingOnceEach() {
        assertEquals(setOf("open", "store"), r("google st").map { it.action }.toSet())
        assertEquals(2, r("google st").size)
        assertEquals(one("start", "open"), r("start start"))
        // The caller decides that "Play Store" the app beats both of these.
        assertEquals(listOf(Reading("play", "store"), Reading("store", "store")), r("play store"))
        assertEquals(one("table", "open"), r("open table"))
    }

    @Test fun theRestIsAsTyped() {
        assertEquals(one("Google  Chrome", "info"), r("  Google  Chrome info "))
        assertEquals(one("Google Chrome", "open"), r("open Google Chrome"))
        assertEquals(one("VSCodeBook", "window"), r("VSCodeBook\tnew  window").take(1))
    }

    @Test fun oddTextIsNoVerb() {
        assertEquals(emptyList<Reading>(), r("chrome -"))
        assertEquals(emptyList<Reading>(), r("chrome 🎉"))
        assertEquals(emptyList<Reading>(), r("🎉 🎉 🎉"))
        assertTrue(r("x ".repeat(20_000) + "info").single().rest.length > 30_000)
    }

    // ---- 2.0: places

    private val places = listOf(
        Verb("left", listOf("left", "left half"), atStart = false),
        Verb("right", listOf("right", "right half"), atStart = false),
        Verb("p:LEFT_THIRD", listOf("left third", "left 1/3"), atStart = false),
        Verb("p:LEFT_TWO_THIRDS", listOf("left two thirds", "left 2/3"), atStart = false),
        Verb("p:TOP_LEFT", listOf("top left"), atStart = false),
        Verb("p:FULL", listOf("full", "maximize", "maximise"), atStart = false),
    )
    private fun p(text: String) = Verbs.readings(text, places)

    @Test fun aVerbOfThreeWords() {
        assertEquals("p:LEFT_TWO_THIRDS", p("chrome left two thirds").first().action)
        assertEquals("chrome", p("chrome left two thirds").first().rest)
        assertEquals("p:LEFT_TWO_THIRDS", p("google chrome left two th").first().action)
        assertEquals("google chrome", p("google chrome left two th").first().rest)
    }

    @Test fun aWholeWordBeatsTheStartOfALongerOne() {
        assertEquals("left", p("chrome left").first().action)                  // Left half, not the start of "left third"
        assertEquals("p:LEFT_THIRD", p("chrome left t").first().action)
        assertEquals("p:LEFT_THIRD", p("chrome left third").first().action)
        assertEquals("p:TOP_LEFT", p("chrome top left").first().action)        // not "left" with a name of "chrome top"
        assertEquals("chrome", p("chrome top left").first().rest)
        assertEquals("p:FULL", p("code full").first().action)
    }

    @Test fun oneLetterIsNotAVerbButKeepsTheName() {
        assertEquals(emptyList<Reading>(), p("chrome t"))
        assertEquals("chrome", Verbs.dangling("chrome t", places))             // on the way to "top left"
        assertEquals("chrome", Verbs.dangling("chrome f", places))
        assertEquals("Google Chrome", Verbs.dangling("Google Chrome l", places))
        assertEquals(null, Verbs.dangling("chrome x", places))                 // no verb starts with x
        assertEquals(null, Verbs.dangling("chrome to", places))                // two letters: a reading, or nothing
        assertEquals(null, Verbs.dangling("t", places))
        assertEquals(null, Verbs.dangling("", places))
    }
}

