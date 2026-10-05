# First run: the proof on a device (milestone 0)

*What part 1 of the first-run plan (`docs/superpowers/plans/2026-10-04-first-run.md`, Tasks 1 to 4) built into a
debug build, and the checks it is there for. Whoever runs a check writes its answer under it, with the date and
"the Lenovo Googlebook" or "the HP Googlebook". Until then an answer reads "not run".*

**Before the first check**

- Choose the device by its model (`adb devices -l`), never by its place in the list; set `ANDROID_SERIAL`.
- Close the Booklight window, then `./bl install`. `./bl idle` before every `./bl open`: the panel takes the keyboard.
- Never `uiautomator dump`. No sign-in is made for a check. A capture of the screen shows real windows: none is
  kept or committed.
- This file is public: no serial, no build number, nothing about what is installed. A referrer is written as its
  kind (the system, the home app, the store, Settings, adb), not as another app's package name.
- Every line the proof logs begins `proof +NNNms` (ms since its first line). `./bl logs 40` shows the last forty
  lines of Booklight's log.

## 1. The held panel has its own page in the dialog

`./bl open stay`, then `./bl debug proof-helper`.

Look: does the system's Keyboard shortcuts dialog open on a page named for Booklight, headed "A shortcut for
this app, in five steps", with five rows that each end in the Action glyph and a cap "Quick Insert"? Read in
`./bl logs 40`: `rows asked: deviceId=… quickInsert=… rows=…`.

Answer (The Lenovo Googlebook, 5 October 2026): yes. The page is named Booklight and headed "A shortcut for this app, in five steps".
`rows asked: deviceId=0 quickInsert=true rows=quick`. The rows stand on 1, 2, 3, 1 and 2 lines, each ending in
the Action glyph and a cap "Quick Insert"; all five are in view without scrolling.

## 2. What the panel is told while the dialog is over it

In the same log, after `proof-helper`: which of `focus=false`, `pause`, `topResumed=false`, `stop`, `destroy`
came, in which order, how many ms apart? Is anything of the panel seen beside or under the dialog?

Answer (The Lenovo Googlebook, 5 October 2026): `focus=false` comes 47 ms after `rows asked`, and nothing else: no `pause`, no
`topResumed=false`, no `stop`, no `destroy` in the 18 seconds the dialog stood over it. The panel's window stays
where it is, behind the dialog; nothing of it is seen, the dialog covers its place.

## 3. The dialog goes

Close it with Esc: does `focus=true` come? Type `x`, then `./bl debug dump`: is `query='x'`? The same with the
dialog's X. Then open it again, click its Customize button, and `./bl debug proof-helper close`: does it go?

Answer (The Lenovo Googlebook, 5 October 2026): Esc: `focus=true` comes, and a typed `x` lands (`query='x'`). The dialog's X: `focus=true`.
After its Customize button, `proof-helper close`: the dialog goes and `focus=true` comes.

## 4. In German

With the system's language set to German: check 1 again. The lines each row stands on; all five in view? And
the dialog's own words on its two buttons (Customize, Set shortcut): they replace the stand-ins
`first_sys_customize` and `first_sys_set` in `app/src/main/res/values-de/strings_32.xml`.

Answer (The Lenovo Googlebook, 5 October 2026): not run with the system in German. Read instead from the system UI's own package on
the device, which carries its German words: Customize is „Anpassen“, Set shortcut is „Speichern“, Keyboard
shortcuts is „Tastenkürzel“, Action is „Aktion“, Done is „Fertig“, the search field is „Tastenkürzel suchen“.
So the German `first_sys_customize` is „Anpassen“ and `first_sys_set` is „Speichern“ (the stand-in said
otherwise). How German rows stand was seen on 4 October with a stand-in window: two or three lines a row, all in
view.

## 5. The rows with Action + M

`./bl debug proof-rows m`, `./bl debug proof-helper close`, `./bl debug proof-helper`: do the rows end in the
Action glyph and "M", and does row 3 say M? Afterwards `./bl debug proof-rows auto`.

Answer (The Lenovo Googlebook, 5 October 2026): yes. `rows asked: deviceId=0 quickInsert=true rows=m`; every row ends in the Action
glyph and "M", and row 3 reads "3. Hold Action and press M, or any keys you like" on two lines.

## 6. The key lands

With the dialog open on Booklight's page, do what its five rows say, with Action + Quick Insert. After "Set
shortcut", press the keys again.

Look: does the dialog close by itself, and is the panel there, in front? Read: the line `start (onNewIntent):
action=… referrer=… byKey=… fromIcon=…`, and whether `focus=true` comes before or after it, and how many ms apart.

Answer (The Lenovo Googlebook, 5 October 2026): run with the shortcut Booklight already has on this device, which was left as it is
(setting one through the dialog's five steps was seen on 4 October with a stand-in app). With the dialog open on
Booklight's page the keys were pressed: the dialog went by itself and the panel was in front. The lines:
`topResumed=false`, `pause` 1 ms later, then `start (onNewIntent): action=android.intent.action.MAIN
categories={android.intent.category.LAUNCHER} extras=null bounds=false referrer=` the system (`android-app://android`)
` byKey=true fromIcon=false` 2 ms after that, `resume` and `topResumed=true` in the same ms, and `focus=true`
45 ms after the start. So the start comes first and the focus after it.

## 7. A held key

With the panel held, hold the keys down for about a second: how many `start (onNewIntent)` lines?

Answer (The Lenovo Googlebook, 5 October 2026): one `start (onNewIntent)` line for keys held a second.

## 8. Other ways to start it

Each with the panel held; one `start` line each: a click on the icon (taskbar or Apps list); "Open" on
Booklight's App info page in Settings; the home screen's widget and the Quick Settings tile, if they are placed.
Then `./bl debug close`, and the keys once more: the line is `start (onCreate)`.

Answer: not run. It needs clicks on the icon, on App info's "Open", on the widget and the tile.

## 9. The system's own words

`./bl debug proof-words`. For each of the five names: its word, or the reason there is none. Also
`visible-without-queries(com.android.shell)=`, and `took=`. Once with the system in English, once in German,
and once with Booklight's own language set to the other one (Settings, Apps, Booklight, Language): are the
words the system's, and the same as on the dialog's buttons?

Answer (The Lenovo Googlebook, 5 October 2026), the system in English: all five are read, in 3 ms: 'Customize', 'Set shortcut',
'Keyboard shortcuts', 'Action', 'Done', the same as on the dialog's buttons;
`visible-without-queries(com.android.shell)=true`. With the system in German, and with Booklight in the other
language: not run (the German words are under check 4).

## 10. Where "Quick Insert" comes from

`TRIES=120 ./bl debug proof-find Quick Insert`: the names it finds in the system UI and in the platform. If
there is one: `./bl debug proof-words NAME` with the system in German: the German word.

Answer (The Lenovo Googlebook, 5 October 2026): `'Quick Insert' is com.android.systemui: keyboard_key_quick_insert | android: -`,
in 48 ms. Its German, read from the same package: „Schnelles Einfügen“.

## 11. The keyboards

`./bl debug proof-keys`, with the device's own keyboard alone, and again with an external keyboard attached:
the lines. And by eye: is Quick Insert the key left of A?

Answer (The Lenovo Googlebook, 5 October 2026): the device's own keyboard: `id=0 external=false virtual=false alphabetic=true
quickInsert=true m=true capsLock=true`. The virtual keyboard (`id=-1`) says the same; three other input devices
are not alphabetic and have none of the three keys. No external keyboard was attached. By eye, whether Quick
Insert is the key left of A: not looked at; it is Alex's to say.

## 12. A tall glass redrawn every frame

`./bl backdrop`, `./bl open stay`, the field empty. For each of `tall`, `light`, `shader`, `canvas`, `both`:
`./bl debug proof-glass MODE`, six seconds, `./bl logs 3`: the line `proof glass MODE: …`. Light theme and dark
(`DARK=true ./bl open stay`).

Look while each runs: the lower edge as the glass grows and comes back; a blurred rectangle beside or under the
glass; anything that stutters.

Answer (The Lenovo Googlebook, 5 October 2026), the debug build after `cmd package compile -m speed`, over the backdrop. While a mode
redraws, the screen draws 120 frames a second. Nobody watched the screen: the lower edge, a blurred rectangle
and stutter were not looked at.

| Theme | Mode | Frames | Whole frame p50 · p99 · max (ms) | Past their deadline |
| --- | --- | --- | --- | --- |
| Light | tall (60 a second) | 258 | 13.6 · 26.0 · 26.8 | 0 |
| Light | light | 476 | 6.7 · 9.8 · 16.4 | 0 |
| Light | shader | 476 | 2.3 · 9.6 · 10.6 | 0 |
| Light | canvas | 475 | 7.3 · 10.3 · 11.9 | 0 |
| Light | both | 475 | 6.4 · 10.0 · 19.2 | 1 |
| Dark | tall (60 a second) | 246 | 2.2 · 8.0 · 15.3 | 0 |
| Dark | light | 476 | 2.3 · 9.1 · 16.5 | 0 |
| Dark | shader | 475 | 2.4 · 8.2 · 13.1 | 0 |
| Dark | canvas | 476 | 5.7 · 9.1 · 14.4 | 0 |
| Dark | both | 475 | 2.6 · 9.3 · 11.2 | 0 |

## 13. The HP Googlebook, with Alex

Checks 1, 6 and 12 again there.

Answer: not run. The HP Googlebook is not used until Alex says so.

## What the answers decide

| Check | Decides |
| --- | --- |
| 1 to 3 | Whether step 1 is built in the panel as designed, and which signal ends the hold (part 2) |
| 4, 9, 10 | Whether the system's words and the key's name are read or written (part 2), and the German fallbacks |
| 5, 11 | How the suggested key is chosen for a keyboard (part 2) |
| 6 to 8 | What counts as "the key was pressed" (part 2) |
| 12, 13 | What the welcome may draw, and how tall (part 4) |

## What the answers decided (5 October)

- **Step 1 is built in the panel, as designed.** The real panel, held, has its own page in the dialog, gets the
  focus and the keys back when the dialog goes, and is told when the key lands.
- **Under the dialog the panel is told one thing, `focus=false`.** So the hold cannot wait for a pause or a stop
  to know that the dialog is up; and a key that lands shows as `pause`, the start, `resume`, then `focus=true`.
- **"The key was pressed" has a positive sign**: a launcher start whose referrer is the system, with no bounds
  and no extras. A held key starts it once.
- **The system's words and the key's name can be read** by an ordinary app, in milliseconds, and the names are
  known. The German fallbacks are „Anpassen“, „Speichern“ and „Schnelles Einfügen“.
- **The welcome can afford to redraw the tall glass every frame** on this device: 120 frames a second with a
  shader pass and a canvas pass together, one frame in 475 past its deadline, in a debug build.
- Still open: checks 8 and 13, the German run of 9, and the look of check 12 by eye.
- Also open: check 11 was not run with an external keyboard, so `hasKeys` answering "no" for a keyboard without the Quick Insert
  key is not yet seen; and check 6 ran with the shortcut the device already had, not one set through the five rows.
