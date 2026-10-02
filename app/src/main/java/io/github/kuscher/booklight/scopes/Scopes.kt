package io.github.kuscher.booklight.scopes

import android.content.Context
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.ai.OnDevice
import io.github.kuscher.booklight.data.PromptEntry
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.Site
import io.github.kuscher.booklight.core.Jumps
import io.github.kuscher.booklight.core.Requests
import io.github.kuscher.booklight.core.Templates
import io.github.kuscher.booklight.data.OwnCommandEntry
import io.github.kuscher.booklight.data.Notes
import io.github.kuscher.booklight.data.Prefs
import java.time.LocalDate
import io.github.kuscher.booklight.data.SiteEntry
import io.github.kuscher.booklight.providers.Dials
import io.github.kuscher.booklight.providers.KeysProvider
import io.github.kuscher.booklight.providers.KeysScope
import io.github.kuscher.booklight.providers.SettingsProvider
import io.github.kuscher.booklight.providers.SettingsScope
import io.github.kuscher.booklight.providers.LevelScope
import io.github.kuscher.booklight.providers.PlayScope

/**
 * Every scope there is: the built-in ones, one for each of the user's links and app commands that
 * take text, one for each of their prompts, and, while it is being shown, the text another app handed over.
 * **A new scope is one more line in [fixed].**
 */
class Scopes(
    private val context: Context, private val prefs: Prefs, dials: Dials, private val notes: Notes, private val web: (String) -> Result,
    settings: SettingsProvider, keys: KeysProvider, private val ai: OnDevice, guide: io.github.kuscher.booklight.Guide,
    /** The keywords other apps declare, given the ones already taken. */
    private val others: (Set<String>) -> List<Scope> = { emptyList() },
    /** What the pinned window shows, if there is one. */
    pinned: () -> io.github.kuscher.booklight.pin.Pinned? = { null },
    /** A flight number's row, for the keyword `flight`. */
    private val flights: io.github.kuscher.booklight.providers.FlightsProvider,
    /** An installed app by its package: the name and the icon of the app an app command of the user's own asks. */
    private val installed: (String) -> io.github.kuscher.booklight.providers.AppsProvider.Installed? = { null },
) {
    /** The tasks: it remembers what was ticked while the panel is open. */
    val todo = TodoScope(context, notes)

    /** Which of the keywords that open another app have one here to answer them. */
    private val takers = Takers(context)

    private val fixed: List<Scope> = listOf(
        MailScope(context), NoteScope(context, notes), NotesScope(context, notes), todo, PinScope(context, pinned), EventScope(context), RemindScope(context), TimerScope(context), AlarmScope(context),
        NewScope(context), AskScope(context), HelpScope(context, guide) { prefs.now.used },
        io.github.kuscher.booklight.providers.FlightScope(context, flights, ::linkKeywords),
        EmojiScope(context, prefs, symbols = false), EmojiScope(context, prefs, symbols = true), LettersScope(context, prefs), QrScope(context), ColorScope(context),
        SnipScope(context, prefs), TextScope(context, null, web, { prefs.now.prompts }, ai, flight = { flights.found(it) }), TranslateScope(context, ai, ::linkKeywords) { editable },
        LevelScope(context, dials, volume = true), LevelScope(context, dials, volume = false), PlayScope(context),
        // `s` and `k`: the two that give their letter up to a link of the user's own with that keyword.
        SettingsScope(context, settings, ::linkKeywords), KeysScope(context, keys, ::linkKeywords),
        // Another app, with something filled in. Each is there only where an app answers it, and gives its keyword up to a link or a prompt of the user's.
        GoScope(context, takers, ::userKeywords), MeetScope(context, takers, ::userKeywords),
        CallScope(context, takers, ::userKeywords), SmsScope(context, takers, ::userKeywords),
        WaScope(context, takers, ::userKeywords), TgScope(context, takers, ::userKeywords),
    )

    /** The keywords of the user's own links and app commands: `s` and `k` yield to them. */
    private fun linkKeywords(): Set<String> = prefs.now.let { s -> (s.sites.map { it.keyword } + s.ownCommands.map { it.keyword }).filter { it.isNotEmpty() }.mapTo(HashSet()) { it.lowercase() } }

    /** The keywords of the user's own links, app commands and prompts: a short keyword that came later (`go`, `wa`) does not take one from any of them. */
    private fun userKeywords(): Set<String> = linkKeywords() + prefs.now.prompts.map { it.keyword.lowercase() }

    init {
        // Whether an app answers `call`, `sms`, `wa` and `tg` is asked for the first time here, at the start of the process: the
        // questions go to a background thread ([Takers]), and their answers are in before anything is typed.
        fixed.filterIsInstance<ReachScope>().forEach { it.there }
    }

    /**
     * The keywords the built-in scopes answer to: a link of the user's can't have one of these. The
     * one-letter ones are not among them: a user's `s` link keeps working, and Settings is then
     * reached by `settings`. A keyword that needs an app (`wa`) is among them whether or not the app is
     * here: what is reserved does not change when an app comes or goes, and working it out asks the
     * package manager nothing.
     */
    val reserved: Set<String> = fixed.flatMapTo(HashSet()) { s -> (if (s is ReachScope) s.claims else s.keywords).map { it.lowercase() } }.filterTo(HashSet()) { it.length > 1 || it == "?" }

    /** Text sent from another app: a scope of its own for as long as the panel shows it. */
    @Volatile var incoming: Scope? = null; private set

    /** The text came from a field that can be edited, and the app it is in takes text back: an answer can replace it there. */
    @Volatile var editable = false; private set

    /** [copied]: it is what the user copied, opened from the line for a fresh copy; else another app handed it over. */
    fun receive(text: String, editable: Boolean = false, copied: Boolean = false): Scope =
        TextScope(context, text, web, { prefs.now.prompts }, ai, { this.editable }, if (copied) TextFrom.COPY else TextFrom.HANDED, flight = { flights.found(it) }).also { incoming = it; this.editable = editable }

    /** The panel closed: someone else's text is not kept, and neither is what was read of the user's notes. */
    fun forget() { incoming = null; editable = false; todo.closed(); notes.forget() }

    // The user's links, prompts and app commands change rarely: their scopes are made again only when a saved list is another one.
    private var sitesFor: List<SiteEntry>? = null
    private var promptsFor: List<PromptEntry>? = null
    private var commandsFor: List<OwnCommandEntry>? = null
    private var sites: List<Scope> = emptyList()
    private var prompts: List<Scope> = emptyList()
    private var commands: List<Scope> = emptyList()

    fun all(): List<Scope> {
        val saved = prefs.now
        if (saved.sites !== sitesFor || saved.prompts !== promptsFor || saved.ownCommands !== commandsFor) {
            sitesFor = saved.sites; promptsFor = saved.prompts; commandsFor = saved.ownCommands
            sites = saved.sites.filter { Templates.takesArgument(it.url) && it.keyword.lowercase() !in reserved }.map { SiteScope(context, it.site()) }
            // A prompt's keyword is its own only if nobody has it: Booklight, a link, or a prompt before it.
            val taken = HashSet(reserved) + saved.sites.map { it.keyword.lowercase() }
            val seen = HashSet<String>()
            prompts = saved.prompts.map { p ->
                val k = p.keyword.lowercase()
                PromptScope(context, p, if (k.isEmpty() || k in taken || !seen.add(k)) "" else p.keyword, ai) { editable }
            }
            // An app command that takes text is a keyword like a link with a placeholder; the same rule for whose the keyword is.
            commands = saved.ownCommands.filter { c -> Requests.takesArgument(c.intent) && c.keyword.lowercase().let { k -> k.isNotEmpty() && k !in taken && seen.add(k) } }
                .map { OwnCommandScope(context, it, installed) }
        }
        // Booklight's own first, then the user's, then other apps': an app never takes a keyword somebody already has.
        val mine = fixed + sites + prompts + commands
        return mine + others(mine.flatMapTo(HashSet()) { sc -> sc.keywords.map { it.lowercase() } }) + listOfNotNull(incoming)
    }
}

/** A link that takes text: `yt lofi` searches YouTube for "lofi". `{clipboard}` and `{date}` are filled in too. */
class SiteScope(private val context: Context, private val site: Site) : Scope {
    override val key = "site:${site.keyword}"
    override val keywords = listOf(site.keyword)
    override val name = site.name
    override val symbol = "search"
    override val hint: String = context.getString(R.string.scope_site_hint, site.name)
    override val title: String = hint

    override suspend fun rows(arg: String): List<Result> {
        val text = arg.trim()
        if (text.isEmpty()) return emptyList()
        val url = Templates.fill(site.url, text, if ("{clipboard}" in site.url) clipboardText(context).orEmpty() else "", LocalDate.now())
        // One of Booklight's own links whose app is installed and can be searched (`store`, `yt`): Enter lands in the app,
        // with the app's icon on the row to say so, and the browser is the next action. Else the browser, as the link says.
        val app = (context.applicationContext as BooklightApp).commands.search.link(site, text)
        val search = Result(
            id = "web:site:${site.keyword}", provider = key, kind = Kind.WEB,
            title = context.getString(R.string.web_search_title, site.name, text),
            icon = app?.first ?: Icon.Symbol("search"), score = 1.0, learnable = false,
            actions = listOfNotNull(
                app?.let { Action("search", context.getString(R.string.action_search), it.second, symbol = "open") },
                Action(if (app == null) "search" else "web", context.getString(if (app == null) R.string.action_search else R.string.action_search_web), Effect.OpenUrl(url), symbol = if (app == null) "open" else "globe"),
                Action("link", context.getString(R.string.action_copy_link), Effect.CopyText(url)),
            ),
        )
        // On GitHub, owner/repo (and owner/repo#12) goes straight there.
        val jump = if (site.keyword == "gh") Jumps.github(text) else null
        return listOfNotNull(jump?.let {
            Result(
                id = "web:jump", provider = key, kind = Kind.WEB, title = it.removePrefix("https://"), subtitle = context.getString(R.string.action_open_link),
                icon = Icon.Symbol("globe"), score = 1.0, learnable = false,
                actions = listOf(Action("open", context.getString(R.string.action_open_link), Effect.OpenUrl(it)), Action("link", context.getString(R.string.action_copy_link), Effect.CopyText(it))),
            )
        }, search)
    }
}

/** An app command of the user's own that takes text: `sp daft punk` asks Spotify to search for "daft punk". Enter asks the app; nothing comes back. */
class OwnCommandScope(private val context: Context, private val command: OwnCommandEntry, private val installed: (String) -> io.github.kuscher.booklight.providers.AppsProvider.Installed?) : Scope {
    override val key = "own:${command.id}"
    override val keywords = listOf(command.keyword)
    override val name = command.name
    override val symbol = "open"
    override val hint: String = command.name
    /** The app it asks, as other apps' keywords say theirs. */
    override val about: String? get() = installed(command.app)?.label

    override suspend fun rows(arg: String): List<Result> {
        val text = arg.trim()
        if (text.isEmpty()) return emptyList()
        // Text that does not fit (a number is wanted) has no row: the way out to the web is what is left.
        val intent = Requests.fill(command.intent, text) ?: return emptyList()
        return listOf(io.github.kuscher.booklight.providers.ownCommand(context, command, installed(command.app), "${command.name}: $text", 1.0, intent).copy(provider = key, learnable = false))
    }
}
