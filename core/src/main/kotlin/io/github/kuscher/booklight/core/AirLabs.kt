package io.github.kuscher.booklight.core

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Why there is no answer, for the row to say in plain words. */
enum class Failure {
    /** The service knows no flight of that number. */
    NOT_FOUND,
    /** The key is unknown to the service, or has run out. */
    REFUSED,
    /** The key's lookups for the month are used up. */
    USED_UP,
    /** The number does not fly on the day that was asked for. */
    NOT_THAT_DAY,
    /** The device reached nobody. */
    OFFLINE,
    /** Anything else: a reply that could not be read, an error of the service's own. */
    NO_ANSWER,
}

/** What a reply held: [value], or why not. [left]: how many lookups the key has left this month, where the reply says. */
class Reply<out T>(val value: T?, val failure: Failure? = null, val left: Int? = null)

/** A line of an airline's timetable: this number flies from here to there at these times on these days. Times are each airport's own. */
data class Route(
    val from: String, val to: String, val leaves: LocalTime, val minutes: Int, val days: Set<DayOfWeek>,
    /** How many minutes each airport's clock is ahead of UTC. */
    val fromOffset: Int, val toOffset: Int,
    val fromTerminal: String? = null, val toTerminal: String? = null,
)

/**
 * AirLabs (airlabs.co, API v9), read: the address to ask, and what its replies say. What is written
 * here was seen in its replies on 2 October 2026 (docs/research/flights.md, "AirLabs, tried"):
 * `flight` answers with the one flight of a number that is nearest to now, `schedules` with the
 * flights of the next ten hours, `routes` with the timetable. An error comes with the status 200
 * and an `error` object. A reply from the network is never trusted to be well formed.
 */
object AirLabs {
    const val NAME = "AirLabs"
    /** Where a key is got. */
    const val SIGN_UP = "https://airlabs.co/signup"
    private const val API = "https://airlabs.co/api/v9/"

    /** The one flight of this number nearest to now. A callsign is asked for as one. */
    fun flight(n: FlightNumber, key: String): String = API + "flight?" + (if (n.callsign) "flight_icao=${n.icao}" else "flight_iata=${n.iata}") + key(key)
    /** The flights of this number in the next ten hours, and the ones just flown. */
    fun schedules(n: FlightNumber, key: String): String = API + "schedules?flight_iata=${n.iata}" + key(key)
    /** The timetable for this number. */
    fun routes(n: FlightNumber, key: String): String = API + "routes?flight_iata=${n.iata}" + key(key)
    private fun key(key: String) = "&api_key=" + java.net.URLEncoder.encode(key.trim(), "UTF-8")

    /** Thrown by whoever fetches when the device reached nobody: the row then says "No connection". */
    class Unreachable : Exception()

    /** What a lookup came to: the flight, or why there is none; and how many lookups the key has left, where a reply said. */
    class Answer(val flight: Flight?, val failure: Failure?, val left: Int?)

    /**
     * The next flight of [n], or the one on its day, asked for through [get] (the address in, the
     * reply's text out; it may throw). `flight` answers with the one nearest to now. If that one
     * landed more than three hours ago, the next is among the coming ten hours' (`schedules`) or,
     * further off, in the timetable (`routes`); a day that is not that flight's is the timetable's
     * too, and so is a number `flight` does not know (one that flies once a week). Up to three
     * requests, and one in the usual case.
     */
    fun lookup(n: FlightNumber, key: String, now: Instant, get: (String) -> String): Answer {
        var left: Int? = null
        fun <T> ask(url: String, read: (String) -> Reply<T>): Reply<T> {
            val body = try { get(url) } catch (_: Unreachable) { return Reply(null, Failure.OFFLINE) } catch (_: Exception) { return Reply(null, Failure.NO_ANSWER) }
            return read(body).also { r -> r.left?.let { left = it } }
        }
        val first = ask(flight(n, key)) { flight(it) }
        val day = n.day
        if (first.failure == Failure.NOT_FOUND) {
            val timetable = ask(routes(n, key)) { routes(it) }
            // The timetable was not to be had (no connection, no lookups left): that is what went wrong, not "nothing found".
            timetable.failure?.takeIf { it != Failure.NOT_FOUND }?.let { return Answer(null, it, left) }
            val lines = timetable.value.orEmpty()
            val like = Flight(n.iata, n.airline.name, FlightEnd(""), FlightEnd(""), FlightState.PLANNED)
            val planned = (if (day != null) on(lines, like, day) else upcoming(lines, like, now))?.let { dated(it, now) }
            return Answer(planned, if (planned != null) null else if (day != null && lines.isNotEmpty()) Failure.NOT_THAT_DAY else Failure.NOT_FOUND, left)
        }
        val f = first.value ?: return Answer(null, first.failure ?: Failure.NO_ANSWER, left)
        if (day != null) {
            if (f.from.planned?.toLocalDate() == day || f.from.time?.toLocalDate() == day) return Answer(f, null, left)
            val timetable = ask(routes(n, key)) { routes(it) }
            val lines = timetable.value ?: return Answer(null, timetable.failure?.takeIf { it != Failure.NOT_FOUND } ?: Failure.NOT_THAT_DAY, left)
            return on(lines, f, day)?.let { Answer(dated(it, now), null, left) } ?: Answer(null, Failure.NOT_THAT_DAY, left)
        }
        if (!FlightStatus.over(f, now)) return Answer(f, null, left)
        val soon = ask(schedules(n, key)) { schedules(it) }.value.orEmpty()
        FlightStatus.next(soon, now)?.let { return Answer(named(it, f), null, left) }
        val lines = ask(routes(n, key)) { routes(it) }.value.orEmpty()
        // Nothing ahead that anyone knows of: the one that was, as it was.
        return Answer(upcoming(lines, f, now)?.let { dated(it, now) } ?: f, null, left)
    }

    /** What asking again about one flight came to (the pinned window's question). */
    sealed interface Again {
        /** The same flight, as the service says it now. */
        data class Is(val flight: Flight) : Again
        /** The service has nothing more to say about it: it answers with a later flight of the number, knows none, or will not answer this key. Asking ends. */
        data object Gone : Again
        /** The service does not have its day yet (it still answers with the flight before): the plan stands, and the next ask comes when it is due. */
        data object NotYet : Again
        /** No answer this time (no connection, a reply nobody can read): worth another try soon. */
        data object Failed : Again
    }

    /** [again], and how many lookups the key has left, where the reply said. */
    class Asked(val again: Again, val left: Int?)

    /**
     * Asks about the flight [was] once more: one request, `flight`, whatever the first lookup took.
     * The pinned window follows one flight, not "the next" of its number: when the service has gone
     * on to another day's, that is the end of the asking, never a new flight to count down to.
     */
    fun again(n: FlightNumber, key: String, was: Flight, get: (String) -> String): Asked {
        val body = try { get(flight(n, key)) } catch (_: Exception) { return Asked(Again.Failed, null) }
        val r = flight(body)
        val got = r.value
        val again = when {
            got != null && FlightStatus.same(was, got) -> Again.Is(got)
            // Another flight of the number. For a plan from the timetable it may be the one before it (the service is not there yet) or one after it.
            got != null -> if (was.timetable && before(got, was)) Again.NotYet else Again.Gone
            r.failure == Failure.OFFLINE || r.failure == Failure.NO_ANSWER -> Again.Failed
            r.failure == Failure.NOT_FOUND && was.timetable -> Again.NotYet
            else -> Again.Gone
        }
        return Asked(again, r.left)
    }

    private fun before(a: Flight, b: Flight): Boolean {
        val x = a.from.planned?.let(a.from::moment) ?: return false
        val y = b.from.planned?.let(b.from::moment) ?: return false
        return x.isBefore(y)
    }

    /** How long an answer is kept before the service is asked again for the same number. */
    fun keep(failure: Failure?): Duration = when (failure) {
        // What may be right again in a moment is asked again soon.
        Failure.OFFLINE, Failure.NO_ANSWER -> Duration.ofSeconds(10)
        // A number nobody flies, or not on that day, costs two lookups to find out and stays so: an hour.
        Failure.NOT_FOUND, Failure.NOT_THAT_DAY -> Duration.ofHours(1)
        // An answer, a refused key (a new key is asked at once), a month's lookups used up: two minutes.
        else -> Duration.ofMinutes(2)
    }

    fun flight(body: String): Reply<Flight> = read(body) { (it as? Map<*, *>)?.let(::flight) }

    fun schedules(body: String): Reply<List<Flight>> = read(body) { r -> (r as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.let(::flight) } }

    fun routes(body: String): Reply<List<Route>> = read(body) { r -> (r as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.let(::route) } }

    private fun <T> read(body: String, take: (Any?) -> T?): Reply<T> {
        val all = try { Json.parse(body) as? Map<*, *> } catch (_: Exception) { null } ?: return Reply(null, Failure.NO_ANSWER)
        val left = (((all["request"] as? Map<*, *>)?.get("key") as? Map<*, *>)?.get("limits_total") as? Double)?.toInt()
        (all["error"] as? Map<*, *>)?.let { e ->
            return Reply(null, when (e["code"]) {
                "not_found" -> Failure.NOT_FOUND
                "unknown_api_key", "expired_api_key" -> Failure.REFUSED
                "month_limit_exceeded" -> Failure.USED_UP
                else -> Failure.NO_ANSWER
            }, left)
        }
        val value = try { take(all["response"]) } catch (_: Exception) { null }
        return if (value == null) Reply(null, Failure.NO_ANSWER, left) else Reply(value, null, left)
    }

    private val TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private fun Map<*, *>.text(key: String): String? = (this[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }
    private fun Map<*, *>.time(key: String): LocalDateTime? = text(key)?.let { runCatching { LocalDateTime.parse(it, TIME) }.getOrNull() }

    private fun flight(m: Map<*, *>): Flight? {
        val number = m.text("flight_iata") ?: m.text("flight_icao") ?: return null
        val from = end(m, "dep") ?: return null
        val to = end(m, "arr") ?: return null
        val state = when (m.text("status")) {
            "scheduled" -> FlightState.PLANNED
            "en-route", "active" -> FlightState.IN_AIR
            "landed" -> FlightState.LANDED
            "cancelled" -> FlightState.CANCELLED
            "diverted" -> FlightState.DIVERTED
            // A word not seen before: what the times say.
            else -> if (to.actual != null) FlightState.LANDED else if (from.actual != null) FlightState.IN_AIR else FlightState.PLANNED
        }
        return Flight(number, m.text("airline_name").orEmpty(), from, to, state, flownAs = m.text("cs_flight_iata"))
    }

    /** One end, from the fields that start with [p] (`dep`, `arr`). Its clock's distance from UTC is read off a time that is given both ways. */
    private fun end(m: Map<*, *>, p: String): FlightEnd? {
        val code = m.text("${p}_iata") ?: m.text("${p}_icao") ?: return null
        val offset = listOf("time", "estimated", "actual").firstNotNullOfOrNull { k ->
            val local = m.time("${p}_$k"); val utc = m.time("${p}_${k}_utc")
            // (No clock on earth is further from UTC than fourteen hours: anything else is not an airport's time.)
            if (local != null && utc != null) Duration.between(utc, local).toMinutes().takeIf { it in -14 * 60..14 * 60 }?.toInt() else null
        } ?: 0
        return FlightEnd(
            code, m.text("${p}_city").orEmpty(), m.text("${p}_name").orEmpty(),
            m.time("${p}_time"), m.time("${p}_estimated"), m.time("${p}_actual"), offset,
            m.text("${p}_terminal"), m.text("${p}_gate"), if (p == "arr") m.text("arr_baggage") else null,
        )
    }

    private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")
    private val DAYS = mapOf("mon" to DayOfWeek.MONDAY, "tue" to DayOfWeek.TUESDAY, "wed" to DayOfWeek.WEDNESDAY, "thu" to DayOfWeek.THURSDAY,
        "fri" to DayOfWeek.FRIDAY, "sat" to DayOfWeek.SATURDAY, "sun" to DayOfWeek.SUNDAY)

    private fun route(m: Map<*, *>): Route? {
        fun clock(key: String) = m.text(key)?.let { runCatching { LocalTime.parse(it, CLOCK) }.getOrNull() }
        /**
         * A clock against UTC, from the same moment said both ways without a day. Ten hours behind and
         * fourteen ahead are the same two clocks, and which it is cannot be told from them: taken as
         * between eleven hours behind and thirteen ahead, which is right for Hawaii, New Zealand, Fiji
         * and Tonga and wrong only for American Samoa, Niue and Kiritimati. ([planned] takes an
         * airport's clock from a dated reply where it has one.)
         */
        fun offset(local: LocalTime?, utc: LocalTime?): Int {
            if (local == null || utc == null) return 0
            val d = (local.toSecondOfDay() - utc.toSecondOfDay()) / 60
            return if (d > 13 * 60) d - 24 * 60 else if (d <= -11 * 60) d + 24 * 60 else d
        }
        val leaves = clock("dep_time") ?: return null
        val minutes = (m["duration"] as? Double)?.toInt()?.takeIf { it > 0 } ?: return null
        val days = (m["days"] as? List<*>)?.mapNotNull { DAYS[it] }?.toSet().orEmpty()
        fun one(key: String) = (m[key] as? List<*>)?.filterIsInstance<String>()?.singleOrNull()
        return Route(
            m.text("dep_iata") ?: return null, m.text("arr_iata") ?: return null, leaves, minutes, days,
            offset(leaves, clock("dep_time_utc")), offset(clock("arr_time"), clock("arr_time_utc")), one("dep_terminals"), one("arr_terminals"),
        )
    }

    /**
     * The flight a line of the timetable is on [day], said with the names [like] has (the same number,
     * another day). A plan: no gate, no delay. Null if it does not fly on that day of the week. Where
     * [like] has the same airport with a dated time, that reply's clock is taken for it: read off a
     * date, it is exact, and the timetable's own is a guess for the clocks furthest from UTC.
     */
    fun planned(route: Route, like: Flight, day: LocalDate): Flight? {
        if (route.days.isNotEmpty() && day.dayOfWeek !in route.days) return null
        fun known(code: String) = listOf(like.from, like.to).firstOrNull { it.code == code }
        val fromOffset = known(route.from)?.takeIf { it.time != null }?.offset ?: route.fromOffset
        val toOffset = known(route.to)?.takeIf { it.time != null }?.offset ?: route.toOffset
        val leaves = day.atTime(route.leaves)
        val lands = LocalDateTime.ofEpochSecond(leaves.toEpochSecond(ZoneOffset.ofTotalSeconds(fromOffset * 60)) + route.minutes * 60L, 0, ZoneOffset.ofTotalSeconds(toOffset * 60))
        return Flight(
            like.number, like.airline,
            FlightEnd(route.from, known(route.from)?.city.orEmpty(), known(route.from)?.airport.orEmpty(), planned = leaves, offset = fromOffset, terminal = route.fromTerminal),
            FlightEnd(route.to, known(route.to)?.city.orEmpty(), known(route.to)?.airport.orEmpty(), planned = lands, offset = toOffset, terminal = route.toTerminal),
            FlightState.PLANNED, like.flownAs, timetable = true,
        )
    }

    /**
     * The next flight of the timetable to leave after [now], within a week: the soonest among the
     * lines that start where [like] started (a number with two legs stays on its leg), else the
     * soonest of the others.
     */
    fun upcoming(routes: List<Route>, like: Flight, now: Instant): Flight? {
        fun soonest(lines: List<Route>): Flight? = lines.mapNotNull { r ->
            val today = LocalDateTime.ofEpochSecond(now.epochSecond, 0, ZoneOffset.ofTotalSeconds(r.fromOffset * 60)).toLocalDate()
            (-1L..7L).firstNotNullOfOrNull { d -> planned(r, like, today.plusDays(d))?.takeIf { it.from.moment(it.from.planned!!) > now } }
        }.minByOrNull { it.from.moment(it.from.planned!!) }
        val (same, other) = routes.partition { it.from == like.from.code }
        return soonest(same) ?: soonest(other)
    }

    /** The timetable's flight on [day], whichever of its lines flies then; the lines that start where [like] started come first. */
    fun on(routes: List<Route>, like: Flight, day: LocalDate): Flight? =
        routes.sortedBy { it.from != like.from.code }.firstNotNullOfOrNull { planned(it, like, day) }

    /** The zones there are, for [steady]: read once. */
    private val ZONES: List<ZoneId> by lazy { ZoneId.getAvailableZoneIds().mapNotNull { runCatching { ZoneId.of(it) }.getOrNull() } }

    /**
     * True if the clocks a timetable's flight was reckoned with still hold on its day. The timetable
     * gives each airport's clock as it is today and no zone, so nobody can say what that clock will
     * be after a change. What can be said: whether any place whose clock stands where this airport's
     * stands today changes it between [now] and the flight. If none does, the moments are right; if
     * one does (Frankfurt's, on a day after the end of summer time), they may be an hour out.
     */
    fun steady(f: Flight, now: Instant): Boolean = listOf(f.from, f.to).all { e ->
        val then = e.time?.let(e::moment) ?: return@all true
        ZONES.none { z -> z.rules.getOffset(now).totalSeconds == e.offset * 60 && z.rules.getOffset(then).totalSeconds != e.offset * 60 }
    }

    /** A timetable's flight with what is known of its clocks: [Flight.loose] where they may not hold on its day. */
    private fun dated(f: Flight, now: Instant): Flight = if (f.timetable && !steady(f, now)) f.copy(loose = true) else f

    /** [f], a flight from a reply that names no airline and no cities (`schedules`), with the names [like] has for them. */
    fun named(f: Flight, like: Flight): Flight {
        fun end(e: FlightEnd) = listOf(like.from, like.to).firstOrNull { it.code == e.code }?.let { e.copy(city = e.city.ifEmpty { it.city }, airport = e.airport.ifEmpty { it.airport }) } ?: e
        return f.copy(airline = f.airline.ifEmpty { like.airline }, from = end(f.from), to = end(f.to))
    }
}

/**
 * A reader for JSON that knows nothing about what it reads: objects as maps, arrays as lists, numbers
 * as doubles, and null for null. It throws on anything that is not JSON; whoever calls it catches.
 */
internal object Json {
    fun parse(text: String): Any? {
        val r = Reader(text)
        val v = r.value()
        r.ws()
        if (!r.done()) throw IllegalStateException("more after the value")
        return v
    }

    private class Reader(private val s: String) {
        private var i = 0
        private var depth = 0
        fun done() = i >= s.length
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        private fun peek(): Char = if (i < s.length) s[i] else throw IllegalStateException("end")
        private fun next(): Char = peek().also { i++ }
        private fun expect(c: Char) { if (next() != c) throw IllegalStateException("expected $c") }
        private fun word(w: String) { if (!s.startsWith(w, i)) throw IllegalStateException("expected $w"); i += w.length }

        fun value(): Any? {
            ws()
            if (++depth > 32) throw IllegalStateException("too deep")
            val v: Any? = when (peek()) {
                '{' -> obj()
                '[' -> list()
                '"' -> string()
                't' -> { word("true"); true }
                'f' -> { word("false"); false }
                'n' -> { word("null"); null }
                else -> number()
            }
            depth--
            return v
        }

        private fun obj(): Map<String, Any?> {
            expect('{')
            val out = LinkedHashMap<String, Any?>()
            ws()
            if (peek() == '}') { i++; return out }
            while (true) {
                ws()
                val k = string()
                ws(); expect(':')
                out[k] = value()
                ws()
                if (next() == ',') continue
                i--; expect('}')
                return out
            }
        }

        private fun list(): List<Any?> {
            expect('[')
            val out = ArrayList<Any?>()
            ws()
            if (peek() == ']') { i++; return out }
            while (true) {
                out.add(value())
                ws()
                if (next() == ',') continue
                i--; expect(']')
                return out
            }
        }

        private fun number(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            return s.substring(start, i).toDouble()
        }

        private fun string(): String {
            expect('"')
            val b = StringBuilder()
            while (true) {
                when (val c = next()) {
                    '"' -> return b.toString()
                    '\\' -> when (val e = next()) {
                        'n' -> b.append('\n'); 't' -> b.append('\t'); 'r' -> b.append('\r')
                        'b' -> b.append('\b'); 'f' -> b.append('\u000C')
                        'u' -> { b.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        else -> b.append(e)
                    }
                    else -> b.append(c)
                }
            }
        }
    }
}
