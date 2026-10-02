package io.github.kuscher.booklight.core

import java.time.LocalDate

/** An airline of the bundled table: its designator (the ticket's two characters), its code (the callsign's three letters) and its name. */
data class Airline(val iata: String, val icao: String, val name: String)

/**
 * The bundled table of airlines. A line is `designator ⇥ code ⇥ name`; blank lines and lines starting
 * with `#` are skipped. A designator may stand on several lines (LH is Lufthansa and Lufthansa
 * Cargo): the first one is the one a typed number is read as, and the table puts the airline with
 * the most routes first.
 */
class Airlines(lines: Sequence<String>) {
    private val byDesignator = HashMap<String, Airline>()
    private val byCode = HashMap<String, Airline>()

    init {
        for (raw in lines) {
            if (raw.isBlank() || raw.startsWith("#")) continue
            val cols = raw.removePrefix("﻿").split('\t').map { it.trim() }
            if (cols.size < 3 || cols[0].length != 2 || cols[1].length != 3 || cols[2].isEmpty()) continue
            val a = Airline(cols[0].uppercase(), cols[1].uppercase(), cols[2])
            byDesignator.putIfAbsent(a.iata, a)
            byCode.putIfAbsent(a.icao, a)
        }
    }

    /** The airline a ticket's designator belongs to: LH, U2, 4Y. */
    fun designator(code: String): Airline? = byDesignator[code.uppercase()]
    /** The airline a callsign's three letters belong to: DLH. */
    fun code(code: String): Airline? = byCode[code.uppercase()]
}

/**
 * A flight number read off a typed line: whose flight, which number, and the day if one stood after
 * it ("LH455 fri"). [strong]: it reads as a flight and as little else, so it may be looked up after
 * a pause in typing. A weak one (`ps5`, `ms 365`, `q4 2026`) is far more often something else: its
 * row is the last one, and nothing is sent for it until the user goes to it.
 */
data class FlightNumber(val airline: Airline, val number: Int, val suffix: String = "", val strong: Boolean = true, val day: LocalDate? = null, /** Typed in the callsign's form (DLH455). */ val callsign: Boolean = false) {
    /** As a ticket says it, closed up: LH455. */
    val iata: String get() = "${airline.iata}$number$suffix"
    /** As the callsign: DLH455. A flight's page at FlightAware is found by this form. */
    val icao: String get() = "${airline.icao}$number$suffix"
    /** As a row says it: "LH 455". */
    val shown: String get() = "${airline.iata} $number$suffix"
    /** One name for this number on this day: what an answer is kept under. */
    val id: String get() = iata + (day?.let { "@$it" } ?: "")
}

/**
 * Reads a flight number: `LH455`, `lh 455`, `LH-455`, `LH0455`, the callsign `DLH455`, and a day after
 * any of them (`LH455 fri`, `LH455 tomorrow`, `LH455 3.10.`). Nothing else may stand in the line.
 * A day is taken from three letters on; while one is being typed (`LH455 s`, `LH455 mo`, `LH455 tomor`)
 * the line is still the number alone, so its row does not go away for a key.
 *
 * Nearly every pair of letters is some airline's designator, so most of what has this shape is not
 * a flight: `PS5` is Ukraine International 5, `MP3` Martinair 3, `H264` Sky Airline 64. A match is
 * strong only when the designator is two letters that are not an everyday word, or a callsign's
 * three letters that are not one, with a number of two to four digits that is not a year standing
 * apart ("DE 2026"). Everything else that reads as a flight is weak: a digit in the designator
 * (`U2 8001`), a single digit (`BA1`), a word (`AM 7`, `IN 100`, `WIN 11`), a year. The price: the
 * numbers of easyJet (U2), Wizz (W6), JetBlue (B6), Ryanair (FR), Austrian (OS), Alaska (AS),
 * Condor (DE) and Qatar (QR) are weak, and so are real flights with one digit.
 */
object Flights {
    private val TICKET = Regex("^([A-Z]{2}|[A-Z][0-9]|[0-9][A-Z])([ -]?)0*([1-9][0-9]{0,3})([A-Z]?)(?:\\s+(\\S.*))?$", RegexOption.IGNORE_CASE)
    private val CALLSIGN = Regex("^([A-Z]{3})([ -]?)0*([1-9][0-9]{0,3})([A-Z]?)(?:\\s+(\\S.*))?$", RegexOption.IGNORE_CASE)

    /** Designators that are words or abbreviations people type with a number after them. English, then German. */
    private val WORDS = setOf(
        "AM", "PM", "IN", "TO", "AT", "NO", "ME", "WE", "US", "IT", "DE", "FR", "OK", "ON", "OR", "IF", "DO", "GO", "BY", "SO", "UP", "MY",
        "AS", "MS", "OS", "HD", "SD", "AI", "PC", "BT", "QR", "PS", "MP", "TV", "ID", "IP", "IQ",
        "AB", "AN", "DA", "ER", "ES", "IM", "JA", "UM", "ZU",
    )

    /** The same for the callsign's three letters: the codes in the table that are also a word, a unit, a file type, a zone, a currency. */
    private val WORDS3 = setOf(
        "ABS", "APP", "ARE", "ART", "ASH", "AXE", "BBC", "BOX", "BYE", "CAD", "CAL", "CAM", "CHF", "COM", "CRC", "CSS", "CST", "CUB", "DHL", "DIG",
        "EIN", "EST", "ETC", "GAP", "GEL", "GER", "GTI", "GUN", "GUY", "HAD", "HAT", "HIM", "HOP", "ICE", "JAW", "JOY", "JST", "KEN", "LAP", "LOG",
        "LOT", "LTE", "MAC", "MAR", "MAY", "MPH", "ONE", "PAL", "PDT", "PEN", "PER", "PGP", "PST", "RAM", "RAN", "RAR", "RIO", "ROT", "RUN", "SAT",
        "SAW", "SET", "SEW", "SHY", "SIT", "SKI", "SKU", "SKY", "SOL", "TAP", "TAR", "TAX", "TIN", "TOM", "TOR", "TOW", "UPS", "USA", "VGA", "WEB",
        "WIN", "WON",
    )

    private val YEARS = 1990..2039

    /**
     * The flight [text] reads as, or null when it is none: not the shape, a designator no airline in
     * the table has, or something after the number that is not a day. [sure]: it was typed after the
     * keyword `flight`, so whatever reads as a flight is one, and is strong.
     */
    fun read(text: String, airlines: Airlines, today: LocalDate, sure: Boolean = false): FlightNumber? {
        val t = text.trim()
        if (t.length < 3 || t.length > 40) return null
        // The callsign's form first: three letters can only be that.
        CALLSIGN.find(t)?.let { m -> airlines.code(m.groupValues[1])?.let { return make(it, m, today, sure, callsign = true) } }
        return TICKET.find(t)?.let { m -> airlines.designator(m.groupValues[1])?.let { make(it, m, today, sure, callsign = false) } }
    }

    /** A flight number as it stands in running text: capital letters, set off from the words around it. */
    private val IN_TEXT = Regex("(?<![A-Za-z0-9])(?:[A-Z]{2,3})[ -]?[0-9]{2,4}(?![A-Za-z0-9])")

    /**
     * The first flight number in a text someone wrote ("Landing with LH 454 at 12:45"): for what was
     * copied or handed over. Only what is strong, and only in capitals: in a sentence "to 100" and
     * "in 2026" are words.
     */
    fun find(text: String, airlines: Airlines, today: LocalDate): FlightNumber? =
        IN_TEXT.findAll(text.take(2000)).firstNotNullOfOrNull { m -> read(m.value, airlines, today)?.takeIf { it.strong } }

    private fun make(airline: Airline, m: MatchResult, today: LocalDate, sure: Boolean, callsign: Boolean): FlightNumber? {
        val code = m.groupValues[1].uppercase()
        val apart = m.groupValues[2].isNotEmpty()
        val number = m.groupValues[3].toInt()
        val rest = m.groupValues[5].trim()
        // A day that is still being typed leaves the number's own row standing: one or two letters (which are also the short
        // forms of days: "mo" on the way to "morgen" is not Monday's flight), and the beginning of a day's word ("tom", "frei").
        val typing = rest.all { it.isLetter() } && (rest.length <= 2 || Days.begins(rest))
        val day = if (rest.isEmpty() || rest.length <= 2 && typing) null else day(rest, today) ?: if (typing) null else return null
        val word = if (callsign) code in WORDS3 else code in WORDS || code.any { it.isDigit() }
        val strong = sure || (!word && number >= 10 && !(apart && number in YEARS))
        return FlightNumber(airline, number, m.groupValues[4].uppercase(), strong, day, callsign)
    }

    /** The day that stands after a number: a weekday is today if today is one, else the next such day. */
    private fun day(text: String, today: LocalDate): LocalDate? {
        val words = Words.of(text).map { it.text.lowercase().trimEnd(',', ';') }
        return when (val d = Days.parse(words, today)) {
            is Day.On -> d.date
            is Day.Every -> today.plusDays(((d.day.value - today.dayOfWeek.value + 7) % 7).toLong())
            null -> null
        }
    }

    /** The flight's page at FlightAware: found by the callsign's form (the ticket's form answers with the wrong flight, or none). */
    fun page(n: FlightNumber): String = "https://www.flightaware.com/live/flight/${n.icao}"
}
