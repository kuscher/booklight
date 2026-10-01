package io.github.kuscher.booklight.overlay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The panel's state: the typed text, the ranked rows, which row is selected, and whether the
 * selected row's actions are showing. The UI only draws this; keys and clicks call in here.
 */
class OverlayModel(private val app: BooklightApp, private val scope: CoroutineScope) {
    var query by mutableStateOf(""); private set
    var results by mutableStateOf<List<Result>>(emptyList()); private set
    var selected by mutableIntStateOf(0); private set
    /** Set while the actions of a row are listed instead of the results (Tab). */
    var actionsOf by mutableStateOf<Result?>(null); private set
    var actionIndex by mutableIntStateOf(0); private set
    /** A word shown in the footer for a moment ("Copied"). */
    var flash by mutableStateOf<String?>(null)
    /** How long the last search took, for `./bl debug dump`. */
    var lastSearchMicros by mutableLongStateOf(0); private set

    private var job: Job? = null

    init { search("") }

    val current: Result? get() = results.getOrNull(selected)

    fun type(text: String) {
        if (text == query) return
        query = text
        actionsOf = null
        search(text)
    }

    private fun search(text: String) {
        job?.cancel()
        job = scope.launch(Dispatchers.Default) {
            val t0 = System.nanoTime()
            val r = app.engine.search(Query(text))
            val took = (System.nanoTime() - t0) / 1000
            withContext(Dispatchers.Main.immediate) { results = r; selected = 0; lastSearchMicros = took }
        }
    }

    fun move(by: Int) {
        if (actionsOf != null) {
            val n = actionsOf!!.actions.size
            actionIndex = (actionIndex + by).coerceIn(0, n - 1)
        } else if (results.isNotEmpty()) {
            selected = (selected + by).coerceIn(0, results.size - 1)
        }
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

    /** Remember the pick, so the same text finds it first next time. */
    fun learn(r: Result) {
        app.engine.picked(Query(query), r)
        app.historyStore.changed()
    }
}
