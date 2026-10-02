package io.github.kuscher.booklight.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A keyword that needs an app (`call`, `wa`) is only there where one answers it. Its scope stays in
 * the list of scopes and says so itself: no keywords, not listed. This is what the engine makes of that.
 */
class AbsentKeywordTest {
    private class Needs(private val there: () -> Boolean) : Scope {
        override val key = "call"
        override val keywords: List<String> get() = if (there()) listOf("call", "anruf") else emptyList()
        override val name = "Call"
        override val symbol = "phone"
        override val hint = ""
        override val listed: Boolean get() = there()
        // As the scope in the app: a preview while the line is a number or can still become one, and no row once it cannot.
        override suspend fun rows(arg: String): List<Result> {
            val d = Reach.phone(arg)
            if (d == null && !Reach.startsNumber(arg)) return emptyList()
            val dial = listOfNotNull(d?.let { Action("dial", "Open phone", Effect.Phone(it.number)) })
            return listOf(Result("reach:call", key, Kind.OTHER, name, icon = Icon.Symbol(symbol), score = 1.0, actions = dial, learnable = false))
        }
    }

    private val web = object : Provider {
        override val id = "web"
        override suspend fun query(q: Query) = listOf(Result("web:search", id, Kind.WEB, "Search the web", icon = Icon.Symbol("globe"), score = 0.1,
            actions = listOf(Action("search", "Search", Effect.OpenUrl(Engines.default.search(q.text)))), learnable = false))
    }

    private fun search(text: String) = Result("web:search", "web", Kind.WEB, "Search the web for $text", icon = Icon.Symbol("globe"), score = 0.1,
        actions = listOf(Action("search", "Search", Effect.OpenUrl(Engines.default.search(text)))), learnable = false)

    private fun engine(there: () -> Boolean) = SearchEngine(listOf(web), History(), clock = { 1000 }, scopes = { listOf(Needs(there)) }, fallback = { listOf(search(it)) })

    @Test fun withAnAppItIsAKeyword() = runTest {
        val e = engine { true }
        assertEquals("call", e.scopeFor("call 030 5550 1234")?.scope?.key)
        assertEquals("call", e.scopeFor("Anruf 030")?.scope?.key)
        assertEquals("call", e.keywordScope("call")?.key)
        assertEquals("scope:call", e.search(Query("call")).first().id)
        assertEquals("scope:call", e.search(Query("ca")).first().id)
    }

    /** "go" alone is how Google starts: the keyword's row stays under the app, and "go " still enters the scope. */
    @Test fun aShortKeywordAloneStaysUnderAnAppOfThatStart() = runTest {
        val go = object : Scope {
            override val key = "go"
            override val keywords = listOf("go", "route")
            override val shy = listOf("go")
            override val name = "Directions"
            override val symbol = "directions"
            override val hint = ""
            override suspend fun rows(arg: String) = emptyList<Result>()
        }
        val apps = object : Provider {
            override val id = "apps"
            override suspend fun query(q: Query) = Matcher.score(q.text, "Google").let { s -> if (s <= 0) emptyList() else listOf(Result("app:google", id, Kind.APP, "Google", icon = Icon.Symbol("app"), score = s, actions = emptyList())) }
        }
        val e = SearchEngine(listOf(apps), History(), clock = { 1000 }, scopes = { listOf(go) })
        assertEquals(listOf("app:google", "scope:go"), e.search(Query("go")).map { it.id })
        assertEquals("scope:go", e.search(Query("route")).first().id)
        assertEquals("go", e.scopeFor("go hamburg")?.scope?.key)
    }

    @Test fun aLineNotMeantForItPutsTheSearchFirst() = runTest {
        val e = engine { true }
        fun inside(text: String) = Query(text, "call", "call")
        // A number: the preview is row one and Enter opens the phone app; the search for all of it is the last row.
        assertEquals(listOf("reach:call", "web:search"), e.search(inside("030 5550 1234")).map { it.id })
        assertEquals(Effect.Phone("03055501234"), e.search(inside("030 5550 1234")).first().actions.first().effect)
        // On its way to a number: the preview stays, with nothing to run yet.
        assertEquals(listOf("reach:call", "web:search"), e.search(inside("+49")).map { it.id })
        assertTrue(e.search(inside("+49")).first().actions.isEmpty())
        // "call of duty": no preview. Row one is the search for the keyword and the text together, and Enter runs it.
        val rows = e.search(inside("of duty"))
        assertEquals(listOf("web:search"), rows.map { it.id })
        assertEquals("Search the web for call of duty", rows.first().title)
        assertTrue(rows.first().actions.isNotEmpty())
    }

    @Test fun withoutOneItIsOrdinaryText() = runTest {
        val e = engine { false }
        assertNull(e.scopeFor("call 030 5550 1234"))
        assertNull(e.keywordScope("call"))
        // No row for the keyword, for its start or for the scope's name: only the way out to the web.
        for (text in listOf("call", "ca", "anruf", "Call")) assertTrue(text, e.search(Query(text)).all { it.kind == Kind.WEB })
    }

    @Test fun anAppThatComesOrGoesChangesItAtOnce() = runTest {
        var there = false
        val e = engine { there }
        assertNull(e.scopeFor("call 030"))
        there = true
        assertEquals("call", e.scopeFor("call 030")?.scope?.key)
        there = false
        assertNull(e.keywordScope("call"))
    }
}
