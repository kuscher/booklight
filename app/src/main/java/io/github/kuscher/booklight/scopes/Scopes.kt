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
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.data.SiteEntry

/**
 * Every scope there is: the built-in ones, and one for each of the user's keyword searches.
 * **A new scope is one more line in [fixed].**
 */
class Scopes(private val context: Context, private val prefs: Prefs) {
    private val fixed: List<Scope> = listOf()

    // The user's keyword searches change rarely: made again only when the saved list is another one.
    private var sitesFor: List<SiteEntry>? = null
    private var sites: List<Scope> = emptyList()

    fun all(): List<Scope> {
        val saved = prefs.now.sites
        if (saved !== sitesFor) {
            sitesFor = saved
            val taken = fixed.flatMapTo(HashSet()) { it.keywords }
            sites = saved.filter { it.url.contains("%s") && it.keyword !in taken }.map { SiteScope(context, it.site()) }
        }
        return fixed + sites
    }
}

/** A keyword search: `yt lofi` searches YouTube for "lofi". */
class SiteScope(private val context: Context, private val site: Site) : Scope {
    override val key = "site:${site.keyword}"
    override val keywords = listOf(site.keyword)
    override val name = site.name
    override val symbol = "search"
    override val hint: String = context.getString(R.string.scope_site_hint, site.name)

    override suspend fun rows(arg: String): List<Result> {
        if (arg.isBlank()) return emptyList()
        val url = site.search(arg.trim())
        return listOf(Result(
            id = "web:site:${site.keyword}", provider = key, kind = Kind.WEB,
            title = context.getString(R.string.web_search_title, site.name, arg.trim()),
            icon = Icon.Symbol("search"), score = 1.0, learnable = false,
            actions = listOf(
                Action("search", context.getString(R.string.action_search), Effect.OpenUrl(url)),
                Action("link", context.getString(R.string.action_copy_link), Effect.CopyText(url)),
            ),
        ))
    }
}
