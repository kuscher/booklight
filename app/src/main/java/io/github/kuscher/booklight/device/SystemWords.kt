package io.github.kuscher.booklight.device

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.FirstRun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The system's own words for its Keyboard shortcuts dialog, so that first run can quote the buttons as the user
 * sees them: "Customize", "Set shortcut", and the name of the Quick Insert key. They are read from the system
 * UI's resources as any app reads another app's label, in the system's own language (Booklight can be set to
 * another one: hence the configuration passed here; `Resources.getSystem()` takes no such override), off the main
 * thread, and kept while the process lives. No permission, and no line in `<queries>`: the system UI's package was
 * visible to an ordinary app without one (docs/research/first-run-proof.md, check 9). Where a word cannot be had,
 * Booklight's own stands in (core `FirstRun.words`).
 */
object SystemWords {
    private const val SYSTEM_UI = "com.android.systemui"

    /** What was read; null until it has been, and where the system UI's resources could not be had. Snapshot state: what quotes it follows it. */
    var read by mutableStateOf<FirstRun.Read?>(null)
        private set

    /** Reads them, once in a process. Asked for when a screen of the key's step is due. */
    fun load(context: Context, scope: CoroutineScope) {
        if (read != null) return
        val app = context.applicationContext
        scope.launch(Dispatchers.Default) {
            val got = fetch(app)
            withContext(Dispatchers.Main) { read = got }
        }
    }

    private fun fetch(context: Context): FirstRun.Read? = runCatching {
        val pm = context.packageManager
        val system = context.getSystemService(LocaleManager::class.java).systemLocales
        val res = pm.getResourcesForApplication(pm.getApplicationInfo(SYSTEM_UI, 0), Configuration(context.resources.configuration).apply { setLocales(system) })
        fun word(name: String): String? = res.getIdentifier(name, "string", SYSTEM_UI).takeIf { it != 0 }?.let { runCatching { res.getString(it) }.getOrNull() }
        FirstRun.Read(word("shortcut_helper_customize_button_text"), word("shortcut_helper_customize_dialog_set_shortcut_button_label"), word("keyboard_key_quick_insert"))
    }.getOrNull()

    /** The words to quote now: the system's where they were read, else Booklight's own. */
    fun words(context: Context): FirstRun.Words = FirstRun.words(
        read, FirstRun.Words(context.getString(R.string.first_sys_customize), context.getString(R.string.first_sys_set), context.getString(R.string.first_key_quick)),
    )

    /** What the key that is suggested beside Action is called. */
    fun name(context: Context, key: FirstRun.Key): String = if (key == FirstRun.Key.QUICK_INSERT) words(context).quick else context.getString(R.string.first_key_letter)

    /**
     * Booklight's page in the system's Keyboard shortcuts dialog while the panel waits for its key: five numbered
     * steps, each with the suggested keys at its end (docs/design/first-run/design.md §5). No label and no heading
     * holds the app's name: the dialog's search would then stay on this page.
     */
    fun rows(context: Context, key: FirstRun.Key): KeyboardShortcutGroup {
        val words = words(context)
        val code = if (key == FirstRun.Key.QUICK_INSERT) KeyEvent.KEYCODE_CONTEXTUAL_INSERT else KeyEvent.KEYCODE_M
        val labels = listOf(
            context.getString(R.string.first_helper_1, words.customize),
            context.getString(R.string.first_helper_2),
            context.getString(R.string.first_helper_3, name(context, key)),
            context.getString(R.string.first_helper_4, words.set),
            context.getString(R.string.first_helper_5),
        )
        return KeyboardShortcutGroup(context.getString(R.string.first_helper_title), labels.map { KeyboardShortcutInfo(it, code, KeyEvent.META_META_ON) })
    }
}
