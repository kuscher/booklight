# Booklight: motion review, round four

1 October 2026. Read: `Panel.kt` (the effect on `model.thinking`, `foldSpeed`), `Motion.kt` (`open`). Looked at:
the four folders in `polish4/`, the two lap listings and the turn sheets. Nothing in the repo was changed and no
device was touched. Measured with the round-two scripts in `r2/`.

## Verdicts

| Part | Verdict |
| --- | --- |
| The lap handing over to the model's light | approved |
| Turning round, with the stiffer spring | approved |
| The whole motion | approved |

## 1. The hand-over: approved

Both recordings, the model's light switched on about 390 ms into the lap (the head at about 386 dp, on the
right cap).

- **Brightness goes one way.** Compared with round two's undisturbed lap at the same places: the cap +155
  (undisturbed +157), then +145, +139, +132 over the next 100 ms, +88 to +100 over the photo at the lower right
  (undisturbed +116), and +112 settling to +106 along the bottom over the terminal (undisturbed +170). That is
  full brightness gliding to three quarters in about 200 ms. No dip: the +121 readings earlier are the head over
  the photo on the top right, where the undisturbed lap reads +120 too.
- **Speed has no step.** One curve of even slowing fits the head's position from 400 to 1,450 ms in both
  recordings: 1,466 to 650 dp/s and 1,469 to 618 dp/s, with a residual of about 20 dp, which is my tracker's
  noise. After that it runs at about 650 dp/s.
- My test of a fifth per 50 ms cannot be read from a debug build that drops frames. The fit above answers the
  same question, and the construction gives 34 dp/s per 50 ms.

## 2. The turn: approved

`turn-medium-slow4` (key with the glass at about 45 %) and `turn-early-slow4` (about 80 %), times in real time:

- **Medium:** the glass goes on from 46 % to 32 % in about 15 ms, turns, and is at 50 % after 35 ms, 90 % after
  90 ms and 98 % after 121 ms. At the turning point the width moves by less than 4 dp over about 9 ms: one
  frame. It never shrinks once it grows, and lands without overshoot.
- **Early:** on from 81 % to 73 %, within 4 dp for about 15 ms at the turning point (two frames, the fold being
  slower that early), then 90 % after 50 ms and 98 % after 88 ms.
- The contents and the blur come back on the way out. A band of blur stands beside the glass for about three
  frames near 80 %, as in the closing; accepted there, accepted here.

**The stiffer spring is right.** The spring has to answer the fold, and the fold is the same 155 ms at Fast and
at Medium; so its stiffness goes with the leaving (1000, and 250 at Slow), not with the opening. At 250 the
panel nearly shut after the key, which reads as the key being ignored. A turn is a correction, not an arrival:
it may be quicker than the opening.

## 3. The whole

Opening, leaving, turn, reflection, the model's light: approved as built. Nothing is outstanding from rounds
one to three.

For the record, two things outside motion that I raised and that are other people's calls: in light theme the
reflection cannot be seen over a white window (visual designer), and the design system's §4 and §11 still
describe the old spring and blur window.

## Files

- `r4/crop-1-turn-medium-every-third-frame.png`
