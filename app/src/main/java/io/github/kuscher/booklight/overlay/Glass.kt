package io.github.kuscher.booklight.overlay

import android.graphics.RuntimeShader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.material3.ColorScheme
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
 * The selection's colour: the one coloured surface of the panel, the scheme's secondary container. Dense in light
 * theme (over a dark window it is what makes the chosen row readable). In dark theme on glass it is see-through
 * like the panel: at 42 % the page behind still reads through it, it keeps its colour, and it is the same step from
 * the glass over a white window and over a dark one. On solid ground there is nothing behind it to show, and it
 * stays dense. [danger]: the row removes something.
 */
fun selectionFill(scheme: ColorScheme, dark: Boolean, glass: Boolean, danger: Boolean = false): Color = when {
    danger -> scheme.errorContainer.copy(alpha = if (!dark) 0.85f else if (glass) 0.50f else 0.70f)
    !dark -> scheme.secondaryContainer.copy(alpha = 0.78f)
    glass -> scheme.secondaryContainer.copy(alpha = 0.42f)
    else -> scheme.secondaryContainer.copy(alpha = 0.66f)
}

/** First run's welcome is one night in both themes: the veil's colour then, and how much of it lies over the blur (by day: white at 0.42, near-black at 0.50). */
val NIGHT = Color(0xFF0A0C12)
const val NIGHT_VEIL = 0.80f
/** How far the desk behind the panel dims in that night. */
const val NIGHT_DESK = 0.50f

/**
 * What first run's welcome asks of the glass, written for each frame and read where the glass is drawn: how far the
 * [veil] has gone to night and how far the outline has ([rim]: full white at 1), each 0 to 1. Two numbers: a typed key
 * puts the lamp out in its frame, and the veil is the theme's own at once while the outline relaxes with the rest.
 */
class Night {
    var veil by mutableFloatStateOf(0f)
    var rim by mutableFloatStateOf(0f)
}

/**
 * The panel's glass, as a shader over the system's window blur: a flat veil (white in light theme,
 * near-black in dark: the most contrast for the least tint), two flat rings at the edge (a black
 * hairline that holds the edge on a white page, then the white outline, even all the way round),
 * and fine grain so the blur doesn't band. Nothing is modelled in 3D: no bevel, no
 * highlights on the surface. A while after the panel has opened one white reflection runs once
 * around the outline, as if a light had passed over the pane's edge: the outline is brighter and a
 * little wider where it is, and a little of it falls on the glass beside it. Android blurs what is
 * behind a window but doesn't let an app bend it, so the see-through look comes from the veil and
 * blur amounts.
 */
private const val GLASS = """
uniform float2 size;
uniform float radius;
uniform float density;    // px per dp
uniform float4 tint;      // straight rgb, alpha
uniform float run;        // the reflection's head: how far it has come round the outline, in laps, clockwise from the top's middle
uniform float glow;       // how bright the reflection is, 0..1
uniform float tail;       // how long its tail is, in dp
uniform float dark;       // 0 light theme, 1 dark
uniform float solid;      // 1: no blur behind, so stay opaque
uniform float night;      // first run's welcome: 0 by day, 1 at night, when the outline is full white

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

    // The reflection: how far along the outline this pixel is, clockwise from the top's middle, following the
    // corners' arcs; and how much of the reflection is here. A short front and a tail behind it: something passing.
    float2 p = xy - size * 0.5;
    float2 h = size * 0.5;
    float2 c = h - radius;                       // the corners' centres are at (±c.x, ±c.y)
    float q = 1.5707963 * radius;                // a quarter turn
    float top = 2.0 * c.x;
    float side = 2.0 * c.y;
    float total = 2.0 * (top + side) + 4.0 * q;
    float2 k = abs(p) - c;
    float s;
    if (k.x > 0.0 && k.y > 0.0) {                // in a corner: by its angle
        if (p.x > 0.0 && p.y < 0.0) { s = c.x + atan(k.x, k.y) * radius; }
        else if (p.x > 0.0) { s = c.x + q + side + atan(k.y, k.x) * radius; }
        else if (p.y > 0.0) { s = c.x + 2.0 * q + side + top + atan(k.x, k.y) * radius; }
        else { s = c.x + 3.0 * q + 2.0 * side + top + atan(k.y, k.x) * radius; }
    } else if (abs(p.x) - h.x > abs(p.y) - h.y) {
        s = p.x > 0.0 ? c.x + q + (p.y + c.y) : c.x + 3.0 * q + side + top + (c.y - p.y);
    } else {
        s = p.y < 0.0 ? p.x : c.x + 2.0 * q + side + (c.x - p.x);
    }
    float lap = total / density;                 // dp
    float t = (run * total - s) / density;       // dp behind the head (negative: ahead of it)
    t = mod(t + 0.25 * lap, lap) - 0.25 * lap;   // the outline is a loop: the tail lies across the starting point too
    float shape = t < 0.0 ? exp(-t * t / (40.0 * 40.0)) : exp(-t * t / (tail * tail));
    float refl = glow * shape;

    // Then the white outline, 1.25 dp, the same all the way round: no light from above. Where the reflection is it
    // is full white and a little wider (more so in light theme, where it has less brightness to gain).
    float w = (1.25 + mix(0.75, 0.5, dark) * refl) * density;
    float line = smoothstep(0.6, 1.2, depth) * (1.0 - smoothstep(1.0 + w - 0.5, 1.0 + w + 0.5, depth));
    float base = mix(mix(0.80, 0.44, dark), 1.0, night);
    float la = line * mix(base, 1.0, refl);
    pm = mix(pm, float3(1.0), la);
    a = mix(a, 1.0, la);
    // Its inner edge is soft: a little of it over the next 3 dp of glass. Enough to soften the line, not a band of light.
    float spill = refl * exp(-max(depth - 1.0 - w, 0.0) / (3.0 * density)) * (1.0 - line) * mix(0.16, 0.12, dark);
    pm = pm + float3(spill) * (1.0 - pm);
    a = a + spill * (1.0 - a);

    return half4(half3(clamp(pm, 0.0, 1.0)) * aa, a * aa);
}
"""

/**
 * Draws the glass behind the content. [run] is how far round the outline the reflection has come, in laps
 * from the middle of the top edge; [glow] its brightness; [tail] the length of its tail in dp.
 */
fun Modifier.glass(
    tint: Color, radiusPx: Float, density: Float, run: () -> Float, glow: () -> Float, tail: () -> Float, dark: Boolean, solid: Boolean,
    /** First run's welcome: how far the veil has gone to night, and how far the outline has gone to full white, each 0 to 1 ([Night]). */
    veil: () -> Float = { 0f }, rim: () -> Float = { 0f },
): Modifier = composed {
    val shader = remember { RuntimeShader(GLASS) }
    drawWithCache {
        val brush = ShaderBrush(shader)
        onDrawBehind {
            // The veil as it stands in this frame: the theme's own by day, on its way to the night's while first run's welcome says so.
            val dusk = veil().coerceIn(0f, 1f)
            val veiled = if (dusk > 0f) lerp(tint, NIGHT.copy(alpha = NIGHT_VEIL), dusk) else tint
            shader.setFloatUniform("night", rim().coerceIn(0f, 1f))
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("radius", minOf(radiusPx, size.width / 2f, size.height / 2f))
            shader.setFloatUniform("density", density)
            shader.setFloatUniform("tint", veiled.red, veiled.green, veiled.blue, veiled.alpha)
            shader.setFloatUniform("run", run())
            shader.setFloatUniform("glow", glow())
            shader.setFloatUniform("tail", tail())
            shader.setFloatUniform("dark", if (dark) 1f else 0f)
            shader.setFloatUniform("solid", if (solid) 1f else 0f)
            drawRect(brush)
        }
    }
}
