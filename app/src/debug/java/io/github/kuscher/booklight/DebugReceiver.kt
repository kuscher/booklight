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
 *   ai none|downloadable|downloading|ready|real (what the rows of a prompt show on a device in that state; `real` asks the device again)
 *   pref theme auto|light|dark | pref tint on|off | pref dim on|off | pref cards (show the first-run cards again) | pref nocards
 *   activity PKG/CLASS (is that activity open to other apps: why an app's shortcut is or is not offered)
 *   think on|off (the light of the model at work, without the model) | turn MS (close, and the key again MS later)
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
                // A name alone only prints: without a value it would store an empty one (`pref glass` once did).
                if (v.isNotEmpty() || k == "cards" || k == "nocards") when (k) {
                    "suggestions" -> app.prefs.update { it.copy(suggestions = v == "on", suggestionsCard = false) }
                    "engine" -> app.prefs.update { it.copy(engine = v) }
                    "glass" -> app.prefs.update { it.copy(glass = v) }
                    "opening" -> app.prefs.update { it.copy(opening = v) }
                    "theme" -> app.prefs.update { it.copy(theme = v) }
                    "tint" -> app.prefs.update { it.copy(tint = v == "on") }
                    "dim" -> app.prefs.update { it.copy(dim = v == "on") }
                    // tips on|off|again (from the first one)|at ID (that tip next)
                    "tips" -> app.prefs.update { s -> when (v) {
                        "on" -> s.copy(tips = true); "off" -> s.copy(tips = false)
                        "again" -> s.copy(tips = true, tipsSeen = emptyList(), tipId = "", tipMs = 0, used = emptyList())
                        else -> s
                    } }
                    "shadow" -> app.prefs.update { it.copy(shadow = v) }
                    "cards" -> app.prefs.update { it.copy(shortcutCard = true, suggestionsCard = true, suggestions = false, keySeen = false) }
                    "key" -> app.prefs.update { it.copy(keySeen = v == "seen") }
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
                    "right" -> if (!m.moveCell(1, 0) && !m.nudge(1) && m.opened == null) { if (m.onMore) m.open() else m.arm(1, wrap = false) }
                    "left" -> if (m.opened != null) m.close() else if (!m.moveCell(-1, 0) && !m.nudge(-1)) m.arm(-1, wrap = false)
                    "tab" -> if (m.tip != null) m.tipTab() else if (m.keyword != null) m.enterKeyword() else if (m.chosen()?.second?.effect is io.github.kuscher.booklight.core.Effect.EnterScope) m.enter { r, a -> act.run(r, a) } else if (m.opened != null) m.step(1) else m.arm(1, wrap = true)
                    "backtab" -> if (m.opened != null) m.step(-1) else m.arm(-1, wrap = true)
                    "more" -> { m.current?.let { m.armAt(it.actions.size) } }
                    "back" -> m.leaveScope()
                    "esc" -> if (!m.cancelConfirm()) act.close()
                    "enter" -> if (m.tip != null) m.tipEnter() else m.enter { r, a -> act.run(r, a) }
                    "stay" -> m.enter { r, a -> act.run(r, a, keep = true) }
                }
                out("ok")
            }
            // What an app's manifest shortcuts look like as XML: `./bl debug xml com.android.chrome`.
            "xml" -> {
                val pm = context.packageManager
                val sb = StringBuilder()
                for (ri in pm.queryIntentActivities(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER).setPackage(arg), android.content.pm.PackageManager.GET_META_DATA)) {
                    val x = runCatching { ri.activityInfo.loadXmlMetaData(pm, "android.app.shortcuts") }.getOrNull() ?: continue
                    sb.append(ri.activityInfo.name).append(": ")
                    var n = 0
                    while (x.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && n++ < 200) if (x.eventType == org.xmlpull.v1.XmlPullParser.START_TAG)
                        sb.append("<").append(x.name).append((0 until x.attributeCount).joinToString("") { " ${x.getAttributeName(it)}=${x.getAttributeValue(it)}" }).append("> ")
                }
                out(sb.toString().take(3500))
            }
            // `activity PKG/CLS`: what the system says about an activity (is it open to other apps): why an app's shortcut is or is not offered.
            "activity" -> out(runCatching {
                val i = context.packageManager.getActivityInfo(android.content.ComponentName.unflattenFromString(arg)!!, 0)
                "exported=${i.exported} enabled=${i.enabled} permission=${i.permission} target=${i.targetActivity}"
            }.getOrElse { "not found: ${it.javaClass.simpleName}" })
            // The panel is put away and the key comes again MS later: the turn, at a moment adb could never hit.
            "turn" -> main.post { act?.let { a -> a.close(); main.postDelayed({ a.turn(Intent()) }, arg.toLongOrNull() ?: 60L) }; out("ok") }
            "think" -> main.post { act?.model?.pretendThinking(arg == "on"); out("ok") }
            "ai" -> app.scope.launch {
                app.onDevice.pretend = when (arg) {
                    "none" -> io.github.kuscher.booklight.ai.OnDevice.State.NONE
                    "downloadable" -> io.github.kuscher.booklight.ai.OnDevice.State.DOWNLOADABLE
                    "downloading" -> io.github.kuscher.booklight.ai.OnDevice.State.DOWNLOADING
                    "ready" -> io.github.kuscher.booklight.ai.OnDevice.State.READY
                    else -> null
                }
                out("${app.onDevice.check()}")
            }
            // The pinned window, without the panel: `pin text gate B22`, `pin timer 90`, `pin answer 42`, `pin color #3478f6`, `pin qr https://…`, `unpin`.
            "pin" -> main.post {
                val (kind, text) = (arg.split(' ', limit = 2) + "").let { it[0] to it[1] }
                val e = when (kind) {
                    "timer" -> io.github.kuscher.booklight.core.Effect.Pin("timer", "", text.toLongOrNull() ?: 90, "tea")
                    "answer" -> io.github.kuscher.booklight.core.Effect.Pin("answer", text, note = "12 × 3.5")
                    "color" -> io.github.kuscher.booklight.core.Effect.Pin("color", text, value = (io.github.kuscher.booklight.core.Colors.parse(text)?.argb ?: 0).toLong())
                    else -> io.github.kuscher.booklight.core.Effect.Pin(kind, text)
                }
                out("${app.executor.run(e)}")
            }
            "unpin" -> main.post { out("${app.executor.run(io.github.kuscher.booklight.core.Effect.Unpin)}") }
            // Every example of the list of everything, run through the engine: what its first row is. An example must work.
            "guide" -> app.scope.launch {
                val sb = StringBuilder()
                for (e in app.guide.entries()) {
                    val scoped = app.engine.scopeFor(e.example)
                    val rows = if (scoped != null) app.engine.search(Query(scoped.text, scoped.scope.key, scoped.word)) else app.engine.search(Query(e.example))
                    val first = rows.firstOrNull()
                    val ok = first != null && first.actions.isNotEmpty() && (e.scope == null || scoped?.scope?.key == e.scope) && (first.kind != io.github.kuscher.booklight.core.Kind.WEB || e.id == "web" || e.id == "ask" || e.id == "links")
                    sb.append(if (ok) "ok " else "BAD ").append(e.id).append(" '").append(e.example).append("' = ").append(first?.let { (it.answer ?: it.title).take(24) + " [" + (scoped?.scope?.key ?: it.provider) + "]" }).append(" ; ")
                }
                out(sb.toString())
            }
            "close" -> main.post { act?.close(); out("ok") }
            "dump" -> main.post {
                val m = act?.model ?: return@post out("no panel")
                val d = act.window.decorView
                val loc = IntArray(2).also { d.getLocationOnScreen(it) }
                out("chip=${m.chip?.key} query='${m.query}' ai=${app.onDevice.state.value}${if (m.thinking) " thinking" else ""} selected=${m.selected} armed=${if (m.onMore) "more" else m.chosen()?.second?.id}${if (m.confirming) "?" else ""} opened=${m.opened} cell=${m.cell} flash=${m.flash} " +
                    "search=${m.lastSearchMicros}us window=${d.width}x${d.height}@${loc[0]},${loc[1]} blur=${act.windowManager.isCrossWindowBlurEnabled} completion=${m.completion} card=${m.card} tip=${m.tip?.id}${if (m.tipArmed != 0) ":" + m.tipArmed else ""}${if (m.tipOff) " off" else ""} rows=" +
                    m.results.joinToString(" | ") { describe(it) })
            }
            // A picture of the Booklight window's own content (PixelCopy: no other apps): `wshot NAME`.
            "wshot" -> main.post {
                val w = io.github.kuscher.booklight.window.MainActivity.current.get() ?: return@post out("no window")
                val v = w.window.decorView
                if (v.width == 0) return@post out("no window")
                val bmp = createBitmap(v.width, v.height)
                PixelCopy.request(w.window, bmp, { r ->
                    if (r == PixelCopy.SUCCESS) {
                        val f = File(context.cacheDir, "${arg.ifEmpty { "window" }}.png")
                        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        out(f.absolutePath)
                    } else out("failed $r")
                }, main)
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
            is io.github.kuscher.booklight.core.Body.Keys -> " {" + b.keys.joinToString(" + ") + "}"
            is io.github.kuscher.booklight.core.Body.Task -> if (b.done) " {done}" else " {open}"
            is io.github.kuscher.booklight.core.Body.Stream -> " {${b.caption}${if (b.answer) " =" else ":"} ${b.text}${if (b.busy) "…" else ""}${if (b.tall) " tall" else ""}${if (b.ask != null && !b.answer) " ?" else ""}}"
        }
        val acts = r.actions.mapIndexed { i, a -> (if (i == r.armed) "*" else "") + a.id + (if (a.more) "+" else "") }.joinToString(",")
        return "${r.answer ?: r.title}${r.subtitle?.let { " ($it)" } ?: ""} [${r.label ?: r.kind}]$body <$acts>"
    }
}
