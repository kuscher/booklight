package io.github.kuscher.booklight.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Calendars
import android.util.Log
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.core.Cal
import io.github.kuscher.booklight.core.Cals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The user's calendars, as the system lists them: each one's name, its colour, the id the system has for
 * it, its owner's address (what a calendar link names it by), whether it is the account's own and whether
 * events can be added to it. **That list and nothing else: no event is ever read**, here or anywhere in
 * Booklight.
 *
 * Only with the Calendar permission, which Booklight asks for in its own window and nowhere else. Without
 * it the list is empty and the system is asked nothing. The list is read when a panel is made and when
 * the Booklight window has the keys again, off the main thread, and kept: typing reads what is kept,
 * never the system. Only the calendars of Google accounts are listed: those are the ones the Calendar
 * app on a Googlebook shows and a calendar link can name. Each is marked, once, where the list is read,
 * with whether its name can be written into a sentence and read back as itself (core `Cals.marked`): only
 * such a one is a line of the event row's list or completed from its first letters.
 */
class CalendarList(private val context: Context, private val scope: CoroutineScope) {
    private val read = MutableStateFlow<List<Cal>>(emptyList())
    /** Debug builds: a list to show in place of the device's (`./bl debug calendars pretend A,B`), to look at the row without the permission. */
    private val pretended = MutableStateFlow<List<Cal>?>(null)
    private val _now = MutableStateFlow<List<Cal>>(emptyList())

    /** The calendars as they were last read (or pretended), the account's own first. */
    val now: StateFlow<List<Cal>> = _now
    val known: List<Cal> get() = _now.value

    /** The list may be read: the user allowed it in the Booklight window. */
    val allowed: Boolean get() = granted(Manifest.permission.READ_CALENDAR)
    /** An event may be added: the user switched that on in the Booklight window, and the system agreed. */
    val writes: Boolean get() = granted(Manifest.permission.WRITE_CALENDAR)
    /** A list is pretended (debug builds): the row is as with the permission, and nothing is written. */
    val pretends: Boolean get() = pretended.value != null

    private fun granted(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    /** The reading that is on its way: a newer one, or the permission gone, gives it up, so an older list never lands over a newer state. */
    private var reading: Job? = null

    /** Reads the list again, if it may be read; taken back (in the system's settings), the list is empty at once. Called on the main thread. */
    fun refresh() {
        reading?.cancel()
        if (!allowed) { reading = null; set(emptyList()); return }
        reading = scope.launch(Dispatchers.IO) { val list = query(); ensureActive(); set(list) }
    }

    /** Debug builds: [list] in place of the device's calendars; null for the device's own again. */
    fun pretend(list: List<Cal>?) { pretended.value = list?.let { Cals.marked(it, java.time.LocalDateTime.now()) }; _now.value = pretended.value ?: read.value }

    private fun set(list: List<Cal>) { read.value = list; _now.value = pretended.value ?: list }

    private fun query(): List<Cal> = try {
        val out = ArrayList<Cal>()
        context.contentResolver.query(Calendars.CONTENT_URI, COLUMNS, "${Calendars.ACCOUNT_TYPE} = ?", arrayOf(GOOGLE), null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1)?.trim().orEmpty()
                if (name.isEmpty()) continue
                val owner = c.getString(3).orEmpty()
                out += Cal(
                    id = c.getLong(0), name = name, color = c.getInt(2) or OPAQUE, owner = owner,
                    // (Where the system does not say which is the account's own: the one the account itself owns.)
                    primary = c.getInt(4) == 1 || (owner.isNotEmpty() && owner == c.getString(6)),
                    writable = c.getInt(5) >= Calendars.CAL_ACCESS_CONTRIBUTOR,
                )
            }
        }
        Cals.marked(out.sortedWith(compareByDescending<Cal> { it.primary }.thenBy { it.name.lowercase() }), java.time.LocalDateTime.now())
    } catch (e: Exception) {
        // Only the kind of failure: nothing of the list is logged.
        Log.w(BooklightApp.TAG, "calendars not read (${e.javaClass.simpleName})")
        emptyList()
    }

    private companion object {
        /** What is read of a calendar, and nothing more. */
        val COLUMNS = arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.CALENDAR_COLOR, Calendars.OWNER_ACCOUNT, Calendars.IS_PRIMARY, Calendars.CALENDAR_ACCESS_LEVEL, Calendars.ACCOUNT_NAME)
        const val GOOGLE = "com.google"
        const val OPAQUE = 0xFF000000.toInt()
    }
}
