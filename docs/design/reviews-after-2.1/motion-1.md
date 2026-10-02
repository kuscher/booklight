# Booklight: motion review of the opening at Medium and the reflection

1 October 2026. Read: `PLAN.md`, `opening-curves.md`, `Motion.kt`, `Panel.kt`, `Glass.kt`, `OverlayActivity.kt`
(working tree), design-system §4, §6, §7, §11. Looked at: the three opening sheets and the four reflection
sheets. Nothing in the repo was changed and no device was touched. Numbers come from `curves.py` in this folder
(`python3 curves.py`).

Verdict in one line each:

- **Opening at Medium:** candidate A's curve is approved. Four changes around it: the wait, the blur's window,
  the two end marks, and the closing's time.
- **Reflection:** not approved as built. It is cut off at the top middle at both ends of its lap, and it is too
  fast. The timing rule in code is not the rule in the plan.

## 1. The opening

### What the frames show

**Today's spring at Medium lands and then shrinks.** Measured from the sheet (right edge of the glass): it
reaches 99 % and then goes back to 96 % and takes about 240 ms to creep out again. That is 14 dp on each side,
with the window's blur standing uncovered beside it the whole time (`crop-2`). This is the fold-back of the
overshoot (`landed()`), and it is the "end animation" people get today. It is a kink in speed and a band of
blur. The spring should go.

**Candidate B jumps in the middle.** Measured: 21 % to 70 % in two 20 ms steps. Computed: 38 dp per side per
frame at 120 Hz, and under 10 % open for the first 100 ms. Rejected.

**Candidate A has the right shape.** Under 10 % for 82 ms, 19 % to 71 % in 60 ms, then 160 ms for the last
15 %. Its rise from 10 % to 90 % takes 138 ms; today's Medium spring takes 139 ms. So it is today's Medium,
redistributed: a slower first step, a quicker middle, a long landing, and no rebound. Its fastest frame is
32 dp per side, less than the 42 dp of the Fast spring that everybody has had until now.

**A's fault is the blur, not the curve** (`crop-1`). The blur is on from 50 % open, and the glass then spends a
long time between 70 % and 97 %. From 120 to 220 ms on the sheet a blurred, round-cornered ghost of the final
panel stands beside the glass. Computed: when the blur is half there the band is 89 dp wide on each side, and
it stays wider than 6 dp for 125 ms. It also spoils what Alex asked for: the eye sees where the glass will end
before the glass gets there.

**The two end marks are sliced** (`crop-4`). With the slow end the edge passes slowly over the G and the esc
cap: half a G at 180 ms, a cut esc at 180 and 200 ms.

**The sheets do not share a zero.** Fitting the measured widths to the computed curves, 0 ms on the sheets is
about 140 ms (A), 170 ms (B) and 175 ms (spring) after the seam began. The seam's own frames are not on them.
On a white page the seam is a white outline on white, so I cannot say from this material how the first 100 ms
look.

### Specification (times at Fast / Medium / Slow)

| Part | Verdict | Numbers |
| --- | --- | --- |
| Seam | approve as built | 80 / 160 / 320 ms, `cubic-bezier(0.2, 0, 0, 1)` |
| Wait before the glass | change | 30 / 60 / 120 ms (was 50 / 100 / 200) |
| Glass | approve A | tween, `cubic-bezier(0.55, 0, 0.1, 1)`, 180 / 360 / 720 ms |
| Landing | change | no overshoot, no fold-back; the spring leaves the opening |
| Contents | approve as built | alpha = smooth(0.35, 0.85, open) |
| G mark and esc cap | change | their own alpha = smooth(0.92, 1.0, open) |
| Blur | change | smooth(0.88, 1.0, open) (was 0.5 to 1.0) |
| Dim, shadow | approve as built | linear in open |
| Under the field | approve as built | at 85 % open |
| Total | | 210 / 420 / 840 ms from the key to rest |

Reasons:

- **Wait 30.** A spring starts at full speed, so it needed 50 ms for the seam to be seen. A starts slowly: its
  own first 60 ms are that pause. With 60 ms at Medium the seam is 94 % grown when the glass is 18 dp wide, so
  the seam still reads as its own beat. It brings the glass to 10 % at 142 ms after the key (today 132; as
  built 182).
- **No overshoot.** The glass cannot pass its final edge, so an overshoot can only be drawn as a bounce inwards.
  The settle is the curve's own end: 51 dp per side over 160 ms, from 8 dp a frame down to nothing.
- **Blur from 88 %.** The band is then 21 dp when the blur is half there, and wider than 6 dp for about 50 ms
  (as built: 89 dp, 125 ms). The sheet's frames at 97 % (11 dp of band at full blur) are where I stop seeing it
  in a still, so this is at the edge of visible. The blur now belongs to the landing: the pane arrives clear and
  frosts over in its last 80 ms. The cost is that the placeholder stands over a sharp page for about 100 ms
  longer than today; it is in motion and the eye is on the edges.
- **Contents stay early** so that a letter typed at once shows as soon as the glass uncovers it.
- **Gate at 85 %** is 258 ms after the key at Medium; today it is 257.

### Is 420 ms too slow for a launcher?

No. What makes a launcher feel slow is a late answer to the key or a late echo of the first letter. The seam
shows in the first frame, the field has the keys from the start, the field is readable at 258 ms (the same as
today's Medium), and the glass is 95 % there at 314 ms. The last 100 ms move 18 dp per side. Today's Medium is
only at rest after about 560 ms, because of the rebound.

### Closing

- **Shape: approve as built**, fold on `cubic-bezier(0.45, 0, 0.4, 1)`. It is already slow, fast, soft. Do not
  mirror A exactly: A backwards is `cubic-bezier(0.9, 0, 0.45, 1)`, which stays above 96 % for 100 ms. Esc
  would look ignored.
- **Time: change.** Fast and Medium both close in 155 ms (fold 105, seam from 75 for 80, last 40 fading); Slow
  310. Reason: `OverlayActivity.close()` keeps the window, and so the keyboard, for `leaveMs`. With Medium as
  the default that becomes 330 ms for everybody, twice today's. Keys typed for the app underneath in that time
  go to a panel that is leaving.
- **Blur and contents on the way out: approve as built** (0.75 to 1.0, 0.55 to 0.95).
- **Turning round** (the key again while it closes): a tween starts from standstill, so the glass stops dead and
  creeps out again. Use the spring for this one case (`motion.open`, clamped at the edge): it takes over the
  speed. From the seam it is always A.

## 2. The reflection

### What the frames show

**It is cut at the top middle, twice** (`crop-3`). The shader measures distance from the head along a line that
starts and ends at the top middle, not around a ring. Leaving (60 to 240 ms): the tail and the light on the
glass under it end in a hard vertical edge at the middle. Returning (840 to 960 ms): the head runs into the
middle at about 1,570 dp/s and 90 % brightness and is swallowed there; a stub stays and dims. These are the
two places Alex named ("start in the top middle ... run around once"), and they are the two places it pops.

**It is fast.** Computed cruise 2,790 dp/s (23 dp a frame); the head is back at the middle after 0.9 s of the
1.5 s. The remaining 0.6 s, where the easing slows down, is spent where nothing shows. A cap is crossed in
38 ms, so each end lights up as a whole (`crop-5`, 300 and 600 ms). Around a full list (2,600 dp) the same
1.5 s means 4,600 dp/s.

**It goes out over a light backdrop.** In dark theme the white line along an edge that borders a white page is
white on white (`crop-5`, 480 and 540 ms): the lap is seen as two flashes with a gap. In light theme it is
mostly the cap that shows. That is for the visual designer (more of it on the glass side of the line), but it
decides whether "once around" is what one sees.

**The timing in code is not the plan's rule.** `waited` counts quiet periods that ran to their end, not time.
After five seconds of typing and a pause, the reflection waits another 2.4 s, not 0.7 s. A key at 1.0 s moves
it to 2.7 s instead of leaving it at 2.4 s. Arrow keys and Tab do not count as activity.

Nothing stutters in the 60 ms sampling; a finer recording has to show that.

### Specification

| Part | Verdict | Numbers |
| --- | --- | --- |
| When | approve the values, change the rule | 2.4 s after the gate and 0.7 s after the last key of any kind or change of the list, both as real time; never once the panel is leaving |
| How far | change | exactly one lap, 0 to 1.0 (was 1.18), clockwise from the top middle |
| How long | change | same speed: 1 ms per dp of outline when it starts (1.6 s around the field), at most 2.2 s |
| Easing | change | `cubic-bezier(0.3, 0, 0.5, 1)`: cruise 1,680 dp/s, 14 dp a frame, a cap in 63 ms |
| Appearing | change | brightness smooth(0, 0.05, lap); the shader measures around the ring |
| Leaving | change | brightness 1 - smooth(0.82, 1.0, lap): it dims along the top left and is gone before it stops |
| Front | approve as built | 36 dp |
| Tail | change | grows with speed: 36 dp at rest, 230 dp at cruise |
| A key or a change of the list during the lap | change | it fades in 160 ms while it keeps moving, and does not come again |
| Closing | approve as built | 90 ms; but no snap back to full if the panel turns round |
| Thinking | same light, other motion | see below |

Reasons:

- **Ring distance** is the whole fix for "more complete": `t = mod(run - s + around / 2, around) - around / 2`.
  Half the field's outline is 788 dp, more than three tail lengths, so nothing shows at the far side.
- **Tail by speed.** At the start it is a small even glint at the top middle that stretches as it gathers
  speed; at the end it gathers itself again. That is what lets it be born and end at one point without a cut.
- **Same speed, not same time.** Speed is what the eye reads. At a fixed 1.5 s a full list runs 65 % faster
  than the field.
- **It ends dimming, not parking.** At 95 % of the lap it is at 19 % brightness and still moving at 540 dp/s.
- **Any key fades it.** This covers the panel growing or shrinking under it (its place on the outline would
  slide) and keeps it from pulling the eye once the user acts again.

### Thinking

Yes, the same white light: the pane has one rim. It must not be the same motion, or a flourish reads as
"working".

- Continuous laps, linear, 1.5 ms per dp (2.4 s around the field), brightness 0.75, in over 200 ms. With the
  ring it passes the top middle without a break; as built it fades out and in there every lap.
- The tail follows the same rule, so it is about 110 dp: a short slow glint against the flourish's long streak.
- First word: it fades in 240 ms while it keeps moving. As built it sprints the rest of the lap (up to 1,700 dp
  in 640 ms) at the moment the eye should go to the answer.

### Does it pop, stutter or pull the eye?

As built: it pops twice at the top middle; no stutter seen; it does not start while typing, but it carries on
at full brightness if typing resumes. With the changes above: none of the three.

## 3. Other things

- `Motion.kt` says nothing is longer than a third of a second and that all motion is in that file. The
  opening's curve and the reflection's values are constants in `Panel.kt`. Move them there as named specs and
  correct the sentence.
- The design system (§4, §11) still describes the spring and the 50 to 100 % blur.

## 4. Second round: what I want to see

All on the built result, over the white page and the dark terminal, with the sheets starting at the key event.

1. Opening at Fast, Medium and Slow, recorded four times slower, **every frame** (8.3 ms), light and dark. I
   check: the seam on white, the band beside the glass, the frosting, the G and esc.
2. Opening at Medium at **real speed** with a frame-time log (or 120 fps): no frame lost while the blur comes,
   and none when rows arrive at the gate (`./bl debug keys chr` at once).
3. Closing at Medium, real speed and four times slower; and the key again in the middle of it.
4. Reflection, bare field and a full list, dark and light, over white, over dark and over a mid grey: every
   frame at four times slower, plus real speed. A crop of the top middle at the start and at the end.
5. Reflection interrupted: a key during the lap; Esc during the lap; Esc at 2.3 s and the key again.
6. Thinking: the start, three laps, the first word.

## Files here

- `curves.py`: every number above.
- `crop-1-A-blur-band-left-half-100-to-240ms.png`
- `crop-2-spring-rebound-left-end-100-to-320ms.png`
- `crop-3-reflection-top-middle-hard-cut-60-240-and-720-1020ms.png`
- `crop-4-A-G-and-esc-sliced-160-to-220ms.png`
- `crop-5-reflection-cap-flash-300-and-600ms-dark-over-white-480-540ms.png`
