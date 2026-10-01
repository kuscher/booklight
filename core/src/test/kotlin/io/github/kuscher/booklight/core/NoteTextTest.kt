package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteTextTest {
    private val notes = """
        # Notes
        - 2026-09-28 09:12  buy milk and coffee
        - 2026-09-29 18:40  Gate B22, boarding 16:40
          ask about the aisle seat
        a line written by hand
        - 2026-10-01 08:03  milk again, oat this time
    """.trimIndent() + "\n"

    @Test fun anEntryIsADatedLineAndItsIndentedLines() {
        assertEquals("- 2026-10-01 14:02  buy milk\n", NoteText.entry("buy milk", "2026-10-01 14:02"))
        assertEquals("- 14:02  first\n  second\n", NoteText.entry(" first\nsecond ", "14:02"))
    }

    @Test fun aNewEntryStartsOnItsOwnLine() {
        assertEquals("", NoteText.lead(""))
        assertEquals("", NoteText.lead("- a\n"))
        assertEquals("\n", NoteText.lead("- a"))                       // the file was edited by hand and left open
    }

    @Test fun theLatestFirst() {
        val all = NoteText.find(notes, "", 10)
        assertEquals("milk again, oat this time", all.first().text)
        assertEquals("2026-10-01 08:03", all.first().date)
        assertEquals("a line written by hand", all[1].text)
        assertEquals("", all[1].date)
        assertEquals("Gate B22, boarding 16:40 ↵ ask about the aisle seat", all[2].text)
        assertEquals(2, all[2].line)                                   // the line the entry starts on
        assertEquals("Notes", all.last().text)                         // a heading is a line like any other
        assertEquals(2, NoteText.find(notes, "", 2).size)
    }

    @Test fun everyWordHasToBeThere() {
        assertEquals(listOf("milk again, oat this time", "buy milk and coffee"), NoteText.find(notes, "milk", 10).map { it.text })
        assertEquals(listOf("buy milk and coffee"), NoteText.find(notes, "MILK coffee", 10).map { it.text })
        assertEquals(1, NoteText.find(notes, "aisle gate", 10).size)   // a word of an indented line counts
        assertTrue(NoteText.find(notes, "tea", 10).isEmpty())
    }

    @Test fun oddFilesAreJustFiles() {
        assertTrue(NoteText.find("", "x", 5).isEmpty())
        assertTrue(NoteText.find("\n\n\n", "", 5).isEmpty())
        assertEquals(listOf("b", "a"), NoteText.find("- a\r\n- b\r\n", "", 5).map { it.text })        // Windows line ends
        assertEquals("no final line break", NoteText.find("- x\n- no final line break", "", 1).first().text)
        val big = (1..20000).joinToString("") { "- 2026-01-01 10:00  line $it\n" }
        assertEquals("line 20000", NoteText.find(big, "", 3).first().text)
        assertEquals(3, NoteText.find(big, "line", 3).size)
    }

    private val todo = "- [ ] 2026-09-30  call the bank\n- [x] 2026-09-29  book the dentist\n- [ ] renew passport\nnot a task\n* [ ] starred\n- [ ] 2026-10-01  call the bank\n"

    @Test fun aTaskIsOneLine() {
        assertEquals("- [ ] 2026-10-01  call bank\n", NoteText.todo("call bank", "2026-10-01"))
        assertEquals("- [ ] 2026-10-01  one two\n", NoteText.todo(" one\n two ", "2026-10-01"))
    }

    @Test fun tasksInTheFilesOrder() {
        val t = NoteText.tasks(todo)
        assertEquals(listOf("call the bank", "book the dentist", "renew passport", "starred", "call the bank"), t.map { it.text })
        assertEquals(listOf(false, true, false, false, false), t.map { it.done })
        assertEquals(listOf("2026-09-30", "2026-09-29", "", "", "2026-10-01"), t.map { it.date })
        assertEquals(listOf(0, 1, 2, 4, 5), t.map { it.line })
        assertTrue(NoteText.tasks("").isEmpty())
    }

    @Test fun tickingIsByLine() {
        val ticked = NoteText.tick(todo, 5, "call the bank", true)!!
        assertEquals(listOf(false, true, false, false, true), NoteText.tasks(ticked).map { it.done })     // the second of two equal tasks, not the first
        assertEquals(todo, NoteText.tick(ticked, 5, "call the bank", false))                              // and back again, the file as it was
        assertEquals(ticked, NoteText.tick(ticked, 5, "call the bank", true))                             // ticking twice changes nothing
    }

    @Test fun aFileThatChangedMeanwhile() {
        val moved = "- [ ] a new first line\n$todo"
        // Line 2 no longer says "renew passport" (it moved to line 3): the nearest line that does is ticked.
        assertEquals(true, NoteText.tasks(NoteText.tick(moved, 2, "renew passport", true)!!).first { it.text == "renew passport" }.done)
        assertNull(NoteText.tick(todo, 2, "gone", true))                                                  // nothing says that: nothing is written
        assertNull(NoteText.tick("", 0, "x", true))
    }

    @Test fun lineEndsStayAsTheyWere() {
        val crlf = "- [ ] one\r\n- [ ] two\r\n"
        assertEquals("- [ ] one\r\n- [x] two\r\n", NoteText.tick(crlf, 1, "two", true))
    }

    @Test fun anotherFileByItsName() {
        val files = listOf("Notes.md", "Todo.md", "ideas.md", "Reading List.md", "2026-10-01.md", "photo.png")
        assertEquals("ideas.md" to "better onboarding", NoteText.target("ideas better onboarding", files))
        assertEquals("ideas.md" to "x", NoteText.target("IDEAS  x", files))
        assertNull(NoteText.target("ideas", files))                    // nothing to add yet
        assertNull(NoteText.target("ideas ", files))
        assertNull(NoteText.target("idea better", files))              // the whole word, not its start
        assertNull(NoteText.target("notes x", files))                  // the default file is not offered twice
        assertNull(NoteText.target("todo x", files))                   // a dated line would break the task list
        assertNull(NoteText.target("2026-10-01 x", files))
        assertNull(NoteText.target("photo x", files))
        assertNull(NoteText.target("", files))
    }

    @Test fun theDaysOwnFile() = assertEquals("2026-10-01.md", NoteText.daily("2026-10-01"))
}
