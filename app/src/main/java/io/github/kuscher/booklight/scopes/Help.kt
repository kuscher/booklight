package io.github.kuscher.booklight.scopes

import android.content.Context
import io.github.kuscher.booklight.Guide
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope

/**
 * `?`: everything Booklight does, each with an example. A question mark as the first character is
 * the scope at once, without a Space; typing narrows the list. Enter on a keyword's row makes that
 * keyword the chip; on any row "Try it" has Booklight type the example, a letter at a time, so the
 * user sees what they would type. With nothing typed the list starts with what has not been used
 * yet. The last row opens the whole list, in the window.
 *
 * Not a search: no "search the web for…" closes it. `help` finds its row but is not a keyword:
 * "help with taxes" is a search.
 */
class HelpScope(private val context: Context, private val guide: Guide, private val used: () -> List<String>) : Scope {
    override val key = "help"
    override val keywords = listOf("?")
    override val name: String = context.getString(R.string.help_name)
    override val title: String = context.getString(R.string.help_title)
    override val symbol = "list"
    override val hint: String = context.getString(R.string.help_hint)
    override val about: String = context.getString(R.string.help_about)
    override val web = false
    override val words: List<String> = context.getString(R.string.help_words).split(',')

    override suspend fun rows(arg: String): List<Result> {
        val text = arg.trim()
        val all = guide.entries()
        val done = used().toSet()
        val found = if (text.isEmpty()) all.sortedBy { it.id in done }     // what is new to the user first, each part in the table's order
        else all.map { it to maxOf(Matcher.score(text, it.name), Matcher.score(text, it.example), if (it.line.contains(text, ignoreCase = true)) 0.3 else 0.0) }
            .filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
        val tryIt = context.getString(R.string.action_try)
        val rows = found.take(ROWS).map { e ->
            // The armed action is named by what it types: on the selected row the strip stands where the example stood.
            val type = Action("try", e.example.trim().ifEmpty { tryIt }, Effect.Type(e.example), keepOpen = true, symbol = "play")
            Result(
                id = "guide:${e.id}", provider = key, kind = Kind.OTHER, title = e.name, subtitle = e.line.ifEmpty { null }, icon = Icon.Symbol(e.symbol), score = 1.0, learnable = false,
                label = e.example,
                // A keyword: Enter (and Tab) makes it the chip, ready for the user's own text. Anything else: Booklight shows how it is typed.
                actions = listOf(if (e.scope != null) Action("enter", e.example.substringBefore(' ') + " …", Effect.EnterScope(e.scope), keepOpen = true, symbol = "edit") else type),
            )
        }
        return rows + Result(
            id = "guide:all", provider = key, kind = Kind.OTHER, title = context.getString(R.string.help_all), icon = Icon.Symbol("booklight"), score = 1.0, learnable = false,
            actions = listOf(Action("open", context.getString(R.string.action_open), Effect.Internal("commands"))),
        )
    }

    private companion object {
        /** With the row that opens the whole list this is what a full panel holds. */
        const val ROWS = 7
    }
}
