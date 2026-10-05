# First run: the lessons, the question, the choices and the ending on a device (part 3)

*The checks for part 3 of the first-run plan (`docs/superpowers/plans/2026-10-05-first-run-3-the-lessons.md`).
Whoever runs a check writes its answer under it, with the date and "the Lenovo Googlebook" or "the HP Googlebook".
Until then an answer reads "not run".*

**Before the first check**

- Choose the device by its model (`adb devices -l`), never by its place in the list; set `ANDROID_SERIAL`.
- Close the Booklight window, then `./bl install`. `./bl idle` before every `./bl open`: the panel takes the keyboard.
- Never `uiautomator dump`. No sign-in is made for a check. A capture of the screen shows real windows: none is
  kept or committed; `./bl shot NAME` is the panel's own window.
- This file is public: no serial, no build number, nothing about what is installed. The lessons' example is an app
  of the device: an answer says "the example's app" and "its letters", never the name or the letters themselves.
- Write down what `./bl debug pref` says before the first check (`suggestions=`, `zero=`, `engine=`, `opening=`)
  and put all four back after the last one. The question only stands while suggestions are off:
  `./bl debug pref suggestions off` before a check that needs it.
- `./bl debug first` prints first run's stored state, the screen that stands and, with a panel open, `example=`:
  what the lessons' recipes show on this device (`open` and `search` are the letters, `enter=true` where lesson 3
  goes by an app's row, `enter=false` where Settings stands in). Below, LETTERS stands for those letters.
- `./bl debug dump` says `first=` (the stage under the empty field, its armed answer, the counter), `due=` (the
  screen that is due whatever the field holds), `coach=`, `pressed`, `choices`, `ended`, the placeholder as
  `hint=`, the footer's word as `flash=`, and the window's size in px (divide by the screen's density for dp).
- `./bl debug keys TEXT` types into the empty field a letter at a time; `./bl debug in TEXT` types under the chip
  that is there; `./bl debug type` with nothing after it empties the field.
- The counters below ("2 of 5", `2/5`) are a new installation's with suggestions off. With suggestions on the
  question is no step, and they read "of 4".
- A lesson is reached without the key's step: `./bl debug first new`, `./bl debug first key`, then
  `./bl debug first at l2` (or `l3`, `l4`, `q`, `c`). An open panel follows at once.
- A screen's keys and clicks count once the screen has been in view for 350 ms: leave a moment between a hook that
  changes the screen and a key (two `./bl` calls in a row are slower than that by themselves).
- After a lesson's Enter its list stands for half a second. With `./bl open stay slow=4` that is two seconds:
  time enough for a dump or a shot in between.

## 1. Lesson 2, as a still

`./bl debug first new`, `./bl debug first key`, `./bl debug first at l2`, `./bl open stay`, `./bl debug dump`,
`./bl shot l2`; again with `DARK=true ./bl open stay`.

In the dump: `first=L2 armed=-1 2/5 due=L2`, `hint='An app’s first letters'`, and the window 252 dp high. Look,
at four times the size: the seat's mark (the `open` symbol) on the field's mark's line; "Open an app" and its line,
the recipe's letters and the caption on one left edge (x = 72); the recipe's letters in the field's type beside
one large cap with the Enter mark, on the caps' centre line; the caption "Practice: nothing opens. Enter also puts
Booklight away."; the counter "2 of 5", the `esc` cap and "Skip" on one right edge (x = 700); "Skip" not lit, a
`tab` cap before it.

Answer (The Lenovo Googlebook, 5 October 2026): yes. `first=L2 armed=-1 2/5 due=L2`, the placeholder as said, 252 dp. The mark stands on the
field's mark's line; the title, its line, the recipe's letters and the caption start on one edge; the letters stand
in the field's type beside one large cap with the Enter mark; the caption is whole; the counter and the `esc` cap
end on one edge, "Skip" is not lit and has its `tab` cap before it. Light theme; the dark one was looked at on the
question only (check 12).

## 2. Lessons 3 and 4, as stills

With the panel open: `./bl debug first at l3`, `./bl shot l3`; `./bl debug first at l4`, `./bl shot l4`.

Lesson 3: the mark is `search`; the recipe reads letters, `Tab`, Enter, a word, Enter (with `enter=false`:
letters, `Tab`, a word, Enter) and ends before the `tab` cap; the placeholder is "LETTERS, then Tab". Lesson 4:
the mark is `calc`; the recipe is `150 + 20%` and Enter; the caption ends "Today it stays."; the placeholder is
`150 + 20%`. Each at 252 dp. Going from one to the next: does anything move that should stand (the seat, the
answers' right edge)?

Answer (The Lenovo Googlebook, 5 October 2026): yes. Lesson 3: the `search` mark; the recipe reads letters, `Tab`, Enter, a word, Enter (this
device's example is an app: `enter=true`) and ends well before the `tab` cap; the placeholder is the letters and
", then Tab". Lesson 4: the `calc` mark, `150 + 20%` and Enter, the caption ends "Today it stays.", the placeholder
is the sum. Each 252 dp. From one to the next the seat, the counter, the `tab` cap and "Skip" stand where they stood.

## 3. The example leads its list

`./bl debug first` for `example=`. Then, on lesson 2, `./bl debug keys LETTERS` and `./bl debug dump`.

Is the first row the example's app, of the kind App, with `*open` armed and `search` among its actions (with
`enter=false`: is it the Settings app)? Are the letters three or four? Write "yes" or what differs, not the app.

Answer (The Lenovo Googlebook, 5 October 2026): yes. Three letters; the first row is the example's app, of the kind App, with `*open` armed and
`search` among its actions.

## 4. Keys on a lesson's stage

On lesson 2 with the field empty: `./bl debug key enter` (nothing: the dump is unchanged), `./bl debug key tab`
(`armed=0`, "Skip" lit), `./bl debug key enter`: `first=L3 armed=-1 3/5`, still 252 dp. `./bl debug first`:
`done=[key, show, open]` or the like, with `open` in it.

Answer (The Lenovo Googlebook, 5 October 2026): yes. Enter: nothing. Tab: `armed=0`. Enter: `first=L3 armed=-1 3/5`, 252 dp, and `open` is in
`done=`.

## 5. Lesson 2: Enter is practice

`./bl debug first at l2`; `./bl debug keys LETTERS`; `./bl debug dump`: `due=L2 coach=opens`, and `first=none`
(the stage has given way to the list). `./bl shot l2-list`: the list stands in its real seats, row one at the
height where the seat stood; the footer's left end reads "2 of 5 · ⏎ opens it · practice: nothing opens", its
right end `tab` Actions and `esc` Close.

`./bl debug key enter`, and at once `./bl debug dump` (open the panel with `slow=4` for this): `flash=That opens
…` with the app's name, `pressed`, `due=L3`. `./bl shot l2-pressed` in the same two seconds: the armed slot a
little darker, the footer's word with its check. `./bl idle`: is the focused window still Booklight's panel, and
has no app opened? Then `./bl debug dump`: `first=L3 armed=-1 3/5`, `query=''`, the window 252 dp.

Answer (The Lenovo Googlebook, 5 October 2026): yes. With the letters typed: `due=L2 coach=opens`, `first=none`; the list stands in its real
seats with row one where the seat stood; the footer reads "2 of 5 · ⏎ opens it · practice: nothing opens", and at
its right end `tab` Actions and `esc` Close. Enter, at a quarter of the speed: `flash=That opens …` with the app's
name, `pressed`, `due=L3`; the armed slot is a little darker and the footer's word has its check. The panel is
still the window in front and no app has opened. Then `first=L3 armed=-1 3/5`, the field empty, 252 dp.

## 6. Lesson 3, from the letters to the word

`./bl debug first at l3`; `./bl debug keys LETTERS`; dump: `coach=to_search`. `./bl debug key tab`:
`armed=search`, `coach=into_app`. `./bl debug key enter`: `chip=appsearch:…:search`, `hint='Search …'`,
`due=L3`, `first=none`; nothing has opened. `./bl debug in lofi`; dump: `coach=searches`; the footer reads "3 of 5 ·
⏎ searches there · practice: nothing opens". `./bl debug key enter`: `flash=That searches …`; `./bl idle`: no app
has opened. A second later: `first=L4 armed=-1 4/5`, no chip.

Where `example=` says `enter=false` (or says none, as long as the example is not worked out; a settings page entered
in a lesson is practice then too, whichever lesson stands): `./bl debug keys s`, `./bl debug key tab` (the Settings chip),
`./bl debug in wifi`, dump: `coach=opens`; `./bl debug key enter`: `flash=That opens …`, and Settings has not
opened.

Answer (The Lenovo Googlebook, 5 October 2026): yes, step for step: `coach=to_search`; Tab: `armed=search`, `coach=into_app`; Enter: the app is
the chip, the placeholder is the app's own, `due=L3`, `first=none`, nothing has opened; the word typed:
`coach=searches`, and the footer reads "3 of 5 · ⏎ searches there · practice: nothing opens"; Enter: `flash=That
searches …`, no app has opened; then `first=L4 armed=-1 4/5`, no chip. Where Settings stands in (`enter=false`):
not run, this device's example is an app.

## 7. Lesson 4: the sum is copied, and the panel stays

`./bl debug first at l4`, `./bl debug pref suggestions off`; `./bl debug keys 150 + 20%`; dump: `coach=copies`,
the row's answer 180. `./bl debug key enter`: `flash=Copied`, `pressed`. A second later: the panel is still
there; `first=Q armed=-1 5/5`; the window 324 dp. Ctrl + V in the field: 180. Filmed or stepped with
`./bl open stay slow=4`: does the lower edge go from the list's height straight to 324, without a visit to 68?

Answer (The Lenovo Googlebook, 5 October 2026): yes. `coach=copies`, the row's answer 180. Enter: `flash=Copied`, `pressed`; then the panel is
still there with `first=Q armed=-1 5/5`, 324 dp; Ctrl + V in the field gives 180. The height, read from the dump
every half second at a quarter of the speed: 324 dp from the Enter on (the sum's list is 324 dp high here too), no
visit to 68. Not filmed.

## 8. Enter twice

`./bl debug first new`, `./bl debug first key`, `./bl debug first at l2`, `./bl debug first ran search`,
`./bl debug first ran sum` (lesson 2 is the last lesson left), `./bl debug pref suggestions off`; open;
`./bl debug keys LETTERS`; `./bl debug key enter2`.

`./bl idle`: no app has opened (the second Enter found the lesson over and its list still standing, and did
nothing). A second later: `first=Q`.

Answer (The Lenovo Googlebook, 5 October 2026): yes. After the two Enters: "That opens …", the panel in front, no app opened; then `first=Q`.

## 9. Typed on before the list gives way

`./bl debug first at l2`; `./bl open stay slow=4` (the half second is two seconds then); `./bl debug keys
LETTERS`; `./bl debug key enter`, and within those two seconds `./bl debug in x`.

Three seconds later: the field still holds the x, with its list (nobody emptied it under the typing hand; the hook
puts the x in the letters' place, where a key would add it to them); `due=L3`. `./bl debug type` with nothing after it: lesson 3's stage stands.

Answer (The Lenovo Googlebook, 5 October 2026): yes. The field holds the x with its list, `due=L3`; the field emptied, lesson 3's stage stands.

## 10. A lesson's thing out of turn

`./bl debug first at l2`; `./bl debug keys 150 + 20%`; dump: `coach=title` (the footer reads "2 of 5 · Open an
app"). `./bl debug key enter`: `flash=Copied`; a second later lesson 2 stands again, and `./bl debug first` has
`sum` in `done=`. After lessons 2 and 3 the question follows, or the choices: lesson 4 is not asked for.

Answer (The Lenovo Googlebook, 5 October 2026): yes. `coach=title`; Enter: `flash=Copied`; then lesson 2 stands again and `sum` is in `done=`.
That lesson 4 is then not asked for was not run on its own.

## 11. Everything else runs as every day

On lesson 2, something that is neither an app, a search inside one, a sum nor a settings page:
`./bl debug keys uuid`, `./bl debug key enter`. It is copied and the panel goes, as every day; nothing of another
app opens. At the next opening lesson 2 stands again, and `./bl debug first` says `opens=` one higher.

A settings page (`./bl debug keys wifi`, `./bl debug key down` to the page if it is not row one, `./bl debug key
enter`): where `example=` says `enter=true` the page opens and the panel goes, as every day (this opens a window
of another app: run it where that is allowed, and say who closed the window). Where it says `enter=false`, or
none yet, that Enter is practice: "That opens …", no page opens, and `./bl debug first` has `search` in `done=`.

Answer (The Lenovo Googlebook, 5 October 2026): `uuid`: copied, the panel goes, nothing of another app opens; at the next opening lesson 2
stands again and `opens=` went from 0 to 1. A settings page: not run (it would open another app's window).

## 12. The question, as a still, and its keys

`./bl debug pref suggestions off`, `./bl debug first at q`, `./bl open stay`, `./bl debug dump`, `./bl shot q`;
again dark.

`first=Q armed=-1 5/5`, 324 dp. Look: the `globe` mark; the title alone in the seat, on its centre line; the
text from x = 72 to the right margin, in strong ink, whole; the note under it in second ink; "Not now" and under
it "Agree" at the right edge, neither lit, a `tab` cap before them. `./bl debug key enter`: nothing, and
`./bl debug pref` still says `suggestions=false`. `./bl debug key tab`: `armed=0` ("Not now"); again: `armed=1`
("Agree"); `./bl debug key backtab`: `armed=0`. `./bl debug close`, open again: the question stands as it did, nothing lit,
`suggestions=false`.

Answer (The Lenovo Googlebook, 5 October 2026): yes, in both themes. `first=Q armed=-1 5/5`, 324 dp; the `globe` mark, the title alone in the
seat, the text whole in strong ink on three lines, the note under it in second ink, "Not now" and under it "Agree"
at the right edge with neither lit and a `tab` cap before them. Enter: nothing, `suggestions=false`. Tab:
`armed=0`, "Not now" lit; again: `armed=1`, "Agree"; Shift + Tab: `armed=0`. Closed and opened again: the question
stands as it did, nothing lit, `suggestions=false`. (The dark theme was looked at before the two answers changed
places; the light one again after.)

## 13. Agree, and Not now

On the question: `./bl debug key tab` twice, `./bl debug key enter`. `./bl debug pref`: `suggestions=true`. The
dump: `choices`, two rows, the window 268 dp. Then `./bl debug pref suggestions off`, `./bl debug first at q`,
`./bl debug key tab` once, `./bl debug key enter`: `suggestions=false`, and the choices stand all the same.

Answer (The Lenovo Googlebook, 5 October 2026): yes. Tab twice, Enter: `suggestions=true`, and the choices stand: two rows, 268 dp.
Suggestions off again, the question again, Tab once, Enter: `suggestions=false`, and the choices stand all the same.

## 14. No answer is given blind

`./bl debug pref suggestions off`, `./bl debug first at q`, no panel open. `./bl open stay opening=slow slow=4`
and at once, while the glass is still opening, `./bl debug key tab` and `./bl debug key enter`.

A click on an answer counts only once its screen has been in view for a moment, as the keys do (adb cannot click
here: for a person).

Once it has opened: `first=Q armed=-1`, and `./bl debug pref` says `suggestions=false`. The same on lesson 2
(`first=L2 armed=-1`, not skipped) and on K1 (`./bl debug first new`, `./bl debug pref key no`: no dialog comes).

Answer (The Lenovo Googlebook, 5 October 2026): yes. Tab and Enter sent while the glass was still opening (slowly, at a quarter of the speed)
changed nothing: the question stood with `armed=-1` and `suggestions=false`; lesson 2 stood with `armed=-1`, not
skipped; K1 stood, and no dialog came. Once open, Tab counted. A click on an answer: not run (for a person).

## 15. The question is never cut

Booklight in German; the search engine with the longest name (`./bl debug pref engine ID`, then put it back);
`./bl shot q-de`. How many lines has the text: three, four? Is its last word there, and is the note on two lines
at most? If the dump's window is taller than 324 dp: by how much, and is the text then whole?

Answer (The Lenovo Googlebook, 5 October 2026): Booklight in German with the engine that has the longest name: the text has three lines, its
last word is there, the note is on one line, 324 dp. A text of more than four lines could not be had here without
changing the system's font scale, so the question growing with its text was not seen.

## 16. The choices, as a still

`./bl debug first at c`, `./bl open stay`, `./bl debug dump`, `./bl shot c`; again dark.

`choices`, `selected=-1`, `first=none due=C`, 268 dp; the rows `Show your usual (…) [OTHER] {off} <*usual>` and
`Everything Booklight does (…) [?] <*enter>`. Look: row tops at 76 and 168; marks on x = 38, titles on 72; the
first row's text on two lines; at its right end "Off" and the switch, the switch ending at x = 700; at the second
row's right end a `?` cap; no row for the device's model; the footer's left end "Play by name and flight times
need your own key: window › Labs", its right end `⏎` Done and `esc` Close.

Answer (The Lenovo Googlebook, 5 October 2026): yes. `choices`, `selected=-1`, `first=none due=C`, 268 dp, the two rows as said (`{on}` here:
"Show your usual" is on on this device). The marks and the titles stand on their lines; the first row's text is on
two lines; at its right end the word and the switch, the switch ending at x = 700; at the second row's right end a
`?` cap; no row for the device's model; the footer reads the Labs line at its left end and `⏎` Done, `esc` Close at
its right. Light theme only.

## 17. The choices' keys

`./bl debug key down`: `selected=0`, the pill on row one. `./bl debug key enter`: `{on}`, the thumb at the
right, "On"; `./bl debug pref` says `zero=true`; the panel stays. `./bl debug key enter` again: off. (Leave the
switch as the device had it.) `./bl debug key up`: `selected=-1`; again: nothing. `./bl debug key tab`:
`selected=0`. `./bl debug key down`: row two, the footer `tab` Fill in. `./bl debug key enter`: `chip=help`, the
list of everything; `./bl debug first`: `run=NONE mark=true`.

Answer (The Lenovo Googlebook, 5 October 2026): yes, key for key. Down: `selected=0`. Enter: the switch goes to the other side, its word
changes, `zero=` follows, the panel stays; Enter again: back (left as the device had it). Up: `selected=-1`; again:
nothing. Tab: `selected=0`. Down: row two, the footer `tab` Fill in. Enter: `chip=help`, the list of everything;
`run=NONE mark=true`. Seen on the way: with the switch's row selected the footer's right end has only `esc` Close,
no word for what Enter does there.

## 18. The fold

`./bl debug first at c`, open, a moment, `./bl debug key enter`.

The rows go and the window is 68 dp; `ended`; `hint='Type ? for everything Booklight does'`; the field's `esc`
cap is back. `./bl debug first`: `run=NONE done=[] mark=true`. Close, open: the ordinary placeholder, and
whatever stood under the empty field before first run stands there again. Filmed or stepped with `slow=4`: does
the height go from 268 to 68 in one move?

Answer (The Lenovo Googlebook, 5 October 2026): yes. The rows go, 68 dp, `ended`, the ending's placeholder, the field's `esc` cap is back;
`run=NONE done=[] mark=true`. Closed and opened: the ordinary placeholder, and "your usual" under the field as
before first run. The height, read every half second at a quarter of the speed: 268, 103 on the way, 67, 68: one
move, with a dp of the spring's overshoot. Not filmed.

## 19. The choices are done however they are left

Each from `./bl debug first at c` with the panel open: `./bl debug keys a` (the typed list takes their place);
`./bl debug key esc`; `./bl debug close`. After each, `./bl debug first`: `run=NONE`.

Answer (The Lenovo Googlebook, 5 October 2026): yes: a typed letter, Esc and `close` each left `run=NONE`.

## 20. One opening, from "Your key works" to the bare field

`./bl debug first new`, `./bl debug pref key no`, `./bl debug pref suggestions off`; open; `./bl debug first
key` ("Your key works"); then with the hooks, or by hand: Go on, the three lessons each done, an answer to the
question, Enter on the choices.

Did the panel stay open from the first screen to the last? `./bl debug first` on the way: `opens=1` throughout.
Did any height change look like a jump, or cut a row or a line at the lower edge? By hand, with a stopwatch,
from "Go on" to the bare field: how many seconds (the product paper's target for the whole of first run is 60)?

Answer (The Lenovo Googlebook, 5 October 2026): by the hooks, at ordinary speed: the panel stayed open from K1 to the bare field. Heights on
the way: 252 (K1, "Your key works", the three lessons), 324 (the question), 268 (the choices), 68. `opens=1` on K1
and `opens=0` from the key's landing on, not 1 throughout: the landing makes that opening the run's first again.
No jump or cut was looked for by eye, and it was not timed by hand.

## 21. Three openings, then the run waits

`./bl debug first at l3`; `./bl open`, `./bl debug dump`, `./bl debug close`, three times: lesson 3 each time.
A fourth: the bare field. `./bl debug first`: `opens=4 parked=true`, `done=` as it was, `suggestions=` as it was.

Answer (The Lenovo Googlebook, 5 October 2026): yes. Lesson 3 in three openings, the fourth shows what stood there before first run;
`opens=4 parked=true`, `done=` and `suggestions=` as they were.

## 22. Heights, screen by screen

From the dump's window size, in dp: K4 252, each lesson 252, a lesson's list as any list of those rows, the
question 324 (or more, check 15), the choices 268, the ending 68. In `./bl shot` of each: is anything cut at the
lower edge, or is there glass with nothing on it under the last line?

Answer (The Lenovo Googlebook, 5 October 2026): K4 252, each lesson 252, the question 324, the choices 268, the ending 68; a lesson's list as
any list (568 with the example's letters, 324 with the sum). In the shots nothing is cut at the lower edge. Under
the question's three lines stands the room of a fourth, as its four-line box has it.

## 23. German

Booklight in German, every screen of this part. The longest lines: lesson 3's line under its title; the caption
„Zum Üben: nichts öffnet sich. Sonst schließt Enter auch Booklight.“ beside „Überspringen“; the coach line „4
von 5 · ⏎ kopiert das Ergebnis · Booklight bleibt nur heute“ beside `tab` Aktionen and `esc` Schließen (does the
counter go where the line has no room?); the question's note; the choices' two lines of text; the Labs line
beside `⏎` Fertig. Is anything cut in the middle of a word?

Answer (The Lenovo Googlebook, 5 October 2026): Booklight in German: the three lessons, the question, the choices, and the coach lines of
lessons 3 and 4. Nothing is cut in a word. The caption beside „Überspringen“ is whole; the coach line „4 von 5 · ⏎
kopiert das Ergebnis · Booklight bleibt nur heute“ stands whole with its counter beside `tab` Aktionen and `esc`
Schließen; the question's note is on one line; the choices' text is on two; the Labs line stands whole beside `⏎`
Fertig.

## 24. Animations off, and a screen reader

With the system's animations off: every screen whole in one frame; a lesson's list still gives way half a
second after its Enter; the switch and its word change in one frame. With a screen reader on: each stage said
once when it comes (the counter, the title, the line, the recipe a piece at a time, the caption, how to act);
the coach line when it changes; the choices with each row's state; "First steps are done" at the fold.

Answer (The Lenovo Googlebook, 5 October 2026): with the system's animations off (set for this check and put back): Tab counted right after
the start; the question and the choices stood whole a third of a second after they came; the switch and its word
had changed 0.15 s after Enter. Whether a lesson's list still stands for half a second could not be read: a hook's
answer takes longer than that. A screen reader: not run.

## 25. Nobody else sees a change

`./bl debug first off`, `./bl debug pref key seen`; open; `./bl debug keys 150 + 20%`; dump: `due=none`, no
`coach=`; `./bl debug key enter`: "Copied", and the panel closes, as every day. Ten openings by the keys: the
placeholder, the tips, the copy's line and "your usual" as before; `./bl debug pref` before and after says the
same but for what was set here.

Answer (The Lenovo Googlebook, 5 October 2026): yes. With no run: `due=none`, no `coach=`; Enter on the sum copies and the panel closes. Ten
openings by the keys: the ordinary placeholder and "your usual" each time, nothing of first run; `./bl debug pref`
the same before and after.

## 26. The HP Googlebook, with Alex

Checks 1, 5, 6, 7, 12, 16, 18 and 20 again there.

Answer: not run. The HP Googlebook is not used until Alex says so.

## 27. The recipe has its room

Lessons 2 and 3, in German and in English, with the example's app and with Settings standing in (`enter=false`):
`./bl debug first at l2`, `./bl shot`, then `l3`. Is every cap and every word of the recipe in view? Does anything
vanish or get cut at the row's end? Does the `tab` cap before "Skip" stand clear of it?

Answer (The Lenovo Googlebook, 5 October 2026): with the example's app, in English and in German: every cap and every word of the recipe is in
view, nothing vanishes or is cut, and the `tab` cap stands clear of lesson 3's last cap by more than a cap's width.
With Settings standing in: not run, this device's example is an app.

## 28. A pointer that rests where the answers come

With the pointer resting where "Agree" will stand, let the question come (`./bl debug first at q`, or from lesson 4).
Is "Agree" lit or armed before the pointer moves, and what does Enter do then? (Not drivable from adb: no hover;
for a person.)

Answer: not run. For a person: adb cannot rest a pointer.

## 29. The keys that skip do not agree

`./bl debug pref suggestions off`, `./bl debug first new`, `./bl debug first key`, `./bl debug first at l2`,
`./bl open stay`. With real keys, in one turn and as fast as they go: Tab, Enter, four times over (`adb shell input
keyevent KEYCODE_TAB KEYCODE_ENTER KEYCODE_TAB KEYCODE_ENTER KEYCODE_TAB KEYCODE_ENTER KEYCODE_TAB KEYCODE_ENTER`,
with the panel in front).

Tab, then Enter skips a lesson; pressed again at once they must not answer what comes next. How far did the four
pairs get, and is `suggestions=false`? Then the same pairs at a person's pace, a second apart, on through the
question: the first Tab there arms "Not now", and `suggestions=false` after its Enter.

Answer (The Lenovo Googlebook, 5 October 2026): the four pairs in one turn got as far as lesson 3: the first pair
skipped lesson 2, the other three were not taken; `suggestions=false`. At a person's pace: lesson 3 skipped, lesson
4 skipped, on the question Tab armed "Not now" and Enter left `suggestions=false` with the choices standing. Pairs
pressed on after that select the first of the choices and flip its switch, a pair at a time.

## What the checks found (5 October 2026)

- **Nothing opens in a lesson**, by any of the ways tried: Enter on the example's row, Enter on the search under
  its chip, two Enters in one turn (checks 5, 6, 8). The sum is copied and the panel stays (7).
- **No answer is given blind** (14), and suggestions go on by "Agree" alone (12, 13).
- **The keys that skip a lesson do not agree** (29). The review of the whole part found that Tab, Enter pressed four
  times over went skip, skip, skip, "Agree". Since then a screen's keys count only once that screen has been in view
  for a moment, and the question's first answer is "Not now".
- **Seen, for Alex**: with the switch's row selected the footer says nothing of what Enter does there (17).
- **Still open**: Settings standing in for an app (6, 27); a settings page in a lesson (11); a click on an answer
  while the glass opens, and a pointer resting where the answers come (14, 28); a text of the question's that needs
  more than four lines (15); the dark theme on the lessons and the choices (1, 16); the whole run by hand, timed and
  watched (20); a screen reader (24); the motion itself, filmed (7, 18); everything on the HP Googlebook (26).
