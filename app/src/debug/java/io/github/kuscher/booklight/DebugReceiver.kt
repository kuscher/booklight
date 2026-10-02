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
 *   ping | dump | type TEXT | key up|down|left|right|tab|backtab|esc|enter|stay|back|more|window | close | shot [NAME]
 *   find TEXT (ranked results without the panel; "KEY: TEXT" searches inside a scope) | apps | forget
 *   pref suggestions on|off | pref engine ID | pref glass clear|balanced|frosted|solid | pref opening off|fast|medium|slow
 *   ai none|downloadable|downloading|ready|real (what the rows of a prompt show on a device in that state; `real` asks the device again)
 *   pref theme auto|light|dark | pref tint on|off | pref dim on|off | pref cards (show the first-run cards again) | pref nocards
 *   activity PKG/CLASS (is that activity open to other apps: why an app's shortcut is or is not offered)
 *   think on|off (the light of the model at work, without the model) | turn MS (close, and the key again MS later)
 *   window (the Booklight window: its size, its section, where the keys are) | window close | window hover KEY | window unhover KEY | window press KEY | window release KEY |
 *   window trace N (the layout of the next N frames, to the log) | window helper | window helper close |
 *   window film N EVERY (N pictures of the window, one every EVERY frames, in cache/film.png) | wshot [NAME]
 *   pill N (the next N frames in which the list's pill travels, to the log: ms since it set off, its edges as drawn, its row) |
 *   held down|up [TIMES] [MS] (the key as a held key repeats it: 8 times, one every 50 ms, unless said otherwise)
 *   pref zero on|off ("Show your usual") | zero (the usual rows as they would stand now; needs no panel) | seed (after forget:
 *   five apps as if run 8, 5, 3, 2 and 1 times) | unhide (empties the "Don't suggest" list)
 *   in TEXT (types under the chip that is there, which `type` would leave first)
 *   copy TEXT (puts TEXT on the clipboard the way another app would: without Booklight's label, so its line is offered;
 *   `copy private TEXT` marks it private) | key tab and key down open the copy's line, key back is Backspace on the empty field
 *   pref flightkey KEY|none (the user's key for the flight service; never printed) | flight TEXT (a flight's row, looked up at once)
 *   appsearch (a search inside an app: every line of the bundled table and every search an app declares, with the app, the source
 *   and whether this device takes it; a star marks the one in use. Run it on a Googlebook before a release)
 *   players (the music apps `play` offers, and the one asked last; `find play TEXT` shows the row and which is armed)
 *   pref spotifykey ID SECRET|none (the user's key for Spotify; never printed) | song TEXT (what Spotify has for the text after `play`,
 *   looked up at once: what was found and the address that would be sent)
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
            "forget" -> { app.historyStore.clear(); app.prefs.update { it.copy(zeroHeld = emptyList(), zeroHidden = emptyList()) }; out("ok") }
            // The usual rows as they would stand now: id, why, weight, age; the held and hidden ids; how long it took. Needs no panel.
            "zero" -> app.scope.launch {
                val u = app.usual(); val now = System.currentTimeMillis(); val s = app.prefs.now
                out("zero=${if (s.zero) "on" else "off"} rows=" + u.picks.joinToString(" | ") { "${it.id} ${it.why} ${"%.2f".format(it.weight)} ${(now - it.last) / 60_000}min" } +
                    " held=${s.zeroHeld} keep=${u.held} hidden=${s.zeroHidden} took=${u.micros}us")
            }
            // After `forget`: five apps of this device as if run 8, 5, 3, 2 and 1 times, from 30 days to 5 minutes ago.
            "seed" -> app.scope.launch {
                app.apps.ready.await()
                val ids = app.apps.ids().sorted().take(5)
                val now = System.currentTimeMillis(); val day = 24L * 60 * 60 * 1000
                val runs = listOf(8 to 30 * day, 5 to 3 * day, 3 to 2 * day, 2 to 1 * day, 1 to 5 * 60 * 1000L)
                ids.zip(runs).forEach { (id, r) -> repeat(r.first) { app.historyStore.history.record("", id, now - r.second) } }
                app.historyStore.changed()
                out("seeded ${ids.size}")
            }
            "unhide" -> { app.prefs.update { it.copy(zeroHidden = emptyList()) }; out("ok") }
            // The music apps `play` offers, in the row's order: each one's name, package and whether it is known to only search; the one asked last.
            "players" -> app.scope.launch {
                app.apps.ready.await()
                out(io.github.kuscher.booklight.providers.Players.find(context, app.apps).joinToString(" | ") { "${it.name} ${it.pkg}${if (it.searches) " searches" else ""}" }.ifEmpty { "none" } + " last=${app.prefs.now.player}")
            }
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
                    "zero" -> app.prefs.update { it.copy(zero = v == "on") }
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
                    // The user's key for the flight service: `pref flightkey KEY`, `pref flightkey none`. It is never printed.
                    "flightkey" -> app.prefs.setFlightKey(if (v == "none") "" else v)
                    // The user's key for Spotify: `pref spotifykey ID SECRET`, `pref spotifykey none`. It is never printed.
                    "spotifykey" -> arg.split(' ').let { w -> if (v == "none") app.prefs.setSpotifyKey("", "") else app.prefs.setSpotifyKey(v, w.getOrElse(2) { "" }) }
                }
                out(app.prefs.now.let { "engine=${it.engine} suggestions=${it.suggestions} cards=${it.shortcutCard},${it.suggestionsCard} glass=${it.glass} opening=${it.opening} theme=${it.theme} tint=${it.tint} dim=${it.dim} shadow=${it.shadow} tips=${it.tips} copy=${it.copyRow} zero=${it.zero} hidden=${it.zeroHidden.size} flightkey=${if (app.prefs.flightKey.value.isEmpty()) "none" else "set"} spotifykey=${if (app.prefs.spotifyKey.value.isEmpty()) "none" else "set"}" })
            }
            "find" -> app.scope.launch {
                val t0 = System.nanoTime()
                val scoped = app.engine.scopeFor(arg)
                val r = if (scoped != null) app.engine.search(Query(scoped.text, scoped.scope.key, scoped.word)) else app.engine.search(Query(arg))
                out("${(System.nanoTime() - t0) / 1000} us | " + r.joinToString(" | ") { describe(it) })
            }
            // A flight's row without the panel, looked up at once: `flight LH455`, `flight lh455 fri`, `flight ps5`. With a key in, this asks the service.
            "flight" -> app.scope.launch {
                val row = app.flights.rows(arg).firstOrNull() ?: return@launch out("not a flight")
                out(describe(app.flights.answer(row.id, pause = false) ?: row) + " left=${app.flights.left.value}")
            }
            // A search inside an app: every line of the bundled table and every search an app declares, each with the app, the
            // source and whether this device takes it; a star marks the one each app uses. The whole list goes to the log a line
            // each (`./bl logs`), and as much of it as one log line holds is the answer.
            "appsearch" -> app.scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                app.apps.ready.await(); app.commands.ready.await()
                val lines = app.commands.search.report()
                lines.forEach { Log.i(BooklightApp.TAG, "appsearch $it") }
                val all = lines.joinToString(" | ")
                out(if (all.length <= 3500) all else all.take(3500) + " … (${lines.size} in all: ./bl logs)")
            }
            // What Spotify has for the text after `play`, looked up at once and without the panel: `song album discovery by daft punk`.
            // With a key in, this asks Spotify. It prints what was found and the address that would be sent, never the key or its token.
            "song" -> app.scope.launch {
                val what = io.github.kuscher.booklight.core.Play.read(arg) ?: return@launch out("nothing to play")
                val k = app.songs.find(what) ?: return@launch out("no key")
                out(k.song?.let { "${it.kind} '${it.name}' by ${it.artists.joinToString(", ").ifEmpty { "-" }} album=${it.album.ifEmpty { "-" }} owner=${it.owner.ifEmpty { "-" }} starts=${it.starts.ifEmpty { "-" }} -> ${it.address}" } ?: "nothing: ${k.failure}")
            }
            "keys" -> main.post {
                val m = act?.model ?: return@post out("no panel")
                while (m.leaveScope()) {}
                m.type("")
                // One character at a time, a frame apart, building on whatever the field holds by then (a keyword may have become the chip).
                arg.forEachIndexed { i, c -> main.postDelayed({ m.type(m.query + c) }, 40L * (i + 1)) }
                main.postDelayed({ out("ok") }, 40L * (arg.length + 2))
            }
            // Under whatever chip is there (the copy's, a scope's): `type` leaves it first.
            "in" -> main.post { act?.model?.type(arg); out(if (act != null) "ok" else "no panel") }
            "type" -> main.post { act?.model?.let { m -> while (m.leaveScope()) {}; m.type(arg) }; out(if (act != null) "ok" else "no panel") }
            "key" -> main.post {
                val m = act?.model ?: return@post out("no panel")
                when (arg) {
                    "down" -> m.down(false)
                    "up" -> m.up(false)
                    // As the keys do (`Panel.keys`): Tab and Right only move on a row that offers more than one thing; Right again on Window or on the arrow opens its list.
                    "right" -> if (!m.moveCell(1, 0) && !m.nudge(1)) { if (m.tabEnters) m.fill() else if (m.opened == null) { if (m.onList != null) m.open() else m.arm(1, wrap = false) } }
                    "left" -> if (m.opened != null) m.close() else if (!m.moveCell(-1, 0) && !m.nudge(-1)) m.arm(-1, wrap = false)
                    "tab" -> if (m.tip != null) m.tipTab() else if (m.zeroUp && m.current == null && m.chip == null && m.query.isBlank()) m.down(false) else if (m.copy != null || m.bare) m.tabCopy() else if (m.keyword != null) m.enterKeyword() else if (m.tabEnters) m.fill() else if (m.opened != null) m.step(1) else if (m.otherAct != null) m.swap() else m.arm(1, wrap = true)
                    "backtab" -> if (m.opened != null) m.step(-1) else if (m.otherAct != null) m.swap() else m.arm(-1, wrap = true)
                    // Straight to one of the row's two list stops: `key more` the arrow, `key window` Window. Enter (or `key right`) then opens it.
                    "more" -> m.armList(io.github.kuscher.booklight.core.Behind.ARROW)
                    "window" -> m.armList(io.github.kuscher.booklight.core.Behind.WINDOW)
                    "back" -> m.back()
                    "esc" -> if (!m.cancelConfirm() && !m.leaveAnswer()) act.close()
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
            "copy" -> {
                val secret = arg.startsWith("private ")
                val clip = android.content.ClipData.newPlainText("test", arg.removePrefix("private "))
                if (secret) clip.description.extras = android.os.PersistableBundle().apply { putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true) }
                context.getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(clip)
                out("ok")
            }
            "think" -> main.post { act?.model?.pretendThinking(arg == "on"); out("ok") }
            "ai" -> app.scope.launch {
                // `ai warm`: what loading the model says, whatever the system reports about it (nothing is fetched by this).
                if (arg == "warm") { out(runCatching { com.google.mlkit.genai.prompt.Generation.getClient().let { c -> val t = android.os.SystemClock.uptimeMillis(); c.warmup(); "loaded in ${android.os.SystemClock.uptimeMillis() - t} ms, status ${c.checkStatus()}" } }.getOrElse { "${it.javaClass.simpleName}: ${it.message}" }); return@launch }
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
            // `pill N`: the next N frames in which the list's pill is on its way, one log line each (`pill t=8 up=0 lo=87 to=84..168`:
            // ms since it left its place, its upper and lower edge as drawn, its row's top and bottom, in px from the list's top). `pill 0` ends it.
            "pill" -> main.post {
                val n = (arg.toIntOrNull() ?: 60).coerceIn(0, 600)
                io.github.kuscher.booklight.overlay.PillTrace.start(n)
                out("tracing $n frames")
            }
            // `held down 8 50`: Down as a held key repeats it, eight times in all, one every 50 ms (Android's own repeat):
            // the pill is sent on while it is on its way, which `key down` after `key down` is too slow for.
            "held" -> main.post {
                val m = act?.model ?: return@post out("no panel")
                val words = arg.split(' ')
                val times = (words.getOrNull(1)?.toIntOrNull() ?: 8).coerceIn(1, 40)
                val every = (words.getOrNull(2)?.toLongOrNull() ?: 50L).coerceIn(8L, 1000L)
                repeat(times) { i -> main.postDelayed({ if (words[0] == "up") m.up(i > 0) else m.down(i > 0) }, every * i) }
                out("ok")
            }
            "close" -> main.post { act?.close(); out("ok") }
            "dump" -> main.post {
                val m = act?.model ?: return@post out("no panel")
                val d = act.window.decorView
                val loc = IntArray(2).also { d.getLocationOnScreen(it) }
                // (Under an app's chip: the action that is armed there after a colon, the keyword it was entered by after "via", and the line that offers its other action.)
                out("chip=${m.chip?.key}${m.act?.let { ":" + it.id } ?: ""}${m.chipVia?.let { " via=$it" } ?: ""}${m.otherAct?.let { " line=" + it.id } ?: ""} hint='${m.hint.orEmpty()}' query='${m.query}' ai=${app.onDevice.state.value}${if (m.thinking) " thinking" else ""} selected=${m.selected} armed=${when (m.onList) { io.github.kuscher.booklight.core.Behind.ARROW -> "more"; io.github.kuscher.booklight.core.Behind.WINDOW -> "window"; null -> m.chosen()?.second?.id }}${if (m.confirming) "?" else ""} opened=${m.opened}${m.list?.let { ":" + it.name.lowercase() } ?: ""} cell=${m.cell} flash=${m.flash} " +
                    "search=${m.lastSearchMicros}us window=${d.width}x${d.height}@${loc[0]},${loc[1]} blur=${act.windowManager.isCrossWindowBlurEnabled} completion=${m.completion} card=${m.card} zero=${if (m.zeroUp) m.results.count { it.kind != io.github.kuscher.booklight.core.Kind.ACTION } else 0} copy=${m.copy?.let { "${it.age}:${it.things.joinToString("+")}${if (it.looking) "…" else ""}" }} tip=${m.tip?.id}${if (m.tipArmed != 0) ":" + m.tipArmed else ""}${if (m.tipOff) " off" else ""} rows=" +
                    m.results.joinToString(" | ") { describe(it) })
            }
            // The Booklight window: its size in dp, its section, where the keys are. `window close` closes it.
            "window" -> main.post {
                val w = io.github.kuscher.booklight.window.MainActivity.current.get()?.takeIf { !it.isFinishing && !it.isDestroyed } ?: return@post out("no window")
                if (arg == "close") { w.finishAndRemoveTask(); return@post out("closed") }
                // `window hover KEY`, `window unhover KEY`: a row as it looks under the pointer.
                if (arg.startsWith("hover ") || arg.startsWith("unhover ") || arg.startsWith("press ") || arg.startsWith("release ")) return@post out(w.poke(arg))
                // `window helper`, `window helper close`: the system's Keyboard Shortcuts Helper over this window, and away again.
                if (arg == "helper") { w.requestShowKeyboardShortcuts(); return@post out("asked for the helper") }
                if (arg == "helper close") { w.dismissKeyboardShortcutsHelper(); return@post out("helper dismissed") }
                // `window film N EVERY`: N pictures of the window's own content, one every EVERY frames from now, each a third of
                // its size, side by side in `cache/film.png` (six in a row): a transition frame by frame, without a recording of the screen.
                if (arg.startsWith("film")) {
                    val words = arg.split(' ')
                    val n = (words.getOrNull(1)?.toIntOrNull() ?: 24).coerceIn(1, 72)
                    val every = (words.getOrNull(2)?.toIntOrNull() ?: 2).coerceIn(1, 12)
                    val v = w.window.decorView
                    val fw = v.width / 3; val fh = v.height / 3
                    val sheet = createBitmap(6 * fw, ((n + 5) / 6) * fh)
                    val canvas = android.graphics.Canvas(sheet)
                    val frames = android.view.Choreographer.getInstance()
                    var seen = 0; var taken = 0; var done = 0
                    frames.postFrameCallback(object : android.view.Choreographer.FrameCallback {
                        override fun doFrame(t: Long) {
                            if (seen++ % every == 0) {
                                val k = taken++
                                val bmp = createBitmap(fw, fh)
                                PixelCopy.request(w.window, bmp, { r ->
                                    if (r == PixelCopy.SUCCESS) canvas.drawBitmap(bmp, (k % 6) * fw.toFloat(), (k / 6) * fh.toFloat(), null)
                                    bmp.recycle()
                                    if (++done == n) {
                                        FileOutputStream(File(context.cacheDir, "film.png")).use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                        Log.i(BooklightApp.TAG, "film done ${fw}x$fh x $n")
                                    }
                                }, main)
                            }
                            if (taken < n) frames.postFrameCallback(this)
                        }
                    })
                    return@post out("filming ${fw}x$fh x $n")
                }
                // `window trace N`: the layout of the next N frames, one log line each (what is resized across a breakpoint, frame by frame).
                if (arg.startsWith("trace")) {
                    w.trace(arg.substringAfter(' ', "120").toIntOrNull() ?: 120) { lines -> lines.forEach { Log.i(BooklightApp.TAG, "trace $it") }; Log.i(BooklightApp.TAG, "trace end") }
                    return@post out("tracing")
                }
                val d = w.resources.displayMetrics.density
                val v = w.window.decorView
                out("${(v.width / d).toInt()}x${(v.height / d).toInt()}dp task=${w.taskId} focus=${w.hasWindowFocus()} ${w.probe()} keys-asked=${w.asked}")
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
            is io.github.kuscher.booklight.core.Body.Slots -> " {" + listOfNotNull(b.caption).plus(b.slots.map { "${it.label}=${it.value}${if (it.state == io.github.kuscher.booklight.core.SlotState.GUESSED) "?" else ""}" }).plus(listOfNotNull(b.note, b.tail, b.source?.let { "from $it" }, "struck".takeIf { b.struck })).joinToString("; ") + "}"
            is io.github.kuscher.booklight.core.Body.Level -> " {${b.percent}%${if (b.muted) " muted" else ""}${if (b.locked) " locked" else ""}${b.target?.let { " →$it" } ?: ""}}"
            is io.github.kuscher.booklight.core.Body.Grid -> " {${b.cells.size} cells: ${b.cells.take(6).joinToString("") { it.glyph }}…}"
            is io.github.kuscher.booklight.core.Body.Code -> " {qr}"
            is io.github.kuscher.booklight.core.Body.Mono -> " {${b.text}}"
            is io.github.kuscher.booklight.core.Body.Keys -> " {" + b.keys.joinToString(" + ") + "}"
            is io.github.kuscher.booklight.core.Body.Task -> if (b.done) " {done}" else " {open}"
            is io.github.kuscher.booklight.core.Body.Stream -> " {${b.caption}${if (b.answer) " =" else ":"} ${b.text}${if (b.busy) "…" else ""}${if (b.tall) " tall" else ""}${if (b.ask != null && !b.answer) " ?" else ""}}"
        }
        // (A line behind the arrow is marked +, one behind Window +w.)
        val acts = r.actions.mapIndexed { i, a -> (if (i == r.armed) "*" else "") + a.id + (if (!a.more) "" else if (a.behind == io.github.kuscher.booklight.core.Behind.WINDOW) "+w" else "+") + (if (a.off) "(off)" else "") }.joinToString(",")
        return "${r.answer ?: r.title}${r.subtitle?.let { " ($it)" } ?: ""} [${r.label ?: r.kind}]$body <$acts>"
    }
}
