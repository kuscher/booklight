#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Draws Booklight's icon as PNGs: store-submission/graphics/icon-512.png (Play's listing icon,
full bleed) and docs/images/icon.png (rounded, for the README). The app's own icon is the vector in
res/drawable/ic_launcher_*.xml: the two paths below are the same as there; keep them the same.

"Beam": the launcher's field as a lamp's head (a white pill with the text caret cut out of it), and
one solid beam falling from it. Flat: no gradient, no glow.
"""
import math
import pathlib
from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parent.parent
BASE, WHITE, YELLOW = "#4B3CCF", "#FFFFFF", "#FFC83A"
# In the adaptive icon's grid of 108 units, of which a launcher shows 18..90.
HEAD = "M43.5 34.4 L64.5 34.4 A6.5 6.5 0 0 1 64.5 47.4 L43.5 47.4 A6.5 6.5 0 0 1 43.5 34.4 Z"
CARET = "M44.4 37.8 A1 1 0 0 1 45.4 36.8 L46.6 36.8 A1 1 0 0 1 47.6 37.8 L47.6 44 A1 1 0 0 1 46.6 45 L45.4 45 A1 1 0 0 1 44.4 44 Z"
BEAM = "M43.1 54.34 A3.5 3.5 0 0 1 46.32 52.2 L61.68 52.2 A3.5 3.5 0 0 1 64.9 54.34 L69.52 65.3 A4.25 4.25 0 0 1 65.6 71.2 L42.4 71.2 A4.25 4.25 0 0 1 38.48 65.3 Z"

S = 2048                      # drawn large, then reduced
VIEW0, VIEW = 18.0, 72.0      # the part of the grid a launcher shows: a store icon is full bleed
U = S / VIEW

def outline(d: str) -> list:
    """The points of a closed path of lines and circular arcs (M, L, A with no large arc, Z), in pixels."""
    t, pts, cur, i = d.split(), [], None, 0
    while i < len(t):
        if t[i][0] in "ML":
            cur = (float(t[i][1:]), float(t[i + 1])); pts.append(cur); i += 2
        elif t[i][0] == "A":
            r, sweep, x, y = float(t[i][1:]), t[i + 4] == "1", float(t[i + 5]), float(t[i + 6])
            (x0, y0), dx, dy = cur, x - cur[0], y - cur[1]
            chord = math.hypot(dx, dy)
            h = math.sqrt(max(r * r - chord * chord / 4, 0))
            s = 1 if sweep else -1
            cx, cy = (x0 + x) / 2 - s * dy / chord * h, (y0 + y) / 2 + s * dx / chord * h
            a0, a1 = math.atan2(y0 - cy, x0 - cx), math.atan2(y - cy, x - cx)
            if sweep and a1 < a0: a1 += 2 * math.pi
            if not sweep and a1 > a0: a1 -= 2 * math.pi
            steps = max(8, int(abs(a1 - a0) * r * U / 6))
            pts += [(cx + r * math.cos(a0 + (a1 - a0) * k / steps), cy + r * math.sin(a0 + (a1 - a0) * k / steps)) for k in range(1, steps + 1)]
            cur = (x, y); i += 7
        else:
            i += 1            # Z
    return [((x - VIEW0) * U, (y - VIEW0) * U) for x, y in pts]

def draw() -> Image.Image:
    im = Image.new("RGB", (S, S), BASE)
    d = ImageDraw.Draw(im)
    d.polygon(outline(HEAD), fill=WHITE)
    d.polygon(outline(CARET), fill=BASE)      # the caret is a hole: the base shows through
    d.polygon(outline(BEAM), fill=YELLOW)
    return im

def main():
    big = draw()
    store = ROOT / "store-submission" / "graphics"; store.mkdir(parents=True, exist_ok=True)
    big.resize((512, 512), Image.LANCZOS).save(store / "icon-512.png")
    docs = ROOT / "docs" / "images"; docs.mkdir(parents=True, exist_ok=True)
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S, S], radius=S * 0.23, fill=255)
    rounded = Image.new("RGBA", (S, S), (0, 0, 0, 0)); rounded.paste(big, mask=mask)
    rounded.resize((256, 256), Image.LANCZOS).save(docs / "icon.png")
    print(store / "icon-512.png", docs / "icon.png")

if __name__ == "__main__":
    main()
