package io.github.kuscher.booklight.overlay

import android.graphics.RuntimeShader
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush

/**
 * How see-through the panel is. The glass is the system's window blur with a thin tint over it:
 * the tint is how much of the surface colour is laid over the blur (less = more of the desktop
 * shows), the blur how far it spreads (less = shapes behind stay recognisable), the dim how much
 * the rest of the screen darkens. The user picks one of three in settings.
 */
enum class GlassLevel(val id: String, val tintLight: Float, val tintDark: Float, val blurDp: Float, val dim: Float) {
    CLEAR("clear", 0.10f, 0.16f, 14f, 0.06f),
    BALANCED("balanced", 0.20f, 0.26f, 22f, 0.08f),
    FROSTED("frosted", 0.34f, 0.40f, 36f, 0.10f);

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: BALANCED
    }
}

/** The values in use. `./bl open stay tint=0.2 blur=24 dim=0.1` tries others on a debug build. */
object Look {
    var tintLight = GlassLevel.BALANCED.tintLight
    var tintDark = GlassLevel.BALANCED.tintDark
    var blurDp = GlassLevel.BALANCED.blurDp
    var dim = GlassLevel.BALANCED.dim

    fun use(level: GlassLevel) { tintLight = level.tintLight; tintDark = level.tintDark; blurDp = level.blurDp; dim = level.dim }
}

/**
 * The panel's glass, as a shader over the system's window blur: a flat, thin tint, a crisp white
 * outline (a little brighter along the top), and fine grain so the blur doesn't band. Nothing is
 * modelled in 3D: no bevel, no highlights on the surface. On arrival a short gleam runs along the
 * outline. Android blurs what is behind a window but doesn't let an app bend it, so the
 * see-through look comes from the tint and blur amounts in [Look].
 */
private const val GLASS = """
uniform float2 size;
uniform float radius;
uniform float4 tint;      // straight rgb, alpha
uniform float sweep;      // the arrival gleam's position along x + 0.6 y, in px
uniform float dark;       // 0 light theme, 1 dark
uniform float solid;      // 1: no blur behind, so stay opaque

float box(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}
float hash(float2 p) { return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453); }

half4 main(float2 xy) {
    float d = box(xy - size * 0.5, size * 0.5, radius);
    float aa = clamp(0.5 - d, 0.0, 1.0);
    if (aa <= 0.0) { return half4(0.0); }
    float depth = -d;

    float3 col = tint.rgb;
    float a = mix(tint.a, 1.0, solid);

    // The outline: one crisp line, strongest along the top edge, never gone.
    float line = 1.0 - smoothstep(0.2, 1.5, depth);
    float up = 1.0 - clamp(xy.y / max(size.y, 1.0), 0.0, 1.0);
    float t = (xy.x + xy.y * 0.6 - sweep) / 90.0;
    float gleam = exp(-t * t);
    float la = line * clamp(mix(0.5, 0.9, up) * mix(1.0, 0.6, dark) + gleam * 0.5, 0.0, 1.0);
    col = mix(col, float3(1.0), la);
    a = max(a, la);

    col += (hash(xy) - 0.5) * 0.012;
    return half4(half3(clamp(col, 0.0, 1.0)) * a * aa, a * aa);
}
"""

/** Draws the glass behind the content. [sweep] runs from before the left edge to past the right one as the panel arrives. */
fun Modifier.glass(tint: Color, radiusPx: Float, sweep: () -> Float, dark: Boolean, solid: Boolean): Modifier = composed {
    val shader = remember { RuntimeShader(GLASS) }
    drawWithCache {
        val brush = ShaderBrush(shader)
        onDrawBehind {
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("radius", radiusPx)
            shader.setFloatUniform("tint", tint.red, tint.green, tint.blue, tint.alpha)
            shader.setFloatUniform("sweep", sweep())
            shader.setFloatUniform("dark", if (dark) 1f else 0f)
            shader.setFloatUniform("solid", if (solid) 1f else 0f)
            drawRect(brush)
        }
    }
}
