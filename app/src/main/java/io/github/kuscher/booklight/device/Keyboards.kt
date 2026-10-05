package io.github.kuscher.booklight.device

import android.view.InputDevice
import android.view.KeyEvent
import io.github.kuscher.booklight.core.FirstRun

/** The keyboards that are attached, as first run asks about them (core `FirstRun.hasQuickInsert`): real ones with letters, and whether each has the Quick Insert key. */
object Keyboards {
    fun attached(): List<FirstRun.Keyboard> = InputDevice.getDeviceIds().toList().mapNotNull { InputDevice.getDevice(it) }
        .filter { !it.isVirtual && it.supportsSource(InputDevice.SOURCE_KEYBOARD) && it.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC }
        .map { FirstRun.Keyboard(it.id, it.isExternal, it.hasKeys(KeyEvent.KEYCODE_CONTEXTUAL_INSERT)[0]) }
}
