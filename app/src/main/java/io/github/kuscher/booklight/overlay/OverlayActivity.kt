package io.github.kuscher.booklight.overlay

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
        shade = Shade.of(if (BuildConfig.DEBUG) intent.getStringExtra("shade") ?: settings.shadow else settings.shadow)
        take(intent)
        placeWindow()

        setContent {
            BooklightTheme(dark, tint = settings.tint) {
                CompositionLocalProvider(LocalMotion provides motion) {
                    BackHandler { close() }
                    Panel(
                        model, icons, glass && !solid, dark, arrival, leaving,
                        onHeight = ::sizeWindow, onPresence = ::present, onGlass = ::shadow,
                        onRun = ::run, onCard = ::card, onClose = ::close,
                    )
                }
            }
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
        val blur = if (solid) 0 else (dp(Look.blurDp) * focused).roundToInt()
        if (blur != lastBlur) { lastBlur = blur; window.setBackgroundBlurRadius(blur) }
        if (dim > 0f) window.setDimAmount(dim * dimmed)
    }

    /** Where the glass stands in the window and how far it has arrived: its shadow is cast from exactly there. */
    private fun shadow(left: Int, top: Int, right: Int, bottom: Int, shown: Float) {
        if (shade == Shade.OFF || !ground.place(left, top, right, bottom, shown)) return
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
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_STAY, false)) { stay = true; return }
        if (fromIcon(intent)) { startActivity(Intent(this, MainActivity::class.java)); close(); return }
        // The key again while the panel is still leaving: it turns round and opens again (unless it is leaving because something ran).
        if (leaving) { if (!ran && !isFinishing) { leaveJob?.cancel(); leaving = false; take(intent) }; return }
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
        val editable = intent.action == Intent.ACTION_PROCESS_TEXT && !intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
        model.enterScope((application as BooklightApp).scopes.receive(text.take(MAX_TEXT), editable))
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
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
        if (!app.executor.run(a.effect, this)) { say(getString(R.string.failed), bad = true); return }
        // Asking for a grant runs nothing yet: what was typed (the first note) is kept, in case the picker is cancelled.
        ran = a.effect !is Effect.Grant
        model.learn(r)
        // An emoji that was picked comes first next time.
        if (r.body is Body.Grid && r.provider == "emoji") (a.effect as? Effect.CopyText)?.let { c -> model.change { it.copy(emojiRecent = (listOf(c.text) + (it.emojiRecent - c.text)).take(14)) } }
        val word = a.done ?: if (a.effect is Effect.CopyText) getString(R.string.copied) else null
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
        leaveJob = lifecycleScope.launch { delay(arrival.leaveMs); finishNow() }
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
        const val EXTRA_STAY = "stay"
        const val EXTRA_DARK = "dark"
        /** Asked for by name (the widget, the tile): the panel, whoever started it. */
        const val ACTION_PANEL = "io.github.kuscher.booklight.PANEL"
        private const val EARLY_MS = 600L
        /** More than this of someone else's text is cut: a chip and its rows are for a phrase, not a document. */
        private const val MAX_TEXT = 20_000
        var current: WeakReference<OverlayActivity> = WeakReference(null)
    }
}
