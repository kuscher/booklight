<img src="docs/images/icon.png" width="96" alt="">

# Booklight

A keyboard launcher for Googlebooks. Press a key, a small glass panel appears over your desktop, type a
few letters, press Enter.

![The Booklight panel over a desktop, listing apps for the letters "st"](docs/images/hero.png)

- **Apps first.** The start of a name, the start of any word in it, initials, or a few loose letters.
- **It learns your shorthand.** What you pick for the text you typed comes first next time.
- **Settings pages, sums and the web.** "dark", `150 + 20%`, a typed address, or a search. A keyword
  searches one site: `yt lofi`.
- **More per row.** Tab lists what else a row can do.
- **Glass and motion.** The panel takes your wallpaper's colors and lets your desktop show through. The
  selection glides between rows and lists cascade in.
- **Private by default.** No account, no ads, no analytics. Search suggestions are off until you turn
  them on. [Privacy](PRIVACY.md).

## Install

**[Download Booklight.apk](https://github.com/kuscher/booklight/releases/latest/download/Booklight.apk)**,
open it from Files and, if Android asks, allow Files to install apps. Made for Googlebooks (Android 14 or
newer); on Google Play it is offered to Googlebooks only.

## Give it a key

Booklight opens whenever you start it, so a keyboard shortcut for the app is all it needs:

1. Open **Keyboard shortcuts** (Action + /).
2. Choose **App shortcuts**, then **Add shortcut**.
3. Find **Booklight**, choose **+**, press your keys, then **Set shortcut**.

The system wants the Action key in every shortcut. **Action + Alt + Space** and **Action + K** are both
free. The same keys close the panel again. Booklight shows these steps the first time you open it.

## Keys

| Key | Does |
| --- | --- |
| ↑ ↓ | Move through the rows |
| Enter | Run the selected row |
| Tab, or → at the end of the text | More actions for the row |
| Ctrl + 1…9 | Run that row |
| Esc, a click outside, your shortcut | Close |

## Settings

Type "Booklight settings" in the panel: the search engine (Google, DuckDuckGo, Bing, Brave Search,
Ecosia), search suggestions, your own keyword searches, how see-through the glass is, and "Forget
everything".

## Building

Android Studio's JDK 21 and the Android SDK (platform 37.0).

```bash
./bl test        # the search core's tests, on this machine
./bl app         # build, install and open on the attached Googlebook
```

`core/` is plain Kotlin (matching, ranking, learning, the calculator); `app/` is the Compose app. See
[CLAUDE.md](CLAUDE.md) for the map, [docs/PLAN.md](docs/PLAN.md) for where it is going and
[docs/RELEASING.md](docs/RELEASING.md) for releases.

## License

MIT. A personal hobby project by Alexander Kuscher; not affiliated with or endorsed by any employer or
by Google.
