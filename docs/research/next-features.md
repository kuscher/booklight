# Booklight: what to build next

*Desk research, 1 October 2026, after 1.1 went to testers. Read for it: this repo's plan, design and research
files and `core/Model.kt`; Alex's other repos (BentoBar, Summa, StudioSnap, PDF Toolbox, Canvas, HearOn Link,
VSCodeBook, Welcome, Windowcast, Missing Link Labs, googlebook.studio); AOSP `android17-release`;
developer.android.com; Play Console Help; the manuals of the launchers named below. No device was touched and
no code was changed.*

**How sure each claim is.** *device* = written down in one of Alex's repos as checked on a Googlebook.
*docs* = read in AOSP source, the developer docs or the product's own manual for this report. *memory* = not
re-read. *check* = needs an experiment on a device before anyone builds on it (§5). Where a device note and the
source disagree, the device note wins.

**What this builds on.** [use-cases.md](use-cases.md) listed 63 ideas by permission tier. Most of its tier 0
shipped in 1.1. What did not ship is carried into the catalogue below, marked *(carried)*, without repeating
its analysis:

| From use-cases.md | State after 1.1 |
| --- | --- |
| Mail, note (file and Keep), event, reminder, timer, alarm, new file, folder and doc, Create in…, Ask Gemini | Shipped |
| Volume, mute, media keys, play from search, brightness, panels, about 35 settings pages | Shipped |
| Colour, QR, password, UUID, GitHub jump, `:3000`, Drive search | Shipped |
| Links with placeholders and `intent:` URIs, recipes (with places), snippets, clipboard transforms, text from other apps | Shipped |
| Open left half, right half, new window; widget; tile; assistant role | Shipped |
| Do Not Disturb, keep awake | Decided: Modes page; BentoBar |
| Apps' static shortcuts, templates, files in granted folders, own files in results, Downloads row | Not shipped |
| Keyboard backlight, translate and define, `lorem`, text tools (`b64`, `jwt`, `sha256`, `epoch`), `ports` scan | Not shipped |
| Your people, contact picker, today's meetings, webhooks, Tasker tasks, Linux commands, dynamic shortcuts and launcher aliases | Not shipped |
| Relay or Gmail send, Gemini Nano, everything in tier 3 (accessibility, listener, adb) | Not shipped; the relay is deferred by decision |

**Tiers in this file** (the brief's scale, one step finer than PLAN.md §6):
0 nothing · 1 an install-time permission, no prompt · 2 one prompt, picker or system consent dialog ·
3 a switch in Settings or a role · 4 accessibility, notification listener, usage access, all-files access,
device admin, or an adb or Shizuku grant.

---

## 1. Summary

### The ideas most worth doing next, in order

| # | Idea | Why |
| --- | --- | --- |
| 1 | **Extensions, static layer first, and apps' own shortcuts** (L1, §4) | One XML file in another app, or nothing at all, puts its commands in the list. No process starts while typing. It is the 2.0 already planned, cut to its cheapest useful part. |
| 2 | **Android's intents as actions on a thing** (F3, F4, F5) | A file, link or text gets "Send to PDF Toolbox", "Edit in Canvas", "Translate", "Calculate" from what those apps already declare. This is LaunchBar's Send To and Alfred's Universal Actions, and no Mac launcher gets it for free. |
| 3 | **Pick a colour from the screen** (C1) | One intent, new in Android 17, no permission; it lands in the colour row that exists. The second most installed Raycast extension. |
| 4 | **A window that stays: pinned note, timer or answer** (C2) | Android 17's pinned layer needs only a normal permission and Summa already uses it on the HP. Booklight today leaves nothing on screen. |
| 5 | **The shortcut cheat sheet** (L15) | `snap`, `desk`, `screenshot` answer with the system's own keys. Booklight cannot press them, but it can teach them, and the table exists in the Welcome repo. |
| 6 | **More places and the other display** (L3, L4) | Thirds, quarters, centre, maximise, "on display 2": rectangle arithmetic on what 1.1 built. Only a desktop Android has a use for it. |
| 7 | **`?`: everything you can type** (L20) | 1.1 added about thirty keywords and an empty panel shows none of them. |
| 8 | **Notes that grow** (F1) | Search Notes.md, `todo`, append to a named file, a daily note. The folder is already granted. |
| 9 | **Prompts as commands** (I1) | "Fix grammar", "shorter", "in German" as rows that hand the text to Gemini, filled in. The Ask Gemini path, with templates. |
| 10 | **A key per command** (L8) | Launcher aliases the system can bind: Action + E for emoji, Action + N for a note. Needs one device check. |
| 11 | **Packs: commands as files** (A1) | Links, snippets, recipes and prompts as files in the notes folder: editable in VSCodeBook, shareable, and the seed of a gallery on googlebook.studio. |
| 12 | **Summa's answers in the panel** (X1) | Units, money, dates, time zones. The first live extension, and the reason PLAN.md gave for having extensions. |
| 13 | **Text tools** (D1) | `b64`, `jwt`, `sha256`, `epoch`, `0xff`. Pure Kotlin in `core/`, for people who build on the device. |
| 14 | **The shelf** (F8) | Everything that passed through Booklight, kept for a while. An honest clipboard history that reads nothing in the background. |
| 15 | **BentoBar answers for Booklight** (X2, X3) | Timers in the status bar, keep awake, Join for the next meeting, lock and screenshot: BentoBar already holds the permissions and the code. Booklight asks; it holds nothing new. |

### The five most tempting ones behind scary permissions

| Idea | What it needs | Why it tempts |
| --- | --- | --- |
| **Paste in place** (F10) | Accessibility service with the input-method flag | Emoji, snippets, sums and rewritten text land in the field you came from. Every Mac launcher does this; Booklight copies. |
| **A list of open windows** (L13) | Accessibility (`getWindows`) | Switching windows is the largest single command in the one Raycast export there is. |
| **Any key, plain Alt + Space included** | Accessibility key filter | The decision in PLAN.md §11.2 that had to wait. |
| **Notifications: reply, search, now playing** (P7, P8) | Notification listener | "reply anna on my way" without leaving the keyboard; the media row gets a title and a scrub bar. |
| **Snap, close, desks and displays for the focused window** (L14) | Shell identity (Shizuku or adb) | The only way to press the system's own window keys. Shizuku's official build is broken on Android 17. |

Clipboard history is not on this list on purpose: an accessibility service gets **no** clipboard access
(F9), so the 3.0 plan's "clipboard history through the accessibility service" does not hold as written.

---

## 2. The catalogue

Columns: **Idea** what is typed and what comes back, in Booklight's model · **From** the product it comes from
· **Value** high, medium or low, with the reason · **Effort** S up to a day, M up to a week, L more, with the
hard part · **Tier** and the exact permission or API · **Play** none, form, declaration + video, or likely
rejection · **GB** *only* makes sense on a desktop Android, *better* there, or *same* anywhere · **Sure**.

Ideas that need something the interaction model does not have yet say so in the Effort cell.

### 2.1 Launching and windows

| ID | Idea | From | Value | Effort | Tier · permission or API | Play | GB | Sure |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L1 | **Apps' own shortcuts** *(carried)*. `summa mini`, `chrome incognito`: each app's manifest shortcuts as rows, and as actions on its row | Sesame, Pixel Launcher, Niagara | High: reaches inside every app with no work on their side | M: parse `android.app.shortcuts` per package; start only exported targets | 0 · visibility already held | none | same | docs; check per app |
| L2 | **Search inside an app** from its declared capabilities. `<capability>` and `url-template` in the same XML become a scope: `keep milk` | App Actions, Spotlight's Tab into an app | Medium: only apps that declare them | M: read the XML by hand (the framework parser skips capabilities) | 0 | none | same | docs; check how many apps declare |
| L3 | **More places.** `chrome right third`, `code left two thirds`, `centre`, `maximise`, quarters | Raycast window management, Hammerspoon | High on a desktop | S: `Place` grows, rectangles; a running window keeps its bounds | 0 · `ActivityOptions.setLaunchBounds` | none | only | device for halves; check (Summa's notes say bounds were ignored for its own windows) |
| L4 | **On the other display.** `chrome on display 2` | Raycast "next display" | Medium: for people with a monitor | S | 0 · `setLaunchDisplayId` | none | better | docs; check |
| L5 | **Layouts.** A recipe drawn on a picture of the screen: up to eight apps, each in a place, each with a link or file | Raycast Create Layout, `hs.layout` | Medium to high | M: the editor | 0 | none | only | device (recipes with places shipped) |
| L6 | **The comma.** `chrome, gmail, calendar` Enter opens all three; one action on several rows | Quicksilver comma trick, LaunchBar staging, Alfred file buffer | Medium | M: the field holds one chip today; staged rows need a counted chip | 0 | none | same | docs |
| L7 | **Aliases you set.** "Set alias…" on any row; an exact alias always wins | LaunchBar, Raycast aliases, Spotlight quick keys | Medium: learning already does most of it | S | 0 | none | same | docs |
| L8 | **A key per command.** Opt-in launcher aliases ("Booklight Emoji", "Booklight Note") that Keyboard shortcuts can bind to their own Action key | Raycast hotkeys, Quicksilver triggers | High | S to M: `activity-alias` switched on by the user; each is one more icon in the Apps list | 0 | none | only | check: does each alias get its own shortcut? |
| L9 | **Fallback rows you choose.** The rows that close every list (Google, Gemini) become a short ordered list: add "Note this", "Search Drive" | Alfred fallback searches, Raycast fallback commands | Medium | S | 0 | none | same | docs |
| L10 | **What you ran.** `!!` or repeated Up lists past texts; search them | PowerToys `!!`, Alfred Up | Low to medium | S | 0 | none | same | docs |
| L11 | **Archive** as an app action: removes the app, keeps its data | Android 15 archiving | Low to medium | S | 1 · `PackageInstaller.requestArchive` with `REQUEST_DELETE_PACKAGES` (held) | none | same | docs; check the dialog and which installers support it |
| L12 | **More app actions:** Notifications, Open by default, App language, Battery | Pixel Launcher long-press | Low to medium | S | 0 · Settings actions with `package:` | none | same | device (resolved on the HP) |
| L13 | **Open windows** *(carried)*. `win inbox` lists windows by title; Enter raises one | Raycast Switch Windows, PowerToys Window Walker, rofi | High | M to L: minimised windows have no window to list | 4 · accessibility `getWindows()`; an action on a node raises its window | declaration + video | better | docs; check |
| L14 | **Window commands.** `snap left`, `maximise`, `close window`, `next desk`, `to other display` on the focused window | Raycast, Rectangle | Medium to high | L: only shell may inject the system's chords or call the desk commands | 4 · Shizuku or adb (`input keycombination`, `wm shell`) | none found | only | docs; Shizuku on 17 unverified |
| L15 | **The shortcut cheat sheet.** `snap`, `desk`, `screenshot`, `lock` show the system's key combination as key caps; Enter opens Keyboard shortcuts | ChromeOS launcher's shortcut search | Medium to high: teaches what Booklight cannot do | S: a table of 46 shortcuts, in English and German | 0 | none | only | device (Welcome's table, HP) |
| L16 | **Booklight as a picker for other apps.** An intent hands over a list; the panel shows it; the pick goes back as the result | dmenu, rofi, `hs.chooser` | Medium: glue for his own apps and scripts | M | 0 · an exported activity for result | none | same | docs |
| L17 | **Links into Booklight.** `booklight://run/morning`; a recipe or scope as an icon on the desktop or taskbar; top recipes on the icon's right-click menu *(carried in part)* | Raycast deeplinks, Alfred external triggers | Medium | S: confirm before running a link from outside | 0 · `requestPinShortcut`, dynamic shortcuts | none | better | docs |
| L18 | **Booklight as the notes app.** Action + Ctrl + N and a hot corner open the note scope; a screenshot can go into the note with a link back to the app | Android's notes role; Raycast Notes | Medium to high: a second system key with no permission | M | 3 · `ROLE_NOTES` (not requestable; chosen in Default apps; replaces Keep there) | none found | only | docs + device (the key and the corner exist); check the role is offered |
| L19 | **Power menu** when Booklight is the assistant | Alfred system commands | Low to medium | S | 3 · `SHOW_POWER_MENU` comes with the assistant role (flag-gated) | none found | same | docs; check the flag |
| L20 | **`?`** lists every keyword with an example; searchable; Enter enters the scope | Raycast root search, VS Code `?` | High: 1.1's abilities are invisible in an empty panel | S | 0 | none | same | — |
| L21 | **Booklight as the browser chooser.** Every clicked link opens the panel with the link as the thing: which browser or app, copy, QR, note | Velja, Choosy | Medium; one more step for every link | M | 3 · `ROLE_BROWSER` (requestable) | none found | same | docs. Better as a separate app |

### 2.2 Files and content

| ID | Idea | From | Value | Effort | Tier · permission or API | Play | GB | Sure |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F1 | **Notes that grow.** `notes milk` searches Notes.md; `todo call bank` adds a checkbox line; `append ideas.md / text`; a note per day | Quicksilver Append To, Sol scratchpad | High: finishes a shipped feature | S to M | 0 beyond the folder already granted | none | same | device |
| F2 | **Files in folders you grant** *(carried)*. Names, newest first | Alfred `open`, Everything | Medium (people navigate; they rarely search) | L: no provider search on a tree grant, so walk and index; `Download` cannot be granted | 2 · `ACTION_OPEN_DOCUMENT_TREE` | none | better | docs |
| F3 | **Actions on a file from what apps declare.** A file row gets Open with (every `VIEW` handler for its type), Send to (every `SEND` handler), Copy path, Show in Files | LaunchBar Send To, Alfred Universal Actions | High: PDF Toolbox, Canvas and VSCodeBook appear with no work there | M | 0 · `<queries>` entries for `SEND` and `VIEW` with types | none | better | docs |
| F4 | **Take files and links, not only text.** Share anything to Booklight; it becomes the chip; the rows are what to do with it | LaunchBar Instant Send | Medium to high | M | 0 · intent filters | none | same | docs |
| F5 | **Text actions from other apps.** On any text chip: every app's `PROCESS_TEXT` action ("Translate", "Calculate" from Summa, "Look up"); what it returns replaces the chip | Alfred Universal Actions | High for the effort | S to M | 0 · `<queries>` for `PROCESS_TEXT` | none | same | docs; check which ones return text |
| F6 | **What the text is decides the rows.** A date offers Event, an address Maps, a tracking number its carrier, an IBAN a tidy copy | Kvaesitso quick actions, Android smart selection | Medium to high | M: the platform `TextClassifier` first, own rules behind it | 0 | none | same | docs for the types; check it answers third-party apps |
| F7 | **Use as text.** Tab on an answer, emoji or link makes it the chip; then Mail, Note, QR | Quicksilver object then verb | Medium | S: `EnterScope` with the text scope | 0 | none | same | — |
| F8 | **The shelf.** What Booklight copied, was handed or transformed stays for a week as `shelf` rows; pin; "Keep in Booklight" in the text-selection menu | Quicksilver Shelf, Spotlight clipboard | Medium to high | S to M | 0 | none | same | — |
| F9 | **Clipboard history** *(carried, corrected)* | Alfred, Raycast, LaunchBar ClipMerge | High in every study, but see Sure | L | 4 · not accessibility: the clipboard service lets only the focused app, the default keyboard and shell read. Routes: a focus trick from a service, or Shizuku | likely rejection for the focus trick ("work around privacy controls") | same | docs. Press says the Quick Insert key already shows copied snippets: check before building |
| F10 | **Paste in place** *(carried)*. Enter puts the emoji, snippet or answer into the field you came from | Raycast, Alfred, Quicksilver | High | M | 4 · accessibility with `FLAG_INPUT_METHOD_EDITOR`, `commitText` | declaration + video | same | docs |
| F11 | **Snippets that expand as you type** (`;sig` anywhere) | Alfred, Raycast | Medium | L: it must watch every key | 4 · accessibility key filter, or being the keyboard | declaration + video; looks like a key logger | same | docs. Not recommended |
| F12 | **Bookmarks, imported.** Pick Chrome's exported bookmarks file once; its links become rows | Raycast's Chrome extension | Medium | S to M | 2 · file picker per import | none | same | docs: no live bookmark, history or tab API exists |
| F13 | **Open tabs** through a companion Chrome extension | ChromeOS launcher, Spotlight | High if it can be done | L: no path for Booklight to pull from Chrome without a server | 0 on the Android side | none | better | unverified: desktop Chrome with extensions on Googlebooks is press-reported |
| F14 | **Look before opening.** Space on a file, picture or link row shows it | Quick Look, Alfred Shift | Medium | M: a new tall body, or the system's quick viewer | 0 · `ACTION_QUICK_VIEW` | none | same | check: who answers it on a Googlebook |
| F15 | **A widget in the panel.** `widget calendar` draws another app's widget as a tall row | Niagara pop-ups, Raycast menu bar | Medium; glanceable | L: a new body kind, host lifecycle, keyboard focus inside a see-through window | 2 · system consent per widget (`ACTION_APPWIDGET_BIND`) | none found | same | docs; check drawing in the panel's window |
| F16 | **Files from a template** *(carried)*. `new meeting-notes` | Alfred, Raycast | Medium | S to M | 0 | none | same | — |
| F17 | **What apps publish to the assistant.** With the assistant role, Booklight may read assistant-visible AppSearch data | Pixel Launcher search | Unknown | M | 3 · `READ_ASSISTANT_APP_SEARCH_DATA` comes with the role | none found | same | docs; nobody knows which apps publish: check first |

### 2.3 People and communication

| ID | Idea | From | Value | Effort | Tier · permission or API | Play | GB | Sure |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P1 | **Your people** *(carried)*. A short list kept in Booklight: `anna` gives Mail, Meet, WhatsApp, Copy address | Alfred contacts, Kvaesitso | High | M | 0 | none | same | — |
| P2 | **Choose a contact** *(carried)* in Mail's To slot | Android 17 contact picker | Medium | M | 2 · `ACTION_PICK_CONTACTS`, no permission | none. From 27 January 2027 `READ_CONTACTS` needs a declaration and launchers are not a listed use | same | docs |
| P3 | **Next meeting and Join** *(carried)*. `join`, `next` | Raycast My Schedule, Sol | High | M, or S if BentoBar answers (X2) | 2 · `READ_CALENDAR`; no conference column, the link is in the description or place | data safety | same | docs; BentoBar already finds Join links |
| P4 | **Meeting rows with no permission.** `meet` starts one, `meet abc-defg-hij` joins, `cal fri` opens the calendar on Friday | Raycast, Sol | Medium | S | 0 · links | none | same | memory; check the calendar date link |
| P5 | **Message links.** `wa anna running late` through `wa.me`; Telegram and Messages for web | Kvaesitso | Low to medium on a laptop | S after P1 | 0 | none | same | memory |
| P6 | **Share… on every row** with text, a link or a file: the system sheet, with its people and Quick Share | Android share sheet | Medium: the only road to Direct Share and Quick Share | S | 0 · `Intent.createChooser` | none | same | docs: share targets cannot be listed by an app |
| P7 | **Notifications as rows.** Search what is waiting; open, dismiss, reply: `reply anna on my way` | Niagara, Samsung Finder | High | M to L: a listener is a bound service, against "nothing in the background" | 4 · notification listener | none found; guarded for sideloaded builds | same | docs. Advanced Protection does not block listeners |
| P8 | **Now playing** *(carried)*: title, scrub, the app's own controls | Alfred mini player, Raycast | Medium | M | 4 · listener, for `getActiveSessions` | none found | same | docs |

Sending mail without the compose window (the relay or the Gmail API) stays deferred, as decided.

### 2.4 Capture and creation

| ID | Idea | From | Value | Effort | Tier · permission or API | Play | GB | Sure |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| C1 | **Pick a colour from the screen.** `pick colour`, or "Pick…" on the colour row: the system's eye dropper, then HEX, RGB, HSL, OKLCH | Raycast Color Picker, PowerToys | High | S | 0 · `Intent.ACTION_OPEN_EYE_DROPPER` (API 37), result `EXTRA_COLOR` | none | better (any Android 17) | docs + device (resolves on the HP); flag-gated |
| C2 | **Pin it.** An action on a note, timer, answer, QR code or colour opens a small window that stays on top | Raycast Notes, Summa's mini calculator | High | M: a second, opaque activity; one pinned window at a time; at least 220 dp; only from a focused desktop window | 1 · `USE_PINNED_WINDOWING_LAYER` (normal), `AppTask.requestWindowingLayer` | none | only | device (Summa on the HP) + docs; flag-gated |
| C3 | **Large type.** `large 0171 555 1234`, or an action on any text: across the screen, gone on a key | Quicksilver, Alfred | Low to medium | S: a second see-through window without blur | 0 | none | same | — |
| C4 | **Scan a code** with the webcam; the result becomes the chip | Raycast, phones | Low on a laptop | S to M | 0 · the Play services code scanner needs no camera permission in the caller | none | same | memory; check on a Googlebook |

Screenshots, recordings and text from the screen come through StudioSnap (X4): a plain app cannot capture.

### 2.5 System and device

| ID | Idea | From | Value | Effort | Tier · permission or API | Play | GB | Sure |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| S1 | **Settings rows that land on the switch.** `screen timeout`, `modifier keys`, `text size`, `system update`, `play protect`, `glowbar`, `magic pointer`: the page opens scrolled to the item | Spotlight, Command Palette | Medium | S | 0 · the `:settings:fragment_args_key` extra | none | better | device (resolved on the HP; two keys launched) |
| S2 | **Answers about the device.** `battery`, `storage`, `ram`, `ip`, `wifi` as answer rows | PowerToys Run, Raycast | Medium | S | 0, or 1 · `ACCESS_NETWORK_STATE` for the address | none | same | docs (BentoBar reads the same values) |
| S3 | **Keyboard backlight** *(carried)*. `backlight 50` as a level | — | Low to medium | S to M | 0 · the input device's `LightsManager` | none | only | check |
| S4 | **Brightness with no grant.** The hollow brightness row opens the system's own slider | — | Low to medium | S | 0 · SystemUI's exported brightness dialog | none | same | device (resolves); check it shows |
| S5 | **Sound output.** `output` opens the system's output switcher | Raycast audio extensions | Medium | S | 0 · `MediaRouter2.showSystemOutputSwitcher()` | none | same | docs; check that it shows for an app that plays nothing |
| S6 | **Lock, screenshot, overview, notifications, quick settings, power** *(carried)* | Alfred system commands | High | S once a service exists | 4 · accessibility `performGlobalAction`. BentoBar's Tools menu already does all of them (X3) | declaration + video | same | docs + device |
| S7 | **Real switches** *(carried)*: dark theme, night light, battery saver, wireless debugging | Raycast, Alfred | Medium | M | 4 · `WRITE_SECURE_SETTINGS` by adb. By source `ui_night_mode` is not applied live | none | same | docs; check each key |
| S8 | **Gemini and Magic Pointer rows.** `magic pointer`, `gemini voice` | — | Low to medium | S | 0 · exported actions | none | only | device (resolve); starting Magic Pointer from an app is untested |
| S9 | **Ranking from the whole system**, and a mark on apps that are open | KISS history, Pixel Launcher | Low: Booklight's own learning covers it | M | 4 · usage access | none found; guarded for sideloaded builds | same | docs |

### 2.6 Automation and recipes

| ID | Idea | From | Value | Effort | Tier · permission or API | Play | GB | Sure |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A1 | **Packs.** A folder `commands/` in the notes folder: one file per set of links, snippets, recipes and prompts. Export yours; add one from a link after seeing what it adds | Alfred Gallery, Raycast script commands | High: sharing, versioning, editing in VSCodeBook | M: the format, and a clear import screen | 0 beyond the folder already granted | none: data, not code | same | docs |
| A2 | **Recipes that take text.** `standup shipped 1.1` passes the text into the steps | Alfred workflows, Raycast arguments | Medium | M | 0 | none | same | — |
| A3 | **Save what I just did.** After two or three runs in a row, `save recipe` offers them as steps | — (the UX model's own open question) | Medium | M | 0 | none | same | — |
| A4 | **Webhooks** *(carried)*. `deploy`, `lights off`: Home Assistant, GitHub workflows, ntfy; second Enter | Raycast, Alfred | Medium | M: tokens in the Keystore | 1 · `INTERNET` (held) | data safety says what is sent | same | docs (Home Assistant's REST API) |
| A5 | **Tasker tasks** *(carried)*. `tasker` lists the user's tasks; Enter runs one. MacroDroid and Automate by their intents | Sesame | Medium for that crowd: Booklight borrows every permission they hold | S to M | 1 · Tasker's `PERMISSION_RUN_TASKS` plus its "Allow External Access" | none | same | docs for Tasker; MacroDroid and Automate from forums |
| A6 | **Linux script commands** *(carried, refined)*. Scripts in the VM with header comments (title, argument, how to show the output) become rows; output shows in the row | Raycast Script Commands | High for developers | L: a helper in the VM on a forwarded loopback port, every request signed. VSCodeBook's agent is a candidate | 1 · `INTERNET` | data safety | only | device for the forwarding (the Acer asks per port, the HP did not) |
| A7 | **Rows from a URL.** A link can be live: Booklight asks it for rows as you type and gets the extension row document back (§4) | Alfred Script Filter | Medium | M after the contract | 1 · `INTERNET` | data safety | same | — |
| A8 | **Termux commands** | — | Low here | M | 2 · `com.termux.permission.RUN_COMMAND`; only the GitHub build has it, and Play Protect reportedly blocks that build on 17 | none | same | docs. Skip: the VM is there |
| A9 | **Locale plug-ins as steps.** Set one up once in its own screen; a recipe fires it | Tasker, Locale | Medium | M | 0 | none | same | docs. As a bridge app (§4), not in the core |

Triggers (time, Wi-Fi, a corner) stay out, as use-cases.md decided: only things the user presses start a command.

### 2.7 AI

| ID | Idea | From | Value | Effort | Tier · permission or API | Play | GB | Sure |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| I1 | **Prompts as commands.** Rows such as "Fix grammar", "Shorter", "In German", your own with `{argument}` and `{clipboard}`: Gemini opens with the prompt filled in | Raycast AI Commands | High for S | S | 0 · today's hand-over to the Gemini app | none | same | device |
| I2 | **Rewrite, proofread, summarise on the device,** in the row. For text handed over from an editable field, "Replace" writes it back | Raycast "Replace Selection" | High | M to L | 0 · ML Kit GenAI on AICore; foreground only, quotas | none | same | docs: no Googlebook on the device list; the HP reports an AICore feature. Check |
| I3 | **An answer in the panel** with the user's own API key | Raycast Quick AI | High | M | 1 · `INTERNET` | data safety and a disclosure: the text leaves the device | same | — |
| I4 | **Say it plainly.** "remind me tomorrow at nine to call the bank" finds the right scope | Spotlight, Siri | Medium | L: a small model with a fixed schema, rules behind it | 0 | none | same | unverified |
| I5 | **Translate and define** *(carried)*. `tr de good morning` | ChromeOS Quick Answers | Medium | S through F5, M on the device (about 30 MB a language) | 0 | none | same | docs |
| I6 | **Let Gemini call Booklight.** Booklight publishes its own App Functions (note, timer, run a recipe) | App Functions | Speculative | M | 0 to publish | none found | same | docs: alpha library, private preview. Calling other apps' functions is closed (allowlist) |

### 2.8 Developer

| ID | Idea | From | Value | Effort | Tier · permission or API | Play | GB | Sure |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| D1 | **Text tools** *(carried)*. `b64`, `jwt`, `sha256`, `epoch`, `json`, `lorem`, `0xff`, `255 in hex`, `U+2192`; also as actions on a text chip | PowerToys value generator | Medium to high for this audience | S | 0 | none | same | — |
| D2 | **GitHub in the panel.** `gh prs`, `gh issues`, `gh inbox` as rows | Raycast's GitHub extension | Medium to high for Alex | M | 1 · `INTERNET`, a token in the Keystore | data safety | same | — |
| D3 | **What is listening.** `ports` probes loopback; with A6, the real list from the VM | — | Medium | S | 1 · `INTERNET` | none | better | device |
| D4 | **Facts about an app.** `pkg summa`: package, version, target SDK, installer, signing digest; Copy | — | Low to medium | S | 0 (apps with an icon only) | none | same | docs |
| D5 | **Developer rows.** `wireless debugging` opens Developer options at the switch; `adb pair`; `running services` | — | Medium | S | 0 | none | better | device (VSCodeBook uses the same extra) |

### 2.9 What extensions make possible

Each row is another app's contribution through §4. The effort is the other app's.

| ID | Idea | From | Value | Effort | Tier · permission or API | Play | GB | Sure |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| X1 | **Summa answers.** `5 ft in cm`, `120 usd in eur`, `3pm berlin in sf`, your own variables; Copy, Open in Summa | Numi, Soulver, Spotlight | High | M: a provider around its pure-Kotlin engine. Or link the engine into Booklight | 0 | none | same | docs |
| X2 | **BentoBar.** `timer 10m` in the status bar, stopwatch, Pomodoro; `awake 2h` as a switch; `join`; CPU, memory, battery as answers | Raycast | High | M | 0 for Booklight | BentoBar's own listing | only | docs |
| X3 | **BentoBar as the power pack.** `lock`, `screenshot`, `overview`, `notifications`, `power`; later the window list | — | High | M, plus new wording in BentoBar's accessibility declaration | 0 for Booklight; BentoBar holds the service | BentoBar's declaration | only | docs + device |
| X4 | **StudioSnap.** `shot area`, `shot window`, `shot scroll`, `record`, `text from screen` | CleanShot, Raycast | High | S: its capture activity already takes the mode as an extra | 0 for Booklight | none | only | docs |
| X5 | **PDF Toolbox.** Each of its hundred tools as a row (`merge pdf`, `compress pdf`); on a PDF, as Send to | — | Medium to high | S to M: a way to open a tool by name | 0 | none | better | docs |
| X6 | **VSCodeBook.** `code booklight` opens a recent folder; New window; Start Linux; Wireless debugging on | Raycast VS Code, PowerToys workspaces | High for developers | M: the recent folders live in the VM | 0 for Booklight | — (private repo) | only | docs |
| X7 | **HearOn Link.** `airpods` answers with the battery; noise control as a row of options | — | Medium | M: needs a "choice" body in the contract | 0 for Booklight | none | better | docs |
| X8 | **Welcome.** "how do I snap a window" opens the lesson | Tips apps | Medium | S to M | 0 | none | only | docs |
| X9 | **The rest.** Canvas (`new image 1920x1080`), Windowcast (`cast` and a window), Disco Sweeper (`sweeper expert`), govee (`strip red`), Script and Summa's shortcuts (free through L1) | — | Low to medium each | S each | 0 | none | better | docs |
| X10 | **The phone.** With the Link SDK: ring it, send the clipboard or a link to it, a button on the phone that runs a Booklight recipe | Alfred Remote, phone hubs | Medium | L: the SDK is at milestone 1 | 0 for Booklight | — | only | docs |

### 2.10 Delight

| ID | Idea | From | Value | Effort | Tier · permission or API | Play | GB | Sure |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| J1 | **Confetti.** `confetti`; also a recipe step | Raycast | Low, but people show it to each other | S to M: a second see-through window with no blur (the panel's window is exactly the panel) | 0 | none | same | — |
| J2 | **Your year in Booklight.** Opens a day, top rows, keys saved; copy as a picture | Raycast Wrapped, LaunchBar statistics | Low to medium | S: the history file has it | 0 | none | same | — |
| J3 | **Chance.** `flip`, `roll 2d6`, `pick anna, ben, cy` | — | Low | S | 0 | none | same | — |
| J4 | **Glowbar.** `disco` opens Glowbar Disco; when Google opens the bar to apps, light it as the panel opens | — | Low today | S | 0 · `glowbar://disco` | none | only | device (resolves). Driving the bar is signature-only |
| J5 | **Weather.** `weather berlin` for a typed city, off until turned on like suggestions | Spotlight, Raycast | Medium | S to M | 1 · `INTERNET` | data safety | same | — |
| J6 | **A tick in the touchpad** when an action runs | — | Low | S | 0 | none | only | check: haptics on the clickpad |

### 2.11 Looked at and not possible for a Play app

So nobody researches them again. All read in `android17-release` unless marked.

- **Other apps' Quick Settings tiles:** binding and clicking are system-only.
- **Device Controls (smart home tiles):** only System UI may bind them. Google Home's own APIs need OAuth and certification.
- **Listing share targets, Direct Share people or Quick Share:** system-only. The share sheet is the only road (P6).
- **Dynamic and pinned shortcuts of other apps:** need the launcher role, or a real voice-interaction service.
- **Running other apps' App Functions:** the permission is normal behind a flag, but the caller must be on an allowlist pinned by certificate. On the HP it is `internal|privileged|knownSigner` (device).
- **Chrome's bookmarks, history and tabs; an incognito tab from outside:** no API.
- **Moving or resizing another app's window; desks; bubbling an app:** no public API. Moving Booklight's own window (`AppTask.moveTaskTo`) is reserved for the default browser.
- **Pausing the work profile; private space:** need the home role.
- **Force-stopping an app:** `killBackgroundProcesses` only kills the caller since Android 14.
- **Reading the clipboard in the background:** only the default keyboard and shell.
- **The system Settings search index, Slices, Quick Search Box suggestion providers:** system-only, deprecated, or guarded by `GLOBAL_SEARCH`.
- **The phone's recent apps (Continue On), phone notifications:** privileged.
- **A screenshot:** not without accessibility, shell or the notes role's capture flow.

---

## 3. The same ideas, ranked four ways

### 3.1 Quick wins: high value, S or M, tier 0 or 1

1. L1 and the static extension layer (§4): commands from other apps, no process started.
2. F3, F4, F5: the thing-then-action model, fed by intent filters that already exist.
3. C1: colour from the screen.
4. L15: the shortcut cheat sheet.
5. L20: `?`.
6. L3, L4: more places, the other display.
7. F1: notes search, `todo`, append.
8. I1: prompts as commands.
9. C2: the pinned window (tier 1: one normal permission, Alex's call).
10. D1: text tools.
11. A1: packs.
12. F8, F7: the shelf; use as text.
13. L8: a key per command, once checked.
14. X1, X4: Summa answers; StudioSnap's capture modes.
15. P1, P4: your people; meeting rows.
16. S1, S2, D5: deeper settings rows, device answers, developer rows.
17. L17, L7, L9: links into Booklight, aliases, chosen fallbacks.
18. J2, J1: the year in Booklight; confetti.

### 3.2 Worth a grant: tier 2 or 3

| Idea | What the user gives | Verdict |
| --- | --- | --- |
| P3 Join the next meeting | The calendar prompt, or nothing if BentoBar answers | Do it through BentoBar first; the prompt later if people ask without BentoBar |
| L18 Notes role | Choosing Booklight under Default apps › Notes | Worth it if the role is offered on Googlebooks: a second system key and a hot corner. It displaces Keep there, so it must stay a quiet option, like the assistant role |
| P2 Contact picker | One picker per use | Cheap and future-proof against the 2027 contacts rule |
| F2 Files in granted folders | One picker per folder | Large effort for the least-used launcher feature; after F3 |
| F12 Bookmarks import | A file picker per import | Small, honest, and the only way to bookmarks |
| F15 A widget in the panel | The system's consent per widget | Interesting and uniquely Android; needs a spike before a promise |
| L19 Power menu, F17 assistant data | The assistant role, already offered | Take them when checked: they cost nothing more than declaring the permissions, and only work for people who chose the role |
| L21 Browser chooser | The browser role | A different product; keep it out of Booklight |

### 3.3 The power pack: tier 4

| Idea | What exactly scares | Separate app? |
| --- | --- | --- |
| F10 Paste in place; L13 window list; any-key summon; S6 lock and screenshot | **Review:** Play names launchers as not accessibility tools: declaration form, a prominent disclosure with consent, a video, and a new submission whenever the use changes. **Trust:** "can view your screen and perform actions"; with Safety Center on, a daily "Review app with full device access" notification. **Advanced Protection (Android 17):** non-tool services are switched off, per package. **Breakage:** a crash unbinds the service until the user re-enables it. | Yes. A refused declaration must never block a Booklight update. The cheapest home is **BentoBar**, which already has the service, the disclosure and the declaration; the alternative is a small companion with one job. Either way it plugs in through §4 |
| P7, P8 Notifications and now playing | **Trust:** it reads every notification. A listener stays bound while enabled, which ends "nothing in the background" unless unbind and rebind on open works (unverified). **Review:** no form found; guarded as a restricted setting for sideloaded builds. Advanced Protection leaves it alone. One-time codes are redacted unless the app is the assistant | Yes, same reasoning; lighter than accessibility, so it could ship first |
| F9 Clipboard history | **It does not work the way the plan assumed.** No accessibility exemption exists. The focus trick is the kind of thing Play's "work around privacy controls" clause is for. The system may already offer it on the Quick Insert key | Do not build beyond the shelf (F8) until checked on a device |
| L14 Window and desk commands; S7 real switches | **Breakage:** Shizuku's official build is broken on Android 17 and hidden on Play for it; forks trip the local-network picker. An adb grant needs a computer or the VM. **Trust:** shell can do nearly anything | Yes: an "expert" companion for people who already run adb. VSCodeBook already holds `WRITE_SECURE_SETTINGS` and could answer for wireless debugging |
| S9 System-wide ranking | Usage access shows which apps are used and when; guarded for sideloaded builds | Not worth a scary switch: Booklight's own history is enough |
| F11 Expanding snippets | It must see every key. No wording makes that comfortable | Do not build |

### 3.4 Googlebook-only specials

| Idea | What makes it Googlebook |
| --- | --- |
| L3, L4, L5 Places, displays, layouts | Desktop windows and connected displays |
| L8 A key per command | The system's custom shortcuts on the Action key |
| L15 Shortcut cheat sheet; S8 Magic Pointer and Gemini rows | The Googlebook's own keys and apps |
| L18 Notes role | Action + Ctrl + N and the hot-corner "Note-taking" action |
| C2 Pinned window | The pinned layer exists in desktop mode |
| C1 Colour from the screen | Android 17's eye dropper |
| A6, D3, X6 Linux commands, ports, VSCodeBook | The Debian VM and its port forwarding |
| S3 Keyboard backlight; J4 Glowbar; J6 touchpad tick | The hardware |
| X2, X3, X4, X8 BentoBar, StudioSnap, Welcome | His own apps, which only exist here |

---

## 4. Extensions

### 4.1 What the precedents teach

| Precedent | How rows arrive | What an action is | What to take |
| --- | --- | --- | --- |
| Android Quick Search Box (2009) | A provider queried per keystroke, plus a cache of picked rows | Intent columns the host fires | Per-source user switch, off by default; a letter threshold declared statically. Dead for third parties today: providers are guarded by `GLOBAL_SEARCH` |
| Kvaesitso plugins | An exported provider found by an intent action; `query` with a cancellation signal | Typed rows; the host derives the actions | The nearest Android precedent. Consent is a dialog; the plugin keeps the allowlist; the caller is checked by package name only, which is too weak |
| Alfred Script Filter | A script per keystroke, or once with Alfred filtering | Data (`arg`) into a workflow | `uid` for learning, `rerun`, `cache`, "host filters" as a mode |
| Raycast | Code rendering rows | Code | Script Commands: a header comment is the whole manifest |
| Command Palette | Top-level commands cached as stubs; live pages on demand | Runs in the extension; the host's follow-up comes back as data (`Dismiss`, `KeepOpen`, `ShowToast`, `Confirm`) | "Frozen" stubs so no process starts until a row is run |
| KRunner, GNOME search providers | Per keystroke over D-Bus | An id, run in the extension | A regex and a minimum length declared statically, so the host never wakes an extension that cannot match |
| Ulauncher | Per keyword query | Effects as data, plus one "call me back" action | The exact two-tier shape Booklight's `Effect` already has |
| Sharing shortcuts (Android 10) | Pushed ahead of time | Intents | Google replaced its pull service with push because binding at share time was slow |
| VS Code | Static contributions | Command ids | The declaration is the lazy-activation trigger |

Three rules follow. Declare statically whatever can be declared. Let the host do the matching wherever the
rows are a list. When a live answer is needed, gate it before waking anyone.

### 4.2 The mechanisms compared

| | What the other app does | Cost per keystroke | Who can do what to whom | Versions | Found how | Play |
| --- | --- | --- | --- | --- | --- | --- |
| **(a) Static declaration**: one `<meta-data>` and an XML file listing commands as intents or links | No code | None: read when the package changes, matched by Booklight | The app can only add rows that open itself. It can squat on a keyword or mimic a name | A version attribute | Read from every app with an icon; no new `<queries>` | Nothing |
| **(b) What the platform already has**: manifest shortcuts, capabilities, `SEND` / `VIEW` / `PROCESS_TEXT` filters; App Functions; AppSearch | Nothing | None | As (a), but nobody wrote it for Booklight, so nobody games it | The platform's | Already visible | `<queries>` entries, no form |
| **(c) A ContentProvider asked per keystroke** | About eighty lines on a small library | A cold process start (hundreds of ms, from memory), then a few ms | The provider sees what is typed. It must check who calls. A crash must not take Booklight down (unstable client) | A version in each call and answer | An intent action on the provider, or named in (a) | Nothing: no code is loaded |
| **(d) A bound service** (AIDL or Messenger) | More: connection lifecycle, death, an interface that is hard to evolve | As (c) once bound; the process is held at Booklight's priority while bound | As (c) | Interface versions | As (c) | Nothing |
| **(e) Broadcast, Locale and Tasker plug-ins** | Nothing new: hundreds exist | None; nothing comes back | Fire-only. The plug-in does what it was set up to do | The plug-in protocol's | `queryIntentActivities` on the edit action | Nothing |
| **(f) No app at all**: files in the notes folder; a URL that returns rows; scripts in the VM | The user writes a file or a script | Files: none. URL: a request | A file can only say what a link or recipe can. A URL sees what is typed in its scope | A version field | The user adds it | Fine while it is data. No interpreter inside Booklight, no downloaded code |

App Functions are the platform's own answer to this and are closed: executing another app's function needs an
allowlist entry. By source, other apps' static function metadata is readable through AppSearch, so Booklight
could one day show what an app offers, but not run it. AppSearch with per-package visibility would work as a
push index (an app shares a schema with Booklight's package and certificate), but it asks more code of the
other app than (c) and is untested here. Keep the row document shaped so both can be bridged later.

### 4.3 Recommended: three layers and one row document

**Layer 0: take what is declared.** L1, L2, F3, F5. No contract, no consent, nothing for anyone to adopt.

**Layer 1: a static file.** The other app adds

```xml
<meta-data android:name="io.github.kuscher.booklight.commands"
           android:resource="@xml/booklight_commands" />
```

```xml
<booklight version="1">
  <!-- a row: title and keywords are string resources, so they follow the app's languages -->
  <command id="mini" title="@string/mini" keywords="@string/mini_words" symbol="calculate">
    <open action="io.github.kuscher.summa.MINI" class=".ui.MiniLauncher" />
  </command>

  <!-- a scope with fixed choices: "shot" Tab, then area / window / screen -->
  <scope id="shot" keywords="shot|screenshot" name="@string/capture" hint="@string/capture_hint">
    <command id="area" title="@string/area">
      <open class=".CaptureActivity"><extra name="source" value="area" /></open>
    </command>
  </scope>

  <!-- a scope whose text goes into the intent: the same placeholders as Booklight's own links -->
  <scope id="pdf" keywords="pdf" name="@string/tools" hint="@string/tool_hint">
    <open class=".MainActivity" data="pdftoolbox://tool?q={argument}" />
  </scope>

  <!-- an action on a kind of thing: appears on text, link or file rows -->
  <action id="merge" on="file" mimeType="application/pdf" title="@string/merge">
    <open class=".MainActivity" />
  </action>

  <!-- rows that need the app: answered by its provider (layer 2) -->
  <scope id="calc" keywords="=|calc" name="@string/app_name" hint="@string/calc_hint"
         rows="live" authority="io.github.kuscher.summa.booklight"
         global="true" pattern="^[\d(.,-].*" minLength="2" />
</booklight>
```

Booklight reads the file when the app list changes and matches titles and keywords with its own `Matcher`, so
ranking, learning and typed verbs behave as for any row. The other app's process does not start until Enter.

**Layer 2: a provider, in two modes.** One exported `ContentProvider`, three `call()` methods:

| Method | When | Returns |
| --- | --- | --- |
| `catalog(scope)` | When the panel opens, if the cached copy is older than its `ttl` | All rows of a scope. Booklight keeps them on disk and filters them itself: recent folders, lessons, tools |
| `rows(text, scope, seq)` | Per keystroke, after a pause, only where the gate allows | Rows for this text: a sum, a conversion |
| `run(row, action, arg)` | On Enter, for an `invoke` action | An outcome |

Both sides speak one **row document**, a JSON rendering of `Result`:

```json
{ "v": 1, "ttl": 0,
  "rows": [ {
    "id": "timer", "title": "Timer", "subtitle": "Rings at 14:12",
    "icon": { "symbol": "timer" }, "kind": "command", "score": 0.9,
    "answer": "10:00",
    "body": { "slots": { "slots": [ { "label": "Length", "value": "10 min", "state": "typed" } ] } },
    "actions": [
      { "id": "start", "label": "Start", "symbol": "play", "done": "Timer started",
        "effect": { "invoke": { "arg": "600" } } },
      { "id": "open", "label": "Open BentoBar", "effect": { "open": { "class": ".ui.MainActivity" } } }
    ] } ] }
```

The same document is what a URL returns (A7) and what a script in the VM prints (A6): one contract, three
transports.

**In terms of `Model.kt`.** Additions, all plain data:

```kotlin
sealed interface Effect {
    // …the cases of 1.1 stay Booklight's own…

    /** Opens something inside the extension's own app. The Executor builds the intent; the extension only names it. */
    data class Open(val owner: String, val action: String? = null, val className: String? = null,
                    val data: String? = null, val extras: Map<String, String> = emptyMap()) : Effect
    /** Runs in the extension, with its permissions, and comes back with an Outcome. */
    data class Invoke(val owner: String, val row: String, val action: String, val arg: String = "") : Effect
}

/** What an Invoke answers: the host's next step, as data. */
sealed interface Outcome {
    data class Done(val message: String?) : Outcome       // footer text, then the panel closes
    data class Rows(val rows: List<Result>) : Outcome     // stay open and show these (a control's new level)
    data class Then(val effect: Effect) : Outcome         // one more effect from the allowed set
    data class Failed(val message: String) : Outcome
}

// Result gains `owner: String?` (the package the row came from): its badge, its switch, its learning key.
// Icon gains `Owner(name: String?)`: the extension's app icon, or one of its drawables by name.
// Body gains `Choice(options, selected)` (HearOn's listening modes) and `Text(lines)` (a script's output).
```

A remote `Provider` in `app/` wraps each enabled extension; `SearchEngine` does not change.

**What an extension's rows may do.** Only this, enforced in the `Executor`:

| Allowed | Rule |
| --- | --- |
| `Open` | An exported activity in the extension's **own** package. Booklight builds the intent from strings: no parcelled intents, no `PendingIntent`, no grant flags, no clip data |
| `OpenUrl` | `http` and `https`; another scheme only if it resolves to the extension's own package |
| `CopyText` | As for any row |
| `EnterScope` | Its own scopes |
| `Invoke` | Its own provider |
| `Then` | One of the above |

Everything that uses Booklight's own permissions or data stays Booklight's: `Uninstall`, `SetVolume`,
`SetBrightness`, `AppendNote`, `NewFile`, `SetTimer`, `Compose`, `Steps`, `Delete`, `Grant`, `Internal`,
launching other apps. `danger` and `confirm` are honoured; a `danger` action is never armed by default and
never run by Ctrl + digit, as for built-in rows. An extension row can be a recipe step: the recipe stores the
package, the row id and the action id.

Why `Open` is data and not a callback: a provider call from Booklight gives the extension no right to start
an activity from the background. Windows are opened by Booklight, which is visible. Work without a window
(start a timer, lock the screen) is an `Invoke`.

**Who may ask whom.**
- *Booklight to an extension.* The extension's provider checks the caller: package name **and** signing
  certificate (`PackageManager.hasSigningCertificate`, the digest in CLAUDE.md). The check lives in a
  one-file library, `BooklightExtension.kt`, so nobody writes it wrong. A custom permission would not do: a
  signature-level one cannot span two signers, and a normal one can be requested by anyone.
- *An extension to the user.* Rows from other apps carry that app's icon as a badge and its name in the
  subtitle. For an equal match they rank under apps and built-in rows. Reserved keywords cannot be taken. The
  Booklight window lists each extension with a switch and one line on what it adds and what it sees.
- *Consent.* Static rows are on once the app is installed: they are no more than an app's own shortcuts, and
  the footer says once "Summa added 3 commands". Live rows are **off until allowed**, because the provider
  sees what is typed: the first Tab into the scope shows one row, "Allow Summa to answer here", armed. A
  `global` live scope, which sees what is typed outside any scope, asks separately and says so.
- *What is never handed over.* Text typed inside another scope (a mail, a note, a password length), and text
  handed in by another app, unless the user picks that extension's action.

**Speed.**
- Static and catalog rows cost nothing per keystroke.
- A live scope the user is in: one `rows` call per pause of about 40 ms; answers carry `seq`, and a late one
  is dropped.
- A global live scope: asked only when its `pattern` and `minLength` match, with a budget of about 80 ms once
  warm. A row that arrives late slots in under the selection and never moves row one.
- Warm-up: when the panel opens, acquire an unstable provider client for each allowed global extension, at
  most three, so their processes start while the first letters are typed. Release on close. No binding is
  kept and nothing runs after the panel has gone.
- A cached app is frozen about ten seconds after it becomes cached, and a synchronous call into a frozen
  process kills it. Acquiring its provider should thaw it first (memory, not checked): measure before
  promising numbers, and never keep a raw binder to an extension without a held client.

**Versions.** `version` in the XML, `v` in every document, the host's version in every call. Fields are only
added. An unknown body falls back to title and subtitle; an unknown effect drops its action; a row left with
no actions is not shown (the 1.1 rule that rows may lose their actions already covers this).

**Being found.** A section "Extensions" in the Booklight window; `ext` in the panel; a "Works with Booklight"
mark and filter on googlebook.studio; `docs/EXTENSIONS.md` with the two snippets above; a lint command that
reads an APK's file and says what Booklight would show.

**Play.** No code is loaded and no broad package visibility is asked for. The privacy text gains one
sentence: what you type in an extension's scope is handed to that app on your device, after you allow it.
The data-safety answers do not change, since nothing leaves the device through Booklight.

**Later, not now.**
- *A bound service* only if rows must update while shown (a running countdown). Until then a control row's
  `Rows` outcome is enough.
- *The automation bridge* (A5, A9) as an extension app of its own, "Booklight for Tasker": it lists Tasker's
  tasks through the catalog mode and fires Locale plug-ins on `Invoke`. The core never becomes Tasker.
- *An AppSearch index* and *App Functions*, if Google opens them.

### 4.4 His own apps, made concrete

| App | What it contributes | Layer | Work on its side |
| --- | --- | --- | --- |
| **Summa** | New sheet, Mini calculator (its manifest shortcuts: free through L1). "Calculate" on any text chip (its `PROCESS_TEXT` activity: free through F5). Then the live scope `=`: units, money, dates, zones, the definitions sheet's names; Copy, Copy with the sum, Open in Summa | 0, then 2 live, global with a pattern | Nothing for the first two. About a day for the provider: `engine/` is pure Kotlin and a line evaluates in well under a millisecond. The alternative is linking `engine/` into Booklight: no hand-over, no consent, but two copies of rates and definitions |
| **BentoBar** | `timer 10m tea`, `stopwatch`, `pomodoro` (its `Timers` object); `awake 2h` as a switch row (`Caffeine`); `join` and today's meetings (its calendar item already finds Join links); CPU, memory, battery, network as answers. As the power pack: lock, screenshot, overview, all apps, notifications, quick settings, power (its Tools menu), later the window list | 1 for the commands, 2 for `invoke` and the answers | Two to three days: a provider with the caller check, mapping to code that exists. Its accessibility declaration and disclosure must say that it acts on Booklight's request. Works only while its service is on; otherwise the row says so and opens BentoBar's Setup |
| **StudioSnap** | `shot` with area, window, screen, scroll; `text from screen`; `record`; Editor. Later its last captures as a catalog | 1 | An hour: `CaptureActivity` is exported and already reads the `source` extra. Recording needs one more extra |
| **PDF Toolbox** | A row per tool; "Send to PDF Toolbox" on PDFs, pictures and documents (free through F3) | 1, a generated XML file | Half a day: generate the file from BentoPDF's tool list, and accept a tool's path in an exported intent (today only its debug hook does) |
| **VSCodeBook** | `code` and a recent folder; New window; Start Linux; Wireless debugging on (it holds the adb-granted setting); Open with for code files (free through F3) | 2 catalog, plus `invoke` | A few days: Java, no Gradle, and the folder list comes from the agent in the VM. Its agent is also the natural helper for A6 |
| **Canvas** | New image with a size; Open and Edit for pictures (free through F3) | 1 | Small, but in a Qt app: one activity extra |
| **HearOn Link** | `airpods`: battery of each bud and the case as an answer; listening mode as a row of options; conversation awareness as a switch | 2 live | A day or two; it needs the `Choice` body. Only while the AirPods are connected |
| **Windowcast** | `cast` and a known host or window | 2 catalog | Later: the Mac host is in design |
| **Welcome** | Lessons as rows, by what people ask ("snap a window", "screenshot") | 2 catalog, or a generated XML file | A day: titles and keywords exist as content. A private prototype; whether it ships is not Booklight's call |
| **Disco Sweeper** | `sweeper beginner`, `expert` | 1 | An hour |
| **Script, OfficeBook, Gmail Book, Perfect Sound** | New note, new document, compose, play a file: mostly what their filters and shortcuts already declare | 0 | Nothing |
| **govee** | `strip red`, `strip disco` against its page on `localhost:8765` | Links today; A7 later | Nothing |
| **Missing Link Labs** | Phone rows (X10) from whichever sibling app holds the link | 2 | After its milestones 2 and 3 |

Order: layer 0 in Booklight; the static file with StudioSnap and Disco Sweeper as the first two (an hour
each, and they prove the format); Summa as the first live provider; BentoBar as the first `invoke`; then
the catalog mode with VSCodeBook or PDF Toolbox.

---

## 5. Verify on a device before building

The device rules of CLAUDE.md apply: no Debian VM, no `uiautomator dump`, key and tap injection only into
Booklight's own window. Items marked *Alex* need his own clicks or his VM.

**Read-only queries (adb, `--user current`)**

| Question | How |
| --- | --- |
| Which text actions exist, and whose (F5) | `cmd package query-activities --brief -a android.intent.action.PROCESS_TEXT -t text/plain` |
| Who takes a PDF, a picture, a text file (F3) | The same with `-a android.intent.action.SEND -t application/pdf`, and with `VIEW` |
| Eye dropper present (C1) | `cmd package resolve-activity --brief -a android.intent.action.OPEN_EYE_DROPPER` |
| Quick viewer (F14) | `cmd package query-activities --brief -a android.intent.action.QUICK_VIEW` |
| Brightness dialog (S4) | `cmd package resolve-activity --brief -a com.android.intent.action.SHOW_BRIGHTNESS_DIALOG` |
| Who holds the notes role (L18) | `cmd role get-role-holders --user 10 android.app.role.NOTES` |
| Manifest shortcuts the system knows (L1) | `dumpsys shortcut`, the manifest entries per package; then `resolve-activity` on a few targets to see which are exported |
| AICore (I2) | `pm list features` and look for `aicore` |
| Protection levels on this build (C2, L19, I6) | `pm list permissions -f`, the entries for `USE_PINNED_WINDOWING_LAYER`, `SHOW_POWER_MENU`, `EXECUTE_APP_FUNCTIONS` |

**Probes in a debug build (`./bl probe …`, to be written)**

1. **Places (L3).** Launch three apps into thirds and one maximised; read `dumpsys activity activities` for
   their bounds. Repeat with an app that is already open. Summa's notes say bounds were ignored for its
   own windows; 1.1's halves worked on the Lenovo: find out which apps obey.
2. **The other display (L4)**, with a monitor: `setLaunchDisplayId` for each id in `DisplayManager.getDisplays()`.
3. **Launcher aliases (L8).** Enable one `activity-alias` with a launcher filter. Does Keyboard shortcuts ›
   Customize list it as its own entry, and can it hold a second key next to Booklight's? Then
   `dumpsys input`, the Custom Gestures section. *Alex* for the dialog.
4. **Pinned window (C2).** From a normal Booklight window, `requestWindowingLayer(PINNED)`; look for
   `windowingLayer = 2` in logcat, as Summa does. From the panel itself it should fail (not a desktop
   window): confirm.
5. **Notes role (L18).** `RoleManager.isRoleAvailable(ROLE_NOTES)`; whether Default apps shows a Notes entry
   once Booklight has a `CREATE_NOTE` activity with show-when-locked. *Alex* chooses it; then Action + Ctrl + N.
6. **Assistant extras (L19, F17).** With Booklight chosen as assistant and the permissions declared: does
   `showPowerMenu` answer; what does a `GlobalSearchSession` search return.
7. **Text classifier (F6).** `classifyText` and `generateLinks` on a date, an address, a tracking number: which
   types and actions come back for an ordinary app.
8. **Output switcher (S5), keyboard backlight (S3), touchpad haptics (J6), code scanner (C4):** one call each.
9. **Text actions that answer (F5).** Start Summa's "Calculate" and the system's "Look up" for result with
   read-only false: which return text.
10. **A widget in the panel (F15).** An `AppWidgetHost` in the overlay: the consent dialog, then whether the
    widget draws and takes clicks in a see-through window.
11. **Extension latency (§4).** A throwaway provider in Summa's debug build: time `call()` cold, warm, and
    after the app has been in the background for a minute (frozen). Kill the provider mid-call with an
    unstable client held.
12. **Archive (L11)** on a throwaway app from Play: is there a dialog, does the icon stay.
13. **On-device rewriting (I2).** `checkFeatureStatus()` for Proofreading, Rewriting, Summarization, Prompt.

**By hand**

- What the Quick Insert key offers (emoji, copied snippets?) on the HP and the Lenovo: it decides how much
  F8 and F9 are worth. *Alex.*
- Whether Chrome on a Googlebook installs extensions (F13). *Alex.*
- Whether another app that captures the keyboard (VSCodeBook with "Capture system shortcuts" on) swallows
  Booklight's key.
- Terminal's port prompts on the Lenovo, for A6. *Alex, his VM.*

**Unknown, and not answerable from a desk**

- Whether the flags for the eye dropper, the pinned layer, the power-menu API and App Function access are
  on in Googlebook builds, beyond what the HP notes show.
- Which apps publish anything to the assistant's AppSearch view.
- How the user-approval state for App Functions is enforced on shipped builds.
- Whether a notification listener can stay unbound while the panel is closed.
- Play's view of a host that hands typed text to other apps the user enabled. No policy text was found
  against it; no precedent was found for it either, beyond Kvaesitso on F-Droid.
- When, and to whom, Google opens the Glowbar.
- Whether Play reviewers accept "acts on another app's request" in BentoBar's accessibility declaration.

---

## 6. Sources

AOSP paths are on `https://android.googlesource.com/platform/` at `refs/heads/android17-release` unless a full
URL is given. Web pages were read on 1 October 2026.

**This repo and Alex's others (device facts)**
- `docs/research/device-findings.md`, `permissions.md`, `android-platform.md`, `launchers.md`, `use-cases.md`; `docs/design/ux-model.md`; `docs/superpowers/specs/2026-10-01-booklight-1.1-design.md`; `core/…/Model.kt`
- Welcome's device notes (a private repo; the path is in the private notes): system shortcuts §2.6, hot corners §4, Magic Pointer and Gemini §5, Files §6, desktop and widgets §8, clipboard and Quick Insert §10; and its platform notes (settings deep links A.4.6, Android 17 windowing and permissions A.6)
- `~/bentobar/docs/research/android-docs.md` (accessibility policy, Advanced Protection, restricted settings, Live Updates), `CLAUDE.md`, `items/ToolItems.kt`, `items/Timers.kt`
- `kuscher/vscodebook` `docs/GOOGLEBOOK.md` (the VM, port forwarding, adb, Android 17 permissions)
- `kuscher/summa` `CLAUDE.md` and manifest (engine, mini window, pinned layer, shortcuts, `PROCESS_TEXT`); `kuscher/studiosnap` `CaptureActivity.kt` and manifest; `kuscher/hearonlink`, `kuscher/canvas`, `kuscher/disco-sweeper` (`docs/WINDOWING.md`), `~/pdf-toolbox`, `~/windowcast`, `~/missing-link-labs`

**Android source**
- `frameworks/base/core/res/AndroidManifest.xml` (protection levels: `USE_PINNED_WINDOWING_LAYER`, `SHOW_POWER_MENU`, `REPOSITION_SELF_WINDOWS`, `EXECUTE_APP_FUNCTIONS`, `READ_CLIPBOARD_IN_BACKGROUND`, `MODIFY_QUIET_MODE`, `BIND_APPWIDGET`, `PACKAGE_USAGE_STATS`)
- `packages/modules/Permission/PermissionController/res/xml/roles.xml` (assistant, browser, notes, home)
- `frameworks/base/core/java/android/content/Intent.java` (`ACTION_OPEN_EYE_DROPPER`, `ACTION_CREATE_NOTE`, `ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE`, `ACTION_QUICK_VIEW`, `EXTRA_PROCESS_TEXT`)
- `frameworks/base/core/java/android/app/StatusBarManager.java` (`showPowerMenu`)
- `frameworks/base/services/core/java/com/android/server/clipboard/ClipboardService.java`
- `frameworks/base/services/accessibility/java/com/android/server/accessibility/AccessibilityManagerService.java`, `AbstractAccessibilityServiceConnection.java`, `AccessibilityServiceConnection.java`
- `frameworks/base/services/appfunctions/java/com/android/server/appfunctions/CallerValidatorImpl.java`
- `packages/modules/AppSearch/framework/java/external/android/app/appsearch/SetSchemaRequest.java`, `service/java/com/android/server/appsearch/appsindexer/AppSearchHelper.java`
- `frameworks/base/core/java/android/content/pm/ShortcutManager.java`, `services/core/java/com/android/server/pm/ShortcutParser.java`
- `frameworks/base/core/java/android/provider/DocumentsProvider.java`
- `frameworks/base/services/appwidget/java/com/android/server/appwidget/AppWidgetServiceImpl.java`
- `frameworks/base/services/core/java/com/android/server/statusbar/StatusBarManagerService.java`, `StatusBarShellCommand.java`
- `frameworks/base/services/core/java/com/android/server/media/MediaRouter2ServiceImpl.java`
- `frameworks/base/services/core/java/com/android/server/pm/PackageArchiver.java`, `UserManagerService.java`, `LauncherAppsService.java`
- `frameworks/base/services/core/java/com/android/server/wm/ActivityTaskManagerService.java`, `ActivityTaskSupervisor.java`, `SafeActivityOptions.java`
- `frameworks/base/services/core/java/com/android/server/input/InputGestureManager.java`, `InputShellCommand.java`
- `frameworks/base/libs/WindowManager/Shell/src/com/android/wm/shell/common/MultiInstanceHelper.kt`, `…/desktopmode/DesktopModeShellCommandHandler.kt`
- `frameworks/base/services/core/java/com/android/server/search/Searchables.java`, `…/slice/SliceManagerService.java`, `core/java/android/app/slice/Slice.java`
- `frameworks/base/packages/Shell/AndroidManifest.xml`
- `packages/modules/Permission/service/java/com/android/ecm/EnhancedConfirmationService.java`
- `packages/modules/Virtualization/android/TerminalApp/` (manifest, `PortNotifier.kt`, `AndroidToVmBridge.kt`)
- `packages/apps/QuickSearchBox/` (`Config.kt`)

**Android and Play documentation**
- https://developer.android.com/about/versions/17/release-notes · https://developer.android.com/about/versions/17/features · https://developer.android.com/about/versions/17/behavior-changes-all · https://developer.android.com/about/versions/17/behavior-changes-17
- https://developer.android.com/ai/appfunctions · https://developer.android.com/jetpack/androidx/releases/appfunctions · https://android-developers.googleblog.com/2026/02/the-intelligent-os-making-ai-agents.html
- https://developer.android.com/training/package-visibility/use-cases · https://developer.android.com/training/package-visibility/declaring
- https://developer.android.com/develop/ui/views/appwidgets/host
- https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts · https://developer.android.com/guide/app-actions/action-schema
- https://developer.android.com/training/sharing/direct-share-targets
- https://developer.android.com/develop/ui/views/search/adding-custom-suggestions · https://developer.android.com/develop/ui/views/search/searchable-config
- https://developer.android.com/develop/adaptive-apps/guides/support-desktop-windowing · https://developer.android.com/develop/adaptive-apps/guides/support-connected-displays · https://developer.android.com/develop/adaptive-apps/guides/support-bubbles
- https://developer.android.com/develop/ui/views/notifications/live-update
- https://developer.android.com/privacy-and-security/risks/intent-redirection · https://developer.android.com/guide/components/activities/background-starts · https://developer.android.com/guide/topics/permissions/defining
- https://developer.android.com/privacy-and-security/advanced-protection-mode · https://developer.android.com/privacy-and-security/local-network-permission
- https://source.android.com/docs/core/perf/cached-apps-freezer
- https://developers.google.com/ml-kit/genai · https://developers.google.com/ml-kit/genai/proofreading/android · https://developers.google.com/ml-kit/language/entity-extraction
- https://developers.home.google.com/apis/android/overview
- https://developer.chrome.com/docs/android/custom-tabs/guide-ephemeral-tab · https://developer.chrome.com/docs/android/intents
- Play: https://support.google.com/googleplay/android-developer/answer/10964491 (accessibility) · /answer/9888170 (permissions) · /answer/9888379 (downloaded and interpreted code) · /answer/10158779 (package visibility) · /answer/16935362 (contacts, from 27 January 2027) · /answer/14115180 (photos and videos)

**Launchers**
- Quicksilver: https://qsapp.com/manual/getting-started/concepts-and-terminology/ · https://qsapp.com/manual/appendix/tips/ · https://qsapp.com/manual/features/triggers/ · https://qsapp.com/manual/features/Text/ · https://qsapp.com/manual/features/Clipboard%20and%20Shelf/
- LaunchBar: https://www.obdev.at/resources/launchbar/help/InstantSend.html · …/SendingItems.html · …/AbbreviationSearch.html · …/Browsing.html · …/ClipboardHistory.html · …/Calculator.html
- Alfred: https://www.alfredapp.com/help/features/universal-actions/ · /help/features/file-search/ · /help/features/clipboard/ · /help/features/snippets/ · /help/features/default-results/fallback-searches/ · /help/workflows/inputs/script-filter/json/ · /help/workflows/triggers/external/ · /help/features/large-type/
- Raycast: https://manual.raycast.com/command-aliases-and-hotkeys · /window-management · /clipboard-history · /calendar · /ai/quick-ai · /ai/ai-commands · /script-commands · /notes · /dynamic-placeholders · https://developers.raycast.com/information/manifest · /information/lifecycle/deeplinks · /information/security · https://github.com/raycast/script-commands · https://www.raycast.com/changelog/1-64-0
- Spotlight: https://www.apple.com/newsroom/2025/06/macos-tahoe-26-makes-the-mac-more-capable-productive-and-intelligent-than-ever/ · https://www.macrumors.com/guide/macos-tahoe-hidden-features/ · https://9to5mac.com/2026/09/14/macos-27-golden-gate-now-available-here-is-everything-new/
- PowerToys: https://learn.microsoft.com/en-us/windows/powertoys/run · https://learn.microsoft.com/en-us/windows/powertoys/command-palette/extensibility-overview · …/command-palette/dock · https://github.com/microsoft/PowerToys (`src/modules/cmdpal/doc/initial-sdk-spec/initial-sdk-spec.md`)
- Linux: https://develop.kde.org/docs/plasma/krunner/metadata/ · https://invent.kde.org/frameworks/krunner (`org.kde.krunner1.xml`) · https://developer.gnome.org/documentation/tutorials/search-provider.html · https://docs.ulauncher.io/en/stable/extensions/actions.html · https://davatorium.github.io/rofi/current/rofi-script.5/ · https://man.archlinux.org/man/dmenu.1.en · https://albertlauncher.github.io/basics/
- Others: https://www.hammerspoon.org/docs/hs.chooser.html · https://www.hammerspoon.org/go/ · https://www.voidtools.com/faq/ · https://github.com/ospfranco/sol · https://www.monarchlauncher.com/ · https://github.com/vicinaehq/vicinae · https://code.visualstudio.com/api/references/activation-events
- Android launchers: https://kvaesitso.mm20.de/docs/developer-guide/plugins/get-started.html · …/plugins/access-control.html · https://kvaesitso.mm20.de/docs/user-guide/search/quickactions.html · https://github.com/MM2-0/Kvaesitso · https://sesame.ninja/ · https://www.androidauthority.com/nova-launcher-best-feature-sesame-shortcuts-3608155/ · https://help.niagaralauncher.app/article/115-pop-ups · https://github.com/Neamar/KISS · https://blog.google/products-and-platforms/devices/chromebooks/whatsnew-100/

**Automation and power-user projects**
- Tasker: https://tasker.joaoapps.com/invoketasks.html · https://tasker.joaoapps.com/contentprovider.html · https://tasker.joaoapps.com/plugins-intro.html · https://github.com/twofortyfouram/android-plugin-api-for-locale
- Termux: https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent · https://github.com/termux-play-store/termux-apps
- Shizuku: https://github.com/RikkaApps/Shizuku/releases · https://github.com/RikkaApps/Shizuku/issues/2432 · https://github.com/RikkaApps/Shizuku/issues/2422
- Home Assistant: https://developers.home-assistant.io/docs/api/rest/ · https://companion.home-assistant.io/docs/integrations/url-handler/
- MacroDroid: https://macrodroidforum.com/wiki/index.php/Trigger:_Webhook_(URL) · Automate: https://groups.google.com/g/automate-user/c/IPmrJ4HjXwk

**Press on the Googlebook (reported, not verified)**
- https://9to5google.com/2026/09/21/googlebook-os-hands-on/ · https://9to5google.com/2026/09/21/googlebook-launch/
- https://www.androidauthority.com/googlebooks-hands-on-impressions-3713390/ (the Quick Insert key and copied snippets)
- https://www.androidpolice.com/google-googlebook-laptop-hands-on/
- https://www.androidauthority.com/android-17-beta-2-advanced-protection-mode-accessibility-apps-3648860/
- Desktop Chrome with extensions on Android: https://www.howtogeek.com/chrome-for-android-extensions-desktop/

**From memory or secondary sources only, flagged where used**
- Cold-start cost of a provider call and whether acquiring a provider thaws a frozen process.
- The Play services code scanner needing no camera permission in the caller.
- The message, Meet and calendar-date links in P4 and P5.
- MacroDroid's and Automate's intents (forum posts).
- LaunchBar's staging keys (a search snippet).
