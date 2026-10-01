package io.github.kuscher.booklight.providers

import android.content.Context
import android.content.Intent
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Calc
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Sites
import io.github.kuscher.booklight.core.Web
import io.github.kuscher.booklight.data.Prefs

/** A sum typed into the panel is answered in place; Enter copies the answer. */
class CalcProvider(private val context: Context, private val prefs: Prefs) : Provider {
    override val id = "calc"
    override suspend fun query(q: Query): List<Result> {
        if (!prefs.now.showSums) return emptyList()
        val a = Calc.answer(q.text) ?: return emptyList()
        return listOf(Result(
            id = "calc", provider = id, kind = Kind.ANSWER, title = a, subtitle = q.text.removePrefix("=").trim(),
            icon = Icon.Symbol("calc"), score = 1.0, answer = a, learnable = false,
            actions = listOf(Action("copy", context.getString(R.string.action_copy), Effect.CopyText(a.replace(",", "")))),
        ))
    }
}

/**
 * System settings pages, from a hand-kept list of public `android.settings.…` actions (the
 * Settings app's own search index isn't open to apps). Names and keywords are string resources
 * (`settings_pages.xml` in each `res/values` folder), so "dunkel" finds Dark theme in German. Pages this device
 * doesn't have are dropped.
 */
class SettingsProvider(private val context: Context, private val prefs: Prefs) : Provider {
    override val id = "settings"

    private class Page(val key: String, val title: String, val action: String, val words: List<String>)

    private val pages: List<Page> by lazy {
        val pm = context.packageManager
        val res = context.resources
        val keys = res.getStringArray(R.array.settings_keys)
        val actions = res.getStringArray(R.array.settings_actions)
        val titles = res.getStringArray(R.array.settings_titles)
        val words = res.getStringArray(R.array.settings_words)
        keys.indices.map { Page(keys[it], titles[it], actions[it], words[it].split(',').filter(String::isNotBlank)) }
            .filter { Intent(it.action).resolveActivity(pm) != null }
    }

    override suspend fun query(q: Query): List<Result> {
        if (!prefs.now.showSettings) return emptyList()
        return pages.mapNotNull { p ->
            // Its name counts in full; a keyword ("dark" for Display) counts a little less.
            val s = maxOf(Matcher.score(q.text, p.title), p.words.maxOfOrNull { Matcher.keyword(q.text, it) } ?: 0.0)
            if (s <= 0) null else Result(
                id = "setting:${p.key}", provider = id, kind = Kind.SETTING, title = p.title,
                icon = Icon.Symbol("settings"), score = s,
                actions = listOf(Action("open", context.getString(R.string.action_open), Effect.OpenSettings(p.action))),
            )
        }
    }
}

/** Booklight's own pages, findable like anything else. */
class CommandsProvider(private val context: Context) : Provider {
    override val id = "commands"

    private class Command(val key: String, val title: Int, val subtitle: Int?, val words: Int, val effect: Effect)

    private val all = listOf(
        Command("settings", R.string.cmd_settings, null, R.string.cmd_settings_words, Effect.Internal("settings")),
        Command("shortcut", R.string.cmd_shortcut, R.string.cmd_shortcut_sub, R.string.cmd_shortcut_words, Effect.Internal("shortcuts")),
    )

    override suspend fun query(q: Query): List<Result> = all.mapNotNull { c ->
        val title = context.getString(c.title)
        val s = maxOf(Matcher.score(q.text, title), context.getString(c.words).split(',').maxOf { Matcher.keyword(q.text, it) })
        if (s <= 0) null else Result(
            id = "command:${c.key}", provider = id, kind = Kind.COMMAND, title = title, subtitle = c.subtitle?.let(context::getString),
            icon = Icon.Symbol("booklight"), score = s,
            actions = listOf(Action("open", context.getString(R.string.action_open), c.effect)),
        )
    }
}

/**
 * The way out to the web. A typed address opens in the browser; a keyword search (`yt lofi`)
 * searches that site; and anything typed can be searched for with the chosen engine. Choosing
 * one of these rows hands the text to the browser. (Suggestions while typing are a separate,
 * opt-in thing: `SuggestProvider`.)
 */
class WebProvider(private val context: Context, private val prefs: Prefs) : Provider {
    override val id = "web"

    override suspend fun query(q: Query): List<Result> {
        val t = q.text
        val s = prefs.now
        val out = ArrayList<Result>(3)
        Sites.parse(t, s.sites())?.let { (site, rest) ->
            out.add(Result(
                id = "web:site:${site.keyword}", provider = id, kind = Kind.WEB,
                title = context.getString(R.string.web_search_title, site.name, rest),
                icon = Icon.Symbol("search"), score = SearchEngine.URL_SCORE + 0.08, learnable = false,
                actions = listOf(Action("search", context.getString(R.string.action_search_engine, site.name), Effect.OpenUrl(site.search(rest)))),
            ))
        }
        Web.url(t)?.let { u ->
            out.add(Result(
                id = "web:url", provider = id, kind = Kind.WEB, title = t, subtitle = context.getString(R.string.action_open_link),
                icon = Icon.Symbol("globe"), score = SearchEngine.URL_SCORE + 0.05, learnable = false,
                actions = listOf(
                    Action("open", context.getString(R.string.action_open_link), Effect.OpenUrl(u)),
                    Action("copy", context.getString(R.string.action_copy), Effect.CopyText(u)),
                ),
            ))
        }
        val engine = s.engine()
        out.add(Result(
            id = "web:search", provider = id, kind = Kind.WEB, title = context.getString(R.string.web_search_title, engine.name, t),
            icon = Icon.Symbol("search"), score = 0.1, learnable = false,
            actions = listOf(Action("search", context.getString(R.string.action_search_engine, engine.name), Effect.OpenUrl(engine.search(t)))),
        ))
        return out
    }
}
