package io.github.kuscher.booklight.overlay

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Booklight's motion, in one place. Things that move through space (the selection, the panel's
 * height, rows finding their place) ride springs; things that only appear or go (fades) use short
 * tweens. With the system's animations off, everything cuts and only the holds remain.
 *
 * The feel: quick to start, a small settle at the end, never a wait. Nothing here is longer than
 * about a third of a second, and typing is never held up by any of it.
 *
 * [slow] stretches every spring and tween (debug builds: `./bl open stay slow=4`), so that a
 * recording can be stepped through frame by frame. It is 1 for everyone else.
 */
class Motion(val on: Boolean, val slow: Float = 1f) {
    private fun <T> s(damping: Float, stiffness: Float): FiniteAnimationSpec<T> = if (on) spring(damping, stiffness / (slow * slow)) else snap()

    /** Sizes and places: the panel's height, a row moving to its new slot, a row's other actions being uncovered. */
    fun <T> place(): FiniteAnimationSpec<T> = s(0.86f, 520f)
    /** The edge of the selection that leads the way: fast. */
    fun <T> lead(): FiniteAnimationSpec<T> = s(0.82f, 1100f)
    /** The edge that follows: slower, so the pill stretches towards where it is going, then gathers itself. */
    fun <T> trail(): FiniteAnimationSpec<T> = s(0.9f, 420f)
    /** Small expressive pops: an icon, a chip, a chevron turning over. */
    fun <T> pop(): FiniteAnimationSpec<T> = s(0.62f, 700f)
    /** The arming gliding along a row's actions: the highlight, and how much of each name shows, ride this one spring. */
    fun <T> arm(): FiniteAnimationSpec<T> = s(0.78f, 560f)
    /** The glass opening out of its seam; [by] is the opening's own speed setting (1, 2 or 4). */
    fun <T> open(by: Float = 1f): FiniteAnimationSpec<T> = s(0.72f, 1000f / (by * by))
    /** Appearing and going. */
    fun <T> fade(ms: Int = 110, delay: Int = 0, easing: Easing = FastOutSlowInEasing): FiniteAnimationSpec<T> =
        if (on) tween((ms * slow).toInt(), (delay * slow).toInt(), easing) else snap()
    /** One digit of a countdown giving way to the next: it happens every second and must be still in between, so not a spring. */
    fun <T> tick(): FiniteAnimationSpec<T> = if (on) tween((160 * slow).toInt(), easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)) else snap()

    /** A changed value rolling up into place, like a counter: half a line of travel. */
    fun roll(): ContentTransform =
        (slideInVertically(place()) { it / 2 } + fadeIn(fade(120))) togetherWith (slideOutVertically(place()) { -it / 2 } + fadeOut(fade(70)))

    /** How long a row waits before rising in, so a new list arrives as a quick cascade. */
    fun stagger(index: Int): Int = if (on) (index * 22 * slow).toInt() else 0

    /**
     * Booklight typing an example for the user: how long each letter waits for the one before it.
     * The whole text takes about half a second, a letter never under 16 ms nor over 40; with
     * animations off the text is there at once (0).
     */
    fun typeStep(letters: Int): Long = if (!on || letters <= 0) 0 else ((480 / letters).coerceIn(16, 40) * slow).toLong()

    /** A pause that is there to be read (a word in the footer, a tick before its row changes): kept with animations off. */
    fun hold(ms: Long): Long = (ms * slow).toLong()

    companion object {
        fun of(context: Context, slow: Float = 1f) =
            Motion(Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f, slow.coerceIn(1f, 16f))
        /** How long the panel takes to fade away when the opening is turned off (the unfold has its own time: [Arrival.leaveMs]). */
        const val LEAVE_MS = 110L
        /** How far the glass is open (0 to 1) before the panel may grow past the field's height: what is under the field arrives after the opening, never as part of it. */
        const val GATE = 0.85f
    }
}

val LocalMotion = staticCompositionLocalOf { Motion(true) }
