package io.github.kuscher.booklight.pin

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.StaticLayout
import android.text.TextPaint
import io.github.kuscher.booklight.R
import java.io.File

/**
 * What the pinned window shows. [kind]: `text`, `answer`, `color`, `qr`, `timer`, `flight`. [text] is what it
 * shows (a colour: its hex; a flight: the line that Copy gives), [note] a line with it (the sum; a timer's
 * label; a flight: the number as typed and the day it leaves, "LH455 2026-10-01"), [value] a number
 * with it (a colour's ARGB; a timer's seconds), [until] when a timer ends, by the wall clock.
 */
data class Pinned(val kind: String, val text: String, val value: Long = 0, val note: String = "", val until: Long = 0) {
    fun bundle() = Bundle().apply { putString(KIND, kind); putString(TEXT, text); putLong(VALUE, value); putString(NOTE, note); putLong(UNTIL, until) }

    /** One line to know it by, in a list. */
    fun title(context: Context): String = when (kind) {
        "timer" -> listOf(note, context.getString(R.string.pin_kind_timer)).first { it.isNotEmpty() }
        "answer" -> if (note.isEmpty()) text else "$note = $text"
        else -> text.trim().replace('\n', ' ')
    }

    /** What the system calls the window where it names windows. */
    fun label(context: Context): String = context.getString(when (kind) {
        "timer" -> R.string.pin_kind_timer; "answer" -> R.string.pin_kind_answer; "color" -> R.string.pin_kind_color; "qr" -> R.string.pin_kind_qr
        "flight" -> R.string.pin_kind_flight
        else -> R.string.pin_kind_text
    })

    /**
     * The size the content is drawn for, in dp. The window opens at this size (see the activity's
     * `<layout>` in the manifest: its smallest size is what the system opens a picture-in-picture
     * window at) and keeps this shape when the user makes it larger. Never wider than 2.39 to 1:
     * the system takes no flatter shape.
     */
    fun size(context: Context): Pair<Int, Int> = when (kind) {
        "color" -> WIDTH to 140
        "qr" -> 216 to 216
        "text" -> WIDTH to maxOf(LOW, 40 + LINE * lines(context))
        else -> WIDTH to LOW
    }

    /** How many lines [text] takes in the window, as near as can be told before it is laid out. */
    private fun lines(context: Context): Int {
        val density = context.resources.displayMetrics.density
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = 17f * density
            FONT.takeIf { it.canRead() }?.let { runCatching { typeface = Typeface.Builder(it).setFontVariationSettings("'wght' 500").build() } }
        }
        val width = ((WIDTH - 2 * MARGIN) * density).toInt()
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width).build().lineCount.coerceIn(1, MAX_LINES)
    }

    companion object {
        const val KIND = "kind"; const val TEXT = "text"; const val VALUE = "value"; const val NOTE = "note"; const val UNTIL = "until"
        /** As wide as the manifest's smallest width for the window, and as low as its smallest height. */
        const val WIDTH = 280
        const val LOW = 118
        const val MARGIN = 20
        const val LINE = 24
        const val MAX_LINES = 6
        private val FONT = File("/product/fonts/GoogleSansFlex-Regular.ttf")

        fun from(b: Bundle?): Pinned? {
            val kind = b?.getString(KIND) ?: return null
            return Pinned(kind, b.getString(TEXT).orEmpty(), b.getLong(VALUE), b.getString(NOTE).orEmpty(), b.getLong(UNTIL))
        }
    }
}
