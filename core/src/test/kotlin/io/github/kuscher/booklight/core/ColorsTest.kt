package io.github.kuscher.booklight.core

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorsTest {
    private val blue = Rgb(52, 120, 246)

    @Test fun hex() {
        assertEquals(blue, Colors.parse("#3478f6"))
        assertEquals(blue, Colors.parse("  #3478F6 "))
        assertEquals(Rgb(255, 255, 255), Colors.parse("#fff"))
        assertEquals(Rgb(170, 187, 204), Colors.parse("#ABC"))
        assertEquals(Rgb(0, 0, 0), Colors.parse("#000000"))
    }

    @Test fun rgb() {
        assertEquals(blue, Colors.parse("rgb(52,120,246)"))
        assertEquals(blue, Colors.parse("RGB( 52 , 120 , 246 )"))
        assertEquals(blue, Colors.parse("rgb(52 120 246)"))
        assertEquals(blue, Colors.parse("rgb (52, 120, 246)"))
    }

    @Test fun hsl() {
        assertEquals(Rgb(255, 0, 0), Colors.parse("hsl(0, 100%, 50%)"))
        assertEquals(Rgb(0, 255, 0), Colors.parse("hsl(120 100% 50%)"))
        assertEquals(Rgb(0, 0, 255), Colors.parse("HSL(240deg, 100 %, 50 %)"))
        assertEquals(Rgb(255, 0, 0), Colors.parse("hsl(360, 100%, 50%)"))
        assertEquals(Rgb(255, 255, 255), Colors.parse("hsl(0,0%,100%)"))
        assertEquals(Rgb(128, 128, 128), Colors.parse("hsl(0, 0, 50)"))
    }

    @Test fun valuesOutOfRange() {
        for (s in listOf("rgb(256,0,0)", "rgb(-1,0,0)", "rgb(0,0,999999999999)", "hsl(361,50%,50%)", "hsl(10,101%,50%)",
            "hsl(10,50%,-1%)", "hsl(NaN,50%,50%)")) assertNull(s, Colors.parse(s))
    }

    @Test fun notAColour() {
        for (s in listOf("", "  ", "#", "#12", "#1234", "#12345", "#12345g", "#3478f6ff", "3478f6", "abc", "red", "rgb", "rgb(",
            "rgb()", "rgb(1,2)", "rgb(1,2,3,4)", "rgb(a,b,c)", "rgb(1.5,2,3)", "rgb(50%,0,0)", "rgb(1,2,3) x", "hsl()", "cmyk(1,2,3)",
            "(1,2,3)", "#３４７８ｆ６", "#" + "f".repeat(10_000))) assertNull(s, Colors.parse(s))
    }

    @Test fun theFourWaysToWriteIt() {
        assertEquals("#3478F6", blue.hex())
        assertEquals("rgb(52, 120, 246)", blue.rgb())
        assertEquals("hsl(219, 92%, 58%)", blue.hsl())
        assertEquals("oklch(0.60 0.20 261)", blue.oklch())
        assertEquals(0xFF3478F6.toInt(), blue.argb)
        assertEquals("#0A0B0C", Rgb(10, 11, 12).hex())
    }

    @Test fun primaries() {
        assertEquals("hsl(0, 100%, 50%)", Rgb(255, 0, 0).hsl())
        assertEquals("hsl(120, 100%, 50%)", Rgb(0, 255, 0).hsl())
        assertEquals("hsl(240, 100%, 50%)", Rgb(0, 0, 255).hsl())
        assertEquals("hsl(300, 100%, 50%)", Rgb(255, 0, 255).hsl())
        // The values CSS Color 4 gives for the sRGB primaries.
        assertEquals("oklch(0.63 0.26 29)", Rgb(255, 0, 0).oklch())
        assertEquals("oklch(0.87 0.29 142)", Rgb(0, 255, 0).oklch())
        assertEquals("oklch(0.45 0.31 264)", Rgb(0, 0, 255).oklch())
    }

    @Test fun greysHaveNoHue() {
        assertEquals("hsl(0, 0%, 0%)", Rgb(0, 0, 0).hsl())
        assertEquals("hsl(0, 0%, 100%)", Rgb(255, 255, 255).hsl())
        assertEquals("hsl(0, 0%, 50%)", Rgb(128, 128, 128).hsl())
        assertEquals("oklch(0.00 0.00 0)", Rgb(0, 0, 0).oklch())
        assertEquals("oklch(1.00 0.00 0)", Rgb(255, 255, 255).oklch())
        assertEquals("oklch(0.60 0.00 0)", Rgb(128, 128, 128).oklch())
        assertEquals(0xFF000000.toInt(), Rgb(0, 0, 0).argb)
        assertEquals(-1, Rgb(255, 255, 255).argb)
    }

    @Test fun whatIsWrittenCanBeReadBack() {
        val random = java.util.Random(3)
        repeat(500) {
            val c = Rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            assertEquals(c, Colors.parse(c.hex()))
            assertEquals(c, Colors.parse(c.rgb()))
            // hsl() is rounded to whole numbers, so it comes back close, not equal.
            val back = Colors.parse(c.hsl())!!
            assertTrue("$c ${c.hsl()} $back", abs(back.r - c.r) <= 4 && abs(back.g - c.g) <= 4 && abs(back.b - c.b) <= 4)
        }
    }
}
