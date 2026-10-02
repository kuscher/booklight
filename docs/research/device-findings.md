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

## Speed, release build (R8, 1.0, 2.3 MB APK)

| Measure | Result |
| --- | --- |
| Cold start, `am start -W` TotalTime | 121 ms |
| Warm start, `am start -W` TotalTime | 61, 64, 69 ms |
| Warm start: `onCreate` → first frame | 18–24 ms |
| A search across all providers | 6–15 ms |

## Glass and motion (1.0)

- The window's height can follow a spring: `window.attributes.height` set on every frame resized the
  window smoothly at 60 fps, the blur region with it (checked frame by frame in a screen recording).
- `Window.setBackgroundBlurRadius` and `setDimAmount` can be animated per frame (the glass "comes into
  focus" over about 170 ms on arrival).
- An AGSL `RuntimeShader` draws the surface (tint, outline, grain) over the system blur.
- **See-through needs restraint in both tint and blur.** A surface-colour tint of 0.46 with a lit bevel
  over a 64 dp blur read as an opaque frosted slab (Alex: "I don't think the app is transparent at all").
  What reads as see-through is mostly the blur radius: at 22 dp shapes behind stay recognisable. A white
  veil (near-black in dark theme) gives the most contrast per unit of tint; 0.42 over a black terminal
  leaves titles at 3.2:1, which is why Balanced isn't thinner. `./bl backdrop` (debug builds) opens a
  window with a white page, a dark terminal and colour to judge it against without showing real windows.
- Apps live in Android users 0 and 10: `pm uninstall --user current` leaves the user-0 copy, and a
  build signed with another key then fails with INSTALL_FAILED_UPDATE_INCOMPATIBLE. `adb uninstall`
  removes both.

## Speed (0.1 debug build, not yet R8)

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
- **What the capture dialog accepts** (tried in the dialog for another app, then cancelled): Alt + Space
  and Ctrl + Space alone are ignored (the dialog has a fixed "Action +" in front); Action + Space says
  "Key combination already in use"; **Action + Alt + Space is accepted**. System actions can get an
  added shortcut the same way.
- **One custom shortcut per app**: once Booklight has one, its row shows a bin ("Remove shortcut?"),
  not a +. So "Alt + Space and also Action + K" is not possible; it is one or the other.
- The custom shortcut survived uninstalling and reinstalling Booklight (the entry stays in
  `dumpsys input` and works again once the app is back).

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

## How the panel can arrive (checked 1 October, HP and Lenovo Googlebook 15)

Tried with a throwaway build (local branch `spike/unfold`), recorded and stepped through frame by frame.

- **Resizing the window in width on every frame does not work.** The window's new position is applied at
  once, its new buffer arrives one or two frames later, and a gravity-centred window is repositioned with
  every width. Result on the HP: the left edge runs ahead, the right edge lags, the contents slide sideways,
  and the size only changes every second frame (30 of 60). Height alone is fine (1.0 does it for the list),
  because the top edge never moves.
- **A window that stays put, with the glass growing inside it, works.** The window is the panel's final
  rectangle from the first frame; a box inside it is laid out narrower and shorter and grows; the contents
  are laid out once at full width, centred, and only uncovered. 60 of 60 frames, symmetric, the text never
  moves on screen.
- **The blur is always the whole window.** On Android 17 the window's background stays the app's own
  drawable (no `LayerDrawable` with a blur layer to resize), and an outline with a smaller rectangle changes
  only the corner radius. A window 24 dp larger than the panel showed a blurred band around the panel at
  rest. So: the window must be exactly the panel, nothing can swing out past the panel's final edge (a
  bounce has to turn back inwards), and while the glass is still narrow the blur radius is kept at 0; it
  comes in over the second half of the opening.
- The Lenovo is 2880 × 1800 at 240 dpi (1 dp = 1.5 px), Android user 10, SDK 37; the same build behaves the same.

## Checked for 1.1 to 1.4 (1 October, HP; nothing was sent or changed)

- **Who answers which intent:** `mailto:` → Gmail; `CREATE_NOTE` → Keep; `SET_TIMER`, `SET_ALARM` → Google
  Clock; calendar `INSERT` (`vnd.android.cursor.dir/event`) → Google Calendar; `MEDIA_PLAY_FROM_SEARCH` →
  Spotify and others; `DELETE` / `UNINSTALL_PACKAGE` → the package installer's own confirmation; text files
  `VIEW` / `EDIT` → the desktop text editor; the Settings panels (volume, internet) exist; `geo:` is only
  answered by Zoom, so maps go through https links.
- **Protection levels here:** `REQUEST_DELETE_PACKAGES`, `SET_ALARM`, `ACCESS_NOTIFICATION_POLICY` normal;
  `WRITE_SETTINGS`, `SCHEDULE_EXACT_ALARM` app-op (a switch in Settings).
- **Files with no permission** (`./bl probe files`, debug builds): `Documents/Booklight/` can be created,
  written, appended to and given sub-folders; `Download/` too; a new top-level folder cannot.
- **Gemini:** `ACTION_SEND` with `text/plain` to `com.google.android.apps.bard/.shellapp.BardEntryPointActivity`
  opens Gemini with the text in the prompt, not sent. No launcher shortcuts are published by the Gemini app.
- **The app icon:** started from the system's Apps list, the panel opens and closes within about 0.2 s: the
  Apps list closing takes focus, and losing focus closes the panel. `Activity.getReferrer()` can tell an
  icon start from a keyboard-shortcut start.

## Rules for testing on this device

- **Never run `uiautomator dump`.** UiAutomation suspends every accessibility service. On 1 October it
  unbound BentoBar, Welcome's pointer service and StudioSnap; BentoBar 0.5 crashed in
  `BarService.onUnbind` twice, Android showed "BentoBar keeps stopping", and its strip stayed away
  until its entry was removed from and re-added to `enabled_accessibility_services`. Read system
  screens from `screencap` crops and `dumpsys window windows` instead.
- Opening the panel takes the keyboard from whoever is typing. Check `./bl idle` first and keep each
  test short. Key and tap injection only into Booklight's own focused window.
- The Debian VM (Terminal app) is off limits: no launch, no force-stop, no reboot, no adbd changes.
- `am task resize` only on a task whose own line in `dumpsys activity activities` names Booklight. On 1 October a
  helper took the wrong task id from that dump and resized another app's window (it was put back from the bounds the
  dump still held).
- A timer started for real keeps ringing in Clock after it ends. After any test that presses Enter on a timer:
  `adb shell am start --user current -a android.intent.action.DISMISS_TIMER` (it stops every expired timer and
  opens nothing).
- `./bl screen` crops a real screenshot, so it shows whatever is behind the panel: never publish
  one. `./bl shot` (PixelCopy of Booklight's own window) is the one for docs.

## The pinned window (Lenovo Googlebook 15, 1 October 2026)

The spec asked for a small window in Android 17's pinned layer with the keys staying in the app the user was in,
and said to check that on a device. What the device does:

- **The pinned layer takes the keys.** `AppTask.requestWindowingLayer(WINDOWING_LAYER_PINNED)` from a focused
  window answers 0 (granted) with `USE_PINNED_WINDOWING_LAYER`, and the window stays on top. But it is the top
  task of the desk: it has the keys when it opens, **and it gets them again every time the window in front of
  it closes**, the panel included (pin a note, click the app, open the panel, Esc: the keys are in the pin).
  An app cannot hand them back: `FLAG_NOT_FOCUSABLE` on the pin leaves no window focused at all
  (`mCurrentFocus=null`, the pin still the focused app), and `moveTaskToBack` minimises it.
- **A picture-in-picture window never takes them.** `enterPictureInPictureMode` from the same activity: the
  keys are back in the app behind at once and stay there when the panel closes later (`mode=pinned`).
- **Its size is the app's to say, through the manifest.** By default the system opens it at about a sixth of
  the screen (645 × 295 dp for a shape of 2.19 to 1). With `<layout android:minWidth="280dp"
  android:minHeight="118dp">` on the activity it opens at exactly 280 × 118 dp, 280 × 140 for a taller shape,
  219 × 219 for a square: the smallest size is the opening size.
- **The system does not reshape it**: `setPictureInPictureParams` with another aspect ratio left the window as
  it was. A pin of another shape needs a new window.
- **A tap or the pointer shows the system's controls**: settings, expand, close. The app's own buttons
  (`setActions`) are shown only on a window some 220 dp high, not on one of 118 or 140.
- **Getting there:** the activity must be an ordinary window first. `ActivityOptions.makeLaunchIntoPip` from the
  panel opened an ordinary window. The system then takes it on top its own way (the window goes, a card with
  the app's icon lands, about 0.5 s); the source rectangle hint did not change that.
- **Opening an ordinary window of one's own at a size:** a single-task activity started while another Booklight
  window is the top desktop window is given that window's bounds ("inheriting bounds from existing closing
  instance" in the log), whatever `setLaunchBounds` says. With `FLAG_ACTIVITY_MULTIPLE_TASK` the bounds are
  taken. A desktop window is never lower than 220 dp; its caption is 40 dp (48 in the pinned layer).
- Not checked: the HP; a video already in picture-in-picture (the system keeps one).

## A shadow around the panel (Lenovo Googlebook 15, 1 October 2026)

Alex asked for a wide, soft shadow around the panel that never lies under the glass. 1.0 had found that the
window's elevation gives no shadow once the window blurs; this is why, and what works.

- **The system makes room for a window's shadow outside the window.** `Window.setElevation(z)`, called once the
  window's root view exists (before that the theme's value replaces it), gives the window surface insets of
  2 × z pixels on every side (`dumpsys window`: `surfaceInsets=Rect(96, 96 - 96, 96)` for 32 dp). **The blur
  stays inside the window's frame**: nothing around the panel is blurred. So the window is still exactly the
  panel, and the shadow lies in room the blur does not reach.
- **The root view casts no shadow by default** because its outline comes from the window's background, and with
  blur on that outline has alpha 0. A `ViewOutlineProvider` of our own on the root view (a round rect, alpha 1)
  brings the shadow back.
- **Its strength** is the theme's `ambientShadowAlpha` and `spotShadowAlpha` (the defaults darken a white page
  by under 2 %: invisible) times the alpha of `outlineAmbientShadowColor` and `outlineSpotShadowColor`. With the
  theme's two at 1.0 the colours' alpha is the strength. Its **size** is the elevation. Measured over a white
  page: 72 dp and spot 0.14: 15 % under the lower edge, gone 30 dp out; 96 dp and 0.24: 24 %, 53 dp; 128 dp and
  0.36: 33 %, 87 dp. The sides get about half of that and the top edge 2 to 3 %: the system's light is above.
- **The root view cannot draw into that room itself** (a rectangle drawn outside its bounds from the window's
  background did not show): only the system's shadow gets there.
- **Under the glass:** the shadow is drawn before anything of the window. The window's background drawable
  clears the glass's own shape first (`PorterDuff.Mode.CLEAR`), and the glass measured the same with the shadow
  on and off (254 against 254 of 255).
- The outline can be made again every frame (`invalidateOutline`), so the shadow is cast from the glass as it
  opens and goes with it as it closes.
- Not checked on the HP.

## The on-device model (Lenovo Googlebook 15, 1 October 2026)

Checked with a throwaway debug build (`com.google.mlkit:genai-prompt:1.0.0-beta4`), from Booklight's own process,
on the user build (`ruby`, `release-keys`).

- **AICore serves an ordinary app.** `Generation.getClient().checkStatus()` said 1 (downloadable), then 3
  (available) once the system's service had fetched the model. Base model `nano-v3`, token limit 8,192. Structured
  output, system prompt and thinking mode report unavailable; the FAST and PREVIEW variants report unavailable.
  Features on the device: `android.hardware.npu`, `com.google.android.feature.AICORE_INTEL`, `…AICORE_INTEL_PTL`,
  `com.google.desktop.AICORE_CROS_GPU`. AICore `0.release.intel.prod_aicore_20260820.00_RC08`.
- **Speed.** `warmup()` about 2 s the first time, tens of ms after. First word after about 0.3 s once loaded; 70 to
  100 characters a second. Translation both ways, a grammar fix, a rewrite, a two-sentence explanation and a JSON
  extraction took 0.7 to 3.7 s and were right; a summary turned "Googlebooks" into "Google Books".
- **Only from the app in front.** Asked from a broadcast receiver the download call never answered and the receiver
  ended in an "isn't responding" dialog; asked from an activity in front everything worked. The model download
  itself (about 1.5 GB by the disk's free space) is AICore's and went on in the background.
- **The library's usage reporting can be taken out.** As shipped, the library schedules
  `com.google.android.datatransport…JobInfoSchedulerService` in Booklight's process and adds `ACCESS_NETWORK_STATE`.
  With `TransportBackendDiscovery`, `JobInfoSchedulerService` and `AlarmManagerSchedulerBroadcastReceiver` removed
  in the manifest (`tools:node="remove"`) and that permission removed, the model answered as before, logcat said
  "Transport backend 'cct' is not registered", and no job was scheduled. The one permission left is
  `com.google.android.apps.aicore.service.BIND_SERVICE` (normal).
- **The system's translator is not offered to apps.** `TranslationManager.getOnDeviceTranslationCapabilities` lists
  552 pairs, but `createOnDeviceTranslator` returns null. **The system's text classifier is**: `detectLanguage`
  ("de" 0.998) and `generateLinks` (a date and time, a link, a flight number).
- **Classic ML Kit translation** works on x86_64 (a 60 s pack download, then offline), with visibly worse German
  than the model ("Das Treffen zog an Donnerstag").
- The HP is unchecked.

## A picture to the model, and the clipboard's description (Lenovo Googlebook 15, 1 October 2026)

A throwaway debug hook, not kept: the Prompt API with an `ImagePart` and a question, the panel in front;
and `ClipboardManager.getPrimaryClipDescription()`.

**The model takes a picture.** `nano-v3`, `genai-prompt` 1.0.0-beta4, temperature 0.2.

| Picture | Asked | Tokens (picture and question) | First word | Whole answer | Answer |
| --- | --- | --- | --- | --- | --- |
| A table, 1200 × 600, text 44 px | Which city has the lowest price? | 279 | 2.0 s (the first request) | 2.4 s | Right ("Lisbon … €98") |
| A build error in a terminal, 1600 × 900, text 28 px | What is the error, in which file and line? | 278 | 0.7 s | 7.0 s | Right: the message, the file, line 42:17, and what it means |
| A 1200 × 700 piece of a real screenshot (the test backdrop's terminal) | How many tests, how many failures? | 279 | 0.7 s | 1.9 s | Right ("36 tests", "0 failures") |
| The whole screen, 2880 × 1800 | What does the terminal say? | 281 | 0.7 s | 13.9 s | **Wrong.** It says there are error messages and the word FAILURE; there are none. It cannot read the text and makes some up |
| The whole screen | Describe it in two sentences | 276 | 0.7 s | 4.6 s | The layout is right (a browser, text left, a console right), the details are not |

- A picture costs about 256 tokens whatever its size: the model sees it small. So a picture of **a region or one
  window** (up to about 1600 px wide, text from about 28 px) can be asked about as it is; **a whole screen cannot**:
  it needs its text read first (OCR), or the user to frame what is meant.
- It answers in Markdown (`**bold**`, lists) unless told not to, and a long answer takes its time (7 to 14 s for a
  paragraph). The row needs "one or two sentences, plain text" in the prompt.
- First word after 0.7 s with a picture (0.3 s for text alone).

**The clipboard says what it holds without being read.** With the panel in front,
`getPrimaryClipDescription()` gave, for a text another call had just set:

| Copied | Type | Age | Entity scores (0 to 1) |
| --- | --- | --- | --- |
| "Dinner with Anna on Friday at 7pm at Borchardt, Französische Straße 47, 10117 Berlin. Call +49 30 81886262 or see https://…" | `text/plain` | 2.6 s | url 1.0, phone 1.0, datetime 1.0; address 0.0 (the German address was not found) |
| "LH 454 lands 14:05" | `text/plain` | 2.6 s | flight 1.0, datetime 1.0 |
| "just some plain words without anything in them" | `text/plain` | 2.6 s | all 0.0 |

`getClassificationStatus()` was complete (3) each time, two seconds after the copy. `getTimestamp()` gives the
age. So a row "there is a fresh copy, with a link and a date in it" can be shown without reading the text, and
so without the system's "pasted from your clipboard" message. Not tried: a picture on the clipboard, and
whether the Googlebook's screenshot puts one there.

**Timers started in a test ring until they are stopped.** Six `timer … tea` countdowns started while testing the
pin were still ringing in Clock two hours later. `adb shell am start --user current -a
android.intent.action.DISMISS_TIMER` stops every expired timer without opening Clock. Do that after any test
that presses Enter on a timer.

## Why Chrome's "New tab" is not offered (Lenovo Googlebook 15, 1 October 2026)

Alex: "it doesnt show the actions for apps like chrome which has new tab as one."

- Booklight reads the shortcuts an app declares in its manifest and shows the ones it may start as rows
  (`providers/AppCommands.kt`): on the Lenovo "New event" (Calendar), Keep's four, YouTube's three, Play Store's
  "My apps".
- Chrome has two (`dumpsys shortcut`): "New tab" (manifest) and "New Incognito window" (dynamic). Both lead to
  `org.chromium.chrome.browser.LauncherShortcutActivity`, and that activity is **not open to other apps**
  (`./bl debug activity com.android.chrome/org.chromium.chrome.browser.LauncherShortcutActivity` says
  `exported=false`). `AppCommands.safe()` drops such a shortcut, rightly: starting it would throw. Gmail shows
  none for the same kind of reason (not looked at in detail).
- Who may start it: the system, on behalf of an app that is a "shortcut host". In the Android 17 source
  (`ShortcutService.hasShortcutHostPermission`, `VoiceInteractionManagerService`) that is the home screen app,
  holders of a signature permission, and **the package of the device's digital assistant**
  (`setShortcutHostPackage` is called for the current voice interaction service). So as the assistant, Booklight
  could list every app's shortcuts, the dynamic ones too, and start them through `LauncherApps.startShortcut`.
  Read in the source, not tried on a device.
- Without that: Booklight's own "New window" on Chrome's row opens a new Chrome window with a new tab, and a
  typed address or search opens in a new tab already. Chrome has no public way to ask for an empty new tab
  (it ignores its own `chrome://` addresses from outside).

