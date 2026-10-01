package io.github.kuscher.booklight.overlay

import android.os.SystemClock
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The small first-run card under the field: one step at a time, only while nothing is typed. */
enum class Card { SHORTCUT, SUGGESTIONS }

/**
 * The panel's state: the chip in the field (a scope) and the text after it, the ranked rows, which
 * row is selected and which of its actions is armed, the cell of a grid, and a confirmation that
 * is waiting. The UI only draws this; keys and clicks call in here.
 */
class OverlayModel(
    private val app: BooklightApp, private val scope: CoroutineScope, private val limit: Int = 8,
    /** The list in the Booklight window that shows what the panel does: it never asks the network and learns nothing. */
    private val demo: Boolean = false,
) {
    /** The scope the text is an argument for: the chip in the field. Null = ordinary search. */
    var chip by mutableStateOf<Scope?>(null); private set
    var query by mutableStateOf(""); private set
    var results by mutableStateOf<List<Result>>(emptyList()); private set
    var selected by mutableIntStateOf(0); private set
    /** Which action of the selected row Enter runs. */
    var armed by mutableIntStateOf(0); private set
    /** The armed action deletes something and has had its first Enter. */
    var confirming by mutableStateOf(false); private set
    /** The cell of the selected row's grid. */
    var cell by mutableIntStateOf(0); private set
    /** A word shown in the footer for a moment ("Copied"); [flashBad] when it says something went wrong. */
    var flash by mutableStateOf<String?>(null)
    var flashBad by mutableStateOf(false)
    /** How long the last local search took, for `./bl debug dump`. */
    var lastSearchMicros by mutableLongStateOf(0); private set
    var settings by mutableStateOf(app.prefs.now); private set

    /** The word that was typed to make the chip ("meeting" for the event scope); null if its row was used. */
    private var word: String? = null
    /** A keyword the user turned back into plain text: it stays plain text until the field has been emptied. */
    private var held: String? = null
    /** The chip's text came from another app (through that app's chip): it is never kept after the panel closes. */
    private var foreign = false
    private var job: Job? = null
    private var confirmJob: Job? = null
    private var confirmAt = 0L
    /** The chip and text [results] was made for: for a moment after a keystroke it is still the old list. */
    private var resultsFor: Pair<String?, String> = null to ""
    private var whenReady: ((Result, Action) -> Unit)? = null

    init {
        scope.launch { app.prefs.state.collect { settings = it } }
    }

    val current: Result? get() = results.getOrNull(selected)

    /** The first-run step to show, if any is left and nothing is typed. */
    val card: Card? by derivedStateOf {
        when {
            demo || query.isNotEmpty() || chip != null -> null
            settings.shortcutCard -> Card.SHORTCUT
            settings.suggestionsCard && !settings.suggestions -> Card.SUGGESTIONS
            else -> null
        }
    }

    /** The rest of the selected row's name, shown grey after the typed text ("chr" + "ome"). */
    val completion: String? by derivedStateOf {
        val r = current
        if (chip != null || r == null || r.answer != null || r.body != null || r.nudge != null || r.kind == Kind.WEB || r.kind == Kind.SUGGESTION || r.kind == Kind.SCOPE) null
        else Matcher.completion(query, r.title)
    }

    fun type(text: String) {
        if (text == query) return
        foreign = false                     // edited: it is the user's own text now
        if (chip == null) {
            if (text.isEmpty()) held = null
            // A keyword and a space at the start of the field: the keyword becomes the chip. Not if the user
            // has just turned that very keyword back into text: then it is a word ("new york weather").
            app.engine.scopeFor(text)?.takeIf { !it.word.equals(held, ignoreCase = true) }?.let { enterScope(it.scope, it.text, it.word); return }
        }
        query = text
        search()
    }

    /** Makes [s] the chip; [text] is what is already typed for it, [word] the keyword that was typed, if one was. */
    fun enterScope(s: Scope, text: String = "", word: String? = null) {
        chip = s
        this.word = word
        query = text
        search()
    }

    /**
     * Backspace on an empty argument, or a click on the chip: the chip turns back into text, the word that
     * was typed for it (or the scope's own first keyword). [withText]: what was typed after it comes along.
     * Text another app handed over has no keyword: its chip just goes.
     */
    fun leaveScope(withText: Boolean = false): Boolean {
        val s = chip ?: return false
        val w = word ?: s.keywords.firstOrNull()
        chip = null; word = null; foreign = false
        held = w
        query = when { w == null -> ""; withText && query.isNotEmpty() -> "$w $query"; else -> w }
        search()
        return true
    }

    private fun search(keep: Boolean = false) {
        job?.cancel()
        cancelConfirm()
        val text = query
        val key = chip?.key
        if (key == null && text.isBlank()) { results = emptyList(); selected = 0; armed = 0; cell = 0; resultsFor = null to text; whenReady = null; return }
        job = scope.launch(Dispatchers.Default) {
            val t0 = System.nanoTime()
            val local = app.engine.search(Query(text, key, word), limit)
            val took = (System.nanoTime() - t0) / 1000
            withContext(Dispatchers.Main.immediate) {
                val was = current?.id
                results = local; lastSearchMicros = took; resultsFor = key to text
                val same = if (keep) local.indexOfFirst { it.id == was } else -1
                if (same >= 0) {
                    selected = same
                    armed = armed.coerceIn(0, (local[same].actions.size - 1).coerceAtLeast(0))
                    cell = cell.coerceIn(0, (((local[same].body as? Body.Grid)?.cells?.size ?: 1) - 1).coerceAtLeast(0))
                } else { selected = 0; armed = local.firstOrNull()?.armed ?: 0; cell = 0 }
                // An Enter that came before these rows did: now it runs, by the same rules as any Enter (a scope's row enters it, a delete waits).
                whenReady?.let { run -> whenReady = null; enter(run) }
            }
            // Suggestions come from the network: after a pause in typing, never holding up the
            // list, and dropped if the text has moved on (this job is cancelled by then).
            if (demo || key != null || !settings.suggestions) return@launch
            delay(SUGGEST_PAUSE_MS)
            val more = app.suggest.fetch(text)
            if (more.isEmpty()) return@launch
            withContext(Dispatchers.Main.immediate) {
                if (query == text && chip == null && results == local) results = app.engine.merge(local, more, limit)
            }
        }
    }

    /** The same text again, keeping the row and the arming: after a level changed or something was saved. */
    fun refresh() = search(keep = true)

    fun move(by: Int) {
        if (results.isEmpty()) return
        val to = (selected + by).coerceIn(0, results.size - 1)
        if (to != selected) select(to)
    }

    fun select(index: Int) {
        if (index !in results.indices || index == selected) return
        cancelConfirm()
        selected = index
        armed = results[index].armed      // another row: its own default again
        cell = 0
    }

    /** Tab and the arrows along the selected row's actions. False when there is nowhere to go. */
    fun arm(by: Int, wrap: Boolean): Boolean {
        val n = current?.actions?.size ?: return false
        if (n < 2) return false
        val to = if (wrap) (armed + by + n) % n else (armed + by).coerceIn(0, n - 1)
        if (to == armed) return false
        cancelConfirm()
        armed = to
        return true
    }

    fun armAt(index: Int) {
        val n = current?.actions?.size ?: return
        if (index in 0 until n && index != armed) { cancelConfirm(); armed = index }
    }

    /** The arrows inside the selected row's grid. False when the row has no grid. */
    fun moveCell(dx: Int, dy: Int): Boolean {
        val g = current?.body as? Body.Grid ?: return false
        val n = minOf(g.cells.size, g.columns * 5)
        // At the start of a line Left is the caret's: the text can still be edited by key.
        if (dx < 0 && cell % g.columns == 0) return false
        val to = cell + dx + dy * g.columns
        if (to in 0 until n) { cell = to; return true }
        return dy == 0     // sideways the grid keeps the key; up or down past its edge belongs to the list
    }

    fun cellAt(index: Int) {
        val g = current?.body as? Body.Grid ?: return
        if (index in 0 until minOf(g.cells.size, g.columns * 5)) cell = index
    }

    /** What Enter would run right now. In a grid, the action is about the chosen cell. */
    fun chosen(): Pair<Result, Action>? {
        val r = current ?: return null
        val a = r.actions.getOrNull(armed) ?: r.actions.firstOrNull() ?: return null
        val g = r.body as? Body.Grid ?: return r to a
        val c = g.cells.getOrNull(cell) ?: return null
        return r to a.copy(effect = Effect.CopyText(c.glyph), done = a.done?.let { "$it ${c.glyph}" })
    }

    /**
     * Enter. Typing is faster than the list: if Enter arrives before the rows for the current text
     * have, the first of those rows runs when they land, never a row of the previous text.
     * An action that deletes waits for a second Enter, a new press a moment later.
     */
    fun enter(run: (Result, Action) -> Unit) {
        if (resultsFor != (chip?.key to query)) { whenReady = run; return }
        val (r, a) = chosen() ?: return
        if (a.confirm) {
            val now = SystemClock.uptimeMillis()
            if (!confirming) {
                confirming = true; confirmAt = now
                confirmJob = scope.launch { delay(CONFIRM_MS); confirming = false }
                return
            }
            if (now - confirmAt < CONFIRM_GAP_MS) return     // a key bounce can't confirm
            cancelConfirm()
        }
        // A scope's own row: its keyword becomes the chip.
        (a.effect as? Effect.EnterScope)?.let { into(it); return }
        run(r, a)
    }

    private fun into(e: Effect.EnterScope) {
        val from = chip
        app.engine.scope(e.key)?.let { enterScope(it, e.text); if (from != null && from.keywords.isEmpty()) foreign = true }
    }

    /**
     * Tab or Right on a scope's row: type into it. Only for the list that is on screen: if the rows for the
     * current text are still on their way, nothing happens (an arrow key must never run a row nobody has seen).
     */
    fun fill(): Boolean {
        if (resultsFor != (chip?.key to query)) return false
        val e = chosen()?.second?.effect as? Effect.EnterScope ?: return false
        into(e)
        return true
    }

    /** Ctrl + a digit: that row's first action, straight away. Never one that removes something, and only for rows on screen. */
    fun runRow(n: Int, run: (Result, Action) -> Unit) {
        if (resultsFor != (chip?.key to query)) return
        val r = results.getOrNull(n)?.takeIf { it.body !is Body.Grid } ?: return
        val a = r.actions.firstOrNull()?.takeIf { !it.danger && !it.confirm } ?: return
        (a.effect as? Effect.EnterScope)?.let { into(it); return }
        run(r, a)
    }

    /** True if a confirmation was waiting. */
    fun cancelConfirm(): Boolean {
        confirmJob?.cancel(); confirmJob = null
        return confirming.also { confirming = false }
    }

    /** Left or Right on a control row: its level changes at once and the panel stays. False when the row has none. */
    fun nudge(dir: Int): Boolean {
        val r = current ?: return false
        val n = r.nudge ?: return false
        if ((r.body as? Body.Level)?.locked == true) return true
        if (app.executor.run(if (dir < 0) n.down else n.up)) refresh()
        return true
    }

    /** Up on an empty field: the last text that was not run, chip included. */
    fun restoreLast(): Boolean {
        if (query.isNotEmpty() || chip != null) return false
        val (key, text) = app.lastText ?: return false
        app.lastText = null
        val s = key?.let { app.engine.scope(it) }
        if (s != null) enterScope(s, text) else { query = text; search() }
        return true
    }

    /** The panel is closing without having run anything: keep what was typed for [restoreLast]. */
    fun keep() {
        // Text another app handed over is never kept, in its own chip or once it has moved into a note or a code.
        if (query.isNotBlank() && !foreign && chip?.keywords?.isEmpty() != true) app.lastText = chip?.key to query
    }

    /** Remember the pick, so the same text finds it first next time. */
    fun learn(r: Result) {
        app.lastText = null
        if (chip != null) return
        app.engine.picked(Query(query), r)
        app.historyStore.changed()
    }

    fun change(f: (Settings) -> Settings) = app.prefs.update(f)

    companion object {
        private const val SUGGEST_PAUSE_MS = 140L
        /** How long a confirmation waits for its second Enter. */
        const val CONFIRM_MS = 3000L
        private const val CONFIRM_GAP_MS = 350L
    }
}
