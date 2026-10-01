# Booklight: more use cases, by permission tier

*Desk research, 1 October 2026. Sources: developer.android.com, AOSP `android17-release`, Google Play
policy pages, Google API docs, and the device notes already in Alex's repos. No device was touched.*

Marks: **(s)** read in AOSP `android17-release` source for this report. **(d)** checked on the HP
Googlebook in earlier notes (Booklight, Welcome, VSCodeBook). **(u)** not verified; listed again at
the end. Unmarked claims come from the developer docs.

## 1. How far without the scary permissions

Far. Of 63 ideas, 45 need nothing the user ever sees: 39 need no permission at all and 6 need only
an install-time permission (`INTERNET`, which 1.0 already has, or the alarm permission). Another 8
need one ordinary prompt or a system picker, and 3 need a switch in Settings that Play does not
police. Only 7 need accessibility, a notification listener, all-files access or an adb grant.
Email, notes, files, folders, media keys, window placement and user-made commands are all in the
first group.

| Tier | Ideas |
| --- | --- |
| 0 | 39 |
| 0+ | 6 |
| 1 | 8 |
| 2 | 3 |
| 3 | 7 |

## 2. Tiers

- **Tier 0:** no permission. Intents and public APIs.
- **Tier 0+:** a normal install-time permission, no prompt. Here: `INTERNET`,
  `com.android.alarm.permission.SET_ALARM`, and Tasker's own `PERMISSION_RUN_TASKS`.
- **Tier 1:** one runtime prompt (`READ_CONTACTS`, `READ_CALENDAR`, `POST_NOTIFICATIONS`), a system
  picker grant (a folder, a contact), or a Google account consent screen.
- **Tier 2:** a role or special-access switch that Play's permissions policy does not name:
  assistant role, notification policy access, `WRITE_SETTINGS`, `SCHEDULE_EXACT_ALARM`, usage access.
  (PLAN.md §6 had usage access in its tier 3; under this definition it is tier 2.)
- **Tier 3:** what makes review harder, plus what only adb can grant.

| Tier 3 item | What Play asks for |
| --- | --- |
| Accessibility service | Permissions Declaration Form and approval; an in-app prominent disclosure with consent; a video of the flow. Launchers, assistants and automation tools are named as *not* accessibility tools, so `isAccessibilityTool` stays unset. The app may follow user-written rules but must not "autonomously initiate, plan, and execute actions". Android 17 Advanced Protection blocks such services (press-reported, see android-platform.md). |
| All-files access (`MANAGE_EXTERNAL_STORAGE`) | Declaration form. Allowed only where file management or on-device file search is the app's core purpose. A launcher is a weak fit. |
| Notification listener | Not named on Play's permissions page; no form found. Data safety and a clear disclosure still apply, and Android shows its own warning. |
| Device admin | Not named on Play's permissions page. Not needed: accessibility covers "lock". |
| `WRITE_SECURE_SETTINGS` | Play asks nothing, because it can never be granted by install. The user runs `adb shell pm grant <pkg> android.permission.WRITE_SECURE_SETTINGS` once. Apps on Play work this way (Key Mapper). |

## 3. Ideas by family

Columns: effort S/M/L; notice 1–3 = how much a user would notice it.

### Compose and capture

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Quick email | `mail anna@x.com Lunch? / See you at 1` | `ACTION_SENDTO` `mailto:` with subject and body; Gmail's compose opens filled in (§4) | 0 | None | S | 3 |
| Quick note, local | `note buy milk` | Appends to `Documents/Booklight/Notes.md` by file path; the panel says "Saved" and closes (§4) | 0 | None | S | 3 |
| Note in Keep or the notes app | Tab on a note → "Keep" | `ACTION_SEND` text to `com.google.android.keep` (u), or `Intent.ACTION_CREATE_NOTE` for the notes-role app (empty note) | 0 | None | S | 2 |
| Calendar event | `event Fri 3pm Dentist` | `ACTION_INSERT` on `CalendarContract.Events.CONTENT_URI` with `EXTRA_EVENT_BEGIN_TIME`, `EXTRA_EVENT_END_TIME`, `Events.TITLE`, `EVENT_LOCATION`, `Intent.EXTRA_EMAIL` (guests); Calendar's editor opens | 0 | None | M | 3 |
| Reminder | `remind 5pm call bank` | `Intent.ACTION_CREATE_REMINDER` with `EXTRA_TITLE`, `EXTRA_TIME` if an app answers (u); otherwise a calendar event | 0 | None | S | 2 |
| Send through your own relay | `send anna@x.com / text` | HTTPS POST to a webhook the user set up (Apps Script, n8n). Sends or saves a draft with no Google review of Booklight (§4) | 0+ `INTERNET` | Data safety | M | 2 |
| Send directly from Gmail | same | Gmail API `users.messages.send`, scope `gmail.send` (§4) | 1 Google consent | Google's OAuth verification, not Play | L | 3 |

### Making files and folders

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| New text or Markdown file | `new file ideas.md` | Created by path in `Documents/` (s), opened in the editor through a `content://` URI with grant flags (§5) | 0 | None | M | 3 |
| New folder | `new folder Projects/Alpha` | `File.mkdirs()` under `Documents/` or `Download/` (s) | 0 | None | S | 3 |
| New Doc, Sheet, Slides | `doc`, `sheet`, `slides`, `form` | Opens `doc.new`, `sheet.new`, `slide.new`, `form.new`. With a title: `docs.google.com/document/create?title=…` (u) | 0 | None | S | 3 |
| File from a template | `new meeting-notes` | User templates with `{date}`, `{clipboard}`, `{argument}`; same creation path | 0 | None | M | 2 |
| Granted folders | Settings → "Add a folder" | `ACTION_OPEN_DOCUMENT_TREE` once, `takePersistableUriPermission`; then `DocumentsContract.createDocument` anywhere under it, silently | 1 picker | None | M | 2 |
| Save as… | Tab → "Create in…" | `ACTION_CREATE_DOCUMENT` with `EXTRA_TITLE` and a type; the picker offers Drive and the Linux VM | 1 picker | None | S | 1 |

### Finding files

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Search Drive | `drive q3 report` | Opens `https://drive.google.com/drive/search?q=…`. Results inside the panel would need `drive.metadata.readonly`, a restricted scope: out | 0 | None | S | 3 |
| Files in granted folders | a file name | An index of the persisted trees (`DocumentsContract` child queries), newest first | 1 picker | None | L | 3 |
| Files Booklight made | a file name | Its own notes and new files, from its own index | 0 | None | S | 2 |
| Downloads and Files places | `downloads` | `DownloadManager.ACTION_VIEW_DOWNLOADS`; a Files root with `ACTION_VIEW` + `vnd.android.document/root` (d: exported) | 0 | None | S | 2 |
| Everything on disk | — | `MANAGE_EXTERNAL_STORAGE` | 3 | Declaration; weak fit | L | 2 |

**Correction to PLAN.md §4.** "Recent downloads first" cannot be built below tier 3. The `Download`
folder and the storage root are blocked from the folder picker (s), the Downloads root offers no
tree at all (s), and `MediaStore.Downloads` returns only the files an app made itself. `Documents`
and subfolders of `Download` can be granted.

### Controlling the device

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Volume and mute | `vol 40`, `mute` | `AudioManager.setStreamVolume`, `adjustStreamVolume(STREAM_MUSIC, ADJUST_TOGGLE_MUTE, FLAG_SHOW_UI)` | 0 | None | S | 3 |
| Wi-Fi, internet, volume panels | `wifi` | `Settings.Panel.ACTION_WIFI`, `ACTION_INTERNET_CONNECTIVITY`, `ACTION_VOLUME`: a system sheet over the current app (u on desktop). There is no toggle: `setWifiEnabled` does nothing since API 29 | 0 | None | S | 2 |
| Pages for what can't be toggled | `dark`, `night light`, `battery saver`, `glowbar`, `adb` | `android.settings.DARK_THEME_SETTINGS` (hidden constant, u), `ACTION_NIGHT_DISPLAY_SETTINGS`, `ACTION_BATTERY_SAVER_SETTINGS`, Glowbar settings (d), Developer options scrolled to Wireless debugging (d) | 0 | None | S | 2 |
| Keyboard backlight | `backlight 50` | `InputDevice.getLightsManager()` session on the `LIGHT_TYPE_KEYBOARD_BACKLIGHT` light. No permission check in source (s); untested (u); lasts while the process lives | 0 | None | M | 1 |
| Do Not Disturb | `dnd 1h` | `NotificationManager.setInterruptionFilter`. At targetSdk 35+ this switches an app-owned mode, not global DND (s) | 2 notification policy access | None found | M | 2 |
| Brightness | `brightness 60` | `Settings.System.SCREEN_BRIGHTNESS` | 2 `WRITE_SETTINGS` | None found | S | 2 |
| Keep awake | `awake 2h` | `Settings.System.SCREEN_OFF_TIMEOUT`, restored afterwards. Or BentoBar's extension, with no grant in Booklight | 2 `WRITE_SETTINGS` | None found | S | 2 |
| Lock, screenshot, notifications, Quick Settings, recents | `lock`, `shot` | `AccessibilityService.performGlobalAction`: `GLOBAL_ACTION_LOCK_SCREEN`, `_TAKE_SCREENSHOT`, `_NOTIFICATIONS`, `_QUICK_SETTINGS`, `_RECENTS`, `_POWER_DIALOG` (s). Screenshots can come sooner through StudioSnap's extension | 3 | Accessibility declaration | M | 3 |
| Real toggles: dark theme, night light, battery saver, wireless debugging | `dark on` | `Settings.Secure` `ui_night_mode` (observed live, s) and `night_display_activated` (u); `Settings.Global` `low_power` (u) and `adb_wifi_enabled` (d) | 3 adb grant | Nothing | M | 2 |

Not possible for a Play app:
- **Glowbar.** `CONTROL_DEVICE_LIGHTS` is signature|privileged (d). One outlet reports from a Google
  briefing that it will be opened to developers; I found no public API or documentation. Booklight
  can open its settings page and the `glowbar://disco` easter egg (d).
- **Dark theme by API.** `UiModeManager.setNightMode` is refused: `config_lockDayNightMode` is true
  and `MODIFY_DAY_NIGHT_MODE` is signature|privileged|role (s).
- **Battery saver by API** (`POWER_SAVER` or `DEVICE_POWER`, s), **Bluetooth on/off**
  (`BluetoothAdapter.enable()` is refused from targetSdk 33), **hotspot**, **sleep** (lock is the
  nearest), **screen recording** (MediaProjection asks every time and would record the panel).

### Windows and multitasking

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Open an app in a place | Tab → "Open left half", "right half", "centre" | `LauncherApps.startMainActivity(…, ActivityOptions.makeBasic().setLaunchBounds(rect).toBundle())`. New windows only; a running window keeps its bounds (d, s) | 0 | None | M | 3 |
| Workspaces | `work` | A saved set of apps with bounds, plus URLs, started in order while the panel is visible | 0 | None | M | 3 |
| New window of an app | Tab → "New window" | Launcher intent with `FLAG_ACTIVITY_NEW_TASK \| FLAG_ACTIVITY_MULTIPLE_TASK`. Offered where the app declares `PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI` (u per app) | 0 | None | S | 2 |
| Window list | a window title | `AccessibilityService.getWindows()` | 3 | Accessibility declaration | M | 3 |
| Close, minimise, snap, move to a desk | `close`, `snap left` | The system has keys for these (Action+W, −, =, [ ], d), but apps cannot inject keys (`INJECT_EVENTS` is signature). Only shell can: `input keyevent`, `am task resize`, through adb or Shizuku | 3 adb or Shizuku | Nothing | L | 2 |

Desks have no public API. `FLAG_ACTIVITY_LAUNCH_ADJACENT` is "only used for split-screen" (s), and
`GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN` is an accessibility action. Opening a running app already
brings its window forward.

### Automation

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Quicklinks with placeholders | `jira BOOK-12` | A URL or an `intent:` URI with `{argument}`, `{clipboard}`, `{date}`; `Intent.parseUri(uri, URI_INTENT_SCHEME)`, then `startActivity` | 0 | None | M | 3 |
| Multi-step commands | `morning` | An ordered list of effects (open an app in a place, open a URL, set volume, copy text), run before the panel closes | 0 | None | M | 3 |
| Actions on selected text | Select text in any app → "Booklight" | An `ACTION_PROCESS_TEXT` activity, and an `ACTION_SEND` share target. The text becomes the object: Search, Email, Note, Translate, Transform. It can hand changed text back to an editable field | 0 | None | M | 3 |
| Snippets | `;sig` | Stored text with placeholders, copied to the clipboard; the user pastes | 0 | None | S | 2 |
| Clipboard transforms | `clip upper`, `clip json` | Read the clip while the panel has focus; change case, trim, format JSON, Base64, URL-decode, count; copy back | 0 | None | S | 2 |
| Apps' static shortcuts | `incognito` | Parse each launcher activity's `android.app.shortcuts` XML (s) and start the intent. Works only for exported targets; `LauncherApps.startShortcut` needs the launcher role | 0 | None | M | 2 |
| More ways in | — | Your commands as dynamic shortcuts on Booklight's icon; a Quick Settings tile; opt-in launcher aliases ("Booklight note") that Keyboard shortcuts can bind to their own key (u) | 0 | None | S | 2 |
| Webhooks | `deploy`, `lights off` | An HTTPS request to a user URL: Home Assistant, GitHub `workflow_dispatch`, ntfy, n8n | 0+ `INTERNET` | Data safety | M | 2 |
| Tasker tasks | `tasker backup` | Broadcast `net.dinglisch.android.tasker.ACTION_TASK` (u, from memory). Needs Tasker's `PERMISSION_RUN_TASKS` (level u) and its "Allow External Access" switch | 0+ | None | S | 1 |
| Linux commands | `$ git pull`, named scripts | A small daemon in the VM on a 127.0.0.1 port, which the Terminal forwards to Android's localhost (d). See below | 0+ `INTERNET` | Data safety | L | 3 |
| Timed reminders | `in 20m stretch` | `AlarmManager.setWindow` or WorkManager, then a notification. It cannot open a window later: background activity starts are blocked | 1 `POST_NOTIFICATIONS` | None | M | 2 |
| Paste in place, clipboard history | — | Accessibility: `getInputMethod().getCurrentInputConnection().commitText()` (s); clipboard watching | 3 | Accessibility declaration | L | 3 |

**The Linux helper and its risk.** Any Android app can connect to a forwarded port (d). So every
request must carry proof of a shared secret (HMAC with a nonce, never the secret in a URL), the
daemon should run a list of named commands by default and not a free shell, and it must bind IPv4
127.0.0.1. The user installs it by hand: apps cannot run commands in the Terminal
(`MANAGE_TERMINAL` is signature|preinstalled, d), and the VM must already be running.

Not usable today: **AppFunctions** (`EXECUTE_APP_FUNCTIONS` is a normal permission in 17, but the
caller must be on a device allowlist (s) and the docs say early access only); **App Actions** (no
public way to call another app's capabilities); **exact-time alarms** without the tier 2 switch
(`USE_EXACT_ALARM` is reserved by Play for alarm, timer and calendar apps). Triggers ("when Wi-Fi
connects") and hosting Locale plug-ins would turn a launcher into Tasker: out.

### Answers in the panel

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Timer, alarm | `timer 10m tea`, `alarm 7:30` | `AlarmClock.ACTION_SET_TIMER` (`EXTRA_LENGTH`, `EXTRA_MESSAGE`, `EXTRA_SKIP_UI=true`); `ACTION_SET_ALARM` (`EXTRA_HOUR`, `EXTRA_MINUTES`) | 0+ `com.android.alarm.permission.SET_ALARM` | None | S | 3 |
| Ask Gemini | `ask why is the sky blue` | `ACTION_SEND` text to `com.google.android.apps.bard` (exported, d; whether it submits or only fills in: u) | 0 | None | S | 3 |
| Generators | `uuid`, `password 20`, `lorem 3`, `random 1-100` | `SecureRandom`; Enter copies | 0 | None | S | 2 |
| Colour values | `#3478f6` | A swatch plus rgb, hsl, oklch; Enter copies | 0 | None | S | 2 |
| QR code | `qr https://…` | Encoded on the device (ZXing core) and drawn in the panel | 0 | None | M | 2 |
| Translate, define | `tr de good morning`, `define ersatz` | `Intent.ACTION_TRANSLATE`, `ACTION_DEFINE` with `EXTRA_TEXT` (handlers u), else the web. In the panel: ML Kit translation, about 30 MB per language, downloaded once | 0 | None | M | 2 |
| On-device Gemini Nano | `rewrite …`, `summarise clipboard` | ML Kit GenAI (Prompt, Rewriting, Proofreading, Summarization) on AICore. Foreground only, with a per-app quota. The HP reports an AICore feature (d), but no Googlebook is on the published device list (u) | 0 | None | L | 3 |

Already planned and not repeated: units, currency, dates and time zones through Summa; emoji and
symbols.

### People

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Your people | `anna` | A short list kept in Booklight (name, email, links), added by hand or with the picker. Actions: Email, Meet, Chat, Copy address | 0 | None | M | 3 |
| Pick a contact | `mail` → "Choose contact…" | Android 17 `ContactsPickerSessionContract.ACTION_PICK_CONTACTS` with the fields wanted; no permission | 1 picker | None | M | 2 |
| Today's meetings | `next`, `join` | `CalendarContract.Instances` for today; Enter opens the event or its Meet link (where the link is stored: u) | 1 `READ_CALENDAR` | Data safety | M | 3 |
| Contacts search | a name | `ContactsContract` query | 1 `READ_CONTACTS` | From 27 January 2027 Play's Contacts Permissions policy requires the picker unless broad access is needed | M | 2 |

`meet.new` and `cal.new` are plain keyword rows. Other apps' direct-share shortcuts are not
readable.

### Media

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Play, pause, next, previous | `pause`, `next` | `AudioManager.dispatchMediaKeyEvent(KEYCODE_MEDIA_PLAY_PAUSE …)`; no permission check (s) | 0 | None | S | 3 |
| Play something | `play radiohead` | `MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH` with `EXTRA_MEDIA_FOCUS` and `SearchManager.QUERY`, sent to the user's player | 0 | None | S | 3 |
| Open in Spotify, YouTube Music | `sp …`, `ytm …` | `spotify:search:…`, `https://music.youtube.com/search?q=…` (u) | 0 | None | S | 2 |
| Now playing | a row with title and controls | `MediaSessionManager.getActiveSessions` needs `MEDIA_CONTENT_CONTROL` (signature|privileged) or an enabled notification listener (s) | 3 | No form found; disclosure | M | 2 |

### Developer

| Idea | You type | How it works | Tier | Play | Effort | Notice |
| --- | --- | --- | --- | --- | --- | --- |
| Localhost ports | `:3000`, `ports` | Opens `http://localhost:3000`. `ports` tries common ports on 127.0.0.1 and lists what answers, servers in the VM included (d) | 0+ `INTERNET` | None | S | 3 |
| GitHub jump | `gh booklight`, `gh booklight#12`, `gh prs` | URL patterns with a default owner. Optional: the repo list from the API with the user's token | 0 | None | S | 3 |
| Text tools | `b64 …`, `jwt …`, `sha256 …`, `epoch 1759300000`, `0xff` | Pure Kotlin in `core/` | 0 | None | S | 2 |
| Open a project | `code booklight` | VSCodeBook as an extension that lists recent folders | 0 | None | M | 2 |

## 4. Quick email and quick note

### Email: a compose window, which becomes a draft

```kotlin
Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:anna@example.com?subject=Lunch%3F&body=See%20you%20at%201"))
    .putExtra(Intent.EXTRA_SUBJECT, "Lunch?")
    .putExtra(Intent.EXTRA_TEXT, "See you at 1")
    .putExtra(Intent.EXTRA_CC, arrayOf("ben@example.com"))
```

- `ACTION_SENDTO` with `mailto:` reaches only mail apps. Put subject and body in both the URI and
  the extras, since mail apps differ in which they read. `setPackage("com.google.android.gm")`
  forces Gmail.
- With an attachment, use `ACTION_SEND`, a MIME type, `EXTRA_EMAIL` (a string array) and
  `EXTRA_STREAM`.
- **What happens in Gmail:** its compose window opens, filled in. Nothing is sent. If the user
  closes it without sending, Gmail keeps it as a draft (Gmail's normal behaviour; u on the
  Googlebook). So "email draft into Gmail" works today: `mail …`, Enter, close.
- No intent or extra means "save as draft without showing" or "send now".

### "Directly emailed": the honest answer

| Route | What it takes | Verdict |
| --- | --- | --- |
| Gmail API `users.messages.send`, scope `gmail.send` (**sensitive**) | Google sign-in and consent; `INTERNET`. Google's OAuth verification: a justification, a demo video, a privacy policy on a verified domain; typically 3–5 business days; no security assessment. Until verified: a warning screen and a user cap | The one real "send". Feasible in 2.x |
| Gmail API `users.drafts.create`, scope `gmail.compose` (**restricted**) | Restricted-scope verification, "several weeks"; a yearly CASA security assessment if data can pass through a server | Out |
| The user's own relay | The user deploys a few lines of Apps Script (`GmailApp.sendEmail`, `GmailApp.createDraft`) as a web app under their own account and pastes the URL and a secret. Booklight POSTs to it. No Google verification for Booklight (u: not built) | Good for Alex's audience; gives silent drafts too |
| SMTP or IMAP with an app password | 2-Step Verification, a mail password stored in a launcher, a mail library | Out |
| Accessibility pressing Send | Tier 3, fragile | Out |

Whatever the route, sending must show the recipient and text and wait for Enter. A launcher that
mails on a typo is worse than one that opens a window.

### Notes

- **Local file (the default).** `note buy milk` appends a dated line to
  `Documents/Booklight/Notes.md`. No permission, no window, visible in Files and to any editor.
  Caveat: after a reinstall Booklight no longer owns the file and cannot write to it; a granted
  folder (tier 1) fixes that.
- **Keep.** `ACTION_SEND`, `text/plain`, `setPackage("com.google.android.keep")`,
  `EXTRA_SUBJECT` as title, `EXTRA_TEXT` as body: Keep's editor opens with the note (u). The older
  documented action is `com.google.android.gms.actions.CREATE_NOTE` with extras
  `com.google.android.gms.actions.extra.NAME` and `.TEXT`; which apps answer it now is unverified.
- **The notes role.** `Intent.ACTION_CREATE_NOTE` (`android.intent.action.CREATE_NOTE`) opens the
  user's default notes app in a floating window. It carries no text, only `EXTRA_USE_STYLUS_MODE`.
  The role is not requestable; the user picks it. The Googlebook already binds Action+Ctrl+N to it
  (d).
- **APIs.** The Keep API is for Workspace administrators, not consumer apps. The Tasks API needs
  Google's OAuth verification (classification of its scope: u).
- **Web.** `keep.new` and `note.new` open a new Keep note in the browser.

## 5. Files and folders to make

| Route | Grant | Where | Notes |
| --- | --- | --- | --- |
| File path (`java.io.File`) | None | `Documents/`, `Download/` and their subfolders | MediaProvider lets a non-legacy app create folders anywhere except new top-level ones, and files whose type fits the folder (s). The app can later change only what it created. Not device-tested (u) |
| `MediaStore` insert | None | `MediaStore.Downloads`, or `MediaStore.Files` with `RELATIVE_PATH` | Returns a `content://` URI to hand to an editor. Folders appear only as a side effect; no empty folder |
| `ACTION_CREATE_DOCUMENT` | Picker each time | Anywhere, Drive and the VM included | `EXTRA_TITLE`, MIME type, `DocumentsContract.EXTRA_INITIAL_URI` |
| `ACTION_OPEN_DOCUMENT_TREE` + `takePersistableUriPermission` | Picker once per folder | Any folder except the storage root, `Download`, `Android/` (s) | Then `DocumentsContract.createDocument(resolver, parent, mime, name)`; `Document.MIME_TYPE_DIR` makes a folder |
| Web | None | Drive | `doc.new`, `sheet.new`, `slide.new`, `form.new` |

Proposed behaviour:
- `new file ideas.md` creates the file in the default folder (a setting: `Documents/`, or a granted
  folder) and opens it with `ACTION_VIEW` plus read and write grant flags. The Googlebook ships a
  text editor (`com.google.android.desktop.texteditor`, d). Tab: "Create in…" (picker), "Copy path",
  "Show in Files".
- `new folder Projects/Alpha` creates it and offers "Show in Files".
- **Linux files.** The picker shows a "Linux VM" root that opens at `/home/droid` but has no tree
  support (d). So Booklight can save one file there through the picker, not make folders silently.
  Two better routes: share `Documents` into the VM (it appears under `/mnt/shared`, d), or use the
  Linux helper.
- **Desktop.** The launcher shows files from a "Home screen" folder on primary storage (d,
  inferred from strings). Its path is unknown (u), so "new file on the desktop" needs a device
  check.

## 6. Milestone map

| Release | Theme | Contents |
| --- | --- | --- |
| **1.1 Jot** | Capture and make. Adds `SET_ALARM` | Quick email; local note; Keep and notes app; event; reminder; timer and alarm; new file, folder, Doc; templates; Drive search; Downloads; Ask Gemini |
| **1.2 Dials** | Media, device, answers | Play/pause/next; play from search; Spotify and YouTube Music; volume; panels; settings pages; keyboard backlight; generators; colour; QR; translate and define; text tools; GitHub jump; localhost ports |
| **1.3 Recipes** | The user's own commands | Quicklinks with placeholders; multi-step commands; open in a place; workspaces; new window; snippets; clipboard transforms; actions on selected text; more ways in |
| **2.0 Extensions** | As planned | The contract; Summa, BentoBar (keep awake), StudioSnap (screenshot), VSCodeBook (projects); static app shortcuts; Tasker tasks |
| **2.1 Reach** | Network and tier 1 grants | Webhooks; send through a relay; Gmail send after Google's verification; Linux commands; granted folders, save as, file search; your people, contact picker, today's meetings; timed reminders; Gemini Nano once tested |
| **2.2 Switches** | Tier 2, each opt-in | Do Not Disturb; brightness; keep awake; the assistant role (already planned); exact reminders |
| **3.0 Power pack** | Tier 3, switched on together | One accessibility service: lock, screenshot, notifications, Quick Settings, window list, paste in place, clipboard history, any hotkey. Notification listener: now playing. Optional adb grant: dark theme, night light, battery saver, wireless debugging. Optional Shizuku: close, snap, move windows |

The power pack costs one declaration, one disclosure screen and one video. Consider shipping it as
a separate app that plugs in through the 2.0 extension contract, as BentoBar and StudioSnap already
carry their own accessibility services. Then a refused declaration never blocks a Booklight update,
and the main app stays at "no sensitive permissions".

**Stays out:** Gmail drafts by API (restricted scope); Drive results in the panel (restricted
scope); all-files access; contacts search by `READ_CONTACTS` unless the picker proves too weak;
media search (`READ_MEDIA_*`, Play wants the photo picker); device admin; SMTP with a stored
password; screen recording; Glowbar control and desks (no API); AppFunctions and App Actions
(closed); triggers and Locale plug-in hosting.

## 7. Top ten to build first

1. **Quick email.** Alex asked for it; one intent, no permission, and the draft comes free.
2. **Quick note to a local file.** The only capture that needs no second window.
3. **New file and new folder.** Asked for, tier 0 by file path, and nothing else on the device does it from the keyboard.
4. **Open an app in a place, and workspaces.** The one feature that only makes sense on a desktop Android, with plain `ActivityOptions`.
5. **Quicklinks with placeholders.** The base of all user automation, and `intent:` URIs make it reach into any app.
6. **Multi-step commands.** "morning" is what people mean by automation, and effects are already data.
7. **Play, pause, next and volume.** Used many times a day, a few lines each.
8. **Actions on selected text.** Gives Booklight an object to act on without accessibility.
9. **Timer and calendar event.** Public intents, visible results, already promised in PLAN.md.
10. **Localhost ports.** Small, and it speaks directly to people who build on the device.

## 8. Sources

**Tiers and Play policy**
- https://support.google.com/googleplay/android-developer/answer/9888170
- https://support.google.com/googleplay/android-developer/answer/10964491
- https://support.google.com/googleplay/android-developer/answer/10467955
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/res/AndroidManifest.xml
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
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/android/content/Intent.java
- https://android.googlesource.com/platform/packages/modules/Permission/+/refs/heads/android17-release/PermissionController/res/xml/roles.xml

**Files**
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/training/data-storage/shared/media
- https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/android17-release/src/com/android/providers/media/MediaProvider.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/packages/ExternalStorageProvider/src/com/android/externalstorage/ExternalStorageProvider.java
- https://android.googlesource.com/platform/packages/providers/DownloadProvider/+/refs/heads/android17-release/src/com/android/providers/downloads/DownloadStorageProvider.java
- https://developers.google.com/workspace/drive/api/guides/api-specific-auth
- https://www.klocworks.com/post/dot-new-shortcuts

**Device control and media** (all under `https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/`)
- `services/core/java/com/android/server/UiModeManagerService.java`, `core/res/res/values/config.xml`
- `core/java/android/accessibilityservice/AccessibilityService.java`
- `services/core/java/com/android/server/media/MediaSessionService.java`
- `media/java/android/media/AudioManager.java`
- `core/java/android/app/NotificationManager.java`
- `services/core/java/com/android/server/power/PowerManagerService.java`
- `services/core/java/com/android/server/input/InputManagerService.java`, `core/java/android/hardware/lights/Light.java`
- `core/java/android/provider/Settings.java`, `core/java/android/provider/AlarmClock.java`
- https://www.androidheadlines.com/2026/09/googles-glowbar-on-googlebooks-is-everything-pixel-hilight-should-have-been.html
- https://www.androidauthority.com/googlebook-glowbar-disco-app-3715417/

**Windows**
- https://developer.android.com/develop/adaptive-apps/guides/support-desktop-windowing
- `services/core/java/com/android/server/wm/DesktopModeLaunchParamsModifier.java`, `core/java/android/app/ActivityOptions.java` (same AOSP branch)

**Automation**
- https://tasker.joaoapps.com/invoketasks.html
- https://developer.android.com/ai/appfunctions
- `core/java/android/app/appfunctions/AppFunctionManager.java`, `services/core/java/com/android/server/pm/ShortcutParser.java` (same AOSP branch)
- https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- https://developer.android.com/privacy-and-security/local-network-permission

**Answers and people**
- https://developers.google.com/ml-kit/genai
- https://developers.google.com/ml-kit/language/translation/android
- https://developer.android.com/about/versions/17/features/contact-picker

**Comparable products** (where the ideas came from)
- https://manual.raycast.com/dynamic-placeholders
- https://www.alfredapp.com/help/features/universal-actions/
- https://learn.microsoft.com/en-us/windows/powertoys/command-palette/overview
- https://userbase.kde.org/Plasma/Krunner

**Device notes (d)**
- `docs/research/device-findings.md` (this repo)
- `Welcome's device notes` (Glowbar, system shortcuts, Files, Gemini)
- `kuscher/vscodebook` `docs/GOOGLEBOOK.md` (Linux VM, port forwarding, launch bounds, wireless debugging)

## 9. Not verified

Nothing here was run on a device. Before building, check on the HP:

- Creating files and folders by path in `Documents/` and `Download/` with no permission (read in
  source only), and whether an editor can write through the URI Booklight hands it.
- Gmail keeping a closed compose window as a draft, and which of the URI and the extras it reads.
- Whether Keep is installed and how it treats `ACTION_SEND` and
  `com.google.android.gms.actions.CREATE_NOTE`; which app holds the notes role.
- Handlers for `ACTION_CREATE_REMINDER`, `ACTION_TRANSLATE` and `ACTION_DEFINE`.
- `docs.google.com/document/create?title=`; Spotify and YouTube Music search links.
- `Settings.Panel` sheets on the desktop; the `android.settings.DARK_THEME_SETTINGS` action.
- Keyboard backlight through `InputDevice.getLightsManager()`.
- The secure and global keys `night_display_activated` and `low_power` (from memory; `ui_night_mode`
  was read in source, `adb_wifi_enabled` is in the VSCodeBook notes).
- New-window launches per app (`MULTIPLE_TASK`), and whether launch bounds hold for every app.
- Whether Gemini submits text shared to it or only fills it in.
- ML Kit GenAI and Gemini Nano on a Googlebook: the published device list names phones only.
- Launcher aliases as separate entries in Keyboard shortcuts → Add shortcut.
- Tasker's `ACTION_TASK` string and the protection level of `PERMISSION_RUN_TASKS`. MacroDroid and
  Automate were not researched; Kvaesitso's docs page returned 404 and Sesame has no public docs.
- The Apps Script relay (designed, not built). The Tasks API scope's classification.
- Where Calendar stores a Meet link; the path of the desktop's "Home screen" folder.
- Play: I found no form for notification listeners, device admin, `WRITE_SETTINGS` or notification
  policy access on the permissions page. That is absence of evidence, not a ruling. Play's view of
  apps that use Shizuku is unverified beyond the fact that such apps are listed.
- The Glowbar developer API: one outlet's account of a briefing, no Google document.
