package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class FlightsTest {
    /** The table the app carries, read where it lies in the repository. */
    private val airlines = Airlines(File("../app/src/main/assets/airlines.tsv").readLines().asSequence())
    /** A Thursday. */
    private val today = LocalDate.of(2026, 10, 1)

    private fun read(text: String, sure: Boolean = false) = Flights.read(text, airlines, today, sure)

    @Test fun `the table knows an airline by both of its codes, and the largest one first`() {
        assertEquals("Lufthansa", airlines.designator("LH")?.name)
        assertEquals("DLH", airlines.designator("lh")?.icao)
        assertEquals("LH", airlines.code("DLH")?.iata)
        assertEquals("Lufthansa Cargo", airlines.code("GEC")?.name)       // the second LH is found by its own code
        assertEquals("easyJet", airlines.designator("U2")?.name)
        assertEquals("Discover Airlines", airlines.designator("4Y")?.name)
        assertNull(airlines.designator("X8"))
    }

    @Test fun `a flight number in the ways people type it`() {
        for (t in listOf("LH455", "lh455", "LH 455", "lh 455", "LH-455", "LH0455", "LH 0455", " lh455 ")) {
            val n = read(t)
            assertNotNull(t, n)
            assertEquals(t, "LH455", n!!.iata)
            assertEquals(t, "DLH455", n.icao)
            assertEquals(t, "LH 455", n.shown)
            assertEquals(t, "Lufthansa", n.airline.name)
            assertTrue(t, n.strong)
            assertFalse(t, n.callsign)
            assertNull(t, n.day)
        }
    }

    @Test fun `the callsign is the same flight`() {
        for (t in listOf("DLH455", "dlh455", "DLH 455", "DLH0455")) {
            val n = read(t)!!
            assertEquals(t, "LH455", n.iata)
            assertEquals(t, "DLH455", n.icao)
            assertTrue(t, n.strong)
            assertTrue(t, n.callsign)
        }
        assertEquals("UAL90", read("UA 90")!!.icao)
        assertEquals("BAW64", read("ba64")!!.icao)
    }

    @Test fun `a suffix letter belongs to the number`() {
        val n = read("LH455A")!!
        assertEquals("LH455A", n.iata)
        assertEquals("LH 455A", n.shown)
    }

    @Test fun `a day after the number`() {
        assertEquals(LocalDate.of(2026, 10, 2), read("LH455 fri")!!.day)
        assertEquals(LocalDate.of(2026, 10, 2), read("LH455 tomorrow")!!.day)
        assertEquals(LocalDate.of(2026, 10, 2), read("lh 455 morgen")!!.day)
        assertEquals(LocalDate.of(2026, 10, 1), read("LH455 today")!!.day)
        // A weekday that is today is today: the flight of this afternoon, not the one in a week.
        assertEquals(LocalDate.of(2026, 10, 1), read("LH455 thursday")!!.day)
        assertEquals(LocalDate.of(2026, 10, 7), read("LH455 Mittwoch")!!.day)
        assertEquals(LocalDate.of(2026, 10, 3), read("LH455 3.10.")!!.day)
        assertEquals(LocalDate.of(2026, 10, 3), read("LH455 oct 3")!!.day)
        assertEquals("LH455@2026-10-02", read("LH455 fri")!!.id)
        assertEquals("LH455", read("LH455")!!.id)
        assertTrue(read("LH455 fri")!!.strong)
    }

    @Test fun `a day that is still being typed leaves the number's own row standing`() {
        // One or two letters are no day yet, though "sa", "mo" and "fr" are the short forms of days: "mo" is on its way to "morgen".
        for (t in listOf("LH455 s", "LH455 sa", "LH455 m", "LH455 mo", "LH455 fr", "LH455 to", "LH455 x", "lh 455 di")) {
            val n = read(t)
            assertNotNull(t, n)
            assertNull(t, n!!.day)
            assertEquals(t, "LH455", n.id)
            assertTrue(t, n.strong)
        }
        // From three letters on a day is a day.
        assertEquals(LocalDate.of(2026, 10, 3), read("LH455 sat")!!.day)
        assertEquals(LocalDate.of(2026, 10, 5), read("LH455 mon")!!.day)
        assertEquals(LocalDate.of(2026, 10, 2), read("LH455 morgen")!!.day)
        // And the beginning of a day's word is still the number's own row: nothing is asked twice on the way to "tomorrow".
        for (t in listOf("LH455 tom", "LH455 tomorr", "LH455 mor", "LH455 frei", "LH455 sam", "LH455 okt", "LH455 Don")) {
            assertNull(t, read(t)!!.day)
            assertEquals(t, "LH455", read(t)!!.id)
        }
        // What begins no day is no flight, as before; nor is a number or a sign after the number.
        for (t in listOf("LH455 xyz", "LH455 sta", "LH455 stat", "LH455 3", "LH455 3.", "LH455 a1", "LH455 s s")) assertNull(t, read(t))
    }

    @Test fun `the pinned window's name for its flight reads back as that number on that day`() {
        // The number as it was typed and the day it leaves, with the year: a day that has passed stays that day.
        val n = read("LH455 2026-10-01", sure = true)!!
        assertEquals(LocalDate.of(2026, 10, 1), n.day)
        assertEquals("LH455@2026-10-01", n.id)
        val old = read("DLH455 2026-09-28", sure = true)!!
        assertEquals(LocalDate.of(2026, 9, 28), old.day)
        assertTrue(old.callsign)
        assertEquals(LocalDate.of(2026, 10, 1), read("U28001 2026-10-01", sure = true)!!.day)
    }

    @Test fun `nothing else may stand in the line`() {
        for (t in listOf("LH455 to Frankfurt", "LH455 status", "flight LH455", "LH", "LH 455 456", "LH12345", "LH455 2026", "the LH455", "LH455?", "LH4 5 5"))
            assertNull(t, read(t))
    }

    @Test fun `a designator no airline has is not a flight`() {
        // B1, X8, M1 and VW are nobody's (docs/research/flights.md §4).
        for (t in listOf("B12", "X86 64", "M1 1AE", "VW 2026", "XXX123")) assertNull(t, read(t))
        // No number after the designator.
        for (t in listOf("A4", "U2", "K9", "B1", "LH", "DLH")) assertNull(t, read(t))
        // A postcode.
        assertNull(read("SW1A 1AA"))
    }

    @Test fun `every false friend of the research's table is weak or nothing`() {
        // What it reads as, by the table: all of them weak.
        val weak = mapOf(
            "PS5" to "Ukraine International Airlines", "MP3" to "Martinair", "H264" to "Sky Airline", "G7 2026" to "GoJet Airlines", "Q4 2026" to "Starlink Aviation",
            "MS 365" to "Egyptair", "OS 26" to "Austrian Airlines", "AS 400" to "Alaska Airlines", "HD 1080" to "AIRDO", "SD 128" to "Sudan Airways",
            "AI 5" to "Air India", "BT 5" to "Air Baltic", "QR 1" to "Qatar Airways", "PC 100" to "Pegasus Airlines", "4K 60" to "Askari Aviation",
            "5G 2" to "Anda Air", "M3 16" to "ABSA Cargo", "E5 2600" to "Air Arabia Egypt", "I7 1260" to "IndiaOne Air",
            "AM 7" to "Aeroméxico", "PM 5" to "Canary Fly", "IN 2026" to "NAM Air", "TO 100" to "Transavia France", "AT 1200" to "Royal Air Maroc",
            "NO 5" to "Neos", "ME 2" to "Middle East Airlines", "WE 3" to "Parata Air", "US 1" to "US Airways", "IT 100" to "Tigerair Taiwan",
            "DE 2026" to "Condor", "FR 24" to "Ryanair", "OK 2" to "Czech Airlines", "ON 2" to "Our Airline", "OR 2" to "TUI fly Netherlands",
            "IF 2" to "Fly Baghdad", "DO 2" to "Sky High", "GO 2" to "Kuzu Airlines Cargo", "BY 2" to "TUI Airways", "SO 2" to null,
            "UP 2" to "Bahamasair", "MY 2" to "Midwest Airlines (Egypt)", "K9 1" to null, "B6 12" to "JetBlue Airways", "A4 210" to null,
        )
        for ((t, name) in weak) {
            val n = read(t)
            assertNotNull(t, n)
            assertFalse("$t is weak", n!!.strong)
            if (name != null) assertEquals(t, name, n.airline.name)
        }
        // The same without the space, in small letters: as weak.
        for (t in listOf("ps5", "mp3", "h264", "ms365", "as400", "hd1080", "fr24", "de2026")) assertFalse(t, read(t)!!.strong)
    }

    @Test fun `a year is weak only where it stands apart`() {
        assertFalse(read("LH 2026")!!.strong)
        assertFalse(read("LH 1999")!!.strong)
        assertTrue(read("LH2026")!!.strong)
        assertTrue(read("LH 2040")!!.strong)
        assertTrue(read("LH 455")!!.strong)
    }

    @Test fun `one digit is weak, real flight or not`() {
        for (t in listOf("BA1", "QF1", "LH2", "LH 9", "DLH5")) assertFalse(t, read(t)!!.strong)
        assertTrue(read("BA12")!!.strong)
    }

    @Test fun `a digit in the designator is weak`() {
        for (t in listOf("U2 8001", "U28001", "W6 2301", "B6 123", "4Y 134", "9W 12")) assertFalse(t, read(t)?.strong ?: false)
        assertEquals("EZY8001", read("U2 8001")!!.icao)
    }

    @Test fun `three letters that are a word are weak`() {
        for (t in listOf("WIN 11", "MAC 15", "RAM 16", "ICE 123", "UPS 123", "LOT 281", "ONE 23", "SET 12", "TAX 30")) assertFalse(t, read(t)!!.strong)
        // LOT's own flights are strong by the ticket's form.
        assertTrue(read("LO 281")!!.strong)
    }

    @Test fun `after the keyword whatever reads as a flight is one`() {
        for (t in listOf("ps5", "u2 8001", "ms 365", "ba1", "de 2026", "win 11")) {
            val n = read(t, sure = true)
            assertNotNull(t, n)
            assertTrue(t, n!!.strong)
        }
        assertEquals("easyJet", read("u2 8001", sure = true)!!.airline.name)
        // What is no flight stays none.
        assertNull(read("hello", sure = true))
        assertNull(read("x8 12", sure = true))
    }

    @Test fun `the page is found by the callsign's form`() {
        assertEquals("https://www.flightaware.com/live/flight/DLH455", Flights.page(read("lh 455")!!))
        assertEquals("https://www.flightaware.com/live/flight/EZY8001", Flights.page(read("U2 8001")!!))
    }

    @Test fun `a flight number in a sentence`() {
        fun find(text: String) = Flights.find(text, airlines, today)
        assertEquals("LH454", find("Landing with LH 454 at 12:45")!!.iata)
        assertEquals("LH455", find("Dein Flug LH455 ab San Francisco, Sitz 34A")!!.iata)
        assertEquals("UA90", find("UA90/LH9052 to Newark")!!.iata)          // the first one
        assertEquals("LH455", find("(DLH455)")!!.iata)
        // In a sentence small letters are words, and what is weak is not offered.
        for (t in listOf("flying to 100 cities in 2026", "lh 455", "my PS5 and an MP3", "room B6 12", "see you AT 1200", "Windows 11, WIN 11", "ABCD1234", "LH455x", "xLH455", "LH 12345", ""))
            assertNull(t, find(t))
    }

    @Test fun `odd input is no flight and no crash`() {
        for (t in listOf("", " ", "L", "LH", "455", "LH000", "LH 0", "ß1 23", "ＬＨ４５５", "LH455 " + "x".repeat(200), "LH\t455", "LH--455", "LH 455 ,"))
            read(t)
        assertNull(read("LH000"))
        assertNull(read("LH 0"))
    }
}
