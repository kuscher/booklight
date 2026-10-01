#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Builds docs/design/booklight-plan.html (the published plan page with the interactive prototype)
from booklight-plan.src.html by inlining two pictures (docs/images/hero.png and dark.png) as data URIs.
  tools/plan_page.py            write the page
  tools/plan_page.py --preview  also write build/plan-preview.html with a document skeleton, for a local look
"""
import base64, pathlib, sys

root = pathlib.Path(__file__).resolve().parent.parent
design = root / "docs" / "design"
src = (design / "booklight-plan.src.html").read_text()
# The pictures are the README's scenes (tools/store_scenes.py), reduced to JPEG so the page stays small.
import io
from PIL import Image
for key, name in {"{{CAP1}}": "hero.png", "{{CAP2}}": "dark.png"}.items():
    buf = io.BytesIO()
    Image.open(root / "docs" / "images" / name).convert("RGB").resize((1200, 750), Image.LANCZOS).save(buf, "JPEG", quality=84)
    src = src.replace(key, "data:image/jpeg;base64," + base64.b64encode(buf.getvalue()).decode())
(design / "booklight-plan.html").write_text(src)
print(design / "booklight-plan.html", len(src) // 1024, "KB")
if "--preview" in sys.argv:
    out = root / "build" / "plan-preview.html"
    out.parent.mkdir(exist_ok=True)
    out.write_text('<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head><body>' + src + "</body></html>")
    print(out)
