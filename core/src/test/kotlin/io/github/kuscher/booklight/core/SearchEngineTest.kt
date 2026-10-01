package io.github.kuscher.booklight.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {
    private fun app(name: String) = Result(
        id = "app:$name", provider = "apps", kind = Kind.APP, title = name, icon = Icon.Symbol("app"), score = 0.0,
        actions = listOf(Action("open", "Open", Effect.LaunchApp(name, name))),
    )

    private class Names(private val names: List<String>, private val make: (String) -> Result) : Provider {
        override val id = "apps"
        override suspend fun query(q: Query) = names.mapNotNull { n ->
            val s = Matcher.score(q.text, n)
            if (s > 0) make(n).copy(score = s) else null
        }
        override suspend fun zeroState() = names.map(make)
    }

    private val web = object : Provider {
        override val id = "web"
        override suspend fun query(q: Query) = listOf(
            Result("web:search", id, Kind.WEB, "Search the web", icon = Icon.Symbol("globe"), score = 0.1,
                actions = listOf(Action("search", "Search", Effect.OpenUrl(Engines.default.search(q.text)))), learnable = false),
        )
    }

    private val calc = object : Provider {
        override val id = "calc"
        override suspend fun query(q: Query) = Calc.answer(q.text)?.let {
            listOf(Result("calc", id, Kind.ANSWER, it, icon = Icon.Symbol("calc"), score = 1.0,
                actions = listOf(Action("copy", "Copy", Effect.CopyText(it))), answer = it, learnable = false))
        } ?: emptyList()
    }

    private fun engine(history: History = History(), vararg extra: Provider) = SearchEngine(
        listOf(Names(listOf("Chrome", "Calendar", "Calculator", "Camera", "Canvas"), ::app), web, calc) + extra, history, clock = { 1000 },
    )

    @Test fun bestMatchFirstWebLast() = runTest {
        val r = engine().search(Query("ca"))
        assertEquals("Camera", r.first().title)                 // equal prefix matches: shortest, then A to Z
        assertEquals(Kind.WEB, r.last().kind)
        assertTrue(r.none { it.title == "Chrome" })
    }

    @Test fun answersGoFirst() = runTest {
        val r = engine().search(Query("2+2"))
        assertEquals("4", r.first().answer)
        assertEquals(Kind.WEB, r.last().kind)
    }

    @Test fun pickingTeachesTheExactText() = runTest {
        val h = History()
        val e = engine(h)
        val q = Query("ca")
        val calendar = e.search(q).first { it.title == "Calendar" }
        e.picked(q, calendar); e.picked(q, calendar)
        assertEquals("Calendar", e.search(q).first().title)
        // "c" wasn't taught: Calendar only gets the small lift for being used.
        assertEquals("Calendar", e.search(Query("cal")).first().title)
        assertEquals("Chrome", e.search(Query("chr")).first().title)
    }

    @Test fun emptyQueryShowsWhatYouUse() = runTest {
        val h = History()
        val e = engine(h)
        assertTrue(e.search(Query("")).isEmpty())
        e.picked(Query("chr"), app("Chrome"))
        assertEquals(listOf("Chrome"), e.search(Query(" ")).map { it.title })
    }

    @Test fun aSlowOrBrokenProviderIsLeftOut() = runTest {
        val slow = object : Provider { override val id = "slow"; override suspend fun query(q: Query): List<Result> { delay(10_000); return listOf(app("Slow")) } }
        val broken = object : Provider { override val id = "broken"; override suspend fun query(q: Query): List<Result> = error("boom") }
        val r = engine(History(), slow, broken).search(Query("ca"))
        assertEquals("Camera", r.first().title)
        assertTrue(r.none { it.title == "Slow" })
    }

    @Test fun limitIsRespectedAndTheWebRowKeepsTheLastPlace() = runTest {
        val r = engine().search(Query("c"), limit = 3)
        assertEquals(3, r.size)
        assertEquals(Kind.WEB, r.last().kind)
        assertEquals(listOf(Kind.APP, Kind.APP), r.take(2).map { it.kind })
    }

    private fun suggestion(text: String) = Result("suggest:$text", "suggest", Kind.SUGGESTION, text, icon = Icon.Symbol("search"), score = 0.2,
        actions = listOf(Action("search", "Search", Effect.OpenUrl(Engines.default.search(text)))), learnable = false)

    @Test fun suggestionsGoBelowAndOnlyTakeFreeRows() = runTest {
        val e = engine()
        val local = e.search(Query("cam"))                      // Camera + the web row
        assertEquals(2, local.size)
        val merged = e.merge(local, listOf("camera", "camping", "cambridge", "camel", "cameo").map(::suggestion))
        assertEquals(local, merged.take(2))                     // nothing already shown moved
        assertEquals(listOf("camping", "cambridge", "camel"), merged.drop(2).map { it.title })   // "camera" is on screen as the app
        val full = e.search(Query("c"))                         // five apps + the web row
        assertEquals(full, e.merge(full, listOf(suggestion("cats")), limit = full.size))
    }

    private class Words(override val key: String, override val keywords: List<String>, override val name: String) : Scope {
        override val symbol = "search"
        override val hint = "…"
        override suspend fun rows(arg: String) = if (arg.isEmpty()) emptyList() else listOf("first", "second").map {
            Result("$key:$it", key, Kind.WEB, "$it $arg", icon = Icon.Symbol("search"), score = if (it == "first") 0.1 else 1.0,
                actions = listOf(Action("go", "Go", Effect.OpenUrl("https://example.com/$arg"))), learnable = false)
        }
    }

    private val yt = Words("yt", listOf("yt", "youtube"), "YouTube")
    private val mail = Words("mail", listOf("mail"), "Mail")

    private fun scoped() = SearchEngine(
        listOf(Names(listOf("Maps", "Mail", "Chrome"), ::app), web), History(), clock = { 1000 },
        scopes = { listOf(yt, mail) },
        fallback = { text -> listOf(Result("web:search", "web", Kind.WEB, "Search for $text", icon = Icon.Symbol("search"), score = 0.1,
            actions = listOf(Action("search", "Search", Effect.OpenUrl(Engines.default.search(text)))), learnable = false)) },
    )

    @Test fun aKeywordAndASpaceIsAScope() {
        val e = scoped()
        assertEquals("yt" to "lofi beats", e.scopeFor("yt lofi beats")?.let { it.first.key to it.second })
        assertEquals("yt" to "", e.scopeFor("YT ")?.let { it.first.key to it.second })
        assertEquals("yt" to "x", e.scopeFor("youtube x")?.let { it.first.key to it.second })
        assertEquals(null, e.scopeFor("yt"))                    // the keyword alone is ordinary text
        assertEquals(null, e.scopeFor("ytx lofi"))
        assertEquals(null, e.scopeFor(" lofi"))
    }

    @Test fun insideAScopeItsRowsKeepTheirOrderAndTheWebRowIsLast() = runTest {
        val r = scoped().search(Query("lofi", scope = "yt"))
        assertEquals(listOf("first lofi", "second lofi", "Search for yt lofi"), r.map { it.title })
        assertTrue(scoped().search(Query("", scope = "yt")).isEmpty())          // nothing typed: no way out needed yet
        assertTrue(scoped().search(Query("x", scope = "gone")).isEmpty())
    }

    @Test fun scopesAreRowsOfTheOrdinaryList() = runTest {
        val e = scoped()
        assertEquals("scope:yt", e.search(Query("yt")).first().id)              // its keyword, exactly
        assertEquals(Effect.EnterScope("yt"), e.search(Query("yt")).first().actions.first().effect)
        assertEquals("app:Mail", e.search(Query("mail")).first().id)            // an app called exactly that still wins
        assertEquals("scope:mail", e.search(Query("mail"))[1].id)
        assertEquals(listOf("app:Mail", "app:Maps", "scope:mail"), e.search(Query("ma")).take(3).map { it.id })   // apps first for a prefix
        assertTrue(e.search(Query("you")).any { it.id == "scope:yt" })          // by name
        assertTrue(e.search(Query("y")).none { it.id == "scope:yt" && it.score >= 0.85 })
    }

    @Test fun twoWaysOutKeepTheEnd() = runTest {
        val gemini = object : Provider {
            override val id = "gemini"
            override suspend fun query(q: Query) = listOf(Result("web:gemini", id, Kind.WEB, "Ask", icon = Icon.Symbol("spark"), score = 0.09,
                actions = listOf(Action("ask", "Ask", Effect.AskGemini(q.text))), learnable = false))
        }
        val r = engine(History(), gemini).search(Query("c"), limit = 4)
        assertEquals(listOf("web:search", "web:gemini"), r.takeLast(2).map { it.id })
        assertEquals(4, r.size)
    }
}
