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
import kotlinx.coroutines.flow.first
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
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

/**
 * Keeps rows alive across lists: the same id keeps its row (and moves), new ids rise in, gone ones
 * fade. An opened row's other actions are not rows of their own here: they are drawn by their row,
 * so that they travel with it and are uncovered and covered under it.
 */
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
        val tops = Metrics.tops(results)
        var fresh = 0
        val now = results.mapIndexedNotNull { i, r ->
            if (r.kind == Kind.ACTION) return@mapIndexedNotNull null
            val s = live[r.id] ?: Slot(next++, r).also { it.delay = motion.stagger(if (fromNothing) i else fresh++) }
            s.result = r; s.index = i; s.top = tops[i]
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
    val tops = Metrics.tops(model.results)
    val picked = model.results.getOrNull(model.selected)

    // A row's other actions, opened under it: one value uncovers them (0 to 1), from the row's lower edge down, and
    // covers them again. They are laid out once and never move. The rows that were opened are kept while they close.
    val block = remember { Animatable(0f) }
    var held by remember { mutableStateOf<Pair<String, List<Result>>?>(null) }
    val open = model.opened
    if (open != null) model.results.filter { it.kind == Kind.ACTION }.let { if (held?.first != open || held?.second != it) held = open to it }
    LaunchedEffect(open) {
        if (open != null) { delay(motion.hold(30)); block.animateTo(1f, motion.place()) }     // the other rows are fading in these first frames
        else { block.animateTo(0f, motion.place()); held = null }
    }

    Box(Modifier.padding(horizontal = Metrics.pad).padding(top = Metrics.pad).fillMaxWidth().height(Metrics.listHeight(model.results))) {
        val danger = picked?.kind == Kind.ACTION && picked.actions.firstOrNull()?.danger == true
        // A grid has its own highlight, the square on its cells: one highlight per level.
        Pill(tops.getOrElse(model.selected) { 0.dp }, picked?.let(Metrics::rowHeight) ?: Metrics.row, visible = picked != null && picked.body !is Body.Grid, danger = danger)
        for (slot in rows) key(slot.uid) {
            val selected = !slot.leaving && slot.index == model.selected
            val r = slot.result
            val mine = held?.takeIf { it.first == r.id }
            // Rows that come back when a list closes wait until it has closed: nothing arrives under an edge that is still moving.
            SlotRow(slot.top, slot.leaving, slot.delay, wait = { open == null && held != null && mine == null && block.value > 0.04f }, onGone = { slots.remove(slot) }) {
                ResultRow(
                    r, icons, selected,
                    armed = if (selected) model.armed else r.armed, confirming = selected && model.confirming, cell = if (selected) model.cell else -1,
                    stops = model.stops(r), opened = open == r.id,
                    onHover = { if (!slot.leaving) model.select(slot.index) },
                    onClick = { if (!slot.leaving) { model.select(slot.index); if (open != r.id) model.armAt(r.armed); model.enter(onRun) } },
                    onArm = { if (selected) model.armAt(it) },
                    onAction = { if (selected) { model.armAt(it); model.enter(onRun) } },
                    onCell = { if (selected) model.cellAt(it) else if (!slot.leaving) model.select(slot.index) },
                    onPick = { if (!slot.leaving) { model.select(slot.index); model.cellAt(it); model.armAt(0); model.enter(onRun) } },
                    onToggle = { if (!slot.leaving) { model.select(slot.index); if (open == r.id) model.close() else model.open() } },
                )
                if (mine != null) ActionBlock(
                    mine.second, shown = { block.value.coerceIn(0f, 1f) },
                    // Which of them the pill is on: they follow their row in the list.
                    selected = if (open == r.id) model.selected - slot.index - 1 else -1,
                    onHover = { i -> if (open == r.id) model.select(slot.index + 1 + i) },
                    onClick = { i -> if (open == r.id) { model.select(slot.index + 1 + i); model.enter(onRun) } },
                )
            }
        }
    }
}

/** Places a row, moves it when its slot changes, and plays its arrival and departure. */
@Composable
private fun SlotRow(top: Dp, leaving: Boolean, delayMs: Int, wait: () -> Boolean, onGone: () -> Unit, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    val y by animateDpAsState(top, motion.place(), label = "row")
    val here = remember { Animatable(if (motion.on) 0f else 1f) }
    val gone by rememberUpdatedState(onGone)
    LaunchedEffect(leaving) {
        if (leaving) { here.animateTo(0f, motion.fade(80)); gone() }
        else {
            if (here.value < 1f && wait()) snapshotFlow { wait() }.first { !it }
            if (delayMs > 0) delay(delayMs.toLong())
            here.animateTo(1f, motion.place())
        }
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
 * then gathers itself there. On an action that removes something it takes the error container's
 * colour, as the pane does one level down.
 */
@Composable
fun Pill(top: Dp, height: Dp, visible: Boolean, danger: Boolean = false) {
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
    // The only coloured surface, and the densest: that is what says "selected". Flat, with the panel's white outline.
    val fill by animateColorAsState(
        if (danger) scheme.errorContainer.copy(alpha = if (dark) 0.70f else 0.85f) else scheme.secondaryContainer.copy(alpha = if (dark) 0.66f else 0.78f),
        motion.fade(120), label = "fill",
    )
    Box(
        Modifier.offset { IntOffset(0, upper.value.roundToPx()) }
            .fillMaxWidth().height((lower.value - upper.value).coerceAtLeast(12.dp))
            .graphicsLayer { alpha = shown }
            .clip(shape)
            .background(fill)
            .border(with(LocalDensity.current) { 1f.toDp() }, Color.White.copy(alpha = if (dark) 0.30f else 0.55f), shape),
    )
}

/**
 * An opened row's other actions, under it: each a line of 40 dp with its glyph in the icon column
 * and its name on the title's edge, in the strip's own type. They are the strip's slots, one per
 * line, on the panel's two existing lines; the list's pill is their highlight, and the one it is on
 * shows the Enter mark where the row's arrow stands. Laid out once; [shown] uncovers them from the
 * top down and covers them again.
 */
@Composable
private fun ActionBlock(rows: List<Result>, shown: () -> Float, selected: Int, onHover: (Int) -> Unit, onClick: (Int) -> Unit) {
    val tops = Metrics.tops(rows)
    val soft = with(LocalDensity.current) { 20.dp.toPx() }
    Box(
        Modifier.offset(y = Metrics.row).fillMaxWidth().height(Metrics.listHeight(rows))
            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
            .drawWithContent {
                val edge = size.height * shown()
                if (edge <= 0f) return@drawWithContent
                clipRect(bottom = edge) { this@drawWithContent.drawContent() }
                // The uncovering edge is soft: a row appears over its first lines, not at a cut.
                if (edge < size.height) drawRect(
                    Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = edge - soft, endY = edge),
                    topLeft = Offset(0f, (edge - soft).coerceAtLeast(0f)), size = Size(size.width, soft), blendMode = BlendMode.DstIn,
                )
            },
    ) {
        rows.forEachIndexed { i, r -> ActionRow(r, i == selected, Modifier.offset(y = tops[i]), onHover = { onHover(i) }, onClick = { onClick(i) }) }
    }
}

@Composable
private fun ActionRow(r: Result, selected: Boolean, modifier: Modifier, onHover: () -> Unit, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val a = r.actions.firstOrNull()
    val bad = a?.danger == true
    val ink by animateColorAsState(
        when { bad && selected -> scheme.onErrorContainer; bad -> scheme.error; selected -> scheme.onSurface; else -> scheme.onSurface.copy(alpha = SECOND) },
        motion.fade(120), label = "ink",
    )
    val mark by animateFloatAsState(if (selected) 1f else 0f, motion.fade(120), label = "mark")
    val hover by rememberUpdatedState(onHover)
    Row(
        modifier.fillMaxWidth().height(Metrics.action).clip(RoundedCornerShape(20.dp))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) if (awaitPointerEvent().type == PointerEventType.Move) hover() } }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = selected; role = Role.Button; contentDescription = r.title }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The glyph sits in the icon column, the name on the title's edge.
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { Icon(Symbols.of((r.icon as? RowIcon.Symbol)?.name ?: "app"), null, Modifier.size(18.dp), tint = ink) }
        Text(r.title, color = ink, style = LABEL, maxLines = 1, modifier = Modifier.padding(start = 16.dp).weight(1f))
        // Where the row's arrow stands: the Enter mark of the action the pill is on.
        Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) { Icon(Symbols.enter, null, Modifier.size(14.dp).graphicsLayer { alpha = mark }, tint = ink) }
    }
}

@Composable
private fun RowFrame(height: Dp, selected: Boolean, label: String, actions: List<Action>, bare: Boolean, onHover: () -> Unit, onClick: () -> Unit, onAction: (Int) -> Unit, content: @Composable RowScope.() -> Unit) {
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
            .padding(horizontal = if (bare) 0.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun ResultRow(
    r: Result, icons: AppIcons, selected: Boolean, armed: Int, confirming: Boolean, cell: Int,
    /** What the arming can rest on (indices into the row's actions; the number of actions is its arrow), and whether its other actions are listed under it. */
    stops: List<Int> = r.actions.indices.toList(), opened: Boolean = false,
    onHover: () -> Unit, onClick: () -> Unit, onArm: (Int) -> Unit, onAction: (Int) -> Unit, onCell: (Int) -> Unit, onPick: (Int) -> Unit, onToggle: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    // One ink for the whole panel, `onSurface`. On the selection everything is at full strength: the row that
    // matters most must never be the faintest one (the scheme's own on-container ink can be a mid-tone).
    val on = scheme.onSurface
    val dim by animateColorAsState(scheme.onSurface.copy(alpha = if (selected) 1f else SECOND), motion.fade(120), label = "dim")
    val pop by animateFloatAsState(if (selected) 1.06f else 1f, motion.pop(), label = "icon")
    val kind = r.label ?: if (r.provider == "gemini") "Gemini" else kindLabel(r.kind)
    val body = r.body
    val described = stringResource(R.string.a11y_selected, r.title, r.actions.getOrNull(armed)?.label ?: kind)
    RowFrame(Metrics.rowHeight(r), selected, described, r.actions, bare = body is Body.Grid, onHover, onClick, onAction) {
        if (body is Body.Grid) { GridBody(body, cell.coerceAtLeast(0), selected, onCell, onPick); return@RowFrame }
        if (body is Body.Code) QrPlate(body.text, Modifier.padding(start = 4.dp))
        // A swatch is a sample, not a mark: it doesn't swell with the selection.
        else if (r.icon is RowIcon.Swatch) RowPicture(r.icon, icons, dim)
        else Box(Modifier.graphicsLayer { scaleX = pop; scaleY = pop }) { RowPicture(r.icon, icons, dim) }

        when {
            body is Body.Slots -> SlotsBody(body, on)
            body is Body.Code -> Column(Modifier.weight(1f).padding(start = 20.dp, end = 12.dp)) {
                Text(r.title, color = on, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 3, overflow = TextOverflow.Ellipsis)
                r.subtitle?.let { Text(it, color = dim, style = SMALL, maxLines = 1, modifier = Modifier.padding(top = 4.dp)) }
            }
            else -> Column(Modifier.weight(1f).padding(start = 16.dp, end = 12.dp, top = if (r.answer != null && body == null && r.icon !is RowIcon.Swatch) 3.dp else 0.dp)) {
                // A row whose actions copy different forms of one value (a colour) shows the armed form large.
                val big = if (body is Body.Mono) body.text else if (r.icon is RowIcon.Swatch) (r.actions.getOrNull(armed)?.effect as? Effect.CopyText)?.text ?: r.answer else r.answer
                if (big != null) {
                    r.subtitle?.let { Text(it, color = dim, style = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    if (body is Body.Mono) MonoText(big, on)
                    // A changed answer rolls up into place, like a counter.
                    else AnimatedContent(big, transitionSpec = { motion.roll() }, label = "answer") { a ->
                        Text(a, color = on, style = TextStyle(fontFamily = Fonts.round, fontSize = if (a.length > 22) 26.sp else 34.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    // Laid out once at its own width and covered by a fade where the strip takes its room: a name unrolling
                    // beside it never makes the title swap letters for an ellipsis.
                    Box(Modifier.fillMaxWidth().fadeEnd()) {
                        Text(r.title, color = on, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                    }
                    r.subtitle?.let { Text(it, color = dim, style = SMALL, maxLines = if (Metrics.rowHeight(r) > Metrics.row) 2 else 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }

        // A level: the number keeps its place whether the row is selected or not; the track joins it when it is.
        if (body is Body.Level) {
            val track by animateFloatAsState(if (selected) 1f else 0f, motion.fade(110), label = "track")
            // The number belongs to the track: close after it, and a clear step before the actions.
            if (!body.locked && (selected || track > 0f)) LevelTrack(body, on, Modifier.padding(end = 12.dp).graphicsLayer { alpha = track })
            if (!body.locked) LevelNumber(body, if (selected) on else scheme.onSurface.copy(alpha = SECOND), end = !selected)
            Spacer(Modifier.width(if (selected && !body.locked) 16.dp else 0.dp))
        }

        // What the row can do arrives on the selected row; the others say what kind of thing they are.
        // The icons are the actions that are not kept behind the arrow; the last slot is the arrow, or the one of
        // the others that a typed verb named.
        val shown = r.actions.filter { !it.more }
        val more = r.actions.any { it.more }
        val tenth = stops.lastOrNull()?.takeIf { more }?.let { r.actions.getOrNull(it) }
        val slot = when { more && (armed == r.actions.size || r.actions.getOrNull(armed)?.more == true) -> shown.size; else -> shown.indexOf(r.actions.getOrNull(armed)).coerceAtLeast(0) }
        fun full(k: Int) = if (k < shown.size) r.actions.indexOf(shown[k]) else stops.lastOrNull() ?: 0
        AnimatedContent(when { selected && r.actions.isNotEmpty() -> 2; opened -> 1; else -> 0 }, transitionSpec = {
            if (targetState == 2) (slideInHorizontally(motion.place()) { it / 4 } + fadeIn(motion.fade(110, 60))) togetherWith fadeOut(motion.fade(60))
            else fadeIn(motion.fade(110)) togetherWith fadeOut(motion.fade(60))
        }, contentAlignment = Alignment.CenterEnd, label = "trail") { state ->
            when {
                // (While this is on its way out the row may already have lost its actions: an empty range must not be coerced into.)
                state == 2 -> ActionStrip(shown, slot, confirming, confirmLabel = stringResource(R.string.confirm_again),
                    onArm = { onArm(full(it)) }, onRun = { if (more && tenth == null && it == shown.size) onToggle() else onAction(full(it)) },
                    more = more, tenth = tenth, opened = opened,
                    moreLabel = stringResource(R.string.action_more), lessLabel = stringResource(R.string.action_less))
                // Its list is open and the pill is on one of its actions: the row keeps only its arrow, turned over, in its place.
                state == 1 -> Box(Modifier.size(32.dp).clip(CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle), contentAlignment = Alignment.Center) {
                    Icon(Symbols.of("more"), null, Modifier.size(18.dp).graphicsLayer { rotationZ = 180f }, tint = scheme.onSurface.copy(alpha = SECOND))
                }
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
        is RowIcon.Swatch -> Box(Modifier.size(size), contentAlignment = Alignment.Center) { Swatch(icon.argb, tinted) }
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
    Kind.SCOPE, Kind.CONTROL, Kind.OTHER, Kind.ACTION -> ""
}
