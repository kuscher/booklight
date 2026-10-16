package io.github.kuscher.booklight.core

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/**
 * Result of a time-zone conversion or world-clock lookup (`3pm sfo in berlin`, `14:00 cet`, `time in tokyo`).
 */
data class ZoneAnswer(
    /** Main row title, e.g. `"00:00 Thu · Berlin"` or `"14:35 · Tokyo"`. */
    val title: String,
    /** Subtitle showing both ends and the offset difference, e.g. `"15:00 San Francisco → 00:00 Thu Berlin · +9h"`. */
    val subtitle: String,
    /** Short converted time for Copy and the right-hand answer badge, e.g. `"00:00"` or `"12:00 AM"`. */
    val time: String,
    /** Target IANA zone ID, e.g. `"Europe/Berlin"`. */
    val targetZone: String,
    /** Display name of the target place/zone, e.g. `"Berlin"`. */
    val targetName: String,
    /** Source IANA zone ID, e.g. `"America/Los_Angeles"`. */
    val sourceZone: String,
    /** Display name of the source place/zone, e.g. `"San Francisco"`. */
    val sourceName: String,
    /** Epoch millis when a specific time was converted; null for a live `"time in Tokyo"` clock. */
    val epochMillis: Long? = null,
) {
    /** Compact metadata stored in `Pinned.note` for rendering a pinned zone or world clock. */
    val pinNote: String get() = "$targetName|$targetZone|$sourceName|$sourceZone|$subtitle"
}

/**
 * Time-zone conversions and world-clock lookups in the search field.
 * Pure Kotlin (`java.time`), zero network, zero permissions.
 */
object Zones {
    class PlaceZone(val id: String, val en: String, val de: String = en, val code: Boolean = false) {
        fun name(german: Boolean): String = if (german) de else en
    }

    private val CONNECTORS = listOf("into", "nach", "in", "to", "->", "→")

    private val TIME_PREFIX_EN = listOf("current time in ", "time in ", "now in ", "time ")
    private val TIME_PREFIX_DE = listOf("aktuelle uhrzeit in ", "uhrzeit in ", "zeit in ", "jetzt in ", "uhrzeit ")
    private val TIME_SUFFIX = listOf(" local time", " ortszeit", " time", " zeit", " uhrzeit", " now", " jetzt")

    // Matches: 3pm, 3:30pm, 3:30 pm, 14:00, 15 uhr, 15:30 uhr, 15 uhr 30, noon, midnight
    private val TIME_TOKEN = Regex(
        """^(?:(\d{1,2})(?::(\d{2}))?\s*(am|pm)|(\d{1,2}):(\d{2})(?:\s*uhr)?|(\d{1,2})\s*uhr(?:\s*(\d{2}))?|(noon|midnight|mittag|mitternacht))$""",
        RegexOption.IGNORE_CASE,
    )

    fun convert(
        input: String,
        now: ZonedDateTime = ZonedDateTime.now(),
        is24Hour: Boolean = true,
        german: Boolean = false,
    ): ZoneAnswer? {
        val raw = input.trim().replace(Regex("\\s+"), " ")
        if (raw.length < 3 || raw.length > 80) return null

        // 1. Explicit conversion with connector: "<time> <from> in/to <to>" or "<from> <time> in/to <to>"
        splitConnector(raw)?.let { (left, right) ->
            val toPlace = resolvePlace(right, now.zone, german)
            if (toPlace != null) {
                parseTimeAndPlace(left, now.zone, german)?.let { (localTime, fromPlace) ->
                    return buildConversion(localTime, fromPlace, toPlace, now, is24Hour, german)
                }
            }
        }

        // 2. Single-zone time conversion to the user's local zone:
        //    "<time> <zone>", "<time> <place> time", or "<time> in <place>"
        parseSingleZoneTime(raw, german)?.let { (localTime, fromPlace) ->
            val herePlace = localPlace(now.zone, german)
            // If the user typed their own zone, only show if they explicitly wrote a timezone code or "time".
            return buildConversion(localTime, fromPlace, herePlace, now, is24Hour, german)
        }

        // 3. World clock ("time in tokyo", "tokyo time", "uhrzeit in tokio", "utc now"):
        parseWorldClock(raw)?.let { targetPlace ->
            val herePlace = localPlace(now.zone, german)
            return buildWorldClock(targetPlace, herePlace, now, is24Hour, german)
        }

        return null
    }

    private fun buildConversion(
        localTime: LocalTime,
        from: PlaceZone,
        to: PlaceZone,
        now: ZonedDateTime,
        is24Hour: Boolean,
        german: Boolean,
    ): ZoneAnswer? {
        val fromZone = runCatching { ZoneId.of(from.id) }.getOrNull() ?: return null
        val toZone = runCatching { ZoneId.of(to.id) }.getOrNull() ?: return null
        val baseDate = now.withZoneSameInstant(fromZone).toLocalDate()
        var srcZdt = ZonedDateTime.of(baseDate, localTime, fromZone)
        // If converting for an event, if the time today has already passed by more than 2 hours, keep today's date for reference
        // unless the user adds an event (where upcoming is preferred). For epochMillis we use the next upcoming occurrence.
        val upcomingSrc = if (srcZdt.isBefore(now.withZoneSameInstant(fromZone))) srcZdt.plusDays(1) else srcZdt
        val dstZdt = srcZdt.withZoneSameInstant(toZone)

        val locale = if (german) Locale.GERMAN else Locale.ENGLISH
        val dstTime = formatTime(dstZdt.toLocalTime(), is24Hour, locale)
        val srcTime = formatTime(srcZdt.toLocalTime(), is24Hour, locale)

        val dayDiff = ChronoUnit.DAYS.between(srcZdt.toLocalDate(), dstZdt.toLocalDate()).toInt()
        val dayBadge = when {
            dayDiff == 1 -> " " + dstZdt.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).trimEnd('.')
            dayDiff == -1 -> " " + dstZdt.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).trimEnd('.')
            dayDiff > 1 -> " (+${dayDiff}d)"
            dayDiff < -1 -> " (${dayDiff}d)"
            else -> ""
        }

        val diffLabel = formatOffsetDiff(srcZdt, dstZdt, german)
        val fromName = from.name(german)
        val toName = to.name(german)
        val title = "$dstTime$dayBadge · $toName"
        val subtitle = "$srcTime $fromName → $dstTime$dayBadge $toName · $diffLabel"
        return ZoneAnswer(
            title = title,
            subtitle = subtitle,
            time = dstTime,
            targetZone = toZone.id,
            targetName = toName,
            sourceZone = fromZone.id,
            sourceName = fromName,
            epochMillis = upcomingSrc.toInstant().toEpochMilli(),
        )
    }

    private fun buildWorldClock(
        target: PlaceZone,
        here: PlaceZone,
        now: ZonedDateTime,
        is24Hour: Boolean,
        german: Boolean,
    ): ZoneAnswer? {
        val targetZone = runCatching { ZoneId.of(target.id) }.getOrNull() ?: return null
        val hereZone = runCatching { ZoneId.of(here.id) }.getOrNull() ?: now.zone
        val targetZdt = now.withZoneSameInstant(targetZone)
        val hereZdt = now.withZoneSameInstant(hereZone)

        val locale = if (german) Locale.GERMAN else Locale.ENGLISH
        val targetTime = formatTime(targetZdt.toLocalTime(), is24Hour, locale)
        val dayName = targetZdt.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).trimEnd('.')
        val diffLabel = formatOffsetDiff(hereZdt, targetZdt, german)
        val targetName = target.name(german)
        val hereName = here.name(german)
        val utcOffset = formatUtcOffset(targetZdt)

        val title = "$targetTime · $targetName"
        val subtitle = "$dayName · $utcOffset · $diffLabel (${formatTime(hereZdt.toLocalTime(), is24Hour, locale)} $hereName)"
        return ZoneAnswer(
            title = title,
            subtitle = subtitle,
            time = targetTime,
            targetZone = targetZone.id,
            targetName = targetName,
            sourceZone = hereZone.id,
            sourceName = hereName,
            epochMillis = null,
        )
    }

    fun formatTime(time: LocalTime, is24Hour: Boolean, locale: Locale = Locale.ENGLISH): String {
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    private fun formatUtcOffset(zdt: ZonedDateTime): String {
        val totalSeconds = zdt.offset.totalSeconds
        if (totalSeconds == 0) return "UTC"
        val sign = if (totalSeconds >= 0) "+" else "−"
        val absMin = abs(totalSeconds) / 60
        val h = absMin / 60
        val m = absMin % 60
        return if (m == 0) "UTC$sign$h" else "UTC$sign$h:%02d".format(Locale.ROOT, m)
    }

    private fun formatOffsetDiff(from: ZonedDateTime, to: ZonedDateTime, german: Boolean): String {
        val diffMin = (to.offset.totalSeconds - from.offset.totalSeconds) / 60
        if (diffMin == 0) return if (german) "gleiche Zeit" else "same time"
        val sign = if (diffMin > 0) "+" else "−"
        val absMin = abs(diffMin)
        val h = absMin / 60
        val m = absMin % 60
        return if (m == 0) "$sign${h}h" else "$sign${h}h ${m}m"
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

    private fun parseTimeAndPlace(text: String, localZone: ZoneId, german: Boolean): Pair<LocalTime, PlaceZone>? {
        val cleaned = stripTrailingTimeWord(text.trim())
        val words = cleaned.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return null

        // Try time at start (1 to 3 tokens: "3pm", "3:30 pm", "15 uhr", "15 uhr 30")
        for (len in minOf(3, words.size - 1) downTo 1) {
            val timePart = words.subList(0, len).joinToString(" ")
            val placePart = words.subList(len, words.size).joinToString(" ").removePrefix("in ").removePrefix("at ").trim()
            val time = parseTime(timePart)
            if (time != null && placePart.isNotEmpty()) {
                val place = resolvePlace(placePart, localZone, german)
                if (place != null) return time to place
            }
        }

        // Try time at end ("sfo 3pm", "berlin 15 uhr")
        for (len in minOf(3, words.size - 1) downTo 1) {
            val timePart = words.subList(words.size - len, words.size).joinToString(" ")
            val placePart = words.subList(0, words.size - len).joinToString(" ").trim()
            val time = parseTime(timePart)
            if (time != null && placePart.isNotEmpty()) {
                val place = resolvePlace(placePart, localZone, german)
                if (place != null) return time to place
            }
        }
        return null
    }

    private fun parseSingleZoneTime(raw: String, german: Boolean): Pair<LocalTime, PlaceZone>? {
        val lower = raw.lowercase()
        val hasTimeSuffix = TIME_SUFFIX.any { lower.endsWith(it) }
        val cleaned = stripTrailingTimeWord(raw)
        val words = cleaned.split(' ').filter { it.isNotEmpty() }
        if (words.size < 2) return null

        for (len in minOf(3, words.size - 1) downTo 1) {
            val timePart = words.subList(0, len).joinToString(" ")
            val restRaw = words.subList(len, words.size).joinToString(" ")
            val hasIn = restRaw.startsWith("in ", ignoreCase = true)
            val placePart = restRaw.removePrefix("in ").removePrefix("In ").trim()
            val time = parseTime(timePart) ?: continue
            val place = PLACES[norm(placePart)] ?: continue
            // Accept if it's a timezone code (cet, pst, utc), or has "in <place>", or ends with "time"/"uhrzeit",
            // or is a direct "<time> <place>" with no extra words.
            if (place.code || hasIn || hasTimeSuffix || words.size <= len + 2) {
                return time to place
            }
        }
        return null
    }

    private fun parseWorldClock(raw: String): PlaceZone? {
        val lower = raw.lowercase().trim()
        for (p in TIME_PREFIX_EN + TIME_PREFIX_DE) {
            if (lower.startsWith(p)) {
                val placePart = lower.substring(p.length).trim()
                PLACES[norm(placePart)]?.let { return it }
            }
        }
        for (s in TIME_SUFFIX) {
            if (lower.endsWith(s) && lower.length > s.length) {
                val placePart = lower.substring(0, lower.length - s.length).trim()
                PLACES[norm(placePart)]?.let { return it }
            }
        }
        return null
    }

    private fun stripTrailingTimeWord(s: String): String {
        val lower = s.lowercase()
        for (suf in TIME_SUFFIX) {
            if (lower.endsWith(suf) && s.length > suf.length) {
                return s.substring(0, s.length - suf.length).trim()
            }
        }
        return s
    }

    internal fun parseTime(raw: String): LocalTime? {
        val m = TIME_TOKEN.matchEntire(raw.trim()) ?: return null
        if (m.groupValues[8].isNotEmpty()) {
            return when (m.groupValues[8].lowercase()) {
                "noon", "mittag" -> LocalTime.NOON
                "midnight", "mitternacht" -> LocalTime.MIDNIGHT
                else -> null
            }
        }
        // 12-hour am/pm
        if (m.groupValues[1].isNotEmpty()) {
            val h = m.groupValues[1].toIntOrNull() ?: return null
            val min = m.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            if (h !in 1..12 || min !in 0..59) return null
            val pm = m.groupValues[3].equals("pm", ignoreCase = true)
            val hour24 = when {
                pm && h < 12 -> h + 12
                !pm && h == 12 -> 0
                else -> h
            }
            return LocalTime.of(hour24, min)
        }
        // 24-hour HH:mm
        if (m.groupValues[4].isNotEmpty()) {
            val h = m.groupValues[4].toIntOrNull() ?: return null
            val min = m.groupValues[5].toIntOrNull() ?: return null
            if (h !in 0..23 || min !in 0..59) return null
            return LocalTime.of(h, min)
        }
        // German "15 uhr [30]"
        if (m.groupValues[6].isNotEmpty()) {
            val h = m.groupValues[6].toIntOrNull() ?: return null
            val min = m.groupValues[7].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            if (h !in 0..23 || min !in 0..59) return null
            return LocalTime.of(h, min)
        }
        return null
    }

    private fun resolvePlace(raw: String, localZone: ZoneId, german: Boolean): PlaceZone? {
        val n = norm(raw)
        if (n in setOf("local", "here", "my time", "hier", "ortszeit", "lokal")) {
            return localPlace(localZone, german)
        }
        return PLACES[n]
    }

    private fun localPlace(zone: ZoneId, german: Boolean): PlaceZone {
        val city = zone.id.substringAfterLast('/').replace('_', ' ')
        val fallback = if (german) "Hier" else "Local"
        val label = city.takeIf { it.isNotEmpty() && it != "UTC" && it != "GMT" } ?: fallback
        return PlaceZone(zone.id, label, label)
    }

    private fun norm(s: String): String = Matcher.fold(s.trim()).replace(Regex("\\s+"), " ")

    private val PLACES: Map<String, PlaceZone> = buildMap {
        fun add(id: String, en: String, de: String = en, code: Boolean = false, vararg keys: String) {
            val p = PlaceZone(id, en, de, code)
            put(norm(en), p)
            put(norm(de), p)
            for (k in keys) put(norm(k), p)
        }

        // Standard timezone abbreviations
        add("UTC", "UTC", "UTC", code = true, "utc", "zulu")
        add("GMT", "GMT", "GMT", code = true, "gmt")
        add("Europe/Berlin", "CET", "MEZ", code = true, "cet", "cest", "mez", "mesz")
        add("Europe/London", "BST", "BST", code = true, "bst", "wet", "west")
        add("Europe/Athens", "EET", "OEZ", code = true, "eet", "eest", "oez")
        add("America/Los_Angeles", "PT", "PT", code = true, "pt", "pst", "pdt", "pacific", "pacific time")
        add("America/Denver", "MT", "MT", code = true, "mt", "mst", "mdt", "mountain", "mountain time")
        add("America/Chicago", "CT", "CT", code = true, "ct", "cst", "cdt", "central", "central time")
        add("America/New_York", "ET", "ET", code = true, "et", "est", "edt", "eastern", "eastern time")
        add("America/Anchorage", "AKT", "AKT", code = true, "akst", "akdt", "alaska")
        add("Pacific/Honolulu", "HST", "HST", code = true, "hst", "hawaii", "honolulu", "hnl")
        add("Asia/Tokyo", "JST", "JST", code = true, "jst")
        add("Asia/Seoul", "KST", "KST", code = true, "kst")
        add("Asia/Kolkata", "IST", "IST", code = true, "ist")
        add("Asia/Singapore", "SGT", "SGT", code = true, "sgt")
        add("Asia/Hong_Kong", "HKT", "HKT", code = true, "hkt")
        add("Australia/Sydney", "AET", "AET", code = true, "aest", "aedt", "aet")
        add("Pacific/Auckland", "NZT", "NZT", code = true, "nzst", "nzdt", "nzt")
        add("Europe/Moscow", "MSK", "MSK", code = true, "msk")
        add("Asia/Dubai", "GST", "GST", code = true, "gst")

        // North America cities & airports
        add("America/Los_Angeles", "San Francisco", "San Francisco", false, "sfo", "sf", "bay area", "silicon valley", "california", "kalifornien", "sjc", "oak")
        add("America/Los_Angeles", "Los Angeles", "Los Angeles", false, "lax", "la", "san diego", "san")
        add("America/Los_Angeles", "Seattle", "Seattle", false, "sea", "portland", "pdx")
        add("America/Vancouver", "Vancouver", "Vancouver", false, "yvr")
        add("America/Phoenix", "Phoenix", "Phoenix", false, "phx", "arizona")
        add("America/Denver", "Denver", "Denver", false, "den", "salt lake city", "slc", "calgary", "yyc")
        add("America/Chicago", "Chicago", "Chicago", false, "ord", "mdw", "minneapolis", "msp")
        add("America/Chicago", "Dallas", "Dallas", false, "dfw", "houston", "iah", "austin", "aus", "texas")
        add("America/Mexico_City", "Mexico City", "Mexiko-Stadt", false, "mex", "mexico", "mexiko")
        add("America/New_York", "New York", "New York", false, "nyc", "ny", "jfk", "ewr", "lga", "manhattan")
        add("America/New_York", "Boston", "Boston", false, "bos")
        add("America/New_York", "Washington", "Washington", false, "dc", "iad", "dca")
        add("America/New_York", "Atlanta", "Atlanta", false, "atl")
        add("America/New_York", "Miami", "Miami", false, "mia", "orlando", "mco", "florida")
        add("America/Toronto", "Toronto", "Toronto", false, "yyz", "montreal", "yul", "ottawa")

        // South America
        add("America/Sao_Paulo", "São Paulo", "São Paulo", false, "sao paulo", "gru", "rio", "rio de janeiro", "gig", "brazil", "brasilien")
        add("America/Argentina/Buenos_Aires", "Buenos Aires", "Buenos Aires", false, "eze", "argentina", "argentinien")
        add("America/Santiago", "Santiago", "Santiago", false, "scl", "chile")
        add("America/Bogota", "Bogotá", "Bogotá", false, "bogota", "bog", "colombia", "kolumbien")
        add("America/Lima", "Lima", "Lima", false, "lim", "peru")

        // Europe
        add("Europe/London", "London", "London", false, "lhr", "lgw", "stn", "uk", "england", "britain", "manchester", "man", "edinburgh", "edi")
        add("Europe/Dublin", "Dublin", "Dublin", false, "dub", "ireland", "irland")
        add("Europe/Lisbon", "Lisbon", "Lissabon", false, "lis", "portugal", "porto", "opo")
        add("Atlantic/Reykjavik", "Reykjavik", "Reykjavik", false, "kef", "iceland", "island")
        add("Europe/Berlin", "Berlin", "Berlin", false, "ber", "txl", "germany", "deutschland")
        add("Europe/Berlin", "Frankfurt", "Frankfurt", false, "fra")
        add("Europe/Berlin", "Munich", "München", false, "muenchen", "munchen", "muc")
        add("Europe/Berlin", "Hamburg", "Hamburg", false, "ham")
        add("Europe/Berlin", "Cologne", "Köln", false, "koeln", "koln", "cgn", "dusseldorf", "duesseldorf", "düsseldorf", "dus", "stuttgart", "str")
        add("Europe/Paris", "Paris", "Paris", false, "cdg", "ory", "france", "frankreich", "lyon", "nice")
        add("Europe/Zurich", "Zurich", "Zürich", false, "zuerich", "zrh", "geneva", "genf", "gva", "switzerland", "schweiz", "basel", "bsl")
        add("Europe/Vienna", "Vienna", "Wien", false, "vie", "austria", "oesterreich", "österreich")
        add("Europe/Amsterdam", "Amsterdam", "Amsterdam", false, "ams", "netherlands", "niederlande", "holland")
        add("Europe/Brussels", "Brussels", "Brüssel", false, "bruessel", "bru", "belgium", "belgien")
        add("Europe/Madrid", "Madrid", "Madrid", false, "mad", "barcelona", "bcn", "spain", "spanien", "mallorca", "pmi")
        add("Europe/Rome", "Rome", "Rom", false, "fco", "milan", "mailand", "mxp", "lin", "italy", "italien")
        add("Europe/Copenhagen", "Copenhagen", "Kopenhagen", false, "cph", "denmark", "daenemark", "dänemark")
        add("Europe/Stockholm", "Stockholm", "Stockholm", false, "arn", "sweden", "schweden")
        add("Europe/Oslo", "Oslo", "Oslo", false, "osl", "norway", "norwegen")
        add("Europe/Warsaw", "Warsaw", "Warschau", false, "waw", "poland", "polen", "krakow", "krakau")
        add("Europe/Prague", "Prague", "Prag", false, "prg", "czechia", "tschechien")
        add("Europe/Budapest", "Budapest", "Budapest", false, "bud", "hungary", "ungarn")
        add("Europe/Athens", "Athens", "Athen", false, "ath", "greece", "griechenland")
        add("Europe/Helsinki", "Helsinki", "Helsinki", false, "hel", "finland", "finnland")
        add("Europe/Bucharest", "Bucharest", "Bukarest", false, "otp", "romania", "rumaenien", "rumänien")
        add("Europe/Kyiv", "Kyiv", "Kiew", false, "kiev", "ukraine")
        add("Europe/Istanbul", "Istanbul", "Istanbul", false, "turkey", "tuerkei", "türkei")
        add("Europe/Moscow", "Moscow", "Moskau", false, "svo", "dme")

        // Middle East & Africa
        add("Asia/Jerusalem", "Tel Aviv", "Tel Aviv", false, "tlv", "jerusalem", "israel")
        add("Asia/Dubai", "Dubai", "Dubai", false, "dxb", "abu dhabi", "auh", "uae")
        add("Asia/Riyadh", "Riyadh", "Riad", false, "ruh", "doha", "doh", "qatar", "katar")
        add("Africa/Cairo", "Cairo", "Kairo", false, "cai", "egypt", "aegypten", "ägypten")
        add("Africa/Johannesburg", "Johannesburg", "Johannesburg", false, "jnb", "cape town", "kapstadt", "cpt", "south africa", "suedafrika", "südafrika")
        add("Africa/Lagos", "Lagos", "Lagos", false, "los", "nigeria")
        add("Africa/Nairobi", "Nairobi", "Nairobi", false, "nbo", "kenya", "kenia")

        // Asia & Oceania
        add("Asia/Kolkata", "Mumbai", "Mumbai", false, "bom", "delhi", "new delhi", "del", "bangalore", "bengaluru", "blr", "india", "indien", "chennai", "maa", "hyderabad", "hyd")
        add("Asia/Bangkok", "Bangkok", "Bangkok", false, "bkk", "thailand", "hanoi", "han", "ho chi minh", "sgn", "vietnam")
        add("Asia/Jakarta", "Jakarta", "Jakarta", false, "cgk", "indonesia", "indonesien")
        add("Asia/Singapore", "Singapore", "Singapur", false, "sin", "kuala lumpur", "kul", "malaysia")
        add("Asia/Hong_Kong", "Hong Kong", "Hongkong", false, "hongkong", "hkg")
        add("Asia/Taipei", "Taipei", "Taipeh", false, "tpe", "taiwan")
        add("Asia/Shanghai", "Shanghai", "Shanghai", false, "pvg", "sha", "beijing", "peking", "pek", "shenzhen", "szx", "china")
        add("Asia/Manila", "Manila", "Manila", false, "mnl", "philippines", "philippinen")
        add("Asia/Seoul", "Seoul", "Seoul", false, "icn", "gmp", "korea", "south korea", "suedkorea", "südkorea")
        add("Asia/Tokyo", "Tokyo", "Tokio", false, "tokio", "hnd", "nrt", "osaka", "kix", "japan")
        add("Australia/Perth", "Perth", "Perth", false, "per")
        add("Australia/Sydney", "Sydney", "Sydney", false, "syd", "melbourne", "mel", "australia", "australien")
        add("Australia/Brisbane", "Brisbane", "Brisbane", false, "bne")
        add("Pacific/Auckland", "Auckland", "Auckland", false, "akl", "wellington", "wlg", "new zealand", "neuseeland")
    }
}
