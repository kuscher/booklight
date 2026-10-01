# Booklight 2.0: interaction review

*1 October 2026. Read: the 2.0 spec, plan and mock-ups (rendered with headless Chrome), `ux-model.md`, `CLAUDE.md`, the 1.1 spec §12–13, and the 1.1 code (`OverlayModel.kt`, `Panel.kt`, `Rows.kt`, `Strip.kt`, `Footer.kt`, `Metrics.kt`, `SearchEngine.kt`, `Matcher.kt`, `Verbs.kt`, `AppsProvider.kt`, `Scopes.kt`, `scopes/Jot.kt`, `scopes/Picks.kt`, `window/MainActivity.kt`, `window/Page.kt`, the string resources). Nothing was run on a device. Where a precedent is from memory and not re-checked, it says so.*

## 1. Verdict

1. The new features fit the 1.1 model in kind. The trouble is at four seams: the opened row, Tab and Space on one-letter keywords, everything that types for you (tips, `?`, the Commands page), and the window's two panes.
2. The opened row cannot be built as written. "The rows below make room" has no room (the HP holds about 12 rows; eight results and eight actions are 16), and the spec gives two different answers for how the row opens.
3. Three of the spec's own examples do not work under 1.1's rules: `new tab` and `new event` become the New scope at the Space (Enter then creates a file called "tab"), and `chrome new` already means New window.
4. Tips as written make Enter live on an empty panel, and Enter twice writes the example into Todo.md or starts a timer. In `todo`, a second Enter ticks the next task and nothing takes it back.
5. None of it needs a redesign. With the ten rules under Must fix and the tables in §5 the keyboard stays one model. Nine icons is acceptable only as a ceiling that the list's room forces; the order should keep 1.1's first five places.

## 2. Must fix

### M1. An opened row has no room for the other results (spec §6, third mock-up)

**Wrong.** The spec says the action rows are "ordinary rows of the list: the rows below make room". The list never scrolls and holds eight rows (`SearchEngine.DEFAULT_LIMIT`, `Metrics.maxRows` clamps to 3…8). The HP is 1067 dp tall: under the panel's top edge (20 %) and above the taskbar there is room for the field, the footer and about 12 rows of 56 dp. Chrome with eight actions under it is already nine rows. Opened from the third place it is eleven; with five more results below it is sixteen. The mock-up shows the one case that fits (two results). A builder has to guess what happens to the rest. The mock-up also indents the action rows 44 dp and makes them 42 px against 50: a second left edge and a second, shorter pill, which is the kind of misalignment Alex has just rejected.

**Rule.**
- While a row is open, the list is that row and its other actions, nothing else. The other results fade where they are (as they do when a scope is entered), the row rides to the first place if it was not there, the action rows arrive under it in the list's cascade. Closing brings the results back in their places.
- Action rows are rows on the list's own grid: the symbol in the icon column, the name in the title column, the same pill at full width, no indent, no kind label. With nothing else in the list, the indent has nothing to say.
- Height: 56 dp each where nine rows fit (both Googlebooks), else 48 dp, so that a row and eight actions (440 dp) never take more than eight results do (448 dp). On a screen that cannot hold even that (under about 770 dp tall) the list is cut at the bottom; every action in it can be typed.
- `Metrics.height` takes the opened state; the window's height follows its one spring.

**Why.** One behaviour for every position and every screen. A destructive row is never directly above an ordinary result, so Down, Down, Enter cannot overshoot onto something else. Hover cannot close the list by drifting onto a row below it. It is what entering a scope already does to the list (ux-model §11: "other rows fade where they are").

### M2. How the row opens and what every key does while it is open (spec §6)

**Wrong.** "Enter or a click on the arrow, or moving the arming past the ninth icon (Tab, →), opens the row" says two things. If moving past the ninth opens it, the arrow is never armed from the keyboard and "Enter on the arrow" cannot happen. If Tab opens it, a held Tab (it repeats, and Tab wraps in 1.1) opens and shuts the panel by 400 dp several times a second, and Tab then sits on an action row with one action, where `OverlayModel.arm` returns false: the wrap that 1.1 promises is gone. Shift + Tab from Open, which in 1.1 arms Uninstall, is not mentioned. Down, Up, Ctrl + digit, Shift + Enter, hover and click in the open list are not defined.

**Rule.**
1. The strip has up to eleven stops, in the order they are drawn: the nine, a typed extra (S2), then **More**. More is a stop like any other: armed, the pane unrolls to "More ⏎" ("Mehr"). It is never the default.
2. Tab and Shift + Tab move through the stops and wrap, as in 1.1. → moves without wrapping and stops on More; ← moves back and on Open gives the caret back. **Moving the arming never opens the list.** A held Tab never changes the panel's height.
3. Enter on More opens the list. → pressed again on More opens it too (as → enters a scope row today, `Panel.kt`: `entersScope -> model.fill()`). A click on the arrow toggles it. First press only, not a repeat.
4. On opening, the pill travels to the first action row and the arrow turns over. The opened row keeps its strip, at rest, with nothing armed: the one case of an unselected row showing its icons.
5. In the list: ↓ ↑ move the pill; ↓ stops on the last row; ↑ from the first row goes to the row itself with the arrow armed, now reading "Less ⏎" (Enter closes). Tab and Shift + Tab are ↓ and ↑ but wrap inside the list: they are still "the next action, wrapping". → does nothing. ← closes. Enter runs the selected action. Shift + Enter runs it and stays. Esc closes the panel, as at every depth.
6. Typing, Backspace or a paste closes the list in the same frame and edits the text; the new text then decides (row one, default armed, or a typed action armed).
7. Ctrl + 1…9 count the rows on screen from the top: 1 is the row's default, 2…9 its actions. Never Uninstall (it is the ninth row of an ordinary app: `runRow` must refuse it as it refuses any `danger` action).
8. Hover selects an action row on pointer movement only, as today; hover never closes the list. A click on an action row runs it; on the row's title, the row's default; on an icon, that icon.
9. Closing with ← or the arrow puts the pill back on the row with More armed, so ← again walks back to App info and → opens again: one path, both ways. Closing because the results changed (typing, a package removed) lands on row one with its default armed, never on "the same index in the new list".
10. Uninstall is the last row, in error ink, and the pill under it takes `errorContainer` (as the pane does in 1.1). Android's dialog stays the only confirmation. The list is entered at its first row, so Uninstall is as far from the entry as it can be; Shift + Tab from Open now lands on More, not on Uninstall as in 1.1.
11. Footer: on More, nothing extra (the pane says it). In the list: `←` "Less", `esc` "Close". "Fewer" in the mock-up should be "Less" ("Weniger"): More and Less are a pair.

**Why.** 1.1's two rules survive unchanged: Tab is the next thing the row offers and wraps (ux-model §1.7, §2), Enter runs what is armed. Opening is a deliberate Enter, so nothing large moves under a key that is only looking around. Alfred opens a result's actions with → and closes them with ← (ux-model source 19).

### M3. `new tab`, `new event` and `chrome new` do not do what the spec says (spec §3, first mock-up's caption)

**Wrong.** `new` is a keyword, and a keyword and a Space is a chip (`OverlayModel.type` → `SearchEngine.scopeFor`). `new tab` is therefore the New scope with "tab", which `Jot.new` reads as a file name: Enter creates and opens a file called "tab" in Documents. The same for `new event`, `new text note`, `new list`, and the Terminal's "New Window". Most shortcuts that apps declare begin with "New". And `chrome new` is 1.1's verb for New window (`verb_window` = "new window, window"; "new" is its start), so it arms New window on Chrome's row and does not "go straight to" New tab.

**Rule.**
- Extend 1.1's "inside a scope, an app whose name starts with the keyword and the text comes first" (1.1 spec §13, `SearchEngine.inScope`) to commands: a command whose title starts with the keyword and the text, with at least two letters typed after the keyword, comes first; two at most. `new tab` shows "New tab · Chrome", then New's own preview, then the web row. `new` and a Space alone shows New's kinds, as today.
- A typed verb wins over a command, as in 1.1: `chrome new` is Chrome with New window armed, and Chrome's commands follow it. Correct the caption.
- A command whose title is one of Booklight's own actions for that app (Open, New window, App info, Uninstall) is not listed: the Terminal's "New Window" is the row's own second icon.

**Why.** Otherwise the feature's headline example writes a file.

### M4. Tab on a keyword "whichever row is selected" takes Tab away from the row (spec §11)

**Wrong.** In 1.1 Tab belongs to the selected row (ux-model §1.7). With the new rule: type `s`, press Down to Spotify, press Tab to reach its New window, and the panel becomes the Settings scope while Spotify's icons are on screen. The same on `drive`, `maps`, `settings` (apps with exactly a keyword's name are row one at 1.0, above the scope row at 0.97) and on every one-letter text. Also, today an exact keyword ranks its scope row above every app that only starts that way (1.0 × 0.97 against 0.9 + a little, `SearchEngine.scopeRows`, `weight`): `w`, Enter enters Wikipedia in 1.1. "A one-letter keyword's row ranks under apps" needs a number and a place, or with eight apps on S the Settings row is off the list.

**Rule.**
- When the text is exactly a keyword, and the selection is where typing left it (row one, its default armed), Tab makes the keyword the chip. First press only, and only for the list on screen (the rule of `OverlayModel.fill`).
- Once ↓, ↑, →, ← or the pointer has moved the selection or the arming, Tab is the selected row's again. On the keyword's own row Tab enters it, as in 1.1. → always walks the row's actions.
- The scope row of an exact one-letter keyword ranks under every app, setting and control that matches, and always keeps the last place above the web rows, with its `tab` cap. So `s`, Enter opens the usual app and the row is always on screen.
- This changes `g` and `w` too (and a user's one-letter links): `w`, Enter now opens the first app on W. Say so in the release notes.
- Footer: in that state the first hint is `tab` and the scope's title: "tab Search settings", "tab Search keys", "tab Search YouTube" (`Scope.title`), not "tab Settings", which reads as the Settings app when that app is row one. Use the same wording on a scope's own row, where 1.1 says "Fill in": one wording for one thing.

**Why.** `s`⇥ and `k`⇥ typed straight through always work, which is what was asked for. Anything the user has navigated to keeps 1.1's Tab. Chrome ties its "Tab to search" hint to the selected line and takes it back when another line is chosen (from memory; ux-model sources 1–3 cover the keyword and Tab part).

### M5. `s` and a Space, `k` and a Space capture ordinary text, and the scope's own last row weakens 1.1's guard (spec §11)

**Wrong.** "s bahn", "s klasse", "k pop", "k cup", "s 25 ultra" become the Settings or Keys chip at the Space. 1.1's guard is the web row at the end of a scope. Here nothing matches "bahn", and the first row left is "Search the Settings app", so Enter opens Settings. The mock-ups of `s` and `k` show no web row at all, against 1.1's "the last row in a scope is always Search Google for the keyword and the argument together".

**Rule.**
- Keep "a keyword and a Space" for `s` and `k`: one rule for every keyword (Alex's decision 2 in 1.1 §12), and `s wifi` typed straight through must work as `yt lofi` does.
- In `s` and `k`, when no page or key matches the argument from the start of a word, row one is the web search for the whole text ("Search Google for “s bahn”") and the hand-over row is second. When something matches, the matches come first, then the hand-over row, then the web row last.
- Backspace at the start of the argument gives the text back and it then stays text (`held`), as in 1.1. Tab on that text still enters: it is the explicit way.
- German: `s` and `k` only, plus `settings`, `einstellungen`, `keys`, `tasten`, `kürzel`. No `e` and no `t`: "e mail", "e bike", "t shirt", "t online".

**Why.** Chrome removed Space for exactly this and had to bring it back (ux-model sources 1, 2). The capture then costs one animated chip and nothing else: Enter still does what was meant.

### M6. Three documents give three answers on who owns a keyword (spec §3, plan "Review focus" 3, `Scopes.kt`)

**Wrong.** The plan says a user's link called `s` or `k` keeps working. The spec §11 does not say it. The code does the opposite: `Scopes.all()` drops a user's link whose keyword a built-in scope has, and `reserved` keeps the editor from giving one. After the update a user's `s` link (Spotify, Stack Overflow) or `k` link (Kagi) would stop working without a word. Prompts add five more keywords (`fix`, `shorter`, `de`…), other apps add theirs.

**Rule.**
1. The user's things (links, recipes, snippets, prompts) cannot share a keyword with each other; the editor refuses.
2. `s` and `k` yield to a user's link: at the update the link keeps the letter, and the editor allows giving a link `s` or `k`, saying "Replaces Booklight's s. `settings` still works." Booklight's other keywords stay reserved, as in 1.1.
3. A seeded prompt whose keyword the user already uses is seeded without a keyword and shows an empty cap in the window.
4. Another app's keyword never takes one that Booklight or the user has, now or later.
5. `?`, the Commands page and the tips show the keyword that works on this device.

**Why.** A keyword the user chose always means what the user chose. Chrome lets a user's site search override a built-in one.

### M7. Tips make Enter live on an empty panel; Enter twice runs the example (spec §12; the same for `?` §7 and the Commands page §10)

**Wrong.** In 1.1 Enter on an empty field does nothing (`chosen()` is null). With "Try it ⏎" armed on every opening, Enter types an example, and a second Enter runs it: `todo call bank` is written into Todo.md, `timer 10m tea` starts a timer, `fix their going too` opens Gemini. The plan's "Enter on a tip types, never runs" covers the first press only; 1.1 guards a held key (`repeatCount`), not two presses. An example that is typed and not run is kept by `OverlayModel.keep()`, so Up on an empty field next time brings back the tip's text and not the user's own. `?` ("on anything else types the example") and the Commands page ("Enter opens the panel with it typed") have the same double Enter; on the Commands page the second press lands in a panel that is still unfolding.

**Rule.**
- On the tip card nothing is armed at rest. Tab arms "Try it", Tab again "Turn off tips", wrapping; Enter does the armed answer. Enter on an untouched card does nothing, as on an empty field in 1.1. The card shows a `tab` cap where the mock-up shows the Enter mark.
- One rule for everything that changes the field for you (a tip's Try it, a `?` row, a Commands row, Enter on a scope's own row): **the next Enter runs only if it is a new press at least 350 ms later**, the rule a confirmation has today (`CONFIRM_GAP_MS`).
- Text put in the field by Try it, `?` or the Commands page is not kept as the last text unless the user edits it (as text from another app is not, `foreign`).
- ↑ on the empty field still brings back the last text and puts the card away. Esc closes. Typing puts the card away in the same frame.
- If Alex wants one key: Enter may be Try it, but then with all three guards above and only once the card has been on screen for 350 ms.

**Why.** "Enter runs what is armed" and "nothing typed, nothing armed" both stay true. Tab is already the key for "type into this" (rule 7; the `tab` cap on scope rows).

### M8. `todo`: typing cannot find a task, and a second Enter ticks the wrong one (spec §8, To-do mock-up)

**Wrong.** Typed text always adds, so with more than eight open tasks the ninth cannot be reached: the list does not scroll and typing does not narrow. "Enter ticks one off and the row folds away" moves the next task under the pill; a second Enter (a bounce, or impatience) ticks that one too, and nothing unticks it. `todo`, Enter, Enter (the first enters the scope from its row) ticks the top task. Whether the panel closes after a tick is not said; by 1.1's rule (`run`: a `done` word, then close after 520 ms) it would.

**Rule.**
- With text: row one is "Add “call bank”" (Add armed); under it the open tasks that contain the text, each with Done. Enter adds; ↓ Enter ticks. Without text: the open tasks in file order.
- Enter on a task ticks it **and the row stays where it is**, drawn ticked (the box filled, the title dim and struck), its action now "Undo". The panel stays open (`keepOpen`, as deleting a snippet does), the footer says "Done". Enter on it again unticks. Ticked rows are gone the next time the scope is entered.
- M7's 350 ms rule applies after the Enter that entered the scope.
- A task is found by its line in the file, not by its text, so two equal tasks are two tasks.

**Why.** Typing narrows in every other list. A row that stays makes the second Enter an undo and not another task; task lists keep a ticked item in place for a moment for this reason (Things, Reminders; from memory).

### M9. A prompt with nothing typed sends the clipboard unseen (spec §9)

**Wrong.** `fix`, Space, Enter hands whatever is on the clipboard to the Gemini app. The row in the mock-up shows the typed text as its second line; with nothing typed nothing says what will go. The clipboard "may hold anything (a password)" (`Picks.kt`, `TextScope`).

**Rule.** With an empty argument the row's second line is the first line of the clipboard, dim, under the caption "From the clipboard". An empty clipboard, or one marked sensitive, gives a row without actions ("Nothing copied"), as `clip` does today. Read the clipboard once, when the scope is entered.

**Why.** Rule 4 of the model: the top row previews what was understood. Rule 6: outward things wait for an Enter that can see them.

### M10. The window: the column cannot be reached from most pages, two equal highlights, a destructive first row (spec §10, window mock-up)

**Wrong.**
- "Left from a row without a control goes to the column." On Look and Results every row is a control, and Left there is "previous choice" or "off" (`Choice`, `Toggle`). There is no key to the column; a user who tries Left turns a switch off.
- The mock-up draws the column's section and the page's row with the same filled pill. Nothing shows which of the two has the keys.
- About's first row is "Forget everything", selected. From the column: Enter (into the page), Enter, Enter forgets everything. `CLAUDE.md`: destructive actions are last, never first.

**Rule.**
- Tab and Shift + Tab move between the column and the page. ← on a row without a control also goes to the column. ← and → on a control change it and never leave the page.
- In the column ↑ and ↓ change the section at once, without wrapping; → or Enter goes into the page: to the row that was selected there, else the first row.
- Only the pane with the keys draws the filled pill. The other keeps its place as an outline (the pill's rim without its fill). When the keys change pane, the fill fades on one and off the other in place; neither pill travels across.
- About: the privacy policy and the version first, "Forget everything" last.
- Opened from the icon: the keys are in the page, no row selected until ↓, as in 1.1. Opened by "All commands": the Commands page, first row. By "Edit…": Yours, on that item.

**Why.** Two panes, one focus, and the focused one is the filled one: macOS draws a sidebar's selection grey when the sidebar does not have the keyboard. Tab between panes is the common desktop rule and is free in the window (1.1's key handler gives Tab no meaning of its own outside an editor).

## 3. Should fix

### S1. The nine: keep 1.1's first five places, and split by action, not by count (spec §6)

**Wrong.** 1.1's order is Open · New window · App info · Left half · Right half. The spec moves App info from third to ninth. ux-model §2: the order is "fixed per kind". Seven of the nine icons are a window with a part filled; with App info gone from between them, New window and the six places are one run of look-alikes. "Nine at most" also moves the boundary for an app of another profile (no New window) or would, if the split is by count: Left two thirds comes up onto the row and every icon after the first shifts.

**Rule.** Open · New window · App info · Left half · Right half · Full · Left third · Middle third · Right third, then More. In the list: Left two thirds · Right two thirds · Top left · Top right · Bottom left · Bottom right · Centre · Uninstall. The list always holds those eight (seven for an app that came with the device); the row holds the rest, eight or nine. Full comes after the halves because it is the next most used; the thirds end the row so that Right third and Left two thirds, which are used together (the mock-up's own recipe), are neighbours across the arrow.

**Is nine right?** As a ceiling, yes, and only because the list forces it: seventeen actions less the eight that fit under a row. Nine unnamed icons are not found by looking; they are found by Tab and by typing. Do not add a tenth kind of place without taking one away.

### S2. The typed extra as a tenth: its Tab order, the list, and the title's room (spec §6, "chrome top left" mock-up)

- Tab order is the drawn order: the nine, the tenth, More. The tenth is the default while the text names it; ← and Shift + Tab go to the ninth; when the text stops naming it, its slot folds away.
- The list still holds all eight in their places when a tenth is on the row. Opened then, the pill lands on that action's row, not on the first.
- "chrome uninstall" shows Uninstall as the tenth in error ink: the same as 1.1, and still only when asked for by name.
- Room: a slot at rest is 36 dp and an armed one about 55 dp plus its name (`Strip.kt`). Nine at rest, an armed "Left two thirds" and More are about 540 dp of the 616 dp a row has after its icon; the title keeps about 75 dp, and in German ("Linke zwei Drittel") less. "Google Chrome" is cut. Give the places short names, 12 characters at most ("Left ⅔", "Links ⅔", "Middle ⅓", "Top left"), and check a minimum title width of 160 dp in both languages in `Metrics`; if it cannot be kept, the row holds eight for everyone.

### S3. The row blinks out while a place is being typed (1.1 behaviour, now on every place)

`Verbs.readings` wants two letters of a verb, and `Matcher.score` wants every word to match. "chrome t" (on the way to "top left"), "chrome f" (full), "chrome b", "chrome l" match nothing in "Chrome": the row leaves for one keystroke and comes back armed. For "chrome c" and "chrome r" it stays, because Chrome has a c and an r. **Rule:** when the last word is a single letter that starts a verb of that row, the name reading stands and the default stays armed. Test with `./bl debug keys`, one character at a time.

### S4. Other apps' commands: clutter, doubles, trust (spec §3)

- **By the app's name alone:** only for the app that is row one, only when the text matches its name from the start with three letters or more, three commands at most, and never in place of a row that matched on its own name. `k` must not list every command of every app on K. iOS Spotlight shows an app's shortcuts under the top hit only (from memory).
- **Equal titles** ("Search", "New message", "Compose"): the app's icon and its name under the title are the only difference, so both must always be shown; among equals the app used most comes first.
- **Keywords from a file** count less than a title (as `Matcher.keyword` does for settings pages), eight per command at most, so a file cannot buy row one for "chr".
- **Another app's scope keyword** is entered with Tab or Enter on its row; a Space does not enter it until the user has entered it once. Otherwise any app can claim "how" or "what". Chrome keeps site searches it found by itself inactive until the user activates them.
- **The kind label.** The mock-up says "Command"; `kind_command` is "Booklight" today (it labels Booklight's own pages). A new kind is needed, or these rows will say "Booklight".

### S5. Tips: when the card arrives, how tips rotate, when they stop, what they say (spec §12)

- **Arrival.** The card arrives only if nothing was typed for about 400 ms after the panel has opened. The opening is then the same every time (the unfold grows to the field's height, as in 1.1), "nothing typed, nothing shown" stays true for anyone who types straight away, and the first keystroke does not have to take a card away. As specified, every use is: taller panel, card out, rows in.
- **Rotation.** A tip stays until it has been on screen for three seconds in total, or has been tried; then the next. A new tip on every opening is never read: the panel is open for under a second.
- **Skipping.** A tip whose feature has been used is skipped (the history knows which scopes were run).
- **Stopping.** After one pass the tips end by themselves; the switch stays on and new tips come with a new version. "Show the tips again" is a row in the window. "Start again at the end" repeats what is known. Apple's TipKit has both rules: a display count, and a tip that is invalidated when its action is done.
- **Wording.** One feature in a tip. It starts with what to type, in strong ink, and that is exactly what Try it types; then what happens, in a few words. 90 characters at most, in both languages. The mock-up's tip has two features in it; cut "Pin keeps the countdown in view". First in the order: `?`, since it unlocks the rest; then places, `s`⇥, `k`⇥, `todo`, prompts, Pin.
- **"No tips"** reads as "there are none". Use "Turn off tips" / "Tipps ausschalten".

### S6. `?` (spec §7)

- `?` as the first character is the scope at once, without a Space; VS Code's Quick Open lists what can be typed on `?`. A lone "?" must not be read by Ask Gemini's "ends in a question mark" rule.
- `help` should not be a keyword: "help with taxes", "hilfe bei …" are searches. The scope is called Help, so "help" finds its row, and Tab or Enter enters it.
- No web row in this scope ("Search Google for “? ti”" means nothing); `SearchEngine.inScope` adds one to every scope that has a keyword.
- One name for the action wherever an example is put in the field: "Try it" on a tip, on a `?` row, on the Commands page. The mock-up's "Type…" on a `?` row is 1.1's label for a scope's row; keep it only for rows that enter a scope.
- Nothing typed: what the user has not used yet, in the tips' order; "All commands" keeps the last place.

### S7. Notes (spec §8)

- `note` adds and `notes` finds: one letter apart, opposite effects, and the slip writes into the file. Keep `notes`, add `find`/`finde` as a second keyword, and make the two captions differ at a glance ("Add to Notes.md" against "In Notes.md").
- "The first word is a file's name": the second row is the safe choice (the default stays Notes.md). Define it: the whole first word, any case, without ".md", followed by a Space. Not Todo.md (a dated line there breaks the task list) and not the daily files. The folder is listed once, when the scope is entered, not on a keystroke.
- No folder yet: `notes` and `todo` show one row, "Choose the notes folder…", as `note` does.
- A found line: Copy · Pin · Open file, Copy armed. "Today" needs a name that says where it goes ("Today's note") and a footer word that names the file.

### S8. Pin (spec §4)

- **Focus.** The panel closes and a new window opens: the keys must go back to the app the user was in, not to the pin. A pin window that takes the keyboard, with a Copy button in it, eats the next Space or Enter. Make it not take focus until it is clicked; then Esc closes it.
- **From the keyboard.** `pin` with nothing typed lists what is pinned, with "Unpin". Otherwise a keyboard launcher makes a window that only the pointer can close.
- **Replacing.** A pinned text exists nowhere else. When a new pin would replace one, the row says so in its second line ("Replaces “Gate B22…”").

### S9. The system's keys (spec §5, "snap" mock-up)

- Enter on "Snap window to the left" opens a window of shortcuts; the row reads as if Enter snapped. On "lock" it is worse. The armed pane in the mock-up is an icon and the Enter mark with no name, against `CLAUDE.md` ("the armed one is unrolled"). Name it: "All shortcuts ⏎" / "Alle Tastenkürzel".
- Unselected key rows need a kind label of their own ("Key" / "Taste"); the mock-up's footer says "tab Actions" on a row that shows one.
- One pattern for the three lists: the last row hands over. `s`: "Search the Settings app". `k`: "All shortcuts", the system's window. `?`: "All commands", the Booklight window.
- Nothing typed in `s` and `k`: the pages or keys opened from Booklight most recently, then a fixed set; never "the first eight of 46".

### S10. "No key yet" (spec §10)

- After the update `keySeen` is false for someone who has pressed Action + K for weeks; the window opens with a statement that is false. Seed it true when the 1.1 first-run card was answered or the history is not empty.
- After "Open Keyboard shortcuts" and back, the line still says "No key yet", which reads as "it failed". Say "Now press the key once" until the panel has been opened by it.
- The assistant key is a key: it counts. The widget and the tile (`ACTION_PANEL`) do not.
- When the panel has been opened by a key, the panel's own "Give Booklight a key" card is done as well: one flag for both.
- The lamp dot in the column is a pointer, not a state: drop it once the Booklight page has been seen. In the mock-up it disappears when the column is icons only; keep it on the icon.

### S11. Prompts (spec §9)

- `fix` and `explain` start ordinary searches ("fix bike tyre"). It is 1.1's accepted risk for `new` and `note`; add it to the risks, and keep the web row for the whole text as the second row of every prompt scope.
- The prompts are also rows of `clip` and of text from another app. That list has up to eleven rows already (`TextScope`) and eight fit. For text from another app: Search, Ask Gemini, the prompts, Note, Mail, QR; the transforms by typing. For `clip`: the transforms first, the ways out last, the prompts by typing their name.
- The chip carries the prompt's whole name ("Fix spelling and grammar"); cap the chip's label at about 24 characters with an ellipsis.

### S12. Places that do nothing, and "on display 2" (spec §6)

- A window that is already open keeps its place, and Booklight cannot know which apps are open. With thirteen places this is what most Enters will meet. The tip, `?` and the release notes must say "when the app is not open yet".
- "… on display 2": say what the row shows (the armed name reads "Right third · Display 2") and what happens with one display (the words are not read as a place; the text is ordinary text).
- German: Centre and Middle third must not both answer to "mitte".

### S13. The mock-up hides parts of the design

- `.pg { display: flex }` overrides `hidden`: in the page as published all seven sections of the window are drawn one under the other, so the column and "one page beside it" cannot be judged. Add `.pg[hidden] { display: none }`.
- Keep's row shows five icons and Slack's three, where 2.0 has nine and the arrow.

## 4. Consider

- **C1. The opened row as a chip.** Alfred and Raycast filter an item's actions by typing. Booklight has the means: More could make the app the field's chip ("Chrome"), the list its actions, typing narrows ("tl", "two"), Backspace leaves. It answers small screens for good. It is not what Alex asked for ("expand a list of rows below"), and M1 gets most of it; worth it only if more actions are coming.
- **C2. Two-letter places:** `chrome tl`, `tr`, `bl`, `br`. Verbs may already be shortened to two letters; these four are not the start of their words.
- **C3. New window in a place.** The answer to "the app is already open" is "a new window, there". Not in 2.0; note it.
- **C4. A pinned timer after it was cancelled in the Clock** still counts down. "Rings at 14:12" is then false; "set for 14:12" is not.
- **C5. "Keys"** sits beside "Your key" and "Keyboard shortcuts" in one product. "Shortcuts" for the scope and the chip would match the system's own word.
- **C6. Screen reader.** An opened action row is an item of its own: "Top left, 3 of 8, Chrome". The opened row announces "8 more actions".
- **C7. The Commands page.** When the panel it opened closes, the window has the keys again, on the same row.
- **C8. Learning.** As in 1.1: an action reached with Tab or from the list teaches the row only. Say it in the spec.

## 5. The table of keys for 2.0

"At the end" means the caret is at the end of the text. A stop is something the arming can rest on: an icon, a typed extra, More.

### The panel

| Key | Results | Text is exactly a keyword, selection untouched | A row is open (pill on its actions) | In a scope | Control row | Grid | Confirmation | Tip card (empty field) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Typing | Filters; row one; default armed, or the action a typed verb names | As results | Closes the list in the same frame, then as results | Edits the argument | As results; a number is the target | Filters; first cell | Cancels, then types | The card goes in the same frame, then as results |
| Space | After an exact keyword at the start: enters the scope (not a keyword just turned back into text; not another app's until used once). Else a space | Enters the scope | Closes the list; a space | A space | A space | A space | Cancels | A space; the card goes |
| ↑ ↓ | Move the row. ↑ on an empty field: last text | Move the row; Tab is then the row's | Move among the actions. ↑ from the first: the row itself, Less armed. ↓ on the last: stays | Move the row | Move the row | Move the cell | Cancel, then move | ↑: last text. ↓: nothing |
| Tab · Shift + Tab | Scope row: enter it. Else next · previous stop, wrapping. Never opens the list | Tab: the keyword becomes the chip. Shift + Tab: previous stop | Next · previous action, wrapping inside the list | Next · previous action | Next · previous action | Next · previous action | Cancel | Arm Try it, then Turn off tips, wrapping |
| → at the end | Next stop, no wrap. On More, again: opens the list. Scope row: enters it | As results | Nothing | Next action | Up a step · on · next track | Next cell | Cancels | Nothing |
| ← | Previous stop; on the first, the caret | As results | Closes the list; the row again, More armed | The same | Down a step · off · previous track | Previous cell | Cancels | Nothing |
| Enter | Runs the armed action. Scope row: enters it. More: opens the list | Runs row one's armed action (`s`, Enter opens the app) | Runs the selected action. On the row itself with Less armed: closes the list | Runs it; after the Enter that entered the scope, only a new press 350 ms later | Commits, closes after 400 ms | Copies, closes | Confirms, after 350 ms | Nothing until an answer is armed. Try it: the example goes into the field, never run. Turn off tips: the switch goes off |
| Shift + Enter | Runs, stays open | Runs, stays | Runs, stays; the list stays open | Runs, stays | Commits, stays | Copies, stays, appends | – | As Enter |
| Ctrl + 1…9 | That row's default; never a destructive one | The same | The rows on screen from the top: 1 the row's default, 2…9 its actions; never Uninstall | The same | The same | – | Cancels | – |
| Backspace on empty | – | – | – (with text: closes the list, deletes) | Chip back to its keyword | – | As a scope | Cancels | – |
| Esc | Closes | Closes | Closes the panel | Closes | Closes | Closes | Cancels only | Closes |
| Summon key · click outside | Closes | Closes | Closes | Closes | Closes | Closes | Closes | Closes |
| Click | Row: default. Icon: that action. Arrow: opens the list | The same; the scope's row: enters it | Action row: runs it. Arrow: closes. The row's title: its default. An icon: that action | Field chip: leaves | Drag the track; click the switch | Cell: copies | Same chip: confirms | An answer: does it. Elsewhere: nothing |
| Pointer moves | Selects the row; arms the stop under it | The same; Tab is then the row's | Selects an action row; never closes the list | Selects the row | Selects the row | Selects the cell | – | Arms the answer under it |

Rules that hold in every column:
- A held key repeats only where nothing runs: moving the pill or the arming. Enter, Tab onto a keyword, opening the list and Ctrl + digit act on the first press.
- After an Enter that only changed the field (entered a scope, typed an example), the next Enter runs only as a new press at least 350 ms later.
- Uninstall is run by Enter or a click on it and by nothing else.
- In `todo`, Enter on a task ticks or unticks it and the panel stays.

### The Booklight window

| Key | In the column | On a page row without a control | On a row with a control | In an editor's field |
| --- | --- | --- | --- | --- |
| ↑ ↓ | Previous · next section at once; no wrap | Previous · next row, brought into view | The same | The field's own |
| ← | Nothing | To the column | Previous choice · off | The caret |
| → | Into the page: the row selected there before, else the first | Nothing | Next choice · on | The caret |
| Enter · Space | Into the page | Runs the row. Commands page: opens the panel with the example in the field. A delete: Enter, then Enter again 350 ms later | Flips the switch · next choice | As 1.1 |
| Tab · Shift + Tab | To the page | To the column | To the column | Next · previous field |
| Esc | Nothing | Nothing | Nothing | As 1.1 |
| Click · pointer moves | Click: that section, the keys stay where they were. Moving: nothing | Selects on movement; click runs | Selects; click sets | Places the caret |

The pane with the keys draws the filled pill; the other shows its place as an outline.
