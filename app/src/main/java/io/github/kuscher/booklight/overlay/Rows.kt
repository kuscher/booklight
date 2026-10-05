package io.github.kuscher.booklight.overlay

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.first
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Behind
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.core.Icon as RowIcon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Results: rows that rise in, move to their new place and fade out, under one gliding selection

/** One row on screen. It outlives its result for a moment while it fades out. */
private class Slot(val uid: Int, var result: Result) {
    var top: Dp = 0.dp
    var index = 0
    var leaving = false
    var delay = 0
    /** It stands whole from its first frame and does not rise: row one of first run's show, which comes into the seat the highlight already has. */
    var whole = false
}

/**
 * Keeps rows alive across lists: the same id keeps its row (and moves), new ids rise in, gone ones
 * fade. An opened row's other actions are not rows of their own here: they are drawn by their row,
 * so that they travel with it and are uncovered and covered under it.
 */
private class RowSlots {
    var all: List<Slot> = emptyList()
    private var last: List<Result>? = null
    private var next = 0
    var removed by mutableIntStateOf(0)

    /**
     * [held]: the highlight stands in row one's seat already (first run's show, whose pill is the welcome's handle): a list
     * that comes from nothing has that row whole at once. [late]: a list that comes from nothing waits this long before
     * its first row rises (first run's choices, which come as the question's words fade: motion.md T9).
     */
    fun sync(results: List<Result>, motion: Motion, held: Boolean = false, late: Int = 0): List<Slot> {
        if (results === last) return all
        last = results
        // A flight's row is one row to the list whatever its number is just now ("LH45", then "LH455"): its words change in
        // the frame of the key, it does not leave and arrive again in its own seat. The ordinary row of a guess is that same
        // row too: when the user goes to it, it grows where it stands into the tall one, once, the pill's lower edge with it.
        fun seat(r: Result) = if (r.provider == io.github.kuscher.booklight.providers.FlightsProvider.ID) "flight:" else r.id
        val live = all.filter { !it.leaving }.associateBy { seat(it.result) }
        val fromNothing = live.isEmpty()
        val tops = Metrics.tops(results)
        var fresh = 0
        val now = results.mapIndexedNotNull { i, r ->
            if (r.kind == Kind.ACTION) return@mapIndexedNotNull null
            val s = live[seat(r)] ?: Slot(next++, r).also { it.delay = motion.stagger(if (fromNothing) i else fresh++) + (if (fromNothing) late else 0); it.whole = held && fromNothing && i == 0 }
            s.result = r; s.index = i; s.top = tops[i]
            s
        }
        val kept = now.toHashSet()
        val going = all.filter { it.leaving || it !in kept }.onEach { it.leaving = true }
        all = now + going
        return all
    }

    fun remove(slot: Slot) { all = all - slot; removed++ }
}

@Composable
fun ResultsBody(model: OverlayModel, icons: AppIcons, onRun: (Result, Action) -> Unit) {
    val motion = LocalMotion.current
    val slots = remember { RowSlots() }
    slots.removed   // read, so a row that has finished fading out is dropped from the composition
    val rows = slots.sync(model.results, motion, held = model.playing == FirstRun.Playing.SHOW, late = if (model.choicesUp) motion.held(Motion.ASK_LEAVES_MS).toInt() else 0)
    val tops = Metrics.tops(model.results)
    val picked = model.results.getOrNull(model.selected)

    // A flight's row that counts minutes follows the clock while it is on screen: at each whole minute its headline's
    // number changes and its plane takes its step. One wake a minute, and none while no such row is in the list.
    val counting = model.results.any { (it.body as? Body.Flight)?.counts == true }
    LaunchedEffect(counting) { while (counting) { delay(60_000 - System.currentTimeMillis() % 60_000 + 50); model.minute() } }

    // A row's other actions, opened under it: one value uncovers them (0 to 1), from the row's lower edge down, and
    // covers them again. They are laid out once and never move. The rows that were opened are kept while they close.
    val block = remember { Animatable(0f) }
    var held by remember { mutableStateOf<Pair<String, List<Result>>?>(null) }
    /** The text the list was opened under. */
    var heldText by remember { mutableStateOf("") }
    val open = model.opened
    if (open != null) {
        model.results.filter { it.kind == Kind.ACTION }.let { if (held?.first != open || held?.second != it) held = open to it }
        if (heldText != model.query) heldText = model.query
    }
    // Closed by typing: typing is never held up. The list fades where it stands, quickly, and the rows for the new text
    // come at once. Closed by Left, the arrow or Enter: the edge covers it again, and what comes back waits for the edge.
    // (And when its row has left the list altogether, by "Don't suggest": nothing is left for an edge to travel back to.)
    val typedAway = open == null && held != null && (model.query != heldText || rows.none { !it.leaving && it.result.id == held?.first })
    LaunchedEffect(open) {
        // The other rows are fading in these first frames. Opened again while it was still closing, it turns round at once.
        if (open != null) { if (block.value == 0f) delay(motion.hold(30)); block.animateTo(1f, motion.place()) }
        else { block.animateTo(0f, if (typedAway) motion.fade(80) else motion.place()); held = null }
    }

    // The list that is closing: where its row stands in the list that is coming back, and how tall it was.
    val closing = held?.takeIf { open == null }?.let { h -> rows.firstOrNull { !it.leaving && it.result.id == h.first }?.let { Closing(it.top, Metrics.listHeight(h.second), Metrics.rowHeight(it.result)) } }

    // As tall as its rows, the ones that are leaving included: the body fades this out when the list empties, and a fade
    // cuts what it fades to its own bounds. At no height the rows would be gone in one frame instead of fading.
    val tall = maxOf(Metrics.listHeight(model.results), rows.maxOfOrNull { it.top + Metrics.rowHeight(it.result) } ?: 0.dp)
    Box(Modifier.padding(horizontal = Metrics.pad).padding(top = Metrics.pad).fillMaxWidth().height(tall)) {
        val danger = picked?.kind == Kind.ACTION && picked.actions.firstOrNull()?.danger == true
        // A grid has its own highlight, the square on its cells: one highlight per level.
        Pill(tops.getOrElse(model.selected) { 0.dp }, picked?.let(Metrics::rowHeight) ?: Metrics.row, visible = picked != null && picked.body !is Body.Grid, danger = danger, row = picked?.id, gone = model.gliding)
        for (slot in rows) key(slot.uid) {
            val selected = !slot.leaving && slot.index == model.selected
            val r = slot.result
            val mine = held?.takeIf { it.first == r.id }
            // Rows that come back when a list closes: nothing arrives under the list. A row below the one that was opened
            // comes as the closing edge passes it, so the list is taken off them from the last row up; a row above it waits
            // until the list has closed, as its row is on its way back down through them.
            val under = closing != null && mine == null && slot.top > closing.top
            SlotRow(slot.top, slot.leaving, if (under) 0 else slot.delay, whole = slot.whole, onGone = { slots.remove(slot) }, wait = {
                when {
                    typedAway || open != null || held == null || mine != null -> false
                    under -> closing!!.top + closing.row + closing.height * block.value > slot.top + Metrics.row / 2
                    else -> block.value > 0.04f
                }
            }) {
                ResultRow(
                    r, icons, selected,
                    armed = if (selected) model.armed else r.armed, confirming = selected && model.confirming, cell = if (selected) model.cell else -1,
                    stops = model.stops(r), opened = model.list.takeIf { open == r.id },
                    // (A pointer passing over a row is not the user going to it: nothing is looked up for that.)
                    onHover = { if (!slot.leaving) model.select(slot.index, passing = true) }, calm = model.atRest,
                    onClick = { if (!slot.leaving) { model.select(slot.index); if (open != r.id) model.armAt(r.armed); model.enter(onRun) } },
                    onArm = { if (selected) model.armAt(it) },
                    // (An action that is off takes no click: Enter would run the armed one in its place.)
                    onAction = { if (selected && r.actions.getOrNull(it)?.off != true) { model.armAt(it); model.enter(onRun) } },
                    // A screen reader's action on a row: the row is gone to first (it may not be the selected one, and in the
                    // usual rows none is). An action behind the arrow is run from the row's list, so every rule of Enter holds.
                    onCustom = { i ->
                        val a = r.actions.getOrNull(i)
                        if (!slot.leaving && a != null && !a.off) {
                            model.select(slot.index)
                            if (a.more) { if (model.opened != r.id || model.list != a.behind) model.open(a.behind); if (model.selectId("act:${r.id}:${a.id}")) model.enter(onRun) }
                            else { model.armAt(i); model.enter(onRun) }
                        }
                    },
                    onCell = { if (selected) model.cellAt(it) else if (!slot.leaving) model.select(slot.index) },
                    onPick = { if (!slot.leaving) { model.select(slot.index); model.cellAt(it); model.armAt(0); model.enter(onRun) } },
                    onToggle = { if (!slot.leaving) { model.select(slot.index); if (open == r.id) model.close() else model.open(Behind.ARROW) } },
                    onGrow = { model.grow(r.id) },
                    pressed = selected && model.pressed,
                )
                if (mine != null) ActionBlock(
                    mine.second, under = Metrics.rowHeight(r), shown = { block.value.coerceIn(0f, 1f) }, fading = typedAway,
                    // Which of them the pill is on: they follow their row in the list.
                    selected = if (open == r.id) model.selected - slot.index - 1 else -1,
                    onHover = { i -> if (open == r.id) model.select(slot.index + 1 + i) },
                    onClick = { i -> if (open == r.id) { model.select(slot.index + 1 + i); model.enter(onRun) } },
                )
            }
        }
    }
}

private class Closing(val top: Dp, val height: Dp, /** The height of the row the list was opened under. */ val row: Dp)

/** Places a row, moves it when its slot changes, and plays its arrival and departure. */
@Composable
private fun SlotRow(top: Dp, leaving: Boolean, delayMs: Int, onGone: () -> Unit, wait: () -> Boolean, /** It does not arrive: it is there, whole, from its first frame. */ whole: Boolean = false, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    val y by animateDpAsState(top, motion.place(), label = "row")
    val here = remember { Animatable(if (motion.on && !whole) 0f else 1f) }
    val gone by rememberUpdatedState(onGone)
    LaunchedEffect(leaving) {
        if (leaving) { here.animateTo(0f, motion.fade(80)); gone() }
        else {
            if (here.value < 1f && wait()) snapshotFlow { wait() }.first { !it }
            if (delayMs > 0) delay(delayMs.toLong())
            here.animateTo(1f, motion.place())
        }
    }
    val rise = with(LocalDensity.current) { 12.dp.toPx() }
    // A row that leaves while it is still rising keeps the offset it had: it fades where it stands, it does not jump up.
    val left = remember { floatArrayOf(-1f) }
    if (!leaving) left[0] = -1f else if (left[0] < 0f) left[0] = (1f - here.value).coerceIn(0f, 1f) * rise
    Box(
        Modifier.zIndex(if (leaving) 0f else 1f)
            .offset { IntOffset(0, y.roundToPx()) }
            .graphicsLayer {
                alpha = here.value.coerceIn(0f, 1f)
                // Arriving rows rise into place; leaving ones just fade where they are.
                translationY = if (leaving) left[0].coerceAtLeast(0f) else (1f - here.value) * rise
            },
    ) { content() }
}

/**
 * One edge of a highlight, in dp. It goes to its place on a spring; given another place or another spring on its way,
 * it goes on from where it is with the speed it has. Nothing is cancelled for that, so nothing starts again from a standstill.
 */
private class Edge(at: Float) {
    var at by mutableFloatStateOf(at)
        private set
    /** Where it is going. */
    var to = at
        private set
    /** How fast it is, in dp a second. */
    private var speed = 0f
    // The spring it rides, and where, how fast and when it set off on it. The spring is asked for the time since then:
    // it counts in whole milliseconds, so stepping it from frame to frame would lose a third of one in every frame.
    private var spring: VectorizedFiniteAnimationSpec<AnimationVector1D>? = null
    private var from = AnimationVector1D(at)
    private var goal = AnimationVector1D(at)
    private var push = AnimationVector1D(0f)
    private var since = 0L
    private var lasts = 0L
    val running get() = spring != null

    /** [clock] is the last frame's time: the next frame is one frame on. 0 when no frame is running: the next frame is then its first, and shows it where it stands. */
    fun go(to: Float, spec: FiniteAnimationSpec<Float>, clock: Long, start: Float = at) = ride(to, spec.vectorize(Float.VectorConverter), clock, start)

    /** Put elsewhere: it goes on from there to the same place, on the spring it was riding (on [spec] if it stood still). */
    fun shift(start: Float, spec: FiniteAnimationSpec<Float>, clock: Long) = ride(to, spring ?: spec.vectorize(Float.VectorConverter), clock, start)

    private fun ride(to: Float, on: VectorizedFiniteAnimationSpec<AnimationVector1D>, clock: Long, start: Float) {
        this.to = to; at = start
        from = AnimationVector1D(start); goal = AnimationVector1D(to); push = AnimationVector1D(speed)
        spring = on; since = clock; lasts = on.getDurationNanos(from, goal, push)
    }

    fun cut(to: Float) { at = to; this.to = to; speed = 0f; spring = null }

    fun frame(now: Long, scale: Float) {
        val s = spring ?: return
        if (since == 0L) since = now
        val t = ((now - since) / scale.toDouble()).toLong()     // (the system's animation scale, as every animation of Compose's takes it)
        if (t < lasts) { at = s.getValueFromNanos(t, from, goal, push).value; speed = s.getVelocityFromNanos(t, from, goal, push).value }
        if (t >= lasts || (abs(to - at) <= 0.01f && abs(speed) <= 0.5f)) cut(to)
    }
}

/**
 * A highlight along one line, from one edge to the other: the list's pill from its top to its bottom, the grid's
 * square once across and once down. It travels like rubber (docs/design/rubber-highlight.md): the edge that leads
 * sets off at once; the old edge holds on for a few frames, so the highlight lies over both rows, then lets go and
 * gathers. Only from rest: one that is already moving (a held key, the pointer) does not hold, and every edge keeps
 * the speed it has. However far it goes it is drawn at most [Motion.STRETCH_MOST] longer than its row.
 *
 * Its edges are not animations of their own: [frame] moves all of them, once in every frame of the screen, and [go]
 * only says where to. So a new place cancels nothing, and the hold is a count of frames.
 */
class Band(first: Dp, size: Dp) {
    private val start = Edge(first.value)
    private val end = Edge((first + size).value)
    /** What the stretch is measured against: the longer of the row it left and the one it is going to. It changes on `place`, as a row's own height does. */
    private val body = Edge(size.value)
    /** Towards [end]: down, or to the right. The limit draws the old edge nearer; the edge that leads is where it is. */
    private var forward = true
    /** The place and the length it was last sent to. */
    private var asked = first.value
    private var length = size.value
    // The old edge holding on: which, how many frames more, and where it goes on what when it lets go.
    private var held: Edge? = null
    private var wait = -1
    private var heldTo = 0f
    private var heldOn: FiniteAnimationSpec<Float>? = null
    /** The last frame's time, and whether frames are running: what sets off between two frames is one frame on in the next. */
    var clock = 0L
        private set
    private var awake = false
    private var began = 0L
    /** It was on its way in the last frame (the debug trace). */
    var stepped = false
        private set

    /** Neither edge is moving and none is holding on: only then does the next move hold. */
    val resting get() = !start.running && !end.running && wait < 0
    val moving get() = !resting || body.running
    /** The milliseconds since it left its place. */
    val ms get() = ((clock - began) / 1_000_000.0).roundToInt()

    /** How far the limit draws the old edge from where it is: nothing up to [Motion.STRETCH_KNEE] of stretch. */
    private val pull: Float get() {
        val extra = end.at - start.at - body.at
        return if (extra > Motion.STRETCH_KNEE.value) extra - Motion.stretch(extra.dp).value else 0f
    }
    /** It is at [first], or on its way there. */
    fun goesTo(first: Dp) = asked == first.value

    /** Where its two edges are drawn. */
    val first get() = (if (forward) start.at + pull else start.at).dp
    val last get() = (if (forward) end.at else end.at - pull).dp

    /**
     * It goes to [first], [size] long. [glued]: it is its own row or cell that moved, and it goes with it as one piece,
     * on `place`. [calm]: nothing of the highlight was moving; a square, which has two bands, says so for both.
     */
    fun go(first: Dp, size: Dp, motion: Motion, refresh: Float, glued: Boolean = false, calm: Boolean = resting) {
        val top = first.value
        val length = size.value
        if (top == asked && length == this.length) return
        if (!motion.on) { cut(first, size); return }
        val now = if (awake) clock else 0L
        // Every edge goes on from where it is drawn, with the speed it has. If the limit was drawing one nearer, the
        // limit starts again from this length, so that nothing jumps when the direction turns round.
        val s = this.first.value
        val e = last.value
        val pulled = pull > 0f
        val holding = wait >= 0
        held = null; wait = -1
        if (calm) began = now
        val most = maxOf(this.length, length)
        if (calm) body.cut(maxOf(most, e - s))
        if (!calm || body.at != most) body.go(most, motion.place(), now, if (pulled) e - s - Motion.STRETCH_KNEE.value else body.at)
        asked = top; this.length = length
        // Only its length changed (an answer needing more lines, Enter on a model's row): no rubber. The lower edge goes
        // with the row's own growth, on the same spring, and an upper edge that is still on its way is left to arrive.
        if (top == start.to && !holding) {
            if (pulled) start.shift(s, motion.place(), now)
            end.go(top + length, motion.place(), now, e)
            return
        }
        if (glued) { start.go(top, motion.place(), now, s); end.go(top + length, motion.place(), now, e); return }
        forward = top + length / 2 > (start.to + end.to) / 2
        val lead = if (forward) end else start
        val old = if (forward) start else end
        val leadTo = if (forward) top + length else top
        val oldTo = if (forward) top else top + length
        val far = abs(leadTo - (if (forward) e else s)) > Motion.PILL_NEAR.value
        lead.go(leadTo, motion.pillLead(), now, if (forward) e else s)
        // (An edge that had not let go yet when the direction turned stays where it is: it is the one that leads now.)
        val frames = if (calm) motion.pillHold(far, refresh) else 0
        if (frames == 0) { old.go(oldTo, motion.pillTrail(far), now, if (forward) s else e); return }
        // It lets go in the frame that is `frames` after the one the other edge sets off in.
        held = old; heldTo = oldTo; heldOn = motion.pillTrail(far); wait = if (awake) frames - 1 else frames
    }

    /** One frame of the screen; false once everything stands still. To be called while it is [moving], in every frame. */
    fun frame(now: Long, scale: Float): Boolean {
        stepped = !resting
        if (began == 0L) began = now
        if (wait > 0) wait-- else if (wait == 0) { wait = -1; held?.go(heldTo, heldOn!!, now); held = null }
        start.frame(now, scale); end.frame(now, scale); body.frame(now, scale)
        clock = now; awake = moving
        return awake
    }

    /** It is there, without a way. */
    fun cut(first: Dp, size: Dp) {
        start.cut(first.value); end.cut((first + size).value); body.cut(size.value)
        asked = first.value; length = size.value; held = null; wait = -1; awake = false
    }

    /** It stays where it is (it is fading away): wherever it is sent next, it sets off from rest. */
    fun stop() {
        val s = first.value
        val e = last.value
        start.cut(s); end.cut(e); body.cut(e - s)
        asked = Float.NaN; length = e - s; held = null; wait = -1; awake = false
    }
}

/**
 * Debug builds, `./bl debug pill N`: the next N frames in which the list's pill is on its way, a line each in the log
 * (`./bl logs`): the milliseconds since it left its place, its two edges as they are drawn, and the row it is going
 * to, in pixels from the list's top.
 */
object PillTrace {
    var left = 0
        private set
    private var last = 0L
    fun start(frames: Int) { left = frames; last = System.nanoTime() }     // (a frame's time is on that clock: what was before this is not written)
    fun frame(band: Band, upper: Int, lower: Int, top: Int, bottom: Int) {
        if (!band.stepped || band.clock <= last) return
        last = band.clock; left--
        Log.i(BooklightApp.TAG, "pill t=${band.ms} up=$upper lo=$lower to=$top..$bottom")
    }
}

/**
 * The selection: one pill for the whole list. It travels like rubber ([Band]): its front edge goes to the new row at
 * once, the old edge holds on for a moment and then follows, so the pill stretches over both rows and gathers itself
 * on the new one. On an action that removes something it takes the error container's colour, as the pane does one
 * level down. [row] is the selected row, so that the pill knows when it is its own row that moved.
 */
@Composable
fun Pill(top: Dp, height: Dp, visible: Boolean, danger: Boolean = false, row: String? = null, gone: Boolean = false) {
    val motion = LocalMotion.current
    val band = remember { Band(top, height) }
    val shown by animateFloatAsState(if (visible) 1f else 0f, motion.fade(120), label = "pill")
    val refresh = LocalView.current.display?.refreshRate ?: 60f
    val was = remember { arrayOfNulls<String>(1) }
    // This effect only says where the pill goes and counts the frames. The way itself is the band's: a new row ends
    // the effect and starts it again, and the pill goes on from where it is with the speed it has.
    LaunchedEffect(top, height, visible, row) {
        val own = row != null && row == was[0]
        was[0] = row
        // Going: it fades where it stands. Coming from nothing: it is where it belongs, it does not fly in from where it
        // last was (or from the top of a page that had no selection yet).
        if (!visible) { band.stop(); return@LaunchedEffect }
        if (shown == 0f) { band.cut(top, height); return@LaunchedEffect }
        // Its own row was moved by a list that changed (rows arriving above it): the pill goes with its row, as one piece.
        band.go(top, height, motion, refresh, glued = own)
        val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        while (band.moving) withFrameNanos { band.frame(it, scale) }
    }
    // Drawn from its two edges, each rounded to a pixel by itself, and as tall as what lies between them: an edge that
    // is holding on stands still, and one that settles never steps back a pixel (as the sum of a rounded top and a
    // rounded height did).
    Slab(
        Modifier.fillMaxWidth().layout { measurable, constraints ->
            val upper = band.first.roundToPx()
            val lower = maxOf(band.last.roundToPx(), upper + 12.dp.roundToPx())
            if (PillTrace.left > 0) PillTrace.frame(band, upper, lower, top.roundToPx(), (top + height).roundToPx())
            val box = measurable.measure(constraints.copy(minHeight = lower - upper, maxHeight = lower - upper))
            layout(box.width, box.height) { box.place(0, upper) }
        },
        // [gone]: the one highlight has left this list for another place (first run's landing): it is not here a frame longer.
        { if (gone) 0f else shown }, danger,
    )
}

/** What the pill is made of. The only coloured surface: that is what says "selected". Flat, with the panel's white outline. */
@Composable
private fun Slab(modifier: Modifier, shown: () -> Float, danger: Boolean) {
    val shape = RoundedCornerShape(24.dp)   // the panel's 32 less the 8 it is inset by: concentric
    val dark = LocalDark.current
    val fill by animateColorAsState(selectionFill(MaterialTheme.colorScheme, dark, LocalGlass.current, danger), LocalMotion.current.fade(120), label = "fill")
    Box(
        modifier.graphicsLayer { alpha = shown() }
            .clip(shape)
            .background(fill)
            .border(with(LocalDensity.current) { 1f.toDp() }, Color.White.copy(alpha = if (dark) 0.30f else 0.55f), shape),
    )
}

/**
 * An opened row's other actions, under it: each a line of 40 dp with its glyph in the icon column
 * and its name on the title's edge, in the strip's own type. They are the strip's slots, one per
 * line, on the panel's two existing lines; the list's pill is their highlight, and the one it is on
 * shows the Enter mark where the row's arrow stands. Laid out once; [shown] uncovers them from the
 * top down and covers them again.
 */
@Composable
private fun ActionBlock(rows: List<Result>, /** How tall the row is that they are opened under. */ under: Dp, shown: () -> Float, /** Going because the text changed: all of it fades, no edge travels. */ fading: Boolean, selected: Int, onHover: (Int) -> Unit, onClick: (Int) -> Unit) {
    val tops = Metrics.tops(rows)
    val soft = with(LocalDensity.current) { 20.dp.toPx() }
    Box(
        Modifier.offset(y = under).fillMaxWidth().height(Metrics.listHeight(rows))
            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen; alpha = if (fading) shown() else 1f }
            .drawWithContent {
                val edge = if (fading) size.height else size.height * shown()
                if (edge <= 0f) return@drawWithContent
                clipRect(bottom = edge) { this@drawWithContent.drawContent() }
                // The uncovering edge is soft: a row appears over its first lines, not at a cut.
                if (edge < size.height) drawRect(
                    Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = edge - soft, endY = edge),
                    topLeft = Offset(0f, (edge - soft).coerceAtLeast(0f)), size = Size(size.width, soft), blendMode = BlendMode.DstIn,
                )
            },
    ) {
        rows.forEachIndexed { i, r -> ActionRow(r, i == selected, Modifier.offset(y = tops[i]), onHover = { onHover(i) }, onClick = { onClick(i) }) }
    }
}

@Composable
private fun ActionRow(r: Result, selected: Boolean, modifier: Modifier, onHover: () -> Unit, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val a = r.actions.firstOrNull()
    val bad = a?.danger == true
    val ink by animateColorAsState(
        when { bad && selected -> scheme.onErrorContainer; bad -> scheme.error; selected -> scheme.onSurface; else -> scheme.onSurface.copy(alpha = SECOND) },
        motion.fade(120), label = "ink",
    )
    val mark by animateFloatAsState(if (selected) 1f else 0f, motion.fade(120), label = "mark")
    val hover by rememberUpdatedState(onHover)
    Row(
        modifier.fillMaxWidth().height(Metrics.action).clip(RoundedCornerShape(20.dp))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) if (awaitPointerEvent().type == PointerEventType.Move) hover() } }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = selected; role = Role.Button; contentDescription = r.title }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The glyph sits in the icon column, the name on the title's edge.
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { Icon(Symbols.of((r.icon as? RowIcon.Symbol)?.name ?: "app"), null, Modifier.size(18.dp), tint = ink) }
        // The names are what is chosen from: full ink, selected or not (over a white window second ink is the faintest text in the panel).
        Text(r.title, color = if (bad) ink else scheme.onSurface, style = LABEL, maxLines = 1, modifier = Modifier.padding(start = 16.dp).weight(1f))
        // Where the row's arrow stands: the Enter mark of the action the pill is on.
        Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) { Icon(Symbols.enter, null, Modifier.size(14.dp).graphicsLayer { alpha = mark }, tint = ink) }
    }
}

@Composable
private fun RowFrame(height: Dp, selected: Boolean, label: String, actions: List<Action>, bare: Boolean, onHover: () -> Unit, onClick: () -> Unit, onAction: (Int) -> Unit, calm: Boolean = false, /** The row is a switch, and whether it is on: said so to a screen reader. */ on: Boolean? = null, content: @Composable RowScope.(Dp) -> Unit) {
    val hover by rememberUpdatedState(onHover)
    val still by rememberUpdatedState(calm)
    val slack = with(LocalDensity.current) { 4.dp.toPx() }
    val act by rememberUpdatedState(onAction)
    // A row that changes its height (its answer is asked for, or needs more lines) grows and shrinks on the spring the
    // pill's lower edge and the rows under it move on: what it holds is uncovered by that edge, never drawn outside the pill.
    val tall by animateDpAsState(height, LocalMotion.current.place(), label = "tall")
    Row(
        Modifier.fillMaxWidth().height(tall.coerceAtLeast(1.dp)).clip(RoundedCornerShape(24.dp))
            // Hover selects only when the pointer moves, so a list growing under a resting pointer doesn't steal the selection.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var from: Offset? = null
                    while (true) {
                        val e = awaitPointerEvent()
                        val at = e.changes.firstOrNull()?.position
                        if (e.type == PointerEventType.Exit) from = null
                        if (e.type != PointerEventType.Move || at == null) continue
                        if (!still) { from = null; hover(); continue }
                        val start = from ?: at.also { from = it }
                        if ((at - start).getDistance() > slack) { from = null; hover() }
                    }
                }
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            // One item for a screen reader, whatever it can do offered as its actions.
            .semantics(mergeDescendants = true) {
                this.selected = selected; contentDescription = label
                // (A row that holds a switch is one to a reader: it says "on" or "off" itself, also when it is flipped.)
                if (on != null) { role = Role.Switch; toggleableState = ToggleableState(on) } else role = Role.Button
                // (Window is a stop, not something to run: its lines are offered by their own names, "Left half".)
                if (actions.size > 1) customActions = actions.withIndex().drop(1).filter { it.value.effect !is Effect.OpenList }.map { (i, a) -> CustomAccessibilityAction(a.label) { act(i); true } }
            }
            .padding(horizontal = if (bare) 0.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content(tall) }
}

@Composable
fun ResultRow(
    r: Result, icons: AppIcons, selected: Boolean, armed: Int, confirming: Boolean, cell: Int,
    /** What the arming can rest on (indices into the row's actions; the number of actions is its arrow), and which of its two lists is open under it, if one is. */
    stops: List<Int> = r.actions.indices.toList(), opened: Behind? = null,
    onHover: () -> Unit, onClick: () -> Unit, onArm: (Int) -> Unit, onAction: (Int) -> Unit, onCell: (Int) -> Unit, onPick: (Int) -> Unit, onToggle: () -> Unit = {},
    /** Nothing is selected yet (the usual rows at rest): the pointer brings the highlight only once it has really moved on the row, not by a tremble. */
    calm: Boolean = false,
    /** One of the row's actions, asked for by a screen reader. */
    onCustom: (Int) -> Unit = onAction,
    /** An answer in this row needs more than its two lines. */
    onGrow: () -> Unit = {},
    /** Its armed action shows as pressed (first run's practice). */
    pressed: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    // One ink for the whole panel, `onSurface`. On the selection everything is at full strength: the row that
    // matters most must never be the faintest one (the scheme's own on-container ink can be a mid-tone).
    val on = scheme.onSurface
    val dim by animateColorAsState(scheme.onSurface.copy(alpha = if (selected) 1f else SECOND), motion.fade(120), label = "dim")
    val pop by animateFloatAsState(if (selected) 1.06f else 1f, motion.pop(), label = "icon")
    // (Read whether it is used or not: a row that gains or loses its own label must stay the same row to Compose.)
    val ofKind = kindLabel(r.kind)
    val kind = r.label ?: if (r.provider == "gemini") "Gemini" else ofKind
    val body = r.body
    // (A flight's row is said with what it answers: who and where, then the headline and the badge.)
    // (A switch's row is said by its name: its state is the switch's own to say.)
    val described = if (body is Body.Switch) r.title
        else stringResource(R.string.a11y_selected, if (body is Body.Flight) listOfNotNull(r.title, body.headline.ifEmpty { null }, body.badge).joinToString(", ") else r.title, r.actions.getOrNull(armed)?.label ?: kind)
    // The answer body as it last was: it goes on being drawn while it fades, when the row turns back into its name.
    val lastStream = remember { arrayOfNulls<Body.Stream>(1) }
    if (body is Body.Stream) lastStream[0] = body
    // And the room its strip keeps stays until it has faded, so its lines are not laid out again on their way out.
    var streamed by remember { mutableStateOf(body is Body.Stream) }
    LaunchedEffect(body is Body.Stream) { if (body is Body.Stream) streamed = true else { delay(motion.hold(80)); streamed = false } }
    RowFrame(Metrics.rowHeight(r), selected, described, r.actions, bare = body is Body.Grid, onHover, onClick, onCustom, calm, on = (body as? Body.Switch)?.on) { tall ->
        if (body is Body.Grid) { GridBody(body, cell.coerceAtLeast(0), selected, onCell, onPick); return@RowFrame }
        // A row that grows under its answer keeps its mark and its strip where they were: on the line of the row's first
        // height. A row that is asked where it stands grows from an ordinary row into that: they travel there with its height.
        val line = minOf(tall, Metrics.tall)
        // A flight's row is taller than that line from its first frame: its mark and its strip stand on the same line, over the
        // flight's own. Its ordinary row (a guess nobody has gone to) is that same row in the list: it keeps to that line too,
        // so nothing jumps when the one becomes the other and the row grows or shrinks (`FlightSeat` holds them both).
        val guess = body == null && r.provider == io.github.kuscher.booklight.providers.FlightsProvider.ID
        val pinned = if (body is Body.Stream || body is Body.Flight || guess) Modifier.align(Alignment.Top).padding(top = ((line - 36.dp) / 2).coerceAtLeast(0.dp)) else Modifier
        if (body is Body.Task) TaskBox(body.done, dim)
        else if (body is Body.Code) QrPlate(body.text, Modifier.padding(start = 4.dp))
        // A swatch is a sample, not a mark: it doesn't swell with the selection.
        else if (r.icon is RowIcon.Swatch) RowPicture(r.icon, icons, dim)
        else Box(pinned.graphicsLayer { scaleX = pop; scaleY = pop }) { RowPicture(r.icon, icons, dim) }

        when {
            body is Body.Flight || guess -> FlightSeat(r.title, body as? Body.Flight, line, on, dim)
            body is Body.Slots -> SlotsBody(body, on)
            body is Body.Code -> Column(Modifier.weight(1f).padding(start = 20.dp, end = 12.dp)) {
                Text(r.title, color = on, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 3, overflow = TextOverflow.Ellipsis)
                r.subtitle?.let { Text(it, color = dim, style = SMALL, maxLines = 1, modifier = Modifier.padding(top = 4.dp)) }
            }
            // A row's name and, once it is asked, the question and its answer stand in one place and change in a fade.
            else -> Box(Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(body is Body.Stream, transitionSpec = { (fadeIn(motion.fade(110, 40)) togetherWith fadeOut(motion.fade(70))).using(null) }, contentAlignment = Alignment.TopStart, label = "asked") { stream ->
                    if (stream) lastStream[0]?.let { StreamBody(it, on, onGrow) }
                    else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = if (r.answer != null && body == null && r.icon !is RowIcon.Swatch) 3.dp else 0.dp)) {
                        // A row whose actions copy different forms of one value (a colour) shows the armed form large.
                        val big = if (body is Body.Mono) body.text else if (r.icon is RowIcon.Swatch) (r.actions.getOrNull(armed)?.effect as? Effect.CopyText)?.text ?: r.answer else r.answer
                        if (big != null) {
                            r.subtitle?.let { Text(it, color = dim, style = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            if (body is Body.Mono) MonoText(big, on)
                            // A changed answer rolls up into place, like a counter.
                            else AnimatedContent(big, transitionSpec = { motion.roll() }, label = "answer") { a ->
                                Text(a, color = on, style = TextStyle(fontFamily = Fonts.round, fontSize = if (a.length > 22) 26.sp else 34.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else {
                            // Laid out once at its own width and covered by a fade where the strip takes its room: a name unrolling
                            // beside it never makes the title swap letters for an ellipsis.
                            if (body is Body.Keys) Text(r.title, color = on, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500), lineHeight = 21.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            else Box(Modifier.fillMaxWidth().fadeEnd()) {
                                val style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500))
                                if (body is Body.Task) TaskTitle(r.title, body.done, on)
                                else Text(fitting(r, style), color = on, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                            }
                            r.subtitle?.let { Text(it, color = dim, style = SMALL, maxLines = if (Metrics.rowHeight(r) > Metrics.row) 2 else 1, overflow = TextOverflow.Ellipsis) }
                        }
                        }
                    }
                }
            }
        }

        // A level: the number keeps its place whether the row is selected or not; the track joins it when it is.
        if (body is Body.Level) {
            val track by animateFloatAsState(if (selected) 1f else 0f, motion.fade(110), label = "track")
            // The number belongs to the track: close after it, and a clear step before the actions.
            if (!body.locked && (selected || track > 0f)) LevelTrack(body, on, Modifier.padding(end = 12.dp).graphicsLayer { alpha = track })
            if (!body.locked) LevelNumber(body, if (selected) on else scheme.onSurface.copy(alpha = SECOND), end = !selected)
            Spacer(Modifier.width(if (selected && !body.locked) 16.dp else 0.dp))
        }

        // A switch is the row's own control: it stands at the row's end, selected or not, and the row has no strip.
        if (body is Body.Switch) { PanelSwitch(body); return@RowFrame }

        // What the row can do arrives on the selected row; the others say what kind of thing they are.
        // The icons are the actions that are not kept in a list; the last slot is the arrow, or the one of its lines
        // that a typed verb named. Window is an icon like the others, and a typed place stands in its slot.
        val shown = r.actions.filter { !it.more }
        val more = r.actions.any { it.more && it.behind == Behind.ARROW }
        val tenth = stops.lastOrNull()?.takeIf { more }?.let { r.actions.getOrNull(it) }
        val window = shown.indexOfFirst { it.effect is Effect.OpenList }
        val place = stops.firstOrNull { r.actions.getOrNull(it)?.let { a -> a.more && a.behind == Behind.WINDOW } == true }?.takeIf { window >= 0 }
        val slot = r.actions.getOrNull(armed).let { a ->
            when {
                more && (armed == r.actions.size || (a?.more == true && a.behind == Behind.ARROW)) -> shown.size
                a?.more == true && window >= 0 -> window
                else -> shown.indexOf(a).coerceAtLeast(0)
            }
        }
        fun full(k: Int) = if (k >= shown.size) stops.lastOrNull() ?: 0 else if (k == window && place != null) place else r.actions.indexOf(shown[k])
        // A key combination: the caps keep one place in every row, selected or not. The strip's room is kept free
        // after them, and the kind label and the strip trade places inside it.
        if (body is Body.Keys) { KeyCaps(body.keys, dim); Spacer(Modifier.width(16.dp)) }
        // An answer's strip has a room of its own, as the key caps have: the text beside it is as wide before the answer as
        // after it, whichever action is armed, and is never laid out again. A strip wider than its room hangs over to the left.
        // (A flight's strip takes only the room it needs: line one and the headline beside it are covered by a fade, not laid out again.)
        Box(if (body is Body.Keys) Modifier.width(KEYS_ROOM) else if (body is Body.Stream) Modifier.align(Alignment.Top).padding(top = ((line - 32.dp) / 2).coerceAtLeast(0.dp)).height(32.dp).width(STREAM_ROOM).wrapContentWidth(Alignment.End, unbounded = true) else if (body is Body.Flight || guess) Modifier.align(Alignment.Top).padding(top = ((line - 32.dp) / 2).coerceAtLeast(0.dp)).height(32.dp) else if (streamed) Modifier.width(STREAM_ROOM).wrapContentWidth(Alignment.End, unbounded = true) else Modifier, contentAlignment = Alignment.CenterEnd) {
        val mode = when { selected && r.actions.isNotEmpty() -> 2; opened != null -> 1; else -> 0 }
        // The arrow turns over when the row's list opens: one turn, whichever of the two arrows is showing.
        val turn = animateFloatAsState(if (opened != null) 180f else 0f, motion.pop(), label = "turn")
        // (The strip's own arrow turns only for its own list: while Window's is open it stays as it is.)
        val arrow = animateFloatAsState(if (opened == Behind.ARROW) 180f else 0f, motion.pop(), label = "arrow")
        val less = stringResource(R.string.action_less)
        AnimatedContent(Trail(mode, if (mode == 2) shown else emptyList()), transitionSpec = {
            // The strip slides in when the row is selected. When only what the row can do changed (an answer landed: Ask became
            // Copy, Pin…), the old strip fades and the new one comes in its place. The room changes at once: no size animation
            // of the container's own, which would wipe the strip and lay the title out again every frame.
            (if (targetState.mode == 2 && initialState.mode != 2) (slideInHorizontally(motion.place()) { it / 4 } + fadeIn(motion.fade(110, 60))) togetherWith fadeOut(motion.fade(60))
            // (Strip to strip: the new one waits until the old one has gone, or two panes would stand in one room.)
            else fadeIn(motion.fade(110, if (initialState.mode == 2 && targetState.mode == 2) 60 else 0)) togetherWith fadeOut(motion.fade(60))).using(null)
        }, contentKey = { it.key }, contentAlignment = Alignment.CenterEnd, label = "trail") { trail ->
            val state = trail.mode
            when {
                // (While this is on its way out the row may already have lost its actions: an empty range must not be coerced into.)
                // Window's slot holds the place a typed verb named ("chrome left"), and reads Less while its own list is open.
                // (Only the stop itself gives its slot up: a strip that is on its way out may hold other actions at that place.)
                state == 2 -> ActionStrip(trail.actions.mapIndexed { k, a -> if (k != window || a.effect !is Effect.OpenList) a else place?.let { r.actions.getOrNull(it) } ?: if (opened == Behind.WINDOW) a.copy(label = less) else a },
                    slot.coerceAtMost(trail.actions.size), confirming, confirmLabel = stringResource(R.string.confirm_again),
                    // (A click on the arrow while Window's list is open goes the way Enter does: that list closes and the arrow's opens.)
                    onArm = { onArm(full(it)) }, onRun = { if (more && tenth == null && it == shown.size && opened != Behind.WINDOW) onToggle() else onAction(full(it)) },
                    more = more, tenth = tenth, opened = opened == Behind.ARROW,
                    moreLabel = stringResource(R.string.action_more), lessLabel = less, turn = { arrow.value }, icons = icons, pressed = pressed)
                // One of its lists is open and the pill is on one of its lines: the row keeps only an arrow, turned over, in
                // its place; before it, for Window's list, the word that says which list this is.
                state == 1 -> Row(verticalAlignment = Alignment.CenterVertically) {
                    if (opened == Behind.WINDOW) Text(stringResource(R.string.action_window), color = scheme.onSurface.copy(alpha = SECOND), style = SMALL, maxLines = 1, modifier = Modifier.padding(end = 8.dp))
                    Box(Modifier.size(32.dp).clip(CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle), contentAlignment = Alignment.Center) {
                        Icon(Symbols.of("more"), null, Modifier.size(18.dp).graphicsLayer { rotationZ = turn.value }, tint = scheme.onSurface.copy(alpha = SECOND))
                    }
                }
                body is Body.Level -> Spacer(Modifier.width(0.dp))
                // (A scope's row that names its own key shows that key, as an answer's caps: `?` for the list of everything.)
                r.kind == Kind.SCOPE -> Keycap(r.label ?: "tab", strong = r.label != null)
                else -> Text(kind, color = scheme.onSurface.copy(alpha = if (r.provider == "help") 1f else SECOND), style = SMALL, maxLines = 1)
            }
        }
        }
    }
}

/**
 * [r]'s title, or its shorter form ([Result.brief]) where the whole one is wider than the least room a
 * title has beside the row's actions: "Search for “dune”" for an app with a long name, whose icon the row
 * has anyway. Measured against that room and not against the row as it stands, so the words do not change
 * while the strip unrolls beside them.
 */
@Composable
private fun fitting(r: Result, style: TextStyle): String {
    val brief = r.brief ?: return r.title
    val measurer = rememberTextMeasurer()
    val room = with(LocalDensity.current) { Metrics.title.roundToPx() }
    return remember(r.title, brief, style, room) { if (measurer.measure(r.title, style, maxLines = 1, softWrap = false).size.width <= room) r.title else brief }
}

/** What stands at a row's right end: its kind (0), the arrow of its open list (1), or the strip of what it can do (2), which is another strip when those are other actions. */
private class Trail(val mode: Int, val actions: List<Action>) {
    val key: Any = mode to actions.map { if (it.off) it.id + " off" else it.id }     // (an action going off is another strip: it is drawn anew, dimmed)
    // Equal by its key: the row is composed again in every frame while its colours move, and a new target each time
    // would make the transition drop the strip it is fading out.
    override fun equals(other: Any?) = other is Trail && other.key == key
    override fun hashCode() = key.hashCode()
}

/** The room a row of key caps keeps free at its right for its strip ("All shortcuts", Copy), so the caps never move. */
private val KEYS_ROOM = 200.dp

/** The room an answer's row keeps for its strip: the widest it gets (Copy, Pin, Gemini, with the longest name unrolled). */
private val STREAM_ROOM = 216.dp

/** A task's box, in the icon column. Ticked, it fills with ink and the check draws itself in the ground's colour. */
@Composable
private fun TaskBox(done: Boolean, ink: Color) {
    val motion = LocalMotion.current
    val fill by animateFloatAsState(if (done) 1f else 0f, motion.fade(120), label = "box")
    val ground = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(20.dp).drawWithContent {
            val r = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx())
            val w = 1.5.dp.toPx()
            if (fill > 0f) drawRoundRect(ink.copy(alpha = ink.alpha * fill), cornerRadius = r)
            drawRoundRect(ink, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), androidx.compose.ui.geometry.CornerRadius(7.dp.toPx() - w / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(w))
            drawContent()
        }, contentAlignment = Alignment.Center) { DrawnCheck(done, ground, Modifier.size(16.dp)) }
    }
}

/** A task's words. Ticked, a line strikes them from their start, its leading end first, and the ink steps back. The line is drawn over the text as it was laid out; the text is not laid out again. */
@Composable
private fun TaskTitle(title: String, done: Boolean, on: Color) {
    val motion = LocalMotion.current
    val struck by animateFloatAsState(if (done) 1f else 0f, motion.lead(), label = "strike")
    val ink by animateColorAsState(if (done) on.copy(alpha = SECOND) else on, motion.fade(120), label = "task")
    var width by remember { mutableStateOf(0f) }
    Text(
        title, color = ink, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
        onTextLayout = { width = it.getLineRight(0) },
        modifier = Modifier.drawWithContent {
            drawContent()
            val s = struck.coerceIn(0f, 1f)
            if (s > 0f) drawLine(ink, Offset(0f, size.height * 0.56f), Offset(minOf(width, size.width) * s, size.height * 0.56f), 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        },
    )
}

@Composable
private fun RowPicture(icon: RowIcon, icons: AppIcons, tinted: Color) {
    val size = 36.dp
    when (icon) {
        is RowIcon.App -> {
            val px = with(LocalDensity.current) { 48.dp.roundToPx() }
            val bitmap by produceState(icons.cached(icon), icon) { if (value == null) value = icons.load(icon, px) }
            Box(Modifier.size(size)) { bitmap?.let { Image(it, null, Modifier.fillMaxSize()) } }
        }
        is RowIcon.Symbol -> Box(Modifier.size(size).clip(CircleShape).background(tinted.copy(alpha = if (LocalDark.current) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            Icon(Symbols.of(icon.name), null, Modifier.size(20.dp), tint = tinted)
        }
        is RowIcon.Swatch -> Box(Modifier.size(size), contentAlignment = Alignment.Center) { Swatch(icon.argb, tinted) }
        is RowIcon.Glyph -> Box(Modifier.size(size).clip(CircleShape).background(tinted.copy(alpha = if (LocalDark.current) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            Text(icon.text, color = tinted, style = TextStyle(fontFamily = Fonts.round, fontSize = if (icon.text.length > 2) 12.sp else 17.sp, fontWeight = FontWeight(600)), maxLines = 1)
        }
    }
}

@Composable
fun kindLabel(k: Kind): String = when (k) {
    Kind.APP -> stringResource(R.string.kind_app)
    Kind.SETTING -> stringResource(R.string.kind_setting)
    Kind.COMMAND -> stringResource(R.string.kind_command)
    Kind.WEB -> stringResource(R.string.kind_web)
    Kind.ANSWER -> stringResource(R.string.kind_answer)
    Kind.SUGGESTION -> stringResource(R.string.kind_suggestion)
    Kind.SCOPE, Kind.CONTROL, Kind.OTHER, Kind.ACTION -> ""
}
