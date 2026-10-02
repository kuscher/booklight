package io.github.kuscher.booklight.core

/** How a way is travelled; [mode] is what Google Maps calls it in a link. */
enum class Travel(val mode: String) { TRANSIT("transit"), WALK("walking"), BIKE("bicycling"), CAR("driving") }

/** A way to go. [from] empty: from here. [to] empty: not typed yet. [by] null: the maps app chooses. */
data class Trip(val from: String, val to: String, val by: Travel?)

/**
 * A phone number and the text after it. [shown] is the number as it was typed ("+49 30 5550 1234"),
 * [number] as it is dialled: its digits, with the + it was typed with.
 */
data class Dial(val shown: String, val number: String, val text: String) {
    /**
     * The number with its country code and nothing else ("493055501234"), as a link to a chat needs
     * it; null when it was typed without one, which no link can make up.
     */
    val full: String? get() = when {
        number.startsWith("+") -> number.drop(1)
        number.startsWith("00") -> number.drop(2)
        else -> null
    }?.takeIf { it.length >= Reach.FULL_MIN && it[0] != '0' }
}

/** A Telegram user name and the text after it. */
data class Handle(val name: String, val text: String)

/**
 * Reads the one-line arguments of the scopes that reach into another app (a way to go, a phone
 * number and a text, a Telegram name, a Meet code), and makes the links those apps take. It only
 * reads and writes text: what is opened, and whether an app is there to open it, is the app's
 * question. Like [Jot], nothing here fails: what is not understood stays in the place or the text.
 */
object Reach {
    // ---- a way to go

    private val FROM = setOf("from", "von")
    private val TO = setOf("to", "nach")
    /** An origin that only says where the user is: the maps app knows that better. */
    private val HERE = setOf("here", "hier")
    /** What leads up to a way of travelling: "by train", "mit dem Zug", "per Rad", "on foot", "zu Fuß". */
    private val LEADS = setOf("by", "mit", "per", "on", "zu")
    private val ARTICLES = setOf("the", "dem", "der", "den")
    /** Folded as [Matcher.fold] folds them: "S-Bahn" is "s bahn", "Öffis" is "offis". */
    private val MEANS: Map<String, Travel> = buildMap {
        for (w in listOf("train", "rail", "bus", "tram", "subway", "metro", "transit", "public transport", "zug", "bahn", "s bahn", "u bahn", "offis", "oeffis")) put(w, Travel.TRANSIT)
        for (w in listOf("foot", "fuß", "fuss")) put(w, Travel.WALK)
        for (w in listOf("bike", "bicycle", "rad", "fahrrad")) put(w, Travel.BIKE)
        for (w in listOf("car", "auto")) put(w, Travel.CAR)
    }
    /** Said in one word, at the end only: "hamburg hbf walking". */
    private val ALONE = mapOf("walking" to Travel.WALK, "cycling" to Travel.BIKE, "driving" to Travel.CAR)
    /** A way of travelling is four words at most: "by the public transport". */
    private const val WIDEST = 4

    /**
     * "hamburg hbf" is a place to go to from here. "from berlin to hamburg", "berlin to hamburg",
     * "to hamburg from berlin" and "von berlin nach hamburg" name both ends. "by train", "by bike",
     * "on foot", "by car" (and "mit dem Zug", "mit dem Rad", "zu Fuß", "mit dem Auto") at the end or
     * at the start say how. The places keep the user's own casing.
     *
     * A place may hold one of these words itself ("Welcome to Las Vegas"): a "to" or "nach" in front
     * makes all that follows the place, up to a "from" or "von" after it.
     */
    fun trip(arg: String): Trip {
        val words = Words.of(arg)
        val low = words.map { Matcher.fold(it.text) }
        val end = travel(low, start = false)
        val head = if (end == null) travel(low, start = true) else null
        val by = (end ?: head)?.first
        val lo = head?.second ?: 0
        val hi = words.size - (end?.second ?: 0)
        val to = (lo until hi).firstOrNull { low[it] in TO }
        // Where the origin starts: at the head of the line, or after the place. After a leading "to" either word says so;
        // otherwise only "from" does: "von" stands in too many street names ("Otto von Guericke Straße").
        val from = when {
            lo < hi && low[lo] in FROM -> lo
            to == lo -> (lo + 2 until hi).firstOrNull { low[it] in FROM }
            else -> (lo + 1 until hi).firstOrNull { low[it] == "from" }
        }
        fun trip(a: Int, b: Int, c: Int, d: Int) = Trip(Words.cut(arg, words, a, b).takeIf { Matcher.fold(it) !in HERE }.orEmpty(), Words.cut(arg, words, c, d), by)
        return when {
            // "from berlin to hamburg"; "from berlin", with the rest still to come.
            from == lo -> if (to != null) trip(lo + 1, to, to + 1, hi) else trip(lo + 1, hi, hi, hi)
            // "to hamburg from berlin"; "to hamburg".
            to == lo -> if (from != null) trip(from + 1, hi, lo + 1, from) else trip(hi, hi, lo + 1, hi)
            // "berlin to hamburg".
            to != null && (from == null || to < from) -> trip(lo, to, to + 1, hi)
            // "hamburg from berlin".
            from != null -> trip(from + 1, hi, lo, from)
            else -> trip(hi, hi, lo, hi)
        }
    }

    /** The way of travelling at the end of [low] (or, with [start], at its start), and how many words say it: the longest first. */
    private fun travel(low: List<String>, start: Boolean): Pair<Travel, Int>? {
        for (n in minOf(WIDEST, low.size) downTo (if (start) 2 else 1)) {
            val part = if (start) low.subList(0, n) else low.subList(low.size - n, low.size)
            way(part)?.let { return it to n }
        }
        return null
    }

    private fun way(part: List<String>): Travel? {
        if (part.size == 1) return ALONE[part[0]]
        val lead = part[0]
        if (lead !in LEADS) return null
        val means = MEANS[(if (part.size > 2 && part[1] in ARTICLES) part.drop(2) else part.drop(1)).joinToString(" ")] ?: return null
        // "on" and "zu" lead only to feet: "stratford on avon" stays a place.
        return means.takeIf { (lead != "on" && lead != "zu") || it == Travel.WALK }
    }

    /** Directions in Google Maps, as a link the Maps app and a browser both take. Without an origin it is from where the user is. */
    fun directions(r: Trip): String = buildString {
        append("https://www.google.com/maps/dir/?api=1&destination=").append(Templates.encode(r.to))
        if (r.from.isNotBlank()) append("&origin=").append(Templates.encode(r.from))
        r.by?.let { append("&travelmode=").append(it.mode) }
    }

    // ---- a number, and the text after it

    /** What stands in a typed number besides its digits: the last five are the dashes a number copied from a page may carry. */
    private const val MARKS = "+-./()\u2010\u2011\u2012\u2013\u2014"
    /** What ends a number when it follows its last digit: "0171 5550123: running late". */
    private const val ENDS = ":,;"
    /** A number is three digits at least ("112") and, with 00 before a country code, seventeen at most. */
    private const val MIN = 3
    private const val MAX = 17
    /** A number with its country code is seven digits at least. */
    internal const val FULL_MIN = 7

    /**
     * A phone number at the start of the line, typed the way people type one: "+49 30 5550 1234",
     * "0049 30 55501234", "(030) 5550-1234", "030/55501234", "+49 (0)30 5550 1234". What follows is
     * the text. Null when the line does not start with a number.
     *
     * The number takes every group of digits it can hold, so a text that starts with a number
     * ("10 min late") needs a colon or a comma after the phone number: "0171 5550123: 10 min late".
     * A group of digits with anything else in it ("5pm") is text.
     */
    fun phone(arg: String): Dial? {
        val words = Words.of(arg)
        var count = 0          // digits so far
        var plus = false
        var n = 0              // the words read as the number, marks between its groups included
        var end = 0            // … up to its last group of digits
        while (n < words.size) {
            val word = words[n].text
            val stop = word.length > 1 && word.last() in ENDS
            var digits = 0
            var lead = plus
            val fits = (if (stop) word.dropLast(1) else word).all { c ->
                when {
                    c in '0'..'9' -> { digits++; true }
                    // A plus stands before the first digit, once.
                    c == '+' -> (!lead && count + digits == 0).also { lead = true }
                    else -> c in MARKS
                }
            }
            if (!fits || count + digits > MAX) break
            count += digits; plus = lead; n++
            if (digits > 0) end = n
            if (stop) break
        }
        if (count < MIN) return null
        val shown = Words.cut(arg, words, 0, end).trimEnd { it in ENDS }
        val all = shown.filter { it in '0'..'9' }
        // "+49 (0)30": the zero in brackets is dialled inside the country only.
        val digits = if (plus || all.startsWith("00")) shown.replace("(0)", "").filter { it in '0'..'9' } else all
        if (digits.length < MIN) return null
        return Dial(shown, if (plus) "+$digits" else digits, Words.cut(arg, words, n, words.size))
    }

    /**
     * Whether [arg], which is no number yet, can still become one as more is typed: nothing, or only
     * what a number is written with ("+49", "(0"). "of duty" cannot: the line was not meant for a number.
     */
    fun startsNumber(arg: String): Boolean = phone(arg) == null && arg.all { it in '0'..'9' || it in MARKS || it.isWhitespace() }

    /** A chat with that number in WhatsApp, [text] in its field. [full] is the number with its country code, digits only. */
    fun whatsapp(full: String, text: String): String = "https://wa.me/$full" + text(text)

    // ---- Telegram

    /** A Telegram user name: a letter, then letters, digits and underscores, four to thirty-two in all. */
    private val NAME = Regex("[A-Za-z][A-Za-z0-9_]{3,31}")

    /** "anna on my way", "@anna …", "t.me/anna …": the name, and the text after it. Null when the first word is no user name. */
    fun handle(arg: String): Handle? {
        val words = Words.of(arg)
        val first = words.firstOrNull()?.text ?: return null
        val name = first.removePrefix("https://").removePrefix("http://").removePrefix("t.me/").removePrefix("@").trimEnd(':', ',')
        return if (NAME.matches(name)) Handle(name, Words.cut(arg, words, 1, words.size)) else null
    }

    /** How a user name may be written before the name itself: as its link, or after an @. */
    private val HEADS = listOf("https://t.me/", "http://t.me/", "t.me/", "@")
    private val NAME_START = Regex("[A-Za-z][A-Za-z0-9_]{0,31}")

    /**
     * Whether [arg], whose first word is no user name yet, can still become one as more is typed: one
     * word, the start of a name or of its link ("an", "@", "t.me/an"). A second word ends the first:
     * "bob hi" has had its chance.
     */
    fun startsName(arg: String): Boolean {
        val words = Words.of(arg)
        if (words.size > 1) return false
        val word = words.firstOrNull()?.text ?: return true
        if (HEADS.any { it.startsWith(word, ignoreCase = true) }) return true
        val head = HEADS.firstOrNull { word.startsWith(it, ignoreCase = true) }.orEmpty()
        return NAME_START.matches(word.substring(head.length))
    }

    /** A chat in Telegram with [who] (a user name, or a number with its country code after a +), [text] in its field. */
    fun telegram(who: String, text: String): String = "https://t.me/$who" + text(text)

    private fun text(text: String) = if (text.isBlank()) "" else "?text=" + Templates.encode(text)

    // ---- Meet

    /** A meeting's code as Meet writes it, alone or in its link. */
    private val CODE = Regex("(?:[Hh][Tt][Tt][Pp][Ss]?://)?(?:[Mm]eet\\.[Gg]oogle\\.[Cc]om/)?([A-Za-z]{3})-([A-Za-z]{4})-([A-Za-z]{3})(?:[?#/].*)?")

    /** "abc-defg-hij", or the meeting's link: the code. Null for anything else (the letters are a to z: nothing else goes into the link). */
    fun meetCode(arg: String): String? = CODE.matchEntire(arg.trim())?.destructured?.let { (a, b, c) -> "$a-$b-$c".lowercase() }

    private val LINKS = listOf("https://meet.google.com/", "http://meet.google.com/", "meet.google.com/")
    private val CODE_START = Regex("[A-Za-z]{0,3}|[A-Za-z]{3}-[A-Za-z]{0,4}|[A-Za-z]{3}-[A-Za-z]{4}-[A-Za-z]{0,3}")

    /**
     * Whether [arg], which is no code yet, can still become one as more is typed: the start of a code
     * ("ab", "abc-de") or of the meeting's link. "the parents" cannot, and neither can "anna": a code
     * has its dash after three letters.
     */
    fun startsCode(arg: String): Boolean {
        val typed = arg.trim()
        if (LINKS.any { it.startsWith(typed, ignoreCase = true) }) return true
        val link = LINKS.firstOrNull { typed.startsWith(it, ignoreCase = true) }.orEmpty()
        return CODE_START.matches(typed.substring(link.length))
    }

    /** The meeting with that [code]; without one, a new meeting. */
    fun meet(code: String?): String = "https://meet.google.com/" + (code ?: "new")
}
