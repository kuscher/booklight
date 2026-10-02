# Booklight: the zero state, "Your usual"

*Design lead's spec, second pass, 2 October 2026, for issue kuscher/booklight#1. One design made from five
proposals (product, interaction, visual, motion, engineering) and the research, then checked by two critics
(28 findings; section 12 says what was done with each). Desk work: every measurement and every function name
below was read in the code on the branch `m1-the-copy`, and the times were worked out from the curves in
`Motion.kt` and `Panel.kt`. No device was touched, so what only a device can show is a check in section 9.
Names that do not exist yet are marked "new". The option defaults to off: tips, as today. Nothing here is
released.*

*As built that night, the spec was followed with three cuts, the first two the ones its own section 8 names for a
short night: (1) a thing whose row has a second line (a link, a recipe, a page of Booklight's with a line under
its name) is not suggested, so every row is one line without the second line's fade; (2) whether a copy is fresh
is asked at the gate, on the main thread, and if the window has no focus by then the rows come at the 320 ms
mark with the copy's line and the tip; (3) an opened list closes when something is run from it with Shift held (the highlight stays on its row). Two
more differences came out of the reviews of the built thing: on an app, "Don't suggest" stands in its list as the
last line before Uninstall, not the first, so that every other line keeps the place it has on any other app; and
a holder of one of the first two seats that has had no row for ten minutes gives its seat up (a stored time,
`zeroAway`), where the spec kept the seat for as long as the thing still weighed enough.
Tried on the Lenovo, in the debug and the release build, by keys and in films; `docs/design/reviews-night/` has what three
designers found in it. Of section 9, the HP, German, the screen reader and the pointer are still to do.*

*State of the branch: the first steps were committed while this was written, to the first pass (3a36ea7 and
b2fbc8d): `Zero.kt` and `ZeroTest.kt`, `History.items()` and `History.faded()`, `byIds` in the engine and in
five providers (`zeroState` is gone), and the three stored settings. Section 8 says what in them changes.
Flights (M5) was merged into the branch in the same hours; the model gained `went()`, `look()` and
`Action.off`. I read the merged code again: nothing below depends on what it changed.*

## 1. Promise and story

**Promise.** Open Booklight, type nothing, and the two things you run most, and the one you ran last if there
is one, are one Down and one Enter away.

**The story.**

1. Alex presses the Booklight key. The glass opens at the field's height, 68 dp, as on every opening.
2. As the glass lands, three rows rise under the field: Chrome, Gmail, Bluetooth. Three names on one rhythm.
   Nothing is highlighted, and Enter does nothing.
3. He presses Down. The highlight fades in on Chrome, where it stands. Enter opens Chrome.
4. Another day he types `g` instead. Gmail keeps its row and moves up; the rest changes as in any list.

The honest limit: a learned letter (`c`, Enter) is already two keys. The gain is seeing instead of recalling.

## 2. What it shows

Two or three ordinary result rows: the rows typing would find, with the same icons, names and actions, drawn
on one line each. Two or three, never one.

### The seats

| Seat | What | Exact rule |
| --- | --- | --- |
| 1 | Most used | The eligible thing with the highest weight, if its weight is 1.5 or more |
| 2 | Second most used | The next highest, weight 1.5 or more |
| 3 | Most recent | Of the rest, the one run last, if that was within 8 hours. Once is enough. If there is none: the third highest weight of 1.5 or more. If there is none of those either: no third row |

Seats 1 and 2 must both be filled, or nothing is shown. Seat 3 is there when there is something for it.

The issue's "most contextual" is the line for a fresh copy (M1). It is the one context Booklight has. Section 3
says how the two share the place.

### The formula, with its numbers

| Quantity | Value | Where |
| --- | --- | --- |
| Weight | count × 0.5 ^ (days since the last run ÷ 28). Each run adds 1 to the faded count | `History.faded` (on the branch; `History.weight` uses it), `History.HALF_LIFE_MS` |
| Earned | weight ≥ 1.5 | `Zero.EARNED` |
| In plain words | Run twice: earned for 11.6 days. Run three times: earned for 28 days | 28 × log2(2 ÷ 1.5) and 28 × log2(3 ÷ 1.5) |
| Recent | The last run was between 5 s ahead of now and 8 hours ago (28,800,000 ms) | `Zero.RECENT_MS` (12 h on the branch: change), new `Zero.AHEAD_MS` = 5,000 |
| A run "from the future" | A last run more than 5 s ahead of now (the clock was set back) is not recent. Its weight is its count, unfaded, as `History` already counts it | The same 5 s as `Clip.offer` |
| Fewest rows | 2 | new `Zero.MIN` (replaces `SEATS - 1` and the "three or none" return) |
| Most rows | 3 | `Zero.MAX` (today `SEATS`) |
| Ties | The one run later comes first, then by id | The same order on every opening |
| While open | Ranked once, off the main thread, before the rows are offered. Never reordered while the panel is open | |

A row run from the usual rows counts as a run, like any other (`History.record` with empty text: the count goes
up, no typed text is learned).

### Holding a seat, as a procedure

`Settings.zeroHeld` (new) keeps the ids of seats 1 and 2 as last shown, in order. `Zero.HOLD` = 1.2.

1. The held ids that are still earned, live and not hidden take their seats, in the order they sat.
2. Every other earned id, heaviest first: if a seat is free, it takes it. If both are taken, it takes the seat
   of the lighter of the two only if it weighs 1.2 times as much or more. It stands where the one it pushed
   out stood.
3. If no held id is left among the two, they stand heaviest first. Otherwise they swap only when seat 2 weighs
   1.2 times seat 1 or more.
4. What is stored afterwards: the two ids as shown. Nothing is stored in an opening that showed no rows, or in
   which a held id was passed over only because it is not live (an app in the middle of an update, a list
   still loading): a seat is never lost that way.

| Example | Held | Weights | Result | Why |
| --- | --- | --- | --- | --- |
| A newcomer just under | A, B | A 6.0, B 3.0, C 3.57 | A, B | 3.57 < 3.6 |
| A newcomer at the mark | A, B | A 6.0, B 3.0, C 3.6 | A, C | C takes B's seat |
| The second catches up | A, B | A 3.0, B 3.5 | A, B | 3.5 < 3.6 |
| The second passes | A, B | A 3.0, B 3.6 | B, A | Swap at 1.2 times |
| The first is pushed out | A, B | A 3.0, B 3.5, C 3.7 | C, B | C takes A's seat (3.7 ≥ 3.6). B keeps seat 2: "Down, Down, Enter" still means B |

Without the hold, two things run every day would swap seats every day, and "Down, Enter" would mean something
else each time. This is the loop `Zero.kt` on the branch already has; the last line of the table was pinned
by no test, and now is (test 12b).

### What counts as a run

Today every action that succeeds counts (`OverlayActivity.run` calls `model.learn(r)`), also App info, Edit,
Copy link, and an Uninstall that is then cancelled in Android's own dialog. With "run last" on screen that is
wrong: `zoom uninstall`, Enter, Cancel would hold seat 3 for hours.

**New rule, for every list:** an action counts as a run unless it is dangerous (`danger`), or is App info, Edit
or Delete. Copy link still counts. This is one of four places where the path with the option off changes
(section 8, "What changes with the option off").

### What can be suggested, and what never is

| Suggested, by id | Never suggested |
| --- | --- |
| Apps, `app:` | Typed text, web searches, addresses, sums |
| Settings pages, `setting:` | Notes, tasks, snippets, pins, timers |
| Booklight's own pages, `command:` | Prompts and their answers |
| Other apps' commands, `cmd:` | What was copied (the copy's line is its own thing) |
| Your links, `link:` | Controls `dial:`, keyboard shortcuts `key:`, keywords `scope:` |
| Your recipes, `recipe:` | A link that uses `{clipboard}`: making its row would read the clipboard |
| | Anything you said "Don't suggest" for |
| | Anything that is gone: a removed app, a deleted link, settings pages or app commands switched off in the window |

Something that is gone is passed over, and the next candidate takes the seat.

### One line per row

In the usual rows every row is one line: its icon, its name, its kind. A link's address and a recipe's steps
are not shown, at rest or highlighted. Two reasons: the three titles stand on one rhythm (a second line lifts
its title about 9 dp), and no address is on screen at every opening. The second line comes back when the row
stands in a typed list (section 5, Z3).

### Day one

Fewer than two earned: no rows. The panel is then as it is with the option off: a tip while tips are on and
one is left, else the field alone. The smallest history that gives rows: two things run twice each.

### Removing one

Each usual row has one more action: **Don't suggest** („Nicht vorschlagen“).

| | |
| --- | --- |
| Where, on an app's row | Behind the arrow, the first line of its list. The keys: Down, Right six times to the arrow, Enter, Enter. The way there never passes Uninstall |
| Where, on any other row | The last icon of the strip, before anything that removes |
| In typed lists | A thing that holds a seat in this opening carries the action in every list of this opening, in the same place. A row that stays when a letter is typed then changes nothing |
| Keys | Never armed by default. Never run by an arrow key or by Ctrl + digit. One Enter, no second Enter: nothing of yours is deleted |
| What you see, in the usual rows | The row fades, the highlight fades out where it stands, the other rows close up, the footer says "Won't be suggested". The panel is back at rest: `↓` Choose. No new row comes in this opening |
| What you see, in a typed list | Only the footer's word. The row stays: it matches what was typed |
| Next opening | The next candidate has the seat |
| What is stored | The id goes into new `Settings.zeroHidden`. `History.forget` is not used: it would also drop what `c` has learned |
| Undo | In the window: "Suggest everything again" |
| All of it off | The switch |

## 3. Behaviour

### One rule for the place under the empty field

**At most one thing stands under the untouched empty field. It is chosen once per opening, in this order.
Typing puts it away.**

| # | What | Its condition | When it comes | Panel |
| --- | --- | --- | --- | --- |
| 1 | A first-run card | A card is due (`OverlayModel.card`) | At the gate (glass 85 % open), as built | 172 dp |
| 2 | The copy's line | Something was copied in the last 2 minutes, and its switch is on | 320 ms after the gate, as built | 140 dp |
| 3 | Your usual | The switch is on, two or three seats are filled, and the system has said there is no fresh copy | At the gate; or, if either answer is late, the moment both are known; at the latest 320 ms after the gate | 288 dp (232 with two rows) |
| 4 | A tip | Tips are on and one is left, and none of the above came | 320 ms after the gate, as built | 172 dp |
| 5 | Nothing | Otherwise | | 68 dp |

- The opening itself is always 68 dp. Whatever is in the table comes after it.
- "Untouched" is the guard the copy's line already has, in full: not a demo, not a guided example, nothing
  typed in this opening, the field empty, no chip, no rows, no card, no tip, no line, no opened row. So text
  handed over by another app, Up before the gate, and an example Booklight types all keep the rows away.
- The order is one pure function in `core` with a truth-table test (new `Under.choose`, section 8). The app
  module has no tests, and this is the part that must not go wrong.
- Whether there is a fresh copy is asked once, off the main thread, when the window gets the focus (the
  clipboard answers only the window in front). Until the answer is in, the rows wait. They never lock the
  copy's line out.
- A card comes back when the field is emptied again (as built: `card` is worked out from the field). The line
  and a tip stay away once typing has put them away (as built). The usual rows come back whenever the field is
  empty again, if they stood in this opening.
- With the switch off, none of this runs: no job, no look at the clipboard on focus.

### State by key

| Key | Empty field, rows not there yet | Rest: rows, no highlight | A row is highlighted |
| --- | --- | --- | --- |
| Enter, Shift + Enter | Nothing. Not kept for later | Nothing. Not kept for later | Runs the armed action. With Shift the panel stays, the rows stay, and an opened list stays open |
| Down | Nothing. Not kept for later | The highlight fades in on row one, where it stands | Next row. Stops on the last |
| Up | The last text that was not run comes back (as today) | The same, on a press of its own | Previous row. From row one, on a press of its own: the highlight fades out, back to rest. On the row of an opened list: nothing |
| Tab | Opens a fresh copy, as today. Otherwise nothing | As Down, on the first press only | Next action, wrapping |
| Shift + Tab | Nothing | Nothing | Previous action, wrapping |
| Right, caret at the end | Nothing | Nothing | Next action, no wrap. On the arrow, Right again opens its list |
| Left | Nothing | Nothing | Previous action. Closes an opened list |
| Ctrl + 1 to 3 | Nothing | That row's first action | The same |
| A letter | It is typed. The rows do not come in this opening | It is typed in the same frame. The rows stand until the typed list lands, then one list change (section 5, Z3) | The same. The highlight is on row one of the typed list |
| Space | It is typed. The rows do not come | The rows stand | The rows stand; the highlight fades out, back to rest |
| Backspace | Nothing | Nothing | Nothing |
| Esc, the Booklight key, a click outside | Closes | Closes | Closes |
| Pointer moves over a row | | Once it has moved more than 4 dp on that row: the highlight fades in there | The highlight travels to it |
| Pointer rests | | Nothing: rows arriving under a resting pointer select nothing | Nothing |
| Click | | A row: runs its first action | An icon: that action. The row: its armed action |

- Enter, Tab and Ctrl + digit act on the first press only (`repeatCount`), as built. So do the two steps of Up
  named above: a held Up from row three stops on row one.
- Enter runs only what the highlight is on. The highlight comes only by a key or a pointer movement made while
  the rows are in the list.
- A typed space is still nothing typed. Space, Down, Enter runs the highlighted row. Space, Enter, `g` runs
  nothing: no Enter is parked while the field is blank.
- Tab at rest is Down, always: it acts on what is on screen. One corner follows from it: with "What you copied"
  switched off, Tab on the empty panel opens a fresh copy only until the rows stand (as built before them, Down
  after them).
- If the run fails (the app was removed while the panel was open), the footer says "Couldn't do that", as built.

### Accessibility

- When the rows arrive: one polite announcement, "Your usual: Chrome, Gmail, Bluetooth. Down arrow to choose."
  (two names when there are two).
- A row reads as it does in a typed list ("Chrome, Open"). Every action, "Don't suggest" included, is a custom
  accessibility action, as built.
- A removal is announced with the footer's words.
- The system's animations off: the rows are there in one frame, with no highlight.

## 4. Look

The zero state is the typed list at rest, one line per row. It adds no new size, colour or type style. New on
the panel: one footer hint, one action name, one footer word, one symbol.

```
x   0  8  20 38 56  72                                                                        700  712 720
  0 ╭──────────────────────────────────────────────────────────────────────────────────────────────────╮
    │        G      Search apps, settings and the web                                                  │ field 68
 68 │                                                                                                  │ pad 8
 76 │     [icon]    Chrome                                                                    App      │ row 56
132 │     [icon]    Gmail                                                                     App      │ row 56
188 │     (gear)    Bluetooth                                                            Settings      │ row 56
244 │                                                                                                  │ footer band 44 (pad 8 + footer 36),
    │                                                                    [↓] Choose   [esc] Close      │ its line centred on y = 266
288 ╰──────────────────────────────────────────────────────────────────────────────────────────────────╯

 68 │                                                                                                  │ pad 8
 76 │  ╭────────────────────────────────────────────────────────────────────────────────────────────╮  │ the pill: x = 8 to 712, radius 24
    │  │  [icon]    Chrome                                       [Open ⏎] (+) (i) (▢) (◧) (◨) (v)   │  │ row one after Down
132 │  ╰────────────────────────────────────────────────────────────────────────────────────────────╯  │
```

The figure is a sketch; the numbers on its edges are exact dp from the panel's left and top edge.

### Parts

| Part | Layout (dp) | Look |
| --- | --- | --- |
| Rows | Two or three of `Metrics.row` 56. Tops at y = 76, 132, 188. Centres at y = 104, 160, 216 | No fill, no outline, no divider |
| Icon | A 36 box, x = 20 to 56, centred on x = 38. An app's own icon fills it; anything else is a 20 dp glyph on a 36 dp disc | Glyph: ink 0.80. Disc: ink 0.08 light, 0.12 dark. Scale 1.0 at rest, 1.06 when highlighted (`pop`) |
| Title | From x = 72. 17 sp, weight 500, one line, centred on the row's centre line, ends in the 24 dp fade, never an ellipsis | `onSurface` 1.0 |
| Second line | None in the usual rows. In a typed list, as built: 14 sp, weight 500 | `onSurface` 0.80 |
| Right end at rest | The row's kind: "App", "Settings", "Booklight" (also for your links and recipes), or the owning app's name for one of its commands. `SMALL` 14 sp, weight 500, tracking 0.1. Right edge at x = 700 | `onSurface` 0.80 |
| Highlight | None at rest. After Down: x = 8 to 712 on the row's seat, radius 24 (the panel's 32 less its 8 dp inset) | `secondaryContainer` 0.78 light, 0.42 dark on glass, 0.66 dark on solid ground. White 1 px outline at 0.55 light, 0.30 dark |
| Highlighted row | The kind gives way to the strip of what the row can do, the armed action unrolled. The strip's last slot is a 32 dp box that ends at x = 700; its arrow is centred on x = 684 and its ink ends at 693 | As in any list |
| Footer | From arrival. A 44 dp band, y = 244 to 288. Right side, from x = 700 leftwards: "Close", 6, the `esc` cap, 16, "Choose", 6, the `↓` cap (27 wide) | Hints 13 sp, weight 500, ink 0.80. Caps 22 dp high, radius 7, fill ink 0.10 light, 0.14 dark |
| Footer, a row highlighted | The `↓` cap and "Choose" give way to the `tab` cap (about 34 wide) and "Actions", as in any list. A cap and its label are one piece in the code (`Footer.kt`), so the pair changes, not only the word. The `esc` cap and "Close" do not move | See Z2 in section 5 |
| Field's `esc` cap | Rows at the gate: never seen (it has not come yet). Rows later, on the 320 ms path: it has stood, and fades out in 120 while the footer's fades in over 140, as in every typed list. In between: it fades from what it has and never gets brighter once the rows are there | See Z1 in section 5 |

### Height

`Metrics.height` is not changed: `field + pad + listHeight(results) + pad + footer`.

| State | Sum | dp |
| --- | --- | --- |
| The opening, every time | 68 | 68 |
| Three rows, at rest or highlighted | 68 + 8 + 168 + 8 + 36 | **288** |
| Two rows (two seats filled, or one of three removed) | 68 + 8 + 112 + 8 + 36 | 232 |
| One row (after "Don't suggest", in that opening only) | 68 + 8 + 56 + 8 + 36 | 176 |
| None left (after "Don't suggest") | 68 | 68 |
| An app's list open: Don't suggest, 10 places, gap, Uninstall | 68 + 8 + 56 + 440 + 48 + 8 + 36 | 664 |
| The same for an app that came with the device (no Uninstall) | 68 + 8 + 56 + 440 + 8 + 36 | 616 |
| For comparison: copy line 140, tip or card 172 | | |

Down and Up never change the height. On the smaller of the two screens (1067 dp high, top edge at 20 %) the
664 dp panel ends at 877 dp.

### Light and dark

| Part | Light | Dark |
| --- | --- | --- |
| Glass veil (Balanced) | `surfaceContainerLowest` 0.42 | 0.50 |
| Titles | ink 1.0 | ink 1.0 |
| Kind, footer hints | ink 0.80 | ink 0.80 |
| Discs / key caps | ink 0.08 / 0.10 | ink 0.12 / 0.14 |
| Highlight | 0.78 | 0.42 on glass |

Nothing here is new, so the known weak spot is the old one: 14 sp text at ink 0.80 on dark glass over a white
window. It is a device check. If the kind labels have to go to full ink, they do so in every list, typed ones
too: a row that stays must not change its ink at the first letter.

### The longest German case

Widths are estimated from character counts, not measured.

| Text | Length | Result |
| --- | --- | --- |
| Kind „Einstellungen“ | about 96 dp | The title runs from x = 72 to about 592: 520 dp, about 57 characters |
| Longest settings title, „Display automatisch ausschalten“ | 31 characters, about 280 dp | Fits |
| Action „Nicht vorschlagen“ | 17 characters | Inside the action limit of 20. Unrolled on a settings row the strip is about 215 dp, under the 340 limit |
| Footer „Auswählen“ and „Schließen“ with both caps | about 212 dp | Fits; the left 468 dp stay free for the footer word |
| Footer „Aktionen“ in place of „Auswählen“ | about 10 dp narrower | The `tab` cap (34) and the `↓` cap (27) overlap for 80 ms: left edges about 3 dp apart, right edges about 10. Check 9 |
| Footer word „Wird nicht mehr vorgeschlagen“ | 29 characters, about 240 dp with its check | Fits |

### What must line up

1. On x = 38: the G, the icons.
2. On x = 72: the typed text, the caret's left edge, the titles. At rest there is no typed text: the only text
   on the field's line is the placeholder, and its box starts at x = 74 (risk 1).
3. On x = 700: the kind labels, the footer's "Close". After Down: the box of the strip's last slot (its arrow's
   ink ends at 693, as in every list).
4. On y = 104, 160, 216: the centres of the icons and of the titles. Every row is one line.
5. The highlight sits exactly on its row's seat, concentric with the panel.

### Risks to alignment

1. **The placeholder stands at x = 74, not 72** (`Field.kt` line 109: 2 dp of start padding on the placeholder's
   text). My reading of why: the caret is drawn at x = 72 and is 2 dp wide, and the padding keeps it off the
   first letter. At rest the eye compares the placeholder (box at 74, ink at about 75) with the titles (box at
   72, ink at about 73): a 2 dp step on every opening. Decision 9, and a pair of captures early in the build.
2. **App icons that load late.** `RowPicture` shows the bitmap when it lands, which is a pop after the row has
   risen. The icons are loaded before the rows are offered, at the size `RowPicture` asks for (48 dp in px).
3. **Unequal kind labels** give a ragged left edge at the right margin. Only their right edges are a line.
   Accepted, as in typed lists.
4. **Adaptive app icons and 36 dp discs** share a box but not an optical size. True of typed lists too; here
   all rows show it at once.
5. **Second lines.** A link's or a recipe's row has two lines in a typed list, and its title then stands about
   9 dp above the centre line. Three rows of mixed kinds would have titles at about 104, 151, 216. Settled by
   "one line per row"; what is left to look at is the second line's return at the first letter (Z3, check 7).

## 5. Motion

Nothing new in `Motion.kt`. The usual rows use the list's own rows (`RowSlots`, keyed by result id), the list's
pill and the list's footer.

### Times, in ms after the key

| Moment | Fast | Medium | Slow |
| --- | --- | --- | --- |
| The gate: glass 85 % open. The rows are offered, the height leaves 68 | 129 | 257 | 514 |
| The rows and the footer begin to show (92 % open) | 145 | 291 | 581 |
| The glass's edges pass x = 20 and x = 700 (94.4 % open) | 154 | 308 | 616 |
| Row one at 90 % | 270 | 399 | 715 |
| All three at 90 % | 315 | 443 | 727 |
| The glass at rest | 210 | 420 | 840 |
| If an answer was late: the latest the rows are offered (the gate + 320) | 449 | 577 | 834 |
| Then all three at 90 % | 635 | 763 | 1,020 |

- At the gate the glass spans x = 53.5 to 666.5. Its edge would cut the icons, the kind labels and the footer
  for 25 / 51 / 102 ms. So the rows and the footer take the same factor the field's G and `esc` cap have
  (`ends()`: 0 at 92 % open, 1 at 100 %). No new number.
- The panel makes room first, then the rows show, as a tip and the copy's line do.
- At Slow the three reach 90 % within 12 ms of each other (715, 719, 727): they come with the glass's last
  stretch, and the cascade is not seen. At Fast and Medium it is.
- Opening set to Off (a small settle and a fade): the rows are offered when the fade has landed, 170 ms after
  the first frame; all three at 90 % at 356 ms.

### Transitions

| # | Transition | What moves | Spec and delay | What stays still | Interrupted | Done carelessly |
| --- | --- | --- | --- | --- | --- | --- |
| Z1 | The rows arrive | Height 68 to 288 (232 with two). The rows rise 12 dp and fade in. The footer fades in on the window's lower edge | Height `place`. Rows `place`, `stagger` 0, 22, 44 ms from the offer. The body's `fade` 140 lies over every row. Footer `fade` 140. Body and footer times `ends()` | The field, the seam, the opening. Icons on x = 38, titles on 72: rows move in y only | A letter before the offer: no rows in this opening. A letter in mid-cascade: Z3; a row that leaves in mid-rise keeps the offset it had and fades | The height is 288 from frame one: a slab opens. An icon pops in after its row. An icon, a kind label or "Close" is cut by the glass's edge. The field's `esc` cap flashes |
| Z2 | The highlight comes, moves, goes | In: it fades in on its row, in place; the kind fades out, the strip slides in; the icon grows to 1.06; the `↓` cap and its label fade out, the `tab` cap and its label fade in, both ending 16 dp before the `esc` cap, nothing slides. Between rows it travels as built. Back to rest: it fades out where it stands; the strip fades out, the kind fades in; the icon settles to 1.0; the hint the other way | Pill `fade` 120. In: kind `fade` 60 out; strip `place` and `fade` 110, 60 ms late; icon `pop`; hint 80 out, 120 in. Travel `lead`, `trail`. Out: strip `fade` 60, kind `fade` 110, icon `pop` | Rows, titles, height, footer band, the `esc` cap | Down again during the fade: it travels from where it is | The highlight flies down from the field. The height changes on Down. Two caps at two places read as a smear (then the outgoing pair cuts, and only the incoming fades) |
| Z3 | The first letter | The letter, same frame. The rows stand until the typed list lands. Then one list change: a row with the same id keeps its row and travels; the others fade where they stand; new rows rise; the highlight fades in on row one. A kept row that has a second line: its title moves up by half that line's height and the line fades in | Kept rows `place`. Leaving `fade` 80. New rows `place`, 22 ms apart among themselves. Pill `fade` 120. Height `place`. Second line: title `place`, line `fade` 110, 40 ms late | The field. The footer stays shown and rides the lower edge (288 up to 568 with eight rows). A kept row is never faded again, and its strip does not change | The next letter: every spring retargets | The list is emptied first: the height dips to 68 and grows again. A second line pops in and the title jumps 9 dp |
| Z4 | The field is emptied again | Z3 backwards in one list change: typed rows fade, the usual rows that are left stand again, the highlight fades out where it stands, a kept row's second line fades out and its title settles | As Z3 | The footer stays shown and rides the edge. The height never visits 68 | Typing again: Z3 | The panel collapses and regrows |
| Z5 | Don't suggest | One list change. The row, and its open list if it has one, fade where they stand: no edge travels. The highlight fades out where it stands; it does not travel. The rows that are left take their seats; rows that had gone (the list was open) rise in at once, 22 ms apart. Height to 232 (176 with one left; 68 with none, and then the field's `esc` cap fades back in 120). The footer word slides in 8 dp with its check | Row and list `fade` 80. Pill `fade` 120. Others and height `place`. Word `place` and `fade` 120, held 1,600 ms | The field, every x. Afterwards nothing is selected: `↓` Choose | A letter: Z3 | The highlight flies 400 dp up from the list onto a row that fades under it. A row slides in under a standing highlight and a second Enter opens it. The glass stands empty for 200 ms |
| Z6 | Closing, and the key again | Nothing of their own: the glass folds over the rows where they stand | The fold: 105 / 105 / 210 ms; gone after 155 / 155 / 310, as built | Height, rows, highlight | The key again: the glass reopens, the rows are still there, no second cascade | The height returns to 68 first: two gestures |
| Z7 | The reflection | Unchanged: 2.4 s after the opening and 0.7 s after the last change. One lap is 2 × (720 + 288) = 2,016 ms (1,904 with two rows) | As built | Rows | A key during the lap: it fades in 160 ms while it goes on | It starts while the rows arrive (it does not: `results` is in its watch list) |
| Z8 | Keys before the rows | Nothing. Enter, Down and Tab are not kept | | Everything | | A kept key runs or selects a row nobody saw |
| Z9a | The system's animations off | Everything cuts: 288 dp and the rows in one frame, no highlight, no reflection. If an answer is late they come when it lands, in one frame | `snap` | All | None | A stagger left running |
| Z9b | The opening set to Off | The panel fades in at 68 dp (170 ms). Then Z1, unchanged | As Z1, from the end of the fade | The opening is the field's height | As Z1 | The height springs to 288 while the panel is still fading in |

### What to cut first, if the device shows it too busy

1. The stagger: the rows rise together.
2. The early offer: the rows come only at the 320 ms mark, with the copy's line and the tip.
3. The gate: the rows wait for the glass to rest (210 / 420 / 840 ms).

## 6. The option

Alex asked for "an option to show it or the tips, defaulting to the tips". So: **one new switch, off. His
"Show tips" switch and "Show the tips again" stay exactly as they are.** The first pass replaced his switch
with one choice of three values; that is now decision 3, not built without his word.

**Place.** The Booklight window, Start page, a new group under "Tips". The settings window that is being
rebuilt on its own branch must carry the same two rows in its Start section.

```
TIPS
(spark)  Show tips                                                                  [on]
         When you open Booklight and type nothing, it shows one thing it can do. ...
(again)  Show the tips again
YOUR USUAL
(list)   Show your usual                                                            [off]
         With nothing typed, Booklight shows the two things you run most ...
(again)  Suggest everything again                                      2 are not suggested
```

| String | English | German |
| --- | --- | --- |
| Group | Your usual | Das Übliche |
| Switch | Show your usual | Das Übliche zeigen |
| Its text | With nothing typed, Booklight shows the two things you run most from here and, if there is one, the one you ran last, in place of a tip. Down, then Enter. Until you have run two things twice each, nothing changes. This uses only what you ran from Booklight, and it stays on this device. | Ohne Eingabe zeigt Booklight anstelle eines Tipps die zwei Dinge, die du von hier am häufigsten startest, und, falls es eines gibt, das zuletzt gestartete. Pfeil nach unten, dann Enter. Bis du zwei Dinge je zweimal gestartet hast, ändert sich nichts. Dafür zählt nur, was du aus Booklight startest, und das bleibt auf diesem Gerät. |
| Row | Suggest everything again | Wieder alles vorschlagen |
| Its count | 1 is not suggested · %d are not suggested | 1 wird nicht vorgeschlagen · %d werden nicht vorgeschlagen |
| Action on a row | Don’t suggest | Nicht vorschlagen |
| Footer word | Won’t be suggested | Wird nicht mehr vorgeschlagen |
| Footer hint (`hint_choose`) | Choose | Auswählen |
| Announcement | Your usual: %1$s. Down arrow to choose. (the names, joined with commas) | Das Übliche: %1$s. Pfeil nach unten zum Auswählen. |

**Default: off.** A new installation and an updated one behave as today.

| Show your usual | Show tips | Under the empty field (a card and the copy's line come first in every case) |
| --- | --- | --- |
| off (default) | on (default) | Today: a tip |
| off | off | Today: nothing |
| on | on | The usual rows. With fewer than two earned: a tip, while one is left |
| on | off | The usual rows. With fewer than two earned: nothing |

- One new stored switch, `Settings.zero`, default false, and two lists, `zeroHidden` and `zeroHeld`. Two stored
  booleans, two switches on screen: every stored state has one picture, and the text says nothing that can be
  false.
- No change of the file's schema number, no migration (`ignoreUnknownKeys`, `encodeDefaults`).
- "Turn off tips" on a tip's card and "Show the tips again" do what they do today, and leave `zero` alone.
- "Suggest everything again" is dimmed while nothing is hidden.
- "What you copied" keeps its own group and switch, above.

## 7. Privacy

| Question | Answer |
| --- | --- |
| What is used | Booklight's own record of what was run from Booklight: for each thing its id, a faded count and the time of the last run (`files/history.json`). It is kept today, for ranking. Nothing new is recorded about what you do; App info, Edit, Delete and Uninstall stop being counted |
| What is new on disk | In `files/settings.json`: the switch, the ids you said "Don't suggest" for, and the two ids that held seats 1 and 2 last time |
| What Booklight does not know | What you open from the taskbar, the shelf or anywhere else. There is no new permission and nothing runs in the background |
| What leaves the device | Nothing. The rows are worked out on the device at each opening. They are never part of a suggestion request |
| The clipboard | Not read for this. With the switch on, Booklight asks for the copy's description (kind and age, the silent look M1 already makes) once more: when the panel gets the focus. A link that uses `{clipboard}` is never suggested |
| What someone looking at the screen can see | Each time the panel opens with nothing typed: the names and icons of up to three things you run from Booklight. Also in a screen share or a recording. Something run once shows for up to 8 hours as the third row |
| What they cannot see | A link's address, a recipe's steps, typed text, searches, notes, what was copied, how often or when anything was run |
| To hide one | "Don't suggest" on its row |
| To hide all | The switch |
| To start over | "Forget everything" clears the record, the hidden list and the held seats (decision 12) |

One sentence more for `PRIVACY.md`, under "On your device", and for the About page's text (`set_privacy_text`,
English and German): "If you turn on Show your usual, the empty panel shows the two things you run most from
Booklight and the one you ran last. This uses the same record; the only new things kept are the switch and the
list of things you asked not to be suggested." The data-safety form does not change: nothing is collected or
shared.

## 8. Build plan

### Files and functions

| File | Add or change |
| --- | --- |
| `core/.../Zero.kt` (on the branch, first pass) | `Zero.pick` now returns `Seats(picks, held)`. Changes: `MIN = 2`, `MAX = 3` (two earned and no third give two rows, not none); `RECENT_MS` 8 h; recent only if `now - last` is between `-AHEAD_MS` and `RECENT_MS`; `held` is null when a held id was passed over for `live` alone. New `Zero.offer(row, label)`: puts the action `Unsuggest` in its place (first behind the arrow if the row has one; else before the first dangerous action; else last), moves `armed` along if it pointed at or past that place, and does nothing the second time |
| `core/.../Under.kt` (new) | `Under.choose`, pure: what stands under the empty field. `UnderTest.kt` beside it |
| `core/.../ZeroTest.kt` (on the branch, first pass) | The cases below: tests 2, 6, 8 and 16 change (two rows where there was none; 8 hours; the clock); 11, 14 and 17 also look at `held`; 3, 12b, 12c and 18 to 21 are new |
| `core/.../History.kt` (on the branch) | `items()` and `faded()` are there. Add the case for `items()` to `HistoryTest` |
| `core/.../Model.kt` | `Provider.byIds(ids: Set<String>): List<Result> = emptyList()` is on the branch (it replaced `zeroState()`). New `Effect.Unsuggest(id)` |
| `core/.../SearchEngine.kt` | On the branch: `zeroState(limit)` is gone, an empty query returns nothing, `byIds(ids)` and `used()` are there, and its test is rewritten. Nothing more |
| `providers/AppsProvider.kt` | `byIds` is on the branch (it makes every app's row and then filters: fine off the main thread, wrong on it). New `ready`, done when the first read of the app list is in |
| `providers/AppCommands.kt` | `byIds` for `cmd:` is on the branch. New: its own `ready`, because its list is read by a job of its own and can be empty when the app list is in |
| `providers/Providers.kt` | `SettingsProvider.byIds` and `CommandsProvider.byIds` are on the branch. Nothing more. (`SettingsProvider.pages` asks the system once per page on first use: one more reason the job is off the main thread) |
| `providers/User.kt` | `byIds` for `link:` and `recipe:` is on the branch, and skips a link with `{clipboard}`. Nothing more |
| `BooklightApp.kt` | New `suspend fun usual(): Usual` (rows, the held ids to store or null, the time it took): waits for both `ready`, then `History.items()`, `Zero.pick`, `byIds`, `Zero.offer` on each row, and the app icons loaded with `AppIcons.load` at `(48 × density).roundToInt()` px (the cache is keyed by icon alone, so another size would be drawn at the wrong size). The model's job and `./bl debug zero` both call it |
| `data/Prefs.kt` | `Settings.zero`, `zeroHidden` and `zeroHeld` are on the branch. Nothing more |
| `overlay/OverlayModel.kt` | See "The model" below |
| `overlay/OverlayActivity.kt` | `AppIcons` is made before the model (today after it). `onWindowFocusChanged(true)`, with the switch on: the look at the copy, off the main thread, into `model.copyFresh`. `model.onSay = ::say`. `run()`: `learn` only for what counts as a run (section 2) |
| `overlay/Panel.kt` | The two moments (below). Down, Up and Tab call the model. The body's Box and the footer's Box take `if (folding) 1f else ends()` as a factor on alpha |
| `overlay/Rows.kt` | Four small changes: (1) a row that starts to leave keeps the offset it had (`translationY` is remembered once, not set to 0); (2) the "typed away" path of an opened list (`fading`) also when the opened row has left the list; (3) `ResultRow` takes how much of the second line shows (1 by default; 0 while the usual rows stand), which fades the line and moves the title by half its height on `place`; (4) at rest, `hover()` only once the pointer has moved more than 4 dp from where it entered the row. It already hides the pill when nothing is selected (`visible = picked != null`) |
| `overlay/Footer.kt` | The hint `↓` Choose while the usual rows stand with none selected |
| `overlay/Field.kt` | The `esc` cap never gets brighter once rows are there: its alpha is the smaller of `ends()` and what `ends()` was when the rows came (read without observing) |
| `overlay/Metrics.kt`, `Motion.kt` | No change |
| `Executor.kt` | `Effect.Unsuggest` joins the line "the panel does these itself" (the `when` is exhaustive) |
| `ui/Icons.kt` | One symbol, `hide` |
| `window/MainActivity.kt` | `StartPage`: the group "Your usual" with its switch and the row "Suggest everything again". `AboutPage`: "Forget everything" also empties `zeroHeld` and `zeroHidden` |
| `window/Stage.kt` | No change, but a check: it calls `type("")` on a demo model, and the blank branch must return nothing there |
| `res/values`, `values-de` | The strings of section 6, and the sentence in `set_privacy_text` |
| `DebugReceiver.kt`, `bl` | The hooks below; the list of commands in both headers |
| Docs | `CLAUDE.md` (the two rules below, and the layout notes), `docs/design/ux-model.md`, `docs/design/design-system.md` (a new section; §2's type table says label 13 and hint 12 where the code has 14 and 13; §3h says "key caps never move, only labels cross-fade" where the code changes the pair), `docs/design/milestones/04-design.md` §3.5 (the order of right under the field), `docs/design/milestones/plan.md`, `docs/design/window-redesign.md`, `docs/PICKING-UP.md`, `PRIVACY.md`, `CHANGELOG.md` |

The two rules in `CLAUDE.md` and `ux-model.md`, rewritten:

- "One panel, one ranked list. Row one is selected in every list that typing made. With Show your usual on, up
  to three rows stand under the empty field with none selected; Enter does nothing until Down."
- "Nothing typed = nothing shown (after the two first-run cards), unless Show your usual is on."

### The model (`OverlayModel.kt`)

| Part | What |
| --- | --- |
| The job | Started at creation only `if (!demo && settings.zero)`. `scope.launch(Dispatchers.Default) { val u = app.usual(); withContext(Dispatchers.Main.immediate) { usual = u } }`. The model's scope is the main thread's, so without the dispatcher the pick would run inside `onCreate` |
| `copyFresh: Boolean?` | Set by the activity. False at creation when "What you copied" is switched off |
| `zeroKnown` | True once the rows are worked out and `copyFresh` is not null. Both are snapshot state, so the panel can wait for it |
| `offerZero(last: Boolean = false)` | True if the rows already stand. It says no unless all of this holds: `!demo && !guided && settings.zero && !typedYet && query.isEmpty() && chip == null && results.isEmpty() && card == null && tip == null && copy == null && opened == null`, the rows are worked out, there are two or more, and (`copyFresh == false` or `last`). After a call with `last` it says no for the rest of the opening. Otherwise: `results` are the rows, `selected = -1`, `zeroUp = true`, `resultsFor = null to query`, `whenReady = null`, the held ids are stored if they are not null and changed. The yes or no comes from `Under.choose` |
| `zeroUp` | True only while the usual rows are `results` (with or without one opened). False from the frame a typed list lands |
| The blank branch of `search()` | If the rows stood in this opening: `results` are those of them that are not hidden, `selected = -1` (on `refresh()`: the selection is kept by id, and an opened row stays open, as the typed branch keeps it). Else as built. Always `resultsFor = null to text; whenReady = null`, as built |
| Seat holders in typed lists | A result whose id was offered in this opening gets `Zero.offer` before the list is shown |
| `down(again)` | The copy's line: `openCopy()` on the first press. A grid's cell. From rest: row one. Else `move(1)` |
| `up(again)` | A grid's cell. `selected > 0`: the row above. `selected == 0 && zeroUp && opened == null`: on a new press, `selected = -1`. `selected < 0` or no rows: on a new press, `restoreLast()`. `move()` stays as built |
| `enter()` | Before `run(r, a)`: `(a.effect as? Effect.Unsuggest)?.let { unsuggest(it.id); return }`. Routed through `run()` the footer would say "Couldn't do that" (the Executor answers false), or the hidden thing would be counted as run |
| `unsuggest(id)` | Stores the id in `zeroHidden`. While `zeroUp`: an open list is shut and the row leaves `results` in one change, `selected = -1`, `zeroUp` stays true while a row is left. In a typed list: nothing else changes. Then `onSay("Won't be suggested")` |
| `onSay: (String) -> Unit` | Set by the activity to its `say` |
| `learn(r)` | A blank field is recorded as empty text (no latch for a space) |

### The panel's two moments (`Panel.kt`)

```kotlin
// As soon as the glass allows and both answers are in (the rows, and whether a copy is fresh):
LaunchedEffect(gate) {
    if (!gate || !model.settings.zero) return@LaunchedEffect                  // the switch off: nothing here runs
    if (!arrival.unfold) snapshotFlow { presence.value >= 1f }.first { it }   // the opening set to Off: after its fade
    snapshotFlow { model.zeroKnown }.first { it }
    model.offerZero()
}
// The second moment, as built, with one call more:
LaunchedEffect(gate) { if (gate) { delay(TIP_AFTER_MS); if (!model.offerCopy() && !model.offerZero(last = true)) model.offerTip() } }
```

### The pure functions

```kotlin
package io.github.kuscher.booklight.core

/** The rows under the empty field: the two most used, then the most recent. Two or three, never one. */
object Zero {
    enum class Why { OFTEN, RECENT }
    data class Pick(val id: String, val why: Why, val weight: Double, val last: Long)
    /** [held]: what to store as the holders of seats one and two; null = store nothing this time. */
    data class Seats(val picks: List<Pick>, val held: List<String>?)

    const val MIN = 2
    const val MAX = 3
    const val EARNED = 1.5
    const val RECENT_MS = 8L * 60 * 60 * 1000
    const val AHEAD_MS = 5_000L
    const val HOLD = 1.2
    val KINDS = listOf("app:", "setting:", "command:", "cmd:", "link:", "recipe:")

    fun pick(items: Map<String, History.Entry>, now: Long, live: (String) -> Boolean,
             hidden: Set<String> = emptySet(), held: List<String> = emptyList()): Seats

    /** [row] with "Don't suggest" among its actions, in its place. */
    fun offer(row: Result, label: String): Result
}

/** What stands under the untouched empty field. */
object Under {
    enum class What { CARD, COPY, USUAL, TIP, NOTHING, WAIT }
    fun choose(untouched: Boolean, card: Boolean, copy: Boolean?, zero: Boolean, seats: Int?, tip: Boolean, last: Boolean): What
}
```

`live` is asked only for candidates. `copy` and `seats` are null while their answer is not in. `last` is the
320 ms mark, where nothing waits any longer and a null counts as no.

### Test cases

`ZeroTest.kt`:

| # | Given | Expect |
| --- | --- | --- |
| 1 | No history | None |
| 2 | Two earned, nothing recent, no third earned | Those two |
| 3 | One earned, and another thing run once an hour ago | None |
| 4 | Three earned, weights 5, 4, 3 | Those three in that order, all OFTEN |
| 5 | Two earned, a third run once 1 hour ago | Seat 3 is the third, RECENT |
| 6 | That third run 8 hours ago; 8 hours and 1 ms ago | Three rows; two rows |
| 7 | The most recent of all is already in seat 1 | Seat 3 is the newest of the rest |
| 8 | Run twice at once, then 11 days later; 12 days later | Earned (1.52); not earned (1.49) |
| 9 | `dial:volume`, `key:snap`, `scope:mail`, `web:url`, `calc`, `text:ask:tr` with the highest weights | Passed over |
| 10 | The top id is in `hidden` | Passed over; the next has its seat |
| 11 | `live` says no for the top id, which is held | Passed over; `held` in the answer is null |
| 12 | `held` = A, B; C weighs 1.19 × B; C weighs 1.2 × B | A, B; A, C |
| 12b | `held` = A, B with A 3.0, B 3.5; C 3.7 | C, B |
| 12c | `held` = A, B; two newcomers both weigh 1.2 times the held ones | The newcomers, heaviest first |
| 13 | `held` = A, B; B weighs 3.5 and A 3.0; B weighs 3.6 | A, B; B, A |
| 14 | A held id that is no longer earned, or is hidden | It gives up its seat; `held` in the answer is the new pair |
| 15 | Equal weights | The one run later first, then by id; the same on every call |
| 16 | A `last` 3 s ahead of `now`; 6 s ahead | Recent; not recent, its weight is its count, no exception |
| 17 | Any input | At most three, no id twice; `held` has at most two |
| 18 | `offer` on an app's row (places and Uninstall behind the arrow) | "Don't suggest" is the first action behind the arrow; an `armed` that pointed at Uninstall still does |
| 19 | `offer` on a settings row (one action) | Second and last |
| 20 | `offer` on a link's row (Open, Copy link, Edit, Delete) | Before Delete |
| 21 | `offer` twice | The same row as once |

`UnderTest.kt`, the truth table:

| # | Given | Expect |
| --- | --- | --- |
| 1 | Not untouched, whatever else | NOTHING |
| 2 | A card is due, either moment | CARD |
| 3 | A fresh copy, at the gate; at the last moment | WAIT; COPY |
| 4 | Switch on, no fresh copy, two or three seats, either moment | USUAL |
| 5 | Switch on, the copy's answer not in, or the seats not in, at the gate | WAIT |
| 6 | Switch on, one seat or none, tips on and one left: at the gate; at the last moment | WAIT; TIP |
| 7 | Switch on, one seat or none, no tip, at the last moment | NOTHING |
| 8 | Switch on, three seats, the copy's answer still not in, at the last moment | USUAL |
| 9 | Switch off: at the gate; at the last moment with a tip; without | WAIT; TIP; NOTHING |

`offerCopy()` and `offerTip()` keep M1's code; the table pins the order they and `offerZero()` make together.

### Debug hooks

| Command | Does |
| --- | --- |
| `./bl debug pref zero on\|off` | Sets the switch as the window does. `pref` prints `tips=` and `zero=` |
| `./bl debug zero` | Calls `app.usual()` and prints the rows: id, why, weight, age; the held and hidden ids; how long it took, in microseconds. Needs no panel |
| `./bl debug seed` | After `forget`: five apps from the app list with 8, 5, 3, 2 and 1 runs, 30 days to 5 minutes old |
| `./bl debug unhide` | Empties `zeroHidden` |
| `./bl debug forget` | Also empties `zeroHeld` and `zeroHidden` |
| `./bl debug dump` | Gains `zero=3` (or 2, 0), `copy=fresh\|none\|unknown`; `selected=-1` at rest |
| `./bl debug key down\|up\|tab` | Call the model's `down()` and `up()`, and the same case for Tab as the panel |
| A log line per opening | "usual: rows ready, copy known, offered" with their ms after `onCreate`: for check 5 |

### Races to close

| # | Race | Answer |
| --- | --- | --- |
| 1 | A letter while the rows stand | They stay until the search lands. `enter`, `runRow`, `fill` and `open` already refuse while `resultsFor` is not the field's text |
| 2 | The job, or the copy's answer, lands after the gate | The rows are offered the moment both are known, at the latest at 320 ms; after that not in this opening |
| 3 | `offerZero()` is called twice | It answers true if the rows already stand |
| 4 | A fresh copy at the gate that is stale 320 ms later | `offerCopy()` says no, `offerZero(last)` is asked and the rows come |
| 5 | Up, or text handed over, or a guided example, before the gate | The field or the list is not empty, or `guided` is set: the rows do not come in this opening |
| 6 | The window has no focus yet at the gate | `copyFresh` is null: the rows wait. A fresh copy still gets its line |
| 7 | A typed space, then Enter, then a letter | `resultsFor` is the blank text and `whenReady` is null: nothing runs unasked |
| 8 | `refresh()` after Shift + Enter | The blank branch returns the same rows, keeps the selection by id and keeps an opened row open |
| 9 | Up with a row highlighted; a held Up | Moves up first. The step to rest and the step to the last text each need a press of their own |
| 10 | Up at rest with no last text | Nothing (`move(-1)` would have brought the highlight to row one) |
| 11 | The app is removed between the job and Enter | `Executor.run` returns false; the footer says so |
| 12 | The app list or the commands are read again during the job | The job waits for both `ready`, then reads each index once |
| 13 | "Don't suggest" while a row's list is open | One list change (Z5). The rows that come back do not wait for the list's edge |
| 14 | The key again while leaving (`turn`) | The same model: the rows are still there; nothing is offered twice |
| 15 | The pointer lies where the panel opens | At rest it must move 4 dp on a row before it selects |
| 16 | The job lands after typing began | The rows are kept but were never offered: the blank branch returns them only if they stood in this opening |
| 17 | Held keys | The `repeatCount` guards stand |

### What changes with the option off

The first pass said "with the option on Tips, the code path is today's, line for line". Four fixes reach it.
Each is small, and each is one of the critics' findings (decision 11 and decision 14).

| # | Change | Who sees it |
| --- | --- | --- |
| 1 | `Panel.kt`: what is under the field, and the footer, wait for the glass's edge (`ends()`) during the opening | Someone who types within the opening, text handed over by another app, and a first-run card: they are no longer cut by the edge for 25 to 102 ms |
| 2 | `Rows.kt`: a row that leaves in mid-rise keeps its offset | Fast typing: no 6 to 12 dp jump of a fading row |
| 3 | `OverlayActivity.run`: App info, Edit, Delete and dangerous actions no longer count as a run | Ranking: such an action no longer lifts its row |
| 4 | `SearchEngine`: an empty query returns nothing (already on the branch) | Only `./bl debug find` with no text; the panel never asks |

### Order of work, and size

| Step | What | Days left |
| --- | --- | --- |
| 1 | `Zero` (the changes), `Zero.offer`, `Under`, their tests, `HistoryTest` (`./bl test`). The first pass of `Zero` is on the branch | 0.5 |
| 2 | The two `ready`; `app.usual()` with the icons. `byIds` is on the branch | 0.5 |
| 3 | `pref zero`, `zero`, `seed`, `unhide`, `forget`. The settings are on the branch | 0.25 |
| 4 | The model (the table above); the panel's two moments, the keys and `ends()`; the activity (focus, icons, `learn`, `onSay`) | 2 |
| 5 | The placeholder's two captures for decision 9 (light and dark, 4x, with and without the 2 dp) | 0.25 |
| 6 | `Rows.kt` (four changes), the footer hint, the field's cap, the announcement | 1 |
| 7 | "Don't suggest": the effect, the symbol, `unsuggest`, the footer word | 0.5 |
| 8 | The window: the switch, "Suggest everything again", Forget everything; strings in both languages | 0.5 |
| 9 | Device pass on the Lenovo, then the HP with Alex (section 9) | 1 |
| 10 | Documents | 0.5 |
| | **In all** | **7** |

After step 4 the app works with the switch off as today, but for the four fixes. Riskiest: `selected = -1`
(every reader of `selected` was written for a list whose row one is selected), the three moments at which the
rows can be offered, and the second line's return in `Rows.kt`.

**If the night is short, in this order:** (1) links, recipes and any row with a second line are not suggested
in this version (`live` says no for them), which drops change 3 of `Rows.kt`; (2) the early offer goes, and the
rows come only at the 320 ms mark.

## 9. Device checks before it is called done

| # | Check | What to look at | If it goes badly |
| --- | --- | --- | --- |
| 1 | Alignment, on a 4x capture, light and dark, with an app, a link and a settings page as the three | G and the icons on one centre line; the three titles on one left edge and on the rows' centre lines; the placeholder against the titles; kind labels and "Close" on one right edge | Fix before anything else. Placeholder: decision 9 |
| 2 | The opening, stepped frame by frame at Fast, Medium, Slow | The glass is 68 dp high until the gate. No frame shows a 288 dp slab. No icon, kind label or footer word is cut by the glass's edge, at Slow too. The field's `esc` cap is never seen, and never gets brighter once rows are there. Each icon is there in its row's first visible frame | A late icon: the rows wait for the icons |
| 3 | Frame times around the gate, release build | As on 2.2: one frame every 8.3 ms, about 1 % late | Cut list of section 5, item 3: the rows wait for the glass to rest |
| 4 | Cold start (the process was not running) | The rows come at the gate, or later up to 320 ms after it, or not in this opening. Never later | Accept "not in this opening" |
| 5 | A fresh copy with the switch on, ten openings each at Fast, Medium, Slow and Off; then ten with no copy | With a copy: the line comes, the rows never do. Without: read the log line for when the copy's answer lands against the gate | If the answer is always late at Fast: accept, or cut list item 2 for one moment instead of three |
| 6 | Keys | Enter at rest does nothing. Down and Enter before the rows do nothing and are not kept. A held Down stops on the last row. A held Up from row three stops on row one; Up again goes to rest; Up again brings the last text. Up at rest with no last text does nothing. Space, Down, Enter runs the row. Space, Enter, `g` runs nothing | |
| 7 | Typing over the rows with `./bl debug keys c`, once with a link among them | A row with the same id keeps its row. Its strip does not change. A link's second line fades in and its title glides, nothing jumps. The height never dips to 68. Backspace: the rows, no highlight | No release with a dip or a jump |
| 8 | Dark over a white window, light over a white window | The kind labels and footer hints can be read | Kind labels at full ink in dark, in every list |
| 9 | German | „Einstellungen“ beside a long title; „Nicht vorschlagen“ unrolled; the footer; the hint's change from „Auswählen“ to „Aktionen“, stepped: does it read as one change or as two caps | Shorten the string, never the layout. Two caps: the outgoing pair cuts |
| 10 | Don't suggest, on a settings row and on an app with its list open | The row and its list fade where they stand, the highlight fades out and does not travel, the others take their seats at once, the footer word shows, `↓` Choose is back. Next opening: the next candidate. "Suggest everything again" brings it back | |
| 11 | A removed app | Removed while closed: not among the rows at the next opening. Removed while open: "Couldn't do that" | |
| 12 | Shift + Enter on a row, and on a place in an app's opened list | It runs, the panel stays, the rows stand, the highlight stays, the list stays open | |
| 13 | The pointer | Rows arriving under a resting pointer select nothing, and a nudge under 4 dp selects nothing. Moving selects. A click runs | |
| 14 | An app's opened list on the HP | "Don't suggest" is the first line and the highlight lands on it; Uninstall, the last line, is on screen (the panel ends at 877 of 1067 dp) | |
| 15 | The reflection | It comes 2.4 s after the opening, never during the cascade; one lap is about 2.0 s | |
| 16a | The system's animations off | One frame, no stagger, no highlight | |
| 16b | The opening set to Off | The panel fades in at the field's height; the rows come after the fade | |
| 17 | The screen reader | One announcement on arrival; "Don't suggest" among the row's actions | |
| 18 | With the switch off | A tip, the copy's line and a first-run card come as in 2.2; `./bl debug dump` shows `zero=0` | No release if anything differs but the four fixes |
| 19 | The HP as a whole | Everything above: the HP has not been looked at | |

**It worked if,** after a week on the HP, the switch is still on, "Don't suggest" was needed twice at most, and
the opening is as fast as 2.2.

## 10. Decisions for Alex

The build goes ahead on the recommended answers. Each is the safer of the two, except 11 and 14, where the
recommended answer touches the path with the switch off and the row says so.

| # | Decision | Recommended | If you say the other thing |
| --- | --- | --- | --- |
| 1 | **Enter on the empty panel.** The rule so far is "row one selected" | Nothing is selected and Enter does nothing. Down, then Enter | Row one is highlighted on arrival and Enter runs it. Then an Enter pressed out of habit on an empty panel opens an app, and Enter has to wait until the rows are on screen |
| 2 | **The name** | "Your usual" / „Das Übliche“ | "Suggestions" / „Vorschläge“: the word already means the search engine's in the app. "Most used" / „Meistgenutzt“: untrue for seat three |
| 3 | **The option's shape.** You asked for "show it or the tips" | One new switch, "Show your usual", off. Your "Show tips" switch stays as it is | One choice with three values in place of both switches: Tips · Your usual · Nothing. Tidier, but it takes your switch away and stores three values in two booleans |
| 4 | **A fresh copy.** Your words in M1: "a suggestion for zero state" | The copy's line alone, exactly as M1 built it. The rows do not show for those two minutes | The line takes seat one and two usual rows stand under it, always 288 dp. Down then goes to the rows and only Tab opens the copy. About 1 day more, and M1's checks of the line are run again |
| 5 | **When the rows come** | As the glass lands (at Medium all three are there 443 ms after the key) | 320 ms later, like a tip (763 ms). Calmer for someone who types at once; it reads as a wait for someone who does not |
| 6 | **Seat three** | The last thing run in the past 8 hours, once is enough | Only things run twice: the three most used. More stable, and nothing recent |
| 7 | **Time of day** ("mornings") | Not in this version. It needs a new stored field and a sentence in the privacy text | Record the part of the day of each run from now on; seat three can use it in a later version. About 1 day |
| 8 | **A row run from the usual rows counts as a run** | Yes: it is one | It does not count. The rows then learn only from what you type, and the list cannot feed itself |
| 9 | **The placeholder at x = 74** over titles at x = 72 | Decide on the pair of captures (build step 5). Move it to 72 only if the caret clears the first letter's ink by 1 px at 4x. My reading of the code says it will not: the caret is 2 dp wide, from 72 to 74 | Move it to 72 anyway: one line in `Field.kt`; the caret then touches the "S" |
| 10 | **The default** | Stays off until you say otherwise, after your week with it | On for everyone at the next release |
| 11 | **The glass's edge at the gate** | What is under the field waits for the edge (`ends()`). No new number, nothing is ever cut. It also reaches a first-run card and rows typed during the opening, with the switch off | The rows are offered at 95 % open (157 / 313 / 626 ms; all three at 343 / 499 / 812). Nothing built is touched; one new number; a highlight brought within 100 ms can still be cut at its ends |
| 12 | **"Forget everything" and the hidden list** | It empties the hidden list too: that list names the things you least want kept. A hidden thing can then come back, but only after it is run twice again | It keeps the hidden list, and "Suggest everything again" is the only way to empty it |
| 13 | **Fewer than three.** The issue says "three" | Two rows show (232 dp) when only two are earned and nothing is recent. Never one. The list is then there on most openings | Three or none. Then the list comes and goes from one opening to the next (a thing run once on Friday afternoon is gone on Monday, and with it the whole list) |
| 14 | **The fixes that reach the switch-off path** (section 8) | Taken for everyone: App info, Edit, Delete and Uninstall stop counting as a run; a row leaving in mid-rise no longer jumps | Only with the switch on. Then a cancelled Uninstall can still hold seat three |

**Settled by the design lead where the group or the critics disagreed.** Say so if you disagree.

| Question | Decided | Why, in one line | Who said otherwise |
| --- | --- | --- | --- |
| Footer | From arrival, 288 dp, with `↓` Choose | Down must not move an edge, a failed run needs a place to say so, and `Metrics.height` stays as built | Visual: none at rest. Motion: none at all |
| When | At the gate, no hold | The switch asks for them, so they belong to the opening's one gesture | Engineering: 320 ms |
| A row's right end | Its kind, as in typed lists | A row that stays when a letter is typed must not change a word | Product, interaction: "Often", "Today" |
| A second line | None in the usual rows; it returns in a typed list on a designed movement | Three titles on one rhythm, and no address on screen at every opening | Design critic: show it when highlighted. Engineering critic: the host only |
| A fresh copy | The line alone | "Never two under the field" stands (04-design 3.5), and M1's line is not touched | Product: line and two rows. Interaction, visual, motion: line and three rows |
| Earned | Weight 1.5 | "Run twice" is a rule a person can say; 2.0 means three runs in practice | Engineering: 2.0 |
| Recent | 8 hours | A working day, and the night clears it: something opened at 22:40 is gone by 06:40 | First pass: 12 hours. Engineering: 18 |
| A newcomer's seat | It takes the seat of the one it pushed out | The one that stays keeps its place, so one seat changes and not two | Engineering critic: the held one always stands first |
| The field emptied again | The same rows come back | The height must not dip to 68 while editing | Interaction: they stay away |
| Don't suggest on an app | Behind the arrow, first | Two Enters from the arrow and never past Uninstall. The cost: someone who opens the list for a place presses Down once more | First pass: last before Uninstall (11 keys). Product, interaction, engineering: an icon |
| After Don't suggest | Back to rest, nothing selected | Nothing slides in under a standing highlight, and a second Enter runs nothing | Engineering critic: the highlight moves to the next row |
| The pointer at rest | Movement of more than 4 dp brings the highlight | A parked pointer must not arm a row; in typed lists nothing changes | Visual: movement does nothing. Engineering critic: 4 dp in every list |
| Down before the rows | Not kept | A kept key is answered at a time nobody can predict (the gate, the answer, or 320 ms) | First pass: kept |
| Tab at rest | Down, always | It acts on what is on screen, never on a copy nobody saw | First pass: opens a fresh copy if there is one |
| Counting how often the rows are used | Not in this version | New stored numbers need Alex's word | Product: two local counters |
| `cmd:` and `command:` | Both can be suggested | They are two things in the code: other apps' commands, and Booklight's own pages | Engineering named only `cmd:` |

## 11. What the research said

1. The limit: no web search ran (0 of 7). Everything below is from first-party help pages and Chromium's
   source. Nothing was found on what users like or dislike, and no product was confirmed to run a suggestion on
   Enter from an untouched empty box.
2. Chrome's omnibox asks for zero suggestions "when the user focuses in the omnibox prior to editing" and shows
   a cached list at once; the caps are 8 on desktop, 15 on Android, 20 on iOS
   ([zero_suggest_provider.h](https://raw.githubusercontent.com/chromium/chromium/main/components/omnibox/browser/zero_suggest_provider.h),
   [autocomplete_result.cc](https://raw.githubusercontent.com/chromium/chromium/main/components/omnibox/browser/autocomplete_result.cc)).
   Booklight: the rows are fixed before they are offered.
3. Chrome ranks recent searches by frecency over 90 days, drops them in incognito, and deleting one deletes it
   from history ([local_history_zero_suggest_provider.cc](https://raw.githubusercontent.com/chromium/chromium/main/components/omnibox/browser/local_history_zero_suggest_provider.cc)).
4. Chrome's clipboard suggestion shows only with empty input, for a copy under 3 minutes old on Android and 10
   elsewhere ([clipboard_recent_content.cc](https://raw.githubusercontent.com/chromium/chromium/main/components/open_from_clipboard/clipboard_recent_content.cc),
   [clipboard_provider.cc](https://raw.githubusercontent.com/chromium/chromium/main/components/omnibox/browser/clipboard_provider.cc)).
   Booklight: the copy keeps its own short-lived place.
5. Pixel Launcher suggests "based on your recent and most-used apps, as well as your routines"; one app can be
   dragged to "Don't suggest app" ([Pixel help](https://support.google.com/pixelphone/answer/2781850)).
   Booklight: the action's name.
6. The ChromeOS launcher shows 4 to 5 recent apps and hides the row below 4; its Continue section needs 3 files
   ([recent_apps_view.cc](https://raw.githubusercontent.com/chromium/chromium/main/ash/app_list/views/recent_apps_view.cc),
   [continue_section_view.cc](https://raw.githubusercontent.com/chromium/chromium/main/ash/app_list/views/continue_section_view.cc)).
   Booklight: a floor of its own, two rows, and never one.
7. Raycast keeps favourites "at the top of Root Search when the search bar is empty"; frecency is the lowest of
   its signals, and "Reset Ranking" clears one command ([Raycast manual](https://manual.raycast.com/search-bar)).
8. Alfred ranks on "a heuristic fingerprint of your usage, maintained on a 4 week rolling window"; its help
   pages describe no list in the empty box ([result ordering](https://www.alfredapp.com/help/kb/understanding-result-ordering/),
   [Advanced](https://www.alfredapp.com/help/advanced/)). Booklight's four-week half-life is already built.
9. Windows Search shows "Recent files, settings, and apps, Quick searches, and Top apps" when its box is
   selected. Spotlight in macOS Tahoe 26 added browse views and "learns from users' routines"; what its empty
   field shows was not confirmed, nor how one item is removed in either
   ([Apple newsroom](https://www.apple.com/newsroom/2025/06/macos-tahoe-26-makes-the-mac-more-capable-productive-and-intelligent-than-ever/),
   [Microsoft support](https://support.microsoft.com/en-us/windows/search-for-anything-anywhere-b14cc5bf-c92a-1e73-ea18-2845891e6cc8)).
10. Android's own guidance: "always provide a way for the user to clear the recent query suggestions"
    ([developer.android.com](https://developer.android.com/develop/ui/views/search/adding-recent-query-suggestions)).
    The old Quick Search Box let chosen suggestions "move up"
    ([Android Developers blog, 2009](https://android-developers.googleblog.com/2009/09/introducing-quick-search-box-for.html)).
    Booklight: "Don't suggest", "Suggest everything again", "Forget everything", and the switch.

## 12. What the critics found, and what was done

Each finding was checked against the code on `m1-the-copy` before anything was changed. "Right" means the code
says what the critic says. D is the design critic (visual and motion), U the critic for interaction and
engineering.

| # | Finding | Done |
| --- | --- | --- |
| D1 | `offerZero()` could put the rows over a list that has a selection: Up before the gate and text handed over fill the field without `typedYet` | Right (`restoreLast()` and `enterScope` never set it). `offerZero()` now has the full guard of `offerCopy()` (section 3, the model table). Race 5 added |
| D2 | Don't suggest: the highlight flies up from an opened list, a row slides in under a standing highlight, and the rows that return wait about 200 ms for the list's edge | Right (`Rows.kt`: the returning rows wait for `block` to fall to 0.04 unless the list was "typed away"). One rule taken as written: everything fades where it stands, back to rest, `selected = -1` (Z5). `Rows.kt` change 2. Heights for one row and none added |
| D3 | At the gate the glass spans x = 53.5 to 666.5 and its edge cuts icons, kind labels and the footer for 25 / 51 / 102 ms | Right; my arithmetic gives the same span and times. `ends()` on the body and the footer (Z1, times table, check 2). Two corrections to the finding: (a) "no time in the table changes" does not hold at Slow, where all three reach 90 % at 727 ms, not 700, and the cascade is no longer seen; (b) the footer's numbers at Medium are overstated, because its band is not uncovered until the window has grown past about 155 dp, some 55 ms after the gate; at Slow it would be cut. The fix reaches the switch-off path, so it is decision 11, with the critic's other fix (95 %) as the alternative |
| D4 | A kept Down can be answered 270 ms late, and could open a copy nobody saw | Right. The kept Down is cut altogether, as U7 asks: no key is kept (key table, Z8) |
| D5 | Z9 is not true for the opening set to Off: the height would spring to 288 while the panel fades in | Right (`gate` is true from the first frame and `Motion.on` is true). Split into Z9a and Z9b; the rows are offered after the 170 ms fade. Check 16 split. The spec called the setting "Fade"; its name in the app is Off |
| D6 | At rest the placeholder is the only text on the field's line, and it stands at 74 | Right; the padding is on line 109 of `Field.kt`, not 103. The placeholder is named in "What must line up". The two captures are build step 5, before the device pass. I could not make them (no device in this seat). My reading is that 72 fails the critic's own test: the caret is 2 dp wide at 72 to 74. Decision 9 says so |
| D7 | A row that leaves in mid-rise jumps up 6 to 12 dp in one frame | Right (`translationY = if (leaving) 0f else …`). `Rows.kt` change 1. It is in every list today, so it is one of the four switch-off fixes |
| D8 | A link's or a recipe's title sits about 9 dp higher than a one-line title | Right. Solved at the root and not left as a risk: every usual row is one line (section 2), and the second line returns in a typed list on a designed movement (Z3, `Rows.kt` change 3). The critic's fallback was not taken: a title held on the centre line with a second line under it puts that line against the row's lower edge. Check 1 captures an app, a link and a settings page together |
| D9 | A kept row could still change: "Don't suggest" was in the strip only in the usual rows, so the strip cross-fades at the first letter and "Open ⏎" is drawn in two places | Right (`Trail`'s key is the action ids). A seat holder carries the action in every list of the opening (`Zero.offer`, the model table). The kind labels' fallback to full ink is for every list |
| D10 | The footer's hint: the cap changes with its label, not only the label | Right (`Footer.kt`: one `AnimatedContent` holds both). Written as it is (Parts, Z2). German added to check 9, with the cut as the fallback. `design-system.md` §3h is corrected in the Docs step |
| D11 | A held Up runs through rest into the last text | Right (Up has no `repeatCount` guard). The two steps each need a press of their own (key table, `up(again)`, race 9, check 6) |
| D12 | Two design rules in `CLAUDE.md` change, the spec named one | Right. Both are rewritten, in `CLAUDE.md` and `ux-model.md` (section 8) |
| D13a | Z8: the pill "`fade` 120 with row one" is in fact at full strength under the body's fade | Right, and the case is gone with the kept Down |
| D13b | Z1: the body's `fade` 140 also lies over every row | Right. Added |
| D13c | Z3, Z4: the footer band does not stay still, it rides the lower edge | Right. "Stays shown, rides the edge (288 up to 568)" |
| D13d | The strip is not on x = 700: its last slot's box ends there, the arrow's ink ends at 693 | Right (`REST` 7 + icon 18 + 7). Said so in Parts and in "What must line up" |
| D13e | The field's `esc` cap: two cases | Right. Both named, and a third found: rows that come while the cap is part shown. One rule covers all three: it never gets brighter once rows are there (`Field.kt`) |
| D13f | Z6: the fold is 210 at Slow; gone after 155 / 155 / 310 | Right. Corrected |
| D13g | Height table: an app that cannot be uninstalled is 616 | Right. Row added |
| D13h | Z2 had only the way in | Right. The way back added |
| D13i | `design-system.md` §2 says label 13 and hint 12; the code has 14 and 13 | Right (`SMALL`, `LABEL`, `HINT`). In the Docs step |
| U1 | `offerZero()` could come over handed text, a restored text and a guided example; nothing said the job runs only with the option on; the choice should be a pure function with a test | Right on all three (`guided` is set after the constructor, so "not when guided" at creation is always false). Full guard; the job starts only `if (!demo && settings.zero)`; new `Under.choose` with its truth table |
| U2 | `resultsFor = null to ""` parks an Enter after a typed space, which the next letter then runs | Right (`enter()` compares with `chip?.key to query`). The blank branch keeps today's two statements; `offerZero()` sets `resultsFor = null to query`. Race 7 and check 6 added |
| U3 | "Don't suggest" would say "Couldn't do that", or count as a run; the model cannot say a footer word | Right (`Executor` answers false for effects the panel does itself; `say` is private to the activity). `enter()` catches the effect; new `onSay`. One part not taken: the highlight does not move to the row above when the last row is hidden. It goes back to rest, by D2's rule, which also answers "no destination is given" |
| U4 | The job would run on the main thread inside `onCreate`, and `app.icons` can be null when the model is made; the icon cache is keyed without the size | Right (`lifecycleScope` is the main thread's; `AppIcons` is made on line 97, the model on line 86). `Dispatchers.Default`; `AppIcons` first; icons at 48 dp in px; the work moved into `app.usual()` |
| U5 | At the gate the window may not have the focus, the look fails, the rows come and lock the copy's line out; Tab at rest would read a copy nobody saw | Right (`Clipboard.look` answers only the window in front, and `offerCopy()` refuses once there are rows). The look is made on focus, off the main thread; the rows wait for its answer. One step further than the critic asked: they are offered the moment the answer lands, not only at the 320 ms mark, or Fast and Off would nearly always be 320 ms late. Tab at rest is Down; the one corner it leaves is named under the key table |
| U6 | `selected = -1`: Up at rest would select row one; a held Up; Up on the row of an open list; the blank branch shuts an open list on `refresh()` | Right on all four (`move()` coerces to 0; `shut()` is called before the blank test). `zeroUp`, `up(again)` as given, the blank branch keeps an open row on `refresh()` (the model table, races 8 to 10, check 12) |
| U7 | The kept Down has holes | Right. Cut. One remark kept in view: Down counts from the frame the rows are in the list, so a blind Down, Enter still depends on when it is pressed. That is the user's own act; section 3 says how the highlight comes |
| U8 | Any action counts as "run last" | Right (`run()` calls `learn(r)` for all). The rule of section 2. It changes ranking for everyone, so it is in "What changes with the option off" and decision 14 |
| U9 | "Three, or none" makes the list come and go, and one "Don't suggest" can erase it | Right; the example checks out (0.5 ^ (2.71 ÷ 28) = 0.93). `Zero.MIN = 2`: two or three, never one. Tests 2 and 3. Because the issue says "three", it is also decision 13 |
| U10 | 12 hours spans the night; a clock set back makes a thing recent for ever; a link shows its whole address; should "Forget everything" empty the hidden list | 8 hours taken. "Recent" needs `now - last` between minus 5 s and 8 h. Not taken as written: a run "from the future" is not recent, but it stays earned at its count, as `History` already weighs it; dropping it would empty seat 1 after a clock change. The address: no second line at all, which is stronger than the host. The hidden list: decision 12, with my answer (empty it) and the critic's (keep it) |
| U11 | The hold is not defined well enough to build, and a list that is still loading rewrites it | Right. The procedure is written (section 2) and pinned by tests 12b and 12c. Its order differs from the critic's in one case (C, B and not B, C): the newcomer takes the seat of the one it pushed out, so the one that stays keeps its place. That is what `Zero.kt` on the branch already does. `AppCommands` gets its own `ready`; nothing is stored in an opening where a held id was passed over for `live` alone |
| U12 | Three values in two booleans; a sentence that is not always true; Alex asked for two values | Right, and the last point decides it: his words are "show it or the tips". The choice of three is withdrawn. One new switch beside his; four stored states, four honest pictures; the text no longer speaks of tips "until it knows three" (section 6). The choice of three is decision 3. So `set_tips_show` and `set_tips_text` stay in use |
| U13 | Pointer jitter at rest arms a row | Right (`RowFrame` selects on any Move). 4 dp of travel, at rest only: typed lists stay as built (`Rows.kt` change 4, race 15, check 13) |
| U14 | "Don't suggest" on an app is 11 keys and passes Uninstall | Right (`step(-1)` from the first line wraps to the last). First behind the arrow; with D9 that holds in every list of the opening. The cost is named in "Settled" |
| U15 | The build plan missed files, and one sentence was wrong | Right. Added: `set_privacy_text`, `hint_choose`, the announcement, `app.usual()` for `./bl debug zero`, `debug forget`, the receiver's and `bl`'s headers, `HistoryTest`, the default of `byIds`, the `Stage.kt` check, `04-design.md` §3.5, `plan.md`, `PICKING-UP.md`, `window-redesign.md`. The sentence about the first-run card is corrected: it comes back when the field is emptied (`card` is worked out from the field) |
