package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcTest {
    private fun a(s: String) = Calc.answer(s)

    @Test fun arithmetic() {
        assertEquals("7", a("3+4"))
        assertEquals("42", a("12*3.5"))
        assertEquals("81", a("(4+5)^2"))
        assertEquals("0.3", a("0.1 + 0.2"))
        assertEquals("2.5", a("10 / 4"))
        assertEquals("-2", a("3 - 5"))
        assertEquals("14", a("2 + 3 × 4"))
        assertEquals("512", a("2^3^2"))
        assertEquals("1", a("10 mod 3"))
        assertEquals("120", a("5!"))
    }

    @Test fun percentages() {
        assertEquals("30", a("20% of 150"))
        assertEquals("180", a("150 + 20%"))
        assertEquals("120", a("150 - 20%"))
        assertEquals("0.1", a("50% * 20%"))
    }

    @Test fun functionsAndConstants() {
        assertEquals("1.41421356237", a("sqrt(2)"))
        assertEquals("6.28318530718", a("2 pi"))
        assertEquals("2", a("log(100)"))
        assertEquals("15", a("3(4+1)"))
    }

    @Test fun bigNumbersAreGrouped() {
        assertEquals("1,000,000", a("1000 * 1000"))
        assertEquals("1,234,567.5", a("1,234,567 + 0.5"))
        assertEquals("1234", a("1200 + 34"))
    }

    @Test fun wordsAndLoneValuesAreNotSums() {
        assertNull(a("chrome"))
        assertNull(a("e"))
        assertNull(a("pi"))
        assertNull(a("42"))
        assertNull(a("-5"))
        assertNull(a("20%"))
        assertNull(a("3 apples"))
        assertNull(a("1.2.3"))
        assertNull(a("2 +"))
        assertNull(a(""))
        assertNull(a("1/0"))
    }

    @Test fun leadingEqualsIsAllowed() {
        assertEquals("4", a("=2+2"))
    }
}
