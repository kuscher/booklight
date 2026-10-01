package io.github.kuscher.booklight.entry

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R

/**
 * Asks once where notes should go: shows the system's folder picker and keeps the grant. It has
 * no screen of its own. If it was started with a note to add (`note`), the note goes in as soon
 * as the folder is chosen.
 */
class PickFolderActivity : ComponentActivity() {
    private val pick = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        val app = application as BooklightApp
        if (tree != null) {
            contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            app.prefs.update { it.copy(notesFolder = tree.toString()) }
            val note = intent.getStringExtra(EXTRA_NOTE)
            if (note != null) Toast.makeText(this, if (app.notes.append(note)) R.string.done_note else R.string.failed_note, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pick.launch(null)
    }

    companion object { const val EXTRA_NOTE = "note" }
}
