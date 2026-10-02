package io.github.kuscher.booklight.providers

import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.provider.MediaStore
import android.provider.Settings
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.Executor
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.ai.OnDevice
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.MediaKey
import io.github.kuscher.booklight.core.Nudge
import io.github.kuscher.booklight.core.Play
import io.github.kuscher.booklight.core.PlayMode
import io.github.kuscher.booklight.core.PlayRequest
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Plain
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.Song
import io.github.kuscher.booklight.core.SongFailure
import io.github.kuscher.booklight.core.Spotify
import io.github.kuscher.booklight.scopes.Answering
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Controls as rows: the volume and the brightness as levels (Left and Right change them at once,
 * the panel stays), and the media keys. A level is read fresh for every list, so the row
 * always shows what the device is at.
 */
class Dials(private val context: Context, private val executor: Executor) : Provider {
    override val id = "dials"

    private fun words(id: Int) = context.getString(id).split(',').filter(String::isNotBlank)
    private fun match(text: String, title: String, words: List<String>) = maxOf(Matcher.score(text, title), words.maxOfOrNull { Matcher.keyword(text, it) } ?: 0.0)

    override suspend fun query(q: Query): List<Result> {
        val out = ArrayList<Result>(3)
        match(q.text, context.getString(R.string.dial_volume), words(R.string.dial_volume_words)).let { if (it > 0) out.add(volume(it, null)) }
        match(q.text, context.getString(R.string.dial_brightness), words(R.string.dial_brightness_words)).let { if (it > 0) out.add(brightness(it, null)) }
        val media = match(q.text, context.getString(R.string.dial_media), words(R.string.dial_media_words) + words(R.string.dial_play_words) + words(R.string.dial_pause_words) + words(R.string.dial_stop_words))
        if (media > 0) out.add(media(media, q.text))
        return out
    }

    fun volume(score: Double, target: Int?): Result {
        val audio = executor.audio
        val muted = audio.muted
        val set = target?.let { Action("set", context.getString(R.string.action_set_level, it), Effect.SetVolume(it), symbol = "check") }
        return Result(
            id = "dial:volume", provider = id, kind = Kind.CONTROL, title = context.getString(R.string.dial_volume),
            icon = Icon.Symbol(if (muted) "mute" else "volume"), score = score, learnable = target == null,
            body = Body.Level(audio.percent, target = target, muted = muted),
            nudge = Nudge(Effect.SetVolume(audio.step(-1)), Effect.SetVolume(audio.step(1))),
            actions = listOfNotNull(
                set,
                Action("mute", context.getString(if (muted) R.string.action_unmute else R.string.action_mute), Effect.ToggleMute, symbol = if (muted) "volume" else "mute"),
                Action("settings", context.getString(R.string.action_sound_settings), Effect.OpenSettings(Settings.ACTION_SOUND_SETTINGS), symbol = "settings"),
            ),
        )
    }

    fun brightness(score: Double, target: Int?): Result {
        val screen = executor.screen
        val allowed = screen.allowed
        val set = target?.takeIf { allowed }?.let { Action("set", context.getString(R.string.action_set_level, it), Effect.SetBrightness(it), symbol = "check") }
        return Result(
            id = "dial:brightness", provider = id, kind = Kind.CONTROL, title = context.getString(R.string.dial_brightness),
            subtitle = if (allowed) null else context.getString(R.string.dial_needs_grant),
            icon = Icon.Symbol("sun"), score = score, learnable = target == null,
            body = Body.Level(screen.percent, target = target, locked = !allowed),
            nudge = Nudge(Effect.SetBrightness(screen.step(-1)), Effect.SetBrightness(screen.step(1))),
            actions = listOfNotNull(
                set,
                if (allowed) Action("done", context.getString(R.string.action_done), Effect.Internal("done"), symbol = "check")
                else Action("allow", context.getString(R.string.action_allow), Effect.Grant("brightness"), symbol = "lock"),
                Action("settings", context.getString(R.string.action_display_settings), Effect.OpenSettings(Settings.ACTION_DISPLAY_SETTINGS), symbol = "settings"),
            ),
        )
    }

    /**
     * Previous, play or pause, next. What is playing can't be read without notification access, so the middle one
     * says both, unless the typed word says which: `pause` sends the key that can only pause, `play` the one that
     * only resumes (whatever played last), `stop` the one that stops.
     */
    private fun media(score: Double, text: String): Result {
        fun typed(id: Int) = words(id).any { Matcher.keyword(text, it) > 0 }
        val wants = when {
            typed(R.string.dial_next_words) -> 2
            typed(R.string.dial_previous_words) -> 0
            else -> 1
        }
        val middle = when {
            typed(R.string.dial_stop_words) -> Action("stop", context.getString(R.string.action_stop), Effect.Media(MediaKey.STOP))
            typed(R.string.dial_pause_words) -> Action("pause", context.getString(R.string.action_pause), Effect.Media(MediaKey.PAUSE))
            typed(R.string.dial_play_words) -> Action("resume", context.getString(R.string.action_play), Effect.Media(MediaKey.PLAY), symbol = "play")
            else -> Action("play", context.getString(R.string.action_play_pause), Effect.Media(MediaKey.PLAY_PAUSE), symbol = if (executor.audio.playing) "pause" else "play")
        }
        return Result(
            id = "dial:media", provider = id, kind = Kind.CONTROL, title = context.getString(R.string.dial_media),
            subtitle = context.getString(R.string.dial_media_sub), icon = Icon.Symbol("music"), score = score, armed = wants,
            nudge = Nudge(Effect.Media(MediaKey.PREVIOUS), Effect.Media(MediaKey.NEXT)),
            actions = listOf(
                Action("previous", context.getString(R.string.action_previous), Effect.Media(MediaKey.PREVIOUS), keepOpen = true),
                middle,
                Action("next", context.getString(R.string.action_next), Effect.Media(MediaKey.NEXT), keepOpen = true),
            ),
        )
    }
}

/** `vol 40`, `brightness 60`: the level as a row, with the typed value marked on its track; Enter sets it. */
class LevelScope(private val context: Context, private val dials: Dials, private val volume: Boolean) : Scope {
    override val key = if (volume) "vol" else "brightness"
    override val keywords: List<String> = context.getString(if (volume) R.string.scope_volume_keys else R.string.scope_brightness_keys).split(',')
    override val name: String = context.getString(if (volume) R.string.dial_volume else R.string.dial_brightness)
    override val symbol = if (volume) "volume" else "sun"
    override val hint: String = context.getString(R.string.scope_level_hint)
    override val listed = false

    override suspend fun rows(arg: String): List<Result> {
        val target = arg.trim().removeSuffix("%").trim().toIntOrNull()?.takeIf { it in 0..100 }
        return listOf(if (volume) dials.volume(1.0, target) else dials.brightness(1.0, target))
    }
}

/**
 * A music app: one that answers "play this", among the apps Booklight sees. Its [name] and [icon] are
 * the device's own. [searches]: it is known to show its search results for the request and never to
 * start playing, and its action says so.
 */
class Player(val pkg: String, val name: String, val icon: Icon.App, val searches: Boolean)

object Players {
    /**
     * Players that were seen to only search: asked to play something, in any mode, they open on their
     * search results for the text (docs/research/intents.md, "Tried on a Googlebook"). One that is not
     * listed here reads "Play in …": what it does is still its own choice.
     */
    private val SEARCH_ONLY = setOf(Spotify.PACKAGE)

    /**
     * The players on this device, by name. One question to the system, and no new visibility: an app
     * Booklight does not list is not a player. Of each app the activity the system would give the
     * request to counts, and only if any app may start it and it asks for no permission, which is
     * what `Executor` checks again before it starts it.
     */
    fun find(context: Context, apps: AppsProvider): List<Player> {
        val me = context.getSystemService(UserManager::class.java).getSerialNumberForUser(android.os.Process.myUserHandle())
        val seen = HashSet<String>()
        return context.packageManager.queryIntentActivities(Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH), 0)
            .mapNotNull { it.activityInfo }
            .filter { seen.add(it.packageName) && it.exported && it.permission == null }
            .mapNotNull { a -> apps.shown(a.packageName)?.let { (name, cls) -> Player(a.packageName, name, Icon.App(a.packageName, cls, me), a.packageName in SEARCH_ONLY) } }
            .sortedBy { it.name.lowercase() }
    }
}

/**
 * `play lofi beats`, `play album discovery by daft punk on spotify`: asks a music app for it. One
 * row, with an action for each player on the device, under the app's own name and icon. Armed is the
 * player named in the text, else the one asked last, else the first. With no player on the device
 * the row says so and offers nothing. `play` alone is the Play key.
 *
 * What the player does with the request is its own choice and nothing comes back, so an action
 * never promises more than is known ("Search in Spotify"), and the footer says "Sent to", never
 * "Playing".
 *
 * Spotify plays a link of its own. With the user's key for Spotify in ([Songs]) and Spotify the
 * player the user chose (named, asked last, or the only one), the row is looked up after a pause in
 * typing and says what Spotify found: its
 * action then reads "Play in Spotify" and sends that link, and "Search in Spotify" is the next one.
 * Enter before the answer has come waits for it. Where nothing was found or no answer came, the row
 * says why and Enter searches, as it does without a key.
 *
 * A description or a misspelt name ("that queen song about the poor boy") finds nothing. Behind the
 * row's arrow, "Which song is this?" asks the device's own model, where there is one, and shows what
 * it says; Enter on that puts the title and the artist into the field as the new text, and the row
 * then says what Spotify finds for it. The model never rewrites what was typed by itself: it also
 * turns a right title into another song.
 */
class PlayScope(private val context: Context) : Scope, Answering {
    private val app get() = context.applicationContext as BooklightApp
    /** No row is asked after a pause in typing: the model is asked on Enter only, and not loaded before. */
    override val row: String? = null
    override val eager = false
    /** The music as it was last typed, and the player named after it (" on Spotify", " auf Spotify"; empty for none): what the model is asked about, and what its answer is typed before. */
    @Volatile private var typed = "" to ""
    /** The text "Use this" puts into the field: a name as the model gave it, which is read as it stands ("Song 2 Blur" has no kind word). Edited, it is the user's own text again. */
    @Volatile private var given: String? = null
    /** How many times the rows have been asked for: only the latest asking says what the row waits for. One for an earlier text can end after the next one's. */
    private val askings = java.util.concurrent.atomic.AtomicInteger()
    override val key = ID
    override val keywords: List<String> = context.getString(R.string.scope_play_keys).split(',')
    override val name: String = context.getString(R.string.scope_play)
    override val symbol = "music"
    override val hint: String = context.getString(R.string.scope_play_hint)
    override val about: String = context.getString(R.string.scope_play_about)

    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)

    override suspend fun rows(arg: String): List<Result> {
        val songs = app.songs
        val asking = askings.incrementAndGet()
        /** The row waits for [what], or for nothing: said only by the latest asking, and a wait is only ended by the one it was made for. */
        fun waitFor(what: PlayRequest?, wait: Boolean = false) {
            if (asking != askings.get()) return
            if (wait && what != null) songs.wait(ID, what) { withContext(Dispatchers.Default) { rows(arg).firstOrNull { it.id == ID } } } else songs.settle(ID)
        }
        // No text: the Play key, which resumes whatever played last.
        if (arg.isBlank()) {
            waitFor(null)
            return listOf(Result(
                id = "play:resume", provider = key, kind = Kind.OTHER, title = text(R.string.play_resume), subtitle = text(R.string.play_resume_sub),
                icon = Icon.Symbol("music"), score = 1.0, learnable = false,
                actions = listOf(Action("resume", text(R.string.action_play), Effect.Media(MediaKey.PLAY), symbol = "play")),
            ))
        }
        val players = Players.find(context, app.apps)
        warm(players)
        val what = Play.read(arg, players.map { it.name }, kinds = arg.trim() != given) ?: return emptyList<Result>().also { waitFor(null) }
        // No music app on this device: the row says so, and offers nothing that would fail.
        if (players.isEmpty()) {
            waitFor(null)
            return listOf(Result(id = ID, provider = key, kind = Kind.OTHER, title = text(R.string.play_title, what.name), subtitle = text(R.string.play_no_app),
                icon = Icon.Symbol("music"), score = 1.0, learnable = false, actions = emptyList()))
        }
        val last = players.indexOfFirst { it.pkg == app.prefs.now.player }
        val armed = players[if (what.player >= 0) what.player else last.coerceAtLeast(0)]
        // Spotify, with the user's key in, as the player the user chose: what it has by that name is looked up, and until
        // that is known the row waits. Chosen means named in the text, asked last, or the only music app: armed merely for
        // being first by name, nothing is asked of it (the privacy text says so). Nor for a single letter, nor where the
        // keyword and the text are the start of an app's name ("play store"): that app's row then stands above this one.
        val chosen = what.player >= 0 || last >= 0 || players.size == 1
        val spotify = armed.takeIf { it.pkg == Spotify.PACKAGE && songs.ready && chosen && what.query.length >= 2 && keywords.none { k -> app.apps.best("$k ${arg.trim()}") != null } }
        val known = spotify?.let { songs.known(what) }
        waitFor(what, wait = spotify != null && known == null)
        val found = known?.song
        // "Which song is this?", where the device has a model to ask.
        val which = text(R.string.play_which).takeIf { app.onDevice.state.value == OnDevice.State.READY }
            ?.let { Action("which", it, Effect.Ask(Play.question(what.query), it), keepOpen = true, symbol = "spark", more = true) }

        fun ask(player: String, link: String = "") = Effect.PlayMusic(what.query, player, what.mode, what.title, what.artist, what.album, what.playlist, what.genre, link)
        // A fixed order, by name, never rearranged by use; past the sixth they wait behind the row's arrow.
        val actions = ArrayList<Action>()
        players.forEachIndexed { i, p ->
            val sent = text(R.string.play_sent, p.name)
            val request = Action(p.pkg, text(if (p.searches) R.string.play_search_in else R.string.play_in, p.name), ask(p.pkg), symbol = p.icon.symbol, done = sent, more = i >= SHOWN)
            // Spotify with an answer on its way or in: Play, which needs it, and then the search. With nothing found or no
            // answer the search has Play's place, so an Enter that waited for the answer searches.
            if (p === spotify && (known == null || found != null)) {
                actions += Action(p.pkg, text(R.string.play_in, p.name), found?.let { ask(p.pkg, it.address) } ?: Songs.WAIT, symbol = p.icon.symbol, done = sent, more = i >= SHOWN)
                actions += request.copy(id = SEARCH, symbol = "search")
            } else actions += request
            // Spotify found nothing by that name: the question for the model is the next action, so a misspelt name is two keys from its fix.
            if (p === spotify && known?.failure == SongFailure.NOT_FOUND && which != null) actions += which.copy(more = false)
        }
        // (The player is named again in the app's own language, with a word the reader takes: "on", "auf".)
        typed = what.query to (players.getOrNull(what.player)?.let { " ${text(R.string.play_on)} ${it.name}" } ?: "")
        // Behind the arrow: the question for the model, where it is not on the row already.
        if (which != null && actions.none { it.id == which.id }) actions += which
        // The way to where the key goes. With a key that was refused it is on the row; without a key it waits behind the
        // arrow wherever Spotify is a player.
        if (known?.failure == SongFailure.REFUSED) actions += Action("key", text(R.string.action_flight_key), Songs.KEY, symbol = "settings")
        else if (!songs.ready && players.any { it.pkg == Spotify.PACKAGE }) actions += Action("setup", text(R.string.play_set_up), Songs.KEY, symbol = "settings", more = true)

        return listOf(Result(
            // Before an answer the row says what was asked for; with one, what Spotify found, under its own name.
            id = ID, provider = key, kind = Kind.OTHER, title = found?.name ?: text(R.string.play_title, what.name), icon = Icon.Symbol("music"), score = 1.0, learnable = false,
            subtitle = when {
                spotify == null -> read(what)
                known == null -> text(R.string.play_looking)
                found != null -> said(found)
                else -> text(when (known.failure) {
                    SongFailure.NOT_FOUND -> R.string.play_none
                    SongFailure.REFUSED -> R.string.play_refused
                    SongFailure.TOO_MANY -> R.string.play_too_many
                    SongFailure.OFFLINE -> R.string.play_offline
                    else -> R.string.play_no_answer
                })
            },
            armed = actions.indexOfFirst { it.id == armed.pkg }.coerceAtLeast(0),
            actions = actions,
        ))
    }

    /**
     * The row while the model is asked. It is an ordinary row from its first frame, and stays one: the answer is one
     * line ("Bohemian Rhapsody / Queen"), not a text that needs an answer's room. Until the answer is whole it offers
     * nothing to run.
     */
    override fun asking(r: Result, e: Effect.Ask): Result = Result(
        id = "play:which", provider = key, kind = Kind.OTHER, title = e.name, subtitle = text(R.string.play_looking), icon = Icon.Symbol("music"), score = 1.0, learnable = false,
        actions = listOf(Action("ask", text(R.string.action_ask), e, keepOpen = true, symbol = "spark")),
    )

    /**
     * What the model said, as the row's name: the title and the artist ("Bohemian Rhapsody · Queen"), or its line as it
     * came where that does not divide. Enter puts it into the field in place of what was typed, before the player that
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
                Play.named(said)?.let { name -> Action("use", text(R.string.play_use), Effect.EnterScope(key, (name + typed.second).also { given = it }), keepOpen = true, symbol = "check") },
                Action("copy", text(R.string.action_copy), Effect.CopyText(said)),
            ),
        )
    }

    override fun unanswered(r: Result): Result = r.copy(subtitle = text(R.string.prompt_failed), actions = emptyList())

    /** The players' icons are fetched as soon as their row is made, off this thread: each is there when its action is first drawn. */
    private fun warm(players: List<Player>) {
        val icons = app.icons ?: return
        val px = (48 * context.resources.displayMetrics.density).toInt()
        if (players.any { icons.cached(it.icon) == null }) app.scope.launch { for (p in players) icons.load(p.icon, px) }
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

    companion object {
        /** The scope's key, and the id of its row: one row, whatever is typed. */
        const val ID = Songs.PROVIDER
        /** The id of "Search in Spotify" where it stands beside "Play in Spotify". */
        private const val SEARCH = "search"
        /** How many players stand on the row as icons. */
        private const val SHOWN = 6
    }
}
