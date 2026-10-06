# Changelog

## 3.2 (6 October 2026)

**An event from a sentence** (`docs/design/event-sentence/design.md`).

- **Type an event the way you say it.** "Add dinner with Sam tomorrow at 7pm to the Team calendar": one row
  shows what Booklight understood (when, the title, where, the calendar), and Enter opens the Calendar app's
  editor, filled in, on that calendar. In any order. With the keyword `event`, or without it where the line
  begins with add, schedule, put, book or plan („trag … ein“, „plane“, „neuer Termin“) and holds a day or a
  time (both, after schedule, put, book and plan: those also begin everyday searches, and such a line always
  opens the editor); such a row stands below anything of your device that matches and above the web. An event
  that names a calendar goes to Google's Calendar app as a calendar link, which alone can say which calendar;
  one that names none goes as the request Booklight has always sent.
- **The day and time are read anywhere in the line**, and more of them: `tmrw`, "on the 12th at 6:45 in the
  morning", "7 in the evening", "12 at night", "from 9am to 10am", "Oct 14 to Oct 16", "all day"; in German „von
  9 bis 10 Uhr“, „am 12.“, „vom 14. bis 16. Oktober“, „ganztägig“, „7 Uhr abends“, "the day after tomorrow",
  "Wednesday 28 October". The row says the year wherever it is not this year's. What leaves something open is
  read and shown as a guess, a step lighter: a time that does not say its half of the day ("at 7", "7:30",
  „um 7“; „9 Uhr“ and "09:30" say it), "every Monday" and any other word for a repeat ("weekly"), a line that
  holds a second day or time, "next Tuesday" (never today, and people mean two days by it), a date without a
  year that had just passed, "12/10", midnight beside a day, and a word beside the day that changes it ("the day
  before Friday", "not tomorrow").
- **Your calendars, if you allow it**, in the Booklight window: Privacy › Calendars, "New events go to", or the
  switch below. Booklight then reads the list of your calendars (names and colours, never an event): name one in
  the sentence, with "the" or without, its start is completed in grey and Right takes it, and a Calendar stop on
  the row lists them.
- **The model on your device helps to split.** Where the rules cannot tell a place from the title, the model is
  asked a moment after you stop typing, and the words in the row change where they stand. It works out no date,
  nothing waits for it, and nothing it says is shown unless you typed every word of it.
- **Save without opening Calendar, if you switch it on.** Booklight window › Results › "Save events without
  opening Calendar", and "New events go to". Enter then saves the event the row shows, all-day events too, into
  the calendar you named, else the one you chose there, else your account's own; "Open" is the second action, and
  the row's caption says what Enter does on the one that is armed. A guess is never saved: where the day or the
  time is one, a calendar's name is still being typed or is none of yours, or the line was typed without the
  keyword and begins like an everyday search ("book flight to boston friday 9am"), Enter opens Calendar and the
  footer says why. An event that was saved is synced by the calendar's own account, not by Booklight.
- Two new permissions, both asked for only in the Booklight window: to read the list of calendars, and (with the
  switch, which asks for both) to add events. `PRIVACY.md` says what each does, and that the address of the
  calendar chosen under "New events go to" is kept in Booklight's settings, which are in your Android backup.

## 3.1 (5 October 2026)

**First steps** (`docs/design/first-run/`; `BUILD.md` there says what was built and what was decided on the way).

- **A welcome, once.** A new installation's very first opening is a slow one: the glass grows tall and goes to
  night, a lamp strikes at the caret, and "Welcome to Booklight" stands in its light with a small Swiss Army
  knife. Then Booklight shows off with its own panel: your apps, a sum, an example flight, the emoji grid. It
  runs nothing and sends nothing. Enter or Space in the welcome begins the show; a typed character ends it and
  is simply typed; every other key sets the key's step down at once (a key that types nothing by itself, such
  as Shift, does nothing). Not played with the system's animations off, with a screen reader on, or on a low
  screen.
- **Give Booklight a key.** Under the empty field, where a card stood: Action + Quick Insert is suggested
  (Action + M on a keyboard without that key), "Open Keyboard shortcuts" opens the system's dialog on a page of
  Booklight's own with the five steps, and the panel waits behind it. Press your new keys: the dialog goes, and
  "Your key works". An installation that was there before and has no key yet is shown this step alone.
- **Three things to try**, each once and for practice: open an app, search inside one, a sum. Nothing opens; the
  footer says what Enter would have done, and the sum is copied.
- **One question**: may searches be suggested as you type? Nothing is armed, "Not now" comes first, and only
  "Agree" switches suggestions on. Then two choices: "Show your usual", and the list of everything.
- **It never nags.** Esc leaves at any point; an unfinished run stands in three openings and then waits in the
  Booklight window, and the bare field says so once. "Not now" on the key ends it.
- **First steps, again**: type `first steps` in the panel, or choose First steps on the window's Start page,
  which plays the welcome too (not where the system's animations are off or a screen reader is on, nor on a low
  screen). Where the steps were left unfinished, both go on where they stopped. It changes no switch.
- **The screens are set down, not shown**: caps come up a beat apart, a recipe is written a letter at a time,
  an answer shows as pressed, the key that lands is held down on the glass and comes up with its check, and the
  run ends in one lap of light round the bare field. A screen takes a key only once the answer it would run has
  been in view for a moment. With the system's animations off everything stands at once.
- The window's Start page and the README tell the same way to a key. The two cards under the empty field are gone.
- Nothing new is asked of the system: no new permission, nothing in the background, no new connection.

## 3.0 (2 October 2026)

**A flight's row shows the flight** (`docs/design/flights-row/`; with your own AirLabs key, as before).

- **A line with the plane on it.** The row is taller. Its large line says the one thing you came for: "Leaves Fri
  10:55 AM", from three hours before "Leaves in 42 min", in the air "Lands in 4 h 07 min", then "Landed 12 min
  ago". Under it the flight is a line from take-off to landing: the part that is flown is solid, the rest dotted,
  and the plane stands where the flight is. Under the line's two ends are the airports with their times, each in
  its own clock, and what matters there now: gate and terminal before it leaves; after, the arrival's terminal
  (and belt, where the service names one) and the aircraft's type.
- **One badge says whether it runs to plan**: "On time" and "24 min early" in green, "Delayed 27 min" and "27 min
  late" in amber, "Planned" when only the plan is known. The colour is only in the badge. "Late" now starts 15
  minutes after the plan, as airlines count it (it was 5). A cancelled flight is the word "Cancelled", both times
  struck through, and no plane.
- **The plane's place is worked out from time**: how much of the flying time has passed, not a place on a map.
  While the panel is open it moves on with the clock, a step a minute, and the minutes count with it, without
  asking the service again. When nobody knows where a flight is (it is cancelled, or only the timetable knows
  it) there is no plane. What the service does not send (a gate, a belt, the aircraft) is simply not shown.
- **The pinned flight** has the same line, small, under its countdown, and says "on time" or "27 min late" as a
  word in colour.
- Nothing more is sent than before: the same one lookup per flight. Without a key the row is the plain one it was.

**One way to do things with an app** (`docs/design/app-structure/`): type the app, Tab to what you want, Enter;
if it takes words, type them and press Enter again.

- **An app's row is Open, Search, Play, Window and the arrow**, in that order for every app. An app shows only
  what it has: a calculator has Open, Window and the arrow; Netflix has Search second; Spotify, with your key
  in, has Play third. Two to four icons and the arrow, where there were six.
- **Tab only moves; Enter does.** Tab goes along the row and enters nothing. (It still types into a row that
  offers nothing else, such as "Search YouTube", and makes an exact keyword the chip.)
- **Search inside an app.** `spo`, Tab, Enter: Spotify is in the field, with its own icon. Type what you are
  looking for; Enter opens the app on its results. Nothing leaves the device before that Enter.
- **Play in a music app.** `spo`, Tab, Tab, Enter, then the song. The row shows what Spotify found (the song with
  its artists and album) and Enter plays exactly that; Enter before the answer has come waits for it. Tab changes
  between Search and Play for the same words. An app has Play only if it really plays what you name: Spotify by
  itself only shows its search results, so its row has Play once your own Spotify key is in (the Booklight
  window › Labs), and Search without it. The footer says "Sent to", never "Playing".
- **Backspace on the empty field** goes back to where you pressed Enter: your letters as you typed them, the
  app's row selected, the action you left still armed.
- **Window** opens a list under the row: New window, Maximise, Left half, Right half, then the ten other
  places. **The arrow** opens App info, the app's Notifications, Language, Open by default and Battery use in
  Settings, and Uninstall. Typed words work as before: `chrome left`, `files right third`,
  `spotify notifications` (`benachrichtigungen`, `sprache`, `standard`, `akku`), `chrome uninstall`. Booklight
  opens a page of Settings; it switches nothing.
- **`play bohemian rhapsody`** is a short way to the same place: at the space, `play` becomes the music app you
  played in last, with Play armed. Start with `album`, `artist`, `song`, `playlist` or `genre`; "X by Y" names
  the artist; `… on spotify` at the end chooses the app. With two music apps the other one stands under it.
- **`yt`, `maps`, `store` and `drive`** become their app, with Search armed, where the app is installed; "On the
  web" waits behind the row's arrow. Where the app is missing they open in the browser, as before.
- **The app's name and the words in one go** (`netflix severance`) is a web search first, with "Search Netflix
  for …" directly under it. Pick that row once and Netflix comes first for words after its name; pick the web's
  row twice running and the web comes first again. With suggestions on, such a text is sent for suggestions
  only while the web comes first.
- **"Which song is this?"** behind the row's arrow, under a music app that plays, asks the model on this device
  for a song you can only describe or cannot spell. Its answer is shown; "Use this" puts it into the field. It
  never changes what you typed by itself.
- **`play` alone resumes** (Enter on the Media row); `pause` can only pause, `stop` stops. `play` is a recipe step.
- **No longer icons on the row:** New window, Maximise, Left half and Right half are the first lines behind
  Window, and App info is the first behind the arrow. Typed (`chr new`, `chr left`, `chr info`) they are as
  quick as before. An app's own shortcut that is only called "Search" is no longer a row of its own where the
  app has Search on its row.
- Where a search goes: the app's own Booklight file, a small bundled table (`app/src/main/assets/appsearch.tsv`),
  or a search the app declares. Shown only if the installed app takes it.

**Commands for other apps** (`docs/research/next-five.md`; what the devices showed: `docs/research/intents.md`, "Tried on a Googlebook").

- **`alarms` and `timers`** open the Clock's two lists.
- **`go`**: `go hamburg hbf` is directions from here; `go berlin to hamburg by train` both ends and how (`von
  berlin nach hamburg mit dem zug`, `zu fuß`, `mit dem rad`, `mit dem auto`). In the Maps app where there is one,
  else Google Maps in the browser.
- **`meet`** starts a meeting; `meet abc-defg-hij` joins that one.
- **`call`, `sms`, `wa`, `tg`**: a number (or a Telegram name) and your text open the phone app's dial screen, a
  new message, or the chat with the text in its field. Booklight dials nothing and sends nothing. Each keyword is
  there only where an app answers it. `go`, `wa` and `tg` typed alone stay under the apps that start that way.
- **A link can open an app.** A link's address may have any scheme: `spotify:search:{argument}` searches Spotify,
  a `spotify:playlist:…:play` link plays it. Not taken: `javascript:`, `file:`, `content:`, `intent:`, `data:`.
  When no app answers, the row stays and the footer says so.
- **An app's address, typed.** `spotify:track:…` in the field is a row "Open in Spotify". Words with a colon stay
  words.
- **App commands**, a fifth kind under Commands › Yours: choose an app and what to ask it (open a link, send
  text, search, play from search, or an action of its own), with up to eight extras. `{argument}` is what you
  type after the keyword. Paste an `am start …` line to fill the form; Try asks the app now and says what
  happened. Booklight only starts an activity the app lets other apps open. A recipe step can be one.
- **Labs**, a sixth section of the Booklight window, for what needs a key of your own from another service:
  the Spotify key, and the flight service's key, which moved there from Results.
- No new permission in any of it.

- **A letter typed right after a keyword's space is no longer lost.** `fix teh text`, typed quickly, could arrive as
  `eh text`: the keyboard's keys went through the input method, and the letter it was still holding was dropped when
  the keyword became the chip. Typed keys now go to the field directly (`docs/research/device-findings.md`, "Typed
  keys go through the input method"). The code was the same since 1.1.

- **The highlight moves like rubber.** Going from one row to the next, its front edge goes first; the old edge
  holds on for a moment and then follows, so the highlight stretches over both rows and gathers itself on the new
  one. It is at rest sooner than before. Hold an arrow key and it runs down the list in one piece instead of
  falling behind and getting long; over many rows it is never more than a row longer than its own. The square in
  the emoji and letters grids moves the same way (`docs/design/rubber-highlight.md`).

## 2.3 (2 October 2026)

**The copy** (M1 of `docs/design/milestones/plan-next.md`).

- **One line for a fresh copy.** Copy something, open Booklight within two minutes, and one line under the
  empty field says what it is: "Copied 20 s ago · a link and a date". Booklight reads only the system's
  description of the copy for that, not its text, and Android shows no "pasted" message. No line for a copy
  marked private or one Booklight made. A switch in the window turns it off.
- **Tab opens it.** The copy becomes the chip. Under it: a link (Open, or Copy clean without its tracking tail),
  a date as an event filled in, a phone number, a mail address, a flight number (its row names the airline;
  it is looked up only when you go to it); then your first three prompts, and Summary for a
  long text. A text that is not in your language has its translation first.
- **Answers where they stand.** Enter on a model's row writes the answer into that row and the chip stays the
  copy. Enter again copies. Backspace on the empty field steps back.
- **Into any language.** Under the copy's chip, type a language: `danish`. From the empty field:
  `tr danish see you on Saturday`.
- **Say what to do with it.** Under the copy's chip, anything that is not a row's name or a language is an
  instruction for the model: `pull out the tasks as a list`.
- **`clip`** shows the same, shorter list (the other rows are found by typing their names) and no longer reads
  a copy marked private. Text another app hands over behaves the same way.
- **The model on the HP Googlebook answers.** The system calls it "downloadable" there although it has it;
  Booklight now tries it before handing over to Gemini.
- A long text is never returned cut: a rewrite or translation of more than about 1,800 characters hands over
  to Gemini.

**Flights** (M5 of `docs/design/milestones/plan-next.md`). Tried on both Googlebooks with a real key.

- **A flight number is a row.** `LH455`, `lh 455`, `LH0455` and the callsign `DLH455` name the airline, from a
  table of 1,375 airlines Booklight carries (`tools/airlines.py`, public domain). Enter opens the flight's page
  at FlightAware; the other actions search the web and ask Gemini. Nothing leaves the device before Enter.
- **With a key of your own it answers.** Put a free AirLabs key into the Booklight window › Results › Flights
  and the row shows who flies it and where, when it leaves and lands in each airport's own time (with the day
  when it is not your today), the status in words ("Delayed 25 min", "In the air · 18 min late", "Landed"), and
  where to go: terminal and gate before it leaves, the arrival's terminal and belt after. One lookup, 400 ms
  after the last key; the answer is kept for two minutes; the footer says who gave it and when. Booklight ships
  no key.
- **The next flight.** In the air, or landed under three hours ago, or the next to leave. A day after the number
  (`LH455 fri`, `LH455 tomorrow`) is that day's, from the timetable.
- **What only looks like a flight** (`ps5`, `mp3`, `ms 365`, `q4 2026`, `win 11`) is the last row, and nothing is
  sent for it until you go to it with Down or Tab. `flight u2 8001` (German `flug`) makes anything a flight.
- **Copy, Pin, and three more behind the arrow**: Add to calendar, Search the web, Ask Gemini. A pinned flight
  counts down to its take-off, then to its landing, then says "Landed". It follows that one flight, and asks
  about it again every half hour from three hours before it leaves until it has landed.
- **When there is no answer** the row says why in plain words (nothing found, the key was not accepted, its
  lookups are used up, no connection) and keeps its place; Enter still opens the flight's page.
- Not in it: the usual route without a key (its table's host has not been asked yet).
- For developers: `core/Flights.kt` (the reader, and which matches are strong), `core/AirLabs.kt` (the replies,
  and which requests "the next flight" takes), `core/FlightStatus.kt` (the words); `./bl debug pref flightkey KEY`
  and `./bl debug flight LH455`. `Action.off` (an action that keeps its place, dimmed) and four more fields on
  `Body.Slots` are new in the model.

**Your usual** (issue 1, `docs/design/zero-state.md`). Behind a switch that starts off: the Booklight window ›
Your usual › Show your usual. With it off, tips come as before.

- **Two or three rows under the empty field**: the two things you run most from Booklight, and the one you ran
  last in the past eight hours. Never one row; none until two things have been run twice each. They are ordinary
  rows, with nothing selected: Enter does nothing until Down (or Tab) brings the highlight to row one.
- **They hold their seats.** What had seat one or two keeps it until something else weighs a fifth more, so
  "Down, Enter" means the same thing from one opening to the next.
- **A fresh copy has the place first**: its line comes, and the rows do not, for those two minutes.
- **Don't suggest** on each of the rows (in an app's list, the last line before Uninstall) takes a thing out for good;
  "Suggest everything again" in the window brings all back.
- **What counts as a run**, for every list: opening a thing or doing its thing. App info, Edit, Delete and
  Uninstall no longer lift a row's rank.
- Also for every list: a row that leaves while it is still rising no longer jumps, and what is under the field
  waits for the glass's edge while the panel opens.

**The Booklight window is a desktop window** (`docs/design/window-redesign.md`; Alex: the settings app "looks very
weird on a Googlebook … more desktop ready and use material 3 expressive … The left nav should be at the left edge").

- **Navigation on the window's leading edge**: Material's navigation rail, 220 dp wide with each name beside its
  mark from 840 dp of window, 96 dp with the name under the mark from 600, and a navigation bar along the bottom
  under 600. One indicator travels between the sections. The page's column starts at the rail and is at most
  720 dp wide; nothing is centred any more. The caption bar is see-through. The window is at least 400 × 480.
- **Five sections**: Start, Commands, Look, Results, Privacy. Yours is the other half of Commands (Built in ·
  Yours), with Find (Ctrl + F, or just type) and New link, whose arrow offers snippet, recipe and prompt
  (Ctrl + N). Access and About are Privacy. The assistant key is on Start. The window opens where it was left.
- **Rows and controls are Material 3 Expressive**: grouped rows, switches, connected button groups, a menu for the
  search engine, Material's text fields and buttons in the editors. Every row has a mark; text, and the controls
  at the rows' ends, each share one line. Hover and pressed states, one focus ring that glides, right-click menus
  (Try it, Copy example; Edit, Duplicate, Delete; Show, Hide), tooltips, a scrollbar that can be dragged.
- **Keys**: Tab and Shift + Tab through everything, F6 between navigation and page, arrows, Home, End, Page Up,
  Page Down, Ctrl + 1 … 5 for the sections, Delete on one of yours (asks once more), Menu or Shift + F10 for a
  row's menu, Escape clears Find, Ctrl + W closes the window. Left and Right step a choice; a switch is flipped
  by Enter or Space, and Left on it goes back to the navigation, as on every row. The window's keys are in the
  system's Keyboard Shortcuts Helper.
- **A choice is changed only by its buttons**: a click on the row beside them does nothing.
- **The editors**: what you typed stays when the window is resized. An editor you changed asks once before it
  is left ("Press again to discard"). A line says why Save cannot be pressed yet; Enter saves. After Save the
  keys are on what you saved.
- **Forget everything** also forgets the recent emoji and letters and which commands were used.
- **A second pane from 1332 dp**: the live demo on Start, a preview of the panel on Look (it shows the theme, the
  colours, the glass, the shadow and the dimming as they are chosen), the editor beside the list on Commands.
  Below that width the demo and the preview are at the top of their pages, and an editor opens under its row.
- Look's explanations are one line each.
- **The night's new settings in it**: "Your usual" and "What you copied" on Start, after Tips; "Flights" on
  Results, with a field for your AirLabs key (pasted, saved with Enter; never shown again; Take out asks twice)
  and a row that opens where a key is got.
- New library: `androidx.compose.material3.adaptive` 1.3.0 (window size classes). No new permission.

## 2.2 (1 October 2026)

Alex's notes on 2.1, each taken through a visual and a motion designer (`docs/design/design-system.md` §12).

- **The opening is at Medium speed** unless chosen otherwise; whoever had Fast stored (everybody) gets Medium once.
- **The opening's curve**: the glass opens slowly, quickly through the middle, and lands over a long stretch, with
  no rebound. Leaving takes 155 ms at Fast and Medium alike.
- **The glass is frosted from its first frame.** The blur used to arrive at the end of the opening, all at once.
  It now belongs to the glass: there from the seam on, the same shape as the glass in every frame, and with it
  until the seam is gone when the panel leaves.
- **The seam the panel opens out of is seen again**: since 2.0's shadow nothing showed until the glass widened.
- **The light on the edge is a white reflection**: it comes 2.4 s after the panel has opened, in a quiet moment,
  runs once round from the middle of the top edge, and gives way to any key. The model's "thinking" light is the
  same light, steady and dimmer.
- **An app's row has six icons**: Open, New window, App info, Maximise, Left half, Right half; the other places
  are in the list under the arrow. "Full" is "Maximise"; "Left ⅔" is "Left two thirds".
- **The selection in dark theme is see-through** (the same colour at 42 % on glass).
- Found, not changed: Chrome's "New tab" is closed to other apps (`docs/research/device-findings.md`).

## 2.1 (1 October 2026)

- **Letters** (`abc`): the letters of sixteen languages in the picker's grid, to copy. By language
  (`abc german`, `abc dansk`; the start of the name is enough, in English, German or the language's own name),
  by plain letter (`abc a`, `abc A` for capitals), by the two letters it is written with (`abc ss`, `abc ae`),
  by its mark (`abc umlaut`, `abc acute`, `abc o slash`), or the letter itself for its other case. Nothing
  typed: the ones picked lately, then every small letter. Its row is also found by `umlaut`, `accent`,
  `letters`. `core/Letters.kt`; marks and plain letters come from Unicode's decomposition, not a hand-kept list.
- **The calculator reads a decimal comma.** Where the system's language writes one (German, Danish, French…)
  `1,5` is one and a half, `1.000` a thousand, and answers are written and copied that way. In any language the
  other sign is understood where it can only mean one thing (`12*3,5` in English, `12*3.5` and `0.125` in
  German), and with both signs in a number the last one is the decimal sign. The German examples use a comma again.

## 2.0 (1 October 2026)

What Alex chose from `docs/research/next-features.md`, plus a shadow, tips, `s` and `k`, and answers from the
device's own model. Spec: `docs/superpowers/specs/2026-10-01-booklight-2.0-design.md`.

**Prompts, answered on this device**
- A prompt is a name, a keyword and a text. Five to start with (`fix`, `shorter`, `de` or `en`, `sum`,
  `explain`); your own in the window under Yours. `{text}` says where the text goes.
- In a prompt's chip the row shows the text, and after a pause in typing the answer of the system's own model
  (Gemini Nano through AICore, where the device has it) is written into the row; the row grows once, to four
  lines. Enter asks at once, and again copies. Pin and Open in Gemini beside it; Replace when the text came
  from a field that can be edited. Nothing typed: what you copied.
- Without the model the row hands over to the Gemini app; where the system can fetch the model, a row asks it to.
- The model can be wrong: its rows never run anything.

**Pin**
- A small window that stays on top: a line of text, a sum's answer, a colour, a QR code, a countdown.
  `pin TEXT`; Pin on those rows; Start and pin on a timer. `pin` alone shows it, with Unpin and Copy.
- It is a picture-in-picture window, so it never takes the keyboard (a window in Android 17's pinned layer
  does, each time the panel closes: `docs/research/device-findings.md`). No permission.

**Finding what Booklight does**
- `?` first is the list of everything, with an example for each. Enter on a keyword's row makes it the chip;
  on any other row Booklight types the example, a letter at a time.
- Tips under the empty field: only after the panel has opened and nothing was typed for a moment. Tab arms
  Try it, then Turn off tips. One pass.
- The window's Commands page is the same list; Enter opens the panel and types the example.

**Apps and places**
- Left, middle and right third, the two thirds, the four quarters, centre and full. "… on display 2".
- A row shows nine actions; the arrow (More) opens the rest as a list under it. A typed place that is among the
  rest takes the arrow's slot. Store page is gone.
- Other apps' commands: their manifest shortcuts and a small file of their own (`docs/EXTENSIONS.md`).

**Keywords**
- `s`: settings pages (49). `k`: the system's keyboard shortcuts (43), shown as key caps.
- Tab on a keyword makes it the chip. The row of a one-letter keyword keeps the last local place.

**Notes**
- `notes TEXT` finds lines; `todo` adds, lists and ticks tasks in Todo.md; `note FILE text` writes to another
  file of the folder; Today's note.

**The window, and the look**
- A column of sections and one page. "No key yet" until a key has opened the panel.
- A shadow around the panel: off, low, medium (the default), high. Never under the glass.
- The closing draws in to the field's centre line; the footer rides the window's lower edge.
- Two old flaws gone: a list that is emptied fades (it was cut in one frame), and a panel that shrinks no
  longer shows a strip of blur without glass under it.

**Under the hood**
- `com.google.mlkit:genai-prompt` 1.0.0-beta4, as shipped: two install-time permissions and Google's usage
  reporting (not the text). `PRIVACY.md` and the data-safety note say what.
- Settings schema 3: prompts are seeded; whoever used 1.1 is taken to have a key.

## 1.1.1 (1 October 2026)

- The panel closes the way it opens, backwards: the glass folds to a line from both sides, and the line draws
  in. At the speed chosen for the opening; with the opening turned off it fades as before.
- The key pressed again while the panel is closing opens it again from where it had got to.

## 1.1 (1 October 2026)

Several planned releases in one (Jot, Dials, Recipes, Switches), on a new way of acting on rows.

**Acting on a row**
- The selected row shows everything it can do as a row of icons. One is armed: it is unrolled to its name
  and the Enter mark. Tab and Shift + Tab, or → and ←, move the arming; the highlight glides and the names
  unroll with it. The separate actions view of 1.0 is gone.
- Typed verbs arm an action on an app's row: "chrome uninstall", "uninstall chrome", "chrome inf".
- Apps gain New window, Left half, Right half and Uninstall (Android asks before it removes anything).
- A keyword and a space, or Tab on its row, turns a keyword into a chip in the field: `yt`, then what to
  search for. Backspace on an empty field turns it back. The last row is always "Search Google for" all of it.
- Deleting something you made needs Enter twice.

**Jot**
- `mail anna@x.com Lunch? / See you at 1`: your mail app's compose window, filled in. Nothing is sent.
- `note buy milk`: a dated line in Notes.md, in a folder you choose once. Or Keep.
- `event Fri 3pm Dentist @ Main St`: the calendar's editor, filled in.
- `remind 5pm call bank`, `remind in 20m stretch`: a Clock alarm or timer with your text as its label; a
  calendar event when it is more than a day away. `timer 10m tea`, `alarm 7:30 gym`.
- `new file ideas.md`, `new folder Projects`, `new doc Q3 plan`.
- Ask Gemini: for a question or a longer text, the Gemini app opens with your text in its prompt. `ask …`.

**Dials**
- `vol`, `brightness`: a level you change with ← and → without the panel closing. `vol 40` sets it.
- `pause`, `next`, `previous`; `play lofi beats` asks your music app.
- `emoji party`, `sym arrow`: a grid to pick from, in English and German. `#3478f6` and `color …` in HEX,
  RGB, HSL and OKLCH. `qr …`. `password 20`, `uuid`.
- `gh owner/repo` and `gh owner/repo#12` go straight there; `:3000` opens localhost.

**Your own**
- Links with `{argument}`, `{clipboard}` and `{date}` (1.0's keyword searches still work), snippets
  (`snip sig`, `snip add sig / Best, Alex`) and recipes: several steps under one name.
- `clip`: what you copied, as rows (search it, ask Gemini, a note, a QR code, UPPERCASE and others).
- Text from other apps: "Booklight" in the text-selection menu and the share sheet.
- A search-pill widget, a Quick Settings tile, and Booklight as the digital assistant if you choose it.

**The panel**
- It unfolds from a line when it opens, at a speed you choose (or not at all), and a light runs once
  around its outline.
- Theme (auto, light, dark), colours (your wallpaper's or Booklight's own), and Solid besides Clear,
  Balanced and Frosted. The screen behind is no longer dimmed unless you turn that on.
- The app's icon opens a Booklight window: what it is, how to give it a key, every setting, and editors
  for your links, snippets and recipes. The keyboard shortcut opens the panel as before.
- The two answers of the first-run card are one strip with one gliding highlight.
- Fixed: started from the Apps list, the panel closed itself at once.

**Access.** Three more permissions, none of which prompts: uninstalling (Android shows its own dialog),
setting alarms and timers, and changing the brightness (off until you switch it on in Settings).
`play` now plays music; `store` searches the Play Store.

## 1.0 (1 October 2026)

The first release.

- A glass panel in the upper middle of the screen, opened by a keyboard shortcut for the app (Action +
  Alt + Space or Action + K are free on a Googlebook) and closed by the same keys.
- Apps from every profile: starts, later words, initials, loose letters. It learns what you pick for
  the text you typed.
- The top hit completes in the field. Tab lists a row's other actions (App info, Store page, Copy).
- Sums in place, about 30 settings pages by name or keyword, typed addresses, web search with a choice
  of engine, keyword searches (`yt lofi`) and your own.
- Search suggestions, off until you turn them on: with them on, what you type goes to your search
  engine.
- Flat glass in your wallpaper's colors with a white outline; Clear, Balanced or Frosted. The selection
  glides, lists cascade in, the actions view slides. Cuts when the system's animations are off.
- A first-run card under the field for the shortcut and for suggestions.
- English (US and British) and German.

## 0.1 (1 October 2026, not released)

The proof that it works on a Googlebook with no permissions.

- A glass panel in the upper middle of the screen, opened by the app's launcher activity, so a
  Googlebook keyboard shortcut (Action + a key of your choice) opens it and the same key closes it.
- Apps from every profile, with matching on starts, later words, initials and loose letters.
- It learns: what you pick for the text you typed comes first next time.
- Sums in place, about 30 system settings pages, typed addresses, and a web search row.
- Tab lists a row's other actions. Esc, a click outside or another window closes the panel.
