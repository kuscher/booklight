package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols

/**
 * The field: the search engine's mark, or the chip of the scope the text is for, then the text.
 * Entering a scope turns the typed keyword into the chip: it grows where the mark was, its name
 * comes in from where the keyword stood, and the text slides over to make room.
 */
@Composable
fun Field(model: OverlayModel, field: TextFieldValue, onChange: (TextFieldValue) -> Unit, focus: FocusRequester) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    Row(Modifier.fillMaxWidth().height(Metrics.field).padding(start = 20.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(
            model.chip,
            transitionSpec = {
                val enter = if (targetState != null) fadeIn(motion.fade(110)) + slideInHorizontally(motion.place()) { it / 2 } else scaleIn(motion.pop(), 0.6f) + fadeIn(motion.fade(110))
                val exit = if (targetState != null) scaleOut(targetScale = 0.6f) + fadeOut(motion.fade(80)) else fadeOut(motion.fade(80))
                (enter togetherWith exit).using(SizeTransform(clip = false) { _, _ -> motion.place() })
            },
            contentKey = { it?.key }, contentAlignment = Alignment.CenterStart, label = "mark",
        ) { chip ->
            if (chip != null) ScopeChip(chip) { model.leaveScope() }
            // The search engine's mark where the magnifier would be: Google's G when Google does the searching.
            else Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                if (model.settings.engine == "google") Text("G", color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.round, fontSize = 24.sp, fontWeight = FontWeight(600)))
                else Icon(Symbols.search, null, Modifier.size(22.dp), tint = scheme.onSurface.copy(alpha = SECOND))
            }
        }
        Box(Modifier.weight(1f).padding(start = if (model.chip != null) 12.dp else 16.dp), contentAlignment = Alignment.CenterStart) {
            val style = TextStyle(fontFamily = Fonts.text, fontSize = 24.sp, fontWeight = FontWeight(500), color = scheme.onSurface)
            if (field.text.isEmpty()) {
                // The placeholder says what to type: what Booklight finds, or what the scope takes.
                AnimatedContent(model.chip?.hint ?: stringResource(R.string.search_hint), transitionSpec = {
                    (fadeIn(motion.fade(140, 60)) + slideInHorizontally(motion.place()) { it / 40 }) togetherWith fadeOut(motion.fade(60))
                }, contentAlignment = Alignment.CenterStart, label = "hint") { hint ->
                    Text(hint, style = style.copy(color = scheme.onSurface.copy(alpha = THIRD), fontWeight = FontWeight(400)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // The rest of the top hit's name, grey, after the cursor: drawn with the text so it sits on the same line.
            val rest = model.completion
            val ghostAlpha by animateFloatAsState(if (rest != null) THIRD else 0f, motion.fade(140), label = "ghost")
            val ghost = scheme.onSurface.copy(alpha = ghostAlpha)
            val label = stringResource(R.string.a11y_search_field)
            BasicTextField(
                value = field,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = label },
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(scheme.primary),
                visualTransformation = remember(rest, ghost) { Ghost(rest.orEmpty(), ghost) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Go),
            )
        }
        // The footer says "esc Close" once there is one; until then the field does.
        AnimatedVisibility(model.results.isEmpty(), enter = fadeIn(motion.fade(120)), exit = fadeOut(motion.fade(120))) { Keycap("esc") }
    }
}

/**
 * The scope's chip: its icon where the mark was, and its name. Neutral, like a key cap: the
 * selection stays the panel's one coloured surface. A click turns it back into its keyword.
 */
@Composable
private fun ScopeChip(scope: Scope, onLeave: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDark.current
    val leave = stringResource(R.string.a11y_leave_scope, scope.name)
    Row(
        Modifier.height(36.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = if (dark) 0.14f else 0.10f))
            .border(with(LocalDensity.current) { 1f.toDp() }, Color.White.copy(alpha = if (dark) 0.30f else 0.55f), CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = leave, onClick = onLeave)
            .padding(start = 8.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Symbols.of(scope.symbol), null, Modifier.size(20.dp), tint = scheme.onSurface)
        Text(scope.name, color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.text, fontSize = 15.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp).widthIn(max = 220.dp))
    }
}

/** Appends grey completion text after what was typed; the cursor stays at the end of the real text. */
private class Ghost(private val rest: String, private val color: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (rest.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val n = text.length
        val shown = buildAnnotatedString { append(text); withStyle(SpanStyle(color = color)) { append(rest) } }
        return TransformedText(shown, object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset
            override fun transformedToOriginal(offset: Int) = minOf(offset, n)
        })
    }
    override fun equals(other: Any?) = other is Ghost && other.rest == rest && other.color == color
    override fun hashCode() = rest.hashCode() * 31 + color.hashCode()
}
