# Booklight: notes for working on the code

A Spotlight / Alfred-style launcher for Googlebooks (Googlebook OS = Android 17 desktop): a key
opens a small glass panel over the desktop; type, Enter. Plain APK, Kotlin + Jetpack Compose,
Material 3 Expressive (material3 1.5.0-alpha, pinned). **No permissions at all in the manifest**
(the user's hard requirement for 1.0; adding one needs his say-so and a place in docs/PLAN.md §6).

## Read first
- Plan and open questions: `docs/PLAN.md`. Design for 1.0: `docs/superpowers/specs/2026-10-01-booklight-design.md`.
- Visual contract: `docs/design/booklight-plan.html` (an interactive prototype of the panel).
- Facts: `docs/research/device-findings.md` (checked on the HP; wins over the desk research),
  `android-platform.md`, `launchers.md`.
- Status: `docs/PICKING-UP.md`.

## Layout
- `core/` — pure Kotlin (no `android.*`), tested with JUnit on the Mac. `./bl test`.
  - `Model.kt` Query, Result, Action, Effect (what an action does, as data), Icon, Provider.
  - `Matcher.kt` scores typed text against a name (start, later word, initials, inside, scattered).
  - `History.kt` what was picked for which typed text (latching) and how often (frecency, 4-week half-life).
  - `SearchEngine.kt` asks every provider, merges, ranks, learns. `Calc.kt` sums. `Web.kt` address detection.
- `app/` — Compose app, package `io.github.kuscher.booklight`.
  - `BooklightApp` the process: providers, engine, history store, icon cache. **Register a new provider here.**
  - `overlay/OverlayActivity` the panel's window (the launcher activity), `OverlayModel` its state,
    `OverlayUi` the Compose panel and `Metrics` (sizes; the window is sized from these).
  - `providers/` `AppsProvider` (LauncherApps index), `Providers.kt` (sums, settings pages, commands, web).
  - `Executor.kt` performs effects: the only place that starts activities or writes the clipboard.
  - `data/HistoryStore` history as `files/history.json`. `settings/SettingsActivity`. `ui/` theme, symbols, app icons.
  - `DebugReceiver.kt` adb hooks (DUMP-guarded).
- `bl` — the helper script (its header lists every command).

## Dev loop
- `./bl app` builds, installs and opens the panel on the Googlebook. `./bl test` runs the core tests.
- `./bl debug type TEXT | key up|down|tab|esc|enter | dump | find TEXT | apps | close | forget`
  drives the panel without injecting input. `./bl shot NAME` = PNG of the panel's own window.
- adb: from the Mac (`~/Library/Android/sdk/platform-tools/adb`; the first non-emulator device or
  `$ANDROID_SERIAL`), or VSCodeBook's Unix socket inside the Googlebook's Linux VM. Android user 10
  → `--user current` everywhere. `local.properties` (not committed): `sdk.dir=$HOME/Library/Android/sdk`.
- Release key: `~/.config/booklight/keystore.jks` + `keystore.pass` (not made yet; debug builds use
  the debug key until then).

## Device rules (Alex's HP Googlebook 14)
- **Never touch the Debian VM**: don't launch or force-stop the Terminal app, no `vm` commands, no
  reboot, no adbd or Wireless-debugging changes.
- **Never run `uiautomator dump`**: it suspends every accessibility service and crashed BentoBar
  (docs/research/device-findings.md). Read system screens from `screencap` crops.
- Opening the panel takes the keyboard. `./bl idle` first; keep tests short; inject keys and taps
  only into Booklight's own focused window.
- `./bl screen` shows what is behind the panel (other people's mail). Never commit or publish one;
  use `./bl shot`.
- Action + K is bound to Booklight on the HP (Keyboard shortcuts → App shortcuts). It was added
  for testing on 2026-10-01; whether it stays is PLAN.md question 13.

## Design rules
- One panel, one ranked list, row one selected. Enter runs it. No groups, no tabs, no toolbars.
- The panel appears and goes without system animation; only the selection moves on a spring.
- The window is exactly the panel: never WRAP_CONTENT, never bigger than what is drawn (the blur
  region and the click-outside test are the window's bounds).
- Every user-facing string is a resource. Copy is plain: "Open", "Copy", "Search the web".

## Gotchas
- Panel sizes live in `Metrics`; `Metrics.height(model)` must match what `Panel` draws, or the
  window clips the list.
- Hover selects only on pointer movement (rows appear under a resting pointer as the list grows).
- A constant alone is not a sum (`e`, `pi`): otherwise typing "e" shows 2.718 instead of apps.
- `LauncherApps.startMainActivity` is used for every launch so work-profile apps open too.
- The app list skips Booklight itself.
- Gradle needs the memory settings in `gradle.properties` (Compose + material3 alpha).
