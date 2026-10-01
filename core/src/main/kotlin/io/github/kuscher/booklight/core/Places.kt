package io.github.kuscher.booklight.core

/** A rectangle on the screen, in pixels; [right] and [bottom] are outside it, as Android counts. */
data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * Where each [Place] is on the part of the screen the system's bars leave free. Plain arithmetic,
 * so it can be tested: thirds and halves share their edges exactly, nothing overlaps and nothing
 * is left over.
 */
object Places {
    /** The rectangle of [place] inside [area]; null for [Place.NONE] (the system decides). */
    fun bounds(place: Place, area: Box): Box? {
        val x1 = area.left + area.width / 3
        val x2 = area.left + area.width * 2 / 3
        val cx = area.left + area.width / 2
        val cy = area.top + area.height / 2
        fun cols(from: Int, to: Int) = Box(from, area.top, to, area.bottom)
        return when (place) {
            Place.NONE -> null
            Place.LEFT -> cols(area.left, cx)
            Place.RIGHT -> cols(cx, area.right)
            Place.LEFT_THIRD -> cols(area.left, x1)
            Place.MIDDLE_THIRD -> cols(x1, x2)
            Place.RIGHT_THIRD -> cols(x2, area.right)
            Place.LEFT_TWO_THIRDS -> cols(area.left, x2)
            Place.RIGHT_TWO_THIRDS -> cols(x1, area.right)
            Place.TOP_LEFT -> Box(area.left, area.top, cx, cy)
            Place.TOP_RIGHT -> Box(cx, area.top, area.right, cy)
            Place.BOTTOM_LEFT -> Box(area.left, cy, cx, area.bottom)
            Place.BOTTOM_RIGHT -> Box(cx, cy, area.right, area.bottom)
            // A window for one thing: three fifths of the width, four fifths of the height, in the middle.
            Place.CENTER -> {
                val w = area.width * 3 / 5
                val h = area.height * 4 / 5
                val l = area.left + (area.width - w) / 2
                val t = area.top + (area.height - h) / 2
                Box(l, t, l + w, t + h)
            }
            Place.FULL -> area
        }
    }
}
