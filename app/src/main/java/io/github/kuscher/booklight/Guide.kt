package io.github.kuscher.booklight

import android.content.Context
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.Templates
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.providers.AppCommands
import io.github.kuscher.booklight.providers.AppsProvider

/**
 * Everything Booklight does, as one table: each keyword and each kind of row, with a name, a line
 * about it and an example that can be typed as it stands. The list behind `?`, the Commands page
 * of the window and the tips are all this table; it is kept in the string resources
 * (`guide`: id, group, symbol, the scope's key, example, name, line), so an example is in the
 * device's language and uses the keywords of that language.
 *
 * Three examples are the user's own: a link of theirs, a prompt of theirs, something another app
 * offers. Where there is none, that line is left out.
 */
class Guide(private val context: Context, private val prefs: Prefs, private val commands: AppCommands) {
    class Entry(val id: String, val group: String, val symbol: String, /** The scope it is the keyword of, or null. */ val scope: String?, val example: String, val name: String, val line: String)

    fun entries(): List<Entry> {
        val s = prefs.now
        val link = s.sites.firstOrNull { Templates.takesArgument(it.url) }?.let { "${it.keyword} ${context.getString(R.string.guide_link_text)}" }
        val prompt = s.prompts.firstOrNull { it.keyword.isNotEmpty() }?.let { "${it.keyword} ${context.getString(R.string.guide_prompt_text)}" }
        val offered = commands.example()
        return context.resources.getStringArray(R.array.guide).mapNotNull { row ->
            val f = row.split('|')
            if (f.size < 7) return@mapNotNull null
            val example = when (f[4]) { "{link}" -> link; "{prompt}" -> prompt; "{command}" -> offered; else -> f[4] } ?: return@mapNotNull null
            Entry(f[0], f[1], f[2], f[3].ifEmpty { null }, example, f[5], f[6])
        }
    }

    /** What a group is called: "Open", "Write down"… */
    fun group(id: String): String = context.getString(when (id) {
        "open" -> R.string.guide_group_open; "write" -> R.string.guide_group_write; "time" -> R.string.guide_group_time
        "answers" -> R.string.guide_group_answers; "text" -> R.string.guide_group_text; "controls" -> R.string.guide_group_controls
        else -> R.string.guide_group_booklight
    })

    /**
     * Which line of the table running [action] on [row] was a use of, inside [chip] or outside any:
     * what has been used is not suggested again. Null when it is none of them.
     */
    fun used(chip: Scope?, row: Result, action: Action): String? {
        val key = chip?.key
        return when {
            key == null -> when (row.provider) {
                AppsProvider.ID -> if (action.id in PLACES) "places" else "apps"
                "calc" -> "sums"
                "appcommands" -> "commands"
                "web" -> "web"
                "answers" -> if (row.id == "answer:color") "color" else "password"
                "flights" -> "flight"
                else -> null
            }
            key.startsWith("site:") -> "links"
            key.startsWith("prompt:") -> "prompts"
            else -> key.takeIf { k -> ids.contains(k) }
        }
    }

    private val ids: Set<String> by lazy { context.resources.getStringArray(R.array.guide).mapTo(HashSet()) { it.substringBefore('|') } }

    private companion object {
        val PLACES = setOf("left", "right", "full", "p3l", "p3m", "p3r", "p23l", "p23r", "ptl", "ptr", "pbl", "pbr", "pc")
    }
}
