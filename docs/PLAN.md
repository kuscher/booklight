# Booklight: product plan

*1 October 2026. Status: Alex answered the questions the same day (§11) and said to build 1.0 and submit
it to Play. 1.0 is built; what follows 1.0 is in §4 and comes from [research/use-cases.md](research/use-cases.md).*

Booklight is a keyboard launcher for Googlebooks: press a key, a small glass panel appears in the
upper middle of the screen, type a few letters, press Enter. Apps first, then settings, sums and
the web, with room to grow into actions from other apps. It installs like any app and asks for no
permissions.

## 1. Why build it

Googlebook OS already has an Apps list on the Action key. Booklight is for a different habit: the
Spotlight / Alfred one, where the launcher is a panel over your work and not a place you go to.

- **It stays out of the way.** A 720 dp panel over the desktop, gone on Enter, Esc or a click
  outside. Nothing rearranges.
- **It learns your shorthand.** The text you type is tied to what you pick, so "c" can be Calendar
  for you and Chrome for someone else.
- **It answers and acts.** Sums in place, settings pages by name, web searches by keyword, and
  later actions from Summa, BentoBar, StudioSnap and other apps.
- **It keeps to itself.** No internet permission. What it learns is one file on the device.

## 2. What the research says to build first

Sources and detail: [research/launchers.md](research/launchers.md). No vendor publishes a usage
breakdown, so this ranking rests on one Microsoft survey, three academic studies and individual
usage exports. Strength is noted per row.

| Rank | What people use a launcher for | Evidence | In Booklight |
| --- | --- | --- | --- |
| 1 | Opening apps | PowerToys survey: about 65 % type an app name. One Raycast year: 46 % of 5,431 opens. Medium. | 1.0, the thing to get perfect |
| 2 | Clipboard history | Top of every "beyond launching" list; Apple and Microsoft both added it. Anecdotal. | Android blocks background clipboard reads: advanced tier only (§6) |
| 3 | Switching windows | Largest single command in one Raycast export. Weak. | Opening a running app already switches to it; a real window list needs the advanced tier |
| 4 | Sums and conversions | In every product. Weak (ubiquity). | 1.0 basic sums; Summa's engine as the first extension |
| 5 | Web search and fallbacks | Alfred's default fallback; core in QSB and ChromeOS. Weak. | 1.0: search row, typed addresses, keyword searches |
| 6 | Settings and system commands | 131 uses in one Raycast year. Weak. | 1.0 settings pages; commands that need privileges later |
| 7 | Finding files | Bergman 2008: search is 4–15 % of file retrievals; people navigate folders. Strong but old. | 2.x, behind a folder grant |
| 8 | Snippets, emoji | One user's export. Weak. | 2.x |
| 9 | Extensions | Raycast Store installs in the hundreds of thousands. Hard numbers, but installs. | 2.0: the extension contract |

Patterns every product shares, which Booklight adopts: one modifier-plus-key toggle; a panel in the
upper middle; few rows; the first row always selected and run by Enter; arrows to move; Tab or →
for more actions; one ranked list instead of groups (Alfred, Raycast, Spotlight since macOS 26);
ranking that learns from picks.

## 3. What the platform allows

Desk research: [research/android-platform.md](research/android-platform.md). Checked on the HP:
[research/device-findings.md](research/device-findings.md). The three facts the product stands on,
all verified on the device with 0.1:

1. **A hotkey without a permission.** Keyboard shortcuts → App shortcuts → Add shortcut lets the
   user bind Action + any free key to an app. It starts the app's launcher activity, so Booklight's
   launcher activity *is* the panel. Action + K opened it from another app; pressing it again closed it.
2. **A real overlay without a permission.** A see-through activity is not turned into a desktop
   window: no caption bar, the desktop stays visible, and the window can be exactly the panel with
   the system's blur behind it.
3. **Fast enough to feel instant.** 43 ms from the key to the first frame when the process is
   cached, about 260 ms cold (debug build).

## 4. Versions

### 1.0: the launcher (no permissions)

| Area | What ships |
| --- | --- |
| Summon | Action + key (guided setup that opens the system's Keyboard shortcuts), taskbar pin, Apps list. Same key closes it. |
| Apps | Every launchable app, work profile included. Prefix, word, initials ("gc"), camelCase ("snap" → StudioSnap) and loose matches. Learns per typed text, with a four-week memory. |
| Empty panel | Your most-used apps as a strip (question 3). |
| Top hit | Row one is selected; its name completes in the field as grey text; Enter runs it. |
| Actions | Tab or → lists what else a row can do: Open, App info, Uninstall, Store page; Copy for answers and links. |
| Sums | `12*3.5`, `20% of 150`, `150 + 20%`, `sqrt(2)`, `2 pi`. Enter copies. |
| Settings | About 35 system settings pages by name and by keyword ("dark", "pair", "printer"). |
| Web | A typed address opens in the browser. "Search the web" is always the last row. Keyword searches: `g`, `yt`, `w`, `maps`, `gh`, `play`, and your own. Search engine setting. |
| Look | Material 3 Expressive, the device's own colours and Google Sans Flex, light and dark, glass with blur and a solid fallback. |
| Settings window | Shortcut help, search engine and keywords, which result kinds to show, forget what it learned, about. |
| Access | Complete by keyboard and by pointer; TalkBack reads rows and the selection. |
| Privacy | No internet permission, no runtime permissions, no analytics. |

Not in 1.0 on purpose: files, contacts, clipboard history, window list, live web suggestions.

### 1.1: what was built (1 October 2026)

Alex asked for 1.1 Jot, 1.2 Dials, 1.3 Recipes and 2.2 Switches as **one release, 1.1**, on a new way of
acting on rows, and decided the open points the same day (the spec's §12:
`superpowers/specs/2026-10-01-booklight-1.1-design.md`). What differs from the table below: reminders go to
the Clock app (no exact-alarm switch); Do Not Disturb is a row that opens the Modes panel (an app can only
switch its own mode); keep awake stays with BentoBar; notes go to a folder granted once, not to
`Documents/Booklight`; `play` plays music and `store` searches the Play Store. Permissions added:
`REQUEST_DELETE_PACKAGES`, `SET_ALARM`, `WRITE_SETTINGS` (§6). Next in line: 2.0 Extensions, then 2.1 Reach
(the Gmail relay is explicitly not built yet).

### After 1.0: plenty before any permission that makes review harder

From [research/use-cases.md](research/use-cases.md): of 63 ideas, 39 need no permission, 6 an
install-time one, 8 one prompt or picker, 3 a Settings switch, and 7 need accessibility, a
notification listener, all-files access or an adb grant. So the releases are ordered by tier, and
everything in the last group is switched on together, once.

| Release | Theme | Contents | Needs |
| --- | --- | --- | --- |
| **1.1 Jot** | Capture and make | Quick email (`mail …` opens Gmail's compose filled in; closing it leaves a draft). Quick note to `Documents/Booklight/Notes.md`, or to Keep. Calendar event, reminder, timer. New file, new folder, new Doc. Ask Gemini. | `SET_ALARM` (no prompt) |
| **1.2 Dials** | Media, device, answers | Play, pause, next, volume. Settings panels. Emoji and symbols, colour values, QR, passwords and UUIDs. GitHub jump, localhost ports. | none |
| **1.3 Recipes** | Your own commands | Quicklinks with placeholders. Multi-step commands ("morning"). Open an app in a place, workspaces, a new window. Snippets, clipboard transforms, actions on selected text. A search pill widget and a Quick Settings tile. | none |
| **2.0 Extensions** | Other apps join in | The contract; Summa (units, money, dates, time zones), BentoBar (timer, keep awake), StudioSnap (capture), PDF Toolbox, VSCodeBook; static app shortcuts. | none |
| **2.1 Reach** | Network and one-time grants | Webhooks. Sending mail through the user's own relay, or Gmail once Google has verified the app. Linux commands through a small helper in the VM. Granted folders and file search. Contacts picker. On-device Gemini Nano, once tested. | a folder grant, contacts picker |
| **2.2 Switches** | Settings switches, each opt-in | Do Not Disturb, brightness, keep awake, exact reminders, the digital assistant role (Action + Space and the Assistant key, replacing Gemini there). | special-access switches |
| **3.0 Power pack** | Everything that needs the hard permissions, together | One accessibility service: lock, screenshot, notifications, a list of open windows, paste in place, clipboard history, any key combination (plain Alt + Space included). Notification listener: now playing. Optional adb grant: the real toggles. | accessibility declaration, disclosure, video |

The power pack costs one declaration, one disclosure screen and one video. It may be better as a
separate app that plugs in through the 2.0 contract, as BentoBar and StudioSnap already carry their
own accessibility services: a refused declaration would then never block a Booklight update.

Not planned: Gmail drafts or Drive results through Google's restricted API scopes, all-files access,
device admin, SMTP, Glowbar control (no public API).

## 5. Design

The full UX is in the spec, [superpowers/specs/2026-10-01-booklight-design.md](superpowers/specs/2026-10-01-booklight-design.md),
and can be tried in the interactive prototype, [design/booklight-plan.html](design/booklight-plan.html).
In short:

- One glass panel, 720 dp wide, its top edge at 20 % of the screen so the field never moves while
  the list grows down. Corners 32 dp. The desktop dims by 16 %.
- A 68 dp field with large type, then at most eight 56 dp rows, then a quiet footer of key hints.
- The selected row is a tonal pill whose corners tighten when selected; it glides between rows
  with a spring. That is the only motion that isn't instant.
- Colour from the wallpaper (Material You). Type is the device's Google Sans Flex, rounded for
  answers.
- Keys: ↑ ↓ move, Enter runs, Tab or → actions, Ctrl + 1–9 runs a row, Esc closes, the summon key
  toggles.

## 6. Permission tiers

| Tier | What the user grants | What it adds | Play friction |
| --- | --- | --- | --- |
| 0 (1.0) | Nothing | Everything in 1.0, extensions, app shortcuts, intent actions, emoji, snippets, quick links | None |
| 0 (1.1) | Nothing at install: `REQUEST_DELETE_PACKAGES` and `SET_ALARM` are granted without a prompt | Uninstall (Android confirms each one), timers, alarms, reminders through the Clock app | None: no form, no declaration |
| 1 (1.1) | A notes folder (system picker), once; "Modify system settings" (a switch in Settings) | Notes.md in that folder; brightness | No form; the switch must be the user's own clear choice and easy to undo |
| 1 | A folder (system picker); contacts (runtime prompt) | Files in that folder; people | Standard data-safety answers |
| 2 | Digital assistant role (Settings) | Action + Space, Assistant key, status-bar assistant chip | None found; user must pick it |
| 3 | Accessibility service; usage access | Window list, clipboard history, system commands, any hotkey, system-wide ranking | Accessibility declaration, prominent disclosure, video; blocked under Android 17 Advanced Protection |

Booklight never needs tier 1–3 to do its main job, and each is switched on in settings with a
plain explanation of what it reads.

## 7. Architecture

Detail in the spec. The shape that makes later versions cheap:

```
key press ─► OverlayActivity ─► OverlayModel ─► SearchEngine ─► Provider × n
                 ▲    │                             │              (apps, sums, settings,
                 │    └── Executor ◄── Action ◄── Result            commands, web, later: extensions)
                 │           │ performs Effects (launch, open, copy…)
                 └───────────┘ closes the panel
                                   History: latching + frecency, one JSON file
```

- `core/` is plain Kotlin with no Android imports: matching, ranking, learning, the calculator and
  address detection, all under JUnit. `app/` is Compose plus thin providers.
- A **Provider** returns **Results**; a Result carries **Actions**; an Action is an **Effect** as
  data, and one **Executor** performs effects. A new ability is a provider (and, rarely, a new
  effect). An extension in another app is a provider on the far side of a ContentProvider call:
  results and effects are already plain data, so they cross the process boundary as they are.

## 8. Name

Working name: **Booklight** = Google*book* + Spot*light*, and a book light is a small light you
clip on when you need it, which is the product. `github.com/booklight` is unclaimed; the largest
repo of that name is a 110-star Chrome extension (a Spotlight-style bookmark search), which is the
same idea in another place and no obstacle to an Android app.

Other names checked on GitHub on 1 October 2026 (account, and the best-known repo with that exact name):

| Name | Idea | GitHub |
| --- | --- | --- |
| **Booklight** | Googlebook + Spotlight | account free; 110★ Chrome extension |
| Searchbeam | Search box + a beam of light | account free; three tiny repos |
| Lamplit | Lit by a lamp | account taken; six tiny repos |
| Glowfind | Glowbar + find | free everywhere |
| Kvick | Swedish "quick" (Fika Labs), as in Quick Search Box | account taken; a 7★ macOS menu bar tool |
| Searchlight, Limelight, Beacon, Glint, Quickbeam, Glim, Summon | | all in real use (500–2,000★ projects) |

## 9. Shipping

- GitHub releases (`Booklight.apk`) from 0.2, listed on googlebook.studio as soon as the repo is
  public; Play closed testing under Fika Labs at 0.9 with a store kit like the other apps; 1.0 to
  Play production.
- Play data-safety is the simplest possible: nothing collected, nothing shared, no permissions.
- A signing key in `~/.config/booklight/` plus the backup, made when 0.2 is cut (question 10).

## 10. Milestones after the go-ahead

| Version | Goal | Contents |
| --- | --- | --- |
| 0.1 (done) | Prove it | Core + tests, glass panel, apps/settings/sums/web, learning, hotkey verified on the HP |
| 0.2 | Feels right | The design in full: completion in the field, suggestion strip, gliding selection, first-run shortcut guide, keyword searches, search engine setting, icon, R8 + baseline profile, release key |
| 0.3 | Actions | App actions (info, uninstall, store page), result-kind switches, static app shortcuts, intent quick actions, TalkBack pass |
| 0.9 | Ready | Store kit, privacy page, README pictures, tag-driven release workflow, Play closed testing |
| 1.0 | Ship | googlebook.studio + Play |
| 2.0 | Extensions | The contract, Summa as the first extension, then BentoBar and StudioSnap |

## 11. Decisions (Alex, 1 October 2026)

1. **Name:** Booklight.
2. **The key:** he wanted the key left of Space plus Space, with Action + K as well if several are
   possible. Checked on the HP: every custom shortcut must include the Action key (Alt + Space and
   Ctrl + Space are ignored, Action + Space is Gemini's), and an app gets one custom shortcut. So
   Booklight suggests **Action + Alt + Space**, with Action + K as the alternative; plain Alt + Space
   waits for the power pack.
3. **Nothing typed:** nothing at all, plus a small first-run card under the field.
4. **Esc:** closes at once.
5. **Web:** internet for suggestions. Built as **off until turned on** (first-run card or settings),
   because Play's User Data policy wants typed text that goes to a third party disclosed first.
6. **Sums:** the small calculator now; Summa as an extension in 2.0.
7. **Extensions** are the way to grow.
8. **Advanced tiers** as planned (now: 2.2 and 3.0 above).
9. **Repo:** private until the first release, then public.
10. **Signing key:** made on 1 October (`~/.config/booklight`, backup by the Play session).
11. **Reach:** Android 14+ (minSdk 34). On Play, `android.hardware.type.pc` is required as well, so Play offers
    it to Googlebooks only (as Alex has started doing for his other apps; the APK from GitHub installs
    anywhere). Narrow first: it can be widened later without stranding anyone.
12. **Languages:** US English, British English, German.
13. **Action + K on his HP:** kept.
14. **BentoBar:** fixed on its `main` (commit 94a952c, unreleased).

Design direction, same day: more transparency and blur; flat glass with a white outline (no 3D
highlights); motion throughout (a gliding selection, lists that cascade in); a first-run card under
the field; a G in place of the magnifier when Google is the engine.
