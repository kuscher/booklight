package io.github.kuscher.booklight.core

import java.util.Random
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretsTest {
    private val symbols = "!#$%&*+-=?@"

    @Test fun theLengthAskedFor() {
        assertEquals(24, Secrets.password(24, Random(1)).length)
        assertEquals(8, Secrets.password(8, Random(1)).length)
        assertEquals(64, Secrets.password(64, Random(1)).length)
    }

    @Test fun theLengthIsHeldBetween8And64() {
        assertEquals(8, Secrets.password(4, Random(1)).length)
        assertEquals(8, Secrets.password(0, Random(1)).length)
        assertEquals(8, Secrets.password(-5, Random(1)).length)
        assertEquals(8, Secrets.password(Int.MIN_VALUE, Random(1)).length)
        assertEquals(64, Secrets.password(100, Random(1)).length)
        assertEquals(64, Secrets.password(Int.MAX_VALUE, Random(1)).length)
    }

    @Test fun oneOfEachKindEvenAtTheShortest() {
        for (seed in 0L until 500) {
            val p = Secrets.password(8, Random(seed))
            assertTrue(p, p.any { it in 'a'..'z' })
            assertTrue(p, p.any { it in 'A'..'Z' })
            assertTrue(p, p.any { it in '0'..'9' })
            assertTrue(p, p.any { it in symbols })
        }
    }

    @Test fun noLookalikesAndNothingUnexpected() {
        for (seed in 0L until 200) {
            val p = Secrets.password(64, Random(seed))
            assertTrue(p, p.none { it in "lIO01" })
            assertTrue(p, p.all { it in 'a'..'z' || it in 'A'..'Z' || it in '2'..'9' || it in symbols })
        }
    }

    @Test fun thePromisedFourAreNotAlwaysInFront() {
        val starts = (0L until 200).map { Secrets.password(16, Random(it))[0] }
        assertTrue(starts.any { it !in 'a'..'z' })
        assertTrue(starts.any { it in 'a'..'z' })
    }

    @Test fun theRandomDecides() {
        assertEquals(Secrets.password(20, Random(42)), Secrets.password(20, Random(42)))
        assertNotEquals(Secrets.password(20, Random(42)), Secrets.password(20, Random(43)))
        val random = Random(42)
        assertNotEquals(Secrets.password(20, random), Secrets.password(20, random))
    }

    @Test fun uuidIsVersion4() {
        val shape = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
        for (seed in 0L until 500) {
            val u = Secrets.uuid(Random(seed))
            assertTrue(u, shape.matches(u))
            val parsed = UUID.fromString(u)
            assertEquals(4, parsed.version())
            assertEquals(2, parsed.variant())
            assertEquals(u, parsed.toString())
        }
    }

    @Test fun uuidFollowsTheRandom() {
        assertEquals(Secrets.uuid(Random(9)), Secrets.uuid(Random(9)))
        assertEquals(500, (0L until 500).map { Secrets.uuid(Random(it)) }.toSet().size)
    }
}
