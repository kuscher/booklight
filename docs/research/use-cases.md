# Booklight: more use cases, by permission tier

*Desk research, 1 October 2026: developer.android.com, AOSP `android17-release`, Google Play policy,
Google API docs, and the device notes in Alex's repos. No device was touched.*

Marks: **(s)** read in AOSP source for this report. **(d)** checked on the HP Googlebook in earlier
notes. **(u)** unverified, listed in §9. Unmarked claims come from the developer docs.

## 1. How far without the scary permissions

Far. Of 63 ideas, 45 need nothing the user ever sees: 39 need no permission (tier 0) and 6 need
only an install-time one (tier 0+: `INTERNET`, which 1.0 has, or the alarm permission). 8 need one
ordinary prompt or picker (tier 1), 3 a Settings switch Play does not police (tier 2), and 7 need
accessibility, a notification listener, all-files access or an adb grant (tier 3). Email, notes,
files, folders, media keys, window placement and user-made commands are all in the first group.

## 2. Tiers

- **Tier 0:** no permission. Intents and public APIs.
- **Tier 0+:** a normal install-time permission, no prompt: `INTERNET`,
  `com.android.alarm.permission.SET_ALARM`, Tasker's `PERMISSION_RUN_TASKS`.
- **Tier 1:** a runtime prompt (`READ_CONTACTS`, `READ_CALENDAR`, `POST_NOTIFICATIONS`), a system
  picker (a folder, a contact), or a Google account consent screen.
- **Tier 2:** a role or special-access switch Play's permissions policy does not name: assistant
  role, notification policy access, `WRITE_SETTINGS`, `SCHEDULE_EXACT_ALARM`, usage access (which
  PLAN.md §6 had in tier 3).
- **Tier 3:** what makes review harder, plus what only adb can grant.

| Tier 3 item | What Play asks for |
| --- | --- |
| Accessibility service | Permissions Declaration Form and approval; in-app prominent disclosure with consent; a video. Launchers, assistants and automation tools are named as *not* accessibility tools. User-written rules are allowed; apps that "autonomously initiate, plan, and execute actions" are not. |
| All-files access | Declaration form; only where file management or file search is the app's core purpose. A weak fit. |
| Notification listener | Not named on Play's permissions page; no form found. Data safety and disclosure still apply. |
| Device admin | Not named there either. Not needed: accessibility covers "lock". |
| `WRITE_SECURE_SETTINGS` | Nothing. Install never grants it; the user runs `adb shell pm grant <pkg> android.permission.WRITE_SECURE_SETTINGS` once. Key Mapper on Play works this way. |

## 3. Ideas by family

Effort is S/M/L. Notice (1–3) is how much a user would notice the feature.

### Compose and capture

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Quick email | `mail anna@x.com Lunch? / See you at 1` | `ACTION_SENDTO` `mailto:`; Gmail's compose opens filled in (§4) | 0 | None | S | 3 |
| Quick note, local | `note buy milk` | Appends to `Documents/Booklight/Notes.md`; no window (§4) | 0 | None | S | 3 |
| Note in Keep or the notes app | Tab → "Keep" | `ACTION_SEND` to `com.google.android.keep` (u), or `Intent.ACTION_CREATE_NOTE` (§4) | 0 | None | S | 2 |
| Calendar event | `event Fri 3pm Dentist` | `ACTION_INSERT` on `CalendarContract.Events.CONTENT_URI` with `EXTRA_EVENT_BEGIN_TIME`, `EXTRA_EVENT_END_TIME`, `Events.TITLE`, `EVENT_LOCATION`; the editor opens | 0 | None | M | 3 |
| Reminder | `remind 5pm call bank` | `Intent.ACTION_CREATE_REMINDER` (`EXTRA_TITLE`, `EXTRA_TIME`) if an app answers (u); else a calendar event | 0 | None | S | 2 |
| Send through your own relay | `send anna@x.com / text` | HTTPS POST to the user's own webhook (§4) | 0+ `INTERNET` | Data safety | M | 2 |
| Send directly from Gmail | same | Gmail API, scope `gmail.send` (§4) | 1 Google consent | Google's OAuth verification | L | 3 |

### Making files and folders

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| New text or Markdown file | `new file ideas.md` | Created by path in `Documents/` (s), opened in the editor (§5) | 0 | None | M | 3 |
| New folder | `new folder Projects/Alpha` | `File.mkdirs()` under `Documents/` or `Download/` (s) | 0 | None | S | 3 |
| New Doc, Sheet, Slides | `doc`, `sheet`, `slides` | `doc.new`, `sheet.new`, `slide.new`, `form.new`; titled: `docs.google.com/document/create?title=…` (u) | 0 | None | S | 3 |
| File from a template | `new meeting-notes` | Templates with `{date}`, `{clipboard}`, `{argument}` | 0 | None | M | 2 |
| Granted folders | Settings → "Add a folder" | `ACTION_OPEN_DOCUMENT_TREE` once, `takePersistableUriPermission`; then silent `DocumentsContract.createDocument` | 1 picker | None | M | 2 |
| Save as… | Tab → "Create in…" | `ACTION_CREATE_DOCUMENT` (`EXTRA_TITLE`); the picker offers Drive and the Linux VM | 1 picker | None | S | 1 |

### Finding files

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Search Drive | `drive q3 report` | Opens `drive.google.com/drive/search?q=…`. Results in the panel need the restricted scope `drive.metadata.readonly`: out | 0 | None | S | 3 |
| Files in granted folders | a file name | An index of the persisted trees, newest first | 1 picker | None | L | 3 |
| Files Booklight made | a file name | Its own index of notes and new files | 0 | None | S | 2 |
| Downloads | `downloads` | `DownloadManager.ACTION_VIEW_DOWNLOADS` opens Files there | 0 | None | S | 2 |
| Everything on disk | — | `MANAGE_EXTERNAL_STORAGE` | 3 | Declaration; weak fit | L | 2 |

**Correction to PLAN.md §4.** "Recent downloads first" cannot be built below tier 3. The picker
refuses the `Download` folder and the storage root, the Downloads root offers no tree (s), and
`MediaStore.Downloads` returns only an app's own files. `Documents` and subfolders of `Download`
can be granted.

### Controlling the device

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Volume and mute | `vol 40`, `mute` | `AudioManager.setStreamVolume`, `adjustStreamVolume(STREAM_MUSIC, ADJUST_TOGGLE_MUTE, FLAG_SHOW_UI)` | 0 | None | S | 3 |
| Wi-Fi, internet, volume panels | `wifi` | `Settings.Panel.ACTION_WIFI`, `ACTION_INTERNET_CONNECTIVITY`, `ACTION_VOLUME`: a system sheet (u on desktop). No toggle exists: `setWifiEnabled` is dead since API 29 | 0 | None | S | 2 |
| Pages for what can't be toggled | `dark`, `night light`, `battery saver`, `glowbar`, `adb` | `android.settings.DARK_THEME_SETTINGS` (hidden, u), `ACTION_NIGHT_DISPLAY_SETTINGS`, `ACTION_BATTERY_SAVER_SETTINGS`, Glowbar settings (d), Wireless debugging (d) | 0 | None | S | 2 |
| Keyboard backlight | `backlight 50` | `InputDevice.getLightsManager()`, the `LIGHT_TYPE_KEYBOARD_BACKLIGHT` light; no permission check in source (s), untested (u) | 0 | None | M | 1 |
| Do Not Disturb | `dnd 1h` | `NotificationManager.setInterruptionFilter`; at targetSdk 35+ it switches an app-owned mode, not global DND (s) | 2 notification policy access | None found | M | 2 |
| Brightness | `brightness 60` | `Settings.System.SCREEN_BRIGHTNESS` | 2 `WRITE_SETTINGS` | None found | S | 2 |
| Keep awake | `awake 2h` | `Settings.System.SCREEN_OFF_TIMEOUT`, restored after; or BentoBar's extension with no grant here | 2 `WRITE_SETTINGS` | None found | S | 2 |
| Lock, screenshot, notifications, Quick Settings | `lock`, `shot` | `performGlobalAction`: `GLOBAL_ACTION_LOCK_SCREEN`, `_TAKE_SCREENSHOT`, `_NOTIFICATIONS`, `_QUICK_SETTINGS`, `_RECENTS` (s). Screenshots sooner through StudioSnap's extension | 3 accessibility | Declaration | M | 3 |
| Real toggles: dark theme, night light, battery saver, wireless debugging | `dark on` | `Settings.Secure` `ui_night_mode` (s), `night_display_activated` (u); `Settings.Global` `low_power` (u), `adb_wifi_enabled` (d) | 3 adb grant | Nothing | M | 2 |

Not possible for a Play app:
- **Glowbar.** `CONTROL_DEVICE_LIGHTS` is signature/privileged (d). One outlet reports from a
  Google briefing that it will open to developers; I found no public API. Booklight can open its
  settings and the `glowbar://disco` easter egg (d).
- **Dark theme and battery saver by API.** `UiModeManager.setNightMode` is refused
  (`config_lockDayNightMode` is true, s); battery saver needs `POWER_SAVER` (s).
- **Bluetooth on/off** (refused from targetSdk 33), **hotspot**, **sleep** (lock is nearest),
  **screen recording** (MediaProjection asks every time).

### Windows and multitasking

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Open an app in a place | Tab → "Open left half" | `LauncherApps.startMainActivity` with `ActivityOptions.makeBasic().setLaunchBounds(rect)`. New windows only; a running window keeps its bounds (d, s) | 0 | None | M | 3 |
| Workspaces | `work` | A saved set of apps with bounds, plus URLs, started in order | 0 | None | M | 3 |
| New window of an app | Tab → "New window" | Launcher intent with `FLAG_ACTIVITY_NEW_TASK \| FLAG_ACTIVITY_MULTIPLE_TASK`, where the app declares `PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI` (u per app) | 0 | None | S | 2 |
| Window list | a window title | `AccessibilityService.getWindows()` | 3 accessibility | Declaration | M | 3 |
| Close, minimise, snap, move to a desk | `close`, `snap left` | The system has keys for these (d), but apps can't inject keys (`INJECT_EVENTS` is signature). Only shell can: `input keyevent`, `am task resize` | 3 adb or Shizuku | Nothing | L | 2 |

Desks have no public API. `FLAG_ACTIVITY_LAUNCH_ADJACENT` is "only used for split-screen" (s).
Opening a running app already brings it forward.

### Automation

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Quicklinks with placeholders | `jira BOOK-12` | A URL or `intent:` URI with `{argument}`, `{clipboard}`, `{date}`; `Intent.parseUri(uri, URI_INTENT_SCHEME)` | 0 | None | M | 3 |
| Multi-step commands | `morning` | An ordered list of effects (open an app in a place, open a URL, set volume), run before the panel closes | 0 | None | M | 3 |
| Actions on selected text | Select text anywhere → "Booklight" | An `ACTION_PROCESS_TEXT` activity and an `ACTION_SEND` share target. The text becomes the object: Search, Email, Note, Transform. It can return changed text to an editable field | 0 | None | M | 3 |
| Snippets | `;sig` | Stored text with placeholders, copied; the user pastes | 0 | None | S | 2 |
| Clipboard transforms | `clip upper`, `clip json` | Read the clip while focused; change case, trim, format JSON, Base64; copy back | 0 | None | S | 2 |
| Apps' static shortcuts | `incognito` | Parse each launcher activity's `android.app.shortcuts` XML (s). Only exported targets start; `startShortcut` needs the launcher role | 0 | None | M | 2 |
| More ways in | — | Commands as dynamic shortcuts on Booklight's icon; a Quick Settings tile; opt-in launcher aliases that Keyboard shortcuts can bind to their own key (u) | 0 | None | S | 2 |
| Webhooks | `deploy`, `lights off` | HTTPS request to a user URL: Home Assistant, GitHub `workflow_dispatch`, ntfy | 0+ `INTERNET` | Data safety | M | 2 |
| Tasker tasks | `tasker backup` | Broadcast `net.dinglisch.android.tasker.ACTION_TASK` (u); needs Tasker's `PERMISSION_RUN_TASKS` and "Allow External Access" | 0+ | None | S | 1 |
| Linux commands | `$ git pull`, named scripts | A daemon in the VM on a 127.0.0.1 port, which the Terminal forwards to Android's localhost (d) | 0+ `INTERNET` | Data safety | L | 3 |
| Timed reminders | `in 20m stretch` | `AlarmManager.setWindow`, then a notification. It can't open a window later: background starts are blocked | 1 `POST_NOTIFICATIONS` | None | M | 2 |
| Paste in place, clipboard history | — | `AccessibilityService.getInputMethod()` and `commitText()` (s); clipboard watching | 3 accessibility | Declaration | L | 3 |

**The Linux helper's risk.** Any Android app can connect to a forwarded port (d). Every request
must prove a shared secret (HMAC with a nonce), the daemon should run named commands and not a free
shell, and it must bind IPv4 127.0.0.1. The user installs it by hand, because apps can't run
commands in the Terminal (`MANAGE_TERMINAL` is signature/preinstalled, d).

Not usable today: **AppFunctions** (`EXECUTE_APP_FUNCTIONS` is normal in 17, but the caller must be
on a device allowlist (s); early access only) and **App Actions** (no public caller API). Exact
alarm times need the tier 2 switch; Play reserves `USE_EXACT_ALARM` for alarm, timer and calendar apps.
Triggers and hosting Locale plug-ins would turn a launcher into Tasker: out.

### Answers in the panel

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Timer, alarm | `timer 10m tea`, `alarm 7:30` | `AlarmClock.ACTION_SET_TIMER` (`EXTRA_LENGTH`, `EXTRA_MESSAGE`, `EXTRA_SKIP_UI`); `ACTION_SET_ALARM` (`EXTRA_HOUR`, `EXTRA_MINUTES`) | 0+ `SET_ALARM` | None | S | 3 |
| Ask Gemini | `ask why is the sky blue` | `ACTION_SEND` text to `com.google.android.apps.bard` (exported, d; submits or only fills in: u) | 0 | None | S | 3 |
| Generators | `uuid`, `password 20`, `lorem 3` | `SecureRandom`; Enter copies | 0 | None | S | 2 |
| Colour values | `#3478f6` | A swatch plus rgb, hsl, oklch | 0 | None | S | 2 |
| QR code | `qr https://…` | Encoded on the device and drawn in the panel | 0 | None | M | 2 |
| Translate, define | `tr de good morning` | `Intent.ACTION_TRANSLATE`, `ACTION_DEFINE` with `EXTRA_TEXT` (handlers u), else the web. In the panel: ML Kit, about 30 MB per language | 0 | None | M | 2 |
| On-device Gemini Nano | `rewrite …` | ML Kit GenAI (Prompt, Rewriting, Summarization) on AICore; foreground only, per-app quota. The HP has an AICore feature (d) but no Googlebook is on the device list (u) | 0 | None | L | 3 |

Already planned, not repeated: units, currency, dates and time zones through Summa; emoji.

### People

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Your people | `anna` | A short list kept in Booklight (name, email, links). Actions: Email, Meet, Copy address | 0 | None | M | 3 |
| Pick a contact | `mail` → "Choose contact…" | Android 17 `ContactsPickerSessionContract.ACTION_PICK_CONTACTS`; no permission | 1 picker | None | M | 2 |
| Today's meetings | `next`, `join` | `CalendarContract.Instances`; Enter opens the event or its Meet link (u) | 1 `READ_CALENDAR` | Data safety | M | 3 |
| Contacts search | a name | `ContactsContract` query | 1 `READ_CONTACTS` | From 27 Jan 2027 Play requires the picker unless broad access is needed | M | 2 |

`meet.new` and `cal.new` are keyword rows. Other apps' direct-share shortcuts are not readable.

### Media

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Play, pause, next | `pause`, `next` | `AudioManager.dispatchMediaKeyEvent(KEYCODE_MEDIA_PLAY_PAUSE)`; no permission check (s) | 0 | None | S | 3 |
| Play something | `play radiohead` | `MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH` with `EXTRA_MEDIA_FOCUS` and `SearchManager.QUERY` | 0 | None | S | 3 |
| Open in Spotify, YouTube Music | `sp …`, `ytm …` | `spotify:search:…`, `music.youtube.com/search?q=…` (u) | 0 | None | S | 2 |
| Now playing | a row with controls | `MediaSessionManager.getActiveSessions` needs `MEDIA_CONTENT_CONTROL` (signature/privileged) or an enabled notification listener (s) | 3 listener | No form found | M | 2 |

### Developer

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Localhost ports | `:3000`, `ports` | Opens `http://localhost:3000`; `ports` tries common ports on 127.0.0.1, VM servers included (d) | 0+ `INTERNET` | None | S | 3 |
| GitHub jump | `gh booklight#12`, `gh prs` | URL patterns with a default owner | 0 | None | S | 3 |
| Text tools | `b64 …`, `jwt …`, `sha256 …`, `epoch …` | Pure Kotlin in `core/` | 0 | None | S | 2 |
| Open a project | `code booklight` | VSCodeBook as an extension listing recent folders | 0 | None | M | 2 |

## 4. Quick email and quick note

### Email: a compose window, which becomes a draft

```kotlin
Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:anna@example.com?subject=Lunch%3F&body=See%20you%20at%201"))
    .putExtra(Intent.EXTRA_SUBJECT, "Lunch?")
    .putExtra(Intent.EXTRA_TEXT, "See you at 1")
    .putExtra(Intent.EXTRA_CC, arrayOf("ben@example.com"))
```

- `ACTION_SENDTO` with `mailto:` reaches only mail apps. Put subject and body in both the URI and
  the extras; mail apps differ in which they read. `setPackage("com.google.android.gm")` forces
  Gmail. With an attachment: `ACTION_SEND`, `EXTRA_EMAIL` (a string array), `EXTRA_STREAM`.
- **In Gmail:** the compose window opens, filled in. Nothing is sent. Closed without sending, it
  stays as a draft (Gmail's normal behaviour; u on the Googlebook). So "a draft in Gmail" works
  today: `mail …`, Enter, close.
- No intent or extra means "save a draft without showing it" or "send now".

### "Directly emailed": the honest answer

| Route | What it takes | Verdict |
| --- | --- | --- |
| Gmail API `users.messages.send`, scope `gmail.send` (**sensitive**) | Google sign-in and consent. Google's OAuth verification: justification, demo video, privacy policy on a verified domain; typically 3–5 business days; no security assessment. Unverified apps get a warning screen and a user cap | The one real "send". Feasible in 2.x |
| Gmail API `users.drafts.create`, scope `gmail.compose` (**restricted**) | Restricted-scope verification, "several weeks"; a yearly CASA assessment if data can pass through a server | Out |
| The user's own relay | A few lines of Apps Script (`GmailApp.sendEmail`, `GmailApp.createDraft`) deployed as a web app under the user's account; Booklight POSTs to its URL with a secret. No verification for Booklight (u: not built) | Good for this audience; silent drafts too |
| SMTP or IMAP with an app password | A mail password stored in a launcher | Out |
| Accessibility pressing Send | Tier 3, fragile | Out |

Any sending route must show recipient and text and wait for Enter.

### Notes

- **Local file, the default.** `note buy milk` appends a dated line to
  `Documents/Booklight/Notes.md`. No permission and no window. After a reinstall Booklight no longer
  owns the file and can't write to it; a granted folder (tier 1) fixes that.
- **Keep.** `ACTION_SEND`, `text/plain`, `setPackage("com.google.android.keep")`, `EXTRA_SUBJECT`
  and `EXTRA_TEXT`: Keep's editor opens with the note (u). The documented older action is
  `com.google.android.gms.actions.CREATE_NOTE` with extras `com.google.android.gms.actions.extra.NAME`
  and `.TEXT`; who answers it now is unverified.
- **The notes role.** `Intent.ACTION_CREATE_NOTE` opens the user's default notes app in a floating
  window. It carries no text, only `EXTRA_USE_STYLUS_MODE`. The Googlebook binds Action+Ctrl+N to
  it (d).
- **APIs.** The Keep API is for Workspace administrators. The Tasks API needs OAuth verification.
- **Web.** `keep.new` opens a new Keep note in the browser.

## 5. Files and folders to make

| Route | Grant | Where | Notes |
| --- | --- | --- | --- |
| File path (`java.io.File`) | None | `Documents/`, `Download/` and below | MediaProvider lets an app create folders anywhere except new top-level ones, and files whose type fits the folder (s). It can later change only what it created. Not device-tested (u) |
| `MediaStore` insert | None | `MediaStore.Downloads`, or `MediaStore.Files` with `RELATIVE_PATH` | Returns a `content://` URI for an editor. No empty folders |
| `ACTION_CREATE_DOCUMENT` | Picker each time | Anywhere, Drive and the VM included | `EXTRA_TITLE`, MIME type, `DocumentsContract.EXTRA_INITIAL_URI` |
| `ACTION_OPEN_DOCUMENT_TREE` | Picker once per folder | Any folder except the storage root, `Download`, `Android/` (s) | Then `DocumentsContract.createDocument`; `Document.MIME_TYPE_DIR` makes a folder |
| Web | None | Drive | `doc.new`, `sheet.new`, `slide.new` |

- `new file ideas.md` creates the file in the default folder (`Documents/`, or a granted folder)
  and opens it with `ACTION_VIEW` plus read and write grant flags. The Googlebook ships a text
  editor (`com.google.android.desktop.texteditor`, d). Tab: "Create in…", "Copy path", "Show in
  Files".
- **Linux files.** The picker's "Linux VM" root opens at `/home/droid` but has no tree support (d):
  one file at a time, no silent folders. Better: share `Documents` into the VM (`/mnt/shared`, d),
  or use the Linux helper.
- **Desktop.** The launcher shows files from a "Home screen" folder (d, inferred). Its path is
  unknown (u).

## 6. Milestone map

| Release | Theme | Contents |
| --- | --- | --- |
| **1.1 Jot** | Capture and make; adds `SET_ALARM` | Quick email; local note; Keep and notes app; event; reminder; timer; new file, folder, Doc; templates; its own files in results; Drive search; Downloads; Ask Gemini |
| **1.2 Dials** | Media, device, answers | Play/pause/next; play from search; Spotify links; volume; panels; settings pages; keyboard backlight; generators; colour; QR; translate; text tools; GitHub jump; localhost ports |
| **1.3 Recipes** | The user's own commands | Quicklinks; multi-step commands; open in a place; workspaces; new window; snippets; clipboard transforms; actions on selected text; more ways in |
| **2.0 Extensions** | As planned | The contract; Summa, BentoBar (keep awake), StudioSnap (screenshot), VSCodeBook; static shortcuts; Tasker tasks |
| **2.1 Reach** | Network and tier 1 | Webhooks; relay send; Gmail send once verified; Linux commands; granted folders, save as, file search; your people, contact picker, today's meetings; timed reminders; Gemini Nano once tested |
| **2.2 Switches** | Tier 2, each opt-in | Do Not Disturb; brightness; keep awake; the assistant role (planned); exact reminders |
| **3.0 Power pack** | Tier 3, switched on together | One accessibility service: lock, screenshot, notifications, window list, paste in place, clipboard history, any hotkey. Notification listener: now playing. Optional adb grant: the real toggles. Optional Shizuku: window commands |

The power pack costs one declaration, one disclosure screen and one video. Consider shipping it as
a separate app that plugs in through the 2.0 contract, as BentoBar and StudioSnap already carry
their own accessibility services. A refused declaration then never blocks a Booklight update.

**Stays out:** Gmail drafts and Drive results by API (restricted scopes); all-files access;
`READ_CONTACTS` search unless the picker proves too weak; media search (`READ_MEDIA_*`); device
admin; SMTP; screen recording; Glowbar control and desks (no API); AppFunctions and App Actions
(closed); triggers and Locale plug-in hosting.

## 7. Top ten to build first

1. **Quick email.** Asked for; one intent, no permission, and the draft comes free.
2. **Quick note to a local file.** The only capture that needs no second window.
3. **New file and new folder.** Asked for, and tier 0 by plain file paths.
4. **Open an app in a place, and workspaces.** Only makes sense on a desktop Android, and needs just `ActivityOptions`.
5. **Quicklinks with placeholders.** The base of user automation; `intent:` URIs reach into any app.
6. **Multi-step commands.** "morning" is what people mean by automation, and effects are already data.
7. **Play, pause, next and volume.** Used many times a day, a few lines each.
8. **Actions on selected text.** Gives Booklight an object to act on without accessibility.
9. **Timer and calendar event.** Public intents, already promised in PLAN.md.
10. **Localhost ports.** Small, and aimed at people who build on the device.

## 8. Sources

AOSP paths are under `https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/`
unless a full URL is given.

**Tiers and Play policy**
- https://support.google.com/googleplay/android-developer/answer/9888170
- https://support.google.com/googleplay/android-developer/answer/10964491
- https://support.google.com/googleplay/android-developer/answer/10467955
- `core/res/AndroidManifest.xml` (protection levels)
- https://docs.keymapper.club/user-guide/adb-permissions/

**Compose and capture; email and notes**
- https://developer.android.com/guide/components/intents-common
- https://developers.google.com/workspace/gmail/api/auth/scopes
- https://developers.google.com/identity/protocols/oauth2/production-readiness/sensitive-scope-verification
- https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification
- https://developers.google.com/android/reference/com/google/android/gms/actions/NoteIntents
- https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/create-a-note-taking-app
- https://developers.google.com/workspace/keep/api/guides
- https://developers.google.com/workspace/tasks/auth
- `core/java/android/content/Intent.java`
- https://android.googlesource.com/platform/packages/modules/Permission/+/refs/heads/android17-release/PermissionController/res/xml/roles.xml

**Files**
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/training/data-storage/shared/media
- https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/android17-release/src/com/android/providers/media/MediaProvider.java
- `packages/ExternalStorageProvider/src/com/android/externalstorage/ExternalStorageProvider.java`
- https://android.googlesource.com/platform/packages/providers/DownloadProvider/+/refs/heads/android17-release/src/com/android/providers/downloads/DownloadStorageProvider.java
- https://developers.google.com/workspace/drive/api/guides/api-specific-auth
- https://www.klocworks.com/post/dot-new-shortcuts

**Device control and media**
- `services/core/java/com/android/server/UiModeManagerService.java`, `core/res/res/values/config.xml`
- `core/java/android/accessibilityservice/AccessibilityService.java`
- `services/core/java/com/android/server/media/MediaSessionService.java`, `media/java/android/media/AudioManager.java`
- `core/java/android/app/NotificationManager.java`
- `services/core/java/com/android/server/power/PowerManagerService.java`
- `services/core/java/com/android/server/input/InputManagerService.java`, `core/java/android/hardware/lights/Light.java`
- `core/java/android/provider/Settings.java`, `core/java/android/provider/AlarmClock.java`
- https://www.androidheadlines.com/2026/09/googles-glowbar-on-googlebooks-is-everything-pixel-hilight-should-have-been.html
- https://www.androidauthority.com/googlebook-glowbar-disco-app-3715417/

**Windows**
- https://developer.android.com/develop/adaptive-apps/guides/support-desktop-windowing
- `services/core/java/com/android/server/wm/DesktopModeLaunchParamsModifier.java`, `core/java/android/app/ActivityOptions.java`

**Automation**
- https://tasker.joaoapps.com/invoketasks.html
- https://developer.android.com/ai/appfunctions
- `core/java/android/app/appfunctions/AppFunctionManager.java`, `services/core/java/com/android/server/pm/ShortcutParser.java`
- https://developer.android.com/about/versions/14/changes/schedule-exact-alarms

**Answers and people**
- https://developers.google.com/ml-kit/genai
- https://developers.google.com/ml-kit/language/translation/android
- https://developer.android.com/about/versions/17/features/contact-picker

**Comparable products** (where ideas came from)
- https://manual.raycast.com/dynamic-placeholders
- https://www.alfredapp.com/help/features/universal-actions/
- https://learn.microsoft.com/en-us/windows/powertoys/command-palette/overview
- https://userbase.kde.org/Plasma/Krunner

**Device notes (d)**
- `docs/research/device-findings.md` (this repo)
- Welcome's device notes (a private repo; the path is in the private notes) (Glowbar, system shortcuts, Files, Gemini)
- `kuscher/vscodebook`, `docs/GOOGLEBOOK.md` (Linux VM, port forwarding, launch bounds, wireless debugging)

## 9. Not verified

Nothing here was run on a device. Check on the HP before building:

- Creating files and folders by path with no permission (read in source only), and an editor
  writing through the URI Booklight hands it.
- Gmail keeping a closed compose window as a draft, and whether it reads the URI or the extras.
- Whether Keep is installed and how it treats `ACTION_SEND` and the `CREATE_NOTE` action; who holds
  the notes role; handlers for `ACTION_CREATE_REMINDER`, `ACTION_TRANSLATE`, `ACTION_DEFINE`.
- `docs.google.com/document/create?title=`; Spotify and YouTube Music search links.
- `Settings.Panel` sheets on the desktop; the `DARK_THEME_SETTINGS` action; keyboard backlight.
- The keys `night_display_activated` and `low_power` (from memory).
- New-window launches per app; launch bounds across apps; launcher aliases in Keyboard shortcuts.
- Whether Gemini submits shared text. ML Kit GenAI on a Googlebook (the device list names phones).
- Tasker's `ACTION_TASK` string and the protection level of `PERMISSION_RUN_TASKS`.
- The Apps Script relay (designed, not built). The Tasks API scope's classification.
- Where Calendar stores a Meet link; the path of the desktop's "Home screen" folder.

Not researched: MacroDroid and Automate. Kvaesitso's docs page returned 404; Sesame publishes none.

Play: I found no form for notification listeners, device admin, `WRITE_SETTINGS` or notification
policy access. That is absence of evidence, not a ruling. Play's view of Shizuku-based apps is
unverified beyond such apps being listed. The Glowbar developer API rests on one outlet's account
of a briefing.
