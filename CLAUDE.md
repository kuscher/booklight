# Booklight: notes for working on the code

A Spotlight / Alfred-style launcher for Googlebooks (Googlebook OS = Android 17 desktop): a key
opens a small glass panel over the desktop; type, Enter. Plain APK, Kotlin + Jetpack Compose,
Material 3 Expressive (material3 1.5.0-alpha, pinned). Since 1.1 it also does things: mail, notes,
events, timers, volume, emoji, the user's own links and recipes. Since 2.0: prompts answered by the system's
own model, a pinned window, other apps' commands, `?`, tips, `s` and `k`, places, a window with sections.

**Permissions:** `INTERNET` (suggestions, off until the user turns them on), `REQUEST_DELETE_PACKAGES`
(Uninstall; Android confirms), `SET_ALARM` (Clock), `WRITE_SETTINGS` (brightness; inert until the user
flips the switch); since 2.0, merged in from Google's ML Kit GenAI library (shipped as it is, Alex's decision):
AICore's `BIND_SERVICE` and `ACCESS_NETWORK_STATE`, and the library's usage reporting to Google. No runtime
permissions, no accessibility service, nothing of Booklight's own in the background (the user's rule: adding a
permission needs his say-so and a place in docs/PLAN.md §4/§6).

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
    Body (what a row shows: slots, a level, a grid, a code), Icon, Provider, Scope.
  - `SearchEngine.kt` asks every provider, merges, ranks, learns; inside a scope asks only that scope.
  - `Matcher.kt`, `History.kt`, `Calc.kt`, `Web.kt`, `Sites.kt`, `Suggest.kt`: as in 1.0.
  - Parsers, each with its tests: `Verbs.kt` ("chrome uninstall"), `When.kt` + `WhenParts.kt` + `Durations.kt`
    (dates and times, English and German), `Jot.kt` (mail, event, reminder, timer, new), `Colors.kt`,
    `Templates.kt` (link placeholders), `Clip.kt`, `Secrets.kt`, `Emoji.kt`, `Letters.kt` (`abc`: other
    languages' letters), `Jumps.kt`, `Ask.kt`, `Places.kt` (where a window goes), `NoteText.kt` (notes and
    tasks as lines), `Prompts.kt`.
- `app/` — Compose app, package `io.github.kuscher.booklight`.
  - `BooklightApp` the process: providers, scopes, engine, stores. **Register a new provider here.**
  - `Guide.kt` everything Booklight does, as one table in the string resources (`guide`): the list behind `?`,
    the window's Commands page. **A new keyword gets a line there, in English and German**, and
    `./bl debug guide` checks that every example works. `Tips.kt` the tips (`tips` in the resources).
  - `ai/OnDevice.kt` the system's model through ML Kit: status, warm-up, one streamed answer, close.
  - `pin/` the pinned window (picture-in-picture) and what it shows.
  - `providers/` apps (with typed verbs), sums, settings pages, commands, web (addresses, ports, Gemini),
    `Dials.kt` (volume, brightness, media), `Answers.kt` (colour, password, UUID), `User.kt` (the user's
    links and recipes), `AppCommands.kt` (what other apps offer: docs/EXTENSIONS.md), `Keys.kt` (the system's
    shortcuts, and the `s` and `k` scopes), `SuggestProvider` (the only network code of Booklight's own).
  - `scopes/` rows that take text: `Scopes.kt` (the registry: **a new scope is one line there**; links that
    take text, prompts), `Jot.kt` (mail, note, event, remind, timer, alarm, new, ask), `Picks.kt` (emoji, symbols,
    QR, colour, snippets, clipboard, text from another app), `NotesScopes.kt` (notes, todo, pin),
    `Prompts.kt` (a prompt's row and its answer), `Help.kt` (`?`).
  - `Executor.kt` performs effects: the only place that starts activities, writes the clipboard or changes
    a system value. Helpers in `device/` (audio, screen, files, QR images) and `data/Notes.kt`.
  - `overlay/` the panel: `OverlayActivity` (window, icon-or-shortcut routing, text from other apps),
    `OverlayModel` (chip, text, rows, selection, arming, an opened row, grid cell, confirmation, the answer of
    the model, tips, Booklight typing), `Panel` (keys, the unfold arrival, the tip card), `Field` (the scope
    chip), `Rows` (and an opened row's list), `Strip` (the action row and the option strip), `Bodies` (slots,
    level, grid, QR, the answer being written), `Marks` (the drawn check), `Footer`, `Metrics`, `Glass`
    (shader, edge light, the shadow's settings), `PanelOutline` (the blur's corners; `GlassFrame`, where the glass stands; keeps the shadow off the
    glass), `Motion`.
  - `window/` the Booklight window the icon opens: `MainActivity` (the sections' pages, the keys), `Nav` (the
    column of sections), `Page` (rows and the one pill), `Stage` (the live demo), `Commands` (the Yours page:
    editors for links, snippets, recipes, prompts).
  - `entry/` the widget, the tile, the notes-folder picker. `data/` settings (`Prefs`), history, notes, recipes.
  - `app/src/debug/…/DebugReceiver.kt` adb hooks (DUMP-guarded, debug builds only: they print what is typed).
- `app/src/main/assets/emoji.tsv` is generated by `tools/emoji.py` (Unicode data; see `NOTICE`).
- `bl` — the helper script (its header lists every command).

## Dev loop
- `./bl app` builds, installs and opens the panel on the Googlebook. `./bl test` runs the core tests.
- `./bl debug type TEXT | key up|down|left|right|tab|backtab|back|enter|stay|esc | dump | find TEXT | apps | close | forget | pref …`
  drives the panel without injecting input (`dump` shows the chip, each row's body and actions, and which is armed).
  Also: `guide` (every example of the list of everything, run through the engine), `ai none|downloadable|downloading|real`
  (a prompt's rows on a device in that state), `pin KIND TEXT` and `unpin`, `pref tips again`, `pref key seen|no`,
  `pref shadow off|low|medium|high`. `./bl open stay slow=4 shade=high key` (every spring four times as long; the shadow; as if a
  key had opened it). `./bl wshot NAME` = PNG of the Booklight window.
  **`./bl debug keys TEXT` types one character at a time**: use it for anything about typing; `type` sets the whole text at
  once and so never shows a row changing under the selection (a crash hid behind that in 1.1's review). `./bl shot NAME` = PNG of the panel's own window (debug builds).
  `./bl open stay tint=0.2 blur=24 dim=0.1 opening=slow` tries glass values and the arrival; `DARK=true ./bl open stay` the dark theme.
- Pictures: captures in `docs/design/captures/` → `tools/store_scenes.py` (a drawn desktop behind them) →
  `store-submission/graphics/` (see its README) and `docs/images/`. `tools/logo.py` draws the icon PNGs.
- Motion can only be judged in motion: `adb shell screenrecord`, then step through the frames
  (the recording shows the whole screen: delete it afterwards, never share it).
- adb: from the Mac (`~/Library/Android/sdk/platform-tools/adb`; the first non-emulator device or
  `$ANDROID_SERIAL`), or VSCodeBook's Unix socket inside the Googlebook's Linux VM. Android user 10
  → `--user current` everywhere. `local.properties` (not committed): `sdk.dir=$HOME/Library/Android/sdk`.
- Release key: `~/.config/booklight/keystore.jks` + `keystore.pass` (alias `booklight`, SHA-256 61:30:F1:F9…90:A7:F6;
  Play's app signing and upload key too; backup exists). Debug builds are signed with it as well, so
  debug and release replace each other. `./bl uninstall` removes the app for every Android user (0 and 10).
- Release: a `v*` tag (docs/RELEASING.md). Play app id 4972005003444967962; the Play Console work is done by
  the session in ~/googlebook-tech.

## Device rules (Alex's HP Googlebook 14; the Lenovo Googlebook 15 is the test device)
- Two Googlebooks may be attached: set `ANDROID_SERIAL` (HP `adb-HP-SERIAL-…`, Lenovo `adb-LENOVO-SERIAL-…`). The HP's
  transport drops when its lid closes. The Lenovo is for tests; what is said below for the HP is the safe default there too.
- **Never touch the Debian VM**: don't launch or force-stop the Terminal app, no `vm` commands, no
  reboot, no adbd or Wireless-debugging changes.
- **Never run `uiautomator dump`**: it suspends every accessibility service and crashed BentoBar
  (docs/research/device-findings.md). Read system screens from `screencap` crops.
- Opening the panel takes the keyboard. `./bl idle` first; keep tests short; inject keys and taps
  only into Booklight's own focused window.
- `./bl screen` shows what is behind the panel (other people's mail). Never commit or publish one;
  use `./bl shot`.
- Action + K is bound to Booklight on the HP (Keyboard shortcuts → App shortcuts); Alex said to keep it.
  The system accepts only shortcuts with the Action key, and one per app (docs/research/device-findings.md).

## Design rules
- One panel, one ranked list, row one selected. Enter runs its armed action. No groups, no tabs, no toolbars.
- A row shows all it can do as icons; the armed one is unrolled. One highlight per level (the list's pill,
  the row's pane, the grid's square), and it travels; never two, never a fade between two places.
- Flat glass: visibly see-through, blurred, a thin tint, a crisp white outline. No bevels, glows or
  sculpted highlights (Alex: "not too 3D esp the highlights. I do like the white outline").
- Motion everywhere, all from `Motion.kt`: highlights move, lists cascade in, names unroll, nothing pops.
  Sizes are known before anything moves; typed text changes in the same frame. Never hold up typing.
- Nothing typed = nothing shown (after the two first-run cards).
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
- A held key repeats: Enter, Tab-into-a-scope and Ctrl + digit act on the first press only (`repeatCount`), and the panel locks once something has run.
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
