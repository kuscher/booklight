package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import kotlin.math.abs
import kotlin.math.roundToInt

/** Where each slot of a strip sits, as laid out last: what the highlight is drawn from and where a pointer lands. */
private class Slots(n: Int) {
    val x = IntArray(n)
    val w = IntArray(n)
    /** Where the highlight would begin and end if it were wholly on this slot (inside any air the slot keeps around itself). */
    val p0 = IntArray(n)
    val p1 = IntArray(n)
    /** Where the slot's icon is drawn. */
    val icon = IntArray(n)

    fun at(px: Float): Int {
        for (k in x.indices) if (px < x[k] + w[k]) return k
        return x.lastIndex
    }
}

/** How much of slot [k] the highlight is on, when the highlight is at [a]: 1 on it, 0 a whole slot away. */
private fun on(a: Float, k: Int) = (1f - abs(a - k)).coerceIn(0f, 1f)

internal val LABEL = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(500), letterSpacing = 0.1.sp)
private val SLOT = 32.dp
/** Inside the pane, before the icon and after the mark. */
private val EDGE = 9.dp
/** A slot at rest: its 18 dp icon and this on either side make the 32 dp pitch that lets ten of them stand beside a name. */
private val REST = 7.dp
private val GAP = 7.dp
/** The space the armed slot keeps between its pane and a neighbouring icon. */
private val AIR = 4.dp

/**
 * What the selected row can do: its actions as icons, in a row, ten at most. One is armed and
 * Enter runs it; it is unrolled to its name and the Enter mark, on a small pane of the panel's own
 * glass.
 *
 * A row with more than its icons show keeps the rest behind the last slot, an arrow ([more]): armed,
 * it reads More, and Enter opens them as a list under the row ([opened]: the arrow is turned over
 * and reads Less). When a typed verb names one of the others ("chrome top left"), the arrow's slot
 * is that action instead ([tenth]): never an eleventh slot.
 *
 * Each slot shows as much of its name as it is armed, a number of its own on one spring. The pane
 * is drawn around the weighted middle of the slots that are part-shown, so a step to the next slot
 * is one movement, and a jump (a typed verb arming the last slot) goes straight there without
 * unrolling every name on the way.
 */
@Composable
fun ActionStrip(
    actions: List<Action>,
    /** Which slot is armed: an index into [actions], or `actions.size` for the last slot (the arrow, or [tenth]). */
    armed: Int,
    /** The armed action is waiting for its second Enter. */
    confirming: Boolean,
    /** What the armed action reads while it waits: "Press again to delete". */
    confirmLabel: String,
    onArm: (Int) -> Unit,
    onRun: (Int) -> Unit,
    modifier: Modifier = Modifier,
    more: Boolean = false,
    tenth: Action? = null,
    opened: Boolean = false,
    moreLabel: String = "",
    lessLabel: String = "",
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val last = more || tenth != null
    val n = actions.size + if (last) 1 else 0
    val slots = remember(n) { Slots(n) }
    val at = armed.coerceIn(0, (n - 1).coerceAtLeast(0))
    // How far each slot is armed, 0 to 1.
    val amount = remember(n) { List(n) { Animatable(if (it == at) 1f else 0f) } }
    LaunchedEffect(at, n) { amount.forEachIndexed { k, a -> launch { a.animateTo(if (k == at) 1f else 0f, motion.arm()) } } }
    fun on(k: Int) = amount[k].value.coerceIn(0f, 1f)
    // The line that drains while a confirmation waits.
    val left = remember { Animatable(1f) }
    LaunchedEffect(confirming) {
        left.snapTo(1f)
        if (confirming && motion.on) left.animateTo(0f, tween(OverlayModel.CONFIRM_MS.toInt(), easing = LinearEasing))
    }
    val arm by rememberUpdatedState(onArm)
    val run by rememberUpdatedState(onRun)
    fun danger(k: Int) = if (k < actions.size) actions[k].danger else tenth?.danger == true
    // The arrow turns over when the row's list opens.
    val turn by animateFloatAsState(if (opened) 180f else 0f, motion.pop(), label = "turn")

    // The pane: a small sheet of the panel's own veil, lighter than the selection in light theme and
    // darker in dark, so it lifts the name's contrast in both and adds no colour of its own. One white
    // rim, like the selection's: a second ring would make a button of it.
    val pane = scheme.surfaceContainerLowest.copy(alpha = if (dark) 0.36f else 0.62f)
    val rim = Color.White.copy(alpha = if (dark) 0.30f else 0.55f)
    val alarm = scheme.errorContainer.copy(alpha = if (dark) 0.70f else 0.85f)
    val drain = scheme.onErrorContainer

    Layout(
        modifier = modifier
            .clearAndSetSemantics {}   // the row speaks for its actions (custom accessibility actions)
            .drawBehind {
                if (n == 0) return@drawBehind
                // Where the pane is: the middle of the slots it is on, each weighing as much as it is armed.
                var sum = 0f; var x0 = 0f; var x1 = 0f; var red = 0f
                for (k in 0 until n) { val w = on(k); sum += w; x0 += slots.p0[k] * w; x1 += slots.p1[k] * w; if (danger(k)) red += w }
                if (sum <= 0f) return@drawBehind
                x0 /= sum; x1 /= sum
                pane(x0, x1, lerp(pane, alarm, (red / sum).coerceIn(0f, 1f)), rim)
                if (confirming && left.value > 0f) {
                    val inset = 14.dp.toPx()
                    val y = size.height - 5.dp.toPx()
                    val from = x0 + inset + (x1 - x0 - 2 * inset) * (1f - left.value)   // it shortens towards the Enter mark
                    if (x1 - inset > from) drawLine(drain, Offset(from, y), Offset(x1 - inset, y), 2.dp.toPx(), StrokeCap.Round)
                }
            }
            .pointerInput(n) { detectTapGestures { run(slots.at(it.x)) } }
            .pointerInput(n) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        if (e.type == PointerEventType.Move) e.changes.firstOrNull()?.let { arm(slots.at(it.position.x)) }
                    }
                }
            },
        content = {
            for (k in 0 until n) {
                val act = if (k < actions.size) actions[k] else tenth
                val bad = act?.danger == true
                // On the selection the ink is the panel's own, at full strength. What removes something is red at
                // rest and takes its container's ink once the pane is on it.
                val ink by animateColorAsState(if (!bad) scheme.onSurface else if (k == at) scheme.onErrorContainer else scheme.error, motion.fade(120), label = "ink")
                // The slot's icon. In the last slot the arrow and a typed action trade places on one centre.
                val symbol = act?.symbol ?: "more"
                AnimatedContent(symbol, transitionSpec = { fadeIn(motion.fade(80)) togetherWith fadeOut(motion.fade(80)) using SizeTransform(clip = false) { _, _ -> motion.arm() } }, label = "icon") { sym ->
                    if (sym.startsWith("t:")) Text(sym.substring(2), color = ink, style = LABEL.copy(fontSize = 12.sp, fontWeight = FontWeight(600), letterSpacing = 0.2.sp), maxLines = 1, softWrap = false)
                    else Icon(Symbols.of(sym), null, Modifier.size(18.dp).graphicsLayer { if (sym == "more") rotationZ = turn }, tint = ink)
                }
                // Its name and the Enter mark: always laid out at full width, shown as far as the slot is armed.
                Row(
                    Modifier
                        .graphicsLayer { alpha = (on(k) * 1.6f).coerceAtMost(1f) }
                        .drawWithContent { clipRect(right = size.width * on(k)) { this@drawWithContent.drawContent() } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val name = act?.label ?: if (opened) lessLabel else moreLabel
                    AnimatedContent(
                        if (confirming && k == at) confirmLabel else name,
                        transitionSpec = { (fadeIn(motion.fade(110, 40)) togetherWith fadeOut(motion.fade(60))).using(SizeTransform(clip = false) { _, _ -> motion.lead() }) },
                        contentAlignment = Alignment.CenterEnd, label = "name",
                    ) { text ->
                        Text(text, color = ink, style = LABEL, maxLines = 1, softWrap = false)
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(Symbols.enter, null, Modifier.size(14.dp), tint = ink)
                }
            }
        },
    ) { measurables, constraints ->
        val h = SLOT.roundToPx()
        val edge = EDGE.roundToPx()
        val rest = REST.roundToPx()
        val gap = GAP.roundToPx()
        val air = AIR.roundToPx()
        val icons = List(n) { measurables[2 * it].measure(Constraints()) }
        val tails = List(n) { measurables[2 * it + 1].measure(Constraints(maxHeight = h)) }
        var x = 0
        for (k in 0 until n) {
            val shown = on(k)
            // The armed slot keeps its neighbours at arm's length: a little air on each side that has one.
            val before = if (k > 0) (air * shown).roundToInt() else 0
            val after = if (k < n - 1) (air * shown).roundToInt() else 0
            // At rest the icon has 7 dp on either side; armed, the pane's own 9.
            val side = rest + ((edge - rest) * shown).roundToInt()
            slots.x[k] = x
            slots.p0[k] = x + before
            slots.w[k] = before + side + icons[k].width + (shown * (gap + tails[k].width)).roundToInt() + side + after
            slots.p1[k] = x + slots.w[k] - after
            slots.icon[k] = slots.p0[k] + side
            x += slots.w[k]
        }
        layout(x.coerceAtMost(constraints.maxWidth), h) {
            for (k in 0 until n) {
                icons[k].place(slots.icon[k], (h - icons[k].height) / 2)
                tails[k].place(slots.icon[k] + icons[k].width + gap, (h - tails[k].height) / 2)
            }
        }
    }
}

/** The pane and its one rim, from [x0] to [x1], the height of the strip. */
private fun DrawScope.pane(x0: Float, x1: Float, fill: Color, rim: Color) {
    val r = CornerRadius(size.height / 2)
    drawRoundRect(fill, Offset(x0, 0f), Size(x1 - x0, size.height), r)
    drawRoundRect(rim, Offset(x0 + 0.5f, 0.5f), Size(x1 - x0 - 1f, size.height - 1f), r, style = Stroke(1f))
}

/**
 * A choice between a few named options, all visible: the first-run card's two answers, a setting's
 * values. One highlight for the whole strip; its leading edge travels on a quicker spring than its
 * trailing edge. No option has a background of its own and none changes size or weight when
 * chosen, so nothing can jump: the mark sits in every slot and shows where the highlight is.
 */
@Composable
fun OptionStrip(
    options: List<String>,
    chosen: Int,
    onChoose: (Int) -> Unit,
    onRun: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** One under the other, all as wide as the widest (the card); else side by side. */
    vertical: Boolean = false,
    mark: ImageVector = Symbols.enter,
    /** On a row that may itself be selected (the Booklight window): the highlight is a pane of glass, not the selection's colour. */
    quiet: Boolean = false,
    /** The text's colour, when the strip sits on something other than the panel's glass. */
    ink: Color? = null,
    /** False: nothing is chosen yet (a tip's answers before Tab): no highlight and no mark, until something is. */
    lit: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val n = options.size
    val slots = remember(n) { Slots(n) }
    // The highlight's two edges along the strip's axis, in px, and how far along it is (for the marks).
    val head = remember { Animatable(Float.NaN) }
    val tail = remember { Animatable(Float.NaN) }
    val a = remember { Animatable(chosen.toFloat()) }
    val choose by rememberUpdatedState(onChoose)
    val run by rememberUpdatedState(onRun)
    val fill = if (quiet) scheme.surfaceContainerLowest.copy(alpha = if (dark) 0.36f else 0.62f) else scheme.secondaryContainer.copy(alpha = if (dark) 0.66f else 0.78f)
    val rim = Color.White.copy(alpha = if (dark) 0.30f else 0.55f)
    val hair = if (!quiet) Color.Transparent else if (dark) Color.Black.copy(alpha = 0.28f) else scheme.onSurface.copy(alpha = 0.20f)
    LaunchedEffect(chosen) { a.animateTo(chosen.toFloat(), motion.arm()) }
    val glow by animateFloatAsState(if (lit) 1f else 0f, motion.fade(120), label = "lit")

    Layout(
        modifier = modifier
            .drawBehind {
                if (n == 0 || glow <= 0f) return@drawBehind
                // Before the first move the highlight simply is where the chosen slot is.
                val still = head.value.isNaN()
                val lo = if (still) slots.x[chosen.coerceIn(0, n - 1)].toFloat() else minOf(head.value, tail.value)
                val hi = if (still) lo + slots.w[chosen.coerceIn(0, n - 1)] else maxOf(head.value, tail.value)
                val across = if (vertical) size.width else size.height
                val r = CornerRadius(SLOT.toPx() / 2)
                val o = if (vertical) Offset(0f, lo) else Offset(lo, 0f)
                val s = if (vertical) Size(across, hi - lo) else Size(hi - lo, across)
                if (hair.alpha > 0f) drawRoundRect(hair.copy(alpha = hair.alpha * glow), o - Offset(0.5f, 0.5f), Size(s.width + 1f, s.height + 1f), CornerRadius(r.x + 0.5f), style = Stroke(1f))
                drawRoundRect(fill.copy(alpha = fill.alpha * glow), o, s, r)
                drawRoundRect(rim.copy(alpha = rim.alpha * glow), o + Offset(0.5f, 0.5f), Size(s.width - 1f, s.height - 1f), r, style = Stroke(1f))
            }
            .pointerInput(n) { detectTapGestures { run(slots.at(if (vertical) it.y else it.x)) } }
            .pointerInput(n) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        if (e.type == PointerEventType.Move) e.changes.firstOrNull()?.let { choose(slots.at(if (vertical) it.position.y else it.position.x)) }
                    }
                }
            },
        content = {
            options.forEachIndexed { k, label ->
                Text(label, color = ink ?: scheme.onSurface, style = LABEL.copy(fontSize = 14.sp, fontWeight = FontWeight(600)), maxLines = 1, softWrap = false,
                    modifier = Modifier.graphicsLayer { alpha = SECOND + (1f - SECOND) * on(a.value, k) * glow })
                Icon(mark, null, Modifier.size(14.dp).graphicsLayer { alpha = on(a.value, k) * glow }, tint = ink ?: scheme.onSurface)
            }
        },
    ) { measurables, _ ->
        val h = SLOT.roundToPx()
        val edge = 14.dp.roundToPx()
        val gap = 8.dp.roundToPx()
        val between = if (vertical) 4.dp.roundToPx() else 0
        val labels = List(n) { measurables[2 * it].measure(Constraints()) }
        val marks = List(n) { measurables[2 * it + 1].measure(Constraints()) }
        val widths = IntArray(n) { edge + labels[it].width + gap + marks[it].width + edge - 4.dp.roundToPx() }
        val widest = widths.maxOrNull() ?: 0
        var p = 0
        for (k in 0 until n) {
            slots.x[k] = p
            slots.w[k] = if (vertical) h else widths[k]
            p += slots.w[k] + between
        }
        val along = (p - between).coerceAtLeast(0)
        layout(if (vertical) widest else along, if (vertical) along else h) {
            for (k in 0 until n) {
                val w = if (vertical) widest else widths[k]
                val x0 = if (vertical) 0 else slots.x[k]
                val y0 = if (vertical) slots.x[k] else 0
                marks[k].place(x0 + w - edge + 4.dp.roundToPx() - marks[k].width, y0 + (h - marks[k].height) / 2)
                // One under the other, the names start on one edge; side by side each stands before its mark.
                labels[k].place(if (vertical) x0 + edge else x0 + w - edge + 4.dp.roundToPx() - marks[k].width - gap - labels[k].width, y0 + (h - labels[k].height) / 2)
            }
        }
    }
    // Leading edge first: towards higher slots the far edge leads, towards lower ones the near edge.
    LaunchedEffect(chosen, n) {
        withFrameNanos { }   // the slots are known once the strip has been laid out
        val lo = slots.x.getOrElse(chosen) { 0 }.toFloat()
        val hi = lo + slots.w.getOrElse(chosen) { 0 }
        if (head.value.isNaN() || !motion.on) { head.snapTo(hi); tail.snapTo(lo); return@LaunchedEffect }
        val forward = hi > head.targetValue
        launch { head.animateTo(hi, if (forward) motion.lead() else motion.trail()) }
        launch { tail.animateTo(lo, if (forward) motion.trail() else motion.lead()) }
    }
}
