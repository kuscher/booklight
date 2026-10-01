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
| | pill | `secondaryContainer` 0.78 / 0.66 |
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
- **Closing does not mirror.** Presence fades in 90 ms while the layer's scaleX goes 1 → 0.94 (tween 110, accelerating); blur and dim follow presence; the window is not resized, because a resize during an app launch is where frames would drop.
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
