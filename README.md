# Booklight

A keyboard launcher for Googlebooks. Press a key, a small glass panel appears over your desktop,
type a few letters, press Enter.

- **Apps first.** "chr" is Chrome, "gc" is Google Chrome, "snap" is StudioSnap.
- **It learns your shorthand.** What you pick for the text you typed comes first next time.
- **Sums, settings, the web.** `150 + 20%`, "dark theme", `github.com/kuscher`, or search for anything.
- **No permissions.** No internet access, nothing to allow. What it learns stays on the device.

Status: **0.1, in development.** Not released yet. The plan is in [docs/PLAN.md](docs/PLAN.md).

## Give it a key

Booklight opens whenever you start it, so any shortcut that opens an app works:

1. Open **Keyboard shortcuts** (the Action key + /).
2. Choose **App shortcuts**, then **Add shortcut**.
3. Find **Booklight**, choose **+**, press the keys you want (for example Action + K), then **Set shortcut**.

The same keys close the panel again.

## Keys

| Key | Does |
| --- | --- |
| ↑ ↓ | Move through the rows |
| Enter | Run the selected row |
| Tab | More actions for the row |
| Ctrl + 1…9 | Run that row |
| Esc | Close |

## Building

Android Studio's JDK 21 and the Android SDK (platform 37.0).

```bash
./bl test        # the search core's tests, on this machine
./bl app         # build, install and open on the attached Googlebook
```

`core/` is plain Kotlin (matching, ranking, learning, the calculator); `app/` is the Compose app.
See [CLAUDE.md](CLAUDE.md) for the map.

## Licence

MIT. A personal hobby project by Alexander Kuscher; not affiliated with or endorsed by any employer.
