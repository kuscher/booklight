package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.core.Act
import io.github.kuscher.booklight.core.AppChip
import io.github.kuscher.booklight.ui.Symbols

/**
 * The line under the empty field of an app's chip, for an app that has both Search and Play: it
 * offers the one that is not armed ("Play in Spotify", or "Search Spotify"), with a `tab` cap.
 * Built like the copy's line: a row's seat and a row's skeleton (the action's symbol in the disc of
 * the icon column, the words on the titles' edge, the cap under the field's `esc`), and not a result:
 * no pill, no footer, nothing armed, and Enter does nothing. Tab, or a click, changes to that action,
 * and the line then offers the other. It goes with the first letter: the row that comes has both
 * actions on it. An app with one action has no line: the field stands alone.
 */
@Composable
fun ChipLine(model: OverlayModel) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val chip = model.chip as? AppChip
    val now = model.otherAct.takeIf { chip != null }
    // While the line fades away the model has already let go of it: it is drawn as it last was.
    val last = remember { arrayOfNulls<Pair<Act, String>>(1) }
    if (now != null && chip != null) last[0] = now to chip.offer(now)
    val (act, words) = last[0] ?: return
    val shown = now != null
    // It comes once the panel has made most of its room, and goes quickly where it stands while the rows arrive under it.
    val here by animateFloatAsState(if (shown) 1f else 0f, if (shown) motion.fade(ARRIVE_MS, AFTER_HEIGHT_MS) else motion.fade(LEAVE_MS), label = "line")
    if (!shown && here == 0f) return

    val ink = scheme.onSurface
    val second = ink.copy(alpha = SECOND)
    Row(
        Modifier.padding(horizontal = Metrics.pad).padding(top = Metrics.pad).fillMaxWidth().height(Metrics.row).clip(RoundedCornerShape(24.dp))
            .graphicsLayer { alpha = here }
            .clickable(enabled = shown, interactionSource = remember { MutableInteractionSource() }, indication = null) { model.swap() }
            .semantics(mergeDescendants = true) { role = Role.Button; contentDescription = words }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The disc of a row that is not selected, with the symbol of what the action does: never an app's icon, the chip has that.
        Box(Modifier.size(36.dp).clip(CircleShape).background(second.copy(alpha = if (dark) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            AnimatedContent(act, transitionSpec = { fadeIn(motion.fade(110, 40)) togetherWith fadeOut(motion.fade(60)) }, label = "symbol") { a ->
                Icon(Symbols.of(a.id), null, Modifier.size(20.dp), tint = second)
            }
        }
        // The words change where they stand when Tab changes the action. Too long, they fade at their end: never an ellipsis.
        Box(Modifier.weight(1f).padding(start = 16.dp, end = 12.dp).fadeEnd()) {
            AnimatedContent(words, transitionSpec = { (fadeIn(motion.fade(110, 40)) togetherWith fadeOut(motion.fade(60))).using(null) }, contentAlignment = Alignment.CenterStart, label = "words") { w ->
                Text(w, color = ink, style = SMALL.copy(fontWeight = FontWeight(600)), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            }
        }
        Keycap("tab", wide = true)
    }
}

private const val ARRIVE_MS = 140
private const val AFTER_HEIGHT_MS = 120
private const val LEAVE_MS = 70
