# First run: the last pass on a device (part 5)

*The checks for part 5 of the first-run plan (`docs/superpowers/plans/2026-10-05-first-run-5-the-last-part.md`), and
after them every check the three earlier lists left open (`first-run-key.md`, `first-run-lessons.md`,
`first-run-welcome.md`), gathered into one. Whoever runs a check writes its answer under it, with the date and "the
Lenovo Googlebook" or "the HP Googlebook". Until then an answer reads "not run".*

**Who runs what.** A check marked **[C]** is the coordinator's: it is run on the test device by adb and the debug
hooks. A check marked **[A]** is Alex's own: it needs a click on the launcher's icon or on another app's window,
another keyboard, the system's language or font scale, the network switched off, a shortcut set on a Googlebook
where Booklight has none yet, a screen reader's voice, the HP Googlebook, or a person's judgement.

**Before the first check**

- Choose the device by its model (`adb devices -l`), never by its place in the list; set `ANDROID_SERIAL`.
- Close the Booklight window, then `./bl install`. `./bl idle` before every `./bl open`: the panel takes the keyboard.
- After `./bl stop` the hooks are not heard until a panel has been opened once: `./bl debug …` says "no answer
  from the app" until then. So `./bl open stay` first (and `./bl debug close`, where no panel is wanted yet), and
  only then the hooks that set the state.
- Never `uiautomator dump`. No sign-in is made for a check, and no other app is opened by one. A capture or a film
  of the screen shows real windows: none is kept or committed. `./bl shot NAME` is the panel's own window, and
  `./bl backdrop` puts a window of test content (a white page, a dark terminal) behind the panel.
- This file is public: no serial, no build number, nothing about what is installed. The lessons' example and the
  show's first rows are apps of the device, and `./bl debug dump`, `./bl debug first` and `./bl debug first says`
  name them: an answer says "the device's example" or "the device's apps", never a name or its letters.
- Write down what `./bl debug pref` says before the first check (`opening=`, `dim=`, `theme=`, `suggestions=`,
  `zero=`, `sums=`) and put them back after the last one. The shortcut the test device already has for Booklight
  stays as it is: its row in the system's dialog is never removed.
- **In German.** A check that says "in German" is run with the app's own language set, not the system's: `adb shell
  cmd locale set-app-locales io.github.kuscher.booklight --user current --locales de`, and back with `--locales ""`
  when the check is done (`./bl sh 'cmd locale …'` is the same on the chosen device). The system's own words (its
  dialog, the key's name) stay in the system's language by this: check 42, with the system in German, is Alex's.
- **Real keys.** Where a check asks for real keys, `./bl sh input keyevent KEYCODE_ENTER KEYCODE_ENTER` sends them
  back to back, each a key event with its own time, as a hand's are; only ever into Booklight's own focused window
  (`./bl idle` says which window has the focus). `./bl debug key enter2` is Enter twice in one turn, with no key
  event: it shows what a second Enter does in the same moment, not what a key's own time does.
  **Real keys back to back are sent only where no typed list can stand**: Tab and Enter on a stage, and on the
  choices. Never a letter, Backspace and Enter in one call, and never Backspaces with Enter after them over a typed
  list: sent a few ms apart, the Backspace can overtake the letter (no hand types that fast), the field still holds
  text, and Enter runs that text's first row. On the key's step and on the question that is no practice: it opens
  what it opens on any day, another app included. (Only a lesson's own rows are practice.) What such a check wanted
  to know is read from a trace instead: `enter=` and `seen=` say frame by frame whether a key would count.
- **The system's dialog.** Enter on "Open Keyboard shortcuts" opens the system's own dialog over the panel, which
  holds on behind it (`./bl debug first` says `hold=UNDER`; the dump says `covered` for a screen that came under
  it). It is the system's window: no key is sent into it. It goes when the key lands (`./bl debug first press
  under` asks the system to put it away, as the real landing does), or by its own close button. `./bl debug close`
  closes the panel and leaves the dialog up alone: close the dialog by its own close button then, before the next
  check. A check that opens the dialog says so (10, 23, 28, 30).
- Check 1 comes first: the welcome's drawing changed, and the stage is drawn in a new way.
- **The stage's clock.** A screen of first run is not there in one frame: its parts come by marks, in ms after the
  screen came (`docs/design/first-run/motion.md` §3). `./bl debug first stage at MS` stands that clock still at MS;
  `./bl debug first stage` lets it run again; `./bl debug first stage as set_down|back|turn|after_list|lands|whole
  [at MS]` has the screen that stands come again in that way, in this moment. `./bl debug dump` says for the stage
  that stands `comes=` (how it came), `lead=` (when the answer Enter would run begins to show), `all=` (when the
  last of its answers does, where that is later), `end=` (when it is at rest), and `seen` (it takes every key and
  click now: 350 ms after all its answers began to show) or `enter` (so far it takes Enter on its armed answer
  alone: 350 ms after `lead`) or `covered` (the screen came under the system's dialog and is still under it: it
  takes no key, and counts 350 ms from when the dialog has gone). A still of a **turn** (one screen in another's
  place): stand the clock first (`first stage at MS`), then make the turn (`first at SCREEN`, or `first answer
  NAME`); what gives way fades in real time and is not in such a still.
- **The marks, for a screen set down as a panel opens**, depend on the speed of the opening, because the band waits
  for the glass to be at rest: it begins 120 ms after the gate at Fast, 162 at Medium, 324 at Slow. The numbers below
  are for Medium: checks 16, 17, 18 and 32 begin with `./bl debug pref opening medium` (and the setting is put back
  after them), and their times are those of a panel opened without `slow=`, which stretches every one of them. At
  another speed add the difference to everything from the band on.
- **A key hook after a screen came** is taken only once the dump says `seen` for that screen (350 ms after its
  answers began to show): wait for it, or the hook presses a key that does not count, as a hand's would not. The
  dump says `enter` while Enter on the armed answer counts already and the other keys do not yet: at the landing of
  the opening piece alone, where the armed answer comes 300 ms before the other (`lead=180 all=480`).
- **Times.** `./bl debug first trace N` takes the next N frames (1 to 2400; 120 unless said), a line each: `t=` ms
  since the first of them, `clock=` the stage's clock in ms (divide by the slow motion's factor), `h=` the window's
  height in px, `playing=`, `stage=`, `comes=`, `lead=`, `all=`, `enter=` and `seen=` (Enter on the armed answer
  counts; every key does), `glide=` (the show's highlight is on its way into the armed answer), `keydown=`,
  `checked=`, `laps=`, `held=` a height that is being kept, `hold=` (`under` while the system's dialog is over the
  panel), `rows=`. **Read a trace with `./bl trace`, never from `./bl logs`**: every `./bl debug` and every `./bl
  shot` clears the log before it runs, and `./bl logs` shows its last 40 lines, so a trace read from the log has
  lost whatever was taken before the last hook. The app keeps the frames itself until the next trace is started:
  `./bl trace` prints those in which something changed besides the two clocks (and the first and the last), `./bl
  trace all` every one, however many hooks were run in between. So a check goes: start the trace, give the keys by
  their hooks, wait until it must be over (N frames: at 120 frames a second, N/120 s), then `./bl trace`. Read the
  times from it, not from a film.
- **What a hook can time.** One `./bl debug` takes about half a second to arrive: two of them cannot fall inside a
  third of a second. Where a check needs a key inside a short span it says so one of two ways: real keys sent back
  to back in one `input keyevent` (their distance is a few ms, whenever the first one lands), or a panel opened
  with `slow=4`, `slow=8` or `slow=16`, which stretches every mark of a screen and every wait of motion that many
  times (the 350 ms a screen must have been in view, and the 0.7 s after the key has landed, are real time and
  are not stretched).
- `./bl open stay slow=4` stretches every spring, every tween and the stage's clock four times; `DARK=true ./bl open
  stay` is the dark theme. A film of the screen is cut to the panel's rectangle, looked at frame by frame, and
  deleted.
- `./bl debug first says` prints what first run told a screen reader in the open panel, and what every node of the
  panel says to one (its role, its words, its state). It needs no screen reader to be on.
- Lengths below are dp from the panel's left and top edges; the window's height in the dump is px (divide by the
  screen's density).

# This part's checks

## 1. The panel still opens, and nobody else sees a change [C]

`./bl debug first off`, `./bl debug pref key seen`, `./bl open stay`, `./bl debug dump`, `./bl shot plain`; again with
`DARK=true`. `./bl logs`: no crash. Then ten openings by the keys, typing nothing, and `./bl debug pref` before and
after.

The glass, the placeholder, the mark, the `esc` cap, the tips, the copy's line and "your usual" as before; the lap
of light a while after the opening, as before. The dump: `first=none`, `playing=none`, no `comes=`, no `later`, no
`laps=`. `./bl debug pref` says the same before and after: nothing was written. `./bl debug keys` with a letter or two
that find apps: the list is as it was, and no row called "First steps" is in it. Nor for the letters that begin its
name or one of its other words: `./bl debug keys f`, `fi`, `w`, `we`, `t`, `to`, `i`, `in`, each with `./bl debug
dump`: no row called "First steps" in any of them, whatever else the list holds (the row is offered only where it
was typed for: check 2). (Ten openings by the keys: `./bl open stay key` is a panel as the key opens it; `./bl
debug close` between two.)

Answer (The Lenovo Googlebook, 5 October 2026): yes. The panel in both themes as before: `first=none`, `playing=none`, nothing of first run in
the dump; ten openings as the key makes them, typing nothing: the same; `./bl debug pref` the same before and
after; no crash. A letter or two that find apps (`f`, `fi`, `w`, `we`, `t`, `i`): the list as it was, no row "First steps".

## 2. "First steps", typed [C]

`./bl debug first off`, `./bl debug pref key seen`, `./bl open stay`, `./bl debug keys "first steps"`, `./bl debug
dump`, `./bl shot steps-row`; then `./bl debug key enter`, a moment later `./bl debug dump`, `./bl shot steps-k4`,
`./bl debug first`. In German (the app's language set as said above): the same with `erste schritte`. Also
`./bl debug keys tour`, `welcome`, `intro`; `./bl debug keys fir` and `./bl debug keys "first s"`; and, for what
does not find it, `./bl debug keys fi`, `tou`, `welc`, `intr`, `steps`. In German `ers`, `willkommen`,
`einführung`; and `er`, `will`.
Then `./bl debug guide`, which tries every example of the list of everything. And, in the panel in which "Your key
works" now stands, hands off for four seconds, watched or filmed.

The row "First steps" is row one, with the `again` mark and "Booklight" where a row's kind stands. Enter: the panel
stays, the field is emptied, the list fades and "Your key works" stands under it with "Go on" lit and "Change the
key" under it; 252 dp; no welcome and no show. `./bl debug first`: `run=REPLAY`, `done=[show]`, `opens=1`,
`screen=K4`, and `suggestions=`, `sums=`, `key=` as they were. The dump: `first=K4 armed=0`, `comes=after_list`.
With `./bl debug pref key no` before it: the key's first screen instead, "1 of 5" (or "of 4" with suggestions on).
The row is there for three letters or more that begin its name (`fir`, `first s`; in German `ers`) and for one of
its other words in full (`tour`, `welcome`, `intro`; in German `willkommen`, `einführung`), in any case; it is not
there for less (`fi`), for another word begun (`tou`, `welc`, `intr`; `will`) or for a later word of its name
(`steps`).
`./bl debug guide`: every entry of its answer begins `ok`, none `BAD`, and `ok first 'first steps'` is among them
(in German `'erste schritte'`).
Left alone on "Your key works": no light runs round the glass. The lap that an ordinary panel has a while after it
opened does not come over a stage, also in a panel that opened with no run in it.

Answer (The Lenovo Googlebook, 5 October 2026): yes. "First steps" is row one for its own name, with the `again` mark; Enter: the panel stays,
the field is emptied and "Your key works" stands with "Go on" lit, 252 dp, no welcome and no show;
`run=REPLAY done=[show] opens=1 screen=K4`, the switches and the key as they were; the dump `first=K4 armed=0
comes=after_list`. With no key known: the key's first screen, "1 of 4" (suggestions are on on this device). In
German (the app's language): „Erste Schritte“, the same. `tour`, `welcome` and `intro` typed in full find it, as do `fir` and `first`; one or two letters do not. `./bl debug guide`
in a process started in that language: `ok first 'first steps'` and `ok first 'erste schritte'`; every other line
`ok` but `clip` (the clipboard held nothing fresh) and, in German, `notes` (its German example finds the web's
row: not this part's, the line was not touched). Seen: the row's armed action reads "Open" with the "opens" mark,
though it stays in the panel.

## 3. The Enter that asked for it answers nothing [C]

As check 2, with `./bl debug key enter2` in place of `key enter` on the row (Enter twice in one turn); then `./bl
debug first`. And with real keys: the words typed (`./bl debug keys "first steps"`), then `./bl sh input keyevent
KEYCODE_ENTER KEYCODE_ENTER KEYCODE_ENTER`, three Enters back to back into Booklight's own window, and `./bl
debug first`. (Enter runs row one, whatever it is: only once `./bl debug dump` has shown that row one is "First
steps", which opens nothing.)

`screen=K4` still, both times: no Enter after the first was "Go on". Half a second later Enter is "Go on".

Answer (The Lenovo Googlebook, 5 October 2026): yes. With `key enter2`, and with three real Enters back to back: `screen=K4` still. An Enter
half a second later went on to lesson 2.

## 4. "First steps" where a run is unfinished [C]

`./bl debug first new`, `./bl debug first shown`, `./bl debug pref key seen`, `./bl debug first at l3`, `./bl open
stay`; `./bl debug keys "first steps"`, `./bl debug key enter`, `./bl debug first`, `./bl debug dump`. Then with the
usual rows standing (`./bl debug pref zero on`, an installation with a history) and with a tip standing.

Lesson 3 stands again where it stopped, with its counter: `run=NEW`, `screen=L3`, `opens=1`; nothing that was done
is undone. Where the usual rows or a tip stood under the empty field, the stage has their place and they do not
come back in this opening.

Answer (The Lenovo Googlebook, 5 October 2026): yes. With the run stopped at lesson 3: lesson 3 stands again, `run=NEW screen=L3 opens=1`,
what was done kept, `comes=after_list`. With the usual rows or a tip standing under the field: not run.

## 5. "First steps" from the window [C]

`./bl debug first off`, `./bl debug pref key seen`, `./bl window start`, `./bl wshot start`, `./bl debug window`.
With the keys in the window, go to the row "First steps" in the Tips group and press Enter (the keys are sent into
Booklight's own window only). Then `./bl debug first`, `./bl debug dump`. Close the panel with `./bl debug close`
three seconds in, `./bl wshot start-2`.

The row stands after "Show the tips again": the `again` mark at 16 dp, "First steps" and "The key, three things to
try, your choices. About a minute." from 52 dp, the "opens" mark ending 16 dp from the row's end. Enter: a panel
opens over Booklight's own window and plays the whole of it, the welcome first, at the speed that is set (not at
Slow): `run=REPLAY`, `overture=WELCOME` before it began, `playing=welcome`; the last start was neither the key nor
the icon (`./bl debug first` ends in `last start: … key=false icon=false`; the stored `key=` and `icon=` before it
are other things: whether a key is known, and whether the icon's first click was used). It lands in "Your key
works". Back in the window, with the run unfinished: the row is called "Go on with the first steps" and "1 of 5" (or
"of 4") stands before its mark; Enter on it then opens the panel on the screen the run stopped at, without the
welcome.

Answer (The Lenovo Googlebook, 5 October 2026): yes, by the keys. The row stands in the Tips group after "Show the tips again" with its mark,
its two lines and the "opens" mark at its end, and takes the focus ring. Enter: a panel over Booklight's own
window plays the welcome (`playing=welcome`, 468 dp), `run=REPLAY`, `last start: … key=false icon=false`. Closed
three seconds in, the window has the focus again, and the row reads "Go on with the first steps" with "1 of 4"
before its mark; Enter on it opens the panel on "Your key works" without the welcome. An Enter pressed a second
time at once goes to the panel, which has the focus, and is the welcome's cue. Two clicks of a pointer on the
row, and the look of the desk's dim over Booklight's own window: not run.

## 6. The same row, as its hook writes it [C]

`./bl debug first off`, `./bl debug pref key seen`, `./bl debug first again`, `./bl debug first`, `./bl open stay`.

`run=REPLAY done=[] opens=0`, `overture=WELCOME slow=false`; the panel plays the piece. `./bl debug first again`
while a run is unfinished changes nothing but `opens=0`.

Answer (The Lenovo Googlebook, 5 October 2026): yes. `run=REPLAY done=[] opens=0`, `overture=WELCOME slow=false`; the panel plays the piece.
`first again` while the run is unfinished changed nothing but `opens=0`.

## 7. Asked for again on an installation that has been lived in [C]

`./bl debug pref sums off`, `./bl debug pref suggestions on`, `./bl debug pref key seen`, `./bl debug first again`,
`./bl open stay`; let it play; `./bl debug dump` during the show; then Enter on "Go on", and Tab, Enter through the
lessons, with `./bl debug first` after each. Put `sums` and `suggestions` back.

The show goes from the apps straight to the example flight: no sum is typed and no answer row comes; the flight's
first letter follows the last Tab by about 220 ms. It lands in "Your key works", and the highlight travels into "Go
on". The counter says "1 of 3": the key, two lessons, no sum's lesson and no question; after lesson 3 the choices
come. `suggestions=true` and `sums=false` throughout: no switch was touched.

Answer (The Lenovo Googlebook, 5 October 2026): yes. The show goes from the apps (five rows, 400 dp) to the example flight (256 dp) and the
grid (376 dp): no sum is typed. It lands in "Your key works", "1 of 3", the highlight travelling into "Go on".
"Go on": lesson 2, "2 of 3"; Tab, Enter: lesson 3, "3 of 3"; Tab, Enter: the choices. `suggestions=true
sums=false` throughout. The 220 ms between the last Tab and the flight's first letter: not measured.

## 8. The word "Not now" leaves behind [C]

`./bl debug first new`, `./bl debug first shown`, `./bl debug pref key no`, `./bl open stay`; once the dump says
`seen`, `./bl debug key tab`, `./bl debug key enter`; `./bl debug dump`, `./bl shot later`, `./bl debug first`. In
German too (the app's language).
Then `./bl debug keys x`, `./bl debug type` with nothing after it, `./bl debug dump`. Close; open again.

The stage folds to the bare field, 68 dp, and its placeholder is "First steps wait in Booklight’s window" (German:
„Die ersten Schritte warten in Booklights Fenster“); the dump says `later`; `run=NONE mark=true`. Typed over and
emptied, the placeholder is the same again. At the next opening it is the ordinary one.

Answer (The Lenovo Googlebook, 5 October 2026): yes, in English. After "Not now": the bare field, 68 dp, the placeholder "First steps wait in
Booklight’s window", `later`, `run=NONE mark=true`. Typed over and emptied: the same placeholder. At the next
opening: the ordinary one. German: not run.

## 9. A run that has used up its openings says the same, once [C]

`./bl debug first new`, `./bl debug first shown`, `./bl debug pref key seen`, `./bl debug first at l2`; then four
times: `./bl open stay`, `./bl debug dump`, `./bl debug close`. A fifth time.

Openings one to three show lesson 2. The fourth shows the bare field with "First steps wait in Booklight’s window"
and `later`; `./bl debug first`: `opens=4 parked=true`. The fifth: the ordinary placeholder, no `later`. In the
window the row says "Go on with the first steps" and "2 of 5".

Answer (The Lenovo Googlebook, 5 October 2026): yes. Openings one to three show lesson 2 (`comes=set_down`, then `back` twice); the fourth
shows the placeholder "First steps wait in Booklight’s window" with `later`, `opens=4 parked=true`; the fifth the
ordinary placeholder. The window's row in that state: not looked at.

## 10. The Start page tells the way to a key as first run does [C]

`./bl debug pref key no`, `./bl window start`, `./bl wshot key`, `./bl debug window`; in German too (the app's
language: the system's own two words stay as the system has them). Then, with the keys in the window, Enter on
"Open Keyboard shortcuts"; a capture cut to the dialog's own bounds; `./bl debug window helper close` (the dialog
is the system's window: no key is sent into it).

Under "No key yet": two caps, `Action` and the Quick Insert key by the system's own name, on the text's edge; under
them "… is left of A · or any keys you like". No "Alt", no "Space", no "K", no "or". The row "Open Keyboard
shortcuts" says "Click Customize, type Booklight, click +, press your keys, click Set shortcut. Then press your keys
once more." with the dialog's own two words. The dialog opens on Booklight's page, which begins with "A shortcut for
this app, in five steps" and its five rows, each ending in the suggested keys, and the window's own keys after them.

Answer (The Lenovo Googlebook, 5 October 2026): yes, in English. (The window reads whether a key is known when it is resumed, as before this
part: it was opened after `pref key no`.) Under "No key yet": two caps, `Action` and `Quick Insert`, on the
text's edge, and "Quick Insert is left of A · or any keys you like"; no "Alt", "Space", "K" or "or". The row "Open
Keyboard shortcuts" with its sentence. Enter on it: the system's dialog on Booklight's page, which begins "A
shortcut for this app, in five steps" with five rows, each ending in the Action key's glyph and Quick Insert,
then the window's own keys. Closed with `./bl debug window helper close`. German: not run.

## 11. The welcome: no ring as the night falls and lifts [C]

A panel that began with the piece and was landed with Esc (a real first opening, then `./bl debug key esc`); in it
`./bl debug first welcome at MS` and `./bl shot ring-MS` for MS = 350, 450, 550, 700 (the night falling) and 3460,
3520, 3580, 3660, 3740 (lifting). Light and dark theme; over the backdrop's white page and over its dark one.

In no picture a ring, an arc or an edge round the middle of the glass. At 700 the glass is a little darker towards
its corners, with no line where that begins. From 3580 on, and before 500, there is no darker shade at the corners
at all: the glass is one even veil on its way between the night and the theme's own.

Answer (The Lenovo Googlebook, 5 October 2026): no ring, arc or edge in any of the nine pictures, in the light and in the dark theme; from
3520 on the glass is one even veil (the same grey from side to side). With a still's contrast stretched to the
limit, the falling night (450 to 600) shows its gradient as bands of one grey level each; at ordinary contrast
none is seen. Over the backdrop's pages: not run.

## 12. The welcome: no words before the lamp's light reaches them [C]

`./bl debug first welcome at 850`, `./bl shot strike`; `at 900`, `at 1000`, `at 1090`.

At 850 nothing stands under the field: no title, no line. At 900 and 1000 they show more and more, dimly, as the
light floods the field from the caret; at 1090 they show dimly, whole, and the seam of light stands on the middle.

Answer (The Lenovo Googlebook, 5 October 2026): yes, in both themes. At 850 nothing readable stands under the field (the title is there at
a fraction of a percent); at 900 and 1000 dimly more; at 1090 dim and whole, the seam on the middle.

## 13. The show's first rows come with their highlight [C]

A real first opening (`./bl debug first new`, `./bl debug pref key no`, `./bl open stay`) with `./bl debug first
trace 600` started in the welcome's last second (it stands for about three and a half), then `./bl trace` once the
show has begun; and a real first opening filmed with `slow=4`; `./bl debug first show at apps`, `./bl shot apps`.

In the frames where the handle has reached row one's seat and the first letter lands: in every frame exactly one
highlight there, and from the first frame in which it is the list's own pill, row one's icon and name stand in it,
whole. No frame shows the pill empty, and none shows the seat without a highlight. Rows two to five rise after it,
22 ms apart. `./bl trace`: `playing=show` and `rows=5` (or fewer) begin in the same line.

Answer (The Lenovo Googlebook, 5 October 2026): yes. In a film at `slow=4`: one highlight in every frame from the knife folding to the first
rows; in the first frame that has the letter, row one's icon and name stand whole in it; rows two to five rise
after. `./bl trace`: `playing=show` and `rows=5` begin in the same line. Seen, for Alex: before that, the handle
rests in row one's seat, empty, for about a fifth of a second at the speed of every day, while Booklight's mark
comes up.

## 14. Booklight's mark comes up before the first letter [C]

`./bl debug first welcome at 3660`, `./bl shot mark-1`; `at 3740`, `./bl shot mark-2`; `at 3800`.

At 3660 the mark's seat (centred on x = 38, y = 34) is empty. At 3740 Booklight's mark stands in it or is coming
up; at 3800 it stands, and the head's light has closed to the caret. In a film: the mark is there before the first
letter, not with it.

Answer (The Lenovo Googlebook, 5 October 2026): yes. At 3660 the mark's seat is empty; at 3740 the mark stands (in the dark theme the head's
light is still closing over it); at 3800 it stands and the light has closed to the caret.

## 15. A cue with nothing to show [A]

On a device where no app's name begins with a letter (none is known): Enter on "Show off" sets the key's step down
at once; left alone, the welcome hands over to the key's step when it has stood, without a second's wait. By
reading: core `FirstRun.pressed` and `handOver`, with their tests.

Answer: not run

## 16. The key's step is set down part by part [C]

`./bl debug pref opening medium` (put back after check 18), `./bl debug first new`, `./bl debug first shown`, `./bl
debug pref key no`, `./bl open stay` (the piece counts as shown: no welcome; no `slow=`); `./bl debug dump` at once.
Then `./bl debug first stage as set_down at MS`, `./bl shot t1-MS` for MS = 0, 120, 250, 340, 420, 600. In motion:
`./bl debug first stage as set_down`, filmed with `slow=4`. Once with `./bl debug first update` in place of `new`
and `shown` (the run of an installation that was there before).

The dump: `first=K1 armed=0 1/5 … comes=set_down lead=382 end=552` (Medium). At 0: the glass at 252 dp with nothing
under the field. At 120: the disc with the key's mark there, the title and its line nearly, the counter coming, each
rising into its place; nothing in the band. At 250: `Action` has all but come up, the "+" is coming, no second cap.
At 340: `Action` and the "+", the second cap coming up; no caption, no answers. At 420: both caps; the caption is
fading in, the two answers are rising with the highlight on the first. At 600: the screen exactly as
`first-run-key.md`'s check 1 has it. An update's run is the same without a counter. In no picture is anything cut by
the lower edge, and nothing ever stands lower or further right than it does at rest but the 12 dp a part rises from.
In motion: left to right and top to bottom, one voice at a time; a cap comes up as a key that is let go.

Answer (The Lenovo Googlebook, 5 October 2026): yes, at Medium. `first=K1 armed=0 1/4 comes=set_down lead=382 end=552` ("of 4": suggestions
are on on this device). The six pictures as said: the bare glass at 252 dp; the disc, title, line and counter;
`Action` and the "+" coming; the second cap coming; the caption fading in and the answers rising with the
highlight on the first; at rest at 600. Nothing cut by the lower edge. Not run: the film, and an update's run.

## 17. It takes a key only once its answers are in view [C]

In the panel of check 16 (the opening at Medium): `./bl debug first trace 200`, `./bl debug first stage as
set_down`, `./bl debug first`; then `./bl trace`. Then keys, in a panel like it opened with `slow=4` (`./bl open stay
slow=4`: the screen then takes Enter 1.9 s after it came, 382 × 4 + 350): `./bl debug first stage as set_down`, at
once `./bl debug key enter2`, and `./bl debug first`; and again with real keys in place of the hook: `./bl sh input
keyevent KEYCODE_ENTER KEYCODE_ENTER KEYCODE_ENTER`. (A hand that presses Enter again and again from the key that
opens the panel until the screen stands is Alex's: check 51.)

In the trace `enter=false` and `seen=false` until `clock` has passed `lead` + 350 (732 at Medium; by `stage as
set_down` in an open panel the band begins as at an opening, `lead=382`), and both true after. The Enters given
while the screen is being set down do nothing, and none is kept: `hold=NONE helper=0`, and no dialog comes a
second later either.

Answer (The Lenovo Googlebook, 5 October 2026): yes. In the trace `enter=false` and `seen=false` until `clock` 728, true after. At `slow=4`
`key enter2` and three real Enters during the set-down: `hold=NONE helper=0`, and no dialog two and a half
seconds later either.

## 18. A screen come back to arrives as rows do [C]

(The opening at Medium, as in check 16; put the setting back after this check.) `./bl debug first new`, `./bl debug
first shown`, `./bl debug pref key no`, `./bl open stay`, `./bl debug close`, `./bl open stay`, `./bl debug dump` at
once; `./bl debug first stage as back at MS`, `./bl shot back-MS` for MS =
40, 90, 150, 300. The same on lesson 3 (`./bl debug first at l3` before the two openings).

`comes=back lead=110`. At 40: the disc coming, hardly anything else. At 90: the seat nearly whole; the caps (or the
whole recipe) beginning to come, all together: nothing is written a letter at a time. At 150: the band there, the
caption and the answers coming. At 300: at rest.

Answer (The Lenovo Googlebook, 5 October 2026): yes for the key's first screen: `comes=back lead=110`; at 40 the disc and the seat coming;
at 90 the seat nearly whole and both caps coming together; at 150 the band there, the caption and the answers
coming; at 300 at rest. Lesson 3: not run.

## 19. A lesson is set down, and its recipe is written [C]

On "Your key works" (`./bl debug pref key seen`, `./bl debug first at k4`, `./bl open stay`), half a second in:
`./bl debug first stage at MS`, then `./bl debug key enter` ("Go on"), `./bl shot go-MS`, for MS = 100, 200, 320,
420, 600; between two of them `./bl debug first at k4` and `./bl debug first stage`. In motion with `slow=4`.

`first=L2 comes=turn`, and `lead=` by the length of the device's example: the recipe is written from 120, a letter
every 40 ms, the Enter cap 66 ms after its last letter, the caption 66 after the cap, Skip 22 after the caption:
`lead` = 274 + 40 × (letters − 1), which is 394 for an example of four letters and 354 for one of three (`./bl debug
first` says the example; its letters are counted, never written down). In every picture the seat's disc stands where
it stood and the title, the line and the counter are lesson 2's. At 100: nothing in the band yet ("your key" and its
check have gone). At 200: the recipe's first three letters (one every 40 ms from 120). At 320, for four letters: its
letters whole, the Enter cap beginning to come up. At 420: the cap up, the caption fading in, Skip and its `tab` cap
beginning to rise (an example of three letters is 40 ms ahead from the cap on). At 600: lesson 2 as
`first-run-lessons.md`'s check 1 has it. In motion: the words roll one after the other (title, line, counter), the
mark's symbol gives way to the new one, "Go on" fades where it stood, its slot darker from the frame of the press
and its highlight staying on it, and the recipe is typed out.

Answer (The Lenovo Googlebook, 5 October 2026): yes, from one film at `slow=4` and the dumps. `first=L2 comes=turn lead=354` (the device's
example has three letters). "Go on" is a little darker from the frame after the press and fades where it stood;
the title, the line and the counter roll to lesson 2's; the seat's disc stays; the recipe is written a letter at
a time, then its Enter cap, the caption, and Skip with its `tab` cap. The five stills: not made.

## 20. Skip: what stays does not move [C]

On lesson 2: `./bl debug key tab`, half a second later `./bl debug key enter`; filmed with `slow=4`; `./bl debug
dump`.

`first=L3 comes=turn lead=0`. "Skip" and the caption stay exactly where they stand and do not fade; Skip's slot is
darker in the frame of the press and lets go; its highlight fades and the `tab` cap comes back; the old recipe
fades and lesson 3's is written from the left (how long that takes is the example's length: check 19).

Answer (The Lenovo Googlebook, 5 October 2026): yes, from the same film. `first=L3 comes=turn lead=0`. Skip and the caption stay where they
stand; Skip's highlight lets go; the old recipe fades and lesson 3's is written from the left.

## 21. After a lesson's Enter the next lesson is set down [C]

On lesson 2, type the device's example (`./bl debug first` says it; never write it down), `./bl debug first trace
200`, `./bl debug key enter`; `./bl debug dump` a second later, then `./bl trace`. For stills: `./bl debug first
stage at MS` before the Enter, MS = 60, 130, 300. Then the same once more with the example typed early, in a panel
opened with `slow=4` (lesson 2 then takes about two seconds to come into view): `./bl debug first at k4`, wait for
`seen`, `./bl debug key enter` ("Go on"), and at once `./bl debug keys` with the example, before a dump would say
`seen` again; `./bl debug key enter`, and the dump three seconds later.

The slot shows as pressed and the footer says "That opens …"; 520 ms later the list gives way and lesson 3 is set
down: `comes=after_list`. In the trace `h=` goes from the list's height to 252 dp without a line at 68 dp's height.
At 60: nothing of the stage yet (the rows are fading). At 130: the disc and the title coming. At 300: the seat
whole and the recipe's first letters; its caps not yet. Typed early, it is the same: lesson 3 is set down part by
part (`comes=after_list`, not `whole`), and takes a key 350 ms after its Skip began to show.

Answer (The Lenovo Googlebook, 5 October 2026): yes for the first half. After the Enter on the example's row the list gives way and lesson 3
is set down (`comes=after_list`); in the trace `h=` goes from the list's height to 252 dp with no line at 68.
The stills, and the example typed early at `slow=4`: not run.

## 22. Typing on a stage: the lower edge never visits the bare field [C]

On lesson 2: `./bl debug first trace 120`, then `./bl debug keys x`, then `./bl trace`; and on the question (`./bl
debug first at q`): the same. Filmed with `slow=4`. Then on lesson 2 a space alone: `./bl debug keys " "`, `./bl
debug dump`.

In the trace `h=` goes from the stage's height (252 dp, 324 for the question) to the list's in one move; no line
has the bare field's 68 dp, and `held=` shows the kept height for the frame or two before `rows=` is more than 0.
In the film the stage fades where it stands and the rows rise over it; the letter is in the field in the frame of
the key. `./bl debug type` with nothing after it: the stage is back whole, in one fade, and nothing is written
again. After the space alone the glass is the bare field's 68 dp at once: no height is held over nothing.

Up on a stage brings the last text back, and its list comes the same way. On lesson 2: `./bl debug keys x`, `./bl
debug close` (the letter is kept as the last text), `./bl open stay`, and once the dump says `seen`: `./bl debug
first trace 120`, `./bl debug key up`, `./bl trace`, `./bl debug dump`. The field holds the letter again, its list
stands, and `h=` went from 252 dp to the list's height with `held=252` in between and no line at 68 dp. (With no
last text kept, Up does nothing on a stage.)

Answer (The Lenovo Googlebook, 5 October 2026): yes on lesson 2. In the trace 252 dp is kept (`held=252` for a frame), then `rows=8` and the
glass grows to the list: no line at 68. Emptied: lesson 2 back, `comes=whole`. A space alone: 68 dp at once. Up
with a last text kept: the text is back and the glass goes from 252 dp to its list's height without a visit to
68. The question, and the film: not run.

## 23. A letter typed while a screen is being set down [C]

In a panel opened with `slow=4` (the set-down then takes two seconds, and a hook, which arrives half a second
later, falls well inside it): `./bl debug first stage as set_down`, at once `./bl debug keys x`, then `./bl debug
first trace 200`, `./bl debug type` with nothing after it; `./bl debug dump`, `./bl trace`; a second later `./bl
debug key enter`. Filmed. (By hooks alone: no real keys over a typed list, as said in "Real keys". Two hooks are
half a second apart, so what happens "at once" is read from the trace.)

The caps that had not come when the letter was typed never come while the stage fades. Emptied, the screen stands
whole: `comes=whole`. In the trace, from the line in which `stage=` is the key's screen again, `enter=false` and
`seen=false` for 350 ms: an Enter pressed at once would answer nothing. The Enter a second later is the screen's
own: on the key's first screen it opens the system's dialog (`hold=UNDER` half a second later). The dialog is then
closed by its own close button, and only after that the panel (`./bl debug close` alone leaves the dialog up).

Answer (The Lenovo Googlebook, 5 October 2026): in part, with an earlier build of this part and at the speed of every day: emptied, the
screen stands whole (`comes=whole`), and an Enter a second later asked for the system's dialog, as it should by
then. The trace's 350 ms after the screen is back, at `slow=4`, and the film: not run.

## 24. The sum is copied, and the question rises [C]

First `./bl debug pref sums on` (there is no sum's lesson without them) and `./bl debug pref suggestions off` (no
question is asked while they are on); both are put back after check 26. On lesson 4 (`./bl debug first at l4`):
`./bl debug keys "150 + 20%"`, `./bl debug first trace 240`, `./bl debug key enter`, a second later `./bl trace`.
For stills: `./bl debug first stage at MS` before the Enter, MS = 60, 110, 150, 210, 400. Filmed with `slow=4`. And
on lesson 2, where the lesson goes on standing: `./bl debug keys "150 + 20%"`, `./bl debug key enter`, filmed with
`slow=4`.

"Copied" with its check takes the coach line's seat, and the two are never drawn over each other, in either
direction: the coach line fades out (80 ms; a third of a second at `slow=4`) and only then does "Copied" begin to
fade in, on lesson 4, where the lesson is over with that Enter, as on lesson 2, where it stands on. On lesson 4, 520
ms later the field is empty, the rows fade, and `h=` goes from the list's height to the question's 324 dp without
68. The question's parts rise in reading order: at 60 nothing yet, at 110 the disc and the title coming, at 150 the
counter and the first of the text too, at 210 the text there and the note and the answers beginning with their `tab`
cap, at 400 the whole question, nothing lit. `comes=after_list lead=202`.

Answer (The Lenovo Googlebook, 5 October 2026): yes, by the dumps and the trace. Lesson 4 `comes=set_down`; the sum typed: its list, 324 dp,
the coach line "copies"; Enter: 520 ms later the question, `comes=after_list lead=202`, "5 of 5"; `h=` stays at
324 dp (the list's height and the question's are the same here) and never 68. "Copied" against the coach line,
and the stills: not filmed.

## 25. The question's keys count from its answers [C]

(`./bl debug pref sums on` and `./bl debug pref suggestions off` first, as for check 24, and put back after check
26.) On lesson 4's stage: `./bl debug key tab`, half a second later `./bl debug key enter` (Skip), and at once `./bl
debug key tab`, `./bl debug key enter`; `./bl debug first`. Then with real keys, from lesson 2 (`./bl debug first at
l2`, and wait for `seen`): `./bl sh input keyevent KEYCODE_TAB KEYCODE_ENTER KEYCODE_TAB KEYCODE_ENTER KEYCODE_TAB
KEYCODE_ENTER KEYCODE_TAB KEYCODE_ENTER`, Tab and Enter four times over, back to back; `./bl debug first`.
Then the question that came while the field held text, in a panel opened with `slow=4` (the list then stands for
two seconds after its Enter): on lesson 4 `./bl debug keys "150 + 20%"`, `./bl debug key enter`, at once `./bl
debug in "150 + 20%1"` (typed on, inside those two seconds: `in` adds to what the field holds, `keys` would empty
it first), three seconds later `./bl debug dump`; then `./bl debug first trace 200`, `./bl debug type` with
nothing after it, `./bl debug dump`, `./bl trace`, and once the dump says `seen`, `./bl debug key tab`, `./bl debug
dump`, `./bl debug first`. (The field is emptied by the hook and the third of a second is read from the trace. No
Backspaces are sent as real keys with Tab and Enter after them: one that is overtaken leaves the list standing,
and Enter then runs its first row for real.)

The question stands (`comes=turn lead=196`) and has no answer: `screen=Q`, `suggestions=false`. With real keys the
first Tab and Enter skip lesson 2 and the others, sent in the same moment, fall on a screen that has just come:
`screen=L3`. However they are timed, suggestions are never on. The question that came under typed text: the dump
before the field is emptied says `first=none due=Q` and the typed list stands (it did not give way); emptied, the
question stands whole (`comes=whole`) and came into view in that moment: in the trace, from the line in which
`stage=` turns to `Q`, `enter=false` and `seen=false` for 350 ms, so a Tab and an Enter pressed with the last
Backspace would reach nothing; the dump right after the emptying says `armed=-1`, and `screen=Q` stays. A third of
a second later (`seen`) Tab arms "Not now".

Answer (The Lenovo Googlebook, 5 October 2026): yes for its first two halves. Skip on lesson 4 and at once Tab, Enter: `screen=Q`,
`suggestions=false` (`comes=turn lead=196`). Real keys, Tab and Enter four times back to back from lesson 2:
`screen=L3`, `suggestions=false`. The question that came under typed text (`slow=4`): not run.

## 26. The question gives way to the choices [C]

On the question: `./bl debug key tab`, half a second later `./bl debug first trace 120`, `./bl debug key enter`
("Not now"), a second later `./bl trace`. Filmed with `slow=4`. Then the choices' second row in their first third
of a second, by real keys: on the question again (`./bl debug first at q`, wait for `seen`), `./bl debug key tab`,
then `./bl sh input keyevent KEYCODE_ENTER KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN KEYCODE_TAB` ("Not now", Down to
the second row, and Tab on it, back to back); `./bl debug dump`, `./bl debug first`.

The slot shows as pressed, and the highlight stays on "Not now" while it fades (it does not set off for the other
answer); the counter fades with the question; the question's words fade where they stand; `held=` keeps 324 dp for
60 ms, and only then `h=` goes to 268 dp: no word is cut by the lower edge. The two rows rise 60 and 82 ms in, and
the footer fades in on the lower edge. Then `./bl debug key down`, `./bl debug key enter`: the switch's thumb goes
across with a small overshoot inside its track, the track fills, "Off" rolls to "On", and the panel stays. Tab on
the second row in the choices' first third of a second does nothing: the dump still says `choices`, the second row
is selected, and `screen=C`. Pressed again later (`./bl debug key tab`), it enters the list of everything, and first
run is over (`ended`, `run=NONE`).

Answer (The Lenovo Googlebook, 5 October 2026): yes. Tab arms "Not now"; Enter: `held=324` for about 66 ms, then 268 dp, the two rows, nothing
selected. Down, Enter flips the switch and the panel stays. Real keys Enter, Down, Down, Tab back to back: the
choices stand, the second row selected, `screen=C`; Tab half a second later: the list of everything, `ended`,
`run=NONE`. The film (the pressed slot, the thumb's overshoot): not made.

## 27. The fold, and its lap of light [C]

On the choices, nothing selected: `./bl debug first trace 300`, `./bl debug key enter`; `./bl debug dump` two
seconds later, then `./bl trace`. Filmed. Then twice more in a panel opened with `slow=4` (the lap is then asked
for a second and a half after the Enter): `./bl debug first trace 600`, `./bl debug key enter`, and at once `./bl
debug keys x`; and the same with `./bl debug close` in place of the letter; `./bl trace` after each.

The rows and the footer fade; `h=` goes from 268 dp to 68 without stopping; the placeholder is "Type ? for
everything Booklight does"; the `esc` cap fades back a moment after. About 360 ms after the Enter `laps=` goes up by
one, and one white light runs once round the bare field and is gone before it stops: once, also where the whole run
was gone through in a panel that opened with no run in it and had "First steps" typed into it (the lap an ordinary
panel has does not come a second after the fold's own and set it off again). With the letter typed: no lap
(`laps=` does not change), and the list for the letter stands. With the panel closed: `laps=` is the same in the
trace's last line as before the Enter: no lap is asked for over a glass that goes.

Answer (The Lenovo Googlebook, 5 October 2026): yes for the first half. `h=` goes from 268 dp to 68 without stopping; the placeholder is "Type
? for everything Booklight does"; `laps=` goes up by one about 350 ms after the Enter. The two variants at
`slow=4` (a letter typed; the panel closed) and the lap's look: not run.

## 28. An answer shows as pressed [C]

`./bl open stay slow=4`; on the key's first screen `./bl debug key enter` (the system's dialog comes; it is the
system's window, so no key is sent into it: `./bl debug first press under` lands the key and asks the system to
put its dialog away, as the real landing does, so the dialog goes and `./bl debug first` says `hold=NONE` a moment
later; should it stand on, it is closed by its own close button, and `./bl debug close` would leave it up alone);
on "Your key works", once the dump says `seen`, Enter; on a lesson Tab, Enter; on the question Tab, Enter. Filmed.

The slot that was run is a little darker in the very frame of the press, in full, with no colour of its own, and
lets go over about half a second at this speed (120 ms at the speed of every day). No answer waits for it: the
screen changes in the same frame. On answers that give way (Go on, Not now, Agree) it is darker while they fade,
about a third of a second here; on a Skip that stays it lets go where it stands; the answers that come in its
place never show it. An ordinary tip's two answers show no press at all.

Answer (The Lenovo Googlebook, 5 October 2026): in part, from one film at `slow=4`: on "Your key works" the slot of "Go on" is a little
darker from the first frame after the press, for about four frames, and fades where it stands over about 0.45 s
of the film; on lesson 2 Skip lets go where it stands. Not filmed: "Open Keyboard shortcuts", the question's
answers, a click, an ordinary tip. Whether it reads as a press at the speed of every day: for Alex.

## 29. The key lands in view [C]

`./bl debug pref key no`, `./bl debug first new`, `./bl debug first shown`, `./bl open stay`, `./bl debug first at
k2`; half a second later `./bl debug first trace 200`, `./bl debug first press`; `./bl debug dump` at once and
again a second later, then `./bl trace`. Filmed with `slow=4`. Then `./bl debug first at k3` and the same. (The
hook is the model's landing, and the request to the system to put its dialog away that the activity's own landing
makes with it: here no dialog is up, and nothing comes of that. It is no start by the key: that is the `held` half
below, and checks 30 and 31.)
Then the key held down, by the hook: `./bl debug first at k2`, half a second later `./bl debug first press held 12
100` (the key lands, and twelve more starts of the panel by the key follow a tenth of a second apart, as a held
key's repeats come), `./bl debug dump` two seconds later; and then `./bl open key`, one more start by the key.

At once: `first=K4`, and `landing` where the dump comes within 0.7 s of the hook (`keydown` too with `slow=4`, where
the key is down for two thirds of a second); a second later neither. In the trace: `keydown=true` for 160 ms, then
`checked=true` 60 ms after it went false, `laps=` up by one 240 ms after it went false. In the film: "your key" goes
down (a little smaller, a little darker), the title rolls to "Your key works" and its line after it, the caption
goes, the two answers fade and "Go on" comes lit; the key comes up with a small spring, the check draws in it, and
one lap of light runs round the 252 dp outline. The key never grows past its size by more than a hair. With the key
held: two seconds later the panel still stands on "Your key works" (`first=K4`): none of the twelve starts put it
away. The start by the key that comes later than 0.7 s after the last of them puts it away, as on any day (the dump
after it says "no panel").

Answer (The Lenovo Googlebook, 5 October 2026): yes, by the hook, on "Now press your keys" behind the dialog's place and in view. At once
`first=K4` and `landing`; a second later neither. In the trace `keydown=true` for 158 ms, `checked=true` 59 ms
after it went false, `laps=` up by one 242 ms after it went false; the screen takes a key from `clock` 425. With
the key held (`first press held 12 100`): two seconds later the panel still stands on "Your key works"; one
more start by the key after that puts it away. The film: not made.

## 30. The key lands under the dialog [C] and [A]

[C], by the hook alone. As check 29 with `./bl debug first press under` (no dialog up: "your key" is only drawn as
under one). Then with the dialog really up: on the key's first screen `./bl debug key enter` (the dialog comes),
`./bl debug first` and `./bl debug dump` half a second later, `./bl debug first trace 400`, `./bl debug first press
under`, a second and a half later `./bl trace`, `./bl debug dump`, `./bl debug first`. And once more up to the
dump under the dialog, then the dialog closed by its own close button in place of the hook (with `./bl debug first
trace 600` started before it), and `./bl trace`, `./bl debug dump`.
[A], for real: the device's own shortcut for Booklight pressed with the dialog up, by hand, filmed; and the keys
held down for a second or two. (A key combination sent by adb while the system's dialog has the focus goes into a
window that is not Booklight's: that is not the coordinator's to send.)

By the hook, no dialog up: `keydown=true` from the first line, for 400 ms; no way down is seen. With the dialog up:
half a second after it came `helper=1`, the stage under it says "Now press your keys" (`hold=UNDER`), and the dump
says `first=K2 … covered`, with neither `enter` nor `seen`. The hook: the dialog goes (`hold=NONE`), "Your key
works" stands, `key=true`, `screen=K4`. In the trace: `hold=under` and `enter=false seen=false` in every line until
the line in which `hold=` turns to `none`, and for 350 ms after that line (longer only where "Go on" began to show
later than the dialog went: with `slow=`); then both true. A screen that came under the dialog takes a key a third
of a second after the dialog has gone, never sooner, however long it stood under it. Closed by its close button
instead: "Now press your keys" stands (`first=K2`, no longer `covered`), and in the trace `seen=false` for 350 ms
from the line in which `hold=` turns to `none`, then true; Tab then arms "Open Keyboard shortcuts". (Should the
dialog stand on after the hook, the system did not take the request: say so in the answer, and close it by its own
button.) For real [A]: the dialog falls away, and the first frame that shows the panel shows "Your key works" with
the key held down; it comes up, the check draws, the light runs; an Enter pressed with the keys, or in the third of
a second after the dialog went, does not go on. `key=true`, `screen=K4`, `hold=NONE`. The keys held down for a
second or two: the panel stays for as long as they are held and for 0.7 s after (real time: no start by the key
does anything until then, whatever the motion is set to); pressed again later than that, they put the panel away,
as every day.

Answer (The Lenovo Googlebook, 5 October 2026), [C], with the dialog really up: Enter on the key's first screen: the dialog comes,
`helper=1 hold=UNDER`, the dump `first=K2 … covered` with neither `enter` nor `seen`. `first press under`: "the
system's dialog is asked to go", and it is gone within a second; "Your key works" stands, `key=true screen=K4
hold=NONE`. In the trace `hold=under` and `enter=false seen=false` until `hold=` turns to `none`, and for 390 ms
after that line (the key was still held down for its 400 ms; "Go on" counts from the later of the two); then both
true; `checked=true` 58 ms after the key came up, `laps=` up by one 242 ms after. The hook with no dialog up, and
the dialog closed by its own button in place of the hook: not run. [A]: not run.

## 31. The keys open a new panel on "Your key works" [C]

`./bl debug pref key no`, `./bl debug first new`, `./bl debug first shown`, `./bl debug close`; then `./bl open stay
key`, a panel as the key makes it, and at once `./bl debug first trace 300`; `./bl debug dump` two seconds later,
`./bl trace`, and a film. Once with `./bl open key` (no `stay`) and at once `./bl open key` again, as a held key's
repeat comes; and the device's own shortcut in place of the hook's `key`, held for a second [A].

The panel opens as every day; "Your key works" is set down at the gate, "your key" comes up, its check draws a
moment later, "Go on" rises lit, and one lap of light runs. `laps=1`. The second start by the key within 0.7 s of
the first does nothing: the panel stays (`landing` in the dump); one that comes later puts it away.

Answer (The Lenovo Googlebook, 5 October 2026): yes. A panel as the key makes it: "Your key works" is set down (`comes=set_down lead=228`),
`laps=1`, `key=true`. Two starts by the key at once: the panel stays (`landing`); a third a second and a half
later puts it away. The film (the check drawing a moment after the key comes up): not made.

## 32. The opening piece lands part by part [C]

`./bl debug pref opening medium` (as for check 16, and put back after; a new installation's very first opening runs
at Slow whatever is set, and the landing's own marks are the same at every speed), a real first opening; `./bl debug
first show at cell`, `./bl debug first stage at MS`, `./bl debug first land`, `./bl shot land-MS` for MS = 100, 200,
300, 420, 520, 700 (a fresh `show at cell` before each). Then `./bl debug first show at cell`, `./bl debug first
trace 300`, `./bl debug first land`, and `./bl trace` three seconds later. A real first opening filmed with
`slow=4`. And the keys at the landing, in a panel opened with `slow=8` (the landing's marks are stretched eight
times, the 350 ms are not: Enter counts 1.8 s after the landing, the other keys 4.2 s after it): `./bl debug first
show at cell`, `./bl debug first land`; three seconds after the landing `./bl debug key tab`, `./bl debug dump`; six
seconds after it the same two again. And a letter typed while the highlight is still on its way, in a panel opened
with `slow=16` (the landing is then over 3.2 s after it began, and the highlight arrives about 4.5 s after): `./bl
debug first show at cell`, `./bl debug first trace 1200`, `./bl debug first land`, three and a half seconds later
`./bl debug keys x`, a second later `./bl debug type` with nothing after it, `./bl debug dump`, `./bl shot
land-back`, `./bl trace`.

The hook's answer: `playing=landing first=K1 gliding`. At 100: the disc beginning; no words yet. At 200: the disc,
the title and its line coming, and "Open Keyboard shortcuts" beginning to fade in where the highlight is arriving.
At 300: the seat whole, the armed answer's words there, `Action` coming up. At 420: `Action` and the "+", the
second cap beginning. At 520: both caps, the caption and "Not now" coming. At 700: the key's first screen at rest.
The highlight ends exactly on the armed answer: its place is the same in every picture. In the trace: `lead=180
all=480`; `enter=false` and `playing=landing` until `clock` has passed 530; `seen=false` until it has passed 830:
"Not now" begins to show at 480, and nothing reaches it sooner than 350 ms after that. With `slow=8` the dump three
seconds after the landing says `enter` and not `seen`, and the Tab before it has moved nothing (`armed=0`); six
seconds after the landing it says `seen`, and Tab arms "Not now" (`armed=1`). The letter typed while the
highlight travels: the trace has lines with `playing=none` and `glide=true`, and from the line in which `stage=`
turns to `none` (the letter) it says `glide=false` and goes on saying so, also once the field is empty again (if
the letter fell while `playing=landing`, or after `glide=false`, the moment was missed: once more, a little later
or sooner). The dump has no `gliding`; the typed
list had its pill on row one; and the emptied field shows the key's step with "Open Keyboard shortcuts" lit and no
highlight setting off again.

Answer (The Lenovo Googlebook, 5 October 2026): yes. The hook's answer: `playing=landing first=K1 gliding height=376dp`. The six pictures as
said, the highlight in the same place in each. In the trace `lead=180 all=480`; `playing=landing` and
`enter=false` until `clock` 526; `seen=false` until 826. At `slow=8`: three seconds after the landing the dump
says `enter` and a Tab has moved nothing (`armed=0`); six seconds after it says `seen` and Tab arms "Not now"
(`armed=1`). The film at `slow=4`, and the letter typed while the highlight travels: not run.

## 33. With the system's animations off, everything stands [A]

With the system's animations off (a setting of the device: Alex's, or set for this check and put back, as in part
4): every check above that has a still, once each.

Every screen stands whole in its first frame: the caps, the recipe, the caption, the answers; "Your key works" has
its check and "Go on" lit at once, the key is never drawn down; the choices' switch and its word change in one
frame; no lap runs; no answer shows as pressed, and no height is held for a fade (the glass is at each screen's own
height in its first frame). One hold stays, because what it waits for is no motion: a letter typed on a stage
(`./bl debug first trace 120`, `./bl debug keys x`, `./bl trace`, on each lesson and on the question) takes `h=`
from the stage's height to the list's with `held=` in between and no line at 68 dp; so does Up with a last text
kept (check 22). The waits that are there to be read stay: 520 ms after a lesson's Enter, 300 ms before "Now
press your keys" under the dialog. A screen still takes a key only 350 ms after it came, and the user's key, held,
still does not put "Your key works" away for 0.7 s after its last repeat (check 29's `first press held 12 100`,
with animations off: the panel stands two seconds later). `./bl debug turn 60 enter` says "not turned": with
animations off a panel that closes is gone at once.

Answer (The Lenovo Googlebook, 5 October 2026): in part, with the system's animator scale set to 0 for the check and put back: lesson 2
stands whole at once; a letter typed on lesson 2 and on lesson 3: 252 dp kept (`held=252`), then the list, with no
line at 68; `first press held 12 100`:
the panel stands on "Your key works" two seconds later; `./bl debug turn 60 enter` says "not turned". The stills
of every screen: not made.

## 34. What a screen reader is told, by the hook [C]

On each screen in turn (`./bl debug first at k1 … q`, the choices, the ending, the word left behind): `./bl debug
first says`. Also: on lesson 2 right after the panel is made; after `./bl debug keys x` and `./bl debug type` with
nothing after it; after flipping the switch on the choices.

`told=` holds each screen's sentence once: the counter, the title, the line, the keys or the recipe ("For example:
…", the app's letters spelled), the caption, and how to act. Each part ends once: the question's reads "… Suggest
searches as you type? Booklight sends …", with no full stop after the question mark, and so does "Didn’t work?" on
the key's third screen; in German too („… Beim Tippen Suchen vorschlagen? Booklight sendet …“). For "Your key
works" in a run asked for again: "Enter goes on. Tab for “Change the key”." Typed over and emptied, a screen is not
said a second time (one that was typed over before it had been in view for a third of a second is: it comes into
view only then), and "Welcome to Booklight" is said once in a panel at the most. Where the recipe came a moment
after the lesson was said, it is said by itself. After the flip: "Show your usual, On". The word left behind: "The
first steps wait in Booklight’s window, on its Start page." Among `nodes=`: the switch's row as `switch 'Show your
usual' off` (or `on`), the coach line as `live`, no node for a large cap or a recipe's letters (they are a
picture), and the field.

Answer (The Lenovo Googlebook, 5 October 2026): yes. `told=` holds each screen's sentence once (the key's first screen, "Your key works",
lesson 2 with "For example: …", the question); typed over and emptied, lesson 2 is not said again; in a run
asked for again: "Enter goes on. Tab for “Change the key”."; after the flip "Show your usual, Off"; the word left
behind: "The first steps wait in Booklight’s window, on its Start page." Among `nodes=`: `switch 'Show your
usual' on`. The question's sentence reads "5 of 5. Suggest searches as you type? Booklight sends …".

## 35. A screen reader's own voice [A]

With TalkBack on: a new installation's first opening (no welcome: the key's step at once, the title said before
it); each screen in turn; the choices; the ending. And TalkBack switched on in the middle of the welcome, and in
the middle of the show.

Each screen is said once, in the order of the hook's sentence; the switch is said as a switch with its state, and
its new state after a flip. Switched on in the piece: the piece ends at once and the key's step stands and is said.

Answer: not run

## 36. A recipe has its room [A]

With the system's font scale above the usual, in German, on lesson 3 (the widest recipe): is the recipe one line,
whole, a little smaller, ending before the `tab` cap, on the caps' own centre line? And on a device whose example's
name begins with an emoji (none is known): no half character in the recipe. By reading: core `FirstRun.head`, with
its test.

Answer: not run

## 37. Heights, and nothing outside the glass [C]

From the traces and dumps above, in dp: the key's step and the lessons 252, the question 324 (or more for a longer
text), the choices 268, the bare field 68, the welcome 468. In every picture: is anything cut at the lower edge, or
is there glass with nothing on it under the last line? The window's width and its top edge never change in any
trace (`./bl debug dump`'s `window=` before and after).

Answer (The Lenovo Googlebook, 5 October 2026): from the traces and dumps above: the key's step and the lessons 252 dp, the question 324, the
choices 268, the bare field 68, the welcome 468; the show 400, 256 and 376. In the pictures nothing is cut at
the lower edge and no glass stands empty under a last line, but for the 0 ms picture of a set-down, which is the
glass at its height before its first part has come. The window's width (720 dp) and its top edge are the same
in every dump.

# What the three earlier lists left open

Each with the list and the check it comes from. Where this part built something that answers it, that is said.

## 38. "Set shortcut" and the first press of a new key, with the panel held [A]

*`first-run-key.md` 21, and the proof's check 6.* On a Googlebook where Booklight has no shortcut yet: from the
key's first screen Enter; in the dialog Customize, the app's name, +, the keys, Set shortcut, the keys again. Does
the dialog go by itself, and does the panel show "Your key works" with the key held down, then up, the check and
the lap (check 30)?

Answer: not run

## 39. A real click on the icon, and the other starts [A]

*`first-run-key.md` 10 and 11.* A new installation: the first click on the launcher's icon shows the panel with the
opening piece; every later one the Booklight window. App info's "Open", the widget and the tile: the panel, not
counted as the key.

Answer: not run

## 40. A keyboard without the Quick Insert key [A]

*`first-run-key.md` 12.* With an external keyboard attached and used: the caps are `Action` and `M`, the caption is
"or any keys you like", the dialog's rows carry Action + M; on the window's Start page the same two caps (check 10).

Answer: not run

## 41. Is Quick Insert the key left of A? [A]

*`first-run-key.md` 13; "Settled for the build", point 8.* The caption under the caps, and now the Start page, say so.

Answer: not run

## 42. The system in German [A]

*`first-run-key.md` 4 and 14.* With the system's language German: the dialog's own two words and the key's name as
the caps, the caption, the dialog's rows and the Start page quote them; the large cap's fallback where the name
has no room.

Answer: not run

## 43. A larger font scale [A]

*`first-run-key.md`, its last list.* Every screen of first run, the Start page's key rows and the row "First steps"
at the largest font scale: nothing cut, the recipe drawn smaller (check 36), the question taller.

Answer: not run

## 44. Other windows, shortcuts and the lock screen under the hold [A]

*`first-run-key.md` 9.* With the dialog over the held panel: a click on another window, another app's keyboard
shortcut, the lock screen. Is the panel gone afterwards, and never left on the desk without the keys?

Answer: not run

## 45. A click on the stage, and a click outside the glass [C] and [A]

*`first-run-key.md` 3; `first-run-lessons.md` 14; `first-run-welcome.md` 22.* [C], taps inside the panel's own
window, sent with `./bl sh input tap X Y` in px of the screen. The coordinates come from `./bl debug dump`:
`window=WxH@X,Y` is the panel's width, its height and where its left top corner stands on the screen, all in px.
`./bl shot NAME` is a picture of exactly that window, px for px: a point at x, y in the picture is at X + x, Y + y
on the screen (an answer's slot, the glass beside the answers, the welcome's handle, a row). A tap goes only to a
point inside that rectangle, and only while `./bl idle` says the panel's window has the focus; a tap outside the
glass is Alex's. A click on an answer runs it (and shows it pressed); a click on the glass beside the answers does
nothing; a click on an answer in the third of a second after its screen came does nothing; in the welcome a click
on the handle begins the show and a click elsewhere on the glass sets the key's step down; in the show a click on a
row sets the key's step down and opens nothing. [A]: a click outside the glass closes the panel, as always.

Answer (The Lenovo Googlebook, 5 October 2026), the taps inside the panel's own window: on the key's first screen a tap on the glass beside
the answers does nothing. On "Your key works" at `slow=16` a tap on "Go on" while it was still being set down
did nothing; once it stood, a tap on it went on to lesson 2. In the welcome a tap on the glass beside the handle
set the key's step down, a tap on the handle began the show, and a tap on a row of the show set the key's step
down and opened nothing. The pressed look of a clicked answer: not filmed. [A]: not run.

## 46. A pointer that rests where the answers come [A]

*`first-run-lessons.md` 28.* The pointer left where "Go on" or "Agree" will stand, the screen changed by the keys:
is an answer lit without the pointer having moved?

Answer: not run

## 47. Settings standing in for an app that can be searched [A]

*`first-run-lessons.md` 6 and 27.* On a device where no app can be searched (none is known): lesson 2 names the
Settings app, lesson 3 goes by the Settings keyword to a page, and Enter there is practice.

Answer: not run

## 48. A settings page, and everything else, in a lesson [A]

*`first-run-lessons.md` 11.* In a lesson, a row that is not the lesson's own (a settings page, the web's row,
another app's command) runs as every day and opens what it opens: is that what a new user expects under "Practice:
nothing opens"? (It opens another app: not run by the coordinator.)

Answer: not run

## 49. A question of more than four lines [C]

*`first-run-lessons.md` 15.* `./bl debug pref engine brave` (the longest name), in German, on the question: is the
stage taller by what the text needs, the text whole, the answers under it? Put the engine back.

Answer (The Lenovo Googlebook, 5 October 2026): in part. In German with the longest engine's name the question's text is three lines at the
usual font size: whole, 324 dp, the answers under it („Nicht jetzt“ first), nothing lit. More than four lines is
not reached at that size: the taller stage needs a larger font scale (Alex's).

## 50. The dark theme on the lessons and the choices [C]

*`first-run-lessons.md` 1 and 16.* `DARK=true ./bl open stay` on lessons 2 to 4 and on the choices, over the
backdrop's white page and its dark one: the caps, the recipe, the switch and the footer's line readable.

Answer: not run

## 51. The whole run by hand, timed and watched [A]

*`first-run-lessons.md` 20; `pm.md` §4.* A new installation, real keys, no hooks: from the first opening to the
bare field. How long does it take (45 to 60 seconds were asked for)? Where does a hand stall?

Answer: not run

## 52. The night and the shaft over a white page [C]

*`first-run-welcome.md` 3, 8 and 29.* A real first opening over the backdrop's white page, in light theme: is the
night a night, the shaft seen as light, the footer's "Skip" readable from the show's first frame?

Answer: not run

## 53. The piece's times, measured [C]

*`first-run-welcome.md` 10, 16 and 18.* A real first opening (`./bl debug first new`, `./bl debug pref key no`,
`./bl open stay`), and as soon as the panel is there `./bl debug first trace 1600`; `./bl trace` fifteen seconds
later. (A trace cannot be started before its panel: the hook answers "no panel". It need not be: the welcome stands
for three and a half seconds before the show's first letter, a trace begun anywhere in it has the whole show, and
every time below is counted from the line in which `playing=show` begins, not from the trace's first frame. 1600
frames are thirteen seconds at 120 frames a second; the app keeps them all, whatever the log holds.) **Leave it
alone**: the welcome hands over by itself about 3.8 s after the gate, and a key sent just then is no cue any more:
in the show Enter sets the key's step down, and the show is over before it began. No key, no hook that presses one,
until `playing=none`. And again with `slow=4`, filmed. From the trace: when `playing=show` begins (the hand-over's
end, and the first letter), when the sum's row, the flight's and the grid come, when `laps=` goes up, when
`playing=landing` begins: against the script's 1,960 · 3,352 · 4,840 · 5,080 · 5,856 ms from the first letter,
within two frames each. `rows=` tells the first of them alone (five apps become one row): the sum's row, the
flight's row and the grid are one row each, so the flight and the grid are read from `h=`, the window's height,
which turns towards 256 dp for the flight's row and towards 376 dp for the grid in the frame after each comes (it
follows on a spring: the first line in which it moves is the one to read). In the film at a quarter of the speed:
the highlight's way from the grid's square into the armed answer, and from a row.

Answer (The Lenovo Googlebook, 5 October 2026): yes, where the trace can say. Left alone (the welcome hands over by itself), from the line
in which `playing=show` begins: the sum's row at 1,967 ms (the script: 1,960), `laps=` up at 5,088 (5,080),
`playing=landing` at 5,863 (5,856). The flight's and the grid's beats were not read (their `rows=` is 1 as the
sum's). The film at `slow=4`: not made.

## 54. The example flight in German, later, with a key, offline [C] and [A]

*`first-run-welcome.md` 14.* [C]: `./bl debug first show at flight` in German, the app's language („Landung in 4
Std. 07 Min.“, „Pünktlich“, „Beispiel“); again a minute later: the headline has not counted down. With a flight key
in and without, if one is at hand: the same row. [A]: with the network off, the same row.

Answer (The Lenovo Googlebook, 5 October 2026), [C]: in German (the app's language): „Landung in 4 Std. 07 Min.“, „Pünktlich“, „Beispiel“,
the footer „Ein Beispiel. Live-Zeiten brauchen deinen eigenen AirLabs-Schlüssel.“, 256 dp. A minute later, and
without a flight key (one is in on this device): not run. [A]: not run.

## 55. Esc held, and the second Esc [A] and [C]

*`first-run-welcome.md` 19 and 20.* [A], a hand on the key (adb sends a key as a tap and cannot hold one): Esc
held for two seconds in the show lands the key's step and does not go on to close the panel. [C]: in the show
`./bl debug key esc` (the piece takes it: the key's step lands), and once the dump says `playing=none`, half a
second later, `./bl debug key esc` again: that one closes the panel.

Answer (The Lenovo Googlebook, 5 October 2026), [C]: yes. Esc in the show lands the key's step; Esc again half a second later closes the
panel. [A]: not run.

## 56. The lock keys, Quick Insert alone, and the system's own keys in the piece [A]

*`first-run-welcome.md`, "Still open".* During the welcome and the show: Caps Lock or the Quick Insert key alone
does nothing; the volume and brightness keys do what they do and the piece plays on.

Answer: not run

## 57. A space that comes by the input method [A]

*`first-run-welcome.md`, "Still open".* With an input method that hands a space over as text and not as a key:
during the welcome it is the cue, during the show it sets the key's step down, and the field stays empty.

Answer: not run

## 58. Enter in the moment a closing panel is turned round [C]

*`first-run-welcome.md`, "Looked at again": not driven by a key from outside.* On the key's first screen, and one
second into the welcome: `./bl debug turn 60 enter` (the panel is closed, and 60 ms later, while it folds, it is
turned round and Enter is pressed in the same call).

The hook's answer: "turned; Enter in the same moment answered nothing". If it says "not turned", the panel had
already gone and nothing was tested (the system's animations are off, or 60 ms is longer than the fold at this
speed: try a shorter time). No dialog comes (`hold=NONE helper=0`), and half a second later Enter is the screen's
own. What this shows and what it does not: the hook's turn is a plain one, not the key's own start (nothing lands
by it), and its Enter is given in the call that turns the panel, with that moment's time, so it shows that a
screen which came by a turn is not answered in that moment. That a key counts by its own time, when it was pressed
and not when it was handled, is the real keys' to show (checks 3, 17 and 25). In the welcome the close ends the
piece: what the turned panel shows is the key's step, whole, and the Enter is that screen's.

Answer (The Lenovo Googlebook, 5 October 2026): yes. On the key's first screen, and one second into the welcome: "turned; Enter in the same
moment answered nothing"; `hold=NONE helper=0`, no dialog; the turned panel shows the key's first screen, whole.

## 59. Not verified by reading or on a device [A]

*`docs/design/first-run/BUILD.md`, "Known, and left for a later part".* A panel whose first start carried a
referrer of its own and is later reached by the key; a process that dies under the dialog after the icon's first
click; whether a screen reader says "Now press your keys" while the dialog is over it.

Answer: not run

## 60. The HP Googlebook [A]

*All three lists.* Checks 1, 5, 10, 11, 13, 16, 19, 24, 27, 30 and 32 again there, and the frame times of a real
first opening (`first-run-welcome.md` 11).

Answer: not run

## 61. What only a person can say [A]

*`first-run-welcome.md`; `design.md` §15.* Does the lamp read as struck? Do the shaft into the caret and the handle
to row one read as one move up? Is the show one movement or five slides, and are eleven seconds a pleasure or a
wait? Is "your key", blank, read as a key waiting? Do the set-downs feel like a hand setting keys down, or like a
delay before Enter works? Is the key left of A?

Answer: not run

## What the pass found (5 October 2026)

- **Forty of the sixty-one checks have an answer**, from the test device; the other twenty-one are Alex's own or
  need a film nobody made. Every answer given is a "yes", or a "yes" for the part that was run.
- **Mended after the pass, and looked at again**: "First steps" stood in the list for one or two letters (now
  three letters of its name, or one of its other words in full); a sentence said to a screen reader ended "?.";
  the hook that lands the key under the dialog left the dialog up.
- **Seen, for Alex**: the welcome's handle rests in row one's seat, empty, for about a fifth of a second before
  the show's first letter (13); the press on an answer that gives way is a tenth of a second of a slightly darker
  slot (19, 28); the row "First steps" in the panel says "Open" for what it does (2); every screen is deaf while
  it is set down and for 350 ms after, without a sign (16, 17, 32).
- **Two cautions for whoever runs these checks again**, both learnt the hard way: real keys sent back to back
  must never include a letter and Enter (a Backspace between them can arrive first, and on the key's step Enter
  then opens the letter's first row as on any day); and a tap is sent only after reading the panel's window from
  the dump, because a panel that has folded to the bare field is 68 dp high and a tap aimed at where an answer
  stood falls on the window behind it.
