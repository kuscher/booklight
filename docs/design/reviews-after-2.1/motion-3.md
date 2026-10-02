# Booklight: motion review, round three

1 October 2026. Read: `PanelOutline.kt`, `Panel.kt`, `Motion.kt`, `OverlayActivity.kt` (working tree). Looked at:
every folder in `polish3/` and `frame-times.txt`. Nothing in the repo was changed and no device was touched.
Measured with the round-two scripts in `r2/`.

## Verdicts

| Part | Verdict |
| --- | --- |
| The seam, opening | approved |
| The seam, closing | approved |
| Turning round | still not shown; not approved as seen, and not a reason to hold the release |
| Esc during the lap | approved |
| The lap handing over to the model's light | change: brightness and speed |
| Everything else (rounds one and two) | approved |

## 1. The seam, opening: approved

In all four recordings the frame before has nothing and the first frame has a dot 5 to 9 dp high. It then grows
on its curve; at Medium it is 57 dp at 65 ms, 63 at 93 ms, 68 at 152 ms. The glass follows its curve as in round
two (at Medium 249 dp at 178 ms, 607 dp at 256 ms). See `r3/crop-1`.

Frame times: the only late frames are the window's first one to three (the second up to 32 ms). They now fall in
the seam's first growth, where the dot holds for a frame or two longer. That is start-up cost; accepted.

## 2. The seam, closing: approved

Four times slower the seam is seen to draw in: 67, 61, 55, 48, 39, 25, 17 dp, fading as it goes, then gone. At
real speed its last frame is 53 dp at a third of its strength. Its curve is late (most of the shrinking is in
the last tenth of the time, when it is nearly faded), so what one sees is a seam that fades while it begins to
shrink. Continuous; nothing is switched off.

## 3. Turning round: not in the recording

`turn-medium-slow4` shows a whole closing (the glass folds from 4.70 s, the seam draws in from about 5.07 s,
gone at about 5.39 s) and nothing after it until the recording ends at 5.60 s. No frame shows the glass growing
again.

A likely reason, not confirmed: `onNewIntent` begins with
`if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_STAY, false)) { stay = true; return }`. A second start
that carries `stay` (as `./bl open stay` does) is "delivered to the running instance" and returns before the
branch that turns the panel round.

What is built reads right (damping 1.0, same stiffness). One thing to look for when it is recorded: whether the
glass takes over its speed. The fold's animation belongs to the effect that is cancelled when `leaving` flips,
and a cancelled `Animatable` drops its velocity; if that happens before the spring starts, the spring begins
from standstill and the glass stops dead for a frame. I have not seen this; it is a reading of the code.

To pass without coming back to me: start again without `stay` while the glass is between 30 and 70 % open, four
times slower. The width must never stand still for more than one frame, and must not shrink again once it grows.
If it does stand still: let the fold note its velocity each frame (the block of `animateTo`) and pass it to the
spring as `initialVelocity`.

It is a window of 155 ms at real speed. I do not hold the release for it.

## 4. Esc during the lap: approved

Esc came about 300 ms into the lap, with the light at full on the top right. The brightest point on the top edge
goes 253, 244, 219, 184 over the next three frames (about 35 ms), still moving, while the glass begins to fold;
then the glass's own edge passes the place. No cut.

## 5. The hand-over to the model's light: change

Measured in `refl-then-thinking` (the model's light switched on about 250 ms into the lap):

- **Brightness dips.** +170 at 4,278 ms, +31 at 4,371 ms, +118 at 4,503 ms. The lap's part falls over 160 ms
  while the model's rises over 200 ms, and the larger of the two is taken; the two cross at about a third. It is
  not a cut, but it is a wink: bright, dim, bright in 220 ms. This follows my round-two wording exactly; the
  wording was wrong.
- **Speed steps.** About 1,500 dp/s before, about 620 dp/s from the next frames on. From its fastest (1,680 to
  2,000 dp/s) it would be a step to a third.

**Change, exactly:**

1. Brightness goes one way. At the hand-over `think.snapTo(once() / THINKING_GLOW)`, then
   `think.animateTo(1f, 200 ms, linear)`. Drop `carry`. From a lap at full it goes from 1.0 down to 0.75; from a
   lap that is still coming or already dimming it rises to 0.75.
2. Speed goes one way. The first stretch of the model's light, from where the head is to the top's middle, slows
   evenly from the lap's speed v0 to the model's speed v1 (both in dp/s; D is the dp still to go):
   duration `2 * D / (v0 + v1)`, easing `CubicBezierEasing(1/3f, m0 / 3, 2/3f, 1 - m1 / 3)` with
   `m0 = 2 * v0 / (v0 + v1)` and `m1 = 2 * v1 / (v0 + v1)`. (That curve is an even change of speed: its start
   slope is m0, its end slope m1, and m0 + m1 = 2.) Every lap after it is linear, as now.

To pass without coming back to me, in the same recording: the light on the top edge over the terminal never
falls under +100 between the hand-over and the model's steady light, and the head's speed over any two
successive windows of 50 ms differs by less than a fifth.

The rest of that recording is right: the model's light goes round at about 650 dp/s, passes the top's middle
without a break, fades over about 200 ms while it goes on, and the lap does not come afterwards.

## Files

- `r3/crop-1-seam-grows-from-a-dot-and-draws-in.png`
