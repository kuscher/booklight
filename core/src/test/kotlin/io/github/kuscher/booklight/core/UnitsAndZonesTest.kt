package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class UnitsAndZonesTest {

    @Test
    fun temperatureConversions() {
        val fToC = Units.convert("72f in c")
        assertNotNull(fToC)
        assertEquals("22.22 °C", fToC!!.formatted)
        assertEquals("22.22", fToC.plainNumber)
        assertEquals("72 °F", fToC.source)

        val cToF = Units.convert("100 °C to F")
        assertNotNull(cToF)
        assertEquals("212 °F", cToF!!.formatted)

        val neg = Units.convert("-40 f in c")
        assertNotNull(neg)
        assertEquals("-40 °C", neg!!.formatted)

        val de = Units.convert("20 grad c in f", comma = true)
        assertNotNull(de)
        assertEquals("68 °F", de!!.formatted)
    }

    @Test
    fun compoundAndStandardLengthConversions() {
        val ftIn = Units.convert("5 ft 10 in cm")
        assertNotNull(ftIn)
        assertEquals("177.8 cm", ftIn!!.formatted)
        assertEquals("5 ft 10 in", ftIn.source)

        val ftInTo = Units.convert("5 ft 10 in to cm")
        assertNotNull(ftInTo)
        assertEquals("177.8 cm", ftInTo!!.formatted)

        val quotes = Units.convert("5'10\" in cm")
        assertNotNull(quotes)
        assertEquals("177.8 cm", quotes!!.formatted)

        val toFeet = Units.convert("178 cm in ft")
        assertNotNull(toFeet)
        assertEquals("5.84 ft", toFeet!!.formatted)
        assertTrue(toFeet.source.contains("5 ft 10 in"))

        val miles = Units.convert("10 mi in km")
        assertNotNull(miles)
        assertEquals("16.09 km", miles!!.formatted)
    }

    @Test
    fun massVolumeSpeedDataAndDurationConversions() {
        val oz = Units.convert("16 oz in ml")
        assertNotNull(oz)
        assertEquals("473.18 ml", oz!!.formatted)

        val flOz = Units.convert("16 fl oz in ml")
        assertNotNull(flOz)
        assertEquals("473.18 ml", flOz!!.formatted)

        val lbsOz = Units.convert("8 lb 4 oz in kg")
        assertNotNull(lbsOz)
        assertEquals("3.74 kg", lbsOz!!.formatted)

        val speed = Units.convert("65 mph in km/h")
        assertNotNull(speed)
        assertEquals("104.61 km/h", speed!!.formatted)

        val data = Units.convert("15,5 gb in mb", comma = true)
        assertNotNull(data)
        assertEquals("15.500 MB", data!!.formatted)
        assertEquals("15500", data.plainNumber)

        val dur = Units.convert("36 h in d")
        assertNotNull(dur)
        assertEquals("1.5 d", dur!!.formatted)
    }

    @Test
    fun everydayQueriesAreNeverUnitConversions() {
        assertNull(Units.convert("chrome"))
        assertNull(Units.convert("sign in to google"))
        assertNull(Units.convert("5 things to do"))
        assertNull(Units.convert("100 m"))
    }

    @Test
    fun timeZoneConversionsAndWorldClock() {
        val now = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, ZoneId.of("America/Los_Angeles"))

        val sfoToBer = Zones.convert("3pm sfo in berlin", now = now, is24Hour = true, german = false)
        assertNotNull(sfoToBer)
        assertEquals("00:00", sfoToBer!!.time)
        assertTrue(sfoToBer.title.startsWith("00:00 Fri · Berlin"))
        assertTrue(sfoToBer.subtitle.contains("+9h"))
        assertNotNull(sfoToBer.epochMillis)

        val cetToLocal = Zones.convert("14:00 cet", now = now, is24Hour = true, german = false)
        assertNotNull(cetToLocal)
        assertEquals("05:00", cetToLocal!!.time)
        assertEquals("America/Los_Angeles", cetToLocal.targetZone)

        val world = Zones.convert("time in tokyo", now = now, is24Hour = true, german = false)
        assertNotNull(world)
        assertEquals("04:00", world!!.time)
        assertEquals("04:00 · Tokyo", world.title)
        assertNull(world.epochMillis)

        val de = Zones.convert("15 uhr berlin nach sfo", now = now, is24Hour = true, german = true)
        assertNotNull(de)
        assertEquals("06:00", de!!.time)

        assertNull(Zones.convert("3pm dentist", now = now))
        assertNull(Zones.convert("time tracker", now = now))
    }

    @Test
    fun taskExtractionFromText() {
        val raw = """
            Here are the tasks:
            - [ ] Call the bank before Friday
            - Send the Q3 slides to Anna
            1. Book train tickets to Hamburg
            - Call the bank before Friday
        """.trimIndent()
        val tasks = NoteText.extractTasks(raw)
        assertEquals(
            listOf(
                "Call the bank before Friday",
                "Send the Q3 slides to Anna",
                "Book train tickets to Hamburg",
            ),
            tasks,
        )

        val explicit = NoteText.explicitTasks(
            """
            Meeting notes Oct 15
            - [ ] Update release notes
            TODO: Check Play Console track
            Some discussion text
            """.trimIndent()
        )
        assertEquals(listOf("Update release notes", "Check Play Console track"), explicit)
    }
}
