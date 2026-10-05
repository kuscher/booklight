package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.Under.What
import org.junit.Assert.assertEquals
import org.junit.Test

class UnderTest {
    private fun at(untouched: Boolean = true, stage: Boolean = false, copy: Boolean? = false, zero: Boolean = false, seats: Int? = 0, tip: Boolean = false, last: Boolean = false) =
        Under.choose(untouched, stage, copy, zero, seats, tip, last)

    @Test fun onceTouchedNothingComes() {
        assertEquals(What.NOTHING, at(untouched = false, stage = true, copy = true, zero = true, seats = 3, tip = true))
        assertEquals(What.NOTHING, at(untouched = false, zero = true, seats = 3, last = true))
    }

    @Test fun firstRunsStageComesFirstAtEitherMoment() {
        assertEquals(What.STAGE, at(stage = true, copy = true, zero = true, seats = 3, tip = true))
        assertEquals(What.STAGE, at(stage = true, copy = true, zero = true, seats = 3, tip = true, last = true))
    }

    @Test fun aFreshCopyHasThePlaceAndComesAtTheLastMoment() {
        assertEquals(What.WAIT, at(copy = true, zero = true, seats = 3, tip = true))
        assertEquals(What.COPY, at(copy = true, zero = true, seats = 3, tip = true, last = true))
    }

    @Test fun theUsualRowsComeAsSoonAsNoCopyIsFresh() {
        assertEquals(What.USUAL, at(zero = true, seats = 2))
        assertEquals(What.USUAL, at(zero = true, seats = 3, tip = true, last = true))
    }

    @Test fun theyWaitWhileAnAnswerIsNotIn() {
        assertEquals(What.WAIT, at(copy = null, zero = true, seats = 3))
        assertEquals(What.WAIT, at(zero = true, seats = null))
        // At the last moment nothing waits: "not known" counts as no copy.
        assertEquals(What.USUAL, at(copy = null, zero = true, seats = 3, last = true))
    }

    @Test fun withOneSeatOrNoneATipComesAtTheLastMoment() {
        assertEquals(What.WAIT, at(zero = true, seats = 1, tip = true))
        assertEquals(What.TIP, at(zero = true, seats = 1, tip = true, last = true))
        assertEquals(What.NOTHING, at(zero = true, seats = 0, last = true))
        assertEquals(What.TIP, at(zero = true, seats = null, tip = true, last = true))
    }

    @Test fun withTheSwitchOffItIsAsItWas() {
        assertEquals(What.WAIT, at(seats = 3, tip = true))
        assertEquals(What.TIP, at(seats = 3, tip = true, last = true))
        assertEquals(What.NOTHING, at(seats = 3, last = true))
    }
}
