package io.github.kuscher.booklight.window

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.data.Settings
import io.github.kuscher.booklight.overlay.Footer
import io.github.kuscher.booklight.overlay.GlassLevel
import io.github.kuscher.booklight.overlay.LocalDark
import io.github.kuscher.booklight.overlay.LocalGlass
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.Look
import io.github.kuscher.booklight.overlay.Metrics
import io.github.kuscher.booklight.overlay.OverlayModel
import io.github.kuscher.booklight.overlay.ResultsBody
import io.github.kuscher.booklight.overlay.Shade
import io.github.kuscher.booklight.overlay.THIRD
import io.github.kuscher.booklight.overlay.glass
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * The panels the window's stages show: one for each thing shown (the demo, the preview of the look, a command's
 * example), kept while the window lives. A stage that changes its place (from the page's column to the second
 * pane when the window is widened, and back) so goes on with the panel as it was: nothing is typed a second
 * time, and nothing grows again from an empty field.
 */
class Stages(private val app: BooklightApp, private val scope: CoroutineScope) {
    private val models = HashMap<String, OverlayModel>()
    private val typed = HashMap<String, String>()
    /** How many stages show the demo now: its loop runs only while one does. */
    var demos by mutableIntStateOf(0)

    fun of(slot: String): OverlayModel = models.getOrPut(slot) { OverlayModel(app, scope, limit = 3, demo = true) }

    /** Types [text] into a slot's panel, once: a second stage for the same text finds it typed. */
    fun type(slot: String, text: String) {
        if (typed[slot] == text) return
        typed[slot] = text
        val model = of(slot)
        while (model.leaveScope()) { }
        model.type(text)
    }

    /**
     * The demo: a few letters typed, the selection moved, a glide along a row's actions, over and over while a stage
     * shows it and the window is in front. With the system's animations off it types once and stands still.
     */
    @Composable
    fun Demo() {
        val motion = LocalMotion.current
        val owner = LocalLifecycleOwner.current
        LaunchedEffect(this) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                snapshotFlow { demos > 0 }.collectLatest { shown ->
                    if (!shown) return@collectLatest
                    val model = of(DEMO)
                    while (true) {
                        model.type(""); delay(900)
                        for (t in listOf("c", "ch")) { model.type(t); delay(260) }
                        if (!motion.on) return@collectLatest
                        delay(1200); model.move(1); delay(800); model.move(-1); delay(700)
                        repeat(3) { model.arm(1, wrap = true); delay(750) }
                        delay(600)
                    }
                }
            }
        }
    }

    companion object { const val DEMO = "demo" }
}

/** The window's stages; `MainActivity` provides them. */
val LocalStages = staticCompositionLocalOf<Stages> { error("no stages: a Stage stands in the Booklight window") }

/**
 * The panel, shown rather than told: its own rows on a small drawn desk, in the look the user has chosen. It
 * is the real list with the real apps of this device; nothing in it can be run.
 *
 * Without a [text] it is the demo ([Stages.Demo]). With a [text] that text is typed and it stands still: the
 * preview of the look, or a command's example.
 *
 * The glass is the panel's own veil and outline (`overlay/Glass.kt`) over the desk blurred where the panel
 * stands: inside an ordinary window there is no window blur to use, so the desk is drawn a second time,
 * blurred by the level's radius and cut to the panel's shape. The shadow is drawn (a soft dark shape under the
 * panel, as wide and as dark as the setting says), and with "Dim the desktop" the desk is darkened as the
 * real desktop is. The stage takes the width it is given; the panel is as large as fits it, at most three
 * quarters of its real size.
 */
@Composable
fun Stage(
    app: BooklightApp, s: Settings, height: Dp,
    /** How far below the stage's top the panel stands. */
    top: Dp = 28.dp,
    text: String? = null,
    /** Which of the window's panels it shows ([Stages]): stages with the same slot show the same one. */
    slot: String = if (text == null) Stages.DEMO else "preview",
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val density = LocalDensity.current
    val stages = LocalStages.current
    val model = stages.of(slot)
    val icons = remember { app.icons ?: AppIcons(app).also { app.icons = it } }
    if (text == null) DisposableEffect(stages) { stages.demos++; onDispose { stages.demos-- } }
    else LaunchedEffect(stages, slot, text) { stages.type(slot, text) }
    val solid = s.glass == "solid"
    val level = GlassLevel.of(s.glass)
    val shade = Shade.of(s.shadow)
    val dim by animateFloatAsState(if (s.dim) (if (dark) Look.dimDark else Look.dimLight) else 0f, motion.fade(160), label = "dim")
    val panel by animateDpAsState(Metrics.height(model), motion.place(), label = "stage")
    BoxWithConstraints(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(28.dp)).background(scheme.card).clearAndSetSemantics {}) {
        // As large as fits: 24 dp of desk on either side, and room under it for a field, three rows and the footer.
        val scale = minOf((maxWidth - 48.dp) / Metrics.width, (height - top - 16.dp) / (Metrics.field + Metrics.row * 3 + Metrics.pad * 2 + Metrics.footer), 0.75f)
        val wide = maxWidth
        /** The panel's outline on the stage, as it stands in this frame. */
        fun outline(scope: androidx.compose.ui.unit.Density): RoundRect = with(scope) {
            val w = Metrics.width.toPx() * scale
            val left = (wide.toPx() - w) / 2
            RoundRect(Rect(left, top.toPx(), left + w, top.toPx() + panel.toPx() * scale), CornerRadius(Metrics.radius.toPx() * scale))
        }
        Desk(dim)
        // The shadow: wider and darker with the setting, most of it under the panel's lower edge. Never under the glass:
        // what is drawn next covers the panel's own shape.
        if (shade != Shade.OFF) Box(Modifier.matchParentSize().drawBehind {
            val at = outline(this)
            val blur = shade.height.dp.toPx() * 0.4f * scale
            val paint = Paint().asFrameworkPaint().apply {
                color = Color.Black.copy(alpha = (shade.spot + shade.ambient) * (if (dark) 1.4f else 1f)).toArgb()
                maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
            }
            val drop = shade.height.dp.toPx() * 0.22f * scale
            drawIntoCanvas { it.nativeCanvas.drawRoundRect(at.left, at.top + drop, at.right, at.bottom + drop, at.topLeftCornerRadius.x, at.topLeftCornerRadius.y, paint) }
        })
        // The glass's blur: the desk again, blurred, where the panel is.
        if (!solid) Box(Modifier.matchParentSize().drawWithContent { clipPath(Path().apply { addRoundRect(outline(this@drawWithContent)) }) { this@drawWithContent.drawContent() } }.blur((level.blurDp * scale).dp)) {
            Box(Modifier.fillMaxSize().background(scheme.card)) { Desk(dim) }
        }
        // The panel itself: its veil and outline, its field, the real list.
        Box(Modifier.fillMaxSize().padding(top = top), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier.graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0.5f, 0f) }
                    // From the top: a panel taller than the room under it must not be centred in that room.
                    .wrapContentSize(Alignment.TopCenter, unbounded = true).requiredWidth(Metrics.width).height(panel)
                    .glass(
                        tint = if (solid) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest.copy(alpha = if (dark) level.tintDark else level.tintLight),
                        radiusPx = with(density) { Metrics.radius.toPx() }, density = density.density, run = { 0f }, glow = { 0f }, tail = { 36f }, dark = dark, solid = solid,
                    )
                    .clip(RoundedCornerShape(Metrics.radius)),
            ) {
                CompositionLocalProvider(LocalGlass provides !solid) {
                    Column(Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).requiredWidth(Metrics.width)) {
                        Row(Modifier.fillMaxWidth().height(Metrics.field).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            val chip = model.chip
                            if (chip == null) Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                Text("G", color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.round, fontSize = 24.sp, fontWeight = FontWeight(600)))
                            } else Row(
                                // A scope's chip, as the panel's field shows it.
                                Modifier.height(36.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = if (dark) 0.14f else 0.10f)).padding(start = 8.dp, end = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Symbols.of(chip.symbol), null, Modifier.size(20.dp), tint = scheme.onSurface)
                                Text(chip.name, color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.text, fontSize = 15.sp, fontWeight = FontWeight(600)), maxLines = 1, modifier = Modifier.padding(start = 6.dp))
                            }
                            val style = TextStyle(fontFamily = Fonts.text, fontSize = 24.sp, fontWeight = FontWeight(500))
                            Text(model.query, color = scheme.onSurface, style = style, maxLines = 1, modifier = Modifier.padding(start = if (chip == null) 16.dp else 12.dp))
                            Box(Modifier.padding(start = 1.dp).size(2.dp, 28.dp).background(scheme.primary))
                            model.completion?.let { Text(it, color = scheme.onSurface.copy(alpha = THIRD), style = style, maxLines = 1) }
                        }
                        if (model.results.isNotEmpty()) { ResultsBody(model, icons) { _, _ -> }; Footer(model) }
                    }
                }
            }
        }
    }
}

/**
 * A flat desk: two windows with a few lines of text, one over the other's edge, darkened by [dim] as the desktop is.
 * They are placed by the stage's own size and run off its edges, so that at no width an edge of theirs comes to lie
 * beside an edge of the panel.
 */
@Composable
private fun Desk(dim: Float) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val off = maxWidth * 0.06f
        Sheet(Modifier.offset(x = -off, y = maxHeight * 0.14f).size(maxWidth * 0.50f, 400.dp), off + 12.dp, scheme.primaryContainer.copy(alpha = 0.55f).compositeOver(scheme.card), scheme.onSurface, listOf(0.40f, 0.74f, 0.60f, 0.80f, 0.50f, 0.66f, 0.72f, 0.44f))
        Sheet(Modifier.offset(x = maxWidth * 0.41f, y = maxHeight * 0.26f).size(maxWidth * 0.65f, 400.dp), 20.dp, scheme.tertiaryContainer.copy(alpha = 0.55f).compositeOver(scheme.card), scheme.onSurface, listOf(0.36f, 0.70f, 0.82f, 0.56f, 0.64f, 0.78f, 0.46f, 0.68f))
        if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
    }
}

/** A window on the stage's desk: bars where text would be, starting [lead] in from its edge. */
@Composable
private fun Sheet(modifier: Modifier, lead: Dp, tint: Color, ink: Color, lines: List<Float>) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(18.dp)).background(tint).padding(start = lead, top = 20.dp, end = 20.dp, bottom = 20.dp)) {
        val wide = maxWidth * 0.8f
        Column {
            for ((i, w) in lines.withIndex()) {
                Box(Modifier.padding(bottom = 12.dp).width(wide * w).height(if (i == 0) 12.dp else 8.dp).clip(RoundedCornerShape(4.dp)).background(ink.copy(alpha = 0.12f)))
            }
        }
    }
}
