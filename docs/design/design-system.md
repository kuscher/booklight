# Booklight: design system and motion specification for 1.1 to 2.2

*1 October 2026. Desk work from the 1.0 code, captures, design pass, review and UX model; no device was touched, so every alpha and spring is a starting value (§9). "Ink" is the ground's on-colour (`onSurface` on glass, `onSecondaryContainer` on the pill) at an alpha. L / D = light / dark.*

## 1. Principles

1. **One sheet of flat glass, one coloured thing.** Veil, white outline, black hairline; the selection pill is the only chromatic surface. (One exception since 3.0, with the owner's word of 2 October 2026: the badge of a flight's row, green or amber, §14. And one sample of a colour that is the user's own, as a swatch is: the dot of a calendar's colour, §18.)
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
| Height | field · row · tall · flight · open row · picture · grid · card · footer | 68 · 56 · 92 · 136 · 96 · 208 · **256** · 96 · 36 (+ 8) dp |
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
| | a flight's badge (§14): fixed, not the wallpaper's (`FlightColors` in `ui/Theme.kt`) | on time, early: `good` #B6F2BE with `onGood` #00210A L / #0A5226 with #B6F2BE D · late: `late` #FFDEA6 with `onLate` #261900 L / #5C4300 with #FFDEA6 D · the fill at 0.88 · "Planned": the scope chip's fill, full ink · in the pinned window, as a word on solid ground: #17662B · #7A5200 L / #8FDB9B · #F1C267 D |
| | a flight's line (§14) | flown part and plane: ink 1.0 · dots still to go: ink 0.50 / 0.55 · the line at rest: ink 0.30 / 0.34 · an empty end's rule: ink 0.30 |
| Outline | panel | white 1.25 dp at 0.80 / 0.44; black hairline 1 px outside at 0.20 / 0.28 |
| | pill, pane, scope chip, a flight's badge | white 1 px at 0.55 / 0.30; the pane adds an ink hairline outside, 1 px at 0.20 |
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
- **First run's answers** (§17). The same strip, vertical: two 32 dp slots 4 dp apart, both as wide as the wider label plus the mark home, flush right, `word`. One highlight in the pill recipe.
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

*As built in October 2026 (`docs/design/window-redesign.md` has the reasons, `booklight-window.html` the drawn
layouts, `docs/research/device-findings.md` what the Lenovo showed). It replaces "the panel without blur".*

An ordinary desktop window in Material 3 Expressive. It keeps Booklight's colour scheme, type, symbols and key
caps, the panel's selection colour for what is open or chosen, the panel itself on Start and Look, and motion from
`Motion.kt`. What stays the panel's alone: glass, the white outline, and the one pill that follows pointer and keys.

**The frame** (widths in dp; the caption bar is the system's, 40 high, see-through, with nothing of Booklight's in it)

| Window | Navigation | Page |
| --- | --- | --- |
| under 600 | Material's `ShortNavigationBar` along the bottom, 64 high | one column, 16 from each edge |
| 600 to 839 | `WideNavigationRail` collapsed on the leading edge, 96, name under mark | one column, 24 from the rail, 24 from the edge |
| from 840 | the rail expanded, 220, name beside mark | one column, 24 from the rail, at most 720 wide, never centred |
| from 1332 | the same | and a second pane, 24 from the column, 320 to 560 wide, 24 from the edge |

- The window is at least 400 × 480 (`<layout>` in the manifest).
- The rail has no fill. Its indicator is Booklight's own: Material's shape (56 × 32 round the mark; in the expanded
  rail 56 high round mark and name), in `secondaryContainer`, travelling between the items on `lead` and `trail`.
  Material's own indicator is switched off.
- The centre line of a page's title (28/36, weight 600, the rounded cut) is 40 below the caption bar, and so is
  the centre of the rail's first mark, at both rail widths.
- The second pane holds the live demo (Start), the preview of the look (Look), and on Commands the open editor
  or a built-in command's example. Results and Privacy leave it empty. Below 1332 the demo and the preview are
  at the top of their pages and an editor opens under its row. The pane's top is the top of the title line's own
  control (Find): 20 over the title's centre line.
- In the smallest window: New is only its plus (a column under 400), and Start has no demo (a pane under 520
  high), so the row about the key is in view.

**Lines everything stands on** (in a column of width C)

| Line | What stands on it |
| --- | --- |
| 0 and C | rows and editors; Find's trailing end; Built in · Yours; New |
| 16 | the title, the lead, a group's name, a row's mark (24 wide) |
| 52 | a row's text (Material's list item: 16 + 24 + 12); a choice that stands under its text; a command's example in a narrow row |
| C − 16 | the trailing end of every control in a row: switch, button group, menu button, a word, a mark (a command's Enter mark, after its example) |

The choices of one page stand in one place: at the rows' trailing end when every one of them leaves its text
200 dp, else every one under its text. At the trailing end every button of the page is one width (the widest
name's), so the groups' seams stand in line, as long as every row's line still fits on one line beside them; else
each button is as wide as its name (German at the default width). The search engine's button stays at the
trailing end at every width.

**Colour.** Ground `surfaceContainer`. Rows, editors and Find `surfaceBright`. Text `onSurface`; second lines, marks
and group names `onSurfaceVariant`. The open section, a chosen option, the row whose editor is open
`secondaryContainer` with `onSecondaryContainer`. A switch that is on, Save, the caret `primary`. Delete `error`.
Unchosen buttons of a choice are in the ground's colour on a row, in the rows' colour on the ground.

**Type.** Title `headlineMedium` 28/36 at 600, rounded. Lead `bodyLarge`, at most 600 wide. Group name `titleSmall`,
sentence case. Row `bodyLarge`, its second line `bodyMedium`. Rail `labelLarge` beside the mark, `labelMedium`
under it. A command's example in the fixed-width cut, 12 sp.

**Parts** (all from `material3` 1.5.0-alpha29)

| Part | Component | Notes |
| --- | --- | --- |
| A group of settings | `SegmentedListItem` with `ListItemDefaults.segmentedShapes` | 2 between rows, 24 between groups; rows 56, 72, 88 by their lines; mark and control on the row's centre line (on the title's line when something stands under the text) |
| On or off | `Switch` (52 × 32) | the row flips it, and Enter and Space; not Left and Right |
| A choice of 2 to 4 | `ToggleButton`s 2 apart with `ButtonGroupDefaults`' connected shapes | 40 high; the chosen one round; only the buttons are pressed: the row has no click and no hover, a click beside the buttons or between them does nothing |
| A choice of 5 or more | a `Button` and `DropdownMenuPopup` with `SelectableDropdownMenuItem`s | the button as wide as the longest name needs; the menu exactly as wide, 4 under it; names start where the button's does; the chosen one's check under the button's arrow |
| New | `SplitButtonLayout`, tonal | New link; the arrow offers snippet, recipe, prompt |
| Find | a rounded field, 40 high, 264 wide | a round button in a compact window, opening over the title |
| A text in a row | `Field` (`Controls.kt`) | under the row's text, from the text's edge to C − 16; a pill 40 high in the ground's colour, like the menu's button; a 2 dp ring in `primary` while it has the keys; Enter on the row puts the caret in it, Enter in it saves, Escape gives up what was typed, Tab goes on. What saves or removes stands at its end in a wide row (the field gives it room as it comes), under it in a narrow one. The flight service's key is the first: it is never shown once saved ("A key is in"), and Take out asks twice |
| The editors | `TextField` (12 dp corners, a ring in `primary` when it has the keys), `Button`, `TextButton` | under a row with 16 dp corners; in the second pane with 28; Delete's word ends 16 inside the fields' edge |
| Menus | `DropdownMenuPopup`, `DropdownMenuGroup`, `DropdownMenuItem` | flat: the rows' colour, a 1 dp outline in `outlineVariant`, no shadow, the items on the menu's own fill; right-click (or Menu, or Shift + F10) on a command, on one of the user's own, on an app |
| The demo and the preview | `Stage` | the panel's own veil and outline over the desk blurred where the panel stands; a drawn shadow; the desk dimmed when dimming is on; the desk's ground is the rows' colour (never the scheme's darkest); its two windows run off the stage's edges; the panel is one for the window (`Stages`), so a stage that moves between column and pane shows it as it was |

**States.** Hover: Material's state layer (8 %) and the row's corners to 12. Pressed: 10 %, corners 16. Keyboard
focus: one ring for the whole window, 2 dp in `secondary`, inside the edge of the stop the keys are on; the row
takes Material's focused shape (corners 16). The ring shows only while the keys are what is used; a press of the
pointer puts it away. Selected: `secondaryContainer`. Disabled: Material's 38 %.

**Keys.** The window's root has the keyboard; Material's parts are not focus stops (text fields and menus are
their own). Tab and Shift + Tab go through every stop of the page in reading order and round through the
navigation; F6 goes between navigation and page; Up and Down move through the stops (in the rail: through the
sections, at once; under a narrow window Down on the last stop goes on to the bar); Left and Right step a
choice; Left goes to the rail from a row without a choice, from a switch, and from a choice at its first option;
Enter and Space run the stop (a switch flips, a choice goes to its next option); Home, End, Page Up, Page Down;
Ctrl + 1 … 5, Ctrl + F, Ctrl + N, Ctrl + W; Delete on one of the user's own asks once more on the row; Menu or
Shift + F10 opens a row's menu; Escape closes a menu or an editor and clears Find, never the window.

**The editors.** What is typed is the window's (`CommandsState.draft`), not the editor's: it is still there when
the editor changes its place at 1332 dp, with the keys in the same field. An open editor has the keys until it
is closed: Tab goes round inside it. Enter in a one-line field saves, Ctrl + Enter in any. While Save cannot be
pressed and something was typed, one line over the buttons says the first thing that is missing. An editor with
something unsaved is not left at once: Escape, Cancel, another row, the other half, a section, Ctrl + N and
Ctrl + W first turn Cancel into "Press again to discard" for three seconds. After Save the keys are on the saved
row; after a new one was cancelled, on New.

**Motion** (Booklight's own on `Motion.kt`; Material's parts on the theme's `MotionScheme.expressive()`)

| What happens | What moves | Spec |
| --- | --- | --- |
| A section changes | the indicator travels, leading edge first; the old page fades where it stands (70 ms); the new page's blocks rise 12 dp, 22 ms apart, from the side the indicator came from; the second pane's content does the same | `lead`, `trail`; `fade`; `place` |
| The keys move | the ring glides, the edge that leads on `lead`, the one that follows on `trail`; it stays on its stop while the page scrolls | `lead`, `trail` |
| An editor opens | under its row the page makes room and follows it into view frame by frame; in the second pane it fades in and rises 12 dp | `place`; `fade` 140 |
| Built in · Yours | one half fades where it stands, the other comes; the page takes the new height | `fade`; `place` |
| The window is resized | widths follow the window frame by frame | none |
| 600 is crossed | the rail's width and the bar's height open or close while they fade | `place`; `fade` |
| 840 is crossed | Material widens the rail, each name moving from under its mark to beside it; the indicator follows it in the same frame | Material's default spatial spring |
| 1332 is crossed | the second pane fades in and slides 24 dp from the trailing edge; the demo or the preview closes from below in the column while it fades (180 ms) and the rows under it move up; back, it opens from its top | `fade`; `place` |
| Animations off | Booklight's own cut; Compose scales Material's by the system's setting | `Motion.on` |

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
| 14 | The grid highlight moves | The two edges on the axis of travel, as the list's pill does (21); a move to the next line's start is rigid | `pillLead`, `pillTrail` · the old edge 5 frames; `place` | Cells. The name fades 80 in, 40 out, left-aligned |
| 15 | Confirmation arms | Pane's left edge to its measured width; fill to `errorContainer` (120); label out 60, in 110, clipped by the pane; siblings dim (80); the line shortens | `lead`; `drain` · label 40, line 120 | The right edge and the mark |
| 16 | Cancels, completes | Cancel: edge back, fill back 120, line fades 60. Complete: the row's height goes to 0 and it fades (80); rows below and the window follow; the pill takes the next row | `trail`; `place` | Other rows keep their keys |
| 17 | Feedback word | Check scales from 0.6; the word slides 8 dp from the left, fades 120; out 80 | `pop`; `place` | The footer's right side |
| 18 | First run's answers (§17) | One highlight: upper and lower edges | `lead`, `trail` | Both labels, marks, widths and the weight. No slot has a background or a size that depends on being chosen, so the 1.0 defect cannot recur |
| 19 | Long text in the field | The scroll offset follows the caret | cut | Tied to typing, never animated |
| 20 | Unfold, close | §4 | `open`, tweens | Content position on screen |
| 21 | The list's pill moves (§15) | The front edge at once; the old edge holds on, then follows. Only from rest: a pill that is moving (a held key, the pointer) does not hold, and every edge keeps its speed. Never drawn more than 56 dp longer than its row. A row that only grows: the lower edge alone. Its own row moved by the list: both edges with it | `pillLead`; `pillTrail` · the old edge 5 frames of the screen (2 on a way of more than 98 dp, then on its firmer spring); `place`; `place` | Width, radius, colour, alpha: one flat shape. Each edge is rounded to a pixel by itself: one that holds on stands still, none steps back |
| 22 | A flight's answer lands (§14) | Its words change where they stand (70 out, 110 in after 40); the empty ends' rules go with the old words. The plane fades in at the start (110) and travels to its place, the flown part growing behind it; the dots ahead step from "at rest" to "still to go" (110) | `fade`; `flies`: the glass's curve over 300 ms + 300 × the share of the line it crosses, 60 ms after the words began to change. Not `place`: it would carry the plane 3 dp past the end of a full line | The row's height, the line's two ends, the dots' places, the mark, the strip. One number places the plane, ends the flown part and uncovers the dots |
| 23 | A minute passes on a flight's row (§14) | The headline rolls; in the air the plane takes its step (1 dp for a ten-hour flight, 10 for a one-hour one) | `roll`; `tick` · one wake a minute, none while no row counts minutes | Everything else. Nothing loops: between two steps the row is still |
| 24 | Another flight, or none yet (the number is typed on) | The plane and the flown part fade where they stand (110), the dots go back to rest; it never flies back. Another answer about the same flight: the plane goes forward to its new place once, never back (it waits until the clock has caught up); the badge's colour fades (120) | `fade`; `flies` | The line |

## 7. Continuity rules

1. **No animated width that text depends on.** Measure both ends first. The only width changes are a highlight's edges and the confirmation pane.
2. **One owner per property.** A fill belongs to the highlight, never to the chips it visits. Things that must stay together read one animated value: fill and thumb; clip and marks; window size, shader size and outline radius.
3. **Stable keys.** Rows by result id, chips by action id, cells by code point, slots by name; never by index.
4. **No reflow.** One-line field, fixed slot columns, tabular figures in fixed slots, fade edges instead of ellipses; weight never changes with state; type size is chosen once.
5. **Typed text cuts, objects spring.** Anything showing what was just typed updates in the same frame.
6. **Fast typing.** Interruptible: every spring retargets from its present value and velocity. Never restarted: the unfold, a row whose id persists, a strip whose selection has not moved. At most one list change per frame; a list replacing one still cascading drops the pending delays. Rolls and cross-fades cut when changes come under 120 ms apart.
7. **One window write per frame:** width, height, position and outline radius together.

## 8. Accessibility

- **Armed is a shape, not a colour:** the Enter mark exists only inside the highlight. On: thumb position. Needs a grant: hollow. Destructive: last, apart, and worded. Failure: its glyph. A flight's verdict is the badge's word ("Delayed 27 min"); its green or amber only repeats it, and a screen reader hears the headline and the word.
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
letter; since §15 `pillLead` 0.85/1400 and `pillTrail` 0.86/900 (0.9/1400 on a long way) for the list's pill and
the grid's square. A debug build stretches all of them: `./bl open stay slow=4`.

- **The app row** (replaces §3a "three chips and more"): nine icons at a 32 dp pitch and a tenth stop, the arrow.
  Enter on the arrow, or Right again, opens the rest as a list under the row: 40 dp lines, uncovered by one edge
  on `place`, the list's own pill as their highlight. A typed place among the rest takes the arrow's slot.
  Store page is gone.
- **The opening** (§4): always at the field's height. What is under the field (first run's stage, a tip, rows for handed
  text) comes once the glass is 85 % open. The closing draws in to the field's centre line.
- **The answer row**: a caption, then two lines; the answer is written in behind a head that follows what
  arrives (14 letters soft), and the row grows once, to four lines, on `place`. The mark and the strip keep
  the line of the row's first height. While the model has said nothing yet the edge light runs a slow lap
  (2.4 s) and finishes it when the first word lands.
- **Key caps in a row**: 24 dp, strong ink, plus signs between; 200 dp kept free at their right for the strip.
- **A task**: a 20 dp box in the mark column; ticked, the check draws itself (one 140 ms stroke, the same mark
  as "Copied" and "Your key works") and a line strikes the title from its start.
- **The tip card**: 96 dp, two answers as on first run's stage (§17). Nothing armed at rest; a `tab` cap beside the two answers.
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

## 14. A flight’s row (M5; redrawn for 3.0, 2 October 2026)

Alex: "I love flights so I am willing to give it more height to fit in a bit more good looking into like a line on
where the plane is for progress with a plane icon, delayed and on time badges." Designed in
`docs/design/flights-row/` (`design.md` is the specification, with every measure; `flights.html` the page he
approved: "Go ahead, approved"). Built to it in `overlay/FlightBody.kt` and `overlay/FlightLine.kt`; **not yet
judged on a device**: every alpha and every time is a starting value, and `design.md` §13 lists what to look at
first. The row of 2.3 (92 dp, two slots LEAVES and LANDS, a status line) is gone, and with it the first slot's
least width.

- **The row is 136 dp in every state, from its first frame.** Its upper part is what a row always is: the mark
  (the plane in its disc), words, the strip, on the tall row's centre line (y = 46). Line one, small: the number,
  who flies it and where. Under it the **headline** in the title type, the one thing needed: "Leaves Fri 10:55 AM",
  from three hours before "Leaves in 42 min", in the air "Lands in 4 h 07 min", then for three hours "Landed
  12 min ago" ("Landed just now" in the first minute), "Cancelled", "Diverted", "From the timetable", "Looking it
  up", or the reason there is no answer. A countdown of an hour or more keeps two places for its minutes, in
  tabular figures, so nothing shifts while it counts.
- **One badge** stands 10 dp after the headline, its word on the headline's baseline: a stadium 22 dp high, 9 dp
  inside each end, the word at 13 / 600, the pill's 1 px white rim. It says whether the flight runs to plan: "On
  time", "Delayed 27 min", "24 min early", after landing "27 min late", and "Planned" when only the plan is known.
  It never repeats the headline: beside "Cancelled", "Diverted" and "From the timetable" there is none. "On time"
  is a claim, made only when the service sent a time of its own. **Late starts 15 minutes after the plan** (it was
  5 while a delay was only words): with a colour on it, "Delayed 6 min" is alarm for nothing, and the exact time
  stands under the line either way.
- **Colour in the badge, nowhere else.** Green for on time and early, amber for late (the tokens of §2), neutral for
  "Planned". This is the one exception to two rules, decided by the owner on 2 October 2026: "a delay is said, not
  painted" (this section, until 3.0) and "the selection is the only coloured surface" (§1). It is kept small on
  purpose. The colour is always a fill that brings its own ground, with its own ink on it: a green or amber word
  or line straight on the glass does not hold (1.2 : 1 in light theme over a black window; the filled badge keeps
  its word at 6.3 : 1 or better over a light window and a dark one, on bare glass and on the pill). The line, the
  plane, the times and every word stay in the row's ink. The word carries the meaning; the colour repeats it.
  **Never red**: a cancelled flight is the headline "Cancelled", both times struck and a line without a plane. Red
  stays the colour of what removes something, and a red badge with a word in it is what an armed Delete looks like.
- **The line** runs from the titles' edge to the strip's right end (x = 72 to 700, 628 dp) on y = 84, in a band of
  its own under the strip, so it is as long selected or not, before the answer and after. The flown part: 3 dp,
  solid, round ends, full ink. Still to go: 70 dots of 3 dp at a pitch of 9.06 dp, the first and the last exactly
  on the two edges, at half ink. **The plane** is the app's own `plane` symbol turned a quarter to point along the
  line (20 dp long), full ink, its tail at the share of the way that has passed; the flown part ends 4 dp before
  its tail, and the dots come into view over 8 dp from 2 dp before its nose, so none is ever cut in half. Flown
  and still to go differ three ways, none a hue: solid against dotted, full ink against half, the plane between.
- **The plane's place is time**, worked out in the core (`FlightStatus.share`): the minutes since it left against
  the minutes from then to its landing (the actual time, else the expected, else the plan). Never the service's
  own percentage, never distance. At the start until the service says it has left; in the air never nearer an end
  than 2 %; at the end, its nose on 700, once it has landed. **No plane where nobody knows where it is**: waiting,
  no answer, cancelled, diverted, a timetable's flight past its time; the line is then its dots at rest, a step
  lighter. A plane on the line is a claim.
- **Under the line's two ends** (y = 100 to 122, one baseline each): at the start the airport's three letters at
  second ink, 7 dp, its time in full ink, 12 dp, small words; at the far end the mirror, so each airport stands
  under its end of the line. Each time is in its airport's own clock, with the day when it is not the user's
  today: the short weekday within six days ("Fri 10:55 AM"), the date beyond. The small words are what matters at
  that end now: before it leaves, gate and terminal at the start ("Gate Z58 · Terminal 1"); once it has left, the
  aircraft's type at the start ("Boeing 747-8") and at the far end the terminal with the belt, or the gate while
  no belt is named. **What the service did not name is simply not there**: no "Gate –", no held place; small
  words are the last thing at their end and grow towards the middle of the row. If the two ends would come closer
  than 24 dp, the aircraft is left out first, then the terminal. The times of a flight that will not happen are
  struck through and a step lighter, as a done task's words are. Before the answer each end is a rule, 20 × 2 dp.
- **A reason too long for the headline** (the German "Die AirLabs-Abfragen dieses Monats sind aufgebraucht") is set
  in the small type, full ink, chosen once for that text.
- **Motion** is rows 22 to 24 of §6. One arrival when the answer lands, then a step a minute, forwards only; with
  animations off the plane is simply there. Nothing loops. The row has its height before the answer comes, so the
  list does not jump; a guess the user goes to grows once from 56 to 136 where it stands, the pill's lower edge
  with it, the mark and the strip travelling from y = 28 to 46, its name fading out (70) and the tall row's words
  in (110 after 40) as the row's edge uncovers them.
- **An action that is off** keeps its slot in the strip at 40 % ink and is passed over by the arming.
- **The footer's left end** names the source of an answer in the small type at second ink.
- **The light of work** (§12) runs only if the answer has not come 600 ms after the request went out.
- **Without a key**, and for a guess nobody has gone to, it is an ordinary 56 dp row with the plane in the icon
  column and "Flight" at its right end. No line, no badge, no faded copy of the tall row.
- **Unselected** (a local match stands above it): the same row on the glass, no pill, "Flight" where the strip
  was, line one and the small words at second ink.
- **Pinned**: the timer's window (280 × 118). A small line at second ink (y = 15 to 35): the number, the verdict
  as a word **in its colour** ("on time", "27 min late": the window's ground is solid, where a coloured word
  holds), then the gate while it has not left, the time of landing after; if the three do not fit 240 dp the last
  is left out. With only the plan known the line is as before (number, where to go, time). Under it the figure in
  the round face at 34 sp, in words ("1 h 07 min", "42 min", "Landed"): beside the timer's "7:42", "1:07" would
  read as 67 seconds. Under that the flight's line, small (y = 87 to 103): 240 dp, the flown part and the dots
  2 dp, the dots at a pitch of 7 dp, the plane 16 dp long. No airport letters, no aircraft, no filled badge: a
  glance takes the figure, the line and one coloured word. Between two answers (half an hour apart) the plane
  moves by the clock with the figure; a new answer moves both once. Cancelled, or with nothing known yet, the
  dots are at rest.
- **In the window**: one group, "Flights", in Labs: a row that says what is sent and when and
  whether a key is in (Enter opens one field under it, as an editor opens under its row on the Yours page), and
  a row that leads to where a key is got. The key that is in is never shown.

## 15. The rubber highlight (2 October 2026)

Alex: "make it so the animation when the highlight switches between rows feels a little more rubber like". Proposed,
reviewed and tried on a page (`rubber-highlight.md`, its A); built as decided there and measured on a device (its first paragraph). Row 21 of §6 is the list's pill; the grid's square (row 14) travels the same way. `Motion.kt` holds the numbers.

- **What changed.** The pill's two edges set off together on `lead` and `trail`: the old edge left at once and was
  the slow part of the move for a quarter of a second. Now the front edge goes at once on `pillLead` (0.85/1400);
  the old edge stands still for five frames of the screen (42 ms at 120 Hz) and then gathers on `pillTrail`
  (0.86/900). One row: 34 dp of stretch where there were 18, and at rest after 197 ms where it took 255.
- **A long way** (the front edge has more than 98 dp to go): the old edge holds two frames and follows on 0.9/1400,
  so the pill arrives in one move. It is drawn at most 56 dp longer than its row, the taller of the one it left and
  the one it goes to: as it is up to 40 dp of stretch, easing into 56 beyond.
- **Moving, it keeps its speed.** A pill that is on its way (a held key, the pointer over the rows, a reversal) does
  not hold, and every edge goes on from where it is drawn with the speed it has. Before, every new row started
  both edges again from a standstill. The edges are no longer animations that a new row cancels: one loop moves
  them once a frame, and the hold is a count of frames (`Band` in `overlay/Rows.kt`).
- **Whole pixels.** Top and bottom are each rounded by themselves and the height is what lies between them.
  Before, the bottom was a rounded top plus a rounded height, and stepped back and forth while it settled.
- **No rubber where it does not travel.** A row that grows under the pill takes its lower edge on `place`, as
  before; a row that a changed list moves takes the pill with it, both edges on `place`.
- **Unchanged:** the pane on a row's actions (`arm`), the card's options and the Booklight window's rail and ring
  (`lead`, `trail`), the speed setting (it is the opening's), animations off (it cuts), colours and alpha.
- **To measure** (debug builds): `./bl debug pill N` writes the pill's next N frames to the log. (The motion as it
  was before could be opened with `pill=old` while the two were compared; it was taken out before 3.0.)

## 16. An app's row, its two lists and its chip (2 October 2026)

Drawn in `app-structure/rows.html`; the interaction is `ux-model.md` §18. Built, not yet judged on a device. No
new colour, type size, row height or spring: every part is one the panel had.

- **The strip on an app's row** (replaces §11 "nine icons and a tenth stop"): two to four icons and the arrow, in
  one order: Open, Search (the magnifier), Play (the triangle), Window (a window with its title bar and nothing in
  it), the arrow. At its widest (German, Play armed) about 265 dp, so the title keeps 340 dp or more.
- **Two stops open a list**: Window and the arrow. The list is §11's (40 dp lines, uncovered by one edge on
  `place`, the list's pill as the highlight). While Window's list is open and the pill is on one of its lines the
  row shows the word "Window" in the kind's place before the turned arrow; with the pill back on the row, Window's
  slot reads "Less". Window's list is 14 lines (56 + 14 × 40 = 616 dp), the arrow's 7 at most with Uninstall's
  8 dp gap (344 dp): both fit under the field on the 14-inch screen.
- **A typed line stands in its list's stop**: a place in Window's slot (the icon and the name change where they
  stand, as in the arrow's slot), a page or Uninstall in the arrow's.
- **An app's chip** (§3b): the same 36 dp stadium, with the app's own icon at 20 dp in place of a symbol, in the
  column the row's icon stood in (centred at x = 38). Its name has 160 dp and fades over its last 16 dp only when
  it is cut; the icon stays. One chip for an app: it does not change when Tab goes from Search to Play; the
  placeholder does, on the placeholder's own fade.
- **The line under the empty field of an app's chip**: the copy's line (§13) with other words. A 36 dp disc with
  the symbol of the action that is not armed, its words at 14 sp, weight 600, full ink ("Play in Spotify",
  "Search Spotify"), a `tab` cap under the field's `esc`. The panel is 68 + 8 + 56 + 8 = 140 dp. It comes 120 ms
  after the height starts (`fade` 140), goes in 70 ms with the first letter, and its symbol and words change
  where they stand when Tab changes the action. An app with one action has none: the field stands alone.
- **The row under the chip** keeps its seat and its strip when Tab changes its action: only its words change,
  in the frame the new row lands. Play that found nothing keeps its slot at 40 % ink (§14).
- **A title that names an app and is too long** says less: "Search for “dune”" in place of "Search … for “dune”",
  when the whole line is wider than 340 dp, the least room a title has beside the strip. It is measured against
  that room and not against the row as it stands, so it does not change while the strip unrolls.
- **Marks**: the web's row and a link's row have the magnifier for their Search action (an action carries a
  symbol for what it does); the play row has the app's icon as its mark and no icon inside its action.

## 17. First run (5 October 2026)

Drawn in `first-run/first-run.html`; the papers are `first-run/design.md` and `first-run/motion.md`, and
`first-run/00-brief.md`, "Settled for the build", wins over both. The interaction is `ux-model.md` §19. Every time
and curve of motion below is in `overlay/Motion.kt`: the stage's as `PACE` and the constants beside it, the
welcome's as `Lights`. When each part of a stage comes is core's `SetDown`, which takes `PACE` and has tests; and
the 350 ms a screen must have been in view before it takes a key are core's too (`FirstRun.SEEN_MS`): a rule, not
a motion.

**The stage** stands where a card stood: under the empty field, on the glass, with no frame of its own.

| Part | Measure |
| --- | --- |
| The stage | 184 dp: 8 of air, a seat (56), a band (112), 8 of air. With the field the panel is 252 dp |
| The seat | a row's own: a 36 dp disc with a 20 dp symbol at x = 20, a title and a line as a row's, a counter ("1 of 5") in the kind's place |
| The band | the keys or a recipe on its centre line, a caption (13 sp, weight 500) on the line a footer's words stand on |
| A large cap | 56 dp high and at least as wide, 16 dp corners, 20 dp inside each end, the field's type (24 sp, weight 500); ink at 10 % (14 % in dark) under a hairline of white |
| A recipe | large caps and typed words in the field's type, 12 dp between two caps, 16 dp otherwise; one too wide for its room is drawn smaller as a whole, from its left end, never cut |
| The answers | the strip, vertical, flush right, 20 dp from the edge: one or two 32 dp slots, 4 dp apart; a `tab` cap beside them while none is armed |
| The question's stage | 256 dp: the seat, the disclosure on lines of 20 in strong ink (four lines, and taller by as much as it needs more), a note, the two answers. With the field 324 dp |
| The choices | two rows of the list's own kind: a switch's row (92 dp) and a row |

**It is set down, not shown.** One clock runs from the moment a screen comes; every part has a mark on it, and
draws by where the clock stands, so a still at any moment is whole.

| A screen comes | Its parts, in order | Enter counts |
| --- | --- | --- |
| Set down, as a run's first opening opens on it | the disc, the words, the counter, 22 ms apart from the gate, each rising 12 dp as a row does; the band once the glass is at rest (120 ms after the gate at the least): its caps a beat (66 ms) apart, each fading in over 110 ms and growing from 0.96 on `pop`, a recipe's typed words a letter at a time (40 ms); the caption a beat after the last piece; the answers 22 ms after the caption | 350 ms after the answers began to show |
| Back, as a later opening opens on it | as rows come: the seat's three parts, the band whole, the caption and the answers, 22 ms apart. Nothing is written twice | the same |
| In another's place (an answer, Skip, a key that landed) | the seat stays and its title, line and counter roll 22 ms apart; the band's old pieces fade in 80 ms and the new ones are written from 120 ms on; what the two screens share does not move | the same |
| After a list that gave way (a lesson's Enter, the sum copied, "First steps" typed) | as set down, from 60 ms after the list gave way | the same |
| At the landing of the opening piece | the disc at 80 ms, the words at 150, the counter at 190; the armed answer's words at 180, inside the highlight that has travelled there; the band from 260. The answers do not rise: they fade in where they stand | 350 ms after the armed answer's words began to show; Tab, the pointer and a click, which reach the other answer, 350 ms after that one's did (300 ms later) |
| Whole (the field emptied again, a panel turned round, animations off) | everything in one frame | 350 ms after it came |

- **Pressed.** An answer that was given shows as pressed: 8 % ink over its slot, whole in the frame of the press
  and out over 120 ms. It has no way in, because what the answer does is not held up by it: the screen changes in
  that frame, and answers that give way carry the ink while they fade (80 ms); the answers that take their place
  never show it. A large cap that is held down goes to 0.96 of its size under the same ink in 80 ms, and comes
  back up on `pop`.
- **The key lands.** "Your key" on the glass is held down as the user's own key is: for 160 ms where the panel
  was in view, for 400 where it was under the system's dialog and is only now uncovered. It comes up, the check
  draws itself 60 ms later (one 140 ms stroke), and 240 ms after the key came up the edge light runs one lap.
  Under the dialog the stage turns to "Now press your keys" 300 ms after the panel lost the focus to it. A screen
  that came under the dialog takes a key 350 ms after the dialog has gone, not after it came: nothing of it is
  drawn again for that. The user's key, held, repeats: for 700 ms of real time after its last start none of them
  puts the panel away.
- **The question** rises: its text at 66 ms, the note at 120, the answers at 142. Nothing is armed, "Not now"
  stands before "Agree", and nothing is pressed for the user.
- **The fold.** After the choices the glass folds to the field on `place`, the field's `esc` cap fades back
  120 ms later, and one lap of light runs 360 ms after the fold, unless a letter was typed first.
- **Interrupted.** A typed letter puts the stage away in that frame and is never held up. A key during a set-down
  answers nothing. A stage that gives way fades as it stood: it is not finished first.
- **Animations off.** Every mark is at its end from the first frame: nothing moves, everything stands, and a key
  counts 350 ms after the screen came. One wait stays, because it is no motion: for the frame or two until the
  list for a typed letter lands, the glass keeps the stage's height (it would else step down to the bare field
  and up again).

**The welcome and the show** are the one place the daily panel's rules are lifted, and by the owner's word alone
(Alex, 4 October 2026: `first-run/00-brief.md`); they hold again from the key's step on. The glass grows to 468 dp
for something that is not a result, the veil goes to night, the desk behind is dimmed, the title is 40 dp type, and a
piece of light is drawn that is no highlight. It happens once, at a new installation's very first opening, which
runs at Slow. Its times are `Lights` (in `Motion.kt`), on one clock from the gate: night falls over 420 ms; the
lamp strikes at the caret (60 %, back to 35 %, on) and floods the field; a seam of light falls and opens to its
shape; the words stand in it once it has reached them, and not before; the handle rises with the cue on it, and
five tools flick out a beat apart. At the hand-over the handle is pressed, the tools fold, the shaft turns up to
the caret and the night lifts without an edge; the handle travels to row one's seat and becomes the list's
pill. The show then types on `Show`'s script (core) with the panel's own motion, and its first rows stand in the
frame the pill arrives. Nothing of the two is drawn outside the glass, and the window is never resized in width.

## 18. An event’s row (5 October 2026)

`docs/design/event-sentence/design.md`. The preview row of §3c, 92 dp, with nothing new drawn but a dot.

- **Two lines of slots, always.** The caption, then `WHEN` and `WHERE`, then `TITLE` and, where the list of
  calendars is known, `CALENDAR` (the title takes what its line has, the calendar a share of at most 200 dp: on
  the first line, beside "When" and the strip, a title had room for two words). Both lines stand from the first letter; an empty slot is its label and the rule.
  So the row's three lines never change their places: when a place or a calendar is read, by the rules at a key
  or by the device's model a moment later, a value changes where it stands and nothing moves. (One line cannot
  hold four slots beside a strip of four actions: at 720 dp the third slot of the old row was already cut.)
- **The dot.** A calendar's own colour, 10 dp, round, before the calendar's name in its slot and in the icon
  column of its line in the list of calendars. It is a sample of something that is the user's, as a colour's
  swatch is, not a state: no word and no line of the row is coloured, and the dot never says anything a word
  does not say beside it.
- **"When" is never cut.** What is saved stands on the glass whole: `WHEN` takes the room its words need, never
  ends in an ellipsis, and `WHERE`, the slot beside it, is the one that gives way. So its
  words are made short (core `Spans`, 31 letters at the most in English and German, which leaves the place 96 dp
  beside the widest strip): "Wed 7 Oct, 11:30 AM–12:30 PM"; a range of days in one month with both weekdays and
  the month once, "Wed 14 – Fri 16 Oct, all day", across months without the weekdays, "28 Oct – 2 Nov, all day";
  the year wherever it is not this year's, after the day or once after a range's last day; and where a form
  would be longer, first the weekday goes, then the room round the dash, then the dot of a month's short name.
- **A guess is a step lighter** (ink 0.80), as ever: "When" where the day or the time was not typed, where the
  time does not say its half of the day, where the sentence asks for a repeat or holds a second day or time, and
  where anything else of it was the parser's to choose (a year, "next Tuesday", "12/10"); the
  calendar where it was not typed and is the one a saved event goes to, and where only the start of its name is.
- **The strip.** Create · Copy · Calendar. With "Save events without opening Calendar" on: Save · Open · Copy ·
  Calendar, and for a guess Open first, with the footer's quiet line saying why. "Open" is the short word every
  row has: the long one ("Open in Calendar"), unrolled, left the title six letters at 720 dp. Save has the
  check, like every action that acts at once and says so in the footer; it is not a removal and not red.
- **The caption says what Enter does on the armed action**: "New event. Enter saves it." only while Save is
  armed, "New event. Enter copies it." on Copy, "New event. Enter lists your calendars." on the Calendar stop,
  and "New event. Opens in your calendar to save." on Create and Open. When the arming moves it rolls into
  place, as an answer's caption does. A line typed without the keyword that begins like an everyday search
  ("book …", "schedule …") says, with the switch on, "An event, or a search? Opens in your calendar to save."
- **The footer's quiet line says why Enter does not save** though the switch is on, one reason at a time: the
  day or the time is a guess; a repeat is set in Calendar; the calendar takes no new events; its name is not
  finished; it is not one of yours; “book” often begins a search.
- **Calendar** is a stop that opens a list under the row, as Window does on an app's row (§16): Tab only moves,
  Enter opens, the row keeps the word and the turned arrow while the pill is on a line. Twelve lines at most,
  and no more than the screen has room for under the row.
- **The field** shows what is left of a calendar's name in grey after the text, as it shows an app's; Right takes it.
- **The window.** "Calendars" is a row of Privacy › What Booklight may use, with "Allow…" as its word, like
  Brightness. Results has a group "Events": a switch, and a menu button for "New events go to", which stands
  where the search engine's stands at every width (both at the rows' ends, or both under their text): the search
  engines' names decide which, and the calendar's button takes the room that place leaves, its name cut inside
  the button where the room ends.

