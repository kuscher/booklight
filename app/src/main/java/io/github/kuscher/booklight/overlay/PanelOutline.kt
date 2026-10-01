package io.github.kuscher.booklight.overlay

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * The window's background: draws nothing (Compose paints the panel), but has the panel's
 * rounded outline. The platform reads the blur region's corner radius from it.
 */
class PanelOutline(private val radius: Float) : Drawable() {
    override fun draw(canvas: Canvas) {}
    override fun getOutline(outline: Outline) { outline.setRoundRect(bounds, minOf(radius, bounds.width() / 2f, bounds.height() / 2f)); outline.alpha = 1f }
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
