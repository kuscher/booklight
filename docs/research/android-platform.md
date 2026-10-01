# Android launcher/search overlay: what the platform allows (Android 16 to 17)

*Desk research for Booklight, 1 October 2026: AOSP release branches and developer.android.com, nothing device-tested. Where the HP Googlebook 14 behaves differently, [device-findings.md](device-findings.md) wins: most importantly, a see-through activity floats over the desktop there **without** `SYSTEM_ALERT_WINDOW` (section B below says it needs it on Android 17).*

**Method.** AOSP `main` has been frozen since March 2025, so I read the release branches instead (`android16-release`, `android16-qpr1-release`, `android16-qpr2-release`, `android17-release`), plus developer.android.com and Play Console Help. Nothing was tested on a device. Feature-flag states in shipped builds and OEM overlays are unknown, so flag-gated items are marked.

**Three findings that shape v1:**
- **Zero-permission hotkey exists.** From Android 16 QPR2 the user can bind a Meta-based combo to any launcher activity in the system Shortcut Helper, and your app can open that helper with a public API.
- **Assistant role gives a default hotkey.** The default assistant is launched by Meta+A on Android 16 and QPR1, and by Meta+Space on QPR2 and 17. An activity handling `ACTION_ASSIST` is enough to qualify.
- **A true overlay is gated on `SYSTEM_ALERT_WINDOW`.** In desktop windowing, a translucent activity only floats fullscreen over the desktop if the app has that permission; otherwise it follows the normal freeform path (appearance unverified). The assistant role grants that app-op.

## A. Summoning by hotkey

### A1. Default digital assistant
- **Qualification:** `ROLE_ASSISTANT` accepts either a `VoiceInteractionService` (needs `sessionService`, `recognitionService`, `supportsAssist=true`) or just an activity with an `android.intent.action.ASSIST` filter.
- **Selection:** the role is `requestable="false"`, so `RoleManager.createRequestRoleIntent` cannot be used. The user picks it in Settings > Apps > Default apps > Digital assistant app. Deep links: `Settings.ACTION_VOICE_INPUT_SETTINGS` or `ACTION_MANAGE_DEFAULT_APPS_SETTINGS`.
- **OEM risk:** picker visibility depends on `config_showDefaultAssistant`; unverified on your target device.
- **Keys:**

| Release | Assistant shortcut |
|---|---|
| Android 16, 16 QPR1 | Meta+A |
| 16 QPR2, 17 | Meta+Space, and Meta+Shift+F23 (the Copilot key sequence) |
| 17, flag-gated | Meta+A is reassigned to contextual search (Circle to Search) |

- `KEYCODE_ASSIST` and `KEYCODE_VOICE_ASSIST` also launch assist. Press reports say Gemini on Android desktop uses "Google key + Space", which matches.
- **Launch path:** `PhoneWindowManager.launchAssistAction` goes to SystemUI `AssistManager`. For an activity-only assistant, the system starts `ACTION_ASSIST` on your component with `FLAG_ACTIVITY_NEW_TASK` and the extra `EXTRA_ASSIST_INPUT_HINT_KEYBOARD`. No background-launch problem, because the system starts it.
- **Caveat:** `AssistManager.shouldOverrideAssist` lets the system launcher intercept some invocation types. Whether the keyboard invocation is overridden on Pixel or Googlebook builds is unverified.
- **What the role grants** (roles.xml): `READ_ASSISTANT_APP_SEARCH_DATA`, the `SYSTEM_ALERT_WINDOW` app-op, and SMS/call-log permissions if requested. I believe the app-op is only applied if you declare the permission in the manifest; that condition is not verified.
- **What a real VoiceInteractionService adds:** shortcut host access (see C2), background-activity-start exemption, and app-switch allowance.
- **Play:** I found no policy restricting who may be an assistant. Assistant handlers are named only as eligible for SMS/Call Log permissions.

### A2. User-customisable shortcuts (Android 16+)
- **Version history:** Android 16 let users rebind system and app-category shortcuts. 16 QPR2 Beta 1 added "+ Add shortcut" for any app. The app list comes from `LauncherApps.getActivityList`, so the target is a launcher activity (package + class), not an app shortcut or arbitrary activity.
- **Limits:** 10 custom app shortcuts and screens over 600dp only (both press-reported). The UI requires the Action (Meta) key. Reserved combos are rejected.
- **No API to request a binding.** `InputManager.addCustomInputGesture`, `InputGestureData`, `AppLaunchData` and `KeyGestureEvent` are all `@hide`, and `MANAGE_KEY_GESTURES` is `signature` (or `signature|recents`).
- **Deep link:** `Activity.requestShowKeyboardShortcuts()` is public and opens the Shortcut Helper, where the Customize button lives. `Settings.ACTION_HARD_KEYBOARD_SETTINGS` opens physical keyboard settings. Neither can preselect your app.

### A3. App-category shortcuts
- **Android 16+ defaults** (`bookmarks.xml`, all with Meta): B browser (role), P contacts, E email, C calendar, M maps, U calculator, and F files from QPR2. Android 15 used C/K/P/S for contacts/calendar/music/SMS. OEMs overlay this file.
- **Hijack route:** category shortcuts resolve via `Intent.makeMainSelectorActivity(ACTION_MAIN, CATEGORY_APP_*)`. A third-party activity declaring, say, `CATEGORY_APP_CALCULATOR` becomes a candidate, and the user would have to choose it in the resolver. This is unverified on device, fragile against preinstalled defaults, and arguably deceptive unless you really are a calculator. Not recommended.

### A4. Accessibility key filtering
- **Capability:** `onKeyEvent` with `FLAG_REQUEST_FILTER_KEY_EVENTS` receives hardware keys "before they are passed to the device policy, the input method, or applications". Returning `true` consumes the event.
- **Meta combos:** in `KeyboardInterceptor`, only volume keys are held back for the policy. Meta combos should therefore be visible and consumable; this is inferred from source, not device-tested. Keys handled before queueing (power, `KEYCODE_ASSIST`) are not interceptable.
- **Bonus:** accessibility services are bound with `BIND_ALLOW_BACKGROUND_ACTIVITY_STARTS`, so they can launch the overlay from the background.
- **Play:** launchers and assistants are explicitly "not accessibility tools". You must not set `isAccessibilityTool`, must file the declaration form, and must show in-app prominent disclosure with consent.
- **Android 17:** Advanced Protection Mode revokes and blocks accessibility services that are not accessibility tools (press-reported).

### A5. Everything else
- **Meta tap (the launcher key):** raises `KEY_GESTURE_TYPE_ALL_APPS`, handled by a registered accessibility system action or the recents app. Registration needs `MANAGE_ACCESSIBILITY` or `MANAGE_KEY_GESTURES`, so it is system-only.
- **`KEYCODE_SEARCH`:** default config delivers it to the focused app, then falls back to the global search activity. OEMs can redirect it to a fixed component.
- **Global search activity:** chosen from `Settings.Secure.SEARCH_GLOBAL_SEARCH_ACTIVITY`, else system apps rank first. A third-party app effectively cannot win this.
- **`ACTION_SEARCH_LONG_PRESS`:** only fires when no hardware keyboard is present, so it is useless on a laptop.
- **Quick Settings tile:** works with `TileService.startActivityAndCollapse(PendingIntent)`; the `Intent` overload throws for targetSdk 34+.
- **Taskbar pin, widget, app shortcut:** normal user-initiated launches. I found no "Meta+number launches taskbar item" gesture in the Android 17 key gesture list.
- **`ROLE_HOME`:** gives Meta+H / Meta+Enter (Home) but replaces the desktop launcher. Too invasive.

### A6. Keyboard capture
Confirmed focused-only. `WindowManager.LayoutParams.setKeyboardCaptureEnabled(true)` with `CAPTURE_KEYBOARD` (normal permission, API 36.1) lets the focused window receive system shortcuts first. Home, Overview and Power are still reserved, and long-press Esc exits. It helps once your overlay is open; it cannot summon it.

**Sources (A):**
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/input/InputGestureManager.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/services/core/java/com/android/server/input/InputGestureManager.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/android/hardware/input/InputManager.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/res/AndroidManifest.xml
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/res/res/xml/bookmarks.xml
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/res/res/values/config.xml
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/policy/PhoneWindowManager.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/input/KeyGestureController.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/packages/SystemUI/src/com/android/systemui/assist/AssistManager.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/packages/SystemUI/src/com/android/systemui/keyboard/shortcut/
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/accessibility/java/com/android/server/accessibility/KeyboardInterceptor.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/com/android/internal/policy/PhoneFallbackEventHandler.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/search/Searchables.java
- https://android.googlesource.com/platform/packages/modules/Permission/+/refs/heads/android17-release/PermissionController/res/xml/roles.xml
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- https://developer.android.com/reference/android/Manifest.permission
- https://support.google.com/googleplay/android-developer/answer/10964491
- https://support.google.com/googleplay/android-developer/answer/10208820
- https://www.androidauthority.com/android-16-qpr2-keyboard-mouse-features-3590626/
- https://9to5google.com/2026/02/09/gemini-icon-android-desktop/
- https://securityaffairs.com/189497/security/advanced-protection-mode-in-android-17-prevents-apps-from-misusing-accessibility-services.html

## B. Overlay presentation without `SYSTEM_ALERT_WINDOW`

### Translucent activities under desktop windowing
`DesktopModeLaunchParamsModifier` asks `DesktopModeCompatPolicy.isTopActivityExemptFromDesktopWindowing`. A task whose activities are all non-occluding (for example `windowIsTranslucent`) is forced to `WINDOWING_MODE_FULLSCREEN` and "just shown on top of the desktop" only under these conditions:

| Release | Condition for the fullscreen-over-desktop exemption |
|---|---|
| Android 16 | Manifest merely requests `SYSTEM_ALERT_WINDOW` (flag-gated; with the flag off, every transparent task is exempt) |
| 16 QPR2 | Requests `SYSTEM_ALERT_WINDOW`, or platform-signed |
| 17 | `SYSTEM_ALERT_WINDOW` actually granted (`Settings.canDrawOverlays` or permission granted), or platform-signed, or privileged |

- This is the path Gemini and Circle to Search use (the source comment names them).
- A TODO in the Android 17 source says this check will be replaced with a "permission and manifest check", so expect change.
- **Without the exemption:** the activity goes through the normal desktop launch path as a freeform task. The docs say "all apps running in desktop windowing have a header bar". The exact look of a translucent freeform window (caption, shadow, see-through) is unverified and needs a device test.
- `shouldDisableDesktopEntryPoints` is true for transparent tasks; its visible effect is also unverified.

### Sizing and task behaviour
- **Launch bounds:** `ActivityOptions.setLaunchBounds` is honoured, but only when your own code starts the activity.
- **Manifest layout:** `<layout android:defaultWidth/defaultHeight/gravity>` is honoured by `DesktopModeBoundsCalculator`. This is the only sizing lever when the system launches you from a hotkey.
- **Resizability:** from targetSdk 36, `resizeableActivity=false` and orientation/aspect restrictions are ignored on sw≥600dp. The opt-out disappears at targetSdk 37.
- **Task attributes:** `excludeFromRecents`, `noHistory`, a separate `taskAffinity` and `singleInstance` are ordinary and unrestricted. I did not verify whether excluded tasks still appear in the desktop taskbar.

### Blur
- **APIs (API 31+):** `Window.setBackgroundBlurRadius` or `R.attr.windowBackgroundBlurRadius` (needs `windowIsTranslucent`); `FLAG_BLUR_BEHIND` with `setBlurBehindRadius`.
- **Availability:** needs OEM `ro.surface_flinger.supports_background_blur=1`. Disabled in battery saver, by the "Allow window-level blurs" developer option, and on weak GPUs.
- **Check at runtime:** `WindowManager.isCrossWindowBlurEnabled()` and `addCrossWindowBlurEnabledListener`. Ship an opaque fallback.

### Background activity launch
- **Allowed when:** the app has a visible window; a system-sent `PendingIntent` (notification, tile, widget); `SYSTEM_ALERT_WINDOW` granted; or bound by the system with `BIND_ALLOW_BACKGROUND_ACTIVITY_STARTS` (accessibility service and VoiceInteractionService are; notification listener is not).
- **Not allowed:** a plain foreground service.
- **Android 17:** extends the restrictions to `IntentSender` and deprecates `MODE_BACKGROUND_ACTIVITY_START_ALLOWED` in favour of `..._ALLOW_IF_VISIBLE`.

**Sources (B):**
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/com/android/internal/policy/DesktopModeCompatPolicy.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-release/libs/WindowManager/Shell/shared/src/com/android/wm/shell/shared/desktopmode/DesktopModeCompatPolicy.kt
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/wm/DesktopModeLaunchParamsModifier.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/wm/DesktopModeBoundsCalculator.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/libs/WindowManager/Shell/src/com/android/wm/shell/desktopmode/DesktopTasksController.kt
- https://developer.android.com/develop/adaptive-apps/guides/support-desktop-windowing
- https://source.android.com/docs/core/display/window-blurs
- https://developer.android.com/guide/components/activities/background-starts
- https://developer.android.com/about/versions/17/behavior-changes-17

## C. Data sources

1. **Apps.**
   - `LauncherApps.getActivityList(null, user)` for each `getProfiles()` entry, with `LauncherApps.Callback` for package changes.
   - Private space needs `ACCESS_HIDDEN_PROFILES` (normal) plus `ROLE_HOME`, so it is invisible to you.
   - Package visibility: declare `<queries><intent>` with `MAIN` + `LAUNCHER` rather than `QUERY_ALL_PACKAGES`. The docs do not explicitly bless this pattern, but it follows the documented syntax.
   - Play allows `QUERY_ALL_PACKAGES` for "device search" with a declaration form, and rejects it when "a less broad app-visibility method" works.
   - Icons: `LauncherActivityInfo.getIcon` / `getBadgedIcon`; themed icons via `AdaptiveIconDrawable.getMonochrome()` (API 33; from memory).
2. **App shortcuts.** `LauncherApps.getShortcuts` throws unless `hasShortcutHostPermission()` is true. That is true only for the current/default launcher, "the currently active voice interaction service", or holders of `ACCESS_SHORTCUTS` (`signature|role|recents`). An activity-only assistant does not qualify; `VoiceInteractionManagerService` sets the shortcut host only for the service package.
3. **Usage ranking.** `UsageStatsManager` needs `PACKAGE_USAGE_STATS` (appop special access via `Settings.ACTION_USAGE_ACCESS_SETTINGS`). Without it you have only your own launch history. Confirmed by protection level.
4. **Settings.** Use a hand-curated list of `Settings.ACTION_*` intents. The Settings search index (`SearchIndexablesProvider`) is protected by `READ_SEARCH_INDEXABLES`, which is system-only. There is no public API.
5. **Web.** `Intent.ACTION_WEB_SEARCH` with `SearchManager.QUERY`, `ACTION_VIEW` to the default browser, or Custom Tabs. Suggestion endpoints (Google `suggestqueries`, DuckDuckGo `/ac`) are unofficial; I did not verify terms of use. `INTERNET` is a normal permission.
6. **Contacts, calendar, files, AppSearch, SearchManager.**
   - Contacts and calendar: `READ_CONTACTS`, `READ_CALENDAR` (runtime). Android 17 adds a Contact Picker, which is a picker, not a search source.
   - Files: `READ_MEDIA_*` (runtime; Play has a separate photo/video permission policy, from memory). SAF tree grants need no permission.
   - `MANAGE_EXTERNAL_STORAGE`: Play permits "Search (On Device)" only where the app's "core purpose is to search through files and folders". A launcher is a weak fit.
   - AppSearch: `GlobalSearchSession` returns only data "the querying application has been granted access to", via per-package visibility or required permissions. The assistant role grants `READ_ASSISTANT_APP_SEARCH_DATA`; the home role grants `READ_HOME_APP_SEARCH_DATA`. `READ_GLOBAL_APP_SEARCH_DATA` is system-only. Which real apps publish data visible this way is unverified.
   - `SearchManager.getSearchablesInGlobalSearch()` is public and visibility-filtered. Actually querying other apps' suggestion providers depends on each provider's export and permissions (commonly `GLOBAL_SEARCH`, `signature|privileged`). Treat as unavailable.
7. **Calculator, unit conversion, definitions.** No permissions for on-device computation.
8. **Clipboard.** Since Android 10, only the default IME or the focused app can read it. No background history is possible. You can read the current clip while your overlay has focus (Android 12+ shows an access toast; from memory).
9. **Open windows and tasks.** A normal app sees only its own tasks. Usage access adds foreground-app history but no task switching. Accessibility adds `getWindows()` and global actions. Re-launching an app's launcher intent is the only permission-free "switch".
10. **Chrome tabs, bookmarks, history.** I found no public provider. The platform `Browser` bookmark APIs and `READ_HISTORY_BOOKMARKS` were removed in Android 6.0.

**Sources (C):**
- https://developer.android.com/reference/android/content/pm/LauncherApps
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/pm/ShortcutService.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/voiceinteraction/java/com/android/server/voiceinteraction/VoiceInteractionManagerService.java
- https://developer.android.com/training/package-visibility/declaring
- https://support.google.com/googleplay/android-developer/answer/10158779
- https://support.google.com/googleplay/android-developer/answer/10467955
- https://developer.android.com/reference/android/app/appsearch/GlobalSearchSession
- https://source.android.com/docs/core/settings/universal-search
- https://developer.android.com/about/versions/10/privacy/changes
- https://developer.android.com/about/versions/17/features
- https://developer.android.com/about/versions/marshmallow/android-6.0-changes

## D. Extensibility precedents

- **Kvaesitso.** Plugins are separate APKs exposing an exported `ContentProvider` with intent filter `de.mm20.launcher2.action.PLUGIN`. SDK: `de.mm20.launcher2:plugin-sdk`. Types: weather, file search, contacts, places, calendar. The security model is not described on the page I read.
- **Tasker/Locale plugins.** Intent-based `EDIT_SETTING` / `FIRE_SETTING` protocol (from memory, not re-verified).
- **Sesame.** Not researched; no evidence gathered.
- **App Actions (`shortcuts.xml` capabilities).** Consumed by Google Assistant. I found no public API for a third-party launcher to enumerate or invoke another app's capabilities (unverified negative).
- **AppFunctions (`android.app.appfunctions`, API 36; Jetpack `androidx.appfunctions`).**
  - Calling another app's functions requires `EXECUTE_APP_FUNCTIONS`.
  - Android 16: `internal|privileged`.
  - Android 17 (flag-gated): `normal`, but "the application must be on a device allowlist, and its ability to execute functions is subject to user approval". The reference repeats "allowlist checks ... enforced at runtime".
  - Discovery of other apps' metadata needs `DISCOVER_APP_FUNCTIONS` or `EXECUTE_APP_FUNCTIONS_SYSTEM` (role-only).
  - Net: not usable by an arbitrary Play app today. I could not find how the allowlist is populated.

**Sources (D):**
- https://kvaesitso.mm20.de/docs/developer-guide/plugins/get-started.html
- https://developer.android.com/ai/appfunctions
- https://developer.android.com/reference/android/app/appfunctions/AppFunctionManager
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/res/AndroidManifest.xml

## E. Permission tiers

| Tier | What is needed | Features unlocked | Play friction |
|---|---|---|---|
| **0** | Nothing (`<queries>` MAIN/LAUNCHER only) | User-bound Meta shortcut (16 QPR2+); app list + icons + work profile; Settings deep links; web search intents; calculator/units; own-history ranking; clipboard while focused; tile, widget, taskbar pin; freeform window sized by `<layout>`; blur where enabled; keyboard capture while focused (36.1+) | None |
| **1** | `INTERNET`; **assistant role** (user picks in Settings); optionally a VoiceInteractionService; `SYSTEM_ALERT_WINDOW` declared | Web suggestions; Meta+Space / Meta+A / assist key; `READ_ASSISTANT_APP_SEARCH_DATA`; overlay app-op, giving fullscreen-over-desktop translucent activity and background launch; with the service: `getShortcuts` and background-start exemption | No assistant-specific policy found. The service route needs a `RecognitionService` implementation. Do not request SMS/call-log |
| **2** | `READ_CONTACTS`, `READ_CALENDAR`, `READ_MEDIA_*` | Contact, calendar, media search | Standard data-safety and disclosure; photo/video policy (from memory) |
| **3** | Accessibility; usage access; notification listener; all-files; manual overlay grant | Global hotkey interception; window list; system-wide usage ranking; full file search; overlay without the role | Accessibility: declaration + prominent disclosure, not an "accessibility tool", blocked under Android 17 Advanced Protection. All-files: declaration, weak fit. `QUERY_ALL_PACKAGES`: declaration, likely unnecessary. Usage access and overlay: no dedicated form found (unverified) |

## Unverified items to test on the device

- Whether the Digital assistant picker is exposed, and whether Meta+Space is routed to the role holder or overridden by the system launcher.
- Feature-flag states: Meta+A contextual search, `EXECUTE_APP_FUNCTIONS` as normal, and the transparent-task permission check.
- How a non-exempt translucent activity actually looks in a freeform window.
- Whether the assistant role's overlay app-op requires the manifest declaration.
- Accessibility consumption of Meta combos in practice.
- The category-handler trick in A3.
