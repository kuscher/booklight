package io.github.kuscher.booklight.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import io.github.kuscher.booklight.ui.lightScheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// First run's welcome, "Lights on" (docs/design/first-run/design.md §3, motion.md §2.1). For four seconds the glass is a
// stage: it grows to 720 × 468 dp and goes to night, the field lights up from its caret and is a lamp's head, a shaft
// of light falls from it, the words stand in the light, and a small Swiss Army knife of five tools flicks out of a
// handle that carries the cue. Then the light folds back into the caret, and the handle rises to row one's seat and is
// the list's highlight.
//
// It is drawn on one clock. Every number that is drawn is a function of that clock's time and of two moments (when
// the hand-over began, when a key put the lamp out): nothing is kept from frame to frame. A frame composes nothing and
// lays nothing out: the type is laid out once, and everything is drawn in the draw phase. Nothing is drawn outside
// the glass, which clips it. This file is used only by a panel that began with the opening piece.

// The stage's measures, in dp of a panel 720 wide, from the panel's left and top edges (design.md §3, "Lights on, in full").
/** The panel's width, and the glass's height in the welcome. */
private const val WIDE = 720f
private const val HIGH = 468f
/** The seam's axis: everything of the welcome is centred on it. */
private const val AXIS = 360f
/** The lamp's head is the field's band, 0 to 68, its corners the glass's own. */
private const val HEAD = 68f
private const val CORNER = 32f
/** The caret, x 72 to 74, y 20 to 48: where the lamp strikes, and what its light folds back into. */
private const val CARET = 73f
private const val CARET_TOP = 20f
private const val CARET_HIGH = 28f
/** The light at rest: its upper edge on y = 92, from x 129 to 591 (0.64 of the head's width); its foot on y = 448, from 32 to 688 (0.91). */
private const val TOP = 92f
private const val FOOT = 448f
private const val TOP_HALF = 231f
private const val FOOT_HALF = 328f
/** As a seam it is 6 dp wide. */
private const val SEAM_HALF = 3f
/** Its edges: hard as a seam, soft once it is open; its penumbra and its core are softer still. */
private const val HARD = 1.5f
private const val SOFT = 20f
private const val HALO_SOFT = 60f
private const val CORE_SOFT = 50f
/** How far the light has spread through the head when it has reached its right end, from the caret. */
private const val FLOOD = WIDE - CARET
/** The title's box, y 124 to 172, and the line's, y 188 to 210; both centred on the axis. */
private const val TITLE_TOP = 124f
private const val TITLE_HIGH = 48f
private const val LINE_TOP = 188f
private const val LINE_HIGH = 22f
/** The title is set at 40, and smaller where it would be wider than this: it stays inside the light, which is 492 wide there. */
private const val TITLE = 40f
private const val TITLE_ROOM = 470f
private const val LINE = 17f
/** The handle, 216 × 48 with a radius of 24: left, top, right, bottom. And row one's seat, where it becomes the list's pill. */
private val HANDLE = floatArrayOf(252f, 372f, 468f, 420f)
private val SEAT = floatArrayOf(8f, 76f, 712f, 132f)
private const val ROUND = 24f
/** It rises this far into the light, as a row rises. */
private const val RISES = 12f
/** The five tools: pivots on the handle's upper edge from x = 288, 36 apart; a stem of 64 to the middle of a disc of 36; a symbol of 20 in it. */
private const val PIVOT = 288f
private const val PIVOTS = 36f
private const val STEM = 64f
private const val DISC = 18f
private const val SYMBOL = 20f
/** Folded, a tool lies along the handle and behind it: this far from upright, the left three to the right and the right two to the left. Open, they stand 40° and 20° to the left of upright, upright, 20° and 40° to the right. */
private const val FOLDED = 112f
private const val FAN = 20f
/** Left to right: the four beats of the show, and the key's step. */
private val TOOLS = listOf("app", "calc", "plane", "smile", "key")

/** Whether a point of the glass, in the stage's own measures, lies on the handle as it stands: a click there is the cue. */
internal fun onHandle(x: Float, y: Float): Boolean = x in HANDLE[0]..HANDLE[2] && y in HANDLE[1]..HANDLE[3]

// The light's strength, as white over the night (motion.md §2.1, "The light, as one pass").
/** Down the shaft: brightest under the lamp, not one even ramp. */
private val FALL = floatArrayOf(0.34f, 0.24f, 0.17f, 0.125f, 0.10f)
/** Its penumbra, outside its edges, and its core, along its axis. */
private val HALO = floatArrayOf(0.05f, 0.02f)
private val CORE = floatArrayOf(0.16f, 0.07f, 0.03f)
/** Twelve rays: where across the shaft (−1 to 1) a little more light lies, and how wide. They overlap, so no edge of one is seen. */
private val RAYS = floatArrayOf(-0.84f, 0.10f, -0.66f, 0.05f, -0.52f, 0.13f, -0.33f, 0.07f, -0.20f, 0.16f, -0.04f, 0.06f, 0.10f, 0.12f, 0.27f, 0.05f, 0.40f, 0.15f, 0.57f, 0.08f, 0.70f, 0.12f, 0.86f, 0.07f)
private val RAY_WIDTHS = floatArrayOf(1f, 0.6f, 0.3f)
private const val RAY = 0.016f
/** The lit head, the words where no light falls, a tool's stem and disc, the disc's ring, a glint; and the breath of the light while it stands. */
private const val HEAD_LIGHT = 0.92f
private const val UNLIT = 0.38f
private const val STEM_INK = 0.60f
private const val DISC_INK = 0.16f
private const val RING_INK = 0.30f
private const val GLINT = 0.70f
private const val BREATH = 0.06f
/** The handle is the selection as light theme has it, a little denser on the night, with the selection's white rim. Pressed, it gains a little ink. */
private const val HANDLE_FILL = 0.92f
private const val HANDLE_RIM = 0.55f
private const val PRESSED = 0.08f

/**
 * The night's deeper shade towards the corners: none in the middle of the glass, [DEEP] of the night's colour 520 dp
 * out. It grows with the square of the distance, in steps too fine to see, so it has no edge anywhere: a shade that
 * began at a ring 200 dp out showed that ring once the glass under it was no longer dark.
 */
private const val DEEP = 0.6f
private val DEEPER: Brush = Brush.radialGradient(*Array(17) { it / 16f to NIGHT.copy(alpha = DEEP * (it / 16f) * (it / 16f)) }, center = Offset(AXIS, 230f), radius = 520f)

private const val NEVER = Float.POSITIVE_INFINITY
private val CLEAR = Color.White.copy(alpha = 0f)

private fun mix(a: Float, b: Float, x: Float) = a + (b - a) * x
/** How far something that began at [from] and takes [ms] has come at [t], 0 to 1, on [curve]. Something that never began ([from] is [NEVER]) has not come at all. */
private fun tw(t: Float, from: Float, ms: Int, curve: Easing = FastOutSlowInEasing): Float = curve.transform(((t - from) / ms).coerceIn(0f, 1f))
private fun soften(a: Float, b: Float, x: Float): Float { val t = ((x - a) / (b - a)).coerceIn(0f, 1f); return t * t * (3f - 2f * t) }

// (The panel's springs as functions of the welcome's clock, `PLACE` and `POP`, are `Motion.kt`'s: first run's stage is drawn by them too.)

/** How dark the night is, 0 to 1: it falls from the gate on, and lifts as the shaft turns back into the caret, or once a key has put the lamp out. */
private fun nightAt(t: Float, hand: Float, off: Float): Float {
    fun alive(at: Float) = tw(at, 0f, Lights.NIGHT_MS) * (1f - tw(at, hand + Lights.TURN_AFTER_MS, Lights.LIFT_MS))
    return if (t >= off) alive(off) * (1f - (t - off) / Lights.DAWN_MS).coerceIn(0f, 1f) else alive(t)
}

/** How bright the lamp is, 0 to 1: it strikes, sags (it has not caught), and catches, fast at first. */
private fun lampAt(t: Float): Float {
    val s = t - Lights.STRIKE
    return when {
        s < 0f -> 0f
        s < Lights.STRIKE_UP_MS -> 0.6f * s / Lights.STRIKE_UP_MS
        s < Lights.STRIKE_SAG_MS -> mix(0.6f, 0.35f, (s - Lights.STRIKE_UP_MS) / (Lights.STRIKE_SAG_MS - Lights.STRIKE_UP_MS))
        else -> ((s - Lights.STRIKE_SAG_MS) / (Lights.STRIKE_ON_MS - Lights.STRIKE_SAG_MS)).coerceIn(0f, 1f).let { mix(0.35f, 1f, 1f - (1f - it) * (1f - it)) }
    }
}

/** How far the head's light has closed on the caret again, 0 to 1. */
private fun closedAt(t: Float, hand: Float) = tw(t, hand + Lights.CLOSE_AFTER_MS, Lights.CLOSE_MS, SEAM_DRAWS_IN)
/** How far the lamp's light has flooded the head, 0 to 1: from the caret to both ends. */
private fun floodAt(t: Float) = tw(t, (Lights.STRIKE + Lights.FLOOD_AFTER_MS).toFloat(), Lights.FLOOD_MS, SEAM_GROWS)
/** Where the front of the head's light stands on its way from the caret to the field's right end. */
private fun frontAt(t: Float) = CARET + FLOOD * floodAt(t)

/** The light as a shape: where its middle stands at its upper edge and at its foot, half its width at each, how far it has fallen, and how soft its edges are. */
private class Shaft(val top: Float, val topHalf: Float, val foot: Float, val footHalf: Float, val fallen: Float, val soft: Float)

/**
 * The welcome at one moment of its clock: every number that is drawn. [t]: ms after the gate. [hand]: when the
 * hand-over began ([NEVER]: not yet). [off]: when a key put the lamp out ([NEVER]: none has).
 */
private class Lit(val t: Float, val hand: Float, off: Float) {
    /** A key has put the lamp out: the light and the head are gone at once, and what stands fades where it stands. */
    val dead = t >= off
    val fades = if (dead) (1f - (t - off) / Lights.OUT_MS).coerceIn(0f, 1f) else 1f
    val night = nightAt(t, hand, off)
    /** How much of the corners' deeper shade there is: none until the night has half fallen, all of it at full night, and gone again before the glass is the theme's own. */
    val deep = soften(Lights.DEEPENS_FROM, 1f, night)
    val lamp = if (dead) 0f else lampAt(t)
    val struck = t - Lights.STRIKE
    private val closed = closedAt(t, hand)
    /** The head's light, from the caret to both ends, and closing on the caret again at the end. */
    val headLeft = mix((CARET - (frontAt(t) - CARET)).coerceAtLeast(0f), CARET - 1f, closed)
    val headRight = mix(frontAt(t).coerceAtMost(WIDE), CARET + 1f, closed)
    val head = lamp > 0f && closed < 1f

    private val turn = tw(t, hand + Lights.TURN_AFTER_MS, Lights.TURN_MS, FOLDS)
    /** How far the light is open out of its seam. */
    val open = tw(t, Lights.OPEN.toFloat(), Lights.OPEN_MS, Motion.OPENS) * (1f - turn)
    private val fallen = TOP + (FOOT - TOP) * tw(t, Lights.SEAM.toFloat(), Lights.SEAM_MS, SEAM_GROWS) * (1f - tw(t, hand + Lights.TURN_AFTER_MS, Lights.RISE_MS, FastOutLinearInEasing))
    private val topAt = mix(AXIS, CARET, turn)
    private val footAt = mix(AXIS, CARET, tw(t, hand + Lights.FOOT_AFTER_MS, Lights.TURN_MS, FOLDS))
    private val topHalf = mix(SEAM_HALF, TOP_HALF, open)
    private val footHalf = mix(SEAM_HALF, FOOT_HALF, open)
    val shaft = Shaft(topAt, topHalf, footAt, footHalf, fallen, mix(HARD, SOFT, open))
    val halo = Shaft(topAt, topHalf + 14f * open, footAt, footHalf + 24f * open, fallen, mix(HARD, HALO_SOFT, open))
    val core = Shaft(topAt, topHalf / 2f, footAt, footHalf / 2f, fallen, mix(HARD, CORE_SOFT, open))
    val shines = !dead && fallen > TOP + 1f
    /** As a seam it is full white; it relaxes to its fall-off as it opens. And it breathes once while it stands. */
    val relaxed = soften(0f, 0.6f, open)
    val bright = lamp.coerceAtMost(1f) * (1f + BREATH * sin(PI.toFloat() * ((t - Lights.STANDS) / (Lights.HAND - Lights.STANDS)).coerceIn(0f, 1f)))

    /**
     * The words in the lamp's room: they show dimly as its light floods the head (the lamp lights the room; at the strike
     * itself there is nothing under the field yet), and are gone as the light leaves them.
     */
    val ambient: Float = (lampAt(minOf(t, off)).coerceAtMost(1f) * floodAt(minOf(t, off)) * (1f - tw(minOf(t, off), hand + Lights.WORDS_OUT_AFTER_MS, Lights.WORDS_OUT_MS))) * fades

    /** The handle: born in the light's last third (at once, where the cue came before that), and from the hand-over on on its way to row one's seat. */
    private val born = minOf(Lights.HANDLE.toFloat(), hand)
    val there = (if (t >= born && !(dead && off < born)) tw(t, born, Lights.HANDLE_MS) else 0f) * fades
    val rise = RISES * (1f - PLACE.at(t - born))
    private val gone = PLACE.at(t - (hand + Lights.GO_AFTER_MS))
    val box = FloatArray(4) { mix(HANDLE[it] + if (it % 2 == 1) rise else 0f, SEAT[it], gone) }
    val pressed = tw(t, hand, Lights.PRESS_MS) * (1f - tw(t, hand + Lights.PRESS_MS, Lights.RELEASE_MS))
    /** How much of the night's own colour the handle has: all of it until the hand-over, then as much as there is night. */
    val handleNight = if (t < hand) 1f else night
    val cue = there * (1f - tw(t, hand + Lights.CUE_OUT_AFTER_MS, Lights.CUE_OUT_MS))
}

/**
 * What of first run's opening piece is drawn under the field and the rows, in a layer of the glass's own that is as
 * wide as the panel and as high as the welcome's glass: the welcome. And the desk behind the panel: dimmed by half in
 * the welcome's night (the welcome says how far, frame by frame), a little through the show, and lifting once the
 * key's step lands. [gate]: the glass has opened far enough for what is under the field to come. [ends]: how much of
 * the field's two ends shows while the glass opens. [onDesk]: how far the desk is to dim, 0 to 1, for this frame.
 */
@Composable
fun Piece(model: OverlayModel, gate: Boolean, ends: () -> Float, night: Night, onDesk: (Float) -> Unit) {
    val motion = LocalMotion.current
    /** The show's own dim: what "Dim the desktop" would be. */
    val through = if (LocalDark.current) Look.dimDark else Look.dimLight
    val desk = remember { Animatable(0f) }
    val phase = when (model.playing) { FirstRun.Playing.WELCOME -> 0; FirstRun.Playing.SHOW -> 1; else -> 2 }
    LaunchedEffect(phase) { if (phase == 1) desk.snapTo(through) else if (phase == 2) desk.animateTo(0f, motion.fade(Motion.DESK_LIFTS_MS)) }
    LaunchedEffect(Unit) { snapshotFlow { desk.value }.collect { if (model.playing != FirstRun.Playing.WELCOME) onDesk(it) } }
    Welcome(model, gate, ends, night, through, onDesk)
    Glide(model)
}

/**
 * The one highlight on its way into the key's step (docs/design/first-run/design.md §3, "The landing in K1"): from
 * where the show left it (the grid's square, or the list's pill) into the armed answer of the screen that lands. It is
 * drawn in the piece's layer, under the field and the rows, so the answers' words stand over it. Its leading edge goes
 * first, the other follows two frames later, and top, bottom and corner go on `place`. Arrived, it is the answers' own
 * highlight: that one comes in its usual fade, and this one goes as that one comes, at the strength that makes the two
 * together one highlight of the selection's own strength throughout.
 */
@Composable
private fun Glide(model: OverlayModel) {
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val fill = selectionFill(MaterialTheme.colorScheme, dark, LocalGlass.current)
    val rim = Color.White.copy(alpha = if (dark) 0.30f else 0.55f)
    // Where it set off: kept here while it hands over, after the model has let it go.
    var from by remember { mutableStateOf<FloatArray?>(null) }
    model.glideFrom?.let { if (from !== it) from = it }
    // The one that has arrived, and is handing over to the answers' own: the model let go of that one by this effect's word.
    var arrived by remember { mutableStateOf<FloatArray?>(null) }
    val lead = remember { Animatable(0f) }
    val trail = remember { Animatable(0f) }
    val place = remember { Animatable(0f) }
    val given = remember { Animatable(0f) }
    LaunchedEffect(from) {
        val mine = from ?: return@LaunchedEffect
        lead.snapTo(0f); trail.snapTo(0f); place.snapTo(0f); given.snapTo(0f)
        // It leaves once what it stood on has begun to fade.
        delay(motion.hold(Motion.GLIDE_AFTER_MS))
        coroutineScope {
            launch { lead.animateTo(1f, motion.pillLead()) }
            launch { withFrameNanos { }; withFrameNanos { }; trail.animateTo(1f, motion.pillTrail(far = true)) }
            launch { place.animateTo(1f, motion.place()) }
        }
        arrived = mine
        model.glided()
        given.animateTo(1f, motion.fade(Motion.TAKES_OVER_MS))
        from = null
    }
    // (A typed character takes the stage away under it: then it is simply gone, and it does not go on from where it was
    // should the field be emptied before it would have arrived. So it is where the model lets go of it before it has
    // arrived: a panel that closes, whose leave may be turned round, has the answers' own highlight from that frame on,
    // and this one is drawn no more: one highlight, never two. The model is told too: forgetting it here ends the
    // effect above before its own word that it has arrived, and the model would go on taking it for travelling, with the
    // list's pill hidden and the answers unlit for as long as the panel stands.)
    val gone = model.stage == null || (from != null && model.glideFrom == null && arrived !== from)
    LaunchedEffect(gone) { if (gone) { from = null; model.glided() } }
    val start = from?.takeIf { !gone } ?: return
    Canvas(Modifier.fillMaxSize()) {
        val u = size.width / WIDE
        val to = model.answerAt
        fun edge(i: Int, by: Float) = if (to == null) start[i] * u else mix(start[i] * u, to[i], by)
        // To the right the right edge leads, to the left the left one.
        val right = to == null || to[2] >= start[2] * u
        val l = edge(0, if (right) trail.value else lead.value)
        val r = edge(2, if (right) lead.value else trail.value)
        val t = edge(1, place.value)
        val b = edge(3, place.value)
        val corner = CornerRadius(mix(start[4], 16f, place.value.coerceIn(0f, 1f)) * u)
        // The answers' own highlight is there at `given` of its strength: this one is what is missing of one whole highlight.
        val a = fill.alpha
        val g = given.value.coerceIn(0f, 1f)
        val own = if (g <= 0f) 1f else if (a >= 0.99f) 1f - g else ((1f - (1f - a) / (1f - a * g)) / a).coerceIn(0f, 1f)
        if (r - l > 1f && b - t > 1f) {
            drawRoundRect(fill.copy(alpha = a * own), Offset(l, t), Size(r - l, b - t), corner)
            drawRoundRect(rim.copy(alpha = rim.alpha * own), Offset(l + 0.5f, t + 0.5f), Size(r - l - 1f, b - t - 1f), corner, style = Stroke(1f))
        }
    }
}

@Composable
private fun Welcome(model: OverlayModel, gate: Boolean, ends: () -> Float, night: Night, through: Float, onDesk: (Float) -> Unit) {
    val motion = LocalMotion.current
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDark.current
    val density = LocalDensity.current
    // The clock: ms after the gate. Before the gate there is nothing but the caret.
    val clock = remember { mutableFloatStateOf(0f) }
    var hand by remember { mutableFloatStateOf(NEVER) }
    var off by remember { mutableFloatStateOf(NEVER) }
    var over by remember { mutableStateOf(model.playing != FirstRun.Playing.WELCOME) }

    // One loop for the whole welcome. It ends in one of two ways: the hand-over is done and the show begins, or a key
    // has put the lamp out and the night has lifted. (A debug hook can begin it again in the open panel: `rounds`.)
    LaunchedEffect(gate, model.rounds) {
        if (!gate) return@LaunchedEffect
        // (A key ended it before the glass had opened: nothing of the welcome was drawn, and nothing of it stays.)
        if (model.playing != FirstRun.Playing.WELCOME) { over = true; return@LaunchedEffect }
        hand = NEVER; off = NEVER; clock.floatValue = 0f; over = false
        var t = 0f
        var before = 0L
        var late = false
        while (true) {
            val now = withFrameNanos { it }
            val stands = model.standsAt
            // (A debug build's slow motion stretches the clock, and a hook can stand it still at a moment of the paper's own clock.)
            t = if (stands != null) stands - Lights.GATE else t + (if (before == 0L) 0f else (now - before) / 1_000_000f / motion.slow)
            before = now
            if (model.playing == FirstRun.Playing.WELCOME) {
                if (hand == NEVER) {
                    when {
                        stands != null -> if (t >= Lights.HAND) hand = Lights.HAND.toFloat()
                        // The cue (the user's Enter, a click on the handle) begins the hand-over in this frame, from wherever the
                        // parts are; or Booklight presses the handle itself, once the welcome has stood. What then happens is core's
                        // to say (`FirstRun.handOver`): with a show it begins; where this device has nothing to show the key's step
                        // lands at once; while its apps are still being read the welcome stands on, a second at most.
                        model.cued || t >= Lights.HAND -> when (FirstRun.handOver(model.casts, waited = t >= Lights.HAND + Lights.WAITS_MS)) {
                            FirstRun.Ends.BEGIN -> hand = if (model.cued || late) t else Lights.HAND.toFloat()
                            FirstRun.Ends.LAND -> model.land()
                            else -> if (t >= Lights.HAND) late = true
                        }
                    }
                } else if (stands == null && t >= hand + Lights.HAND_MS) {
                    // The hand-over is done: the show's first letter lands, and its list comes with the pill in the seat the
                    // handle has reached. The handle is not drawn again: never two highlights in one frame.
                    model.show()
                    if (model.playing != FirstRun.Playing.WELCOME) break
                }
                // The handle carries the cue once it stands, and until it is pressed.
                model.cueUp = hand == NEVER && t >= Lights.HANDLE + Lights.HANDLE_MS
                // Booklight's mark comes up in the field's seat as the head's light closes past it, before the first letter.
                model.marked = hand != NEVER && t >= hand + Lights.MARK_AFTER_MS
            } else {
                // The show has begun: nothing of the welcome is left. Or a key ended it: the lamp is out in this frame.
                if (model.playing == FirstRun.Playing.SHOW) break
                if (off == NEVER) off = t
                if (t >= off + Lights.DAWN_MS) break
            }
            clock.floatValue = t
            val n = nightAt(t, hand, off)
            // A key that types puts the lamp out as a lamp goes out: the veil is the theme's own at once, so that the typed
            // character stands in the theme's ink; the outline, and what is left of the night under the field, relax.
            night.rim = n; night.veil = if (t >= off) 0f else n
            onDesk(mix(if (t >= hand && t < off) through else 0f, NIGHT_DESK, n))
        }
        night.rim = 0f; night.veil = 0f; over = true
        // Where a key ended the welcome the desk behind is as on any day again, to the last trace: the loop's last frame
        // still asked for what was left of the night. (Where the show has begun, it has the desk from here.)
        if (model.playing != FirstRun.Playing.SHOW) onDesk(0f)
    }
    if (over) return

    val light = lightScheme(model.settings.tint)
    val sel = selectionFill(scheme, dark, LocalGlass.current)
    val handle = light.secondaryContainer.copy(alpha = HANDLE_FILL)
    val ink = light.onSecondaryContainer
    val rim = if (dark) 0.30f else 0.55f
    val caret = scheme.primary
    val marks = TOOLS.map { rememberVectorPainter(Symbols.of(it)) }
    val enter = rememberVectorPainter(Symbols.enter)
    // The type is laid out once. Its sizes are the design's dp, whatever the system's font scale is: it is a picture,
    // and it must stay inside the light.
    val measurer = rememberTextMeasurer()
    val title = stringResource(R.string.first_hello_title)
    val line = stringResource(R.string.first_hello_line)
    val cue = stringResource(R.string.first_hello_cue)
    val laid = remember(title, line, cue, density) {
        with(density) {
            fun lay(text: String, family: FontFamily, weight: Int, size: Float) =
                measurer.measure(text, TextStyle(fontFamily = family, fontWeight = FontWeight(weight), fontSize = size.dp.toSp()), maxLines = 1, softWrap = false)
            val wide = lay(title, Fonts.round, 600, TITLE).size.width / 1.dp.toPx()
            listOf(lay(title, Fonts.round, 600, if (wide > TITLE_ROOM) TITLE * TITLE_ROOM / wide else TITLE), lay(line, Fonts.text, 500, LINE), lay(cue, Fonts.text, 600, LINE))
        }
    }

    // (A picture: nothing of it is a screen reader's. The welcome is not played with one on; one switched on midway finds the field.)
    Box(Modifier.fillMaxSize().clearAndSetSemantics { }) {
        Canvas(Modifier.fillMaxSize()) {
            // The design's own units: `u` px to a dp of a panel 720 wide.
            val u = size.width / WIDE
            val f = Lit(clock.floatValue, hand, off)
            val t = f.t
            scale(u, u, Offset.Zero) {
                // The night is deeper towards the corners. Once a key has put the lamp out, what is left of it lifts under the field.
                if (f.dead) { if (f.night > 0f) drawRect(NIGHT.copy(alpha = NIGHT_VEIL * f.night), Offset(0f, HEAD), Size(WIDE, HIGH - HEAD)) }
                else if (f.deep > 0f) drawRect(DEEPER, Offset.Zero, Size(WIDE, HIGH), alpha = f.deep)
                if (f.head) {
                    // The strike: for a fifth of a second a halo stands round the caret, brightest at the lamp's first flare.
                    if (f.struck < Lights.STRIKE_ON_MS) drawRect(
                        Brush.radialGradient(0f to Color.White.copy(alpha = 0.9f), 1f to CLEAR, center = Offset(CARET, HEAD / 2f), radius = 80f), Offset.Zero, Size(160f, 120f),
                        alpha = (if (f.struck < Lights.STRIKE_UP_MS) f.struck / Lights.STRIKE_UP_MS else 1f - (f.struck - Lights.STRIKE_UP_MS) / (Lights.STRIKE_ON_MS - Lights.STRIKE_UP_MS)).coerceIn(0f, 1f),
                    )
                    // The head: the field's band, lit from the caret to both ends, the light's front soft.
                    val edge = 30f
                    val q = edge / (f.headRight - f.headLeft + 2f * edge)
                    clipPath(Path().apply { addRoundRect(RoundRect(0f, 0f, WIDE, HEAD, CornerRadius(CORNER))) }) {
                        drawRect(Brush.horizontalGradient(0f to CLEAR, q to Color.White, 1f - q to Color.White, 1f to CLEAR, startX = f.headLeft - edge, endX = f.headRight + edge), Offset.Zero, Size(WIDE, HEAD), alpha = (HEAD_LIGHT * f.lamp).coerceIn(0f, 1f))
                    }
                }
                if (f.shines) {
                    // The light: a penumbra outside its edges, the shaft with its fall-off and its rays, a core along its axis.
                    if (f.relaxed > 0.02f) light(f.halo, FloatArray(HALO.size) { HALO[it] * f.relaxed }, f.bright, 0f)
                    light(f.shaft, FloatArray(FALL.size) { mix(1f, FALL[it], f.relaxed) }, f.bright, f.relaxed)
                    if (f.relaxed > 0.02f) light(f.core, FloatArray(CORE.size) { CORE[it] * f.relaxed }, f.bright, 0f)
                }
            }
            // The words: dim in the lamp's room, white where the light falls, with a little bloom. The lit layer is masked by
            // the light's own soft edges, so the light opens across the letters.
            if (f.ambient > 0f) {
                val titleAt = Offset((size.width - laid[0].size.width) / 2f, TITLE_TOP * u + (TITLE_HIGH * u - laid[0].size.height) / 2f)
                val lineAt = Offset((size.width - laid[1].size.width) / 2f, LINE_TOP * u + (LINE_HIGH * u - laid[1].size.height) / 2f)
                drawText(laid[0], Color.White, titleAt, alpha = UNLIT * f.ambient)
                drawText(laid[1], Color.White, lineAt, alpha = UNLIT * f.ambient)
                if (f.shines) {
                    val area = Rect(0f, TOP, WIDE, 240f)
                    drawIntoCanvas { it.saveLayer(Rect(0f, area.top * u, size.width, area.bottom * u), Paint().apply { alpha = f.lamp.coerceIn(0f, 1f) }) }
                    val bloom = Shadow(Color.White.copy(alpha = 0.45f), Offset.Zero, 9f * u)
                    drawText(laid[0], Color.White, titleAt, shadow = bloom)
                    drawText(laid[1], Color.White, lineAt, shadow = bloom)
                    scale(u, u, Offset.Zero) { edges(f.shaft, area, upper = false) }
                    drawIntoCanvas { it.restore() }
                }
            }
            scale(u, u, Offset.Zero) {
                if (f.there > 0f) {
                    // The knife: five tools flick out from behind the handle, left to right, a beat apart, each once past its
                    // angle and back; a glint crosses each disc as it opens. At the hand-over they fold back, right to left.
                    for (i in TOOLS.indices) {
                        val since = t - Lights.TOOLS - Lights.BEAT_MS * i
                        val back = if (f.dead) 0f else tw(t, hand + Lights.FOLD_EACH_MS * (TOOLS.lastIndex - i), Lights.FOLD_MS, FastOutLinearInEasing)
                        if (since < 0f || back >= 1f) continue
                        val folded = if (i < 3) FOLDED else -FOLDED
                        val angle = Math.toRadians(mix(mix(folded, FAN * (i - 2), POP.at(since)), folded, back).toDouble())
                        val sx = sin(angle).toFloat()
                        val cx = cos(angle).toFloat()
                        val px = PIVOT + PIVOTS * i
                        val py = HANDLE[1] + f.rise
                        val at = Offset(px + STEM * sx, py - STEM * cx)
                        drawLine(Color.White.copy(alpha = STEM_INK * f.there), Offset(px, py), Offset(at.x - DISC * sx, at.y + DISC * cx), 4f, StrokeCap.Round)
                        drawCircle(Color.White.copy(alpha = DISC_INK * f.there), DISC, at)
                        drawCircle(Color.White.copy(alpha = RING_INK * f.there), DISC, at, style = Stroke(1f))
                        translate(at.x - SYMBOL / 2f, at.y - SYMBOL / 2f) { with(marks[i]) { draw(Size(SYMBOL, SYMBOL), alpha = f.there, colorFilter = ColorFilter.tint(Color.White)) } }
                        if (since > Lights.GLINT_AFTER_MS && since < Lights.GLINT_AFTER_MS + Lights.GLINT_MS) {
                            val gx = at.x + mix(-30f, 30f, (since - Lights.GLINT_AFTER_MS) / Lights.GLINT_MS)
                            clipPath(Path().apply { addOval(Rect(at, DISC)) }) {
                                rotate(45f, at) { drawRect(Brush.horizontalGradient(0f to CLEAR, 0.5f to Color.White.copy(alpha = GLINT), 1f to CLEAR, startX = gx - 7f, endX = gx + 7f), Offset(gx - 7f, at.y - 26f), Size(14f, 52f), alpha = f.there) }
                            }
                        }
                    }
                    // The handle, over the folded tools: the one coloured surface. It is the selection: as light theme has it
                    // while it is night, the theme's own as the night lifts. From the hand-over on it travels to row one's
                    // seat, and there it is the list's pill.
                    val fill = lerp(sel, handle, f.handleNight).let { lerp(it, ink.copy(alpha = it.alpha), PRESSED * f.pressed) }
                    val b = f.box
                    drawRoundRect(fill.copy(alpha = fill.alpha * f.there), Offset(b[0], b[1]), Size(b[2] - b[0], b[3] - b[1]), CornerRadius(ROUND))
                    drawRoundRect(Color.White.copy(alpha = mix(rim, HANDLE_RIM, f.handleNight) * f.there), Offset(b[0] + 0.5f / u, b[1] + 0.5f / u), Size(b[2] - b[0] - 1f / u, b[3] - b[1] - 1f / u), CornerRadius(ROUND), style = Stroke(1f / u))
                }
                // The caret is the switch: the theme's own until the night falls, white in the night, off for a blink before
                // the lamp strikes, and in the lit head the dark slit of Booklight's mark. At the end it is all that is left of
                // the lamp. (Once a key has ended the welcome the field's own caret is back.)
                if (!f.dead && !(t >= Lights.CARET_OFF && t < Lights.STRIKE))
                    drawRect(if (f.head && f.headRight > CARET + 2f) NIGHT else lerp(caret, Color.White, f.night), Offset(CARET - 1f, CARET_TOP), Size(2f, CARET_HIGH), alpha = ends())
            }
            // The cue, on the handle wherever the handle is: the words, 8 dp, the Enter mark at 16.
            if (f.cue > 0f) {
                val cx = (f.box[0] + f.box[2]) / 2f * u
                val cy = (f.box[1] + f.box[3]) / 2f * u
                val wide = laid[2].size.width + 24f * u
                drawText(laid[2], ink, Offset(cx - wide / 2f, cy - laid[2].size.height / 2f), alpha = f.cue)
                translate(cx + wide / 2f - 16f * u, cy - 8f * u) { with(enter) { draw(Size(16f * u, 16f * u), alpha = f.cue, colorFilter = ColorFilter.tint(ink)) } }
            }
        }
        // The field's `esc` cap, where the field has it (its right edge at 700): white in the night, and in the night's ink
        // once the lit head is under it.
        if (model.playing == FirstRun.Playing.WELCOME) {
            val dusk by remember { derivedStateOf { night.rim > 0.5f } }
            val lit by remember { derivedStateOf { clock.floatValue.let { it < off && lampAt(it) > 0f && frontAt(it) > 700f && closedAt(it, hand) < 0.05f } } }
            Box(Modifier.align(Alignment.TopEnd).padding(end = 20.dp).height(Metrics.field).graphicsLayer { alpha = ends() }, contentAlignment = Alignment.Center) {
                MaterialTheme(colorScheme = scheme.copy(onSurface = if (lit) NIGHT else if (dusk) Color.White else scheme.onSurface)) {
                    CompositionLocalProvider(LocalDark provides (if (lit) false else if (dusk) true else dark)) { Keycap("esc", wide = true) }
                }
            }
        }
    }
}

/** The light as one pass: its fall-off down the shaft ([stops], from its upper edge to its foot), a few rays, and its four soft edges. */
private fun DrawScope.light(s: Shaft, stops: FloatArray, bright: Float, rays: Float) {
    val area = Rect(0f, HEAD - 8f, WIDE, HIGH + 12f)
    drawIntoCanvas { it.saveLayer(area, Paint()) }
    val fall = Array(stops.size) { it / (stops.size - 1f) to Color.White.copy(alpha = (stops[it] * bright).coerceIn(0f, 1f)) }
    drawRect(Brush.verticalGradient(*fall, startY = TOP, endY = maxOf(TOP + 1f, s.fallen)), area.topLeft, area.size)
    if (rays > 0f) {
        val ray = Path()
        val ink = Color.White.copy(alpha = (RAY * rays * bright).coerceIn(0f, 1f))
        for (i in RAYS.indices step 2) for (w in RAY_WIDTHS) {
            val at = RAYS[i]
            val d = RAYS[i + 1] * w
            ray.rewind()
            ray.moveTo(s.top + (at - d) * s.topHalf, TOP); ray.lineTo(s.top + (at + d) * s.topHalf, TOP)
            ray.lineTo(s.foot + (at + d) * s.footHalf, s.fallen); ray.lineTo(s.foot + (at - d) * s.footHalf, s.fallen)
            ray.close()
            drawPath(ray, ink)
        }
    }
    edges(s, area, upper = true)
    drawIntoCanvas { it.restore() }
}

/** The light's soft edges, as what is kept of everything drawn since the layer began: inside its two sides, above its foot, and ([upper]) under its upper edge. */
private fun DrawScope.edges(s: Shaft, area: Rect, upper: Boolean) {
    side(s.top - s.topHalf, s.foot - s.footHalf, s.fallen, 1f, s.soft, area)
    side(s.top + s.topHalf, s.foot + s.footHalf, s.fallen, -1f, s.soft, area)
    if (upper) keep(0f, TOP, 0f, 1f, -8f, 6f, area)
    keep(0f, s.fallen, 0f, -1f, -s.soft, 0.3f * s.soft, area)
}

/** One side of the light, a line from ([x1], the upper edge) to ([x2], [y2]): what lies on its inner side is kept. [sign]: 1 for the left side, −1 for the right. */
private fun DrawScope.side(x1: Float, x2: Float, y2: Float, sign: Float, soft: Float, area: Rect) {
    val dx = x2 - x1
    val dy = y2 - TOP
    val long = hypot(dx, dy).takeIf { it > 0f } ?: 1f
    keep(x1, TOP, sign * dy / long, -sign * dx / long, -soft, 0.3f * soft, area)
}

/** Keeps what lies beyond the line through ([px], [py]) in the direction ([nx], [ny]): nothing of it at [from] along that direction, all of it at [to], and softly between. */
private fun DrawScope.keep(px: Float, py: Float, nx: Float, ny: Float, from: Float, to: Float, area: Rect) {
    drawRect(
        Brush.linearGradient(
            0f to Color.Black.copy(alpha = 0f), 0.25f to Color.Black.copy(alpha = 0.16f), 0.5f to Color.Black.copy(alpha = 0.5f), 0.75f to Color.Black.copy(alpha = 0.84f), 1f to Color.Black,
            start = Offset(px + nx * from, py + ny * from), end = Offset(px + nx * to, py + ny * to),
        ),
        area.topLeft, area.size, blendMode = BlendMode.DstIn,
    )
}
