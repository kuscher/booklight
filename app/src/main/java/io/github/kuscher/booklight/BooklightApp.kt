package io.github.kuscher.booklight

import android.app.Application
import io.github.kuscher.booklight.ai.OnDevice
import io.github.kuscher.booklight.core.Act
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Zero
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Show
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
    /** The list of the user's calendars, once they allowed it in the window: names, colours, ids. Never an event. */
    lateinit var calendars: io.github.kuscher.booklight.device.CalendarList private set
    /** What the pinned window shows, while there is one. */
    val pinned = kotlinx.coroutines.flow.MutableStateFlow<io.github.kuscher.booklight.pin.Pinned?>(null)
    /** Everything Booklight does, with examples: the list behind `?`, the Commands page, the tips. */
    lateinit var guide: Guide private set
    lateinit var tips: Tips private set
    /** What other apps offer. */
    lateinit var commands: AppCommands private set
    /** A flight number's row. With a key of the user's own it asks a flight service: the other network code, beside [suggest]. */
    lateinit var flights: FlightsProvider private set
    /** An event typed as a sentence: its row under the keyword `event`, and for a line typed without it. */
    lateinit var events: io.github.kuscher.booklight.providers.Events private set
    /** What Spotify has by a name, for `play`. With a key of the user's own it asks Spotify: network code like [flights]'. */
    lateinit var songs: io.github.kuscher.booklight.providers.Songs private set
    /**
     * What was typed when the panel last closed without running anything: the chip's [key] and the [text]; for an
     * app's chip also the action that was armed ([act]), the keyword it was entered by ([word]) and that keyword's
     * own scope ([via]). Up brings it back.
     */
    class Last(val key: String?, val text: String, val act: io.github.kuscher.booklight.core.Act? = null, val word: String? = null, val via: String? = null)
    var lastText: Last? = null
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

    /**
     * First run's example for this device (core `FirstRun.example`): what the recipes of lessons 2 and 3 show to type. An
     * app that can be searched, by the shortest start of its name that puts its row first, so that lesson 3 builds on
     * lesson 2's app; where there is none, the Settings app and the Settings keyword. Worked out off the main thread, once
     * the app list and other apps' commands have been read, by asking the engine what those letters find: nothing is run,
     * sent or learned by it. Not kept: the same apps give the same example.
     */
    suspend fun firstExample(): FirstRun.Example = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        apps.ready.await(); commands.ready.await()
        // Row one for these letters is the app called [name]; with [search], its row also offers Search (lesson 3's Tab goes there).
        suspend fun leads(letters: String, name: String, search: Boolean): Boolean {
            val first = engine.search(Query(letters)).firstOrNull() ?: return false
            return first.kind == Kind.APP && first.title.equals(name, ignoreCase = true) && (!search || first.actions.any { (it.effect as? Effect.EnterScope)?.act == Act.SEARCH })
        }
        val app = guide.appSearch()?.let { name -> FirstRun.letters(name) { leads(it, name, search = true) } }
        val settings = if (app != null) null else apps.installed(SETTINGS)?.label?.let { name -> FirstRun.letters(name) { leads(it, name, search = false) } }
        FirstRun.example(app, getString(R.string.guide_link_text), settings, engine.scope("settings")?.keywords?.firstOrNull().orEmpty(), getString(R.string.first_search_page))
    }

    /**
     * What first run's show needs of this device (core `Show`): the [letter] Booklight types first and the [apps] it
     * finds, the example [flight]'s row, and the emoji grid ([scope], its [keyword], its row [grid]). Every row is the
     * panel's own, as typing would find it, but carries nothing to run. [comma]: the user's language writes 1,5.
     */
    class Cast(val letter: String, val apps: List<Result>, val flight: Result, val scope: Scope, val keyword: String, val grid: Result, private val copy: String, val comma: Boolean) {
        /** The answer's row for the sum [text], as the calculator's own row is, with nothing to run. */
        fun sum(text: String, answer: String) = Result(
            id = "calc", provider = "calc", kind = Kind.ANSWER, title = answer, subtitle = text.trim(), icon = io.github.kuscher.booklight.core.Icon.Symbol("calc"),
            score = 1.0, answer = answer, learnable = false, actions = listOf(io.github.kuscher.booklight.core.Action("copy", copy, Show.NOTHING)),
        )
    }

    /**
     * The cast for first run's show, worked out off the main thread once the app list and other apps' commands have
     * been read. The apps are the engine's own rows for the letter most app names here begin with, the first five
     * that are apps, with their icons loaded: nothing is asked that typing that letter would not ask, and nothing is
     * run, sent, learned, logged or kept by it. Every action of every row carries `Show.NOTHING` in place of what it
     * would do. Null where this device has nothing to show: no app whose name begins with a letter, or no emoji grid.
     */
    suspend fun cast(): Cast? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        apps.ready.await(); commands.ready.await()
        fun idle(r: Result) = r.copy(actions = r.actions.map { it.copy(effect = Show.NOTHING, done = null) }, nudge = null)
        val letter = Show.letter(apps.names()) ?: return@withContext null
        val found = engine.search(Query(letter), CAST_ROWS).filter { it.provider == AppsProvider.ID && it.kind == Kind.APP }.take(Show.APPS).map(::idle)
        if (found.isEmpty()) return@withContext null
        val scope = engine.scope("emoji") ?: return@withContext null
        val keyword = scope.keywords.firstOrNull() ?: return@withContext null
        val grid = scope.rows("").firstOrNull() ?: return@withContext null
        val flight = flights.example(Show.FLIGHT, Show.LOOKED, getString(R.string.first_show_example), getString(R.string.first_show_flight))
        val cache = icons ?: io.github.kuscher.booklight.ui.AppIcons(this@BooklightApp).also { icons = it }
        val px = (48 * resources.displayMetrics.density).toInt()
        for (r in found) (r.icon as? io.github.kuscher.booklight.core.Icon.App)?.let { cache.load(it, px) }
        val comma = java.text.DecimalFormatSymbols.getInstance(resources.configuration.locales[0]).decimalSeparator == ','
        Cast(letter, found, flight, scope, keyword, idle(grid), getString(R.string.action_copy), comma)
    }

    /**
     * Whether first run stands in the panel (core `FirstRun.fits`): on a screen too low for it "First steps" is not offered,
     * neither as a command nor in the list of everything. The screen that counts is the one the panel stands on, which
     * need not be the application's own (a second display): while a panel is open it is that panel's ([panelDp]), so
     * the command is offered exactly where the panel it is typed in can hold a stage. With no panel open (the Booklight
     * window's Commands page, a debug hook) it is the application's display, asked of the system once, when first needed.
     */
    val firstFits: Boolean get() = FirstRun.fits(panelDp ?: ownDp)
    /** How high the screen is that the open panel stands on, in dp; null: no panel is open. Said by the panel as it is made, from what it has asked for itself already. */
    @Volatile var panelDp: Float? = null
    private val ownDp: Float by lazy { getSystemService(android.view.WindowManager::class.java).maximumWindowMetrics.bounds.height() / resources.displayMetrics.density }

    /**
     * What the system allows of the calendars, looked at as a panel is made and when the Booklight window has the keys
     * again: the list is read again (or emptied, where it may no longer be read), and "Save events without opening
     * Calendar" goes off where its permission is gone. So the switch is on only by the user's own press in the window,
     * also after the permission was taken back and given again in the system's screens, and on a device the settings
     * came to with a backup. (Not while a list is pretended, in a debug build: there the switch is set by its hook, to
     * look at the row, and nothing can be written.)
     */
    fun lookAtCalendars() {
        calendars.refresh()
        if (prefs.now.saveEvents && !calendars.pretends && !(calendars.allowed && calendars.writes)) prefs.update { it.copy(saveEvents = false) }
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
        calendars = io.github.kuscher.booklight.device.CalendarList(this, scope)
        guide = Guide(this, prefs, commands)
        tips = Tips(this, prefs)
        flights = FlightsProvider(this, prefs, scope)
        songs = io.github.kuscher.booklight.providers.Songs(this, prefs, scope)
        events = io.github.kuscher.booklight.providers.Events(this, prefs, calendars, onDevice)
        scopes = Scopes(this, prefs, dials, notes, { web.search(it) }, pages, keys, onDevice, guide, others = { commands.scopes(it) }, pinned = { pinned.value }, flights = flights, installed = { apps.installed(it) }, events = events)
        providers = listOf(apps, CalcProvider(this, prefs), Answers(this), pages, CommandsProvider(this), dials, User(this, prefs, apps), commands, keys, flights, events, web)
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
        /** The Settings app, which every Googlebook has: first run's example where no app can be searched. (It is named in the manifest's `<queries>` already, for its pages.) */
        private const val SETTINGS = "com.android.settings"
        /** How many rows the engine is asked for to find the show's five apps among them: a letter also finds settings pages and keywords. */
        private const val CAST_ROWS = 40
    }
}
