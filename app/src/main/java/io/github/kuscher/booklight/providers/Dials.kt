package io.github.kuscher.booklight.providers

import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.provider.MediaStore
import android.provider.Settings
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.Executor
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Door
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.MediaKey
import io.github.kuscher.booklight.core.Nudge
import io.github.kuscher.booklight.core.Play
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.Spotify

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
        val said = words(R.string.dial_play_words) + words(R.string.dial_pause_words) + words(R.string.dial_stop_words)
        val media = match(q.text, context.getString(R.string.dial_media), words(R.string.dial_media_words) + said)
        // `play`, `pause` or `stop`, typed whole, is this row before an app whose name merely starts that way ("Play Store"):
        // `play`, Enter resumes whatever played last. (`play` and a space is the music app's chip: `PlayScope`.)
        val whole = Matcher.fold(q.text).let { t -> said.any { Matcher.fold(it) == t } }
        if (media > 0) out.add(media(if (whole) 1.0 else media, q.text))
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
 * start playing, so its row has no Play by that alone.
 */
class Player(val pkg: String, val name: String, val icon: Icon.App, val searches: Boolean)

object Players {
    /**
     * Players that were seen to only search: asked to play something, in any mode, they open on their
     * search results for the text (docs/research/intents.md, "Tried on a Googlebook"). One that is not
     * listed here has Play on its row: what it does with the request is still its own choice.
     */
    private val SEARCH_ONLY = setOf(Spotify.PACKAGE)

    /**
     * The players on this device, by name. One question to the system (asked when the app list is
     * read, never while typing: `AppChips.read`), and no new visibility: an app
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
 * `play lofi beats`, `play album discovery by daft punk on spotify`: a short way to Play on a music
 * app's row. At the keyword's space the chip in the field is that app's own: the one the text names
 * at its end, else the one played in last, with Play armed ([door]; the chip and its rows are
 * `AppChips`'). So this scope is the chip only on a device with no music app, where its row says so
 * and offers nothing.
 *
 * It has no row in the ordinary list: `play` typed alone is the Media row, which resumes whatever
 * played last on Enter, and Tab or a space makes the keyword the chip.
 */
class PlayScope(private val context: Context) : Scope {
    private val app get() = context.applicationContext as BooklightApp
    override val key = ID
    override val keywords: List<String> = context.getString(R.string.scope_play_keys).split(',')
    override val name: String = context.getString(R.string.scope_play)
    override val symbol = "music"
    override val hint: String = context.getString(R.string.scope_play_hint)
    override val about: String = context.getString(R.string.scope_play_about)
    override val listed = false

    override fun door(text: String): Door? = app.commands.chips.play(text)

    /** Only ever asked where no music app is here to be the chip: the row says so, and offers nothing that would fail. Nothing typed, no row. */
    override suspend fun rows(arg: String): List<Result> {
        val what = Play.read(arg) ?: return emptyList()
        return listOf(Result(id = ID, provider = key, kind = Kind.OTHER, title = context.getString(R.string.play_title, what.name), subtitle = context.getString(R.string.play_no_app),
            icon = Icon.Symbol("music"), score = 1.0, learnable = false, actions = emptyList()))
    }

    companion object {
        /** The scope's key, and the id of its row. */
        const val ID = Songs.PROVIDER
    }
}
