# Device findings: HP Googlebook 14

Checked on Alex's HP Googlebook 14 (Android 17, SDK 37, build (build),
1920 × 1200 at 180 dpi, Android user 10) on 1 October 2026 with Booklight 0.1 debug builds
over Wi-Fi adb. Each item says how it was checked. Desk research is in
[android-platform.md](android-platform.md); where the two differ, this file wins.

## The panel as a window

| Finding | How it was checked |
| --- | --- |
| A `windowIsTranslucent` activity in its own task is **not** made a desktop window. Its task is `mode=fullscreen translucent=true`: no caption bar, no resize handles, the desktop and its windows stay visible behind it. No permission was held (in particular not `SYSTEM_ALERT_WINDOW`). | `dumpsys activity activities`, screenshots |
| The window can be smaller than the screen and placed freely inside that task: `Gravity.TOP \| CENTER_HORIZONTAL`, `y = 20 %` of the screen, 810 × 77 px (720 × 68 dp) gave frame `[555,240][1365,317]`. | `dumpsys window windows` |
| Changing `window.attributes.height` as rows arrive resizes the window in place (77 → 455 px) with no visible jump of the top edge. | screenshots between `./bl debug type` calls |
| Cross-window blur is on: `ro.surface_flinger.supports_background_blur=1`, `WindowManager.isCrossWindowBlurEnabled()` is true. `Window.setBackgroundBlurRadius(40 dp)` blurs what is behind the panel, and the blur region takes its corner radius from the background drawable's outline (a custom `Drawable` whose `getOutline` is a round rect). | `./bl screen` |
| `FLAG_DIM_BEHIND` with `dimAmount = 0.16` dims the rest of the screen, status bar and taskbar included. | `./bl screen` |
| The decor view's elevation gives no visible shadow once the window blurs (the background becomes a `LayerDrawable` whose outline has alpha 0). The panel uses a hairline border instead. | `./bl screen` |
| The window is touch-modal: a touch anywhere outside it arrives in `Activity.onTouchEvent` with out-of-bounds coordinates, so one click outside closes the panel and does not reach the window underneath. | `input tap 150 700` |
| When the panel closes, focus returns to the window that had it before (the app in front stayed focused through every test). | `dumpsys window \| grep mCurrentFocus` |
| `excludeFromRecents` + `singleInstance` + its own `taskAffinity`: a second start while the panel is up arrives as `onNewIntent` ("intent has been delivered to currently running top-most instance"), which Booklight uses as the toggle. | `./bl open` twice |
| Settings (an ordinary activity started from the panel) opens as a normal freeform desktop window (`mode=freeform`). | `dumpsys activity activities` |

## Speed (debug build, not yet R8)

| Measure | Result |
| --- | --- |
| Cold start, `am start -W` TotalTime | 339 ms |
| Cold start through the keyboard shortcut: process start → first frame | 257 ms |
| Warm start (process cached), `am start -W` TotalTime | 78, 90, 91 ms |
| Warm start through the shortcut: `onCreate` → first frame | 43 ms |
| App index: 80 launchable activities across profiles | read once at process start |
| A search across all providers | 5–16 ms (first ones include JIT warm-up) |

## The keyboard shortcut (no permission)

- **Keyboard shortcuts** (Action + /) → **App shortcuts** → **Add shortcut** switches to *Customize
  shortcuts* and lists every launchable app with a **+**. The search field narrows it ("Booklight").
- **+** opens "Booklight. To create this shortcut, press the Action key and one or more other keys
  together" with **Cancel** / **Set shortcut**.
- After Action + K, `dumpsys input` shows under *Custom Gestures, UserId = 10*:
  `KeyTrigger{KEYCODE_K, META} → Action[keyGestureType=51, appLaunchData=ComponentData{io.github.kuscher.booklight,
  …overlay.OverlayActivity}]`. So a custom shortcut starts the app's **launcher activity**, which is why
  the panel is the launcher activity.
- Pressing the shortcut opens the panel from any app; pressing it again closes it.
- `Activity.requestShowKeyboardShortcuts()` opens that helper from Booklight's settings. It can't
  preselect an app or a category.
- Taken Action combinations on this build: A B C E F G H I L N P Q S U V W, Space, Tab, Enter, Esc,
  /, -, =, [ ], arrows, Backspace, and several Ctrl/Alt/Shift variants (list: Welcome's
  `device notes` §2.6). Free letters: D J K M O R T X Y Z.
- Up to 10 custom app shortcuts per user (SystemUI strings; not counted here).

## Other triggers that need no permission (seen in Settings; not exercised)

- Touchpad **three-finger tap → Open another app**, and **corner shortcuts**.
- Pinning Booklight to the taskbar; the Apps list.
- The digital assistant role would give Action + Space, the Assistant key and the status bar's
  assistant chip, but it replaces Gemini there (`ACTION_ASSIST` handlers present on this unit: the
  Google app, ChatGPT, Firefox). Kept for the "advanced" tier.

## Data without permissions

- `LauncherApps.getActivityList` for every profile, with only the manifest's `<queries>` MAIN/LAUNCHER
  intent: 80 activities, icons through `LauncherActivityInfo.getIcon`.
- Google Sans Flex is readable at `/product/fonts/GoogleSansFlex-Regular.ttf` (variable, with the
  `ROND` axis), so the panel uses the device's own font without bundling it.
- `android.settings.*` actions resolve with `<queries><package android:name="com.android.settings"/>`.

## Rules for testing on this device

- **Never run `uiautomator dump`.** UiAutomation suspends every accessibility service. On 1 October it
  unbound BentoBar, Welcome's pointer service and StudioSnap; BentoBar 0.5 crashed in
  `BarService.onUnbind` twice, Android showed "BentoBar keeps stopping", and its strip stayed away
  until its entry was removed from and re-added to `enabled_accessibility_services`. Read system
  screens from `screencap` crops and `dumpsys window windows` instead.
- Opening the panel takes the keyboard from whoever is typing. Check `./bl idle` first and keep each
  test short. Key and tap injection only into Booklight's own focused window.
- The Debian VM (Terminal app) is off limits: no launch, no force-stop, no reboot, no adbd changes.
- `./bl screen` crops a real screenshot, so it shows whatever is behind the panel: never publish
  one. `./bl shot` (PixelCopy of Booklight's own window) is the one for docs.
