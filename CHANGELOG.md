# Changelog

## 1.1 (not yet released)

Several planned releases in one (Jot, Dials, Recipes, Switches), on a new way of acting on rows.

**Acting on a row**
- The selected row shows everything it can do as a row of icons. One is armed: it is unrolled to its name
  and the Enter mark. Tab and Shift + Tab, or → and ←, move the arming; the highlight glides and the names
  unroll with it. The separate actions view of 1.0 is gone.
- Typed verbs arm an action on an app's row: "chrome uninstall", "uninstall chrome", "chrome inf".
- Apps gain New window, Left half, Right half and Uninstall (Android asks before it removes anything).
- A keyword and a space, or Tab on its row, turns a keyword into a chip in the field: `yt`, then what to
  search for. Backspace on an empty field turns it back. The last row is always "Search Google for" all of it.
- Deleting something you made needs Enter twice.

**Jot**
- `mail anna@x.com Lunch? / See you at 1`: your mail app's compose window, filled in. Nothing is sent.
- `note buy milk`: a dated line in Notes.md, in a folder you choose once. Or Keep.
- `event Fri 3pm Dentist @ Main St`: the calendar's editor, filled in.
- `remind 5pm call bank`, `remind in 20m stretch`: a Clock alarm or timer with your text as its label; a
  calendar event when it is more than a day away. `timer 10m tea`, `alarm 7:30 gym`.
- `new file ideas.md`, `new folder Projects`, `new doc Q3 plan`.
- Ask Gemini: for a question or a longer text, the Gemini app opens with your text in its prompt. `ask …`.

**Dials**
- `vol`, `brightness`: a level you change with ← and → without the panel closing. `vol 40` sets it.
- `pause`, `next`, `previous`; `play lofi beats` asks your music app.
- `emoji party`, `sym arrow`: a grid to pick from, in English and German. `#3478f6` and `color …` in HEX,
  RGB, HSL and OKLCH. `qr …`. `password 20`, `uuid`.
- `gh owner/repo` and `gh owner/repo#12` go straight there; `:3000` opens localhost.

**Your own**
- Links with `{argument}`, `{clipboard}` and `{date}` (1.0's keyword searches still work), snippets
  (`snip sig`, `snip add sig / Best, Alex`) and recipes: several steps under one name.
- `clip`: what you copied, as rows (search it, ask Gemini, a note, a QR code, UPPERCASE and others).
- Text from other apps: "Booklight" in the text-selection menu and the share sheet.
- A search-pill widget, a Quick Settings tile, and Booklight as the digital assistant if you choose it.

**The panel**
- It unfolds from a line when it opens, at a speed you choose (or not at all), and a light runs once
  around its outline.
- Theme (auto, light, dark), colours (your wallpaper's or Booklight's own), and Solid besides Clear,
  Balanced and Frosted. The screen behind is no longer dimmed unless you turn that on.
- The app's icon opens a Booklight window: what it is, how to give it a key, every setting, and editors
  for your links, snippets and recipes. The keyboard shortcut opens the panel as before.
- The two answers of the first-run card are one strip with one gliding highlight.
- Fixed: started from the Apps list, the panel closed itself at once.

**Access.** Three more permissions, none of which prompts: uninstalling (Android shows its own dialog),
setting alarms and timers, and changing the brightness (off until you switch it on in Settings).
`play` now plays music; `store` searches the Play Store.

## 1.0 (1 October 2026)

The first release.

- A glass panel in the upper middle of the screen, opened by a keyboard shortcut for the app (Action +
  Alt + Space or Action + K are free on a Googlebook) and closed by the same keys.
- Apps from every profile: starts, later words, initials, loose letters. It learns what you pick for
  the text you typed.
- The top hit completes in the field. Tab lists a row's other actions (App info, Store page, Copy).
- Sums in place, about 30 settings pages by name or keyword, typed addresses, web search with a choice
  of engine, keyword searches (`yt lofi`) and your own.
- Search suggestions, off until you turn them on: with them on, what you type goes to your search
  engine.
- Flat glass in your wallpaper's colors with a white outline; Clear, Balanced or Frosted. The selection
  glides, lists cascade in, the actions view slides. Cuts when the system's animations are off.
- A first-run card under the field for the shortcut and for suggestions.
- English (US and British) and German.

## 0.1 (1 October 2026, not released)

The proof that it works on a Googlebook with no permissions.

- A glass panel in the upper middle of the screen, opened by the app's launcher activity, so a
  Googlebook keyboard shortcut (Action + a key of your choice) opens it and the same key closes it.
- Apps from every profile, with matching on starts, later words, initials and loose letters.
- It learns: what you pick for the text you typed comes first next time.
- Sums in place, about 30 system settings pages, typed addresses, and a web search row.
- Tab lists a row's other actions. Esc, a click outside or another window closes the panel.
