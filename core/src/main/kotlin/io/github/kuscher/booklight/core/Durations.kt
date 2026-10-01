package io.github.kuscher.booklight.core

/**
 * Lengths of time as people type them: 10m, 90s, 1h30, "1h 30m", 1.5h, "10 min", "2 Std".
 * Shared by timers ("timer 10m tea") and by "in 20m" in [When].
 */
internal object Durations {
    private val UNITS: Map<String, Int> = buildMap {
        for (w in listOf("s", "sec", "secs", "second", "seconds", "sek", "sekunde", "sekunden")) put(w, 1)
        for (w in listOf("m", "min", "mins", "minute", "minutes", "minuten")) put(w, 60)
        for (w in listOf("h", "hr", "hrs", "hour", "hours", "std", "stunde", "stunden")) put(w, 3600)
    }

    /** Seconds, and how many of the words were read. */
    class Length(val seconds: Double, val used: Int)

    /**
     * The duration that starts [words] (lower case), or null when they don't start with one.
     * Units must get smaller ("1h 30m", never "30m 1h"). A number with no unit is minutes if
     * [bareMinutes] and it comes first ("10 tea"); straight after a unit in the same word it is the
     * next smaller one ("1h30").
     */
    fun leading(words: List<String>, bareMinutes: Boolean): Length? {
        var total = 0.0
        var last = Int.MAX_VALUE
        var i = 0
        while (i < words.size) {
            val pieces = pieces(words[i]) ?: break
            var sum = 0.0
            var rank = last
            var used = 1
            var ok = true
            for ((j, piece) in pieces.withIndex()) {
                val (number, unit) = piece
                val next = words.getOrNull(i + 1)?.let(::unit)
                val seconds = when {
                    unit.isNotEmpty() -> unit(unit)
                    j > 0 -> when (rank) { 3600 -> 60; 60 -> 1; else -> null }
                    next != null -> { used = 2; next }                     // "10 min"
                    i == 0 && bareMinutes -> 60
                    else -> null
                }
                if (seconds == null || seconds >= rank) { ok = false; break }
                sum += number * seconds
                rank = seconds
            }
            if (!ok) break
            total += sum
            last = rank
            i += used
        }
        return if (i == 0) null else Length(total, i)
    }

    /** Seconds in a unit word ("min", "Std."), or null. */
    fun unit(word: String): Int? = UNITS[word.trimEnd('.', ',')]

    /** "1h30" → (1, h), (30, ""). Null when the word is anything but numbers each followed by letters. */
    fun pieces(word: String): List<Pair<Double, String>>? {
        val t = word.trimEnd('.', ',')
        val out = ArrayList<Pair<Double, String>>(2)
        var i = 0
        while (i < t.length) {
            val a = i
            while (i < t.length && (t[i] in '0'..'9' || t[i] == '.' || t[i] == ',')) i++
            val number = t.substring(a, i).replace(',', '.').toDoubleOrNull() ?: return null
            val b = i
            while (i < t.length && t[i].isLetter()) i++
            out.add(number to t.substring(b, i))
        }
        return out.ifEmpty { null }
    }
}
