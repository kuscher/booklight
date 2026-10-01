package io.github.kuscher.booklight.overlay

import android.os.SystemClock
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.Tips
import io.github.kuscher.booklight.ai.OnDevice
import io.github.kuscher.booklight.scopes.PromptScope
import kotlinx.coroutines.flow.drop
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
    /** The row whose other actions are listed under it, by its id; null = none. While it is, [results] is that row and its actions. */
    var opened by mutableStateOf<String?>(null); private set
    /** The list as it was before a row was opened: closing brings it back. */
    private var closed: List<Result> = emptyList()
    /** The device's own model has been asked and has not said a word yet: the panel's edge light runs while it works. */
    var thinking by mutableStateOf(false); private set
    private var answering: Job? = null
    /** The last answer and what was asked for it: the same thing is not asked twice in a row. */
    private var answered: Pair<String, Result>? = null
    /** An Enter that came while the answer was still arriving: it runs when the answer is whole. */
    private var whenAnswered: ((Result, Action) -> Unit)? = null
    /** The selection or the arming has been moved by hand since the text last changed. */
    private var touched by mutableStateOf(false)

    /**
     * The scope the whole text is a keyword of ("s", "yt"), while the selection is where typing left
     * it: Tab then makes the keyword the chip, as a browser's address bar does. Once the user has
     * moved to a row or along its actions, Tab is that row's again.
     */
    val keyword: Scope? by derivedStateOf { if (chip != null || touched || opened != null) null else app.engine.keywordScope(query) }

    /** Tab on a keyword: it becomes the chip. Only for the list on screen. */
    fun enterKeyword(): Boolean {
        val s = keyword ?: return false
        if (resultsFor != (null to query)) return false
        enterScope(s, "", query.trim())
        return true
    }

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
    /** When Booklight last filled the field itself (entered a scope from its row, typed an example). */
    private var filledAt = 0L
    /** Booklight is typing an example into the field, a letter at a time. */
    private var typist: Job? = null
    /** What is in the field was typed by Booklight and not touched since: it is nobody's "last text". */
    private var shown = false
    /** How long each letter waits for the one before it when Booklight types [letters] letters (`Motion.typeStep`). */
    var typeStep: (letters: Int) -> Long = { 0 }

    init {
        scope.launch { app.prefs.state.collect { settings = it } }
        // What the system says about its model (there, being fetched, how far) changes a prompt's rows.
        if (!demo) {
            scope.launch { app.onDevice.state.drop(1).collect { if (chip is PromptScope) refresh() } }
            scope.launch { app.onDevice.progress.drop(1).collect { if (chip is PromptScope) refresh() } }
        }
    }

    val current: Result? get() = results.getOrNull(selected)

    /** The first-run step to show, if any is left and nothing is typed. */
    val card: Card? by derivedStateOf {
        when {
            demo || query.isNotEmpty() || chip != null -> null
            // "Give Booklight a key": not for someone whose key has already opened the panel.
            settings.shortcutCard && !settings.keySeen -> Card.SHORTCUT
            settings.suggestionsCard && !settings.suggestions -> Card.SUGGESTIONS
            else -> null
        }
    }

    /** The tip that is on screen under the empty field; null = none. */
    var tip by mutableStateOf<Tips.Tip?>(null); private set
    /** Which of the tip's two answers Enter would do: 0 neither (Enter on an empty panel does nothing), 1 Try it, 2 Turn off tips. */
    var tipArmed by mutableIntStateOf(0); private set
    /** Tips were just turned off: the card says so for a moment before it goes. */
    var tipOff by mutableStateOf(false); private set
    private var tipSince = 0L
    /** Something has been typed since the panel opened: whoever types at once never sees a tip. */
    private var typedYet = false

    /**
     * The panel has been open for a moment and nothing was typed: a tip, if there is one whose turn
     * it is. Never as part of the opening, and never while a first-run card is to be shown.
     */
    fun offerTip() {
        if (demo || typedYet || tip != null || query.isNotEmpty() || chip != null || results.isNotEmpty() || !settings.tips || card != null) return
        tip = app.tips.next(settings) ?: return
        tipArmed = 0; tipOff = false
        tipSince = SystemClock.uptimeMillis()
    }

    /** The tip goes: something was typed, the panel closes. How long it was on screen counts towards its turn. */
    fun hideTip() {
        val t = tip ?: return
        tip = null; tipArmed = 0; tipOff = false
        val ms = SystemClock.uptimeMillis() - tipSince
        change { Tips.shown(it, t.id, ms) }
    }

    /** Tab on a tip: its first answer, then its second, and round again. */
    fun tipTab() { if (tip != null && !tipOff) tipArmed = if (tipArmed == 1) 2 else 1 }
    fun tipArm(which: Int) { if (tip != null && !tipOff) tipArmed = which }

    /** Enter on a tip does the armed answer; with neither armed, nothing. */
    fun tipEnter() {
        val t = tip ?: return
        when (tipArmed) {
            // Booklight types the example; the card stays until the last letter. Tried counts as its turn.
            1 -> { change { Tips.seen(it, t.id) }; tipArmed = 0; typeOut(t.example) }
            2 -> {
                change { it.copy(tips = false) }
                tipOff = true; tipArmed = 0
                scope.launch { delay(TIP_OFF_MS); if (tipOff) { tip = null; tipOff = false } }
            }
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
        typist?.cancel(); typist = null     // the user's own typing takes over from Booklight's
        shown = false
        typedYet = true
        hideTip()
        shut()                              // typing closes an opened row in the same frame
        touched = false
        foreign = false                     // edited: it is the user's own text now
        if (chip == null) {
            if (text.isEmpty()) held = null
            // A question mark first is the list of everything, at once: nothing else begins with one.
            if (text.startsWith(HELP) && held != HELP) app.engine.scope("help")?.let { enterScope(it, text.drop(1).trimStart(), HELP); return }
            // A keyword and a space at the start of the field: the keyword becomes the chip. Not if the user
            // has just turned that very keyword back into text: then it is a word ("new york weather").
            app.engine.scopeFor(text)?.takeIf { !it.word.equals(held, ignoreCase = true) }?.let { enterScope(it.scope, it.text, it.word); return }
        }
        query = text
        search()
    }

    /** Makes [s] the chip; [text] is what is already typed for it, [word] the keyword that was typed, if one was. */
    fun enterScope(s: Scope, text: String = "", word: String? = null) {
        touched = false
        chip = s
        this.word = word
        query = text
        search()
        // The list of everything has been opened: its tip need not come.
        if (!demo && s.key == "help" && "help" !in settings.used) change { it.copy(used = it.used + "help") }
        // A prompt: ask the system what it has, and have its model loaded by the time the text is typed.
        if (s is PromptScope && !demo) scope.launch { if (app.onDevice.check() == OnDevice.State.READY) app.onDevice.warm() }
    }

    /**
     * Booklight types [text] for the user to see: an example from the list of everything or from a
     * tip. From an empty field, a letter at a time; a keyword becomes its chip when its Space lands,
     * as it does under the user's own hands. The list that was showing stays until the last letter,
     * and there is one search, at the end. The Enter that asked for this does not also run what it
     * brings: only a new press, a moment later.
     */
    fun typeOut(text: String) {
        typist?.cancel()
        job?.cancel(); answering?.cancel(); answering = null; thinking = false; whenAnswered = null; whenReady = null
        cancelConfirm()
        chip = null; word = null; held = null; foreign = false; touched = false
        query = ""
        shown = true
        filledAt = SystemClock.uptimeMillis()
        val step = typeStep(text.length)
        typist = scope.launch {
            for (c in text) {
                val next = query + c
                // The same two ways into a scope the user's own typing has: a question mark first, or a keyword and its Space.
                val help = if (chip == null && next.startsWith(HELP)) app.engine.scope("help") else null
                val s = if (chip == null && help == null) app.engine.scopeFor(next) else null
                when {
                    help != null -> { chip = help; word = HELP; query = next.drop(1).trimStart() }
                    s != null -> { chip = s.scope; word = s.word; query = s.text }
                    else -> query = next
                }
                if (step > 0) delay(step)
            }
            typist = null
            filledAt = SystemClock.uptimeMillis()
            search()
        }
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
        hideTip()
        job?.cancel()
        answering?.cancel(); answering = null; thinking = false; whenAnswered = null     // typing again takes the question back
        cancelConfirm()
        if (!keep) shut()
        val text = query
        val key = chip?.key
        if (key == null && text.isBlank()) { shut(); results = emptyList(); selected = 0; armed = 0; cell = 0; resultsFor = null to text; whenReady = null; return }
        job = scope.launch(Dispatchers.Default) {
            val t0 = System.nanoTime()
            val local = app.engine.search(Query(text, key, word), limit)
            val took = (System.nanoTime() - t0) / 1000
            withContext(Dispatchers.Main.immediate) {
                val was = current?.id
                lastSearchMicros = took; resultsFor = key to text
                // A row that is open stays open over a refresh (something ran with Shift held), if it is still there.
                val parent = if (keep) local.firstOrNull { it.id == opened } else null
                if (parent != null) {
                    closed = local
                    results = listOf(parent) + actionRows(parent)
                    selected = selected.coerceIn(0, results.lastIndex)
                    return@withContext
                }
                opened = null; closed = emptyList()
                results = local
                val same = if (keep) local.indexOfFirst { it.id == was } else -1
                if (same >= 0) {
                    selected = same
                    if (armed !in stops(local[same])) armed = local[same].armed
                    cell = cell.coerceIn(0, (((local[same].body as? Body.Grid)?.cells?.size ?: 1) - 1).coerceAtLeast(0))
                } else { selected = 0; armed = local.firstOrNull()?.armed ?: 0; cell = 0 }
                ask()
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

    /**
     * A prompt's row is answered by the device's own model, in the row. Not for every letter: after
     * a pause in typing, and not for a word that has hardly begun ([now]: Enter asked for it). One
     * question at a time; the text changing takes it back.
     */
    private fun ask(now: Boolean = false) {
        answering?.cancel(); answering = null; thinking = false
        if (demo) return
        val s = chip as? PromptScope ?: return
        val row = results.firstOrNull { it.id == s.row } ?: return
        val q = (row.body as? Body.Stream)?.takeIf { !it.answer }?.ask ?: return
        answered?.let { (was, r) -> if (was == q) { put(r); return } }
        if (!now && query.isNotBlank() && query.trim().length < ASK_MIN) return
        answering = scope.launch {
            if (!now) delay(ASK_PAUSE_MS)
            thinking = true
            val text = StringBuilder()
            try {
                app.onDevice.ask(q).collect { piece ->
                    text.append(piece)
                    thinking = false
                    put(s.answered(row, text.toString().trimStart(), busy = true))
                }
            } finally { thinking = false }
            val all = text.toString().trim()
            if (all.isEmpty()) { whenAnswered = null; put(s.unanswered(row)); return@launch }
            answered = q to put(s.answered(row, all, busy = false))
            whenAnswered?.let { run -> whenAnswered = null; if (current?.id == row.id) chosen()?.let { (r, a) -> run(r, a) } }
        }
    }

    /** [row] takes the place of the row with its id. The arming stays on the action it was on, if the row still has it. */
    private fun put(row: Result): Result {
        val i = results.indexOfFirst { it.id == row.id }
        if (i < 0) return row
        val was = results[i]
        // An answer that has grown to four lines stays that tall while more of it arrives.
        val grown = (was.body as? Body.Stream)?.let { it.answer && it.tall } == true
        val next = (row.body as? Body.Stream)?.takeIf { grown && it.answer && !it.tall }?.let { row.copy(body = it.copy(tall = true)) } ?: row
        if (i == selected) {
            val id = was.actions.getOrNull(armed)?.id
            armed = next.actions.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: 0
        }
        results = results.toMutableList().also { it[i] = next }
        return next
    }

    /** The answer in row [id] needs a third line: the row grows, once, to hold four. */
    fun grow(id: String) {
        val r = results.firstOrNull { it.id == id } ?: return
        val b = r.body as? Body.Stream ?: return
        if (b.tall || !b.answer) return
        results = results.map { if (it.id == id) it.copy(body = b.copy(tall = true)) else it }
        answered?.let { (q, a) -> if (a.id == id) answered = q to a.copy(body = (a.body as? Body.Stream)?.copy(tall = true)) }
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
        touched = true
        selected = index
        // Another row: its own default again. Back on a row whose actions are listed: its arrow, which now closes them.
        armed = if (opened != null && index == 0) results[0].actions.size else results[index].armed
        cell = 0
    }

    /**
     * What the arming can rest on, on [r], in the order it is drawn: the actions shown as icons, then
     * one more stop if the row keeps others behind its arrow. That stop is the arrow itself (More:
     * the number of actions, an index no action has), unless a typed verb named one of the others
     * ("chrome top left"): then it is that action, and the arrow's place shows it.
     */
    fun stops(r: Result): List<Int> {
        val shown = r.actions.indices.filter { !r.actions[it].more }
        if (r.actions.none { it.more }) return shown
        val typed = r.armed.takeIf { r.actions.getOrNull(it)?.more == true && opened != r.id }
        return shown + (typed ?: r.actions.size)
    }

    /** The arming is on the row's arrow. */
    val onMore: Boolean get() = current?.let { armed == it.actions.size && it.actions.any { a -> a.more } } ?: false

    /** Tab and the arrows along the selected row's stops. False when there is nowhere to go. Moving the arming never opens anything. */
    fun arm(by: Int, wrap: Boolean): Boolean {
        val st = stops(current ?: return false)
        val n = st.size
        if (n < 2) return false
        val at = st.indexOf(armed).coerceAtLeast(0)
        val to = if (wrap) (at + by + n) % n else (at + by).coerceIn(0, n - 1)
        if (st[to] == armed) return false
        cancelConfirm()
        touched = true
        armed = st[to]
        return true
    }

    fun armAt(index: Int) {
        val r = current ?: return
        if (index in stops(r) && index != armed) { cancelConfirm(); touched = true; armed = index }
    }

    private fun actionRows(r: Result): List<Result> = r.actions.filter { it.more }.map { a ->
        Result(
            id = "act:${r.id}:${a.id}", provider = r.provider, kind = Kind.ACTION, title = a.label, icon = io.github.kuscher.booklight.core.Icon.Symbol(a.symbol),
            score = 1.0, actions = listOf(a.copy(more = false)), learnable = false,
        )
    }

    /**
     * Opens the selected row: its other actions become rows of their own under it, and while they
     * are there the list is that row and those rows. Only for the list on screen.
     */
    fun open(): Boolean {
        val r = current ?: return false
        if (opened != null || r.actions.none { it.more } || resultsFor != (chip?.key to query)) return false
        cancelConfirm()
        val rows = actionRows(r)
        // A typed place that was shown on the row goes back into the list: the pill lands on its row there.
        val typed = r.actions.getOrNull(r.armed)?.takeIf { it.more }?.let { a -> rows.indexOfFirst { it.actions[0].id == a.id } } ?: -1
        closed = results
        opened = r.id
        results = listOf(r) + rows
        selected = 1 + typed.coerceAtLeast(0); armed = 0; cell = 0
        return true
    }

    /** Closes the opened row: the list is back as it was, the pill on the row, its arrow armed. */
    fun close(): Boolean {
        val id = opened ?: return false
        cancelConfirm()
        val back = closed
        shut()
        selected = back.indexOfFirst { it.id == id }.coerceAtLeast(0)
        armed = back.getOrNull(selected)?.actions?.size ?: 0
        return true
    }

    /** The opened row is no longer open, whatever happens to the list next. */
    private fun shut() {
        if (opened == null) return
        opened = null
        results = closed
        closed = emptyList()
        selected = selected.coerceIn(0, (results.size - 1).coerceAtLeast(0))
    }

    /** Tab and Shift + Tab while a row is open: down and up through its actions, wrapping inside them. */
    fun step(by: Int) {
        val n = results.size - 1
        if (opened == null || n < 1) return
        cancelConfirm()
        val at = (selected - 1).coerceAtLeast(if (by > 0) -1 else n)
        selected = 1 + ((at + by) % n + n) % n
        armed = 0
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
        if (typist != null) return          // Booklight is still typing: there is nothing to run yet
        // The field was just filled in for the user (a scope entered from its row, an example typed): the Enter that
        // did it must not also run what it brought. Only a new press, a moment later, runs.
        if (SystemClock.uptimeMillis() - filledAt < CONFIRM_GAP_MS) return
        if (resultsFor != (chip?.key to query)) { whenReady = run; return }
        // On the row's arrow: Enter opens its other actions, or closes them again.
        if (onMore) { if (opened == null) open() else close(); return }
        val (r, a) = chosen() ?: return
        // A prompt's row: the model is asked now, without waiting for a pause in typing. While its answer is still
        // arriving, Enter waits for all of it.
        if (a.effect == PromptScope.ASK) { if (thinking) whenAnswered = run else ask(now = true); return }
        if ((r.body as? Body.Stream)?.busy == true) { whenAnswered = run; return }
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
        // "Try it": Booklight types the example.
        (a.effect as? Effect.Type)?.let { typeOut(it.text); return }
        run(r, a)
    }

    private fun into(e: Effect.EnterScope) {
        val from = chip
        filledAt = SystemClock.uptimeMillis()
        app.engine.scope(e.key)?.let {
            enterScope(it, e.text); if (from != null && from.keywords.isEmpty()) foreign = true
            // Another app's keyword, entered from its row: from now on its keyword and a Space enters it, like Booklight's own.
            if (!demo && !it.spaceEnters) change { s -> if (e.key in s.usedScopes) s else s.copy(usedScopes = s.usedScopes + e.key) }
        }
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
        if (a.effect == PromptScope.ASK || (r.body as? Body.Stream)?.busy == true) return     // an answer is Enter's
        (a.effect as? Effect.Type)?.let { typeOut(it.text); return }
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
        hideTip()
        // Text another app handed over is never kept, in its own chip or once it has moved into a note or a code. Nor is
        // an example Booklight typed, unless the user made it their own by editing it.
        if (query.isNotBlank() && !foreign && !shown && chip?.keywords?.isEmpty() != true) app.lastText = chip?.key to query
    }

    /** Something was run: the line of the list of everything it belongs to has been used, and is not suggested again. */
    fun used(r: Result, a: Action) {
        if (demo) return
        val id = app.guide.used(chip, r, a) ?: return
        if (id !in settings.used) change { it.copy(used = it.used + id) }
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
        /** How long typing rests before the device's own model is asked, and how much must be typed for it to be asked unasked. */
        private const val ASK_PAUSE_MS = 500L
        private const val ASK_MIN = 3
        private const val HELP = "?"
        /** How long "Tips are off…" stands before the card goes. */
        const val TIP_OFF_MS = 1600L
    }
}
