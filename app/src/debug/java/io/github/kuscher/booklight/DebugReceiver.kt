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
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.data.asUpdate
import io.github.kuscher.booklight.data.firstRun
import io.github.kuscher.booklight.data.withFirstRun
import io.github.kuscher.booklight.overlay.OverlayActivity
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Test hooks for driving Booklight from adb (`./bl debug …`). Debug builds only. The receiver requires
 * the DUMP permission, which only the shell (adb) holds, so other apps can't use it.
 *   keys TEXT (typed one character at a time, as a person does: every list in between is made) |
 *   ping | dump | type TEXT | key up|down|left|right|tab|backtab|esc|enter|enter2|stay|back|more|window | close | shot [NAME]
 *   find TEXT (ranked results without the panel; "KEY: TEXT" searches inside a scope) | apps | forget
 *   pref suggestions on|off | pref engine ID | pref glass clear|balanced|frosted|solid | pref opening off|fast|medium|slow
 *   ai none|downloadable|downloading|ready|real (what the rows of a prompt show on a device in that state; `real` asks the device again)
 *   pref theme auto|light|dark | pref tint on|off | pref dim on|off
 *   activity PKG/CLASS (is that activity open to other apps: why an app's shortcut is or is not offered)
 *   think on|off (the light of the model at work, without the model) | turn MS (close, and the key again MS later: says "turned",
 *   or "not turned" where the panel had already gone) | turn MS enter (and Enter in the same moment: what it answered on first run's stage)
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
 *   flight show NAME (a sample flight's row in the open panel, in the phase its name says, looked at when the name says: friday, soon, air,
 *   air-late, landed, cancelled, looking, offline… and the saved replies, LH455-in-the-air…; the service is not asked) | flight show (the names) |
 *   flight show off (ends it) | flight pin NAME (the same flight in the pinned window); dump says a flight's row as phase, headline, badge, share, ends
 *   appsearch (a search inside an app: every line of the bundled table and every search an app declares, with the app, the source
 *   and whether this device takes it; a star marks the one in use. Run it on a Googlebook before a release)
 *   players (the music apps `play` offers, and the one asked last; `find play TEXT` shows the row and which is armed)
 *   pref spotifykey ID SECRET|none (the user's key for Spotify; never printed) | song TEXT (what Spotify has for the text after `play`,
 *   looked up at once: what was found and the address that would be sent)
 *   first (first run's stored state, shown) | first new|update|off | first replay [show] | first resume|key|helper|opened|shown|older |
 *   first answer NAME | first ran open|search|sum | first at SCREEN (core `FirstRun`: only first run's own fields, but for `key` and `answer agree`)
 *   first welcome [at MS] (first run's welcome in the open panel; with `at`, standing still at that ms of the paper's clock, motion.md §2.1) |
 *   first show [at apps|row|stop|sum|answer|flight|grid|cell] (its show; with `at`, up to that moment at once, and then it stands) | first land
 *   While the piece plays, `key …` is taken by it first, as the keys are; dump says `playing=`, `cast=ready|none`, `laps=`, `gliding`.
 *   first again (as the window's "First steps" row writes it: the next plain panel plays the whole of it, or goes on with an unfinished run) |
 *   first stage at MS (the stage's clock stands at that ms after the screen came: a set-down as a still) | first stage (it runs again) |
 *   first stage as whole|set_down|back|turn|after_list|lands [at MS] (the screen that stands comes again that way, now) |
 *   first press [under] [held [TIMES] [MS]] (the user's key lands on the screen that waits for it, in view or as under the system's dialog,
 *   and the system's dialog is asked to go, as the real landing asks it; with `held`, TIMES more starts by the key follow MS apart, as a
 *   held key's repeats come, 8 and 50 unless said: each is a real start of the panel's own activity again, marked as the key's) |
 *   first says (what first run told a screen reader in this panel, and what every node of the panel says to one) |
 *   first trace N (the next N frames, 1 to 2400, 120 unless said: ms, the window's height, the stage's clock, whether it takes a key, the
 *   highlight on its way, the key on the glass, laps, the hold; logged, and kept here: every later hook clears the log) | first trace show [all] [PAGE] (the frames that were kept: how many,
 *   or one page of them; without `all`, only the frames in which something changed; `./bl trace` prints every page) |
 *   pref sums on|off ("Show sums": for a run asked for again where they are off); dump says `comes=`, `lead=`, `all=` (where the last answer comes
 *   later than the armed one), `end=`, `enter` (Enter on the armed answer counts) or `seen` (every key and click does) or `covered` (the screen came
 *   under the system's dialog and is still under it: no key counts until a moment after the dialog has gone), `later`, `keydown`, `landing`.
 *   While a lesson of first run stands, `key enter` on an app's Open and on a search inside an app is practice, as the key is: nothing opens.
 *   `key enter2` is Enter twice in one turn, as a quick double press; dump says `due=`, `coach=`, `pressed`, `choices`, `ended`.
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
                if (v.isNotEmpty()) when (k) {
                    "suggestions" -> app.prefs.update { it.copy(suggestions = v == "on", suggestionsCard = false) }
                    "engine" -> app.prefs.update { it.copy(engine = v) }
                    "glass" -> app.prefs.update { it.copy(glass = v) }
                    "opening" -> app.prefs.update { it.copy(opening = v) }
                    "theme" -> app.prefs.update { it.copy(theme = v) }
                    "tint" -> app.prefs.update { it.copy(tint = v == "on") }
                    "zero" -> app.prefs.update { it.copy(zero = v == "on") }
                    "sums" -> app.prefs.update { it.copy(showSums = v == "on") }
                    "dim" -> app.prefs.update { it.copy(dim = v == "on") }
                    // tips on|off|again (from the first one)|at ID (that tip next)
                    "tips" -> app.prefs.update { s -> when (v) {
                        "on" -> s.copy(tips = true); "off" -> s.copy(tips = false)
                        "again" -> s.copy(tips = true, tipsSeen = FirstRun.keepMark(s.tipsSeen), tipId = "", tipMs = 0, used = emptyList())
                        else -> s
                    } }
                    "shadow" -> app.prefs.update { it.copy(shadow = v) }
                    "key" -> app.prefs.update { it.copy(keySeen = v == "seen") }
                    // The user's key for the flight service: `pref flightkey KEY`, `pref flightkey none`. It is never printed.
                    "flightkey" -> app.prefs.setFlightKey(if (v == "none") "" else v)
                    // The user's key for Spotify: `pref spotifykey ID SECRET`, `pref spotifykey none`. It is never printed.
                    "spotifykey" -> arg.split(' ').let { w -> if (v == "none") app.prefs.setSpotifyKey("", "") else app.prefs.setSpotifyKey(v, w.getOrElse(2) { "" }) }
                }
                out(app.prefs.now.let { "engine=${it.engine} suggestions=${it.suggestions} glass=${it.glass} opening=${it.opening} theme=${it.theme} tint=${it.tint} dim=${it.dim} shadow=${it.shadow} tips=${it.tips} copy=${it.copyRow} zero=${it.zero} sums=${it.showSums} hidden=${it.zeroHidden.size} flightkey=${if (app.prefs.flightKey.value.isEmpty()) "none" else "set"} spotifykey=${if (app.prefs.spotifyKey.value.isEmpty()) "none" else "set"}" })
            }
            "find" -> app.scope.launch {
                val t0 = System.nanoTime()
                val scoped = app.engine.scopeFor(arg)
                val r = if (scoped != null) app.engine.search(Query(scoped.text, scoped.scope.key, scoped.word)) else app.engine.search(Query(arg))
                out("${(System.nanoTime() - t0) / 1000} us | " + r.joinToString(" | ") { describe(it) })
            }
            // `flight show NAME`: a sample flight's row in the open panel, in the phase its name says (`FlightSamples`: `friday`, `soon`, `air`, `air-late`,
            // `landed`, `cancelled`, `looking`, `offline`…, and the saved replies by their names, `LH455-in-the-air`). Its number is typed for it, the row
            // stands waiting and the sample lands in it as an answer does; for that row the clock is the sample's, and runs on from there. The service is
            // not asked, with a key in or without. `flight show` alone lists the names; `flight show off` ends it (so does any other text typed).
            // `flight pin NAME`: the same flight in the pinned window, without the panel.
            "flight" -> if (arg == "show" || arg.startsWith("show ") || arg.startsWith("pin ")) {
                val (what, name) = (arg.split(' ').filter { it.isNotEmpty() } + "").let { it[0] to it[1] }
                if (name == "off") { app.flights.unstage(); return out("off") }
                val sample = FlightSamples.of(context, name) ?: return out(FlightSamples.names(context))
                fun stage() = app.flights.stage(sample.number, "sample $name", sample.flight, sample.failure, sample.now)
                if (what == "pin") app.scope.launch {
                    if (sample.flight == null) return@launch out("nothing to pin: $name has no flight")
                    stage()
                    val row = app.flights.rows(sample.number).firstOrNull()?.let { app.flights.answer(it.id, pause = false) }
                    val pin = row?.actions?.firstOrNull { it.id == "pin" && !it.off }?.effect as? io.github.kuscher.booklight.core.Effect.Pin
                    main.post { out(if (pin != null) "${app.executor.run(pin)} ${describe(row)}" else "nothing to pin: ${row?.let(::describe)}") }
                } else main.post {
                    val m = act?.model ?: return@post out("no panel")
                    // The field is emptied first (a list still being made for what was in it would end the sample), then the sample's number is typed.
                    while (m.leaveScope()) {}
                    m.type("")
                    stage() ?: return@post out("not a flight: ${sample.number}")
                    m.type(sample.number)
                    out("$name as ${sample.number}: the row waits, then the sample lands in it (`dump` says it; `flight show off` ends it)")
                }
            }
            // A flight's row without the panel, looked up at once: `flight LH455`, `flight lh455 fri`, `flight ps5`. With a key in, this asks the service.
            else app.scope.launch {
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
                // First run's opening piece takes a key first, as the panel's keys do (core `FirstRun.pressed`): Enter is the cue in
                // the welcome; any other of these sets the key's step down. Nothing of the piece is run.
                if (m.playing != null && m.press(if (arg.startsWith("enter") || arg == "stay") FirstRun.Press.ENTER else FirstRun.Press.OTHER)) {
                    // (Twice in one turn, as a quick double press: the second one finds the piece where the first one left it.)
                    if (arg == "enter2") m.press(FirstRun.Press.ENTER)
                    return@post out("the opening piece took it: playing=${m.playing?.name?.lowercase() ?: "none"}")
                }
                when (arg) {
                    "down" -> m.down(false)
                    "up" -> m.up(false)
                    // As the keys do (`Panel.keys`): Tab and Right only move on a row that offers more than one thing; Right again on Window or on the arrow opens its list.
                    "right" -> if (!m.moveCell(1, 0) && !m.nudge(1)) { if (m.tabEnters) m.fill() else if (m.opened == null) { if (m.onList != null) m.open() else m.arm(1, wrap = false) } }
                    "left" -> if (m.opened != null) m.close() else if (!m.moveCell(-1, 0) && !m.nudge(-1)) m.arm(-1, wrap = false)
                    "tab" -> if (m.stage != null) m.stageTab(false) else if (m.tip != null) m.tipTab() else if (m.atRest && m.chip == null && m.query.isBlank()) m.down(false) else if (m.copy != null || m.bare) m.tabCopy() else if (m.keyword != null) m.enterKeyword() else if (m.tabEnters) m.fill() else if (m.opened != null) m.step(1) else if (m.otherAct != null) m.swap() else m.arm(1, wrap = true)
                    "backtab" -> if (m.stage != null) m.stageTab(true) else if (m.opened != null) m.step(-1) else if (m.otherAct != null) m.swap() else m.arm(-1, wrap = true)
                    // Straight to one of the row's two list stops: `key more` the arrow, `key window` Window. Enter (or `key right`) then opens it.
                    "more" -> m.armList(io.github.kuscher.booklight.core.Behind.ARROW)
                    "window" -> m.armList(io.github.kuscher.booklight.core.Behind.WINDOW)
                    "back" -> m.back()
                    "esc" -> if (!m.cancelConfirm() && !m.leaveAnswer()) act.close()
                    "enter" -> if (m.stage != null) m.stageEnter()?.let { act.stage(it) } else if (m.tip != null) m.tipEnter() else m.enter { r, a -> act.run(r, a) }
                    // Enter twice in one turn, as a quick double press does: after a lesson's Enter the second one must do nothing.
                    "enter2" -> repeat(2) { if (m.stage != null) m.stageEnter()?.let { act.stage(it) } else if (m.tip != null) m.tipEnter() else m.enter { r, a -> act.run(r, a) } }
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
            // The panel is put away and the key comes again MS later: the turn, at a moment adb could never hit. `turn MS enter`: and
            // Enter in that same moment, as a hand does that pressed it with the key; what it answered on the stage is said (nothing,
            // if a screen takes a key only once it has been in view).
            // It says whether the panel was turned at all: one that had already gone (the system's animations are off, MS is longer
            // than its fold) is not, and an Enter that answers nothing there would prove nothing. A turn is told by the model:
            // what stands under the field came in that moment.
            "turn" -> main.post {
                val words = arg.split(' ').filter { it.isNotEmpty() }
                val ms = (if (words.isEmpty()) 60L else words[0].toLongOrNull() ?: return@post out("turn [MS] | turn MS enter (MS comes first: how long after the close the key comes again)")).coerceAtLeast(1L)
                val enter = words.getOrNull(1) == "enter"
                val a = act ?: return@post out("no panel")
                a.close()
                main.postDelayed({
                    val before = a.model.cameAt
                    a.turn(Intent())
                    if (a.model.cameAt == before) out("not turned: the panel had already gone $ms ms after its close (the system's animations off, or MS longer than its fold)")
                    else if (enter) out("turned; Enter in the same moment answered ${a.model.stageEnter()?.also { a.stage(it) } ?: "nothing"}")
                    else out("turned")
                }, ms)
            }
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
                out("chip=${m.chip?.key}${m.act?.let { ":" + it.id } ?: ""}${m.chipVia?.let { " via=$it" } ?: ""}${m.otherAct?.let { " line=" + it.id } ?: ""} hint='${m.hint ?: m.firstHint.orEmpty()}' query='${m.query}' ai=${app.onDevice.state.value}${if (m.thinking) " thinking" else ""} selected=${m.selected} armed=${when (m.onList) { io.github.kuscher.booklight.core.Behind.ARROW -> "more"; io.github.kuscher.booklight.core.Behind.WINDOW -> "window"; null -> m.chosen()?.second?.id }}${if (m.confirming) "?" else ""} opened=${m.opened}${m.list?.let { ":" + it.name.lowercase() } ?: ""} cell=${m.cell} flash=${m.flash} " +
                    "search=${m.lastSearchMicros}us window=${d.width}x${d.height}@${loc[0]},${loc[1]} blur=${act.windowManager.isCrossWindowBlurEnabled} completion=${m.completion} first=${m.stage?.name ?: "none"}${if (m.stage != null) " armed=" + m.stageArmed else ""}${FirstRun.count(app.prefs.now.firstRun())?.let { " ${it.first}/${it.second}" } ?: ""} playing=${m.playing?.name?.lowercase() ?: "none"}${if (m.began) " cast=" + (if (m.cast != null) "ready" else "none") + " laps=" + m.laps else if (m.laps > 0) " laps=" + m.laps else ""}${if (m.gliding) " gliding" else ""} due=${m.due?.name ?: "none"}${if (m.stage != null) " comes=" + m.comes.name.lowercase() + " lead=" + m.marks.lead + (if (m.marks.all != m.marks.lead) " all=" + m.marks.all else "") + " end=" + m.marks.end + (if (m.seen) " seen" else if (m.takesEnter) " enter" else if (m.underDialog) " covered" else "") else ""}${if (m.later) " later" else ""}${if (m.keyDown) " keydown" else ""}${if (m.landing) " landing" else ""}${m.coach?.let { " coach=" + it.name.lowercase() } ?: ""}${if (m.pressed) " pressed" else ""}${if (m.choicesUp) " choices" else ""}${if (m.ended) " ended" else ""} zero=${if (m.zeroUp) m.results.count { it.kind != io.github.kuscher.booklight.core.Kind.ACTION } else 0} copy=${m.copy?.let { "${it.age}:${it.things.joinToString("+")}${if (it.looking) "…" else ""}" }} tip=${m.tip?.id}${if (m.tipArmed != 0) ":" + m.tipArmed else ""}${if (m.tipOff) " off" else ""} rows=" +
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
            // First run's stored state, shown and set (core `FirstRun`); `first` alone only shows it. It changes first run's own fields and nothing
            // else of the settings, with two exceptions that are the product's own steps: `key` (a key has opened the panel: `keySeen`) and
            // `answer agree` (search suggestions on, as Agree on the question does).
            "first" -> {
                val words = arg.split(' ').filter { it.isNotEmpty() }
                // Each change is worked out inside the settings update (`Prefs.firstRun`), never from a snapshot read before it.
                when (words.getOrNull(0)) {
                    null -> {}
                    // The stored state of a new installation's run, of the run of one that was there before, of none. (A run that begins has
                    // not marked step 1 as over; `off` leaves the mark.) Whether a key is known is another hook's: `pref key seen|no`.
                    "new", "update", "off" -> app.prefs.update { s ->
                        val run = when (words[0]) { "new" -> FirstRun.Run.NEW; "update" -> FirstRun.Run.UPDATE; else -> FirstRun.Run.NONE }
                        val t = if (run == FirstRun.Run.NONE) s else s.copy(tipsSeen = s.tipsSeen - FirstRun.MARK)
                        t.withFirstRun(t.firstRun().copy(run = run, done = emptyList(), opens = 0, helper = 0, icon = false))
                    }
                    "replay" -> app.prefs.firstRun { FirstRun.replay(it, overture = words.getOrNull(1) == "show") }
                    "resume" -> app.prefs.firstRun { FirstRun.resume(it) }
                    "key" -> app.prefs.firstRun { FirstRun.keyLanded(it) }
                    "helper" -> app.prefs.firstRun { FirstRun.helperCame(it) }
                    "opened" -> app.prefs.firstRun { FirstRun.opened(it) }
                    "shown" -> app.prefs.firstRun { FirstRun.shown(it) }
                    // As the Booklight window's "First steps" row writes it: a finished run is asked for again with its opening piece,
                    // an unfinished one goes on where it stopped. The next plain panel (`./bl open stay`) then begins as the row's does.
                    "again" -> app.prefs.firstRun { FirstRun.again(it, overture = true) }
                    // The stage's own clock (docs/design/first-run/motion.md §3): `stage at MS` stands it still at that ms after the
                    // screen came, `stage` lets it run again, `stage as HOW [at MS]` has the screen that stands come again in that way
                    // (whole, set_down, back, turn, after_list, lands), in this moment. Nothing that is kept changes.
                    "stage" -> {
                        val m = act?.model ?: return out("no panel")
                        val how = io.github.kuscher.booklight.core.SetDown.Comes.entries.firstOrNull { words.getOrNull(1) == "as" && it.name.equals(words.getOrNull(2), ignoreCase = true) }
                        if (words.getOrNull(1) == "as" && how == null) return out(io.github.kuscher.booklight.core.SetDown.Comes.entries.joinToString(" ") { it.name.lowercase() })
                        val at = words.indexOf("at").takeIf { it >= 0 }?.let { words.getOrNull(it + 1)?.toFloatOrNull() ?: return out("stage at MS | stage | stage as HOW [at MS]") }
                        // (A word that is neither `as` nor `at` is a slip: `stage 400` must not be taken for "let it run".)
                        if (words.size > 1 && words[1] != "as" && words[1] != "at") return out("stage at MS | stage | stage as HOW [at MS]")
                        main.post { m.stageAs(how, at) }
                        return out("stage=${m.stage?.name ?: "none"}${how?.let { " comes as " + it.name.lowercase() } ?: ""}${at?.let { " stands at $it" } ?: " runs"}")
                    }
                    // The user's key lands on the screen that waits for it, in the open panel: in view, or (`press under`) as it does
                    // under the system's dialog, where "your key" is held down until the dialog has gone. The system's dialog is
                    // asked to go, as the activity's own landing asks it (`OverlayActivity.landed`: the system closes it by itself
                    // when a new key is pressed, which no hook can press): where the dialog is up, it goes, the panel has the
                    // focus again and its hold is over, as after a real landing. Where none is up, nothing comes of that.
                    // `press [under] held [TIMES] [MS]`: and the key stays down: TIMES more starts of the panel by the key follow, MS
                    // apart (8 and 50 unless said), as a held key's repeats come. Each starts the panel's own activity again, for
                    // real, marked as the key's (`./bl open key` is one too): what the panel makes of them is the activity's to say.
                    "press" -> {
                        val a = act ?: return out("no panel")
                        val m = a.model
                        if (!m.awaitsKey) return out("no screen waits for the key here: `first at k2` first")
                        val under = words.getOrNull(1) == "under"
                        val held = words.indexOf("held").takeIf { it >= 0 }
                        val times = held?.let { (words.getOrNull(it + 1)?.toIntOrNull() ?: 8).coerceIn(1, 40) } ?: 0
                        val every = held?.let { (words.getOrNull(it + 2)?.toLongOrNull() ?: 50L).coerceIn(8L, 1000L) } ?: 0L
                        main.post {
                            m.keyLanded(under = under)
                            a.dismissKeyboardShortcutsHelper()
                            repeat(times) { i -> main.postDelayed({ if (!a.isFinishing) a.startActivity(Intent(a, OverlayActivity::class.java).putExtra("key", true)) }, every * (i + 1)) }
                        }
                        return out("the key lands${if (under) ", as under the dialog" else ""}; the system's dialog is asked to go${if (times > 0) "; $times more starts by the key follow, $every ms apart" else ""}")
                    }
                    // What first run told a screen reader in this panel (the last sentences, as they were said), and what the panel
                    // says to one now: every node of its semantics, merged as a reader meets them, a line each. Rows are among them:
                    // they name apps of the device, and are not for any file of this repo.
                    "says" -> {
                        val a = act ?: return out("no panel")
                        main.post {
                            val lines = ArrayList<String>()
                            fun walk(v: android.view.View) {
                                (v as? androidx.compose.ui.node.RootForTest)?.let { root -> root.semanticsOwner.getAllSemanticsNodes(mergingEnabled = true).mapNotNullTo(lines, ::spoken) }
                                (v as? android.view.ViewGroup)?.let { g -> for (i in 0 until g.childCount) walk(g.getChildAt(i)) }
                            }
                            walk(a.window.decorView)
                            a.model.said.forEach { Log.i(BooklightApp.TAG, "says told: $it") }
                            lines.forEach { Log.i(BooklightApp.TAG, "says node: $it") }
                            out(("told=" + a.model.said.joinToString(" | ").ifEmpty { "nothing" } + " || nodes=" + lines.joinToString(" | ")).let { if (it.length <= 3500) it else it.take(3500) + " … (./bl logs has every line)" })
                        }
                        return
                    }
                    // `trace N`: the next N frames, a line each: ms since the first of them, the stage's clock in ms since its screen
                    // came into view, the window's height in px, what plays and what stands, how the screen came, from when Enter
                    // and from when every key counts and whether they do yet, whether the show's highlight is on its way, the key
                    // on the glass, the laps asked for, a height that is held, the hold behind the system's dialog (a screen that
                    // came under it counts from the line where `hold=under` ends), the rows. For times a film cannot give. Each line is logged, and kept here as well: `bl`'s `dbg`
                    // clears the log before every hook, so a check that gives a key by a hook after starting a trace would lose
                    // its frames. `trace show` says how many were kept; `trace show PAGE` prints one page of those in which
                    // something changed besides the two clocks (and the first and the last), `trace show all PAGE` one page of
                    // them all, each page as much as one log line holds. `./bl trace [all]` asks for every page.
                    "trace" -> {
                        if (words.getOrNull(1) == "show") {
                            val all = words.getOrNull(2) == "all"
                            val lines = if (all) traced.map { it.first + it.second } else traced.filterIndexed { i, f -> i == 0 || i == traced.lastIndex || f.second != traced[i - 1].second }.map { it.first + it.second }
                            val pages = ArrayList<String>()
                            val page = StringBuilder()
                            for (line in lines) {
                                if (page.isNotEmpty() && page.length + line.length + 1 > PAGE) { pages += page.toString(); page.setLength(0) }
                                if (page.isNotEmpty()) page.append(';')
                                page.append(line)
                            }
                            if (page.isNotEmpty()) pages += page.toString()
                            val p = words.getOrNull(if (all) 3 else 2)?.toIntOrNull()
                                ?: return out("${traced.size} frames kept${if (tracing) " so far" else ""}; ${lines.size} lines in ${pages.size} pages${if (all) "" else " where something changed"} (./bl trace${if (all) " all" else ""} prints them)")
                            return out(pages.getOrNull(p - 1)?.let { "page $p/${pages.size}: $it" } ?: "no page $p: ${pages.size} pages")
                        }
                        val a = act ?: return out("no panel")
                        val n = (words.getOrNull(1)?.toIntOrNull() ?: 120).coerceIn(1, 2400)
                        val frames = android.view.Choreographer.getInstance()
                        var left = n
                        var first = 0L
                        // (A trace asked for while one runs takes its place: the one before stops at its next frame.)
                        val mine = ++traces
                        traced.clear()
                        tracing = true
                        main.post {
                            frames.postFrameCallback(object : android.view.Choreographer.FrameCallback {
                                override fun doFrame(t: Long) {
                                    if (mine != traces) return
                                    val m = a.model
                                    if (first == 0L) first = t
                                    val clocks = "t=${(t - first) / 1_000_000} clock=${t / 1_000_000 - m.viewFrom} "
                                    val rest = "h=${a.window.decorView.height} playing=${m.playing?.name?.lowercase() ?: "none"} stage=${m.stage?.name ?: "none"} comes=${m.comes.name.lowercase()} " +
                                        "lead=${m.marks.lead} all=${m.marks.all} enter=${m.takesEnter} seen=${m.seen} glide=${m.gliding} keydown=${m.keyDown} checked=${m.keyChecked} laps=${m.laps} held=${m.heldHeight?.value?.toInt() ?: "-"} hold=${a.held.lowercase()} rows=${m.results.size}"
                                    traced += clocks to rest
                                    Log.i(BooklightApp.TAG, "first $clocks$rest")
                                    if (--left > 0 && !a.isFinishing) frames.postFrameCallback(this) else tracing = false
                                }
                            })
                        }
                        return out("tracing $n frames (./bl trace prints them, whatever is asked meanwhile)")
                    }
                    // The opening piece in the open panel, whatever the stored state is: `welcome` plays it from the gate, `welcome at MS`
                    // stands it still at that moment of the paper's own clock (motion.md §2.1), `show` skips the welcome, `show at NAME`
                    // performs the show up to a named moment at once and stands, `land` sets the key's step down. Nothing of first
                    // run's state is written by them but what playing the piece writes: that it was shown, where a run stands.
                    "welcome", "show", "land" -> {
                        val m = act?.model ?: return out("no panel")
                        val at = words.getOrNull(2).takeIf { words.getOrNull(1) == "at" }
                        val still = io.github.kuscher.booklight.core.Show.Still.entries.firstOrNull { it.name.equals(at, ignoreCase = true) }
                        if (words[0] == "show" && at != null && still == null) return out(io.github.kuscher.booklight.core.Show.Still.entries.joinToString(" ") { it.name.lowercase() })
                        if (words[0] == "welcome" && words.size > 1 && at?.toFloatOrNull() == null) return out("welcome | welcome at MS")
                        // (The landing is over in a third of a second, sooner than a second hook can ask: it is said in the same turn.)
                        if (words[0] == "land") {
                            main.post {
                                m.land()
                                out("landing: playing=${m.playing?.name?.lowercase() ?: "none"} first=${m.stage ?: "none"}${if (m.gliding) " gliding" else ""} height=${io.github.kuscher.booklight.overlay.Metrics.height(m).value.toInt()}dp")
                            }
                            return
                        }
                        main.post {
                            // The field is emptied first, as for any hook that types: the piece plays over the empty field.
                            while (m.leaveScope()) {}
                            m.type("")
                            m.begin(if (words[0] == "welcome") at?.toFloatOrNull() else null)
                            if (words[0] == "show") m.show(still)
                        }
                        return out("${words[0]}${at?.let { " stands at $it" } ?: " plays"} (`dump` says where it is; a key ends it)")
                    }
                    "answer" -> {
                        val answer = FirstRun.Answer.entries.firstOrNull { it.name.equals(words.getOrNull(1), ignoreCase = true) }
                            ?: return out(FirstRun.Answer.entries.joinToString(" ") { it.name.lowercase() })
                        app.prefs.firstRun { FirstRun.answer(it, answer) }
                    }
                    "ran" -> {
                        val enter = when (words.getOrNull(1)) {
                            "open" -> FirstRun.Enter.PRACTICE_OPEN; "search" -> FirstRun.Enter.PRACTICE_SEARCH; "sum" -> FirstRun.Enter.COPY_STAYS
                            else -> return out("open search sum")
                        }
                        app.prefs.firstRun { FirstRun.ran(it, enter) }
                    }
                    "at" -> {
                        val screen = FirstRun.Screen.entries.firstOrNull { it.name.equals(words.getOrNull(1), ignoreCase = true) }
                            ?: return out(FirstRun.Screen.entries.joinToString(" ") { it.name.lowercase() })
                        FirstRun.at(app.prefs.now.firstRun(), screen) ?: return out("${screen.name} cannot stand: K4 needs a key (`first key`), L4 needs sums on (`pref sums on`), Q needs suggestions off")
                        app.prefs.firstRun { FirstRun.at(it, screen) ?: it }
                    }
                    // As if an older build had written the settings and this one read them again: what that build does not know is gone, and the
                    // migration runs anew.
                    "older" -> app.prefs.update { it.asUpdate() }
                    else -> return out("new | update | off | replay [show] | again | resume | key | helper | opened | shown | answer NAME | ran open|search|sum | at SCREEN | older | welcome [at MS] | show [at NAME] | land | stage [as HOW] [at MS] | press [under] [held [TIMES] [MS]] | says | trace N | trace show [all] [PAGE]")
                }
                // An open panel follows at once: the screen that now stands is drawn, without a new opening. (`first` alone only shows.)
                main.post { if (words.isNotEmpty()) act?.model?.firstChanged(); io.github.kuscher.booklight.device.SystemWords.load(context, app.scope) }
                val f = app.prefs.now.firstRun()
                val dp = context.getSystemService(android.view.WindowManager::class.java).maximumWindowMetrics.bounds.height() / context.resources.displayMetrics.density
                val motion = io.github.kuscher.booklight.overlay.Motion.of(context).on
                val reader = context.getSystemService(android.view.accessibility.AccessibilityManager::class.java).isTouchExplorationEnabled
                val overture = FirstRun.overture(f, motion, reader, plain = true, screenDp = dp)
                val boards = io.github.kuscher.booklight.device.Keyboards.attached()
                val key = FirstRun.suggest(FirstRun.hasQuickInsert(boards, null))
                out("run=${f.run} done=${f.done} opens=${f.opens} helper=${f.helper} icon=${f.icon} key=${f.key} suggestions=${f.suggestions} mark=${f.mark} | " +
                    "parked=${FirstRun.parked(f)} screen=${FirstRun.screen(f)} count=${FirstRun.count(f)?.let { "${it.first}/${it.second}" }} | " +
                    "overture=$overture slow=${FirstRun.slow(f, overture)} greets=${FirstRun.greets(f, motion, reader, plain = true, screenDp = dp)} fits=${FirstRun.fits(dp)} (${dp.toInt()} dp high, animations=$motion, reader=$reader) | " +
                    "suggested=$key other=${FirstRun.other(key)} keyboards=${boards.joinToString(",") { (if (it.external) "external" else "own") + (if (it.quickInsert) "+quick" else "") }.ifEmpty { "none" }} | " +
                    "words=${io.github.kuscher.booklight.device.SystemWords.read ?: "not read"} | hold=${act?.held ?: "no panel"} | example=${act?.model?.example ?: "none (no panel, or not worked out)"} | last start: ${OverlayActivity.lastStart.ifEmpty { "none in this process" }}")
            }
            else -> out("unknown")
        }
    }

    companion object {
        /**
         * The frames the last `first trace N` took, each as its two clocks and the rest of its line. Kept by the receiver
         * itself, for as long as the process lives: the log is cleared before every hook (`bl`'s `dbg`), so a check that
         * reads the log after another hook has lost them.
         */
        private val traced = ArrayList<Pair<String, String>>()
        /** Which trace is the one that runs (a new one takes the place of the one before), and whether it still takes frames. */
        private var traces = 0
        private var tracing = false
        /** As much of a trace as one log line holds. */
        private const val PAGE = 3400
    }

    /**
     * One node of the panel as a screen reader meets it: its role, what it says (a description, else its text), its state,
     * whether it is announced when it changes, and what it offers besides a click. Null for a node that says nothing.
     */
    private fun spoken(n: androidx.compose.ui.semantics.SemanticsNode): String? {
        val c = n.config
        val words = c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)?.joinToString(" ")
            ?: c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.joinToString(" ") { it.text }
            ?: c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.EditableText)?.text
        val role = c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Role)?.toString()?.lowercase()
        if (words.isNullOrBlank() && role == null) return null
        return listOfNotNull(
            role, words?.let { "'$it'" },
            c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ToggleableState)?.name?.lowercase(),
            "selected".takeIf { c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Selected) == true },
            "live".takeIf { c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.LiveRegion) != null },
            c.getOrNull(androidx.compose.ui.semantics.SemanticsActions.CustomActions)?.joinToString(",", "+[", "]") { it.label },
        ).joinToString(" ")
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
            is io.github.kuscher.booklight.core.Body.Switch -> if (b.on) " {on}" else " {off}"
            is io.github.kuscher.booklight.core.Body.Stream -> " {${b.caption}${if (b.answer) " =" else ":"} ${b.text}${if (b.busy) "…" else ""}${if (b.tall) " tall" else ""}${if (b.ask != null && !b.answer) " ?" else ""}}"
            // A flight's row: its phase, line one, the headline, the badge and its tone, the plane's place (none: no plane), and what stands under each end
            // of the line (a struck time has a ~ after it; the small words in brackets, and after a bar what is left of them where the ends would meet).
            is io.github.kuscher.booklight.core.Body.Flight -> {
                fun words(s: io.github.kuscher.booklight.core.Stop) = s.words?.let { "[$it${s.brief?.let { b -> " | $b" } ?: ""}]" }
                fun time(s: io.github.kuscher.booklight.core.Stop) = s.time + if (s.struck) "~" else ""
                " {" + listOfNotNull(
                    "phase=" + (b.phase?.name?.lowercase() ?: if (b.headline.isEmpty()) "empty" else if (b.answer == 0L) "looking" else "no-answer"), b.caption, "headline=${b.headline}",
                    "badge=" + (b.badge?.let { "$it (${b.tone.name.lowercase()})" } ?: "none"), "share=" + (b.share?.let { "%.2f".format(java.util.Locale.ROOT, it) } ?: "none"),
                    "from=" + (b.from?.let { listOfNotNull(it.code, time(it), words(it)).joinToString(" ") } ?: "-"),
                    "to=" + (b.to?.let { listOfNotNull(words(it), time(it), it.code).joinToString(" ") } ?: "-"),
                    b.source?.let { "from $it" }, "counts".takeIf { b.counts },
                ).joinToString("; ") + "}"
            }
        }
        // (A line behind the arrow is marked +, one behind Window +w.)
        val acts = r.actions.mapIndexed { i, a -> (if (i == r.armed) "*" else "") + a.id + (if (!a.more) "" else if (a.behind == io.github.kuscher.booklight.core.Behind.WINDOW) "+w" else "+") + (if (a.off) "(off)" else "") }.joinToString(",")
        return "${r.answer ?: r.title}${r.subtitle?.let { " ($it)" } ?: ""} [${r.label ?: r.kind}]$body <$acts>"
    }
}
