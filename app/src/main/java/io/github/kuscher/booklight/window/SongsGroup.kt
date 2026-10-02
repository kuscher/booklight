package io.github.kuscher.booklight.window

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Spotify
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.delay

/** The row of the window where the key for Spotify is set: "Set up playing" on the row of `play` opens the window on it. */
const val SONG_KEY_ROW = "song-key"

/**
 * Spotify, in the Booklight window › Labs: the one place where the user's own key for Spotify goes in. It is
 * `FlightsGroup` with a key of two halves, and stands wherever that one does: it needs nothing of the page
 * but [onTyping], which says that a text field has the keys.
 *
 * A row that says what is sent and when. Under its text, on the text's edge and ending on the line the rows'
 * controls end on, two fields, for the client id and for its secret ([Field]). While both hold something, Save
 * stands at their end; while a key is in and nothing is typed, one field says so and Take out stands there, in
 * the error colour, and asks for a second press. In a narrow row the fields stand one under the other and the
 * button under them. The key that is in is never shown again, and nothing here logs it: it can be replaced or
 * taken out.
 *
 * Keys: Enter on the row puts the caret in the first field; Enter there goes on to the second, Enter in the
 * second saves; Escape gives up what was typed; Tab goes from the first to the second and on, Shift + Tab back;
 * leaving them gives the keys back to the page. Delete on the row, or its menu, takes the key out (asked twice).
 *
 * A second row leads to where a key is got.
 */
@Composable
fun SongsGroup(page: Page, app: BooklightApp, onTyping: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val activity = LocalActivity.current as ComponentActivity
    val key by app.prefs.spotifyKey.collectAsState()
    var id by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var inId by remember { mutableStateOf(false) }
    var inSecret by remember { mutableStateOf(false) }
    val idField = remember { FocusRequester() }
    val secretField = remember { FocusRequester() }
    val focused = inId || inSecret
    // Taking the key out needs a second press, a moment later: a double click or a bouncing key does not do it.
    var sure by remember { mutableStateOf(false) }
    var asked by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sure) { if (sure) { delay(3000); sure = false } }
    LaunchedEffect(focused) { onTyping(focused) }
    DisposableEffect(Unit) { onDispose { onTyping(false) } }
    /** The keys go back to the page, on this row. */
    fun leave() { page.selected = SONG_KEY_ROW; onTyping(false) }
    val whole = id.isNotBlank() && secret.isNotBlank()
    fun save() { if (whole) { app.prefs.setSpotifyKey(id, secret); id = ""; secret = "" } }
    fun takeOut() {
        val now = android.os.SystemClock.uptimeMillis()
        if (sure && now - asked >= 350) { app.prefs.setSpotifyKey("", ""); sure = false }
        else if (!sure) { sure = true; asked = now }
    }
    val has = key.isNotEmpty()
    val typing = id.isNotEmpty() || secret.isNotEmpty()
    // With a key in and nothing going on, one field says so; the second comes when a new key is being put in.
    val both = !has || focused || typing
    val narrow = LocalColumn.current < ROW_WIDE
    val share by animateFloatAsState(if (both) 1f else 0f, motion.place(), label = "second")
    val takeOutWord = stringResource(R.string.action_take_out)

    Group(stringResource(R.string.set_songs)) {
        row(SONG_KEY_ROW) { place ->
            // What stands at the fields' end: Save while both hold something, Take out while a key is in and nothing is typed.
            val saving = whole
            val taking = has && !typing
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
            val first: @Composable (Modifier) -> Unit = { m ->
                Field(
                    id, { id = it.trim().take(128); sure = false },
                    // With a key in, the field says so until it has the keys; then it asks for the new one.
                    hint = stringResource(if (has && !both) R.string.set_song_key_in else R.string.set_song_id_hint),
                    focus = idField, modifier = m,
                    leading = if (has && !both) ({ Icon(Symbols.check, null, Modifier.size(20.dp), tint = scheme.primary) }) else null,
                    onFocus = { inId = it },
                    onEnter = { if (whole) { save(); leave() } else runCatching { secretField.requestFocus() } },
                    onEscape = { id = ""; secret = ""; leave() },
                    onTab = { back -> if (back) { leave(); page.move(-1) } else runCatching { secretField.requestFocus() } },
                )
            }
            val second: @Composable (Modifier) -> Unit = { m ->
                Field(
                    secret, { secret = it.trim().take(128); sure = false },
                    hint = stringResource(R.string.set_song_secret_hint), focus = secretField, modifier = m,
                    onFocus = { inSecret = it },
                    onEnter = { if (whole) { save(); leave() } else runCatching { idField.requestFocus() } },
                    onEscape = { id = ""; secret = ""; leave() },
                    onTab = { back -> if (back) runCatching { idField.requestFocus() } else { leave(); page.move(1) } },
                )
            }
            PageRow(page, SONG_KEY_ROW, stringResource(R.string.set_song_key), stringResource(R.string.set_song_key_text), place = place, mark = { MarkIcon("music") },
                onEnter = { runCatching { idField.requestFocus() } },
                onDelete = if (has) ::takeOut else null,
                menu = if (has) listOf(RowAction(takeOutWord, danger = true, run = ::takeOut)) else emptyList(),
                below = {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp)) {
                        if (!narrow) Row(verticalAlignment = Alignment.CenterVertically) {
                            first(Modifier.weight(1f))
                            // The first field gives the second its half of the row as it comes, and takes it back as it goes; so with the button.
                            if (share > 0.01f) second(Modifier.weight(share).padding(start = 8.dp * share).graphicsLayer { alpha = share })
                            AnimatedVisibility(saving, enter = expandHorizontally(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkHorizontally(motion.place()) + fadeOut(motion.fade(70))) { Box(Modifier.padding(start = 8.dp)) { saveButton() } }
                            AnimatedVisibility(taking, enter = expandHorizontally(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkHorizontally(motion.place()) + fadeOut(motion.fade(70))) { Box(Modifier.padding(start = 8.dp)) { takeButton() } }
                        } else {
                            first(Modifier.fillMaxWidth())
                            AnimatedVisibility(both, enter = expandVertically(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkVertically(motion.place()) + fadeOut(motion.fade(70))) { second(Modifier.padding(top = 8.dp).fillMaxWidth()) }
                            AnimatedVisibility(saving || taking, enter = expandVertically(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkVertically(motion.place()) + fadeOut(motion.fade(70))) {
                                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (saving) saveButton() else takeButton() }
                            }
                        }
                    }
                })
        }
        row("song-get") { place ->
            PageRow(page, "song-get", stringResource(R.string.set_song_get), stringResource(R.string.set_song_get_text), place = place, onTitleLine = true, mark = { MarkIcon("key") },
                onEnter = { app.executor.run(Effect.OpenUrl(Spotify.DASHBOARD), activity) }) { Opens() }
        }
    }
}
