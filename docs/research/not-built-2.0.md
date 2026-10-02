# Booklight: ideas that were written down and are not in 2.0

*Inventory, 1 October 2026, against this repo. Every statement about the code was checked against the tag `v2.0` itself (321fd59, read with `git show` and `git grep`), not against the working tree. Reading only: no file in the repo was changed, no device was touched, nothing was built. Nothing here is re-rated and no idea is new: every row names where it was written.*

**2.1 came after this.** It added a letters picker (`abc`, `core/Letters.kt`), which comes from no written idea in this inventory, and the calculator's decimal comma, listed in section 3 as a 2.0 flaw.

**How to read it**

- **Table A** is the catalogue of `docs/research/next-features.md` §2 (95 ideas, ids L, F, P, C, S, A, I, D, X, J). Only the 86 that are not in 2.0 are listed. Value, Effort, Tier and GB are copied from the catalogue as written. GB means how Googlebook-only the idea is: *only*, *better* or *same*. Tiers are that file's: 0 nothing, 1 an install-time permission, 2 one prompt or picker, 3 a Settings switch or a role, 4 accessibility, listener, usage access or an adb grant.
- **Table B** holds the 48 ideas written anywhere else (the extension contract, PLAN.md, the three specs, PICKING-UP, on-device-ai.md, the 2.0 reviews, ux-model.md, and use-cases.md where next-features carries it over). Most have no ratings in their source; the cell says so.
- **Status** is one of: *never decided* · *deferred* (by the owner, with his words where a document has them, or by the roadmap) · *not chosen for 2.0* (he was shown it in the top fifteen and did not tick it; the spec records the outcome, not his words) · *partly built* · *ruled out* (and by whom).
- Where a document left it unclear whether something is in the app, the status names the file that settled it.

**Two things the documents say that the repo does not bear out**

- `next-features.md` has no section of questions for the owner. PICKING-UP ("after 1.1") mentions "twelve questions for Alex", but the file has had one commit (9a1c3fd) and it never held them. His answers survive only as the 2.0 spec's §1 and §2.
- The 2.0 spec says of the top fifteen: "Not chosen, so not built: actions on a thing, colour from the screen, a key per command, packs, Summa's answers, text tools, the shelf, BentoBar's answers; live extensions; the Gmail relay." For every other catalogue idea no decision is recorded anywhere.

---

## 1. Every idea not in 2.0

### Table A. The catalogue (`docs/research/next-features.md` §2)

#### §2.1 Launching and windows

| ID · name | The user would | Value | Effort | Tier | GB | Status in 2.0 |
| --- | --- | --- | --- | --- | --- | --- |
| L2 · Search inside an app | type `keep milk` and land in Keep's own search, from what the app declares as a capability | Medium: only apps that declare them | M: read the XML by hand (the framework parser skips capabilities) | 0 | same | Never decided. `providers/AppCommands.kt` reads manifest shortcuts and Booklight's own file, no capabilities |
| L5 · Layouts | draw a recipe on a picture of the screen: up to eight apps, each in a place, each with a link or file | Medium to high | M: the editor | 0 | only | **Partly built.** A recipe can open apps in any of the thirteen places (`data/Recipes.kt`). The drawn editor and a link or file per app are missing. Never decided |
| L6 · The comma | type `chrome, gmail, calendar` and open all three with one Enter | Medium | M: the field holds one chip today; staged rows need a counted chip | 0 | same | Never decided |
| L7 · Aliases you set | give any row an alias that always wins | Medium: learning already does most of it | S | 0 | same | Never decided |
| L8 · A key per command | switch on "Booklight Emoji" or "Booklight Note" and bind each to its own Action key | High | S to M: `activity-alias` switched on by the user; each is one more icon in the Apps list | 0 | only | Not chosen for 2.0 (spec §1). No alias in the manifest. Still needs the device check: does each alias get its own shortcut |
| L9 · Fallback rows you choose | decide which rows close every list (add "Note this", "Search Drive") | Medium | S | 0 | same | Never decided. Today the two are fixed (web search, Ask Gemini); Ask Gemini can only be hidden |
| L10 · What you ran | type `!!` or press Up again to list and search past texts | Low to medium | S | 0 | same | Never decided. Since 1.1 Up on an empty field brings back the one last text that was not run |
| L11 · Archive | remove an app and keep its data, from its row | Low to medium | S | 1 · `PackageInstaller.requestArchive` with `REQUEST_DELETE_PACKAGES` (held) | same | Never decided |
| L12 · More app actions | open an app's Notifications, Open by default, App language or Battery page from its row | Low to medium | S | 0 · Settings actions with `package:` | same | Never decided. `providers/AppsProvider.kt` has App info only |
| L13 · Open windows | type `win inbox` and raise a window by its title | High | M to L: minimised windows have no window to list | 4 · accessibility `getWindows()`; an action on a node raises its window | better | Deferred to the power pack (PLAN §4 3.0; §11.8, his decision: "Advanced tiers as planned"). Research: a separate app, or BentoBar |
| L14 · Window commands | type `snap left`, `close window`, `next desk` for the focused window | Medium to high | L: only shell may inject the system's chords or call the desk commands | 4 · Shizuku or adb (`input keycombination`, `wm shell`) | only | Never decided. Research: an "expert" companion; Shizuku is broken on Android 17. `k snap` shows the keys instead |
| L16 · Booklight as a picker | let another app or script hand Booklight a list and get the pick back | Medium: glue for his own apps and scripts | M | 0 · an exported activity for result | same | Never decided |
| L17 · Links into Booklight | open `booklight://run/morning`; put a recipe on the desktop or the icon's right-click menu | Medium | S: confirm before running a link from outside | 0 · `requestPinShortcut`, dynamic shortcuts | better | Never decided. The widget and the tile exist; the manifest has no link filter and no shortcuts |
| L18 · Booklight as the notes app | press Action + Ctrl + N or use the hot corner to open the note scope | Medium to high: a second system key with no permission | M | 3 · `ROLE_NOTES` (not requestable; chosen in Default apps; replaces Keep there) | only | Never decided. Research §3.2: "Worth it if the role is offered on Googlebooks" |
| L19 · Power menu | type `power` and get the system's power menu, when Booklight is the assistant | Low to medium | S | 3 · `SHOW_POWER_MENU` comes with the assistant role (flag-gated) | same | Never decided. Research: "Take them when checked" |
| L21 · Browser chooser | click any link and choose in the panel which browser or app opens it | Medium; one more step for every link | M | 3 · `ROLE_BROWSER` (requestable) | same | Ruled out by the research (not by the owner): "A different product; keep it out of Booklight" |

#### §2.2 Files and content

| ID · name | The user would | Value | Effort | Tier | GB | Status in 2.0 |
| --- | --- | --- | --- | --- | --- | --- |
| F2 · Files in folders you grant | type a file's name and get files from granted folders, newest first | Medium (people navigate; they rarely search) | L: no provider search on a tree grant, so walk and index; `Download` cannot be granted | 2 · `ACTION_OPEN_DOCUMENT_TREE` | better | Deferred by the roadmap (PLAN §4, 2.1 Reach). Research: "Large effort for the least-used launcher feature; after F3" |
| F3 · Actions on a file from what apps declare | get Open with, Send to, Copy path and Show in Files on a file row | High: PDF Toolbox, Canvas and VSCodeBook appear with no work there | M | 0 · `<queries>` entries for `SEND` and `VIEW` with types | better | Not chosen for 2.0 (spec §1: "actions on a thing") |
| F4 · Take files and links, not only text | share a file or link to Booklight and choose what to do with it | Medium to high | M | 0 · intent filters | same | Not chosen for 2.0. The manifest takes `text/plain` only (since 1.1) |
| F5 · Text actions from other apps | run another app's "Translate" or "Calculate" on a text chip and get the result back as the chip | High for the effort | S to M | 0 · `<queries>` for `PROCESS_TEXT` | same | Not chosen for 2.0. Booklight is itself a `PROCESS_TEXT` target; it lists nobody else's |
| F6 · What the text is decides the rows | get Event for a date, Maps for an address, the carrier for a tracking number | Medium to high | M: the platform `TextClassifier` first, own rules behind it | 0 | same | Never decided. Also on-device-ai.md runner-up 1. No `TextClassifier` call in the code |
| F7 · Use as text | press Tab on an answer, emoji or link to make it the chip, then mail, note or QR it | Medium | S: `EnterScope` with the text scope | 0 | same | Never decided |
| F8 · The shelf | find for a week what Booklight copied, was handed or transformed, and pin items | Medium to high | S to M | 0 | same | Not chosen for 2.0 |
| F9 · Clipboard history | search everything copied lately | High in every study, but see Sure | L | 4 · not accessibility: the clipboard service lets only the focused app, the default keyboard and shell read. Routes: a focus trick from a service, or Shizuku | same | Deferred to the power pack in PLAN §4, but the research says that plan "does not hold as written" and: "Do not build beyond the shelf (F8) until checked on a device". Play: "likely rejection" |
| F10 · Paste in place | press Enter and have the emoji, snippet or answer land in the field he came from | High | M | 4 · accessibility with `FLAG_INPUT_METHOD_EDITOR`, `commitText` | same | Deferred to the power pack (PLAN §4 3.0, §11.8). 2.0 has one relative: Replace puts a prompt's answer back into the field the text was handed over from |
| F11 · Snippets that expand as you type | type `;sig` anywhere and have it expand | Medium | L: it must watch every key | 4 · accessibility key filter, or being the keyboard | same | Ruled out by the research: "Not recommended", "Do not build" |
| F12 · Bookmarks, imported | pick Chrome's exported bookmarks file once and find its links as rows | Medium | S to M | 2 · file picker per import | same | Never decided. Research: "Small, honest, and the only way to bookmarks" |
| F13 · Open tabs | find and switch to an open Chrome tab, through a companion extension | High if it can be done | L: no path for Booklight to pull from Chrome without a server | 0 on the Android side | better | Never decided. Unverified whether Chrome on a Googlebook installs extensions |
| F14 · Look before opening | press Space on a file, picture or link to see it | Medium | M: a new tall body, or the system's quick viewer | 0 · `ACTION_QUICK_VIEW` | same | Never decided |
| F15 · A widget in the panel | type `widget calendar` and see another app's widget as a tall row | Medium; glanceable | L: a new body kind, host lifecycle, keyboard focus inside a see-through window | 2 · system consent per widget (`ACTION_APPWIDGET_BIND`) | same | Never decided. Research: "needs a spike before a promise" |
| F16 · Files from a template | type `new meeting-notes` and get a file made from a template | Medium | S to M | 0 | same | Never decided. `scopes/Jot.kt`: `new` knows file, folder, doc, sheet, slides |
| F17 · What apps publish to the assistant | find what other apps publish to the assistant's search, when Booklight has that role | Unknown | M | 3 · `READ_ASSISTANT_APP_SEARCH_DATA` comes with the role | same | Never decided. Research: "nobody knows which apps publish: check first" |

#### §2.3 People and communication

| ID · name | The user would | Value | Effort | Tier | GB | Status in 2.0 |
| --- | --- | --- | --- | --- | --- | --- |
| P1 · Your people | type `anna` and get Mail, Meet, WhatsApp, Copy address from a short list kept in Booklight | High | M | 0 | same | Never decided |
| P2 · Choose a contact | pick a recipient from the system's contact picker in Mail's To slot | Medium | M | 2 · `ACTION_PICK_CONTACTS`, no permission | same | Deferred by the roadmap (PLAN §4, 2.1 Reach). Research: "Cheap and future-proof against the 2027 contacts rule" |
| P3 · Next meeting and Join | type `join` or `next` and open the next meeting's link | High | M, or S if BentoBar answers (X2) | 2 · `READ_CALENDAR`; no conference column, the link is in the description or place | same | Never decided. Research: "Do it through BentoBar first" |
| P4 · Meeting rows with no permission | type `meet` to start a meeting, `meet abc-defg-hij` to join, `cal fri` for Friday | Medium | S | 0 · links | same | Never decided. Not among the default links (`core/Sites.kt`); a user can add his own |
| P5 · Message links | type `wa anna running late` and open the chat with the text | Low to medium on a laptop | S after P1 | 0 | same | Never decided |
| P6 · Share… on every row | send any text, link or file through the system's share sheet | Medium: the only road to Direct Share and Quick Share | S | 0 · `Intent.createChooser` | same | **Partly built.** Only the QR code row has Share (`scopes/Picks.kt`); a note for Keep falls back to the share sheet when no notes app answers (`Executor.kt`). Never decided for the rest |
| P7 · Notifications as rows | search waiting notifications; open, dismiss, `reply anna on my way` | High | M to L: a listener is a bound service, against "nothing in the background" | 4 · notification listener | same | Never decided. Research: a separate app; "it could ship first" |
| P8 · Now playing | see the title, scrub, and use the player's own controls | Medium | M | 4 · listener, for `getActiveSessions` | same | Deferred to the power pack (PLAN §4 3.0: "Notification listener: now playing") |

#### §2.4 Capture and creation

| ID · name | The user would | Value | Effort | Tier | GB | Status in 2.0 |
| --- | --- | --- | --- | --- | --- | --- |
| C1 · Pick a colour from the screen | type `pick colour`, click a pixel, and get HEX, RGB, HSL, OKLCH | High | S | 0 · `Intent.ACTION_OPEN_EYE_DROPPER` (API 37), result `EXTRA_COLOR` | better (any Android 17) | Not chosen for 2.0 (spec §1) |
| C3 · Large type | type `large 0171 555 1234` and see it across the screen until a key is pressed | Low to medium | S: a second see-through window without blur | 0 | same | Never decided |
| C4 · Scan a code | scan a QR code with the webcam and get its text as the chip | Low on a laptop | S to M | 0 · the Play services code scanner needs no camera permission in the caller | same | Never decided |

#### §2.5 System and device

| ID · name | The user would | Value | Effort | Tier | GB | Status in 2.0 |
| --- | --- | --- | --- | --- | --- | --- |
| S1 · Settings rows that land on the switch | type `screen timeout`, `modifier keys`, `play protect`, `glowbar`, `magic pointer` and arrive at that item | Medium | S | 0 · the `:settings:fragment_args_key` extra | better | **Partly built.** `screen timeout`, `text size` and `system update` open pages of their own (`res/values/settings_pages.xml`). Nothing opens scrolled to an item, and the other four have no row. Never decided |
| S2 · Answers about the device | type `battery`, `storage`, `ram`, `ip`, `wifi` and read the value in the row | Medium | S | 0, or 1 · `ACCESS_NETWORK_STATE` for the address | same | Never decided. `battery` and `storage` find the settings pages |
| S3 · Keyboard backlight | type `backlight 50` and set it as a level | Low to medium | S to M | 0 · the input device's `LightsManager` | only | Never decided. Needs a device check |
| S4 · Brightness with no grant | open the system's own brightness slider from the hollow brightness row | Low to medium | S | 0 · SystemUI's exported brightness dialog | same | Never decided |
| S5 · Sound output | type `output` and get the system's output switcher | Medium | S | 0 · `MediaRouter2.showSystemOutputSwitcher()` | same | Never decided |
| S6 · Lock, screenshot, overview, notifications, quick settings, power | type `lock` or `screenshot` and have it happen | High | S once a service exists | 4 · accessibility `performGlobalAction`. BentoBar's Tools menu already does all of them (X3) | same | Deferred to the power pack (PLAN §4 3.0, §11.8). `k lock` shows the keys instead |
| S7 · Real switches | flip dark theme, night light, battery saver, wireless debugging from a row | Medium | M | 4 · `WRITE_SECURE_SETTINGS` by adb. By source `ui_night_mode` is not applied live | same | Deferred to the power pack (PLAN §4 3.0: "Optional adb grant: the real toggles") |
| S8 · Gemini and Magic Pointer rows | type `magic pointer` or `gemini voice` and start it | Low to medium | S | 0 · exported actions | only | Never decided |
| S9 · Ranking from the whole system | get rows ranked by all app use, with a mark on open apps | Low: Booklight's own learning covers it | M | 4 · usage access | same | Ruled out by the research: "Not worth a scary switch: Booklight's own history is enough" |

#### §2.6 Automation and recipes

| ID · name | The user would | Value | Effort | Tier | GB | Status in 2.0 |
| --- | --- | --- | --- | --- | --- | --- |
| A1 · Packs | keep links, snippets, recipes and prompts as files in `commands/`, export them, add one from a link | High: sharing, versioning, editing in VSCodeBook | M: the format, and a clear import screen | 0 beyond the folder already granted | same | Not chosen for 2.0 (spec §1) |
| A2 · Recipes that take text | type `standup shipped 1.1` and have the text go into the steps | Medium | M | 0 | same | Never decided |
| A3 · Save what I just did | type `save recipe` after a few runs and get them offered as steps | Medium | M | 0 | same | Never decided. ux-model §9 marks it "Test first" |
| A4 · Webhooks | type `deploy` or `lights off`, press Enter twice, and call a URL | Medium | M: tokens in the Keystore | 1 · `INTERNET` (held) | same | Deferred by the roadmap (PLAN §4, 2.1 Reach) |
| A5 · Tasker tasks | type `tasker` and run one of his tasks | Medium for that crowd: Booklight borrows every permission they hold | S to M | 1 · Tasker's `PERMISSION_RUN_TASKS` plus its "Allow External Access" | same | Never decided. Research §4.3: as a bridge app, "Booklight for Tasker" |
| A6 · Linux script commands | run a script in the VM from a row and read its output there | High for developers | L: a helper in the VM on a forwarded loopback port, every request signed. VSCodeBook's agent is a candidate | 1 · `INTERNET` | only | Deferred by the roadmap (PLAN §4, 2.1 Reach) |
| A7 · Rows from a URL | make a link live: it returns rows as he types | Medium | M after the contract | 1 · `INTERNET` | same | Never decided. Needs the row document (Table B, EX5) |
| A8 · Termux commands | run a Termux command from a row | Low here | M | 2 · `com.termux.permission.RUN_COMMAND`; only the GitHub build has it, and Play Protect reportedly blocks that build on 17 | same | Ruled out by the research: "Skip: the VM is there" |
| A9 · Locale plug-ins as steps | set a plug-in up once and fire it from a recipe | Medium | M | 0 | same | Never decided. Research: "As a bridge app (§4), not in the core" |

#### §2.7 AI

| ID · name | The user would | Value | Effort | Tier | GB | Status in 2.0 |
| --- | --- | --- | --- | --- | --- | --- |
| I3 · An answer with his own API key | get a cloud model's answer in the panel | High | M | 1 · `INTERNET` | same | Never decided |
| I4 · Say it plainly | type "remind me tomorrow at nine to call the bank" and get the right row, filled in | Medium | L: a small model with a fixed schema, rules behind it | 0 | same | Deferred. Spec §13: "Larger; proposed for the release after, starting with dates and reminders". PICKING-UP lists it under "Not built". The proposal is the session's, not his words |
| I5 · Translate and define | type `tr de good morning` and get the translation; define a word | Medium | S through F5, M on the device (about 30 MB a language) | 0 | same | **Partly built.** `de …` (or `en …`) translates through the on-device model, or hands over to Gemini (`prompt_seeds` in `strings_20.xml`). Missing: `tr` with any language, define, translation without the model |
| I6 · Let Gemini call Booklight | have Gemini add a note, start a timer or run a recipe through App Functions | Speculative | M | 0 to publish | same | Never decided |

#### §2.8 Developer

| ID · name | The user would | Value | Effort | Tier | GB | Status in 2.0 |
| --- | --- | --- | --- | --- | --- | --- |
| D1 · Text tools | type `b64`, `jwt`, `sha256`, `epoch`, `json`, `lorem`, `0xff`, `255 in hex`, `U+2192` and copy the result | Medium to high for this audience | S | 0 | same | Not chosen for 2.0 (spec §1). `core/Clip.kt` has six transforms, none of these |
| D2 · GitHub in the panel | type `gh prs`, `gh issues`, `gh inbox` and get them as rows | Medium to high for Alex | M | 1 · `INTERNET`, a token in the Keystore | same | Never decided. `gh owner/repo` and `#12` jumps exist since 1.1 |
| D3 · What is listening | type `ports` and see which local ports answer | Medium | S | 1 · `INTERNET` | better | Never decided. `:3000` opens one port |
| D4 · Facts about an app | type `pkg summa` and copy package, version, SDK, installer, signing digest | Low to medium | S | 0 (apps with an icon only) | same | Never decided |
| D5 · Developer rows | type `wireless debugging`, `adb pair`, `running services` and land on the switch | Medium | S | 0 | better | Never decided. "adb" finds the Developer options page, not the switch |

#### §2.9 What extensions make possible

"The effort is the other app's." None of the repos on this Mac (`~/bentobar`, `~/pdf-toolbox`, `~/windowcast`, `~/googlebook-welcome`, `~/missing-link-labs`) declares `io.github.kuscher.booklight.commands`; the others are not checked out here.

| ID · name | The user would | Value | Effort | Tier | GB | Status in 2.0 |
| --- | --- | --- | --- | --- | --- | --- |
| X1 · Summa answers | type `5 ft in cm`, `120 usd in eur`, `3pm berlin in sf` and copy the answer | High | M: a provider around its pure-Kotlin engine. Or link the engine into Booklight | 0 | same | Not chosen for 2.0 (spec §1). It was his plan for 2.0: PLAN §11.6 "Sums: the small calculator now; Summa as an extension in 2.0". Needs the live layer (EX1) |
| X2 · BentoBar | type `timer 10m` for the status bar, `awake 2h`, `join`; read CPU, memory, battery | High | M | 0 for Booklight | only | Not chosen for 2.0 ("BentoBar's answers"). Needs Invoke (EX3) and live rows |
| X3 · BentoBar as the power pack | type `lock`, `screenshot`, `overview`, `notifications`, `power`; later the window list | High | M, plus new wording in BentoBar's accessibility declaration | 0 for Booklight; BentoBar holds the service | only | Not chosen for 2.0 (item 15 of the top list). Unknown whether Play accepts "acts on another app's request" |
| X4 · StudioSnap | type `shot area`, `shot window`, `shot scroll`, `record`, `text from screen` | High | S: its capture activity already takes the mode as an extra | 0 for Booklight | only | **Partly built.** Booklight's side is in 2.0 (it reads an app's commands file). StudioSnap's file is missing: spec §3, "His own apps each need a small release to carry such a file: not part of 2.0" |
| X5 · PDF Toolbox | type `merge pdf`, `compress pdf`; get "Send to" on a PDF | Medium to high | S to M: a way to open a tool by name | 0 | better | **Partly built**, as X4 for the rows. "Send to" on a PDF also needs F3 |
| X6 · VSCodeBook | type `code booklight` for a recent folder; New window; Start Linux | High for developers | M: the recent folders live in the VM | 0 for Booklight | only | Never decided. Needs the catalog layer (EX2) |
| X7 · HearOn Link | type `airpods` for the battery; choose noise control from a row | Medium | M: needs a "choice" body in the contract | 0 for Booklight | better | Never decided. Needs live rows and the Choice body (EX6) |
| X8 · Welcome and another app | type "how do I snap a window" and open the lesson; `scene hero` | Medium | S to M | 0 | only | **Partly built**, as X4, for the route through a generated file only |
| X9 · The rest | type `new image 1920x1080`, `cast`, `sweeper expert`, `strip red` | Low to medium each | S each | 0 | better | **Partly built.** Any app's exported manifest shortcuts are rows already (Script, Summa). The rest needs each app's file |
| X10 · The phone | ring the phone, send it the clipboard or a link, run a recipe from it | Medium | L: the SDK is at milestone 1 | 0 for Booklight | only | Never decided. §4.4: "After its milestones 2 and 3" |

#### §2.10 Delight

| ID · name | The user would | Value | Effort | Tier | GB | Status in 2.0 |
| --- | --- | --- | --- | --- | --- | --- |
| J1 · Confetti | type `confetti`, or use it as a recipe step | Low, but people show it to each other | S to M: a second see-through window with no blur (the panel's window is exactly the panel) | 0 | same | Never decided |
| J2 · Your year in Booklight | see opens a day, top rows, keys saved; copy as a picture | Low to medium | S: the history file has it | 0 | same | Never decided |
| J3 · Chance | type `flip`, `roll 2d6`, `pick anna, ben, cy` | Low | S | 0 | same | Never decided |
| J4 · Glowbar | type `disco` for Glowbar Disco; later light the bar as the panel opens | Low today | S | 0 · `glowbar://disco` | only | Never decided. Driving the bar is signature-only |
| J5 · Weather | type `weather berlin` and read it in the row | Medium | S to M | 1 · `INTERNET` | same | Never decided |
| J6 · A tick in the touchpad | feel a tick when an action runs | Low | S | 0 | only | Never decided. Needs a device check |

### Table B. Ideas written outside the catalogue

#### The extension contract (`next-features.md` §4.3; `docs/EXTENSIONS.md` "What it cannot do, on purpose"; 1.0 spec §3.5)

What 2.0 built of it is layer 0 in part (manifest shortcuts) and layer 1 in part (`command`, `scope`, `open`, `extra`). None of the rows below has ratings of its own; §4.2 rates only the mechanisms.

| # · name | The user would | Where written | Ratings as written | Status in 2.0 |
| --- | --- | --- | --- | --- |
| EX1 · Live rows from another app | get another app's answer while typing (a sum, a conversion), after allowing it once | next-features §4.3 layer 2 `rows`; EXTENSIONS.md; PLAN §4 "2.0: what was built" | §4.2 (c): "About eighty lines on a small library" for the other app; "A cold process start (hundreds of ms, from memory), then a few ms"; Play "Nothing: no code is loaded" | Deferred. Spec §1: "Not chosen, so not built: … live extensions". EXTENSIONS.md: "planned as a separate, opt-in layer and is not part of this version". With it go the consent row ("Allow Summa to answer here"), global scopes by pattern, and the warm-up |
| EX2 · Catalog rows from another app | find another app's list (recent folders, lessons, tools), kept and filtered by Booklight | §4.3 `catalog(scope)` | none | Never decided |
| EX3 · Actions that run inside the other app | start something with no window (a timer, a lock) and read what it answered | §4.3 `Invoke`, `Outcome` | none | Never decided. `core/Model.kt` has `Effect.Open` only |
| EX4 · Actions on a kind of thing, from an app's file | get "Merge" on a PDF row because PDF Toolbox declared it | §4.3 `<action on="file" …>`; also `symbol` on a command | none | Never decided. `AppCommands.kt` parses `command`, `scope`, `open`, `extra` and skips the rest |
| EX5 · One row document, three transports | have a provider, a URL (A7) and a VM script (A6) all return the same rows | §4.3 | none | Never decided |
| EX6 · New row bodies | choose from options in a row (listening modes); read a script's output | §4.3 `Body.Choice`, `Body.Text`, `Icon.Owner` | none | Never decided. Not in `core/Model.kt` |
| EX7 · Another app's row as a recipe step | put `shot area` into a recipe | §4.3 | none | Never decided. `data/Recipes.kt` has no step for it |
| EX8 · Extensions being found | type `ext`; see a "Works with Booklight" mark on googlebook.studio; lint an APK's file | §4.3 "Being found" | none | **Partly built.** The window lists each app with a switch (Results, "Other apps") and `docs/EXTENSIONS.md` exists. `ext` and the lint command are not in the code; the site mark is outside this repo |
| EX9 · "Summa added 3 commands" | be told once in the footer when an app adds commands | §4.3 "Consent" | none | Never decided. No such string |
| EX10 · The caller-check library | (for developers) drop in `BooklightExtension.kt` so a provider checks who calls | §4.3 "Who may ask whom" | none | Never decided. Goes with EX1 |
| EX11 · Rows that update while shown | watch a running countdown in a row | §4.3 "Later, not now": a bound service | none | Later, by the research |
| EX12 · AppSearch index and App Functions | see what an app offers through the platform's own index | §4.3 "Later, not now"; §4.2 | none | Later, "if Google opens them" |

#### PLAN.md, the specs, PICKING-UP, use-cases.md

| # · name | The user would | Where written | Ratings as written | Status in 2.0 |
| --- | --- | --- | --- | --- |
| PL1 · Send mail without the compose window | type `send anna@x.com / text`, press Enter twice, and it is sent (his own relay, or Gmail's API) | PLAN §4 2.1 Reach; use-cases §4; ux-model §8 | use-cases: relay "0+ `INTERNET`", Play "Data safety", Effort M, Notice 2; Gmail "1 Google consent", "Google's OAuth verification", Effort L, Notice 3 | **Deferred by the owner.** PICKING-UP: "the Gmail relay (Alex: not yet)". 1.1 request: "(not the Gmail relay)". next-features: "stays deferred, as decided" |
| PL2 · Any key, plain Alt + Space included | open Booklight with Alt + Space | PLAN §4 3.0, §11.2; next-features §1 | "Accessibility key filter" | **Deferred by the owner.** PLAN §11.2: "plain Alt + Space waits for the power pack" |
| PL3 · Most-used apps under the empty field | see his top apps as a strip before typing | PLAN §4 1.0 table; 1.0 spec §2.4 | none | **Ruled out by the owner.** PLAN §11.3: "Nothing typed: nothing at all" |
| PL4 · Do Not Disturb as a switch | type `dnd 1h` and have it on | PLAN §4 2.2; ux-model §6; use-cases | use-cases: "2 notification policy access", "None found", M, 2 | **Ruled out.** 1.1 spec §12.15: "Do Not Disturb: just open the Modes panel". PLAN: "an app can only switch its own mode" |
| PL5 · Keep awake in Booklight | type `awake 2h` | PLAN §4 2.2; ux-model §6; use-cases | use-cases: "2 `WRITE_SETTINGS`", "None found", S, 2 | **Ruled out for Booklight by the owner.** 1.1 spec §12.16: "Keep awake: left to BentoBar" (see X2) |
| PL6 · Reminders Booklight rings itself | get Booklight's own notification at the exact time | PLAN §4 2.2 "exact reminders"; use-cases "Timed reminders" | use-cases: "1 `POST_NOTIFICATIONS`", None, M, 2 | **Ruled out by the owner.** 1.1 spec §12.14: "Clock alarm with a label" |
| PL7 · Contacts search | type a name and search all contacts | PLAN §6 tier 1; use-cases "People" | use-cases: "1 `READ_CONTACTS`", "From 27 Jan 2027 Play requires the picker unless broad access is needed", M, 2 | Ruled out by the research (use-cases §6: stays out "unless the picker proves too weak") |
| PL8 · Files Booklight made | find his notes files and new files by name | use-cases "Finding files"; next-features carry-over table ("Not shipped") | use-cases: 0, None, S, 2 | Never decided. No catalogue id |
| PL9 · Downloads row | type `downloads` and open Files there | use-cases; next-features carry-over table ("Not shipped") | use-cases: 0, None, S, 2 | Never decided. No catalogue id; not in the code |
| PL10 · Spotify and YouTube Music links | type `sp …`, `ytm …` | use-cases "Media" | use-cases: 0, None, S, 2 | Never decided. Not among the default links (`core/Sites.kt`) |
| PL11 · Snippets with placeholders | copy a snippet with the date or the clipboard filled in | use-cases "Automation" | use-cases: 0, None, S, 2 | Never decided. `SnipScope` copies the text as stored |
| PL12 · Phones and other devices | use Booklight on a phone; find it on Play outside Googlebooks | 1.0 spec §7; PLAN §11.11 | none | **Deferred by the owner.** PLAN §11.11: "Narrow first: it can be widened later without stranding anyone" |
| PL13 · Colours that follow what is underneath | see the panel's text and tint adapt to the window behind it | 2.0 spec §1, §2 | none | **Ruled out.** Not possible for a plain app; his answer: "Leave it out" |
| PL14 · The pin as first designed | have a pinned window with a caption, its own Copy, a colour's four forms to choose, Esc to close | 2.0 spec §4 | "`USE_PINNED_WINDOWING_LAYER` (normal; no prompt)" | **Ruled out by the device.** A pinned-layer window takes the keyboard each time the panel closes. Built as picture-in-picture; Copy and Unpin are on the `pin` row |
| PL15 · The "not planned" list | Gmail drafts and Drive results in the panel; search every file on disk; SMTP; device admin; Glowbar control; media search; screen recording; triggers (time, Wi-Fi, a corner) | PLAN §4 "Not planned"; use-cases §6 "Stays out"; next-features §2.6 | use-cases, "Everything on disk": 3, "Declaration; weak fit", L, 2 | Ruled out by the plan: restricted API scopes, Play policy, no public API; "only things the user presses start a command" |
| PL16 · Not possible for a Play app (13) | other apps' Quick Settings tiles; Device Controls; listing share targets; other apps' dynamic shortcuts; running App Functions; Chrome's bookmarks, history, tabs; moving other windows and desks; pausing the work profile; force-stop; background clipboard; the Settings search index; the phone's recent apps; a screenshot | next-features §2.11 | none | Ruled out: system-only, or no API |
| PL17 · Three things left after the 1.1 review | a monospace face for passwords; fade-outs in place of ellipses; a site's own icon in a link's chip | PICKING-UP, "1.1 released", "Left on purpose" | none | Left on purpose. 2.0 fades an app row's title; the other two are as they were |

#### On-device model (`docs/research/on-device-ai.md`; 2.0 spec §13)

| # · name | The user would | Where written | Ratings as written | Status in 2.0 |
| --- | --- | --- | --- | --- |
| AI1 · More ready-made prompts | type `friendly …`, `formal …`, `define …`, `emojify …`; have `ask` answered on the device | 2.0 spec §13 (item 1 names `friendly`); on-device-ai §4.1, §4.4, runner-up 3 | runner-up 3: "S, low value" | Never decided. Seeded are fix, shorter, de, sum, explain; `ask` still hands over to Gemini. A user can add his own |
| AI2 · An answer into Notes.md | add a summary to his notes from its row | on-device-ai §4.3 | part of feature 3, "S after feature 1" | Never decided. The answer row has Copy, Replace, Pin, Open in Gemini (`scopes/Prompts.kt`) |
| AI3 · Sums asked in words | type "what is 15 percent of 80 euros in dollars" | on-device-ai runner-up 4 | none | "Wait for the Summa extension" |
| AI4 · Reply drafts | get three short replies to a shared message | on-device-ai runner-up 5 | "M" | Never decided. Research: "Gmail and Messages already do it where it matters" |
| AI5 · Translation without the model | translate instantly through the system's translator, or with downloaded language packs | on-device-ai §2, §3.2, §4.2 | system APIs: Effort S, permission None; classic ML Kit: Effort M, "APK size ×10 or more" | System translator: ruled out by the device (spec §13: "not offered to apps (checked)"). Language packs: never decided ("only if Alex wants … and accepts its size") |
| AI6 · A model of Booklight's own | get answers on devices without AICore | on-device-ai §2, §3.3 | Effort L; permission "None. No telemetry"; "+10 MB download per device" plus a 0.3 to 3.7 GB model | Never decided. Research: "At most, later, an opt-in for devices without AICore" |
| AI7 · Answers without Google's usage reporting | use the model with the uploader stripped, or through a separate "Booklight AI" app | on-device-ai §6, ways 2 and 3 | none | **Ruled out by the owner:** "In 2.0, library as shipped" |
| AI8 · "Sounds good, not worth it" (7) | a model that answers as you type; a chat in the panel; describing a screenshot; semantic search over notes; dictation; the model doing sums; classic Smart Reply and entity extraction | on-device-ai §4, last table | none | Ruled out by the research (quota, policy, a runtime prompt, wrong answers) |

#### The 2.0 reviews and the UX model

| # · name | The user would | Where written | Ratings as written | Status in 2.0 |
| --- | --- | --- | --- | --- |
| DR1 · Type to filter an app's actions | press More and type "tl" to narrow the app's actions | reviews-2.0/ux.md C1 | none | Never decided. "worth it only if more actions are coming" |
| DR2 · Two-letter places | type `chrome tl`, `tr`, `bl`, `br` | ux.md C2 | none | Never decided. Not among the verbs in `strings_20.xml` |
| DR3 · New window in a place | open a second window of an app that is already open, in a place | ux.md C3 | none | "Not in 2.0; note it." `AppsProvider.kt`: no action has both |
| DR4 · Key caps that go down in order | see a shortcut's caps press one by one on the selected row | reviews-2.0/motion.md row 34, (b) | "Optional, cheap, only if the first three are in" | Never decided. Not in the code |
| DR5 · Two visual options | see an example's keyword as a key cap; a little space between groups of icons on an app's row | reviews-2.0/visual.md §4 | none | Never decided. The second is a fallback "if the strip still reads as a blur on the device" |
| DR6 · Make your own from the panel | press "Save as quicklink…" on a link row; type `new quicklink` or `new recipe` | ux-model §9 | none | Never decided. Not in the code: `new` knows five kinds |
| DR7 · Copy with the sum | copy "12*3.5 = 42" from an answer | ux-model §2 | none | Never decided. A sum's row has Copy and Pin (`providers/Providers.kt`) |
| DR8 · Several emoji in one go | press Shift + Enter to keep picking and add each to the copied text | ux-model §7 | none | Never decided. `OverlayModel.chosen()` copies one |
| DR9 · What you did there lately | enter a scope with nothing typed and see the last things done there | ux-model §4; 2.0 spec §11; ux.md S9 | none | **Partly built.** Emoji shows the ones picked lately. `s` and `k` show the table's first seven, not "what was opened from Booklight lately" (`providers/Keys.kt`, `Providers.kt`) |
| DR10 · Learning an action from Tab | have "chr" come to mean New window after choosing it with Tab | ux-model §12.7 | none | **Ruled out by the owner.** 1.1 spec §12.7: "Only typed verbs teach an action" |
| DR11 · Alternatives he decided against | Esc leaves a scope before closing; fields with Tab between them; editing his commands in the panel; Tab only (no Space) into a scope | ux-model §12 | none | **Ruled out by the owner.** 1.1 spec §12: "Scope: Tab or Space", "Esc closes", "One line" |

---

## 2. Partly built, and what is missing

**From the catalogue (8)**

- **L5 Layouts.** Recipes open apps in any of thirteen places. Missing: the editor drawn on a picture of the screen; a link or file per app.
- **P6 Share….** The QR code row shares its picture. Missing: Share on text, link and file rows.
- **S1 Settings rows that land on the switch.** Three of the seven named searches reach a page of their own. Missing: opening a page scrolled to the item; rows for modifier keys, Play Protect, Glowbar, Magic Pointer.
- **I5 Translate and define.** `de` or `en` translates through the on-device model. Missing: a `tr` keyword with any language; define; translation where there is no model.
- **X4 StudioSnap, X5 PDF Toolbox, X8 Welcome and another app, X9 the rest.** Booklight reads an app's commands file and its manifest shortcuts. Missing: the file in each app (one small release each); for X5 also "Send to" on a PDF (F3); for X8 the catalog route.

**From elsewhere (4)**

- **The extension contract (EX8 and the layers around it).** Built: manifest shortcuts as rows; an app's file with `command`, `scope`, `open`, `extra`; one switch for all and one per app. Missing: everything in EX1 to EX12, and the rest of layer 0 (L2, F3, F5).
- **The pin (PL14).** Built as picture-in-picture: text, answer, colour, QR code, countdown. Missing against the spec: a Copy of its own while it is small (PICKING-UP: "a pin has no Copy of its own"); choosing among a colour's four forms in the window; Enter and Esc in the pin.
- **Recent things first (DR9).** Built for emoji. Missing in `s` and `k`, where the spec asked for it, and in the other scopes. This one is not noted in PICKING-UP.
- **Left after 1.1 (PL17).** Built: an app row's title ends in a fade. Missing: a monospace face for passwords; a site's own icon in a link's chip.

Related, already counted as built: **L1** shows an app's shortcuts as rows (three under the app's own row), not as actions on its row as the catalogue also said. **L4** ("on display 2") is in the code and was never tried on a second display.

---

## 3. Known flaws and unverified things (not features)

From `docs/PICKING-UP.md`, newest entry first, unless a line names another source.

**2.0**

- The HP was offline throughout: nothing of 2.0 has run on it. Untried there: the on-device model, a pin, the shadow, the key.
- "… on display 2" was not tried on a second display.
- Not as specified: the keyword does not travel into the chip (it grows where the mark was); the pill returns to a closing row on `lead` and `trail`, not rigid.
- A letter typed right after a keyword's Space can be lost at the speed `adb shell input text` types ("fix this" became Fix + "his", seen once).
- The calculator has no decimal comma; the German examples use a point. (Fixed in 2.1.)
- Left from the motion review: a row's first action is drawn over the row fading under it for about 50 ms, with the pill's lower edge ahead of the uncovering edge; a strip whose actions change is exchanged in a fade, not slot by slot; kind labels blink along a held Down; app icons cut in.
- TalkBack was not run (also open since 1.0).
- The release build needs a keep rule for ML Kit or every prompt falls back to Gemini. The rule is in; the check (install the release build before every tag) is a standing duty.
- The model got one name wrong in a summary ("Google Books" for Googlebooks; 2.0 spec §13): its rows never run anything for that reason.
- For the Play session, not this repo: the audience change to 18 and over, the data-safety entries, the privacy page's new text.

**1.1**

- Not tested, because they need Alex's own clicks: the app icon opening the Booklight window; the notes-folder picker; the brightness switch; adding the widget and the tile; Uninstall's system dialog; a bound key on the Lenovo.
- Loose ends: the HP still holds a throwaway spike build; test leftovers on the Lenovo (a file, windows, a force-stopped Clock); the local branch `spike/unfold` still exists.

**1.0**

- The Acer Googlebook and plain Android phones are untested.
- No baseline profile.
- Tiny float leftovers show as `5.55e-17`.
- The plan page's prototype is a browser copy; the app is the reference.
- BentoBar's unbind fix (its commit 94a952c) is unreleased and untested on a device.

**Unknowns the research could not settle from a desk** (`next-features.md` §5): whether the flags for the eye dropper, the power-menu API and App Function access are on in Googlebook builds; which apps publish to the assistant's search; whether a notification listener can stay unbound while the panel is closed; Play's view of a host that hands typed text to other apps; whether Play accepts "acts on another app's request" in BentoBar's declaration; what the Quick Insert key already offers (it decides how much F8 and F9 are worth).

---

## 4. Counts

| | Total | Built in 2.0 | Not in 2.0 |
| --- | --- | --- | --- |
| The catalogue (`next-features.md` §2) | 95 | 9 | 86 |
| Written elsewhere (Table B rows) | 48 | 0 | 48 |
| **All** | **143** | **9** | **134** |

**Built (9):** L1 apps' own shortcuts, L3 more places, L4 the other display, L15 the shortcut cheat sheet, L20 `?`, F1 notes that grow, C2 pin, I1 prompts as commands, I2 rewrite, proofread and summarise on the device.

**The 86 catalogue ideas not in 2.0, by status**

| Status | Count | Which |
| --- | --- | --- |
| Partly built | 8 | L5, P6, S1, I5, X4, X5, X8, X9 |
| Not chosen for 2.0 (shown in the top fifteen, not ticked) | 11 | L8, F3, F4, F5, F8, C1, A1, D1, X1, X2, X3 |
| Deferred to the power pack (PLAN 3.0) | 6 | L13, F9, F10, P8, S6, S7 |
| Deferred by the roadmap (PLAN 2.1 Reach) | 4 | F2, P2, A4, A6 |
| Deferred to "the release after" | 1 | I4 |
| Ruled out by the research | 4 | L21, F11, A8, S9 |
| Never decided | 52 | the rest |

**The 48 rows of Table B, by status**

| Status | Count | Which |
| --- | --- | --- |
| Deferred or not chosen by the owner | 4 | EX1, PL1, PL2, PL12 |
| Ruled out (owner, device, platform or research) | 13 | PL3, PL4, PL5, PL6, PL7, PL13, PL14, PL15, PL16, AI7, AI8, DR10, DR11 |
| Partly built | 3 | EX8, PL17, DR9 (PL14 is counted under ruled out) |
| Never decided | 28 | the rest |

How Table B was counted: one row per idea as its source wrote it. Five rows group several rejected or small items and count once each: PL15 (8 items), PL16 (13), PL17 (3), AI8 (7), DR11 (4). Ideas that only repeat a catalogue idea under another name (the 3.0 power pack as a bundle, PLAN's 2.0 list of his apps, on-device-ai's runner-up 1 and feature 5) are not counted again.
