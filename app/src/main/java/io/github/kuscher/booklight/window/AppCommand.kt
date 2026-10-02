package io.github.kuscher.booklight.window

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.Executor
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Extra
import io.github.kuscher.booklight.core.ExtraKind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Problem
import io.github.kuscher.booklight.core.Read
import io.github.kuscher.booklight.core.Request
import io.github.kuscher.booklight.core.Requests
import io.github.kuscher.booklight.core.What
import io.github.kuscher.booklight.data.OwnCommandEntry
import io.github.kuscher.booklight.overlay.LocalMotion
import io.github.kuscher.booklight.providers.AppsProvider
import io.github.kuscher.booklight.ui.Symbols

/** What an app command can ask its app for, in the order of the editor's menu, each with its name. */
private val WHATS = listOf(
    What.LINK to R.string.cmd_what_link, What.SEND to R.string.cmd_what_send, What.SEARCH to R.string.cmd_what_search,
    What.PLAY to R.string.cmd_what_play, What.CUSTOM to R.string.cmd_what_custom,
)

internal fun whatName(what: What): Int = WHATS.first { it.first == what }.second

/** What keeps an app command from being saved or tried, as one line says it. */
private fun whyNot(p: Problem, what: What): Int = when (p.kind) {
    Problem.Kind.APP -> R.string.cmd_why_app
    Problem.Kind.ADDRESS -> R.string.win_why_scheme
    Problem.Kind.REFUSED -> R.string.win_why_scheme_refused
    Problem.Kind.NO_ADDRESS -> R.string.win_why_address_empty
    Problem.Kind.TEXT -> if (what == What.SEND) R.string.win_why_text else R.string.cmd_why_query
    Problem.Kind.ACTION -> R.string.cmd_why_action
    Problem.Kind.EXTRA_NAME -> R.string.cmd_why_extra_name
    Problem.Kind.EXTRA_TWICE -> R.string.cmd_why_extra_twice
    Problem.Kind.TOO_MANY -> R.string.cmd_why_extras
    else -> R.string.cmd_why_number
}

/** What was not read of a pasted line, with the flag, the word or the value it is about. */
private fun notRead(p: Problem): Pair<Int, String>? = when (p.kind) {
    Problem.Kind.NOT_A_LINE -> R.string.cmd_paste_not
    Problem.Kind.QUOTE -> R.string.cmd_paste_quote
    Problem.Kind.FLAG, Problem.Kind.FIELD -> R.string.cmd_paste_flag
    Problem.Kind.VALUE -> R.string.cmd_paste_value
    Problem.Kind.WORD -> R.string.cmd_paste_word
    Problem.Kind.COMPONENT -> R.string.cmd_paste_component
    Problem.Kind.TWO_APPS -> R.string.cmd_paste_two
    Problem.Kind.EXTRA_TYPE -> R.string.cmd_paste_extra
    Problem.Kind.UNFINISHED -> R.string.cmd_paste_end
    Problem.Kind.NUMBER -> R.string.cmd_paste_number
    Problem.Kind.YES_NO -> R.string.cmd_paste_yes_no
    Problem.Kind.TOO_MANY -> R.string.cmd_why_extras
    else -> null
}?.let { it to p.detail }

/** What the Try button says when the app was not asked: why not. */
private fun refused(r: Executor.Refusal): Int = when (r) {
    Executor.Refusal.UNREADABLE -> R.string.cmd_try_unreadable
    Executor.Refusal.GONE -> R.string.cmd_try_gone
    Executor.Refusal.ANOTHER, Executor.Refusal.NOTHING -> R.string.cmd_try_nothing
    Executor.Refusal.CLOSED -> R.string.cmd_try_closed
    Executor.Refusal.PERMISSION -> R.string.cmd_try_permission
    Executor.Refusal.FAILED -> R.string.cmd_try_failed
}

// The extras of the open editor are one field of the window's draft, like every other field: each extra's name,
// kind and value, parted by characters nobody types.
private const val PART = "\u0001"
private const val NEXT = "\u0002"
private fun pack(extras: List<Extra>): String = extras.joinToString(NEXT) { listOf(it.name, it.kind.name, it.value).joinToString(PART) }
private fun unpack(text: String): List<Extra> = if (text.isEmpty()) emptyList() else text.split(NEXT).mapNotNull { e ->
    val f = e.split(PART)
    val kind = ExtraKind.entries.firstOrNull { it.name == f.getOrNull(1) }
    if (f.size != 3 || kind == null) null else Extra(f[0], kind, f[2])
}
/** What is typed into an extra, without anything a keyboard has no key for (and so never one of the two above). */
private fun typed(text: String, most: Int) = text.filter { it >= ' ' }.take(most)

/**
 * Something of the editor ([content]), and under it a line that says what just happened with it: a line was read,
 * the app was asked. The line opens and closes like the one that says why Save is grey.
 */
@Composable
private fun Said(text: String?, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    // The sentence stays readable while its line closes.
    var last by remember { mutableStateOf(text.orEmpty()) }
    if (text != null) last = text
    Column {
        content()
        AnimatedVisibility(text != null, enter = expandVertically(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkVertically(motion.place()) + fadeOut(motion.fade(70))) {
            Help(last, strong = true, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

/**
 * An app command is a name, a keyword, an app, and what that app is asked for: to open a link, to take a text,
 * to search, to play what a search finds, or an action of the app's own; with up to eight extras. `{argument}`
 * in the link, the text or an extra is what is typed after the keyword: with it the command is a keyword that
 * takes text, without it a row that runs on Enter. It is kept as an `intent:` address (core `Requests`) and the
 * form is read back from that address.
 *
 * A line from a terminal (`am start …`) or an `intent:` address pasted into the box at the end fills in the
 * form. Try asks the app now and says what happened. Whatever the form says, Booklight starts only an activity
 * of the chosen app that is open to other apps (`Executor.open`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppCommandEditor(cs: CommandsState, app: BooklightApp, initial: OwnCommandEntry?, taken: Set<String>, onSave: (OwnCommandEntry) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val activity = LocalActivity.current as ComponentActivity
    // The installed apps, by name. (A window that opens as Booklight starts may be there before the list is.)
    val apps by produceState(app.apps.installed()) { app.apps.ready.await(); value = app.apps.installed() }
    val was = remember(initial) { initial?.let { (Requests.read(it.intent) as? Read.Ok)?.request?.copy(app = it.app) } ?: Request() }
    val wasName = initial?.name.orEmpty()
    val wasKeyword = initial?.keyword.orEmpty()
    // An app that is no longer on the device is shown by its package: the command is still its own.
    val wasLabel = remember(was, apps) { apps.firstOrNull { it.pkg == was.app }?.label ?: was.app }
    val wasExtras = remember(was) { pack(was.extras) }

    val name = cs.field("name", wasName)
    val keyword = cs.field("keyword", wasKeyword)
    val pkg = cs.field("app", was.app)
    val appName = cs.field("appName", wasLabel)
    val what = WHATS.firstOrNull { it.first.name == cs.field("what", was.what.name) }?.first ?: What.LINK
    val action = cs.field("action", was.action)
    val address = cs.field("address", was.address)
    val text = cs.field("text", was.text)
    val cls = cs.field("activity", was.activity)
    val type = cs.field("type", was.type)
    val extras = unpack(cs.field("extras", wasExtras))
    fun extras(new: List<Extra>) = cs.type("extras", pack(new), wasExtras)

    // The form as a request: only what its kind uses. An activity may be written short, from the dot on (".Main").
    val form = Requests.tidy(Request(pkg, what, action, address, text, if (cls.startsWith(".")) pkg + cls else cls, type, extras))
    val takes = Requests.takesArgument(form)
    val problem = Requests.why(form)
    // What keeps it from being saved, in the order of its fields.
    val why = when {
        name.isBlank() -> R.string.win_why_name
        ' ' in keyword -> R.string.win_why_space
        keyword.isNotEmpty() && keyword.lowercase() in taken && !keyword.equals(initial?.keyword, ignoreCase = true) -> R.string.win_why_taken
        // Text is typed after a keyword: a command that takes text cannot do without one.
        takes && keyword.isEmpty() -> R.string.cmd_why_keyword
        problem != null -> whyNot(problem, what)
        else -> null
    }
    val save = { if (why == null) onSave(OwnCommandEntry(initial?.id ?: "c${System.currentTimeMillis()}", name.trim(), keyword, form.app, Requests.write(form))) }
    /** The keys go to the field of this name: what had them (a button that was pressed) is going away, and they must not be left on nothing. */
    fun keysTo(field: String) { cs.focused = field; cs.refocus++ }
    val appLabel = stringResource(R.string.cmd_app)
    fun choose(a: AppsProvider.Installed) { cs.type("app", a.pkg, was.app); cs.type("appName", a.label, wasLabel); keysTo(appLabel) }
    // The first field of each kind: it has the keys when its kind is chosen.
    val firsts = mapOf(
        What.LINK to stringResource(R.string.win_link_address), What.SEND to stringResource(R.string.slot_text), What.SEARCH to stringResource(R.string.cmd_query),
        What.PLAY to stringResource(R.string.cmd_query), What.CUSTOM to stringResource(R.string.cmd_action),
    )
    /**
     * Another kind. One that hands over a text starts with the typed text as that text; when it is left again for one
     * that hands over none, that start goes with it (it was no change of the user's).
     */
    fun kind(to: What) {
        if (to == what) return
        cs.type("what", to.name, was.what.name)
        val texts = to != What.LINK && to != What.CUSTOM
        if (texts && text.isBlank()) cs.type("text", Requests.ARGUMENT, was.text) else if (!texts && text == Requests.ARGUMENT) cs.type("text", was.text, was.text)
        keysTo(firsts.getValue(to))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Input(stringResource(R.string.slot_name), name, { cs.type("name", it.take(40), wasName) }, Modifier.weight(2f), hint = stringResource(R.string.cmd_name_hint), first = true, onSubmit = save)
        Input(stringResource(R.string.set_site_keyword), keyword, { cs.type("keyword", it.trim().take(16), wasKeyword) }, Modifier.weight(1f), hint = "sp", onSubmit = save)
    }

    // The app, by its name: what is typed narrows the installed apps to a few, and one of them is chosen (Enter takes
    // the first). Typing on gives the choice up, unless it is the whole name of one app.
    val found = remember(appName, pkg, apps) {
        if (pkg.isNotEmpty() || appName.isBlank()) emptyList()
        else apps.map { it to Matcher.score(appName, it.label) }.filter { it.second > 0 }.sortedWith(compareByDescending<Pair<AppsProvider.Installed, Double>> { it.second }.thenBy { it.first.label.length }).take(4).map { it.first }
    }
    Input(appLabel, appName, { t ->
        cs.type("appName", t.take(60), wasLabel)
        cs.type("app", apps.singleOrNull { it.label.equals(t.trim(), ignoreCase = true) }?.pkg.orEmpty(), was.app)
    }, hint = stringResource(R.string.cmd_app_hint), onSubmit = { found.firstOrNull()?.let(::choose) ?: save() })
    if (found.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Two apps of one name are told apart by their packages.
        for (a in found) FilledTonalButton({ choose(a) }) { Text(if (found.count { it.label == a.label } > 1) "${a.label} · ${a.pkg}" else a.label, maxLines = 1) }
    }

    // What it asks for: five names are a menu. Its name stands on the fields' text edge, its button ends on the fields' edge.
    Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        var menu by remember { mutableStateOf(false) }
        Text(stringResource(R.string.cmd_what), color = scheme.onSurface, style = MaterialTheme.typography.bodyLarge, maxLines = 1, modifier = Modifier.weight(1f))
        MenuButton(WHATS.map { stringResource(it.second) }, WHATS.indexOfFirst { it.first == what }, menu, { menu = it }, stop = true) { kind(WHATS[it].first) }
    }
    // The fields of one kind give way to another's where they stand, and the editor takes the new height.
    AnimatedContent(what, transitionSpec = { (fadeIn(motion.fade(140, 60)) togetherWith fadeOut(motion.fade(70))).using(SizeTransform(clip = false) { _, _ -> motion.place() }) }, contentAlignment = Alignment.TopStart, label = "what") { shown ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (shown) {
                What.LINK -> Input(stringResource(R.string.win_link_address), address, { cs.type("address", it.trim(), was.address) }, hint = "spotify:search:{argument}", onSubmit = save)
                What.SEND -> Input(stringResource(R.string.slot_text), text, { cs.type("text", it, was.text) }, hint = Requests.ARGUMENT, onSubmit = save)
                What.SEARCH, What.PLAY -> Input(stringResource(R.string.cmd_query), text, { cs.type("text", it, was.text) }, hint = Requests.ARGUMENT, onSubmit = save)
                What.CUSTOM -> {
                    Input(stringResource(R.string.cmd_action), action, { cs.type("action", it.trim(), was.action) }, hint = "com.example.app.SHOW", onSubmit = save)
                    Input(stringResource(R.string.win_link_address), address, { cs.type("address", it.trim(), was.address) }, hint = "example://item/{argument}", onSubmit = save)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Input(stringResource(R.string.cmd_activity), cls, { cs.type("activity", it.trim(), was.activity) }, Modifier.weight(2f), hint = ".MainActivity", onSubmit = save)
                        Input(stringResource(R.string.cmd_type), type, { cs.type("type", it.trim(), was.type) }, Modifier.weight(1f), hint = "text/plain", onSubmit = save)
                    }
                }
            }
        }
    }

    // The extras: each its name, its kind (three names are buttons) with Remove at the line's end, and its value. They are
    // for a custom action; another kind shows them only when it has some (a pasted line brought them, or it was saved
    // with them): a link, a search or a text needs none, and "Add an extra" under each of them asked a question nobody had.
    val kinds = listOf(stringResource(R.string.cmd_kind_text), stringResource(R.string.cmd_kind_number), stringResource(R.string.cmd_kind_yes_no))
    val yesNo = listOf(stringResource(R.string.cmd_yes), stringResource(R.string.cmd_no))
    val add = remember { FocusRequester() }
    val fresh = stringResource(R.string.cmd_extra, extras.size + 1)
    Column {
        AnimatedVisibility(what == What.CUSTOM || extras.isNotEmpty(), enter = expandVertically(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkVertically(motion.place()) + fadeOut(motion.fade(70))) {
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).animateContentSize(motion.place()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                extras.forEachIndexed { i, e ->
                    fun set(new: Extra) = extras(extras.toMutableList().also { it[i] = new })
                    val valueLabel = stringResource(R.string.cmd_value, i + 1)
                    Input(stringResource(R.string.cmd_extra, i + 1), e.name, { set(e.copy(name = typed(it, 128))) }, hint = "android.intent.extra.SUBJECT", onSubmit = save)
                    BoxWithConstraints {
                        // This button goes with its extra. The keys are then in the name of the extra that takes its place, or of the
                        // one before it; after the last one, on the button that adds one, or (where the extras go with their last
                        // one) in the kind's first field.
                        val remove = {
                            val left = extras.size - 1
                            extras(extras.filterIndexed { j, _ -> j != i })
                            if (left > 0) keysTo(activity.getString(R.string.cmd_extra, minOf(i, left - 1) + 1)) else if (what == What.CUSTOM) runCatching { add.requestFocus() } else keysTo(firsts.getValue(what)); Unit
                        }
                        val word = stringResource(R.string.set_site_remove)
                        val room = maxWidth
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // (12 dp round each name, not 16: in the smallest window the three and Remove share 320 dp, in German too.)
                            Connected(kinds, e.kind.ordinal, pad = 12.dp, stops = true) { k ->
                                val to = ExtraKind.entries[k]
                                // A yes or no starts as yes. What was a yes or no is no text to keep: its field comes back empty, with the keys in it.
                                if (to != e.kind) set(e.copy(kind = to, value = if (to == ExtraKind.YES_NO) "true" else if (e.kind == ExtraKind.YES_NO) "" else e.value))
                                if (to != e.kind && e.kind == ExtraKind.YES_NO) keysTo(valueLabel)
                            }
                            Spacer(Modifier.weight(1f))
                            // Its word ends 16 dp inside the fields' edge, like Delete's. In the narrowest second pane (272 dp between
                            // the editor's edges) the word has no room beside the three: there it is its mark, ending on the same line,
                            // with the word as a tooltip (as New is only its plus in a narrow column).
                            if (room >= 316.dp) TextButton(remove, contentPadding = PaddingValues(horizontal = 16.dp)) { Text(word, maxLines = 1) }
                            else Tip(word) {
                                TextButton(remove, Modifier.defaultMinSize(minWidth = 1.dp).semantics { contentDescription = word }, contentPadding = PaddingValues(horizontal = 16.dp)) { Icon(Symbols.of("trash"), null, Modifier.size(20.dp)) }
                            }
                        }
                    }
                    // A yes or no stands where the value's field would, as high as the field: the extra keeps its height.
                    if (e.kind == ExtraKind.YES_NO) Box(Modifier.height(56.dp), contentAlignment = Alignment.CenterStart) {
                        Connected(yesNo, if (e.value == "false") 1 else 0, stops = true) { set(e.copy(value = if (it == 0) "true" else "false")) }
                    } else Input(valueLabel, e.value, { set(e.copy(value = typed(it, 512))) }, hint = if (e.kind == ExtraKind.NUMBER) "0" else Requests.ARGUMENT, onSubmit = save)
                }
                // A new extra has the keys in its name.
                if (extras.size < Requests.MAX_EXTRAS) FilledTonalButton({ extras(extras + Extra("", ExtraKind.TEXT, "")); keysTo(fresh) }, Modifier.focusRequester(add)) { Text(stringResource(R.string.cmd_add_extra), maxLines = 1) }
            }
        }
        Help(stringResource(R.string.cmd_help))
    }
    // Like a link, it may take one of Booklight's two letters; the long keyword still reaches what the letter did.
    LETTERS[keyword.lowercase()]?.let { long -> Help(stringResource(R.string.win_link_letter, keyword.lowercase(), long), strong = true) }

    // A line from a terminal, or an `intent:` address: read into the form when it is pasted, or on Enter when it was typed.
    val pasted = cs.field(CommandsState.PASTE, "")
    var read by remember { mutableStateOf<String?>(null) }
    fun take(line: String) {
        read = when (val r = Requests.paste(line)) {
            is Read.Bad -> notRead(r.problem)?.let { (id, detail) -> activity.getString(id, detail) } ?: activity.getString(whyNot(r.problem, what))
            is Read.Ok -> {
                val q = r.request
                val named = apps.firstOrNull { it.pkg == q.app }
                // A line that names no app leaves the one that is chosen; an app that is not here is shown, and is to be chosen.
                if (q.app.isNotEmpty()) { cs.type("app", named?.pkg.orEmpty(), was.app); cs.type("appName", named?.label ?: q.app, wasLabel) }
                cs.type("what", q.what.name, was.what.name); cs.type("action", q.action, was.action); cs.type("address", q.address, was.address)
                cs.type("text", q.text, was.text); cs.type("activity", q.activity, was.activity); cs.type("type", q.type, was.type)
                extras(q.extras)
                cs.type(CommandsState.PASTE, "", "")
                if (q.app.isNotEmpty() && named == null) activity.getString(R.string.cmd_paste_no_app, q.app) else activity.getString(R.string.cmd_paste_done)
            }
        }
    }
    Input(stringResource(R.string.cmd_paste), pasted, { new ->
        val whole = pasted.isBlank() && new.trim().length > 12     // it came all at once: pasted, not typed
        cs.type(CommandsState.PASTE, new, "")
        if (whole) take(new) else if (new.isNotBlank()) read = null
    }, hint = "am start -a … -d …", onSubmit = { if (pasted.isNotBlank()) take(pasted) })
    Said(read) { Help(stringResource(R.string.cmd_paste_help)) }

    // Try: the app is asked now, as Enter in the panel would ask it. A command that takes text is tried with a text.
    val trial = cs.field(CommandsState.TRY, "")
    var tried by remember { mutableStateOf<String?>(null) }
    val label = apps.firstOrNull { it.pkg == form.app }?.label ?: form.app
    val tryIt = {
        tried = when {
            problem != null -> activity.getString(whyNot(problem, what))
            takes && trial.isBlank() -> activity.getString(R.string.cmd_try_text)
            else -> when (val intent = Requests.fill(Requests.write(form), trial.trim())) {
                // The text does not fit the command: a number is wanted.
                null -> activity.getString(R.string.cmd_try_number)
                else -> app.executor.tryOpen(Effect.Open(form.app, intent), activity)?.let { activity.getString(refused(it), label) } ?: activity.getString(R.string.cmd_try_sent, label)
            }
        }
    }
    Said(tried) {
        Column {
            // The text to try it with has its field only while the command takes text: the field opens over the button as `{argument}` is typed.
            AnimatedVisibility(takes, enter = expandVertically(motion.place()) + fadeIn(motion.fade(140)), exit = shrinkVertically(motion.place()) + fadeOut(motion.fade(70))) {
                Input(stringResource(R.string.cmd_try_with), trial, { cs.type(CommandsState.TRY, it, "") }, Modifier.padding(bottom = 12.dp), hint = "daft punk", onSubmit = tryIt)
            }
            FilledTonalButton(tryIt) { Text(stringResource(R.string.win_try), maxLines = 1) }
        }
    }

    Buttons(cs, why, save, onDelete, onClose)
}
