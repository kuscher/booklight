# First run: the motion

*Motion designer seat, 4 October 2026; second round the same evening (the overture, Action + Quick Insert, the
lessons in one opening, two rows in C); third round that night (the welcome, a flight in the show). Timed at
the desk from `design.md`, `overlay/Motion.kt` and
`overlay/Panel.kt`; no device was touched, so every number is a starting value until it is filmed (§8). Springs
go by their names in `Motion.kt`; `fade(n)` is its tween of n ms. The prototype is `first-run.html`.*

## 1. Principles

1. **The field and the top edge never move.** No frame changes a pixel of the top 68 dp, apart from the text,
   the mark's seat and the `esc` cap. Typing owns the frame: a letter is in the field in the frame it is pressed in.
2. **Three things move:** the lower edge (the height, on `place`), words (they rise 12 dp or roll half a line)
   and keys (a cap comes up from its pressed size). One highlight, one check and one light do the rest. For
   four seconds, in the welcome, the glass is a stage and light itself moves: there and nowhere else.
3. **Room first, then what stands in it.** A part starts once its seat is inside the glass. Every width is
   measured before anything moves.
4. **One voice leads at a time:** the field, the list or the highlight. The next starts before the last has
   quite settled, so nothing waits and nothing collides.
5. **Seen once, so it may take its time; it holds nothing up.** Only the overture runs by itself, any key ends
   it in that frame, nothing loops. The daily opening is untouched.

## 2. The overture: the welcome, then the show

About eleven seconds, once, on the first start. t = 0 is the first frame. The opening runs at the speed that is
set (Medium below: the gate at 257, the glass at rest at 420). The first letter of the show lands at 3,820, K1
stands at 10.3 s, the last light is gone at 11.1 s.

### 2.1 The welcome (0 to 3,820)

One clock. Every mark is a function of t and of the glass's size: nothing is kept from frame to frame, and the
dust and the grain are seeded. Per frame these change, and nothing else: the night (one number); the lamp (its
brightness, how far the head's light has spread); the light's shape (how far it has fallen, how far it is open,
where its upper edge and its foot stand); five angles; the handle's four edges.

| ms | What | Spec | The eye is on |
| --- | --- | --- | --- |
| 0 to 420 | The daily opening: the seam, the glass at 68, the caret. No mark and no placeholder | `Arrival`, as set | the caret |
| 257 (G) | The lower edge falls from 68 to 468 (there at 481, at rest at 640). **Night falls** after it, slower than the glass grows: the veil goes to #0A0C12 at 0.80, the desk to 0.50, the outline to full white, the caret to white, the corners a little deeper | `place`; `fade(420)` | the lower edge, then the dark |
| 560 | The caret blinks off, as a caret does | cut | |
| 677 to 800 | Full night: the outline and nothing else | 123 ms | the outline |
| **800** | **The lamp strikes**, where the caret would blink on again. Brightness: 60 % after 30 ms, back to 35 % at 70 ms (it has not caught), 100 % at 200 ms, fast at first. A halo of 80 dp round the caret is brightest at 30 ms and gone at 200. From its first frame the caret is the dark slit | new: the strike | the caret |
| 840 to 1,100 | The light floods the head from the caret: the left end at about 880, the right at 1,100. The words show at 0.38 with it: the lamp lights the room dimly. The `esc` cap takes the night's ink as the front passes it | 260 ms on the seam's curve (`SEAM_GROWS`) | the front, to the right |
| 960 to 1,120 | A seam of light, 6 dp, full white, falls on x = 360 from 92 to 448 | 160 ms, `SEAM_GROWS`: the glass's own seam, at the speed that is set | the seam |
| 1,000 | *With dust and a lap:* the lap sets off at the top's middle, over the seam: once round 2 × (720 + 468) dp | 2,200 ms, `REFLECTS`, as built | (edge of sight) |
| 1,180 to 1,540 | The light opens out of its seam to its shape: the glass's own unfold, a second time (60 ms of wait, then the curve). As it widens it relaxes from full white to its fall-off, and its edges soften from 1.5 dp to 26. Title and line light from the middle outwards: the soft edge crosses the letters | `opens(360)` | the title |
| 1,420 | The handle rises into the foot of the light, the cue in it | 12 dp on `place`, `fade(140)` | the handle |
| 1,560 · 1,626 · 1,692 · 1,758 · 1,824 | The five tools flick out from behind the handle, left to right: each swings from folded (lying inside the handle) to its angle and 9 % past it, once. A glint crosses each disc from 90 to 250 ms after it sets off | `pop`, `beat`; glint 160 ms | the fan, left to right |
| 1,540 to 3,200 | **It stands.** One breath: the light swells by 6 % and settles. *With dust:* 24 specks drift up through the beam. All five tools are open at about 2,200: a second to read | a half sine, once | the words |
| 3,200 (H) | **The hand-over.** Booklight presses the handle: ink + 0.08. The tools fold back into it, right to left, 30 ms apart. The lap has just ended | `fade(80)`, out `fade(120)`; 120 ms each, ease in | the handle |
| H + 160 | The shaft turns: its upper edge slides to the caret, the foot follows 60 ms behind and rises, the sides close. It brightens as it narrows. The night begins to lift | 300 ms on `FOLDS`; the rise 360 ms, ease in; `fade(400)` | the light, up and left |
| H + 200 | The cue fades inside the handle | `fade(80)` | |
| H + 240 | The handle sets off for row one's seat (x 8 to 712, y 76 to 132), its colour going to the theme's selection with the night. There at H + 464 | four edges on `place` | the handle |
| H + 380 | The unlit words fade where they stand | `fade(80)` | |
| H + 440 | The head's light closes on the caret from both ends. Booklight's mark comes up in its seat as the light passes it (H + 520) | 160 ms, `SEAM_DRAWS_IN`; `pop` | the caret |
| H + 600 = 3,800 | All that is left of the lamp is the caret, white | | the caret |
| 3,820 | The first letter lands on it | | |

**The light, as one pass.** At a point, with d the distance inside each of its four edges and s the softness
(1.5 dp as a seam, 20 open):

    light = fall(y) × rays(u) × step(dLeft) × step(dRight) × step(dTop) × step(dFoot) + halo + core, ± grain
    step(d) = smoothstep(−s, 0.3 s, d)      fall: 0.34 under the lamp, 0.24, 0.17, 0.125, 0.10 at the foot

The halo is the same shape 14 to 24 dp wider with s = 60, at 0.05 to 0.02; the core is the shape at half its
width with s = 50, at 0.16 to 0.03; the rays are twelve soft bands along the shaft, each adding under 0.05. About
30 lines beside the glass's 70. The lit layer of the type takes the same four steps as its mask.

**Where the polish went, and what of it is inside the tech lead's kit** (`eng.md` §13.2)

| Polish | What it is | In the kit |
| --- | --- | --- |
| The strike | The lamp catches: 60 %, 35 %, 100 % in 200 ms, with a halo at the caret. A fade would be a dimmer, not a lamp | Yes: one number and a radial gradient |
| The caret is the switch | It blinks off in the falling night; where it would come back, the lamp does | Yes |
| The fall-off | Brightest under the lamp, not one even ramp; a core along the axis; a halo outside the edge, so the sides have a penumbra | Yes: the same function three times in the one pass |
| Rays | Twelve soft bands from the lamp to the foot, overlapping so that no edge shows | Yes: a sum of bumps across the shaft |
| Grain | ± 0.03, fixed, so that the fall-off does not band over the blur | Yes: the glass's own hash |
| The breath | + 6 % and back, once, while it stands. Nothing loops | Yes: one number |
| Dust | 24 specks, seeded, each a function of t, lit by the light at its place; every fourth is near and soft | Yes: 24 circles |
| The lap on the tall outline | As built; it sets off as the head is lit and ends as Booklight presses the handle. Its spill on the glass is twice as strong at night | Yes: the reflection, one constant |
| White type catching the light | The lit layer is masked by the light's soft edges, not cut by a path: the edge crosses a letter as a gradient. And a bloom round the lit letters | The mask: yes (gradients, `DstIn`). **The bloom is a blur**: a shadow of the type, drawn once into its layer and then only shown. If a device needs it drawn per frame, it goes |
| Light on less glass is brighter | The seam is full white and relaxes as it opens; the shaft brightens as it folds into the caret | Yes |
| Deeper corners | The night is 0.6 darker towards the corners | Yes: a radial gradient |

Moving marks: 24 specks, 5 tools of 4 marks, the handle, the head, 4 layers of type: about 55 of the 150 allowed.
One shader pass; the reflection is the glass's own. **If a frame is late (8.3 ms)**, in this order: the dust, the
bloom, the rays, the halo and the core, the grain, the lap.

### 2.2 The show (3,820 to K1)

New values, no new spring: `perform` (64 ms a letter; 80 before a letter that lands a row; 124 before the space
that makes the chip: a hand, not a metronome, and never under two frames); `LAND_AT` (the lap's `run` reaches
0.38, 776 ms after it sets off; the lower edge follows 160 ms later, at `run` 0.5); the dim's lift, `fade(640)`.
"Lands" means a frame or two after its letter: a search is a job. No time below names that frame.

| ms | Booklight | What moves | Spec | The eye is on |
| --- | --- | --- | --- | --- |
| **B1** 3,820 | the letter | It lands on the caret. The list lands: the lower edge draws up from 468 to 400, five rows rise 22 ms apart. The pill is already in row one's seat; its strip comes 60 ms later, the footer rides the lower edge | `place`; `stagger` | the rows, then row one |
| 4,300 | Down | The pill's lower edge goes; the upper holds five frames, then gathers. The row's strip 60 ms later | `pillLead`, `pillTrail` (at rest + 197) | the pill |
| 4,600 | Down | The same, to row three | | |
| 4,920 | Tab | The pane glides to Window, the name unrolls | `arm` (90 % at + 123) | the row's right end |
| 5,160 | Tab | The pane glides on to More | `arm` | |
| **B2** 5,380 | `1`, typed over the letter | The text changes in one frame. `5 0 ␣ + ␣` follow. The app rows stand | cut | the field |
| 5,780 | `2` | The sum's row lands in row one's seat as the app rows fade where they stand (80). The pill's upper edge leads 112 dp up to it, the lower holds two frames and follows; it is 92 high. The lower edge draws up from 400 to 212. "Copy ⏎" 60 ms later | `pillLead`, `pillTrail(far)`; `place` (at rest + 335) | the pill going up |
| 6,120 | `0` | 152 rolls to 170 | `roll` | the answer |
| 6,420 | `%` | 170 rolls to 180, and is held | `roll` | 180 |
| **B3** 6,900 | `L`, typed over the sum | `H 4 5` follow. The answer stands | cut | the field |
| 7,172 | `5` | **The flight's row lands** in the same seat as the sum's row fades (80): line one, the headline with its one green badge, the line at rest, the two ends, "Example" where the kind stands. The pill's upper edge stays on 76; its lower edge goes from 168 to 212 with the row. The lower edge of the glass goes from 212 to 256. The footer's left end says it is an example | rises, as a row; `place` (at rest + 200); words cross-fade 80, 120 | the headline |
| 7,232 | | The plane fades in at the line's start, under the headline's first letter, and flies to its place at 61 %: the flown part grows behind it, the dots ahead step to "still to go". There at 7,715; then it is still | `flies(0.61)`: 483 ms on the glass's curve, as built | the plane |
| **B4** 8,280 | `e`, typed over the number | `m o j i` follow. The flight's row stands: it has been whole for 0.6 s | cut | the field |
| 8,660 | the space | The keyword becomes the chip where the mark was. The flight's row fades (80). The pill's four edges and its radius go to the first cell: a 44 dp square. The cells rise in their diagonal wave. The lower edge goes from 256 to 376 | chip as built; pill `place` (there + 224); cells 8 dp, `fade(110)`, 8 ms a step, landed + 230; `place` | the chip, then the wave |
| 8,900 | | The light sets off from the top's middle | 2,192 ms, `REFLECTS` | (edge of sight) |
| 9,060 · 9,170 · 9,280 | Right × 3 | The square glides three cells: the first step holds five frames, the next two do not, as with a held key. The cell's name in the footer cuts | `pillLead`, `pillTrail` | the square |
| 9,440 | Down | One settled step, with its stretch and gather; at rest at 9,637. The name cross-fades | the same | the square |
| **B5** T = 9,676 | | **The light turns onto the lower edge.** The cells leave in the wave they came in, top left first. The chip shrinks and fades, the engine's mark grows from 0.6. The footer fades. The dim begins to lift | cells `fade(80)`, 8 ms a step, gone at + 200; `pop`; `fade(640)` | the wave |
| T + 32 | | The highlight leaves its cell as the wave passes it. Its right edge leads to x = 700, the left follows two frames later; top, bottom and radius go to the answer's. Longest 243 dp at + 77, there at + 180 | `pillLead`, `pillTrail(far)`, `place` | the highlight |
| T + 80 · 150 · 190 | | K1's disc, its title and line, its counter rise, each once the wave has left its seat | as rows | |
| T + 160 | | **The lower edge draws in** from 376 to 252, behind the wave: it cuts no cell. The light is at its middle and rides it up | `place` (there at T + 384, at rest T + 464) | |
| T + 180 | | "Open Keyboard shortcuts" and its Enter mark fade in inside the pill. Enter counts from here | `fade(110)` | the answer |
| T + 260 · 326 · 392 | | `Action`, "+", `Quick Insert` come up | `pop`, `beat` | back, to the caps |
| T + 458 · 480 | | The caption fades in; "Not now" rises | `fade(140)`; `place` | |
| 10,326 | | K1 stands as `design.md` §5 draws it. The desk is back | | the suggestion |
| 11,092 | | The light rode the lower edge as it rose (to about 10,090), climbed the left side, dimmed along the top left and is gone | as built | |

### The one highlight

It is born as the knife's handle (it fades in once, at 1,420). After that it only travels, until it is K1's own.

| From | To | Its edges |
| --- | --- | --- |
| the handle (x 252 to 468, y 372 to 420) | the pill in row one's seat | Four edges on `place`; its colour goes from the night's to the theme's selection |
| a row | the next row | The front edge at once on `pillLead`; the old edge holds five frames, then `pillTrail`. Stretch 34 dp |
| row three | the answer's row, 92 dp | The upper edge leads; the lower holds two frames, then `pillTrail(far)` |
| the answer's row | the flight's row, 136 dp | The lower edge alone, on `place`, with the row |
| the flight's row | the first cell | Four edges and the radius (24 to 14) on `place`: it condenses into the cell the wave starts from |
| a cell | the next cell | As the list's pill |
| the cell | K1's armed answer | Right edge `pillLead`; left two frames later, `pillTrail(far)`; top, bottom, radius (14 to 16) on `place`. In the frame it rests it is that strip's own highlight |

### The lower edge, the light, the dim

- **The edge** goes 68, 468, 400, 212, 256, 376, 252, each on `place` from where it is: at rest at 640, 4,060,
  6,135, 7,390, 8,990 and 10,140. After the welcome it is still for 1.7 s, 1.1 s, 1.3 s and 0.8 s between moves.
- **The light** is as built: one lap, 1 ms a dp of the outline it sets off on (2,200 ms on the tall glass, 2,192
  at the grid). It is placed by `run` on the outline of each frame. A change of height shifts it along the
  outline everywhere but on the lower edge, where at `run` 0.5 it does not move at all. So the landing is cued
  by the light and not by a clock: the wave leaves as the light turns onto the lower edge, and the edge draws in
  while the light is on it (`run` 0.5 to 0.64), so that the light rides it up and both reach the lower left
  corner together. With the lap cut, the cue is 776 ms after the wave lands. Booklight's own typing and moves
  do not put a lap out; any key of the user's does (160 ms, as built). On the tall glass nothing changes height
  while the lap runs.
- **The dim** is 0.50 at night. It goes to the show's 0.10 (light) or 0.22 (dark) as the night lifts, and lifts on
  its own time from T.

### What I changed from the designer's proposal

| The proposal | Now | Why |
| --- | --- | --- |
| The night falls from 0.42 to 0.9 s, with the lower edge | The edge leaves at the gate (257); the night takes 420 ms, and is full at 677 | The opening hands over to the height as it does every day. The glass is tall for a moment before it is dark: the fall is seen |
| The lamp, 0.9 to 1.1 s | It strikes at 800, where the caret would blink on again; 60 %, 35 %, 100 % | A lamp is switched on. And the caret is the switch |
| The seam of light, then the light opens: 1.1 to 1.7 s | 960 to 1,540, with the glass's own times (160, 60, 360) and curves, begun while the head is still flooding | The light makes the move the glass made a second before. No beat waits for the last |
| The knife, 1.7 to 2.3 s | The handle at 1,420, in the light's last third; tools from 1,560; all open at about 2,200 | |
| The read, 2.3 to 3.3 s | It stands from 1,540 to 3,200: the words are lit for 1.7 s, the whole picture for 1.0 s | |
| The hand-over, 3.3 to 3.9 s: three moves | 3,200 to 3,820. The tools lead (0), the shaft follows (160), the handle (240), the head closes last (440) | One leads at a time; each starts before the last has ended |
| Eight seconds of show; each beat ends, then the next begins | 6.5 s for one beat more. The next phrase is typed while the last picture stands | Rests between beats are what makes slides |
| The landing comes at a time | **The landing waits for the light**; the edge sets off 160 ms after the first cell | The light rides the rising edge and cannot be seen to jump (the designer's check); the edge wipes no emoji that has not begun to fade |
| Typing at one pace; four even moves of the square | 64 ms, slower where it commits; three quick steps and one settled | A hand; the glide of a held key and the stretch of one step, each shown once |
| A key in the welcome: the stage's parts fade | The handle, once it is there, goes to row one of the user's list and is not faded | One highlight, also then |

### Interrupting it

| The user | In the welcome | In the show |
| --- | --- | --- |
| A key that types | The lamp is out in that frame, as a lamp is: the field's band is the theme's own glass with the character in it. The light, the head and the dust are gone at once; the words, the tools and the cue fade where they stand (80); the night lifts (160); a lap fades (160). The handle, if it has risen, goes to row one of the list for that character on `place`. The lower edge turns to that list's height from where it is. No show. K1 is set down when the field is empty again | The overture ends in that frame, in any beat. Booklight's text and chip are gone and the character is the whole text. Its list lands as any list: what was Booklight's fades where it stands (80), the highlight goes to row one on `place`, the lower edge turns to that list's height. The mark is the engine's (`pop`), the dim lifts (640), the light fades in 160 ms. K1 when the field is empty again |
| Enter, a click on the handle | The hand-over starts in that frame, from wherever the parts are; the show follows | K1 lands, as below |
| Esc, Tab, an arrow, a click on the glass | K1 lands: the lamp is out, the stage's parts fade (80), the night lifts (160), the edge goes to 252 from where it is, K1 arrives as rows do | The landing starts in that frame: a list's rows fade together where they stand, the highlight glides from where it is into the armed answer, the edge goes to 252. Nothing is run. One press, one step |
| The same during the landing | | Nothing more. Enter counts from T + 180 |
| A click outside, the focus lost | The panel closes. K1 at the next opening | The same |

**With the system's animations off, or a screen reader on, there is no welcome and no show.** K1 stands whole in
frame one, and for that opening the field's placeholder is the welcome's title.

### What to cut first in it

1. The dust; then the bloom on the lit type.
2. Tab, Tab in B1 (0.46 s).
3. The square's three quick steps: Right once, then Down.
4. The glints; then the tools open together.
5. The lap on the tall outline (the lap at the grid stays: it cues the landing).
6. The seam of light: the light opens at once (0.22 s).
7. The shaft's turn: it fades where it stands, and the caret is simply there.
8. The sum: apps, the flight, the grid.

Never cut: the night, the strike, the words lighting from the middle, the handle becoming the highlight, the
flight's plane arriving, the cascade, the wave, the landing cued by the light, the first key ending it.

## 3. The other transitions

**Clocks.** t = 0 is the trigger each table names. An ordinary opening has marks of its own (ms after the key;
Medium is the default):

| | Fast | Medium | Slow |
| --- | --- | --- | --- |
| **G**, the gate (glass 85 % open): the height leaves 68 | 129 | 257 | 515 |
| The glass at rest | 210 | 420 | 840 |
| **B**, the band's start: the later of G + 120 and the glass at rest | 249 | 420 | 840 |

**Values from the first round.** `beat` 66 ms (three steps of `stagger`: one key after another).
`KEY_HELD_MS` 400 (the helper's exit, about 240 ms, to be measured, and 160 ms in view). `KEY_TAP_MS` 160. The
landing of a key: the check 60 ms after the release, the lap 240 ms after it. New: `AGAIN_AFTER_MS` 400.

**Words used below.** *Rises*: 12 dp up on `place` with `fade(140)`, as a row. *Comes up*: a cap, `fade(110)` and
0.96 to 1 on `pop` about its centre. *Written*: a typed part, letter by letter, each letter cut in, `typeStep`
apart (40 ms at these lengths), in the stage and never in the field. *Rolls*: `roll()`.

### T1. K1 without the overture (an update without a key, a later opening). Trigger: the start

| ms | What | Spec |
| --- | --- | --- |
| G | The height leaves 68 for 252 | `place` |
| G, G + 22, G + 44 | The disc, the title with its line, the counter rise | times `ends()` |
| B, B + 66, B + 132 | `Action`, "+", `Quick Insert` come up | `pop`; `fade(110)` |
| B + 198 | The caption, under the caps. No travel: it stands on a footer's line | `fade(140)` |
| B + 220 | The answers rise, the armed one's pill with them. Enter counts from here | `place`, `fade(140)` |
| B + 390 | At rest: 810 ms after the key at Medium | |

A screen that comes back is not set down again at this pace: its parts arrive as rows do, 22 ms apart from G.

### T2. The hand-off. Trigger: Enter on "Open Keyboard shortcuts"

| ms | What | Spec |
| --- | --- | --- |
| 0 | The armed slot is pressed: ink + 0.08. The helper is asked for in the same frame | `fade(80)`, out `fade(120)` |
| the system's | The helper covers the panel and dims the rest. Nothing of the panel moves or leaves | not ours |
| focus lost + 300 | Under the helper the stage becomes K2 (K3 from the second time): the words roll; the caps and the "+" fade where they stand (80), the blank key (112 dp) fades in at x = 72 (110, 40 ms late); the caption cross-fades (80 out, 120 in); the pill fades out, the `tab` cap in (120) | `hold(300)`, `roll`, `fade` |

It runs as motion, not as a cut: nothing pops on a device that shows the panel beside the helper.

### T3. The reveal. Trigger: the start by the user's keys arrives while the helper is up

| ms | What | Spec |
| --- | --- | --- |
| 0 | Under the helper: the words roll to K4's, the caption goes, the answers cross-fade to "Go on", armed; "your key" goes down: 0.96, fill + 0.08 | `roll`, `fade(80)` |
| 0 to about 240 | The dialog falls away, its dim lifts. The first frame that shows the panel shows the key down | the system's |
| 400 | The key comes up: at its size at 508, a fifth of a dp past it at 552. The pressed fill leaves | `pop`; `fade(120)` |
| 460 to 600 | The check draws: 28 dp, stroke 2.5 | `DrawnCheck`, 140 ms, linear |
| 640 | The reflection: once round 2 × (720 + 252) dp | 1,944 ms, `REFLECTS`, as built |
| 2,584 | Rest. K4 waits for Enter or a letter | |

### T4. K2 or K3 to K4, the keys pressed in view. Trigger: the same start, the panel in front

"Your key" goes down at 0 (`fade(80)`) while the title rolls, the line 22 ms later, and the caption and the two
answers fade where they stand. "Go on" fades in with its pill at 80. The key comes up at 160, the check draws
220 to 360, the lap sets off at 400. *The panel had closed and the keys open a new one:* K4 at the gate as T1; the
key comes up at B, the check at B + 60, "Go on" at B + 66, the lap at B + 240.

### T5. A lesson, set down

The recipe is written from x = 72 at one pace: a letter 40 ms after the one before, a cap or a new typed part
one `beat` after the piece before it. The caption fades in one `beat` after the last cap; the `tab` cap and Skip
rise 22 ms after it.

| Recipe | Written, ms after its start | Caption · Skip | At rest |
| --- | --- | --- | --- |
| L2 `yout` `⏎` | letters 0 to 120, `⏎` 186 | 252 · 274 | 445 |
| L3 `yout` `Tab` `⏎` `lofi` `⏎` | 0 to 120, 186, 252, 318 to 438, 504 | 570 · 592 | 760 |
| L4 `150 + 20%` `⏎` | 0 to 320, 386 | 452 · 474 | 645 |

*In the same panel, after "Go on" or Skip (252 stays).* Trigger: Enter on the armed answer.

| ms | What | Spec |
| --- | --- | --- |
| 0 | The slot is pressed. The old recipe (or the key and its check) fades where it stands | `fade(80)` |
| 0 | The disc stays. Its symbol shrinks to 0.6 and fades; the new one grows from 0.6 | `pop`, `fade` |
| 0, 22, 44 | The title rolls, the line rolls, the counter's digit rolls | `roll` |
| 0 | "Go on" and its pill fade. Skip stays where it stands: its pill fades out, the `tab` cap comes back. A caption whose words do not change does not move | `fade(80)`, `fade(120)` |
| 120 | The new recipe is written, as above | `typeStep`, `beat` |

*In a panel that came back (T6):* at its gate the disc, words and counter rise as in T1, and the recipe is
written from B.

### T6. The step aside. Trigger: Enter on an app's row (L2), or on the word under the chip (L3)

| ms | What | Whose |
| --- | --- | --- |
| 0 | The armed slot is pressed, the app is started, the step is written | ours, as every day |
| 0 to 155 | The glass folds to its seam and is gone | ours, as every day; the system's fade of a see-through task (about 200 ms) lies over it |
| about 30 on | The app's window comes to the front | the system's |
| 400 | Booklight asks for itself again | new: `hold(AGAIN_AFTER_MS)` from the app's start |
| about 430 | The new panel's first frame: the seam, on the same centre line. Then its ordinary opening at the speed that is set | ours. The system fades the new window in over about 200 ms: it lies over the seam's growth (160 ms at Medium) and is gone before the glass is half open |
| 430 + G = 687 | The next lesson is set down at the gate; its recipe is written from 430 + B = 850 | ours |
| about 1,500 (L4), 1,610 (L3) | At rest | |

The app is seen bare for about 275 ms (155 to 430). Sooner, the fold and the new seam overlap and read as a
flicker; much later the system has taken the panel away for good (1.5 s was too late on the device). What the
eye sees between the two glasses is the system's and was not filmed: §8.

### T7. Typing in a lesson. Trigger: the key

| ms | What | Spec |
| --- | --- | --- |
| 0 | The letter is in the field, the placeholder gone. The stage fades where it stands. The height turns to the list's from where it is, with the speed it has | same frame; `fade(70)`; `place` |
| the list lands | Rows rise 22 ms apart; the pill fades in on row one; the footer and its coach line fade in on the lower edge | `stagger`; `fade(120)`; `fade(140)` |

The field emptied: the rows fade (80), the lesson fades in whole where it stood (140), the height returns to
252. It never visits 68 and nothing is written twice. The coach line's words cross-fade where they stand, 80
out and 120 in; changes under 120 ms apart cut.

### T8. The sum copies and stays; the question rises. Trigger: Enter on the sum's row

| ms | What | Spec |
| --- | --- | --- |
| 0 | The slot is pressed. "Copied" takes the coach line's seat with its check | `fade(80)`; as built |
| 520 | When on any other day the panel would go: the field is emptied in one frame and the placeholder comes back; the rows, the pill and the footer fade where they stand; the lower edge goes from the list's height to 324. It never visits 68 | `hold(520)`; `fade(140)`; `fade(80)`, `fade(70)`; `place` |
| 580 to 722 | Q's parts rise in reading order: disc 580, title 602, counter 624, the text 646, the note 700, the `tab` cap and the answers 722 | as rows |
| 900 | At rest. Nothing is armed | |

*Q come back to, at a gate:* the same offsets from G.

### T9. The question and the choices

*Tab.* First press: the pill fades in on Agree where it stands, with its Enter mark; the `tab` cap fades out
(`fade(120)` each). The next: the pill's far edge goes on `lead`, the near one on `trail`, as built
(`OptionStrip`).

*Q to C.* Trigger: Enter on an armed answer.

| ms | What | Spec |
| --- | --- | --- |
| 0 | The slot is pressed. Q's parts and the pill fade where they stand | `fade(80)` |
| 60 | The lower edge rises 56 dp, from 324 to 268: only now, so that it cuts no word that is still fading | `place` |
| 60, 82 | C's two rows rise; the footer fades in on the lower edge | `stagger`; `fade(140)` |

*In C.* Down: the pill fades in on row one, in place (`fade(120)`); between the rows it travels as built. Enter on
"Show your usual": the thumb goes 20 dp on `pop` (1.7 dp past, inside the track's 3 dp), the track's fill
cross-fades (120), "Off" rolls to "On". The panel stays.

### T10. The fold. Trigger: Enter with nothing selected

| ms | What | Spec |
| --- | --- | --- |
| 0 | The rows and the footer fade where they stand | `fade(70)`, as built |
| 0 | The height returns from 268 to 68: half way at 69, there at 224, at rest at 338 | `place` |
| 0, 60 | The placeholder goes (60); the ending's line comes | `fade(140)`, as built in `Field` |
| 120 | The `esc` cap fades back | `fade(120)` |
| 360 | The reflection: once round the bare field, 2 × (720 + 68) dp | 1,576 ms |
| 1,936 | Rest: the product, over the app the user opened | |

## 4. The large cap, pressed

| Phase | ms | What | Spec |
| --- | --- | --- | --- |
| Down | 0 to 80 | Scale 1 to 0.96 about its centre; the fill gains ink 0.08 | `fade(80)`: the pressed token's time in |
| Held | to 160 in view, to 400 at the reveal | Nothing moves | `KEY_TAP_MS`, `KEY_HELD_MS` |
| Up | at its size after 108, 0.19 dp a side past it after 152 (the 112 dp key), at rest after 186 | Scale to 1; the fill leaves | `pop`; `fade(120)` |

The scale is never over 1.0034 and never under 0.96. The label and the check ride it. An arriving cap is the
"up" half alone: every cap on the stage has moved the way the user's key will. The 190 dp `Quick Insert` cap is
0.3 dp a side past its size for a moment, as it comes up.

## 5. Interruption, after the overture

A letter and Esc win in the frame they are pressed in, in every row. Nothing is finished first.

| During | A letter | Enter | Tab | Esc |
| --- | --- | --- | --- | --- |
| A set-down (T1, T5, Q rising) | T7. Parts not yet started never start | Before the armed answer has begun to show: nothing, not kept. After: it runs | Parts not yet started start in that frame, the recipe whole; the first answer is armed | The glass folds over what stands (155 ms, as built). Next opening: the same screen, as rows |
| T2, before the helper has the focus | Typed. The helper later uncovers the list | Not kept | Nothing | Closes. The helper opens without Booklight's page; K1 next time |
| The landing of the key (T3, T4) | T7. Key and check fade with the stage. The step was written at t = 0 | "Go on" at once, from wherever the key is | Nothing: one answer | Closes. L2 next time |
| The step aside (T6) | Before the new panel has the focus it goes to the app (about 90 ms, accepted). After: T7 in the new panel | From the lesson's Enter until the next lesson stands: nothing | The same | Closes the new panel. The next lesson waits |
| "Copied", before Q (T8) | T7; the question comes when the field is empty again | Nothing | Nothing | Closes. Q next time |
| Q to C, the fold | T7 from where the height is. No lap | Runs the armed answer, if any | Arms the next | Closes |
| A lap | Any key: it fades in 160 ms while it goes on and does not come again (as built) | | | |

**The user's keys again.** During the landing of the key a held key repeats, and each repeat is a new start:
until the check is whole (600 ms in T3, 360 in T4) a second start does nothing. During the step aside the
user's keys count as the return: they do not close the panel that is coming back.

## 6. System animations off

Every spring and tween is `snap()`, `stagger` and `typeStep` are 0, no light runs, and there is no welcome and no
show: no still of the stage stands, because it is a picture made to move.
`hold(300)` in T2, `hold(520)` in T8 and `hold(400)` in T6 stay. `KEY_HELD_MS` and `KEY_TAP_MS` are motion and are 0.

| Screen | The still |
| --- | --- |
| The first start | K1 in the first frame, 252 dp: caps, caption, the armed answer. The field's placeholder is the welcome's title |
| Under the helper | K2, or K3 |
| The reveal, T4 | K4 in one frame: the key up, the check whole, "Go on" armed |
| L2 to L4 | The recipe whole, in its order, with its caption. A letter: the list in one frame |
| After an app opens | The new panel stands whole in its first frame, the lesson with it (under the system's fade) |
| Q | Whole. Tab puts the pill and its mark on the answer |
| C | Both rows. The switch and its word change in one frame |
| The end | 68 dp, the ending's placeholder |

Nothing is told by motion alone: "done" is the check and the title, "armed" the pill and its mark, the order
the recipe's left to right, "Booklight is back" the panel itself.

## 7. What to cut first, after the overture's own list

1. `beat` goes to 22 ms: the caps come as a cascade.
2. The recipe is not written: typed parts arrive whole.
3. `pop` on an arriving cap: it only fades in. The pressed key keeps its spring.
4. The lap at the fold.
5. The seat's cascade: its three parts together.
6. The lap at the reveal.

Never cut: the key held down and coming up, the check, the height on `place`, the letter in its own frame.

## 8. Measuring

**With what exists.** `./bl open stay slow=4` stretches every spring and tween four times; `./bl debug key
tab|enter|down|esc` and `./bl debug keys TEXT` press and type as the keys do; `./bl debug pref key seen|no` sets
the key's state; `./bl debug pill N` logs the highlight's two edges. Film with `adb shell screenrecord`, step
through the frames, delete the film (it shows the whole screen). Every new wait goes through `hold()`, so that
`slow` stretches it.

**Asked of the tech lead** (`eng.md` §13.2 has the first three). `./bl debug first welcome` (play it, also at
`slow=4`), `first welcome at MS` (stand it still at t, for `./bl shot` beside the prototype's frame of the same
t), `first overture`, `first press` (the landing of the key without the helper) and `first trace N` (log N
frames: each frame's time, the height, the highlight's four edges, the night, the lamp, the light's five
numbers, the key's scale, the check's share, a lap's `run`, the dim).

| What | It must be |
| --- | --- |
| The top 68 dp, frame to frame, in every transition | The same pixels, apart from the text, the mark's seat and the `esc` cap |
| Anything of the stage below the glass's lower edge; a cap, or a cell that has not begun to fade, cut by an edge | In no frame |
| The welcome's marks: the gate, the strike, the seam, the light open, the first tool, the hand-over, the letter | 257 · 800 · 960 · 1,540 · 1,560 · 3,200 · 3,820, within two frames each |
| The welcome at t = 850, 1,090, 1,330, 1,750, 2,900, 3,470 and 3,660 | The prototype's frame of the same t, laid beside it: over the white and the dark test window, both themes |
| Every frame of the welcome | Under 8.3 ms on both Googlebooks; nothing drawn outside the glass |
| The show's marks: `2`, the flight's row, the plane at its place, the space, the lap, the landing, the edge, K1 at rest, the light gone | 5,780 · 7,172 · 7,715 · 8,660 · 8,900 · 9,676 · 9,836 · 10,326 · 11,092, within two frames each |
| The field in the show | Never empty from 3,820 to 8,660; the lower edge never under 212 after 3,840 |
| The highlight, from 1,560 until K1 rests | Exactly one in every frame, at full alpha; never longer than its target and 56 dp |
| The light | On the outline in every frame; never a step backwards; on the lower edge while the edge moves |
| A typing key in the overture | The character alone in the field in the next frame; in the welcome the lamp is out in that same frame |
| The step aside | Frames without glass between the fold and the new seam: about 33 at 120 Hz. Frames with two glasses: none |
| K1 at rest after the key (T1) · the reveal: key up, check whole, the lap's length | 810 ms at Medium · 400, 600 and 1,944 ms, within one frame each |
| The slowest frame while the height moves over the blur | Under two frames of the screen |

**Only a device can say.** The helper's own exit sets `KEY_HELD_MS` (count the frames from the first in which
it is lighter to the last in which it shows). The step aside, filmed at 120 Hz on both Googlebooks, sets
`AGAIN_AFTER_MS`, and decides whether the lessons open apps at all (`eng.md` §12.1).

## 9. Decisions for Alex

Settled by his answers to the first two plans, all as recommended: how a cap arrives, the written recipe, the
key held 0.4 s, its release on `pop`, the light after the check, the light at the fold; the show's overlapping
beats, the landing cued by the light, Booklight's typing rhythm, the square's three quick steps and one settled.
Still open from the second round: the panel's return after an app opens (its ordinary opening 0.4 s after the
app's start, or at once on the system's fade alone: `eng.md`, decision 8).

| # | Decision | Recommended | If you say the other thing |
| --- | --- | --- | --- |
| 1 | How the lamp comes on | A strike: 60 %, 35 %, 100 % in a fifth of a second, from the caret | A rise of 200 ms: calmer; it reads as a dimmer, and the caret is no longer the switch |
| 2 | The light | With dust and a lap, and with the rays, the core and the breath in every level but the solid one | The plain shaft: the designer's table as it stands, a day less of tuning. Or solid, as the icon: the mark exactly, and hard on the eye at night |
| 3 | The bloom round the lit letters | Yes. It is the one thing outside the tech lead's kit: a blur, drawn once | None: the type is crisp and wholly inside the kit; it catches the light less |
| 4 | When the lap runs on the tall outline | From the head lit (1.0 s) to the press of the handle (3.2 s) | From the knife: it would still run while everything goes back up, and compete with it |
| 5 | The hand-over | 0.62 s, four moves that overlap: tools, shaft, handle, head | 0.9 s, one after another: each is seen alone; the welcome passes four seconds |
| 6 | A key during the welcome | The handle travels to row one of the user's list | It fades with the rest (as `design.md` has it): for 80 ms two highlights cross-fade |
| 7 | How long the flight rests | 0.6 s after the plane is at its place, and through the typing of `emoji` | 1.2 s: time to read both ends; the show is 0.6 s longer |
