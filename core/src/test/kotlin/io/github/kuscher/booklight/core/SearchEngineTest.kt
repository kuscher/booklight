package io.github.kuscher.booklight.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        override fun byIds(ids: Set<String>) = names.map(make).filter { it.id in ids }
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

    /** A flight's row as the app scores it: under every local match, and a guess under the ways out too. */
    private fun flights(score: Double) = object : Provider {
        override val id = "flights"
        override suspend fun query(q: Query) = listOf(Result("flight:CA12", id, Kind.OTHER, "CA 12 · Air China", icon = Icon.Symbol("plane"), score = score,
            actions = listOf(Action("open", "Open", Effect.OpenUrl("https://example.com"))), learnable = false))
    }

    @Test fun aFlightStandsUnderEveryLocalMatchAndOverTheWeb() = runTest {
        val r = engine(History(), flights(0.3)).search(Query("ca"))
        assertEquals("Camera", r.first().title)
        assertEquals(listOf("flights", "web"), r.takeLast(2).map { it.provider })
    }

    @Test fun aGuessIsTheLastRow() = runTest {
        val r = engine(History(), flights(SearchEngine.GUESS)).search(Query("ca"))
        assertEquals("Camera", r.first().title)
        assertEquals(listOf("web", "flights"), r.takeLast(2).map { it.provider })
        // It keeps its place when the list is full, as the web row does.
        val few = engine(History(), flights(SearchEngine.GUESS)).search(Query("ca"), limit = 3)
        assertEquals(listOf("apps", "web", "flights"), few.map { it.provider })
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

    @Test fun anEmptyQueryFindsNothingAndIdsFindTheirRows() = runTest {
        val h = History()
        val e = engine(h)
        e.picked(Query("chr"), app("Chrome"))
        assertTrue(e.search(Query(" ")).isEmpty())
        assertEquals(listOf(app("Chrome").id), e.used().keys.toList())
        assertEquals(listOf("Chrome"), e.byIds(e.used().keys).map { it.title })
        assertTrue(e.byIds(setOf("app:gone")).isEmpty())
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
        assertEquals("yt" to "lofi beats", e.scopeFor("yt lofi beats")?.let { it.scope.key to it.text })
        assertEquals("yt" to "", e.scopeFor("YT ")?.let { it.scope.key to it.text })
        assertEquals("yt" to "x", e.scopeFor("youtube x")?.let { it.scope.key to it.text })
        assertEquals(null, e.scopeFor("yt"))                    // the keyword alone is ordinary text
        assertEquals("youtube", e.scopeFor("youtube x")?.word)         // the word that was typed is kept
        assertEquals(null, e.scopeFor("ytx lofi"))
        assertEquals(null, e.scopeFor(" lofi"))
    }

    @Test fun insideAScopeItsRowsKeepTheirOrderAndTheWebRowIsLast() = runTest {
        val r = scoped().search(Query("lofi", scope = "yt"))
        assertEquals(listOf("first lofi", "second lofi", "Search for yt lofi"), r.map { it.title })
        assertTrue(scoped().search(Query("", scope = "yt")).isEmpty())          // nothing typed: no way out needed yet
        // The way out searches for what was typed, whichever of the scope's keywords that was.
        assertEquals("Search for youtube lofi", scoped().search(Query("lofi", scope = "yt", keyword = "youtube")).last().title)
        assertTrue(scoped().search(Query("x", scope = "gone")).isEmpty())
        // The keyword was the start of an app's name: the app comes first.
        val named = SearchEngine(listOf(Names(listOf("YT Music"), ::app)), History(), clock = { 1000 }, scopes = { listOf(yt) })
        assertEquals(listOf("app:YT Music", "yt:first", "yt:second"), named.search(Query("mus", scope = "yt")).map { it.id })
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

    // ---- 2.0

    private class Pages(override val key: String, override val keywords: List<String>, override val name: String, private val pages: List<String>) : Scope {
        override val symbol = "settings"
        override val hint = "…"
        override suspend fun rows(arg: String) = pages.filter { arg.isEmpty() || Matcher.score(arg, it) > 0 }.map {
            Result("page:$it", key, Kind.SETTING, it, icon = Icon.Symbol("settings"), score = 1.0, actions = listOf(Action("open", "Open", Effect.OpenSettings(it))))
        } + Result("${SearchEngine.HANDOVER}$key", key, Kind.COMMAND, "Search the other place", icon = Icon.Symbol("open"), score = 1.0,
            actions = listOf(Action("open", "Open", Effect.OpenSettings("search"))), learnable = false)
    }

    private val settings = Pages("settings", listOf("s", "settings"), "Search settings", listOf("Bluetooth", "Sound", "Storage"))
    private val help = object : Scope {
        override val key = "help"; override val keywords = listOf("?"); override val name = "Everything"; override val symbol = "list"; override val hint = "…"
        override val web = false
        override suspend fun rows(arg: String) = listOf(Result("help:timer", key, Kind.OTHER, "Timer", icon = Icon.Symbol("timer"), score = 1.0, actions = emptyList()))
    }
    private val other = object : Scope {
        override val key = "ext:shot"; override val keywords = listOf("shot"); override val name = "Capture"; override val symbol = "app"; override val hint = "…"
        override val spaceEnters = false
        override suspend fun rows(arg: String) = emptyList<Result>()
    }

    private class Commands(private val titles: List<String>) : Provider {
        override val id = SearchEngine.COMMANDS
        override suspend fun query(q: Query) = titles.mapNotNull { t ->
            val s = Matcher.score(q.text, t)
            if (s > 0) Result("cmd:$t", id, Kind.COMMAND, t, icon = Icon.Symbol("app"), score = s * 0.9, actions = listOf(Action("open", "Open", Effect.Open("pkg", "intent:#Intent;end"))), label = "Chrome") else null
        }
    }

    private fun two(vararg providers: Provider) = SearchEngine(
        listOf(Names(listOf("Slack", "Spotify", "Settings", "Summa", "Sheets", "Slides", "Snapseed", "Signal", "Skype", "Stocks"), ::app), web) + providers,
        History(), clock = { 1000 }, scopes = { listOf(settings, help, other, Words("new", listOf("new"), "New")) },
        fallback = { text -> listOf(Result("web:search", "web", Kind.WEB, "Search for $text", icon = Icon.Symbol("search"), score = 0.1,
            actions = listOf(Action("search", "Search", Effect.OpenUrl(Engines.default.search(text)))), learnable = false)) },
    )

    @Test fun aOneLetterKeywordsRowKeepsTheLastLocalPlace() = runTest {
        val r = two().search(Query("s"))
        assertEquals(Kind.APP, r.first().kind)                                  // "s", Enter still opens an app
        assertEquals(listOf("scope:settings", "web:search"), r.takeLast(2).map { it.id })   // and the keyword's row is always there, last before the web
        assertEquals(8, r.size)                                                 // ten apps on S: it is not pushed off the list
        // A longer keyword keeps its place at the top, as in 1.1.
        assertEquals("scope:settings", two().search(Query("settings")).first { it.kind == Kind.SCOPE }.id)
        assertTrue(two().search(Query("settings")).indexOfFirst { it.id == "scope:settings" } <= 1)
    }

    @Test fun whatTabTurnsIntoAChip() {
        val e = two()
        assertEquals("settings", e.keywordScope("s")?.key)
        assertEquals("settings", e.keywordScope(" S ")?.key)
        assertEquals("settings", e.keywordScope("Settings")?.key)
        assertEquals(null, e.keywordScope("se"))
        assertEquals(null, e.keywordScope("s x"))
        assertEquals(null, e.keywordScope(""))
        assertEquals("ext:shot", e.keywordScope("shot")?.key)                   // Tab enters another app's keyword…
        assertEquals(null, e.scopeFor("shot area"))                             // …a Space does not, until it has been used
    }

    @Test fun insideAScopeNothingMatchedMeansTheWebComesFirst() = runTest {
        val e = two()
        assertEquals(listOf("page:Bluetooth", "handover:settings", "web:search"), e.search(Query("blu", scope = "settings", keyword = "s")).map { it.id })
        val none = e.search(Query("bahn", scope = "settings", keyword = "s"))
        assertEquals(listOf("web:search", "handover:settings"), none.map { it.id })
        assertEquals("Search for s bahn", none.first().title)
        // Nothing typed yet: the scope's own rows, no way out.
        assertEquals(4, e.search(Query("", scope = "settings")).size)
    }

    @Test fun aListThatIsNotASearchHasNoWebRow() = runTest {
        assertEquals(listOf("help:timer"), two().search(Query("ti", scope = "help", keyword = "?")).map { it.id })
    }

    @Test fun aCommandThatStartsWithTheKeywordComesFirst() = runTest {
        val e = two(Commands(listOf("New tab", "New incognito tab", "New event")))
        val r = e.search(Query("tab", scope = "new", keyword = "new"))
        assertEquals("cmd:New tab", r.first().id)                               // "new tab" is Chrome's command, not a file called tab
        assertEquals(listOf("new:first", "new:second", "web:search"), r.drop(1).map { it.id })
        // One letter after the keyword is not enough to put commands in front.
        assertEquals("new:first", e.search(Query("t", scope = "new", keyword = "new")).first().id)
        // Two at most.
        assertTrue(two(Commands(listOf("New a", "New ab", "New abc"))).search(Query("a", scope = "new", keyword = "new")).none { it.provider == SearchEngine.COMMANDS })
        assertEquals(2, two(Commands(listOf("New ab", "New abc", "New abcd"))).search(Query("ab", scope = "new", keyword = "new")).count { it.provider == SearchEngine.COMMANDS })
    }

    @Test fun `a scope's row is found by its other words, which do not enter it`() = runTest {
        val help = object : Scope {
            override val key = "help"; override val keywords = listOf("?"); override val name = "Everything"; override val symbol = "list"; override val hint = ""
            override val words = listOf("help")
            override suspend fun rows(arg: String) = emptyList<Result>()
        }
        val engine = SearchEngine(emptyList(), History(), scopes = { listOf(help) })
        assertEquals("scope:help", engine.search(Query("help")).first().id)
        assertEquals("scope:help", engine.search(Query("he")).first().id)
        assertNull(engine.scopeFor("help with taxes"))
        assertNull(engine.keywordScope("help"))
    }

    @Test fun `another app's keyword stays under an exact local match`() = runTest {
        fun scope(enters: Boolean) = object : Scope {
            override val key = "ext:wifi"; override val keywords = listOf("wifi"); override val name = "Hotspots"; override val symbol = "app"; override val hint = ""
            override val spaceEnters = enters
            override suspend fun rows(arg: String) = emptyList<Result>()
        }
        val page = object : Provider {
            override val id = "settings"
            override suspend fun query(q: Query) = if (q.text == "wifi") listOf(Result("setting:wifi", id, Kind.SETTING, "Wi-Fi", icon = Icon.Symbol("settings"), score = 1.0, actions = listOf(Action("open", "Open", Effect.OpenSettings("x"))))) else emptyList()
        }
        // Not chosen yet: under the settings page. Chosen (entered once from its row): a keyword like Booklight's own.
        assertEquals("setting:wifi", SearchEngine(listOf(page), History(), scopes = { listOf(scope(false)) }).search(Query("wifi")).first().id)
        assertEquals("scope:ext:wifi", SearchEngine(listOf(page), History(), scopes = { listOf(scope(true)) }).search(Query("wifi")).first().id)
    }

    /** A search inside an app, as the app makes its rows: for each of [names] that starts the text, a row under every local match. */
    private class InApps(private val names: List<String>) : Provider {
        override val id = SearchEngine.COMMANDS
        override suspend fun query(q: Query) = AppSearch.readings(q.text).flatMap { r ->
            names.filter { Matcher.fold(it) == r.name }.map { n ->
                Result(SearchEngine.IN_APP + n, id, Kind.COMMAND, "Search $n for ${r.text}", icon = Icon.Symbol("app"), score = 0.02 + r.name.length * 0.0001,
                    actions = listOf(Action("search", "Search", Effect.Open(n, "intent:#Intent;end"))), learnable = false, label = n)
            }
        }
    }

    private fun searched(vararg scopes: Scope) = SearchEngine(
        listOf(Names(listOf("Spotify", "Spotify Desktop", "Google", "Google Maps", "Google Meet", "YouTube", "YouTube Music", "YT Music", "Maps", "Play Store"), ::app), web,
            InApps(listOf("Spotify", "Google", "YouTube", "YouTube Music", "YT Music", "Maps", "Play Store"))),
        History(), clock = { 1000 }, scopes = { scopes.toList() },
        fallback = { text -> listOf(Result("web:search", "web", Kind.WEB, "Search for $text", icon = Icon.Symbol("search"), score = 0.1,
            actions = listOf(Action("search", "Search", Effect.OpenUrl(Engines.default.search(text)))), learnable = false)) },
    )

    @Test fun `a search inside an app is the first row when nothing else matches`() = runTest {
        assertEquals(listOf("Search Spotify for daft punk", "Search the web"), searched().search(Query("spotify daft punk")).map { it.title })
        // Both readings of a name of two words, the longer name first.
        assertEquals(listOf("Search YouTube Music for daft punk", "Search YouTube for music daft punk", "Search the web"), searched().search(Query("youtube music daft punk")).map { it.title })
    }

    @Test fun `a search inside an app never stands above a local match`() = runTest {
        // An app whose name is the whole text is still row one, on every letter of the way.
        assertEquals(listOf("app:Spotify Desktop", "appsearch:Spotify", "web:search"), searched().search(Query("spotify d")).map { it.id })
        assertEquals(listOf("app:Google Maps", "app:Google Meet", "appsearch:Google", "web:search"), searched().search(Query("google m")).map { it.id })
        assertEquals(listOf("app:Google Maps", "appsearch:Google", "web:search"), searched().search(Query("google maps")).map { it.id })
        // The name alone, with or without a space after it, is the app.
        assertEquals(listOf("app:Spotify", "app:Spotify Desktop", "web:search"), searched().search(Query("spotify ")).map { it.id })
        // Not even once it has been run for that very text: it is the text itself, and nothing is learned from it.
        val history = History()
        val e = SearchEngine(listOf(Names(listOf("Google Maps"), ::app), web, InApps(listOf("Google"))), history, clock = { 1000 })
        val row = e.search(Query("google m")).first { it.id == "appsearch:Google" }
        repeat(5) { e.picked(Query("google m"), row) }
        assertEquals("app:Google Maps", e.search(Query("google m")).first().id)
    }

    @Test fun `under a keyword's chip an app's name that goes on into the text is searched first`() = runTest {
        val yt = Words("yt", listOf("yt"), "YouTube")
        val play = Words("play", listOf("play"), "Play")
        val maps = Words("maps", listOf("maps"), "Maps")
        val e = searched(yt, play, maps)
        // "yt music daft punk": YT Music is an app, and "daft punk" is looked for there before YouTube is searched for all of it.
        assertEquals(listOf("appsearch:YT Music", "yt:first", "yt:second", "web:search"), e.search(Query("music daft punk", scope = "yt", keyword = "yt")).map { it.id })
        assertEquals(listOf("appsearch:Play Store", "play:first", "play:second", "web:search"), e.search(Query("store calculator", scope = "play", keyword = "play")).map { it.id })
        // The name alone is the app, as before; and then its search.
        assertEquals("app:YT Music", e.search(Query("music", scope = "yt", keyword = "yt")).first().id)
        // An app called what the keyword is called: the keyword was typed, and its scope answers.
        assertEquals(listOf("maps:first", "maps:second", "web:search"), e.search(Query("coffee", scope = "maps", keyword = "maps")).map { it.id })
        assertEquals(listOf("maps:first", "maps:second", "web:search"), e.search(Query("coffee", scope = "maps", keyword = "MAPS")).map { it.id })
        // A chip that was entered from its row: no word of a name was typed, and the scope answers.
        assertEquals(listOf("yt:first", "yt:second", "web:search"), e.search(Query("music daft punk", scope = "yt")).map { it.id })
    }
}
