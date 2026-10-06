# Data safety: the facts

Written the way the app says it, for the Play data-safety answers and the privacy page.

## The short version
Booklight keeps what it learns on the device and sends nothing anywhere, with one exception that is
**off until the user turns it on**: search suggestions. With suggestions on, the text being typed is sent
to the chosen search engine to get suggested searches. Two more need a key of the user's own before anything
is sent: since 2.3 a flight number goes to AirLabs (see "Flight lookups"), and since 3.0 the name of a song
typed for Play in Spotify goes to Spotify (see "Spotify lookups"). (The library of 2.0 is another matter: see
"2.0: what changed for the form".)

## What is stored on the device
- `files/history.json`: for results the user picked, the result's id (for an app: its package and
  activity name), the text that was typed when it was picked, a count and a time. Used to put the usual
  choice first. Cleared by Settings › What Booklight keeps › Forget everything, and by uninstalling.
- `files/settings.json`: the chosen search engine, whether suggestions are on, the look, which result kinds
  show, where the first steps stand (which are done, in how many openings they stood, how often the system's
  Keyboard shortcuts dialog was opened from them, whether the app's icon has shown the panel once), and (1.1)
  what the user made: links, snippets, recipes, the emoji
  picked lately, and the address of the notes folder the user granted; and, with an event from a sentence (see that
  section), whether events are saved from the panel, the owner's address of the calendar chosen for them, and that
  the system's question for the calendars has been answered.
- (1.1) `Notes.md` in a folder the user chose with the system's folder picker: the notes typed with `note …`.
  Outside the app's storage, in the user's own files; Booklight holds a write grant for that folder only.
- (1.1) Files made with `new file …` go to the shared Documents folder; QR codes saved go to Downloads.
- The two files in the app's storage are included in the user's own Android backup and device-to-device transfer. Booklight has no
  server and no account.

## What leaves the device, and when
**Search suggestions (off by default).**
- Turned on only by the user: the question in the first steps, alone under the search field ("Suggest
  searches as you type? Booklight sends what you type to Google while you type, to suggest searches. Google
  also sees your IP address. Not sent: sums, web addresses, and anything after a keyword or with an app in
  the field." with **Not now** / **Agree**; nothing is chosen for the user, and Enter alone, Esc and waiting
  agree to nothing), or Settings › Web search › Search suggestions. Turned off in the same setting at any
  time.
- When on: after a 140 ms pause in typing, if the text is 2 to 80 characters and reads as words, Booklight
  makes one HTTPS GET request with the typed text as a query parameter to the chosen engine's suggestion
  address. Never sent, also while still being typed: sums (anything with a digit next to a sign of
  arithmetic, or starting with =), web and email addresses (anything with `://`, `@`, or digits with dots,
  colons or slashes), text without letters, keyword searches (`yt lofi`) and anything typed with an app in
  the field as the chip. The rule is `Suggest.worthAsking` in the core, with tests. An app's name followed by
  other words, typed in one go without a chip (`netflix severance`), is sent like any other text until the
  user has picked that app's own search row for such a text; from then on that app leads for words after its
  name and such a text is not sent (`History.leads`, with tests; the web's row picked twice running undoes it).
  Nor is a line that reads as an event (see "An event from a sentence": it goes to the calendar, not to a search).

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

**Flight lookups (since 2.3; only with a key of the user's own, none by default).**
- Booklight ships no key. Until the user pastes their own AirLabs key into the Booklight window (Labs ›
  Flights), a flight number is read on the device from a bundled table of airlines and nothing is sent.
- With a key in: one HTTPS GET request to `https://airlabs.co/api/v9/flight?flight_iata=<number>&api_key=<the user's key>`
  (`flight_icao=` for a callsign), 400 ms after the last key, for a strong match only (the rule is
  `Flights.read` in the core, with tests); for a weak match (`ps5`) only when the user moves to its row;
  after the keyword `flight`. Never per letter. When the flight that answers landed more than three hours
  ago, or a day was typed after the number, up to two more requests follow for the same number:
  `…/schedules?flight_iata=<number>&api_key=…` and `…/routes?flight_iata=<number>&api_key=…`.
  A pinned flight asks again while its window is on screen and it has not landed (and was not to land more
  than three hours ago): one request, `…/flight`, every 30 minutes from three hours before it leaves, every
  three hours before that, and none while it is a plan from the timetable more than ten hours off; after a
  lookup that got no answer, after 2 minutes, then 4, 8, 16, and 30. It asks about the one flight that was
  pinned: when the service answers with another day's flight, it stops.
- The request carries the flight number, the user's key, an `Accept: application/json` header and the user
  agent `Booklight`. No cookies, no advertising ID, no device identifier, no location. It reveals the device's
  IP address to AirLabs, and AirLabs can tie the lookups to the account the key belongs to.
- The reply is kept in memory for two minutes and not written to storage. AirLabs' reply repeats the request
  (the key, the caller's address); Booklight reads none of that but the count of lookups left.
- The key is `files/flights.key` in the app's storage: not in the Android backup, not in device transfer,
  never logged.
- For the form: the same data type as suggestions (App activity › In-app search history, to a third party the
  user chose, optional): the answers stay as they are. A third party's API key that the user supplies has no
  data type of its own in Play's form; whether Play wants one declared is not verified.

**Spotify lookups (only with a key of the user's own, none by default).**
- Booklight ships no key. Until the user puts their own Spotify Web API key (a client ID and its secret) into
  the Booklight window › Labs, Spotify's row has Search and no Play: Enter is an ordinary hand-over to the
  Spotify app on the device (a link of Spotify's own, aimed at its package), which shows its search results,
  and Booklight sends nothing.
- With a key in, Spotify's row has Play. Only while Spotify is the chip in the field with Play armed (never
  with Search armed), by the user's choice (Enter on Play on Spotify's own row; or `play` with Spotify named in
  the text, played in last, or the only music app that plays), and only while its row is the first row:
  400 ms after the last key, never per letter, and not for text another app handed over. Only on the user's
  Enter on Play: where `play` made Spotify the chip merely as the first of several by name, for one letter,
  and where another row stands first (`play store` puts the Play Store's row first). Where Spotify's row
  stands under another music app's after `play`: when the user moves to it. Then:
  - a token, when none is held: one HTTPS POST to `https://accounts.spotify.com/api/token` with the body
    `grant_type=client_credentials` and the header `Authorization: Basic <the user's client ID and secret>`
    (the client credentials flow: a token for the key, not for a person; no user account is read). The token is
    held in memory until a minute before it ends (an hour) and is asked for again once if Spotify refuses it early.
  - one HTTPS GET to `https://api.spotify.com/v1/search?q=<text>&type=<kinds>&limit=<5 or 10>&market=<country>`
    with the header `Authorization: Bearer <token>`. `<text>` is what was typed for Play, without a kind
    word (`album`, `artist`…) and without the player's name. After a kind word, "X by Y" is sent as
    `track:"X" artist:"Y"` (`album:"X"` for an album); free text is sent as its words, and by those fields only
    if the words found nothing. `<kinds>` is `track,artist` for free text, else `track`, `album`, `artist`
    or `playlist`. `<country>` is the country of the device's first language (two letters; `US` where it
    names none).
  - when a search by fields finds nothing, the same search once more with the words as free text; for an album,
    `https://api.spotify.com/v1/albums/<id>/tracks?limit=1&market=<country>` (its first song); for an artist,
    a search for `artist:"<name>"` with `type=track` (a song of their own to start from); for free text that
    got no answer, once more with `type=track` alone. Three requests at most for one lookup. The rules are
    `Spotify.search` and `Spotify.lookup` in the core, with tests on Spotify's real replies.
- Each request carries what is listed above, an `Accept: application/json` header and the user agent `Booklight`.
  No cookies, no advertising ID, no device identifier, no location beyond that country code. It reveals the
  device's IP address to Spotify, and Spotify can tie the lookups to the account the key belongs to.
- What was found (a name, its artists, its album, its Spotify id) is kept in memory for five minutes and not
  written to storage, with one exception the user makes: a recipe step made of a found song keeps that song's
  `spotify:` link in `files/settings.json`.
- Enter then hands the link (`spotify:track:<id>`, with `?context=…` for an album or an artist, or
  `spotify:playlist:<id>:play`) to the Spotify app on the device with `ACTION_VIEW`, aimed at its package, with
  Booklight's package name as the referrer (Spotify's guide for links asks for it). That is a hand-over on the
  device, not a transmission by Booklight.
- The key is `files/spotify.key` in the app's storage: not in the Android backup, not in device transfer,
  never logged, never shown again once it is in.
- For the form: the same data type as suggestions and flights (App activity › In-app search history, to a third
  party the user chose, optional): the answers stay as they are.

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
- **The clipboard:** its description (kind, age, what the system found) is looked at when the panel opens, for the
  line for a fresh copy; its content is read only on the user's request (Tab, Down or a click on that line; `clip`;
  `tr` or a prompt with nothing typed; a link with `{clipboard}`). It stays on the device; with the user's own
  AirLabs key in, a flight number found in it is sent to AirLabs only when the user goes to that flight's row.
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

## An event from a sentence: what changed for the form

Nothing new leaves the device through Booklight, and nothing new is collected. New, all on the device or handed
to an app the user sees open:

- **Permissions, both runtime, both the Calendar group's, both asked for only by the user's own press in the
  Booklight window:** `android.permission.READ_CALENDAR` and `android.permission.WRITE_CALENDAR`. Three rows ask:
  Privacy › Calendars › Allow… and Results › "New events go to" › Allow… ask for the first; the switch Results ›
  "Save events without opening Calendar" asks for both (the list is needed to save into one of its calendars).
  Neither has a Play declaration form. Until the user allows one, Booklight asks the system nothing of it. No
  `<queries>` line was added.
- **What is read with the first:** the system's list of calendars and nothing else, and only the calendars of
  Google accounts: for each its id, name, colour, owner's address, whether it is the account's own, how much may
  be done with it (whether events can be added), and the account's name, which is used only to tell which calendar
  is the account's own (`device/CalendarList.kt` names the seven columns). **No event is read, anywhere in the
  app.** The list is kept in memory while the app runs and is not written to storage.
- **What is written with the second:** one event, on the user's Enter or click on Save, with the switch on: its
  title, start, end, all day or not (an all-day event also as "free", as the Calendar app's own editor makes one),
  the device's time zone, the place, the calendar; and one reminder row that
  says "this calendar's default". Into the calendar the sentence names; with none named, the one chosen under "New
  events go to"; with none chosen, the account's own, else the first that takes events (core `Cals.target`). Only
  where nothing of when it is is a guess (core `Cals.saves` and `EventDraft.sure`, with tests): the day and the
  time both read from the typed sentence, the time with its half of the day, or the day with "all day", or a range
  of days (those two are saved as all-day events); no repeat asked for; no second day or time in the line;
  nothing else of the day or the time chosen by the parser (a year, "next Tuesday", which number is the month);
  the calendar's name typed whole, and one of the user's; and not a line typed without the keyword that begins
  like an everyday search (schedule, put, book, plan). Booklight never updates or deletes an event and keeps no
  record of the ones it wrote. **The saved event is then an ordinary event of the user's calendar: the account that owns the calendar
  syncs it to its own servers, as it does every event. That is the account's doing, under its terms, not a
  transmission by Booklight.**
- **Handed to a calendar app by the user's Enter (the switch off, the event a guess, or the row's "Open"):** where
  the event names no calendar, the `INSERT` request of 1.1, to whichever calendar app there is, as before. Where
  there is a calendar to name (the one the sentence names; or, with the switch on, the one the event would be
  saved to, where the row shows it), a
  link to `https://calendar.google.com/calendar/render?action=TEMPLATE` with the title, the dates, the place and
  the calendar's address (`src`: the owner's address, for most calendars an e-mail address), sent to
  `com.google.android.calendar`, Google's Calendar app, alone; where no such app takes it, the `INSERT` request
  again, without a calendar. An ordinary Android hand-over: the calendar app
  opens its editor with those fields and the user saves there, or does not. Booklight itself opens no connection
  for it.
- **The model on the device** is asked to split an event's sentence into title, time, place and calendar, where
  Booklight's own rules leave the title in pieces: after 0.7 s without a key, once for a text. The sentence stays
  on the device, as every prompt does (see "2.0: what changed for the form" for what the library itself reports).
- **Stored in `files/settings.json`:** whether the switch is on, the chosen calendar's owner's address ("New
  events go to"; empty until one is chosen; for most calendars an e-mail address), and that the system's question
  has been answered once. **That file is in the user's own Android backup and device-to-device transfer, and the
  address with it.** (That the question was answered, and that the switch is on, are believed only on the device
  they were set on: a mark beside the settings, `files/calendars.asked`, which no backup holds. Settings that
  arrive with a restore never have saving on.)
- **Suggestions:** a line that reads as an event (it begins with add, schedule, put, book or plan, in German with
  „trag … ein“, „plane“ or „neuer Termin“, and holds a day or a time; both, after schedule, put, book and plan)
  is not sent to the search engine, with suggestions on or off. Before it reads as one it is a typed
  line like any other.
- **For the data-safety answers:** no new data type. "Calendar events" is not collected and not shared: nothing of
  a calendar is transmitted off the device by Booklight. In the Console's questionnaire "Data collected" and "Data
  shared" stay "No" for Calendar events (and for everything else they are "No" for today). Should the Console's own
  check ask about the two Calendar permissions: they are used on the device only, to list the user's calendars and
  to add the one event the user saves; that answer is this section.

