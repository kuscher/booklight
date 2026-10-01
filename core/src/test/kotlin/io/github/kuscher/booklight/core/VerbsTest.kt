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
        assertEquals(listOf(Reading("chrome new", "window"), Reading("chrome", "window")), r("chrome new window"))
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
        assertEquals(one("VSCodeBook", "window"), r("VSCodeBook\tnew  window").takeLast(1))
    }

    @Test fun oddTextIsNoVerb() {
        assertEquals(emptyList<Reading>(), r("chrome -"))
        assertEquals(emptyList<Reading>(), r("chrome 🎉"))
        assertEquals(emptyList<Reading>(), r("🎉 🎉 🎉"))
        assertTrue(r("x ".repeat(20_000) + "info").single().rest.length > 30_000)
    }
}
