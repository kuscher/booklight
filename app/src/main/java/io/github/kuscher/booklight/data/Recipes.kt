package io.github.kuscher.booklight.data

import android.content.Context
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.MediaKey
import io.github.kuscher.booklight.core.Place
import io.github.kuscher.booklight.core.PlayMode

/**
 * A recipe's steps, as saved and as done. A step is one of a few plain effects: open an app
 * (maybe in a place), open a link, open a settings page, set a level, copy a text, press a media
 * key, ask a music app to play something, or ask another app for something it offers (one of its own
 * commands, an address with its own scheme, an app command of the user's own). Nothing that sends
 * anything anywhere, so running a recipe needs no confirmation.
 */
object Recipes {
    /** The serial number of the user Booklight runs as; a recipe opens that user's apps. Set once at start. */
    var me: Long = 0

    /** What a saved step does; null if it is of a kind this version doesn't know. */
    fun effect(s: StepEntry): Effect? = when (s.kind) {
        "app" -> Effect.LaunchApp(s.a, s.b, me, place = Place.entries.getOrElse(s.n) { Place.NONE })
        "url" -> Effect.OpenUrl(s.a)
        // The app's package and what it is asked, as an `intent:` address: checked again when it runs (`Executor.open`).
        "open" -> Effect.Open(s.a, s.b)
        "setting" -> Effect.OpenSettings(s.a)
        "volume" -> Effect.SetVolume(s.n)
        "brightness" -> Effect.SetBrightness(s.n)
        "copy" -> Effect.CopyText(s.a)
        "media" -> runCatching { Effect.Media(MediaKey.valueOf(s.a)) }.getOrNull()
        // What to play, as lines: the query, then the title, artist, album, playlist and genre, and the link that plays it
        // if one was found when the step was made. The player's package, and the mode by its number.
        "play" -> s.a.split('\n').let { f ->
            fun at(i: Int) = f.getOrElse(i) { "" }
            Effect.PlayMusic(at(0), s.b.ifEmpty { null }, PlayMode.entries.getOrElse(s.n) { PlayMode.ANY }, at(1), at(2), at(3), at(4), at(5), at(6))
        }
        else -> null
    }

    /** The step that does [effect], if it is one a recipe may hold. [label] is kept so the list can name it later. */
    fun step(effect: Effect, label: String): StepEntry? = when (effect) {
        is Effect.LaunchApp -> if (effect.user == me && !effect.newWindow) StepEntry("app", effect.packageName, effect.className, effect.place.ordinal, label) else null
        is Effect.OpenUrl -> StepEntry("url", effect.url, label = label)
        is Effect.Open -> StepEntry("open", effect.owner, effect.intent, label = label)
        is Effect.OpenSettings -> StepEntry("setting", effect.action, label = label)
        is Effect.SetVolume -> StepEntry("volume", n = effect.percent, label = label)
        is Effect.SetBrightness -> StepEntry("brightness", n = effect.percent, label = label)
        is Effect.CopyText -> if (effect.sensitive) null else StepEntry("copy", effect.text, label = label)
        is Effect.Media -> StepEntry("media", effect.key.name, label = label)
        is Effect.PlayMusic -> StepEntry("play", listOf(effect.query, effect.title, effect.artist, effect.album, effect.playlist, effect.genre, effect.link).joinToString("\n"), effect.packageName.orEmpty(), effect.mode.ordinal, label)
        else -> null
    }

    fun effects(r: RecipeEntry): Effect.Steps = Effect.Steps(r.steps.mapNotNull(::effect))

    /** "Open Chrome on the left · Volume 30 %": what the row says under the recipe's name. */
    fun describe(context: Context, r: RecipeEntry): String =
        if (r.steps.isEmpty()) context.getString(R.string.recipe_empty) else r.steps.joinToString(" · ") { it.label.ifEmpty { it.kind } }
}
