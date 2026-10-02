package io.github.kuscher.booklight.providers

import android.content.Context
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Read
import io.github.kuscher.booklight.core.Requests
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Templates
import io.github.kuscher.booklight.core.What
import io.github.kuscher.booklight.data.OwnCommandEntry
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.data.Recipes
import io.github.kuscher.booklight.scopes.clipboardText
import java.time.LocalDate

/**
 * What the user made: links and app commands that need no text (those that take text are scopes),
 * and recipes. Found by keyword or by name; each can be run, edited, and deleted with a second Enter.
 */
class User(private val context: Context, private val prefs: Prefs, private val apps: AppsProvider) : Provider {
    override val id = "user"

    private fun match(text: String, keyword: String, name: String) = when {
        keyword.isNotEmpty() && keyword.equals(text, ignoreCase = true) -> 1.0
        else -> maxOf(Matcher.score(text, name), if (keyword.isNotEmpty()) Matcher.keyword(text, keyword) else 0.0)
    }

    private fun own(kind: String, id: String) = listOf(
        Action("edit", context.getString(R.string.action_edit), Effect.Edit(kind, id)),
        Action("delete", context.getString(R.string.action_delete), Effect.Delete(kind, id), keepOpen = true, symbol = "trash", danger = true, confirm = true, done = context.getString(R.string.done_deleted)),
    )

    private fun link(link: io.github.kuscher.booklight.data.SiteEntry, score: Double): Result {
        // {clipboard} and {date} are filled in when the row is made; the clipboard is only read if the link asks for it.
        val url = Templates.fill(link.url, "", if ("{clipboard}" in link.url) clipboardText(context).orEmpty() else "", LocalDate.now())
        return Result(
            id = "link:${link.keyword}", provider = id, kind = Kind.COMMAND, title = link.name, subtitle = url, icon = Icon.Symbol("link"), score = score,
            actions = listOf(
                Action("open", context.getString(R.string.action_open), Effect.OpenUrl(url)),
                Action("link", context.getString(R.string.action_copy_link), Effect.CopyText(url)),
            ) + own("quicklink", link.keyword),
        )
    }

    private fun recipe(r: io.github.kuscher.booklight.data.RecipeEntry, score: Double) = Result(
        id = "recipe:${r.id}", provider = id, kind = Kind.COMMAND, title = r.name, subtitle = Recipes.describe(context, r), icon = Icon.Symbol("bolt"), score = score,
        actions = listOf(Action("run", context.getString(R.string.action_run), Recipes.effects(r), symbol = "play")) + own("recipe", r.id),
    )

    /** An app command that takes no text: Enter asks its app. It has that app's icon, and names the app where other rows say their kind. */
    private fun command(c: OwnCommandEntry, score: Double): Result? {
        val intent = Requests.fill(c.intent, "") ?: return null
        return ownCommand(context, c, apps.installed(c.app), c.name, score, intent, own("appcommand", c.id))
    }

    override suspend fun query(q: Query): List<Result> {
        val s = prefs.now
        val out = ArrayList<Result>()
        for (c in s.ownCommands) {
            val score = match(q.text, c.keyword, c.name)
            if (score > 0 && !Requests.takesArgument(c.intent)) command(c, score)?.let(out::add)
        }
        for (link in s.sites) {
            if (Templates.takesArgument(link.url)) continue
            val score = match(q.text, link.keyword, link.name)
            if (score > 0) out += link(link, score)
        }
        for (r in s.recipes) {
            val score = match(q.text, r.keyword, r.name)
            if (score > 0) out += recipe(r, score)
        }
        return out
    }

    /** A link that uses `{clipboard}` has no row here: making it would read the clipboard, and nobody asked. */
    override fun byIds(ids: Set<String>): List<Result> {
        val s = prefs.now
        return s.sites.filter { "link:${it.keyword}" in ids && !Templates.takesArgument(it.url) && "{clipboard}" !in it.url }.map { link(it, 1.0) } +
            s.recipes.filter { "recipe:${it.id}" in ids }.map { recipe(it, 1.0) } +
            s.ownCommands.filter { "own:${it.id}" in ids && !Requests.takesArgument(it.intent) }.mapNotNull { command(it, 1.0) }
    }
}

/**
 * The row of an app command of the user's own: outside a scope under its name, inside its scope with the
 * typed text. [intent] is what its app is asked, the text already in it; the executor checks it when it runs.
 * What Enter does is said by the command's kind: Search, Send, Play; a link and an action of the app's own, Open.
 */
fun ownCommand(context: Context, c: OwnCommandEntry, app: AppsProvider.Installed?, title: String, score: Double, intent: String, more: List<Action> = emptyList()): Result {
    val (word, symbol) = when ((Requests.read(c.intent) as? Read.Ok)?.request?.what) {
        What.SEARCH -> R.string.action_search to "search"
        What.SEND -> R.string.action_send to "send"
        What.PLAY -> R.string.action_play to "play"
        else -> R.string.action_open to "open"
    }
    return Result(
        id = "own:${c.id}", provider = "user", kind = Kind.COMMAND, title = title, icon = app?.let { Icon.App(it.pkg, it.cls, Recipes.me) } ?: Icon.Symbol("open"), score = score,
        label = app?.label ?: c.app,
        // (Its id stays the same whatever its word: it is what Enter runs, and what a row's actions are told apart by.)
        actions = listOf(Action("open", context.getString(word), Effect.Open(c.app, intent), symbol = symbol)) + more,
    )
}
