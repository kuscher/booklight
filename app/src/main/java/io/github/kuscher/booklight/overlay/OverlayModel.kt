package io.github.kuscher.booklight.overlay

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Site
import io.github.kuscher.booklight.core.Sites
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
 * The panel's state: the typed text, the ranked rows, which row is selected, and whether the
 * selected row's actions are showing. The UI only draws this; keys and clicks call in here.
 */
class OverlayModel(private val app: BooklightApp, private val scope: CoroutineScope, private val limit: Int = 8) {
    var query by mutableStateOf(""); private set
    var results by mutableStateOf<List<Result>>(emptyList()); private set
    var selected by mutableIntStateOf(0); private set
    /** Set while the actions of a row are listed instead of the results (Tab). */
    var actionsOf by mutableStateOf<Result?>(null); private set
    var actionIndex by mutableIntStateOf(0); private set
    /** A word shown in the footer for a moment ("Copied"). */
    var flash by mutableStateOf<String?>(null)
    /** How long the last local search took, for `./bl debug dump`. */
    var lastSearchMicros by mutableLongStateOf(0); private set
    var settings by mutableStateOf(app.prefs.now); private set

    private var job: Job? = null
    /** The text [results] was made for: for a moment after a keystroke it is still the old list. */
    private var resultsFor = ""
    private var whenReady: ((Result, Action) -> Unit)? = null

    init {
        scope.launch { app.prefs.state.collect { settings = it } }
    }

    val current: Result? get() = results.getOrNull(selected)

    /** The first-run step to show, if any is left and nothing is typed. */
    val card: Card? by derivedStateOf {
        when {
            query.isNotEmpty() -> null
            settings.shortcutCard -> Card.SHORTCUT
            settings.suggestionsCard && !settings.suggestions -> Card.SUGGESTIONS
            else -> null
        }
    }

    /** The rest of the selected row's name, shown grey after the typed text ("chr" + "ome"). */
    val completion: String? by derivedStateOf {
        val r = current
        if (actionsOf != null || r == null || r.answer != null || r.kind == Kind.WEB || r.kind == Kind.SUGGESTION) null
        else Matcher.completion(query, r.title)
    }

    /** The site a keyword search goes to ("yt lofi" → YouTube), shown as a chip in the field. */
    val site: Site? by derivedStateOf { Sites.parse(query, settings.sites())?.first }

    fun type(text: String) {
        if (text == query) return
        query = text
        actionsOf = null
        search(text)
    }

    private fun search(text: String) {
        job?.cancel()
        if (text.isBlank()) { results = emptyList(); selected = 0; resultsFor = text; whenReady = null; return }
        job = scope.launch(Dispatchers.Default) {
            val t0 = System.nanoTime()
            val local = app.engine.search(Query(text), limit)
            val took = (System.nanoTime() - t0) / 1000
            withContext(Dispatchers.Main.immediate) {
                results = local; selected = 0; lastSearchMicros = took; resultsFor = text
                whenReady?.let { run -> whenReady = null; chosen()?.let { run(it.first, it.second) } }
            }
            // Suggestions come from the network: after a pause in typing, never holding up the
            // list, and dropped if the text has moved on (this job is cancelled by then).
            if (!settings.suggestions) return@launch
            delay(SUGGEST_PAUSE_MS)
            val more = app.suggest.fetch(text)
            if (more.isEmpty()) return@launch
            withContext(Dispatchers.Main.immediate) {
                if (query == text && results == local) results = app.engine.merge(local, more, limit)
            }
        }
    }

    fun move(by: Int) {
        val of = actionsOf
        if (of != null) actionIndex = (actionIndex + by).coerceIn(0, of.actions.size - 1)
        else if (results.isNotEmpty()) selected = (selected + by).coerceIn(0, results.size - 1)
    }

    fun select(index: Int) { if (index in results.indices) selected = index }
    fun selectAction(index: Int) { actionsOf?.let { if (index in it.actions.indices) actionIndex = index } }

    /** Tab: list what else the selected row can do. False when there is nothing more. */
    fun openActions(): Boolean {
        val r = current ?: return false
        if (r.actions.size < 2) return false
        actionsOf = r; actionIndex = 0
        return true
    }

    fun closeActions(): Boolean = (actionsOf != null).also { actionsOf = null }

    /** What Enter would run right now. */
    fun chosen(): Pair<Result, Action>? {
        actionsOf?.let { r -> return r.actions.getOrNull(actionIndex)?.let { r to it } }
        val r = current ?: return null
        return r.actions.firstOrNull()?.let { r to it }
    }

    /**
     * Enter. Typing is faster than the list: if Enter arrives before the rows for the current text
     * have, the first of those rows runs when they land, never a row of the previous text.
     */
    fun enter(run: (Result, Action) -> Unit) {
        if (actionsOf != null || resultsFor == query) chosen()?.let { run(it.first, it.second) }
        else whenReady = run
    }

    /** Remember the pick, so the same text finds it first next time. */
    fun learn(r: Result) {
        app.engine.picked(Query(query), r)
        app.historyStore.changed()
    }

    fun change(f: (Settings) -> Settings) = app.prefs.update(f)

    private companion object {
        const val SUGGEST_PAUSE_MS = 140L
    }
}
