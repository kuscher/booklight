package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.Show.Cue
import io.github.kuscher.booklight.core.Show.Step
import io.github.kuscher.booklight.core.Show.Still
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class ShowTest {
    /** The script as the product plays it: the sum of lesson 4, the emoji keyword. */
    private val script = Show.script("150 + 20%", "emoji")

    private fun at(step: Step): Int = script.first { it.step == step }.at
    private fun typed(): List<Cue> = script.filter { it.step is Step.Typed }
    private fun text(c: Cue): String = (c.step as Step.Typed).text

    /** `motion.md` §2.2, counted from the show's first letter (its 3,820 ms). */
    @Test fun theMarksAreThePapers() {
        assertEquals(0, at(Step.Letter))
        assertEquals(0, at(Step.Apps))
        assertEquals(listOf(480, 780), script.filter { it.step == Step.Down }.map { it.at })
        assertEquals(listOf(1100, 1340), script.filter { it.step == Step.Tab }.map { it.at })
        assertEquals(1560, at(Step.Typed("1")))
        assertEquals(1960, at(Step.Typed("150 + 2")))
        assertEquals(2300, at(Step.Typed("150 + 20")))
        assertEquals(2600, at(Step.Typed("150 + 20%")))
        assertEquals(3080, at(Step.Typed("L")))
        assertEquals(3352, at(Step.Flight))
        assertEquals(4460, at(Step.Typed("e")))
        assertEquals(4840, at(Step.Grid))
        assertEquals(5080, at(Step.Lap))
        assertEquals(listOf(5240, 5350, 5460, 5620), script.filter { it.step is Step.Cell }.map { it.at })
        assertEquals(5856, at(Step.Land))
    }

    /**
     * "Show sums" switched off (a replay on an installation that has been lived in): the show shows nothing this
     * installation has switched off. The sum's beat is left out, and the flight is typed over the letter.
     */
    @Test fun whereSumsAreSwitchedOffTheShowLeavesTheSumOut() {
        val bare = Show.script(null, "emoji")
        assertTrue(bare.none { it.step is Step.Sum })
        // Nothing that is typed is a sum: every text is the flight's number or the keyword, a letter at a time.
        val texts = bare.mapNotNull { (it.step as? Step.Typed)?.text }
        assertEquals((1..5).map { "LH455".take(it) } + (1..5).map { "emoji".take(it) }, texts)
        assertTrue(texts.none { Calc.answer(it) != null })
        // The flight's first letter comes after the last Tab as the sum's first would have: 220 ms.
        assertEquals(bare.last { it.step == Step.Tab }.at + 220, bare.first { it.step is Step.Typed }.at)
        // What is left is in the same order, and ends on the landing, cued by the light as before.
        assertEquals(script.filter { it.step !is Step.Sum && !(it.step is Step.Typed && text(it).startsWith("1")) }.map { it.step }, bare.map { it.step })
        assertEquals(bare.map { it.at }.sorted(), bare.map { it.at })
        assertEquals(Step.Land, bare.last().step)
        assertEquals(bare.first { it.step == Step.Lap }.at + Show.LAP_TO_LAND_MS, bare.last().at)
        // A still of a beat that is not played is nothing to perform; the others are as they were.
        assertEquals(emptyList<Cue>(), Show.until(bare, Still.SUM))
        assertEquals(Step.Flight, Show.until(bare, Still.FLIGHT).last().step)
    }

    @Test fun itEndsOnTheLandingAndNothingComesAfter() {
        assertEquals(Step.Land, script.last().step)
        assertEquals(1, script.count { it.step == Step.Land })
        assertEquals(script.map { it.at }.sorted(), script.map { it.at })
        // Under seven seconds from the first letter: with the welcome's four, the whole piece stays under the thirteen it may take.
        assertTrue(script.last().at < 7000)
    }

    /** The show never shows a number the product would not: each answer is what the calculator says for the text that stands then. */
    @Test fun everyAnswerIsTheCalculatorsForTheTextInTheField() {
        val sums = script.filter { it.step is Step.Sum }
        assertEquals(listOf("152", "170", "180"), sums.map { (it.step as Step.Sum).answer })
        for (s in sums) {
            val field = text(typed().last { it.at <= s.at })
            assertEquals(field, Calc.answer(field), (s.step as Step.Sum).answer)
        }
        // And nothing answers before the text is a sum: the apps stand until then.
        assertEquals(1960, sums.first().at)
        for (c in typed().filter { it.at in 1560 until 1960 }) assertNull(text(c), Calc.answer(text(c)))
    }

    /** One movement, not four slides: the text is typed over, never cleared, and the field is never empty before the chip. */
    @Test fun theTextIsTypedOverAndTheFieldIsNeverEmpty() {
        var before = ""
        for (c in typed()) {
            val now = text(c)
            assertTrue(now, now.isNotEmpty())
            // A letter more, or the first letter of the next thing in place of all that stood there.
            assertTrue("$before -> $now", now.length == 1 || (now.length == before.length + 1 && now.startsWith(before)))
            before = now
        }
        assertEquals(listOf("1", "L", "e"), typed().map(::text).filter { it.length == 1 })
        // The whole of each thing is typed: the sum, the flight's number, the keyword.
        assertTrue(typed().any { text(it) == "150 + 20%" })
        assertTrue(typed().any { text(it) == Show.FLIGHT.number })
        assertEquals("emoji", text(typed().last()))
        // The keyword's space makes the chip: no text is typed after it.
        assertTrue(typed().all { it.at < at(Step.Grid) })
    }

    /** A hand, not a machine gun: no two letters closer than two frames of a 60 Hz screen, and the letter that lands a row a little later than the others. */
    @Test fun itIsTypedByAHand() {
        val gaps = typed().zipWithNext { a, b -> b.at - a.at }
        assertTrue(gaps.toString(), gaps.all { it >= 34 })
        assertEquals(Show.LETTER_MS, at(Step.Typed("15")) - at(Step.Typed("1")))
        assertEquals(Show.LANDS_MS, at(Step.Typed("150 + 2")) - at(Step.Typed("150 + ")))
        assertEquals(Show.LANDS_MS, at(Step.Flight) - at(Step.Typed("LH45")))
        assertEquals(Show.SPACE_MS, at(Step.Grid) - at(Step.Typed("emoji")))
        assertEquals(Show.LAP_TO_LAND_MS, at(Step.Land) - at(Step.Lap))
    }

    /** Booklight's moves in the first beat come before it types on, and its moves in the grid after the grid has come. */
    @Test fun theMovesBelongToTheirBeats() {
        val moves = script.filter { it.step == Step.Down || it.step == Step.Tab }
        assertTrue(moves.all { it.at > at(Step.Apps) && it.at < at(Step.Typed("1")) })
        val cells = script.filter { it.step is Step.Cell }
        assertEquals(listOf(Step.Cell(1, 0), Step.Cell(1, 0), Step.Cell(1, 0), Step.Cell(0, 1)), cells.map { it.step })
        assertTrue(cells.all { it.at > at(Step.Grid) && it.at < at(Step.Land) })
    }

    /** Another sum is typed by the same hand, and its answers are still the calculator's. */
    @Test fun anotherSumIsPlayedByTheSameRules() {
        val other = Show.script("12*3", "emoji")
        assertEquals(listOf("36"), other.filter { it.step is Step.Sum }.map { (it.step as Step.Sum).answer })
        assertEquals(Step.Land, other.last().step)
        // Where the user's language writes one and a half as 1,5 the answer is written so too.
        assertEquals(listOf("1,5"), Show.script("3/2", "emoji", comma = true).filter { it.step is Step.Sum }.map { (it.step as Step.Sum).answer })
    }

    @Test fun aStillIsTheScriptUpToItsMoment() {
        fun last(still: Still) = Show.until(script, still).last().step
        assertEquals(Step.Apps, last(Still.APPS))
        assertEquals(Step.Down, last(Still.ROW))
        assertEquals(Step.Tab, last(Still.STOP))
        assertEquals(Step.Sum("152"), last(Still.SUM))
        assertEquals(Step.Sum("180"), last(Still.ANSWER))
        assertEquals(Step.Flight, last(Still.FLIGHT))
        assertEquals(Step.Grid, last(Still.GRID))
        assertEquals(Step.Cell(0, 1), last(Still.CELL))
        // Each is the script from its beginning to that moment, without the light (a still has no motion), and none of them lands.
        for (still in Still.entries) {
            val part = Show.until(script, still)
            assertEquals(script.filter { it.at <= part.last().at && it.step != Step.Lap }, part)
            assertFalse(part.any { it.step == Step.Land || it.step == Step.Lap })
        }
        assertEquals(2, Show.until(script, Still.ROW).count { it.step == Step.Down })
        assertEquals(2, Show.until(script, Still.STOP).count { it.step == Step.Tab })
    }

    @Test fun theFirstLetterIsTheOneMostAppsBeginWith() {
        assertEquals("c", Show.letter(listOf("Calculator", "Calendar", "camera", "Maps", "Messages", "Chrome")))
        // Of two that begin as many, the earlier in the alphabet; a name is taken as it is shown, whatever its case.
        assertEquals("m", Show.letter(listOf("Zebra", "zoo", "Maps", "messages")))
        // A name that begins with a digit or a sign has no letter to give.
        assertEquals("p", Show.letter(listOf("1 Player", "2048", " Photos", "+Plus")))
        assertNull(Show.letter(listOf("1 Player", "2048")))
        assertNull(Show.letter(emptyList()))
        assertEquals("ä", Show.letter(listOf("Ärzte", "ärger", "Maps")))
    }

    /** The example is a flight in the air, on time, the plane 61 % of the way: said by the same rules as any flight's row. */
    @Test fun theExampleFlightIsInTheAirAndOnTime() {
        val row = FlightStatus.row(Show.FLIGHT, Show.LOOKED)
        assertEquals(FlightPhase.IN_AIR, row.phase)
        assertEquals(Headline(Heading.LANDS_IN, 247), row.headline)       // 4 h 07 min
        assertEquals(Badge(Verdict.ON_TIME), row.badge)
        assertEquals(Tone.GOOD, row.badge!!.tone)
        val share = row.share!!
        assertEquals(391.0 / 638.0, share, 1e-9)
        assertTrue(share > 0.60 && share < 0.62)
        // Under the line's ends: where it left, with the aircraft; where it lands, with the terminal. No gate, no belt.
        assertEquals(EndSays("SFO", LocalDateTime.parse("2026-10-01T14:47"), aircraft = "Boeing 747-8"), row.from)
        assertEquals(EndSays("FRA", LocalDateTime.parse("2026-10-02T10:25"), terminal = "1"), row.to)
        assertEquals("San Francisco", Show.FLIGHT.from.place)
        assertEquals("Frankfurt/Main", Show.FLIGHT.to.place)
    }

    /** What Booklight types for it reads as that flight, by the airline table the app ships. */
    @Test fun itsNumberReadsAsAFlightNumber() {
        val airlines = Airlines(File("../app/src/main/assets/airlines.tsv").readLines().asSequence())
        val n = Flights.read(Show.FLIGHT.number, airlines, LocalDate.parse("2026-10-01"))!!
        assertTrue(n.strong)
        assertEquals("LH 455", n.shown)
        assertEquals(Show.FLIGHT.airline, n.airline.name)
    }

    /** What the show's rows carry in place of what they would do is something the executor has no branch for. */
    @Test fun theShowsRowsCarryNothingToRun() {
        assertEquals(Effect.Internal("show"), Show.NOTHING)
    }
}
