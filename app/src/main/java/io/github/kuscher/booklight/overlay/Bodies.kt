package io.github.kuscher.booklight.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.SlotState
import io.github.kuscher.booklight.ui.Fonts
import kotlinx.coroutines.launch

val SMALL = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(500), letterSpacing = 0.1.sp)
private val HINT = TextStyle(fontFamily = Fonts.text, fontSize = 12.sp, fontWeight = FontWeight(600), letterSpacing = 0.5.sp)
private val VALUE = TextStyle(fontFamily = Fonts.text, fontSize = 17.sp, fontWeight = FontWeight(500))

/**
 * A preview of what was understood from the typed line: a caption, then labelled slots that fill
 * as you type, then a line for the long part (a message). What was typed is in full ink, what was
 * guessed a step lighter, and an empty slot is its label and a short rule. The values change in
 * the same frame as the typing: they mirror the field and are never animated.
 */
@Composable
fun RowScope.SlotsBody(b: Body.Slots, ink: Color) {
    Column(Modifier.weight(1f).padding(start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        b.caption?.let { Text(it, color = ink.copy(alpha = ink.alpha * SECOND), style = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            b.slots.forEachIndexed { i, s ->
                // The first slots keep to their share; the last takes what is left, so a growing value never pushes its neighbour away.
                val last = i == b.slots.lastIndex
                Row(if (last) Modifier.weight(1f, fill = false) else Modifier.widthIn(max = if (i == 0) 240.dp else 170.dp)) {
                    // Label and value sit on one baseline.
                    Text(s.label.uppercase(), color = ink.copy(alpha = ink.alpha * SECOND), style = HINT, maxLines = 1, modifier = Modifier.alignByBaseline().padding(end = 7.dp))
                    if (s.state == SlotState.EMPTY) Text("–", color = ink.copy(alpha = ink.alpha * 0.40f), style = VALUE, modifier = Modifier.alignByBaseline())
                    else Text(s.value, color = ink.copy(alpha = ink.alpha * if (s.state == SlotState.GUESSED) SECOND else 1f), style = VALUE, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alignByBaseline())
                }
            }
        }
        b.note?.let { Text(it, color = ink, style = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/**
 * A level as a track: the fill up to where it is, a flat thumb, and a tick where a typed value
 * would put it. Hollow while Booklight may not change it yet.
 */
@Composable
fun LevelTrack(b: Body.Level, ink: Color, modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    val shown = if (b.muted) 0f else b.percent / 100f
    // One animated level drives the fill's end and the thumb, so they cannot part.
    val level by animateFloatAsState(shown, motion.place(), label = "level")
    Canvas(modifier.size(200.dp, 24.dp)) {
        val h = 6.dp.toPx()
        val y = (size.height - h) / 2
        val r = CornerRadius(h / 2)
        drawRoundRect(ink.copy(alpha = 0.20f), Offset(0f, y), Size(size.width, h), r)
        val gap = 4.dp.toPx()
        val bar = 4.dp.toPx()
        val x = level * (size.width - bar)
        if (x > gap) drawRoundRect(ink.copy(alpha = if (b.muted) 0.40f else 1f), Offset(0f, y), Size(x - gap, h), r)
        drawRoundRect(ink, Offset(x, (size.height - 20.dp.toPx()) / 2), Size(bar, 20.dp.toPx()), CornerRadius(bar / 2))
        b.target?.let { t ->
            val tx = t.coerceIn(0, 100) / 100f * (size.width - bar) + bar / 2
            drawRoundRect(ink.copy(alpha = 0.55f), Offset(tx - 1.dp.toPx(), (size.height - 12.dp.toPx()) / 2), Size(2.dp.toPx(), 12.dp.toPx()), CornerRadius(1.dp.toPx()))
        }
    }
}

/** A level in words, in a slot that never changes width: "40 %", "Muted". */
@Composable
fun LevelNumber(b: Body.Level, ink: Color, modifier: Modifier = Modifier, end: Boolean = false) {
    val text = when {
        b.locked -> ""
        b.muted -> stringResource(R.string.level_muted)
        else -> stringResource(R.string.level_percent, b.percent)
    }
    // Beside its track it reads from the left; alone at the row's end (the row isn't selected) it ends where the kind labels end.
    Text(text, color = ink, style = SMALL.copy(fontFeatureSettings = "tnum"), maxLines = 1,
        modifier = modifier.width(if (b.muted) 56.dp else 48.dp), textAlign = if (end) TextAlign.End else TextAlign.Start)
}

/**
 * A grid of characters to pick from. One square highlight for the whole grid; all four arrows
 * move it, and it stretches towards where it is going (the edge that leads moves on the quicker
 * spring). On arrival the cells come in as a short diagonal wave.
 */
@Composable
fun GridBody(b: Body.Grid, cell: Int, selected: Boolean, onCell: (Int) -> Unit, onPick: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val density = LocalDensity.current
    val cols = b.columns
    // Fourteen columns from 14 dp to 14 dp from the panel's edges, so the first is centred on the mark column (38 dp).
    val pitchDp = (Metrics.width - 28.dp) / cols
    val side = with(density) { Metrics.cell.toPx() }
    val pitch = with(density) { pitchDp.toPx() }
    val shown = b.cells.take(cols * 5)
    val col = cell % cols
    val line = cell / cols
    // The highlight's four edges, in cells.
    val x0 = remember { Animatable(col.toFloat()) }
    val x1 = remember { Animatable(col + 1f) }
    val y0 = remember { Animatable(line.toFloat()) }
    val y1 = remember { Animatable(line + 1f) }
    LaunchedEffect(cell) {
        val dx = col - x0.targetValue
        val dy = line - y0.targetValue
        // Along one axis the leading edge goes first; a jump to another line's start moves as one piece.
        val rigid = dx != 0f && dy != 0f
        launch { x0.animateTo(col.toFloat(), if (rigid) motion.place() else if (dx > 0) motion.trail() else motion.lead()) }
        launch { x1.animateTo(col + 1f, if (rigid) motion.place() else if (dx > 0) motion.lead() else motion.trail()) }
        launch { y0.animateTo(line.toFloat(), if (rigid) motion.place() else if (dy > 0) motion.trail() else motion.lead()) }
        launch { y1.animateTo(line + 1f, if (rigid) motion.place() else if (dy > 0) motion.lead() else motion.trail()) }
    }
    val wave = remember { Animatable(if (motion.on) 0f else 1f) }
    LaunchedEffect(Unit) { wave.animateTo(1f, motion.fade(240)) }
    val pick by rememberUpdatedState(onPick)
    val hover by rememberUpdatedState(onCell)
    val fill = scheme.secondaryContainer.copy(alpha = if (dark) 0.66f else 0.78f)
    val rim = Color.White.copy(alpha = if (dark) 0.30f else 0.55f)
    val count = shown.size
    fun at(o: Offset): Int? {
        val c = (o.x / pitch).toInt()
        val l = (o.y / side).toInt()
        val i = l * cols + c
        return if (c in 0 until cols && i in 0 until count) i else null
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier.size(pitchDp * cols, Metrics.cell * Metrics.gridRows(b))
                .drawBehind {
                    if (!selected || count == 0) return@drawBehind
                    val inset = 2.dp.toPx()
                    val across = (pitch - side) / 2 + inset     // the square stays 44 dp, centred in its column
                    val o = Offset(x0.value * pitch + across, y0.value * side + inset)
                    val s = Size((x1.value - x0.value) * pitch - 2 * across, (y1.value - y0.value) * side - 2 * inset)
                    val r = CornerRadius(14.dp.toPx())
                    drawRoundRect(fill, o, s, r)
                    drawRoundRect(rim, o + Offset(0.5f, 0.5f), Size(s.width - 1f, s.height - 1f), r, style = Stroke(1f))
                }
                .pointerInput(count, cols) { detectTapGestures { o -> at(o)?.let { pick(it) } } }
                .pointerInput(count, cols) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent()
                            if (e.type == PointerEventType.Move) e.changes.firstOrNull()?.let { c -> at(c.position)?.let { hover(it) } }
                        }
                    }
                },
        ) {
            val rise = with(density) { 8.dp.toPx() }
            shown.forEachIndexed { i, c ->
                val step = ((i % cols) + (i / cols)) * 0.03f      // a little later for every step down and to the right
                Box(
                    Modifier.offset { IntOffset(((i % cols) * pitch).toInt(), ((i / cols) * side).toInt()) }.size(pitchDp, Metrics.cell)
                        .graphicsLayer {
                            val t = ((wave.value - step.coerceAtMost(0.5f)) / 0.5f).coerceIn(0f, 1f)
                            alpha = t
                            translationY = (1f - t) * rise
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(c.glyph, color = scheme.onSurface, style = TextStyle(fontSize = if (c.glyph.length <= 1) 26.sp else 27.sp), maxLines = 1)
                }
            }
        }
    }
}

/** A QR code on a plate that is white in both themes (a code has to be dark on light to scan). */
@Composable
fun QrPlate(text: String, modifier: Modifier = Modifier) {
    val matrix = remember(text) { qr(text) }
    Box(modifier.size(176.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(12.dp)) {
        Canvas(Modifier.size(152.dp)) {
            val m = matrix ?: return@Canvas
            val n = m.width
            val px = size.width / n
            for (y in 0 until n) for (x in 0 until n) if (m.get(x, y)) drawRect(Color.Black, Offset(x * px, y * px), Size(px + 0.5f, px + 0.5f))
        }
    }
}

/** The smallest QR code for [text], without a quiet zone (the plate is the quiet zone); null if it doesn't fit in one. */
fun qr(text: String): BitMatrix? = runCatching {
    QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.CHARACTER_SET to "UTF-8"))
}.getOrNull()

/** A swatch of one colour, with a hairline so white and black still have an edge. */
@Composable
fun Swatch(argb: Int, ink: Color) {
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.size(40.dp).clip(shape).background(Color(argb)).border(with(LocalDensity.current) { 1f.toDp() }, ink.copy(alpha = 0.20f), shape))
}

/**
 * A password or a UUID, large. In the panel's own face rather than a fixed-width one (the device has no
 * monospace cut of it, and the system's looks like another app): figures are tabular, the zero is slashed,
 * and the letters are set a little apart so each can be read off.
 */
@Composable
fun MonoText(text: String, ink: Color) {
    Text(text, color = ink, style = TextStyle(fontFamily = Fonts.text, fontSize = if (text.length > 28) 19.sp else 24.sp, fontWeight = FontWeight(500), letterSpacing = 0.8.sp, fontFeatureSettings = "tnum, zero"),
        maxLines = 1, overflow = TextOverflow.Ellipsis)
}

