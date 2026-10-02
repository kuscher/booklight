# A richer flight row: the engineering paper

*2 October 2026. From the nine saved replies in `core/src/test/resources/airlabs/flight-*.json` and the code on `next-five`. Nothing was run on a device, nothing was asked of the service, nothing in the repo was changed. Contrast figures are computed, not measured, for Booklight's own colours at the Clear and Balanced glass levels; a wallpaper's colours will differ. "Not measured" marks what I could not check.*

## The short of it

- Every flight has both airport codes, both planned times and a state. Anything else can be missing.
- Do not use the service's `percent`. Work the plane's place out from the times: it is more accurate, it needs nothing the row does not already read, and it moves by the clock without a request.
- Build the row at **136 dp**. The most I would allow is **148 dp**.
- Draw the line and the plane in the row's ink. Colour belongs in the badge only, as a small filled chip with its own text colour.
- About six working days: four for the row, one for the pin, one for tests.

## 1. What the data really supports

| Reply | `status` | `percent` | `eta` | `duration` | `delayed` · `dep_` · `arr_` | Gate dep · arr | Terminal dep · arr | Belt | Position, aircraft | Today's row says |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| JL101, landed nine hours ago | landed | 100 | none | 65 | none · 1 · none | 14 · none | 1 · none | none | none | Landed |
| LH1184 | cancelled | 0 | none | 60 | none · none · none | A26 · none | 1 · 1 | none | none | Cancelled, times struck, no place |
| LH152, late, not gone | scheduled | 0 | none | 60 | 45 · 45 · 45 | A2 · none | 1 · none | none | none | Delayed 45 min · Terminal 1, gate A2 |
| LH454, an hour before | scheduled | 0 | none | 675 | none · none · none | Z58 · none | 1 · INTL | none | none | Planned · Terminal 1, gate Z58 |
| LH455, in the air | en-route | 95 | 34 | 645 | none · 7 · none | G13 · Z69 | INTL · 1 | none | both | In the air · 24 min early · Terminal 1 |
| LH455, five minutes after landing | landed | 100 | none | 645 | none · 7 · none | G13 · Z69 | INTL · 1 | none | both (on the ground) | Landed 29 min early · Terminal 1 |
| LH9152, a codeshare | en-route | 5 | 523 | 550 | 9 · 9 · 13 | Z20 · none | 1 · 5 | none | none | In the air · 13 min late · Terminal 5 |
| LH96 | landed | 100 | none | 55 | 3 · 3 · 3 | A14 · G10 | 1 · 2 | none | none | Landed · Terminal 2 |
| SQ26, second leg | en-route | 14 | 447 | 515 | 27 · 27 · none | B42 · none | 1 · 4 | none | both | In the air · 13 min early · Terminal 4 |

Today's `Flight` model reads none of `percent`, `eta`, `duration`, the delay fields, the position or the aircraft. It reads the three times at each end, terminal, gate, belt, the names and the state, and works "late" out from the times.

**What a design can rely on**

| Phase | Always there | Often missing |
| --- | --- | --- |
| Every flight | both airport codes, both planned times, the state, the airline | city names (a flight from the timetable may have only the codes) |
| Before it leaves | departure terminal and gate (3 of 3 here, all at large airports: do not count on it) | any expected time (1 of 2), so "On time" often cannot be said: the row says "Planned" and there is no badge |
| In the air | when it left, when it is expected to land (3 of 3) | position and aircraft (2 of 3, they come together), arrival gate (1 of 3) |
| Landed | when it left | the actual landing time (missing five minutes after landing; the expected time stands in), arrival gate (2 of 3), belt (0 of 9) |
| Cancelled | the planned times | everything else |
| From the timetable | codes, planned times | state, delays, gate, any estimate |

**The delay fields are not usable.** They are empty for "on time", for "early" and for "unknown" alike, and `delayed` disagrees with itself (empty for LH455, which left 7 minutes late; 27 for SQ26, which is expected 13 minutes early). Keep today's rule: late or early is the shown time against the plan, and within 5 minutes is on time (`FlightStatus.ON_TIME`). Eight of the nine replies give a badge by that rule; LH454 gives none.

**`percent`.** It is 0 before leaving (also for LH152, 39 minutes past its planned time) and 100 once landed, in every reply, so it is right at both ends and says nothing the state does not. In the air it is the share of the *scheduled* duration that the minutes to landing have left: 100 × (1 − `eta` / `duration`), rounded up. That fits all three replies. So it is linear in time, not in distance (SQ26 was 75 km into 6,189 km at "14"), and it does not start at 0: SQ26 said 14 after 27 of 475 minutes in the air. The `schedules` and `routes` replies have no `percent` at all, and two of the three roads of "the next flight" end there.

**Worked out locally instead:** share = (now − left) ÷ (lands − left), where "left" is the actual departure and "lands" is the actual, else expected, else planned arrival, each as a moment (so time zones do not matter). Against the replies: LH455 94 % (service 95), LH9152 5 % (5), SQ26 6 % (14). Rules: 0 until the service says it has left; between 2 % and 98 % in the air (the plane reaches the far end only when the service says "landed"); 1 once landed; no plane for a cancelled or diverted flight, nor in the row for a flight from the timetable (nobody knows whether it flew). The minutes to landing come from the same two moments.

**Moving between lookups.** The row carries the two moments, not a number, and the drawing reads a clock: one wake a minute, on the minute, only while a flight in the air is on screen. On a 628 dp line a one-hour flight moves 10 dp a minute and a ten-hour flight 1 dp, so nothing faster would show.

## 2. What the row can be

**Today.** `FlightsProvider.full()` makes a `Body.Slots`; four of its seven fields (`first`, `tail`, `struck`, `source`) were added for flights. `SlotsBody` draws three lines (14, 17, 14 sp) and, for a flight, cross-fades them when the answer lands (70 ms out, 110 in). `Metrics.rowHeight` gives every `Body.Slots` 92 dp by its type alone, which is why the height is known before the answer. `RowFrame` animates a height change on `place`; `Metrics.height(model)` sums the rows; `Panel` springs the window to that sum, the pill's lower edge on the same spring. A taller row changes one line in `Metrics` and nothing else in that chain.

**A new kind, `Body.Flight`,** not more fields on `Slots`. `Slots` is also the preview of mail, events, calls and snippets, and its height must then depend on a field. A new kind touches eight places, all mechanical: `Model.kt`, `Metrics.rowHeight`, three spots in `Rows.kt` (the row's `when`, the shared seat for "LH45" then "LH455", the mark and strip kept on the top band), the footer's source, the debug dump, and the provider.

**The drawing** is a `Canvas` in a new `overlay/FlightLine.kt`, as `LevelTrack` and the timer's line are, shared with the pin. The plane is the `plane` path already in `ui/Icons.kt` turned a quarter, moved in the draw phase, so its travel lays nothing out. No image files.

**The line's length.** In a band of its own under the mark and the strip, it can run from the text's edge to the strip's right end: x = 72 to 700, 628 dp, the same selected or not. Beside the strip it would change length whenever the row is selected or another action is armed, so it must not share the strip's band. The mark and strip stay centred on the top 92 dp, where they are today.

**Height before the answer.** By the body's type, as today: 136 dp from the first frame for a number that reads as a flight with a key in, and for the empty row under the `flight` keyword. A guess (`ps5`) or a number found in copied text stays 56 dp and grows once when the user goes to it; that step exists (56 to 92) and becomes 56 to 136. Without a key it stays 56 dp: nothing is known. A row with no answer ("Nothing found") keeps its height, with the line at rest.

**The largest height: 148 dp; build at 136.** Fitting is not the limit on the 14-inch screen: with eight rows the panel is 512 dp plus the flight row, and there is room for 798, so even 286 dp would fit. The limits are these:

- Row one is selected, so the whole row is the lilac pill. Past about 150 dp it reads as a card in a list.
- A guess that the user goes to pushes the rows under it down by the difference: 80 dp at 136, 92 at 148.
- While it waits, and when nothing was found, the row is that tall and mostly empty.
- 136 dp is a height the panel already has (an answer grown to four lines, released and seen on both devices). 148 is today's row plus one ordinary row.
- `Metrics.maxRows` counts every row as 56 dp. On a screen under about 880 dp high, eight rows with a 136 dp row run past the lower edge. That must be fixed with this work (a few lines).

## 3. Motion

**What runs today while the panel sits:** the caret's blink, and nothing else. The white reflection runs one lap of at most 2.2 s, once per opening. The light of work runs only while an answer is outstanding (for a flight, from 600 ms after the request until it lands). There is no endless animation anywhere in the app.

| Motion | Verdict | Cost |
| --- | --- | --- |
| The line draws in once when the answer lands, the plane riding its end, from the departure end | Yes. `place`, 40 to 60 ms after the text's cross-fade starts | About 0.4 s of frames on a 628 × 24 dp band, as a volume track does |
| The plane steps on with the clock | Yes. One short `tick` move a minute, only for a flight in the air | A sixth of a second of frames each minute |
| A new estimate moves the plane | Yes, once, on `place`; forwards only | As above |
| A plane that glides, bobs or pulses all the time; moving dashes; a "live" dot | No | Every frame the panel draws makes the system blur the desktop behind it again (not measured; this is why nothing else runs) |

The track, its end marks and the code slots must stand from the first frame, outside the text's cross-fade; only the plane and the flown part move. While a number is still being typed the plane goes in a fade, it does not fly back.

**Animations off** (`Motion.on` false): every spec from `Motion.kt` becomes a cut, so the plane is at its place in the first frame with the answer and steps each minute without a glide. This is free if the new code uses only `motion.place()`, `fade()` and `tick()`.

## 4. Colour and glass

- **Today:** one ink, `onSurface`, at 1.0, 0.80 and 0.60; fills are that ink at 0.08 to 0.24. The pill is `secondaryContainer` (from the wallpaper when that is on) at 0.78 light and 0.42 dark. The only semantic colours are `error` and `errorContainer`, kept for what removes something. There is no green, no amber and no fixed colour outside `ui/Theme.kt`.
- **The ground is not known.** At Balanced, light theme over a black window is mid-grey (#6B6B6B), and dark theme over a white one too (#6A6C6E). The row's ink holds 3.2:1 and 4.2:1 there. A mid-tone green or amber *text or line* straight on glass comes to **1.2:1** in light theme over black and 3.1:1 in dark over white. It fails. The footer's "Copied" lost its colour for this reason.
- **What works:** a filled chip that brings its own ground, as the red pill does. A light container at 0.85 with dark text on it (dark theme: the reverse) keeps the words at **6.3:1 or better** in all twelve cases I computed (both themes, over black and over white, Clear and Balanced, and on the pill). The chip's edge against its ground is weak (1.1:1 in light theme over a white window, 1.1 to 1.6 on the pill), so it needs the pill's 1 px white outline, and the word must carry the meaning.
- **New tokens needed:** a container and its text colour for green and for amber, in each theme: eight values, fixed (not from the wallpaper), in `Theme.kt`. Starting values: light #B6F2BE with #002109, #FFDEA6 with #261900; dark #0A5226 with #B6F2BE, #5C4300 with #FFDEA6.
- **Two rules of the design system change** and need the owner's word: "A delay is said, not painted" (§14) and "the pill is the only coloured surface" (§1). I would keep red for what removes something: a cancelled flight stays struck through with a neutral chip.

## 5. The pinned flight

**Today** `PinnedFlight` draws a small line (20 dp) and a figure at 34 sp (42 dp) in a 280 × 118 dp window on solid ground (`surfaceContainerHigh`). It wakes once a minute while on screen and asks the service every half hour when the flight is near. The timer's pin beside it already has a 3 dp line under its figure.

**To show the line:** the same drawing from `FlightLine.kt`, 8 dp under the figure in a 16 dp band, 240 dp long. 20 + 2 + 42 + 8 + 16 = 88 of 118 dp, so it fits. **Keep 280 × 118**: the system does not reshape an open pin, and another shape would need a window of its own. The minute's wake is already there, so the plane moves by the clock for the thirty minutes between two asks at no cost; a new answer moves it once. Colours are easy here: the ground is solid (green and amber text reach 5.3:1 light, 8.4:1 dark). A pin made anew after the process died has no flight for a moment: the line must have a resting state.

## 6. Costs and risks

| Part | Days | What is in it |
| --- | --- | --- |
| (a) The row | 4 | `Body.Flight`, the provider, the layout with fixed slots, line, plane and chip, the clock, tokens, strings in English and German, `maxRows`, the debug dump, the two design documents; one of the four days is a pass on a device (both themes, light and dark windows behind, three glass levels and Solid, both clocks, German) |
| (b) The pin | 1 | the shared line, its resting state, a look at it in the pinned window |
| (c) Tests | 1 | see below |

**Into `core/`, with tests on the saved replies:** `FlightStatus.span(f)` (the two moments), `FlightStatus.progress(f, now)` (94 % for LH455, 6 % for SQ26, 0 before, 1 landed also without an actual time, never 1 in the air, none when cancelled), a rule that the shown share never goes back for the same flight, and `FlightStatus.badge(f, now)` (on time, late, early, cancelled, none; 4 and 5 minutes either side; the departure's delay until it has left, the arrival's after).

**Most likely to go wrong**

1. **The plane goes backwards.** A later estimate lengthens the flight and the share drops (ten minutes later on a ten-hour flight at 90 %: about 9 dp back). With the service's `percent` it would also start 8 % along. Answer: the local share, and a hold: the plane waits until the clock catches up.
2. **The row is too tall.** Not by overflow on the 14-inch screen (150 dp to spare at 136), but as a slab of pill, as an empty row while waiting, and on smaller screens through `maxRows`. Answer: 136, the `maxRows` fix, and a designed waiting state.
3. **Glass eats the colours.** Answer: chips with their own ground, ink for the line, and the device pass over both kinds of window. Only one test device is free for that now, and today's flight row is itself "built, not yet judged on a device" (§14).

## 7. For the designer

**Do**

- Put the airport codes at the line's two ends: both are always present. Give each a fixed slot (about 44 dp), so the line is as long before the answer as after.
- Draw every state: waiting (line at rest, no plane, codes as a rule), planned without a badge, late before leaving, in the air, landed, cancelled, no answer.
- Draw the selected row first: row one sits on the lilac pill, whose hue is the wallpaper's.
- Draw the line, its end marks and the plane in ink: full for what is flown, 0.20 for the rest (the volume track's values).
- Make the badge a small filled chip with a word in it, in English and German ("On time", "Pünktlich"; "45 min late"). Colour only there.
- Give the line a band of its own under the strip, 628 dp wide at most, and keep the mark and the strip where they are.
- Use "lands in 34 min" if you like: it can always be worked out in the air.
- Design the pin's line for 240 × 16 dp inside the present 280 × 118 window.

**Do not**

- Rely on gate, arrival terminal, belt, aircraft, altitude or speed. Treat them as extra words that may be absent; the belt was never there.
- Show a share as a number from the service, or place the plane by distance: there is only time.
- Show "On time" hours ahead: without an expected time there is no badge.
- Put coloured text or a coloured line straight on glass, or use red for late.
- Let the plane touch the far end before "Landed", or move for a flight from the timetable.
- Animate anything endlessly: no pulse, no moving dashes, no gliding plane. One draw-in, then a step a minute.
- Draw a map, a route curve over land, an airline logo or a photo: no tiles and no images from the network.
- Let anything change the line's length or the row's height when the answer lands, when the row is selected, or between a 12-hour and a 24-hour clock.
- Go past 148 dp, or make the pin a new size.
