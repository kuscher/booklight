package io.github.kuscher.booklight.window

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.overlay.LocalMotion

/**
 * A pane's scrollbar: a thumb 4 dp wide, 4 dp from the pane's trailing edge, there while the pane scrolls or
 * the pointer is over the pane ([over]) or on the bar, and gone when the pane has nothing to scroll. It can
 * be dragged; its target is wider than what is drawn, so a pointer finds it at the window's edge.
 */
@Composable
fun Scrollbar(scroll: ScrollState, over: Boolean, modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    val ink = MaterialTheme.colorScheme.onSurface
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val dragged by source.collectIsDraggedAsState()
    val can = scroll.maxValue > 0 && scroll.maxValue != Int.MAX_VALUE
    val alpha by animateFloatAsState(if (can && (over || hovered || dragged || scroll.isScrollInProgress)) 1f else 0f, motion.fade(if (can) 160 else 80), label = "scrollbar")
    val strong by animateFloatAsState(if (hovered || dragged) 0.5f else 0.26f, motion.fade(120), label = "thumb")
    /** How far the content goes for each px the thumb is dragged: set when it is drawn. */
    val gear = remember { FloatArray(1) }
    Box(
        modifier.fillMaxHeight().width(16.dp).hoverable(source)
            .draggable(rememberDraggableState { d -> scroll.dispatchRawDelta(d * gear[0]) }, Orientation.Vertical, enabled = can, interactionSource = source)
            .drawBehind {
                if (alpha <= 0f || !can) return@drawBehind
                val view = size.height
                val all = view + scroll.maxValue
                val inset = 4.dp.toPx()
                val track = view - 2 * inset
                val thumb = (track * view / all).coerceAtLeast(32.dp.toPx())
                gear[0] = scroll.maxValue / (track - thumb).coerceAtLeast(1f)
                val top = inset + (track - thumb) * scroll.value / scroll.maxValue
                val w = 4.dp.toPx()
                drawRoundRect(ink.copy(alpha = strong * alpha), Offset(size.width - inset - w, top), Size(w, thumb), CornerRadius(w / 2))
            },
    )
}

/**
 * What scrolls goes out softly at the pane's upper and lower edge, over 16 dp, instead of being cut on a line:
 * at the top only once it has been scrolled, at the bottom only while there is more below.
 */
fun Modifier.softEdges(scroll: ScrollState): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val soft = 16.dp.toPx()
        val above = (scroll.value / soft).coerceIn(0f, 1f)
        val below = if (scroll.maxValue == Int.MAX_VALUE) 0f else ((scroll.maxValue - scroll.value) / soft).coerceIn(0f, 1f)
        if (above > 0f) drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 1f - above), Color.Black), 0f, soft), Offset.Zero, Size(size.width, soft), blendMode = BlendMode.DstIn)
        if (below > 0f) drawRect(Brush.verticalGradient(listOf(Color.Black, Color.Black.copy(alpha = 1f - below)), size.height - soft, size.height), Offset(0f, size.height - soft), Size(size.width, soft), blendMode = BlendMode.DstIn)
    }
