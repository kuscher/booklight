package io.github.kuscher.booklight.window

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalRippleThemeConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleThemeConfiguration
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.ui.Fonts
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What the keys can reach on the page that shows: its rows, and the few controls that stand outside rows
 * (the Commands page's own). Material's parts are not focus stops themselves; the window's root has the
 * keyboard, and this registry knows each stop's place, its order and what the keys do to it. Up and Down
 * move through the stops in reading order, Left and Right change a stop's control, Enter runs it. One ring,
 * drawn by the window ([FocusRing]), shows where the keys are.
 */
class Page {
    class Entry(var left: Float = 0f, var top: Float = 0f, var width: Float = 0f, var height: Float = 0f) {
        var enter: () -> Unit = {}
        /** Left (−1) or Right (+1) on it. It says whether anything was stepped: a choice at its first option has nothing before it. */
        var step: ((Int) -> Boolean)? = null
        /** Delete was pressed on it (one of the user's own). */
        var delete: (() -> Unit)? = null
        /** The Menu key was pressed on it: its right-click menu opens at the row. */
        var menu: (() -> Unit)? = null
        /** How round the ring is on it: a row's 16 dp, or as much as half its height for a round button. */
        var corner: Dp = 16.dp
        /** What Material draws its hover, press and focused shape from. */
        var source: MutableInteractionSource? = null
        val bounds: Rect get() = Rect(left, top, left + width, top + height)
    }

    var selected by mutableStateOf<String?>(null)
    /** The keys are on the page and have moved since the pointer was last pressed: the ring shows, and the selected row has its focused shape. */
    var keys by mutableStateOf(false)
    /** Bumped when a stop moved or changed size, so the ring follows. */
    var placed by mutableIntStateOf(0)
    val rows = LinkedHashMap<String, Entry>()
    /** The scrolling content: stops measure themselves against it. */
    var root: LayoutCoordinates? = null
    /** Where that content is in the window, now: it changes as the page scrolls. */
    var origin by mutableStateOf(Offset.Zero)

    /** The stops in reading order: from the top, and from the leading side on one line. */
    fun order(): List<Map.Entry<String, Entry>> = rows.entries.filter { it.value.height > 0 }.sortedWith(compareBy({ it.value.top }, { it.value.left }))

    /** The stop [by] places after (or before) the selected one. */
    fun move(by: Int): Entry? {
        val order = order()
        if (order.isEmpty()) return null
        val at = order.indexOfFirst { it.key == selected }
        val to = if (at < 0) (if (by > 0) 0 else order.lastIndex) else (at + by).coerceIn(0, order.lastIndex)
        selected = order[to].key
        return order[to].value
    }

    /** The first stop, or the last. */
    fun end(last: Boolean): Entry? = order().let { if (last) it.lastOrNull() else it.firstOrNull() }?.also { selected = it.key }?.value

    /** The stop about [distance] px further down (or up): a page of rows. */
    fun page(distance: Float): Entry? {
        val order = order()
        val from = current ?: return move(if (distance > 0) 1 else -1)
        val want = from.top + distance
        val to = if (distance > 0) order.lastOrNull { it.value.top <= want } else order.firstOrNull { it.value.top >= want }
        return (to ?: if (distance > 0) order.lastOrNull() else order.firstOrNull())?.also { selected = it.key }?.value
    }

    val current: Entry? get() = selected?.let { rows[it] }

    /** Notes a stop's place in the scrolling content. */
    fun place(entry: Entry, c: LayoutCoordinates) {
        val at = root?.takeIf { it.isAttached }?.localPositionOf(c, Offset.Zero) ?: return
        val w = c.size.width.toFloat(); val h = c.size.height.toFloat()
        if (at.x != entry.left || at.y != entry.top || w != entry.width || h != entry.height) { entry.left = at.x; entry.top = at.y; entry.width = w; entry.height = h; placed++ }
    }
}

/** A row's place in its group: which of how many. Material's segmented rows take their corners from it. */
class Place(val index: Int, val count: Int)

/**
 * A group's rows, collected before any of them is drawn, so that each knows its place in the group. [row] adds
 * one; [between] adds something that stands between rows without being one (an editor under its row).
 */
class GroupScope {
    internal class Item(val key: Any?, val row: Boolean, val content: @Composable (Place) -> Unit)
    internal val items = ArrayList<Item>()
    fun row(key: Any? = null, content: @Composable (Place) -> Unit) { items += Item(key, true, content) }
    fun between(key: Any? = null, content: @Composable () -> Unit) { items += Item(key, false) { content() } }
}

/**
 * A group of related settings: a heading in sentence case, 16 dp in, and under it the rows as Material's
 * segmented list: 2 dp apart, the group's outer corners 16 dp and the corners between rows 4. Groups are 24 dp
 * apart; the first one of a page ([first]) stands 8 dp under what is above it. A group with nothing in it is
 * not there.
 */
@Composable
fun Group(title: String? = null, first: Boolean = false, build: GroupScope.() -> Unit) {
    val items = GroupScope().apply(build).items
    if (items.isEmpty()) return
    val count = items.count { it.row }
    Column(Modifier.fillMaxWidth().padding(top = if (first) 8.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        if (title != null) GroupLabel(title)
        var at = 0
        items.forEachIndexed { i, item ->
            val place = Place(at, count)
            if (item.row) at++
            key(item.key ?: i) { item.content(place) }
        }
    }
}

/** The name of a group of rows: on the title's text edge, 8 dp over its first row (6 here, 2 from the group's own spacing). */
@Composable
fun GroupLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = INSET, end = INSET, bottom = 6.dp))
}

/** How far text is from the column's edge: the title, the lead, a group's name and a row's mark all start here; a row's control ends this far from the other edge. */
val INSET: Dp = 16.dp

/** A row's mark has a box this wide and high; the row's text starts 12 dp after it (Material's measure). */
val MARK: Dp = 24.dp

/** One thing a right-click on a row offers. */
class RowAction(val label: String, val danger: Boolean = false, val run: () -> Unit)

/**
 * One row of a page: Material's segmented list item on `surfaceBright`, a mark at its leading end, its name
 * and maybe a line under it, and its control at its trailing end, 16 dp from the row's edge like every other
 * row's. The whole row is one target for the pointer: hover and press are Material's own. It is one stop for
 * the keys: when they are on it the window's ring is round it and it takes Material's focused shape.
 */
@Composable
fun PageRow(
    page: Page, key: String, title: String, subtitle: String? = null,
    mark: (@Composable (ink: Color) -> Unit)? = null,
    onEnter: () -> Unit = {},
    /** Left (−1) and Right (+1) on the row: the next or previous choice of its control. True if there was one. */
    onStep: ((Int) -> Boolean)? = null,
    /** Delete on the row (one of the user's own). */
    onDelete: (() -> Unit)? = null,
    /** False: the row keeps its place but is dimmed, and the keys pass over it (what it switches is switched off above it). */
    enabled: Boolean = true,
    /** The row only says something: it cannot be pressed and the keys pass over it. */
    still: Boolean = false,
    /** Not null: the row can be the one whose detail is open (it is then in the selection's colour); and whether it is. */
    chosen: Boolean? = null,
    /** The title rolls to its new text when it changes. */
    roll: Boolean = false,
    /** How many lines the line under the title may take; 0 = as many as it needs. */
    lines: Int = 0,
    /** Something under the text, on the text's edge: a choice in a narrow row. Mark and control then stand on the title's line. */
    below: (@Composable () -> Unit)? = null,
    /** Mark and control on the title's line and not on the row's centre line: when something stands under the text now. */
    onTitleLine: Boolean = below != null,
    /**
     * False: the pointer does not press the row, and the row shows no hover (a choice: only its buttons are pressed; a
     * click beside them must not change anything). The keys still reach it.
     */
    press: Boolean = true,
    /** What a right-click on the row offers. */
    menu: List<RowAction> = emptyList(),
    /** Where the row stands in its group. */
    place: Place? = null,
    trailing: (@Composable (ink: Color) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val live = enabled && !still
    val enter by rememberUpdatedState(onEnter)
    val step by rememberUpdatedState(onStep)
    val delete by rememberUpdatedState(onDelete)
    val source = remember(key) { MutableInteractionSource() }
    val entry = remember(key) { Page.Entry() }
    /** Where the right-click menu opens: at the pointer, or under the row when the Menu key asked for it. */
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    entry.enter = { enter() }
    entry.step = if (onStep == null) null else { d -> step?.invoke(d) == true }
    entry.delete = if (onDelete == null) null else ({ delete?.invoke() })
    entry.menu = if (menu.isEmpty()) null else ({ menuAt = Offset(entry.width / 2, entry.height) })
    entry.source = source.takeIf { press }
    DisposableEffect(key, live) {
        if (live) page.rows[key] = entry else if (page.selected == key) page.selected = null
        onDispose { page.rows.remove(key); if (page.selected == key) page.selected = null }
    }
    // The keys are on this row: Material is told it has the focus, and gives it its focused shape. The ring is the window's.
    val focused = live && page.keys && page.selected == key
    LaunchedEffect(focused) {
        if (!focused) return@LaunchedEffect
        val focus = FocusInteraction.Focus()
        source.emit(focus)
        try { awaitCancellation() } finally { withContext(NonCancellable) { source.emit(FocusInteraction.Unfocus(focus)) } }
    }
    val shapes = ListItemDefaults.segmentedShapes(place?.index ?: 0, place?.count ?: 1)
    // A row that the pointer does not press has no interaction source for Material to take its focused shape from: it is
    // given that shape here while the keys are on it, on the spring Material's own rows use for theirs. (Else the ring,
    // which is as round as the focused shape, would have the row's squarer corners showing outside it.)
    val morph by animateFloatAsState(if (focused && !press) 1f else 0f, motion.place(), label = "shape")
    val plain = if (press || morph == 0f) shapes else shapes.copy(shape = remember(shapes, morph) { Between(shapes.shape, shapes.focusedShape, morph) })
    val colors = ListItemDefaults.segmentedColors(
        containerColor = scheme.card, contentColor = scheme.onSurface, leadingContentColor = scheme.onSurfaceVariant, trailingContentColor = scheme.onSurfaceVariant,
        supportingContentColor = scheme.onSurfaceVariant, disabledContainerColor = scheme.card,
    )
    // A row with something under its text has its mark and its control on the title's line; any other has them on its own
    // centre line. When what is under the text goes (the keys of the first run), they move there with the row's height.
    val bias by animateFloatAsState(if (onTitleLine) -1f else 0f, motion.place(), label = "line")
    val align = BiasAlignment.Vertical(bias)
    val leading: (@Composable () -> Unit)? = mark?.let { m -> { Box(Modifier.size(MARK), contentAlignment = Alignment.Center) { m(LocalContentColor.current) } } }
    val end: (@Composable () -> Unit)? = trailing?.let { t -> { t(LocalContentColor.current) } }
    // The line under the name alone, or a column of it and what stands under it. Not one column for both cases: Material
    // takes a row for a three-line one when the first and the last baseline of this differ, and a column that loses its
    // second child kept the old last baseline (seen on the Lenovo: after the rail had closed, every command's row stayed
    // 88 dp high, because for a few frames of that its example had stood under its text).
    val supporting: (@Composable () -> Unit)? = if (subtitle == null && below == null) null else ({
        val line: @Composable () -> Unit = { subtitle?.let { Text(it, maxLines = if (lines > 0) lines else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis) } }
        if (below == null) line() else Column { line(); below() }
    })
    val name: @Composable () -> Unit = {
        if (roll) AnimatedContent(title, transitionSpec = { motion.roll() }, contentAlignment = Alignment.CenterStart, label = "title") { Text(it) }
        else Text(title)
    }
    Box(
        Modifier.fillMaxWidth().onGloballyPositioned { page.place(entry, it) }
            // The secondary button opens the row's menu where the pointer is; the row itself is not pressed.
            .then(if (!live || menu.isEmpty()) Modifier else Modifier.pointerInput(key) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        if (e.type == PointerEventType.Press && e.buttons.isSecondaryPressed) { e.changes.forEach { it.consume() }; page.selected = key; page.keys = false; menuAt = e.changes.first().position }
                    }
                }
            }),
    ) {
        // Material's own focus mark is not drawn on rows: the window has one ring for all of them.
        CompositionLocalProvider(LocalRippleThemeConfiguration provides NO_RING_THEME, LocalRippleConfiguration provides NO_RING) {
            val click = { page.selected = key; page.keys = false; enter() }
            // Material's row is not a focus stop of its own: the window's root has the keys, also when an editor's last button is left by Tab.
            val unfocused = Modifier.focusProperties { canFocus = false }
            when {
                still || !press -> SegmentedListItem(shapes = plain, leadingContent = leading, trailingContent = end, supportingContent = supporting, verticalAlignment = align, colors = colors, content = name)
                chosen != null -> SegmentedListItem(selected = chosen, onClick = click, modifier = unfocused, shapes = shapes, enabled = enabled, leadingContent = leading, trailingContent = end, supportingContent = supporting, verticalAlignment = align, colors = colors, interactionSource = source, content = name)
                else -> SegmentedListItem(onClick = click, modifier = unfocused, shapes = shapes, enabled = enabled, leadingContent = leading, trailingContent = end, supportingContent = supporting, verticalAlignment = align, colors = colors, interactionSource = source, content = name)
            }
        }
        if (menu.isNotEmpty()) menuAt?.let { at ->
            Box(Modifier.offset { at.round() }.size(1.dp)) { ContextMenu(menu) { menuAt = null } }
        }
    }
}

/** A shape part of the way from one rounded shape to another: each corner's radius between the two. */
private class Between(val from: Shape, val to: Shape, val t: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        fun round(o: Outline): RoundRect? = when (o) { is Outline.Rounded -> o.roundRect; is Outline.Rectangle -> RoundRect(o.rect); else -> null }
        val a = from.createOutline(size, layoutDirection, density)
        val b = round(to.createOutline(size, layoutDirection, density))
        val r = round(a)
        if (r == null || b == null) return a
        fun mix(x: CornerRadius, y: CornerRadius) = CornerRadius(x.x + (y.x - x.x) * t, x.y + (y.y - x.y) * t)
        return Outline.Rounded(RoundRect(r.left, r.top, r.right, r.bottom, mix(r.topLeftCornerRadius, b.topLeftCornerRadius), mix(r.topRightCornerRadius, b.topRightCornerRadius), mix(r.bottomRightCornerRadius, b.bottomRightCornerRadius), mix(r.bottomLeftCornerRadius, b.bottomLeftCornerRadius)))
    }
}

/** Material's rows draw no focus mark of their own (it would be a second one). Menus put Material's back: [Menu]. */
private val NO_RING_THEME = RippleThemeConfiguration(RippleThemeConfiguration.Focus.InsetRing(0.dp, 0.dp, 0.dp, 0.dp))
private val NO_RING = RippleConfiguration(RippleConfiguration.Focus.InsetRing(Color.Transparent, Color.Transparent))

/** A few sentences in a group that are not a row to press: what Booklight keeps, or that a group has nothing in it yet. */
@Composable
fun PageNote(place: Place?, vararg text: String, mark: (@Composable (ink: Color) -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(place?.index ?: 0, place?.count ?: 1),
        leadingContent = mark?.let { m -> { Box(Modifier.size(MARK), contentAlignment = Alignment.Center) { m(LocalContentColor.current) } } },
        verticalAlignment = Alignment.CenterVertically,
        colors = ListItemDefaults.segmentedColors(containerColor = scheme.card, contentColor = scheme.onSurfaceVariant, leadingContentColor = scheme.onSurfaceVariant),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { for (t in text) Text(t, style = MaterialTheme.typography.bodyMedium) }
    }
}

/**
 * Something on a page that is not a row but is a stop for the keys all the same: one of the Commands page's
 * own controls. The keys reach it like a row, Left and Right step it, Enter runs it; the ring is round it,
 * as round as [corner] says.
 */
@Composable
fun Stop(page: Page, key: String, onEnter: () -> Unit, onStep: ((Int) -> Boolean)? = null, corner: Dp = 20.dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val enter by rememberUpdatedState(onEnter)
    val step by rememberUpdatedState(onStep)
    val entry = remember(key) { Page.Entry() }
    entry.enter = { enter() }
    entry.step = if (onStep == null) null else { d -> step?.invoke(d) == true }
    entry.corner = corner
    DisposableEffect(key) {
        page.rows[key] = entry
        onDispose { page.rows.remove(key); if (page.selected == key) page.selected = null }
    }
    Box(modifier.onGloballyPositioned { page.place(entry, it) }) { content() }
}

/**
 * The one focus ring of the window: 2 dp in `secondary`, just inside the edge of what the keys are on. It is
 * drawn over everything, from where its target is in that frame ([rect], in the window): it stays on a row
 * while the page scrolls and on the navigation's indicator while that travels. When the keys go to another
 * stop ([target] changes) it glides there, the edge that leads on the quick spring and the one that follows
 * on the slow one, as the panel's selection does. It shows only while the keys are what moves it ([shown]);
 * it fades where it stands, and comes back where the keys are.
 */
@Composable
fun FocusRing(
    target: Any?, shown: Boolean,
    /** Where the target is now, in the window. */
    rect: () -> Rect?,
    /** How round it is there, in px. */
    corner: () -> Float,
    /** What the ring may be drawn within: a row's ring is cut where the page is cut. */
    within: () -> Rect?,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val color = MaterialTheme.colorScheme.secondary
    // How far each edge still is from the target's own: all 0 at rest.
    val left = remember { Animatable(0f) }
    val top = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    val bottom = remember { Animatable(0f) }
    val round = remember { Animatable(0f) }
    /** Where the ring was drawn last. */
    val last = remember { arrayOfNulls<Rect>(1) }
    val alpha by animateFloatAsState(if (shown && target != null) 1f else 0f, motion.fade(120), label = "ring")
    val now by rememberUpdatedState(rect)
    val roundness by rememberUpdatedState(corner)
    LaunchedEffect(target) {
        val from = last[0]
        val to = now()
        // Nothing to come from, or not seen there: it simply is where the keys are.
        if (target == null || to == null || from == null || alpha == 0f) {
            left.snapTo(0f); top.snapTo(0f); right.snapTo(0f); bottom.snapTo(0f); round.snapTo(roundness())
            return@LaunchedEffect
        }
        left.snapTo(from.left - to.left); top.snapTo(from.top - to.top); right.snapTo(from.right - to.right); bottom.snapTo(from.bottom - to.bottom)
        val forward = to.center.x >= from.center.x
        val down = to.center.y >= from.center.y
        launch { left.animateTo(0f, if (forward) motion.trail() else motion.lead()) }
        launch { right.animateTo(0f, if (forward) motion.lead() else motion.trail()) }
        launch { top.animateTo(0f, if (down) motion.trail() else motion.lead()) }
        launch { bottom.animateTo(0f, if (down) motion.lead() else motion.trail()) }
        launch { round.animateTo(roundness(), motion.place()) }
    }
    // The ring's own box is not the window (the caption bar is above it): what is given in the window is drawn from here.
    var base by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.fillMaxSize().onGloballyPositioned { base = it.positionInRoot() }.drawBehind {
        val at = rect() ?: return@drawBehind
        val r = Rect(at.left + left.value, at.top + top.value, at.right + right.value, at.bottom + bottom.value)
        last[0] = r
        if (alpha <= 0f || r.width <= 0f || r.height <= 0f) return@drawBehind
        val w = 2.dp.toPx()
        val radius = (minOf(round.value, r.height / 2, r.width / 2) - w / 2).coerceAtLeast(0f)
        val cut = within()?.translate(-base.x, -base.y) ?: Rect(0f, 0f, size.width, size.height)
        clipRect(cut.left, cut.top, cut.right, cut.bottom) {
            drawRoundRect(color.copy(alpha = alpha), r.topLeft - base + Offset(w / 2, w / 2), Size(r.width - w, r.height - w), CornerRadius(radius), style = Stroke(w))
        }
    })
}

/** A key cap, as the keys in the text about shortcuts and the keyword of a link: on the window's ground, in a row or a field. */
@Composable
fun Cap(label: String, ink: Color, large: Boolean = false) {
    Box(
        Modifier.defaultMinSize(minWidth = if (large) 32.dp else 28.dp, minHeight = if (large) 28.dp else 24.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.ground).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = ink, style = MaterialTheme.typography.labelMedium.copy(fontFamily = Fonts.text, fontWeight = FontWeight(600)), maxLines = 1) }
}

/** A few key caps in a row, with plus signs between them: Action + K. */
@Composable
fun Keys(vararg keys: String, ink: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.forEachIndexed { i, k ->
            if (i > 0) Text("+", color = ink, style = MaterialTheme.typography.labelMedium)
            Cap(k, MaterialTheme.colorScheme.onSurface, large = true)
        }
    }
}
