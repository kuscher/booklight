package io.github.kuscher.booklight.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The measures of a flight's line. [length] from end to end; [band] the height it is drawn in (the plane's wings);
 * [stroke] the flown part's thickness and a dot's size; [dots] how many dots stand along it, the first and the last
 * exactly on its ends; [plane] the plane's size against its symbol's own 24 dp (it is 20 long at 1); [gap] between
 * the flown part's end and the plane's tail.
 */
class LineMeasures(val length: Dp, val band: Dp, val stroke: Dp, val dots: Int, val plane: Float, val gap: Dp) {
    companion object {
        /** In the panel: from the titles' edge to the strip's right end, 70 dots at a pitch of 9.06 dp. */
        val ROW = LineMeasures(628.dp, 20.dp, 3.dp, 70, 1f, 4.dp)
        /** In the pinned window: the content column's 240 dp, 35 dots at a pitch of 7 dp, the plane 16 dp long. */
        val PIN = LineMeasures(240.dp, 16.dp, 2.dp, 35, 0.8f, 3.dp)
    }
}

/** The plane's symbol is 20 of its 24 units long (2 to 22), and its body runs along 11.5, not 12: it is placed by these, not by its box. */
private const val PLANE_FROM = 2f
private const val PLANE_AXIS = 11.5f
private val PLANE_LONG = 20.dp
/** The dots ahead of the plane come into view over [SOFT], beginning [AHEAD] before its nose: a soft edge, so no dot is ever cut in half. */
private val AHEAD = 2.dp
private val SOFT = 8.dp
/** The flown part comes in as the plane leaves the start: nothing of it at 7 dp, all of it at 10. */
private val FLOWN_FROM = 7.dp
private val FLOWN_OVER = 3.dp
/** A step the clock makes is small (a minute of an hour's flight is a sixtieth of the line): only up to here is a move on the same answer taken for one. */
private const val STEP = 0.06f
/** The line's dots at rest (no plane), and the dots still to go, as strengths of the ink: light theme, dark theme. */
private const val REST = 0.30f
private const val REST_DARK = 0.34f
private const val TO_GO = 0.50f
private const val TO_GO_DARK = 0.55f

/**
 * A flight as a line from take-off to landing (docs/design/flights-row/design.md): the part that is flown solid,
 * the rest dotted, and the plane on it, the app's own `plane` symbol turned to point along the line. All of it in
 * the row's [ink]: flown and still to go differ by solid against dotted, by full ink against half, and by the plane
 * between them, never by a hue.
 *
 * [share]: how much of the flying time has passed, 0 to 1; the plane's tail stands at that share of the way its
 * tail can go. Null: nobody knows where it is; the line is its dots at rest and has no plane. One number places the
 * plane, ends the flown part and uncovers the dots ahead, so the three cannot come apart.
 *
 * Motion. When an answer lands the plane comes in at the start and travels to its place once, the flown part
 * growing behind it (`Motion.flies`). After that it only steps with the clock (`Motion.tick`), and another answer
 * about the same flight ([flight], [answer]) moves it once more, forwards only. A plane that goes (the number is
 * typed on, another flight) fades where it stands: it never flies back. Nothing here runs without a cause: between
 * two of these the line is still. It is all drawn in the draw phase, so the travel lays nothing out.
 */
@Composable
fun FlightLine(share: Float?, flight: String?, answer: Long, ink: Color, m: LineMeasures, modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    val dark = LocalDark.current
    /** Where the plane is, as a share of its way, and how much it is there. */
    val at = remember { Animatable(0f) }
    val there = remember { Animatable(0f) }
    /** Whose plane was last put on the line, and by which answer. */
    val was = remember { arrayOfNulls<Any>(2) }
    LaunchedEffect(share, flight, answer) {
        if (share == null) { there.animateTo(0f, motion.fade(110)); return@LaunchedEffect }
        val mine = was[0] == flight
        val again = mine && was[1] == answer
        // Another flight's plane goes where it stands before this one comes in at the start. (Whose plane it is, is noted
        // only once that one has gone: asked again in the middle of its going, it still goes.)
        if (!mine && there.value > 0f) there.animateTo(0f, motion.fade(110))
        was[0] = flight; was[1] = answer
        if (there.value == 0f) {
            at.snapTo(0f)
            launch { there.animateTo(1f, motion.fade(110)) }
            at.animateTo(share, motion.flies(share, first = true))
        } else {
            launch { there.animateTo(1f, motion.fade(110)) }      // (it was on its way out: it stays)
            val way = share - at.value
            // Forwards only. A place behind it is never gone to: whoever says where it is holds it until the clock has caught up.
            if (way > 0f) at.animateTo(share, if (again && way <= STEP) motion.tick() else motion.flies(way))
        }
    }
    val plane = rememberVectorPainter(Symbols.of("plane"))
    val rest = if (dark) REST_DARK else REST
    val toGo = if (dark) TO_GO_DARK else TO_GO
    Canvas(modifier.size(m.length, m.band)) {
        val mid = size.height / 2
        val w = m.stroke.toPx()
        val long = PLANE_LONG.toPx() * m.plane
        val on = there.value.coerceIn(0f, 1f)
        /** The plane's tail: from the line's start to where its nose stands on the line's end. */
        val x = at.value.coerceIn(0f, 1f) * (size.width - long)
        val from = x + long + AHEAD.toPx()
        val soft = SOFT.toPx()
        val pitch = (size.width - w) / (m.dots - 1)
        for (i in 0 until m.dots) {
            val cx = w / 2 + i * pitch
            val ahead = ((cx - from) / soft).coerceIn(0f, 1f)
            // At rest every dot, a step lighter. With a plane only those ahead of it: what is behind it is the flown part.
            val a = rest * (1f - on) + toGo * on * ahead
            if (a > 0f) drawCircle(ink.copy(alpha = ink.alpha * a), w / 2, Offset(cx, mid))
        }
        if (on <= 0f) return@Canvas
        val end = x - m.gap.toPx()
        val flown = on * ((x - FLOWN_FROM.toPx()) / FLOWN_OVER.toPx()).coerceIn(0f, 1f)
        if (flown > 0f && end > w) drawLine(ink.copy(alpha = ink.alpha * flown), Offset(w / 2, mid), Offset(end - w / 2, mid), w, StrokeCap.Round)
        // The symbol points up: turned a quarter it points along the line, its tail on x and its body on the line's axis.
        val box = 24.dp.toPx() * m.plane
        val unit = box / 24f
        // (On whole pixels: the symbol is drawn from a picture of itself, which is soft between two.)
        translate((x - PLANE_FROM * unit).roundToInt().toFloat(), (mid - PLANE_AXIS * unit).roundToInt().toFloat()) {
            rotate(90f, Offset(box / 2, box / 2)) { with(plane) { draw(Size(box, box), alpha = on, colorFilter = ColorFilter.tint(ink)) } }
        }
    }
}
