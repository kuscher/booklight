# Interaction review: the copy (re-check), flights, your usual

Seat: interaction designer. Branch `m1-the-copy`, read only. Worked from the code, the 26 stills (`n2-light`,
`n2-dark`) and the films `zM`, `fA`, `mA`, `mB`, `mS`, stepped frame by frame around each change. Frame numbers
are the recorder's (about 60 to the second). Where a finding rests on the code alone, it says so.

## Findings

| # | Severity | Feature | Where | What is wrong | Fix |
| --- | --- | --- | --- | --- | --- |
| 1 | must fix | flights | A flight's row under a copy or under text another app handed over, with a key in. `FlightsProvider.found` and `row`, `OverlayModel.look` | The row stands at 92 dp and says "Looking it up", and nothing is being looked up: `look()` returns for a chip without a keyword, so the request starts only when the user goes to the row (`went`). The sentence is untrue for as long as the user stays on row one. From the code; `mA` cannot show it, because a kept answer stood in the row (frame 208) | `found()` gives the row a weak match gets: `row(it.copy(strong = false))`. It is then the plain 56 dp row, and `gone()` turns it into the full row and looks it up at once on Down, Tab or a click, as for `ps5`. Or keep the 92 dp row and leave its note empty until `answer()` is called |
| 2 | must fix | flights | `strings_24.xml` `set_flight_key_text`, both languages | "is sent with the key to AirLabs ... The key stays on this device." The second sentence contradicts the first. The text also leaves out the one place where Booklight asks with the panel closed, the pinned flight (PRIVACY.md has it) | EN last sentences: "A pinned flight asks again every half hour until it has landed. The key is kept only on this device and goes to AirLabs alone." DE: "Ein angehefteter Flug fragt bis zur Landung jede halbe Stunde neu. Der Schlüssel wird nur auf diesem Gerät gespeichert und geht nur an AirLabs." |
| 3 | should fix | flights | `PinnedFlight.kt`, `AGAIN_MS` (30 min), line 88 | The pin asks every 30 minutes however far off the flight is. `LH455 sat` pinned on Thursday asks 48 times a day, and each ask is two requests (`flight`, then `routes`); a number whose last flight is over costs two or three. The plan promised "about twenty lookups for a long flight" out of 1,000 a month | Before it leaves with more than 180 minutes to go: ask every 3 hours (`wait = 180 * 60_000L`). Inside 180 minutes and in the air: 30 minutes, as now |
| 4 | should fix | flights | Still 10 (light and dark); `Flights.kt` `event()` | A flight that landed 30 minutes ago offers "Add to calendar" as the first line of its list, and the pill lands on it | `event()` returns null for `FlightState.LANDED` and `DIVERTED` too. The list then opens on "Search the web" |
| 5 | should fix | flights | Still 07; `action_get_times`, `action_flight_key`, `flight_refused`, `flight_used_up`, `flight_about`, the tip in `strings_20.xml` line 230 | (a) "Get times" gets no times: Enter opens the window, where an account elsewhere is needed. Its mark is a gear, the fourth icon, and its name shows only after Tab three times. (b) "key" already means a keyboard key in this app ("Give Booklight a key"): "with your key", "Times need a key", "The key was not accepted" read as that. (c) "Open settings" does not say whose | (a) "Set up times" / "Zeiten einrichten". (b) Name it, as the list of everything already does: `flight_about` "Who flies it; with your own AirLabs key, its times, gate and status"; tip "Times need an AirLabs key"; `flight_refused` "AirLabs did not accept the key" / "AirLabs hat den Schlüssel nicht angenommen"; `flight_used_up` "This month's AirLabs lookups are used up" / "Die AirLabs-Abfragen dieses Monats sind aufgebraucht". (c) `action_flight_key` "Booklight settings" / "Booklight-Einstellungen", the name the command has |
| 6 | should fix | usual | Still 04; `Zero.offer` | On an app that holds a seat, "Don't suggest" is the first line of its list in every list of that opening, and `open()` lands the pill on it. "Left third" and the nine lines after it stand one line lower than on every other app in the same list (still 05: Files has it, Feedback has not). Arrow, Enter, Enter used to be "Left third"; on a seated app it now takes the app out of the usual rows | In `Zero.offer`, for a row with actions behind the arrow: `at` = the index of the first dangerous action, else the end (`more = true` stays). It is then the last line before the gap and Uninstall, every other line keeps its place, and the way to it still never passes Uninstall |
| 7 | should fix | usual | `strings_25.xml` `set_usual_text`, both languages; `BooklightApp.usual` (`filter { it.subtitle == null }`) | "the two things you run most from here" is not true in this build: links, recipes and every row with a second line are passed over (cut 1). Someone whose most used things are their links turns the switch on and sees other things, or nothing | Add one sentence until cut 1 is undone: "Your links and recipes are not suggested yet." / "Deine Links und Rezepte werden noch nicht vorgeschlagen." |
| 8 | should fix | usual | `OverlayModel.search`, the blank branch (`val was = current?.id.takeIf { keep && opened == null }`) | Shift + Enter on a line of an opened list in the usual rows: the list closes (cut 3, declared) and the highlight is gone too: `selected = -1`. In a typed list the same keys keep the list open and the pill where it is | `val was = if (keep) opened ?: current?.id else null`, read before `shut()`: the highlight lands on the row whose list closed |
| 9 | should fix | copy | `TranslateScope.rows`, the `known == null` branch | Since the fix for `tr thai ...`, any first word that is not a language is taken for one. `tr guten morgen` offers "In guten", second line "morgen", and Enter sends "Translate this text into guten" to Gemini. The plainest first use of `tr`, a text with no language, gets a wrong reading said with confidence. From the code | Do not name a language Booklight does not know. Title `tr_name` ("Translate"), second line all of what was typed, label Gemini, and hand the whole text over: "Translate this. If its first word is a language, translate the rest into it: {text}" |
| 10 | should fix | copy | `Field.kt`, the mark's `SizeTransform` and the text field's caret | The chip appears in its place without a slide (holds), but the caret rides through it: `mB` frames 160 to 163 show the caret on "treffen", "wir", "uns" inside the chip before it reaches its place at 164; `mS` frames 230 to 239 the same, four times slower | For a chip nobody typed, the chip's room is taken in the frame it appears: `SizeTransform(clip = false) { _, _ -> if (typed) motion.place() else snap() }`. Or keep the caret at alpha 0 for the 160 ms the placeholder already waits |
| 11 | should fix | flights | `fA` frames 189 to 197; `RowSlots.sync` keys rows by id, and a flight's id holds its number (`flight:LH45`, `flight:LH455`) | Each digit makes another row. Frames 195 to 197: "LH 45 · Lufthansa" and "LH 455 · Lufthansa" are printed over each other inside the highlight, and the selected row is nearly empty for three frames: its text arrives late over the selection. Frames 189 to 194 (field "LH455"): the list is 'Search Google for "LH45"' with a faint "LH 4 · Lufthansa" under it, rows of two earlier texts. Frame 194 to 195: the web row drops 83 px (56 dp) in one frame and the pill's lower edge 32 px. (Typed at one key per 3 to 6 frames, faster than a person) | One seat for the typed flight row: in `RowSlots.sync`, look a row of the provider `flights` up by the provider, not by its id, so the row stays and its words change in the frame of the key. The motion seat should trace the one-frame drop |
| 12 | could | copy | `Languages.find`; `tr_lead` German has "ins" | "ins" asks for the noun's form with an e: "ins englische", "ins dänische". `find("englische")` is null, so the words become an instruction instead of the translation's row | In `find`: `val w = bare(word).let { if (it.endsWith("sche")) it.dropLast(1) else it }` |
| 13 | could | flights | Still 11; `SearchEngine.merge` | "A guess is the last row" is not true with suggestions on: "PS 5 · Ukraine International Airlines" is row two of five, between the web search and its three suggestions | Either put suggestions before a trailing guess in `merge` (the guess then moves down as they land), or say in ux-model §16 that it is the last local row |
| 14 | could | flights | `Flights.make`, the `day(rest)` return; `Days` WEAK words | Typing a day takes the answered row away for a key: `LH455 s` is no flight (the list is a web search), `LH455 sa` is Saturday's row. `LH455 mo` on the way to `morgen` is Monday's flight, and after 400 ms two requests are spent on it | While what follows the number is one letter, keep the number's own row (day = null). Take the two-letter day words only when a space or the line's end follows them in a text that is complete: simplest, require three letters after a flight number |
| 15 | could | flights | `FlightsProvider.rows`, the empty row under the keyword with a key in | Its caption is `flight_hint`, the same sentence as the field's placeholder 60 px above it, and the row is highlighted with nothing Enter can do. From the code (still 12 has the number typed) | Caption null: the two labels and their dashes are enough to hold the height |
| 16 | could | flights | A row that says "No connection" or "No answer this time" | The only way to ask again is to change the text: `fresh()` forgets the failure after 10 s, but nothing calls `row()` again | In `went()`: a row whose kept failure is OFFLINE or NO_ANSWER and older than `RETRY_MS` is looked up again when the user goes to it (Tab or a click on it) |
| 17 | could | usual | `OverlayModel.offerZero(last = true)` with the switch off | It still reads the clipboard's description a second time (`fresh()`), 320 ms after the gate, when `offerCopy` has just said no. The spec says none of this runs with the switch off | First line: `if (!settings.zero) { zeroOver = true; return false }` |

## The copy: what the fixes changed

- The line fades where it stands: holds. `mB` 160 to 162, `mS` 227 to 230: the words fade in row one's seat, the
  mark stays one frame longer (`mB` 162), "In English" comes under it.
- The pill comes whole: holds. `mS` 227: full width at once, faint, never a sliver.
- An asked row grows with its pill and changes in a fade: holds. `mS` 339 to 351: the pill's lower edge and the
  text go together, the name and the question cross in a fade, "Copy" comes after "Ask" has gone.
- The chip appears without a slide: holds (`mB` 160, `mS` 230), with finding 10 (the caret).
- The placeholder is not cut: holds. `mB` 168 to 172, `mS` 248 to 260: "What to do with it" fades in whole.
- Tab after typing: `Panel.keys` sends Tab to `tabCopy()` whenever the field is bare, and `tabCopy` looks with
  `always = true`. Right. One corner, as the spec says: once the usual rows stand, Tab is Down.
- Held Backspace: `field.text.isEmpty() && (again || model.back())`: a repeat is swallowed, one press is one
  step. Right. No film holds a key.
- Lead words: "in", "into", "to" and "auf", "ins", "in", "nach" are dropped before one language word. Right,
  with finding 12.
- `tr` with an unknown language: finding 9.

## Flights: every state, and what the keys do

| State | Row | Enter | Tab / Right | Arrow's list | Sent |
| --- | --- | --- | --- | --- | --- |
| No key | 56 dp, "LH 455 · Lufthansa" | Open (FlightAware) | Search, Ask Gemini, Get times | none | Nothing |
| Waiting | 92 dp, two dashes, "Looking it up" | Open at once. On Copy, Pin, Add to calendar: runs when the answer is in | Copy, Pin, arrow | Add to calendar, Search the web, Ask Gemini | One lookup, 400 ms after the last key |
| Answered | times, status, where to go; footer "AirLabs · 1:46 AM" | Open | Copy, Pin, arrow | the same three | Nothing more for two minutes |
| Landed | "LEFT", "LANDED", "Landed 9 min early · Terminal 1, belt 3" | Open | the same | Add to calendar still first (finding 4) | |
| Failed | the plain sentence where the status stood | Open | Copy and Pin dimmed and passed over; refused key: "Open settings" in their place | Search the web, Ask Gemini | Not again for two minutes (10 s for no connection, but see 16) |
| Weak, before | last local row, 56 dp, no sign that it can answer | Open, if gone to | | | Nothing |
| Weak, gone to | becomes the 92 dp row under the pill | as waiting | | | One lookup at once |
| Keyword | chip "Flight"; with a key an empty row (finding 15) | nothing until a number | | | Every text that reads as a number, after 400 ms |
| A day | "LEAVES SFO Sat 2:40 PM", "Planned · from the timetable" | Open (the live page, not that day's) | as answered | | Two requests |

Nothing is sent that the design did not allow. What the user is not told: the pin's asking (finding 2), and
that it costs more than promised (finding 3).

## Your usual: the key table of zero-state.md §3 against the code

Walked every cell. They match: Enter and Shift + Enter at rest do nothing and nothing is parked (`chosen()` is
null); Down fades the pill in on row one; Up from row one goes to rest and Up at rest brings the last text
back, each on a press of its own (`up(again)`); Tab at rest is Down on the first press; Shift + Tab, Left,
Right and Backspace at rest do nothing; Ctrl + digit runs the row's first action; a letter is typed at once and
the rows stand until the list lands (`zM` 258 to 262), with Files keeping its row and the pill; Space keeps the
rows and puts the highlight back to rest; the field emptied again brings the same rows at rest (`zM` 336 to
346, still 06); the pointer needs 4 dp on a row (`RowFrame`, `calm`); a click runs the first action; "Don't
suggest" is run by Enter only, never by an arrow or Ctrl + digit, and nothing is counted as a run. The one
cell that differs is Shift + Enter in an opened list (cut 3, and finding 8).

With the switch off: no job for the rows, no early offer, `search()` gives an empty list for an empty field, no
"Don't suggest" anywhere. Two things do change for everyone: what counts as a run (declared), and finding 17.

## What a first-time user would not understand

- Usual rows: why Enter does nothing. Every other list has row one selected. Only "↓ Choose" in the footer says
  so, in the far corner from where the eye is.
- "Don't suggest" in a typed list (`files`, arrow): it does not say what is not suggested where, and the footer's
  "Won't be suggested" does not say where to undo it.
- The gear at the end of a flight's row without a key, and the word "key" (finding 5).
- With a key in, a weak row for a real flight (`FR 1234`, `U2 8001`: Ryanair, easyJet) looks like a row without a
  key and stands last. Nothing says that going to it brings the times. One more clause in the guide's line
  would do: "Ryanair, easyJet and a few others: `flight fr1234`".
- The times are each airport's own. "LANDED FRA 10:16 AM" at 1:46 AM by the user's clock reads as the future.
- "Planned · from the timetable": that no gate and no delay will come for that row.
- "AirLabs · 1:46 AM" is when the answer was given, not a time of the flight.

## Verdicts

- The copy: approved (no must-fix; 9 and 10 should follow).
- Flights: approved with the must-fixes (1 and 2).
- Your usual: approved (no must-fix; 6 and 7 should be done before the switch is on for anyone else).
