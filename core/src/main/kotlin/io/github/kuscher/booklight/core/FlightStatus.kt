package io.github.kuscher.booklight.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * One end of a flight: the airport and what is known of the time there. The times are the airport's
 * own wall clock; [offset] is how many minutes that clock is ahead of UTC (−420 for San Francisco in
 * summer), so each of them is a moment too.
 */
data class FlightEnd(
    /** The airport's three letters: SFO. */
    val code: String,
    /** "San Francisco"; empty when the service did not say. */
    val city: String = "",
    val airport: String = "",
    val planned: LocalDateTime? = null,
    val expected: LocalDateTime? = null,
    val actual: LocalDateTime? = null,
    val offset: Int = 0,
    val terminal: String? = null,
    val gate: String? = null,
    /** The baggage belt, at the arrival's end. */
    val belt: String? = null,
) {
    /** The time to show: what happened, else what is expected, else the plan. */
    val time: LocalDateTime? get() = actual ?: expected ?: planned
    /** How many minutes after the plan that is; negative when it is before; null when only the plan is known. */
    val late: Int? get() = (actual ?: expected)?.let { t -> planned?.let { Duration.between(it, t).toMinutes().toInt() } }
    fun moment(t: LocalDateTime): Instant = t.toInstant(ZoneOffset.ofTotalSeconds(offset * 60))
    /** City or, where there is none, the three letters. */
    val place: String get() = city.ifEmpty { code }
}

enum class FlightState { PLANNED, IN_AIR, LANDED, CANCELLED, DIVERTED }

/**
 * One flight on one day, as the service said it. [timetable]: made from the airline's timetable, not
 * from that day's operations: a plan, with no gate, nothing known of delays and no state of its own
 * (whether it has left is what the clock says against its plan). [loose]: a timetable's flight on a
 * day after a clock change at one of its airports; its times are the timetable's, but the moments
 * they stand for may be an hour out, so nothing is made of them that must be right to the hour.
 */
data class Flight(
    /** As a ticket says it: LH455. */
    val number: String,
    val airline: String,
    val from: FlightEnd,
    val to: FlightEnd,
    val state: FlightState,
    /** The number it is flown under when this one is sold by another airline (a codeshare): UA945. */
    val flownAs: String? = null,
    val timetable: Boolean = false,
    val loose: Boolean = false,
) {
    /** It has left the ground (or will not). */
    val left: Boolean get() = state == FlightState.IN_AIR || state == FlightState.LANDED || state == FlightState.DIVERTED || from.actual != null
}

/** What a flight's row says about it in two or three words. The words themselves are the app's, in the user's language. */
enum class Saying { PLANNED, TIMETABLE, ON_TIME, DELAYED, IN_AIR, AIR_ON_TIME, AIR_LATE, AIR_EARLY, LANDED, LANDED_LATE, LANDED_EARLY, DIVERTED, CANCELLED }

/** [minutes]: how late or early, where the saying has a number. */
data class Said(val saying: Saying, val minutes: Int = 0)

/** Where to go: the terminal, and the gate or the belt. Any of them may be unknown; an unknown one is not said. */
data class Where(val terminal: String?, val gate: String?, val belt: String?)

/** What the pinned window shows for a flight: which of its lines, and the minutes to go where it counts down. */
enum class PinState { BEFORE, IN_AIR, LANDED, CANCELLED }
data class FlightPin(val state: PinState, val minutes: Long = 0)

/** How a time's day is said beside it: not at all (it is the user's today), by its weekday (within six days), or by its date. */
enum class DayForm { NONE, WEEKDAY, DATE }

/** The rules for what a flight's row says. */
object FlightStatus {
    /** A plane that leaves or lands within this many minutes of its plan is on time. */
    const val ON_TIME = 5
    /** After this long on the ground a flight is over, and "the next flight" is the one after it. */
    val OVER: Duration = Duration.ofHours(3)

    /**
     * The status in words. "On time" only when the service has given a new time and it is the plan's
     * (within five minutes); with only the plan it is "Planned". A delay is the departure's until the
     * flight has left, and the landing's from then on. A timetable's flight whose time to leave has
     * passed is not "Planned" any more, and nobody knows what became of it: the row says only where
     * its times are from.
     */
    fun said(f: Flight, now: Instant): Said = when (f.state) {
        FlightState.CANCELLED -> Said(Saying.CANCELLED)
        FlightState.DIVERTED -> Said(Saying.DIVERTED)
        FlightState.LANDED -> against(f.to.late, Saying.LANDED_LATE, Saying.LANDED_EARLY, Saying.LANDED, Saying.LANDED)
        FlightState.IN_AIR -> against(f.to.late, Saying.AIR_LATE, Saying.AIR_EARLY, Saying.AIR_ON_TIME, Saying.IN_AIR)
        FlightState.PLANNED -> f.from.late.let {
            if (f.timetable) Said(if (departed(f, now)) Saying.TIMETABLE else Saying.PLANNED)
            else if (it == null) Said(Saying.PLANNED) else if (it >= ON_TIME) Said(Saying.DELAYED, it) else Said(Saying.ON_TIME)
        }
    }

    /** It has left: the service says so, or, for a timetable's flight, its time to leave has passed. */
    fun departed(f: Flight, now: Instant): Boolean = f.left || (f.timetable && passed(f.from, now))

    /** It has landed: the service says so, or, for a timetable's flight, its time to land has passed. */
    fun arrived(f: Flight, now: Instant): Boolean = f.state == FlightState.LANDED || (f.timetable && passed(f.to, now))

    private fun passed(e: FlightEnd, now: Instant): Boolean = e.time?.let { !e.moment(it).isAfter(now) } ?: false

    /** A landing against its plan. */
    private fun against(minutes: Int?, late: Saying, early: Saying, onTime: Saying, unknown: Saying): Said = when {
        minutes == null -> Said(unknown)
        minutes >= ON_TIME -> Said(late, minutes)
        minutes <= -ON_TIME -> Said(early, -minutes)
        else -> Said(onTime)
    }

    /** Where to go: before it leaves, the departure's terminal and gate; after, the arrival's terminal and belt. Nothing for a flight that will not happen. */
    fun where(f: Flight): Where = when {
        f.state == FlightState.CANCELLED || f.state == FlightState.DIVERTED -> Where(null, null, null)
        f.left -> Where(f.to.terminal, null, f.to.belt)
        else -> Where(f.from.terminal, f.from.gate, null)
    }

    /** True if [t], an airport's own time, is on another day than the user's [today]: the row then names the day. */
    fun otherDay(t: LocalDateTime, today: LocalDate): Boolean = t.toLocalDate() != today

    /**
     * How the day of [t] is said: a weekday only within six days of the user's [today], where it can
     * mean one day alone; further off (a flight at Christmas, or a week from today, which has today's
     * weekday) the date.
     */
    fun dayForm(t: LocalDateTime, today: LocalDate): DayForm = when (ChronoUnit.DAYS.between(today, t.toLocalDate())) {
        0L -> DayForm.NONE
        in -6L..6L -> DayForm.WEEKDAY
        else -> DayForm.DATE
    }

    /**
     * True when [f] is over for someone asking at [now]: it landed, or was to land, more than three
     * hours ago (a cancelled one: was to leave). Then the next flight of that number is what was
     * asked for.
     */
    fun over(f: Flight, now: Instant): Boolean {
        val end = if (f.state == FlightState.CANCELLED) f.from.planned?.let(f.from::moment) else f.to.time?.let(f.to::moment)
        return end != null && (f.state == FlightState.LANDED || f.state == FlightState.CANCELLED || f.state == FlightState.DIVERTED) && Duration.between(end, now) > OVER
    }

    /**
     * Which of several flights of one number is "the next": the one in the air now; else one that
     * landed in the last three hours; else the next to leave, or the next that was to leave and is
     * cancelled (the timetable would call that one planned). Null when none of them is any of these.
     */
    fun next(flights: List<Flight>, now: Instant): Flight? {
        fun leaves(f: Flight) = f.from.time?.let(f.from::moment)
        flights.filter { it.state == FlightState.IN_AIR }.maxByOrNull { leaves(it) ?: Instant.MIN }?.let { return it }
        flights.filter { it.state == FlightState.LANDED && !over(it, now) }.maxByOrNull { it.to.time?.let(it.to::moment) ?: Instant.MIN }?.let { return it }
        return flights.filter { (it.state == FlightState.PLANNED || it.state == FlightState.CANCELLED) && (leaves(it) ?: Instant.MIN) > now.minus(OVER) }.minByOrNull { leaves(it) ?: Instant.MAX }
    }

    /**
     * What the pinned window shows at [now]: the time to go until it leaves, then the time to landing,
     * then that it has landed. A flight that was to land more than three hours ago has landed as far
     * as the window is concerned, whatever was last heard of it: a pin that slept through the landing
     * must not stand at "0 min". A timetable's flight goes by its plan alone.
     */
    fun pinned(f: Flight, now: Instant): FlightPin {
        fun until(end: FlightEnd): Long = end.time?.let { (Duration.between(now, end.moment(it)).seconds + 59) / 60 }?.coerceAtLeast(0) ?: 0
        val landing = f.to.time?.let(f.to::moment)
        return when {
            f.state == FlightState.CANCELLED -> FlightPin(PinState.CANCELLED)
            arrived(f, now) || (landing != null && Duration.between(landing, now) > OVER) -> FlightPin(PinState.LANDED)
            departed(f, now) -> FlightPin(PinState.IN_AIR, until(f.to))
            else -> FlightPin(PinState.BEFORE, until(f.from))
        }
    }

    /** A pinned flight is asked for again this often once it is near: from three hours before it leaves until it has landed. */
    val NEAR: Duration = Duration.ofMinutes(30)
    /** And this often while it is further off. */
    val FAR: Duration = Duration.ofHours(3)
    /** The service knows a flight's day about this long before it leaves (its `schedules` reach ten hours ahead): before that a timetable's plan cannot change. */
    val KNOWN: Duration = Duration.ofHours(10)

    /**
     * How long a pinned flight waits between two asks at [now]; null when there is nothing to ask:
     * it has landed (or was to, more than three hours ago), it is cancelled, or it is a plan from the
     * timetable more than ten hours off, which the service could only repeat.
     */
    fun every(f: Flight, now: Instant): Duration? {
        val pin = pinned(f, now)
        return when (pin.state) {
            PinState.LANDED, PinState.CANCELLED -> null
            PinState.IN_AIR -> NEAR
            PinState.BEFORE -> when {
                f.timetable && pin.minutes > KNOWN.toMinutes() -> null
                pin.minutes > FAR.toMinutes() -> FAR
                else -> NEAR
            }
        }
    }

    /** True if [got] is the flight [was] is, heard of again: it starts at the same airport on the same planned day. Another day's flight of the number is another flight. */
    fun same(was: Flight, got: Flight): Boolean =
        was.from.code == got.from.code && was.from.planned != null && was.from.planned.toLocalDate() == got.from.planned?.toLocalDate()
}
