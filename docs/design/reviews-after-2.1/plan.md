# Booklight: small improvements on main, after 2.1

*The plan as it went to the two designers. What they decided, over one visual and four motion rounds, is in `visual.md` and `motion-1.md` to `motion-4.md` beside this file; what was built is `docs/design/design-system.md` §12. The pictures and recordings named here were scratch and are not kept.*

Alex's words (1 October 2026):

> First, default to medium for opening speed. second the shimmer which flies around the edge of the box should
> come like a few seconds later and be more complete. like start in the top middle and run around once. it should
> look more like a white reflection running around. this is purely fun and will fade fast if we close. Then it
> doesnt show the actions for apps like chrome which has new tab as one. also the many Window management optoins
> by default are too much. do only maximize, left, right on the default page. next up the highlight feels stark on
> dark. is there a way it can be sort of transparent too and a bit less color (but not devid). make a plan for all
> of these changes too and run it through a visual designer to ensure it looks good. also make sure the motion
> designer really approves of any motion especially the shimmer. Have him look at the open animation at medium
> speed too again to see if there are improvements like slow beginning then fast then slow again sort of as a
> curve to show the end animation more than the mid.

Candidates for all of it are built in the working tree (not committed) and were captured on the Lenovo
Googlebook 15 (2880 × 1800, 1 dp = 1.5 px, 120 Hz) over the app's own test backdrop (a white page on the left, a
dark terminal on the right). Pictures are in this folder.

## 1. The opening is at medium speed unless chosen otherwise

- `Settings.opening` defaults to `medium` (was `fast`). The setting keeps Off, Fast, Medium, Slow.
- Everyone who has the app has `fast` stored, chosen or not (the file stores every field). So once, on the first
  start of this version, `fast` becomes `medium` (settings schema 4). Someone who really chose Fast sets it again.

## 2. The opening's curve (motion designer)

**Today** (`overlay/Panel.kt`, `Arrival`, `Motion.open`). Times are for Fast; Medium is twice, Slow four times.
1. A seam of outline, 6 dp wide, grows up and down on the field's centre line: 80 ms, `cubic-bezier(0.2, 0, 0, 1)`.
2. 50 ms after the seam began, the glass opens out of it to both sides on a spring (damping 0.72, stiffness
   1000; at Medium 250). A spring starts at full acceleration: at Medium the glass is 56 % open after 100 ms and
   97 % after 200 ms, then lands (an overshoot turns back instead of being drawn outside the window).
3. Contents fade in while the glass is between 35 % and 85 % open; the window's blur comes into focus between
   50 % and 100 % (the blur is the whole window's, so it waits until the glass is more than half open); what is
   under the field may arrive once the glass is 85 % open.
4. Closing is the opening backwards: the glass folds to the seam in 105 ms on `cubic-bezier(0.45, 0, 0.4, 1)`,
   then the seam draws in. Alex asked for that mirror in 1.1.1.

**What Alex asks:** "slow beginning then fast then slow again sort of as a curve to show the end animation more
than the mid".

**Candidates** (step 2 only; a tween in place of the spring; 180 ms at Fast, 360 ms at Medium, 720 ms at Slow):
- A: `cubic-bezier(0.55, 0, 0.1, 1)`.
- B: `cubic-bezier(0.7, 0, 0.1, 1)` (a slower start).
- Today's spring, for comparison.

`opening-curves.md` has the three as numbers (per cent open every 20 ms, at Medium). `open-medium-spring.png`,
`open-medium-a.png`, `open-medium-b.png` are frames of the real thing, one every 20 ms of real time (recorded
four times slower so that none is missing). 0 ms on those sheets is the first frame in which anything of the
panel shows.

**Open questions for the motion designer:** which curve, or another (give the bezier or the spring); the time at
each of the three speeds; whether the seam and the wait before it want other times with the new curve; whether
the windows of step 3 should move (with a long slow end the glass stands at 90 to 99 % for longer: a band of blur
without glass stands beside it meanwhile); whether the glass should still land with a small bounce; and the
closing: mirror the new curve, or leave it.

## 3. The light on the edge becomes a white reflection, later (visual and motion designer)

**Today:** 190 ms after the glass has opened (380 ms at Medium), one light runs once around the outline in
1.1 s, white at its core with the device's two accent colours ahead and behind. While the on-device model works
on an answer it runs again, slowly, until the first word.

**Candidate** (`overlay/Glass.kt` shader, `overlay/Panel.kt`):
- It comes **2.4 s after the panel has opened**, and only in a quiet moment: what is typed and what it finds must
  have stood still for 0.7 s. Once per opening. If the panel closes first, it never comes.
- It starts in the middle of the top edge, runs clockwise once around the outline and ends where it began, a
  little past it, fading over the last stretch (its tail leaves too).
- It is **white only**. Where it is, the outline (1.25 dp, white at 80 % in light theme and 44 % in dark) goes to
  full white and widens to 2.5 dp, and a little of it falls on the glass beside the line, fading inwards over
  about 7 dp (up to 30 % white in light theme, 22 % in dark). Its shape along the outline: a short front (36 dp)
  and a long tail (230 dp).
- The lap: 1.5 s in all on `cubic-bezier(0.35, 0, 0.3, 1)`; the head is back at the top's middle after about 1.0 s.
- When the panel leaves it fades in 90 ms (the panel itself folds away in about 150 to 300 ms).
- The model's "thinking" light is the same reflection now (white, slow laps of 2.4 s, finishing its lap when the
  first word lands).
- With the system's animations off it does not come at all.

Pictures: `reflection-dark.png` and `reflection-light.png` (five moments of the lap at full size),
`reflection-dark-lap.png` (every 60 ms of the lap), `reflection-dark-rows.png` (around a taller panel, half size).

**Known:** in light theme over a white page it is nearly invisible (white on white); over anything darker it shows.

## 4. An app's row: six icons, the rest behind the arrow (visual designer)

**Today:** nine icons (Open, New window, App info, Left half, Right half, Full, Left third, Middle third, Right
third), then the arrow, which opens two thirds, the four quarters, Centre and Uninstall as a list under the row.
**Candidate:** Open, New window, App info, Left half, Right half, Full, then the arrow; the three thirds join the
list (eleven lines). Typed places still work at once ("chrome left third").
Pictures: `strip-light-all.png`, `strip-dark-all.png` (at rest; armed on Left half; the list opened).

**Open questions:** the order of the three places (Alex said "maximize, left, right"); the word "Full" (German
"Maximiert"; Alex says "maximize"); whether App info should stay on the row; the order and grouping of the list.

## 5. Chrome's "New tab": found, not fixable as a plain app

Booklight already shows an app's own shortcuts as rows ("New event" under Calendar, Keep's four, YouTube's
three). Chrome's are missing because Chrome keeps them closed: its "New tab" and "New Incognito window" lead to
an activity that is not open to other apps (checked on the Lenovo: `exported=false`). Only the home screen app
and the device's digital assistant may start such a shortcut (read in the Android 17 source: the assistant's
package is made a "shortcut host"). So: nothing to design here. Booklight's own "New window" on Chrome's row
opens a new Chrome window with a new tab. It is one more thing the assistant role would give, for Alex to decide.

## 6. The selection in dark theme (visual designer)

**Today:** the selection (the pill behind the chosen row, the square in the emoji grid, the choice in a card) is
the colour scheme's secondary container at 66 % in dark theme (78 % in light). On the dark glass that is a
dense coloured slab. Alex: "feels stark on dark ... sort of transparent too and a bit less color (but not devoid)".

**Candidates**, all in `pill-dark-options.png` (left: the pill on an app's row with its strip; right: on a plain
row; the panel lies half over a white page and half over a dark terminal; colours come from this device's
wallpaper):
- 0: today.
- 1: the same colour at 42 %.
- 2: half the colour, half the neutral bright surface, at 46 %.
- 3: a pane of lighter glass: white with 45 % of the scheme's secondary in it, at 16 %.
- 4: the same with the primary, at 20 %.
Unchanged in all: the white rim of the pill (30 % in dark), the armed action's small pane inside the strip
(the panel's own darkest surface at 36 %), the red variant for an action that removes something, and light theme.

**Open questions:** which one, or exact other values; whether the rim and the strip's pane need to follow;
whether text and icons on it keep their contrast over both a light and a dark window behind the panel.

## Not changing

The panel's size, type, glass levels, shadow, rows; anything in the Booklight window (it is being re-planned
separately).
