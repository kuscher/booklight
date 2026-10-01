package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Icon as RowIcon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Text on glass is one ink, `onSurface`, at three strengths: an alpha ink follows whatever shows through, a fixed grey would vanish on grey glass. */
private const val SECOND = 0.80f
private const val THIRD = 0.60f
private val LocalDark = staticCompositionLocalOf { false }

/** The panel's sizes. The window is exactly this big, so its blur follows the panel. */
object Metrics {
    val width = 720.dp
    val field = 68.dp
    val row = 56.dp
    val answer = 92.dp
    val card = 96.dp
    val pad = 8.dp
    val footer = 36.dp
    val radius = 32.dp
    /** Where the panel's top edge sits, as a share of the screen's height: the field stays put while the list grows down. */
    const val TOP = 0.2f

    fun rowHeight(r: Result): Dp = if (r.answer != null) answer else row
    fun listHeight(rows: List<Result>): Dp = rows.fold(0.dp) { h, r -> h + rowHeight(r) }

    /** The panel's height for what the model is showing. Must match what [Panel] draws. */
    fun height(m: OverlayModel): Dp {
        val of = m.actionsOf
        return field + when {
            of != null -> pad + rowHeight(of) + row * of.actions.size + pad + footer
            m.results.isNotEmpty() -> pad + listHeight(m.results) + pad + footer
            m.card != null -> card + pad
            else -> 0.dp
        }
    }

    /** How many rows fit under the field on a screen this tall, so the panel never runs off it. */
    fun maxRows(screenHeightDp: Float): Int {
        val room = screenHeightDp * (1 - TOP) - 56 - field.value - pad.value * 2 - footer.value
        return (room / row.value).toInt().coerceIn(3, 8)
    }
}

@Composable
fun Panel(
    model: OverlayModel,
    icons: AppIcons,
    /** True when the window is blurring what's behind it: the surface can be see-through. */
    glass: Boolean,
    dark: Boolean,
    /** True once the panel is on its way out: it fades and the activity finishes after [Motion.LEAVE_MS]. */
    leaving: Boolean,
    onHeight: (Dp) -> Unit,
    /** 0 → 1 as the panel arrives, back to 0 as it leaves: the window's blur and dim follow it. */
    onPresence: (Float) -> Unit,
    onRun: (Result, Action) -> Unit,
    onCard: (Card, primary: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val density = LocalDensity.current
    val focus = remember { FocusRequester() }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var cardChoice by remember { mutableIntStateOf(0) }
    // Text set from outside (the debug hook) lands in the field too.
    LaunchedEffect(model.query) { if (field.text != model.query) field = TextFieldValue(model.query, TextRange(model.query.length)) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(model.card) { cardChoice = 0 }

    // Arriving and leaving: the glass comes into focus while the panel settles into place.
    val presence = remember { Animatable(if (motion.on) 0f else 1f) }
    val scale = remember { Animatable(if (motion.on) 0.96f else 1f) }
    LaunchedEffect(leaving) {
        launch { presence.animateTo(if (leaving) 0f else 1f, motion.fade(if (leaving) 90 else 170)) }
        launch { scale.animateTo(if (leaving) 0.975f else 1f, if (leaving) motion.fade(100) else motion.pop()) }
    }
    LaunchedEffect(Unit) { snapshotFlow { presence.value }.collect { onPresence(it) } }

    // The window follows the panel on a spring: taller as rows arrive, never moving its top edge.
    val height by animateDpAsState(Metrics.height(model), motion.place(), label = "height")
    LaunchedEffect(Unit) { snapshotFlow { height }.collect { onHeight(it) } }

    // On arrival, a short gleam runs along the outline.
    val sweep = remember { Animatable(-400f) }
    LaunchedEffect(Unit) {
        if (motion.on) sweep.animateTo(with(density) { (Metrics.width * 2f).toPx() }, tween(560, 70, CubicBezierEasing(0.3f, 0f, 0.2f, 1f)))
    }

    fun keys(e: KeyEvent): Boolean {
        if (e.type != KeyEventType.KeyDown) return false
        val card = model.card
        return when (e.key) {
            Key.DirectionDown -> { model.move(1); true }
            Key.DirectionUp -> { model.move(-1); true }
            Key.Enter, Key.NumPadEnter -> {
                if (card != null) onCard(card, cardChoice == 0) else model.enter(onRun)
                true
            }
            Key.Tab -> {
                if (card != null) cardChoice = 1 - cardChoice
                else if (e.isShiftPressed) model.closeActions() else model.openActions()
                true
            }
            Key.DirectionRight -> field.selection.end == field.text.length && model.actionsOf == null && model.openActions()
            Key.DirectionLeft -> model.actionsOf != null && model.closeActions()
            Key.Escape -> { if (!model.closeActions()) onClose(); true }
            else -> {
                // Ctrl+1…9 runs that row straight away.
                val n = DIGITS.indexOf(e.key)
                if (n >= 0 && e.isCtrlPressed && !e.isAltPressed && !e.isMetaPressed && model.actionsOf == null) {
                    model.results.getOrNull(n)?.let { r -> r.actions.firstOrNull()?.let { onRun(r, it) } }
                    true
                } else false
            }
        }
    }

    val radiusPx = with(density) { Metrics.radius.toPx() }
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = presence.value }
            .glass(
                // The veil is white in light theme and near-black in dark: the most contrast for the least tint.
                tint = if (glass) scheme.surfaceContainerLowest.copy(alpha = if (dark) Look.tintDark else Look.tintLight) else scheme.surfaceContainerHigh,
                lead = scheme.tertiary, tail = scheme.primary,
                radiusPx = radiusPx, density = density.density, sweep = { sweep.value }, dark = dark, solid = !glass,
            )
            .clip(RoundedCornerShape(Metrics.radius))
            .onPreviewKeyEvent(::keys),
    ) {
      CompositionLocalProvider(LocalDark provides dark) {
        Column(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true)) {
            Field(model, field, onChange = { field = it; model.type(it.text) }, focus = focus)

            val of = model.actionsOf
            val body = when {
                of != null -> "actions:${of.id}"
                model.results.isNotEmpty() -> "results"
                model.card != null -> "card:${model.card}"
                else -> "none"
            }
            AnimatedContent(
                targetState = body,
                transitionSpec = {
                    val deeper = targetState.startsWith("actions")
                    val back = initialState.startsWith("actions") && targetState == "results"
                    when {
                        !motion.on -> fadeIn(motion.fade(0)) togetherWith fadeOut(motion.fade(0))
                        // Tab goes deeper into a row, so the lists pass each other sideways.
                        deeper -> (slideInHorizontally(motion.place()) { it / 7 } + fadeIn(tween(140, 30))) togetherWith
                            (slideOutHorizontally(motion.place()) { -it / 7 } + fadeOut(tween(80)))
                        back -> (slideInHorizontally(motion.place()) { -it / 7 } + fadeIn(tween(140, 30))) togetherWith
                            (slideOutHorizontally(motion.place()) { it / 7 } + fadeOut(tween(80)))
                        else -> fadeIn(tween(140)) togetherWith fadeOut(tween(70))
                    }.using(null)
                },
                contentAlignment = Alignment.TopStart,
                label = "body",
            ) { state ->
                when {
                    state.startsWith("actions") -> ActionsBody(model, icons, onRun)
                    state == "results" -> ResultsBody(model, icons, onRun)
                    state.startsWith("card") -> model.card?.let { CardBody(it, model, cardChoice, onChoice = { c -> cardChoice = c }, onCard = onCard) }
                    else -> Spacer(Modifier.fillMaxWidth())
                }
            }
        }
      }
    }
}

private val DIGITS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)

// ---------------------------------------------------------------------------------------------
// The field

@Composable
private fun Field(model: OverlayModel, field: TextFieldValue, onChange: (TextFieldValue) -> Unit, focus: FocusRequester) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    Row(Modifier.fillMaxWidth().height(Metrics.field).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        // The search engine's mark where the magnifier would be: Google's G when Google does the searching.
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(model.settings.engine == "google", transitionSpec = { (scaleIn(motion.pop(), 0.6f) + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "mark") { google ->
                if (google) Text("G", color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.round, fontSize = 24.sp, fontWeight = FontWeight(600)))
                else Icon(Symbols.search, null, Modifier.size(22.dp), tint = scheme.onSurface.copy(alpha = SECOND))
            }
        }
        Box(Modifier.weight(1f).padding(start = 16.dp), contentAlignment = Alignment.CenterStart) {
            val style = TextStyle(fontFamily = Fonts.text, fontSize = 24.sp, fontWeight = FontWeight(500), color = scheme.onSurface)
            if (field.text.isEmpty()) {
                Text(stringResource(R.string.search_hint), style = style.copy(color = scheme.onSurface.copy(alpha = THIRD), fontWeight = FontWeight(400)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // The rest of the top hit's name, grey, after the cursor: drawn with the text so it sits on the same line.
            val rest = model.completion
            val ghostAlpha by animateFloatAsState(if (rest != null) THIRD else 0f, motion.fade(140), label = "ghost")
            val ghost = scheme.onSurface.copy(alpha = ghostAlpha)
            val label = stringResource(R.string.a11y_search_field)
            BasicTextField(
                value = field,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = label },
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(scheme.primary),
                visualTransformation = remember(rest, ghost) { Ghost(rest.orEmpty(), ghost) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Go),
            )
        }
        val site = model.site
        AnimatedVisibility(site != null, enter = scaleIn(motion.pop(), 0.7f) + fadeIn(), exit = scaleOut(targetScale = 0.8f) + fadeOut(tween(80))) {
            val name by rememberUpdatedState(site?.name ?: "")
            Box(Modifier.padding(end = 10.dp).clip(CircleShape).background(scheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text(name, color = scheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight(600)))
            }
        }
        // The footer says "esc Close" once there is one; until then the field does.
        AnimatedVisibility(model.results.isEmpty() && model.actionsOf == null, enter = fadeIn(motion.fade(120)), exit = fadeOut(motion.fade(120))) { Keycap("esc") }
    }
}

/** Appends grey completion text after what was typed; the cursor stays at the end of the real text. */
private class Ghost(private val rest: String, private val color: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (rest.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val n = text.length
        val shown = buildAnnotatedString { append(text); withStyle(SpanStyle(color = color)) { append(rest) } }
        return TransformedText(shown, object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset
            override fun transformedToOriginal(offset: Int) = minOf(offset, n)
        })
    }
    override fun equals(other: Any?) = other is Ghost && other.rest == rest && other.color == color
    override fun hashCode() = rest.hashCode() * 31 + color.hashCode()
}

// ---------------------------------------------------------------------------------------------
// Results: rows that rise in, move to their new place and fade out, under one gliding selection

/** One row on screen. It outlives its result for a moment while it fades out. */
private class Slot(val uid: Int, var result: Result) {
    var top: Dp = 0.dp
    var index = 0
    var leaving = false
    var delay = 0
}

/** Keeps rows alive across lists: the same id keeps its row (and moves), new ids rise in, gone ones fade. */
private class Slots {
    var all: List<Slot> = emptyList()
    private var last: List<Result>? = null
    private var next = 0
    var removed by mutableIntStateOf(0)

    fun sync(results: List<Result>, motion: Motion): List<Slot> {
        if (results === last) return all
        last = results
        val live = all.filter { !it.leaving }.associateBy { it.result.id }
        val fromNothing = live.isEmpty()
        var y = 0.dp
        var fresh = 0
        val now = results.mapIndexed { i, r ->
            val s = live[r.id] ?: Slot(next++, r).also { it.delay = motion.stagger(if (fromNothing) i else fresh++) }
            s.result = r; s.index = i; s.top = y
            y += Metrics.rowHeight(r)
            s
        }
        val ids = results.mapTo(HashSet()) { it.id }
        val going = all.filter { it.leaving || it.result.id !in ids }.onEach { it.leaving = true }
        all = now + going
        return all
    }

    fun remove(slot: Slot) { all = all - slot; removed++ }
}

@Composable
private fun ResultsBody(model: OverlayModel, icons: AppIcons, onRun: (Result, Action) -> Unit) {
    val motion = LocalMotion.current
    val slots = remember { Slots() }
    slots.removed   // read, so a row that has finished fading out is dropped from the composition
    val rows = slots.sync(model.results, motion)
    val picked = model.results.getOrNull(model.selected)
    Column {
        Box(Modifier.padding(horizontal = Metrics.pad).padding(top = Metrics.pad).fillMaxWidth().height(Metrics.listHeight(model.results))) {
            val top = model.results.take(model.selected).fold(0.dp) { h, r -> h + Metrics.rowHeight(r) }
            Pill(top, picked?.let(Metrics::rowHeight) ?: Metrics.row, visible = picked != null)
            for (slot in rows) key(slot.uid) {
                val selected = !slot.leaving && slot.index == model.selected
                SlotRow(slot.top, slot.leaving, slot.delay, onGone = { slots.remove(slot) }) {
                    ResultRow(slot.result, icons, selected, hint = slot.result.actions.firstOrNull()?.label,
                        onHover = { if (!slot.leaving) model.select(slot.index) },
                        onClick = { if (!slot.leaving) slot.result.actions.firstOrNull()?.let { onRun(slot.result, it) } })
                }
            }
        }
        Footer(model.flash, stringResource(R.string.hint_close), actions = (model.current?.actions?.size ?: 0) > 1)
    }
}

/** Places a row, moves it when its slot changes, and plays its arrival and departure. */
@Composable
private fun SlotRow(top: Dp, leaving: Boolean, delayMs: Int, onGone: () -> Unit, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    val y by animateDpAsState(top, motion.place(), label = "row")
    val here = remember { Animatable(if (motion.on) 0f else 1f) }
    val gone by rememberUpdatedState(onGone)
    LaunchedEffect(leaving) {
        if (leaving) { here.animateTo(0f, motion.fade(80)); gone() }
        else { if (delayMs > 0) delay(delayMs.toLong()); here.animateTo(1f, motion.place()) }
    }
    val rise = with(LocalDensity.current) { 12.dp.toPx() }
    Box(
        Modifier.zIndex(if (leaving) 0f else 1f)
            .offset { IntOffset(0, y.roundToPx()) }
            .graphicsLayer {
                alpha = here.value.coerceIn(0f, 1f)
                // Arriving rows rise into place; leaving ones just fade where they are.
                translationY = if (leaving) 0f else (1f - here.value) * rise
            },
    ) { content() }
}

/**
 * The selection: one pill for the whole list. When it moves, the edge that leads travels on a
 * quicker spring than the edge that follows, so it stretches towards the row it is going to and
 * then gathers itself there.
 */
@Composable
private fun Pill(top: Dp, height: Dp, visible: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val upper = remember { Animatable(top, Dp.VectorConverter) }
    val lower = remember { Animatable(top + height, Dp.VectorConverter) }
    LaunchedEffect(top, height) {
        val down = top + height / 2 > (upper.targetValue + lower.targetValue) / 2
        launch { upper.animateTo(top, if (down) motion.trail() else motion.lead()) }
        launch { lower.animateTo(top + height, if (down) motion.lead() else motion.trail()) }
    }
    val shown by animateFloatAsState(if (visible) 1f else 0f, motion.fade(120), label = "pill")
    val shape = RoundedCornerShape(24.dp)   // the panel's 32 less the 8 it is inset by: concentric
    val dark = LocalDark.current
    Box(
        Modifier.offset { IntOffset(0, upper.value.roundToPx()) }
            .fillMaxWidth().height((lower.value - upper.value).coerceAtLeast(12.dp))
            .graphicsLayer { alpha = shown }
            .clip(shape)
            // The only coloured surface, and the densest: that is what says "selected". Flat, with the panel's white outline.
            .background(scheme.secondaryContainer.copy(alpha = if (dark) 0.66f else 0.78f))
            .border(with(LocalDensity.current) { 1f.toDp() }, Color.White.copy(alpha = if (dark) 0.30f else 0.55f), shape),
    )
}

@Composable
private fun RowFrame(height: Dp, selected: Boolean, label: String, onHover: () -> Unit, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val hover by rememberUpdatedState(onHover)
    Row(
        Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(24.dp))
            // Hover selects only when the pointer moves, so a list growing under a resting pointer doesn't steal the selection.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) if (awaitPointerEvent().type == PointerEventType.Move) hover()
                }
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = selected; role = Role.Button; contentDescription = label }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun ResultRow(r: Result, icons: AppIcons, selected: Boolean, hint: String?, onHover: () -> Unit, onClick: () -> Unit, head: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    // As the head of the actions view a row is context, not a choice: quieter, and without its kind.
    val on by animateColorAsState(if (selected) scheme.onSecondaryContainer else scheme.onSurface.copy(alpha = if (head) SECOND else 1f), motion.fade(120), label = "on")
    val dim by animateColorAsState(if (selected) scheme.onSecondaryContainer else scheme.onSurface.copy(alpha = SECOND), motion.fade(120), label = "dim")
    val pop by animateFloatAsState(if (selected) 1.06f else 1f, motion.pop(), label = "icon")
    val small = TextStyle(fontFamily = Fonts.text, fontSize = 13.sp, fontWeight = FontWeight(500), letterSpacing = 0.1.sp)
    val kind = kindLabel(r.kind)
    RowFrame(Metrics.rowHeight(r), selected, stringResource(R.string.a11y_selected, r.title, hint ?: kind), onHover, onClick) {
        Box(Modifier.graphicsLayer { scaleX = pop; scaleY = pop }) { RowPicture(r.icon, icons, dim) }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            if (r.answer != null) {
                r.subtitle?.let { Text("$it =", color = dim, style = small, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                // A changed answer rolls up into place, like a counter.
                AnimatedContent(r.answer!!, transitionSpec = {
                    (slideInVertically(motion.place()) { it / 2 } + fadeIn(motion.fade(120))) togetherWith (slideOutVertically(motion.place()) { -it / 2 } + fadeOut(motion.fade(70)))
                }, label = "answer") { a ->
                    Text(a, color = on, style = TextStyle(fontFamily = Fonts.round, fontSize = 34.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(r.title, color = on, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                r.subtitle?.let { Text(it, color = dim, style = small, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        // What Enter does slides in on the selected row; the others say what kind of thing they are.
        AnimatedContent(selected && hint != null, transitionSpec = {
            if (targetState) (slideInHorizontally(motion.pop()) { it / 3 } + fadeIn(motion.fade(110))) togetherWith fadeOut(motion.fade(60))
            else fadeIn(motion.fade(110)) togetherWith (slideOutHorizontally(motion.fade(90)) { it / 4 } + fadeOut(motion.fade(60)))
        }, contentAlignment = Alignment.CenterEnd, label = "trail") { showHint ->
            if (showHint) Row(verticalAlignment = Alignment.CenterVertically) {
                Text(hint.orEmpty(), color = dim, style = small, modifier = Modifier.padding(end = 8.dp), maxLines = 1)
                EnterKey()
            } else if (!head) Text(kind, color = scheme.onSurface.copy(alpha = SECOND), style = small, maxLines = 1)
        }
    }
}

@Composable
private fun RowPicture(icon: RowIcon, icons: AppIcons, tinted: Color) {
    val size = 36.dp
    when (icon) {
        is RowIcon.App -> {
            val px = with(LocalDensity.current) { 48.dp.roundToPx() }
            val bitmap by produceState(icons.cached(icon), icon) { if (value == null) value = icons.load(icon, px) }
            Box(Modifier.size(size)) { bitmap?.let { Image(it, null, Modifier.fillMaxSize()) } }
        }
        is RowIcon.Symbol -> Box(Modifier.size(size).clip(CircleShape).background(tinted.copy(alpha = if (LocalDark.current) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            Icon(Symbols.of(icon.name), null, Modifier.size(20.dp), tint = tinted)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Actions: what else a row can do

@Composable
private fun ActionsBody(model: OverlayModel, icons: AppIcons, onRun: (Result, Action) -> Unit) {
    val of = model.actionsOf ?: return
    val scheme = MaterialTheme.colorScheme
    val head = Metrics.rowHeight(of)
    Column {
        Box(Modifier.padding(horizontal = Metrics.pad).padding(top = Metrics.pad).fillMaxWidth().height(head + Metrics.row * of.actions.size)) {
            Pill(head + Metrics.row * model.actionIndex, Metrics.row, visible = true)
            ResultRow(of, icons, selected = false, hint = null, onHover = {}, onClick = { model.closeActions() }, head = true)
            of.actions.forEachIndexed { i, a ->
                val selected = i == model.actionIndex
                SlotRow(head + Metrics.row * i, leaving = false, delayMs = LocalMotion.current.stagger(i), onGone = {}) {
                    RowFrame(Metrics.row, selected, a.label, onHover = { model.selectAction(i) }, onClick = { onRun(of, a) }) {
                        Spacer(Modifier.width(52.dp))
                        val on by animateColorAsState(if (selected) scheme.onSecondaryContainer else scheme.onSurface, LocalMotion.current.fade(120), label = "action")
                        Text(a.label, Modifier.weight(1f), color = on, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)))
                        AnimatedVisibility(selected, enter = fadeIn(tween(110)) + scaleIn(LocalMotion.current.pop(), 0.7f), exit = fadeOut(tween(60))) { EnterKey() }
                    }
                }
            }
        }
        Footer(model.flash, stringResource(R.string.hint_back))
    }
}

// ---------------------------------------------------------------------------------------------
// The first-run card: one small step under the field, only while nothing is typed

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
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CardButton(yes, chosen = choice == 0, onHover = { onChoice(0) }) { onCard(card, true) }
            CardButton(no, chosen = choice == 1, onHover = { onChoice(1) }) { onCard(card, false) }
        }
    }
}

@Composable
private fun CardButton(label: String, chosen: Boolean, onHover: () -> Unit, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val bg by animateColorAsState(if (chosen) scheme.secondaryContainer.copy(alpha = if (dark) 0.66f else 0.78f) else Color.Transparent, motion.fade(120), label = "button")
    val edge by animateColorAsState(if (chosen) Color.White.copy(alpha = if (dark) 0.30f else 0.55f) else Color.Transparent, motion.fade(120), label = "buttonEdge")
    val on by animateColorAsState(if (chosen) scheme.onSecondaryContainer else scheme.onSurface.copy(alpha = SECOND), motion.fade(120), label = "buttonText")
    val hover by rememberUpdatedState(onHover)
    Row(
        Modifier.clip(CircleShape).background(bg).border(with(LocalDensity.current) { 1f.toDp() }, edge, CircleShape)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) if (awaitPointerEvent().type == PointerEventType.Move) hover() } }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .semantics { role = Role.Button; selected = chosen }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = on, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight(if (chosen) 600 else 500)), maxLines = 1)
        AnimatedVisibility(chosen, enter = fadeIn(tween(110)) + scaleIn(motion.pop(), 0.7f), exit = fadeOut(tween(60))) {
            Box(Modifier.padding(start = 8.dp)) { EnterKey() }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Small parts

@Composable
private fun Footer(flash: String?, escLabel: String, actions: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val hint = TextStyle(fontFamily = Fonts.text, fontSize = 12.sp, fontWeight = FontWeight(500), letterSpacing = 0.1.sp)
    val ink = scheme.onSurface.copy(alpha = SECOND)
    Row(Modifier.fillMaxWidth().height(Metrics.footer + Metrics.pad).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            AnimatedContent(flash, transitionSpec = { (fadeIn(motion.fade(120)) + scaleIn(motion.pop(), 0.8f)) togetherWith fadeOut(motion.fade(80)) }, contentAlignment = Alignment.CenterStart, label = "flash") { word ->
                Text(word.orEmpty(), color = scheme.primary, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight(600)))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(actions, enter = fadeIn(motion.fade(120)), exit = fadeOut(motion.fade(80))) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Keycap("tab")
                    Text(stringResource(R.string.hint_actions), color = ink, style = hint, modifier = Modifier.padding(end = 10.dp))
                }
            }
            Keycap("esc")
            Text(escLabel, color = ink, style = hint)
        }
    }
}

@Composable
private fun Keycap(label: String) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.height(22.dp).clip(RoundedCornerShape(7.dp)).background(scheme.onSurface.copy(alpha = if (LocalDark.current) 0.14f else 0.10f)).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
        Text(label, color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 12.sp, fontWeight = FontWeight(500), letterSpacing = 0.1.sp))
    }
}

/** The Enter key, on whatever Enter would run. */
@Composable
private fun EnterKey() {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.size(26.dp, 22.dp).clip(RoundedCornerShape(7.dp)).background(scheme.onSecondaryContainer.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        Icon(Symbols.enter, null, Modifier.size(14.dp), tint = scheme.onSecondaryContainer)
    }
}

@Composable
private fun kindLabel(k: Kind): String = when (k) {
    Kind.APP -> stringResource(R.string.kind_app)
    Kind.SETTING -> stringResource(R.string.kind_setting)
    Kind.COMMAND -> stringResource(R.string.kind_command)
    Kind.WEB -> stringResource(R.string.kind_web)
    Kind.ANSWER -> stringResource(R.string.kind_answer)
    Kind.SUGGESTION -> stringResource(R.string.kind_suggestion)
    Kind.OTHER -> ""
}
