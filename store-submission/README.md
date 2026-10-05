# Store submission kit

Everything Google Play asks for, in the layout the other Googlebook apps use.

- `listing/en-US/` and `listing/de-DE/`: title (30), short description (80), full description (4000),
  release notes (500). Check lengths with `wc -m`. The release workflow uploads
  `listing/en-US/release-notes.txt` with each bundle.
- `graphics/icon-512.png`: from `tools/logo.py`.
- `graphics/scenes/`: the panel's own-window captures (`docs/design/captures/`, taken with `./bl shot` on
  the HP with a debug build) on a drawn stand-in desktop, from `tools/store_scenes.py`. No real screen
  content is in them.
- `graphics/spec.json` → `graphics/feature-graphic.png` and `graphics/screens/*.png` (1920 × 1080, no
  alpha), made by googlebook-tech's `scripts/play/graphics.mjs`:
  `node ~/googlebook-tech/scripts/play/graphics.mjs store-submission/graphics/spec.json store-submission/graphics`
- `forms/`: the answers for App content and Data safety. `forms/data-safety.md` is the source for the
  privacy page (googlebook.studio/privacy/booklight) too: the question in the app's first steps, that file
  and the page must say the same thing.

Play app id 4972005003444967962 (Fika Labs). App signing and upload key: the Booklight release key
(SHA-256 `61:30:F1:F9:…:90:A7:F6`). Closed testing only for now.
