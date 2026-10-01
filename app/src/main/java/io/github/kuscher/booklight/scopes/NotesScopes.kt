package io.github.kuscher.booklight.scopes

import android.content.Context
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.NoteText
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.data.Notes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** A day as a row says it at its right end: Today, Yesterday, Monday, 24 Sep. [stamp] starts with 2026-10-01. */
internal fun shortDay(context: Context, stamp: String, today: LocalDate = LocalDate.now()): String {
    val day = runCatching { LocalDate.parse(stamp.take(10)) }.getOrNull() ?: return ""
    val locale = context.resources.configuration.locales[0]
    return when (val ago = ChronoUnit.DAYS.between(day, today)) {
        0L -> context.getString(R.string.day_today)
        1L -> context.getString(R.string.day_yesterday)
        in 2..6 -> day.format(DateTimeFormatter.ofPattern("EEEE", locale))
        else -> day.format(DateTimeFormatter.ofPattern(if (day.year == today.year || ago < 300) "d MMM" else "d MMM yyyy", locale))
    }
}

/** The one row of a notes scope before a folder is chosen. */
internal fun chooseFolder(context: Context, provider: String) = Result(
    id = "notes:choose", provider = provider, kind = Kind.OTHER, title = context.getString(R.string.notes_choose), subtitle = context.getString(R.string.notes_choose_sub),
    icon = Icon.Symbol("folder"), score = 1.0, learnable = false,
    actions = listOf(Action("choose", context.getString(R.string.win_choose), Effect.Grant("notes"), symbol = "folder")),
)

/** `notes milk`: the lines of Notes.md that say so, newest first; nothing typed, the latest. */
class NotesScope(private val context: Context, private val notes: Notes) : Scope {
    override val key = "notes"
    override val keywords: List<String> = context.getString(R.string.notes_keys).split(',')
    override val name: String = context.getString(R.string.notes_name)
    override val title: String = context.getString(R.string.notes_title)
    override val symbol = "note"
    override val hint: String = context.getString(R.string.notes_hint)
    override val about: String = context.getString(R.string.notes_about)

    override suspend fun rows(arg: String): List<Result> {
        if (!notes.ready) return listOf(chooseFolder(context, key))
        val text = notes.read(NoteText.NOTES) ?: return emptyList()
        val found = NoteText.find(text, arg, FIT)
        if (found.isEmpty()) return listOf(Result(
            id = "notes:none", provider = key, kind = Kind.OTHER, title = context.getString(if (text.isBlank()) R.string.notes_empty else R.string.notes_no_match),
            subtitle = if (text.isBlank()) context.getString(R.string.notes_empty_sub) else null, icon = Icon.Symbol("note"), score = 1.0, learnable = false, actions = emptyList(),
        ))
        // A row is its line in the file, so narrowing the search moves rows instead of drawing them anew.
        return found.map { n ->
            Result(
                id = "note:${n.line}", provider = key, kind = Kind.OTHER, title = n.text, icon = Icon.Symbol("note"), score = 1.0, learnable = false,
                label = shortDay(context, n.date),
                actions = listOf(
                    Action("copy", context.getString(R.string.action_copy), Effect.CopyText(n.text.replace(" ↵ ", "\n"))),
                    Action("pin", context.getString(R.string.action_pin), Effect.Pin("text", n.text.replace(" ↵ ", "\n")), symbol = "pin"),
                    Action("file", context.getString(R.string.action_open_file), Effect.OpenNote(NoteText.NOTES), symbol = "open"),
                ),
            )
        }
    }

    private companion object { const val FIT = 7 }
}

/**
 * `todo call the bank` adds a task to Todo.md; `todo` alone lists what is open. Enter on a task
 * ticks it, and the row stays where it is, ticked, with Undo: a second Enter takes the tick back
 * instead of ticking the next task. What was ticked is gone the next time the panel opens.
 */
class TodoScope(private val context: Context, private val notes: Notes) : Scope {
    override val key = "todo"
    override val keywords: List<String> = context.getString(R.string.todo_keys).split(',')
    override val name: String = context.getString(R.string.todo_name)
    override val symbol = "check"
    override val hint: String = context.getString(R.string.todo_hint)
    override val about: String = context.getString(R.string.todo_about)

    /** The lines ticked while this panel was open: they stay in the list, ticked, until it closes. */
    private val ticked = HashSet<Int>()
    fun closed() = synchronized(ticked) { ticked.clear() }
    fun ticked(line: Int, done: Boolean) = synchronized(ticked) { if (done) ticked.add(line) else ticked.remove(line) }

    override suspend fun rows(arg: String): List<Result> {
        if (!notes.ready) return listOf(chooseFolder(context, key))
        val text = arg.trim()
        val all = NoteText.tasks(notes.read(NoteText.TODO) ?: return emptyList())
        val kept = synchronized(ticked) { HashSet(ticked) }
        val words = io.github.kuscher.booklight.core.Matcher.fold(text).split(' ').filter { it.isNotEmpty() }
        val shown = all.filter { (!it.done || it.line in kept) && io.github.kuscher.booklight.core.Matcher.fold(it.text).let { t -> words.all { w -> t.contains(w) } } }
        val rows = ArrayList<Result>()
        if (text.isNotEmpty()) rows += Result(
            id = "todo:add", provider = key, kind = Kind.OTHER, title = context.getString(R.string.todo_add, text), subtitle = context.getString(R.string.todo_add_sub),
            icon = Icon.Symbol("plus"), score = 1.0, learnable = false,
            actions = listOf(Action("add", context.getString(R.string.action_add), Effect.AddTodo(text), symbol = "check", done = context.getString(R.string.done_todo))),
        )
        if (text.isEmpty() && shown.isEmpty()) rows += Result(
            id = "todo:none", provider = key, kind = Kind.OTHER, title = context.getString(R.string.todo_none), subtitle = context.getString(R.string.todo_none_sub),
            icon = Icon.Symbol("check"), score = 1.0, learnable = false, actions = emptyList(),
        )
        // A task is its line in the file, not its words: two tasks that say the same are two tasks.
        shown.take(FIT - rows.size).mapTo(rows) { t ->
            Result(
                id = "todo:${t.line}", provider = key, kind = Kind.OTHER, title = t.text, icon = Icon.Symbol("check"), score = 1.0, learnable = false,
                label = shortDay(context, t.date), body = Body.Task(t.done),
                actions = listOf(
                    Action(
                        "tick", context.getString(if (t.done) R.string.action_undo else R.string.action_done), Effect.TickTodo(t.line, t.text, !t.done), keepOpen = true,
                        symbol = if (t.done) "again" else "check", done = context.getString(if (t.done) R.string.done_unticked else R.string.done_ticked),
                    ),
                    Action("copy", context.getString(R.string.action_copy), Effect.CopyText(t.text)),
                ),
            )
        }
        return rows
    }

    private companion object { const val FIT = 7 }
}

/**
 * `pin gate B22, 14:05`: a line in a small window that stays on top. With nothing typed: what is
 * pinned, with the way to take it away (a keyboard launcher must not make a window only the
 * pointer can close). A pinned text exists nowhere else, so a new one says what it replaces.
 */
class PinScope(private val context: Context, private val pinned: () -> io.github.kuscher.booklight.pin.Pinned?) : Scope {
    override val key = "pin"
    override val keywords: List<String> = context.getString(R.string.pin_keys).split(',')
    override val name: String = context.getString(R.string.action_pin)
    override val symbol = "pin"
    override val hint: String = context.getString(R.string.pin_hint)
    override val about: String = context.getString(R.string.pin_about)

    override suspend fun rows(arg: String): List<Result> {
        val text = arg.trim()
        val now = pinned()
        if (text.isEmpty()) return listOf(
            if (now == null) Result(
                id = "pin:none", provider = key, kind = Kind.OTHER, title = context.getString(R.string.pin_none), subtitle = context.getString(R.string.pin_none_sub),
                icon = Icon.Symbol("pin"), score = 1.0, learnable = false, actions = emptyList(),
            ) else Result(
                id = "pin:now", provider = key, kind = Kind.OTHER, title = now.title(context), subtitle = now.label(context), icon = Icon.Symbol("pin"), score = 1.0, learnable = false,
                actions = listOfNotNull(
                    Action("unpin", context.getString(R.string.action_unpin), Effect.Unpin, keepOpen = true, symbol = "trash", done = context.getString(R.string.done_unpinned)),
                    Action("copy", context.getString(R.string.action_copy), Effect.CopyText(now.text)).takeIf { now.kind != "timer" && now.kind != "qr" },
                ),
            ),
        )
        val old = now?.takeIf { it.kind == "text" }?.title(context)?.let { if (it.length > 24) it.take(23) + "…" else it }
        return listOf(Result(
            id = "pin:new", provider = key, kind = Kind.OTHER, title = text, subtitle = old?.let { context.getString(R.string.pin_replaces, it) } ?: context.getString(R.string.pin_sub),
            icon = Icon.Symbol("pin"), score = 1.0, learnable = false,
            actions = listOf(
                Action("pin", context.getString(R.string.action_pin), Effect.Pin("text", text), symbol = "pin"),
                Action("copy", context.getString(R.string.action_copy), Effect.CopyText(text)),
            ),
        ))
    }
}
