package io.github.kuscher.booklight.core

import java.time.Instant
import java.time.LocalDateTime

/**
 * First run's show: after the welcome Booklight types four things into its own field, and the panel
 * answers each with the rows it has every day: this device's apps, a sum, a flight, the emoji grid.
 * Then the key's screen is set down. What happens, in which order and how long after the thing
 * before, is said here as data; the app performs it. Nothing in it can be run: no step is an Enter,
 * and the rows the app puts up for it carry [NOTHING] in place of what they would do.
 * (docs/design/first-run/design.md §3, motion.md §2.2.)
 */
object Show {
    /** One thing Booklight does. */
    sealed interface Step {
        /** The first thing typed: one letter, the one most of this device's apps begin with ([letter]). */
        data object Letter : Step
        /** The field holds [text] now: a letter more, or the first letter of the next thing in place of what stood there. */
        data class Typed(val text: String) : Step
        /** The list is this device's apps for that letter, row one selected. */
        data object Apps : Step
        /** Down: the next row. */
        data object Down : Step
        /** Tab: the next stop of the selected row. */
        data object Tab : Step
        /** The list is one row: the answer of the sum that stands in the field. */
        data class Sum(val answer: String) : Step
        /** The list is one row: the example flight's. */
        data object Flight : Step
        /** The keyword's space: the keyword becomes the chip, the field is empty under it, and the list is its grid. */
        data object Grid : Step
        /** An arrow in the grid: [dx] cells across, [dy] lines down. */
        data class Cell(val dx: Int, val dy: Int) : Step
        /** The light sets off round the outline. */
        data object Lap : Step
        /** The key's screen is set down, and the show is over. */
        data object Land : Step
    }

    /** [step], [at] ms after the show's first letter. */
    data class Cue(val at: Int, val step: Step)

    /** Booklight's hand: a letter after the one before it. A hand, not a metronome, and never under two frames. */
    const val LETTER_MS = 64
    /** The letter that lands a row comes a little later than the others. */
    const val LANDS_MS = 80
    /** And the space that makes the chip later still. */
    const val SPACE_MS = 124
    /** After a sum's row has landed, and after its answer has rolled: before the next key. */
    const val LANDED_MS = 340
    const val ROLLED_MS = 300
    /** The landing waits for the light: it comes when the lap has turned onto the lower edge, this long after it set off. */
    const val LAP_TO_LAND_MS = 776

    // The first beat's moves, each after the thing before it: Down, Down, Tab, Tab.
    private const val FIRST_MOVE_MS = 480
    private const val DOWN_MS = 300
    private const val TAB_MS = 320
    private const val TAB_AGAIN_MS = 240
    // Before the first letter of the next thing: after the last Tab; after the answer has been read; after the plane has reached its place and rested.
    private const val TO_SUM_MS = 220
    private const val TO_FLIGHT_MS = 480
    private const val TO_GRID_MS = 1108
    // In the grid: the light sets off as the wave lands; then three quick steps, as a held key glides, and one settled.
    private const val TO_LAP_MS = 240
    private const val TO_CELLS_MS = 160
    private const val CELL_MS = 110
    private const val LAST_CELL_MS = 160

    /**
     * The show. [sum]: what is typed for the sum (lesson 4's own); null where sums are switched off
     * on this installation: the show shows nothing that is switched off, and that beat is left out.
     * [keyword]: the emoji grid's keyword. [comma]: the user's language writes one and a half as 1,5.
     * Each answer is what [Calc] says for the text that stands in the field then, and comes with the
     * letter that changes it.
     */
    fun script(sum: String?, keyword: String, comma: Boolean = false): List<Cue> {
        val cues = ArrayList<Cue>()
        var at = 0
        fun cue(after: Int, step: Step) { at += after; cues += Cue(at, step) }

        // Apps: one letter, the device's apps, and Booklight's four moves in them.
        cue(0, Step.Letter); cue(0, Step.Apps)
        cue(FIRST_MOVE_MS, Step.Down); cue(DOWN_MS, Step.Down); cue(TAB_MS, Step.Tab); cue(TAB_AGAIN_MS, Step.Tab)

        // A sum, typed over the letter. The apps stand until the text is a sum; then its row, and the answer rolls as the sum goes on.
        var answer: String? = null
        var gap = TO_SUM_MS
        if (sum != null) for (n in 1..sum.length) {
            val text = sum.take(n)
            val now = Calc.answer(text, comma)
            val changes = now != null && now != answer
            if (changes && answer == null) gap = maxOf(gap, LANDS_MS)
            cue(gap, Step.Typed(text))
            gap = LETTER_MS
            if (changes) { gap = if (answer == null) LANDED_MS else ROLLED_MS; answer = now; cue(0, Step.Sum(now)) }
        }

        // A flight, typed over the sum: its row lands with the last character of the number. (Without the sum's beat it is
        // typed over the letter, as long after the last Tab as the sum's first character would have come.)
        val number = FLIGHT.number
        val toFlight = if (sum != null) TO_FLIGHT_MS else TO_SUM_MS
        for (n in 1..number.length) cue(if (n == 1) toFlight else if (n == number.length) LANDS_MS else LETTER_MS, Step.Typed(number.take(n)))
        cue(0, Step.Flight)

        // The grid: the keyword typed over the number, its space, the light, four moves of the square.
        for (n in 1..keyword.length) cue(if (n == 1) TO_GRID_MS else LETTER_MS, Step.Typed(keyword.take(n)))
        cue(SPACE_MS, Step.Grid)
        cue(TO_LAP_MS, Step.Lap)
        val lap = at
        cue(TO_CELLS_MS, Step.Cell(1, 0)); cue(CELL_MS, Step.Cell(1, 0)); cue(CELL_MS, Step.Cell(1, 0)); cue(LAST_CELL_MS, Step.Cell(0, 1))

        // The landing, cued by the light.
        cue(maxOf(0, lap + LAP_TO_LAND_MS - at), Step.Land)
        return cues
    }

    /**
     * A moment of the show to stand still at, for a picture. [APPS]: the list has landed. [ROW]: after
     * Down, Down. [STOP]: after Tab, Tab. [SUM]: the sum's row has landed. [ANSWER]: its last answer
     * stands. [FLIGHT]: the flight's row. [GRID]: the grid, the square on its first cell. [CELL]: after
     * the square's four moves.
     */
    enum class Still { APPS, ROW, STOP, SUM, ANSWER, FLIGHT, GRID, CELL }

    /**
     * [script] up to [still], with everything that comes in the same moment: what to perform at once,
     * and then stand. Without the light: a still has no motion.
     */
    fun until(script: List<Cue>, still: Still): List<Cue> {
        val i = when (still) {
            Still.APPS -> script.indexOfFirst { it.step == Step.Apps }
            Still.ROW -> script.indexOfLast { it.step == Step.Down }
            Still.STOP -> script.indexOfLast { it.step == Step.Tab }
            Still.SUM -> script.indexOfFirst { it.step is Step.Sum }
            Still.ANSWER -> script.indexOfLast { it.step is Step.Sum }
            Still.FLIGHT -> script.indexOfFirst { it.step == Step.Flight }
            Still.GRID -> script.indexOfFirst { it.step == Step.Grid }
            Still.CELL -> script.indexOfLast { it.step is Step.Cell }
        }
        if (i < 0) return emptyList()
        return script.take(script.indexOfLast { it.at == script[i].at } + 1).filter { it.step != Step.Lap }
    }

    /** How many apps the first beat shows at most. */
    const val APPS = 5

    /**
     * The letter Booklight types first: the one that begins the most of [names], the device's apps;
     * of two that begin as many, the earlier one. Null where no name begins with a letter.
     */
    fun letter(names: List<String>): String? =
        names.mapNotNull { it.trim().firstOrNull()?.takeIf(Char::isLetter)?.lowercaseChar() }
            .groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<Char, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()?.key?.toString()

    /**
     * The example flight: LH 455 from San Francisco to Frankfurt, in the air and on time. Held here and
     * never asked of any service: its row says "Example". It left at 14:47 against a plan of 14:40, and
     * is expected as planned. Its number is what Booklight types for it.
     */
    val FLIGHT = Flight(
        number = "LH455", airline = "Lufthansa",
        from = FlightEnd("SFO", "San Francisco", "San Francisco International Airport", planned = LocalDateTime.parse("2026-10-01T14:40"), actual = LocalDateTime.parse("2026-10-01T14:47"), offset = -420),
        to = FlightEnd("FRA", "Frankfurt/Main", "Frankfurt Airport", planned = LocalDateTime.parse("2026-10-02T10:25"), expected = LocalDateTime.parse("2026-10-02T10:25"), offset = 120, terminal = "1"),
        state = FlightState.IN_AIR, aircraft = "Boeing 747-8",
    )

    /** The moment the example is looked at, whatever the clock says: 4 h 07 min before it lands, 391 of its 638 minutes in the air behind it. */
    val LOOKED: Instant = Instant.parse("2026-10-02T04:18:00Z")

    /** What every row of the show carries in place of what it would do: nothing the executor knows. */
    val NOTHING: Effect = Effect.Internal("show")
}
