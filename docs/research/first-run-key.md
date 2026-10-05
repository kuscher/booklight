# First run: the key's step on a device (part 2)

*The checks for part 2 of the first-run plan (`docs/superpowers/plans/2026-10-04-first-run-2-the-key.md`). Whoever
runs a check writes its answer under it, with the date and "the Lenovo Googlebook" or "the HP Googlebook". Until
then an answer reads "not run".*

**Before the first check**

- Choose the device by its model (`adb devices -l`), never by its place in the list; set `ANDROID_SERIAL`.
- Close the Booklight window, then `./bl install`. `./bl idle` before every `./bl open`: the panel takes the keyboard.
- Never `uiautomator dump`. No sign-in is made for a check. A capture of the screen shows real windows: none is
  kept or committed; `./bl shot NAME` is the panel's own window.
- This file is public: no serial, no build number, nothing about what is installed. A referrer is written as its
  kind (the system, the home app, the store, Settings, adb), not as another app's package name.
- `./bl debug first` prints first run's stored state, the screen that stands, the suggested key, the system's
  words as they were read, the hold, and the last start. `./bl debug pref key no` makes Booklight forget that it
  has a key (the device's own shortcut stays as it is); `./bl debug pref key seen` puts that back.

## 1. K1, as a still

`./bl debug first new`, `./bl debug pref key no`, `./bl open stay`, `./bl debug dump`, `./bl shot k1`; again
with `DARK=true ./bl open stay`.

Look, at four times the size: the panel is 252 dp high; the seat's mark on the field's mark's line (x = 38); the
title, the line, the first cap and the caption on one left edge (x = 72); the counter "1 of 5", the `esc` cap and
the answers on one right edge (x = 700); the caps `Action` + `Quick Insert` flat, with their white ring, over a
white window and a dark one; "Open Keyboard shortcuts" armed. In the dump: `first=K1 armed=0 1/5`.

Answer (The Lenovo Googlebook, 5 October 2026): the panel is 252 dp high (1080 × 378 px at this screen's 1.5). The seat's mark stands on the
field's mark's line; the title, the line, the first cap and the caption start on one edge, x = 72; the counter, the
`esc` cap and the answers end on one edge, x = 700. The caps are flat in the light theme and the dark one, "Open
Keyboard shortcuts" is armed. The dump: `first=K1 armed=0 1/4`: "of 4" and not "of 5", because suggestions are
already on on this device and the question is then no step. Looked at in `./bl shot`, which is the panel's own
window with no desk behind it: the caps' ring over a white window and a dark one was not judged.

## 2. K2, K3, K4, by the hooks

With the panel open: `./bl debug first at k2`, `./bl shot k2`; `first at k3`, `shot k3`; `./bl debug first key`,
`shot k4`. K2: "your key" blank, nothing armed, a `tab` cap before the answers. K3: "Open Keyboard shortcuts"
armed, the caption "Or try Action + M". K4: the check in the key, "Go on" armed, no caption. Does "your key"
read as a key that waits, or as an empty box? Does anything move that should stand (the answers' right edge, the
seat)?

Answer (The Lenovo Googlebook, 5 October 2026): all three as said. K2: a blank cap with "your keys" under it, nothing armed, the `tab` cap
before the answers. K3: "Open Keyboard shortcuts" armed, "Or try Action + M" under a blank cap. K4: the check in
the cap, "Go on" armed, no caption. Nothing moves that should stand: the seat, the counter and the answers' right
edge are the same in all four; K4's one answer stands at the middle of the band's height, where two answers begin
higher. To this reader the blank cap is an empty box more than a key that waits: for Alex to judge, and for part
5's motion.

## 3. Keys on the stage

On K2: `./bl debug key enter` (nothing; the dump is unchanged), `key tab` (`armed=0`), `key tab` (`armed=1`),
`key backtab` (`armed=0`). `./bl debug keys ch`: the list comes in the stage's place and the letters are there
in the same frame; `./bl debug type` with nothing after it: the stage is back. Esc and a click outside close;
the next opening shows the same screen.

Answer (The Lenovo Googlebook, 5 October 2026): on K2 Enter does nothing (the dump is unchanged); Tab arms the first answer, Tab the second,
Shift + Tab the first again, and it goes round. `keys ch`: the list has the stage's place and the letters are
there; the field emptied, the stage is back at once with the answer that was armed. A real letter, looked at frame
by frame: the stage goes in the frame the letter comes, the glass keeps its 252 dp for the few frames until the
rows are there and then grows; it does not start back towards the field's height. A real Esc closes; the next
opening shows K2 again with nothing armed. A click outside: not run.

## 4. The system's words, read without a line in `<queries>`

`./bl debug first`: `words=Read(customize=…, set=…, quick=…)`. (Right after the app has started it can say
`words=not read`: the words are asked for then. Say it again.) Are all three there, and the system's own? With
the system in German (if it can be set): „Anpassen“, „Speichern“, „Schnelles Einfügen“? With Booklight set to
the other language than the system: still the system's? If `words=not read` stays on a device, Booklight's own
words stand in for the system's (the fallbacks): note it here. No line for `com.android.systemui` goes into
`<queries>` without Alex's word.

Answer (The Lenovo Googlebook, 5 October 2026): `words=Read(customize=Customize, set=Set shortcut, quick=Quick Insert)`, with no line in
`<queries>`; all three are the system's own, as its dialog says them. The first call after the app had started
said `not read`, the next had them. With Booklight in German and the system in English they are still the
system's: K3 quotes „Customize“, the dialog's rows „Customize“ and „Set shortcut“. With the system in German:
not run (the system's language is not changed for a check).

## 5. The real dialog, and back without a key

On K1, Enter. Does the system's dialog open on Booklight's page, with five rows that quote "Customize" and "Set
shortcut" as the dialog's own buttons say them and end in the suggested keys? `./bl debug first`: `hold=UNDER`,
`helper=1`. Esc: K2 stands, `hold=NONE`, a typed letter lands. Enter by Tab on "Open Keyboard shortcuts", Esc
again: K3, `helper=2`.

Answer (The Lenovo Googlebook, 5 October 2026): yes. The dialog opens on Booklight's page, headed "A shortcut for this app, in five steps",
with five rows on 1, 2, 3, 1 and 2 lines that quote "Customize" and "Set shortcut" and each end in the Action
glyph and a cap "Quick Insert". `hold=UNDER`, `helper=1`, K2 under the dialog. Esc: K2 stands, `hold=NONE`, a
typed x lands. Enter with nothing armed does nothing; Tab, Enter: the dialog again, `helper=2`, K3 under it; Esc:
K3 with "Open Keyboard shortcuts" armed. The first run of this check found a fault: a panel opened with
`./bl open stay` never heard of the dialog (the hold stayed `NONE` and K1 stood). It was fixed, and the check
passes with `stay` and without.

## 6. The key lands under the dialog

From K1: Enter, then in the dialog its five steps (or, where the device already has Booklight's shortcut, just
its keys). Does the dialog go by itself, and is the panel there with "Your key works", the check, "Go on"
armed? Which screen is in the first frame that shows: K4, or K2 for a frame? `./bl debug first`: `key=true`,
`screen=K4`, `hold=NONE`, and the last start: `MAIN referrer=android bounds=false extras=false key=true
icon=false`.

Answer (The Lenovo Googlebook, 5 October 2026): run with the shortcut this device already has for Booklight, left as it is. The dialog goes by
itself and the panel is in front with "Your key works", the check, "Go on" armed. Frame by frame: while the
dialog fades, the panel under it still says K2's title and line (its answers are already K4's, with the `tab` cap
beside "Go on" for a few frames); then the title and the line roll to "Your key works" and the check draws
itself. So K2 is seen for a moment and turns into K4 in view, which reads as an answer to the press; whether K4
should stand whole in the first frame is part 5's (motion). `key=true`, `screen=K4`, `hold=NONE`, and the last
start: `MAIN referrer=android bounds=false extras=false key=true icon=false`.

## 7. The key in view, and a key held

`./bl debug pref key no`, `./bl debug first at k2`, the panel open and in front: press the keys. K4, and the
panel stays. Hold the keys for a second on K4: does the panel go once (as every day), or flicker?

Answer (The Lenovo Googlebook, 5 October 2026): on K2 with the panel in front, the keys: K4, and the panel stays. The keys held for a second
on K4: the panel goes, and none comes back (the dump says `no panel`). By the dump only: not watched for a
flicker.

## 8. "Go on" and "Not now"

On K4, Enter: the bare field at 68 dp; `./bl debug first` says `done=[key]`, `screen=L2`, and the dump
`first=none 2/5` (the lessons are not drawn yet: nothing stands). The next opening: nothing of first run; a
tip comes as it always did. Then `first new`, `pref key no`, open, Tab, Enter on "Not now": the bare field;
`run=NONE mark=true`.

Answer (The Lenovo Googlebook, 5 October 2026): on K4, Enter: the bare field, 68 dp; `screen=L2`, the dump `first=none 2/4`. (`done=[show,
key]` here: the hook `first at k2` had marked the step before the key's.) The next opening shows nothing of first
run; "your usual" has the place under the field (it is switched on on this device), so no tip came. Then `first
new`, `pref key no`, open, Tab, Enter on "Not now": the bare field, `run=NONE mark=true`.

## 9. What ends the hold

With the dialog over the held panel, each from a fresh K1: a click beside the dialog; a click on another
window; another app's keyboard shortcut; the lock screen. Each time: is the panel gone afterwards (`./bl debug
dump` says `no panel`) or in front with K2, and never left on the desk without the keys? Where it is still
there, `./bl debug first` says `hold=NONE`.

Answer (The Lenovo Googlebook, 5 October 2026): a click beside the dialog: the dialog goes and the panel is in front with K2, `hold=NONE`
(with `stay` and without). Another window taking the front under the dialog (Booklight's own window, started
from adb, as a stand-in for another app's shortcut): the panel is gone (`no panel`); the dialog stays, over the
new window. A click on another app's window, another app's shortcut and the lock screen: not run (taps and keys
go only into Booklight and into the dialog it opened).

## 10. The icon's first click

`./bl debug first new`, `./bl debug pref key no`, no panel open. Click Booklight's icon: the panel with K1.
Click it again: the Booklight window. `./bl debug first`: `icon=true`. Does the launcher's own closing close
the panel in its first second?

Answer (The Lenovo Googlebook, 5 October 2026): by a stand-in only: a start from adb that carries an icon's place, which the panel reads as
a click on the icon (`bounds=true icon=true`). The first: the panel with K1, and `icon=true` is kept. The second,
with the panel open: the Booklight window, and the panel is gone. A third, with no panel open: the window. A
real click on the icon in the taskbar or the Apps list, and whether the launcher's own closing closes the panel
in its first second: not run.

## 11. Other starts (the proof's check 8)

Each once, then `./bl debug first` for the line `last start:`: "Open" on Booklight's App info page in Settings;
the home screen's widget and the Quick Settings tile, if they are placed; the keys with no panel open. Is the
key the only one that says `key=true`?

Answer (The Lenovo Googlebook, 5 October 2026): the keys with no panel open: a new panel that stands on K4 at once; `MAIN referrer=android
bounds=false extras=false key=true icon=false`. A start from adb (`./bl open stay`): the sender is adb,
`extras=true key=false icon=false`. With check 10's stand-in the key is the only one that says `key=true`.
"Open" on the App info page, the widget and the tile: not run.

## 12. A keyboard without Quick Insert

With an external keyboard attached: `./bl debug first`, `keyboards=`: does the external one lack `+quick`? Open
the panel on K1 and press Tab on the external keyboard, then Enter on "Open Keyboard shortcuts": do the dialog's
rows end in Action + M, and does row 3 say M? `./bl debug first at k3`: is the caption "or any keys you like"?
(Until this is seen, "Action + M for a keyboard without the key" rests on tests alone.)

Answer (The Lenovo Googlebook, 5 October 2026): not run: no external keyboard was attached (`keyboards=own+quick`).

## 13. Is Quick Insert the key left of A?

By eye, on the keyboard, for Alex to confirm: K1's caption says so.

Answer: not run. It is Alex's to say.

## 14. German

Booklight in German: K1 to K4, nothing cut, the caption „… liegt links neben A · oder Tasten deiner Wahl“ in
its room, the answers „Tastenkürzel öffnen“ and „Nicht jetzt“. With the system in German: does the cap read
„Schnelles Einfügen“ and fit, or does it fall back; the dialog's rows in German, all five in view?

Answer (The Lenovo Googlebook, 5 October 2026): Booklight in German, the system in English: K1 to K4, nothing cut. The caption „Quick Insert
liegt links neben A · oder Tasten deiner Wahl“ has its room; the answers are „Tastenkürzel öffnen“, „Nicht jetzt“
and „Weiter“; the caps read „Aktion“ and the system's "Quick Insert". The dialog's rows are German on 2, 2, 3, 1
and 3 lines, all five in view. With the system in German: not run.

## 15. An installation that was there before

`./bl debug first update`, `./bl debug pref key no`, open: K1 with no counter. Its "Go on" after the key ends
the run (`run=NONE`).

Answer (The Lenovo Googlebook, 5 October 2026): yes. K1 with no counter; after the key, "Go on" ends the run (`run=NONE`, `mark=true`).

## 16. The same answer twice

`./bl debug first update`, `./bl debug first answer not_now`, `./bl debug first older`: `run=NONE mark=true`
both times. `./bl debug first update`, `./bl debug first helper`, `./bl debug first older`: `run=UPDATE
helper=0`, K1.

Answer (The Lenovo Googlebook, 5 October 2026): yes. After "Not now": `run=NONE mark=true`, and the same after `first older`, twice. After
`first update` and `first helper` (`helper=1`, K2): `first older` gives `run=UPDATE helper=0`, K1, twice.

## 17. Animations off, and a screen reader

With the system's animations off: every K screen whole in one frame, K4 with its check. With a screen reader on:
each screen said once when it comes (the counter, the title, the line, the keys, the caption, how to act).

Answer (The Lenovo Googlebook, 5 October 2026): with the system's animations off (set for this check and put back): K1, K2 and K4 each stood
whole a quarter of a second after they came, K4 with its check. A screen reader: not run.

## 18. Nobody else sees a change

`./bl debug first off`, `./bl debug pref key seen`: open and close by the keys ten times; a tip, the copy's
line and "your usual" come as before; no stage ever comes; `./bl debug pref` before and after says the same but
for what was set here.

Answer (The Lenovo Googlebook, 5 October 2026): with no run and a key known, opened and closed by the keys ten times: "your usual" stood
under the field each time (it is on on this device, so no tip had the place), no stage came, and every second
press put the panel away. `./bl debug pref` said the same before and after. The copy's line: not tried.

## 19. The HP Googlebook, with Alex

Checks 1, 5, 6, 9 and 10 again there.

Answer: not run. The HP Googlebook is not used until Alex says so.

## 20. The dialog does not take the front as an activity would

On K1, Enter; with the system's dialog over the panel, press nothing for two seconds. `./bl debug first` still
says `hold=UNDER`, and `./bl debug dump` does not say `no panel`. (The hold ends 300 ms after another window has
the front; if the dialog counted as one, the panel would go behind its own dialog.)

Answer (The Lenovo Googlebook, 5 October 2026): yes. More than two seconds under the dialog with no key pressed: `hold=UNDER`, and the panel
is there, with `stay` and without. The dialog does not take the front as an activity would.

## 21. The dialog's own steps over the held panel

From K1 with a panel that was not told to stay: Enter; in the dialog Customize; a name typed into its search
field; the + of an app's row, so that the dialog that takes the keys comes; Cancel there; Done; then close the
dialog. After each: `./bl debug first` for `hold=`, and `./bl debug dump` for the panel. (The hold ends at the
first focus that comes back and 300 ms after another window has the front: if the second dialog's coming or going
did either, the panel would go behind the dialog.)

Answer (The Lenovo Googlebook, 5 October 2026): the panel holds through every step: `hold=UNDER` and K2 under the
dialog after Customize, after the name, with the dialog for the keys up, after its Cancel and after Done; the
dialog closed, K2 is in front with `hold=NONE`. Nothing was set: this device's shortcut for Booklight was left as
it is (its row there has no +, an app has one shortcut), so the + was another app's and was cancelled. Not seen
with the held panel: "Set shortcut" and the first press of a new key.

## What the checks found (5 October 2026)

- **A fault, fixed**: a panel opened to stay did not hear of the dialog (check 5).
- **The key's landing is seen as a change**: K2 for the frames of the dialog's fade, then K4 (check 6). Kept; part
  5's motion may make more of it.
- **The hold bears the dialog's own steps** (check 21): Customize, the name, the dialog for the keys and its
  going leave the panel held.
- **Still open**: the last two of the five steps with the held panel, "Set shortcut" and the first press of a new
  key, on a Googlebook where Booklight has no shortcut yet (21; the proof's check 6 is open in the same way); a
  real click on the icon and the other starts (10, 11); a keyboard without Quick Insert (12);
  whether Quick Insert is the key left of A (13); the system in German (4, 14); a screen reader (17); a click
  outside the stage (3); other apps' windows and shortcuts and the lock screen under the hold (9); a font scale
  above the usual; everything on the HP Googlebook (19).
