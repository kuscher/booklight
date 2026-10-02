package io.github.kuscher.booklight.overlay

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.lifecycleScope
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.BuildConfig
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.BooklightTheme
import io.github.kuscher.booklight.window.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.function.Consumer
import kotlin.math.roundToInt

/**
 * The panel: a see-through activity whose window is exactly the panel (so the window's blur is the
 * panel's), placed in the upper middle of the screen. It is also the launcher activity, because
 * that is what a Googlebook keyboard shortcut can start.
 *
 * It closes when something is run, on Esc, on a click outside, when another window takes focus,
 * and when the shortcut is pressed again (singleInstance: the second start arrives as onNewIntent).
 */
class OverlayActivity : ComponentActivity() {
    lateinit var model: OverlayModel private set
    private var glass by mutableStateOf(false)
    private var leaving by mutableStateOf(false)
    /** Debug builds only: stay open when focus goes elsewhere (`./bl open stay`). */
    private var stay = false
    private val created = SystemClock.uptimeMillis()
    private val blurListener = Consumer<Boolean> { on -> glass = on }
    private lateinit var motion: Motion
    private var lastHeightPx = -1
    private var lastBlur = -1
    private var dark = false
    /** No blur and an opaque surface: the user chose Solid. */
    private var solid = false
    /** How much the rest of the screen darkens while the panel is open; 0 = not at all (the default). */
    private var dim = 0f
    private var flashJob: Job? = null
    /** Something was run: what was typed need not be kept for next time. */
    private var ran = false
    /** Something ran and the panel is only waiting to close: a second Enter in that moment must not run it again. */
    private var settled = false
    /** This start was a click on the icon: there is no panel, only a hand-over to the Booklight window. */
    private var handedOver = false
    private lateinit var arrival: Arrival
    /** The window's background: the blur's corners, and where the glass stands for its shadow. */
    private lateinit var ground: PanelOutline
    private var shade = Shade.MEDIUM
    private var leaveJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (fromIcon(intent)) { handOver(); return }
        current = WeakReference(this)
        val app = application as BooklightApp
        val settings = app.prefs.now
        // `--ef slow 4` (debug): every spring and tween four times as long, to step through a recording.
        motion = Motion.of(this, if (BuildConfig.DEBUG) intent.getFloatExtra("slow", 1f) else 1f)
        val screen = windowManager.maximumWindowMetrics.bounds
        model = OverlayModel(app, lifecycleScope, limit = Metrics.maxRows(screen.height() / resources.displayMetrics.density))
        model.typeStep = motion::typeStep
        model.onSay = { say(it) }
        model.onTell = { window.decorView.announceForAccessibility(it) }
        stay = BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_STAY, false)
        solid = settings.glass == "solid"
        Look.use(GlassLevel.of(settings.glass))
        var opening = settings.opening
        if (BuildConfig.DEBUG) {   // try other values from adb: ./bl open stay tint=0.2 blur=24 dim=0.1 opening=slow
            if (intent.hasExtra("tint")) intent.getFloatExtra("tint", 0f).let { Look.tintLight = it; Look.tintDark = it }
            if (intent.hasExtra("blur")) Look.blurDp = intent.getFloatExtra("blur", Look.blurDp)
            intent.getStringExtra("opening")?.let { opening = it }
        }
        val icons = app.icons ?: AppIcons(app).also { app.icons = it }
        // `--ez dark true|false` (debug, for pictures) overrides the theme for this panel only.
        dark = when {
            intent.hasExtra(EXTRA_DARK) -> intent.getBooleanExtra(EXTRA_DARK, false)
            settings.theme == "dark" -> true
            settings.theme == "light" -> false
            else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        dim = when {
            BuildConfig.DEBUG && intent.hasExtra("dim") -> intent.getFloatExtra("dim", 0f)
            settings.dim -> if (dark) Look.dimDark else Look.dimLight
            else -> 0f
        }
        arrival = Arrival.of(opening, motion)
        // Opened the way a key opens it (a keyboard shortcut starts the launcher activity; the assistant key asks for
        // assistance): Booklight has a key, and need not ask for one.
        if (!settings.keySeen && byKey(intent)) app.prefs.update { it.copy(keySeen = true) }
        shade = Shade.of(if (BuildConfig.DEBUG) intent.getStringExtra("shade") ?: settings.shadow else settings.shadow)
        take(intent)
        placeWindow()

        val content = ComposeView(this).apply { setContent {
            BooklightTheme(dark, tint = settings.tint) {
                CompositionLocalProvider(LocalMotion provides motion) {
                    BackHandler { close() }
                    Panel(
                        model, icons, glass && !solid, dark, arrival, leaving,
                        onHeight = ::sizeWindow, onPresence = ::present, frame = frame,
                        onRun = ::run, onCard = ::card, onClose = ::close,
                    )
                }
            }
        } }
        setContentView(KeysFirst(this, content))
        // A row of the window's Commands page: Booklight types its example, once the panel has opened.
        app.example?.let { text ->
            app.example = null; model.guided = true
            // Once the panel has opened, whatever speed it opens at; then a beat, then the letters.
            lifecycleScope.launch { androidx.compose.runtime.snapshotFlow { model.arrived }.first { it }; delay(motion.hold(TYPE_AFTER_MS)); if (!leaving) model.typeOut(text) }
        }
        window.decorView.post {
            Log.i(BooklightApp.TAG, "panel shown ${SystemClock.uptimeMillis() - created} ms after onCreate, ${SystemClock.uptimeMillis() - android.os.Process.getStartUptimeMillis()} ms after process start")
        }
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density

    private fun placeWindow() {
        window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        // Optional: a soft dim separates the panel from busy windows. Off unless the user turns it on.
        if (dim > 0f) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(if (motion.on) 0f else dim)
        }
        // The background draws nothing; the platform reads the blur region's corner radius from its outline.
        ground = PanelOutline(dp(Metrics.radius.value))
        window.setBackgroundDrawable(ground)
        window.decorView.viewTreeObserver.addOnPreDrawListener { frameGlass(); true }
        // The blur is asked for here, not with the first frame: the platform turns it on in a message of its own, which
        // would wait behind the opening's frames. With the glass's first frame it is already there.
        if (motion.on && !solid) present(0f, if (arrival.unfold) 1f else 0f)
        if (shade != Shade.OFF) {
            // The shadow is the system's own, cast by the window's root view; it lies in room the system adds around the
            // window for it, where the window's blur does not reach. Darker in dark theme, where it has less to show against.
            val deep = if (dark) 1.4f else 1f
            ground.shaded = true
            window.decorView.apply {
                outlineProvider = ground.caster
                outlineAmbientShadowColor = android.graphics.Color.argb((shade.ambient * deep).coerceAtMost(1f), 0f, 0f, 0f)
                outlineSpotShadowColor = android.graphics.Color.argb((shade.spot * deep).coerceAtMost(1f), 0f, 0f, 0f)
            }
            window.setElevation(dp(shade.height))
        }
        val screen = windowManager.maximumWindowMetrics.bounds
        val lp = window.attributes
        lp.width = minOf(dp(Metrics.width.value).roundToInt(), screen.width() - dp(48f).roundToInt())
        // The opening is the field's height; what is under the field arrives after it (Panel's gate).
        lp.height = dp((if (arrival.unfold) Metrics.field else Metrics.height(model)).value).roundToInt()
        lp.y = (screen.height() * Metrics.TOP).roundToInt()
        window.attributes = lp
        lastHeightPx = lp.height
        if (!motion.on) present(1f, 1f)
    }

    /** Called on every frame of the height spring: the window is always exactly as tall as the panel. Its width never changes. */
    private fun sizeWindow(height: Dp) {
        val px = dp(height.value).roundToInt().coerceAtLeast(1)
        if (px == lastHeightPx) return
        lastHeightPx = px
        val lp = window.attributes
        lp.height = px
        window.attributes = lp
    }

    /** The room dims (if asked to) and the glass comes into focus as the panel arrives, and they let go as it leaves. */
    private fun present(dimmed: Float, focused: Float) {
        // Never 0 on the way in: at 0 the platform lets go of the blur, and has to be asked again.
        val blur = if (solid) 0 else (dp(Look.blurDp) * focused).roundToInt().coerceAtLeast(if (leaving) 0 else 1)
        if (blur != lastBlur) { lastBlur = blur; window.setBackgroundBlurRadius(blur) }
        if (dim > 0f) window.setDimAmount(dim * dimmed)
    }

    private val frame = GlassFrame()
    private var framed = false

    /**
     * Before each frame is drawn: the window's root view is framed to the glass as it stands, and what is in it is
     * moved back so that it stays where it was on screen. The blur's region is the root view's rectangle, sent with
     * the frame that is drawn (docs/research/device-findings.md): so the blur is the glass's own from its first
     * frame, while the window itself stays the panel's final rectangle (resized in width it is neither smooth nor
     * symmetric). The shadow is cast from the same rectangle.
     */
    private fun frameGlass() {
        val root = window.decorView as ViewGroup
        val content = root.getChildAt(0) ?: return
        val w = content.width; val h = content.height
        if (w == 0 || h == 0) return
        val box = frame.at(w, h)
        val follow = glass && !solid && (box.left != 0 || box.top != 0 || box.right != w || box.bottom != h)
        if (follow || framed) {
            // A layout pass gives the root view the whole window again, so the frame is set anew every time.
            framed = follow; ground.framed = follow
            val left = if (follow) box.left else 0; val top = if (follow) box.top else 0
            root.setLeftTopRightBottom(left, top, if (follow) box.right else w, if (follow) box.bottom else h)
            for (i in 0 until root.childCount) root.getChildAt(i).apply { translationX = -left.toFloat(); translationY = -top.toFloat() }
        }
        if (framed) shadow(0, 0, box.width, box.height, frame.cast()) else shadow(box.left, box.top, box.right, box.bottom, frame.cast())
    }

    /** Where the glass stands in the root view and how far it has arrived: its shadow is cast from exactly there. */
    private fun shadow(left: Int, top: Int, right: Int, bottom: Int, shown: Float) {
        if (!ground.place(left, top, right, bottom, shown) || shade == Shade.OFF) return
        window.decorView.invalidateOutline()
        ground.invalidateSelf()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        glass = windowManager.isCrossWindowBlurEnabled
        windowManager.addCrossWindowBlurEnabledListener(blurListener)
    }

    override fun onDetachedFromWindow() {
        windowManager.removeCrossWindowBlurEnabledListener(blurListener)
        super.onDetachedFromWindow()
    }

    /** The shortcut again while the panel is open: put it away. */
    /** The key again while the panel is still leaving: it turns round and opens again (unless it is leaving because something ran). */
    fun turn(intent: Intent) { if (leaving && !ran && !isFinishing) { leaveJob?.cancel(); leaving = false; take(intent) } }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_STAY, false)) { stay = true; return }
        if (fromIcon(intent)) { startActivity(Intent(this, MainActivity::class.java)); close(); return }
        if (leaving) { turn(intent); return }
        (application as BooklightApp).let { app -> app.example?.let { app.example = null; model.typeOut(it); return } }
        if (!take(intent)) close()
    }

    /**
     * Was this start a click on the app's icon? Then the Booklight window is wanted, not the panel. The
     * launcher, the taskbar and the Apps list all belong to the home app, and they say where on screen the
     * icon was (source bounds). A keyboard shortcut, the assistant key, the widget, the tile and adb do neither.
     */
    private fun fromIcon(intent: Intent): Boolean {
        if (intent.action != Intent.ACTION_MAIN || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)) return false
        if (intent.sourceBounds != null) return true
        val home = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        return home != null && referrer?.host == home
    }

    /**
     * Was this start a keyboard shortcut or the assistant key? A shortcut starts the launcher activity, as "Open" in
     * the store, in the installer and in Settings' App info do: those say who they are, and they are not a key.
     */
    private fun byKey(intent: Intent): Boolean {
        if (BuildConfig.DEBUG && intent.getBooleanExtra("key", false)) return true
        if (intent.action == Intent.ACTION_ASSIST) return true
        if (intent.action != Intent.ACTION_MAIN || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)) return false
        val from = referrer?.host ?: return true
        val installer = runCatching { packageManager.getInstallSourceInfo(packageName).installingPackageName }.getOrNull()
        return from != installer && from != packageName && from !in NOT_A_KEY
    }

    /** The icon was clicked: open the Booklight window and leave without ever showing the panel. */
    private fun handOver() {
        handedOver = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
    }

    /** Text another app handed over (its selection menu, its share sheet) becomes the chip. True if there was any. */
    private fun take(intent: Intent): Boolean {
        val text = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            else -> null
        }?.toString()?.takeIf { it.isNotBlank() } ?: return false
        // A selection in a field that can be edited comes with the offer to take text back.
        // (Not for several paragraphs: a prompt is given the text on one line, and its answer would come back as one.)
        val editable = intent.action == Intent.ACTION_PROCESS_TEXT && !intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false) && '\n' !in text.trim()
        model.enterScope((application as BooklightApp).scopes.receive(text.take(MAX_TEXT), editable))
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) model.focused = true
        if (hasFocus || stay || handedOver) return
        // Whatever started the panel (the taskbar, a widget, the Apps list) may still be closing and
        // take focus for a moment: early on, only close if focus is still gone a little later.
        if (SystemClock.uptimeMillis() - created < EARLY_MS) window.decorView.postDelayed({ if (!hasWindowFocus() && !isFinishing) close() }, 250)
        else close()
    }

    /** The window takes every touch on the screen while it is up; one outside the panel closes it. */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_OUTSIDE) {
            val d = window.decorView
            if (event.x < 0 || event.y < 0 || event.x > d.width || event.y > d.height) { close(); return true }
        }
        return super.onTouchEvent(event)
    }

    /** Runs an action of a row. [keep]: Shift was held, so the panel stays whatever the action says. */
    fun run(r: Result, a: Action, keep: Boolean = false) {
        if (leaving || settled) return   // a second Enter or click while the panel is on its way out
        val app = application as BooklightApp
        // A pinned text exists nowhere else: when another pin takes its place, the footer says which one went.
        fun pins(e: Effect): Boolean = e is Effect.Pin || (e is Effect.Steps && e.steps.any(::pins))
        val replaced = app.pinned.value?.takeIf { it.kind == "text" && pins(a.effect) }?.title(this)?.let { getString(R.string.pin_replaced, if (it.length > 24) it.take(23) + "…" else it) }
        // The row stays, and the footer says why. A link's address, or what another app was asked for, found nobody to open it
        // (an app's own address is no browser's business).
        if (!app.executor.run(a.effect, this)) { say(getString(if (a.effect is Effect.OpenUrl || a.effect is Effect.Open) R.string.failed_open else R.string.failed), bad = true); return }
        // Asking for a grant runs nothing yet: what was typed (the first note) is kept, in case the picker is cancelled.
        ran = a.effect !is Effect.Grant
        // What counts as a run: opening it, or doing its thing. Not looking at its details, changing it or removing it:
        // "chrome uninstall", Enter, Cancel must not make Chrome the thing that was run last.
        if (!a.danger && a.id !in NOT_A_RUN) model.learn(r, a) else app.lastText = null
        model.used(r, a)
        // An emoji that was picked comes first next time.
        if (r.body is Body.Grid && r.provider == "emoji") (a.effect as? Effect.CopyText)?.let { c -> model.change { it.copy(emojiRecent = (listOf(c.text) + (it.emojiRecent - c.text)).take(14)) } }
        if (r.body is Body.Grid && r.provider == "abc") (a.effect as? Effect.CopyText)?.let { c -> model.change { it.copy(lettersRecent = (listOf(c.text) + (it.lettersRecent - c.text)).take(14)) } }
        val word = a.done ?: if (a.effect is Effect.CopyText) getString(R.string.copied) else replaced
        if (word != null) say(word)
        when {
            a.keepOpen || keep -> model.refresh()
            // Long enough to read the word, or to see a level arrive, then away.
            word != null -> { settled = true; lifecycleScope.launch { delay(520); close() } }
            r.nudge != null -> { settled = true; model.refresh(); lifecycleScope.launch { delay(400); close() } }
            else -> close()
        }
    }

    /** A word in the footer for a moment. */
    private fun say(word: String, bad: Boolean = false) {
        model.flash = word; model.flashBad = bad
        flashJob?.cancel()
        flashJob = lifecycleScope.launch { delay(1600); model.flash = null }
    }

    /** A button on the first-run card. Either way the step is done and doesn't come back. */
    private fun card(card: Card, primary: Boolean) {
        if (leaving) return
        when (card) {
            Card.SHORTCUT -> {
                model.change { it.copy(shortcutCard = false) }
                if (primary) { requestShowKeyboardShortcuts(); close() }
            }
            Card.SUGGESTIONS -> model.change { it.copy(suggestionsCard = false, suggestions = primary) }
        }
    }

    /** Leaves: the panel folds away the way it came (or fades, with the opening turned off), its blur with it, then the activity finishes. */
    fun close() {
        if (leaving || handedOver) return
        if (!ran) model.keep()
        leaving = true
        if (!motion.on) { finishNow(); return }
        leaveJob = lifecycleScope.launch { delay(motion.hold(arrival.leaveMs)); finishNow() }   // (a debug build's slow motion stretches the wait with the fold)
    }

    private fun finishNow() {
        if (isFinishing) return
        finish()
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
    }

    override fun onStop() {
        super.onStop()
        if (handedOver) return
        // Covered or sent away without a close (a new desk, the lock screen): don't come back half open.
        if (!stay) { if (!ran && !leaving) model.keep(); finishNow() }
    }

    override fun onDestroy() {
        if (current.get() === this) {
            current = WeakReference(null)
            (application as BooklightApp).let { it.scopes.forget(); it.onDevice.close() }
        }
        super.onDestroy()
    }

    companion object {
        /** Actions that do not count as running the thing: App info and the app's other pages in Settings, Edit, Delete (and whatever is marked as removing). */
        private val NOT_A_RUN = setOf("info", "edit", "delete") + io.github.kuscher.booklight.providers.AppPages.ids
        const val EXTRA_STAY = "stay"
        const val EXTRA_DARK = "dark"
        /** Who starts the launcher activity with a button of their own that says "Open". */
        private val NOT_A_KEY = setOf("com.android.vending", "com.google.android.packageinstaller", "com.android.packageinstaller", "com.android.settings", "com.android.shell")
        private const val TYPE_AFTER_MS = 160L
        /** Asked for by name (the widget, the tile): the panel, whoever started it. */
        const val ACTION_PANEL = "io.github.kuscher.booklight.PANEL"
        private const val EARLY_MS = 600L
        /** More than this of someone else's text is cut: a chip and its rows are for a phrase, not a document. */
        private const val MAX_TEXT = 20_000
        var current: WeakReference<OverlayActivity> = WeakReference(null)
    }
}

/**
 * The panel's content, in a view that takes a typed key before the input method does. On a Googlebook every key of
 * the keyboard goes to the input method first, which hands the letter back as text a moment later. When the panel
 * changes the field's text itself in that moment (a keyword becomes the chip), the input method is started anew and
 * the letter it was still holding is lost, or lands after the next one: "fix teh" arrived as "fix eh" and as "fix
 * eth". So keys that print, Space and Backspace go straight to the field, where each is in the text before the next
 * is looked at. Keys with Ctrl, Alt or the Action key are left to the input method, and so is all typing in a
 * language that is put together by the input method itself ([COMPOSED]).
 */
private class KeysFirst(context: Context, private val content: View) : FrameLayout(context) {
    private val composed = context.getSystemService(InputMethodManager::class.java)?.currentInputMethodSubtype?.languageTag.orEmpty().substringBefore('-').lowercase() in COMPOSED

    init { addView(content) }

    override fun dispatchKeyEventPreIme(e: KeyEvent): Boolean {
        val plain = !e.isCtrlPressed && !e.isAltPressed && !e.isMetaPressed
        // Only a key that prints something as it is pressed now (a number-pad key with Num Lock off is an arrow), and only if
        // the field took it: what nobody took goes the usual way, and keeps what the system makes of it.
        if (plain && !composed && (e.unicodeChar != 0 || e.keyCode == KeyEvent.KEYCODE_DEL) && content.dispatchKeyEvent(e)) return true
        return super.dispatchKeyEventPreIme(e)
    }
}

/** Languages whose text the input method puts together from several keys: their typing must go through it. */
private val COMPOSED = setOf("zh", "ja", "ko", "vi", "hi", "bn", "mr", "ta", "te", "kn", "ml", "gu", "pa", "ne", "si", "th", "km", "lo", "my")
