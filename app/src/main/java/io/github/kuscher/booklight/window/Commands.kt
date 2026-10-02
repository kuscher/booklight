package io.github.kuscher.booklight.window

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalRippleThemeConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleThemeConfiguration
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Read
import io.github.kuscher.booklight.core.Requests
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Schemes
import io.github.kuscher.booklight.core.Templates
import io.github.kuscher.booklight.data.PromptEntry
import io.github.kuscher.booklight.data.RecipeEntry
import io.github.kuscher.booklight.data.Recipes
import io.github.kuscher.booklight.data.Settings
import io.github.kuscher.booklight.data.SiteEntry
import io.github.kuscher.booklight.data.SnippetEntry
import io.github.kuscher.booklight.data.StepEntry
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.OverlayActivity
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The Commands page's own state, kept by the window: the keys reach it from any section (Ctrl + F, Ctrl + N),
 * and it is still there when the section is left and opened again.
 */
class CommandsState {
    /** Which half of the list shows: what the user made, else what is built in. */
    var yours by mutableStateOf(false)
    /** What the Find field holds: the list shows only what it matches. */
    var find by mutableStateOf("")
    /** The Find field has the keys. */
    var finding by mutableStateOf(false)
    /** The Find field is to take the keys: it does, and clears this. */
    var ask by mutableStateOf(false)
    private var opened by mutableStateOf<Pair<String, String>?>(null)
    /** What is being edited: its kind and its id; an empty id is a new one. Another one (or none) starts with nothing typed. */
    var open: Pair<String, String>?
        get() = opened
        set(value) { if (value != opened) { draft.clear(); steps = null; discard = false; focused = null }; opened = value }
    /** The field of the open editor that has the keys (its label): when the editor is drawn in its other place, that field has them again. */
    var focused: String? = null
    /** Bumped when the field that has the keys is taken away with them (its editor was drawn in two places for a moment): the same field of the other one takes them. */
    var refocus by mutableIntStateOf(0)

    /**
     * What the open editor holds and has not saved: its fields by name, and a recipe's steps. It is kept here and
     * not in the editor, because the editor is drawn under its row or in the second pane and changes its place when
     * the window crosses 1332 dp: what was typed must still be there. A field is in it only while it differs from
     * what is saved.
     */
    val draft = mutableStateMapOf<String, String>()
    var steps by mutableStateOf<List<StepEntry>?>(null)
    /** Something in the open editor was changed and is not saved. (What is typed to look for a step, pasted to be read, or typed to try a command with is not a change.) */
    val dirty: Boolean get() = steps != null || draft.keys.any { it !in SCRATCH }
    /** Leaving a changed editor was asked for once: Cancel says so, and the next time it is left. */
    var discard by mutableStateOf(false)
    /** The row of what was just saved: the keys go to it when its editor has closed. */
    var saved: String? = null

    /** A field of the open editor: what was typed into it, else what is saved. */
    fun field(name: String, saved: String): String = draft[name] ?: saved
    fun type(name: String, value: String, saved: String) { if (value == saved) draft.remove(name) else draft[name] = value; discard = false }

    /**
     * Closes the open editor, or opens [next] in its place. If something in it was changed, the first time only
     * asks (Cancel reads "Press again to discard" for three seconds) and the second time does it. True if it was done.
     */
    fun leave(next: Pair<String, String>? = null): Boolean {
        if (opened != null && opened != next && dirty && !discard) { discard = true; return false }
        open = next
        return true
    }

    /** A new link, snippet, recipe, prompt or app command: its editor opens at the end of its group. */
    fun new(kind: String) { if (leave(kind to "")) { yours = true; find = "" } }

    companion object {
        const val SEARCH = "search"
        const val PASTE = "paste"
        const val TRY = "try"
        /** What an editor holds that is no part of what it saves. */
        private val SCRATCH = setOf(SEARCH, PASTE, TRY)
    }
}

/**
 * Commands: everything Booklight does (the same table as the list behind `?`, each row with its example;
 * Enter opens the panel and Booklight types it) and, in its other half, what the user made: links, snippets,
 * recipes, prompts and app commands, each opening its editor in place. One switch between the two, one field that
 * narrows whichever shows, and one button that makes a new one.
 */
@Composable
fun CommandsPage(
    page: Page, app: BooklightApp, s: Settings, cs: CommandsState,
    /** What the panel asked to be edited ("Edit…"); consumed. */
    edit: Pair<String, String>?, onEdited: () -> Unit,
    /** An editor is open: the keys are its own. */
    onTyping: (Boolean) -> Unit,
    /** An editor is opening: it starts this far down the page and is this high by now. */
    onGrow: (top: Float, height: Float) -> Unit,
    arrive: Animatable<Float, *>, from: Float,
) {
    val motion = LocalMotion.current
    LaunchedEffect(edit) { if (edit != null) { cs.yours = true; cs.find = ""; cs.open = edit; page.selected = "${edit.first}:${edit.second}"; delay(1600); onEdited() } }
    // While an editor is open the keys are its own (Tab goes from field to field and to its buttons, Esc closes it);
    // when it closes, the page has them again.
    LaunchedEffect(cs.open) { onTyping(cs.open != null) }
    DisposableEffect(Unit) { onDispose { onTyping(false) } }
    // "Press again to discard" stands for three seconds.
    LaunchedEffect(cs.discard) { if (cs.discard) { delay(3000); cs.discard = false } }
    /** The other half of the list, unless an editor with something unsaved is in the way. True if it changed. */
    fun half(yours: Boolean): Boolean { if (yours == cs.yours || !cs.leave()) return false; cs.yours = yours; return true }
    Rise(arrive, 1, from) {
        // The page's own controls, on one line: which half shows, starting on the column's edge, and (for the user's own)
        // the button that makes a new one, ending on the column's other edge.
        Row(Modifier.fillMaxWidth().padding(top = 20.dp).height(CONTROL), verticalAlignment = Alignment.CenterVertically) {
            Stop(page, "half", onEnter = { half(!cs.yours) }, onStep = { d -> half(d > 0) }) {
                Connected(listOf(stringResource(R.string.win_builtin), stringResource(R.string.nav_yours)), if (cs.yours) 1 else 0, onCard = false) { page.keys = false; half(it == 1) }
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(cs.yours, enter = fadeIn(motion.fade(140)), exit = fadeOut(motion.fade(70))) { New(page, cs) }
        }
    }
    Rise(arrive, 2, from) {
        // One half gives way to the other where it stands; the page takes the new half's height on its spring.
        AnimatedContent(cs.yours, transitionSpec = { (fadeIn(motion.fade(140, 60)) togetherWith fadeOut(motion.fade(70))).using(SizeTransform(clip = false) { _, _ -> motion.place() }) }, contentAlignment = Alignment.TopStart, label = "half") { yours ->
            if (yours) Yours(page, app, s, cs, onTyping, onGrow) else BuiltIn(page, app, s, cs)
        }
    }
}

/**
 * New link, and an arrow for the other four: Material's split button. Enter on it is a new link; Right opens the
 * arrow's menu. In a column under 400 dp (the smallest window) the first button is only its plus, with its name as
 * a tooltip: beside Built in · Yours the words would leave 4 dp between the two.
 */
@Composable
private fun New(page: Page, cs: CommandsState) {
    var menu by remember { mutableStateOf(false) }
    val more = stringResource(R.string.win_new_more_all)
    val name = stringResource(R.string.win_new_link)
    val short = LocalColumn.current < 400.dp
    Stop(page, "new", onEnter = { cs.new("quicklink") }, onStep = { d -> if (d > 0) { menu = true; true } else false }) {
        SplitButtonLayout(
            leadingButton = {
                val button: @Composable () -> Unit = {
                    SplitButtonDefaults.TonalLeadingButton(onClick = { page.keys = false; cs.new("quicklink") }, modifier = Modifier.focusProperties { canFocus = false }.semantics { if (short) contentDescription = name }) {
                        Icon(Symbols.of("plus"), null, Modifier.size(SplitButtonDefaults.LeadingIconSize))
                        if (!short) {
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(name, maxLines = 1, softWrap = false)
                        }
                    }
                }
                if (short) Tip(name, button) else button()
            },
            trailingButton = {
                Box {
                    Tip(more) {
                        SplitButtonDefaults.TonalTrailingButton(menu, { menu = it }, Modifier.focusProperties { canFocus = false }.semantics { contentDescription = more }) {
                            val turn by animateFloatAsState(if (menu) 180f else 0f, LocalMotion.current.pop(), label = "arrow")
                            Icon(Symbols.of("more"), null, Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer { rotationZ = turn })
                        }
                    }
                    Menu(menu, { menu = false }) {
                        val kinds = listOf("snippet" to R.string.win_new_snippet, "recipe" to R.string.win_new_recipe, "prompt" to R.string.win_new_prompt, "appcommand" to R.string.win_new_app_command)
                        val first = remember { FocusRequester() }
                        LaunchedEffect(Unit) { withFrameNanos { }; runCatching { first.requestFocus() } }
                        kinds.forEachIndexed { i, (kind, name) ->
                            DropdownMenuItem(onClick = { menu = false; cs.new(kind) }, text = { Text(stringResource(name), maxLines = 1) }, shape = MenuDefaults.itemShape(i, kinds.size).shape, colors = menuItemColors(),
                                modifier = if (i == 0) Modifier.focusRequester(first) else Modifier)
                        }
                    }
                }
            },
        )
    }
}

/**
 * Find a command: a rounded field at the end of the title's line, 264 wide, ending on the column's edge. In
 * a narrow window it is a round button there that opens over the title when it is pressed (or Ctrl + F), and
 * closes when it is left empty. Down, Enter and Tab leave it for the list's first row; Escape empties it, and
 * leaves it when it is empty.
 */
@Composable
fun Find(page: Page, cs: CommandsState, compact: Boolean, /** The keys go back to the page. */ onLeave: (Leave) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val field = remember { FocusRequester() }
    val open = !compact || cs.finding || cs.ask || cs.find.isNotEmpty()
    val width by animateDpAsState(if (!open) CONTROL else if (compact) LocalColumn.current else 264.dp, motion.place(), label = "find")
    // Closed to a round button the field has no width, and nothing without a width can take the keys: it opens first.
    LaunchedEffect(cs.ask) { if (cs.ask) { withFrameNanos { }; withFrameNanos { }; runCatching { field.requestFocus() }; cs.ask = false } }
    // What it holds, with its caret. Text that comes from outside (a letter typed on the page) leaves the caret after it.
    var value by remember { mutableStateOf(TextFieldValue(cs.find, TextRange(cs.find.length))) }
    if (value.text != cs.find) value = TextFieldValue(cs.find, TextRange(cs.find.length))
    val style = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface)
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val hover by animateFloatAsState(if (hovered && !cs.finding) 0.08f else 0f, motion.fade(120), label = "hover")
    // While it has the keys it has the ring an editor's field has: the window's own ring is off while text is typed.
    val ring by animateFloatAsState(if (cs.finding) 1f else 0f, motion.fade(120), label = "ring")
    val label = stringResource(R.string.win_find)
    Stop(page, "find", onEnter = { cs.ask = true }) {
        val body: @Composable () -> Unit = {
            Row(
                Modifier.width(width).height(CONTROL).clip(CircleShape).background(scheme.card).background(scheme.onSurface.copy(alpha = hover)).border(2.dp, scheme.primary.copy(alpha = ring), CircleShape).hoverable(source)
                    .clickable(interactionSource = null, indication = null) { page.keys = false; cs.ask = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(CONTROL), contentAlignment = Alignment.Center) { Icon(Symbols.search, label, Modifier.size(20.dp), tint = scheme.onSurfaceVariant) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (cs.find.isEmpty()) Text(label, style = style.copy(color = scheme.onSurfaceVariant), maxLines = 1, softWrap = false)
                    BasicTextField(
                        value, { value = it.copy(text = it.text.take(60)); cs.find = value.text },
                        Modifier.fillMaxWidth().focusRequester(field).onFocusChanged { cs.finding = it.isFocused }.onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (e.key) {
                                Key.Escape -> { if (cs.find.isNotEmpty()) cs.find = "" else onLeave(Leave.STAY); true }
                                Key.DirectionDown, Key.Enter, Key.NumPadEnter -> { onLeave(Leave.LIST); true }
                                Key.Tab -> { onLeave(Leave.NEXT); true }
                                else -> false
                            }
                        },
                        singleLine = true, textStyle = style, cursorBrush = SolidColor(scheme.primary),
                    )
                }
                // What opens it from anywhere, as key caps, while it is empty and there is room for them.
                if (!compact) {
                    val caps by animateFloatAsState(if (cs.find.isEmpty() && !cs.finding) 1f else 0f, motion.fade(120), label = "caps")
                    Row(Modifier.padding(start = 8.dp, end = 8.dp).graphicsLayer { alpha = caps }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Cap(stringResource(R.string.key_ctrl), scheme.onSurfaceVariant); Cap("F", scheme.onSurfaceVariant)
                    }
                } else Spacer(Modifier.width(12.dp))
            }
        }
        // Closed to a round button it has only its mark: the pointer resting on it says what it is.
        if (compact && !open) Tip(label, body) else body()
    }
}

/** Where the keys go when they leave Find: to the list's first row (Down, Enter), to the next stop (Tab), or nowhere: Find's own stop (Escape). */
enum class Leave { LIST, NEXT, STAY }

/** Whether a row of the list is one Find is looking for. */
private fun matches(find: String, vararg text: String?): Boolean = find.isEmpty() || text.any { it != null && it.contains(find, ignoreCase = true) }

/** How many rows of a long list are composed per frame. */
private const val SLICE = 8

/**
 * The built-in half: the list of everything, in its groups. Each row's example stands at its trailing end, in
 * the fixed-width cut, with room after it for the Enter mark that the row with the keys shows; in a narrow row
 * the example stands under the text. A right-click offers to try it and to copy its example.
 */
@Composable
private fun BuiltIn(page: Page, app: BooklightApp, s: Settings, cs: CommandsState) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val activity = LocalActivity.current as ComponentActivity
    val entries = remember { app.guide.entries() }
    val find = cs.find.trim()
    val shown = remember(entries, find) { entries.filter { matches(find, it.name, it.line, it.example, app.guide.group(it.group)) } }
    // Some thirty rows: a few in the frame the page comes in, the rest over the next frames, so the rail's indicator, which
    // is still travelling, does not lose frames to them. (Rows further down are below the window's edge at first anyway.)
    var count by remember { mutableIntStateOf(SLICE) }
    LaunchedEffect(Unit) { while (count < entries.size) { withFrameNanos { }; count += SLICE } }
    val narrow = LocalColumn.current < ROW_WIDE
    val tryIt = stringResource(R.string.win_try)
    val copy = stringResource(R.string.win_copy_example)
    val mono = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = MaterialTheme.typography.bodySmall.fontSize, color = scheme.onSurface)
    Column {
        if (shown.isEmpty()) NoMatch(page, cs, find, there = yours(s, app, find).size, other = stringResource(R.string.nav_yours))
        for ((group, rows) in shown.take(count).groupBy { it.group }) key(group) {
            Group(app.guide.group(group)) {
                for (e in rows) row(e.id) { place ->
                    val key = "guide:${e.id}"
                    val run = { app.example = e.example.take(200); activity.startActivity(Intent(activity, OverlayActivity::class.java).setAction(OverlayActivity.ACTION_PANEL)) }
                    // The example is what would be typed, so in full ink.
                    val example: @Composable () -> Unit = { Text(e.example.trim(), style = mono, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = if (narrow) 2.dp else 0.dp).widthIn(max = 240.dp)) }
                    PageRow(page, key, e.name, e.line.ifEmpty { null }, mark = { MarkIcon(e.symbol) }, place = place, onEnter = run, below = if (narrow) example else null,
                        menu = listOf(RowAction(tryIt, run = run), RowAction(copy) { app.executor.run(Effect.CopyText(e.example.trim()), activity) })) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!narrow) example()
                            // The Enter mark ends every command's row on the line the other rows' controls end on: quiet, and in
                            // full ink on the row the keys are on. (Its glyph has 2 dp of air in its box: the box stands 2 dp over the line.)
                            val mark by animateFloatAsState(if (page.keys && page.selected == key) 1f else 0f, motion.fade(120), label = "mark")
                            Box(Modifier.size(MARK), contentAlignment = Alignment.CenterEnd) { Icon(Symbols.enter, null, Modifier.size(16.dp).offset(x = 2.dp), tint = lerp(scheme.onSurfaceVariant.copy(alpha = 0.6f), scheme.onSurface, mark)) }
                        }
                    }
                }
            }
        }
    }
}

/** Find matches nothing in the half that shows; if it does in the other, a row that goes there. */
@Composable
private fun NoMatch(page: Page, cs: CommandsState, find: String, there: Int, other: String) {
    Group {
        row("none") { PageNote(it, stringResource(R.string.win_find_none, find)) }
        if (there > 0) row("other") { place ->
            PageRow(page, "find:other", other, pluralStringResource(R.plurals.win_find_there, there, there), place = place, mark = { MarkIcon("search") },
                onEnter = { if (cs.leave()) cs.yours = !cs.yours }) { Icon(Symbols.of("arrow"), null, Modifier.size(20.dp).graphicsLayer { rotationZ = 180f }) }
        }
    }
}

/** The keys of the user's own rows that Find matches, in the order of the page. */
private fun yours(s: Settings, app: BooklightApp, find: String): List<String> =
    s.sites.filter { matches(find, it.name, it.keyword, it.url) }.map { "quicklink:${it.keyword}" } +
        s.snippets.filter { matches(find, it.key, it.text) }.map { "snippet:${it.key}" } +
        s.recipes.filter { matches(find, it.name, it.keyword, Recipes.describe(app, it)) }.map { "recipe:${it.id}" } +
        s.prompts.filter { matches(find, it.name, it.keyword, it.text) }.map { "prompt:${it.id}" } +
        s.ownCommands.filter { matches(find, it.name, it.keyword, app.apps.installed(it.app)?.label) }.map { "appcommand:${it.id}" }

/** One of the user's own, as its row shows it and as the page works on it. */
private class Mine(
    val id: String, val title: String, val line: String?, val symbol: String, /** Its keyword, as a key cap. */ val cap: String,
    val editor: @Composable () -> Unit, /** Makes a copy and says its id. */ val duplicate: () -> String, val delete: () -> Unit,
)

/** A kind of thing the user makes: its group's name, what there is of it, and the editor of a new one. */
private class Kind(val kind: String, val heading: Int, /** What one of them is called, and a new one. */ val one: Int, val fresh: Int, val items: List<Mine>, val new: @Composable () -> Unit)

/** A name that nothing in [taken] has: [base], or [base] with the first number that is free. */
private fun free(base: String, taken: Set<String>): String = generateSequence(2) { it + 1 }.map { base + it }.first { it.lowercase() !in taken }

/**
 * Everything the user made, kind by kind, with each one's editor and what duplicating and deleting it does. The
 * list shows it, and so does the second pane (the editor of the one that is open).
 */
private fun kinds(app: BooklightApp, s: Settings, cs: CommandsState): List<Kind> {
    /** After Save or Delete: the editor goes, whatever it held. [row] is the saved one's row, where the keys go. */
    fun close(row: String? = null) { cs.saved = row; cs.open = null }
    /** Cancel and Escape: not at once if something was changed. */
    fun cancel() { cs.leave() }
    fun set(change: (Settings) -> Settings) = app.prefs.update(change)
    // A link can't have a keyword one of Booklight's own scopes answers to: it would never be reached. Nor one a prompt has.
    val words = s.prompts.mapNotNull { it.keyword.lowercase().takeIf { k -> k.isNotEmpty() } }.toSet()
    val recipeWords = s.recipes.mapNotNull { it.keyword.lowercase().takeIf { k -> k.isNotEmpty() } }.toSet()
    val commandWords = s.ownCommands.mapNotNull { it.keyword.lowercase().takeIf { k -> k.isNotEmpty() } }.toSet()
    val taken = s.sites.map { it.keyword.lowercase() }.toSet() + app.scopes.reserved + words + recipeWords + commandWords
    // A prompt's keyword must be free of links, recipes, Booklight's own and the other prompts.
    val used = taken + recipeWords
    val snippetKeys = s.snippets.map { it.key.lowercase() }.toSet()
    return listOf(
        Kind("quicklink", R.string.win_links, R.string.win_kind_link, R.string.win_new_link, s.sites.map { link ->
            Mine(link.keyword, link.name, link.url, if (Templates.takesArgument(link.url)) "search" else "link", link.keyword,
                editor = {
                    LinkEditor(cs, link, taken,
                        onSave = { new -> set { st -> st.copy(sites = st.sites.map { if (it.keyword == link.keyword) new else it }) }; close("quicklink:${new.keyword}") },
                        onDelete = { set { st -> st.copy(sites = st.sites.filter { it.keyword != link.keyword }) }; close() }, onClose = ::cancel)
                },
                duplicate = { free(link.keyword, taken).also { k -> set { st -> st.copy(sites = st.sites + link.copy(keyword = k)) } } },
                delete = { set { st -> st.copy(sites = st.sites.filter { it.keyword != link.keyword }) } })
        }) { LinkEditor(cs, null, taken, onSave = { new -> set { it.copy(sites = it.sites + new) }; close("quicklink:${new.keyword}") }, onDelete = null, onClose = ::cancel) },
        Kind("snippet", R.string.snip_name, R.string.win_kind_snippet, R.string.win_new_snippet, s.snippets.map { snip ->
            Mine(snip.key, snip.key, snip.text.lineSequence().first(), "text", "",
                editor = {
                    SnippetEditor(cs, snip, snippetKeys,
                        onSave = { new -> set { st -> st.copy(snippets = st.snippets.map { if (it.key == snip.key) new else it }) }; close("snippet:${new.key}") },
                        onDelete = { set { st -> st.copy(snippets = st.snippets.filter { it.key != snip.key }) }; close() }, onClose = ::cancel)
                },
                duplicate = { free(snip.key, snippetKeys).also { k -> set { st -> st.copy(snippets = st.snippets + snip.copy(key = k)) } } },
                delete = { set { st -> st.copy(snippets = st.snippets.filter { it.key != snip.key }) } })
        }) { SnippetEditor(cs, null, snippetKeys, onSave = { new -> set { it.copy(snippets = it.snippets + new) }; close("snippet:${new.key}") }, onDelete = null, onClose = ::cancel) },
        Kind("recipe", R.string.win_recipes, R.string.win_kind_recipe, R.string.win_new_recipe, s.recipes.map { recipe ->
            Mine(recipe.id, recipe.name, Recipes.describe(app, recipe), "bolt", recipe.keyword,
                editor = {
                    RecipeEditor(cs, app, recipe, taken,
                        onSave = { new -> set { st -> st.copy(recipes = st.recipes.map { if (it.id == recipe.id) new else it }) }; close("recipe:${new.id}") },
                        onDelete = { set { st -> st.copy(recipes = st.recipes.filter { it.id != recipe.id }) }; close() }, onClose = ::cancel)
                },
                // A copy has no keyword: a keyword is one thing's alone.
                duplicate = { "r${System.currentTimeMillis()}".also { id -> set { st -> st.copy(recipes = st.recipes + recipe.copy(id = id, keyword = "")) } } },
                delete = { set { st -> st.copy(recipes = st.recipes.filter { it.id != recipe.id }) } })
        }) { RecipeEditor(cs, app, null, taken, onSave = { new -> set { it.copy(recipes = it.recipes + new) }; close("recipe:${new.id}") }, onDelete = null, onClose = ::cancel) },
        Kind("prompt", R.string.win_prompts, R.string.win_kind_prompt, R.string.win_new_prompt, s.prompts.map { prompt ->
            Mine(prompt.id, prompt.name, prompt.text.replace("{text}", "").lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }, "spark", prompt.keyword,
                editor = {
                    PromptEditor(cs, prompt, used,
                        onSave = { new -> set { st -> st.copy(prompts = st.prompts.map { if (it.id == prompt.id) new else it }) }; close("prompt:${new.id}") },
                        onDelete = { set { st -> st.copy(prompts = st.prompts.filter { it.id != prompt.id }) }; close() }, onClose = ::cancel)
                },
                duplicate = { "p${System.currentTimeMillis()}".also { id -> set { st -> st.copy(prompts = st.prompts + prompt.copy(id = id, keyword = "", seed = null)) } } },
                delete = { set { st -> st.copy(prompts = st.prompts.filter { it.id != prompt.id }) } })
        }) { PromptEditor(cs, null, used, onSave = { new -> set { it.copy(prompts = it.prompts + new) }; close("prompt:${new.id}") }, onDelete = null, onClose = ::cancel) },
        Kind("appcommand", R.string.win_app_commands, R.string.win_kind_app_command, R.string.win_new_app_command, s.ownCommands.map { command ->
            // Its row says which app it asks, and for what.
            val asks = (Requests.read(command.intent) as? Read.Ok)?.request?.let { app.getString(whatName(it.what)) }
            Mine(command.id, command.name, listOfNotNull(app.apps.installed(command.app)?.label ?: command.app, asks).joinToString(" · "), "open", command.keyword,
                editor = {
                    AppCommandEditor(cs, app, command, taken,
                        onSave = { new -> set { st -> st.copy(ownCommands = st.ownCommands.map { if (it.id == command.id) new else it }) }; close("appcommand:${new.id}") },
                        onDelete = { set { st -> st.copy(ownCommands = st.ownCommands.filter { it.id != command.id }) }; close() }, onClose = ::cancel)
                },
                // A copy has no keyword: a keyword is one thing's alone.
                duplicate = { "c${System.currentTimeMillis()}".also { id -> set { st -> st.copy(ownCommands = st.ownCommands + command.copy(id = id, keyword = "")) } } },
                delete = { set { st -> st.copy(ownCommands = st.ownCommands.filter { it.id != command.id }) } })
        }) { AppCommandEditor(cs, app, null, taken, onSave = { new -> set { it.copy(ownCommands = it.ownCommands + new) }; close("appcommand:${new.id}") }, onDelete = null, onClose = ::cancel) },
    )
}

/**
 * The user's half: the links (1.0's keyword searches, now with placeholders), the snippets, the recipes, the
 * prompts and the app commands they made. A row opens its editor in place, under it, and is in the selection's colour while
 * it is open; a new one's editor opens at the end of its group. A right-click offers Edit, Duplicate and
 * Delete. Delete (the key, or the menu) asks once more on the row itself, then deletes: the panel's rule.
 */
@Composable
private fun Yours(page: Page, app: BooklightApp, s: Settings, cs: CommandsState, onTyping: (Boolean) -> Unit, onGrow: (Float, Float) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val find = cs.find.trim()
    val open = cs.open
    /** Escape in an editor: it closes, unless something in it was changed (then the second Escape closes it). */
    fun close() { cs.leave() }
    // In a window with a second pane the editor opens there, beside the list; else under its row.
    val second = LocalSecond.current
    val has = remember(s, find) { yours(s, app, find).toSet() }
    val none = stringResource(R.string.win_none_yet)
    val editWord = stringResource(R.string.win_edit)
    val duplicateWord = stringResource(R.string.win_duplicate)
    val deleteWord = stringResource(R.string.action_delete)
    /** The row that was asked to be deleted and waits for the second press. */
    var armed by remember { mutableStateOf<String?>(null) }
    /** When it was asked: a second press sooner than a bouncing key's does not delete. */
    var asked by remember { mutableStateOf(0L) }
    LaunchedEffect(armed) { if (armed != null) { delay(3000); armed = null } }
    val kinds = kinds(app, s, cs)

    Column {
        if (find.isNotEmpty() && has.isEmpty()) {
            val entries = remember { app.guide.entries() }
            NoMatch(page, cs, find, there = entries.count { matches(find, it.name, it.line, it.example, app.guide.group(it.group)) }, other = stringResource(R.string.win_builtin))
            return@Column
        }
        for (k in kinds) {
            // While Find holds something a group shows only what matches, and is not there at all if nothing does.
            val items = k.items.filter { "${k.kind}:${it.id}" in has }
            if (find.isNotEmpty() && items.isEmpty()) continue
            key(k.kind) {
                Group(stringResource(k.heading)) {
                    for (m in items) {
                        val key = "${k.kind}:${m.id}"
                        row(m.id) { place ->
                            val wait = armed == key
                            val ask = {
                                val now = android.os.SystemClock.uptimeMillis()
                                if (wait && now - asked >= 350) {
                                    // The keys go to the row after it (or the one before): they are never left on nothing.
                                    val order = page.order().map { it.key }
                                    val at = order.indexOf(key)
                                    armed = null; if (open == (k.kind to m.id)) cs.open = null; m.delete()
                                    page.selected = order.getOrNull(at + 1) ?: order.getOrNull(at - 1)
                                } else if (!wait) { armed = key; asked = now }
                            }
                            PageRow(page, key, m.title, m.line, lines = 1, place = place, chosen = open == (k.kind to m.id), mark = { MarkIcon(m.symbol) },
                                // The row that waits for the second press deletes on any press; any other opens its editor.
                                onEnter = { if (wait) ask() else cs.leave(k.kind to m.id) }, onDelete = ask,
                                menu = listOf(RowAction(editWord) { cs.leave(k.kind to m.id) }, RowAction(duplicateWord) { if (cs.leave()) cs.open = k.kind to m.duplicate() }, RowAction(deleteWord, danger = true, run = ask))) {
                                if (wait) Text(stringResource(R.string.confirm_again), color = scheme.error, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                else if (m.cap.isNotEmpty()) Cap(m.cap, scheme.onSurface)
                            }
                        }
                        between("edit:${m.id}") { Editor(page, cs, !second && open == (k.kind to m.id), ::close, onGrow) { m.editor() } }
                    }
                    if (k.items.isEmpty() && find.isEmpty()) row("none") { PageNote(it, none) }
                    between("new") { Editor(page, cs, !second && open == (k.kind to ""), ::close, onGrow) { k.new() } }
                }
            }
        }
    }
}

/** Booklight's two one-letter keywords, which a link or an app command of the user's may take, and the long keyword that then still works. */
internal val LETTERS = mapOf("s" to "settings", "k" to "keys")

/** Inside an editor the keys are Material's own, from field to field and to the buttons: its focus mark is the window's ring, drawn by Material. */
private val RING_THEME = RippleThemeConfiguration(RippleThemeConfiguration.Focus.InsetRing(0.dp, 2.dp, 0.dp, 0.dp))

/** The open editor's first field and its last button: Tab goes round between them and never out of the editor. And the page's state, which knows the field the keys were in. */
internal class Ends(val cs: CommandsState) { val first = FocusRequester(); val last = FocusRequester() }
internal val LocalEnds = compositionLocalOf<Ends?> { null }

/**
 * An editor opens under its row (a new item's at the end of its group) and closes back into it: the page makes
 * room, nothing pops over it. It is a container in the rows' colour with 16 dp corners. While it opens it tells
 * the page how far down it reaches, frame by frame, so the page can keep its lower edge in view as it grows.
 */
@Composable
private fun Editor(page: Page, cs: CommandsState, visible: Boolean, onEscape: () -> Unit, onGrow: (top: Float, height: Float) -> Unit, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    // It comes in on its spring also when it is composed already open.
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    // How high it was when it was last placed: it also tells the page when it grows later (the line that says why it
    // cannot be saved, a recipe's new step), never when the page is only scrolled.
    var high by remember { mutableIntStateOf(0) }
    AnimatedVisibility(
        state,
        Modifier.onGloballyPositioned { c ->
            val grew = c.size.height > high
            high = c.size.height
            if (!state.targetState || (state.isIdle && !grew)) return@onGloballyPositioned
            val top = page.root?.takeIf { it.isAttached }?.localPositionOf(c, Offset.Zero)?.y ?: return@onGloballyPositioned
            onGrow(top, c.size.height.toFloat())
        },
        enter = expandVertically(motion.place()) + fadeIn(motion.fade(140, 60)), exit = shrinkVertically(motion.place()) + fadeOut(motion.fade(80)),
    ) { EditorFrame(cs, 16.dp, onEscape) { content() } }
}

/**
 * An editor's container, in the rows' colour: under a row with 16 dp corners, in the second pane with 28. Escape
 * closes the editor from anywhere in it. Inside it the keys are Material's own, from field to field and to the
 * buttons, and Material draws the focus mark: the same 2 dp ring in `secondary` as the window's. Tab stays in
 * it: from its last button it goes round to its first field, and Shift + Tab the other way. (Left to itself Tab
 * would go on to whatever can take the focus next, with no mark on it and the draft still open.)
 */
@Composable
private fun EditorFrame(cs: CommandsState, corner: Dp, onEscape: () -> Unit, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val ends = remember { Ends(cs) }
    /** Tab is moving the keys: only then are they kept from leaving the editor. (Its other place may ask for them at any time.) */
    var tabbing by remember { mutableStateOf(false) }
    CompositionLocalProvider(
        LocalRippleThemeConfiguration provides RING_THEME, LocalRippleConfiguration provides RippleConfiguration(RippleConfiguration.Focus.InsetRing(scheme.secondary, Color.Transparent)),
        LocalEnds provides ends,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.Escape -> { onEscape(); true }
                        Key.Tab -> {
                            val back = e.isShiftPressed
                            tabbing = true
                            val moved = focusManager.moveFocus(if (back) FocusDirection.Previous else FocusDirection.Next)
                            tabbing = false
                            if (!moved) runCatching { (if (back) ends.last else ends.first).requestFocus() }
                            true
                        }
                        else -> false
                    }
                }
                .focusProperties { onExit = { if (tabbing) cancelFocusChange() } }.focusGroup()
                .clip(RoundedCornerShape(corner)).background(scheme.card).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

/**
 * The Commands page's second pane (a window of 1332 dp or more). On the user's half: the editor of the link,
 * snippet, recipe, prompt or app command that is open, under its name, while its row in the list is in the selection's
 * colour; it fades in and rises 12 dp; while nothing is open, a line that says what the pane is for. On the
 * built-in half: the panel with the example of the command the keys are on (the first one, until they are on
 * one), and under it that command's name.
 */
@Composable
fun CommandsSide(page: Page, app: BooklightApp, s: Settings, cs: CommandsState, @Suppress("UNUSED_PARAMETER") onTyping: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val rise = with(LocalDensity.current) { 12.dp.roundToPx() }
    if (!cs.yours) {
        val entries = remember { app.guide.entries() }
        val on = page.selected?.removePrefix("guide:")
        val entry = entries.firstOrNull { it.id == on } ?: entries.firstOrNull() ?: return
        Beside(stringResource(R.string.win_side_example_of, entry.name, entry.example.trim())) { Stage(app, s, height = 340.dp, top = 56.dp, text = entry.example.trim(), slot = "example") }
        return
    }
    AnimatedContent(cs.open, transitionSpec = { (fadeIn(motion.fade(140, 60)) + slideInVertically(motion.place()) { rise }) togetherWith fadeOut(motion.fade(70)) }, contentAlignment = Alignment.TopStart, label = "editor") { open ->
        if (open == null) {
            // On the title's centre line (the pane starts 20 dp over it, the line is 20 high), on the pane's text edge.
            Text(stringResource(R.string.win_side_none), color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(start = INSET, end = INSET, top = 10.dp))
            return@AnimatedContent
        }
        val kind = kinds(app, s, cs).firstOrNull { it.kind == open.first } ?: return@AnimatedContent
        val mine = kind.items.firstOrNull { it.id == open.second }
        if (mine == null && open.second.isNotEmpty()) return@AnimatedContent
        EditorFrame(cs, 28.dp, { cs.leave() }) {
            Row(Modifier.padding(start = 16.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(mine?.title ?: stringResource(kind.fresh), color = scheme.onSurface, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alignByBaseline().weight(1f, fill = false))
                if (mine != null) Text(stringResource(kind.one), color = scheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.alignByBaseline())
            }
            if (mine != null) mine.editor() else kind.new()
        }
    }
}

/**
 * A labelled line (or a few) of text to edit: Material's filled text field, rounded, on the ground's colour,
 * with a ring in `primary` while it has the keys. Enter in a one-line field saves the editor, and Ctrl + Enter
 * in any field ([onSubmit]; a held key does not).
 */
@Composable
internal fun Input(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, lines: Int = 1, hint: String = "", /** The editor's first field: it has the keys when the editor opens. */ first: Boolean = false, onSubmit: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    val ends = LocalEnds.current
    val focus = remember(ends, first) { if (first && ends != null) ends.first else FocusRequester() }
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val ring by animateFloatAsState(if (focused) 1f else 0f, LocalMotion.current.fade(120), label = "field")
    val shape = RoundedCornerShape(12.dp)
    // The keys are in the editor's first field when it opens. When the editor is drawn in its other place (the window
    // crossed 1332 dp) they are in the field they were in. A field that is not placed yet cannot take them: it is asked
    // again over the next frames.
    val was = ends?.cs?.focused
    if (was == label || (was == null && first)) LaunchedEffect(ends?.cs?.refocus) {
        repeat(40) { withFrameNanos { }; if (runCatching { focus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect }
    }
    // An editor can be drawn twice for a moment (in the second pane and under its row, while one of them goes). If the
    // field that goes has the keys, they would be nowhere: the same field of the one that stays is told to take them.
    var mine by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { if (mine) ends?.cs?.let { it.refocus++ } } }
    // What it holds, with its caret: after the text when the field is made (also when it is made again in the editor's other place).
    var held by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (held.text != value) held = TextFieldValue(value, TextRange(value.length))
    TextField(
        held, { held = it; onChange(it.text) },
        modifier.fillMaxWidth().border(2.dp, scheme.primary.copy(alpha = ring), shape).focusRequester(focus).onFocusChanged { mine = it.isFocused; if (it.isFocused) ends?.cs?.focused = label }.onPreviewKeyEvent { e ->
            val enter = e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)
            if (enter && onSubmit != null && (lines == 1 || e.isCtrlPressed)) { if (e.nativeKeyEvent.repeatCount == 0) onSubmit(); true } else false
        },
        label = { Text(label) }, placeholder = if (hint.isEmpty()) null else ({ Text(hint, maxLines = 1, overflow = TextOverflow.Ellipsis) }),
        singleLine = lines == 1, minLines = lines, shape = shape, interactionSource = source,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.ground, unfocusedContainerColor = scheme.ground,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, errorIndicatorColor = Color.Transparent,
        ),
    )
}

/** A line of help under an editor's fields, on the fields' text edge. */
@Composable
internal fun Help(text: String, strong: Boolean = false, modifier: Modifier = Modifier) {
    Text(text, color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = modifier.padding(horizontal = 16.dp))
}

/**
 * An editor's buttons: Save (Material's filled button), Cancel beside it, and Delete last, apart from them at the
 * other end and in the error colour. Delete asks for a second press. So does Cancel when something was changed.
 * While Save cannot be pressed and something was typed, a line over the buttons says what is missing ([why]).
 */
@Composable
internal fun Buttons(cs: CommandsState, why: Int?, onSave: () -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val ends = LocalEnds.current
    var sure by remember { mutableStateOf(false) }
    LaunchedEffect(sure) { if (sure) { delay(3000); sure = false } }
    // The reason stays readable while its line closes.
    var said by remember { mutableIntStateOf(why ?: 0) }
    if (why != null) said = why
    val last = if (ends != null) Modifier.focusRequester(ends.last) else Modifier
    Column {
        AnimatedVisibility(why != null && cs.dirty, enter = expandVertically(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkVertically(motion.place()) + fadeOut(motion.fade(70))) {
            if (said != 0) Help(stringResource(said), strong = true, modifier = Modifier.padding(bottom = 12.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onSave, enabled = why == null) { Text(stringResource(R.string.action_save)) }
            TextButton(onClose, if (onDelete == null) last else Modifier) { Text(stringResource(if (cs.discard) R.string.win_discard_again else R.string.hint_cancel), maxLines = 1) }
            Spacer(Modifier.weight(1f))
            // Its word ends 16 dp inside the fields' edge, where the fields' own text starts on the other side.
            if (onDelete != null) TextButton({ if (sure) { sure = false; onDelete() } else sure = true }, last, colors = ButtonDefaults.textButtonColors(contentColor = scheme.error), contentPadding = PaddingValues(horizontal = 16.dp)) {
                Text(stringResource(if (sure) R.string.confirm_again else R.string.action_delete), maxLines = 1)
            }
        }
    }
}

/** What keeps an address from being saved, as the line over an editor's buttons says it; null if nothing does. */
internal fun whyNot(why: Schemes.Why?): Int? = when (why) {
    Schemes.Why.NONE -> R.string.win_why_scheme
    Schemes.Why.REFUSED -> R.string.win_why_scheme_refused
    Schemes.Why.EMPTY -> R.string.win_why_address_empty
    null -> null
}

/** A link is a keyword nobody else has, a name and an address. What it holds while it is edited is the window's ([CommandsState.draft]). */
@Composable
private fun LinkEditor(cs: CommandsState, initial: SiteEntry?, taken: Set<String>, onSave: (SiteEntry) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    val was = initial ?: SiteEntry("", "", "https://")
    val keyword = cs.field("keyword", was.keyword)
    val name = cs.field("name", was.name)
    val url = cs.field("url", was.url)
    // What keeps it from being saved, in the order of its fields.
    val why = when {
        keyword.isEmpty() -> R.string.win_why_keyword
        ' ' in keyword -> R.string.win_why_space
        keyword.lowercase() in taken && !keyword.equals(initial?.keyword, ignoreCase = true) -> R.string.win_why_taken
        name.isBlank() -> R.string.win_why_name
        // Any scheme will do (an app's own opens in that app), but for a few that are never a link.
        else -> whyNot(Schemes.refused(url))
    }
    val save = { if (why == null) onSave(SiteEntry(keyword, name.trim(), Schemes.tidy(url))) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Input(stringResource(R.string.set_site_keyword), keyword, { cs.type("keyword", it.trim().take(16), was.keyword) }, Modifier.weight(1f), hint = "jira", first = true, onSubmit = save)
        Input(stringResource(R.string.set_site_name), name, { cs.type("name", it.take(40), was.name) }, Modifier.weight(2f), hint = "Jira", onSubmit = save)
    }
    Input(stringResource(R.string.win_link_address), url, { cs.type("url", it.trim(), was.url) }, hint = "https://example.com/browse/{argument}", onSubmit = save)
    Help(stringResource(R.string.win_link_help))
    Help(stringResource(R.string.win_link_app))
    // A link may take one of Booklight's two letters; the long keyword still reaches what the letter did.
    LETTERS[keyword.lowercase()]?.let { long -> Help(stringResource(R.string.win_link_letter, keyword.lowercase(), long), strong = true) }
    Buttons(cs, why, save, onDelete, onClose)
}

@Composable
private fun SnippetEditor(cs: CommandsState, initial: SnippetEntry?, taken: Set<String>, onSave: (SnippetEntry) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    val was = initial ?: SnippetEntry("", "")
    val key = cs.field("key", was.key)
    val text = cs.field("text", was.text)
    val why = when {
        key.isEmpty() -> R.string.win_why_name
        ' ' in key -> R.string.win_why_name_space
        key.lowercase() in taken && !key.equals(initial?.key, ignoreCase = true) -> R.string.win_why_name_taken
        text.isBlank() -> R.string.win_why_text
        else -> null
    }
    val save = { if (why == null) onSave(SnippetEntry(key, text)) }
    Input(stringResource(R.string.slot_name), key, { cs.type("key", it.trim().take(24), was.key) }, hint = "sig", first = true, onSubmit = save)
    Input(stringResource(R.string.slot_text), text, { cs.type("text", it, was.text) }, lines = 3, onSubmit = save)
    Buttons(cs, why, save, onDelete, onClose)
}

/** A prompt is a name, a keyword (it can do without: it is then found by its name) and the text the model is given. */
@Composable
private fun PromptEditor(cs: CommandsState, initial: PromptEntry?, taken: Set<String>, onSave: (PromptEntry) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    val was = initial ?: PromptEntry("", "", "", "")
    val name = cs.field("name", was.name)
    val keyword = cs.field("keyword", was.keyword)
    val text = cs.field("text", was.text)
    val why = when {
        name.isBlank() -> R.string.win_why_name
        ' ' in keyword -> R.string.win_why_space
        keyword.isNotEmpty() && keyword.lowercase() in taken && !keyword.equals(initial?.keyword, ignoreCase = true) -> R.string.win_why_taken
        text.isBlank() -> R.string.win_why_text
        else -> null
    }
    val save = { if (why == null) onSave(PromptEntry(initial?.id ?: "p${System.currentTimeMillis()}", name.trim(), keyword, text.trim())) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Input(stringResource(R.string.slot_name), name, { cs.type("name", it.take(28), was.name) }, Modifier.weight(2f), hint = stringResource(R.string.win_prompt_name_hint), first = true, onSubmit = save)
        Input(stringResource(R.string.set_site_keyword), keyword, { cs.type("keyword", it.trim().take(16), was.keyword) }, Modifier.weight(1f), hint = "nice", onSubmit = save)
    }
    Input(stringResource(R.string.win_prompt_text), text, { cs.type("text", it.take(2000), was.text) }, lines = 4, hint = stringResource(R.string.win_prompt_text_hint), onSubmit = save)
    Help(stringResource(R.string.win_prompt_help))
    Buttons(cs, why, save, onDelete, onClose)
}

/**
 * A recipe is a name, a keyword and steps. A step is picked the way anything is picked in
 * Booklight: type into the step field and choose a row's action (an app, maybe in a place; a link;
 * a settings page; `vol 30`). Only things that are safe to do unasked can be steps.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeEditor(cs: CommandsState, app: BooklightApp, initial: RecipeEntry?, taken: Set<String>, onSave: (RecipeEntry) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val was = initial ?: RecipeEntry("", "")
    val name = cs.field("name", was.name)
    val keyword = cs.field("keyword", was.keyword)
    val steps = cs.steps ?: was.steps
    fun steps(new: List<StepEntry>) { cs.steps = new.takeIf { it != was.steps }; cs.discard = false }
    // What is typed to look for a step: kept like the fields, but it is no change to the recipe.
    val find = cs.field(CommandsState.SEARCH, "")
    var found by remember { mutableStateOf<List<Result>>(emptyList()) }
    LaunchedEffect(find) {
        delay(120)
        found = if (find.isBlank()) emptyList() else withContext(Dispatchers.Default) {
            val scoped = app.engine.scopeFor(find)
            val rows = if (scoped != null) app.engine.search(Query(scoped.text, scoped.scope.key, scoped.word), 6) else app.engine.search(Query(find), 6)
            rows.filter { r -> r.actions.any { Recipes.step(it.effect, "") != null } }.take(4)
        }
    }
    // A recipe can do without a keyword; one it has is its own alone.
    val why = when {
        name.isBlank() -> R.string.win_why_name
        ' ' in keyword -> R.string.win_why_space
        keyword.isNotEmpty() && keyword.lowercase() in taken && !keyword.equals(initial?.keyword, ignoreCase = true) -> R.string.win_why_taken
        steps.isEmpty() -> R.string.win_why_step
        else -> null
    }
    val save = { if (why == null) onSave(RecipeEntry(initial?.id ?: "r${System.currentTimeMillis()}", name.trim(), keyword, steps)) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Input(stringResource(R.string.slot_name), name, { cs.type("name", it.take(40), was.name) }, Modifier.weight(2f), hint = stringResource(R.string.win_recipe_name_hint), first = true, onSubmit = save)
        Input(stringResource(R.string.set_site_keyword), keyword, { cs.type("keyword", it.trim().take(16), was.keyword) }, Modifier.weight(1f), hint = "work", onSubmit = save)
    }
    Help(stringResource(R.string.win_recipe_steps))
    steps.forEachIndexed { i, step ->
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Cap("${i + 1}", scheme.onSurface)
            Text(step.label.ifEmpty { step.kind }, color = scheme.onSurface, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            TextButton({ steps(steps.filterIndexed { j, _ -> j != i }) }) { Text(stringResource(R.string.set_site_remove)) }
        }
    }
    // Enter here is not Save: this field looks for a step.
    Input(stringResource(R.string.win_recipe_add_step), find, { cs.type(CommandsState.SEARCH, it, "") }, lines = 1, hint = stringResource(R.string.win_recipe_step_hint), onSubmit = null)
    for (r in found) {
        FlowRow(Modifier.padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(r.title, color = scheme.onSurface, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp))
            for (a in r.actions) {
                val label = "${r.title}: ${a.label}"
                val step: StepEntry = Recipes.step(a.effect, label) ?: continue
                FilledTonalButton({ steps(steps + step); cs.type(CommandsState.SEARCH, "", "") }) { Text(a.label, maxLines = 1) }
            }
        }
    }
    Buttons(cs, why, save, onDelete, onClose)
}
