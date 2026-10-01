# Launcher/search overlay research: Spotlight, Alfred, Quick Search Box and peers

*Desk research for Booklight, 1 October 2026 (web sources, listed at the end). Product conclusions are in [docs/PLAN.md](../PLAN.md).*

No vendor publishes a breakdown of what people do in these launchers. The usage ranking below rests on one Microsoft survey, three academic studies, a handful of individual usage exports and forum anecdotes; I say which is which per row.

## 1. What the evidence supports for v1

- **App launching is the core job.** In Microsoft's PowerToys survey "nearly 65%" of respondents type an application name when searching. In one Raycast Wrapped export, 2,508 of 5,431 opens (46%) were app launches.
- **File search is a last resort.** Bergman et al. (2008) found people navigate folders for 56–68% of file retrievals and search for only 4–15%; better search engines did not change this. Alfred keeps files out of default results by design.
- **Clipboard history is the most-cited second feature.** It tops the anecdotes, Apple added it to Spotlight in Tahoe, and Microsoft's Command Palette ships it. The evidence is qualitative.
- **Per-query learning is universal.** Alfred, Raycast, Android QSB, QSB for Mac, PowerToys Run and ChromeOS all re-rank on what you picked.
- **Android constraint:** app shortcuts are only readable by the default launcher (or the active voice-interaction service), so an overlay that is not the home app loses them.

## 2. Apple Spotlight

**Architecture**
- `mds` (root daemon) owns a hidden `.Spotlight-V100` store on each volume; `mds_stores` manages the database.
- FSEvents reports file changes; `mdworker` processes check the file type (UTI) and load the matching `.mdimporter` plug-in to extract metadata and text.
- Importers live in `/System/Library/Spotlight`, `/Library/Spotlight` and inside app bundles. The index is inverted (terms to postings).
- Clients query via `NSMetadataQuery` or `mdfind`.
- Core Spotlight lets apps donate their own items (`CSSearchableItem`, `NSUserActivity`).
- Ranking (WWDC 2017): an on-device Core ML ranker, "personalized and adaptive". Match quality and usage are the main signals; `rankingHint` (1–100) only orders items within one app; `lastUsedDate` and engagement feed it.

**Milestones**
- Announced June 2004, shipped in 10.4 Tiger (April 2005) as a menu-bar dropdown.
- 10.5 added calculator, dictionary, Boolean operators and Quick Look.
- 10.10 Yosemite moved it to a centred panel with results on the left and a preview pane on the right, plus web sources (Wikipedia, Maps, Bing, stores) and unit/currency conversion.
- macOS 26 Tahoe (Apple calls it the biggest update ever):
  - Results are "listed together and ranked intelligently" rather than grouped, with filters (for example PDFs or Mail messages).
  - Hundreds of actions via App Intents, open to third parties.
  - Quick keys: short strings such as `sm` (send message) or `ar` (add reminder).
  - Browse views on Cmd+1 to Cmd+4: Applications, Files, Actions, Clipboard. Launchpad was replaced by the Apps view.
  - Clipboard history kept 8 hours at launch; 26.1 added 30 minutes / 8 hours / 7 days and a clear button.

**Keyboard (Apple support page)**
- Cmd+Space toggles; Return opens; Space opens Quick Look.
- Cmd+R reveals in Finder; holding Cmd shows the path.
- Tab after an app name searches inside that app; `/` filters; Up arrow recalls search history.
- The top hit is inline-autocompleted in the field.

**Features:** core = apps, files, settings, calculator/conversions, dictionary, web suggestions. Advanced = actions, quick keys, clipboard, in-app search.
**Extensibility:** mdimporters, Core Spotlight, App Intents.

## 3. Alfred (Running with Crayons, 2010)

**Architecture and model**
- Proprietary native Mac app. Files come from the macOS metadata index; Alfred adds its own "knowledge".
- Default results are deliberately narrow: Applications, Contacts and Preferences. The docs advise "only choose the essential file types".
- Everything else is keyword-triggered:
  - Files: `open` (or a leading space), `find`, `in`; `/` and `~` browse the file system.
  - Other: `define`, `spell`, `=` for advanced calculation, `snip`, web-search keywords such as `maps`, system keywords such as `sleep`, `lock`, `emptytrash`.
- Fallback searches (Google, Wikipedia, Amazon) appear when nothing local matches.

**Ranking**
- A rolling four-week usage "fingerprint".
- Keyword latching: the exact typed string is bound to the picked result. Picking Calculator for `c` two or three times makes it the top result for `c`, while `ca` can still map to Calendar.
- With no knowledge, it falls back to the metadata fields Last Used and Last Modified.

**Free vs Powerpack (£34 single, £59 Mega Supporter)**

| Free (core) | Powerpack (advanced) |
|---|---|
| App launch, default results | Clipboard history (off by default "for privacy reasons"; 24 h to 3 months retention; ignores password managers) |
| Web and custom searches (`{query}`) | Snippets and auto-expansion |
| Calculator, dictionary | Workflows (node graph of triggers, inputs, actions, outputs; Script Filters; Alfred Gallery) |
| System commands | Custom fallback searches, File Filters, file buffer and richer file navigation |
| Built-in themes | Contacts viewer, 1Password, music mini player, URL history, sync, custom themes |

**Appearance and hotkey**
- Default hotkey Option+Space.
- Position set on a screen grid, with a choice of display.
- At most 9 visible rows, with optional Cmd+1 to 9 labels.
- Runs as a non-activating panel by default ("Compatibility" mode is the alternative).

**Keys:** Return acts; Cmd+Return reveals; Ctrl+Return web-searches; Right arrow opens Universal Actions ("over 60"); Shift or Cmd+Y previews; Tab autocompletes; Esc backs out; Up arrow recalls the last 20 queries.

**Usage tab:** all-time total, a 28-day graph and a per-feature split (General, Expansion, Clipboard, Navigation, Action, iTunes, Remote, Hotkeys). The data is local and is not published in aggregate.

## 4. Google Quick Search Box

### (a) QSB for Mac (2009)
- Developer preview 12 January 2009, launch 9 June 2009, last build August 2010. Apache 2.0, Objective-C, led by Quicksilver author Nicholas Jitkoff.
- Default hotkey Ctrl+Space, or double-tap Cmd.
- Google's framing: "contextual search, actions, and extensibility".
- Pivot model: select a result, press Tab or Right arrow, and get actions or a search inside it.

**Vermilion framework (read from the source headers)**
- `HGSSearchSource`: `isValidSourceForQuery:`, `searchOperationForQuery:`, declared pivotable types, `promoteResult:`.
- `HGSAction`: `appliesToResult(s):`, direct-object types, `nextArgumentToFillIn:`, and can return results for chaining.
- `HGSResult`: typed, with `rankFlags`, `lastUsedDate`, a default action and `promote`.
- `HGSMixer` merges sources. `HGSSearchSourceRanker` tracks `averageTimeForSource:` and `promotionCountForSource:`, so sources are ordered by speed and by how often their results get picked.
- Python and AppleScript plug-ins are supported.
- Bundled modules include Apps, SpotlightFiles, Calculator, Clipboard, Contacts, Dictionary, WebBookmarks, NavSuggest, RecentDocuments, Shortcuts, Weather, Google Docs/Calendar/Bookmarks/Picasa, iTunes and Terminal.

### (b) Android QSB / SearchManager (Android 1.6, 2009)
- An app declares `searchable.xml` with `android:includeInGlobalSearch="true"`, a `searchSuggestAuthority` and a `searchSettingsDescription`, and exposes a suggestions ContentProvider.
- Sources are off by default; the user enables them under Settings > Search > Searchable items.
- New sources start under "more results" and rise as their suggestions are chosen.
- Clicked suggestions become shortcuts, shown immediately without querying providers and refreshed through a shortcut id.

**What happened to it**
- July 2012: local search was removed from the Galaxy Nexus and Galaxy S III after Apple's unified-search patent injunction.
- Android 4.1 folded QSB into the Google Search app / Google Now (search-result summary only; the Wikipedia page I fetched did not confirm it).
- 2016: Firebase App Indexing ("In Apps" personal content) became the route into Google's on-device index.
- Android 12: AppSearch plus Pixel Launcher device search (apps, shortcuts, people, settings, tips).
- December 2025: Pixel Launcher device search was replaced by the Google app's search, losing contacts lookup; 9to5Google reports it as intentional.
- The current developer docs still describe `includeInGlobalSearch` with no deprecation note. I could not confirm that any shipping surface still consumes it.

### (c) ChromeOS Launcher
- Opened with the Search/Launcher ("Everything") key. Since ChromeOS 100 it is a floating pane on the left, not full screen.
- Results sit under category headers and include inline answers (weather, maths) and open-tab search.
- The zero state has a "Continue where you left off" section (recent Docs, Drive and local files), which can be hidden.
- Provider code in the Chromium tree: app search, app zero-state, ARC, omnibox, files, OS settings, help app, keyboard shortcuts, personalization, system info, local image search, desk templates.
- Rankers: score normalising, FTRL, MRFU (usage), best match, answer, continue, filtering, removed results.
- A `BurnInController` holds results briefly "to reduce the UI effect of results popping in".

## 5. Secondary products

- **Raycast:** native app; extensions are React/TypeScript in a Node child process with one worker per extension, rendered natively over JSON-RPC. Ranking order: exact alias, alias prefix, fuzzy title, keywords, then frecency. Cmd+K opens the action panel. Clipboard history is free.
- **Quicksilver (2003, Jitkoff):** three-pane object, action, argument grammar over a background catalog, with global triggers and learned abbreviations. Open source since 2007.
- **LaunchBar:** adaptive abbreviation search; Instant Send passes the current selection to an action list.
- **PowerToys Run:** Alt+Space, Wox-derived plug-ins, each with a prefix (`=` calc, `?` files, `<` windows, `$` settings, `>` shell, `??` web). "Selected item weight" tunes how fast picks rise.
- **Command Palette:** the successor to Run, on Win+Alt+Space. Adds a home page with pins and recents, clipboard history, a window switcher and an extension gallery.
- **KRunner:** runner plug-ins, including out-of-process D-Bus runners exposing `Match()`, `Run()` and `Actions()`.
- **Ulauncher:** Python/GTK, fuzzy matching, remembers previous picks, Python extensions.
- **Kvaesitso:** searches apps, shortcuts, contacts, calendar, files, places, calculator and unit conversion. Uses `READ_CONTACTS`, `READ_CALENDAR`, `READ_EXTERNAL_STORAGE` and notification access (badges, media). App-shortcut search only works when it is the default launcher.
- **Sesame Search:** learns from picks, matches word initials, integrates with Nova. Reported to need accessibility plus about 21 permissions (secondary sources only).
- **Niagara:** searches apps, contacts and web links; needs notification access and an accessibility service for some features.
- **Pixel Launcher:** used the system AppSearch index. Third-party access to that index is visibility-gated; I did not pin down the exact permission.

## 6. Feature-usage ranking

| # | Feature | Evidence | Strength |
|---|---|---|---|
| 1 | App launching | PowerToys survey: ~65% type an app name. Raycast Wrapped (one user, 2025): 2,508 launches in 5,431 opens. Most-mentioned use in an HN thread ("3 key strokes"). Alfred's defaults centre on it. | Medium: one vendor survey (developer audience, no sample size) plus individual data |
| 2 | Clipboard history | Listed first in an HN "most used beyond launching" comment and a top-four action for a 17k-opens/year Raycast user. Wrapped samples: 106/year for one user, and about 2.5/day for another (search snippet only). Added by Apple and Microsoft; struck from the PowerToys v1 spec. | Weak to medium: consistent anecdotes, no aggregate |
| 3 | Window switching / management | 1,924 uses/year in one Wrapped (the largest command, probably hotkey-driven). Priority 2 in the PowerToys spec. | Weak: one user |
| 4 | Calculator and conversions | Present in every product; priority 3 in the PowerToys spec. | Weak: ubiquity only |
| 5 | Web search / fallback | Alfred's default fallback, a QSB core source, the ChromeOS omnibox provider. | Weak: design evidence only |
| 6 | Settings and system commands | 131/year in one Wrapped; ~70% of the PowerToys survey wanted "run as administrator". | Weak |
| 7 | File search | Bergman 2008: search is 4–15% of file retrievals. Stuff I've Seen (234 users): opened items were 76% email, 14% web, 10% files; queries averaged 1.6 words; 25% included a person's name; 47% of opened items were under a month old. | Strong, but dated (2003–2008) |
| 8 | Snippets, emoji | Wrapped: snippets 102, emoji 296 per year. | Weak: one user |
| 9 | Extensions | Raycast Store download counts: Kill Process 767,613; Color Picker 608,706; Google Chrome 565,655; Google Translate 492,651; Spotify 485,621. | Hard vendor numbers, but installs, not use |

**Invocation frequency (self-reported)**
- Alfred: the docs' example shows 85 uses/day; forum posts report 41.8 and 120 per day (search snippet; the thread itself returned 403).
- Raycast: 5,431, "over 8,000" and 17,000 opens per year.

**Supporting research**
- AccessRank (CHI 2012): combining recency, frequency, time-of-day and list stability predicts revisits better than either signal alone.
- Böhmer et al. (2011) logged over 4,100 Android users. The claim that launch probabilities follow Zipf's law is attributed to it in secondary citations; I could not read the paper to confirm.

**No numbers exist for:**
- Aggregate Alfred usage.
- Aggregate Raycast Wrapped totals.
- PowerToys Run telemetry results (the events are defined; results are unpublished).
- ChromeOS launcher query mix.
- Apple Spotlight usage.

## 7. UX patterns common to all

- **Hotkey:** one global modifier+Space chord toggles the panel (Cmd, Option, Ctrl, Alt or Win+Alt), and it is rebindable. ChromeOS uses a dedicated key.
- **Layout:** a centred panel in the upper part of the screen, on the display with the cursor or focus. Spotlight opens in the top third and can be dragged. PowerToys specified "center of the screen". ChromeOS is the exception, anchored left.
- **Rows:** few. Alfred caps at 9; the PowerToys spec defaulted to 4.
- **Top hit:** the first row is always pre-selected and Return runs it. Spotlight also autocompletes it inline. PowerToys has a setting to wait for slow plug-ins before selecting, to avoid jumpiness.
- **Navigation:** Up/Down moves; Esc closes or steps back. Cmd/Ctrl+number picks a row directly in Alfred and in the PowerToys spec.
- **Secondary actions:** Tab or Right arrow pivots into actions or in-item search (QSB Mac, Alfred, Spotlight's Tab-into-app). Raycast uses Cmd+K. Modified Return gives alternates such as reveal, web search or run as administrator.
- **Grouping:** older designs group by category (Yosemite Spotlight, ChromeOS). Tahoe, Alfred, Raycast and PowerToys use one ranked list, with prefixes or filters for scoping.
- **Zero state:**
  - Alfred shows nothing (Up arrow recalls history).
  - Raycast shows favourites, recent files and today's events.
  - Command Palette shows a home page with pins and recents.
  - ChromeOS shows Continue plus apps.
  - Tahoe offers browse views.
- **Learning:** pick counts are bound to the typed string (Alfred latching, Raycast frecency, QSB shortcuts and promotion counts, PowerToys selected-item weight, ChromeOS FTRL/MRFU).
- **Latency:** nobody publishes a millisecond target. PowerToys' goal is "faster than start menu/Win+S", and it measures cold and warm hotkey-to-visible time and per-query time. Android QSB shows cached shortcuts before providers answer; QSB Mac orders sources by measured speed.

## 8. Not verified or rejected

- **Panel sizes:** the Spotlight panel width is unverified. Raycast's 750×474 comes from a single gist.
- **Core Spotlight version:** which OS release introduced it was not checked.
- **`LauncherApps.getActivityList`:** whether it works without the home role on Android 17 is unverified.
- **Later Spotlight changes:** Wikipedia lists a macOS 27 "Golden Gate" Spotlight with Siri integration; not cross-checked.
- **Rejected statistics:**
  - A "UXPA 2024 survey of 1,243 macOS users, 68% never use Spotlight for calculations" claim, which I could not trace beyond a content-farm page.
  - TechLila's Raycast statistics, which are unsourced.
  - A "1 million Raycast users, January 2026" claim, which appeared only in a search summary.
- **Fetch failures:** AppleInsider's Tahoe guide, one Medium Wrapped post and the Alfred forum thread returned 403. The PowerToys wiki page now redirects, so I cloned the wiki and read `Launcher.md` directly.

## 9. Sources

**Spotlight**
- https://eclecticlight.co/2021/01/28/spotlight-on-search-how-spotlight-works/
- https://en.wikipedia.org/wiki/Spotlight_(Apple)
- https://asciiwwdc.com/2017/sessions/231
- https://www.apple.com/newsroom/2025/06/macos-tahoe-26-makes-the-mac-more-capable-productive-and-intelligent-than-ever/
- https://9to5mac.com/2025/06/10/macos-26-spotlight-gets-actions-clipboard-manager-custom-shortcuts/
- https://support.apple.com/guide/mac-help/spotlight-keyboard-shortcuts-mh26783/mac
- https://www.macrumors.com/2025/11/04/more-spotlight-clipboard-settings-macos-26-1/
- https://www.macworld.com/article/225438/how-to-use-spotlight-in-yosemite-to-search-for-files-apps-web-info-and-more.html
- https://www.techradar.com/how-to/software/os-x-yosemite-spotlight-7-handy-search-shortcuts-1282854
- https://www.laptopmag.com/how-to/move-the-spotlight-search-bar-on-macos

**Alfred**
- https://www.alfredapp.com/help/features/default-results/
- https://www.alfredapp.com/help/features/default-results/fallback-searches/
- https://www.alfredapp.com/help/kb/understanding-result-ordering/
- https://www.alfredapp.com/powerpack/
- https://www.alfredapp.com/help/features/clipboard/
- https://www.alfredapp.com/help/features/file-search/
- https://www.alfredapp.com/help/features/web-search/
- https://www.alfredapp.com/help/features/system/
- https://www.alfredapp.com/help/features/universal-actions/
- https://www.alfredapp.com/help/workflows/
- https://www.alfredapp.com/help/appearance/
- https://www.alfredapp.com/help/advanced/
- https://www.alfredapp.com/help/getting-started/cheatsheet/
- https://www.alfredapp.com/help/usage/
- https://www.alfredforum.com/topic/2301-post-your-usage-stats/
- https://en.wikipedia.org/wiki/Alfred_(software)

**Quick Search Box (Mac and Android)**
- https://en.wikipedia.org/wiki/Google_Quick_Search_Box
- http://googlemac.blogspot.com/2009/06/introducing-google-quick-search-box.html
- https://www.macworld.com/article/198537/google_quick_search_box_released.html
- https://github.com/rcarmo/qsb-mac (headers under `Vermilion/Vermilion/`)
- https://android-developers.googleblog.com/2009/09/introducing-quick-search-box-for.html
- https://developer.android.com/develop/ui/views/search/searchable-config
- https://www.pcworld.com/article/460164/samsung_removes_local_search_feature_from_galaxy_s_iii_as_patent_suit_nears.html
- https://firebase.blog/posts/2016/11/firebase-app-indexing-for-personal-content/
- https://9to5google.com/2021/09/08/android-12-beta-5-device-search-now-live-in-pixel-launcher-app-drawer/
- https://www.xda-developers.com/android-12-device-search-api-third-party-launchers/
- https://9to5google.com/2025/12/13/pixel-launcher-device-search/
- https://developer.android.com/reference/kotlin/android/content/pm/LauncherApps

**ChromeOS Launcher**
- https://chromium.googlesource.com/chromium/src/+/HEAD/chrome/browser/ash/app_list/search/README.md
- https://github.com/chromium/chromium/tree/main/chrome/browser/ash/app_list/search
- https://9to5google.com/2022/03/30/chrome-os-100/
- https://chromeunboxed.com/productivity-launcher-categories-update/
- https://chromeunboxed.com/chromeos-launcher-hide-recents-update
- https://support.google.com/chromebook/answer/15470775

**Secondary products**
- https://manual.raycast.com/search-bar
- https://www.raycast.com/blog/how-raycast-api-extensions-work
- https://www.raycast.com/store/popular
- https://en.wikipedia.org/wiki/Quicksilver_(software)
- https://thesweetsetup.com/apps/best-app-keyboard-launcher-mac/
- https://learn.microsoft.com/en-us/windows/powertoys/run
- https://learn.microsoft.com/en-us/windows/powertoys/command-palette/overview
- https://develop.kde.org/docs/plasma/krunner/
- https://docs.ulauncher.io/
- https://f-droid.org/packages/de.mm20.launcher2.release/
- https://github.com/MM2-0/Kvaesitso/discussions/1428
- https://sesame.ninja/
- https://help.niagaralauncher.app/article/31-notifications-not-showing

**Usage evidence**
- https://github.com/microsoft/PowerToys/wiki/Launcher (cloned from `PowerToys.wiki.git`)
- https://github.com/microsoft/PowerToys/blob/main/doc/devdocs/modules/launcher/telemetry.md
- https://adamlevoy.com/blog/raycast-wrapped-2025/
- https://rmoff.net/2025/12/18/a-love-letter-to-raycast/
- https://x.com/wesbos/status/1868686963806736478
- https://medium.com/the-mac-alchemist/my-raycast-wrapped-2025-a-data-driven-roast-0bab8db848f2
- https://news.ycombinator.com/item?id=24393137
- https://news.ycombinator.com/item?id=43201715
- https://www.researchgate.net/publication/220515581_Improved_Search_Engines_and_Navigation_Preference_in_Personal_Information_Management
- http://susandumais.com/SIGIR2010-DesktopWorkshop-Dumais.pdf
- https://www.csse.canterbury.ac.nz/andrew.cockburn/papers/AccessRank-camera.pdf
- https://brenthecht.com/publications/bhecht_mobilehci2011_sleepbirds.pdf
