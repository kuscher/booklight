package io.github.kuscher.booklight.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.BuildConfig
import io.github.kuscher.booklight.ui.BooklightTheme

/**
 * Settings, as a normal window. 0.1 has the two things a new user needs: how to give Booklight
 * a keyboard shortcut, and a way to forget what it has learned.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as BooklightApp
        setContent {
            BooklightTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Booklight", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight(600)))
                            Card("Give Booklight a key") {
                                Text(
                                    "Booklight opens whenever you start it, so any shortcut that opens an app works.\n\n" +
                                        "1. Open Keyboard shortcuts (the G key + /).\n" +
                                        "2. Choose Customize, then add a shortcut under App shortcuts.\n" +
                                        "3. Pick Booklight and press the keys you want, for example G + K.",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Button(onClick = { requestShowKeyboardShortcuts() }) { Text("Open Keyboard shortcuts") }
                            }
                            Card("What Booklight has learned") {
                                var cleared by remember { mutableStateOf(false) }
                                Text(
                                    "Booklight remembers what you pick for the text you type, so your usual choice comes first. " +
                                        "It stays on this device.",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                OutlinedButton(onClick = { app.historyStore.clear(); cleared = true }, enabled = !cleared) {
                                    Text(if (cleared) "Forgotten" else "Forget everything")
                                }
                            }
                            Text("Version ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight(600)))
            content()
        }
    }
}
