# Booklight polish: visual review (plan sections 6, 4, 3)

1 October 2026. Review only: nothing in the repo, on a device or in git was touched. All numbers are from
the Lenovo captures in the parent folder (1.5 px per dp) unless marked *computed*. Contrast is WCAG; "dL*"
is the difference in CIE lightness between the selection and the bare glass beside it.

Files in this folder

| File | What it shows |
| --- | --- |
| `pill-alpha-steps-b.png`, `pill-alpha-steps-a.png` | secondaryContainer at 0.42, 0.46, 0.50, 0.54, 0.66, made from captures 0 and 1 (every pixel is linear in the fill's alpha, so these are exact) |
| `strip-order-mock.png` | the strip in three orders, dark and light, pasted together from the captures |
| `crop-strips-3x.png`, `crop-list-2x.png`, `crop-place-glyphs-8x.png` | the strip, the list and its glyphs enlarged |
| `crop-reflection-dark.png`, `crop-reflection-light.png`, `crop-lap-right.png`, `crop-lap-left.png` | the reflection's flaws enlarged |
| `reflection-sim-dark.png`, `reflection-sim-light.png` | as built against the proposed values at four moments (a software copy of the shader over the resting capture; it reproduces the built flaws) |
| `measure*.py`, `synth.py`, `reflect_sim.py`, `reflect_sheet.py` | the scripts behind the numbers and pictures |

## 1. The selection in dark theme: candidate 1, with three things that follow

**Verdict: `secondaryContainer` at 0.42 (candidate 1 as built), on glass only.**

Measured, pill on the Cast row (`pill-dark-N-b.png`); bare glass is #707070 over the white page and
#0d1310 over the terminal; the ink is #ebe3ef:

| | over white: pill, dL*, title | over dark: pill, dL*, title | 0.80 ink, white / dark |
| --- | --- | --- | --- |
| 0 today (0.66) | #504b58, -14, 6.74:1 | #2e2b37, +13, 11.05:1 | 4.97 / 7.64 |
| **1 (0.42)** | **#5c5861, -9, 5.55:1** | **#23222a, +8, 12.57:1** | **4.21 / 8.50** |
| 2 half neutral | #56535a, -11, 6.03:1 | #202026, +7, 12.94:1 | 4.53 / 8.72 |
| 3 white + secondary | #838284, +7, 3.05:1 | #303434, +16, 10.06:1 | 2.51 / 7.06 |
| 4 white + primary | #88878b, +9, 2.85:1 | #383c3e, +20, 8.90:1 | 2.37 / 6.33 |

The same title on bare glass over the white page is 3.95:1.

- **3 and 4 are out.** Over a white window they lighten grey glass under light text: the selected row's
  title falls to 3.05:1 and 2.85:1, under the 3.95:1 of the rows around it. The row that matters most
  becomes the faintest. Over a dark window they are starker than today (+16 and +20 against +13), the
  opposite of the request, and they carry no colour (#303434 and #383c3e take their tint from the terminal).
- **2 is out.** No more see-through than 1, weaker over a dark window (+7), and the colour is gone
  (#202026): "not devoid" fails.
- **1** shows 58 % of what is behind it (the page's lines and the page-to-terminal step read through it),
  keeps about two thirds of today's colour, and is the same step from the glass on both sides (-9, +8).
  Every text on it gains or keeps contrast: title 5.55:1 over white, 12.57:1 over dark; resting strip icons
  12.8:1 (dark side, measured); the armed name on its pane 14.3:1 (dark side, measured) and 8.5:1 (white
  side, *computed*); 0.80 ink (slot labels, guessed values) 4.21:1 and 8.50:1, above the 3:1 target.
  The dimmer right-hand kind label of unselected rows is not affected: 9.95:1 over dark, 3.15:1 over white.
- If on the device it is too quiet over a dark window, the next step is 0.50 (-11 / +10, title 5.96:1 and
  12.09:1). Not below 0.42. 0.46 is not worth building: 0.5 L* from 0.42.

**What follows**

| Thing | Verdict |
| --- | --- |
| Pill's rim | Keep: white 0.30, 1 px. It now carries the shape over a dark window; it stays under the panel's 0.44. |
| Armed pane in the strip | Keep: `surfaceContainerLowest` 0.36, rim 0.30. On candidate 1 it is #17171b on #222028 (dL* -4.9): still the darkest thing on the row, held by its rim. |
| Emoji grid square, first-run card's strip | Follow through `selectionFill`: same 0.42, same rim. No change of their own. |
| Red pill (list's Uninstall line) | `errorContainer` 0.70 -> **0.50** in dark, on glass. *Computed* with the baseline error roles: `onErrorContainer` 6.3:1 over white, 11.7:1 over dark. The red pane inside the strip stays 0.70: it is small and must not be missed. |
| Solid ground | **Stays 0.66 / red 0.70.** The Booklight window's page pill (`MainActivity.kt:270`) and the panel with glass set to Solid have nothing behind them to show: at 0.42 the pill is only about 3 L* from `surfaceContainerHigh` (5 today, *computed*). `selectionFill(scheme, dark, glass)`: `dark && glass` -> 0.42, `dark && !glass` -> 0.66. |
| Hover | Nothing: hover moves the one pill. Pressed stays ink 0.08. |
| Light theme | **No change.** Over a dark window the dense pill (#d1c8dc) is what gives the selected title 7.9:1 where bare glass gives 3.04:1; over a white page it is 2 L* from the glass and told by its hue. |
| Docs | design-system.md section 2 "pill": 0.78 / 0.42 on glass, 0.66 on solid ground. |

## 2. The app row

**Order: Open, New window, App info, Maximise, Left half, Right half, arrow** (`strip-order-mock.png`, A).
In `AppsProvider.spots` move `full` before `left`.

- **App info stays, third.** Its circle is the only round glyph: it separates New window's frame from the
  three place frames. Moved to the end (mock B) four identical frames stand in a row and New window reads
  as a fourth place; and the arrow should follow the places, because ten of its eleven lines are more places.
- **Name: "Maximise".** It is what the action does (the area the system's bars leave free, not full
  screen), it is Alex's word, and `keys_table.xml` already says "Maximise or restore window". Typed words
  stay (full, maximize, maximise, max). German: "Maximieren", a verb like "Öffnen".
- **The list, as built:** Left third, Middle third, Right third, Left two thirds, Right two thirds,
  Top left, Top right, Bottom left, Bottom right, Centre, then 8 dp, Uninstall. Left to right, top to
  bottom, tiles before the floating one. No headers, rules or extra gaps: the glyphs group them, and the
  one gap must keep meaning "this removes something".
- **Rename "Left ⅔", "Right ⅔" to "Left two thirds", "Right two thirds"** (English). They stand under
  "Left third"; the fraction's digits are about 5 sp tall. The 12-character limit was for nine icons.

**Alignment: approved.** Measured in `strip-dark-all.png`:

- Pill 84 px (56 dp), pane 48 px (32 dp), both centred on y = 186; every resting glyph's centre is within
  0.5 px of it.
- Icon centres 810.5, 859.5, 908.5, 957.5, 1006.5, 1055.5: an even 49 px. (That is 32.67 dp, not 32: 7 dp
  rounds up to 11 px twice. Harmless.)
- In the pane: 12 dp before the icon's ink, 11.3 dp after the mark's.
- List glyph column centre 87.5 px, the app icon's 87.0; names and title both start at 138 px; the Enter
  mark's centre 1056.0, the arrow's 1056.5.
- The solid Maximise glyph is the heaviest mark on the row. Approved: it has the silhouette of the two
  halves, so the three read as a set (full, left, right).

## 3. The white reflection: change

On the straight top edge over a dark window it already reads as light on the pane's edge. Five things do
not hold; the first two are drawing errors.

1. **A hard cut at the top's middle**, at the start and again at the end (`crop-lap-right.png` first tile,
   `crop-lap-left.png` last tiles; also in `pill-dark-0.png`). The tail is clipped where the lap begins, so
   the line and the light under it start as a block with a vertical edge: a stroke being drawn.
   Change: the shape is a loop along the outline; coming and going is the fade alone.
2. **A diagonal crease in each corner** while the head passes (`crop-reflection-dark.png` third tile,
   `crop-lap-right.png` fourth). The distance along the outline is measured along the bounding rectangle
   and jumps by up to 18.7 dp at the corner's diagonal, half of the 36 dp front.
   Change: measure it along the rounded outline.
3. **The light inside the edge** (7 dp, 22 / 30 %) is a glow on the surface: a soft halo inside the caps,
   a fog band under the top edge over the white page, a moulded rim in light theme
   (`crop-reflection-light.png`). Outside "flat glass". Change: 3 dp, 0.12 dark / 0.16 light: enough to
   soften the line's inner edge, not enough to be a band.
4. **The widening** (1.25 -> 2.5 dp) reads as a bold stroke on the caps. Change: to 1.75 dp in dark, 2.0 dp
   in light (light has less to gain in brightness, 80 -> 100 %, so width carries more of it). The line
   still goes to full white.
5. **The tail** (visible for about 400 dp, over half the top edge) is a streak. Change: front 40, tail 140
   (about 270 dp visible).

**Light theme over a white page: accept that it is nearly invisible.** A white reflection has nothing to
show on white. No dark or coloured stand-in.

Shader, in place of the `s` / `around` / `t` / `shape` / `w` / `spill` lines of `Glass.kt` (`run` becomes
the share of the lap, so `Modifier.glass` sets `shader.setFloatUniform("run", run())`):

```
    // The reflection: how far along the outline this pixel is, clockwise from the top's middle, following the corners' arcs.
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
        if (p.x > 0.0 && p.y < 0.0) s = c.x + atan(k.x, k.y) * radius;
        else if (p.x > 0.0)         s = c.x + q + side + atan(k.y, k.x) * radius;
        else if (p.y > 0.0)         s = c.x + 2.0 * q + side + top + atan(k.x, k.y) * radius;
        else                        s = c.x + 3.0 * q + 2.0 * side + top + atan(k.y, k.x) * radius;
    } else if (abs(p.x) - h.x > abs(p.y) - h.y) {
        s = p.x > 0.0 ? c.x + q + (p.y + c.y) : c.x + 3.0 * q + side + top + (c.y - p.y);
    } else {
        s = p.y < 0.0 ? p.x : c.x + 2.0 * q + side + (c.x - p.x);
    }
    float lap = total / density;                 // dp
    float t = (run * total - s) / density;       // dp behind the head (negative: ahead of it)
    t = mod(t + 0.25 * lap, lap) - 0.25 * lap;   // the outline is a loop: the tail lies across the starting point too
    float shape = t < 0.0 ? exp(-t * t / (40.0 * 40.0)) : exp(-t * t / (140.0 * 140.0));
    float refl = glow * shape;

    float w = (1.25 + mix(0.75, 0.5, dark) * refl) * density;
    // line, base, la: unchanged
    float spill = refl * exp(-max(depth - 1.0 - w, 0.0) / (3.0 * density)) * (1.0 - line) * mix(0.16, 0.12, dark);
```

`reflect_sim.py` uses these formulas; `reflection-sim-dark.png` and `-light.png` show them against the
built ones. The timing, the curve and the fade at each end are the motion designer's.

## 4. Other things in the captures

1. **Dark theme, the opened list over a white page:** the ten names are second ink, 3.15:1, the faintest
   text in the panel, and they are what is chosen. Change `ActionRow`: name at ink 1.0 (3.95:1), glyph
   stays 0.80. The selected line still differs by pill and mark. Unselected "Uninstall" in `error` is
   2.90:1 there; leave it.
2. **Light theme over a dark window** (not from this change; glass levels are out of scope, so for the
   separate pass): "Settings" 2.48:1, footer "Close" 2.76:1, "esc" on its cap 2.25:1. Full ink would only
   reach 3.04:1, so it needs the veil, not the ink.
3. **`pill-dark-0.png` and `pill-dark-3.png`** caught the reflection mid-lap; `0` shows the hard start of
   item 3.1 in an ordinary capture. Retake them after the shader change if they go into the docs.

## Round two: the built version (captures in `../../polish2/`)

All three approved as built. Measured from `row-dark*.png`, `row-light*.png` and the lap's frames.

**Selection.** Dark pill on glass: #5c5861 over the white page (dL* -9.2), #23222a over the terminal (+8.3);
title 5.55:1 and 12.57:1, as candidate 1. Over the orange photo (`refl-list-dark-real`, half size): about
#64482e on glass #7d5a19, dL* -8 to -9, ink 6.5 to 6.9:1, so the middle case holds too. Red pill at 0.50:
#7d4643 over white (-11.0), #4c1713 over dark (+12.1); "Uninstall" on it 5.84:1 and 11.47:1. It is a
stronger step than the neutral pill, which is right for the one thing that removes something. Light theme
unchanged (pill #ebe1f5, red #f7e0de with 12.9:1). `LocalGlass` defaults to false, so the Booklight window
keeps 0.66.

**Row and list.** Order and names as specified (`r2-strip-dark-2x.png`). Glyph centres 810.5 to 1055.5 at an
even 49 px, all within 0.5 px of the pill's centre line, in both themes. List names at full ink: 3.95:1 over
the white page (glyphs 3.15:1); list glyph column 87.5 px, Enter mark 1056.0 against the arrow's 1056.5.

**Reflection** (`r2-dark-start.png`, `r2-dark-rightcap.png`, `r2-dark-leftcap.png`, `r2-dark-end.png`, the
same four with `light`, `r2-list-dark-lap.png`). Born as a small even glint just right of the top's middle,
stretching as it gathers speed; no cut at the start or the end; no crease in any corner over the orange
photo or the white page; no band of light inside the edge; the widened line is a clean rim on the caps in
both themes. The growing tail (36 to 140 dp) is fine as a picture: no still shows more than a short stretch
of one edge lit. Light theme over the white page: invisible, as accepted.

Left to do, not visual: `design-system.md` section 2 still gives the pill as 0.78 / 0.66.

Scripts: `round2-measure.py` (run from `polish2/`), `round2-sheet.py`, `measure_strip_fns.py`.
