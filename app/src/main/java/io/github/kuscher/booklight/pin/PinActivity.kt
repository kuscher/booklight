package io.github.kuscher.booklight.pin

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.app.PictureInPictureUiState
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Colors
import io.github.kuscher.booklight.core.ImageUse
import io.github.kuscher.booklight.device.QrImages
import io.github.kuscher.booklight.overlay.DrawnCheck
import io.github.kuscher.booklight.overlay.LocalDark
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.Motion
import io.github.kuscher.booklight.overlay.QrPlate
import io.github.kuscher.booklight.overlay.SECOND
import io.github.kuscher.booklight.overlay.SMALL
import io.github.kuscher.booklight.overlay.Swatch
import io.github.kuscher.booklight.overlay.fadeEnd
import io.github.kuscher.booklight.ui.BooklightTheme
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.isDark
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The pinned window: a small window that stays above the others and shows one thing, a line of
 * text, a sum's answer, a colour, a QR code or a countdown. One at a time.
 *
 * It is a picture-in-picture window. That is the one kind of window the system keeps on top
 * without ever giving it the keyboard: the keys stay with the app the user is in, when the pin
 * opens and every time the panel closes after it. (Android 17's pinned layer keeps a window on top
 * too, but such a window takes the keys each time the window in front of it closes, and an app has
 * no way to hand them back: docs/research/device-findings.md.) The system draws the window, moves
 * it, and closes it with its own cross. It is for looking at: Copy and Unpin are in the panel, on
 * the row `pin` shows for it. (The system shows a button of the app's own only on a window some
 * 220 dp high, which a pinned line is not.)
 *
 * Opened out of picture-in-picture by the user it is an ordinary small window with the same
 * content: a click or Enter copies, Esc closes.
 */
class PinActivity : ComponentActivity() {
    private var pinned by mutableStateOf<Pinned?>(null)
    /** The size this window was opened for, in dp: a pin of another shape gets a window of its own (the system does not reshape one). */
    var size: Pair<Int, Int> = 0 to 0; private set
    private var pip by mutableStateOf(false)
    private var landed = false
    /** When something was last copied from here, for the word that says so. */
    private var copied by mutableLongStateOf(0L)
    private var entered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val p = Pinned.from(intent.extras) ?: run { finish(); return }
        current = WeakReference(this)
        pip = isInPictureInPictureMode
        // The window is an ordinary one for a moment before the system takes it on top, and the system shows that
        // step its own way (the window goes, a card with the app's mark lands). Nothing of ours is shown until it has
        // landed; then the content comes, once, as a fade. One arrival, not two.
        if (savedInstanceState == null && !pip) reveal(0f)
        size = p.size(this)
        show(p)
        val app = application as BooklightApp
        val motion = Motion.of(this)
        setContent {
            val s by app.prefs.state.collectAsState()
            val dark = isDark(s.theme)
            BooklightTheme(dark, tint = s.tint) {
                CompositionLocalProvider(LocalMotion provides motion, LocalDark provides dark) {
                    pinned?.let { PinWindow(it, pip, copied, onCopy = ::copy, onClose = ::finishAndRemoveTask) }
                }
            }
        }
    }

    /** What the window shows: at first, and when a new pin of the same shape takes the window of the one before. */
    fun show(p: Pinned) {
        pinned = p
        intent.replaceExtras(p.bundle())      // what the window shows if the system makes it again
        (application as BooklightApp).pinned.value = p
        setTaskDescription(ActivityManager.TaskDescription.Builder().setLabel(p.label(this)).build())
        // A window the user had opened out goes back on top.
        if (entered && !isInPictureInPictureMode) enter()
    }

    override fun onResume() {
        super.onResume()
        if (!entered) enter()
    }

    /** Goes on top. */
    private fun enter() {
        val p = pinned ?: return
        entered = true
        // Refused (the user turned picture-in-picture off for Booklight): it stays an ordinary small window.
        val ok = runCatching { enterPictureInPictureMode(params(p)) }.getOrDefault(false)
        if (!ok) { Log.i(BooklightApp.TAG, "pin: not on top (picture-in-picture is off for Booklight)"); landed = true; reveal(1f) }
        // Shown once it has landed; if the system never says that it has, shown anyway.
        else window.decorView.postDelayed({ arrive() }, 1200)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pip = isInPictureInPictureMode
        if (isInPictureInPictureMode) landed = true else reveal(1f)      // opened out by the user: an ordinary window, there at once
    }

    /** The system says when the window has landed on top: then its content is shown. */
    override fun onPictureInPictureUiStateChanged(state: PictureInPictureUiState) {
        super.onPictureInPictureUiStateChanged(state)
        if (isInPictureInPictureMode && (Build.VERSION.SDK_INT < 35 || !state.isTransitioningToPip)) arrive()
    }

    private var arrived = false
    private fun arrive() {
        if (arrived || isFinishing) return
        arrived = true
        if (!Motion.of(this).on) { reveal(1f); return }
        ValueAnimator.ofFloat(0f, 1f).apply { duration = 140; addUpdateListener { reveal(it.animatedValue as Float) }; start() }
    }

    private fun reveal(alpha: Float) {
        val lp = window.attributes
        if (lp.alpha == alpha) return
        lp.alpha = alpha
        window.attributes = lp
    }

    private fun params(p: Pinned): PictureInPictureParams {
        val (w, h) = p.size(this)
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(w, h))
            .setSeamlessResizeEnabled(false)     // text, not video: it is laid out at its new size, not stretched to it
            .build()
    }

    /** Copies what is pinned: the text, the answer, the colour as it was typed; a QR code as its picture. */
    fun copy() {
        val p = pinned ?: return
        when (p.kind) {
            "timer" -> return
            "qr" -> QrImages.use(this, p.text, ImageUse.COPY)
            else -> getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Booklight", p.text))
        }
        copied = System.nanoTime()
    }

    override fun onDestroy() {
        if (current.get() === this) {
            current = WeakReference(null)
            if (isFinishing) (application as BooklightApp).pinned.value = null
        }
        super.onDestroy()
    }

    companion object {
        var current: WeakReference<PinActivity> = WeakReference(null)
    }
}

private val TITLE = TextStyle(fontFamily = Fonts.text, fontSize = 17.sp, fontWeight = FontWeight(500))
private val FIGURE = TextStyle(fontFamily = Fonts.round, fontSize = 34.sp, fontWeight = FontWeight(600), fontFeatureSettings = "tnum")
private val WORD = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(600))

@Composable
private fun PinWindow(p: Pinned, pip: Boolean, copied: Long, onCopy: () -> Unit, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    // "Copied", for a moment. A second copy starts the moment again, not the word.
    var said by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied != 0L) { said = true; delay(motion.hold(1600)); said = false } }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BoxWithConstraints(
        (if (pip) Modifier.fillMaxSize().background(scheme.surfaceContainerHigh) else Modifier.fillMaxSize().background(scheme.surfaceContainerHigh).safeDrawingPadding())
            .focusRequester(focus).focusable()
            // Only as an ordinary window does it get keys and clicks: on top, the system keeps them from it.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !pip, onClick = onCopy)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown || pip) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.Escape -> { onClose(); true }
                    Key.Enter, Key.NumPadEnter -> { if (e.nativeKeyEvent.repeatCount == 0) onCopy(); true }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Drawn at its own size and made larger with the window, as one piece: a pin the user drags larger is the same pin, larger.
        AnimatedContent(p, transitionSpec = {
            (fadeIn(motion.fade(140)) + slideInVertically(motion.place()) { it / 8 }) togetherWith fadeOut(motion.fade(70))
        }, contentAlignment = Alignment.Center, label = "pin") { now ->
            val (w, h) = remember(now) { now.size(context) }
            val scale = minOf(maxWidth / w.dp, maxHeight / h.dp)
            Box(Modifier.requiredSize(w.dp, h.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
                when (now.kind) {
                    "timer" -> Countdown(now)
                    "answer" -> Column(Modifier.fillMaxSize().padding(horizontal = Pinned.MARGIN.dp), verticalArrangement = Arrangement.Center) {
                        Text(now.note, color = scheme.onSurface.copy(alpha = SECOND), style = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(20.dp))
                        Spacer(Modifier.height(2.dp))
                        Box(Modifier.height(42.dp).fillMaxWidth().fadeEnd(), contentAlignment = Alignment.CenterStart) {
                            Text(now.text, color = scheme.onSurface, style = if (now.text.length > 12) FIGURE.copy(fontSize = 26.sp) else FIGURE, maxLines = 1, softWrap = false)
                        }
                    }
                    "color" -> Row(Modifier.fillMaxSize().padding(horizontal = Pinned.MARGIN.dp), verticalAlignment = Alignment.CenterVertically) {
                        Swatch(now.value.toInt(), scheme.onSurface, size = 52.dp, radius = 16.dp)
                        // The colour as it was pinned, then its other ways of being written.
                        val c = remember(now) { Colors.parse(now.text) }
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(now.text, color = scheme.onSurface, style = FIGURE.copy(fontSize = 26.sp, fontFeatureSettings = "tnum, zero"), maxLines = 1)
                            if (c != null) for (form in listOf(c.rgb(), c.hsl(), c.oklch())) Text(form, color = scheme.onSurface.copy(alpha = SECOND), style = SMALL.copy(fontFeatureSettings = "tnum, zero"), maxLines = 1, modifier = Modifier.height(20.dp))
                        }
                    }
                    "qr" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { QrPlate(now.text) }
                    else -> Box(Modifier.fillMaxSize().padding(horizontal = Pinned.MARGIN.dp), contentAlignment = Alignment.CenterStart) {
                        // Six lines at most; Copy still copies all of it.
                        Text(now.text, color = scheme.onSurface, style = TITLE.copy(lineHeight = with(LocalDensity.current) { Pinned.LINE.dp.toSp() }), maxLines = Pinned.MAX_LINES, overflow = TextOverflow.Ellipsis)
                    }
                }
                // "Copied", where the eye is not reading: the upper right corner, on a small plate of the window's own ground.
                AnimatedVisibility(said, Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 10.dp), enter = fadeIn(motion.fade(120)), exit = fadeOut(motion.fade(80))) {
                    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(scheme.surfaceContainerHigh).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        DrawnCheck(said, scheme.onSurface, Modifier.size(16.dp))
                        Text(stringResource(R.string.copied), color = scheme.onSurface, style = WORD, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * A countdown: what it is for and when it was set to end, the time left, and a line that shortens
 * with it. A digit that changes comes down into its slot while the old one leaves; the others stand
 * still. The shape of the time is chosen once, from how long the timer runs, so nothing shifts when
 * a place reaches zero. It stops at 0:00 and swells, once. One wake a second, and none while the
 * window is not on screen. The Clock app rings; this only shows. "Set for", not "rings at": a timer
 * cancelled in the Clock still counts here.
 */
@Composable
private fun Countdown(p: Pinned) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    fun left() = ((p.until - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0)
    var seconds by remember(p) { mutableLongStateOf(left()) }
    val swell = remember(p) { Animatable(1f) }
    LaunchedEffect(p) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val before = seconds
                seconds = left()
                if (seconds == 0L) {
                    if (before > 0L) { swell.animateTo(1.06f, motion.pop()); swell.animateTo(1f, motion.pop()) }
                    break
                }
                // To the next whole second.
                delay(((p.until - System.currentTimeMillis()) % 1000).let { if (it <= 0) 1000 else it })
            }
        }
    }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val at = remember(p, locale) {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        Instant.ofEpochMilli(p.until).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern, locale))
    }
    val set = stringResource(R.string.pin_set_for, at)
    val text = when {
        p.value >= 36000 -> "%02d:%02d:%02d".format(Locale.ROOT, seconds / 3600, seconds % 3600 / 60, seconds % 60)
        p.value >= 3600 -> "%d:%02d:%02d".format(Locale.ROOT, seconds / 3600, seconds % 3600 / 60, seconds % 60)
        p.value >= 600 -> "%02d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
        else -> "%d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
    }
    // Every digit has a slot as wide as the widest digit, measured once.
    val measurer = rememberTextMeasurer()
    val slot = with(LocalDensity.current) { remember(measurer) { measurer.measure("0", FIGURE).size.width }.toDp() }
    val drop = with(LocalDensity.current) { 8.dp.roundToPx() }
    val share by animateFloatAsState(if (p.value <= 0) 0f else (seconds.toFloat() / p.value).coerceIn(0f, 1f), motion.tick(), label = "left")
    val ink = scheme.onSurface
    Column(Modifier.fillMaxSize().padding(horizontal = Pinned.MARGIN.dp), verticalArrangement = Arrangement.Center) {
        Text(if (p.note.isEmpty()) set.replaceFirstChar { it.titlecase() } else "${p.note} · $set", color = ink.copy(alpha = SECOND), style = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(20.dp))
        Spacer(Modifier.height(2.dp))
        Row(Modifier.height(42.dp).graphicsLayer { scaleX = swell.value; scaleY = swell.value; transformOrigin = TransformOrigin(0f, 0.5f) }, verticalAlignment = Alignment.CenterVertically) {
            text.forEachIndexed { i, c ->
                if (!c.isDigit()) Text(c.toString(), color = ink, style = FIGURE)
                else Box(Modifier.width(slot), contentAlignment = Alignment.Center) {
                    AnimatedContent(c, transitionSpec = {
                        (slideInVertically(motion.tick()) { -drop } + fadeIn(motion.fade(120))) togetherWith (slideOutVertically(motion.tick()) { drop } + fadeOut(motion.fade(70)))
                    }, label = "digit$i") { d -> Text(d.toString(), color = ink, style = FIGURE, maxLines = 1) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // How much is left, as a line: flat and quiet, the panel's own track.
        Box(Modifier.fillMaxWidth().height(3.dp).drawBehind {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(ink.copy(alpha = 0.16f), cornerRadius = r)
            if (share > 0f) drawRoundRect(ink.copy(alpha = SECOND), size = Size(size.width * share, size.height), cornerRadius = r)
        })
    }
}
