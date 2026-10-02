package io.github.kuscher.booklight.data

import android.content.Context
import android.util.Log
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.core.History
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Keeps what Booklight has learned ([History]) in `files/history.json`. Loaded once at start
 * (it is small); saved a moment after each pick, off the main thread.
 */
class HistoryStore(context: Context, private val scope: CoroutineScope) {
    @Serializable private data class E(val c: Double, val t: Long)
    @Serializable private data class Saved(val v: Int = 1, val items: Map<String, E> = emptyMap(), val latches: Map<String, Map<String, E>> = emptyMap(), val leads: Map<String, Int> = emptyMap())

    private val file = File(context.filesDir, "history.json")
    private val json = Json { ignoreUnknownKeys = true }
    private var pending: Job? = null
    private val writing = Mutex()

    val history: History = load()

    private fun load(): History = try {
        if (!file.exists()) History() else {
            val s = json.decodeFromString<Saved>(file.readText())
            History(History.Data(
                s.items.mapValues { History.Entry(it.value.c, it.value.t) },
                s.latches.mapValues { m -> m.value.mapValues { History.Entry(it.value.c, it.value.t) } },
                s.leads,
            ))
        }
    } catch (e: Exception) {
        Log.w(BooklightApp.TAG, "history unreadable (${e.javaClass.simpleName}), starting fresh")
        History()
    }

    /** Call after a pick. Writes are batched: a burst of picks is one write. */
    fun changed() {
        pending?.cancel()
        pending = scope.launch(Dispatchers.IO) {
            delay(500)
            val d = history.data()
            val s = Saved(1, d.items.mapValues { E(it.value.count, it.value.last) }, d.latches.mapValues { m -> m.value.mapValues { E(it.value.count, it.value.last) } }, d.leads)
            // One write at a time, and a full disk loses this save, not the app.
            writing.withLock {
                runCatching {
                    val tmp = File(file.parentFile, file.name + ".tmp")
                    tmp.writeText(json.encodeToString(Saved.serializer(), s))
                    tmp.renameTo(file)
                }.onFailure { Log.w(BooklightApp.TAG, "history not saved", it) }
            }
        }
    }

    fun clear() { history.clear(); changed() }
}
