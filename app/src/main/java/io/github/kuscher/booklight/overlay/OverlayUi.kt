package io.github.kuscher.booklight.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Icon as RowIcon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols

/** The panel's sizes. The window is exactly this big, so its blur and shadow follow the panel. */
object Metrics {
    val width = 720.dp
    val search = 68.dp
    val row = 56.dp
    val answer = 92.dp
    val pad = 8.dp
    val footer = 40.dp
    val radius = 32.dp
    /** Where the panel's top edge sits, as a share of the screen's height: the search field stays put while the list grows down. */
    const val TOP = 0.2f

    fun rowHeight(r: Result): Dp = if (r.answer != null) answer else row

    /** The panel's height for what the model is showing. */
    fun height(m: OverlayModel): Dp {
        val of = m.actionsOf
        val list = when {
            of != null -> rowHeight(of) + row * of.actions.size
            else -> m.results.fold(0.dp) { h, r -> h + rowHeight(r) }
        }
        return if (list == 0.dp) search else search + pad + list + pad + footer
    }
}

@Composable
fun Panel(
    model: OverlayModel,
    icons: AppIcons,
    /** True when the window is blurring what's behind it: the surface can be see-through. */
    glass: Boolean,
    onRun: (Result, Action) -> Unit,
    onClose: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Metrics.radius)
    val focus = remember { FocusRequester() }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    // Text set from outside (the debug hook) lands in the field too.
    LaunchedEffect(model.query) { if (field.text != model.query) field = TextFieldValue(model.query, TextRange(model.query.length)) }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun keys(e: KeyEvent): Boolean {
        if (e.type != KeyEventType.KeyDown) return false
        return when (e.key) {
            Key.DirectionDown -> { model.move(1); true }
            Key.DirectionUp -> { model.move(-1); true }
            Key.Enter, Key.NumPadEnter -> { model.chosen()?.let { onRun(it.first, it.second) }; true }
            Key.Tab -> { if (e.isShiftPressed) model.closeActions() else model.openActions(); true }
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

    Column(
        Modifier.fillMaxSize().clip(shape)
            .background(scheme.surfaceContainerHigh.copy(alpha = if (glass) 0.78f else 1f))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.6f), shape)
            .onPreviewKeyEvent(::keys),
    ) {
        Row(Modifier.fillMaxWidth().height(Metrics.search).padding(start = 24.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Symbols.search, null, Modifier.size(26.dp), tint = scheme.onSurfaceVariant)
            Box(Modifier.weight(1f).padding(start = 16.dp), contentAlignment = Alignment.CenterStart) {
                val style = TextStyle(fontFamily = Fonts.text, fontSize = 24.sp, fontWeight = FontWeight(450), color = scheme.onSurface)
                if (field.text.isEmpty()) {
                    Text(stringResource(R.string.search_hint), style = style.copy(color = scheme.onSurfaceVariant.copy(alpha = 0.7f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                BasicTextField(
                    value = field,
                    onValueChange = { field = it; model.type(it.text) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    singleLine = true,
                    textStyle = style,
                    cursorBrush = SolidColor(scheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Go),
                )
            }
            Keycap("esc")
        }

        val of = model.actionsOf
        if (of != null) {
            Column(Modifier.padding(horizontal = Metrics.pad).padding(top = Metrics.pad)) {
                ResultRow(of, icons, selected = false, hint = null, onHover = {}, onClick = { model.closeActions() })
                of.actions.forEachIndexed { i, a ->
                    ActionRow(a, selected = i == model.actionIndex, onHover = { model.selectAction(i) }, onClick = { onRun(of, a) })
                }
            }
            Footer(model.flash, stringResource(R.string.hint_back))
        } else if (model.results.isNotEmpty()) {
            Column(Modifier.padding(horizontal = Metrics.pad).padding(top = Metrics.pad)) {
                model.results.forEachIndexed { i, r ->
                    ResultRow(r, icons, selected = i == model.selected, hint = r.actions.firstOrNull()?.label,
                        onHover = { model.select(i) }, onClick = { r.actions.firstOrNull()?.let { onRun(r, it) } })
                }
            }
            Footer(model.flash, stringResource(R.string.hint_close), actions = (model.current?.actions?.size ?: 0) > 1)
        }
    }
}

private val DIGITS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)

/** The selected row is a tonal pill; its corners tighten a little while it is the pick (Material 3 Expressive). */
@Composable
private fun RowFrame(selected: Boolean, height: Dp, onHover: () -> Unit, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) scheme.secondaryContainer else Color.Transparent, spring(stiffness = Spring.StiffnessHigh), label = "row")
    val corner by animateDpAsState(if (selected) 20.dp else 28.dp, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium), label = "corner")
    val source = remember { MutableInteractionSource() }
    val hover by rememberUpdatedState(onHover)
    Row(
        Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(corner)).background(bg)
            // Hover selects only when the pointer moves, so a list growing under a resting pointer doesn't steal the selection.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) if (awaitPointerEvent().type == PointerEventType.Move) hover()
                }
            }
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun ResultRow(r: Result, icons: AppIcons, selected: Boolean, hint: String?, onHover: () -> Unit, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val on = if (selected) scheme.onSecondaryContainer else scheme.onSurface
    val dim = if (selected) scheme.onSecondaryContainer.copy(alpha = 0.75f) else scheme.onSurfaceVariant
    RowFrame(selected, Metrics.rowHeight(r), onHover, onClick) {
        RowPicture(r.icon, icons, tinted = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            if (r.answer != null) {
                r.subtitle?.let { Text("$it =", color = dim, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Text(r.answer!!, color = on, style = TextStyle(fontFamily = Fonts.round, fontSize = 34.sp, fontWeight = FontWeight(600), fontFeatureSettings = "tnum"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(r.title, color = on, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                r.subtitle?.let { Text(it, color = dim, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        if (selected && hint != null) {
            Text(hint, color = dim, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 8.dp))
            EnterKey()
        } else {
            Text(kindLabel(r.kind), color = dim.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight(400)))
        }
    }
}

@Composable
private fun ActionRow(a: Action, selected: Boolean, onHover: () -> Unit, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    RowFrame(selected, Metrics.row, onHover, onClick) {
        Spacer(Modifier.width(50.dp))
        Text(a.label, Modifier.weight(1f), color = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)))
        if (selected) EnterKey()
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
        is RowIcon.Symbol -> Box(Modifier.size(size).clip(CircleShape).background(tinted.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Symbols.of(icon.name), null, Modifier.size(20.dp), tint = tinted)
        }
    }
}

@Composable
private fun Footer(flash: String?, escLabel: String, actions: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(Metrics.footer + Metrics.pad).padding(start = 24.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(flash ?: "", Modifier.weight(1f), color = scheme.primary, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (actions) {
                Keycap("tab")
                Text(stringResource(R.string.hint_actions), color = scheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 10.dp))
            }
            Keycap("esc")
            Text(escLabel, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Keycap(label: String) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.clip(RoundedCornerShape(7.dp)).background(scheme.onSurface.copy(alpha = 0.08f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(label, color = scheme.onSurfaceVariant, style = TextStyle(fontFamily = Fonts.text, fontSize = 12.sp, fontWeight = FontWeight(500)))
    }
}

/** The Enter key, on the selected row. */
@Composable
private fun EnterKey() {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.clip(RoundedCornerShape(7.dp)).background(scheme.onSecondaryContainer.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 4.dp)) {
        Icon(Symbols.enter, null, Modifier.size(15.dp), tint = scheme.onSecondaryContainer)
    }
}

@Composable
private fun kindLabel(k: Kind): String = when (k) {
    Kind.APP -> stringResource(R.string.kind_app)
    Kind.SETTING -> stringResource(R.string.kind_setting)
    Kind.COMMAND -> stringResource(R.string.kind_command)
    Kind.WEB -> stringResource(R.string.kind_web)
    Kind.ANSWER -> stringResource(R.string.kind_answer)
    Kind.OTHER -> ""
}
