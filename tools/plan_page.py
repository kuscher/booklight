#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Builds docs/design/booklight-plan.html (the published plan page with the interactive prototype)
from booklight-plan.src.html by inlining the device captures as data URIs.
  tools/plan_page.py            write the page
  tools/plan_page.py --preview  also write build/plan-preview.html with a document skeleton, for a local look
"""
import base64, pathlib, sys

root = pathlib.Path(__file__).resolve().parent.parent
design = root / "docs" / "design"
src = (design / "booklight-plan.src.html").read_text()
for key, name in {"{{CAP1}}": "panel-results.png", "{{CAP2}}": "panel-sum.png"}.items():
    data = base64.b64encode((design / "captures" / name).read_bytes()).decode()
    src = src.replace(key, "data:image/png;base64," + data)
(design / "booklight-plan.html").write_text(src)
print(design / "booklight-plan.html", len(src) // 1024, "KB")
if "--preview" in sys.argv:
    out = root / "build" / "plan-preview.html"
    out.parent.mkdir(exist_ok=True)
    out.write_text('<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head><body>' + src + "</body></html>")
    print(out)
