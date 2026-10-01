package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import kotlinx.coroutines.flow.first
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** How the panel arrives. [slow] stretches every time in it: 1 = as designed, 2 and 4 for people who want to watch it. */
data class Arrival(val unfold: Boolean, val slow: Float) {
    /** How long the panel takes to leave, for the activity to wait before finishing. Folding is unfolding backwards, and takes as long. */
    val leaveMs: Long get() = if (unfold) ((FOLD_MS + WAIT_MS) * slow).toLong() + 20 else Motion.LEAVE_MS

    companion object {
        /** From the setting: `off` is 1.0's small settle and fade; `fast`, `medium` and `slow` unfold. */
        fun of(setting: String, motion: Motion) = Arrival(motion.on && setting != "off", when (setting) { "medium" -> 2f; "slow" -> 4f; else -> 1f })

        /** The unfold's times before [slow]: the seam grows for [SEAM_MS]; the glass starts to open [WAIT_MS] in and first reaches its width [FOLD_MS] later. */
        const val SEAM_MS = 80
        const val WAIT_MS = 50
        const val FOLD_MS = 105
        /** The last of the seam fades as it goes, so nothing is switched off. */
        const val GONE_MS = 40
    }
}

/** A spring that would pass its end turns back instead, like something landing: nothing may be drawn outside the window. */
private fun landed(v: Float) = 1f - abs(1f - v)
private fun smooth(a: Float, b: Float, x: Float): Float { val t = ((x - a) / (b - a)).coerceIn(0f, 1f); return t * t * (3f - 2f * t) }

/** The seam the glass opens out of, as a width. */
private val SEAM = 6.dp
private val SEAM_GROWS = CubicBezierEasing(0.2f, 0f, 0f, 1f)
/** [SEAM_GROWS] run backwards. */
private val SEAM_DRAWS_IN = CubicBezierEasing(1f, 0f, 0.8f, 1f)
/** The glass closing: the opening spring's way from the seam to full width, backwards (it lands softly on the seam; the bounce is left out). */
private val FOLDS = CubicBezierEasing(0.45f, 0f, 0.4f, 1f)

@Composable
fun Panel(
    model: OverlayModel,
    icons: AppIcons,
    /** True when the window is blurring what's behind it: the surface can be see-through. */
    glass: Boolean,
    dark: Boolean,
    arrival: Arrival,
    /** True once the panel is on its way out: it folds away (or fades) and the activity finishes after [Arrival.leaveMs]. */
    leaving: Boolean,
    onHeight: (Dp) -> Unit,
    /** How far the room has dimmed and how far the glass has come into focus, each 0 to 1: the window's dim and blur follow. */
    onPresence: (dim: Float, blur: Float) -> Unit,
    /** Where the glass stands in the window, in pixels, and how far it has arrived (0 to 1): its shadow follows it. */
    onGlass: (left: Int, top: Int, right: Int, bottom: Int, shown: Float) -> Unit,
    /** Runs an action; [stay]: Shift was held, keep the panel open. */
    onRun: (Result, Action, stay: Boolean) -> Unit,
    onCard: (Card, primary: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val density = LocalDensity.current
    val focus = remember { FocusRequester() }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var cardChoice by remember { mutableIntStateOf(0) }
    // Text set by the model (a scope entered, the last text restored, the debug hook) lands in the field too.
    LaunchedEffect(model.query) { if (field.text != model.query) field = TextFieldValue(model.query, TextRange(model.query.length)) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(model.card) { cardChoice = 0 }

    // The window follows the panel on a spring: taller as rows arrive, never moving its top edge. The opening itself is
    // always the field's height: whatever is under the field (a card, rows for text another app handed over) arrives
    // once the glass is nearly open, never as part of the opening.
    var gate by remember { mutableStateOf(!arrival.unfold) }
    val height by animateDpAsState(if (gate) Metrics.height(model) else Metrics.field, motion.place(), label = "height")
    LaunchedEffect(Unit) { snapshotFlow { height }.collect { onHeight(it) } }

    // Arriving. Unfold: a seam of outline grows up and down, then the glass opens out of it to both
    // sides. The window is the panel's final rectangle throughout (its blur is the whole window, and
    // resizing a window in width is neither smooth nor symmetric): only the glass inside it grows,
    // and the contents, laid out once at full width, are uncovered.
    val slow = arrival.slow
    val w0 = SEAM.value / Metrics.width.value
    val wide = remember { Animatable(if (arrival.unfold) w0 else 1f) }
    val high = remember { Animatable(if (arrival.unfold) 0.1f else 1f) }
    val presence = remember { Animatable(if (motion.on && !arrival.unfold) 0f else 1f) }
    val scale = remember { Animatable(if (motion.on && !arrival.unfold) 0.96f else 1f) }
    fun opened() = if (arrival.unfold) ((landed(wide.value) - w0) / (1f - w0)).coerceIn(0f, 1f) else 1f
    LaunchedEffect(Unit) { if (!gate) { snapshotFlow { opened() >= Motion.GATE }.first { it }; gate = true } }
    // A tip comes only after the opening, and only if nothing has been typed for a moment: the opening is the same every time.
    LaunchedEffect(gate) { if (gate) { delay(TIP_AFTER_MS); model.offerTip() } }
    // The seam the glass grows out of, and draws back into, lies on the field's centre line whatever the panel's height.
    val seamPx = with(density) { Metrics.field.toPx() * 0.1f }
    val fieldPx = with(density) { Metrics.field.toPx() }
    fun unrolled() = ((landed(high.value) - 0.1f) / 0.9f).coerceIn(0f, 1f)
    fun glassTop() = ((fieldPx - seamPx) / 2f * (1f - unrolled())).roundToInt()
    // On the way out the blur and the contents let go early: the glass closes slowly at first, and a
    // blurred band would stand beside it for those frames.
    var folding by remember { mutableStateOf(false) }
    fun focused() = if (folding) smooth(0.75f, 1f, opened()) else smooth(0.5f, 1f, opened())
    fun shown() = if (folding) smooth(0.55f, 0.95f, opened()) else smooth(0.35f, 0.85f, opened())
    LaunchedEffect(leaving) {
        if (!arrival.unfold) {
            launch { presence.animateTo(if (leaving) 0f else 1f, motion.fade(if (leaving) 90 else 170)) }
            launch { scale.animateTo(if (leaving) 0.975f else 1f, if (leaving) motion.fade(100) else motion.pop()) }
        } else if (!leaving) {
            // Also the way back, when the key is pressed again while the panel is leaving: it opens from wherever it had got to.
            val fromSeam = high.value < 1f
            launch { presence.animateTo(1f, motion.fade((Arrival.GONE_MS * slow).toInt())) }
            launch { high.animateTo(1f, motion.fade((Arrival.SEAM_MS * slow).toInt(), easing = SEAM_GROWS)) }
            if (fromSeam) delay(motion.hold((Arrival.WAIT_MS * slow).toLong()))
            wide.animateTo(1f, motion.open(slow))
            folding = false
        } else {
            folding = true
            // Leaving is arriving backwards: the glass closes to its seam from both sides and covers the contents,
            // the blur lets go on the way, then the seam draws in and is gone.
            wide.snapTo(landed(wide.value))
            launch { wide.animateTo(w0, motion.fade((Arrival.FOLD_MS * slow).toInt(), easing = FOLDS)) }
            delay(motion.hold(((Arrival.FOLD_MS + Arrival.WAIT_MS - Arrival.SEAM_MS) * slow).toLong()))
            launch { high.animateTo(0.1f, motion.fade((Arrival.SEAM_MS * slow).toInt(), easing = SEAM_DRAWS_IN)) }
            delay(motion.hold(((Arrival.SEAM_MS - Arrival.GONE_MS) * slow).toLong()))
            presence.animateTo(0f, motion.fade((Arrival.GONE_MS * slow).toInt(), easing = LinearEasing))
        }
    }
    // The blur covers the whole window, so it waits until the glass is more than half open.
    LaunchedEffect(Unit) { snapshotFlow { presence.value * opened() to presence.value * focused() }.collect { (dim, blur) -> onPresence(dim, blur) } }

    // Once the glass is open, one light runs once around the outline.
    val run = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!motion.on) return@LaunchedEffect
        delay(motion.hold(((if (arrival.unfold) 190 else 70) * slow).toLong()))
        run.animateTo(1.15f, motion.fade((1100 * slow).toInt(), easing = CubicBezierEasing(0.3f, 0f, 0.2f, 1f)))
    }

    // While the device's own model works on an answer the light runs again, slowly. When the first word lands it
    // finishes its lap and is gone: it is never switched off where it stands.
    LaunchedEffect(model.thinking) {
        if (!motion.on) return@LaunchedEffect
        fun left() = (1.15f - run.value) / 1.15f
        if (model.thinking) while (true) {
            if (run.value >= 1.15f) run.snapTo(0f)
            run.animateTo(1.15f, motion.fade((2400 * left()).toInt().coerceAtLeast(1), easing = LinearEasing))
        } else if (run.value > 0f && run.value < 1.15f) {
            run.animateTo(1.15f, motion.fade((700 * left()).toInt().coerceAtLeast(1), easing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)))
        }
    }

    fun go(r: Result, a: Action, stay: Boolean) = onRun(r, a, stay)

    fun keys(e: KeyEvent): Boolean {
        if (e.type != KeyEventType.KeyDown) return false
        val card = model.card
        val atEnd = field.selection.collapsed && field.selection.end == field.text.length
        val enter = e.key == Key.Enter || e.key == Key.NumPadEnter
        // A key that is held repeats. Whatever runs something takes one press for one run: a held Enter must not confirm its own delete.
        val again = e.nativeKeyEvent.repeatCount > 0
        val bare = !e.isShiftPressed && !e.isCtrlPressed && !e.isAltPressed && !e.isMetaPressed
        // A waiting confirmation is cancelled by any key but Enter; Esc then only cancels.
        if (!enter && model.cancelConfirm() && e.key == Key.Escape) return true
        val r = model.current
        val entersScope = r?.actions?.getOrNull(model.armed)?.effect is Effect.EnterScope
        return when (e.key) {
            Key.DirectionDown -> { if (!model.moveCell(0, 1)) model.move(1); true }
            Key.DirectionUp -> { if (!model.moveCell(0, -1) && !model.restoreLast()) model.move(-1); true }
            Key.Enter, Key.NumPadEnter -> {
                if (!again) { if (card != null) onCard(card, cardChoice == 0) else if (model.tip != null) model.tipEnter() else model.enter { row, a -> go(row, a, e.isShiftPressed) } }
                true
            }
            Key.Tab -> {
                when {
                    card != null -> if (!again) cardChoice = 1 - cardChoice
                    model.tip != null -> if (!again) model.tipTab()
                    // The text is exactly a keyword and nothing has been moved: Tab makes it the chip. Text and chip change in one frame.
                    model.keyword != null && !e.isShiftPressed -> if (!again && model.enterKeyword()) field = TextFieldValue("")
                    entersScope && !e.isShiftPressed -> if (!again) model.fill()
                    // A row is open: Tab is the next of its actions, wrapping inside them.
                    model.opened != null -> model.step(if (e.isShiftPressed) -1 else 1)
                    // Along the row's stops, wrapping. It stops on the arrow like on any other; only Enter opens it.
                    else -> model.arm(if (e.isShiftPressed) -1 else 1, wrap = true)
                }
                true
            }
            // Right at the end of the text and Left go along what the row offers: a grid's cells, a level, its actions.
            // With a modifier held they are the text's own (select, a word back).
            Key.DirectionRight -> when {
                !atEnd || !bare -> false
                r?.body is Body.Grid -> model.moveCell(1, 0)
                model.nudge(1) -> true
                entersScope -> { if (!again) model.fill(); true }
                model.opened != null -> true                                  // nowhere to go from an action's row
                // On the arrow, Right again opens the row (a press of its own: a held key stops on the arrow).
                model.onMore -> { if (!again) model.open(); true }
                else -> model.arm(1, wrap = false)
            }
            Key.DirectionLeft -> when {
                !bare -> false
                model.opened != null -> { if (!again) model.close(); true }   // Left closes an opened row
                r?.body is Body.Grid -> atEnd && model.moveCell(-1, 0)
                atEnd && model.nudge(-1) -> true
                else -> model.arm(-1, wrap = false)     // on the first action Left is the caret's again
            }
            Key.Backspace -> field.text.isEmpty() && model.leaveScope()
            Key.Escape -> { onClose(); true }
            else -> {
                // Ctrl+1…9 runs that row's first action straight away (never one that removes something: those are never first).
                val n = DIGITS.indexOf(e.key)
                if (n >= 0 && e.isCtrlPressed && !e.isAltPressed && !e.isMetaPressed) {
                    if (!again) model.runRow(n) { row, a -> go(row, a, false) }
                    true
                } else false
            }
        }
    }

    val radiusPx = with(density) { Metrics.radius.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize().onPreviewKeyEvent(::keys)) {
        val full = maxWidth
        val fullPx = constraints.maxWidth
        // The same arithmetic as the glass's own layout below: its shadow is cast from where it is drawn.
        LaunchedEffect(fullPx) {
            snapshotFlow {
                val h = with(density) { height.roundToPx() }
                val w = (fullPx * landed(wide.value)).roundToInt().coerceIn(1, fullPx.coerceAtLeast(1))
                val u = unrolled()
                val hh = (h * u + seamPx * (1f - u)).roundToInt().coerceIn(1, h.coerceAtLeast(1))
                val top = glassTop().coerceAtMost((h - hh).coerceAtLeast(0))
                listOf((fullPx - w) / 2, top, (fullPx - w) / 2 + w, top + hh, (presence.value * opened() * 1000).roundToInt())
            }.collect { (l, t, r, b, a) -> onGlass(l, t, r, b, a / 1000f) }
        }
        Box(
            Modifier
                // The glass: as wide and as high as the arrival has opened it, centred on the seam.
                .layout { measurable, constraints ->
                    val full = constraints.maxWidth
                    val h = height.roundToPx().coerceAtMost(constraints.maxHeight)
                    val w = (full * landed(wide.value)).roundToInt().coerceIn(1, full.coerceAtLeast(1))
                    val u = unrolled()
                    val hh = (h * u + seamPx * (1f - u)).roundToInt().coerceIn(1, h.coerceAtLeast(1))
                    val p = measurable.measure(Constraints.fixed(w, hh))
                    layout(full, constraints.maxHeight) { p.place((full - w) / 2, glassTop().coerceAtMost((h - hh).coerceAtLeast(0))) }
                }
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = presence.value }
                .glass(
                    // The veil is white in light theme and near-black in dark: the most contrast for the least tint.
                    tint = if (glass) scheme.surfaceContainerLowest.copy(alpha = if (dark) Look.tintDark else Look.tintLight) else scheme.surfaceContainerHigh,
                    lead = scheme.tertiary, tail = scheme.primary,
                    radiusPx = radiusPx, density = density.density,
                    run = { run.value }, glow = { smooth(0f, 0.06f, run.value) * (1f - smooth(0.92f, 1.12f, run.value)) },
                    dark = dark, solid = !glass,
                )
                .clip(RoundedCornerShape(Metrics.radius)),
        ) {
            CompositionLocalProvider(LocalDark provides dark) {
                // Laid out once at the panel's width and only uncovered: it never moves on screen while the glass grows.
                Column(
                    Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).requiredWidth(full)
                        .offset { IntOffset(0, -glassTop()) }
                        .graphicsLayer { alpha = shown() },
                ) {
                    Field(model, field, focus = focus, onChange = { v ->
                        model.type(v.text)
                        field = if (model.query == v.text) v else TextFieldValue(model.query, TextRange(model.query.length))
                    })

                    val body = when {
                        model.results.isNotEmpty() -> "results"
                        model.card != null -> "card:${model.card}"
                        model.tip != null -> "tip"
                        else -> "none"
                    }
                    val rows = body == "results"
                    val footerPx = with(density) { (Metrics.footer + Metrics.pad).roundToPx() }
                    // What is under the field shows as far as the window has grown; the footer rides the window's bottom
                    // edge, so it is never left behind by a list that grows and never lies over a row.
                    Box(
                        Modifier.fillMaxWidth().clipToBounds().layout { measurable, constraints ->
                            val p = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                            val room = (height.roundToPx() - fieldPx.roundToInt() - if (rows) footerPx else 0).coerceAtLeast(0)
                            layout(p.width, room) { p.place(0, 0) }
                        },
                    ) {
                        AnimatedContent(
                            targetState = body,
                            transitionSpec = { (fadeIn(motion.fade(140)) togetherWith fadeOut(motion.fade(70))).using(null) },
                            contentAlignment = Alignment.TopStart,
                            label = "body",
                        ) { state ->
                            when {
                                state == "results" -> ResultsBody(model, icons) { r, a -> go(r, a, false) }
                                state.startsWith("card") -> model.card?.let { CardBody(it, model, cardChoice, onChoice = { c -> cardChoice = c }, onCard = onCard) }
                                state == "tip" -> TipBody(model)
                                else -> Spacer(Modifier.fillMaxWidth())
                            }
                        }
                    }
                    AnimatedVisibility(rows, enter = fadeIn(motion.fade(140)), exit = fadeOut(motion.fade(70))) { Footer(model) }
                }
            }
        }
    }
}

/** How long the open panel waits, with nothing typed, before a tip comes. */
private const val TIP_AFTER_MS = 320L

/**
 * A tip: the first-run card's shape. The feature's own mark, its name, one line that begins with
 * what to type (in full ink: it is what "Try it" types), and two answers. Neither answer is armed at
 * rest, so Enter on an empty panel still does nothing; a `tab` cap says how to get to them. Its
 * parts rise in one after the other, once the panel has made room.
 */
@Composable
private fun TipBody(model: OverlayModel) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val tip = remember { model.tip } ?: return
    val off = model.tipOff
    val arrive = remember { Animatable(if (motion.on) 0f else 1f) }
    LaunchedEffect(Unit) { arrive.animateTo(1f, motion.fade(140 + 2 * 22, easing = LinearEasing)) }
    val rise = with(LocalDensity.current) { 12.dp.toPx() }
    // Part [i] of three starts 22 ms after the one before it and takes 140 ms.
    fun Modifier.part(i: Int) = graphicsLayer {
        val t = ((arrive.value * (140f + 44f) - i * 22f) / 140f).coerceIn(0f, 1f)
        alpha = t; translationY = (1f - t) * rise
    }
    Row(Modifier.padding(horizontal = Metrics.pad).fillMaxWidth().height(Metrics.card).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.part(0).size(36.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = if (dark) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            Icon(Symbols.of(tip.symbol), null, Modifier.size(20.dp), tint = scheme.onSurface)
        }
        Column(Modifier.part(1).weight(1f).padding(start = 16.dp, end = 14.dp)) {
            Text(tip.name, color = scheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = TextStyle(fontFamily = Fonts.text, fontSize = 13.sp, fontWeight = FontWeight(500), lineHeight = 17.sp)
            AnimatedContent(off, transitionSpec = { motion.roll() }, contentAlignment = Alignment.TopStart, label = "line") { gone ->
                if (gone) Text(stringResource(R.string.tips_off), color = scheme.onSurface.copy(alpha = SECOND), style = line, maxLines = 2, overflow = TextOverflow.Ellipsis)
                else Text(androidx.compose.ui.text.buildAnnotatedString {
                    append(tip.example.trim())
                    addStyle(androidx.compose.ui.text.SpanStyle(color = scheme.onSurface), 0, length)
                    append(" "); append(tip.rest)
                }, color = scheme.onSurface.copy(alpha = SECOND), style = line, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        val answers by androidx.compose.animation.core.animateFloatAsState(if (off) 0f else 1f, motion.fade(60), label = "answers")
        Row(Modifier.part(2).graphicsLayer { alpha *= answers }, verticalAlignment = Alignment.CenterVertically) {
            // Nothing is armed until Tab: the cap says so, and goes once an answer is.
            val cap by androidx.compose.animation.core.animateFloatAsState(if (model.tipArmed == 0) 1f else 0f, motion.fade(120), label = "cap")
            Box(Modifier.padding(end = 10.dp).graphicsLayer { alpha = cap }) { Keycap("tab") }
            OptionStrip(listOf(stringResource(R.string.action_try), stringResource(R.string.tips_turn_off)), (model.tipArmed - 1).coerceAtLeast(0),
                onChoose = { model.tipArm(it + 1) }, onRun = { model.tipArm(it + 1); model.tipEnter() }, vertical = true, lit = model.tipArmed != 0)
        }
    }
}

private val DIGITS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)

/** The first-run card: one small step under the field, only while nothing is typed. Its two answers are one option strip. */
@Composable
private fun CardBody(card: Card, model: OverlayModel, choice: Int, onChoice: (Int) -> Unit, onCard: (Card, Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val engine = model.settings.engine()
    val (title, text, yes, no) = when (card) {
        Card.SHORTCUT -> listOf(stringResource(R.string.card_shortcut_title), stringResource(R.string.card_shortcut_text),
            stringResource(R.string.card_shortcut_action), stringResource(R.string.card_done))
        Card.SUGGESTIONS -> listOf(stringResource(R.string.card_suggest_title), stringResource(R.string.card_suggest_text, engine.name),
            stringResource(R.string.card_suggest_on), stringResource(R.string.card_not_now))
    }
    Row(
        Modifier.padding(horizontal = Metrics.pad).fillMaxWidth().height(Metrics.card)
            // No plate of its own: a box inside the glass would read as a second rim.
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Symbols.booklight, null, Modifier.size(20.dp), tint = scheme.primary)
        }
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 14.dp)) {
            Text(title, color = scheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text, color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 13.sp, fontWeight = FontWeight(500), lineHeight = 17.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        OptionStrip(listOf(yes, no), choice, onChoose = onChoice, onRun = { onCard(card, it == 0) }, vertical = true)
    }
}
