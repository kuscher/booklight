# Booklight 2.0: visual review

*1 October 2026. Reviewed: `docs/design/booklight-2.0.html` (rendered light and dark in headless Chrome, boxes read with `getBoundingClientRect`), the 2.0 spec, `design-system.md`, the 1.1 captures and the overlay / window code. Text widths were measured in Google Sans Flex at the code's sizes (`LABEL` 14/500/0.1, title 17/500, `HINT` 13/500/0.1). Nothing was run on a device: every dp below is a desk value, and four things need a device before they are locked (listed at the end).*

*Scale: the panel mock-ups are drawn at 640 px for 720 dp (1 px = 1.125 dp). The window mock-up is 1 px = 1 dp. All numbers below are dp in the app unless they say px. x is measured from the panel's left edge.*

## 1. Verdict

1. The direction holds: icons at the right, rows underneath, one card, one column. None of it needs a new idea; it needs the 1.1 grid applied without exceptions.
2. As drawn it is not buildable to 1.1's standard. The nine-icon row leaves the title 130 dp (108 in German); with the typed "tenth" it leaves 71 dp (51 in German), less than the word "Chrome".
3. The opened list is where "misaligned" will come back: an indented pill whose left edge sits on no line, a third text edge, a mark-only pane, and a strip left standing on an unselected row.
4. The window and the pins import things Booklight does not have: divider lines, a yellow "New" pill, an orange status, mono chips, a home-made title bar.
5. Fix the thirteen must-fix items, hand the builder section 5, and re-draw the mock-ups from it before Alex looks again (the published page currently shows all seven window pages stacked, with five pills).

## 2. Must fix

**M1. Nine icons and an arrow do not leave a title at the coded slot width.**
The strip's slot is 36 dp, not 32 (`Strip.kt`: `EDGE 9 + icon 18 + EDGE 9`; 55 px on the Lenovo capture). Row maths: 720 − 16 (pill inset) − 24 (row padding) − 36 (icon) − 28 (title padding 16 + 12) = **616 dp for title text plus strip**. Strip = 36·n + 27 + name + air (6 at an end, 12 inside).

| Armed | Strip, 10 slots at 36 | Title left |
| --- | --- | --- |
| Open (38 dp) | 431 | 185 |
| New window (87) | 486 | **130** |
| Middle third (84) | 483 | 133 |
| DE Neues Fenster (99) | 498 | 118 |
| DE Mittleres Drittel (109) | 508 | **108** |

1.1's worst case is 238 dp (seven slots), and `design-system.md` promises title ≥ 240 and strip ≤ 340. "Google Calendar" is 136 dp, "Visual Studio Code" 150. The title's width also changes with every Tab (the strip's width is live, the title is `weight(1f)`), and the code truncates with an ellipsis, so a long name would swap glyphs while the pane travels.
Fix: slot pitch **32 dp** at rest (edge 7), air 4; the armed slot keeps the pane's inner edge 9. Strip = 32·n + 31 + name + air (4 / 8). Worst cases become 446 (EN) and 468 (DE): title **170 / 148 dp**. Fix the title's box per row at 616 − the strip's widest state, and end it in the 24 dp fade the design system already asks for. Spec in 5.1.

**M2. The typed "tenth" must not be an eleventh slot.**
Mock 3 inserts a slot before the arrow when "top left" is typed. Measured: strip 434 px = 488 dp, title 131 dp. With longer names: "Right two thirds" leaves 71 dp, "Rechte zwei Drittel" 51 dp. It also adds a slot on a keystroke and shifts nine icons sideways, which `design-system.md` §6 line 4 forbids ("a verb never adds or re-keys one").
Fix: there are never more than ten slots. The tenth slot is "the rest": a chevron at rest, and when the typed verb names one of the rest it *is* that action (its glyph cross-fades in over 80 ms, its name unrolls, the pane is on it). Widest case 465 dp (EN), 485 (DE): title 151 / 131.

**M3. The arrow has no defined states and uses a forbidden ink.**
Mock: chevron at ink 0.60 on the pill ("never 0.60" on the pill, §2), other icons at 0.85 (not a token). No armed look is drawn, and since hovering a slot arms it, "arming past the ninth opens the row" would open a 336 dp list on pointer travel.
Fix: the arrow is a slot like the others: 18 dp chevron, ink 1.0, same pitch, last. Armed it reads "⌄ More ⏎" (36 dp name). Enter, click, → or Tab from there opens. Hover arms, never opens. Its icon centre is x = 684, which is also where an Enter mark sits in a pane that ends at the margin: call it the arrow column.

**M4. The opened rows are off the grid.** Measured in mock 2 (px → dp):
- the selected sub-row's pill starts at 51 px (57 dp); ordinary pills start at 7 px (8 dp). 57 is 2 px right of the parent icon's edge and 10 px left of the title: it is on no line;
- three left edges: icons 17 px, titles and sub-row discs 61 px, sub-row names 101 px (114 dp, a line that exists nowhere else in the panel);
- sub-row disc 28 px (31.5 dp; rows have 36), name 14.5 px (16.3 sp; the scale has 17 and 14), row 42 px (47 dp; not a token);
- the pane on the selected sub-row holds only an Enter mark (35.5 × 32 px): an oval button.
Fix: sub-rows sit on the panel's two existing lines (glyph centred at 38, name from 72), the pill keeps its edges (8 to 712), they speak in the strip's voice (bare 18 dp glyph, `LABEL` 14/500), 40 dp high. Spec and grid in 5.2.

**M5. A strip on an unselected row.** With the list open the mock keeps nine icons and a chevron on Chrome's row, on bare glass, while the pill is on "Top left". In 1.1 the strip belongs to the selection; every other unselected row shows a kind label. Two sets of action marks at once is the "two highlights" problem in another form, and nine full-ink icons on glass compete with the pill.
Fix: while its list is open and the pill is on a sub-row, the parent shows one thing at its right end: the chevron, turned up, at x = 684, ink 0.80 (where its kind label would be). The strip comes back when the pill does.

**M6. The opened list does not fit the list's budget, and nothing says what gives way.**
The list is capped at eight rows = 448 dp (`Metrics.maxRows`). As drawn: 56 + 8 × 47 + 56 = 488. With 5.2's values: 56 + 7 × 40 + 8 + 40 + 8 = 392, plus one 56 dp result = **448 exactly**. Spec: the list never exceeds 448 dp; results after the block leave first (lowest first, fade 80), then rows above the parent from the top (the parent rises on `place`); they return on close. `Metrics.height` must count the block or the window clips it.

**M7. One pane, three contents.** Ordinary rows: icon, name, mark. Key rows: icon and mark, no name (59.5 px). Sub-rows: mark only. A pane without a name is a new, unexplained state.
Fix: a pane always holds icon, name, mark. Key rows get a short name (≤ 70 dp; "Keyboard shortcuts" is 136 and does not fit). Sub-rows get no pane at all: the row is the action, the pill is its highlight, a bare Enter mark sits in the arrow column.

**M8. Half and third cannot be told apart at device size.**
On the HP an 18 dp icon is 20.25 px. Proposed `p3l` fills 7.5 of 20 units, `left` fills 10: 6.3 px against 8.4 px, a 2.1 px difference between neighbours in the same strip (see the rendered strip: slots 3 and 5 read as the same icon). Cause: `left` / `right` are halves of the outer rectangle, the proposed thirds are thirds of the inner one plus the frame. Corrected paths in 5.8: thirds on the outer boundaries (8.5 and 15.5) and drawn as three columns, so a third is a different species from a half.

**M9. "New" pills, the yellow dot, the orange "No key yet".**
`#FFC94D` and `#A2500A` are fixed hues outside the scheme (the app is Material You), used as decoration next to the one coloured thing; "New" is 11 px / 700 (not on the scale) and goes stale the day 2.1 ships. The design system's own rule is that a state is a shape, not a colour.
Fix: no "New" anywhere in the app (it belongs in release notes). The "No key yet" dot is 8 dp of `onSurface`; the words are `word` 14/600 in strong ink.

**M10. The window draws two identical highlights and two divider lines.**
The column's highlight and the page's pill are both the pill recipe: two lilac pills side by side on one ground, and nothing says which has the keys. The column has a `border-right` and the caption a `border-bottom`: an L-shaped frame, on a surface whose rule is "no boxes, one ground".
Fix: the column's highlight is the quiet pane 1.1 already uses for choices on a row that can itself be selected (`OptionStrip(quiet = true)`): `surfaceContainerLowest` 0.62 / 0.36, white rim, ink hairline. The page keeps the one coloured pill. No divider, no caption line.

**M11. The example chips cannot be built as drawn.** Mono face (the device has none: `Bodies.kt`, `MonoText`), 12.5 px, a fill and a white rim: a new kind of box that means neither key nor scope.
Fix: an example is plain text in the panel's face at strong ink ("typed = ink 1.0" is already the system's rule), right-aligned in one column. Spec in 5.6.

**M12. The pinned windows have a home-made title bar, no Copy, and three of four forms.**
A 26 px bar with a kind word and "✕" is drawn inside the window. The system draws a caption on an ordinary window, so there would be two bars. The spec says "one Copy button where there is something to copy" and "its four forms"; the mock draws no Copy and three forms, in a mono face.
Fix: no bar of our own; content starts under the system caption; Copy is an action strip of one at the bottom right; a colour's forms are a vertical option strip. Spec in 5.7.

**M13. The published mock-up misrepresents the window.** `.pg { display: flex }` overrides the `hidden` attribute, so all seven pages render stacked: five pills at once, and each page title touches the row above it (Tips row bottom 671.6 px = "Commands" top 671.6 px). Fix before the page goes back to Alex.

## 3. Should fix

**S1. Key caps jump between rows.** Selected row: caps end 13.5 dp before the pane; unselected: caps end at the margin. Measured shift 71.5 px = 80 dp. 1.1's level rows do the same (number, 16 dp, strip), so keep it, but say so: 16 dp step, the caps slide on `place` with the strip's arrival, and a held arrow shows no strip so nothing slides. Caps in the panel are the panel's `Keycap` (no rim; the mock draws the window's rimmed cap, which in dark reads as a row of outlined boxes).

**S2. Other apps' rows say the app three times.** Icon (Keep), subtitle "Keep", kind "Command". 1.1's kind label already names the source: `kind_command` is "Booklight", the Gemini row says "Gemini". Make the kind label the app's name and drop the subtitle: single-line rows, titles on one baseline (mock: single-line title 15 px from the row top, two-line 7.5 px).

**S3. To-do rows.** Put the date at the right as the kind label ("Yesterday") and keep the row to one line. The tick box needs a spec (5.5); the mock's 6 px radius and 0.70 ink are not tokens.

**S4. Prompt rows ignore §3g.** A prompt is "Ask Gemini" with a preface: build it as the 92 dp preview (Gemini's own icon, the prompt's name as title, the text at ink 1.0 on two lines, pane "Ask ⏎"), not a 56 dp row with the typed text demoted to a second-ink subtitle. The chip "Fix spelling and grammar" is about 180 dp: over the design system's 160 dp cap (the code allows 220 and ends in an ellipsis). Settle one cap and fade it.

**S5. Tip card drawn off 1.1's card.** Mark 36 px in a mock where rows have 32 (centre 37 px against 33 for the G above it); text edge 69 px against 61. "Try it" is drawn as glass on glass; on glass the highlight is the pill recipe (`CardBody` already does this). The inline example is 600 inside 500: use ink, and put the example where typing happens (5.4).

**S6. Three text edges in the window.** Rows without a mark start at 253 px, rows with an app icon at 301, rows with a symbol at 305; app icons are 32 px, marks 36. On the Results page give every row a mark and make app icons 36: one edge.

**S7. Window layout.** Column 216 + page 640 hug the left of a 1024 window and leave 143 px empty at the right. Centre column and page as a group (the design system already says so). Collapse to icons under **840 dp**, not 720: at 720 the page would be 448 dp and the four-option strips collide with their titles. Page title 30.4 px and column label weight 550 are not on the scale (the font is loaded at 400, 500, 600, 700 only); the first column item and the page title are 3.9 px off centre.

**S8. Pins.** Timer: figure, then caption. Answer: caption, then figure. The panel's rows put the caption first; do that in both. The bar label starts at 10 px and the content at 16: one left edge (20).

**S9. `design-system.md` now contradicts the design.** §2 widths (strip ≤ 340, title ≥ 240), §3a ("⋯ 4", Tab never rests on it, the second line at 96 dp), §6 lines 2 and 3, and its type table (label 13, hint 12; the code is 14 and 13). Rewrite those lines in the same change, or the builder has two specs.

**S10. Uninstall.** Last, 8 dp apart from the others, `error` ink at rest. Under the pill, the pill takes the armed-destructive recipe the pane already has (`errorContainer` 0.85 / 0.70, `onErrorContainer` ink): one rule at both levels. Check on a device that a 704 × 40 dp bar of it is not too loud; the fallback is the ordinary pill with `error` ink.

**S11. Mock values not to copy:** pane names at 600 (the app is 500 in every state), footer ink 0.70 (0.80), rimmed caps in the footer, radius 28 on the panel (32), the chosen option of a setting drawn as a key cap (it is the quiet option strip with a check).

## 4. Consider

- "Booklight" appears three times within 100 dp at the window's top left (caption, first column item, page title). Name the first section "Start".
- `Page.kt` `Cap` sets 12.5 sp; the scale has 13. The first-run card's mark is `primary` on a `primary` 0.14 disc, the tip's is the neutral row disc: make both neutral, so cards and rows share one disc.
- `full` is a solid 20 × 16 slab, the heaviest mark in the strip and close to the `app` fallback square. It is the family's logic (solid = where the window goes); leave it unless it reads as "missing icon" on the device.
- In the `k` scope every row carries the same key disc. Harmless (settings rows do the same), but the mark column carries no information there.
- "tab Settings" in the footer while the selected row's strip also answers to Tab: one for the usage review, but the footer is the row's hint line in 1.1.
- An example could show its keyword as a key cap and the rest as text ("[timer] 10m tea"): the cap already means "keyword" on the Yours page.
- Six of the nine icons share one silhouette. If the strip still reads as a blur on the device, 4 dp of extra space between the groups (open, new window | halves | thirds, full | info | arrow) costs 16 dp of title.

## 5. Component specs for 2.0

### 5.1 App row: nine icons and the arrow

```
slot at rest   32 × 32 dp  = 7 · icon 18 · 7          pointer target 32 × 40
armed slot     air · 9 · icon 18 · 7 · name · 6 · mark 14 · 9 · air     air = 4 (none at the strip's ends)
strip          flush right at x = 700; ten slots at most, never eleven
order          Open · New window · Left half · Right half · Left third · Middle third · Right third · Full · App info · [arrow]
```

- Icons 18 dp, ink 1.0 on the pill (never 0.85, never 0.60). Names `LABEL` 14/500/0.1, weight the same in every state. Pane and rim as 1.1.
- **Arrow slot**: chevron-down (5.8), 18 dp, ink 1.0, icon centre x = 684. Armed: "More ⏎" / "Mehr ⏎". List open and the parent selected: chevron-up, "Fewer ⏎" / "Weniger ⏎"; the glyph cross-fades 80 ms.
- **Typed extra** ("chrome top left"): the arrow slot shows that action's glyph and name, armed; chevron out, glyph in, 80 ms; the pane's left edge springs. An action among the nine is armed where it stands. Uninstall typed: same slot, `error` → `errorContainer` pane.
- **Title**: box fixed per row = 616 − the widest state of that row's strip (EN 170, DE 148; with a typed extra 151 / 131). One line, 24 dp alpha fade at the end, no ellipsis. If the box would be under 120 dp, move the last icon before the arrow into the list and measure again.
- Rows with fewer than ten actions keep the same slot. Everything is measured once per row, before anything moves.

### 5.2 The opened list

```
x:   0   8    20      38      56   72                                              684   700  712 720
     |   |    |       ·       |    |                                                ·     |    |   |
         ╭─────────────────────────────────────────────────────────────────────────────────────╮
 56      │    [ app icon 36 ]      Chrome               ↗ ▣ ◧ ◨ ▮▯▯ ▯▮▯ ▯▯▮ ■ ⓘ    ⌃         │   parent, selected
         ╰─────────────────────────────────────────────────────────────────────────────────────╯
 40                   ◧            Left two thirds
 40                   ◨            Right two thirds
         ╭─────────────────────────────────────────────────────────────────────────────────────╮
 40      │            ◰            Top left                                         ⏎          │   the list's one pill
         ╰─────────────────────────────────────────────────────────────────────────────────────╯
 40                   ◳            Top right
 40                   ◱            Bottom left
 40                   ◲            Bottom right
 40                   ▣            Centre
  8      (space)
 40                 (bin)          Uninstall                                                       error ink
  8      (space)
 56           ( disc 36 )          Search Google for “chr”                                Web
```

- **Lines.** Glyph centred on x = 38 (the icon column's centre), name from x = 72 (the title's text edge). No indent, no third edge. The pill keeps its edges, 8 to 712; on a 40 dp row its 24 dp radius clamps to a stadium by itself.
- **Row** 40 dp (the pointer-target height the system already uses). Glyph 18 dp, bare: no disc. Name `LABEL` 14/500/0.1. Ink 0.80 at rest, 1.0 under the pill (glyph and name together, fade 120). No kind label, no subtitle.
- **Why nobody takes them for results:** results have a 36 dp disc or app icon, a 17 sp title and a kind label; these have a bare 18 dp glyph, a 14 sp name at second ink and a 40 dp pitch. They are the strip's slots, one per line.
- **Selected sub-row:** the pill, and an Enter mark (14 dp, ink 1.0) centred on x = 684. No pane.
- **Parent while the pill is on a sub-row:** app icon, title, and the chevron-up at x = 684, ink 0.80. No strip.
- **Uninstall:** last, 8 dp of space above it. `error` ink at rest. Under the pill: the pill's fill goes to `errorContainer` 0.85 / 0.70 and the ink to `onErrorContainer`, 120 ms, driven by how far the pill is on the row (as the pane's `danger` term). Arrows never run it; Android confirms.
- **Separation from results:** 8 dp of space after the block and the return of the 56 dp pitch. No line.
- **Height:** block = 7 × 40 + 8 + 40 + 8 = 336. List ≤ 448 (M6). Footer: `←` Fewer · `esc` Close.

### 5.3 Rows with key caps

- **Cap** (the panel's `Keycap`, with a strong variant): height 24, radius 7, min width 24, padding 7, fill `onSurface` 0.10 / 0.14, no rim. Text `HINT` 13/500/0.1, ink 1.0 (it is the row's answer; footer caps stay 22 dp at 0.80). Arrow, Enter and Backspace keys as the 13 dp drawn marks.
- **Plus:** "+" in `HINT`, the row's `dim` ink (0.80, 1.0 when selected), 4 dp either side. Kept: the system's shortcuts are chords and the window writes them the same way.
- **Place:** the group's right edge at x = 700 on unselected rows (where kind labels end). On the selected row: group, 16 dp, strip; the group slides with the strip's arrival on `place`. Centred vertically with the 32 dp pane.
- **Strip:** two slots; the armed one named, the name ≤ 70 dp ("Shortcuts" / "Kurzbefehle"), then Copy.
- **Long combinations:** caps never shrink or wrap. Four keys measure 211 dp (EN), 244 (DE, with "Umschalt"); with a 169 dp strip (two slots, a 70 dp name) the selected row leaves the title 220 / 187 dp, the unselected 405 / 372. The title fades over 24 dp. If it would fall under 160 dp, the strip shows its armed slot only.
- Mark: the `key` symbol on the row disc, as drawn.

### 5.4 Tip card

`CardBody` unchanged in measure: 96 dp high, content from x = 20.

- **Mark:** the feature's own row mark: 36 dp disc, ink 0.08 / 0.12, 20 dp glyph at ink 1.0, centred on x = 38. Not `primary`.
- **Text**, from x = 72, 14 dp before the options: name 15/600 strong ink, one line; then 13/500, line 17, ink 0.80, two lines at most.
- **The example** inside that line: ink 1.0, same size and weight, no quotation marks, no mono, no fill.
- **The same example is the field's placeholder** while the card shows (`field` 24/400, third ink, from x = 72): it reads as "type this" because it stands where typing happens, in the style that already means "not typed yet". "Try it ⏎" turns it into typed text in place; typing anything removes card and placeholder in the same frame.
- **Answers:** the vertical option strip as it is: two 32 dp slots 4 dp apart, equal width (EN 96, DE about 142), flush right at x = 700, names 14/600, pill-recipe highlight, Enter mark in the chosen slot. First: "Try it"; second: "No tips".

### 5.5 Other apps, to-do, `?`, prompts

All on 1.1's row: icon column 36 centred on 38, title 17/500 from 72, kind label `SMALL` 14/500 at 0.80 ending at 700, strip in its place when selected.

- **Other apps' command:** the app's icon (36), title = the command, kind label = the app's name (cap 160 dp, fade). One line.
- **To-do:** in the icon column a 20 dp box, radius 7, stroke 1.5 dp in the row's `dim` ink, no fill, no disc. Title = the task; kind label = its date. Strip: "Done ⏎", Copy. On Enter the box fills with ink 1.0 and a 14 dp check in the ground's colour (`pop`), then the row folds away (§6 line 16).
- **`?` rows:** the feature's symbol on the row disc; title = what it is; second line = the example in `SMALL` at ink 1.0 (an example is typed text); pane "Type ⏎" (no ellipsis: nothing further is asked). The last row ("All commands") carries the Booklight mark and the `open` icon in its pane.
- **Prompt:** §3g's 92 dp preview: Gemini's icon, the prompt's name in `title`, the text in `SMALL` at ink 1.0 on two lines (the clipboard's text at 0.80 while nothing is typed: guessed), pane "Ask ⏎", then Copy. Chip label capped and faded (S4).

### 5.6 The Booklight window

Ground `surfaceContainerHigh`, one sheet, running under the system's caption. Nothing of ours in the caption, no line under it.

```
W ≥ 1008        [ column 200 ] 40 [ page 720 ]          centred as a group
840 ≤ W < 1008  24 [ column 200 ] 24 [ page W − 272 ] 24
W < 840         16 [ column 48 ] 16 [ page W − 96 ] 16
top             24 dp under the caption inset, column and page alike; the column does not scroll
```

- **Column item:** 48 dp high, 200 wide: 14 · icon 20 · 14 · name · mark home. The icon's centre is 24 dp in, the centre of the highlight's end cap. Name `scope` 15/600, ink 0.80, 1.0 when current; the weight never changes. Items abut.
- **Highlight:** one quiet pane for the column, 200 × 48, radius 24: `surfaceContainerLowest` 0.62 / 0.36, white rim 1 px 0.55 / 0.30, ink hairline outside. Its upper and lower edges travel on `lead` and `trail` (the vertical `OptionStrip`).
- **Keys in the column** (after Left): the pane gains the 2 dp `onSurface` focus ring and the page's pill hides; Right or Enter gives the pill back to the page.
- **"No key yet":** an 8 dp dot of `onSurface`, centred 20 dp from the item's right edge. Collapsed: centred at (36, 14) in the 48 dp item. It scales out on `pop` once a key has opened the panel.
- **Collapsed:** the item is 48 × 48, the pane a circle, the icon does not move (its centre stays at 24); names fade 80 ms; the pane's right edge travels on `trail`.
- **Page title:** display 36/700 rounded in a 48 dp line box, so its centre line is the first item's. Same place and size on every page; it cross-fades in place when the section changes. Lead 18/400, line 26, ink 0.80, 24 dp below it. Start at the 12 dp gutter.
- **Group label** (Your key, Open, Find, Other apps): 12/600/0.5 upper case, ink 0.80 (the slot label of `Bodies.kt`), gutter 12, 28 dp above, 8 below.
- **Rows:** `PageRow` as it is: 56 dp minimum, radius 24, mark box 36 centred 30 dp from the row's left, text from 64, control ending 12 dp from the right. One pill for the page.
- **Your key:** dot 8 dp + 8 + "No key yet" in `word` 14/600, strong ink, at the gutter. Then the caps and the row as 1.1.
- **Commands page:** mark, title, one line under it. The example at the right in `SMALL` 14/500, ink 1.0, its right edge 32 dp from the row's right, at most 240 dp with a 24 dp fade, 16 dp clear of the title. On the selected row an Enter mark (14 dp) appears in the 20 dp kept free after it, so the column of examples never shifts. No chip, no "New".
- **Results page:** every row has a mark (gear, calc, spark, key, app glyph), so all text starts at 64. Per-app rows: the app's icon at 36 in the mark box, name, its commands in one faded line, `FlatSwitch` 44 × 24. With "Commands from your apps" off: those rows at ink 0.40 and the pill skips them.
- **Page change:** rows rise 12 dp and fade in, 22 ms apart, as sections do today.

### 5.7 Pinned windows

Ordinary window, ground `surfaceContainerHigh`, the theme's light or dark. The system's caption is the only bar; set the task's label to the kind. Content from 8 dp under the caption inset; left and right margin 20; bottom 16. One left edge, x = 20. Sizes are content sizes (add the caption); set them with launch bounds per kind.

| Kind | Width | Content, top to bottom | Height |
| --- | --- | --- | --- |
| Countdown | 280 | caption `SMALL` 0.80 ("tea · rings at 14:12") 20 · 2 · figure `answer` 34/600 rounded, tabular, 42 | 88 |
| Answer | 280 | the sum, `SMALL` 0.80, 20 · 2 · answer 34/600 (26 above 12 characters, then fade) 42 · 8 · action line 32 | 128 |
| Note | 320 | text `title` 17/500, line 24, ink 1.0, six lines at most, then a 24 dp fade · 12 · action line 32 | 68 + 24 n |
| Colour | 280 | swatch 52, radius 16, 1 px ring ink 0.20, at x = 20, centred on the strip · from x = 88 the four forms as a vertical option strip: 32 dp slots, 4 apart | 164 |
| QR | 216 | plate 176 (white in both themes, radius 12, code 152) · 12 · action line 32 | 244 |

- **Copy:** an action strip of one at the bottom right, its right edge at W − 20: copy glyph 18, "Copy" in `LABEL`, Enter mark. Pill-recipe highlight while the window has focus or the pointer is on it; otherwise the name at ink 0.80 and no highlight, so an unfocused pin is colourless. Enter and click copy.
- **Feedback:** "✓ Copied" (16 dp check, `word` 14/600, strong ink) at x = 20 on the action line, as the footer does.
- **Colour:** no separate Copy. The option strip is it: the chosen form has the highlight and the mark, Up and Down move it, Enter copies. Forms in `SMALL` with `tnum, zero`; no mono face.
- **Countdown:** nothing to copy, no action line.
- Dark: same recipe; the plate stays white, the swatch keeps its ring.

### 5.8 Icons

24-unit grid, one even-odd path each, built on the existing frame
`W = M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z`.
Solid is where the window goes. Halves split the outer rectangle at 12; thirds split it at 8.5 and 15.5; a third shows both divisions, so it cannot be read as a half.

| Name | Path | Change from the proposal |
| --- | --- | --- |
| `third_left` | `W` + `M8.5 6v12h5V6zM15.5 6v12H20V6z` | was a single hole from 9.5; now 6.5 solid, two empty columns |
| `third_middle` | `W` + `M4 6v12h4.5V6zM15.5 6v12H20V6z` | holes 4.5 wide (were 5), solid 7 |
| `third_right` | `W` + `M4 6v12h4.5V6zM10.5 6v12h5V6z` | mirror of the left |
| `two_thirds_left` | `W` + `M15.5 6v12H20V6z` | boundary 15.5 (was 14.5) |
| `two_thirds_right` | `W` + `M4 6v12h4.5V6z` | boundary 8.5 (was 9.5) |
| `top_left` | `W` + `M12 6v6H4v6h16V6z` | as proposed |
| `top_right` | `W` + `M4 6v12h16v-6h-8V6z` | as proposed |
| `bottom_left` | `W` + `M4 6v6h8v6h8V6z` | as proposed |
| `bottom_right` | `W` + `M4 6v12h8v-6h8V6z` | as proposed |
| `centre` | `W` + `M4 6v12h16V6zM8 9h8v6H8z` | as proposed |
| `full` | `W` | exists in `Icons.kt` |
| `more` | `M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6z` | as proposed |
| `less` | `M7.41 15.41 12 10.83l4.59 4.58L18 14l-6-6-6 6z` | as proposed |

At the HP's 20.25 px the smallest hole is 3.8 px and the divider 1.7 px (rendered and checked). The unused `third` path in the page's script goes. The quarters are 10 × 8 solid, consistent with the halves.

### 5.9 Rules that must hold in the build

- One coloured highlight per surface: the list's pill in the panel, the page's pill in the window, the strip of one in a focused pin. The column's highlight is glass, not colour.
- No fill-and-rim boxes inside the glass other than the pane, the scope chip and key caps. No divider lines anywhere.
- Colour only for the pill and for `error`. No fixed hues.
- Type only from: 36/700 r · 34/600 r · 24/500 · 17/500 · 15/600 · 14/600 · 14/500 · 13/500 · 12/600 caps.
- Ink 1.0, 0.80, 0.60 on glass; 1.0 on the pill for anything that acts.

## Check on a device before locking

1. Nine icons at a 32 dp pitch on the pill: a strip, or a blur? (Fallback: seven icons and the arrow at 36.)
2. The corrected thirds at 18 dp on the HP (1.125 px per dp).
3. `errorContainer` as the whole pill on Uninstall.
4. The pinned window: the system caption's height, the smallest size the system grants, and whether the caption can be drawn over the app's ground.
