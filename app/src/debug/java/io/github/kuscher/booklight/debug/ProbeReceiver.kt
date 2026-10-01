package io.github.kuscher.booklight.debug

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import java.io.File

/**
 * Feasibility probes for features that are planned, not built (debug builds only, DUMP-guarded:
 * `./bl probe files|state`). Each line says what a plain app may do on this device with no
 * permission. Throwaway: delete once the features exist.
 */
class ProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val what = intent.getStringExtra("c") ?: return
        fun out(s: String) = Log.i("Booklight", "probe $what -> $s")
        fun attempt(name: String, block: () -> Any?) = try { "$name: ok ${block() ?: ""}" } catch (e: Throwable) { "$name: ${e.javaClass.simpleName} ${e.message?.take(90)}" }
        when (what) {
            "files" -> {
                val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val dir = File(docs, "Booklight")
                val note = File(dir, "probe.md")
                out(listOf(
                    attempt("mkdir Documents/Booklight") { dir.mkdirs() || dir.isDirectory },
                    attempt("write probe.md") { note.writeText("probe\n"); note.length() },
                    attempt("append") { note.appendText("second line\n"); note.readText().lines().size },
                    attempt("mkdir nested") { File(dir, "Folder").mkdirs() || File(dir, "Folder").isDirectory },
                    attempt("write Download/") { File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "booklight-probe.txt").apply { writeText("x") }.delete() },
                    attempt("new top-level folder") { File(Environment.getExternalStorageDirectory(), "BooklightTop").mkdirs() },
                    attempt("MediaStore insert") {
                        val v = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "probe-ms.md"); put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Booklight")
                        }
                        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), v)
                        uri?.also { context.contentResolver.openOutputStream(it)?.use { o -> o.write("ms\n".toByteArray()) } }
                    },
                    attempt("list") { dir.list()?.joinToString(",") },
                    attempt("cleanup") { File(dir, "Folder").delete(); note.delete(); File(dir, "probe-ms.md").delete(); dir.delete() },
                ).joinToString(" | "))
            }
            "state" -> {
                val am = context.getSystemService(AudioManager::class.java)
                val nm = context.getSystemService(NotificationManager::class.java)
                val alarm = context.getSystemService(AlarmManager::class.java)
                out(listOf(
                    attempt("music volume") { "${am.getStreamVolume(AudioManager.STREAM_MUSIC)}/${am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)} fixed=${am.isVolumeFixed} musicActive=${am.isMusicActive}" },
                    attempt("dnd access granted") { nm.isNotificationPolicyAccessGranted },
                    attempt("interruption filter") { nm.currentInterruptionFilter },
                    attempt("can write settings") { Settings.System.canWrite(context) },
                    attempt("brightness") { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) },
                    attempt("screen off timeout") { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT) },
                    attempt("exact alarms allowed") { alarm.canScheduleExactAlarms() },
                ).joinToString(" | "))
            }
            else -> out("unknown")
        }
    }
}
