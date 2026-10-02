# The Booklight window as a desktop window: how it is built

*1 October 2026. The design is `docs/design/window-redesign.md` (its §10 decisions are taken as recommended) and the
drawn layouts are `docs/design/booklight-window.html`. This file is the order of work: four steps, each a commit that
builds and runs. All sizes are dp.*

## Ground rules

- The panel (`overlay/`) is not changed. The window uses its parts where the design says so (`Stage`, key caps).
- `material3` stays at 1.5.0-alpha29. One new library: `androidx.compose.material3.adaptive:adaptive` 1.3.0. No new permission.
- Everything Booklight draws itself moves on `overlay/Motion.kt`; Material's parts keep the theme's motion scheme.
- Every string is a resource in English and German. Keys that still apply are kept.
- Keys stay in one place: the window's root has the keyboard and a key handler; Material's parts are not focus
  stops of their own (text fields and menus excepted). The `Page` registry knows the rows, their order and their
  bounds; one ring is drawn from it.
- Check after every step: `./bl test`; `./gradlew :app:assembleDebug` with the output searched for `FAILURE` and `e:`;
  install on the Lenovo Googlebook; `./bl wshot` at compact, medium and expanded widths, light and dark, English and
  German; every capture looked at in full size.

## Step 1. The frame

| Task | Files | Check |
| --- | --- | --- |
| The library, and the window's minimum size (400 × 480) | `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml` | It builds; the merged manifest has no new permission; the desktop does not let the window go narrower |
| Size classes: compact under 600, medium to 839, expanded from 840 (`currentWindowAdaptiveInfo`), and the measures that follow from the width | `window/Frame.kt` (new) | Captures at 520, 720, 1053 |
| The rail on the leading edge: Material's `WideNavigationRail`, 96 collapsed and 220 expanded, no fill; under 600 `ShortNavigationBar` at the bottom. One indicator of Booklight's own travels between the items on `lead` and `trail`; Material's own indicator is switched off | `window/Nav.kt` | The rail touches the window's edge; one highlight; it travels |
| The column starts 24 from the rail (16 from the edge in compact), is at most 720 wide and never centred | `window/MainActivity.kt` | The title is in the same place in every section and at 1053 and 1840 |
| The caption bar is transparent and its marks follow Booklight's theme; the rail and the page start under it | `window/MainActivity.kt` | One surface from the top edge, light and dark |
| A choice stands under its text when its row is narrower than 560 | `window/MainActivity.kt` (`Choice`) | Look at 520: no word is broken |
| Ctrl + 1 … 7 | `window/MainActivity.kt` | Injected keys |

## Step 2. Five sections

| Task | Files | Check |
| --- | --- | --- |
| `Part`: Start, Commands, Look, Results, Privacy; their names in both languages | `window/Nav.kt`, `res/values*/strings_20.xml` | Rail in English and German |
| Pages are built from groups (`Group { }`): a heading and its rows, so a row knows its place in its group. The pages move out of the activity's file | `window/Page.kt`, `window/Pages.kt` (new) | Arrow order unchanged |
| Start: the demo; "Open Booklight" (key status, Keyboard shortcuts, the assistant key; the two free shortcuts only while no key works); Tips | `window/Pages.kt` | With `pref key seen` and `pref key no` |
| Commands: Built in · Yours, Find (Ctrl + F, and letters typed on the page), New link with an arrow for snippet, recipe, prompt (Ctrl + N) | `window/Commands.kt` | Find narrows the half that shows and says how many the other half has; a new item's editor opens at the end of its group and the page follows it into view |
| Privacy: what Booklight may use, what it keeps, Forget everything last, the policy, the closing line | `window/Pages.kt` | Forget still asks twice |
| The window remembers its section; a first run opens on Start | `data/Prefs.kt` (`Settings.windowPart`), `window/MainActivity.kt` | Close, open: the same section |
| The panel's two ways in: `page=commands` opens Commands with the keys in Find; `edit` opens Commands on Yours with that item open | `window/MainActivity.kt` | `am start` with the extras, and the panel's own rows |

## Step 3. Rows and controls

| Task | Files | Check |
| --- | --- | --- |
| Colour roles: ground `surfaceContainer`, rows `surfaceBright` (added to Booklight's own two schemes), second text `onSurfaceVariant`, selection `secondaryContainer` | `ui/Theme.kt`, `window/*` | Light and dark, wallpaper colours and Booklight's own |
| Type: title `headlineMedium` 28/36 at 600 in the rounded cut, lead and rows `bodyLarge`, second lines `bodyMedium`, headings `titleSmall` in sentence case | `ui/Theme.kt`, `window/*` | Captures |
| A row is Material's `SegmentedListItem` in its group's shape; hover and pressed are Material's; 2 between rows, 24 between groups | `window/Page.kt` | Hover and press seen on the device |
| One focus ring for the window, 2 wide in `secondary`, gliding on `lead` and `trail`; shown when keys move it, put away by the pointer | `window/Page.kt`, `window/MainActivity.kt` | Never two rings; Material's parts draw none |
| `Switch`; connected `ToggleButton`s for a choice of two to four; a button with a `DropdownMenu` for the search engine | `window/Controls.kt` (new) | Each worked by the row's keys and by the pointer |
| The editors: `TextField`s, `Button` Save, `TextButton` Cancel and Delete | `window/Commands.kt` | Tab order inside an editor; Escape closes |
| Right-click menus (built-in command: Try it, Copy example; yours: Edit, Duplicate, Delete; an app: Show, Hide); tooltips on icon-only buttons; a 4 dp scrollbar that can be dragged | `window/Page.kt`, `window/Scrollbar.kt` (new) | On the device with injected pointer events where possible |
| The key table of §6: Home, End, Page Up, Page Down, Delete, Escape, Ctrl + W, F6 | `window/MainActivity.kt` | Each key injected |
| Look's explanations in one line each | `res/values*/` | Captures in both languages |

## Step 4. The second pane and the polish

| Task | Files | Check |
| --- | --- | --- |
| `Stage` takes a size and the user's glass and shadow: the desk blurred inside the panel's outline, the veil of the chosen level, a drawn shadow | `window/Stage.kt` | Against the real panel at each glass level |
| The preview on Look (720 × 212 at the top of the page), the demo on Start (240 high) | `window/MainActivity.kt` | Captures |
| From 1332 a second pane, 24 from the column, 320 to 560 wide: Look's preview, Start's demo, the open editor on Commands (its row selected), a built-in command's example | `window/MainActivity.kt`, `window/Commands.kt` | Captures at 1440 and 1840 |
| Motion of §7: the second pane fades in and slides 24 from the trailing edge; bar and rail cross-fade at 600; the rail widens at 840 with Material's own animation; an editor in the second pane fades in and rises 12 | `window/MainActivity.kt`, `window/Nav.kt` | A recording of the Booklight window stepped through; `Motion.on` off cuts everything |
| The Keyboard Shortcuts Helper lists the window's keys | `window/MainActivity.kt` (`onProvideKeyboardShortcuts`) | The helper opened from the window |
| The nine device checks of §9, the keys of §6, captures for the reviewers, the notes | `docs/research/device-findings.md`, `docs/design/captures/window-redesign/`, `CLAUDE.md`, `docs/design/design-system.md`, `CHANGELOG.md`, `docs/PICKING-UP.md` | |

## Where the build may have to leave the design

Each of these is taken as the design says first; what the device or the library forces is written into
`docs/research/device-findings.md` and the status notes.

- Material's expanded rail item draws its indicator round the mark and the name, not across the rail (the mock-up
  drew it across). The travelling indicator takes Material's shape, so hover and selection are the same shape.
- Material's `ButtonGroup` composable adds an overflow menu and squeezes neighbours on a press. A connected group
  here is a row of `ToggleButton`s with `ButtonGroupDefaults`' connected shapes, as the library's own sample does.
- The preview's glass is a blur of a drawn desk, not the system's window blur. If it does not look like the real
  panel on the device, the preview shows theme and colours only and Look keeps a line of explanation (§9 of the design).
