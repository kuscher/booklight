package io.github.kuscher.booklight.providers

import android.content.Context
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.AppPage
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Verb

/**
 * An app's own pages in the system's Settings, as actions on the app's row: its notifications, its
 * language, what it opens by default, its battery use. A word typed after the app's name ("spotify
 * notifications") puts that one page on the row, behind its arrow, and arms it, as "chrome uninstall"
 * arms Uninstall. Untyped they are not listed: the row's list is long enough. Booklight opens the page
 * and switches nothing: no plain app can.
 */
internal object AppPages {
    private class Entry(val id: String, val page: AppPage, val label: Int, val words: Int, val symbol: String)

    private val all = listOf(
        Entry("notifications", AppPage.NOTIFICATIONS, R.string.page_notifications, R.string.verb_notifications, "bell"),
        Entry("language", AppPage.LANGUAGE, R.string.page_language, R.string.verb_language, "globe"),
        Entry("defaults", AppPage.DEFAULTS, R.string.page_defaults, R.string.verb_defaults, "link"),
        Entry("battery", AppPage.BATTERY, R.string.page_battery, R.string.verb_battery, "battery"),
    )

    /** The ids of these actions: running one is a use of the `pages` line in the list of everything. */
    val ids: Set<String> = all.mapTo(HashSet()) { it.id }

    /**
     * The words that arm a page. Only after the name: at the start "battery" and "language" begin
     * searches of their own ("battery saver"), and the app list would read an app into what follows.
     */
    fun verbs(context: Context): List<Verb> = all.map { Verb(it.id, context.getString(it.words).split(',').filter(String::isNotBlank), atStart = false) }

    /**
     * The four pages of the app [pkg], in the order they are listed under its row. None for the Settings
     * app itself: "settings battery" means the system's Battery page, and with these the Settings app,
     * Battery use armed, would stand above that page.
     */
    fun actions(context: Context, pkg: String): List<Action> = if (pkg == SETTINGS) emptyList() else
        all.map { Action(it.id, context.getString(it.label), Effect.AppSettings(it.page, pkg), symbol = it.symbol, more = true) }

    private const val SETTINGS = "com.android.settings"
}
