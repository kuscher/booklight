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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SiteEntry(val keyword: String, val name: String, val url: String) {
    fun site() = Site(keyword, name, url)
}

/** A piece of text with a short name: `snip sig` copies it. */
@Serializable
data class SnippetEntry(val key: String, val text: String)

/** One step of a recipe, as saved: [kind] says which effect, the rest are its values (`data/Recipes.kt`). */
@Serializable
data class StepEntry(val kind: String, val a: String = "", val b: String = "", val n: Int = 0, val label: String = "")

/** Several things done in order under one name. */
@Serializable
data class RecipeEntry(val id: String, val name: String, val keyword: String = "", val steps: List<StepEntry> = emptyList())

/** Everything the user can choose, plus which first-run cards are still to show. */
@Serializable
data class Settings(
    val engine: String = Engines.default.id,
    /** Off until the user turns it on: with it on, typed text goes to the search engine for suggestions. */
    val suggestions: Boolean = false,
    val showSettings: Boolean = true,
    val showSums: Boolean = true,
    /** Offer "Ask Gemini" for longer text. */
    val showGemini: Boolean = true,
    /** How see-through the panel is: clear, balanced, frosted or solid (`overlay/Glass.kt`). */
    val glass: String = "balanced",
    /** `auto` follows the system; `light` and `dark` don't. */
    val theme: String = "auto",
    /** Colours from the wallpaper (the system's), or Booklight's own. */
    val tint: Boolean = true,
    /** Darken the rest of the screen a little while the panel is open. */
    val dim: Boolean = false,
    /** How the panel arrives: `off` (a small settle and a fade), or unfolding `fast`, `medium` or `slow`. */
    val opening: String = "fast",
    /** The keyword searches and links: 1.0's sites, now with placeholders. */
    val sites: List<SiteEntry> = Sites.defaults.map { SiteEntry(it.keyword, it.name, it.url) },
    val snippets: List<SnippetEntry> = emptyList(),
    val recipes: List<RecipeEntry> = emptyList(),
    /** The folder notes go to, as the tree address the user granted; null until they have. */
    val notesFolder: String? = null,
    val emojiRecent: List<String> = emptyList(),
    /** What other apps offer (their shortcuts, their commands for Booklight) shows as rows; and the apps the user turned off one by one. */
    val appCommands: Boolean = true,
    val mutedApps: List<String> = emptyList(),
    /** Other apps' keywords the user has entered once from their row: from then on the keyword and a Space enters them. */
    val usedScopes: List<String> = emptyList(),
    val shortcutCard: Boolean = true,
    val suggestionsCard: Boolean = true,
    /** The shape of this file: 1 = Booklight 1.0, 2 = 1.1. */
    val schema: Int = 1,
) {
    fun engine(): Engine = Engines.byId(engine)
    fun sites(): List<Site> = sites.map { it.site() }
}

/** The settings as `files/settings.json`: read once at start, written off the main thread on change. */
class Prefs(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "settings.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val writing = Mutex()
    private companion object { const val SCHEMA = 2 }
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state
    val now: Settings get() = _state.value

    private fun load(): Settings = try {
        if (file.exists()) migrate(json.decodeFromString(Settings.serializer(), file.readText())) else Settings(schema = SCHEMA)
    } catch (e: Exception) {
        // The file holds what the user made (links, snippets, recipes): put it aside rather than write over it.
        // Only the kind of error is logged: the message of a parse error quotes the file.
        Log.w(BooklightApp.TAG, "settings unreadable (${e.javaClass.simpleName}); kept as settings.json.bad, using defaults")
        runCatching { file.copyTo(File(file.parentFile, file.name + ".bad"), overwrite = true) }
        Settings(schema = SCHEMA)
    }

    /**
     * A 1.0 file: `play` used to search the Play Store; it plays music now and `store` searches the store
     * (unless the user already has a `store` of their own). The one default that is new in 1.1, `drive`, is
     * added; defaults the user had removed stay removed.
     */
    private fun migrate(s: Settings): Settings {
        if (s.schema >= SCHEMA) return s
        val free = s.sites.none { it.keyword == "store" }
        val renamed = s.sites.map { if (free && it.keyword == "play" && it.url.contains("play.google.com")) it.copy(keyword = "store") else it }
        val added = Sites.defaults.filter { it.keyword == "drive" && renamed.none { r -> r.keyword == "drive" } }.map { SiteEntry(it.keyword, it.name, it.url) }
        return s.copy(sites = renamed + added, schema = SCHEMA)
    }

    fun update(change: (Settings) -> Settings) {
        _state.update(change)
        scope.launch(Dispatchers.IO) {
            // One write at a time; each writes the newest state, so the last one to run leaves the newest file.
            writing.withLock {
                runCatching {
                    val tmp = File(file.parentFile, file.name + ".tmp")
                    tmp.writeText(json.encodeToString(Settings.serializer(), _state.value))
                    tmp.renameTo(file)
                }.onFailure { Log.w(BooklightApp.TAG, "settings not saved", it) }
            }
        }
    }
}
