package io.github.kuscher.booklight.scopes

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import androidx.core.net.toUri
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Reach
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.Slot
import io.github.kuscher.booklight.core.SlotState
import io.github.kuscher.booklight.core.Travel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Who takes what on this device: whether a link has its app here, and whether a request that names
 * no app has one to answer it. The system is asked once for each and the answer is kept (a keyword's
 * scope is asked on every keystroke), and all are asked again when an app comes, goes or changes.
 *
 * The package manager is never asked on the main thread: the panel's first frame and every keystroke
 * come through here. A question that is put there for the first time goes to a background thread, and
 * until its answer is in the app counts as not there. [Scopes] puts the questions when the process
 * starts, so the answers are in before anything is typed.
 *
 * It sees what Booklight sees: the apps with an icon in the app list (the manifest's `<queries>`).
 * An app without one counts as not there.
 */
class Takers(context: Context) {
    private val app = context.applicationContext as BooklightApp
    private val pm = context.packageManager
    /** Every question that was put, to put it again; and the answers that are in: the package that takes it, empty for none. */
    private val asked = ConcurrentHashMap<String, () -> String>()
    private val known = ConcurrentHashMap<String, String>()
    private val asking = AtomicBoolean(false)

    init {
        context.getSystemService(LauncherApps::class.java).registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) = again()
            override fun onPackageAdded(packageName: String, user: UserHandle) = again()
            override fun onPackageChanged(packageName: String, user: UserHandle) = again()
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = again()
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = again()
        }, Handler(Looper.getMainLooper()))
    }

    private fun answer(key: String, ask: () -> String): String {
        known[key]?.let { return it }
        asked.putIfAbsent(key, ask)
        if (Looper.myLooper() == Looper.getMainLooper()) { again(); return "" }
        return ask().also { known[key] = it }
    }

    /** Puts every question again, off the main thread. The answers that are in stand until the new ones are. */
    private fun again() {
        if (!asking.compareAndSet(false, true)) return
        app.scope.launch(Dispatchers.IO) {
            asking.set(false)     // a question that is put from here on gets a pass of its own
            for ((key, ask) in asked) known[key] = runCatching(ask).getOrDefault("")
        }
    }

    /**
     * The first of [packages] that is installed and takes [probe], a link of the kind it will be
     * handed, in an activity that is open to other apps and asks for no permission: the test the
     * executor makes again on the real link when the row is run. Null when none does.
     */
    fun taker(packages: List<String>, probe: String): String? = answer(probe) {
        val link = probe.toUri()
        packages.firstOrNull { pkg ->
            val a = pm.resolveActivity(Intent(Intent.ACTION_VIEW, link).setPackage(pkg), 0)?.activityInfo
            a != null && a.packageName == pkg && a.exported && a.permission == null
        }.orEmpty()
    }.ifEmpty { null }

    /** Whether some app answers [request], which names none: the phone app for a number, the messages app for a text. */
    fun answers(request: Intent): Boolean = answer("${request.action} ${request.scheme}") {
        if (pm.resolveActivity(request, PackageManager.MATCH_DEFAULT_ONLY) != null) ANY else ""
    }.isNotEmpty()

    /** [link] as an effect: handed to [app] when there is one, else opened as any link is, in the browser. */
    fun open(app: String?, link: String): Effect =
        if (app == null) Effect.OpenUrl(link) else Effect.Open(app, Intent(Intent.ACTION_VIEW, link.toUri()).setPackage(app).toUri(Intent.URI_INTENT_SCHEME))

    private companion object { const val ANY = "*" }
}

/**
 * A keyword that opens another app with something filled in: directions, a meeting, a number on the
 * dial screen, a text in a chat. Each reads its line with [Reach] and shows what it understood as a
 * preview row, as the scopes that jot something down do; Enter hands it over. Booklight dials
 * nothing and sends nothing: the last click is the user's, in that app.
 *
 * A keyword is only there where an app answers it on this device ([there]): elsewhere it has no
 * row and is ordinary text. And a keyword the user gave to a link, a prompt or an app command of
 * their own stays theirs.
 *
 * A line that was not meant for the keyword ("call of duty", "meet the parents") has no row: the
 * search for all of it is then row one, and Enter does something. While what is typed can still
 * become a number, a name or a code ([Reach.startsNumber] and its two neighbours), the preview
 * stays, with its hint and no action.
 */
abstract class ReachScope(
    protected val context: Context, final override val key: String, keys: Int, name: Int, hint: Int, about: Int, final override val symbol: String,
    /** The keywords of the user's own links, prompts and app commands. */
    private val users: () -> Set<String>,
) : Scope {
    private val all = context.getString(keys).split(',')
    final override val name: String = context.getString(name)
    final override val hint: String = context.getString(hint)
    final override val about: String = context.getString(about)

    /** Whether an app answers this keyword here. Directions and a meeting always open: a browser will do. */
    open val there: Boolean get() = true
    /** The words this scope answers to wherever its app is here: its own, but for those the user gave to something of theirs. */
    val claims: List<String> get() = users().let { u -> all.filter { it.lowercase() !in u } }
    final override val keywords: List<String> get() = if (there) claims else emptyList()
    /** `go`, `wa` and `tg` typed alone stay under the apps and settings that start with those letters. */
    final override val shy: List<String> get() = keywords.filter { it.length <= 2 }
    final override val listed: Boolean get() = there

    protected fun text(id: Int) = context.getString(id)
    protected fun slot(label: Int, value: String, guessed: Boolean = false) =
        Slot(context.getString(label), value, if (value.isEmpty()) SlotState.EMPTY else if (guessed) SlotState.GUESSED else SlotState.TYPED)

    /** The scope's one row: what was understood. No actions = not enough typed yet. */
    protected fun preview(caption: Int, slots: List<Slot>, actions: List<Action>) = Result(
        id = "reach:$key", provider = key, kind = Kind.OTHER, title = name, icon = Icon.Symbol(symbol), score = 1.0, learnable = false,
        body = Body.Slots(text(caption), slots), actions = actions,
    )
}

/**
 * `go hamburg hbf`, `go berlin to hamburg by train`: directions in Google Maps. The link goes to the
 * Maps app where it is installed; a Googlebook may have none, and then it opens in the browser.
 */
class GoScope(context: Context, private val takers: Takers, users: () -> Set<String>) : ReachScope(context, "go", R.string.go_keys, R.string.go_name, R.string.go_hint, R.string.go_about, "directions", users) {
    override suspend fun rows(arg: String): List<Result> {
        val trip = Reach.trip(arg)
        val app = takers.taker(MAPS, PROBE)
        val link = Reach.directions(trip)
        val by = when (trip.by) {
            Travel.TRANSIT -> text(R.string.travel_transit); Travel.WALK -> text(R.string.travel_walk)
            Travel.BIKE -> text(R.string.travel_bike); Travel.CAR -> text(R.string.travel_car); null -> ""
        }
        return listOf(preview(
            if (app != null) R.string.go_caption else R.string.go_caption_web,
            // No origin typed: the maps app starts from where the user is. The way of travelling has a slot once it is said:
            // until then the place is the last slot and has the room of the row (a middle slot is cut at 170 dp).
            listOfNotNull(slot(R.string.reach_from, trip.from.ifEmpty { text(R.string.go_here) }, guessed = trip.from.isEmpty()), slot(R.string.reach_to, trip.to), slot(R.string.reach_by, by).takeIf { by.isNotEmpty() }),
            if (trip.to.isBlank()) emptyList() else listOf(
                Action("go", text(R.string.action_directions), takers.open(app, link), symbol = "directions"),
                Action("link", text(R.string.action_copy_link), Effect.CopyText(link)),
            ),
        ))
    }

    private companion object {
        val MAPS = listOf("com.google.android.apps.maps")
        const val PROBE = "https://www.google.com/maps/dir/?api=1&destination=x"
    }
}

/**
 * `meet`: a new meeting. `meet abc-defg-hij`: that meeting. The link goes to the Meet app where it is
 * installed (on a Googlebook a web app, which opens the link in a window of its own), else to the browser.
 */
class MeetScope(context: Context, private val takers: Takers, users: () -> Set<String>) : ReachScope(context, "meet", R.string.meet_keys, R.string.meet_name, R.string.meet_hint, R.string.meet_about, "video", users) {
    // The Meet app has this scope's name: its row in the ordinary list is called something else.
    override val title: String = context.getString(R.string.meet_title)

    override suspend fun rows(arg: String): List<Result> {
        val typed = arg.trim()
        val code = Reach.meetCode(typed)
        val link = Reach.meet(code)
        val open = takers.open(takers.taker(MEET, PROBE), link)
        // Something that is no code and cannot become one: no row. The search for all of it is row one.
        if (code == null && !Reach.startsCode(typed)) return emptyList()
        return listOf(when {
            code != null -> preview(R.string.meet_caption_join, listOf(slot(R.string.reach_code, code)), listOf(
                Action("join", text(R.string.action_join), open, symbol = "video"),
                Action("link", text(R.string.action_copy_link), Effect.CopyText(link)),
            ))
            typed.isEmpty() -> preview(R.string.meet_caption_new, listOf(slot(R.string.reach_code, "")), listOf(Action("new", text(R.string.action_start), open, symbol = "video")))
            // The start of a code: the hint, and nothing to run yet.
            else -> preview(R.string.meet_caption_code, listOf(slot(R.string.reach_code, "")), emptyList())
        })
    }

    private companion object {
        val MEET = listOf("com.google.android.apps.tachyon")
        const val PROBE = "https://meet.google.com/new"
    }
}

/** `call +49 30 23125 123`: the phone app's dial screen with the number in it. The user presses call there. */
class CallScope(context: Context, private val takers: Takers, users: () -> Set<String>) : ReachScope(context, "call", R.string.call_keys, R.string.call_name, R.string.call_hint, R.string.call_about, "phone", users) {
    private val dial = Intent(Intent.ACTION_DIAL, "tel:0".toUri())
    override val there: Boolean get() = takers.answers(dial)

    override suspend fun rows(arg: String): List<Result> {
        val d = Reach.phone(arg)
        if (d == null && !Reach.startsNumber(arg)) return emptyList()
        return listOf(preview(
            R.string.call_caption, listOf(slot(R.string.slot_to, d?.shown.orEmpty())),
            if (d == null) emptyList() else listOf(
                Action("dial", text(R.string.action_dial), Effect.Phone(d.number), symbol = "phone"),
                Action("copy", text(R.string.action_copy), Effect.CopyText(d.number)),
            ),
        ))
    }
}

/** `sms +49 30 23125 123 running late`: the messages app's new message to that number, the text in its field. Nothing is sent. */
class SmsScope(context: Context, private val takers: Takers, users: () -> Set<String>) : ReachScope(context, "sms", R.string.sms_keys, R.string.sms_name, R.string.sms_hint, R.string.sms_about, "chat", users) {
    private val write = Intent(Intent.ACTION_SENDTO, "smsto:0".toUri())
    override val there: Boolean get() = takers.answers(write)

    override suspend fun rows(arg: String): List<Result> {
        val d = Reach.phone(arg)
        if (d == null && !Reach.startsNumber(arg)) return emptyList()
        return listOf(preview(
            R.string.sms_caption, listOf(slot(R.string.slot_to, d?.shown.orEmpty()), slot(R.string.slot_text, d?.text.orEmpty())),
            if (d == null) emptyList() else listOf(Action("compose", text(R.string.action_compose), Effect.Phone(d.number, message = true, text = d.text), symbol = "edit")),
        ))
    }
}

/** `wa +49 30 23125 123 running late`: WhatsApp's chat with that number, the text in its field. Nothing is sent. */
class WaScope(context: Context, private val takers: Takers, users: () -> Set<String>) : ReachScope(context, "wa", R.string.wa_keys, R.string.wa_name, R.string.wa_hint, R.string.chat_about, "chat", users) {
    override val title: String = context.getString(R.string.wa_title)
    private val app: String? get() = takers.taker(APPS, PROBE)
    override val there: Boolean get() = app != null

    override suspend fun rows(arg: String): List<Result> {
        val pkg = app ?: return emptyList()
        val d = Reach.phone(arg)
        if (d == null && !Reach.startsNumber(arg)) return emptyList()
        val full = d?.full
        return listOf(preview(
            // The link finds the chat by the whole number: one typed without its country code is nobody's. That row
            // stays, with no action: the line was meant for WhatsApp, and the caption says what the number lacks.
            if (d != null && full == null) R.string.wa_caption_code else R.string.wa_caption,
            listOf(slot(R.string.slot_to, d?.shown.orEmpty()), slot(R.string.slot_text, d?.text.orEmpty())),
            if (d == null || full == null) emptyList() else listOf(Action("chat", text(R.string.action_open_chat), takers.open(pkg, Reach.whatsapp(full, d.text)), symbol = "chat")),
        ))
    }

    private companion object {
        val APPS = listOf("com.whatsapp", "com.whatsapp.w4b")
        const val PROBE = "https://wa.me/0"
    }
}

/** `tg anna on my way`: Telegram's chat with that user name (or a number with its country code), the text in its field. Nothing is sent. */
class TgScope(context: Context, private val takers: Takers, users: () -> Set<String>) : ReachScope(context, "tg", R.string.tg_keys, R.string.tg_name, R.string.tg_hint, R.string.chat_about, "chat", users) {
    override val title: String = context.getString(R.string.tg_title)
    private val app: String? get() = takers.taker(APPS, PROBE)
    override val there: Boolean get() = app != null

    override suspend fun rows(arg: String): List<Result> {
        val pkg = app ?: return emptyList()
        // A number first, as in the other chats; else the first word is a user name.
        val d = Reach.phone(arg)
        val h = if (d == null) Reach.handle(arg) else null
        val who = d?.full?.let { "+$it" } ?: h?.name
        // Neither a number nor a name, and no longer the start of one: no row.
        if (d == null && h == null && !Reach.startsNumber(arg) && !Reach.startsName(arg)) return emptyList()
        return listOf(preview(
            if (who == null && arg.isNotBlank()) R.string.tg_caption_who else R.string.tg_caption,
            listOf(slot(R.string.slot_to, d?.shown ?: h?.name.orEmpty()), slot(R.string.slot_text, d?.text ?: h?.text.orEmpty())),
            if (who == null) emptyList() else listOf(Action("chat", text(R.string.action_open_chat), takers.open(pkg, Reach.telegram(who, d?.text ?: h?.text.orEmpty())), symbol = "chat")),
        ))
    }

    private companion object {
        val APPS = listOf("org.telegram.messenger", "org.telegram.messenger.web")
        const val PROBE = "https://t.me/telegram"
    }
}
