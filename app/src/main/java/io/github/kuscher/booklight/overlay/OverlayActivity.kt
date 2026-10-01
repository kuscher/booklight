package io.github.kuscher.booklight.overlay

import android.content.Intent
import android.graphics.Outline
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
 * The panel: a see-through activity whose window is exactly the panel (so the window's blur and
 * shadow are the panel's), placed in the upper middle of the screen. It is also the launcher
 * activity, because that is what a Googlebook keyboard shortcut can start.
 *
 * It closes when something is run, on Esc, on a click outside, when another window takes focus,
 * and when the shortcut is pressed again (singleInstance: the second start arrives as onNewIntent).
 */
class OverlayActivity : ComponentActivity() {
    lateinit var model: OverlayModel private set
    private var glass by mutableStateOf(false)
    private var closing = false
    /** Debug: stay open when focus goes elsewhere (`./bl open stay`). */
    private var stay = false
    private val created = SystemClock.uptimeMillis()
    private val blurListener = Consumer<Boolean> { on -> glass = on }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        current = WeakReference(this)
        val app = application as BooklightApp
        model = OverlayModel(app, lifecycleScope)
        stay = intent.getBooleanExtra(EXTRA_STAY, false)
        val icons = app.icons ?: AppIcons(app).also { app.icons = it }
        placeWindow()

        setContent {
            BooklightTheme {
                BackHandler { close() }
                Panel(model, icons, glass, onRun = ::run, onClose = ::close)
                // The window follows the panel: taller as rows arrive, never moving its top edge.
                LaunchedEffect(Unit) { snapshotFlow { Metrics.height(model) }.collect { sizeWindow() } }
            }
        }
        window.decorView.post {
            Log.i(BooklightApp.TAG, "panel shown ${SystemClock.uptimeMillis() - created} ms after onCreate, ${SystemClock.uptimeMillis() - android.os.Process.getStartUptimeMillis()} ms after process start")
        }
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density

    private fun placeWindow() {
        val radius = dp(Metrics.radius.value)
        window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        // A soft dim separates the panel from busy windows; the desktop stays readable.
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(DIM)
        // The window's outline: the blur region's corners, the clip and the shadow all follow it.
        window.decorView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
                outline.alpha = 1f
            }
        }
        window.setBackgroundDrawable(PanelOutline(radius))
        window.decorView.clipToOutline = true
        window.setBackgroundBlurRadius(dp(BLUR_DP).roundToInt())
        sizeWindow()
    }

    private fun sizeWindow() {
        val screen = windowManager.maximumWindowMetrics.bounds
        val lp = window.attributes
        lp.width = minOf(dp(Metrics.width.value).roundToInt(), screen.width() - dp(48f).roundToInt())
        lp.height = dp(Metrics.height(model).value).roundToInt()
        lp.y = (screen.height() * Metrics.TOP).roundToInt()
        window.attributes = lp
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
        if (!app.executor.run(a.effect, this)) return
        model.learn(r)
        when {
            a.effect is Effect.CopyText -> {
                model.flash = getString(R.string.copied)
                lifecycleScope.launch { delay(500); close() }
            }
            !a.keepOpen -> close()
        }
    }

    fun close() {
        if (closing) return
        closing = true
        finish()
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
    }

    override fun onDestroy() {
        if (current.get() === this) current = WeakReference(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_STAY = "stay"
        const val DIM = 0.16f
        const val BLUR_DP = 40f
        var current: WeakReference<OverlayActivity> = WeakReference(null)
    }
}
