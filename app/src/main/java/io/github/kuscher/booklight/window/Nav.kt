package io.github.kuscher.booklight.window

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.overlay.LocalDark
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.SECOND
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.launch

/** The window's sections, in the order of the column. */
enum class Part(val id: String, val symbol: String, val title: Int) {
    START("start", "booklight", R.string.nav_start),
    COMMANDS("commands", "list", R.string.nav_commands),
    YOURS("yours", "user", R.string.nav_yours),
    LOOK("look", "sun", R.string.nav_look),
    RESULTS("results", "search", R.string.nav_results),
    ACCESS("access", "lock", R.string.nav_access),
    ABOUT("about", "info", R.string.nav_about);

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id }
    }
}

/** A column item is this high; the column at rest this wide, and never narrower than one item is high. */
val NAV_ITEM = 48.dp
val NAV_WIDE = 200.dp

/**
 * The column of sections. One quiet pane of glass says which section is open and travels between
 * them, its leading edge first, as the selection does in the panel. It is not the page's pill: that
 * one is coloured and says where the keys are. When the keys are in the column ([focused]) the pane
 * takes a ring. The column's width follows the window's and is never animated: as it narrows the
 * names are covered, down to a column of marks.
 */
@Composable
fun NavColumn(current: Part, focused: Boolean, width: Dp, /** A small dot on Start: no key has opened the panel yet. */ dot: Boolean, onPick: (Part) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val top = NAV_ITEM * current.ordinal
    val upper = remember { Animatable(top, Dp.VectorConverter) }
    val lower = remember { Animatable(top + NAV_ITEM, Dp.VectorConverter) }
    LaunchedEffect(top) {
        val down = top > upper.targetValue
        launch { upper.animateTo(top, if (down) motion.trail() else motion.lead()) }
        launch { lower.animateTo(top + NAV_ITEM, if (down) motion.lead() else motion.trail()) }
    }
    val ring by animateFloatAsState(if (focused) 1f else 0f, motion.fade(120), label = "ring")
    // How much of the names shows: all at full width, none once the column is a column of marks.
    val names = ((width - NAV_ITEM) / (NAV_WIDE - NAV_ITEM)).coerceIn(0f, 1f)
    val shape = RoundedCornerShape(24.dp)
    val hair = with(LocalDensity.current) { 1f.toDp() }
    Box(modifier.width(width).height(NAV_ITEM * Part.entries.size)) {
        Box(
            Modifier.offset { IntOffset(0, upper.value.roundToPx()) }.width(width).height((lower.value - upper.value).coerceAtLeast(12.dp))
                .border(hair, if (dark) Color.Black.copy(alpha = 0.28f) else scheme.onSurface.copy(alpha = 0.20f), shape)
                .clip(shape).background(scheme.surfaceContainerLowest.copy(alpha = if (dark) 0.36f else 0.62f))
                .border(hair, Color.White.copy(alpha = if (dark) 0.30f else 0.55f), shape)
                .border(2.dp, scheme.onSurface.copy(alpha = ring), shape),
        )
        Column {
            for (p in Part.entries) {
                val on = p == current
                val ink by androidx.compose.animation.animateColorAsState(scheme.onSurface.copy(alpha = if (on) 1f else SECOND), motion.fade(120), label = "nav")
                val name = stringResource(p.title)
                Box(
                    Modifier.width(width).height(NAV_ITEM).clip(shape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPick(p) }
                        .semantics(mergeDescendants = true) { selected = on; role = Role.Tab }
                        .clipToBounds(),
                ) {
                    // Laid out once at the column's full width; a narrower column only covers the name.
                    Row(Modifier.wrapContentWidth(Alignment.Start, unbounded = true).requiredWidth(NAV_WIDE).height(NAV_ITEM), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(14.dp))
                        Icon(Symbols.of(p.symbol), null, Modifier.size(20.dp), tint = ink)
                        Spacer(Modifier.width(14.dp))
                        Text(name, color = ink, style = TextStyle(fontFamily = Fonts.text, fontSize = 15.sp, fontWeight = FontWeight(600)), maxLines = 1, modifier = Modifier.weight(1f).graphicsLayer { alpha = names })
                    }
                    if (p == Part.START) {
                        val there by animateFloatAsState(if (dot) 1f else 0f, motion.pop(), label = "dot")
                        // At the item's right end; in a column of marks, at the mark's upper right.
                        Box(Modifier.align(if (names > 0.5f) Alignment.CenterEnd else Alignment.TopEnd).offset(x = if (names > 0.5f) (-16).dp else (-8).dp, y = if (names > 0.5f) 0.dp else 10.dp)
                            .size(8.dp).graphicsLayer { scaleX = there.coerceAtLeast(0f); scaleY = there.coerceAtLeast(0f); alpha = there.coerceIn(0f, 1f) }.clip(CircleShape).background(scheme.onSurface))
                    }
                }
            }
        }
    }
}
