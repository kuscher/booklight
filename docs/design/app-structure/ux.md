# Booklight: one structure for doing things with an app (UX)

*2 October 2026. Desk work from the brief, `CLAUDE.md`, `ux-model.md`, `design-system.md`, the code on `next-five` (`AppsProvider`, `AppSearch`, `Dials`/`PlayScope`, `Panel`, `OverlayModel`) and the captures. Nothing was run on a device. The drawings are in `rows.html`.*

## 1. The model

A row is one thing; an app's row is the app. Its actions are what can be done with that thing, in one fixed order for every app, shown as icons on the selected row with one of them unrolled and armed. An action either runs at once (Open) or first asks for the one thing it lacks: words (Search, Play) or a choice from a short list (Window, More). When it asks for words, the app becomes the chip in the field, so a chip always means "what you type now goes to this". The sentence a user needs: **type the app, Tab to what you want, Enter; if it takes words, type them and press Enter again.**

Three rules follow, and everything below uses them:

- **Tab only moves; Enter does.** Today Tab on an armed action that takes text enters it at once (`Panel.kt`, `entersScope`). On an app's row that would stop Tab at Search and never reach Play, so there Tab moves on and Enter enters. Tab keeps entering only where a row offers nothing else: a keyword's own row ("Search YouTube `tab`") and an exact keyword.
- **One act, one row.** "Search Spotify for these words" has one look, and "play this in Spotify" has one look, whichever way the user came.
- **Words leave the device only for the armed action.** Under Search nothing is sent before Enter. Under Play the name goes to Spotify after the pause in typing, as today.

## 2. An app's row

| # | Action | Which apps | Enter |
| --- | --- | --- | --- |
| 1 | **Open** | all | opens; armed unless a typed word arms another |
| 2 | **Search** | apps that can be searched (what `AppSearches` finds today: the app's own file, the table, a declared search) | the app becomes the chip |
| 3 | **Play** | music apps that play what is named: every player the system lists, except one known to only search; Spotify once the user's key is in | the app becomes the chip |
| 4 | **Window** | all | opens a list under the row: New window, Maximise, Left half, Right half, then today's ten places |
| 5 | **More** (the arrow) | all | opens a list: App info, Notifications, Language, Open by default, Battery use, Don't suggest (only among your usual), Uninstall (after its gap, red; not for apps that came with the device) |

**On the row:** two to four icons and the arrow, where today there are six. A calculator: Open · Window · arrow. Netflix: Open · Search · Window · arrow. Spotify: Open · Search · Play · Window · arrow. An app shows only what it has; nothing is dimmed to hold a place, and the order never changes.

**Where today's six icons go.** Open stays first. New window is line one behind Window. Maximise, Left half and Right half are lines two to four there, ahead of the ten places that were already in a list. App info is line one behind the arrow. Search leaves the arrow's list for second place.

**Why Window is one entry, and not the arrow or a second level.** One list holding everything would be 16 to 20 lines and does not fit the 14-inch screen (§9). Two short lists, each about one thing, do. Window is drawn and behaves exactly as the arrow does today: a stop like the others, Enter (or Right again) opens its list under the row, the pill lands on line one, Left closes, typing closes. Nothing opens inside a list, so there is no second level anywhere.

**Placement stays quick.** The typed words are unchanged: `chrome left`, `files right third`, `code full`, `… on display 2`. A typed place stands unrolled in Window's slot (today it takes the arrow's); a typed page (`spotify notifications`) or `uninstall` stands in the arrow's. By the row it is Tab to Window, Enter, Down, Down, Enter.

**A gain on the side:** the four Settings pages were kept off the list only for its height. In a list of seven lines they fit, so "Notifications" can be found without knowing its word.

## 3. After Enter on Search or Play

Use the existing pattern: `Effect.EnterScope` on the app's own scope (today's `AppSearches.In`), carrying which action was armed. One chip per app, not one per action.

- **The chip** is the app's own icon and its name. The icon sits in the column the row's icon was in, so the app is seen to move up into the field. It replaces the magnifier in today's app chip.
- **The hint** names what to type for the armed action: "Search Spotify" for Search, "Song, artist or album" for Play (both strings exist).
- **Nothing typed:** no row, and Enter does nothing (row one's Enter must run something, and there is nothing to run). For an app with both actions one line stands in row one's seat, built like the copy's line: the other action's symbol in the disc, "Play in Spotify" (or "Search Spotify"), a `tab` cap at the right. Netflix shows the field alone.
- **Typed, under Search:** row one is the app's icon, "Search Spotify for “daft punk”", and the strip Search (armed) · Play · arrow. Enter opens the app on its results. The last row is "Search Google for “spotify daft punk”", as in every scope.
- **Typed, under Play:** row one is the app's icon and "Play “bohemian rhapsody”" over "Looking it up"; when Spotify has answered, the song's name over "Queen · A Night At The Opera". The strip is Search · Play (armed) · arrow ("Which song is this?"). Enter before the answer waits for it, as built. Nothing found: "Spotify found nothing by that name", Play dimmed, the pill on Search. This is today's `play` row with two changes: its mark is the app's icon, not the note, and its action reads "Play", not "Play in Spotify" with an icon inside.
- **From Play to Search and back:** Tab (or Shift + Tab, Right, Left) on row one. Both actions are always on it, in the row's order, and its words follow the armed one. Before anything is typed, Tab on the empty field does the same and the hint changes. Going to Play starts the lookup; going to Search sends nothing.
- **Backspace on the empty field** is one step back to exactly where Enter was pressed: the letters as typed (`spo`, not the whole name as today), the app's row selected, the action that was left still armed. A wrong Enter costs one key, and the other action is one Tab away.
- **Escape** closes, as everywhere. Up on the empty field brings chip and words back (built). The Enter that entered does not also run (the 350 ms rule, built).

## 4. The count of keys

Keys pressed besides the app's first letters and the words themselves.

| Task | Today | Proposed |
| --- | --- | --- |
| Open an app | 1: Enter | 1: Enter |
| Search inside an app | 19 by the row: Tab ×6, Enter, Down ×10, Enter, the words, Enter (6 for someone who knows that Shift + Tab runs backwards). Or the app's whole name, Space, the words, Enter | **3**: Tab, Enter, the words, Enter. The typed form stays (§7) |
| Play a named song | 6 with the keyword: `play`, Space, the words, Enter. Not possible from the app's row | The same 6. From the app's row **4**: Tab, Tab, Enter, the words, Enter |
| Open an app in the left half | 5 by the row: Tab ×4, Enter. 4 typed: Space, `le`, Enter | 5 to 7 by the row: Tab ×1 to 3, Enter, Down, Down, Enter. 4 typed, unchanged |
| An app's notification settings | 4 typed, for someone who knows the word: Space, `no`, Enter. By the row only as far as App info (Tab, Tab, Enter), then the pointer in Settings | 4 typed, unchanged. **4 by the row**: Shift + Tab, Enter, Down, Enter (Tab forwards: 5 to 7) |

The one count that gets worse is placing a window by the row on an app that has Search or Play. The typed words are the quick way and do not change.

## 5. The other doors

| Door | Decision |
| --- | --- |
| `spotify daft punk` in one go | **Stays, as a shortcut.** It is the same row as under the chip (same icon, words and strip), in the ordinary list; no chip, because the name is already in the text. Where it ranks: §7 |
| `play bohemian rhapsody` | **Stays, as a shortcut.** At the space `play` becomes the chip of the music app last played in, Play armed: the chip and row of Tab, Tab, Enter. `album`, `by`, `… on spotify` stay as typed words. "play store" is still the Play Store |
| `play` + Space with nothing typed | The "Resume" row **goes** (nothing typed, no row). `play` and Enter still resumes, through the Media row. `pause`, `next`, `stop` stay: a control, not an app |
| `yt lofi`, `store …`, `maps …`, `drive …` | **Stay, as shortcuts.** Where the app is installed the keyword becomes that app's chip with Search armed; "On the web" waits behind the row's arrow. Where it is not, the link is as today, with a symbol |
| The app's own shortcut "Search · Spotify" | **Goes** for an app that has Search on its row: the same act with another look, and it takes no words. An app's other shortcuts stay as rows |
| "Search" behind the arrow | **Goes**: it is the second icon |
| The user's own links and app commands | **Stay.** They carry the user's name for them and are drawn by §6. They are not folded into the app's Search: what the user named is the user's |
| `go`, `meet`, `call`, `sms`, `wa`, `tg`, `mail`, `timer` | **Stay as they are.** The user names an act, not an app: the chip is the act, the row has a symbol and says in words which app takes it |
| A typed address (`spotify:track:…`), a copied Spotify link | **Stay.** "Open in Spotify" with the app's icon and the action Open |

No new typed words are added.

## 6. One rule for marks

**The mark is what the row is about.** If that is an app (its own row, a search or a play inside it, one of its shortcuts, a command or link that opens it, an address of its own), the mark is the app's icon as the launcher shows it. Anything else (an act with a keyword, a web search, a setting, an answer, a control) has a symbol in the disc, and names in words any app it hands to.

**An action always carries a symbol for what it does, and never an app's icon:** the app is already the mark. The armed action shows symbol, name and the Enter mark; the others their symbol; in an opened list, symbol and name.

This settles the three rules the review found. The `play` row takes the app's icon as its mark and loses the icon inside its action. `go` and `meet` keep their symbols. A chip follows the same rule: an app's chip has the app's icon, an act's chip a symbol.

## 7. The typed sentence

`netflix stock price` and `netflix severance` cannot be told apart, so the rule goes by what the user does, per app:

- **Without a chip, words after an app's name go to the web first.** "Search Netflix for “severance”" stands directly under the web row, with Netflix's icon: Down, Enter.
- **Pick that row once, and from then on the app leads** for words after its name. Pick the web row twice running and the web leads again.
- A local match for the whole text still leads both, as today. The Tab way always goes to the app and teaches nothing here.

This answers both reviews: Enter no longer opens an app's search for a sentence nobody meant for it, and the order can be learned both ways. It costs whoever already types `spotify daft punk` one Down, once.

## 8. What gets simpler, and what is lost

**Simpler**
- One sentence covers every app, and the row says it: use it, place it, the rest.
- Two to four icons instead of six, each a different kind of thing.
- Search inside an app goes from 19 keys to 3, and works from the app's first letters instead of its whole name.
- One row for a search inside an app and one for playing in it, from every door. "Resume", the app's own Search shortcut and the app icons inside `play`'s actions go.
- Two short lists instead of one long one; the Settings pages are visible; both lists fit the screen.
- One rule for marks. A sentence does what the user last taught it.

**Lost, or to relearn**
- New window: Tab, Enter becomes Tab (one to three), Enter, Enter. `chr new` is unchanged.
- Maximise, Left half and Right half are no longer icons: one to two keys more by the row on an app with Search or Play.
- App info: Tab, Tab, Enter becomes Shift + Tab, Enter, Enter (the same three keys, another shape).
- With two music apps, `play` shows one row for each instead of one row with two actions (§9).
- `play`, Space, Enter no longer resumes; `play`, Enter does.
- Spotify's empty search page can no longer be opened from Booklight; Open the app.
- `netflix severance` goes to the web until its row has been picked once.
- `yt` and `play` show an app's chip, not their own.

## 9. Edge cases, each decided

- **A very long name.** The strip never gives way: at most four icons and the arrow, about 265 dp at its widest (German, Play armed), so the title keeps 340 dp or more on the 720 dp panel and ends in its 24 dp fade. In the chip the name fades at 160 dp; the icon stays. In "Search … for “dune”" the app's name gives way, not the words: when the line does not fit it reads "Search for “dune”", and the mark and chip name the app.
- **Two music apps.** Both have Play on their own rows. `play` goes to the one played in last (the first by name the first time). Under its chip the other app's row stands second, with its own icon and the same Play; Enter there plays there and makes it the one for next time. `… on youtube music` at the end changes the chip to that app, as a keyword at the start becomes a chip. Coming from an app's own row, only that app is shown.
- **An app that can be searched in two ways.** One Search per app: the first that the installed app takes, in today's order (its own file, the table, a declared search). Further keywords of its file stay that app's own commands. For the four built-in links the web is behind the arrow.
- **An app's row that is not row one.** Down to it, then the same keys. An opened list replaces the whole list, as today, so its height does not depend on where the row stood. Backspace out of the chip returns to that row, selected.
- **"Your usual" at rest.** Nothing selected, no icons, Enter does nothing: unchanged. Tab or Down selects row one; the next Tab arms Search. "Don't suggest" stays the last line before Uninstall behind the arrow. Backspace out of a chip entered from there returns to the usual rows.
- **The opened list on the 14-inch screen** (1067 dp high, top at 213, about 678 dp for rows after field, pads, footer and taskbar). Window: 56 + 14 × 40 = 616 dp, lower edge at 949. More: 56 + 7 × 40 + 8 = 344 dp at its longest. Today's list is 584 dp, and 744 with the four pages, which is why they were left out.
- **German.** Öffnen · Suchen · Abspielen · Fenster · Mehr. The widest pill, "Abspielen ⏎", is about 131 dp. Hints: "Spotify durchsuchen", "Lied, Band oder Album". The line under the empty field: "In Spotify abspielen", "Spotify durchsuchen". `netflix kündigen` goes to the web by §7.
- **A held Tab** runs along the row and wraps, as today, and enters or opens nothing: only Enter does, on its first press. Under the chip, changing between Search and Play acts on the first press only, so a held Tab neither flickers nor sends two lookups.
- **The screen reader.** The row stays one item ("Spotify, app, Open"). Its custom actions, in the row's order: Open, Search, Play, every Window line by its full name ("Left half"), App info, the pages, Uninstall. Tab announces "Search, 2 of 5". Entering the chip announces "Spotify. Search. Type what to look for"; the chip's description says the action although it shows only the app. A change to Play is announced with its hint. A lookup's answer is announced once ("Bohemian Rhapsody, Queen. Play").

## What has to change in the code's rules

For the lead, not the owner: Tab on an app's row must not enter a scope (`entersScope` in `Panel.keys`, and Right); `EnterScope` carries the armed action; `AppsProvider` marks Window's lines and the arrow's lines as two groups (`more` is one flag today) and `OverlayModel.opened` says which is open; the app's scope produces the row for both actions and looks up only while Play is armed; `PlayScope` and the built-in links enter the app's scope instead of their own; the typed-sentence row keeps a pick count per app.

## Where the PM's paper differs

Read after this paper was written (`pm.md`). We agree on the order Open · Search · Play, on Play only where it plays, on `play` and `yt` as shortcuts into the same chip and row, on dropping the app's own Search shortcut, and on the web leading a typed sentence. We differ on five things:

1. **The row's end.** PM: New window stays an icon; the arrow holds the two halves, Maximise, App info, Notifications, Uninstall; the ten finer places are typed only. Here: New window and all thirteen places behind Window, the rest behind the arrow. The PM's is one list and one key less for New window and Left half; it takes ten places off the row, which the brief's hard rule does not allow as worded.
2. **The chip.** PM: the app's icon and the action's word. Here: the app's icon and name, with the action in the hint and on the row, so the chip does not change when Tab goes from Search to Play.
3. **The sentence.** PM: the web leads, fixed, nothing learned. Here: the web leads until the app's row has been picked once. The PM's is simpler and never flips; this one keeps `spotify daft punk`, Enter for whoever uses it.
4. **New typed words.** PM adds `search` and `play` after the name, with text after them (`netflix search severance`). Here none: a word followed by text is a grammar the typed words do not have today, and "Search Party" is a title.
5. **The Settings pages.** PM lists Notifications only and drops three words. Here all four are listed, since the list has room.
