package io.github.kuscher.booklight

import android.content.Context
import io.github.kuscher.booklight.core.AirLabs
import io.github.kuscher.booklight.core.Failure
import io.github.kuscher.booklight.core.Flight
import io.github.kuscher.booklight.core.FlightEnd
import io.github.kuscher.booklight.core.FlightState
import java.time.Instant
import java.time.LocalDateTime

/**
 * Flights for looking at a flight's row in each of its phases without waiting for a real one to be in
 * it (`./bl debug flight show NAME`, `./bl debug flight pin NAME`; debug builds). Each comes with the
 * moment it is looked at, so its phase is what its name says. Nothing of this is asked of the
 * flight service, and a sample's row says so in the footer ("sample air-late" where a real answer
 * says "AirLabs · 3:55 PM").
 *
 * Two kinds. The built-in ones are the example of the design (docs/design/flights-row): LH 400,
 * Frankfurt to New York, planned 10:55 to 13:35 on Friday 2 October 2026, with made-up times. The
 * others are the flight service's own replies as they were saved for the core's tests
 * (`core/src/test/resources/airlabs/flight-*.json`, which a debug build carries as assets), looked
 * at when they were asked for: `LH455-in-the-air`, `LH1184-cancelled`…
 */
object FlightSamples {
    /** [number]: what is typed for it. [flight] and [failure] both null: the row stands waiting. [now]: the moment it is looked at. */
    class Sample(val number: String, val flight: Flight?, val failure: Failure?, val now: Instant?)

    private const val NUMBER = "LH400"
    /** When the saved replies were asked for. */
    private val ASKED: Instant = Instant.parse("2026-10-02T07:29:00Z")

    private fun at(clock: String?): LocalDateTime? = clock?.let { LocalDateTime.parse("2026-10-02T$it") }
    private fun utc(time: String): Instant = Instant.parse("${time}Z")
    /** Frankfurt, two hours ahead of UTC; New York, four behind. */
    private fun fra(expected: String? = null, actual: String? = null, terminal: String? = null, gate: String? = null) =
        FlightEnd("FRA", "Frankfurt/Main", "Frankfurt Airport", at("10:55"), at(expected), at(actual), 120, terminal, gate)
    private fun jfk(expected: String? = null, actual: String? = null, terminal: String? = null, belt: String? = null) =
        FlightEnd("JFK", "New York", "John F. Kennedy International Airport", at("13:35"), at(expected), at(actual), -240, terminal, null, belt)
    private fun lh400(from: FlightEnd, to: FlightEnd, state: FlightState, aircraft: String? = null, timetable: Boolean = false) =
        Flight("LH400", "Lufthansa", from, to, state, timetable = timetable, aircraft = aircraft)

    private val gone = fra("11:02", "11:02")            // left seven minutes after its plan
    private val late = fra("11:19", "11:19")            // left twenty-four minutes after it

    /** The built-in samples: each name, what its row should say, and the sample. */
    private val built: List<Triple<String, String, () -> Sample>> = listOf(
        Triple("friday", "Leaves Fri 10:55 AM · Planned; the plane at rest at the start") {
            Sample(NUMBER, lh400(fra(), jfk(), FlightState.PLANNED), null, utc("2026-09-30T13:55:00")) },
        Triple("soon", "Leaves in 42 min · On time; Gate Z58 · Terminal 1 at the start") {
            Sample(NUMBER, lh400(fra("10:55", terminal = "1", gate = "Z58"), jfk("13:35"), FlightState.PLANNED), null, utc("2026-10-02T08:13:00")) },
        Triple("soon-late", "Leaves in 42 min · Delayed 45 min") {
            Sample(NUMBER, lh400(fra("11:40", terminal = "1", gate = "Z58"), jfk("14:20"), FlightState.PLANNED), null, utc("2026-10-02T08:58:00")) },
        Triple("air", "Lands in 1 h 12 min · On time; the plane at 86 % of the way") {
            Sample(NUMBER, lh400(gone, jfk("13:38", terminal = "1"), FlightState.IN_AIR, "Boeing 747-8"), null, utc("2026-10-02T16:26:00")) },
        Triple("air-late", "Lands in 4 h 07 min · Delayed 27 min; the plane at 53 %") {
            Sample(NUMBER, lh400(late, jfk("14:02", terminal = "1"), FlightState.IN_AIR, "Boeing 747-8"), null, utc("2026-10-02T13:55:00")) },
        Triple("air-early", "Lands in 2 h 30 min · 24 min early; the plane at 69 %") {
            Sample(NUMBER, lh400(gone, jfk("13:11", terminal = "1"), FlightState.IN_AIR, "Boeing 747-8"), null, utc("2026-10-02T14:41:00")) },
        Triple("landed", "Landed 12 min ago · On time; the plane at the end") {
            Sample(NUMBER, lh400(gone, jfk("13:38", "13:38", terminal = "1"), FlightState.LANDED, "Boeing 747-8"), null, utc("2026-10-02T17:50:00")) },
        Triple("landed-late", "Landed 12 min ago · 27 min late; Terminal 1 · Belt 4 at the far end") {
            Sample(NUMBER, lh400(late, jfk("14:02", "14:02", terminal = "1", belt = "4"), FlightState.LANDED, "Boeing 747-8"), null, utc("2026-10-02T18:14:00")) },
        Triple("landed-now", "Landed just now · On time") {
            Sample(NUMBER, lh400(gone, jfk("13:38", "13:38", terminal = "1"), FlightState.LANDED, "Boeing 747-8"), null, utc("2026-10-02T17:38:00")) },
        Triple("cancelled", "Cancelled; both times struck, no badge, no plane") {
            Sample(NUMBER, lh400(fra(terminal = "1", gate = "Z58"), jfk(), FlightState.CANCELLED), null, utc("2026-10-02T07:40:00")) },
        Triple("diverted", "Diverted; the landing's time struck, no badge, no plane") {
            Sample(NUMBER, lh400(late, jfk("14:02", terminal = "1"), FlightState.DIVERTED, "Boeing 747-8"), null, utc("2026-10-02T13:55:00")) },
        Triple("timetable", "a timetable's flight still to come: Leaves Fri 10:55 AM · Planned") {
            Sample(NUMBER, lh400(fra(), jfk(), FlightState.PLANNED, timetable = true), null, utc("2026-09-30T13:55:00")) },
        Triple("timetable-past", "a timetable's flight past its time: From the timetable; no badge, no plane") {
            Sample(NUMBER, lh400(fra(), jfk(), FlightState.PLANNED, timetable = true), null, utc("2026-10-02T12:00:00")) },
        // The ends at their widest: a date at both (it is looked at eight days before), a long aircraft name, terminal and belt. Its minutes make no sense.
        Triple("wide", "both ends at their widest: which small words give way") {
            Sample(NUMBER, lh400(late, jfk("14:02", "14:02", terminal = "1", belt = "4"), FlightState.LANDED, "Boeing 777-300ER"), null, utc("2026-09-24T18:14:00")) },
        Triple("looking", "Looking it up, for as long as it is looked at; the line at rest, two rules") { Sample(NUMBER, null, null, null) },
        Triple("offline", "No connection") { Sample(NUMBER, null, Failure.OFFLINE, null) },
        Triple("no-answer", "No answer this time") { Sample(NUMBER, null, Failure.NO_ANSWER, null) },
        Triple("not-found", "Nothing found for this number") { Sample(NUMBER, null, Failure.NOT_FOUND, null) },
        Triple("no-day", "It does not fly on that day") { Sample(NUMBER, null, Failure.NOT_THAT_DAY, null) },
        Triple("used-up", "This month's AirLabs lookups are used up (in German, the sentence too long for the headline's size)") { Sample(NUMBER, null, Failure.USED_UP, null) },
        Triple("refused", "AirLabs did not accept the key") { Sample(NUMBER, null, Failure.REFUSED, null) },
    )

    /** The saved replies a debug build carries, by the name after `flight-`. */
    private fun saved(context: Context): List<String> =
        context.assets.list("").orEmpty().filter { it.startsWith("flight-") && it.endsWith(".json") }.map { it.removePrefix("flight-").removeSuffix(".json") }.sorted()

    /** Every name `flight show` takes, each built-in one with what its row should say. */
    fun names(context: Context): String = built.joinToString(" | ") { "${it.first} (${it.second})" } + " | saved replies, as asked on 2 Oct 2026 at 07:29 UTC: " + saved(context).joinToString(" ")

    fun of(context: Context, name: String): Sample? {
        built.firstOrNull { it.first == name }?.let { return it.third() }
        if (name !in saved(context)) return null
        val flight = runCatching { context.assets.open("flight-$name.json").bufferedReader().use { AirLabs.flight(it.readText()).value } }.getOrNull() ?: return null
        return Sample(flight.number, flight, null, ASKED)
    }
}
