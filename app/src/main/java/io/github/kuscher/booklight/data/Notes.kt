package io.github.kuscher.booklight.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import io.github.kuscher.booklight.BooklightApp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Quick notes: lines added to `Notes.md` in a folder the user picked once. A granted folder keeps
 * working after a reinstall or on another device's synced copy, which a file Booklight simply
 * made in Documents would not (Android stops treating it as Booklight's).
 */
class Notes(private val context: Context, private val prefs: Prefs) {
    private val folder: Uri? get() = prefs.now.notesFolder?.toUri()

    /** A folder is chosen and Booklight may still write to it. */
    val ready: Boolean
        get() = folder?.let { f -> context.contentResolver.persistedUriPermissions.any { it.uri == f && it.isWritePermission } } ?: false

    /** The folder's name, for showing where notes go. */
    val where: String?
        get() = folder?.let { DocumentsContract.getTreeDocumentId(it).substringAfter(':').substringAfterLast('/').ifEmpty { null } }

    /** Adds one note as a dated line at the end of the file; further lines of it are indented under it. */
    fun append(text: String, now: LocalDateTime = LocalDateTime.now()): Boolean {
        val tree = folder ?: return false
        if (!ready) return false
        return try {
            val file = find(tree) ?: DocumentsContract.createDocument(context.contentResolver,
                DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)), "text/markdown", NAME) ?: return false
            val lines = text.trim().lines()
            // If the file's last line was left without a line break (edited by hand), start a new one first.
            val open = context.contentResolver.openInputStream(file)?.use { it.readBytes() }?.let { it.isNotEmpty() && it.last() != '\n'.code.toByte() } ?: false
            val entry = (if (open) "\n" else "") + "- ${now.format(STAMP)}  ${lines.first()}\n" + lines.drop(1).joinToString("") { "  $it\n" }
            context.contentResolver.openOutputStream(file, "wa")?.use { it.write(entry.toByteArray()) } ?: return false
            true
        } catch (e: Exception) {
            Log.w(BooklightApp.TAG, "note not added", e)
            false
        }
    }

    private fun find(tree: Uri): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            while (c.moveToNext()) if (c.getString(1).equals(NAME, ignoreCase = true)) return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
        }
        return null
    }

    private companion object {
        const val NAME = "Notes.md"
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
