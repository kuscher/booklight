# M1 · The copy: three reviews of the built feature, and what was done

Three designers looked at the built M1 on 2 October 2026: an interaction designer ([ux.md](ux.md)), a visual
designer ([visual.md](visual.md)) and a motion designer ([motion.md](motion.md)). They worked from stills in
light and dark and from three films, all taken on the HP Googlebook over the test backdrop. Each said "approved
with the must-fixes". The films were then taken again four times slower and stepped through.

| Finding | Done |
| --- | --- |
| The model's rows read "Gemini" for half a second after Tab, then changed under the selection (UX must, visual F2) | The system is asked about its model when the line is offered. A row offers "Ask" while the answer is not known yet, and hands over on Enter if the device has no model. Two checks at once wait for each other |
| The line was cut out in one frame and the pill came as a sliver (motion 1, visual F3, F4) | The footer's band is taken only from what the window has beyond row one. The line is drawn over the list's place and fades where it stands, its mark a little longer than its words, so the seat is never empty |
| Enter on a model's row: the content jumped and lay outside the pill (motion 2, visual F5) | A row's height travels on the pill's spring; its mark and strip travel with it; the name and the question change in a fade; Gemini keeps its slot beside Ask. The row had been rebuilt by Compose each time its label came or went: the label is now read whether it is used or not |
| The text beside the strip wrapped again when the strip changed, and a two-line answer could end in a four-line row (motion 3, visual F1, F6) | An answer's row keeps one room of 216 dp for its strip, also while it fades out |
| The line's parts were cut by the glass's lower edge as it arrived (motion 4) | The parts start 120 ms after the height. The tip's parts too |
| The chip flew in over the placeholder (motion 5, visual F13) | A chip nobody typed appears where the mark was, without a slide; the new placeholder waits for the chip's room |
| The placeholder was cut through its letters on Backspace (motion 6, visual F7) | Its box changes width on the app's own spring, without a clip |
| A held Backspace left the chip | One step for one press |
| Tab did nothing once something had been typed | Tab on the empty field opens a fresh copy whatever was typed before, and with the line switched off |
| "fi" put Finnish above "Fix spelling" | Under four letters, a row whose name starts that way comes first |
| "translate" and "in danish" became instructions | The translation is found by "translate"; "in", "into" and their German forms may stand before a language |
| `tr thai …` was a dead end; "any language" promised too much | An unknown language with a text hands over to Gemini; the wording says "another language" and "most languages" |
| `tr` let in 8,000 characters | The same 1,800 as under a copy's chip |
| The age changed when the line came back | It is said once in an opening |
| A link without a tracking tail had no Copy | It has: "Copy clean", or "Copy" |
| The line's first part was weak over a dark window (visual F8) | All of the line is full ink; what it holds is heavier |
| `esc` and `tab` were 1 px apart in width; the phone mark was outlined; the clean-link mark sat low; a space before the chip's ellipsis; „Kopiert gerade eben“ | All five changed |

Left as they are, on purpose: the pane's cross-fade when an answer lands (2.0's way, motion 9); the one pixel of
overshoot in the panel's height (motion 10); a three-line answer in a four-line row (motion 11); a hint for
Backspace on an answer; `tr danish` alone translating the copy after a pause, as every typed prompt does.
Motion 7 (frames dropped on Tab) was the debug build: on the release build, on the Lenovo, Tab on a copy with
five rows drew 83 frames with one late (1.2 %, 99th percentile 32 ms), and the answer's 334 frames had none late.
