package io.github.kuscher.booklight.overlay

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewOutlineProvider

/**
 * The window's background. It paints nothing (Compose paints the panel), but it has the panel's
 * rounded outline: the platform reads the blur region's corner radius from it.
 *
 * It also keeps the shadow off the glass. The shadow around the panel is the system's own, cast by
 * the window's root view from [caster]'s outline: the glass as it stands ([glass]: it grows during
 * the arrival), at the strength [cast]. The system draws that shadow before anything of the window,
 * and under a see-through shape it would darken it; so the first thing drawn is the glass's own
 * shape, cleared. What is left of the shadow is only what lies around the panel.
 */
class PanelOutline(private val radius: Float) : Drawable() {
    /** Where the glass is in the window, in pixels; empty until the panel has said. */
    val glass = Rect()
    /** How much of its shadow the glass casts, 0 to 1: it comes with the glass as it opens and goes with it. */
    var cast = 0f; private set
    /** False: no shadow at all, and nothing to clear. */
    var shaded = false

    private val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    /** True if anything changed: the caster's outline and this drawable are then to be made again. */
    fun place(left: Int, top: Int, right: Int, bottom: Int, cast: Float): Boolean {
        if (glass.left == left && glass.top == top && glass.right == right && glass.bottom == bottom && this.cast == cast) return false
        glass.set(left, top, right, bottom); this.cast = cast
        return true
    }

    private fun corner(width: Int, height: Int) = minOf(radius, width / 2f, height / 2f)

    override fun draw(canvas: Canvas) {
        if (!shaded || glass.isEmpty) return
        val r = corner(glass.width(), glass.height())
        canvas.drawRoundRect(glass.left.toFloat(), glass.top.toFloat(), glass.right.toFloat(), glass.bottom.toFloat(), r, r, clear)
    }

    override fun getOutline(outline: Outline) { outline.setRoundRect(bounds, corner(bounds.width(), bounds.height())); outline.alpha = 1f }
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java") override fun getOpacity() = PixelFormat.TRANSLUCENT

    /** The outline the window's root view casts its shadow from: the glass, never more than the window. */
    val caster = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val bottom = minOf(glass.bottom, view.height)
            if (!shaded || cast <= 0f || glass.width() <= 0 || bottom <= glass.top) { outline.setEmpty(); return }
            outline.setRoundRect(glass.left, glass.top, glass.right, bottom, corner(glass.width(), bottom - glass.top))
            outline.alpha = cast.coerceIn(0f, 1f)
        }
    }
}
