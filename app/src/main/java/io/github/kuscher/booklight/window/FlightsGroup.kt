package io.github.kuscher.booklight.window

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.AirLabs
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.delay

/** The row of the window where the flight service's key is set: "Set up times" in the panel opens the window on it. */
const val FLIGHT_KEY_ROW = "flight-key"

/**
 * Flights, on Results: the one place where the user's own key for the flight service goes in.
 *
 * A row that says what is sent and when, and how many lookups are left. Under its text, on the text's edge and
 * ending on the line the rows' controls end on, a field to paste the key into ([Field]). While something is
 * typed, Save stands at the field's end; while a key is in and nothing is typed, the field says so and Take out
 * stands there, in the error colour, and asks for a second press. In a narrow row the button stands under the
 * field. The key that is in is never shown again, and nothing here logs it: it can be replaced or taken out.
 *
 * Keys: Enter on the row puts the caret in the field; Enter there saves, Escape gives up what was typed, Tab
 * goes on; each gives the keys back to the page. Delete on the row, or its menu, takes the key out (asked twice).
 *
 * A second row leads to where a key is got.
 *
 * It needs nothing of the page but [onTyping], which says that a text field has the keys.
 */
@Composable
fun FlightsGroup(page: Page, app: BooklightApp, onTyping: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val activity = LocalActivity.current as ComponentActivity
    val key by app.prefs.flightKey.collectAsState()
    val left by app.flights.left.collectAsState()
    var typed by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    // Taking the key out needs a second press, a moment later: a double click or a bouncing key does not do it.
    var sure by remember { mutableStateOf(false) }
    var asked by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sure) { if (sure) { delay(3000); sure = false } }
    LaunchedEffect(focused) { onTyping(focused) }
    DisposableEffect(Unit) { onDispose { onTyping(false) } }
    /** The keys go back to the page, on this row. */
    fun leave() { page.selected = FLIGHT_KEY_ROW; onTyping(false) }
    fun save() { if (typed.isNotBlank()) { app.prefs.setFlightKey(typed); typed = "" } }
    fun takeOut() {
        val now = android.os.SystemClock.uptimeMillis()
        if (sure && now - asked >= 350) { app.prefs.setFlightKey(""); sure = false }
        else if (!sure) { sure = true; asked = now }
    }
    val has = key.isNotEmpty()
    val narrow = LocalColumn.current < ROW_WIDE
    val takeOutWord = stringResource(R.string.action_take_out)

    Group(stringResource(R.string.set_flights)) {
        row(FLIGHT_KEY_ROW) { place ->
            val about = stringResource(R.string.set_flight_key_text) + if (has && left != null) "\n" + stringResource(R.string.set_flight_left, left!!) else ""
            // What stands at the field's end: Save while something is typed, Take out while a key is in and nothing is.
            val saving = typed.isNotBlank()
            val taking = has && !saving
            val saveButton: @Composable () -> Unit = {
                Button(::save, Modifier.height(CONTROL).focusProperties { canFocus = false }, contentPadding = PaddingValues(horizontal = 20.dp)) { Text(stringResource(R.string.action_save), maxLines = 1, softWrap = false) }
            }
            val takeButton: @Composable () -> Unit = {
                // In the ground's colour, like a choice's button that is not chosen; its word in the error colour.
                Button(::takeOut, Modifier.height(CONTROL).focusProperties { canFocus = false }, contentPadding = PaddingValues(horizontal = 20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.ground, contentColor = scheme.error)) {
                    Text(stringResource(if (sure) R.string.win_take_out_again else R.string.action_take_out), maxLines = 1, softWrap = false)
                }
            }
            PageRow(page, FLIGHT_KEY_ROW, stringResource(R.string.set_flight_key), about, place = place, mark = { MarkIcon("plane") },
                onEnter = { runCatching { field.requestFocus() } },
                onDelete = if (has) ::takeOut else null,
                menu = if (has) listOf(RowAction(takeOutWord, danger = true, run = ::takeOut)) else emptyList(),
                below = {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Field(
                                typed, { typed = it.trim().take(128); sure = false },
                                // With a key in, the field says so until it has the keys; then it asks for the new one.
                                hint = stringResource(if (has && !focused) R.string.set_flight_key_in else R.string.set_flight_key_hint),
                                focus = field, modifier = Modifier.weight(1f),
                                leading = if (has && !focused && typed.isEmpty()) ({ Icon(Symbols.check, null, Modifier.size(20.dp), tint = scheme.primary) }) else null,
                                onFocus = { focused = it },
                                onEnter = { save(); leave() },
                                onEscape = { typed = ""; leave() },
                                onTab = { back -> leave(); page.move(if (back) -1 else 1) },
                            )
                            if (!narrow) {
                                // The field gives the button its room as it comes, and takes it back as it goes.
                                AnimatedVisibility(saving, enter = expandHorizontally(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkHorizontally(motion.place()) + fadeOut(motion.fade(70))) { Box(Modifier.padding(start = 8.dp)) { saveButton() } }
                                AnimatedVisibility(taking, enter = expandHorizontally(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkHorizontally(motion.place()) + fadeOut(motion.fade(70))) { Box(Modifier.padding(start = 8.dp)) { takeButton() } }
                            }
                        }
                        if (narrow) {
                            AnimatedVisibility(saving || taking, enter = expandVertically(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkVertically(motion.place()) + fadeOut(motion.fade(70))) {
                                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (saving) saveButton() else takeButton() }
                            }
                        }
                    }
                })
        }
        row("flight-get") { place ->
            PageRow(page, "flight-get", stringResource(R.string.set_flight_get), stringResource(R.string.set_flight_get_text), place = place, mark = { MarkIcon("key") },
                onEnter = { app.executor.run(Effect.OpenUrl(AirLabs.SIGN_UP), activity) }) { Opens() }
        }
    }
}
