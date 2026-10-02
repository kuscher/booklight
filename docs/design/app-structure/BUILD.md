# One structure for apps: the build

*2 October 2026. `ux.md` is the specification; this says what was built, in what order, where it differs from
`ux.md` and why, and what to check on a device. It compiles, the core tests pass and lint is clean.
**Nothing of it was run on a device.***

## What was built

Each step compiled and passed `./gradlew :core:test :app:assembleDebug` before it was committed.

1. **The words of the model** (`core/Model.kt`, `core/AppRow.kt`, `core/Play.kt`; `AppRowTest`, `PlayTest`).
   `Behind` (which of a row's two lists a line stands in), `Effect.OpenList` (a stop that opens its list), `Act`
   (Search, Play) on `Effect.EnterScope` and on `Query`, `Result.brief`, `AppChip` and `Door` beside `Scope`.
   `AppRow.arrange`: the order and presence of an app's actions and which list a line belongs to. `Chips`: the
   action a chip is entered with, and the other one. `Origin`: where Enter was pressed, and what Backspace
   restores. `Play.door`: which music app `play` goes to. `core/Stops.kt` (`StopsTest`): what the arming can rest
   on, where a typed line stands, which list a stop opens.
2. **Who leads a typed sentence** (`core/Lead.kt`, `core/History.kt`, `core/SearchEngine.kt`,
   `data/HistoryStore.kt`; `LeadTest`, `SearchEngineTest`). For each app one number in `history.json`
   (`leads`): the web leads until the app's row is picked once; two picks of the web's row running give it
   back; "Forget everything" clears it. The engine puts the app's row over the ways out or directly under the
   web's row, follows a door inside a scope, asks an app's chip for the armed action, and ends an app's chip
   with the web's row for the app's name and the text.
3. **The row and its two lists** (`providers/AppsProvider.kt`, `AppPages.kt`, `overlay/OverlayModel.kt`,
   `Panel.kt`, `Rows.kt`, `Footer.kt`, `ui/Icons.kt`, `DebugReceiver.kt`). Open · Search · Play · Window · arrow;
   Window's 14 lines and the arrow's (App info, the four pages, Don't suggest, Uninstall); a typed place in
   Window's slot, a typed page or `uninstall` in the arrow's; `OverlayModel.list` says which list is open; Tab
   only moves on a row that offers more than one thing (`tabEnters`); Right again on Window or the arrow opens.
4. **The app's chip, and the other doors** (`providers/AppChips.kt`, `AppSearch.kt`, `Dials.kt`, `Songs.kt`,
   `AppCommands.kt`, `Providers.kt`, `scopes/Scopes.kt`, `overlay/Field.kt`, `ChipLine.kt`, `Metrics.kt`,
   `OverlayModel.kt`, `OverlayActivity.kt`, `Guide.kt`, `window/Stage.kt`). One chip for an app with the app's
   icon; the armed action in the model (`act`), in the placeholder and on the one row; Tab between Search and
   Play; the lookup only under Play; the line under the empty field; Backspace back to where Enter was pressed;
   `play` and the built-in links as doors; the typed sentence as the same row; "Resume" and an app's own Search
   shortcut gone; suggestions held back only while the app leads; what a screen reader is told. (Planned as two
   steps; the chip could not be entered without its doors compiling, so they are one commit.)
5. **Words and papers** (`strings_30.xml`, the guide and tips in `strings_20.xml`, the Labs text in
   `strings_26.xml`; `ux-model.md` §18, `design-system.md` §16, `CLAUDE.md`, `docs/EXTENSIONS.md`,
   `CHANGELOG.md`, `PRIVACY.md`, `store-submission/forms/data-safety.md`).

6. **After a code review** (a second reader, by reading only): with "what other apps offer" switched off an
   app's chip is still there for Play; a line run from one of a row's lists counts as a use of its app (New
   window and the three places were icons before and counted; the ten other places did not, and now do); a click
   on the arrow while Window's list is open opens the arrow's; Ctrl and a digit on a keyword's row goes back to
   that row.

The commits, oldest first: `09e2485` (1), `00bba90` (2), `c3a2868` (3), `cbd13f4` (4), `777a539` (5), `23d52c1`
(the stops in the core, this paper), `4af5763` (6).

## Where it differs from `ux.md`, and why

- **Play on the row that searches** (§3, "Tab changes between them"): by key it is as written. By pointer, a
  pass over Play does not change the row (a lookup must not be sent by hovering), and a click on Play while
  Search is armed changes the row to Play and starts the lookup; the next click, or Enter, plays what was
  found. The same holds for Play on the typed sentence's row: Enter there makes the app the chip with the
  words and Play armed, and a second Enter plays.
- **`play`, Enter resumes** (§5) needed two things the paper does not name. `play` has no row of its own in
  the ordinary list any more (typing `pla` shows no "Play" hint row; `play` and Tab, or a space, still makes
  the chip). And the Media row leads for a whole `play`, `pause` or `stop`: without that, an app called
  "Play Store" stood above it and Enter opened the store.
- **`… on youtube music`** (§9): the chip follows the text. While the words name a music app the chip is that
  app's; the words stay in the field as typed; taken out again, the chip is the app played in last again. The
  words are not removed from the field as a keyword's are: a name has no key that ends it, and a chip made at
  the first matching letters would cut what the user is still typing.
- **Spotify as the chip merely for being first by name** (several music apps, none played in yet): the row
  reads "Play “…”" and nothing is sent until Enter on Play, which looks the name up and then plays. The same
  for a single letter, and where another row stands first (`play store`). This keeps the rule the privacy text
  already had: nothing goes to Spotify unless the user chose it.
- **Spotify without a key under `play`**: where another music app plays, `play` goes to that one. Where
  Spotify is the only one, it is the chip with Search armed (all it has), and "Add a Spotify key" waits behind
  the row's arrow.
- **The other music apps under `play`** (§9) have Play and nothing else on their rows. If one of them is
  Spotify with a key, it is looked up when the user moves to its row.
- **"Which song is this?"** stands behind the arrow under Search too, for an app that plays: the strip is then
  the same under both actions, as drawn. The typed sentence's row does not have it (no chip to ask under).
- **Backspace** (§3): "the action that was left" is the one armed under the chip at that moment. If Tab changed
  it under the chip, the app's row comes back with that one armed.
- **Right again on Window opens its list** (§2, as written), so the arrow is reached with Tab or Shift + Tab,
  not with Right.
- **A keyword of an app's own Booklight file** keeps the file's words on the app's row and on its row under
  the chip, as before ("Tools: merge"), where the paper says Search. Every other app says Search.
- **The row's description for a screen reader** is "Spotify, Open" as before, not "Spotify, app, Open".
- **The long title** (§9) is measured against 340 dp, the least room a title has, not against the row as it
  stands: else the words would change while the strip unrolls.
- **Under a keyword's chip** (`yt music daft punk`) another app's search comes first only where that app
  leads (§7's rule); else it stands under the chip's own row.
- **Only the Search action counts as a pick** for who leads: copying the web row's link teaches nothing.
- **At most two apps' rows** stand under the web's row (a name of two words has two readings).
- **An app's own shortcut** goes only if it is called exactly "Search" (in the device's language or in
  English) and the app has Search on its row.
- **"Copy link"** stays with "On the web" behind the arrow of a built-in link's row.
- **The web's row and a link's row** have the magnifier on their Search action (§6; drawn so in `rows.html`).

## Not done

- No picture was remade (`docs/design/captures/`, the store graphics) and the prototype
  `docs/design/booklight-next.html` still shows the old row.
- `docs/PICKING-UP.md` is not updated.
- The helper script's header (`bl`) does not list `key more|window`: `DebugReceiver.kt` and `CLAUDE.md` do.
- The window's Commands page shows the new guide lines; its demo stage was only given the app's icon on a chip.

## To check on a device

`./bl app`, then with the panel open (`./bl open stay`). `type` sets the text, `keys` types it a letter at a
time (a keyword becomes its chip at the space), `in` types under the chip that is there. In `dump`: `chip=`
is the chip with `:search` or `:play` after an app's, `via=` the keyword it came by, `line=` the action the
line under the empty field offers, `armed=` the armed action (`more`, `window` on a list's stop), `opened=`
the open row with `:arrow` or `:window`; a row's actions are in `<…>`, `+` behind the arrow, `+w` behind
Window, `*` its own default.

The five tasks of `ux.md` §4:

| Task | Keys | In `dump` |
| --- | --- | --- |
| Open an app | `type spo` · `key enter` | before Enter: `selected=0 armed=open`, row `Spotify [APP] <*open,search,play,places,window+w,full+w,…,pc+w,info+,notifications+,language+,defaults+,battery+,uninstall+>` (no `play` without a key) |
| Search inside an app | `type spo` · `key tab` · `key enter` · `in daft punk` · `key enter` | after Tab `armed=search`; after Enter `chip=appsearch:com.spotify.music:search … hint='Search Spotify' query=''`, no rows, `line=play` with a key; after the words `Search Spotify for “daft punk” [Spotify] <*search,play,which+>` (`which+` only where the device has a model) and the web's row last; Enter opens Spotify on its results |
| Play a named song | `type spo` · `key tab` · `key tab` · `key enter` · `in bohemian rhapsody` · `key enter` | after two Tabs `armed=play`; after Enter `chip=…:play line=search hint='Song, artist or album'`; then `Play “bohemian rhapsody” (Looking it up) [Spotify] <search,*play,which+>`, and a moment later `Bohemian Rhapsody (Queen · A Night At The Opera)`; Enter plays, the footer says "Sent to Spotify" |
| Left half | `type chr` · `key window` (or Tab to it) · `key enter` · `key down` · `key down` · `key enter` | `armed=window`; after Enter `opened=app:…:window selected=1`, 14 action rows from New window; after two Downs the pill is on Left half |
| Notification settings | `type spo` · `key backtab` · `key enter` · `key down` · `key enter` | `armed=more`; `opened=app:…:arrow`, lines App info, Notifications, Language, Open by default, Battery use, Uninstall |

The doors of §5:

| Door | Keys | In `dump` |
| --- | --- | --- |
| `spotify daft punk` in one go | `keys spotify daft punk`; then `key down` · `key enter`; then the same text again | first `chip=null`, rows `Search Google for “spotify daft punk” [WEB] … \| Search Spotify for “daft punk” [Spotify] <*search,play>`; after the pick the Spotify row is first; after Enter on the web's row twice it is second again |
| `play bohemian rhapsody` | `keys play bohemian rhapsody` | `chip=appsearch:<the app played in last>:play via=play`, the play row as above, the last row `Search Google for “play bohemian rhapsody”`; with two music apps the other one's row second |
| `play` and a space | `keys play ` | the chip, no rows, `hint='Song, artist or album'`; Enter does nothing |
| `play`, Enter | `type play` · `key enter` | row one `Media … <previous,*resume,next>`; Enter resumes; `pause`, `next`, `stop` as before |
| `yt lofi` | `keys yt lofi` | with YouTube installed `chip=appsearch:com.google.android.youtube:search via=site:yt`, row `Search YouTube for “lofi” [YouTube] <*search,…,web+,link+>`; without it `chip=site:yt` and the link's row |
| Backspace | under any app's chip with nothing typed: `key back` | `chip=null query='spo'`, the app's row selected, `armed=search` (or `play`) |
| An app's own shortcut | `type spotify` | no row "Search" with the label Spotify under the app |
| The user's links and commands; `go`, `meet`, `mail`, `timer`; a typed `spotify:track:…` | as before | unchanged |

Also worth a look, because none of it could be seen here:

- The strip on an app's row when a place is typed (`chr le`): the Window slot's icon and name change where they stand.
- Window's list on the 14-inch screen: 14 lines, the lower edge above the taskbar.
- The chip's arrival from a row (the icon in the row's icon column), and from a typed `play ` or `yt `.
- The line under the empty field: its height, its fade when the first letter comes, Tab changing its words.
- A held Tab under the chip: it stops before the other action.
- Tab from Play to Search before the pause is over: nothing goes to Spotify.
- A pointer over Play on the row that searches: no lookup.
- German: `fenster`, the strip's width with "Abspielen" armed, the line "In Spotify abspielen".
- A screen reader: Tab says "Search, 2 of 5"; the chip says the app and the action.
- `./bl debug guide`: every line `ok` (the `appsearch` example is now the app's name alone).
- `./bl debug appsearch` and `./bl debug players`, as before a release.
