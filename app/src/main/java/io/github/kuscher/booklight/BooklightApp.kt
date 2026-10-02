package io.github.kuscher.booklight

import android.app.Application
import io.github.kuscher.booklight.ai.OnDevice
import io.github.kuscher.booklight.core.Zero
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.data.HistoryStore
import io.github.kuscher.booklight.data.Notes
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.data.Recipes
import io.github.kuscher.booklight.providers.Answers
import io.github.kuscher.booklight.providers.AppCommands
import io.github.kuscher.booklight.providers.AppsProvider
import io.github.kuscher.booklight.providers.User
import io.github.kuscher.booklight.providers.CalcProvider
import io.github.kuscher.booklight.providers.CommandsProvider
import io.github.kuscher.booklight.providers.Dials
import io.github.kuscher.booklight.providers.FlightsProvider
import io.github.kuscher.booklight.providers.KeysProvider
import io.github.kuscher.booklight.providers.SettingsProvider
import io.github.kuscher.booklight.providers.SuggestProvider
import io.github.kuscher.booklight.providers.WebProvider
import io.github.kuscher.booklight.scopes.Scopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The process: everything that should outlive the panel lives here, so the panel opens on a warm
 * index. The app list is read once at start and kept current by LauncherApps callbacks.
 */
class BooklightApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var historyStore: HistoryStore private set
    lateinit var prefs: Prefs private set
    /** The one source that uses the network; asked after the local rows are shown, and only if switched on. */
    lateinit var suggest: SuggestProvider private set
    lateinit var apps: AppsProvider private set
    lateinit var engine: SearchEngine private set
    lateinit var executor: Executor private set
    lateinit var scopes: Scopes private set
    lateinit var notes: Notes private set
    /** The device's own model, where the system has one for apps: it answers the user's prompts. */
    lateinit var onDevice: OnDevice private set
    /** What the pinned window shows, while there is one. */
    val pinned = kotlinx.coroutines.flow.MutableStateFlow<io.github.kuscher.booklight.pin.Pinned?>(null)
    /** Everything Booklight does, with examples: the list behind `?`, the Commands page, the tips. */
    lateinit var guide: Guide private set
    lateinit var tips: Tips private set
    /** What other apps offer. */
    lateinit var commands: AppCommands private set
    /** A flight number's row. With a key of the user's own it asks a flight service: the other network code, beside [suggest]. */
    lateinit var flights: FlightsProvider private set
    /** What Spotify has by a name, for `play`. With a key of the user's own it asks Spotify: network code like [flights]'. */
    lateinit var songs: io.github.kuscher.booklight.providers.Songs private set
    /** What was typed when the panel last closed without running anything (the chip's key, the text): Up brings it back. */
    var lastText: Pair<String?, String>? = null
    /** An example the Booklight window asks the panel to type when it next opens (a row of its Commands page). Booklight's own, in its own process: no other app can put text here. */
    @Volatile var example: String? = null
    /** App icons, made when the panel first needs them and kept after. */
    var icons: io.github.kuscher.booklight.ui.AppIcons? = null

    /**
     * "Your usual": the rows for the empty field as they stand now. [held]: what to keep as the holders of the first
     * two seats (null: keep what is kept). [picks] and [micros] are for `./bl debug zero`.
     */
    class Usual(val rows: List<Result>, val held: List<String>?, val picks: List<Zero.Pick>, val micros: Long)

    /**
     * Works out the usual rows, off the main thread: waits until the app list and other apps' commands have been read
     * once, picks from what was run from Booklight ([Zero]), makes each pick's row as typing would find it, and has the
     * app icons loaded, so no icon comes after its row. Every row is one line: a thing whose row has a second line (a
     * link's address, a recipe's steps) is not suggested in this version.
     */
    suspend fun usual(): Usual = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        apps.ready.await(); commands.ready.await()
        val t0 = System.nanoTime()
        val s = prefs.now
        val items = engine.used()
        val rows = engine.byIds(items.keys.filterTo(HashSet()) { id -> Zero.KINDS.any(id::startsWith) }).filter { it.subtitle == null }.associateBy { it.id }
        val seats = Zero.pick(items, System.currentTimeMillis(), live = { it in rows }, hidden = s.zeroHidden.toSet(), held = s.zeroHeld)
        val label = getString(R.string.action_unsuggest)
        val out = seats.picks.mapNotNull { rows[it.id] }.map { Zero.offer(it, label) }
        val cache = icons ?: io.github.kuscher.booklight.ui.AppIcons(this@BooklightApp).also { icons = it }
        val px = (48 * resources.displayMetrics.density).toInt()
        for (r in out) (r.icon as? io.github.kuscher.booklight.core.Icon.App)?.let { cache.load(it, px) }
        Usual(out, seats.held, seats.picks, (System.nanoTime() - t0) / 1000)
    }

    /** Every source of results. A new ability is one more line here. */
    lateinit var providers: List<Provider> private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        historyStore = HistoryStore(this, scope)
        prefs = Prefs(this, scope)
        suggest = SuggestProvider(this, prefs)
        apps = AppsProvider(this, scope)
        notes = Notes(this, prefs)
        Recipes.me = getSystemService(android.os.UserManager::class.java).getSerialNumberForUser(android.os.Process.myUserHandle())
        val web = WebProvider(this, prefs, apps)
        executor = Executor(this)
        val dials = Dials(this, executor)
        commands = AppCommands(this, prefs, scope, apps)
        val pages = SettingsProvider(this, prefs)
        val keys = KeysProvider(this, prefs)
        onDevice = OnDevice(scope)
        guide = Guide(this, prefs, commands)
        tips = Tips(this, prefs)
        flights = FlightsProvider(this, prefs, scope)
        songs = io.github.kuscher.booklight.providers.Songs(this, prefs, scope)
        scopes = Scopes(this, prefs, dials, notes, { web.search(it) }, pages, keys, onDevice, guide, others = { commands.scopes(it) }, pinned = { pinned.value }, flights = flights, installed = { apps.installed(it) })
        providers = listOf(apps, CalcProvider(this, prefs), Answers(this), pages, CommandsProvider(this), dials, User(this, prefs, apps), commands, keys, flights, web)
        engine = SearchEngine(
            providers, historyStore.history,
            scopes = { scopes.all() },
            enterLabel = { getString(if (it.symbol == "search" || it.key == "settings" || it.key == "keys") R.string.scope_search else R.string.scope_type) },
            fallback = { listOf(web.search(it)) },
        )
    }

    companion object {
        lateinit var instance: BooklightApp private set
        const val TAG = "Booklight"
    }
}
