package io.github.kuscher.booklight.core

import java.net.URLEncoder

/** A search engine: where a search goes, and where suggestions come from while you type. `%s` is the text. */
data class Engine(val id: String, val name: String, val searchUrl: String, val suggestUrl: String) {
    fun search(text: String): String = searchUrl.replace("%s", encode(text))
    fun suggest(text: String): String = suggestUrl.replace("%s", encode(text))
    /** The host suggestions are fetched from, for telling the user where their text goes. */
    val suggestHost: String get() = suggestUrl.substringAfter("://").substringBefore('/')
}

object Engines {
    val all = listOf(
        Engine("google", "Google", "https://www.google.com/search?q=%s", "https://suggestqueries.google.com/complete/search?client=firefox&ie=utf-8&oe=utf-8&q=%s"),
        Engine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com/ac/?type=list&q=%s"),
        Engine("bing", "Bing", "https://www.bing.com/search?q=%s", "https://api.bing.com/osjson.aspx?query=%s"),
        Engine("brave", "Brave Search", "https://search.brave.com/search?q=%s", "https://search.brave.com/api/suggest?q=%s"),
        Engine("ecosia", "Ecosia", "https://www.ecosia.org/search?q=%s", "https://ac.ecosia.org/autocomplete?type=list&q=%s"),
    )
    val default: Engine = all.first()
    fun byId(id: String): Engine = all.firstOrNull { it.id == id } ?: default
}

/** A keyword search: `yt lofi` searches YouTube for "lofi". `%s` in [url] is the text. */
data class Site(val keyword: String, val name: String, val url: String) {
    fun search(text: String): String = url.replace("%s", encode(text))
}

object Sites {
    val defaults = listOf(
        Site("g", "Google", "https://www.google.com/search?q=%s"),
        Site("yt", "YouTube", "https://www.youtube.com/results?search_query=%s"),
        Site("w", "Wikipedia", "https://en.wikipedia.org/w/index.php?search=%s"),
        Site("maps", "Maps", "https://www.google.com/maps/search/%s"),
        Site("gh", "GitHub", "https://github.com/search?q=%s"),
        Site("store", "Play Store", "https://play.google.com/store/search?c=apps&q=%s"),
        Site("drive", "Drive", "https://drive.google.com/drive/search?q=%s"),
    )

    /**
     * The site and the text to search for, when [text] is a keyword, a space and something more.
     * The keyword alone ("yt", "g ") is ordinary text: it may be the start of an app's name.
     */
    fun parse(text: String, sites: List<Site> = defaults): Pair<Site, String>? {
        val t = text.trimStart()
        val space = t.indexOf(' ')
        if (space <= 0) return null
        val rest = t.substring(space + 1).trim()
        if (rest.isEmpty()) return null
        val key = t.substring(0, space)
        val site = sites.firstOrNull { it.keyword.equals(key, ignoreCase = true) } ?: return null
        return site to rest
    }
}

private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8")
