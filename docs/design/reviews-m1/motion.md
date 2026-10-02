# M1 · The copy: motion review

Seat: motion designer. Branch `m1-the-copy`, read only.

What I did: stepped through the three films frame by frame (mA 285 frames, mB 579, mC 271; frame numbers
are sixtieths of a second, a missing number is a vsync with no new frame), measured the glass's lower edge,
the pill's two edges and the ink of the line in every frame around each change, and read the code that
drives each transition (`CopyLine.kt`, `Panel.kt`, `Field.kt`, `Rows.kt`, `Bodies.kt`, `Footer.kt`,
`Motion.kt`, `OverlayModel.kt`, `Picks.kt`). Scale 1.125 px per dp. A whole-frame change every 120 frames
(mA f117, f236; mB f119, f360, f480, f600; mC f120, f239) is the video's key frame, not the app.

## Findings

| # | Severity | Where | What | Fix |
| --- | --- | --- | --- | --- |
| 1 | must fix | Tab and a typed letter: `Panel.kt` 456 to 464 | The line is cut out in one frame, then the pill comes as a cut sliver | The footer's band is taken only from what the window has past row one's seat |
| 2 | must fix | Enter and Backspace on a model's row: `Rows.kt` 344, 385, 392, 442; `Picks.kt` 390 | The row's content, mark and strip jump in one frame; the copy's second line lies over row two | Uncover with the pill's spring, cross-fade 70 / 110, mark and strip on `place`, keep the Gemini slot |
| 3 | should fix | The row before its answer: `Rows.kt` 442 | The copy's text wraps again when the wider strip takes its room | A fixed room for a Stream row's strip |
| 4 | should fix | The line arrives: `CopyLine.kt` 65 | Disc, text and cap are cut by the glass's lower edge for 3 frames | The parts start 120 ms after the height |
| 5 | should fix | Tab, the chip: `Field.kt` 82, 101 | The chip flies in 112 dp and lies over the placeholder | No slide without a keyword; the placeholder waits 160 ms |
| 6 | should fix | Backspace to the line: `Field.kt` 100 to 102 | The placeholder is uncovered letter by letter for 15 frames | `using` on the hint's transition |
| 7 | should fix | Tab on six rows, the first letter | About 100 ms of the transition are never shown; the lower edge jumps 256 px | Check on the release build first; then spread the rows' first composition |
| 8 | could | The line after Backspace: `OverlayModel.offerCopy` | "just now" has become "10 s ago" | Keep the age first said |
| 9 | could | The answer lands: `Rows.kt` 446 to 452 | Two panes for 2 frames | Key the strip on its mode only |
| 10 | could | Every height change: `Panel.kt` 167 | The lower edge is 1 px off for 16 frames | Leave, unless seen on the device |
| 11 | could | A three-line answer (still 07) | One spare line in the grown row | The visual designer's call |

### 1 · must fix · The line does not fade; it is cut out, and the pill arrives as a sliver

Frames:

- mA f163 to f164, mB f159 to f160, mC f158 to f159: the line's ink goes from full (depth 152 of 255) to
  none (2) between two consecutive frames. Disc, text and cap together. No frame in any film shows the line
  at a part strength. M1-c and M1-d and design-system §13 say it fades where it stands (60 to 70 ms).
- mA f164 and mB f160 are a 140 dp panel with nothing under the field, and the G and the old placeholder
  still untouched. That is the flash of an empty panel between the line and the rows.
- The pill then shows as a sliver from 87 to 107 px (18 dp of its 56 dp) with a hard lower cut: mA f165 and
  f166 (at full strength), mB f161, mC f160. M1-d: "the pill appears on row one, in place"; a cut band is
  not that.
- mC: the letter c is drawn in f155. The line stands at full strength in f155 to f158 (4 frames, 67 ms)
  and is gone in f159. So to the eye it is neither "the same frame" nor a fade.

Cause, the certain part: `room` (Panel.kt 458) takes the footer's band (44 dp, 50 px) off the body in the
frame `rows` turns true, while the window is still 140 dp. 158 - 77 - 50 = 31 px are left. The line's text
sits at 34 to 46 px in the body and the pill spans 9 to 72 px. The pill's measured cut at 107 px is exactly
77 + 31 - 1.

Cause, the open part: the disc's upper 10 px lie inside those 31 px and are gone as well, so the 70 ms exit
of line 464 is not seen at all. I could not find why from the code, and the films are at real speed.

Fix:

1. Panel.kt 458:
   ```
   val seat = with(density) { (Metrics.pad + Metrics.row).roundToPx() }
   val band = if (rows || foot.value > 0f) footerPx.coerceAtMost((windowPx - fieldPx.roundToInt() - seat).coerceAtLeast(0)) else 0
   val room = (windowPx - fieldPx.roundToInt() - band).coerceAtLeast(0)
   ```
   At 140 dp the body keeps 72 px: the line can fade whole and the pill comes whole in the line's seat.
   From 176 dp on (one row and the footer) the band is its full 44 dp, as today.
2. Then film Tab and `./bl debug keys c` with `slow=4`. The line must be seen at falling strength for
   4 x 70 = 280 ms, uncut. If it still goes in one frame, the exit itself is not running and that is the
   next thing to find. Do not ship on fix 1 alone without this film.

### 2 · must fix · Enter on a model's row: the content jumps and lies over row two (and Backspace jumps back)

Frames, mB:

- f226 to f227, one frame: the title "In English" (17 sp, on the row's centre line) is replaced by the
  caption (14 sp, at the top) and two lines of the copy. The mark drops 19 px (17 dp). The Ask pane jumps
  42 px to the right, because the Gemini slot is taken away. Nothing fades and nothing travels.
- The pill is still 56 dp in f227 and f228 (lower edge 147 px) and reaches 92 dp at f239: 149, 154, 160,
  168, 172, 176, 181, 181, 182, 186, 187 px. So the copy's second line (163 to 181 px) stands under the
  pill, on bare glass, from f227 to about f234: 8 frames, 133 ms.
- Row two has not gone yet. "Fix spelling" is at full ink in f227 and f228 and fades over f229 to f232
  (ink depth 193, 193, 177, 68, 18, 2). In f227 to f230 the copy's second line touches it: three lines of
  text with no row between them, and two marks 44 px apart.
- f228 repeats f227: 33 ms with nothing moving, straight after the jump.
- The footer's hint flips twice in 12 frames: "tab Actions" to "Leave" at f230 and f231, back at f239 to
  f242. Both labels are drawn over each other in f230, f239, f240 and f241.
- Backspace, f428 to f429: the answer is gone and the title, the strip and the mark are back in their list
  places in one frame, inside a pill that is still 92 dp (188 px) and shrinks until f441.

What is right in the same frames: the other rows fade where they are and do not move; the row keeps its
place; the glass shrinks smoothly (322 to 237 px, f230 to f241).

Fix:

1. `Rows.kt` `RowFrame` (344): the content is uncovered by the row's height on the pill's own spring.
   `val uncovered = animateDpAsState(height, motion.place(), label = "uncover")`, and before `.clip(…)`:
   `.drawWithContent { clipRect(bottom = uncovered.value.toPx()) { drawContent() } }`. `Pill` already moves
   its lower edge on `place` when a row only grew (Rows.kt 261), so the text and the pill's edge are one
   motion and no line ever stands outside the pill.
2. `Rows.kt` `ResultRow` (392): the change between the title column and `StreamBody` is a cross-fade in
   place, 70 ms out and 110 ms in (design-system §6 row 6), keyed on `body is Body.Stream`. On the way back
   the row keeps the taller height until the 70 ms are over.
3. `Rows.kt` 385 and 442: the mark's top inset goes from 10 dp to 28 dp, the strip's from 12 dp to 30 dp,
   on `motion.place()`, not as a switch.
4. `Picks.kt` 390, `TextScope.asking`: keep the Gemini slot while the row is asked:
   `actions = listOfNotNull(Action("ask", …), r.actions.firstOrNull { it.id == "gemini" })`. The strip's
   key stays the same at Enter (no 42 px jump), and the footer keeps "tab Actions" (`Footer.kt` 89), so
   "Leave" does not flash.

### 3 · should fix · The copy wraps again in the row, two frames before the new strip shows

mB f236 to f237: line one ends "… bitte bringt die" in f227 to f236 and "… bitte bringt" from f237. "die"
jumps to line two and the whole second line shifts. The answered strip (Copy, Pin, Gemini: 176 px) takes
its room in f237 and is first seen in f239; Ask alone is 88 px. Design-system §7, rules 1 and 4 (no width
that text depends on, no reflow). Still 06 to 07 has the same change waiting ("… 1234 or" as the line end).

`GROW_AFTER_MS` in `Bodies.kt` (157, 191) treats the same cause for the row's growth only, and it is a raw
`delay(120)`, not a time from `Motion.kt` (it is not stretched by `slow=4`).

Fix: a Stream row keeps a fixed room for its strip, as a keys row does (`KEYS_ROOM`, Rows.kt 479). Rows.kt
442: for `body is Body.Stream` add `.width(STREAM_ROOM)` with `STREAM_ROOM = 200.dp` ("Copy, Pin, Gemini"
measures 156 dp in English; German and Replace need more). The text is then 412 dp wide in every state;
04-design §3.3 says 446, so the visual designer should agree to the 34 dp. With the room fixed,
`GROW_AFTER_MS` can go.

### 4 · should fix · The line's parts are cut by the glass's lower edge as it arrives

mA f101 to f103, mB f102 and f103. The glass grows from 68 to 140 dp over f98 to f111 (lower edge 89, 101,
114, 124, 133, 140, 145, 149, 152, 153, 155, 155, 156 px: smooth, no step). The three parts start in the
same frame as the growth, so they are there before their room is:

- mA f101: the text is cut through its middle; disc and cap show their upper halves only.
- mA f102: the disc is cut 17 px above its lower edge. f103: 8 px. Clear from f104.

`CopyLine.kt`'s own comment says "once the panel has made room", and M1-a puts the height first.

Fix: `CopyLine.kt` 65: `arrive.animateTo(1f, motion.fade(140 + 2 * 22, delay = 120, easing = LinearEasing))`.
The edge passes the disc's lowest place (138 px plus 13 px of rise) at f105 to f106, 7 to 8 frames after
the growth starts. The whole arrival is then about 300 ms. `TipBody` (Panel.kt 501) has the same line:
film it and give it its own delay.

### 5 · should fix · Tab: the chip flies in and lies over the placeholder

mB f161 to f172, mA f165 to f170. The chip enters about 126 px (112 dp, half its own width) to the right of
its place and slides left for 11 frames. For a typed keyword that reads as the name coming from where the
keyword stood. Here nothing stood there, and the chip is 234 dp wide. On its way it covers the old
placeholder (mB f161, f162) and the first letters of the new one, "What to do with it" (mB f165 to f168,
mA f165 to f169). M1-d says it grows where the G was.

Fix:

1. `Field.kt` 82: `slideInHorizontally(motion.place()) { if (targetState?.keywords.isNullOrEmpty()) 0 else it / 2 }`.
   The copy's chip then fades in where the G was (110 ms).
2. `Field.kt` 101: the new placeholder waits for its room:
   `fadeIn(motion.fade(140, if (model.chip != null) 160 else 60))`. At 160 ms the chip's room is within
   12 dp of its width.

### 6 · should fix · Backspace to the line: the placeholder is uncovered letter by letter

mB f504 to f519 (250 ms). "Search apps, settings and the web" is cut at its right end: "… and tl" (f505),
"… the" (f506), "… the v" (f508), "… the we" (f510), "… the wel" (f514), cut through the b (f516), whole at
f520. The hint's `AnimatedContent` (Field.kt 100 to 102) has no `using`, so its box grows from the short
hint's width to the long one's on the library's default spring, with its clip. That spring is not from
`Motion.kt`. It is 2.0's code, but M1-f is where it now shows, on every Backspace out of the copy.

Fix: add `.using(SizeTransform(clip = false) { _, _ -> motion.place() })` to that transition (or
`.using(null)`).

### 7 · should fix · Tab on a copy with six rows: the first 100 ms of the transition are not seen

- mA f164 is the first frame (line gone, field untouched). f165, one frame later, shows everything about
  100 ms on: the chip at full strength and about 45 px from its place, the G gone, the pill at full
  strength. No fade of the chip, the G or the pill is seen.
- The glass stands at 156 px in f165 and f166, f167 is missing, f168 is at 412 px, f169 the same, then 483,
  494, 502, 507, 510, 512, 513, 511. 412 is 72% of the way; `place` is 73% through at 100 ms. So the lower
  edge jumps 256 px in one step, and in f168 and f169 the glass is 412 px tall around a pill and nothing
  else.
- mC, the first letter: no frame between f155 and f159; the edge is 156 px in f160 and 314 px in f161
  (158 px in one step).
- mB, three rows, is smooth: 156, 186, 210, 236, 258, 276, 290, 301, 309, 314, 318, 320, 322 px.

The films are from the debug build, where a first composition costs several times what it does in the
release build. So check before fixing.

Fix:

1. Film the same Tab on the release APK (the real key: `adb shell input keyevent 61`), or read
   `dumpsys gfxinfo io.github.kuscher.booklight framestats` after it.
2. If the long frame is still there: `Rows.kt` `SlotRow` composes a row's content only when its stagger
   delay is up (rows 4 to 6 are not due for 66 to 110 ms anyway), so six rows are not built in one frame.
   And time `Clipboard.text` in `OverlayModel.openCopy`: two binder calls on the main thread inside the
   key's handler.

### 8 · could · The age changes when the line comes back

mB f159: "Copied just now · text". After Tab, Enter, Backspace, Backspace, f503 on: "Copied 10 s ago ·
text". ux-model §15 and 04-design §3.1: the age is said once and does not count. Fix:
`OverlayModel.offerCopy(back = true)` keeps the age first said in this opening
(`copy = offer.copy(age = firstAge)`).

### 9 · could · Two panes for two frames when the answer lands

mB f239 and f240: the Ask pane fades out at the right while the Copy pane fades in 88 px to its left.
CLAUDE.md: one highlight per level, never a fade between two places. It is 2.0's way for a prompt's row
(the `Trail` key holds the action ids, Rows.kt 446 to 452, so a second strip is composed). Keyed on
`it.mode` alone, the one strip would move its pane on `lead` and `trail`. Not M1's doing; M1 shows it on
every answer.

### 10 · could · The lower edge is 1 px off for 16 frames after each height change

mB f243 to f258 at 236 px, 237 from f259; mB f514 to f529 at 155, 156 from f530; mA f176 to f179 at 513,
511 from f184. The footer moves with it. It is `place`'s overshoot (damping 0.86) rounded to a pixel.
Leave it unless it is seen on the device.

### 11 · could · A three-line answer stands in a four-line row

Still 07, light and dark: 37 dp under the last line, 12 dp above the caption. The row grows once to 136 dp
by design, and growing once is the calmer motion. No film shows a row growing, so I did not judge it in
motion. Whether the spare line stays is the visual designer's call.

## What holds

- The opening is the field's height every time: the lower edge is at 75 px from f64 to f97 (mA). No
  140 dp slab.
- The line comes 26 to 29 frames (433 to 483 ms) after the glass is 87% wide (mA f69 to f98, mB f72 to
  f98, mC f68 to f94): the 320 ms hold plus the frame pipeline and the window. Not a defect.
- Tab, mB: the pill appears in place and never flies. Row one's mark lands where the clipboard's disc stood
  (both centred on x = 43 px). Rows cascade (mA f170 to f178). The footer fades in (mB f163 to f165).
- Down, mA f245 to f259: one pill, it travels and stretches. Lower edge 147, 154, 168, 181, 192, 201, 208,
  210 px (there at f251); upper edge 87, 90, 97, 105, 113, 121, 133, 138 … 150 (there at f259). The new
  strip comes in f249 to f251.
- The answer, mB f243 to f358: written in behind a soft head, no line laid out again, the row stays 92 dp,
  caption, mark and strip do not move. The caption rolls once (f240).
- Backspace to the line, mB f500 to f513: rows and pill fade where they stand, the glass goes 322 to 156 px
  with no step, the line's parts rise in.

## Not judged from these films

- M1-b, the words becoming known late: both copies had their words when the line came.
- Tab before the line has come; a letter while the line is still rising; a click and its pressed tint;
  Down on the line; leaving the panel with the line showing.
- An answer growing to four lines (stills only). The model's slow light while it thinks: the first word
  came 13 frames after Enter.
- The second Down: mA ends 3 frames into it (f292 to f294).
- A typed language, an instruction, `tr`, Pin armed, the private copy: stills only.
- The white reflection waiting for the line: the line stood for about 1 s before Tab in every film.
- Animations off, the dark theme, `slow=4`, the Lenovo, the release build.
- mC f180 to f205 (the letter h) is 2.0's list changing under the selection, not M1.

## Verdict

approved with the must-fixes
