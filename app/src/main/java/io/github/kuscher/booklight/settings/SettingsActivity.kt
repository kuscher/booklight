package io.github.kuscher.booklight.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.BuildConfig
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Engines
import io.github.kuscher.booklight.data.SiteEntry
import io.github.kuscher.booklight.ui.BooklightTheme
import io.github.kuscher.booklight.ui.Fonts

/**
 * Settings, as a normal window: the shortcut, web search (engine, suggestions, keyword searches),
 * which kinds of results show, and what Booklight keeps.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as BooklightApp
        setContent {
            BooklightTheme {
                val s by app.prefs.state.collectAsState()
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(Modifier.widthIn(max = 680.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight(700)))

                            Section(stringResource(R.string.set_shortcut_title)) {
                                Body(stringResource(R.string.set_shortcut_text))
                                Button(onClick = { requestShowKeyboardShortcuts() }) { Text(stringResource(R.string.set_shortcut_button)) }
                            }

                            Section(stringResource(R.string.set_search_title)) {
                                Label(stringResource(R.string.set_engine))
                                Chips(Engines.all.map { it.id to it.name }, s.engine) { id -> app.prefs.update { it.copy(engine = id) } }
                                val engine = s.engine()
                                Toggle(stringResource(R.string.set_suggestions), s.suggestions) { on -> app.prefs.update { it.copy(suggestions = on, suggestionsCard = false) } }
                                Body(stringResource(R.string.set_suggestions_text, engine.name, engine.suggestHost), quiet = true)
                            }

                            Section(stringResource(R.string.set_sites_title)) {
                                Body(stringResource(R.string.set_sites_text), quiet = true)
                                for (site in s.sites) SiteRow(site) { app.prefs.update { it.copy(sites = it.sites - site) } }
                                AddSite(taken = s.sites.map { it.keyword.lowercase() }) { new -> app.prefs.update { it.copy(sites = it.sites + new) } }
                            }

                            Section(stringResource(R.string.set_look_title)) {
                                Label(stringResource(R.string.set_glass))
                                Chips(listOf("clear" to stringResource(R.string.set_glass_clear), "balanced" to stringResource(R.string.set_glass_balanced),
                                    "frosted" to stringResource(R.string.set_glass_frosted)), s.glass) { id -> app.prefs.update { it.copy(glass = id) } }
                                Body(stringResource(R.string.set_glass_text), quiet = true)
                            }

                            Section(stringResource(R.string.set_results_title)) {
                                Toggle(stringResource(R.string.set_show_settings), s.showSettings) { on -> app.prefs.update { it.copy(showSettings = on) } }
                                Toggle(stringResource(R.string.set_show_sums), s.showSums) { on -> app.prefs.update { it.copy(showSums = on) } }
                            }

                            Section(stringResource(R.string.set_privacy_title)) {
                                var cleared by remember { mutableStateOf(false) }
                                Body(stringResource(R.string.set_privacy_text))
                                OutlinedButton(onClick = { app.historyStore.clear(); cleared = true }, enabled = !cleared) {
                                    Text(stringResource(if (cleared) R.string.set_forgotten else R.string.set_forget))
                                }
                            }

                            Column(Modifier.padding(horizontal = 8.dp)) {
                                Body(stringResource(R.string.set_version, BuildConfig.VERSION_NAME), quiet = true)
                                Body(stringResource(R.string.set_about_text), quiet = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight(600)))
            content()
        }
    }
}

@Composable
private fun Body(text: String, quiet: Boolean = false) {
    Text(text, style = if (quiet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
        color = if (quiet) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight(500)))
}

@Composable
private fun Toggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(role = Role.Switch) { onChange(!on) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight(500)))
        Switch(checked = on, onCheckedChange = null)
    }
}

/** One of several, as pills: the chosen one is filled. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(options: List<Pair<String, String>>, chosen: String, onPick: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((id, name) in options) {
            val on = id == chosen
            Text(
                name,
                Modifier.clip(CircleShape).background(if (on) scheme.secondaryContainer else scheme.surfaceContainerHighest)
                    .clickable { onPick(id) }.semantics { role = Role.RadioButton; selected = on }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                color = if (on) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight(if (on) 600 else 500)),
            )
        }
    }
}

@Composable
private fun SiteRow(site: SiteEntry, onRemove: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(site.keyword, Modifier.clip(RoundedCornerShape(8.dp)).background(scheme.surfaceContainerHighest).padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = Fonts.text, fontWeight = FontWeight(600)))
        Column(Modifier.weight(1f)) {
            Text(site.name, style = MaterialTheme.typography.bodyLarge)
            Text(site.url, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1)
        }
        TextButton(onClick = onRemove) { Text(stringResource(R.string.set_site_remove, "")) }
    }
}

@Composable
private fun AddSite(taken: List<String>, onAdd: (SiteEntry) -> Unit) {
    var open by remember { mutableStateOf(false) }
    if (!open) { FilledTonalButton(onClick = { open = true }) { Text(stringResource(R.string.set_site_add)) }; return }
    var keyword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(keyword, { keyword = it.trim().take(12) }, Modifier.weight(1f), label = { Text(stringResource(R.string.set_site_keyword)) }, singleLine = true)
            OutlinedTextField(name, { name = it.take(30) }, Modifier.weight(2f), label = { Text(stringResource(R.string.set_site_name)) }, singleLine = true)
        }
        OutlinedTextField(url, { url = it.trim() }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.set_site_url)) }, singleLine = true)
        // A keyword search needs a keyword nobody else has, a name, and a web address with a place for the text.
        val ok = keyword.isNotEmpty() && ' ' !in keyword && keyword.lowercase() !in taken && name.isNotBlank() &&
            (url.startsWith("https://") || url.startsWith("http://")) && "%s" in url
        Button(onClick = { onAdd(SiteEntry(keyword, name.trim(), url)); open = false }, enabled = ok) { Text(stringResource(R.string.set_site_save)) }
    }
}
