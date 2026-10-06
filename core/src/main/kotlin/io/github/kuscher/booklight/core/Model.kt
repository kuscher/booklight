package io.github.kuscher.booklight.core

/**
 * What the user typed, as typed; [text] is what providers match against. Inside a scope (the chip
 * in the field: a keyword search, a note, a timer) [scope] is its key and the text is its argument.
 */
data class Query(
    val raw: String,
    val scope: String? = null,
    /** The word that was typed to enter [scope]: a scope may have several keywords. Null = entered from its row. */
    val keyword: String? = null,
    /** Under an app's chip: which of the app's two actions the text is for. Null = the first the app has. */
    val act: Act? = null,
) {
    val text: String = raw.trim()
    val isEmpty: Boolean get() = text.isEmpty()
}

/** What a result is. Drives the row's label and how the ranker weighs it. */
enum class Kind {
    APP, ANSWER, SETTING, COMMAND, WEB, SUGGESTION, SCOPE, CONTROL, OTHER,
    /** One of an opened row's other actions, shown as a row of its own under it. */
    ACTION,
}

/**
 * A picture for a row, as data: the core has no drawables. The app turns it into pixels
 * (`ui/Icons.kt`), and a plugin in another process could send the same thing.
 */
sealed interface Icon {
    /** The launcher icon of an activity. [user] is the profile's serial number (0 = the owner). */
    data class App(val packageName: String, val className: String, val user: Long = 0) : Icon {
        /** This icon as an action's symbol: an action that hands something to an app is marked by the app's own icon. */
        val symbol: String get() = "$SYMBOL$packageName/$className#$user"

        companion object {
            private const val SYMBOL = "app:"

            /** The app's icon a symbol names; null for one of Booklight's own symbols. */
            fun of(symbol: String): App? {
                if (!symbol.startsWith(SYMBOL)) return null
                val slash = symbol.indexOf('/')
                val hash = symbol.lastIndexOf('#')
                if (slash <= SYMBOL.length || hash <= slash + 1) return null
                return App(symbol.substring(SYMBOL.length, slash), symbol.substring(slash + 1, hash), symbol.substring(hash + 1).toLongOrNull() ?: return null)
            }
        }
    }
    /** One of Booklight's own symbols, by name (`ui/Icons.kt`). */
    data class Symbol(val name: String) : Icon
    /** A patch of one colour (a colour value typed into the panel). */
    data class Swatch(val argb: Int) : Icon {
        /** This colour as an action's mark: a line of a row's list that is told from the others by its colour (a calendar's own) is marked by a dot of it. */
        val symbol: String get() = SYMBOL + Integer.toHexString(argb)

        companion object {
            private const val SYMBOL = "dot:"

            /** The colour a symbol names; null for any other symbol. */
            fun of(symbol: String): Swatch? = if (!symbol.startsWith(SYMBOL)) null else symbol.substring(SYMBOL.length).toLongOrNull(16)?.takeIf { it in 0..0xFFFFFFFFL }?.let { Swatch(it.toInt()) }
        }
    }
    /** A character shown as the picture: an emoji, a snippet's first letter. */
    data class Glyph(val text: String) : Icon
}

/**
 * Where on the screen an app's window is asked to open. Recipes save a place by its number:
 * new ones go at the end.
 */
enum class Place {
    NONE, LEFT, RIGHT,
    LEFT_THIRD, MIDDLE_THIRD, RIGHT_THIRD, LEFT_TWO_THIRDS, RIGHT_TWO_THIRDS,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER, FULL,
}

/**
 * A media key. [PLAY] resumes whatever played last and [PAUSE] can never start anything; [PLAY_PAUSE] is the
 * one key that does either. Recipes save a key by its name.
 */
enum class MediaKey { PLAY_PAUSE, NEXT, PREVIOUS, PLAY, PAUSE, STOP }

/** What to do with a picture Booklight made (a QR code). */
enum class ImageUse { COPY, SAVE, SHARE }

/**
 * What an app's chip can be armed with: the two actions of an app's row that take words, in the
 * row's order. [id] is the id of that action wherever it stands.
 */
enum class Act(val id: String) {
    SEARCH("search"), PLAY("play");

    companion object {
        /** The action with this id; null for any other action. */
        fun of(id: String?): Act? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Which stop of its row an action waits behind, where it is not an icon on the row ([Action.more]):
 * the arrow, or a stop of the row's own: Window (where an app's window goes), Calendar (which of the
 * user's calendars an event goes to). A row has the arrow's list and at most one other: two lists at
 * most, and never a list inside a list.
 */
enum class Behind { ARROW, WINDOW, CALENDAR }

/** One of an app's own pages in the system's Settings. */
enum class AppPage { NOTIFICATIONS, LANGUAGE, DEFAULTS, BATTERY }

/**
 * What running an action does, as data. The core never touches Android: the app's `Executor`
 * performs effects. New abilities are a new effect plus a few lines there.
 */
sealed interface Effect {
    /** [display]: which screen, counted from 1; 0 = wherever the system puts it. */
    data class LaunchApp(val packageName: String, val className: String, val user: Long = 0, val place: Place = Place.NONE, val newWindow: Boolean = false, val display: Int = 0) : Effect
    data class AppInfo(val packageName: String, val className: String, val user: Long = 0) : Effect
    /**
     * One of an app's own pages in the system's Settings: its notifications, its language, what it
     * opens by default, its battery use. The page is opened; the switches on it are the user's.
     */
    data class AppSettings(val page: AppPage, val packageName: String) : Effect
    /**
     * Something another app offers (one of its own shortcuts, or a command from its Booklight file), or
     * what an app command of the user's own asks an app for: [intent] as an `intent:` address. The
     * executor starts it only if it leads to an activity of [owner] that is open to other apps and asks
     * for no permission.
     */
    data class Open(val owner: String, val intent: String) : Effect
    /** Asks the system to remove an app; the system shows its own confirmation. */
    data class Uninstall(val packageName: String, val user: Long = 0) : Effect
    data class OpenUrl(val url: String) : Effect
    /** [sensitive]: a password; the clipboard is told not to show it. */
    data class CopyText(val text: String, val sensitive: Boolean = false) : Effect
    /** A system settings screen, by its `android.settings.…` intent action. */
    data class OpenSettings(val action: String) : Effect
    /** One of Booklight's own pages or commands (`window`, `shortcuts`…). */
    data class Internal(val command: String) : Effect

    /** The mail app's compose window, filled in. Nothing is sent. */
    data class Compose(val to: List<String>, val subject: String, val body: String) : Effect
    /**
     * The phone app's dial screen with [number] in it, or, with [message], the messages app's new
     * message to [number] with [text] in its field. [number] is digits, with a + in front when it
     * was typed so. Nothing is dialled and nothing is sent: the last click is the user's.
     */
    data class Phone(val number: String, val message: Boolean = false, val text: String = "") : Effect
    /** A line for the user's notes: [file] empty is Notes.md, [TODAY] the day's own file, else that file in the notes folder. */
    data class AppendNote(val text: String, val file: String = "") : Effect {
        companion object { const val TODAY = "@today" }
    }
    /** A task for Todo.md. */
    data class AddTodo(val text: String) : Effect
    /** Ticks the task on line [line] of Todo.md, or unticks it. [text] is what the line said, so a file changed meanwhile is not ticked in the wrong place. */
    data class TickTodo(val line: Int, val text: String, val done: Boolean) : Effect
    /** Opens a file of the notes folder in whatever edits text. */
    data class OpenNote(val file: String) : Effect
    /** The notes app's editor with this text. */
    data class KeepNote(val text: String) : Effect
    /**
     * The calendar's editor, filled in. Times are epoch milliseconds. [link]: a calendar link for the
     * same event ([Cals.link]), which names the calendar too: it is sent to the Calendar app first, and
     * the rest is what is sent where no Calendar app takes it. Nothing is saved: the user saves it there.
     */
    data class InsertEvent(val title: String, val startMillis: Long, val endMillis: Long, val allDay: Boolean, val place: String, val link: String = "") : Effect
    /**
     * The event itself, written into the calendar with the id [calendar] on this device, with that
     * calendar's own default reminder. Times are epoch milliseconds; [zone] is the device's time zone
     * ("UTC" for an all-day event). Only ever what the row shows, on the user's own Enter, with the
     * option switched on and the permission given: the executor checks the last two again.
     */
    data class SaveEvent(val calendar: Long, val title: String, val startMillis: Long, val endMillis: Long, val allDay: Boolean, val zone: String, val place: String) : Effect
    data class SetTimer(val seconds: Int, val label: String) : Effect
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : Effect
    /** The Clock's list of alarms, or its list of [timers]. */
    data class ClockList(val timers: Boolean) : Effect
    /** A new file or folder in Documents. [pick]: ask where instead. */
    data class NewFile(val name: String, val folder: Boolean, val pick: Boolean = false) : Effect
    /** Hands text to the Gemini app, which shows it in its prompt; the user sends it there. */
    data class AskGemini(val text: String) : Effect
    /**
     * A small window that stays above the others. [kind]: `text`, `answer`, `color`, `qr`, `timer`.
     * [text] is what it shows (a colour: its hex), [note] a line with it (the sum; a timer's label),
     * [value] a number with it (a colour's ARGB; a timer's seconds).
     */
    data class Pin(val kind: String, val text: String, val value: Long = 0, val note: String = "") : Effect
    /** Takes away the pinned window. */
    data object Unpin : Effect
    /** Hands the text back to the app that handed text over from a field that can be edited. */
    data class Replace(val text: String) : Effect

    data class SetVolume(val percent: Int) : Effect
    data object ToggleMute : Effect
    data class Media(val key: MediaKey) : Effect
    /**
     * "Play this", asked of the music app [packageName], or of whichever answers when it is null.
     * [mode] says what kind of thing is named, and the fields after it are that mode's names (empty:
     * not given); [query] is always sent too, for a player that does not know the modes. The executor
     * starts only an activity of that app that is open to other apps and asks for no permission.
     * What the player does with it (play, or show its search results) is the player's choice.
     * [link]: an address of the player's own that plays exactly this, where a lookup found one
     * (`spotify:track:…`); it is sent to [packageName] in place of the request.
     */
    data class PlayMusic(
        val query: String, val packageName: String? = null, val mode: PlayMode = PlayMode.ANY,
        val title: String = "", val artist: String = "", val album: String = "", val playlist: String = "", val genre: String = "",
        val link: String = "",
    ) : Effect
    data class SetBrightness(val percent: Int) : Effect

    /** The QR code for [text]: copied, saved to Downloads, or shared. */
    data class QrImage(val text: String, val use: ImageUse) : Effect

    /**
     * Asks the device's own model [prompt] and writes the answer into the row that was asked, under
     * the caption [name] ("In English"; empty for none). The panel does this itself; the model's
     * words are only shown, and what is done with them is another action.
     */
    data class Ask(val prompt: String, val name: String) : Effect
    /** "Don't suggest": the thing with this id is no longer one of the rows under the empty field. The panel does it itself. */
    data class Unsuggest(val id: String) : Effect
    /**
     * Turns a scope's row into the chip in the field; [text] becomes its argument. [act]: for an
     * app's chip, which of the app's actions is armed there (Search or Play on the app's row).
     */
    data class EnterScope(val key: String, val text: String = "", val act: Act? = null) : Effect
    /**
     * A stop of a row that opens the lines kept behind it as a list under the row (Window, on an
     * app's row). The panel does this itself; the arrow at a row's end needs no action of its own.
     */
    data class OpenList(val behind: Behind) : Effect
    /** Booklight types [text] into the field, a letter at a time (an example from a tip or from the list of everything). The panel does this itself. */
    data class Type(val text: String) : Effect
    /**
     * What stands in the field becomes [text], at once: a line of a row's list that changes what the
     * row reads (a calendar chosen for an event is written into its sentence). The panel does this
     * itself; nothing runs, and the Enter that chose it does not also run the row.
     */
    data class Retype(val text: String) : Effect
    /** Opens the place where the user allows something, once: `brightness`, `notes`, `calendars` (the Booklight window, on that row). */
    data class Grant(val what: String) : Effect

    data class SaveSnippet(val key: String, val text: String) : Effect
    /** Removes something the user made: `snippet`, `quicklink`, `recipe`, `appcommand`. */
    data class Delete(val kind: String, val id: String) : Effect
    /** Opens the editor for something the user made, or for a new one ([id] empty). */
    data class Edit(val kind: String, val id: String) : Effect
    /** A recipe: these, in order. */
    data class Steps(val steps: List<Effect>) : Effect
}

/**
 * One thing you can do with a result. A row shows all of its actions as icons; one is armed
 * (the first, unless a typed verb armed another) and Enter runs it.
 */
data class Action(
    val id: String,
    val label: String,
    val effect: Effect,
    /** Leave the panel open afterwards. */
    val keepOpen: Boolean = false,
    /** The action's icon, by name (`ui/Icons.kt`), or an app's own icon ([Icon.App.symbol]). */
    val symbol: String = id,
    /** Removes or uninstalls: drawn last and in the error colour, never armed unless asked for by name. */
    val danger: Boolean = false,
    /** Needs Enter twice (deleting something the user made). */
    val confirm: Boolean = false,
    /** What the footer says once it is done ("Added to Notes"); null = nothing to say. */
    val done: String? = null,
    /** Kept behind the row's arrow: a row shows nine actions as icons and opens the rest as a list under it. */
    val more: Boolean = false,
    /** With [more]: which of the row's two lists it is a line of. */
    val behind: Behind = Behind.ARROW,
    /** It cannot be run now (it needs an answer that did not come): it keeps its place, dimmed, and the arming passes over it. */
    val off: Boolean = false,
)

/** What a row shows besides, or instead of, its title. */
sealed interface Body {
    /**
     * A preview of what was understood: labelled slots that fill as the argument is typed, and a line for the long part (a message).
     * [more]: a second line of slots, in the note's place (an event's place and calendar): the row is as high with it as without.
     * [completes]: what is left of a name the row read by its start ("tea" for the Team calendar): the field shows it in grey
     * after the text, and Right takes it. [footer]: what the footer says of the row while it is selected. [ask]: the typed
     * line this preview was made from, where the device's own model may be asked to split it better (an event's sentence
     * whose title the rules left in pieces); null where there is nothing to ask. The row carries it, so that an answer is
     * only ever for the text the row itself was made from. [captions]: what the caption says instead while one of the
     * row's actions is armed, by that action's id: a caption that says what Enter does says it of the action Enter would
     * run ("Enter saves it" only while Save is armed).
     */
    data class Slots(val caption: String?, val slots: List<Slot>, val note: String? = null, val more: List<Slot> = emptyList(),
        val completes: String? = null, val footer: String? = null, val ask: String? = null, val captions: Map<String, String> = emptyMap()) : Body {
        /** The caption while the action [armed] (its id; null: none) is the armed one. */
        fun caption(armed: String?): String? = captions[armed] ?: caption
    }
    /** A level from 0 to 100 (volume, brightness). [target]: a typed value, not yet set. [locked]: needs a grant first. */
    data class Level(val percent: Int, val target: Int? = null, val muted: Boolean = false, val locked: Boolean = false) : Body
    /** A grid of characters to pick from; the arrows move between cells. */
    data class Grid(val cells: List<Cell>, val columns: Int = 14) : Body
    /** A QR code of [text]. */
    data class Code(val text: String) : Body
    /** Text shown large in the fixed-width face (a password). */
    data class Mono(val text: String) : Body
    /** A key combination, drawn as key caps: Action, Ctrl, ]. */
    data class Keys(val keys: List<String>) : Body
    /** A task: a box, ticked or not. */
    data class Task(val done: Boolean) : Body
    /** A switch at the row's end, [on] or off, with its state as a [word] before it ("On", "Off"). The row has no strip: its one action flips the switch. */
    data class Switch(val on: Boolean, val word: String) : Body
    /**
     * A text under a caption, on up to four lines: what a prompt will be asked about, and then the
     * answer of the device's own model as it arrives. [answer]: [text] is the model's; before that
     * it is the user's own text (the caption says which). [busy] while more is coming.
     * [ask]: what the device's own model would be asked for this row (the prompt with the text in
     * it); null when there is nothing to ask or nobody to ask. [tall]: the answer needed a third
     * line, and the row has grown, once, to hold four.
     */
    data class Stream(val text: String, val busy: Boolean, val caption: String, val answer: Boolean = false, val ask: String? = null, val tall: Boolean = false) : Body
    /**
     * A flight: who flies it and where ([caption]), a [headline] with one [badge] beside it, and under
     * them the flight as a line from take-off to landing, the plane on it, and under its two ends the
     * airport and its time. All of it stands from the row's first frame, at one height: before the
     * answer the ends are empty ([from] and [to] null) and the line has no plane.
     *
     * [share]: how much of the flying time has passed, 0 to 1, which is where the plane stands; null:
     * nobody knows where it is, and there is no plane. [flight] names the flight the answer is about
     * and [answer] the answer it was read from (0: none has come): the plane of one flight only goes
     * forward, another answer about it moves the plane once, and another flight's plane comes in
     * anew. [counts]: the headline's minutes follow the clock, and the row is said again each minute
     * while it is on screen. [source]: who gave the answer and when, which the footer says while the
     * row is selected. [phase]: which phase the words are for; null while there is no flight to say
     * it of.
     */
    data class Flight(
        val caption: String?, val headline: String, val badge: String? = null, val tone: Tone = Tone.PLAIN, val share: Float? = null,
        val from: Stop? = null, val to: Stop? = null, val flight: String? = null, val answer: Long = 0,
        val counts: Boolean = false, val source: String? = null, val phase: FlightPhase? = null,
    ) : Body
}

/** What a flight's badge says besides its word: good news (on time, early), a delay, or neither (only the plan is known). The one place a row has a colour of its own. */
enum class Tone { PLAIN, GOOD, LATE }

/**
 * What stands under one end of a flight's line: the airport's letters, its time there (with the day
 * where it is not today's; [struck]: it will not happen), and small words after it. [words]: all of
 * them ("Gate Z58 · Terminal 1"); [brief]: what is left of them where the two ends would meet in
 * the middle ("Gate Z58"), null for nothing.
 */
data class Stop(val code: String, val time: String, val struck: Boolean = false, val words: String? = null, val brief: String? = null)

enum class SlotState { TYPED, GUESSED, EMPTY }
/**
 * [dot]: a colour that belongs to the value (a calendar's own), drawn as a dot before it; null for none. [whole]: the
 * value is never cut and never ends in an ellipsis: it takes the room it needs, and the slots after it in its line
 * give way (an event's "When": what is saved stands on the glass whole). [wide]: the value takes what its line has
 * left once the slot after it has its room, and that one keeps to a share (an event's title, which may be long, beside
 * its calendar).
 */
data class Slot(val label: String, val value: String, val state: SlotState, val dot: Int? = null, val whole: Boolean = false, val wide: Boolean = false)
data class Cell(val glyph: String, val name: String)

/** What Left and Right do on a control row, at once and without closing. */
data class Nudge(val down: Effect, val up: Effect)

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
    /** Which action Enter runs: 0, unless a typed verb ("chrome uninstall") armed another. */
    val armed: Int = 0,
    val body: Body? = null,
    val nudge: Nudge? = null,
    /** What the row says at its right end in place of its kind's name: the app a command belongs to, "Key". */
    val label: String? = null,
    /** The title for where the whole one does not fit beside the row's actions: "Search for “dune”", when the app's name is a long one. */
    val brief: String? = null,
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
    /**
     * The rows for these ids, as typing would find them, for the three under the empty field: from
     * memory, without a search and without reading anything new. An id that is not this provider's,
     * or whose thing is gone or switched off, has no row.
     */
    fun byIds(ids: Set<String>): List<Result> = emptyList()
}

/**
 * A row that takes text: a keyword search, a note, a timer. Typing one of its [keywords] and a
 * space, or Tab on its row, makes it the chip in the field; what is typed after that is its
 * argument, and [rows] says what the list shows for it, best first, in the scope's own order.
 */
interface Scope {
    val key: String
    val keywords: List<String>
    /** On the chip: "YouTube", "Mail". */
    val name: String
    /** On its row in the ordinary list, where an app may have the same name: "Search YouTube". */
    val title: String get() = name
    val symbol: String
    /** The field's placeholder inside the scope: what to type. */
    val hint: String
    /** One line under the row's name outside the scope; null for none. */
    val about: String? get() = null
    /** False when something else already puts a row for it in the ordinary list (a level is its own row): then only its keyword and a space enter it. */
    val listed: Boolean get() = true
    /** False for a list that is not a search (the list of everything): no "search the web for…" closes it. */
    val web: Boolean get() = true
    /** Other words its row is found by, which do not enter it: `help` finds the list of everything, but "help with taxes" is a search. */
    val words: List<String> get() = emptyList()
    /** False for a keyword the user has not chosen yet (another app's): it is entered from its row, not by a Space. */
    val spaceEnters: Boolean get() = true
    /**
     * Keywords of it that are too short to take the first row when typed alone: "go" is also how Google starts, "wa" how
     * WhatsApp and Wallpaper do. Typed alone, such a keyword is a hint under the apps and settings of that start; with its
     * space it enters the scope like any other.
     */
    val shy: List<String> get() = emptyList()
    /**
     * A keyword that is a short way into an app's chip: `play` is the music app last played in (or
     * the one [text] names), `yt` is YouTube where it is installed. The chip in the field is then
     * that app's, and its rows are that chip's. Null: the scope is its own chip.
     */
    fun door(text: String): Door? = null
    suspend fun rows(arg: String): List<Result>
}

/**
 * An app as the chip in the field: what is typed goes to this app, for the one of its [acts] that
 * is armed. One chip for an app, whichever is armed: the chip shows the app's own [icon] and its
 * name, and the placeholder says what to type.
 */
interface AppChip : Scope {
    val icon: Icon.App
    /** What the app has of the two, in the row's order. Empty: the app has no chip (any more). */
    val acts: List<Act>
    /** The field's placeholder while [act] is armed: "Search Spotify", "Song, artist or album". */
    fun hint(act: Act): String
    /** [act] in a few words, for the line under the empty field that offers the action that is not armed: "Play in Spotify". */
    fun offer(act: Act): String
    /**
     * The rows for [arg] with [act] armed. [word]: the keyword the chip was entered by (`play`, `yt`);
     * null when it was entered from the app's own row.
     */
    suspend fun rows(arg: String, act: Act, word: String?): List<Result>
    override suspend fun rows(arg: String): List<Result> = acts.firstOrNull()?.let { rows(arg, it, null) } ?: emptyList()
}

/** Where a keyword leads that is a short way into an app's chip: the chip, and the action armed there. */
class Door(val chip: AppChip, val act: Act)
