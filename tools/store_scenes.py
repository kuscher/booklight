#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Puts the panel's own-window captures (docs/design/captures/*.png, taken with `./bl shot`) on a
stand-in desktop, the way they look on a Googlebook: the desktop dimmed a little and blurred behind
the glass. Writes opaque 1600 x 1000 scenes to store-submission/graphics/scenes/, which
googlebook-tech's scripts/play/graphics.mjs frames into the Play screenshots, and docs/images/hero.png
for the README. No real screen content is used: the windows are drawn here.
"""
import pathlib
from PIL import Image, ImageDraw, ImageFilter

ROOT = pathlib.Path(__file__).resolve().parent.parent
CAPS = ROOT / "docs" / "design" / "captures"
OUT = ROOT / "store-submission" / "graphics" / "scenes"
W, H = 1600, 1000
PANEL = 810

def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def wallpaper(dark: bool) -> Image.Image:
    stops = [(43, 63, 120), (30, 39, 73), (23, 26, 44), (31, 24, 34)] if dark else [(157, 184, 245), (201, 214, 247), (233, 230, 246), (246, 233, 227)]
    im = Image.new("RGB", (W, H))
    px = im.load()
    for y in range(H):
        for x in range(W):
            d = min(1.0, (((x - W * 0.15) / (W * 1.2)) ** 2 + ((y - H * 0.1) / (H * 0.9)) ** 2) ** 0.5)
            t = d * 3
            i = min(2, int(t))
            px[x, y] = lerp(stops[i], stops[i + 1], t - i)
    return im

def window(d: ImageDraw.ImageDraw, box, dark: bool, accent):
    x0, y0, x1, y1 = box
    body, bar, line = ((35, 38, 52), (48, 52, 70), (58, 63, 82)) if dark else ((255, 255, 255), (228, 231, 241), (201, 206, 221))
    d.rounded_rectangle(box, 18, fill=body)
    d.rounded_rectangle((x0, y0, x1, y0 + 36), 18, fill=bar)
    d.rectangle((x0, y0 + 18, x1, y0 + 36), fill=bar)
    y = y0 + 64
    d.rounded_rectangle((x0 + 28, y, x0 + 28 + (x1 - x0) * 0.4, y + 20), 8, fill=line); y += 48
    for i, w in enumerate((0.86, 0.74, 0.5)):
        d.rounded_rectangle((x0 + 28, y, x0 + 28 + (x1 - x0 - 56) * w, y + 11), 5, fill=line); y += 28
    d.rounded_rectangle((x0 + 28, y + 6, x1 - 28, y + 120), 14, fill=accent); y += 150
    for w in (0.9, 0.6, 0.8, 0.84, 0.45, 0.7, 0.88, 0.5):
        if y > y1 - 30: break
        d.rounded_rectangle((x0 + 28, y, x0 + 28 + (x1 - x0 - 56) * w, y + 11), 5, fill=line); y += 28

def desktop(dark: bool) -> Image.Image:
    im = wallpaper(dark)
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    boxes = [(70, 110, 800, 820), (720, 190, 1530, 860)]
    for b in boxes: sd.rounded_rectangle((b[0], b[1] + 14, b[2], b[3] + 22), 18, fill=(10, 14, 40, 60))
    im.paste(shadow.filter(ImageFilter.GaussianBlur(22)), mask=shadow.filter(ImageFilter.GaussianBlur(22)).split()[3])
    d = ImageDraw.Draw(im)
    window(d, boxes[0], dark, (70, 92, 150) if dark else (171, 190, 235))
    window(d, boxes[1], dark, (96, 74, 128) if dark else (222, 200, 232))
    # The taskbar: a pill of app tiles.
    bar = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    bd = ImageDraw.Draw(bar)
    bd.rounded_rectangle((W / 2 - 170, H - 66, W / 2 + 170, H - 12), 24, fill=(20, 22, 34, 150) if dark else (255, 255, 255, 150))
    for i, c in enumerate([(92, 107, 192), (66, 133, 244), (234, 67, 53), (244, 119, 42), (38, 50, 56), (31, 164, 99)]):
        x = W / 2 - 150 + i * 52
        bd.rounded_rectangle((x, H - 57, x + 36, H - 21), 11, fill=c + (255,))
    im.paste(bar, mask=bar.split()[3])
    return im

def scene(capture: pathlib.Path, dark: bool) -> Image.Image:
    panel = Image.open(capture).convert("RGBA")
    # Captures come from whichever Googlebook was at hand (1.125 or 1.5 px per dp): bring them to one size, 720 dp = 810 px.
    if panel.width != PANEL: panel = panel.resize((PANEL, round(panel.height * PANEL / panel.width)), Image.LANCZOS)
    im = desktop(dark)   # no dim behind the panel: since 1.1 it is off unless switched on
    x, y = (W - panel.width) // 2, int(H * 0.16)
    # The glass: what is behind the panel, blurred, shows through the capture's own transparency.
    region = im.crop((x, y, x + panel.width, y + panel.height)).filter(ImageFilter.GaussianBlur(12))   # the Balanced glass: a 22 dp blur radius is a sigma of about 12 px here
    mask = Image.new("L", panel.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, panel.width - 1, panel.height - 1), 36, fill=255)
    im.paste(region, (x, y), mask)
    im.paste(panel, (x, y), panel)
    return im

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("*.png"): old.unlink()
    for name in sorted(c.stem for c in CAPS.glob("*.png")):
        scene(CAPS / f"{name}.png", dark=name == "dark").save(OUT / f"{name}.png")
        print(OUT / f"{name}.png")
    (ROOT / "docs" / "images").mkdir(parents=True, exist_ok=True)
    scene(CAPS / "apps.png", False).save(ROOT / "docs" / "images" / "hero.png")
    scene(CAPS / "dark.png", True).save(ROOT / "docs" / "images" / "dark.png")

if __name__ == "__main__":
    main()
