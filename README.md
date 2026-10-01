<img src="docs/images/icon.png" width="96" alt="">

# Booklight

A keyboard launcher for Googlebooks. Press a key, a small glass panel appears over your desktop, type a
few letters, press Enter.

![The Booklight panel over a desktop, listing apps for the letters "st"](docs/images/hero.png)

- **Apps first.** The start of a name, the start of any word in it, initials, or a few loose letters.
- **It learns your shorthand.** What you pick for the text you typed comes first next time.
- **A row shows what it can do.** Its actions sit on it as a row of icons; Tab moves along them, and the
  arrow at the end opens the rest as a list. "chrome top left" or "chrome uninstall" arms that one straight away.
- **Answers on this device.** `fix teh text`, `shorter …`, `de …`, `sum`, `explain …`, or a prompt of your
  own: the system's own model (Gemini Nano, where the device has it) writes the answer into the row.
- **Pin.** A note, a sum's answer, a colour, a QR code or a countdown in a small window that stays on top
  and never takes the keyboard.
- **`?` lists everything**, each with an example that Booklight types for you.
- **Jot things down.** `mail …`, `note …`, `event …`, `remind …`, `timer 10m tea`, `new file ideas.md`:
  one line, a preview of what was understood, Enter. Longer questions go to Gemini.
- **Small controls.** `vol`, `brightness`, `pause`; an emoji grid, colour values, QR codes, passwords.
- **Settings pages, sums and the web.** "dark", `150 + 20%`, a typed address, or a search. A keyword
  and a space search one site: `yt lofi`.
- **Your own commands.** Links with placeholders, snippets, prompts, and recipes that do several things at
  once. What your other apps offer shows as rows too: `new event`.
- **Glass and motion.** The panel unfolds, lets your desktop show through, and nothing in it pops:
  highlights glide, lists cascade in.
- **Private by default.** No account, no ads. Search suggestions are off until you turn them on. What
  you ask the on-device model stays on the device; the Google library that reaches it reports how it is
  used (not your text). [Privacy](PRIVACY.md).

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
| Enter | Run the armed action of the selected row |
| Tab, Shift + Tab | The row's next or previous action; on a keyword (`s`, `yt`) or its row: make it the chip |
| → at the end of the text, ← | The same; on a level: up and down; in a grid: the next cell |
| Space after a keyword | The keyword becomes a chip; Backspace on an empty field undoes it |
| `?` first | The list of everything Booklight does |
| Shift + Enter | Run and keep the panel open |
| Ctrl + 1…9 | Run that row |
| ↑ on an empty field | The last text you didn't run |
| Esc, a click outside, your shortcut | Close |

## What you can type

| Type | Get |
| --- | --- |
| `chr`, `chrome right third`, `chrome uninstall` | An app, where to open it, and what to do with it |
| `fix teh text` · `sum` · `de good morning` · `explain idempotent` | An answer from the model on this device |
| `pin gate B22, 14:05` · `timer 10m tea`, Start and pin | A small window that stays on top |
| `s wifi` · `k snap` · `new event` | A settings page, a keyboard shortcut, something another app offers |
| `notes milk` · `todo call the bank` | A line of your notes; your tasks |
| `mail anna@x.com Lunch? / See you at 1` | A filled-in compose window |
| `note buy milk` · `event Fri 3pm Dentist` · `remind 5pm call bank` | A note, an event, a reminder |
| `timer 10m tea` · `alarm 7:30` · `new file ideas.md` | A timer, an alarm, a new file |
| `vol 40` · `brightness` · `pause` · `play lofi` | Controls |
| `emoji party` · `sym arrow` · `#3478f6` · `qr …` · `password 20` | Things to copy |
| `yt lofi` · `gh owner/repo` · `:3000` · `ask …` | The web, GitHub, localhost, Gemini |
| `snip sig` · `clip` · your own keywords | Your snippets, your clipboard, your links and recipes |

## The Booklight window

The app's icon opens a window with everything else, in seven sections: Start (what Booklight is, your key,
tips), Commands (everything it does; Enter on a row types the example into the panel), Yours (links,
snippets, recipes, prompts), Look (theme, colours, glass, shadow, opening), Results (the search engine,
suggestions, which kinds of rows show, other apps' commands), Access (the notes folder, brightness, the
assistant key) and About. "Booklight settings" in the panel opens it too.
A search-pill widget and a Quick Settings tile open the panel without a keyboard.

## Building

Android Studio's JDK 21 and the Android SDK (platform 37.0).

```bash
./bl test        # the search core's tests, on this machine
./bl app         # build, install and open on the attached Googlebook
```

`core/` is plain Kotlin (matching, ranking, learning, the calculator, the parsers for dates, mail and the rest); `app/` is the Compose app. See
[CLAUDE.md](CLAUDE.md) for the map, [docs/PLAN.md](docs/PLAN.md) for where it is going and
[docs/RELEASING.md](docs/RELEASING.md) for releases.

## License

MIT. A personal hobby project by Alexander Kuscher; not affiliated with or endorsed by any employer or
by Google.
