# Night two: motion review (your usual, flights, the copy again)

Seat: motion designer. Branch `m1-the-copy`, read only.

What I did: stepped through all seven films frame by frame (zM 222 frames, zS 258, zF 88, fA 269, mA 379,
mB 395, mS 468), measured the glass's lower edge and how far it is open in every frame around each change,
compared neighbouring frames pixel by pixel to find which frames the panel really drew, and read the code
behind each transition (`Panel.kt`, `Rows.kt`, `Bodies.kt`, `Field.kt`, `Footer.kt`, `OverlayModel.kt`,
`providers/Flights.kt`, `Motion.kt`). Scale 1.5 px per dp. Frame numbers are sixtieths of a second.

Three things to know before reading the numbers:

- **The frames are not evenly spaced in real time.** Every spring shows a three-frame beat (zS f124 to
  f140, the lower edge: +11, +9, +5, +16, +12, +6, +13, +19, +12, +7 px). The glass's own width, which
  is drawn inside a window that does not change, has the same beat (zS f103 to f122). So it is the
  capture, not the window. I do not report it, and nothing below depends on steps being even.
- **The films are from the debug build.** Where that matters I say so.
- **The working tree was being edited while I read it** (`OverlayModel.kt`, `Panel.kt`, `Flights.kt`
  have uncommitted changes). Line numbers are as I read them; go by the function names.

## Findings

| # | Severity | Feature | Where | What | Fix |
| --- | --- | --- | --- | --- | --- |
| 1 | must fix | flights | `Footer.kt` 59 to 61, the footer's left word | "AirLabs · 1:49 AM" is uncovered letter by letter, cut through its letters, for 17 frames | `.using(null)` on that transition |
| 2 | must fix | copy | `Field.kt` 87, the chip's room | On Tab the caret runs through the chip's name for 5 to 6 frames | The room is taken at once for a chip nobody typed |
| 3 | must fix | copy | `OverlayModel.stream` (684), `Bodies.kt` `Written` (166 to 168) | A two-line answer ends in a four-line row: 44 dp of empty pill | `trim()` the text while it arrives; count only lines that hold letters |
| 4 | should fix | usual | `Panel.kt` 501, the footer's alpha | The footer is cut by the glass's lower edge as the rows arrive | Its alpha waits for its band |
| 5 | should fix | usual | `Panel.kt` 473, the body's clip | Rows two and three are cut by a hard line while they rise | A soft lower edge of 20 dp while the height grows |
| 6 | should fix | flights | `Bodies.kt` `SlotsBody` | The answer lands as a cut: three lines change in one frame | Cross-fade 70 out, 110 in, where they stand |
| 7 | should fix | flights | `Rows.kt` `RowSlots.sync` 118 to 124 | Each digit makes a new row: the old one fades and the new one rises in the same seat, the texts doubled | The flight's full row keeps its slot |
| 8 | should fix | flights | the film itself, `DebugReceiver.kt` 109 | 25 keys a second on the debug build: the panel drew 7 frames in 24 | Film again at 120 ms a key and on the release build |
| 9 | should fix | all three | `Rows.kt` `Trail` (516) | A strip that leaves is cut in one frame, also four times slower | `Trail` equal by its key; strip to strip, the new one waits 60 ms |
| 10 | could | usual | `Footer.kt` 97 | "↓ Choose" and "tab Actions" lie over each other for a frame | Out 40 ms, then in |
| 11 | could | copy | `Bodies.kt` 136 | The answer's first word is written over the question while it fades | The answer's fade starts 70 ms later |
| 12 | could | copy | `Rows.kt` `RowSlots.sync`, Backspace on an answer | The rows that come back rise inside the pill that is still shrinking | They start 100 ms later |
| 13 | could | copy | `CopyLine.kt`, the mark | The line's glyph and row one's glyph lie over each other in the disc | The glyph goes with the words; only the disc stays |

### 1 · must fix · flights · the source is uncovered letter by letter

Frames: fA f251 to f268 (the answer landed at f248), and again in mA f318 to f334 (Down onto the
flight's row). f251: three dots, the tops of letters. f252: "AirLa", upper halves only. f254: "AirLabs ·".
f256: "AirLabs · 1:4". f259: "1:49" and half an A. f262 and f264: cut through the M. Whole at f268. That
is 283 ms of cut letters at the panel's lower left, on every answer and every time the pill comes onto an
answered flight.

Cause: the `AnimatedContent` at `Footer.kt` 59 has no `using`, so its box grows from nothing to the
word's size on the library's own spring, with its clip. It is the defect of M1's finding 6 (the
placeholder), in another place. "Copied" has always come this way; it is short and has a check before it,
so it was not seen. The source is 17 characters and comes without a key press.

Fix: `Footer.kt` 60:
`((fadeIn(motion.fade(120)) + slideInHorizontally(motion.place()) { -slide }) togetherWith fadeOut(motion.fade(80))).using(null)`.
The word is left aligned in a box that takes the footer's free width, so nothing else depends on its size.

### 2 · must fix · copy · the caret runs through the chip

Frames: mA f196 to f201 ("Dinner with |Anna", "Anna| on", "o|n Friday", "Fr|iday", "Frida|y"), at the
chip's end at f202, in its place at f204. mB f160 to f164. mS f228 to f239. In mS f224 and f226 the old
placeholder also slides 25 and then 55 px to the right while it fades.

Cause: this is new, and it comes from the fix of M1's finding 5. The chip no longer slides: it is drawn at
its full width where the G was. Its room still grows on `place` (`Field.kt` 87), and the text field
starts where the room ends. So the caret starts at the G and travels through the chip's name.

Fix: `Field.kt` 87: the room is taken at once when the chip that comes or goes has no keyword.
`.using(SizeTransform(clip = false) { _, _ -> if (typed) motion.place() else snap() })`, with `typed`
also true when a typed scope is left (`initialState?.keywords` not empty). The caret is then right of the
chip from the first frame. With the room there at once, the new placeholder's wait (`Field.kt` 106) can go
back from 160 to 60 ms: today the field holds only the chip and the caret from mA f196 to f204.

### 3 · must fix · copy · a two-line answer in a four-line row

Frames: mS f402 to f430. The answer is "On Friday, we will meet at the office at 3 PM. / Please bring the
signed forms and your laptops.", two lines. While the second line is written the row grows from 92 to
136 dp (the glass from 318 to 384 px) and stays so: f432 to f521 show two lines of text and 44 dp of empty
pill under them, the mark and the strip on the first line. In mB the same answer stays at 92 dp (318 px,
f232 to f395). The README of the first review lists this as done; it comes back by another way.

Likely cause (read from the code, not proved): a piece of the answer that ends in a line break.
`OverlayModel.stream` puts `text.toString().trimStart()` while the answer arrives; `Plain.of` keeps a
last empty line; `Written` counts `lineCount`, gets 3, calls `onGrow`, and `put` keeps a grown row grown.
The last `put` trims, but the row is tall by then.

Fix, both: `OverlayModel.kt` 684: `text.toString().trim()`. `Bodies.kt` 168: count a last line only if it
holds something: `lines = it.lineCount.let { n -> if (n > 0 && it.getLineEnd(n - 1, true) == it.getLineStart(n - 1)) n - 1 else n }`.

### 4 · should fix · usual · the footer is cut by the lower edge

Frames: zF f68; zM f79 (the caps' tops show at the rim) and f80 (the line sits on the rim); zS f134 to
f136, clear from f137. At that moment the glass is 158 to 174 dp high and the footer is at about half
strength.

Cause: the footer's band is taken only from what the window has beyond row one's seat (the fix of M1's
finding 1). Between 140 and 184 dp the band is not whole, and the footer hangs below the window. From the
copy's line that range is passed before the footer shows. From the field's height it is passed at 50 to
80 ms, with the footer's fade well on its way.

Fix: `Panel.kt` 501. The caps stand at 11 to 33 dp of the 44 dp band, so they are whole once three
quarters of it are there: `alpha = foot.value * edges() * smooth(0.75f, 1f, ((windowPx - fieldPx - seatPx) / footerPx).coerceIn(0f, 1f))`,
`seatPx` being pad + row + pad as at line 478.

### 5 · should fix · usual · rows two and three are cut while they rise

Frames: zF f69 (row two), f72 (row three). zM f81 (the top of the Files icon), f83 and f84 ("Sheets" and
"App" cut through their middle). zS f140 (row two), f150 to f155 (row three). The cut is the body's lower
bound, the top of the footer's band. Row three's seat is whole only when the window has all of its 288 dp,
and its rise starts 44 ms after the offer.

The spec's own sentence is "the panel makes room first, then the rows show". A delay for all three would
make that true and would be worse: the glass would grow empty for 120 ms, which is the slab. So the edge
should uncover softly, as it does under an opened row.

Fix: `Panel.kt` 473. In place of `clipToBounds()`: an offscreen layer and, while `height` is short of
`Metrics.height(model)`, a 20 dp gradient at the lower end drawn with `BlendMode.DstIn`, exactly as
`ActionBlock` does (`Rows.kt` 302 to 311). The same clip stands under every list that grows, so the
soft edge serves a first typed letter as well; no film here shows that case from the field's height.

### 6 · should fix · flights · the answer lands as a cut

Frames: fA f237 and f248 are neighbours (the recorder wrote nothing between). In f248 the caption has
grown by " · San Francisco → Frankfurt/Main", "LEAVES –" is "LEFT SFO Thu 2:47 PM", "LANDS –" is
"LANDED FRA 10:16 AM", and "Looking it up" is "Landed 9 min early · Terminal 1, belt 3". Nothing fades,
nothing is written in. On the selected row that is a pop.

What holds in the same frames: "Lands" keeps its x (432 px, 288 dp, before and after; also in stills 08
and 13). The row, the pill, the mark and the strip do not move. The height does not change.

Fix: `SlotsBody` cuts on purpose, because its values mirror typing. A row that is answered from elsewhere
is not typing. For such a body (`first > 0`, or a flag set in `FlightsProvider.full`): the column's
content in one `AnimatedContent` keyed on `b.note`, `(fadeIn(motion.fade(110, 40)) togetherWith fadeOut(motion.fade(70))).using(null)`.
The note is "Looking it up" for every number that is still being typed, so digits still cut, and the
answer and a failure both fade. This is row 8 of the motion table ("a re-parse cross-fades the values
concerned").

### 7 · should fix · flights · a new row for each digit

Frames: fA f195 to f199. f195: the full row of "LH 45" at a quarter strength, the web row for "LH455"
across its third line. f197: the caption reads "LH 455 · L" over "LH 45 · Lufthansa", two rows in one
seat. The pill's lower edge is at 77, 77, 83, 87, 89 dp (f195 to f199) while the row is laid out at 92:
"Looking it up" stands under the edge in f195 to f197. The strip slides in again.

Cause: the row's id is `flight:` plus the number (`Flights.kt` 212), so `LH45` and `LH455` are two rows
to `RowSlots`. And `Pill` (`Rows.kt` 270) takes a taller row in the same seat for "its row grew".

Fix: `RowSlots.sync`: a full flight row keeps its slot. Key the slots by
`if (r.provider == FlightsProvider.ID && r.body is Body.Slots) FlightsProvider.ID else r.id` in `live`
(118), in the lookup (124) and in `ids` (128). The texts then change in the frame of the key, the strip
stays (its action ids are the same), and nothing is drawn twice. The plain row of a guess keeps its own id.

### 8 · should fix · flights · the typing cannot be judged from fA

`./bl debug keys` types a key every 40 ms. The panel drew at f173, f179, f183, f186, f189, f195, f197 and
at no frame between (the frames between differ by compression noise only, at most 10 of 255). So each
letter cost one frame of 50 to 100 ms, and a list landed 4 to 6 frames after its letter (the 4 at f179,
its list at f183; the first 5 at f183, its list at f189; the second 5 at f189, its list at f195). In zM
the letter f is at f258 and its list at f262. The usual rows stand under "L" and then "LH" for 9 frames
(f170 to f178) because each key cancels the search before it.

Nobody types 25 keys a second, and the debug build composes slowly, so what flickers between f173 and
f197 is not what a user sees. But "never hold up typing" is not shown either.

Fix: give `keys` a pace (`DebugReceiver.kt` 109, `40L`), film `LH455` at 120 ms a key on the release
build, and step through it. If a letter still costs more than one frame, the first composition of the
flight's row is where I would look.

### 9 · should fix · all three · a strip that leaves is cut

Frames (contrast in the strip's place, of 255): zM f202 198, f203 72 (Down, row one's strip). zM f337
159, f338 16 (Backspace). mS, four times slower: f345 200, f346 4 (Ask gives way to Copy), and Copy only
starts at f347: the row's right end is empty for 2 frames. A 60 ms fade is 14 frames in mS.

Likely cause: `Trail` (`Rows.kt` 516) is a plain class without `equals`. `ResultRow` is composed again in
every frame while its colours move, each time with a new `Trail`. `AnimatedContent` takes that for a new
target, and a transition that is given a new target drops the state it was leaving.

Fix: `override fun equals(other: Any?) = other is Trail && other.key == key` and `hashCode`. The 60 ms
fade then runs as written. For strip to strip (an answer lands) the new one must then wait, or two panes
stand in one room: `Rows.kt` 493, `fadeIn(motion.fade(110, 60))`. That also settles finding 9 of the
first review without a second pane.

### 10 to 13 · could

- **10.** zM f162, zS f368 to f372: the `↓` cap and the `tab` cap, "Choose" and "Actions", are drawn over
  each other. `zero-state.md` Z2 names it and its cure. `Footer.kt` 97:
  `fadeIn(motion.fade(120, 40)) togetherWith fadeOut(motion.fade(40))`.
- **11.** mB f302 and f303, mS f353 to f360: "On" is written over "Am Freitag …" while that fades.
  `Bodies.kt` 136: `if (b.answer) motion.fade(110, 70) else motion.fade(70)`.
- **12.** mS f524 to f532: "Fix spelling" and its label stand inside the pill, which is still on its way
  from 136 to 56 dp. In mB (92 dp) it is one frame, f397. `RowSlots.sync`: when a standing row's height
  gets smaller in this change, new rows get `delay + 100`.
- **13.** mB f163, mS f230 to f237: the clipboard and the sparkle are drawn over each other in the disc.
  Let the line's glyph go with its words (70 ms) and only the disc stay for 140.

## Your usual: what holds

- **The opening is the field's height.** The lower edge is at 102 px (68 dp) until the glass is 95 to 96 %
  open: zF f61 to f64, zM f70 to f76, zS f104 to f120. No slab. The height leaves 68 dp three to four
  frames after the gate (zF f65, zM f77, zS f121).
- **Nothing is cut at either side.** Row one is first seen with the glass 99 % open (zM f79, right edge
  at 1075 of 1080 px). The G comes with the last stretch. The lower end is findings 4 and 5.
- **The cascade** is seen at Fast and Medium (zF f67, f70, f73; zM f79, f82, f85) and at four times
  slower (zS f130, f142, f156).
- **The field's `esc` cap never shows**: not in one frame of zF f60 to f94, zM f66 to f100, zS f100 to
  f180.
- **Down: the highlight fades in in place.** zM f161 to f165, zS f366 to f378. It does not fly. The kind
  goes first, the strip comes from f167 (zS f380).
- **Between rows it travels**: zM f204 to f209, one pill, stretched over both rows in f204 to f206.
- **The letter f**: the letter at f258, the rows stand until f262, then one change. Files keeps its row
  and travels to seat one with the pill and its strip (f263, f264). The height goes 288, 292, 339, 383 dp
  and on: no dip.
- **Backspace**: zM f336 to f349. The same three rows; the pill fades where it stands (f338 to f340); the
  lower edge comes down from beyond the film's own edge through 453, 366, 332, 317, 298 and 293 to 288 dp
  (f340 to f349) and never visits 68. In f339 and f340
  "Booklight settings" rises where Files still is, for two frames: the list's own way, not new.

## Flights: what holds

- The row is 92 dp from its first frame, with its three lines and its strip (fA f198).
- "Lands" keeps its x. The height does not move when the answer lands (436 dp, f237 to f521).
- No light of work: the answer came 880 ms after the list, under the 600 ms after the request.
- Tab along the strip: fA f392 to f402 and f465 to f477. One pane, it travels, the names unroll with it.
- The reflection runs once, f300 to f362, after the answer.

## The copy: the first review's findings, checked again

| # | Finding | Now | Frames |
| --- | --- | --- | --- |
| 1 | The line is cut out, the pill a sliver | Fixed. The line fades where it stands over three frames; the pill comes whole | mA f195 to f198, mB f160 to f163, mS f224 to f236 |
| 2 | Enter: content jumps, lies over row two | Fixed. The row grows with its pill, mark and strip travel, the name fades out before the question fades in, Gemini keeps its slot, the footer says "tab Actions" throughout. Backspace the same, backwards | mB f222 to f232 and f396 to f404, mS f336 to f365 and f522 to f546 |
| 3 | The text wraps again | Fixed. Line one ends "… im Büro, bitte" before and after the strip changes | mB f298 to f305, mS f345 to f355 |
| 4 | The line's parts cut as it arrives | Fixed. The glass grows empty f98 to f103, the disc from f104, the text from f106, the cap from f108 | mA f98 to f110 |
| 5 | The chip flies in | Fixed: no slide. But see finding 2 above | mA f196 to f199, mB f160 to f163 |
| 6 | The placeholder cut through its letters | Fixed. It is whole in every frame and slides about 42 dp as the mark's room closes | mB f464 to f470 |
| 7 | 100 ms of the Tab never shown | Better here: one frame's worth is missing (the edge goes 210, 208, 299, 381, 420, 493 px; a step of 91 where 30 then 63 are due). Debug build, another device: still to be judged on the release build | mA f194 to f199 |
| 8 | The age changes | Fixed: "Copied just now · text" | mB f469 to f486 |
| 9 | Two panes when the answer lands | Left on purpose. Now the two lie over each other in the fixed room for one frame; in mS the old one is cut instead. See finding 9 above | mB f301, mS f345 to f347 |
| 10 | The lower edge 1 px off | Left on purpose. Here 2 to 3 px (434 for 432, 741 for 738) | zM f91 to f97, mA f210 to f218 |
| 11 | Three lines in a four-line row | Not in these films. But see finding 3 above | |

What the fixes brought that is new:

- **The caret through the chip**: finding 2.
- **The field with only the chip in it**: mA f196 to f204, the new placeholder from f205, whole at f208.
  Not a defect by itself; it gets shorter with finding 2.
- **The name and the question in the cross-fade**: they overlap only four times slower (mS f340 to f342,
  both under a fifth of their strength). At speed no frame holds both (mB f223 the name, f224 the question).
- **The strip's fixed room** holds. No text is laid out again.
- **Backspace to the line**: mB f462 and f463 show the G, the `esc` cap and an empty glass 231 and 219 dp
  high: two frames with nothing in the field and nothing under it. The placeholder comes from f464.

## Not judged from these films

- Whether any spring is even, and any lag under one frame: the capture's own beat hides it.
- Flights: a guess gone to (`ps5`, Down onto it, the row growing from 56 to 92 dp with its pill): no film
  has it. The row without a key in motion. No answer, the dimmed actions. The light of work. The keyword's
  chip. A day after the number. Enter on Copy before the answer. The pinned flight's figure rolling. The
  window's group. Typing at a person's speed (finding 8).
- Your usual: Up back to rest. The pointer. "Don't suggest" and the opened list (Z5). Closing and the key
  again (Z6). The late path, at the 320 ms mark. Animations off, the opening set to Off, the dark theme
  in motion. A kept row with a second line.
- The copy: the words becoming known late. An answer that truly needs three or four lines. Finding 7 on
  the release build.
- zM f203, mA f373 to f381 (the letter d): 2.0's list changing under the selection, apart from finding 9.

## Verdicts

- The copy: approved with the must-fixes
- Flights: approved with the must-fixes
- Your usual: approved
