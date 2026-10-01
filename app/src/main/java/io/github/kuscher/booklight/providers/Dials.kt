package io.github.kuscher.booklight.providers

import android.content.Context
import android.provider.Settings
import io.github.kuscher.booklight.Executor
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.MediaKey
import io.github.kuscher.booklight.core.Nudge
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope

/**
 * Controls as rows: the volume and the brightness as levels (Left and Right change them at once,
 * the panel stays), and the three media keys. A level is read fresh for every list, so the row
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
        val media = match(q.text, context.getString(R.string.dial_media), words(R.string.dial_media_words))
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

    /** Previous, play or pause, next. What is playing can't be read without notification access, so the middle one says both. */
    private fun media(score: Double, text: String): Result {
        val wants = when {
            words(R.string.dial_next_words).any { Matcher.keyword(text, it) > 0 } -> 2
            words(R.string.dial_previous_words).any { Matcher.keyword(text, it) > 0 } -> 0
            else -> 1
        }
        return Result(
            id = "dial:media", provider = id, kind = Kind.CONTROL, title = context.getString(R.string.dial_media),
            subtitle = context.getString(R.string.dial_media_sub), icon = Icon.Symbol("music"), score = score, armed = wants,
            nudge = Nudge(Effect.Media(MediaKey.PREVIOUS), Effect.Media(MediaKey.NEXT)),
            actions = listOf(
                Action("previous", context.getString(R.string.action_previous), Effect.Media(MediaKey.PREVIOUS), keepOpen = true),
                Action("play", context.getString(R.string.action_play_pause), Effect.Media(MediaKey.PLAY_PAUSE), symbol = if (executor.audio.playing) "pause" else "play"),
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

/** `play lofi beats`: "play this" to whichever music app answers. */
class PlayScope(private val context: Context) : Scope {
    override val key = "play"
    override val keywords: List<String> = context.getString(R.string.scope_play_keys).split(',')
    override val name: String = context.getString(R.string.scope_play)
    override val symbol = "music"
    override val hint: String = context.getString(R.string.scope_play_hint)
    override val about: String = context.getString(R.string.scope_play_about)

    override suspend fun rows(arg: String): List<Result> {
        val what = arg.trim()
        if (what.isEmpty()) return emptyList()
        return listOf(Result(
            id = "play", provider = key, kind = Kind.OTHER, title = context.getString(R.string.play_title, what), icon = Icon.Symbol("music"), score = 1.0, learnable = false,
            actions = listOf(Action("play", context.getString(R.string.action_play), Effect.PlayMusic(what))),
        ))
    }
}
