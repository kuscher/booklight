# Booklight: design system and motion specification for 1.1 to 2.2

*1 October 2026. Desk work from the 1.0 code, captures, design pass, review and UX model; no device was touched, so every alpha and spring is a starting value (§9). "Ink" is the ground's on-colour (`onSurface` on glass, `onSecondaryContainer` on the pill) at an alpha. L / D = light / dark.*

## 1. Principles

1. **One sheet of flat glass, one coloured thing.** Veil, white outline, black hairline; the selection pill is the only chromatic surface.
2. **A highlight is the opposite material of its ground:** colour on glass (the pill), glass on colour (the armed chip). One per level, and it travels; never drawn twice, never faded between two places.
3. **Sizes are settled before anything moves.** Every slot reserves its final size; motion changes edges, offsets and alpha, never a text measurement.
4. **Things open from where they are:** the panel from a seam, a chip from the typed keyword, a track from its number. Leading edge first.
5. **Typing owns the frame.** Everything retargets from where it is, nothing restarts, nothing waits; with animations off everything cuts.

## 2. Tokens

| Group | Token | Value |
| --- | --- | --- |
| Spacing | steps | 2 · 4 · 6 · 8 · 12 · 16 · 20 · 24 dp; the Booklight window adds 32 · 48 · 64 |
| Grid | as 1.0 | panel 720; list pad 8; margin 20; marks centred at 38; text from 72 |
| Radius | panel · row, pill · cell highlight · swatch · QR plate · key cap | 32 · 24 · 14 · 16 · 12 · 7 dp; chips and tracks are stadiums; the seam is min(32, w/2, h/2) |
| Height | field · row · tall · open row · picture · grid · card · footer | 68 · 56 · 92 · 96 · 208 · **256** · 96 · 36 (+ 8) dp |
| | action chip · scope chip · card option · key cap · cell | 28 · 36 · 32 · 22 · 48 dp |
| Width | strip ≤ 340 · title ≥ 240 · label pitch 40 · track 200 · number slot 64 · state slot 120 · switch 44 × 24 · swatch 52 · plate 176 | |
| Type | `field` | Sans Flex 24 sp / 500 / 0; placeholder 400, third ink |
| | `title` | 17 / 500 / 0: row titles, slot values |
| | `label` | 13 / 500 / 0.1: kind, subtitle, action chips, captions, cell name, state text |
| | `hint` | 12 / 500 / 0.1: footer hints, key caps, slot labels |
| | `scope` · `word` | 15 / 600 / 0: scope chip · 14 / 600 / 0.1: feedback, card options |
| | `answer` | rounded cut 34 / 600: sums, timer |
| | `mono` | 24 / 500 (20 if it doesn't fit; colour values 26) |
| | glyphs | emoji 28 sp, symbols 22 sp, row symbol 20 dp, Enter mark 14 dp |
| Ink | strong · second · third | 1.0 · 0.80 · 0.60. On the pill: 1.0 for content and actions, 0.80 only for slot labels and guessed values, never 0.60 |
| Fill | veil | `surfaceContainerLowest` 0.42 L / 0.50 D (Clear 0.28 / 0.38, Frosted 0.58 / 0.64) |
| | solid ground (no blur; the Booklight window) | `surfaceContainerHigh` |
| | pill | `secondaryContainer` 0.78 L / 0.42 D on glass, 0.66 D on solid ground (the Booklight window, glass set to Solid). Red: `errorContainer` 0.85 L / 0.50 D on glass, 0.70 D on solid ground (§12) |
| | pane (armed chip on the pill) | `surfaceContainerLowest` 0.62 / 0.36 |
| | scope chip, key cap · symbol disc | `onSurface` 0.10 / 0.14 · ink 0.08 / 0.12 |
| | track at rest · fill, thumb, tick, switch on | ink 0.20 / 0.24 · ink 1.0 |
| | pressed | ink 0.08 over the pressed slot, 80 ms in, 120 out |
| | destructive | `error` ink at rest; `errorContainer` with `onErrorContainer` when armed |
| | QR plate | white in both themes, black modules |
| Outline | panel | white 1.25 dp at 0.80 / 0.44; black hairline 1 px outside at 0.20 / 0.28 |
| | pill, pane, scope chip | white 1 px at 0.55 / 0.30; the pane adds an ink hairline outside, 1 px at 0.20 |
| | hollow control · swatch ring · window focus ring | 1.5 dp ink 0.40 · 1 px ink 0.20 · 2 dp `onSurface` |

**Changes from 1.0**

- The filled Enter key cap becomes the **pane**: the cap grown around its label, the bare mark inside.
- The site chip at the field's right becomes the scope chip at its left, neutral, not `secondaryContainer`: one coloured thing.
- The feedback word goes from `primary` (a fixed mid-tone, 1.6:1 over a terminal) to strong ink with a check glyph.
- Row titles end in a 24 dp alpha fade, not an ellipsis: a change of width never swaps glyphs.
- Card buttons lose their own fills and the 500/600 weight swap; both changed widths.
- The pill gains left and right edges and an animated radius (the grid). The selected row's hint slides on `place`, not `pop`.
- Against the UX model: the grid is 256 dp, not 208 (five 48 dp rows need 240 + 16); unarmed chips are full ink, not 0.80 (4.05:1 on the pill).

## 3. Components

### a. Option strip

**Anatomy.** A line of slots, flush right at the 20 dp margin, centred vertically in 56 and 92 dp rows, in the top 56 dp band of taller ones. A slot is 28 dp high: 12 · label · 6 · mark home 14 · 8. Slots abut, so labels sit 40 dp apart and the space between two labels *is* the mark's home. "More" is 12 · "⋯ 4" · 12 at second ink, without a mark home: Tab never rests on it.

**The highlight** is one rounded rectangle per strip, the size of the armed slot, over two layers:

- the base layer: every label in its rest ink;
- the armed layer: every label in its armed ink and an Enter mark in every slot, **clipped to the highlight**.

The mark is printed in every slot; the highlight is the window that shows one. No chip has a background, nothing fades, no width changes. Labels are `label` at 500 in every state.

| State | Fill | Ink | Mark |
| --- | --- | --- | --- |
| Rest, on the pill | none | ink 1.0 | hidden |
| Rest, on glass (card) | none | `onSurface` 0.80 | hidden |
| Armed, on the pill | pane and its two rings | ink 1.0 | shown |
| Armed, on glass | pill recipe | `onSecondaryContainer` | shown |
| Pressed | adds ink 0.08 | – | – |
| Disabled | none | ink 0.40; Tab skips it | never |
| Destructive: rest, armed | none, `errorContainer` | `error`, `onErrorContainer` | hidden, shown |
| Confirming | `errorContainer`, widened (f) | `onErrorContainer` | shown |

Roles are the same in both themes; alphas as §2.

- **On the pill.** The pane is a small sheet of the panel's own veil: lighter than the pill in light theme, darker in dark, so it raises the label's contrast in both and adds no hue. Every selected row has one ("Open ⏎").
- **With the title.** The strip is measured once per row; the title takes the rest. Chips that don't fit in 340 dp go to "more".
- **Ownership.** The strip belongs to the selection, not the row. When the pill moves, the old strip fades and the new one arrives 60 ms after the pill's target changes, cancelled if it changes again: a held arrow key shows no strips. When the row under the pill changes during typing, the strip stays; differing labels cross-fade (80 ms) and the pane's left edge springs.
- **Second line.** The row grows to 96 dp; a 40 dp band holds the other slots, flush right, centred at y = 76. The destructive slot is last, after 12 dp of extra space. "⋯ 4" keeps its place; its glyph cross-fades to a chevron.
- **First-run card.** The same strip, vertical: two 32 dp slots 4 dp apart, both as wide as the wider label plus the mark home, flush right, `word`. One highlight in the pill recipe.
- **Narrow:** the title keeps 240 dp; under a 560 dp panel only the armed chip and "more" show.
- **Pointer.** Hovering a chip (on movement) arms it.

### b. Scope chip

A 36 dp stadium at the left margin: 8 · icon 20 dp (centred at 38, where the G was) · 6 · `scope` label · 14; the argument starts 12 dp after it. Fill `onSurface` 0.10 / 0.14, white 1 px outline, strong ink. Unlike an action chip it sits on glass, has an icon and an outline, never holds the Enter mark and never glides. Label at most 160 dp, fading at its end (280 dp for text sent from another app); under 560 dp only the icon remains.

Placeholder in a scope: `field` at 400, third ink, naming the argument ("Search YouTube", "to  subject / message").

### c. Preview row (92 dp)

The scope's symbol disc, then three lines from x = 72: a caption (`label`: "Add to Notes.md · 14:02"), the slot line (24 dp), a body line (`label`: the message, the place).

- **Fixed columns per scope,** so a growing value never pushes its neighbour. Mail: To 220 dp, Subject the rest. Event: When 220, Title the rest, Place 160. A slot is its `hint` label, 6 dp, its `title` value.
- **Typed:** ink 1.0. **Guessed:** 0.80. **Empty:** the label and a 20 × 2 dp rule at ink 0.30; no grey example text.
- **Under the caret:** one 2 dp underline at ink 1.0 beneath the value, at least 20 dp wide: the row's own small highlight.
- **Long text** fades over its last 24 dp; ` / ` shows as "↵" in second ink. The timer's "10:00" is `answer`; a quicklink's address is `label` with the argument at ink 1.0 and the rest at 0.80.

### d. Control rows

Unselected, a control shows its state as a kind label ("40 %"). Selected, the number keeps that exact place and the control appears to its left.

- **Track:** a 200 × 6 dp stadium, 12 dp left of the number slot (tabular figures). **With a thumb:** a flat 4 × 20 dp bar at ink 1.0, a 4 dp gap either side. A typed target is a 2 × 12 dp tick; the number shows the present level until Enter. Muted: fill at 0.40, the slot reads "Muted". Hit area 200 × 32 dp. Narrow: 140 dp.
- **Switch:** an 18 dp thumb in a 44 × 24 stadium, right of the state slot ("On until 15:00"). On: track ink 1.0, thumb in the ground's colour, right. Off: track ink 0.20, thumb ink 1.0, left.
- **Needs a grant:** track and thumb hollow, 1.5 dp at ink 0.40; subtitle at ink 1.0; the pane holds "Allow… ⏎".
- **Media:** a strip of three slots: ⏮ 36 dp, ⏯ with the mark 58 dp, ⏭ 36 dp; 18 dp glyphs; the pane rests on the middle.

### e. Tall rows

- **Grid (256 dp):** 14 × 5 cells of 48 dp, inset 16. The highlight is **the list's own pill**, reshaped to a 44 dp square of radius 14; no slab behind the grid. The cell's name sits in the footer's left slot (`label`, second ink). Narrow: columns = ⌊(width − 48) / 48⌋.
- **Colour (92 dp):** a 52 dp swatch of radius 16, centred at 38, with the ring; the value in `mono` 26.
- **QR (208 dp):** the plate 16 dp from the row's left and top, the code 152 dp inside a 12 dp quiet zone. From x = 220: the text in `title`, up to three lines, then "QR code · 41 characters" (`label`).
- **Password, UUID (92 dp):** `mono`: Google Sans Mono if `/product/fonts` has it (check), else `FontFamily.Monospace`; slashed zero. Size is chosen once per value.

### f. Confirmation

The destructive pane widens leftward from its fixed right edge to hold "Press again to delete" and the mark in its home; both widths are measured first. A 2 dp line in `onErrorContainer`, inset 14 dp from each end and 4 dp above the lower edge, shortens towards the mark over 3 s. Chips it covers go to alpha 0; the row's others to ink 0.40.

### g. Ask Gemini

- **The field never wraps or resizes.** Text wider than the field scrolls so the caret stays 48 dp from the right edge; the overflow at the left fades over 32 dp. The grey completion hides while it overflows; a pasted line break shows as "↵" in third ink.
- **As row one** it is a 92 dp preview: the Gemini app's own icon, "Ask Gemini" in `title`, then the text in `label` at ink 1.0 on two lines, no quotation marks, the second fading over its last 48 dp. The field shows the end of a long text, the row its beginning. Pane: "Ask ⏎". Footer: "Sends this text to the Gemini app". Lower in the list: 56 dp, kind "Gemini".
- **"Search Google for “…”"** stays as 1.0: magnifier disc, the text quoted inside the title, one line, kind "Web". Gemini is an app receiving a message; Google is a search for a term.

### h. Footer and feedback

Left: a feedback word, else a context line. Right: at most two hints and `esc`. Key caps never move; only labels cross-fade.

| State | Left | Right |
| --- | --- | --- |
| Results | – | `tab` Actions (if several) · `esc` Close |
| Scope row selected | – | `tab` Fill in · `esc` Close |
| In a scope, argument empty | – | `⌫` Leave · `esc` Close |
| Control row | – | `←` `→` Adjust · `esc` Close |
| Grid | the cell's name | `⇧⏎` Copy more · `esc` Close |
| QR | "Stays open to scan" | `esc` Close |
| Confirming | – | `esc` Cancel |

Feedback: a 16 dp check and a word in `word`, strong ink: "Copied", "Added to Notes", "Timer started", "Created", "Deleted". Failure has its own glyph, not its own colour.

## 4. The opening: "unfold"

> **Checked on the devices, 1 October** (`docs/research/device-findings.md`, "How the panel can arrive").
> This section was written at the desk and three things in it change: (1) the window is not resized in
> width; it is the panel's final rectangle from the first frame and the glass grows inside it; (2) the blur
> is always the whole window, so its radius stays 0 until the glass is more than half open, and nothing
> overshoots past the final edge; (3) Alex likes the edge light ("the little highlight running around the
> edge is excellent"), so it stays, slower, rather than being retired. Times and the order of events below
> still hold.

A seam of outline grows up and down to the field's height, then the glass opens left and right out of it. The window itself is animated; its content is laid out once at 720 dp, centred on the seam, and never moves on screen.

| Time | Window | Outline, veil, blur, dim | Content |
| --- | --- | --- | --- |
| 0 ms | 6 × 6 dp, radius 3, centred 34 dp below the 20 % line | outline alpha 1.0; veil at its level; blur 0; dim 0 | field focused and taking keys; nothing visible |
| 0 to 80 | height 6 → 68: tween 80, cubic-bezier(0.2, 0, 0, 1); top = centre − h/2 | unchanged | – |
| 50 to 230 | width 6 → 720 on `open` = spring(0.9, 800), clamped; radius min(32, w/2); p = (w − 6) / 714 | outline alpha 1.0 → 0.80 / 0.44, blur and dim 0 → full, linear in p | alpha = smoothstep(0.35, 0.85, p); the edges uncover placeholder, caret, then the mark (p ≈ 0.9) |
| ≈ 160 (p ≥ 0.85) | the body gate opens: height leaves 68 on `place` | – | rows or the card cascade, 22 ms apart |
| ≈ 240 | at rest, top edge at 20 % | – | the caret starts to blink |

- Radius, blur, dim, outline and content alpha are **functions of the window's size**, not animations of their own: they cannot drift out of phase.
- The travelling gleam is retired with unfold on; the seam relaxing from 1.0 to the outline's resting alpha is the arrival's one light event.
- **Chaining.** Results that arrive before p = 0.85 wait for the gate, so width hands over to height while the spring is still settling: seam, field, list, one gesture.
- **Closing mirrors** (since 1.1.1; Alex: "update the animation when it closes to be the inverse from when it opens"). The glass closes to its seam from both sides in 105 ms (cubic-bezier(0.45, 0, 0.4, 1): the opening spring's way to full width, backwards, without the bounce), covering the contents where they stand; from 75 ms the seam draws in for 80 ms (the opening's curve reversed) and its last 40 ms fade. 155 ms in all, times the opening's speed setting. The window is not resized, as in the opening. The blur is the whole window, so on the way out it lets go early (gone at three quarters of the width, the contents at a little over half) or a blurred band stands beside the glass. The key pressed again while it closes turns it round: it opens from where it had got to. With the opening off, closing is 1.0's fade: presence 90 ms, scale to 0.975.
- **If two phases are too long: "bloom".** Height and width start together: a dot becomes the field in about 190 ms.
- **Setting off** ("Opening: Unfold · Fade"): 1.0's arrival: presence 170 ms, scale 0.96 → 1 on `pop`, the gleam. **System animations off:** full size in the first frame.

## 5. The Booklight window

The panel without blur: ground `surfaceContainerHigh` (the panel's own solid fallback), the same grid, rows, pill and controls.

- **Layout.** One scrolling column 720 dp wide (the panel's measure) and a section index: What it is · Your key · Look · Search · Results · Privacy · About.
- **No boxes.** A section is a title and rows on the one ground, 48 dp from the next. Rows are the panel's: 56 dp, mark at 38, text at 72, control flush right. Only the stage is framed.
- **What it is.** "Booklight" in the display style, one sentence, then the **stage**: 720 × 300 dp, radius 32, ground `surfaceContainerLowest`, a flat drawn desk (two window shapes in `primaryContainer` and `tertiaryContainer`, text bars at `onSurface` 0.12). On it runs the real panel composable at 0.75 scale over an in-app blur of the stage, in the user's glass level: a 9 s loop (unfold, "st" typed, rows cascade, the pill moves, the pane glides twice, close) while the window is focused; a still with animations off.
- **Your key.** Two sentences, the suggested keys as key caps (28 dp, 14 sp), and "Open Keyboard shortcuts ⏎" as a strip of one.
- **Controls.** A choice (engine, glass, opening) is an option strip at the row's right end: 32 dp slots, pill-recipe highlight, a check in the mark's home. Switches and tracks are (d)'s. A keyword search is a row with its keyword as a key cap in the mark column and a destructive "Remove" chip that confirms as (f).
- **Selection.** One pill for the page follows the arrow keys and pointer movement; Left and Right change the row's control live; Enter flips or runs. Keyboard focus adds the 2 dp ring.

| Window width | Layout | Display | Lead | Section title |
| --- | --- | --- | --- | --- |
| 900 to 1099 dp | index as a strip under the caption bar; column centred | 36 / 700 rounded | 18 / 400 | 20 / 600 |
| 1100 to 1399 | rail 200 + gap 40 + column, centred as a group | 45 / 700 | 20 / 400 | 22 / 600 |
| 1400 to 1700 | rail 220 + gap 56 + column; stage 960 × 340 | 57 / 700 | 22 / 400 | 24 / 600 |

Row text keeps the panel's sizes at every width (`title`, body 15 / 400, `label`). Below 900 dp the column is the width less 48.

**Motion.** Sections rise 12 dp and fade in on `place`, 22 ms apart, once. The index highlight is a vertical option strip following the scroll; a click scrolls on `place`. Choices, switches and tracks move as §6.

## 6. Motion specification

New specs: `open` = spring(0.9, 800); `drain` = tween(3000, linear); `roll` = 1.0's answer roll (half a line on `place`, fade 120 in, 70 out). **With animations off every line cuts to its end state;** the confirmation line then stays full until its 3 s are up.

| # | Transition | What moves | Spec · delay | What stays still, and why it can't jitter |
| --- | --- | --- | --- | --- |
| 1 | Arm the next chip, same line | The highlight's leading edge goes to the slot's far edge; the other follows | `lead`, `trail` | Labels, marks, title. Two independent edges: unequal widths need no width animation; the clip uncovers the mark |
| 2 | Row opens | Height 56 → 96, the pill's lower edge, rows below, window height. Second-line slots rise 6 dp, fade 110. The highlight moves as one rigid rectangle | `place` (pill edge `lead`) · slots 22 ms apart, left to right | The row's first 56 dp. Across lines the highlight never stretches: it would cover chips it isn't choosing |
| 3 | Row closes | Second line fades 60 together; height returns; the highlight returns rigid | `place` | First line |
| 4 | A typed verb arms a chip | As 1 or 2, from wherever the highlight is | as 1, 2 | The row: a verb never adds or re-keys one |
| 5 | Strip arrives | The whole strip: 12 dp from the right, fade 110; kind label fades out 60 in place | `place` · 60 ms after the pill's target changes | The title's width switches once, behind its fade edge |
| 6 | Enter a scope | (i) A copy of the keyword slides and scales (24 → 15 sp) to the label's place, cross-fading to the full name (70 out, 120 in). (ii) The chip's rectangle grows from the keyword's bounds; fill fades in 110. (iii) The G shrinks to 0.6 and fades (80) while the icon grows from 0.6 on the same centre. (iv) Argument and caret slide to the new start. (v) Placeholder fades in 140, 8 dp from the right | (i), (ii), (iv), (v) `place`; (iii) `pop` · (v) 60 ms | The field's text is replaced in one frame, offset so the argument is exactly where it was; the offset then springs to 0. The pill stays on row one; its content cross-fades (110 / 70) |
| 7 | Leave a scope | 6 reversed: label to keyword, rectangle shrinks to the keyword's bounds and fades (80), text back to 72, the G grows, rows cascade | `place`, `pop` | Baseline, panel top |
| 8 | Preview slots fill | Values mirror the field in the same frame. An empty slot's rule fades (80). A re-parse cross-fades the values concerned (110 / 70). The underline glides | `lead`, `trail` | Columns, labels. Nothing tied to typed text is sprung |
| 9 | A row becomes a control | Track fades in at full length (110); the fill grows from the left; the thumb scales in vertically | fill `place` · 40; thumb `pop` · 90 | The number: it is the kind label, unmoved |
| 10 | A value changes | One animated level drives the fill's end and the thumb; the number rolls | `place`; `roll` | Slot width. Under 120 ms apart the number cuts |
| 11 | A switch flips | Thumb x; track fill cross-fades 120; state text rolls | `pop` | Track, state slot. Enter closes 400 ms later |
| 12 | Media: previous, next | The pane glides to the side slot, holds 140 ms, returns | `lead`, `trail` | Glyphs |
| 13 | The grid arrives | Rows fade (80); height springs; the pill's four edges and radius go to the first cell; cells rise 8 dp, fade 110 | `place` · (column + row) × 8 ms, at most 120 | Field, footer |
| 14 | The grid highlight moves | The two edges on the axis of travel; a move to the next line's start is rigid | `lead`, `trail`; `place` | Cells. The name fades 80 in, 40 out, left-aligned |
| 15 | Confirmation arms | Pane's left edge to its measured width; fill to `errorContainer` (120); label out 60, in 110, clipped by the pane; siblings dim (80); the line shortens | `lead`; `drain` · label 40, line 120 | The right edge and the mark |
| 16 | Cancels, completes | Cancel: edge back, fill back 120, line fades 60. Complete: the row's height goes to 0 and it fades (80); rows below and the window follow; the pill takes the next row | `trail`; `place` | Other rows keep their keys |
| 17 | Feedback word | Check scales from 0.6; the word slides 8 dp from the left, fades 120; out 80 | `pop`; `place` | The footer's right side |
| 18 | First-run card's options | One highlight: upper and lower edges | `lead`, `trail` | Both labels, marks, widths and the weight. No slot has a background or a size that depends on being chosen, so the 1.0 defect cannot recur |
| 19 | Long text in the field | The scroll offset follows the caret | cut | Tied to typing, never animated |
| 20 | Unfold, close | §4 | `open`, tweens | Content position on screen |

## 7. Continuity rules

1. **No animated width that text depends on.** Measure both ends first. The only width changes are a highlight's edges and the confirmation pane.
2. **One owner per property.** A fill belongs to the highlight, never to the chips it visits. Things that must stay together read one animated value: fill and thumb; clip and marks; window size, shader size and outline radius.
3. **Stable keys.** Rows by result id, chips by action id, cells by code point, slots by name; never by index.
4. **No reflow.** One-line field, fixed slot columns, tabular figures in fixed slots, fade edges instead of ellipses; weight never changes with state; type size is chosen once.
5. **Typed text cuts, objects spring.** Anything showing what was just typed updates in the same frame.
6. **Fast typing.** Interruptible: every spring retargets from its present value and velocity. Never restarted: the unfold, a row whose id persists, a strip whose selection has not moved. At most one list change per frame; a list replacing one still cascading drops the pending delays. Rolls and cross-fades cut when changes come under 120 ms apart.
7. **One window write per frame:** width, height, position and outline radius together.

## 8. Accessibility

- **Armed is a shape, not a colour:** the Enter mark exists only inside the highlight. On: thumb position. Needs a grant: hollow. Destructive: last, apart, and worded. Failure: its glyph.
- With the system's high-contrast text on, the pane's ink hairline becomes 1.5 dp at 1.0 and second ink becomes 1.0.
- **Pointer targets:** chip slot width × 40 dp; cell 48 × 48; switch 56 × 40; track 200 × 32; nothing under 32 × 32.
- **Contrast targets,** over a white page and a terminal, both themes: chip labels on the pill 4.5:1 (hence full ink); the armed label on its pane 7:1; `error` on the pill 4.5:1, else strong ink with a warning glyph; slot labels and guessed values 3:1 (they repeat the field).
- Screen reader as the model says; a confirmation also announces its time limit.

## 9. What to cut first, and what to check

**Cut in this order** if the device shows it too busy:
1. The stagger of second-line chips.
2. The grid's diagonal wave (cells fade together).
3. The strip's slide on selection (fade only).
4. The keyword's travel into the scope chip (cross-fade in place; keep the caret's travel).
5. Lead and trail on the chip highlight (both edges on `place`).
6. Unfold's first phase (bloom), then unfold (fade).

The gliding highlight itself is never cut.

**Check on the device before locking values**
1. **Window resize in step with content.** Record the unfold and step through it: content must not shift sideways for a frame, the outline radius must follow, no frame may drop at full blur. If it fails: bloom, then a fixed window with an animated clip.
2. **The pane on the pill** over a white page and a terminal, both themes: visible at 0.62 / 0.36 without reading as a raised button? Chip labels at 4.5:1?
3. **The highlight at key-repeat speed** across chips of unequal width and across the two lines: a glide or a smear? Does the mark uncover cleanly?

## 10. Open decisions

1. **Unfold on by default?** Recommended: yes, with "Fade" as the other setting.
2. **Enter mark inside the armed chip** (labels 40 dp apart), or at the row's right end (labels closer, the mark away from its chip)? Recommended: inside.
3. **Unarmed chips at full ink,** not the model's 0.80, to hold 4.5:1? Recommended: yes.
4. **Long text in the field:** one scrolling line, or a field that grows to three? Recommended: one line.
5. **Retire the arrival gleam** when unfold is on? Recommended: yes.

## 11. What 2.0 changed (1 October 2026)

*§12 replaces what this section says about the opening's spring, the edge light, the nine icons and the pill.*

The specs for 2.0's components and motion are the three reviews in `reviews-2.0/` (visual §5, motion rows 21 to
55); this section says where the built app differs from them and from the sections above. `Motion.kt` is the
source for every spring: `place` 0.86/520, `lead` 0.82/1100, `trail` 0.9/420, `pop` 0.62/700, `arm` 0.78/560,
`open` 0.72/1000 (not the 0.9/800 of §6), `tick` 160 ms, `type` 480 ms for a whole example at 16 to 40 ms a
letter. A debug build stretches all of them: `./bl open stay slow=4`.

- **The app row** (replaces §3a "three chips and more"): nine icons at a 32 dp pitch and a tenth stop, the arrow.
  Enter on the arrow, or Right again, opens the rest as a list under the row: 40 dp lines, uncovered by one edge
  on `place`, the list's own pill as their highlight. A typed place among the rest takes the arrow's slot.
  Store page is gone.
- **The opening** (§4): always at the field's height. What is under the field (a card, a tip, rows for handed
  text) comes once the glass is 85 % open. The closing draws in to the field's centre line.
- **The answer row**: a caption, then two lines; the answer is written in behind a head that follows what
  arrives (14 letters soft), and the row grows once, to four lines, on `place`. The mark and the strip keep
  the line of the row's first height. While the model has said nothing yet the edge light runs a slow lap
  (2.4 s) and finishes it when the first word lands.
- **Key caps in a row**: 24 dp, strong ink, plus signs between; 200 dp kept free at their right for the strip.
- **A task**: a 20 dp box in the mark column; ticked, the check draws itself (one 140 ms stroke, the same mark
  as "Copied" and "Your key works") and a line strikes the title from its start.
- **The tip card**: the first-run card's measure. Nothing armed at rest; a `tab` cap beside the two answers.
  It comes 320 ms after the gate, never as part of the opening.
- **The shadow**: the system's window shadow, cast from the glass as it opens, cleared under the glass.
  Low 72 dp / 0.14, medium 96 dp / 0.24, high 128 dp / 0.36 (height, darkness under the lower edge); darker by
  1.4 in dark theme. `device-findings.md` has the measurements.
- **The window** (replaces §5): a column of seven sections (item 48 dp, 200 wide, quiet pane on `lead` and
  `trail`, 2 dp ring when it has the keys) and a page of 720 dp. The page's pill is only there while the keys
  are in the page. The page follows the pane by 60 ms; its blocks rise from the side the pane came from.
  Column width = window width less 808 dp, between 48 and 200: never animated.
- **The pin**: a picture-in-picture window, 280 × 118 dp and up, the content drawn at its size and scaled as
  one piece when the user makes the window larger. Its arrival is the system's; the content then fades in once.
- **Not as specified:** the keyword does not travel into the chip (it grows where the mark was, as in 1.1); the
  pill returns to a closing row on `lead` and `trail`, not as one rigid piece; a pin has no Copy of its own.

## 12. After 2.1 (October 2026)

Alex's notes on the app as released, taken through a visual designer and a motion designer (three rounds of
recordings on the Lenovo). `Motion.kt` holds every number below. Where this section and §4, §6 or §11 disagree,
this one is the app.

- **The opening is at Medium unless chosen otherwise.** Times at Fast / Medium / Slow:

  | Part | What | Numbers |
  | --- | --- | --- |
  | Seam | grows up and down on the field's centre line | 80 / 160 / 320 ms, cubic-bezier(0.2, 0, 0, 1) |
  | Wait | before the glass starts; the curve's own slow start is the rest of the pause | 30 / 60 / 120 ms |
  | Glass | from the seam to its width on `opens`: slow, fast, and a long way of slowing down. No overshoot | 180 / 360 / 720 ms, cubic-bezier(0.55, 0, 0.1, 1) |
  | Contents | fade in | smooth(0.35, 0.85) of how far the glass is open |
  | The G and the esc cap | fade on their own, last: the glass's edge passes them slowly and would cut them | smooth(0.92, 1.0) |
  | Blur | is the glass's own, from the seam's first frame (see "The blur follows the glass" below) | the level's full radius, times the panel's presence |
  | Under the field | may arrive | at 85 % open, as before |
  | In all | from the key to rest | 210 / 420 / 840 ms |

  At Medium the field is readable 258 ms after the key (the old spring: 257) and nothing rebounds; the old spring
  reached its width and then shrank back 14 dp a side for 240 ms.
- **Leaving** keeps its shape (the glass folds on cubic-bezier(0.45, 0, 0.4, 1), then the seam draws in) but not
  the opening's time: the window keeps the keyboard until it is gone, so Fast and Medium both leave in 155 ms and
  Slow in 310. The key pressed again part of the way opens on a spring without overshoot that takes over the
  speed the glass has (damping 1.0; stiffness 1000 at Fast and Medium, 250 at Slow: as stiff as the leaving is
  quick, or the glass would go on closing long after the key). From the seam it is the curve.
- **The blur follows the glass** (Alex: "why does the blur just pop in ... is there a chance to have it from the
  getgo or fade in during animation timed"). The blur was held back until the glass had all but landed, because
  it was taken to be the whole window's; it came in two to four frames at the end and read as a pop. It is the
  window's root view's, and that view is now framed to the glass before each frame
  (`docs/research/device-findings.md`, "The blur follows the glass"). So:
  - The glass is frosted from the seam on, at the level's full radius. The radius has no curve of its own: it is
    a property of the material, like the veil and the outline, and cannot drift out of step with the width.
  - It stays on the glass through the fold and the turn, and goes with the seam (radius times presence). The
    band of blurred page that stood beside the folding glass for two or three frames is gone.
  - Nothing else moved: contents smooth(0.35, 0.85), the G and the esc cap smooth(0.92, 1), the gate at 0.85. The
    placeholder now comes up over frosted glass.
  - The window still never moves or changes its width. The glass's middle is within half a pixel of the
    panel's in every frame, and the release build draws the whole opening at one frame every 8.3 ms.
  - The desktop itself fades the panel's window in over its first 140 to 200 ms, and a window's blur is as
    strong as the window is opaque. At Medium and Slow that is over as the glass starts to open. At Fast the
    glass, its veil, its outline and its blur come up on that one ramp while it spreads; left so (to open after
    the fade Fast would take 320 ms). Booklight cannot turn that fade off.
  - With the opening turned off, and with Solid glass, nothing changed.
  - §4's "the blur is always the whole window" and its radius kept at 0 no longer hold. Motion review five:
    `docs/design/reviews-after-2.1/motion-5.md`.
- **The seam is seen again.** Since the shadow came (2.0) nothing of the panel showed until the glass began to
  widen: the root view had an empty outline while the shadow's strength was 0. The outline is now the glass's
  shape at alpha 0. The seam grows from its first frame and is seen to draw in at the end.
- **The reflection** (replaces "the edge light" of §4 and §11). One white light runs once round the outline:
  - *When:* 2.4 s after the panel has opened and 0.7 s after the last key of any kind or change of the list,
    both in real time; once per opening; never while leaving; not at all with the system's animations off.
  - *Where:* from the middle of the top edge, clockwise, exactly one lap. Its brightness comes in over the first
    5 % of the lap and goes out over the last 18 %: it dims along the top left and is gone before it stops.
  - *How fast:* 1 ms for each dp of outline (1.6 s round the bare field), at most 2.2 s, on
    cubic-bezier(0.3, 0, 0.5, 1): about 1,700 dp a second at its fastest, the same round a field and a full list.
  - *What it is:* the outline at full white, widened from 1.25 dp to 1.75 (dark) or 2.0 (light), its inner edge
    softened over 3 dp at 12 % (dark) or 16 % (light). A front of 40 dp and a tail that grows with its speed, from
    36 dp at rest to 140 dp. Measured along the rounded outline, as a loop. White only: no colours.
  - *Giving way:* any key, or the list changing under it, and it fades in 160 ms while it goes on; it does not
    come again. Leaving fades it in 90 ms.
  - *The model at work* is the same light with another motion, so that a flourish is not read as "working":
    steady laps at 1.5 ms per dp (2.4 s round the field), 75 % as bright, in over 200 ms, out over 240 ms while
    it goes on. If it starts during the lap, the lap's light becomes it: the brightness goes one way from what
    it has to the model's, and the speed changes evenly from the lap's to the model's over the rest of that lap.
    Once it has run, the lap does not come in that opening.
  - In light theme over a white window it cannot be seen. Accepted: a white reflection has nothing to show on white.
- **The selection in dark theme** is see-through like the panel: `secondaryContainer` at 0.42 on glass (0.66
  before; measured over a white window and a dark one it is the same step from the glass, about 9 L*, and the
  title on it is 5.6:1 and 12.6:1). On solid ground (the Booklight window, glass set to Solid) it stays 0.66. The
  red pill is 0.50 on glass. Rim, the strip's pane and light theme are unchanged (`selectionFill`, `LocalGlass`).
- **An app's row** has six icons: Open, New window, App info, Maximise, Left half, Right half, then the arrow.
  The list under the arrow: the three thirds, the two "two thirds", the four quarters, Centre, then Uninstall
  after its gap. Names in that list are full ink; their glyphs second ink.

## 13. The copy's line (M1, October 2026)

Drawn in `milestones/04-design.md` §3.1; as built in `overlay/CopyLine.kt`.

| Part | Layout | Look |
| --- | --- | --- |
| Seat | row one's: 8 dp under the field, 56 dp high. The panel is 68 + 8 + 56 + 8 = 140 dp | No fill, no outline, no pill, no footer |
| Mark | a 36 dp disc centred on x = 38, the clipboard glyph at 20 dp | the disc of a row that is not selected: ink at 0.80, the disc at 0.08 of it (dark 0.12) |
| Text | from x = 72, one text on one baseline, 14 sp | All of it full ink (over a window of the other brightness the glass is mid-grey, and second ink is too weak there); "Copied 20 s ago · " at weight 500, what it holds at 600. Too long: the 24 dp fade, never an ellipsis |
| Cap | `tab`, 22 dp high, its right edge at x = 700: under the field's `esc`; both at least 36 dp wide, so they make one column | the footer's cap |

| Transition | What moves | Spec | What stays still |
| --- | --- | --- | --- |
| The line arrives | the height goes from 68 to 140; 120 ms later the disc, the text and the cap rise 12 dp and fade in, 22 ms apart | `place`; `fade` 140 · 320 ms after the glass is 85 % open, and only if nothing was typed | the field, and the opening itself |
| Its words become known late | the second part of the text fades out and in where it stands | `fade` 70 out, 110 in | the first part, the cap, the height |
| A letter is typed | the line fades where it stands, over the rows that come: its words in 70 ms, its mark in 140 | `fade` | the field |
| Tab, Down or a click | the line fades the same way; the chip appears where the mark was (no slide: nothing was typed for it); rows cascade; the pill appears on row one, whole | `fade` 110 after 30; rows as row 7 of §6 | the seat: row one's disc comes into the line's disc as it goes |
| Enter on a model's row | the row grows from 56 to 92 dp with the pill's lower edge; its mark and strip travel with it; the name fades out and the question in; the other rows fade where they are | `place`; `fade` 70 out, 110 in | the row's place, the strip (Gemini keeps its slot beside Ask) |
| Backspace on the empty field | the same, backwards | the same | the row's place |

The white reflection waits for the line as it waits for a list: the line arriving, and its words settling,
count as the list changing.

An answer's row keeps a room of 216 dp for its strip, as a row of key caps keeps one: the text beside it is as
wide before the answer as after it and whichever action is armed, so it is laid out once.

## 14. A flight’s row (M5, 2 October 2026)

Drawn on screens 12 to 15 of `booklight-milestones.html`; built, not yet judged on a device. No new colour,
type size or row height: it is the preview row (§3c) with four things added to it.

- **The row** is 92 dp from its first frame. Line one: the number, who flies it and where, small, second ink.
  Line two: two slots, LEAVES and LANDS ("Left", "Landed" once they have; German „Ab“ and „An“ for both), each
  the airport's three letters and the time in that airport's own time, with the day's short name before the
  time when it is not the user's today. Line three: the status in full ink, then where to go a step lighter.
- **A first slot with a least width**, so that "Lands" stands at one x before and after the answer: 176 dp on
  a 24-hour clock, 196 dp on a 12-hour one, and 24 dp more when a day more than six days off was typed (it is
  said as a date). The width follows only what is known before the answer, never the answer. (It was 136 dp
  until the answer showed a departure on another day, which is every lookup with a day and every overnight
  flight on the day it lands: "Lands" then moved 60 dp. The reviews of 2 October.) Worked out, not measured on
  a 24-hour device: "LEAVES SFO Wed 2:47 PM" measures 180 dp on the Lenovo (label 65 px, gap 14 px, value
  191 px at 1.5 px per dp); without " PM" and with a fourth digit "LEAVES SFO Wed 14:47" is about 165 dp, and
  about 175 dp with the widest airport letters (WAW, MMX), which leaves it one dp; a date for the weekday
  ("24 Dec") is about 23 dp more. One case still widens the slot as the answer lands: a
  date nobody typed, the timetable's flight a week from today.
- **Struck values**: the times of a flight that is cancelled or diverted are struck through and a step lighter, as a done
  task's words are.
- **A delay is said, not painted.** No red, no mark. Red stays the colour of what removes something.
- **A day beside a time** is its short weekday within six days of today ("Fri 10:55") and its date beyond
  ("24 Dec 14:40"), in the row, the copied line and the pin. To be looked at on a device, on both clocks.
- **An action that is off** keeps its slot in the strip at 40 % ink and is passed over by the arming.
- **The footer's left end** names the source of an answer in the small type at second ink.
- **The light of work** (§12) runs only if the answer has not come 600 ms after the request went out.
- **Without an answer** the row keeps its three lines and its strip: the slots show their rule, and the third
  line says why in full ink.
- **Without a key**, and for a guess nobody has gone to, it is an ordinary 56 dp row with the plane in the icon
  column and "Flight" at its right end.
- **Pinned**: the timer's window (280 × 118), a small line at second ink and a figure in the round face at 34 sp,
  in words ("1 h 07 min", "42 min", "Landed"): beside the timer's "7:42", "1:07" would read as 67 seconds. The
  figure rolls when the minute changes, as a changed answer does in the panel.
- **In the window**: one group, "Flights", at the end of Results: a row that says what is sent and when and
  whether a key is in (Enter opens one field under it, as an editor opens under its row on the Yours page), and
  a row that leads to where a key is got. The key that is in is never shown.

