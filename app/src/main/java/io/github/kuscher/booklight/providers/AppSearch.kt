package io.github.kuscher.booklight.providers

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import io.github.kuscher.booklight.core.AppSearch
import io.github.kuscher.booklight.core.AppSearch.Source
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Site
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.data.Settings

/**
 * A search inside an app: Search on the app's own row, or "spotify daft punk" typed in one go, lands
 * on the results inside that app. This class knows where each app's search goes; the chip and the
 * row are `AppChips`'.
 *
 * Where an app's search goes is read when the app list is read ([read], called by [AppCommands]), so
 * typing asks the package manager nothing. For one app it is the first of: a keyword of the app's own
 * Booklight file that takes text, a line of the bundled table (`assets/appsearch.tsv`), a search
 * activity the app declares for everyone. Each is kept only if [AppCommands.safe] says the installed
 * app takes it: an activity of that very app, open to other apps, asking for no permission; and
 * `Executor` checks the same again when it is run. Nothing of another maker is bundled but the form
 * of a link: names and icons are the device's own.
 *
 * It is part of what other apps offer: the switch for that turns it off, for all apps or for one.
 */
class AppSearches(private val context: Context, private val prefs: Prefs, private val commands: AppCommands) {
    /** A launcher activity of the user Booklight runs as, as the app list has it. */
    class Listed(val pkg: String, val cls: String, val label: String)

    /**
     * An app that can be searched: [template] is what [AppCommands.fill] makes the address from.
     * [name] and [hint] are the words of the app's own file for its keyword; null where the search
     * is the table's or one the app declares, and Booklight says "Search".
     */
    class Found(val pkg: String, val app: String, val cls: String, val source: Source, val template: String, val name: String?, val hint: String?)

    /** One way an app might be searched, not yet asked of the device: a keyword of its file, a link of the table, or an activity it declares for [action]. */
    private class Way(val key: AppCommands.Keyword? = null, val link: String? = null, val action: String? = null, val cls: String? = null)

    /**
     * What one read of the app list found, in one piece: the apps that can be searched by package,
     * by the folded name of each of their icons (with that icon's own name), and the apps that were
     * looked at.
     */
    class Index internal constructor(internal val byPackage: Map<String, Found> = emptyMap(), internal val byName: Map<String, List<Pair<Found, String>>> = emptyMap(),
        internal val installed: List<Listed> = emptyList()) {
        val found: Collection<Found> get() = byPackage.values
    }

    /** One way to read a typed sentence: [found]'s app, the name it was typed by ([app]), what to look for there ([text]), and how the row scores. */
    class Named(val found: Found, val app: String, val text: String, val score: Double)

    @Volatile private var index = Index()
    private val table: List<AppSearch.Line> by lazy { context.assets.open(TABLE).bufferedReader().useLines { AppSearch.table(it) } }

    // ---- read with the app list

    /**
     * Reads where each of [apps] is searched; [keys] are the keywords of their own files. This asks
     * the package manager: it is called when the app list has been read, off the main thread. What
     * it found is not in use until it is handed to [use]: only the newest read of the app list may be.
     */
    fun read(apps: List<Listed>, keys: List<AppCommands.Keyword>): Index {
        val declared = declared()
        val found = LinkedHashMap<String, Found?>()
        val names = HashMap<String, ArrayList<Pair<Found, String>>>()
        for (a in apps) {
            if (a.pkg !in found) found[a.pkg] = find(a, keys, declared[a.pkg].orEmpty())
            val f = found[a.pkg] ?: continue
            // An app with two icons is found by the name of either, and its row then says that name.
            names.getOrPut(Matcher.fold(a.label)) { ArrayList() }.let { if (it.none { n -> n.first === f }) it.add(f to a.label) }
        }
        val all = found.values.filterNotNull()
        return Index(all.associateBy { it.pkg }, names, apps)
    }

    fun use(read: Index) { index = read }

    /** How [a]'s app is searched: the first way, in the order of the sources, that the installed app takes. */
    private fun find(a: Listed, keys: List<AppCommands.Keyword>, declared: List<Way>): Found? {
        val ways = keys.filter { it.owner == a.pkg && it.template != null }.take(1).map { Source.FILE to Way(key = it) } +
            table.filter { it.pkg == a.pkg }.map { Source.TABLE to Way(link = it.address) } +
            declared.map { Source.DECLARED to it }
        val (source, kept) = AppSearch.choose(ways) { w -> template(a.pkg, w)?.let { w to it } } ?: return null
        return Found(a.pkg, a.label, a.cls, source, kept.second, kept.first.key?.name, kept.first.key?.let { it.hint.ifEmpty { it.name } })
    }

    /** [w] as a template for [AppCommands.fill], if the installed app takes it. What is checked with one text stands for every text: the same activity answers. */
    private fun template(pkg: String, w: Way): String? = when {
        // A file's keyword was checked when the file was read.
        w.key != null -> w.key.template
        w.link != null -> commands.safe(pkg, commands.intent(pkg, null, null, AppSearch.address(w.link, PROBE), emptyMap()))
            ?.let { commands.template(null, null, w.link, emptyMap()) }
        else -> commands.safe(pkg, commands.intent(pkg, w.action, w.cls, null, mapOf(SearchManager.QUERY to PROBE)))
            ?.let { commands.template(w.action, w.cls, null, mapOf(SearchManager.QUERY to AppSearch.ARGUMENT)) }
    }

    /**
     * The search activities apps declare for everyone, by package: the platform's search first, then
     * Google's search action; both take the text as the extra `query`. One question to the package
     * manager for each of the two, for all apps at once.
     */
    private fun declared(): Map<String, List<Way>> = ACTIONS.flatMap { action ->
        runCatching { context.packageManager.queryIntentActivities(Intent(action), 0) }.getOrDefault(emptyList())
            .map { it.activityInfo.packageName to Way(action = action, cls = it.activityInfo.name) }
    }.groupBy({ it.first }, { it.second }).mapValues { it.value.take(MAX_DECLARED) }

    // ---- for the rows

    /** The switch for what other apps offer turns an app's search off too, for all apps or for one. */
    internal fun allowed(f: Found, s: Settings = prefs.now) = s.appCommands && f.pkg !in s.mutedApps

    /** What Enter runs to search [f]'s app for [text]. `Executor` checks it again when it is run. */
    internal fun open(f: Found, text: String) = Effect.Open(f.pkg, commands.fill(f.pkg, f.template, text))

    /**
     * "spotify daft punk": each app whose whole name starts the text, with the rest as what to look
     * for there, the longest name first. Scored under every local match, so with "spotify d" or
     * "google m" an app or a command whose name is the whole text still leads.
     */
    fun named(text: String): List<Named> {
        val names = index.byName
        if (names.isEmpty()) return emptyList()
        val s = prefs.now
        val out = ArrayList<Named>(2)
        for (r in AppSearch.readings(text)) for ((f, app) in names[r.name].orEmpty()) if (allowed(f, s)) out += Named(f, app, r.text, TYPED + r.name.length * LONGER)
        return out
    }

    /**
     * The app that searches where [site] does, if [site] is one of Booklight's own links as it came
     * (`yt`, `maps`, `store`, `drive`) and that app is installed and can be searched. Null when it is
     * not, or the link is no longer as it came: then the link opens in the browser, as it says.
     */
    fun owner(site: Site): Found? {
        val apps = index.byPackage
        if (apps.isEmpty()) return null
        val pkg = AppSearch.owner(site, table) ?: return null
        return apps[pkg]?.takeIf { it.source != Source.FILE && allowed(it) }
    }

    // ---- for the list of everything, and for the debug hook

    /**
     * An app that can be searched, as its name would be typed ("spotify"), for the list of everything
     * and the tips: the first app of the table that is here, else one that declares a search. [free]:
     * whether the name can be typed without its first word becoming a keyword's chip. Null when no app
     * here can be searched.
     */
    fun example(free: (String) -> Boolean): String? {
        val s = prefs.now
        val all = index.byPackage.values.filter { it.name == null && allowed(it, s) }
        if (all.isEmpty()) return null
        val inOrder = table.mapNotNull { line -> all.firstOrNull { it.pkg == line.pkg && it.source == Source.TABLE } }.distinct() +
            all.filter { it.source == Source.DECLARED }.sortedBy { it.app.lowercase() }
        return inOrder.map { it.app.lowercase() }.firstOrNull(free)
    }

    /**
     * For `./bl debug appsearch`: every keyword of an app's file that is its search, every line of
     * the table and every search an app declares, each with the app, the source and whether the
     * installed app takes it. A star marks the one in use. This asks the package manager again:
     * never on the main thread.
     */
    fun report(): List<String> {
        val apps = HashMap<String, Listed>().also { m -> for (a in index.installed) m.putIfAbsent(a.pkg, a) }
        val used = index.byPackage
        fun line(pkg: String, source: Source, what: String, w: Way): String {
            val a = apps[pkg]
            val t = if (a == null) null else template(pkg, w)
            val star = if (t != null && used[pkg]?.let { it.source == source && it.template == t } == true) "*" else ""
            return "$star${a?.label ?: pkg} · ${source.name.lowercase()} · $what · " + when { a == null -> "not installed"; t != null -> "resolves"; else -> "does not resolve" }
        }
        return used.values.filter { it.source == Source.FILE }.map { "*${it.app} · file · ${it.name} · resolves" } +
            table.map { line(it.pkg, Source.TABLE, it.address, Way(link = it.address)) } +
            declared().filterKeys { it in apps }.flatMap { (pkg, ways) -> ways.map { line(pkg, Source.DECLARED, "${it.action?.substringAfterLast('.')} ${it.cls?.substringAfterLast('.')}", it) } }
    }

    companion object {
        private const val TABLE = "appsearch.tsv"
        /** Stands in for the typed text when a way is asked of the device. */
        private const val PROBE = "x"
        private val ACTIONS = listOf(Intent.ACTION_SEARCH, "com.google.android.gms.actions.SEARCH_ACTION")
        /** An app's first search activities are looked at; the first that is open to others is the one. */
        private const val MAX_DECLARED = 4
        /** The typed form's score: under every local match (the weakest of those is several times this), over the ways out to the web. */
        private const val TYPED = 0.02
        /** A longer name counts a little more: "youtube music daft punk" is YouTube Music before it is YouTube. */
        private const val LONGER = 0.0001
    }
}
