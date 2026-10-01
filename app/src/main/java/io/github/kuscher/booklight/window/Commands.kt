package io.github.kuscher.booklight.window

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Templates
import io.github.kuscher.booklight.data.PromptEntry
import io.github.kuscher.booklight.data.RecipeEntry
import io.github.kuscher.booklight.data.Recipes
import io.github.kuscher.booklight.data.Settings
import io.github.kuscher.booklight.data.SiteEntry
import io.github.kuscher.booklight.data.SnippetEntry
import io.github.kuscher.booklight.data.StepEntry
import io.github.kuscher.booklight.overlay.LocalDark
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.overlay.SECOND
import io.github.kuscher.booklight.overlay.SMALL
import io.github.kuscher.booklight.overlay.THIRD
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * "Your commands": the links (1.0's keyword searches, now with placeholders), the snippets, the
 * recipes and the prompts the user made. A row opens its editor in place, under it; names, keywords and ordered
 * steps are form work, which a window does better than a one-line panel.
 */
@Composable
fun Commands(page: Page, app: BooklightApp, s: Settings, edit: Pair<String, String>?, onEdited: () -> Unit, onTyping: (Boolean) -> Unit) {
    /** What is being edited: its kind and its id; an empty id is a new one. */
    var open by remember { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(edit) { if (edit != null) { open = edit; page.selected = "${edit.first}:${edit.second}"; delay(1600); onEdited() } }
    // A link can't have a keyword one of Booklight's own scopes answers to: it would never be reached. Nor one a prompt has.
    val words = s.prompts.mapNotNull { it.keyword.lowercase().takeIf { k -> k.isNotEmpty() } }.toSet()
    val taken = s.sites.map { it.keyword.lowercase() }.toSet() + app.scopes.reserved + words
    fun close() { open = null; onTyping(false) }
    fun set(change: (Settings) -> Settings) = app.prefs.update(change)

    Column {
        Label(stringResource(R.string.win_links))
        for (link in s.sites) {
            val takes = Templates.takesArgument(link.url)
            PageRow(page, "quicklink:${link.keyword}", link.name, link.url, mark = { Icon(Symbols.of(if (takes) "search" else "link"), null, tint = it) }, onEnter = { open = "quicklink" to link.keyword }) {
                Cap(link.keyword, it)
            }
            Editor(open == ("quicklink" to link.keyword)) {
                LinkEditor(link, taken, onTyping,
                    onSave = { new -> set { st -> st.copy(sites = st.sites.map { if (it.keyword == link.keyword) new else it }) }; close() },
                    onDelete = { set { st -> st.copy(sites = st.sites.filter { it.keyword != link.keyword }) }; close() }, onClose = ::close)
            }
        }
        Add(page, "quicklink:", stringResource(R.string.win_link_add)) { open = "quicklink" to "" }
        Editor(open == ("quicklink" to "")) {
            LinkEditor(null, taken, onTyping, onSave = { new -> set { it.copy(sites = it.sites + new) }; close() }, onDelete = null, onClose = ::close)
        }

        Label(stringResource(R.string.snip_name))
        for (snip in s.snippets) {
            PageRow(page, "snippet:${snip.key}", snip.key, snip.text.lineSequence().first(), mark = { Icon(Symbols.of("text"), null, tint = it) }, onEnter = { open = "snippet" to snip.key })
            Editor(open == ("snippet" to snip.key)) {
                SnippetEditor(snip, s.snippets.map { it.key.lowercase() }.toSet(), onTyping,
                    onSave = { new -> set { st -> st.copy(snippets = st.snippets.map { if (it.key == snip.key) new else it }) }; close() },
                    onDelete = { set { st -> st.copy(snippets = st.snippets.filter { it.key != snip.key }) }; close() }, onClose = ::close)
            }
        }
        Add(page, "snippet:", stringResource(R.string.win_snippet_add)) { open = "snippet" to "" }
        Editor(open == ("snippet" to "")) {
            SnippetEditor(null, s.snippets.map { it.key.lowercase() }.toSet(), onTyping, onSave = { new -> set { it.copy(snippets = it.snippets + new) }; close() }, onDelete = null, onClose = ::close)
        }

        Label(stringResource(R.string.win_recipes))
        for (recipe in s.recipes) {
            PageRow(page, "recipe:${recipe.id}", recipe.name, Recipes.describe(app, recipe), mark = { Icon(Symbols.of("bolt"), null, tint = it) }, onEnter = { open = "recipe" to recipe.id }) {
                if (recipe.keyword.isNotEmpty()) Cap(recipe.keyword, it)
            }
            Editor(open == ("recipe" to recipe.id)) {
                RecipeEditor(app, recipe, onTyping,
                    onSave = { new -> set { st -> st.copy(recipes = st.recipes.map { if (it.id == recipe.id) new else it }) }; close() },
                    onDelete = { set { st -> st.copy(recipes = st.recipes.filter { it.id != recipe.id }) }; close() }, onClose = ::close)
            }
        }
        Add(page, "recipe:", stringResource(R.string.win_recipe_add)) { open = "recipe" to "" }
        Editor(open == ("recipe" to "")) {
            RecipeEditor(app, null, onTyping, onSave = { new -> set { it.copy(recipes = it.recipes + new) }; close() }, onDelete = null, onClose = ::close)
        }

        Label(stringResource(R.string.win_prompts))
        // A prompt's keyword must be free of links, recipes, Booklight's own and the other prompts.
        val used = taken + s.recipes.mapNotNull { it.keyword.lowercase().takeIf { k -> k.isNotEmpty() } }
        for (prompt in s.prompts) {
            PageRow(page, "prompt:${prompt.id}", prompt.name, prompt.text.replace("{text}", "").lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() },
                mark = { Icon(Symbols.of("spark"), null, tint = it) }, onEnter = { open = "prompt" to prompt.id }) {
                if (prompt.keyword.isNotEmpty()) Cap(prompt.keyword, it)
            }
            Editor(open == ("prompt" to prompt.id)) {
                PromptEditor(prompt, used, onTyping,
                    onSave = { new -> set { st -> st.copy(prompts = st.prompts.map { if (it.id == prompt.id) new else it }) }; close() },
                    onDelete = { set { st -> st.copy(prompts = st.prompts.filter { it.id != prompt.id }) }; close() }, onClose = ::close)
            }
        }
        Add(page, "prompt:", stringResource(R.string.win_prompt_add)) { open = "prompt" to "" }
        Editor(open == ("prompt" to "")) {
            PromptEditor(null, used, onTyping, onSave = { new -> set { it.copy(prompts = it.prompts + new) }; close() }, onDelete = null, onClose = ::close)
        }
    }
}

/** Booklight's two one-letter keywords, which a link of the user's may take, and the long keyword that then still works. */
private val LETTERS = mapOf("s" to "settings", "k" to "keys")

@Composable
private fun Label(text: String) = GroupLabel(text)

@Composable
private fun Add(page: Page, key: String, title: String, onEnter: () -> Unit) {
    PageRow(page, key, title, mark = { Icon(Symbols.of("plus"), null, tint = it) }, onEnter = onEnter)
}

/** An editor opens under its row and closes back into it: the page makes room, nothing pops over it. */
@Composable
private fun Editor(visible: Boolean, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    AnimatedVisibility(visible, enter = expandVertically(motion.place()) + fadeIn(motion.fade(140, 60)), exit = shrinkVertically(motion.place()) + fadeOut(motion.fade(80))) {
        val scheme = MaterialTheme.colorScheme
        val dark = LocalDark.current
        Column(
            Modifier.fillMaxWidth().padding(start = GUTTER, end = GUTTER, top = 4.dp, bottom = 12.dp).clip(RoundedCornerShape(24.dp))
                .background(scheme.surfaceContainerLowest.copy(alpha = if (dark) 0.36f else 0.62f))
                .border(with(LocalDensity.current) { 1f.toDp() }, Color.White.copy(alpha = if (dark) 0.20f else 0.55f), RoundedCornerShape(24.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

/** A labelled line (or a few) of text to edit. While it has the keyboard, the page's arrow keys are its own. */
@Composable
private fun Input(label: String, value: String, onChange: (String) -> Unit, onTyping: (Boolean) -> Unit, modifier: Modifier = Modifier, lines: Int = 1, hint: String = "") {
    val scheme = MaterialTheme.colorScheme
    Column(modifier) {
        Text(label, color = scheme.onSurface.copy(alpha = SECOND), style = SMALL, modifier = Modifier.padding(bottom = 4.dp))
        val style = TextStyle(fontFamily = Fonts.text, fontSize = 16.sp, color = scheme.onSurface)
        Box(Modifier.fillMaxWidth().heightIn(min = if (lines == 1) 44.dp else 96.dp).clip(RoundedCornerShape(14.dp)).background(scheme.onSurface.copy(alpha = 0.07f)).padding(horizontal = 14.dp, vertical = 11.dp)) {
            if (value.isEmpty() && hint.isNotEmpty()) Text(hint, style = style.copy(color = scheme.onSurface.copy(alpha = THIRD)))
            BasicTextField(value, onChange, Modifier.fillMaxWidth().onFocusChanged { onTyping(it.isFocused) }, singleLine = lines == 1, minLines = lines, textStyle = style, cursorBrush = SolidColor(scheme.primary))
        }
    }
}

/** A button of the editor: quiet, or the one that saves. A destructive one asks for a second press. */
@Composable
private fun Button(label: String, strong: Boolean = false, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var sure by remember { mutableStateOf(false) }
    LaunchedEffect(sure) { if (sure) { delay(3000); sure = false } }
    val ground = when { sure -> scheme.errorContainer; strong && enabled -> scheme.secondaryContainer; else -> scheme.onSurface.copy(alpha = 0.07f) }
    val ink = when { sure -> scheme.onErrorContainer; danger -> scheme.error; strong -> scheme.onSecondaryContainer; else -> scheme.onSurface }
    Text(
        if (sure) stringResource(R.string.confirm_again) else label,
        color = ink.copy(alpha = if (enabled) 1f else 0.4f), style = TextStyle(fontFamily = Fonts.text, fontSize = 14.sp, fontWeight = FontWeight(600)),
        modifier = Modifier.clip(CircleShape).background(ground)
            .clickable(enabled = enabled, role = Role.Button) { if (danger && !sure) sure = true else { sure = false; onClick() } }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun Buttons(canSave: Boolean, onSave: () -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(stringResource(R.string.action_save), strong = true, enabled = canSave, onClick = onSave)
        Button(stringResource(R.string.hint_cancel), onClick = onClose)
        if (onDelete != null) Button(stringResource(R.string.action_delete), danger = true, onClick = onDelete)
    }
}

@Composable
private fun LinkEditor(initial: SiteEntry?, taken: Set<String>, onTyping: (Boolean) -> Unit, onSave: (SiteEntry) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    var keyword by remember { mutableStateOf(initial?.keyword.orEmpty()) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var url by remember { mutableStateOf(initial?.url ?: "https://") }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Input(stringResource(R.string.set_site_keyword), keyword, { keyword = it.trim().take(16) }, onTyping, Modifier.weight(1f), hint = "jira")
        Input(stringResource(R.string.set_site_name), name, { name = it.take(40) }, onTyping, Modifier.weight(2f), hint = "Jira")
    }
    Input(stringResource(R.string.win_link_address), url, { url = it.trim() }, onTyping, hint = "https://example.com/browse/{argument}")
    Text(stringResource(R.string.win_link_help), color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECOND), style = SMALL.copy(lineHeight = 18.sp))
    // A link may take one of Booklight's two letters; the long keyword still reaches what the letter did.
    LETTERS[keyword.lowercase()]?.let { long -> Text(stringResource(R.string.win_link_letter, keyword.lowercase(), long), color = MaterialTheme.colorScheme.onSurface, style = SMALL.copy(lineHeight = 18.sp)) }
    // A link needs a keyword nobody else has, a name, and an address.
    val ok = keyword.isNotEmpty() && ' ' !in keyword && (keyword.lowercase() !in taken || keyword.equals(initial?.keyword, ignoreCase = true)) && name.isNotBlank() &&
        (url.startsWith("https://") || url.startsWith("http://")) && url.length > 10
    Buttons(ok, { onSave(SiteEntry(keyword, name.trim(), url)) }, onDelete, onClose)
}

@Composable
private fun SnippetEditor(initial: SnippetEntry?, taken: Set<String>, onTyping: (Boolean) -> Unit, onSave: (SnippetEntry) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    var key by remember { mutableStateOf(initial?.key.orEmpty()) }
    var text by remember { mutableStateOf(initial?.text.orEmpty()) }
    Input(stringResource(R.string.slot_name), key, { key = it.trim().take(24) }, onTyping, hint = "sig")
    Input(stringResource(R.string.slot_text), text, { text = it }, onTyping, lines = 3)
    val ok = key.isNotEmpty() && ' ' !in key && (key.lowercase() !in taken || key.equals(initial?.key, ignoreCase = true)) && text.isNotBlank()
    Buttons(ok, { onSave(SnippetEntry(key, text)) }, onDelete, onClose)
}

/** A prompt is a name, a keyword (it can do without: it is then found by its name) and the text the model is given. */
@Composable
private fun PromptEditor(initial: PromptEntry?, taken: Set<String>, onTyping: (Boolean) -> Unit, onSave: (PromptEntry) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var keyword by remember { mutableStateOf(initial?.keyword.orEmpty()) }
    var text by remember { mutableStateOf(initial?.text.orEmpty()) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Input(stringResource(R.string.slot_name), name, { name = it.take(28) }, onTyping, Modifier.weight(2f), hint = stringResource(R.string.win_prompt_name_hint))
        Input(stringResource(R.string.set_site_keyword), keyword, { keyword = it.trim().take(16) }, onTyping, Modifier.weight(1f), hint = "nice")
    }
    Input(stringResource(R.string.win_prompt_text), text, { text = it.take(2000) }, onTyping, lines = 4, hint = stringResource(R.string.win_prompt_text_hint))
    Text(stringResource(R.string.win_prompt_help), color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECOND), style = SMALL.copy(lineHeight = 18.sp))
    val ok = name.isNotBlank() && text.isNotBlank() && ' ' !in keyword && (keyword.isEmpty() || keyword.lowercase() !in taken || keyword.equals(initial?.keyword, ignoreCase = true))
    Buttons(ok, { onSave(PromptEntry(initial?.id ?: "p${System.currentTimeMillis()}", name.trim(), keyword, text.trim())) }, onDelete, onClose)
}

/**
 * A recipe is a name, a keyword and steps. A step is picked the way anything is picked in
 * Booklight: type into the step field and choose a row's action (an app, maybe in a place; a link;
 * a settings page; `vol 30`). Only things that are safe to do unasked can be steps.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeEditor(app: BooklightApp, initial: RecipeEntry?, onTyping: (Boolean) -> Unit, onSave: (RecipeEntry) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var keyword by remember { mutableStateOf(initial?.keyword.orEmpty()) }
    var steps by remember { mutableStateOf(initial?.steps ?: emptyList()) }
    var find by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<Result>>(emptyList()) }
    LaunchedEffect(find) {
        delay(120)
        found = if (find.isBlank()) emptyList() else withContext(Dispatchers.Default) {
            val scoped = app.engine.scopeFor(find)
            val rows = if (scoped != null) app.engine.search(Query(scoped.text, scoped.scope.key, scoped.word), 6) else app.engine.search(Query(find), 6)
            rows.filter { r -> r.actions.any { Recipes.step(it.effect, "") != null } }.take(4)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Input(stringResource(R.string.slot_name), name, { name = it.take(40) }, onTyping, Modifier.weight(2f), hint = stringResource(R.string.win_recipe_name_hint))
        Input(stringResource(R.string.set_site_keyword), keyword, { keyword = it.trim().take(16) }, onTyping, Modifier.weight(1f), hint = "work")
    }
    Text(stringResource(R.string.win_recipe_steps), color = scheme.onSurface.copy(alpha = SECOND), style = SMALL)
    steps.forEachIndexed { i, step ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Cap("${i + 1}", scheme.onSurface)
            Text(step.label.ifEmpty { step.kind }, color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.text, fontSize = 16.sp), modifier = Modifier.weight(1f))
            Button(stringResource(R.string.set_site_remove)) { steps = steps.filterIndexed { j, _ -> j != i } }
        }
    }
    Input(stringResource(R.string.win_recipe_add_step), find, { find = it }, onTyping, hint = stringResource(R.string.win_recipe_step_hint))
    for (r in found) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(r.title, color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.text, fontSize = 15.sp, fontWeight = FontWeight(500)), modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp))
            for (a in r.actions) {
                val label = "${r.title}: ${a.label}"
                val step: StepEntry = Recipes.step(a.effect, label) ?: continue
                Button(a.label) { steps = steps + step; find = "" }
            }
        }
    }
    val ok = name.isNotBlank() && steps.isNotEmpty() && ' ' !in keyword
    Buttons(ok, { onSave(RecipeEntry(initial?.id ?: "r${System.currentTimeMillis()}", name.trim(), keyword, steps)) }, onDelete, onClose)
}
