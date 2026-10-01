package io.github.kuscher.booklight.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor

/**
 * The quick calculator: `12*3.5`, `(4+5)^2`, `20% of 150`, `150 + 20%`, `sqrt(2)`, `2 pi`.
 *
 * Small on purpose. Booklight answers a sum while you type; units, money, dates and time zones
 * are Summa's job (see docs/PLAN.md, "Summa inside").
 */
object Calc {
    /** The answer as text, or null when [input] isn't a calculation (plain words, a lone number). */
    fun answer(input: String): String? {
        val src = input.trim().removePrefix("=").trim()
        if (src.isEmpty() || src.length > 200) return null
        val p = Parser(src)
        val v = try { p.parse() } catch (_: Stop) { return null }
        if (!p.worked || v.isNaN() || v.isInfinite()) return null
        return format(v)
    }

    fun format(v: Double): String {
        if (v == 0.0) return "0"
        val a = abs(v)
        if (a >= 1e15 || a < 1e-9) {
            val s = String.format("%.6e", v)
            val (m, e) = s.split('e')
            return m.trimEnd('0').trimEnd('.') + "e" + e.toInt()
        }
        // 12 significant digits hides binary noise (0.1 + 0.2), then trailing zeros go.
        val bd = BigDecimal(v).round(MathContext(12, RoundingMode.HALF_EVEN)).stripTrailingZeros()
        val plain = bd.toPlainString()
        val neg = plain.startsWith("-")
        val digits = plain.removePrefix("-")
        val whole = digits.substringBefore('.')
        val frac = digits.substringAfter('.', "")
        val grouped = if (whole.length > 4) whole.reversed().chunked(3).joinToString(",").reversed() else whole
        return (if (neg) "-" else "") + grouped + (if (frac.isNotEmpty()) ".$frac" else "")
    }

    private class Stop : Exception() {
        override fun fillInStackTrace(): Throwable = this
    }

    private class Parser(private val s: String) {
        private var i = 0
        /** True once something was calculated: an operator or a function was used. */
        var worked = false

        fun parse(): Double {
            val v = sum()
            ws()
            if (i < s.length) throw Stop()
            return v
        }

        private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        private fun eat(c: Char): Boolean { ws(); if (i < s.length && s[i] == c) { i++; return true }; return false }
        private fun word(w: String): Boolean {
            ws()
            if (!s.startsWith(w, i, ignoreCase = true)) return false
            val end = i + w.length
            if (end < s.length && s[end].isLetter()) return false
            i = end
            return true
        }

        // A trailing percent on the right of + or − means "of the left side": 150 + 20% = 180.
        private fun sum(): Double {
            var v = product()
            while (true) {
                val plus = eat('+')
                if (!plus && !(eat('-') || eat('−'))) return v
                val start = i
                val r = product()
                val pct = percentAt(start)
                val rhs = if (pct != null) v * pct / 100 else r
                v = if (plus) v + rhs else v - rhs
                worked = true
            }
        }

        /** If the term that began at [start] was just `N%`, its N. */
        private fun percentAt(start: Int): Double? {
            val t = s.substring(start, i).trim()
            if (!t.endsWith("%")) return null
            return t.dropLast(1).trim().replace(",", "").toDoubleOrNull()
        }

        private fun product(): Double {
            var v = power()
            while (true) {
                ws()
                when {
                    eat('*') || eat('×') || eat('·') -> { v *= power(); worked = true }
                    eat('/') || eat('÷') -> { v /= power(); worked = true }
                    word("of") -> { v *= power(); worked = true }            // 20% of 150
                    word("mod") -> { val d = power(); v -= d * floor(v / d); worked = true }
                    // "2 pi", "3(4+1)": a value right after a value multiplies.
                    i < s.length && (s[i] == '(' || s[i].isLetter()) -> { v *= power(); worked = true }
                    else -> return v
                }
            }
        }

        private fun power(): Double {
            val base = unary()
            if (eat('^')) { worked = true; return Math.pow(base, power()) }
            return base
        }

        private fun unary(): Double {
            if (eat('-') || eat('−')) return -unary()
            if (eat('+')) return unary()
            return postfix()
        }

        private fun postfix(): Double {
            var v = atom()
            while (true) {
                if (eat('%')) { v /= 100; continue }   // counts as work only with an operator: "20%" alone is no sum
                if (eat('!')) { v = factorial(v); worked = true; continue }
                return v
            }
        }

        private fun factorial(v: Double): Double {
            if (v < 0 || v != floor(v) || v > 170) throw Stop()
            var r = 1.0
            for (k in 2..v.toInt()) r *= k
            return r
        }

        private fun atom(): Double {
            ws()
            if (eat('(')) { val v = sum(); if (!eat(')')) throw Stop(); return v }
            if (i < s.length && (s[i].isDigit() || s[i] == '.')) return number()
            val start = i
            while (i < s.length && s[i].isLetter()) i++
            val name = s.substring(start, i).lowercase()
            if (name.isEmpty()) throw Stop()
            CONSTANTS[name]?.let { return it }   // a constant alone is no sum: typing "e" finds apps
            val f = FUNCTIONS[name] ?: throw Stop()
            if (!eat('(')) throw Stop()
            val arg = sum()
            if (!eat(')')) throw Stop()
            worked = true
            return f(arg)
        }

        /** 1234.5, 1,234.5, .5, 1e6. A comma is a thousands separator when groups of three follow it. */
        private fun number(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.' || (s[i] == ',' && i > start && groupOfThree(i + 1)))) i++
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                val save = i
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                if (i < s.length && s[i].isDigit()) { while (i < s.length && s[i].isDigit()) i++ } else i = save
            }
            return s.substring(start, i).replace(",", "").toDoubleOrNull() ?: throw Stop()
        }

        private fun groupOfThree(from: Int): Boolean =
            from + 2 < s.length && (0..2).all { s[from + it].isDigit() } && (from + 3 >= s.length || !s[from + 3].isDigit())
    }

    private val CONSTANTS = mapOf("pi" to Math.PI, "π" to Math.PI, "e" to Math.E, "tau" to 2 * Math.PI)
    private val FUNCTIONS: Map<String, (Double) -> Double> = mapOf(
        "sqrt" to Math::sqrt, "cbrt" to Math::cbrt, "abs" to { x -> abs(x) },
        "sin" to Math::sin, "cos" to Math::cos, "tan" to Math::tan,
        "asin" to Math::asin, "acos" to Math::acos, "atan" to Math::atan,
        "ln" to Math::log, "log" to Math::log10, "exp" to Math::exp,
        "round" to { x -> Math.rint(x) }, "floor" to Math::floor, "ceil" to Math::ceil,
    )
}
