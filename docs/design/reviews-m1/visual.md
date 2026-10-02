# M1 · The copy: visual review

Seat: visual designer. Branch `m1-the-copy`. Read only: stills measured in pixels (1.125 px per dp), films
stepped frame by frame. Work files are in `reviews-m1/work/`.

## 1. What holds (measured)

| Asked | Measured | Verdict |
| --- | --- | --- |
| The line's disc on the icon column | Pixels 23 to 63 wide, 97 to 137 high, in light and dark: centre (43.5, 117.5) px. The rows' discs in 02 are on the same pixels (23 to 63). That is 38.7 dp, 0.7 dp right of 38, because the 20 dp margin is 22.5 px and rounds up; the line and the rows round the same way | Holds |
| The line's text on the titles' edge | First glyph at px 82 to 83 in 01; titles in 02 ("example.com", "Fix spelling") at 82 to 83; the answer's caption and text in 06, 07, 11, 12, 13 at 82 to 83. That is x = 72 dp plus the letter's own side bearing | Holds |
| The `tab` cap under `esc` | Right edge of both at px 787 = 699.6 dp, light and dark. The line's text and the cap's text stand on one baseline (y = 122 px) | Holds (but see F9) |
| Panel heights | 01: 158 px = 140 dp. 02: 513 = 456 (68 + 8 + 6 x 56 + 8 + 36). 05, 14: 198 = 176. 06: 239 = 212. 07: 288 = 256. 10: 324 = 288. 13: 302 = 268 | As the design system says |
| Row pitch | 63 px = 56 dp between all six rows of 02 | Holds |
| The answer row | Spark at (45, 138) px in 06, 07, 11, 12, 13, both themes. Caption top at y = 103 to 104. Gemini's arrow at px 764 to 777 in every strip. Text measure beside "Copy ⏎ · Pin · Gemini": 72 to 518 dp = 446 dp, the number in 04-design §3.3 | Holds |
| 06 against 07, 11 against 12 | Mark, caption, text start, arrow: no pixel moves. The pane changes width, nothing else | Holds |
| 01 against 02 | Row one's disc and title stand where the line's disc and text stood | Holds in the stills (not in the film: F4) |
| Against a typed list (mC 294) | Same disc column, same title edge, kind labels end at the same px (785 to 786) | Consistent |
| Row marks | Clipboard, calendar, phone: all 17 px of ink in the 20 dp box. Same size | Holds (weight: F10) |
| 07: three lines in the 136 dp row | 22 dp spare under the text. It is what the design system asks ("grown once to four lines") | Leave |

## 2. Findings

| # | Severity | Where | What is wrong | Fix |
| --- | --- | --- | --- | --- |
| F1 | must fix | Light 11 and 12 | The answer has two lines and the row is 136 dp (the pill runs from 87 to 238 px): 44 dp of empty pill under the text. The same text in dark 11 and 12, and in film mB frames 364 to 424, sits in a 92 dp row. So the height depends on the run. The row grows when a third line exists for 120 ms (`Bodies.kt`, `Written`, `GROW_AFTER_MS`) and never comes back (`OverlayModel.grow`). The text's width is whatever the strip leaves (`StreamBody` is `weight(1f)`), and the strip changes: Ask to Copy, and every Tab. With "Open in Gemini" armed the text is about 60 dp narrower than with "Copy" armed, so a full two-line answer gets a third line, grows, and stays tall after Tab goes back (from the code, not captured) | (a) One width for the question and the answer for as long as the row is a `Body.Stream`: reserve the strip's room once, at its widest armed state (design system §3a: "the strip is measured once per row"), in `Rows.kt` where the strip's `Box` is placed. (b) Decide `tall` from that layout only. (c) In `OverlayModel`, beside `grow`, set `tall = false` when the answer is finished and has two lines or fewer, so no path ends at 136 dp over two lines |
| F2 | must fix | Film mA, rows 4 to 6 | The three rows say "Gemini" from frame 170 to 202 and "On this device" from frame 203: one frame, no fade, 0.55 s after the list was drawn. Cause: `OverlayModel.entered` starts `onDevice.check()` at Tab, after `search()` has built the rows; `TextScope.here` reads the state as it is then. On a German copy row one is such a row: its strip ("Ask Gemini" against "Ask ⏎ · Gemini") and what Enter does would change under the selection | Start `check()` when the line is offered (`offerCopy` returns true) and when text is handed over; the line stands at least 320 ms before any Tab. In `TextScope.rows`, wait for a state other than `UNKNOWN` inside the 150 ms `read` already allows (`FIND_MS`). As a net, the kind label in `Rows.kt` cross-fades (`fade` 70 out, 110 in) when a row keeps its id and changes its label |
| F3 | must fix | Film mA 165 to 166, mB 161 to 162, mC 160 | The pill arrives as a flat-cut band: in mA 165 it runs from y = 87 to 108 px and is cut straight across at 96 dp, while the glass is still 140 dp tall. Cause: `Panel.kt` line 458 takes the footer's band (36 + 8 dp) out of the body's room as soon as there are rows, before the window has grown: 140 - 68 - 44 = 28 dp of room | Take the footer's band only from what the window has beyond row one: `room = max(windowPx - fieldPx - footerPx, min(windowPx - fieldPx, 8 + 56 + 8 dp))`. The footer's alpha is 0 in those frames, so nothing overlaps |
| F4 | should fix | Film mA 163 to 170, mB 159 to 166, mC 155 to 159 | The line never fades. Its darkest pixel is 72 in mC 155 to 158 and 235 in 159: a cut. On Tab the disc's seat is empty for 3 frames in mB (160 to 162) and 6 in mA (164 to 169); then row one's disc rises 12 px into it. In mC the letter "c" is in the field from frame 155 and the line stands at full strength for 4 more frames. Design system §13 says the line fades (70 ms) and "row one's disc lands where the line's disc stood" | Find why the body's `fadeOut(motion.fade(70))` in `Panel.kt` holds and then cuts. On Tab from the line, row one's mark is drawn at full alpha and without the 12 dp rise from its first frame (`SlotRow` in `Rows.kt`, row 0 only), so the disc never leaves its seat |
| F5 | should fix | Film mB 226 to 231 (Enter on "In English"), 428 to 431 (Backspace) | The row's content jumps to the 92 dp layout in one frame while the pill is still 56 dp: in 227 to 230 the second line of the German text lies outside the pill, 21 px above "Fix spelling"; the spark jumps down 18 dp to 43 px above the next row's spark; the "Ask ⏎" pane jumps 41 px right and Gemini's arrow is cut. Backwards the same: the title sits at the top of a 92 dp pill for 3 frames | `Rows.kt`: `RowFrame` takes `animateDpAsState(height, motion.place())` and clips to it, so line two is uncovered by the edge the pill's lower edge rides. The two pinned paddings (lines 385 and 442) start at the 56 dp row's values (10 dp and 12 dp) and travel to 28 and 30 on `place`. The strip keeps Gemini's slot while asking |
| F6 | should fix | Film mB 236 to 241 | The question re-wraps under the pill before the answer comes: in 237 "die" drops from the end of line one to the start of line two while "Ask ⏎" still stands, and stays so for 5 frames. The room for the wider strip is taken a few frames before the strip shows | Solved by F1 (a): one width from the first frame of the question |
| F7 | should fix | Film mB 503 to 517 (Backspace to the line) | The placeholder "Search apps, settings and the web" is cut through its letters by a width that grows for 13 frames: "and tl" (505), "the w" (509), "the wel" (514). `Field.kt` line 100: the hint's `AnimatedContent` has the default size transform, which clips | `.using(SizeTransform(clip = false) { _, _ -> motion.place() })` on that transition, as the chip beside it has |
| F8 | should fix | Light 01 to 04, over the dark terminal | In light the glass over a dark window is mid-grey (about 120). Second ink on it measures 2.4 : 1 ("On this device", footer), full ink 3.5 : 1. The line's first part ("Copied 20 s ago ·") is second ink in light, so over a dark window it is the weakest text of a line that is said nowhere else. Dark already has the whole line at full ink for the mirrored reason (3.8 : 1 measured over the white page) | `CopyLine.kt`: `color = ink` for the first span in both themes. If the two parts should still differ, give what it holds weight 600, not a second alpha |
| F9 | could | 01, both themes | The two caps make a column, and the column is ragged at its left: `esc` is 40 px wide (746.5 to 787), `tab` 39 px (747.5 to 787) | `Keycap` gets a minimum width of 36 dp for these two (the field's `esc`, the line's `tab`) |
| F10 | could | 02 to 04, row three | The phone mark is the outlined handset; the calendar above it, the envelope (the fourth found thing) and the sparks below are filled. It reads one weight lighter | `Icons.kt`, "phone": the filled handset (Material `call`, filled) |
| F11 | could | 02, row one's strip | In the clean-link mark the chain is centred on y = 14 of 24, so it sits 1.5 dp below the centre line of "Open ⏎" and of the copy mark in 04 (118.7 px against 117) | `Icons.kt`, "clean": chain from y = 8 to 18, the spark from 0.5 to 6.5 |
| F12 | could | 02, 10: the chip | "Dinner with Anna on Friday …": a space before the ellipsis, because the 27th character is a space | `Picks.kt`, `TextScope.name`: `t.take(27).trimEnd() + "…"` |
| F13 | could | Film mA 165 to 169, mB 161 to 166 | The copy's chip slides in from half its own width (`Field.kt`, `slideInHorizontally { it / 2 }`): about 112 px for this chip, across the placeholder, whose letters show through it for 5 frames | Cap the slide: `{ minOf(it / 2, 24.dp.roundToPx()) }` |

## 3. Contrast, as measured (ink against the glass behind it)

| Text | Light, over the white page | Light, over the terminal | Dark, over the white page | Dark, over the terminal |
| --- | --- | --- | --- | --- |
| The line, first part | 6.4 | about 2.5 (F8) | 3.8 | 14 |
| The line, what it holds | 6.2 | 3.5 | 4.8 | 14.5 |
| Row titles | 11.4 | 3.5 | 3.8 | over 10 |
| "On this device", kind labels | 5.5 (over colour) | 2.4 | 3.9 (over colour) | 10.1 |
| Placeholder (third ink, 24 sp) | 3.7 | 2.1 | 2.5 | 5.9 |
| Footer labels | not in a capture | 2.4 | not in a capture | 10.1 |
| Answer text on the pill | not in a capture | 7.2 | not in a capture | 11.8 |

The plan's device check "the line in dark over a white window" passes as far as flat glass allows: 3.8 : 1 at
full ink, the same as a row title there. The weak side is the other one, light over a dark window, and it is
the glass's (veil 0.42), not M1's: only F8 is M1's to fix.

## 4. Not reviewed

No capture has the German interface ("Kopiert vor 20 s · ein Link und ein Datum", "Auf diesem Gerät",
"Hinzufügen …", "In Gemini öffnen"). The strip with "Open in Gemini" armed is in no capture; F1 needs it.

## 5. Verdict

approved with the must-fixes
