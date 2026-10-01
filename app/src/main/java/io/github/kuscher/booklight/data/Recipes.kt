package io.github.kuscher.booklight.data

import android.content.Context
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.MediaKey
import io.github.kuscher.booklight.core.Place

/**
 * A recipe's steps, as saved and as done. A step is one of a few plain effects: open an app
 * (maybe in a place), open a link, open a settings page, set a level, copy a text, press a media
 * key. Nothing that sends anything anywhere, so running a recipe needs no confirmation.
 */
object Recipes {
    /** The serial number of the user Booklight runs as; a recipe opens that user's apps. Set once at start. */
    var me: Long = 0

    /** What a saved step does; null if it is of a kind this version doesn't know. */
    fun effect(s: StepEntry): Effect? = when (s.kind) {
        "app" -> Effect.LaunchApp(s.a, s.b, me, place = Place.entries.getOrElse(s.n) { Place.NONE })
        "url" -> Effect.OpenUrl(s.a)
        "setting" -> Effect.OpenSettings(s.a)
        "volume" -> Effect.SetVolume(s.n)
        "brightness" -> Effect.SetBrightness(s.n)
        "copy" -> Effect.CopyText(s.a)
        "media" -> runCatching { Effect.Media(MediaKey.valueOf(s.a)) }.getOrNull()
        else -> null
    }

    /** The step that does [effect], if it is one a recipe may hold. [label] is kept so the list can name it later. */
    fun step(effect: Effect, label: String): StepEntry? = when (effect) {
        is Effect.LaunchApp -> if (effect.user == me && !effect.newWindow) StepEntry("app", effect.packageName, effect.className, effect.place.ordinal, label) else null
        is Effect.OpenUrl -> StepEntry("url", effect.url, label = label)
        is Effect.OpenSettings -> StepEntry("setting", effect.action, label = label)
        is Effect.SetVolume -> StepEntry("volume", n = effect.percent, label = label)
        is Effect.SetBrightness -> StepEntry("brightness", n = effect.percent, label = label)
        is Effect.CopyText -> if (effect.sensitive) null else StepEntry("copy", effect.text, label = label)
        is Effect.Media -> StepEntry("media", effect.key.name, label = label)
        else -> null
    }

    fun effects(r: RecipeEntry): Effect.Steps = Effect.Steps(r.steps.mapNotNull(::effect))

    /** "Open Chrome on the left · Volume 30 %": what the row says under the recipe's name. */
    fun describe(context: Context, r: RecipeEntry): String =
        if (r.steps.isEmpty()) context.getString(R.string.recipe_empty) else r.steps.joinToString(" · ") { it.label.ifEmpty { it.kind } }
}
