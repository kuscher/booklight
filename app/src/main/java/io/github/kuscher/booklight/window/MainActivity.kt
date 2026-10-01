package io.github.kuscher.booklight.window

import android.content.Intent
import android.os.Bundle
import android.provider.Settings as SystemSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
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
 * The Booklight window: what Booklight is, how to give it a key, and every setting. It is what the
 * app's icon opens (the keyboard shortcut opens the panel). An ordinary window, built from the
 * panel's parts: rows on one ground, one pill that glides between them, strips for choices.
 */
class MainActivity : ComponentActivity() {
    /** What the panel asked to be edited (`snippet`, `quicklink`, `recipe`) and which one; consumed by the page. */
    private var edit by mutableStateOf<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) take(intent)     // not again when the window is only rebuilt
        val app = application as BooklightApp
        val motion = Motion.of(this)
        setContent {
            val s by app.prefs.state.collectAsState()
            val dark = isDark(s.theme)
            BooklightTheme(dark, tint = s.tint) {
                CompositionLocalProvider(LocalMotion provides motion, LocalDark provides dark) {
                    Window(app, s, edit, onEdited = { edit = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_EDIT) ?: return
        edit = kind to intent.getStringExtra(EXTRA_ID).orEmpty()
    }

    companion object {
        const val EXTRA_EDIT = "edit"
        const val EXTRA_ID = "id"
        const val PRIVACY_URL = "https://googlebook.studio/privacy/booklight"
    }
}

@Composable
private fun Window(app: BooklightApp, s: Settings, edit: Pair<String, String>?, onEdited: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val activity = LocalActivity.current as ComponentActivity
    val page = remember { Page() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    /** A text field of an editor has the keyboard: the arrow keys are its own. */
    var typing by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var viewport by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    // What needs a look when the window comes back from Settings or the folder picker.
    var resumed by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { resumed++; onPauseOrDispose { } }
    fun set(change: (Settings) -> Settings) = app.prefs.update(change)

    /** Keeps the selected row in view after an arrow key. */
    fun reveal(e: Page.Entry) {
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

    Box(
        Modifier.fillMaxSize().background(scheme.surfaceContainerHigh).safeDrawingPadding()
            .onGloballyPositioned { viewport = it.size.height }
            .focusRequester(focus).focusable()
            .onPreviewKeyEvent { e ->
                if (typing || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val again = e.nativeKeyEvent.repeatCount > 0     // a held key runs a row once
                when (e.key) {
                    Key.DirectionDown -> { page.move(1)?.let(::reveal); true }
                    Key.DirectionUp -> { page.move(-1)?.let(::reveal); true }
                    Key.DirectionLeft -> page.current?.step?.let { it(-1); true } ?: false
                    Key.DirectionRight -> page.current?.step?.let { it(1); true } ?: false
                    Key.Enter, Key.NumPadEnter, Key.Spacebar -> page.current?.let { if (!again) it.enter(); true } ?: false
                    else -> false
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.verticalScroll(scroll).widthIn(max = 760.dp).padding(horizontal = 20.dp).onGloballyPositioned { page.root = it }) {
            // The page's one selection, behind the rows.
            page.placed
            val on = page.selected?.let { page.rows[it] }
            with(density) { Pill((on?.top ?: 0f).toDp(), (on?.height ?: 0f).toDp(), visible = on != null) }

            // Sections rise in one after the other, once.
            val arrive = remember { Animatable(if (motion.on) 0f else 1f) }
            LaunchedEffect(Unit) { arrive.animateTo(1f, motion.fade(520)) }
            Column(Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 64.dp)) {
                Rise(arrive, 0) {
                    Column {
                        Text(stringResource(R.string.app_name), color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.round, fontSize = 45.sp, fontWeight = FontWeight(700)), modifier = Modifier.padding(start = GUTTER))
                        Text(stringResource(R.string.win_lead), color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 20.sp, lineHeight = 28.sp),
                            modifier = Modifier.padding(start = GUTTER, end = GUTTER, top = 6.dp, bottom = 24.dp))
                        Stage(app)
                    }
                }

                Rise(arrive, 1) {
                    Section(stringResource(R.string.win_key_title), stringResource(R.string.win_key_text)) {
                        Row(Modifier.padding(start = GUTTER, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Keys(stringResource(R.string.key_action), "Alt", stringResource(R.string.key_space), ink = scheme.onSurface)
                            Text(stringResource(R.string.win_key_or), color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp))
                            Keys(stringResource(R.string.key_action), "K", ink = scheme.onSurface)
                        }
                        PageRow(page, "key", stringResource(R.string.set_shortcut_button), stringResource(R.string.win_key_steps),
                            mark = { Icon(Symbols.of("key"), null, tint = it) }, onEnter = { activity.requestShowKeyboardShortcuts() }) { Icon(Symbols.of("open"), null, tint = it) }
                    }
                }

                Rise(arrive, 2) {
                    Section(stringResource(R.string.set_look_title)) {
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

                Rise(arrive, 3) {
                    Section(stringResource(R.string.set_search_title)) {
                        Choice(page, "engine", stringResource(R.string.set_engine), null, Engines.all.map { it.id }, null, s.engine, names = Engines.all.map { it.name }) { v -> set { it.copy(engine = v) } }
                        val engine = s.engine()
                        Toggle(page, "suggest", stringResource(R.string.set_suggestions), stringResource(R.string.set_suggestions_text, engine.name, engine.suggestHost), s.suggestions) { v -> set { it.copy(suggestions = v, suggestionsCard = false) } }
                    }
                }

                Rise(arrive, 4) { Commands(page, app, s, edit, onEdited, onTyping = { typing = it; if (!it) focus.requestFocus() }) }
                // "Edit…" in the panel opens the window at that item: bring it into view once it has a place.
                LaunchedEffect(edit) {
                    val key = edit?.let { "${it.first}:${it.second}" } ?: return@LaunchedEffect
                    repeat(30) { page.rows[key]?.takeIf { it.height > 0 }?.let { reveal(it); return@LaunchedEffect }; delay(50) }
                }

                Rise(arrive, 5) {
                    Section(stringResource(R.string.set_results_title)) {
                        Toggle(page, "pages", stringResource(R.string.set_show_settings), null, s.showSettings) { v -> set { it.copy(showSettings = v) } }
                        Toggle(page, "sums", stringResource(R.string.set_show_sums), null, s.showSums) { v -> set { it.copy(showSums = v) } }
                        Toggle(page, "gemini", stringResource(R.string.set_show_gemini), stringResource(R.string.set_show_gemini_text), s.showGemini) { v -> set { it.copy(showGemini = v) } }
                    }
                }

                Rise(arrive, 6) {
                    Section(stringResource(R.string.win_access_title), stringResource(R.string.win_access_text)) {
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

                Rise(arrive, 7) {
                    Section(stringResource(R.string.set_privacy_title), stringResource(R.string.set_privacy_text)) {
                        // Forgetting needs Enter twice, like deleting in the panel.
                        var sure by remember { mutableStateOf(false) }
                        var done by remember { mutableStateOf(false) }
                        var asked by remember { mutableStateOf(0L) }
                        LaunchedEffect(sure) { if (sure) { delay(3000); sure = false } }
                        PageRow(page, "forget", stringResource(R.string.set_forget), null, mark = { Icon(Symbols.of("trash"), null, tint = it) },
                            onEnter = {
                                val now = android.os.SystemClock.uptimeMillis()
                                // A second press, a moment later: a double click or a bouncing key doesn't forget everything.
                                if (sure && now - asked >= 350) { app.historyStore.clear(); sure = false; done = true }
                                else if (!sure && !done) { sure = true; asked = now }
                            }) {
                            Text(stringResource(if (done) R.string.set_forgotten else if (sure) R.string.win_forget_again else R.string.action_delete),
                                color = if (sure) scheme.error else it, style = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(600)))
                        }
                        // Play asks for the policy to be reachable from inside the app.
                        PageRow(page, "policy", stringResource(R.string.set_privacy_policy), null, mark = { Icon(Symbols.of("lock"), null, tint = it) },
                            onEnter = { app.executor.run(Effect.OpenUrl(MainActivity.PRIVACY_URL), activity) }) { Icon(Symbols.of("open"), null, tint = it) }
                    }
                }

                Rise(arrive, 8) {
                    Column(Modifier.padding(start = GUTTER, end = GUTTER, top = 44.dp)) {
                        val quiet = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, lineHeight = 20.sp)
                        Text(stringResource(R.string.set_version, BuildConfig.VERSION_NAME), color = scheme.onSurface.copy(alpha = SECOND), style = quiet)
                        Text(stringResource(R.string.set_about_text), color = scheme.onSurface.copy(alpha = SECOND), style = quiet)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Sections rise 12 dp and fade in, one after the other, once: [i] is the section's place in that order. */
@Composable
private fun Rise(arrive: Animatable<Float, *>, i: Int, content: @Composable () -> Unit) {
    val rise = with(LocalDensity.current) { 12.dp.toPx() }
    Box(Modifier.graphicsLayer {
        val t = ((arrive.value - i * 0.06f) / 0.45f).coerceIn(0f, 1f)
        alpha = t; translationY = (1f - t) * rise
    }) { content() }
}

/** A row whose control is one of several: an option strip with one gliding highlight. Left and Right step through it. */
@Composable
fun Choice(page: Page, key: String, title: String, about: String?, ids: List<String>, labels: List<Int>?, chosen: String, names: List<String>? = null, onPick: (String) -> Unit) {
    val at = ids.indexOf(chosen).coerceAtLeast(0)
    val shown = names ?: labels!!.map { stringResource(it) }
    PageRow(page, key, title, about, onStep = { d -> ids.getOrNull(at + d)?.let(onPick) }, onEnter = { onPick(ids[(at + 1) % ids.size]) }) { ink ->
        OptionStrip(shown, at, onChoose = {}, onRun = { onPick(ids[it]) }, mark = Symbols.check, quiet = true, ink = ink)
    }
}

/** A row that is on or off. Enter, Space and a click flip it; Left is off, Right is on. */
@Composable
fun Toggle(page: Page, key: String, title: String, about: String?, on: Boolean, onChange: (Boolean) -> Unit) {
    PageRow(page, key, title, about, onStep = { d -> onChange(d > 0) }, onEnter = { onChange(!on) }) { ink -> FlatSwitch(on, ink) }
}
