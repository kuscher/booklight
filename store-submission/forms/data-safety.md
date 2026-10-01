# Data safety: the facts

Written the way the app says it, for the Play data-safety answers and the privacy page.

## The short version
Booklight keeps what it learns on the device and sends nothing anywhere, with one exception that is
**off until the user turns it on**: search suggestions. With suggestions on, the text being typed is sent
to the chosen search engine to get suggested searches.

## What is stored on the device
- `files/history.json`: for results the user picked, the result's id (for an app: its package and
  activity name), the text that was typed when it was picked, a count and a time. Used to put the usual
  choice first. Cleared by Settings › What Booklight keeps › Forget everything, and by uninstalling.
- `files/settings.json`: the chosen search engine, whether suggestions are on, the look, which result kinds
  show, which first-run cards were seen, and (1.1) what the user made: links, snippets, recipes, the emoji
  picked lately, and the address of the notes folder the user granted.
- (1.1) `Notes.md` in a folder the user chose with the system's folder picker: the notes typed with `note …`.
  Outside the app's storage, in the user's own files; Booklight holds a write grant for that folder only.
- (1.1) Files made with `new file …` go to the shared Documents folder; QR codes saved go to Downloads.
- The two files in the app's storage are included in the user's own Android backup and device-to-device transfer. Booklight has no
  server and no account.

## What leaves the device, and when
**Search suggestions (off by default).**
- Turned on only by the user: the first-run card under the search field ("Search suggestions are off.
  Turn them on and what you type is sent to Google to suggest searches. Sums and web addresses are not
  sent." with **Turn on** / **Not now**), or Settings › Web search › Search suggestions. Turned off in
  the same setting at any time.
- When on: after a 140 ms pause in typing, if the text is 2 to 80 characters and reads as words, Booklight
  makes one HTTPS GET request with the typed text as a query parameter to the chosen engine's suggestion
  address. Never sent, also while still being typed: sums (anything with a digit next to a sign of
  arithmetic, or starting with =), web and email addresses (anything with `://`, `@`, or digits with dots,
  colons or slashes), text without letters, and keyword searches (`yt lofi`). The rule is
  `Suggest.worthAsking` in the core, with tests.

  | Engine | Address |
  | --- | --- |
  | Google (default) | `https://suggestqueries.google.com/complete/search?client=firefox&ie=utf-8&oe=utf-8&q=<text>` |
  | DuckDuckGo | `https://duckduckgo.com/ac/?type=list&q=<text>` |
  | Bing | `https://api.bing.com/osjson.aspx?query=<text>` |
  | Brave Search | `https://search.brave.com/api/suggest?q=<text>` |
  | Ecosia | `https://ac.ecosia.org/autocomplete?type=list&q=<text>` |

- The request carries the typed text, an `Accept: application/json` header and the user agent
  `Booklight` (not Android's default, which names the device model). No cookies, no account, no advertising ID, no device identifier, no location. Like any
  internet request it reveals the device's IP address to that search engine.
- The reply (a list of suggested searches) is shown as rows and kept in memory for the session
  (64 entries at most). It is not written to storage.
- Booklight's developer receives nothing: there is no Booklight server.

**Choosing a web row.** "Search Google for …", a keyword search, a suggestion or a typed address opens
that address in the user's browser. That is the user sending it, in their browser, not Booklight in the
background.

**Nothing else.** No analytics, no crash reporting, no ads, no third-party SDKs.

## For the form
- Data collected by the developer: none.
- Data shared with third parties: with suggestions on, the text typed into the search field (Play's
  "App activity › In-app search history") is sent to the search engine the user chose, to provide the
  suggestions feature. Optional (off unless the user turns it on); not used for advertising or
  analytics by Booklight; transferred over HTTPS; not stored by Booklight.
- Data encrypted in transit: yes. Deletion request: nothing is held by the developer.

**As filed in the Play Console (1 October 2026).** App activity › In-app search history is declared as
both **collected** and **shared**. That is not a mistake: Play's form defines "collected" as data
transmitted off the device "to you or a third party", so declaring sharing alone would under-declare, even
though the developer receives nothing. Optional (users choose); not marked as processed ephemerally
(Booklight can't vouch for what a search engine keeps); purpose App functionality for both; encrypted in
transit; no account. The optional deletion-request question is left unanswered: the developer holds nothing.
Don't "correct" the form to "not collected".

## 1.1: what changed for the form

Nothing new leaves the device through Booklight. New in 1.1, all on the device or handed to an app the
user sees open:

- **Permissions:** `REQUEST_DELETE_PACKAGES` (the Uninstall action; Android shows its own confirmation),
  `com.android.alarm.permission.SET_ALARM` (timers and alarms through the Clock app), `WRITE_SETTINGS`
  (brightness; inert until the user turns on "Modify system settings" for Booklight). None has a Play
  declaration form. No runtime permission, no accessibility service, no foreground service.
- **Handed to other apps by the user's Enter:** a mail draft (`mailto:`), a calendar event (`INSERT`), a
  note for Keep (`CREATE_NOTE`), a timer or alarm (`SET_TIMER`, `SET_ALARM`), text for Gemini (`SEND` to the
  Gemini app, shown in its prompt, not sent), "play this" (`MEDIA_PLAY_FROM_SEARCH`). These are ordinary
  Android hand-overs, not collection or sharing by Booklight.
- **Received from other apps:** selected or shared plain text (`PROCESS_TEXT`, `SEND`). Shown in the panel,
  not stored, not sent.
- **The clipboard:** read only on the user's request (`clip`, or a link with `{clipboard}`).
- **Suggestions:** unchanged, and still off by default. Text typed after a keyword (inside a chip) is never sent.

## 2.0: what changed for the form

**One library now sends data off the device: Google's ML Kit (GenAI Prompt API), shipped as it is.**
Alex's decision of 1 October 2026 ("In 2.0, library as shipped"). The on-device model's input and output do
not leave the device; the library's usage metrics do.

- **What it sends, to Google, in the background** (ML Kit's own disclosure, which applies to every ML Kit
  library: https://developers.google.com/ml-kit/android-data-disclosure): device information (model, OS
  build), application information (package name, version), a per-installation identifier, performance
  metrics (latency), sizes of input and output, API configuration, event types, error codes. Not the text
  that is typed and not the answer.
- **How:** the library's `datatransport` components (`TransportBackendDiscovery`, `JobInfoSchedulerService`,
  `AlarmManagerSchedulerBroadcastReceiver`) upload over HTTPS when a job runs. Checked on the Lenovo: the
  model answers without them, and Booklight could remove them in a later version.
- **For the form** (what Google's disclosure page tells apps that use ML Kit to declare): **Device or other
  IDs** and **App info and performance › Diagnostics**: collected, not shared, not optional (a user who never
  uses a prompt still has the library in the app), purpose Analytics, encrypted in transit, no deletion
  request mechanism at the developer (Google holds it, per installation, not per account). "No third-party
  SDKs" above is no longer true: say "one, Google's ML Kit".
- **New permissions, both from the library, both install-time without a prompt:**
  `com.google.android.apps.aicore.service.BIND_SERVICE`, `android.permission.ACCESS_NETWORK_STATE`. A
  `<queries>` entry for `com.google.android.aicore`.
- **Audience.** Google's terms for the GenAI APIs exclude apps directed at, or likely to be used by, people
  under 18. The target audience moves from 13 and over to **18 and over** (Alex, 1 October 2026).
- **The pinned window** is picture-in-picture: no permission (`USE_PINNED_WINDOWING_LAYER` is not requested).
- **Other apps' commands:** read from installed packages' manifests and resources under the existing
  `<queries>` for launcher activities; nothing leaves the device.
- **Notes:** the granted folder is now read as well as written (`notes`, `todo`), on the device.
- **Suggestions:** unchanged, still off by default; text typed inside a chip (a prompt's included) is never
  sent to the search engine.

