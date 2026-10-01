package io.github.kuscher.booklight.core

/** What the user typed, as typed; [text] is what providers match against. */
data class Query(val raw: String) {
    val text: String = raw.trim()
    val isEmpty: Boolean get() = text.isEmpty()
}

/** What a result is. Drives the row's label and how the ranker weighs it. */
enum class Kind { APP, ANSWER, SETTING, COMMAND, WEB, SUGGESTION, OTHER }

/**
 * A picture for a row, as data: the core has no drawables. The app turns it into pixels
 * (`ui/Icons.kt`), and a plugin in another process could send the same thing.
 */
sealed interface Icon {
    /** The launcher icon of an activity. [user] is the profile's serial number (0 = the owner). */
    data class App(val packageName: String, val className: String, val user: Long = 0) : Icon
    /** One of Booklight's own symbols, by name (`ui/Icons.kt`). */
    data class Symbol(val name: String) : Icon
}

/**
 * What running an action does, as data. The core never touches Android: the app's `Executor`
 * performs effects. New abilities are a new effect plus a few lines there.
 */
sealed interface Effect {
    data class LaunchApp(val packageName: String, val className: String, val user: Long = 0) : Effect
    data class AppInfo(val packageName: String, val className: String, val user: Long = 0) : Effect
    /** The app's page in the store it came from. */
    data class StorePage(val packageName: String) : Effect
    data class OpenUrl(val url: String) : Effect
    data class CopyText(val text: String) : Effect
    /** A system settings screen, by its `android.settings.…` intent action. */
    data class OpenSettings(val action: String) : Effect
    /** One of Booklight's own pages or commands (`settings`, `hotkey`…). */
    data class Internal(val command: String) : Effect
}

/** One thing you can do with a result. The first action of a result is what Enter does. */
data class Action(
    val id: String,
    val label: String,
    val effect: Effect,
    /** Leave the panel open afterwards (copying an answer keeps it open only if this is set). */
    val keepOpen: Boolean = false,
)

data class Result(
    /** Stable across sessions (`app:pkg/cls`, `setting:wifi`…): what the ranker learns on. */
    val id: String,
    val provider: String,
    val kind: Kind,
    val title: String,
    val subtitle: String? = null,
    val icon: Icon,
    /** How well it matches the query, 0..1, from the provider. The engine adds what it has learned. */
    val score: Double,
    val actions: List<Action>,
    /** Results that answer the query in place (a sum, a conversion) show this large. */
    val answer: String? = null,
    /** False for results that are the query itself (a sum, a web search): nothing to learn. */
    val learnable: Boolean = true,
)

/**
 * A source of results. Everything Booklight can find or do comes from a provider, so a new
 * ability is a new provider registered in `BooklightApp`; nothing else changes.
 *
 * [query] runs off the main thread and is cancelled when the user types on. It must be quick
 * (the engine gives up on a provider after its time budget) and must not throw for odd input.
 */
interface Provider {
    val id: String
    suspend fun query(q: Query): List<Result>
    /** What to offer before anything is typed. */
    suspend fun zeroState(): List<Result> = emptyList()
}
