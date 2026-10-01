package io.github.kuscher.booklight.core

import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt

/** A colour as sRGB, each value 0 to 255, and the four ways of writing it that can be copied. */
data class Rgb(val r: Int, val g: Int, val b: Int) {
    /** As Android packs a colour, opaque. */
    val argb: Int get() = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    fun hex(): String = String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b)

    fun rgb(): String = "rgb($r, $g, $b)"

    fun hsl(): String {
        val rf = r / 255.0
        val gf = g / 255.0
        val bf = b / 255.0
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val l = (max + min) / 2
        val d = max - min
        if (d == 0.0) return "hsl(0, 0%, ${(l * 100).roundToInt()}%)"
        val s = d / (1 - abs(2 * l - 1))
        val h = 60 * when (max) {
            rf -> (gf - bf) / d
            gf -> (bf - rf) / d + 2
            else -> (rf - gf) / d + 4
        }
        return "hsl(${turn(h)}, ${(s * 100).roundToInt()}%, ${(l * 100).roundToInt()}%)"
    }

    /** CSS Color 4: sRGB to linear light, to OKLab (Björn Ottosson's matrices), to lightness, chroma and hue. */
    fun oklch(): String {
        val lr = linear(r)
        val lg = linear(g)
        val lb = linear(b)
        val l = cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb)
        val m = cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb)
        val s = cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb)
        val light = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val b2 = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        val chroma = hypot(a, b2)
        // A grey has no hue; what the arithmetic leaves there is noise.
        val hue = if (chroma < 0.005) 0 else turn(Math.toDegrees(atan2(b2, a)))
        return String.format(Locale.ROOT, "oklch(%.2f %.2f %d)", light, chroma, hue)
    }

    private fun linear(v: Int): Double {
        val c = v / 255.0
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    /** Degrees as a whole number from 0 to 359. */
    private fun turn(degrees: Double): Int = ((degrees.roundToInt() % 360) + 360) % 360
}

/** Reads a colour as typed: #rgb, #rrggbb, rgb(52, 120, 246), hsl(219, 91%, 58%). */
object Colors {
    /** The colour, or null when [text] isn't one or a value is out of range. Case and spaces don't matter. */
    fun parse(text: String): Rgb? {
        val t = text.trim().lowercase()
        if (t.length > 64) return null
        if (t.startsWith("#")) return hex(t.substring(1).trim())
        val open = t.indexOf('(')
        if (open < 0 || !t.endsWith(")")) return null
        val inner = t.substring(open + 1, t.length - 1)
        return when (t.substring(0, open).trim()) {
            "rgb" -> rgb(values(inner))
            "hsl" -> hsl(values(inner.replace("%", " ").replace("deg", " ").replace("°", " ")))
            else -> null
        }
    }

    private fun values(inner: String): List<String> = inner.split(',', ' ', '\t').filter { it.isNotEmpty() }

    private fun hex(s: String): Rgb? {
        if ((s.length != 3 && s.length != 6) || s.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
        val v = (if (s.length == 3) s.map { "$it$it" }.joinToString("") else s).toInt(16)
        return Rgb(v shr 16, (v shr 8) and 0xFF, v and 0xFF)
    }

    private fun rgb(v: List<String>): Rgb? {
        if (v.size != 3) return null
        val n = v.map { it.toIntOrNull()?.takeIf { c -> c in 0..255 } ?: return null }
        return Rgb(n[0], n[1], n[2])
    }

    private fun hsl(v: List<String>): Rgb? {
        if (v.size != 3) return null
        val h = v[0].toDoubleOrNull()?.takeIf { it >= 0 && it <= 360 } ?: return null
        val s = (v[1].toDoubleOrNull()?.takeIf { it >= 0 && it <= 100 } ?: return null) / 100
        val l = (v[2].toDoubleOrNull()?.takeIf { it >= 0 && it <= 100 } ?: return null) / 100
        val c = (1 - abs(2 * l - 1)) * s
        val sixth = h % 360 / 60
        val x = c * (1 - abs(sixth % 2 - 1))
        val (r, g, b) = when (sixth.toInt()) {
            0 -> Triple(c, x, 0.0)
            1 -> Triple(x, c, 0.0)
            2 -> Triple(0.0, c, x)
            3 -> Triple(0.0, x, c)
            4 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        val m = l - c / 2
        fun byte(f: Double) = ((f + m) * 255).roundToInt().coerceIn(0, 255)
        return Rgb(byte(r), byte(g), byte(b))
    }
}
