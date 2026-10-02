package io.github.kuscher.booklight.providers

import android.content.Context
import android.os.UserManager
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.ai.OnDevice
import io.github.kuscher.booklight.core.Act
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.AppChip
import io.github.kuscher.booklight.core.Door
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Plain
import io.github.kuscher.booklight.core.Play
import io.github.kuscher.booklight.core.PlayMode
import io.github.kuscher.booklight.core.PlayRequest
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Site
import io.github.kuscher.booklight.core.Song
import io.github.kuscher.booklight.core.SongFailure
import io.github.kuscher.booklight.core.Spotify
import io.github.kuscher.booklight.core.Templates
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.scopes.Answering
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/**
 * An app as the chip in the field. Enter on Search or on Play on an app's row puts the app there
 * (its own icon and its name), and what is typed then is looked for in that app, or played in it.
 * One chip for an app, whichever of the two is armed: the placeholder and the one row under the
 * field say which, and Tab changes between them.
 *
 * The other ways in end in the same chip and the same row. `play` is the music app last played in,
 * with Play armed ([play]). One of Booklight's own links (`yt`, `maps`, `store`, `drive`) is its app,
 * with Search armed, where that app is installed ([site]). The typed sentence ("spotify daft punk")
 * is the same row in the ordinary list, with no chip ([sentence]).
 *
 * Search: where an app's search goes is `AppSearches`' to know. Nothing leaves the device before Enter.
 *
 * Play: an app that answers Android's "play from search" request (`Players`) and is not known to only
 * show its search results for it. What the player does with the request is its own choice and nothing
 * comes back, so the footer says "Sent to", never "Playing". Spotify only searches by itself: it has
 * Play once the user's own Spotify key is in. Then what Spotify has by the typed name is looked up
 * after a pause in typing ([Songs]), while Play is armed and never under Search; the row says what was
 * found, and Enter plays exactly that with a link of Spotify's own. Enter before the answer has come
 * waits for it. Where nothing was found Play is dimmed and Search, armed in its place, is what Enter runs.
 *
 * A description or a misspelt name finds nothing. Behind the play row's arrow, "Which song is this?"
 * asks the device's own model, where there is one, and shows what it says; Enter on that puts the title
 * and the artist into the field as the new text. The model never rewrites what was typed by itself.
 *
 * Which apps play is read when the app list is read ([read], called by `AppCommands`), so typing asks
 * the package manager nothing.
 */
class AppChips(private val context: Context, private val prefs: Prefs, private val search: AppSearches, private val apps: AppsProvider) {
    private val app get() = context.applicationContext as BooklightApp
    private val me = context.getSystemService(UserManager::class.java).getSerialNumberForUser(android.os.Process.myUserHandle())

    /** The music apps of this device, by name; and one chip for each app that can be searched or is a music app, by package. */
    @Volatile private var players: List<Player> = emptyList()
    @Volatile private var chips: Map<String, Chip> = emptyMap()

    /** The words that are `play`: entered by one of them, the chip stands for whichever music app the text names, and the others stand under it. */
    private val playWords: List<String> = context.getString(R.string.scope_play_keys).split(',')

    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)

    /**
     * Makes the chips for [found], the apps that can be searched, and for the music apps. This asks the
     * package manager which apps play: it is called when the app list has been read, off the main thread.
     */
    fun read(found: Collection<AppSearches.Found>) {
        val music = runCatching { Players.find(context, apps) }.getOrDefault(emptyList())
        val all = LinkedHashMap<String, Chip>()
        for (f in found) all[f.pkg] = Chip(f.pkg, f.app, f.cls, f, music.firstOrNull { it.pkg == f.pkg })
        for (p in music) if (p.pkg !in all) all[p.pkg] = Chip(p.pkg, p.name, p.icon.className, null, p)
        players = music
        chips = all
    }

    /** One chip for each app that has Search or Play just now. (Asked for on every keystroke: the chips themselves are made when the app list is read.) */
    fun scopes(): List<Scope> = chips.values.filter { it.acts.isNotEmpty() }

    /**
     * Search and Play for the row of the app [pkg], in that order, each only if the app has it. Enter
     * on one makes the app the chip with that action armed. None for an app of another profile:
     * nothing can be sent there from here.
     */
    fun actions(pkg: String, user: Long): List<Action> {
        if (user != me) return emptyList()
        val chip = chips[pkg] ?: return emptyList()
        return chip.acts.map { chip.enter(it, "") }
    }

    /** The music apps that have a chip, by name: the ones `play` can go to. */
    private fun music(): List<Chip> = players.mapNotNull { p -> chips[p.pkg]?.takeIf { it.acts.isNotEmpty() } }

    /**
     * `play`: the chip of the music app [text] names at its end ("… on spotify"), else of the one
     * played in last (the first by name the first time), with Play armed. An app that only searches
     * (Spotify without a key) is the chip only where no other plays, and then it searches. Null with
     * no music app on this device.
     */
    fun play(text: String): Door? {
        val all = music()
        if (all.isEmpty()) return null
        val named = Play.read(text, all.map { it.name })?.player ?: -1
        val chip = all.getOrNull(named) ?: all[Play.door(all.map { it.pkg to it.plays }, prefs.now.player)]
        return Door(chip, Act.PLAY)
    }

    /**
     * One of Booklight's own links (`yt`, `maps`, `store`, `drive`) as it came: the chip of the app
     * that searches the same place, with Search armed. Null when that app is not installed or cannot
     * be searched, or the link was changed: then the link opens in the browser, as it says.
     */
    fun site(site: Site): Door? = search.owner(site)?.let { chips[it.pkg] }?.takeIf { Act.SEARCH in it.acts }?.let { Door(it, Act.SEARCH) }

    /**
     * "spotify daft punk": for each app whose whole name starts the text, the row that searches it
     * for the rest, the longest name first. It is the row its chip shows, in the ordinary list. Where
     * it stands there is the engine's to say (`SearchEngine`: the web first, until the app leads).
     */
    fun sentence(text: String): List<Result> = search.named(text).mapNotNull { n -> chips[n.found.pkg]?.searchRow(n.text, null, n.score, n.app, chip = false) }

    /** The app's icon is fetched as soon as its row is made, off this thread: it is there when the chip or the row is first drawn. */
    private fun warm(all: List<Chip>) {
        val icons = app.icons ?: return
        val px = (48 * context.resources.displayMetrics.density).toInt()
        if (all.any { icons.cached(it.icon) == null }) app.scope.launch { for (c in all) icons.load(c.icon, px) }
    }

    /** How the text was read, under the row's name: "Album by daft punk". Free text has no such line. */
    private fun read(what: PlayRequest): String? = when (what.mode) {
        PlayMode.ANY -> null
        PlayMode.SONG -> if (what.artist.isEmpty()) text(R.string.play_song) else text(R.string.play_song_by, what.artist)
        PlayMode.ALBUM -> if (what.artist.isEmpty()) text(R.string.play_album) else text(R.string.play_album_by, what.artist)
        PlayMode.ARTIST -> text(R.string.play_artist)
        PlayMode.PLAYLIST -> text(R.string.play_playlist)
        PlayMode.GENRE -> text(R.string.play_genre)
    }

    /** What Spotify found, under its name: a song's artists and its album ("Queen · A Night At The Opera"), an album's artists, whose playlist it is. */
    private fun said(s: Song): String = when (s.kind) {
        PlayMode.ALBUM -> if (s.artists.isEmpty()) text(R.string.play_album) else text(R.string.play_found_album, s.artists.joinToString(", "))
        PlayMode.ARTIST -> text(R.string.play_artist)
        PlayMode.PLAYLIST -> if (s.owner.isEmpty()) text(R.string.play_playlist) else text(R.string.play_found_playlist, s.owner)
        else -> listOf(s.artists.joinToString(", "), s.album).filter { it.isNotEmpty() }.joinToString(" · ").ifEmpty { text(R.string.play_song) }
    }

    /**
     * The chip of one app. [found]: how it is searched, if it can be. [player]: it is a music app.
     * It has no keyword: an app's name stays a name ("google m" is Google Maps), and the app is made
     * the chip from its row, or by a keyword that is a short way to it.
     */
    inner class Chip internal constructor(val pkg: String, override val name: String, cls: String, private val found: AppSearches.Found?, private val player: Player?) : AppChip, Answering {
        override val key = SearchEngine.IN_APP + pkg
        override val keywords = emptyList<String>()
        /** (Where a chip is drawn without the app's own icon.) */
        override val symbol = "search"
        override val icon = Icon.App(pkg, cls, me)
        override val listed = false
        override val hint: String get() = hint(acts.firstOrNull() ?: Act.SEARCH)
        /** No row is asked after a pause in typing: the model is asked on Enter only ("Which song is this?"), and not loaded before. */
        override val row: String? = null
        override val eager = false

        /** How many times the rows have been asked for: only the latest asking says what a row waits for. One for an earlier text can end after the next one's. */
        private val askings = AtomicInteger()
        /** The music as it was last typed, and the app named after it (" on Spotify", " auf Spotify"; empty for none): what the model is asked about, and what its answer is typed before. */
        @Volatile private var typed = "" to ""
        /** The text "Use this" puts into the field: a name as the model gave it, which is read as it stands ("Song 2 Blur" has no kind word). Edited, it is the user's own text again. */
        @Volatile private var given: String? = null

        /** The app's search, while the switch for what other apps offer allows it. */
        private val searched: AppSearches.Found? get() = found?.takeIf { search.allowed(it) }

        /**
         * The app plays what is named. One that is known to only show its search results does not;
         * Spotify does once the user's key is in: what it has is then looked up, and a link of its own plays it.
         */
        val plays: Boolean get() = player != null && (!player.searches || (pkg == Spotify.PACKAGE && app.songs.ready))

        override val acts: List<Act> get() = listOfNotNull(Act.SEARCH.takeIf { searched != null }, Act.PLAY.takeIf { plays })

        // A keyword of the app's own Booklight file keeps the file's words ("Find a note").
        override fun hint(act: Act): String = when (act) {
            Act.SEARCH -> found?.hint ?: text(R.string.scope_site_hint, name)
            Act.PLAY -> text(R.string.scope_play_hint)
        }

        override fun offer(act: Act): String = when (act) {
            Act.SEARCH -> found?.name ?: text(R.string.scope_site_hint, name)
            Act.PLAY -> text(R.string.play_in, name)
        }

        /** [act] as an action that makes this app the chip with it armed, [text] as what is typed there: on the app's row, and beside the other action on the row under the chip. */
        internal fun enter(act: Act, text: String) = Action(
            act.id, if (act == Act.PLAY) text(R.string.action_play) else found?.name ?: text(R.string.action_search),
            Effect.EnterScope(key, text, act), keepOpen = true, symbol = act.id,
        )

        override suspend fun rows(arg: String, act: Act, word: String?): List<Result> {
            val asking = askings.incrementAndGet()
            val text = arg.trim()
            if (text.isEmpty() || act == Act.SEARCH) {
                // Nothing typed: no row. Under Search nothing is looked up, and a row that stood waiting for Spotify waits no more.
                if (asking == askings.get()) app.songs.settle(key)
                return if (text.isEmpty()) emptyList() else listOfNotNull(searchRow(text, word))
            }
            return played(arg, word) { asking == askings.get() }
        }

        /**
         * The row that searches this app for [text]: named like the web's row, with the app's own
         * icon; Play, where the app has it, is the next action. [word]: the keyword the chip was
         * entered by. [by]: the name the app was typed by, in a sentence. [chip]: the app is the chip
         * in the field; false for the typed sentence, which has nobody to put a question to.
         */
        internal fun searchRow(text: String, word: String?, score: Double = 1.0, by: String = name, chip: Boolean = true): Result? {
            val f = searched ?: return null
            if (chip) typed = text to ""
            // One of Booklight's own links that stands for this app (`yt`): the same search in the browser waits behind the arrow.
            val url = word?.let { w -> prefs.now.sites().firstOrNull { it.keyword.equals(w, ignoreCase = true) } }
                ?.takeIf { search.owner(it)?.pkg == pkg }?.let { Templates.fill(it.url, text, "", LocalDate.now()) }
            return Result(
                id = key, provider = SearchEngine.COMMANDS, kind = Kind.COMMAND,
                title = if (f.name != null) "${f.name}: $text" else text(R.string.app_search_title, by, text),
                // Where the line does not fit, the app's name gives way, not the words: the mark and the chip name the app.
                brief = if (f.name != null) null else text(R.string.app_search_brief, text),
                // It is the text itself, like a web search: nothing to learn, and nothing for the rows under the empty field.
                icon = icon, score = score, label = by, learnable = false,
                actions = listOfNotNull(
                    Action(Act.SEARCH.id, text(if (f.name != null) R.string.action_open else R.string.action_search), search.open(f, text), symbol = if (f.name != null) "open" else "search"),
                    // Play is the same row with other words: the app becomes the chip (or stays it) with Play armed and this text.
                    enter(Act.PLAY, text).takeIf { plays },
                    url?.let { Action("web", text(R.string.action_on_web), Effect.OpenUrl(it), symbol = "globe", more = true) },
                    url?.let { Action("link", text(R.string.action_copy_link), Effect.CopyText(it), more = true) },
                    // The question for the model stands behind the arrow under Search as under Play: the strip is the same for both.
                    which(text).takeIf { chip && plays },
                    // The way to where the key goes, for Spotify while it has none.
                    Action("setup", text(R.string.play_set_up), Songs.KEY, symbol = "settings", more = true).takeIf { pkg == Spotify.PACKAGE && player != null && !app.songs.ready },
                ),
            )
        }

        /**
         * The rows that play [arg]: this app's own, and, where the chip was entered by `play`, one for
         * each other music app that plays, under it. Entered from the app's own row only this app is shown.
         * [latest]: whether these rows are still the latest that were asked for.
         */
        private suspend fun played(arg: String, word: String?, latest: () -> Boolean): List<Result> {
            val songs = app.songs
            val door = word != null && playWords.any { it.equals(word, ignoreCase = true) }
            val all = if (door) music() else emptyList()
            val what = Play.read(arg, all.map { it.name }, kinds = arg.trim() != given) ?: return emptyList<Result>().also { if (latest()) songs.settle(key) }
            val others = all.filter { it !== this && it.plays }
            warm(listOf(this) + others)
            // (The app is named again in the app's own language, with a word the reader takes: "on", "auf".)
            typed = what.query to (all.getOrNull(what.player)?.let { " ${text(R.string.play_on)} ${it.name}" } ?: "")
            // The keyword and the text may be the start of an app's name ("play store"): that app's row then stands above this
            // one, and the text is sent nowhere unasked.
            val name = word != null && apps.best("$word ${arg.trim()}") != null
            // Chosen: from the app's own row, named in the text, played in last, or the only one that plays. An app that is
            // the chip merely for being first by name is asked nothing until Enter (the privacy text says so).
            val chosen = !door || what.player >= 0 || prefs.now.player == pkg || others.isEmpty()
            /** A row waits for what Spotify has, or for nothing: said only by the latest asking, and a wait is only ended by the one it was made for. */
            fun waits(id: String): (PlayRequest?, Boolean) -> Unit = { wanted, unasked ->
                if (latest()) {
                    if (wanted != null) songs.wait(id, wanted, unasked) { withContext(Dispatchers.Default) { rows(arg, Act.PLAY, word).firstOrNull { it.id == id } } }
                    else songs.settle(id)
                }
            }
            return listOf(playRow(what, own = true, unasked = !chosen || name, waits(key))) + others.map { it.playRow(what, own = false, unasked = false, waits(it.key)) }
        }

        /**
         * The row that plays [what] in this app: its mark is the app's icon and its action reads Play.
         * [own]: it is the chip's own row, with Search beside Play and the question for the model behind
         * its arrow; else one of the other music apps under `play`. [unasked]: what Spotify has is looked
         * up on Enter only, not after a pause in typing. [wait]: says what the row waits for.
         */
        private fun playRow(what: PlayRequest, own: Boolean, unasked: Boolean, wait: (PlayRequest?, Boolean) -> Unit): Result {
            val songs = app.songs
            val sent = text(R.string.play_sent, name)
            fun ask(link: String = "") = Effect.PlayMusic(what.query, pkg, what.mode, what.title, what.artist, what.album, what.playlist, what.genre, link)
            // Spotify plays a link of its own, which is looked up: until that is known the row waits. Any other player is sent the request.
            val spotify = pkg == Spotify.PACKAGE && songs.ready
            // (Not unasked for a single letter: Enter asks then. One of the other apps' rows is asked when the user goes to it.)
            val late = unasked || what.query.length < 2
            val known = if (spotify) songs.known(what) else null
            wait(what.takeIf { spotify && known == null }, late)
            val found = known?.song
            val failed = known != null && found == null
            val play = Action(Act.PLAY.id, text(R.string.action_play), if (!spotify) ask() else found?.let { ask(it.address) } ?: Songs.WAIT, symbol = "play", done = sent, off = failed)
            val f = searched.takeIf { own }
            val actions = listOfNotNull(
                // Search for the same words: with nothing found it is armed in Play's place, so an Enter that waited for the answer searches.
                f?.let { Action(Act.SEARCH.id, text(if (it.name != null) R.string.action_open else R.string.action_search), search.open(it, what.query), symbol = if (it.name != null) "open" else "search") },
                play,
                // With a key that was refused, the way to where the key goes is on the row.
                Action("key", text(R.string.action_flight_key), Songs.KEY, symbol = "settings").takeIf { known?.failure == SongFailure.REFUSED },
                which(what.query).takeIf { own },
            )
            return Result(
                // Before an answer the row says what was asked for; with one, what Spotify found, under its own name.
                id = key, provider = Songs.PROVIDER, kind = Kind.OTHER, title = found?.name ?: text(R.string.play_title, what.name), icon = icon, score = 1.0, label = name, learnable = false,
                subtitle = when {
                    !spotify -> read(what)
                    // (Only the chip's own row is looked up after a pause in typing, and says so.)
                    known == null -> if (own && !late) text(R.string.play_looking) else read(what)
                    found != null -> said(found)
                    else -> text(when (known.failure) {
                        SongFailure.NOT_FOUND -> R.string.play_none
                        SongFailure.REFUSED -> R.string.play_refused
                        SongFailure.TOO_MANY -> R.string.play_too_many
                        SongFailure.OFFLINE -> R.string.play_offline
                        else -> R.string.play_no_answer
                    })
                },
                armed = actions.indexOfFirst { it.id == if (failed && f != null) Act.SEARCH.id else Act.PLAY.id }.coerceAtLeast(0),
                actions = actions,
            )
        }

        /** "Which song is this?", behind the arrow, where the device has a model to ask: [text] is what it is asked about. */
        private fun which(text: String): Action? = text(R.string.play_which).takeIf { app.onDevice.state.value == OnDevice.State.READY }
            ?.let { Action("which", it, Effect.Ask(Play.question(text), it), keepOpen = true, symbol = "spark", more = true) }

        /**
         * The row while the model is asked. It is an ordinary row from its first frame, and stays one: the answer is one
         * line ("Bohemian Rhapsody / Queen"), not a text that needs an answer's room. Until the answer is whole it offers
         * nothing to run. It is the model's answer, not the app's: its mark is a symbol.
         */
        override fun asking(r: Result, e: Effect.Ask): Result = Result(
            id = "play:which", provider = key, kind = Kind.OTHER, title = e.name, subtitle = text(R.string.play_looking), icon = Icon.Symbol("music"), score = 1.0, learnable = false,
            actions = listOf(Action("ask", text(R.string.action_ask), e, keepOpen = true, symbol = "spark")),
        )

        /**
         * What the model said, as the row's name: the title and the artist ("Bohemian Rhapsody · Queen"), or its line as it
         * came where that does not divide. Enter puts it into the field in place of what was typed, before the app that
         * was named: nothing is played by this. Copy is next.
         */
        override fun answered(r: Result, text: String, busy: Boolean): Result {
            val said = Plain.of(text).lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
            val parts = Play.song(said)
            val title = if (parts.size == 2) "${parts[0]} · ${parts[1]}" else said
            if (busy) return r.copy(title = title.ifEmpty { r.title })
            return r.copy(
                title = title, subtitle = text(R.string.prompt_device),
                actions = listOfNotNull(
                    Play.named(said)?.let { name -> Action("use", text(R.string.play_use), Effect.EnterScope(key, (name + typed.second).also { given = it }, Act.PLAY), keepOpen = true, symbol = "check") },
                    Action("copy", text(R.string.action_copy), Effect.CopyText(said)),
                ),
            )
        }

        override fun unanswered(r: Result): Result = r.copy(subtitle = text(R.string.prompt_failed), actions = emptyList())
    }

    companion object {
        /** The hook for an app's own row (`AppsProvider`): its Search and its Play, each if the app has it. */
        fun actions(context: Context, pkg: String, user: Long): List<Action> = (context.applicationContext as BooklightApp).commands.chips.actions(pkg, user)
    }
}
