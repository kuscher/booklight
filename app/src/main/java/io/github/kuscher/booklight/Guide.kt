package io.github.kuscher.booklight

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Requests
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Templates
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.providers.AppCommands
import io.github.kuscher.booklight.providers.AppPages
import io.github.kuscher.booklight.providers.AppsProvider
import io.github.kuscher.booklight.scopes.ReachScope

/**
 * Everything Booklight does, as one table: each keyword and each kind of row, with a name, a line
 * about it and an example that can be typed as it stands. The list behind `?`, the Commands page
 * of the window and the tips are all this table; it is kept in the string resources
 * (`guide`: id, group, symbol, the scope's key, example, name, line), so an example is in the
 * device's language and uses the keywords of that language.
 *
 * Five examples are the user's own: a link of theirs, a prompt of theirs, an app command of theirs,
 * something another app offers, an app of theirs that can be searched. Where there is none, that line
 * is left out. So is the line of a keyword that needs an app this device does not have.
 */
class Guide(private val context: Context, private val prefs: Prefs, private val commands: AppCommands) {
    class Entry(val id: String, val group: String, val symbol: String, /** The scope it is the keyword of, or null. */ val scope: String?, val example: String, val name: String, val line: String)

    fun entries(): List<Entry> {
        val s = prefs.now
        val link = s.sites.firstOrNull { Templates.takesArgument(it.url) }?.let { "${it.keyword} ${context.getString(R.string.guide_link_text)}" }
        val prompt = s.prompts.firstOrNull { it.keyword.isNotEmpty() }?.let { "${it.keyword} ${context.getString(R.string.guide_prompt_text)}" }
        val offered = commands.example()
        val searched = appSearch()
        // A keyword no app on this device answers (call, sms, wa, tg) is not there: its line is left out too. So are the Clock's lists without a clock.
        val gone = (context.applicationContext as BooklightApp).scopes.all().filterIsInstance<ReachScope>().filter { !it.there }.mapTo(HashSet()) { it.key }
        // An app command of theirs: its keyword and a text if it takes one; else its keyword, or its name. (One that takes
        // text and has no keyword yet, a copy, cannot be typed.)
        val own = s.ownCommands.firstNotNullOfOrNull { c ->
            if (!Requests.takesArgument(c.intent)) c.keyword.ifEmpty { c.name.lowercase() }
            else if (c.keyword.isEmpty()) null else "${c.keyword} ${context.getString(R.string.guide_link_text)}"
        }
        return context.resources.getStringArray(R.array.guide).mapNotNull { row ->
            val f = row.split('|')
            if (f.size < 7 || f[3] in gone || (f[0] == "alarms" && !clock)) return@mapNotNull null
            val example = when (f[4]) { "{link}" -> link; "{prompt}" -> prompt; "{command}" -> offered; "{own}" -> own; APP_SEARCH -> searched; else -> f[4] } ?: return@mapNotNull null
            Entry(f[0], f[1], f[2], f[3].ifEmpty { null }, example, f[5], f[6])
        }
    }

    /**
     * A search inside an app of this device, as it would be typed ("spotify jazz"): an app whose name does not begin
     * with a keyword, so that typing it makes no chip. Null when no app here can be searched.
     */
    fun appSearch(): String? = commands.search.example { name -> (context.applicationContext as BooklightApp).engine.scopeFor("$name x") == null }

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
                AppsProvider.ID -> if (action.id in PLACES) "places" else if (action.id in AppPages.ids) "pages" else "apps"
                "commands" -> if (row.id == "command:alarms" || row.id == "command:timers") "alarms" else null
                "calc" -> "sums"
                "appcommands" -> if (row.id.startsWith(SearchEngine.IN_APP)) "appsearch" else "commands"
                "web" -> "web"
                "answers" -> if (row.id == "answer:color") "color" else "password"
                "flights" -> "flight"
                "dials" -> if (row.id == "dial:media") "media" else null
                "user" -> "own".takeIf { row.id.startsWith("own:") }
                else -> null
            }
            key.startsWith("site:") -> "links"
            key.startsWith("prompt:") -> "prompts"
            key.startsWith(SearchEngine.IN_APP) -> "appsearch"
            key.startsWith("own:") -> "own"
            else -> key.takeIf { k -> ids.contains(k) }
        }
    }

    /** Whether a clock shows its lists (the rows `alarms` and `timers`): asked once, as the rows are. */
    private val clock: Boolean by lazy { Intent(AlarmClock.ACTION_SHOW_ALARMS).resolveActivity(context.packageManager) != null }

    private val ids: Set<String> by lazy { context.resources.getStringArray(R.array.guide).mapTo(HashSet()) { it.substringBefore('|') } }

    companion object {
        /** In place of an example: an app of this device that can be searched, and something to look for. */
        const val APP_SEARCH = "{appsearch}"
        private val PLACES = setOf("left", "right", "full", "p3l", "p3m", "p3r", "p23l", "p23r", "ptl", "ptr", "pbl", "pbr", "pc")
    }
}
