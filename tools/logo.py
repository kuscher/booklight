#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Draws Booklight's icon as PNGs: store-submission/graphics/icon-512.png (Play's listing icon,
full bleed) and docs/images/icon.png (rounded, for the README). The app's own icon is the vector in
res/drawable/ic_launcher_*.xml; keep the two pictures the same.

A book light: the lamp's head, which is also a search field with its lens, and the light falling from it.
"""
import pathlib
from PIL import Image, ImageDraw, ImageFilter

ROOT = pathlib.Path(__file__).resolve().parent.parent
INK, LAMP = (23, 26, 43), (255, 209, 92)
S = 2048                      # drawn large, then reduced
U = S / 108 * 1.32            # the adaptive icon's 108 units, enlarged: a store icon has no safe-zone margin
OX, OY = S / 2 - 54 * U, S / 2 - 57 * U

def p(x, y): return (OX + x * U, OY + y * U)

def draw() -> Image.Image:
    im = Image.new("RGB", (S, S), INK)
    # A faint pool of light on the ground, so the ink isn't flat.
    glow = Image.new("L", (S, S), 0)
    ImageDraw.Draw(glow).ellipse([*p(14, 60), *p(94, 104)], fill=70)
    im.paste(Image.new("RGB", (S, S), (48, 52, 84)), mask=glow.filter(ImageFilter.GaussianBlur(S / 9)))
    # The beam: brightest at the lamp, gone at the bottom.
    beam = Image.new("L", (S, S), 0)
    ImageDraw.Draw(beam).polygon([p(40, 48), p(68, 48), p(82, 80), p(26, 80)], fill=255)
    fade = Image.new("L", (S, S), 0)
    top, bottom = p(0, 48)[1], p(0, 80)[1]
    fd = ImageDraw.Draw(fade)
    for y in range(int(top), int(bottom)):
        fd.line([(0, y), (S, y)], fill=int(166 * (1 - (y - top) / (bottom - top))))
    beam = Image.composite(fade, Image.new("L", (S, S), 0), beam)
    im.paste(Image.new("RGB", (S, S), LAMP), mask=beam)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([*p(33, 34), *p(75, 50)], radius=8 * U, fill=LAMP)
    d.ellipse([*p(40, 39), *p(46, 45)], fill=INK)
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
