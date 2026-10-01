package io.github.kuscher.booklight

import android.app.Application
import io.github.kuscher.booklight.ai.OnDevice
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
    /** What other apps offer. */
    lateinit var commands: AppCommands private set
    /** What was typed when the panel last closed without running anything (the chip's key, the text): Up brings it back. */
    var lastText: Pair<String?, String>? = null
    /** App icons, made when the panel first needs them and kept after. */
    var icons: io.github.kuscher.booklight.ui.AppIcons? = null

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
        val web = WebProvider(this, prefs)
        executor = Executor(this)
        val dials = Dials(this, executor)
        commands = AppCommands(this, prefs, scope, apps)
        val pages = SettingsProvider(this, prefs)
        val keys = KeysProvider(this, prefs)
        onDevice = OnDevice(scope)
        guide = Guide(this, prefs, commands)
        scopes = Scopes(this, prefs, dials, notes, { web.search(it) }, pages, keys, onDevice, guide, others = { commands.scopes(it) }, pinned = { pinned.value })
        providers = listOf(apps, CalcProvider(this, prefs), Answers(this), pages, CommandsProvider(this), dials, User(this, prefs), commands, keys, web)
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
