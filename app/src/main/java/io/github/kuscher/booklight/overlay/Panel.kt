package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
    companion object {
        /** From the setting: `off` is 1.0's small settle and fade; `fast`, `medium` and `slow` unfold. */
        fun of(setting: String, motion: Motion) = Arrival(motion.on && setting != "off", when (setting) { "medium" -> 2f; "slow" -> 4f; else -> 1f })
    }
}

/** A spring that would pass its end turns back instead, like something landing: nothing may be drawn outside the window. */
private fun landed(v: Float) = 1f - abs(1f - v)
private fun smooth(a: Float, b: Float, x: Float): Float { val t = ((x - a) / (b - a)).coerceIn(0f, 1f); return t * t * (3f - 2f * t) }

/** The seam the glass opens out of, as a width. */
private val SEAM = 6.dp

@Composable
fun Panel(
    model: OverlayModel,
    icons: AppIcons,
    /** True when the window is blurring what's behind it: the surface can be see-through. */
    glass: Boolean,
    dark: Boolean,
    arrival: Arrival,
    /** True once the panel is on its way out: it fades and the activity finishes after [Motion.LEAVE_MS]. */
    leaving: Boolean,
    onHeight: (Dp) -> Unit,
    /** How far the room has dimmed and how far the glass has come into focus, each 0 to 1: the window's dim and blur follow. */
    onPresence: (dim: Float, blur: Float) -> Unit,
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

    // The window follows the panel on a spring: taller as rows arrive, never moving its top edge.
    val height by animateDpAsState(Metrics.height(model), motion.place(), label = "height")
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
    LaunchedEffect(Unit) {
        if (!arrival.unfold) return@LaunchedEffect
        launch { high.animateTo(1f, tween((80 * slow).toInt(), easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))) }
        delay((50 * slow).toLong())
        wide.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 1000f / (slow * slow)))
    }
    LaunchedEffect(leaving) {
        launch { presence.animateTo(if (leaving) 0f else 1f, motion.fade(if (leaving) 90 else 170)) }
        launch { scale.animateTo(if (leaving) 0.975f else 1f, if (leaving) motion.fade(100) else motion.pop()) }
    }
    // The blur covers the whole window, so it waits until the glass is more than half open.
    LaunchedEffect(Unit) { snapshotFlow { presence.value * opened() to presence.value * smooth(0.5f, 1f, opened()) }.collect { (dim, blur) -> onPresence(dim, blur) } }

    // Once the glass is open, one light runs once around the outline.
    val run = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!motion.on) return@LaunchedEffect
        delay(((if (arrival.unfold) 190 else 70) * slow).toLong())
        run.animateTo(1.15f, tween((1100 * slow).toInt(), easing = CubicBezierEasing(0.3f, 0f, 0.2f, 1f)))
    }

    fun go(r: Result, a: Action, stay: Boolean) = onRun(r, a, stay)

    fun keys(e: KeyEvent): Boolean {
        if (e.type != KeyEventType.KeyDown) return false
        val card = model.card
        val atEnd = field.selection.collapsed && field.selection.end == field.text.length
        val enter = e.key == Key.Enter || e.key == Key.NumPadEnter
        // A waiting confirmation is cancelled by any key but Enter; Esc then only cancels.
        if (!enter && model.cancelConfirm() && e.key == Key.Escape) return true
        val r = model.current
        val entersScope = r?.actions?.getOrNull(model.armed)?.effect is Effect.EnterScope
        return when (e.key) {
            Key.DirectionDown -> { if (!model.moveCell(0, 1)) model.move(1); true }
            Key.DirectionUp -> { if (!model.moveCell(0, -1) && !model.restoreLast()) model.move(-1); true }
            Key.Enter, Key.NumPadEnter -> {
                if (card != null) onCard(card, cardChoice == 0) else model.enter { row, a -> go(row, a, e.isShiftPressed) }
                true
            }
            Key.Tab -> {
                when {
                    card != null -> cardChoice = 1 - cardChoice
                    entersScope && !e.isShiftPressed -> model.enter { row, a -> go(row, a, false) }
                    else -> model.arm(if (e.isShiftPressed) -1 else 1, wrap = true)
                }
                true
            }
            // Right at the end of the text and Left go along what the row offers: a grid's cells, a level, its actions.
            Key.DirectionRight -> when {
                !atEnd -> false
                r?.body is Body.Grid -> model.moveCell(1, 0)
                model.nudge(1) -> true
                entersScope -> { model.enter { row, a -> go(row, a, false) }; true }
                else -> model.arm(1, wrap = false)
            }
            Key.DirectionLeft -> when {
                r?.body is Body.Grid -> model.moveCell(-1, 0)
                atEnd && model.nudge(-1) -> true
                else -> model.arm(-1, wrap = false)     // on the first action Left is the caret's again
            }
            Key.Backspace -> field.text.isEmpty() && model.leaveScope()
            Key.Escape -> { onClose(); true }
            else -> {
                // Ctrl+1…9 runs that row's first action straight away (never one that removes something: those are never first).
                val n = DIGITS.indexOf(e.key)
                if (n >= 0 && e.isCtrlPressed && !e.isAltPressed && !e.isMetaPressed) {
                    model.results.getOrNull(n)?.takeIf { it.body !is Body.Grid }?.let { row -> row.actions.firstOrNull()?.takeIf { !it.danger && !it.confirm }?.let { go(row, it, false) } }
                    true
                } else false
            }
        }
    }

    val radiusPx = with(density) { Metrics.radius.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize().onPreviewKeyEvent(::keys)) {
        val full = maxWidth
        Box(
            Modifier
                // The glass: as wide and as high as the arrival has opened it, centred on the seam.
                .layout { measurable, constraints ->
                    val full = constraints.maxWidth
                    val h = height.roundToPx().coerceAtMost(constraints.maxHeight)
                    val w = (full * landed(wide.value)).roundToInt().coerceIn(1, full)
                    val hh = (h * landed(high.value)).roundToInt().coerceIn(1, h.coerceAtLeast(1))
                    val p = measurable.measure(Constraints.fixed(w, hh))
                    layout(full, constraints.maxHeight) { p.place((full - w) / 2, (h - hh) / 2) }
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
                        .offset { IntOffset(0, -((height.roundToPx() * (1f - landed(high.value))) / 2f).roundToInt()) }
                        .graphicsLayer { alpha = smooth(0.35f, 0.85f, opened()) },
                ) {
                    Field(model, field, onChange = { field = it; model.type(it.text) }, focus = focus)

                    val body = when {
                        model.results.isNotEmpty() -> "results"
                        model.card != null -> "card:${model.card}"
                        else -> "none"
                    }
                    AnimatedContent(
                        targetState = body,
                        transitionSpec = { (if (motion.on) fadeIn(tween(140)) togetherWith fadeOut(tween(70)) else fadeIn(motion.fade(0)) togetherWith fadeOut(motion.fade(0))).using(null) },
                        contentAlignment = Alignment.TopStart,
                        label = "body",
                    ) { state ->
                        when {
                            state == "results" -> ResultsBody(model, icons) { r, a -> go(r, a, false) }
                            state.startsWith("card") -> model.card?.let { CardBody(it, model, cardChoice, onChoice = { c -> cardChoice = c }, onCard = onCard) }
                            else -> Spacer(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
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
