package io.github.kuscher.booklight.overlay

import android.graphics.RuntimeShader
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush

/**
 * The panel's glass, as a shader over the system's window blur.
 *
 * Android blurs what is behind a window but does not let an app bend it, so this is not true
 * refraction. What gives glass its look here is drawn: a tint thin enough to see through, a bevel
 * along the rim that is bright where it faces the light and dark where it turns away (the way a
 * thick edge bends light), a faint prismatic fringe on the very edge, a lamp's glow falling from
 * the top (the book light), fine grain, and a light that follows the pointer.
 */
private const val GLASS = """
uniform float2 size;
uniform float radius;
uniform float4 tint;      // straight rgb, alpha
uniform float2 light;     // where the light is, in px
uniform float lightOn;    // 0..1: how much the pointer's light shows
uniform float sweep;      // the summon sweep's position along x + 0.6 y, in px
uniform float dark;       // 0 light theme, 1 dark
uniform float solid;      // 1: no blur behind, so stay opaque

float box(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}
float hash(float2 p) { return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453); }

half4 main(float2 xy) {
    float2 c = xy - size * 0.5;
    float2 h = size * 0.5;
    float d = box(c, h, radius);
    float aa = clamp(0.5 - d, 0.0, 1.0);
    if (aa <= 0.0) { return half4(0.0); }
    float2 n = normalize(float2(
        box(c + float2(1.0, 0.0), h, radius) - box(c - float2(1.0, 0.0), h, radius),
        box(c + float2(0.0, 1.0), h, radius) - box(c - float2(0.0, 1.0), h, radius)) + 0.00001);
    float depth = -d;
    float bevel = 1.0 - smoothstep(0.0, 13.0, depth);
    float rim = 1.0 - smoothstep(0.0, 1.5, depth);

    float key = dot(n, normalize(float2(-0.3, -1.0)));           // the lamp, above and a little left
    float facing = dot(n, normalize(light - xy + 0.001));
    float lit = max(key, 0.0) * 0.8 + max(facing, 0.0) * 0.55 * lightOn;
    float shade = max(-key, 0.0);

    float3 col = tint.rgb;
    float a = tint.a;

    float gx = (xy.x - light.x) / (size.x * 0.5);
    float glow = exp(-gx * gx * 1.4) * exp(-xy.y / 64.0);
    col += glow * mix(0.11, 0.07, dark);
    a += glow * 0.08;

    float pd = distance(xy, light);
    float spot = exp(-pd * pd / 42000.0) * lightOn;
    col += spot * mix(0.05, 0.045, dark);

    col += bevel * lit * mix(0.24, 0.15, dark);
    col -= bevel * shade * mix(0.07, 0.12, dark);
    a += bevel * (0.08 + 0.3 * lit);

    float3 prism = 0.5 + 0.5 * cos(atan(n.y, n.x) * 2.0 + float3(0.0, 2.094, 4.188));
    float rimLight = rim * (0.3 + 0.7 * clamp(lit, 0.0, 1.0));
    col = mix(col, float3(1.0), rimLight * mix(0.8, 0.5, dark));
    col += prism * rim * 0.09;
    a = max(a, rimLight * 0.95);
    a = max(a, rim * mix(0.35, 0.45, dark));

    float t = (xy.x + xy.y * 0.6 - sweep) / 70.0;
    float band = exp(-t * t);
    col += band * (bevel * 0.5 + 0.035);
    a += band * bevel * 0.3;

    col += (hash(xy) - 0.5) * 0.016;
    a = mix(clamp(a, 0.0, 1.0), 1.0, solid);
    return half4(half3(clamp(col, 0.0, 1.0)) * a * aa, a * aa);
}
"""

/**
 * Draws the glass behind the content. [light] is in pixels inside the panel; [lightOn] fades the
 * pointer's light in and out; [sweep] runs from before the left edge to past the right one when
 * the panel is summoned.
 */
fun Modifier.glass(
    tint: Color,
    radiusPx: Float,
    light: () -> Offset,
    lightOn: () -> Float,
    sweep: () -> Float,
    dark: Boolean,
    solid: Boolean,
): Modifier = composed {
    val shader = remember { RuntimeShader(GLASS) }
    drawWithCache {
        val brush = ShaderBrush(shader)
        onDrawBehind {
            val l = light()
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("radius", radiusPx)
            shader.setFloatUniform("tint", tint.red, tint.green, tint.blue, tint.alpha)
            shader.setFloatUniform("light", l.x, l.y)
            shader.setFloatUniform("lightOn", lightOn())
            shader.setFloatUniform("sweep", sweep())
            shader.setFloatUniform("dark", if (dark) 1f else 0f)
            shader.setFloatUniform("solid", if (solid) 1f else 0f)
            drawRect(brush)
        }
    }
}
