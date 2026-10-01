# Booklight 1.0: design

*1 October 2026. Written after 0.1 proved the platform facts on the HP Googlebook 14
([device findings](../../research/device-findings.md)). The product reasoning and roadmap are in
[PLAN.md](../../PLAN.md); this is what 1.0 looks like, how it behaves and how it is built. Open
decisions are PLAN.md §11; where one is pending, this spec assumes its default.*

## 1. What it is for

Alex wants the Spotlight / Alfred habit on a Googlebook: a hotkey, a light panel in the middle of
the screen, apps first, and a structure that takes new kinds of results and actions without
rework. It must install on any Googlebook with no unusual permissions. Success for 1.0:

- From any app, the key opens the panel and it is ready to type into before the user notices a wait
  (target: under 100 ms warm, under 400 ms cold, on the HP).
- The app the user wants is row one after one to three letters, and stays there once picked.
- Nothing about it needs a permission prompt, and nothing typed leaves the device unless the user
  picks a web row.
- A new provider is one file plus one line of registration.

## 2. The panel

### 2.1 Geometry (dp)

| Part | Size |
| --- | --- |
| Panel width | 720, or the screen width − 48 if narrower |
| Top edge | 20 % of the screen height (the field stays put; the list grows down) |
| Corner radius | 32 |
| Field row | 68 high; search symbol 26; text 24 sp, weight 450 |
| Result row | 56 high; icon 36; title 17 sp, weight 500; subtitle 13 sp |
| Answer row | 92 high; answer 34 sp rounded, tabular figures |
| Suggestion strip (nothing typed) | 96 high; up to seven 44 dp app icons with labels |
| List padding | 8 around the rows |
| Footer | 40 high; 12 sp key hints |
| Rows shown | at most 8; no scrolling |

The window is exactly the panel. Its height follows the content; the top edge never moves.

### 2.2 Material

- **Glass**: `surfaceContainerHigh` at 78 % over a 40 dp window blur, a 1 dp `outlineVariant`
  border at 60 %, and the rest of the screen dimmed by 16 %. When the system turns blur off
  (battery saver, developer option), the surface is solid and the border stays.
- **Colour**: the device's Material You scheme, light or dark with the system. Selection is
  `secondaryContainer`; text on it `onSecondaryContainer`; quiet text `onSurfaceVariant`; the cursor
  and "Copied" are `primary`.
- **Type**: the device's Google Sans Flex. The rounded cut (`ROND` 100) for answers and the
  suggestion labels, the standard cut for everything else. Other devices fall back to the system font.
- **Shape**: Material 3 Expressive. Rows are pills (radius 28); the selected row tightens to 20.
  Key hints are 7 dp chips.

### 2.3 Motion

Speed first: the panel appears and disappears with no system animation.

- Appear: content fades in over 90 ms while scaling from 0.98 (the window is already there).
- Selection: one highlight glides between rows on a spring (damping 0.8, stiffness 700) and its
  corners spring from 28 to 20. Hover moves it only when the pointer actually moves.
- Rows changing: no per-row animation; the window resizes in the same frame.
- Run: the panel closes in the frame the target is started. Copy shows "Copied" for half a second,
  then closes.
- Reduced motion (system setting): no scale, no spring; cuts.

### 2.4 States

1. **Nothing typed, first run.** The field and one row: "Give Booklight a key" with the keys shown
   as caps, Enter opens Keyboard shortcuts. The row goes away once the user has opened the panel
   three times after seeing it, or dismisses it.
2. **Nothing typed.** The field and the suggestion strip: the apps you open most through Booklight,
   Ctrl + 1–7 or a click to open. Empty until there is history.
3. **Typing, with a top hit.** The best row is selected. If its name starts with the typed text,
   the rest of the name shows in the field as grey text after the cursor (it is not inserted).
   Each row: icon, name, and at the right its kind ("App", "Settings"…). The selected row shows its
   action and the Enter key instead.
4. **Answer.** A sum shows as the first row: the expression small, the answer large. Enter copies.
5. **Keyword search.** `yt lofi`: a "YouTube" chip appears in the field, and the
   first row is "Search YouTube for 'lofi'".
6. **Actions.** Tab or → on a row with more than one action: the row stays at the top, its actions
   list below. ← or Esc goes back.
7. **Nothing found.** The fallback rows only: Search the web, and (if installed and it accepts
   shared text) Ask Gemini.
8. **Solid fallback.** Same layout on an opaque surface.

### 2.5 Keys and pointer

| Input | Does |
| --- | --- |
| The summon key (user-bound, suggested Action + K) | Opens the panel; closes it if open |
| Typing | Filters; row one is always selected again |
| ↑ ↓ | Move the selection |
| Enter | Run the selected row's first action |
| Tab, → at the end of the text | Show the row's actions |
| Shift + Tab, ← in actions | Back to results |
| Ctrl + 1…9 | Run that row |
| Esc | Close (in actions: back) |
| Click a row | Run it |
| Click outside, another window gets focus | Close |

### 2.6 Ranking

One list, best first. `rank = match × kind weight + learned boost`.

- **Match** (0–1): same 1.0, start 0.9, start of a later word 0.8, initials 0.75, inside 0.6,
  letters in order 0.45 and lower with gaps; plus up to 0.05 for a shorter name. Keywords the user
  can't see ("dark" for Display) count only from the start of a word, at 85 %.
- **Kind weight**: app 1.0, command 0.92, setting 0.9.
- **Learned boost**: up to 0.7 for the exact typed text (two picks reach about 0.45), plus up to
  0.15 for general use; both halve every four weeks.
- Answers are always first. "Search the web" is always last. A typed address ranks as a strong match.
- Ties: shorter name, then A to Z.

## 3. How it is built

### 3.1 Modules

- `core/` (Kotlin/JVM, no Android): `Model` (Query, Result, Action, Effect, Icon, Provider),
  `Matcher`, `History`, `SearchEngine`, `Calc`, `Web`. JUnit.
- `app/` (Compose): `BooklightApp` (the process: providers, engine, history store, icon cache),
  `overlay/` (activity, model, UI), `providers/`, `Executor`, `settings/`, `data/`, `ui/`,
  `DebugReceiver`.

### 3.2 The contracts

```kotlin
interface Provider {
    val id: String
    suspend fun query(q: Query): List<Result>      // off the main thread, cancellable, time-boxed
    suspend fun zeroState(): List<Result> = emptyList()
}
data class Result(id, provider, kind, title, subtitle?, icon, score, actions, answer?, learnable)
data class Action(id, label, effect, keepOpen)
sealed interface Effect { LaunchApp, AppInfo, OpenUrl, WebSearch, CopyText, OpenSettings, Internal }
```

- A **result id** is stable across sessions (`app:pkg/class`, `setting:wifi`), because learning keys on it.
- An **effect** is data. `Executor.run(effect)` is the only code that starts activities or writes the
  clipboard, so the core is testable and an out-of-process extension can return the same shapes.
- The **engine** asks all providers at once, drops any that exceed the time budget (150 ms) or
  throw, de-duplicates by id, ranks, and returns at most eight.

### 3.3 Additions 1.0 needs over 0.1

| Addition | Where |
| --- | --- |
| `Effect.Uninstall`, `Effect.StorePage`, `Effect.StartIntent(uri)` | `core/Model`, `Executor` |
| Keyword searches (`yt`, `g`…) with user-defined entries | `providers/WebProvider`, `data/Prefs` |
| Completion text and keyword chip in the field | `overlay/OverlayUi`, `OverlayModel` |
| Suggestion strip and first-run row | `overlay/OverlayUi`, `CommandsProvider.zeroState` |
| One gliding selection highlight | `overlay/OverlayUi` |
| `Prefs` (DataStore): engine, keywords, kinds on/off, first-run state | `data/Prefs`, settings UI |
| Results arriving as a flow, fast providers first, with a short hold so rows don't jump | `core/SearchEngine` |
| TalkBack: the list as a collection, selection announced, the field labelled | `overlay/OverlayUi` |
| Baseline profile, R8, release key | `app/` |

### 3.4 The window

- `OverlayActivity` is the launcher activity (a custom keyboard shortcut starts launcher
  activities), `singleInstance`, own task affinity, excluded from Recents, translucent theme with
  no start-up window and no system animation.
- The window is sized to the panel (`Gravity.TOP|CENTER_HORIZONTAL`, `y` = 20 % of the screen),
  blurred with `setBackgroundBlurRadius`, dimmed with `FLAG_DIM_BEHIND`. Its background drawable
  draws nothing and exists for its rounded outline, which the blur region follows.
- A second start arrives as `onNewIntent` and closes the panel.
- The process stays alive after the panel closes, so the app index, icons and history are warm.

### 3.5 Extensions (2.0, shaped now)

An app that wants to appear in Booklight exports a ContentProvider with the intent filter
`io.github.kuscher.booklight.EXTENSION`. Booklight finds them through `<queries>`, lists them in
settings (each off until the user turns it on), and calls `query` with the typed text. Rows come
back as a cursor with the Result fields; actions carry `Effect.StartIntent` URIs that Booklight
starts on the user's Enter. The engine's time budget applies. This keeps the trust model simple:
an extension can only show rows and name intents, and the user chooses what runs.

## 4. Errors

- A provider that throws or is slow is left out of that keystroke's list; the panel never waits.
- An effect that fails (the app was just removed, no browser) keeps the panel open and shows a
  short line in the footer; nothing is learned from a failed run.
- A corrupt history file is logged and replaced by an empty one.
- No blur: the solid surface. No Google Sans Flex: the system font.

## 5. Privacy

- Manifest permissions: none. `<queries>`: launcher activities, the Settings app, https viewers.
- Stored: `files/history.json` (result ids, typed text, counts, times) and preferences. Included in
  the user's device backup; "Forget everything" clears it.
- Sent: nothing. A web row hands text to the browser the user chose.

## 6. Testing

- **Core**: JUnit for the matcher (ordering, initials, camelCase, accents), the calculator
  (a table of inputs and non-inputs), history (latching, decay, round trip), the engine (order,
  learning, slow and broken providers), address detection. Run by `./bl test` and CI.
- **On the device**: `./bl debug` drives the panel without injecting input (`type`, `key`, `dump`,
  `find`, `shot`); `./bl screen` shows blur and placement. A release check list in
  `docs/RELEASING.md` from 0.2: cold and warm start times, the shortcut toggle, focus return,
  click outside, light and dark, blur off, a work-profile app.
- **House rules** for the HP are in CLAUDE.md (idle check, no `uiautomator`, the Debian VM).

## 7. Out of scope for 1.0

Files, contacts, clipboard history, a window list, live web suggestions, the assistant role, any
accessibility service, theming beyond the system's colours, phone layouts beyond "it works".

## 8. What changed after Alex's answers (1 October 2026)

This spec was written before he answered. Where it differs, this section and the code win.

- **Nothing typed** shows nothing: no suggestion strip (§2.1, §2.4 state 2). The first-run row became a
  small card under the field with two steps: the shortcut, then the opt-in for search suggestions.
- **Glass** (§2.2): flat and visibly see-through. A veil of white (light theme) or near-black (dark)
  over the system blur, in three levels the user picks from: Clear (veil 0.28 light / 0.38 dark, blur
  14 dp), Balanced (0.42 / 0.50, 22 dp, the default) and Frosted (0.58 / 0.64, 32 dp). The rest of the
  screen dims by 10 % in light theme and 22 % in dark. The edge is two flat rings: a black hairline, then
  a 1.25 dp white outline, even all the way round. No bevel, glow or highlights. Text is one ink
  (`onSurface`) at three strengths (1.0, 0.80, 0.60), so it follows whatever shows through. Marks are
  centred at 38 dp and text starts at 72 dp in every row. The selection pill is the only coloured
  surface. On arrival one gleam runs along the outline. Values: `overlay/Glass.kt`, `OverlayUi.kt`.
  Measured on the HP over a white page and a black terminal: titles 10:1 and 3.2:1 in light theme,
  5.6:1 and 15:1 in dark; the selected row stays above 4.5:1 everywhere. Small grey text over a window
  of the opposite colour is the price of the transparency; Frosted is there for that.
- **Motion** (§2.3) is everywhere, not only the selection: the glass comes into focus on arrival, the
  window's height follows a spring, rows rise in as a cascade, move to their new places and fade out, the
  selection pill stretches towards its target, results and actions pass each other sideways, an answer
  rolls. All specs are in `overlay/Motion.kt`; with the system's animations off, everything cuts.
- **The field** shows a G in place of the magnifier when Google is the engine.
- **Web** (§5): the app has the `INTERNET` permission for search suggestions, which are off until the
  user turns them on. Engines: Google, DuckDuckGo, Bing, Brave Search, Ecosia. Suggestions fill free
  rows below the web row and never move rows already shown.
- **Actions** for an app: Open, App info, Store page. No Uninstall action (it would need
  `REQUEST_DELETE_PACKAGES`; App info has the button).
- **The key**: the system only accepts shortcuts that include the Action key, one per app. Booklight
  suggests Action + Alt + Space, or Action + K.
- **Languages**: US English, British English, German.
- Results arrive in one step from the local providers (they answer in about 10 ms), then suggestions;
  the streaming engine in §3.3 was not needed.

