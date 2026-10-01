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
 * Test hooks for driving Booklight from adb (`./bl debug …`). Debug builds only. The receiver requires
 * the DUMP permission, which only the shell (adb) holds, so other apps can't use it.
 *   keys TEXT (typed one character at a time, as a person does: every list in between is made) |
 *   ping | dump | type TEXT | key up|down|left|right|tab|backtab|esc|enter|stay|back | close | shot [NAME]
 *   find TEXT (ranked results without the panel; "KEY: TEXT" searches inside a scope) | apps | forget
 *   pref suggestions on|off | pref engine ID | pref glass clear|balanced|frosted|solid | pref opening off|fast|medium|slow
 *   pref theme auto|light|dark | pref tint on|off | pref dim on|off | pref cards (show the first-run cards again) | pref nocards
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
                    "glass" -> app.prefs.update { it.copy(glass = v) }
                    "opening" -> app.prefs.update { it.copy(opening = v) }
                    "theme" -> app.prefs.update { it.copy(theme = v) }
                    "tint" -> app.prefs.update { it.copy(tint = v == "on") }
                    "dim" -> app.prefs.update { it.copy(dim = v == "on") }
                    "cards" -> app.prefs.update { it.copy(shortcutCard = true, suggestionsCard = true, suggestions = false) }
                    "nocards" -> app.prefs.update { it.copy(shortcutCard = false, suggestionsCard = false) }
                }
                out(app.prefs.now.let { "engine=${it.engine} suggestions=${it.suggestions} cards=${it.shortcutCard},${it.suggestionsCard} glass=${it.glass} opening=${it.opening} theme=${it.theme} tint=${it.tint} dim=${it.dim}" })
            }
            "find" -> app.scope.launch {
                val t0 = System.nanoTime()
                val scoped = app.engine.scopeFor(arg)
                val r = if (scoped != null) app.engine.search(Query(scoped.text, scoped.scope.key, scoped.word)) else app.engine.search(Query(arg))
                out("${(System.nanoTime() - t0) / 1000} us | " + r.joinToString(" | ") { describe(it) })
            }
            "keys" -> main.post {
                val m = act?.model ?: return@post out("no panel")
                while (m.leaveScope()) {}
                m.type("")
                // One character at a time, a frame apart, building on whatever the field holds by then (a keyword may have become the chip).
                arg.forEachIndexed { i, c -> main.postDelayed({ m.type(m.query + c) }, 40L * (i + 1)) }
                main.postDelayed({ out("ok") }, 40L * (arg.length + 2))
            }
            "type" -> main.post { act?.model?.let { m -> while (m.leaveScope()) {}; m.type(arg) }; out(if (act != null) "ok" else "no panel") }
            "key" -> main.post {
                val m = act?.model ?: return@post out("no panel")
                when (arg) {
                    "down" -> if (!m.moveCell(0, 1)) m.move(1)
                    "up" -> if (!m.moveCell(0, -1) && !m.restoreLast()) m.move(-1)
                    "right" -> if (!m.moveCell(1, 0) && !m.nudge(1)) m.arm(1, wrap = false)
                    "left" -> if (!m.moveCell(-1, 0) && !m.nudge(-1)) m.arm(-1, wrap = false)
                    "tab" -> if (m.chosen()?.second?.effect is io.github.kuscher.booklight.core.Effect.EnterScope) m.enter { r, a -> act.run(r, a) } else m.arm(1, wrap = true)
                    "backtab" -> m.arm(-1, wrap = true)
                    "back" -> m.leaveScope()
                    "esc" -> if (!m.cancelConfirm()) act.close()
                    "enter" -> m.enter { r, a -> act.run(r, a) }
                    "stay" -> m.enter { r, a -> act.run(r, a, keep = true) }
                }
                out("ok")
            }
            "close" -> main.post { act?.close(); out("ok") }
            "dump" -> main.post {
                val m = act?.model ?: return@post out("no panel")
                val d = act.window.decorView
                val loc = IntArray(2).also { d.getLocationOnScreen(it) }
                out("chip=${m.chip?.key} query='${m.query}' selected=${m.selected} armed=${m.chosen()?.second?.id}${if (m.confirming) "?" else ""} cell=${m.cell} flash=${m.flash} " +
                    "search=${m.lastSearchMicros}us window=${d.width}x${d.height}@${loc[0]},${loc[1]} blur=${act.windowManager.isCrossWindowBlurEnabled} completion=${m.completion} card=${m.card} rows=" +
                    m.results.joinToString(" | ") { describe(it) })
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

    /** A row in one line: its title, kind, what it holds, and its actions with the armed one marked. */
    private fun describe(r: io.github.kuscher.booklight.core.Result): String {
        val body = when (val b = r.body) {
            null -> ""
            is io.github.kuscher.booklight.core.Body.Slots -> " {" + listOfNotNull(b.caption).plus(b.slots.map { "${it.label}=${it.value}${if (it.state == io.github.kuscher.booklight.core.SlotState.GUESSED) "?" else ""}" }).plus(listOfNotNull(b.note)).joinToString("; ") + "}"
            is io.github.kuscher.booklight.core.Body.Level -> " {${b.percent}%${if (b.muted) " muted" else ""}${if (b.locked) " locked" else ""}${b.target?.let { " →$it" } ?: ""}}"
            is io.github.kuscher.booklight.core.Body.Grid -> " {${b.cells.size} cells: ${b.cells.take(6).joinToString("") { it.glyph }}…}"
            is io.github.kuscher.booklight.core.Body.Code -> " {qr}"
            is io.github.kuscher.booklight.core.Body.Mono -> " {${b.text}}"
        }
        val acts = r.actions.mapIndexed { i, a -> (if (i == r.armed) "*" else "") + a.id }.joinToString(",")
        return "${r.answer ?: r.title}${r.subtitle?.let { " ($it)" } ?: ""} [${r.kind}]$body <$acts>"
    }
}
