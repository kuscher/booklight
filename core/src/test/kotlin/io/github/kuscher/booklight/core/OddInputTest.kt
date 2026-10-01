package io.github.kuscher.booklight.core

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** The parsers read whatever is typed or pasted: none of them may throw, whatever it is. */
class OddInputTest {
    private val now = LocalDateTime.of(2026, 10, 1, 10, 0)
    private val odd = listOf(
        "", " ", "\t\n", "\u0000", "\uD83D", "\uDE00\uD83D", "🎉", "👨‍👩‍👧‍👦 family", "​‍﻿", "مرحبا بالعالم", "日本語 のテキスト",
        "İstanbul ǅ ß ﬁ", "á́́́", "%", "%s", "{", "{argument", "{}", "#", "#g", "(", ")", "rgb(", "hsl(,,)",
        "rgb(,,,)", ":", "::", ":/", "/", "//", "a/", "/a", "#/", "@", " @ ", "@ @ @", " / ", "/ / /", "-", "–", "- - -", "9-", "-9",
        "9:", ":9", "9:99", "99:99", "0", "00", "-0", "-1", "1e9", "1e999", "NaN", "Infinity", "0x10", "٣ دقائق", "３pm",
        "2147483648", "9223372036854775808", "99999999999999999999999999", "9".repeat(5_000), "9".repeat(5_000) + "m",
        "in " + "9".repeat(400) + "h", "in 1.7976931348623157E308h", "in 999999999 days", "in 9999 months", "31.12.9999",
        "0000-01-01", "9999-12-31 23:59", "29.2.", "1/1/0000", "x".repeat(100_000), "x ".repeat(50_000), "@ ".repeat(10_000),
        " / ".repeat(10_000), "am ".repeat(5_000), "in ".repeat(5_000), "9-".repeat(5_000), "1h".repeat(5_000),
    )

    @Test fun nothingThrows() {
        val verbs = listOf(Verb("info", listOf("info", "", " ", "🎉")), Verb("x", emptyList()), Verb("y", listOf("new window"), min = -1))
        val emoji = EmojiIndex(odd.asSequence() + sequenceOf("🎉\te\tparty popper", "\t\t\t\t\t\t\t", "a\ts\t\u0000"), german = true)
        for (s in odd) {
            Verbs.readings(s, verbs)
            Verbs.readings("chrome $s", verbs)
            When.parse(s, now)
            When.parse("x $s", now)
            Jot.mail(s); Jot.note(s); Jot.event(s, now); Jot.reminder(s, now); Jot.timer(s); Jot.alarm(s, now); Jot.new(s)
            Colors.parse(s)
            Templates.takesArgument(s)
            Templates.fill(s, s, s, LocalDate.MAX)
            Clip.transforms(s)
            emoji.search(s, false, 70); emoji.search(s, true, Int.MAX_VALUE); emoji.find(s)
            Jumps.github(s); Jumps.port(s)
            Ask.first(s); Ask.offered(s)
        }
    }

    @Test fun atTheEdgesOfTheCalendar() {
        val late = LocalDateTime.of(2026, 12, 31, 23, 59, 59)
        assertEquals(LocalDateTime.of(2027, 1, 1, 0, 0), When.parse("tomorrow", late)!!.start)
        assertEquals(LocalDateTime.of(2027, 1, 1, 9, 0), When.parse("9am", late)!!.start)
        assertEquals(LocalDateTime.of(2027, 1, 1, 0, 19, 59), When.parse("in 20m", late)!!.start)
        assertEquals(LocalDateTime.of(2028, 2, 29, 0, 0), When.parse("29.2.", LocalDateTime.of(2027, 3, 1, 8, 0))!!.start)
        assertEquals(LocalDateTime.of(2028, 2, 29, 0, 0), When.parse("feb 29", LocalDateTime.of(2028, 2, 29, 8, 0))!!.start)
        for (s in odd) for (edge in listOf(LocalDateTime.of(9999, 12, 31, 23, 59), LocalDateTime.of(1, 1, 1, 0, 0))) {
            When.parse(s, edge); Jot.event(s, edge); Jot.reminder(s, edge); Jot.alarm(s, edge)
        }
    }

    @Test fun passwordsForAnyLength() {
        for (n in listOf(Int.MIN_VALUE, -1, 0, 1, 7, 8, 9, 63, 64, 65, Int.MAX_VALUE)) {
            assertEquals(n.coerceIn(8, 64), Secrets.password(n, java.util.Random(n.toLong())).length)
        }
    }
}
