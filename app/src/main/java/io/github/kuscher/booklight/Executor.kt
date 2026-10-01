package io.github.kuscher.booklight

import android.app.Activity
import android.app.ActivityOptions
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.PersistableBundle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.core.net.toUri
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.MediaKey
import io.github.kuscher.booklight.core.Place
import io.github.kuscher.booklight.data.SnippetEntry
import io.github.kuscher.booklight.device.Audio
import io.github.kuscher.booklight.device.Files
import io.github.kuscher.booklight.device.QrImages
import io.github.kuscher.booklight.device.Screen
import io.github.kuscher.booklight.entry.PickFolderActivity
import io.github.kuscher.booklight.settings.SettingsActivity

/**
 * Performs effects: the one place where a result's action touches Android. Returns false when it
 * couldn't (the app was removed a moment ago, no browser, nothing to play…), so the panel can
 * stay open and say so.
 *
 * Everything here either opens a window the user sees (a compose window, the calendar's editor,
 * Android's own "uninstall?" dialog), copies to the clipboard, or changes something the user can
 * see and undo at once (the volume). Nothing is sent anywhere.
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
        Log.w(BooklightApp.TAG, "effect failed: ${effect::class.simpleName}", e)
        false
    }

    private fun perform(effect: Effect, from: Activity?): Boolean {
        val ctx = from ?: context
        fun start(intent: Intent) = ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        when (effect) {
            is Effect.LaunchApp -> launch(effect, ctx)
            is Effect.AppInfo -> launcher.startAppDetailsActivity(ComponentName(effect.packageName, effect.className), user(effect.user), null, null)
            is Effect.StorePage -> try {
                start(Intent(Intent.ACTION_VIEW, "market://details?id=${effect.packageName}".toUri()))
            } catch (_: ActivityNotFoundException) {
                start(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${effect.packageName}".toUri()))
            }
            // Android asks "Do you want to uninstall this app?" itself. Needs REQUEST_DELETE_PACKAGES; without it nothing happens at all.
            is Effect.Uninstall -> start(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", effect.packageName, null)).putExtra(Intent.EXTRA_USER, user(effect.user)))
            is Effect.OpenUrl -> start(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
            is Effect.CopyText -> copy(effect.text, effect.sensitive)
            is Effect.ShareText -> start(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, effect.text), null))
            is Effect.OpenSettings -> start(Intent(effect.action))
            is Effect.Internal -> when (effect.command) {
                "settings", "window" -> start(Intent(context, SettingsActivity::class.java))
                // The system's Keyboard shortcuts window, where Customize adds an app shortcut.
                "shortcuts" -> from?.requestShowKeyboardShortcuts() ?: return false
                "done", "again" -> {}     // nothing to do here: the level was set as it was moved; a new password comes with the next list
                else -> return false
            }

            // The mail app's compose window, filled in. The user sends it there, or doesn't.
            is Effect.Compose -> start(Intent(Intent.ACTION_SENDTO, mailto(effect)))
            is Effect.AppendNote -> return app.notes.append(effect.text)
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
            })
            is Effect.PlayMusic -> start(Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*").putExtra(SearchManager.QUERY, effect.query))
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
                    else -> it
                }
            }
            is Effect.Edit -> start(Intent(context, SettingsActivity::class.java).putExtra("edit", effect.kind).putExtra("id", effect.id))
            // A recipe: each step in turn; it stops at the first that can't be done.
            is Effect.Steps -> return effect.steps.all { perform(it, from) }
            is Effect.EnterScope -> return false     // the panel does this itself
        }
        return true
    }

    /** Opens an app: as the launcher would, or in a place on the screen, or as another window of it. */
    private fun launch(e: Effect.LaunchApp, ctx: Context) {
        val component = ComponentName(e.packageName, e.className)
        val bounds = place(e.place)
        val options = bounds?.let { ActivityOptions.makeBasic().setLaunchBounds(it).toBundle() }
        if (!e.newWindow || e.user != me) {
            // Through LauncherApps, so apps of a work profile open too.
            launcher.startMainActivity(component, user(e.user), bounds, options)
            return
        }
        // Another window of the same app: a new task beside the ones it has. Apps that allow only one ignore this.
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        ctx.startActivity(intent, options)
    }

    /** The half or the whole of the screen that is left beside the system's bars. */
    private fun place(p: Place): Rect? {
        if (p == Place.NONE) return null
        val wm = context.getSystemService(WindowManager::class.java)
        val metrics = wm.maximumWindowMetrics
        val bars = metrics.windowInsets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars())
        val r = Rect(metrics.bounds).apply { left += bars.left; top += bars.top; right -= bars.right; bottom -= bars.bottom }
        return when (p) {
            Place.LEFT -> Rect(r.left, r.top, r.centerX(), r.bottom)
            Place.RIGHT -> Rect(r.centerX(), r.top, r.right, r.bottom)
            else -> r
        }
    }

    private fun copy(text: String, sensitive: Boolean) {
        val clip = ClipData.newPlainText("Booklight", text)
        // A password: the system's clipboard preview and history are asked not to show it.
        if (sensitive) clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    }

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
    }
}
