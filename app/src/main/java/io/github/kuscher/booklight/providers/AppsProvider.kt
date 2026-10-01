package io.github.kuscher.booklight.providers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Place
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Verb
import io.github.kuscher.booklight.core.Verbs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Installed apps, as the launcher sees them: every launchable activity of every profile
 * (LauncherApps; visibility from the manifest's MAIN/LAUNCHER <queries>, no permission).
 * The list is held in memory and refreshed when packages change.
 */
class AppsProvider(private val context: Context, private val scope: CoroutineScope) : Provider {
    override val id = "apps"

    /** [system]: came with the device, so it can't be uninstalled. */
    private data class App(val label: String, val pkg: String, val cls: String, val user: Long, val system: Boolean)

    @Volatile private var index: List<App> = emptyList()
    @Volatile var loadedMs: Long = -1; private set
    val count: Int get() = index.size

    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val users = context.getSystemService(UserManager::class.java)
    /** The serial number of the user Booklight runs as: apps of other profiles can't be given a second window from here. */
    private val me = users.getSerialNumberForUser(android.os.Process.myUserHandle())

    init {
        reload()
        launcher.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
            override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
            override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        }, Handler(Looper.getMainLooper()))
    }

    private var loading: Job? = null

    private companion object {
        /** A name found through a verb ranks just under the same name typed alone. */
        const val VERB = 0.95
    }

    fun reload() {
        loading?.cancel()   // only the newest read of the app list may set the index
        loading = scope.launch(Dispatchers.IO) {
            val t0 = System.nanoTime()
            val own = context.packageName
            val out = ArrayList<App>()
            for (profile in launcher.profiles) {
                val serial = users.getSerialNumberForUser(profile)
                for (a in launcher.getActivityList(null, profile)) {
                    if (a.componentName.packageName == own) continue   // Booklight doesn't list itself
                    val flags = a.applicationInfo.flags
                    val system = flags and ApplicationInfo.FLAG_SYSTEM != 0 && flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
                    out.add(App(a.label.toString(), a.componentName.packageName, a.componentName.className, serial, system))
                }
            }
            ensureActive()
            index = out.sortedBy { it.label.lowercase() }
            loadedMs = (System.nanoTime() - t0) / 1_000_000
        }
    }

    /** The words that arm an action when typed with an app's name ("chrome uninstall"); English ones work in every language. */
    private val verbs: List<Verb> by lazy {
        fun words(id: Int) = context.getString(id).split(',').filter(String::isNotBlank)
        listOf(
            Verb("open", words(R.string.verb_open)),
            Verb("window", words(R.string.verb_window), atStart = false),      // "new window" at the start is the New scope
            Verb("info", words(R.string.verb_info)),
            Verb("left", words(R.string.verb_left), atStart = false),
            Verb("right", words(R.string.verb_right), atStart = false),
            Verb("uninstall", words(R.string.verb_uninstall), min = 3),
        )
    }

    override suspend fun query(q: Query): List<Result> {
        val apps = index
        val plain = DoubleArray(apps.size) { Matcher.score(q.text, apps[it].label) }
        // The whole text is a name first: if an app matches it from the start of a word, there is no verb ("play store", "open table").
        val readings = if (plain.any { it >= Matcher.WORD_PREFIX }) emptyList() else Verbs.readings(q.text, verbs)
        val out = ArrayList<Result>()
        for (i in apps.indices) {
            var best = plain[i]
            var verb: String? = null
            for (r in readings) {
                val s = Matcher.score(r.rest, apps[i].label)
                if (s >= Matcher.WORD_PREFIX && s * VERB > best) { best = s * VERB; verb = r.action }
            }
            if (best <= 0) continue
            val row = result(apps[i], best)
            // The verb arms its action on the app's own row. An app without that action (it came with the device: no Uninstall) has no such reading.
            val armed = verb?.let { v -> row.actions.indexOfFirst { it.id == v } }
            if (armed == null) out += row else if (armed >= 0) out += row.copy(armed = armed) else if (plain[i] > 0) out += result(apps[i], plain[i])
        }
        return out
    }

    /** Every app with score 0: the engine keeps only the ones you have used. */
    override suspend fun zeroState(): List<Result> = index.map { result(it, 0.0) }

    private fun result(a: App, score: Double) = Result(
        id = "app:${a.pkg}/${a.cls}" + if (a.user != 0L) "#${a.user}" else "",
        provider = id, kind = Kind.APP, title = a.label,
        icon = Icon.App(a.pkg, a.cls, a.user), score = score,
        // A fixed order, never rearranged by use; what removes the app is last and never armed unless asked for by name.
        actions = listOfNotNull(
            Action("open", context.getString(R.string.action_open), Effect.LaunchApp(a.pkg, a.cls, a.user)),
            Action("window", context.getString(R.string.action_new_window), Effect.LaunchApp(a.pkg, a.cls, a.user, newWindow = true)).takeIf { a.user == me },
            Action("info", context.getString(R.string.action_app_info), Effect.AppInfo(a.pkg, a.cls, a.user)),
            Action("left", context.getString(R.string.action_left_half), Effect.LaunchApp(a.pkg, a.cls, a.user, place = Place.LEFT)),
            Action("right", context.getString(R.string.action_right_half), Effect.LaunchApp(a.pkg, a.cls, a.user, place = Place.RIGHT)),
            Action("uninstall", context.getString(R.string.action_uninstall), Effect.Uninstall(a.pkg, a.user), symbol = "trash", danger = true).takeIf { !a.system },
        ),
    )
}
