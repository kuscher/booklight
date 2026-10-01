package io.github.kuscher.booklight

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.UserManager
import android.util.Log
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.settings.SettingsActivity

/**
 * Performs effects: the one place where a result's action touches Android. Returns false when it
 * couldn't (the app was removed a moment ago, no browser…), so the panel can stay open.
 */
class Executor(private val context: Context) {
    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val users = context.getSystemService(UserManager::class.java)

    fun run(effect: Effect, from: Activity? = null): Boolean = try {
        val ctx = from ?: context
        when (effect) {
            is Effect.LaunchApp -> launcher.startMainActivity(ComponentName(effect.packageName, effect.className), user(effect.user), null, null)
            is Effect.AppInfo -> launcher.startAppDetailsActivity(ComponentName(effect.packageName, effect.className), user(effect.user), null, null)
            is Effect.OpenUrl -> ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            is Effect.WebSearch -> ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(SEARCH_URL + Uri.encode(effect.query))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            is Effect.CopyText -> context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Booklight", effect.text))
            is Effect.OpenSettings -> ctx.startActivity(Intent(effect.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            is Effect.Internal -> when (effect.command) {
                "settings" -> ctx.startActivity(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                // The system's Keyboard shortcuts window, where Customize adds an app shortcut.
                "shortcuts" -> from?.requestShowKeyboardShortcuts() ?: return false
                else -> return false
            }
        }
        true
    } catch (e: Exception) {
        Log.w(BooklightApp.TAG, "effect failed: $effect", e)
        false
    }

    private fun user(serial: Long) = users.getUserForSerialNumber(serial) ?: android.os.Process.myUserHandle()

    companion object {
        // 0.1: one fixed engine, opened in the default browser. Choosing the engine is on the plan.
        const val SEARCH_URL = "https://www.google.com/search?q="
    }
}
