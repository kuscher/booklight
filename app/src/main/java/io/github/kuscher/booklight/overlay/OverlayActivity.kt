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
import android.view.KeyboardShortcutGroup
import android.view.Menu
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
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.data.firstRun
import io.github.kuscher.booklight.device.SystemWords
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
    /**
     * Something ran and the panel is only waiting to close: a second Enter in that moment must not run it again. Also while
     * the list of one of first run's lessons waits to give way: its lesson is over, and a second Enter must not run for real
     * what the first one only practised.
     */
    private var settled = false
    /** This start was a click on the icon: there is no panel, only a hand-over to the Booklight window. */
    private var handedOver = false
    /** This panel begins with first run's opening piece (core `FirstRun.overture`): for it alone the desk behind dims as the piece asks ([desk]). */
    private var staged = false
    private lateinit var arrival: Arrival
    /** The window's background: the blur's corners, and where the glass stands for its shadow. */
    private lateinit var ground: PanelOutline
    private var shade = Shade.MEDIUM
    private var leaveJob: Job? = null
    /** First run's hold: the panel kept alive behind the system's Keyboard shortcuts dialog while it waits for its key (core `FirstRun.hold`). */
    private var hold = FirstRun.Hold.NONE
    /** The hold, by its name: for the debug hooks. */
    val held: String get() = hold.name
    /** This activity is the one in front. False while another has its place, if only for the moment the key's own start takes. */
    private var topResumed = true
    /** The start that was described last, and what was made of it ([start]). */
    private var described: Pair<Intent, FirstRun.Start>? = null
    /**
     * A screen reader is switched on while first run's opening piece plays: the piece is a picture and a performance, with
     * nothing in it to read, so it ends there and the key's step stands, which says itself. Listened for only in a panel
     * that began with the piece, and until that panel goes.
     */
    private val readerCame = android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener { on -> if (on) model.land() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as BooklightApp
        val screenDp = windowManager.maximumWindowMetrics.bounds.height() / resources.displayMetrics.density
        lastStart = said(intent)
        // A click on the icon opens the Booklight window. But the first one of a new installation that still asks for its key
        // shows the panel, once: whoever starts from the Apps list meets the key's step, not the settings.
        if (fromIcon(intent)) {
            if (!FirstRun.iconShowsPanel(app.prefs.now.firstRun(), screenDp)) { handOver(); return }
            first(app) { FirstRun.iconClicked(it) }
        }
        current = WeakReference(this)
        // (This panel's own screen says whether "First steps" is offered in it: `BooklightApp.firstFits`.)
        app.panelDp = screenDp
        val settings = app.prefs.now
        // `--ef slow 4` (debug): every spring and tween four times as long, to step through a recording.
        motion = Motion.of(this, if (BuildConfig.DEBUG) intent.getFloatExtra("slow", 1f) else 1f)
        // First run, in this order for every panel (core `FirstRun.opening`): the key, if it made this panel (a keyboard shortcut
        // starts the launcher activity; the assistant key asks for assistance); then this opening is counted for an unfinished
        // run, wherever it stopped; only then is the model built, which asks what stands.
        val keyed = byKey(intent)
        // (Whether this key is news: a first key, or the other one a replay asked for. Then "Your key works" is greeted below.)
        val news = keyed && app.prefs.now.firstRun().let { FirstRun.keyLanded(it) != it }
        if (keyed) first(app) { FirstRun.keyLanded(it) }
        // A panel that carries another app's text, or that an example is about to be typed into, shows no screen of first
        // run: it is not one of the run's openings.
        val plain = app.example == null && handed(intent) == null
        val counted = app.prefs.now.firstRun()
        first(app) { FirstRun.opening(it, screenDp, plain) }
        // How this opening begins (core `FirstRun.overture`): asked once the opening is counted, with the same `plain`. The
        // system is asked for a screen reader only where the opening piece is this opening's but for animations and a
        // reader: on every other day nothing here is read from it.
        val run = app.prefs.now.firstRun()
        // (The welcome is a picture drawn from the left: where the system lays out from the right it is not played, and the
        // key's step stands as it does where animations are off.)
        val level = plain && resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_LTR
        val due = FirstRun.overture(run, motion = true, reader = false, plain = level, screenDp = screenDp) != FirstRun.Overture.NONE
        val reader = due && getSystemService(android.view.accessibility.AccessibilityManager::class.java)?.isTouchExplorationEnabled == true
        staged = due && FirstRun.overture(run, motion.on, reader, level, screenDp) != FirstRun.Overture.NONE
        // The very first opening of a new installation, the one its opening piece begins, runs at Slow whatever is set.
        val slowly = staged && FirstRun.slow(run, FirstRun.Overture.WELCOME)
        model = OverlayModel(app, lifecycleScope, limit = Metrics.maxRows(screenDp), screenDp = screenDp)
        // Said before anything asks the model what stands (the words to read, the window's first height).
        if (app.example != null) model.guided = true
        // An unfinished run that has stood in its three openings waits in the Booklight window from this one on: the bare
        // field says so, in this opening alone (core `FirstRun.later`).
        if (FirstRun.later(counted, run)) model.later = true
        // The key that made this panel is the one its step waited for: "Your key works" is set down, and the light runs its lap.
        if (news) model.keyOpened()
        // The piece begins with this panel: the welcome, then the show, then the key's step. Where it is this opening's and
        // is not played (animations off, a screen reader), the key's step stands at once, as on any day, and the welcome's
        // title greets as the field's placeholder.
        if (staged) model.begin() else if (due) model.greets = FirstRun.greets(run, motion.on, reader, plain, screenDp)
        if (staged) getSystemService(android.view.accessibility.AccessibilityManager::class.java)?.addTouchExplorationStateChangeListener(readerCame)
        // The system's own words for its dialog are read before the stage that quotes them comes: the key's. (Asked of what
        // is due, not of what stands: while the piece plays nothing stands, and the key's step is what it lands in.)
        if (model.due?.step == FirstRun.Step.KEY) SystemWords.load(this, app.scope)
        model.typeStep = motion::typeStep
        model.hold = motion::hold
        model.lasts = motion::held
        model.onSay = { say(it) }
        model.onTell = { window.decorView.announceForAccessibility(it) }
        stay = BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_STAY, false)
        solid = settings.glass == "solid"
        Look.use(GlassLevel.of(settings.glass))
        var opening = if (slowly) "slow" else settings.opening
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
        // (First run's stage, where one is set down as this panel opens, begins its band when the glass is at rest.)
        model.opening(arrival.bandAfter)
        shade = Shade.of(if (BuildConfig.DEBUG) intent.getStringExtra("shade") ?: settings.shadow else settings.shadow)
        take(intent)
        // First run's choices are a list, not a stage: set down here, once it is known that the panel carries no text.
        model.offerChoices()
        placeWindow()

        val content = ComposeView(this).apply { setContent {
            BooklightTheme(dark, tint = settings.tint) {
                CompositionLocalProvider(LocalMotion provides motion) {
                    BackHandler { close() }
                    Panel(
                        model, icons, glass && !solid, dark, arrival, leaving,
                        onHeight = ::sizeWindow, onPresence = ::present, onDesk = ::desk, frame = frame,
                        onRun = ::run, onStage = ::stage, onClose = ::close,
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

    /**
     * A change of first run's state, worked out inside the settings' own update (`Prefs.firstRun`), and written only where
     * it changes something: on every day but the first few this costs no write.
     */
    private fun first(app: BooklightApp, change: (FirstRun.State) -> FirstRun.State) {
        val was = app.prefs.now.firstRun()
        if (change(was) != was) app.prefs.firstRun(change)
    }

    private fun placeWindow() {
        window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        // Optional: a soft dim separates the panel from busy windows. Off unless the user turns it on.
        // (A panel that begins with first run's opening piece dims the desk for the piece, whatever is set: `desk`.)
        if (dim > 0f || staged) {
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
        arrived = dimmed
        if (dim > 0f || staged) window.setDimAmount(maxOf(dim, desk) * dimmed)
    }

    /** How far the desk behind is dimmed for first run's opening piece, 0 to 1; and how far the panel has arrived, with which every dim comes and goes. */
    private var desk = 0f
    private var arrived = 0f

    /**
     * First run's opening piece asks for the desk behind the panel to dim: by half in the welcome's night, as "Dim the
     * desktop" would through the show, not at all once the key's step has landed. Never less than the setting asks for.
     */
    private fun desk(amount: Float) {
        if (!staged || amount == desk) return
        desk = amount
        window.setDimAmount(maxOf(dim, desk) * arrived)
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
    fun turn(intent: Intent) { if (leaving && !ran && !isFinishing) { leaveJob?.cancel(); leaving = false; model.turned(); take(intent) } }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_STAY, false)) { stay = true; return }
        lastStart = said(intent)
        // (Asked now: inside this call the system says who sent this start; afterwards it says who sent the first one again.)
        val key = byKey(intent)
        if (fromIcon(intent)) { startActivity(Intent(this, MainActivity::class.java)); close(); return }
        // The user's key, held, repeats, and each repeat is a new start: for a moment of real time after the key has landed,
        // and after each of its repeats, none of them does anything (`Motion.KEY_SETTLES_MS`): the panel that says "Your
        // key works" is not put away by the key that is still down. (Asked first: nor does a repeat land a second time,
        // should the screen wait for a key again by then.)
        if (key && model.keyAgain()) return
        // The user's key while first run waits for it, under the system's dialog or in view after it: it has landed, and the
        // panel stays. (Pressed while the panel was on its way out, it turns round first.)
        if (key && model.awaitsKey) { if (leaving) turn(intent); landed(); return }
        // A key nobody knew of, pressed with the panel open: it is known now. It puts the panel away, as every day.
        if (key) model.keyLanded()
        // Under the dialog nothing else puts the panel away.
        if (hold != FirstRun.Hold.NONE) return
        if (leaving) { turn(intent); return }
        (application as BooklightApp).let { app -> app.example?.let { app.example = null; model.typeOut(it); return } }
        if (!take(intent)) close()
    }

    /**
     * This start as the system describes it (core `FirstRun.Start`): whether the launcher activity was asked for, who the
     * system says sent it, whether it says where on screen an icon was, whether it carries anything. Inside `onNewIntent`
     * the sender is that start's; outside it, the first start's. Worked out once for each start: who the home app is, is a
     * question to the system, and every opening passes here.
     */
    private fun start(intent: Intent): FirstRun.Start {
        described?.takeIf { it.first === intent }?.let { return it.second }
        val launcher = intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        // (Asked only where the start says nothing of an icon's place, as before.)
        val home = if (!launcher || intent.sourceBounds != null) null else packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        return FirstRun.Start(launcher, intent.action == Intent.ACTION_ASSIST, referrer?.host, intent.sourceBounds != null, intent.extras?.isEmpty == false, home).also { described = intent to it }
    }

    /**
     * Was this start a click on the app's icon? Then the Booklight window is wanted, not the panel. The
     * launcher, the taskbar and the Apps list all belong to the home app, and they say where on screen the
     * icon was (source bounds). A keyboard shortcut, the assistant key, the widget, the tile and adb do neither.
     */
    private fun fromIcon(intent: Intent): Boolean = FirstRun.fromIcon(start(intent))

    /**
     * Was this start a keyboard shortcut or the assistant key? A shortcut's start of the launcher activity comes from the
     * system itself and carries nothing (docs/research/first-run-proof.md, check 6); "Open" in the store, in the installer
     * and in Settings' App info, and another app, say who they are.
     */
    private fun byKey(intent: Intent): Boolean = (BuildConfig.DEBUG && intent.getBooleanExtra("key", false)) || FirstRun.byKey(start(intent))

    /** A start in a few words, for the debug hooks: what it asks for and who the system says sent it. Nothing of what it carries. */
    private fun said(intent: Intent) = "${intent.action?.substringAfterLast('.')} referrer=${referrer?.host} bounds=${intent.sourceBounds != null} extras=${intent.extras?.isEmpty == false} key=${byKey(intent)} icon=${fromIcon(intent)}"

    /** The icon was clicked: open the Booklight window and leave without ever showing the panel. */
    private fun handOver() {
        handedOver = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
    }

    /** The text another app handed over with this start (its selection menu, its share sheet); null: none. */
    private fun handed(intent: Intent): String? = when (intent.action) {
        Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        else -> null
    }?.toString()?.takeIf { it.isNotBlank() }

    /** Text another app handed over becomes the chip. True if there was any. */
    private fun take(intent: Intent): Boolean {
        val text = handed(intent) ?: return false
        model.carries = true
        // A selection in a field that can be edited comes with the offer to take text back.
        // (Not for several paragraphs: a prompt is given the text on one line, and its answer would come back as one.)
        val editable = intent.action == Intent.ACTION_PROCESS_TEXT && !intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false) && '\n' !in text.trim()
        model.enterScope((application as BooklightApp).scopes.receive(text.take(MAX_TEXT), editable))
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (handedOver) return
        // The focus is back: the system's dialog has gone (closed by hand, or by the key that landed), and the hold is over.
        if (hasFocus) { model.focused = true; signal(FirstRun.Signal.FOCUS_BACK); return }
        // Behind the system's Keyboard shortcuts dialog the panel stays (first run's hold): the first focus lost after asking
        // for the dialog is the dialog. Without a hold a lost focus closes the panel, as every day. (A panel told to stay is
        // told of the dialog all the same, and then stays: a test sees the hold as a user's panel has it.)
        if (!signal(FirstRun.Signal.FOCUS_LOST) || stay) return
        // Whatever started the panel (the taskbar, a widget, the Apps list) may still be closing and
        // take focus for a moment: early on, only close if focus is still gone a little later.
        if (SystemClock.uptimeMillis() - created < EARLY_MS) window.decorView.postDelayed({ if (!hasWindowFocus() && !isFinishing && hold == FirstRun.Hold.NONE) close() }, 250)
        else close()
    }

    /** The wait for the system's dialog, one at a time: asked for and not there a second later, the hold is over. */
    private val noDialog = Runnable { signal(FirstRun.Signal.NO_DIALOG) }

    /** The hold's one place: what a signal makes of it (core `FirstRun.hold`). True: the panel is to go. */
    private fun signal(s: FirstRun.Signal): Boolean {
        val after = FirstRun.hold(hold, s)
        hold = after.hold
        // (A screen reader is with the dialog while it is over the panel: what first run has to say waits until it has gone.)
        model.under = hold == FirstRun.Hold.UNDER
        // (Under the dialog the stage turns to "Now press your keys" a moment after the panel lost the focus to it: the dialog
        // covers the panel by then, and nothing is seen to change beside it. The wait is there to be kept, also with animations
        // off. It is that hold's own: a dialog that has gone again meanwhile, by whichever way, leaves the stage as it was,
        // and nothing turns it late, in view.)
        if (hold != FirstRun.Hold.UNDER) { turning?.cancel(); turning = null }
        if (after.came) turning = lifecycleScope.launch { delay(motion.hold(Motion.HELPER_AFTER_MS)); if (hold == FirstRun.Hold.UNDER) model.helperCame() }
        return after.close
    }
    /** The wait before the stage turns under the system's dialog ([signal]). */
    private var turning: Job? = null

    /**
     * While the panel is held, the system's Keyboard shortcuts dialog opens on a page of the panel's own: the five steps
     * to a key, with the suggested keys at each row's end (docs/design/first-run/design.md §5).
     */
    override fun onProvideKeyboardShortcuts(data: MutableList<KeyboardShortcutGroup>, menu: Menu?, deviceId: Int) {
        super.onProvideKeyboardShortcuts(data, menu, deviceId)
        if (hold != FirstRun.Hold.NONE) data.add(SystemWords.rows(this, model.suggested))
    }

    /**
     * Under the dialog the panel is told nothing but that it lost the focus. If another window then takes the front for
     * good (a click beside the dialog, another app's shortcut), nothing would ever end the hold: this does. The key's own
     * start also takes the front, for a few milliseconds: hence the wait.
     */
    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        topResumed = isTopResumedActivity
        if (!isTopResumedActivity && hold != FirstRun.Hold.NONE) window.decorView.postDelayed({ if (!topResumed && !isFinishing && signal(FirstRun.Signal.GONE)) close() }, GONE_MS)
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
        // (Nor while first run's opening piece plays: it performs, and nothing of it is run.)
        if (leaving || settled || model.playing != null) return   // a second Enter or click while the panel is on its way out
        val app = application as BooklightApp
        // "First steps" is the panel's own (core `FirstRun.AGAIN`): nothing is run and the panel stays; the run stands under
        // the emptied field. Caught here, where everything that runs passes (Enter, a click, Ctrl + digit).
        if (a.effect == FirstRun.AGAIN) {
            model.used(r, a)
            model.again()
            // (The system's own words for its dialog are read before the key's step quotes them, as at an opening.)
            if (model.due?.step == FirstRun.Step.KEY) SystemWords.load(this, app.scope)
            return
        }
        // First run: while a lesson stands nothing opens (core `FirstRun.enters`). An app's Open and a search inside an app are
        // practice: nothing is started, the footer says what Enter would have done, and the lesson is over. Asked here, where
        // everything that runs passes (Enter, a click, Ctrl + digit, an Enter that waited for its list or its answer).
        val lesson = model.enters(r, a)
        if (lesson == FirstRun.Enter.PRACTICE_OPEN || lesson == FirstRun.Enter.PRACTICE_SEARCH) {
            model.used(r, a)
            // (Where no app can be searched a settings page stands in for the search: Enter would open it.)
            val searches = lesson == FirstRun.Enter.PRACTICE_SEARCH && a.effect !is Effect.OpenSettings
            say(getString(if (searches) R.string.first_practice_search else R.string.first_practice_open, model.named(r)))
            lessonOver(lesson, r, a)
            return
        }
        // A pinned text exists nowhere else: when another pin takes its place, the footer says which one went.
        fun pins(e: Effect): Boolean = e is Effect.Pin || (e is Effect.Steps && e.steps.any(::pins))
        val replaced = app.pinned.value?.takeIf { it.kind == "text" && pins(a.effect) }?.title(this)?.let { getString(R.string.pin_replaced, if (it.length > 24) it.take(23) + "…" else it) }
        // The row stays, and the footer says why. A link's address, or what another app was asked for, found nobody to open it
        // (an app's own address is no browser's business).
        if (!app.executor.run(a.effect, this)) { say(getString(if (a.effect is Effect.OpenUrl || a.effect is Effect.Open) R.string.failed_open else R.string.failed), bad = true); return }
        // Asking for a grant runs nothing yet: what was typed (the first note) is kept, in case the picker is cancelled.
        // (Nor does a lesson's sum: it was copied, and the panel stays for what is typed next.)
        ran = a.effect !is Effect.Grant && lesson != FirstRun.Enter.COPY_STAYS
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
            // First run: a lesson's sum is copied for real, the footer says so, and the panel stays.
            lesson == FirstRun.Enter.COPY_STAYS -> lessonOver(lesson, r, a)
            a.keepOpen || keep -> model.refresh()
            // Long enough to read the word, or to see a level arrive, then away.
            word != null -> { settled = true; lifecycleScope.launch { delay(520); close() } }
            r.nudge != null -> { settled = true; model.refresh(); lifecycleScope.launch { delay(400); close() } }
            else -> close()
        }
    }

    /**
     * A lesson's Enter has had its answer (practice, or its sum copied). The lesson is over at once; its list stands for as
     * long as the panel would on any other day take to go, the slot pressed, and then gives way to what stands next. Until
     * then Enter does nothing more.
     */
    private fun lessonOver(enter: FirstRun.Enter, r: Result, a: Action) {
        settled = true
        model.ran(enter, r, a)
        // (Also on a panel that is leaving: a leave can be turned round by the key, and the list must not stand on then.)
        lifecycleScope.launch { delay(motion.hold(LESSON_MS)); settled = false; model.giveWay() }
    }

    /** A word in the footer for a moment. */
    private fun say(word: String, bad: Boolean = false) {
        model.flash = word; model.flashBad = bad
        flashJob?.cancel()
        flashJob = lifecycleScope.launch { delay(1600); model.flash = null }
    }

    /**
     * An answer on first run's stage. "Open Keyboard shortcuts" is the window's to do: the system is asked for its dialog
     * and the panel holds on behind it, so that the dialog opens on the panel's own page and the panel is there, changed,
     * when the dialog goes. Every other answer only changes what is kept.
     */
    fun stage(answer: FirstRun.Answer) {
        if (leaving) return
        if (answer != FirstRun.Answer.OPEN_HELPER) { model.answerStage(answer); return }
        // Asked already, and the dialog is on its way or up: a second Enter would start a second wait, and the first one's end would cut it short.
        if (hold != FirstRun.Hold.NONE) return
        // (The slot shows as pressed in the frame the dialog is asked for; nothing that is kept changes by this answer.)
        model.answerStage(answer)
        // The key is suggested for the keyboard this Enter came from: the dialog's rows carry it.
        model.resuggest()
        signal(FirstRun.Signal.ASK)
        requestShowKeyboardShortcuts()
        // No dialog a second later (the system showed none): the hold is over, and a lost focus closes the panel as every day.
        // One wait: a dialog that came and went inside the second leaves none behind to end the next one early.
        window.decorView.removeCallbacks(noDialog)
        window.decorView.postDelayed(noDialog, NO_DIALOG_MS)
    }

    /**
     * The user's key has opened the panel while its step waited for it: "Your key works". The system closes its dialog by
     * itself when the new key is pressed; it is asked to as well, for a build that does not.
     */
    private fun landed() {
        // (Under the dialog "your key" is held down until the dialog has gone; in view, for a tap's length.)
        model.keyLanded(under = hold == FirstRun.Hold.UNDER)
        dismissKeyboardShortcutsHelper()
        signal(FirstRun.Signal.KEY)
    }

    /** Leaves: the panel folds away the way it came (or fades, with the opening turned off), its blur with it, then the activity finishes. */
    fun close() {
        if (leaving || handedOver) return
        if (!ran) model.keep()
        model.closing()
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
        if (!stay) { if (!ran && !leaving) model.keep(); model.closing(); finishNow() }
    }

    override fun onDestroy() {
        if (staged) getSystemService(android.view.accessibility.AccessibilityManager::class.java)?.removeTouchExplorationStateChangeListener(readerCame)
        if (current.get() === this) {
            current = WeakReference(null)
            (application as BooklightApp).let { it.panelDp = null; it.scopes.forget(); it.onDevice.close() }
        }
        super.onDestroy()
    }

    companion object {
        /** Actions that do not count as running the thing: App info and the app's other pages in Settings, Edit, Delete (and whatever is marked as removing). */
        private val NOT_A_RUN = setOf("info", "edit", "delete") + io.github.kuscher.booklight.providers.AppPages.ids
        const val EXTRA_STAY = "stay"
        const val EXTRA_DARK = "dark"
        /** How long the system has to show its Keyboard shortcuts dialog before the hold gives up waiting for it. */
        private const val NO_DIALOG_MS = 1000L
        /** How long another window must have had the front before a held panel goes: the key's own start has it for a few ms. */
        private const val GONE_MS = 300L
        /** The last start of the panel, in a few words ([said]): for the debug hooks. */
        var lastStart = ""
        private const val TYPE_AFTER_MS = 160L
        /** How long a lesson's list stands after its Enter before it gives way: as long as the panel takes to go once a word was said. */
        private const val LESSON_MS = 520L
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
