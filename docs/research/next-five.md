# Booklight: the next five, commands for other apps

*2 October 2026. A plan from desk research. **Later that day: Alex said to build all five, and the device session was held** (intents.md, "Tried on a Googlebook"); what it changed is in the box below. It answers
Alex's request: "Brainstorm the next 5 features. I want more features to control other apps, like I want to tell
Spotify to play a certain song. Gemini does this through intents I think, so could we construct intents to send
other apps?"*

> **After the device session (2 October).** Spotify takes "play from search" in every mode and only shows its
> search results; it never starts playing, and it refuses a media connection from an app it does not know. So
> feature 1's row for Spotify reads **Search in Spotify** (decision 4), and its "second step" is dropped. New:
> **a Spotify link plays** (`spotify:track:…`, or the address its Share gives), so a link of your own with a
> keyword (feature 3) and a copied Spotify link are the way to "play exactly this". The per-app settings pages
> of feature 5 all work, with battery use as a fourth. Feature 2's links work for Spotify, the Play Store and
> the Googlebook's web apps (YouTube, YouTube Music, Meet). Built with every decision at its recommended answer,
> except decision 10: all of feature 4 is built, each keyword shown only where an app answers it.

**What it rests on.**
- [intents.md](intents.md): the research. What one app can tell another on Android 14 to 17 with no new
  permission, how the Assistant and Gemini really do it, what is not possible, every claim with its source.
- [next-features-2.md](next-features-2.md): the earlier list of 22. This list builds on it and repeats none of
  it. Two of its ideas are neighbours and stay where they are: idea 14 (what other apps can do with a thing) and
  idea 15 (stop what is ringing).
- [not-built-2.0.md](not-built-2.0.md): the old catalogue. Five of its never-decided ideas are folded in here:
  L2 (search inside an app), L12 (more app actions), P4 (meeting rows), P5 (message links), PL10 (Spotify links).

**Not in this list, on purpose,** because they are underway or parked: the line for a fresh copy with
translation (M1), flight numbers (M5), a zero state with suggestions, the settings window, asking about a
picture (M2), the selection (M3), more languages (M4).

**The short answer to the question.** Yes, Booklight can construct intents for other apps, and `play`, `mail`,
`event` and `timer` already do. An intent is a request: the other app decides what happens and nothing comes
back. Gemini's Spotify control is not a plain intent: it is an account link between Google and Spotify, plus
the assistant role and notification access for pause and next (Google's own help pages; intents.md, section 5).
So Booklight can **ask Spotify to play something by name**, and **land on Spotify's search with the text**. It
cannot promise that one named song starts. That needs Spotify's login and Premium, and Spotify does not let a
public app of this size have it (intents.md, section 3.2).

**Effort:** S a day or less · M a few days · L a week or more, as in next-features-2.md.

---

## Summary

| # | Feature | Example | How | New permission? | Difficulty | Verified? |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | **Play it there** | `play bohemian rhapsody on spotify` | Android's "play from search" request, aimed at the named player, in the artist, album, song or playlist mode. The app's search link as the second action | No | M, about 4 days | No. What Spotify does with the request is the open question; reports disagree |
| 2 | **Search inside an app** | `spotify daft punk`, `netflix severance` | What apps declare themselves (a search action, an assistant capability), plus a small bundled table of links in the format of `docs/EXTENSIONS.md` | No | M, about 5 days | Partly. Maps and Play Store formats are documented; the other links are from memory |
| 3 | **Your own commands for other apps** | A link `sp` = `spotify:search:{argument}`; an intent written in the window | The link editor takes any scheme; a new "app command" kind stored as an `intent:` address and run through the check other apps' commands already pass | No | M, about 4 days | The mechanism yes (Android's documentation, and Booklight's own `Effect.Open`). Each command is the user's to try |
| 4 | **Go, call, write** | `go hamburg hbf by train`, `wa +49 171 2345678 running late` | Maps links, the phone app's dial request, `smsto:`, `wa.me`, `t.me`, a new Meet | No | M, about 3 days | Maps, phone, SMS and Telegram formats are documented. WhatsApp, Signal and Meet are from memory |
| 5 | **An app's own switches** | `spotify notifications`, `chrome defaults`, `alarms` | Settings actions that take a package; the Clock's two list actions | No | S, about 1.5 days | The per-app pages resolved on one Googlebook earlier (`next-features.md`, L12). Not tried from Booklight |

Together about 17 to 18 working days, after a device session of half a day. None of the five adds a permission,
a service, or a line to the manifest's `<queries>` (intents.md, section 1, says why).

---

## Decisions for Alex

1. **A device session first?** Half a day on the test Googlebook answers the open questions of all five at
   once (the list is in intents.md, section 9). It starts music and opens apps; it sends nothing.
   *Recommended: yes, before any of it is designed. Feature 1's promise depends on what Spotify does.*
2. **Which player, when none is named?** *Recommended: the one named in the text; else the one used last;
   the others stand on the same row as actions. Never the system's chooser.*
3. **Does Enter start the music at once?** It acts without a second look, like a timer.
   *Recommended: yes. The footer says "Sent to Spotify", never "Playing": Booklight cannot know.*
4. **If Spotify only searches and does not play** (one report says so): is "one Enter from the song" enough
   for now? *Recommended: yes. Ship the row with the honest name "Search in Spotify" and keep "Play" for the
   players that do play.*
5. **A bundled table of other makers' apps** (about 15 lines: Spotify, YouTube, YouTube Music, Netflix, Maps,
   Play Store, Drive and the like). It must be kept up as those apps change.
   *Recommended: yes, small. A line shows only when the app is installed and the link resolves. Names and icons
   are the device's own; nothing of theirs is bundled.*
6. **How "search inside an app" is typed.** The app's name and then the text (`spotify daft punk`), as an
   action on the app's row, or only behind a keyword of its own (`sp daft punk`)?
   *Recommended: the first two. The app's name alone still opens the app. A keyword stays possible through
   feature 3.*
7. **Your own commands: activities only, or broadcasts too?** Some apps (Tasker and the like) listen for
   broadcasts. A broadcast shows nothing on screen, so a wrong one fails silently.
   *Recommended: activities only. Broadcasts later, as a named choice in the editor, if somebody asks.*
8. **Spotify with a login** (your own Spotify client id, Spotify's SDK): the one way to "play exactly this
   song". Spotify allows five named users for such an app and reviews only organisations.
   *Recommended: not in Booklight. If you want it for yourself, it is a separate small build with your own
   client id, about 4 days, Premium needed.*
9. **Notification access, as a later tier.** It is the single grant that would change this theme: a row for
   what is playing, pause and next aimed at one app, play-from-search inside a running player. It also means a
   service of Booklight's that the system keeps bound. *Recommended: not now. Write it into PLAN.md as the
   first thing to look at if the "power" tier is ever opened (intents.md, section 7).*
10. **Is "go, call, write" worth it on a laptop?** `go` and `meet` are; `call`, `sms` and `wa` only where
    those apps are used on a Googlebook. *Recommended: build `go` and `meet` (a day), decide the rest after the
    device session.*
11. **Order.** *Recommended: feature 3's first half (any scheme in a link: half a day) and feature 1 as one
    small release. Then 2 and 5. Then 4.*

---

## 1. Play it there

**Promise.** Name the player and what you want to hear, and Enter asks that app to find it and play it.

**Typed, and what happens**
- `play bohemian rhapsody on spotify`: one row, "Play 'bohemian rhapsody'", Spotify armed. Enter sends the
  request to Spotify. Spotify plays it, or shows its results: its choice.
- `play artist daft punk`: the same, in Android's artist mode, to the player used last. `play album discovery
  by daft punk` and `play playlist focus` likewise.
- `pause` sends Pause, which can never start anything. `play` alone resumes the last player. `stop` stops.

**How it is built on what exists**
- Today `play <text>` sends `MEDIA_PLAY_FROM_SEARCH` in the unstructured mode to no app in particular
  (`PlayScope` in `providers/Dials.kt`, `Effect.PlayMusic`, one line in `Executor.kt`).
- New in `core/`: `Play.kt` with tests. It reads the text after `play`: a kind word at the start (song, album,
  artist, playlist, genre; German words too), "X by Y", and "on <app>" or "in <app>" at the end. It only
  splits the text. Whether the last words name an installed player is the app list's question, as with verbs.
- `Effect.PlayMusic` gains the package, the mode and the mode's extras (intents.md, section 3.1). `Executor`
  sets them. With a package it uses the check of `Executor.open`: an exported activity of that app.
- `PlayScope.rows`: the players are the apps that answer the request among the apps Booklight already sees (one
  `queryIntentActivities`). One row, one action per player with the app's own name and icon. Armed: the named
  one, else the last used (a new setting), else the first. Where a search link is known (feature 2), "Search
  in Spotify" is the next action.
- Outside the scope: `spotify bohemian rhapsody` offers "Play … in Spotify" under the app's own row, where an
  app's commands already go (`AppCommands`), never above a local match.
- `MediaKey` gains `PLAY`, `PAUSE`, `STOP`; `Dials.media` picks by the typed word. All ten keys are allowed by
  the platform (intents.md, section 3.4).
- `play` becomes a recipe step ("morning": a playlist, then the calendar on the left).
- Strings in English and German; a line in the guide and a tip; `./bl debug guide` checks the examples.
- **A second step, only if the device says yes:** connect to the player's media browser service and call
  `playFromSearch` there. That is how the Assistant drives players, it needs no permission where the app lets
  an unknown client in, and it would not raise the player's window. It needs an effect that waits (the panel
  shows "Asking Spotify" for up to a second). About 2 more days.

**To check on a device first** (intents.md, section 9, items 1 to 8). The ones that decide the design:
what each player does with the request in each mode; whether the player's window comes to the front and takes
the keys; whether a media browser connection is accepted; what Pause and Play do with nothing playing.

**Risks**
- Spotify may search and not play. Then decision 4 applies.
- It may differ between free and paid accounts, and change with any update of the player. Nothing can be
  pinned; the footer must not claim more than "Sent".
- An activity start raises the player's window on a desktop. If that annoys, the second step is the cure, where
  the player allows it.
- "on" and "in" are also words in titles ("Smoke on the Water"). Only an installed player's name after them
  counts.

**Difficulty.** M, about 4 days: the reader and its tests 1, the row and the remembered player 1, the keys and
the recipe step 0.5, strings, guide and tips 0.5, device fixes 1. The second step is 2 more.

**Deliberately out.** One exact track by a login (decision 8). A row for what is playing (decision 9). Choosing
among results in the panel (that needs the service's own search API). Queue, like, a volume per app.

---

## 2. Search inside an app

**Promise.** Type an app's name and what you are looking for, and Enter lands on the results inside that app.

**Typed, and what happens**
- `spotify daft punk`: under Spotify's own row, "Search Spotify for 'daft punk'". Enter opens Spotify on its
  results.
- `netflix severance`: Netflix on its search.
- `store calculator` and `maps coffee` open in the Play Store app and the Maps app, where today they open a web
  page.

**How it is built on what exists**
- `AppCommands` already turns an app's declared keyword into a scope that takes text (`Keyword` with a
  `template`, the `Ext` scope, `Effect.Open`). This feature feeds that same structure from two more sources:
  1. *What the app declares for everyone:* an exported activity for `ACTION_SEARCH` or
     `com.google.android.gms.actions.SEARCH_ACTION` (extra `query`), and the assistant's `GET_THING` capability
     with a `url-template` in the app's `shortcuts.xml`. Booklight reads that file already for shortcuts;
     capabilities are read by hand beside them (the old catalogue's L2).
  2. *A bundled table* for well-known apps that declare nothing: an asset in the format of
     `docs/EXTENSIONS.md`, one `scope` per app with its package. An app's own Booklight file wins over it.
- Every line goes through `safe()` as today: it is kept only if it leads to an exported activity of that very
  app. A link the installed version does not take is simply not shown.
- In the panel: an action "Search" on the app's own row, which enters the scope (as `new` does with
  `Effect.EnterScope`); and the typed form above, as a row of kind Command with the app's name as its label.
- The default links `yt`, `maps`, `store`, `drive` (`core/Sites.kt`) go to the app when it is installed, and to
  the browser when not.
- No manifest change: Booklight sees every app with an icon.

The table's first lines, and how sure each is (intents.md, section 4):

| App | Link | Sure |
| --- | --- | --- |
| Maps | `geo:0,0?q={argument}` to the package | Google's documentation |
| Play Store | `https://play.google.com/store/search?q={argument}&c=apps` to the package | Google's documentation |
| Spotify | `spotify:search:{argument}` | memory; **not verified** |
| YouTube | `https://www.youtube.com/results?search_query={argument}` to the package | memory; **not verified** |
| YouTube Music | `https://music.youtube.com/search?q={argument}` to the package | memory; **not verified** |
| Netflix | `https://www.netflix.com/search?q={argument}` to the package | memory; **not verified** |
| Drive | `https://drive.google.com/drive/search?q={argument}` to the package | the web form is in use; the app **not verified** |

**To check on a device first.** Every line of the table: results, the home screen, or nothing. Which installed
apps declare a search action or a capability (intents.md, section 9, items 4, 10, 13). And typing: with
`spotify d` in the field, row one must still be the app.

**Risks**
- The table goes stale as apps change. A debug command that resolves every line on the test device, run before
  each release, keeps it honest.
- An app takes the link and shows its home screen. Only a look at the device tells.
- Few apps may declare anything themselves. Then the table carries the feature, and it stays short on purpose.

**Difficulty.** M, about 5 days: the two readers 1.5, the table and its loader 1, the row action and the typed
form 1, checking each line on a device 1, strings, guide and tips 0.5.

**Deliberately out.** Results inside the panel (each service's own API, a login each). Apps that are not
installed. Sending a file or a text to an app (idea 14 of next-features-2.md).

---

## 3. Your own commands for other apps

**Promise.** Anything an app accepts can get a keyword of yours: a link with any scheme, or an intent you put
together in the window.

**Typed, and what happens**
- A link `sp` with the address `spotify:search:{argument}`: `sp daft punk` opens Spotify's search.
- A link `standup` with a `slack://channel?team=…&id=…` address: Enter opens that channel.
- An app command `pod`: App a podcast player, What "Play from search", the text as `query`: `pod the daily`
  asks that player for it.

**How it is built on what exists**
- *First half, half a day.* The link editor refuses anything that does not start with `https://` or `http://`
  (`window/Commands.kt`). It takes any address with a scheme, except `javascript:`, `file:`, `content:` and
  `intent:`. `Effect.OpenUrl` and `core/Templates.kt` stay as they are. When no app answers, the row stays and
  the footer says so (`Executor.run` already returns false).
- *Second half, about 3.5 days.* A fifth kind on the Yours page beside links, snippets, recipes and prompts:
  **App command**. Its editor: App (from the app list); What (Open a link, Send text, Search, Play from search,
  Custom action); Address; up to eight extras, each text, number or yes/no; and a **Try** button. It is saved
  as an `intent:` address with `{argument}` still in it, the way `AppCommands` keeps a file's template, and it
  runs as `Effect.Open(owner, intent)`. So the rule other apps' commands live under holds for the user's too:
  an exported activity of the chosen app, no grant flags, no clip data.
- A paste box reads an `am start …` line or an `intent:` address into the form (a small reader in `core/` with
  tests). People who build on the device have such lines at hand.
- `Recipes.step` gains the kind `open`, so a link with a custom scheme and an app command are recipe steps.

**To check on a device first.** A custom-scheme link from the panel. A number and a yes/no extra through
`Intent.parseUri` and back. What the Try button says for an activity that is not exported.

**Risks**
- A command that does nothing is the user's puzzle. Try, and a plain sentence ("Spotify has nothing that takes
  this"), are the help there is.
- An `intent:` address that arrives from outside (a pack, one day) must never run without a look. Packs are
  not built; the rule is written down now.
- Broadcasts are what some tools want (decision 7).

**Difficulty.** M, about 4 days: any scheme 0.5, the editor and its storage 2, the paste reader 0.5, the recipe
step 0.5, strings and the guide 0.5.

**Deliberately out.** Broadcasts and services. Sharing and importing commands (packs, A1). Recipes that take
text (A2).

---

## 4. Go, call, write

**Promise.** A short word and a place, a number or a name opens the right app with it filled in. Booklight
sends and dials nothing: the last click is yours.

**Typed, and what happens**
- `go hamburg hbf`: directions from here in Maps. `go berlin to hamburg by train`: both ends and the way of
  travelling filled in.
- `wa +49 171 2345678 running 10 min late`: WhatsApp's chat with that number, the text in the field.
  `tg anna on my way` for a Telegram user name. `sms` and `call` for the system's own apps.
- `meet` opens a new meeting; `meet abc-defg-hij` joins that one.

**How it is built on what exists**
- New in `core/`: `Reach.kt` with tests. It reads "from … to …", "by train / bike / foot" (German too), a phone
  number in the ways people type one, and the text after it.
- Three scopes in `scopes/`, each one line in `Scopes.kt`, each a preview row with slots as Mail has (`To`,
  `Text`; `From`, `To`, `By`).
- Effects: `Effect.OpenUrl` for the links (`https://www.google.com/maps/dir/?api=1&…`, `https://wa.me/…`,
  `https://t.me/…`, `https://meet.google.com/new`), sent to the app's package when it is installed. One new
  effect for the two requests that have no package: the phone app's `ACTION_DIAL` with `tel:` and the messages
  app's `ACTION_SENDTO` with `smsto:` and `sms_body`. Only Booklight's own code builds it.
- A keyword's row appears only when an app answers it on this device.
- `ACTION_CALL` is not used: it needs a permission and would dial by itself.

**To check on a device first.** Which of these apps exist and answer on a Googlebook; whether the text and the
number arrive filled in (intents.md, section 9, items 10 and 11). The `wa.me` and Meet forms are from memory.

**Risks**
- Little use on a laptop: the old catalogue called message links "Low to medium on a laptop" (P5).
- People have names, not numbers. Without the contacts permission the number is typed. A short list of your
  people kept in Booklight (idea 19 of next-features-2.md) would be the cure.
- A web link sent to no package opens the browser first. It is sent to the package.

**Difficulty.** M, about 3 days. `go` and `meet` alone: S, a day.

**Deliberately out.** Looking up contacts. Sending. Dialling. Choosing a chat inside the panel.

---

## 5. An app's own switches

**Promise.** An app's name and one word opens that app's page in the system: its notifications, its language,
what it opens by default.

**Typed, and what happens**
- `spotify notifications`: Settings on Spotify's notification page.
- `chrome defaults`: Chrome's "Open by default" page. `slack language`: the app's language page.
- `alarms` and `timers`: the Clock's two lists.

**How it is built on what exists**
- An app's row has typed verbs already (`core/Verbs.kt`, `AppsProvider`: "chrome uninstall", "chrome left").
  Three more verbs and three more actions, kept behind the row's arrow (`more = true`) so the six icons stay.
- One new effect with the Settings action and the package: `APP_NOTIFICATION_SETTINGS` (the package as an
  extra), `APP_LOCALE_SETTINGS` and `APP_OPEN_BY_DEFAULT_SETTINGS` (the package as `package:` data). All three
  are public Settings actions (intents.md, section 2).
- `alarms` and `timers` are two rows of `CommandsProvider` with the Clock's `SHOW_ALARMS` and `SHOW_TIMERS`.
- This is the old catalogue's L12, which was never decided.

**To check on a device first.** That each page opens on the app's own entry in a Googlebook's Settings
(intents.md, section 9, item 12). A battery page per app has no public action in the reference: not confirmed,
left out; App info is one click from it.

**Risks.** More verbs, more chances that a verb is also an app's name. The existing rule (the whole text is a
name first) covers it. The gain is small: it saves three clicks, not a task.

**Difficulty.** S, about 1.5 days.

**Deliberately out.** Switching anything by itself (notifications off, a default changed): no plain app can.
Pages per notification channel.

---

## The order, and what it adds up to

| Step | What | Days |
| --- | --- | --- |
| 0 | The device session (intents.md, section 9) | 0.5 |
| 1 | Any scheme in a link (feature 3, first half) | 0.5 |
| 2 | Play it there (feature 1) | 4 |
| 3 | Search inside an app (feature 2) | 5 |
| 4 | An app's own switches (feature 5) | 1.5 |
| 5 | App commands in the window (feature 3, second half) | 3.5 |
| 6 | Go, call, write (feature 4) | 3, or 1 for `go` and `meet` |
| | Play through the media session, if the device allows it | 2 |

The days are estimates from reading the code, not from having built any of it. Each feature needs its strings
in English and German, a line in the guide, and a look by the designers at its row.

**What would change the list.** If the device session shows that players let Booklight connect to their media
session, "Play it there" becomes much stronger (it plays without moving a window) and its second step moves
up. If it shows that Spotify plays from a search, decision 4 falls away. If it shows that the links of feature
2 mostly land on a home screen, feature 2 shrinks to the apps that declare a search themselves.
