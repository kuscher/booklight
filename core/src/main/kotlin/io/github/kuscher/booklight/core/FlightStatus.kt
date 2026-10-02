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
    /** The aircraft's type, where the service names it: "Boeing 747-8". It comes with the position, so mostly once it has left. */
    val aircraft: String? = null,
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

/** Where a flight is in its life for someone who looks at it now: what the row's headline is about. */
enum class FlightPhase {
    /** Still to leave, and more than three hours off: the row says when. */
    AHEAD,
    /** It leaves within three hours: the row counts down to it. */
    SOON,
    IN_AIR,
    LANDED,
    CANCELLED,
    DIVERTED,
    /** A timetable's flight whose time to leave has passed: nobody knows what became of it. */
    TIMETABLE,
}

/** The sentence a flight's row has as its headline. The words are the app's, in the user's language. */
enum class Heading { LEAVES_AT, LEAVES_IN, LANDS_IN, IN_AIR, LANDED_AGO, LANDED_NOW, LANDED, CANCELLED, DIVERTED, TIMETABLE }

/** [minutes]: to go ([Heading.LEAVES_IN], [Heading.LANDS_IN]) or since ([Heading.LANDED_AGO]), where the sentence has a number. */
data class Headline(val heading: Heading, val minutes: Long = 0)

/** Whether a flight runs to plan: what the one badge beside the headline says. */
enum class Verdict {
    /** Only the plan is known. */
    PLANNED,
    ON_TIME,
    /** It will leave, or land, later than planned. */
    DELAYED,
    /** It landed later than planned. */
    LATE,
    EARLY,
}

/** [minutes]: how late or early, where the verdict has a number. */
data class Badge(val verdict: Verdict, val minutes: Int = 0) {
    /** Good news, a delay, or neither: the badge's colour. */
    val tone: Tone get() = when (verdict) {
        Verdict.ON_TIME, Verdict.EARLY -> Tone.GOOD
        Verdict.DELAYED, Verdict.LATE -> Tone.LATE
        Verdict.PLANNED -> Tone.PLAIN
    }
}

/**
 * What stands under one end of a flight's line: the airport, its time ([struck]: it will not happen), and
 * what matters at that end now. Before the flight leaves that is where to go at the start; after, where
 * to meet it at the far end, and at the start the aircraft. Whatever the service did not name is null,
 * and is then simply not said.
 */
data class EndSays(
    val code: String, val time: LocalDateTime?, val struck: Boolean = false,
    val terminal: String? = null, val gate: String? = null, val belt: String? = null, val aircraft: String? = null,
)

/**
 * What a flight's row shows at one moment. [share]: how much of the flying time has passed, 0 to 1,
 * which is where the plane stands on the line; null when nobody knows where it is, and the line has
 * no plane.
 */
data class FlightRow(val phase: FlightPhase, val headline: Headline, val badge: Badge?, val share: Double?, val from: EndSays, val to: EndSays) {
    /** The headline has minutes in it, which the clock changes: the row is said anew each minute while it is looked at. */
    val counts: Boolean get() = when (headline.heading) {
        Heading.LEAVES_IN, Heading.LANDS_IN, Heading.LANDED_AGO, Heading.LANDED_NOW -> true
        else -> false
    }
}

/** The rules for what a flight's row says. */
object FlightStatus {
    /**
     * A plane that leaves or lands within this many minutes of its plan is on time: what airlines call
     * on time. (It was 5 until the row had a badge: with a colour on it, "Delayed 6 min" is alarm for
     * nothing, and the exact time stands under the line either way.)
     */
    const val ON_TIME = 15
    /** After this long on the ground a flight is over, and "the next flight" is the one after it. */
    val OVER: Duration = Duration.ofHours(3)

    /**
     * The status in words, for the line that is copied. "On time" only when the service has given a
     * new time and it is the plan's (within [ON_TIME] minutes); with only the plan it is "Planned". A
     * delay is the departure's until the flight has left, and the landing's from then on. A
     * timetable's flight whose time to leave has passed is not "Planned" any more, and nobody knows
     * what became of it: the line says only where its times are from.
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

    /** From this long before it leaves, the row counts down to a flight. */
    val SOON: Duration = Duration.ofHours(3)
    /** In the air the plane is never nearer an end of its line than this: it reaches the far end only when the service says "landed". */
    const val EDGE = 0.02

    /** Whole minutes from [now] until [then], rounded up: a minute that has begun is still to go. Negative once it has passed. */
    private fun until(now: Instant, then: Instant): Long = Math.floorDiv(Duration.between(now, then).seconds + 59, 60L)

    /**
     * Which phase [f] is in at [now]. What the service says, not the clock, decides whether it has
     * left and landed; only a timetable's flight goes by its plan, and once its time to leave has
     * passed nobody knows more. A plan whose clocks may be an hour out ([Flight.loose]) is never
     * counted down to.
     */
    fun phase(f: Flight, now: Instant): FlightPhase = when (f.state) {
        FlightState.CANCELLED -> FlightPhase.CANCELLED
        FlightState.DIVERTED -> FlightPhase.DIVERTED
        FlightState.LANDED -> FlightPhase.LANDED
        FlightState.IN_AIR -> FlightPhase.IN_AIR
        FlightState.PLANNED -> {
            val toGo = f.from.time?.let { until(now, f.from.moment(it)) }
            if (f.timetable && departed(f, now)) FlightPhase.TIMETABLE
            else if (toGo != null && toGo in 1..SOON.toMinutes() && !f.loose) FlightPhase.SOON
            else FlightPhase.AHEAD
        }
    }

    /**
     * The row's headline: the one thing needed in that phase. When it leaves; within three hours of
     * that, how long until it does; in the air, how long until it lands (never less than a minute: it
     * has landed when the service says so); for three hours after, how long ago it landed.
     */
    fun headline(f: Flight, now: Instant): Headline = when (phase(f, now)) {
        FlightPhase.CANCELLED -> Headline(Heading.CANCELLED)
        FlightPhase.DIVERTED -> Headline(Heading.DIVERTED)
        FlightPhase.TIMETABLE -> Headline(Heading.TIMETABLE)
        FlightPhase.AHEAD -> Headline(Heading.LEAVES_AT)
        FlightPhase.SOON -> Headline(Heading.LEAVES_IN, until(now, f.from.moment(f.from.time!!)))
        FlightPhase.IN_AIR -> f.to.time?.let { Headline(Heading.LANDS_IN, until(now, f.to.moment(it)).coerceAtLeast(1)) } ?: Headline(Heading.IN_AIR)
        FlightPhase.LANDED -> {
            val ago = f.to.time?.let { Duration.between(f.to.moment(it), now) }
            when {
                ago == null || ago > OVER -> Headline(Heading.LANDED)
                ago.seconds < 60 -> Headline(Heading.LANDED_NOW)
                else -> Headline(Heading.LANDED_AGO, ago.toMinutes())
            }
        }
    }

    /**
     * The badge: whether it runs to plan. The departure's delay until the flight has left, the
     * landing's from then on. Late, and early, start [ON_TIME] minutes from the plan. "On time" is a
     * claim, made only when the service has sent a time of its own: before leaving the plan alone is
     * "Planned", in the air and after landing it is no badge at all. None either where the headline
     * has said it all: cancelled, diverted, a timetable's flight past its time, and a flight nobody
     * names a time to leave for (its headline can only say "Planned").
     */
    fun badge(f: Flight, now: Instant): Badge? = when (phase(f, now)) {
        FlightPhase.CANCELLED, FlightPhase.DIVERTED, FlightPhase.TIMETABLE -> null
        FlightPhase.AHEAD, FlightPhase.SOON -> f.from.late.let {
            if (f.from.time == null) null
            else if (it == null || f.timetable) Badge(Verdict.PLANNED) else if (it >= ON_TIME) Badge(Verdict.DELAYED, it) else Badge(Verdict.ON_TIME)
        }
        FlightPhase.IN_AIR -> verdict(f.to.late, Verdict.DELAYED)
        FlightPhase.LANDED -> verdict(f.to.late, Verdict.LATE)
    }

    private fun verdict(minutes: Int?, late: Verdict): Badge? = when {
        minutes == null -> null
        minutes >= ON_TIME -> Badge(late, minutes)
        minutes <= -ON_TIME -> Badge(Verdict.EARLY, -minutes)
        else -> Badge(Verdict.ON_TIME)
    }

    /**
     * How much of the flight is behind it at [now], 0 to 1: where the plane stands on the row's line.
     * From time alone: the minutes since it left against the minutes from then to its landing (the
     * actual time, else the expected, else the plan), each as a moment, so time zones do not matter.
     * Never from the service's own percentage (it is a share of the planned duration, and starts some
     * way along) nor from distance. 0 until the service says it has left; in the air between [EDGE]
     * and 1 − [EDGE], however the clock stands; 1 once it has landed.
     *
     * Null where a plane on the line would claim what nobody knows: a flight that is cancelled or
     * diverted, a timetable's flight past its time to leave, and one in the air with no time of
     * landing to reckon with.
     */
    fun share(f: Flight, now: Instant): Double? = when (phase(f, now)) {
        FlightPhase.CANCELLED, FlightPhase.DIVERTED, FlightPhase.TIMETABLE -> null
        FlightPhase.AHEAD, FlightPhase.SOON -> 0.0
        FlightPhase.LANDED -> 1.0
        FlightPhase.IN_AIR -> {
            val left = f.from.time?.let(f.from::moment)
            val lands = f.to.time?.let(f.to::moment)
            if (left == null || lands == null || !lands.isAfter(left)) null
            else (Duration.between(left, now).seconds.toDouble() / Duration.between(left, lands).seconds).coerceIn(EDGE, 1 - EDGE)
        }
    }

    /**
     * The plane only goes forward. [shown]: where it was last drawn for this flight; [share]: where the
     * times put it now. A later estimate lengthens a flight, and its share drops: the plane then waits
     * where it is until the clock has caught up. Null (no plane) is taken as it comes.
     */
    fun forward(shown: Double?, share: Double?): Double? = if (shown == null || share == null) share else maxOf(shown, share)

    /**
     * What stands under the two ends of the line. Each airport with its time (what happened, else what
     * is expected, else the plan). Before it leaves: gate and terminal at the start, nothing at the far
     * end but its time. Once it has left: the aircraft at the start; at the far end the terminal and
     * the belt, or the gate while no belt is named. A flight that will not happen has its times struck
     * (a cancelled one both, at their plan; a diverted one the landing's) and nothing else is said.
     */
    private fun ends(f: Flight, phase: FlightPhase): Pair<EndSays, EndSays> = when (phase) {
        FlightPhase.CANCELLED -> EndSays(f.from.code, f.from.planned ?: f.from.time, struck = true) to EndSays(f.to.code, f.to.planned ?: f.to.time, struck = true)
        FlightPhase.DIVERTED -> EndSays(f.from.code, f.from.time) to EndSays(f.to.code, f.to.time, struck = true)
        FlightPhase.TIMETABLE -> EndSays(f.from.code, f.from.time) to EndSays(f.to.code, f.to.time)
        FlightPhase.AHEAD, FlightPhase.SOON -> EndSays(f.from.code, f.from.time, terminal = f.from.terminal, gate = f.from.gate) to EndSays(f.to.code, f.to.time)
        FlightPhase.IN_AIR, FlightPhase.LANDED ->
            EndSays(f.from.code, f.from.time, aircraft = f.aircraft) to EndSays(f.to.code, f.to.time, terminal = f.to.terminal, gate = f.to.gate.takeIf { f.to.belt == null }, belt = f.to.belt)
    }

    /** Everything a flight's row shows at [now]: its phase, the headline, the badge, where the plane is and what stands under the line's ends. */
    fun row(f: Flight, now: Instant): FlightRow {
        val phase = phase(f, now)
        val (from, to) = ends(f, phase)
        return FlightRow(phase, headline(f, now), badge(f, now), share(f, now), from, to)
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

    /**
     * Where the plane stands in the pinned window at [now]: as in the row, moving by the clock between
     * two answers. A flight the window takes for landed though nobody said so (it was to land more
     * than three hours ago) is at the line's end. No plane where the row has none.
     */
    fun pinShare(f: Flight, now: Instant): Double? = share(f, now)?.let { if (pinned(f, now).state == PinState.LANDED) 1.0 else it }

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
