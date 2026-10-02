package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import android.os.SystemClock
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** How the panel arrives. [slow] stretches every time in it: 1 = as designed, 2 and 4 for people who want to watch it. */
data class Arrival(val unfold: Boolean, val slow: Float) {
    /**
     * How much the leaving is stretched. It is the arrival backwards in shape, but not in time: the window keeps the
     * keyboard until it is gone, so at Fast and at Medium it leaves in the same 155 ms, and only Slow is slower.
     */
    val leave: Float get() = if (slow >= 4f) 2f else 1f
    /** How long the panel takes to leave, for the activity to wait before finishing. */
    val leaveMs: Long get() = if (unfold) ((FOLD_MS + LEAVE_WAIT_MS) * leave).toLong() + 20 else Motion.LEAVE_MS

    companion object {
        /** From the setting: `off` is 1.0's small settle and fade; `fast`, `medium` and `slow` unfold. */
        fun of(setting: String, motion: Motion) = Arrival(motion.on && setting != "off", when (setting) { "medium" -> 2f; "slow" -> 4f; else -> 1f })

        /**
         * The unfold's times before [slow]: the seam grows for [SEAM_MS]; the glass starts to open [WAIT_MS] in (its own
         * slow start is the rest of the pause) and takes [OPEN_MS] to its width: 210 ms in all, 420 at Medium.
         */
        const val SEAM_MS = 80
        const val WAIT_MS = 30
        const val OPEN_MS = 180
        /** Leaving, before [leave]: the glass folds to its seam in [FOLD_MS]; the seam starts to draw in [LEAVE_WAIT_MS] before the fold would be as long as the seam. */
        const val FOLD_MS = 105
        const val LEAVE_WAIT_MS = 50
        /** The last of the seam fades as it goes, so nothing is switched off. */
        const val GONE_MS = 40
    }
}

/** A spring that would pass its end turns back instead, like something landing: nothing may be drawn outside the window. */
private fun landed(v: Float) = 1f - abs(1f - v)
private fun smooth(a: Float, b: Float, x: Float): Float { val t = ((x - a) / (b - a)).coerceIn(0f, 1f); return t * t * (3f - 2f * t) }

/** The seam the glass opens out of, as a width. */
private val SEAM = 6.dp
private val SEAM_GROWS = CubicBezierEasing(0.2f, 0f, 0f, 1f)
/** [SEAM_GROWS] run backwards. */
private val SEAM_DRAWS_IN = CubicBezierEasing(1f, 0f, 0.8f, 1f)
/** The glass closing: the opening spring's way from the seam to full width, backwards (it lands softly on the seam; the bounce is left out). */
private val FOLDS = CubicBezierEasing(0.45f, 0f, 0.4f, 1f)

@Composable
fun Panel(
    model: OverlayModel,
    icons: AppIcons,
    /** True when the window is blurring what's behind it: the surface can be see-through. */
    glass: Boolean,
    dark: Boolean,
    arrival: Arrival,
    /** True once the panel is on its way out: it folds away (or fades) and the activity finishes after [Arrival.leaveMs]. */
    leaving: Boolean,
    onHeight: (Dp) -> Unit,
    /** How far the room has dimmed and how far the glass has come into focus, each 0 to 1: the window's dim and blur follow. */
    onPresence: (dim: Float, blur: Float) -> Unit,
    /** Asked by the window before each frame is drawn: where the glass stands (its blur and its shadow follow it), and how much of its shadow it casts. */
    frame: GlassFrame,
    /** Runs an action; [stay]: Shift was held, keep the panel open. */
    onRun: (Result, Action, stay: Boolean) -> Unit,
    onCard: (Card, primary: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val density = LocalDensity.current
    val focus = remember { FocusRequester() }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var cardChoice by remember { mutableIntStateOf(0) }
    // Text set by the model (a scope entered, the last text restored, the debug hook) lands in the field too.
    LaunchedEffect(model.query) { if (field.text != model.query) field = TextFieldValue(model.query, TextRange(model.query.length)) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(model.card) { cardChoice = 0 }

    // The window follows the panel on a spring: taller as rows arrive, never moving its top edge. The opening itself is
    // always the field's height: whatever is under the field (a card, rows for text another app handed over) arrives
    // once the glass is nearly open, never as part of the opening.
    var gate by remember { mutableStateOf(!arrival.unfold) }
    val height by animateDpAsState(if (gate) Metrics.height(model) else Metrics.field, motion.place(), label = "height")
    LaunchedEffect(Unit) { snapshotFlow { height }.collect { onHeight(it) } }

    // Arriving. Unfold: a seam of outline grows up and down, then the glass opens out of it to both
    // sides. The window is the panel's final rectangle throughout (resizing a window in width is
    // neither smooth nor symmetric): only the glass inside it grows, the window's blur with it, and
    // the contents, laid out once at full width, are uncovered.
    val slow = arrival.slow
    val w0 = SEAM.value / Metrics.width.value
    val wide = remember { Animatable(if (arrival.unfold) w0 else 1f) }
    val high = remember { Animatable(if (arrival.unfold) 0.1f else 1f) }
    val presence = remember { Animatable(if (motion.on && !arrival.unfold) 0f else 1f) }
    val scale = remember { Animatable(if (motion.on && !arrival.unfold) 0.96f else 1f) }
    fun opened() = if (arrival.unfold) ((landed(wide.value) - w0) / (1f - w0)).coerceIn(0f, 1f) else 1f
    LaunchedEffect(Unit) { if (!gate) { snapshotFlow { opened() >= Motion.GATE }.first { it }; gate = true } }
    LaunchedEffect(gate) { if (gate) model.arrived = true }
    // What stands under the empty field comes only after the opening, and only if nothing has been typed for a moment:
    // the opening is the same every time. Something just copied has the place before a tip.
    // "Your usual" (a switch, off by default) may come sooner: as the glass lands, once it is known that no copy is fresh.
    LaunchedEffect(gate) {
        if (!gate || !model.settings.zero) return@LaunchedEffect
        if (!arrival.unfold) snapshotFlow { presence.value >= 1f }.first { it }       // the opening set to Off: after its fade
        snapshotFlow { model.zeroSeats != null && model.focused }.first { it }
        model.offerZero()
    }
    LaunchedEffect(gate) { if (gate) { delay(TIP_AFTER_MS); if (!model.offerCopy() && !model.offerZero(last = true)) model.offerTip() } }
    // The usual rows arrive unasked and with nothing selected: a screen reader is told once that they are there, and how to get in.
    val view = androidx.compose.ui.platform.LocalView.current
    val said = stringResource(R.string.a11y_usual, model.results.takeIf { model.zeroUp }.orEmpty().joinToString(", ") { it.title })
    LaunchedEffect(model.zeroUp) { if (model.zeroUp) view.announceForAccessibility(said) }
    // The seam the glass grows out of, and draws back into, lies on the field's centre line whatever the panel's height.
    val seamPx = with(density) { Metrics.field.toPx() * 0.1f }
    val fieldPx = with(density) { Metrics.field.toPx() }
    fun unrolled() = ((landed(high.value) - 0.1f) / 0.9f).coerceIn(0f, 1f)
    fun glassTop() = ((fieldPx - seamPx) / 2f * (1f - unrolled())).roundToInt()
    /** The glass in a window [full] wide and [h] high, as far as the arrival has opened it: centred on the seam. */
    fun glassBox(full: Int, h: Int): IntRect {
        val w = (full * landed(wide.value)).roundToInt().coerceIn(1, full.coerceAtLeast(1))
        val u = unrolled()
        val hh = (h * u + seamPx * (1f - u)).roundToInt().coerceIn(1, h.coerceAtLeast(1))
        val top = glassTop().coerceAtMost((h - hh).coerceAtLeast(0))
        return IntRect((full - w) / 2, top, (full - w) / 2 + w, top + hh)
    }
    // The window asks before each frame, and its shadow is cast from exactly this rectangle in the frame that draws it.
    SideEffect { frame.at = ::glassBox; frame.cast = { presence.value * opened() } }
    // On the way out the contents let go early: the glass closes over them.
    var folding by remember { mutableStateOf(false) }
    /** How fast the glass was closing, last frame (widths a second): a turn takes it over. A cancelled animation forgets its own. */
    var foldSpeed by remember { mutableFloatStateOf(0f) }
    fun shown() = if (folding) smooth(0.55f, 0.95f, opened()) else smooth(0.35f, 0.85f, opened())
    // The mark at the field's start and the cap at its end come last: the glass's edge passes them slowly, and would cut them.
    fun ends() = smooth(0.92f, 1f, opened())
    LaunchedEffect(leaving) {
        if (!arrival.unfold) {
            launch { presence.animateTo(if (leaving) 0f else 1f, motion.fade(if (leaving) 90 else 170)) }
            launch { scale.animateTo(if (leaving) 0.975f else 1f, if (leaving) motion.fade(100) else motion.pop()) }
        } else if (!leaving) {
            // Also the way back, when the key is pressed again while the panel is leaving: it opens from wherever it had got to.
            val seamGrowing = high.value < 1f
            val fromSeam = wide.value <= w0 + 0.02f
            launch { presence.animateTo(1f, motion.fade((Arrival.GONE_MS * slow).toInt())) }
            launch { high.animateTo(1f, motion.fade((Arrival.SEAM_MS * slow).toInt(), easing = SEAM_GROWS)) }
            if (fromSeam) {
                if (seamGrowing) delay(motion.hold((Arrival.WAIT_MS * slow).toLong()))
                wide.animateTo(1f, motion.opens((Arrival.OPEN_MS * slow).toInt()))
            } else {
                // Turned round part of the way: a spring takes over the speed the glass has, where a curve would
                // stop it dead and start again. It is as stiff as the leaving is quick (not as the opening is slow):
                // the glass is closing fast, and a soft spring would let it go on closing long after the key.
                wide.animateTo(1f, motion.open(arrival.leave), initialVelocity = foldSpeed)
            }
            foldSpeed = 0f
            folding = false
        } else {
            folding = true
            // Leaving is arriving backwards: the glass closes to its seam from both sides and covers the contents,
            // the blur lets go on the way, then the seam draws in and is gone.
            val by = arrival.leave
            wide.snapTo(landed(wide.value))
            launch { wide.animateTo(w0, motion.fade((Arrival.FOLD_MS * by).toInt(), easing = FOLDS)) { foldSpeed = velocity } }
            delay(motion.hold(((Arrival.FOLD_MS + Arrival.LEAVE_WAIT_MS - Arrival.SEAM_MS) * by).toLong()))
            launch { high.animateTo(0.1f, motion.fade((Arrival.SEAM_MS * by).toInt(), easing = SEAM_DRAWS_IN)) }
            delay(motion.hold(((Arrival.SEAM_MS - Arrival.GONE_MS) * by).toLong()))
            presence.animateTo(0f, motion.fade((Arrival.GONE_MS * by).toInt(), easing = LinearEasing))
        }
    }
    // The blur is the glass's own (the window frames its root view to the glass before each frame): it is there from
    // the seam on, and goes with the seam.
    LaunchedEffect(Unit) { snapshotFlow { presence.value * opened() to presence.value }.collect { (dim, blur) -> onPresence(dim, blur) } }

    // The reflection. A while after the panel has opened, in a quiet moment, one white light runs once round the
    // outline: from the middle of the top edge, clockwise, and back to it. It is there for the pleasure of it. It is
    // born as a small glint, stretches as it gathers speed, and dims along the last stretch so that it is gone
    // before it stops. Any key, or the list changing under it, and it fades while it goes on; it does not come again.
    val run = remember { Animatable(0f) }                       // how far round, in laps
    val flourish = remember { Animatable(1f) }                  // 1, until something happens during its lap
    val think = remember { Animatable(0f) }                     // the same light while the device's model works
    val present = remember { Animatable(1f) }                   // 0 as the panel leaves
    var lapping by remember { mutableStateOf(false) }
    var thought by remember { mutableStateOf(false) }           // the model's light has run in this opening
    var stir by remember { mutableIntStateOf(0) }               // any key at all
    val laps = rememberCoroutineScope()
    fun outlineDp() = 2f * (Metrics.width.value + height.value)
    LaunchedEffect(gate, leaving) {
        if (!motion.on || !gate || leaving) return@LaunchedEffect
        val since = SystemClock.uptimeMillis()
        var came = false
        var first = true
        // (The copy's line arriving, and its words settling, are the list changing too.)
        snapshotFlow { listOf(model.query, model.results, model.copy, stir) }.collectLatest {
            val acted = !first; first = false
            if (lapping) { if (acted) flourish.animateTo(0f, motion.fade(160)); return@collectLatest }
            // Once the model's light has gone round, the lap does not come: the same light again would read as more work.
            if (came || thought) return@collectLatest
            // The panel has been open a while, and everything has stood still for a moment: both in real time.
            val open = SystemClock.uptimeMillis() - since
            delay(motion.hold(maxOf(Motion.REFLECTION_QUIET_MS, Motion.REFLECTION_AFTER_MS - open)))
            if (model.working || think.value > 0f || thought) return@collectLatest
            came = true
            // Its own job: what happens next must not stop it where it stands.
            laps.launch {
                lapping = true
                try {
                    flourish.snapTo(1f); run.snapTo(0f)
                    val ms = (outlineDp() * Motion.REFLECTION_MS_PER_DP).toInt().coerceAtMost(Motion.REFLECTION_MAX_MS)
                    run.animateTo(1f, motion.fade(ms, easing = Motion.REFLECTS))
                    run.snapTo(0f)
                } finally { lapping = false }
            }
        }
    }
    LaunchedEffect(leaving) { present.animateTo(if (leaving) 0f else 1f, motion.fade(if (leaving) 90 else 160)) }
    /** The lap's own brightness: in over its first twentieth, out over its last fifth. */
    fun once(): Float = if (lapping) run.value.let { smooth(0f, 0.05f, it) * (1f - smooth(0.82f, 1f, it)) } * flourish.value else 0f

    // While the device's own model works on an answer the same light goes round steadily, slower and less bright: a
    // flourish must not read as "working". When the first word lands it fades while it goes on.
    LaunchedEffect(model.working) {
        if (!motion.on) return@LaunchedEffect
        suspend fun round() {
            while (true) {
                if (run.value >= 1f) run.snapTo(0f)
                run.animateTo(1f, motion.fade(((1f - run.value) * outlineDp() * Motion.THINKING_MS_PER_DP).toInt().coerceAtLeast(1), easing = LinearEasing))
            }
        }
        if (model.working) {
            thought = true
            if (lapping) {
                // The lap is on its way: the light it is becomes the model's, in the same place. Its brightness goes one
                // way from what it has to the model's, and its speed changes evenly from the lap's to the model's over
                // the rest of this lap (a curve whose slope falls in a straight line from the one to the other).
                val v0 = abs(run.velocity)                                                          // laps a second, now
                val v1 = 1000f / (outlineDp() * Motion.THINKING_MS_PER_DP * motion.slow)            // and the model's
                val rest = 1f - run.value
                think.snapTo(once() / Motion.THINKING_GLOW)
                launch { think.animateTo(1f, motion.fade(200, easing = LinearEasing)) }
                if (rest > 0f && v0 + v1 > 0f) {
                    val m0 = 2f * v0 / (v0 + v1)
                    val m1 = 2f * v1 / (v0 + v1)
                    run.animateTo(1f, tween((2000f * rest / (v0 + v1)).toInt().coerceAtLeast(1), easing = CubicBezierEasing(1f / 3f, m0 / 3f, 2f / 3f, 1f - m1 / 3f)))
                }
            } else launch { think.animateTo(1f, motion.fade(200, easing = LinearEasing)) }
            round()
        } else if (think.value > 0f) {
            val going = launch { round() }
            think.animateTo(0f, motion.fade(240, easing = LinearEasing))
            going.cancel()
            run.snapTo(0f)
        }
    }
    /** How bright the light is: the lap's own coming and going (in over its first twentieth, out over its last fifth), or the steady light of the model at work. */
    fun glow(): Float = maxOf(once(), Motion.THINKING_GLOW * think.value) * present.value
    /** How long its tail is: it grows with its speed. */
    fun tail(): Float {
        val speed = abs(run.velocity) * outlineDp() * motion.slow     // (a debug build's slow motion must not shorten it)
        return Motion.REFLECTION_TAIL + (Motion.REFLECTION_TAIL_LONG - Motion.REFLECTION_TAIL) * (speed / Motion.REFLECTION_CRUISE).coerceIn(0f, 1f)
    }

    fun go(r: Result, a: Action, stay: Boolean) = onRun(r, a, stay)

    fun keys(e: KeyEvent): Boolean {
        if (e.type != KeyEventType.KeyDown) return false
        stir++   // any key is something happening: the reflection waits for quiet, and gives way
        val card = model.card
        val atEnd = field.selection.collapsed && field.selection.end == field.text.length
        val enter = e.key == Key.Enter || e.key == Key.NumPadEnter
        // A key that is held repeats. Whatever runs something takes one press for one run: a held Enter must not confirm its own delete.
        val again = e.nativeKeyEvent.repeatCount > 0
        val bare = !e.isShiftPressed && !e.isCtrlPressed && !e.isAltPressed && !e.isMetaPressed
        // A waiting confirmation is cancelled by any key but Enter; Esc then only cancels.
        if (!enter && model.cancelConfirm() && e.key == Key.Escape) return true
        val r = model.current
        val entersScope = r?.actions?.getOrNull(model.armed)?.effect is Effect.EnterScope
        return when (e.key) {
            // On the copy's line Down opens it, as Tab does: the line is where row one will be.
            Key.DirectionDown -> { model.down(again); true }
            Key.DirectionUp -> { model.up(again); true }
            Key.Enter, Key.NumPadEnter -> {
                if (!again) { if (card != null) onCard(card, cardChoice == 0) else if (model.tip != null) model.tipEnter() else model.enter { row, a -> go(row, a, e.isShiftPressed) } }
                true
            }
            Key.Tab -> {
                when {
                    card != null -> if (!again) cardChoice = 1 - cardChoice
                    model.tip != null -> if (!again) model.tipTab()
                    // The usual rows at rest, where nothing is selected: Tab is Down. It acts on what is on screen.
                    // (Not in the frames between a typed letter, or a scope entered, and its list: those rows are on their way out.)
                    model.zeroUp && model.current == null && model.chip == null && model.query.isBlank() -> if (!again && !e.isShiftPressed) model.down(false)
                    // The copy's line, or the empty field before the line has come: Tab opens what was copied.
                    model.copy != null || model.bare -> if (!again && !e.isShiftPressed) model.tabCopy()
                    // The text is exactly a keyword and nothing has been moved: Tab makes it the chip. Text and chip change in one frame.
                    model.keyword != null && !e.isShiftPressed -> if (!again && model.enterKeyword()) field = TextFieldValue("")
                    entersScope && !e.isShiftPressed -> if (!again) model.fill()
                    // A row is open: Tab is the next of its actions, wrapping inside them.
                    model.opened != null -> model.step(if (e.isShiftPressed) -1 else 1)
                    // Along the row's stops, wrapping. It stops on the arrow like on any other; only Enter opens it.
                    else -> model.arm(if (e.isShiftPressed) -1 else 1, wrap = true)
                }
                true
            }
            // Right at the end of the text and Left go along what the row offers: a grid's cells, a level, its actions.
            // With a modifier held they are the text's own (select, a word back).
            Key.DirectionRight -> when {
                !atEnd || !bare -> false
                r?.body is Body.Grid -> model.moveCell(1, 0)
                model.nudge(1) -> true
                entersScope -> { if (!again) model.fill(); true }
                model.opened != null -> true                                  // nowhere to go from an action's row
                // On the arrow, Right again opens the row (a press of its own: a held key stops on the arrow).
                model.onMore -> { if (!again) model.open(); true }
                else -> model.arm(1, wrap = false)
            }
            Key.DirectionLeft -> when {
                !bare -> false
                model.opened != null -> { if (!again) model.close(); true }   // Left closes an opened row
                r?.body is Body.Grid -> atEnd && model.moveCell(-1, 0)
                atEnd && model.nudge(-1) -> true
                else -> model.arm(-1, wrap = false)     // on the first action Left is the caret's again
            }
            // One step back for one press: a held Backspace empties the text and stops there.
            Key.Backspace -> field.text.isEmpty() && (again || model.back())
            Key.Escape -> { onClose(); true }
            else -> {
                // Ctrl+1…9 runs that row's first action straight away (never one that removes something: those are never first).
                val n = DIGITS.indexOf(e.key)
                if (n >= 0 && e.isCtrlPressed && !e.isAltPressed && !e.isMetaPressed) {
                    if (!again) model.runRow(n) { row, a -> go(row, a, false) }
                    true
                } else false
            }
        }
    }

    val radiusPx = with(density) { Metrics.radius.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize().onPreviewKeyEvent(::keys)) {
        val full = maxWidth
        // The window as it stands in this frame. It follows the height's spring a frame behind (a window is resized through
        // the system), and its blur is the whole of it: so the glass and what is in it are sized from the window, never from
        // the spring, or a list that shrinks would leave a strip of blur with no glass on it under the panel.
        val windowPx = constraints.maxHeight
        Box(
            Modifier
                // The glass: as wide and as high as the arrival has opened it, centred on the seam.
                .layout { measurable, constraints ->
                    val box = glassBox(constraints.maxWidth, constraints.maxHeight)
                    val p = measurable.measure(Constraints.fixed(box.width, box.height))
                    layout(constraints.maxWidth, constraints.maxHeight) { p.place(box.left, box.top) }
                }
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = presence.value }
                .glass(
                    // The veil is white in light theme and near-black in dark: the most contrast for the least tint.
                    tint = if (glass) scheme.surfaceContainerLowest.copy(alpha = if (dark) Look.tintDark else Look.tintLight) else scheme.surfaceContainerHigh,
                    radiusPx = radiusPx, density = density.density,
                    run = { run.value }, glow = ::glow, tail = ::tail,
                    dark = dark, solid = !glass,
                )
                .clip(RoundedCornerShape(Metrics.radius)),
        ) {
            CompositionLocalProvider(LocalDark provides dark, LocalGlass provides glass) {
                // Laid out once at the panel's width and only uncovered: it never moves on screen while the glass grows.
                Column(
                    Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).requiredWidth(full)
                        .offset { IntOffset(0, -glassTop()) }
                        .graphicsLayer { alpha = shown() },
                ) {
                    Field(model, field, focus = focus, ends = ::ends, onChange = { v ->
                        model.type(v.text)
                        field = if (model.query == v.text) v else TextFieldValue(model.query, TextRange(model.query.length))
                    })

                    val body = when {
                        model.results.isNotEmpty() -> "results"
                        model.card != null -> "card:${model.card}"
                        model.tip != null -> "tip"
                        else -> "none"
                    }
                    val rows = body == "results"
                    val footerPx = with(density) { (Metrics.footer + Metrics.pad).roundToPx() }
                    // The footer comes and goes as a fade, and keeps its band at the window's lower edge for as long as any of it shows.
                    val foot = androidx.compose.animation.core.animateFloatAsState(if (rows) 1f else 0f, motion.fade(if (rows) 140 else 70), label = "footer")
                    // What is under the field shows as far as the window has grown; the footer rides the window's bottom
                    // edge, so it is never left behind by a list that grows and never lies over a row.
                    // What is under the field, and the footer, wait for the glass's edge while it opens: at the gate the glass
                    // is not yet as wide as a row, and its edge would cut the icons and the words at both ends.
                    fun edges() = if (folding) 1f else ends()
                    val target = Metrics.height(model)
                    val soft = with(density) { 20.dp.toPx() }
                    Box(
                        // Cut at its lower edge, which is the window's as it grows: softly while it is still growing, so a row
                        // that rises into room that is not there yet appears over its first lines and not at a hard line.
                        Modifier.fillMaxWidth().graphicsLayer { alpha = edges(); clip = true; compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                if (height < target - 1.dp && size.height > soft) drawRect(
                                    Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = size.height - soft, endY = size.height),
                                    topLeft = Offset(0f, size.height - soft), size = Size(size.width, soft), blendMode = BlendMode.DstIn,
                                )
                            }
                            .layout { measurable, constraints ->
                            val p = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                            // The footer's band is taken only from what the window has beyond row one's seat: while the window
                            // is still at the height of the copy's line (or a tip's), the first row and the pill stand whole in it.
                            val under = windowPx - fieldPx.roundToInt()
                            val seat = (Metrics.pad + Metrics.row + Metrics.pad).roundToPx()
                            val band = if (rows || foot.value > 0f) footerPx.coerceAtMost((under - seat).coerceAtLeast(0)) else 0
                            val room = (under - band).coerceAtLeast(0)
                            layout(p.width, room) { p.place(0, 0) }
                        },
                    ) {
                        AnimatedContent(
                            targetState = body,
                            transitionSpec = { (fadeIn(motion.fade(140)) togetherWith fadeOut(motion.fade(70))).using(null) },
                            contentAlignment = Alignment.TopStart,
                            label = "body",
                        ) { state ->
                            when {
                                state == "results" -> ResultsBody(model, icons) { r, a -> go(r, a, false) }
                                state.startsWith("card") -> model.card?.let { CardBody(it, model, cardChoice, onChoice = { c -> cardChoice = c }, onCard = onCard) }
                                state == "tip" -> TipBody(model)
                                else -> Spacer(Modifier.fillMaxWidth())
                            }
                        }
                        // The copy's line stands in row one's seat, over whatever comes into it: it fades where it
                        // stands while the rows arrive under it.
                        CopyLine(model)
                    }
                    // The footer shows once its band is (nearly) whole: while the window has not grown that far it would hang
                    // below the glass's lower edge and be cut by it.
                    val seatPx = with(density) { (Metrics.pad + Metrics.row + Metrics.pad).toPx() }
                    fun band() = smooth(0.75f, 1f, ((windowPx - fieldPx - seatPx) / footerPx).coerceIn(0f, 1f))
                    if (rows || foot.value > 0f) Box(Modifier.graphicsLayer { alpha = foot.value * edges() * band() }) { Footer(model) }
                }
            }
        }
    }
}

/** How long the open panel waits, with nothing typed, before a tip comes. */
private const val TIP_AFTER_MS = 320L

/**
 * A tip: the first-run card's shape. The feature's own mark, its name, one line that begins with
 * what to type (in full ink: it is what "Try it" types), and two answers. Neither answer is armed at
 * rest, so Enter on an empty panel still does nothing; a `tab` cap says how to get to them. Its
 * parts rise in one after the other, once the panel has made room.
 */
@Composable
private fun TipBody(model: OverlayModel) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val tip = remember { model.tip } ?: return
    val off = model.tipOff
    val arrive = remember { Animatable(if (motion.on) 0f else 1f) }
    // (They start once the panel's height has made most of their room: the glass's lower edge does not cut them.)
    LaunchedEffect(Unit) { arrive.animateTo(1f, motion.fade(140 + 2 * 22, delay = 120, easing = LinearEasing)) }
    val rise = with(LocalDensity.current) { 12.dp.toPx() }
    // Part [i] of three starts 22 ms after the one before it and takes 140 ms.
    fun Modifier.part(i: Int) = graphicsLayer {
        val t = ((arrive.value * (140f + 44f) - i * 22f) / 140f).coerceIn(0f, 1f)
        alpha = t; translationY = (1f - t) * rise
    }
    Row(Modifier.padding(horizontal = Metrics.pad).fillMaxWidth().height(Metrics.card).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.part(0).size(36.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = if (dark) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            Icon(Symbols.of(tip.symbol), null, Modifier.size(20.dp), tint = scheme.onSurface)
        }
        Column(Modifier.part(1).weight(1f).padding(start = 16.dp, end = 14.dp)) {
            Text(tip.name, color = scheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = TextStyle(fontFamily = Fonts.text, fontSize = 13.sp, fontWeight = FontWeight(500), lineHeight = 17.sp)
            AnimatedContent(off, transitionSpec = { motion.roll() }, contentAlignment = Alignment.TopStart, label = "line") { gone ->
                if (gone) Text(stringResource(R.string.tips_off), color = scheme.onSurface.copy(alpha = SECOND), style = line, maxLines = 2, overflow = TextOverflow.Ellipsis)
                else Text(androidx.compose.ui.text.buildAnnotatedString {
                    append(tip.example.trim())
                    addStyle(androidx.compose.ui.text.SpanStyle(color = scheme.onSurface), 0, length)
                    append(" "); append(tip.rest)
                }, color = scheme.onSurface.copy(alpha = SECOND), style = line, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        val answers by androidx.compose.animation.core.animateFloatAsState(if (off) 0f else 1f, motion.fade(60), label = "answers")
        Row(Modifier.part(2).graphicsLayer { alpha *= answers }, verticalAlignment = Alignment.CenterVertically) {
            // Nothing is armed until Tab: the cap says so, and goes once an answer is.
            val cap by androidx.compose.animation.core.animateFloatAsState(if (model.tipArmed == 0) 1f else 0f, motion.fade(120), label = "cap")
            Box(Modifier.padding(end = 10.dp).graphicsLayer { alpha = cap }) { Keycap("tab") }
            OptionStrip(listOf(stringResource(R.string.action_try), stringResource(R.string.tips_turn_off)), (model.tipArmed - 1).coerceAtLeast(0),
                onChoose = { model.tipArm(it + 1) }, onRun = { model.tipArm(it + 1); model.tipEnter() }, vertical = true, lit = model.tipArmed != 0)
        }
    }
}

private val DIGITS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)

/** The first-run card: one small step under the field, only while nothing is typed. Its two answers are one option strip. */
@Composable
private fun CardBody(card: Card, model: OverlayModel, choice: Int, onChoice: (Int) -> Unit, onCard: (Card, Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val engine = model.settings.engine()
    val (title, text, yes, no) = when (card) {
        Card.SHORTCUT -> listOf(stringResource(R.string.card_shortcut_title), stringResource(R.string.card_shortcut_text),
            stringResource(R.string.card_shortcut_action), stringResource(R.string.card_done))
        Card.SUGGESTIONS -> listOf(stringResource(R.string.card_suggest_title), stringResource(R.string.card_suggest_text, engine.name),
            stringResource(R.string.card_suggest_on), stringResource(R.string.card_not_now))
    }
    Row(
        Modifier.padding(horizontal = Metrics.pad).fillMaxWidth().height(Metrics.card)
            // No plate of its own: a box inside the glass would read as a second rim.
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Symbols.booklight, null, Modifier.size(20.dp), tint = scheme.primary)
        }
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 14.dp)) {
            Text(title, color = scheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text, color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 13.sp, fontWeight = FontWeight(500), lineHeight = 17.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        OptionStrip(listOf(yes, no), choice, onChoose = onChoice, onRun = { onCard(card, it == 0) }, vertical = true)
    }
}
