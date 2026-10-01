package io.github.kuscher.booklight

import android.app.Application
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.data.HistoryStore
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.providers.AppsProvider
import io.github.kuscher.booklight.providers.CalcProvider
import io.github.kuscher.booklight.providers.CommandsProvider
import io.github.kuscher.booklight.providers.SettingsProvider
import io.github.kuscher.booklight.providers.SuggestProvider
import io.github.kuscher.booklight.providers.WebProvider
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
        providers = listOf(apps, CalcProvider(this, prefs), SettingsProvider(this, prefs), CommandsProvider(this), WebProvider(this, prefs))
        engine = SearchEngine(providers, historyStore.history)
        executor = Executor(this)
    }

    companion object {
        lateinit var instance: BooklightApp private set
        const val TAG = "Booklight"
    }
}
