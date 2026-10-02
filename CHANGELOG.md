# Changelog

## On main, after 2.1 (not released)

Alex's notes on 2.1, each taken through a visual and a motion designer (`docs/design/design-system.md` §12).

- **The opening is at Medium speed** unless chosen otherwise; whoever had Fast stored (everybody) gets Medium once.
- **The opening's curve**: the glass opens slowly, quickly through the middle, and lands over a long stretch, with
  no rebound. Leaving takes 155 ms at Fast and Medium alike.
- **The glass is frosted from its first frame.** The blur used to arrive at the end of the opening, all at once.
  It now belongs to the glass: there from the seam on, the same shape as the glass in every frame, and with it
  until the seam is gone when the panel leaves.
- **The seam the panel opens out of is seen again**: since 2.0's shadow nothing showed until the glass widened.
- **The light on the edge is a white reflection**: it comes 2.4 s after the panel has opened, in a quiet moment,
  runs once round from the middle of the top edge, and gives way to any key. The model's "thinking" light is the
  same light, steady and dimmer.
- **An app's row has six icons**: Open, New window, App info, Maximise, Left half, Right half; the other places
  are in the list under the arrow. "Full" is "Maximise"; "Left ⅔" is "Left two thirds".
- **The selection in dark theme is see-through** (the same colour at 42 % on glass).
- Found, not changed: Chrome's "New tab" is closed to other apps (`docs/research/device-findings.md`).

## 2.1 (1 October 2026)

- **Letters** (`abc`): the letters of sixteen languages in the picker's grid, to copy. By language
  (`abc german`, `abc dansk`; the start of the name is enough, in English, German or the language's own name),
  by plain letter (`abc a`, `abc A` for capitals), by the two letters it is written with (`abc ss`, `abc ae`),
  by its mark (`abc umlaut`, `abc acute`, `abc o slash`), or the letter itself for its other case. Nothing
  typed: the ones picked lately, then every small letter. Its row is also found by `umlaut`, `accent`,
  `letters`. `core/Letters.kt`; marks and plain letters come from Unicode's decomposition, not a hand-kept list.
- **The calculator reads a decimal comma.** Where the system's language writes one (German, Danish, French…)
  `1,5` is one and a half, `1.000` a thousand, and answers are written and copied that way. In any language the
  other sign is understood where it can only mean one thing (`12*3,5` in English, `12*3.5` and `0.125` in
  German), and with both signs in a number the last one is the decimal sign. The German examples use a comma again.

## 2.0 (1 October 2026)

What Alex chose from `docs/research/next-features.md`, plus a shadow, tips, `s` and `k`, and answers from the
device's own model. Spec: `docs/superpowers/specs/2026-10-01-booklight-2.0-design.md`.

**Prompts, answered on this device**
- A prompt is a name, a keyword and a text. Five to start with (`fix`, `shorter`, `de` or `en`, `sum`,
  `explain`); your own in the window under Yours. `{text}` says where the text goes.
- In a prompt's chip the row shows the text, and after a pause in typing the answer of the system's own model
  (Gemini Nano through AICore, where the device has it) is written into the row; the row grows once, to four
  lines. Enter asks at once, and again copies. Pin and Open in Gemini beside it; Replace when the text came
  from a field that can be edited. Nothing typed: what you copied.
- Without the model the row hands over to the Gemini app; where the system can fetch the model, a row asks it to.
- The model can be wrong: its rows never run anything.

**Pin**
- A small window that stays on top: a line of text, a sum's answer, a colour, a QR code, a countdown.
  `pin TEXT`; Pin on those rows; Start and pin on a timer. `pin` alone shows it, with Unpin and Copy.
- It is a picture-in-picture window, so it never takes the keyboard (a window in Android 17's pinned layer
  does, each time the panel closes: `docs/research/device-findings.md`). No permission.

**Finding what Booklight does**
- `?` first is the list of everything, with an example for each. Enter on a keyword's row makes it the chip;
  on any other row Booklight types the example, a letter at a time.
- Tips under the empty field: only after the panel has opened and nothing was typed for a moment. Tab arms
  Try it, then Turn off tips. One pass.
- The window's Commands page is the same list; Enter opens the panel and types the example.

**Apps and places**
- Left, middle and right third, the two thirds, the four quarters, centre and full. "… on display 2".
- A row shows nine actions; the arrow (More) opens the rest as a list under it. A typed place that is among the
  rest takes the arrow's slot. Store page is gone.
- Other apps' commands: their manifest shortcuts and a small file of their own (`docs/EXTENSIONS.md`).

**Keywords**
- `s`: settings pages (49). `k`: the system's keyboard shortcuts (43), shown as key caps.
- Tab on a keyword makes it the chip. The row of a one-letter keyword keeps the last local place.

**Notes**
- `notes TEXT` finds lines; `todo` adds, lists and ticks tasks in Todo.md; `note FILE text` writes to another
  file of the folder; Today's note.

**The window, and the look**
- A column of sections and one page. "No key yet" until a key has opened the panel.
- A shadow around the panel: off, low, medium (the default), high. Never under the glass.
- The closing draws in to the field's centre line; the footer rides the window's lower edge.
- Two old flaws gone: a list that is emptied fades (it was cut in one frame), and a panel that shrinks no
  longer shows a strip of blur without glass under it.

**Under the hood**
- `com.google.mlkit:genai-prompt` 1.0.0-beta4, as shipped: two install-time permissions and Google's usage
  reporting (not the text). `PRIVACY.md` and the data-safety note say what.
- Settings schema 3: prompts are seeded; whoever used 1.1 is taken to have a key.

## 1.1.1 (1 October 2026)

- The panel closes the way it opens, backwards: the glass folds to a line from both sides, and the line draws
  in. At the speed chosen for the opening; with the opening turned off it fades as before.
- The key pressed again while the panel is closing opens it again from where it had got to.

## 1.1 (1 October 2026)

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
