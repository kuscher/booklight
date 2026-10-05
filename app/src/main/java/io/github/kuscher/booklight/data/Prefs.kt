package io.github.kuscher.booklight.data

import android.content.Context
import android.util.Log
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.core.Kept
import io.github.kuscher.booklight.core.Seed
import io.github.kuscher.booklight.core.Seeds
import io.github.kuscher.booklight.core.Engine
import io.github.kuscher.booklight.core.Engines
import io.github.kuscher.booklight.core.FirstRun
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

/**
 * A prompt the user keeps: a name, a keyword (may be empty) and the text, with `{text}` where what is typed goes.
 * [seed]: it is still the ready-made prompt at that place of the table, word for word, and its words come from
 * the resources each time the settings are read (core `Seeds`); null for the user's own and for an edited one.
 */
@Serializable
data class PromptEntry(val id: String, val name: String, val keyword: String = "", val text: String, val seed: Int? = null)

/**
 * An app command of the user's own: a name, a keyword (it can do without one, unless it takes text), the app
 * it asks, and what it asks: an `intent:` address with `{argument}` where the typed text goes (core `Requests`).
 */
@Serializable
data class OwnCommandEntry(val id: String, val name: String, val keyword: String = "", val app: String, val intent: String)

/** Everything the user can choose, plus where first run stands. */
@Serializable
data class Settings(
    val engine: String = Engines.default.id,
    /** Off until the user turns it on: with it on, typed text goes to the search engine for suggestions. */
    val suggestions: Boolean = false,
    val showSettings: Boolean = true,
    val showSums: Boolean = true,
    /** The system's own keyboard shortcuts as answers ("snap"). */
    val showKeys: Boolean = true,
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
    /** The shadow around the panel: `off`, `low`, `medium` or `high` (`overlay/Glass.kt`). */
    val shadow: String = "medium",
    /** How the panel arrives: `off` (a small settle and a fade), or unfolding `fast`, `medium` or `slow`. */
    val opening: String = "medium",
    /** The keyword searches and links: 1.0's sites, now with placeholders. */
    val sites: List<SiteEntry> = Sites.defaults.map { SiteEntry(it.keyword, it.name, it.url) },
    val snippets: List<SnippetEntry> = emptyList(),
    val recipes: List<RecipeEntry> = emptyList(),
    /** Prompts: `fix teh text`. Five to start with, in the device's language when Booklight first ran. */
    val prompts: List<PromptEntry> = emptyList(),
    /**
     * App commands: `sp daft punk` asks one app for something, put together in the Booklight window. They are made
     * there and nowhere else (see [Prefs], where this list is read).
     */
    val ownCommands: List<OwnCommandEntry> = emptyList(),
    /** A line under the empty field for something copied in the last two minutes: what kind of thing it is, and Tab opens it. */
    val copyRow: Boolean = true,
    /**
     * "Your usual" under the empty field: the two things run most from Booklight and the one run last, in place of
     * the tips. Off until chosen. [zeroHidden]: what the user said "Don't suggest" for. [zeroHeld]: which two had
     * seats one and two at the last showing, so they do not swap from one opening to the next (core `Zero`).
     */
    val zero: Boolean = false,
    val zeroHidden: List<String> = emptyList(),
    val zeroHeld: List<String> = emptyList(),
    /** Since when a holder of one of those seats has had no row (its app was removed, or its kind of row switched off), by the wall clock; 0 = neither is away. */
    val zeroAway: Long = 0L,
    /** The music app `play` was last sent to, as its package: it is the one armed next time. Null until one was asked. */
    val player: String? = null,
    /** The folder notes go to, as the tree address the user granted; null until they have. */
    val notesFolder: String? = null,
    val emojiRecent: List<String> = emptyList(),
    /** The letters of other languages picked lately (`abc`), newest first. */
    val lettersRecent: List<String> = emptyList(),
    /** What other apps offer (their shortcuts, their commands for Booklight) shows as rows; and the apps the user turned off one by one. */
    val appCommands: Boolean = true,
    val mutedApps: List<String> = emptyList(),
    /** Other apps' keywords the user has entered once from their row: from then on the keyword and a Space enters them. */
    val usedScopes: List<String> = emptyList(),
    /** The lines of the list of everything (`Guide`) the user has run something of: they are not suggested again. */
    val used: List<String> = emptyList(),
    /** Tips under the empty field. On from the start; the ones that have had their turn; the one whose turn it is and how long it has been on screen. */
    val tips: Boolean = true,
    val tipsSeen: List<String> = emptyList(),
    val tipId: String = "",
    val tipMs: Long = 0,
    /** The panel has been opened the way a keyboard shortcut (or the assistant key) opens it: Booklight has a key. */
    val keySeen: Boolean = false,
    val shortcutCard: Boolean = true,
    val suggestionsCard: Boolean = true,
    /**
     * First run (core `FirstRun`): which run this installation is in (0 none, 1 a new installation's, 2 the key alone for one
     * that was there before, 3 asked for again); the steps that are over (and `show`: the opening has been shown; `change`: a
     * replay asks for another key); in how many openings the run has stood; how often the system's dialog came for the key;
     * and whether the icon's first click has shown the panel. A build from before first run drops these five when it writes
     * the settings: that step 1 is over is therefore also marked in [tipsSeen] (`FirstRun.MARK`).
     */
    val first: Int = 0,
    val firstDone: List<String> = emptyList(),
    val firstOpens: Int = 0,
    val firstHelper: Int = 0,
    val firstIcon: Boolean = false,
    /** The section the Booklight window was left on (`window/Nav.kt`): it opens there again. */
    val windowPart: String = "start",
    /** How many of the ready-made prompts this installation has been given: a version that brings a new one adds it once. */
    val seeded: Int = 0,
    /** The shape of this file: 1 = Booklight 1.0, 2 = 1.1, 3 = 2.0, 4 = 2.2, 5 = ready-made prompts by reference, 6 = app commands of the user's own, 7 = first run. */
    val schema: Int = 1,
) {
    fun engine(): Engine = Engines.byId(engine)
    fun sites(): List<Site> = sites.map { it.site() }
}

/**
 * What first run needs of the settings, as the core's own state. That step 1 is over is read from the list the mark is in.
 * Whether sums are answered ("Show sums") is only read: where they are switched off the sum's lesson is no step.
 */
fun Settings.firstRun(): FirstRun.State = FirstRun.State(FirstRun.Run.of(first), firstDone, firstOpens, firstHelper, firstIcon, keySeen, suggestions, FirstRun.MARK in tipsSeen, sums = showSums)

/** The settings with first run's state [f] in them: its own fields, `keySeen` and `suggestions` (the consent switch) too, and nothing else. The mark is only ever added. */
fun Settings.withFirstRun(f: FirstRun.State): Settings = copy(
    first = f.run.id, firstDone = f.done, firstOpens = f.opens, firstHelper = f.helper, firstIcon = f.icon, keySeen = f.key, suggestions = f.suggestions,
    tipsSeen = if (f.mark && FirstRun.MARK !in tipsSeen) tipsSeen + FirstRun.MARK else tipsSeen,
)

/**
 * Changes first run's state inside one settings update: [change] is worked out from the settings as they are at the write,
 * so a state read earlier never puts older `keySeen` or `suggestions` values back.
 */
fun Prefs.firstRun(change: (FirstRun.State) -> FirstRun.State) = update { it.withFirstRun(change(it.firstRun())) }

/**
 * Schema 7, for settings that were there before this build: no run where a key is known or step 1 is marked as over, else
 * the key's step once (core `FirstRun.forUpdate`). It reads only what every build keeps, so it gives the same answer each
 * time the same file is read, and again after an older build has written the file.
 */
fun Settings.asUpdate(): Settings = withFirstRun(FirstRun.forUpdate(firstRun()))

/** The settings as `files/settings.json`: read once at start, written off the main thread on change. */
class Prefs(private val context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "settings.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val writing = Mutex()
    private companion object {
        const val SCHEMA = 7
        /** The ready-made prompts 2.0 came with: what an installation from before schema 5 has been given. */
        const val FIRST_SEEDS = 5
    }
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state
    val now: Settings get() = _state.value

    /**
     * The rule for what is read here. An app command ([Settings.ownCommands]) and a recipe's `open` step are a
     * request to another app, kept as an `intent:` address, and Booklight runs them on one Enter. So they are only
     * ever made by the user, in the editors of the Booklight window (an app command in its editor; a recipe's step
     * chosen there from the rows Booklight itself offers: another app's declared command, a search inside an app, an
     * app's own address the user typed): this file, written by Booklight and carried by
     * the user's own backup, is the one place they are read from. An `intent:` address that arrives from outside
     * (another app, a link, a file somebody shares, a pack of commands one day) must never be put into these lists,
     * and never run, without the user having made it a command there, where every field of it can be seen. What
     * is read is still checked each time it runs (`Executor.open`): only an activity of the named app that is open
     * to other apps and asks for no permission, with nothing granted along.
     */
    private fun load(): Settings = try {
        if (file.exists()) current(migrate(json.decodeFromString(Settings.serializer(), file.readText()))) else started(Settings(schema = SCHEMA, first = FirstRun.Run.NEW.id))
    } catch (e: Exception) {
        // The file holds what the user made (links, snippets, recipes): put it aside rather than write over it.
        // Only the kind of error is logged: the message of a parse error quotes the file.
        Log.w(BooklightApp.TAG, "settings unreadable (${e.javaClass.simpleName}); kept as settings.json.bad, using defaults")
        runCatching { file.copyTo(File(file.parentFile, file.name + ".bad"), overwrite = true) }
        // (What was in the file is not known: like an installation that was there before, with no key known.)
        started(Settings(schema = SCHEMA).asUpdate())
    }

    /**
     * A 1.0 file: `play` used to search the Play Store; it plays music now and `store` searches the store
     * (unless the user already has a `store` of their own). The one default that is new in 1.1, `drive`, is
     * added; defaults the user had removed stay removed.
     */
    private fun migrate(old: Settings): Settings {
        var s = old
        if (s.schema < 2) {
            val free = s.sites.none { it.keyword == "store" }
            val renamed = s.sites.map { if (free && it.keyword == "play" && it.url.contains("play.google.com")) it.copy(keyword = "store") else it }
            val added = Sites.defaults.filter { it.keyword == "drive" && renamed.none { r -> r.keyword == "drive" } }.map { SiteEntry(it.keyword, it.name, it.url) }
            s = s.copy(sites = renamed + added)
        }
        // 2.0: the five prompts to start with. And whoever answered 1.1's first card, or has picked anything, has a key:
        // the window must not tell them "No key yet".
        if (s.schema < 3) s = started(s).copy(keySeen = !s.shortcutCard || File(context.filesDir, "history.json").length() > 8)
        // 2.2: the opening is at medium speed unless chosen otherwise. Fast was what everyone had, chosen or not.
        if (s.schema < 4 && s.opening == "fast") s = s.copy(opening = "medium")
        // The ready-made prompts are kept by reference: the ones nobody changed are recognised, in English or German.
        if (s.schema < 5) {
            val tables = listOf("en", "de").map { seeds(it) }
            s = s.copy(prompts = s.prompts.map { it.copy(seed = Seeds.of(it.kept(), tables)) }, seeded = FIRST_SEEDS)
        }
        // Schema 6: app commands of the user's own, and recipe steps that ask another app for something. A file from before
        // has none that were made here (a build from before ignored such a step): whatever it holds of either is not taken.
        // Nothing else changed its shape.
        if (s.schema < 6) s = s.copy(ownCommands = emptyList(), recipes = s.recipes.map { r -> r.copy(steps = r.steps.filter { it.kind != "open" }) })
        // Schema 7: first run. An installation that was there before gets no run, or the key's step once ([asUpdate]).
        if (s.schema < 7) s = s.asUpdate()
        return s.copy(schema = SCHEMA)
    }

    /** The ready-made prompts in the device's language, or in [language]'s. */
    private fun seeds(language: String? = null): List<Seed> {
        val res = if (language == null) context.resources else
            context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag(language)) }).resources
        return Seeds.parse(res.getStringArray(io.github.kuscher.booklight.R.array.prompt_seeds).toList())
    }

    private fun PromptEntry.kept() = Kept(id, name, keyword, text, seed)
    private fun taken(s: Settings) = (s.sites.map { it.keyword } + s.recipes.map { it.keyword } + s.ownCommands.map { it.keyword }).mapTo(HashSet()) { it.lowercase() }

    /** Each time the settings are read: the unchanged ready-made prompts in the device's language as it is now, and a new one added once. */
    private fun current(s: Settings): Settings {
        val seeds = seeds()
        val now = Seeds.current(s.prompts.map { it.kept() }, seeds, s.seeded, taken(s)).map { PromptEntry(it.id, it.name, it.keyword, it.text, it.seed) }
        return if (now == s.prompts && s.seeded >= seeds.size) s else s.copy(prompts = now, seeded = maxOf(s.seeded, seeds.size))
    }

    /**
     * What a new installation, and one that comes from 1.1, starts with: the prompts, in the
     * device's language. A keyword the user already has for a link or a recipe stays theirs: that
     * prompt comes without one, and is found by its name.
     */
    private fun started(s: Settings): Settings {
        if (s.prompts.isNotEmpty()) return s
        val seeds = seeds()
        return s.copy(prompts = Seeds.fresh(seeds, taken(s)).map { PromptEntry(it.id, it.name, it.keyword, it.text, it.seed) }, seeded = seeds.size)
    }

    /**
     * The user's own key for the flight service: what makes a flight's row answer with times. It is a
     * file of its own, `files/flights.key`, and not a line of the settings: the settings travel with
     * the user's backup and to a new device, and a key must not. It is sent to that service and
     * nowhere else, and never logged.
     */
    private val keyFile = File(context.filesDir, "flights.key")
    private val _flightKey = MutableStateFlow(runCatching { if (keyFile.exists()) keyFile.readText().trim() else "" }.getOrDefault(""))
    val flightKey: StateFlow<String> = _flightKey

    /** [key] empty takes the key away. */
    fun setFlightKey(key: String) {
        val k = key.trim()
        _flightKey.value = k
        scope.launch(Dispatchers.IO) {
            // One write at a time; each writes the newest key, so the last one to run leaves the newest file.
            writing.withLock {
                val now = _flightKey.value
                runCatching { if (now.isEmpty()) keyFile.delete() else keyFile.writeText(now) }.onFailure { Log.w(BooklightApp.TAG, "flight key not saved (${it.javaClass.simpleName})") }
            }
        }
    }

    /**
     * The user's own key for Spotify: a client id and its secret, which make `play … on spotify` find
     * the song and play it. A file of its own, `files/spotify.key`, for the flight key's reasons: it
     * is not part of a backup and does not travel to a new device. It is sent to Spotify and nowhere
     * else, never logged, and never shown again once it is in. Here: the id, a line break, the
     * secret; empty for none.
     */
    private val songFile = File(context.filesDir, "spotify.key")
    private val _spotifyKey = MutableStateFlow(runCatching { if (songFile.exists()) songFile.readText().trim().takeIf { it.lines().size == 2 }.orEmpty() else "" }.getOrDefault(""))
    val spotifyKey: StateFlow<String> = _spotifyKey

    /** Both halves, or the key is taken away. */
    fun setSpotifyKey(id: String, secret: String) {
        val i = id.trim(); val s = secret.trim()
        _spotifyKey.value = if (i.isEmpty() || s.isEmpty() || '\n' in i || '\n' in s) "" else "$i\n$s"
        scope.launch(Dispatchers.IO) {
            // One write at a time; each writes the newest key, so the last one to run leaves the newest file.
            writing.withLock {
                val now = _spotifyKey.value
                runCatching { if (now.isEmpty()) songFile.delete() else songFile.writeText(now) }.onFailure { Log.w(BooklightApp.TAG, "spotify key not saved (${it.javaClass.simpleName})") }
            }
        }
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
