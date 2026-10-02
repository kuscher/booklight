package io.github.kuscher.booklight.window

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailItemDefaults
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.launch
import kotlin.math.floor

/**
 * The window's sections, in the order a person meets them: set it up and see what it is, see what it can do
 * and make your own, change how it looks, choose what the list shows, check what it may use and keeps.
 */
enum class Part(val id: String, val symbol: String, val title: Int) {
    START("start", "booklight", R.string.nav_start),
    COMMANDS("commands", "list", R.string.nav_commands),
    LOOK("look", "palette", R.string.nav_look),
    RESULTS("results", "search", R.string.nav_results),
    LABS("labs", "labs", R.string.nav_labs),
    PRIVACY("privacy", "lock", R.string.nav_privacy);

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id }
    }
}

/** The room a section's name keeps to its neighbour's in the bar. */
private val NAME_GAP = 8.dp

/** The rail's two widths (Material's own). */
val RAIL_COLLAPSED = 96.dp
val RAIL_EXPANDED = 220.dp

/**
 * How far the first item is from the rail's top, so that the centre of its mark is [TITLE_LINE] below the
 * caption bar at both widths: the page's title stands on that line. An expanded item is 56 high with its
 * mark in the middle; a collapsed one is 64 high and its mark's centre is 22 from its top.
 */
private val TOP_EXPANDED = TITLE_LINE - 28.dp
private val TOP_COLLAPSED = TITLE_LINE - 22.dp

/**
 * What the rail (or the bar) knows of itself: where Material has put each section's mark and name, in the
 * window, and how far the indicator has travelled. The indicator is drawn from these in the same frame, so
 * it is where the item is while the rail widens or the window is resized; and the window's focus ring is
 * drawn round it from the same numbers ([indicator]).
 */
class NavState(start: Part, private val bar: Boolean) {
    /** How wide the rail is now, in px (it animates between its two widths). */
    var width by mutableIntStateOf(0)
        internal set
    internal val mark = mutableStateMapOf<Part, Rect>()
    internal val name = mutableStateMapOf<Part, Rect>()
    /** The indicator's upper edge (the bar's: its leading one) and its lower edge (the trailing one), counted in items. */
    internal val first = Animatable(start.ordinal.toFloat())
    internal val last = Animatable(start.ordinal.toFloat())

    /**
     * Where the indicator is now, in the window: Material's shape (56 × 32 round the mark; in the expanded rail
     * 56 high round mark and name), between the items its two edges are at.
     */
    fun indicator(density: Density): Rect? = with(density) {
        // How far the rail is between collapsed (0) and expanded (1): Material's items go by the same spring.
        val wide = if (bar) 0f else ((width - RAIL_COLLAPSED.toPx()) / (RAIL_EXPANDED - RAIL_COLLAPSED).toPx()).coerceAtLeast(0f)
        fun rect(p: Part): Rect? {
            val mark = mark[p] ?: return null
            val name = name[p]
            val left = mark.left - 16.dp.toPx()
            // Material's own measures, between its two layouts (NavigationItem.kt, AnimatedMeasurePolicy): the mark is 4
            // below the indicator's top when the name is under it, 16 when the name is beside it, and on the way from
            // one to the other the item's own height (the name's line and 4 above it, going) shifts it a little more.
            val pad = lerp(4.dp.toPx(), 16.dp.toPx(), wide.coerceIn(0f, 1f))
            val top = mark.top - pad - wide * (1f - wide) * (4.dp.toPx() + (name?.height ?: 0f)) / 2
            val width = lerp(mark.width, mark.width + 8.dp.toPx() + (name?.width ?: 0f), wide) + 32.dp.toPx()
            return Rect(left, top, left + width, top + mark.height + 2 * pad)
        }
        val rects = Part.entries.map { rect(it) ?: return null }
        /** An edge at a place between two items; a spring that runs past the last item runs on by the same step. */
        fun edge(at: Float, of: (Rect) -> Float): Float {
            if (rects.size < 2) return of(rects[0])
            val i = floor(at).toInt().coerceIn(0, rects.size - 2)
            return lerp(of(rects[i]), of(rects[i + 1]), at - i)
        }
        val mid = (first.value + last.value) / 2
        if (bar) Rect(edge(first.value) { it.left }, edge(mid) { it.top }, edge(last.value) { it.right }, edge(mid) { it.bottom })
        else Rect(edge(mid) { it.left }, edge(first.value) { it.top }, edge(mid) { it.right }, edge(last.value) { it.bottom })
    }
}

/**
 * The navigation rail on the window's leading edge: Material's wide rail, collapsed (96 dp, each
 * name under its mark) or [expanded] (220 dp, each name beside its mark), standing on the window's
 * ground with no fill of its own. Material widens and narrows it. The indicator is Booklight's: one
 * shape that travels to the open section, its leading edge first, as the selection does in the
 * panel; Material's own, which grows in place, is switched off. The rail is one stop for the keys
 * (the window draws its focus ring round the indicator), so its items take no focus themselves.
 */
@Composable
fun Rail(nav: NavState, current: Part, expanded: Boolean, /** A small dot on Start: no key has opened the panel yet. */ dot: Boolean, onPick: (Part) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val state = rememberWideNavigationRailState(if (expanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed)
    LaunchedEffect(expanded) { if (expanded) state.expand() else state.collapse() }
    // What the rail itself goes by: the items and the top space change in the same frame as its width starts to.
    val wide = state.targetValue == WideNavigationRailValue.Expanded
    val top by animateDpAsState(if (wide) TOP_EXPANDED else TOP_COLLAPSED, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "top")
    Box(modifier.fillMaxHeight()) {
        Indicator(nav, current, Modifier.matchParentSize())
        WideNavigationRail(
            modifier = Modifier.onSizeChanged { nav.width = it.width },
            state = state,
            colors = WideNavigationRailDefaults.colors(containerColor = Color.Transparent, contentColor = scheme.onSurface),
            windowInsets = WindowInsets(0, 0, 0, 0),
            contentPadding = PaddingValues(top = top),
        ) {
            for (p in Part.entries) {
                WideNavigationRailItem(
                    selected = p == current, onClick = { onPick(p) }, railExpanded = wide,
                    icon = { Mark(p, nav, dot) }, label = { Name(p, nav, bar = false) },
                    colors = WideNavigationRailItemDefaults.colors(selectedIndicatorColor = Color.Transparent),
                    modifier = Modifier.focusProperties { canFocus = false },
                )
            }
        }
    }
}

/** Under 600 dp: Material's short navigation bar along the bottom, every section one click away, with the same travelling indicator. */
@Composable
fun Bar(nav: NavState, current: Part, dot: Boolean, onPick: (Part) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Six names stand side by side only where the widest of them has its room in one item (in German "Datenschutz" and
        // "Ergebnisse" need the most). In a narrower window none is written: marks and the indicator only, every mark on one line.
        val measurer = rememberTextMeasurer()
        val style = MaterialTheme.typography.labelMedium
        val names = Part.entries.map { stringResource(it.title) }
        val widest = remember(names, style, density.density, density.fontScale) { names.maxOf { measurer.measure(it, style, maxLines = 1).size.width } }
        val all = with(density) { (maxWidth / Part.entries.size).toPx() } >= widest + with(density) { NAME_GAP.toPx() }
        Indicator(nav, current, Modifier.matchParentSize())
        ShortNavigationBar(containerColor = Color.Transparent, contentColor = scheme.onSurface, windowInsets = WindowInsets(0, 0, 0, 0)) {
            for (p in Part.entries) {
                ShortNavigationBarItem(
                    selected = p == current, onClick = { onPick(p) },
                    icon = { Mark(p, nav, dot) },
                    label = if (all) ({ Name(p, nav, bar = true) }) else null,
                    colors = ShortNavigationBarItemDefaults.colors(selectedIndicatorColor = Color.Transparent),
                    // (Without its name under it, the mark still says which section it is to a screen reader.)
                    modifier = Modifier.focusProperties { canFocus = false }.then(if (all) Modifier else Modifier.semantics { contentDescription = names[p.ordinal] }),
                )
            }
        }
    }
}

/** A section's mark; on Start, a small dot at its upper right while no key has opened the panel. */
@Composable
private fun Mark(p: Part, nav: NavState, dot: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.onGloballyPositioned { nav.mark[p] = Rect(it.positionInRoot(), it.size.toSize()) }) {
        Icon(Symbols.of(p.symbol), null)
        if (p == Part.START) {
            val there by animateFloatAsState(if (dot) 1f else 0f, LocalMotion.current.pop(), label = "dot")
            Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp).size(8.dp)
                .graphicsLayer { scaleX = there.coerceAtLeast(0f); scaleY = there.coerceAtLeast(0f); alpha = there.coerceIn(0f, 1f) }
                .clip(CircleShape).background(scheme.primary))
        }
    }
}

/**
 * A section's name. Under its mark it is in the small label style, beside it in the large one, changing half way
 * as the rail widens. (Material's item is meant to do this itself, but in 1.5.0-alpha29 it remembers which of the
 * two it was first shown as: a rail that starts expanded keeps the large style when it collapses.)
 */
@Composable
private fun Name(p: Part, nav: NavState, bar: Boolean) {
    val density = LocalDensity.current
    val small by remember(bar, density) { derivedStateOf { bar || nav.width < with(density) { ((RAIL_COLLAPSED + RAIL_EXPANDED) / 2).toPx() } } }
    Text(stringResource(p.title), maxLines = 1, softWrap = false, style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
        modifier = Modifier.onGloballyPositioned { nav.name[p] = Rect(it.positionInRoot(), it.size.toSize()) })
}

/**
 * The open section's indicator, in the selection's colour. Its two edges along the navigation's axis
 * ride [io.github.kuscher.booklight.overlay.Motion.lead] and `trail`, counted in items, not in
 * pixels: where an item is comes from Material's layout in every frame, so nothing lags when the
 * window is resized.
 */
@Composable
private fun Indicator(nav: NavState, current: Part, modifier: Modifier) {
    val motion = LocalMotion.current
    val at = current.ordinal.toFloat()
    LaunchedEffect(at) {
        val forward = at > nav.first.targetValue
        launch { nav.first.animateTo(at, if (forward) motion.trail() else motion.lead()) }
        launch { nav.last.animateTo(at, if (forward) motion.lead() else motion.trail()) }
    }
    val fill = MaterialTheme.colorScheme.secondaryContainer
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.onGloballyPositioned { origin = it.positionInRoot() }.drawBehind {
        val r = nav.indicator(this)?.translate(-origin.x, -origin.y) ?: return@drawBehind
        if (r.width <= 0f || r.height <= 0f) return@drawBehind
        drawRoundRect(fill, r.topLeft, r.size, CornerRadius(minOf(r.width, r.height) / 2))
    })
}
