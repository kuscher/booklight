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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols

private val HINT = TextStyle(fontFamily = Fonts.text, fontSize = 12.sp, fontWeight = FontWeight(500), letterSpacing = 0.1.sp)

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
    Row(Modifier.fillMaxWidth().height(Metrics.footer + Metrics.pad).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            val word = model.flash
            AnimatedContent(word ?: grid?.cells?.getOrNull(model.cell)?.name?.let { " $it" }, transitionSpec = {
                (fadeIn(motion.fade(120)) + slideInHorizontally(motion.place()) { -it / 6 }) togetherWith fadeOut(motion.fade(80))
            }, contentAlignment = Alignment.CenterStart, label = "flash") { text ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    when {
                        text == null -> {}
                        text.startsWith(" ") -> Text(text.trim(), color = ink, style = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        else -> {
                            // Strong ink and a mark, not a colour: a mid-tone would vanish over the wrong window.
                            if (!model.flashBad) Icon(Symbols.check, null, Modifier.size(16.dp), tint = scheme.onSurface)
                            Text(text, color = scheme.onSurface, style = HINT.copy(fontSize = 14.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        // (key, what it does) for the selected row; empty key = nothing to say.
        val first: Pair<String, String> = when {
            model.confirming -> "" to ""
            grid != null -> "↑↓←→" to stringResource(R.string.hint_move)
            r?.body is Body.Level -> if ((r.body as Body.Level).locked) "" to "" else "← →" to stringResource(R.string.hint_adjust)
            r?.nudge != null -> "← →" to stringResource(R.string.hint_skip)
            r?.actions?.getOrNull(model.armed)?.effect is Effect.EnterScope -> "tab" to stringResource(R.string.hint_fill)
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
            AnimatedContent(model.confirming, transitionSpec = { fadeIn(motion.fade(120)) togetherWith fadeOut(motion.fade(80)) }, label = "esc") { sure ->
                Text(stringResource(if (sure) R.string.hint_cancel else R.string.hint_close), color = ink, style = HINT)
            }
        }
    }
}

@Composable
fun Keycap(label: String) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.height(22.dp).clip(RoundedCornerShape(7.dp)).background(scheme.onSurface.copy(alpha = if (LocalDark.current) 0.14f else 0.10f)).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
        Text(label, color = scheme.onSurface.copy(alpha = SECOND), style = HINT)
    }
}
