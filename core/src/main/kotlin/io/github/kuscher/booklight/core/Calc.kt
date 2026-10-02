package io.github.kuscher.booklight.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor

/**
 * The quick calculator: `12*3.5`, `(4+5)^2`, `20% of 150`, `150 + 20%`, `sqrt(2)`, `2 pi`.
 *
 * Numbers are read the way they are written where the user lives. With `comma` (German, Danish,
 * French…) one and a half is `1,5` and a thousand `1.000`, and answers are written that way; without
 * it, `1.5` and `1,000`. Either way the other sign is understood where it can only mean one thing:
 * `12*3,5` is 42 in English and `12*3.5` is 42 in German ([Parser.plain] has the rule).
 *
 * Small on purpose. Booklight answers a sum while you type; units, money, dates and time zones
 * are Summa's job (see docs/PLAN.md, "Summa inside").
 */
object Calc {
    /** The answer as text, or null when [input] isn't a calculation (plain words, a lone number). [comma]: the user's language writes 1,5. */
    fun answer(input: String, comma: Boolean = false): String? {
        val src = input.trim().removePrefix("=").trim()
        if (src.isEmpty() || src.length > 200) return null
        val p = Parser(src, if (comma) ',' else '.')
        val v = try { p.parse() } catch (_: Stop) { return null }
        if (!p.worked || v.isNaN() || v.isInfinite()) return null
        return format(v, comma)
    }

    /** [v] as the user writes numbers: thousands in groups from five digits on, the decimal sign of their language. */
    fun format(v: Double, comma: Boolean = false): String {
        val text = english(v)
        return if (comma) buildString(text.length) { for (c in text) append(when (c) { ',' -> '.'; '.' -> ','; else -> c }) } else text
    }

    /** An answer of [format] without its groups: what is copied, so that it pastes as a number. */
    fun plain(answer: String, comma: Boolean = false): String = answer.replace(if (comma) "." else ",", "")

    private fun english(v: Double): String {
        if (v == 0.0) return "0"
        val a = abs(v)
        if (a >= 1e15 || a < 1e-9) {
            val s = String.format(java.util.Locale.ROOT, "%.6e", v)
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

    private class Parser(private val s: String, /** The decimal sign of the user's language. */ private val point: Char) {
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
            return plain(t.dropLast(1).trim())?.toDoubleOrNull()
        }

        private fun product(): Double {
            var v = unary()
            while (true) {
                ws()
                when {
                    eat('*') || eat('×') || eat('·') -> { v *= unary(); worked = true }
                    eat('/') || eat('÷') -> { v /= unary(); worked = true }
                    word("of") -> { v *= unary(); worked = true }            // 20% of 150
                    word("mod") -> { val d = unary(); v -= d * floor(v / d); worked = true }
                    // "2 pi", "3(4+1)": a value right after a value multiplies.
                    i < s.length && (s[i] == '(' || s[i].isLetter()) -> { v *= unary(); worked = true }
                    else -> return v
                }
            }
        }

        // A minus in front applies to the whole power: -2^2 is -(2^2), and 2^-2 still works.
        private fun unary(): Double {
            if (eat('-') || eat('−')) return -unary()
            if (eat('+')) return unary()
            return power()
        }

        private fun power(): Double {
            val base = postfix()
            if (eat('^')) { worked = true; return Math.pow(base, unary()) }
            return base
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
            if (i < s.length && (s[i].isDigit() || s[i] == '.' || (s[i] == ',' && i + 1 < s.length && s[i + 1].isDigit()))) return number()
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

        /** 1234.5, 1,234.5, 1.234,5, .5, 1e6: digits with their signs, then an exponent. */
        private fun number(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.' || (s[i] == ',' && i + 1 < s.length && s[i + 1].isDigit()))) i++
            val digits = plain(s.substring(start, i)) ?: throw Stop()
            val from = i
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                val save = i
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                if (i < s.length && s[i].isDigit()) { while (i < s.length && s[i].isDigit()) i++ } else i = save
            }
            return (digits + s.substring(from, i)).toDoubleOrNull() ?: throw Stop()
        }

        /**
         * A number's digits with a point for its decimal sign and no groups, or null when its signs make no number.
         *
         * Both signs in it: the one that comes last is the decimal sign, in any language (1,234.5 and 1.234,5). One
         * sign, once: the language's own decimal sign is just that (1.234 is a little over one in English, 1,234 in
         * German); the other one separates thousands if it can (1,234 in English, 1.234 in German), and is the
         * decimal sign where it can't (3,5 in English, 3.5 and 0.125 in German). One sign several times: thousands.
         */
        private fun plain(t: String): String? {
            val dots = t.count { it == '.' }
            val commas = t.count { it == ',' }
            if (dots + commas == 0) return t
            val only = if (dots > 0) '.' else ','
            val decimal: Char? = when {
                dots > 0 && commas > 0 -> if (t.lastIndexOf('.') > t.lastIndexOf(',')) '.' else ','
                dots + commas > 1 -> null
                only == point || !groups(t, only) -> only
                else -> null
            }
            if (decimal == null) return if (groups(t, only)) t.replace(only.toString(), "") else null
            if (t.count { it == decimal } != 1) return null
            val group = if (decimal == '.') ',' else '.'
            val whole = t.substringBefore(decimal)
            val part = t.substringAfter(decimal)
            if (group in part || (group in whole && !groups(whole, group))) return null
            return whole.replace(group.toString(), "") + "." + part
        }

        /** [t] is digits in thousands: one to three of them, not a lone or a leading 0, then threes. */
        private fun groups(t: String, sign: Char): Boolean {
            val parts = t.split(sign)
            val head = parts[0]
            return parts.size > 1 && head.length in 1..3 && head.all(Char::isDigit) && head[0] != '0' &&
                parts.drop(1).all { it.length == 3 && it.all(Char::isDigit) }
        }
    }

    private val CONSTANTS = mapOf("pi" to Math.PI, "π" to Math.PI, "e" to Math.E, "tau" to 2 * Math.PI)
    private val FUNCTIONS: Map<String, (Double) -> Double> = mapOf(
        "sqrt" to Math::sqrt, "cbrt" to Math::cbrt, "abs" to { x -> abs(x) },
        "sin" to Math::sin, "cos" to Math::cos, "tan" to Math::tan,
        "asin" to Math::asin, "acos" to Math::acos, "atan" to Math::atan,
        "ln" to Math::log, "log" to Math::log10, "exp" to Math::exp,
        "round" to { x -> floor(x + 0.5) }, "floor" to Math::floor, "ceil" to Math::ceil,
    )
}
