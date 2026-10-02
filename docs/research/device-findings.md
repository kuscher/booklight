# Device findings: HP Googlebook 14

Checked on Alex's HP Googlebook 14 (Android 17, SDK 37,
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
  device notes §2.6). Free letters: D J K M O R T X Y Z.
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
  assistant chip, but it replaces Gemini there (several other apps on this unit answer
  `ACTION_ASSIST` as well). Kept for the "advanced" tier.

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
  comes in over the second half of the opening. **Corrected later the same day:** the blur is the window's
  *root view*, and that view can be framed to the glass. See "The blur follows the glass" below.
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

## A copy: what the system says about it, and what it finds (HP Googlebook 14, 2 October 2026)

The checks M1 (the copy) asked for, run with a small helper app that copies a text the way any other app does,
and Booklight's panel in front.

- **Looking is silent, reading is announced once.** `getPrimaryClipDescription()` (kind, age, what was found, the
  private flag, the label) shows nothing. `getPrimaryClip()` on another app's copy makes the system put up one
  toast, "Booklight pasted from your clipboard"; a second read of the same copy makes none. So the line under
  the empty field costs no message, and Tab costs one.
- **The system looks at short copies only, and quickly.** Up to 400 characters: `getClassificationStatus()` is
  complete about 170 ms after the copy. Over 400 characters it is "not performed", always; such a copy can only
  be called "text" before it is read.
- **A copy marked private says so** (`EXTRA_IS_SENSITIVE` in the description's extras), and a copy Booklight
  made carries its label. Both are readable without reading the content: neither gets a line.
- **What the classifier finds after the read.** The copy's item comes with the system's `TextLinks` for short
  texts. Asked directly (`TextClassifier.generateLinks`), it took 20 ms for 400 characters, 41 ms for 2,000 and
  80 to 108 ms for 8,000; it refuses more than 10,000. Links, dates and times, phone numbers and mail addresses
  were found in every test text, and **street addresses were found on the HP** (they were not on the Lenovo the
  day before). A flight is an entity of its own with the action "Track": found for `LH 455`, `LH455`, `lh455`
  and `UA 90`, and also for `PS5` and `H264`, which are not flights; not for `U2 8001`.
- **The language of a text** (`detectLanguage`): 3 to 14 ms. A clear German sentence scored 0.98; a sentence
  with English names in it 0.66 and 0.57; a few words, or German written without its umlauts, "unknown". A
  threshold of 0.5 keeps the mixed sentences.
- **What answers.** `tel:`, `geo:`, an `https` maps address and `mailto:` each have exactly one app that answers
  on the HP.
- **The HP's model answers, though the system calls it "downloadable".** `checkStatus()` says `DOWNLOADABLE`
  there, which reads as "not fetched", and so every prompt's row said "Gemini" and handed over. Asked all the
  same, the model answered at once: a German sentence came back in English within a quarter of a second,
  streamed, and "teh quick brown fox" corrected. `warmup()` returns in 32 ms either way and proves nothing; the
  status stays `DOWNLOADABLE` after an answer. So Booklight now tries a model in that state once (a question of
  two words, four tokens back) and treats it as ready if it answers (`OnDevice.answers`). Nothing was
  downloaded: "Get the on-device model" was not pressed, and what it would fetch on the HP is not known.
- **Translations, on the HP.** One German sentence through `tr` into English, Danish, French, Spanish, Italian,
  Swedish, Norwegian, Polish, Dutch, Portuguese and Japanese: each came back as one clean sentence in that
  language, without an introduction. (Read by someone who knows only some of them: none looked wrong; nobody
  fluent has checked.) A short sentence is answered within a quarter of a second and streams.
- **A long text.** A German text of 1,914 characters was translated whole in 36 seconds: first words within
  four seconds, then about 55 characters a second. That was 1,847 characters out, close to the 512 tokens the
  answer was then limited to. So a row now takes at most 1,800 characters of prompt and text for a rewrite or a
  translation (a summary may read 8,000), the answer may be 768 tokens, and it is given a minute; a longer text
  hands over to Gemini instead of coming back cut.
- **The Lenovo, the same checks** (2 October): the line, Tab and the answer in the row behave as on the HP; the
  three translations tried (Danish, Polish, Japanese) came back word for word the same, and a text of 1,489
  characters was translated in 21 seconds. Two differences. No app answers `tel:` there, so a phone number's
  row offers Copy only. And its classifier called the postcode in "10117 Berlin" a phone number, ahead of the
  real one: the row now takes the first candidate with seven digits or more (`Clip.pick`). The street address
  was again not found there.
- **Reading the HP's log.** It writes some 20,000 lines in 2.5 seconds, so a line is gone from `logcat -d` before
  it can be read: a probe's answer has to be read from a stream that was started before the probe.

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

## The blur follows the glass (Lenovo Googlebook 15, 1 October 2026)

Alex: "why does the blur just pop in when booklight opens? is there a chance to have it from the getgo or fade
in during animation timed vs just sort of becoming blurred randomly?" Until then the blur's radius was held at 0
while the glass grew and came in over the last stretch, because the blur was taken to be the whole window.

- **What the blur's region really is** (Android's source, `DecorView` and `BackgroundBlurDrawable`, read in the
  SDK's 36.1 sources; the Lenovo behaves the same). `Window.setBackgroundBlurRadius` puts a blur layer under the
  window's background, inside the root view. `getBackground()` on that view still answers the app's own
  drawable, which is why 1.0's check found "no `LayerDrawable`". The region sent to the compositor is **where
  that layer's render node is drawn**: the root view's rectangle. It is reported for every frame and travels in
  the same transaction as that frame's buffer. Its corner radius is read before each frame from the background's
  outline.
- **So the root view is framed to the glass.** In a pre-draw listener, before each frame:
  `decorView.setLeftTopRightBottom(glass)` and the root view's children moved back by the same amount, so what
  they draw stays where it was on screen (`OverlayActivity.frameGlass`). The window itself never moves. A
  layout pass gives the root view the whole window again, so the frame is set anew every time. It is done before
  the draw, not from Compose's layout during it: the background takes its bounds when the root view is drawn.
  The shadow's outline and the cleared shape are then the root view's own rectangle.
- **Measured at real speed (120 Hz), Fast, Medium and Slow, opening and closing, five of each:** the glass's
  middle is within 0.5 px of the panel's in every frame; no frame stalls; what is behind the glass is blurred
  across its whole width, edge to edge, and nothing beside it is (four times slower: from a glass 27 px wide;
  at real speed the first frames are under the system's fade, below). Light and dark, with the shadow, with
  rows arriving while it opens, and through a turn. Solid glass and the opening turned off are as before.
- **Resizing the window itself in width was tried once more and thrown away.** At real speed the glass's middle
  was 16 to 57 px off for the frames in which it grows fastest (the window's new position is applied before its
  new buffer). Four times slower it looked right. Alex saw it at once: "one side first".
- **The system fades the panel's task in, and the blur with it.** The desktop's shell animates a see-through
  task itself (`SystemModalsTransitionHandler` in its transition log; the same handler animates the closing).
  For about 200 ms after the first frame the whole window is under that fade: the seam has always come up over
  its first 130 to 200 ms. The compositor multiplies a blur region by its window's alpha, so the blur comes up
  with the glass, not later. `onEnterAnimationComplete` arrives 195 to 210 ms after the first frame.
  `overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)` changes nothing (measured both ways). At Medium
  and Slow the glass starts to open when that fade is over or all but over (220 and 440 ms); at Fast it opens
  under it (from 110 ms).
- **The blur is asked for in `onCreate`.** The first `setBackgroundBlurRadius` above 0 only registers a
  listener; the platform makes the blur layer in a message of its own, and during an opening those wait behind
  the frames. Asked for before the first frame, the layer is there with it. And the radius is never set to 0 on
  the way in: at 0 the platform lets the layer go and has to be asked again. (From the source. On the device
  the system's fade covers the same first frames, so no difference could be measured.)
- How it was looked at: recordings over the test backdrop, the glass's edges and the sharpness of the text
  behind it measured in every frame. The shell's transition log (`wm shell protolog enable-text
  WM_SHELL_TRANSITIONS`) was on for one opening and turned off again.
- Not tried: the HP. A system that does not put its blur in the root view would show a blurred rectangle
  around the growing glass; the code cannot detect that, so look at the first opening on any new device.

## The Booklight window as a desktop window (Lenovo Googlebook 15, October 2026)

`docs/design/window-redesign.md` §9 named nine things a device had to show before the window could be called
right. What the Lenovo showed, with debug builds of the redesign. Pictures are of the window's own content
(`./bl wshot`, PixelCopy); keys and clicks were injected with `adb shell input` into Booklight's own focused
window; sizes were set with `am task resize` on Booklight's own task. The display is 1920 × 1200 dp at 1.5 px
per dp; the taskbar takes the lowest 56 dp.

1. **The caption bar.** With `APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND` the system draws its chip (the
   app's icon and a chevron) and its three controls straight on the window's ground (`surfaceContainer`), 40 dp
   high, and both are readable in the light and in the dark theme (`results-engine-*-open.png` in
   `docs/design/captures/window-redesign/` are cut from the screen and show it). The marks are light or dark
   as `APPEARANCE_LIGHT_CAPTION_BARS` says, so they follow Booklight's theme and not the system's: with the
   system light and Booklight dark the marks are white. The other way round (system dark, Booklight light)
   was not tried.
2. **The minimum size.** `<layout android:minWidth="400dp" android:minHeight="480dp"/>` reaches the task
   (`minWidth=600 minHeight=720` px in `dumpsys activity activities`). `am task resize` to 300 × 400 dp is
   not refused: the task's bounds become 450 × 600 px, **but the window is still laid out at 600 × 720 px**
   (400 × 480 dp: the bar, a title, a row with its control at the trailing end all fit). What the screen
   shows of a window that is larger than its task was not looked at. Not tried: a drag with the pointer below
   the minimum, and so the desktop's own smallest width.
3. **The default size, and the last bounds.** A new window opens at 1053 × 888 dp. Closed and opened again
   it has that size again: the system does not bring the last bounds back. A task whose process was killed
   keeps its bounds when it comes back. Not tried: the HP's default size.
4. **Title and rail on one line.** The caption bar's inset is 40 dp; the centre of the rail's first mark is
   80 dp below the window's top at both rail widths (120 px in the pictures), and the title's capitals are
   centred on 119 to 120 px. Measured in the pictures, at 1053 and at 720 dp of window.
5. **Keys in the rail.** A `WideNavigationRailItem` and a `ShortNavigationBarItem` are focus stops of their
   own if left alone; arrows and Tab then move Compose's focus among them while the page has its own order.
   With `focusProperties { canFocus = false }` on every Material part and the window's root holding the keys,
   every key of the plan's §6 does what the table says (injected, on the last build): F6; Up and Down in the
   rail (the section changes at once); Right or Enter into the page; Left on a row without a choice, on a
   switch and on a choice at its first option back to the rail; Left and Right on a choice; Enter and Space (a switch, the engine's menu, a
   command's example typed into the panel); Home, End, Page Up, Page Down; Tab and Shift + Tab through the
   stops and round through the rail; Ctrl + 1 … 5; Ctrl + F from any section, at the compact width too;
   letters on Commands; Ctrl + N; Delete once on one of the user's own (it asks; the second press was done on
   an earlier build with a link made for the test); the Menu key; Escape (a menu, an editor, Find; never the
   window); Ctrl + W; Shift + F10 for a row's menu. In the bar: Left and Right change the section, Up goes
   into the page; Down on the page's last stop goes to the bar. In an editor: Tab round its fields and buttons
   and never out of it, Enter saves, Escape asks once when something was changed.
6. **Material's own focus mark.** A list item that is told it has the focus (through its interaction source)
   takes its focused shape and would draw a focus mark of its own; that mark is switched off for the rows
   (`LocalRippleThemeConfiguration`), so the window's ring is the only one, and it is put back inside menus,
   where Material moves the focus itself. Not tried: the system's "remove animations" (a system setting of
   the device; Booklight's own motion asks `Motion.on`).
7. **`surfaceBright` on `surfaceContainer`.** Rows against the ground, read from the pictures: light with the
   wallpaper's colours `#fef7fe` on `#f3ebf5`; light with Booklight's own `#f9f9ff` on `#ededf4`; dark with
   the wallpaper's `#2e2a33` on `#1b181f`; dark with Booklight's own `#2f313a` on `#1d1f27`. The step is 11
   or 12 levels in the light themes and 18 or 19 in the dark ones: visible and quiet in all four.
8. **Across 600, 840 and 1332.** The task was resized in steps of 4 dp (each step holds about three frames)
   while the window logged its layout every frame (`./bl debug window trace`), and across each breakpoint in
   one step while it took a picture of itself every second frame (`./bl debug window film`; no recording of
   the screen). 1332, both ways: the column's start and the title's line do not move by a pixel; the second
   pane fades and slides 24 dp at the trailing edge; the demo or the preview opens from its top in the column
   (closes towards it) while the rows under it move on the same spring; the panel in it is the same one, as it
   was. 840, narrowing: Material moves each name from beside its mark to under it, the indicator goes with it,
   the column follows the rail's width frame by frame. 600, narrowing: the rail fades while its width closes
   and the bar opens from the bottom; for a few frames both are there, each partly (a cross-fade), never both
   in full. The title's line keeps its height in all three. Not filmed on the last build: 840 and 600
   widening. Not tried: a drag with the pointer (adb cannot drag a window's edge without touching the
   system's part of it).
9. **Unseen 48 dp targets.** With `LocalMinimumInteractiveComponentSize` unspecified in the window, a click
   3 dp above a button of a choice did not choose it, and a click 5 dp inside it did. (On the first build the
   row took the click and stepped the choice; a choice's row is no longer pressed, and the same click now
   changes nothing.) With Material's default the same click was not tried.

What else it showed:

- **The Keyboard Shortcuts Helper.** `requestShowKeyboardShortcuts()` from the window opens the system's
  helper as a dialog over the desktop (a `SystemUIDialog` window has the keys). The system asks the window
  for its keys once (`onProvideKeyboardShortcuts`), and the helper lists "Booklight", with the app's icon,
  under System, Multitasking, App shortcuts, Input and Accessibility: the five sections with Ctrl + 1 … 5,
  Find a command, New link, Navigation or page (F6), Close the window. `dismissKeyboardShortcutsHelper()`
  closes it and the keys are the window's again.
- **A new window is laid out twice.** A Booklight window that opens at the default 1053 dp is first composed
  with a pane 1162 dp wide (a window of some 1380) and about 30 ms later with its own 833. For that one frame
  the window had a second pane: the editor that the panel's "Edit…" opens was drawn there, took the keys, and
  lost them when that copy went. Nothing moves between column and pane in a window's first half second now.
- **The minimum size in German** (400 × 480): the bar's five names fit ("Datenschutz" ends 3 dp from the
  window's edge); "Eingebaut · Eigenes" and the full "Neuer Link" button leave about 5 dp between them, so New is
  only its plus there; Start's title, lead and demo filled the pane, so the demo gives way and the key's row
  shows.
- **A row that stayed three lines high.** Material's list item takes a row for a three-line one (88 dp) when the
  first and the last baseline of what stands under its name differ. While the rail closes from 220 to 96 dp the
  column is narrow for a few frames and a command's example stands under its text, in one `Column` with it;
  when the example went back to the row's end, that column kept its old last baseline and every command's row
  stayed 88 dp high at 720 dp of window (logged: one line of text, `first=22 last=22`, row 132 px). With the
  text alone in that place when nothing stands under it (no column), the rows are 72 dp again.
- **A build installed under an open Booklight window** leaves its task in an odd state: the desktop puts its
  own "package update" activity into the task, and an `am start` that arrives meanwhile leaves the window
  full screen. Close the window first.
- **A menu is a window of its own** (`mCurrentFocus` is "Pop-Up Window" while Booklight stays the focused
  app). It is not in a picture of the activity's window; a picture with a menu in it is a screen capture cut
  to the window's bounds.
- **What adb can and cannot inject:** keys, key combinations (`input keycombination CTRL_LEFT KEYCODE_4`: a
  bare `4` is key code 4, Back), text, a primary click (`input mouse tap`), the wheel (`input mouse scroll`),
  a drag inside the window (the scrollbar's thumb follows it). No hover and no secondary click: a row under
  the pointer was looked at through a debug hook that tells the row it is hovered, a row's menu through the
  Menu key. Not tried: hover and right-click with the real pointer.
- **material3 1.5.0-alpha29:** a `WideNavigationRailItem` keeps the label style it was first composed with
  (a rail that starts expanded keeps the large style when it collapses); the expanded rail's item draws its
  hover and press round its mark and name, not across the rail; a text field with no width cannot take the
  focus.
- **Two Googlebooks on adb:** the one that attaches later can be listed first. A script that takes "the first
  device that is not an emulator" then talks to the other one. Choose by model.
- Not tried at all: the HP.

