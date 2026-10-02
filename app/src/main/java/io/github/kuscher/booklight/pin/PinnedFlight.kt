package io.github.kuscher.booklight.pin

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.AirLabs
import io.github.kuscher.booklight.core.FlightEnd
import io.github.kuscher.booklight.core.FlightStatus
import io.github.kuscher.booklight.core.PinState
import io.github.kuscher.booklight.core.Tone
import io.github.kuscher.booklight.overlay.FlightLine
import io.github.kuscher.booklight.overlay.LineMeasures
import io.github.kuscher.booklight.overlay.LocalDark
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.SECOND
import io.github.kuscher.booklight.overlay.SMALL
import io.github.kuscher.booklight.overlay.fadeEnd
import io.github.kuscher.booklight.providers.flightCount
import io.github.kuscher.booklight.providers.flightDay
import io.github.kuscher.booklight.providers.flightVerdict
import io.github.kuscher.booklight.scopes.clock
import io.github.kuscher.booklight.ui.FlightColors
import io.github.kuscher.booklight.ui.Fonts
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

/** After no answer, the first wait before trying once more; it doubles from there, up to [AGAIN_MS]. */
private const val RETRY_MS = 2 * 60_000L
private const val AGAIN_MS = 30 * 60_000L

/** Where the pinned flight's small line begins: with its 20 dp, the figure's 42 and the flight's line under them, the three stand in the window's 118 with 15 over and under. */
private val TOP = 15.dp
/** Between the figure and the flight's line. */
private val LINE_GAP = 8.dp

/**
 * A pinned flight, at the timer's size: a small line (the number, whether it runs to plan, where to
 * go or when it lands), under it the time to go in words, then the time to landing, then "Landed",
 * and under that the flight as a line with the plane on it, as in the panel's row but small. The
 * timer beside it counts minutes and seconds, so this one says hours and minutes ("1 h 07 min"):
 * "1:07" would read as 67 seconds.
 *
 * Whether it runs to plan is one word in a colour (green: on time, early; amber: late): the window's
 * ground is solid, so here a coloured word holds, as it would not on the panel's glass. No airports
 * under the line and no badge: a glance takes the figure, the line and that word.
 *
 * It follows one flight, the one that was pinned: the number on the day it leaves ([p]'s note,
 * "LH455 2026-10-01"), never "the next flight" of that number. It counts by itself, a minute at a
 * time, from what the service last said, and the plane moves with the figure, by the clock; a new
 * answer moves both once. It asks the service again while its window is on screen:
 * the one place where Booklight asks with the panel closed. How often is `FlightStatus.every`: every
 * half hour from three hours before it leaves until it has landed, every three hours before that,
 * and not at all for a plan from the timetable more than ten hours off, nor once it has landed (or
 * was to land more than three hours ago: a pin that slept through the landing says "Landed" and
 * asks nothing). Each ask is one request. When the service answers with another day's flight, the
 * asking ends. With no answer to show (no key, no connection) it is the line that was pinned, over
 * the flight's line at rest.
 */
@Composable
fun PinnedFlight(p: Pinned) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val app = context.applicationContext as BooklightApp
    // (A pin made by an earlier build has the row's id on a first line: the flight's own name is the last.)
    val key = p.note.substringAfterLast('\n')
    var flight by remember(p) { mutableStateOf(app.flights.pinned(key)) }
    var now by remember { mutableStateOf(app.flights.clock(key)) }
    // When the service was last asked; that there is nothing more to ask; and, after no answer, how long to wait before the next try.
    var asked by remember(p) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var done by remember(p) { mutableStateOf(false) }
    var retry by remember(p) { mutableLongStateOf(0L) }
    // Which answer the window shows: another one moves the plane once, the clock in between only by steps.
    var heard by remember(p) { mutableLongStateOf(1L) }
    LaunchedEffect(p) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = app.flights.clock(key)
                val f = flight
                // With nothing to show (the process was made anew), at once. Else as often as the flight is near; null: nothing to ask.
                val gap = if (f == null) 0L else FlightStatus.every(f, now)?.toMillis()
                if (!done && gap != null && SystemClock.elapsedRealtime() - asked >= (if (retry > 0) retry else gap)) {
                    asked = SystemClock.elapsedRealtime()
                    when (val a = app.flights.again(key, f)) {
                        is AirLabs.Again.Is -> { flight = a.flight; heard++; retry = 0 }
                        AirLabs.Again.NotYet -> retry = 0
                        AirLabs.Again.Failed -> retry = (retry * 2).coerceIn(RETRY_MS, AGAIN_MS)
                        // Another day's flight, or none: this one is not asked about again. What it shows goes on by the clock.
                        AirLabs.Again.Gone -> done = true
                    }
                    now = app.flights.clock(key)
                }
                // To the next whole minute: the figure changes with the clock.
                delay(60_000 - System.currentTimeMillis() % 60_000 + 50)
            }
        }
    }
    val ink = scheme.onSurface
    val f = flight
    if (f == null) {
        Column(Modifier.fillMaxSize().padding(horizontal = Pinned.MARGIN.dp).padding(top = TOP)) {
            Box(Modifier.height(64.dp), contentAlignment = Alignment.CenterStart) {
                Text(p.text, color = ink, style = TextStyle(fontFamily = Fonts.text, fontSize = 15.sp, fontWeight = FontWeight(500), lineHeight = 20.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(LINE_GAP))
            FlightLine(null, null, 0, ink, LineMeasures.PIN)
        }
        return
    }
    val pin = FlightStatus.pinned(f, now)
    val today = LocalDate.ofInstant(now, ZoneId.systemDefault())
    fun time(e: FlightEnd, t: java.time.LocalDateTime? = e.time): String = t?.let { listOfNotNull(flightDay(context, it, today), clock(context, it)).joinToString(" ") }.orEmpty()
    val number = f.number.let { if (it.length > 2) it.take(2) + " " + it.drop(2) else it }
    // Whether it runs to plan, as the row's badge says it, here a word in the line. None while only the plan is known, and for a flight that will not happen.
    val badge = FlightStatus.badge(f, now).takeIf { pin.state != PinState.CANCELLED }
    val verdict = badge?.let { flightVerdict(context, it) }
    val parts = when {
        pin.state == PinState.CANCELLED -> listOf(number, stringResource(R.string.pin_flight_was, time(f.from, f.from.planned)))
        // With a verdict: the number, the verdict, and the one thing the figure cannot tell: the gate while it has not left, the time of landing after.
        verdict != null -> listOf(number, verdict, if (pin.state == PinState.BEFORE) f.from.gate?.let { stringResource(R.string.pin_flight_gate, it) } ?: time(f.from) else time(f.to))
        // Without one, as before. Before it leaves: where to go, and when. In the air: when it lands. Landed: when it did, and where to meet it.
        pin.state == PinState.BEFORE -> listOfNotNull(number, f.from.gate?.let { stringResource(R.string.pin_flight_gate, it) } ?: f.from.terminal?.let { stringResource(R.string.flight_terminal, it) }, time(f.from))
        pin.state == PinState.IN_AIR -> listOf(number, stringResource(R.string.pin_flight_lands, time(f.to)))
        else -> listOfNotNull(number, time(f.to), f.to.terminal?.let { stringResource(R.string.flight_terminal, it) })
    }.filter { it.isNotEmpty() }
    val colors = FlightColors.of(LocalDark.current)
    val colour = if (badge?.tone == Tone.GOOD) colors.goodWord else colors.lateWord
    fun line(parts: List<String>): AnnotatedString = buildAnnotatedString {
        parts.forEachIndexed { i, part ->
            if (i > 0) append(" · ")
            if (part === verdict) withStyle(SpanStyle(color = colour)) { append(part) } else append(part)
        }
    }
    // If the three do not fit the window's width, the last is left out: never half a word behind an ellipsis.
    val measurer = rememberTextMeasurer()
    val room = with(LocalDensity.current) { (Pinned.WIDTH - 2 * Pinned.MARGIN).dp.toPx() }
    val line = remember(parts, verdict, colour, room) { line(parts).takeIf { parts.size < 3 || measurer.measure(it, SMALL, maxLines = 1, softWrap = false).size.width <= room } ?: line(parts.dropLast(1)) }
    val figure = when (pin.state) {
        PinState.LANDED -> stringResource(R.string.flight_landed)
        PinState.CANCELLED -> stringResource(R.string.flight_cancelled)
        else -> flightCount(context, pin.minutes)
    }
    // The plane's place, by the clock; it only goes forward (a later estimate leaves it where it is until the clock has caught up).
    val place = remember(p) { arrayOfNulls<Double>(1) }
    val share = FlightStatus.forward(place[0], FlightStatus.pinShare(f, now)).also { if (it != null) place[0] = it }
    Column(Modifier.fillMaxSize().padding(horizontal = Pinned.MARGIN.dp).padding(top = TOP)) {
        Text(line, color = ink.copy(alpha = SECOND), style = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(20.dp))
        Spacer(Modifier.height(2.dp))
        // The figure changes once a minute: the new one rolls into place, as a changed answer does in the panel.
        Box(Modifier.height(42.dp).fillMaxWidth().fadeEnd(), contentAlignment = Alignment.CenterStart) {
            AnimatedContent(figure, transitionSpec = { motion.roll() }, contentAlignment = Alignment.CenterStart, label = "figure") { text ->
                Text(text, color = ink, style = TextStyle(fontFamily = Fonts.round, fontSize = if (text.length > 12) 28.sp else 34.sp, fontWeight = FontWeight(600), fontFeatureSettings = "tnum"), maxLines = 1, softWrap = false)
            }
        }
        Spacer(Modifier.height(LINE_GAP))
        FlightLine(share?.toFloat(), key, heard, ink, LineMeasures.PIN)
    }
}
