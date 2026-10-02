package io.github.kuscher.booklight.providers

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.net.Uri
import android.os.UserManager
import android.util.Log
import android.util.Xml
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.BuildConfig
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.AppSearch
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser

/**
 * What other apps offer, as rows: "New tab", "New event", "New text note".
 *
 * Two sources, both read when the app list is read, so nothing of another app runs before Enter
 * and no app ever sees what is typed:
 *  - the shortcuts an app declares in its manifest (the items on its icon's menu);
 *  - one file an app can add for Booklight (`io.github.kuscher.booklight.commands`, see
 *    docs/EXTENSIONS.md): commands, and keywords that take a choice or text.
 *
 * A command is kept only if it leads to an activity of the app that declared it, open to other
 * apps and asking for no permission; the same is checked again when it is run (`Executor`).
 *
 * Where a search inside an app goes is read here too and held by [search]: an app's own keyword
 * first, then a bundled table, then what the app declares for everyone (`AppSearch.kt`). And with it
 * which apps play music: an app that can be searched or plays has a chip of its own ([chips]).
 */
class AppCommands(private val context: Context, private val prefs: Prefs, private val scope: CoroutineScope, private val apps: AppsProvider) : Provider {
    override val id = SearchEngine.COMMANDS

    /** One thing an app offers. [intent] is an `intent:` address; in a file's text-taking scope it still holds `{argument}`. */
    class Command(val owner: String, val app: String, val cls: String, val id: String, val title: String, val words: List<String>, val intent: String)

    /** A keyword an app's file declares: fixed [choices], or [template] that takes the typed text. */
    class Keyword(val owner: String, val app: String, val cls: String, val id: String, val keywords: List<String>, val name: String, val hint: String, val choices: List<Command>, val template: String?)

    /** An app that offers something, for the list in the Booklight window. */
    class Offer(val owner: String, val app: String, val cls: String, val titles: List<String>)

    @Volatile private var commands: List<Command> = emptyList()
    @Volatile private var keywords: List<Keyword> = emptyList()
    @Volatile var offers: List<Offer> = emptyList(); private set
    /** Bumped when the index was read again, so lists made from it can be made again. */
    @Volatile var version = 0; private set

    private val pm = context.packageManager
    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val me = context.getSystemService(UserManager::class.java).getSerialNumberForUser(android.os.Process.myUserHandle())
    private var loading: Job? = null
    /** Done once other apps' commands have been read for the first time (a job of its own, after the app list). */
    val ready = kotlinx.coroutines.CompletableDeferred<Unit>()
    /** Where a search inside an app goes, for every app that has one. Read with the commands: an app's own file comes first. */
    val search = AppSearches(context, prefs, this)
    /** An app as the chip in the field, for Search and Play on its row; made anew with every read of the app list. */
    val chips = AppChips(context, prefs, search, apps)

    init { apps.onReload = ::reload; reload() }

    fun reload() {
        loading?.cancel()
        loading = scope.launch(Dispatchers.IO) {
            val own = context.packageName
            // Booklight's own names for what an app's row already does: a shortcut called the same adds nothing.
            val taken = listOf(R.string.action_open, R.string.action_new_window, R.string.action_app_info, R.string.action_uninstall).map { Matcher.fold(context.getString(it)) }.toSet()
            val cmds = ArrayList<Command>()
            val keys = ArrayList<Keyword>()
            val seen = HashSet<String>()
            val listed = ArrayList<AppSearches.Listed>()
            for (a in launcher.getActivityList(null, android.os.Process.myUserHandle())) {
                ensureActive()
                val pkg = a.componentName.packageName
                if (pkg == SETTINGS || (pkg == own && !BuildConfig.DEBUG)) continue
                val label = a.label.toString()
                val cls = a.componentName.className
                listed += AppSearches.Listed(pkg, cls, label)
                runCatching { shortcuts(pkg, cls, label) }.onFailure { Log.w(BooklightApp.TAG, "shortcuts of $pkg not read: ${it.javaClass.simpleName}") }
                    .getOrNull()?.filterTo(cmds) { Matcher.fold(it.title) !in taken }
                if (seen.add(pkg)) runCatching { file(pkg, cls, label, cmds, keys) }.onFailure { Log.w(BooklightApp.TAG, "commands of $pkg not read: ${it.javaClass.simpleName}") }
            }
            ensureActive()
            // After the files: a keyword of the app's own that takes text is its search, before the table and before what it declares.
            val searches = runCatching { search.read(listed, keys) }.onFailure { Log.w(BooklightApp.TAG, "searches not read: ${it.javaClass.simpleName}") }.getOrNull()
            ensureActive()
            // An app's own shortcut that is only called Search adds nothing where Search stands on the app's row: it is the
            // same act with another look, and it takes no words.
            val searching = setOf(Matcher.fold(context.getString(R.string.action_search)), "search")
            commands = cmds.distinctBy { it.owner + "/" + it.id }.filterNot { Matcher.fold(it.title) in searching && searches?.byPackage?.containsKey(it.owner) == true }
            keywords = keys
            searches?.let(search::use)
            // Which apps play music is asked now too, so that typing asks the package manager nothing.
            runCatching { chips.read(searches?.found.orEmpty()) }.onFailure { Log.w(BooklightApp.TAG, "music apps not read: ${it.javaClass.simpleName}") }
            // An app that is only searched is in the window's list too, so it can be turned off there like the others.
            val searched = context.getString(R.string.action_search)
            offers = (commands.map { Triple(it.owner, it.app to it.cls, it.title) } + keys.map { Triple(it.owner, it.app to it.cls, it.name) } +
                searches?.found.orEmpty().filter { it.name == null }.map { Triple(it.pkg, it.app to it.cls, searched) })
                .groupBy { it.first }.map { (owner, all) -> Offer(owner, all.first().second.first, all.first().second.second, all.map { it.third }.distinct()) }.sortedBy { it.app.lowercase() }
            version++
            ready.complete(Unit)
        }
    }

    // ---- what apps declare anyway

    /** The shortcuts an activity declares in its manifest that another app may start. */
    private fun shortcuts(pkg: String, cls: String, app: String): List<Command> {
        val info = pm.getActivityInfo(android.content.ComponentName(pkg, cls), PackageManager.GET_META_DATA)
        val xml = info.loadXmlMetaData(pm, "android.app.shortcuts") ?: return emptyList()
        val res = pm.getResourcesForApplication(info.applicationInfo)
        val out = ArrayList<Command>()
        xml.use { p ->
            var id = ""; var title = ""; var enabled = true; var last: Intent? = null; var events = 0
            while (p.next() != XmlPullParser.END_DOCUMENT && ++events < MAX_EVENTS && out.size < MAX_COMMANDS) {
                if (p.eventType == XmlPullParser.START_TAG && p.name == "shortcut") {
                    // Read by the attributes' own ids, as the system does: some apps' files carry no namespace to read them by name.
                    val t = res.obtainAttributes(Xml.asAttributeSet(p), SHORTCUT)
                    id = t.getString(SHORTCUT.indexOf(android.R.attr.shortcutId)).orEmpty()
                    title = (t.getString(SHORTCUT.indexOf(android.R.attr.shortcutShortLabel)) ?: t.getString(SHORTCUT.indexOf(android.R.attr.shortcutLongLabel))).orEmpty().trim()
                    enabled = t.getBoolean(SHORTCUT.indexOf(android.R.attr.enabled), true)
                    t.recycle()
                    last = null
                } else if (p.eventType == XmlPullParser.START_TAG && p.name == "intent") {
                    // A shortcut may list several (a back stack); the last is the one that is shown.
                    last = runCatching { Intent.parseIntent(res, p, Xml.asAttributeSet(p)) }.getOrNull()
                } else if (p.eventType == XmlPullParser.END_TAG && p.name == "shortcut") {
                    val uri = last?.let { safe(pkg, it) }
                    if (enabled && uri != null && id.isNotEmpty() && title.isNotEmpty()) out.add(Command(pkg, app, cls, id, title.take(MAX_TITLE), emptyList(), uri))
                }
            }
        }
        return out
    }

    // ---- the file for Booklight

    private fun file(pkg: String, cls: String, app: String, cmds: MutableList<Command>, keys: MutableList<Keyword>) {
        val meta = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA).metaData ?: return
        val xmlId = meta.getInt(META, 0).takeIf { it != 0 } ?: return
        val res = pm.getResourcesForApplication(pkg)
        res.getXml(xmlId).use { p ->
            var events = 0
            var commandsHere = 0
            // The keyword being read, if inside one; and the command being read.
            var key: Array<String>? = null
            var keyWords: List<String> = emptyList()
            var choices = ArrayList<Command>()
            var template: String? = null
            var cmd: Array<String>? = null
            var cmdWords: List<String> = emptyList()
            var cmdIntent: String? = null
            while (p.next() != XmlPullParser.END_DOCUMENT && ++events < MAX_EVENTS) {
                when (p.eventType) {
                    XmlPullParser.START_TAG -> when (p.name) {
                        "scope" -> if (key == null && keys.count { it.owner == pkg } < MAX_KEYWORDS) {
                            key = arrayOf(text(p, res, null, "id").orEmpty().take(MAX_ID), text(p, res, null, "name").orEmpty().take(MAX_TITLE), text(p, res, null, "hint").orEmpty().take(MAX_TITLE))
                            keyWords = words(text(p, res, null, "keywords"), single = true)
                            choices = ArrayList(); template = null
                        }
                        "command" -> if (cmd == null) {
                            cmd = arrayOf(text(p, res, null, "id").orEmpty().take(MAX_ID), text(p, res, null, "title").orEmpty().trim().take(MAX_TITLE))
                            cmdWords = words(text(p, res, null, "keywords")); cmdIntent = null
                        }
                        "open" -> {
                            val uri = open(pkg, p, res, argument = cmd == null && key != null)
                            if (cmd != null) cmdIntent = uri else if (key != null) template = uri
                        }
                    }
                    XmlPullParser.END_TAG -> when (p.name) {
                        "command" -> {
                            val c = cmd; val uri = cmdIntent
                            if (c != null && uri != null && c[0].isNotEmpty() && c[1].isNotEmpty()) {
                                val made = Command(pkg, app, cls, (key?.let { it[0] + "/" } ?: "") + c[0], c[1], cmdWords, uri)
                                if (key != null) { if (choices.size < MAX_CHOICES) choices.add(made) } else if (commandsHere++ < MAX_COMMANDS) cmds.add(made)
                            }
                            cmd = null
                        }
                        "scope" -> {
                            val k = key
                            if (k != null && k[0].isNotEmpty() && k[1].isNotEmpty() && keyWords.isNotEmpty() && (choices.isNotEmpty() || template != null))
                                keys.add(Keyword(pkg, app, cls, k[0], keyWords, k[1], k[2], choices, template))
                            key = null
                        }
                    }
                }
            }
        }
    }

    /** An `<open>`: the intent it describes, as an address. With [argument], `{argument}` stays in it for the typed text. */
    private fun open(pkg: String, p: XmlResourceParser, res: Resources, argument: Boolean): String? {
        val action = text(p, res, null, "action")?.take(MAX_VALUE)
        val cls = text(p, res, null, "class")?.take(MAX_VALUE)?.let { if (it.startsWith(".")) pkg + it else it }
        val data = text(p, res, null, "data")?.take(MAX_DATA)
        val extras = LinkedHashMap<String, String>()
        val depth = p.depth
        // Its extras, up to the tag's end.
        var events = 0
        while (!(p.next() == XmlPullParser.END_TAG && p.depth == depth) && p.eventType != XmlPullParser.END_DOCUMENT && ++events < 64) {
            if (p.eventType == XmlPullParser.START_TAG && p.name == "extra" && extras.size < MAX_EXTRAS) {
                val n = text(p, res, null, "name"); val v = text(p, res, null, "value")
                if (n != null && v != null) extras[n.take(128)] = v.take(MAX_VALUE)
            }
        }
        // What is checked now stands for every text that may be typed later: the same activity answers.
        val probe = intent(pkg, action, cls, data?.replace(ARGUMENT, "x"), extras.mapValues { it.value.replace(ARGUMENT, "x") })
        if (safe(pkg, probe) == null) return null
        if (!argument && (data?.contains(ARGUMENT) == true || extras.values.any { ARGUMENT in it })) return null
        return template(action, cls, data, extras)
    }

    /** An intent's parts as they are kept until the text is typed: `{argument}` may stand in [data] and in an extra. */
    internal fun template(action: String?, cls: String?, data: String?, extras: Map<String, String>): String =
        TEMPLATE + listOf(action.orEmpty(), cls.orEmpty(), data.orEmpty()).plus(extras.flatMap { listOf(it.key, it.value) }).joinToString(SEP)

    internal fun intent(pkg: String, action: String?, cls: String?, data: String?, extras: Map<String, String>): Intent {
        val i = Intent(action ?: if (data != null) Intent.ACTION_VIEW else Intent.ACTION_MAIN)
        if (cls != null) i.setClassName(pkg, cls) else i.setPackage(pkg)
        if (data != null) i.data = Uri.parse(data)
        for ((k, v) in extras) i.putExtra(k, v)
        return i
    }

    /** A file's command as the address the executor starts, with [argument] where the file says `{argument}`: escaped in the data, as typed in an extra. */
    internal fun fill(pkg: String, template: String, argument: String): String {
        if (!template.startsWith(TEMPLATE)) return template
        val parts = template.removePrefix(TEMPLATE).split(SEP)
        val extras = LinkedHashMap<String, String>()
        var i = 3
        while (i + 1 < parts.size) { extras[parts[i]] = parts[i + 1].replace(ARGUMENT, argument); i += 2 }
        return intent(pkg, parts[0].ifEmpty { null }, parts[1].ifEmpty { null }, parts[2].ifEmpty { null }?.let { AppSearch.address(it, argument) }, extras).toUri(Intent.URI_INTENT_SCHEME)
    }

    /**
     * [intent] as an address, if another app may start it: it leads to an activity of [pkg] itself,
     * open to other apps, that asks for no permission. Null otherwise.
     */
    internal fun safe(pkg: String, intent: Intent): String? {
        val i = Intent(intent)
        if (i.component == null) i.setPackage(pkg) else if (i.component?.packageName != pkg) return null
        val a = pm.resolveActivity(i, 0)?.activityInfo ?: return null
        if (a.packageName != pkg || !a.exported || a.permission != null || !a.enabled) return null
        i.setClassName(a.packageName, a.name)
        i.selector = null; i.clipData = null; i.flags = 0
        return i.toUri(Intent.URI_INTENT_SCHEME)
    }

    private fun text(p: XmlPullParser, res: Resources, ns: String?, name: String): String? {
        if (p !is XmlResourceParser) return p.getAttributeValue(ns, name)
        val id = p.getAttributeResourceValue(ns, name, 0)
        return if (id != 0) runCatching { res.getString(id) }.getOrNull() else p.getAttributeValue(ns, name)
    }

    private fun words(list: String?, single: Boolean = false) = list.orEmpty().split(',', '|').map { it.trim() }.filter { it.isNotEmpty() && it.length <= MAX_WORD && !(single && ' ' in it) }.take(MAX_WORDS)

    // ---- as rows

    private fun allowed(owner: String, s: io.github.kuscher.booklight.data.Settings = prefs.now) = s.appCommands && owner !in s.mutedApps

    private fun row(c: Command, score: Double, argument: String = "") = Result(
        id = "cmd:${c.owner}/${c.id}", provider = id, kind = Kind.COMMAND, title = c.title, icon = Icon.App(c.owner, c.cls, me), score = score,
        // The app is named once in words: where other rows say what kind they are.
        label = c.app,
        actions = listOf(Action("open", context.getString(R.string.action_open), Effect.Open(c.owner, fill(c.owner, c.intent, argument)))),
    )

    override fun byIds(ids: Set<String>): List<Result> = commands.filter { "cmd:${it.owner}/${it.id}" in ids && allowed(it.owner) }.map { row(it, 1.0) }

    override suspend fun query(q: Query): List<Result> {
        val s = prefs.now
        if (!s.appCommands) return emptyList()
        val text = q.text
        // By the app's name alone its commands are listed only under the app that leads, three of them, from three letters.
        val lead = if (text.length >= 3) apps.best(text) else null
        var byName = 0
        val out = ArrayList<Result>()
        for (c in commands) {
            if (c.owner in s.mutedApps) continue
            val title = Matcher.score(text, c.title)
            // A file's keywords count less than a title: a file cannot buy the first row.
            val word = c.words.maxOfOrNull { Matcher.keyword(text, it) }?.let { it * 0.9 } ?: 0.0
            val named = Matcher.score(text, c.app) >= Matcher.PREFIX
            val both = if (named) 0.0 else Matcher.score(text, c.app + " " + c.title) * 0.95        // "chrome new tab"
            var score = maxOf(title, word, both) * 0.9
            if (score <= 0 && named && c.owner == lead && byName < 3) { score = BY_NAME; byName++ }
            if (score > 0) out += row(c, score)
        }
        // "spotify daft punk": the search inside the app that is named, as the row its chip shows.
        out += chips.sentence(text)
        return out
    }

    /** The keywords apps' files declare, as scopes. One the user has entered before is entered by its keyword and a Space, like Booklight's own. */
    /** Something another app offers, as it would be typed: an example for the list of everything. Null when no app offers anything. */
    fun example(): String? = commands.filter { allowed(it.owner) && it.owner != context.packageName }.let { all ->
        // "new event" says more than "my apps": something that makes a new thing, if any app offers one; else two words.
        val short = all.filter { it.title.length <= 24 }
        (short.firstOrNull { t -> NEW.any { t.title.trim().startsWith(it, ignoreCase = true) } } ?: short.firstOrNull { ' ' in it.title.trim() } ?: all.firstOrNull())?.title?.lowercase()
    }

    fun scopes(taken: Set<String>): List<Scope> {
        val s = prefs.now
        // With the switch for what other apps offer off, an app's chip is still there for what does not depend on it: Play
        // on a music app's row, and `play`. (Search is gone from the chip then: `AppSearches.allowed`.)
        if (!s.appCommands) return chips.scopes()
        val used = HashSet(taken)
        return keywords.filter { it.owner !in s.mutedApps }.mapNotNull { k ->
            // A keyword Booklight or the user has, or another app took first, is not this app's to take.
            val free = k.keywords.filter { it.lowercase() !in used }
            if (free.isEmpty()) return@mapNotNull null
            used += free.map { it.lowercase() }
            Ext(k, free, "ext:${k.owner}/${k.id}" in s.usedScopes)
        } + chips.scopes()
    }

    private inner class Ext(private val k: Keyword, override val keywords: List<String>, override val spaceEnters: Boolean) : Scope {
        override val key = "ext:${k.owner}/${k.id}"
        override val name = k.name
        override val title get() = "${k.name} · ${k.app}"
        override val symbol = "bolt"
        override val hint = k.hint.ifEmpty { k.name }
        override val about: String get() = k.app

        override suspend fun rows(arg: String): List<Result> {
            val text = arg.trim()
            val template = k.template
            if (template != null) return if (text.isEmpty()) emptyList() else listOf(row(Command(k.owner, k.app, k.cls, k.id, "${k.name}: $text", emptyList(), template), 1.0, text).copy(learnable = false))
            return k.choices.map { it to if (text.isEmpty()) 1.0 else maxOf(Matcher.score(text, it.title), it.words.maxOfOrNull { w -> Matcher.keyword(text, w) } ?: 0.0) }
                .filter { it.second > 0 }.sortedByDescending { it.second }.map { (c, _) -> row(c, 1.0).copy(learnable = false) }
        }
    }

    private companion object {
        const val SETTINGS = "com.android.settings"
        /** The attributes of a `<shortcut>`, in the ascending order the system asks for. */
        val SHORTCUT = intArrayOf(android.R.attr.enabled, android.R.attr.shortcutId, android.R.attr.shortcutShortLabel, android.R.attr.shortcutLongLabel).sortedArray()
        const val META = "io.github.kuscher.booklight.commands"
        const val ARGUMENT = AppSearch.ARGUMENT
        /** How a file's command is kept until it is run: its parts, not yet an address (the typed text is still to come). */
        const val TEMPLATE = "booklight-open:"
        const val SEP = "\u0001"
        /** Found only by the app's name: under everything that matched by its own. */
        const val BY_NAME = 0.45
        /** How the title of a command that makes a new thing begins, in the languages Booklight speaks. */
        private val NEW = listOf("new ", "neue", "neu ")
        // What one app may put in: enough for any honest file, little enough that a hostile one costs nothing.
        const val MAX_EVENTS = 4000
        const val MAX_COMMANDS = 40
        const val MAX_KEYWORDS = 8
        const val MAX_CHOICES = 12
        const val MAX_WORDS = 8
        const val MAX_WORD = 24
        const val MAX_TITLE = 60
        const val MAX_EXTRAS = 8
        const val MAX_VALUE = 512
        const val MAX_ID = 40
        const val MAX_DATA = 2048
    }
}
