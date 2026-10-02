# Booklight after 2.1: the design direction and the screens to draw

*Visual and motion designer, 1 October 2026. Planning and design only: nothing was built, no device was touched,
nothing in the repo was changed.*

*Read for this: `docs/design/design-system.md`; the twelve captures in `docs/design/captures/` (1080 px wide for
720 dp, so 1.5 px per dp); `tools/design-2.0/gen.py`, `page.css`, `page.js` and the page they make; `Motion.kt`;
and, for the real numbers, the code that draws the panel and the pin (`Metrics`, `Rows`, `Bodies`, `Strip`,
`Field`, `Panel`, `Glass`, `Footer`, `PinActivity`, `Pinned`). Every existing dp below is taken from that code, and every new one
is built on those. Every new time is a starting value until it has been seen on a device.*

*The UX designer's answers were not written when this was. Where a layout leans on one of them, it says so and
says what changes if the answer is the other one. Sources are named as in the frame: NF2, S&C, DF, LANG.*

---

## 0. In short

1. **One idea carries M1, M2 and M3.** Something you brought (a copy, a picture, a selection) is first a quiet
   line under the empty field, then the chip in the field, and the list under it is what can be done with it.
   One look, three milestones. 2.0 already shows text another app handed over this way.
2. **Four things are new to draw**, each built from parts the app has: the quiet line; a chip with a picture in
   it; a row with a picture; a picture in the pinned window. And one rule for words: an answer names its source.
3. **Nothing else is new.** No new colour, no new type size, no new row height, no new motion spec, no new text
   edge (the picture's row uses the QR row's edge).
4. **The quiet line is not a result.** No pill, no footer, the small caption type, a `tab` cap at its end. It sits
   exactly where row one would sit.
5. **What was found is said in words, not icons.** Icons in a line would be a strip, and a strip means "the
   selected row's actions".
6. **The picture is the one loud thing in this plan.** At most 176 × 104 dp, in its own shape, with a hairline
   ring, never see-through, never moving by itself. Two smaller fallbacks are named.
7. **M3 and M4 need no new element.** M3's design work is the opening when an answer is already on its way.
   M4's is a table of limits per component.
8. **Eleven screens to draw, four of them playing,** and one small timeline (§9).

---

## 1. The ground: the lines everything is drawn on

From `Metrics.kt`, `Rows.kt`, `Field.kt`, `Strip.kt`. x is measured from the panel's left edge.

```
x:  0   8     20     38     56    72                                              684    700  712 720
    |   |     |      ·      |     |                                                ·      |    |   |
    |   pill  icon   icon   icon  text edge                                 arrow column  margin pill
        left  column centre column                                          (Enter mark)         right
              left          right
```

| What | Value |
| --- | --- |
| Panel | 720 wide, radius 32 |
| Field | 68 high; mark or chip from x = 20; text from 72 |
| Row | 56 · tall 92 · an answer grown to four lines 136 (92 + 2 × 22) · QR 208 · a line of an opened list 40 |
| List | 8 above, 8 below; the footer is 36 |
| Icon | 36 dp, centred on x = 38 |
| Chip | 36 high, from x = 20: 8 · icon 20 · 6 · name 15/600 · 14. Its icon is centred on x = 38 too |
| Strip | slots at a 32 pitch, icons 18; the armed slot is 9 · icon · 7 · name · 6 · mark 14 · 9; flush right at 700 |
| Type | field 24/500 · title 17/500 · small 14/500 · hint 13/500 · chip 15/600 · answer 34/600 rounded |
| Ink | 1.0 · 0.80 · 0.60 of `onSurface` |

**For whoever draws the page.** Reuse the 2.0 page's helpers (`row`, `strip`, `fld`, `foot`, `panel`, the window,
the pin) at 1 px = 1 dp, but take these five things from the code, not from the 2.0 page's CSS:

1. The chip. The page pads it 10 · icon 18 · 8; the app is 8 · icon 20 · 6. On the page the chip's icon is 1 px
   right of the mark column. Small, and exactly the kind of thing that reads as "misaligned".
2. The strip. The page draws nine icons on an app's row. After today's polish there are fewer: three placements
   by default, and the app's own shortcuts.
3. The dark pill. The page uses a fixed `#33456F` at 78 %. Use whatever today's polish settles.
4. The light at the edge. The page draws it with the primary colour in it. It is white.
5. Small type is 14/500 and hints are 13/500, as the code and the 2.0 page have them. The design system's table
   still says 13 and 12.

---

## 2. One look for three milestones

| Stage | The copy (M1) | The picture (M2) | The selection (M3) |
| --- | --- | --- | --- |
| Nothing typed | The quiet line: "Copied 20 s ago · a link and a date" | The same line: "Copied 10 s ago · a picture" | None: you picked from the menu, so the panel opens one stage on |
| Tab, or handed over | The chip is the copy; the rows are what is in it, then what the model can do | The chip is the picture; row one is the picture | The chip is the prompt you picked; row one is its answer |
| An answer | The answer row, caption "From what you copied · on this device" | The picture's row, caption "From the picture · on this device" | The answer row, caption "From your selection · on this device" |

**The new elements, and what each is built from**

| New | Milestone | Built from | What is new in it |
| --- | --- | --- | --- |
| The quiet line | M1, reused in M2 | A row's skeleton (disc at 38, text from 72, a `tab` cap at 700, as a keyword's row has) in the tip card's role (under the empty field, nothing armed) | A row without a pill; its text in the small type |
| A link's extra actions | M1 | The strip; a text glyph in a slot, as a colour's HEX and RGB have | The glyph "MD" |
| An answer that names its source | M1 to M3 | The answer row's caption | Two parts: where from, then "on this device" |
| A chip with a picture | M2 | The scope chip | A 28 dp round picture where the icon is |
| A row with a picture | M2 | The grown answer row (136) with the QR row's two columns (picture from 24, text from 220) | The picture |
| A picture as a mark | M2 | The colour swatch (40 dp, radius 12, a ring) | A picture in it |
| A picture in the pin | M2 | The pinned window | Edge to edge, no text |
| A prompt answered at the opening | M3 | The opening's gate; the answer row | Nothing; an order of events |
| Limits per component | M4 | The two limits there are (12 and 90 characters) | Seven more |

**Rules that hold for all of it**

- One pill. The new line has none. A picture never gets a highlight of its own.
- No plate, no card, no divider. The only filled shapes stay the pill, the pane, the chip, key caps and discs.
  A picture is the one exception, as the QR plate and the swatch are: it is content, not a surface.
- The white outline belongs to the panel, the pill and the chip. A picture's edge is a 1 px ring of ink at 0.20,
  as the swatch has. A white edge on it would read as a second rim.
- Two kinds of answer look different, as they do today: what Booklight computed is large and rounded (a sum, a
  letter put back in one word); what the model wrote is title-size text, written in, under a caption that ends
  "on this device".

---

## 3. M1 · The copy

### 3.1 The quiet line

**Is it a row?** It takes a row's seat and a row's skeleton, and it is not a result. A result comes with the pill
and the footer, and row one is selected. A line nobody asked for must have none of that (D4). The tip card
already set the pattern: under the empty field, nothing armed, a `tab` cap that says how to get in. The line is
that, at a row's height, because it has one line to say.

```
x   0   8     20   38   56  72                                                   650  666   700 712 720
y 0 ╭───────────────────────────────────────────────────────────────────────────────────────────────╮
    │         G       Search apps, settings and the web                               [esc]          │ field 68
 68 │                                                                                                │ 8
 76 │      (clip)     Copied 20 s ago · a link, a date and a phone number             [tab]          │ the line 56
132 │                                                                                                │ 8
140 ╰───────────────────────────────────────────────────────────────────────────────────────────────╯
```

| Part | Layout | Look |
| --- | --- | --- |
| Seat | y = 76 to 132: row one's seat. The panel is 68 + 8 + 56 + 8 = **140 dp** (empty: 68; with a tip: 172) | No fill, no outline, no pill |
| Mark | 36 dp disc centred on (38, 104), 20 dp clipboard glyph | The disc of a row that is not selected: glyph at ink 0.80, disc at that ink's 0.08 (light) / 0.12 (dark) |
| Text | One line, one text layout, from x = 72 to at most 650. Small, 14/500 | "Copied 20 s ago" at ink 0.80, then " · ", then what it holds at ink 1.0. Too long: it ends in the 24 dp fade, never an ellipsis |
| Cap | `tab`, 22 dp high, right edge at x = 700 | The footer's cap. It stands under the field's `esc` cap: the two make one column on the margin |
| Footer | None | The panel ends 8 dp under the line |

- **One text, two inks.** Both parts are one text with a span, so they share a baseline by construction. This is
  the answer to "align the marks with the text baseline": there are no marks to align.
- **Why words and not icons.** A run of small icons at a line's end is the strip, and the strip means "what the
  selected row can do". The 2.0 visual review ruled out a strip on a row that is not selected (its M5). The
  things found get their icons one step later, each in the icon column of its own row.
- **The order of the words** is fixed and is the order of the rows after Tab: link, date, phone number, address,
  mail address, flight. Then "text" when nothing was found or the text is too long to be looked at (over 400
  characters: S&C §4.1), and "a picture" in M2. The wording is the UX designer's.
- **It cannot show the text.** The text has not been read (S&C §4.1; DF). So nothing is quoted, and no length.
- **The age is said once.** It is what it was when the panel opened, rounded ("just now", "20 s ago", "1 min
  ago"). It does not count. A number ticking on an empty panel is motion nobody asked for.
- **The mark is always the clipboard.** One mark, one meaning: this is your copy. The words say what kind.
- **Pointer.** A click is Tab. It answers with the pressed tint (ink 0.08, 80 ms in, 120 out) in the row's shape.
  Moving the pointer over it does nothing: there is no pill to bring.
- **Limit for M4:** 80 characters, the longest sentence in the longest language. "Copied 20 s ago · a link, a date
  and a phone number" is 51; the German is 61.

*Leans on the UX designer:* what Down does. My recommendation: Down is Tab. Then there is never a state "the line
with the pill on it" and nothing to design for it. If Down must select it instead: the pill fades in where the
line stands (it never flies in), the `tab` cap gives way to a strip of one, and the footer fades in.

### 3.2 After Tab: the chip and the list

**The chip is the copy**, as text from another app is today: the clipboard glyph, then the first 27 characters
of the text, 15/600, at most 220 dp wide (as built). Now the text has been read, so it may be shown. The field is
empty behind it; the placeholder says what typing does (UX).

**The rows** are ordinary 56 dp rows on the two lines.

```
x   8     20   38   56  72                                                        ~530            700 712
    ╭──────────────────────────────────────────────────────────────────────────────────────────────╮
 56 │     (link)      github.com/kuscher/booklight                          [Open ⏎]  Copy  MD      │  selected
    ╰──────────────────────────────────────────────────────────────────────────────────────────────╯
 56       (date)      Fri 3 Oct, 19:00                                                      Date
 56       (phone)     +49 30 81886262                                                      Phone
 56       (spark)     Translate into German
 56       (spark)     Fix spelling
 56       (spark)     Shorter
 56       (spark)     Explain
 56       (search)    Search Google for “Dinner with Anna on Friday…”                        Web
```

In the drawing a word in brackets is a 36 dp disc with that glyph, and "Copy" and "MD" in the strip are icon
slots at rest.

| Kind of row | Mark | Title (17/500) | Right end, not selected | Selected: the strip |
| --- | --- | --- | --- | --- |
| A thing found in it | Its own glyph on the disc: link, calendar, phone, map pin, envelope, plane | **The thing itself:** the link without its tracking tail, the day and time as Booklight's reader read them, the number, the flight | What it is, 14/500 at 0.80: Link, Date, Phone, Address, Mail, Flight | Link: "Open ⏎" · Copy · "MD" (about 170 dp). Date: "Add event ⏎" · Copy. Phone: "Call ⏎" · Copy. Address: "Maps ⏎" · Copy. Flight: "Track ⏎" · Copy |
| What the model can do | The spark, on every one of them | **The prompt's name:** Translate into German, Fix spelling, Shorter, Explain, Summarise | Nothing | "Ask ⏎" |
| The web row | The magnifier | As today | Web | As today |

**Two kinds without groups or headers.** Three things tell them apart, and all three exist:

1. Order: what is in it comes first.
2. The mark: each thing has its own glyph; the model's rows all carry the spark, which makes them a family
   without a heading.
3. The title and the right end: a thing's title is a value (it has digits, a host) and a word at the right says
   what it is; a model's row is a verb and its right end is bare.

No gap between the two kinds and no line. An 8 dp gap exists only before something that removes.

- **The thing as the title, not the verb.** The row shows what was found, so a wrong date is seen before it is
  used; the verb is where verbs always are, in the pane. One line, every title on one baseline. If the UX
  designer wants "Open the link" as the title, the value becomes the second line (14/500, 0.80) of the same
  56 dp row; nothing else changes.
- **Copy clean and Copy as Markdown are slots of the link's strip,** not rows. If the link had a tracking tail,
  the second slot is named "Copy clean" when armed; otherwise "Copy". The third is the text glyph "MD", named
  "Copy as Markdown" when armed (German about 150 dp; the strip stays under 340).
- **A title is settled before its row is drawn.** "Translate into German" needs the text's language. If that is
  not known in the frame the list is built, the title is "Translate" and stays so for this opening; the answer's
  caption then says the direction. A title never changes under the pill.
- **The budget.** The list is at most eight rows, 448 dp; the panel is then 568 dp, 1.1's tallest. Three things,
  four prompts and the web row are eight. Today's rows of the clipboard list (Note, Mail, QR code, UPPERCASE and
  the rest) are found by typing their name. What gives way first is the UX designer's call; the limit is not.

### 3.3 The answer, and `do`

The answer row is 2.0's, unchanged in measure: 92 dp, grown once to 136; caption 14/500 at 0.80; text 17/500 on
22 dp lines, from x = 72 to 16 dp before the strip (446 dp beside "Copy ⏎ · Pin · Gemini").

- **The caption names the source.** Two parts joined by " · ": where the material came from, then "on this
  device". "From what you copied · on this device". When both do not fit, the second part goes: it is the same
  on every row. The caption rolls when it changes, as it does today.
- **A prompt run from the list** makes the prompt the chip ("Translate"), leaves the field empty, and its row
  shows the start of the copy until the answer is written over it. Today this path puts the whole copy into the
  field as if typed; with a long copy that is a field full of text nobody typed.
- **`do`**: the chip is the prompt's name, the field holds the instruction (it is what was typed), the row holds
  the material and then the answer. Nothing copied: today's row with nothing to run ("Type the text, or copy it
  first"). Too long: the caption says so ("the first 3,000 words of what you copied · on this device").
- **The model will not answer** (busy, the quota, a policy refusal: S&C §1.4): today's state, for every row old
  and new. The caption rolls to the plain sentence, the material stays, the strip becomes the way to the Gemini
  app. No colour, no glyph.

### 3.4 Motion

Row numbers without a letter are the existing tables' (1 to 20 in the design system, 21 to 55 in the 2.0 motion
review).

| # | Transition | What moves | Spec · delay | What stays still |
| --- | --- | --- | --- | --- |
| M1-a | The line arrives | The opening is row 20 at 68 dp, as always. Then the height goes from 68 to 140; the disc, the text and the cap rise 12 dp and fade in 140 | `place`; the parts 22 ms apart · 320 ms after the glass is 85 % open and only if nothing was typed: the tip's hold | The field, the seam, the opening itself. Tab does not wait for it (M1-d) |
| M1-b | What it holds becomes known late (the system may still be looking two seconds after a copy: DF) | The second part of the text cross-fades in place: " · text" to " · a link and a date" | `fade` 110 in, 70 out · once at most | The first part, the cap, the height. Nothing is pushed: the room to its right is empty |
| M1-c | A letter is typed | The letter, in the same frame. The line fades where it stands; rows cascade; the height goes on to the list's | `fade` 60; rows `place`, 22 ms apart; height `place` | The field. It is row 36, the tip's leaving. Row one's disc lands where the line's disc stood |
| M1-d | Tab, or a click | Text and cap fade where they stand. The chip arrives as it does for any scope (grows where the G was). Rows cascade. The pill appears on row one, in place. The footer fades in | `fade` 60; chip `fade` 110 and `place`, the G `pop`; rows `place`, 22 ms apart; pill `fade` 120; footer `fade` 140 | The line's seat becomes row one's: the disc of row one arrives where the clipboard's disc was. Field text and chip change in one frame |
| M1-e | A prompt is run from the list | Chip to chip: the name rolls, the glyph swaps on one centre, the chip's right edge goes to its new width. The other rows fade; the answer row comes under a pill that does not move. The edge light runs its slow lap until the first word; the answer is written in; the row grows once | `roll`, `pop`, `place`; rows `fade` 80; the answer as built | The pill, the chip's left edge. Rows 40 and the answer row of 2.0: nothing new |
| M1-f | Backspace on the empty field | M1-e and M1-d backwards, one step each press | as rows 7 and 40 | The baseline, the panel's top |

- **M1-a.** *Interrupted:* a letter before it has landed is M1-c from wherever it is; its alpha runs back, it
  never restarts. A letter before the hold is over: no line at all. *Animations off:* the hold stays, then the
  line is there in one frame. *If built carelessly:* the line is part of the height from the first frame and the
  opening becomes a 140 dp slab (the 2.0 motion review's M1, again).
- **M1-b.** *Careless:* the whole line is replaced and rises again; or the second part arrives while a key is
  down. It cuts if the line is already leaving.
- **M1-d.** *Interrupted:* typing during it is the argument. Tab before the line has arrived opens the copy all
  the same: chip and rows come once the glass is open, as rows for handed text do today. *Careless:* the pill
  flies down from the field or up from where it last was. It must appear where it belongs.
- **Optional, and the first thing to cut:** on Tab the clipboard glyph rides from the line's disc up into the
  chip. Both are centred on x = 38, so it is 70 dp of travel straight up, on `place`. It is the design system's
  fourth principle (things open from where they are). 2.0 planned the same for a keyword and did not build it.

**What must never pop in M1:** the line (it rises); its late words (they cross-fade); the pill on Tab (in place,
faded); the footer; a row's title (never changed after it is drawn).

### 3.5 The three guests on the empty panel

The copy's line, a tip, and the white reflection (today's polish). With the times in the code this afternoon, at
medium speed (the polish may still change them):

```
key ──── glass open ──── the line ─────────────── still ─────────────── the reflection ────────────
0 s      0.3 to 0.46 s   0.6 to 0.9 s                                   from about 2.7 s, for 1.5 s
```

- **Never two under the field.** The order of right: a first-run card, then the copy's line, then a tip, then
  nothing (D3).
- **Once put away by typing, the line stays away** for this opening, even if the field is emptied again. A line
  that comes back each time the field empties would flicker while editing. Tab on the empty field still opens
  the copy. (UX to confirm.)
- **The reflection keeps away by time and by one rule.** It waits 2.4 s after the opening and needs 0.7 s in
  which nothing changed. The line has landed about 1.8 s before. The rule to add: "nothing changed" must also
  count what is under the field (the line arriving, its late words), a row changing height, and an answer still
  being written. Today it watches only the typed text and the number of rows, and it stands back only until
  the model's first word (`Panel.kt`, `OverlayModel.kt`), so it could start its lap while an answer is still
  arriving. This adds to today's polish; it does not change it.
- **Tab while the reflection runs:** it finishes its lap, as it does when you type. It is drawn from the glass's
  own size each frame, so it rides the edge as the panel grows. To be looked at on the device.

### 3.6 Screens to draw

| Screen | States | Light and dark |
| --- | --- | --- |
| **1. "Open, and the copy is there."** The empty panel with the line | "a link, a date and a phone number"; "text". **Plays:** the opening at medium speed, the line arriving after it, a letter putting it away | Both. Dark is the real test: the line has no pill, so it is second ink on dark glass over a white page |
| **2. "Tab."** The chip and the list, the DF probe text as the copy | The pill on the link (strip "Open ⏎ · Copy · MD"); the pill on "Translate into German" ("Ask ⏎"). **Plays:** Tab from screen 1 | Both: the first list designed on today's softer dark highlight |
| **3. "The answer names its source."** | Translate, answered, four lines; `do make this a bullet list`; the plain "no answer" state, small | Light; one dark |

Beside screen 1, the timeline of §3.5 as a small drawing.

### 3.7 Risks to the look, and what to cut

1. **The empty panel is the calmest state Booklight has, and this changes it on many openings.** That is why the
   line is small type, has no pill and can be switched off. If it still feels like noise on the device: show it
   only for a copy that holds something (a link, a date, a number), not for plain text.
2. **Icons creeping into the line.** Resist: words only.
3. **The list after Tab is the fullest list in the app.** Eight rows at once is the limit. Cut the transforms
   from the resting list before cutting a prompt.
4. **"MD" among drawn icons** may look like a different voice. HEX and RGB already do this on a colour's row. If
   it jars: one drawn mark for Markdown, 18 dp, in the icon set.
5. **Two inks in one line** could read as a link. Check on the device; the fallback is one ink at 0.80.
6. **Cut in this order:** the glyph's ride into the chip; the late words' cross-fade (the line then says "text"
   and Tab shows the rest); the stagger of the line's three parts.

---

## 4. M2 · The picture

### 4.1 Before Tab: the same line

"Copied 10 s ago · a picture", with the clipboard's mark. No thumbnail: showing one would mean reading the clip,
and the system would say "pasted" before the user asked for anything (S&C §4.1). So M2 adds nothing to the empty
panel. Whether the Googlebook's capture puts a picture on the clipboard at all is **not verified** (NF2 §2.2;
D5). A picture that was shared to Booklight, or picked, skips this stage: the panel opens with the chip.

### 4.2 The chip with a picture

```
        20  24      52  58                 ← x
        ╭──────────────────────────╮
   36   │  ( ● )   Picture         │       ● = the picture, 28 dp, round, centred on x = 38
        ╰──────────────────────────╯
```

The scope chip, 36 dp high, from x = 20: 4 · picture 28 · 6 · "Picture" 15/600 · 14. The picture is cropped to a
circle (radius 14: the chip's 18 less the 4 it is inset by) with a 1 px ring of ink at 0.20. Its centre is on
x = 38, the mark column, where every chip's icon is. It is a token, not the picture: the picture is in the row.
In a narrow panel only the picture remains, as only the icon does today. The placeholder behind it says what to
type (UX: "Ask about this picture").

### 4.3 The row with the picture

Row one, 136 dp from the first frame: the answer row's grown height, so it never has to grow.

```
x   8      24                   200  220                                  518    534          700 712
y 0 ╭───────────────────────────────────────────────────────────────────────────────────────────────╮
 12 │                                From the picture · on this device                               │
 16 │      ┌──────────────────┐                                                                      │
 30 │      │                  │      Lisbon has the lowest price,              [Copy ⏎]  Pin  Gemini │
    │      │    the picture   │      at €98.                                                         │
    │      │ 176 × 104 at most│                                                                      │
120 │      └──────────────────┘                                                                      │
136 ╰───────────────────────────────────────────────────────────────────────────────────────────────╯
```

| Part | Layout |
| --- | --- |
| Picture | From x = 24, y = 16: sixteen inside the pill on the left and on top, where the QR plate stands. It is fitted into 176 × 104 in its own shape: never cropped, never padded out, never drawn larger than it is |
| Text | From x = 220 (the QR row's text edge) to 16 dp before the strip. Caption at y = 12, then four 22 dp lines. Beside "Copy ⏎ · Pin · Gemini" the column is 298 dp, about 140 characters in four lines |
| Strip | Top-aligned on the row's first line (y = 30 to 62), as in every answer row |
| No mark | The picture is the row's mark, as the plate is in the QR row and the swatch in a colour's row |

| The picture's shape | Example | Drawn at |
| --- | --- | --- |
| 3 : 1 | a strip of a window | 176 × 59 |
| 2 : 1 | DF's table, 1200 × 600 | 176 × 88 |
| 16 : 10 | a window, a whole screen | 166 × 104 |
| 1 : 1 | | 104 × 104 |
| 9 : 16 | a phone-shaped window | 58 × 104 |

The picture always hangs from x = 24, y = 16 and the text always starts at 220, whatever the shape. Its top edge
is level with the top of the caption's letters. A narrow picture leaves air before the text; the edges stay.

**What keeps it from looking pasted on**

1. Its own shape. There is no box around it and no plate behind it, so nothing reads as a frame.
2. Radius 12, the QR plate's.
3. A 1 px ring of ink at 0.20 inside its edge, the swatch's ring. It gives a white picture an edge in light
   theme and a dark one an edge in dark. Not white: white outlines are the glass's.
4. No shadow, no tilt, no gloss.
5. It is opaque. A see-through screenshot is mud. App icons, the swatch and the QR plate are opaque too.
6. It is small against the glass: at most 7 % of the panel's area in this state. The QR plate is larger.
7. It arrives with its row and never moves by itself afterwards.

**What the row says, by state** (words are the UX designer's; the slots are fixed)

| State | Caption | Body | Strip |
| --- | --- | --- | --- |
| Nothing typed | Where it came from and its size: "Copied 10 s ago · 1200 × 600" | One line at 0.80 that says what to do | None |
| A question typed | The same | The same | "Ask ⏎" |
| Answered | "From the picture · on this device" | The answer, written in, ink 1.0 | "Copy ⏎" · Pin · Gemini |
| Too large to read as a picture, answered through its text | "From the picture's text · on this device" | The answer | The same |
| Cannot be read | One plain sentence | Nothing | None |

"This may be wrong" is said by naming the source every time, and by the picture standing beside the answer. No
warning glyph, no colour.

**The two rows under it**, ordinary 56 dp rows:

| Row | Mark | Title | Second line (14/500, 0.80) | Strip |
| --- | --- | --- | --- | --- |
| Copy its text | The text glyph on the disc | "Copy its text" | Reserved from the first frame. The start of the text fades in when the recogniser has read it; "No text found" if there is none, and the row then has nothing to run | "Copy ⏎" |
| Pin it | The pin glyph on the disc | "Keep it on top" | None | "Pin ⏎" |

The list is 136 + 56 + 56 = 248 dp; the panel 368 dp. Typing a question changes neither row: nothing in the list
moves while you type.

### 4.4 A picture as a mark, and in the pin

- **As a mark** (the row `pin` shows for a pinned picture, and anywhere a 56 dp row stands for a picture): the
  swatch's shape, 40 dp, radius 12, ring of ink 0.20, centred on x = 38. Cropped to the square: here it is a
  mark.
- **In the pinned window:** the picture fills the window edge to edge. No margin, no caption, no ring: the
  window's own corners and the system's shadow are its frame, and a margin would make a frame in a frame. The
  window takes the picture's shape, up to 2.39 : 1 (the system takes no flatter one: `Pinned.kt`). How tall a
  shape it takes is not in the research: **not verified**. A shape outside the limits is centred on the
  window's ground. It is drawn from the picture's own pixels at the window's size, so it gets sharper when the
  user makes the window larger.
- **Its size.** The system opens the pin at the smallest size the manifest names: 280 × 118 for the flattest
  shape, 280 × 140 at 2 : 1, 219 × 219 for a square (DF, "The pinned window"). A 16 : 10 picture would open at
  about 280 × 175: **not verified**. That is small for a table one types from. Ask to the engineer: can a
  picture's pin open at about 420 dp on its long side? It would need a window entry of its own.

### 4.5 Motion

| # | Transition | What moves | Spec · delay | What stays still |
| --- | --- | --- | --- | --- |
| M2-a | The picture's row arrives (Tab on the line, a share, a pick) | As M1-d. The row rises 12 dp and fades in with the picture in it, as one piece | `place`, 22 ms apart | The picture has no motion of its own. If its pixels are not ready, its place (known from the file's size) is the disc's flat fill, and the picture fades in |
| M2-b | The picture's pixels arrive late | The picture fades in over the flat fill | `fade` 110 | Its size and place |
| M2-c | The chip arrives | As any chip. The G gives way to the round picture on one centre | `place`, `pop` | x = 38 |
| M2-d | A question is typed | The text, in the same frame. "Ask ⏎" comes as a strip does (12 dp from the right) | `place`, `fade` 110 · once, on the first letter | The picture, the row, the two rows under it |
| M2-e | Asked | The edge light runs its slow lap until the first word (about 0.7 s with a picture: DF). The caption rolls; the answer is written in | as built; `roll` | The picture. The row's height: it was 136 from the start |
| M2-f | The text is read | The second line of "Copy its text" fades in | `fade` 110 | The row's height; its title |
| M2-g | Copy its text | "Copied" and its check in the footer. Then, if the UX designer wants the text's own list to follow: chip to chip (the round picture gives way to the text glyph on one centre, the name rolls), the picture's rows fade, the text's rows cascade | row 55; row 40; rows `fade` 80, `place` | The pill stays on row one |
| M2-h | Pin it | The system's arrival; the panel closes at once. In the pin the picture fades in once | row 50; `fade` 140 | A new picture over an old pin cross-fades in place and does not rise: a rise would show the ground under an edge-to-edge picture |

**What must never pop in M2:** the picture (it comes with its row, or fades in over its own place); the text read
from it; the caption.

**What the picture never does:** shimmer, pulse or dim while the model works. The edge light is the one sign of
work, as today.

### 4.6 Screens to draw

| Screen | States | Light and dark |
| --- | --- | --- |
| **4. "Ask about what you framed."** The chip with the picture, a question, the picture's row, the two rows under it. DF's table as the picture | Before the answer; answered. **Plays:** the answer being written | Both, and in dark twice: the pill on the picture's row, and the pill moved to "Copy its text" so that a bright picture stands on bare dark glass. This is the state most likely to be too loud |
| **5. "Every shape hangs from the same corner."** The picture's row five times: 3 : 1, 2 : 1, 16 : 10, 1 : 1, 9 : 16 | Each answered. One of them a whole screen, with "From the picture's text"; the "cannot be read" state | Light. This screen is the proof of alignment |
| **6. "Kept on top."** A drawn desk with the picture pinned at 2 : 1 and at 16 : 10, 2.0's pins beside it for scale; under it the row `pin` shows, with the 40 dp mark | One pin of a shape the system will not take, with its bands | Light; the banded one in dark |

### 4.7 Risks to the look, and what to cut

1. **A bright, opaque picture on see-through glass.** The largest risk to the look in the whole plan. If 176 × 104
   is too loud on the device, the fallbacks are, in order: (a) the picture at most 60 dp high in a 92 dp row
   that grows as answer rows do; (b) the 40 dp mark in today's answer row with the text back at 72, and the
   picture seen large only in the pin.
2. **140 characters for an answer about a picture.** The model is asked for one or two sentences in plain text
   (DF says the prompt needs that). The fourth line ends in an ellipsis; Copy and Pin carry all of it. To judge
   with real answers.
3. **A strip that tries to hold everything** (Ask, Copy its text, Pin, Save, Read its QR code). Three rows with
   one job each instead. Save and the QR code stay out.
4. **A shimmer on the picture while the model thinks.** No.
5. **The pin of a picture is the system's window:** its controls show on hover, its smallest size is small, its
   shape is limited. None of that is ours to draw.
6. **Cut in this order:** the round picture in the chip (the picture glyph instead); the text's start on "Copy its
   text"; the large picture (fallbacks above).

---

## 5. M3 · The selection

### 5.1 What is new to draw

Nothing in the panel is a new element. The menu is the system's.

| Thing | Built from | Layout |
| --- | --- | --- |
| The entries in the selection menu | The system draws them. Ours are only the names | Short names, two words at most: the system cuts long ones. How many entries of one app it shows before it folds them away is **not verified** (NF2 §3, idea 5; D8) |
| The panel, opened from the menu | The chip is the prompt (spark and its name). Row one is the answer row | 92 dp, grown once to 136. Caption "From your selection · on this device". Strip, where the field takes text back: "Replace ⏎" · Copy · Pin · Gemini, 215 dp, text column 72 to 469. Where it does not: "Copy ⏎" first and no Replace. The same row either way |
| Put the letters back, a sentence | The `abc` chip, then the answer row instead of the letter grid | 92 or 136 dp. The answer is not shown while it arrives: it is checked first, then written in. Refused: the caption rolls to one plain sentence and the body keeps what was typed |
| Put the letters back, one word (`abc Koeln`) | A sum's row: caption, then the answer large and rounded, 34/600 | 92 dp, "Copy ⏎". Booklight computed it, so it looks computed |
| `tr danish …` | One chip whose name becomes the direction | The chip reads "Translate"; when the language's word and its space have landed it rolls to "Into Danish" and the word leaves the field, as a keyword does when it becomes a chip. Never two chips. (UX to confirm; the other way is to leave the word in the text, and nothing moves) |
| `formal`, `friendly`, `error` | Prompt rows as they are | `error`: under its answer row stands the web row, "Search Google for “the error's first line”". "Beside it" means the next row |
| Which prompts stand in the menu | A switch per prompt in the window | One row with the switch the window already has. Its place is the window redesign's |

No marks for the letters that changed. Underlining them is tempting, and it is a new element that M3 does not
need: the promise is kept by the check, not by a drawing.

### 5.2 Motion

| # | Transition | What moves | Spec · delay | What stays still |
| --- | --- | --- | --- | --- |
| M3-a | The panel opens from the menu | Row 20 at 68 dp, with the chip in the field from the first frame, uncovered by the glass as the placeholder is. When the glass is 85 % open the height goes to the answer row's and the row rises 12 dp | `open`; then `place` | The opening is the same opening. The row waits for the glass even when the answer is already there |
| M3-b | The answer | The edge light's slow lap starts when the glass is open, not before: while it opens, the outline is still a seam. If words are already there when the row lands, they are written in from the start | as built | The row shows the selection first, then the answer over it, however fast the model was |
| M3-c | Replace | "Replaced" and its check; the panel folds after its usual hold | row 55, row 20 | |
| M3-d | The letters cannot be put back | The caption rolls | `roll` | The body, the row's height |
| M3-e | `abc`: from the letter grid to an answer row and back, as the text becomes a sentence | Row 13 backwards: the cells fade together, the height springs once, the pill's four edges and its radius go from the cell's square back to the row | `fade` 80; `place` | The field, the chip |
| M3-f | `tr` and a language | The chip's name rolls, its right edge goes to the new width, the text slides with it | `roll`, `place` | The chip's left edge and height (row 40) |

**What must never pop in M3:** the answer row at the opening (it comes after the glass, always); a wrong answer
for the letters (it is never shown, not even for a frame); the chip's width (measured for both names first).

### 5.3 Screens to draw

| Screen | States | Light and dark |
| --- | --- | --- |
| **7. "Right-click, Fix spelling."** A drawn mail window with a sentence selected and a plain menu with two of Booklight's entries; then the panel over it | The menu (drawn plainly, marked "the system draws this"); the panel with the answer and "Replace ⏎". **Plays:** the opening, the row after it, the answer written in | Light; the panel also in dark, since this row on the softer pill is what M3's user sees most |
| **8. "Put the letters back."** | The sentence, answered; one word, answered large; the refused state | Light |
| **9. "Into any language", and `error`.** | `tr danish see you on Saturday` with the chip "Into Danish"; `error` with its answer and the web row under it | Light |

### 5.4 Risks to the look, and what to cut

1. **The menu is not ours.** Its order, its icons, how it cuts names and when it folds are the system's. The page
   must not promise a look for it.
2. **The opening with an answer already there.** The careless build is the tall opening that the gate was made to
   prevent. One rule: under the field, nothing before the glass.
3. **Long prompt names in the chip.** The chip holds 220 dp. A limit for prompt names: 24 characters.
4. **Marks for changed letters.** Cut before it is drawn.
5. **Cut in this order:** the chip that becomes "Into Danish" (the language stays in the text); the large answer
   for one word (an ordinary answer row).

---

## 6. M4 · Your language

No new element and no new motion. The design work is to find what breaks when text is 20 to 30 % longer, and to
give every part a limit that a test can check. Two exist today: place names 12, tips 90 (LANG §3.1).

**What breaks, and the rule for each.** Type sizes never change with the language, and no row gains a line.

| Component | Why it breaks | Rule |
| --- | --- | --- |
| A name in the armed slot of a strip (a place, an action) | German is at the limit of 12 for places already (LANG §3.1). French will not fit | 12 characters for places, 20 for other actions. With today's polish only three placements stand in the strip; the rest are lines of the opened list, which have 600 dp |
| The tip card | Two lines of 13/500 beside two answers that grow with the language (German: about 170 dp) | 90 characters, as now. The answers: 18 characters each |
| A chip's name | 220 dp | 24 characters |
| The quiet line | One line, 578 dp | 80 characters for the longest sentence; things are dropped from the end, never cut mid-word |
| An answer's caption | Two parts in as little as 298 dp beside a picture | The second part goes when both do not fit. First part: 32 characters |
| The footer | Two hints and `esc`; the first can be a scope's title | 28 characters for a hint. The caps keep their places |
| The preview's slot labels (TO, SUBJECT, WHEN) | Small capitals in fixed columns | 10 characters. Check that the marks on Å, É, Ö are not cut in the 12/600 line |
| Key names on caps | "Umschalt", "Mayús" | They never shrink or wrap; a row of four keeps the 2.0 rule (the strip gives way first) |
| The window: a choice of four words on one row | Collides with its title in a narrow window | The window redesign's. It must be final before M4 (frame §2) |
| The store text | 3,995 of 4,000 in German (LANG §3.3) | Written to fit, not translated (the frame says so) |

Not at risk for these six: the face. Google Sans Flex has every letter they need (LANG §4), so no row changes
height. That is not true of Japanese, Korean and Chinese, which are not in M4.

**Motion in M4:** none new. Rolls and cross-fades already measure both texts first. "Booklight types" takes
about half a second whatever the length.

### Screens to draw

| Screen | States | Light and dark |
| --- | --- | --- |
| **10. "The same panel, in your language."** One state three times: the event preview, typed the way each language writes it ("ven 15h dentiste", "fre kl. 15.30 tandlæge") | French, Danish, Spanish | Light |
| **11. "Where long words go."** Each component of the table at its limit, English above, the longest language under it: the armed slot, the tip, the chip, the quiet line, the caption, the footer | One sheet | Light |

For the native readers the frame plans a picture book of about forty screens (D15). Make it from captures of the
real app (`./bl shot`), not from drawings: a drawing would hide exactly the clipping the reader is there to find.

### Risks to the look

1. **The pull to shrink type or wrap a row** when one word does not fit. The rule is the other way round: the
   word is chosen to fit, and the test fails the build when it does not.
2. **A prompt in a language the model does not write well** hands over to the Gemini app. That row exists. It
   must look like a normal row, not like an error.

---

## 7. The two tracks, from the design side

- **Today's polish.** Everything above is drawn on it: three placements, an app's own shortcuts as actions, the
  softer dark highlight, the later white reflection, medium speed. None of the new elements uses the pill's
  colour: the line, the chip and the picture are ink, disc and ring. So a change to the dark pill changes no
  layout here; it changes how screens 2, 4 and 7 look in dark, and those are the three to look at again when it
  is settled.
- **The window redesign.** M1 needs one switch there, M3 one switch per prompt. Both are "a title, one line, a
  switch", the row every version of the window has. Nothing here depends on how the window looks.

---

## 8. Motion across the plan

- **New specs needed: none.** Every row above rides `place`, `lead`, `trail`, `pop`, `arm`, `fade`, `roll` and
  the existing stagger of 22 ms.
- **One hold to name.** The 320 ms that what is under the field waits after the glass is open. It is a private
  number in `Panel.kt` for the tip today. The line uses the same one, so it should have one name. To check on
  the device: whether 320 ms feels late for something you came for. Tab does not wait for it, so lateness costs
  only the eye. The fallback is 200.
- **One rule to add** to the reflection: what counts as quiet (§3.5).
- **Would need new machinery, so not in this plan:** a picture that grows out of its thumbnail into the row or the
  pin; marks that fly from the line into the rows; any light on a picture.
- **The moments worth the care** (as the unfold, "Booklight types" and the drawn check were in 2.0):
  1. *The copy is there.* The glass opens as always, and one beat later a line rises under the field. It is the
     motion that will be seen most.
  2. *The picture lands.* The chip takes the picture where the G was, and its row rises with the picture in it.
  3. *The answer is already being written* when the panel has opened from the selection menu.
- **Cut first, if the device shows it too busy:** the glyph's ride into the chip; the stagger of the line's
  parts; the chip that becomes "Into Danish"; the round picture in the chip. The line's rise, the picture's fade
  and the gate at the opening are never cut.

**To check frame by frame on a device before values are locked**

1. The opening at medium speed with a fresh copy: the seam and the glass are as without one; the line's first
   frame is after the hold; no blurred band under the field.
2. A letter at 0.3, 0.6 and 0.8 s after the key: the line never shows, or runs back from where it is; the height
   changes direction once at most.
3. Tab at 0.2 s (before the line) and at 1 s (after it): the pill appears on row one in place in both.
4. The reflection with the line showing, and Tab pressed in the middle of its lap.
5. A white picture and a terminal's picture in the row, both themes, with the pill on the row and off it.
6. The opening from the selection menu with the model warm: no frame of the answer row before the glass is open.
7. All of it with animations off: everything cuts, the holds stay.

---

## 9. All screens

| # | Milestone | Screen | Plays | Dark |
| --- | --- | --- | --- | --- |
| 1 | M1 | Open, and the copy is there | Yes | Yes |
| 2 | M1 | Tab: what is in it, what the model can do | Yes | Yes |
| 3 | M1 | The answer names its source; `do` | No | One |
| 4 | M2 | Ask about what you framed | Yes | Yes, twice |
| 5 | M2 | Every shape hangs from the same corner | No | No |
| 6 | M2 | Kept on top | No | One |
| 7 | M3 | Right-click, Fix spelling | Yes | The panel |
| 8 | M3 | Put the letters back | No | No |
| 9 | M3 | Into any language, and `error` | No | No |
| 10 | M4 | The same panel, in your language | No | No |
| 11 | M4 | Where long words go | No | No |

Four play: 1, 2, 4 and 7. Beside screen 1 stands the timeline of §3.5, which plays too.

---

## 10. The frame's seven questions, answered

1. **A quiet row.** A row's seat and skeleton, the tip card's role: no pill, no footer, small type, a `tab` cap
   (§3.1).
2. **The marks for what was found.** Words in the line, in the order of the rows; icons only on the rows after
   Tab (§3.1, §3.2).
3. **A picture on glass.** 176 × 104 at most in a 136 dp row, its own shape, radius 12, a ring of ink, opaque,
   still. In the pin: edge to edge (§4.3, §4.4).
4. **Two kinds of entries.** Order, the mark (the spark is the family), and the right end (a word for things,
   bare for the model). No header, no gap (§3.2).
5. **Dark theme.** Nothing new uses the pill's colour. Three screens to look at again once the dark pill is
   settled: 2, 4, 7 (§7).
6. **Motion.** §3.4, §3.5, §4.5, §5.2. No new spec.
7. **M4: what breaks.** The table in §6.

---

## 11. Open, and who decides

**For the UX designer**
- What Down does on the line (my recommendation: the same as Tab).
- Whether the line comes back when the field is emptied again (mine: no).
- The thing or the verb as a row's title (mine: the thing).
- Whether "Copy its text" then shows the text's own list.
- `tr`: the language in the chip or in the text.
- All wording. I fixed the slots and their lengths, not the words.

**For the engineer**
- The line is a third thing under the field beside the card and the tip. `Metrics.height` must count it (8 + 56
  + 8), or the window clips it.
- The reflection's test for quiet (§3.5).
- A picture's size must be known before its row is laid out; its pixels may come later.
- The pin of a picture: its opening size, and whether it can open larger than the smallest (§4.4).
- A prompt run from the list should enter its scope with an empty field (§3.3).

**For Alex, on a device, before these are final**
- The line at 320 ms and at 200 ms after the opening, at medium speed.
- The picture at 176 × 104 on dark glass.
- Whether a capture lands on the clipboard at all (D5): it decides whether screen 1 has a picture state.

**Not verified, and relied on above:** a picture on the clipboard (DF: not tried); the capture tool (NF2 §2.2);
which actions the system's text classifier returns, and addresses (S&C §4.2; DF); the recogniser on a Googlebook
(S&C §3.1); how many entries the selection menu shows (NF2 §3); a pin's opening size for shapes other than the
three measured (DF); the model in languages other than German and English (LANG §5); everything on the HP.
