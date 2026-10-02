package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Stop
import io.github.kuscher.booklight.core.Tone
import io.github.kuscher.booklight.ui.FlightColors
import io.github.kuscher.booklight.ui.Fonts

// A flight's row (docs/design/flights-row/design.md). Its measures, with y from the row's top edge: line one 12 to 32,
// the headline 34 to 58 (its centre, 46, is the mark's and the strip's), the line on y = 84 in a band 74 to 94, the
// two ends 100 to 122, and 14 under them: 136. Across, everything hangs on two edges of the panel: the titles' edge
// (72) and the strip's right end (700).

/** Line one's top, its height, and the headline's height: 12 + 20 + 2 + 24 puts the headline's centre on 46. */
private val WORDS_TOP = 12.dp
private val CAPTION = 20.dp
private val HEADLINE = 24.dp
/** The words' box is a little taller than its two lines, so the fade at its end (it cuts to its bounds) never cuts the badge's lower edge. */
private val WORDS = 50.dp
private val BAND_TOP = 74.dp
private val ENDS_TOP = 100.dp
private val ENDS = 22.dp
/** Between the headline's last letter and the badge. */
private val BADGE_GAP = 10.dp
private val BADGE = 22.dp
private val BADGE_EDGE = 9.dp
/** The badge's fill brings its own ground: this much of it covers whatever the glass shows. */
private const val BADGE_FILL = 0.88f
/** Under an end: between the airport's letters and its time, and between the time and the small words. */
private val CODE_GAP = 7.dp
private val WHERE_GAP = 12.dp
/** The two ends never come closer than this: before they do, small words are left out. */
private val ENDS_APART = 24.dp
/** An empty end, before the answer: a short rule where the airport will stand. */
private val RULE = 20.dp
private val RULE_HIGH = 2.dp
private const val RULE_INK = 0.30f
/**
 * The least room a headline has beside the strip (700 less the widest strip, "Anheften" unrolled, less 16, from 72).
 * A reason there is no answer that is wider than this is set in the small type, chosen once for that text.
 */
private val HEADLINE_ROOM = 388.dp
/** The type of what stands under the line's ends: the airport's letters and its time. */
private val END = TextStyle(fontFamily = Fonts.text, fontSize = 17.sp, fontWeight = FontWeight(500), fontFeatureSettings = "tnum")

private val CENTRED = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
private val HEAD = TextStyle(fontFamily = Fonts.text, fontSize = 17.sp, fontWeight = FontWeight(500), fontFeatureSettings = "tnum")
/** The badge's word: the footer hint's size at the weight of its word. */
private val WORD = TextStyle(fontFamily = Fonts.text, fontSize = 13.sp, fontWeight = FontWeight(600), letterSpacing = 0.1.sp)

/**
 * A style whose line is exactly [height] high, the letters in its middle: the row's lines have their places in dp, and
 * a line as high as its place is never cut at its foot, whatever size the system sets its text at.
 */
@Composable
private fun TextStyle.lined(height: Dp): TextStyle = copy(lineHeight = with(LocalDensity.current) { height.toSp() }, lineHeightStyle = CENTRED)

/**
 * What a flight's row holds beside its mark: line one and the headline with its badge, where a row's words always
 * are; under them the flight's line with the plane on it, from the titles' edge to the strip's right end; and under
 * the line's two ends the airport and its time, with what matters at that end in small words. The line and the two
 * ends lie under the strip and never meet it, so they are as long whether the row is selected or not.
 *
 * Everything stands from the row's first frame at the height it keeps. When the answer lands (or the reason there
 * is none) the words change where they stand: the old ones fade out, the new ones in. While a number is still being
 * typed the row stays a waiting one, and its words change in the frame of the key. A minute passing, or another
 * answer about the same flight, changes only what changed: the headline rolls, the badge's colour fades.
 *
 * [ink] is the panel's ink at full strength; [dim] the ink of what stands back a step (line one, the small words):
 * a step lighter, and full on the selection.
 */
@Composable
private fun FlightBody(b: Body.Flight, ink: Color, dim: Color, /** How much of it shows, 0 to 1, while it and the ordinary row's name change places. */ shown: () -> Float) {
    val motion = LocalMotion.current
    val lands: AnimatedContentTransitionScope<*>.() -> ContentTransform = { (fadeIn(motion.fade(110, 40)) togetherWith fadeOut(motion.fade(70))).using(null) }
    // One flight's words are one thing through its answers and its minutes; a row that waits is one whatever is typed.
    val key: (Body.Flight) -> Any = { it.flight ?: it.answer }
    // Each of the three fades as itself, in its own bounds: one fade round all of them would cut what lies under the strip.
    // Beside the strip. Laid out once at their own width and covered by a fade where the strip takes its room.
    AnimatedContent(b, Modifier.padding(start = 16.dp, end = 16.dp, top = WORDS_TOP).fillMaxWidth().height(WORDS).graphicsLayer { alpha = shown() }.fadeEnd(),
        transitionSpec = { lands() }, contentKey = key, contentAlignment = Alignment.TopStart, label = "words") { Words(it, ink, dim) }
    // Under the strip: wider than the room the words have, from this edge to the strip's right end.
    FlightLine(b.share, b.flight, b.answer, ink, LineMeasures.ROW, Modifier.padding(start = 16.dp, top = BAND_TOP).wrapContentWidth(Alignment.Start, unbounded = true).graphicsLayer { alpha = shown() })
    AnimatedContent(b, Modifier.padding(start = 16.dp, top = ENDS_TOP).wrapContentWidth(Alignment.Start, unbounded = true).graphicsLayer { alpha = shown() }.size(LineMeasures.ROW.length, ENDS),
        transitionSpec = { lands() }, contentKey = key, contentAlignment = Alignment.TopStart, label = "ends") { Ends(it, ink, dim, lands) }
}

/**
 * What stands beside the mark in a row of the flights provider: the tall row's body ([b]), or, for a guess nobody has
 * gone to and for a number without a key, the ordinary row's name ([title], [b] null). The two are one row to the
 * list. When the user goes to a guess it becomes a flight's row where it stands: the row grows, once, and uncovers the
 * body with its lower edge, while the name fades out and the body's words in (and the other way when the text
 * turns back into a guess). The name stands on the line the mark and the strip keep to ([line]), so nothing jumps.
 */
@Composable
fun RowScope.FlightSeat(title: String, b: Body.Flight?, line: Dp, ink: Color, dim: Color) {
    val motion = LocalMotion.current
    // The body as it last was: it goes on being drawn while it fades, when the row turns back into its name.
    val last = remember { arrayOfNulls<Body.Flight>(1) }
    if (b != null) last[0] = b
    val tall by animateFloatAsState(if (b != null) 1f else 0f, if (b != null) motion.fade(110, 40) else motion.fade(70), label = "tall")
    val plain by animateFloatAsState(if (b == null) 1f else 0f, if (b == null) motion.fade(110, 40) else motion.fade(70), label = "plain")
    // At its own height from the row's top, whatever height the row has just now: the row's edge uncovers it.
    Box(Modifier.weight(1f).align(Alignment.Top).wrapContentHeight(Alignment.Top, unbounded = true).height(Metrics.flight)) {
        if (b == null || plain > 0f) Box(Modifier.fillMaxWidth().height(line).graphicsLayer { alpha = plain }.padding(start = 16.dp, end = 12.dp), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().fadeEnd()) {
                Text(title, color = ink, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            }
        }
        if (b != null || tall > 0f) last[0]?.let { FlightBody(it, ink, dim) { tall } }
    }
}

/** Line one, then the headline and its badge on one baseline. */
@Composable
private fun Words(b: Body.Flight, ink: Color, dim: Color) {
    val motion = LocalMotion.current
    val measurer = rememberTextMeasurer()
    val room = with(LocalDensity.current) { HEADLINE_ROOM.toPx() }
    // (A reason too long for the headline's room, "Die AirLabs-Abfragen dieses Monats sind aufgebraucht": smaller, not cut.)
    val small = remember(b.headline, b.badge == null, room) { b.badge == null && measurer.measure(b.headline, HEAD, maxLines = 1, softWrap = false).size.width > room }
    val head = HEAD.lined(HEADLINE)
    Column(Modifier.wrapContentWidth(Alignment.Start, unbounded = true)) {
        Text(b.caption.orEmpty(), color = dim, style = SMALL.lined(CAPTION), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        Spacer(Modifier.height(2.dp))
        Row(Modifier.height(HEADLINE)) {
            AnimatedContent(b.headline, transitionSpec = { motion.roll() }, contentAlignment = Alignment.CenterStart, label = "headline") { text ->
                Text(text, color = ink, style = if (small) SMALL.lined(HEADLINE) else head, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            }
            // (What the badge's word is set on: the headline's baseline, from a letter of no width that stands still while a new headline rolls in.)
            Text(" ", style = head, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline().width(0.dp))
            b.badge?.let { Spacer(Modifier.width(BADGE_GAP)); Badge(it, b.tone, ink, Modifier.alignByBaseline()) }
        }
    }
}

/**
 * The one badge beside the headline: whether the flight runs to plan. A small filled shape with the pill's white
 * rim: the only place in a row with a colour of its own, green for on time and early, amber for late, and for
 * "Planned" none (the scope chip's fill). The colour is a fill with its own ink on it, never a coloured word on
 * the glass, so it reads over a light window and a dark one, on bare glass and on the selection.
 */
@Composable
private fun Badge(word: String, tone: Tone, ink: Color, modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val colors = FlightColors.of(dark)
    val fill by animateColorAsState(when (tone) {
        Tone.GOOD -> colors.good.copy(alpha = BADGE_FILL)
        Tone.LATE -> colors.late.copy(alpha = BADGE_FILL)
        Tone.PLAIN -> ink.copy(alpha = if (dark) 0.14f else 0.10f)
    }, motion.fade(120), label = "badge")
    val on by animateColorAsState(when (tone) { Tone.GOOD -> colors.onGood; Tone.LATE -> colors.onLate; Tone.PLAIN -> ink }, motion.fade(120), label = "word")
    val shape = RoundedCornerShape(BADGE / 2)
    Box(
        modifier.height(BADGE).clip(shape).background(fill)
            .border(with(LocalDensity.current) { 1f.toDp() }, Color.White.copy(alpha = if (dark) 0.30f else 0.55f), shape)
            .padding(horizontal = BADGE_EDGE),
        contentAlignment = Alignment.Center,
    ) { Text(word, color = on, style = WORD, maxLines = 1, softWrap = false) }
}

/**
 * Under the line's two ends: at the start the airport's letters, its time and the small words; at the far end the
 * mirror of it, so each airport stands under its end of the line. Nothing is a held place: small words are the
 * last thing at their end and grow towards the middle of the row. Before the answer each end is a short rule.
 */
@Composable
private fun Ends(b: Body.Flight, ink: Color, dim: Color, lands: AnimatedContentTransitionScope<*>.() -> ContentTransform) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val (first, last) = remember(b.from, b.to, density) { with(density) { fitting(b.from, b.to, measurer) { it.toPx() } } }
    val end = END.lined(ENDS)
    Box(Modifier.size(LineMeasures.ROW.length, ENDS)) {
        // (Another answer about the same flight: an end whose time or words changed fades, the other stands.)
        AnimatedContent(b.from to first, Modifier.align(Alignment.CenterStart), transitionSpec = { lands() }, contentAlignment = Alignment.CenterStart, label = "from") { (stop, words) ->
            if (stop == null) Rule(ink) else Row {
                Text(stop.code, color = ink.copy(alpha = ink.alpha * SECOND), style = end, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline())
                Time(stop, ink, end, Modifier.alignByBaseline().padding(start = CODE_GAP))
                words?.let { Text(it, color = dim, style = SMALL, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline().padding(start = WHERE_GAP)) }
            }
        }
        AnimatedContent(b.to to last, Modifier.align(Alignment.CenterEnd), transitionSpec = { lands() }, contentAlignment = Alignment.CenterEnd, label = "to") { (stop, words) ->
            if (stop == null) Rule(ink) else Row {
                words?.let { Text(it, color = dim, style = SMALL, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline().padding(end = WHERE_GAP)) }
                Time(stop, ink, end, Modifier.alignByBaseline().padding(end = CODE_GAP))
                Text(stop.code, color = ink.copy(alpha = ink.alpha * SECOND), style = end, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

/** An end's time: full ink; what will not happen is struck through and a step lighter, as a done task is. */
@Composable
private fun Time(stop: Stop, ink: Color, style: TextStyle, modifier: Modifier) =
    Text(stop.time, color = if (stop.struck) ink.copy(alpha = ink.alpha * SECOND) else ink, style = if (stop.struck) style.copy(textDecoration = TextDecoration.LineThrough) else style,
        maxLines = 1, softWrap = false, modifier = modifier)

@Composable
private fun Rule(ink: Color) = Box(Modifier.size(RULE, RULE_HIGH).clip(RoundedCornerShape(RULE_HIGH / 2)).background(ink.copy(alpha = ink.alpha * RULE_INK)))

/**
 * Which small words each end gets, so that the two ends keep [ENDS_APART] between them: all of them where there is
 * room (there nearly always is); else the start's give way first (the aircraft goes, a gate stays without its
 * terminal), then the far end's (the terminal goes), then whatever is left of either. Chosen once for what the ends
 * say, never while something moves.
 */
private fun fitting(from: Stop?, to: Stop?, measurer: TextMeasurer, px: (Dp) -> Float): Pair<String?, String?> {
    if (from == null || to == null) return from?.words to to?.words
    fun wide(s: Stop, words: String?): Float =
        measurer.measure(s.code, END).size.width + px(CODE_GAP) + measurer.measure(s.time, END).size.width + (words?.let { px(WHERE_GAP) + measurer.measure(it, SMALL).size.width } ?: 0f)
    val room = px(LineMeasures.ROW.length) - px(ENDS_APART)
    return listOf(from.words to to.words, from.brief to to.words, from.brief to to.brief, null to to.brief, null to null).distinct()
        .firstOrNull { (a, c) -> wide(from, a) + wide(to, c) <= room } ?: (null to null)
}
