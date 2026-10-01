package io.github.kuscher.booklight.device

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap
import io.github.kuscher.booklight.BooklightApp
import java.io.File

/**
 * New files and folders. They go into the shared Documents folder, which an app may add to
 * without any permission (it may not read what others put there, and doesn't). A file is opened
 * in whatever edits it; a folder in the Files app.
 */
object Files {
    fun create(context: Context, name: String, folder: Boolean, pick: Boolean): Boolean {
        val path = clean(name) ?: return false
        if (pick) {   // "Create in…": the system's own picker makes the (empty) file where the user says
            context.startActivity(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime(path))
                .putExtra(Intent.EXTRA_TITLE, path.substringAfterLast('/')).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        }
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), path)
        if (folder) {
            if (!file.isDirectory && !file.mkdirs()) return false
            show(context, Intent(Intent.ACTION_VIEW).setDataAndType(
                DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:${Environment.DIRECTORY_DOCUMENTS}/$path"), DocumentsContract.Document.MIME_TYPE_DIR))
            return true
        }
        file.parentFile?.mkdirs()
        if (!file.exists() && !file.createNewFile()) return false      // one that is already there is opened, never emptied
        // Other apps reach the file through the media index; it tells us the address once it has it.
        MediaScannerConnection.scanFile(context.applicationContext, arrayOf(file.path), arrayOf(mime(path))) { _, uri ->
            if (uri != null) show(context, Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime(path)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
        }
        return true
    }

    private fun show(context: Context, intent: Intent) {
        runCatching { context.applicationContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Log.i(BooklightApp.TAG, "made, but nothing opens it: ${it.message}") }
    }

    /** A path inside Documents: no way out of it, no empty parts. Null when nothing usable is left. */
    private fun clean(name: String): String? {
        val parts = name.replace('\\', '/').split('/').map { it.trim() }.filter { it.isNotEmpty() && it != "." }
        if (parts.isEmpty() || parts.any { it == ".." || it.any { c -> c in "<>:\"|?*" || c.code < 32 } }) return null
        return parts.joinToString("/")
    }

    private fun mime(path: String): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(path.substringAfterLast('.', "").lowercase()) ?: "text/plain"
}
