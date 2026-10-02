package io.github.kuscher.booklight.window

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalRippleThemeConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleThemeConfiguration
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.ui.Symbols

/** A control is this high, whichever it is: a button of a choice, the button that opens a menu, Find, New. */
val CONTROL = 40.dp

/** A row's text keeps at least this much room when its choice stands beside it; with less, the choice goes under the text. */
private val TEXT_ROOM = 200.dp

/** How wide each of [names] is in [style], in dp: known before anything is drawn. */
@Composable
private fun widths(names: List<String>, style: TextStyle): List<Dp> {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(names, style, density) { names.map { with(density) { measurer.measure(it, style, maxLines = 1, softWrap = false).size.width.toDp() } + 1.dp } }
}

/** A row's mark by its name, in Material's leading icon size. */
@Composable
fun MarkIcon(name: String) = Icon(Symbols.of(name), null, Modifier.size(MARK))

/**
 * A row that is on or off: Material's switch at its trailing end. The whole row flips it, and so do Enter and Space.
 * Left and Right do not: on a row without a choice Left goes back to the rail, on every page alike.
 */
@Composable
fun Toggle(page: Page, key: String, title: String, about: String?, on: Boolean, mark: String? = null, place: Place? = null, onChange: (Boolean) -> Unit) {
    PageRow(page, key, title, about, mark = mark?.let { m -> { MarkIcon(m) } }, onEnter = { onChange(!on) }, place = place) { SwitchMark(on) }
}

/** Material's switch as a row's control: the row is what is pressed, so the switch only shows and says its state. */
@Composable
fun SwitchMark(on: Boolean, enabled: Boolean = true) {
    Switch(on, onCheckedChange = null, enabled = enabled, modifier = Modifier.semantics { role = Role.Switch; toggleableState = ToggleableState(on) })
}

/**
 * A row whose control is a choice of two to four: Material's connected button group, the chosen one fully
 * round and in the selection's colour. It stands at the row's trailing end while the row's text keeps
 * [TEXT_ROOM] beside it; in a narrower row it stands under the text, on the text's edge. Left and Right on
 * the row step through it, and Enter goes to the next one. The pointer presses a button, never the row: a click
 * beside the buttons, or in the 2 dp between them, changes nothing.
 */
@Composable
fun Choice(
    page: Page, key: String, title: String, about: String?, ids: List<String>, labels: List<Int>, chosen: String, mark: String? = null, place: Place? = null,
    /** Where the group stands, when the page decides it for all its choices together; else the row decides for itself. */
    beside: Boolean? = null,
    /** How wide each button is, when the page gives all its choices one width so that their seams stand in line; else each is as wide as its name needs. */
    cell: Dp? = null,
    onPick: (String) -> Unit,
) {
    val at = ids.indexOf(chosen).coerceAtLeast(0)
    val names = labels.map { stringResource(it) }
    val each = widths(names, MaterialTheme.typography.labelLarge)
    val gaps = ButtonGroupDefaults.ConnectedSpaceBetween * (names.size - 1)
    // What the row has for its text and its control together, after its own edges and its mark.
    val room = LocalColumn.current - INSET * 2 - (if (mark != null) MARK + 12.dp else 0.dp)
    @Suppress("NAME_SHADOWING") val beside = beside ?: besideText(labels, mark != null)
    val group: @Composable () -> Unit = {
        // Under the text, names that are too long for the row give up some of the room round them, never a letter.
        val pad = if (beside) CHOICE_PAD else ((room - each.fold(0.dp) { a, b -> a + b } - gaps) / (2 * names.size)).coerceIn(6.dp, CHOICE_PAD)
        Box(Modifier.padding(top = if (beside) 0.dp else 8.dp, bottom = if (beside) 0.dp else 2.dp)) { Connected(names, at, pad, cell = cell.takeIf { beside }) { onPick(ids[it]) } }
    }
    PageRow(page, key, title, about, mark = mark?.let { m -> { MarkIcon(m) } }, onStep = { d -> ids.getOrNull(at + d)?.let { onPick(it); true } ?: false }, onEnter = { onPick(ids[(at + 1) % ids.size]) },
        below = if (beside) null else group, press = false, place = place, trailing = if (beside) ({ group() }) else null)
}

/**
 * Whether a choice of these names has room at the trailing end of its row, beside the row's text. A page asks
 * this for all its choices and puts them all in the same place: a page where some stand beside their text and
 * some under it has no line that its controls share.
 */
@Composable
fun besideText(labels: List<Int>, mark: Boolean, cell: Dp? = null): Boolean {
    val names = labels.map { stringResource(it) }
    val each = widths(names, MaterialTheme.typography.labelLarge)
    val gaps = ButtonGroupDefaults.ConnectedSpaceBetween * (names.size - 1)
    val full = if (cell != null) cell * names.size + gaps else each.fold(0.dp) { a, b -> a + b } + CHOICE_PAD * 2 * names.size + gaps
    val room = LocalColumn.current - INSET * 2 - (if (mark) MARK + 12.dp else 0.dp)
    return room - 12.dp - full >= TEXT_ROOM
}

/**
 * One width for every button of a page's choices: the widest of all their names, with the room round it. With it
 * the seams of the groups stand on a few lines, one under the other, where widths taken from each name leave them
 * a pixel or two apart.
 */
@Composable
fun choiceCell(groups: List<List<Int>>): Dp {
    val names = groups.flatten().map { stringResource(it) }
    return widths(names, MaterialTheme.typography.labelLarge).max() + CHOICE_PAD * 2
}

/**
 * Whether a row's line under its name still stands on one line beside a group of [count] buttons that are each
 * [cell] wide. Look's lines are one line each; buttons of one width must not cost a row its second line.
 */
@Composable
fun lineFits(text: String?, count: Int, cell: Dp, mark: Boolean = true): Boolean {
    if (text == null) return true
    val group = cell * count + ButtonGroupDefaults.ConnectedSpaceBetween * (count - 1)
    val room = LocalColumn.current - INSET * 2 - (if (mark) MARK + 12.dp else 0.dp) - 16.dp - group
    return widths(listOf(text), MaterialTheme.typography.bodyMedium).single() <= room
}

/** The room on either side of a name in a choice's button. */
private val CHOICE_PAD = 16.dp

/**
 * Material's connected button group: toggle buttons 2 dp apart, the outer ends round, the inner corners 8 dp,
 * and the chosen one round all the way. On a row ([onCard]) the others are in the ground's colour; on the
 * ground itself they are in the rows' colour. [stops]: each button is a stop for the keys itself (in an
 * editor, where Tab goes through everything); on a page it is not: its row is. A button that is a stop has
 * the keys once it is pressed, by the pointer too: what it changes may take away the field that had them.
 */
@Composable
fun Connected(names: List<String>, chosen: Int, pad: Dp = CHOICE_PAD, onCard: Boolean = true, cell: Dp? = null, stops: Boolean = false, onPick: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = ToggleButtonDefaults.colors(containerColor = if (onCard) scheme.ground else scheme.card, contentColor = scheme.onSurfaceVariant, checkedContainerColor = scheme.secondaryContainer, checkedContentColor = scheme.onSecondaryContainer)
    val focus = remember(names.size) { List(names.size) { FocusRequester() } }
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        names.forEachIndexed { i, name ->
            ToggleButton(
                i == chosen, { if (stops) runCatching { focus[i].requestFocus() }; onPick(i) },
                // The row (or the stop) it stands in is what the keys work; the buttons are for the pointer.
                (if (cell != null) Modifier.width(cell) else Modifier).height(CONTROL).focusRequester(focus[i]).focusProperties { canFocus = stops }.semantics { role = Role.RadioButton },
                shapes = when (i) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); names.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() },
                colors = colors, contentPadding = PaddingValues(horizontal = if (cell != null) 0.dp else pad),
            ) { Text(name, maxLines = 1, softWrap = false) }
        }
    }
}

/**
 * A row whose control is a choice of five or more: a button that shows the chosen name and opens a menu of
 * all of them. The button is as wide as the longest name needs, so it never changes size, and the menu is
 * exactly as wide: its edges are the button's, each name starts where the button's does, and the chosen
 * one's check is under the button's arrow. Enter or Space on the row opens it, with the keys on the chosen
 * name; arrows move, Enter chooses, Escape closes it and the keys are on the row again. Left and Right on the
 * row step through the names without opening it.
 */
@Composable
fun Pick(
    page: Page, key: String, title: String, about: String?, ids: List<String>, names: List<String>, chosen: String, mark: String? = null, place: Place? = null,
    onPick: (String) -> Unit,
) {
    val at = ids.indexOf(chosen).coerceAtLeast(0)
    var open by remember { mutableStateOf(false) }
    val width = menuWidth(names)
    val room = LocalColumn.current - INSET * 2 - (if (mark != null) MARK + 12.dp else 0.dp)
    // The button is narrow enough to stay at the row's trailing end down to the window's smallest width, on the line
    // the switches under it end on: the row's name keeps its one line beside it, and the line under the name wraps.
    val beside = room - 12.dp - width >= 120.dp
    val button: @Composable () -> Unit = {
        Box(Modifier.padding(top = if (beside) 0.dp else 8.dp, bottom = if (beside) 0.dp else 2.dp)) {
            MenuButton(names, at, open, { if (it) page.selected = key; open = it }) { onPick(ids[it]) }
        }
    }
    PageRow(page, key, title, about, mark = mark?.let { m -> { MarkIcon(m) } }, onStep = { d -> ids.getOrNull(at + d)?.let { onPick(it); true } ?: false }, onEnter = { open = true },
        below = if (beside) null else button, place = place, trailing = if (beside) ({ button() }) else null)
}

/** How wide the button of a menu of these names is: 16, the longest name, 8, the arrow (or the check) of 20, 16. */
@Composable
fun menuWidth(names: List<String>): Dp = widths(names, MaterialTheme.typography.labelLarge).max() + 60.dp

/**
 * The button of a choice of five or more: it shows the chosen name and opens a menu of all of them, exactly as
 * wide as itself, with the keys on the chosen name. [stop]: it is a stop for the keys itself (in an editor,
 * where Tab goes through everything), and has them once it is pressed; on a page it is not: its row is.
 */
@Composable
fun MenuButton(names: List<String>, at: Int, open: Boolean, onOpen: (Boolean) -> Unit, stop: Boolean = false, onPick: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val width = menuWidth(names)
    val focus = remember { FocusRequester() }
    Box {
        Button(
            { if (stop) runCatching { focus.requestFocus() }; onOpen(true) }, Modifier.width(width).height(CONTROL).focusRequester(focus).focusProperties { canFocus = stop },
            colors = ButtonDefaults.buttonColors(containerColor = scheme.ground, contentColor = scheme.onSurface), contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
        ) {
            Text(names[at], Modifier.weight(1f), maxLines = 1, softWrap = false)
            val turn by animateFloatAsState(if (open) 180f else 0f, LocalMotion.current.pop(), label = "arrow")
            Icon(Symbols.of("more"), null, Modifier.size(20.dp).graphicsLayer { rotationZ = turn }, tint = scheme.onSurfaceVariant)
        }
        Menu(open, { onOpen(false) }, width = width) {
            val first = remember { FocusRequester() }
            // The keys are on the chosen name when it opens: Down and Up go on from there.
            LaunchedEffect(Unit) { withFrameNanos { }; runCatching { first.requestFocus() } }
            names.forEachIndexed { i, name ->
                SelectableDropdownMenuItem(
                    selected = i == at, onClick = { onOpen(false); onPick(i) }, text = { Text(name, maxLines = 1) }, shapes = MenuDefaults.itemShape(i, names.size),
                    modifier = if (i == at) Modifier.focusRequester(first) else Modifier,
                    trailingContent = if (i == at) ({ Icon(Symbols.check, null, Modifier.size(20.dp)) }) else null,
                    colors = MenuDefaults.selectableItemColors(containerColor = scheme.card, selectedContainerColor = scheme.secondaryContainer, selectedTextColor = scheme.onSecondaryContainer, selectedTrailingContentColor = scheme.onSecondaryContainer),
                )
            }
        }
    }
}

/**
 * A line of text to type into, as a row's control: it stands under the row's text, on the text's edge, and ends on
 * the line the rows' controls end on. [CONTROL] high, a pill in the ground's colour like the menu's button. While
 * it has the keys it has the ring an editor's field has, 2 dp in `primary`. What Enter, Escape and Tab do is the
 * row's to say; the window must be told when it has the keys ([onFocus]), because the arrows are then its own.
 */
@Composable
fun Field(
    value: String, onChange: (String) -> Unit, hint: String, focus: FocusRequester, modifier: Modifier = Modifier,
    /** Something before the text: a mark that says what the field holds. */
    leading: (@Composable () -> Unit)? = null,
    onFocus: (Boolean) -> Unit, onEnter: () -> Unit, onEscape: () -> Unit, onTab: (back: Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    var focused by remember { mutableStateOf(false) }
    val ring by animateFloatAsState(if (focused) 1f else 0f, motion.fade(120), label = "field")
    val style = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface)
    // What it holds, with its caret after the text when the text comes from outside (emptied by Escape or by Save).
    var held by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (held.text != value) held = TextFieldValue(value, TextRange(value.length))
    Row(
        modifier.height(CONTROL).clip(CircleShape).background(scheme.ground).border(2.dp, scheme.primary.copy(alpha = ring), CircleShape)
            .clickable(interactionSource = null, indication = null) { runCatching { focus.requestFocus() } }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        leading?.invoke()
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(hint, style = style.copy(color = scheme.onSurfaceVariant), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                held, { held = it; onChange(it.text) },
                Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { focused = it.isFocused; onFocus(it.isFocused) }.onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.Enter, Key.NumPadEnter -> { if (e.nativeKeyEvent.repeatCount == 0) onEnter(); true }
                        Key.Escape -> { onEscape(); true }
                        Key.Tab -> { onTab(e.isShiftPressed); true }
                        else -> false
                    }
                },
                singleLine = true, textStyle = style, cursorBrush = SolidColor(scheme.primary),
                // What goes in here is a key: the input method is told so, and does not learn it or correct it.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
            )
        }
    }
}

/**
 * A menu under what it belongs to (the layout it is called in): Material's popup with one group in it, 4 dp
 * below its anchor and starting on the anchor's leading edge, kept inside the window. With a [width] it is
 * exactly that wide. It is flat like everything else in the window: the rows' colour and a 1 dp outline, no
 * shadow (Material's shadow was the only one in the window in light, and in dark the menu had no edge at all).
 * Inside it Material's own focus mark is drawn again: a menu's keys are Material's.
 */
@Composable
fun Menu(open: Boolean, onClose: () -> Unit, width: Dp? = null, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenuPopup(open, onClose, if (width != null) Modifier.width(width) else Modifier, MenuDefaults.rememberDropdownMenuPopupPositionProvider(MenuAnchorPosition.Below, DpOffset(0.dp, 4.dp))) {
        CompositionLocalProvider(LocalRippleThemeConfiguration provides RippleThemeConfiguration(RippleThemeConfiguration.Focus.Opacity), LocalRippleConfiguration provides RippleConfiguration()) {
            val scheme = MaterialTheme.colorScheme
            DropdownMenuGroup(MenuDefaults.groupShape(0, 1), containerColor = scheme.card, tonalElevation = 0.dp, shadowElevation = 0.dp, border = BorderStroke(1.dp, scheme.outlineVariant), content = content)
        }
    }
}

/** A menu item's colours: on the menu's own fill (Material gives each item a fill of its own, a shade off the menu's), and in the error colour for what removes. */
@Composable
fun menuItemColors(danger: Boolean = false): MenuItemColors {
    val scheme = MaterialTheme.colorScheme
    return (if (danger) MenuDefaults.itemColors(textColor = scheme.error) else MenuDefaults.itemColors()).copy(containerColor = scheme.card)
}

/** What a right-click on a row offers, where the pointer was. Something that removes is last and in the error colour. */
@Composable
fun ContextMenu(actions: List<RowAction>, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Menu(true, onClose) {
        actions.forEachIndexed { i, a ->
            DropdownMenuItem(onClick = { onClose(); a.run() }, text = { Text(a.label, maxLines = 1) }, shape = MenuDefaults.itemShape(i, actions.size).shape,
                colors = menuItemColors(a.danger))
        }
    }
}

/** What a button with only a mark on it is for, shown above it when the pointer rests on it. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Tip(text: String, content: @Composable () -> Unit) {
    TooltipBox(TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above), { PlainTooltip { Text(text) } }, rememberTooltipState()) { content() }
}
