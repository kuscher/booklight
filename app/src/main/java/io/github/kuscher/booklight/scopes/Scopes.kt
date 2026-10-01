package io.github.kuscher.booklight.scopes

import android.content.Context
import io.github.kuscher.booklight.R
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
import io.github.kuscher.booklight.providers.LevelScope
import io.github.kuscher.booklight.providers.PlayScope

/**
 * Every scope there is: the built-in ones, one for each of the user's links that take text, and,
 * while it is being shown, the text another app handed over.
 * **A new scope is one more line in [fixed].**
 */
class Scopes(private val context: Context, private val prefs: Prefs, dials: Dials, notes: Notes, private val web: (String) -> Result) {
    private val fixed: List<Scope> = listOf(
        MailScope(context), NoteScope(context, notes), EventScope(context), RemindScope(context), TimerScope(context), AlarmScope(context),
        NewScope(context), AskScope(context),
        EmojiScope(context, prefs, symbols = false), EmojiScope(context, prefs, symbols = true), QrScope(context), ColorScope(context),
        SnipScope(context, prefs), TextScope(context, null, web),
        LevelScope(context, dials, volume = true), LevelScope(context, dials, volume = false), PlayScope(context),
    )

    /** Text sent from another app: a scope of its own for as long as the panel shows it. */
    @Volatile var incoming: Scope? = null; private set

    fun receive(text: String): Scope = TextScope(context, text, web).also { incoming = it }

    /** The panel closed: someone else's text is not kept. */
    fun forget() { incoming = null }

    // The user's links change rarely: their scopes are made again only when the saved list is another one.
    private var sitesFor: List<SiteEntry>? = null
    private var sites: List<Scope> = emptyList()

    fun all(): List<Scope> {
        val saved = prefs.now.sites
        if (saved !== sitesFor) {
            sitesFor = saved
            val taken = fixed.flatMapTo(HashSet()) { it.keywords }
            sites = saved.filter { Templates.takesArgument(it.url) && it.keyword !in taken }.map { SiteScope(context, it.site()) }
        }
        return fixed + sites + listOfNotNull(incoming)
    }
}

/** A link that takes text: `yt lofi` searches YouTube for "lofi". `{clipboard}` and `{date}` are filled in too. */
class SiteScope(private val context: Context, private val site: Site) : Scope {
    override val key = "site:${site.keyword}"
    override val keywords = listOf(site.keyword)
    override val name = site.name
    override val symbol = "search"
    override val hint: String = context.getString(R.string.scope_site_hint, site.name)

    override suspend fun rows(arg: String): List<Result> {
        val text = arg.trim()
        if (text.isEmpty()) return emptyList()
        val url = Templates.fill(site.url, text, if ("{clipboard}" in site.url) clipboardText(context).orEmpty() else "", LocalDate.now())
        val search = Result(
            id = "web:site:${site.keyword}", provider = key, kind = Kind.WEB,
            title = context.getString(R.string.web_search_title, site.name, text),
            icon = Icon.Symbol("search"), score = 1.0, learnable = false,
            actions = listOf(
                Action("search", context.getString(R.string.action_search), Effect.OpenUrl(url)),
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
