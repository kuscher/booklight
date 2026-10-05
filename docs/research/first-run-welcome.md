# First run: the welcome and the show on a device (part 4)

*The checks for part 4 of the first-run plan (`docs/superpowers/plans/2026-10-05-first-run-4-the-welcome.md`).
Whoever runs a check writes its answer under it, with the date and "the Lenovo Googlebook" or "the HP Googlebook".
Until then an answer reads "not run".*

**Before the first check**

- Choose the device by its model (`adb devices -l`), never by its place in the list; set `ANDROID_SERIAL`.
- Close the Booklight window, then `./bl install`. `./bl idle` before every `./bl open`: the panel takes the keyboard.
- Never `uiautomator dump`. No sign-in is made for a check, and no other app is opened by one. A capture or a film
  of the screen shows real windows: none is kept or committed. `./bl shot NAME` is the panel's own window, and
  `./bl backdrop` puts a window of test content (a white page, a dark terminal) behind the panel.
- This file is public: no serial, no build number, nothing about what is installed. The show's first beat lists
  apps of the device, and `./bl debug dump` names them after `rows=`: an answer says "the device's apps" and how
  many, never a name, and never the letter Booklight types for them.
- Write down what `./bl debug pref` says before the first check (`opening=`, `dim=`, `theme=`, `suggestions=`) and
  put them back after the last one. Check 1 comes first: the glass's shader changed, and it is compiled on the
  device.
- **A real first opening**: `./bl debug close`, `./bl debug first new`, `./bl debug pref key no`, then
  `./bl open stay` (a plain start; `stay` keeps the panel up when it loses the focus). `./bl debug first` before it
  says `overture=WELCOME slow=true`. The piece is marked as shown once it has played for two seconds or has ended:
  `./bl debug first new` before the next one.
- **A still of the welcome**: with a panel open and its field empty, `./bl debug first welcome at MS`. Use a panel
  that began with the piece and was landed with Esc (a real first opening, then `./bl debug key esc`): in a panel
  that opened on an ordinary day a tip or the usual rows may stand under the field, and they show through the night. MS is the
  paper's own clock (`docs/design/first-run/motion.md` §2.1: ms from the first frame of an opening at Medium). The
  welcome stands at that moment until a key ends it or another hook begins it again. The prototype's frame of the
  same moment is in `docs/design/first-run/first-run.html`, with its light on "The shaft" and its words on A.
  `./bl debug first welcome` plays it from the gate. In a panel that did not begin with the piece the desk behind
  does not dim: a still shows the night over a brighter desk than a user's first opening has. What depends on the
  dim (how dark the night is, whether the shaft reads as light over a white page, the footer's words in the night:
  checks 3, 8 and 29) is asked again in a real first opening or its film (check 10).
- **A still of the show**: `./bl debug first show at apps|row|stop|sum|answer|flight|grid|cell`. `./bl debug first
  show` plays it from its first letter; `./bl debug first land` sets the key's step down and says, in the same
  turn, what stands in the landing's first moment. A show that plays or stands, like the welcome that plays, counts
  as shown after two seconds: `./bl debug first new` before a check that needs the piece due again.
- `./bl debug dump` says `playing=welcome|show|landing|none`, `cast=ready|none` (whether this device's rows for the
  show are worked out), `laps=` (how often the show asked for the light), `gliding`, `first=`, `due=`, the
  placeholder as `hint=`, and the window's size in px (divide by the screen's density for dp).
- While the piece plays, `./bl debug key enter|esc|tab|down` are taken by it as the keys are (the answer says so);
  `./bl debug keys TEXT` types a letter at a time, and the first letter ends the piece.
- `./bl open stay slow=4` stretches the piece's clock and every spring four times; `DARK=true ./bl open stay` is
  the dark theme. A film of the screen is cut to the panel's rectangle, looked at frame by frame, and deleted.
- Lengths below are dp from the panel's left and top edges.

## 1. The panel still opens

`./bl debug first off`, `./bl debug pref key seen`, `./bl open stay`, `./bl debug dump`, `./bl shot plain`; again
with `DARK=true`. `./bl logs`: no crash, no line about a shader.

The glass as it was: the veil, the white outline at its everyday strength (not full white), the grain. `playing=none`.

Answer (The Lenovo Googlebook, 5 October 2026): yes, with the build of the commit that changed the shader: the
panel opens, the glass and its rows are as ever, and the log has no fatal line. Looked at again with the part's
last build, in both themes: the same, `playing=none`.

## 2. Who gets it

`./bl debug first new`, `./bl debug pref key no`, `./bl debug first`: `overture=WELCOME slow=true greets=false`.
`./bl debug first shown`: `overture=NONE slow=false`. `./bl debug first update`: `NONE`. `./bl debug pref key
seen`, `./bl debug first replay show`: `overture=WELCOME slow=false`; `./bl debug first replay`: `NONE`.

Answer (The Lenovo Googlebook, 5 October 2026): yes, each as said.

## 3. Night (the paper's 700)

`./bl open stay`, `./bl debug first welcome at 700`, `./bl debug dump`, `./bl shot night`. Light and dark theme;
over the backdrop's white page and over its dark one.

The window 468 dp high and as wide as before. The veil near black in both themes, a tenth of the desk still
showing through; the outline full white, the brightest line there is; a little darker towards the corners. No mark
in the field, no placeholder, no caret (it has blinked off); the `esc` cap white, its right edge at 700. Nothing
else. Is the night too dark, or not a night, in light theme over the white page?

Answer (The Lenovo Googlebook, 5 October 2026): 468 dp, as wide as before. Near black, the outline white; no mark, no placeholder, no caret; the
`esc` cap white at the right edge; nothing else. The dark theme gives the same picture. Looked at in the panel's own
shot and in a film of a real first opening, where the desk shows dimly through. Over a white page in light theme,
and whether it is a little darker towards the corners: not judged (the test backdrop was not behind the panel).

## 4. The strike (850)

`./bl debug first welcome at 850`, `./bl shot strike`.

The field's band is lit from the caret outwards: to its left end, and part of the way to the right, the front
soft; not yet at its full brightness (the lamp has not caught); a halo round the caret; the caret a dark slit on
x = 72 to 74. Nothing below the field yet.

Answer (The Lenovo Googlebook, 5 October 2026): a halo round the caret, the band lit only round it so far, the caret a dark slit. Below the
field the title and the line already show dimly, where the check expects nothing yet.

## 5. The head lit, the seam (1090)

`./bl debug first welcome at 1090`, `./bl shot seam`.

The whole band white, corners as the glass's own; the `esc` cap in the night's ink now, readable on the white. A
seam of light 6 dp wide on x = 360 from y = 92 down to about 440. The title and the line show dimly.

Answer (The Lenovo Googlebook, 5 October 2026): yes. The band is white with the glass's own corners, the cap is in the night's ink and can be
read, the seam stands on the middle from under the head to near the foot, and the title and the line show dimly.

## 6. The light opens (1330)

`./bl debug first welcome at 1330`, `./bl shot opening`.

The light is partly open, the same to both sides of x = 360, its edges soft. The title is white in its middle and
dim at its ends: the light's edge crosses the letters as a gradient, not as a cut. No handle yet.

Answer (The Lenovo Googlebook, 5 October 2026): yes. The light is partly open, the same to both sides, its edges soft; the title is white in
its middle and dim at its ends; no handle yet.

## 7. The knife (1750)

`./bl debug first welcome at 1750`, `./bl shot knife`.

The light at its shape. The handle stands at x 252 to 468, y 372 to 420, in the selection's colour, "Show off" and
the Enter mark centred on it. The tools come left to right: the first two stand at their angles (40° and 20° left
of upright) or a little past them, the third is on its way out, the fourth and the fifth are not there yet. A tool
that is folded lies behind the handle and does not show.

Answer (The Lenovo Googlebook, 5 October 2026): yes. The light at its shape; the handle in the selection's colour with "Show off" and the Enter
mark centred on it; the first two tools at their angles, a glint on the first; the third on its way, still right of
upright; the fourth and the fifth not there.

## 8. It stands (2900)

`./bl debug first welcome at 2900`, `./bl shot stands`; light and dark; white page and dark; German.

Measure in the shot, at four times the size. The light: upper edge on y = 92 from x 129 to 591, foot on y = 448
from 32 to 688, 24 dp of night between the head and the light, brightest under the lamp. The title centred on 360
in the box y 124 to 172; the line centred on 360 in y 188 to 210, on one line. Five discs of 36 dp, centred on
(247, 323), (302, 312), (360, 308), (418, 312), (473, 323), with `app`, `calc`, `plane`, `smile`, `key` in them,
on stems from the handle's upper edge. The handle and its cue as in check 7. Everything the same to both sides of
x = 360. The words are A: "Welcome to Booklight" · "Your Swiss Army knife launcher. Blades not included." · "Show
off"; German: „Willkommen bei Booklight“ · „Dein Schweizer Taschenmesser zum Tippen. Ohne Klinge.“ · „Kurz
angeben“. In German: is the title inside the light, and is the line whole? No dust in the beam; no light running
round the outline. Is the shaft seen as light over the white page? Are the words read in the time they stand?

Answer (The Lenovo Googlebook, 5 October 2026): by eye in the shots, not with a ruler: the light's upper edge and its foot, the title and the
line centred in their boxes, the five discs with their marks on their stems, the handle and its cue, all the same
to both sides of the middle. The words are A in English and in German; in German the title is inside the light and
the line is whole. The dark theme gives the same picture. No dust, and no light round the outline. In the film of
a real first opening the shaft reads as light over the dimmed desk. Over a white page, and whether the words are
read in the time they stand: not judged.

## 9. The hand-over (3470 and 3660)

`./bl debug first welcome at 3470`, `./bl shot hand-1`; `./bl debug first welcome at 3660`, `./bl shot hand-2`.

At 3470: the tools are folded away; the shaft is narrower and its upper edge has slid towards the caret, the foot
a little behind it; the cue has all but faded; the handle has just set off for row one's seat; the night has begun
to lift. At 3660: all that is left of the shaft is a thin seam under the caret, rising; the handle rests, or all
but rests, on x 8 to 712, y 76 to 132, in the theme's own selection colour; the head's light has begun to close on
the caret from both ends; the theme's own glass is nearly back.

Answer (The Lenovo Googlebook, 5 October 2026): yes, both as said. Seen at 3660: a faint large ring round the middle of the glass, where the
night's deeper shade begins, for the moment in which the night lifts.

## 10. The welcome, in motion

A real first opening, filmed; again with `slow=4`.

The gate, the strike, the seam of light, the light open, the first tool, the hand-over, the first letter: at Slow
(which the very first opening is) each is 258 ms later than the paper's 257 · 800 · 960 · 1,540 · 1,560 · 3,200 ·
3,820; within two frames each? The lower edge falls from 68 to 468 in one move, and the night follows it. In no
frame is anything drawn outside the glass, or cut by its lower edge while it falls. The top 68 dp are the same
pixels throughout but for the light, the caret and the cap. The window's width and its top edge never move. The
desk behind dims to about half and comes back with the hand-over. Does the lamp read as struck, not faded in? Do
the shaft into the caret and the handle to row one read as one move up?

Answer (The Lenovo Googlebook, 5 October 2026): filmed once, a real first opening at ordinary speed. The lower edge falls to 468 in one move
and the night follows it; the lamp strikes from the caret outwards; the seam, the light opening, the knife; at the
hand-over the shaft narrows and slides up to the caret while the handle rises into row one's seat and grows to its
width. In the frames looked at nothing is drawn outside the glass, the field's band is the same but for the light,
the caret and the cap, and the window's width and top stay. The desk dims and comes back. Not measured: the seven
moments against the paper's times (the film's times were not kept). Not run: the film at `slow=4`. Whether the
lamp reads as struck and the hand-over as one move up: for Alex.

## 11. Frame times

`./bl sh dumpsys gfxinfo io.github.kuscher.booklight reset`, a real first opening left to play to the key's step,
then `./bl sh dumpsys gfxinfo io.github.kuscher.booklight`.

How many frames, how many of them janky, the 90th and 99th percentile in ms: a frame of this screen is 8.3 ms. If
frames are late in the welcome, the order of what goes is `motion.md`'s: the bloom round the lit letters, the
rays, the penumbra and the core.

Answer (The Lenovo Googlebook, 5 October 2026): a debug build, one real first opening to the key's step: 1,174 frames, 12 of them janky
(1.0 %); the 50th percentile 7 ms, the 90th 9 ms, the 95th 15 ms, the 99th 25 ms.

## 12. The show: apps

`./bl debug first show at apps`, `./bl debug dump`, `./bl shot apps`; then `at row`, `at stop`.

`playing=show cast=ready`. The field holds one letter, the rest of row one's name grey after it, Booklight's mark
in the mark's seat. Up to five rows, all of them apps of the device, each with its icon; the pill on row one with
its strip; the footer's right end `tab` Actions and `esc` Skip. The window is 400 dp with five rows (344 with
four, 288 with three). `at row`: the pill on row three. `at stop`: the pane on that row's third stop, its name
unrolled. How many rows: write the number, not the apps.

Answer (The Lenovo Googlebook, 5 October 2026): yes. `playing=show cast=ready`; one letter in the field with the rest of row one's name grey
after it, Booklight's mark in the mark's seat; five rows, all apps of the device with their icons, the pill on row
one with its strip; `tab` Actions and `esc` Skip; 400 dp. `at row`: the pill on row three. `at stop`: that row's
third stop, its name unrolled. Five rows.

## 13. The show: the sum

`./bl debug first show at sum`, `./bl shot sum`; `./bl debug first show at answer`, `./bl shot answer`.

The field holds `150 + 2`, the row's small line the same, its answer 152, "Copy" with the Enter mark; 212 dp. Then
`150 + 20%` and 180.

Answer (The Lenovo Googlebook, 5 October 2026): yes. `150 + 2`, 152, "Copy" with the Enter mark, 212 dp; then `150 + 20%` and 180.

## 14. The show: the example flight

`./bl debug first show at flight`, `./bl debug dump`, `./bl shot flight`; German; with a flight key in
(`./bl debug pref flightkey` as it was) and without; with the device offline.

256 dp. Line one "LH 455 · Lufthansa · San Francisco → Frankfurt/Main"; the headline "Lands in 4 h 07 min" with
one green badge "On time"; at the row's right end "Example", and no strip; the line from x = 72 to 700 with the
plane's tail near x = 443, solid behind it and dots ahead; under its ends "SFO", the time it left and "Boeing
747-8", and "Terminal 1", the time it lands and "FRA", in the device's 12 or 24 hours and with no day beside
them. The footer's left end: "An example. Live times need your own AirLabs key." German: „Landung in 4 Std. 07
Min.“, „Pünktlich“, „Beispiel“, „Ein Beispiel. Live-Zeiten brauchen deinen eigenen AirLabs-Schlüssel.“ The dump:
`phase=in_air`, `share=0.61`, and no action between the row's `<` and `>`. It is the same row with a key in,
without one and offline, and a minute later the headline has not counted down.

Answer (The Lenovo Googlebook, 5 October 2026): yes, in English: 256 dp, the three lines, the green badge, "Example" at the row's right end
and no strip, the plane at its place with the line solid behind it and dots ahead, the two ends as said, the
footer's sentence. The dump: `phase=in_air`, `share=0.61`, no action. Not run: German; with a flight key in and
without; offline; a minute later.

## 15. The show: the grid

`./bl debug first show at grid`, `./bl shot grid`; `./bl debug first show at cell`, `./bl shot cell`.

The chip "Emoji" where the mark was, the scope's placeholder, five lines of fourteen cells, 376 dp; the square on
the first cell. `at cell`: the square on the fourth cell of the second line (x 165 to 209, y 134 to 178), that
cell's name at the footer's left end, `⏎` Copy, the arrows' cap Move and `esc` Skip at its right.

Answer (The Lenovo Googlebook, 5 October 2026): yes. The chip "Emoji", the scope's placeholder, five lines of fourteen cells, 376 dp, the
square on the first cell; `at cell`: the square on the fourth cell of the second line, that cell's name at the
footer's left end, Copy, Move and Skip at its right. The first run of this check found a fault: a search that was
still on its way when the hook began the piece landed among the show's rows (the show types the same word). The
piece now cancels a search that is on its way when it begins.

## 16. The show, in motion

A real first opening, filmed from the hand-over to the key's step; again with `slow=4`.

Counted from the first letter: the sum's row at 1,960 ms, the flight's at 3,352, the plane at its place about half
a second later, the keyword's space at 4,840, the light setting off at 5,080, the landing at 5,856; within two
frames each? The field is never empty before the chip, and each new text replaces the old one in one frame. The
lower edge goes 400 (or less, check 12), 212, 256, 376, each in one move and never back to 68. **The frame in
which the first list comes**: the handle is gone and the pill stands in row one's seat in the same frame; no frame
with two highlights there, none with none. From then on: exactly one highlight in every frame but while the pill
gives way to the grid's square (the product's own change: how many frames have none, or two?). The light runs once
round the outline from the grid on, and the user's keys were not needed for any of it. One movement, or five
slides? Eleven seconds: a pleasure or a wait?

Answer (The Lenovo Googlebook, 5 October 2026): filmed once, a real first opening at ordinary speed. The letter and the apps' cascade, the
pill down to row three and its stop, the sum growing to 180, the flight's row with its plane flying to its place,
the keyword becoming the chip, the grid and its square's two moves: each text replaces the one before, the field is
never empty before the chip, and the lower edge goes from one height to the next without a visit to 68. Seen: the
pill stands in row one's seat with the first letter for a frame or two before the rows draw in. Not measured: the
five times against the script (the film's times were not kept); frames with no highlight or two where the pill
gives way to the grid's square. Not picked out in the film: the lap of light. One movement or five slides, a
pleasure or a wait: for Alex.

## 17. The landing

`./bl debug first new`, `./bl debug pref key no`, `./bl open stay`, `./bl debug first show at cell`, then
`./bl debug first land` (its answer is the landing's first moment); a moment later `./bl debug dump`,
`./bl shot landed`.

The hook's answer: `playing=landing first=K1 gliding height=376dp`. Later: `playing=none`, `first=K1
armed=0 1/5`, 252 dp, no `gliding`; the key's step exactly as `docs/research/first-run-key.md` has it, "Open
Keyboard shortcuts" lit, the engine's mark back, the ordinary placeholder, the field's `esc` cap.

Answer (The Lenovo Googlebook, 5 October 2026): yes. The hook's answer: `playing=landing first=K1 gliding height=376dp`. A moment later:
`playing=none`, `first=K1 armed=0`, 252 dp; the key's step as part 2 has it.

## 18. The highlight's way into the key's step

As check 17 with `./bl open stay slow=4`, filmed.

The square leaves its cell in the frame the cells begin to fade, travels almost level to the right, its right edge
first, and rests on "Open Keyboard shortcuts" (y 154 to 186, its right edge at 700), where the answer's own
highlight takes its place. In every frame one highlight and one only, of one strength: no second one fading in at
the answer while the first is on its way, no flash and no dip as the one becomes the other. The lower edge waits
for the cells to fade and then draws in to 252; it cuts no cell. From a list's row instead (`./bl debug first show
at row`, then `land`): the same from the pill; is a denser patch seen on the row for the first frames?

Answer (The Lenovo Googlebook, 5 October 2026): in the film of the real first opening, at ordinary speed: the square leaves its cell as the
cells fade, travels to the right, and where it rests "Open Keyboard shortcuts" is lit; one highlight in each frame
looked at; the lower edge draws in after. From a row the hook answers `gliding height=400dp`. Not run: the film at
`slow=4`, and the look of the first frames from a row.

## 19. Keys in the welcome

Each from `./bl debug first welcome` in an open panel, a second in: `./bl debug key enter`: the hand-over begins
at once from wherever the parts are, and the show follows (`playing=show`). `./bl debug key esc`: `playing=landing`,
the lamp out, the key's step at 252 dp; the panel is still open; `./bl debug key esc` a second later closes it.
`./bl debug key tab` and `./bl debug key down`: as the first Esc. `./bl debug keys x`: the lamp is out in that
frame, the field's band is the theme's own glass with the x in it, the list for x lands; `playing=none`;
`./bl debug type` with nothing after it: the key's step stands. With real keys: Shift alone does nothing.

Answer (The Lenovo Googlebook, 5 October 2026): yes. Enter a second and a half in: the hand-over begins at once and the show follows
(`playing=show`). Esc: `playing=landing`, then the key's step at 252 dp with the panel open. Tab and Down: as Esc.
A typed x: the field holds the x with its list, `playing=none`; the field emptied, the key's step stands. Shift
alone, a real key: nothing. Space, a real key: the cue, as Enter. Not run: the second Esc that closes.

## 20. Keys in the show

Each from `./bl debug first show`, during a beat: `./bl debug key enter`: `playing=landing`; `./bl idle`: the
focused window is still Booklight's panel, nothing has opened and nothing was copied. `./bl debug key esc`: the
same, and the panel is still open. `./bl debug keys x`: the field holds x alone (not Booklight's text and an x),
no chip, the list is the real one for x. With real keys: Esc held down for two seconds lands the key's step and
does not go on to close the panel.

Answer (The Lenovo Googlebook, 5 October 2026): yes. Enter: `playing=landing`, the panel is still the window in front, nothing has opened,
`hold=NONE`. Esc: the same. A typed x: the field holds x alone, no chip, the real list. Space, a real key: lands
the key's step. Not run: Esc held for two seconds.

## 21. No press reaches the key's armed answer

`./bl debug first new`, `./bl debug pref key no`, `./bl open stay`, `./bl debug first welcome`, and a second in
`./bl debug key enter2`: both Enters are the cue (the hand-over is under way; `playing=welcome`, then `show`).
During the show `./bl debug key enter2`, then at once `./bl debug key enter`.

Of those three the first lands the key's step and the other two do nothing: no Keyboard shortcuts dialog comes,
`./bl debug first` says `hold=NONE` and `helper=0`, and the dump `first=K1 armed=0`. With real keys: Enter pressed
again and again as fast as a hand can, from the welcome to the key's step, and Enter held down through all of it:
no dialog. Half a second after the key's step stands, Enter is that step's own again (it asks for the dialog, as
part 2 has it).

Answer (The Lenovo Googlebook, 5 October 2026): yes, with real keys. Six Enters in a row in the welcome: the cue. Eight in a row in the show:
the first lands the key's step, the others do nothing; no dialog, `helper=0`, `hold=NONE`. Enter held down from the
welcome on: the key's step stands, no dialog. And once the key's step has stood for a moment, Enter is its own
again: the dialog comes (`helper=1`, `hold=UNDER`).

## 22. Clicks

In the welcome, once the handle stands: a click on the handle begins the show; a click elsewhere on the glass sets
the key's step down. In the show: a click on a row sets the key's step down, and nothing is opened; the pointer
moved over the rows moves no highlight. A click outside the glass closes the panel, as always.

Answer: not run.

## 23. Seen once

A real first opening, and `./bl debug close` within its first second: `./bl debug first` has no `show` in `done=`,
and the next opening plays the piece again. A real first opening left for three seconds, then closed: `show` is in
`done=`; the next opening shows the key's step at once, at the speed that is set (`slow=false`), with the
ordinary placeholder.

Answer (The Lenovo Googlebook, 5 October 2026): yes. Closed about a second in: no `show` in `done=`, and the next opening is the piece again
(`overture=WELCOME slow=true`). Closed about three and a half seconds in: `show` is in `done=`, and the opening
after shows the key's step at once.

## 24. Where it is not played

With the system's animations off, `./bl debug first new`, `./bl debug pref key no`, `./bl open stay`:
`./bl debug first` said `overture=NONE greets=true`; the key's step stands whole in the first frame, 252 dp; the
dump's `hint=` is "Welcome to Booklight"; `done=` has no `show`. With a screen reader on: the same, and the title
is said before the step's own words. Animations on again: the piece plays at the next real first opening.

Answer (The Lenovo Googlebook, 5 October 2026): with the system's animations off (set for this check and put back): `overture=NONE
greets=true`; the key's step stands at once, 252 dp; the placeholder is "Welcome to Booklight"; `done=` has no
`show`. With animations on again the piece is due (`overture=WELCOME`). A screen reader: not run.

## 25. Heights, and nothing outside the glass

From the dumps of checks 3 to 17, in dp: the welcome 468; apps 400, 344 or 288; the sum 212; the flight 256; the
grid 376; the key's step 252. In each shot: is anything cut at the lower edge, or is there glass with nothing on
it under the last line? Over the backdrop, filmed while the lower edge moves: is there a strip of blur without
glass under the panel, or anything of the welcome beside or below the glass?

Answer (The Lenovo Googlebook, 5 October 2026): the welcome 468; the apps 400 (five rows); the sum 212; the flight 256; the grid 376; the key's
step 252. In the shots nothing is cut at the lower edge and no glass stands empty under a last line. In the film
over the desk no strip of blur without glass was seen under the panel. Over the test backdrop: not run.

## 26. A replay

`./bl debug pref key seen`, `./bl debug first replay show`, `./bl open stay`.

The piece plays at the speed that is set, not at Slow. It lands in "Your key works", and the highlight travels
into "Go on".

Answer (The Lenovo Googlebook, 5 October 2026): yes: `run=REPLAY overture=WELCOME slow=false`; the piece plays and lands in "Your key works"
with "Go on" armed. The highlight's way into "Go on" was not looked at.

## 27. Nobody else sees a change

`./bl debug first off`, `./bl debug pref key seen`; ten openings by the keys: the opening at the speed that is
set, the placeholder, the mark, the caret and the `esc` cap, the lap of light a while after the opening, the tips,
the copy's line and "your usual" as before; the desk behind is not dimmed unless "Dim the desktop" is on;
`playing=none` and no `cast=` in the dump; `./bl debug pref` before and after says the same.

Answer (The Lenovo Googlebook, 5 October 2026): yes. Ten openings by the keys with no run: "your usual" as before (it is on on this device),
`playing=none`, no `cast=`; no dim asked for behind the panel; `./bl debug pref` the same before and after.

## 28. The HP Googlebook, with Alex

Checks 1, 8, 10, 11, 14, 16 and 18 again there.

Answer: not run. The HP Googlebook is not used until Alex says so.

## 29. The footer in the night

During the welcome on a light theme (`./bl debug first welcome at 2900`, over the backdrop's white page; again at
3470 and 3660).

The welcome itself has no footer (a footer stands only under rows): are the field's caret and its `esc` cap the
lamp's own, not the theme's, in the night and on the lit band? And in the show, whose glass is the theme's own
again: are the footer's "Skip" and its cap readable from its first frame, while the night still lifts?

Answer (The Lenovo Googlebook, 5 October 2026): the caret and the cap are the lamp's own: no caret and a white cap in the night, a dark slit
and a cap in the night's ink on the lit band. In the show the footer stands on the theme's glass with "Skip" from
the first list on (in the film). On a light theme over a white page: not looked at.

## 30. A panel closed in the middle

Close the panel one second into the welcome and one second into the show, with `./bl debug close` or a click beside
the glass (Esc would land the key's step first, and a landing marks the piece as shown). Then `./bl debug first`,
and open the panel again.

Does anything of the piece go on after the panel has gone (a step of the show on the glass as it folds)? Is the piece
due again at the next opening where it had played less than two seconds, and shown (the key's step at once) where it
had played more?

Answer (The Lenovo Googlebook, 5 October 2026): closed with `./bl debug close` about a second into the welcome: the piece is due again. Closed
a second into the show (more than two seconds of the piece): it counts as shown. Nothing of it went on: a panel
that closes now ends its piece.

## What the checks found (5 October 2026)

- **The opening piece plays on the device as it was drawn on paper**: the night, the lamp's strike, the seam, the
  light, the words in it, the knife, the hand-over, the show's five beats and the landing, 1 % of its frames late
  in a debug build.
- **No press reaches the key's armed answer** (21), nothing is opened or fetched by the show (14, 20), and the
  piece is seen once (23, 30).
- **A fault, fixed**: a search still on its way when the piece began landed among the show's rows (15).
- **Seen, for Alex**: the title and the line show dimly as soon as the lamp strikes (4); a faint large ring for the
  moment the night lifts (9); the pill stands empty in row one's seat for a frame or two before the first rows (16).
- **Still open**: the night and the shaft over a white page (3, 8, 29); every time against the paper's clock, and
  the films at a quarter of the speed (10, 16, 18); clicks (22); the flight's row in German, with a key, offline
  and a minute later (14); a screen reader (24); Esc held, and the second Esc (19, 20); the lock keys, the Quick
  Insert key alone and the system's own keys during the piece (they act on the system and were not sent); a space that
  comes by the input method and not by the key; the HP Googlebook (28).
- **Looked at again with the part's last build** (the same device, the same day): a real first opening with Space
  as the cue plays through to the key's step (252 dp, the piece shown); Space in the show sets the key's step
  down; a panel closed in the welcome and turned round at once by Booklight's own key stands again with the piece
  over and "Your key works" under the field, no fatal line. Not driven: an Enter in the third of a second after
  that turn (no key can be sent that fast from outside). And everything only a person can say: whether the lamp reads as struck, whether the show is one
  movement, whether eleven seconds are a pleasure.
