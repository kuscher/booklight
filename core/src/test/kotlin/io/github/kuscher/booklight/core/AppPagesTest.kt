package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An app's own pages ("spotify notifications") add four verbs to the ones an app's row already has,
 * and every verb more is one more word that may also be an app's name. These tests hold the words as
 * the string resources list them (the German lists, which keep the English words) and read a text
 * the way the app list does: the whole text is a name first, and only then a name and a verb.
 */
class AppPagesTest {
    private fun words(list: String) = list.split(',')

    // In the order the app list has them: what was there, the places, the pages, and what removes the app last.
    private val verbs = listOf(
        Verb("open", words("open,launch,start,öffnen,oeffnen,starten")),
        Verb("window", words("new window,window,neues fenster,fenster"), atStart = false),
        Verb("info", words("info,app info,details,app-info")),
        Verb("full", words("full,maximize,maximise,max,voll,vollbild,maximieren,ganzer bildschirm"), atStart = false),
        Verb("left", words("left,left half,links,linke hälfte"), atStart = false),
        Verb("right", words("right,right half,rechts,rechte hälfte"), atStart = false),
        Verb("pc", words("center,centre,middle,mitte,zentriert,zentrum"), atStart = false),
        Verb("notifications", words("notifications,notification,benachrichtigungen,benachrichtigung"), atStart = false),
        Verb("language", words("language,languages,sprache,sprachen"), atStart = false),
        Verb("defaults", words("defaults,default,open by default,standard,standards,standardmäßig öffnen"), atStart = false),
        Verb("battery", words("battery,battery use,akku,akkunutzung,batterie"), atStart = false),
        Verb("uninstall", words("uninstall,remove,delete,deinstallieren,entfernen,löschen,loeschen"), min = 3),
    )

    /**
     * What the app list makes of [text] with apps of these [names] installed: each app it finds, with
     * the action the text arms on its row (null: none, Enter opens it). The rule of `AppsProvider.query`,
     * repeated here because that class needs a device: a name found through a verb counts 0.95 of the
     * same name typed alone, and no verb is read when the whole text matches a name from the start of a word.
     */
    private fun find(text: String, vararg names: String): Map<String, String?> {
        val plain = names.map { Matcher.score(text, it) }
        val readings = if (plain.any { it >= Matcher.WORD_PREFIX }) emptyList() else Verbs.readings(text, verbs)
        val out = LinkedHashMap<String, String?>()
        names.forEachIndexed { i, name ->
            var best = plain[i]
            var verb: String? = null
            for (r in readings) {
                val s = Matcher.score(r.rest, name)
                if (s >= Matcher.WORD_PREFIX && s * 0.95 > best) { best = s * 0.95; verb = r.action }
            }
            if (best > 0) out[name] = verb
        }
        return out
    }

    @Test fun aPageAfterTheName() {
        assertEquals(mapOf("Spotify" to "notifications"), find("spotify notifications", "Spotify", "Chrome"))
        assertEquals(mapOf("Chrome" to "defaults"), find("chrome defaults", "Spotify", "Chrome"))
        assertEquals(mapOf("Chrome" to "defaults"), find("chrome open by default", "Spotify", "Chrome"))
        assertEquals(mapOf("Slack" to "language"), find("slack language", "Slack"))
        assertEquals(mapOf("Spotify" to "battery"), find("spotify battery", "Spotify"))
        assertEquals(mapOf("Google Chrome" to "battery"), find("google chrome battery use", "Google Chrome"))
    }

    @Test fun inGerman() {
        assertEquals(mapOf("Spotify" to "notifications"), find("spotify benachrichtigungen", "Spotify"))
        assertEquals(mapOf("Slack" to "language"), find("slack sprache", "Slack"))
        assertEquals(mapOf("Chrome" to "defaults"), find("chrome standard", "Chrome"))
        assertEquals(mapOf("Spotify" to "battery"), find("spotify akku", "Spotify"))
    }

    @Test fun theStartOfAPagesWord() {
        assertEquals(mapOf("Spotify" to "notifications"), find("spotify no", "Spotify"))
        assertEquals(mapOf("Spotify" to "battery"), find("spotify ba", "Spotify"))
        assertEquals(mapOf("Chrome" to "defaults"), find("chrome open by d", "Chrome"))
        // One letter is not a verb, but the row stays while the word is typed.
        assertEquals(emptyMap<String, String?>(), find("spotify n", "Spotify"))
        assertEquals("spotify", Verbs.dangling("spotify n", verbs))
    }

    @Test fun theWholeTextIsANameFirst() {
        // An app whose name holds a page's word is that app, with nothing armed, whatever else is installed.
        assertEquals(mapOf("Battery Guru" to null), find("battery guru", "Battery Guru", "Guru"))
        assertEquals(mapOf("Battery Guru" to null), find("guru battery", "Battery Guru", "Guru"))
        assertEquals(mapOf("Standard Notes" to null), find("standard notes", "Standard Notes", "Notes"))
        assertEquals(mapOf("Standard Notes" to null), find("notes standard", "Standard Notes", "Notes"))
        assertEquals(mapOf("Language Reactor" to null), find("language reactor", "Language Reactor", "Reactor"))
        assertEquals(mapOf("Akku Doktor" to null), find("doktor akku", "Akku Doktor", "Doktor"))
        assertEquals(mapOf("Notification History" to null), find("notification history", "Notification History", "History"))
        assertEquals(mapOf("Notification History" to null), find("history notif", "Notification History", "History"))
        // The start of such a name too: "battery g" is on its way to Battery Guru.
        assertEquals(mapOf("Battery Guru" to null), find("battery g", "Battery Guru", "Guru"))
        // Without that app the same words are a name and a page.
        assertEquals(mapOf("Guru" to "battery"), find("guru battery", "Guru"))
        assertEquals(mapOf("Notes" to "defaults"), find("notes standard", "Notes"))
    }

    @Test fun aPagesWordAloneIsNoVerb() {
        // One word has nothing beside it to be the name: it finds apps and settings pages called that.
        for (alone in listOf("notifications", "language", "defaults", "battery", "akku", "sprache")) assertEquals(alone, emptyList<Reading>(), Verbs.readings(alone, verbs))
        assertEquals(mapOf("Battery Guru" to null), find("battery", "Battery Guru", "Spotify"))
    }

    @Test fun aPageOnlyFollowsTheName() {
        // At the start these words begin searches of their own ("battery saver", "language settings").
        assertEquals(emptyMap<String, String?>(), find("battery spotify", "Spotify"))
        assertEquals(emptyMap<String, String?>(), find("notifications chrome", "Chrome"))
        assertTrue(Verbs.readings("language slack", verbs).none { it.action == "language" })
        assertTrue(Verbs.readings("standard notes", verbs).none { it.rest == "notes" })
    }

    @Test fun theVerbsThatWereThereKeepTheirShortForms() {
        // Where the start of a word fits an old verb and a page, the old verb has it; one letter more names the page.
        assertEquals(mapOf("Chrome" to "info"), find("chrome de", "Chrome"))            // details
        assertEquals(mapOf("Chrome" to "defaults"), find("chrome def", "Chrome"))
        assertEquals(mapOf("Chrome" to "uninstall"), find("chrome del", "Chrome"))      // delete
        assertEquals(mapOf("Chrome" to "open"), find("chrome la", "Chrome"))            // launch
        assertEquals(mapOf("Chrome" to "language"), find("chrome lan", "Chrome"))
        assertEquals(mapOf("Chrome" to "open"), find("chrome sta", "Chrome"))           // start, starten
        assertEquals(mapOf("Chrome" to "defaults"), find("chrome stan", "Chrome"))
        assertEquals(mapOf("Chrome" to "left"), find("chrome links", "Chrome"))         // the left half, in German
        assertEquals(mapOf("Chrome" to "full"), find("chrome ma", "Chrome"))            // maximise
        assertEquals(mapOf("Chrome" to "open"), find("chrome op", "Chrome"))            // open, not "open by default"
        assertEquals(mapOf("Chrome" to "notifications"), find("chrome be", "Chrome"))   // nothing older starts so
    }
}
