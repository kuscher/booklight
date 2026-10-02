package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Who leads for words typed after an app's name: the web, until the app's row is picked once; the web again after two picks running. */
class LeadTest {
    @Test fun theWebLeadsUntilTheAppsRowIsPickedOnce() {
        assertFalse(Lead.app(0))
        assertTrue(Lead.app(Lead.pickedApp()))
    }

    @Test fun twoPicksOfTheWebsRowRunningGiveTheLeadBack() {
        val once = Lead.pickedWeb(Lead.pickedApp())
        assertTrue(Lead.app(once))
        assertFalse(Lead.app(Lead.pickedWeb(once)))
    }

    @Test fun theAppsRowInBetweenStartsTheCountAgain() {
        val again = Lead.pickedApp().let(Lead::pickedWeb).let { Lead.pickedApp() }
        assertTrue(Lead.app(Lead.pickedWeb(again)))
        assertFalse(Lead.app(Lead.pickedWeb(Lead.pickedWeb(again))))
    }

    @Test fun pickingTheWebWhileItLeadsChangesNothing() {
        assertEquals(0, Lead.pickedWeb(0))
        assertEquals(0, Lead.pickedWeb(Lead.pickedWeb(Lead.pickedWeb(Lead.pickedApp()))))
    }

    @Test fun historyKeepsItForEachAppAndForgetsItWithTheRest() {
        val h = History()
        assertFalse(h.leads("com.netflix"))
        h.led("com.netflix", inApp = true)
        assertTrue(h.leads("com.netflix"))
        assertFalse(h.leads("com.spotify"))
        h.led("com.netflix", inApp = false)
        assertTrue(h.leads("com.netflix"))
        h.led("com.netflix", inApp = false)
        assertFalse(h.leads("com.netflix"))
        assertTrue(h.data().leads.isEmpty())
        // The web's row for an app that never led: nothing to keep.
        h.led("com.spotify", inApp = false)
        assertTrue(h.data().leads.isEmpty())
        h.led("com.spotify", inApp = true)
        h.clear()
        assertFalse(h.leads("com.spotify"))
    }
}
