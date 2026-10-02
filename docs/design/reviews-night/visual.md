# Visual review: the copy (re-check), flights, your usual

Seat: visual designer. 2 October 2026, branch `m1-the-copy`.

## What this rests on

- **Stills** `n2-light/` and `n2-dark/` (taken 01:46 to 01:48). Every number below is measured in them; px divided by 1.5 is dp.
- **Films, first takes** (`zM`, `zS`, `zF`, `fA`, `mA`, `mB`, `mS` as they stood until about 02:15). While I worked the session folder changed: `zM`, `zF`, `mA`, `mB` were removed, and `fA`, `zS` (02:19) and `mS` (02:20, again 02:24) were taken again. Frame numbers marked "take 1" are from films that no longer exist; my contact sheets of them are in `reviews-night/work/`. Frames marked "take 2" are from the folders as they stood at 02:25.
- **Code** read between 01:50 and 02:25. Two commits landed in that time (`ef42b45` 02:15, `e12c315` 02:19) and `Field.kt` has an uncommitted change. Line numbers are those of `e12c315`.
- Contrast is WCAG contrast of the text's darkest (lightest) ink against the median of the ground under it, read off the stills.

## Findings

| # | Severity | Feature | Where | What is wrong | Fix |
| --- | --- | --- | --- | --- | --- |
| 1 | must fix | flights | `providers/Flights.kt` `full()`, line 265 (`wide`) and 272 (`first`) | "LANDS" does not stand at one x on a 24-hour clock. Before the answer `wide` is only `!is24HourFormat`, so the first slot is 136 dp; when the answer has a departure on another day it becomes 196 dp, and "LANDS" moves 60 dp (90 px) to the right. That is every lookup with a day typed (still 13, `LH455 sat`) and every overnight flight on the day it lands (still 08: "LEFT SFO Thu", read on Friday). design-system.md section 14 calls it rare. The captures do not show it only because the Lenovo runs a 12-hour clock: there "LANDS" starts at x = 433 px (288.7 dp) in stills 08, 12 and 13 and before the answer (`fA` take 1, frame 237). Since `e12c315` the two layouts would cross-fade, so the label would stand in two places for about 100 ms. Read in the code, not seen on screen | The width follows the clock only, never the answer: `first = if (DateFormat.is24HourFormat(context)) 176 else 196`. 176 dp holds "LEAVES SFO Wed 14:47" (about 165 dp, worked out from the measured 12-hour string: label 65 px, gap 14 px, value 191 px less " PM"). Drop `FIRST = 136`; correct section 14 |
| 2 | should fix | flights, usual (every list) | Kind labels `Rows.kt` 524; footer hints and caps `Footer.kt` 51 and 116; slot labels `Bodies.kt` 115 | Second ink (0.80) on the glass over a window of the other brightness is too weak, and your usual puts three kind labels and the footer there at every opening. Light theme over the dark terminal: kind labels 2.37 : 1, "Choose" and "Close" 2.34 : 1, the caps' letters 2.2 : 1 (light 01, 07, 08, 11). A flight's row that is not selected: caption 2.54 : 1 at its right end (light 10); in dark over the white page caption, slot labels (12 sp) and the note's second part 3.1 to 3.15 : 1 (dark 10). Full ink on the same grounds measures 3.5 (light) and 3.95 (dark). On the pill everything is fine: 5.8 light, 4.2 dark. This is the check zero-state.md section 4 leaves to a device; its sentence names the wrong side (the weaker case is light glass over a dark window, as the M1 review measured) | As the copy's line did (M1, F8): full ink, in every list. `Rows.kt` 524: alpha `1f`. `Footer.kt` 51: `val ink = scheme.onSurface`; 116: `1f`. `Bodies.kt` 115: the slot labels at `ink` (they are 12 sp, weight 600: size and caps still set them apart). Caption (107) and the note's tail (126) stay second ink |
| 3 | should fix | flights | `Footer.kt` 66, the footer's left end | "AirLabs · 1:46 AM" is 14 sp (`SMALL`), the hints beside it on the same line are 13 sp (`HINT`): capitals 16 px high against 14 px (light 08: rows 612 to 628 against 614 to 628). Same weight, same baseline (y = 628 px), so the left end simply reads one size larger than "Actions" and "Close". Its place is right: it starts at x = 30 px = 20 dp, the discs' left edge | `style = HINT` (the file's own, 13 sp) on line 66. It also sets a grid cell's name: one line, one size |
| 4 | could | flights | `strings_24.xml` `flight_route`, both languages | The arrow in "San Francisco → Frankfurt/Main" is thinner and smaller than the words around it: its ink measures 4.14 : 1 on the pill where the caption measures 5.82 (light 08; dark 3.86 against 4.25), 13 px wide with 11 px of air on each side. It looks like a glyph from another face | An en dash with a space on each side in `flight_route`, or the panel's own drawn arrow (`Symbols.of("arrow")`, 12 dp, turned 180) between two texts |
| 5 | could | usual | `Field.kt` 114, decision 9 | Measured: the caret stands at 108 to 111 px (72 to 74 dp), the placeholder's "S" starts at 112 px, the titles' ink at 109 to 110 px ("Sheets" 109, "Files" and "Booklight settings" 110). A step of 2 to 3 px (up to 2 dp), seen only in the caret's off phase (stills 02, 03, 06). In its on phase the caret's left edge is the head of the column, 1 px left of the titles | Leave it at 74. At 72 the "S" would start at 109 px and the caret (108 to 111) would lie over its first 2 px: the decision's own test (the caret clears the ink by 1 px) fails. `1.dp` would gain 1 px on this screen, not worth a change |

## Measured and found right

**Your usual** (light and dark 01 to 06; the two themes agree to the pixel).

| What | Measured | Wanted |
| --- | --- | --- |
| Panel | 1080 x 432 px | 720 x 288 dp |
| G | ink 44 to 71 px, centre 38.3 dp | 38 |
| Three icons | each 30 to 84 px wide (20 to 56 dp), centres y = 104, 160, 216 dp | x = 38; pitch 56 |
| Titles | ink from 109 to 110 px (72.7 to 73.3 dp); centres 104, 159.7, 216 dp | box at 72 |
| Kind labels, "Close" | ink ends at 1047 to 1048 px and 1048 to 1049 px (698 to 699 dp) | one edge, box at 700 |
| Footer | caps 22 dp high (383 to 416 px), centre y = 266.3 dp; the `↓` cap about 27 dp wide, `tab` about 35; both end at about 830 px, 16 to 17 dp before `esc` | 266; 27 |
| Highlight, row one and two | y = 76 to 132 and 132 to 188 dp, x = 8 to 712 dp | the row's seat |
| Strip | last slot centred on x = 683.7 dp on both rows (the eye mark on row one, the arrow on row two); slots 32.7 dp apart | 684 |
| Opened list (04) | glyphs centred on x = 38.3 dp, names from 72.7 dp, the Enter mark centred on 684.3 dp, lines 40 dp apart, first line y = 132 to 172 dp, panel 616 dp | the row's columns |
| Arrival, take 2 of `zS` (frames 128 to 188) | the rows move in y only (title ink at x = 110 px in every frame); the lower edge uncovers them softly; nothing is cut | |

**Flights** (07 to 13).

| What | Measured |
| --- | --- |
| Row | pill y = 76 to 168 dp: 92 dp |
| Plane's disc | 20 to 56 dp, centre (38, 122): the row's centre |
| Caption, slot labels, note | all start at 109 to 110 px: the title edge of the row below |
| Lines | caption capitals from 141 px, baseline 156; slots: label and value share the baseline 193 px (label 180 to 193, value 174 to 193); note from 209, baseline 226. 18 dp of air above, 17 dp below: centred |
| Strip | centre y = 122 dp, the values' own centre line; slots at x = 618, 651, 683.7 dp, the same as the 56 dp row in 07 |
| Weak match (11) | an ordinary row: disc 20 to 56 dp, title from 110 px, centre y = 160 dp, "Flight" ends at 1048 px. (Its place in the middle of the web's rows is the interaction seat's finding 13) |
| Opened list under the row (10) | first line y = 168 to 208 dp, glyphs on 38.3, names from 72.7, the turned arrow on (684, 122), panel 332 dp |
| Chip (12) | starts at x = 20 dp, its plane on x = 38 |
| Not new, noted | the highlight on glass over the test backdrop: light theme over the white page 1.08 : 1 against the glass (hue and the outline carry it); in dark it is darker than the glass over the page (1.42) and lighter over the terminal (1.23) |

## The copy: the five fixes, checked again

| Fix | Holds? | Frames |
| --- | --- | --- |
| The line fades where it stands | Yes | `mA` take 1, 194 to 198; `mS` take 2, 326 to 336 |
| The pill comes whole | Yes | `mA` take 1, 199; `mS` take 2, 333 |
| An asked row grows with its pill and changes in a fade | Yes. Row and pill 56 to 92 dp together, strip and mark travel | `mS` take 1, 334 to 345 |
| The chip appears without a slide | Yes. In take 1 the caret crossed the chip's words (`mA` 195 to 201). In take 2 the placeholder fades where it stands, the caret takes one step, the chip fades in behind it | `mS` take 2, 328 to 346 |
| The placeholder is not cut | Yes, whole in every frame | `mA` take 1, 205 to 208; `mB` take 1, 460 to 470; `mS` take 2, 338 to 348 |

Seen on the first takes, reported by the motion seat, and named in `e12c315`; I confirm each and add no finding: the footer's "AirLabs" uncovered through its letters (`fA` 251 to 266); "LH 45" and "LH 455" printed over each other (`fA` 195 to 197); rows two and three cut by the lower edge (`zF` 69 and 72); a two-line answer in a 136 dp pill, 44 dp of it empty (`mS` 469: pill 114 to 317 px, text ends at 227); the strip cut in one frame when the answer lands early (`mS` 345 to 346); "Choose" and "Actions" over each other (`zM` 161 to 162). Of these I saw only the chip and the soft edge in a second take.

## Verdicts

- **The copy:** approved.
- **Flights:** approved with the must-fixes.
- **Your usual:** approved.
