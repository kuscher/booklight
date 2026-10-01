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
                actions = listOf(Action("search", "Search", Effect.WebSearch(q.text))), learnable = false),
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

    @Test fun limitIsRespected() = runTest {
        assertEquals(3, engine().search(Query("c"), limit = 3).size)
    }
}
