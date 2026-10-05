# Getting Booklight a key: what is possible, and the shortest way

*4 October 2026. Asked by Alex: "investigate better ways to enable the shortcut for Booklight. Right now users have
a hard time setting it themselves. One idea I had was a custom keyboard mapping as long as this doesn't break
anything and doesn't have crazy permissions. Another is maybe a better deep link."*

Marks: **[S]** read in Android's source (`android17-release`, the same on `android16-qpr2-release` unless said);
**[D]** seen on the Lenovo Googlebook on 4 October 2026 (Android 17, the 3.0 release build); **[I]** inferred;
**[P]** press or other secondary source. Nothing on the device was changed.

## 1. The short answer

- **There is no way round the system's Keyboard shortcuts helper.** Android 17 has no public call that lets an app
  register a global shortcut or ask the user for one [S, and checked in the API 37 `android.jar`]. The calls
  underneath (`InputManager.addCustomInputGesture`) need `MANAGE_KEY_GESTURES`, a signature permission that no role
  grants [S].
- **There is no better deep link.** The helper is a dialog of the system UI, not an activity [S, D]. It opens on a
  bare broadcast (`Activity.requestShowKeyboardShortcuts()`) or on Action + /, and takes no arguments: customize
  mode, the category, the search text and the "add" dialog cannot be preset [S].
- **A keyboard layout shipped with the app does not help** (section 4).
- **What can change is the path and the guidance.** The same helper can be done in three clicks, and Booklight can
  put its instructions inside the helper itself (section 2). Today's card sends people the long way.
- **Stop suggesting Action + K** (section 3).

## 2. The helper, as it really behaves

**Which page it opens on** [S, D]. The helper shows the focused app's own shortcut groups
(`onProvideKeyboardShortcuts`) as the selected category, under the app's name and icon. With no groups it opens on
*System*.
- From the panel today: the panel asks for the helper and closes, so the helper opens on *System* (or on whatever
  window gets the focus next) [D].
- From the Booklight window, which stays in front: it opens on a page called **Booklight** that lists the window's
  own keys (Start Ctrl 1, Commands Ctrl 2 …) [D]. **Those rows are Booklight's own text.** Each row is a label and
  at least one key [S]. So that page can carry the steps, in the system's own dialog.
- That page stays as it is when *Customize* is clicked [D].

**The search field** [D]. It has the focus as soon as the helper opens, and still has it after *Customize*.
- Before *Customize*, a search finds only shortcuts that exist: an app without one gives **"No search results"**,
  and no way forward is offered. A new user who types "Booklight" straight away lands there.
- After *Customize*, the same text finds the app with a **+** beside it, and the helper jumps to *App shortcuts*
  showing only that row. Text typed before the click is kept.
- The search matches row labels [S]. If a row on Booklight's own page has the word "Booklight" in it, the search
  finds that row and does not jump: keep the app's name out of those labels.

**The shortest path** [D], counted from the moment the helper is open:

| # | The user | Sees |
| --- | --- | --- |
| 1 | clicks **Customize** (top right) | "Customize shortcuts"; the caret is in the search field |
| 2 | types **Booklight** | one row, Booklight, with **+** |
| 3 | clicks **+** | "Booklight. To create this shortcut, press the Action key and one or more other keys together"; a fixed *Action +*, a field *Press key*, **Cancel**, **Set shortcut** (greyed until keys are pressed) |
| 4 | presses the keys with Action held | the keys in the field |
| 5 | clicks **Set shortcut** | the row with its keys |
| 6 | clicks **Done**, or closes | |

Three clicks, one word, one key press. The way the card describes today ("choose App shortcuts, then Add
shortcut") is the same number of clicks to reach the list, but then leaves the user in a list of every launchable
app with no hint to search.

**Step 6 is not needed: the new key closes the helper** [D]. Right after *Set shortcut*, with the helper still in
*Customize*, pressing the new keys makes the system close the helper by itself and start the app's launcher
activity; the app's window comes to the front with the focus. The app is told: a launcher start arrives
(`onNewIntent`: `ACTION_MAIN`, `CATEGORY_LAUNCHER`, no extras, the referrer `android-app://android`). So the
path ends "… Set shortcut, then press your keys", and the first press of the user's own key is the moment the
system's dialog falls away.

**A stand-in for the panel, tried on the device** [D] (a throwaway app: a see-through window of the panel's size
at the panel's place that does not close when it loses the focus, and gives the helper one group of rows):
- The helper opens on that app's page. The page's heading is the group's label, free text ("First steps").
- Rows wrap and are not cut: an English label of 73 characters stood on three lines of about 25, a German one on
  four. A row's keys stand at its right end: a digit as a plain cap, Action + J as the device's Action glyph and
  a "J".
- A row with the app's name in it stops the search from jumping to the app: the search then stays on the app's
  own page and shows that row.
- The panel's place lies wholly behind the helper: nothing of the panel shows while the helper is up.
- Closing the helper by its X or by Esc gives the focus straight back to the window that stayed open.
- An Action + letter combination that is not a shortcut yet did not reach the app as a key: the panel cannot let
  someone try keys first, and cannot show which keys were pressed.

**The helper's own words have names** [D]. The system UI's strings are there under the names AOSP gives them:
`shortcut_helper_title` ("Keyboard shortcuts"), `shortcut_helper_customize_button_text` ("Customize"),
`shortcut_helper_customize_dialog_set_shortcut_button_label` ("Set shortcut"),
`shortcut_helper_customizer_action_key_text` ("Action"), `shortcut_helper_search_placeholder` ("Search
shortcuts"), `shortcut_helper_done_button_text` ("Done"). If Booklight may read them
(`createPackageContext("com.android.systemui", 0)`, with the package in `<queries>`) [I], its instructions can
quote the buttons exactly as the user sees them, in the user's language, with Booklight's own words as the
fallback.

**What the system stores** [D]: a custom gesture, the keys → "launch this app's launcher activity". One per app
row, ten per user, Action always in it (rules of the helper's own UI, not of the service [S]). It survives a
reinstall.

**What Booklight can know.** Not whether a shortcut exists (reading the list needs the same signature
permission). Only that its key was pressed: the panel was started as the launcher activity by something that is
not the icon (`OverlayActivity.byKey`, which sets `keySeen`). That is enough: the step is done when the key opens
the panel.

**A note beside the helper?** The helper dims everything behind it. A pinned Booklight window stays on screen
beside it, under that dim: its brightness fell from 233 to 93 of 255 in a capture, dark text on a light note
became hard to read [D]. Light text on a dark note would survive; it is still second-best to rows inside the
helper.

## 3. Which keys to suggest

- **Action + K: no longer.** Android 17 defines Action + K as a system shortcut (`CONTEXTUAL_INPUT`) behind the
  flag `enable_contextual_input_trigger`, and system shortcuts are matched before the user's own [S]. It is not a
  system shortcut on either Googlebook today [D for the Lenovo; the HP by the earlier notes], but a build that turns
  the flag on would take the key away from Booklight without a word. The README, the window's Start page and the
  docs suggest K.
- **Letters Android 17's source leaves alone with Action**: D, J, O, R, T, X, Y, Z (D, T, X, Y, Z have meanings
  with a second modifier). M is "maps" in AOSP's own table but free on the Googlebooks.
- **Action + Alt + Space** is accepted by the dialog (Action + Space is the assistant's).
- **Action + Quick Insert: Alex's choice (4 October), and it works** [D]. Quick Insert is the key in the Caps Lock
  place on a Googlebook's keyboard (Fn + that key is Caps Lock); its key code is `KEYCODE_CONTEXTUAL_INSERT`. On
  the Lenovo Googlebook, with the stand-in app: the helper draws the key as a cap that reads "Quick Insert", after
  the Action glyph; the capture dialog shows "Quick Insert" in its field and lets it be set; the system stores
  `KeyTrigger{KEYCODE_CONTEXTUAL_INSERT, META}`; pressing it starts the app, closes the helper when it is open,
  and a second press arrives too. No system shortcut uses the key with Action there, and Android 17's source
  defines none [S] and does not bar the key from custom shortcuts (`InputGestureManager.isKeyAllowedForCustomGesture`)
  [S]. A keyboard without the key (most external ones) cannot use it: the suggestion needs a fallback there.
- A recommendation for the first run: one suggestion, shown as key caps, with "or any keys you like". Which one is
  a product decision (`docs/design/first-run/`).

## 4. A keyboard layout shipped with the app (Alex's idea)

An app may ship keyboard layouts with no permission (a receiver for
`android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS` and `.kcm` files). What such a file can do [S,
`KeyCharacterMap.cpp`]:

- **Rename keys, nothing more.** `map key <scancode> <KEY>` turns one physical key into one key code (also into a
  modifier: Caps Lock as Action). `key X { alt: replace F13 }` turns a chord into one key code with those
  modifiers taken away. It cannot start an app, cannot produce a chord (one key → Action + K), cannot add Action.
- **So a second choice is always needed** to get from the renamed key to Booklight: the same helper flow (and the
  helper only records with Action held), or the assistant role (`ASSIST`), or a chooser for a category
  (`CALCULATOR`, `MUSIC` …) or for voice search (`VOICE_ASSIST`). `MACRO_1…4` are dropped before apps see them;
  `SEARCH` goes to the focused app and then to the system's own search.
- **And the layout itself must be chosen**: Settings › Physical keyboard › the keyboard › the language › the
  layout, once per typing language. An app cannot choose it (`SET_KEYBOARD_LAYOUT` is a signature permission).
- **It replaces the user's layout.** One layout is active per keyboard and language. With Booklight's chosen, a
  German keyboard types QWERTY unless Booklight ships a German copy; AOSP has 58 layouts.
- A layout that names a locale would compete with the system's own in the automatic choice [S for the rule, I for
  the outcome]: an install that silently changes how someone types. Not to be shipped.
- Prior art [P]: apps that carry a layout for Caps Lock → Ctrl and the like; their users report lost AltGr symbols
  after removal and a picker that is not there until a keyboard is detected. No app was found that uses a layout
  as its own hotkey.

**Verdict:** it adds a second, harder setup on top of the first and risks people's typing. Not a route.

## 5. Every other route

| Route | The user does | Costs | Verdict |
| --- | --- | --- | --- |
| **The helper, guided** (section 2) | Customize, type the name, +, the keys, Set shortcut | Nothing; no permission | **The one to build** |
| **The assistant role** | One button opens the role's picker itself (`Settings.ACTION_VOICE_INPUT_SETTINGS` [S]; Booklight answers the assist request [D], which is what qualifies an app [S]; the picker itself was not opened today), choose Booklight, confirm | Booklight takes Action + Space and the keyboard's assistant key **from Gemini**, with the three-finger "Launch Gemini" and the hotword, and other system features that expect Gemini there | Stays what it is: an opt-in row in the window, not the first run's way |
| **Touchpad: three-finger tap › Open another app** | Settings › Touchpad › Use three-finger tap › Open another app › Booklight [S] | Replaces middle click; a gesture, not a key; no deep link | Worth a line under "other ways to open" |
| **The Quick Settings tile** | One system dialog (`StatusBarManager.requestAddTileService`; the tile exists) | Two actions per use | A fallback, cheap to add |
| **An app category's key** (Action + U calculator, E email …) | Nothing, where no other app answers the category; else a chooser, once | Booklight would claim to be something it is not; a chooser on a system key for everyone else; a store-policy risk | **No.** Named because it is the only route with no setup at all (on the test device nothing answers the email or maps categories [D]) |
| **The notes role** (Action + Ctrl + N, a hot corner) | Default apps › Notes app | Displaces the notes app; opens as a bubble | No |
| **An accessibility service that reads keys** | Switch it on, through restricted settings | Reads every key; against Booklight's promise and the store's rules for launchers | No (ruled out since 1.0) |
| Action + digit for pinned apps, an app on a hot corner, global search, the Action key alone | | Not in Android 17, or the system's own [S] | Not available |

## 6. What follows for Booklight

1. Open the helper from a Booklight surface that **stays in front**, so that it opens on Booklight's page, and make
   that page the instructions (rows without the app's name in them).
2. Teach the short path: **Customize, type Booklight, +, your keys, Set shortcut, press your keys.**
3. Count the step as done when the key opens the panel, which it can do from inside the helper; have a state for
   "back, no key yet" and a line for "it did not work".
4. Stop suggesting Action + K, everywhere it is written.
5. Later, and outside the app: a real answer needs the platform (an argument for the helper, or a way for an app
   to ask). The source has one hint of an API for others to build customization, a flag that nothing uses yet
   (`enable_poem_input_customization`) [S].

## 7. Not verified

- The real panel under the helper (the stand-in was a plain see-through window, not the panel with its blur and
  its closing on a lost focus), and the same on the HP Googlebook.
- Whether Booklight can read the helper's own words ("Customize", "Set shortcut") from the system, to quote them
  in the user's language.
- Whether the launcher intercepts Action + Space for an assistant that is not Google's.
- Whether three-finger tap opens the panel (the launcher activity) rather than the window.
- The helper in German, and at window sizes other than the Lenovo's.

## Sources

Android source, branch `android17-release` under `https://android.googlesource.com/platform/`:
`frameworks/base/packages/SystemUI/src/com/android/systemui/keyboard/shortcut/` (`ShortcutHelperCoreStartable.kt`,
`data/repository/ShortcutHelperStateRepository.kt`, `ui/viewmodel/ShortcutHelperViewModel.kt`,
`ui/viewmodel/ShortcutCustomizationViewModel.kt`, `data/repository/CustomShortcutCategoriesRepository.kt`);
`frameworks/base/services/core/java/com/android/server/input/` (`InputManagerService.java`,
`InputGestureManager.java`, `KeyGestureController.java`, `KeyboardLayoutManager.java`,
`AppLaunchShortcutManager.java`); `frameworks/base/core/java/android/app/Activity.java`
(`requestShowKeyboardShortcuts`); `frameworks/base/core/res/res/xml/bookmarks.xml`;
`frameworks/base/core/res/AndroidManifest.xml` (`MANAGE_KEY_GESTURES`, `SET_KEYBOARD_LAYOUT`);
`frameworks/base/core/java/android/hardware/input/input_framework.aconfig`;
`frameworks/native/libs/input/KeyCharacterMap.cpp`, `InputEventLabels.cpp`;
`packages/apps/Settings` (`inputmethod/TouchpadThreeFingerTapUtils.java`, `PhysicalKeyboardFragment.java`);
`packages/modules/Permission` (`PermissionController/res/xml/roles.xml`).
Earlier notes: `android-platform.md` §A, `device-findings.md` ("The keyboard shortcut").
