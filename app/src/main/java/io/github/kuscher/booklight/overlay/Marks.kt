package io.github.kuscher.booklight.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The check that draws itself: one stroke, wherever Booklight says "done" (a ticked task, the word
 * in the footer, "Your key works", "Copied" in a pinned window). [shown] false takes it back the
 * way it came. The same mark everywhere is the point.
 */
@Composable
fun DrawnCheck(shown: Boolean, color: Color, modifier: Modifier = Modifier, stroke: Dp = 2.dp) {
    val motion = LocalMotion.current
    val drawn = remember { Animatable(0f) }
    LaunchedEffect(shown) { drawn.animateTo(if (shown) 1f else 0f, motion.fade(140, easing = LinearEasing)) }
    val whole = remember { Path() }
    val part = remember { Path() }
    val measure = remember { PathMeasure() }
    Canvas(modifier) {
        if (drawn.value <= 0f) return@Canvas
        // On a 24-unit grid: down to the corner, then up to the right.
        val u = size.minDimension / 24f
        whole.rewind(); whole.moveTo(5f * u, 12.5f * u); whole.lineTo(10f * u, 17.5f * u); whole.lineTo(19.5f * u, 7f * u)
        measure.setPath(whole, false)
        part.rewind(); measure.getSegment(0f, measure.length * drawn.value, part, true)
        drawPath(part, color, style = Stroke(stroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Text that does not fit its box fades over its last [width] instead of ending in an ellipsis:
 * a title beside a strip whose width changes keeps its glyphs, laid out once, and is only covered.
 */
fun Modifier.fadeEnd(width: Dp = 24.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val w = width.toPx().coerceAtMost(size.width)
        drawRect(
            Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - w, endX = size.width),
            topLeft = Offset(size.width - w, 0f), blendMode = BlendMode.DstIn,
        )
    }
