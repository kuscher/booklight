# Booklight: motion review, round two (the built result)

1 October 2026. Read: `Motion.kt`, `Panel.kt`, `Glass.kt`, `Field.kt`, `OverlayActivity.kt`, `PanelOutline.kt`,
`OverlayModel.kt` (working tree). Looked at: every folder in `polish2/` and `frame-times.txt`. Nothing in the
repo was changed and no device was touched. Measurements were made with the scripts in `r2/` (`edges.py`: the
glass's width and the seam's height per frame; `lap.py`: where the reflection is along the outline; `over.py`
and `ends.py`: sheets).

## Verdicts

| Part | Verdict |
| --- | --- |
| Opening: curve, landing, blur, the two end marks, the gate | approved |
| Opening: the seam | change: it is not on screen until the glass starts |
| Closing: time and shape | approved |
| Closing: the seam | change: the same fault, mirrored |
| Turning round | not shown by the recording; one change from the numbers |
| Reflection, with the visual designer's four changes | approved |
| Interrupted by a key | approved |
| Interrupted by Esc | approved from the code; the recording missed the lap |
| Thinking light | approved; two changes from reading the code |

## 1. The opening

**The glass is the curve that was specified.** Measured at Medium (time after the glass starts): 12 % at 87 ms,
56 % at 140, 85 % at 202, 95 % at 267, 99 % at 342. No rebound. Fast and Slow follow the same curve at half and
twice the time.

**The blur** is sharp until 225 ms and mostly there by 255 ms. A crescent of blurred page stands beside the
glass from about 240 to 300 ms, at most about 20 dp wide, on the white page only. That is what round one
predicted and I accept it. At Fast the blur comes in two to four frames; brisk, continuous.

**The G and the esc cap** fade in between 240 and 290 ms and are never cut.

**No flash:** the glass's brightness over the terminal is level through the landing (121 to 120 of 255).

**The seam is the fault** (`crop-1`). In all five recordings (Fast, Medium and Slow four times slower, light and
dark, and Medium at real speed) the first frame in which anything of the panel shows has the seam at 53 to
58 dp of its 68, and the glass begins in that frame. The frame before has nothing. So:

- For the first 30 / 60 / 120 ms of the arrival the screen is empty. At Slow that is 120 ms with no answer to
  the key. Round one's defence of 420 ms ("the seam shows in the first frame") does not hold as built.
- The seam's growth, the arrival's first beat, is never seen. A line four fifths of its height appears in one
  frame: a pop.
- The moment is exact, whatever the speed and the stretch: nothing of the window is on screen while `opened()`
  is 0. The time from the first screen change to the seam is 115 ms plus the wait, in every recording.

The cause is not established. Two things at window level switch exactly there: the shadow's caster outline
(`PanelOutline.caster` calls `setEmpty()` while `cast` is 0) and the window's dim. The outline is my first
suspect: try a round rect with alpha 0 in place of the empty outline. This was probably so before this work
(round one's sheets began with a seam at full height); the shorter wait makes it show more.

**Change:** the seam is on screen from its first frame and is seen to grow. Check: in the first frame with the
panel the seam is at most 15 dp high. If that cannot be done, set the wait to 0 at every speed, so that glass
and seam begin together in the first frame; do not ship 60 ms of nothing.

**Frame times** (release): 56 frames, none or one late. One run has a frame of 40 ms. Say which frame it is; if
it is the frame in which the blur turns on, it lies in the landing and I want to know.

## 2. The closing

At real speed the fold takes about 140 ms from the first movement to the seam, and the shape is right (98 %,
95 %, 87 %, 75 %, 59 %, then the seam). Approved.

**The seam is cut off** (`crop-2`). In all three real-speed closings the last frame with the panel is a seam at
full height; the next has nothing. Its drawing in (50 ms) and its fade are not seen. It is the same switch:
`opened()` reaches 0. The same fix should cure it.

**`close-medium-slow4` and `turn-medium-slow4` do not show the closing.** `OverlayActivity.close()` waits
`arrival.leaveMs` (175 ms) of real time, not stretched by the debug `slow`; the fold is stretched to 420 ms. So
the activity finishes while the glass is at a third to 60 % of its width, and the system fades the rest. For a
recording, stretch that wait by `motion.slow` (debug only).

**Blur while closing** (`crop-3`): a blurred band stands beside the glass for two or three frames (the glass is
at 87 to 69 %). Unchanged from 1.1.1; accepted.

## 3. Turning round

Not shown. In `turn-medium-slow4` the activity had finished (see above) before the second key; what follows is
a new opening from the seam, 380 ms later. I cannot approve what I have not seen.

**One change, from the numbers:** the spring for this case has damping 0.72. That overshoots by about 4 % of
the way travelled (about 7 dp a side from half open), and `landed()` turns it back inwards: the rebound that
was taken out of the opening. Use damping 1.0 at the same stiffness. No overshoot, at rest in about 210 / 420 ms.

## 4. The reflection

Measured in `refl-field-dark-real` and `-slow4` (outline 1,521 dp along the arcs):

- **Start:** born at the top's middle as an even glint (lit 24 dp behind, 31 ahead), then it stretches. No cut
  (`crop-4`).
- **Speed:** about 1,000 dp/s at 12 % of the lap, 1,500 to 2,000 through the middle, 600 at 92 %.
- **End:** it dims from 82 % along the top left and is last seen at 96 %, about 50 dp before the middle, still
  moving. No stub, nothing swallowed.
- **Shape at real speed:** lit about 160 dp behind the head and 50 ahead at its fastest (to a quarter of its
  brightness). The corners show no crease.
- **All the way round in dark theme:** brightest over the terminal (+170 of 255), +120 over the photo, +99
  along the white page. No gap.
- **Around a full list** (2,516 dp, 2.2 s): top, right side, bottom, left side, and it dims on the way back to
  the top. Complete.
- **When:** 2.4 s after the gate in the bare-field recordings; 0.77 s after the last letter in the typed one.
- **Frame times:** 249 frames, one late.

**The designer's four changes do not hurt the motion.** A tail of 140 dp is ten frames of travel at its fastest,
so every frame overlaps the one before by nine tenths: no strobing. The front at 40 dp with a tail of 36 at rest
gives the even glint at birth. Less widening is less swelling. Approved.

**For the visual designer, not a motion fault:** in light theme the light is +130 over the terminal, +45 over
the photo and +20 over the white page. Over the white page it cannot be seen, from 61 % to 85 % of the lap. Over
a window that is white all over, light theme shows almost none of it.

**The slow recordings understate the tail.** `tail()` reads the animation's speed, which the debug `slow`
divides by four: the tail there is about 62 dp, not 140. Multiply the speed by `motion.slow` (debug only).

## 5. Interruptions

- **A key during the lap:** the light fades over about 130 ms while it goes on, and does not come again in the
  2.7 s that follow. Approved.
- **Esc during the lap:** the recording does not show it. Esc came at 3.78 s, where the lap would have begun;
  no light is on the outline in any frame. The code is right (brightness × `present`, 90 ms; no new lap while
  leaving). Approved on that; one recording with Esc about 400 ms into the lap, please.
- **Esc, then the key:** no lap before or during the closing; the new opening gets its lap about 2.4 s after
  its own gate. Approved. It was a new opening, not a turn.

## 6. The thinking light

Measured: in over about 200 ms; 2.37 s a lap (about 650 dp/s); it passes the top's middle without a break in
three laps; +105 against the flourish's +170; out over 130 to 240 ms while it goes on. Approved.

Two things from the code, neither recorded:

1. **Enter on a prompt while the flourish runs cuts it in one frame.** `round()` starts a new animation of
   `run`; that cancels the lap's; `lapping` goes false; `glow()` drops the flourish's part to 0 at once, and
   the thinking light then rises from 0 over 200 ms. Change: the flourish's brightness fades (the 160 ms) while
   the thinking light rises; it must not depend on `lapping` being true.
2. **The flourish comes 0.7 s after an answer has been written,** because the answer is a change of the list
   and then all is quiet. The same light has just gone round for seconds; one more lap reads as more work.
   Change: once the thinking light has run in an opening, the flourish does not come (`came = true`).

## Round three

1. Opening at Medium and Slow, four times slower, every frame from before the first frame: the seam.
2. Closing and turning round at Medium, four times slower, with the wait stretched.
3. Esc 400 ms into the lap, real speed.
4. Enter on a prompt during the flourish, real speed (debug hook for thinking).
5. Which frame is the late one in the opening's frame times.

## Files

- `r2/crop-1-seam-appears-at-four-fifths-of-its-height.png`
- `r2/crop-2-closing-seam-cut-off-at-full-height.png`
- `r2/crop-3-closing-blur-stands-beside-the-glass.png`
- `r2/crop-4-reflection-top-middle-no-cut.png`
- `r2/edges.py`, `r2/lap.py`, `r2/over.py`, `r2/ends.py`
