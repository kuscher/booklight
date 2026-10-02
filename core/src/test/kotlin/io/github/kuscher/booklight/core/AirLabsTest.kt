package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * AirLabs' replies, read. The files in `resources/airlabs` are what the service answered on 2 October
 * 2026 between 07:26 and 07:36 UTC, without their `request` object (it repeats the key and names the
 * caller's address).
 */
class AirLabsTest {
    private fun reply(name: String): String = javaClass.getResource("/airlabs/$name.json")!!.readText()
    private fun flight(name: String): Flight = AirLabs.flight(reply(name)).value!!
    private fun at(text: String): Instant = Instant.parse(text)
    private fun time(text: String): LocalDateTime = LocalDateTime.parse(text)
    private val lh455 = FlightNumber(Airline("LH", "DLH", "Lufthansa"), 455)
    /** When the saved replies were asked for. */
    private val asked = Instant.parse("2026-10-02T07:29:00Z")

    @Test fun `the addresses`() {
        assertEquals("https://airlabs.co/api/v9/flight?flight_iata=LH455&api_key=k", AirLabs.flight(lh455, "k"))
        assertEquals("https://airlabs.co/api/v9/flight?flight_icao=DLH455&api_key=k", AirLabs.flight(lh455.copy(callsign = true), " k\n"))
        assertEquals("https://airlabs.co/api/v9/schedules?flight_iata=LH455&api_key=k", AirLabs.schedules(lh455, "k"))
        assertEquals("https://airlabs.co/api/v9/routes?flight_iata=LH455&api_key=a%26b", AirLabs.routes(lh455, "a&b"))
    }

    @Test fun `a flight in the air`() {
        val f = flight("flight-LH455-in-the-air")
        assertEquals("LH455", f.number)
        assertEquals("Lufthansa", f.airline)
        assertEquals(FlightState.IN_AIR, f.state)
        assertTrue(f.left)
        assertNull(f.flownAs)
        assertEquals("SFO", f.from.code)
        assertEquals("San Francisco", f.from.city)
        assertEquals("San Francisco International Airport", f.from.airport)
        assertEquals(time("2026-10-01T14:40"), f.from.planned)
        assertEquals(time("2026-10-01T14:47"), f.from.actual)
        assertEquals(-420, f.from.offset)
        assertEquals(7, f.from.late)
        assertEquals("INTL", f.from.terminal)
        assertEquals("G13", f.from.gate)
        assertEquals("Boeing 747-8", f.aircraft)
        assertEquals("FRA", f.to.code)
        assertEquals("Frankfurt/Main", f.to.city)
        assertEquals(time("2026-10-02T10:25"), f.to.planned)
        assertEquals(time("2026-10-02T10:01"), f.to.expected)
        assertNull(f.to.actual)
        assertEquals(time("2026-10-02T10:01"), f.to.time)
        assertEquals(120, f.to.offset)
        assertEquals(-24, f.to.late)
        assertEquals("1", f.to.terminal)
        assertNull(f.to.belt)
        // The airport's own time is a moment.
        assertEquals(at("2026-10-01T21:47:00Z"), f.from.moment(f.from.time!!))
        assertEquals(at("2026-10-02T08:01:00Z"), f.to.moment(f.to.time!!))
    }

    @Test fun `what the row says for it`() {
        val f = flight("flight-LH455-in-the-air")
        assertEquals(Said(Saying.AIR_EARLY, 24), FlightStatus.said(f, asked))
        assertEquals(Where("1", null, null), FlightStatus.where(f))
        // Asked from San Francisco on Thursday: it left today and lands on another day.
        val today = LocalDate.of(2026, 10, 1)
        assertFalse(FlightStatus.otherDay(f.from.time!!, today))
        assertTrue(FlightStatus.otherDay(f.to.time!!, today))
        // Asked from Frankfurt on Friday it is the other way round.
        assertTrue(FlightStatus.otherDay(f.from.time!!, today.plusDays(1)))
        assertFalse(FlightStatus.otherDay(f.to.time!!, today.plusDays(1)))
    }

    @Test fun `planned, with a gate and no new time`() {
        val f = flight("flight-LH454-planned")
        assertEquals(FlightState.PLANNED, f.state)
        assertFalse(f.left)
        assertEquals(Said(Saying.PLANNED), FlightStatus.said(f, asked))
        assertEquals(Where("1", "Z58", null), FlightStatus.where(f))
        assertEquals(time("2026-10-02T10:25"), f.from.time)
        assertEquals(time("2026-10-02T12:40"), f.to.time)
        assertEquals(-420, f.to.offset)
    }

    @Test fun `delayed before it leaves`() {
        val f = flight("flight-LH152-delayed")
        assertEquals(FlightState.PLANNED, f.state)
        assertEquals(Said(Saying.DELAYED, 45), FlightStatus.said(f, asked))
        assertEquals(time("2026-10-02T09:35"), f.from.time)
        assertEquals(Where("1", "A2", null), FlightStatus.where(f))
        assertEquals("EN152", f.flownAs)
    }

    @Test fun `landed, three minutes after its plan, is landed`() {
        val f = flight("flight-LH96-landed")
        assertEquals(FlightState.LANDED, f.state)
        assertEquals(Said(Saying.LANDED), FlightStatus.said(f, asked))
        assertEquals(time("2026-10-02T09:13"), f.to.actual)
        assertEquals(Where("2", null, null), FlightStatus.where(f))
    }

    @Test fun `just landed, half an hour early, before the service has the minute it touched down`() {
        // The same flight as "in the air", asked again five minutes after it landed (07:58 UTC): landed, and still no actual time.
        val f = flight("flight-LH455-landed")
        assertEquals(FlightState.LANDED, f.state)
        assertNull(f.to.actual)
        assertEquals(time("2026-10-02T09:56"), f.to.time)
        assertEquals(Said(Saying.LANDED_EARLY, 29), FlightStatus.said(f, asked))
        assertEquals(Where("1", null, null), FlightStatus.where(f))
        assertEquals(FlightPin(PinState.LANDED), FlightStatus.pinned(f, at("2026-10-02T07:58:00Z")))
        assertFalse(FlightStatus.over(f, at("2026-10-02T10:56:00Z")))
        assertTrue(FlightStatus.over(f, at("2026-10-02T10:57:00Z")))
    }

    @Test fun `cancelled keeps its plan and says nothing of where to go`() {
        val f = flight("flight-LH1184-cancelled")
        assertEquals(FlightState.CANCELLED, f.state)
        assertEquals(Said(Saying.CANCELLED), FlightStatus.said(f, asked))
        assertEquals(Where(null, null, null), FlightStatus.where(f))
        assertEquals(time("2026-10-02T07:45"), f.from.time)
    }

    @Test fun `a number sold by one airline and flown by another`() {
        val f = flight("flight-LH9152-codeshare")
        assertEquals("LH9152", f.number)
        assertEquals("Lufthansa", f.airline)
        assertEquals("UA945", f.flownAs)
        // Thirteen minutes behind its plan: on time, since late starts at fifteen (it was "13 min late" while late started at five).
        assertEquals(13, f.to.late)
        assertEquals(Said(Saying.AIR_ON_TIME), FlightStatus.said(f, asked))
        assertEquals(-300, f.to.offset)
    }

    @Test fun `the errors`() {
        assertEquals(Failure.NOT_FOUND, AirLabs.flight(reply("error-not-found")).failure)
        assertEquals(Failure.REFUSED, AirLabs.flight(reply("error-unknown-key")).failure)
        assertEquals(Failure.NO_ANSWER, AirLabs.flight(reply("error-wrong-params")).failure)
        // The codes the documentation names and nobody could try.
        fun code(c: String) = AirLabs.flight("""{"error":{"message":"x","code":"$c"}}""").failure
        assertEquals(Failure.REFUSED, code("expired_api_key"))
        assertEquals(Failure.USED_UP, code("month_limit_exceeded"))
        assertEquals(Failure.NO_ANSWER, code("minute_limit_exceeded"))
        assertEquals(Failure.NO_ANSWER, code("internal_error"))
        assertNull(AirLabs.flight(reply("error-not-found")).value)
    }

    @Test fun `what is not a reply is no answer`() {
        for (body in listOf("", "<html>502</html>", "[]", "{", """{"response":"pong"}""", """{"response":{}}""", """{"response":{"flight_iata":"LH455"}}""", """{"response":[1,2]}""", "null", "{\"response\":" + "[".repeat(100) + "}"))
            assertEquals(body.take(30), Failure.NO_ANSWER, AirLabs.flight(body).failure)
        assertEquals(Failure.NO_ANSWER, AirLabs.schedules("{}").failure)
        assertEquals(Failure.NO_ANSWER, AirLabs.routes("""{"response":{}}""").failure)
    }

    @Test fun `a status nobody has seen is read off the times`() {
        fun f(status: String, extra: String = "") = AirLabs.flight("""{"response":{"flight_iata":"XX1","dep_iata":"AAA","arr_iata":"BBB","status":"$status","dep_time":"2026-10-02 08:00","dep_time_utc":"2026-10-02 06:00"$extra}}""").value!!
        assertEquals(FlightState.PLANNED, f("boarding").state)
        assertEquals(FlightState.IN_AIR, f("x", ""","dep_actual":"2026-10-02 08:05"""").state)
        assertEquals(FlightState.LANDED, f("x", ""","arr_actual":"2026-10-02 09:05"""").state)
        assertEquals(FlightState.DIVERTED, f("diverted").state)
        assertEquals(120, f("scheduled").from.offset)
        assertEquals("AAA", f("scheduled").from.place)
    }

    @Test fun `how many lookups are left is read where the reply says`() {
        val r = AirLabs.flight("""{"request":{"key":{"type":"free","limits_total":994}},"error":{"message":"Flight not found","code":"not_found"}}""")
        assertEquals(994, r.left)
        assertNull(AirLabs.flight(reply("error-unknown-key")).left)
    }

    @Test fun `the next ten hours of one number`() {
        val list = AirLabs.schedules(reply("schedules-LH455")).value!!
        assertEquals(1, list.size)
        assertEquals(FlightState.IN_AIR, list[0].state)       // `active` here is `en-route` there
        assertEquals("", list[0].from.city)                   // no names in this reply
        assertEquals("SFO", list[0].from.place)
    }

    @Test fun `which flight is the next`() {
        // SQ 26 is Singapore to Frankfurt and on to New York: three flights under one number in one reply.
        val list = AirLabs.schedules(reply("schedules-SQ26-two-legs")).value!!
        assertEquals(3, list.size)
        val now = at("2026-10-02T07:29:00Z")
        // The one in the air.
        assertEquals("JFK", FlightStatus.next(list, now)!!.to.code)
        // Without it: the one that landed two hours ago.
        val rest = list.filter { it.state != FlightState.IN_AIR }
        val landed = FlightStatus.next(rest, now)!!
        assertEquals(FlightState.LANDED, landed.state)
        assertEquals("21", landed.to.belt)
        assertEquals(Said(Saying.LANDED_LATE, 24), FlightStatus.said(landed, asked))
        assertEquals(Where("1", null, "21"), FlightStatus.where(landed))
        // Four hours after that landing: the next to leave.
        val later = FlightStatus.next(rest, at("2026-10-02T09:30:00Z"))!!
        assertEquals(FlightState.PLANNED, later.state)
        assertEquals(time("2026-10-02T23:55"), later.from.planned)
        // Nothing to choose from.
        assertNull(FlightStatus.next(emptyList(), now))
    }

    @Test fun `a flight is over three hours after it landed`() {
        val f = flight("flight-JL101-landed-nine-hours-ago")        // landed 22:34 UTC
        assertFalse(FlightStatus.over(f, at("2026-10-01T23:00:00Z")))
        assertFalse(FlightStatus.over(f, at("2026-10-02T01:34:00Z")))
        assertTrue(FlightStatus.over(f, at("2026-10-02T01:35:00Z")))
        assertTrue(FlightStatus.over(f, at("2026-10-02T07:28:00Z")))
        // In the air or still to leave is never over; a cancelled one is, three hours after it was to leave.
        assertFalse(FlightStatus.over(flight("flight-LH455-in-the-air"), at("2026-10-03T00:00:00Z")))
        assertFalse(FlightStatus.over(flight("flight-LH454-planned"), at("2026-10-03T00:00:00Z")))
        val gone = flight("flight-LH1184-cancelled")              // was to leave 05:45 UTC
        assertFalse(FlightStatus.over(gone, at("2026-10-02T07:29:00Z")))
        assertTrue(FlightStatus.over(gone, at("2026-10-02T08:46:00Z")))
    }

    @Test fun `the timetable`() {
        val routes = AirLabs.routes(reply("routes-LH455")).value!!
        assertEquals(1, routes.size)
        val r = routes[0]
        assertEquals("SFO", r.from); assertEquals("FRA", r.to)
        assertEquals(LocalTime.of(14, 40), r.leaves)
        assertEquals(645, r.minutes)
        assertEquals(DayOfWeek.entries.toSet(), r.days)
        assertEquals(-420, r.fromOffset)
        assertEquals(120, r.toOffset)
        assertNull(r.fromTerminal)              // two are named: none is said
        assertEquals("1", r.toTerminal)
    }

    @Test fun `another day is the timetable's flight, with the names of today's`() {
        val today = flight("flight-LH455-in-the-air")
        val r = AirLabs.routes(reply("routes-LH455")).value!![0]
        val f = AirLabs.planned(r, today, LocalDate.of(2026, 10, 3))!!
        assertTrue(f.timetable)
        assertEquals(FlightState.PLANNED, f.state)
        assertEquals(Said(Saying.PLANNED), FlightStatus.said(f, asked))
        assertEquals("LH455", f.number); assertEquals("Lufthansa", f.airline)
        assertEquals("San Francisco", f.from.city); assertEquals("Frankfurt/Main", f.to.city)
        assertEquals(time("2026-10-03T14:40"), f.from.planned)
        assertEquals(time("2026-10-04T10:25"), f.to.planned)      // 645 minutes later, on Frankfurt's clock
        assertNull(f.from.gate)
        assertEquals(Where(null, null, null), FlightStatus.where(f))
        // A number that does not fly that day.
        assertNull(AirLabs.planned(r.copy(days = setOf(DayOfWeek.MONDAY)), today, LocalDate.of(2026, 10, 3)))
        // A timetable that names no days is taken to fly on all of them.
        assertNotNull(AirLabs.planned(r.copy(days = emptySet()), today, LocalDate.of(2026, 10, 3)))
    }

    @Test fun `the next one in the timetable`() {
        val today = flight("flight-LH455-in-the-air")
        val routes = AirLabs.routes(reply("routes-LH455")).value!!
        // Hours after it landed on Friday: Friday's own, which leaves at 21:40 UTC.
        assertEquals(time("2026-10-02T14:40"), AirLabs.upcoming(routes, today, at("2026-10-02T12:00:00Z"))!!.from.planned)
        // After that one has left: Saturday's.
        assertEquals(time("2026-10-03T14:40"), AirLabs.upcoming(routes, today, at("2026-10-02T21:41:00Z"))!!.from.planned)
        // Only on Mondays: the next Monday.
        val mondays = routes.map { it.copy(days = setOf(DayOfWeek.MONDAY)) }
        assertEquals(time("2026-10-05T14:40"), AirLabs.upcoming(mondays, today, at("2026-10-02T12:00:00Z"))!!.from.planned)
        assertNull(AirLabs.upcoming(emptyList(), today, at("2026-10-02T12:00:00Z")))
    }

    /** A service that answers each of its three questions with a saved reply (or the text given), and counts what it was asked. */
    private class Service(private val replies: Map<String, String>) {
        val asked = ArrayList<String>()
        fun get(url: String): String {
            val what = url.substringAfter("/v9/").substringBefore("?")
            asked.add(what)
            return replies[what] ?: throw AirLabs.Unreachable()
        }
    }

    private val none = """{"response":[]}"""

    @Test fun `one lookup in the usual case`() {
        val s = Service(mapOf("flight" to reply("flight-LH455-in-the-air")))
        val a = AirLabs.lookup(lh455, "k", at("2026-10-02T07:29:00Z"), s::get)
        assertEquals(listOf("flight"), s.asked)
        assertEquals(FlightState.IN_AIR, a.flight!!.state)
        assertNull(a.failure)
    }

    @Test fun `hours after it landed the next one is asked for`() {
        val jl101 = FlightNumber(Airline("JL", "JAL", "Japan Airlines"), 101)
        val landed = reply("flight-JL101-landed-nine-hours-ago")
        val now = at("2026-10-02T07:28:00Z")
        // Among the coming ten hours' flights, with the names of the one that landed.
        val soon = Service(mapOf("flight" to landed, "schedules" to """{"response":[{"flight_iata":"JL101","dep_iata":"HND","arr_iata":"ITM","status":"scheduled",
            "dep_time":"2026-10-03 06:30","dep_time_utc":"2026-10-02 21:30","arr_time":"2026-10-03 07:35","arr_time_utc":"2026-10-02 22:35","dep_terminal":"1","dep_gate":"14"}]}"""))
        val a = AirLabs.lookup(jl101, "k", now, soon::get)
        assertEquals(listOf("flight", "schedules"), soon.asked)
        assertEquals(time("2026-10-03T06:30"), a.flight!!.from.planned)
        assertEquals("Tokyo", a.flight!!.from.city)
        assertEquals("Japan Airlines", a.flight!!.airline)
        assertEquals("14", a.flight!!.from.gate)
        // Not among them: the timetable's next (here another number's lines stand in for it: the flow is what is tried).
        val far = Service(mapOf("flight" to landed, "schedules" to none, "routes" to reply("routes-LH455")))
        val b = AirLabs.lookup(jl101, "k", now, far::get)
        assertEquals(listOf("flight", "schedules", "routes"), far.asked)
        assertTrue(b.flight!!.timetable)
        assertEquals(time("2026-10-02T14:40"), b.flight!!.from.planned)
        // Nobody knows of a next one: the one that landed, as it was.
        val last = Service(mapOf("flight" to landed, "schedules" to none, "routes" to none))
        assertEquals(FlightState.LANDED, AirLabs.lookup(jl101, "k", now, last::get).flight!!.state)
        // Under three hours after the landing it is still the answer, and nothing more is asked.
        val fresh = Service(mapOf("flight" to landed))
        assertEquals(FlightState.LANDED, AirLabs.lookup(jl101, "k", at("2026-10-02T00:00:00Z"), fresh::get).flight!!.state)
        assertEquals(listOf("flight"), fresh.asked)
    }

    @Test fun `a day after the number`() {
        val now = at("2026-10-02T07:29:00Z")
        val both = mapOf("flight" to reply("flight-LH455-in-the-air"), "routes" to reply("routes-LH455"))
        // The day of the flight that is in the air: that flight, one request.
        val same = Service(both)
        assertEquals(FlightState.IN_AIR, AirLabs.lookup(lh455.copy(day = LocalDate.of(2026, 10, 1)), "k", now, same::get).flight!!.state)
        assertEquals(listOf("flight"), same.asked)
        // Another day: the timetable's.
        val other = Service(both)
        val a = AirLabs.lookup(lh455.copy(day = LocalDate.of(2026, 10, 3)), "k", now, other::get)
        assertEquals(listOf("flight", "routes"), other.asked)
        assertTrue(a.flight!!.timetable)
        assertEquals(time("2026-10-03T14:40"), a.flight!!.from.planned)
        assertEquals("San Francisco", a.flight!!.from.city)
        // A day the timetable does not have it on, and a timetable that is not to be had.
        val mondays = Service(mapOf("flight" to reply("flight-LH455-in-the-air"), "routes" to reply("routes-LH455").replace(Regex("\"days\": \\[[^]]*]"), "\"days\": [\"mon\"]")))
        assertEquals(Failure.NOT_THAT_DAY, AirLabs.lookup(lh455.copy(day = LocalDate.of(2026, 10, 3)), "k", now, mondays::get).failure)
        val gone = Service(mapOf("flight" to reply("flight-LH455-in-the-air")))
        assertEquals(Failure.OFFLINE, AirLabs.lookup(lh455.copy(day = LocalDate.of(2026, 10, 3)), "k", now, gone::get).failure)
    }

    @Test fun `a number the service does not know may still be in the timetable`() {
        val now = at("2026-10-02T07:29:00Z")
        val unknown = Service(mapOf("flight" to reply("error-not-found"), "routes" to none))
        val a = AirLabs.lookup(lh455, "k", now, unknown::get)
        assertEquals(Failure.NOT_FOUND, a.failure)
        assertEquals(listOf("flight", "routes"), unknown.asked)
        val weekly = Service(mapOf("flight" to reply("error-not-found"), "routes" to reply("routes-LH455")))
        val b = AirLabs.lookup(lh455, "k", now, weekly::get)
        assertTrue(b.flight!!.timetable)
        assertEquals("Lufthansa", b.flight!!.airline)           // from the table Booklight carries
        assertEquals("SFO", b.flight!!.from.place)               // no city: the three letters
        assertEquals(time("2026-10-02T14:40"), b.flight!!.from.planned)
        // The timetable could not be asked: that is what the row says, not "nothing found" (which would be kept for two minutes).
        val cut = Service(mapOf("flight" to reply("error-not-found")))
        assertEquals(Failure.OFFLINE, AirLabs.lookup(lh455, "k", now, cut::get).failure)
        val spent = Service(mapOf("flight" to reply("error-not-found"), "routes" to """{"error":{"message":"x","code":"month_limit_exceeded"}}"""))
        assertEquals(Failure.USED_UP, AirLabs.lookup(lh455, "k", now, spent::get).failure)
    }

    @Test fun `a clock that is days from UTC is nobody's clock`() {
        val f = AirLabs.flight("""{"response":{"flight_iata":"XX12","dep_iata":"AAA","arr_iata":"BBB","status":"scheduled",
            "dep_time":"2026-10-02 08:00","dep_time_utc":"2026-09-02 06:00","arr_time":"2026-10-02 10:00","arr_time_utc":"2026-10-02 23:30"}}""").value!!
        assertEquals(0, f.from.offset)
        assertEquals(-13 * 60 - 30, f.to.offset)
        // And nothing that is made from it throws.
        FlightStatus.pinned(f, at("2026-10-02T07:00:00Z"))
        FlightStatus.over(f, at("2026-10-02T07:00:00Z"))
    }

    @Test fun `what went wrong is said, and costs one request`() {
        val now = at("2026-10-02T07:29:00Z")
        val refused = Service(mapOf("flight" to reply("error-unknown-key")))
        assertEquals(Failure.REFUSED, AirLabs.lookup(lh455, "k", now, refused::get).failure)
        assertEquals(listOf("flight"), refused.asked)
        assertEquals(Failure.OFFLINE, AirLabs.lookup(lh455, "k", now, Service(emptyMap())::get).failure)
        assertEquals(Failure.NO_ANSWER, AirLabs.lookup(lh455, "k", now) { throw IllegalStateException("x") }.failure)
        assertEquals(Failure.NO_ANSWER, AirLabs.lookup(lh455, "k", now) { "" }.failure)
        assertEquals(Failure.USED_UP, AirLabs.lookup(lh455, "k", now) { """{"request":{"key":{"limits_total":0}},"error":{"message":"x","code":"month_limit_exceeded"}}""" }.failure)
        assertEquals(0, AirLabs.lookup(lh455, "k", now) { """{"request":{"key":{"limits_total":0}},"error":{"message":"x","code":"month_limit_exceeded"}}""" }.left)
    }

    @Test fun `what the pinned window counts`() {
        val air = flight("flight-LH455-in-the-air")               // lands 08:01 UTC
        assertEquals(FlightPin(PinState.IN_AIR, 32), FlightStatus.pinned(air, at("2026-10-02T07:29:00Z")))
        assertEquals(FlightPin(PinState.IN_AIR, 1), FlightStatus.pinned(air, at("2026-10-02T08:00:30Z")))
        assertEquals(FlightPin(PinState.IN_AIR, 0), FlightStatus.pinned(air, at("2026-10-02T09:00:00Z")))
        val planned = flight("flight-LH454-planned")               // leaves 08:25 UTC
        assertEquals(FlightPin(PinState.BEFORE, 56), FlightStatus.pinned(planned, at("2026-10-02T07:29:00Z")))
        assertEquals(FlightPin(PinState.LANDED), FlightStatus.pinned(flight("flight-LH96-landed"), at("2026-10-02T07:29:00Z")))
        assertEquals(FlightPin(PinState.CANCELLED), FlightStatus.pinned(flight("flight-LH1184-cancelled"), at("2026-10-02T07:29:00Z")))
    }

    @Test fun `on time needs a new time that is the plan's`() {
        val planned = flight("flight-LH454-planned")
        assertEquals(Said(Saying.PLANNED), FlightStatus.said(planned, asked))
        val same = planned.copy(from = planned.from.copy(expected = planned.from.planned))
        assertEquals(Said(Saying.ON_TIME), FlightStatus.said(same, asked))
        // Late starts fifteen minutes after the plan (five, until the row had a badge: docs/design/flights-row).
        val fourteen = planned.copy(from = planned.from.copy(expected = planned.from.planned!!.plusMinutes(14)))
        assertEquals(Said(Saying.ON_TIME), FlightStatus.said(fourteen, asked))
        val fifteen = planned.copy(from = planned.from.copy(expected = planned.from.planned!!.plusMinutes(15)))
        assertEquals(Said(Saying.DELAYED, 15), FlightStatus.said(fifteen, asked))
        // In the air with nothing known of the landing but its plan.
        val air = flight("flight-LH455-in-the-air")
        assertEquals(Said(Saying.IN_AIR), FlightStatus.said(air.copy(to = air.to.copy(expected = null)), asked))
        assertEquals(Said(Saying.AIR_ON_TIME), FlightStatus.said(air.copy(to = air.to.copy(expected = air.to.planned)), asked))
        assertEquals(Said(Saying.DIVERTED), FlightStatus.said(air.copy(state = FlightState.DIVERTED), asked))
    }
}
