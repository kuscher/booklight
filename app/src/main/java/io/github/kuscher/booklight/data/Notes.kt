package io.github.kuscher.booklight.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.NoteText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The notes folder: `Notes.md`, `Todo.md`, a file per day and whatever other `.md` files the user
 * keeps there, in a folder picked once. A granted folder keeps working after a reinstall or on
 * another device's synced copy, which a file Booklight simply made in Documents would not (Android
 * stops treating it as Booklight's).
 *
 * Files are read only when a notes scope asks, and what was read is kept for a moment so that
 * typing in such a scope does not read the file again on every letter. The text itself (what an
 * entry looks like, how a task is ticked) is `core/NoteText.kt`.
 */
class Notes(private val context: Context, private val prefs: Prefs) {
    private val folder: Uri? get() = prefs.now.notesFolder?.toUri()
    private val resolver get() = context.contentResolver

    /** A folder is chosen and Booklight may still write to it. */
    val ready: Boolean
        get() = folder?.let { f -> resolver.persistedUriPermissions.any { it.uri == f && it.isWritePermission } } ?: false

    /** The folder's name, for showing where notes go. */
    val where: String?
        get() = folder?.let { DocumentsContract.getTreeDocumentId(it).substringAfter(':').substringAfterLast('/').ifEmpty { null } }

    // What was read last, and when: valid for a few seconds, and until Booklight writes.
    private class Kept(val at: Long, val text: String)
    private val kept = HashMap<String, Kept>()
    private var listed: Pair<Long, List<Pair<String, Uri>>>? = null

    /** The panel closed: nothing of the user's files stays in memory. */
    @Synchronized fun forget() { kept.clear(); listed = null }

    @Synchronized private fun children(): List<Pair<String, Uri>> {
        val tree = folder ?: return emptyList()
        listed?.let { if (System.currentTimeMillis() - it.first < KEEP_MS) return it.second }
        val out = ArrayList<Pair<String, Uri>>()
        try {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                // Every entry is kept for finding a file by its name (Notes.md may be the thousandth); `files()` caps what it offers.
                while (c.moveToNext() && out.size < MAX_LISTED) out.add(c.getString(1) to DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)))
            }
        } catch (e: Exception) { Log.w(BooklightApp.TAG, "notes folder not listed: ${e.javaClass.simpleName}") }
        listed = System.currentTimeMillis() to out
        return out
    }

    /** The `.md` files of the folder, by name. */
    fun files(): List<String> = if (ready) children().asSequence().map { it.first }.filter { it.endsWith(".md", ignoreCase = true) }.take(MAX_FILES).toList() else emptyList()

    private fun find(name: String): Uri? = children().firstOrNull { it.first.equals(name, ignoreCase = true) }?.second

    /** What a file holds; empty if it is not there yet. Null if the folder is gone or the file cannot be read. */
    @Synchronized fun read(name: String): String? {
        if (!ready) return null
        kept[name.lowercase()]?.let { if (System.currentTimeMillis() - it.at < KEEP_MS) return it.text }
        val uri = find(name) ?: return ""
        return try {
            // A notes file is text a person wrote: past a few megabytes only its end is read (the newest notes are there).
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val text = (if (bytes.size > MAX_BYTES) bytes.copyOfRange(bytes.size - MAX_BYTES, bytes.size) else bytes).toString(Charsets.UTF_8)
            kept[name.lowercase()] = Kept(System.currentTimeMillis(), text)
            text
        } catch (e: Exception) { Log.w(BooklightApp.TAG, "notes file not read: ${e.javaClass.simpleName}"); null }
    }

    private fun create(name: String): Uri? {
        val tree = folder ?: return null
        return DocumentsContract.createDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)), "text/markdown", name)
            .also { synchronized(this) { listed = null } }
    }

    /** Adds [entry] at the end of [name], on a line of its own. */
    private fun add(name: String, entry: (lead: String) -> String): Boolean {
        if (!ready) return false
        return try {
            val file = find(name) ?: create(name) ?: return false
            // If the file's last line was left without a line break (edited by hand), start a new one first.
            val open = resolver.openInputStream(file)?.use { it.readBytes() }?.let { it.isNotEmpty() && it.last() != '\n'.code.toByte() } ?: false
            resolver.openOutputStream(file, "wa")?.use { it.write(entry(if (open) "\n" else "").toByteArray()) } ?: return false
            synchronized(this) { kept.remove(name.lowercase()) }
            true
        } catch (e: Exception) {
            Log.w(BooklightApp.TAG, "note not added", e)
            false
        }
    }

    /** Adds one note as a dated line: to Notes.md, to the day's own file ([Effect.AppendNote.TODAY]), or to the file named. */
    fun append(text: String, file: String = "", now: LocalDateTime = LocalDateTime.now()): Boolean = when (file) {
        "" -> add(NoteText.NOTES) { it + NoteText.entry(text, now.format(STAMP)) }
        Effect.AppendNote.TODAY -> add(today(now.toLocalDate())) { it + NoteText.entry(text, now.format(TIME)) }
        else -> add(file) { it + NoteText.entry(text, now.format(STAMP)) }
    }

    /** The file a day's own notes go to. */
    fun today(date: LocalDate = LocalDate.now()): String = NoteText.daily(date.toString())

    fun addTodo(text: String, now: LocalDate = LocalDate.now()): Boolean = add(NoteText.TODO) { it + NoteText.todo(text, now.toString()) }

    /** Ticks or unticks a task of Todo.md. The whole file is written again, as it was but for that one mark. */
    fun tick(line: Int, text: String, done: Boolean): Boolean {
        if (!ready) return false
        return try {
            val file = find(NoteText.TODO) ?: return false
            val bytes = resolver.openInputStream(file)?.use { it.readBytes() } ?: return false
            val before = bytes.toString(Charsets.UTF_8)
            // The whole file is written back: one that does not read as UTF-8 (an old Windows file) would come out damaged. Left alone.
            if (!before.toByteArray(Charsets.UTF_8).contentEquals(bytes)) return false
            val after = NoteText.tick(before, line, text, done) ?: return false
            resolver.openOutputStream(file, "wt")?.use { it.write(after.toByteArray()) } ?: return false
            synchronized(this) { kept.remove(NoteText.TODO.lowercase()) }
            true
        } catch (e: Exception) {
            Log.w(BooklightApp.TAG, "task not ticked", e)
            false
        }
    }

    /** The address of a file of the folder, to hand to whatever edits text; null if it is not there. */
    fun uri(name: String): Uri? = if (ready) find(name) else null

    private companion object {
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        const val KEEP_MS = 4000L
        const val MAX_BYTES = 2 * 1024 * 1024
        const val MAX_FILES = 500
        /** How many entries of the folder are read at most: enough for a large vault, not without end. */
        const val MAX_LISTED = 20_000
    }
}
