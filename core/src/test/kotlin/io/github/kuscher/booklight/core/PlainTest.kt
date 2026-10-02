package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PlainTest {
    @Test fun theMarksComeOff() {
        assertEquals("On Friday we meet at 3 pm.", Plain.of("**On Friday** we meet at *3 pm*."))
        assertEquals("Tasks", Plain.of("## Tasks"))
        assertEquals("- send the contract\n- book the room", Plain.of("* send the contract\n* book the room"))
        assertEquals("  - nested", Plain.of("  * nested"))
        assertEquals("Run gradlew test first.", Plain.of("Run `gradlew test` first."))
        assertEquals("val a = 1", Plain.of("```kotlin\nval a = 1\n```"))
        assertEquals("Really important", Plain.of("__Really__ important"))
    }

    @Test fun whatIsNotAMarkStays() {
        assertEquals("2 * 3 * 4 = 24", Plain.of("2 * 3 * 4 = 24"))
        assertEquals("snake_case_name and file_name.txt", Plain.of("snake_case_name and file_name.txt"))
        assertEquals("- already a dash", Plain.of("- already a dash"))
        assertEquals("a*b*c", Plain.of("a*b*c"))
        assertEquals("#3478f6 is a colour, #1 too", Plain.of("#3478f6 is a colour, #1 too"))
        assertEquals("line one\n\nline two", Plain.of("line one\n\nline two"))
        assertEquals("", Plain.of(""))
        assertEquals("5 * 4 and 3 * 2", Plain.of("5 * 4 and 3 * 2"))
    }

    @Test fun inSeveralLines() {
        assertEquals("Tasks\n- Anna: send the contract\n- Jonas: book the room", Plain.of("### Tasks\n* **Anna:** send the contract\n* **Jonas:** book the room"))
    }
}
