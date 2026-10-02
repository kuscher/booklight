package io.github.kuscher.booklight.window

import androidx.compose.animation.ExitTransition
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.Menu
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.data.Settings
import io.github.kuscher.booklight.overlay.LocalDark
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.Motion
import io.github.kuscher.booklight.ui.BooklightTheme
import io.github.kuscher.booklight.ui.isDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The Booklight window: what Booklight is, how to give it a key, everything it does, every setting,
 * and what the user made. It is what the app's icon opens (the keyboard shortcut opens the panel).
 * An ordinary desktop window in Material 3 Expressive: the sections in a navigation rail on its leading
 * edge (a bar along the bottom when it is narrow), and one page beside it whose column starts at the rail
 * and is never centred. The system's caption bar is see-through, so the window is one surface from its
 * top edge.
 */
class MainActivity : ComponentActivity() {
    /** What the panel asked to be edited (`snippet`, `quicklink`, `recipe`, `prompt`) and which one; consumed by the page. */
    private var edit by mutableStateOf<Pair<String, String>?>(null)
    /** The section the panel asked for ("All commands"); consumed by the window. */
    private var goto by mutableStateOf<String?>(null)
    /** What the window is showing, in a line: for the debug hooks. */
    var probe: () -> String = { "" }
    /** Puts the pointer over a row, or takes it off again, as far as the row can tell (`hover KEY`, `unhover KEY`), or shows it pressed (`press KEY`, `release KEY`): for pictures of those states. */
    var poke: (String) -> String = { "" }
    /**
     * Notes the layout of every frame for a while and hands the lines to [done]: how wide the window and the rail are,
     * where the page's column and its title's line start, whether the bar and the second pane are there. For looking at
     * a resize across a breakpoint frame by frame without a recording of the screen.
     */
    var trace: (frames: Int, done: (List<String>) -> Unit) -> Unit = { _, _ -> }
    /** How often the system has asked this window for its keys (the Keyboard Shortcuts Helper does, when it opens). */
    var asked = 0
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) take(intent)     // not again when the window is only rebuilt
        current = java.lang.ref.WeakReference(this)
        val app = application as BooklightApp
        val motion = Motion.of(this)
        setContent {
            val s by app.prefs.state.collectAsState()
            val dark = isDark(s.theme)
            // The caption bar is the window's own ground, and its marks are light or dark as Booklight is, not as the system is.
            SideEffect { caption(dark) }
            BooklightTheme(dark, tint = s.tint) {
                // A pointer's targets are what is drawn: Material adds no unseen room round a small control here.
                CompositionLocalProvider(LocalMotion provides motion, LocalDark provides dark, LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Window(app, s, edit, goto, onEdited = { edit = null }, onGone = { goto = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    /** The window's keys, for the system's Keyboard Shortcuts Helper (the Start page's "Open Keyboard shortcuts" shows it). */
    override fun onProvideKeyboardShortcuts(data: MutableList<KeyboardShortcutGroup>, menu: Menu?, deviceId: Int) {
        super.onProvideKeyboardShortcuts(data, menu, deviceId)
        asked++
        val ctrl = KeyEvent.META_CTRL_ON
        val keys = Part.entries.mapIndexed { i, part -> KeyboardShortcutInfo(getString(part.title), KeyEvent.KEYCODE_1 + i, ctrl) } + listOf(
            KeyboardShortcutInfo(getString(R.string.win_find), KeyEvent.KEYCODE_F, ctrl),
            KeyboardShortcutInfo(getString(R.string.win_new_link), KeyEvent.KEYCODE_N, ctrl),
            KeyboardShortcutInfo(getString(R.string.win_keys_region), KeyEvent.KEYCODE_F6, 0),
            KeyboardShortcutInfo(getString(R.string.win_keys_close), KeyEvent.KEYCODE_W, ctrl),
        )
        data.add(KeyboardShortcutGroup(getString(R.string.app_name), keys))
    }

    /** The system draws its chip and its three controls over what Booklight draws; nothing of Booklight's is in the bar. */
    private fun caption(dark: Boolean) {
        if (Build.VERSION.SDK_INT < 35) return
        val clear = WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
        val light = WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS
        window.insetsController?.setSystemBarsAppearance(if (dark) clear else clear or light, clear or light)
    }

    private fun take(intent: Intent) {
        goto = intent.getStringExtra(EXTRA_PAGE)
        val kind = intent.getStringExtra(EXTRA_EDIT) ?: return
        edit = kind to intent.getStringExtra(EXTRA_ID).orEmpty()
    }

    companion object {
        const val EXTRA_EDIT = "edit"
        const val EXTRA_ID = "id"
        /** Which section to open at: `commands`; [PAGE_FLIGHTS] is Results, on the row where the flight service's key is set. */
        const val EXTRA_PAGE = "page"
        const val PAGE_FLIGHTS = "flights"
        const val PRIVACY_URL = "https://googlebook.studio/privacy/booklight"
        /** The window that is open, for pictures of it (debug builds). */
        var current: java.lang.ref.WeakReference<MainActivity> = java.lang.ref.WeakReference(null)
    }
}

@Composable
private fun Window(app: BooklightApp, s: Settings, edit: Pair<String, String>?, goto: String?, onEdited: () -> Unit, onGone: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val activity = LocalActivity.current as ComponentActivity
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // The panels the stages show (the demo, the preview, an example): the window's, so that a stage can change its place.
    val stages = remember { Stages(app, scope) }
    stages.Demo()
    val page = remember { Page() }
    val focus = remember { FocusRequester() }
    val width = windowWidth()
    val compact = width == Width.COMPACT
    /**
     * The section the rail says is open, and the one whose page is on screen: the page follows a moment later. The
     * window opens on the section it was left on (a first run: Start), unless the panel asked for one.
     */
    var part by remember { mutableStateOf(Part.of(goto) ?: if (goto == MainActivity.PAGE_FLIGHTS) Part.RESULTS else if (edit != null) Part.COMMANDS else Part.of(s.windowPart) ?: Part.START) }
    var shown by remember { mutableStateOf(part) }
    val rail = remember { NavState(part, bar = false) }
    val bar = remember { NavState(part, bar = true) }
    /** The keys are in the rail (or the bar); else in the page. */
    var inNav by remember { mutableStateOf(false) }
    /** The keys are what was used last, not the pointer: the focus ring shows. */
    var byKeys by remember { mutableStateOf(false) }
    /** The keys have just gone into the page: a row is to be selected once the page has its rows. */
    var entering by remember { mutableStateOf(false) }
    /** The window was opened on a row (the flight key's, from the panel): it is brought into view with room round it. */
    var landing by remember { mutableStateOf(false) }
    /** The row that was selected in each section, for coming back to it. */
    val picked = remember { HashMap<Part, String?>() }
    /** Which way the last change of section went: the new page comes from that side. */
    var down by remember { mutableStateOf(true) }
    /** The Commands page's half, its Find and its open editor. */
    val commands = remember { CommandsState() }
    /** A text field has the keyboard (an editor's, or Find): the arrow keys are its own. */
    var editing by remember { mutableStateOf(false) }
    val typing = editing || commands.finding
    var viewport by remember { mutableIntStateOf(0) }
    /** The window has a second pane beside the column. */
    var beside by remember { mutableStateOf(false) }
    /** The window has had its size for a moment ([LocalSettled]). */
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(500); settled = true }
    /** Where the pane is in the window: the ring of a row is cut where the page is. */
    var pane by remember { mutableStateOf(Rect.Zero) }
    val scrolls = remember { Part.entries.associateWith { ScrollState(0) } }
    /** The row that Enter or Space is holding down, to let go when the key comes up. */
    var held by remember { mutableStateOf<Pair<MutableInteractionSource, PressInteraction.Press>?>(null) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    // What needs a look when the window comes back from Settings, the folder picker or the panel.
    // (A desktop window that stays in view behind the panel is not paused: getting the keys back counts as well.)
    var resumed by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { resumed++; onPauseOrDispose { } }
    val windowFocus = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocus) { if (windowFocus) resumed++ }
    /** "Open Keyboard shortcuts" was pressed: the line about the key says what to do next, whichever section was looked at in between. */
    val askedForKey = remember { mutableStateOf(false) }
    // The rows know whether the ring is on the page: the one it is on takes its focused shape.
    SideEffect { page.keys = byKeys && !inNav && !typing && windowFocus }

    fun go(to: Part) {
        if (to == part) return
        // An editor with something unsaved in it is not left at once: the first time only asks (its Cancel says so).
        if (!commands.leave()) return
        picked[part] = page.selected
        page.selected = null
        down = to.ordinal > part.ordinal
        part = to
        app.prefs.update { it.copy(windowPart = to.id) }
    }
    // The panel's two ways in. "All commands": the Commands section, built in, with the keys in Find. "Edit…": the
    // Commands section on the user's own, with that item's editor open (the page does that part).
    LaunchedEffect(goto) {
        // "Set up times" on a flight's row: Results, with the keys on the row where the key goes.
        if (goto == MainActivity.PAGE_FLIGHTS) { picked[Part.RESULTS] = FLIGHT_KEY_ROW; landing = true; go(Part.RESULTS); inNav = false; entering = true; byKeys = true; onGone(); return@LaunchedEffect }
        val to = Part.of(goto) ?: return@LaunchedEffect
        go(to); inNav = false
        if (to == Part.COMMANDS) { commands.yours = false; commands.ask = true } else entering = true
        onGone()
    }
    LaunchedEffect(edit) { if (edit != null) { go(Part.COMMANDS); inNav = false } }
    // The page follows the rail's indicator a moment after it last moved: a held arrow shows no pages in between.
    LaunchedEffect(part) { if (shown != part) { delay(motion.hold(60)); shown = part } }

    /** Keeps a stop in view after a key, with [room] around it so the next row shows too. Something higher than the window shows its top. */
    fun reveal(e: Page.Entry, room: Dp = 96.dp) {
        val scroll = scrolls.getValue(shown)
        val margin = with(density) { room.toPx() }
        val top = e.top - margin
        val bottom = e.top + e.height + margin
        // A stop near the page's top brings the title with it: the page is never left a few lines down. (Only on the way
        // up: on the way down a short step is a step, or a row just under the window's edge would never come into view.)
        val near = with(density) { 120.dp.toPx() }
        val to = when {
            top < scroll.value || bottom - top > viewport -> if (top < near) 0f else top
            bottom > scroll.value + viewport -> bottom - viewport
            else -> return
        }
        scope.launch { scroll.animateScrollTo(to.toInt().coerceIn(0, scroll.maxValue), motion.place()) }
    }
    /**
     * An editor is opening under its row: the page goes with it, frame by frame, so its lower edge stays in view while it
     * grows; never further than its upper edge allows.
     */
    fun follow(top: Float, height: Float) {
        val scroll = scrolls.getValue(shown)
        val margin = with(density) { 16.dp.toPx() }
        val over = top + height + margin - (scroll.value + viewport)
        val room = top - margin - scroll.value
        if (over > 0f && room > 0f) scroll.dispatchRawDelta(minOf(over, room))
    }
    // Into the page: the row that was selected there, else the first one. Not before the page that was there has
    // gone (it fades for 70 ms and its rows are still known until then) and the new one has its rows.
    LaunchedEffect(entering, shown) {
        if (!entering || shown != part) return@LaunchedEffect
        delay(motion.hold(100))
        snapshotFlow { page.placed }.first { page.rows.values.any { it.height > 0 } }
        entering = false
        val want = picked[part]
        if (want != null && page.rows[want] != null) page.selected = want else { page.selected = null; page.move(1) }
        // A page that is entered stays where it is, title and all, unless its row is out of view. A row the panel sent
        // the window to is shown with what stands round it.
        page.current?.let { reveal(it, if (landing) 96.dp else 0.dp) }
        landing = false
    }

    LaunchedEffect(commands.find, commands.yours) {
        if (page.selected == null || commands.finding) return@LaunchedEffect
        delay(motion.hold(120))
        page.current?.let { reveal(it) }
    }

    // An editor has closed. After Save the keys go to the row of what was saved (a new one's, or the row a new keyword
    // or name gave it). With the keys on nothing (a new command's editor, cancelled) they go to New, which opens one.
    var edited by remember { mutableStateOf(false) }
    LaunchedEffect(commands.open) {
        val closed = edited && commands.open == null
        edited = commands.open != null
        val saved = commands.saved
        commands.saved = null
        if (!closed || !commands.yours || part != Part.COMMANDS) return@LaunchedEffect
        delay(motion.hold(120))
        if (saved != null) snapshotFlow { page.placed }.first { page.rows[saved]?.let { it.height > 0 } == true || page.rows[saved] == null }
        if (saved != null && page.rows[saved] != null) page.selected = saved
        else if (page.selected?.let { page.rows[it] } == null) page.selected = "new"
        page.current?.let { reveal(it) }
    }

    /** Enter or Space is down on a row: Material shows it pressed until the key comes up. */
    fun press(e: Page.Entry) {
        val source = e.source ?: return
        val press = PressInteraction.Press(Offset(e.width / 2, e.height / 2))
        scope.launch { source.emit(press) }
        held = source to press
    }
    fun release() { held?.let { (source, press) -> scope.launch { source.emit(PressInteraction.Release(press)) } }; held = null }

    val hovers = remember { HashMap<String, HoverInteraction.Enter>() }
    val presses = remember { HashMap<String, PressInteraction.Press>() }
    SideEffect {
        (activity as? MainActivity)?.poke = { what ->
            val key = what.substringAfter(' ')
            val source = page.rows[key]?.source
            when {
                source == null -> "no such row"
                what.startsWith("hover ") -> { val h = HoverInteraction.Enter(); hovers[key] = h; scope.launch { source.emit(h) }; "ok" }
                // Shown as pressed, not run: for a picture of that state.
                what.startsWith("press ") -> { val p = PressInteraction.Press(Offset(page.rows[key]!!.width / 2, page.rows[key]!!.height / 2)); presses[key] = p; scope.launch { source.emit(p) }; "ok" }
                what.startsWith("release ") -> { presses.remove(key)?.let { scope.launch { source.emit(PressInteraction.Release(it)) } }; "ok" }
                else -> { hovers.remove(key)?.let { scope.launch { source.emit(HoverInteraction.Exit(it)) } }; "ok" }
            }
        }
        (activity as? MainActivity)?.trace = { frames, done ->
            scope.launch {
                val lines = ArrayList<String>()
                var t0 = 0L
                repeat(frames) {
                    withFrameNanos { now ->
                        if (t0 == 0L) t0 = now
                        val d = density.density
                        lines += "${(now - t0) / 1_000_000}ms window=${(pane.right / d).toInt()} rail=${(rail.width / d).toInt()} bar=${if (compact) 1 else 0} column@${(page.origin.x / d * 10).toInt() / 10f} title@${(page.origin.y / d * 10).toInt() / 10f} second=${if (beside) 1 else 0}"
                    }
                }
                done(lines)
            }
        }
        (activity as? MainActivity)?.probe = {
            "width=$width part=${part.id} shown=${shown.id} keys=${if (commands.finding) "find" else if (editing) "field" else if (inNav) "nav" else "page"}${if (byKeys) "+ring" else ""} row=${page.selected} scroll=${scrolls.getValue(shown).value}" +
                " half=${if (commands.yours) "yours" else "builtin"} find='${commands.find}' open=${commands.open} rows=${page.rows.size}${if (beside) " second-pane" else ""}"
        }
    }

    Box(
        Modifier.fillMaxSize().background(scheme.ground).safeDrawingPadding()
            // Any press of the pointer puts the ring away: it shows where the keys are, while the keys are what is used.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) byKeys = false } }
            .focusRequester(focus).focusable()
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyUp) { if (e.key == Key.Enter || e.key == Key.NumPadEnter || e.key == Key.Spacebar) release(); return@onPreviewKeyEvent false }
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val again = e.nativeKeyEvent.repeatCount > 0     // a held key runs a row once
                if (e.isCtrlPressed) return@onPreviewKeyEvent when (e.key) {
                    // Find a command, and a new link, from any section.
                    Key.F -> { if (!again) { go(Part.COMMANDS); inNav = false; commands.ask = true }; true }
                    Key.N -> { if (!again) { go(Part.COMMANDS); inNav = false; commands.new("quicklink") }; true }
                    Key.W -> { if (!again && commands.leave()) activity.finishAndRemoveTask(); true }
                    // Ctrl + 1 … 5: the sections.
                    // (Not while an editor with something unsaved keeps the page: then the section has not changed.)
                    else -> Part.entries.getOrNull(DIGITS.indexOf(e.key))?.let { if (!again) { go(it); if (part == it) { inNav = false; entering = true; byKeys = true } }; true } ?: false
                }
                // A key in a text field is the field's own. The keys are what is used: the ring is there when the field is left.
                if (typing) { byKeys = true; return@onPreviewKeyEvent false }
                // F6: between the navigation and the page.
                if (e.key == Key.F6) { if (!again) { byKeys = true; if (inNav) { inNav = false; entering = true } else { picked[part] = page.selected; inNav = true } }; return@onPreviewKeyEvent true }
                val at = part.ordinal
                // Along the navigation: down and up the rail, right and left along the bar.
                val next = if (compact) Key.DirectionRight else Key.DirectionDown
                val back = if (compact) Key.DirectionLeft else Key.DirectionUp
                if (inNav) return@onPreviewKeyEvent when (e.key) {
                    next -> { byKeys = true; Part.entries.getOrNull(at + 1)?.let(::go); true }
                    back -> { byKeys = true; Part.entries.getOrNull(at - 1)?.let(::go); true }
                    // Tab goes on to the page's first stop (Shift + Tab: back to its last one).
                    Key.Tab -> { if (shown == part) { byKeys = true; inNav = false; page.end(last = e.isShiftPressed)?.let(::reveal) }; true }
                    // Into the page, where the keys last were: Enter, and the arrow that points at it.
                    Key.Enter, Key.NumPadEnter, Key.Spacebar, if (compact) Key.DirectionUp else Key.DirectionRight -> { if (!again) { byKeys = true; inNav = false; entering = true }; true }
                    Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight -> true
                    else -> false
                }
                /** The keys leave the page for the navigation. */
                fun toNav() { picked[part] = page.selected; inNav = true }
                val handled = when (e.key) {
                    // Tab goes through every stop in reading order, and from the last one round to the navigation.
                    Key.Tab -> {
                        val order = page.order()
                        val i = order.indexOfFirst { it.key == page.selected }
                        if (e.isShiftPressed) { if (i <= 0) toNav() else page.move(-1)?.let(::reveal) }
                        else if (i == order.lastIndex && i >= 0) toNav() else page.move(1)?.let(::reveal)
                        true
                    }
                    // In a narrow window the bar is under the page: Down on the last stop goes on to it.
                    Key.DirectionDown -> { if (compact && page.selected != null && page.order().lastOrNull()?.key == page.selected) toNav() else page.move(1)?.let(::reveal); true }
                    Key.DirectionUp -> { page.move(-1)?.let(::reveal); true }
                    // Left steps a row's choice back. Where there is nothing to step (a row without a choice, a switch, a choice at
                    // its first option) it goes to the rail beside the page; a held key stops at the first option.
                    Key.DirectionLeft -> { if (page.current?.step?.invoke(-1) != true && !compact && !again) toNav(); true }
                    Key.DirectionRight -> { page.current?.step?.invoke(1); true }
                    Key.Enter, Key.NumPadEnter, Key.Spacebar -> page.current?.let { if (!again) { press(it); it.enter() }; true } ?: false
                    Key.MoveHome -> { page.end(last = false)?.let(::reveal); true }
                    Key.MoveEnd -> { page.end(last = true)?.let(::reveal); true }
                    Key.PageDown -> { page.page(viewport * 0.8f)?.let(::reveal); true }
                    Key.PageUp -> { page.page(-viewport * 0.8f)?.let(::reveal); true }
                    Key.Delete -> page.current?.delete?.let { if (!again) it(); true } ?: false
                    Key.Menu -> page.current?.menu?.let { if (!again) it(); true } ?: false
                    // Shift + F10 is the Menu key of a keyboard that has none.
                    Key.F10 -> if (e.isShiftPressed) page.current?.menu?.let { if (!again) it(); true } ?: false else false
                    // Escape clears Find. It never closes the window.
                    Key.Escape -> if (part == Part.COMMANDS && commands.find.isNotEmpty()) { commands.find = ""; true } else false
                    else -> {
                        // On Commands, a letter typed on the page goes to Find.
                        val letter = e.nativeKeyEvent.unicodeChar.takeIf { part == Part.COMMANDS && !e.isAltPressed && !e.isMetaPressed && it > ' '.code }
                        if (letter != null) { commands.find += letter.toChar(); commands.ask = true }
                        return@onPreviewKeyEvent letter != null
                    }
                }
                if (handled && e.key != Key.Escape) byKeys = true
                handled
            },
    ) {
        // The navigation is on the window's leading edge and the page's column starts at it: nothing is centred, so a
        // title is in the same place in every section and at every width. Widths follow the window frame by frame. Only
        // crossing a breakpoint moves anything: at 600 the bar and the rail change places, at 840 Material widens the rail.
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedVisibility(!compact, enter = expandHorizontally(motion.place(), Alignment.Start) + fadeIn(motion.fade(140)), exit = shrinkHorizontally(motion.place(), Alignment.Start) + fadeOut(motion.fade(70))) {
                    Rail(rail, part, expanded = width == Width.EXPANDED, dot = !s.keySeen, onPick = { go(it); inNav = false })
                }
                val margin by animateDpAsState(width.margin, motion.place(), label = "margin")
                val over = remember { MutableInteractionSource() }
                val hovered by over.collectIsHoveredAsState()
                // The pane: everything beside the rail. The wheel scrolls it wherever the pointer is in it; its scrollbar is at its trailing edge.
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().hoverable(over).onSizeChanged { viewport = it.height }.onGloballyPositioned { pane = it.boundsInRoot() }) {
                    val column = minOf(COLUMN, maxWidth - margin * 2)
                    // From 1332 dp of window a second pane has room beside a full column: it takes what is left, 320 to 560.
                    val second = width == Width.EXPANDED && maxWidth >= margin + COLUMN + PANE_GAP + PANE_MIN + PANE_GAP
                    val side = (maxWidth - margin - COLUMN - PANE_GAP * 2).coerceIn(PANE_MIN, PANE_MAX)
                    SideEffect { beside = second }
                    CompositionLocalProvider(LocalColumn provides column, LocalSecond provides second, LocalStages provides stages, LocalSettled provides settled, LocalPaneHeight provides maxHeight) {
                        // The old page fades where it stands; the new one's blocks rise in, one after the other, from the
                        // side the indicator came from.
                        AnimatedContent(shown, transitionSpec = { EnterTransition.None togetherWith fadeOut(motion.fade(70)) }, contentAlignment = Alignment.TopStart, label = "page") { p ->
                            val live = p == shown
                            val from = remember { if (down) 1f else -1f }
                            val arrive = remember { Animatable(if (motion.on) 0f else 1f) }
                            LaunchedEffect(Unit) { arrive.animateTo(1f, motion.fade(520)) }
                            Box(Modifier.fillMaxSize().softEdges(scrolls.getValue(p)).verticalScroll(scrolls.getValue(p))) {
                                Column(
                                    Modifier.padding(start = margin).width(column)
                                        .onGloballyPositioned { if (live) { page.root = it; page.origin = it.positionInRoot() } }
                                        .padding(top = TITLE_LINE - TITLE / 2, bottom = 48.dp),
                                ) {
                                    Rise(arrive, 0, from) {
                                        PageTitle(stringResource(if (p == Part.START) R.string.app_name else p.title), lead(p, commands.yours), trailing = if (p != Part.COMMANDS) null else ({
                                            Find(page, commands, compact) { to ->
                                                // The keys leave Find for the page: for the list's first row, for the next stop, or to stay on Find's own.
                                                focus.requestFocus(); byKeys = true
                                                when (to) {
                                                    Leave.LIST -> page.order().firstOrNull { it.key !in setOf("find", "half", "new") }?.let { page.selected = it.key; reveal(it.value) }
                                                    Leave.NEXT -> { page.selected = "find"; page.move(1) }
                                                    Leave.STAY -> page.selected = "find"
                                                }
                                            }
                                        }))
                                    }
                                    when (p) {
                                        Part.START -> StartPage(page, app, s, resumed, askedForKey, arrive, from)
                                        Part.COMMANDS -> CommandsPage(page, app, s, commands, edit, onEdited, onTyping = { editing = it; if (!it) focus.requestFocus() }, onGrow = ::follow, arrive, from)
                                        Part.LOOK -> LookPage(page, s, app, arrive, from)
                                        // (Guarded: the page says "no field has the keys" once more as the window closes, when nothing can take them.)
                                        Part.RESULTS -> ResultsPage(page, app, s, arrive, from, onTyping = { editing = it; if (!it) runCatching { focus.requestFocus() } })
                                        Part.PRIVACY -> PrivacyPage(page, app, resumed, arrive, from)
                                    }
                                }
                            }
                        }
                        // The second pane starts on the line the title's own control starts on (Find, 40 high on the title's line).
                        // It fades in and slides 24 dp from the trailing edge when the window grows past 1332,
                        // and goes the same way. What it shows changes with the section as the page does: the old fades where it
                        // stands, the new rises in.
                        val slide = with(density) { PANE_GAP.roundToPx() }
                        androidx.compose.animation.AnimatedVisibility(
                            second, Modifier.padding(start = margin + COLUMN + PANE_GAP, top = TITLE_LINE - CONTROL / 2).width(side),
                            enter = if (settled) fadeIn(motion.fade(140)) + slideInHorizontally(motion.place()) { slide } else EnterTransition.None,
                            exit = if (settled) fadeOut(motion.fade(70)) + slideOutHorizontally(motion.place()) { slide } else ExitTransition.None,
                        ) {
                            AnimatedContent(shown, transitionSpec = { EnterTransition.None togetherWith fadeOut(motion.fade(70)) }, contentAlignment = Alignment.TopStart, label = "side") { p ->
                                val from = remember { if (down) 1f else -1f }
                                val arrive = remember { Animatable(if (motion.on) 0f else 1f) }
                                LaunchedEffect(Unit) { arrive.animateTo(1f, motion.fade(520)) }
                                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                                    Rise(arrive, 1, from) { SecondPane(p, page, app, s, commands, onTyping = { editing = it; if (!it) focus.requestFocus() }) }
                                }
                            }
                        }
                    }
                    // The scrollbar is at the trailing edge of what scrolls: of the window, or of the column when a second pane is beside it.
                    Scrollbar(scrolls.getValue(shown), over = hovered, if (second) Modifier.align(Alignment.CenterStart).padding(start = margin + COLUMN - 4.dp) else Modifier.align(Alignment.CenterEnd))
                }
            }
            AnimatedVisibility(compact, enter = expandVertically(motion.place(), Alignment.Top) + fadeIn(motion.fade(140)), exit = shrinkVertically(motion.place(), Alignment.Top) + fadeOut(motion.fade(70))) {
                Bar(bar, part, dot = !s.keySeen, onPick = { go(it); inNav = false })
            }
        }
        // The one focus ring, over everything: round the open section in the navigation, or round the page's stop the keys are on.
        val nav = if (compact) bar else rail
        FocusRing(
            target = if (typing) null else if (inNav) "nav" else page.selected,
            shown = byKeys && windowFocus && !typing,
            rect = { if (inNav) nav.indicator(density) else { page.placed; page.current?.bounds?.translate(page.origin) } },
            corner = { with(density) { (if (inNav) 28.dp else page.current?.corner ?: 16.dp).toPx() } },
            within = { if (inNav) null else pane },
        )
    }
}

/** The digits of Ctrl + 1 … 9, in order. */
private val DIGITS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)

/** What a section says about itself under its title, if anything. */
@Composable
private fun lead(p: Part, yours: Boolean): String? = when (p) {
    Part.START -> stringResource(R.string.win_lead)
    Part.COMMANDS -> stringResource(if (yours) R.string.win_commands_text else R.string.win_commands_lead)
    Part.PRIVACY -> stringResource(R.string.win_privacy_lead)
    else -> null
}
