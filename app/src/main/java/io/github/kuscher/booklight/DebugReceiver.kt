package io.github.kuscher.booklight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.overlay.OverlayActivity
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Test hooks for driving Booklight from adb (`./bl debug …`). The receiver requires the DUMP
 * permission, which only the shell (adb) holds, so other apps can't use it.
 *   ping | dump | type TEXT | key up|down|tab|esc|enter | close | shot [NAME]
 *   find TEXT (ranked results without the panel) | apps | forget
 *   pref suggestions on|off | pref engine ID | pref cards (show the first-run cards again)
 */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cmd = intent.getStringExtra("c") ?: return
        val parts = cmd.split(' ', limit = 2)
        val arg = parts.getOrNull(1).orEmpty()
        val app = BooklightApp.instance
        val act = OverlayActivity.current.get()
        val main = Handler(Looper.getMainLooper())
        fun out(s: String) { Log.i(BooklightApp.TAG, "debug ${parts[0]} -> $s") }
        when (parts[0]) {
            "ping" -> out("pong ${BuildConfig.VERSION_NAME}")
            "apps" -> out("${app.apps.count} apps, indexed in ${app.apps.loadedMs} ms")
            "forget" -> { app.historyStore.clear(); out("ok") }
            "pref" -> {
                val (k, v) = (arg.split(' ') + "").let { it[0] to it[1] }
                when (k) {
                    "suggestions" -> app.prefs.update { it.copy(suggestions = v == "on", suggestionsCard = false) }
                    "engine" -> app.prefs.update { it.copy(engine = v) }
                    "cards" -> app.prefs.update { it.copy(shortcutCard = true, suggestionsCard = true, suggestions = false) }
                    "nocards" -> app.prefs.update { it.copy(shortcutCard = false, suggestionsCard = false) }
                }
                out(app.prefs.now.let { "engine=${it.engine} suggestions=${it.suggestions} cards=${it.shortcutCard},${it.suggestionsCard}" })
            }
            "find" -> app.scope.launch {
                val t0 = System.nanoTime()
                val r = app.engine.search(Query(arg))
                out("${(System.nanoTime() - t0) / 1000} us | " + r.joinToString(" | ") { "${it.title} [${it.kind}]" })
            }
            "type" -> main.post { act?.model?.type(arg); out(if (act != null) "ok" else "no panel") }
            "key" -> main.post {
                val m = act?.model ?: return@post out("no panel")
                when (arg) {
                    "down" -> m.move(1)
                    "up" -> m.move(-1)
                    "tab" -> m.openActions()
                    "esc" -> if (!m.closeActions()) act.close()
                    "enter" -> m.chosen()?.let { act.run(it.first, it.second) }
                }
                out("ok")
            }
            "close" -> main.post { act?.close(); out("ok") }
            "dump" -> main.post {
                val m = act?.model ?: return@post out("no panel")
                val d = act.window.decorView
                val loc = IntArray(2).also { d.getLocationOnScreen(it) }
                out("query='${m.query}' selected=${m.selected} search=${m.lastSearchMicros}us window=${d.width}x${d.height}@${loc[0]},${loc[1]} " +
                    "blur=${act.windowManager.isCrossWindowBlurEnabled} actions=${m.actionsOf?.title} completion=${m.completion} site=${m.site?.name} card=${m.card} rows=" +
                    m.results.joinToString(" | ") { "${it.title} [${it.kind}]" })
            }
            "shot" -> main.post {
                val a = act ?: return@post out("no panel")
                val v = a.window.decorView
                if (v.width == 0) return@post out("no window")
                val bmp = createBitmap(v.width, v.height)
                PixelCopy.request(a.window, bmp, { r ->
                    if (r == PixelCopy.SUCCESS) {
                        val f = File(context.cacheDir, "${arg.ifEmpty { "shot" }}.png")
                        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        out(f.absolutePath)
                    } else out("failed $r")
                }, main)
            }
            else -> out("unknown")
        }
    }
}
