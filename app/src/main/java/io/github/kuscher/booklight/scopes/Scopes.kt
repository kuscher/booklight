package io.github.kuscher.booklight.scopes

import android.content.Context
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
import io.github.kuscher.booklight.core.Templates
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
 * Every scope there is: the built-in ones, one for each of the user's links that take text, one
 * for each of their prompts, and, while it is being shown, the text another app handed over.
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
) {
    /** The tasks: it remembers what was ticked while the panel is open. */
    val todo = TodoScope(context, notes)

    private val fixed: List<Scope> = listOf(
        MailScope(context), NoteScope(context, notes), NotesScope(context, notes), todo, PinScope(context, pinned), EventScope(context), RemindScope(context), TimerScope(context), AlarmScope(context),
        NewScope(context), AskScope(context), HelpScope(context, guide) { prefs.now.used },
        io.github.kuscher.booklight.providers.FlightScope(context, flights, ::linkKeywords),
        EmojiScope(context, prefs, symbols = false), EmojiScope(context, prefs, symbols = true), LettersScope(context, prefs), QrScope(context), ColorScope(context),
        SnipScope(context, prefs), TextScope(context, null, web, { prefs.now.prompts }, ai, flight = { flights.found(it) }), TranslateScope(context, ai, ::linkKeywords) { editable },
        LevelScope(context, dials, volume = true), LevelScope(context, dials, volume = false), PlayScope(context),
        // `s` and `k`: the two that give their letter up to a link of the user's own with that keyword.
        SettingsScope(context, settings, ::linkKeywords), KeysScope(context, keys, ::linkKeywords),
    )

    /** The keywords of the user's own links: `s` and `k` yield to them. */
    private fun linkKeywords(): Set<String> = prefs.now.sites.mapTo(HashSet()) { it.keyword.lowercase() }

    /**
     * The keywords the built-in scopes answer to: a link of the user's can't have one of these. The
     * one-letter ones are not among them: a user's `s` link keeps working, and Settings is then
     * reached by `settings`.
     */
    val reserved: Set<String> = fixed.flatMapTo(HashSet()) { s -> s.keywords.map { it.lowercase() } }.filterTo(HashSet()) { it.length > 1 || it == "?" }

    /** Text sent from another app: a scope of its own for as long as the panel shows it. */
    @Volatile var incoming: Scope? = null; private set

    /** The text came from a field that can be edited, and the app it is in takes text back: an answer can replace it there. */
    @Volatile var editable = false; private set

    /** [copied]: it is what the user copied, opened from the line for a fresh copy; else another app handed it over. */
    fun receive(text: String, editable: Boolean = false, copied: Boolean = false): Scope =
        TextScope(context, text, web, { prefs.now.prompts }, ai, { this.editable }, if (copied) TextFrom.COPY else TextFrom.HANDED, flight = { flights.found(it) }).also { incoming = it; this.editable = editable }

    /** The panel closed: someone else's text is not kept, and neither is what was read of the user's notes. */
    fun forget() { incoming = null; editable = false; todo.closed(); notes.forget() }

    // The user's links and prompts change rarely: their scopes are made again only when a saved list is another one.
    private var sitesFor: List<SiteEntry>? = null
    private var promptsFor: List<PromptEntry>? = null
    private var sites: List<Scope> = emptyList()
    private var prompts: List<Scope> = emptyList()

    fun all(): List<Scope> {
        val saved = prefs.now
        if (saved.sites !== sitesFor || saved.prompts !== promptsFor) {
            sitesFor = saved.sites; promptsFor = saved.prompts
            sites = saved.sites.filter { Templates.takesArgument(it.url) && it.keyword.lowercase() !in reserved }.map { SiteScope(context, it.site()) }
            // A prompt's keyword is its own only if nobody has it: Booklight, a link, or a prompt before it.
            val taken = HashSet(reserved) + saved.sites.map { it.keyword.lowercase() }
            val seen = HashSet<String>()
            prompts = saved.prompts.map { p ->
                val k = p.keyword.lowercase()
                PromptScope(context, p, if (k.isEmpty() || k in taken || !seen.add(k)) "" else p.keyword, ai) { editable }
            }
        }
        // Booklight's own first, then the user's, then other apps': an app never takes a keyword somebody already has.
        val mine = fixed + sites + prompts
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
        val search = Result(
            id = "web:site:${site.keyword}", provider = key, kind = Kind.WEB,
            title = context.getString(R.string.web_search_title, site.name, text),
            icon = Icon.Symbol("search"), score = 1.0, learnable = false,
            actions = listOf(
                Action("search", context.getString(R.string.action_search), Effect.OpenUrl(url), symbol = "open"),
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
