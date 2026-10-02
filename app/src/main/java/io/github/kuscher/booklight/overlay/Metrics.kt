package io.github.kuscher.booklight.overlay

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Result

/** Text on glass is one ink, `onSurface`, at three strengths: an alpha ink follows whatever shows through, a fixed grey would vanish on grey glass. */
const val SECOND = 0.80f
const val THIRD = 0.60f
val LocalDark = staticCompositionLocalOf { false }
/** True where what is drawn lies on see-through glass (the panel with the window's blur behind it); false on solid ground (the Booklight window, the Solid setting). */
val LocalGlass = staticCompositionLocalOf { false }

/** The panel's sizes. The window is exactly this big, so its blur follows the panel. */
object Metrics {
    val width = 720.dp
    val field = 68.dp
    val row = 56.dp
    /** An answer or a preview: two or three lines. */
    val tall = 92.dp
    /**
     * A flight's row, in every state from its first frame: line one and the headline on the tall row's centre line, and
     * under them the flight's line and its two ends (docs/design/flights-row/design.md).
     */
    val flight = 136.dp
    /** A row that holds a picture (a QR code). */
    val picture = 208.dp
    val cell = 48.dp
    val card = 96.dp
    val pad = 8.dp
    val footer = 36.dp
    val radius = 32.dp
    /** The least room a row's title has beside the widest strip: a title that names an app and does not fit in it says less ([Result.brief]). */
    val title = 340.dp
    /** Where the panel's top edge sits, as a share of the screen's height: the field stays put while the list grows down. */
    const val TOP = 0.2f

    /** A line of an answer. A row holds two, and four once it has grown. */
    val streamLine = 22.dp

    /** One of an opened row's other actions: the strip's slot as a line of its own. */
    val action = 40.dp

    fun rowHeight(r: Result): Dp = when (val b = r.body) {
        is Body.Grid -> cell * gridRows(b) + 16.dp
        is Body.Code -> picture
        is Body.Slots, is Body.Mono -> tall
        is Body.Flight -> flight
        is Body.Stream -> if (b.tall) tall + streamLine * 2 else tall
        else -> if (r.kind == Kind.ACTION) action else if (r.answer != null) tall else row
    }

    /** The space above a row: what removes something stands a little apart from the actions before it. */
    fun gap(r: Result): Dp = if (r.kind == Kind.ACTION && r.actions.firstOrNull()?.danger == true) 8.dp else 0.dp

    /** Where each row's top edge is in the list. */
    fun tops(rows: List<Result>): List<Dp> {
        var y = 0.dp
        return rows.map { r -> y += gap(r); val top = y; y += rowHeight(r); top }
    }

    /** How many lines of cells a grid shows: what it has, five at most. */
    fun gridRows(g: Body.Grid): Int = ((g.cells.size + g.columns - 1) / g.columns).coerceIn(1, 5)

    fun listHeight(rows: List<Result>): Dp = rows.fold(0.dp) { h, r -> h + gap(r) + rowHeight(r) }

    /** The panel's height for what the model is showing. Must match what [Panel] draws. */
    fun height(m: OverlayModel): Dp = field + when {
        m.results.isNotEmpty() -> pad + listHeight(m.results) + pad + footer
        m.card != null || m.tip != null -> card + pad
        m.copy != null -> pad + row + pad
        // Under the empty field of an app's chip: the line that offers the app's other action.
        m.otherAct != null -> pad + row + pad
        else -> 0.dp
    }

    /**
     * How many rows fit under the field on a screen this tall, so the panel never runs off it. Counted as ordinary rows,
     * with room kept for one of them to be the tallest a list can hold: a flight's, or an answer grown to four lines,
     * both 80 dp more than a row. (Without it eight rows with a flight among them ran past the lower edge of a screen
     * under 880 dp high; and a guess that becomes a flight's row when the user goes to it grows where it stands.)
     */
    fun maxRows(screenHeightDp: Float): Int {
        val room = screenHeightDp * (1 - TOP) - 56 - field.value - pad.value * 2 - footer.value - (flight - row).value
        return (room / row.value).toInt().coerceIn(3, 8)
    }
}
