package io.github.kuscher.booklight.overlay

import android.content.Intent
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
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.BooklightTheme
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
    /** Debug: stay open when focus goes elsewhere (`./bl open stay`). */
    private var stay = false
    private val created = SystemClock.uptimeMillis()
    private val blurListener = Consumer<Boolean> { on -> glass = on }
    private lateinit var motion: Motion
    private var lastHeightPx = -1
    private var lastBlur = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        current = WeakReference(this)
        val app = application as BooklightApp
        motion = Motion.of(this)
        val screen = windowManager.maximumWindowMetrics.bounds
        model = OverlayModel(app, lifecycleScope, limit = Metrics.maxRows(screen.height() / resources.displayMetrics.density))
        stay = intent.getBooleanExtra(EXTRA_STAY, false)
        val icons = app.icons ?: AppIcons(app).also { app.icons = it }
        // `--ez dark true|false` (debug, for pictures) overrides the system's theme for this panel only.
        val dark = if (intent.hasExtra(EXTRA_DARK)) intent.getBooleanExtra(EXTRA_DARK, false)
            else resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        placeWindow()

        setContent {
            BooklightTheme(dark) {
                CompositionLocalProvider(LocalMotion provides motion) {
                    BackHandler { close() }
                    Panel(
                        model, icons, glass, dark, leaving,
                        onHeight = ::sizeWindow, onPresence = ::present,
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
        // A soft dim separates the panel from busy windows; the desktop stays readable.
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(if (motion.on) 0f else DIM)
        // The background draws nothing; the platform reads the blur region's corner radius from its outline.
        window.setBackgroundDrawable(PanelOutline(dp(Metrics.radius.value)))
        val screen = windowManager.maximumWindowMetrics.bounds
        val lp = window.attributes
        lp.width = minOf(dp(Metrics.width.value).roundToInt(), screen.width() - dp(48f).roundToInt())
        lp.height = dp(Metrics.height(model).value).roundToInt()
        lp.y = (screen.height() * Metrics.TOP).roundToInt()
        window.attributes = lp
        lastHeightPx = lp.height
        if (!motion.on) present(1f)
    }

    /** Called on every frame of the height spring: the window is always exactly as tall as the panel. */
    private fun sizeWindow(height: Dp) {
        val px = dp(height.value).roundToInt().coerceAtLeast(1)
        if (px == lastHeightPx) return
        lastHeightPx = px
        val lp = window.attributes
        lp.height = px
        window.attributes = lp
    }

    /** The glass comes into focus as the panel arrives, and lets go as it leaves. */
    private fun present(amount: Float) {
        val blur = (dp(BLUR_DP) * amount).roundToInt()
        if (blur != lastBlur) { lastBlur = blur; window.setBackgroundBlurRadius(blur) }
        window.setDimAmount(DIM * amount)
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
        if (intent.getBooleanExtra(EXTRA_STAY, false)) { stay = true; return }
        close()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !stay) close()
    }

    /** The window takes every touch on the screen while it is up; one outside the panel closes it. */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_OUTSIDE) {
            val d = window.decorView
            if (event.x < 0 || event.y < 0 || event.x > d.width || event.y > d.height) { close(); return true }
        }
        return super.onTouchEvent(event)
    }

    fun run(r: Result, a: Action) {
        val app = application as BooklightApp
        if (!app.executor.run(a.effect, this)) { model.flash = getString(R.string.failed); return }
        model.learn(r)
        when {
            a.effect is Effect.CopyText -> {
                model.flash = getString(R.string.copied)
                lifecycleScope.launch { delay(520); close() }
            }
            !a.keepOpen -> close()
        }
    }

    /** A button on the first-run card. Either way the step is done and doesn't come back. */
    private fun card(card: Card, primary: Boolean) {
        when (card) {
            Card.SHORTCUT -> {
                model.change { it.copy(shortcutCard = false) }
                if (primary) { requestShowKeyboardShortcuts(); close() }
            }
            Card.SUGGESTIONS -> model.change { it.copy(suggestionsCard = false, suggestions = primary) }
        }
    }

    /** Leaves: the panel fades with its blur, then the activity finishes. */
    fun close() {
        if (leaving) return
        leaving = true
        if (!motion.on) { finishNow(); return }
        lifecycleScope.launch { delay(Motion.LEAVE_MS); finishNow() }
    }

    private fun finishNow() {
        if (isFinishing) return
        finish()
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
    }

    override fun onStop() {
        super.onStop()
        // Covered or sent away without a close (a new desk, the lock screen): don't come back half open.
        if (!stay) finishNow()
    }

    override fun onDestroy() {
        if (current.get() === this) current = WeakReference(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_STAY = "stay"
        const val EXTRA_DARK = "dark"
        const val DIM = 0.14f
        const val BLUR_DP = 64f
        var current: WeakReference<OverlayActivity> = WeakReference(null)
    }
}
