# Booklight 2.0: motion review and motion spec

*1 October 2026. Desk review of `2026-10-01-booklight-2.0-design.md`, the plan, `booklight-2.0.html` and the 1.1.1 code. No device was touched and no file in the repo was changed. Every new value is a starting value; (c) says what to check. Line numbers are the code as it stands today.*

## Verdict

1. Not ready to build as written. Seven things in the spec and plan will pop, jump or change the opening. Each has a fix that stays inside the house rules and the existing springs.
2. The largest is the tip card. As planned it is part of the panel's height from the first frame, so every opening stops being the 68 dp unfold and becomes a 172 dp slab that opens from a seam 52 dp lower.
3. The opened row is under-specified. Built as "ordinary rows that cascade" it is 155 dp taller than the HP has room for, moves the footer 384 dp in one frame, and lets a row slide over the pill on closing. It needs one uncovering edge and a height budget in dp.
4. Three holes in the spec produce pops: held keys at the arrow, the tenth slot inserted by index, and `Effect.Type` putting a whole example in the field in one frame.
5. All of 2.0 rides the seven existing specs except two new ones: `type` (Booklight typing an example) and `tick` (the pinned countdown). Rows 21 to 55 below are the spec to build from.

## Must fix

**M1. The tip card changes the unfold on every opening.** `Metrics.height(model)` includes the card, `OverlayActivity.placeWindow` (line 138) sets the window to that height before the first frame, and `Panel` (lines 263 to 265) grows the seam about the middle of that height. With a card the seam starts 17 dp tall, centred 86 dp below the top, and grows to 172 dp; without one it starts 7 dp tall, centred at 34 dp, and grows to 68 dp. Today that tall variant is seen twice in a lifetime (the first-run cards). With tips on from the start it is every opening. Fix: build the gate design-system §4 already describes and the code never got. Until the glass is 85 % open (about 160 ms) the height target is the field's 68 dp, whatever the model holds; then it follows `Metrics.height` on `place` and the card's parts cascade (row 35). The window's first height must be 68 dp too: a 172 dp window around 68 dp of glass shows a blurred band under the field from the moment the blur comes in. A letter typed before the gate means the card never shows and the tip is not counted as shown. The same gate serves rows that are there at the opening (text from another app, a Commands row of the window). Cost: a second beat of about 250 ms on each opening. The alternative with no second beat is the tall unfold, which changes the opening itself.

**M2. An opened row does not fit on the screen.** `Metrics.maxRows` counts 56 dp rows and stops at 8. On the HP (1067 dp tall, panel top at 213 dp, 56 dp kept clear at the bottom) the panel may be 797 dp tall. Chrome opened under a full list is 68 + 8 + (8 × 56 + 8 × 48) + 8 + 36 = 952 dp with 48 dp action rows (1016 dp with 56 dp ones): 155 dp too much, 99 dp of it past the bottom of the screen. On the Lenovo (limit 904 dp) it is 48 dp too much. The height spring would then run the window into the screen's edge and the system clips it or moves its top. Fix, decided before anything moves: the list has a budget in dp (the room `maxRows` computes, 677 dp on the HP), not a count. When a row opens, rows under the block give way first (they leave, fade 80), then rows above the parent from the top (the parent and the pill slide up together on `place`). The action row's height becomes a token, `Metrics.action` = 48 dp.

**M3. "Ordinary rows that arrive as the cascade" pops in four places.** (a) `ResultsBody` (Rows.kt line 130) gives the list box its final height at once and lays the footer out under it: opening a row moves the footer 384 dp in one frame, out of the window, until the height spring uncovers it; closing puts it over the fading action rows in one frame. (b) Arriving rows rise 12 dp from below into places the rows underneath have not left yet: the eighth action row and the first row below it overlap for a few frames. (c) On closing, `RowSlots` fades the eight rows where they stand (80 ms) while the rows below slide up through them. (d) Closing from the last action row, the pill's lower edge rides `trail` (420) and the rows below ride `place` (520): the row under the list overtakes the pill and stands on it, highlighted, for about 100 ms. Fix: the block is one value (rows 21 and 25). The action rows are laid out once in their final places and uncovered by the edge of the rows below; the footer is anchored to the window's bottom edge, not to the list box; a pill that leaves a closing block moves rigid on `place`.

**M4. A held Tab pumps the list open and shut.** `Panel` (line 221) arms on every repeat of Tab, wrapping. With "moving the arming past the ninth icon opens the row" a held Tab runs nine icons, opens the list, runs eight rows, wraps, closes it: 17 steps, 850 ms a turn at the default 50 ms repeat, with 384 dp of window height each way. Fix: opening and closing take a press of their own (`repeatCount == 0`), like Enter and Tab into a scope. A held Tab or Right stops on the arrow; a held Down runs through the action rows and on, and never closes the list; a wrap never crosses the arrow in either direction.

**M5. The tenth slot jumps nine icons.** `ActionStrip` keeps its slots by index (`remember(n) { Slots(n) }`, `forEachIndexed` without keys, Strip.kt lines 105 and 156). A typed place that is not among the nine becomes slot 9 and the arrow slot 10: the nine icons move 36 dp left in the frame of the keystroke and the arrow's node becomes the place's icon. Then the one arming number runs from 0 to 9 and every name in between unrolls in full as it passes (about 28 ms each): nine names flash while the user is typing. Fix: slots keyed by action id. The tenth has no width at rest; it unrolls out of the arrow on `arm` and the nine slide left by exactly that (row 29). A move of more than one slot goes straight: only the slot left and the slot reached change their names (row 28). Motion row 4 says "a verb never adds or re-keys one"; 2.0 adds one, and this is how it is added without a jump.

**M6. "Try it" and `?` set a whole example in one frame.** `Effect.Type("timer 10m tea")` through `OverlayModel.type` makes the chip, the argument, the preview row and the new height appear together while the card goes: a jump cut, and the chip has no typed keyword to grow from (principle 4). Fix: Booklight types it, a letter at a time, on the new spec `type` (row 37). The card or list underneath stays until the last letter; one search runs at the end, so the list changes once and the height moves once. "timer" really stands in the field, so row 6 plays as designed when the space lands.

**M7. Ticking a task shows no tick and sends the pill to row one.** As planned, Enter runs `TickTodo` and refreshes: the row fades in 80 ms unticked. `search(keep = true)` (OverlayModel.kt lines 143 to 148) falls back to `selected = 0` when the selected id is gone, so the pill travels to the first row instead of taking the next (motion row 16). Fix: row 41. The file is written at once; the row shows its tick and strike for 260 ms, then folds; the selection keeps its index.

## Should fix

**S1. The title beside ten slots.** Design-system §2 says strip ≤ 340 dp, title ≥ 240 dp. Ten slots at rest are 360 dp; with "Open" unrolled about 430 dp, with "New window" about 485 dp, with a tenth "Left two thirds" armed about 535 dp (more in German). The title is left with about 185, 130 and 80 dp. It is a weighted `Text` with `TextOverflow.Ellipsis` (Rows.kt line 273), so each frame of an unrolling name re-measures it and the ellipsis swaps glyphs. Lay the title out once at its own width and clip it with the 24 dp fade the design system already asks for; restate the two tokens.

**S2. Key caps move when the strip arrives.** In the mock-up the caps sit left of the strip, so they slide about 145 dp left when the row is selected and back when it is not, on every step of the pill. The slide would ride Compose's default `SizeTransform` spring (the trailing `AnimatedContent`, Rows.kt line 289), which is not in `Motion.kt`. Reserve the strip's slot at its widest armed width on every keys row; kind label and strip trade places inside it; the caps sit left of it and never move (row 34).

**S3. Tab on a keyword: text and chip in the same frame.** `fill()` empties `model.query`; the field follows through `LaunchedEffect(model.query)` (Panel.kt line 137), a frame late, so there is likely one frame with the chip arriving and the "s" still standing. Set the field in the Tab handler, as `onChange` does. A one-letter keyword also makes the missing travel of row 6 plain: the "s" vanishes and "Settings" fades in about 18 dp to its left. Build row 6 (i) and (ii) for this path. Repeats of the Tab that entered the scope must not arm the new first row.

**S4. The Enter mark inside an opened list.** The mock-up gives every action row a strip of one ("⏎" in a pane). By the 1.1 rule it fades out with the old row and arrives 60 ms after the pill's target changes: the same pane fading between two places 48 dp apart on every step, which is the thing the house rules forbid. Print the bare mark in every action row and let the pill show it (row 24), as the option strip does one level down. If the pane is wanted: one pane, drawn with the pill, at the pill's vertical centre.

**S5. Closing draws in to the wrong place.** The seam draws in to the middle of the panel: 86 dp down with a card, 284 dp with eight rows, 476 dp with an opened row. The opening starts 34 dp down. Anchor the seam on the field's centre line in both directions (row 39).

**S6. The first rows of `notes` and `todo` come from a file.** Every 1.1 scope answers in milliseconds; these read through the folder grant. Until the rows land, either the rows of the previous text stand under the new chip, or the list empties and the height shrinks and then grows. Hold the height (row 42).

**S7. The window's page change.** "A short rise and fade" as in the mock-up makes the old page vanish in one frame. The old page fades (70) with its own pill; the new one follows the column's pill by 60 ms and comes from the side the pill came from (row 44). `Pill` animates from wherever it last was (top 0, height 0 in the window when nothing is selected): a pill that becomes visible must snap to its row first, then fade in.

**S8. The column under 720 dp.** A threshold makes the page jump 148 dp while the user drags the window's edge, and an animated column width re-wraps the whole page each frame. Let the column's width follow the window's between 868 and 720 dp (row 46). The spec's column (216 dp, icons under 720) and design-system §5's breakpoints (strip under 1100 dp, rail above) contradict each other; task 13 has to settle one.

**S9. "No key yet".** Removed the moment `keySeen` is set, the line takes its height with it and every row under it jumps, behind the panel that is opening. Hold it until the window has the keys again, then change it in place (row 48).

**S10. The pinned window.** Reuse one window (`singleInstance`, `onNewIntent`) so that a new pin is ours to animate; a second task is the system closing one window and opening another. One size for every kind. No arrival of our own on top of the system's. Digits change one at a time on `tick`; the format is chosen once; the count stops at 0:00 (rows 50 to 54).

**S11. Specs outside `Motion.kt`.** The unfold's spring is `spring(0.72, 1000)` in Panel.kt (design-system §6 says `open` = spring(0.9, 800)); its tweens and easings are in Panel.kt; `roll` is written out in Rows.kt; Rows.kt line 289 uses a default spring. 2.0 uses `roll` in five new places. Move `open`, `roll`, `type` and `tick` into `Motion.kt` and correct the doc.

**S12. Feedback where there is no footer.** The footer exists only with rows. "No tips", the last open task ticked, and Copy in the pinned window leave nothing to write "Done" on. Each needs its own answer in place (rows 38, 41, 54).

**S13. Per-app switches and icons.** With the master switch off, dim the app rows; do not fold them (row 49). An app icon that loads after its row has landed cuts in (`produceState` in `RowPicture`); it should fade (80) or be in the cache before the page or the row arrives.

**S14. A typed tenth and an opened list.** "chrome top left", then Right: the list would hold "Top left" again under a row that already shows it. The tenth rolls back into the arrow as the list opens and the pill lands on that action's row, not on the first.

## Motion table for 2.0

Specs as in `Motion.kt`: `place` spring(0.86, 520) · `lead` spring(0.82, 1100) · `trail` spring(0.9, 420) · `pop` spring(0.62, 700) · `arm` spring(0.78, 560) · `fade(ms)` tween, 110 unless given · stagger 22 ms. From the design system: `roll` (half a line on `place`, fade 120 in, 70 out).

**New named specs**

- `type`: one character per step; step = 480 ms ÷ the number of characters, never under 16 ms and never over 40 ms ("timer 10m tea": 13 characters, 37 ms each, 480 ms; "snap": 40 ms each, 160 ms). The first character lands in the frame of the key. A cadence, not a curve. Animations off: the whole text in one frame.
- `tick`: tween(160 ms, cubic-bezier(0.2, 0, 0, 1)), 8 dp of travel; the old digit fades 70, the new one 120. Not a spring: it repeats every second and must be still for the other 840 ms. Animations off: cut.

**Not animated, derived** (as radius, blur and content alpha are functions of the window's size in the unfold): the pill's *cover* of a row (the share of the row's height the pill overlaps, 0 to 1), the block's uncovering, the column's width.

**Holds** (kept with animations off): strip delay 60 ms (1.1), tick hold 120 ms, "Tips are off" 1600 ms, feedback 520 ms (1.1).

| # | What | What moves | Spec · delay | What stays fixed |
| --- | --- | --- | --- | --- |
| | **The opened row (§6)** | | | |
| 21 | A row opens its other actions (replaces row 2) | One value, `block`, from 0 to N × 48 dp. It is the top of the first row below (the footer's, when there is none), the lower clip of the action rows and the growth of the window. Each action row shows as far as the edge is past its top (alpha over its first 24 dp). The chevron turns 180° | `place`; chevron `pop` · no stagger: the edge is the cascade | The parent row, its nine icons, the arrow's place, rows above, the panel's top. The action rows are laid out once and never move |
| 22 | The pill goes into the first action row | Lower edge to the action row's lower edge; the upper edge and the left edge (inset 48 dp) follow | lower `lead`; upper and left `trail` · same frame as 21 | The pill's right edge. The uncovering edge is ahead of the pill's lower edge throughout (about 70 dp against 16 dp at 33 ms) |
| 23 | The parent's strip while its list is open | The armed name rolls up; the pane goes to the arrow's slot and shows as far as the pill covers the parent; icons go from ink 1.0 to 0.80 once the pill has left | `arm`; pane alpha = cover; ink `fade(120)` | Nine icons, their pitch, the arrow. The strip stays on the opened parent: the one exception to "the strip belongs to the selection". No kind label |
| 24 | The pill moves in the list, back onto the parent, past the list | Upper and lower edges; the left edge goes with the upper edge at the block's two ends. The Enter mark of each action row shows by the pill's cover | `lead`, `trail` | Marks, icons, names. No pane in the block: the pill is its highlight. The list stays open |
| 25 | The list closes: Left, the arrow, Enter on the arrow (replaces row 3) | 21 backwards: `block` to 0, the rows below and the window with it, the chevron back. Action rows are covered where they stand and removed under 1 px. A pill inside returns to the parent as one rigid piece; a pill below rides its row | `place`, pill included; chevron `pop` | The action rows until covered; the parent. Rigid, on the same spring, the pill cannot be overtaken |
| 26 | The list closes because the user typed | The text, in the same frame. Parent keeps its id: 25, while the list re-ranks. Parent gone: its action rows fade with it. The pill goes to row one | `place`; fade 80; pill `lead`, `trail` (rigid `place` if it was in the block) | The field. No row is re-keyed: action rows are keyed parent id + action id |
| 27 | Uninstall as an action row | Rows 15 and 16: the alarm pane grows leftward from the mark's home to "Press again to uninstall"; the line drains | `lead`; `drain` | The mark; the row's height. It is the only pane the block ever shows |
| | **A typed place (§6)** | | | |
| 28 | A typed place among the nine (amends row 4) | The pane goes straight to its slot: its two edges are the mean of the slots it is on, weighted by how far each name is unrolled. Only the slot left and the slot reached change | `arm`, one value per slot | Every icon between; the arrow. No name between the two unrolls |
| 29 | A typed place beyond the nine: the tenth | The tenth slot unrolls out of the arrow, from no width to its measured width; the nine icons slide left by the same amount; the pane travels to it as 28 | `arm` | The arrow (the strip's right end), the icons' pitch. The tenth's width is measured before it opens |
| 30 | The typed place changes while armed ("top l" to "top r") | Icon and name cross-fade; the pane's left edge goes to the new measured width | fade 80; `lead` | The pane's right edge, the arrow, the mark |
| 31 | The typed place is deleted | 29 backwards, from the width it has; the pane returns to the row's own default | `arm` | The arrow |
| | **Keywords and key caps (§5, §11)** | | | |
| 32 | Tab on a keyword, whichever row has the pill | Row 6, from the keyword standing in the field: its copy slides and scales (24 to 15 sp) into the label, the chip grows from its bounds, the G gives way to the icon on one centre, the caret slides to the chip's end. The rows are replaced under a pill that does not move; the scope's own row fades with the others; the footer's label cross-fades | row 6: `place`, `pop`; rows fade 80, cascade 22 ms | The pill, the baseline, the footer's key caps. Field text and chip change in one frame |
| 33 | A row with key caps arrives | Nothing of their own: they are part of the row and rise 12 dp and fade with it | `place` · the row's stagger | Cap widths, measured once. No stagger per cap |
| 34 | A row with key caps becomes selected | Cap ink 0.80 to 1.0; kind label out, strip in, inside the reserved slot (row 5). Optional: the caps go down in order and come up together | fade 120; row 5 · optional press `pop`, 40 ms apart, after 200 ms at rest | The caps' place, in every state: the column of combinations stays a column |
| | **The tip card (§12)** | | | |
| 35 | The card arrives | The unfold is row 20 at 68 dp, always. At 85 % open the height leaves 68 for 172 dp; the mark, the text and the two answers rise 12 dp and fade 140 | `place` · 22 ms apart, from the gate (about 160 ms) | The field, the seam's centre (34 dp), the edge light's start (190 ms). Nothing typed before the gate, or no card |
| 36 | The card leaves on the first letter | The letter, in the same frame. The card fades where it stands; rows cascade as ever; the height goes on to the list's | card fade 60; rows `place` · 22 ms; height `place` | The field. The card does not move and the rows do not wait for it |
| 37 | Booklight types: "Try it", an example in `?`, a Commands row of the window | The answer takes the pressed tint (80 in, 120 out). Letters land one by one; the keyword becomes its chip when its space lands (row 6). After the last letter: 36 | `type`; then row 36 | The card (or the `?` list) and the height, until the last letter. One search, at the end |
| 38 | "No tips" | The card's line rolls to "Tips are off. Turn them on in Booklight's window."; the two answers fade; then the card folds and the height returns to 68 dp | `roll`; fade 60 · hold 1600 ms · `place`, fade 80 | The mark, the card's height until it folds |
| 39 | Closing with a card, an opened row or a list (amends row 20) | Row 20's closing, unchanged in time (155 ms). The seam draws in to the 7 dp seam on the field's centre line, whatever the panel's height was | row 20 | The contents, covered where they stand. Nothing folds first; the height is not animated on the way out |
| | **`?`, tasks, notes (§7, §8)** | | | |
| 40 | Enter on a keyword's row inside `?` | Chip to chip: the label rolls, the icon swaps on one centre, the chip's right edge goes to the new measured width, the argument slides with it | `roll`; `pop`; `place` | The chip's left edge and height. The pill stays on row one |
| 41 | A task is ticked | The check draws itself in the box; a 1.5 dp line strikes the title from its start, leading end first; the title's ink goes to 0.60. Hold. Then the row leaves, the rows below and the window close up, the pill stays and takes the next row | check `fade(140)` as the stroke's length; strike `lead`; ink fade 120 · hold 120 ms · row fade 80, the rest `place` | The box, the title's layout (the line is drawn over the measured text), the pill. The file is written at Enter, not after the hold |
| 42 | The first rows of a notes scope | The old rows fade; the height holds; when the file has been read the rows cascade and the height moves once | fade 80; then `place` · 22 ms | The height, until the rows are known. Rows are keyed by the line, not by rank, so narrowing moves them |
| | **The Booklight window (§10)** | | | |
| 43 | The column's pill | Down: lower edge first; up: upper edge first. The list's `Pill` | `lead`, `trail` | Icons, labels. It is there on every page |
| 44 | The page changes | The old page fades where it stands, its own pill with it. The new page's blocks come 12 dp from the side the pill came from (from below when it went down) and fade 140 | old fade 70; new `place` · 60 ms after the pill's target last changed, then 22 ms apart, six steps at most | The column, the page's left edge and width. A held arrow shows no pages in between |
| 45 | The window arrives | The window itself is the system's. Inside: the column's items, then the page's blocks, rise 12 dp and fade; the column's pill comes with its own item | as 1.1's `Rise` · items 22 ms apart, the page from 66 ms | The pill's place: it is on the right section from the first frame |
| 46 | The column narrows | Nothing of ours. Column width = window width − 652 dp, held between 68 and 216 dp: from 868 down to 720 dp the labels are covered behind a 24 dp fade and the page keeps its width | derived | The page's width and every line break in it, between 720 and 868 dp. Labels are laid out once |
| 47 | The keys cross between column and page | The pill of the level that loses the keys goes to half its alpha; the other comes to full. Going into the page, the page's pill appears on its row, in place | fade 120 | Both pills' places. Nothing travels between levels |
| 48 | "No key yet" ends | When the window next has the keys: the lamp dot becomes the drawn check, the line rolls to "Your key works", the column's dot shrinks away | check `fade(140)`; `roll`; `pop`, fade 80 | The line's height and every row under it. Nothing changes while the panel is open in front |
| 49 | Switches per app | One switch: row 11. The master off: the app rows dim to 0.40, top to bottom; their thumbs keep their sides | `pop`; fade 120 · 22 ms apart | Every row's height and place |
| | **The pinned window (§4)** | | | |
| 50 | The pin arrives | The system's: the window, its place, its layer, its shadow. Ours: nothing; the content is whole in the first frame. The panel closes at once, with no feedback word | the system's | The content. Two arrivals on one window would fight |
| 51 | A countdown digit changes | Only the digits that changed: the new one comes down 8 dp, the old one leaves downward | `tick` · on the second | The colon, every digit that did not change, each digit's slot (tabular, measured once), the format |
| 52 | Zero | The last tick; then the time swells to 1.06 and settles, once; the line under it rolls from "rings at 14:12" to "rang at 14:12" | `tick`; `pop`; `roll` | 0:00. It does not go negative, pulse or repeat |
| 53 | A new pin | The old content fades where it stands; the new rises 12 dp and fades 140 | fade 70; `place` | The window and its size |
| 54 | Copy in the pin | The label rolls to "Copied"; the check draws itself; back after 1600 ms | `roll`; `fade(140)` | The button's width: the wider of the two labels, measured first |
| | **Feedback (§8)** | | | |
| 55 | The feedback word: "Added to Todo", "Added to ideas.md" (amends row 17) | The check draws itself; the word slides 8 dp from the left and fades 120; out 80. Then row 20's closing after 520 ms | `fade(140)`; `place` | The footer's right side. Where there is no footer the answer is in place: rows 38, 41, 54 |

**Per row: how it is interrupted · with the system's animations off · what pops if built naively**

- **21.** *Interrupted:* Left or the arrow mid-flight retargets `block` from its value and velocity (row 25); a held key does nothing (M4); typing is row 26. *Off:* the block is open in one frame. *Naive:* M2 and M3; an icon swap instead of a turning chevron.
- **22.** *Interrupted:* Down retargets both edges to the next row. *Off:* cut. *Naive:* without the left edge the pill covers the inset; with all edges on one spring it stops being the list's pill.
- **23.** *Interrupted:* the pane's alpha is a function of the pill, so it cannot be caught half-way. *Off:* cut. *Naive:* the strip leaves with the selection (1.1's rule) and the nine icons vanish while their list is open; or the pane stays on an unselected row, a second highlight on one level.
- **24.** *Interrupted:* key repeat retargets; marks follow the pill, so a held arrow needs no 60 ms rule. *Off:* cut. *Naive:* S4.
- **25.** *Interrupted:* the arrow again mid-flight re-opens from where the edge is. *Off:* cut. *Naive:* M3 (c) and (d).
- **26.** *Interrupted:* each letter retargets; at most one list change per frame (rule 6). *Off:* cut. *Naive:* action rows keyed by index become other rows' content for a frame; the pill is left on a row that is gone.
- **27.** As rows 15 and 16. Ctrl + digit and the arrows never run it.
- **28.** *Interrupted:* each slot's amount retargets from where it is; three slots can be part-shown and the pane is still their mean. *Off:* cut. *Naive:* one number from 0 to 8 unrolls every name on the way.
- **29.** *Interrupted:* a letter that keeps the reading restarts nothing; one that changes it is row 30; one that removes it is row 31. *Off:* cut. *Naive:* M5.
- **30.** *Interrupted:* retargets; under 120 ms apart the cross-fade cuts (rule 6). *Off:* cut. *Naive:* the new name's width is applied at once and the pane's left edge jumps.
- **31.** *Interrupted:* typing the verb again unrolls from the width it has. *Off:* cut. *Naive:* the slot is removed and nine icons jump right 36 dp.
- **32.** *Interrupted:* typing during it is the argument, placed where the caret is going; Backspace is row 7 from where it is; a held Tab enters once and arms nothing. *Off:* cut. *Naive:* S3; the pill flying to the scope's row and back.
- **33.** *Interrupted:* as any row. *Off:* cut. *Naive:* a stagger per cap: 24 things arriving for eight rows.
- **34.** *Interrupted:* the optional press is cancelled when the pill moves on. *Off:* no press; ink cuts. *Naive:* S2.
- **35.** *Interrupted:* a letter is row 36 from wherever the card is (its alpha runs back, it never restarts); the key again folds the panel from where it is. *Off:* the panel is 172 dp in the first frame with the card in it. *Naive:* M1.
- **36.** *Interrupted:* more letters retarget the height only. *Off:* cut. *Naive:* a card that slides away or a list that waits for it: both hold up typing.
- **37.** *Interrupted:* any typed key stops it; the field keeps what has landed and takes the key. A second press of Enter lands the rest in one frame and runs nothing. Esc folds. *Off:* the whole text at once, then row 36. *Naive:* M6; or a search per letter, which makes the window rise to five rows of apps for "t" and fall back to one.
- **38.** *Interrupted:* typing puts the card away at once (row 36); the switch is already off. *Off:* the line cuts, the hold stays, the fold cuts. *Naive:* the card and its 104 dp go in one frame and nothing says where tips went.
- **39.** *Interrupted:* the key again turns it round, as 1.1.1. *Off:* gone in one frame. *Naive:* S5; folding the card or the block first would make closing longer than opening.
- **40.** *Interrupted:* typing goes to the new scope. *Off:* cut. *Naive:* the old chip scales out and the new one slides in from the right: two objects for one thing.
- **41.** *Interrupted:* typing ends the hold and the row leaves at once (fade 80); Enter on the same row again does nothing; Down and Enter ticks the next, each row on its own. *Off:* tick and strike cut, the 260 ms stay, the row then goes in one frame. *Naive:* M7; a `LineThrough` span grown per frame re-lays the title each frame.
- **42.** *Interrupted:* typing before the file is read just waits with it. *Off:* cut. *Naive:* S6; rows keyed by rank cross-fade on every letter.
- **43.** *Interrupted:* key repeat retargets. *Off:* cut. *Naive:* none; it is the existing `Pill`.
- **44.** *Interrupted:* another section within 60 ms cancels the page that had not started; one that had started fades from where it is. *Off:* cut. *Naive:* S7; a page composed inside the pill's flight drops a frame of the pill.
- **45.** *Interrupted:* a key during it acts at once; the rise continues. *Off:* everything in the first frame. *Naive:* the pill drawn before its item, on an empty column; the arrival replayed when the window is rebuilt for a new theme or language (remember that it has played).
- **46.** *Interrupted:* it is the user's hand. *Off:* the same. *Naive:* S8.
- **47.** *Interrupted:* retargets. *Off:* cut. *Naive:* the page's pill flying in from the top of the page (S7).
- **48.** *Interrupted:* none. *Off:* cut, in place. *Naive:* S9.
- **49.** *Interrupted:* flipping back mid-dim retargets. *Off:* cut. *Naive:* nine rows folding away under the pointer; icons cutting in (S13).
- **50.** *Off:* the system's choice. *Naive:* our own rise on an empty flat window while the system scales it in; the panel waiting 520 ms to say "Pinned".
- **51.** *Interrupted:* a new pin cuts it. *Off:* cut. *Naive:* the whole time in one `AnimatedContent`, as the answer in Rows.kt: four digits rolling half a line every second; proportional figures shifting the colon; "10:00" to "9:59" moving every digit.
- **52.** *Off:* cut. *Naive:* "−0:01" adds a glyph and moves the rest; a loop that pulses until closed.
- **53.** *Interrupted:* a third pin retargets the fades. *Off:* cut. *Naive:* S10.
- **54.** *Interrupted:* a second click restarts the 1600 ms, not the roll. *Off:* cut. *Naive:* the button changes width with its label.
- **55.** As row 17. *Naive:* `Footer` slides the word by a sixth of its width, not 8 dp, and the check does not arrive on its own; correct both when the check is drawn.

## (a) Where the spec and plan pop or jump as written

| Where | Built as written | Fix |
| --- | --- | --- |
| Spec §12, plan 11: `Card.TIP` in the model, in `Metrics.height` | Tall unfold on every opening; a tip that flashes when the user types at once | M1, rows 35 and 36 |
| Spec §12: "Try it … the example is typed for you"; §7 "types the example"; §10 "opens the panel with it typed" | Text, chip, row and height in one frame | M6, row 37 |
| Spec §6: "the rows below make room on `place`, the new rows arrive as the list's cascade" | Footer jump, overlap, fade under moving rows | M3, rows 21 and 25 |
| Spec §6, plan 3: no limit on the opened list's height | 952 dp on a screen with room for 797 | M2 |
| Spec §6: "moving the arming past the ninth icon (Tab, →) opens the row" | A held key opens and closes it every 850 ms | M4 |
| Spec §6: "else shown as a tenth, armed and named, before the arrow"; plan 3 "`Strip`: … a typed extra action as an armed tenth" | Nine icons jump 36 dp; nine names flash | M5, rows 28 to 31 |
| Spec §6: "Left, the arrow again, or typing closes them" with the pill inside | The row below overtakes the pill; on typing the pill is on a row that is gone | rows 25 and 26 |
| Mock-up: "⏎" pane on the selected action row | A pane fading between rows on each step | S4, row 24 |
| Mock-up: the opened parent with its icons and no pane | By 1.1's rule the strip leaves with the selection | row 23 |
| Mock-up: `more` and `less` as two drawings | The arrow swaps in one frame | row 21: one chevron, turned |
| Spec §11: "Tab makes it the chip, whichever row is selected" | One frame of chip and keyword together; "s" vanishing | S3, row 32 |
| Spec §5, mock-up: caps left of the strip | Caps slide about 145 dp on each selection | S2, row 34 |
| Spec §8: "Enter ticks one off" | No tick seen; pill to row one | M7, row 41 |
| Spec §8: "Files are read when a notes scope is open" | Stale rows, or a height that moves twice | S6, row 42 |
| Spec §7: Enter on a keyword inside `?` | Old chip out, new chip in from the right | row 40 |
| Spec §10: "the page changes with a short rise and fade" | Old page gone in one frame; page pill left behind for 50 ms | S7, row 44 |
| Spec §10: "Under 720 dp of width the column is icons only" | The page jumps 148 dp during a drag | S8, row 46 |
| Spec §10: "Until the panel has once been opened … 'No key yet'" | The line and its height go while nobody looks; rows jump | S9, row 48 |
| Spec §3: the master switch and one per app | Rows folding away, or left live-looking | row 49 |
| Spec §4: "a new pin replaces the old one"; "the countdown" | Two system transitions; a roll of the whole time each second | S10, rows 51 to 53 |
| Spec §12: "No tips (turns the switch off)" | 104 dp gone in one frame, no word | row 38 |
| Design-system rows 2 and 3 (the second line of chips) | Never built; 2.0 replaces them | Retire them for rows 21 and 25 in task 13 |

## (b) Signature moments

1. **The row unfolds.** The panel opens out of a seam; a row opens its actions out of its arrow, by the same idea: contents laid out once and uncovered, one value driving the rows, the clip and the window, closing the same way backwards. The chevron's turn on `pop` is its small flourish, the pill pouring into the first action row its lead and trail. This is the gesture 2.0 will be judged by, because it sits beside the unfold. Cost: one `Animatable`, a clip, the footer anchored to the bottom edge.
2. **Booklight types.** "Try it", an example from `?`, a line of the Commands page: the letters land at 37 ms each, "timer" becomes its chip in front of the user, the row arrives. It teaches the one thing a launcher has to teach, and it reuses paths that fast typing has already hardened. Cost: a loop, a flag that holds the body, one search.
3. **The check that draws itself.** One stroke, 140 ms, wherever Booklight says "done": the ticked task with the line chasing it across the title, the feedback word, "Your key works", "Copied" in the pin. One mark for the whole app. Cost: a path and its length, about 20 lines.

Optional, cheap, only if the first three are in: the caps of a keys row going down in the order they are pressed and coming up together (row 34). Keep the edge light for the arrival alone; a second light on the tip or the opened row would spend it.

## (c) Verify frame by frame on a device

First, a debug-only slow factor in `Motion` (springs' stiffness ÷ slow², tweens × slow), as `Arrival` has: at 60 fps a one-frame fault on the Lenovo's 120 Hz panel cannot be seen in a recording, and only the opening can be slowed today. Recordings show the whole screen: delete them afterwards.

1. **The opening with a tip** (fast and `opening=slow`). The seam is centred 34 dp down and never taller than 68 dp before the gate. No blurred band under the field in any frame. The card's first frame is after 85 % open. The edge light starts at 190 ms and does not stutter while the height grows.
2. **A letter at 0, 100, 160 and 250 ms after the key** (`./bl debug keys`). No card before the gate; after it the card's alpha only ever runs one way and back, never restarts. The height changes direction at most once.
3. **A row opening and closing**, 60 and 120 Hz, with eight results, Chrome first and Chrome fifth. The window's top edge does not move by a pixel. The footer is on the bottom edge in every frame, never missing, never over a row. No frame with two rows' text over each other. The pill's lower edge is never past the uncovering edge. Closing from Uninstall: no row passes over the pill. The window's bottom never reaches the taskbar on the HP. No frame dropped at full blur with the panel at its tallest (design-system §9, check 1).
4. **Tab and Right held for three seconds on Chrome's row.** The list opens at most once and never closes. The title swaps no glyph.
5. **`./bl debug keys "chrome top left"`**, then Backspace five times, then `r`. At "to" the tenth comes out of the arrow; no frame in which the nine icons move more than the tenth has grown; no name between the two unrolls; the arrow does not move by a pixel; the pane's right edge stays when the name changes.
6. **`s`, Tab, with Slack selected.** No frame with the chip and the "s" together, and none with neither. G and icon share a centre. The caret's x is continuous. The pill does not move. The footer's caps do not move.
7. **`k`, Tab, Down held through eight rows.** The caps stay a column to the pixel. No strip during the hold.
8. **A tick.** The stroke is visible for at least 8 frames at 60 Hz; the line reaches the title's end before the row leaves; the pill does not move; the rows below and the window's bottom move as one. Two ticks 150 ms apart. The last open task.
9. **`todo` and `notes` first thing after the process starts.** Measure chip to rows. The height changes once.
10. **"Try it"** for the shortest and the longest example; a letter typed in the middle; Enter twice. The window's height changes once; nothing runs on the second Enter.
11. **The window.** Up and Down held in the column: no page in between, no frame dropped while the pill is in flight (count frames, or `dumpsys gfxinfo`). The page comes from the pill's side. Drag the edge slowly from 900 to 700 dp: no line of the page re-wraps between 868 and 720.
12. **"No key yet"**, the window beside the panel: nothing in the window moves while the panel is open; on return it plays once and no row shifts.
13. **The pin.** What the system does when the window opens and when the layer is granted (a second transition, a flash, a move?). 10:00 to 9:59 and 1:00 to 0:59: only changed digits move, nothing shifts sideways. Zero. A note pinned over a running timer: no tick after. Frames produced in 10 s of counting: about ten bursts, not 600 frames.
14. **Closing with a card, with a list, with an opened row.** The seam ends on the field's centre line; nothing reflows on the way.
15. **Animations off.** Every row of the table cuts; the holds remain; the panel with a tip is 172 dp in its first frame.

## (d) Performance cautions

- **Window width is never animated**, anywhere: not the panel for ten slots, not the pin for a QR code, not the column (its width follows the window, the user's hand). Device finding: 30 of 60 frames, asymmetric.
- **Window height has one writer per frame**, `sizeWindow`. The block must not add a second: it feeds `Metrics.height` and the one height spring carries it.
- **Text laid out every frame.** (1) The row title beside a strip whose width changes: today, on every frame of an arming; in 2.0 also while a tenth unrolls during typing (S1). (2) Any animated width of the window's column: the whole page re-wraps. (3) Key caps and title pushed by a size transform (S2). (4) A strike-through as a text span grown per frame: draw a line. (5) Digits scaled by font size: use the layer's scale. (6) A chip label re-measured while it rolls: measure both names first.
- **`ActionStrip` lays itself out on every frame of the arming** (its measure block reads the animated value). With eleven slots that is still cheap, because the children are measured once; it stops being cheap the moment a child's text changes per frame. Keep names, icons and marks at fixed sizes and only place them.
- **One search for a typed example**, not one per letter: thirteen searches and thirteen list changes in 480 ms otherwise.
- **The page must not be composed inside the pill's flight.** Either compose all seven when the window starts (the stage's loop gated on its page being shown; it runs a model and a search every 260 ms) or start at 60 ms and prove no dropped frame. The Commands page is the long one: make it lazy if it passes 40 rows.
- **The countdown is not a frame loop.** One wake per second, aligned to the end time; 160 ms of frames, then none; nothing while the window is stopped. `Motion.of` reads the animation scale once, at creation: a window that lives for an hour should read it again on resume.
- **The glass shader and the blur run over the whole panel on every frame of a height change.** 2.0's tallest panel is taller than 1.1's 568 dp; M2's budget is also the blur's budget.
- **Cascades stay short:** 22 ms steps, six at most for a page, none for caps, none for the block (its edge is the cascade).
