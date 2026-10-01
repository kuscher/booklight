package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacesTest {
    private val area = Box(0, 41, 1920, 1137)          // the HP: status bar above, taskbar below
    private fun b(p: Place, a: Box = area) = Places.bounds(p, a)!!

    @Test fun noneIsTheSystems() = assertNull(Places.bounds(Place.NONE, area))

    @Test fun fullIsTheArea() = assertEquals(area, b(Place.FULL))

    @Test fun halvesMeetInTheMiddle() {
        assertEquals(Box(0, 41, 960, 1137), b(Place.LEFT))
        assertEquals(Box(960, 41, 1920, 1137), b(Place.RIGHT))
    }

    @Test fun thirdsTileTheWidth() {
        val l = b(Place.LEFT_THIRD); val m = b(Place.MIDDLE_THIRD); val r = b(Place.RIGHT_THIRD)
        assertEquals(area.left, l.left); assertEquals(l.right, m.left); assertEquals(m.right, r.left); assertEquals(area.right, r.right)
        assertEquals(640, l.width); assertEquals(640, m.width); assertEquals(640, r.width)
    }

    @Test fun aThirdAndTwoThirdsShareAnEdge() {
        assertEquals(b(Place.LEFT_THIRD).right, b(Place.RIGHT_TWO_THIRDS).left)
        assertEquals(b(Place.LEFT_TWO_THIRDS).right, b(Place.RIGHT_THIRD).left)
        assertEquals(area.right, b(Place.RIGHT_TWO_THIRDS).right)
    }

    @Test fun quartersTileTheArea() {
        val tl = b(Place.TOP_LEFT); val tr = b(Place.TOP_RIGHT); val bl = b(Place.BOTTOM_LEFT); val br = b(Place.BOTTOM_RIGHT)
        assertEquals(tl.right, tr.left); assertEquals(tl.bottom, bl.top); assertEquals(tr.bottom, br.top); assertEquals(bl.right, br.left)
        assertEquals(area.width * area.height, listOf(tl, tr, bl, br).sumOf { it.width * it.height })
    }

    @Test fun anOddWidthLosesNothing() {
        val odd = Box(0, 0, 1001, 701)
        assertEquals(1001, b(Place.LEFT, odd).width + b(Place.RIGHT, odd).width)
        assertEquals(1001, b(Place.LEFT_THIRD, odd).width + b(Place.MIDDLE_THIRD, odd).width + b(Place.RIGHT_THIRD, odd).width)
        assertEquals(701, b(Place.TOP_LEFT, odd).height + b(Place.BOTTOM_LEFT, odd).height)
    }

    @Test fun anAreaThatDoesNotStartAtZero() {
        val second = Box(2880, 100, 2880 + 1920, 100 + 1080)       // another display, to the right
        assertEquals(Box(2880, 100, 3840, 1180), b(Place.LEFT, second))
        assertEquals(Box(2880 + 1280, 100, 4800, 1180), b(Place.RIGHT_THIRD, second))
    }

    @Test fun centreIsInsideAndCentred() {
        val c = b(Place.CENTER)
        assertTrue(c.left > area.left && c.right < area.right && c.top > area.top && c.bottom < area.bottom)
        assertEquals(c.left - area.left, area.right - c.right)
        assertTrue(kotlin.math.abs((c.top - area.top) - (area.bottom - c.bottom)) <= 1)
    }

    @Test fun everyPlaceHasARectangleButNone() {
        for (p in Place.entries) if (p != Place.NONE) { val r = b(p); assertTrue("$p", r.width > 0 && r.height > 0) }
    }

    @Test fun recipesKeepTheirNumbers() {
        // Recipes save a place by its number: these three must never move.
        assertEquals(0, Place.NONE.ordinal); assertEquals(1, Place.LEFT.ordinal); assertEquals(2, Place.RIGHT.ordinal)
    }
}
