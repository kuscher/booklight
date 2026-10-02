package io.github.kuscher.booklight.window

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * How wide the Booklight window is, in Material's classes, and what follows from it
 * (docs/design/window-redesign.md §4): under 600 dp a navigation bar at the bottom and margins of
 * 16; from 600 a rail of 96 on the leading edge and margins of 24; from 840 the rail is 220 wide
 * with each name beside its mark. The page's column starts at the margin and is never centred.
 */
enum class Width {
    COMPACT, MEDIUM, EXPANDED;

    /** The space between the navigation (or the window's edge) and the column, and after the column. */
    val margin: Dp get() = if (this == COMPACT) 16.dp else 24.dp
}

/** The window's class now. It follows the window as it is resized. */
@Composable
fun windowWidth(): Width {
    val size = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass
    return when {
        size.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> Width.EXPANDED
        size.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> Width.MEDIUM
        else -> Width.COMPACT
    }
}

/**
 * How far below the caption bar the centre line of a page's title is. The rail's first mark has its
 * centre on the same line at both of its widths.
 */
val TITLE_LINE = 40.dp

/** The widest the page's column gets: the panel's own width. */
val COLUMN = 720.dp

/** A row at least this wide has its choice beside its text; a narrower one has it under the text. */
val ROW_WIDE = 560.dp

/**
 * The second pane: beside the column, 24 dp from it and 24 from the window's edge, from 320 to 560 dp wide. It
 * is there once a full column and 320 dp fit beside the expanded rail (a window of 1332 dp), so the column
 * never shrinks to make room for it.
 */
val PANE_GAP = 24.dp
val PANE_MIN = 320.dp
val PANE_MAX = 560.dp

/** The second pane is there: what it shows (the demo, the preview, an editor) is not in the column as well. */
/**
 * The window has had its size for a moment. A new window is laid out once at the size the system first gives it
 * (wider than 1332 dp on the Lenovo) and at its own a frame later: until it has settled, what changes place
 * between the column and the second pane does so at once, without its motion.
 */
val LocalSettled = compositionLocalOf { true }

/** How high the pane is that the page scrolls in: the window less its caption bar, and less the bar under a narrow one. */
val LocalPaneHeight = compositionLocalOf { 800.dp }

val LocalSecond = compositionLocalOf { false }

/** How wide the page's column is now: rows decide by it where their controls stand. */
val LocalColumn = compositionLocalOf { COLUMN }

/**
 * The window's two surfaces (docs/design/window-redesign.md §5): the ground, and what stands on it as a
 * surface of its own (a row, an editor, a field). `surfaceBright` is the one role that is lighter than
 * `surfaceContainer` in both themes; Material's own choice for segmented rows, `surface`, is darker than the
 * ground in dark.
 */
val ColorScheme.ground: Color get() = surfaceContainer
val ColorScheme.card: Color get() = surfaceBright
