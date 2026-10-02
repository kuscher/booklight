package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The rules the code review of flights asked for (October 2026), each with the case that was wrong.
 * The replies are the saved ones of `resources/airlabs`; where a case needs a reply nobody saved (a
 * cancelled row in `schedules`, a timetable with two lines, an airport far from UTC), it is written
 * here in the service's own form.
 */
class FlightRulesTest {
    private fun reply(name: String): String = javaClass.getResource("/airlabs/$name.json")!!.readText()
    private fun flight(name: String): Flight = AirLabs.flight(reply(name)).value!!
    private fun at(text: String): Instant = Instant.parse(text)
    private fun time(text: String): LocalDateTime = LocalDateTime.parse(text)
    private val lh455 = FlightNumber(Airline("LH", "DLH", "Lufthansa"), 455)
    private val none = """{"response":[]}"""

    /** A saved reply with every date in it moved on by [days]: the same flight on another day. */
    private fun later(reply: String, days: Long): String =
        Regex("\\d{4}-\\d{2}-\\d{2}").replace(reply) { LocalDate.parse(it.value).plusDays(days).toString() }

    private class Service(private val replies: Map<String, String>) {
        val asked = ArrayList<String>()
        fun get(url: String): String {
            val what = url.substringAfter("/v9/").substringBefore("?")
            asked.add(what)
            return replies[what] ?: throw AirLabs.Unreachable()
        }
    }

    private fun line(from: String, to: String, leaves: String, utc: String, lands: String, landsUtc: String, minutes: Int, days: String) =
        """{"flight_iata":"XX12","dep_iata":"$from","arr_iata":"$to","dep_time":"$leaves","dep_time_utc":"$utc","arr_time":"$lands","arr_time_utc":"$landsUtc","duration":$minutes,"days":[$days]}"""
    private fun lines(vararg l: String) = AirLabs.routes("""{"response":[${l.joinToString(",")}]}""").value!!
    private val daily = "\"mon\",\"tue\",\"wed\",\"thu\",\"fri\",\"sat\",\"sun\""

    // ---- the next flight

    @Test fun `a cancelled next flight is cancelled, not the timetable's plan`() {
        val jl101 = FlightNumber(Airline("JL", "JAL", "Japan Airlines"), 101)
        val cancelled = """{"response":[{"flight_iata":"JL101","dep_iata":"HND","arr_iata":"ITM","status":"cancelled",
            "dep_time":"2026-10-03 06:30","dep_time_utc":"2026-10-02 21:30","arr_time":"2026-10-03 07:35","arr_time_utc":"2026-10-02 22:35"}]}"""
        // The last one landed hours ago, the next is cancelled, and the timetable knows of a flight every day.
        val s = Service(mapOf("flight" to reply("flight-JL101-landed-nine-hours-ago"), "schedules" to cancelled,
            "routes" to """{"response":[${line("HND", "ITM", "06:30", "21:30", "07:35", "22:35", 65, daily)}]}"""))
        val a = AirLabs.lookup(jl101, "k", at("2026-10-02T12:00:00Z"), s::get)
        assertEquals(listOf("flight", "schedules"), s.asked)
        assertEquals(FlightState.CANCELLED, a.flight!!.state)
        assertFalse(a.flight!!.timetable)
        assertEquals(time("2026-10-03T06:30"), a.flight!!.from.planned)
        assertEquals("Tokyo", a.flight!!.from.city)
        assertEquals(Said(Saying.CANCELLED), FlightStatus.said(a.flight!!, at("2026-10-02T12:00:00Z")))
        // Three hours after it was to leave it is over, and the one after it is the next.
        assertNull(FlightStatus.next(AirLabs.schedules(cancelled).value!!, at("2026-10-03T01:00:00Z")))
    }

    @Test fun `the timetable's next flight is the soonest of its lines, not the first line's`() {
        val like = flight("flight-LH455-in-the-air")
        val both = lines(
            line("SFO", "FRA", "14:40", "21:40", "10:25", "08:25", 645, "\"mon\",\"tue\",\"wed\",\"thu\",\"fri\""),
            line("SFO", "FRA", "15:10", "22:10", "10:55", "08:55", 645, "\"sat\",\"sun\""),
        )
        // Saturday morning in San Francisco: today's 15:10, not Monday's 14:40.
        assertEquals(time("2026-10-03T15:10"), AirLabs.upcoming(both, like, at("2026-10-03T12:00:00Z"))!!.from.planned)
        assertEquals(time("2026-10-03T15:10"), AirLabs.upcoming(both.reversed(), like, at("2026-10-03T12:00:00Z"))!!.from.planned)
        // On Sunday evening, after the 15:10 has gone: Monday's.
        assertEquals(time("2026-10-05T14:40"), AirLabs.upcoming(both, like, at("2026-10-04T23:00:00Z"))!!.from.planned)
        // A number with two legs stays on the leg the last flight was on, though the other leaves sooner.
        val legs = lines(
            line("FRA", "JFK", "08:35", "06:35", "11:10", "15:10", 515, daily),
            line("SFO", "FRA", "14:40", "21:40", "10:25", "08:25", 645, daily),
        )
        assertEquals("SFO", AirLabs.upcoming(legs, like, at("2026-10-03T00:00:00Z"))!!.from.code)
        // With no leg of its own in the timetable: the soonest of the others.
        assertEquals("FRA", AirLabs.upcoming(legs.take(1), like, at("2026-10-03T00:00:00Z"))!!.from.code)
    }

    // ---- clocks

    @Test fun `a clock far from UTC is read the same at every hour of the day`() {
        // Honolulu is ten hours behind UTC. Said without a day, 22:00 there and 08:00 UTC are fourteen hours apart the other way.
        val hawaii = lines(
            line("HNL", "LAX", "22:00", "08:00", "06:30", "13:30", 330, daily),
            line("HNL", "LAX", "08:00", "18:00", "16:30", "23:30", 330, daily),
        )
        assertEquals(listOf(-600, -600), hawaii.map { it.fromOffset })
        assertEquals(listOf(-420, -420), hawaii.map { it.toOffset })
        // Auckland in summer is thirteen ahead; Fiji twelve.
        val south = lines(
            line("AKL", "SYD", "07:00", "18:00", "08:35", "21:35", 215, daily),
            line("AKL", "SYD", "19:00", "06:00", "20:35", "09:35", 215, daily),
            line("NAN", "AKL", "09:00", "21:00", "13:00", "00:00", 180, daily),
        )
        assertEquals(listOf(780, 780, 720), south.map { it.fromOffset })
        assertEquals(listOf(660, 660, 780), south.map { it.toOffset })
        // The late flight from Honolulu leaves on Friday and lands on Saturday, after it left.
        val stub = Flight("XX12", "", FlightEnd(""), FlightEnd(""), FlightState.PLANNED)
        val f = AirLabs.planned(hawaii[0], stub, LocalDate.of(2026, 10, 2))!!
        assertEquals(time("2026-10-02T22:00"), f.from.planned)
        assertEquals(time("2026-10-03T06:30"), f.to.planned)
        assertEquals(at("2026-10-03T08:00:00Z"), f.from.moment(f.from.planned!!))
        // At two in the morning on Friday in Honolulu tonight's flight is still to come.
        assertEquals(time("2026-10-02T22:00"), AirLabs.upcoming(hawaii.take(1), stub, at("2026-10-02T12:00:00Z"))!!.from.planned)
        // Ordinary clocks are read as before.
        val usual = AirLabs.routes(reply("routes-LH455")).value!![0]
        assertEquals(-420, usual.fromOffset); assertEquals(120, usual.toOffset)
    }

    @Test fun `an airport's clock from a dated reply wins over the timetable's guess`() {
        // Pago Pago is eleven behind, which the timetable alone reads as thirteen ahead.
        val line = lines(line("PPG", "HNL", "23:20", "10:20", "05:50", "15:50", 330, daily))[0]
        assertEquals(780, line.fromOffset)
        val like = Flight("XX12", "Some Air", FlightEnd("PPG", "Pago Pago", planned = time("2026-10-01T23:20"), offset = -660), FlightEnd("HNL", "Honolulu", planned = time("2026-10-02T05:50"), offset = -600), FlightState.LANDED)
        val f = AirLabs.planned(line, like, LocalDate.of(2026, 10, 5))!!
        assertEquals(-660, f.from.offset)
        assertEquals(time("2026-10-06T05:50"), f.to.planned)
        assertEquals("Pago Pago", f.from.city)
    }

    @Test fun `a timetable's flight after a clock change is marked, and one before it is not`() {
        val like = flight("flight-LH455-in-the-air")
        val route = AirLabs.routes(reply("routes-LH455")).value!![0]
        val now = at("2026-10-02T07:29:00Z")
        // Europe's clocks go back on 25 October 2026, America's on 1 November.
        assertTrue(AirLabs.steady(AirLabs.planned(route, like, LocalDate.of(2026, 10, 3))!!, now))
        assertTrue(AirLabs.steady(AirLabs.planned(route, like, LocalDate.of(2026, 10, 23))!!, now))
        assertFalse(AirLabs.steady(AirLabs.planned(route, like, LocalDate.of(2026, 10, 28))!!, now))      // Frankfurt's has changed
        assertFalse(AirLabs.steady(AirLabs.planned(route, like, LocalDate.of(2026, 11, 5))!!, now))       // both have
        // The lookup says so on the flight.
        fun asked(day: LocalDate): Flight {
            val s = Service(mapOf("flight" to reply("flight-LH455-in-the-air"), "routes" to reply("routes-LH455")))
            return AirLabs.lookup(lh455.copy(day = day), "k", now, s::get).flight!!
        }
        assertFalse(asked(LocalDate.of(2026, 10, 3)).loose)
        assertTrue(asked(LocalDate.of(2026, 10, 28)).loose)
        // A flight from that day's operations is never loose: its clocks came with its date.
        assertFalse(like.loose)
        assertTrue(AirLabs.steady(like, now))
    }

    // ---- what the row says

    @Test fun `a timetable's flight whose time has passed is not planned any more`() {
        val like = flight("flight-LH455-in-the-air")
        val route = AirLabs.routes(reply("routes-LH455")).value!![0]
        val f = AirLabs.planned(route, like, LocalDate.of(2026, 10, 3))!!          // leaves 21:40 UTC, lands 08:25 UTC the day after
        val before = at("2026-10-03T12:00:00Z")
        val during = at("2026-10-04T00:00:00Z")
        val after = at("2026-10-04T09:00:00Z")
        assertEquals(Said(Saying.PLANNED), FlightStatus.said(f, before))
        assertEquals(Said(Saying.TIMETABLE), FlightStatus.said(f, during))
        assertEquals(Said(Saying.TIMETABLE), FlightStatus.said(f, after))
        assertEquals(listOf(false, true, true), listOf(before, during, after).map { FlightStatus.departed(f, it) })
        assertEquals(listOf(false, false, true), listOf(before, during, after).map { FlightStatus.arrived(f, it) })
        // A flight the service knows goes by what the service says, not by the clock.
        val planned = flight("flight-LH454-planned")
        assertFalse(FlightStatus.departed(planned, at("2026-10-05T00:00:00Z")))
        assertEquals(Said(Saying.PLANNED), FlightStatus.said(planned, at("2026-10-05T00:00:00Z")))
        // Through the lookup: a day that has passed, asked for by its date (as the pinned window asks).
        val s = Service(mapOf("flight" to reply("flight-LH455-in-the-air"), "routes" to reply("routes-LH455")))
        val old = AirLabs.lookup(lh455.copy(day = LocalDate.of(2026, 9, 28)), "k", at("2026-10-02T07:29:00Z"), s::get).flight!!
        assertEquals(Said(Saying.TIMETABLE), FlightStatus.said(old, at("2026-10-02T07:29:00Z")))
        assertEquals(FlightPin(PinState.LANDED), FlightStatus.pinned(old, at("2026-10-02T07:29:00Z")))
    }

    @Test fun `a day is a weekday within six days and a date beyond`() {
        val today = LocalDate.of(2026, 10, 1)
        fun form(t: String) = FlightStatus.dayForm(time(t), today)
        assertEquals(DayForm.NONE, form("2026-10-01T14:40"))
        assertEquals(DayForm.WEEKDAY, form("2026-10-02T10:25"))
        assertEquals(DayForm.WEEKDAY, form("2026-10-07T10:25"))
        assertEquals(DayForm.DATE, form("2026-10-08T14:40"))            // a week from today has today's weekday
        assertEquals(DayForm.DATE, form("2026-12-24T14:40"))
        assertEquals(DayForm.WEEKDAY, form("2026-09-30T23:10"))         // it left yesterday
        assertEquals(DayForm.DATE, form("2026-09-20T23:10"))
    }

    @Test fun `how long an answer is kept`() {
        assertEquals(Duration.ofMinutes(2), AirLabs.keep(null))
        assertEquals(Duration.ofSeconds(10), AirLabs.keep(Failure.OFFLINE))
        assertEquals(Duration.ofSeconds(10), AirLabs.keep(Failure.NO_ANSWER))
        // A number nobody flies costs two lookups to find out: it is not asked again for an hour.
        assertEquals(Duration.ofHours(1), AirLabs.keep(Failure.NOT_FOUND))
        assertEquals(Duration.ofHours(1), AirLabs.keep(Failure.NOT_THAT_DAY))
        assertEquals(Duration.ofMinutes(2), AirLabs.keep(Failure.REFUSED))
        assertEquals(Duration.ofMinutes(2), AirLabs.keep(Failure.USED_UP))
    }

    // ---- the pinned flight

    @Test fun `a pin that slept through the landing says landed and asks nothing`() {
        val air = flight("flight-LH455-in-the-air")                              // expected to land 08:01 UTC
        // Still in the air, and for three hours after the landing nobody saw: it counts down to nothing and goes on asking.
        assertEquals(FlightPin(PinState.IN_AIR, 0), FlightStatus.pinned(air, at("2026-10-02T11:00:00Z")))
        assertEquals(FlightStatus.NEAR, FlightStatus.every(air, at("2026-10-02T11:00:00Z")))
        // The lid opens at nine in the morning in San Francisco: landed, at its last time, and nothing more to ask.
        assertEquals(FlightPin(PinState.LANDED), FlightStatus.pinned(air, at("2026-10-02T16:00:00Z")))
        assertNull(FlightStatus.every(air, at("2026-10-02T16:00:00Z")))
        // The same for one that was pinned before it left and never heard of again.
        val planned = flight("flight-LH454-planned")                             // to land 19:40 UTC
        assertEquals(FlightPin(PinState.BEFORE, 0), FlightStatus.pinned(planned, at("2026-10-02T12:00:00Z")))
        assertEquals(FlightPin(PinState.LANDED), FlightStatus.pinned(planned, at("2026-10-02T22:41:00Z")))
        assertNull(FlightStatus.every(planned, at("2026-10-02T22:41:00Z")))
        assertNull(FlightStatus.every(flight("flight-LH96-landed"), at("2026-10-02T07:29:00Z")))
        assertNull(FlightStatus.every(flight("flight-LH1184-cancelled"), at("2026-10-02T07:29:00Z")))
    }

    @Test fun `asking again is one request about the same flight, and another day's flight ends it`() {
        val air = flight("flight-LH455-in-the-air")
        // The same flight, landed now.
        val landed = Service(mapOf("flight" to reply("flight-LH455-landed")))
        val a = AirLabs.again(lh455, "k", air, landed::get).again
        assertTrue(a is AirLabs.Again.Is && a.flight.state == FlightState.LANDED)
        assertEquals(listOf("flight"), landed.asked)
        // The service has gone on to the next day's flight: that is not this pin's flight.
        val next = Service(mapOf("flight" to later(reply("flight-LH455-in-the-air"), 1), "schedules" to none, "routes" to reply("routes-LH455")))
        assertEquals(AirLabs.Again.Gone, AirLabs.again(lh455, "k", air, next::get).again)
        assertEquals(listOf("flight"), next.asked)
        // It knows the number no more, or refuses the key: nothing more to ask. No connection: try again.
        assertEquals(AirLabs.Again.Gone, AirLabs.again(lh455, "k", air) { reply("error-not-found") }.again)
        assertEquals(AirLabs.Again.Gone, AirLabs.again(lh455, "k", air) { reply("error-unknown-key") }.again)
        assertEquals(AirLabs.Again.Failed, AirLabs.again(lh455, "k", air, Service(emptyMap())::get).again)
        assertEquals(AirLabs.Again.Failed, AirLabs.again(lh455, "k", air) { "<html>" }.again)
        assertEquals(990, AirLabs.again(lh455, "k", air) { """{"request":{"key":{"limits_total":990}},"error":{"message":"x","code":"not_found"}}""" }.left)
        // Same day, same airport is the same flight; a day on is not; nor is another airport on the same day.
        assertTrue(FlightStatus.same(air, flight("flight-LH455-landed")))
        assertFalse(FlightStatus.same(air, AirLabs.flight(later(reply("flight-LH455-in-the-air"), 1)).value!!))
        assertFalse(FlightStatus.same(air, air.copy(from = air.from.copy(code = "LAX"))))
    }

    @Test fun `a pinned plan from the timetable waits for the service to know its day`() {
        val like = flight("flight-LH455-in-the-air")                              // left on 1 October
        val route = AirLabs.routes(reply("routes-LH455")).value!![0]
        val plan = AirLabs.planned(route, like, LocalDate.of(2026, 10, 5))!!      // leaves 5 October 21:40 UTC
        // Days off: nothing to ask. From ten hours before: every three hours. From three hours before, and in the air: every half hour.
        assertNull(FlightStatus.every(plan, at("2026-10-02T07:29:00Z")))
        assertNull(FlightStatus.every(plan, at("2026-10-05T11:39:00Z")))
        assertEquals(FlightStatus.FAR, FlightStatus.every(plan, at("2026-10-05T11:40:00Z")))
        assertEquals(FlightStatus.FAR, FlightStatus.every(plan, at("2026-10-05T18:39:00Z")))
        assertEquals(FlightStatus.NEAR, FlightStatus.every(plan, at("2026-10-05T18:40:00Z")))
        assertEquals(FlightStatus.NEAR, FlightStatus.every(plan, at("2026-10-06T02:00:00Z")))
        assertNull(FlightStatus.every(plan, at("2026-10-06T08:25:00Z")))          // its time to land: the plan is over
        // A flight the service knows is asked about however far off it is, but only every three hours until it is near.
        val known = flight("flight-LH454-planned")                                // leaves 2 October 08:25 UTC
        assertEquals(FlightStatus.FAR, FlightStatus.every(known, at("2026-10-01T18:00:00Z")))
        assertEquals(FlightStatus.NEAR, FlightStatus.every(known, at("2026-10-02T07:29:00Z")))
        // The service still answers with the flight before it: not yet. With its own day: that flight. With a later one: gone.
        assertEquals(AirLabs.Again.NotYet, AirLabs.again(lh455, "k", plan) { reply("flight-LH455-in-the-air") }.again)
        assertEquals(AirLabs.Again.NotYet, AirLabs.again(lh455, "k", plan) { reply("error-not-found") }.again)
        val day = AirLabs.again(lh455, "k", plan) { later(reply("flight-LH455-in-the-air"), 4) }.again
        assertTrue(day is AirLabs.Again.Is && !day.flight.timetable && day.flight.from.gate == "G13")
        assertEquals(AirLabs.Again.Gone, AirLabs.again(lh455, "k", plan) { later(reply("flight-LH455-in-the-air"), 5) }.again)
    }

    @Test fun `how often the pin asks in the usual case`() {
        // Pinned an hour before it leaves, an eleven-hour flight: every half hour, about twenty-four asks of one request each.
        val air = flight("flight-LH455-in-the-air")                               // left 21:47 UTC, lands 08:01 UTC
        var asks = 0
        var t = at("2026-10-01T20:40:00Z")
        var last: Instant? = null
        while (t.isBefore(at("2026-10-02T20:00:00Z"))) {
            val gap = FlightStatus.every(air, t)
            if (gap != null && (last == null || Duration.between(last, t) >= gap)) { asks++; last = t }
            t = t.plusSeconds(60)
        }
        // (The saved reply stays "in the air" here, so the asks run on for the three hours after its landing time.)
        assertTrue("asks: $asks", asks in 20..30)
    }
}
