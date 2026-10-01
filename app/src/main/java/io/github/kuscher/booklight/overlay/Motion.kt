package io.github.kuscher.booklight.overlay

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Booklight's motion, in one place. Things that move through space (the selection, the panel's
 * height, rows finding their place) ride springs; things that only appear or go (fades) use short
 * tweens. With the system's animations off, everything cuts.
 *
 * The feel: quick to start, a small settle at the end, never a wait. Nothing here is longer than
 * about a third of a second, and typing is never held up by any of it.
 */
class Motion(val on: Boolean) {
    /** Sizes and places: the panel's height, a row moving to its new slot. */
    fun <T> place(): FiniteAnimationSpec<T> = if (on) spring(dampingRatio = 0.86f, stiffness = 520f) else snap()
    /** The edge of the selection that leads the way: fast. */
    fun <T> lead(): FiniteAnimationSpec<T> = if (on) spring(dampingRatio = 0.82f, stiffness = 1100f) else snap()
    /** The edge that follows: slower, so the pill stretches towards where it is going, then gathers itself. */
    fun <T> trail(): FiniteAnimationSpec<T> = if (on) spring(dampingRatio = 0.9f, stiffness = 420f) else snap()
    /** Small expressive pops: an icon, a chip, the panel arriving. */
    fun <T> pop(): FiniteAnimationSpec<T> = if (on) spring(dampingRatio = 0.62f, stiffness = 700f) else snap()
    /** The arming gliding along a row's actions: the highlight, and how much of each name shows, ride this one spring. */
    fun <T> arm(): FiniteAnimationSpec<T> = if (on) spring(dampingRatio = 0.78f, stiffness = 560f) else snap()
    /** Appearing and going. */
    fun <T> fade(ms: Int = 110, delay: Int = 0): FiniteAnimationSpec<T> = if (on) tween(ms, delay) else snap()

    /** How long a row waits before rising in, so a new list arrives as a quick cascade. */
    fun stagger(index: Int): Int = if (on) index * 22 else 0

    companion object {
        fun of(context: Context) = Motion(Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f)
        /** How long the panel takes to fade away when the opening is turned off (the unfold has its own time: [Arrival.leaveMs]). */
        const val LEAVE_MS = 110L
    }
}

val LocalMotion = staticCompositionLocalOf { Motion(true) }
