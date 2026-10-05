package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Cast
import io.github.kuscher.booklight.core.FirstRun.Ends
import io.github.kuscher.booklight.core.FirstRun.Playing
import io.github.kuscher.booklight.core.FirstRun.Press
import org.junit.Assert.assertEquals
import org.junit.Test

class FirstRunPlayingTest {
    /** The cue is the one thing in the piece that is the user's to run: Enter, or a click on the handle that carries it. */
    @Test fun enterInTheWelcomeBeginsTheShow() {
        assertEquals(Ends.BEGIN, FirstRun.pressed(Playing.WELCOME, Press.ENTER))
        assertEquals(Ends.BEGIN, FirstRun.pressed(Playing.WELCOME, Press.CUE))
    }

    @Test fun escTabAnArrowAndAClickOnTheGlassSetTheKeysStepDown() {
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.WELCOME, Press.OTHER))
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.SHOW, Press.OTHER))
        // In the show Enter runs nothing: the highlighted row was Booklight's choice. It lands the key's step, as Esc does.
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.SHOW, Press.ENTER))
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.SHOW, Press.CUE))
    }

    @Test fun aKeyThatTypesEndsThePieceAtEveryMomentAndIsTyped() {
        for (playing in Playing.entries) assertEquals(Ends.TYPE, FirstRun.pressed(playing, Press.TYPES))
    }

    @Test fun aModifierAloneIsNothing() {
        for (playing in Playing.entries) assertEquals(Ends.NOTHING, FirstRun.pressed(playing, Press.MODIFIER))
    }

    /** One press, one step: while the key's step is being set down, nothing more happens. */
    @Test fun whileTheKeysStepIsSetDownNothingMoreHappens() {
        for (press in listOf(Press.ENTER, Press.CUE, Press.OTHER)) assertEquals(Ends.NOTHING, FirstRun.pressed(Playing.LANDING, press))
    }

    /**
     * The key's step comes with its first answer armed. No press reaches it through the piece: but for a typed
     * character, which goes to the field, every press at every moment is the piece's own, and is used up by it.
     */
    @Test fun noPressPassesThroughThePieceButATypedCharacter() {
        for (playing in Playing.entries) for (press in Press.entries)
            assertEquals("$playing $press", press == Press.TYPES, FirstRun.pressed(playing, press) == Ends.TYPE)
    }

    /**
     * A device with nothing to show (no app whose name begins with a letter): the cue has no show to begin, and sets
     * the key's step down. It never does nothing: "Show off" with Enter beside it must answer Enter.
     */
    @Test fun theCueWithNothingToShowSetsTheKeysStepDown() {
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.WELCOME, Press.ENTER, Cast.NONE))
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.WELCOME, Press.CUE, Cast.NONE))
        // While this device's rows are still being worked out the cue is kept: the show begins once they are there.
        assertEquals(Ends.BEGIN, FirstRun.pressed(Playing.WELCOME, Press.ENTER, Cast.WAITS))
        assertEquals(Ends.BEGIN, FirstRun.pressed(Playing.WELCOME, Press.ENTER, Cast.READY))
        // Everything else is as it is with a show: a typed character types, a modifier is nothing, the landing takes nothing more.
        for (playing in Playing.entries) for (press in Press.entries) for (cast in Cast.entries)
            if (!(playing == Playing.WELCOME && (press == Press.ENTER || press == Press.CUE)))
                assertEquals("$playing $press $cast", FirstRun.pressed(playing, press), FirstRun.pressed(playing, press, cast))
    }

    /**
     * Booklight presses the handle itself once the welcome has stood. With a show it begins; with none the key's step
     * lands in that moment; while this device's rows are still being read it waits for them, a second at most.
     */
    @Test fun theHandOverWaitsOnlyForRowsThatAreStillComing() {
        assertEquals(Ends.BEGIN, FirstRun.handOver(Cast.READY, waited = false))
        assertEquals(Ends.BEGIN, FirstRun.handOver(Cast.READY, waited = true))
        assertEquals(Ends.LAND, FirstRun.handOver(Cast.NONE, waited = false))
        assertEquals(Ends.NOTHING, FirstRun.handOver(Cast.WAITS, waited = false))
        assertEquals(Ends.LAND, FirstRun.handOver(Cast.WAITS, waited = true))
    }

    /** Enter held down from the cue to the key's step: it begins the show, lands the key's step, and then does nothing. Never a fourth thing. */
    @Test fun enterPressedAgainAndAgainNeverGoesFurtherThanTheLanding() {
        var playing: Playing? = Playing.WELCOME
        val did = ArrayList<Ends>()
        repeat(6) {
            val ends = FirstRun.pressed(playing ?: return@repeat, Press.ENTER)
            did += ends
            playing = when (ends) { Ends.BEGIN -> Playing.SHOW; Ends.LAND -> Playing.LANDING; Ends.TYPE -> null; Ends.NOTHING -> playing }
        }
        assertEquals(listOf(Ends.BEGIN, Ends.LAND, Ends.NOTHING, Ends.NOTHING, Ends.NOTHING, Ends.NOTHING), did)
    }
}
