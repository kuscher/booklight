package io.github.kuscher.booklight.window

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.AirLabs
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.overlay.LocalDark
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.THIRD
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols

/** The row of the window where the flight service's key is set: "Set up times" in the panel opens the window on it. */
const val FLIGHT_KEY_ROW = "flight-key"

/**
 * Flights: the one place where the user's own key for the flight service goes in. A row that says
 * what is sent and when, and whether a key is in; Enter opens one field under it to paste the key
 * into. The key that is in is never shown again: it can be replaced or taken out. A second row
 * leads to where a key is got.
 *
 * It stands by itself: its rows are ordinary rows of the page, and it needs nothing of the page
 * but [onTyping], which says that a text field has the keys.
 */
@Composable
fun FlightsGroup(page: Page, app: BooklightApp, onTyping: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val activity = LocalActivity.current as ComponentActivity
    val key by app.prefs.flightKey.collectAsState()
    val left by app.flights.left.collectAsState()
    var open by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    fun close() { open = false; typed = "" }
    LaunchedEffect(open) { onTyping(open) }
    DisposableEffect(Unit) { onDispose { onTyping(false) } }
    val word = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(600))

    Column {
        GroupLabel(stringResource(R.string.set_flights))
        val about = stringResource(R.string.set_flight_key_text) + if (key.isNotEmpty() && left != null) "\n" + stringResource(R.string.set_flight_left, left!!) else ""
        PageRow(page, FLIGHT_KEY_ROW, stringResource(R.string.set_flight_key), about, mark = { Icon(Symbols.of("plane"), null, tint = it) }, onEnter = { open = !open; typed = "" }) {
            Text(stringResource(if (key.isEmpty()) R.string.set_flight_key_none else R.string.set_flight_key_in), color = it, style = word)
        }
        // The field opens under its row and closes back into it: the page makes room, nothing pops over it.
        AnimatedVisibility(open, enter = expandVertically(motion.place()) + fadeIn(motion.fade(140, 60)), exit = shrinkVertically(motion.place()) + fadeOut(motion.fade(80))) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { withFrameNanos { }; runCatching { focus.requestFocus() } }
            fun save() { if (typed.isNotBlank()) { app.prefs.setFlightKey(typed); close() } }
            Column(
                Modifier.fillMaxWidth().padding(start = GUTTER, end = GUTTER, top = 4.dp, bottom = 12.dp)
                    .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) { close(); true } else false }
                    .clip(RoundedCornerShape(24.dp))
                    .background(scheme.surfaceContainerLowest.copy(alpha = if (dark) 0.36f else 0.62f))
                    .border(with(LocalDensity.current) { 1f.toDp() }, Color.White.copy(alpha = if (dark) 0.20f else 0.55f), RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val style = TextStyle(fontFamily = Fonts.text, fontSize = 16.sp, color = scheme.onSurface)
                Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).background(scheme.onSurface.copy(alpha = 0.07f)).padding(horizontal = 14.dp, vertical = 11.dp)) {
                    if (typed.isEmpty()) Text(stringResource(R.string.set_flight_key_hint), style = style.copy(color = scheme.onSurface.copy(alpha = THIRD)))
                    BasicTextField(typed, { typed = it.trim().take(128) }, Modifier.fillMaxWidth().focusRequester(focus).onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)) { save(); true } else false
                    }, singleLine = true, textStyle = style, cursorBrush = SolidColor(scheme.primary))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    KeyButton(stringResource(R.string.action_save), strong = true, enabled = typed.isNotBlank(), onClick = ::save)
                    KeyButton(stringResource(R.string.hint_cancel), onClick = ::close)
                    // Taking the key out is last, and red, like everything that removes something.
                    if (key.isNotEmpty()) KeyButton(stringResource(R.string.action_take_out), danger = true) { app.prefs.setFlightKey(""); close() }
                }
            }
        }
        PageRow(page, "flight-get", stringResource(R.string.set_flight_get), stringResource(R.string.set_flight_get_text), mark = { Icon(Symbols.of("key"), null, tint = it) },
            onEnter = { app.executor.run(Effect.OpenUrl(AirLabs.SIGN_UP), activity) }) { Icon(Symbols.of("open"), null, tint = it) }
    }
}

/** A button under the key's field: quiet, the one that saves, or the one that takes the key out. */
@Composable
private fun KeyButton(label: String, strong: Boolean = false, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Text(
        label, color = (if (danger) scheme.error else if (strong) scheme.onSecondaryContainer else scheme.onSurface).copy(alpha = if (enabled) 1f else 0.4f),
        style = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(600)),
        modifier = Modifier.clip(CircleShape).background(if (strong && enabled) scheme.secondaryContainer else scheme.onSurface.copy(alpha = 0.07f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
