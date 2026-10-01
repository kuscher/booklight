package io.github.kuscher.booklight.core

/** One note found in the notes file: its [text] without the stamp, the [date] it carries ("2026-10-01 14:02", or nothing), and its [line]. */
data class NoteLine(val text: String, val date: String, val line: Int)

/** One task of Todo.md: what it says, the day it was added (or nothing), whether it is ticked, and its [line] in the file. */
data class TodoItem(val text: String, val date: String, val done: Boolean, val line: Int)

/**
 * The text of the notes files, with no Android in it: what a new entry looks like, how notes are
 * found, how a task is ticked. The files are the user's and may have been edited by hand, on
 * another system, or be huge: nothing here assumes Booklight wrote every line.
 */
object NoteText {
    const val NOTES = "Notes.md"
    const val TODO = "Todo.md"
    private val STAMP = Regex("""^(\d{4}-\d{2}-\d{2}(?: \d{2}:\d{2})?)\s+""")
    private val DAILY = Regex("""^\d{4}-\d{2}-\d{2}\.md$""", RegexOption.IGNORE_CASE)
    private val BOX = Regex("""^\s*[-*] \[([ xX])] """)

    /** A note as it is added to a file: a dated line, further lines of it indented under it. [stamp] is "2026-10-01 14:02", or a time alone in a day's own file. */
    fun entry(text: String, stamp: String): String {
        val lines = text.trim().lines()
        return "- $stamp  ${lines.first()}\n" + lines.drop(1).joinToString("") { "  $it\n" }
    }

    /** A task as it is added to Todo.md; a line break in it becomes a space, so a task is always one line. */
    fun todo(text: String, date: String): String = "- [ ] $date  ${text.trim().lines().joinToString(" ") { it.trim() }}\n"

    /** The file a day's own notes go to. */
    fun daily(date: String): String = "$date.md"

    /** What has to stand before a new entry: a line break, if the file's last line was left without one. */
    fun lead(content: String): String = if (content.isEmpty() || content.endsWith("\n")) "" else "\n"

    /**
     * The notes that hold every word of [query], newest first (the file grows at its end); with
     * nothing asked for, the latest. A note is a line and the indented lines under it.
     */
    fun find(content: String, query: String, limit: Int): List<NoteLine> {
        val words = Matcher.fold(query).split(' ').filter { it.isNotEmpty() }
        val out = ArrayList<NoteLine>()
        val lines = content.split('\n')
        var i = lines.size - 1
        while (i >= 0 && out.size < limit) {
            // Walk up to the line this entry starts on: lines indented under another belong to it.
            var start = i
            while (start > 0 && continues(lines[start])) start--
            val raw = lines.subList(start, i + 1).map { it.trimEnd('\r') }
            i = start - 1
            val first = raw.first().trimStart().removePrefix("- ").removePrefix("* ").trimStart('#', ' ')
            if (first.isBlank() && raw.size == 1) continue
            val stamp = STAMP.find(first)
            val text = (listOf(if (stamp != null) first.substring(stamp.range.last + 1) else first) + raw.drop(1).map { it.trim() })
                .filter { it.isNotEmpty() }.joinToString(" ↵ ")
            if (text.isEmpty()) continue
            val folded = Matcher.fold(text)
            if (words.all { folded.contains(it) }) out.add(NoteLine(text, stamp?.groupValues?.get(1).orEmpty(), start))
        }
        return out
    }

    private fun continues(line: String) = (line.startsWith("  ") || line.startsWith("\t")) && line.isNotBlank()

    /** Every task of the file, ticked or not, in the file's order. */
    fun tasks(content: String): List<TodoItem> {
        val out = ArrayList<TodoItem>()
        content.split('\n').forEachIndexed { n, l ->
            val line = l.trimEnd('\r')
            val box = BOX.find(line) ?: return@forEachIndexed
            val rest = line.substring(box.range.last + 1)
            val stamp = STAMP.find(rest)
            val text = (if (stamp != null) rest.substring(stamp.range.last + 1) else rest).trim()
            if (text.isNotEmpty()) out.add(TodoItem(text, stamp?.groupValues?.get(1).orEmpty(), box.groupValues[1] != " ", n))
        }
        return out
    }

    /**
     * The file with the task on [line] ticked ([done]) or unticked. The line must still be that
     * task: if the file changed meanwhile, the nearest line that says [text] is taken instead;
     * null if there is none (nothing is written then). Line ends stay as they were.
     */
    fun tick(content: String, line: Int, text: String, done: Boolean): String? {
        val lines = content.split('\n').toMutableList()
        val all = tasks(content).filter { it.text == text }
        val item = all.firstOrNull { it.line == line } ?: all.minByOrNull { kotlin.math.abs(it.line - line) } ?: return null
        val old = lines[item.line]
        val box = BOX.find(old) ?: return null
        val at = box.range.last - 2          // the character between the brackets
        lines[item.line] = old.substring(0, at) + (if (done) "x" else " ") + old.substring(at + 1)
        return lines.joinToString("\n")
    }

    /**
     * "ideas better onboarding" with a file called ideas.md in the folder: that file, and the rest
     * as the note. The whole first word must be the file's name, whatever its case; Notes.md,
     * Todo.md and the days' own files are never offered this way.
     */
    fun target(arg: String, files: List<String>): Pair<String, String>? {
        val t = arg.trimStart()
        val space = t.indexOfFirst { it.isWhitespace() }
        if (space <= 0) return null
        val word = t.substring(0, space)
        val rest = t.substring(space).trim()
        if (rest.isEmpty()) return null
        val file = files.firstOrNull { f ->
            f.endsWith(".md", ignoreCase = true) && f.dropLast(3).equals(word, ignoreCase = true) &&
                !f.equals(NOTES, ignoreCase = true) && !f.equals(TODO, ignoreCase = true) && !DAILY.matches(f)
        } ?: return null
        return file to rest
    }
}
