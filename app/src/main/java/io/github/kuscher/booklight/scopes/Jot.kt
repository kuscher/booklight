package io.github.kuscher.booklight.scopes

import android.content.Context
import android.text.format.DateFormat
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.EventDraft
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Jot
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.NewKind
import io.github.kuscher.booklight.core.NoteText
import io.github.kuscher.booklight.core.ReminderPlan
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.Slot
import io.github.kuscher.booklight.core.SlotState
import io.github.kuscher.booklight.data.Notes
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The scopes that jot something down: a mail, a note, an event, a reminder, a timer, an alarm,
 * a new file. Each reads its one line with a parser from the core and shows what it understood
 * as a preview row; Enter hands it to the app that keeps such things (Gmail's compose window, the
 * calendar's editor, the Clock). Nothing is sent or saved behind the user's back.
 */
abstract class JotScope(protected val context: Context, final override val key: String, keys: Int, name: Int, hint: Int, about: Int, final override val symbol: String) : Scope {
    final override val keywords: List<String> = context.getString(keys).split(',')
    final override val name: String = context.getString(name)
    final override val hint: String = context.getString(hint)
    final override val about: String = context.getString(about)

    protected fun text(id: Int, vararg args: Any) = context.getString(id, *args)
    protected fun slot(label: Int, value: String, guessed: Boolean = false) =
        Slot(context.getString(label), value, if (value.isEmpty()) SlotState.EMPTY else if (guessed) SlotState.GUESSED else SlotState.TYPED)

    /** The scope's one row: a preview of what was understood. No actions = not enough typed yet. */
    protected fun preview(caption: String, slots: List<Slot>, note: String?, actions: List<Action>) = Result(
        id = "jot:$key", provider = key, kind = Kind.OTHER, title = name, icon = Icon.Symbol(symbol), score = 1.0, learnable = false,
        body = Body.Slots(caption, slots, note?.takeIf { it.isNotBlank() }), actions = actions,
    )

    protected fun copy(text: String) = Action("copy", text(R.string.action_copy), Effect.CopyText(text))

    // Days and times as the device shows them: "Fri 2 Oct", "15:00" or "3:00 pm".
    private val locale: Locale get() = context.resources.configuration.locales[0]
    protected fun day(t: LocalDateTime): String = t.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
    protected fun clock(t: LocalDateTime): String = t.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale))
    /** As [clock], but a full hour on the 12-hour clock is just "3 PM": a range has to fit its slot. */
    private fun short(t: LocalDateTime): String = if (!DateFormat.is24HourFormat(context) && t.minute == 0) t.format(DateTimeFormatter.ofPattern("h a", locale)) else clock(t)

    protected fun span(e: EventDraft): String = when {
        e.allDay -> text(R.string.jot_all_day, day(e.start))
        else -> {
            // "3–4 PM", not "3:00 PM–4:00 PM": the half of the day is said once when both ends share it.
            val from = short(e.start)
            val to = short(e.end)
            val half = to.substringAfterLast(' ', "")
            "${day(e.start)}, ${if (half.isNotEmpty() && from.endsWith(" $half")) from.removeSuffix(" $half") else from}–$to"
        }
    }

    protected fun insert(e: EventDraft): Effect {
        // An all-day event is midnight to midnight in UTC, whatever the device's zone: that is how calendars store one.
        val zone = if (e.allDay) ZoneOffset.UTC else ZoneId.systemDefault()
        return Effect.InsertEvent(e.title, e.start.atZone(zone).toInstant().toEpochMilli(), e.end.atZone(zone).toInstant().toEpochMilli(), e.allDay, e.place)
    }
}

/** `mail anna@x.com Lunch? / See you at 1`: the mail app's compose window, filled in. */
class MailScope(context: Context) : JotScope(context, "mail", R.string.mail_keys, R.string.mail_name, R.string.mail_hint, R.string.mail_about, "mail") {
    override suspend fun rows(arg: String): List<Result> {
        val d = Jot.mail(arg)
        val all = listOf(d.subject, d.body).filter { it.isNotEmpty() }.joinToString("\n\n")
        return listOf(preview(
            text(R.string.mail_caption), listOf(slot(R.string.slot_to, d.to.joinToString(", ")), slot(R.string.slot_subject, d.subject)), d.body.replace("\n", " ↵ "),
            listOfNotNull(
                Action("compose", text(R.string.action_compose), Effect.Compose(d.to, d.subject, d.body), symbol = "edit"),
                copy(all).takeIf { all.isNotEmpty() },
            ),
        ))
    }
}

/**
 * `note buy milk / and coffee`: a dated line in Notes.md, in the folder the user chose once. When the
 * first word is the name of another `.md` file there ("ideas better onboarding"), a second row adds
 * the rest to that file; the default stays Notes.md. "Today's note" puts the line in the day's own file.
 */
class NoteScope(context: Context, private val notes: Notes) : JotScope(context, "note", R.string.note_keys, R.string.note_name, R.string.note_hint, R.string.note_about, "note") {
    override suspend fun rows(arg: String): List<Result> {
        val note = Jot.note(arg)
        val lines = note.lines()
        val ready = notes.ready
        val caption = if (ready) text(R.string.note_caption, notes.where ?: "") else text(R.string.note_caption_first)
        val actions = if (note.isEmpty()) emptyList() else listOfNotNull(
            if (ready) Action("add", text(R.string.action_add), Effect.AppendNote(note), symbol = "check", done = text(R.string.done_note))
            // The first note asks where notes should go, then goes there.
            else Action("choose", text(R.string.action_choose_folder), Effect.Grant("notes\n$note"), symbol = "folder"),
            Action("today", text(R.string.action_today), Effect.AppendNote(note, Effect.AppendNote.TODAY), symbol = "event", done = text(R.string.done_note_file, notes.today())).takeIf { ready },
            Action("pin", text(R.string.action_pin), Effect.Pin("text", note), symbol = "pin"),
            Action("keep", text(R.string.action_keep), Effect.KeepNote(note), symbol = "note"),
            copy(note),
        )
        val first = preview(caption, listOf(slot(R.string.slot_note, lines.first())), lines.drop(1).joinToString(" ↵ "), actions)
        // Another file of the folder, named by the first word: offered second, never in place of Notes.md.
        val other = if (ready) NoteText.target(arg, notes.files())?.let { (file, rest) ->
            val text = Jot.note(rest)
            Result(
                id = "jot:note:file", provider = key, kind = Kind.OTHER, title = text(R.string.note_to_file, file), subtitle = text.replace("\n", " ↵ "),
                icon = Icon.Symbol("file"), score = 1.0, learnable = false,
                actions = listOf(Action("add", text(R.string.action_add), Effect.AppendNote(text, file), symbol = "check", done = text(R.string.done_note_file, file))),
            )
        } else null
        return listOfNotNull(first, other)
    }
}

/** `event Fri 3pm Dentist @ Main St`: the calendar's editor, filled in. */
class EventScope(context: Context) : JotScope(context, "event", R.string.event_keys, R.string.event_name, R.string.event_hint, R.string.event_about, "event") {
    override suspend fun rows(arg: String): List<Result> {
        val e = Jot.event(arg, LocalDateTime.now())
        val actions = if (arg.isBlank()) emptyList() else listOf(
            Action("create", text(R.string.action_create), insert(e), symbol = "plus"),
            copy(listOf(e.title, span(e), e.place).filter { it.isNotEmpty() }.joinToString(", ")),
        )
        return listOf(preview(
            text(R.string.event_caption),
            listOfNotNull(slot(R.string.slot_when, span(e), guessed = !e.dayGiven || !e.timeGiven), slot(R.string.slot_title, e.title), slot(R.string.slot_where, e.place).takeIf { e.place.isNotEmpty() }),
            null, actions,
        ))
    }
}

/**
 * `remind 5pm call bank`, `remind in 20m stretch`. Booklight keeps nothing running, so the
 * reminder goes to what rings anyway: "in 20 minutes" is a Clock timer with the text as its
 * label, a time within the next day a Clock alarm, and anything later a calendar event.
 */
class RemindScope(context: Context) : JotScope(context, "remind", R.string.remind_keys, R.string.remind_name, R.string.remind_hint, R.string.remind_about, "bell") {
    override suspend fun rows(arg: String): List<Result> = listOf(when (val plan = Jot.reminder(arg, LocalDateTime.now())) {
        null -> preview(text(R.string.remind_caption_when), listOf(slot(R.string.slot_when, ""), slot(R.string.slot_note, arg.trim())), null, emptyList())
        is ReminderPlan.Timer -> preview(
            text(R.string.remind_caption_timer), listOf(slot(R.string.slot_in, length(plan.spec.seconds)), slot(R.string.slot_note, plan.spec.label)),
            text(R.string.timer_rings, clock(LocalDateTime.now().plusSeconds(plan.spec.seconds.toLong()))),
            listOf(Action("set", text(R.string.action_set), Effect.SetTimer(plan.spec.seconds, plan.spec.label), symbol = "check", done = text(R.string.done_timer))),
        )
        is ReminderPlan.Alarm -> preview(
            text(R.string.remind_caption_alarm), listOf(slot(R.string.slot_when, "${day(plan.at)}, ${clock(plan.at)}"), slot(R.string.slot_note, plan.spec.label)), null,
            listOf(Action("set", text(R.string.action_set), Effect.SetAlarm(plan.spec.hour, plan.spec.minute, plan.spec.label), symbol = "check", done = text(R.string.done_alarm))),
        )
        is ReminderPlan.Event -> preview(
            text(R.string.remind_caption_event), listOf(slot(R.string.slot_when, span(plan.draft)), slot(R.string.slot_title, plan.draft.title)), null,
            listOf(Action("create", text(R.string.action_create), insert(plan.draft), symbol = "plus")),
        )
    })
}

/** How long, as a clock: 10:00, 1:30:00. */
internal fun length(seconds: Int): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(Locale.ROOT, h, m, s) else "%d:%02d".format(Locale.ROOT, m, s)
}

/** `timer 10m tea`: the Clock app starts it. */
class TimerScope(context: Context) : JotScope(context, "timer", R.string.timer_keys, R.string.timer_name, R.string.timer_hint, R.string.timer_about, "timer") {
    override suspend fun rows(arg: String): List<Result> {
        val spec = Jot.timer(arg)
        val rings = spec?.let { text(R.string.timer_rings, clock(LocalDateTime.now().plusSeconds(it.seconds.toLong()))) }
        return listOf(Result(
            id = "jot:timer", provider = key, kind = Kind.OTHER, title = name, icon = Icon.Symbol("timer"), score = 1.0, learnable = false,
            answer = length(spec?.seconds ?: 0),
            subtitle = if (spec == null) text(R.string.timer_how_long) else listOf(spec.label, rings.orEmpty()).filter { it.isNotEmpty() }.joinToString(" · "),
            actions = spec?.let { listOf(
                Action("start", text(R.string.action_start), Effect.SetTimer(it.seconds, it.label), symbol = "play", done = text(R.string.done_timer)),
                // The Clock app's timer, and its countdown in a small window that stays on top.
                Action("pin", text(R.string.action_start_pin), Effect.Steps(listOf(Effect.SetTimer(it.seconds, it.label), Effect.Pin("timer", "", it.seconds.toLong(), it.label))), symbol = "pin"),
            ) } ?: emptyList(),
        ))
    }
}

/** `alarm 7:30 gym`: the Clock app sets it. */
class AlarmScope(context: Context) : JotScope(context, "alarm", R.string.alarm_keys, R.string.alarm_name, R.string.alarm_hint, R.string.alarm_about, "bell") {
    override suspend fun rows(arg: String): List<Result> {
        val now = LocalDateTime.now()
        val spec = Jot.alarm(arg, now)
        val at = spec?.let { s -> now.withHour(s.hour).withMinute(s.minute).withSecond(0).let { if (it.isAfter(now)) it else it.plusDays(1) } }
        return listOf(Result(
            id = "jot:alarm", provider = key, kind = Kind.OTHER, title = name, icon = Icon.Symbol("bell"), score = 1.0, learnable = false,
            answer = at?.let(::clock) ?: "–:––",
            subtitle = if (spec == null || at == null) text(R.string.alarm_what_time) else listOf(spec.label, day(at)).filter { it.isNotEmpty() }.joinToString(" · "),
            actions = spec?.let { listOf(Action("set", text(R.string.action_set), Effect.SetAlarm(it.hour, it.minute, it.label), symbol = "check", done = text(R.string.done_alarm))) } ?: emptyList(),
        ))
    }
}

/** `new file ideas.md`, `new folder Projects`, `new doc Q3 plan`. Nothing typed yet: the kinds there are. */
class NewScope(context: Context) : JotScope(context, "new", R.string.new_keys, R.string.new_name, R.string.new_hint, R.string.new_about, "plus") {
    private fun kind(k: NewKind) = text(when (k) {
        NewKind.FILE -> R.string.new_file; NewKind.FOLDER -> R.string.new_folder; NewKind.DOC -> R.string.new_doc
        NewKind.SHEET -> R.string.new_sheet; NewKind.SLIDES -> R.string.new_slides
    })

    override suspend fun rows(arg: String): List<Result> {
        if (arg.isBlank()) return NewKind.entries.map { k ->
            Result(
                id = "jot:new:${k.name}", provider = key, kind = Kind.OTHER, title = kind(k), score = 1.0, learnable = false,
                icon = Icon.Symbol(when (k) { NewKind.FOLDER -> "folder"; NewKind.SHEET -> "sheet"; NewKind.SLIDES -> "slides"; else -> "file" }),
                // Choosing a kind writes its word into the field, ready for the name.
                actions = listOf(Action("choose", text(R.string.scope_type), Effect.EnterScope(key, kind(k).lowercase() + " "), keepOpen = true, symbol = "edit")),
            )
        }
        val spec = Jot.new(arg)
        val online = spec.kind == NewKind.DOC || spec.kind == NewKind.SHEET || spec.kind == NewKind.SLIDES
        val where = if (online) text(R.string.new_in_drive) else text(R.string.new_in_documents)
        val actions = when {
            online -> listOf(Action("create", text(R.string.action_create), Effect.OpenUrl(google(spec.kind, spec.name)), symbol = "plus"))
            spec.name.isEmpty() -> emptyList()
            spec.kind == NewKind.FOLDER -> listOf(Action("create", text(R.string.action_create), Effect.NewFile(spec.name, folder = true), symbol = "plus", done = text(R.string.done_created)))
            else -> listOf(
                Action("create", text(R.string.action_create), Effect.NewFile(spec.name, folder = false), symbol = "plus", done = text(R.string.done_created)),
                Action("pick", text(R.string.action_create_in), Effect.NewFile(spec.name, folder = false, pick = true), symbol = "folder"),
            )
        }
        return listOf(preview(
            text(if (online) R.string.new_caption_online else if (spec.kind == NewKind.FOLDER) R.string.new_caption_folder else R.string.new_caption),
            listOf(slot(R.string.slot_kind, kind(spec.kind), guessed = !spec.kindGiven), slot(R.string.slot_name, spec.name), slot(R.string.slot_in, where, guessed = true)),
            null, actions,
        ))
    }

    /** Google's own "make a new one" addresses; a name becomes the title. */
    private fun google(k: NewKind, name: String): String {
        val what = when (k) { NewKind.SHEET -> "spreadsheets"; NewKind.SLIDES -> "presentation"; else -> "document" }
        return "https://docs.google.com/$what/create" + if (name.isBlank()) "" else "?title=" + android.net.Uri.encode(name)
    }
}

/** "Ask Gemini": hands the text to the Gemini app, which shows it in its prompt. The user sends it there. */
fun gemini(context: Context, text: String, score: Double): Result = Result(
    id = "web:gemini", provider = "gemini", kind = Kind.WEB, title = context.getString(R.string.gemini_title), subtitle = text,
    icon = Icon.Symbol("spark"), score = score, learnable = false,
    actions = listOf(
        Action("ask", context.getString(R.string.action_ask), Effect.AskGemini(text), symbol = "send"),
        Action("copy", context.getString(R.string.action_copy), Effect.CopyText(text)),
    ),
)

/** `ask how do I…`: Gemini, whatever the text is. */
class AskScope(context: Context) : JotScope(context, "ask", R.string.ask_keys, R.string.ask_name, R.string.ask_hint, R.string.ask_about, "spark") {
    override suspend fun rows(arg: String): List<Result> = if (arg.isBlank()) emptyList() else listOf(gemini(context, arg.trim(), 1.0))
}
