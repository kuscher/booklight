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
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Templates
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.data.Recipes
import io.github.kuscher.booklight.scopes.clipboardText
import java.time.LocalDate

/**
 * What the user made: links that need no text (those that take text are scopes), and recipes.
 * Found by keyword or by name; each can be run, edited, and deleted with a second Enter.
 */
class User(private val context: Context, private val prefs: Prefs) : Provider {
    override val id = "user"

    private fun match(text: String, keyword: String, name: String) = when {
        keyword.isNotEmpty() && keyword.equals(text, ignoreCase = true) -> 1.0
        else -> maxOf(Matcher.score(text, name), if (keyword.isNotEmpty()) Matcher.keyword(text, keyword) else 0.0)
    }

    private fun own(kind: String, id: String) = listOf(
        Action("edit", context.getString(R.string.action_edit), Effect.Edit(kind, id)),
        Action("delete", context.getString(R.string.action_delete), Effect.Delete(kind, id), keepOpen = true, symbol = "trash", danger = true, confirm = true, done = context.getString(R.string.done_deleted)),
    )

    override suspend fun query(q: Query): List<Result> {
        val s = prefs.now
        val out = ArrayList<Result>()
        for (link in s.sites) {
            if (Templates.takesArgument(link.url)) continue
            val score = match(q.text, link.keyword, link.name)
            if (score <= 0) continue
            // {clipboard} and {date} are filled in when the row is made; the clipboard is only read if the link asks for it.
            val url = Templates.fill(link.url, "", if ("{clipboard}" in link.url) clipboardText(context).orEmpty() else "", LocalDate.now())
            out += Result(
                id = "link:${link.keyword}", provider = id, kind = Kind.COMMAND, title = link.name, subtitle = url, icon = Icon.Symbol("link"), score = score,
                actions = listOf(
                    Action("open", context.getString(R.string.action_open), Effect.OpenUrl(url)),
                    Action("link", context.getString(R.string.action_copy_link), Effect.CopyText(url)),
                ) + own("quicklink", link.keyword),
            )
        }
        for (r in s.recipes) {
            val score = match(q.text, r.keyword, r.name)
            if (score <= 0) continue
            out += Result(
                id = "recipe:${r.id}", provider = id, kind = Kind.COMMAND, title = r.name, subtitle = Recipes.describe(context, r), icon = Icon.Symbol("bolt"), score = score,
                actions = listOf(Action("run", context.getString(R.string.action_run), Recipes.effects(r), symbol = "play")) + own("recipe", r.id),
            )
        }
        return out
    }
}
