package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols

private val HINT = TextStyle(fontFamily = Fonts.text, fontSize = 13.sp, fontWeight = FontWeight(500), letterSpacing = 0.1.sp)

/**
 * The line under the list. Left: what just happened ("Copied"), or the name of the grid's cell.
 * Right: the one key worth knowing for the selected row, then Esc. The key caps keep their places;
 * only what they say changes.
 */
@Composable
fun Footer(model: OverlayModel) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val ink = scheme.onSurface.copy(alpha = SECOND)
    val r = model.current
    val grid = r?.body as? Body.Grid
    val slide = with(LocalDensity.current) { 8.dp.roundToPx() }
    Row(Modifier.fillMaxWidth().height(Metrics.footer + Metrics.pad).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            val word = model.flash
            // (Under an answer that came from somewhere else: who gave it and when, said as quietly as a cell's name.)
            AnimatedContent(word ?: grid?.cells?.getOrNull(model.cell)?.name?.let { " $it" } ?: (r?.body as? Body.Slots)?.source?.let { " $it" }, transitionSpec = {
                // (No size animation of the box: it would uncover a long word letter by letter, cut through its letters.)
                ((fadeIn(motion.fade(120)) + slideInHorizontally(motion.place()) { -slide }) togetherWith fadeOut(motion.fade(80))).using(null)
            }, contentAlignment = Alignment.CenterStart, label = "flash") { text ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    when {
                        text == null -> {}
                        // (The quiet word is the size of the hints it shares the line with.)
                        text.startsWith(" ") -> Text(text.trim(), color = ink, style = HINT, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        else -> {
                            // Strong ink and a mark, not a colour: a mid-tone would vanish over the wrong window.
                            if (!model.flashBad) DrawnCheck(true, scheme.onSurface, Modifier.size(16.dp))
                            Text(text, color = scheme.onSurface, style = HINT.copy(fontSize = 14.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        // (key, what it does) for the selected row; empty key = nothing to say.
        // In a grid the arrows are taken; say what Enter does too.
        if (grid != null && !model.confirming) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 6.dp)) {
            Keycap("⏎")
            Text(stringResource(R.string.action_copy), color = ink, style = HINT, modifier = Modifier.padding(end = 10.dp))
        }
        val first: Pair<String, String> = when {
            model.confirming -> "" to ""
            // The usual rows at rest: nothing is selected, and Down is how to get in.
            model.zeroUp && r == null -> "↓" to stringResource(R.string.hint_choose)
            model.opened != null -> "←" to stringResource(R.string.hint_less)
            // The text is a keyword: Tab makes it the chip. Said with the scope's own words ("Search settings"), not the app's name.
            model.keyword != null -> "tab" to model.keyword!!.title
            grid != null -> "↑↓←→" to stringResource(R.string.hint_move)
            r?.body is Body.Level -> if ((r.body as Body.Level).locked) "" to "" else "← →" to stringResource(R.string.hint_adjust)
            r?.nudge != null -> "← →" to stringResource(R.string.hint_skip)
            model.tabEnters -> "tab" to stringResource(R.string.hint_fill)
            (r?.actions?.size ?: 0) > 1 -> "tab" to stringResource(R.string.hint_actions)
            model.chip != null && model.query.isEmpty() -> "⌫" to stringResource(R.string.hint_leave)
            else -> "" to ""
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(first, transitionSpec = { fadeIn(motion.fade(120)) togetherWith fadeOut(motion.fade(80)) }, contentAlignment = Alignment.CenterEnd, label = "hint") { (cap, label) ->
                if (cap.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Keycap(cap)
                    Text(label, color = ink, style = HINT, modifier = Modifier.padding(end = 10.dp))
                }
            }
            Keycap("esc")
            // What Escape does now: cancel a waiting confirmation, go back from an answer to the rows it was asked from, or close.
            AnimatedContent(if (model.confirming) R.string.hint_cancel else if (model.escapeLeaves) R.string.hint_back else R.string.hint_close,
                transitionSpec = { fadeIn(motion.fade(120)) togetherWith fadeOut(motion.fade(80)) }, label = "esc") { word ->
                Text(stringResource(word), color = ink, style = HINT)
            }
        }
    }
}

/** A key, as on the keyboard. Arrows, Enter and Backspace are drawn as marks: as font glyphs they are hairlines. */
@Composable
fun Keycap(label: String, /** In a row, where the caps are the answer: a little taller, at full ink. */ strong: Boolean = false, /** One of the caps that make a column at the panel's right (the field's esc, the tab under it): they are one width. */ wide: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val ink = scheme.onSurface.copy(alpha = if (strong) 1f else SECOND)
    Box(Modifier.height(if (strong) 24.dp else 22.dp).defaultMinSize(minWidth = if (wide) 36.dp else if (strong) 24.dp else 0.dp).clip(RoundedCornerShape(7.dp)).background(scheme.onSurface.copy(alpha = if (LocalDark.current) 0.14f else 0.10f)).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
        val marks = label.filter { it != ' ' }
        if (marks.isNotEmpty() && marks.all { it in MARKS }) Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            for (c in marks) Icon(Symbols.of(if (c == '⏎') "enter" else if (c == '⌫') "backspace" else "arrow"), null, Modifier.size(13.dp).rotate(MARKS.getValue(c)), tint = ink)
        } else Text(label, color = ink, style = HINT)
    }
}

/** A key combination as caps with plus signs between them: Action + Ctrl + ]. The caps never shrink or wrap. */
@Composable
fun KeyCaps(keys: List<String>, plus: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        keys.forEachIndexed { i, k ->
            if (i > 0) Text("+", color = plus, style = HINT, modifier = Modifier.padding(horizontal = 4.dp))
            Keycap(k, strong = true)
        }
    }
}

/** The marks a key cap can show, and how far the one drawing (an arrow pointing left) is turned for each. */
private val MARKS = mapOf('←' to 0f, '↑' to 90f, '→' to 180f, '↓' to 270f, '⏎' to 0f, '⌫' to 0f)
