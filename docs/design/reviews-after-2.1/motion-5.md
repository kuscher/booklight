# Booklight: motion review, round five (the blur belongs to the glass)

1 October 2026. Read: `OverlayActivity.kt` (`frameGlass`, `present`), `Panel.kt`, `PanelOutline.kt` (working
tree, uncommitted). Looked at: every sheet named in the request, and the frames of `v-real`, `v-slow-light`,
`v-turn`, `v-rows` and `final-real`. No code was changed and no device was touched. Measured with `blur.py`
(in the session's scratch folder, `polish/motion-review/r5/`; per frame: the glass's width, how much of the
window is there, how sharp the terminal's text still is behind the glass).

## Verdict: APPROVED

The blur on the glass from its first frame is right, at all three speeds, and so is keeping it through the
fold. No value in the motion needs to change. The one measurement I asked for (finding 6: how many frames are
drawn) has been made and passes; nothing is outstanding.

| Question | Verdict |
| --- | --- |
| 1. Full blur from the seam on | approved as built; no shape of its own |
| 2. The blur stays through the fold | approved as built |
| 3. `shown()`, `ends()`, the gate | keep 0.35 to 0.85, 0.92 to 1, 0.85 |
| 4. Fast under the system's fade | leave as it is |
| 5. Anything else | nothing wrong in the frames |
| 6. Frames drawn during the opening | measured in a release build: passes; closed |

## 1. Full blur from the seam on: approved, no shape

**What I saw.** `zoom-slow.png`: the glass is frosted from 27 px wide, its edges are crisp, nothing is blurred
outside it and its round ends are the blur's ends. `v-slow-light.png` and `v-slow-dark.png`: frosted at every
width, and no band beside the glass at the landing. `v-real` at Medium: once the window is fully there, the
terminal's text behind the glass has none of its contrast left (0.00 to 0.02 of the bare backdrop's; the veil
alone would leave about 0.55).

**Why no shape.** The radius was only ever animated because the blur was the whole window's. Now that it is
the glass's, it is a property of the material, like the veil and the outline: one owner, one value, and it
cannot drift out of step with the width. Behind a seam 6 dp wide a radius of 22 dp and a radius of 0 look the
same, so easing it over the seam would change nothing one can see. And the easing Alex asked for is already
there: the system's fade brings the blur in with the window (finding 4).

**Exact value:** radius full (22 dp at Balanced) from the first frame, as built.

## 2. The blur stays through the fold: approved

**What I saw.** `v-close-light.png`, `v-close-dark.png`, `v-real-medium-close.png`, `v-turn.png`: the glass is
frosted down to the seam and on the way back out in the turn. The band of blurred page that stood beside the
folding glass for two or three frames, which I accepted in rounds two to four, is gone.

**Exact value:** radius × `presence`, as built. The last 40 ms shrink it behind a seam 6 dp wide; not visible.

## 3. `shown()`, `ends()`, the gate: keep

**What I saw.** `v-slow-light.png`: the placeholder comes up between about 55 % and 87 % of the width, now over
frosted glass instead of over the page's sharp text (round two's one cost of the late blur is gone). G and esc
come at 95 %, whole. `v-rows.png`: rows arrive from 85 % without a step.

The landing is still marked: by the glass slowing, by G and esc, and by the shadow reaching its strength. It
does not need the blur as a signal.

**Exact values:** contents smooth(0.35, 0.85), ends smooth(0.92, 1), gate 0.85. No change.

## 4. Fast under the system's fade: leave

**What I measured** in `v-real`, the same in all three openings: the window fades in evenly over about 142 ms
from its first visible frame (the outline at the panel's middle goes from +16 to +211 in 17 frames).

- **Medium:** the glass is at 11 % when the fade ends. Everything after that is at full strength.
- **Slow:** the glass has not started when the fade ends.
- **Fast:** the glass is at 11 % with the window 45 % there, at 51 % with 62 %, at 85 % with 73 %, at 99 % with
  84 %; the fade ends 25 ms after the glass has landed. The text behind the glass loses its contrast in a
  straight line over those frames: 0.48, 0.41, 0.36, 0.31, 0.26, 0.22, 0.17, 0.12, 0.09, 0.04, 0.

**Why leave it.** At Fast the pane comes up out of nothing while it spreads, and its frost, its veil and its
outline come up on the one ramp. That is continuous: no frame in which one of them arrives alone. Before, the
blur at Fast came in two to four frames at the very end. To open after the fade the wait would have to go from
30 to about 140 ms, and Fast would take 320 ms instead of 210: no longer Fast. The default is Medium, where
the question does not arise.

**The seam under the same fade** (`v-real`, Medium): it is on screen from the first frame at 8 % strength and
7 dp, and grows on its curve while it brightens (29 dp at 18 %, 54 dp at 42 %, 65 dp at 74 %). It reads as a
line that grows as it appears. Nothing pops; nothing to change.

## 5. Other things in the frames

- **Opening turned off** (`v-off-open.png`): the page behind goes from sharp to blurred with the panel's own
  fade. Right.
- **Solid** (`v-solid-open.png`): unchanged.
- **The first three frames of growth,** which the request leaves out of "blurred edge to edge": at Medium the
  text behind reads 0.08 and 0.08 with the window 90 and 94 % there, then 0.04 and falling. That is the last
  of the system's fade, not a blur that is late.
- I found no frame with a blurred strip outside the glass or a sharp strip inside it, in any sheet.

## 6. How many frames are drawn: measured, closed

*The question as first written, then the result.*

**What I saw.** In `v-real` all five openings are recorded at one frame every 8.3 ms for the first 225 to
240 ms after the first change, and at one every 16.7 ms from then on. At Medium that is the glass from about
19 % of its width to the end; at Slow it is the whole opening. In `v-turn` the
opening is at 8.3 ms throughout (about 55 frames). In the recordings of rounds three and four (main) the step
to 17 to 28 ms came later, at about 310 to 330 ms, which is where the old blur came on.

**What I do not know.** Whether the screen itself shows every second frame there, or only the recording. The
recordings disagree with each other, and I cannot tell from frames alone. If it is the screen, the fast part of
the glass moves 64 dp a side per frame instead of 32, and the old design did not pay that until the last 15 %.

**The check.** A release build, opened the way a key opens it, Medium: the times of the frames the app drew
(`dumpsys gfxinfo … framestats`) and the display's rate while it opens. It passes if the glass from 10 % to
100 % (278 ms) is drawn in at least 30 frames.

**If it fails:** ask for the high rate for as long as the arrival, the fold or the turn runs (the window's
preferred refresh rate, or a frame rate request on the root view; neither tried here), and record again. If
the screen cannot hold 120 Hz with the blur on, bring it back to me with the numbers: that is a choice between
the blur from the first frame and the frame rate, and it is Alex's to make.

**Result** (run by the coordinator; I read the raw files `gfx-1.txt` to `gfx-3.txt` and ran `gfx.py` on them).
The release build of the working tree on the Lenovo, Medium, the panel started three times by a plain start of
its activity, `dumpsys gfxinfo … framestats` after each. Gaps between the frames' intended vsync:

| Run | Frames | Gaps |
| --- | --- | --- |
| 1 | 58 | 8.3, 33.3, then 51 of 8.3 to 8.6 ms, one of 16.7, one of 8.3; then idle |
| 2 | 57 | 12.3, 25.0, then 51 of 8.3 ms; then idle |
| 3 | 57 | 8.3, 25.0, then 51 of 8.3 ms; then idle |

- From about 40 ms after the first frame to the end of the opening, every frame follows the one before by
  8.3 ms. The glass from 10 % to 100 % is 33 frames in each run; the test asked for 30.
- The only frames that finish late are the window's first two (19 to 31 ms from intended vsync to done), as on
  main. Every other frame is done within 9 ms.
- The display reads 120 Hz right after one run and 60 Hz after the other two: it falls to 60 Hz when the panel
  sits idle, and it gives the app a vsync every 8.3 ms while the opening is drawn.

**Final word.** The test passes. The steps of 16.7 ms in the debug recordings were the recording or the debug
build, not what the release build draws. The blur from the first frame costs no frames. Finding 6 is closed
and the verdict stands without a condition: APPROVED.

What this does not show, and I do not ask for: what the glass of the screen physically shows (the numbers are
the app's frames, on time and at the display's rate, not a camera), the HP, and a start by the real key.

## For the record

`device-findings.md` now records that the blur's region is the root view's rectangle and travels with the drawn
frame. The design system's §4 still describes the blur as the whole window's, held back until the glass is more
than half open; it wants the same correction.
