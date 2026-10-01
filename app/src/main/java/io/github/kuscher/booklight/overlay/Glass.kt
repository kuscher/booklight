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
enum class GlassLevel(val id: String, val tintLight: Float, val tintDark: Float, val blurDp: Float) {
    CLEAR("clear", 0.28f, 0.38f, 14f),
    // Light theme over a black terminal is the hard case: 0.42 keeps titles at about 3:1 there. Frosted is for people who live in dark windows.
    BALANCED("balanced", 0.42f, 0.50f, 22f),
    FROSTED("frosted", 0.58f, 0.64f, 32f);

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: BALANCED
    }
}

/**
 * The shadow around the panel: how high the panel stands over the windows behind it. Higher is a
 * wider, softer shadow; [spot] is how dark it is under the panel's lower edge, where it is darkest,
 * and [ambient] the even part all round. Large and faint, never a dark rim: it is there so the panel
 * reads as on top, also over a window as light as itself. It lies only around the glass (see
 * [PanelOutline]). Measured on the Lenovo at 96 dp and spot 0.5: 48 % at the lower edge, gone 60 dp
 * out; 20 % at the sides; 3 % above.
 */
enum class Shade(val id: String, val height: Float, val spot: Float, val ambient: Float) {
    OFF("off", 0f, 0f, 0f),
    LOW("low", 72f, 0.14f, 0.03f),
    MEDIUM("medium", 96f, 0.24f, 0.05f),
    HIGH("high", 128f, 0.36f, 0.07f);

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}

/** The values in use. `./bl open stay tint=0.2 blur=24 dim=0.1` tries others on a debug build. */
object Look {
    var tintLight = GlassLevel.BALANCED.tintLight
    var tintDark = GlassLevel.BALANCED.tintDark
    var blurDp = GlassLevel.BALANCED.blurDp
    /** The rest of the screen darkens a little in light theme, and more in dark: a dark panel needs a darker room. */
    var dimLight = 0.10f
    var dimDark = 0.22f

    fun use(level: GlassLevel) { tintLight = level.tintLight; tintDark = level.tintDark; blurDp = level.blurDp }
}

/**
 * The panel's glass, as a shader over the system's window blur: a flat veil (white in light theme,
 * near-black in dark: the most contrast for the least tint), two flat rings at the edge (a black
 * hairline that holds the edge on a white page, then the white outline, even all the way round),
 * and fine grain so the blur doesn't band. Nothing is modelled in 3D: no bevel, no
 * highlights on the surface. On arrival one light runs once around the outline and splits into the
 * device's colours, like light through the edge of a pane. Android blurs what is behind a window
 * but doesn't let an app bend it, so the see-through look comes from the veil and blur amounts.
 */
private const val GLASS = """
uniform float2 size;
uniform float radius;
uniform float density;    // px per dp
uniform float4 tint;      // straight rgb, alpha
uniform float3 cLead;     // the gleam's leading colour
uniform float3 cTail;     // and its trailing one
uniform float run;        // the light's head: how far it has come round the outline, in px, clockwise from the top's middle
uniform float glow;       // how bright the light is, 0..1
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

    float a = mix(tint.a, 1.0, solid);
    float3 pm = (tint.rgb + (hash(xy) - 0.5) * 0.012) * a;       // premultiplied glass

    // The outermost pixel: a black hairline, so the edge holds on a white page.
    float hair = (1.0 - smoothstep(0.6, 1.2, depth)) * mix(0.20, 0.28, dark);
    pm *= 1.0 - hair;
    a = mix(a, 1.0, hair);

    // Then the white outline, 1.25 dp.
    float w = 1.25 * density;
    float line = smoothstep(0.6, 1.2, depth) * (1.0 - smoothstep(1.0 + w - 0.5, 1.0 + w + 0.5, depth));
    float base = mix(0.80, 0.44, dark);       // the same all the way round: no light from above
    // The arrival light lives only in this line: white at its core, the device's colours ahead and behind.
    // Where this pixel is along the outline, measured the way the light travels.
    float2 p = xy - size * 0.5;
    float2 h = size * 0.5;
    float2 e = abs(p) - h;
    float s = e.x > e.y
        ? (p.x > 0.0 ? size.x + p.y + h.y : 2.0 * size.x + size.y + h.y - p.y)
        : (p.y < 0.0 ? p.x + h.x : size.x + size.y + h.x - p.x);
    float around = 2.0 * (size.x + size.y);
    s = mod(s - h.x + around, around);
    float t = (run - s) / (120.0 * density);
    float core = glow * exp(-t * t);
    float lead = glow * exp(-(t + 0.9) * (t + 0.9));
    float tail = glow * exp(-(t - 0.9) * (t - 0.9));
    float3 lineCol = (float3(1.0) * (base + core) + cLead * lead + cTail * tail) / (base + core + lead + tail);
    float la = line * clamp(base + 0.35 * core + 0.30 * (lead + tail), 0.0, 1.0);
    pm = mix(pm, lineCol, la);
    a = mix(a, 1.0, la);

    return half4(half3(clamp(pm, 0.0, 1.0)) * aa, a * aa);
}
"""

/**
 * Draws the glass behind the content. [run] is how far round the outline the arrival light has come, 0 to 1
 * (and a little beyond, so its tail leaves too); [glow] its brightness.
 */
fun Modifier.glass(tint: Color, lead: Color, tail: Color, radiusPx: Float, density: Float, run: () -> Float, glow: () -> Float, dark: Boolean, solid: Boolean): Modifier = composed {
    val shader = remember { RuntimeShader(GLASS) }
    drawWithCache {
        val brush = ShaderBrush(shader)
        onDrawBehind {
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("radius", minOf(radiusPx, size.width / 2f, size.height / 2f))
            shader.setFloatUniform("density", density)
            shader.setFloatUniform("tint", tint.red, tint.green, tint.blue, tint.alpha)
            shader.setFloatUniform("cLead", lead.red, lead.green, lead.blue)
            shader.setFloatUniform("cTail", tail.red, tail.green, tail.blue)
            shader.setFloatUniform("run", run() * 2f * (size.width + size.height))
            shader.setFloatUniform("glow", glow())
            shader.setFloatUniform("dark", if (dark) 1f else 0f)
            shader.setFloatUniform("solid", if (solid) 1f else 0f)
            drawRect(brush)
        }
    }
}
