package io.github.kuscher.booklight.window

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.overlay.LocalDark
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.Metrics
import io.github.kuscher.booklight.overlay.OverlayModel
import io.github.kuscher.booklight.overlay.Footer
import io.github.kuscher.booklight.overlay.ResultsBody
import io.github.kuscher.booklight.overlay.THIRD
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.Fonts
import kotlinx.coroutines.delay

/**
 * What Booklight is, shown rather than told: the panel's own rows on a small drawn desk, typing a
 * few letters, moving the selection, gliding along a row's actions, over and over while the window
 * is in front. It is the real list with the real apps of this device; nothing in it can be run.
 * With the system's animations off it types once and stands still.
 */
@Composable
fun Stage(app: BooklightApp) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val scope = rememberCoroutineScope()
    val model = remember { OverlayModel(app, scope, limit = 3, demo = true) }
    val icons = remember { app.icons ?: AppIcons(app).also { app.icons = it } }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                model.type(""); delay(900)
                for (text in listOf("c", "ch")) { model.type(text); delay(260) }
                if (!motion.on) return@repeatOnLifecycle
                delay(1200); model.move(1); delay(800); model.move(-1); delay(700)
                repeat(3) { model.arm(1, wrap = true); delay(750) }
                delay(600)
            }
        }
    }
    val shape = RoundedCornerShape(32.dp)
    Box(Modifier.fillMaxWidth().height(300.dp).clip(shape).background(scheme.surfaceContainerLowest).clearAndSetSemantics {}) {
        // A flat desk: two windows with a few lines of text.
        Sheet(Modifier.align(Alignment.TopStart).padding(start = 36.dp, top = 44.dp).size(300.dp, 320.dp), scheme.primaryContainer, scheme.onSurface)
        Sheet(Modifier.align(Alignment.TopEnd).padding(end = 40.dp, top = 84.dp).size(340.dp, 300.dp), scheme.tertiaryContainer, scheme.onSurface)
        // The panel at three quarters of its size: its field, then the real list.
        val height by animateDpAsState(Metrics.height(model), motion.place(), label = "stage")
        Box(Modifier.fillMaxSize().padding(top = 36.dp), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier.graphicsLayer { scaleX = 0.75f; scaleY = 0.75f; transformOrigin = TransformOrigin(0.5f, 0f) }
                    .wrapContentSize(unbounded = true).requiredWidth(Metrics.width).height(height)
                    .clip(shape).background(scheme.surfaceContainerHigh)
                    .border(with(LocalDensity.current) { 1.25.dp }, Color.White.copy(alpha = if (dark) 0.44f else 0.80f), shape),
            ) {
                Column(Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).requiredWidth(Metrics.width)) {
                    Row(Modifier.fillMaxWidth().height(Metrics.field).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            Text("G", color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.round, fontSize = 24.sp, fontWeight = FontWeight(600)))
                        }
                        val style = TextStyle(fontFamily = Fonts.text, fontSize = 24.sp, fontWeight = FontWeight(500))
                        Text(model.query, color = scheme.onSurface, style = style, modifier = Modifier.padding(start = 16.dp))
                        Box(Modifier.padding(start = 1.dp).size(2.dp, 28.dp).background(scheme.primary))
                        model.completion?.let { Text(it, color = scheme.onSurface.copy(alpha = THIRD), style = style) }
                    }
                    if (model.results.isNotEmpty()) { ResultsBody(model, icons) { _, _ -> }; Footer(model) }
                }
            }
        }
    }
}

/** A window on the stage's desk: a title bar and bars where text would be. */
@Composable
private fun Sheet(modifier: Modifier, tint: Color, ink: Color) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(tint.copy(alpha = 0.55f)).padding(20.dp)) {
        for ((i, w) in listOf(120, 220, 180, 240, 150, 200).withIndex()) {
            Box(Modifier.padding(bottom = 12.dp).width(w.dp).height(if (i == 0) 14.dp else 8.dp).clip(RoundedCornerShape(4.dp)).background(ink.copy(alpha = 0.12f)))
        }
    }
}
