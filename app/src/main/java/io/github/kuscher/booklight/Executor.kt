package io.github.kuscher.booklight

import android.app.Activity
import android.app.ActivityOptions
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.WindowManager
import androidx.core.net.toUri
import io.github.kuscher.booklight.core.AppPage
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.device.Clipboard
import io.github.kuscher.booklight.core.MediaKey
import io.github.kuscher.booklight.core.Box
import io.github.kuscher.booklight.core.Place
import io.github.kuscher.booklight.core.PlayMode
import io.github.kuscher.booklight.core.Places
import io.github.kuscher.booklight.data.SnippetEntry
import io.github.kuscher.booklight.device.Audio
import io.github.kuscher.booklight.device.Files
import io.github.kuscher.booklight.device.QrImages
import io.github.kuscher.booklight.device.Screen
import io.github.kuscher.booklight.entry.PickFolderActivity
import io.github.kuscher.booklight.pin.PinActivity
import io.github.kuscher.booklight.pin.Pinned
import io.github.kuscher.booklight.window.MainActivity

/**
 * Performs effects: the one place where a result's action touches Android. Returns false when it
 * couldn't (the app was removed a moment ago, no browser, nothing to play…), so the panel can
 * stay open and say so.
 *
 * Most effects open a window the user sees (a compose window, the calendar's editor, Android's own
 * "uninstall?" dialog) or copy to the clipboard. A few act at once and say so in the panel's footer:
 * a note is added, a file made, a timer or alarm set, a level changed, a snippet saved or deleted.
 * Nothing is sent anywhere.
 */
class Executor(private val context: Context) {
    private val app get() = context.applicationContext as BooklightApp
    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val users = context.getSystemService(UserManager::class.java)
    private val me = users.getSerialNumberForUser(android.os.Process.myUserHandle())
    val audio = Audio(context)
    val screen = Screen(context)

    fun run(effect: Effect, from: Activity? = null): Boolean = try {
        perform(effect, from)
    } catch (e: Exception) {
        // Only the kind of failure: an exception's message can carry what was typed or copied (an address that could not be opened).
        Log.w(BooklightApp.TAG, "effect failed: ${effect::class.simpleName} (${e.javaClass.simpleName})")
        false
    }

    private fun perform(effect: Effect, from: Activity?): Boolean {
        val ctx = from ?: context
        fun start(intent: Intent) = ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        when (effect) {
            is Effect.LaunchApp -> launch(effect, ctx)
            is Effect.AppInfo -> launcher.startAppDetailsActivity(ComponentName(effect.packageName, effect.className), user(effect.user), null, null)
            is Effect.AppSettings -> start(page(effect))
            // Android asks "Do you want to uninstall this app?" itself. Needs REQUEST_DELETE_PACKAGES; without it nothing happens at all.
            is Effect.Uninstall -> start(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", effect.packageName, null)).putExtra(Intent.EXTRA_USER, user(effect.user)))
            is Effect.OpenUrl -> start(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
            is Effect.CopyText -> copy(effect.text, effect.sensitive)
            is Effect.OpenSettings -> start(Intent(effect.action))
            is Effect.Internal -> when (effect.command) {
                "settings", "window" -> start(Intent(context, MainActivity::class.java))
                // The list of everything, in the window.
                "commands" -> start(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_PAGE, "commands"))
                // Where the key for a flight's times is set: the window, at that row.
                "flights" -> start(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_PAGE, MainActivity.PAGE_FLIGHTS))
                // And where the key for Spotify is set.
                "songs" -> start(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_PAGE, MainActivity.PAGE_SONGS))
                // The system's Keyboard shortcuts window, where Customize adds an app shortcut.
                "shortcuts" -> from?.requestShowKeyboardShortcuts() ?: return false
                "done", "again" -> {}     // nothing to do here: the level was set as it was moved; a new password comes with the next list
                // The system fetches the on-device model, once, for every app; Booklight only asks it to.
                "model" -> app.onDevice.download()
                else -> return false
            }

            // The mail app's compose window, filled in. The user sends it there, or doesn't.
            is Effect.Compose -> start(Intent(Intent.ACTION_SENDTO, mailto(effect)))
            // The dial screen or a new message, filled in, for whichever app answers. Never ACTION_CALL: that dials by itself and needs a permission.
            is Effect.Phone -> {
                if (!NUMBER.matches(effect.number)) return false
                start(if (!effect.message) Intent(Intent.ACTION_DIAL, "tel:${effect.number}".toUri())
                    else Intent(Intent.ACTION_SENDTO, "smsto:${effect.number}".toUri()).apply { if (effect.text.isNotBlank()) putExtra("sms_body", effect.text) })
            }
            is Effect.AppendNote -> return app.notes.append(effect.text, effect.file)
            is Effect.AddTodo -> return app.notes.addTodo(effect.text)
            is Effect.TickTodo -> return app.notes.tick(effect.line, effect.text, effect.done).also { if (it) app.scopes.todo.ticked(effect.line, effect.done) }
            // Whatever edits text opens the file; Booklight hands over the right to read and write this one file.
            is Effect.OpenNote -> {
                val uri = app.notes.uri(effect.file) ?: return false
                val grant = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try { start(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "text/markdown").addFlags(grant)) }
                catch (_: ActivityNotFoundException) { start(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "text/plain").addFlags(grant)) }
            }
            is Effect.KeepNote -> try {
                start(Intent("com.google.android.gms.actions.CREATE_NOTE").setType("text/plain").putExtra(Intent.EXTRA_TEXT, effect.text))
            } catch (_: ActivityNotFoundException) {
                start(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, effect.text), null))
            }
            is Effect.InsertEvent -> start(Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, effect.title)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, effect.startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, effect.endMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, effect.allDay)
                .apply { if (effect.place.isNotBlank()) putExtra(CalendarContract.Events.EVENT_LOCATION, effect.place) })
            is Effect.SetTimer -> start(Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, effect.seconds).putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .apply { if (effect.label.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, effect.label) })
            is Effect.SetAlarm -> start(Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, effect.hour).putExtra(AlarmClock.EXTRA_MINUTES, effect.minute).putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .apply { if (effect.label.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, effect.label) })
            is Effect.ClockList -> start(Intent(if (effect.timers) AlarmClock.ACTION_SHOW_TIMERS else AlarmClock.ACTION_SHOW_ALARMS))
            is Effect.NewFile -> return Files.create(ctx, effect.name, effect.folder, effect.pick)
            // The Gemini app shows the text in its prompt; the user sends it there. Without that app: whatever takes text.
            is Effect.AskGemini -> {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, effect.text)
                try { start(Intent(send).setPackage(GEMINI)) } catch (_: ActivityNotFoundException) { start(Intent.createChooser(send, null)) }
            }

            is Effect.SetVolume -> audio.set(effect.percent)
            is Effect.ToggleMute -> audio.toggleMute()
            is Effect.Media -> audio.key(when (effect.key) {
                MediaKey.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                MediaKey.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
                MediaKey.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                // Play resumes the player that played last; Pause and Stop can never start anything.
                MediaKey.PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
                MediaKey.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
                MediaKey.STOP -> KeyEvent.KEYCODE_MEDIA_STOP
            })
            is Effect.PlayMusic -> {
                val player = effect.packageName
                // A link of the player's own that plays exactly this, where one was found (Spotify asks who sends it); else the request.
                val intent = if (player != null && effect.link.isNotEmpty()) Intent(Intent.ACTION_VIEW, effect.link.toUri()).putExtra(Intent.EXTRA_REFERRER, "android-app://${context.packageName}".toUri())
                    else play(effect)
                if (player == null) start(intent) else {
                    // Aimed at one player: only at an activity of its own that any app may start.
                    if (aim(intent, player) != null) return false
                    start(intent)
                    // The player that was asked last is the one armed next time.
                    if (app.prefs.now.player != player) app.prefs.update { it.copy(player = player) }
                }
            }
            is Effect.SetBrightness -> return screen.set(effect.percent)
            is Effect.QrImage -> return QrImages.use(ctx, effect.text, effect.use)

            is Effect.Grant -> when (effect.what) {
                "brightness" -> start(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                "notes" -> start(Intent(context, PickFolderActivity::class.java))
                // With a note to add once the folder is chosen: "notes", a line break, the note.
                else -> if (effect.what.startsWith("notes\n")) start(Intent(context, PickFolderActivity::class.java).putExtra(PickFolderActivity.EXTRA_NOTE, effect.what.substringAfter('\n'))) else return false
            }
            is Effect.SaveSnippet -> app.prefs.update { it.copy(snippets = it.snippets.filter { s -> s.key != effect.key } + SnippetEntry(effect.key, effect.text)) }
            is Effect.Delete -> app.prefs.update {
                when (effect.kind) {
                    "snippet" -> it.copy(snippets = it.snippets.filter { s -> s.key != effect.id })
                    "quicklink" -> it.copy(sites = it.sites.filter { s -> s.keyword != effect.id })
                    "recipe" -> it.copy(recipes = it.recipes.filter { r -> r.id != effect.id })
                    "appcommand" -> it.copy(ownCommands = it.ownCommands.filter { c -> c.id != effect.id })
                    else -> it
                }
            }
            is Effect.Edit -> start(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_EDIT, effect.kind).putExtra(MainActivity.EXTRA_ID, effect.id))
            // A recipe: each step in turn; it stops at the first that can't be done.
            is Effect.Steps -> return effect.steps.all { perform(it, from) }
            is Effect.EnterScope, is Effect.OpenList, is Effect.Type, is Effect.Ask, is Effect.Unsuggest -> return false     // the panel does these itself
            // 2.0, each in its own task:
            is Effect.Open -> return open(effect, ctx)
            // The app the text came from asked for text back (its selection menu): this is the answer to that.
            is Effect.Replace -> (from ?: return false).setResult(Activity.RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, effect.text))
            is Effect.Pin -> pin(effect, ctx)
            is Effect.Unpin -> { PinActivity.current.get()?.finishAndRemoveTask(); app.pinned.value = null }
        }
        return true
    }

    /** Why something another app was asked for is not started. */
    enum class Refusal {
        /** The address is not one Android reads. */
        UNREADABLE,
        /** The app is not on this device. */
        GONE,
        /** It names an activity of another app. */
        ANOTHER,
        /** The app has no activity that takes it. */
        NOTHING,
        /** The activity is not open to other apps. */
        CLOSED,
        /** The activity asks for a permission. */
        PERMISSION,
        /** Android would not start it. */
        FAILED,
    }

    /**
     * One of an app's own pages in Settings: the notification page takes the package as an extra, the
     * others as the address. Where Settings has no such page nothing answers, and [run] says so.
     */
    private fun page(e: Effect.AppSettings): Intent {
        val app = Uri.fromParts("package", e.packageName, null)
        return when (e.page) {
            AppPage.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, e.packageName)
            AppPage.LANGUAGE -> Intent(Settings.ACTION_APP_LOCALE_SETTINGS, app)
            AppPage.DEFAULTS -> Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, app)
            AppPage.BATTERY -> Intent(POWER_USAGE_DETAIL, app)
        }
    }

    /**
     * Something another app offers, or an app command of the user's own. Whatever the address says, it is
     * started only if it leads to an activity of [Effect.Open.owner] itself, open to other apps, that asks
     * for no permission; with no flags but Booklight's own, no data to grant, nothing to choose from. Only
     * ever an activity: nothing is broadcast and no service is started.
     */
    private fun open(e: Effect.Open, ctx: Context): Boolean {
        ctx.startActivity(checked(e).first ?: return false)
        return true
    }

    /** [open] for the Try button of an app command's editor: it starts it, or says why not. Null: it was started. */
    fun tryOpen(e: Effect.Open, from: Activity): Refusal? {
        val (intent, refusal) = checked(e)
        if (intent == null) return refusal
        return try { from.startActivity(intent); null } catch (x: Exception) {
            Log.w(BooklightApp.TAG, "try failed (${x.javaClass.simpleName})")
            Refusal.FAILED
        }
    }

    /** The intent to start for [e], if it passes; else why it does not. */
    private fun checked(e: Effect.Open): Pair<Intent?, Refusal?> {
        val intent = try { Intent.parseUri(e.intent, Intent.URI_INTENT_SCHEME) } catch (_: Exception) { return null to Refusal.UNREADABLE }
        intent.selector = null
        intent.clipData = null
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        aim(intent, e.owner)?.let { return null to it }
        return intent to null
    }

    /**
     * Makes [intent] one for the activity of [owner] that answers it. Null when it may be started; else why
     * not: nothing may be started unless that is an activity of [owner] itself, open to other apps, that
     * asks for no permission.
     */
    private fun aim(intent: Intent, owner: String): Refusal? {
        if (intent.component == null) intent.setPackage(owner) else if (intent.component?.packageName != owner) return Refusal.ANOTHER
        val pm = context.packageManager
        // Only the owner is asked. Where several of its activities take the request and the system has no favourite among
        // them (it would show its chooser, which is not the owner's), it is the first of them the system lists.
        val a = pm.resolveActivity(intent, 0)?.activityInfo?.takeIf { it.packageName == owner }
            ?: pm.queryIntentActivities(intent, 0).firstOrNull { it.activityInfo?.packageName == owner }?.activityInfo
            ?: return if (runCatching { pm.getApplicationInfo(owner, 0) }.isSuccess) Refusal.NOTHING else Refusal.GONE
        if (!a.exported) return Refusal.CLOSED
        if (a.permission != null) return Refusal.PERMISSION
        intent.setClassName(a.packageName, a.name)
        return null
    }

    /**
     * Android's "play from search" request: the mode as its focus, the mode's names as its extras,
     * and always the query, which is all a player reads that does not know the modes.
     */
    private fun play(e: Effect.PlayMusic): Intent {
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).putExtra(SearchManager.QUERY, e.query)
        intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, when (e.mode) {
            PlayMode.ANY -> "vnd.android.cursor.item/*"
            PlayMode.SONG -> MediaStore.Audio.Media.ENTRY_CONTENT_TYPE
            PlayMode.ALBUM -> MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE
            PlayMode.ARTIST -> MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE
            // (The platform's own constants for a playlist are marked as outdated since Android 12; the request still has the mode.)
            PlayMode.PLAYLIST -> "vnd.android.cursor.item/playlist"
            PlayMode.GENRE -> MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE
        })
        fun name(extra: String, value: String) { if (value.isNotEmpty()) intent.putExtra(extra, value) }
        name(MediaStore.EXTRA_MEDIA_TITLE, e.title)
        name(MediaStore.EXTRA_MEDIA_ARTIST, e.artist)
        name(MediaStore.EXTRA_MEDIA_ALBUM, e.album)
        name("android.intent.extra.playlist", e.playlist)
        name(MediaStore.EXTRA_MEDIA_GENRE, e.genre)
        return intent
    }

    /**
     * The pinned window. One at a time: a new pin of the shape of the one that is there takes its
     * window; another shape closes it and opens its own. A new window opens where the system will
     * put it once it is on top (the lower right corner), at about its size, so that going on top
     * is a small step and not a flight across the screen.
     */
    private fun pin(e: Effect.Pin, ctx: Context) {
        val p = Pinned(e.kind, e.text, e.value, e.note, if (e.kind == "timer") System.currentTimeMillis() + e.value * 1000 else 0)
        app.pinned.value = p
        val (w, h) = p.size(context)
        PinActivity.current.get()?.takeIf { !it.isFinishing }?.let { if (it.size == (w to h)) { it.show(p); return } else it.finishAndRemoveTask() }
        val metrics = context.getSystemService(WindowManager::class.java).maximumWindowMetrics
        val bars = metrics.windowInsets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars())
        val d = context.resources.displayMetrics.density
        val right = metrics.bounds.right - bars.right - (16 * d).toInt()
        val bottom = metrics.bounds.bottom - bars.bottom - (16 * d).toInt()
        // An ordinary window is never lower than 220 dp here, and has the system's caption on top.
        val bounds = Rect(right - (maxOf(w, Pinned.WIDTH) * d).toInt(), bottom - (maxOf(h + 40, 220) * d).toInt(), right, bottom)
        // A task of its own, said outright: for a single-task activity the desktop would give the new window the size of
        // whichever Booklight window is in front (docs/research/device-findings.md).
        ctx.startActivity(Intent(context, PinActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK).putExtras(p.bundle()),
            ActivityOptions.makeBasic().setLaunchBounds(bounds).toBundle())
    }

    /** Opens an app: as the launcher would, or in a place on a screen, or as another window of it. */
    private fun launch(e: Effect.LaunchApp, ctx: Context) {
        val component = ComponentName(e.packageName, e.className)
        // The screen that was asked for by its number ("… on display 2"), if it is still there.
        val display = if (e.display > 0) context.getSystemService(DisplayManager::class.java).displays.getOrNull(e.display - 1) else null
        val bounds = place(e.place, display)
        val options = if (bounds == null && display == null) null else ActivityOptions.makeBasic().apply {
            bounds?.let { setLaunchBounds(it) }
            display?.let { launchDisplayId = it.displayId }
        }.toBundle()
        if (!e.newWindow || e.user != me) {
            // Through LauncherApps, so apps of a work profile open too.
            launcher.startMainActivity(component, user(e.user), null, options)
            return
        }
        // Another window of the same app: a new task beside the ones it has. Apps that allow only one ignore this.
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        ctx.startActivity(intent, options)
    }

    /** Where [p] is on a screen: the part of it the system's bars leave free, divided by [Places]. */
    private fun place(p: Place, display: Display?): Rect? {
        if (p == Place.NONE) return null
        val on = if (display == null) context else context.createWindowContext(display, WindowManager.LayoutParams.TYPE_APPLICATION, null)
        val metrics = on.getSystemService(WindowManager::class.java).maximumWindowMetrics
        val bars = metrics.windowInsets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars())
        val b = metrics.bounds
        val r = Places.bounds(p, Box(b.left + bars.left, b.top + bars.top, b.right - bars.right, b.bottom - bars.bottom)) ?: return null
        return Rect(r.left, r.top, r.right, r.bottom)
    }

    private fun copy(text: String, sensitive: Boolean) = Clipboard.set(context, text, sensitive)

    private fun mailto(e: Effect.Compose): Uri {
        val query = listOfNotNull(
            e.subject.takeIf { it.isNotEmpty() }?.let { "subject=" + Uri.encode(it) },
            e.body.takeIf { it.isNotEmpty() }?.let { "body=" + Uri.encode(it) },
        ).joinToString("&")
        return ("mailto:" + e.to.joinToString(",") { Uri.encode(it, "@") } + if (query.isEmpty()) "" else "?$query").toUri()
    }

    private fun user(serial: Long) = users.getUserForSerialNumber(serial) ?: android.os.Process.myUserHandle()

    companion object {
        const val GEMINI = "com.google.android.apps.bard"
        /** A number as an address takes it: digits, with a + in front or without. Anything else is not put into one. */
        private val NUMBER = Regex("\\+?[0-9]+")
        /**
         * An app's battery-use page. The SDK (37) has no public constant for this action, unlike the other
         * three pages, so it is written out: Settings answers it from an activity open to every app that asks
         * for no permission (docs/research/intents.md, "Tried on a Googlebook").
         */
        const val POWER_USAGE_DETAIL = "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL"
    }
}
