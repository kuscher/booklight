package io.github.kuscher.booklight.providers

import android.content.Context
import android.os.SystemClock
import android.text.format.DateFormat
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.AirLabs
import io.github.kuscher.booklight.core.Airlines
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.DayForm
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Failure
import io.github.kuscher.booklight.core.Flight
import io.github.kuscher.booklight.core.FlightEnd
import io.github.kuscher.booklight.core.FlightNumber
import io.github.kuscher.booklight.core.FlightState
import io.github.kuscher.booklight.core.FlightStatus
import io.github.kuscher.booklight.core.Flights
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Saying
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Slot
import io.github.kuscher.booklight.core.SlotState
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.scopes.clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.URL
import java.net.UnknownHostException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * A flight number is a row: `LH455` names Lufthansa, and Enter opens the flight's page. That much
 * needs nothing but the bundled table of airlines; nothing leaves the device before Enter.
 *
 * With a key of the user's own for AirLabs (set in the Booklight window; Booklight ships none) the
 * row answers: who flies it and where, when it leaves and lands in each airport's own time, the
 * status in words, and where to go. For that the number and the key go to airlabs.co over HTTPS,
 * and nothing else does (no cookies, no identifiers, a plain "Booklight" user agent, as in
 * `SuggestProvider`). When: once, after a pause in typing, for a number that reads as a flight and
 * as little else; for one that is far more often something else (`ps5`) only when the user goes to
 * its row; never for each letter. An answer is kept for two minutes, in memory only.
 *
 * The row stands in its full height with its lines and its actions from its first frame, and the
 * answer is written into it. The panel asks ([waits], [answer], [gone]); this class says what the
 * row looks like before, with and without an answer.
 */
class FlightsProvider(private val context: Context, private val prefs: Prefs, private val scope: CoroutineScope) : Provider {
    override val id = ID

    private val airlines: Airlines by lazy { context.assets.open("airlines.tsv").bufferedReader().useLines { Airlines(it) } }

    /** What the service said for a number, or why it said nothing; [at] by the clock that never jumps, [wall] as the user's clock showed it. */
    private class Kept(val flight: Flight?, val failure: Failure?, val key: Int, val at: Long = SystemClock.elapsedRealtime(), val wall: LocalDateTime = LocalDateTime.now())

    private val kept = ConcurrentHashMap<String, Kept>()
    private val asking = HashMap<String, Deferred<Kept>>()
    /** The numbers of the rows made lately, by the row's id. */
    private val numbers = ConcurrentHashMap<String, FlightNumber>()
    /** The rows that stand waiting for an answer. */
    private val looking: MutableSet<String> = ConcurrentHashMap.newKeySet()
    /** The row of a weak match the user went to: it is looked up like any other from then on, until other text is typed. */
    @Volatile private var wanted: String? = null

    /** How many lookups the key has left this month, as the last reply said; null until one has. It is the key's: another key starts with none. */
    val left = MutableStateFlow<Int?>(null)

    /** What the pinned window shows, by the pin's own name for its flight (the number and the day it leaves): kept apart from the rows' answers, which go on to the next flight. */
    private val pins = ConcurrentHashMap<String, Flight>()

    init {
        scope.launch { prefs.flightKey.drop(1).collect { left.value = null } }
    }

    override suspend fun query(q: Query): List<Result> {
        val n = Flights.read(q.text, airlines, LocalDate.now())
        if (n == null) { wanted = null; return emptyList() }
        return listOf(row(n))
    }

    /** Under the keyword: whatever reads as a flight is one. Before it does, with a key in, the row stands empty, at the height it will have. */
    fun rows(arg: String): List<Result> {
        val n = Flights.read(arg, airlines, LocalDate.now(), sure = true)
        if (n != null) return listOf(row(n))
        if (prefs.flightKey.value.isEmpty()) return emptyList()
        return listOf(Result(
            id = "flight:", provider = ID, kind = Kind.OTHER, title = text(R.string.flight_name), icon = Icon.Symbol("plane"), score = 1.0, learnable = false, actions = emptyList(),
            // (No caption: it would say what the field's placeholder says, just above it. The two labels hold the height.)
            body = Body.Slots(null, listOf(slot(R.string.flight_leaves, null), slot(R.string.flight_lands, null)), first = first(null, LocalDate.now())),
        ))
    }

    /**
     * The row of the first flight number in a text someone wrote ("Landing with LH 454 at 12:45"), or
     * null: for the list under what was copied or handed over. Nothing is asked for it unasked there;
     * it answers when the user goes to it.
     */
    fun found(text: String): Result? = Flights.find(text, airlines, LocalDate.now())?.let { row(it, unasked = true) }

    /** True if [r] is a flight's row that stands waiting for its answer. */
    fun waits(r: Result): Boolean = r.provider == ID && r.id in looking

    /**
     * The user went to [r], a plain row nothing has been asked for (a weak match, or a number found in
     * a text), with a key in: the row it becomes, which waits for its answer (or has one that is
     * kept). Null for any other row.
     */
    fun gone(r: Result): Result? {
        if (r.provider != ID || r.body != null) return null
        val n = numbers[r.id] ?: return null
        if (prefs.flightKey.value.isEmpty()) return null
        wanted = r.id
        return row(n)
    }

    /**
     * Asks the service for the row [id], after a pause if [pause] (a pause that is cancelled sends
     * nothing), and gives the row with the answer in it, or with what went wrong. A request that is
     * on its way is waited for, not sent again; it finishes and is kept even when nobody waits for it
     * any more, so that the lookup it cost is not lost.
     */
    suspend fun answer(id: String, pause: Boolean): Result? {
        val n = numbers[id] ?: return null
        if (pause) delay(PAUSE_MS)
        val key = prefs.flightKey.value.ifEmpty { return null }
        if (fresh(id) == null) request(id, n, key)
        return row(n)
    }

    /**
     * One request for the row [id] at a time, in the process's own scope: whoever asks while it is on
     * its way waits for the same one, and it finishes and is kept whether or not anyone still waits.
     */
    private suspend fun request(id: String, n: FlightNumber, key: String): Kept {
        val job = synchronized(asking) {
            asking.getOrPut(id) { scope.async(Dispatchers.IO) { try { lookup(n, key).also { kept[id] = it } } finally { synchronized(asking) { asking.remove(id) } } } }
        }
        return job.await()
    }

    /**
     * The user went to [r], a row that says "No connection" or "No answer this time", and that was
     * long enough ago for another try: the row it becomes, which waits for its answer. Null for any
     * other row. (Without this such a row could only be asked again by changing the text.)
     */
    fun retry(r: Result): Result? {
        if (r.provider != ID || prefs.flightKey.value.isEmpty()) return null
        val n = numbers[r.id] ?: return null
        val k = kept[r.id] ?: return null
        if (k.flight != null || (k.failure != Failure.OFFLINE && k.failure != Failure.NO_ANSWER) || fresh(r.id) != null) return null
        wanted = r.id
        return row(n)
    }

    /** What the pinned window [key] shows, if this process has heard of that flight. */
    fun pinned(key: String): Flight? = pins[key]

    /**
     * Asks about a pinned flight again. [key] is the pin's name for it: the number as it was typed and
     * the day it leaves ("LH455 2026-10-01"). With [was], what the window shows, it is one request
     * about that one flight (`AirLabs.again`); with nothing to show (the process was made anew) it is
     * the lookup for that number on that day. Never "the next flight": a pin follows one flight. It
     * runs in the process's own scope, so an answer that was paid for is kept if the window goes.
     */
    suspend fun again(key: String, was: Flight?): AirLabs.Again {
        val n = Flights.read(key, airlines, LocalDate.now(), sure = true) ?: return AirLabs.Again.Gone
        val with = prefs.flightKey.value.ifEmpty { return AirLabs.Again.Gone }
        return scope.async(Dispatchers.IO) {
            try {
                val again = if (was != null) AirLabs.again(n, with, was, ::fetch).also { said(it.left, with) }.again
                else AirLabs.lookup(n, with, Instant.now(), ::fetch).also { said(it.left, with) }.let { a ->
                    val f = a.flight
                    when {
                        f != null -> AirLabs.Again.Is(f)
                        a.failure == Failure.OFFLINE || a.failure == Failure.NO_ANSWER -> AirLabs.Again.Failed
                        else -> AirLabs.Again.Gone
                    }
                }
                if (again is AirLabs.Again.Is) pins[key] = again.flight
                again
            } catch (_: Exception) {
                AirLabs.Again.Failed
            }
        }.await()
    }

    /** What is kept for the row [id], while it is the key's own and young enough (`AirLabs.keep`: two minutes; ten seconds for no connection; an hour for a number nobody flies). */
    private fun fresh(id: String): Kept? = kept[id]?.takeIf {
        it.key == prefs.flightKey.value.hashCode() && SystemClock.elapsedRealtime() - it.at < AirLabs.keep(it.failure).toMillis()
    }

    /** Asks the service (`AirLabs.lookup`: one request in the usual case, three at most) and keeps how many lookups it says are left. */
    private fun lookup(n: FlightNumber, key: String): Kept {
        // (Nothing of a failed request is logged: its address holds the key.)
        val a = try {
            AirLabs.lookup(n, key, Instant.now(), ::fetch)
        } catch (_: Exception) {
            // A reply that reads as JSON and still makes no sense (a date nobody can reckon with): no answer, never a crash.
            return Kept(null, Failure.NO_ANSWER, key.hashCode())
        }
        said(a.left, key)
        return Kept(a.flight, a.failure, key.hashCode())
    }

    /** The count of lookups left that a reply named, if the key it was asked with is still the one that is in. */
    private fun said(count: Int?, key: String) {
        if (count != null && key == prefs.flightKey.value) left.value = count
    }

    private fun fetch(url: String): String =
        try { get(url) } catch (e: Exception) { if (e is UnknownHostException || e is ConnectException || e is NoRouteToHostException) throw AirLabs.Unreachable() else throw e }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 4000
            c.readTimeout = 6000
            c.instanceFollowRedirects = false
            c.setRequestProperty("Accept", "application/json")
            // Not Android's default, which names the device model and build.
            c.setRequestProperty("User-Agent", "Booklight")
            // (The service says what is wrong in a reply with the status 200; anything else is no answer.)
            if (c.responseCode != 200) return ""
            return c.inputStream.use { it.readNBytes(256 * 1024) }.toString(Charsets.UTF_8)
        } finally {
            c.disconnect()
        }
    }

    // ---- what the row looks like

    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)

    /** [unasked]: the number stands in a text nobody typed here (what was copied or handed over): like a weak match, nothing is asked for it until the user goes to its row. */
    private fun row(n: FlightNumber, unasked: Boolean = false): Result {
        val id = "flight:${n.id}"
        numbers[id] = n
        if (numbers.size > 64) numbers.keys.filter { it != id && it != wanted }.take(32).forEach { numbers.remove(it) }
        if (wanted != null && wanted != id) wanted = null
        val key = prefs.flightKey.value
        val score = if (n.strong) SCORE else SearchEngine.GUESS
        val k = if (key.isEmpty()) null else fresh(id)
        // No key, or a row nobody has gone to (a weak match, a number in a text): an ordinary row that names the airline.
        if (key.isEmpty() || (k == null && wanted != id && (!n.strong || unasked))) { looking.remove(id); return plain(n, id, score, times = key.isEmpty()) }
        if (k == null) looking.add(id) else looking.remove(id)
        return full(n, id, score, k)
    }

    private fun open(n: FlightNumber) = Action("open", text(R.string.action_open), Effect.OpenUrl(Flights.page(n)))
    private fun search(n: FlightNumber, more: Boolean) = Action("search", text(if (more) R.string.action_search_web else R.string.action_search),
        Effect.OpenUrl(prefs.now.engine().search(text(R.string.flight_search, n.iata))), symbol = "search", more = more)
    private fun gemini(n: FlightNumber, more: Boolean) = Action("gemini", text(R.string.gemini_title), Effect.AskGemini(text(R.string.flight_question, n.shown)), symbol = "send", more = more)

    /** The 56 dp row: who flies it. [times]: no key is in, and the last action says where one goes. */
    private fun plain(n: FlightNumber, id: String, score: Double, times: Boolean) = Result(
        id = id, provider = ID, kind = Kind.OTHER, title = text(R.string.flight_who, n.shown, n.airline.name), icon = Icon.Symbol("plane"),
        score = score, learnable = false, label = text(R.string.flight_kind),
        actions = listOfNotNull(open(n), search(n, more = false), gemini(n, more = false),
            Action("times", text(R.string.action_get_times), KEY, symbol = "settings").takeIf { times }),
    )

    /** The 92 dp row: waiting ([k] null), answered, or saying why not. Its three lines and its actions keep their places through all three. */
    private fun full(n: FlightNumber, id: String, score: Double, k: Kept?): Result {
        val f = k?.flight
        val failed = k != null && f == null
        val today = LocalDate.now()
        val now = Instant.now()
        // A timetable's flight whose time to leave has passed: nobody knows what became of it, and nothing is made of it but a copy.
        val past = f != null && f.timetable && FlightStatus.departed(f, now)
        val who = text(R.string.flight_who, n.shown, f?.airline?.ifEmpty { null } ?: n.airline.name)
        val caption = if (f == null) who else text(R.string.flight_who, who, text(R.string.flight_route, f.from.place, f.to.place))
        val status: String
        var rest: String? = null
        when {
            k == null -> status = text(R.string.flight_looking)
            f == null -> status = text(when (k.failure) {
                Failure.NOT_FOUND -> R.string.flight_none
                Failure.NOT_THAT_DAY -> R.string.flight_no_day
                Failure.REFUSED -> R.string.flight_refused
                Failure.USED_UP -> R.string.flight_used_up
                Failure.OFFLINE -> R.string.flight_offline
                else -> R.string.flight_no_answer
            })
            else -> {
                val (main, extra) = said(f, now)
                status = main
                rest = listOfNotNull(extra, where(f).ifEmpty { null }).joinToString(" · ").ifEmpty { null }
            }
        }
        val line = f?.let { line(n, it, today, now) }
        val event = f?.let { event(it, now) }
        val pin = f?.let { pinKey(n, it) }
        if (f != null && pin != null) { if (pins.size > 32) pins.keys.filter { it != pin }.take(16).forEach { pins.remove(it) }; pins[pin] = f }
        return Result(
            id = id, provider = ID, kind = Kind.OTHER, title = caption, icon = Icon.Symbol("plane"), score = score, learnable = false, label = text(R.string.flight_kind),
            body = Body.Slots(
                caption,
                listOf(slot(if (f != null && FlightStatus.departed(f, now)) R.string.flight_left else R.string.flight_leaves, f?.from?.let { at(it, today) }),
                    slot(if (f != null && FlightStatus.arrived(f, now)) R.string.flight_landed_at else R.string.flight_lands, f?.to?.let { at(it, today) })),
                note = status, tail = rest, first = first(n.day, today),
                struck = f?.state == FlightState.CANCELLED || f?.state == FlightState.DIVERTED,
                source = k?.takeIf { f != null }?.let { text(R.string.flight_source, AirLabs.NAME, clock(context, it.wall)) },
            ),
            // All of them from the first frame. What needs the answer waits for it; where none came, it stands dimmed.
            actions = listOfNotNull(
                open(n),
                // The key was refused: the way to where it is set takes the place of what cannot be had.
                Action("key", text(R.string.action_flight_key), KEY, symbol = "settings").takeIf { k?.failure == Failure.REFUSED },
                Action("copy", text(R.string.action_copy), line?.let { Effect.CopyText(it) } ?: WAIT, off = failed).takeIf { k?.failure != Failure.REFUSED },
                Action("pin", text(R.string.action_pin), if (line != null && pin != null) Effect.Pin("flight", line, note = pin) else WAIT, symbol = "pin", off = failed || past || (f != null && pin == null)).takeIf { k?.failure != Failure.REFUSED },
                Action("calendar", text(R.string.action_calendar), event ?: WAIT, symbol = "event", more = true).takeIf { !failed && (f == null || event != null) },
                search(n, more = true),
                gemini(n, more = true),
            ),
        )
    }

    /**
     * The least width of the row's first slot, in dp, so that "Lands" stands at one x before the answer
     * and after it. It follows only what is known before the answer: the clock, and a typed day more
     * than six days off (said as a date). Never the answer: a slot that widened when a flight turned
     * out to leave on another day moved "Lands" by 60 dp as the answer landed.
     */
    private fun first(day: LocalDate?, today: LocalDate): Int =
        (if (DateFormat.is24HourFormat(context)) FIRST else FIRST_12) + if (day != null && FlightStatus.dayForm(day.atStartOfDay(), today) == DayForm.DATE) FIRST_DATE else 0

    private fun slot(label: Int, value: String?) = Slot(text(label), value.orEmpty(), if (value == null) SlotState.EMPTY else SlotState.TYPED)

    /** A time at one end, in that airport's own time: "SFO 15:05", and with the day when it is not the user's today: "FRA Fri 10:55". */
    private fun at(e: FlightEnd, today: LocalDate): String? {
        val t = e.time ?: return null
        return listOfNotNull(e.code, flightDay(context, t, today), clock(context, t)).joinToString(" ")
    }

    /** The pin's name for the flight it follows: the number as it was typed and the day it leaves, which `Flights.read` reads back as that number on that day. */
    private fun pinKey(n: FlightNumber, f: Flight): String? =
        (f.from.planned ?: f.from.time)?.toLocalDate()?.let { (if (n.callsign) n.icao else n.iata) + " " + it }

    /** "25 min", "1 h 15 min", "2 h". */
    private fun length(minutes: Int): String = when {
        minutes < 60 -> text(R.string.flight_min, minutes)
        minutes % 60 == 0 -> text(R.string.flight_h, minutes / 60)
        else -> text(R.string.flight_h_min, minutes / 60, minutes % 60)
    }

    /** The status in words, and what goes on after it in the lighter ink ("In the air", then "18 min late"). */
    private fun said(f: Flight, now: Instant): Pair<String, String?> {
        val s = FlightStatus.said(f, now)
        return when (s.saying) {
            Saying.PLANNED -> text(R.string.flight_planned) to text(R.string.flight_timetable).takeIf { f.timetable }
            // Its time to leave has passed: not "Planned" any more, and nothing else is known.
            Saying.TIMETABLE -> text(R.string.flight_timetable).replaceFirstChar { it.titlecase(context.resources.configuration.locales[0]) } to null
            Saying.ON_TIME -> text(R.string.flight_on_time) to null
            Saying.DELAYED -> text(R.string.flight_delayed, length(s.minutes)) to null
            Saying.IN_AIR -> text(R.string.flight_air) to null
            Saying.AIR_ON_TIME -> text(R.string.flight_air) to text(R.string.flight_air_on_time)
            Saying.AIR_LATE -> text(R.string.flight_air) to text(R.string.flight_air_late, length(s.minutes))
            Saying.AIR_EARLY -> text(R.string.flight_air) to text(R.string.flight_air_early, length(s.minutes))
            Saying.LANDED -> text(R.string.flight_landed) to null
            Saying.LANDED_LATE -> text(R.string.flight_landed_late, length(s.minutes)) to null
            Saying.LANDED_EARLY -> text(R.string.flight_landed_early, length(s.minutes)) to null
            Saying.DIVERTED -> text(R.string.flight_diverted) to null
            Saying.CANCELLED -> text(R.string.flight_cancelled) to null
        }
    }

    /** "Terminal G, gate G4"; "Gate A2" where no terminal is known; nothing where nothing is. */
    private fun where(f: Flight): String {
        val w = FlightStatus.where(f)
        return listOfNotNull(w.terminal?.let { text(R.string.flight_terminal, it) }, w.gate?.let { text(R.string.flight_gate, it) }, w.belt?.let { text(R.string.flight_belt, it) })
            .joinToString(", ").replaceFirstChar { it.titlecase(context.resources.configuration.locales[0]) }
    }

    /** One line to send to someone: "LH 455 San Francisco 15:05 → Frankfurt Fri 10:55, delayed 25 min, Terminal G, gate G4". */
    private fun line(n: FlightNumber, f: Flight, today: LocalDate, now: Instant): String {
        fun time(e: FlightEnd) = e.time?.let { t -> listOfNotNull(flightDay(context, t, today), clock(context, t)).joinToString(" ") }.orEmpty()
        val (main, extra) = said(f, now)
        val status = listOfNotNull(main, extra).joinToString(", ").replaceFirstChar { it.lowercase(context.resources.configuration.locales[0]) }
        return listOf(text(R.string.flight_line, n.shown, f.from.place, time(f.from), f.to.place, time(f.to)).replace("  ", " ").trim(), status, where(f)).filter { it.isNotEmpty() }.joinToString(", ")
    }

    /**
     * The flight as an event from take-off to landing, for the calendar's editor. Null where there is
     * no event to make: a time is missing; the flight is over (landed, diverted) or will not happen;
     * it is a timetable's flight whose time has passed; or one after a clock change, whose moments may
     * be an hour out (an event at the wrong hour is worse than none).
     */
    private fun event(f: Flight, now: Instant): Effect? {
        val from = f.from.time?.let(f.from::moment) ?: return null
        val to = f.to.time?.let(f.to::moment) ?: return null
        if (f.state == FlightState.CANCELLED || f.state == FlightState.LANDED || f.state == FlightState.DIVERTED || !to.isAfter(from)) return null
        if (f.loose || (f.timetable && FlightStatus.departed(f, now))) return null
        val number = f.number.let { if (it.length > 2) it.take(2) + " " + it.drop(2) else it }
        return Effect.InsertEvent("$number ${text(R.string.flight_route, f.from.place, f.to.place)}", from.toEpochMilli(), to.toEpochMilli(), allDay = false, place = f.from.airport.ifEmpty { f.from.code })
    }

    companion object {
        const val ID = "flights"
        /** "This needs the answer, which is on its way": the panel runs the action once the answer is in. */
        val WAIT: Effect = Effect.Internal("flight")
        /** The place in Booklight's window where the key is set. */
        val KEY: Effect = Effect.Internal("flights")
        /** Under every local match (the weakest is a scope found by a few scattered letters, 0.36), over the web search. */
        private const val SCORE = 0.3
        /** How long typing rests before the service is asked. */
        private const val PAUSE_MS = 400L
        /**
         * The least width of the row's first slot. Worked out from the widest string measured on a device:
         * "LEAVES SFO Wed 2:47 PM" is 270 px at 1.5 px per dp (the label 65, the gap 14, the value 191), 180 dp.
         * On a 24-hour clock the value loses " PM" and gains a digit, about 15 dp less: "LEAVES SFO Wed 14:47" is
         * about 165 dp. A date in the weekday's place ("24 Dec" for "Wed") is about 23 dp more on either clock.
         */
        private const val FIRST = 176
        private const val FIRST_12 = 196
        private const val FIRST_DATE = 24
    }
}

/** The day beside a flight's time, as the row, the copied line and the pinned window say it: nothing on the user's today, the weekday within six days ("Fri"), further off the date ("24 Dec"). */
internal fun flightDay(context: Context, t: LocalDateTime, today: LocalDate): String? {
    val pattern = when (FlightStatus.dayForm(t, today)) { DayForm.NONE -> return null; DayForm.WEEKDAY -> "EEE"; DayForm.DATE -> "d MMM" }
    return t.format(DateTimeFormatter.ofPattern(pattern, context.resources.configuration.locales[0]))
}

/** `flight u2 8001`: after the keyword whatever reads as a flight number is one, and is looked up like any other. */
class FlightScope(private val context: Context, private val flights: FlightsProvider, /** The keywords of the user's own links: one of those keeps its keyword. */ private val users: () -> Set<String> = { emptySet() }) : Scope {
    override val key = "flight"
    private val all = context.getString(R.string.flight_keys).split(',')
    override val keywords: List<String> get() = users().let { u -> all.filter { it.lowercase() !in u } }
    override val name: String = context.getString(R.string.flight_name)
    override val symbol = "plane"
    override val hint: String = context.getString(R.string.flight_hint)
    override val about: String = context.getString(R.string.flight_about)
    override suspend fun rows(arg: String): List<Result> = flights.rows(arg)
}
