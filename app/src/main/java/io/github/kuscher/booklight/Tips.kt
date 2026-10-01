package io.github.kuscher.booklight

import android.content.Context
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.data.Settings

/**
 * Tips: one thing Booklight can do, shown under the empty field when the panel has been open for a
 * moment and nothing was typed. A tip is what to type and, in a few words, what happens; the
 * example is the same text Booklight types on "Try it".
 *
 * They are a short list in the string resources (`tips`: id, symbol, example, name, what happens),
 * `?` first, since it unlocks the rest. A tip stays the tip until it has been on screen three
 * seconds in all, or was tried; then the next. What the user has already used is passed over. After
 * one pass they end; a tip that a later version adds is new and gets its turn.
 */
class Tips(private val context: Context, private val prefs: Prefs) {
    class Tip(val id: String, val symbol: String, val example: String, val name: String, val rest: String)

    fun all(): List<Tip> {
        val prompt = prefs.now.prompts.firstOrNull { it.keyword.isNotEmpty() }?.let { "${it.keyword} ${context.getString(R.string.guide_prompt_text)}" }
        return context.resources.getStringArray(R.array.tips).mapNotNull { row ->
            val f = row.split('|')
            if (f.size < 5) return@mapNotNull null
            Tip(f[0], f[1], (if (f[2] == "{prompt}") prompt else f[2]) ?: return@mapNotNull null, f[3], f[4])
        }
    }

    /** The tip to show now, or null when there is none left. */
    fun next(s: Settings): Tip? = all().firstOrNull { it.id !in s.tipsSeen && it.id !in s.used }

    companion object {
        /** How long a tip is on screen, all its showings together, before the next one takes its place. */
        const val READ_MS = 3000L

        /** The tip [id] was on screen for [ms] more. */
        fun shown(s: Settings, id: String, ms: Long): Settings {
            if (id in s.tipsSeen) return s
            val total = (if (s.tipId == id) s.tipMs else 0L) + ms.coerceAtLeast(0)
            return if (total >= READ_MS) seen(s, id) else s.copy(tipId = id, tipMs = total)
        }

        /** The tip [id] has had its turn (it was read, or tried). */
        fun seen(s: Settings, id: String): Settings = s.copy(tipsSeen = (s.tipsSeen + id).distinct(), tipId = "", tipMs = 0)
    }
}
