package io.github.kuscher.booklight.providers

import android.content.Context
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.data.Prefs

/**
 * The system's own keyboard shortcuts, as answers: "snap" shows Action + [. Booklight cannot press
 * them (only the system may); it can say what they are. From a hand-kept table
 * (`keys_table.xml`, English and German), read on the HP Googlebook.
 */
class KeysProvider(private val context: Context, private val prefs: Prefs) : Provider {
    override val id = "keys"

    private class Entry(val key: String, val caps: List<String>, val title: String, val words: List<String>)

    private val entries: List<Entry> by lazy {
        val res = context.resources
        val ids = res.getStringArray(R.array.keys_ids)
        val combos = res.getStringArray(R.array.keys_combos)
        val titles = res.getStringArray(R.array.keys_titles)
        val words = res.getStringArray(R.array.keys_words)
        ids.indices.map { Entry(ids[it], combos[it].split('+').filter(String::isNotEmpty).map(::cap), titles[it], words[it].split(',').filter(String::isNotBlank)) }
    }

    /** A key as it is written on a cap: modifiers in the device's language, arrows as arrows. */
    private fun cap(token: String): String = when (token) {
        "Action" -> context.getString(R.string.key_action)
        "Ctrl" -> context.getString(R.string.key_ctrl)
        "Shift" -> context.getString(R.string.key_shift)
        "Alt" -> context.getString(R.string.key_alt)
        "Space" -> context.getString(R.string.key_space)
        "Enter" -> context.getString(R.string.key_enter)
        "Del" -> context.getString(R.string.key_del)
        "Tab" -> context.getString(R.string.key_tab)
        "Esc" -> context.getString(R.string.key_esc)
        "Overview" -> context.getString(R.string.key_overview)
        "Left" -> "←"; "Right" -> "→"; "Up" -> "↑"; "Down" -> "↓"
        else -> token
    }

    private fun row(e: Entry, score: Double) = Result(
        id = "key:${e.key}", provider = id, kind = Kind.COMMAND, title = e.title, icon = Icon.Symbol("key"), score = score,
        label = context.getString(R.string.kind_key), body = Body.Keys(e.caps),
        actions = listOf(
            // Enter does not press the keys: the name says what it does.
            Action("all", context.getString(R.string.action_all_keys), Effect.Internal("shortcuts"), symbol = "open"),
            Action("copy", context.getString(R.string.action_copy), Effect.CopyText(e.caps.joinToString(" + "))),
        ),
    )

    private fun score(text: String, e: Entry) = maxOf(Matcher.score(text, e.title), e.words.maxOfOrNull { Matcher.keyword(text, it) } ?: 0.0)

    /** In the ordinary list they rank under apps, settings and Booklight's own rows. */
    override suspend fun query(q: Query): List<Result> {
        if (!prefs.now.showKeys || q.text.length < 2) return emptyList()
        return entries.mapNotNull { e -> score(q.text, e).takeIf { it >= Matcher.WORD_PREFIX * 0.85 }?.let { row(e, it * 0.8) } }
    }

    /** For the scope: the ones that match, best first; nothing typed, the table's own order (windows first). */
    fun find(text: String): List<Result> =
        if (text.isBlank()) entries.map { row(it, 1.0) }
        else entries.map { it to score(text, it) }.filter { it.second > 0 }.sortedByDescending { it.second }.map { row(it.first, 1.0) }
}

/** `k`, then Tab: the system's keyboard shortcuts. A user's own link called `k` keeps the letter. */
class KeysScope(private val context: Context, private val keys: KeysProvider, private val users: () -> Set<String>) : Scope {
    override val key = "keys"
    private val all = context.getString(R.string.keys_keys).split(',')
    override val keywords: List<String> get() = users().let { u -> all.filter { it.length > 1 || it.lowercase() !in u } }
    override val name: String = context.getString(R.string.keys_name)
    override val title: String = context.getString(R.string.keys_title)
    override val symbol = "key"
    override val hint: String = context.getString(R.string.keys_hint)
    override val about: String = context.getString(R.string.keys_about)

    override suspend fun rows(arg: String): List<Result> = keys.find(arg.trim()).map { it.copy(learnable = false) } + Result(
        id = "${SearchEngine.HANDOVER}keys", provider = key, kind = Kind.COMMAND, title = context.getString(R.string.keys_all), subtitle = context.getString(R.string.keys_all_sub),
        icon = Icon.Symbol("open"), score = 1.0, learnable = false, label = "",
        actions = listOf(Action("all", context.getString(R.string.action_open), Effect.Internal("shortcuts"), symbol = "open")),
    )
}

/** `s`, then Tab: every settings page Booklight knows; the last row hands over to the Settings app's own search. */
class SettingsScope(private val context: Context, private val pages: SettingsProvider, private val users: () -> Set<String>) : Scope {
    override val key = "settings"
    private val all = context.getString(R.string.settings_keys_list).split(',')
    override val keywords: List<String> get() = users().let { u -> all.filter { it.length > 1 || it.lowercase() !in u } }
    override val name: String = context.getString(R.string.settings_name)
    override val title: String = context.getString(R.string.settings_title)
    override val symbol = "settings"
    override val hint: String = context.getString(R.string.settings_hint)
    override val about: String = context.getString(R.string.settings_about)

    override suspend fun rows(arg: String): List<Result> = pages.find(arg.trim()).map { it.copy(learnable = false) } + Result(
        id = "${SearchEngine.HANDOVER}settings", provider = key, kind = Kind.COMMAND, title = context.getString(R.string.settings_search), subtitle = context.getString(R.string.settings_search_sub),
        icon = Icon.Symbol("open"), score = 1.0, learnable = false, label = "",
        actions = listOf(Action("search", context.getString(R.string.action_open), Effect.OpenSettings(SEARCH), symbol = "open")),
    )

    private companion object { const val SEARCH = "android.settings.APP_SEARCH_SETTINGS" }
}
