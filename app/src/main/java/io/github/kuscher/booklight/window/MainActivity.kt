package io.github.kuscher.booklight.window

import android.content.Intent
import android.os.Bundle
import android.provider.Settings as SystemSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.github.kuscher.booklight.core.Icon as RowIcon
import io.github.kuscher.booklight.data.Recipes
import io.github.kuscher.booklight.overlay.DrawnCheck
import io.github.kuscher.booklight.overlay.OverlayActivity
import io.github.kuscher.booklight.overlay.SMALL
import io.github.kuscher.booklight.overlay.fadeEnd
import io.github.kuscher.booklight.ui.AppIcons
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.BuildConfig
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Engines
import io.github.kuscher.booklight.data.Settings
import io.github.kuscher.booklight.entry.PickFolderActivity
import io.github.kuscher.booklight.overlay.LocalDark
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.Motion
import io.github.kuscher.booklight.overlay.OptionStrip
import io.github.kuscher.booklight.overlay.Pill
import io.github.kuscher.booklight.overlay.SECOND
import io.github.kuscher.booklight.ui.BooklightTheme
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import io.github.kuscher.booklight.ui.isDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Booklight window: what Booklight is, how to give it a key, everything it does, every setting,
 * and what the user made. It is what the app's icon opens (the keyboard shortcut opens the panel).
 * An ordinary window, built from the panel's parts: a column of sections and one page beside it,
 * on one ground with no lines between them; rows, one pill that glides between them, strips for
 * choices.
 */
class MainActivity : ComponentActivity() {
    /** What the panel asked to be edited (`snippet`, `quicklink`, `recipe`, `prompt`) and which one; consumed by the page. */
    private var edit by mutableStateOf<Pair<String, String>?>(null)
    /** The section the panel asked for ("All commands"); consumed by the window. */
    private var goto by mutableStateOf<String?>(null)

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
            BooklightTheme(dark, tint = s.tint) {
                CompositionLocalProvider(LocalMotion provides motion, LocalDark provides dark) {
                    Window(app, s, edit, goto, onEdited = { edit = null }, onGone = { goto = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(intent: Intent) {
        goto = intent.getStringExtra(EXTRA_PAGE)
        val kind = intent.getStringExtra(EXTRA_EDIT) ?: return
        edit = kind to intent.getStringExtra(EXTRA_ID).orEmpty()
    }

    companion object {
        const val EXTRA_EDIT = "edit"
        const val EXTRA_ID = "id"
        /** Which section to open at: `commands`. */
        const val EXTRA_PAGE = "page"
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
    val page = remember { Page() }
    val focus = remember { FocusRequester() }
    /** The section the column says is open, and the one whose page is on screen: the page follows a moment later. */
    var part by remember { mutableStateOf(Part.of(goto) ?: if (edit != null) Part.YOURS else Part.START) }
    var shown by remember { mutableStateOf(part) }
    /** The keys are in the column; else in the page. */
    var inColumn by remember { mutableStateOf(false) }
    /** The keys have just gone into the page: a row is to be selected once the page has its rows. */
    var entering by remember { mutableStateOf(false) }
    /** The row that was selected in each section, for coming back to it. */
    val picked = remember { HashMap<Part, String?>() }
    /** Which way the last change of section went: the new page comes from that side. */
    var down by remember { mutableStateOf(true) }
    /** A text field of an editor has the keyboard: the arrow keys are its own. */
    var typing by remember { mutableStateOf(false) }
    var viewport by remember { mutableIntStateOf(0) }
    val scrolls = remember { Part.entries.associateWith { ScrollState(0) } }
    LaunchedEffect(Unit) { focus.requestFocus() }
    // What needs a look when the window comes back from Settings, the folder picker or the panel.
    var resumed by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { resumed++; onPauseOrDispose { } }

    fun go(to: Part) {
        if (to == part) return
        picked[part] = page.selected
        page.selected = null
        down = to.ordinal > part.ordinal
        part = to
    }
    LaunchedEffect(goto) { Part.of(goto)?.let { go(it); inColumn = false; entering = true; onGone() } }
    LaunchedEffect(edit) { if (edit != null) { go(Part.YOURS); inColumn = false } }
    // The page follows the column's pane a moment after it last moved: a held arrow shows no pages in between.
    LaunchedEffect(part) { if (shown != part) { delay(motion.hold(60)); shown = part } }

    /** Keeps the selected row in view after an arrow key. */
    fun reveal(e: Page.Entry) {
        val scroll = scrolls.getValue(shown)
        val margin = with(density) { 96.dp.toPx() }
        val top = e.top - margin
        val bottom = e.top + e.height + margin
        val to = when {
            top < scroll.value -> top
            bottom > scroll.value + viewport -> bottom - viewport
            else -> return
        }
        scope.launch { scroll.animateScrollTo(to.toInt().coerceIn(0, scroll.maxValue), motion.place()) }
    }
    // Into the page: the row that was selected there, else the first one.
    LaunchedEffect(entering, shown, page.placed) {
        if (!entering || shown != part || page.rows.values.none { it.height > 0 }) return@LaunchedEffect
        entering = false
        val want = picked[part]
        if (want != null && page.rows[want] != null) page.selected = want else { page.selected = null; page.move(1) }
        page.current?.let(::reveal)
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(scheme.surfaceContainerHigh).safeDrawingPadding()
            .onGloballyPositioned { viewport = it.size.height }
            .focusRequester(focus).focusable()
            .onPreviewKeyEvent { e ->
                if (typing || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val again = e.nativeKeyEvent.repeatCount > 0     // a held key runs a row once
                val at = part.ordinal
                when (e.key) {
                    // Tab goes between the column and the page; so does Left from a row that has nothing to step through.
                    Key.Tab -> { if (inColumn) { inColumn = false; entering = true } else { picked[part] = page.selected; inColumn = true }; true }
                    Key.DirectionDown -> { if (inColumn) Part.entries.getOrNull(at + 1)?.let(::go) else page.move(1)?.let(::reveal); true }
                    Key.DirectionUp -> { if (inColumn) Part.entries.getOrNull(at - 1)?.let(::go) else page.move(-1)?.let(::reveal); true }
                    Key.DirectionLeft -> if (inColumn) true else page.current?.step?.let { it(-1); true } ?: run { picked[part] = page.selected; inColumn = true; true }
                    Key.DirectionRight -> if (inColumn) { inColumn = false; entering = true; true } else page.current?.step?.let { it(1); true } ?: false
                    Key.Enter, Key.NumPadEnter, Key.Spacebar ->
                        if (inColumn) { if (!again) { inColumn = false; entering = true }; true }
                        else page.current?.let { if (!again) it.enter(); true } ?: false
                    else -> false
                }
            },
    ) {
        // The column and the page are one group in the middle of a wide window. As the window narrows the column
        // gives way first, down to a column of marks; then the page. Nothing here is animated: it follows the window.
        val column = (maxWidth - 808.dp).coerceIn(NAV_ITEM, NAV_WIDE)
        val wide = minOf(720.dp, maxWidth - 48.dp - column - 40.dp).coerceAtLeast(240.dp)
        Row(Modifier.align(Alignment.TopCenter).padding(top = 24.dp)) {
            NavColumn(part, inColumn, column, dot = !s.keySeen, onPick = { go(it); inColumn = false })
            Spacer(Modifier.width(40.dp))
            Box(Modifier.width(wide).fillMaxHeight()) {
                // The old page fades where it stands; the new one's blocks rise in, one after the other, from the side
                // the column's pane came from.
                AnimatedContent(shown, transitionSpec = { EnterTransition.None togetherWith fadeOut(motion.fade(70)) }, contentAlignment = Alignment.TopStart, label = "page") { p ->
                    val live = p == shown
                    val from = remember { if (down) 1f else -1f }
                    val arrive = remember { Animatable(if (motion.on) 0f else 1f) }
                    LaunchedEffect(Unit) { arrive.animateTo(1f, motion.fade(520)) }
                    Box(Modifier.fillMaxWidth().verticalScroll(scrolls.getValue(p)).onGloballyPositioned { if (live) page.root = it }) {
                        // The page's one selection, behind the rows: the coloured pill is where the keys are, so it is
                        // only there while the keys are in the page.
                        if (live) {
                            page.placed
                            val on = page.selected?.let { page.rows[it] }
                            with(density) { Pill((on?.top ?: 0f).toDp(), (on?.height ?: 0f).toDp(), visible = on != null && !inColumn) }
                        }
                        Column(Modifier.fillMaxWidth().padding(bottom = 64.dp)) {
                            Rise(arrive, 0, from) { PageTitle(stringResource(if (p == Part.START) R.string.app_name else p.title), lead(p)) }
                            when (p) {
                                Part.START -> StartPage(page, app, s, resumed, arrive, from)
                                Part.COMMANDS -> Rise(arrive, 1, from) { CommandsPage(page, app) }
                                Part.YOURS -> Rise(arrive, 1, from) { Commands(page, app, s, edit, onEdited, onTyping = { typing = it; if (!it) focus.requestFocus() }) }
                                Part.LOOK -> Rise(arrive, 1, from) { LookPage(page, s, app) }
                                Part.RESULTS -> ResultsPage(page, app, s, arrive, from)
                                Part.ACCESS -> Rise(arrive, 1, from) { AccessPage(page, app, resumed) }
                                Part.ABOUT -> Rise(arrive, 1, from) { AboutPage(page, app) }
                            }
                        }
                    }
                    // "Edit…" in the panel opens the window at that item: bring it into view once it has a place.
                    if (p == Part.YOURS) LaunchedEffect(edit) {
                        val key = edit?.let { "${it.first}:${it.second}" } ?: return@LaunchedEffect
                        repeat(30) { page.rows[key]?.takeIf { it.height > 0 }?.let { reveal(it); return@LaunchedEffect }; delay(50) }
                    }
                }
            }
        }
    }
}

/** What a section says about itself under its title, if anything. */
@Composable
private fun lead(p: Part): String? = when (p) {
    Part.START -> stringResource(R.string.win_lead)
    Part.COMMANDS -> stringResource(R.string.win_commands_lead)
    Part.YOURS -> stringResource(R.string.win_commands_text)
    Part.ACCESS -> stringResource(R.string.win_access_text)
    Part.ABOUT -> stringResource(R.string.set_about_text)
    else -> null
}

/** A page's title, on the centre line of the column's first item, in the same place on every page; then its lead. */
@Composable
private fun PageTitle(title: String, lead: String?) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(start = GUTTER, end = GUTTER, bottom = 12.dp)) {
        Box(Modifier.height(NAV_ITEM), contentAlignment = Alignment.CenterStart) {
            Text(title, color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.round, fontSize = 36.sp, fontWeight = FontWeight(700)), maxLines = 1)
        }
        lead?.let { Text(it, color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 18.sp, lineHeight = 26.sp), modifier = Modifier.padding(top = 12.dp)) }
    }
}

/** The name of a group of rows: the panel's slot label. */
@Composable
fun GroupLabel(text: String) {
    Text(text.uppercase(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 12.sp, fontWeight = FontWeight(600), letterSpacing = 0.5.sp),
        modifier = Modifier.padding(start = GUTTER, top = 28.dp, bottom = 8.dp))
}

/** Start: what Booklight is, shown; the key that opens it; the tips. */
@Composable
private fun StartPage(page: Page, app: BooklightApp, s: Settings, resumed: Int, arrive: Animatable<Float, *>, from: Float) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val activity = LocalActivity.current as ComponentActivity
    fun set(change: (Settings) -> Settings) = app.prefs.update(change)
    Rise(arrive, 1, from) { Box(Modifier.padding(top = 12.dp)) { Stage(app) } }
    Rise(arrive, 2, from) {
        Column {
            GroupLabel(stringResource(R.string.win_key_title))
            // Whether a key has ever opened the panel. It changes while the panel is in front; here it is read when the
            // window has the keys again, and then the line changes in place: nothing under it moves.
            val seen = remember(resumed) { app.prefs.now.keySeen }
            var asked by remember { mutableStateOf(false) }
            Row(Modifier.padding(start = GUTTER, bottom = 12.dp).height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    val dot by androidx.compose.animation.core.animateFloatAsState(if (seen) 0f else 1f, motion.fade(80), label = "dot")
                    Box(Modifier.size(8.dp).graphicsLayer { alpha = dot }.clip(androidx.compose.foundation.shape.CircleShape).background(scheme.onSurface))
                    DrawnCheck(seen, scheme.onSurface, Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                AnimatedContent(if (seen) R.string.key_works else if (asked) R.string.key_press else R.string.key_none, transitionSpec = { motion.roll() }, contentAlignment = Alignment.CenterStart, label = "key") { word ->
                    Text(stringResource(word), color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(600)), maxLines = 1)
                }
            }
            Text(stringResource(R.string.win_key_text), color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 15.sp, lineHeight = 21.sp), modifier = Modifier.padding(start = GUTTER, end = GUTTER, bottom = 12.dp))
            Row(Modifier.padding(start = GUTTER, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Keys(stringResource(R.string.key_action), "Alt", stringResource(R.string.key_space), ink = scheme.onSurface)
                Text(stringResource(R.string.win_key_or), color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp))
                Keys(stringResource(R.string.key_action), "K", ink = scheme.onSurface)
            }
            PageRow(page, "key", stringResource(R.string.set_shortcut_button), stringResource(R.string.win_key_steps),
                mark = { Icon(Symbols.of("key"), null, tint = it) }, onEnter = { asked = true; activity.requestShowKeyboardShortcuts() }) { Icon(Symbols.of("open"), null, tint = it) }
        }
    }
    Rise(arrive, 3, from) {
        Column {
            GroupLabel(stringResource(R.string.set_tips))
            Toggle(page, "tips", stringResource(R.string.set_tips), stringResource(R.string.set_tips_text), s.tips, mark = "spark") { v -> set { it.copy(tips = v) } }
            var again by remember { mutableStateOf(false) }
            PageRow(page, "tips-again", stringResource(R.string.set_tips_again), null, mark = { Icon(Symbols.of("again"), null, tint = it) },
                onEnter = { set { it.copy(tips = true, tipsSeen = emptyList(), tipId = "", tipMs = 0) }; again = true }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DrawnCheck(again, it, Modifier.size(16.dp))
                    if (again) Text(stringResource(R.string.set_tips_again_done), color = it, style = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(600)))
                }
            }
        }
    }
}

/**
 * Commands: everything Booklight does, the same table as the list behind `?`, in its groups. Each
 * row has its example at the right, as plain text. Enter opens the panel and Booklight types it.
 */
@Composable
private fun CommandsPage(page: Page, app: BooklightApp) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val activity = LocalActivity.current as ComponentActivity
    val entries = remember { app.guide.entries() }
    Column {
        for ((group, rows) in entries.groupBy { it.group }) {
            GroupLabel(app.guide.group(group))
            for (e in rows) {
                val key = "guide:${e.id}"
                PageRow(page, key, e.name, e.line.ifEmpty { null }, mark = { Icon(Symbols.of(e.symbol), null, Modifier.size(20.dp), tint = it) },
                    onEnter = { activity.startActivity(Intent(activity, OverlayActivity::class.java).setAction(OverlayActivity.ACTION_PANEL).putExtra(OverlayActivity.EXTRA_TYPE, e.example)) }) { ink ->
                    // The example: what would be typed, so in full ink. Its column ends in the same place on every row; the
                    // Enter mark of the selected row has its room after it.
                    Text(e.example.trim(), color = ink, style = SMALL, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 240.dp))
                    val mark by androidx.compose.animation.core.animateFloatAsState(if (page.selected == key) 1f else 0f, motion.fade(120), label = "mark")
                    Box(Modifier.width(20.dp), contentAlignment = Alignment.CenterEnd) { Icon(Symbols.enter, null, Modifier.size(14.dp).graphicsLayer { alpha = mark }, tint = ink) }
                }
            }
        }
    }
}

@Composable
private fun LookPage(page: Page, s: Settings, app: BooklightApp) {
    fun set(change: (Settings) -> Settings) = app.prefs.update(change)
    Column(Modifier.padding(top = 8.dp)) {
        Choice(page, "theme", stringResource(R.string.set_theme), null, listOf("auto", "light", "dark"),
            listOf(R.string.set_theme_auto, R.string.set_theme_light, R.string.set_theme_dark), s.theme) { v -> set { it.copy(theme = v) } }
        Choice(page, "tint", stringResource(R.string.set_tint), stringResource(R.string.set_tint_text), listOf("on", "off"),
            listOf(R.string.set_tint_wallpaper, R.string.set_tint_own), if (s.tint) "on" else "off") { v -> set { it.copy(tint = v == "on") } }
        Choice(page, "glass", stringResource(R.string.set_glass), stringResource(R.string.set_glass_text), listOf("clear", "balanced", "frosted", "solid"),
            listOf(R.string.set_glass_clear, R.string.set_glass_balanced, R.string.set_glass_frosted, R.string.set_glass_solid), s.glass) { v -> set { it.copy(glass = v) } }
        Choice(page, "shadow", stringResource(R.string.set_shadow), stringResource(R.string.set_shadow_text), listOf("off", "low", "medium", "high"),
            listOf(R.string.set_shadow_off, R.string.set_shadow_low, R.string.set_shadow_medium, R.string.set_shadow_high), s.shadow) { v -> set { it.copy(shadow = v) } }
        Toggle(page, "dim", stringResource(R.string.set_dim), stringResource(R.string.set_dim_text), s.dim) { v -> set { it.copy(dim = v) } }
        Choice(page, "opening", stringResource(R.string.set_opening), stringResource(R.string.set_opening_text), listOf("off", "fast", "medium", "slow"),
            listOf(R.string.set_opening_off, R.string.set_opening_fast, R.string.set_opening_medium, R.string.set_opening_slow), s.opening) { v -> set { it.copy(opening = v) } }
    }
}

/** Results: where a search goes, what kinds of rows the list has, and what other apps may put in it. Every row has a mark, so all text starts on one edge. */
@Composable
private fun ResultsPage(page: Page, app: BooklightApp, s: Settings, arrive: Animatable<Float, *>, from: Float) {
    val scheme = MaterialTheme.colorScheme
    fun set(change: (Settings) -> Settings) = app.prefs.update(change)
    Rise(arrive, 1, from) {
        Column(Modifier.padding(top = 8.dp)) {
            // Five names are a line of their own, under the title.
            Choice(page, "engine", stringResource(R.string.set_engine), null, Engines.all.map { it.id }, null, s.engine, names = Engines.all.map { it.name }, mark = "search", under = true) { v -> set { it.copy(engine = v) } }
            val engine = s.engine()
            Toggle(page, "suggest", stringResource(R.string.set_suggestions), stringResource(R.string.set_suggestions_text, engine.name, engine.suggestHost), s.suggestions, mark = "globe") { v -> set { it.copy(suggestions = v, suggestionsCard = false) } }
            Toggle(page, "pages", stringResource(R.string.set_show_settings), null, s.showSettings, mark = "settings") { v -> set { it.copy(showSettings = v) } }
            Toggle(page, "sums", stringResource(R.string.set_show_sums), null, s.showSums, mark = "calc") { v -> set { it.copy(showSums = v) } }
            Toggle(page, "gemini", stringResource(R.string.set_show_gemini), stringResource(R.string.set_show_gemini_text), s.showGemini, mark = "spark") { v -> set { it.copy(showGemini = v) } }
            Toggle(page, "keys", stringResource(R.string.set_show_keys), stringResource(R.string.set_show_keys_text), s.showKeys, mark = "key") { v -> set { it.copy(showKeys = v) } }
        }
    }
    Rise(arrive, 2, from) {
        Column {
            GroupLabel(stringResource(R.string.win_other_apps))
            Toggle(page, "apps", stringResource(R.string.set_app_commands), stringResource(R.string.set_app_commands_text), s.appCommands, mark = "bolt") { v -> set { it.copy(appCommands = v) } }
            val offers = app.commands.offers
            val icons = remember { app.icons ?: AppIcons(app).also { app.icons = it } }
            if (offers.isEmpty()) Text(stringResource(R.string.win_apps_none), color = scheme.onSurface.copy(alpha = SECOND), style = SMALL, modifier = Modifier.padding(start = 64.dp, top = 6.dp, end = GUTTER))
            // With the switch for all of them off these stay where they are, dimmed, and the pill passes over them.
            for (o in offers) {
                val on = o.owner !in s.mutedApps
                PageRow(page, "app:${o.owner}", o.app, o.titles.joinToString(" · "), enabled = s.appCommands, lines = 1,
                    mark = { AppMark(icons, RowIcon.App(o.owner, o.cls, Recipes.me)) },
                    onStep = { d -> set { it.copy(mutedApps = if (d > 0) it.mutedApps - o.owner else (it.mutedApps + o.owner).distinct()) } },
                    onEnter = { set { it.copy(mutedApps = if (on) (it.mutedApps + o.owner).distinct() else it.mutedApps - o.owner) } }) { ink -> FlatSwitch(on, ink) }
            }
        }
    }
}

/** An app's own icon in a row's mark box. It fades in when it had to be loaded: nothing cuts in. */
@Composable
private fun AppMark(icons: AppIcons, icon: RowIcon.App) {
    val px = with(LocalDensity.current) { 48.dp.roundToPx() }
    val bitmap by produceState(icons.cached(icon), icon) { if (value == null) value = icons.load(icon, px) }
    val there by androidx.compose.animation.core.animateFloatAsState(if (bitmap != null) 1f else 0f, LocalMotion.current.fade(80), label = "icon")
    Box(Modifier.size(36.dp)) { bitmap?.let { Image(it, null, Modifier.fillMaxSize().graphicsLayer { alpha = there }) } }
}

@Composable
private fun AccessPage(page: Page, app: BooklightApp, resumed: Int) {
    val activity = LocalActivity.current as ComponentActivity
    Column(Modifier.padding(top = 8.dp)) {
        resumed
        val folder = if (app.notes.ready) app.notes.where else null
        PageRow(page, "notes", stringResource(R.string.win_notes), folder?.let { stringResource(R.string.win_notes_in, it) } ?: stringResource(R.string.win_notes_none),
            mark = { Icon(Symbols.of("note"), null, tint = it) }, onEnter = { activity.startActivity(Intent(activity, PickFolderActivity::class.java)) }) {
            Text(stringResource(if (folder == null) R.string.win_choose else R.string.win_change), color = it, style = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(600)))
        }
        val bright = app.executor.screen.allowed
        PageRow(page, "brightness", stringResource(R.string.dial_brightness), stringResource(if (bright) R.string.win_brightness_on else R.string.win_brightness_off),
            mark = { Icon(Symbols.of("sun"), null, tint = it) }, onEnter = { app.executor.run(Effect.Grant("brightness"), activity) }) {
            Text(stringResource(if (bright) R.string.win_allowed else R.string.action_allow), color = it, style = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(600)))
        }
        PageRow(page, "assistant", stringResource(R.string.win_assistant), stringResource(R.string.win_assistant_text),
            mark = { Icon(Symbols.of("spark"), null, tint = it) }, onEnter = { runCatching { activity.startActivity(Intent(SystemSettings.ACTION_VOICE_INPUT_SETTINGS)) } }) { Icon(Symbols.of("open"), null, tint = it) }
    }
}

/** About: the policy and the version first, what Booklight keeps, and, last, the row that forgets it. */
@Composable
private fun AboutPage(page: Page, app: BooklightApp) {
    val scheme = MaterialTheme.colorScheme
    val activity = LocalActivity.current as ComponentActivity
    Column(Modifier.padding(top = 8.dp)) {
        // Play asks for the policy to be reachable from inside the app.
        PageRow(page, "policy", stringResource(R.string.set_privacy_policy), stringResource(R.string.set_version, BuildConfig.VERSION_NAME), mark = { Icon(Symbols.of("lock"), null, tint = it) },
            onEnter = { app.executor.run(Effect.OpenUrl(MainActivity.PRIVACY_URL), activity) }) { Icon(Symbols.of("open"), null, tint = it) }
        GroupLabel(stringResource(R.string.set_privacy_title))
        Text(stringResource(R.string.set_privacy_text), color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 15.sp, lineHeight = 21.sp), modifier = Modifier.padding(start = GUTTER, end = GUTTER, bottom = 6.dp))
        Text(stringResource(R.string.about_device), color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 15.sp, lineHeight = 21.sp), modifier = Modifier.padding(start = GUTTER, end = GUTTER, bottom = 10.dp))
        // Forgetting needs Enter twice, like deleting in the panel; and it is the last row, never the first.
        var sure by remember { mutableStateOf(false) }
        var done by remember { mutableStateOf(false) }
        var asked by remember { mutableStateOf(0L) }
        LaunchedEffect(sure) { if (sure) { delay(3000); sure = false } }
        PageRow(page, "forget", stringResource(R.string.set_forget), null, mark = { Icon(Symbols.of("trash"), null, tint = it) },
            onEnter = {
                val now = android.os.SystemClock.uptimeMillis()
                // A second press, a moment later: a double click or a bouncing key doesn't forget everything.
                if (sure && now - asked >= 350) { app.historyStore.clear(); app.lastText = null; sure = false; done = true }
                else if (!sure && !done) { sure = true; asked = now }
            }) {
            Text(stringResource(if (done) R.string.set_forgotten else if (sure) R.string.win_forget_again else R.string.action_delete),
                color = if (sure) scheme.error else it, style = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(600)))
        }
    }
}

/** A page's blocks rise 12 dp and fade in, one after the other, once: [i] is the block's place in that order, [from] the side they come from (1 below, -1 above). */
@Composable
private fun Rise(arrive: Animatable<Float, *>, i: Int, from: Float = 1f, content: @Composable () -> Unit) {
    val rise = with(LocalDensity.current) { 12.dp.toPx() }
    Box(Modifier.graphicsLayer {
        val t = ((arrive.value - i * 0.045f) / 0.3f).coerceIn(0f, 1f)
        alpha = t; translationY = (1f - t) * rise * from
    }) { content() }
}

/** A row whose control is one of several: an option strip with one gliding highlight. Left and Right step through it. */
@Composable
fun Choice(
    page: Page, key: String, title: String, about: String?, ids: List<String>, labels: List<Int>?, chosen: String, names: List<String>? = null, mark: String? = null,
    /** The strip stands under the title, not beside it: for choices too many or too long to share a line with it. */
    under: Boolean = false,
    onPick: (String) -> Unit,
) {
    val at = ids.indexOf(chosen).coerceAtLeast(0)
    val shown = names ?: labels!!.map { stringResource(it) }
    val strip: @Composable (Color) -> Unit = { ink -> OptionStrip(shown, at, onChoose = {}, onRun = { onPick(ids[it]) }, mark = Symbols.check, quiet = true, ink = ink) }
    PageRow(page, key, title, about, mark = mark?.let { m -> { ink -> Icon(Symbols.of(m), null, Modifier.size(20.dp), tint = ink) } }, onStep = { d -> ids.getOrNull(at + d)?.let(onPick) }, onEnter = { onPick(ids[(at + 1) % ids.size]) },
        below = if (under) strip else null) { ink -> if (!under) strip(ink) }
}

/** A row that is on or off. Enter, Space and a click flip it; Left is off, Right is on. */
@Composable
fun Toggle(page: Page, key: String, title: String, about: String?, on: Boolean, mark: String? = null, onChange: (Boolean) -> Unit) {
    PageRow(page, key, title, about, mark = mark?.let { m -> { ink -> Icon(Symbols.of(m), null, Modifier.size(20.dp), tint = ink) } }, onStep = { d -> onChange(d > 0) }, onEnter = { onChange(!on) }) { ink -> FlatSwitch(on, ink) }
}
