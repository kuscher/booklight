# Booklight: notes for working on the code

A Spotlight / Alfred-style launcher for Googlebooks (Googlebook OS = Android 17 desktop): a key
opens a small glass panel over the desktop; type, Enter. Plain APK, Kotlin + Jetpack Compose,
Material 3 Expressive (material3 1.5.0-alpha, pinned). Since 1.1 it also does things: mail, notes,
events, timers, volume, emoji, the user's own links and recipes. Since 2.0: prompts answered by the system's
own model, a pinned window, other apps' commands, `?`, tips, `s` and `k`, places, a window with sections.

**Permissions:** `INTERNET` (suggestions, off until the user turns them on; a flight's times, only with a key the user put in), `REQUEST_DELETE_PACKAGES`
(Uninstall; Android confirms), `SET_ALARM` (Clock), `WRITE_SETTINGS` (brightness; inert until the user
flips the switch); since 2.0, merged in from Google's ML Kit GenAI library (shipped as it is, Alex's decision):
AICore's `BIND_SERVICE` and `ACCESS_NETWORK_STATE`, and the library's usage reporting to Google. No runtime
permissions, no accessibility service, nothing of Booklight's own in the background (the user's rule: adding a
permission needs his say-so and a place in docs/PLAN.md §4/§6).

## This repo is public
The Play listing links here. Keep out of every file, commit message and release note: device serial numbers and
adb names, build numbers and codenames, what else is installed or open on Alex's devices, the names of his
private projects and paths into their repos, and where keys are backed up. Those live in
`~/.config/booklight/NOTES.md` on the Mac (not in any repo); read it when a note here says "the private notes".

## Read first
- Plan, decisions and roadmap: `docs/PLAN.md`. Design: `docs/superpowers/specs/2026-10-01-booklight-2.0-design.md`
  (2.0; §1 and §2 are Alex's decisions, §4 has what the device changed) on top of `…-booklight-1.1-design.md`
  and `…-booklight-design.md` (1.0). The three design reviews of 2.0: `docs/design/reviews-2.0/`.
- How it should behave and look: `docs/design/ux-model.md` (keys, scopes, verbs; §14 for 2.0), `docs/design/design-system.md`
  (tokens, components, the motion table; §4 carries a note on what the devices changed, §11 what 2.0 changed), and the prototype
  `docs/design/booklight-next.html` (https://claude.ai/artifact/Nc98FHZosXidEaHqMpU1me; publish the same file
  to update it). The 1.0 page: `docs/design/booklight-plan.html`.
- Facts: `docs/research/device-findings.md` (checked on the devices; wins over the desk research),
  `permissions.md`, `android-platform.md`, `launchers.md`, `use-cases.md`.
- Status: `docs/PICKING-UP.md`.

## Layout
- `core/` — pure Kotlin (no `android.*`), tested with JUnit on the Mac. `./bl test`.
  - `Model.kt` Query (text + the scope it is for), Result, Action, Effect (what an action does, as data),
    Body (what a row shows: slots, a level, a grid, a code, a flight), Icon, Provider, Scope; `AppChip` (an app as the chip,
    with `Act`: Search or Play) and `Door` (a keyword that is a short way into one).
  - `SearchEngine.kt` asks every provider, merges, ranks, learns; inside a scope asks only that scope (an app's
    chip for the action that is armed; a keyword's door is followed); places the typed sentence by who leads.
  - `AppRow.kt` (an app's row: Open · Search · Play · Window · the arrow, which list a line stands in; `Chips`: the
    action a chip is entered with; `Origin`: where Enter was pressed, for Backspace), `Stops.kt` (what the arming
    can rest on, and where a typed line stands), `Lead.kt` (words after an app's name: the web or the app first).
    The design: `docs/design/app-structure/`.
  - `Matcher.kt`, `History.kt`, `Calc.kt`, `Web.kt`, `Sites.kt`, `Suggest.kt`: as in 1.0.
  - Parsers, each with its tests: `Verbs.kt` ("chrome uninstall"), `When.kt` + `WhenParts.kt` + `Durations.kt`
    (dates and times, English and German), `Jot.kt` (mail, event, reminder, timer, new), `Colors.kt`,
    `Templates.kt` (link placeholders), `Clip.kt` (the clipboard's transforms, and when a fresh copy is offered),
    `Zero.kt` (your usual: which rows stand under the empty field, and `Under`: what has that place), `FirstRun.kt` (first run: which screen stands at the next opening, what an answer changes, who gets a run at all, what Enter does while a lesson stands, the coach line, the recipes and this device's example; `overlay/FirstStage.kt` draws the key's step, the three lessons and the question on one skeleton, the choices are a list of two rows; `Show.kt` is the opening's show as data, its beats, their times and its example flight; `overlay/Welcome.kt` draws the welcome on one clock, whose marks are `Lights` in `overlay/Motion.kt`, and the highlight's way into the key's step; `SetDown.kt` says when each part of a stage comes, for every way a screen can come (set down part by part, come back to, in another's place, after a list, at the piece's landing), and from which of those marks a key counts (`since`), and the stage is drawn by those marks on one clock; "First steps" asks for the run again, as a command in the panel and as a row on the window's Start page),
    `Reach.kt` (a way to go, a phone number and its text, a Telegram name, a Meet code, and their links), `Schemes.kt` (which addresses a link may have, and when typed text is an app's address), `Requests.kt` (what an app command asks an app for, as an `intent:` address and back; the `am start` reader),
    `Play.kt` (what was typed after `play`, split), `Spotify.kt` (its searches, its replies, the links that play), `AppSearch.kt` (a search inside an app: the bundled table, which source wins, the name split from the text),
    `Links.kt` (a link without its tracking tail), `Languages.kt`, `Plain.kt` (an answer without Markdown), `Secrets.kt`, `Emoji.kt`, `Letters.kt` (`abc`: other
    languages' letters), `Jumps.kt`, `Ask.kt`, `Places.kt` (where a window goes), `NoteText.kt` (notes and
    tasks as lines), `Prompts.kt`.
  - Flights (M5): `Flights.kt` (a flight number read off the line, strong or weak; the airline table),
    `AirLabs.kt` (the flight service's replies read, and which requests "the next flight" takes),
    `FlightStatus.kt` (one flight as a small model, and what its row says: `FlightStatus.row` gives the phase, the
    headline, the one badge with the 15-minute rule, the plane's place by time and what stands under the line's two
    ends; `FlightRowTest` says it per phase). Their tests read real replies
    kept in `core/src/test/resources/airlabs/`, without the `request` object the service repeats the key in.
    The row's design: `docs/design/flights-row/` (`design.md` is the specification).
- `app/` — Compose app, package `io.github.kuscher.booklight`.
  - `BooklightApp` the process: providers, scopes, engine, stores. **Register a new provider here.**
  - `Guide.kt` everything Booklight does, as one table in the string resources (`guide`): the list behind `?`,
    the window's Commands page. **A new keyword gets a line there, in English and German**, and
    `./bl debug guide` checks that every example works. `Tips.kt` the tips (`tips` in the resources).
  - `ai/OnDevice.kt` the system's model through ML Kit: status, warm-up, one streamed answer, close.
  - `pin/` the pinned window (picture-in-picture) and what it shows.
  - `providers/` apps (with typed verbs), sums, settings pages, commands, web (addresses, ports, Gemini),
    `Dials.kt` (volume, brightness, media; the music apps; `play` as a door), `Answers.kt` (colour, password, UUID), `User.kt` (the user's
    links and recipes), `AppCommands.kt` (what other apps offer: docs/EXTENSIONS.md), `AppSearch.kt` (where an
    app's search goes), `AppChips.kt` (an app as the chip: its Search row and its Play row, `play` and the built-in
    links as ways into it, the typed sentence), `Keys.kt` (the system's
    shortcuts, and the `s` and `k` scopes), `SuggestProvider` and `Flights.kt` (the only network code of Booklight's
    own: suggestions once switched on, and a flight's times once the user has put in their own AirLabs key;
    `FlightsProvider` is the row, `FlightScope` the keyword `flight`).
  - `scopes/` rows that take text: `Scopes.kt` (the registry: **a new scope is one line there**; links that
    take text, prompts), `Jot.kt` (mail, note, event, remind, timer, alarm, new, ask), `Picks.kt` (emoji, symbols,
    QR, colour, snippets; `TextScope`: what was copied or handed over, and what can be done with it; `tr`), `NotesScopes.kt` (notes, todo, pin),
    `Prompts.kt` (a prompt's row and its answer), `Help.kt` (`?`).
  - `Executor.kt` performs effects: the only place that starts activities, writes the clipboard or changes
    a system value. Helpers in `device/` (audio, screen, files, QR images; `Clipboard`, the one place that
    touches the system's clipboard and its text classifier) and `data/Notes.kt`.
  - `overlay/` the panel: `OverlayActivity` (window, icon-or-shortcut routing, text from other apps),
    `OverlayModel` (chip and the action armed on an app's chip, text, rows, selection, arming, an opened row and
    which of its two lists, grid cell, confirmation, the answer of the model, tips, Booklight typing), `Panel` (keys,
    the unfold arrival, the tip card), `Field` (the scope chip; an app's chip has the app's icon), `Rows` (and an
    opened row's list), `Strip` (the action row and the option strip), `Bodies` (slots,
    level, grid, QR, the answer being written), `FlightBody` (a flight's row: line one, the headline and its badge, the two
    ends) and `FlightLine` (the flight as a line with the plane on it; the pinned window draws the same one, small), `Marks` (the drawn check), `CopyLine` (the line for a fresh copy), `ChipLine` (the line under an app's chip that offers its other action), `Footer`, `Metrics`, `Glass`
    (shader, edge light, the shadow's settings), `PanelOutline` (the blur's corners; `GlassFrame`, where the glass stands; keeps the shadow off the
    glass), `Motion`.
  - `window/` the Booklight window the icon opens, an ordinary desktop window in Material 3 Expressive
    (`docs/design/window-redesign.md`, `docs/design/design-system.md` §5): `MainActivity` (the frame: rail or bar,
    the pane and its column, the second pane, every key, the caption bar, the shortcuts for the system's helper),
    `Frame` (size classes and measures), `Nav` (Material's rail and bar with Booklight's travelling indicator;
    `Part` is the six sections), `Page` (the registry of stops for the keys, `Group` and `PageRow` on Material's
    segmented list, the one focus ring), `Controls` (switch, connected choice, the menu button, a field as a row's control, menus, tooltips),
    `Pages` (Start, Look, Results, Labs, Privacy, the second pane), `FlightsGroup` and `SongsGroup` (Labs' groups for the flight service's key and for Spotify's), `Commands` (the Commands page: Built in · Yours, Find,
    New, the editors), `Stage` (the panel on a drawn desk: the demo, the preview of the look, a command's example),
    `Scrollbar`.
  - `entry/` the widget, the tile, the notes-folder picker. `data/` settings (`Prefs`), history, notes, recipes.
  - `app/src/debug/…/DebugReceiver.kt` adb hooks (DUMP-guarded, debug builds only: they print what is typed).
- `app/src/main/assets/emoji.tsv` is generated by `tools/emoji.py` (Unicode data; see `NOTICE`).
- `app/src/main/assets/airlines.tsv` is generated by `tools/airlines.py` (Virtual Radar Server's standing data, CC0; see `NOTICE`).
- `app/src/main/assets/appsearch.tsv` (where a search inside an app goes) is kept by hand; run `./bl debug appsearch` on a Googlebook before a release.
- Spotify (`providers/Songs.kt`, the third piece of network code; asked only while Spotify is the chip and Play is armed): only with the user's own client id and secret (`files/spotify.key`, set in the window › Labs or with `./bl debug pref spotifykey ID SECRET`; never printed). `./bl debug song TEXT` looks a name up at once; `./bl debug players` lists the music apps. Its tests read real replies in `core/src/test/resources/spotify/`.
- An app's own pages in Settings (`providers/AppPages.kt`) are lines behind the arrow of its row; their typed word puts one in the arrow's slot. `scopes/Reach.kt`: go, meet, call, sms, wa, tg; `Takers` says which of them have an app here (a keyword without one is not a keyword). `window/AppCommand.kt`: the editor of an app command; such a command is only ever made there, never read from outside.
- **No key of anyone's in the repo.** The flight service's key is the user's own (`files/flights.key`, set in the
  window or with `./bl debug pref flightkey KEY`). The service repeats the key in every reply: a reply saved as a
  test file loses its `request` object first.
- `bl` — the helper script (its header lists every command).

## Dev loop
- `./bl app` builds, installs and opens the panel on the Googlebook. `./bl test` runs the core tests.
- `./bl debug type TEXT | key up|down|left|right|tab|backtab|back|enter|enter2|stay|esc|more|window | dump | find TEXT | apps | close | forget | pref …`
  drives the panel without injecting input, as the keys do (`key more` and `key window` arm the row's arrow and its
  Window stop). `dump` shows the chip (an app's as `chip=appsearch:PKG:search|play`, with `via=` the keyword it was
  entered by and `line=` the action the line under the empty field offers), the placeholder, each row's body and
  actions (`+` a line behind the arrow, `+w` one behind Window, `*` the row's own default), which is armed
  (`armed=more`, `armed=window` on a list's stop) and which list is open (`opened=ID:arrow|window`).
  Also: `guide` (every example of the list of everything, run through the engine), `ai none|downloadable|downloading|real`
  (a prompt's rows on a device in that state), `pin KIND TEXT` and `unpin`, `pref tips again`, `pref key seen|no`,
  `pref shadow off|low|medium|high`, `pref flightkey KEY|none` (the flight service's key; never printed) and
  `flight TEXT` (a flight's row, looked up at once, without the panel). `flight show NAME` puts a sample flight's row
  into the open panel in the phase its name says, looked at when the name says (`friday`, `soon`, `soon-late`, `air`,
  `air-late`, `air-early`, `landed`, `landed-late`, `landed-now`, `cancelled`, `diverted`, `timetable`,
  `timetable-past`, `wide`, `looking`, `offline`, `not-found`, `used-up`, `refused`…, or a saved reply by its name,
  `LH455-in-the-air`: `app/src/debug/…/FlightSamples.kt`); the row waits and the sample lands in it as an answer does,
  the service is not asked, and the footer says "sample NAME". `flight show` lists the names, `flight show off` ends it
  (so does any other text typed), `flight pin NAME` shows the same flight in the pinned window. `dump` says a flight's
  row as `phase=`, line one, `headline=`, `badge=` with its tone, `share=` (the plane's place; `none`: no plane), and
  `from=` / `to=` (a struck time has a `~`; the small words in brackets, after a bar what is left of them when the ends
  would meet). `./bl open stay slow=4 shade=high key` (every spring four times as long; the shadow; as if a
  key had opened it). `./bl wshot NAME` = PNG of the Booklight window.
  The window (debug builds): `./bl window [start|commands|look|results|labs|privacy]` opens it (a release only lets
  Booklight open it); `./bl debug window` says its size in dp, its section, where the keys are and what is selected;
  `./bl debug window close`; `./bl debug window hover KEY` and `unhover KEY` show a row as under the pointer, `press KEY` and `release KEY` as pressed (adb can
  inject keys, clicks and the wheel into it, but no hover and no right-click). A menu is a window of its own and
  is not in a `wshot`: cut a screen capture to the window's bounds for that.
  Motion in the window without a recording of the screen: `./bl debug window film N EVERY` takes N pictures of the
  window's own content, one every EVERY frames, into `cache/film.png` (six in a row, a third of the size; pull it with
  `adb exec-out run-as … cat cache/film.png`); `./bl debug window trace N` logs the layout of the next N frames
  (window and rail width, where the column and the title stand, bar, second pane). `./bl debug window helper` and
  `helper close` show and dismiss the system's Keyboard Shortcuts Helper over the window.
  **`./bl debug keys TEXT` types one character at a time**: use it for anything about typing; `type` sets the whole text at
  once and so never shows a row changing under the selection (a crash hid behind that in 1.1's review). `./bl shot NAME` = PNG of the panel's own window (debug builds).
  `./bl open stay tint=0.2 blur=24 dim=0.1 opening=slow` tries glass values and the arrival; `DARK=true ./bl open stay` the dark theme.
  The highlight's motion: `./bl debug pill N` logs the pill's two edges for the next N frames it moves in (`./bl logs`), `./bl debug held down|up [TIMES] [MS]` repeats a key as a held one does (debug builds).
- First run's stored state (core `FirstRun`; `docs/design/first-run/`): `./bl debug first` shows the run, what is done, the screen that stands and its
  counter, whether the opening piece would play, the key that would be suggested for the keyboards attached, the system's words for its
  dialog as they were read (`words=`), the hold (`hold=`) and the last start as the panel read it (`last start:`). `first new|update|off`,
  `first replay [show]`, `first resume|key|helper|opened|shown`, `first answer not_now|go_on|change_key|skip|agree|done`, `first ran open|search|sum`
  and `first at k1…k4|l2|l3|l4|q|c` set it through the same pure functions the panel calls, and an open panel follows at once; `first older`
  reads the settings as after an older build wrote them. They change first run's own fields only, but for `first key` (`keySeen`) and
  `first answer agree` (suggestions on). While a stage stands (the key's step, a lesson, the question), `./bl debug key tab|backtab|enter` act
  on it as the keys do; on a lesson's typed list `key enter` is practice, as the key is, and `key enter2` is Enter twice in one turn.
  `dump` says `first=` (the stage under the empty field), `due=` (the screen that is due whatever the field holds), `coach=` (the footer's coach
  line), `pressed`, `choices` and `ended`; `./bl debug first` says `example=` (what the lessons' recipes show on this device: never copy it
  into a file of this repo, it names an app of the device). The checks for a device: `docs/research/first-run-key.md` (the key's step),
  `docs/research/first-run-lessons.md` (the lessons, the question, the choices, the ending) and `docs/research/first-run-welcome.md` (the
  welcome and the show).
  The opening piece (the welcome, then the show) begins a new installation's very first opening, which runs at Slow. In an open panel:
  `./bl debug first welcome` plays the welcome, `first welcome at MS` stands it still at that ms of `motion.md`'s own clock (for `./bl shot`
  beside the prototype's frame of the same moment), `first show` plays the show, `first show at apps|row|stop|sum|answer|flight|grid|cell`
  stands it still there, `first land` sets the key's step down. While it plays `./bl debug key …` is taken by it first, as the keys are;
  `dump` says `playing=welcome|show|landing|none`, `cast=ready|none`, `laps=` and `gliding`, and `./bl debug first` says `overture=`,
  `slow=` and `greets=`. The show's first rows are apps of the device: never copy them from a dump into a file of this repo.
  The stage between its screens (`motion.md` §3): `./bl debug first stage at MS` stands the stage's clock still at that ms after its
  screen came, `first stage` lets it run, and `first stage as whole|set_down|back|turn|after_list|lands [at MS]` has the screen that stands
  come again in that way; `first at SCREEN` in an open panel is a real turn from the screen that stands. `first press [under]` lands the
  user's key on the screen that waits for it (in view, or as under the system's dialog) and asks the system's dialog to go, as the real
  landing does; `first press held TIMES MS` sends that many more starts by the key after it, as a held key's repeats come (each starts
  the panel's own activity again); `first again` writes what the window's "First
  steps" row writes; `first says` prints what first run told a screen reader and what every node of the panel says to one; `first trace N`
  takes the next N frames (1 to 2400, 120 unless said: ms, the stage's clock, the window's height, whether it takes Enter and every
  key, the key on the glass, laps, the hold behind the system's dialog), and `./bl trace` prints them, `./bl trace all` every one: the app keeps them, because every
  `./bl debug` and `./bl shot` clears the log first (`./bl logs` is its last 40 lines); `pref sums on|off` is "Show sums".
  `./bl debug turn MS` closes the panel and brings the key again MS later ("turned", or "not turned" where the panel had already
  gone), and `turn MS enter` presses Enter in that moment and says what it answered. `dump` says `comes=`, `lead=`, `all=` (where
  the last answer comes later than the armed one), `end=` and `enter` or `seen` for the stage that stands (Enter on the armed answer
  counts; every key and click does) or `covered` (it came under the system's dialog and is still under it: no key counts yet),
  `later`, `keydown` and `landing` (a start by the key does nothing just now).
  The last pass on a device, with every check the three earlier lists left open: `docs/research/first-run-last-pass.md`.
- Pictures: captures in `docs/design/captures/` → `tools/store_scenes.py` (a drawn desktop behind them) →
  `store-submission/graphics/` (see its README) and `docs/images/`. `tools/logo.py` draws the icon PNGs.
- Motion can only be judged in motion: `adb shell screenrecord`, then step through the frames
  (the recording shows the whole screen: delete it afterwards, never share it).
- adb: from the Mac (`~/Library/Android/sdk/platform-tools/adb`; the first non-emulator device or
  `$ANDROID_SERIAL`), or VSCodeBook's Unix socket inside the Googlebook's Linux VM. Android user 10
  → `--user current` everywhere. `local.properties` (not committed): `sdk.dir=$HOME/Library/Android/sdk`.
- Release key: `~/.config/booklight/keystore.jks` + `keystore.pass` (alias `booklight`, SHA-256 61:30:F1:F9…90:A7:F6;
  Play's app signing and upload key too; it is backed up). Debug builds are signed with it as well, so
  debug and release replace each other. `./bl uninstall` removes the app for every Android user (0 and 10).
- Release: a `v*` tag (docs/RELEASING.md). Play app id 4972005003444967962; the Play Console work is done by
  the session in ~/googlebook-tech.

## Device rules (Alex's HP Googlebook 14; the Lenovo Googlebook 15 is the test device)
- Two Googlebooks may be attached: set `ANDROID_SERIAL` (`adb devices` lists them; which is which is in the private notes). The HP's
  transport drops when its lid closes. The Lenovo is for tests; what is said below for the HP is the safe default there too.
  **Choose the device by its model (`adb devices -l`), never as "the first one that is not an emulator"**: the HP can
  attach in the middle of a run and is then listed first (`./bl` without `ANDROID_SERIAL` takes the first).
- **Never touch the Debian VM**: don't launch or force-stop the Terminal app, no `vm` commands, no
  reboot, no adbd or Wireless-debugging changes.
- **Never run `uiautomator dump`**: it suspends every accessibility service and crashed BentoBar
  (docs/research/device-findings.md). Read system screens from `screencap` crops.
- Opening the panel takes the keyboard. `./bl idle` first; keep tests short; inject keys and taps
  only into Booklight's own focused window.
- `./bl screen` shows what is behind the panel, private things included. Never commit or publish one;
  use `./bl shot`.
- Action + K is bound to Booklight on the HP (Keyboard shortcuts → App shortcuts); Alex said to keep it.
  The system accepts only shortcuts with the Action key, and one per app (docs/research/device-findings.md).

## Design rules
- One panel, one ranked list. Row one is selected in every list that typing made; Enter runs its armed action. With
  "Show your usual" on, two or three rows stand under the empty field with none selected, and Enter does nothing
  until Down. No groups, no tabs, no toolbars.
- A row shows all it can do as icons; the armed one is unrolled. One highlight per level (the list's pill,
  the row's pane, the grid's square), and it travels; never two, never a fade between two places.
- An app's row is Open · Search · Play · Window · the arrow, in that order for every app; an app shows only what it
  has and nothing is dimmed to hold a place. Window and the arrow each open a list under the row: two lists, never
  a list inside a list. **Tab only moves; Enter does**: on a row that offers more than one thing Tab enters
  nothing. Enter on Search or Play makes the app the chip (its own icon and name; one chip for an app, not one for
  an action). `play`, `yt`, `store`, `maps`, `drive` and the typed sentence end in that same chip and row
  (`docs/design/app-structure/ux.md`, `ux-model.md` §18).
- The mark is what the row is about: an app's icon where that is one app, a symbol in the disc for anything else.
  An action carries a symbol for what it does and never an app's icon.
- One coloured surface, the selection. The one exception is the badge of a flight's row: green for on time and early,
  amber for late (`FlightColors` in `ui/Theme.kt`; Alex, 2 October 2026). Always a small fill with its own ink on it,
  never a coloured word or line on the glass, and never red: red is for what removes something.
- Flat glass: visibly see-through, blurred, a thin tint, a crisp white outline. No bevels, glows or
  sculpted highlights (Alex: "not too 3D esp the highlights. I do like the white outline"). Lifted for first run's welcome alone, by
  his word (night, a lamp's light, large type, a drawn knife: `overlay/Welcome.kt`); it holds again from the key's step on.
- Motion everywhere, all from `Motion.kt`: highlights move, lists cascade in, names unroll, nothing pops.
  Sizes are known before anything moves; typed text changes in the same frame. Never hold up typing.
- Nothing typed = nothing shown, apart from one thing that may stand under the empty field, in this order: first
  run's stage, the line for a fresh copy, your usual (a switch, off unless chosen), a tip (core `Under`).
- The window is exactly the panel: never WRAP_CONTENT, never bigger than what is drawn, and never resized in
  width (it is neither smooth nor symmetric). The arrival grows the glass inside a window that stays put.
  **The blur is the window's root view**, so before each frame that view is framed to the glass
  (`OverlayActivity.frameGlass`; device-findings.md, "The blur follows the glass"): the blur is the glass's own
  from the seam on. Nothing may be drawn outside the glass: the root view clips it.
- Destructive actions are last, in the error colour, never first, and never run by an arrow or Ctrl + digit.
- What the device's model says is only shown, copied, pinned or put back where the text came from: it never
  runs anything and never outranks a local match.
- The opening is always the field's height; whatever is under the field comes after it (a tip, first run's stage).
- Every user-facing string is a resource, English and German. Copy is plain: "Open", "Copy", "Search".
- The Booklight window is not the panel: rows in Material's containers, hover and focus as two things, one focus
  ring that glides. Its lines: the title, the lead, a group's name and every row's mark start 16 dp in from the
  column's edge; every row's text starts on one edge after the mark; every control in a row ends 16 dp from the
  row's trailing edge; what has a fill of its own outside a row (Find, Built in · Yours, New) stands on the
  column's own edges. The choices of one page all stand in the same place (at the row's end, or all under their
  text). Nothing is centred in the window. Alex judges alignment first. Pictures of every size:
  `docs/design/captures/window-redesign/` (in them the window is lower where more would show the user's own
  snippets or the apps of the device).

## Gotchas
- Panel sizes live in `Metrics`; `Metrics.height(model)` must match what `Panel` draws, or the
  window clips the list.
- A flight's row is said again each minute from the answer that is kept (`OverlayModel.minute`, `FlightsProvider.told`),
  never through `FlightsProvider.row`: that one takes a kept answer only for two minutes and would then ask the
  service again. The plane only goes forward (`FlightStatus.forward`; the provider remembers where each flight's was drawn).
- Every row of the flights provider has one seat in the list (`RowSlots.sync`): the ordinary row of a guess grows
  into the tall one where it stands, and keeps its mark and strip on the tall row's line while it does.
- `Metrics.maxRows` keeps room for one row of the list to be 136 dp (a flight's, a grown answer): a taller kind of
  row in an ordinary list needs that room made larger.
- Hover selects only on pointer movement (rows appear under a resting pointer as the list grows).
- While a lesson of first run stands (`OverlayModel.lesson`: also while its list is typed, when `stage` is null), nothing opens:
  `OverlayActivity.run`, where everything that runs passes, asks core `FirstRun.enters` before the executor. An app's Open and a
  search inside an app are practice (and a settings page under its keyword, where Settings stands in for an app that can be searched
  or the example is not worked out yet: `example?.enter`); a sum is copied and the panel stays; 520 ms later the list gives way to what stands next
  (`lessonOver`, `OverlayModel.giveWay`), and until then `settled` keeps a second Enter from running anything. Anything that runs
  an action by another way than `OverlayActivity.run` would open things in a lesson.
- A screen of first run takes a key or a click only once the answer it would run has been in view for 350 ms (`OverlayModel.seen`,
  core `FirstRun.inView`: the glass open, the screen not just come, and that answer begun to show): the Enter that skipped a lesson,
  pressed again at once, answers nothing on the screen that took its place, and nothing is answered that is still being set down. On the
  question the first answer is "Not now" (core `FirstRun.answers`): Tab then Enter, the keys that skip, agree to nothing. A hook that
  changes the screen and a key hook need that moment between them. Which mark a key counts from is core's `SetDown.since`: Enter on the
  answer that was armed when the screen came, from that answer's own (`Marks.lead`); Tab, the pointer, a click, and Enter once Tab has
  moved the arming, from the last answer's (`Marks.all`, later at a landing alone). A screen that could not stand when it came (a
  lesson's Enter on a list that is typed on) or was covered before it had been in view (a letter, the last text, a chip:
  `OverlayModel.covered`, which everything that fills the field calls first) is `unseen`: it comes, and is counted, when the field
  is empty again (`search`). One that was in view before it was covered is only back, and counts at once. (`unseen` is `rearm`'s and `covered`'s to
  set, where something really is in the screen's place: what ends the opening piece for a typed character only says that the key's step
  came, and the character's own `covered` does the rest.) The system's dialog covers a screen too: one of the key's that came while the
  dialog was over the panel ("Now press your keys" a moment after the dialog came, "Your key works" where the key landed under it), or
  that the dialog came over before it had been in view, counts from when the panel is uncovered and not from when it came
  (`OverlayModel.uncoveredAt`, core `FirstRun.inView`'s `uncovered`). Nothing is drawn or said again for that, and no key counts while
  it is covered. The end of the hold reaches the model through `OverlayModel.under`, which `OverlayActivity.signal` sets, the one
  place the hold changes: a hold that ended any other way would leave such a screen deaf.
- A screen of first run comes onto the glass by marks (core `SetDown.marks` with `Motion.kt`'s times, as `PACE`): the model works them
  out in the moment the screen comes (`OverlayModel.came`: at its making, in `rearm`, `giveWay`, `again`, `land`, `turned`, `closing`,
  and for the choices in `offerChoices`) and counts by them; `overlay/FirstStage.kt` draws every part by its mark on one clock that
  starts at `OverlayModel.viewFrom`. Whatever makes a screen come must say so through `came`, or the screen is drawn and counted by the
  marks of the one before; and what sets a screen down in a way of its own after Booklight emptied the field (`giveWay`, `again`) calls
  `empty()` first and `came` after it: emptying the field brings an `unseen` screen into view whole, which would undo that way. The parts
  move as layers over a layout that never changes: nothing may move the answers' place (the highlight's way into the armed answer reads
  it once), and what gives way is drawn by the clock, the marks, the arming and the counter of its last moment (`own`, `kept`).
- "First steps" is the panel's own: its command carries core `FirstRun.AGAIN`, which `OverlayActivity.run` catches before the executor
  (`OverlayModel.again`: the run stands in the panel that is open, without the opening piece). Its row is offered only where it was typed
  for (core `FirstRun.typedFor`, asked by `CommandsProvider`: three letters or more that begin its name, or one of its other words in
  full): by the matching every other row is found by, "f", "fi" and "w" find it too, and someone who never asked for first run would
  meet it in an everyday list. And only on a screen that can hold a stage: the open panel's own (`BooklightApp.firstFits`, which the
  panel answers for the display it stands on; the window's row asks the window's). The window's row writes the state and
  starts a plain panel by `ACTION_PANEL`, as a row of the Commands page does for an example: Booklight starts its panel in no other
  way, and never over another app's window.
- First run's choices are no stage: they are `results` with nothing selected, as the usual rows stand at rest (`choicesUp`; `atRest`
  is either). They are left, and the run is over, by Enter at rest, a typed letter, their second row, or the panel closing. Their keys
  (Enter, Ctrl + digit, Tab or Right on the second row, a click) count 350 ms from the moment they are set down, as a stage's do.
- The user's key, held, repeats, and each repeat is a new start of the panel. Where the key has just landed ("Your key works": in view,
  under the system's dialog, or in a panel the key made), a start by the key does nothing for 700 ms of real time after the last one
  (`Motion.KEY_SETTLES_MS`, `OverlayModel.keyAgain`, asked first in `onNewIntent`): not built from motion's times, so it holds with the
  system's animations off.
- While first run's opening piece plays (`OverlayModel.playing`), the show performs with the panel's real rows and runs nothing: its
  rows are the cast's (`BooklightApp.cast`), whose every action carries core `Show.NOTHING`; `stage` is null while the welcome or the show plays, and the key's step's at the landing; `Panel.keys` and a layer
  over the glass give every key and click to `OverlayModel.press` first (core `FirstRun.pressed`), and `enter`, `runRow`, `went` and
  `OverlayActivity.run` return at once. Only a typed character passes, and the piece is over by then. The key's step it lands in takes a
  key 350 ms after its armed answer has begun to show (`seen`): the piece is `LANDING` until then. The piece stops performing in one
  place (`stop`), whichever way it ends. A panel that closes ends its piece
  (`closing`), and one the key turns round while it folds counts what stands under its field as come in that moment (`turned`). `OverlayModel.playing` is declared before
  `stage`, which reads it while the model is made: keep that order.
- The welcome's height is `Metrics.welcome` under the field (468 dp in all), from which core `FirstRun.WELCOME_SCREEN_DP` is reckoned.
  Its layer is the first child of the glass, 720 × 468 dp, cut by the glass: nothing of it may be drawn outside that layer.
- The glass's shader (`Glass.kt`, `GLASS`) is compiled on the device, not by the build: a mistake in it stops every panel from opening.
  After a change to it, open the panel on a device before anything else.
- A constant alone is not a sum (`e`, `pi`): otherwise typing "e" shows 2.718 instead of apps.
- `LauncherApps.startMainActivity` is used for every launch so work-profile apps open too.
- The app list skips Booklight itself.
- User serial numbers are not 0 on these devices (the main user is 10): compare with the own user's serial, never with 0.
- A scope's keyword and a space becomes a chip as soon as it is typed (`OverlayModel.type`); a debug `type` leaves any scope first.
- A row can keep its id and lose its actions (a timer whose text stops being a duration): anything indexed by the action list must cope with it being empty.
- A held key repeats: Enter, Tab-into-a-scope, Tab between Search and Play under an app's chip, and Ctrl + digit act on the first press only (`repeatCount`), and the panel locks once something has run.
- An app's chip (`AppChip`) has no keyword, like the chip of text another app handed over. Wherever "no keyword" is taken to mean handed-over text (not kept, not looked up, no web row), an app's chip is the exception: ask `is AppChip`.
- Under an app's chip the list is made for the chip, the text and the armed action (`OverlayModel.resultsFor`): a change of action makes the list anew, and Enter in between waits for it. The row keeps one id for both actions, so it does not leave and arrive.
- Which apps play music is asked of the package manager when the app list is read (`AppChips.read`), never while typing; the chips are made anew then, so nothing may hold a chip across a read but by its key.
- A keyword that is a door (`Scope.door`: `play`, a built-in link whose app is installed) is never the chip itself: `OverlayModel.aim` puts the app's chip there and remembers the keyword (`via`), and `SearchEngine.inScope` follows the door for anyone who asks with the keyword's own key (`./bl debug find`, `guide`).
- The model remembers the word that was typed for a scope (`meeting` → Event): the web row and Backspace give that word back, not the scope's first keyword.
- Inside a scope the engine also offers an app whose name starts with the keyword and the text ("play store").
- `am start -n …OverlayActivity` is the panel; an icon click (source bounds, or the home app as referrer) is the window.
- Gradle needs the memory settings in `gradle.properties` (Compose + material3 alpha).
- The system's model answers only the app in front (the panel is). Asked from a receiver it never answers.
- A scope with no keyword is treated as text another app handed over (not kept, no web row): a prompt whose
  keyword was taken is one.
- `PanelOutline` clears the glass's shape before anything is drawn: without it the window's shadow shows
  through the glass. The shadow's size is the window's elevation, set once the root view exists.
- Opening an activity of Booklight's own at a size: `FLAG_ACTIVITY_MULTIPLE_TASK`, or the desktop gives it the
  bounds of whichever Booklight window is in front.
- A build check must look for `FAILURE` as well as `e:`: a resource error has no `e:` line, and the old APK installs.
- A build that does not know a setting drops it the first time it writes `settings.json` (`ignoreUnknownKeys`, and
  the whole file is written). On a device that goes between branches: copy the file out before the other
  branch's build writes (`run-as`, a debug build), and put it back before handing the device on.
- Close the Booklight window before installing a build: under an open window the desktop puts its own "package
  update" activity into the task, and an `am start` that arrives meanwhile leaves the task full screen.
- `adb shell input keycombination CTRL_LEFT 4` sends Ctrl + Back: a bare number is a key code. Write `KEYCODE_4`.
- In the window Material's parts are not focus stops (`focusProperties { canFocus = false }`, a `Switch` without a
  callback): the root has the keys and `Page` knows the stops. A row is told it has the focus through its
  interaction source so that it takes Material's focused shape; Material's own focus mark is switched off for rows
  (`LocalRippleThemeConfiguration`) and put back inside menus.
- material3 1.5.0-alpha29: a `WideNavigationRailItem` keeps the label style it was first composed with (a rail
  that starts expanded keeps the large style when it collapses), so `Nav` sets the style itself; and a rail item's
  indicator is round its mark and name, not across the rail. A text field with no width cannot take the focus
  (Find, closed to a round button, opens first).
- In the window a row's text starts 52 dp in (16 + a 24 dp mark + 12: Material's list item), not 56.
- An editor's fields are not the editor's: `CommandsState.draft` holds what was typed, because the editor is drawn
  under its row or in the second pane and changes its place when the window crosses 1332 dp. Leave an open editor
  through `CommandsState.leave()` (it asks once if something was changed), never by setting `open` (that is for
  Save and Delete).
- A new window is laid out once at the size the system first gives it (wider than 1332 dp on the Lenovo) and at
  its own a frame later: until `LocalSettled`, what changes place between the column and the second pane does so
  without its motion. (An editor that was open then lost the keys to the copy that was going.)
- What stands under a row's name is the text alone, or a `Column` of it and what is under it, never one `Column` for
  both: a column that loses its second child keeps its old last baseline, and Material then takes the row for a
  three-line one (device-findings.md, "A row that stayed three lines high").
- A choice's row is `PageRow(press = false)`: the keys reach it, the pointer presses only its buttons. A step
  (`onStep`) answers whether it stepped: Left goes to the rail when it did not.
- The window's stages share their panels (`Stages` in `Stage.kt`, provided by `MainActivity`): a `Stage` only draws.
  The demo's loop runs in `Stages.Demo()` while a stage shows it.
- `wrapContentSize(unbounded = true)` centres what is larger than its room: say `Alignment.TopCenter` where the
  top must stay (the stage's panel was pushed up out of a low stage).
- Typed keys reach the field before the input method (`KeysFirst` in `OverlayActivity`: plain keys that print, and
  Backspace, only if the field takes them; not for languages the input method puts together). The editor can hold an
  older text than the model's for a frame (a keyword just became the chip): `Field` remembers what the editor holds,
  and `Panel`'s `onChange` keeps `model.query` as the truth and adds only what the edit added. Check typing with real
  keys (`adb shell input text` / `keyevent`) or `./bl debug keys`; dead keys and AltGr were not tried.
- The list's highlight moves as two edges (`Band`, `Edge` in `Rows`; `pillLead`, `pillTrail`, `pillHold` and the stretch
  limits in `Motion`): the edge in front leads, the old one holds a few frames and follows. Change the feel there,
  then measure with `./bl debug pill N` (`docs/design/rubber-highlight.md` has the numbers it should meet).
