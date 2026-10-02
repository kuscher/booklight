# Booklight: telling other apps what to do (intents)

*Desk research, 2 October 2026. One short look at a device followed it (see "Asked of the test Googlebook"); apart from that nothing here was tried on a device. No code was changed, nothing was built.*

**The question.** The owner wants commands that make other apps and the system do a specific thing: "I want to
tell Spotify to play a certain song. Gemini does this through intents I think, so could we construct intents to
send other apps?" What can one Android app tell another to do, on Android 14 to 17, with no new permission, no
accessibility service and nothing of Booklight's running in the background?

**How it was done, and its limit.** The session's web-search budget was already used up, so nothing here comes
from a search engine. Official pages were opened directly and read (the list is at the end). The Android source
(`android17-release`) and three apps' published manifests were downloaded and read. Five Stack Overflow threads
were read through Stack Overflow's own API. Everything else is marked as memory.

**How sure each claim is.** *docs* = read today on an official Android or Google page. *vendor* = read today on
the app maker's own page. *source* = read today in the Android source or in an app's published manifest.
*community* = a Stack Overflow thread, with its year. *repo* = already in this repository (some of it was checked
on a device earlier; `device-findings.md` says what). *memory* = what I know from before, not confirmed today.
**not verified** = needs a check on a device before anything is promised.

---

## The short version

Yes: Booklight can construct intents and send them to other apps, and it already does (mail, events, timers,
`play`). No permission is needed to start another app's activity, and Booklight is in front when it does it, so
Android lets it.

But an intent is a request, not a remote control. The other app decides what to do with it, nothing comes back,
and most apps only promise to **open** a thing, not to **do** it. For the owner's example:

- **"Play this in Spotify" by a typed name:** Android has a standard request for it
  (`MEDIA_PLAY_FROM_SEARCH`). Booklight's `play` already sends it, to whichever player answers. It can be aimed
  at Spotify by naming its package. What Spotify then does is Spotify's choice. The reports disagree: one says an
  artist search plays at once (2015), one says it "does a search but does not play", even with Premium (2020).
  Spotify documents nothing about it. **Not verified.**
- **A specific song for certain** needs Spotify's own SDK or Web API: a login, a client id registered with
  Spotify, and, in Spotify's words, "A Spotify Premium account is required to play a single track uri". Spotify
  also limits an unreviewed app to five named users, and since May 2025 only reviews organisations. So this
  cannot be shipped to everyone. It could work for the owner alone, with his own client id.
- **Gemini does not do it with a plain intent.** Google's help page says Spotify must be linked to the Google
  Account, and that it plays "specific media ... only if you have a subscription with them that supports
  on-demand playback". Pause and next go through Gemini's device access (notification access, the assistant
  role). Booklight has none of those, by the owner's rule.

### What each road gives

| Road | What Booklight could do | What it needs | Sure |
| --- | --- | --- | --- |
| **Standard actions** (`MEDIA_PLAY_FROM_SEARCH`, `SET_TIMER`, `INSERT`, `DIAL`, `SENDTO`, Settings actions) | Ask an app of a kind to do a standard thing: play from a search, set a timer, open an editor filled in, open a settings page | Nothing | docs for the request; the answer is each app's |
| **Links** (`ACTION_VIEW` with `https:` or an app's own scheme, aimed at a package) | Land inside an app: a search, a track, a chat with the text filled in, directions, a new document | Nothing | docs or vendor for the formats; landing spots **not verified** |
| **What apps declare** (manifest shortcuts, `SEARCH` filters, the assistant's capabilities in `shortcuts.xml`) | Offer what an app itself published, with no table to maintain | Nothing. Only exported targets can be started | docs; how many apps declare any is **not verified** |
| **A media session through the app's media browser service** | `playFromSearch`, pause, next, aimed at one app, without opening its window | Nothing, if the app lets an unknown client connect. Many check who calls | docs for the mechanism; per app **not verified** |
| **The service's own API with a login** (Spotify SDK and Web API) | Search, then play exactly that track | A login, a client id, Premium; five users at most | vendor |
| **Notification access** (later tier) | See what is playing, control and play-from-search in any active player | A Settings switch, and a service the system keeps bound | docs, source |
| **Assistant role with a voice interaction service** (later tier) | Start every app's shortcuts, the dynamic and the non-exported ones too | Replaces Gemini on its key; a service of Booklight's | docs, repo |
| **Accessibility** | Press buttons in other apps | Ruled out by the owner | |

---

## 1. How one app starts another

**Three ways to address an intent.**
- *Implicit:* an action, maybe data and extras, no receiver named. The system picks the app, or asks. Example:
  Booklight's `play` today.
- *To a package:* the same, with `setPackage("com.spotify.music")`. Only that app is asked. This is what
  "tell Spotify" means in code.
- *Explicit:* a named activity class. Brittle, because class names change between versions.

**No permission is needed to start an activity.** "You can start another app's activity using either an
implicit or explicit intent regardless of whether that app is visible to your app" (docs, [package visibility:
automatic][pv-auto]). If nothing answers, `startActivity` throws `ActivityNotFoundException`; the guide says to
catch it ([use cases][pv-use]). `Executor.run` already does.

**Seeing who would answer is a different thing.** Package visibility filters what `queryIntentActivities`,
`resolveActivity` and `getPackageInfo` return, and it also limits binding another app's service (docs,
[package visibility][pv]). An app is made visible by a `<queries>` entry: a package name, an intent signature
(exactly one action; wildcards allowed for the scheme and host) or a provider authority (docs,
[declaring][pv-decl]).

**What Booklight would have to declare: nothing new.** Its manifest already queries `MAIN` + `LAUNCHER`, so
every app with an icon is visible to it, and visibility is per app, not per intent. That is why
`AppCommands.safe()` can already resolve other apps' activities for any action (repo). The guide states the
"appears in the results of any query" rule for `<package>` entries; that an `<intent>` entry gives the same
whole-app visibility is how the code works today, not a sentence I found in the guide. Only an app with no
launcher icon would need an entry of its own.

**Starting while Booklight is in front.** Android blocks activity starts from the background. The first
exception on the list is "The app has a visible window, such as an activity in the foreground" (docs,
[background starts][bal]). The panel is that window. So every effect must be started before the panel is gone,
from the panel's activity, as `Executor` does now. Android 15 to 17 tightened this for `PendingIntent` and
`IntentSender` (docs, [15][b15], [17][b17]); Booklight uses neither for these starts.

**Web links are a special case since Android 12.** "a generic web intent resolves to an activity in your app
only if your app is approved for the specific domain" (docs, [Android 12][b12]). So an `https://open.spotify.com/…`
link sent with no package may open the browser. Sent with `setPackage`, it goes to the app if the app has a
matching filter. Google's own guides do this: Maps and the Play Store pages both say to call `setPackage`
(docs, [Maps intents][maps-intents], [linking to Google Play][play-link]).

**Rules that grew stricter, and what they mean here.**
- Android 14: "Implicit intents are only delivered to exported components" (docs, [14][b14]). Booklight only
  starts exported activities anyway.
- Android 16, "Safer Intents": a receiving app can opt in so that "Explicit Intents Must Match the Target
  Component's Intent Filter", and "The plan is to eventually make strict intent resolution the default" (docs,
  [16][b16]). So: send an action the app declares, to its package. Do not name a class and hope.
- Android 17, background audio hardening: playback, audio focus and volume changes from the background are
  restricted (docs, [17][b17]). Booklight changes volume and sends media keys while its panel is visible.
  Whether a key sent in the instant the panel closes counts as "background" is **not verified**.

**Nothing comes back.** `startActivity` says only that an activity was found. Whether Spotify played, searched,
or showed a login is not reported. A row can say "Sent to Spotify", never "Playing".

**On a Googlebook, starting an activity opens or raises a window.** "Play this" sent as an activity start is
likely to bring the player's window to the front and take the keys from the app the user was in. A
media-session route (section 3.5) starts no activity, so it should not. **Not verified** for any player.

**Booklight's own rule for other apps' intents** (repo, `Executor.open` and `AppCommands.safe`): start only an
activity of the named app, exported, asking for no permission; no flags but Booklight's own; no clip data, no
selector, no grant flags. Everything below fits inside that rule, except implicit starts with no package
(`tel:`, `smsto:`), which need a small effect of their own.

---

## 2. Standard actions and their extras

"Today" = what Booklight already sends (repo: `Executor.kt`, `scopes/Jot.kt`, `providers/Dials.kt`).

| Action | Data and extras | Who typically answers | Source | In Booklight today |
| --- | --- | --- | --- | --- |
| `android.media.action.MEDIA_PLAY_FROM_SEARCH` | `android.intent.extra.focus` (the search mode) and `query`; for a mode: `android.intent.extra.artist`, `.album`, `.title`, `.genre`, `.playlist` (section 3.1) | Music players that declare it | docs, [common intents][common], [MediaStore][mediastore] | Yes: `play …`, unstructured, no package |
| `android.intent.action.MEDIA_SEARCH` | `query`, optional artist, album, title, focus. "Perform a search for media": no playing | Music and video apps | docs, [MediaStore][mediastore] | No |
| `android.media.action.VIDEO_PLAY_FROM_SEARCH` | `query` (a film, an actor, a genre) | Video apps that declare it; which do is not confirmed | docs, [MediaStore][mediastore] | No |
| `android.media.action.TEXT_OPEN_FROM_SEARCH` | `query` (a book, an author) | Reading apps; which do is not confirmed | docs, [MediaStore][mediastore] | No |
| `ACTION_VIEW` | A link: `https:`, or an app's own scheme (section 4). Optional boolean `android.intent.extra.START_PLAYBACK`: "content should immediately be played without any intermediate screens" | The app that owns the link | docs, [IntentCompat][intentcompat] | Yes: `Effect.OpenUrl`, `https` and `http` only in the user's links |
| `ACTION_SEND`, type `text/plain`, with `setPackage` | `EXTRA_TEXT`, `EXTRA_SUBJECT` | Any app with a share target: notes, chat, mail | docs, [common intents][common] | Yes, to the Gemini app; the share sheet as Keep's fallback |
| `ACTION_SENDTO` | `mailto:` with subject and body; `EXTRA_EMAIL`, `EXTRA_CC`, `EXTRA_BCC` | Mail apps only | docs, [common intents][common] | Yes: `mail …` |
| `ACTION_SENDTO` | `sms:`, `smsto:`, `mms:`, `mmsto:` with a number; extra `sms_body` | The messages app | docs, [common intents][common] | No |
| `ACTION_DIAL` | `tel:<number>`, `voicemail:` | The phone app. It opens filled in; the user presses call | docs, [common intents][common] | No |
| `ACTION_CALL` | `tel:` | Places the call. Needs the `CALL_PHONE` permission | docs, [common intents][common] | No, and it should stay out |
| `ACTION_INSERT` on `CalendarContract.Events.CONTENT_URI` | begin, end, all-day, title, location; also `Events.DESCRIPTION` and `EXTRA_EMAIL` (invitees, comma-separated) | The calendar's editor | docs, [common intents][common] | Yes: `event …`. Description and invitees are not sent |
| `ACTION_VIEW` on `content://com.android.calendar/time/<millis>` | | The calendar, on that day | memory | No |
| `android.intent.action.SET_ALARM`, `SET_TIMER` | hour, minutes, message, `EXTRA_DAYS` (repeat), `EXTRA_RINGTONE`, `EXTRA_VIBRATE`, `EXTRA_SKIP_UI`; `EXTRA_LENGTH` (1 to 86400 s) | The Clock app. Needs the install-time `SET_ALARM` permission | docs, [AlarmClock][alarmclock] | Yes. `EXTRA_DAYS` is not sent ("alarm 7:30 weekdays") |
| `SHOW_ALARMS`, `SHOW_TIMERS` | | The Clock app's lists | docs, [AlarmClock][alarmclock] | No |
| `DISMISS_ALARM`, `SNOOZE_ALARM`, `DISMISS_TIMER` | A search mode or a link to one alarm; `EXTRA_ALARM_SNOOZE_DURATION` (minutes). `DISMISS_TIMER` with no data: "dismiss all expired timers" | The Clock app. The open-source Clock handles all seven actions (source, `HandleApiCalls.kt`) | docs, [AlarmClock][alarmclock] | No (idea 15 of `next-features-2.md`) |
| `ACTION_WEB_SEARCH` | `query` | The search app or the browser | docs, [Intent][intent] | No: Booklight opens the chosen engine's address |
| `ACTION_SEARCH` | `query` | An app's own search activity, if it is exported | docs, [Intent][intent] | No |
| `com.google.android.gms.actions.SEARCH_ACTION` | `query` | Apps that declared "search for X on this app". Google now says: "We don't recommend using SEARCH_ACTION for app search" and points to the assistant's `GET_THING` | docs, [common intents][common] | No |
| `com.google.android.gms.actions.CREATE_NOTE` | name, text | The notes app | docs, [common intents][common] | Yes: "Keep" on a note |
| `android.intent.action.CREATE_NOTE` (API 34) | No text | The app holding the notes role, in a floating window | docs, [Intent][intent]; repo | No |
| `android.intent.action.CREATE_REMINDER` (API 30) | `EXTRA_TITLE`, `EXTRA_TEXT`, `EXTRA_TIME` | Who answers is not confirmed | docs, [Intent][intent] | No |
| `android.intent.action.TRANSLATE`, `DEFINE` (API 29) | `EXTRA_TEXT` | Who answers is not confirmed | docs, [Intent][intent] | No |
| `ACTION_PROCESS_TEXT` | `EXTRA_PROCESS_TEXT`; the result comes back | Any app in the selection menu | docs, [Intent][intent] | Booklight receives it. It does not send it to others |
| `android.settings.*` | One action per page | Settings | docs, [Settings][settings] | Yes: 47 pages |
| `android.settings.panel.action.INTERNET_CONNECTIVITY`, `VOLUME`, `WIFI`, `NFC` | | A system sheet | docs, [Settings.Panel][panel] | Internet and volume |
| Per-app pages: `APP_NOTIFICATION_SETTINGS` (extra `EXTRA_APP_PACKAGE`), `APP_LOCALE_SETTINGS`, `APP_OPEN_BY_DEFAULT_SETTINGS`, `APPLICATION_DETAILS_SETTINGS` (data `package:<name>`), `APP_USAGE_SETTINGS` (extra `EXTRA_PACKAGE_NAME`) | | Settings, on that app's page | docs, [Settings][settings]; repo (`next-features.md` L12 says they resolved on one Googlebook) | App info only |
| `android.intent.action.OPEN_EYE_DROPPER` (API 37) | Returns `EXTRA_COLOR` | The system's eye dropper | docs, [Intent][intent] | No (idea 8 of `next-features-2.md`) |
| `ACTION_MAIN` with a selector for `CATEGORY_APP_MUSIC`, `_EMAIL`, `_MAPS`, `_CALENDAR`, `_CALCULATOR`, `_WEATHER` … | Through `Intent.makeMainSelectorActivity` | The default app of that kind | docs, [Intent][intent] | No |
| `android.media.action.STILL_IMAGE_CAMERA`, `VIDEO_CAMERA` | | The camera app | docs, [common intents][common] | No |
| `ACTION_INSERT`, type `Contacts.CONTENT_TYPE` | `Insert.NAME`, `Insert.EMAIL`, phone | The contacts editor, filled in | docs, [common intents][common] | No |
| Media keys: `AudioManager.dispatchMediaKeyEvent` | Play, pause, play/pause, stop, next, previous, rewind, fast forward (source, `KeyEvent.isMediaSessionKey`) | The app that holds the media button session; if none, the last one that did (section 3.4) | docs, [AudioManager][audiomanager]; source | Play/pause, next, previous |

---

## 3. Media: playing a specific thing

### 3.1 `MEDIA_PLAY_FROM_SEARCH`, in full

The contract (docs, [common intents][common]): "The receiving app for this intent performs a search within its
inventory to match existing content to the given query and starts playing that content." The mode is the extra
`android.intent.extra.focus`:

| Mode | Value of `android.intent.extra.focus` | Required extras | Optional |
| --- | --- | --- | --- |
| Any ("play some music") | `vnd.android.cursor.item/*` | `query` = empty | |
| Unstructured | `vnd.android.cursor.item/*` | `query` = anything | |
| Genre | `Audio.Genres.ENTRY_CONTENT_TYPE` | `android.intent.extra.genre`, `query` | |
| Artist | `Audio.Artists.ENTRY_CONTENT_TYPE` | `android.intent.extra.artist`, `query` | genre |
| Album | `Audio.Albums.ENTRY_CONTENT_TYPE` | `android.intent.extra.album`, `query` | artist, genre |
| Song | `vnd.android.cursor.item/audio` | `android.intent.extra.title`, `query` | album, artist, genre |
| Playlist | `Audio.Playlists.ENTRY_CONTENT_TYPE` | `query` | album, artist, genre, `android.intent.extra.playlist`, title |

Booklight sends only the second row, to no app in particular (repo, `Effect.PlayMusic`). With several players
installed the system chooses or asks. **Not verified** what a Googlebook shows then.

A player "should" treat an empty query as "play something": "An empty or null query should be treated as a
request to play any music" (docs, [TransportControls][transport]).

### 3.2 Spotify, as far as documents and reports go

Spotify publishes one page about being opened from another Android app ([content linking][sp-link]) and an SDK.
It publishes nothing about `MEDIA_PLAY_FROM_SEARCH`.

| Way | What is known | Plays the song? | Sure |
| --- | --- | --- | --- |
| `MEDIA_PLAY_FROM_SEARCH` to `com.spotify.music` | Spotify declares the action: in both threads it is among the apps that answer. In 2015 a developer wrote "if I specify a search for an artist it will auto play", and that an empty query "just launches and goes to the search screen" ([community][so-2015]). In 2020 another wrote: "If I select Spotify, it does a search but does not play, even though I have a Spotify premium subscription", with the unstructured mode; the only answer suggests trying the song or playlist mode, untested ([community][so-2020]) | Unknown. It may depend on the mode, the app's version and the plan | **not verified** |
| `spotify:search:<text>` with `ACTION_VIEW` | Opens Spotify on the search results for the text. Not on Spotify's Android pages read today | No: a search page. One click from playing | memory; **not verified** |
| `https://open.spotify.com/search/<text>` to the package | The web player's address for a search. Whether the app takes it is not confirmed | No | memory; **not verified** |
| `spotify:track:<id>`, `spotify:album:<id>`, `spotify:artist:<id>`, `spotify:playlist:<id>`, or the same as `https://open.spotify.com/track/<id>` | Spotify's page: "navigate directly to Spotify deeplinks" with an `ACTION_VIEW` intent, and it asks callers to pass their package name as `EXTRA_REFERRER` or `utm_campaign` ([vendor][sp-link]). The page does not say whether anything plays. Booklight would need the id, which only a search through Spotify's API gives | The page opens. Playing is not promised. A 2012 answer says the track started ([community][so-2012]) | vendor for the link; playing **not verified** |
| The same link with `:play` on the end | "The music starts playing automatically since there is a ':play' at the end" (2017, for a playlist) ([community][so-2017]) | Reported yes in 2017 | community; **not verified** |
| The same link with the extra `android.intent.extra.START_PLAYBACK` = true | The Assistant "adds the extra EXTRA_START_PLAYBACK with value true to the intent it sends" for deep links (docs, [Assistant and media apps][media-assistant]). Whether Spotify honours it from another caller is not confirmed | Unknown | **not verified** |
| A media key (play) | Resumes whatever played last (section 3.4). It cannot choose a song | Resume only | source |
| A media button broadcast to Spotify's own receiver, or its old widget broadcasts | 2015: a `MEDIA_BUTTON` broadcast to a named Spotify receiver class resumed playback ([community][so-2015]). 2017: the widget's `PLAY` broadcast "doesn't work anymore" ([community][so-widget]). Class names change | Resume only, if at all | community; **not verified**; not a base to build on |
| Spotify's App Remote SDK | It "supports getting metadata for the currently playing track and context, issuing basic playback commands as well as initiating playback of tracks" ([vendor][sp-android]). Needs: a client id from Spotify's dashboard, the calling app's package name and signing fingerprint registered there, the Spotify app installed and logged in, and the user's yes to the `app-remote-control` scope ([vendor][sp-start]). "A Spotify Premium account is required to play a single track uri" ([vendor][sp-remote-readme]). The SDK is "currently in Beta" and has no search: a name must first become a URI through the Web API | Yes, for Premium, by URI | vendor |
| Spotify's Web API | Search needs an OAuth token ([vendor][sp-search]). Start playback: `PUT /me/player/play`, scope `user-modify-playback-state`, "This API only works for users who have Spotify Premium" ([vendor][sp-play]) | Yes, for Premium | vendor |

**The limit that decides it** ([vendor][sp-quota]): a new Spotify app is in "development mode". "Up to 5
authenticated Spotify users can use an app that is in development mode", each added by hand to an allowlist, and
"The app owner must have a Spotify Premium account". To lift that: "as of May 15th 2025, Spotify only accepts
applications from organizations (not individuals)", with "at least 250k MAUs". So a login-based Spotify feature
cannot ship in a public launcher. It can exist for one person with his own client id.

**With and without Premium.** Everything Spotify documents about starting a named track says Premium (SDK and
Web API, above). Google says the same about Gemini's Spotify connection: "Media streaming apps play specific
media, like songs or podcasts, only if you have a subscription with them that supports on-demand playback"
([docs][gem-media]). What a free account gets from `MEDIA_PLAY_FROM_SEARCH` (a shuffle of the artist, a search
page, nothing) is **not verified**. From memory, Spotify's free plan has let tablets and desktops choose songs
more freely than phones; which of those the Android app thinks a Googlebook is, is **not verified**.

**Spotify can say what is playing, if the user switches it on.** With "Device Broadcast Status" on in Spotify's
settings, it broadcasts `com.spotify.music.metadatachanged` (track, artist, album, URI, length) and
`…playbackstatechanged`. "It is not possible for your app to programmatically configure Spotify to enable
broadcasting" ([vendor][sp-broadcast]). A receiver registered while the panel is open would only hear changes
that happen in that moment, so it is of little use to a launcher.

### 3.3 Other players

- **YouTube Music.** `MEDIA_PLAY_FROM_SEARCH`: "YouTube Music also does only a search" (2020,
  [community][so-2020]). Links: `https://music.youtube.com/search?q=…`, `https://music.youtube.com/watch?v=<id>`
  (memory). Google's help for Gemini: without Premium "the YouTube Music app automatically opens and plays
  music", with Premium it plays in the background ([docs][gem-ytm]); that is Gemini's own connection, not an
  intent anyone can send. **Not verified.**
- **YouTube.** `https://www.youtube.com/watch?v=<id>` and `…/results?search_query=…` to the package
  `com.google.android.youtube`; the old scheme `vnd.youtube:<id>`. Google's `YouTubeIntents` reference page is
  gone (404 today). The scheme is real: an open-source YouTube client registers `vnd.youtube` and
  `vnd.youtube.launch` (source, [NewPipe manifest][newpipe]). **Not verified** on the app itself.
- **Any open-source or small player** that follows Google's guide plays from `MEDIA_PLAY_FROM_SEARCH`: the
  guide's own manifest sample is that filter (docs, [Assistant and media apps][media-assistant]).
- **Video** has its own action (`VIDEO_PLAY_FROM_SEARCH`). Which apps answer it is not confirmed.

### 3.4 Media keys

`AudioManager.dispatchMediaKeyEvent` needs no permission (source, `MediaSessionService.dispatchMediaKeyEvent`:
no permission check; it returns early only during device setup or when a call holds the "global priority"
session). Where the key goes (source, `dispatchMediaKeyEventLocked`): to the current media button session; **if
there is none, to the last app that held it**, through its media button receiver. Google's guide says the same:
"If Android can identify the last active media session, it tries to restart the session" (docs,
[media buttons][media-buttons]). So "play" with nothing playing resumes the last player.

Ten keys count as media session keys (source, `KeyEvent.isMediaSessionKey`): play, pause, play/pause, headset
hook, stop, next, previous, rewind, record, fast forward. Booklight sends three. `pause` could send Pause, which
can never start anything, where it sends the toggle today.

The key cannot be aimed. Which app has the session is not readable without notification access
(`getMediaKeyEventSession`: docs, [MediaSessionManager][msm]).

### 3.5 Media sessions: the way the Assistant really drives a player

"Assistant communicates with Android media apps using a media session. It can use intents or services to launch
your app and start playback" (docs, [Assistant and media apps][media-assistant]). The service way:

1. The Assistant connects to the app's `MediaBrowserService`. The app's `onGetRoot()` decides who may:
   "If the method returns null, the connection is refused" (docs, [media browser service][mbs]).
2. It reads the session token from the connection and builds a `MediaController`.
3. It calls `playFromSearch(query, extras)` with the same extras as section 3.1, or `play()`, `pause()`,
   `skipToNext()`.

Any app may attempt step 1. Google ships a test tool that does exactly this for every installed player:
"Search: Sends the text provided as a search via prepareFromSearch() or playFromSearch()" (source,
[Media Controller Test][mct]). The service must be visible to the caller: "Apps targeting SDK level 30 or
higher must include a `<queries>` element in their manifest to connect to a different app's
MediaSessionService" (docs, [Media3][m3-connect]). Booklight already sees apps with an icon (section 1).

**Who is let in is each app's choice.**
- Google's advice to players: "The easiest way is to allow all MediaBrowser apps to connect" (docs,
  [Assistant and media apps][media-assistant]).
- Google's sample does the opposite: it lets in its own app, the system, a list of signatures (Android Auto,
  Wear OS, the Google app), holders of `MEDIA_CONTENT_CONTROL`, and **apps with an enabled notification
  listener** (source, [UAMP PackageValidator][uamp]).
- Apps on the newer Media3 library, unless they changed it: "it allows all controllers to connect. If the
  controller is trusted, all session and player commands are made available. Untrusted controllers obtain read
  access only" (docs, [MediaSession.Callback][m3-callback]). Trusted means the system, the app itself, and apps
  with `MEDIA_CONTENT_CONTROL` or an enabled notification listener (source, `MediaSessionService.isTrusted`).
  So Booklight might read what such a player is playing with no permission, and not be allowed to command it.
- What Spotify, YouTube Music and the others do with an unknown caller is **not verified**.

**The other way to a token needs a grant.** `MediaSessionManager.getActiveSessions`: "This requires the
`MEDIA_CONTENT_CONTROL` permission … You may also retrieve this list if your app is an enabled notification
listener" (docs, [MediaSessionManager][msm]). `MEDIA_CONTENT_CONTROL` is `signature|privileged`, "Not for use by
third-party applications" (source, the platform manifest).

---

## 4. Links into apps

All are sent as `ACTION_VIEW`. "To the package" means with `setPackage`, so the app opens it and not the
browser.

| App | What to send | What it should do | Source | Sure |
| --- | --- | --- | --- | --- |
| Spotify | Section 3.2 | | | |
| YouTube, YouTube Music | Section 3.3 | | | |
| Netflix | `https://www.netflix.com/title/<id>`, `https://www.netflix.com/search?q=…`; an own scheme `nflx:` | The title's page; a search | memory | **not verified** |
| Google Maps | `geo:0,0?q=<text>`, `geo:<lat>,<lng>?z=<zoom>`; `google.navigation:q=<place>&mode=d\|b\|l\|w&avoid=t\|h\|f`; `google.streetview:cbll=<lat>,<lng>`; package `com.google.android.apps.maps` | A search; turn-by-turn "from the user's current location"; Street View | docs, [Maps intents][maps-intents] | Formats sure. `geo:` only helps where a maps app takes it; Booklight uses `https` map links for that reason (repo, `device-findings.md`) |
| Google Maps, any platform | `https://www.google.com/maps/search/?api=1&query=…`; `https://www.google.com/maps/dir/?api=1&origin=…&destination=…&travelmode=driving\|walking\|bicycling\|two-wheeler\|transit`; `&dir_action=navigate` | A search; directions; works in a browser too. "`api=1` is a required parameter" | docs, [Maps URLs][maps-urls] | Formats sure |
| WhatsApp | `https://wa.me/<number>?text=<text>`; the number in full international form, digits only | The chat with that number, the text in the field, not sent | vendor (WhatsApp's "click to chat" page answered HTTP 400 today), so memory | **not verified** |
| Telegram | `https://t.me/<username>?text=<draft>`, `tg://resolve?domain=<username>&text=<draft>`; by phone `https://t.me/+<number>?text=<draft>`; `https://t.me/share?url=<url>&text=<text>` | The chat, the draft in the field; a share to a chosen chat | vendor, [Telegram links][tg-links]; source ([manifest][tg-manifest]: `tg:`, `t.me`) | Formats sure |
| Signal | `https://signal.me/#p/+<number>`; own scheme `sgnl://signal.me/…`; also a `text/plain` share target | The chat with that number | source ([manifest][signal-manifest]: hosts `signal.me`, `signal.group`, `signal.link`); the `#p/` form from memory | **not verified** |
| Slack | `slack://open?team=<T…>`, `slack://channel?team=<T…>&id=<C…>`, `slack://user?team=<T…>&id=<U…>`; `https://slack.com/app_redirect?channel=<name or id>` | The workspace, a channel, a direct message. Ids are needed, not names, except in the redirect form | vendor, [Slack deep linking][slack] | Formats sure |
| Zoom | `zoommtg://zoom.us/join?confno=<id>&pwd=<code>`, `https://zoom.us/j/<id>` | Joins the meeting | memory (Zoom's pages answered 404 today) | **not verified** |
| Google Meet | `https://meet.google.com/new`, `https://meet.google.com/<code>`; `meet.new` | A new meeting; a meeting by its code | memory | **not verified** |
| Docs, Sheets, Slides | `https://docs.google.com/document/create?title=…` (and `spreadsheets`, `presentation`); `doc.new`, `sheet.new`, `slide.new` | A new file with that title | repo (`scopes/Jot.kt`, since 1.1) | In use |
| Keep, Calendar | `keep.new`, `cal.new`; the platform's own calendar insert is better | A new note; a new event | memory | **not verified** |
| Drive | `https://drive.google.com/drive/search?q=…` | A search | repo (a default link) | In use |
| Gmail | `mailto:` (the platform way); `https://mail.google.com/mail/?view=cm&to=…&su=…&body=…` | Compose | docs for `mailto:`; the web form from memory | `mailto:` in use |
| Play Store | `https://play.google.com/store/apps/details?id=<package>`, `…/store/search?q=<text>&c=apps`, to the package `com.android.vending`. The older `market://details?id=` form is still in Spotify's own sample | The app's page; a search | docs, [linking to Google Play][play-link]; `market:` from memory | `https` forms sure |
| Chrome | Any `https:` link, to the package. No way to ask for an empty new tab | The page in a new tab | repo (`device-findings.md`) | Checked earlier |
| Gemini | `ACTION_SEND`, `text/plain`, to `com.google.android.apps.bard` | The text in the prompt, not sent | repo (`device-findings.md`) | Checked earlier |

**The `intent:` address.** Any intent can be written as one line: `intent://<host>/<path>#Intent;scheme=…;
package=…;action=…;category=…;S.<name>=<text>;B.<name>=true;i.<name>=5;end` (docs, [Chrome: Android
intents][chrome-intents]; source, `Intent.parseUri`: the typed extras are `S.` text, `B.` boolean, `i.` int,
`l.` long, `f.` float, `d.` double, `b.` byte, `c.` char, `s.` short). Booklight already stores other apps'
commands this way (repo, `Effect.Open`). It is the natural format for commands the user writes himself.

---

## 5. How Google Assistant and Gemini control apps

### 5.1 App Actions: capabilities in `shortcuts.xml`

An app declares `<capability android:name="actions.intent.…">` in the same `shortcuts.xml` that holds its
launcher shortcuts. Each capability holds one or more `<intent>` elements (`android:action`, `targetPackage`,
`targetClass`, or a `<url-template>` such as `myapp://search{?q}`) and `<parameter>` elements that map the
assistant's slots to extras or template variables (docs, [create shortcuts.xml][aa-schema]).

"Assistant generates an Android intent to launch the in-app destination of the request … Assistant extracts the
parameters of the query and passes them as extras" (docs, [Assistant for Android][aa]). So the last step **is**
a plain intent or deep link. The built-in intents listed today are few: Open app feature, Get thing ("Search
for content or entities using the default in-app search feature"), Create call, Create message, Get message,
item lists, and the fitness and transport ones (docs, [built-in intents][aa-bii]).

**What a third party can do.** Read another app's `shortcuts.xml` (Booklight already reads it for manifest
shortcuts) and build the same intent, if the target is exported. For `GET_THING` that gives "search inside this
app" with no table. **What it cannot do:** there is no public API to call a built-in intent, the language
understanding is Google's, and targets that are not exported cannot be started (section 5.5). "Users can only
access App Actions on Android phones" (docs, [Assistant for Android][aa]); how many apps on a Googlebook declare
capabilities is **not verified**.

Also from that page: "If your app exports its activities … Assistant may automatically use these intents to
bootstrap your app's support for Assistant queries." Exported activities are fair game for the Assistant too.

### 5.2 Media

Section 3.5: a media session, reached through the player's media browser service, with Google's signatures on
the player's allow-list. Deep links with `EXTRA_START_PLAYBACK` are the second way, and "URI-based actions only
work for companies that provide URIs to Google" (docs, [Assistant and media apps][media-assistant]): Google has
a catalogue of each service's content, which is how a spoken song name becomes a URI.

### 5.3 Gemini's connected apps

- **Spotify:** "your media streaming account for that service must be linked to your Google Account", and "Keep
  Activity" must be on ([docs][gem-media]). An account link is a connection between the two companies' servers.
  The same page: Gemini cannot "Control media playback with commands like pause, resume, next, and previous"
  through that connection.
- **Pause, next, alarms, opening apps:** the "Device assistance" app. Reading and answering notifications
  needs notification access for the Google app. Switching Wi-Fi, Bluetooth, Do Not Disturb or the flashlight,
  the volume, a screenshot and power off need the Google app to be the default digital assistant
  ([docs][gem-device]).
- **WhatsApp, Phone, Messages:** built in for Gemini on Android ([docs][gem-wa]).

So Gemini's reach comes from account links, a catalogue, the assistant role and notification access. A plain app
has the intents of sections 2 to 4.

### 5.4 App Functions

Android 16 and later have a real "call a function in another app" API. "As of May 2026, AppFunctions
integration with Gemini is in a private preview with trusted testers", and "Callers must have the
`EXECUTE_APP_FUNCTIONS` permission" (docs, [AppFunctions][appfunctions]). In the Android 17 source that
permission is `normal`, but: "the application must be on a device allowlist, and its ability to execute
functions is subject to user approval". So: a new permission, and an allow-list Booklight is not on. Not usable
today. It is the thing to watch.

### 5.5 Launcher shortcuts

`LauncherApps.startShortcut` works only for "The current launcher" and "The currently active voice interaction
service" (docs, [LauncherApps][launcherapps]). Everyone else can read the static shortcuts from the app's XML
and start the ones whose target is exported; Chrome's "New tab" is not (repo, `device-findings.md`, "Why
Chrome's 'New tab' is not offered"). Dynamic shortcuts (a chat, a playlist the app pinned) are not readable at
all without that role.

---

## 6. What is not possible without a permission or a role

Said plainly:

1. **Start a named song in Spotify for certain.** A link opens the page; `MEDIA_PLAY_FROM_SEARCH` asks and
   Spotify decides. Certain playback needs Spotify's SDK or API, a login, and Premium.
2. **Know whether it worked.** No result comes back from another app's activity.
3. **See what is playing, or in which app.** Needs notification access. Exception to check: players on Media3
   that let any controller read (section 3.5).
4. **Aim pause or next at one app.** A media key goes where the system sends it. Aiming needs that app's
   session token: its media browser service must let Booklight in, or notification access.
5. **Start another app's shortcuts that are not exported, or any dynamic shortcut.** Needs the launcher role or
   a voice interaction service as the assistant.
6. **Call App Functions.** A new permission and an allow-list.
7. **Call the assistant's built-in intents as such.** No API. Only the plain intents behind them.
8. **Switch Wi-Fi, Bluetooth, Do Not Disturb, dark theme, the flashlight.** Not for a plain app; Gemini does it
   as the system's assistant. A third-party assistant does not get those either (repo, `use-cases.md`).
9. **Send a message or place a call without the user's last click.** Links fill in; the user sends. `ACTION_CALL`
   needs `CALL_PHONE`.
10. **Press a button inside another app.** Accessibility: ruled out.
11. **Do any of it while the panel is closed.** Background starts are blocked, and the owner's rule is the same.

---

## 7. The "power" options, as a later tier

Each is against the owner's present rule and needs his say-so. What each would unlock:

| Option | What the user grants | What it unlocks | What it costs |
| --- | --- | --- | --- |
| **Notification access** | A Settings switch ("Device and app notifications") | The active media sessions: a "now playing" row with title and artist, pause and next aimed at a named app, `playFromSearch` and `playFromUri` on a running player. It also makes Booklight a trusted controller for Media3 players and for players that copied Google's sample validator | A listener service the system keeps bound: something of Booklight's in the background. It can technically read every notification. Play: data-safety answers; no form found (repo, `use-cases.md`) |
| **A Spotify login with the user's own client id** | A Spotify developer app of his own (client id; Booklight's package and fingerprint registered), a consent screen in Spotify | Search by name, then play exactly that track, queue, "what is playing", all without raising Spotify's window | Spotify's SDK in the APK, or Web API calls with a token; Premium; five users at most. For the owner, not for the public |
| **Assistant role with a voice interaction service** | Choosing Booklight as the digital assistant | Every app's shortcuts through `LauncherApps`, dynamic ones included; starts from the background; the assistant key | Replaces Gemini there; a service of Booklight's; a real assistant needs a recognition service (repo, `android-platform.md`) |
| **`EXECUTE_APP_FUNCTIONS`** | An install-time permission, plus an allow-list Google controls | Typed functions in other apps, the way Gemini will call them | Not open today |
| **Accessibility service** | | Everything else | Ruled out |

If one of these is ever opened, notification access gives the most for this theme: it is what turns "ask and
hope" into "see and control" for every player at once.

---

## 8. What Booklight already does (so nothing is invented twice)

- `play <text>`: `MEDIA_PLAY_FROM_SEARCH`, unstructured, to no app in particular (`providers/Dials.kt`,
  `Effect.PlayMusic`).
- Media keys play/pause, next, previous, sent blind; volume and mute (`device/Audio.kt`).
- Mail, event, timer, alarm, reminder, a note to Keep, new Doc, Sheet and Slides, Ask Gemini (`scopes/Jot.kt`,
  `Executor.kt`).
- 47 settings pages and two panels (`res/values/settings_pages.xml`).
- Other apps' manifest shortcuts, and the commands an app declares for Booklight in a file, with keywords that
  take text (`providers/AppCommands.kt`, `docs/EXTENSIONS.md`). Only exported activities of the declaring app.
- The user's own links with `{argument}`, `{clipboard}`, `{date}`, and recipes of steps (`providers/User.kt`,
  `core/Templates.kt`, `data/Recipes.kt`). **A link must start with `https://` or `http://` today**
  (`window/Commands.kt`), so `spotify:search:{argument}` cannot be saved as a link yet. A recipe step can be an
  app, a link, a settings page, a level, a copy or a media key: not `play`, not another app's command.
- `Executor.kt` is the only place that starts activities. Other apps' intents travel as `intent:` addresses in
  `Effect.Open(owner, intent)` and are checked again when run.

---

## 9. To check on a device

One short session answers most of it. Nothing below sends anything; some of it starts music.

1. `MEDIA_PLAY_FROM_SEARCH` to each installed player by package, with a song, an artist, an album and a
   playlist, in the unstructured mode and in the matching structured mode: plays, searches, or nothing. With a
   free and with a paid Spotify account if both are at hand.
2. The same with an empty query: does the player resume?
3. With no package and more than one player: what the system shows.
4. `spotify:search:<text>` and `https://open.spotify.com/search/<text>` to the package: the search page?
5. A known `spotify:track:<id>`: the page, or playback? The same with `:play`, and with
   `android.intent.extra.START_PLAYBACK` = true.
6. Each start of step 1 and 5: does the player's window come to the front and take the keys?
7. A `MediaBrowser` connection from a throwaway debug build to each player's media browser service: accepted,
   an empty root, or refused? If accepted: does `playFromSearch` play, and does the window stay where it was?
   Can the metadata be read?
8. Media keys: Play with nothing playing (does the last player resume?), Pause (never starts anything?), Stop.
9. `resolveActivity` for another app's activity with an action other than `MAIN`: confirms that the present
   `<queries>` are enough.
10. The links of section 4 marked not verified, each to its package: Netflix title and search, YouTube watch and
    results, YouTube Music search, `wa.me`, `t.me`, `signal.me`, a Zoom join link, `meet.google.com/new`.
11. `smsto:` with `sms_body`, and `tel:` with `ACTION_DIAL`: which app opens, and is the text or number filled in?
12. `SHOW_ALARMS`, `SHOW_TIMERS`, and the per-app settings pages (notifications, language, open by default).
13. Which installed apps declare `<capability>` in `shortcuts.xml`, which declare `SEARCH_ACTION` or an exported
    `ACTION_SEARCH` activity: counts only.
14. `ACTION_SEND` with text to a named notes app and a named chat app: where does the text land?
15. The calendar's `time/<millis>` link.

Rule for that session, from `device-findings.md`: stop every timer that was started, and close what was opened.

---

## Asked of the test Googlebook (2 October 2026)

After the desk research, the test Googlebook's package manager was asked which requests have an app that
answers (`cmd package query-activities`; read-only, nothing was started for this). It says who would take a
request, not what the app then does.

| Request | Apps that answer |
| --- | --- |
| `ACTION_SEARCH` (an app's own search activity) | 22 |
| `com.google.android.gms.actions.SEARCH_ACTION` | 6 |
| `smsto:` | 2 |
| `APP_NOTIFICATION_SETTINGS`, `APP_OPEN_BY_DEFAULT_SETTINGS` | 1 each |
| `APP_LOCALE_SETTINGS` | 2 |
| `SHOW_ALARMS`, `SHOW_TIMERS`, `CREATE_NOTE` | 1 each |

What this changes: many apps do declare a search activity of their own (22), so "search inside an app" can
lean on what apps declare before it needs a bundled table. And two requests cannot be relied on on a
Googlebook: `geo:` (a Googlebook may have no maps app, so `go` must fall back to the web form) and `tel:`
(without a phone app `call` has nothing to open).

One request was really sent, with the music volume at zero: `MEDIA_PLAY_FROM_SEARCH` with a song's name to a
player. It confirms the cost named in section 3: the request raises the player's window. What a set-up player
then does with the request is still **not verified**.

## What could not be verified

- What Spotify does with `MEDIA_PLAY_FROM_SEARCH` today, with and without Premium, in any mode.
- Whether a `spotify:track:` link, `:play` or `EXTRA_START_PLAYBACK` starts playback.
- `spotify:search:` and the `open.spotify.com/search/` link in the Android app.
- Which players let an unknown app connect to their media browser service, and what it may do there.
- Every link in section 4 marked memory: Netflix, YouTube, YouTube Music, WhatsApp, Signal's `#p/` form, Zoom,
  Meet, Keep, the calendar's `time/` link.
- Who answers `VIDEO_PLAY_FROM_SEARCH`, `CREATE_REMINDER`, `TRANSLATE`, `DEFINE` on a Googlebook.
- How many apps declare assistant capabilities or a search action.
- What the system shows when several players answer.
- That an `<intent>` entry in `<queries>` makes the whole app visible (it is how the code behaves; I did not find
  the sentence in the guide).
- Whether a media key sent as the panel closes meets Android 17's background audio rules.
- Spotify's free plan on a Googlebook (phone rules or tablet rules).

---

## Sources

**Android and Google documentation, read today**
[package visibility][pv] · [declaring][pv-decl] · [visible automatically][pv-auto] · [use cases][pv-use] ·
[background activity starts][bal] · [common intents][common] · [MediaStore][mediastore] ·
[AlarmClock][alarmclock] · [Intent][intent] · [IntentCompat][intentcompat] · [Settings][settings] ·
[Settings.Panel][panel] · [AudioManager][audiomanager] · [MediaSessionManager][msm] ·
[MediaController.TransportControls][transport] · [LauncherApps][launcherapps] ·
[Assistant and media apps][media-assistant] · [media browser service][mbs] · [media buttons][media-buttons] ·
[Media3: connect to a media app][m3-connect] · [Media3 MediaSession.Callback][m3-callback] ·
[Assistant for Android][aa] · [create shortcuts.xml][aa-schema] · [built-in intents][aa-bii] ·
[AppFunctions][appfunctions] · behaviour changes [12][b12], [14][b14], [15][b15], [16][b16], [17][b17] ·
[linking to Google Play][play-link] · [Maps intents][maps-intents] · [Maps URLs][maps-urls] ·
[Chrome: Android intents][chrome-intents] · Gemini help: [connected apps][gem-apps],
[media streaming apps][gem-media], [YouTube Music][gem-ytm], [Device assistance][gem-device],
[WhatsApp][gem-wa]

**Vendors' pages, read today**
Spotify: [Android SDK][sp-android] · [getting started][sp-start] · [content linking][sp-link] ·
[media notifications][sp-broadcast] · [quota modes][sp-quota] · [start playback][sp-play] · [search][sp-search] ·
[App Remote README][sp-remote-readme] · [PlayerApi][sp-playerapi]. [Telegram links][tg-links] ·
[Slack deep linking][slack]

**Source, read today**
Android `android17-release`: `frameworks/base/core/res/AndroidManifest.xml` (`MEDIA_CONTENT_CONTROL`,
`EXECUTE_APP_FUNCTIONS`, `ACCESS_SHORTCUTS`), `services/core/java/com/android/server/media/MediaSessionService.java`,
`media/java/android/media/AudioManager.java`, `core/java/android/view/KeyEvent.java`,
`core/java/android/content/Intent.java` ([frameworks/base][aosp]); `packages/apps/DeskClock`,
`HandleApiCalls.kt` (main). Manifests: [Signal][signal-manifest], [Telegram][tg-manifest], [NewPipe][newpipe].
Google samples: [UAMP PackageValidator][uamp], [Media Controller Test][mct].

**Community, read today**
Stack Overflow: [2012, a track from a URI][so-2012] · [2015, play on launch][so-2015] ·
[2017, `:play`][so-2017] · [2017, the widget broadcasts][so-widget] · [2020, search and play][so-2020]

**Not reachable today:** WhatsApp's "click to chat" page (HTTP 400), Zoom's link pages (404), Google's
`YouTubeIntents` reference (404), Android 13's "intent filters block non-matching intents" (no longer on the
page).

**In this repository:** `docs/research/device-findings.md`, `use-cases.md`, `android-platform.md`,
`permissions.md`, `next-features.md`, `next-features-2.md`, `docs/EXTENSIONS.md`, and the files named in
section 8.

[pv]: https://developer.android.com/training/package-visibility
[pv-decl]: https://developer.android.com/training/package-visibility/declaring
[pv-auto]: https://developer.android.com/training/package-visibility/automatic
[pv-use]: https://developer.android.com/training/package-visibility/use-cases
[bal]: https://developer.android.com/guide/components/activities/background-starts
[common]: https://developer.android.com/guide/components/intents-common
[mediastore]: https://developer.android.com/reference/android/provider/MediaStore
[alarmclock]: https://developer.android.com/reference/android/provider/AlarmClock
[intent]: https://developer.android.com/reference/android/content/Intent
[intentcompat]: https://developer.android.com/reference/androidx/core/content/IntentCompat
[settings]: https://developer.android.com/reference/android/provider/Settings
[panel]: https://developer.android.com/reference/android/provider/Settings.Panel
[audiomanager]: https://developer.android.com/reference/android/media/AudioManager
[msm]: https://developer.android.com/reference/android/media/session/MediaSessionManager
[transport]: https://developer.android.com/reference/android/media/session/MediaController.TransportControls
[launcherapps]: https://developer.android.com/reference/android/content/pm/LauncherApps
[media-assistant]: https://developer.android.com/media/implement/assistant
[mbs]: https://developer.android.com/media/legacy/audio/mediabrowserservice
[media-buttons]: https://developer.android.com/media/legacy/media-buttons
[m3-connect]: https://developer.android.com/media/media3/session/connect-to-media-app
[m3-callback]: https://developer.android.com/reference/androidx/media3/session/MediaSession.Callback
[aa]: https://developer.android.com/develop/devices/assistant/overview
[aa-schema]: https://developer.android.com/develop/devices/assistant/action-schema
[aa-bii]: https://developer.android.com/reference/app-actions/built-in-intents/bii-index
[appfunctions]: https://developer.android.com/ai/appfunctions
[b12]: https://developer.android.com/about/versions/12/behavior-changes-all
[b14]: https://developer.android.com/about/versions/14/behavior-changes-14
[b15]: https://developer.android.com/about/versions/15/behavior-changes-15
[b16]: https://developer.android.com/about/versions/16/behavior-changes-16
[b17]: https://developer.android.com/about/versions/17/behavior-changes-17
[play-link]: https://developer.android.com/distribute/marketing-tools/linking-to-google-play
[maps-intents]: https://developers.google.com/maps/documentation/urls/android-intents
[maps-urls]: https://developers.google.com/maps/documentation/urls/get-started
[chrome-intents]: https://developer.chrome.com/docs/android/intents
[gem-apps]: https://support.google.com/gemini/answer/13695044
[gem-media]: https://support.google.com/gemini/answer/15300097
[gem-ytm]: https://support.google.com/gemini/answer/15237118
[gem-device]: https://support.google.com/gemini/answer/15235441
[gem-wa]: https://support.google.com/gemini/answer/15574928
[sp-android]: https://developer.spotify.com/documentation/android
[sp-start]: https://developer.spotify.com/documentation/android/tutorials/getting-started
[sp-link]: https://developer.spotify.com/documentation/android/tutorials/content-linking
[sp-broadcast]: https://developer.spotify.com/documentation/android/tutorials/android-media-notifications
[sp-quota]: https://developer.spotify.com/documentation/web-api/concepts/quota-modes
[sp-play]: https://developer.spotify.com/documentation/web-api/reference/start-a-users-playback
[sp-search]: https://developer.spotify.com/documentation/web-api/reference/search
[sp-remote-readme]: https://github.com/spotify/android-sdk/blob/master/app-remote-lib/README.md
[sp-playerapi]: https://spotify.github.io/android-sdk/app-remote-lib/docs/com/spotify/android/appremote/api/PlayerApi.html
[tg-links]: https://core.telegram.org/api/links
[slack]: https://docs.slack.dev/interactivity/deep-linking
[aosp]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release
[signal-manifest]: https://github.com/signalapp/Signal-Android/blob/main/app/src/main/AndroidManifest.xml
[tg-manifest]: https://github.com/DrKLO/Telegram/blob/master/TMessagesProj/src/main/AndroidManifest.xml
[newpipe]: https://github.com/TeamNewPipe/NewPipe/blob/dev/app/src/main/AndroidManifest.xml
[uamp]: https://github.com/android/uamp/blob/main/common/src/main/java/com/example/android/uamp/media/PackageValidator.kt
[mct]: https://github.com/googlesamples/android-media-controller
[so-2012]: https://stackoverflow.com/questions/12698428
[so-2015]: https://stackoverflow.com/questions/28524063
[so-2017]: https://stackoverflow.com/questions/43806017
[so-widget]: https://stackoverflow.com/questions/43129548
[so-2020]: https://stackoverflow.com/questions/60197217
