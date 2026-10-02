package io.github.kuscher.booklight.providers

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.UserManager
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.AppSearch
import io.github.kuscher.booklight.core.AppSearch.Source
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Site
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.data.Settings

/**
 * A search inside an app: "spotify daft punk", or Search behind the arrow of the app's own row,
 * lands on the results inside that app.
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
     * by the folded name of each of their icons (with that icon's own name), each app's chip, and the
     * apps that were looked at.
     */
    class Index internal constructor(internal val byPackage: Map<String, Found> = emptyMap(), internal val byName: Map<String, List<Pair<Found, String>>> = emptyMap(),
        internal val chips: List<In> = emptyList(), internal val installed: List<Listed> = emptyList()) {
        val found: Collection<Found> get() = byPackage.values
    }

    @Volatile private var index = Index()
    private val table: List<AppSearch.Line> by lazy { context.assets.open(TABLE).bufferedReader().useLines { AppSearch.table(it) } }
    private val me = context.getSystemService(UserManager::class.java).getSerialNumberForUser(android.os.Process.myUserHandle())

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
        return Index(all.associateBy { it.pkg }, names, all.map { In(it) }, apps)
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

    // ---- as rows

    private fun allowed(f: Found, s: Settings = prefs.now) = s.appCommands && f.pkg !in s.mutedApps

    /** The row that searches [f]'s app for [text]: named like the web's row, with the app's own icon. A file's keyword keeps the file's words. [app]: the name it was typed by. */
    private fun row(f: Found, text: String, score: Double, app: String = f.app) = Result(
        id = SearchEngine.IN_APP + f.pkg, provider = SearchEngine.COMMANDS, kind = Kind.COMMAND,
        title = if (f.name != null) "${f.name}: $text" else context.getString(R.string.app_search_title, app, text),
        icon = Icon.App(f.pkg, f.cls, me), score = score, label = app,
        // It is the text itself, like a web search: nothing to learn, and nothing for the rows under the empty field.
        learnable = false,
        actions = listOf(Action("search", context.getString(if (f.name != null) R.string.action_open else R.string.action_search), open(f, text), symbol = "open")),
    )

    private fun open(f: Found, text: String) = Effect.Open(f.pkg, commands.fill(f.pkg, f.template, text))

    /**
     * "spotify daft punk": for each app whose whole name starts the text, the search inside it for
     * the rest, the longest name first. Scored under every local match, so with "spotify d" or
     * "google m" an app or a command whose name is the whole text still leads; only the ways out to
     * the web come after it.
     */
    fun rows(text: String): List<Result> {
        val names = index.byName
        if (names.isEmpty()) return emptyList()
        val s = prefs.now
        val out = ArrayList<Result>(2)
        for (r in AppSearch.readings(text)) for ((f, app) in names[r.name].orEmpty()) if (allowed(f, s)) out += row(f, r.text, TYPED + r.name.length * LONGER, app)
        return out
    }

    /**
     * "Search" on an app's own row, behind its arrow: it makes the app the chip in the field, and what
     * is typed then is searched for there. Null for an app that cannot be searched, or that is another
     * profile's (a link cannot be sent there from here).
     */
    fun action(pkg: String, user: Long): Action? {
        if (user != me) return null
        val f = index.byPackage[pkg]?.takeIf { allowed(it) } ?: return null
        return Action("search", f.name ?: context.getString(R.string.action_search), Effect.EnterScope(SearchEngine.IN_APP + pkg), keepOpen = true, symbol = "search", more = true)
    }

    /** One chip for each app that can be searched: entered from the app's row. (Asked for on every keystroke: nothing is made here.) */
    fun scopes(): List<Scope> {
        val s = prefs.now
        val chips = index.chips
        return if (!s.appCommands) emptyList() else if (s.mutedApps.isEmpty()) chips else chips.filter { it.f.pkg !in s.mutedApps }
    }

    /**
     * The chip of an app's search: the app's name; what is typed is looked for there. It has no
     * keyword: an app's name stays a name ("google m" is Google Maps), and typing the name and the
     * text needs no chip.
     */
    inner class In internal constructor(internal val f: Found) : Scope {
        override val key = SearchEngine.IN_APP + f.pkg
        override val keywords = emptyList<String>()
        override val name = f.app
        override val symbol = "search"
        override val hint: String get() = f.hint ?: context.getString(R.string.scope_site_hint, f.app)
        override val listed = false
        override suspend fun rows(arg: String): List<Result> = arg.trim().let { if (it.isEmpty()) emptyList() else listOf(row(f, it, 1.0)) }
    }

    // ---- Booklight's own links

    /**
     * Where one of Booklight's own links (`yt`, `maps`, `store`, `drive`) goes when its app is
     * installed and can be searched: the app's icon and the search for [text] inside it. Null when
     * it is not, or the link is no longer as it came: then the link opens in the browser, as it says.
     */
    fun link(site: Site, text: String): Pair<Icon, Effect>? {
        val apps = index.byPackage
        if (apps.isEmpty()) return null
        val pkg = AppSearch.owner(site, table) ?: return null
        val f = apps[pkg]?.takeIf { it.source != Source.FILE && allowed(it) } ?: return null
        return Icon.App(f.pkg, f.cls, me) to open(f, text)
    }

    // ---- for the list of everything, and for the debug hook

    /**
     * An app's search as it would be typed ("spotify jazz"), for the list of everything and the tips:
     * the first app of the table that is here, else one that declares a search. [free]: whether the
     * name can be typed without its first word becoming a keyword's chip. Null when no app here can
     * be searched.
     */
    fun example(free: (String) -> Boolean): String? {
        val s = prefs.now
        val all = index.byPackage.values.filter { it.name == null && allowed(it, s) }
        if (all.isEmpty()) return null
        val inOrder = table.mapNotNull { line -> all.firstOrNull { it.pkg == line.pkg && it.source == Source.TABLE } }.distinct() +
            all.filter { it.source == Source.DECLARED }.sortedBy { it.app.lowercase() }
        val text = context.getString(R.string.guide_search_text)
        return inOrder.map { it.app.lowercase() }.firstOrNull { name -> free(name) && AppSearch.readings("$name $text").any { it.name == Matcher.fold(name) } }?.let { "$it $text" }
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

        /** The hook for an app's own row (`AppsProvider`): its Search action, if the app can be searched. */
        fun action(context: Context, pkg: String, user: Long): Action? = (context.applicationContext as BooklightApp).commands.search.action(pkg, user)
    }
}
