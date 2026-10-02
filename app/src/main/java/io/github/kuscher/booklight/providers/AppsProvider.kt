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
import io.github.kuscher.booklight.core.AppRow
import io.github.kuscher.booklight.core.Behind
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
    override val id = ID

    /** [system]: came with the device, so it can't be uninstalled. */
    private data class App(val label: String, val pkg: String, val cls: String, val user: Long, val system: Boolean)

    /** Done once the app list has been read for the first time: until then [byIds] has no rows to give. (Declared before anything starts that read.) */
    val ready = kotlinx.coroutines.CompletableDeferred<Unit>()
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
    /** Called when the app list has been read again: what is made from it (other apps' commands) is read again too. */
    var onReload: (() -> Unit)? = null

    companion object {
        const val ID = "apps"
        /** A name found through a verb ranks just under the same name typed alone. */
        private const val VERB = 0.95
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
            ready.complete(Unit)
            onReload?.invoke()
        }
    }

    /**
     * The places an app's window can be asked to open in, as the id of the action, its place, its name and its typed
     * words. All of them are lines behind Window, in the order `AppRow` lists them; typed, one stands in Window's slot at once.
     */
    private class Spot(val id: String, val place: Place, val label: Int, val words: Int)

    private val spots = listOf(
        Spot("full", Place.FULL, R.string.place_full, R.string.verb_full),
        Spot("left", Place.LEFT, R.string.action_left_half, R.string.verb_left),
        Spot("right", Place.RIGHT, R.string.action_right_half, R.string.verb_right),
        Spot("p3l", Place.LEFT_THIRD, R.string.place_p3l, R.string.verb_p3l),
        Spot("p3m", Place.MIDDLE_THIRD, R.string.place_p3m, R.string.verb_p3m),
        Spot("p3r", Place.RIGHT_THIRD, R.string.place_p3r, R.string.verb_p3r),
        Spot("p23l", Place.LEFT_TWO_THIRDS, R.string.place_p23l, R.string.verb_p23l),
        Spot("p23r", Place.RIGHT_TWO_THIRDS, R.string.place_p23r, R.string.verb_p23r),
        Spot("ptl", Place.TOP_LEFT, R.string.place_ptl, R.string.verb_ptl),
        Spot("ptr", Place.TOP_RIGHT, R.string.place_ptr, R.string.verb_ptr),
        Spot("pbl", Place.BOTTOM_LEFT, R.string.place_pbl, R.string.verb_pbl),
        Spot("pbr", Place.BOTTOM_RIGHT, R.string.place_pbr, R.string.verb_pbr),
        Spot("pc", Place.CENTER, R.string.place_pc, R.string.verb_pc),
    )

    /** The words that arm an action when typed with an app's name ("chrome uninstall", "chrome top left"); English ones work in every language. */
    private val verbs: List<Verb> by lazy {
        fun words(id: Int) = context.getString(id).split(',').filter(String::isNotBlank)
        listOf(
            Verb("open", words(R.string.verb_open)),
            Verb("window", words(R.string.verb_window), atStart = false),      // "new window" at the start is the New scope
            Verb("info", words(R.string.verb_info)),
        ) + spots.map { Verb(it.id, words(it.words), atStart = false) } + AppPages.verbs(context) + Verb("uninstall", words(R.string.verb_uninstall), min = 3)
    }

    /** "… on display 2" at the end of the text: the words before the number, and how many screens there are. */
    private val displayWords: Regex by lazy {
        val on = context.getString(R.string.verb_display_on).split(',').joinToString("|") { Regex.escape(it.trim()) }
        val what = context.getString(R.string.verb_display).split(',').joinToString("|") { Regex.escape(it.trim()) }
        Regex("""\s+(?:(?:$on)\s+)?(?:$what)\s+(\d)\s*$""", RegexOption.IGNORE_CASE)
    }
    private val displays get() = context.getSystemService(android.hardware.display.DisplayManager::class.java).displays.size

    override suspend fun query(q: Query): List<Result> {
        // A screen that is there is asked for by its number; with one screen the words are ordinary text.
        val shown = displayWords.find(q.text)?.takeIf { it.groupValues[1].toInt() in 1..displays && displays > 1 }
        val display = shown?.groupValues?.get(1)?.toInt() ?: 0
        val text = if (shown != null) q.text.substring(0, shown.range.first) else q.text
        val apps = index
        val plain = DoubleArray(apps.size) { Matcher.score(text, apps[it].label) }
        // The whole text is a name first: if an app matches it from the start of a word, there is no verb ("play store", "open table").
        val named = plain.any { it >= Matcher.WORD_PREFIX }
        val readings = if (named) emptyList() else Verbs.readings(text, verbs)
        // "chrome t", one letter into "top left": not a verb yet, but the row stays where it was.
        val dangling = if (named || readings.isNotEmpty()) null else Verbs.dangling(text, verbs)
        val out = ArrayList<Result>()
        for (i in apps.indices) {
            var best = plain[i]
            var verb: String? = null
            for (r in readings) {
                val s = Matcher.score(r.rest, apps[i].label)
                if (s >= Matcher.WORD_PREFIX && s * VERB > best) { best = s * VERB; verb = r.action }
            }
            if (best <= 0 && dangling != null) Matcher.score(dangling, apps[i].label).let { if (it >= Matcher.WORD_PREFIX) best = it * VERB }
            if (best <= 0) continue
            val row = result(apps[i], best, display)
            // The verb arms its action on the app's own row: a place stands in Window's slot, a page or Uninstall in the arrow's.
            // An app without that action (it came with the device: no Uninstall) has no such reading.
            val armed = verb?.let { v -> row.actions.indexOfFirst { it.id == v } }
            if (armed == null) out += row else if (armed >= 0) out += row.copy(armed = armed) else if (plain[i] > 0) out += result(apps[i], plain[i], display)
        }
        return out
    }

    /** The package of the app whose name [text] matches best from its start, if any: the app that leads the list for it. */
    fun best(text: String): String? = index.map { it to Matcher.score(text, it.label) }.filter { it.second >= Matcher.PREFIX }
        .maxWithOrNull(compareBy<Pair<App, Double>> { it.second }.thenBy { -it.first.label.length })?.first?.pkg

    /** The app of this user with the package [pkg], as the launcher shows it: its name, and the activity its icon is of. Null for one Booklight does not see. */
    fun shown(pkg: String): Pair<String, String>? = index.firstOrNull { it.pkg == pkg && it.user == me }?.let { it.label to it.cls }

    override fun byIds(ids: Set<String>): List<Result> = index.map { result(it, 1.0, 0) }.filter { it.id in ids }

    /** An app of the user Booklight runs as, by what names it: its package, the name it shows, and the activity its icon comes from. */
    class Installed(val pkg: String, val label: String, val cls: String)

    /** The apps a command of the user's own can be made for, by name; one entry for each app. */
    fun installed(): List<Installed> = index.filter { it.user == me }.distinctBy { it.pkg }.map { Installed(it.pkg, it.label, it.cls) }

    /** The app with this package, if it is in the list. */
    fun installed(pkg: String): Installed? = index.firstOrNull { it.pkg == pkg && it.user == me }?.let { Installed(it.pkg, it.label, it.cls) }

    /** Every app's id (the debug hook `seed` picks five). */
    fun ids(): Set<String> = index.mapTo(HashSet()) { result(it, 1.0, 0).id }

    private fun result(a: App, score: Double, display: Int) = Result(
        id = "app:${a.pkg}/${a.cls}" + if (a.user != 0L) "#${a.user}" else "",
        provider = id, kind = Kind.APP, title = a.label,
        subtitle = if (display > 0) context.getString(R.string.place_on_display, display) else null,
        icon = Icon.App(a.pkg, a.cls, a.user), score = score,
        // One structure for every app, never rearranged by use (core `AppRow`, which orders these and says where each
        // stands): Open · Search · Play · Window · the arrow. An app has Search if it can be searched and Play if it plays
        // what is named, and shows neither if it does not. Window opens New window and the places; the arrow App info, the
        // app's pages in Settings and, last and never armed unless asked for by name, what removes the app.
        actions = AppRow.arrange(listOfNotNull(
            Action(AppRow.OPEN, context.getString(R.string.action_open), Effect.LaunchApp(a.pkg, a.cls, a.user, display = display)),
            Action(AppRow.WINDOW, context.getString(R.string.action_window), Effect.OpenList(Behind.WINDOW), keepOpen = true, symbol = "frame"),
            Action(AppRow.NEW_WINDOW, context.getString(R.string.action_new_window), Effect.LaunchApp(a.pkg, a.cls, a.user, newWindow = true, display = display)).takeIf { a.user == me },
            Action(AppRow.INFO, context.getString(R.string.action_app_info), Effect.AppInfo(a.pkg, a.cls, a.user)),
            Action(AppRow.UNINSTALL, context.getString(R.string.action_uninstall), Effect.Uninstall(a.pkg, a.user), symbol = "trash", danger = true).takeIf { !a.system },
        ) + spots.map { Action(it.id, context.getString(it.label), Effect.LaunchApp(a.pkg, a.cls, a.user, place = it.place, display = display)) } +
            // Its own pages in Settings (AppPages), for the user Booklight runs as: Settings shows that user's apps.
            (if (a.user == me) AppPages.actions(context, a.pkg) else emptyList()) +
            AppChips.actions(context, a.pkg, a.user)),     // Search and Play, each for an app that has it (AppChips.kt)
    )
}
