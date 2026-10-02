package io.github.kuscher.booklight.device

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import io.github.kuscher.booklight.core.ImageUse
import io.github.kuscher.booklight.overlay.qr
import java.io.File

/** A QR code as a picture: on the clipboard, in Downloads, or handed to the share sheet. */
object QrImages {
    fun use(context: Context, text: String, use: ImageUse): Boolean {
        val bitmap = bitmap(text) ?: return false
        when (use) {
            ImageUse.SAVE -> {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "QR ${System.currentTimeMillis() / 1000}.png")
                    put(MediaStore.Downloads.MIME_TYPE, "image/png")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: return false
            }
            else -> {
                val file = File(File(context.cacheDir, "share").apply { mkdirs() }, "qr.png")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
                if (use == ImageUse.COPY) Clipboard.set(context, context.contentResolver, uri)
                else context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        return true
    }

    /** Black on white, with the quiet zone a scanner needs, about [side] px across. */
    private fun bitmap(text: String, side: Int = 960): Bitmap? {
        val m = qr(text) ?: return null
        val quiet = 4
        val px = (side / (m.width + 2 * quiet)).coerceAtLeast(2)
        val size = (m.width + 2 * quiet) * px
        val pixels = IntArray(size * size) { Color.WHITE }
        for (y in 0 until m.height) for (x in 0 until m.width) if (m.get(x, y)) {
            for (dy in 0 until px) { val row = ((y + quiet) * px + dy) * size + (x + quiet) * px; pixels.fill(Color.BLACK, row, row + px) }
        }
        return createBitmap(size, size).apply { setPixels(pixels, 0, size, 0, 0, size, size) }
    }
}
