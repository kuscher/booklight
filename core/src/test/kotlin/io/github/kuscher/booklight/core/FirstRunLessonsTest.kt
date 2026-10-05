package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Enter
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Test

class FirstRunLessonsTest {
    private val l2 = State(Run.NEW, done = listOf("key"), key = true)
    private val l3 = State(Run.NEW, done = listOf("key", "open"), key = true)
    private val l4 = State(Run.NEW, done = listOf("key", "open", "search"), key = true)
    private val lessons = listOf(l2, l3, l4)

    private val open = Effect.LaunchApp("com.example.videos", "Main")
    private val inAPlace = Effect.LaunchApp("com.example.videos", "Main", place = Place.LEFT)
    private val search = Effect.Open("com.example.videos", "intent:#Intent;action=android.intent.action.SEARCH;end")
    private val copy = Effect.CopyText("180")
    private val page = Effect.OpenSettings("android.settings.WIFI_SETTINGS")

    @Test fun nothingOpensInALesson() {
        for (s in lessons) {
            assertEquals(Enter.PRACTICE_OPEN, FirstRun.enters(s, "apps", open, searchable = true))
            assertEquals(Enter.PRACTICE_OPEN, FirstRun.enters(s, "places", inAPlace, searchable = true))
            assertEquals(Enter.PRACTICE_SEARCH, FirstRun.enters(s, "appsearch", search, searchable = true))
        }
    }

    @Test fun aSumIsCopiedForRealAndThePanelStays() {
        for (s in lessons) assertEquals(Enter.COPY_STAYS, FirstRun.enters(s, "sums", copy, searchable = true))
        // A copy that is no sum (a colour, a password) is as every day.
        assertEquals(Enter.AS_ALWAYS, FirstRun.enters(l4, "color", copy, searchable = true))
        assertEquals(Enter.AS_ALWAYS, FirstRun.enters(l4, null, copy, searchable = true))
    }

    @Test fun makingTheAppTheChipIsDoneForReal() {
        assertEquals(Enter.AS_ALWAYS, FirstRun.enters(l3, "apps", Effect.EnterScope("appsearch:com.example.videos", act = Act.SEARCH), searchable = true))
    }

    @Test fun whereNoAppCanBeSearchedASettingsPageStandsIn() {
        assertEquals(Enter.PRACTICE_SEARCH, FirstRun.enters(l3, "settings", page, searchable = false))
        assertEquals(Enter.AS_ALWAYS, FirstRun.enters(l3, "settings", page, searchable = true))
        // (A page found without the keyword belongs to no line of the list of everything.)
        assertEquals(Enter.AS_ALWAYS, FirstRun.enters(l3, null, page, searchable = false))
    }

    @Test fun anythingElseIsAsEveryDay() {
        assertEquals(Enter.AS_ALWAYS, FirstRun.enters(l2, "web", Effect.OpenUrl("https://example.com"), searchable = true))
        assertEquals(Enter.AS_ALWAYS, FirstRun.enters(l2, "apps", Effect.AppInfo("com.example.videos", "Main"), searchable = true))
        assertEquals(Enter.AS_ALWAYS, FirstRun.enters(l2, "commands", search, searchable = true))
    }

    @Test fun outsideTheLessonsEnterIsAsEveryDay() {
        val elsewhere = listOf(
            State(), State(Run.NEW), State(Run.NEW, key = true), State(Run.UPDATE),
            State(Run.NEW, done = listOf("key", "open", "search", "sum"), key = true),
            State(Run.NEW, done = listOf("key", "open", "search", "sum", "ask"), key = true),
            l2.copy(opens = FirstRun.MAX_OPENS + 1),
        )
        for (s in elsewhere) {
            assertEquals(Enter.AS_ALWAYS, FirstRun.enters(s, "apps", open, searchable = true))
            assertEquals(Enter.AS_ALWAYS, FirstRun.enters(s, "sums", copy, searchable = true))
        }
    }

    @Test fun whatEnterDidEndsItsLesson() {
        assertEquals(Screen.L3, FirstRun.screen(FirstRun.ran(l2, Enter.PRACTICE_OPEN)))
        assertEquals(Screen.L4, FirstRun.screen(FirstRun.ran(l3, Enter.PRACTICE_SEARCH)))
        assertEquals(Screen.Q, FirstRun.screen(FirstRun.ran(l4, Enter.COPY_STAYS)))
        assertEquals(l2, FirstRun.ran(l2, Enter.AS_ALWAYS))
    }

    @Test fun aLessonsThingCountsWhicheverLessonStands() {
        val summed = FirstRun.ran(l2, Enter.COPY_STAYS)
        assertEquals(Screen.L2, FirstRun.screen(summed))
        val searched = FirstRun.ran(FirstRun.ran(summed, Enter.PRACTICE_OPEN), Enter.PRACTICE_SEARCH)
        assertEquals(Screen.Q, FirstRun.screen(searched))
        // Done again, it changes nothing.
        assertEquals(l3, FirstRun.ran(l3, Enter.PRACTICE_OPEN))
    }

    @Test fun noLessonStandingNothingIsMarkedDone() {
        assertEquals(State(), FirstRun.ran(State(), Enter.PRACTICE_OPEN))
        val onTheKey = State(Run.NEW)
        for (enter in Enter.entries) assertEquals(onTheKey, FirstRun.ran(onTheKey, enter))
    }

    @Test fun aSettingsPageOutsideTheLessonsIsAsEveryDay() {
        assertEquals(Enter.AS_ALWAYS, FirstRun.enters(State(), "settings", page, searchable = false))
    }
}
