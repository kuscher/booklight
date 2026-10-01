package io.github.kuscher.booklight.window

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.overlay.LocalDark
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.SECOND
import io.github.kuscher.booklight.overlay.SMALL
import io.github.kuscher.booklight.ui.Fonts

/**
 * The Booklight window's rows and their one selection. The window is built like the panel: rows
 * on one ground, and a single pill that glides to whichever row the pointer or the arrow keys
 * are on. Up and Down move it, Left and Right change the row's control, Enter runs the row.
 */
class Page {
    class Entry(var top: Float = 0f, var height: Float = 0f, var enter: () -> Unit = {}, var step: ((Int) -> Unit)? = null)

    var selected by mutableStateOf<String?>(null)
    /** Bumped when a row moved, so the pill follows a section that opened above it. */
    var placed by mutableIntStateOf(0)
    val rows = LinkedHashMap<String, Entry>()
    /** The scrolling content: rows measure themselves against it. */
    var root: LayoutCoordinates? = null

    /** The row above or below the selected one, in the order they are on the page. */
    fun move(by: Int): Entry? {
        val order = rows.entries.filter { it.value.height > 0 }.sortedBy { it.value.top }
        if (order.isEmpty()) return null
        val at = order.indexOfFirst { it.key == selected }
        val to = if (at < 0) (if (by > 0) 0 else order.lastIndex) else (at + by).coerceIn(0, order.lastIndex)
        selected = order[to].key
        return order[to].value
    }

    val current: Entry? get() = selected?.let { rows[it] }
}

/** A section: its title, then its rows, 48 dp from the next. No box around it. */
@Composable
fun Section(title: String, about: String? = null, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(top = 44.dp)) {
        Text(title, color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.round, fontSize = 22.sp, fontWeight = FontWeight(600)), modifier = Modifier.padding(start = 12.dp, bottom = if (about == null) 8.dp else 2.dp))
        about?.let { Text(it, color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 15.sp, lineHeight = 21.sp), modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) }
        content()
    }
}

/**
 * One row of the page, with the panel's own measure: a mark in the first column, a title and maybe
 * a line under it, and its control at the right end.
 */
@Composable
fun PageRow(
    page: Page, key: String, title: String, subtitle: String? = null,
    mark: (@Composable (ink: Color) -> Unit)? = null,
    onEnter: () -> Unit = {},
    /** Left (−1) and Right (+1) on the row: the next or previous choice of its control. */
    onStep: ((Int) -> Unit)? = null,
    /** False: the row keeps its place but is dimmed, and the selection passes over it (what it switches is switched off above it). */
    enabled: Boolean = true,
    /** How many lines the line under the title may take; 0 = as many as it needs. */
    lines: Int = 0,
    /** Something under the title, on the title's edge: a strip of choices that is too wide to stand beside it. */
    below: (@Composable (ink: Color) -> Unit)? = null,
    trailing: @Composable RowScope.(ink: Color) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val selected = page.selected == key
    val ink by animateColorAsState(scheme.onSurface.copy(alpha = if (enabled) 1f else 0.40f), motion.fade(120), label = "row")
    val enter by rememberUpdatedState(onEnter)
    val step by rememberUpdatedState(onStep)
    val entry = remember(key) { Page.Entry() }
    entry.enter = { enter() }
    entry.step = if (onStep == null) null else { d -> step?.invoke(d) }
    DisposableEffect(key, enabled) {
        if (enabled) page.rows[key] = entry else if (page.selected == key) page.selected = null
        onDispose { page.rows.remove(key); if (page.selected == key) page.selected = null }
    }
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
            .onGloballyPositioned { c ->
                val top = page.root?.takeIf { it.isAttached }?.localPositionOf(c, Offset.Zero)?.y ?: return@onGloballyPositioned
                if (top != entry.top || c.size.height.toFloat() != entry.height) { entry.top = top; entry.height = c.size.height.toFloat(); page.placed++ }
            }
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(key, enabled) { awaitPointerEventScope { while (true) if (awaitPointerEvent().type == PointerEventType.Move && enabled) page.selected = key } }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled) { page.selected = key; enter() }
            .semantics(mergeDescendants = true) { this.selected = selected; role = Role.Button }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mark != null) Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { mark(ink) }
        Column(Modifier.weight(1f).padding(start = if (mark != null) 16.dp else 0.dp, end = 12.dp)) {
            Text(title, color = ink, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)))
            subtitle?.let { Text(it, color = ink.copy(alpha = ink.alpha * SECOND), style = SMALL.copy(lineHeight = 18.sp), maxLines = if (lines > 0) lines else Int.MAX_VALUE, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
            // The strip's first name stands on the title's edge: its highlight reaches 14 dp out to the left of it.
            below?.let { Box(Modifier.padding(top = 6.dp).offset(x = (-14).dp)) { it(ink) } }
        }
        trailing(ink)
    }
}

/** A switch, flat: a thumb in a stadium. On: the track is ink and the thumb is on the right. */
@Composable
fun FlatSwitch(on: Boolean, ink: Color) {
    val motion = LocalMotion.current
    val at by animateFloatAsState(if (on) 1f else 0f, motion.pop(), label = "switch")
    val fill by animateColorAsState(if (on) ink else ink.copy(alpha = 0.20f), motion.fade(120), label = "track")
    val thumb = if (on) MaterialTheme.colorScheme.surfaceContainerLowest else ink
    Box(Modifier.size(44.dp, 24.dp).clip(CircleShape).background(fill).semantics { role = Role.Switch }) {
        val travel = with(LocalDensity.current) { 20.dp.toPx() }
        Box(Modifier.offset { IntOffset((3.dp.toPx() + at.coerceIn(0f, 1.1f) * travel).toInt(), 3.dp.roundToPx()) }.size(18.dp).clip(CircleShape).background(thumb))
    }
}

/** A key cap, as the keys in the text about shortcuts and the keyword of a link. */
@Composable
fun Cap(label: String, ink: Color, large: Boolean = false) {
    val dark = LocalDark.current
    Box(
        Modifier.defaultMinSize(minWidth = if (large) 34.dp else 28.dp, minHeight = if (large) 30.dp else 24.dp).clip(RoundedCornerShape(8.dp))
            .background(ink.copy(alpha = if (dark) 0.14f else 0.10f)).border(with(LocalDensity.current) { 1f.toDp() }, Color.White.copy(alpha = if (dark) 0.20f else 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = ink, style = TextStyle(fontFamily = Fonts.text, fontSize = if (large) 14.sp else 12.5.sp, fontWeight = FontWeight(600)), maxLines = 1) }
}

/** A few key caps in a row, with plus signs between them: Action + K. */
@Composable
fun Keys(vararg keys: String, ink: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.forEachIndexed { i, k ->
            if (i > 0) Text("+", color = ink.copy(alpha = SECOND), style = SMALL)
            Cap(k, ink, large = true)
        }
    }
}

/** How far a row is from the left edge of its column, for things that line up with row text. */
val GUTTER: Dp = 12.dp
