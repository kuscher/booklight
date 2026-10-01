package io.github.kuscher.booklight.data

import android.content.Context
import android.util.Log
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.core.Engine
import io.github.kuscher.booklight.core.Engines
import io.github.kuscher.booklight.core.Site
import io.github.kuscher.booklight.core.Sites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SiteEntry(val keyword: String, val name: String, val url: String) {
    fun site() = Site(keyword, name, url)
}

/** Everything the user can choose, plus which first-run cards are still to show. */
@Serializable
data class Settings(
    val engine: String = Engines.default.id,
    /** Off until the user turns it on: with it on, typed text goes to the search engine for suggestions. */
    val suggestions: Boolean = false,
    val showSettings: Boolean = true,
    val showSums: Boolean = true,
    val sites: List<SiteEntry> = Sites.defaults.map { SiteEntry(it.keyword, it.name, it.url) },
    val shortcutCard: Boolean = true,
    val suggestionsCard: Boolean = true,
) {
    fun engine(): Engine = Engines.byId(engine)
    fun sites(): List<Site> = sites.map { it.site() }
}

/** The settings as `files/settings.json`: read once at start, written off the main thread on change. */
class Prefs(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "settings.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state
    val now: Settings get() = _state.value

    private fun load(): Settings = try {
        if (file.exists()) json.decodeFromString(Settings.serializer(), file.readText()) else Settings()
    } catch (e: Exception) {
        Log.w(BooklightApp.TAG, "settings unreadable, using defaults", e)
        Settings()
    }

    fun update(change: (Settings) -> Settings) {
        _state.update(change)
        val s = _state.value
        scope.launch(Dispatchers.IO) {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(Settings.serializer(), s))
            tmp.renameTo(file)
        }
    }
}
