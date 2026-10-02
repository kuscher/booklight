package io.github.kuscher.booklight.overlay

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
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
 * The feel: quick to start, a small settle at the end, never a wait. What answers a key is never
 * longer than about a third of a second; the two things that are (the panel's own arrival at the
 * speed the user chose, and the reflection that runs round its edge) hold nothing up: the field has
 * the keys from the first frame, and typing is never held up by any of it.
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
    /**
     * The glass opening out of its seam, over [ms]: slowly at first, quickly through the middle, and a long way of
     * slowing down, so that the arrival is what one sees. No overshoot: the glass cannot pass its own edge.
     */
    fun <T> opens(ms: Int): FiniteAnimationSpec<T> = fade(ms, easing = OPENS)
    /**
     * The glass opening again from part of the way, when the key is pressed while it is closing: a spring, because
     * it takes over the speed the glass has; without overshoot, because the glass cannot pass its own edge.
     * [by] is how much the leaving is stretched (1, or 2 at the Slow setting).
     */
    fun <T> open(by: Float = 1f): FiniteAnimationSpec<T> = s(1f, 1000f / (by * by))
    /** Appearing and going. */
    fun <T> fade(ms: Int = 110, delay: Int = 0, easing: Easing = FastOutSlowInEasing): FiniteAnimationSpec<T> =
        if (on) tween((ms * slow).toInt(), (delay * slow).toInt(), easing) else snap()
    /** One digit of a countdown giving way to the next: it happens every second and must be still in between, so not a spring. */
    fun <T> tick(): FiniteAnimationSpec<T> = if (on) tween((160 * slow).toInt(), easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)) else snap()

    /** A changed value rolling up into place, like a counter: half a line of travel. */
    fun roll(): ContentTransform =
        ContentTransform(
            slideInVertically(place()) { it / 2 } + fadeIn(fade(120)), slideOutVertically(place()) { -it / 2 } + fadeOut(fade(70)),
            // Its room goes to the new text's on a spring of ours and without a clip: a longer line is not cut while it rolls in.
            sizeTransform = SizeTransform(clip = false) { _, _ -> place() },
        )

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

        /** The glass's way from its seam to its width. */
        val OPENS = CubicBezierEasing(0.55f, 0f, 0.1f, 1f)

        // The reflection: one white light that runs once round the panel's outline, a while after it has opened.
        /** How long after the opening it comes, and how long everything must have stood still. */
        const val REFLECTION_AFTER_MS = 2400L
        const val REFLECTION_QUIET_MS = 700L
        /** Its lap takes this long for each dp of outline (the same speed round a field and round a full list), and never longer than [REFLECTION_MAX_MS]. */
        const val REFLECTION_MS_PER_DP = 1f
        const val REFLECTION_MAX_MS = 2200
        /** It gathers speed over the first third and eases off over the last: its fastest is 1.68 times its average. */
        val REFLECTS = CubicBezierEasing(0.3f, 0f, 0.5f, 1f)
        /** How fast it is at its fastest, in dp a second: where its tail is longest. */
        const val REFLECTION_CRUISE = 1680f
        /** Its tail, in dp: at rest and at its fastest. It is born as a small even glint, stretches as it gathers speed and gathers itself again. */
        const val REFLECTION_TAIL = 36f
        const val REFLECTION_TAIL_LONG = 140f
        /** While the device's own model works the same light goes round steadily and slower, and less bright. */
        const val THINKING_MS_PER_DP = 1.5f
        const val THINKING_GLOW = 0.75f
    }
}

val LocalMotion = staticCompositionLocalOf { Motion(true) }
