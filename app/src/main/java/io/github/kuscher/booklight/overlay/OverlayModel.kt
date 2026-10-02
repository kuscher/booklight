package io.github.kuscher.booklight.overlay

import android.os.SystemClock
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.Tips
import io.github.kuscher.booklight.ai.OnDevice
import io.github.kuscher.booklight.core.Zero
import io.github.kuscher.booklight.core.Under
import io.github.kuscher.booklight.core.Clip
import io.github.kuscher.booklight.core.Offer
import io.github.kuscher.booklight.device.Clipboard
import io.github.kuscher.booklight.providers.FlightsProvider
import io.github.kuscher.booklight.providers.Songs
import io.github.kuscher.booklight.providers.WebProvider
import io.github.kuscher.booklight.scopes.Answering
import io.github.kuscher.booklight.scopes.PromptScope
import io.github.kuscher.booklight.scopes.TextFrom
import io.github.kuscher.booklight.scopes.TextScope
import kotlinx.coroutines.flow.drop
import io.github.kuscher.booklight.core.Act
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.AppChip
import io.github.kuscher.booklight.core.Behind
import io.github.kuscher.booklight.core.Chips
import io.github.kuscher.booklight.core.Origin
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.Stops
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
    /** Under an app's chip: which of the app's two actions the text is for, Search or Play. Null under any other chip, and with none. */
    var act by mutableStateOf<Act?>(null); private set
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
    /** Which of that row's two lists is open: the one behind its arrow, or the one behind Window. Never both, and never one inside the other. */
    var list by mutableStateOf<Behind?>(null); private set
    /** The list as it was before a row was opened: closing brings it back. */
    private var closed: List<Result> = emptyList()
    /** The device's own model has been asked and has not said a word yet: the panel's edge light runs while it works. */
    var thinking by mutableStateOf(false); private set
    /** A flight's lookup is taking its time: the same light runs, but it is not the model's work (an Ask on another row is still taken). */
    var looked by mutableStateOf(false); private set
    /** Something is being worked on for the list: the panel's edge light runs. */
    val working: Boolean get() = thinking || looked
    /** Debug builds: the light of the model at work, without the model (`./bl debug think on|off`), to watch it go round. */
    fun pretendThinking(on: Boolean) { thinking = on }
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
        if (!onScreen) return false
        enterScope(s, "", query.trim())
        if (!demo && !s.spaceEnters) change { st -> if (s.key in st.usedScopes) st else st.copy(usedScopes = st.usedScopes + s.key) }
        return true
    }

    /** The word that was typed to make the chip ("meeting" for the event scope); null if its row was used. */
    private var word: String? = null
    /** The keyword's own scope, where the chip is an app's that a keyword is a short way into (`play`, `yt`); null for a chip entered as itself. */
    private var via: Scope? = null
    /** Where Enter was pressed to make an app the chip: Backspace on the empty field is one step back to exactly there. */
    private var origin: Origin? = null
    /** That step back is on its way: the list for the text as it was typed is being made, and the row and the action are put back when it lands. */
    private var restoring: Origin? = null
    /** A keyword the user turned back into plain text: it stays plain text until the field has been emptied. */
    private var held: String? = null
    /** The chip's text came from another app (through that app's chip): it is never kept after the panel closes. */
    private var foreign = false
    private var job: Job? = null
    private var confirmJob: Job? = null
    private var confirmAt = 0L
    /** What a list was made for: the chip, the text, and under an app's chip the action that was armed. */
    private data class For(val key: String?, val text: String, val act: Act? = null)
    /** What [results] was made for: for a moment after a keystroke it is still the old list. */
    private var resultsFor = For(null, "")
    /** The rows on screen are the rows for what the field holds now. A key that runs or opens something acts only then. */
    private val onScreen: Boolean get() = resultsFor == For(chip?.key, query, act)
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
            scope.launch {
                var was = app.onDevice.state.value
                app.onDevice.state.drop(1).collect { now ->
                    // Rows that offered "Ask" on trust (the system had not been asked yet) keep it when its answer is no: an
                    // Enter then says so in the row. Turned into a hand-over under the selection, an Enter already on its way
                    // would open another app.
                    val trusted = was == OnDevice.State.UNKNOWN && now != OnDevice.State.READY
                    was = now
                    // (A prompt typed by its keyword never offers Ask on trust: its rows always follow the state.)
                    if (chip is Answering && asked == null && (chip is PromptScope || !trusted)) refresh()
                }
            }
            scope.launch { app.onDevice.progress.drop(1).collect { if (chip is PromptScope) refresh() } }
            // "Your usual": worked out once for this opening, off the main thread. With the switch off none of it runs.
            if (settings.zero) scope.launch { usual = app.usual() }
        }
    }

    val current: Result? get() = results.getOrNull(selected)

    /** The glass has opened far enough for what is under the field to come (`Motion.GATE`): set by the panel. */
    var arrived by mutableStateOf(false)
    /** The panel was opened to have an example typed into it (a row of the window's Commands page): no card and no tip come first. */
    var guided by mutableStateOf(false)

    /** The first-run step to show, if any is left and nothing is typed. */
    val card: Card? by derivedStateOf {
        when {
            demo || guided || query.isNotEmpty() || chip != null -> null
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
        // The copy's line has the place: the tip keeps its turn for another opening.
        if (demo || guided || typedYet || tip != null || copy != null || query.isNotEmpty() || chip != null || results.isNotEmpty() || !settings.tips || card != null) return
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

    // ---- "your usual": the rows under the empty field

    /** The usual rows for this opening; null while they are being worked out, and with the switch off. */
    private var usual by mutableStateOf<BooklightApp.Usual?>(null)
    /** The window has the focus: only then does the clipboard say whether a copy is fresh. Set by the activity. */
    var focused by mutableStateOf(false)
    /** The usual rows are the list (with or without one of them opened). False from the frame a typed list lands. */
    var zeroUp by mutableStateOf(false); private set
    /** They stood in this opening: they stand again whenever the field is empty again. */
    private var zeroStood = false
    /** The last moment for them has passed: they do not come in this opening. */
    private var zeroOver = false
    /** What the user said "Don't suggest" for in this opening (the settings take a moment to say so). */
    private val unsuggested = HashSet<String>()
    /** A word for the footer; set by the activity. */
    var onSay: (String) -> Unit = {}
    /** A sentence for a screen reader, where what changed cannot be seen from what it reads (which action is armed); set by the activity. */
    var onTell: (String) -> Unit = {}
    /** How many usual rows there are for this opening, or null while that is not known: the panel waits for it. */
    val zeroSeats: Int? get() = usual?.rows?.size

    private fun usualRows(): List<Result> = usual?.rows.orEmpty().filter { it.id !in unsuggested }

    /**
     * The usual rows may come: at the gate if it is already known that no copy is fresh, else at
     * the [last] moment (with the copy's line and the tip), and never after it. True if they stand.
     * Which of the things under the empty field has the place is [Under.choose]'s to say.
     */
    fun offerZero(last: Boolean = false): Boolean {
        if (zeroUp) return true
        if (zeroOver || demo || !settings.zero) return false      // with the switch off nothing here runs, not even the look
        if (last) zeroOver = true
        val untouched = !guided && !typedYet && query.isEmpty() && chip == null && results.isEmpty() && tip == null && copy == null && opened == null
        // Whether a copy is fresh: no, with its line switched off; not known, while the window has no focus yet.
        val fresh: Boolean? = if (!settings.copyRow) false else if (!focused) null else fresh() != null
        if (Under.choose(untouched, card != null, fresh, settings.zero, zeroSeats, tip = false, last) != Under.What.USUAL) return false
        val u = usual ?: return false
        results = usualRows(); selected = -1; armed = 0; cell = 0
        zeroUp = true; zeroStood = true
        resultsFor = For(null, query); whenReady = null
        // The holders of the first two seats are kept as shown. Not while one of them is only away (its app is being
        // updated, a list is still read): unless it has been away for a while, and then its seat is given up after all.
        val now = System.currentTimeMillis()
        val shown = results.take(2).map { it.id }
        when {
            u.held != null -> if (u.held != settings.zeroHeld || settings.zeroAway != 0L) change { it.copy(zeroHeld = u.held, zeroAway = 0L) }
            settings.zeroAway == 0L || now < settings.zeroAway -> change { it.copy(zeroAway = now) }
            now - settings.zeroAway > AWAY_MS -> change { it.copy(zeroHeld = shown, zeroAway = 0L) }
        }
        return true
    }

    /** Down. On the copy's line it opens the copy; from the usual rows at rest it brings the highlight to row one. */
    fun down(again: Boolean) {
        if (copy != null) { if (!again) openCopy(); return }
        if (moveCell(0, 1)) return
        move(1)       // (from rest, where nothing is selected, that is row one)
    }

    /**
     * Up. In the usual rows: the row above; from row one back to rest, where nothing is selected;
     * from rest, the last text that was not run. The last two each need a press of their own, so a
     * held Up stops on row one. Everywhere else as it was: the last text on an empty field, else the row above.
     */
    fun up(again: Boolean) {
        if (moveCell(0, -1)) return
        if (zeroUp) {
            when {
                selected > 0 -> move(-1)
                selected == 0 && opened == null -> if (!again) { moved(); selected = -1; armed = 0; cell = 0 }
                selected < 0 -> if (!again) restoreLast()
            }
            return
        }
        if (!restoreLast()) move(-1)
    }

    /** "Don't suggest": from the next opening on the next candidate has the seat. In the usual rows the row goes at once, and the panel is back at rest. */
    private fun unsuggest(id: String) {
        unsuggested += id
        change { if (id in it.zeroHidden) it else it.copy(zeroHidden = it.zeroHidden + id, zeroHeld = it.zeroHeld - id) }
        if (zeroUp) {
            cancelConfirm()
            opened = null; list = null; closed = emptyList()      // an opened list goes with its row, in the same change
            results = usualRows(); selected = -1; armed = 0; cell = 0
            zeroUp = results.isNotEmpty()
            resultsFor = For(null, query)
        }
        onSay(app.getString(R.string.done_unsuggested))
    }

    /**
     * The line under the empty field for something just copied ("Copied 20 s ago · a link and a
     * date"); null = none. It is not a row: nothing is selected, and Enter does nothing. Tab or
     * Down opens it ([openCopy]).
     */
    var copy by mutableStateOf<Offer?>(null); private set
    /** Typing put the line away in this opening: it stays away (it would flicker while you edit). Only leaving its own chip brings it back. */
    private var copyGone = false
    private var copyAge: io.github.kuscher.booklight.core.Age? = null
    private var copyJob: Job? = null

    /**
     * The panel has opened and has the focus: a look at what was copied, by its description alone
     * (the content is not read, and the system shows no "pasted" message). True if the line is
     * there now. While the system is still looking at a copy just made, Booklight looks again a
     * few times: only the line's words change then, never its place.
     */
    fun offerCopy(back: Boolean = false): Boolean {
        // (Whoever types at once gets no line. Coming back out of the copy's own chip is not that: the line is where it was.)
        if (demo || guided || copyGone || (typedYet && !back) || tip != null || query.isNotEmpty() || chip != null || results.isNotEmpty() || card != null) return false
        val offer = fresh() ?: return false
        // The age is said once in an opening: the line that comes back says what it said.
        copy = offer.copy(age = copyAge ?: offer.age)
        copyAge = copy?.age
        // The rows behind Tab say whether the device's model answers them: asked now, so they are right from their first frame.
        scope.launch { if (app.onDevice.check() == OnDevice.State.READY) app.onDevice.warm() }
        copyJob?.cancel()
        if (offer.looking) copyJob = scope.launch {
            repeat(COPY_LOOKS) {
                delay(COPY_LOOK_MS)
                val shown = copy ?: return@launch
                val next = fresh() ?: return@launch
                copy = next.copy(age = shown.age)
                if (!next.looking) return@launch
            }
        }
        return true
    }

    /** What the system says of the copy now, if it is one to offer. [always]: asked for by Tab, so the switch does not matter. */
    private fun fresh(always: Boolean = false): Offer? = Clipboard.look(app)?.let { Clip.offer(it, always || settings.copyRow) }

    private fun hideCopy(gone: Boolean) {
        copyJob?.cancel(); copyJob = null
        if (copy != null && gone) copyGone = true
        copy = null
    }

    /** Nothing is typed and nothing stands under the field (or only the copy's line). */
    val bare: Boolean get() = chip == null && query.isEmpty() && results.isEmpty() && card == null && tip == null

    /**
     * Tab on the empty field: a fresh copy opens, whether its line is there or not: before it has
     * come, after typing put it away, or with the line switched off.
     */
    fun tabCopy(): Boolean = (copy != null || fresh(always = true) != null) && openCopy()

    /** Tab, Down or a click on the line: the copy is read (the system says so, once) and becomes the chip, with what can be done with it under it. */
    fun openCopy(): Boolean {
        val text = Clipboard.text(app)
        hideCopy(gone = false)
        if (text == null) return false
        // The Tab that opened it does not also run a row; a new press may, as soon as the rows can be read.
        filledAt = SystemClock.uptimeMillis() - (CONFIRM_GAP_MS - COPY_GUARD_MS)
        enterScope(app.scopes.receive(text, copied = true))
        foreign = true                             // what was copied is not kept as "the last text", and nothing typed under it goes out for suggestions
        return true
    }

    /** The rest of the selected row's name, shown grey after the typed text ("chr" + "ome"). */
    val completion: String? by derivedStateOf {
        val r = current
        // (A flight's row is named "LH 455 · Lufthansa": that is what the number is, not the rest of what was typed.)
        if (chip != null || r == null || r.answer != null || r.body != null || r.nudge != null || r.kind == Kind.WEB || r.kind == Kind.SUGGESTION || r.kind == Kind.SCOPE || r.provider == FlightsProvider.ID) null
        else Matcher.completion(query, r.title)
    }

    fun type(text: String) {
        if (text == query) return
        typist?.cancel(); typist = null     // the user's own typing takes over from Booklight's
        shown = false
        typedYet = true
        hideTip()
        hideCopy(gone = true)               // the line goes with the first letter, in the same frame
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
        } else follow(text)
        query = text
        search()
    }

    /**
     * Makes [s] the chip; [text] is what is already typed for it, [word] the keyword that was typed, if one was.
     * A keyword that is a short way into an app's chip (`play`, `yt`) makes that app the chip. [act]: for an
     * app's chip, which of its two actions is armed there; with none asked for, the one the keyword stands
     * for, else the app's first.
     */
    fun enterScope(s: Scope, text: String = "", word: String? = null, act: Act? = null) {
        touched = false
        aim(s, text, word, act)
        query = text
        search()
        chip?.let(::entered)
    }

    /** [s] becomes the chip, or the app's chip it is a short way into, with the action armed that was asked for if the app has it. */
    private fun aim(s: Scope, text: String, word: String?, asked: Act?) {
        // The chip that is there, again (an answer put into the field, its other action entered): how it was entered stays as it was.
        val same = chip != null && chip?.key == s.key && word == null
        val door = if (same) null else s.door(text)
        val to = door?.chip ?: s
        if (!same) {
            via = s.takeIf { door != null }
            this.word = word ?: via?.keywords?.firstOrNull()
            origin = null
        }
        chip = to
        act = (to as? AppChip)?.let { Chips.enter(it.acts, asked ?: door?.act ?: act.takeIf { same }) }
    }

    /**
     * Under an app's chip that a keyword led to, the chip follows what the text says: `play … on youtube
     * music` is YouTube Music's chip, and without those words it is the app played in last again. The
     * text stays as it was typed, and the action that is armed stays armed where the app has it.
     */
    private fun follow(text: String) {
        val to = via?.door(text) ?: return
        if (to.chip.key == chip?.key) return
        chip = to.chip
        act = Chips.enter(to.chip.acts, act ?: to.act)
    }

    /** [s] has just become the chip, by whichever way (typed, its row, Tab, Booklight typing an example). */
    private fun entered(s: Scope) {
        if (demo) return
        // The list of everything has been opened: its tip need not come.
        if (s.key == "help" && "help" !in settings.used) change { it.copy(used = it.used + "help") }
        // A prompt: ask the system what it has, and have its model loaded by the time the text is typed. (An app's chip has
        // a question for the model only behind Play's arrow.)
        if (s is Answering && (s !is AppChip || Act.PLAY in s.acts)) scope.launch { if (app.onDevice.check() == OnDevice.State.READY && s.eager) app.onDevice.warm() }
        // An app's chip shows only the app: a screen reader is told which action is armed, and what to type.
        if (s is AppChip) act?.let { onTell(app.getString(R.string.a11y_chip, s.name, label(it), wanted(s, it))) }
    }

    private fun label(a: Act) = app.getString(if (a == Act.SEARCH) R.string.action_search else R.string.action_play)
    /** What to type for [a] under [c], for a screen reader. */
    private fun wanted(c: AppChip, a: Act) = if (a == Act.SEARCH) app.getString(R.string.a11y_type_search) else c.hint(a)

    /** The field's placeholder under a chip: what the scope takes; under an app's chip, what to type for the action that is armed. */
    val hint: String? get() = (chip as? AppChip)?.let { c -> act?.let(c::hint) } ?: chip?.hint

    /** The chip was made by a typed keyword: it comes from where the keyword stood. */
    val chipTyped: Boolean get() = word != null

    /** For `./bl debug dump`: the keyword the chip was entered by, where a keyword is a short way into an app's chip. */
    val chipVia: String? get() = via?.key

    /**
     * The line under the empty field of an app's chip: the app's other action, which Tab changes to.
     * Null for an app that has only one, and once something is typed.
     */
    val otherAct: Act? get() = (chip as? AppChip)?.takeIf { query.isBlank() && results.isEmpty() }?.let { Chips.other(it.acts, act) }

    /** Tab on the empty field under an app's chip: its other action is armed, and the placeholder says what to type for it. Nothing is sent by this. */
    fun swap(): Boolean {
        val c = chip as? AppChip ?: return false
        val to = otherAct ?: return false
        act = to
        resultsFor = For(c.key, query, to)
        if (!demo) onTell(app.getString(R.string.a11y_act, label(to), wanted(c, to)))
        return true
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
        chip = null; word = null; via = null; origin = null; act = null; held = null; foreign = false; touched = false
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
                    help != null -> { chip = help; word = HELP; query = next.drop(1).trimStart(); entered(help) }
                    s != null -> { aim(s.scope, s.text, s.word, null); query = s.text; chip?.let(::entered) }
                    else -> { if (chip != null) follow(next); query = next }
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
     * Text another app handed over has no keyword: its chip just goes. An app's chip has none either. Entered
     * from a row (Search or Play on the app's row), Backspace goes back to exactly where Enter was pressed
     * ([restore]). Else, and on a click with its text, it has the app's name: that comes back, and with the
     * text it is the typed sentence ("spotify daft punk").
     */
    fun leaveScope(withText: Boolean = false): Boolean {
        val s = chip ?: return false
        origin?.takeIf { !withText || query.isEmpty() }?.let { return restore(it) }
        val w = word ?: s.keywords.firstOrNull() ?: s.name.lowercase().takeIf { s is AppChip }
        // Text another app handed over stays that app's when it comes back into the field: not kept, not sent for suggestions.
        val others = foreign && withText && query.isNotEmpty()
        chip = null; word = null; via = null; origin = null; act = null; foreign = others
        if (s is TextScope && w == null) app.scopes.forget()
        held = w
        query = when {
            // No keyword to go back to: a prompt of the user's keeps what was typed for it; the chip that was another app's text just goes.
            w == null -> if (withText && s is PromptScope) query else ""
            withText && query.isNotEmpty() -> "$w $query"
            else -> w
        }
        search()
        // Back out of the copy's own chip, with nothing typed: the line it was opened from is there again, if the copy is still fresh.
        if (w == null && query.isEmpty() && s is TextScope && s.from == TextFrom.COPY) offerCopy(back = true)
        return true
    }

    /**
     * One step back out of an app's chip that was entered from a row: the letters as they were typed
     * ("spo", not the app's whole name), that row selected, and the action that was left still armed.
     * A wrong Enter costs one key, and the app's other action is one Tab away. The row and the action
     * are put back when the list for those letters lands (core `Origin`).
     */
    private fun restore(o: Origin): Boolean {
        // (The action that is left: the one armed under the chip now, which Tab may have changed since Enter.)
        val left = act?.id ?: o.action
        chip = null; word = null; via = null; origin = null; act = null; foreign = false
        query = o.text
        restoring = o.copy(action = left)
        search()
        return true
    }

    /**
     * Backspace on the empty field: one step back. From an answer that stands in a thing's row to the
     * rows it was asked from; from those rows out of the chip.
     */
    fun back(): Boolean {
        val rows = asked ?: return leaveScope()
        answering?.cancel(); answering = null; thinking = false; whenAnswered = null
        val id = current?.id
        asked = null
        results = rows
        selected = rows.indexOfFirst { it.id == id }.coerceAtLeast(0)
        armed = rows.getOrNull(selected)?.armed ?: 0
        // A row that stood waiting for a lookup when the model was asked: the lookup was given up then, so the list is made anew.
        if (rows.any { r -> r.actions.any { needsAnswer(it.effect) } }) refresh()
        return true
    }

    private fun search(keep: Boolean = false) {
        hideTip()
        hideCopy(gone = false)
        asked = null
        job?.cancel()
        answering?.cancel(); answering = null; thinking = false; whenAnswered = null     // typing again takes the question back
        looking?.cancel(); looking = null; whenLooked = null                             // and a flight's lookup that has not been sent yet
        cancelConfirm()
        if (!keep) shut()
        val text = query
        val key = chip?.key
        val by = word
        val armedAct = act
        // A step back out of an app's chip is for this list only: any other list that is made forgets it.
        val back = restoring
        restoring = null
        if (key == null && text.isBlank()) {
            // The empty field again: the usual rows, if they stood in this opening; else nothing. After something ran with
            // Shift held ([keep]) the highlight stays on its row.
            val was = if (keep) opened ?: current?.id else null      // (an opened list closes: the highlight stays on its row)
            shut()
            results = if (zeroStood) usualRows() else emptyList()
            zeroUp = results.isNotEmpty()
            selected = if (zeroUp) results.indexOfFirst { it.id == was } else 0
            armed = current?.armed ?: 0; cell = 0; resultsFor = For(null, text); whenReady = null
            // Back out of an app's chip that was entered from one of the usual rows: that row again, the action that was left armed.
            back?.takeIf { zeroUp }?.place(results, ::stops)?.let { (row, action) -> selected = row; armed = action }
            return
        }
        job = scope.launch(Dispatchers.Default) {
            val t0 = System.nanoTime()
            // A thing that had one of the usual seats in this opening keeps "Don't suggest" in every list of it: a row that
            // stays when a letter is typed then changes nothing.
            val seated = usual?.rows?.takeIf { zeroStood }.orEmpty().mapTo(HashSet()) { it.id }
            val local = app.engine.search(Query(text, key, by, armedAct), limit).let { rows ->
                if (seated.isEmpty()) rows else rows.map { if (it.id in seated && it.id !in unsuggested) Zero.offer(it, app.getString(R.string.action_unsuggest)) else it }
            }
            val took = (System.nanoTime() - t0) / 1000
            withContext(Dispatchers.Main.immediate) {
                val was = current?.id
                lastSearchMicros = took; resultsFor = For(key, text, armedAct)
                // A row that is open stays open over a refresh (something ran with Shift held), if it is still there.
                val parent = if (keep) local.firstOrNull { it.id == opened } else null
                if (parent != null) {
                    zeroUp = false
                    closed = local
                    results = listOf(parent) + actionRows(parent, list ?: Behind.ARROW)
                    selected = selected.coerceIn(0, results.lastIndex)
                    look()
                    return@withContext
                }
                opened = null; list = null; closed = emptyList()
                zeroUp = false
                results = local
                val same = if (keep) local.indexOfFirst { it.id == was } else -1
                if (same >= 0) {
                    selected = same
                    if (armed !in stops(local[same])) armed = local[same].armed
                    cell = cell.coerceIn(0, (((local[same].body as? Body.Grid)?.cells?.size ?: 1) - 1).coerceAtLeast(0))
                } else { selected = 0; armed = local.firstOrNull()?.armed ?: 0; cell = 0 }
                // Back out of an app's chip: the row Enter was pressed on, and the action that was left, still armed.
                back?.takeIf { key == null && it.text == text }?.place(local, ::stops)?.let { (row, action) -> selected = row.coerceAtLeast(0); armed = action; touched = true }
                ask()
                look()
                // An Enter that came before these rows did: now it runs, by the same rules as any Enter (a scope's row enters it, a delete waits).
                whenReady?.let { run -> whenReady = null; enter(run) }
            }
            // Suggestions come from the network: after a pause in typing, never holding up the
            // list, and dropped if the text has moved on (this job is cancelled by then).
            if (demo || key != null || foreign || !settings.suggestions) return@launch
            // What is meant for one app is not sent to the search engine either: an address of an app's own, or an app's name
            // and what to look for in it ("spotify daft punk") once that app leads for words after its name. While the web
            // leads, such a text is an ordinary web search, and its suggestions come as for any other.
            if (local.any { app.engine.leads(it) || it.id == WebProvider.IN_APP }) return@launch
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
        val s = chip as? Answering ?: return
        val row = results.firstOrNull { it.id == s.row } ?: return
        val q = (row.body as? Body.Stream)?.takeIf { !it.answer }?.ask ?: return
        answered?.let { (was, r) -> if (was == q && r.id == row.id) { put(r); return } }
        if (!now && query.isNotBlank() && query.trim().length < ASK_MIN) return
        stream(s, row, q, wait = !now)
    }

    /** A flight's row is being looked up; and an Enter on one of its actions that needs the answer (the action's id), which runs when it is in. */
    private var looking: Job? = null
    /** The id of the row that lookup is for. */
    private var lookingFor: String? = null
    private var whenLooked: Pair<String, (Result, Action) -> Unit>? = null

    /**
     * The pill or the arming has moved: an Enter that waits for an answer was for where they stood, and is given up.
     * (Else it would run when the answer lands: Enter on "Play in Spotify", Tab to another player, and Spotify plays.)
     */
    private fun moved() { cancelConfirm(); whenLooked = null }

    // What Spotify has for Play is looked up the same way: the row waits, the answer lands in it, an Enter that came first runs then.
    private fun waits(r: Result) = app.flights.waits(r) || app.songs.waits(r)
    /** [r] waits and may be looked up without an Enter (after a pause in typing, or when the user goes to it). */
    private fun asks(r: Result) = app.flights.waits(r) || app.songs.asks(r)
    /** A lookup for [r] is on its way. */
    private fun lookedUp(r: Result) = looking?.isActive == true && lookingFor == r.id
    private fun needsAnswer(e: Effect) = e == FlightsProvider.WAIT || e == Songs.WAIT
    private suspend fun answer(row: Result, pause: Boolean): Result? = if (row.provider == Songs.PROVIDER) app.songs.answer(row.id, pause) else app.flights.answer(row.id, pause)

    /** The list has landed: a flight's row that stands waiting for its answer is looked up, once, after a pause in typing. */
    private fun look() {
        // Text another app handed over is never sent unasked: not in its own chip (a chip without a keyword), not once it has
        // moved into the field. Under a text's own chip (what was copied, `clip`, handed-over text) a flight's row waits only
        // once the user has gone to it: then it is looked up again when the list is made anew.
        // (An app's chip has no keyword either, but what is typed under it is the user's own.)
        if (demo || (chip !is TextScope && (foreign || (chip?.keywords?.isEmpty() == true && chip !is AppChip)))) return
        // A flight's row wherever it stands. The row that plays only as row one, and only where it may be asked unasked: under
        // an app's own row ("play store") the text is that app's name, and is sent nowhere unless the user asks for it. The
        // row is there only while Play is armed on an app's chip: under Search nothing is looked up.
        (results.firstOrNull { app.flights.waits(it) } ?: results.firstOrNull()?.takeIf { app.songs.asks(it) })?.let { lookUp(it, pause = true) }
    }

    /**
     * The user went to the selected row by key (or by a click). If it is the plain row of text that
     * only looks like a flight number, it becomes a flight's row now and is looked up at once: nothing
     * was sent for it before.
     */
    private fun went() {
        // Only for the list on screen: for a moment after a keystroke the rows are still the previous text's, and a key that
        // lands in that moment must not look up what is no longer in the field. (The list that comes is looked at by [look].)
        if (demo || !onScreen) return
        val on = current ?: return
        // (Or a row that said "No connection" or "No answer this time" a while ago: going to it asks again.)
        val row = (app.flights.gone(on) ?: app.flights.retry(on))?.also { land(it) } ?: on
        // (Also a flight's row that waits and has not been asked for: one under text another app handed over. And the row of
        // another music app under `play`: what Spotify has is looked up when the user goes to it.)
        if (asks(row) && !lookedUp(row)) lookUp(row, pause = false)
    }

    private fun lookUp(row: Result, pause: Boolean) {
        looking?.cancel()
        lookingFor = row.id
        looking = scope.launch {
            // The light of work only for an answer that is slow to come: a quick one must not flash.
            var lit = false
            val slow = launch { delay((if (pause) LOOK_PAUSE_MS else 0L) + LOOK_LIGHT_MS); lit = true; looked = true }
            val got = try { answer(row, pause) } finally { slow.cancel(); if (lit) looked = false }
            if (got == null) return@launch
            land(got)
            // What Spotify found is said once to a screen reader: the song, who it is by, and what Enter does with it.
            if (got.provider == Songs.PROVIDER && current?.id == got.id) got.actions.firstOrNull { it.id == Act.PLAY.id && !it.off && !needsAnswer(it.effect) }
                ?.let { onTell(app.getString(R.string.a11y_found, got.title, got.subtitle.orEmpty(), it.label)) }
            whenLooked?.let { (id, run) ->
                whenLooked = null
                // The action that was asked for, if the answer made it possible, and nothing else: with no answer, the Enter that
                // waited runs nothing. (The pill may be on the row, or on one of its other actions in the list under it.)
                // Play where Spotify found nothing by that name: the search for the same words, which is armed in Play's place.
                if (current?.id == got.id || current?.id?.startsWith("act:${got.id}:") == true)
                    (got.actions.firstOrNull { it.id == id && !it.off && !needsAnswer(it.effect) }
                        ?: got.actions.getOrNull(got.armed)?.takeIf { got.provider == Songs.PROVIDER && id == Act.PLAY.id && it.id == Act.SEARCH.id })?.let { run(got, it) }
            }
        }
    }

    /** [row] takes the place of the row with its id, in the list and under a row that is open. The arming stays on its action if the row still offers it, else goes to the row's own default. */
    private fun land(row: Result) {
        if (opened != null) {
            closed = closed.map { if (it.id == row.id) row else it }
            if (opened == row.id) {
                // Its list is open: the pill stays on the action it is on, if the row still has it; else it is back on the row, on its arrow.
                val on = current?.id
                results = listOf(row) + actionRows(row, list ?: Behind.ARROW)
                selected = results.indexOfFirst { it.id == on }.coerceAtLeast(0)
                if (selected == 0) armed = stop(row, list)
                return
            }
        }
        val i = results.indexOfFirst { it.id == row.id }
        if (i < 0) return
        if (i == selected) {
            val was = results[i]
            val id = was.actions.getOrNull(armed)?.id
            // (An action that is gone: the row's own default, which for `play` is the player it is aimed at. Never simply the
            // first action: that may be another app's, and Enter would go there.)
            armed = if (armed == was.actions.size && row.actions.any { it.more && it.behind == Behind.ARROW }) row.actions.size
                else row.actions.indexOfFirst { it.id == id && !it.more && !it.off }.takeIf { it >= 0 } ?: row.armed.takeIf { it in stops(row) } ?: 0
        }
        results = results.toMutableList().also { it[i] = row }
    }

    /** The rows as they were when one of them was asked where it stands: Backspace on the empty field brings them back. */
    private var asked by mutableStateOf<List<Result>?>(null)

    /** An answer stands in place of the rows it was asked from: its row is an answer's, whatever its actions are (Tab goes along them). */
    val inAnswer: Boolean get() = asked != null
    /** And the field holds text, so Backspace is the text's: Escape is then the step back to those rows, with the text as it was. */
    val escapeLeaves: Boolean get() = asked != null && query.isNotEmpty()

    /** Escape on such an answer: back to the rows it was asked from. False where there is none to leave that way. */
    fun leaveAnswer(): Boolean = escapeLeaves && back()

    /**
     * Enter on a row the model answers where it stands (under a thing's chip): the row takes the
     * first place at an answer's height, the others go, and the answer is written into it. The
     * chip stays the thing. Only ever on Enter: an answer costs a second and an allowance nobody publishes.
     */
    private fun askHere(r: Result, e: Effect.Ask) {
        val s = chip as? Answering ?: return
        if (thinking || answering != null) return
        cancelConfirm(); shut()
        val row = s.asking(r, e)
        asked = results
        results = listOf(row); selected = 0; armed = 0; cell = 0
        answered?.let { (was, a) -> if (was == e.prompt && a.id == row.id) { put(a); return } }
        stream(s, row, e.prompt, wait = false)
    }

    /** Asks the device's model [q] for [row] and writes what it says into the row as it arrives. */
    private fun stream(s: Answering, row: Result, q: String, wait: Boolean) {
        answering = scope.launch {
            if (wait) delay(ASK_PAUSE_MS)
            thinking = true
            // The row may have offered "Ask" on trust, before the system was asked. A device without the model says so in the row.
            val state = app.onDevice.state.value.let { if (it == OnDevice.State.UNKNOWN) app.onDevice.check() else it }
            if (state != OnDevice.State.READY) { thinking = false; whenAnswered = null; put(s.unanswered(row)); return@launch }
            val text = StringBuilder()
            try {
                kotlinx.coroutines.withTimeoutOrNull(ANSWER_MS) {
                    app.onDevice.ask(q).collect { piece ->
                        text.append(piece)
                        thinking = false
                        // (Trimmed at both ends as it arrives: a piece that ends in a line break must not count as a line more.)
                        put(s.answered(row, text.toString().trim(), busy = true))
                    }
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

    /** [passing]: the pointer moved over the row; it was not gone to. */
    /** For a screen reader's action on a row: one of the row's actions that is kept behind its arrow is run from its list, as by key. */
    fun selectId(id: String): Boolean = results.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.also { select(it) } != null

    fun select(index: Int, passing: Boolean = false) {
        if (index !in results.indices || index == selected) return
        moved()
        touched = true
        selected = index
        // Another row: its own default again. Back on a row whose actions are listed: the stop they were opened from, which now closes them.
        armed = if (opened != null && index == 0) stop(results[0], list) else results[index].armed
        cell = 0
        if (!passing) went()
    }

    /**
     * What the arming can rest on, on [r], in the order it is drawn (core `Stops`): the actions shown as
     * icons, then one more stop if the row keeps others behind its arrow. That stop is the arrow itself
     * (More: the number of actions, an index no action has). A typed verb that named a line of one of the
     * row's lists stands in that list's stop: a place in Window's ("chrome top left"), a page or Uninstall
     * in the arrow's ("chrome uninstall").
     */
    fun stops(r: Result): List<Int> = Stops.of(r, open = opened == r.id)

    /** The arming is on a stop that opens one of the row's two lists: its arrow, or Window. Null on any other stop. */
    val onList: Behind? get() = current?.takeIf { it.kind != Kind.ACTION }?.let { Stops.list(it, armed) }

    /** Where the arming rests on [r] for the list [behind]: the arrow (the number of its actions), or its Window stop. */
    private fun stop(r: Result?, behind: Behind?): Int = r?.let { Stops.at(it, behind) } ?: 0

    /**
     * Tab (and Right at the end of the text) on the selected row types into it: only where the row
     * offers nothing else, as a keyword's own row does ("Search YouTube"). On an app's row Tab only
     * moves, past Search and Play like past any other stop: Enter is what enters them.
     */
    val tabEnters: Boolean get() {
        val r = current ?: return false
        return opened == null && !inAnswer && r.actions.getOrNull(armed)?.effect is Effect.EnterScope && stops(r).size < 2
    }

    /** Tab and the arrows along the selected row's stops. False when there is nowhere to go. Moving the arming never opens anything, and never enters anything. */
    fun arm(by: Int, wrap: Boolean, /** The key is held and repeats. */ again: Boolean = false): Boolean {
        // Tab on a row the pointer brought the pill to is going to it. (Not on the row under an app's chip: what is looked
        // up for that row goes by which of its actions is armed, below.)
        if ((chip as? AppChip)?.key != current?.id) went()
        val r = current ?: return false
        val st = stops(r)
        val n = st.size
        if (n < 2) return false
        val at = st.indexOf(armed).coerceAtLeast(0)
        val to = if (wrap) (at + by + n) % n else (at + by).coerceIn(0, n - 1)
        if (st[to] == armed) return false
        // Under an app's chip, going from Search to Play or back changes what the row is: its words, and under Play the
        // lookup. A press of its own does that: a held key stops before it, so it neither flickers nor sends two lookups.
        val next = actAt(r, st[to])
        if (next != null && again) return true
        moved()
        touched = true
        armed = st[to]
        if (next != null) {
            act = next
            if (!demo) (chip as? AppChip)?.let { onTell(app.getString(R.string.a11y_act, label(next), wanted(it, next))) }
            refresh()     // going to Play starts the lookup, after its pause; going to Search sends nothing, and takes a lookup that is waiting back
        } else if (!demo) onTell(app.getString(R.string.a11y_stop, r.actions.getOrNull(st[to])?.label ?: app.getString(if (opened == r.id) R.string.action_less else R.string.action_more), to + 1, n))
        return true
    }

    /**
     * Under an app's chip, on the chip's own row: the one of the app's two actions that arming the
     * action at [index] changes to. Null where the arming changes nothing but itself.
     */
    private fun actAt(r: Result, index: Int): Act? {
        val c = chip as? AppChip ?: return null
        if (r.id != c.key || opened != null) return null
        return Act.of(r.actions.getOrNull(index)?.id)?.takeIf { it != act && it in c.acts && !r.actions[index].off }
    }

    fun armAt(index: Int) {
        val r = current ?: return
        if (index in stops(r) && index != armed) { moved(); touched = true; armed = index }
    }

    /** For `./bl debug key more` and `key window`: the arming goes to the stop of one of the row's lists, if the row has it. */
    fun armList(behind: Behind) { current?.let { armAt(stop(it, behind)) } }

    private fun actionRows(r: Result, behind: Behind): List<Result> = r.actions.filter { it.more && it.behind == behind }.map { a ->
        Result(
            id = "act:${r.id}:${a.id}", provider = r.provider, kind = Kind.ACTION, title = a.label, icon = io.github.kuscher.booklight.core.Icon.Symbol(a.symbol),
            score = 1.0, actions = listOf(a.copy(more = false)), learnable = false,
        )
    }

    /**
     * Opens one of the selected row's two lists: the lines kept behind that stop become rows of their
     * own under it, and while they are there the list is that row and those rows. Only for the list
     * on screen. The row's other list, if it was open, closes in the same change: never two.
     */
    fun open(behind: Behind = onList ?: Behind.ARROW): Boolean {
        opened?.let { id ->
            if (list == behind) return false
            shut()
            selected = results.indexOfFirst { it.id == id }.coerceAtLeast(0)
        }
        val r = current ?: return false
        if (r.actions.none { it.more && it.behind == behind } || !onScreen) return false
        moved()
        val rows = actionRows(r, behind)
        // A typed line that was shown on the row goes back into its list: the pill lands on its row there.
        val typed = r.actions.getOrNull(r.armed)?.takeIf { it.more && it.behind == behind }?.let { a -> rows.indexOfFirst { it.actions[0].id == a.id } } ?: -1
        closed = results
        opened = r.id; list = behind
        results = listOf(r) + rows
        selected = 1 + typed.coerceAtLeast(0); armed = 0; cell = 0
        return true
    }

    /** Closes the opened row: the list is back as it was, the pill on the row, the stop armed that had opened it. */
    fun close(): Boolean {
        val id = opened ?: return false
        val behind = list
        moved()
        val back = closed
        shut()
        selected = back.indexOfFirst { it.id == id }.coerceAtLeast(0)
        armed = stop(back.getOrNull(selected), behind)
        return true
    }

    /** The opened row is no longer open, whatever happens to the list next. */
    private fun shut() {
        if (opened == null) return
        opened = null; list = null
        results = closed
        closed = emptyList()
        selected = selected.coerceIn(0, (results.size - 1).coerceAtLeast(0))
    }

    /** Tab and Shift + Tab while a row is open: down and up through its actions, wrapping inside them. */
    fun step(by: Int) {
        val n = results.size - 1
        if (opened == null || n < 1) return
        moved()
        // From the row itself: down to its first action, up to its last. From an action: the next or the one before, wrapping.
        val at = if (selected == 0) (if (by > 0) -1 else n) else selected - 1
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
        if (!onScreen) { whenReady = run; return }
        // On Window or on the row's arrow: Enter opens that stop's lines as a list, or closes them again.
        onList?.let { if (opened != null && list == it) close() else open(it); return }
        val (r, a) = chosen() ?: return
        // "Don't suggest" is the panel's own: nothing runs, and the thing is not counted as run.
        (a.effect as? Effect.Unsuggest)?.let { unsuggest(it.id); return }
        // A row the model answers where it stands: asked now, in its place.
        (a.effect as? Effect.Ask)?.let { askHere(r, it); return }
        // A prompt's row that reads "Ask": the model is asked now, without waiting for a pause in typing. Asked again
        // while it has not said a word yet, nothing more happens: an Enter on "Ask" never becomes a copy of an answer
        // nobody has read. Once words arrive the row reads "Copy", and Enter then waits for all of them.
        if (a.effect == PromptScope.ASK) { if (!thinking) ask(now = true); return }
        if ((r.body as? Body.Stream)?.busy == true) { whenAnswered = run; return }
        // A flight's Copy, Pin or Add to calendar before its answer has come: it runs when the answer is in. So does Play in
        // Spotify before Spotify has said what it found (and where it found nothing, the search that takes its place).
        if (needsAnswer(a.effect)) {
            // (Where no lookup is on its way, under a text's chip or after the list was made anew, it is started now.)
            // (The row the pill is on, if it is the one that waits: under `play` another music app's row may wait too.)
            whenLooked = a.id to run
            val row = r.takeIf { waits(it) } ?: results.firstOrNull { waits(it) }
            if (row != null && !lookedUp(row)) lookUp(row, pause = false)
            if (looking?.isActive != true) whenLooked = null
            return
        }
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

    private fun into(e: Effect.EnterScope, /** The row and the action that were run, where they are not the selected row's (Ctrl + digit). */ ran: Pair<Result, Action>? = chosen()) {
        val from = chip
        // Where Enter is pressed: the letters as typed, the row, the action. An app's chip goes back to exactly there.
        val at = if (from == null) ran?.let { (r, a) -> Origin(query, if (r.kind == Kind.ACTION) opened ?: r.id else r.id, a.id, zeroUp) } else null
        filledAt = SystemClock.uptimeMillis()
        app.engine.scope(e.key)?.let {
            enterScope(it, e.text, act = e.act)
            if (at != null && chip is AppChip) origin = at
            // (Text another app handed over stays that app's in the scope it moves into. An app's chip has no keyword either, but its text is the user's own.)
            if (from != null && from.keywords.isEmpty() && from !is AppChip) foreign = true
            // Another app's keyword, entered from its row: from now on its keyword and a Space enters it, like Booklight's own.
            if (!demo && !it.spaceEnters) change { s -> if (e.key in s.usedScopes) s else s.copy(usedScopes = s.usedScopes + e.key) }
        }
    }

    /**
     * Tab or Right on a scope's row: type into it. Only for the list that is on screen: if the rows for the
     * current text are still on their way, nothing happens (an arrow key must never run a row nobody has seen).
     */
    fun fill(): Boolean {
        if (!onScreen) return false
        val e = chosen()?.second?.effect as? Effect.EnterScope ?: return false
        into(e)
        return true
    }

    /** Ctrl + a digit: that row's first action, straight away. Never one that removes something, and only for rows on screen. */
    fun runRow(n: Int, run: (Result, Action) -> Unit) {
        if (!onScreen) return
        val r = results.getOrNull(n)?.takeIf { it.body !is Body.Grid } ?: return
        val a = r.actions.firstOrNull()?.takeIf { !it.danger && !it.confirm } ?: return
        (a.effect as? Effect.EnterScope)?.let { into(it, r to a); return }
        if (a.effect == PromptScope.ASK || a.effect is Effect.Ask || needsAnswer(a.effect) || (r.body as? Body.Stream)?.busy == true) return     // an answer is Enter's
        if (a.effect is Effect.Unsuggest) return        // and so is "Don't suggest"
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
        val last = app.lastText ?: return false
        app.lastText = null
        val s = last.key?.let { app.engine.scope(it) }
        // (An app's chip comes back as it was left: the action that was armed, and the keyword it was entered by.)
        if (s != null) { enterScope(s, last.text, last.word, last.act); via = last.via?.let { app.engine.scope(it) } } else { query = last.text; search() }
        return true
    }

    /** The panel is closing without having run anything: keep what was typed for [restoreLast]. */
    fun keep() {
        hideTip()
        // Text another app handed over is never kept, in its own chip or once it has moved into a note or a code. Nor is
        // an example Booklight typed, unless the user made it their own by editing it.
        // (An app's chip has no keyword either, but what is typed under it is the user's own.)
        if (query.isNotBlank() && !foreign && !shown && (chip?.keywords?.isEmpty() != true || chip is AppChip)) app.lastText = BooklightApp.Last(chip?.key, query, act, word, via?.key)
    }

    /** Something was run: the line of the list of everything it belongs to has been used, and is not suggested again. */
    fun used(r: Result, a: Action) {
        if (demo) return
        val id = app.guide.used(chip, r, a) ?: return
        if (id !in settings.used) change { it.copy(used = it.used + id) }
    }

    /** Remember the pick, so the same text finds it first next time. [a]: the action that was run. */
    fun learn(r: Result, a: Action) {
        app.lastText = null
        // (Under a chip nothing is learned: the way by an app's row always goes to the app, and teaches nothing about the typed sentence.)
        if (chip != null) return
        // A line of one of a row's lists (Left half, behind Window) is a use of the row it belongs to: the app was opened.
        val row = if (r.kind == Kind.ACTION) closed.firstOrNull { it.id == opened } ?: r else r
        // (From the usual rows, or after a typed space, nothing was typed for it: it counts as run, and no text is tied to it.)
        app.engine.picked(Query(if (query.isBlank()) "" else query), row)
        // Words after an app's name: which of the two searches was run says who leads next time, the app or the web (core `Lead`).
        if (a.id == Act.SEARCH.id) app.engine.led(r, if (opened != null) closed else results)
        app.historyStore.changed()
    }

    fun change(f: (Settings) -> Settings) = app.prefs.update(f)

    companion object {
        private const val SUGGEST_PAUSE_MS = 140L
        /** How long a confirmation waits for its second Enter. */
        const val CONFIRM_MS = 3000L
        private const val CONFIRM_GAP_MS = 350L
        /** After Tab on the copy's line a row may be run this soon: its rows are readable by then. */
        private const val COPY_GUARD_MS = 200L
        /** A holder of one of the first two usual seats that has had no row for this long has given its seat up. */
        private const val AWAY_MS = 10 * 60 * 1000L
        /** How long typing rests before the device's own model is asked, and how much must be typed for it to be asked unasked. */
        private const val ASK_PAUSE_MS = 500L
        private const val ASK_MIN = 3
        /** How long an answer may take in all. A model that takes the question and then says nothing is given up on; what has arrived by then stands. */
        private const val ANSWER_MS = 60_000L
        private const val HELP = "?"
        /** While the system is still looking at a copy just made: how often Booklight looks again, and how long between. */
        private const val COPY_LOOKS = 5
        private const val COPY_LOOK_MS = 400L
        /** A flight's lookup: how long typing rests before it is sent (the provider waits that long), and how long it may then take before the light of work comes on. */
        private const val LOOK_PAUSE_MS = 400L
        private const val LOOK_LIGHT_MS = 600L
        /** How long "Tips are off…" stands before the card goes. */
        const val TIP_OFF_MS = 1600L
    }
}
