# Booklight: the interaction model for 1.1, 1.2, 1.3 and 2.2

*UX model, 1 October 2026. Desk work from the 1.0 code, the plan and web research; no device was touched. Bracketed numbers are sources (§13). "Test first" marks a judgement call.*

## 1. The model

The panel stays one field over one list. The field holds at most one chip, then text. The selected row shows what it can do at its right end, with one action armed. Everything in the four releases is a row of one of four kinds: with several actions, taking text (a scope), holding a state (a control), or tall (a preview or a grid).

1. **A row** is one thing: an app, a setting, an answer, a control, a command's preview. Row one is selected; Up and Down choose.
2. **An action** is something a row does. A row has one to eight, in a fixed order, shown as chips on the selected row. One is armed and Enter runs it: the first, unless a typed verb arms another.
3. **A scope** is a row that takes text. Entering it makes it a chip in the field; the text after it is its argument. One chip at most; Backspace on an empty argument undoes it.
4. **An argument** is one line of plain text, parts separated by ` / `. The top row previews what was understood.
5. **A control** is a row with a state. Left and Right change it live; Enter commits and closes.
6. **Reversible things are live. Outward or permanent things wait for Enter**, are never armed by default and are never run by an arrow key.
7. **Tab moves to the next thing the selected row offers:** typing into it if it takes text, otherwise its next action. The row's right end shows which.

## 2. Acting on a row

| | For | Against |
| --- | --- | --- |
| **A. Chips in the row** | No change of view. PowerToys Run Tabs through such buttons [10]. | A row holds three labels; apps will have eight actions. |
| **B. A separate list (1.0)** | Room for any number; built; Alfred's Right-arrow list [19]. | A second view for one alternative. Extra steps are the complaint about Raycast and about Command Palette's Ctrl + K menu [5][12][13]. |
| **C. Hybrid** | Common alternatives one Tab away. | Two tiers. |

**Recommended: C, with the "more" step as the same row opening, not another view,** so actions are chips in the row from first to last.

- **Inline:** up to three chips at the right end of the selected row, in at most 340 dp; the title keeps at least 240 dp. Other rows keep their kind label.
- **More:** a quiet fourth chip, "⋯ 4". Tab past the third chip opens the row: it grows from 56 to 96 dp and the rest appear as a second line. It closes when the selection or the text changes.
- **Order** is fixed per kind, never reordered by use (a complaint about Raycast's lists: "there's no guarantee which order your options will be in" [5]). Apps: Open · New window · App info ‖ Left half · Right half · Store page · Uninstall. Answers: Copy · Copy with the sum. Links: Open · Copy link · Save as quicklink…
- **Labels:** the armed chip is a filled 28 dp pill with the Enter mark; the others are plain text at 0.80 ink. At most two words and 16 characters ("Deinstallieren" fits), never truncated: if three don't fit, two show.
- **Destructive:** last, on the second line, in `error` ink, filled with `errorContainer` when armed. Never the default, never run by Ctrl + digit.
- **Pointer:** hover shows the chips. Click a chip to run it, "⋯" to open the row, elsewhere for the default.
- **Screen reader:** the row stays one item, "Chrome, app, Open". Its actions are custom accessibility actions, Android's advice for list items with several actions [17]. Tab announces "New window, 2 of 7".
- **Keys:** Tab arms the next action and wraps; Shift + Tab the previous. Right at the end of the text does the same without wrapping; Left steps back to the first chip, then moves the caret again. Enter runs the armed action. Up, Down or typing re-arm the default.

## 3. Typed verbs

A verb moves the arming; it never makes a second row. "chrome uninstall" shows the Chrome row with Uninstall armed in error ink, so Enter's effect is visible.

1. The whole text is matched as a name first, as today.
2. Then a verb reading: the last word or two, or the first, is a verb, and the rest matches an app at "start of a word" quality or better.
3. At the end a verb may be abbreviated from two letters ("chrome inf"); Uninstall needs three. At the start it must be the whole word and a space, and it arms every app row ("uninstall ch"). A scope keyword wins at the start, so "new window" follows the name.
4. Score = the app's match × 0.95. An app matching the whole text at start-of-word quality ranks above every verb reading: "play store" opens Play Store, "open table" finds OpenTable. A verb with no object is not a verb: "info" finds apps called Info.
5. **Learning** stores typed text → row and action, only when the text contained the verb: "ch nw" picked once comes first next time, armed New window. An action reached with Tab teaches the row only, so "chr" + Enter always opens. Test first: whether people want "chr" to learn New window.

| Action | English | German |
| --- | --- | --- |
| Open | open, launch, start, run | öffnen, starten, start |
| New window (after the name) | new window, window, new | neues fenster, fenster, neu |
| Place, 1.3 (after the name) | left, right, full | links, rechts, voll |
| App info | info, app info, details | info, app-info, details |
| Store page | store, play, update | store, play, aktualisieren |
| Uninstall | uninstall, remove, delete | deinstallieren, entfernen, löschen |

English verbs work in German too; "loeschen" counts as "löschen".

## 4. Scopes and chips

**Entering,** two ways:
- **Tab** (or Enter) on a selected scope row, partial matches included: "you" → Down to "Search YouTube" → Tab.
- **Space** after a keyword typed exactly at the start. `yt lofi` typed straight through still works; the chip forms at the Space.

Chrome does both. It removed Space in Chrome 88 because ordinary words triggered by accident, restored it after the backlash, and now has a setting, as Arc does [1][2][14]. Booklight's keywords include ordinary words (`new`, `note`, `event`), so Space gets two guards: the last row in a scope is always "Search Google for" the whole typed text, and Backspace at the start of the argument turns the chip back into text. Test first: how often those three capture a web search.

**The field.** The chip sits where the G is, with the scope's icon and name, as in Chrome [3]; 1.0's chip at the right goes. The placeholder names the argument: "Search YouTube", "to  subject / message", "10m tea".

**Leaving.** Backspace on an empty argument, or a click on the chip, turns it back into its keyword as text, as Chrome does [4].

**Esc** closes at every depth, as decided (PLAN §11.4); only a waiting confirmation is cancelled first. Linear has the same split: "Back is never the same key as close" [15]. To make a stray Esc cheap, Up on an empty field brings back the last text that was not run, chip included, as in Alfred.

**Nothing typed yet:** the preview row with empty slots, then up to three things last done there. `new` lists its kinds; emoji shows recents; sites show nothing.

**Scopes** are everything that takes text after a keyword:
- Sites and quicklinks: `g` `yt` `w` `maps` `gh` `play` `drive`, the user's own
- Capture and make: `mail`, `note`, `event`, `remind`, `timer`, `alarm`, `ask`, `new`
- Answers: `emoji` `sym`, `qr`, `color` `farbe`, `password`, `snip`, `clip`
- Controls with a value: `vol`, `brightness`, `dnd`, `awake`
- Text sent from another app: no keyword; the chip holds the text, the rows are what to do with it

Not scopes, recognised as typed, like sums: `#3478f6`, `:3000`, `uuid`, `pause`, `next`, `mute`.

**Tab** follows rule 7: onward with the selected item, as LaunchBar's Tab sends an item on and Quicksilver's moves to the next pane [20]. It never "accepts" the grey completion; nothing needs it.

## 5. Arguments and previews

**One line, not a form.** Spotlight in Tahoe and Raycast use fields [6][7]; the complaint about Raycast is that extra step against Alfred's `define word` [5]. The preview row is the form: 92 dp, labelled slots that fill as you type, the slot under the caret marked, empty ones dim.

| Scope | Line | Examples | Preview row | Enter |
| --- | --- | --- | --- | --- |
| Mail | `to[, to] subject [/ message]`; a word with @ is a recipient | `mail anna@x.com Lunch? / See you at 1` · `mail anna@x.com, ben@x.com Q3 numbers` | To and Subject, the message below. Chips: Compose · Copy text | Gmail's compose window, filled in; nothing is sent |
| Note | `text [/ next line]` | `note buy milk` · `note Idea: quieter footer / try 0.6 ink` | "Add to Notes.md", the text, the time. Chips: Add · Keep · Copy | Appended; "Added to Notes" |
| Event | `when title [@ place]`, when first or last | `event Fri 3pm Dentist` · `event Standup tomorrow 9-9:30 @ Room 4` | "Fri 2 Oct, 15:00–16:00", title, place | The calendar editor, filled in |
| Reminder | `when text` | `remind 5pm call bank` · `remind in 20m stretch` | The time in full, the text | The reminder app, else an event |
| Timer | `duration [label]`; a bare number is minutes | `timer 10m tea` · `timer 1h30` | "10:00" large, label, "Rings at 14:12" | Starts; "Timer started" |
| New | `[kind] name`; an extension means file | `new file ideas.md` · `new folder Projects/Alpha` | "Documents/ideas.md · opens in Text Editor". Chips: Create · Create in… | Created and opened |
| Quicklink | all of it is `{argument}`, as in Command Palette [11] | `jira BOOK-12` · `gh booklight#12` | The finished address, the argument in strong ink | Opens it |

Text that was not understood stays in the title or message. A guessed slot (no time: "Today, all day") is dim. Test first: whether people find ` / ` untold.

## 6. Controls as rows

A control shows its state where other rows show their kind.

| Control | Shows | Left / Right | Enter | Typed |
| --- | --- | --- | --- | --- |
| Volume | 200 dp track, "40 %" | ∓ 5 % | Mute or unmute | `vol 40` |
| Brightness (2.2) | track, "60 %" | ∓ 5 % | Done | `brightness 60` |
| Media | ⏮ ⏯ ⏭; no state (unreadable before the power pack) | previous / next track | Play or pause | `pause`, `next`, `previous` |
| Do Not Disturb (2.2) | switch, "On until 15:00" | off / on | Switch | `dnd 1h` |
| Keep awake (2.2) | switch, "On until 16:00" | off / on | Switch | `awake 2h` |

- **Left and Right are live and the panel stays.** They act with the caret at the end of the text, so on a control row Left does not move the caret (Home does). Test first: whether that bites when fixing a typo.
- **Enter commits and closes** after 400 ms, enough to see the switch travel. Shift + Enter commits and stays, on any row.
- A typed value shows as a mark on the track; Enter moves the fill to it. Tab reaches the row's other actions (Sound settings).
- **A switch that needs a grant** shows its control hollow at 0.4, the subtitle "Needs one switch in Settings, once", and one armed chip, "Allow… ⏎". Enter opens that exact Settings page; Left and Right change nothing. Afterwards the same row is live. The looks follow Quick Settings tiles: on densest, off lighter, unavailable lightest [18].

## 7. Grids and rich results

Rows have three heights: 56, 92 (answer or preview) and 208 (picture). A tall row is always row one.

- **Emoji and symbols.** 14 columns of 48 dp cells, five rows, no scrolling: the best 70. Nothing typed: your 14 most recent (four-week frecency), then a fixed common set. All four arrows move one square highlight; the caret stays at the end. The footer names the cell ("Party popper"). Enter copies and closes ("Copied 🎉"); Shift + Enter copies, stays, and adds the next pick to the copied text. Raycast pastes [9]; Booklight can't before the power pack.
- **Colour.** 92 dp, a 52 dp swatch in the icon's place, the value large. Chips: HEX · RGB · HSL · OKLCH; Tab changes which is large and copied.
- **QR.** 208 dp: a 176 dp code on a plate that is white in both themes, the text beside it. Chips: Copy image · Save · Share. The panel stays open to be scanned.
- **Password, UUID.** 92 dp, monospace. Chips: Copy · New one (stays open). The copy is marked sensitive; no recents.

## 8. Confirmation

| Action | Beat |
| --- | --- |
| Uninstall | None of ours: Android shows its own dialog |
| Delete a snippet, quicklink or recipe; Forget everything | Second Enter |
| Send through a relay or Gmail (2.1) | Second Enter, on a preview showing recipient and text |
| Mail, event, Keep note | None: an editor opens; nothing is sent or saved yet |
| Local note, timer, new file, switches | None: cheap or reversible; the footer says what happened |
| Recipe | None; its subtitle lists its steps. With an outward step (a webhook, 2.1): second Enter every time |

**Enter arms, Enter again does.** The chip widens in place to "Press ⏎ again to delete" in `errorContainer`, and a line along its lower edge drains over 3 s. The second Enter must be a new key press at least 350 ms later, so a bounce can't confirm. Any other key cancels; Esc cancels without closing. By pointer: a second click on the chip. Raycast uses a modal alert [7], a second surface in a panel that has one.

## 9. Making your own

**Recommended: write in a small editor window, start from the panel.** Names, keywords, placeholders and ordered steps are form work; VS Code says of its own palette that it isn't suited to be "a wizard" [16]. Minimum for 1.3:

- **Quicklinks.** Extend 1.0's keyword-search editor: Name, Keyword, Link (an address or an `intent:` URI) with `{argument}`, `{clipboard}`, `{date}`. Any link row has "Save as quicklink…", which opens the editor filled in; `new quicklink` fills it from a copied address, as Raycast does [8].
- **Snippets,** the one kind a line can hold, in the panel: `snip add sig / Best, Alex` previews "Save snippet sig"; `snip sig` copies it.
- **Recipes.** In the editor: Name, Keyword, steps. A step is picked with a Booklight field inside the editor, so it is "a row and one of its actions": open an app in its place, open a link, set the volume. `new recipe` opens it.
- **Every user-made row:** Run · Edit… · Delete. Edit… opens the editor at that item; Delete takes the second Enter.

Test first: whether anyone builds a recipe without a "save what I just did" shortcut.

## 10. Key map

"At the end" means the caret is at the end of the text. The columns follow the selected row: a control row inside a scope (`vol 40`) behaves as a control row.

| Key | Results | In a scope | Control row | Grid | Confirmation |
| --- | --- | --- | --- | --- | --- |
| Typing | Filters; row one; default armed | Edits the argument | As results; a number is the target | Filters; first cell | Cancels, then types |
| Space | After an exact keyword at the start: enters the scope. Else a space | A space | A space | A space | Cancels |
| ↑ ↓ | Move the row. ↑ on an empty field: last text | Move the row | Move the row | Move the cell | Cancel, then move |
| Tab · Shift + Tab | Scope row: enter it. Else next · previous action, wrapping | Next · previous action | Next · previous action | Next · previous action | Cancel |
| → at the end | Next action, no wrap | Next action | Up a step · on · next track | Next cell | Cancels |
| ← | Previous action; on the first chip, the caret | The same | Down a step · off · previous track | Previous cell | Cancels |
| Enter | Runs the armed action (a scope row: enters it) | Runs it | Commits, closes after 400 ms | Copies, closes | Confirms, after 350 ms |
| Shift + Enter | Runs, stays open | Runs, stays | Commits, stays | Copies, stays, appends | – |
| Ctrl + 1…9 | That row's default; never a destructive one | The same | The same | – | Cancels |
| Backspace on empty | – | Chip back to its keyword | – | As a scope | Cancels |
| Esc | Closes | Closes | Closes | Closes | Cancels only |
| Summon key · click outside | Closes | Closes | Closes | Closes | Closes |
| Click | Row: default. Chip: that action. ⋯: opens the row | Field chip: leaves | Drag the track; click the switch | Cell: copies | Same chip: confirms |

## 11. Motion intent

Nothing arrives at full size in one frame. The field stays put, the window's height follows its spring, text changes by cross-fade or roll. Everything cuts when the system's animations are off.

- **Entering a scope.** The typed keyword does not vanish: its letters slide left to the mark's place while a pill grows around them, the label cross-fades from "yt" to "YouTube" as the pill widens, and the G shrinks into the chip's icon. The caret travels left to the start of the argument. The selection pill stays on row one while its content cross-fades into the preview; other rows fade where they are.
- **Cycling actions.** One small highlight, kin to the selection pill, glides from chip to chip, leading edge first, carrying the Enter mark. Chips stay put; only their ink changes. When the row opens, the pill's lower edge leads downward, rows below ride down, and the second line of chips rises in left to right.
- **A row becoming a control.** On selection the state label stretches into the track, which draws from the left up to the present level; the thumb arrives last. On Left or Right the fill springs and the number rolls like an answer.
- **A grid arriving.** Rows fade, the height springs, cells come in as a diagonal wave from the top left (8 ms a step, 120 ms at most), each rising 8 dp. One square highlight glides both ways, stretching towards its target.
- **A confirmation.** The armed chip widens leftward from its fixed right edge, its fill cross-fades to `errorContainer`, the label rolls, the line drains; the row's other chips dim. Nothing else moves. A deleted row folds to zero height and the rows below close up.
- **Leaving a scope.** The reverse: the pill tightens as its label cross-fades to the keyword, the letters slide back to the text's start, the caret follows, the G grows back, rows cascade in.

## 12. Open decisions

1. **Space enters a scope after an exact keyword?** Recommended: yes, and Tab. Alternative: Tab only.
2. **Esc inside a scope?** Recommended: closes. Alternative: first leaves the scope.
3. **More than three actions?** Recommended: the row opens. Alternative: 1.0's list (cheaper).
4. **Controls live on selection?** Recommended: yes. Alternative: Tab into the control first.
5. **Uninstall** needs `REQUEST_DELETE_PACKAGES`, a new install-time permission. Recommended: add it, with no confirmation of ours. Alternative: keep "App info".
6. **Arguments?** Recommended: one line with ` / `. Alternative: fields with Tab between them, as in Spotlight.
7. **Learning actions?** Recommended: only from typed verbs. Alternative: also from Tab, so "chr" can come to mean New window.
8. **Editing your own commands?** Recommended: the editor window. Alternative: in the panel.

## 13. Sources

Fetched 1 October 2026 unless noted.

1. Chrome 88 removes Space for custom searches; reason and reversal: https://www.androidpolice.com/2021/02/16/chrome-88-disables-space-bar-shortcut-for-custom-search-engines-but-theres-a-fix/
2. Chrome's "Space or Tab" / "Tab" setting: https://techdows.com/2021/05/chrome-to-provide-a-setting-to-select-keyboard-shortcut-for-custom-searches.html
3. Keyword then Tab; the site's name becomes a label on the left: https://www.howtogeek.com/24227/complete-guide-to-keyword-shortcuts-to-search-sites-in-google-chrome/ · `@tabs`, `@bookmarks`, `@history` (6 December 2022): https://blog.google/products/chrome/search-your-tabs-bookmarks-and-history-in-the-chrome-address-bar/
4. Backspace on an empty field restores the keyword: my own use of Chrome. The only written account I found describes a Chromium fork (search summary, not opened): https://github.com/BenItBuhner/Zenium/pull/267. Chromium issue 40153420 needs a sign-in and was not read.
5. Alfred and Raycast compared first-hand: https://joshcollinsworth.com/blog/alfred-raycast
6. Spotlight in Tahoe: Tab and Shift + Tab between parameters, quick keys, app name then Tab: https://macmost.com/how-to-use-spotlight-actions-in-macos-tahoe.html · https://www.macrumors.com/how-to/do-more-with-spotlight-in-macos-tahoe/
7. Raycast: https://manual.raycast.com/action-panel · https://developers.raycast.com/information/lifecycle/arguments · https://manual.raycast.com/command-aliases-and-hotkeys · https://manual.raycast.com/search-bar · https://developers.raycast.com/api-reference/feedback/alert
8. Raycast quicklinks: https://manual.raycast.com/quicklinks
9. Raycast emoji grid: https://manual.raycast.com/emoji-symbols
10. PowerToys Run, "Tab through context buttons", prefixes: https://learn.microsoft.com/en-us/windows/powertoys/run
11. Command Palette: prefixes, and bookmarks that ask for placeholder values in the search line: https://learn.microsoft.com/en-us/windows/powertoys/command-palette/overview
12. Command Palette's Ctrl + K menu, a request for faster selection (search summary, not opened): https://github.com/microsoft/PowerToys/issues/39140
13. "Every single action in Alfred takes at least 1–2 fewer keypresses" (search summary, not opened): https://talk.macpowerusers.com/t/alfred-vs-raycast/44100
14. Arc site search, Tab or Space by setting (the page returned 403; search summary): https://resources.arc.net/hc/en-us/articles/20855018192791-Site-Search-Directly-Search-any-Website
15. Command palette teardowns; Linear's back and close keys: https://www.setproduct.com/blog/command-palette-ui-design-guide
16. VS Code: https://code.visualstudio.com/api/ux-guidelines/quick-picks · https://code.visualstudio.com/docs/getstarted/userinterface
17. Android: https://developer.android.com/develop/ui/compose/accessibility/semantics · https://developer.android.com/develop/ui/compose/components/chip · https://developer.android.com/develop/ui/compose/components/segmented-button
18. Quick Settings tile states: https://developer.android.com/develop/ui/views/quicksettings-tiles
19. Alfred: https://www.alfredapp.com/help/features/universal-actions/ · https://www.alfredapp.com/help/workflows/inputs/script-filter/json/ · https://www.alfredapp.com/help/getting-started/cheatsheet/
20. LaunchBar, Tab sends an item to a target: https://www.obdev.at/resources/launchbar/help/SendingItems.html · Quicksilver's three panes: https://en.wikipedia.org/wiki/Quicksilver_(software)

Not found: a written source for Quicksilver's Tab between panes (its manual pages returned 404; that part is from memory), Material's own chip guidance (the page did not render; the Compose pages stand in), and anything on Slack beyond its search filters.

## 14. What 2.0 added to the model (1 October 2026)

The table of keys for 2.0 is `reviews-2.0/ux.md` §5. The rules that are new, as built:

- **A row's other actions** (replaces "More" in §2): the arrow is a stop like the others. Moving the arming
  never opens anything; Enter on the arrow, Right again, or a click opens the list. In the list ↓ ↑ and Tab move,
  ← closes, Enter runs, typing closes it in the same frame. Uninstall is last there, apart, in the error colour.
- **Typed places** (§3): `chrome top left`, `files right third`, `code full`, `… on display 2`. The longest
  verb wins ("top left" before "left"). A last word of one letter that begins a verb leaves the row standing.
- **Tab on a keyword** (§4): when the text is exactly a keyword and the selection is where typing left it, Tab
  makes it the chip. Once the selection or the arming was moved by hand, Tab is the row's again.
- **One-letter keywords** rank under everything local that matched: `s`, Enter opens the app; `s`, Tab is
  Settings. A link of the user's called `s` or `k` takes the letter; `settings` and `keys` still work.
- **`?`** as the first character is the list of everything at once. `help` finds its row and is not a keyword.
- **Whatever fills the field for you** (a scope entered from its row, Try it, a Commands row): the next Enter
  runs only as a new press 350 ms later, and text Booklight typed is not kept as the last text.
- **A prompt's row** is answered after a pause of half a second with three letters or more; Enter asks at once;
  Enter while the answer arrives waits for all of it, then copies. The model's rows never run anything.
- **A task** stays where it is when ticked; Enter again unticks it. Ticked tasks are gone the next time.
- **Tips**: nothing armed at rest; Tab, then Enter. Typing puts the card away in the same frame.
- **The window**: Tab and Shift + Tab go between the column and the page; ← from a row without a control goes
  to the column; ↑ ↓ in the column change the section at once; → or Enter goes into the page.
- **The pin never has the keys.** Copy and Unpin are in the panel, on the row `pin` shows.

## 15. The copy (M1, October 2026)

The plan and its drawings: `milestones/plan.md` §4 and `milestones/04-design.md` §3. As built:

- **One line under the empty field.** Something copied in the last two minutes gets one line a moment after
  the panel has opened: "Copied 20 s ago · a link and a date". It is not a row: nothing is selected, Enter does
  nothing, and there is no footer. Tab, Down or a click opens it. Tab on the empty field opens a fresh copy
  whether the line is there or not: before it has come, after typing put it away, or with the line switched
  off. A typed letter takes the line away, and it stays away for that opening. First run's
  stage has the place before it; it has the place before a tip. No line for a copy marked private, for a copy
  Booklight made, or with the switch off (the window › What you copied).
- **What Booklight knows before Tab** is the system's description only: that it is text, how old it is, and
  which kinds of thing the system found (it looks at copies of up to 400 characters; a longer one is "text").
  The age is said once, as it was when the panel opened.
- **Tab makes the copy the chip.** Under it: the translation first when the text is not in the app's language;
  what was found in it, one row for each kind and three at most (a link: Open, Copy clean; a date: the event
  row, filled in; a phone number: Call, Copy; a mail address: Compose, Copy; a flight number: the airline's row,
  looked up only when the user goes to it); the first three prompts, of which
  the ready-made translation knows its direction; Summary for a text of 1,000 characters or more.
- **Typing under the chip** narrows the rows by their names and finds the rest (Note, Mail, QR code, the other
  writings, a web search, Gemini). Rows are found by what they are called, never by the copied text, and not by
  scattered letters. A language's name is the translation into it ("danish", "in danish", from four letters, or its
  tag; under four letters a row whose name starts that way comes first).
  What matches nothing is an instruction: "With what you copied".
- **A model's row is answered where it stands.** Enter asks, and only Enter: the row takes the first place at an
  answer's height, the others go, the answer is written in, and the chip stays the copy. Enter then copies;
  Pin and Gemini are beside it. Backspace on the empty field goes back one step for each press (a held key stops at
  the empty field): from the answer to the rows, from the rows to the line, which says the age it said before. A prompt typed by its own keyword (`fix …`, `tr …`) is still asked after a pause,
  as in 2.0.
- **How much the model takes.** A rewrite or a translation of up to about 1,800 characters is answered in the
  row; a summary may read 8,000. A longer text hands over to Gemini: the row says "Gemini".
- **`tr`**: `tr danish see you on Saturday`. A language, then the text; with no text, what was copied. A
  language the table does not hold, with a text after it, hands over to Gemini.
- **`clip`** opens the same list at any age, and says "What you copied is marked private" instead of reading
  such a copy. **Text another app hands over** (its selection menu, its share sheet) gets the same list and
  the same answers in place; where the text came from a field that takes it back, Replace comes first.

## 16. What flights added to the model (M5, 2 October 2026)

The plan is `milestones/plan-next.md` §4; the facts about the service are `research/flights.md` §10. The row was
redrawn for 3.0 (`design/flights-row/`, approved by the owner on 2 October 2026; how it looks is
`design-system.md` §14). As built, the new row not yet seen on a device:

- **What the row answers** (3.0). One thing per phase, as its headline: when it leaves ("Leaves Fri 10:55 AM");
  from three hours before, how long until it does ("Leaves in 42 min"); in the air, how long until it lands
  ("Lands in 4 h 07 min"); for three hours after, how long ago it landed; "Cancelled"; "Diverted"; for a
  timetable's flight past its time, "From the timetable". Beside it one badge says whether that is the plan: "On
  time", "Delayed 27 min", "24 min early", after landing "27 min late", "Planned" when only the plan is known
  (green, amber, neutral: the one place a row has a colour of its own). Late starts 15 minutes after the plan.
  Under them the flight as a line from take-off to landing, the plane at the share of the flying time that has
  passed, and under the line's two ends the airport, its time, and what matters there now (gate and terminal
  before it leaves; the aircraft, and the arrival's terminal and belt, after). No plane where nobody knows where
  it is. A field the service did not send is simply not said.
- **It follows the clock, and asks nothing for that.** While a row whose headline counts minutes is on screen,
  it is said again at each whole minute from the answer that is kept: the number changes and, in the air, the
  plane takes its step, forwards only. One wake a minute; no request. A later answer that puts the plane further
  back leaves it where it is until the clock has caught up.
- **A row that answers from elsewhere.** A flight's row stands in its full height (136 dp), with its line and its
  actions, from its first frame, and the answer is written into it. The panel asks once, 400 ms after the last
  key; a new letter before that sends nothing. One request at a time; a request that was sent finishes and is
  kept (two minutes) even if the text moved on, so the lookup it cost is not lost.
- **A guess is the last row.** Text that only looks like a flight number (`ps5`, `ms 365`, `q4 2026`) gets a
  plain row under the ways out to the web, and nothing is sent for it. It is the one kind of row that stands
  under "Search the web". Going to it (Down onto it, Tab on it, a click) makes it a flight's row and looks it
  up at once: it grows where it stands, once, from an ordinary row into the tall one. A pointer passing over it
  is not going to it.
- **Never above a local match.** A flight's row ranks under every app, sum, setting and link that matched, and
  over the web search.
- **An action that waits.** Copy, Pin and Add to calendar stand in the row before the answer does. Enter on
  one of them while the answer is on its way runs it when the answer is in (as Enter on an answer being
  written waits for all of it).
- **An action that is off.** When no answer came, Copy and Pin keep their places, dimmed; Tab, the arrows and
  the pointer pass over them. Enter still opens the flight's page. When the key was refused, "Booklight
  settings" stands where they stood. A row that says "No connection" or "No answer this time" is asked again
  when the user goes to it, once ten seconds have passed (`FlightsProvider.retry`).
- **What is not offered.** "Add to calendar" is there only for a flight that is still to come or in the air,
  and whose times are sure to the hour: not for one that has landed, was diverted or cancelled, not for a
  timetable's flight whose time has passed, and not for one dated after a clock change at either airport (the
  timetable gives no zone, so the event could be an hour out). Pin is dimmed for a timetable's flight whose
  time to leave has passed.
- **Who said it.** While a row that was answered from elsewhere is selected, the footer's left end says who
  gave the answer and when ("AirLabs · 13:58"), as quietly as it names a grid's cell. A word that just
  happened ("Copied") takes its place for its moment.
- **The keyword** `flight` (German `flug`): after it, whatever reads as a flight number is one. With a key
  in, the row stands empty under the chip before the number is complete.
- **A day after the number** (`LH455 fri`, `LH455 tomorrow`, `LH455 3.10.`) is that day's flight; a weekday
  that is today is today. A day is taken from three letters on. While it is being typed (one or two letters,
  or the beginning of a day's word: `LH455 s`, `LH455 mo`, `LH455 tomor`) the number's own row stays, so the
  answer does not go away for a key and nothing is asked for a day nobody meant. A day beside a time is its
  weekday within six days of today, and its date beyond ("24 Dec").
- **A timetable's flight goes by the clock.** It has no state of its own. While its time to leave is still to
  come it is a plan: "Leaves …", the badge "Planned", the plane at rest at the start (it has not left: that much
  is certain). Once that time has passed nobody knows what became of it: the headline says only "From the
  timetable", there is no badge and no plane, and its two times stand as the timetable's.
- **The pinned flight** follows one flight, the number on the day it leaves, never "the next" of its number.
  It shows the flight's line small under its figure, and whether it runs to plan as one word in its colour. It
  counts by itself, a minute at a time (the plane moves with the figure, by the clock), and asks the service
  again while its window is on screen: every
  half hour from three hours before it leaves until it has landed, every three hours before that, and not at
  all for a timetable's plan more than ten hours off. One request each time. A flight that was to land more
  than three hours ago says "Landed" and asks nothing, whatever was last heard of it; when the service
  answers with another day's flight, the asking ends. It is the one place where Booklight asks with the panel
  closed.
- **No key, no request.** Without a key the row is an ordinary one that names the airline; its last action,
  "Set up times", opens the Booklight window on the row where a key goes.

## 17. Your usual (issue 1, 2 October 2026)

The design: `zero-state.md`. Behind the switch "Show your usual", off unless chosen. As built:

- **What stands under the empty field** is one thing at most, in this order: first run's stage (§19), the line for a
  fresh copy, your usual, a tip (`Under.choose` in `core`, with its truth table as a test). The usual rows come
  as the glass lands when it is already known that no copy is fresh, else a moment later with the line and the
  tip, and never after that.
- **The rows** are the two things run most from Booklight (a faded count of 1.5 or more: run twice, for eleven
  days) and the one run last in the past eight hours. Two or three, never one. Each is one line.
- **Nothing is selected.** Enter does nothing. Down, Tab, a pointer that has moved 4 dp on a row, or Ctrl + digit
  choose. Up from row one goes back to rest; Up at rest brings the last text back; each needs a press of its own.
- **A letter** is typed at once; the rows stand until the typed list lands, and a thing that is in both keeps
  its row. When the field is empty again the same rows stand again.
- **Don't suggest** takes the row out, and the panel is back at rest. It is not a run, and nothing is deleted.
  On an app it stands in the list behind the arrow, the last line before Uninstall; on any other row it is the
  last icon before anything that removes. Ctrl + digit never runs it.
- **One line each.** A thing whose row has a second line (a link, a recipe) is not suggested in this version.
- **What counts as a run** (for every list): any action that is not dangerous and is not App info, Edit or Delete.

## 18. One structure for apps (2 October 2026)

The design: `app-structure/ux.md` (with the drawn rows in `rows.html` and the five decisions in its
`README.md`); how it was built and where it differs: `app-structure/BUILD.md`. It replaces what §2, §3, §4 and
§14 say of an app's row, of `play` and of the built-in links. Built, not yet judged on a device.

The sentence: **type the app, Tab to what you want, Enter; if it takes words, type them and press Enter again.**

- **An app's row** is Open · Search · Play · Window · the arrow, in that order for every app. An app shows only
  what it has (a calculator: Open · Window · arrow), and nothing is dimmed to hold a place. Search: an app that
  can be searched. Play: a music app that answers Android's "play from search" and is not known to only search;
  Spotify once the user's key is in. The order and what stands where is core `AppRow`.
- **Two lists, never one inside the other.** Window is a stop like the arrow: Enter, or Right again, opens New
  window, Maximise, Left half, Right half and the ten other places under the row; the arrow opens App info, the
  app's four pages in Settings, Don't suggest (among your usual) and, after its gap and in red, Uninstall. Left
  closes, typing closes. Right again on Window opens it, so the arrow is reached with Tab.
- **Typed words are unchanged** (§3): a typed place stands unrolled in Window's slot (`chrome left`), a typed
  page or `uninstall` in the arrow's (`spotify notifications`).
- **Tab only moves; Enter does.** On a row that offers more than one thing, Tab and Right go along its stops and
  enter nothing: Enter enters Search and Play, and opens Window and the arrow. Tab still types into a row that
  offers nothing else (a keyword's own row, "Search YouTube"), and makes an exact keyword the chip (§14).
- **After Enter on Search or Play the app is the chip**: the app's own icon and its name, one chip for an app
  whichever action is armed. The placeholder names what to type ("Search Spotify", "Song, artist or album").
  With nothing typed there is no row and Enter does nothing; for an app with both actions one line stands in
  row one's seat and offers the other ("Play in Spotify", a `tab` cap), built like the copy's line. Typed text
  gives one row whose strip is Search · Play · arrow; its words follow the armed one. Tab changes between them:
  going to Play starts the lookup (after its pause), going to Search sends nothing. A held Tab does not change it.
- **Under Play** the row is the play row: "Play “…”" over "Looking it up", then what Spotify found, its artists
  and album; Enter before the answer waits for it. Nothing found: Play is dimmed, Search is armed in its place,
  and "Which song is this?" waits behind the arrow. The mark is the app's icon; the action reads "Play".
- **Backspace on the empty field** is one step back to where Enter was pressed: the letters as typed (`spo`),
  the app's row selected, the action that was left still armed (core `Origin`). From one of the usual rows: back
  to the usual rows. Escape closes; Up on the empty field brings chip, action and words back.
- **The other ways in end in the same chip and row.** `play` and a space is the chip of the music app last
  played in (the first by name the first time), Play armed; `album`, `by` and `… on spotify` stay, and a named
  app is the chip. With several music apps the others stand under it, each with its icon and Play. `yt`, `store`,
  `maps`, `drive` are their app's chip with Search armed where the app is installed, with "On the web" behind
  the row's arrow; else the link as before. `play` alone has no row of its own: it is the Media row, and Enter
  resumes. The "Resume" row, "Search" behind the arrow and an app's own shortcut called Search are gone.
- **The typed sentence** (`netflix severance`): without a chip, words after an app's name are a web search
  first, with "Search Netflix for “…”" directly under the web's row. Pick that row once and the app leads for
  words after its name; pick the web's row twice running and the web leads again (core `Lead`, kept with what
  is learned). A local match for the whole text leads both. Suggestions are held back only while the app leads.
  The row under a chip teaches nothing.
- **One rule for marks**: the mark is what the row is about. An app's icon where that is one app (its row, a
  search or a play in it, its shortcuts, a command or link of the user's that opens it, an address of its own);
  a symbol in the disc for anything else. An action has a symbol for what it does, never an app's icon.
- **Screen reader**: the row is one item; its actions are Open, Search, Play, every line of Window by its own
  name, App info, the pages, Uninstall. Tab says "Search, 2 of 5"; entering the chip says the app, the action and
  what to type; a change of action and what a lookup found are said once.

| Key | On an app's row | Under an app's chip, nothing typed | Under an app's chip, typed |
| --- | --- | --- | --- |
| Tab · Shift + Tab | next · previous stop, wrapping; enters and opens nothing | the app's other action (one press) | Search · Play · arrow; a change of action on its first press only |
| → at the end | next stop; again on Window or the arrow: opens its list | – | the same |
| Enter | Open opens; Search and Play make the app the chip; Window and the arrow open their list | nothing | runs the armed action; under Play before the answer, waits for it |
| Backspace on empty | – | back to where Enter was pressed | the text's |

## 19. First run (5 October 2026)

The papers: `first-run/` (`00-brief.md`, "Settled for the build", wins over the others; `BUILD.md` says what was
built and what was decided on the way). As built:

- **A stage under the empty field**, where two cards stood: one screen at a time, in this order: the key (four
  screens), three lessons (open an app, search inside an app, a sum), one question (search suggestions), two
  choices. It has the place under the empty field before everything else (core `Under`). It is not a row: nothing
  of it is selected, the field above it is live, and a typed letter puts it away in that frame; it is back, whole,
  when the field is empty again.
- **Who gets it.** A new installation gets all of it; an installation that was there before and has no key yet
  gets the key's step alone, without the opening piece; whoever asks for it again ("First steps", below) gets it
  again.
- **The stage's keys.** Tab and Shift + Tab arm its answers, round and round; Enter runs the armed one, and with
  none armed does nothing; Esc closes, and the same screen stands at the next opening. A screen takes a key or a
  click only once the answer that key would run has been in view for 350 ms: Enter, once the armed answer has;
  Tab, the pointer and a click, which reach any answer, once every answer has. Nothing is answered blind, and
  nothing by the Enter that answered the screen before. A screen that came while something was typed, or was typed
  over before it had been in view, comes into view when the field is empty again, and counts from then. So does one
  of the key's screens that came while the system's dialog was over the panel: it counts from when the dialog has
  gone. Up on a stage is Up on any empty field: the last text that was not run comes back, and the stage gives way
  to its list.
- **The key.** Action + Quick Insert is suggested, Action + M on a keyboard without that key. "Open Keyboard
  shortcuts" asks the system for its dialog; the panel holds on behind it, and the dialog opens on Booklight's own
  page: five numbered steps in the system's own words. The key that lands is told to the panel: "Your key works";
  held down, it does not put that panel away again (no start by the key does anything for 0.7 s after its last).
  "Not now" ends first run.
- **The lessons are practice.** Nothing opens: Enter on an app's row, and on a search inside an app, shows its
  slot pressed and the footer says "That opens …"; half a second later the next lesson stands. A sum is copied for
  real, and the panel stays. While a lesson's list is typed, the field's placeholder says what to type and the
  footer's left end says the next key and what it does today.
- **The question is consent**: alone on its screen, nothing armed, "Not now" first; only "Agree" switches
  suggestions on. It is not asked while they are on.
- **The choices are a list** of two rows with nothing selected: "Show your usual" with a switch, and the list of
  everything. Enter with nothing selected is "Done": first run is over and the glass folds to the bare field.
  Closing the panel on them ends first run too. So does a letter typed over them: the glass shows the typed list,
  and the choices do not come back.
- **An unfinished run** stands in three openings, then waits in the Booklight window. The opening in which it
  begins to wait, and the one in which "Not now" was said to the key, say so in the field's placeholder.
- **"First steps"** asks for it again. Typed in the panel, the run stands in that panel, from "Your key works"
  where a key is known, without the opening piece. Its row is there only where it was typed for: for three letters
  or more that begin its name ("fir", "first s"), or for one of its other words in full (tour, welcome, intro). A
  letter or two that would find it by the usual matching ("f", "fi", "w") do not: nobody meets it in an everyday
  list. The row on the window's
  Start page opens a panel that plays the whole of it, the opening piece first; where a run is unfinished, both go
  on with it where it stopped, and the row says so ("Go on with the first steps"). The opening piece is left out
  wherever it is not played (below). It changes no switch: where suggestions are on the question is not asked, and
  where sums are off there is no sum's lesson and no sum in the show.
- **The opening piece**, once, at a new installation's very first opening, which runs at Slow: the welcome, then a
  show in which Booklight types into its own field (this device's apps, a sum, an example flight, the emoji grid)
  and runs nothing. Every key is the piece's first: Enter and Space are the cue in the welcome and set the key's
  step down in the show; Esc, Tab, an arrow and Backspace set it down; a typed character ends it and is typed. It
  is not played with the system's animations off, with a screen reader on, on a screen lower than 655 dp or in a
  right-to-left layout: the key's step stands at once. With animations off or a screen reader on, the welcome's
  title greets as the field's placeholder.
- **Booklight never opens itself over another window**, and nothing in first run opens another app.
- **Screen reader**: each screen is said once when it comes, with its keys or its recipe and how to act; so are
  the choices, the ending and the word that the steps wait in the window. The switch is a switch, and its new
  state is said.

| Key | On a stage | On the choices | While the opening piece plays |
| --- | --- | --- | --- |
| A letter | typed; the stage gives way to its list, and is back when the field is empty | typed; first run is over | ends the piece, and is typed |
| Enter | the armed answer; none armed: nothing | nothing selected: Done, the fold; on a row: that row | the cue in the welcome; in the show the key's step lands |
| Tab · Shift + Tab | arms the next · the previous answer | as Down | the key's step lands |
| ↓ ↑ | ↓: nothing. ↑: the last text that was not run comes back, and the stage gives way to its list; with none kept, nothing | the highlight comes to a row, and goes | the key's step lands |
| Esc | closes; the screen waits | closes; the choices are done | the key's step lands |

