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
    Body (what a row shows: slots, a level, a grid, a code), Icon, Provider, Scope; `AppChip` (an app as the chip,
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
    `Zero.kt` (your usual: which rows stand under the empty field, and `Under`: what has that place),
    `Reach.kt` (a way to go, a phone number and its text, a Telegram name, a Meet code, and their links), `Schemes.kt` (which addresses a link may have, and when typed text is an app's address), `Requests.kt` (what an app command asks an app for, as an `intent:` address and back; the `am start` reader),
    `Play.kt` (what was typed after `play`, split), `Spotify.kt` (its searches, its replies, the links that play), `AppSearch.kt` (a search inside an app: the bundled table, which source wins, the name split from the text),
    `Links.kt` (a link without its tracking tail), `Languages.kt`, `Plain.kt` (an answer without Markdown), `Secrets.kt`, `Emoji.kt`, `Letters.kt` (`abc`: other
    languages' letters), `Jumps.kt`, `Ask.kt`, `Places.kt` (where a window goes), `NoteText.kt` (notes and
    tasks as lines), `Prompts.kt`.
  - Flights (M5): `Flights.kt` (a flight number read off the line, strong or weak; the airline table),
    `AirLabs.kt` (the flight service's replies read, and which requests "the next flight" takes),
    `FlightStatus.kt` (one flight as a small model, and what its row says). Their tests read real replies
    kept in `core/src/test/resources/airlabs/`, without the `request` object the service repeats the key in.
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
    level, grid, QR, the answer being written), `Marks` (the drawn check), `CopyLine` (the line for a fresh copy), `ChipLine` (the line under an app's chip that offers its other action), `Footer`, `Metrics`, `Glass`
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
- `./bl debug type TEXT | key up|down|left|right|tab|backtab|back|enter|stay|esc|more|window | dump | find TEXT | apps | close | forget | pref …`
  drives the panel without injecting input, as the keys do (`key more` and `key window` arm the row's arrow and its
  Window stop). `dump` shows the chip (an app's as `chip=appsearch:PKG:search|play`, with `via=` the keyword it was
  entered by and `line=` the action the line under the empty field offers), the placeholder, each row's body and
  actions (`+` a line behind the arrow, `+w` one behind Window, `*` the row's own default), which is armed
  (`armed=more`, `armed=window` on a list's stop) and which list is open (`opened=ID:arrow|window`).
  Also: `guide` (every example of the list of everything, run through the engine), `ai none|downloadable|downloading|real`
  (a prompt's rows on a device in that state), `pin KIND TEXT` and `unpin`, `pref tips again`, `pref key seen|no`,
  `pref shadow off|low|medium|high`, `pref flightkey KEY|none` (the flight service's key; never printed) and
  `flight TEXT` (a flight's row, looked up at once, without the panel). `./bl open stay slow=4 shade=high key` (every spring four times as long; the shadow; as if a
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
- Flat glass: visibly see-through, blurred, a thin tint, a crisp white outline. No bevels, glows or
  sculpted highlights (Alex: "not too 3D esp the highlights. I do like the white outline").
- Motion everywhere, all from `Motion.kt`: highlights move, lists cascade in, names unroll, nothing pops.
  Sizes are known before anything moves; typed text changes in the same frame. Never hold up typing.
- Nothing typed = nothing shown, apart from one thing that may stand under the empty field, in this order: a
  first-run card, the line for a fresh copy, your usual (a switch, off unless chosen), a tip (core `Under`).
- The window is exactly the panel: never WRAP_CONTENT, never bigger than what is drawn, and never resized in
  width (it is neither smooth nor symmetric). The arrival grows the glass inside a window that stays put.
  **The blur is the window's root view**, so before each frame that view is framed to the glass
  (`OverlayActivity.frameGlass`; device-findings.md, "The blur follows the glass"): the blur is the glass's own
  from the seam on. Nothing may be drawn outside the glass: the root view clips it.
- Destructive actions are last, in the error colour, never first, and never run by an arrow or Ctrl + digit.
- What the device's model says is only shown, copied, pinned or put back where the text came from: it never
  runs anything and never outranks a local match.
- The opening is always the field's height; whatever is under the field comes after it (a tip, a card).
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
- Hover selects only on pointer movement (rows appear under a resting pointer as the list grows).
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
