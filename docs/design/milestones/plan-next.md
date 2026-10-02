# Booklight after 2.1: the copy, then flights

*1 October 2026. The plan as adjusted on Alex's word: "adjust the plan for the main ux by doing M1. Let's add a
new M5 which handles flights. I want to type LH455 and get info about the next flight. Plan, design, and add
this in. Put M1 and M5 into a plan including the other improvements your new plan had and present to me for
final answer." Planning and design only: nothing of M1 or M5 is built and no device was touched for them.*

*The same plan with its screens drawn at real size is the page `docs/design/booklight-milestones.html` (made by
`tools/design-milestones/gen.py`): screens 1 to 4 are M1, 12 to 15 are M5. The four-milestone plan this one
grew out of is `plan.md` beside this file; its M2, M3 and M4 are parked, unchanged. Sources: **FL** =
`docs/research/flights.md`; the others as in `plan.md`. "Check n" is a line of the engineer's device session
(`03-eng.md` §12); "check F n" is one of M5's (§4 below). "Not verified" is said wherever the research says it.*

---

## 1. What comes next

| | What | Size | State |
| --- | --- | --- | --- |
| **2.2** | What Alex asked for after 2.1: the opening at Medium on its new curve, glass frosted from its first frame, the white reflection, three placements on an app's row, the see-through selection in dark | Built | On main, reviewed, not released |
| **M1 · The copy** | Copy something, open Booklight, and one line offers what can be done with it | About 14 days (the engineer's, not re-sized) | Planned and drawn; fourteen checks wait for a device session |
| **M5 · Flights** | Type a flight number and the row answers with the next flight | About 10 days (not sized by an engineer) | Planned and drawn here; the full answer needs a key of the user's own |

No new permission in any of it. Parked, not dropped: M2 the picture, M3 the selection, M4 your language.

---

## 2. 2.2: what is on main

Each of Alex's notes on 2.1 went through a visual designer and a motion designer
(`docs/design/reviews-after-2.1/`, `design-system.md` §12).

| What he said | What it is now |
| --- | --- |
| "default to medium for opening speed"; "slow beginning then fast then slow again" | The opening is at Medium unless chosen otherwise, on a curve that starts slowly, is quick through the middle and lands over a long stretch, with no rebound |
| "why the blur just pops in" | The glass is frosted from its first frame and stays so until the seam is gone. The window never moves; the blur's region is fitted to the glass before every frame (`device-findings.md`, "The blur follows the glass") |
| "the shimmer … should come like a few seconds later and be more complete … a white reflection running around" | One white light, 2.4 s after the panel has opened, once round from the middle of the top edge. Any key and it fades. While the model works, the same light runs steady and dimmer |
| "the many Window management options by default are too much" | An app's row has six icons: Open, New window, App info, Maximise, Left half, Right half. The other places are in the list under the arrow |
| "the highlight feels stark on dark" | The selection in dark is see-through like the panel: the same colour at 42 % |
| "it doesnt show the actions for apps like chrome which has new tab" | Found, not changed: Chrome keeps that shortcut closed to other apps; only the device's assistant may start it (DF) |
| Found on the way | The seam the panel opens out of had been invisible since 2.0's shadow. It is seen again |

Releasing it is a tag: a GitHub release and a draft on Play's closed testing. Play's production stays empty
until Alex says otherwise. Decision 2.

---

## 3. M1 · The copy

**Promise.** Copy something, open Booklight, and one line offers what can be done with it.

M1 is `plan.md` §4, whole: the quiet line for a copy under two minutes old; Tab makes the copy the chip and
shows what is in it (a link, a date, a phone number, a mail address); the model's rows answered where they
stand; a typed language or `tr` translates into any language; a typed instruction; `clip` with fewer rows;
shared text behaving the new way. Its screens are 1 to 4 on the page. Its device checks are 1, 3 to 8, the
translation half of 18, and the line in dark over a white window.

**What changed for this plan.**
- A flight number in a copy was left out of M1 because Booklight could only search the web for it. With M5 it
  is a thing found in a copy and gets M5's row.
- Its four decisions have new numbers in §6: the line under the empty field is 3; the translation first is 8;
  `clip`'s shorter list is 9; shared text the new way is 10.
- The six shared pieces M1 builds (one way to an answer for any row, the material travelling beside the field,
  ready-made prompts kept by reference, one clipboard helper, the language of a text, plain answers) are still
  built: the parked milestones stand on them, and M5's row in a copy uses the first two.

---

## 4. M5 · Flights

**Promise.** Type a flight number and the row answers with the next flight.

**Alex's words:** "Let's add a new M5 which handles flights. I want to type LH455 and get info about the next
flight."

**The story.** Someone you are meeting is on LH 455. You press the key and type `LH455`. A moment after the
last letter the first row says: Lufthansa, San Francisco to Frankfurt, leaves 15:05, lands Friday 10:55,
delayed 25 minutes, Terminal G, gate G4. Enter opens the flight's page with its map. Tab, Tab, Enter keeps the
flight on top of your windows, counting down. (Every time, gate and delay in this plan is made up; the route is
real.)

### The hard fact

There is no free source of a flight's planned or expected times that an app may call without a key; the
research called every candidate (FL §1). Free are: which airline a designator belongs to (a table of about
30 KB in the public domain), a flight's usual route (public tables, which disagreed with each other on three of
five flights compared), and where an aircraft is at this moment, but only while a volunteer's receiver on the
ground hears it. LH 455 itself was on none of those feeds during the research. Times, terminal, gate and a real
status are sold: Raycast's flight extension asks each of its users for a key (FL §6); Apple and Google show
such a card without one and do not say where the data comes from.

So M5 has two layers.
- **For everyone:** the row names the airline, and Enter opens the flight's page.
- **With a key of the user's own** (AeroDataBox through RapidAPI: a free account, about 200 lookups a month):
  the row answers as in the story. Booklight ships no key: its code is public and the service's terms forbid a
  shared one (FL §2). No keyed service was called in the research, because there was no key: every word about
  the answer's fields is from the service's own description, **not verified**.

### What is in it

1. **A flight number is a row.** `LH455`, `lh 455`, `LH0455` and the callsign `DLH455` are read as Lufthansa
   455, from the bundled airline table. The row says who flies it. Enter opens the flight's page at FlightAware;
   the other actions are a web search, Ask Gemini, and "Get times", which opens the place in Booklight's window
   where a key is set up. Nothing leaves the device before Enter. No key, no switch.
2. **With the key, the row answers.** 400 ms after the last letter (never after each one) Booklight asks the
   service once and the row is written in: who flies it and where; when it leaves and lands, each in its
   airport's own time, with the day when that is not the user's today; the status in plain words; and where to
   go: the terminal and gate before it leaves, the arrival's terminal and belt after. Which flight is "the
   next" is decision 12. A day after the number picks that day's (`LH455 fri`, `LH455 tomorrow`). The answer is
   kept for two minutes, and the footer says who gave it and when.
3. **What to do with it.** Enter opens the page, which has everything else (desks, the aircraft, the map). Copy
   puts one line on the clipboard. Pin keeps it on top. The arrow opens three more as a list under the row: Add
   to calendar, Search the web, Ask Gemini. All four stand in the row from its first frame; one that needs the
   answer waits for it.
4. **Keep it on top.** Pin puts the flight in 2.0's small window: the time to go, then the time to landing,
   then "Landed". It counts down by itself. To stay true it asks the service again every 30 minutes while it is
   pinned and the panel is closed, about twenty lookups for a long flight: sending without a key press, so it
   is part of decision 7.
5. **Its usual route, without a key.** Behind a switch that starts off: the row adds "Usually San Francisco →
   Frankfurt" from a public table (CC0, FL §1.1). The table's host asks to be told before an app relies on it;
   that letter comes first.
6. **A flight in a copy** (with M1): the line says "a flight", and after Tab its row stands with the link and
   the date and answers when selected.

**What looks like a flight and is not.** Nearly every pair of letters is some airline's designator: `PS5`,
`MP3`, `H264`, `MS 365`, `Q4 2026` all read as flights (FL §4). So: a *strong* match (two letters that are not
an everyday word, two to four digits; or the three-letter form) may be looked up after a pause. A *weak* match
(a digit in the designator, a single digit, a word like AM, IN or OK, a year) is the last row and nothing is
sent for it until the user goes to it (Down or Tab). A flight's row never stands above an app, a sum or one of
the user's links. The keyword `flight` (German `flug`) makes any of them a flight at once. The price is
decision 13: Ryanair (FR), easyJet (U2), Wizz (W6), JetBlue (B6), Austrian (OS), Alaska (AS), Condor (DE) and
Qatar (QR) are all weak.

### Deliberately out

A key of Booklight's own in the app. A server of Booklight's that holds one (Alex said "not yet" to a relay).
"In the air now" from volunteers' receivers: it shows nothing over oceans and for most short flights in
Europe, which fly under callsigns unrelated to the ticket's number (FL §1.5). A map. A list of facts under the
row (desks, the aircraft): the flight's page has them. The flight after this one (a second lookup). Codeshares
and numbers with two legs get no drawing yet. Search by route. Booking,
prices, seats. Notifications when a gate changes (that needs something in the background). A second service to
choose from (FlightAware's personal key fits the same field later). Airports as rows of their own.

### The designs (drawn on the page)

**The flight's row.** The app's slots body at the Event row's height: 92 dp from the moment it appears; the
plane on the icon column (x = 38); text from x = 72; the strip ending at x = 700. Three lines, which stand where
they stand from the first frame (the second time starts at a fixed x), so nothing moves when the answer is
written in:

| Line | Content | Type |
| --- | --- | --- |
| 1 | The number, who flies it and where: "LH 455 · Lufthansa · San Francisco → Frankfurt" | Small, second ink |
| 2 | The two times as named values: LEAVES SFO 15:05 · LANDS FRA Fri 10:55. "Leaves" becomes "Left", "Lands" becomes "Landed". Struck through when the flight is cancelled or diverted | The Event row's slots, title size |
| 3 | The status in full ink, then where to go: "Delayed 25 min · Terminal G, gate G4" | Small; the status full ink, the rest second ink |

A delay is said, not painted: no red, no warning mark. The strip: Open ⏎ · Copy · Pin · the arrow (Add to
calendar, Search the web, Ask Gemini). The footer's left end says "AeroDataBox · 13:58" while an answer is
shown. The white light's slow lap starts only if the answer takes longer than 600 ms. No new colour, type size
or row height.

The service's thirteen states and the row's words (from the service's description, **not verified**):
Unknown, or Expected with no new time: "Planned". Expected with a new time equal to the plan: "On time".
Delayed, or a later new time: "Delayed 25 min". CheckIn, Boarding, GateClosed: "Check-in open", "Boarding",
"Gate closed". Departed, EnRoute: "In the air" (with "18 min late" or "on time"). Approaching: "Landing soon".
Arrived: "Landed" (with "12 min late" or "early"). Diverted: "Diverted", the planned landing struck. Canceled,
CanceledUncertain: "Cancelled", "May be cancelled".

| Screen | What it shows |
| --- | --- |
| **12 · Type the number.** | The answered row over a web row; the frame before it (its height, lines and actions from its first frame, "Looking it up"); dark; German („Ab“, „An“); the widest case (twelve-hour clock, a day at both ends, long names) |
| **13 · What the row says, state by state.** | One sheet, eight states on the three guide lines: planned, on time, delayed, boarding, in the air, landed, diverted, cancelled; and the table of the service's states |
| **14 · More, another day, and kept on top.** | The list under the row, drawn as the app opens one (the highlight leaves the row, the arrow turns over, the line carries the Enter mark); `LH455 fri`; the pin at the timer's size in four states beside 2.0's timer, its figure in hours and minutes as words |
| **15 · Without a key, and when it is not a flight.** | The 56 dp row without a key, with the usual route, and with "Get times" armed; `ps5` (a search first, the flight last, nothing sent); the keyword `flight u2 8001`; four no-answer states that keep the row's lines and actions in place; the flight in M1's line and list; the one new tip |

### How it is built, and its size

A reader for flight numbers in `core/`, with a test for every false friend in FL §4's table. The airline table
is an asset made by a script from a public-domain list (a line in `NOTICE`). A provider gives the row; it ranks
under every local match. With a key, a strong match starts one request 400 ms after the last key; a new letter
cancels it. The request, the answer's reader and the two-minute memory are plain code beside
`SuggestProvider`, the only other network code Booklight has; the reader is tested against saved answers. The
key is typed into the Booklight window ("Flights": the key's field with two lines on how to get one, the switch
for the usual route, the month's count), kept in the app's private storage and out of its backup, and sent to
that service only. The row is `Body.Slots` with its note line. Add to calendar is the Event row's effect; the
pin is one more kind of 2.0's pin, with one timer of its own for the lookups while it is pinned. `PRIVACY.md` and the window name the new recipients; Play's data-safety
answers stay as they are (the data type declared for suggestions: FL §5). The footer names AeroDataBox while an
answer is shown; its free plan asks for attribution, and whether that line is enough is **not verified**.

**About 10 days** (not sized by an engineer): the reader, the table and their tests 1; the row without a key
and its ways out 1; the request, the key's field, the states and the memory 3; the three more actions and the
day 1; the pin 1; the usual route and its switch 1; the copy's row, the tip, the list of
everything, German 1; a pass on both devices and the release build 1. The first layer alone is about 2 days.

### To check on a device first

| Check | What | If it goes badly |
| --- | --- | --- |
| F1 | With a real key: LH 455, a short flight in Europe, a codeshare, a number with two legs, one that landed two hours ago. How long an answer takes; which fields come filled at a large and a small airport; whether "revised" is what a traveller calls "expected"; what the reply says when the key's lookups are used up | The row shows less: the times without a gate. The design never shows an empty field |
| F2 | Which flight the service calls nearest at 23:00 and just after a landing | Booklight asks for a day itself: two requests where there was one |
| F3 | The addresses in Chrome on a Googlebook: FlightAware's page for DLH455; whether an installed tracker takes the link | Enter opens a web search for "LH455 flight status" |
| F4 | Not in the half-day session: a day of Alex's own typing with the reader on, in a build made for it. How often a flight's row turns up for text that was not a flight | The rule gets stricter: only after the keyword |
| F5 | The row with the longest names and a twelve-hour clock, English and German, both devices | City names give way to the three-letter codes in line one |
| F6 | What the sign-up really asks for: whether the free plan wants a card | FlightAware's personal key becomes the first choice |
| F7 | The answer from the host of the public route table | No usual route without a key |
| F8 | What the system's own "Track" action for a flight number opens on a Googlebook | Nothing changes: Booklight does not use it |

Also **not verified**: that a user's own key in the app's private storage is what the service's terms allow
(Raycast's extension does the same; the vendor was not asked).

### How we will know it worked

- `LH455` with the key in: a moment after the last letter the row shows both times, the status and the gate,
  and they agree with the airline's own page. How long that moment is, is check F1.
- `ps5`, `mp3`, `h264`, `q4 2026`: no flight in the first row, and nothing was sent.
- Without a key: `LH455`, Enter opens the right page. Nothing left the device before Enter.
- A pinned flight counts down, says "Landed" when it has, and never takes the keyboard.
- After a month of Alex's own use the free plan's lookups have not run out.

---

## 5. The order

1. **2.2,** released when Alex says so (decision 2).
2. **One device session,** half a day, both devices: M1's checks and M5's F1 to F3, F5, F6 and F8. It needs the HP
   awake and attached, and for F1 a key.
3. **M1 · The copy,** released as 2.3.
4. **M5 · Flights,** released as 2.4. Its first layer needs nothing from M1 and can ride in 2.3.

Beside these: **the settings window.** Its build started on 1 October on a branch of its own, on Alex's word
("kick off the settings window redesign"), with the nine recommended answers of `window-redesign.md` §10. It
ships alone once it is built and he has seen it. M1 adds one switch to it ("What you copied"); M5 adds the
"Flights" group.

After these two, the parked three come back in the order they had, unless Alex says otherwise.

---

## 6. Decisions for Alex

The recommended answer is what happens if he says only "go".

| # | For | Decision | Recommended | If he says the other thing |
| --- | --- | --- | --- | --- |
| 1 | All | This plan: 2.2, the device session, M1, then M5; the picture, the selection and your language stay parked | Yes | Name what comes back in, or what goes first |
| 2 | 2.2 | Release what is on main now as 2.2: a GitHub release and a draft on Play's closed testing; production stays empty | Yes | It waits, and goes out with the settings window or with M1 |
| 3 | M1 | A line under the empty field for something just copied, with a switch | Yes | The empty panel stays empty; Tab there opens the copy |
| 4 | M5 | The times, gate and status come through a key of the user's own (AeroDataBox through RapidAPI, free, about 200 lookups a month) | Yes | The row names the airline (and the usual route), Enter opens the page, no time is ever shown; about 4 days |
| 5 | M5 | Booklight asks the service a moment after the last letter, for a strong match, once a key is in | Yes | Only on Enter: nothing is sent while typing, the answer is one key press later |
| 6 | M5 | The usual route without a key, from a public table, behind a switch that starts off, after its host has been asked | Yes, and the first thing to cut | Without a key the row names the airline only; saves about a day |
| 7 | M5 | Pin a flight. A pinned flight asks the service again every 30 minutes with the panel closed, about twenty lookups for a long flight | Yes | Later (saves about a day); or a pin that never asks again and may be wrong by the time it lands |
| 8 | M1 | The translation first for a text not in the user's language, above what was found | Yes, as the one exception | It stands after the found things |
| 9 | M1 | `clip` shows fewer rows | Yes | The eleven rows stay, under the new ones |
| 10 | M1 | Shared text behaves the new way in the released app | Yes | Two ways for the same rows |
| 11 | M5 | Enter on a flight opens its page at FlightAware (its address answered with the right flight every time in the research when built from the airline's three-letter code; FlightStats' did too) | Yes | FlightStats, or a search for "LH455 flight status"; the search is in the row's list either way |
| 12 | M5 | Which flight is "the next": the one in the air now; else one that landed in the last three hours; else the next to leave | Yes | Strictly the next to leave: after a landing the row shows tomorrow's flight |
| 13 | M5 | A flight's row never stands above a local match; a weak match is the last row and sends nothing until the user goes to it. The price: Ryanair, easyJet, Wizz, JetBlue, Austrian, Alaska, Condor and Qatar numbers are weak | Yes | Looser: every known designator is looked up after a pause ("ps5" goes to the service). Stricter: a flight only after the keyword `flight` |

Settled without asking, to be overruled by a word: times in each airport's own time; the status as words,
never a colour; the answer kept for two minutes; the light of work after 600 ms.

A notice, not a decision: with M5 the privacy text names who gets a flight number (the service the key belongs
to, and the host of the route table if that switch is on). Play's form does not change.

**Questions asked of Alex with this plan:** whether "the other improvements your new plan had" means the 2.2
list and M1's own contents; whether he will make the account and paste a key; whether the HP can be attached
for half a day.

---

## 7. Parked

M2 the picture, M3 the selection and M4 your language are `plan.md` §5 to §7, with their designs (screens 5 to
11 on the page) and their decisions under their old numbers (`plan.md` §11: 3 to 8 and 12 to 14). Nothing in
them changed, and nothing in M5 changes them.
