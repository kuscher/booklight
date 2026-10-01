package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon as RowIcon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Results: rows that rise in, move to their new place and fade out, under one gliding selection

/** One row on screen. It outlives its result for a moment while it fades out. */
private class Slot(val uid: Int, var result: Result) {
    var top: Dp = 0.dp
    var index = 0
    var leaving = false
    var delay = 0
}

/** Keeps rows alive across lists: the same id keeps its row (and moves), new ids rise in, gone ones fade. */
private class RowSlots {
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
fun ResultsBody(model: OverlayModel, icons: AppIcons, onRun: (Result, Action) -> Unit) {
    val motion = LocalMotion.current
    val slots = remember { RowSlots() }
    slots.removed   // read, so a row that has finished fading out is dropped from the composition
    val rows = slots.sync(model.results, motion)
    val picked = model.results.getOrNull(model.selected)
    Column {
        Box(Modifier.padding(horizontal = Metrics.pad).padding(top = Metrics.pad).fillMaxWidth().height(Metrics.listHeight(model.results))) {
            val top = model.results.take(model.selected).fold(0.dp) { h, r -> h + Metrics.rowHeight(r) }
            // A grid has its own highlight, the square on its cells: one highlight per level.
            Pill(top, picked?.let(Metrics::rowHeight) ?: Metrics.row, visible = picked != null && picked.body !is Body.Grid)
            for (slot in rows) key(slot.uid) {
                val selected = !slot.leaving && slot.index == model.selected
                val r = slot.result
                SlotRow(slot.top, slot.leaving, slot.delay, onGone = { slots.remove(slot) }) {
                    ResultRow(
                        r, icons, selected,
                        armed = if (selected) model.armed else r.armed, confirming = selected && model.confirming, cell = if (selected) model.cell else -1,
                        onHover = { if (!slot.leaving) model.select(slot.index) },
                        onClick = { if (!slot.leaving) { model.select(slot.index); model.armAt(r.armed); model.enter(onRun) } },
                        onArm = { if (selected) model.armAt(it) },
                        onAction = { if (selected) { model.armAt(it); model.enter(onRun) } },
                        onCell = { if (selected) model.cellAt(it) else if (!slot.leaving) model.select(slot.index) },
                        onPick = { if (!slot.leaving) { model.select(slot.index); model.cellAt(it); model.armAt(0); model.enter(onRun) } },
                    )
                }
            }
        }
        Footer(model)
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
fun Pill(top: Dp, height: Dp, visible: Boolean) {
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
private fun RowFrame(height: Dp, selected: Boolean, label: String, actions: List<Action>, onHover: () -> Unit, onClick: () -> Unit, onAction: (Int) -> Unit, content: @Composable RowScope.() -> Unit) {
    val hover by rememberUpdatedState(onHover)
    val act by rememberUpdatedState(onAction)
    Row(
        Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(24.dp))
            // Hover selects only when the pointer moves, so a list growing under a resting pointer doesn't steal the selection.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) if (awaitPointerEvent().type == PointerEventType.Move) hover()
                }
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            // One item for a screen reader, whatever it can do offered as its actions.
            .semantics(mergeDescendants = true) {
                this.selected = selected; role = Role.Button; contentDescription = label
                if (actions.size > 1) customActions = actions.drop(1).mapIndexed { i, a -> CustomAccessibilityAction(a.label) { act(i + 1); true } }
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun ResultRow(
    r: Result, icons: AppIcons, selected: Boolean, armed: Int, confirming: Boolean, cell: Int,
    onHover: () -> Unit, onClick: () -> Unit, onArm: (Int) -> Unit, onAction: (Int) -> Unit, onCell: (Int) -> Unit, onPick: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val on by animateColorAsState(if (selected) scheme.onSecondaryContainer else scheme.onSurface, motion.fade(120), label = "on")
    val dim by animateColorAsState(if (selected) scheme.onSecondaryContainer else scheme.onSurface.copy(alpha = SECOND), motion.fade(120), label = "dim")
    val pop by animateFloatAsState(if (selected) 1.06f else 1f, motion.pop(), label = "icon")
    val kind = kindLabel(r.kind)
    val body = r.body
    val described = stringResource(R.string.a11y_selected, r.title, r.actions.getOrNull(armed)?.label ?: kind)
    RowFrame(Metrics.rowHeight(r), selected, described, r.actions, onHover, onClick, onAction) {
        if (body is Body.Grid) { GridBody(body, cell.coerceAtLeast(0), selected, onCell, onPick); return@RowFrame }
        if (body is Body.Code) QrPlate(body.text)
        else Box(Modifier.graphicsLayer { scaleX = pop; scaleY = pop }) { RowPicture(r.icon, icons, dim) }

        when {
            body is Body.Slots -> SlotsBody(body, on)
            body is Body.Code -> Column(Modifier.weight(1f).padding(start = 20.dp, end = 12.dp)) {
                Text(r.title, color = on, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 3, overflow = TextOverflow.Ellipsis)
                r.subtitle?.let { Text(it, color = dim, style = SMALL, maxLines = 1, modifier = Modifier.padding(top = 4.dp)) }
            }
            else -> Column(Modifier.weight(1f).padding(start = 16.dp)) {
                // A row whose actions copy different forms of one value (a colour) shows the armed form large.
                val big = if (body is Body.Mono) body.text else if (r.icon is RowIcon.Swatch) (r.actions.getOrNull(armed)?.effect as? Effect.CopyText)?.text ?: r.answer else r.answer
                if (big != null) {
                    r.subtitle?.let { Text(it, color = dim, style = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    if (body is Body.Mono) MonoText(big, on)
                    // A changed answer rolls up into place, like a counter.
                    else AnimatedContent(big, transitionSpec = {
                        (slideInVertically(motion.place()) { it / 2 } + fadeIn(motion.fade(120))) togetherWith (slideOutVertically(motion.place()) { -it / 2 } + fadeOut(motion.fade(70)))
                    }, label = "answer") { a ->
                        Text(a, color = on, style = TextStyle(fontFamily = Fonts.round, fontSize = if (a.length > 22) 26.sp else 34.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    Text(r.title, color = on, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    r.subtitle?.let { Text(it, color = dim, style = SMALL, maxLines = if (Metrics.rowHeight(r) > Metrics.row) 2 else 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }

        // A level: the number keeps its place whether the row is selected or not; the track joins it when it is.
        if (body is Body.Level) {
            val track by animateFloatAsState(if (selected) 1f else 0f, motion.fade(110), label = "track")
            if (selected || track > 0f) LevelTrack(body, on, Modifier.padding(end = 12.dp).graphicsLayer { alpha = track })
            if (!body.locked) LevelNumber(body, if (selected) on else scheme.onSurface.copy(alpha = SECOND))
            Spacer(Modifier.width(if (selected) 8.dp else 0.dp))
        }

        // What the row can do arrives on the selected row; the others say what kind of thing they are.
        AnimatedContent(selected && r.actions.isNotEmpty(), transitionSpec = {
            if (targetState) (slideInHorizontally(motion.place()) { it / 4 } + fadeIn(motion.fade(110, 60))) togetherWith fadeOut(motion.fade(60))
            else fadeIn(motion.fade(110)) togetherWith fadeOut(motion.fade(60))
        }, contentAlignment = Alignment.CenterEnd, label = "trail") { strip ->
            when {
                strip -> ActionStrip(r.actions, armed.coerceIn(0, r.actions.size - 1), confirming,
                    confirmLabel = stringResource(R.string.confirm_again), onArm = onArm, onRun = onAction)
                body is Body.Level -> Spacer(Modifier.width(0.dp))
                r.kind == Kind.SCOPE -> Keycap("tab")
                else -> Text(kind, color = scheme.onSurface.copy(alpha = SECOND), style = SMALL, maxLines = 1)
            }
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
        is RowIcon.Swatch -> Swatch(icon.argb, tinted)
        is RowIcon.Glyph -> Box(Modifier.size(size).clip(CircleShape).background(tinted.copy(alpha = if (LocalDark.current) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            Text(icon.text, color = tinted, style = TextStyle(fontFamily = Fonts.round, fontSize = if (icon.text.length > 2) 12.sp else 17.sp, fontWeight = FontWeight(600)), maxLines = 1)
        }
    }
}

@Composable
fun kindLabel(k: Kind): String = when (k) {
    Kind.APP -> stringResource(R.string.kind_app)
    Kind.SETTING -> stringResource(R.string.kind_setting)
    Kind.COMMAND -> stringResource(R.string.kind_command)
    Kind.WEB -> stringResource(R.string.kind_web)
    Kind.ANSWER -> stringResource(R.string.kind_answer)
    Kind.SUGGESTION -> stringResource(R.string.kind_suggestion)
    Kind.SCOPE, Kind.CONTROL, Kind.OTHER -> ""
}
