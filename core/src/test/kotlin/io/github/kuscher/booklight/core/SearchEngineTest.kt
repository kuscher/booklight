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

    /** A line read as a sentence (an event typed the way it is said), and the question for Gemini that ranks as a match where the line is five words long. */
    private fun sentence(score: Double = 0.5) = object : Provider {
        override val id = "events"
        override suspend fun query(q: Query) = listOf(Result(SearchEngine.SENTENCE + "event", id, Kind.OTHER, "Event", icon = Icon.Symbol("event"), score = score,
            actions = listOf(Action("create", "Create", Effect.InsertEvent("x", 0, 1, false, ""))), learnable = false))
    }
    private val gemini = object : Provider {
        override val id = "gemini"
        override suspend fun query(q: Query) = listOf(Result("web:gemini", id, Kind.WEB, "Ask Gemini", icon = Icon.Symbol("spark"), score = 0.79,
            actions = listOf(Action("ask", "Ask", Effect.AskGemini(q.text))), learnable = false))
    }

    @Test fun aSentenceStandsUnderWhatTheDeviceMatchesAndOverTheWeb() = runTest {
        // Nothing of the device matches: the sentence's row is row one, over Gemini's question and the web search.
        val r = engine(History(), sentence(), gemini).search(Query("add dinner with sam tomorrow at 7pm"))
        assertEquals(listOf("sentence:event", "web:gemini", "web:search"), r.map { it.id })
        // Something of the device matches, however weakly, and however the sentence's row is scored: that comes first.
        for (score in listOf(0.01, 0.5, 1.0, 5.0)) {
            val both = engine(History(), sentence(score), gemini).search(Query("ca"))
            assertEquals("$score", listOf("app:Camera", "app:Canvas", "app:Calendar", "app:Calculator", "sentence:event", "web:gemini", "web:search"), both.map { it.id })
        }
        // An answer is the device's too.
        assertEquals(listOf("calc", "sentence:event", "web:gemini", "web:search"), engine(History(), sentence(), gemini).search(Query("2+2")).map { it.id })
    }

    @Test fun aSentencesRowIsNeverCrowdedOut() = runTest {
        // A list too short for everything: the device's rows give way, the sentence's row and the web's keep their places.
        val r = engine(History(), sentence(), gemini).search(Query("ca"), limit = 4)
        assertEquals(listOf("app:Camera", "app:Canvas", "sentence:event", "web:search"), r.map { it.id })
        assertEquals(listOf("sentence:event", "web:search"), engine(History(), sentence(), gemini).search(Query("ca"), limit = 2).map { it.id })
    }

    @Test fun theWebsOwnRowsStandUnderASentenceAndASentenceIsNotLearned() = runTest {
        // A typed address is the web's, like the question for Gemini: under the sentence's row, over the web search.
        val address = object : Provider {
            override val id = "web"
            override suspend fun query(q: Query) = listOf(Result("web:url", id, Kind.WEB, "example.com", icon = Icon.Symbol("globe"), score = 0.95, actions = listOf(Action("open", "Open", Effect.OpenUrl("https://example.com")))))
        }
        assertEquals(listOf("sentence:event", "web:url", "web:search"), engine(History(), sentence(5.0), address).search(Query("add dinner tomorrow 7pm at example.com")).map { it.id })
        // Running the sentence's row teaches nothing: what was typed is the user's appointment, and is kept nowhere.
        val h = History()
        val e = engine(h, sentence())
        val q = Query("add dinner with sam tomorrow at 7pm")
        val row = e.search(q).first()
        assertEquals("sentence:event", row.id)
        e.picked(q, row); e.picked(q, row)
        assertTrue(h.items().isEmpty())
        assertEquals(History.Data(), h.data())
    }

    @Test fun withoutASentenceTheOrderIsAsItWas() = runTest {
        // Gemini's question for a line that reads as one ranks as a match: over a weaker local row, as before.
        val r = engine(History(), gemini).search(Query("clr"))
        assertEquals(listOf("web:gemini", "app:Calendar", "app:Calculator", "web:search"), r.map { it.id })
        // With one, those local rows stand over it, and Gemini's question under it.
        assertEquals(listOf("app:Calendar", "app:Calculator", "sentence:event", "web:gemini", "web:search"), engine(History(), sentence(), gemini).search(Query("clr")).map { it.id })
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

    private val apps = listOf("Spotify", "Spotify Desktop", "Google", "Google Maps", "Google Meet", "YouTube", "YouTube Music", "YT Music", "Maps", "Play Store")
    private val searchable = listOf("Spotify", "Google", "YouTube", "YouTube Music", "YT Music", "Maps", "Play Store")
    private val webFor: (String) -> List<Result> = { text -> listOf(Result("web:search", "web", Kind.WEB, "Search for $text", icon = Icon.Symbol("search"), score = 0.1,
        actions = listOf(Action("search", "Search", Effect.OpenUrl(Engines.default.search(text)))), learnable = false)) }

    private fun searched(history: History, vararg scopes: Scope) = SearchEngine(
        listOf(Names(apps, ::app), web, InApps(searchable)), history, clock = { 1000 }, scopes = { scopes.toList() }, fallback = webFor,
    )
    private fun searched(vararg scopes: Scope) = searched(History(), *scopes)

    /** A history in which the search inside each of these apps has been picked once: they lead for words after their names. */
    private fun leading(vararg names: String) = History().also { h -> for (n in names) h.led(n, inApp = true) }

    @Test fun `words after an app's name go to the web first, with the search inside the app directly under it`() = runTest {
        assertEquals(listOf("Search the web", "Search Spotify for daft punk"), searched().search(Query("spotify daft punk")).map { it.title })
        // Both readings of a name of two words, the longer name first.
        assertEquals(listOf("Search the web", "Search YouTube Music for daft punk", "Search YouTube for music daft punk"), searched().search(Query("youtube music daft punk")).map { it.title })
        // It keeps its place under the web's row when the list is full.
        assertEquals(listOf("web:search", "appsearch:Spotify"), searched().search(Query("spotify d"), limit = 2).map { it.id })
    }

    @Test fun `once its row was picked an app leads for words after its name`() = runTest {
        val h = History()
        val e = searched(h)
        val first = e.search(Query("spotify daft punk"))
        assertEquals(listOf("web:search", "appsearch:Spotify"), first.map { it.id })
        e.led(first[1], first)
        assertTrue(e.leads(first[1]))
        assertEquals(listOf("appsearch:Spotify", "web:search"), e.search(Query("spotify daft punk")).map { it.id })
        assertEquals(listOf("appsearch:Spotify", "web:search"), e.search(Query("spotify queen")).map { it.id })
        // For that app only.
        assertEquals(listOf("web:search", "appsearch:Maps"), e.search(Query("maps coffee")).map { it.id })
        // And each reading by its own app: YouTube Music leads, YouTube does not.
        val two = searched(leading("YouTube Music"))
        assertEquals(listOf("appsearch:YouTube Music", "web:search", "appsearch:YouTube"), two.search(Query("youtube music daft punk")).map { it.id })
    }

    @Test fun `the web's row picked twice running takes the lead back`() = runTest {
        val h = leading("Spotify")
        val e = searched(h)
        val rows = e.search(Query("spotify stock price"))
        val webRow = rows.first { it.id == "web:search" }
        e.led(webRow, rows)
        assertEquals("appsearch:Spotify", e.search(Query("spotify stock price")).first().id)     // once is not enough
        e.led(rows.first(), rows)                                                                   // the app's row in between: the count starts again
        e.led(webRow, rows)
        assertEquals("appsearch:Spotify", e.search(Query("spotify stock price")).first().id)
        e.led(webRow, rows)
        assertEquals(listOf("web:search", "appsearch:Spotify"), e.search(Query("spotify stock price")).map { it.id })
        // The web's row picked for a text that names no app teaches nothing.
        val other = searched(leading("Spotify"))
        val plain = other.search(Query("weather"))
        repeat(3) { other.led(plain.first { it.id == "web:search" }, plain) }
        assertEquals("appsearch:Spotify", other.search(Query("spotify queen")).first().id)
        // "Forget everything" forgets this too.
        h.led("Spotify", inApp = true); h.clear()
        assertEquals("web:search", searched(h).search(Query("spotify queen")).first().id)
    }

    @Test fun `who leads is kept with what is learned`() {
        val h = leading("Spotify")
        assertTrue(History(h.data()).leads("Spotify"))
        assertTrue(!History(h.data()).leads("Netflix"))
        assertEquals(mapOf("Spotify" to Lead.BACK), h.data().leads)
    }

    @Test fun `a search inside an app never stands above a local match`() = runTest {
        // An app whose name is the whole text is still row one, on every letter of the way: with the web leading and with the app leading.
        assertEquals(listOf("app:Spotify Desktop", "web:search", "appsearch:Spotify"), searched().search(Query("spotify d")).map { it.id })
        assertEquals(listOf("app:Spotify Desktop", "appsearch:Spotify", "web:search"), searched(leading("Spotify")).search(Query("spotify d")).map { it.id })
        assertEquals(listOf("app:Google Maps", "app:Google Meet", "appsearch:Google", "web:search"), searched(leading("Google")).search(Query("google m")).map { it.id })
        assertEquals(listOf("app:Google Maps", "appsearch:Google", "web:search"), searched(leading("Google")).search(Query("google maps")).map { it.id })
        // The name alone, with or without a space after it, is the app.
        assertEquals(listOf("app:Spotify", "app:Spotify Desktop", "web:search"), searched().search(Query("spotify ")).map { it.id })
        // Not even once it has been run for that very text: it is the text itself, and nothing is learned from it.
        val history = leading("Google")
        val e = SearchEngine(listOf(Names(listOf("Google Maps"), ::app), web, InApps(listOf("Google"))), history, clock = { 1000 })
        val row = e.search(Query("google m")).first { it.id == "appsearch:Google" }
        repeat(5) { e.picked(Query("google m"), row) }
        assertEquals("app:Google Maps", e.search(Query("google m")).first().id)
    }

    @Test fun `under a keyword's chip an app's name that goes on into the text is searched first where that app leads`() = runTest {
        val yt = Words("yt", listOf("yt"), "YouTube")
        val play = Words("play", listOf("play"), "Play")
        val maps = Words("maps", listOf("maps"), "Maps")
        val e = searched(leading("YT Music"), yt, play, maps)
        // "yt music daft punk": YT Music is an app, and "daft punk" is looked for there before YouTube is searched for all of it.
        assertEquals(listOf("appsearch:YT Music", "yt:first", "yt:second", "web:search"), e.search(Query("music daft punk", scope = "yt", keyword = "yt")).map { it.id })
        // An app that does not lead for words after its name: the keyword's own rows first, then that search.
        assertEquals(listOf("play:first", "play:second", "appsearch:Play Store", "web:search"), e.search(Query("store calculator", scope = "play", keyword = "play")).map { it.id })
        // The name alone is the app, as before; and then its search.
        assertEquals("app:YT Music", e.search(Query("music", scope = "yt", keyword = "yt")).first().id)
        // An app called what the keyword is called: the keyword was typed, and its scope answers.
        assertEquals(listOf("maps:first", "maps:second", "web:search"), e.search(Query("coffee", scope = "maps", keyword = "maps")).map { it.id })
        assertEquals(listOf("maps:first", "maps:second", "web:search"), e.search(Query("coffee", scope = "maps", keyword = "MAPS")).map { it.id })
        // A chip that was entered from its row: no word of a name was typed, and the scope answers.
        assertEquals(listOf("yt:first", "yt:second", "web:search"), e.search(Query("music daft punk", scope = "yt")).map { it.id })
    }

    // ---- an app's chip

    /** An app as the chip: one row for what is typed, which says which of its actions is armed and the word it was entered by. */
    private class Chip(private val app: String, override val acts: List<Act>) : AppChip {
        override val key = SearchEngine.IN_APP + app
        override val keywords = emptyList<String>()
        override val name = app
        override val symbol = "search"
        override val hint = "…"
        override val listed = false
        override val icon = Icon.App(app, app)
        override fun hint(act: Act) = act.id
        override fun offer(act: Act) = act.id
        override suspend fun rows(arg: String, act: Act, word: String?) = if (arg.isBlank()) emptyList() else listOf(
            Result(key, if (act == Act.PLAY) "play" else SearchEngine.COMMANDS, Kind.COMMAND, "${act.id} $arg in $app${word?.let { " by $it" } ?: ""}", icon = icon, score = 1.0,
                actions = listOf(Action("search", "Search", Effect.Open(app, "intent:#Intent;end"))), learnable = false, label = app))
    }

    /** A keyword that is a short way into an app's chip. */
    private class Way(override val key: String, word: String, private val to: (String) -> Door?) : Scope {
        override val keywords = listOf(word)
        override val name = word
        override val symbol = "music"
        override val hint = "…"
        override fun door(text: String) = to(text)
        override suspend fun rows(arg: String) = listOf(Result("$key:own", key, Kind.OTHER, "own $arg", icon = Icon.Symbol("music"), score = 1.0, actions = emptyList(), learnable = false))
    }

    @Test fun `an app's chip ends with the web's row for the app's name and the text`() = runTest {
        val spotify = Chip("Spotify", listOf(Act.SEARCH, Act.PLAY))
        val e = searched(spotify)
        val r = e.search(Query("daft punk", scope = spotify.key))
        assertEquals(listOf("search daft punk in Spotify", "Search for spotify daft punk"), r.map { it.title })
        // Entered from the app's row no word of a name was typed: no other app is read into the text ("google", then "maps").
        val google = Chip("Google", listOf(Act.SEARCH))
        assertEquals(listOf("appsearch:Google", "web:search"), searched(google).search(Query("maps", scope = google.key)).map { it.id })
        // Nothing typed: no row.
        assertTrue(e.search(Query("", scope = spotify.key)).isEmpty())
    }

    @Test fun `an app's chip is asked for the action that is armed`() = runTest {
        val spotify = Chip("Spotify", listOf(Act.SEARCH, Act.PLAY))
        val e = searched(spotify)
        assertEquals("play queen in Spotify", e.search(Query("queen", scope = spotify.key, act = Act.PLAY)).first().title)
        assertEquals("search queen in Spotify", e.search(Query("queen", scope = spotify.key, act = Act.SEARCH)).first().title)
        assertEquals("search queen in Spotify", e.search(Query("queen", scope = spotify.key)).first().title)            // its first, when none is named
        // An action the app does not have: the one it has.
        val netflix = Chip("Netflix", listOf(Act.SEARCH))
        assertEquals("search dune in Netflix", searched(netflix).search(Query("dune", scope = netflix.key, act = Act.PLAY)).first().title)
        // An app that has neither any more has no rows, only the way out.
        val gone = Chip("Gone", emptyList())
        assertEquals(listOf("web:search"), searched(gone).search(Query("x", scope = gone.key)).map { it.id })
    }

    @Test fun `a keyword that is a short way into an app's chip is answered by that chip`() = runTest {
        val spotify = Chip("Spotify", listOf(Act.SEARCH, Act.PLAY))
        val music = Chip("YT Music", listOf(Act.PLAY))
        // `play`: the app played in last, with Play armed; "… on yt music" at the end names another.
        val play = Way("play", "play") { text -> Door(if (text.endsWith(" on yt music")) music else spotify, Act.PLAY) }
        val e = searched(play, spotify, music)
        val r = e.search(Query("bohemian rhapsody", scope = "play", keyword = "play"))
        assertEquals(listOf("play bohemian rhapsody in Spotify by play", "Search for play bohemian rhapsody"), r.map { it.title })
        assertEquals("appsearch:YT Music", e.search(Query("queen on yt music", scope = "play", keyword = "play")).first().id)
        // Tab under the chip arms the other action: the same chip, asked for Search.
        assertEquals("search queen in Spotify by play", e.search(Query("queen", scope = "play", keyword = "play", act = Act.SEARCH)).first().title)
        // The keyword was the first word of an app's name: that app still comes first.
        assertEquals("app:Play Store", e.search(Query("store", scope = "play", keyword = "play")).first().id)
        // A keyword with no app to lead to is its own chip.
        val none = Way("play", "play") { null }
        assertEquals("play:own", searched(none).search(Query("queen", scope = "play", keyword = "play")).first().id)
    }
}
