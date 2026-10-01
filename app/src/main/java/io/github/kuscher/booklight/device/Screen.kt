package io.github.kuscher.booklight.device

import android.content.Context
import android.provider.Settings
import kotlin.math.roundToInt

/**
 * The screen's brightness. Changing it needs the user to switch on "Modify system settings" for
 * Booklight once (a page in Settings); until then [allowed] is false and nothing is written.
 */
class Screen(private val context: Context) {
    val allowed: Boolean get() = Settings.System.canWrite(context)

    val percent: Int get() = (Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) * 100f / MAX).roundToInt().coerceIn(0, 100)

    fun step(dir: Int): Int = (percent + dir * 5).coerceIn(5, 100)

    fun set(percent: Int): Boolean {
        if (!allowed) return false
        return Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (percent.coerceIn(1, 100) * MAX / 100f).roundToInt().coerceIn(1, MAX))
    }

    private companion object { const val MAX = 255 }
}
