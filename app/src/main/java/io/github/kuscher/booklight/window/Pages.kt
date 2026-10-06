package io.github.kuscher.booklight.window

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import android.content.Intent
import android.provider.Settings as SystemSettings
import androidx.compose.runtime.collectAsState
import io.github.kuscher.booklight.core.Cals
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.BuildConfig
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Engines
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.core.Icon as RowIcon
import io.github.kuscher.booklight.data.Recipes
import io.github.kuscher.booklight.data.Settings
import io.github.kuscher.booklight.data.firstRun
import io.github.kuscher.booklight.device.Keyboards
import io.github.kuscher.booklight.device.SystemWords
import io.github.kuscher.booklight.entry.PickFolderActivity
import io.github.kuscher.booklight.overlay.DrawnCheck
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.OverlayActivity
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.delay

/** A page's title has a box this high, with the title on its centre line. */
val TITLE = 56.dp

/**
 * A page's title, on the centre line of the rail's first mark, in the same place on every page and at every
 * width; something of the page's own at the end of that line, ending on the column's edge (the Commands
 * page's Find); then its lead, which is never wider than 600 dp. Title and lead start [INSET] in, on the
 * edge that group names and the rows' marks share.
 */
@Composable
fun PageTitle(title: String, lead: String?, trailing: (@Composable () -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(TITLE)) {
            Text(title, color = scheme.onSurface, style = MaterialTheme.typography.headlineMedium, maxLines = 1, softWrap = false, modifier = Modifier.align(Alignment.CenterStart).padding(start = INSET))
            trailing?.let { Box(Modifier.align(Alignment.CenterEnd)) { it() } }
        }
        // A lead that changes (the Commands page's two halves) gives way where it stands.
        if (lead != null) AnimatedContent(lead, transitionSpec = { (fadeIn(motion.fade(140, 60)) togetherWith fadeOut(motion.fade(70))).using(SizeTransform(clip = false) { _, _ -> motion.place() }) }, contentAlignment = Alignment.TopStart, label = "lead") {
            Text(it, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = INSET).widthIn(max = 600.dp))
        }
    }
}

/** A page's blocks rise 12 dp and fade in, one after the other, once: [i] is the block's place in that order, [from] the side they come from (1 below, -1 above). */
@Composable
fun Rise(arrive: Animatable<Float, *>, i: Int, from: Float = 1f, content: @Composable () -> Unit) {
    val rise = with(LocalDensity.current) { 12.dp.toPx() }
    Box(Modifier.graphicsLayer {
        val t = ((arrive.value - i * 0.045f) / 0.3f).coerceIn(0f, 1f)
        alpha = t; translationY = (1f - t) * rise * from
    }) { content() }
}

/** What the preview of the look has typed in it: the demo's letters, standing still. */
private const val PREVIEW = "ch"

/**
 * Something that stands in the page's column only while the window has no second pane (the demo, the preview).
 * When the window is widened past 1332 dp it goes from the column as the pane arrives, and the rows under it
 * move up on the same spring; it comes back the same way.
 */
@Composable
private fun InColumn(/** False: not in the column either (the window is too low for it). */ room: Boolean = true, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    // It opens and closes from its own top: it stands where it is and the rows under it give way.
    val settled = LocalSettled.current
    AnimatedVisibility(
        !LocalSecond.current && room,
        enter = if (settled) expandVertically(motion.place(), expandFrom = Alignment.Top) + fadeIn(motion.fade(140)) else EnterTransition.None,
        exit = if (settled) shrinkVertically(motion.place(), shrinkTowards = Alignment.Top) + fadeOut(motion.fade(180)) else ExitTransition.None,
    ) { content() }
}

/**
 * The second pane: what stands beside the page's column in a window of 1332 dp or more. Start: the live demo.
 * Look: the preview, in view while the settings scroll. Commands: the editor of the open link, snippet, recipe,
 * prompt or app command, or a built-in command's example as the panel shows it. Results and Privacy: nothing.
 */
@Composable
fun SecondPane(part: Part, page: Page, app: BooklightApp, s: Settings, commands: CommandsState, onTyping: (Boolean) -> Unit) {
    when (part) {
        Part.START -> Beside(stringResource(R.string.win_side_demo)) { Stage(app, s, height = 340.dp, top = 56.dp) }
        Part.LOOK -> Beside(stringResource(R.string.win_side_look)) { Stage(app, s, height = 340.dp, top = 56.dp, text = PREVIEW) }
        Part.COMMANDS -> CommandsSide(page, app, s, commands, onTyping)
        else -> Unit
    }
}

/** A stage in the second pane, and a line under it that says what it is. */
@Composable
fun Beside(note: String, content: @Composable () -> Unit) {
    Column {
        content()
        Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = INSET, end = INSET, top = 12.dp))
    }
}

/** A word at a row's trailing end that says what pressing the row does: Change…, Allow…, Delete. */
@Composable
fun Word(text: String, danger: Boolean = false) {
    Text(text, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, maxLines = 1)
}

/** The mark of a row that opens something outside the window. Its glyph has 2 dp of air inside its box, so the box stands 2 dp over the line the controls end on and the ink ends on it. */
@Composable
fun Opens() = Icon(Symbols.of("open"), null, Modifier.size(20.dp).offset(x = 2.dp))

/** Start: what Booklight is, shown; the ways to open it; and what may stand under the empty field: a tip, your usual, the line for a fresh copy. */
@Composable
fun StartPage(page: Page, app: BooklightApp, s: Settings, resumed: Int, askedForKey: MutableState<Boolean>, arrive: Animatable<Float, *>, from: Float) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val activity = LocalActivity.current as ComponentActivity
    fun set(change: (Settings) -> Settings) = app.prefs.update(change)
    // The live demo: at the top of the page, or in the second pane when the window has one. In a pane under 520 dp
    // high (the smallest window is 480 with its caption bar and the bar) it gives way: title, lead and demo would
    // fill the pane, and what Start is for the first time, the key, would be a scroll away.
    Rise(arrive, 1, from) { InColumn(room = LocalPaneHeight.current >= 520.dp) { Box(Modifier.padding(top = 20.dp)) { Stage(app, s, height = 240.dp, top = 28.dp) } } }
    Rise(arrive, 2, from) {
        // Whether a key has ever opened the panel. It changes while the panel is in front; here it is read when the
        // window has the keys again.
        val seen = remember(resumed) { app.prefs.now.keySeen }
        var asked by askedForKey
        // The keys that are suggested, as first run suggests them (core `FirstRun.suggest`): Action + Quick Insert where the
        // keyboard has that key, Action + M where it has not. The key's name and the dialog's two buttons are quoted in
        // the system's own words where they can be read (`SystemWords`: no permission), else in Booklight's.
        LaunchedEffect(Unit) { SystemWords.load(activity, app.scope) }
        val key = remember(resumed) { FirstRun.suggest(FirstRun.hasQuickInsert(Keyboards.attached(), null)) }
        val name = SystemWords.name(activity, key)
        val words = SystemWords.words(activity)
        val where = if (key == FirstRun.Key.QUICK_INSERT) stringResource(R.string.first_key_where, name) + " · " + stringResource(R.string.first_key_any) else stringResource(R.string.first_key_any)
        Group(stringResource(R.string.win_open_title)) {
            // What is so: no key yet (and then the keys that are suggested, and where the second one is), or the key works. Not a row to press.
            row("status") { place ->
                PageRow(page, "status", stringResource(if (seen) R.string.key_works else if (asked) R.string.key_press else R.string.key_none),
                    stringResource(if (seen) R.string.win_key_works_text else R.string.win_key_text), still = true, roll = true, place = place, onTitleLine = !seen,
                    mark = { ink ->
                        val dot by animateFloatAsState(if (seen) 0f else 1f, motion.fade(80), label = "dot")
                        Box(Modifier.size(8.dp).graphicsLayer { alpha = dot }.clip(CircleShape).background(ink))
                        DrawnCheck(seen, scheme.primary, Modifier.size(20.dp))
                    },
                    below = {
                        AnimatedVisibility(!seen, enter = expandVertically(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkVertically(motion.place()) + fadeOut(motion.fade(70))) {
                            // The caps on the text's edge, and under them what first run's own caption says: one edge for all three lines.
                            Column(Modifier.padding(top = 8.dp, bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Keys(stringResource(R.string.key_action), name, ink = scheme.onSurfaceVariant)
                                Text(where, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    })
            }
            row("key") { place ->
                // The dialog then opens on the window's own page, which begins with the five steps (`MainActivity.guides`).
                PageRow(page, "key", stringResource(R.string.set_shortcut_button), stringResource(R.string.win_key_steps, words.customize, words.set), place = place,
                    mark = { MarkIcon("key") }, onEnter = { asked = true; (activity as? MainActivity)?.guides = true; activity.requestShowKeyboardShortcuts() }) { Opens() }
            }
            // The assistant key is a way to open Booklight too, so it stands beside the keyboard shortcut.
            row("assistant") { place ->
                PageRow(page, "assistant", stringResource(R.string.win_assistant), stringResource(R.string.win_assistant_text), place = place,
                    mark = { MarkIcon("spark") }, onEnter = { runCatching { activity.startActivity(Intent(SystemSettings.ACTION_VOICE_INPUT_SETTINGS)) } }) { Opens() }
            }
        }
    }
    Rise(arrive, 3, from) {
        var again by remember { mutableStateOf(false) }
        val fits = remember(resumed) { FirstRun.fits(activity.windowManager.maximumWindowMetrics.bounds.height() / activity.resources.displayMetrics.density) }
        Group(stringResource(R.string.set_tips)) {
            row("tips") { Toggle(page, "tips", stringResource(R.string.set_tips_show), stringResource(R.string.set_tips_text), s.tips, mark = "bulb", place = it) { v -> set { st -> st.copy(tips = v) } } }
            row("again") { place ->
                PageRow(page, "tips-again", stringResource(R.string.set_tips_again), null, place = place, mark = { MarkIcon("again") },
                    onEnter = { set { it.copy(tips = true, tipsSeen = FirstRun.keepMark(it.tipsSeen), tipId = "", tipMs = 0) }; again = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DrawnCheck(again, LocalContentColor.current, Modifier.size(16.dp))
                        if (again) Text(stringResource(R.string.set_tips_again_done), style = MaterialTheme.typography.labelLarge)
                        else Word(stringResource(R.string.win_reset))
                    }
                }
            }
            // "First steps": first run, once more. The row plays the whole of it in a new panel, the opening piece first; while
            // a run is unfinished it goes on where that stopped, and says where (core `FirstRun.offer`, `again`). The panel is
            // started as a row of the Commands page starts it for an example: by this click, over Booklight's own window.
            // Not on a screen too low for first run to stand in the panel: the screen this window stands on, which is
            // where the panel it starts opens (it need not be the application's own display). Read when the window has
            // the keys again, as the key is.
            if (fits) row("first") { place ->
                val offer = FirstRun.offer(s.firstRun())
                PageRow(page, "first", stringResource(if (offer.goesOn) R.string.first_row_go_on else R.string.first_name), stringResource(R.string.first_row_text), place = place, roll = true,
                    mark = { MarkIcon("again") },
                    onEnter = {
                        app.prefs.firstRun { FirstRun.again(it, overture = true) }
                        activity.startActivity(Intent(activity, OverlayActivity::class.java).setAction(OverlayActivity.ACTION_PANEL))
                    }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        offer.count?.let { Text(stringResource(R.string.first_step, it.first, it.second), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                        Opens()
                    }
                }
            }
        }
    }
    // What else can stand under the empty field. Tips stay where they were; "Your usual" follows them, because it takes
    // a tip's place; the line for a fresh copy is last: it has nothing to do with the other two.
    Rise(arrive, 4, from) {
        val hidden = s.zeroHidden.size
        Group(stringResource(R.string.set_usual)) {
            row("usual") { Toggle(page, "usual", stringResource(R.string.set_usual_show), stringResource(R.string.set_usual_text), s.zero, mark = "list", place = it) { v -> set { st -> st.copy(zero = v) } } }
            // What "Don't suggest" took out comes back. The row always says how many there are, so it keeps its height; with
            // none it is dimmed and the keys pass over it.
            row("usual-again") { place ->
                PageRow(page, "usual-again", stringResource(R.string.set_usual_again), pluralStringResource(R.plurals.set_usual_hidden, hidden, hidden), place = place, mark = { MarkIcon("again") },
                    enabled = hidden > 0, onEnter = { set { it.copy(zeroHidden = emptyList()) } }) {
                    if (hidden > 0) Word(stringResource(R.string.win_reset))
                }
            }
        }
    }
    Rise(arrive, 5, from) {
        Group(stringResource(R.string.set_copy)) {
            row("copy") { Toggle(page, "copy", stringResource(R.string.set_copy_show), stringResource(R.string.set_copy_text), s.copyRow, mark = "clip", place = it) { v -> set { st -> st.copy(copyRow = v) } } }
        }
    }
}

/**
 * Look: the theme and the colours, then the panel's own look. Every explanation is one line: the panel itself
 * shows the rest. Its choices all stand in the same place: at the rows' trailing end if every one of them has
 * room there, else every one under its text. At the trailing end their buttons are all one width, if the rows
 * have room for that: the seams of four groups then stand on three lines.
 */
@Composable
fun LookPage(page: Page, s: Settings, app: BooklightApp, arrive: Animatable<Float, *>, from: Float) {
    fun set(change: (Settings) -> Settings) = app.prefs.update(change)
    val theme = listOf(R.string.set_theme_auto, R.string.set_theme_light, R.string.set_theme_dark)
    val tint = listOf(R.string.set_tint_wallpaper, R.string.set_tint_own)
    val glass = listOf(R.string.set_glass_clear, R.string.set_glass_balanced, R.string.set_glass_frosted, R.string.set_glass_solid)
    val shadow = listOf(R.string.set_shadow_off, R.string.set_shadow_low, R.string.set_shadow_medium, R.string.set_shadow_high)
    val opening = listOf(R.string.set_opening_off, R.string.set_opening_fast, R.string.set_opening_medium, R.string.set_opening_slow)
    val all = listOf(theme, tint, glass, shadow, opening)
    val lines = listOf(null, R.string.set_tint_text, R.string.set_glass_text, R.string.set_shadow_text, R.string.set_opening_text).map { id -> id?.let { stringResource(it) } }
    // Every button of the page as wide as the widest name needs, so the groups' seams stand under one another; where
    // the rows have no room for that (longer names, a narrower window, a line that would wrap), each button as wide
    // as its own name.
    val widest = choiceCell(all)
    val even = all.indices.map { besideText(all[it], true, widest) && lineFits(lines[it], all[it].size, widest) }.all { it }
    val beside = even || all.map { besideText(it, true) }.all { it }
    val cell = widest.takeIf { even }
    // The preview: the panel with these settings, at the top of the page, or in the second pane when the window has one.
    Rise(arrive, 1, from) { InColumn { Box(Modifier.padding(top = 8.dp, bottom = 16.dp)) { Stage(app, s, height = 212.dp, top = 22.dp, text = PREVIEW) } } }
    Rise(arrive, 2, from) {
        Group(first = true) {
            row("theme") { Choice(page, "theme", stringResource(R.string.set_theme), null, listOf("auto", "light", "dark"), theme, s.theme, mark = "contrast", place = it, beside = beside, cell = cell) { v -> set { st -> st.copy(theme = v) } } }
            row("tint") { Choice(page, "tint", stringResource(R.string.set_tint), stringResource(R.string.set_tint_text), listOf("on", "off"), tint, if (s.tint) "on" else "off", mark = "palette", place = it, beside = beside, cell = cell) { v -> set { st -> st.copy(tint = v == "on") } } }
        }
    }
    Rise(arrive, 3, from) {
        Group(stringResource(R.string.win_panel)) {
            row("glass") { Choice(page, "glass", stringResource(R.string.set_glass), stringResource(R.string.set_glass_text), listOf("clear", "balanced", "frosted", "solid"), glass, s.glass, mark = "glass", place = it, beside = beside, cell = cell) { v -> set { st -> st.copy(glass = v) } } }
            row("shadow") { Choice(page, "shadow", stringResource(R.string.set_shadow), stringResource(R.string.set_shadow_text), listOf("off", "low", "medium", "high"), shadow, s.shadow, mark = "layers", place = it, beside = beside, cell = cell) { v -> set { st -> st.copy(shadow = v) } } }
            row("dim") { Toggle(page, "dim", stringResource(R.string.set_dim), stringResource(R.string.set_dim_text), s.dim, mark = "sun", place = it) { v -> set { st -> st.copy(dim = v) } } }
            row("opening") { Choice(page, "opening", stringResource(R.string.set_opening), stringResource(R.string.set_opening_text), listOf("off", "fast", "medium", "slow"), opening, s.opening, mark = "unfold", place = it, beside = beside, cell = cell) { v -> set { st -> st.copy(opening = v) } } }
        }
    }
}

/**
 * Results: where a search goes, what kinds of rows the list has, the flight service's key, and what other apps may put in it. Every row
 * has a mark, so all text starts on one edge, and every control ends on one line at the rows' trailing end.
 */
@Composable
fun ResultsPage(page: Page, app: BooklightApp, s: Settings, resumed: Int, arrive: Animatable<Float, *>, from: Float) {
    fun set(change: (Settings) -> Settings) = app.prefs.update(change)
    val window = LocalActivity.current as? MainActivity
    // The calendars an event can be added to, where the list is allowed. (A very long name is cut for the menu's button.)
    val calendars by app.calendars.now.collectAsState()
    val open = calendars.filter { it.writable }
    val names = open.map { if (it.name.length > CALENDAR_NAME) it.name.take(CALENDAR_NAME - 1).trimEnd() + "…" else it.name }
    // The page's two menus stand in the same place: both at their rows' trailing ends, or both under their text. The search
    // engines' names decide which, at every width; a calendar's name is the user's own and may be any length: its button
    // takes the room that place leaves, and the name is cut inside it (`Pick`'s `cut`).
    val engines = Engines.all.map { it.name }
    val beside = menuBeside(engines, true)
    Rise(arrive, 1, from) {
        Group(stringResource(R.string.win_search), first = true) {
            // Five names are too many for buttons: a menu.
            row("engine") { Pick(page, "engine", stringResource(R.string.set_engine), stringResource(R.string.set_engine_text), Engines.all.map { e -> e.id }, engines, s.engine, mark = "search", place = it, beside = beside) { v -> set { st -> st.copy(engine = v) } } }
            row("suggest") {
                val engine = s.engine()
                Toggle(page, "suggest", stringResource(R.string.set_suggestions), stringResource(R.string.set_suggestions_text, engine.name, engine.suggestHost), s.suggestions, mark = "globe", place = it) { v -> set { st -> st.copy(suggestions = v, suggestionsCard = false) } }
            }
        }
    }
    Rise(arrive, 2, from) {
        Group(stringResource(R.string.win_show)) {
            row("pages") { Toggle(page, "pages", stringResource(R.string.set_show_settings), null, s.showSettings, mark = "settings", place = it) { v -> set { st -> st.copy(showSettings = v) } } }
            row("sums") { Toggle(page, "sums", stringResource(R.string.set_show_sums), null, s.showSums, mark = "calc", place = it) { v -> set { st -> st.copy(showSums = v) } } }
            row("gemini") { Toggle(page, "gemini", stringResource(R.string.set_show_gemini), stringResource(R.string.set_show_gemini_text), s.showGemini, mark = "spark", place = it) { v -> set { st -> st.copy(showGemini = v) } } }
            row("keys") { Toggle(page, "keys", stringResource(R.string.set_show_keys), stringResource(R.string.set_show_keys_text), s.showKeys, mark = "key", place = it) { v -> set { st -> st.copy(showKeys = v) } } }
        }
    }
    // An event typed as a sentence: whether Enter saves it, and where one goes that names no calendar.
    Rise(arrive, 3, from) {
        resumed     // (read: what the system allows is looked at again when the window has the keys again)
        val reads = app.calendars.allowed
        val may = reads && app.calendars.writes
        Group(stringResource(R.string.win_events)) {
            // On only while the system's permission to add events is there: refused, or taken back, the switch stands off.
            row("save-events") {
                Toggle(page, "save-events", stringResource(R.string.set_save_events), stringResource(R.string.set_save_events_text), s.saveEvents && may, mark = "event", place = it) { on ->
                    when {
                        !on -> set { st -> st.copy(saveEvents = false) }
                        may -> set { st -> st.copy(saveEvents = true) }
                        // The system's own question first: for the list of calendars and for adding an event. Refused, nothing changes.
                        else -> window?.askCalendar(write = true) { given -> if (given) set { st -> st.copy(saveEvents = true) } }
                    }
                }
            }
            row("event-calendar") { place ->
                when {
                    // Not allowed yet: the row asks. Allowed, and no calendar that takes events: it only says so.
                    !reads -> PageRow(page, "event-calendar", stringResource(R.string.set_event_calendar), stringResource(R.string.set_event_calendar_off), place = place, mark = { MarkIcon("list") },
                        onEnter = { window?.askCalendar(write = false) }) { Word(stringResource(R.string.action_allow)) }
                    open.isEmpty() -> PageRow(page, "event-calendar", stringResource(R.string.set_event_calendar), stringResource(R.string.set_event_calendar_none), place = place, mark = { MarkIcon("list") }, still = true)
                    else -> Pick(page, "event-calendar", stringResource(R.string.set_event_calendar), stringResource(R.string.set_event_calendar_text), open.map { c -> c.owner }, names,
                        Cals.target(null, open, s.eventCalendar)?.owner.orEmpty(), mark = "list", place = place, beside = beside, cut = true) { v -> set { st -> st.copy(eventCalendar = v) } }
                }
            }
        }
    }
    Rise(arrive, 4, from) {
        val offers = app.commands.offers
        val icons = remember { app.icons ?: AppIcons(app).also { app.icons = it } }
        val show = stringResource(R.string.win_app_show)
        val hide = stringResource(R.string.win_app_hide)
        Group(stringResource(R.string.win_other_apps)) {
            row("apps") { Toggle(page, "apps", stringResource(R.string.set_app_commands), stringResource(R.string.set_app_commands_text), s.appCommands, mark = "bolt", place = it) { v -> set { st -> st.copy(appCommands = v) } } }
            if (offers.isEmpty()) row("none") { PageNote(it, stringResource(R.string.win_apps_none)) }
            // With the switch for all of them off these stay where they are, dimmed, and the keys pass over them.
            for (o in offers) row("app:${o.owner}") { place ->
                val on = o.owner !in s.mutedApps
                fun mute(off: Boolean) = set { it.copy(mutedApps = if (off) (it.mutedApps + o.owner).distinct() else it.mutedApps - o.owner) }
                PageRow(page, "app:${o.owner}", o.app, o.titles.joinToString(" · "), enabled = s.appCommands, lines = 1, place = place,
                    mark = { AppMark(icons, RowIcon.App(o.owner, o.cls, Recipes.me)) }, onEnter = { mute(on) },
                    menu = listOf(RowAction(show) { mute(false) }, RowAction(hide) { mute(true) })) { SwitchMark(on, enabled = s.appCommands) }
            }
        }
    }
}

/** The most letters of a calendar's name on the button of "New events go to". */
private const val CALENDAR_NAME = 28

/** The row of the window where the list of calendars is allowed: "Allow…" on an event's row in the panel opens the window on it. */
const val CALENDARS_ROW = "calendars"

/** An app's own icon as a row's mark. It fades in when it had to be loaded: nothing cuts in. */
@Composable
private fun AppMark(icons: AppIcons, icon: RowIcon.App) {
    val px = with(LocalDensity.current) { 48.dp.roundToPx() }
    val bitmap by produceState(icons.cached(icon), icon) { if (value == null) value = icons.load(icon, px) }
    val there by animateFloatAsState(if (bitmap != null) 1f else 0f, LocalMotion.current.fade(80), label = "icon")
    Box(Modifier.size(MARK)) { bitmap?.let { Image(it, null, Modifier.fillMaxSize().graphicsLayer { alpha = there }) } }
}

/**
 * Labs: what only works with a key of the user's own from another service (Alex: "a Labs section in settings which
 * contains features needing random keys and such"). Each group is one such thing; each is off until its key is in.
 */
@Composable
fun LabsPage(page: Page, app: BooklightApp, arrive: Animatable<Float, *>, from: Float, /** A text field of the page has the keys (a key's). */ onTyping: (Boolean) -> Unit) {
    // Spotify: the key that makes `play … on spotify` find what was named and play it (`SongsGroup.kt`).
    Rise(arrive, 1, from) { SongsGroup(page, app, onTyping) }
    // Flights: the key that makes a flight's row answer with its times (`FlightsGroup.kt`).
    Rise(arrive, 2, from) { FlightsGroup(page, app, onTyping) }
}

/**
 * Privacy: what Booklight may use (given and taken back in the system's own screens), what it keeps and the
 * row that forgets it (last in its group, never first), the policy, and who made it.
 */
@Composable
fun PrivacyPage(page: Page, app: BooklightApp, s: Settings, resumed: Int, arrive: Animatable<Float, *>, from: Float) {
    val scheme = MaterialTheme.colorScheme
    val activity = LocalActivity.current as ComponentActivity
    Rise(arrive, 1, from) {
        resumed
        val folder = if (app.notes.ready) app.notes.where else null
        val bright = app.executor.screen.allowed
        // (Read here, beside `resumed`, and not in the row: a row whose own values did not change is not drawn again, and
        // it went on saying "Not allowed yet" after the system's question had been answered with Allow.)
        val reads = app.calendars.allowed
        val writes = reads && app.calendars.writes
        Group(stringResource(R.string.win_may_use)) {
            row("notes") { place ->
                PageRow(page, "notes", stringResource(R.string.win_notes), folder?.let { stringResource(R.string.win_notes_in, it) } ?: stringResource(R.string.win_notes_none), place = place,
                    mark = { MarkIcon("note") }, onEnter = { activity.startActivity(Intent(activity, PickFolderActivity::class.java)) }) { Word(stringResource(if (folder == null) R.string.win_choose else R.string.win_change)) }
            }
            row("brightness") { place ->
                PageRow(page, "brightness", stringResource(R.string.dial_brightness), stringResource(if (bright) R.string.win_brightness_on else R.string.win_brightness_off), place = place,
                    mark = { MarkIcon("sun") }, onEnter = { app.executor.run(Effect.Grant("brightness"), activity) }) { Word(stringResource(if (bright) R.string.win_change else R.string.action_allow)) }
            }
            // The list of calendars: allowed by this row and by nothing else in Booklight. Allowed, the row leads to the
            // system's own page, where it is taken back. The line says what is read, and what is written only where the
            // switch for that is on and the system agreed.
            row(CALENDARS_ROW) { place ->
                PageRow(page, CALENDARS_ROW, stringResource(R.string.win_calendars),
                    stringResource(if (!reads) R.string.win_calendars_off else if (s.saveEvents && writes) R.string.win_calendars_write else R.string.win_calendars_on), place = place,
                    mark = { MarkIcon("event") }, onEnter = { (activity as? MainActivity)?.let { w -> if (reads) w.systemPage() else w.askCalendar(write = false) } }) {
                    Word(stringResource(if (reads) R.string.win_change else R.string.action_allow))
                }
            }
        }
    }
    Rise(arrive, 2, from) {
        // Forgetting needs Enter twice, like deleting in the panel; and it is the last row, never the first.
        var sure by remember { mutableStateOf(false) }
        var done by remember { mutableStateOf(false) }
        var asked by remember { mutableStateOf(0L) }
        LaunchedEffect(sure) { if (sure) { delay(3000); sure = false } }
        Group(stringResource(R.string.set_privacy_title)) {
            row("kept") { PageNote(it, stringResource(R.string.set_privacy_text), stringResource(R.string.about_device), mark = { MarkIcon("info") }) }
            row("forget") { place ->
                PageRow(page, "forget", stringResource(R.string.set_forget), null, place = place, mark = { MarkIcon("trash") },
                    onEnter = {
                        val now = android.os.SystemClock.uptimeMillis()
                        // A second press, a moment later: a double click or a bouncing key doesn't forget everything.
                        if (sure && now - asked >= 350) {
                            app.historyStore.clear(); app.lastText = null
                            // What was learned is gone: so are the seats it gave under the empty field, and the list of what was not to be suggested.
                            app.prefs.update { it.copy(emojiRecent = emptyList(), lettersRecent = emptyList(), usedScopes = emptyList(), used = emptyList(), zeroHeld = emptyList(), zeroHidden = emptyList()) }
                            sure = false; done = true
                        }
                        else if (!sure && !done) { sure = true; asked = now }
                    }) {
                    if (done) Text(stringResource(R.string.set_forgotten), style = MaterialTheme.typography.labelLarge)
                    else Word(stringResource(if (sure) R.string.win_forget_again else R.string.win_forget), danger = true)
                }
            }
        }
    }
    Rise(arrive, 3, from) {
        Column {
            Group {
                // Play asks for the policy to be reachable from inside the app.
                row("policy") { place ->
                    PageRow(page, "policy", stringResource(R.string.set_privacy_policy), null, place = place, mark = { MarkIcon("lock") },
                        onEnter = { app.executor.run(Effect.OpenUrl(MainActivity.PRIVACY_URL), activity) }) { Opens() }
                }
            }
            Text(stringResource(R.string.set_version, BuildConfig.VERSION_NAME) + " · " + stringResource(R.string.set_about_text), color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = INSET, end = INSET, top = 16.dp))
        }
    }
}
