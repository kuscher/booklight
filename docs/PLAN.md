# Booklight: product plan

*1 October 2026. Status: proposal for Alex. 0.1 exists and runs on the HP Googlebook 14; nothing
below is committed until the questions at the end are answered.*

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

### 2.x: more to find and do (no or light permissions)

- **Extensions.** A small documented contract (a ContentProvider another app exports) so apps can
  offer results and actions to Booklight. First ones, in Alex's own apps: Summa (units, money with
  its rates, dates, time zones), BentoBar (start a timer, keep awake), StudioSnap (capture window
  or area), PDF Toolbox (open a tool), HearOn Link (noise control).
- **App shortcuts.** The static shortcuts apps declare ("New incognito tab", Summa's "New sheet").
- **Quick actions through public intents.** "timer 10 min", "event Friday 3pm", "email anna",
  "navigate to…".
- **Files** in folders the user grants (Downloads, Documents), recent downloads first.
- **Contacts** (asks for the contacts permission when first used).
- **Emoji and symbols, snippets** (copied to the clipboard).
- **Quick links** of your own; import a bookmarks export.
- **A desktop search pill widget** and a Quick Settings tile as extra ways in.

### 3.x: advanced, each one opt-in

- **Digital assistant role**: Action + Space and the Assistant key open Booklight. It replaces
  Gemini as the default assistant, so it is a clear choice in settings, never a default.
- **Accessibility tier**: a list of open windows, clipboard history, system commands (lock,
  screenshot, notifications), pasting a snippet into the focused field, any key combination as the
  hotkey. Needs Play's accessibility declaration, as BentoBar and StudioSnap did.
- **Usage access**: rank by what you use system-wide, not only through Booklight.
- **On-device AI** (Gemini Nano through AICore): typed requests turned into actions without an
  internet permission. To be explored once extensions exist.

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

## 11. Questions for Alex

Each has a recommended default; "Default" accepts all of them.

1. **Name.** Booklight (default), or Searchbeam, Lamplit, Glowfind, Kvick, or your own. Renaming is
   cheap until the first release.
2. **The key Booklight suggests.** Action + K (default: free on the device, and "K" is the command
   palette key people know from other tools). Free alternatives: D J M O R T X Y Z. Action + Space
   is Gemini's and only reachable through the assistant role (3.x).
3. **Nothing typed yet.** A strip of your most-used apps (default), nothing at all (Alfred), or a
   short list.
4. **Esc.** Closes at once (default, Alfred), or clears the text first and closes on the second
   press (Spotlight).
5. **Web.** No internet permission in 1.0, so no live suggestions; searches open in your browser
   with Google as the default engine and a setting to change it (default). The alternative is the
   internet permission for suggestions.
6. **Sums.** Keep the small calculator in 1.0 and bring units, money, dates and time zones in as a
   Summa extension in 2.0 (default: one engine, one set of rates, and it shows what extensions are
   for). The alternative is to copy Summa's engine into Booklight now.
7. **Extensions as the way to grow.** Other apps offer results through a small ContentProvider
   contract, starting with your own apps (default), or Booklight keeps everything built in.
8. **Advanced tiers.** Plan the assistant role for 1.x as an optional switch and the accessibility
   tier (window list, clipboard history, system commands) for 3.x (default), or sooner, or never.
9. **Repo.** Private until 0.2 is worth showing, then public (default). A public repo becomes a
   draft listing on googlebook.studio by itself.
10. **Signing key.** I make `~/.config/booklight/keystore.jks` and its backup when 0.2 is cut
    (default), the same way as the other apps.
11. **Reach.** Any Android 14+ device can install it, described as "made for Googlebook" (default,
    your usual minSdk 34), or Googlebook only (minSdk 37).
12. **Languages.** US English with British spellings, like BentoBar (default); German as well?
13. **Action + K on your HP.** I bound it during testing so you can try 0.1. Keep it (default) or
    I remove it.
14. **BentoBar.** My testing found that BentoBar 0.5 crashes whenever its accessibility service is
    unbound (see device findings). Shall I fix that in kuscher/bentobar?
