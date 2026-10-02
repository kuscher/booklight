# Doing things with an app: the product paper

*PM seat, 2 October 2026. Read: the brief, `CLAUDE.md`, `docs/design/ux-model.md`, `CHANGELOG.md` (Unreleased), `docs/research/next-five.md` and `intents.md`, the pictures in `n5caps/`, and the code on `next-five`. Nothing was changed, no device was touched. A claim about today names where it was read.*

## 1. The jobs

| # | What a person wants from an app | How often | Where it is today |
| --- | --- | --- | --- |
| 1 | Open it, or go back to it | Many times a day | Enter. Right. |
| 2 | Find something inside it (a show, an artist, an app in the store) | Daily, in apps that hold things | Line 11 behind the arrow, or a typed sentence |
| 3 | Play something by name | Daily, for people who play music | Only the keyword `play`; not on the app at all |
| 4 | Pause, next | Daily for some | One Media row. Fine. |
| 5 | A second window | Weekly in a browser or Files; never in Spotify | Icon 2 on every row |
| 6 | Its window in a half, or maximised | Weekly for some, never for many | Icons 4, 5, 6 on every row |
| 7 | A task where the app is beside the point (`go`, `meet`, `mail`, `timer`) | Weekly | Keywords. Fine. |
| 8 | One of the app's own things (New tab, Incognito) | Weekly | Rows of their own, found by name |
| 9 | Its notifications, App info | Monthly | App info is icon 3; notifications only if you know the word |
| 10 | Thirds, quarters, centre | Rare; those who want them type them | Ten lines of the list |
| 11 | Remove it; take it out of "your usual" | A few times a year | Last in the list. Right. |

The row's six seats hold one daily job, one weekly, one monthly and three placements (`AppsProvider.result`); the two daily jobs after Open have no seat. That is the owner's own point: Search and Play matter, placement less. The plan knew it: `next-five.md`, decision 6, wanted Search as "an action on the app's row"; it was built behind the arrow, the six seats being taken. The same plan says of the app's settings pages that they "save three clicks, not a task", and of `call`, `sms`, `wa`: "little use on a laptop".

## 2. The diagnosis

**One job, many doors.** Searching inside Spotify has six: the typed sentence, Search behind the arrow, "Search in Spotify" on the `play` row, a link of your own, an app command, and Spotify's own shortcut, a second row "Search · Spotify" (brief; `AppSearch.kt`, `Dials.kt`). Booklight gives them four names: "Search", "Search Spotify for …", "Search in Spotify", "Search · Spotify" (pictures 10, 05). `yt lofi`, `youtube lofi` and Search behind YouTube's arrow are one job too; `appsearch.tsv` even ties `yt` to the app. Playing something in Spotify has four doors: `play`, a link ending in `:play`, a typed Spotify address, a copied link.

**Rules a person must know.**
- The app's whole name: `spotify daft punk` works, `spo daft punk` does not (`AppSearches.rows`). The app's own row is found from three letters.
- A keyword, `play`, and its grammar (`album`, `by`, `on spotify`).
- The arrow: Search is its eleventh line. Going forward that is six Tabs, Enter, ten Downs, Enter: eighteen keys after the name (`OverlayModel.stops`, `open`); the brief says eleven at the least.
- Four words shown nowhere: `notifications`, `language`, `defaults`, `battery` are on the row only once typed, because the places had made the list too long (comment in `AppsProvider.result`).
- A setting in another window: with a Spotify key in Labs `play` plays; without, the same row searches (`Dials.kt`).

**Where doors disagree.**
- Play is only a keyword, never on Spotify's row. Search is on the row and in the sentence, never a keyword. Whoever learns one cannot guess the other.
- A word after an app's name does one of three things. `spotify left` arms an action on the row. `spotify daft punk` makes a second row. `spotify notifications` does both (picture 11). And `spotify search` does not arm Search, the one action with no typed word (`AppsProvider.verbs`).
- The sentence opens the app for any text after its name, as row one when nothing local matches, and nothing is learned from it (`AppSearches.row`, `learnable = false`).
- Three chips for one kind of thing: "Play" for the keyword, the app's name for Search behind the arrow, the link's name for a link of your own (pictures 02, 18; `AppSearches.In`).

**The three that cost the most.**
1. **The row is in the order things were built, not the order they are used.** A daily job is eighteen keys away; a monthly one is in sight.
2. **Search and Play are the same kind of thing, reached in opposite ways, under four names.**
3. **What a word after an app's name will do cannot be seen**, and the one reading that takes Enter by itself is the wrong one for "netflix kündigen".

## 3. The principle

> **Pick the app, pick what to do, say what. A keyword is a short way to the same place.**

That is candidate (c), the app first.

- It is the owner's request in his own keys: Tab, Tab, Enter, type the name.
- It asks for nothing known beforehand: the row shows what the app can do. A keyword has to be remembered.
- It works for every installed app, without Booklight having a word for each.
- The verb stays where people think of the task first (`play`, `go`, `timer`), but it must land on the same chip, the same row and the same words as the app's way. `play` is "Play, in the music app you used last". `yt` is "Search, in YouTube".

The user meets two words: **the row** (an app and its **actions**) and **keywords**. "App command", "link with an app's address", "what apps offer" say how a thing was made; they are not names the panel uses.

## 4. The structure

**For every app: what you do in it is on the row; where its window goes, its settings and its removal are behind the arrow.** The order is fixed; an action an app does not have leaves no gap.

On the row: **Open** (armed) · **Search**, where the app can be searched · **Play**, where Enter can start something playing · **New window** · the arrow.

Behind the arrow: **Left half, Right half, Maximise, App info, Notifications, Don't suggest** (only among your usual), **Uninstall** (red, last). Seven lines at most, against thirteen today, which is close to the screen's height on the 14-inch device (brief).

| App | Its row |
| --- | --- |
| Spotify, with the user's key | Open · Search · Play · New window · ⌄ |
| Netflix, YouTube, Play Store; Spotify without a key | Open · Search · New window · ⌄ |
| A browser | Open · Search (if it declares one) · New window · ⌄ |
| A plain tool, a settings-heavy app | Open · New window · ⌄ |

**After Enter on Search or Play** the app becomes the chip, with the action's word, and the field waits. One row under it says what Enter will do. Search: Enter opens the app on its results; nothing leaves the device before that. Play: the row says what the player found and Enter plays exactly that, as the `play` row does today. The other of the two is the next action on that row, so a song Spotify does not find is one Tab from its search. Backspace on the empty field goes back to the app's row. One chip form and one mark for all of it (the app's icon is the mark, the action is the word); the designer draws them.

**Play is on a row only where it plays.** Spotify needs the user's key for that (`SEARCH_ONLY` in `Dials.kt`). No other player was tried on a device (`intents.md`: "One player was tried"), so each needs one look before its row promises Play.

**Every action has a typed word** after the name, as `left` and `uninstall` have. New, in English and German: `search` and `play`, with the text after them: `netflix search severance`.

**Window placement** leaves the row's icons and stays on the row's arrow. The list opens with its first line selected (`OverlayModel.open`), so "Chrome, left half" costs no more keys than today: Tab to the arrow, Enter, Enter, against four Tabs, Enter. All thirteen places stay as typed words. The brief's hard rule says placement stays reachable "by the row"; I read that as the job, not as all thirteen places (decision 2).

| Door | Today | Proposed | Why |
| --- | --- | --- | --- |
| The app's row | Open, New window, App info, three places; behind the arrow ten places, Search, Don't suggest, Uninstall | **Reorder**, as above | Seats by how often, not by when built |
| `spotify daft punk` | A row of its own; row one when nothing local matches | **Keep, never first** (§5) | It is also how people search the web |
| Search behind the arrow | Line 11 | **Merge** into the row, seat 2 | The owner's request |
| `play …` | A keyword with its own row and chip | **Keep as a shortcut**: the chip and row of Play on the last player | Two doors, one place |
| `go`, `meet`, `mail`, `event`, `timer`, `alarm`, `note`, `alarms`, `timers` | Keywords | **Keep as is** | The task comes first; the app is beside the point |
| `call`, `sms`, `wa`, `tg` | Keywords where an app answers | **Off until switched on** | Phone jobs on a laptop |
| `yt`, `store`, `maps`, `drive` | Links that open in the app | **Keep as shortcuts** to that app's Search: same chip, same row | Already the same place; make it look so |
| Your own links, with an app's address | In the window | **Keep**: the one way to make a keyword of your own | One editor, one field |
| App commands | A fifth kind in the window | **Move to Labs** | A link does the same for most; this is for people who paste `am start` |
| An app's own shortcuts, Booklight files | Rows of their own | **Keep**, minus one that repeats a row action ("Search · Spotify") | They are found by their own names; one Search per app |
| A typed app address; a copied Spotify link | A row; the copy's line | **Keep as is** | Pasted, not learned |

## 5. The typed sentence

**The rule: words after an app's name that are not one of its actions are ordinary text. Ordinary text goes to the web, and "Search Netflix for …" stands directly under the web row, one Down away. The order is fixed; nothing is learned.** (A local match by name still leads, as today.)

- `netflix stock price`, Enter: the web. `netflix severance`, Enter: the web too, Netflix's row under it.
- The sure ways into the app: `net`, Tab, Enter, `severance`, Enter; or `netflix search severance`.
- Why the web first: a wrong guess towards the web is a results page that still helps; a wrong guess towards the app raises another window on a useless search. And the row is the shorter way anyway: `net`, Tab, Enter is five keys, `netflix` and a space is eight.
- Why no learning: every sentence is new, so only "what this person does after this app's name" could be learned, and the same person types both kinds. A first row that flips between two destinations is what "your usual" was built to avoid: "Down, Enter means the same thing from one opening to the next" (changelog 2.3). The place has a precedent: a flight number that is only a guess stands under the ways out (ux-model §16).

**What we give up:** `spotify daft punk`, Enter, as built today. It becomes Down, Enter, or the row.

## 6. What I would cut or defer

1. **Maximise, Left half, Right half as icons on the row**: they become the first three lines behind the arrow.
2. **The ten finer places as lines**: typed only.
3. **App info on the row**: behind the arrow.
4. **`language`, `defaults`, `battery` as typed words**: out. App info leads to all three, and every reserved word is one more that cannot be searched for. `notifications` stays, as a line and a word.
5. **The sentence as row one** (§5).
6. **Play on Spotify's row without a key.** The `play` keyword still says "Search in Spotify" and still leads to where the key goes.
7. **"Which song is this?" behind the Play row's arrow**: shown only where Spotify found nothing, where it already moves onto the row (`Dials.kt`). The Play row then has no arrow in the usual case.
8. **`call`, `sms`, `wa`, `tg`**: off until switched on in Commands. `next-five.md`, decision 10, recommended `go` and `meet` and deciding the rest later; all were built.
9. **The app command editor**: to Labs.
10. **Deferred:** Directions on Maps's row, New meeting on Meet's row and the like (the keywords do it); an app's own shortcuts behind its arrow; any learning of which row leads.

## 7. Open decisions

| # | Question | Recommended | The other answer costs |
| --- | --- | --- | --- |
| 1 | Do the two halves stay as icons on the row? | No: first lines behind the arrow | Spotify's row has six icons again; Search and Play are still second and third |
| 2 | The ten finer places: typed only? This bends the hard rule as the brief words it | Yes | A list of fifteen lines or more, past the 14-inch screen; or the designer finds one line that holds all ten |
| 3 | `netflix severance`, Enter: the web or Netflix? | The web, Netflix under it, fixed | As today: a web search that starts with an installed app's name opens that app |
| 4 | Play on Spotify's row without a key? | Not shown | A seat called Play that searches, with "Add a Spotify key" beside it: easier to find, less honest |
| 5 | The app's settings words | `notifications` only, and listed | Three words nobody can see keep working and keep a reading away from search |
| 6 | `call`, `sms`, `wa`, `tg` off; app commands in Labs | Yes | Four more keywords to know of, and two editors in the window that make the same thing |

## 8. How we would know it worked

1. **Play a named song.** `s-p-o`, Tab, Tab, Enter, the name, Enter: seven keys and the name. `play`, a space and the same name show the same row in the same words.
2. **Find a show.** `n-e-t`, Tab, Enter, `severance`, Enter: six keys and the title.
3. **A web search stays one.** `netflix stock price`, Enter opens the browser; Netflix stays shut. `netflix search severance`, Enter opens Netflix on its results.
4. **Placing still works, and the list is short.** `chrome left`, Enter, as today. By the row: `c-h-r`, Tab to the arrow, Enter, Enter. No app's list has more than seven lines; Uninstall is last and armed only when typed.
5. **One job, one look.** Typing `spotify` shows one thing called Search. `yt lofi` and `y-o-u`, Tab, Enter, `lofi` show the same chip and the same row.
