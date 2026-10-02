# Changelog

## Unreleased

**The copy** (M1 of `docs/design/milestones/plan-next.md`).

- **One line for a fresh copy.** Copy something, open Booklight within two minutes, and one line under the
  empty field says what it is: "Copied 20 s ago · a link and a date". Booklight reads only the system's
  description of the copy for that, not its text, and Android shows no "pasted" message. No line for a copy
  marked private or one Booklight made. A switch in the window turns it off.
- **Tab opens it.** The copy becomes the chip. Under it: a link (Open, or Copy clean without its tracking tail),
  a date as an event filled in, a phone number, a mail address, a flight number (its row names the airline;
  it is looked up only when you go to it); then your first three prompts, and Summary for a
  long text. A text that is not in your language has its translation first.
- **Answers where they stand.** Enter on a model's row writes the answer into that row and the chip stays the
  copy. Enter again copies. Backspace on the empty field steps back.
- **Into any language.** Under the copy's chip, type a language: `danish`. From the empty field:
  `tr danish see you on Saturday`.
- **Say what to do with it.** Under the copy's chip, anything that is not a row's name or a language is an
  instruction for the model: `pull out the tasks as a list`.
- **`clip`** shows the same, shorter list (the other rows are found by typing their names) and no longer reads
  a copy marked private. Text another app hands over behaves the same way.
- **The model on the HP Googlebook answers.** The system calls it "downloadable" there although it has it;
  Booklight now tries it before handing over to Gemini.
- A long text is never returned cut: a rewrite or translation of more than about 1,800 characters hands over
  to Gemini.

**Flights** (M5 of `docs/design/milestones/plan-next.md`). Built without a device: its checks are still to do.

- **A flight number is a row.** `LH455`, `lh 455`, `LH0455` and the callsign `DLH455` name the airline, from a
  table of 1,375 airlines Booklight carries (`tools/airlines.py`, public domain). Enter opens the flight's page
  at FlightAware; the other actions search the web and ask Gemini. Nothing leaves the device before Enter.
- **With a key of your own it answers.** Put a free AirLabs key into the Booklight window › Results › Flights
  and the row shows who flies it and where, when it leaves and lands in each airport's own time (with the day
  when it is not your today), the status in words ("Delayed 25 min", "In the air · 18 min late", "Landed"), and
  where to go: terminal and gate before it leaves, the arrival's terminal and belt after. One lookup, 400 ms
  after the last key; the answer is kept for two minutes; the footer says who gave it and when. Booklight ships
  no key.
- **The next flight.** In the air, or landed under three hours ago, or the next to leave. A day after the number
  (`LH455 fri`, `LH455 tomorrow`) is that day's, from the timetable.
- **What only looks like a flight** (`ps5`, `mp3`, `ms 365`, `q4 2026`, `win 11`) is the last row, and nothing is
  sent for it until you go to it with Down or Tab. `flight u2 8001` (German `flug`) makes anything a flight.
- **Copy, Pin, and three more behind the arrow**: Add to calendar, Search the web, Ask Gemini. A pinned flight
  counts down to its take-off, then to its landing, then says "Landed". It follows that one flight, and asks
  about it again every half hour from three hours before it leaves until it has landed.
- **When there is no answer** the row says why in plain words (nothing found, the key was not accepted, its
  lookups are used up, no connection) and keeps its place; Enter still opens the flight's page.
- Not in it: the usual route without a key (its table's host has not been asked yet).
- For developers: `core/Flights.kt` (the reader, and which matches are strong), `core/AirLabs.kt` (the replies,
  and which requests "the next flight" takes), `core/FlightStatus.kt` (the words); `./bl debug pref flightkey KEY`
  and `./bl debug flight LH455`. `Action.off` (an action that keeps its place, dimmed) and four more fields on
  `Body.Slots` are new in the model.

**Your usual** (issue 1, `docs/design/zero-state.md`). Behind a switch that starts off: the Booklight window ›
Your usual › Show your usual. With it off, tips come as before.

- **Two or three rows under the empty field**: the two things you run most from Booklight, and the one you ran
  last in the past eight hours. Never one row; none until two things have been run twice each. They are ordinary
  rows, with nothing selected: Enter does nothing until Down (or Tab) brings the highlight to row one.
- **They hold their seats.** What had seat one or two keeps it until something else weighs a fifth more, so
  "Down, Enter" means the same thing from one opening to the next.
- **A fresh copy has the place first**: its line comes, and the rows do not, for those two minutes.
- **Don't suggest** on each of the rows (in an app's list, the last line before Uninstall) takes a thing out for good;
  "Suggest everything again" in the window brings all back.
- **What counts as a run**, for every list: opening a thing or doing its thing. App info, Edit, Delete and
  Uninstall no longer lift a row's rank.
- Also for every list: a row that leaves while it is still rising no longer jumps, and what is under the field
  waits for the glass's edge while the panel opens.

**The Booklight window is a desktop window** (`docs/design/window-redesign.md`; Alex: the settings app "looks very
weird on a Googlebook … more desktop ready and use material 3 expressive … The left nav should be at the left edge").

- **Navigation on the window's leading edge**: Material's navigation rail, 220 dp wide with each name beside its
  mark from 840 dp of window, 96 dp with the name under the mark from 600, and a navigation bar along the bottom
  under 600. One indicator travels between the sections. The page's column starts at the rail and is at most
  720 dp wide; nothing is centred any more. The caption bar is see-through. The window is at least 400 × 480.
- **Five sections**: Start, Commands, Look, Results, Privacy. Yours is the other half of Commands (Built in ·
  Yours), with Find (Ctrl + F, or just type) and New link, whose arrow offers snippet, recipe and prompt
  (Ctrl + N). Access and About are Privacy. The assistant key is on Start. The window opens where it was left.
- **Rows and controls are Material 3 Expressive**: grouped rows, switches, connected button groups, a menu for the
  search engine, Material's text fields and buttons in the editors. Every row has a mark; text, and the controls
  at the rows' ends, each share one line. Hover and pressed states, one focus ring that glides, right-click menus
  (Try it, Copy example; Edit, Duplicate, Delete; Show, Hide), tooltips, a scrollbar that can be dragged.
- **Keys**: Tab and Shift + Tab through everything, F6 between navigation and page, arrows, Home, End, Page Up,
  Page Down, Ctrl + 1 … 5 for the sections, Delete on one of yours (asks once more), Menu or Shift + F10 for a
  row's menu, Escape clears Find, Ctrl + W closes the window. Left and Right step a choice; a switch is flipped
  by Enter or Space, and Left on it goes back to the navigation, as on every row. The window's keys are in the
  system's Keyboard Shortcuts Helper.
- **A choice is changed only by its buttons**: a click on the row beside them does nothing.
- **The editors**: what you typed stays when the window is resized. An editor you changed asks once before it
  is left ("Press again to discard"). A line says why Save cannot be pressed yet; Enter saves. After Save the
  keys are on what you saved.
- **Forget everything** also forgets the recent emoji and letters and which commands were used.
- **A second pane from 1332 dp**: the live demo on Start, a preview of the panel on Look (it shows the theme, the
  colours, the glass, the shadow and the dimming as they are chosen), the editor beside the list on Commands.
  Below that width the demo and the preview are at the top of their pages, and an editor opens under its row.
- Look's explanations are one line each.
- **The night's new settings in it**: "Your usual" and "What you copied" on Start, after Tips; "Flights" on
  Results, with a field for your AirLabs key (pasted, saved with Enter; never shown again; Take out asks twice)
  and a row that opens where a key is got.
- New library: `androidx.compose.material3.adaptive` 1.3.0 (window size classes). No new permission.

## 2.2 (1 October 2026)

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
