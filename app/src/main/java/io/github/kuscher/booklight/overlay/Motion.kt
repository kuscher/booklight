package io.github.kuscher.booklight.overlay

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.core.SetDown
import kotlin.math.exp
import kotlin.math.roundToInt

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
    /** The edge that follows: slower, so a highlight stretches towards where it is going, then gathers itself. */
    fun <T> trail(): FiniteAnimationSpec<T> = s(0.9f, 420f)
    /**
     * The list's pill and the grid's square, like rubber (docs/design/rubber-highlight.md): the edge that leads goes at
     * once, on this. The old edge holds on for [pillHold] frames, so the highlight lies over both rows, and then gathers
     * on [pillTrail]: faster than [trail], so the whole move is shorter than it was.
     */
    fun <T> pillLead(): FiniteAnimationSpec<T> = s(0.85f, 1400f)
    /** The old edge, once it lets go. On a long way ([PILL_NEAR]) it is as firm as the edge that leads: it arrives with it, and does not slow down and set off again at the landing. */
    fun <T> pillTrail(far: Boolean): FiniteAnimationSpec<T> = if (far) s(0.9f, 1400f) else s(0.86f, 900f)
    /**
     * How long the old edge holds on, in frames of a screen that draws [refresh] a second: five at 120 (42 ms), two on
     * a long way. Frames and not a time: a wait of 40 ms would end four, five or six frames after the other edge set
     * off, and the stretch would differ from one press to the next. A highlight that is already moving does not hold.
     */
    fun pillHold(far: Boolean, refresh: Float): Int = if (on) (((if (far) 0.017f else 0.040f) * refresh).roundToInt() * slow).roundToInt() else 0
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

    /**
     * A flight's plane going to its place on the line when the answer lands, the flown part growing behind it: 300 ms
     * and 300 more for a way across the whole line ([way]: how much of the line it crosses, 0 to 1). On the glass's
     * own curve, not on `place`: that one overshoots by about half a per cent, 3 dp on a full line, and a plane that
     * passes the end of its line and comes back is wrong. [first]: it has just come in at the start, and sets off 60 ms
     * after the words began to change. Once: after it the plane only takes a step a minute, on [tick].
     */
    fun <T> flies(way: Float, first: Boolean = false): FiniteAnimationSpec<T> = fade((300 + 300 * way.coerceIn(0f, 1f)).roundToInt(), if (first) 60 else 0, OPENS)

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

    /**
     * A span of motion that is neither a spring nor a tween (how long a key on the glass is held down, how long a part
     * waits its turn): stretched as they are, and nothing with the system's animations off. A pause that is there to be
     * read is [hold], which stays.
     */
    fun held(ms: Long): Long = if (on) (ms * slow).toLong() else 0L

    companion object {
        fun of(context: Context, slow: Float = 1f) =
            Motion(Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f, slow.coerceIn(1f, 16f))
        /** How long the panel takes to fade away when the opening is turned off (the unfold has its own time: [Arrival.leaveMs]). */
        const val LEAVE_MS = 110L

        // First run's show landing in the key's step (docs/design/first-run/motion.md §2.2, "B5").
        /** The lower edge waits this long while what was Booklight's fades where it stands, and only then draws in: it cuts no row and no cell. */
        const val LANDS_AFTER_MS = 80L
        /** The desk behind the panel, dimmed through the piece, lifts over this long once the key's step lands. */
        const val DESK_LIFTS_MS = 640
        /** The highlight leaves for the key's armed answer this long after the landing began: once what it stood on has begun to fade. */
        const val GLIDE_AFTER_MS = 32L
        /** Arrived, it is the answers' own highlight within this long: the time that highlight takes to come. */
        const val TAKES_OVER_MS = 120

        // First run's stage, and what moves between its screens (docs/design/first-run/motion.md §3 and §4). The stage is
        // drawn on a clock of its own, by core's schedule (`SetDown`, which takes these as [PACE]): that clock's lengths, in ms.
        /** One key after another: a cap, or a new typed part of a recipe, comes this long after the piece before it (three steps of [stagger]). */
        const val BEAT_MS = 66
        /** A recipe's typed part is written a letter at a time, each this long after the one before ([typeStep] at these lengths). */
        const val LETTER_MS = 40
        /** The seat's parts, and whatever comes "as rows do", come this far apart ([stagger]'s step). */
        const val PART_MS = 22
        /** A part rises 12 dp on `place` and fades in over this long, as a row does; the caption fades in over the same. A cap comes up over [CAP_MS], from [CAP_SMALL] of its size, on `pop`. */
        const val RISE_MS = 140
        const val CAP_MS = 110
        const val CAP_SMALL = 0.96f
        /** The band of a screen set down at an opening begins when the glass is at rest, and this long after the gate at the least. */
        const val BAND_AFTER_MS = 120
        /** From the gate to the glass at rest at Fast (motion.md §3, "Clocks": 129 to 210); times the opening's speed. */
        const val GATE_TO_REST_MS = 81
        /** A screen that takes another's place where it stands: its band is written this long after its words began to roll, once the old band has faded ([PRESS_MS]). */
        const val TURN_MS = 120
        /** A stage that follows a list (a lesson's Enter, the sum copied, "First steps" asked for) begins this long after the list gave way: its rows have begun to fade by then. */
        const val AFTER_LIST_MS = 60
        /** The question's parts after its seat: its text, its note, its answers (motion.md T8: 646, 700 and 722 for a seat at 580). */
        const val ASK_TEXT_MS = 66
        const val ASK_NOTE_MS = 120
        const val ASK_ANSWERS_MS = 142
        /** At the landing of the opening piece (motion.md §2.2, B5, counted from its T): the disc, the words, the counter; the armed answer's words inside the highlight that travels there; the band's first piece. What follows the band comes as everywhere. */
        const val LANDS_DISC_MS = 80
        const val LANDS_WORDS_MS = 150
        const val LANDS_COUNTER_MS = 190
        const val LANDS_ARMED_MS = 180
        const val LANDS_BAND_MS = 260
        /** A stage is at rest this long after its last part began to come. */
        const val REST_MS = 170
        /**
         * The stage's clock runs on this long past that, and is then set at rest: at [REST_MS] a part that rises on `place`
         * is still about 0.4 dp under its place (the spring has come 0.96 of its way), and would stand there for good; this
         * much later it is within a hundredth of a dp, and `pop` has let go too.
         */
        const val SETTLES_MS = 180
        /**
         * Pressed: ink over a cap, this long in and this long out (the design system's pressed token). Over an answer's
         * slot it has no way in: the answer is run in the frame of the press, so the ink is whole in that frame and
         * goes over [RELEASE_MS], while answers that give way fade over [PRESS_MS].
         */
        const val PRESS_MS = 80
        const val RELEASE_MS = 120
        /**
         * "Your key" on the glass is held down as the user's own key is: this long where the keys were pressed with the panel
         * in view, and this long where they uncovered it under the system's dialog (the dialog's own fade, about 240 ms, and
         * 160 ms in view). Then it comes up on `pop`; the check draws [CHECK_AFTER_MS] later, in the time the drawn check
         * takes everywhere (`DrawnCheck`); the light sets off [LAP_AFTER_MS] after the key came up.
         */
        const val KEY_TAP_MS = 160L
        const val KEY_HELD_MS = 400L
        const val CHECK_AFTER_MS = 60L
        const val LAP_AFTER_MS = 240L
        /**
         * The user's key, held, repeats, and each repeat is a new start of the panel, which on any day puts an open panel
         * away. Where the key has just landed ("Your key works": in view, under the system's dialog, or in a panel the key
         * itself made), a start by the key does nothing for this long after the last one, each repeat beginning the wait
         * anew. Real time, not motion's: it is not stretched by a debug build's slow motion and not nothing with the
         * system's animations off, because a key is held just as long then. Longer than a keyboard waits before it
         * repeats a held key (about half a second); a second press meant to close the panel comes later than this.
         */
        const val KEY_SETTLES_MS = 700L
        /** Under the system's dialog the stage turns to "Now press your keys" this long after the panel lost the focus to it: the dialog covers the panel by then, and nothing is seen to pop beside it. */
        const val HELPER_AFTER_MS = 300L
        /** The question gives way to the choices: the lower edge rises only after this long, so that it cuts no word that is still fading. */
        const val ASK_LEAVES_MS = 60L
        /** The fold: the field's `esc` cap fades back this long after the choices were left, and the light sets off round the bare field this long after. */
        const val FOLD_CAP_MS = 120
        const val FOLD_LAP_MS = 360L
        /** A coach line that takes the seat of a word that just happened ("Copied") waits for that word's fade. */
        const val COACH_AFTER_MS = 80
        /** How far the glass is open (0 to 1) before the panel may grow past the field's height: what is under the field arrives after the opening, never as part of it. */
        const val GATE = 0.85f

        /** The glass's way from its seam to its width. */
        val OPENS = CubicBezierEasing(0.55f, 0f, 0.1f, 1f)

        /** The edge that leads has more than this to go: a long way. The next row never is, whether it is 56, 92 or 40 dp. */
        val PILL_NEAR = 98.dp
        /** A highlight is drawn at most this much longer than its row, however far it goes: one row more, never a bar down the list. */
        val STRETCH_MOST = 56.dp
        /** Up to here a stretch is drawn as it is (a step of one row peaks at 34 dp); beyond, it eases into [STRETCH_MOST]. */
        val STRETCH_KNEE = 40.dp
        /** How much of a stretch of [extra] is drawn. */
        fun stretch(extra: Dp): Dp {
            if (extra <= STRETCH_KNEE) return extra
            val room = STRETCH_MOST - STRETCH_KNEE
            return STRETCH_KNEE + room * (1f - exp(-((extra - STRETCH_KNEE) / room)))
        }

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

/** The seam the glass opens out of, growing to its length. */
internal val SEAM_GROWS = CubicBezierEasing(0.2f, 0f, 0f, 1f)
/** [SEAM_GROWS] run backwards. */
internal val SEAM_DRAWS_IN = CubicBezierEasing(1f, 0f, 0.8f, 1f)
/** The glass closing: the opening spring's way from the seam to full width, backwards (it lands softly on the seam; the bounce is left out). */
internal val FOLDS = CubicBezierEasing(0.45f, 0f, 0.4f, 1f)

/**
 * One of the panel's springs as a function of time: how far it has come, 0 to a little past 1, [ms] after it set off.
 * For what is drawn on a clock of its own and keeps nothing from frame to frame: first run's welcome, and its stage.
 */
internal class Sprung(spec: FiniteAnimationSpec<Float>) {
    private val spring = spec.vectorize(Float.VectorConverter)
    private val zero = AnimationVector1D(0f)
    private val one = AnimationVector1D(1f)
    fun at(ms: Float): Float = if (!(ms > 0f)) 0f else if (ms > 4000f) 1f else spring.getValueFromNanos((ms * 1_000_000f).toLong(), zero, one, zero).value
}
/** (Such a clock is already as slow as a debug build asks: the springs are asked at their own speed.) */
internal val PLACE = Sprung(Motion(true).place())
internal val POP = Sprung(Motion(true).pop())

/**
 * The times of first run's stage as core's schedule takes them (`SetDown.marks`: when each part of a screen comes, for
 * every way a screen can come). The stage is drawn on a clock of its own by that schedule, and the model counts a
 * screen as in view by the same marks.
 */
val PACE = SetDown.Pace(
    part = Motion.PART_MS, beat = Motion.BEAT_MS, letter = Motion.LETTER_MS, band = Motion.BAND_AFTER_MS, turn = Motion.TURN_MS,
    afterList = Motion.AFTER_LIST_MS, press = Motion.PRESS_MS,
    askText = Motion.ASK_TEXT_MS, askNote = Motion.ASK_NOTE_MS, askAnswers = Motion.ASK_ANSWERS_MS,
    landsDisc = Motion.LANDS_DISC_MS, landsWords = Motion.LANDS_WORDS_MS, landsCounter = Motion.LANDS_COUNTER_MS,
    landsArmed = Motion.LANDS_ARMED_MS, landsBand = Motion.LANDS_BAND_MS, rest = Motion.REST_MS,
)

/**
 * First run's welcome, "Lights on" (docs/design/first-run/motion.md §2.1): one clock, and every mark of it here, in
 * ms. The paper counts from the first frame of an opening at Medium, whose gate is at 257 ms; the clock here starts
 * at the gate, whatever the speed of the opening, so each mark is the paper's less [GATE]. A mark named `…_MS` is a
 * length of time; one named `…_AFTER_MS` is counted from the beginning of the hand-over ([HAND]), but for two: the
 * flood's from the strike, and a tool's glint from that tool's own start; the others are moments. None of them is
 * stretched by the speed of the opening: the clock only starts later. (A debug build's slow motion stretches them all.)
 *
 * Its curves are the panel's own: fades on the standard curve, the lamp's light flooding the field and the seam of
 * light falling on [SEAM_GROWS], the light opening on [Motion.OPENS] (the glass's own unfold, a second time), the
 * shaft turning on [FOLDS], the head's light closing on [SEAM_DRAWS_IN], the handle on `place`, a tool on `pop`.
 */
object Lights {
    /** Where the paper's clock has the gate. */
    const val GATE = 257
    /** Night falls from the gate on, slower than the glass grows: the veil, the desk behind, the outline. */
    const val NIGHT_MS = 420
    /** The caret blinks off in the falling night, as a caret does. Where it would come back, the lamp does. */
    const val CARET_OFF = 560 - GATE
    /** The lamp strikes: 60 % after [STRIKE_UP_MS], back to 35 % at [STRIKE_SAG_MS] (it has not caught), 100 % at [STRIKE_ON_MS], fast at first. A fade would be a dimmer, not a lamp. */
    const val STRIKE = 800 - GATE
    const val STRIKE_UP_MS = 30
    const val STRIKE_SAG_MS = 70
    const val STRIKE_ON_MS = 200
    /** The light floods the field from the caret to both ends. */
    const val FLOOD_AFTER_MS = 40
    const val FLOOD_MS = 260
    /** A seam of light falls under the lamp, */
    const val SEAM = 960 - GATE
    const val SEAM_MS = 160
    /** and opens out of itself to both sides, to its shape. */
    const val OPEN = 1180 - GATE
    const val OPEN_MS = 360
    /** The handle rises into the foot of the light, the cue on it. */
    const val HANDLE = 1420 - GATE
    const val HANDLE_MS = 140
    /** The five tools flick out of it, left to right, a beat apart; a glint crosses each disc once as it opens. */
    const val TOOLS = 1560 - GATE
    const val BEAT_MS = 66
    const val GLINT_AFTER_MS = 90
    const val GLINT_MS = 160
    /** It stands, to be read: one breath of the light, from here to the hand-over. */
    const val STANDS = 1540 - GATE
    /** The hand-over: Booklight presses the handle, and everything goes back up to the field. One leads at a time, each beginning before the last has ended. */
    const val HAND = 3200 - GATE
    const val PRESS_MS = 80
    const val RELEASE_MS = 120
    /** The tools fold back into the handle, right to left. */
    const val FOLD_EACH_MS = 30
    const val FOLD_MS = 120
    /** The shaft turns: its upper edge slides to the caret, the foot follows and rises, the sides close. The night begins to lift with it. */
    const val TURN_AFTER_MS = 160
    const val TURN_MS = 300
    const val FOOT_AFTER_MS = 220
    const val RISE_MS = 360
    const val LIFT_MS = 400
    /** The cue fades inside the handle; the handle sets off for row one's seat; the unlit words fade where they stand. */
    const val CUE_OUT_AFTER_MS = 200
    const val CUE_OUT_MS = 80
    const val GO_AFTER_MS = 240
    const val WORDS_OUT_AFTER_MS = 380
    const val WORDS_OUT_MS = 80
    /** The head's light closes on the caret from both ends: then all that is left of the lamp is the caret. */
    const val CLOSE_AFTER_MS = 440
    const val CLOSE_MS = 160
    /** Booklight's mark comes up in the field's seat as that light passes it, before the show's first letter: it says who is about to type. */
    const val MARK_AFTER_MS = 520
    /**
     * The night is deeper towards the glass's corners, but only while it is night: that shade begins once the night has
     * fallen this far (0 to 1) and is whole at full night, and it leaves the same way before the theme's own glass is
     * back. (Under a glass that is half day its edge showed as a faint ring.)
     */
    const val DEEPENS_FROM = 0.5f
    /** The hand-over's length: at its end the show's first letter lands on the caret. */
    const val HAND_MS = 620
    /** A key puts the lamp out in its frame: what stands fades where it stands over [OUT_MS], and the night lifts over [DAWN_MS]. */
    const val OUT_MS = 80
    const val DAWN_MS = 160
    /** If the show cannot begin when its cue is due (this device's apps are still being read), the welcome stands on for this long; then the key's step lands. */
    const val WAITS_MS = 1000
}

val LocalMotion = staticCompositionLocalOf { Motion(true) }
