# The flight row: the design

*2 October 2026. Drawn and looked at as a page (`flights.html`, pictures in `shots/`), not on a device: every alpha and
every time below is a starting value. The page is set in the Mac's system face, not Google Sans Flex, so widths on the
device will differ by a few dp. Coordinates: x is in the panel (0 to 720), y is in the row (0 at its top edge). "Ink" is
`onSurface` at an alpha, as in the design system.*

## 1. The idea in four lines

- The row is **136 dp** in every state, from its first frame. Today's is 92.
- Its upper part is what a row always is: mark, words, strip, on one centre line (y = 46, where today's flight row has them).
- Under that hangs the flight: **one line from the text's edge to the strip's right end** (x = 72 to 700), the plane on it, and under its two ends the airport and its time.
- One **badge** after the headline says whether it runs to plan. It is the only place with a colour.

```
x: 20      56  72                                                              700
    ┌──────────────────────────────────────────────────────────────────────────────┐ y 0
    │          LH 400 · Lufthansa · Frankfurt/Main → New York                      │ 12–32   line one
    │  (mark)  Lands in 4 h 07 min  (Delayed 27 min)        [Open ⏎] copy pin  ⌄   │ 34–58   headline, badge; centre 46
    │                                                                              │
    │          ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━✈ · · · · · · · · · · · · · · · ·  │ 84      the line
    │          FRA 11:19 AM  Boeing 747-8                 Terminal 1  2:02 PM JFK  │ 100–122 the two ends
    └──────────────────────────────────────────────────────────────────────────────┘ 136
```

## 2. Anatomy, every measure

| Part | x | y | Measure and type |
| --- | --- | --- | --- |
| The row | 8 to 712 | 0 to 136 | the list's pill when selected, radius 24, as every row |
| Mark | 20 to 56, centred on 38 | 28 to 64, centred on 46 | the 36 dp disc with the `plane` symbol at 20 dp: unchanged from today's flight row |
| Line one | from 72 to 16 dp before the strip | 12 to 32 | the small type (`SMALL`: 14 / 500 / 0.1), one line, the 24 dp fade at its end |
| Headline | from 72 | 34 to 58 | the title type (17 / 500), tabular figures, one line |
| Badge | 10 dp after the headline's last letter | its word on the headline's baseline | a stadium 22 dp high, 9 dp inside each end, the word at 13 / 600 / 0.1 (the footer hint's size at the weight of its word), a 1 px white rim like the pill's |
| Strip | its right end at 700 | 30 to 62, centred on 46 | unchanged: 32 dp high, 18 dp icons at a 32 dp pitch, the armed one unrolled. Unselected: the kind, "Flight", on the same line |
| The line | 72 to 700, 628 dp, always | centre line y = 84, in a band 74 to 94 | **flown part**: 3 dp, solid, round ends, full ink. **Still to go**: dots of 3 dp, 70 of them at a pitch of 9.06 dp, so the first and the last stand exactly on 72 and 700 |
| The plane | its tail at 72 + share × 608 | centred on y = 84 | the `plane` symbol at its own 24 dp size, turned a quarter to point right: 20 dp long, 19 dp across the wings, full ink |
| Around the plane | | | the flown part ends 4 dp before the tail. The dots come into view over 8 dp, beginning 2 dp ahead of the nose (a soft edge, as an opened row's list has), so no dot is ever cut in half |
| The left end | from 72 | 100 to 122, all on one baseline | airport code (17 / 500, ink 0.80) · 7 dp · time (17 / 500, tabular, full ink) · 12 dp · small words (14 / 500 / 0.1) |
| The right end | ending at 700 | the same | small words · 12 dp · time · 7 dp · airport code: the mirror, so each code stands under its end of the line |
| Under the ends | | 122 to 136 | 14 dp |

**Where the measures come from.** 12 + 20 (line one) + 2 + 24 (headline) puts the headline's centre on y = 46: the centre
line of the mark and the strip in every tall row today. So nothing of today's row moves but the words. The strip ends at
y = 62, the plane's wings begin at 74. The ends' words begin 6 dp under the plane, closer to the line than the headline
is: they are the line's labels.

**The plane's symbol.** In `Icons.kt` its body runs along x = 11.5 of 24, not 12. Turned, it must be moved half a dp so
that its body lies on the line's axis. (In the mark's disc it is the same 0.4 dp off centre today.) It is 20 units long
in a 24 box (2 to 22): place it by those, not by the box, or its tail misses the edge by 2 dp.

**Alignment** (measured in the pictures, `shots/ruler.png`):

| Edge | What stands on it |
| --- | --- |
| x = 72, the titles' edge | line one, the headline, the line's start, the plane's tail at rest, the departure airport's code, and the titles of the rows above and below |
| x = 700, the strip's right end | the strip, the line's last dot, the plane's nose once landed, the arrival airport's code, the footer's last word |
| y = 46 | the mark, the headline's capitals, the strip (or "Flight") |
| one baseline each | headline and the badge's word (measured: within a quarter of a dp); code, time and small words at each end |

The line's round ends reach the two edges; a capital's stem stands about 1.3 dp inside its box, which is where the round
end's centre is (1.5 dp in). Looked at closely it reads as one edge.

## 3. What the words are

| Line | Says | Ink |
| --- | --- | --- |
| Line one | "LH 400 · Lufthansa · Frankfurt/Main → New York". Before the answer: "LH 400 · Lufthansa" (the bundled table knows it) | 0.80; full on the pill |
| Headline | the one thing needed: "Leaves Fri 10:55 AM", "Leaves in 42 min", "Lands in 4 h 07 min", "Landed 12 min ago", "Cancelled", "Looking it up", the reason there is no answer | full |
| Badge | whether it runs to plan: "On time", "Delayed 27 min", "24 min early", after landing "27 min late"; "Planned" when only the plan is known | its own (§4) |
| Left end | "FRA 11:19 AM", then small: gate and terminal while it has not left ("Gate Z58 · Terminal 1"); the aircraft once it has ("Boeing 747-8") | code 0.80, time full, small words 0.80 (full on the pill) |
| Right end | small: terminal and belt (else gate) once it has left; then "2:02 PM JFK" | the same |

- **A countdown keeps its width.** From an hour on, the minutes have two places ("4 h 07 min", as the pin writes it), in
  tabular figures: nothing shifts when it counts.
- **The badge never repeats the headline.** "Cancelled", "Diverted" and "From the timetable" are headlines; there is no
  badge beside them. "Landed 12 min ago" has the verdict of the landing as its badge, not the word "Landed".
- **A day beside a time** as today: nothing on the user's today, the short weekday within six days, the date beyond.
  It can be as wide as it likes: nothing depends on it. Today's least width for the first slot (176, 196, + 24) is gone.

## 4. Colour

**The decision: colour in the badge, nowhere else.** The line, the plane, the times and every word stay in the row's ink.

| Token (new, fixed, in `Theme.kt`) | Light | Dark | Used for |
| --- | --- | --- | --- |
| `flightGood` · `onFlightGood` | #B6F2BE · #00210A | #0A5226 · #B6F2BE | "On time", "24 min early" |
| `flightLate` · `onFlightLate` | #FFDEA6 · #261900 | #5C4300 · #FFDEA6 | "Delayed 27 min", "27 min late" |
| the neutral badge | ink at 0.10 · full ink | ink at 0.14 · full ink | "Planned" (the scope chip's recipe) |
| the badge's fill | at 0.88 | at 0.88 | so it brings its own ground |
| the badge's rim | white 1 px at 0.55 | white 1 px at 0.30 | the pill's, the pane's, the scope chip's |
| in the pin, as words on solid ground | #17662B · #7A5200 | #8FDB9B · #F1C267 | "on time", "27 min late" |

| Ink | Light | Dark |
| --- | --- | --- |
| Flown part, plane | 1.0 | 1.0 |
| Dots still to go | 0.50 | 0.55 |
| The line at rest (no plane: waiting, no answer, cancelled, diverted) | 0.30 | 0.34 |
| An empty end's rule, 20 × 2 dp | 0.30 | 0.30 |
| Struck times | 0.80, struck through | the same |

**On glass.** The four cases are in `shots/` (light and dark panel, each over a light and a dark window). The line, the
plane and the words are ink and hold as the row's titles hold. A green or amber word or line straight on the glass does
not (the engineer's figure: 1.2 : 1 in light theme over a black window), which is why the colour is a filled badge with
its own text colour: 6.3 : 1 or better in every case. The badge's edge against the lilac pill is weak in light theme;
the white rim holds it. The weakest thing in the row is the dots over mid-grey glass on an unselected row: they say
"not yet", and may be faint, but they must be seen. First thing to look at on the device.

**Flown and still to go differ three ways,** none of them a hue: solid against dotted, full ink against half, and the
plane between them. In a grey picture the line reads the same.

**Red is not used.** A cancelled flight is the headline "Cancelled", struck times and a line without a plane. Red stays
the colour of what removes something, and a red badge with a word in it is exactly what an armed Delete looks like.

**What the ink-only look loses** (the page's last switch shows it): the answer to "is anything wrong?" without reading.
Every badge is then the neutral one, and "Delayed 27 min" looks like "On time" until the words are read; in the pin,
across the room, there is nothing to see at all. It keeps the rule "a delay is said, not painted", the pill as the one
coloured surface, and it needs no new token. It is a sound row; it is not what was asked for.

## 5. Every state, in words

| State | Line one | Headline · badge | The line | Left end | Right end |
| --- | --- | --- | --- | --- | --- |
| **Planned, far off** (also a timetable's flight still to come) | number · airline · cities | "Leaves Fri 10:55 AM" · Planned | all dots; the plane at rest at the start, its tail on 72 | FRA Fri 10:55 AM, then gate and terminal if known | Fri 1:35 PM JFK |
| **Leaving within 3 h** | the same | "Leaves in 42 min" · On time, Delayed 45 min, or Planned when no expected time has come | the same | FRA 10:55 AM · Gate Z58 · Terminal 1 | 1:35 PM JFK |
| **In the air** | the same | "Lands in 4 h 07 min" · On time, Delayed 27 min, 24 min early; none when no expected landing is known | solid behind the plane, dots ahead; the plane at the share of the flying time that has passed, never nearer an end than 2 % | FRA 11:19 AM · Boeing 747-8 | Terminal 1 · 2:02 PM JFK |
| **Landed** (for 3 h) | the same | "Landed 12 min ago" ("Landed just now" in the first minute) · On time, 27 min late, 29 min early; none when only the plan is known | all solid; the plane at the end, its nose on 700 | FRA 11:19 AM · Boeing 747-8 | Terminal 1 (and "· Belt 4" when the service names one) · 2:02 PM JFK |
| **Cancelled** | the same | "Cancelled" · no badge | dots at rest, no plane | FRA, the time struck | the time struck, JFK |
| **Diverted** | the same | "Diverted" · no badge | dots at rest, no plane | FRA and when it left | the time struck, JFK; no terminal |
| **A timetable's flight past its time** | the same, or without cities if the timetable has none | "From the timetable" · no badge | dots at rest, no plane: nobody knows | FRA and the timetable's time | the timetable's time, JFK |
| **Looking it up** | number · airline | "Looking it up" · no badge | dots at rest, no plane | a rule, 20 × 2 dp, at 72 | a rule ending at 700 |
| **No answer** | number · airline | the reason ("No connection", "Nothing found for this number", …) · no badge | the same | the rule | the rule; Copy and Pin dimmed, as today |

The line, its dots and the two ends' places stand from the first frame. The white light of work runs round the outline
as today when the answer takes more than 600 ms.

**Unselected** (a local match stands above it): the same row on the glass, no pill, "Flight" where the strip was, line
one and the small words at 0.80.

## 6. What is missing leaves no hole

| Missing | What the row does |
| --- | --- |
| Gate | "Terminal 1" alone |
| Gate and terminal | the time is the last thing at that end |
| Belt | the terminal alone; arrival gate if there is one |
| Aircraft | nothing after the departure time |
| City names | line one is "LH 400 · Lufthansa"; the codes are under the line anyway |
| Expected time | the countdown runs to the planned time; no badge in the air and after landing, "Planned" before leaving |
| Actual landing time, just after landing | the expected one stands in |
| Any time of landing at all, in the air | the headline is "In the air", the line has no plane |
| Position, altitude, speed, `percent` | never used: the plane's place is time (the engineer's rule) |

Nothing is a held place: small words are the last thing at their end and grow towards the middle of the row, where there
is always room. If both ends are so long that they would come closer than 24 dp (dates at both ends on a 12-hour clock
with a long aircraft name), the aircraft is left out first, then the terminal.

A reason too long for the headline's room ("Die AirLabs-Abfragen dieses Monats sind aufgebraucht", 431 dp against about
390) is set in the small type, full ink, chosen once for that text, as an answer's size is chosen once.

## 7. Motion

All from `Motion.kt`. The line is drawn from one number, the plane's place: it sets the plane, the end of the flown part
and the edge that uncovers the dots, so the three cannot come apart.

| What happens | What moves | Spec | What stays still |
| --- | --- | --- | --- |
| **The answer lands** | Words change where they stand, as today (70 ms out, 110 in after 40). The empty ends' rules go with the old words. The plane fades in at the start (110 ms) and, 60 ms after the words began to change, travels to its place; the flown part grows behind it; the dots ahead step from "at rest" to "still to go" (110 ms) | the travel on `opens(300 + 300 × share)`: 300 ms for a plane that stays at the start, 600 for one that has landed | the row's height, the line's two ends, the dots' places, the mark, the strip |
| **A minute passes** (in the air only) | the headline's number rolls, the plane takes its step: 1 dp for a ten-hour flight, 10 dp for a one-hour one | `roll`; `tick` | everything else. One wake a minute, none when no flight in the air is on screen |
| **Another answer, the same flight** | words that changed fade as above; the badge's colour fades (120 ms) while its word changes; the plane goes forward to its new place, never back: if the new place is behind it, it waits until the clock has caught up | `fade`; `opens(300 + 300 × the way)` | the plane, if nothing changed |
| **It lands while the panel is open** | the plane travels the last of the line; the headline changes | `opens` | |
| **The number is typed on** (another flight, or no flight yet) | the plane and the flown part fade where they stand (110 ms), the dots go back to rest. It never flies back | `fade` | the line |
| **The selection comes or goes** | the pill's edges on `lead` and `trail`, as for any row: coming down onto the row its lower edge leads and the pill is 136 dp for it; leaving, it gathers itself to the next row's 56. The strip slides in 12 dp, 60 ms after | as today | the line and the plane: they do nothing for the selection. The mark keeps its small swell |
| **A guess the user goes to** | the row grows from 56 to 136 once, the pill's lower edge with it; mark and strip travel from y = 28 to 46 | `place`, as today's 56 to 92 | |
| **Animations off** | nothing travels: the plane is at its place in the frame of the answer and steps each minute without a glide | every spec is a cut | |

- **Why `opens` and not `place`** for the plane's travel: `place` overshoots by about half a per cent, 3 dp on a full
  line, and a plane that passes the end of its line and comes back is wrong. `opens` is the glass's own curve (slow,
  fast, a long way of slowing down, no overshoot) and is a cut with animations off like the rest.
- **Nothing loops.** No pulse, no moving dots, no gliding. After the one arrival the row is still until the next minute.
- The reflection treats the answer landing as the list changing, as today.

## 8. German

| English | German | New string? |
| --- | --- | --- |
| Leaves Fri 10:55 AM | Abflug Fr. 10:55 | new |
| Leaves in 42 min | Abflug in 42 Min. | new |
| Lands in 4 h 07 min | Landung in 4 Std. 07 Min. | new |
| Landed 12 min ago · Landed just now | Vor 12 Min. gelandet · Gerade gelandet | new |
| Planned · On time · Delayed 27 min · 24 min early · 27 min late | Geplant · Pünktlich · 27 Min. verspätet · 24 Min. früher · 27 Min. verspätet | there |
| Cancelled · Diverted · From the timetable | Gestrichen · Umgeleitet · Laut Flugplan | there |
| Gate Z58 · Terminal 1 · Belt 4 | Gate Z58 · Terminal 1 · Band 4 | there (joined with " · ", the gate first) |

The German headlines are my proposal: the first paper gave them in English only.

**The widest case** is the headline and its badge beside the strip. Room: 700 less the strip less 16, from 72: 404 dp
with "Öffnen" armed, 388 with "Anheften". "Landung in 12 Std. 07 Min." (205) + 10 + "1 Std. 15 Min. verspätet" (166) is
381: it fits, with 7 dp to spare. If a typed verb puts a long name in the strip's last slot ("In den Kalender"), the
room is about 345: then the headline's line ends in the 24 dp fade, the badge's end first. Line one (318 to 364 dp)
fits; a longer one fades. The line and the two ends are under the strip and never meet it. Everything was looked at
in German (`shots/de-*.png`).

## 9. The pinned flight

The window stays 280 × 118 on its solid ground. Content column 20 to 260.

| Part | y | What |
| --- | --- | --- |
| Small line | 15 to 35 | 14 / 500 at 0.80: the number · the verdict as a word in its colour ("on time", "27 min late") · the gate while it has not left, the time of landing after. If the three do not fit 240 dp, the last is left out (never half a word behind the fade). Planned: number · time, as today. Cancelled: "LH 400 · was 10:55 AM" |
| Figure | 37 to 79 | as today: "42 min", "4 h 07 min", "Landed", "Cancelled", 34 sp (28 if longer than twelve letters) |
| The line | 87 to 103, centre y = 95 | 240 dp: flown part 2 dp, dots of 2 dp at a pitch of 7 dp, the plane at 0.8 of its size (16 dp long), 3 dp between line and tail |

- No airport codes, no aircraft, no filled badge: a glance takes the figure, the line and one coloured word.
- Between two answers (half an hour apart) the plane moves by the clock with the figure, a step a minute, on `tick`. A
  new answer moves both once.
- Before leaving the plane rests at the start; cancelled, or with nothing known yet (a pin made anew): the dots at rest.
- Words in colour are right here and wrong in the panel: the pin's ground is solid.

## 10. Without a key

The ordinary 56 dp row, unchanged: the plane's mark, "LH 400 · Lufthansa", and on the selected row Open, Search, Ask
Gemini and "Set up times". No line, no badge, no faded copy of the rich row. The page shows it under "Without a key".

## 11. Where I went another way

**From the first paper (what the row must say)**

| It said | The design | Why |
| --- | --- | --- |
| Where to go stands after the badge | Under the line, at the end it belongs to | The headline shares its band with the strip: in German headline, badge and "Terminal 1 · Band 4" do not fit there. Under the line there is room, and a gate reads as "at FRA" |
| Aircraft as a supporting word | Small, after the departure time, once it has left | The only free place that is always wide enough. Line one is full (it fades at 388 dp) |
| Badges "Landed", "In the air", "Cancelled", "Diverted", "From the timetable" | No badge that repeats the headline | The same word twice, 10 dp apart. After landing the badge is the landing's verdict instead |
| Cancelled and diverted in the error red | No red; the headline, struck times, no plane | Red is what removes something; a red badge is the armed Delete's look. One line to change if the owner wants it |
| The pin shows the verdict alone after the number | Number · verdict · gate or time, the last left out when it does not fit | The gate is the one thing the figure cannot tell |
| At most 148 | 136 | It fits, and the panel already has that height |

**From the second paper (what can be built)**

| It said | The design | Why |
| --- | --- | --- |
| Airport codes in fixed 44 dp slots at the line's ends | Codes under the ends; the line runs the whole 628 dp | The line's length then depends on nothing: not the codes, not the clock, not a date |
| The rest of the line at ink 0.20, like the volume track | Dots at 0.50 | 0.20 is right for a 6 dp band; 3 dp dots vanish at it |
| The plane's travel on `place` | On `opens` | No overshoot past the line's end (§7) |
| Late from 5 minutes (today's rule) | From 15, the first paper's figure | With a colour on it, "Delayed 6 min" is an alarm for nothing; the exact time is under the line either way. To the code: `ON_TIME = 15` |
| No badge when only the plan is known | The neutral "Planned" | It says how much is known, costs no colour, and keeps the headline's line the same shape in every state |
| No plane for a timetable's flight | At rest at the start while its time is still to come; none once it has passed | A plane at the start claims only "it has not left", and that is certain. It never moves for such a flight |

Taken as given from the second paper: the place by time and never by `percent` or distance; 2 % to 98 % in the air; the
plane never goes back; a step a minute and one arrival; 136 dp; the filled badge with its rim; `Body.Flight` as a kind of
its own; the `maxRows` fix.

## 12. For the owner to decide

1. Colour in the badge (green, amber), and so two rules changed: "a delay is said, not painted" and "the pill is the only coloured surface". Recommended: yes.
2. Late from 15 minutes. Recommended.
3. Cancelled without red. Recommended.
4. 136 dp. Recommended; 148 is the most.
5. The aircraft's place, and no altitude or speed.

## 13. To look at on a device

1. The dots over mid-grey glass (light theme over a dark window, dark over a light one), unselected and on the pill: seen, and still quieter than the flown part?
2. The badge on the wallpaper's own lilac, whatever hue that is on his device: do mint and amber sit with it? And the dark theme's deep green on the see-through pill.
3. The plane's arrival at 300 to 600 ms: a pleasure or a wait? And its last stretch on `opens`.
4. The plane at 20 dp beside 18 dp strip icons, in Google Sans Flex's company: the right size?
5. The minute's step together with the number's roll.
6. Widths in the real face: the German headline and badge beside "Anheften", the ends with a date on a 12-hour clock.
7. The pin's coloured word at arm's length.
