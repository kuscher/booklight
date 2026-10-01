# Booklight: permissions and Play friction

Researched 2026-10-01 from AOSP (`android16-qpr2-release`, spot-checked against `android17-release`), developer.android.com and Play Console Help. No device testing. Protection levels are identical on both branches; Android 17 only adds `android:permissionFlags="allowedInPrivateComputeCore"` to most of them.

Confidence: **confirmed** (read in source or docs), **reported** (inferred from source or secondary), **unverified**.

Play rules that apply throughout:

- **Forms.** The policy pages name a declaration only for `USE_EXACT_ALARM` and foreground-service types among the items below. `REQUEST_DELETE_PACKAGES`, `WRITE_SETTINGS`, `ACCESS_NOTIFICATION_POLICY`, `SET_ALARM` and `WAKE_LOCK` do not appear in the policy text at all.
- **Special permissions.** These count as restricted: "restricted permissions are permissions that are designated as Dangerous, Special, Signature, or as documented below". The app must "Direct users to the system settings page for approval of special permissions".
- **Settings changes (Deceptive Behavior 2.1).** "We don't allow apps that make changes to the user's device settings or features outside of the app without the user's knowledge and consent"; 2.2.1 bans changes "not easily reversible".
- **Data safety.** "'Collect' means transmitting data from your app off a user's device." None of these permissions creates an entry by itself.

Policy URLs: https://support.google.com/googleplay/android-developer/answer/9888170 (permissions), https://support.google.com/googleplay/android-developer/answer/9888077 (deceptive behavior), https://support.google.com/googleplay/android-developer/answer/10787469 (data safety).

## 1. Uninstalling another app

- **Permission:** `REQUEST_DELETE_PACKAGES`, `android:protectionLevel="normal"`. Install-time, no prompt.
- **Since when:** targetSdk 28. Manifest comment: "Apps targeting APIs P or greater must hold this permission in order to use ACTION_UNINSTALL_PACKAGE or PackageInstaller#uninstall". `ACTION_DELETE` reaches the same `UninstallerActivity`, so it needs the permission too. `ACTION_UNINSTALL_PACKAGE` is deprecated in favour of `PackageInstaller.uninstall`.
- **Without it:** no exception and no dialog. The installer logs "Uid … does not have REQUEST_DELETE_PACKAGES or DELETE_PACKAGES", sets `RESULT_FIRST_USER` and finishes.
- **Dialog (AOSP strings):** app icon and label, "Do you want to uninstall this app?", Cancel / OK. Google's own installer wording is unverified.
- **Play:** nothing. Contrast `REQUEST_INSTALL_PACKAGES` (`signature|appop`), "restricted to the app's core functionality" with "a declaration form in your Play Console". Stay clear of Deceptive Behavior 2.2.3: "Apps that mislead users into removing or disabling third-party apps".
- **Launchers that declare it:** AOSP Launcher3, Lawnchair (`app.lawnchair.play`), KISS (`fr.neamar.kiss`), Olauncher (`app.olauncher`). Read in their source manifests; Play listings exist; shipped APKs not inspected.

Sources:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/res/AndroidManifest.xml#6398
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/packages/PackageInstaller/src/com/android/packageinstaller/UninstallerActivity.java#135
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/packages/PackageInstaller/AndroidManifest.xml#110
- https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/android16-qpr2-release/AndroidManifest-common.xml#40
- https://github.com/LawnchairLauncher/lawnchair/blob/16-dev/AndroidManifest-common.xml
- https://github.com/Neamar/KISS/blob/master/app/src/main/AndroidManifest.xml
- https://github.com/tanujnotes/Olauncher/blob/master/app/src/main/AndroidManifest.xml
- https://support.google.com/googleplay/android-developer/answer/9888170
- https://support.google.com/googleplay/android-developer/answer/9888077

## 2. Timers and alarms through the clock app

- **Permission:** `com.android.alarm.permission.SET_ALARM`, `normal`. No prompt.
- **Still required:** yes. Docs: "To invoke the ACTION_SET_TIMER intent, your app must have the SET_ALARM permission" (same for `ACTION_SET_ALARM`). AOSP DeskClock puts `android:permission="com.android.alarm.permission.SET_ALARM"` on the alias handling both actions, so `startActivity` without it throws `SecurityException`. Google Clock is closed source: reported to match.
- **`EXTRA_SKIP_UI`:** "If true, the application is asked to bypass any intermediate UI." Default false; a request, not a guarantee. With no `EXTRA_LENGTH`, `ACTION_SET_TIMER` ignores it and opens the picker.
- **Play:** nothing.

Sources:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/res/AndroidManifest.xml#2074
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/java/android/provider/AlarmClock.java#129
- https://android.googlesource.com/platform/packages/apps/DeskClock/+/refs/heads/android16-qpr2-release/AndroidManifest.xml#123
- https://developer.android.com/guide/components/intents-common

## 3. Exact alarms and the reminder notification

- **`SCHEDULE_EXACT_ALARM`:** `signature|privileged|appop`. Special access "Alarms & reminders", off by default for targetSdk 33+. Intent: `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` with `package:` URI (returns `RESULT_OK` if granted). Check `AlarmManager.canScheduleExactAlarms()`. Revoking it stops the app and cancels its exact alarms.
- **`USE_EXACT_ALARM`:** `normal`, granted at install, not revocable.
- **Without either:** `SecurityException` ("Caller … needs to hold SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM to set exact alarms."). Inexact alarms (`setWindow`, `setAndAllowWhileIdle`) need nothing.
- **Play, `USE_EXACT_ALARM`:** restricted. "Apps that request this restricted permission are subject to review, and those that do not meet the acceptable use case criteria will be disallowed from publishing on Google Play." Acceptable: "The app is an alarm or timer app" or "a calendar app that shows event notifications". Also: "Complete Play Console declaration to indicate app functionality." A launcher does not qualify.
- **Play, `SCHEDULE_EXACT_ALARM`:** the policy points to it as the fallback ("Use SCHEDULE_EXACT_ALARM instead if the above criteria is not met") and names no form for it. That the Console shows none is reported, not confirmed.
- **`POST_NOTIFICATIONS`:** `dangerous|instant`, runtime prompt. If denied, "your app can't send notifications"; no exception. Play: no form.

Sources:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/res/AndroidManifest.xml#6349
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/apex/jobscheduler/service/java/com/android/server/alarm/AlarmManagerService.java#2803
- https://developer.android.com/develop/background-work/services/alarms/schedule
- https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- https://developer.android.com/develop/ui/views/notifications/notification-permission
- https://support.google.com/googleplay/android-developer/answer/9888170

## 4. Do Not Disturb

- **Permission:** `ACCESS_NOTIFICATION_POLICY`, `normal`, a "Marker permission". Real access is a Settings switch: `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` (takes no package URI; the per-app action is `@SystemApi`). Not available on managed profiles. Check with `isNotificationPolicyAccessGranted()`.
- **Without it:** `SecurityException("Notification policy access denied")`.
- **Targeting 35+:** "Apps that target Android 15 (API level 35) and higher can no longer change the global state or policy of Do Not Disturb". `setInterruptionFilter` instead creates and toggles an implicit `AutomaticZenRule` owned by the app. Booklight can turn its own mode on, and `INTERRUPTION_FILTER_ALL` only deactivates that rule ("if there is no implicit rule, the call will be ignored"). It cannot switch off DND that the user or another app turned on. Explicit rules work through `addAutomaticZenRule` and `setAutomaticZenRuleState`. Exempt: system, System UI, companion-device managers.
- **Play:** no form. Settings-change rule applies.

Sources:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/res/AndroidManifest.xml#7575
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/java/android/app/NotificationManager.java#3362
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/services/core/java/com/android/server/notification/NotificationManagerService.java#6710
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/services/core/java/com/android/server/notification/ZenModeHelper.java#580
- https://developer.android.com/about/versions/15/behavior-changes-15

## 5. Brightness

- **System brightness:** `WRITE_SETTINGS`, `signature|preinstalled|appop|pre23|role`. Settings switch via `Settings.ACTION_MANAGE_WRITE_SETTINGS` with `package:` URI; check `Settings.System.canWrite()`. Without it, the write throws `SecurityException` ("… was not granted this permission: android.permission.WRITE_SETTINGS.").
- **`SCREEN_BRIGHTNESS`:** "between 1 (minimum) and 255 (maximum)". In automatic mode "the system may change SCREEN_BRIGHTNESS automatically". The int setting is still synced to the float brightness by `BrightnessSynchronizer` (reported from source, not device-tested).
- **Own window only:** `WindowManager.LayoutParams.screenBrightness` (0 to 1), no permission, applies only while that window is in front.
- **Play:** no form. Settings-change rule applies.

Sources:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/res/AndroidManifest.xml#4915
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/java/android/provider/Settings.java#5391
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/java/android/view/WindowManager.java#4048
- https://developer.android.com/training/permissions/requesting-special

## 6. Keep awake

Cheapest first, in Play terms:

1. **`FLAG_KEEP_SCREEN_ON` on a visible Booklight window.** No permission, no Play friction. Works "as long as this window is visible to the user"; "If an app with the FLAG_KEEP_SCREEN_ON flag goes into the background, the system allows the screen to turn off normally." A small visible desktop window should qualify (unverified on device).
2. **`SCREEN_OFF_TIMEOUT` via `WRITE_SETTINGS`.** One Settings switch, shared with brightness; no form. It changes the user's real setting, so the old value must be restored (2.2.1).
3. **Overlay window with the flag.** `TYPE_APPLICATION_OVERLAY` "Requires SYSTEM_ALERT_WINDOW" (`signature|setup|appop|installer|pre23|development`), a Settings switch. No form.
4. **Foreground service plus wake lock.** `WAKE_LOCK` `normal|instant`, `FOREGROUND_SERVICE` `normal|instant`, `FOREGROUND_SERVICE_SPECIAL_USE` `normal|appop|instant`. Only the deprecated `SCREEN_DIM_WAKE_LOCK` / `SCREEN_BRIGHT_WAKE_LOCK` keep the screen on; `PARTIAL_WAKE_LOCK` does not. Play: "you'll need to declare any foreground service types that you use in a new declaration on the App content page", with a description and "a link to a video demonstrating each foreground service feature"; "All foreground service types are subject to review." The service must also be something that "can't be interrupted or deferred by the system without causing a negative user experience". This is the most expensive route.

Sources:
- https://developer.android.com/develop/background-work/background-tasks/awake/screen-on
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/java/android/os/PowerManager.java#92
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/res/AndroidManifest.xml#8272
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://support.google.com/googleplay/android-developer/answer/13392821
- https://support.google.com/googleplay/android-developer/answer/9888379

## 7. Digital assistant role

- **Qualifying:** either an exported activity with an `android.intent.action.ASSIST` filter (default category), or a `VoiceInteractionService` protected by `BIND_VOICE_INTERACTION` whose metadata has `sessionService`, `recognitionService` and `supportsAssist="true"`. The activity route is enough.
- **Not requestable:** `roles.xml` sets `requestable="false"`; `RequestRoleActivity` logs "Role is not requestable" and finishes.
- **Settings:** `Settings.ACTION_VOICE_INPUT_SETTINGS` opens the "Assist & voice input" page in AOSP Settings; `Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS` opens Default apps. The direct per-role screen needs `MANAGE_ROLE_HOLDERS`.
- **Play:** no assistant-specific policy found. The only mention is that SMS and Call Log permissions require being "the default SMS, Phone, or Assistant handler". The role grants those only if the manifest requests them, so do not request them.
- **Unverified:** how the assistant is invoked on the target laptop.

Sources:
- https://android.googlesource.com/platform/packages/modules/Permission/+/refs/heads/android16-qpr2-release/PermissionController/res/xml/roles.xml#105
- https://android.googlesource.com/platform/packages/modules/Permission/+/refs/heads/android16-qpr2-release/PermissionController/role-controller/java/com/android/role/controller/behavior/AssistantRoleBehavior.java
- https://android.googlesource.com/platform/packages/modules/Permission/+/refs/heads/android16-qpr2-release/PermissionController/src/com/android/permissioncontroller/role/ui/RequestRoleActivity.java#127
- https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/android16-qpr2-release/AndroidManifest.xml#1352

## 8. Media control without notification access

- **`AudioManager.dispatchMediaKeyEvent`:** no permission; the server does no permission check. Events are dropped while a global-priority session (a call) is active.
- **`adjustStreamVolume` / `setStreamVolume`:** no permission. They throw `SecurityException("Not allowed to change Do Not Disturb state")` only when the change "triggers a Do Not Disturb change and the caller is not granted notification policy access", which means ringer-mode transitions. `STREAM_MUSIC` does not do that.
- **Android 17 change:** "All apps running on Android 17 that have these background audio interactions must have a visible activity or must be running a foreground service", otherwise "the audio playback and volume change APIs fail silently". Booklight must change volume while its window is still visible.
- **Reading what is playing:** `MediaSessionManager.getActiveSessions` needs `MEDIA_CONTENT_CONTROL` (`signature|privileged`, "Not for use by third-party applications") or an enabled notification listener; otherwise `SecurityException("Missing permission to control media.")`.
- **Play:** nothing for key events or volume.

Sources:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/media/java/android/media/AudioManager.java#991
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/services/core/java/com/android/server/audio/AudioService.java#5677
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/services/core/java/com/android/server/media/MediaSessionService.java#937
- https://developer.android.com/about/versions/17/changes/bg-audio

## 9. Files and folders with no permission

- **Yes, both ways.** For generic files MediaProvider allows the primary directories `Download` and `Documents`. Through FUSE (`java.io.File`): "This is a non-legacy app. Rest of the directories are generally writable except for non-default top-level directories." So `Documents/Booklight/Notes.md` and new subfolders work; a new top-level folder does not. `MediaStore.Files` inserts accept `Documents/` or `Download/`; `MediaStore.Downloads` accepts `Download/` only.
- **Afterwards:** the app can read, edit, rename and delete files it owns, not files other apps put in its folder: "If your app wants to access a file within the MediaStore.Downloads collection that your app didn't create, you must use the Storage Access Framework." If another editor replaces the file, ownership moves (reported).
- **After reinstall:** uninstall nulls `owner_package_name`. Files stay on disk but the new install cannot open them, and re-creating the same path returns `EEXIST` (reported from source). The docs say "it's better to use the Storage Access Framework for these use cases".
- **Play:** nothing. Only `MANAGE_EXTERNAL_STORAGE` carries a declaration.

Sources:
- https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/android16-qpr2-release/src/com/android/providers/media/MediaProvider.java#11829
- https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/android16-qpr2-release/src/com/android/providers/media/MediaProvider.java#2432
- https://developer.android.com/training/data-storage/shared/media
- https://developer.android.com/training/data-storage/shared/documents-files

## 10. App widget and Quick Settings tile

- **Widget:** no permission. `requestPinAppWidget` needs a foreground activity or service ("Otherwise it'll throw IllegalStateException") and a default launcher that supports it (`isRequestPinAppWidgetSupported`).
- **Tile:** the service declares `android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"` (`signature|recents`); the app does not hold it. `StatusBarManager.requestAddTileService` (API 33+) shows a system prompt; "The requesting application must be in the foreground … and the TileService must be exported."
- **Play:** nothing.

Sources:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/java/android/appwidget/AppWidgetManager.java#1506
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/java/android/app/StatusBarManager.java#1014
- https://developer.android.com/develop/ui/views/quicksettings-tiles

## 11. Launching into a particular window

- **No permission for any of it.** `SafeActivityOptions.checkPermissions` gates launch task id, display area, private displays, pinned mode and similar, not launch bounds.
- **`setLaunchBounds`:** "ignored on devices that don't have FEATURE_FREEFORM_WINDOW_MANAGEMENT or FEATURE_PICTURE_IN_PICTURE enabled". In desktop windowing the bounds are taken from the options, though desktop policy may adjust size or cascade position (reported from source). `setLaunchWindowingMode` is `@hide @TestApi`.
- **`FLAG_ACTIVITY_LAUNCH_ADJACENT`:** "only used for split-screen multi-window mode", best effort, needs `FLAG_ACTIVITY_NEW_TASK`. Behaviour inside desktop windowing is unverified.
- **Second window of another app:** `NEW_TASK | MULTIPLE_TASK` (or `NEW_DOCUMENT | MULTIPLE_TASK`) forces a new task for any exported activity. No opt-in is needed; the target opts out by launch mode, because the starter reuses the existing task when the activity is `singleTask` or `singleInstance`. `PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI` is only "an explicit signal for system UI"; Booklight can read it to decide whether to offer "New window".

Sources:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/java/android/app/ActivityOptions.java#1488
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/services/core/java/com/android/server/wm/SafeActivityOptions.java#249
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/services/core/java/com/android/server/wm/ActivityStarter.java#3011
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/services/core/java/com/android/server/wm/DesktopModeBoundsCalculator.java#99
- https://developer.android.com/develop/ui/compose/layouts/adaptive/support-multi-window-mode

## 12. Receiving selected text

- **No permission.** Declare activity intent filters for `ACTION_PROCESS_TEXT` (`text/plain`; text arrives in `EXTRA_PROCESS_TEXT`) and `ACTION_SEND` `text/plain`.
- **Play:** nothing for the filters. Because Booklight holds `INTERNET`, sending that text to a server would be "collection" and need a data-safety entry.

Sources:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-qpr2-release/core/java/android/content/Intent.java#4835
- https://developer.android.com/training/sharing/receive
- https://support.google.com/googleplay/android-developer/answer/10787469

## Summary

| Feature | Permission / access | User-facing step | Play friction | Confidence |
|---|---|---|---|---|
| Uninstall another app | `REQUEST_DELETE_PACKAGES` (normal) | Install-time, no prompt; system confirm dialog per uninstall | None | Confirmed |
| Timer / alarm via clock | `SET_ALARM` (normal) | Install-time, no prompt | None | Confirmed (Google Clock: reported) |
| Exact reminders | `SCHEDULE_EXACT_ALARM` (appop) | Settings switch | None named; Console behaviour reported | Confirmed / reported |
| Exact reminders, auto-granted | `USE_EXACT_ALARM` (normal) | Install-time, no prompt | Restricted to alarm, timer, calendar apps; declaration | Confirmed |
| Reminder notification | `POST_NOTIFICATIONS` (dangerous) | Runtime prompt | None | Confirmed |
| Do Not Disturb | `ACCESS_NOTIFICATION_POLICY` + policy access | Settings switch | None; settings-change consent rule | Confirmed |
| System brightness | `WRITE_SETTINGS` (appop) | Settings switch | None; settings-change consent rule | Confirmed (effect on device: reported) |
| Own-window brightness | none | None | None | Confirmed |
| Keep awake, visible window | none (`FLAG_KEEP_SCREEN_ON`) | None | None | Confirmed (desktop window: unverified) |
| Keep awake, screen timeout | `WRITE_SETTINGS` | Settings switch | None; must be reversible | Confirmed |
| Keep awake, overlay | `SYSTEM_ALERT_WINDOW` (appop) | Settings switch | None named | Confirmed |
| Keep awake, service + wake lock | `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Install-time, no prompt; ongoing notification | Declaration form with video, reviewed | Confirmed |
| Assistant | `android.app.role.ASSISTANT` | Role, chosen in Settings | None found | Confirmed (invocation: unverified) |
| Media keys, volume | none | None | None | Confirmed |
| What is playing | notification listener (or signature permission) | Settings switch | Not researched | Confirmed (access rule) |
| Files in Documents / Download | none | None | None | Confirmed (ownership edge cases: reported) |
| Widget, Quick Settings tile | none held by the app | None; launcher or system confirm on request | None | Confirmed |
| Launch into a window | none | None | None | Confirmed in source; device behaviour unverified |
| Receive selected text | none (intent filters) | None | Data-safety entry only if text leaves the device | Confirmed |

## What would add real review risk

1. **`USE_EXACT_ALARM`.** A launcher is not an alarm, timer or calendar app; apps outside the criteria "will be disallowed from publishing". Use `SCHEDULE_EXACT_ALARM` or inexact alarms.
2. **Any foreground service.** `specialUse` means a Console declaration, a demo video and a reviewer's judgement. The window flag or the screen-timeout setting avoids it.
3. **`REQUEST_INSTALL_PACKAGES`**, if uninstall ever grows into install: core-functionality restricted, with a declaration.
4. **Silent or sticky settings changes.** Brightness, screen timeout and DND need in-app consent and an easy way back. A screen timeout left raised after a crash is the likely failure.
5. **Uninstall wording** that nudges users to remove other apps.
6. **Sending selected text or notes off the device** without a data-safety entry.
7. **Requesting SMS or Call Log permissions** because the assistant role would grant them. That triggers the Permissions Declaration Form and an "extended review" that "may require up to several weeks" (https://support.google.com/googleplay/android-developer/answer/9214102).

## What Play actually asked (1.1, 1 October 2026)

1.1 went to closed testing with `REQUEST_DELETE_PACKAGES`, `com.android.alarm.permission.SET_ALARM` and
`WRITE_SETTINGS` in its manifest. The Console's review step raised no declaration form and no error for any
of them; App content stayed "all caught up". (Closed testing; production review may look again.)
