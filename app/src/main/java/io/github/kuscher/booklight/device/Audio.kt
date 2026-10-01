package io.github.kuscher.booklight.device

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import kotlin.math.roundToInt

/**
 * The media volume and the media keys. No permission: Android lets an app change the volume while
 * one of its windows is on screen, which the panel is. What is playing, and in which app, is not
 * readable without notification access, so the media keys are sent blind.
 */
class Audio(context: Context) {
    private val am = context.getSystemService(AudioManager::class.java)
    private val stream = AudioManager.STREAM_MUSIC
    private val max get() = am.getStreamMaxVolume(stream).coerceAtLeast(1)

    val percent: Int get() = (am.getStreamVolume(stream) * 100f / max).roundToInt()
    val muted: Boolean get() = am.isStreamMute(stream) || am.getStreamVolume(stream) == 0
    val playing: Boolean get() = am.isMusicActive

    /** The level one step down or up: 5 %, or one of the device's own steps where those are coarser. */
    fun step(dir: Int): Int {
        val by = (max * 0.05f).roundToInt().coerceAtLeast(1)
        return ((am.getStreamVolume(stream) + dir * by).coerceIn(0, max) * 100f / max).roundToInt()
    }

    fun set(percent: Int) {
        if (am.isStreamMute(stream)) am.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
        am.setStreamVolume(stream, (percent.coerceIn(0, 100) * max / 100f).roundToInt(), 0)
    }

    fun toggleMute() = am.adjustStreamVolume(stream, AudioManager.ADJUST_TOGGLE_MUTE, 0)

    /** A media key, pressed and released: whichever app is playing gets it. */
    fun key(code: Int) {
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}
