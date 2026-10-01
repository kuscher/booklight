package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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

    fun at(px: Float): Int {
        for (k in x.indices) if (px < x[k] + w[k]) return k
        return x.lastIndex
    }
}

/** How much of slot [k] the highlight is on, when the highlight is at [a]: 1 on it, 0 a whole slot away. */
private fun on(a: Float, k: Int) = (1f - abs(a - k)).coerceIn(0f, 1f)

private val LABEL = TextStyle(fontFamily = Fonts.text, fontSize = 13.sp, fontWeight = FontWeight(500), letterSpacing = 0.1.sp)
private val SLOT = 32.dp
private val EDGE = 9.dp
private val GAP = 7.dp

/**
 * What the selected row can do: every action as an icon, in a row. One is armed and Enter runs it;
 * it is unrolled to its name and the Enter mark, on a small pane of the panel's own glass.
 *
 * The arming is one number on a spring. Each action shows as much of its name as that number is
 * on it, and the pane is drawn around wherever the number is, so the name unrolling, the
 * neighbours making room and the pane travelling are one movement that cannot fall out of step.
 */
@Composable
fun ActionStrip(
    actions: List<Action>,
    armed: Int,
    /** The armed action is waiting for its second Enter. */
    confirming: Boolean,
    /** What the armed action reads while it waits: "Press again to delete". */
    confirmLabel: String,
    onArm: (Int) -> Unit,
    onRun: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val n = actions.size
    val slots = remember(n) { Slots(n) }
    val a = remember { Animatable(armed.toFloat()) }
    LaunchedEffect(armed) { a.animateTo(armed.toFloat(), motion.arm()) }
    // The line that drains while a confirmation waits.
    val left = remember { Animatable(1f) }
    LaunchedEffect(confirming) {
        left.snapTo(1f)
        if (confirming && motion.on) left.animateTo(0f, tween(OverlayModel.CONFIRM_MS.toInt(), easing = LinearEasing))
    }
    val arm by rememberUpdatedState(onArm)
    val run by rememberUpdatedState(onRun)

    // The pane: a small sheet of the panel's own veil, lighter than the selection in light theme and
    // darker in dark, so it lifts the name's contrast in both and adds no colour of its own.
    val pane = scheme.surfaceContainerLowest.copy(alpha = if (dark) 0.36f else 0.62f)
    val rim = Color.White.copy(alpha = if (dark) 0.30f else 0.55f)
    val hair = scheme.onSecondaryContainer.copy(alpha = 0.20f)
    val alarm = scheme.errorContainer
    val drain = scheme.onErrorContainer

    Layout(
        modifier = modifier
            .clearAndSetSemantics {}   // the row speaks for its actions (custom accessibility actions)
            .drawBehind {
                if (n == 0) return@drawBehind
                val at = a.value.coerceIn(0f, (n - 1).toFloat())
                val k0 = at.toInt()
                val k1 = minOf(n - 1, k0 + 1)
                val f = at - k0
                val x0 = slots.x[k0] + (slots.x[k1] - slots.x[k0]) * f
                val x1 = slots.x[k0] + slots.w[k0] + (slots.x[k1] + slots.w[k1] - slots.x[k0] - slots.w[k0]) * f
                var danger = 0f
                for (k in 0 until n) if (actions[k].danger) danger += on(at, k)
                pane(x0, x1, lerp(pane, alarm, danger.coerceIn(0f, 1f)), rim.copy(alpha = rim.alpha * (1f - danger)), hair.copy(alpha = hair.alpha * (1f - danger)))
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
            actions.forEachIndexed { k, act ->
                val ink = if (act.danger) scheme.error else scheme.onSecondaryContainer
                if (act.symbol.startsWith("t:")) Text(act.symbol.substring(2), color = ink, style = LABEL.copy(fontSize = 11.sp, fontWeight = FontWeight(700), letterSpacing = 0.sp), maxLines = 1, softWrap = false)
                else Icon(Symbols.of(act.symbol), null, Modifier.size(18.dp), tint = ink)
                // Its name and the Enter mark: always laid out at full width, shown as far as the pane is on it.
                Row(
                    Modifier
                        .graphicsLayer { alpha = (on(a.value, k) * 1.6f).coerceAtMost(1f) }
                        .drawWithContent { clipRect(right = size.width * on(a.value, k)) { this@drawWithContent.drawContent() } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(
                        confirming && k == armed,
                        transitionSpec = { (fadeIn(motion.fade(110, 40)) togetherWith fadeOut(motion.fade(60))).using(SizeTransform(clip = false) { _, _ -> motion.lead() }) },
                        contentAlignment = Alignment.CenterEnd, label = "name",
                    ) { sure ->
                        Text(if (sure) confirmLabel else act.label, color = if (sure) scheme.onErrorContainer else ink, style = LABEL, maxLines = 1, softWrap = false)
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(Symbols.enter, null, Modifier.size(14.dp), tint = if (act.danger) scheme.onErrorContainer else ink)
                }
            }
        },
    ) { measurables, constraints ->
        val h = SLOT.roundToPx()
        val edge = EDGE.roundToPx()
        val gap = GAP.roundToPx()
        val icons = List(n) { measurables[2 * it].measure(Constraints()) }
        val tails = List(n) { measurables[2 * it + 1].measure(Constraints(maxHeight = h)) }
        val at = a.value.coerceIn(0f, (n - 1).coerceAtLeast(0).toFloat())
        var x = 0
        for (k in 0 until n) {
            slots.x[k] = x
            slots.w[k] = edge + icons[k].width + (on(at, k) * (gap + tails[k].width)).roundToInt() + edge
            x += slots.w[k]
        }
        layout(x.coerceAtMost(constraints.maxWidth), h) {
            for (k in 0 until n) {
                val left = slots.x[k] + edge
                icons[k].place(left, (h - icons[k].height) / 2)
                tails[k].place(left + icons[k].width + gap, (h - tails[k].height) / 2)
            }
        }
    }
}

/** The pane and its two rings, from [x0] to [x1], the height of the strip. */
private fun DrawScope.pane(x0: Float, x1: Float, fill: Color, rim: Color, hair: Color) {
    val r = CornerRadius(size.height / 2)
    val px = 1f
    drawRoundRect(hair, Offset(x0 - px, -px), Size(x1 - x0 + 2 * px, size.height + 2 * px), CornerRadius(size.height / 2 + px), style = Stroke(px))
    drawRoundRect(fill, Offset(x0, 0f), Size(x1 - x0, size.height), r)
    drawRoundRect(rim, Offset(x0 + px / 2, px / 2), Size(x1 - x0 - px, size.height - px), r, style = Stroke(px))
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
    val fill = scheme.secondaryContainer.copy(alpha = if (dark) 0.66f else 0.78f)
    val rim = Color.White.copy(alpha = if (dark) 0.30f else 0.55f)
    LaunchedEffect(chosen) { a.animateTo(chosen.toFloat(), motion.arm()) }

    Layout(
        modifier = modifier
            .drawBehind {
                if (n == 0) return@drawBehind
                // Before the first move the highlight simply is where the chosen slot is.
                val still = head.value.isNaN()
                val lo = if (still) slots.x[chosen.coerceIn(0, n - 1)].toFloat() else minOf(head.value, tail.value)
                val hi = if (still) lo + slots.w[chosen.coerceIn(0, n - 1)] else maxOf(head.value, tail.value)
                val across = if (vertical) size.width else size.height
                val r = CornerRadius(SLOT.toPx() / 2)
                val o = if (vertical) Offset(0f, lo) else Offset(lo, 0f)
                val s = if (vertical) Size(across, hi - lo) else Size(hi - lo, across)
                drawRoundRect(fill, o, s, r)
                drawRoundRect(rim, o + Offset(0.5f, 0.5f), Size(s.width - 1f, s.height - 1f), r, style = Stroke(1f))
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
                Text(label, color = scheme.onSurface, style = LABEL.copy(fontSize = 14.sp, fontWeight = FontWeight(600)), maxLines = 1, softWrap = false,
                    modifier = Modifier.graphicsLayer { alpha = SECOND + (1f - SECOND) * on(a.value, k) })
                Icon(mark, null, Modifier.size(14.dp).graphicsLayer { alpha = on(a.value, k) }, tint = scheme.onSecondaryContainer)
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
                labels[k].place(x0 + w - edge + 4.dp.roundToPx() - marks[k].width - gap - labels[k].width, y0 + (h - labels[k].height) / 2)
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
