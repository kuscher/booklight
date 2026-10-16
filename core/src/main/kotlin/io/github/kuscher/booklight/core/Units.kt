package io.github.kuscher.booklight.core

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Result of a deterministic physical unit conversion (`72f in c`, `5 ft 10 in cm`, `16 oz in ml`, `65 mph in kmh`).
 */
data class UnitAnswer(
    /** Formatted target value and unit symbol, e.g. `"22.22 °C"` or `"177.8 cm"`. */
    val formatted: String,
    /** Target number alone for pasting into a calculation or table, e.g. `"22.22"`. */
    val plainNumber: String,
    /** Normalized source side, e.g. `"72 °F"` or `"5 ft 10 in"`. */
    val source: String,
)

/**
 * Physical unit conversions in the search field: temperature, length, weight/mass, volume/cooking,
 * speed, area, data size, and duration. Requires an explicit connector (`in`, `to`, `into`, `as`,
 * `->`, `→`, `nach`, `zu`, `als`) between compatible units so everyday queries never collide.
 */
object Units {
    private enum class Dim { TEMP, LENGTH, MASS, VOLUME, SPEED, AREA, DATA, DURATION }

    private class UnitDef(
        val dim: Dim,
        val symbol: String,
        /** Factor to the dimension's base unit (unused for [Dim.TEMP]). */
        val toBase: Double = 1.0,
        val tempId: Char = ' ',
    )

    private val CONNECTORS = listOf("into", "nach", "als", "in", "to", "as", "zu", "->", "→")

    // Compound feet + inches shorthand: 5'10", 5' 10", 5′10″
    private val FEET_INCHES_QUOTES = Regex(
        """^\s*(\d+(?:[.,]\d+)?)\s*['′]\s*(\d+(?:[.,]\d+)?)?\s*(?:["″]|in|inch|inches)?\s+(?:in|to|into|as|nach|zu|als|->|→)\s+([a-zA-ZäöüßÄÖÜ°²/ ]+?)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    // Compound feet + inches in words: "5 ft 10 in cm", "5 ft 10 in to cm", "5 ft 10 in in cm"
    private val FEET_INCHES_WORDS = Regex(
        """^\s*(\d+(?:[.,]\d+)?)\s*(?:ft|foot|feet|fuß|fuss)\s+(\d+(?:[.,]\d+)?)\s*(?:(?:in|inch|inches)\s+(?:in|to|into|as|nach|zu|als|->|→)|in|to|into|nach|zu|->|→)\s+([a-zA-ZäöüßÄÖÜ°²/ ]+?)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    // Compound pounds + ounces: "8 lb 4 oz in kg", "8 lbs 4 oz to g"
    private val POUNDS_OUNCES = Regex(
        """^\s*(\d+(?:[.,]\d+)?)\s*(?:lb|lbs|pound|pounds)\s+(\d+(?:[.,]\d+)?)\s*(?:oz|ounce|ounces)\s+(?:in|to|into|as|nach|zu|als|->|→)\s+([a-zA-ZäöüßÄÖÜ°²/ ]+?)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    fun convert(input: String, comma: Boolean = false): UnitAnswer? {
        val src = input.trim().removePrefix("=").trim()
        if (src.length < 4 || src.length > 80) return null

        FEET_INCHES_QUOTES.matchEntire(src)?.let { m ->
            val ft = parseNum(m.groupValues[1], comma) ?: return null
            val inches = m.groupValues[2].takeIf { it.isNotEmpty() }?.let { parseNum(it, comma) } ?: 0.0
            val to = findUnit(m.groupValues[3]) ?: return null
            if (to.dim != Dim.LENGTH) return null
            val meters = ft * 0.3048 + inches * 0.0254
            val srcLabel = if (inches == 0.0) "${fmt(ft, comma)} ft" else "${fmt(ft, comma)} ft ${fmt(inches, comma)} in"
            return buildLengthOrGeneral(meters, to, srcLabel, comma)
        }

        FEET_INCHES_WORDS.matchEntire(src)?.let { m ->
            val ft = parseNum(m.groupValues[1], comma) ?: return null
            val inches = parseNum(m.groupValues[2], comma) ?: return null
            val to = findUnit(m.groupValues[3]) ?: return null
            if (to.dim != Dim.LENGTH) return null
            val meters = ft * 0.3048 + inches * 0.0254
            val srcLabel = "${fmt(ft, comma)} ft ${fmt(inches, comma)} in"
            return buildLengthOrGeneral(meters, to, srcLabel, comma)
        }

        POUNDS_OUNCES.matchEntire(src)?.let { m ->
            val lbs = parseNum(m.groupValues[1], comma) ?: return null
            val oz = parseNum(m.groupValues[2], comma) ?: return null
            val to = findUnit(m.groupValues[3]) ?: return null
            if (to.dim != Dim.MASS) return null
            val grams = lbs * 453.59237 + oz * 28.349523125
            val targetVal = grams / to.toBase
            val num = fmt(targetVal, comma)
            return UnitAnswer("$num ${to.symbol}", Calc.plain(num, comma), "${fmt(lbs, comma)} lb ${fmt(oz, comma)} oz")
        }

        // Standard form: <number> <from-unit> <connector> <to-unit>
        val split = splitConnector(src) ?: return null
        val (left, right) = split
        var to = findUnit(right) ?: return null
        val (value, fromText) = splitNumberAndUnit(left, comma) ?: return null
        var from = findUnit(fromText) ?: return null
        if (from.dim != to.dim) {
            // People write "16 oz in ml" for fluid ounces when the other side is a volume.
            val flOz = UnitDef(Dim.VOLUME, "fl oz", toBase = 29.5735295625)
            if (from.symbol == "oz" && to.dim == Dim.VOLUME) from = flOz
            else if (to.symbol == "oz" && from.dim == Dim.VOLUME) to = flOz
            else return null
        }

        val sourceText = "${fmt(value, comma)} ${from.symbol}"
        if (from.dim == Dim.TEMP) {
            val targetVal = convertTemp(value, from.tempId, to.tempId)
            val num = fmt(targetVal, comma)
            return UnitAnswer("$num ${to.symbol}", Calc.plain(num, comma), sourceText)
        }

        val base = value * from.toBase
        return buildLengthOrGeneral(base, to, sourceText, comma)
    }

    private fun buildLengthOrGeneral(base: Double, to: UnitDef, sourceText: String, comma: Boolean): UnitAnswer {
        val targetVal = base / to.toBase
        val num = fmt(targetVal, comma)
        // When converting to feet and the result has a fractional part, also show feet + inches in the subtitle.
        val extra = if (to.dim == Dim.LENGTH && to.symbol == "ft" && targetVal > 0 && abs(targetVal - floor(targetVal)) >= 0.04) {
            val totalInches = (base / 0.0254).roundToInt()
            val wholeFt = totalInches / 12
            val remIn = totalInches % 12
            if (remIn > 0) " · $wholeFt ft $remIn in" else ""
        } else ""
        return UnitAnswer("$num ${to.symbol}", Calc.plain(num, comma), sourceText + extra)
    }

    private fun convertTemp(v: Double, from: Char, to: Char): Double {
        val c = when (from) {
            'C' -> v
            'F' -> (v - 32.0) * 5.0 / 9.0
            'K' -> v - 273.15
            else -> v
        }
        return when (to) {
            'C' -> c
            'F' -> c * 9.0 / 5.0 + 32.0
            'K' -> c + 273.15
            else -> c
        }
    }

    private fun splitConnector(s: String): Pair<String, String>? {
        for (c in CONNECTORS) {
            val token = if (c == "->" || c == "→") c else " $c "
            val idx = s.lastIndexOf(token, ignoreCase = true)
            if (idx > 0) {
                val left = s.substring(0, idx).trim()
                val right = s.substring(idx + token.length).trim()
                if (left.isNotEmpty() && right.isNotEmpty()) return left to right
            }
        }
        return null
    }

    private fun splitNumberAndUnit(left: String, comma: Boolean): Pair<Double, String>? {
        val t = left.trim()
        var i = 0
        if (i < t.length && (t[i] == '-' || t[i] == '−' || t[i] == '+')) i++
        val numStart = i
        while (i < t.length && (t[i].isDigit() || t[i] == '.' || t[i] == ',')) i++
        if (i == numStart || i >= t.length) return null
        val numStr = t.substring(0, i).replace('−', '-')
        val unitStr = t.substring(i).trim()
        if (unitStr.isEmpty()) return null
        val v = parseNum(numStr, comma) ?: return null
        return v to unitStr
    }

    private fun parseNum(raw: String, comma: Boolean): Double? =
        Calc.answer("$raw + 0", comma)?.let { Calc.plain(it, comma).replace(',', '.').toDoubleOrNull() }

    /** Clean formatting for unit answers: up to 2 decimals for everyday numbers, up to 4 for small ones. */
    private fun fmt(v: Double, comma: Boolean): String {
        if (v.isNaN() || v.isInfinite()) return "0"
        val a = abs(v)
        val rounded = when {
            a == 0.0 -> 0.0
            a >= 100.0 -> kotlin.math.round(v * 100.0) / 100.0
            a >= 1.0 -> kotlin.math.round(v * 100.0) / 100.0
            a >= 0.01 -> kotlin.math.round(v * 10_000.0) / 10_000.0
            else -> v
        }
        return Calc.format(rounded, comma)
    }

    private fun findUnit(raw: String): UnitDef? {
        val key = raw.trim().lowercase()
            .removePrefix("degrees ").removePrefix("degree ").removePrefix("deg ")
            .removePrefix("grad ")
            .replace(Regex("\\s+"), " ")
        return UNITS[key] ?: UNITS[key.removePrefix("°").trim()]
    }

    private val UNITS: Map<String, UnitDef> = buildMap {
        fun add(dim: Dim, symbol: String, toBase: Double, vararg names: String) {
            val def = UnitDef(dim, symbol, toBase = toBase)
            for (n in names) put(n.lowercase(), def)
        }
        fun temp(symbol: String, id: Char, vararg names: String) {
            val def = UnitDef(Dim.TEMP, symbol, tempId = id)
            for (n in names) put(n.lowercase(), def)
        }

        // Temperature
        temp("°C", 'C', "c", "°c", "celsius")
        temp("°F", 'F', "f", "°f", "fahrenheit")
        temp("K", 'K', "k", "°k", "kelvin")

        // Length (base: m)
        add(Dim.LENGTH, "mm", 0.001, "mm", "millimeter", "millimeters", "millimetre", "millimetres")
        add(Dim.LENGTH, "cm", 0.01, "cm", "centimeter", "centimeters", "centimetre", "centimetres", "zentimeter")
        add(Dim.LENGTH, "m", 1.0, "m", "meter", "meters", "metre", "metres")
        add(Dim.LENGTH, "km", 1000.0, "km", "kilometer", "kilometers", "kilometre", "kilometres")
        add(Dim.LENGTH, "in", 0.0254, "in", "inch", "inches", "zoll", "\"", "″")
        add(Dim.LENGTH, "ft", 0.3048, "ft", "foot", "feet", "fuss", "fuß", "'", "′")
        add(Dim.LENGTH, "yd", 0.9144, "yd", "yard", "yards")
        add(Dim.LENGTH, "mi", 1609.344, "mi", "mile", "miles", "meile", "meilen")
        add(Dim.LENGTH, "nmi", 1852.0, "nmi", "sm", "nautical mile", "nautical miles", "seemeile", "seemeilen")

        // Mass (base: g)
        add(Dim.MASS, "mg", 0.001, "mg", "milligram", "milligrams", "milligramm")
        add(Dim.MASS, "g", 1.0, "g", "gram", "grams", "gramm")
        add(Dim.MASS, "kg", 1000.0, "kg", "kilo", "kilos", "kilogram", "kilograms", "kilogramm")
        add(Dim.MASS, "t", 1_000_000.0, "t", "tonne", "tonnes", "ton", "tons", "tonnen")
        add(Dim.MASS, "oz", 28.349523125, "oz", "ounce", "ounces", "unze", "unzen")
        add(Dim.MASS, "lb", 453.59237, "lb", "lbs", "pound", "pounds", "pfund")
        add(Dim.MASS, "st", 6350.29318, "st", "stone", "stones")

        // Volume (base: ml)
        add(Dim.VOLUME, "ml", 1.0, "ml", "milliliter", "milliliters", "millilitre", "millilitres")
        add(Dim.VOLUME, "cl", 10.0, "cl", "centiliter", "centiliters", "zentiliter")
        add(Dim.VOLUME, "dl", 100.0, "dl", "deciliter", "deciliters", "deziliter")
        add(Dim.VOLUME, "L", 1000.0, "l", "liter", "liters", "litre", "litres")
        add(Dim.VOLUME, "tsp", 4.92892159375, "tsp", "teaspoon", "teaspoons", "tl", "teelöffel", "teeloeffel")
        add(Dim.VOLUME, "tbsp", 14.78676478125, "tbsp", "tablespoon", "tablespoons", "el", "esslöffel", "essloeffel")
        add(Dim.VOLUME, "fl oz", 29.5735295625, "fl oz", "floz", "fluid ounce", "fluid ounces")
        add(Dim.VOLUME, "cups", 236.5882365, "cup", "cups", "tasse", "tassen")
        add(Dim.VOLUME, "pt", 473.176473, "pt", "pint", "pints")
        add(Dim.VOLUME, "qt", 946.352946, "qt", "quart", "quarts")
        add(Dim.VOLUME, "gal", 3785.411784, "gal", "gallon", "gallons", "gallone", "gallonen")

        // Speed (base: m/s)
        add(Dim.SPEED, "m/s", 1.0, "m/s", "mps")
        add(Dim.SPEED, "km/h", 1.0 / 3.6, "km/h", "kmh", "kph")
        add(Dim.SPEED, "mph", 0.44704, "mph")
        add(Dim.SPEED, "kn", 1852.0 / 3600.0, "kn", "kt", "kts", "knot", "knots", "knoten")

        // Area (base: m²)
        add(Dim.AREA, "mm²", 1e-6, "mm2", "mm²", "sq mm")
        add(Dim.AREA, "cm²", 1e-4, "cm2", "cm²", "sq cm")
        add(Dim.AREA, "m²", 1.0, "m2", "m²", "sqm", "sq m", "qm", "quadratmeter")
        add(Dim.AREA, "km²", 1e6, "km2", "km²", "sq km", "qkm", "quadratkilometer")
        add(Dim.AREA, "in²", 0.00064516, "in2", "in²", "sq in")
        add(Dim.AREA, "ft²", 0.09290304, "ft2", "ft²", "sqft", "sq ft", "quadratfuß", "quadratfuss")
        add(Dim.AREA, "yd²", 0.83612736, "yd2", "yd²", "sq yd")
        add(Dim.AREA, "acres", 4046.8564224, "acre", "acres")
        add(Dim.AREA, "ha", 10_000.0, "ha", "hectare", "hectares", "hektar")
        add(Dim.AREA, "mi²", 2_589_988.110336, "mi2", "mi²", "sq mi")

        // Data (base: B)
        add(Dim.DATA, "B", 1.0, "b", "byte", "bytes")
        add(Dim.DATA, "KB", 1e3, "kb", "kilobyte", "kilobytes")
        add(Dim.DATA, "MB", 1e6, "mb", "megabyte", "megabytes")
        add(Dim.DATA, "GB", 1e9, "gb", "gigabyte", "gigabytes")
        add(Dim.DATA, "TB", 1e12, "tb", "terabyte", "terabytes")
        add(Dim.DATA, "KiB", 1024.0, "kib", "kibibyte", "kibibytes")
        add(Dim.DATA, "MiB", 1048576.0, "mib", "mebibyte", "mebibytes")
        add(Dim.DATA, "GiB", 1073741824.0, "gib", "gibibyte", "gibibytes")
        add(Dim.DATA, "TiB", 1099511627776.0, "tib", "tebibyte", "tebibytes")

        // Duration (base: s)
        add(Dim.DURATION, "ms", 0.001, "ms", "millisecond", "milliseconds", "millisekunde", "millisekunden")
        add(Dim.DURATION, "s", 1.0, "s", "sec", "secs", "second", "seconds", "sek", "sekunde", "sekunden")
        add(Dim.DURATION, "min", 60.0, "min", "mins", "minute", "minutes", "minuten")
        add(Dim.DURATION, "h", 3600.0, "h", "hr", "hrs", "hour", "hours", "std", "stunde", "stunden")
        add(Dim.DURATION, "d", 86400.0, "d", "day", "days", "tag", "tage")
        add(Dim.DURATION, "wk", 604800.0, "wk", "week", "weeks", "woche", "wochen")
    }
}
