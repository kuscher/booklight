package io.github.kuscher.booklight.overlay

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import io.github.kuscher.booklight.core.Act
import io.github.kuscher.booklight.core.AppChip
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.ui.AppIcons
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols

/**
 * The field: the search engine's mark, or the chip of the scope the text is for, then the text.
 * Entering a scope turns the typed keyword into the chip: it grows where the mark was, its name
 * comes in from where the keyword stood, and the text slides over to make room. An app's chip
 * shows the app's own icon, in the column the row's icon stood in: the app is seen to move up
 * into the field.
 */
@Composable
fun Field(
    model: OverlayModel, field: TextFieldValue, icons: AppIcons,
    /** An edit: the text it made, and the text it was made on (what the editor held, which may be older than [field]). */
    onChange: (now: TextFieldValue, on: TextFieldValue) -> Unit, focus: FocusRequester,
    /** How much of the mark at the field's start and of the cap at its end shows, 0 to 1: they come when the glass is all but open, so its edge never cuts them. */
    ends: () -> Float = { 1f },
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    // What the editor holds: the text it was last given here, or what its own last edit made of that. After every edit
    // it is given [field] again (which the edit may not have changed), so this is composed again then, and knows.
    val held = remember { arrayOf(field) }
    var edits by remember { mutableIntStateOf(0) }
    edits.let { SideEffect { held[0] = field } }
    Row(Modifier.fillMaxWidth().height(Metrics.field).padding(start = 20.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(
            model.chip,
            transitionSpec = {
                // A typed keyword's chip comes from where the keyword stood. A chip nobody typed (what was copied, text another
                // app handed over) has nowhere to come from: it appears where the mark was.
                val typed = !targetState?.keywords.isNullOrEmpty() || (targetState is AppChip && model.chipTyped)
                val enter = if (targetState != null) fadeIn(motion.fade(110, if (typed) 0 else UNTYPED_AFTER_MS)) + slideInHorizontally(motion.place()) { if (typed) it / 2 else 0 } else scaleIn(motion.pop(), 0.6f) + fadeIn(motion.fade(110))
                val exit = if (targetState != null) scaleOut(targetScale = 0.6f) + fadeOut(motion.fade(80)) else fadeOut(motion.fade(80))
                // A typed keyword's chip takes its room on a spring, as the text slides over. A chip nobody typed takes its room in
                // one step, once the old placeholder has faded where it stood, and then appears in it: its name is drawn in
                // full from its first frame, and the caret never runs through it.
                val springs = typed || !initialState?.keywords.isNullOrEmpty() || initialState is AppChip
                (enter togetherWith exit).using(SizeTransform(clip = false) { _, _ -> if (springs) motion.place() else motion.fade(1, UNTYPED_AFTER_MS, LinearEasing) })
            },
            contentKey = { it?.key }, contentAlignment = Alignment.CenterStart, label = "mark", modifier = Modifier.graphicsLayer { alpha = ends() },
        ) { chip ->
            // An app's chip shows only the app, whichever of its two actions is armed: a screen reader is told the action too.
            if (chip != null) ScopeChip(chip, icons, said = (chip as? AppChip)?.let { c -> model.act?.let { stringResource(R.string.a11y_selected, c.name, stringResource(if (it == Act.SEARCH) R.string.action_search else R.string.action_play)) } }) { model.leaveScope(withText = true) }
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
                // While a tip shows, its example stands here: where it would be typed, in the ink that means "not typed yet".
                // (Under an app's chip: what to type for the action that is armed, which Tab changes.)
                AnimatedContent(model.hint ?: model.tip?.takeIf { !model.tipOff }?.example?.trim() ?: stringResource(R.string.search_hint), transitionSpec = {
                    // Their box changes its width on our spring and without a clip: a longer placeholder is never cut through its
                    // letters while it comes.
                    ((fadeIn(motion.fade(140, 60)) + slideInHorizontally(motion.place()) { it / 40 }) togetherWith fadeOut(motion.fade(60)))
                        .using(SizeTransform(clip = false) { _, _ -> motion.place() })
                }, contentAlignment = Alignment.CenterStart, label = "hint") { hint ->
                    Text(hint, style = style.copy(color = scheme.onSurface.copy(alpha = THIRD), fontWeight = FontWeight(400)), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
                }
            }
            // The rest of the top hit's name, grey, after the cursor: drawn with the text so it sits on the same line.
            val rest = model.completion
            val ghostAlpha by animateFloatAsState(if (rest != null) THIRD else 0f, motion.fade(140), label = "ghost")
            val ghost = scheme.onSurface.copy(alpha = ghostAlpha)
            val label = stringResource(R.string.a11y_search_field)
            BasicTextField(
                value = field,
                onValueChange = { v -> val on = held[0]; held[0] = v; onChange(v, on); edits++ },
                modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = label },
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(scheme.primary),
                visualTransformation = remember(rest, ghost) { Ghost(rest.orEmpty(), ghost) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Go),
            )
        }
        // The footer says "esc Close" once there is one; until then the field does.
        // Rows that come while the glass is still opening (the usual rows) take the cap away before it was ever seen: from
        // then on it never gets brighter than it was at that moment, or it would flash in the middle of its own leaving.
        val none = model.results.isEmpty()
        val most = remember(none) { if (none) 1f else androidx.compose.runtime.snapshots.Snapshot.withoutReadObservation { ends() } }
        AnimatedVisibility(none, Modifier.graphicsLayer { alpha = minOf(ends(), most) }, enter = fadeIn(motion.fade(120)), exit = fadeOut(motion.fade(120))) { Keycap("esc", wide = true) }
    }
}

/** A chip nobody typed comes this long after the key: the placeholder it replaces has faded by then. */
private const val UNTYPED_AFTER_MS = 60

/**
 * The scope's chip: its icon where the mark was, and its name. Neutral, like a key cap: the
 * selection stays the panel's one coloured surface. A click turns it back into its keyword.
 * An app's chip has the app's own icon, as every mark has that is about one app; its name fades
 * at its end where it is a long one, and the icon stays. [said]: what a screen reader calls it,
 * where that is more than its name.
 */
@Composable
private fun ScopeChip(scope: Scope, icons: AppIcons, said: String? = null, onLeave: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDark.current
    val leave = stringResource(R.string.a11y_leave_scope, scope.name)
    Row(
        Modifier.height(36.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = if (dark) 0.14f else 0.10f))
            .border(with(LocalDensity.current) { 1f.toDp() }, Color.White.copy(alpha = if (dark) 0.30f else 0.55f), CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = leave, onClick = onLeave)
            .then(if (said != null) Modifier.semantics(mergeDescendants = true) { contentDescription = said } else Modifier)
            .padding(start = 8.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChipMark(scope, icons)
        val style = TextStyle(fontFamily = Fonts.text, fontSize = 15.sp, fontWeight = FontWeight(600))
        if (scope is AppChip) {
            // (Only a name that is cut fades: a short one stands whole to its last letter.)
            var cut by remember(scope.name) { mutableStateOf(false) }
            Box(Modifier.padding(start = 6.dp).widthIn(max = APP_NAME).then(if (cut) Modifier.fadeEnd(16.dp) else Modifier)) {
                Text(scope.name, color = scheme.onSurface, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, onTextLayout = { cut = it.didOverflowWidth })
            }
        } else Text(scope.name, color = scheme.onSurface, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp).widthIn(max = 220.dp))
    }
}

/** An app's name on its chip has this much room: a longer one fades at its end. */
private val APP_NAME = 160.dp

/** The picture on a chip, 20 dp: a scope's symbol, or the app's own icon for an app's chip. */
@Composable
internal fun ChipMark(scope: Scope, icons: AppIcons) {
    val app = (scope as? AppChip)?.icon ?: io.github.kuscher.booklight.core.Icon.App.of(scope.symbol)
    if (app == null) { Icon(Symbols.of(scope.symbol), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface); return }
    // (Asked for at the size a row's picture is: one bitmap for an app is kept, and it is there already when the app's row was drawn.)
    val px = with(LocalDensity.current) { 48.dp.roundToPx() }
    val bitmap by produceState(icons.cached(app), app) { if (value == null) value = icons.load(app, px) }
    Box(Modifier.size(20.dp)) { bitmap?.let { Image(it, null, Modifier.fillMaxSize()) } }
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
