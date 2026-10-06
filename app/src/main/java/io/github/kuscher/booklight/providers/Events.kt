package io.github.kuscher.booklight.providers

import android.content.Context
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Behind
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Cals
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.EventReading
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Sentence
import io.github.kuscher.booklight.ai.OnDevice
import io.github.kuscher.booklight.core.Slot
import io.github.kuscher.booklight.core.SlotState
import io.github.kuscher.booklight.core.Splits
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.device.CalendarList
import io.github.kuscher.booklight.scopes.insert
import io.github.kuscher.booklight.scopes.span
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * An event from a sentence: the event's row, the same one under the keyword `event` and for a line typed
 * the way it is said, without a keyword ("Add dinner with Sam tomorrow at 7pm to the Team calendar").
 *
 * The row shows what was understood, read by core's rules at every letter ([Sentence]): when and what
 * on its first line of slots, where and on which calendar on its second. Both lines stand from the first
 * letter, an empty slot as its label and a rule, so nothing in the row ever moves when a place or a
 * calendar is read.
 *
 * Enter opens the Calendar app's editor, filled in, on the calendar that was named (**Create**); the
 * user saves it there. An event that names no calendar goes to the editor by the request Booklight has
 * always sent; the calendar link is for one that names a calendar (or is shown one, where it would be
 * saved). With "Save events without opening Calendar" switched on in the Booklight window, Enter is
 * **Save**: the event is written into the calendar that was named, or the one chosen there, and **Open**
 * (in Calendar: the caption says where) is the second action. A guess is never saved (core `Cals.saves`):
 * where the day or the time was not read from the sentence, a calendar's name is still being typed or is
 * none of the user's, or the line was typed without the keyword and begins like an everyday search
 * ("book flight to boston friday 9am"), Enter opens the editor whatever the switch says, and the footer
 * says why. The caption says what Enter does on the action that is armed: "Enter saves it" only while
 * Save is. With the list of calendars allowed, a stop **Calendar** opens those that can be named as a
 * list under the row; Enter on one writes its name into the sentence.
 *
 * Without the keyword the row is offered only for a line that begins with a cue and holds a day or a time
 * (both, where the cue is as often the first word of a search: core `Sentence.reads`), and the engine puts
 * it below everything of the device that matches and above the web (its id begins with
 * `SearchEngine.SENTENCE`). Under the keyword the line is the event, whatever its first word is.
 *
 * **The device's model helps to split, and nothing waits for it.** The row is the rules' reading in the
 * frame of the key. Only where the rules leave the title in pieces (core `Splits.asks`), and only once
 * the typing has rested for [REST_MS], the model is asked which words are the title, the place and the
 * calendar: once for a text, never for a date. What it says is taken only where every check of core
 * `Splits.merge` passes (every part a piece of what was typed, the day and time the rules' own, no word
 * lost); then the row is given again with the same lines, and the words in its slots change where they
 * stand. No model here, no answer, a slow one: the row stays as the rules read it. Whether a line typed
 * without the keyword is an event at all is the rules' to say, never the model's. What was asked and
 * answered is kept in memory until the panel closes ([forget]) and nowhere else.
 */
class Events(private val context: Context, private val prefs: Prefs, private val calendars: CalendarList, private val ai: OnDevice) : Provider {
    override val id = ID

    /**
     * What the model said in this opening, by the text it was asked about (trimmed: a space typed after it is no other
     * sentence): no text is asked twice. Only whole answers; the oldest goes where there are too many.
     */
    private val said: MutableMap<String, String> = java.util.Collections.synchronizedMap(object : LinkedHashMap<String, String>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > KEPT
    })
    @Volatile private var warmed = false

    override suspend fun query(q: Query): List<Result> {
        // (A line that does not begin as an event's is not read at all: the everyday list costs nothing more than this.)
        if (!Sentence.cued(q.text)) return emptyList()
        val now = LocalDateTime.now()
        val read = read(q.text, now, keyword = false)
        // A line that is no event has no row. That is the rules' to say: the model's answer can change what the row's
        // slots hold, and never whether the row is there.
        return if (Sentence.reads(read.own)) listOf(row(SENTENCE, q.text, read.taken, now, asks(read, q.text))) else emptyList()
    }

    /** The row under the keyword, for whatever is typed there. */
    fun row(text: String): Result = LocalDateTime.now().let { now -> read(text, now, keyword = true).let { row(SCOPE, text, it.taken, now, asks(it, text)) } }

    /** A row with this id stands under the keyword `event`: its line is read as the keyword's (core `Sentence.read`). */
    private fun keyed(id: String) = id == SCOPE

    /** What is read of a text: by the rules ([own]), and with the model's answer for this very text where one is kept and passes ([taken]; else the rules' own again). [asked]: the model has answered for it. */
    private class Read(val own: EventReading, val taken: EventReading, val asked: Boolean)

    private fun read(text: String, now: LocalDateTime, keyword: Boolean): Read {
        val own = Sentence.read(text, now, calendars.known, keyword)
        val answer = said[text.trim()] ?: return Read(own, own, false)
        return Read(own, Splits.merge(own, answer, text, now, calendars.known, keyword), true)
    }

    /**
     * Whether the model may be asked about [text]: not asked yet, the rules left its title in pieces (core `Splits.asks`),
     * and a model may be there (one the system has, or one it has not been asked about yet).
     */
    private fun asks(read: Read, text: String): Boolean =
        !read.asked && Splits.asks(read.own, text) && ai.state.value.let { it == OnDevice.State.READY || it == OnDevice.State.UNKNOWN }

    /**
     * What the rules read of [text], here and now, under the [keyword] or typed without it: for the debug hooks, and for
     * the row of a date that was copied, which leads into the keyword's row. Nothing is noted, nothing asked.
     */
    fun rules(text: String, keyword: Boolean = false): EventReading = Sentence.read(text, LocalDateTime.now(), calendars.known, keyword)

    /** [r] is an event's row whose sentence the device's model may split better: the row says so itself (`Body.Slots.ask`). */
    fun asks(r: Result): Boolean = r.provider == ID && (r.body as? Body.Slots)?.ask != null

    /**
     * Asks the device's model to split the sentence [row] was made from, after the typing has rested ([pause]), and
     * gives the row again with what it said. Null where nothing changes: no model, no answer, or an answer that did not
     * pass every check. Whoever calls this cancels it when a letter is typed: the model is never asked while the user
     * types. Only a whole answer is kept: one that was cut off (by a key, or by the time it may take) or that is empty is
     * not "answered", and the text may be asked about again when its list is next made.
     */
    suspend fun answer(row: Result, pause: Boolean): Result? {
        val text = (row.body as? Body.Slots)?.ask ?: return null
        // (Loaded while the typing rests, where the system is known to have it: the first answer of an opening comes sooner.)
        if (ai.ready && !warmed) { warmed = true; ai.warm() }
        if (pause) delay(REST_MS)
        val state = ai.state.value.let { if (it == OnDevice.State.UNKNOWN) ai.check() else it }
        if (state != OnDevice.State.READY) return null
        val answer = asked(text) ?: return null
        said[text.trim()] = answer
        val now = LocalDateTime.now()
        val own = Sentence.read(text, now, calendars.known, keyed(row.id))
        val merged = Splits.merge(own, answer, text, now, calendars.known, keyed(row.id))
        // (The same row, with nothing more to ask: its words change where they stand, or stay.)
        return if (merged === own) null else row(row.id, text, merged, now, asks = false)
    }

    /** The model's whole answer to the question for [text]; null where it gave none, or was still giving it when its time was up. */
    private suspend fun asked(text: String): String? {
        val answer = StringBuilder()
        val whole = withTimeoutOrNull(ANSWER_MS) { ai.ask(Splits.prompt(text)).collect { answer.append(it) }; true } == true
        return answer.toString().takeIf { whole && it.isNotBlank() }
    }

    /**
     * The model asked about [text] as the row asks it, at once and whatever the rules made of it: what it said, the rules'
     * reading, and the reading with its answer (the same object as the rules' where the answer did not pass). For the debug
     * hook, with a panel open; nothing of it is kept.
     */
    suspend fun split(text: String, keyword: Boolean = false): Triple<String, EventReading, EventReading> {
        val answer = if (ai.check() == OnDevice.State.READY) asked(text).orEmpty() else ""
        val now = LocalDateTime.now()
        val own = Sentence.read(text, now, calendars.known, keyword)
        return Triple(answer, own, Splits.merge(own, answer, text, now, calendars.known, keyword))
    }

    /**
     * [text] with the calendar's own name where the sentence has the start of one at its very end ("… to te" becomes
     * "… to Team"): what Right writes into the field when it takes the grey rest. Null where nothing is being completed.
     * [keyword]: the text stands under the keyword `event`.
     */
    fun completed(text: String, keyword: Boolean): String? {
        val now = LocalDateTime.now()
        val all = calendars.known
        val r = Sentence.read(text, now, all, keyword)
        return r.calendar?.takeIf { r.rest != null }?.let { Sentence.put(text, it.name, context.getString(R.string.event_into), now, all, keyword) }
    }

    /** The panel closed: nothing of what was typed or answered is kept. */
    fun forget() { said.clear(); warmed = false }

    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)

    /**
     * The row [id] for [text] as [r] reads it. [asks]: the device's model may be asked to split this text better. [keyword]:
     * the text stands under the keyword `event` (a calendar chosen from the list is written into it as that line is read).
     */
    fun row(id: String, text: String, r: EventReading, now: LocalDateTime, asks: Boolean = false, keyword: Boolean = keyed(id)): Result {
        val s = prefs.now
        val all = calendars.known
        val e = r.draft
        val zone = ZoneId.systemDefault()
        // Saved from here only with the switch on and the system's leave for it. (While a list is pretended, in a debug build,
        // the row is as with that leave, to be looked at: the executor writes nothing then.)
        val on = s.saveEvents && (calendars.pretends || (calendars.allowed && calendars.writes))
        val target = Cals.target(r.calendar, all, s.eventCalendar)
        val saves = Cals.saves(r, on, target)
        // The calendar the row shows: the one that was named; else, where Enter saves, the one it is saved to. The editor opens on that one too.
        val shown = r.calendar ?: target.takeIf { saves }
        // With a calendar to name, the calendar link, which alone can say which calendar. With none (none named, none shown as
        // the one it would be saved to), the request Booklight has always sent: someone who allows nothing gets what 3.1 did.
        val open = insert(e, shown?.let { Cals.link(e, it, zone) }.orEmpty())
        // The calendars an event can be added to, as the lines of the row's own list: each writes its name into the sentence.
        // Only those whose name reads back as that calendar (core `Cals.marked`): the second of two with one name, or a name of
        // signs alone, would write a sentence that means another calendar, or none.
        // (The first twelve: a list under a row is never longer than the list of an app's window places. Any other is named by typing.)
        val word = text(R.string.event_into)
        // (The sentence is read once for all of them.)
        val put = Sentence.putter(text, word, now, all, keyword)
        val lines = if (text.isBlank()) emptyList() else all.filter { it.writable && it.nameable }.take(LINES).map { c ->
            Action("cal:${c.id}", c.name, Effect.Retype(put(c.name)), keepOpen = true, symbol = Icon.Swatch(c.color).symbol, more = true, behind = Behind.CALENDAR)
        }
        // The sentence names a calendar and the list is not allowed: the row points at the step, until the system's question has been answered once.
        val points = r.named && !calendars.allowed && !calendars.pretends && !s.calendarsAsked
        val actions = if (text.isBlank()) emptyList() else listOfNotNull(
            // What is written is core's to say (`Cals.write`): exactly what the row shows.
            target?.takeIf { saves }?.let { c ->
                val w = Cals.write(e, c, zone)
                Action("save", text(R.string.action_save_event), Effect.SaveEvent(w.calendar, w.title, w.start, w.end, w.allDay, w.zone, w.place), symbol = "check", done = text(R.string.done_saved_event, c.name))
            },
            // With the switch on this is "Open", whether it is the second action or, for a guess, the first. The short word every
            // row has for it: the longer one took the title's room, and the caption says where it opens.
            if (on) Action("open", text(R.string.action_open), open, symbol = "open") else Action("create", text(R.string.action_create), open, symbol = "plus"),
            Action("copy", text(R.string.action_copy), Effect.CopyText(listOf(e.title, span(context, e), e.place).filter { it.isNotEmpty() }.joinToString(", "))),
            Action("calendar", text(R.string.slot_calendar), Effect.OpenList(Behind.CALENDAR), keepOpen = true, symbol = "list").takeIf { lines.isNotEmpty() },
            Action("allow", text(R.string.action_allow), Effect.Grant("calendars"), symbol = "lock").takeIf { points },
        ) + lines
        fun slot(label: Int, value: String, guessed: Boolean = false, dot: Int? = null, whole: Boolean = false, wide: Boolean = false) =
            Slot(text(label), value, if (value.isEmpty()) SlotState.EMPTY else if (guessed) SlotState.GUESSED else SlotState.TYPED, dot, whole, wide)
        // A line typed without the keyword that begins like an everyday search ("book …", "schedule …"): with the switch on its
        // Enter still opens the editor, and the caption and the footer say why, with the word that was typed.
        val everyday = on && r.everyday
        val footer = when {
            text.isBlank() -> null
            points -> text(R.string.event_footer_allow)
            e.repeats -> text(R.string.event_footer_repeats)
            everyday -> text(R.string.event_footer_everyday, text.trim().substringBefore(' '))
            on && !saves && !e.sure -> text(R.string.event_footer_guess)
            on && !saves && target == null && r.calendar != null -> text(R.string.event_footer_closed, r.calendar!!.name)
            // (The calendar is read by the first letters of its name: until the name is whole, Enter does not save there.)
            on && !saves && r.rest != null -> text(R.string.event_footer_rest)
            // (It names a calendar that is none of the user's: not saved into the one new events go to, which it did not ask for.)
            on && !saves && r.named -> text(R.string.event_footer_named)
            else -> null
        }
        return Result(
            id = id, provider = ID, kind = Kind.OTHER, title = text(R.string.event_name), icon = Icon.Symbol("event"), score = if (id == SCOPE) 1.0 else SCORE, learnable = false,
            body = Body.Slots(
                // What Enter does, said of the action that is armed: "Enter saves it" only while that is Save.
                text(if (everyday) R.string.event_caption_everyday else R.string.event_caption),
                // ("When" is never cut: what is saved stands on the glass whole, with the place beside it. The title has the second
                // line, and all of it but the calendar's share: beside "When" and the strip it had room for two words.)
                listOf(slot(R.string.slot_when, span(context, e), guessed = !e.sure, whole = true), slot(R.string.slot_where, e.place)),
                // (A calendar that was not typed, the one a saved event goes to, is a step lighter. So is one read by the first letters of its name.)
                more = listOfNotNull(slot(R.string.slot_title, e.title, wide = true), slot(R.string.slot_calendar, shown?.name.orEmpty(), guessed = r.calendar == null || r.rest != null, dot = shown?.color).takeIf { all.isNotEmpty() }),
                completes = r.rest, footer = footer, ask = text.takeIf { asks },
                // (Nor does it say "Opens in your calendar" while Copy or the Calendar stop is the armed one.)
                captions = if (text.isBlank()) emptyMap() else listOfNotNull(("save" to text(R.string.event_caption_save)).takeIf { saves }, "copy" to text(R.string.event_caption_copy),
                    ("calendar" to text(R.string.event_caption_calendar)).takeIf { lines.isNotEmpty() }).toMap(),
            ),
            actions = actions,
        )
    }

    companion object {
        const val ID = "events"
        /** The row under the keyword `event`. */
        const val SCOPE = "jot:event"
        /** The row of a line typed without the keyword: the engine places it by this id, not by its score. */
        const val SENTENCE = SearchEngine.SENTENCE + "event"
        private const val SCORE = 0.5
        /** How long the typing rests before the device's model is asked about a sentence. */
        const val REST_MS = 700L
        /** How long its answer may take: the first one of a process loads the model (about six seconds on a Googlebook). A later one is no use. */
        private const val ANSWER_MS = 20_000L
        /** How many answers are kept for one opening. */
        private const val KEPT = 32
        /** The most calendars the row's list shows. */
        private const val LINES = 12
    }
}
