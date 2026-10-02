package io.github.kuscher.booklight.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Age
import io.github.kuscher.booklight.core.Clip
import io.github.kuscher.booklight.core.Offer
import io.github.kuscher.booklight.core.Thing
import io.github.kuscher.booklight.ui.Symbols

/**
 * The line under the empty field for something just copied: "Copied 20 s ago · a link and a date".
 * It has a row's seat and a row's skeleton (the mark in the icon column, the text on the titles'
 * edge, a `tab` cap under the field's `esc`), and it is not a result: no pill, no footer, nothing
 * armed. Its parts rise in one after the other, once the panel has made room. What the copy holds
 * may become known a moment later: those words change where they stand, and nothing else moves.
 */
@Composable
fun CopyLine(model: OverlayModel) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    // While the line fades away the model has already let go of it: it is drawn as it last was.
    val last = remember { arrayOfNulls<Offer>(1) }
    model.copy?.let { last[0] = it }
    val offer = last[0] ?: return
    val shown = model.copy != null

    // Coming: the panel makes room first, then the parts rise in. Going: it fades where it stands; the mark a little
    // longer than the words, so that its seat is not empty before row one's mark has come into it.
    val arrive = remember { Animatable(if (motion.on) 0f else 1f) }
    val stay = remember { Animatable(0f) }
    LaunchedEffect(shown) {
        if (shown) {
            stay.snapTo(1f)
            if (motion.on) { arrive.snapTo(0f); arrive.animateTo(1f, motion.fade(ARRIVE_MS + 2 * STAGGER_MS, delay = AFTER_HEIGHT_MS, easing = LinearEasing)) }
        } else stay.animateTo(0f, motion.fade(MARK_LEAVES_MS, easing = LinearEasing))
    }
    if (!shown && stay.value == 0f) return
    val rise = with(LocalDensity.current) { 12.dp.toPx() }
    /** How much of the words is left while the line goes: gone in the first [WORDS_LEAVE_MS] of the mark's time. */
    fun words() = (1f - (1f - stay.value) * MARK_LEAVES_MS / WORDS_LEAVE_MS).coerceIn(0f, 1f)
    // Part [i] of three starts 22 ms after the one before it and takes 140 ms.
    fun Modifier.part(i: Int) = graphicsLayer {
        val t = ((arrive.value * (ARRIVE_MS + 2f * STAGGER_MS) - i * STAGGER_MS) / ARRIVE_MS).coerceIn(0f, 1f)
        alpha = t * if (i == 0) stay.value else words(); translationY = (1f - t) * rise
    }

    val copied = stringResource(R.string.copy_line, age(offer.age))
    val holds = holds(offer.things)
    // The words for what it holds, as they stand on screen: new ones come in the place of the old, which go first.
    var words by remember { mutableStateOf(holds) }
    val said = remember { Animatable(1f) }
    LaunchedEffect(holds) {
        if (holds == words) return@LaunchedEffect
        said.animateTo(0f, motion.fade(70))
        words = holds
        said.animateTo(1f, motion.fade(110))
    }
    val spoken = "$copied · $holds"

    val ink = scheme.onSurface
    val second = ink.copy(alpha = SECOND)
    val open by rememberUpdatedState(shown)
    Row(
        Modifier.padding(horizontal = Metrics.pad).padding(top = Metrics.pad).fillMaxWidth().height(Metrics.row).clip(RoundedCornerShape(24.dp))
            .clickable(enabled = shown, interactionSource = remember { MutableInteractionSource() }, indication = null) { if (open) model.openCopy() }
            .semantics(mergeDescendants = true) { role = Role.Button; contentDescription = spoken }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The disc of a row that is not selected.
        Box(Modifier.part(0).size(36.dp).clip(CircleShape).background(second.copy(alpha = if (dark) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            // (The glyph goes with the words; the disc stays for row one's mark to come into.)
            Icon(Symbols.of("clip"), null, Modifier.size(20.dp).graphicsLayer { alpha = words() }, tint = second)
        }
        // One text in two inks, so both parts stand on one baseline. Too long, it fades at its end: never an ellipsis.
        Box(Modifier.part(1).weight(1f).padding(start = 16.dp, end = 12.dp).fadeEnd()) {
            Text(buildAnnotatedString {
                // All of it in full ink: over a window of the other brightness the glass is mid-grey, and second ink is too
                // weak there for a line that is said nowhere else. What it holds stands out by its weight.
                append(copied); append(" · ")
                withStyle(SpanStyle(color = ink.copy(alpha = said.value), fontWeight = FontWeight(600))) { append(words) }
            }, color = ink, style = SMALL, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        }
        Box(Modifier.part(2)) { Keycap("tab", wide = true) }
    }
}

/** The parts take 140 ms each, 22 ms apart, and start once the panel's height has made most of their room. */
private const val ARRIVE_MS = 140
private const val STAGGER_MS = 22
private const val AFTER_HEIGHT_MS = 120
private const val MARK_LEAVES_MS = 140
private const val WORDS_LEAVE_MS = 70f

@Composable
private fun age(a: Age): String = when (a) {
    Age.JustNow -> stringResource(R.string.copy_age_now)
    is Age.Seconds -> stringResource(R.string.copy_age_s, a.n)
    Age.Minute -> stringResource(R.string.copy_age_min)
}

/** What the copy holds, in words: the things the system found, two at most by name; "text" when it found none or has not looked. */
@Composable
private fun holds(things: List<Thing>): String {
    val (named, more) = Clip.named(things)
    val names = named.map {
        stringResource(when (it) {
            Thing.LINK -> R.string.copy_link
            Thing.DATE -> R.string.copy_date
            Thing.PHONE -> R.string.copy_phone
            Thing.MAIL -> R.string.copy_mail
        })
    }
    return when {
        names.isEmpty() -> stringResource(R.string.copy_text)
        names.size == 1 -> names[0]
        more -> stringResource(R.string.copy_more, names[0], names[1])
        else -> stringResource(R.string.copy_two, names[0], names[1])
    }
}
