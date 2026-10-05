package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Coach
import io.github.kuscher.booklight.core.FirstRun.Enter
import io.github.kuscher.booklight.core.FirstRun.Example
import io.github.kuscher.booklight.core.FirstRun.Part
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirstRunRecipeTest {
    private val l2 = State(Run.NEW, done = listOf("key"), key = true)
    private val l3 = State(Run.NEW, done = listOf("key", "open"), key = true)
    private val l4 = State(Run.NEW, done = listOf("key", "open", "search"), key = true)
    private val q = State(Run.NEW, done = listOf("key", "open", "search", "sum"), key = true)
    private val noLesson = listOf(State(), State(Run.NEW), State(Run.NEW, key = true), q, l2.copy(opens = FirstRun.MAX_OPENS + 1))

    private val open = Effect.LaunchApp("com.example.videos", "Main")
    private val intoApp = Effect.EnterScope("appsearch:com.example.videos", act = Act.SEARCH)
    private val search = Effect.Open("com.example.videos", "intent:#Intent;action=android.intent.action.SEARCH;end")
    private val copy = Effect.CopyText("180")
    private val page = Effect.OpenSettings("android.settings.WIFI_SETTINGS")
    private val sum = "150 + 20%"

    /** The coach line for what Enter would do on the selected row now: asked the way the panel asks, through [FirstRun.enters]. */
    private fun coach(s: State, line: String?, effect: Effect, searchable: Boolean = true, search: Boolean = true) =
        FirstRun.coach(s, FirstRun.enters(s, line, effect, searchable), effect, search)

    @Test fun theLessonThatStands() {
        assertEquals(listOf(Screen.L2, Screen.L3, Screen.L4), listOf(l2, l3, l4).map { FirstRun.lesson(it) })
        for (s in noLesson) assertNull(FirstRun.lesson(s))
    }

    @Test fun theCoachLineSaysTheNextKeyAndWhatItDoesToday() {
        assertEquals(Coach.OPENS, coach(l2, "apps", open))
        assertEquals(Coach.TO_SEARCH, coach(l3, "apps", open))
        assertEquals(Coach.INTO_APP, coach(l3, "apps", intoApp))
        assertEquals(Coach.SEARCHES, coach(l3, "appsearch", search))
        assertEquals(Coach.COPIES, coach(l4, "sums", copy))
    }

    /** It never promises what Enter will not do: where the selected row is not the lesson's, the lesson's own title stands there. */
    @Test fun whereTheSelectedRowIsNotTheLessonsItsTitleStands() {
        // Lesson 3 on an app that cannot be searched: its row has no Search to move to.
        assertEquals(Coach.TITLE, coach(l3, "apps", open, search = false))
        // A sum while lesson 2 stands, an app while lesson 4 stands, the web, App info.
        assertEquals(Coach.TITLE, coach(l2, "sums", copy))
        assertEquals(Coach.TITLE, coach(l4, "apps", open))
        assertEquals(Coach.TITLE, coach(l2, "web", Effect.OpenUrl("https://example.com")))
        assertEquals(Coach.TITLE, coach(l3, "apps", Effect.AppInfo("com.example.videos", "Main")))
        // Making the app the chip is lesson 3's step, not lesson 2's.
        assertEquals(Coach.TITLE, coach(l2, "apps", intoApp))
        // Nothing is selected.
        assertEquals(Coach.TITLE, FirstRun.coach(l2, Enter.AS_ALWAYS, null, search = false))
    }

    /** On every other day, and on every other screen of first run, the footer's left end is as it was. */
    @Test fun whereNoLessonStandsThereIsNoCoachLine() {
        for (s in noLesson) for (enter in Enter.entries) assertNull(FirstRun.coach(s, enter, open, search = true))
    }

    @Test fun theSettingsPageThatStandsInIsSaidToOpen() {
        assertEquals(Coach.OPENS, coach(l3, "settings", page, searchable = false))
        // Where an app can be searched a settings page is as every day, and nothing is promised of it.
        assertEquals(Coach.TITLE, coach(l3, "settings", page, searchable = true))
    }

    @Test fun anAppsLettersAreTheStartOfItsFirstWordThreeAtLeastFourAtMost() {
        assertEquals(listOf("you", "yout"), FirstRun.starts("YouTube"))
        assertEquals(listOf("pla", "play"), FirstRun.starts(" Play Store "))
        assertEquals(listOf("map"), FirstRun.starts("Map"))
        assertEquals(listOf("tv"), FirstRun.starts("TV"))
        assertEquals(emptyList<String>(), FirstRun.starts("  "))
    }

    /** The letters a recipe shows must put the app's row first: Enter is pressed on row one. */
    @Test fun theLettersShownAreTheShortestThatPutTheAppFirst() {
        assertEquals("you", FirstRun.letters("YouTube") { true })
        assertEquals("yout", FirstRun.letters("YouTube") { it == "yout" })
        // No start of it leads its list: this app is not the example.
        assertNull(FirstRun.letters("YouTube") { false })
    }

    @Test fun lessonThreeBuildsOnLessonTwosApp() {
        val e = FirstRun.example(app = "yout", word = "lofi", settings = null, keyword = "s", page = "wifi")
        assertEquals(Example("yout", "yout", enter = true, word = "lofi"), e)
        assertEquals(listOf(Part.Typed("yout"), Part.Key.ENTER), FirstRun.recipe(l2, e, sum))
        assertEquals(listOf(Part.Typed("yout"), Part.Key.TAB, Part.Key.ENTER, Part.Typed("lofi"), Part.Key.ENTER), FirstRun.recipe(l3, e, sum))
        assertEquals(listOf(Part.Typed(sum), Part.Key.ENTER), FirstRun.recipe(l4, e, sum))
    }

    /** No app of this device can be searched, or none leads its list: Settings stands in, by its keyword, and the page found there is lesson 3's thing. */
    @Test fun whereNoAppCanBeTheExampleSettingsStandsIn() {
        val e = FirstRun.example(app = null, word = "lofi", settings = "sett", keyword = "s", page = "wifi")
        assertEquals(listOf(Part.Typed("sett"), Part.Key.ENTER), FirstRun.recipe(l2, e, sum))
        // The keyword's Tab makes Settings the chip by itself: no Enter follows it.
        assertEquals(listOf(Part.Typed("s"), Part.Key.TAB, Part.Typed("wifi"), Part.Key.ENTER), FirstRun.recipe(l3, e, sum))
        // What the recipe shows is what Enter then practises: asked with what the example says.
        assertEquals(Enter.PRACTICE_SEARCH, FirstRun.enters(l3, "settings", page, searchable = e.enter))
        assertEquals(Enter.AS_ALWAYS, FirstRun.enters(l3, "settings", page, searchable = FirstRun.example("yout", "lofi", null, "s", "wifi").enter))
    }

    @Test fun aRecipeShowsNoMoreThanItHasRoomFor() {
        val long = Example("youtube", "youtube", enter = true, word = "a long word to look for")
        assertEquals(Part.Typed("yout"), FirstRun.recipe(l2, long, sum).first())
        assertEquals(Part.Typed("a long w"), FirstRun.recipe(l3, long, sum)[3])
        // Nothing to name (no app that can be the example, and no Settings app either): the keys alone.
        val none = FirstRun.example(app = null, word = "lofi", settings = null, keyword = "", page = "wifi")
        assertEquals(listOf<Part>(Part.Key.ENTER), FirstRun.recipe(l2, none, sum))
        assertEquals(listOf(Part.Key.TAB, Part.Typed("wifi"), Part.Key.ENTER), FirstRun.recipe(l3, none, sum))
    }

    /**
     * A name that begins with an emoji, or holds a letter that takes two of the text's units: a recipe never shows half
     * of one, and what belongs to a character stays with it.
     */
    @Test fun aNameIsNeverCutInsideACharacter() {
        val grin = "😀"                                          // one character, two units
        assertEquals(grin + "ab", FirstRun.head(grin + "abc", 3))
        assertEquals(grin.repeat(3), FirstRun.head(grin.repeat(5), 3))
        // A skin tone, a variation selector, what a joiner joins, a mark over a letter, the second half of a flag.
        val thumb = "👍🏽"
        assertEquals(thumb + "a", FirstRun.head(thumb + "ab", 2))
        val heart = "❤️"
        assertEquals(heart, FirstRun.head(heart + "abc", 1))
        val family = "👨‍👩‍👧"
        assertEquals(family, FirstRun.head(family + "x", 1))
        assertEquals("ét", FirstRun.head("été", 2))
        val flags = "🇩🇪🇫🇷"
        assertEquals(flags.substring(0, 4), FirstRun.head(flags, 1))
        assertEquals(flags, FirstRun.head(flags + "x", 2))
        // Plain letters are cut as they were, and nothing is made longer.
        assertEquals("yout", FirstRun.head("youtube", 4))
        assertEquals("tv", FirstRun.head("tv", 4))
        assertEquals("", FirstRun.head("abc", 0))
        assertEquals("", FirstRun.head("", 3))
        // The starts of a name and a recipe's typed parts are cut by it: a letter of two units is one letter.
        val script = "\uD835\uDCB6"                              // a small script a, outside the common letters
        assertEquals(listOf(script + "bc", script + "bcd"), FirstRun.starts(script + "bcde"))
        val e = Example(grin.repeat(6), grin.repeat(6), enter = true, word = grin.repeat(12))
        assertEquals(Part.Typed(grin.repeat(4)), FirstRun.recipe(l2, e, sum).first())
        assertEquals(Part.Typed(grin.repeat(8)), FirstRun.recipe(l3, e, sum)[3])
    }

    /**
     * A name that begins with what a keyboard cannot type (an emoji, a sign) is not offered as letters to type: it has
     * no start, so its app is not the example, and Settings stands in.
     */
    @Test fun aNameThatBeginsWithWhatCannotBeTypedHasNoStart() {
        val grin = "😀"
        assertEquals(emptyList<String>(), FirstRun.starts(grin + "Music"))
        assertEquals(emptyList<String>(), FirstRun.starts(grin + grin))
        assertEquals(emptyList<String>(), FirstRun.starts("★ Stars"))
        assertEquals(emptyList<String>(), FirstRun.starts("+1"))
        assertNull(FirstRun.letters(grin + "Music") { true })
        // A digit is typed as a letter is; and only the first character is asked.
        assertEquals(listOf("204", "2048"), FirstRun.starts("2048 Puzzle"))
        assertEquals(listOf("go!"), FirstRun.starts("Go!"))
        // With no letters for the app, the example is the Settings app's.
        val e = FirstRun.example(FirstRun.letters(grin + "Music") { true }, word = "lofi", settings = "sett", keyword = "s", page = "wifi")
        assertEquals(Example("sett", "s", enter = false, word = "wifi"), e)
    }

    /**
     * Where each character of a text ends, as a reader counts them: a recipe's typed part is written a character at a
     * time, never half of one, and its pace is counted by the same characters. It is `head`'s own count.
     */
    @Test fun aTypedPartIsWrittenAWholeCharacterAtATime() {
        assertEquals(listOf(1, 2, 3, 4), FirstRun.ends("yout"))
        assertEquals(emptyList<Int>(), FirstRun.ends(""))
        val grin = "😀"
        assertEquals(listOf(2, 3, 5), FirstRun.ends(grin + "a" + grin))
        // A flag is one character of four units, a family one of eight, a letter with its mark one of two.
        assertEquals(listOf(4, 8), FirstRun.ends("🇩🇪🇫🇷"))
        assertEquals(listOf(8, 9), FirstRun.ends("👨‍👩‍👧" + "x"))
        assertEquals(listOf(2, 3, 5), FirstRun.ends("e\u0301te\u0301"))
        assertEquals(listOf(4, 5), FirstRun.ends("👍🏽" + "a"))
        // A mark with no letter before it belongs to the letter after it: it is never shown alone.
        assertEquals(listOf(2, 3), FirstRun.ends("\u0301ab"))
        for (text in listOf("yout", "150 + 20%", grin + "ab", "🇩🇪🇫🇷x", "👨‍👩‍👧x", "e\u0301te\u0301", "❤️abc", "\u0301ab", "a")) {
            val ends = FirstRun.ends(text)
            assertEquals(text, text.length, ends.last())
            for (n in 1..ends.size) assertEquals("$text $n", text.substring(0, ends[n - 1]), FirstRun.head(text, n))
            assertEquals(text, text, FirstRun.head(text, ends.size + 1))
            assertEquals(text, "", FirstRun.head(text, 0))
        }
    }

    @Test fun noRecipeWhereNoLessonStands() {
        val e = FirstRun.example("yout", "lofi", null, "s", "wifi")
        for (s in noLesson) assertEquals(emptyList<Part>(), FirstRun.recipe(s, e, sum))
    }
}
