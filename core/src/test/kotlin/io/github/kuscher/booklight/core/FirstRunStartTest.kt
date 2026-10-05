package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Key
import io.github.kuscher.booklight.core.FirstRun.Keyboard
import io.github.kuscher.booklight.core.FirstRun.Overture
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunStartTest {
    private fun begins(s: State, motion: Boolean = true, reader: Boolean = false, plain: Boolean = true, screenDp: Float = 1200f) =
        FirstRun.overture(s, motion, reader, plain, screenDp)

    @Test fun theOpeningIsShownOnceOnTheVeryFirstStart() {
        val first = State(Run.NEW)
        assertEquals(Overture.WELCOME, begins(first))
        assertEquals(Overture.NONE, begins(FirstRun.shown(first)))
        assertEquals(FirstRun.shown(first), FirstRun.shown(FirstRun.shown(first)))
    }

    @Test fun notForAnUpdateNorWithoutARun() {
        assertEquals(Overture.NONE, begins(State(Run.UPDATE)))
        assertEquals(Overture.NONE, begins(State()))
    }

    @Test fun askingUsesNothingUp() {
        // Animations off, a screen reader on, text handed over by another app: no opening now, and it is still to come.
        val first = State(Run.NEW)
        assertEquals(Overture.NONE, begins(first, motion = false))
        assertEquals(Overture.NONE, begins(first, reader = true))
        assertEquals(Overture.NONE, begins(first, plain = false))
        assertEquals(Overture.WELCOME, begins(first))
    }

    /** The welcome's glass is 468 dp high: a screen too low for it has no opening piece at all, and the key's step stands at once. */
    @Test fun aScreenTooLowForTheWelcomeHasNoOpeningPiece() {
        assertEquals(Overture.WELCOME, begins(State(Run.NEW), screenDp = 655f))
        assertEquals(Overture.NONE, begins(State(Run.NEW), screenDp = 654f))
        assertEquals(Overture.NONE, begins(State(Run.NEW), screenDp = 475f))
        assertEquals(Overture.NONE, begins(State(Run.NEW), screenDp = 474f))
        // And such an opening is not the Slow one: there is nothing to be slow for.
        assertFalse(FirstRun.slow(State(Run.NEW), begins(State(Run.NEW), screenDp = 654f)))
        assertEquals(listOf(Overture.NONE, Overture.WELCOME), Overture.entries)
    }

    private fun greets(s: State, motion: Boolean = true, reader: Boolean = false, plain: Boolean = true, screenDp: Float = 1200f) =
        FirstRun.greets(s, motion, reader, plain, screenDp)

    /** Animations off, or a screen reader on: no piece is played, and the welcome's title is the field's placeholder for that opening. */
    @Test fun whereThePieceIsDueAndNotPlayedItsTitleGreets() {
        val first = State(Run.NEW)
        assertTrue(greets(first, motion = false))
        assertTrue(greets(first, reader = true))
        assertTrue(greets(first, motion = false, reader = true))
        // Where it is played, nothing greets in its place.
        assertFalse(greets(first))
        // It uses nothing up: the piece is still to come once animations are on.
        assertEquals(Overture.WELCOME, begins(first))
    }

    /** Nobody is greeted for whom the piece would not have played either. */
    @Test fun nobodyElseIsGreeted() {
        for (motion in listOf(true, false)) for (reader in listOf(true, false)) {
            // No run, an update's run, a run past the key, a piece that was shown, a run that waits.
            assertFalse(greets(State(), motion, reader))
            assertFalse(greets(State(key = true), motion, reader))
            assertFalse(greets(State(Run.UPDATE), motion, reader))
            assertFalse(greets(State(Run.NEW, done = listOf("key"), key = true), motion, reader))
            assertFalse(greets(FirstRun.shown(State(Run.NEW)), motion, reader))
            assertFalse(greets(State(Run.NEW, opens = FirstRun.MAX_OPENS + 1), motion, reader))
            // A panel that carries another app's text, and a screen too low for the welcome.
            assertFalse(greets(State(Run.NEW), motion, reader, plain = false))
            assertFalse(greets(State(Run.NEW), motion, reader, screenDp = 654f))
        }
        // A replay greets only where it asked for the piece.
        assertTrue(greets(FirstRun.replay(State(key = true), overture = true), motion = false))
        assertFalse(greets(FirstRun.replay(State(key = true), overture = false), motion = false))
    }

    @Test fun itBelongsToTheKeysStepAndToARunThatStands() {
        assertEquals(Overture.NONE, begins(State(Run.NEW, done = listOf("key"), key = true)))
        assertEquals(Overture.NONE, begins(State(Run.NEW, opens = FirstRun.MAX_OPENS + 1)))
        // A first opening that a lost focus cut short has not used it up: it was not marked as shown.
        assertEquals(Overture.WELCOME, begins(FirstRun.opened(State(Run.NEW))))
    }

    @Test fun aReplayHasItOnlyWhereItIsAskedFor() {
        assertEquals(Overture.WELCOME, begins(FirstRun.replay(State(key = true), overture = true)))
        assertEquals(Overture.NONE, begins(FirstRun.replay(State(key = true), overture = false)))
    }

    @Test fun theVeryFirstOpeningIsSlowOnce() {
        assertTrue(FirstRun.slow(State(Run.NEW), Overture.WELCOME))
        assertFalse(FirstRun.slow(State(Run.NEW), Overture.NONE))
        assertFalse(FirstRun.slow(FirstRun.replay(State(), overture = true), Overture.WELCOME))
        // Once it has been shown there is no opening piece, and so no Slow.
        val later = FirstRun.shown(State(Run.NEW))
        assertFalse(FirstRun.slow(later, begins(later)))
    }

    private val own = Keyboard(id = 3, external = false, quickInsert = true)
    private val plugged = Keyboard(id = 9, external = true, quickInsert = false)

    @Test fun aGooglebooksOwnKeyboardIsOfferedQuickInsert() {
        assertEquals(Key.QUICK_INSERT, FirstRun.suggest(FirstRun.hasQuickInsert(listOf(own), last = null)))
        assertEquals(Key.QUICK_INSERT, FirstRun.suggest(FirstRun.hasQuickInsert(listOf(plugged, own), last = null)))
    }

    @Test fun aKeyboardWithoutQuickInsertIsOfferedActionM() {
        // The last key came from the plugged-in keyboard: that is the one in front.
        assertEquals(Key.M, FirstRun.suggest(FirstRun.hasQuickInsert(listOf(own, plugged), last = 9)))
        // And back on the device's own.
        assertEquals(Key.QUICK_INSERT, FirstRun.suggest(FirstRun.hasQuickInsert(listOf(own, plugged), last = 3)))
        // Only a keyboard without the key, or none that is known.
        assertEquals(Key.M, FirstRun.suggest(FirstRun.hasQuickInsert(listOf(plugged), last = null)))
        assertEquals(Key.M, FirstRun.suggest(FirstRun.hasQuickInsert(emptyList(), last = null)))
        // A keyboard that has gone since its last key: the device's own decides.
        assertEquals(Key.QUICK_INSERT, FirstRun.suggest(FirstRun.hasQuickInsert(listOf(own), last = 9)))
    }

    @Test fun theOtherKeysToTry() {
        assertEquals(Key.M, FirstRun.other(Key.QUICK_INSERT))
        assertNull(FirstRun.other(Key.M))
    }
}
