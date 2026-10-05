package io.github.kuscher.booklight.overlay

import android.os.SystemClock
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.flow.first
import io.github.kuscher.booklight.core.Act
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.AppChip
import io.github.kuscher.booklight.core.Behind
import io.github.kuscher.booklight.core.Chips
import io.github.kuscher.booklight.core.Origin
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.SetDown
import io.github.kuscher.booklight.core.Show
import io.github.kuscher.booklight.core.Stops
import io.github.kuscher.booklight.data.Settings
import io.github.kuscher.booklight.data.firstRun
import io.github.kuscher.booklight.device.Keyboards
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The panel's state: the chip in the field (a scope) and the text after it, the ranked rows, which
 * row is selected and which of its actions is armed, the cell of a grid, and a confirmation that
 * is waiting. The UI only draws this; keys and clicks call in here.
 */
class OverlayModel(
    private val app: BooklightApp, private val scope: CoroutineScope, private val limit: Int = 8,
    /** The list in the Booklight window that shows what the panel does: it never asks the network and learns nothing. */
    private val demo: Boolean = false,
    /** The screen's height in dp: on a screen too low for it first run does not stand in the panel (core `FirstRun.fits`). */
    private val screenDp: Float = Float.MAX_VALUE,
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
    // (Not while first run's show types: a keyword Booklight types is not the user's to make a chip of.)
    val keyword: Scope? by derivedStateOf { if (playing != null || chip != null || touched || opened != null) null else app.engine.keywordScope(query) }

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
    var arrived: Boolean
        get() = hasArrived
        set(value) { if (value && !hasArrived) arrivedAt = SystemClock.uptimeMillis(); hasArrived = value }
    private var hasArrived by mutableStateOf(false)
    private var arrivedAt = 0L
    /** The panel was opened to have an example typed into it (a row of the window's Commands page): neither first run's stage nor a tip comes first. */
    var guided by mutableStateOf(false)
    /**
     * The panel carries text another app handed over (its selection menu, its share sheet). Like a guided panel it is
     * not one of a run's openings: no screen of first run is due in it, so no lesson's coach line stands over that text
     * and Enter there is as every day.
     */
    var carries by mutableStateOf(false)

    // ---- first run: the opening piece (the welcome, then the show)

    // (Its state stands here, before the stage's: the stage asks it, and is first asked while this object is made.)
    /** Where the opening piece is while it plays (core `FirstRun.Playing`); null: it does not play, or is over. */
    var playing by mutableStateOf<FirstRun.Playing?>(null); private set
    /** This panel began with the opening piece: what draws the piece is composed in this panel, and in no other. */
    var began by mutableStateOf(false); private set
    /**
     * The opening piece is this opening's and is not played (the system's animations are off, or a screen reader is on:
     * core `FirstRun.greets`): the welcome's title greets as the field's placeholder. Set by the activity.
     */
    var greets by mutableStateOf(false)
    /** What the show needs of this device (`BooklightApp.cast`); null until it is worked out, and where the device has nothing to show. */
    var cast by mutableStateOf<BooklightApp.Cast?>(null); private set
    private var casting: Job? = null
    /** Whether the show has something to show here (core `FirstRun.Cast`): its rows are there, still being worked out, or this device has none. */
    val casts: FirstRun.Cast get() = if (cast != null) FirstRun.Cast.READY else if (casting?.isCompleted == true) FirstRun.Cast.NONE else FirstRun.Cast.WAITS
    private var performer: Job? = null
    /** The cue was given (Enter in the welcome, a click on its handle): the welcome hands over as soon as the show can begin. */
    var cued by mutableStateOf(false); private set
    /** The welcome's handle stands and carries the cue: a click on it is the cue. Set by the welcome. */
    var cueUp by mutableStateOf(false)
    /** The welcome's hand-over has come as far as the field's mark: Booklight's own stands in its seat, a moment before Booklight types. Set by the welcome. */
    var marked by mutableStateOf(false)
    /** How often the show has asked for the light to set off round the outline: the panel runs one lap for each. */
    var laps by mutableIntStateOf(0); private set
    /** How often the piece has begun in this panel: once, but for a debug hook that plays it again. The welcome's clock starts anew with each. */
    var rounds by mutableIntStateOf(0); private set
    /** Debug builds: the welcome's clock stands at this many ms of the paper's own clock (`./bl debug first welcome at MS`); null: it runs. */
    var standsAt by mutableStateOf<Float?>(null); private set
    /** The height the glass keeps for a moment while what was Booklight's fades at the landing: the lower edge draws in after it (`Metrics.height`). */
    var heldHeight by mutableStateOf<Dp?>(null); private set
    /** How long a wait of the piece really is (`Motion.hold`: a debug build's slow motion stretches it). Set by the activity. */
    var hold: (Long) -> Long = { it }
    private var keeping: Job? = null

    /**
     * The glass keeps the height [high] for [ms] of motion at the most ([lasts]), and then goes to its own: what stood in
     * it is still fading (the question as the choices come, what was Booklight's at the landing), or what comes has not
     * landed yet (the list for a letter typed on a stage: the lower edge then turns to that list's height from where it
     * is, and never sets off for the bare field's first). One hold at a time: a new one takes the place of the one
     * before, whose wait is given up, so that no old wait lets go of a newer hold. With the system's animations off
     * nothing fades, and a wait for a fade is nothing: the glass is at its own height at once. [real]: what is waited for
     * is no motion (the list for what was typed, which the engine makes in its own time): the wait is [ms] as they are,
     * whatever the motion is, and with the system's animations off too. Without it the glass would step from the stage's
     * height to the bare field's and on to the list's at the first letter of each lesson.
     */
    private fun keep(high: Dp, ms: Long, real: Boolean = false) {
        val span = if (real) ms else lasts(ms)
        if (span <= 0L) return
        keeping?.cancel()
        heldHeight = high
        keeping = scope.launch { delay(span); heldHeight = null }
    }
    /** The glass is at its own height again, and no wait is left that would let go of a later hold. */
    private fun letGo() { keeping?.cancel(); keeping = null; heldHeight = null }
    /**
     * The screen of first run that is due has not been in view, and something is in its place: the screen came while
     * the field held a list ([rearm]); or it was typed over before its answers had been in view for a moment
     * ([covered]: so is the key's step that came as a typed character ended the opening piece, [typed]). It comes when
     * the field is empty again ([search]), and its keys count from then.
     */
    private var unseen = false
    /**
     * The one highlight on its way from where the show left it into the armed answer of the screen that lands: where it
     * stood when the landing began, in dp from the glass's corner (left, top, right, bottom, corner radius). Null: none
     * travels, or it has arrived.
     */
    var glideFrom by mutableStateOf<FloatArray?>(null); private set
    /** While it is on its way the stage's own answers are not lit: one highlight, never two. */
    val gliding: Boolean get() = glideFrom != null
    /** Where the stage's armed answer stands, in px from the glass's corner (left, top, right, bottom), as the stage last said while the highlight was on its way. */
    var answerAt by mutableStateOf<FloatArray?>(null)
    /** It has arrived: from now on the highlight is the answers' own. */
    fun glided() { glideFrom = null }

    /**
     * Where the highlight of what stands now is drawn, in dp from the glass's corner: the square on a grid's cell (44 dp
     * in its 48 dp cell, radius 14), else the pill on the selected row (radius 24). By the sums the rows and the grid
     * draw by. Null: nothing is selected.
     */
    private fun highlight(): FloatArray? {
        val r = current ?: return null
        val top = Metrics.field.value + Metrics.pad.value + Metrics.tops(results)[selected].value
        val grid = r.body as? Body.Grid
            ?: return floatArrayOf(Metrics.pad.value, top, Metrics.width.value - Metrics.pad.value, top + Metrics.rowHeight(r).value, 24f)
        val pitch = (Metrics.width.value - 28f) / grid.columns
        val inset = (pitch - Metrics.cell.value) / 2f + 2f
        val left = 14f + (cell % grid.columns) * pitch
        val up = top + 8f + (cell / grid.columns) * Metrics.cell.value
        return floatArrayOf(left + inset, up + 2f, left + pitch - inset, up + Metrics.cell.value - 2f, 14f)
    }

    // ---- first run: its stage under the empty field

    /**
     * The screen of first run that is due in this panel (core `FirstRun.stage`), whatever the field holds; null: none.
     * Not in the Booklight window's demo, not in a panel that was opened to have an example typed into it, and not in
     * one that carries another app's text.
     */
    val due: FirstRun.Screen? by derivedStateOf { if (demo || guided || carries) null else FirstRun.stage(settings.firstRun(), screenDp) }
    /**
     * The screen whose stage has the place under the empty field; null: none. Like a tip, only while nothing is typed and
     * no chip stands. The choices are no stage: they are a list of their own.
     */
    // (Not while first run's welcome or its show plays: the stage is what they land in.)
    val stage: FirstRun.Screen? by derivedStateOf {
        due?.takeIf { it != FirstRun.Screen.C && query.isEmpty() && chip == null && (playing == null || playing == FirstRun.Playing.LANDING) }
    }
    /**
     * The lesson that stands in this panel (the second, third or fourth step's screen): first its stage under the empty
     * field, then the list that is typed for it. It goes on standing while that list is on screen, though its stage does not.
     */
    val lesson: FirstRun.Screen? by derivedStateOf { due?.takeIf { it.step == FirstRun.Step.OPEN || it.step == FirstRun.Step.SEARCH || it.step == FirstRun.Step.SUM } }
    /** Which of the stage's answers Enter runs; -1: none (Enter then does nothing). */
    var stageArmed by mutableIntStateOf(FirstRun.armed(app.prefs.now.firstRun())); private set
    /** The screen, and its answers, that [stageArmed] was last set for ([rearm]). */
    private var armedFor = app.prefs.now.firstRun().let { FirstRun.screen(it) to FirstRun.answers(it) }
    /** The key suggested beside Action, for the keyboard in front ([resuggest]). */
    var suggested by mutableStateOf(FirstRun.Key.QUICK_INSERT); private set
    /** The keyboard the last key came from, by its id; null before any key. Set by the panel. */
    var keyboard: Int? = null
    /** The key's step waits for the key: none has opened the panel yet, or another one is asked for. */
    val awaitsKey: Boolean get() = stage.let { it == FirstRun.Screen.K1 || it == FirstRun.Screen.K2 || it == FirstRun.Screen.K3 }

    // ---- first run: how the screen that stands comes onto the glass

    /**
     * How the screen that stands came (core `SetDown.Comes`: set down part by part, come back to, in another's place,
     * after a list, at the landing of the opening piece, or whole at once), and when each of its parts comes by that
     * ([marks], core `SetDown.marks` with `Motion.kt`'s times). Worked out once, in the moment it comes ([came]). The
     * stage draws by these marks on one clock; [seen] counts by them.
     */
    var comes by mutableStateOf(SetDown.Comes.WHOLE); private set
    var marks by mutableStateOf(SetDown.WHOLE); private set
    /** When the screen of first run that stands came (the model's making, for the one it was made with), by the clock keys are timed by. */
    var cameAt by mutableLongStateOf(SystemClock.uptimeMillis()); private set
    /** The zero of the stage's clock: when the screen came, and not before the glass had opened far enough for it to be seen. */
    val viewFrom: Long get() = maxOf(arrivedAt, cameAt)
    /**
     * For a screen that came while the system's Keyboard shortcuts dialog was over the panel ([under]: "Now press your
     * keys" a moment after the dialog came, "Your key works" where the key landed under it), and for one the dialog
     * came over before it had been in view for its moment: when the panel was uncovered again, by the clock keys are
     * timed by, and [COVERED] for as long as it is not. 0: the screen came in view, and was in view when the dialog
     * came, if one did. Nobody saw such a screen under the dialog, however long it stood there, so its keys count from
     * when the dialog went and not from when it came ([taken], core `FirstRun.inView`). Only that: it is drawn by the
     * moment it came ([viewFrom]: its parts were set down under the dialog and stand), no mark of it is made anew, and
     * it is said to a screen reader once, for that coming. It never stays [COVERED]: whatever ends the hold says so
     * through [under], in the one place the hold changes (`OverlayActivity.signal`).
     */
    private var uncoveredAt = 0L
    /** The screen that stands came under the system's dialog and is still under it: no key counts. (For the debug hooks that say so.) */
    val underDialog: Boolean get() = uncoveredAt == COVERED
    /** How long after the gate the band of a screen begins that is set down as this panel opens (`Arrival.bandAfter`): said by the activity ([opening]). */
    private var bandAfter = Motion.BAND_AFTER_MS
    /** How long a span of motion really is (`Motion.held`: a debug build's slow motion stretches it, and with the system's animations off it is nothing). Set by the activity. */
    var lasts: (Long) -> Long = { it }
    /** Debug builds: the stage's clock stands at this many ms (`./bl debug first stage at MS`); null: it runs. */
    var stageAt by mutableStateOf<Float?>(null)

    /**
     * What the recipes of lessons 2 and 3 show to type on this device (core `FirstRun.Example`); null until it is worked
     * out. That is asked only where a screen of first run is due.
     */
    var example by mutableStateOf<FirstRun.Example?>(null); private set
    private var exampling: Job? = null

    /** The recipe of the lesson that stands, on this device (core `FirstRun.recipe`). Lessons 2 and 3 have none until their example is worked out. */
    fun recipe(f: FirstRun.State = first()): List<FirstRun.Part> =
        if (example == null && FirstRun.lesson(f) != FirstRun.Screen.L4) emptyList()
        else FirstRun.recipe(f, example ?: FirstRun.example(null, "", null, "", ""), app.getString(R.string.first_sum_example))

    /**
     * The screen that stands came in this moment, in the way [how] says. Its marks are worked out (for a screen that
     * takes the place of [was], the screen and the answers that stood: what they share with it does not move), and its
     * keys count from when its answers have begun to show ([seen]).
     */
    private fun came(how: SetDown.Comes, was: Pair<FirstRun.Screen?, List<FirstRun.Answer>>? = null) {
        comes = how
        cameIn = was
        marks = marksFor(how, was)
        cameAt = SystemClock.uptimeMillis()
        // (Under the system's dialog it came unseen: it is in view once the panel is uncovered, which [under] says.)
        uncoveredAt = if (under) COVERED else 0L
    }
    /** What the screen that stands took the place of when it came ([came]): kept for a recipe that is worked out a moment late ([exemplify]). */
    private var cameIn: Pair<FirstRun.Screen?, List<FirstRun.Answer>>? = null

    /** The marks of the screen that is due, for the way [how] it comes (core `SetDown.marks` with `Motion.kt`'s times). */
    private fun marksFor(how: SetDown.Comes, was: Pair<FirstRun.Screen?, List<FirstRun.Answer>>?): SetDown.Marks {
        val f = first()
        val on = FirstRun.screen(f)
        // The band's pieces as core's schedule counts them: how many letters each typed part has, 0 for a cap or a sign.
        // (Letters as a reader counts them, which is how they are written: core `FirstRun.ends`.)
        val letters = when (on?.step) {
            FirstRun.Step.KEY -> if (on == FirstRun.Screen.K1) listOf(0, 0, 0) else listOf(0)
            FirstRun.Step.OPEN, FirstRun.Step.SEARCH, FirstRun.Step.SUM -> recipe(f).map { (it as? FirstRun.Part.Typed)?.text?.let { t -> FirstRun.ends(t).size } ?: 0 }
            else -> emptyList()
        }
        // (The choices are a list of their own: nothing of a stage comes for them.)
        return if (on == null || on == FirstRun.Screen.C) SetDown.WHOLE else SetDown.marks(
            how, PACE, letters, ask = on == FirstRun.Screen.Q,
            // (Under "Your key works" and under the question's answers stands no caption.)
            caption = on != FirstRun.Screen.K4 && on != FirstRun.Screen.Q, bandAfter = bandAfter,
            bandChanges = was == null || !SetDown.sameBand(was.first, on), captionChanges = was == null || !SetDown.sameCaption(was.first, on, suggested),
            answersChange = was == null || was.second != FirstRun.answers(f),
        )
    }

    /**
     * Debug builds (`./bl debug first stage …`): the screen that stands comes again in the way [how] says, as if in this
     * moment (null: as it came); and its clock stands at [at] ms, or runs (null). For a picture of a set-down at a named
     * moment. Nothing that is kept changes.
     */
    fun stageAs(how: SetDown.Comes?, at: Float?) {
        stageAt = at
        if (how != null) came(how)
    }

    /** The activity says how this panel opens: how long after the gate the band of a screen begins that is set down in it. Said before the glass opens. */
    fun opening(bandAfter: Int) {
        this.bandAfter = bandAfter
        if (comes == SetDown.Comes.SET_DOWN) came(SetDown.Comes.SET_DOWN)
    }

    /**
     * The example is worked out, once for this panel, off the main thread (`BooklightApp.firstExample`). It can land a
     * moment after a lesson came whose recipe it is (a run that stopped on lesson 2 or 3 and is asked for again: the
     * marks were made for a band with nothing in it). Where that lesson is being set down and its band is not due yet,
     * the marks are worked out again with the recipe in them, on the clock that runs: the recipe is written a letter at
     * a time, and its caption and Skip come after it. The screen does not come again by this (it is said once, and its
     * keys count from the same moment by marks that are only later). Where the band was due already, or the screen came
     * back as rows do, the recipe fades in where it belongs, as before.
     */
    private fun exemplify() {
        if (example != null || exampling != null) return
        exampling = scope.launch {
            example = app.firstExample()
            val on = stage
            if ((on != FirstRun.Screen.L2 && on != FirstRun.Screen.L3) || !marks.written || marks.pieces.isNotEmpty()) return@launch
            val next = marksFor(comes, cameIn)
            val due = next.pieces.firstOrNull() ?: return@launch
            if (!arrived || SystemClock.uptimeMillis() - viewFrom < lasts(due.toLong())) marks = next
        }
    }

    /** What the question's text needs beyond its four lines; the stage is that much taller (`Metrics.stage`). Measured by the panel before the question stands. */
    var askMore by mutableStateOf(0.dp); private set
    fun askRoom(more: Dp) { if (more != askMore) askMore = more }

    // (The keyboards are asked, and the example worked out, only where a screen is due: on every other day nothing here runs.)
    init {
        if (!demo && stage != null) resuggest()
        // (A screen the panel opens on is set down at its gate part by part; one the run has stood on before comes back as rows do.)
        if (!demo && due != null) { exemplify(); came(if (FirstRun.comesBack(first())) SetDown.Comes.BACK else SetDown.Comes.SET_DOWN) }
        // The settings also change from outside the model (the window, a debug hook): see [rearm].
        if (!demo) scope.launch { app.prefs.state.collect { rearm(it.firstRun()) } }
    }

    private fun first() = app.prefs.now.firstRun()

    /**
     * A step of first run, worked out inside the settings' own update (`Prefs.firstRun`); what stands is read from the
     * settings in the same call (they reach [settings] by themselves only a moment later). Nothing is written where
     * nothing changes.
     */
    private fun step(f: (FirstRun.State) -> FirstRun.State) {
        val was = first()
        if (f(was) == was) return
        app.prefs.firstRun(f)
        settings = app.prefs.now
        rearm(first())
        offerChoices()
    }

    /**
     * Another screen of first run has its own answer armed, however the screen changed (a step here, the window, a debug
     * hook): an index kept from the screen before would arm the wrong answer, or none. The screen itself is asked and not
     * [stage], which is null while something is typed: a key can land then, too.
     */
    private fun rearm(first: FirstRun.State) {
        val on = FirstRun.screen(first) to FirstRun.answers(first)
        if (on != armedFor) {
            val was = armedFor
            armedFor = on; stageArmed = FirstRun.armed(first)
            // It takes the place of a screen that stood on the glass: its words roll and its band is written where that one's
            // was. Where none stood there (a list is typed; the choices; nothing), it stands whole whenever it shows.
            val turns = stage != null && was.first != null && was.first != FirstRun.Screen.C
            came(if (turns) SetDown.Comes.TURN else SetDown.Comes.WHOLE, was)
            // A stage's screen that does not stand now (its lesson's Enter was pressed on a list that is still typed on; the
            // key landed over typed text) has not been in view, however long ago this moment will be when it is first drawn:
            // it comes into view when the field is empty again, and its keys count from then ([search]). What sets it down
            // itself in a way of its own says so after emptying the field ([giveWay], [again], [land]).
            if (on.first != null && on.first != FirstRun.Screen.C && stage == null) unseen = true
        }
    }

    /**
     * The screen that stands has been in view for a moment (core `FirstRun.inView`): the glass is open, and neither it
     * nor the screen came just now, and where the screen is set down part by part its answers have begun to show
     * ([marks]). A screen's keys and clicks count from then, each screen for itself: the Enter that skipped a lesson,
     * pressed again at once, must not answer the question that took the lesson's place, and nothing is answered on a
     * screen that is still under the lower edge of a glass that grows, or whose answers are still to come.
     *
     * Which answer a key can reach says from when it counts (core `SetDown.since`). Enter runs the armed answer and no
     * other: it counts from when that one began to show ([stageEnter]). Tab, the pointer and a click reach any answer:
     * they count from when every answer has begun to show, which at the landing of the opening piece is 300 ms after
     * the armed one; and so does Enter once the arming has been moved from the answer that was armed when the screen
     * came. This is the one for everything that can reach any answer.
     *
     * A screen that came while the system's dialog was over the panel is in view only once the panel is uncovered: no
     * key counts while it is covered, and each counts a moment after the later of the two, its answer's beginning to
     * show and the dialog's going ([uncoveredAt]).
     */
    val seen: Boolean get() = taken(0L, enter = false)
    /** Enter would be taken now: the answer that is armed has been in view for a moment. (For the debug hooks that say so; the keys ask [stageEnter].) */
    val takesEnter: Boolean get() = taken(0L, enter = true)
    /**
     * [made]: when the key that asks was pressed (uptime; 0: now). A press counts by when it was made, not by when it is
     * handled: an Enter pressed while a screen was still coming must not run its answer because the frame that drew the
     * screen took long. [enter]: the key is Enter.
     */
    private fun taken(made: Long, enter: Boolean): Boolean {
        // (A screen that came under the system's dialog and is still under it is in nobody's view.)
        if (uncoveredAt == COVERED) return false
        val since = SetDown.since(marks, enter, moved = stageArmed != FirstRun.armed(first()))
        return arrived && FirstRun.inView(if (made > 0L) made else SystemClock.uptimeMillis(), arrivedAt, cameAt, lasts(since.toLong()), uncoveredAt)
    }

    /**
     * Something takes the place of the stage that stands, in this frame: a typed letter, the last text brought back, a
     * chip, an example Booklight types. What of the screen has not come never comes: when the field is empty again it
     * stands whole, and nothing is written twice. And where not every one of its answers had been in view for a moment,
     * the screen has not been in view: it comes into view when the field is empty again, and takes a key a moment after
     * that ([search]), whichever way it had come. Nor does the show's highlight arrive that was still on its way into
     * the screen's armed answer: it is gone with the stage. Called before the field changes, by everything that fills it.
     */
    private fun covered() {
        if (stage == null) return
        if (!seen) unseen = true
        if (comes != SetDown.Comes.WHOLE) { comes = SetDown.Comes.WHOLE; marks = SetDown.WHOLE }
        glideFrom = null
    }

    /**
     * The key is suggested for the keyboard in front: the one the last key came from; before any key, the device's own.
     * Asked when the stage is set down and when the system's dialog is asked for, never while the caps are being read.
     */
    fun resuggest() { suggested = FirstRun.suggest(FirstRun.hasQuickInsert(Keyboards.attached(), keyboard)) }

    // The stage's keys count only once the glass has opened and the stage can be seen ([arrived]; core `FirstRun.tabbed` and
    // `entered`): Tab and Enter pressed while the panel was still opening must not answer a question nobody has read.
    /** Tab on the stage: the next answer is armed, with [back] the one before; the first press arms the first. */
    fun stageTab(back: Boolean, made: Long = 0L) { if (stage != null) stageArmed = FirstRun.tabbed(first(), stageArmed, back, taken(made, enter = false)) }
    /** The pointer is on an answer: it is armed. */
    fun stageArm(index: Int) { if (stage != null && seen && index in FirstRun.answers(first()).indices) stageArmed = index }
    /** Enter on the stage: the armed answer; null where none is armed (Enter then does nothing, and is not kept). */
    fun stageEnter(made: Long = 0L): FirstRun.Answer? = if (stage == null) null else FirstRun.entered(first(), stageArmed, taken(made, enter = true))
    /**
     * An answer that was given on the stage: its place among the [answers] it was given on, and when ([at], by the
     * clock keys are timed by). Each press is one of these, a thing of its own, so that the same slot pressed again
     * shows again. The stage shows it as pressed on those answers and on no others, from that moment (`FirstStage.kt`,
     * `pressedInk`): the answers that take their place never show it.
     */
    class Pressed(val slot: Int, val answers: List<FirstRun.Answer>, val at: Long = SystemClock.uptimeMillis())
    /** The answer of the stage that was given last; null before any. Nothing waits for it to show: the answer is run in the call that sets this. */
    var stagePress by mutableStateOf<Pressed?>(null); private set

    /** An answer was given on the stage: its slot shows as pressed. ("Open Keyboard shortcuts" changes nothing else here: the rest is the window's to do.) */
    fun answerStage(answer: FirstRun.Answer) {
        val was = first()
        val asked = stage == FirstRun.Screen.Q
        val high = Metrics.height(this)
        FirstRun.answers(was).let { all -> all.indexOf(answer).takeIf { it >= 0 }?.let { at -> stagePress = Pressed(at, all) } }
        step { FirstRun.answer(it, answer) }
        // The question gives way to the choices: the lower edge rises only once the question's words have begun to fade, so
        // that it cuts none of them (motion.md T9).
        if (asked && choicesUp) keep(high, Motion.ASK_LEAVES_MS)
        // "Not now" on the key's step ends the run: the steps wait in the Booklight window, and the bare field says so.
        if (FirstRun.later(was, first())) later = true
    }
    /** The system's Keyboard shortcuts dialog has come over the panel: under it the stage says what to do next. */
    fun helperCame() = step { FirstRun.helperCame(it) }
    /** "Your key" on the glass is held down, as the user's own key is in this moment (docs/design/first-run/motion.md §4). */
    var keyDown by mutableStateOf(false); private set
    /** It went down under the system's dialog, unseen: the first frame that shows the panel shows it down, with no way there. */
    var keyUncovered by mutableStateOf(false); private set
    /** The check may draw in "your key": always, but while a landing is shown, where it waits for the key to come up. */
    var keyChecked by mutableStateOf(true); private set
    /**
     * When the user's key last started the panel that greets it, by the clock keys are timed by; 0: no key has landed in
     * this panel. The key, held, repeats, and each repeat is a new start of the panel, which on any day puts an open
     * panel away: for [Motion.KEY_SETTLES_MS] after the last of them a start by the key does nothing ([keyAgain]). Real
     * time, whatever the motion is: the key is held as long with the system's animations off, and in a panel the key
     * itself made.
     */
    private var keyAt = 0L
    /** The user's key has just landed here: a start by the key does nothing yet. */
    val landing: Boolean get() = keyAt != 0L && SystemClock.uptimeMillis() - keyAt < Motion.KEY_SETTLES_MS
    /**
     * A start by the key. True: it is the key that landed, still held or pressed again at once; it does nothing (it does
     * not put the panel away, and it answers nothing), and the wait begins anew with it. False: the wait is over, or
     * no key landed here, and the start is one like any other.
     */
    fun keyAgain(): Boolean {
        if (!landing) return false
        keyAt = SystemClock.uptimeMillis()
        return true
    }
    /** The landing as it is shown, and the wait for its lap of light ([keyLanded], [keyOpened]): given up when the panel closes ([closing]). */
    private var landed: Job? = null

    /**
     * The user's key has opened the panel, or uncovered it. Where its step waited for it, "Your key works" stands; and
     * where that step stood on the glass, the landing is shown (motion.md T3 and T4): "your key" is down as the user's is,
     * for 160 ms in view, for 400 where the panel was [under] the system's dialog and is only now uncovered; it comes
     * up; 60 ms later its check draws; 240 ms after the key came up the light runs its one lap. With the system's
     * animations off all of it stands in the first frame.
     */
    fun keyLanded(under: Boolean = false) {
        val waited = awaitsKey
        step { FirstRun.keyLanded(it) }
        if (!waited || stage != FirstRun.Screen.K4) return
        keyAt = SystemClock.uptimeMillis()
        landed?.cancel()
        keyUncovered = under; keyDown = true; keyChecked = false
        landed = scope.launch {
            delay(lasts(if (under) Motion.KEY_HELD_MS else Motion.KEY_TAP_MS))
            keyDown = false
            delay(lasts(Motion.CHECK_AFTER_MS))
            keyChecked = true
            // (The light sets off `LAP_AFTER_MS` after the key came up: the check has been drawing since `CHECK_AFTER_MS` of that.)
            delay(lasts(Motion.LAP_AFTER_MS - Motion.CHECK_AFTER_MS))
            if (stage == FirstRun.Screen.K4) laps++
        }
    }

    /**
     * The user's key has made this panel, and "Your key works" is set down in it for the first time (the system's dialog
     * was closed before the keys were pressed): the same lap of light, 240 ms after "your key" has come up at its mark.
     * Said by the activity.
     */
    fun keyOpened() {
        if (due != FirstRun.Screen.K4) return
        // (The key that made this panel is held as any key is: its repeats do not put the panel away either.)
        keyAt = SystemClock.uptimeMillis()
        landed?.cancel()
        landed = scope.launch {
            androidx.compose.runtime.snapshotFlow { arrived }.first { it }
            delay(lasts(maxOf(0, SetDown.piece(marks, 0)).toLong() + Motion.LAP_AFTER_MS))
            if (stage == FirstRun.Screen.K4) laps++
        }
    }
    /** Debug builds: the stored state was set from outside (`./bl debug first …`). What stands follows it at once. */
    fun firstChanged() {
        settings = app.prefs.now
        if (stage != null) resuggest()
        if (due != null) exemplify()
        rearm(first())
        // (The choices are a list: it goes where another screen is due now, and comes where they are.)
        if (choicesUp && due != FirstRun.Screen.C) { choicesUp = false; results = emptyList(); selected = 0; resultsFor = For(null, query) }
        offerChoices()
    }

    // ---- first run: what a screen reader is told

    /** What first run has told a screen reader in this panel, the last few sentences as they were said: for the debug hook that prints them (`./bl debug first says`). */
    var said: List<String> = emptyList(); private set
    /**
     * A sentence of first run's for a screen reader: said, and kept for that hook. Not while the system's Keyboard
     * shortcuts dialog is over the panel ([under]): a reader is with the dialog then, and a sentence from the window
     * under it would speak into what the dialog says. It waits, the last one alone (only the key's screens change under
     * the dialog, and what is worth saying is the one that stands when the dialog goes), and is said then.
     */
    fun tell(text: String) {
        if (under) { untold = text; return }
        said = (said + text).takeLast(SAID); onTell(text)
    }
    /**
     * The system's dialog is over the panel, which is held behind it (core `FirstRun.Hold.UNDER`). Said by the activity,
     * in the one place the hold changes. Once it has gone (the hold is over: the focus is back, or the panel goes), a
     * screen that came under it is in view from that moment ([uncoveredAt]), and what waited to be said is said.
     */
    var under = false
        set(value) {
            val was = field
            field = value
            if (value) {
                // The dialog has come over a screen that had not been in view for its moment (the key landed as the dialog
                // was on its way up): that one too counts from when the panel is uncovered, as if it had come under it.
                if (!was && stage != null && !seen) uncoveredAt = COVERED
                return
            }
            if (uncoveredAt == COVERED) uncoveredAt = SystemClock.uptimeMillis()
            untold?.let { untold = null; tell(it) }
        }
    private var untold: String? = null
    /** The screen that was last said, with the moment it came; whether its recipe was said with it; and whether the greeting was said in this panel. */
    private var saidFor: Pair<FirstRun.Screen, Long>? = null
    private var recipeSaid = false
    private var greeted = false

    /**
     * The screen [on] is to be said now: true once for each coming of a screen. A stage that is only back, after something
     * was typed over it and deleted, is not said again. [recipe]: what is said includes its recipe.
     */
    fun says(on: FirstRun.Screen, recipe: Boolean): Boolean {
        val key = on to cameAt
        if (saidFor == key) return false
        saidFor = key; recipeSaid = recipe
        return true
    }
    /** The recipe of [on] is still to be said: this device's example was worked out after its lesson was said. True once. */
    fun saysRecipe(on: FirstRun.Screen): Boolean = (saidFor?.first == on && !recipeSaid).also { if (it) recipeSaid = true }
    /** The welcome's title is to be said before the first screen's words, where the opening piece is this opening's and is not played: once in a panel, whatever is typed and deleted in it. */
    fun greeting(): Boolean = (greets && !greeted).also { greeted = true }

    // ---- first run: asked for again, and where the steps wait

    /**
     * The steps have begun to wait in the Booklight window in this opening (core `FirstRun.later`): "Not now" was
     * answered on the key's step, or an unfinished run has stood in its three openings (the activity says that one). The
     * bare field's placeholder says where they are, for the rest of this opening.
     */
    var later by mutableStateOf(false)

    /**
     * "First steps", asked for by its command in this panel (core `FirstRun.againHere`): an unfinished run goes on where
     * it stopped, a finished one is played again from its first screen, without the opening piece: the user is at work
     * here. The field is emptied in one change and the screen that is due stands under it, whatever this panel was opened
     * for (an example typed into it, another app's text) and whatever stood under its empty field before. The screen came
     * in this moment: the Enter that asked for it, pressed again at once, answers nothing on it ([seen]).
     */
    fun again() {
        if (demo || playing != null) return
        guided = false; carries = false
        step { FirstRun.againHere(it) }
        // (What stood under the empty field in this opening gives its place to the stage: `Under`'s order.)
        zeroStood = false; zeroUp = false; ended = false; later = false; taken = null; flash = null
        resuggest()
        exemplify()
        // The field is emptied first: emptying it brings into view, whole, a screen that came while something was typed
        // ([search]), and that would undo the way this one comes. It follows a list: it is set down once the rows have
        // begun to fade, with its own answer armed, also where it is the screen that stood here before.
        empty()
        unseen = false
        stageArmed = FirstRun.armed(first())
        came(SetDown.Comes.AFTER_LIST)
    }

    // ---- first run: the opening piece, played

    /**
     * The opening piece begins with this panel (core `FirstRun.overture` said so): said by the activity before anything
     * asks what stands. The welcome stands first; what the show needs of this device is worked out meanwhile, off the
     * main thread. [at]: debug builds, the welcome's clock stands at that moment and nothing follows.
     */
    fun begin(at: Float? = null) {
        // (A search still on its way must not land among the show's rows: the show types the same words a user might have.)
        job?.cancel()
        stop()
        began = true; playing = FirstRun.Playing.WELCOME; standsAt = at; unseen = false
        rounds++
        // (Whatever goes wrong in gathering the cast, the welcome stands and then lands in the key's step: it must not take the panel down.)
        if (cast == null && casting == null) casting = scope.launch {
            cast = try { app.cast() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null }
        }
        // It does not come again once it has played for two seconds, or has ended: a panel that loses the focus in its first moment has not used it up.
        marking?.cancel()
        // (Two seconds of the piece in view: counted from the glass's opening, which at Slow is half a second after the panel is made.)
        marking = if (at == null) scope.launch { androidx.compose.runtime.snapshotFlow { arrived }.first { it }; delay(SHOWN_AFTER_MS); if (playing != null) shown() } else null
    }
    /** The two seconds after which a piece that is still playing counts as shown: not counted on once the panel closes ([closing]). */
    private var marking: Job? = null

    /** The piece has been shown: it does not come again in this run (core `FirstRun.shown`). Where there is no run, nothing is written. */
    private fun shown() { if (first().run != FirstRun.Run.NONE) step { FirstRun.shown(it) } }

    /**
     * The piece stops performing: the one place for every way it ends or begins anew (a typed character, the landing, a
     * panel that closes, a hook that plays it again). Booklight's hand is off the field, nothing of the welcome's is
     * left to press, and what the show had put into the field and the list goes in one change, so that none of it is
     * taken for the user's. Answers where the piece was. What follows is the caller's: where [playing] goes, and when the
     * screen that stands came.
     */
    private fun stop(): FirstRun.Playing? {
        val was = playing
        performer?.cancel(); performer = null
        cued = false; cueUp = false; marked = false; standsAt = null; glideFrom = null
        letGo()
        if (was == FirstRun.Playing.SHOW) clear()
        return was
    }

    /** What was Booklight's goes from the field and from the list, in one change. */
    private fun clear() {
        chip = null; word = null; via = null; origin = null; act = null; held = null; touched = false
        query = ""; results = emptyList(); selected = 0; armed = 0; cell = 0
        resultsFor = For(null, ""); whenReady = null
    }

    /**
     * A key or a click while the piece plays (core `FirstRun.pressed`). True: the piece has used it up, and it reaches
     * nothing else. False: nothing plays; or it is a typed character, the piece is over, and the character goes to the
     * field as on any day. [again]: a held key repeating: one press, one step.
     */
    fun press(press: FirstRun.Press, again: Boolean = false): Boolean {
        val on = playing ?: return false
        if (again && press != FirstRun.Press.TYPES) return true
        // (Where this device has nothing to show, the cue has no show to begin: it sets the key's step down.)
        when (FirstRun.pressed(on, press, casts)) {
            FirstRun.Ends.NOTHING -> {}
            FirstRun.Ends.BEGIN -> cued = true
            FirstRun.Ends.LAND -> land()
            FirstRun.Ends.TYPE -> { typed(); return false }
        }
        return true
    }

    /**
     * The piece ends in this call for a typed character. What was Booklight's goes from the field and from the list (its
     * rows fade where they stand, as rows do), so the character is the whole text and its list lands as any list. The
     * key's step stands under the emptied field from this moment: it came now, and its keys count a moment later
     * ([seen]). The character follows in the same turn and takes its place before anyone saw it ([type], whose
     * [covered] says so): it then comes into view when the field is empty again, and counts from then. Should no
     * character follow after all (a key that only begins one), nothing has covered it: the key's step stands, came this
     * once, and is said once. (That it has not been in view is [covered]'s to say, where something really covers it:
     * said here as well, a screen that nothing covered would come a second time at the next emptying of the field.)
     */
    private fun typed() {
        stop()
        playing = null
        came(SetDown.Comes.WHOLE)
        shown()
    }

    /**
     * The welcome has handed over: Booklight performs the show from core's script (`Show.script`), a cue at a time.
     * Nothing in it is run, looked up or fetched: the rows are the cast's, which carry nothing to run, and the engine is
     * not asked. Without a cast there is no show: the key's step lands. [only]: debug builds, the show up to that moment
     * at once, and then it stands.
     */
    fun show(only: Show.Still? = null) {
        if (playing != FirstRun.Playing.WELCOME) return
        val c = cast
        if (c == null) {
            // (Only a debug hook asks before the cast is there: the welcome itself waits for it.)
            if (casting?.isActive == true) scope.launch { casting?.join(); show(only) } else land()
            return
        }
        playing = FirstRun.Playing.SHOW; cued = false; cueUp = false; standsAt = null
        // (The show shows nothing this installation has switched off: with "Show sums" off the sum's beat is left out.)
        val script = Show.script(app.getString(R.string.first_sum_example).takeIf { settings.showSums }, c.keyword, c.comma)
        if (only != null) { Show.until(script, only).forEach { perform(it.step, c) }; return }
        performer = scope.launch {
            var at = 0
            for (cue in script) {
                if (cue.at > at) { delay(hold((cue.at - at).toLong())); at = cue.at }
                perform(cue.step, c)
            }
        }
    }

    /** One step of the show: the field, the list, the selection, the arming or the grid's cell as a key of the user's would leave them. */
    private fun perform(step: Show.Step, c: BooklightApp.Cast) {
        fun list(rows: List<Result>) { results = rows; selected = 0; armed = 0; cell = 0; resultsFor = For(SHOWS, ""); whenReady = null }
        when (step) {
            Show.Step.Letter -> query = c.letter
            is Show.Step.Typed -> query = step.text
            Show.Step.Apps -> list(c.apps)
            Show.Step.Down -> if (selected < results.lastIndex) { selected++; armed = current?.armed ?: 0 }
            Show.Step.Tab -> current?.let { r -> stops(r).let { st -> if (st.size > 1) armed = st[(st.indexOf(armed).coerceAtLeast(0) + 1) % st.size] } }
            is Show.Step.Sum -> list(listOf(c.sum(query, step.answer)))
            Show.Step.Flight -> list(listOf(c.flight))
            // The keyword's space: the keyword is the chip, the field is empty under it, and the list is its grid.
            Show.Step.Grid -> { chip = c.scope; word = c.keyword; query = ""; list(listOf(c.grid)) }
            is Show.Step.Cell -> (current?.body as? Body.Grid)?.let { g ->
                val to = cell + step.dx + step.dy * g.columns
                if (to in 0 until minOf(g.cells.size, g.columns * 5)) cell = to
            }
            Show.Step.Lap -> laps++
            Show.Step.Land -> land()
        }
    }

    /**
     * The key's step is set down, from wherever the piece is: the show's own last step, or a press that is not a typed
     * character. What was Booklight's goes from the field and the list in one change, and the screen that is due stands.
     * It came now, not when the stored state first said so: its keys count once it has been in view for a moment
     * ([seen]), and for that long the piece is still [FirstRun.Playing.LANDING] and uses every press up.
     */
    fun land() {
        val was = playing ?: return
        if (was == FirstRun.Playing.LANDING) return
        // The lower edge waits while what was Booklight's fades where it stands: it cuts no row and no cell, and nothing of
        // a welcome that is put out while its glass is tall.
        val held = if (results.isNotEmpty() || was == FirstRun.Playing.WELCOME) Metrics.height(this) else null
        // The one highlight goes on: from the grid's square, or from the list's pill, into the armed answer of the screen that
        // lands. Where that screen arms none, or nothing was highlighted (the welcome), none travels.
        val from = highlight()?.takeIf { due != null && FirstRun.armed(first()) >= 0 }
        // (After a show `stop` has taken Booklight's text and rows away; under a welcome the field holds nothing of anyone's.)
        if (stop() != FirstRun.Playing.SHOW) clear()
        if (held != null) keep(held, Motion.LANDS_AFTER_MS)
        answerAt = null
        glideFrom = from
        playing = FirstRun.Playing.LANDING; unseen = false
        resuggest()
        // (The screen that lands has its own answer armed: the one the highlight travels into, whose words come first.)
        stageArmed = FirstRun.armed(first())
        came(SetDown.Comes.LANDS)
        shown()
        // (The landing lasts until the key's step is in view: until then every press but a typed character is the piece's, and used up.)
        scope.launch { delay(lasts(marks.lead.toLong()) + FirstRun.SEEN_MS); if (playing == FirstRun.Playing.LANDING) playing = null }
    }

    // ---- first run: the lessons

    /**
     * What Enter does with [a] on [r] now (core `FirstRun.enters`). While a lesson stands nothing opens: an app's Open and
     * a search inside an app are practice, and a sum is copied for real with the panel staying. On every other day, and
     * for everything else, as always. (Where Settings stands in for an app that can be searched, a settings page entered in
     * any lesson is practice; until this device's example is worked out it is too: nothing opens in a lesson, and that is
     * the side to err on for a moment.)
     */
    fun enters(r: Result, a: Action): FirstRun.Enter =
        if (lesson == null) FirstRun.Enter.AS_ALWAYS else FirstRun.enters(first(), app.guide.used(chip, r, a), a.effect, searchable = example?.enter ?: false)

    /**
     * The coach line for the footer's left end (core `FirstRun.coach`): while a lesson's list is typed, the next key and
     * what it does today, by the row that is selected and the action that is armed. Null where no lesson stands, and
     * where no list does.
     */
    val coach: FirstRun.Coach? get() {
        if (lesson == null || results.isEmpty()) return null
        // On the arrow's stop Enter opens the row's list: `chosen` would say the row's first action there, and the line
        // would promise an Enter that does not come.
        val picked = if (onList != null) null else chosen()
        val enter = picked?.let { (r, a) -> enters(r, a) } ?: FirstRun.Enter.AS_ALWAYS
        return FirstRun.coach(first(), enter, picked?.second?.effect, search = current?.actions?.any { (it.effect as? Effect.EnterScope)?.act == Act.SEARCH } == true)
    }

    /** The coach line has the footer's left seat in this frame: said by the line itself as it is drawn, for a word that comes into that seat (`Footer`). */
    var coachUp = false

    /**
     * The field's placeholder where first run has something to say there: what to type, while a lesson's stage stands;
     * and once the choices were left, for the rest of this opening, how to learn more. Null: the ordinary one. (Under a
     * chip the chip's own stands, [hint].)
     */
    val firstHint: String? get() = when (stage) {
        FirstRun.Screen.L2 -> app.getString(R.string.first_open_hint)
        FirstRun.Screen.L3 -> example?.search?.let { FirstRun.head(it, FirstRun.MOST_LETTERS) }?.takeIf { it.isNotEmpty() }?.let { app.getString(R.string.first_search_hint, it) }
        FirstRun.Screen.L4 -> app.getString(R.string.first_sum_example)
        else -> when {
            // The welcome has no placeholder: the field is the lamp's head, and nothing is written on it.
            playing == FirstRun.Playing.WELCOME -> ""
            ended -> app.getString(R.string.first_end_hint)
            // The steps began to wait in the Booklight window in this opening: where they are.
            later -> app.getString(R.string.first_later_hint)
            // Where the opening piece is this opening's and is not played, its title greets here, over the key's step.
            greets && stage?.step == FirstRun.Step.KEY -> app.getString(R.string.first_hello_title)
            else -> null
        }
    }

    // ---- first run: the choices, and the ending

    /**
     * First run's choices are the list (docs/design/first-run/design.md §7): two rows under the empty field with none
     * selected, as the usual rows stand at rest. "Show your usual" with the panel's switch, and the list of everything.
     */
    var choicesUp by mutableStateOf(false); private set
    /** The choices were left in this opening, and first run with them: the field's placeholder says how to learn more. */
    var ended by mutableStateOf(false); private set
    /** A list stands under the empty field with nothing selected (the usual rows, first run's choices): Down, Tab or the pointer brings the highlight. */
    val atRest: Boolean get() = (zeroUp || choicesUp) && current == null

    /**
     * The choices come, where they are what is due and the field is empty with nothing under it: when their turn comes in
     * the open panel, and for a panel that opens on them. True if they stand.
     */
    fun offerChoices(): Boolean {
        if (choicesUp) return true
        if (due != FirstRun.Screen.C || query.isNotEmpty() || chip != null || results.isNotEmpty() || opened != null) return false
        results = choiceRows(); selected = -1; armed = 0; cell = 0
        choicesUp = true
        resultsFor = For(null, query); whenReady = null
        // The Enter that answered the question must not also be the one that ends the choices: only a new press, a moment later.
        filledAt = SystemClock.uptimeMillis()
        // They come onto the glass in this moment, whenever their turn came (it can come while a list is still typed on, and
        // the choices then stand only once the field is empty): their keys count from now ([seen]).
        came(SetDown.Comes.WHOLE)
        return true
    }

    private fun choiceRows(): List<Result> = listOf(
        Result(
            id = "first:usual", provider = FIRST, kind = Kind.OTHER, title = app.getString(R.string.set_usual_show), subtitle = app.getString(R.string.first_usual_text),
            icon = io.github.kuscher.booklight.core.Icon.Symbol("list"), score = 1.0, learnable = false,
            actions = listOf(Action("usual", app.getString(R.string.set_usual_show), USUAL, keepOpen = true, symbol = "list")),
            body = Body.Switch(settings.zero, app.getString(if (settings.zero) R.string.first_on else R.string.first_off)),
        ),
        // The list of everything, as its own row is everywhere (a scope's row: Enter or Tab enters it), with its key at the row's end.
        Result(
            id = "first:help", provider = FIRST, kind = Kind.SCOPE, title = app.getString(R.string.help_title), subtitle = app.getString(R.string.help_about),
            icon = io.github.kuscher.booklight.core.Icon.Symbol("booklight"), score = 1.0, learnable = false, label = HELP,
            actions = listOf(Action("enter", app.getString(R.string.action_open), Effect.EnterScope("help"), keepOpen = true, symbol = "list")),
        ),
    )

    /** Enter on "Show your usual": the switch flips where it stands, and the panel stays. The rows themselves come from the next opening on. */
    private fun flipUsual() {
        change { it.copy(zero = !it.zero) }
        settings = app.prefs.now
        val on = current?.id
        results = choiceRows()
        selected = results.indexOfFirst { it.id == on }
        // (A screen reader is told the switch's new state: the row does not change its place, and may not have the reader's focus.)
        results.firstOrNull { it.body is Body.Switch }?.let { tell(app.getString(R.string.a11y_selected, it.title, (it.body as Body.Switch).word)) }
    }

    /**
     * The choices are left, however they are left (Enter with nothing selected, a typed letter, the row that leads to the
     * list of everything): first run is over (core `FirstRun.answer`).
     */
    private fun leaveChoices() {
        if (!choicesUp) return
        choicesUp = false; ended = true
        step { FirstRun.answer(it, FirstRun.Answer.DONE) }
    }

    /**
     * Enter on the choices with nothing selected: "Done". The rows go, and what is left is the bare field; once the glass
     * has folded to it the light runs its one lap round it (motion.md T10). Not where a letter was typed meanwhile.
     */
    private fun fold() {
        leaveChoices()
        results = emptyList(); selected = 0; armed = 0; cell = 0
        resultsFor = For(null, query)
        folding?.cancel()
        folding = scope.launch { delay(lasts(Motion.FOLD_LAP_MS)); if (ended && bare && !typedSince) laps++ }
        typedSince = false
    }
    /** Something was typed since the choices were folded away: the fold's light does not come over a list. */
    private var typedSince = false
    /** The wait for the fold's lap of light: given up when the panel closes ([closing]), which the light does not run over. */
    private var folding: Job? = null

    /**
     * The panel is closing: choices that stand are left by that too. (The list itself stays as it is: it goes with the
     * glass.) Not choices that were not in view yet (a panel closed as it opened; a second click that fell outside the
     * glass as it shrank round them): those stand again at the next opening.
     */
    fun closing() {
        // (A piece that had not played its two seconds when the panel closed is not used up: it plays at the next opening.)
        marking?.cancel(); marking = null
        // Nor does it play on over a glass that folds away: should the leave be turned round (the key again, text another app
        // hands over), an ordinary panel stands, and nothing of Booklight's own is written over what it then holds.
        // (Only where a piece plays: once it has landed, a height that is held is not the piece's to end.)
        if (playing != null) {
            stop()
            playing = null
            // The key's step is what stands now, and it came in this moment: no key pressed for the piece answers it.
            came(SetDown.Comes.WHOLE)
        }
        // The highlight that may still be on its way into the key's step goes with the glass, also once the piece has
        // landed: should the leave be turned round, the screen stands whole with its own answer lit ([turned]), and
        // nothing travels on towards it. (What draws it lets go in the same frame, `Glide`: one highlight, never two.)
        glideFrom = null
        // (The fold's lap of light is not asked for over a glass that goes. Nor is the key's: "your key" is up and has its
        // check from here on, so that a leave turned round shows "Your key works" at rest, not a key held down for good.)
        folding?.cancel(); folding = null
        landed?.cancel(); landed = null; keyDown = false; keyChecked = true
        if (choicesUp && seen) step { FirstRun.answer(it, FirstRun.Answer.DONE) }
    }

    /**
     * A panel that was leaving stands again (the key, pressed while it folded): what stands under its field came in this
     * moment, and takes a key a moment later ([seen]). A key pressed at a glass that was going must not answer anything.
     */
    fun turned() { came(SetDown.Comes.WHOLE) }

    /** A lesson's Enter that was taken: what the field held, and the row and the action it was pressed on. */
    private data class Taken(val on: For, val row: String?, val armed: Int)
    private var taken by mutableStateOf<Taken?>(null)

    /**
     * The armed slot of the selected row shows as pressed: a lesson's Enter was taken there, nothing was run (or a sum was
     * copied and the panel stays), and its list is about to give way.
     */
    val pressed: Boolean get() = taken.let { it != null && it == Taken(For(chip?.key, query, act), current?.id, armed) }

    /** A lesson's Enter did [enter]: that lesson is over at once (core `FirstRun.ran`), while its list still stands for a moment ([giveWay]). */
    fun ran(enter: FirstRun.Enter, r: Result, a: Action) {
        step { FirstRun.ran(it, enter) }
        // The row and the action that were run: Ctrl + digit runs another row than the selected one, and that one's slot must not look pressed.
        taken = Taken(For(chip?.key, query, act), r.id, r.actions.indexOf(a))
    }

    /**
     * The list of a lesson that is over gives way to what stands next: the field is emptied in one change, and the next
     * screen's stage is there. Not where the user has typed on since that Enter: then it comes when the field is empty again.
     */
    fun giveWay() {
        val was = taken ?: return
        taken = null
        if (was.on != For(chip?.key, query, act)) return
        flash = null
        // The field is emptied first: emptying it brings into view, whole, a screen that came while something was typed
        // ([search]), as this one did, and that would undo the way it comes. What stands next follows this list: it comes
        // in this moment, and is set down once the rows have begun to fade.
        empty()
        unseen = false
        came(SetDown.Comes.AFTER_LIST)
    }

    /** Booklight empties the field itself: chip, text and rows go in one change, and nothing of it counts as typed. */
    private fun empty() {
        typist?.cancel(); typist = null
        answering?.cancel(); answering = null; thinking = false; whenAnswered = null; whenReady = null
        chip = null; word = null; via = null; origin = null; act = null; held = null; foreign = false; touched = false
        query = ""
        search()
    }

    /** What [r] is about, by its name, for the footer's word: the app, also for a line of one of its row's two lists and for a search inside it. */
    fun named(r: Result): String = when {
        r.kind == Kind.ACTION -> closed.firstOrNull { it.id == opened }?.title ?: r.title
        r.id.startsWith(SearchEngine.IN_APP) -> r.label ?: (chip as? AppChip)?.name ?: r.title
        else -> r.title
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
     * it is. Never as part of the opening, and never while first run's stage stands.
     */
    fun offerTip() {
        // The copy's line has the place: the tip keeps its turn for another opening.
        if (demo || guided || typedYet || tip != null || copy != null || query.isNotEmpty() || chip != null || results.isNotEmpty() || !settings.tips || stage != null || playing != null) return
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
        // (First run's opening piece has the place as a stage has it.)
        if (Under.choose(untouched, stage != null || playing != null, fresh, settings.zero, zeroSeats, tip = false, last) != Under.What.USUAL) return false
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
        if (zeroUp || choicesUp) {
            when {
                selected > 0 -> move(-1)
                selected == 0 && opened == null -> if (!again) { moved(); selected = -1; armed = 0; cell = 0 }
                // (From first run's choices at rest Up goes nowhere: the last text would take their place, and end them.)
                selected < 0 -> if (!again && zeroUp) restoreLast()
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
        if (demo || guided || copyGone || (typedYet && !back) || tip != null || query.isNotEmpty() || chip != null || results.isNotEmpty() || stage != null || playing != null) return false
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
    val bare: Boolean get() = chip == null && query.isEmpty() && results.isEmpty() && stage == null && tip == null

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
        // First run's opening piece ends for a typed character, by whichever way it comes (a key, the input method, a debug hook).
        // (A change of the text, that is: the editor also reports where its caret stands, with the same text, and it does so
        // after the show's own letters.)
        if (playing != null && text != query) press(FirstRun.Press.TYPES)
        if (text == query) return
        // A letter typed on a screen of first run takes its place in this frame, whatever of it has come ([covered]).
        // And the lower edge turns to the typed list's height from where it is (motion.md T7): for the frame or two until
        // that list lands the glass keeps the stage's height, in real time: also with the system's animations off, where
        // it would else step down to the bare field and up again. (Not for a space alone: no list follows it, and the
        // glass would stand tall and empty.)
        covered()
        if (stage != null && text.isNotBlank()) keep(Metrics.height(this), LIST_LANDS_MS, real = true)
        typedSince = true
        leaveChoices()                      // a typed letter takes the place of first run's choices: they are done
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
        covered()
        leaveChoices()
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
        covered()
        leaveChoices()
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
            // (First run's choices stand until they are left: a list made anew for the empty field is theirs again.)
            results = if (choicesUp) choiceRows() else if (zeroStood) usualRows() else emptyList()
            zeroUp = !choicesUp && results.isNotEmpty()
            selected = if (zeroUp || choicesUp) results.indexOfFirst { it.id == was } else 0
            armed = current?.armed ?: 0; cell = 0; resultsFor = For(null, text); whenReady = null
            // Back out of an app's chip that was entered from one of the usual rows: that row again, the action that was left armed.
            back?.takeIf { zeroUp }?.place(results, ::stops)?.let { (row, action) -> selected = row; armed = action }
            // A screen of first run that has not been in view ([unseen]: it came under a typed list, or was typed over before
            // it had been in view, as the key's step is where the opening piece ended for a typed character): it comes
            // into view now, with the empty field, and takes a key a moment later ([seen]).
            if (unseen && text.isEmpty()) { unseen = false; came(SetDown.Comes.WHOLE) }
            // First run's choices, where their turn has come (a lesson's list has just given way; or it came while something
            // was typed): they stand now that the field is empty.
            offerChoices()
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
                // (The list for a letter typed on first run's stage has landed: the glass goes to its height from the stage's.)
                if (playing == null && heldHeight != null) letGo()
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
        if (demo || playing != null || !onScreen) return
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

    /**
     * A minute has passed while a flight's row counts minutes ("Lands in 4 h 07 min"): it is said again for the
     * clock as it stands, and its plane takes its step. From what is kept: nothing is asked of the service.
     */
    fun minute() {
        if (demo) return
        (if (opened != null) closed else results).mapNotNull { app.flights.told(it) }.forEach { land(it) }
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
    fun enter(run: (Result, Action) -> Unit) = enter(0L, run)

    /** [made]: when the key was pressed (uptime; 0: now). First run's choices count a press by when it was made, as its stages do. */
    fun enter(made: Long, run: (Result, Action) -> Unit) {
        if (playing != null) return         // first run's opening piece: nothing of it is the user's to run but its cue ([press])
        if (typist != null) return          // Booklight is still typing: there is nothing to run yet
        // The field was just filled in for the user (a scope entered from its row, an example typed): the Enter that
        // did it must not also run what it brought. Only a new press, a moment later, runs.
        if (SystemClock.uptimeMillis() - filledAt < CONFIRM_GAP_MS) return
        if (!onScreen) { whenReady = run; return }
        // First run's choices, like a stage, take a key only once they have been in view for a moment: a key pressed before
        // that, and handled a long frame later, is still one pressed before that.
        if (choicesUp && !taken(made, enter = false)) return
        // First run's choices with nothing selected: Enter is "Done", and the list folds to the bare field.
        if (choicesUp && current == null) { fold(); return }
        // On Window or on the row's arrow: Enter opens that stop's lines as a list, or closes them again.
        onList?.let { if (opened != null && list == it) close() else open(it); return }
        val (r, a) = chosen() ?: return
        // The switch of first run's choices is the panel's own: it flips where it stands, and nothing runs.
        if (a.effect == USUAL) { flipUsual(); return }
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
     * [made]: when the key was pressed (uptime; 0: now).
     */
    fun fill(made: Long = 0L): Boolean {
        if (!onScreen) return false
        // (On the second row of first run's choices this enters the list of everything, and the run is over: like Enter
        // there, only once the choices have been in view for a moment, by when the key was pressed.)
        if (choicesUp && !taken(made, enter = false)) return false
        val e = chosen()?.second?.effect as? Effect.EnterScope ?: return false
        into(e)
        return true
    }

    /** Ctrl + a digit: that row's first action, straight away. Never one that removes something, and only for rows on screen. */
    fun runRow(n: Int, made: Long = 0L, run: (Result, Action) -> Unit) {
        if (!onScreen || playing != null) return
        if (choicesUp && !taken(made, enter = false)) return      // first run's choices take a key only once they are in view, as Enter has it
        val r = results.getOrNull(n)?.takeIf { it.body !is Body.Grid } ?: return
        val a = r.actions.firstOrNull()?.takeIf { !it.danger && !it.confirm } ?: return
        (a.effect as? Effect.EnterScope)?.let { into(it, r to a); return }
        if (a.effect == PromptScope.ASK || a.effect is Effect.Ask || needsAnswer(a.effect) || (r.body as? Body.Stream)?.busy == true) return     // an answer is Enter's
        if (a.effect is Effect.Unsuggest) return        // and so is "Don't suggest"
        if (a.effect == USUAL) { flipUsual(); return }  // the switch of first run's choices flips where it stands
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
        covered()
        // (On a screen of first run the last text takes the stage's place as a typed letter does: the glass keeps the
        // stage's height until that text's list lands, [type].)
        if (stage != null && last.text.isNotBlank()) keep(Metrics.height(this), LIST_LANDS_MS, real = true)
        val s = last.key?.let { app.engine.scope(it) }
        // (An app's chip comes back as it was left: the action that was armed, and the keyword it was entered by.)
        if (s != null) { enterScope(s, last.text, last.word, last.act); via = last.via?.let { app.engine.scope(it) } } else { query = last.text; search() }
        return true
    }

    /** The panel is closing without having run anything: keep what was typed for [restoreLast]. */
    fun keep() {
        hideTip()
        if (playing != null) return         // what Booklight typed in first run's show is nobody's last text
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
        /** The rows of first run's choices are the panel's own: no provider made them. */
        private const val FIRST = "first"
        /** What the switch of first run's choices does: caught by the panel before anything runs. */
        private val USUAL = Effect.Internal("usual")
        /** First run's opening piece does not come again once it has played for this long, or has ended. */
        private const val SHOWN_AFTER_MS = 2000L
        /** How many of first run's sentences for a screen reader are kept for the debug hook. */
        private const val SAID = 8
        /**
         * A list lands a frame or two after its letter: the glass waits no longer than this for one before it goes to its own
         * height. Real time: the engine is no quicker with the system's animations off, and no slower in a debug build's slow motion.
         */
        private const val LIST_LANDS_MS = 300L
        /** In place of the moment the panel was uncovered, for a screen that came under the system's dialog and is still under it ([uncoveredAt]). */
        private const val COVERED = Long.MAX_VALUE
        /** What the list is said to be for while first run's show performs: never what the field holds, so nothing takes its rows for the rows of what is typed. */
        private const val SHOWS = "first:show"
    }
}
