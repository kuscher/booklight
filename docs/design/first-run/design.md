# First run: the design

*Designer seat, 4 October 2026; second and third round the same evening. Drawn at the desk from the brief, `pm.md`,
`eng-constraints.md`, the design system and the panel's code; this seat built nothing and touched no device.
Coordinates: x from the panel's left edge (0 to 720), y from its top edge. "Ink" is `onSurface` at an alpha: strong
1.0, second 0.80, third 0.60. L / D = light / dark. Type styles go by their names in `design-system.md` §2; where
the code's size differs from that table, the code's is given. Widths of words are estimated (about 7 dp a character
at 14 sp, 6.5 at 13 sp), not measured. Every figure is a sketch; the numbers on its edges are exact.*

## Third round: what changed

Alex, to the opening: "Yes but have sort of a 'boot screen' at the beginning where it says 'Welcome to Booklight'
[…] but maybe cuter and funnier. Also add flights into the opening celebration." And a few minutes later: "make
the welcome visually stunning. It should be in an expanded version of our panel (same width) but you can play
inside of it with animation, graphics, motions, and light. Go all out for the boot welcome."

1. **New: the welcome** (§3). Four seconds in which the glass is a stage, 720 × 468: night, Booklight's own lamp,
   the words in its light, a small Swiss Army knife. Three concepts, one chosen and specified; three versions of
   the words.
2. **New: a flight in the show** (§3, B3): the real flight row, as an example, said so on the glass. Nothing is
   fetched.
3. **The order and the length:** welcome, apps, a sum, a flight, the grid, the landing. K1 stands at about
   10.9 s (the limit is thirteen).
4. What follows: the first opening runs at the speed that is set, not at Slow (the welcome is the spectacle
   now); Booklight's mark takes its seat at the welcome's hand-over; the one highlight is born as the knife's
   handle; with animations off the title is the field's placeholder. Heights, moments, states, what lines up
   and the decisions are restated.

## Second round: what changed

Alex, to the first plan: "Love it. Some adjustments. The suggested key should be Action + Quick insert […]. Can you
add a more visual beginning with more motion and more animation and polish. It should wow at the very beginning
showcasing truly this app. Re 5, the other thing. Re 11 no row."

1. **New: the overture** (§3). Booklight uses itself, in the real panel, and lands in the key step without a
   cut. (Eight seconds then; with the third round's welcome and flight, about eleven.)
2. **The key is Action + Quick Insert** (§5): K1's caps, a caption that says where the key is, K3's other keys,
   the helper's rows, every string. Action + J where the keyboard has no such key.
3. **The panel stays through the lessons** (§6). One opening from the overture to the fold; the user's keys are
   pressed once. After an app is opened Booklight steps aside for a blink and is back by itself.
4. **No row for the device's model** (§7). The last screen has two rows and is 268 dp.
5. What follows from these: the words that stood beside the caps stand under them (the wide cap needs the room);
   "your key" has a width of its own, 112 dp; each lesson has one line that says what Enter does on other days.

Unchanged: the stage, the skeleton, the large cap, K2 to K4, the reveal, the question, the fold.

**Verified on the Lenovo Googlebook, 4 October 2026** (by the coordinator, with a stand-in window of the panel's
size and place):

1. A panel-like window that stays open gets its own page in the helper. The page's heading is the group's label,
   free text of ours. The left column names the page by the app's name and icon.
2. Rows wrap, uncut. A row's keys stand at its right end. The five rows of §5 were drawn as designed, in English
   and German.
3. A row whose label holds the app's name stops the search from jumping to the app.
4. The panel is fully behind the helper. Nothing of it shows.
5. Closing the helper by its X or by Esc gives the focus straight back to the window that stayed open.
6. **Right after "Set shortcut", with the helper still open, pressing the new keys makes the system close the
   helper by itself and bring the app's window to the front; a launcher start arrives.**
7. A combination that is not yet a shortcut does not reach the app. The panel cannot let people try keys, and
   cannot show which keys were pressed.
8. The capture dialog's words are the system's, in the system's language: "To create this shortcut, press the
   Action key and one or more other keys together", a fixed "Action" cap, a field "Press key", "Cancel", "Set
   shortcut".
9. **Action + Quick Insert works** (`shortcut-setup.md` §3). The helper draws the key as a cap that reads "Quick
   Insert" after the Action glyph; a 59-character English row beside it stood on three lines. The dialog takes
   the key; pressing it starts the app and closes the helper. It sits where Caps Lock sits on other keyboards;
   most external keyboards do not have it.
10. **A see-through window that starts an app does not stay in front.** The app takes the front and the focus,
    and the system removes the window that started it (0.2 to 1.5 s later). Started again at once, it was in
    front of the app with the focus 0.1 s after the app's start; started again after 0.4 s, 0.43 s after. After
    1.5 s nothing came back. What the eye sees in between was not filmed; expect the system's own fade, about
    200 ms each way, and a newly made panel.

## 1. The idea

- **It begins with a welcome and a performance.** Four seconds of Booklight's lamp, then seven in which
  Booklight uses itself. Then it asks for one thing: a key.
- **The field is never covered.** First run stands under it, where rows stand. Type at any moment, the overture
  included, and it is Booklight, at once.
- **Keys are the picture.** Each step shows what to press as large key caps, and what to type in the field's own
  type. No illustration, no slide, no paragraph.
- **One skeleton for every step:** a row-like seat that says what and why, a band that shows the keys, the answers
  at the right end. All on the panel's lines: 38, 72, 700.
- **One opening.** Welcome, show, key, the system's dialog, the reveal, three lessons, the question, the choices, the
  fold. Twice an app opens and Booklight comes back in front of it by itself.
- **It ends as the bare field.** The last screen is a real list; Enter folds it to 68 dp.

## 2. The stage and the heights

Settled in the first round: (b). The three ways, for the record, as drawn then (with the keys suggested then).
Costs are from `eng-constraints.md` §3.

```
(a) the card's own size: 720 × 172
  0 ╭──────────────────────────────────────────────────────────────────╮
    │  G     Search apps, settings and the web                  [esc]  │ field 68
 68 │ (key)  Give Booklight a key   [Action]+[J]     [ Open …    ⏎ ]   │ card 96: caps 24 dp,
    │        three lines of 13 sp at most             Not now          │ a title, three lines, two answers
164 │                                                                  │ pad 8
172 ╰──────────────────────────────────────────────────────────────────╯

(b) the same width, taller: 720 × 252 (the key, the three lessons) and 720 × 324 (the question)
  0 ╭──────────────────────────────────────────────────────────────────╮
    │  G     Search apps, settings and the web                  [esc]  │ field 68
 68 │                                                                  │ pad 8
 76 │ (key)  title and one line                               1 of 5   │ seat 1: 56
132 │                                                                  │ band: 112
160 │        ╭────────╮   ╭────╮                     [ answer   ⏎ ]    │ caps 56 dp,
    │        │ Action │ + │ J  │                       answer          │ y 160 to 216
216 │        ╰────────╯   ╰────╯                                       │
244 │                                                                  │ pad 8
252 ╰──────────────────────────────────────────────────────────────────╯

(c) a larger glass that shrinks into the panel: a window 960 × 404 for these openings, the glass framed in it
     x −120          0                                              720          840
  0 ╭──────────────────────────────────────────────────────────────────────────────────╮
    │                G     Search apps, settings and the web                           │ field 68
 68 │                caps 96 dp, the helper's five steps drawn beside them             │
404 ╰──────────────────────────────────────────────────────────────────────────────────╯
     at the end the side edges come in 120 dp each and the lower edge rises: 720 × 68
```

| Stage | It lets the design | Its cost |
| --- | --- | --- |
| (a) 172 | Reuse `CardBody` as it is | Free. Caps no larger than 24 dp. The question's German text does not fit three lines: a disclosure would be cut |
| **(b) 252 and 324, no dim** | Caps 56 dp, as tall as a row. Every part on the row grid. The whole disclosure. The daily opening unchanged | **Free** to move. The layout is the work. One fixed height per screen |
| (c) 960 wide | Caps 96 dp, an ending in which the glass visibly becomes the panel | **4 to 5 days and a broken rule**: "the window is exactly the panel". And the helper hides it for the hard part |

The overture uses what the first plan left on the table: more height (the welcome takes 468 dp, the most that is
free) and a dim behind for those seconds only.

| Screen | Sum | dp |
| --- | --- | --- |
| The opening, always | field | 68 |
| Overture: the welcome | the stage | **468** |
| Overture: apps | 68 + 8 + 5 × 56 + 8 + 36 | 400 (344 with four apps, 288 with three) |
| Overture: the sum | 68 + 8 + 92 + 8 + 36 | 212 |
| Overture: the flight | 68 + 8 + 136 + 8 + 36 | 256 |
| Overture: the grid | 68 + 8 + 256 + 8 + 36 | 376 |
| The key (K1 to K4) and the lessons (L2, L3, L4) | 68 + 8 + 56 + 112 + 8 | **252** |
| A lesson's typed list | the list's own | |
| The question (Q) | 68 + 8 + 56 + 184 + 8 | **324** |
| Your choices (C) | 68 + 8 + 92 + 56 + 8 + 36 | **268** |
| The ending | field | 68 |

All fit a screen from 655 dp of height; without the welcome, from 570; without the overture, from 475.

## 3. The overture: the welcome, then the show

Shown once, on the very first start of a new install. It has two parts. **The welcome** is a greeting on a
stage: for four seconds the glass is taller, dark, and lit by Booklight's own lamp. **The show** is the product
at work: Booklight types four things into its own field and the panel answers with the rows, the highlight and
the motion it has every day. Then it lands in K1. Nothing in it asks for anything, nothing in it is run or
fetched, and the first key ends it. A start that carries text from another app is not a first start: the
overture waits for the next plain one.

### The order

Seconds from the first frame are this seat's proposal; the motion designer sets the ms.

| Part | Seconds | Booklight types | The glass | What carries the eye on |
| --- | --- | --- | --- | --- |
| **W The welcome** | 0 to 3.9 | nothing | 68, then 468 | The light folds into the caret; the handle rises to row one and is the highlight |
| **B1 Apps** | 3.9 to 5.5 | one letter | 400 | The pill, down the list; at the sum it travels up to row one |
| **B2 A sum** | 5.5 to 7.0 | `150 + 20%` | 212 | The pill stays and only grows; the plane sets off under the headline's first letter |
| **B3 A flight** | 7.0 to 8.8 | `LH455` | 256 | The pill draws in to the first cell |
| **B4 The grid** | 8.8 to 10.2 | `emoji` and a space | 376 | The square, and the lap of light on the outline |
| **B5 The landing** | 10.2 to 10.9 | nothing | 252 | The highlight, into "Open Keyboard shortcuts" |

K1 stands at about 10.9 s; the lap of light is gone at about 11.6 s. The limit is thirteen. (B3 and B4 of the
second round are B4 and B5 now.) After the welcome's dark the lower edge goes 400, 212, 256, 376, 252: a deep
breath in, then two steps up to the peak, then the height it keeps.

### The welcome

Alex: "have sort of a 'boot screen' at the beginning […] but maybe cuter and funnier", and then: "make the
welcome visually stunning. It should be in an expanded version of our panel (same width) but you can play inside
of it with animation, graphics, motions, and light. Go all out for the boot welcome."

For these four seconds his word lifts the daily panel's rules: flat, no glow, no illustration, plain copy. What
stays: it opens as the panel opens (the seam, then the glass at the field's height); the same 720 dp; the window
is exactly the glass and nothing is drawn outside it; the caret is on x = 72; the first key ends it.

**The stage is 720 × 468.** That is the most height that is free (`eng-constraints.md` §3), and it fits every
Googlebook (a screen from 655 dp of height). Taller, to the screen's ceiling, costs half a day and a second
layout for the lower screen; the picture below does not need it.

**Three concepts**

**1. Lights on.** Booklight is a reading light, and its mark says how: the field is a lamp's head, a white pill
with the caret cut out of it, and one beam falls from it (`tools/logo.py`). So the panel plays its own mark. The
glass grows tall and goes dark. The field lights up from the caret and is the lamp's head. Light falls from it
and opens as the glass itself opens: a seam first, then out to both sides. The words were there in the dark; they
are white where the light falls. In the light a small Swiss Army knife opens: its handle is the selection's
pill and carries the cue, its five tools are Booklight's own symbols. Then the light folds back into the caret,
and the handle rises to be the highlight of row one.

```
╭──────────────────────────╮  ╭──────────────────────────╮  ╭──────────────────────────╮  ╭──────────────────────────╮
│  ▏                 [esc] │  │  ▏                 [esc] │  │██▏███████████████████████│  │██▏███████████████████████│
╰──────────────────────────╯  │                          │  │             ┃            │  │     ░░░░░░░░░░░░░░░░     │
                              │                          │  │             ┃            │  │   Welcome to Booklight   │
                              │                          │  │             ┃            │  │  Your Swiss Army knife…  │
                              │                          │  │             ┃            │  │   ░░░░░░░░░░░░░░░░░░░░   │
                              │                          │  │             ┃            │  │   ░░░░░░░░░░░░░░░░░░░░   │
                              │                          │  │             ┃            │  │  ░░░░░░░░░░░░░░░░░░░░░░  │
                              │                          │  │             ┃            │  │  ░░░░░░░░░░░░░░░░░░░░░░  │
                              │                          │  │             ┃            │  │ ░░░░░░░░░░░░░░░░░░░░░░░░ │
                              ╰──────────────────────────╯  ╰──────────────────────────╯  ╰──────────────────────────╯
0.42 s. 720 × 68. The daily   0.9 s. 720 × 468. Night: the  1.3 s. The lamp is on: the    1.7 s. The light opens out
opening, the caret on x 72    veil near black, the desk     field is its head, white,     of the seam, as the glass
                              dimmed by half, the outline   the caret a dark slit. A      did. Words are white where
                              full white                    seam of light falls on x 360  it falls

╭──────────────────────────╮  ╭──────────────────────────╮
│██▏███████████████████████│  │ ◗▏c                [esc] │
│     ░░░░░░░░░░░░░░░░     │  │(────────────────────────)│
│   Welcome to Booklight   │  │   ╱                      │
│  Your Swiss Army knife…  │  │  ░                       │
│   ░░░░░░░░░░░░░░░░░░░░   │  │                          │
│   ░░░░░  o  o  o  ░░░░   │  │                          │
│  ░░░░░ o  ╲ │ ╱  o ░░░░  │  │                          │
│  ░░░ ( Show off ⏎ ) ░░░  │  │                          │
│ ░░░░░░░░░░░░░░░░░░░░░░░░ │  │                          │
╰──────────────────────────╯  ╰──────────────────────────╯
2.3 s. The knife: five tools  3.7 s. The shaft turns and
flick out of a handle in the  folds into the caret; the
selection's colour. The       handle rises to be the pill
handle is the cue             of row one; the night lifts
```

**2. The knife.** The field is the handle. Five blades of glass, each a row's pill with a tool's symbol at its
tip, swing out from a rivet in the mark's seat and fan across the tall glass; a glint runs along each edge. The
words stand in the corner the fan leaves free. Then the blades fold flat and are the rows of the first list.
No night: it plays in the theme's own glass.

```
╭──────────────────────────╮  ╭──────────────────────────╮  ╭──────────────────────────╮  ╭──────────────────────────╮
│ ◉ ▏                [esc] │  │ ◉ ▏                [esc] │  │ ◉ ▏                [esc] │  │ ◗▏c                [esc] │
│                          │  │   ·······                │  │   ·······                │  │(o) ───────────────────── │
│                          │  │     ···· ···········     │  │  ······· ···········     │  │(o) ───────────────────── │
│                          │  │         ·····       ··(o)│  │   ····· ·····       ··(o)│  │(o) ───────────────────── │
│                          │  │              ····        │  │    ··· ···   ····        │  │(o) ───────────────────── │
│                          │  │                  ···(o)  │  │     · ··  ··     ···(o)  │  │(o) ───────────────────── │
│                          │  │                          │  │     ··  ·   ···          │  │                          │
│                          │  │                          │  │      (o) ··    ··        │  │                          │
│                          │  │                          │  │      Welcome to Booklight│  │                          │
╰──────────────────────────╯  ╰──────────────────────────╯  ╰──────────────────────────╯  ╰──────────────────────────╯
0.9 s. 720 × 468, the         1.4 s. Blades of glass swing  2.2 s. The fan is open; a     3.6 s. The blades fold flat:
theme's own glass. A rivet    out from under the field,     glint runs along each edge.   they are the rows of the
in the mark's seat            each a row's pill with a      The words stand in the free   first list
                              tool at its tip               corner
```

**3. Written in light.** Night, and the lap of light that the outline already carries. At the top's middle it
leaves the outline, dives into the glass and writes the title, letter by letter, with its tail. The tools come
as five points joined by a line, as a flight's dots are. Then the light runs up into the caret.

```
╭────────────✦─────────────╮  ╭──────────────────────────╮  ╭──────────────────────────╮  ╭──────────────────────────╮
│  ▏                 [esc] │  │  ▏                 [esc] │  │  ▏                 [esc] │  │  ✦                 [esc] │
│                          │  │                          │  │                          │  │                          │
│                          │  │   Welcome to Bo✦         │  │   Welcome to Booklight   │  │   Welcome to Booklight   │
│                          │  │                          │  │   Your Swiss Army knife… │  │   Your Swiss Army knife… │
│                          │  │                          │  │                          │  │                          │
│                          │  │                          │  │     a  c  p  s  k        │  │     a  c  p  s  k        │
│                          │  │                          │  │     ·──·──·──·──·        │  │     ·──·──·──·──·        │
│                          │  │                          │  │                          │  │                          │
│                          │  │                          │  │                          │  │                          │
╰──────────────────────────╯  ╰──────────────────────────╯  ╰──────────────────────────╯  ╰──────────────────────────╯
0.9 s. Night. The lap of      1.6 s. At the top's middle    2.6 s. The title glows; the   3.6 s. The light runs up
light runs along the outline  it leaves the outline and     tools are five points joined  into the caret
                              writes the title, letter by   by a line, as a flight's
                              letter, with a tail           dots are
```

**Chosen: 1, Lights on.** It is the only one that could open no other app: the picture on the glass is the
icon the user has just clicked, 720 dp wide and switched on. It answers "light" with a lamp, not with a glint.
It has a place for his Swiss Army knife and for the three lines of words. And its last move puts the light into
the caret, which is where the show begins.

- Concept 2 has little light in it, and glass blades on glass are hard to see over a light window. Its best
  part, tools unfolding from one handle, is kept as the small knife in concept 1.
- Concept 3 is a shine running over a wordmark. It is beautiful, and it could open any app. Writing letters with
  a point of light is also the most work to draw.

**Lights on, in full.** One night for both themes: a lamp needs the dark. Everything stands on the seam's axis,
x = 360, the one centred line the panel has; from the show on, everything stands on 72 again.

```
x    0        72     129              252           360           468              591          688 720
  0 ╭────────────────────────────────────────────────────────────────────────────────────────────────╮
    │▓▓▓▓▓▓▓▓▓▓ ▏ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ [esc] ▓▓│ the head: the field, 0 to 68, lit; the caret a dark slit on x 72
 68 │                                                                                                │ 24 dp of night between head and light
 92 │                 ╭─────────────────────────────────────────────────────────────╮                │ the light's upper edge: x 129 to 591
148 │               ╱                      Welcome to Booklight                       ╲              │ the title, rounded 40 / 600, box y 124 to 172
199 │             ╱        Your Swiss Army knife launcher. Blades not included.         ╲            │ the line, 17 / 500, box y 188 to 210
    │           ╱                                                                         ╲          │
310 │         ╱                           (calc)  (plane) (smile)                           ╲        │ three tools: discs of 36 on (302, 312), (360, 308), (418, 312)
323 │         ╱                     (app)                         (key)                     ╲        │ two more on (247, 323) and (473, 323); stems to the handle
372 │       ╱                          ╭───────────────────────────╮                          ╲      │ the handle: x 252 to 468, y 372 to 420, radius 24
    │      ╱                           │        Show off  ⏎        │                           ╲     │ the cue, 17 / 600, and the Enter mark, on y 396
420 │     ╱                            ╰───────────────────────────╯                            ╲    │
448 │    ╰───────────────────────────────────────────────────────────────────────────────────────╯   │ the light's foot: x 32 to 688
468 ╰────────────────────────────────────────────────────────────────────────────────────────────────╯
```

| Element | Place and size (dp) | Colour and strength, light and dark theme alike unless said | Drawn with |
| --- | --- | --- | --- |
| The glass | 720 × 468, radius 32. The window is exactly this | | The panel's own |
| The night | The whole glass | The veil becomes #0A0C12 at 0.80 (by day it is white at 0.42 L, near-black at 0.50 D). The desk behind dims to 0.50 (by day 0, or 0.10 L / 0.22 D) | The glass shader's own tint, changed per frame; the window's dim, as "Dim the desktop" |
| The outline | As built, 1.25 dp | White at 1.0 while it is night (0.80 L / 0.44 D by day), as the seam has it | As built |
| The head | The field's band: 720 × 68, radius 32 at all four corners: the glass as it opened | White at 0.92 | A rounded rectangle |
| The caret | x 72 to 74, y about 20 to 48 | Before the lamp: white, blinking. In the lit head: the night's colour, steady: the slit in the mark | The field's own caret |
| The `esc` cap | The field's own, right edge at 700 | In the lit head: the night's ink | As built |
| The light | At rest a trapezoid: upper edge y = 92, x 129 to 591; foot y = 448, x 32 to 688; corners rounded 24 above, 32 below. The mark's beam at the panel's scale: 0.64 of the head's width above, 0.91 below, 24 dp under the head | White: 0.30 at its upper edge, falling evenly to 0.10 at its foot. Sides and foot soften over 20 dp outside the shape. Fine grain, so it does not band | A shader over the glass (a soft-edged trapezoid with a fall-off), or a path with a gradient over a blurred copy of itself |
| The title | Centred on x = 360, box y 124 to 172. The rounded cut at 40 / 600 (the `answer` type's cut and weight, larger). About 398 dp wide in English, 474 in German; the light is 492 wide there | Two layers: white at 0.38 everywhere; white at 1.0 inside the light's shape | Text, drawn twice; the upper layer clipped to the light's path. It is the option strip's own trick: a second layer that shows only inside the highlight |
| The line | Centred on x = 360, box y 188 to 210. `title` (17 / 500), one line, 55 characters at most; the light is 520 wide there | The same two layers | The same |
| The handle | x 252 to 468, y 372 to 420: 216 × 48, radius 24 | The selection: the light scheme's `secondaryContainer` at 0.92, a white 1 px rim at 0.55 | A rounded rectangle |
| The cue | On the handle, centred on (360, 396): the words at 17 / 600, then 8 dp, then the Enter mark at 16 dp | The light scheme's `onSecondaryContainer` | Text and the `enter` symbol |
| The five tools | Discs of 36 dp centred on (247, 323), (302, 312), (360, 308), (418, 312), (473, 323). In them at 20 dp, left to right: `app`, `calc`, `plane`, `smile`, `key`: the four beats of the show, and the key step | Disc: white at 0.14. Symbol: white at 1.0 | Circles and Booklight's own symbols: a row's mark, on the night |
| Their stems | 4 dp wide, round ends, from pivots on the handle's upper edge at x = 288, 324, 360, 396, 432 to each disc's edge. Open at 40° and 20° to the left of upright, upright, 20° and 40° to the right. Folded, they lie along the handle, behind it | White at 0.60 | Lines; one angle a tool |
| A glint | A bar 10 dp wide, at 45°, that crosses a disc once as it opens, in 160 ms | White at 0.70, inside the disc only | A clipped rectangle |

**The order**

| Seconds | What happens |
| --- | --- |
| 0 to 0.42 | The daily opening, at the speed that is set (the default here): the seam, the glass at 68, the caret. The theme's own glass. The built reflection does not come in this opening |
| 0.42 to 0.9 | **Night.** The lower edge falls from 68 to 468 on `place`. The veil goes to night, the desk to 0.50, the outline to full white. The caret blinks once |
| 0.9 to 1.1 | **The lamp.** Light spreads through the field from the caret, to both ends. The caret is the dark slit |
| 1.1 to 1.3 | A seam of light, 6 dp wide, falls on x = 360 from y = 92 to 448 |
| 1.3 to 1.7 | The light opens out of its seam to both sides, on the glass's own curve (`opens`), to its shape. Title and line light up from the middle outwards; where no light is yet they show at 0.38 |
| 1.7 to 2.3 | **The knife.** The handle rises into the light's foot (12 dp, as a row does). The tools flick out of it, left to right, 66 ms apart (the `beat`), each on `pop`, each with its glint |
| 2.3 to 3.3 | Everything stands, to be read. Nothing loops |
| 3.3 to 3.9 | **The hand-over**, below |

**The words.** Three versions; each a title, a line and a cue. The title and the line are spoken by Booklight;
the cue is what the handle's Enter does, and Booklight presses it itself.

| | `first_hello_title` | `first_hello_line` | `first_hello_cue` |
| --- | --- | --- | --- |
| **A** (recommended) | Welcome to Booklight · Willkommen bei Booklight | Your Swiss Army knife launcher. Blades not included. · Dein Schweizer Taschenmesser zum Tippen. Ohne Klinge. | Show off · Kurz angeben |
| B | Welcome to Booklight · Willkommen bei Booklight | Small panel, big ego. Ten seconds, tops. · Kleines Fenster, großes Ego. Höchstens zehn Sekunden. | Watch this · Pass auf |
| C | Hi, I’m Booklight · Hallo, ich bin Booklight | Launcher by trade, Swiss Army knife at heart. · Launcher von Beruf, im Herzen Taschenmesser. | Here goes · Los geht’s |

A is recommended. It keeps his title, his picture and his "show off", and adds one joke that the drawing then
makes: what unfolds from the handle are tools, not blades. Thirteen words, read in the two seconds they stand. B
is the funniest alone, but it says nothing of what Booklight is, and it promises a length. C is the sweetest, and
gives up his title.

**How it reads over any desk.** Sums, not measurements; the prototype shows the four cases.

| | Over a white window | Over a dark window |
| --- | --- | --- |
| The stage, both themes | about #222222 | about #0B0B0D |
| The ground in the light, at the title | about #5E5E5E | about #4D4D4D |
| The title, lit | white on that: about 6 : 1 | about 8 : 1 |
| The title where no light is | about 3.5 : 1 | about 5 : 1 |
| The desk | A tenth of its contrast still shows: a bright window is a soft lighter patch. The glass stays glass | Less |

**How loud the light is** is Alex's to choose (§14). Three levels of the same picture:

| Level | The light | The words and tools |
| --- | --- | --- |
| **The shaft** (recommended, and what the table above specifies) | 0.30 to 0.10, soft sides | White, brighter where the light falls |
| The shaft, with dust and a lap | The same; about 24 specks of 1 to 2 dp drift in it at 0.3 to 0.6; the lap of light runs once round the tall outline while the words stand (2.2 s) | The same |
| Solid, as the icon | White at 0.92, hard edges: the icon exactly, as bright as a white window, hard on the eye at night | In the night's ink on the light; discs at ink 0.10 |

**The hand-over** (3.3 to 3.9 s). Everything goes back up to the field.

| Of the welcome | Becomes | How |
| --- | --- | --- |
| The handle | **The highlight**: the pill of row one (x 8 to 712, y 76 to 132) | Booklight presses it: the pressed look, 80 ms. The tools fold back into it, right to left. Its four edges travel to row one's seat on `place`; the cue fades inside it (80); its colour goes to the theme's own selection as the night lifts. From here to K1 it is never gone |
| The light | **The caret** | The foot rises while the upper edge slides left: the shaft turns, as a lamp's head is turned, and its sides close on x = 73. The head's light closes on the caret from both ends. For a frame all that is left of the lamp is the caret, white, x 72 to 74 |
| The caret | The place of **the first letter** | The show's first letter lands on it, at x = 72, in that frame or the next |
| The title and the line | Nothing | As the light leaves them they fall to their unlit layer; that fades where it stands (80) |
| The night | The theme's own glass; the desk at the show's dim (0.10 L / 0.22 D) | Veil and dim return together; the outline relaxes to its rest |
| The mark's seat (empty in the welcome: the whole panel is the mark) | Booklight's mark, 22 dp, strong ink, centred on x = 38 | It comes up as marks do there (0.6 to 1 on `pop`) as the head's light passes the seat. It stays until the chip takes the seat in B4, and says who is typing |
| The field's `esc` cap | The footer's `esc` Skip | As built when a list lands |
| The glass | The first list's | The lower edge draws up from 468 to 400 on `place` as the rows land. It does not go back to 68 |

**During the welcome, the user**

| The user | What happens |
| --- | --- |
| A key that types (a letter, a digit, a sign, Backspace) | In that frame the lamp is out, as a lamp is: the field's band is the theme's own glass with the character in it, in the theme's ink. Below it the stage's parts fade where they stand (80) and the night lifts (160). The list for the character lands as any list, and the lower edge goes to its height from where it is. No show. K1 stands when the field is empty again |
| Enter, or a click on the handle | The hand-over starts at once and the show begins. The cue is the one armed thing in the overture that is the user's to run |
| Esc, Tab, an arrow, a click elsewhere on the glass | K1 lands: the lamp goes out, the stage's parts fade, the night lifts, the lower edge goes from where it is to 252, K1 is set down as rows are. One press, one step |
| A click outside, the focus lost | The panel closes. K1 at the next opening |

**With the system's animations off, or a screen reader on, there is no welcome and no show.** K1 stands whole in
the first frame. For that opening the field's placeholder is the title (`first_hello_title`), and a screen
reader says it before K1's words. No still of the stage stands: it would have to wait for a key or leave by a
clock, and it is a picture made to move.

### The show

**Four rules make it one movement and not four slides**

1. **The text is typed over, never cleared.** The first character of the next thing replaces the old text in one
   frame, as typing over a selection does. The field is never empty between two beats and the glass never
   returns to 68.
2. **Rows stand until the next thing's row lands** (the rule the usual rows have, `zero-state.md` Z3). Then one
   list change: what leaves fades where it stands, what comes rises.
3. **One highlight, never gone.** Born as the knife's handle; the list's pill in the first beat; the same pill
   at the answer's height, then at the flight's; reshaped to a cell's square in the grid; at the end, K1's armed
   answer. It travels. It is never faded out in one place and in at another.
4. **The lower edge breathes:** 400, 212, 256, 376, 252, each on `place` from where it is.

| Beat | Booklight types | What shows, by its real parts | This device's · fixed |
| --- | --- | --- | --- |
| **B1 Apps** | One letter: the one that starts the most app names here | Five app rows cascade in (`ResultsBody`, 22 ms apart), their icons loaded before. The pill is already in row one's seat; its strip slides in (`ActionStrip`: "Open ⏎" on its pane). Down, Down: the pill stretches and gathers to row three, each row's strip coming 60 ms after it. Tab, Tab: the pane glides along that row's strip and the names unroll. Footer: `tab` Actions · `esc` Skip | The letter, the apps, their icons, what each strip holds: this device's. Fixed: apps only, five rows at most, the four moves |
| **B2 A sum** | `150 + 20%` | The app rows stand until `150 + 2` is a sum. Then one list change: the answer's row, 92 dp: the `calc` disc, the typed sum as its small line, the answer in the `answer` type (rounded, 34 / 600), "Copy ⏎". Then `0`, then `%`: the answer rolls 152, 170, 180 | Fixed |
| **B3 A flight** | `LH455` | The answer stands until the number is whole. Then the flight's row, 136 dp, in row one's seat: below | Fixed: an example, held in the app. Nothing is asked of any service |
| **B4 The grid** | `emoji` and a space | The flight stands until the space. Then the keyword becomes the chip where the mark was (`ScopeChip`: the `smile` symbol, "Emoji"), the placeholder is the scope's, and the grid arrives in its diagonal wave (`GridBody`, 14 × 5 cells, 8 ms a step). The pill's four edges and its radius go to the first cell: it is a 44 dp square now. Right three times, Down: the square travels, and the footer's left end names each cell. **As the wave lands, the lap of light sets off** from the middle of the top edge | Fixed: the table's first 70 emoji, in the device's emoji font |

```
x    0 8 20 38 56 72                                                                        700 712 720
  0 ╭────────────────────────────────────────────────────────────────────────────────────────────────╮
    │   (B)    c|                                                                                    │ field 68: Booklight's mark on x 38, its letter from 72
 68 │                                                                                                │ pad 8
 76 │  [icon]  Calculator                                                                      App   │ rows of 56: tops 76, 132, 188, 244, 300
132 │  [icon]  Calendar                                                                        App   │
188 │╭──────────────────────────────────────────────────────────────────────────────────────────────╮│ the pill, x 8 to 712, after Down, Down
    ││ [icon]  Camera                                                    [ Open ⏎ ]  (window)  (v)  ││ the strip ends at 700; Tab, Tab glides the pane along it
    │╰──────────────────────────────────────────────────────────────────────────────────────────────╯│
244 │  [icon]  Chrome                                                                          App   │
300 │  [icon]  Clock                                                                           App   │
356 │                                                                                                │ pad 8
364 │                                                                   [tab] Actions   [esc] Skip   │ footer 36, its line on y 378
400 ╰────────────────────────────────────────────────────────────────────────────────────────────────╯
```

App names in the figure stand in for the device's own. Rows: icon box x 20 to 56, title from 72 (`title`), the
kind's right edge at 700, as in every list.

```
  0 ╭────────────────────────────────────────────────────────────────────────────────────────────────╮
    │   (B)    150 + 20%|                                                                            │ field 68
 68 │                                                                                                │ pad 8
 76 │╭──────────────────────────────────────────────────────────────────────────────────────────────╮│ the pill on row one, now 92 high
    ││ (calc)  150 + 20%                                                                            ││ small line: the typed sum, `label`
    ││         180                                                                      [ Copy ⏎ ]  ││ the answer, `answer` type; it rolled 152, 170, 180
    │╰──────────────────────────────────────────────────────────────────────────────────────────────╯│ mark and strip on y 122
168 │                                                                                                │ pad 8
176 │                                                                                   [esc] Skip   │ footer 36, its line on y 190
212 ╰────────────────────────────────────────────────────────────────────────────────────────────────╯
```

**B3, the flight.** The row is the real one (`design-system.md` §14, `flights-row/design.md`): 136 dp, line one,
the headline with its one badge, the line with the plane on it, the two ends. It shows a flight in the air, on
time.

```
  0 ╭────────────────────────────────────────────────────────────────────────────────────────────────╮
    │   (B)    LH455|                                                                                │ field 68
 68 │                                                                                                │ pad 8
 76 │╭──────────────────────────────────────────────────────────────────────────────────────────────╮│ the pill, now 136 high: y 76 to 212
 88 ││         LH 455 · Lufthansa · San Francisco → Frankfurt/Main                                  ││ line one, y 88 to 108
110 ││(plane)  Lands in 4 h 07 min  ( On time )                                            Example  ││ headline y 110 to 134; badge; the kind's word; all centred on y 122
    ││                                                                                              ││
160 ││         ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ✈ · · · · · · · · · · · · · · · ·  ││ the line on y 160, x 72 to 700; the plane's tail at x 443
176 ││         SFO 2:47 PM  Boeing 747-8                                  Terminal 1  10:25 AM FRA  ││ the two ends, y 176 to 198
    │╰──────────────────────────────────────────────────────────────────────────────────────────────╯│
212 │                                                                                                │ footer band 44, its line on y 234
    │  An example. Live times need your own AirLabs key.                                [esc] Skip   │
256 ╰────────────────────────────────────────────────────────────────────────────────────────────────╯
```

| The example, held as fixed data | |
| --- | --- |
| Typed | `LH455`; shown as "LH 455" |
| Line one | LH 455 · Lufthansa · San Francisco → Frankfurt/Main |
| Left | SFO at 2:47 PM (planned 2:40 PM), with the small words "Boeing 747-8" |
| Lands | FRA at 10:25 AM, as planned, with the small words "Terminal 1" |
| Looked at | 4 h 07 min before it lands: 391 of its 638 minutes in the air are over |
| Headline · badge | "Lands in 4 h 07 min" · "On time", green · German: „Landung in 4 Std. 07 Min.“ · „Pünktlich“ |
| The plane | 61 % of the way: its tail at x = 443, its nose at 463. Solid line from 72 to 439; dots ahead from 461 to 700 |
| Times | In each airport's own clock, 12 or 24 hours as the device has it (German: 14:47, 10:25). No day stands beside them, and the minutes do not count down while it shows |
| Not shown | Gate, belt, a day: the service names none here |

- **How the glass says it is an example.** In two seats the product already has, and in few words.
  **The row's right end**, where an unselected row names its kind ("Flight"), on the headline's centre line
  (y = 122), right edge at 700, `label`, strong ink: `first_show_example`, "Example" · „Beispiel“. The row has no
  strip: nothing of an example can be opened, copied or pinned.
  **The footer's left end**, where a real flight's row names the source of its answer, `hint` type, second ink,
  from x = 20: `first_show_flight`, "An example. Live times need your own AirLabs key." · „Ein Beispiel. Live-
  Zeiten brauchen deinen eigenen AirLabs-Schlüssel.“ It says "AirLabs key" and not "a key", because two beats
  later Booklight asks for a key of another kind.
- **The plane arrives as it does when an answer lands** (`design-system.md` §6, row 22). The row rises with its
  words; 60 ms later the plane fades in at the line's start, under the headline's first letter, and flies to its
  place, the flown part growing behind it and the dots ahead stepping from "at rest" to "still to go"
  (`flies`: about half a second for this way). Then it is still.
- **The one highlight.** The pill is on row one, 92 high. The flight's row lands in the same seat: the pill's
  upper edge stays on y = 76 and its lower edge goes from 168 to 212 on `place`, with the row. The glass grows
  44 dp, from 212 to 256.
- **Why here, after the sum and before the grid.** The show then climbs in two steps to its peak, and it ends
  on something every new user has at once. Last, before the key step, it would end on the one thing in the show
  they cannot have yet.

**B4, the grid.** As in the second round; only its place in the order is new.

```
  0 ╭────────────────────────────────────────────────────────────────────────────────────────────────╮
    │  ( :) Emoji )   Emoji name                                                                     │ the chip, x 20 to about 112, its symbol on x 38
 68 │                                                                                                │ pad 8, and 8 inside the row
 84 │   o      o     o      o     o      o      o     o      o     o      o     o      o      o      │ 14 cells of 48 at a pitch of 49.4 dp, from x = 14
132 │   o      o     o     [o]    o      o      o     o      o     o      o     o      o      o      │ the square: 44 dp, radius 14, after Right x 3, Down
180 │   o      o     o      o     o      o      o     o      o     o      o     o      o      o      │
228 │   o      o     o      o     o      o      o     o      o     o      o     o      o      o      │
276 │   o      o     o      o     o      o      o     o      o     o      o     o      o      o      │ the grid ends at 324, its row at 332
332 │                                                                                                │ pad 8
340 │  kissing face                                            [⏎] Copy   [↑↓←→] Move   [esc] Skip   │ footer 36, its line on y 354
376 ╰────────────────────────────────────────────────────────────────────────────────────────────────╯
```

### The landing in K1 (B5)

One sweep from the top left to the bottom right takes the grid away and leaves K1. The motion designer has cued
it by the light: it begins as the lap turns onto the lower edge (`motion.md` §2).

| Part of the last beat | Becomes in K1 | How |
| --- | --- | --- |
| The glass | The same sheet | Its lower edge goes from 376 to 252 on `place` |
| The highlight: the square on the fourth cell of the second line (x 164 to 208, y 134 to 178) | The pill of the armed answer (x about 482 to 700, y 154 to 186) | It travels, almost level: the leading edge first, the other following; radius 14 to 16. "Open Keyboard shortcuts" and its Enter mark fade in inside it once it is as wide as they are |
| The cells | Nothing | They leave in the wave they came in, top left first. K1's parts follow that wave: mark, title and line, counter; then the caps one by one from x = 72; the caption; "Not now" |
| The chip | The search engine's mark | As a scope is left (design system §6, row 7): the chip's rectangle shrinks and fades, the mark grows from 0.6 on x = 38 |
| The scope's placeholder | "Search apps, settings and the web" | The placeholder's own fade |
| The footer with `esc` Skip | The field's `esc` cap, right edge at 700 | As built when a list goes |
| The lap of light | It is still running, round the lower half of the outline, while the glass draws in. It dims along the top left and is gone as K1 rests | As built; it follows the outline as the height changes |
| The dim | The desk as it is | It lifts on its own time |

K1 then stands exactly as §5 draws it. In a replay with a key that works the landing is K4, and the highlight
goes to "Go on".

**During the show, the user**

| The user | What happens |
| --- | --- |
| A key that types | The overture ends in that frame. Booklight's text and chip are gone, the user's character is in the field, the list is the real one for it. The mark is the engine's, the dim lifts, the light fades in 160 ms. K1 stands when the field is empty again, as a card does. The overture does not come again |
| Esc, Enter, Tab, an arrow, a click on the glass | K1 lands at once, from wherever the show is. Nothing is run: the highlighted row was Booklight's choice. One press, one step: a held Esc does not go on to close. Enter counts on K1 only once its armed answer shows |
| A modifier alone | Nothing |
| A click outside, the focus lost | The panel closes, as always. The next opening shows K1 |

### Where each of Booklight's own things has its moment

| Booklight's own | Its moment |
| --- | --- |
| The mark | The welcome: the whole panel is the mark, lit. Then, small, in the field's mark seat while Booklight types. It never draws itself in the middle of a screen |
| The white outline | The seam; then the night, where at full white it is the brightest line on the screen |
| Light | The lamp and its shaft in the welcome; the lap round the outline at the grid's peak |
| The glass | The night, where a tenth of the desk still shows; B1 at 400 dp by day, the most glass the show has |
| The unfolding | Twice: the glass out of its seam, and the light out of its own |
| The field's type | Every typed character; B2's sum |
| The cascade | B1: five rows, 22 ms apart |
| The travelling highlight | From the knife's handle to row one, down the list, along a strip, up to the sum, around the flight, into a cell, across the grid, into K1's answer: one highlight for seven seconds |
| The answer's type | The welcome's title, in its cut; B2: 180, rolled into place |
| The flight's row | B3: the line, the plane flying to its place, the one green badge |
| The wave | B4 in, the landing out |
| The check | Not here. It is kept for the reveal |

### Light and dark, and German

The welcome is one night in both themes. The show is the product, so it follows the theme:

| | Light | Dark |
| --- | --- | --- |
| Veil | `surfaceContainerLowest` 0.42 | 0.50 |
| Dim behind, during the show | 0.10 | 0.22 |
| The pill and the square | `secondaryContainer` 0.78 | 0.42 |
| The pane on the pill | 0.62 | 0.36 |
| Titles, the answer, the flight's line and plane | ink 1.0 | ink 1.0 |
| The flight's dots still to go | ink 0.50 | 0.55 |
| The badge "On time" | #B6F2BE under #00210A, the fill at 0.88 | #0A5226 under #B6F2BE |
| Emoji | their own colours | the same |
| The lap | white; not seen over a white window (accepted in design system §12) | white |

German in the show: the welcome's three strings; „Öffnen“, „Fenster“, „Aktionen“, „Kopieren“, „Bewegen“; the
flight's „Landung in 4 Std. 07 Min.“, „Pünktlich“, „Beispiel“ and its footer line; the chip "Emoji", its
placeholder „Emoji-Name“; the cells' German names; the footer's word for Esc, `first_skip`, "Skip" ·
„Überspringen“. All but the welcome's, the two for the example and `first_skip` are the product's own.

### Ways not taken for the show

1. **End on keys.** The last thing typed is `k` and a space: the system's own shortcuts as rows with their key
   caps; one row's caps grow into K1's. A good bridge into "Give Booklight a key". But the peak would be a list
   of words, in the system's wording, different on every build; and caps would have to change their type size on
   the way, which nothing in the panel does.
2. **The flight last.** The glass would hardly move at the landing (256 to 252), and the highlight would only
   draw in to the answer. But the show would end on an example the user cannot have yet, and "AirLabs key" would
   stand a second before "Give Booklight a key".

### What to cut first, if it proves too long or too busy on a device

1. Tab, Tab in B1 (about 0.5 s).
2. Three of the square's four moves in B4.
3. The glints on the tools; then the tools open together.
4. The seam of light: the light opens at once (0.2 s).
5. The shaft's turn at the hand-over: it fades where it stands, and the caret is simply there.
6. The sum (1.5 s): apps, the flight, the grid.

Never cut: the night and the lamp, the words, the flight, the cascade, the wave, the one highlight from the
handle to K1, the landing cued by the light, the first key ending it.

### New for the two seats that follow

**For the motion designer.** To time: the night's fall with the lower edge; the lamp spreading from the caret;
the seam of light and its opening; the tools' `beat`; the read; the hand-over's three moves (the tools fold, the
shaft turns into the caret, the handle rises) so that one leads at a time; the night's lift. In the show: `LH455`
typed over the sum, the row's landing, the plane's flight, how long it rests before `emoji`.

**What changes in each frame of the welcome, and only this:** how dark the night is (one number); how far the
head's light has spread from the caret (one); the light's shape (three: how far it has fallen, how far it is
open, where its foot's middle stands); each tool's angle (five); the handle's four edges. Everything else is
laid out once. The window is resized only while the lower edge moves: for half a second at the start and at the
hand-over.

**What could be a shader:** the light (its soft sides, its fall-off, its grain) and the head's spreading light,
in the glass's own shader or one pass over it. The lit words need none: they are text drawn again inside a clip.

**For the tech lead, to price:** a window 468 dp high for the welcome; the veil's colour and the window's dim as
values set per frame; the field's band lit, with no text in it; the stage's layers inside the glass's clip; the
bright layer of the words clipped to the light's path; the tools and the handle; the handle handed to the list
as its pill; the example flight as held data (the debug build's `flight show` does this with its samples, and
its footer says "sample"); a selected row without a strip, with a word at its right end; a footer line for it.
And as before: Booklight typing with waits and typing over (`typeOut` can do neither); a list held to one kind
and five rows a beat, and rows that stand until the next row lands; Booklight's own moves (`move`, `arm`,
`moveCell`); the cells leaving as a wave; the highlight's travel from a cell into the strip; keys and clicks
that land K1; the mark in the seat; a trigger for the lap that Booklight's own typing does not put out. If the
apps and their icons are not ready when the cue is due, the welcome stands on; after one more second K1 lands.

**What the prototype must show, to be judged:** the welcome over a white window and over a dark one, in both
themes; the three levels of light; the three versions of the words, in English and German; the hand-over into
B1 at full speed and at a quarter; a letter typed at three moments (night falling, the lamp on, the hand-over);
the flight's beat with its two lines of "example"; the whole from first frame to K1 with a clock on it.

## 4. The skeleton, and what is new

The stage comes at the gate, as the card does (`Under.choose`: it has the place first). Its parts wait for the
glass's edge, as the usual rows do (`ends()`).

| Part | Layout (dp) | Look |
| --- | --- | --- |
| Field | 0 to 68, untouched: mark centred on x = 38, text from 72, placeholder from 74, the `esc` cap ending at 700 | As built |
| Seat 1 | y 76 to 132, a row's seat (`Metrics.row`), centre line y = 104 | No fill, no pill: it is not a row and cannot be selected |
| Its mark | A 36 dp disc, x 20 to 56, centred on (38, 104), the step's symbol at 20 dp | The disc of an unselected row: ink 0.08 L / 0.12 D, glyph at second ink |
| Its words | From x = 72 to 16 dp before the counter. The row's two lines: `title` (17 / 500), then `label` (as built `SMALL`: 14 / 500 / 0.1) | Title strong ink, line second ink. One line each; too long, the 24 dp fade, never an ellipsis |
| Counter | "1 of 5", `label`, right edge at 700, centred on y = 104: where a row's kind stands | Second ink |
| Band | y 132 to 244 | |
| Recipe | In the band from x = 72, at most to 476: large caps and typed parts on one baseline, all centred on y = 188 | Below |
| Caption | `hint` type (13 / 500 / 0.1), from x = 72, one line, centred on y = 230: the line a footer's words stand on, 22 dp above the lower edge. It ends 24 dp before the answers | Second ink. Too long: the 24 dp fade |
| Answers | The card's strip, vertical (`OptionStrip`): slots 32 dp high, 4 apart, as wide as the widest, `word` (14 / 600). Right edge at 700, the block centred on y = 188 (two slots: 154 to 222; one: 172 to 204) | Armed: the pill's recipe and the Enter mark. Not lit: second ink, no mark, and the footer's `tab` cap (22 dp) 10 dp before the strip, centred on y = 188, as on a tip |

**New: the large key cap.** Height 56 (`Metrics.row`). Least width 56. 20 dp inside each end. Radius 16 (the
swatch's). Fill: the key cap's, ink 0.10 L / 0.14 D. A white 1 px ring inside its edge at 0.55 L / 0.30 D (the
scope chip's). No bevel, no shadow, no gradient. Its label is the key's name in `field` type (24 / 500), strong
ink; Enter is the drawn `enter` mark at 24 dp. Its width is measured once.

- **A chord** (keys together): a "+" between caps in `title` at second ink, 12 dp either side.
- **A sequence** (one after another): 12 dp between caps, no sign; 16 dp between a cap and a typed part.
- **A typed part** is `field` type, strong ink, no fill: it looks as it will in the field.
- **Pressed** (new state): the fill gains ink 0.08 (the design system's pressed token) and the cap is drawn at
  0.96 of its size about its centre. Nothing else changes. No colour: the selection stays the one coloured surface.

**New: "your key".** One blank large cap, 112 × 56 (two rows' heights wide), x 72 to 184, y 160 to 216. It has
this width whatever was suggested: as wide as the suggestion it would be a bar, and as a square it reads as a
checkbox. At rest: the cap's fill and ring, nothing in it. Pressed: as above. Done: the drawn check
(`DrawnCheck`) at 28 dp, stroke 2.5 dp, strong ink, centred on (128, 188). It never carries a letter: Booklight
cannot know the keys (fact 7).

## 5. Step 1: your key

```
x    0 8 20 38 56 72                                                                        700 712 720
  0 ╭────────────────────────────────────────────────────────────────────────────────────────────────╮
    │    G     Search apps, settings and the web                                             [esc]   │ field 68, live
 68 │                                                                                                │ pad 8
 76 │   (key)  Give Booklight a key                                                         1 of 5   │ seat 1: 56, centre y 104
    │          One press, from any app, and Booklight is there.                                      │
132 │                                                                                                │ band: 112
160 │          ╭──────────╮   ╭────────────────╮                 ╭───────────────────────────────╮   │ caps 160 to 216, centre y 188
    │          │  Action  │ + │  Quick Insert  │                 │ Open Keyboard shortcuts    ⏎  │   │ x 72 to about 414; strip 154 to 222
    │          ╰──────────╯   ╰────────────────╯                 ╰───────────────────────────────╯   │
216 │                                                              Not now                           │
230 │          Quick Insert is left of A · or any keys you like                                      │ the caption, centred on y 230
244 │                                                                                                │ pad 8
252 ╰────────────────────────────────────────────────────────────────────────────────────────────────╯
```

Four screens, all 252 dp, one skeleton. Only the words and the band change.

| | K1 Ask | K2 Back, no key yet | K3 "It didn't work" | K4 Your key works |
| --- | --- | --- | --- | --- |
| Mark | `key` | `key` | `key` | `key` |
| When | The overture lands; or first run starts without one | The helper was closed (X, Esc) and no key came | The same, from the second time on | The key opened or uncovered the panel |
| Title | Give Booklight a key | Now press your keys | Didn't work? | Your key works |
| Line | One press, from any app, and Booklight is there. | Nothing happens? Open Keyboard shortcuts once more. | Click Customize first. Hold Action, then press the second key. | The same keys put Booklight away. |
| Band | The suggestion: `Action` + `Quick Insert` | Your key, blank | Your key, blank | Your key, with the check |
| Caption | Quick Insert is left of A · or any keys you like | your keys | Or try Action + J | none |
| Answers | **Open Keyboard shortcuts** · Not now | Open Keyboard shortcuts · Not now | **Open Keyboard shortcuts** · Not now | **Go on** (the replay adds: Change the key) |
| Armed | The first | Nothing: the thing to press is the key | The first | The first |

**The suggestion's measures.** `Action` about 118 dp, the "+" with its air 34, `Quick Insert` about 190 (its
letters about 150, 20 inside each end): 342 in all, x 72 to about 414. The strip begins near 482 (English) or
511 (German), so at least 68 dp stay clear. The words that stood beside the caps in the first round do not fit
there any more; they are the caption, under the caps, on every K screen.

**Where the key is.** Few people know the name. The caption says it by a key everyone finds: "left of A", true
on English and German keyboards, the two languages Booklight speaks. The key's name on the cap, in the caption
and in the helper's rows is the system's own where the build can read it; "Quick Insert" is the fallback, not
translated.

**A keyboard without the key** (most external ones): the caps are `Action` + `J` (about 208 dp), K1's caption is "or
any keys you like", K3's is "If J was taken, try R", the helper's rows carry Action + J and say J.

| Resource | English | German |
| --- | --- | --- |
| `card_shortcut_title` (there) | Give Booklight a key | Booklight eine Taste geben |
| `first_key_text` | One press, from any app, and Booklight is there. | Ein Druck, aus jeder App, und Booklight ist da. |
| `key_action` (there) | Action | Aktion |
| `first_key_quick` · `first_key_letter` (neither translated) | Quick Insert · J | Quick Insert · J |
| `first_key_where` | %1$s is left of A | %1$s liegt links neben A |
| `first_key_any` | or any keys you like | oder Tasten deiner Wahl |
| `card_shortcut_action` · `card_not_now` (there) | Open Keyboard shortcuts · Not now | Tastenkürzel öffnen · Nicht jetzt |
| `first_key_press` | Now press your keys | Jetzt deine Tasten drücken |
| `first_key_press_text` | Nothing happens? Open Keyboard shortcuts once more. | Nichts passiert? Öffne die Tastenkürzel noch einmal. |
| `first_key_yours` | your keys | deine Tasten |
| `first_key_retry` | Didn’t work? | Hat nicht geklappt? |
| `first_key_retry_text` | Click %1$s first. Hold Action, then press the second key. | Zuerst „%1$s“ klicken. Aktion halten, dann die zweite Taste drücken. |
| `first_key_try_j` · `first_key_other` | Or try Action + J · If J was taken, try R | Oder Aktion + J versuchen · War J vergeben, nimm R |
| `key_works` (there) | Your key works | Deine Taste funktioniert |
| `first_key_works_text` | The same keys put Booklight away. | Dieselben Tasten schließen Booklight wieder. |
| `first_next` · `first_key_change` | Go on · Change the key | Weiter · Taste ändern |
| `first_step` | %1$d of %2$d | %1$d von %2$d |

K1's caption is `first_key_where`, " · ", `first_key_any`; without the key, `first_key_any` alone.

**Keys, on every screen of the stage** (differences are said per screen):

| Key | Does |
| --- | --- |
| A letter | It is typed, in the same frame. The stage fades where it stands and the list comes. The field emptied: the stage is back, as the card is today |
| Enter | Runs the armed answer. Nothing armed: nothing, and it is not kept |
| Tab, Shift + Tab | Arms the next or the previous answer, wrapping; the first press arms the first. One press, one step |
| Esc, a click outside | Closes. The same screen at the next opening |
| The user's keys | With K2 or K3 in view: K4. On every other screen, as every day: they put the panel away |
| Pointer | Moving onto an answer arms it; a click runs it. Caps are pictures: no hover, no click |

**The hand-off.** Enter on K1 asks for the helper and the panel stays. The helper covers it (fact 4). Under it,
unseen, the stage is set to K2, so that whatever uncovers the panel finds a true screen. (K2 and K4 share the
band: if one frame of K2 shows as the helper falls away, only words differ.)

The helper's page is the instruction. It is read once: after the name is typed the helper shows only the app's
row (`shortcut-setup.md` §2). So it is five short rows, numbered at the start of each label. Every row carries
the suggested keys at its right end: the device's own Action glyph and the "Quick Insert" cap stay in view while
the user reads, and teach which keys are meant.

| # | Resource | English | German | Keys |
| --- | --- | --- | --- | --- |
| Heading | `first_helper_title` | A shortcut for this app, in five steps | Ein Tastenkürzel für diese App, in fünf Schritten | none |
| 1 | `first_helper_1` | 1. Click %1$s, top right | 1. Oben rechts auf „%1$s“ klicken | Action + Quick Insert |
| 2 | `first_helper_2` | 2. Type this app’s name, then click + | 2. Den Namen dieser App tippen, dann auf + klicken | the same |
| 3 | `first_helper_3` | 3. Hold Action and press %3$s, or any keys you like | 3. Aktionstaste halten und %3$s drücken, oder Tasten deiner Wahl | the same |
| 4 | `first_helper_4` | 4. Click %2$s | 4. Auf „%2$s“ klicken | the same |
| 5 | `first_helper_5` | 5. Press your keys again. This closes by itself. | 5. Die Tasten noch einmal drücken. Das hier schließt sich von selbst. | the same |

- %1$s and %2$s are the system's own words for "Customize" and "Set shortcut", %3$s the key's name, each read
  from the system where the build can (fact 8; the tech lead checks). The fallbacks `first_sys_customize` and
  `first_sys_set` are "Customize" and "Set shortcut"; their German must be read off a device, not translated at
  a desk.
- No label and no heading holds the app's name (fact 3).
- Row 3 in English is the 59-character row that stood on three lines (fact 9): beside the wider keys a label has
  about 20 characters a line. That makes about twelve lines in English and more in German. Whether all five
  still show without scrolling is a device check (§15).

**Coming back.** The three states are K2, K3 and K4. How each is reached:

| How | What the user sees |
| --- | --- |
| **The keys are pressed while the helper is open** (fact 6): the main way | The dialog falls away. The panel is there with K4: §8, "The reveal" |
| The helper is closed by X or Esc | K2, standing still: it was set under the helper. From the second time, K3 |
| The keys are pressed with K2 or K3 in view | The blank key goes down, comes up, the check draws; title and line roll to K4's; the caption goes; the answers change where they stand |
| The panel had closed; the keys open a new one, perhaps days later | The daily opening, then K4 arrives at the gate, its check drawing |

A press of keys that are not yet the shortcut does nothing on the panel: Booklight does not see it (fact 7).
"Not now" on K1, K2 or K3 ends first run (`pm.md` §6): the stage folds to the bare field, whose placeholder says
for that opening `first_later_hint`: "First steps wait in Booklight’s window" · „Die ersten Schritte warten in
Booklights Fenster“.

## 6. Steps 2 to 4: the lessons, in one opening

From K4 on it is one opening: L2, L3, L4, Q, C, the fold. The user's keys are pressed once, at the reveal. Each
lesson is still the real field and the real list, and any app and any sum count.

```
 76 │ (search) Search inside an app                                                         3 of 5   │ seat 1
    │          Tab moves along the row, Enter does. Then type what to look for.                      │
132 │                                                                                                │
160 │                 ╭───────╮ ╭─────╮          ╭─────╮                                             │ the recipe: x 72 to 476 at most
    │          yout   │  Tab  │ │  ⏎  │   lofi   │  ⏎  │                        [tab]   Skip         │ one slot, 172 to 204, not lit
216 │                 ╰───────╯ ╰─────╯          ╰─────╯                                             │
230 │          Enter also puts Booklight away. Today it comes back.                                  │ the caption, centred on y 230
244 │                                                                                                │ pad 8
```

| | L2 Open an app | L3 Search inside an app | L4 A sum |
| --- | --- | --- | --- |
| Mark | `open` | `search` | `calc` |
| Title · `first_open_title`, `first_search_title`, `first_sum_title` | Open an app · Eine App öffnen | Search inside an app · In einer App suchen | A sum · Eine Rechnung |
| Line · `…_text` | Type its first letters, then Enter. · Die ersten Buchstaben tippen, dann Enter. | Tab moves along the row, Enter does. Then type what to look for. · Tab geht die Zeile entlang, Enter führt aus. Dann tippen, was du suchst. | The answer stands there as you type. Enter copies it. · Das Ergebnis steht da, während du tippst. Enter kopiert es. |
| Recipe | `yout` `⏎` | `yout` `Tab` `⏎` `lofi` `⏎` | `150 + 20%` `⏎` |
| Caption | `first_enter_back`: Enter also puts Booklight away. Today it comes back. · Sonst schließt Enter auch Booklight. Heute kommt es zurück. | the same | `first_enter_stays`: Enter also puts Booklight away. Today it stays. · Sonst schließt Enter auch Booklight. Heute bleibt es. |
| Placeholder · `…_hint` | An app’s first letters · Die ersten Buchstaben einer App | %1$s, then Tab · %1$s, dann Tab | 150 + 20% (`first_sum_example`, not translated) |
| Answer | Skip · Überspringen (`first_skip`), not lit | the same | the same |
| Done when | An app's Open ran | An app's Search ran | A sum was copied |

- **The example is this device's.** L2 and L3 use one app, the one `Guide.appSearch()` picks, so that step 3
  builds on step 2. Its letters are the shortest start of its name, three at least, that puts its row first. Where
  no app qualifies: L2 `sett` `⏎`, L3 `s` `Tab` `wifi` `⏎` (Settings).
- **Nothing is armed.** Enter on the empty field does nothing, as every day. Tab, Enter skips.
- **The caption is how a lesson says what other days are like**: one line under the recipe, and the same
  thought in the coach line at the moment of Enter. No paragraph.

**What Enter does now**

| Where | Enter | The panel | What comes |
| --- | --- | --- | --- |
| L2, an app's row | Opens the app, as every day | Steps aside and is back by itself (below) | L3, at the new panel's gate |
| L3, Search armed on the app's row | Makes the app the chip, as built | Stays, as built | The scope's own placeholder; the user types the word |
| L3, the word under the chip | Runs the search: the app opens on its results | Steps aside and is back by itself | L4 |
| L4, a sum's row | Copies. The footer says "✓ Copied" | **Stays.** 520 ms later, when on any other day the panel would go, the field is emptied in one frame, the row and the footer fade, and Q rises. The lower edge goes from the list's height to 324 on `place`; it never visits 68 | Q |
| Any lesson, some other row (a settings page, the web) | What it does every day | Goes, as every day | The user's keys bring the same lesson |

**The step aside.** Its parts are verified (fact 10); the whole was not filmed.

| # | What happens | Whose motion |
| --- | --- | --- |
| 1 | Enter. The armed slot is pressed. The app is started, and the step counts as done | As every day |
| 2 | The glass folds to its seam and is gone (155 ms) | As every day |
| 3 | The app's window comes to the front | The system's |
| 4 | Booklight asks for itself again within 0.4 s of the app's start (at once and after 0.4 s both worked; 1.5 s did not). This seat would take the late end, so that the app is seen to arrive | New |
| 5 | A new panel opens in front of the app: **its ordinary opening**, at the user's own speed: the seam on the same centre line, the glass, the field. The app shows through the glass. The system's own fade lies over its first frames, as at every opening | As every day |
| 6 | At the gate the next lesson is set down, as K1 is | §4 |

- **Why the ordinary opening.** It is the move the user's own keys will make from tomorrow, here seen over the
  app they have just opened. A panel that stood at once would pop in on the system's fade. A shortened opening
  would be a third kind, found nowhere else.
- **If Booklight does not come back** (the system says no, or the user has clicked into the app): nothing is
  lost. The user's keys open the panel on the next lesson.
- **The user's keys pressed during the blink** must count as the return, not close the returning panel (for the
  tech lead).
- **Animations off:** the new panel stands whole in its first frame, the lesson with it.

**Practice only: the fallback**, if the real panel or the HP Googlebook does not bear the step aside. Enter in L2
and on L3's word starts nothing. The slot is pressed; the footer says with its check `first_practice_open`,
"That opens %1$s" · „Das öffnet %1$s“ (or `first_practice_search`, "That searches %1$s" · „Das durchsucht
%1$s“); 520 ms later the list gives way to the next lesson as L4's does to Q, and 252 stays. The caption is
`first_practice`: "Practice: nothing opens. Enter also puts Booklight away." · „Zum Üben: nichts öffnet sich.
Sonst schließt Enter auch Booklight.“ It is second because it is an Enter that exists nowhere else, and because
nothing real happens.

**While the user types.** The first letter puts the lesson away, as it puts a card away, and the real list
stands in its real seats. No line is added under the field: it would push row one off y = 76. Guidance uses the
two free seats:

| Seat | Says |
| --- | --- |
| The placeholder | Before the first letter, what to type (the table above). Under the app's chip in step 3 it is the product's own: "Search YouTube" |
| The footer's left end, from x = 20: the counter, " · ", one footer cap (22 dp), words. `hint` type, second ink | The next key, what it does, and what today differs in |

| State | Coach line (`first_coach_*`) | German |
| --- | --- | --- |
| Step 2, an app's row first | 2 of 5 · `⏎` opens it · Booklight goes, and today comes back | `⏎` öffnet sie · Booklight geht und kommt heute zurück |
| Step 3, the app's row, Open armed | 3 of 5 · `tab` moves to Search | `tab` geht zu „Suchen“ |
| Step 3, Search armed | 3 of 5 · `⏎` goes into the app | `⏎` geht in die App |
| Step 3, a word under the chip | 3 of 5 · `⏎` searches there · Booklight goes, and today comes back | `⏎` sucht dort · Booklight geht und kommt heute zurück |
| Step 4, a sum's row first | 4 of 5 · `⏎` copies the answer · Booklight stays today only | `⏎` kopiert das Ergebnis · Booklight bleibt nur heute |
| Anything else is first | The counter and the lesson's title | |

The footer's caps never move; the line's words cross-fade (80 out, 120 in), as its hints do. A feedback word
("Copied") takes the seat for its time, as built. The tech lead prices the line at 0.75 day (`eng.md` §5).

**One lesson giving way to the next, in the same panel**

| After | What moves |
| --- | --- |
| "Go on" (K4) or Skip | The words roll, the recipe and the caption change where they stand, the counter's digit rolls. 252 stays |
| L4's Enter | The row above: "Copied", 520 ms, the field emptied, Q rises |
| A step aside | Nothing gives way: the new panel opens on the next lesson |

**Leaving and coming back.** Esc closes at any point, and the step waits. The user's keys open the panel on the
same lesson; it arrives as rows do, not written again.

## 7. Step 5 and the ending

**The question (Q), 324 dp. Alone. Nothing armed.** Unchanged.

```
  0 ╭────────────────────────────────────────────────────────────────────────────────────────────────╮
    │    G     Search apps, settings and the web                                             [esc]   │ field 68
 68 │                                                                                                │ pad 8
 76 │  (globe) Suggest searches as you type?                                                5 of 5   │ seat 1: one line, centre y 104
132 │                                                                                                │
140 │          Booklight sends what you type to Google while you type, to suggest searches. Google   │ the text: x 72 to 700,
    │          also sees your IP address. Not sent: sums, web addresses, and anything after a        │ room for 4 lines of 20
    │          keyword or with an app in the field.                                                  │
220 │                                                                                                │
240 │          Off until you agree. Change it in Booklight’s                         Agree           │ slots 240 to 272
    │          window › Results.                                            [tab]    Not now         │ and 276 to 308, neither lit
308 │                                                                                                │
316 │                                                                                                │ pad 8
324 ╰────────────────────────────────────────────────────────────────────────────────────────────────╯
```

- Mark: `globe`, as the switch has in the window. Words: `pm.md` §8, unchanged, as `first_suggest_title`,
  `first_suggest_text` (%1$s is the engine), `first_suggest_note`, `first_agree` (Agree · Zustimmen),
  `card_not_now`.
- The text: `label` at **strong** ink (a disclosure is not second ink), lines of 20 sp, x 72 to 700, top at 140.
  The note: `label`, second ink, x 72 to 514, two lines at most, its block centred on y = 274. The `tab` cap is
  centred on y = 274.
- Enter does nothing until Tab or the pointer has armed an answer. Tab arms Agree, then Not now. Esc, a lost focus
  and waiting are no answer; the question is there at the next opening.

**From Q to C.** An answer: Q's parts fade where they stand; the lower edge rises 56 dp, from 324 to 268, on
`place`; C's two rows rise 22 ms apart; the footer fades in on the lower edge.

**Your choices (C), 268 dp: a real list of two rows, nothing selected.** There is no row for the device's model
and no word about it; first run asks the system nothing about the model.

```
 68 │                                                                                                │ pad 8
 76 │   (list)  Show your usual                                                       Off  (●    )   │ tall row 92, centre y 122
    │           With nothing typed, the two things you run most stand under the field.               │
    │           Nothing changes until you have run two things twice each.                            │
168 │   (mark)  Everything Booklight does                                                      [?]   │ row 56, centre y 196
    │           Every keyword, with an example to try                                                │
224 │                                                                                                │ footer band 44, its line on y 246
    │  Play by name and flight times need your own key: window › Labs       [⏎] Done   [esc] Close   │
268 ╰────────────────────────────────────────────────────────────────────────────────────────────────╯
```

| Row | Words (English · German) | Right end, at x = 700 | Enter on it |
| --- | --- | --- | --- |
| Your usual, 92 dp, mark `list` | `set_usual_show` (there). `first_usual_text`: With nothing typed, the two things you run most stand under the field. Nothing changes until you have run two things twice each. · Ohne Eingabe stehen deine zwei meistgenutzten Dinge unter dem Feld. Nichts ändert sich, bis zwei Dinge je zweimal liefen. | The design system's switch (§3d: 44 × 24, an 18 dp thumb; the panel has none built yet), shown at rest too; 12 dp before it `first_off`, `first_on` (Off · Aus, On · An) in `label`, second ink | Flips it. The panel stays |
| Everything, 56 dp, mark `booklight` | `help_title`, `help_about` (there) | A `?` cap, 24 dp, strong (a row's key cap) | Opens the list of everything. So does typing `?` |

- Rows are the list's own: tops at 76 and 168, marks on x = 38, titles on 72, the pill x 8 to 712 once Down, Tab
  or the pointer brings it. Up from row one puts it away again.
- **Footer**, its line on y = 246. Left, from x = 20, `first_labs` in `hint` type, second ink: "Play by name and
  flight times need your own key: window › Labs" · „Musik nach Namen, Flugzeiten: eigener Schlüssel, im Fenster ›
  Labs“. Right: `⏎` Done (`card_done`) · `esc` Close. With a row selected, `⏎ Done` gives way to that row's own
  hint.
- A letter is typed and the typed list takes the rows' place. C is done when it is left, however it is left.

**The ending.** Enter with nothing selected: the rows fade where they stand, the height returns from 268 to 68
on `place`, the field's `esc` cap fades back. For this one opening the placeholder is `first_end_hint`: "Type ?
for everything Booklight does" · „Tippe ? für alles, was Booklight kann“. Then the reflection runs its lap round
the bare field. What is left on screen is the product, over the app the user opened.

## 8. The strong moments

| Moment | What it is, and where its parts stand at rest | Why it is this product's own |
| --- | --- | --- |
| **The welcome** | §3. Night falls on a tall glass. The field lights up from the caret and is the lamp's head; light falls from it and opens out of its own seam; the words are white where it falls; a small knife opens its five tools; the light folds back into the caret | The picture is Booklight's own mark, the icon the user has just clicked, 720 dp wide and switched on. No other app could open this way |
| **The show** | §3. Five of the user's own apps cascading, the pill down the list and along a strip, a sum rolled into its answer, a plane flying to its place on a flight's line, seventy emoji in a wave, one lap of light, and the highlight gliding into "Open Keyboard shortcuts" | Every part is the product at work, with the user's apps on the user's desk. Nothing was drawn for it. The first key makes it theirs |
| **The key caps** | §4. Caps as tall as a row, their labels in the field's type, flat, with the white ring. Typed parts beside them in the same type on the same baseline | A launcher is keys and typed words. The picture of each step is made of exactly those, in the sizes the panel already has |
| **The reveal** | The user clicks Set shortcut and presses the keys. The system's dialog falls away. The first frame of the panel that shows is K4 with "your key" **held down** (pressed, blank). As the dim lifts it comes up, the check draws in it, and the white reflection runs one lap round the 252 dp outline. "Go on" is armed. With animations off: K4 at rest, the check drawn | The panel does not arrive: it was there, and their own keys uncover it. The key on the glass is down because theirs is. The check is the one check Booklight has. No letters, because Booklight does not know them |
| **The step aside** | §6. Enter opens the app; the glass folds to its seam; the app is there; the seam comes again in the same place and the glass opens over the app, on the next lesson | It is the daily Enter and the daily opening, back to back, with the user's own app seen through the glass |
| **The fold** | §7: the list closes to the bare field, one line of placeholder, one lap of light | First run ends as the thing itself |

The reflection at the show's peak, at the reveal and at the fold needs a trigger of its own (0.5 day,
`eng-constraints.md` §4). It stays the single lap, never the steady light that means "working".

## 9. Progress and leaving

- **The overture has no counter.** It is not a step. The welcome's five tools are its only promise of what comes.
- **Progress** is the counter where a row's kind stands, from K1 on, and the same words at the start of the
  coach line. The total is the number of steps this run will show. A run of one step (an update from 3.0 without
  a key) shows no counter. Q is "5 of 5"; C, the last screen, shows none.
- **"Not now" is an answer**: that step is over and is not offered again (on step 1, first run is over). **Skip**
  is the same for a lesson. **Esc is only Esc**: the panel closes and the step waits; in the overture it lands
  K1. Nothing on screen explains Esc beyond the `esc` cap and, in the overture, the word "Skip".
- **A step come back to** looks as it did: the same screen arrives at the gate. Nothing says "again". After three
  openings without doing it, it counts as skipped; the question, as "Not now" (`pm.md` §6).
- **Nothing moves on by a clock.** K4 waits for Enter or a letter. The overture is the one thing that runs by
  itself, once, and any key ends it.

## 10. Every state

**The longest German line in each screen** (room: seat 1's line 72 to about 636, 564 dp, about 78 characters)

| Screen | Longest German text | Length | What gives if it does not fit |
| --- | --- | --- | --- |
| Welcome | The title „Willkommen bei Booklight“ at 40 sp | about 474 dp; the light is 492 wide there | The title is set at 36 sp, chosen once before the stage opens |
| Welcome | The line „Dein Schweizer Taschenmesser zum Tippen. Ohne Klinge.“ | 53 characters, about 461 dp; the light is 520 wide there | The copy is written to fit: 55 characters at most. Never two lines |
| Welcome | The cue „Kurz angeben“ and the Enter mark | about 130 dp in a handle of 216 | Nothing |
| The flight | Headline and badge: „Landung in 4 Std. 07 Min.“, „Pünktlich“ | end near x = 380; „Beispiel“ begins near 645 | Nothing |
| The flight | Footer: „Ein Beispiel. Live-Zeiten brauchen deinen eigenen AirLabs-Schlüssel.“ | 68 characters, about 442 dp; about 540 free beside „esc Überspringen“ | Nothing |
| K1 | Caption „Quick Insert liegt links neben A · oder Tasten deiner Wahl“ | 57 characters, about 370 dp; room to x = 487 | The caption's 24 dp fade. A longer name of the system's for the key: "· oder Tasten deiner Wahl" goes |
| K1 | The caps with a longer name for the key | Room to x = 487, a cap of about 260 dp | The cap never shrinks or wraps: the fallback name is used |
| K3 | „Zuerst „%1$s“ klicken. Aktion halten, dann die zweite Taste drücken.“ | about 72 characters with a word of eight letters | The line's 24 dp fade. A longer word of the system's: the second sentence goes |
| L2, L3 | Caption „Sonst schließt Enter auch Booklight. Heute kommt es zurück.“ | 59 characters, about 384 dp; room to x = 546 beside „Überspringen“ | The fade |
| L3 | The recipe with a long app name and word | at most 404 dp by rule | Letters are cut to four, the word to eight |
| Coach line | „4 von 5 · ⏎ kopiert das Ergebnis · Booklight bleibt nur heute“ | about 430 dp of 468 | The counter and its dot go first |
| Q | The text, about 245 characters | three lines of about 90 | A fourth line has room. Five lines (a long engine name): the stage is 344, chosen before it opens. Never cut |
| Q | The note, 68 characters | two lines in 442 dp | It has two |
| C | „Ohne Eingabe … je zweimal liefen.“, about 120 characters | two lines in about 510 dp | The tall row has two lines; the copy is written to fit |
| C | The Labs line, 64 characters, about 416 dp | 473 dp free | `⏎ Done` and its cap give way before the line is cut |
| Helper | Rows 3 and 5, about 65 and 69 characters | four lines each | The system wraps them (fact 2) |

| State | What happens |
| --- | --- |
| Light and dark | No new colour. Large cap: fill ink 0.10 / 0.14, ring white 0.55 / 0.30, pressed + 0.08. Marks' discs 0.08 / 0.12. Armed answer: `secondaryContainer` 0.78 / 0.42. The overture: §3. Second-ink words over a window of the other brightness are the known weak spot; the disclosure is strong ink for that reason |
| The device's model | Not looked at. No row, no word |
| A keyboard without Quick Insert | Action + J: §5 |
| A screen too low | Under 655 dp of height there is no welcome: the show begins after the opening, its pill fading in on row one. Under 570 dp the show's first beat has fewer rows (the list's own cap). Under 475 dp first run is not shown in the panel (the window's Start page has the key's row). No Googlebook is that low; a zoomed external screen may be |
| Pointer only | A click on the welcome's handle starts the show; a click elsewhere on the glass lands K1. Every answer, the switch and the rows take a click; hover arms. The key itself needs the keyboard, as the product does |
| Keyboard only | Tab and Enter reach every answer; Down and Enter every row of C. The helper is the system's |
| System animations off | No welcome and no show: K1 at once, with the welcome's title as the field's placeholder for that opening. Every screen is laid out in one frame and reads as a still: the caps are there, K4 has its check, C has its rows. After an app opens, the new panel stands at once. No reflection. Rolls and fades cut |

**TalkBack.** There is no overture with a screen reader on; the welcome's title is said once, before K1
(`first_hello_title`). Each screen is said once when it arrives, politely:
the counter, the title, the line, the recipe, the caption, then how to act (`a11y_first_*`). A cap is said by its
key's name; a chord with "and"; a sequence with commas.

| Screen | Recipe and caption as said | How to act (English · German) |
| --- | --- | --- |
| K1 | Suggested keys: Action and Quick Insert, the key left of A. Or any keys you like · Vorschlag: Aktion und Quick Insert, die Taste links neben A. Oder Tasten deiner Wahl | Enter opens Keyboard shortcuts. Tab for Not now. · Enter öffnet die Tastenkürzel. Tab für „Nicht jetzt“. |
| K2 · K3 | K3: Or try Action and J · Oder Aktion und J versuchen | Tab, then Enter opens Keyboard shortcuts. · Tab, dann Enter öffnet die Tastenkürzel. In K3 without Tab |
| K4 | none | Enter goes on. · Enter geht weiter. |
| L2 to L4 | For example: y o u t, Enter. Then the caption as written | Tab, then Enter skips. · Tab, dann Enter überspringt. |
| The panel is back after an app opened | | Booklight is back. Then the lesson · Booklight ist zurück. |
| Q | The whole text and the note | Nothing is chosen. Tab for Agree or Not now. · Nichts ist gewählt. Tab für „Zustimmen“ oder „Nicht jetzt“. |
| C | Each row with its state: "Show your usual, off" | Down arrow to choose. Enter when done. · Pfeil nach unten zum Auswählen. Enter zum Beenden. |
| The ending | | First steps are done. Type question mark for everything. · Die ersten Schritte sind fertig. Fragezeichen tippen für alles. |

The coach line is a polite live region and is said when it changes.

## 11. The replay, and the icon

- **"First steps" · „Erste Schritte“** (`first_name`; also found by tour, welcome, intro). In the panel it is a
  row of the kind "Booklight" with the `again` mark; Enter keeps the panel open, the list fades and the stage
  rises. No overture there: the user is at work in the panel. With a key that works it starts on K4 ("Go on"
  armed, "Change the key" second, which leads to K1). It changes no switch and asks Q only while suggestions are
  off.
- **On the window's Start page**: one row in the Tips group, after "Show the tips again", on the window's own
  lines (mark at 16, text at 52, its control ending 16 dp from the row's end). Title "First steps"; second line
  `first_row_text`: "The key, three things to try, your choices. About a minute." · „Die Taste, drei Dinge zum
  Ausprobieren, deine Auswahl. Etwa eine Minute.“; at its end the "opens" mark the key's row has. While first run
  is unfinished the title is `first_row_go_on`, "Go on with the first steps" · „Erste Schritte fortsetzen“, and
  the counter stands before the mark. **This row plays the whole of it, the overture first**, when first run is
  finished; when it is unfinished it goes on where it stopped, without.
- **The first click on the icon** shows the panel: the welcome, the show, then K1. Every later click shows the window
  (`pm.md`, decision 3).

## 12. What must line up

| Line | What stands on it |
| --- | --- |
| x = 20 | The left edge of every mark's box and of the chip; the footer's left end (the coach line, the Labs line, a cell's name) |
| x = 38 | The centres of the field's mark (Booklight's from the welcome's hand-over on, the engine's after the show), of the chip's symbol, of seat 1's disc, of every row's icon |
| x = 72 | The typed text and the caret, Booklight's typing included; seat 1's title and line; the left edge of the first large cap or the box of the first typed part; "your key"; the caption; Q's text and note; every row's title. (The placeholder's box is at 74, as built) |
| x = 700 | The field's `esc` cap; the counter; the answers' strip; every row's kind or strip; the flight's "Example", the end of its line and its far airport; C's switch and `?` cap; the footer's last word |
| x = 8 and 712 | The pill, in the overture and in C |
| y = 34 | The field's centre line, the seam's, and the lit head's |
| y = 76 | The top of seat 1, and of row one in the overture, in C and in every typed list |
| y = 104 | The centres of seat 1's disc, of its two lines taken together, of the counter |
| y = 160 and 216 | The top and the bottom of every large cap and of "your key" |
| y = 188 | The centres of the caps, the typed parts, the "+", the answers' block, the `tab` cap |
| y = 230 | The centre of the caption: the footer's line for a glass of 252 |
| One baseline | Cap labels and typed parts (both `field` type) |
| The welcome | Everything on the seam's axis, x = 360: the light (129 to 591 above, 32 to 688 at its foot), the title, the line, the middle tool, the handle (252 to 468). The head is the field's band, 0 to 68. The light's upper edge on y = 92, its foot on 448. The caret, and the place the light folds into, on x = 72 to 74 |
| The show | Row tops at 76, 132, 188, 244, 300. The answer's row 76 to 168, its mark and strip on y = 122. The flight's row 76 to 212: its mark, headline, badge and "Example" on y = 122; its line on y = 160 from x = 72 to 700; the plane's tail at 443; the two ends on one baseline, the far one ending at 700. The grid's cells from x = 14 at a pitch of 49.4, lines from y = 84. The square on the second line (y 134 to 178) is almost level with K1's first answer (154 to 186) |
| Q | Text top at 140; slots at 240 and 276; the note's block and the `tab` cap centred on 274; the strip ends at 308 |
| C | Row tops at 76 and 168; centres 122 and 196; the footer's line on 246 |
| Widths that never change | "Your key" is 112. The strip is as wide in K2 as in K1. A recipe and a cap are measured once |

## 13. Where I differ from the PM

| The PM | This design | Why |
| --- | --- | --- |
| The key lands and step 2 follows | K4 stands until Enter ("Go on") or a letter | The PM's own rule: no step moves on by a clock. It is the still for animations off and the replay's first screen, for one Enter |
| One state after the helper, with "Didn't work? Try other keys" | Two: K2 first, K3 from the second return | Booklight cannot tell not-yet from failed. The first return gets one calm line; the second gets the two traps |
| Any app in step 2 | Any app counts; the example is the app step 3 uses | Step 3 then adds one key to something just done |
| The last screen says one thing: type `?` | It is a list of two rows; `?` is its last row and the ending's placeholder; Labs is the footer's line | Switches are rows in this panel. The one thing is said where it is typed |
| Customize, the name, +, the keys, Set shortcut | Five rows; the fifth is "press your keys again"; "Done" is never taught | Fact 6: the system closes the helper |
| Updates without a key get step 1 | The same, without a counter and without the overture | "1 of 1" says nothing; the overture is for a first start |

Changed by Alex since the PM wrote, not by this seat: the key (Action + Quick Insert, not Action + J); Enter in
the lessons (the panel stays or comes back by itself; the key is pressed once, not four times); the model (no
row). The lessons' guidance no longer ends on "your keys for the next one": it says "today it comes back".
And for the welcome alone he has lifted "copy is plain" and the daily panel's look.

## 14. Decisions for Alex

**From the first round**

| # | Decision | Where it stands |
| --- | --- | --- |
| 1 | The stage | **Settled:** the panel's width, 252 and 324 dp |
| 2 | The size of the caps | Stands at 56 dp (it was not on his page) |
| 3 | The key on the glass after the helper | **Settled:** one blank key that takes the check |
| 4 | "Your key works" | **Settled:** waits for Enter or a letter |
| 5 | Progress | Stands: "1 of 5" where a row's kind stands |
| 6 | The helper's rows | Stands, and seen on a device: a number at the start of each label, the suggested keys at each row's end |
| 7 | A dim behind first run | **Settled** for the steps: none. Asked again for the overture alone: 12 |
| 8 | The white reflection as the reward | **Settled:** one lap at the reveal, one at the fold |
| 9 | The step's mark | Stands: the neutral disc of a row |
| 10 | The ending's placeholder | Stands: "Type ? for everything Booklight does" |
| | The key to suggest · Enter in the lessons · the device's model | **Changed by him:** Action + Quick Insert · the panel stays · no row |

**From the second round** (he said yes to the opening)

| # | Decision | Where it stands |
| --- | --- | --- |
| 11 | What the show shows | **Changed by him:** your apps, a sum, a flight, the emoji grid |
| 12 | The desk behind the overture | Stands: dimmed while the show plays; deeper, 0.50, for the welcome's night |
| 13 | The very first opening at the Slow speed | Asked again as 26: the welcome is the spectacle now |
| 14 | Booklight's mark | Stands: in the field's mark seat while Booklight types, from the welcome's hand-over on |
| 15 | Where Quick Insert is | Stands: the caption says "left of A" |
| 16 | After an app opens in a lesson | Stands: Booklight comes back by itself, with its ordinary opening |
| 17 | The overture again | Stands: the window's "First steps" row plays it, the welcome with it; the typed command does not |

**New in the third round**

| # | Decision | Recommended | If you say the other thing |
| --- | --- | --- | --- |
| 18 | The welcome's picture | **Lights on:** the panel is its own mark; the field is the lamp, the words stand in its light | The knife: blades of glass fan out from the field and fold into the first list; little light in it, and hard to see over a light window. Written in light: the lap of light writes the title; beautiful, and it could open any app |
| 19 | How loud the light is | A shaft: soft sides, 0.30 falling to 0.10, white words that brighten where it falls | With dust and a lap round the outline: more life, more to draw and to cut. Or solid, as the icon: the icon exactly, as bright as a white window, hard on the eye at night |
| 20 | The words | A: "Welcome to Booklight" · "Your Swiss Army knife launcher. Blades not included." · "Show off" | B: "Small panel, big ego. Ten seconds, tops." · "Watch this": funnier, says nothing of what it is. C: "Hi, I’m Booklight" · "Launcher by trade, Swiss Army knife at heart." · "Here goes": sweeter, without your title |
| 21 | The knife in the light | Five tools flick out of the cue's handle: the four beats and the key | No knife: the title, the line and the cue alone. Calmer, half a second shorter, and nothing of your Swiss Army knife is drawn |
| 22 | Where the flight comes | After the sum, before the grid | Last, before the key: the calmest landing, but the show ends on the one thing a new user cannot have yet |
| 23 | How "example" is said | "Example" where the row's kind stands, and one line where a flight names its source: "An example. Live times need your own AirLabs key." | "Example ·" at the start of line one: seen a moment sooner, and the row is then not word for word the real one. Or the footer's line alone: easy to miss in two seconds |
| 24 | What the flight shows | LH 455, San Francisco to Frankfurt, in the air, on time, the plane 61 % of the way | Landed ("Landed 12 min ago"): the plane at the end, no dots ahead, less of a journey. Delayed (the amber badge): both colours are seen, and the show has a late flight in it |
| 25 | Without animations | K1 at once; the title is the field's placeholder | A still of the lit stage that waits for Enter: the greeting is seen, and it is one more step before the key |
| 26 | The speed of the first opening | The speed that is set: the last ordinary thing before the night | Slow, as in the second round: 0.4 s more of the seam before a welcome that is already four seconds |

## 15. For the seats that follow

**For the motion designer.** Rest states are above. New in the third round, all in §3: the welcome and its
hand-over; the flight's beat. From the second round: the pace of Booklight's typing, the waits before `0` and
`%`, the moves of B1 and of the grid, when the lap sets off, the dim's lift; the landing; the step aside; L4
giving way to Q; Q to C with the lower edge rising 56 dp. As before: the caps arriving, K4 uncovered held down
and its release, the check, the laps, the rolls between K screens and after Skip, the fold. Nothing loops.

**Beyond the memo's sums, this design asks for:** the welcome's stage and the show's list in §3 ("New for the
two seats that follow"); the step aside (a start of its own within 0.4 s of an app's); a sum that copies and
stays (0.5 day, `eng-constraints.md` §7); the coach line; a trigger for the reflection; the switch of design
system §3d in the panel; a way to tell that the keyboard has the Quick Insert key; the stage's layouts.

**To look at on a device first**
1. The welcome over a white window and over a dark one, both themes: is the shaft seen as light? Are the words
   read in the time they stand? Is the night too dark for someone in light theme?
2. The welcome at 60 frames a second: the lower edge falling 400 dp with the veil changing; the shaft's pass.
3. The hand-over: do the shaft into the caret and the handle to row one read as one move up, or as two things?
4. The flight's beat: is "Example" seen, and its footer line read, in under two seconds?
5. The whole opening, filmed: one movement, or five slides? Eleven seconds: a pleasure or a wait?
6. Rows standing under a text they do not match, for the third of a second before the sum, the flight and
   the chip.
7. The highlight's travel from the cell into the answer: a glide, or a smear across the band?
8. The lap running while the height changes: does it stay on the outline?
9. The step aside, filmed, on both Googlebooks: what shows between the fold and the new seam? Asked for at
   once, and after 0.4 s.
10. Five rows in the helper beside Action + Quick Insert: how many lines in English and in German, all in view?
11. The key's own name in German, and whether the build can read it.
12. The frame that shows as the helper falls away: is it K4 held down, or does K2 show for a frame?
13. A 56 dp cap, and the 190 dp `Quick Insert` cap, on glass over a white window and a dark one, both themes.
14. "Your key", blank, 112 wide, in K2: read as a key waiting, or as an empty box?
15. The caption 22 dp above the lower edge: close to the edge, or at home there as a footer's words are?
16. Q in German with the longest engine name: three lines or four?
17. Every screen with animations off.
