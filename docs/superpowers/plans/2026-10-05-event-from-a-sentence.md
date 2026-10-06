# An event from a sentence. Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Type an event the way you would say it ("Add dinner with Sam tomorrow at 7pm to the Team calendar"), in any order and with or without the keyword `event`; one row shows what Booklight understood (when, the title, where, the calendar); Enter opens the Calendar app's editor filled in, on that calendar; and with an option switched on in the Booklight window, Enter saves the event itself. A calendar's name is matched against the user's own calendars and completed in the field as it is typed.

**Architecture:** Core decides and is tested. `When` learns what a spoken sentence says (a span and the half of the day in words, a range of days, "on the 12th", "all day", "every Monday" as a guess) and finds a day and a time anywhere in a line (`When.spot`). `Sentence` reads an event's sentence by rules at every letter: the cue, the place, the day and time, a calendar by its name against a list of calendars, the title. `Splits` is the device's model as a splitter: when it is asked at all, its one question, and its answer taken apart and merged with the rules' reading only where every part of it was typed; the date is always the parser's. `Cals` says which calendar a new event goes to, builds the calendar link, the values of a direct save, and whether Enter saves at all (never a guess). `SearchEngine` gives a sentence's row its place: under everything of the device that matches, over the web. In the app, `device/CalendarList` is the one place that reads the system's list of calendars (and nothing else of it), only once the user allowed it in the Booklight window; `providers/Events` builds the event's row, the same one under the keyword and for a sentence without it, and asks the model after the typing has rested, through the lookup the panel already has for a flight's row (`OverlayModel.lookUp`, `land`); `Executor` sends the link to the Calendar app with the insert request as its fallback, and is the one place that writes an event. The window gains one row on Privacy and one group on Results. Nothing runs in the background and no connection is opened.

**Tech Stack:** Kotlin 2.4, JUnit 4 in `core/` (plain JVM, no `android.*`), Jetpack Compose in `app/` (compile SDK 37, min SDK 34), the system's calendar provider (`CalendarContract`), Google's ML Kit GenAI prompt API through the app's own `ai/OnDevice`, the repo's `./bl` helper over adb.

**Spec:**
- `docs/design/event-sentence/design.md`. **§11, "Settled for the build" (Alex, 5 October 2026), wins over the sections before it**: all seven decisions of §9 are yes; saving directly is built too, behind a switch, with its rules (a guess is never saved; "Open in Calendar" stays as the second action; "New events go to"); two permissions, both inert until the user allows them in the window; it goes out as 3.2 (the release is not this plan's).
- §3 (the row), §4 (how a sentence is read: the rules first, then the model, only where it can help), §5 (the calendars), §6 (what the parser must learn), §7 (what it changes: the permission, the rule on the model), §10 (how it is checked).
- `docs/research/event-sentence.md` (written by Task 10): what a Googlebook said before the build. The sixteen sentences are the test table, the model's sixteen answers the fixtures; the Calendar app there ignores a calendar's id in the insert request and honours a calendar link's `src=`.
- `CLAUDE.md`: the layout, the design rules, the gotchas, that the repo is public.

**Who sees what in a build of this plan.** Someone who allows nothing: `event …` reads more (a day and time in the middle of the line, and the forms of §6), its row has a second line of slots (WHERE, empty until a place is read), a sentence that begins with add, schedule, put, book or plan and holds a day or a time gets that same row without the keyword, under whatever of the device matches and over the web; where the rules cannot tell a place from the title and the device has the model, the row's words change a moment after the typing has rested; Enter opens the Calendar app's editor as before, now by a calendar link. Nothing is asked of the system, and nothing new is read. Someone who allows the calendars (the Booklight window › Privacy › Calendars): the row has a CALENDAR slot, a calendar named in the sentence is matched, its start is completed in grey, a Calendar stop lists the calendars, and the editor opens on the calendar that was named. Someone who also switches on "Save events without opening Calendar" (Results): Enter is Save where the day and the time were both read; "Open in Calendar" is the second action. Nothing is released by this plan: the version stays 3.1, code 9.

## Global Constraints

- Branch `event-sentence`. Local only: never pushed, no tag, no release. **The version stays 3.1 / code 9**, no file says that a version with this feature is out, and the changelog's entry is headed "Unreleased".
- No attribution lines of any kind in commits or code: no `Co-Authored-By`, no Claude lines, no session links.
- **The repo is public.** No person's and no calendar's real name, no address, no device serial, no adb name, no build number, nothing about what is installed on a device, in any file or commit message. The examples are "Sam" (a person, and the account's own calendar) and "Team" (a calendar). The debug hooks print the names of the user's own calendars: they are never copied into a file.
- `core/` has no `android.*` import.
- Every user-facing string is a resource, in English (`res/values`) and German (`res/values-de`). In German a quotation opens with „ (U+201E) and closes with “ (U+201C); an English apostrophe is ’ (U+2019).
- **Typing is never held up.** The row's own reading (core's rules) is there in the frame of the key. Nothing waits for the device's model: not the row, not Enter.
- **The model is never asked while the user types**: only after the typing has rested for 0.7 s, once for a text, and only where the rules leave the title in pieces (core `Splits.asks`). It is never asked for a date or a sum: the day and time are always read by `When`.
- **What the model returns is never shown unless every part of it is a piece of the typed text** (core `Splits.merge`). The model runs nothing and saves nothing by itself: the user's Enter saves what the row shows, and every field that will be saved stands in the row before Enter.
- **The row never changes its height, and none of its lines moves, when the model answers**: sizes are known before anything moves. An event's row is 92 dp (`Metrics.tall`) from its first letter, with both lines of slots.
- **One ranked list.** The event's row of a sentence typed without the keyword stands below anything of the device that matches and above the web's rows, and it never appears for a text without a day or a time.
- **Booklight reads the list of calendars only: never an event.** The one place that asks the system's calendar provider anything is `device/CalendarList.kt`, and it asks for the list of calendars. The list is read when a panel is made and when the Booklight window has the keys again, never while typing.
- **Booklight writes only the event the row shows, only with the switch on, only on the user's Enter, and never a guess.** The one place that writes is `Executor.save`. Booklight never changes or removes an event.
- **Both permissions are inert until the user allows them in the Booklight window.** Nothing in the panel asks the system for a permission. Someone who allows nothing is asked nothing.
- Nothing in the background; no network (nothing in this plan opens a connection); **no new line in `<queries>`**. The manifest gains the two `<uses-permission>` lines and nothing else.
- Destructive actions are last, in the error colour, never first, and never run by an arrow or Ctrl + digit. Saving is not destructive, and may be armed (see "Decided here", 10); it is Enter's alone, on the selected row.
- The panel's rules hold: one panel, one ranked list; a row shows all it can do as icons, the armed one unrolled; **Tab only moves, Enter does**; two lists at most under a row, never a list inside a list; one coloured surface, the selection (a calendar's own colour as a 10 dp dot is a sample, like a colour's swatch, and is the only new thing drawn).
- The Booklight window is not the panel: rows in Material's containers; every row's mark starts 16 dp in from the column's edge, every row's text on one edge after the mark, every control in a row ends 16 dp from the row's trailing edge; the choices of one page all stand in the same place; nothing is centred.
- Comments in the surrounding code's own style and density: plain sentences that say why; a KDoc where the neighbours have one.
- Never run `uiautomator dump`. A device is chosen by its model (`adb devices -l`), never by its place in the list. **Only the coordinator touches a device**: the engineer builds, and runs no `./bl` command but `./bl test` and `./bl build`.
- Use a task's code as it is written. A "Find" is a text that stands in its file exactly once at the moment the task runs; "Find, in one long line" is a piece of one line, which also stands in the file exactly once. If a Find does not stand exactly once, stop and report it: do not guess the place.

**The commands of this repo, as every task uses them**
- All core tests: `./bl test` (quiet: no output and exit status 0 mean every test passed).
- One test class: `./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SomeTest' --console=plain`.
- A debug build with its check: `./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT` (the build is good only if this prints the one word `BUILT`: a resource error has no `e:` line, and both words count).

**How this plan was checked.** It is written on `event-sentence` at `fdebfb0`. Every task below was applied from this text, in order, to a fresh scratch checkout of `fdebfb0` with this plan committed on it first: each "Find" stood exactly once when its turn came; the failing tests of Tasks 1 to 6 failed as said and passed after; after every task `./bl test` passed, the debug build printed `BUILT`, and the tree was the one the code had been developed to; Task 12's commands gave what it expects. **Nothing was run on a device, and nobody has seen what this plan draws.** How the row looks, what the model answers there, what the Calendar app does with the link, and the one save, are Task 10's list of checks, every answer of which reads "not run".

## Review Focus

Five inputs the design implies that would bite a real user and that nothing would exercise unless a test is written for it. Each has its test in the task that owns the code.

| # | The input | What must hold | Its test |
| --- | --- | --- | --- |
| 1 | A word that is a day or a time only by accident, now that the middle of a line is read: "we sat in the sun today", "buy 2-3 gifts for Sam", "the 1st draft review tomorrow", "2nd interview", "add pictures of the sun", "add 2-3 eggs", "book 2-3 nights" | It is not read as the day: a short weekday alone, and two bare numbers with a dash, are not read in the middle of a line; a number with "th" is a day only after "on" or beside a time; and none of them makes a line an event without the keyword | Task 1: `aNumberWithThAloneNeedsATime`. Task 2: `whatIsFarMoreOftenSomethingElseIsNotReadInTheMiddle`, `whatTheEndsReadIsReadTheSame`. Task 3: `aLineTypedWithoutTheKeywordIsOfferedAsAnEvent` |
| 2 | A calendar's name that is an ordinary word too, or that only begins a word: "Add handover to team tomorrow 9am", "send invites to Team members", "dinner with the Team", "to tea" in the middle of the line, "to t" | The calendar is taken only after "to", "in" or "on" at the end of a piece of the sentence, by its whole name; by its start only at the very end and from two letters (and, once the name is whole, while the word "calendar" is still being typed after it); otherwise the words stay in the title. Where it is taken, the row says so in its slot before anything is opened or saved | Task 3: `aCalendarsNameThatIsAlsoAnOrdinaryWordIsTheCalendar`, `wordsThatAreNoCalendarStayInTheTitle`, `theStartOfACalendarsNameAtTheVeryEnd`, `theCalendarStaysWhileTheWordCalendarIsTyped`, `aCalendarThatIsNoneOfYoursStaysInTheTitle` |
| 3 | An answer of the model that is almost right: a word that was not typed, typed words in another order, a word of the sentence in none of its parts ("for Sam" dropped), a "when" that says less than the rules read, a calendar that is none of the user's, the calendar given as the place too, broken JSON | Nothing of it is shown: the rules' own reading stands, the same object. What is taken is cut from the typed text, never from the answer; the day and time are the parser's | Task 4: `nothingTheModelSaysIsShownUnlessItWasTyped`, `theSixteenAnswers`, `whatIsNoAnswerIsDropped`, `whatTheModelSplitsIsTakenFromTheTextAsItWasTyped`, `theRulesOwnCalendarAndPlaceStand`, `withoutTheCalendarsAnAnswerThatNamesOneIsNotTaken` |
| 4 | A guess that would be saved: only a time ("Call mom 10am"), only a day, "every Monday 6pm", "all day" with no day, no title, a calendar that takes no events, a chosen calendar that is gone (the settings came to another device with a backup) | Enter does not save: `Cals.saves` is false, and the event goes to no calendar it should not go to | Task 5: `aGuessIsNeverSaved`, `whereANewEventGoes`. Task 2: `whenAnEventIsSure` |
| 5 | Where and when the device is: an all-day event west and east of UTC, a moment across the night the clocks go back, a title with `&`, `#`, `+`, `%`, quotation marks, an umlaut and an emoji | An all-day event is the days that were typed in every zone, in the link and in what is written; a moment is the same moment in UTC; nothing typed can add a parameter to the link | Task 5: `anAllDayEventIsItsFirstDayAndTheDayAfterItsLast`, `aMomentIsTheSameMomentInEveryZone`, `whatATitleHoldsIsWrittenIntoTheLinkSafely`, `whatIsWrittenIsWhatTheRowShows` |

Also pinned: the sixteen sentences that were tried on a device, by the rules alone and with the model's sixteen answers (Task 3, `theSixteenSentences`; Task 4, `theSixteenAnswers`, `theModelIsAskedOnlyWhereWordsAreLeftOver`); everything the ends read is read the same by the reader of the middle (Task 2, `whatTheEndsReadIsReadTheSame`); a sentence's row is under every row of the device however it is scored, and is never crowded out (Task 6, `aSentenceStandsUnderWhatTheDeviceMatchesAndOverTheWeb`, `aSentencesRowIsNeverCrowdedOut`); "all day" is no time for a reminder to ring at (Task 1, in `aReminderNeedsAWhen`). **No JVM test reaches the app.** That the row's lines do not move when the model's answer lands, that an Enter in that moment is not taken, that the model is not asked while letters come, that the switch goes off with its permission, that nothing but the list of calendars is read and nothing but the one event written, and what the Calendar app does with the link, are Task 10's checks on a device; that nothing else of the provider is asked is also Task 12's grep.

## Decided here

Where the design differs from what could be built, or is silent. Each is Alex's to overturn. Rows 1, 2, 3, 11 and 14 are where the design could not be built as written.

| # | What | Why | What it costs if wrong |
| --- | --- | --- | --- |
| 1 | **The row has two lines of slots, and both always stand**: WHEN and TITLE, then WHERE and (where the calendars are known) CALENDAR; an empty slot is its label and a rule. The design has "one more slot" in the row as it is, and WHERE and CALENDAR "only where" one was read | One line cannot hold four slots beside a strip of four actions: at 720 dp the third slot of today's row is already cut to nothing. And a line that came only when a place was read would move the row's other lines when the model's answer lands, which the design forbids | Every event's row says "WHERE –". To show a slot only once it is filled is one condition in `Events.row`, and then the lines move by 11 dp when the second line comes |
| 2 | **WHEN stands before TITLE**, as it does today. The design lists Title first | "The event row as it is": WHEN is the slot that must never be cut, and it has the fixed width | The order of two slots in one list |
| 3 | **"Book" and "plan" (and „plane“) make a line an event but stay in its title**; "add", "schedule", "put", "new event", „trag … ein“, „neuer Termin“ are dropped, as the design says of all of them | "Book club Friday 7pm" and "Plan review tomorrow" are what such events are called. A title that keeps a word is never wrong; one that lost a word can be, and with the switch on it would be saved | "Book dentist tomorrow 3pm" is called "Book dentist", not "dentist". One set in `Sentence`. (Ruled in the last round of fixes, 6 October: under the keyword `event` no first word is dropped but "new event" and „neuer Termin“, as in 3.1: "event put out bins thu 7am" keeps its "put".) |
| 4 | **Without the keyword the row needs a cue, a day or a time, and a title, and is not offered where the only "day" is a short weekday alone or two bare numbers with a dash** ("add pictures of the sun", "add 2-3 eggs"). In the middle of any event's line those two are not read alone either | With no keyword the row is row one wherever nothing of the device matches: Enter would open the calendar where it opened a web search before | "Add lunch sat" needs "on sat" or "Saturday". One condition in `Sentence.reads`. (Ruled in the last round of fixes, 6 October: "schedule", "put", "book", "plan" and „plane“ also begin everyday searches. After one of them the row needs a day and a time of day, both, „plane“ only where they were said in German; and that row never has Save. "Add", "new event" and their German forms are as this row says.) |
| 5 | **"12th" and "the 12th" are a day only after "on" („am“, „den“) or beside a time** | "The 1st draft review tomorrow", "2nd interview", "the 3rd floor" | "Dinner the 12th" is not read; "Dinner on the 12th" is |
| 6 | **A calendar is taken after "to", "in" or "on" at the end of a piece of the sentence**: at the sentence's end, or just before the day and time (there, since the last round of fixes, only with the word "calendar" after its name: "Drive to work tomorrow" saved a wrong title into a wrong calendar; and the word on its way after a whole name counts from its third letter). **Its start is enough only at the very end of the typed text, from two letters, and the row then reads that calendar at once**, as the design's picture has it; Right takes the rest. Once its name is whole the row keeps it while the word "calendar" is typed after it ("… to the Team cal"), so the slot does not empty and fill again | "Add K night to the Team calendar Saturday 8pm" has it before the day. A start anywhere else would take "to tea" out of a title | A calendar called like a common word is taken wherever those words end a piece (Review Focus 2). The row shows it; with the switch on, Enter would save there |
| 7 | **A calendar chosen from the row's list is written into the sentence** ("… to Team"), in place of the one it names; it is not kept beside the text | What the row reads stays a function of what is typed: it survives a closed panel (Up brings the text back), and can be seen and edited | The field's text changes by a choice from a list. A choice kept in the model instead is about a task |
| 8 | **The model's answer must also lose no word**: a word of the sentence that is in none of its parts, and is not one that only joins them ("to", "the", "add"), drops the answer. Its "when" must be read whole by the parser from the text's own words (with a leading "on", "the", „am“), and say no less than the rules read; a calendar it names must be one of the user's, and the one the rules found if they found one. Any failure: the rules' reading stands | The design asks that every part be a piece of what was typed; one of the sixteen answers passed that and had dropped "for Sam". With the switch on, a lost word would be saved | An answer that left out a harmless word the list does not know is dropped: the row then stays as the rules read it, which is never wrong, only less split |
| 9 | **"Words are left over" means: the title is in pieces**, words on more than one side of the day and time (not counting a piece that is a calendar named by the word). Only then is the model asked, and never about more than 200 characters | It is the one case the rules know they have not split (six of the sixteen); a rule that guesses more would ask while nothing is wrong | A place at the end of a title on one side ("Dinner at Luigi's tomorrow 7pm") is not split off. One function, `Splits.asks` |
| 10 | **Save may be the armed action.** CLAUDE.md's rule is that what removes, or cannot be taken back, is never armed first. Save adds one event the user typed and sees whole in the row; it is on only by the user's own switch; a guess is never saved; and the calendar keeps the event for the user to change or delete. Guarded five ways: only Enter (or a click: the pointer's Enter, through the same guards) on the selected row saves, never Ctrl + digit; an Enter that was pressed before the row for the last keystroke was on the glass never saves (it is dropped, and the next Enter does: the reviews' ruling, 6 October); the panel is locked while the event is written and goes with a save whether Shift is held or not, so nothing saves twice; an Enter in the third of a second after the model changed the row, or after a calendar was chosen from the list, is not taken; `Executor.save` asks for the switch and the permission again. **Booklight cannot take a save back** (it never changes or removes an event), so there is no Undo | §11 says Enter is Save with the switch on; the rule's reason (nothing is lost by a wrong Enter that the user cannot see and mend) holds here except for an event in a calendar others see, which is why the switch is off unless chosen | If Alex wants Save second: two actions change places in `Events.row` |
| 11 | **With the switch on and a guess, the first action is "Open in Calendar"** (not "Create"), and the footer's quiet line says why ("Opens in Calendar: the day or the time is a guess"; "A repeat is set in Calendar"; "… takes no new events") | §11's words. Someone who switched saving on must see that this Enter does not save | One more line in the footer for a guess |
| 12 | **"New events go to" keeps the calendar by its owner's address**, not by the id the system has for it on this device. With the switch on and no calendar named, the row shows that calendar a step lighter, and "Open in Calendar" opens on it too | An id is another calendar on another device, and the settings travel with a backup; an address that is not there falls back to the account's own calendar | An address (for the account's own calendar, the account's) is kept in the settings file, on the device and in the user's own backup. `PRIVACY.md` says so |
| 13 | **The switch goes off when its permission is gone**: looked at when a panel is made and when the window has the keys again (`BooklightApp.lookAtCalendars`) | "Refused: the switch stays off". A permission taken back and given again in the system's screens, or settings restored to another device, must not switch saving on behind the user's back | After taking the permission back, the switch must be pressed again. (Ruled in the last round of fixes, 6 October: the switch is also believed only beside this device's own mark, the file no backup holds, as "asked" is: settings that arrive with a restore never have saving on, whatever the restore did with the permission.) |
| 14 | **The row points at the calendars ("Allow…", and a line in the footer) whenever a sentence names a calendar by the word and the list is not allowed, until Android's question has been answered once**; the design says "the first time". It leads to the window's row: nothing is asked in the panel | One showing can be missed; and "inert until allowed in the window" forbids asking from the panel | Someone who never answers sees one more action and one line whenever they name a calendar. One flag |
| 15 | **Only the calendars of Google accounts are listed**; the row's list shows twelve at most, and only those that take events. A calendar that takes none is matched by its name, shown, and never saved to | On a Googlebook the Calendar app is the web calendar, and a link's `src=` names a Google calendar. Twelve lines are as many as the longest list under an app's row | A calendar of another kind of account cannot be named. One filter in `CalendarList` |
| 16 | **The default reminder is asked for in the provider's own words** (minutes: the default; method: the default), in a second row written with the event | "The calendar's own default reminder" (§11) and no number of Booklight's | If the Calendar app shows none, check 24 says so, and a time would have to be chosen |
| 17 | **A line that reads as an event is not sent for search suggestions**, with suggestions on | It is the user's own appointment, and the rule is already that what is meant for one app is not sent. Before a day or a time is typed the line is an ordinary one, and is sent like any other | No suggested searches under "add … tomorrow". One condition in `OverlayModel.search` |
| 18 | **Everything under `event`, and the row for a date that was copied, read the middle of the line too; `remind` does not** (it reads the ends, as before), and "all day" alone is no reminder | §4.1 speaks of the event; a reminder's line was not tried on a device | "remind call tomorrow about it" is still not read |
| 19 | **A range of days is all day, and is not a guess**; a time typed with it stays in the title. "every Monday" is the next Monday and a guess (§6) | §6: "`Oct 14 to Oct 16`, all day" | "Offsite Oct 14 to Oct 16 9am" has "9am" in its title |
| 20 | **A preview row says its slots to a screen reader**, every preview row; an event's row says them again when the model changed them | What Enter saves must be heard before Enter. Until now such a row said its name and its armed action only | Longer descriptions for mail, note and the others too |
| 21 | **Refused for good, the window's row opens Booklight's page in the system's Settings** (the system then shows no question and says no at once) | The only place it can still be allowed | An answer that comes in under 0.3 s is taken for "no question was shown" |
| 22 | **"new event …" cannot be typed in the panel**: `new` is a keyword, and its space makes it the chip. The cue is read under `event`, and „neuer Termin“ works | The keyword came first | "new event dinner tomorrow" makes a file. To give the cue the right of way is a rule in `OverlayModel.type` |
| 23 | **Left out, as the design leaves them out**: repeats, guests, a reminder of one's own, a video call, reading events ("am I free"), Undo | §8 | Nothing: it is settled |
| 24 | **The hooks**: `event TEXT`, `event ask TEXT`, `calendars`, `calendars pretend …`, `pref save`, `pref eventcal`, `pref calasked` | The coordinator's notes: what Enter would send or write must be said without doing it, and the row must be looked at without the permission | Debug code only. While a list is pretended the row offers Save where the switch is on, and the executor writes nothing |

## What each task hands the next

For every pair of tasks that share a file or a name: what the one produces and the other consumes. Where a later task's "Find" is text an earlier task wrote, the row says so.

| From | To | What |
| --- | --- | --- |
| 1 | 2 | `core/…/When.kt`: Task 1 changes `Moment`, the header, the words, `parse`, `find`, `Hit`, `read`; Task 2 adds `Spot`, `Piece`, `spot`, `leads`, a line to the header and two constants. `Hit.timed`, `both`, `moment(hit, rest)`, `read` are Task 1's and `spot` uses them. `core/…/WhenParts.kt`: Task 2 adds `Days.slight` beside what Task 1 wrote. `core/…/Jot.kt`: Task 1 changes one line of `reminder`; Task 2 changes `EventDraft` and `event` |
| 1 | 2, 3, 5 | `Moment.repeats`; an all-day `Moment` whose `timeGiven` says it was said, and whose `end` is the midnight after a range's last day |
| 2 | 3 | `When.spot(text, now): Spot?`, `Spot.moment`, `Spot.pieces`, `Spot.said`, `Piece.text`, `Piece.start`; `Jot.placed(arg)`, `Jot.draft(m, text, place, now, title)`; `EventDraft.repeats`, `EventDraft.sure`. Task 3 adds `When.slight` above `When.leads`, which Task 2 wrote |
| 2 | 4 | `When.leads(word)`; `Jot.draft` |
| 3 | 4 | `EventReading`, `Cal`, `Sentence.read`, `Sentence.uncued`, `Sentence.bare`, `Cals.named` |
| 3 | 5 | `core/…/Sentence.kt`: Task 5 adds to `object Cals`, whose first lines Task 3 wrote, and three imports |
| 3, 4, 5 | 8, 9 | `Sentence.read`, `Sentence.reads`, `Sentence.put`; `Splits.asks`, `Splits.prompt`, `Splits.merge`; `Cals.target`, `Cals.saves`, `Cals.link`, `Cals.write`, `Cals.TEMPLATE`, `EventWrite` |
| 6 | 8 | `SearchEngine.SENTENCE`; `Behind.CALENDAR`; `Icon.Swatch.symbol` and `Icon.Swatch.of`; `Effect.InsertEvent.link`; `Body.Slots.more`, `completes`, `footer`, `ask`; `Slot.dot`. Task 8 adds two effects to `Model.kt` (`SaveEvent`, `Retype`) with the lines of `Executor` that take them, so that the build stays whole |
| 6 | 10 | `DebugReceiver.kt`: Task 6 adds `Behind.CALENDAR` to the dump's `armed=`; Task 10 changes the same long line at its start |
| 7 | 8 | `BooklightApp.calendars: CalendarList` (`known`, `allowed`, `writes`, `pretends`); `Settings.saveEvents`, `eventCalendar`, `calendarsAsked`; `Effect.Grant("calendars")` in `Executor`; `strings_33.xml` in both languages, to which Task 8 adds before `</resources>` |
| 7 | 10 | `CalendarList.pretend(list)`; `MainActivity.PAGE_CALENDARS` |
| 8 | 9 | `providers/Events.kt`: `Events(context, prefs, calendars)`, `row(text)`, `row(id, text, r, now)`, `ID`, `SCOPE`, `SENTENCE`: Task 9 gives it the model (`ai`), `asks`, `answer`, `forget`, and a fifth parameter of `row`. `OverlayModel`: Task 8 adds `retype` and the list's dot; Task 9 changes `completion`, `entered`, `search`, `answer`, `look`, `lookUp`. `Rows.kt`: Task 8 changes the strip's stops and a line of a list; Task 9 the row's description |
| 9 | 10 | `Events.asks(r)`, `Events.answer(row, pause)`; `OverlayModel.take()`, `looked`. Task 10 adds `Events.rules(text)` and `Events.split(text)` for the hooks |
| 10 | 11 | `docs/research/event-sentence.md`, which the status and CLAUDE.md name; the hooks, which CLAUDE.md lists |

---

### Task 1: `When` learns what a spoken sentence says

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/Jot.kt`
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/When.kt`
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/WhenParts.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/JotTest.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/WhenTest.kt`

**Interfaces:**
- Consumes (as they stand at `fdebfb0`): in `WhenParts.kt`, `internal sealed interface Day { class On(val date: LocalDate); class Every(val day: DayOfWeek, val weak: Boolean) }`, `internal object Days { fun parse(words: List<String>, today: LocalDate): Day? }` with its private `one`, `numeric`, `named`, `fixed`, `dayNumber`, `MONTHS`, and `internal object Times { fun parse(words: List<String>, bare: Boolean, afternoon: Boolean = false, span: Boolean = true): Clock? }` with its private `join` and `part`. In `When.kt`: `data class Moment(start, end, allDay, dayGiven, timeGiven, rest)`, `When.parse(text, now): Moment?`, `internal fun find(text, now): Hit?`, `internal class Hit(start, end, day, time, seconds)` with `var rest`, the private `findOne`, `read`, `resolve`, `next`. In `Jot.kt`: `Jot.reminder(arg, now)`.
- Produces:
  - `Moment` has one more field, last: `val repeats: Boolean = false`. A moment that is all day because that was said ("all day", „ganztägig“, a range of days) has `allDay = true` and `timeGiven = true`; a range's `end` is the midnight after its last day.
  - `When.Hit` has two more fields, last: `val whole: Boolean = false`, `val repeats: Boolean = false`; and `val timed: Boolean` (`time || whole`). `private fun both(one: Hit, two: Hit): Hit`, `private fun moment(hit: Hit, rest: String): Moment`: Task 2 uses both.
  - `Day.On(val date: LocalDate, val weak: Boolean = false)`.
  - `Days.parse(words: List<String>, today: LocalDate, dotted: Boolean = false, led: Boolean = dotted): Day?` and `Days.range(words: List<String>, today: LocalDate): Pair<LocalDate, LocalDate>?`.
  - `Times.parse` keeps its signature and reads "from 9 to 10", "9am to 10:30am", „von 9 bis 10 Uhr“, "6:45 in the morning", „7 abends“.
  - `Jot.reminder`: "all day" alone is no reminder.

The parser of 3.1 read three of the sixteen sentences that were tried on a device. What it could not read is in the design's §6, and all of it is the kind of thing people say: `tmrw`; "on the 12th at 6:45 in the morning"; "from 9 to 10"; "Oct 14 to Oct 16"; "all day"; "every Monday". This task teaches `Days`, `Times` and `When.read` those forms, in English and German, and nothing else: where a line is read (its ends) is unchanged here and is Task 2's.

Three of the forms bring a way to be wrong, and each has its rule:
- **A number with "th" is as often the twelfth of something as a day** ("2nd interview", "the 1st draft"). So `12th` and `the 12th` are a *weak* day, read only beside a time, like the weekdays' two-letter forms; after "on" („am“, „den“) it is a day.
- **Two numbers with "to" between them are no span** ("2 to 3 Dentists"): a worded span needs "from" before it, or am, pm, Uhr or minutes on one of its ends.
- **"every Monday"** cannot be carried in this version: it is read as the next Monday and marked (`repeats`), so that the row shows it as a guess and it is never saved (Task 5).

"All day" said outright is not a time of day, and it is not a missing one either: the moment is all day *and* its time counts as given, which is what lets such an event be saved later. A reminder needs a time to ring at, so `Jot.reminder` does not take "all day" alone for a when.

- [ ] **Step 1: The failing tests, in `core/src/test/kotlin/io/github/kuscher/booklight/core/WhenTest.kt`.** Two changes: eight tests before `nothingToRead`, and the new words among the fuzz test's bits.

Find:

```kotlin
    }

    @Test fun nothingToRead() {
```

Make it:

```kotlin
    }

    // What a spoken sentence says that the parser could not read (docs/design/event-sentence/design.md §6). Today is Thursday 1 October 2026, 10:00.

    @Test fun tmrwIsTomorrow() {
        assertEquals(Moment(at(2, 9), at(2, 10), false, true, true, "meeting"), p("meeting tmrw 9-10am"))
        assertEquals(at(2), start("tmrw"))
        assertEquals(at(2, 15), start("tmr 3pm"))
    }

    @Test fun theTwelfth() {
        assertEquals(Moment(at(12), null, true, true, false, "Flight to Frankfurt"), p("Flight to Frankfurt on the 12th"))
        assertEquals(at(12), start("on 12th"))
        assertEquals(at(1), start("on the 1st"))                    // today counts
        assertEquals(at(31), start("on the 31st"))
        assertEquals(at(12, 6, 45), start("on the 12th at 6:45 in the morning"))
        assertEquals(at(12, 15), start("3pm on the 12th"))
        assertEquals(at(3), start("3rd of October"))
        assertEquals(at(3), start("the 3rd of oct"))
        // A month that has no such day: the next one that has.
        assertEquals(LocalDateTime.of(2026, 12, 31, 0, 0), When.parse("on the 31st", LocalDateTime.of(2026, 11, 2, 10, 0))!!.start)
        // In German the number has a dot, and „am“ or „den“ before it says it is a day.
        assertEquals(Moment(at(12), null, true, true, false, "Abgabe"), p("Abgabe am 12."))
        assertEquals(at(12, 18), start("am 12. um 18 Uhr"))
        assertEquals(at(12), start("den 12."))
        assertEquals(at(1, 18), start("um 18 Uhr am 1."))
    }

    @Test fun aNumberWithThAloneNeedsATime() {
        // "2nd interview", "the 3rd floor", "the 1st draft": a number like that is the second of something as often as a day.
        for (s in listOf("12th", "the 12th", "2nd interview", "the 3rd floor", "1st", "the", "the sun is out", "am 12", "12.", "on 12")) assertNull(s, p(s))
        assertEquals(at(12, 9), start("12th 9am"))
        assertEquals(at(12, 9), start("the 12th 9am"))
        assertEquals(Moment(at(12, 6, 45), null, false, true, true, "Flight"), p("Flight 12th at 6:45am"))
        assertEquals("2nd interview", p("2nd interview tomorrow")!!.rest)
        assertEquals("the 1st draft review", p("the 1st draft review tomorrow")!!.rest)
    }

    @Test fun theHalfOfTheDayInWords() {
        assertEquals(at(2, 6, 45), start("6:45 in the morning"))
        assertEquals(at(1, 19), start("7 in the evening"))
        assertEquals(at(1, 15), start("3 in the afternoon"))
        assertEquals(at(1, 23), start("11 at night"))
        assertEquals(at(1, 19), start("um 7 abends"))
        assertEquals(at(2, 7), start("7 Uhr morgens"))
        assertEquals(at(2, 7), start("morgen 7 Uhr früh"))
        assertEquals(at(1, 15), start("3 nachmittags"))
        assertEquals(at(1, 19), start("7 am Abend"))
        // A time that says its own half of the day keeps it.
        assertEquals(at(1, 19), start("19 Uhr abends"))
        assertEquals(at(1, 19), start("7pm in the evening"))
        // "at 6:45" alone is the afternoon; the morning said outright is the morning.
        assertEquals(at(1, 18, 45), start("at 6:45"))
        assertEquals(at(2, 6, 45), start("at 6:45 in the morning"))
        assertEquals(Moment(at(2, 19), at(2, 21), false, true, true, "Film"), p("Film tomorrow 7-9 in the evening"))
        assertEquals("Call", p("Call 9 in the morning")!!.rest)
        // The words alone are no time.
        for (s in listOf("in the morning", "abends", "at night", "früh")) assertNull(s, p(s))
    }

    @Test fun aSpanInWords() {
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, "Standup"), p("Standup from 9 to 10"))
        assertEquals(Moment(at(1, 14), at(1, 15), false, false, true, ""), p("from 2 to 3"))                  // after lunch, as "2-3" is
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, ""), p("von 9 bis 10"))
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, ""), p("von 9 bis 10 Uhr"))
        assertEquals(Moment(at(2, 9), at(2, 10, 30), false, false, true, ""), p("9am to 10:30am"))
        assertEquals(Moment(at(2, 9), at(2, 10), false, false, true, ""), p("9 to 10am"))
        assertEquals(Moment(at(1, 15), at(1, 16), false, false, true, ""), p("15 bis 16 Uhr"))
        assertEquals(Moment(at(2, 9), at(2, 10), false, true, true, "Sync"), p("Sync tomorrow from 9 to 10"))
        assertEquals(Moment(at(2, 9), at(2, 10), false, true, true, "Sync"), p("Sync morgen von 9 bis 10"))
        // Two bare numbers with a word between them are no span, and "from" without an end is no time.
        for (s in listOf("2 to 3 Dentists", "9 to 10", "from 9", "from Berlin to Paris", "von 9", "9 bis")) assertNull(s, p(s))
    }

    @Test fun aRangeOfDays() {
        assertEquals(Moment(at(14), at(17), true, true, true, "Team offsite"), p("Team offsite Oct 14 to Oct 16"))
        assertEquals(Moment(at(14), at(17), true, true, true, ""), p("Oct 14 to 16"))
        assertEquals(Moment(at(14), at(17), true, true, true, ""), p("Oct 14-16"))
        assertEquals(Moment(at(14), at(17), true, true, true, "Team offsite"), p("Team offsite Oct 14-16"))
        assertEquals(Moment(at(14), at(17), true, true, true, "Offsite"), p("Offsite 14.10. bis 16.10."))
        assertEquals(Moment(at(14), at(17), true, true, true, "Offsite"), p("Offsite vom 14. bis 16. Oktober"))
        assertEquals(Moment(at(14), at(17), true, true, true, ""), p("14.-16. Oktober"))
        assertEquals(Moment(at(5), at(8), true, true, true, "Messe"), p("Messe Montag bis Mittwoch"))
        assertEquals(Moment(at(5), at(8), true, true, true, ""), p("from Monday to Wednesday"))
        assertEquals(Moment(at(2), at(4), true, true, true, "Trip"), p("Trip tomorrow to Saturday"))
        // Over the year's end: the last day is the next such day after the first.
        assertEquals(Moment(at(30, month = 12), at(3, month = 1, year = 2027), true, true, true, ""), p("Dec 30 to Jan 2"))
        // No range where the last day is not after the first: the first day is read, and the rest stays.
        assertEquals(Moment(at(16), null, true, true, false, "to 14"), p("Oct 16 to 14"))
        // Nor where the two are months apart: two dates that happen to stand side by side.
        assertEquals(Moment(at(14), null, true, true, false, "to Mar 3"), p("Oct 14 to Mar 3"))
    }

    @Test fun allDaySaidOutright() {
        assertEquals(Moment(at(3, month = 11), null, true, true, true, "Birthday party"), p("Birthday party on November 3rd all day"))
        assertEquals(Moment(at(3), null, true, true, true, "Geburtstag"), p("Geburtstag am Samstag ganztägig"))
        assertEquals(Moment(at(3), null, true, true, true, "Messe"), p("ganztägig Messe am Samstag"))           // the day at the other end
        assertEquals(Moment(at(2), null, true, true, true, "Offsite"), p("all day tomorrow Offsite"))
        assertEquals(Moment(at(3), null, true, true, true, "Messe"), p("Messe Samstag den ganzen Tag"))
        assertEquals(Moment(at(2), null, true, true, true, "Offsite"), p("Offsite tomorrow all-day"))
        // Without a day it is today's, and the day is the guess. A day alone is all day too, and there the time is the guess.
        assertEquals(Moment(at(1), null, true, false, true, "Inventory"), p("Inventory all day"))
        assertEquals(Moment(at(2), null, true, true, false, "Offsite"), p("Offsite tomorrow"))
    }

    @Test fun everyMondayIsReadAsTheNextOneAndMarked() {
        assertEquals(Moment(at(5, 18), null, false, true, true, "yoga class", repeats = true), p("yoga class every Monday 6pm"))
        assertEquals(Moment(at(5, 18), null, false, true, true, "Yoga", repeats = true), p("jeden Montag 18 Uhr Yoga"))
        assertEquals(Moment(at(5, 18), null, false, true, true, "Yoga", repeats = true), p("Yoga 6pm every Monday"))
        assertEquals(Moment(at(5), null, true, true, false, "Yoga", repeats = true), p("Yoga each Monday"))
        assertFalse(p("yoga class Monday 6pm")!!.repeats)
        // Only a weekday repeats that way: before anything else the word stays in the line.
        assertEquals(Moment(at(2), null, true, true, false, "every"), p("every tomorrow"))
        assertNull(p("every"))
        assertNull(p("every other day"))
    }

    @Test fun nothingToRead() {
```

Find:

```kotlin
            "\uD83D", "9".repeat(40), "1e9", "0", "-1", "days", "wochen", "1h30", "٣", "İ", "ß", ",", ";")
```

Make it:

```kotlin
            "\uD83D", "9".repeat(40), "1e9", "0", "-1", "days", "wochen", "1h30", "٣", "İ", "ß", ",", ";",
            "to", "from", "bis", "von", "vom", "the", "12th", "0th", "99th", "every", "jeden", "all", "day", "ganztägig", "morning", "abends", "den", "14-16", "14.-16.", "-", "of", "tmrw")
```

- [ ] **Step 2: The reminder's test, in `core/src/test/kotlin/io/github/kuscher/booklight/core/JotTest.kt`.** One change, in `aReminderNeedsAWhen`.

Find:

```kotlin
        assertNull(Jot.reminder("3 things", now))
```

Make it:

```kotlin
        assertNull(Jot.reminder("3 things", now))
        // "All day" is no time to ring at.
        assertNull(Jot.reminder("all day sale", now))
        assertNull(Jot.reminder("Inventur ganztägig", now))
```

- [ ] **Step 3: Run them and see them fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.WhenTest' --tests 'io.github.kuscher.booklight.core.JotTest' --console=plain
```

Expected: `BUILD FAILED` in `:core:compileTestKotlin`: `No parameter with name 'repeats' found.` (the tests of "every Monday" build a `Moment` with it).

- [ ] **Step 4: The days and the times, in `core/src/main/kotlin/io/github/kuscher/booklight/core/WhenParts.kt`.** Twelve changes: a weak day of the month; `tmrw`, the words of a range and its longest length; `parse` with `dotted` and `led`, `one`, `ordinal`, `ofMonth` and `range`; "3rd of October" in `named`; then in `Times` the words of a span and of the half of the day, `parse`, the worded span's rule, `join` and `half`.

Find:

```kotlin
    class On(val date: LocalDate) : Day
```

Make it:

```kotlin
    /** [weak]: a day of the month by its number alone ("12th", "the 12th"), which is as often the twelfth of something: it needs "on" before it or a time beside it. */
    class On(val date: LocalDate, val weak: Boolean = false) : Day
```

Find:

```kotlin
    private val WORDS = mapOf("today" to 0, "heute" to 0, "tomorrow" to 1, "morgen" to 1, "ubermorgen" to 2, "uebermorgen" to 2)
    private val WEAK = setOf("mo", "tu", "we", "th", "fr", "sa", "su", "di", "mi", "do", "so")
```

Make it:

```kotlin
    private val WORDS = mapOf("today" to 0, "heute" to 0, "tomorrow" to 1, "tmrw" to 1, "tmr" to 1, "morgen" to 1, "ubermorgen" to 2, "uebermorgen" to 2)
    private val WEAK = setOf("mo", "tu", "we", "th", "fr", "sa", "su", "di", "mi", "do", "so")
    /** Between two days: "Oct 14 to Oct 16", „14. bis 16. Oktober“. And what may stand before the first. */
    private val TO = setOf("to", "through", "thru", "until", "till", "bis", "-", "–", "—")
    private val FROM = setOf("from", "von", "vom")
    /** The longest a range of days is: more is two dates that happen to stand side by side. */
    private const val RANGE = 62L
```

Find:

```kotlin
    fun parse(words: List<String>, today: LocalDate): Day? = when {
        words.isEmpty() -> null
        words.size == 1 -> one(words[0], today)
```

Make it:

```kotlin
    /**
     * [dotted]: the words follow „am“ or „den“, where a number with a dot is a day of the month („am 12.“).
     * [led]: they follow "on", „am“ or „den“: a day of the month by its number alone is then a day ("on the
     * 12th"); without, it is a weak one. "the" before a number belongs to it ("the 12th", "the 3rd of May").
     */
    fun parse(words: List<String>, today: LocalDate, dotted: Boolean = false, led: Boolean = dotted): Day? = when {
        words.isEmpty() -> null
        words.size == 1 -> one(words[0], today, dotted, led)
        words[0] == "the" -> if (words[1].firstOrNull()?.isDigit() == true) parse(words.subList(1, words.size), today, led = led) else null
```

Find:

```kotlin
    private fun one(word: String, today: LocalDate): Day? {
```

Make it:

```kotlin
    private fun one(word: String, today: LocalDate, dotted: Boolean = false, led: Boolean = dotted): Day? {
```

Find:

```kotlin
        return numeric(word, today)?.let { Day.On(it) }
```

Make it:

```kotlin
        numeric(word, today)?.let { return Day.On(it) }
        return ordinal(word, dotted)?.let { n -> ofMonth(n, today) }?.let { Day.On(it, weak = !led) }
    }

    /** "12th", "1st", "3rd"; with [dotted] also „12.“: the number, 1 to 31. A number alone is never a day. */
    private fun ordinal(word: String, dotted: Boolean): Int? {
        val t = word.trimEnd(',')
        val digits = t.takeWhile { it in '0'..'9' }
        val tail = t.substring(digits.length)
        if (digits.isEmpty() || digits.length > 2) return null
        return if (tail in ORDINALS || (dotted && tail == ".")) digits.toInt().takeIf { it in 1..31 } else null
    }
    private val ORDINALS = setOf("st", "nd", "rd", "th")

    /** The next time the month has a day [n] (today counts): "the 31st" in a month of thirty days is the next month's. */
    private fun ofMonth(n: Int, today: LocalDate): LocalDate? {
        for (m in 0..2L) {
            val month = YearMonth.from(today).plusMonths(m)
            if (n > month.lengthOfMonth()) continue
            val d = month.atDay(n)
            if (!d.isBefore(today)) return d
        }
        return null
    }

    /**
     * A range of days: "oct 14 to oct 16", "monday to wednesday", „14.10. bis 16.10.“, "oct 14 to 16",
     * „vom 14. bis 16. oktober“, "oct 14-16". The first and the last day; null for anything else. One
     * side must be a whole day by itself; the other may be a number that takes its month. The last day
     * is the next such day after the first.
     */
    fun range(words: List<String>, today: LocalDate): Pair<LocalDate, LocalDate>? {
        val w = if (words.size > 1 && words[0] in FROM) words.subList(1, words.size) else words
        fun day(d: Day?, from: LocalDate): LocalDate? = when (d) {
            is Day.On -> d.date
            is Day.Every -> if (d.weak) null else from.plusDays(((d.day.value - from.dayOfWeek.value + 6) % 7 + 1).toLong())
            null -> null
        }
        fun number(word: String): Int? = dayNumber(word)
        fun checked(a: LocalDate?, b: LocalDate?): Pair<LocalDate, LocalDate>? =
            if (a != null && b != null && b.isAfter(a) && !b.isAfter(a.plusDays(RANGE))) a to b else null
        // "oct 14-16", "14.-16. oktober": one word holds both numbers, the month stands beside it.
        if (w.size == 2) for ((both, month) in listOf(w[1] to w[0], w[0] to w[1])) {
            val cut = both.indexOfFirst { it in "-–—" }
            if (cut <= 0 || MONTHS[Matcher.fold(month)] == null) continue
            val a = number(both.substring(0, cut)) ?: continue
            val b = number(both.substring(cut + 1)) ?: continue
            val first = named(listOf(month, a.toString()), today) ?: continue
            return checked(first, runCatching { first.withDayOfMonth(b) }.getOrNull())
        }
        for (at in 1 until w.size - 1) {
            if (w[at] !in TO) continue
            val left = w.subList(0, at)
            val right = w.subList(at + 1, w.size)
            val first = day(parse(left, today), today.minusDays(1))
            if (first != null) {
                val last = day(parse(right, first), first) ?: right.singleOrNull()?.let(::number)?.let { n -> runCatching { first.withDayOfMonth(n) }.getOrNull() }
                checked(first, last)?.let { return it }
            } else if (left.size == 1) {
                // „14. bis 16. Oktober“, "14 to 16 oct": the first day takes the month of the last.
                val n = number(left[0]) ?: continue
                val last = (parse(right, today) as? Day.On)?.date ?: continue
                checked(runCatching { last.withDayOfMonth(n) }.getOrNull(), last)?.let { return it }
            }
        }
        return null
```

Find:

```kotlin
    private fun named(words: List<String>, today: LocalDate): LocalDate? {
```

Make it:

```kotlin
    private fun named(all: List<String>, today: LocalDate): LocalDate? {
        // "3rd of October": the "of" is nothing.
        val words = if (all.size in 3..4 && all[1] == "of") all.filterIndexed { i, _ -> i != 1 } else all
```

Find:

```kotlin
    private val NAMED = listOf("midnight" to 0, "mitternacht" to 0, "mittags" to 12, "mittag" to 12, "noon" to 12)
```

Make it:

```kotlin
    /** Between two times, as a word: "9am to 10am", „9 bis 10 Uhr“. And what may stand before the first: "from 9 to 10". */
    private val TO = setOf("to", "until", "till", "bis")
    private val FROM = setOf("from", "von")
    /** Words after a time that say which half of the day it is in, folded: the morning's, then the others'. */
    private val MORNING = listOf(listOf("in", "the", "morning"), listOf("am", "vormittag"), listOf("morgens"), listOf("vormittags"), listOf("fruh"))
    private val LATER = listOf(listOf("in", "the", "afternoon"), listOf("in", "the", "evening"), listOf("at", "night"), listOf("am", "nachmittag"), listOf("am", "abend"),
        listOf("nachmittags"), listOf("abends"), listOf("nachts"))
    private val NAMED = listOf("midnight" to 0, "mitternacht" to 0, "mittags" to 12, "mittag" to 12, "noon" to 12)
```

Find:

```kotlin
        val s = join(words) ?: return null
```

Make it:

```kotlin
        // "from 9 to 10", „von 9 bis 10“: the words say it is a span, so a number alone is an hour. Without its end it is nothing.
        if (words.size > 1 && words[0] in FROM) return if (span) parse(words.subList(1, words.size), bare = true, afternoon)?.takeIf { it.end != null } else null
        // "6:45 in the morning", „7 abends“: as if am or pm stood after the time; one that says its own half of the day keeps it („19 Uhr abends“).
        half(words)?.let { (time, mark) -> return parse(time + mark, bare = true, span = span) ?: parse(time, bare = true, span = span) }
        val joined = join(words) ?: return null
        val s = joined.text
```

Find:

```kotlin
        // "9-10am": the start borrows the end's half of the day, unless that puts it after the end ("11-1pm").
```

Make it:

```kotlin
        // Two numbers with a word between them are a span only where something says they are times: "2 to 3 dentists" is not one.
        if (joined.worded && !bare && a.mark == ' ' && b.mark == ' ' && !a.exact && !b.exact) return null
        // "9-10am": the start borrows the end's half of the day, unless that puts it after the end ("11-1pm").
```

Find:

```kotlin
    /** "3" "pm" → 3pm, "9" "-" "10" → 9-10. Any other second word means these words are not one time. */
    private fun join(words: List<String>): String? {
        if (words.isEmpty()) return null
        val b = StringBuilder(words[0])
```

Make it:

```kotlin
    /** The words as one text; [worded]: its two ends were joined by a word ("to"), not by a dash. */
    private class Joined(val text: String, val worded: Boolean)

    /** "3" "pm" → 3pm, "9" "-" "10" → 9-10, "9" "to" "10" → 9-10. Any other second word means these words are not one time. */
    private fun join(words: List<String>): Joined? {
        if (words.isEmpty()) return null
        val b = StringBuilder(words[0])
        var worded = false
```

Find:

```kotlin
                w.length == 1 && w[0] in DASHES && i + 1 < words.size -> { b.append('-').append(words[i + 1]); i++ }
```

Make it:

```kotlin
                w.length == 1 && w[0] in DASHES && i + 1 < words.size -> { b.append('-').append(words[i + 1]); i++ }
                w in TO && i + 1 < words.size -> { b.append('-').append(words[i + 1]); i++; worded = true }
```

Find:

```kotlin
        return b.toString()
```

Make it:

```kotlin
        return Joined(b.toString(), worded)
    }

    /** [words] without the words at their end that say the half of the day, and that half as its mark: "am" for the morning, "pm" for the rest. Null where no such words end them. */
    private fun half(words: List<String>): Pair<List<String>, String>? {
        for ((phrases, mark) in listOf(MORNING to "am", LATER to "pm")) for (p in phrases) {
            if (words.size <= p.size) continue
            val from = words.size - p.size
            if (p.indices.all { Matcher.fold(words[from + it]) == p[it] }) return words.subList(0, from) to mark
        }
        return null
```

- [ ] **Step 5: The expression, in `core/src/main/kotlin/io/github/kuscher/booklight/core/When.kt`.** Eight changes: `Moment` and its words; the header's new rules; the words that lead, repeat and say "all day"; `parse`, `find`, `both` and `Hit`; `read`, which becomes `read` and `once`, with `whole` and `allDay`; and a weak day alone in `resolve`.

Find:

```kotlin
 * A day, a time or both, read off a typed line. [end] only when an end time was typed ("9-9:30").
 * [allDay] when no time was given: [start] is then that day's midnight. [dayGiven] and [timeGiven] say
 * what was typed and what was guessed; [rest] is the line without the expression.
 */
data class Moment(val start: LocalDateTime, val end: LocalDateTime?, val allDay: Boolean,
    val dayGiven: Boolean, val timeGiven: Boolean, val rest: String)
```

Make it:

```kotlin
 * A day, a time or both, read off a typed line. [end] only when an end was typed: an end time ("9-9:30"),
 * or the last day of a range of days ("Oct 14 to Oct 16"), for which it is the midnight after that day.
 * [allDay] when no time was given: [start] is then that day's midnight. [dayGiven] and [timeGiven] say
 * what was typed and what was guessed: "all day" said outright, and a range of days, count as the time
 * given. [rest] is the line without the expression. [repeats]: the line said "every Monday": the day
 * that was read is the next such day, and no more than a guess at what was meant.
 */
data class Moment(val start: LocalDateTime, val end: LocalDateTime?, val allDay: Boolean,
    val dayGiven: Boolean, val timeGiven: Boolean, val rest: String, val repeats: Boolean = false)
```

Find:

```kotlin
 * - "next", "this", "nächsten", "kommenden" in front of a day are read with it and change nothing.
```

Make it:

```kotlin
 * - "next", "this", "nächsten", "kommenden" in front of a day are read with it and change nothing.
 * - "tmrw" is tomorrow. "on the 12th", "on 12th", „am 12.“: the next day of that number. Without
 *   the "on" ("12th", "the 12th") it is a day only beside a time: it is as often the twelfth of something.
 * - "6:45 in the morning", "7 in the evening", „7 abends“: the words say which half of the day.
 * - "from 9 to 10", „von 9 bis 10“, "9am to 10am": a span in words. Two bare numbers need the "from".
 * - "Oct 14 to Oct 16", "Monday to Wednesday", „vom 14. bis 16. Oktober“, "Oct 14-16": a range of
 *   days, all day, from the first to the last.
 * - "all day", „ganztägig“ beside a day, or at the line's other end: all day, said outright.
 * - "every Monday", „jeden Montag“: read as the next such day, and marked as a guess ([Moment.repeats]).
```

Find:

```kotlin
    private val LEADING = setOf("at", "on", "um", "am", "next", "this", "coming", "nächsten", "nächste", "nächster", "kommenden", "diesen", "dieses")
    private val HOUR = setOf("at", "um")

    /** What [text] says about when, or null when neither end of it is a day or a time. */
    fun parse(text: String, now: LocalDateTime): Moment? {
        val hit = find(text, now) ?: return null
        return Moment(hit.start, hit.end, !hit.time, hit.day, hit.time, hit.rest)
    }
```

Make it:

```kotlin
    private val LEADING = setOf("at", "on", "um", "am", "next", "this", "coming", "nächsten", "nächste", "nächster", "kommenden", "diesen", "dieses",
        "every", "each", "jeden", "jede", "jedes", "den")
    private val HOUR = setOf("at", "um")
    /** After these a number with a dot is a day of the month: „am 12.“. */
    private val DOTTED = setOf("am", "den")
    /** "every Monday": this version reads the next one and says it is a guess. */
    private val EVERY = setOf("every", "each", "jeden", "jede", "jedes")
    /** "All day", said outright, folded. */
    private val WHOLE = setOf("all day", "allday", "ganztagig", "ganztaegig", "ganztags", "den ganzen tag")

    /** What [text] says about when, or null when neither end of it is a day or a time. */
    fun parse(text: String, now: LocalDateTime): Moment? = find(text, now)?.let { moment(it, it.rest) }

    private fun moment(hit: Hit, rest: String) = Moment(hit.start, hit.end, !hit.time, hit.day, hit.time || hit.whole, rest, hit.repeats)
```

Find:

```kotlin
        if (one.seconds != null || (one.day && one.time)) return one
        // Only a day, or only a time: the other half may stand at the other end of what is left.
        val two = findOne(one.rest, now)?.takeIf { it.seconds == null && it.day != one.day && it.time != one.time } ?: return one
        val day = if (one.day) one else two
        val time = if (one.time) one else two
        val start = day.start.toLocalDate().atTime(time.start.toLocalTime())
        val end = time.end?.let { start.plusSeconds(java.time.Duration.between(time.start, it).seconds) }
        return Hit(start, end, day = true, time = true).also { it.rest = two.rest }
    }

    /** As [Moment], plus [seconds] when the expression was "in 20m": reminders make a timer of those. */
    internal class Hit(val start: LocalDateTime, val end: LocalDateTime?, val day: Boolean, val time: Boolean, val seconds: Long? = null) {
        var rest = ""
```

Make it:

```kotlin
        if (one.seconds != null || (one.day && one.timed)) return one
        // Only a day, or only a time: the other half may stand at the other end of what is left.
        val two = findOne(one.rest, now)?.takeIf { it.seconds == null && it.day != one.day && it.timed != one.timed } ?: return one
        return both(one, two).also { it.rest = two.rest }
    }

    /** A day from one of the two and a time (or "all day") from the other, as one. */
    private fun both(one: Hit, two: Hit): Hit {
        val day = if (one.day) one else two
        val time = if (one.day) two else one
        if (time.whole) return Hit(day.start, day.end, day = true, time = false, whole = true, repeats = day.repeats)
        val start = day.start.toLocalDate().atTime(time.start.toLocalTime())
        val end = time.end?.let { start.plusSeconds(java.time.Duration.between(time.start, it).seconds) }
        return Hit(start, end, day = true, time = true, repeats = day.repeats)
    }

    /**
     * As [Moment], plus [seconds] when the expression was "in 20m": reminders make a timer of those.
     * [whole]: "all day" was said, or the expression is a range of days: no time is missing. [repeats]: "every Monday".
     */
    internal class Hit(val start: LocalDateTime, val end: LocalDateTime?, val day: Boolean, val time: Boolean, val seconds: Long? = null,
        val whole: Boolean = false, val repeats: Boolean = false) {
        var rest = ""
        /** Nothing about the time of day is left to say. */
        val timed: Boolean get() = time || whole
```

Find:

```kotlin
        val led = w[0] in LEADING
        val hour = w[0] in HOUR
        val body = if (led) w.subList(1, w.size) else w
        if (body.isEmpty()) return null
        val today = now.toLocalDate()
        Days.parse(body, today)?.let { return resolve(it, null, now) }
        Times.parse(body, bare = hour, afternoon = hour)?.let { return resolve(null, it, now) }
```

Make it:

```kotlin
        // "every Monday 6pm": the next Monday at six, and a guess. Only a weekday repeats that way.
        if (w[0] in EVERY) return if (w.size > 1 && Days.parse(w.subList(1, minOf(w.size, 2)), now.toLocalDate()) is Day.Every) once(w, now)?.let { Hit(it.start, it.end, it.day, it.time, whole = it.whole, repeats = true) } else null
        return once(w, now)
    }

    private fun once(w: List<String>, now: LocalDateTime): Hit? {
        val today = now.toLocalDate()
        // "All day", said outright: alone it is today's, and the day may stand at the line's other end.
        if (whole(w)) return Hit(today.atStartOfDay(), null, day = false, time = false, whole = true)
        val led = w[0] in LEADING
        val hour = w[0] in HOUR
        val dotted = w[0] in DOTTED
        // ("on the 12th", „am 12.“: after these a day of the month by its number alone is a day.)
        val on = dotted || w[0] == "on"
        val body = if (led) w.subList(1, w.size) else w
        if (body.isEmpty()) return null
        Days.parse(body, today, dotted, on)?.let { return resolve(it, null, now) }
        Times.parse(body, bare = hour, afternoon = hour)?.let { return resolve(null, it, now) }
        Days.range(body, today)?.let { (first, last) -> return Hit(first.atStartOfDay(), last.plusDays(1).atStartOfDay(), day = true, time = false, whole = true) }
```

Find:

```kotlin
            var b = body.subList(i, body.size)
            val at = b[0] in HOUR
            if (b[0] in LEADING) b = b.subList(1, b.size)
            if (b.isEmpty()) continue
            val day = Days.parse(a, today)
            if (day != null) Times.parse(b, bare = at, afternoon = at)?.let { return resolve(day, it, now) }
            val clock = Times.parse(a, bare = hour, afternoon = hour)
            if (clock != null) Days.parse(b, today)?.let { return resolve(it, clock, now) }
```

Make it:

```kotlin
            val rest = body.subList(i, body.size)
            val at = rest[0] in HOUR
            val dot = rest[0] in DOTTED
            val after = dot || rest[0] == "on"
            val b = if (rest[0] in LEADING && rest[0] !in EVERY) rest.subList(1, rest.size) else rest
            if (b.isEmpty()) continue
            val day = Days.parse(a, today, dotted, on)
            if (day != null) {
                Times.parse(b, bare = at, afternoon = at)?.let { return resolve(day, it, now) }
                if (whole(rest)) return allDay(day, today)
            }
            val clock = Times.parse(a, bare = hour, afternoon = hour)
            if (clock != null) Days.parse(b, today, dot, after)?.let { return resolve(it, clock, now) }
            if (!led && whole(a)) Days.parse(b, today, dot, after)?.let { return allDay(it, today) }
```

Find:

```kotlin
    }

    private fun relative(w: List<String>, now: LocalDateTime): Hit? {
```

Make it:

```kotlin
    }

    private fun whole(w: List<String>): Boolean = w.size <= 3 && Matcher.fold(w.joinToString(" ")) in WHOLE

    /** [day], all day, said outright. */
    private fun allDay(day: Day, today: LocalDate): Hit {
        val date = when (day) { is Day.On -> day.date; is Day.Every -> next(today, day.day) }
        return Hit(date.atStartOfDay(), null, day = true, time = false, whole = true)
    }

    private fun relative(w: List<String>, now: LocalDateTime): Hit? {
```

Find:

```kotlin
                is Day.On -> day.date
```

Make it:

```kotlin
                is Day.On -> if (day.weak) return null else day.date
```

- [ ] **Step 6: A reminder needs a day or a time, in `core/src/main/kotlin/io/github/kuscher/booklight/core/Jot.kt`.** One change, in `reminder`.

Find:

```kotlin
        val hit = When.find(arg, now) ?: return null
```

Make it:

```kotlin
        // ("All day" alone names neither a day nor a time to ring at.)
        val hit = When.find(arg, now)?.takeIf { it.day || it.time } ?: return null
```

- [ ] **Step 7: Run them and see them pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.WhenTest' --tests 'io.github.kuscher.booklight.core.JotTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Run every core test, and build the app**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0 (every test that was there still passes: what the ends of a line read is read as before); then the one word `BUILT`.

- [ ] **Step 9: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/Jot.kt core/src/main/kotlin/io/github/kuscher/booklight/core/When.kt core/src/main/kotlin/io/github/kuscher/booklight/core/WhenParts.kt core/src/test/kotlin/io/github/kuscher/booklight/core/JotTest.kt core/src/test/kotlin/io/github/kuscher/booklight/core/WhenTest.kt
git commit -m "An event from a sentence: the parser reads tmrw, the 12th, the half of the day in words, a span in words, a range of days, all day said outright, and every Monday as a guess (core, tested)"
```

### Task 2: A day and a time anywhere in an event's line

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/Jot.kt`
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/When.kt`
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/WhenParts.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/JotTest.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/SpotTest.kt`

**Interfaces:**
- Consumes: Task 1's `When` (`find`, `read`, `both`, `moment`, `Hit.timed`, `SPAN`), `Words.of`, `Words.cut`, `Matcher.fold`; `Jot.event(arg, now): EventDraft`, `data class EventDraft(title, start, end, allDay, place, dayGiven, timeGiven)`.
- Produces:
  - `class Spot(val moment: Moment, val pieces: List<Piece>, val said: String)` and `class Piece(val text: String, val start: Int)`, in `When.kt`.
  - `fun When.spot(text: String, now: LocalDateTime): Spot?`: a day or a time anywhere in the line; null where there is none. `moment.rest` is the pieces joined by a space.
  - `internal fun When.leads(word: String): Boolean` (Task 4 uses it).
  - `fun Days.slight(word: String): Boolean`.
  - `EventDraft` has one more field, last: `val repeats: Boolean = false`; and `val sure: Boolean` (`dayGiven && timeGiven && !repeats`).
  - `internal fun Jot.placed(arg: String): Pair<String, String>` (the line without its place, and the place) and `internal fun Jot.draft(m: Moment?, text: String, place: String, now: LocalDateTime, title: String? = null): EventDraft`. `Jot.event` is these two with `When.spot` between them.

"Add dinner with Sam tomorrow at 7pm to the Team calendar" has its day and time in the middle, which `When.parse` has never read: it looks at a line's first and last words, and that rule stays for reminders and for everything else that calls it. An event's line is read by a second way in, `When.spot`: **the longest run of words that is one expression, wherever it stands**; where two are as long, the one at the line's start, then the one at its end, then the leftmost. So whatever `parse` reads, `spot` reads the same (`whatTheEndsReadIsReadTheSame`). Where the run is only a day or only a time, the other half is looked for before it and after it.

`spot` also says what stands around the expression: the line's **pieces**, in their order, as typed, each with where it starts in the line. Task 3 looks for a calendar at the end of a piece, calls a title "loose" when it is put together from more than one, and writes a chosen calendar back into the line at the right place.

Reading the middle of a line makes more words a day by accident. Two kinds are not read alone there: a weekday's short form that is a word as well ("we **sat** in the **sun**"), and two bare numbers with a dash ("buy **2-3** gifts"). At an end of the line both are what they have always been. A line of more than sixty words is read at its ends only.

`EventDraft` gains `repeats` and `sure`: an event is sure when its day and its time were both read (or "all day" said, or a range of days) and it is no repeat. The app has drawn "When" as a guess by that rule since 1.1; Task 5 saves by it.

- [ ] **Step 1: Write the failing tests `core/src/test/kotlin/io/github/kuscher/booklight/core/SpotTest.kt`**

```kotlin
package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A day and a time anywhere in a line ([When.spot]): what an event's sentence is read by. */
class SpotTest {
    /** Monday 5 October 2026, twenty past four: the day the sixteen sentences were tried on a device. */
    private val now = LocalDateTime.of(2026, 10, 5, 16, 20)
    private fun s(text: String) = When.spot(text, now)
    private fun at(day: Int, hour: Int = 0, minute: Int = 0) = LocalDateTime.of(2026, 10, day, hour, minute)
    private fun pieces(text: String) = s(text)!!.pieces.map { it.text }

    @Test fun aDayAndATimeInTheMiddle() {
        val x = s("Add dinner with Sam tomorrow at 7pm to Team calendar")!!
        assertEquals(at(6, 19), x.moment.start)
        assertTrue(x.moment.dayGiven && x.moment.timeGiven)
        assertEquals(listOf("Add dinner with Sam", "to Team calendar"), x.pieces.map { it.text })
        // Where each piece starts in the line: what is cut out of one can be put back in its place.
        assertEquals(listOf(0, 36), x.pieces.map { it.start })
        assertEquals("tomorrow at 7pm", x.said)
        assertEquals("Add dinner with Sam to Team calendar", x.moment.rest)
    }

    @Test fun whatTheEndsReadIsReadTheSame() {
        for (t in listOf("Fri 3pm Dentist", "Standup tomorrow 9-9:30", "tomorrow call friday", "Friday Dentist 3pm", "Dentist, Fri 3pm", "in 20m stretch",
            "Brunch am 3. Okt 2027 um 9 am - 10 am", "9-9:30 Standup tomorrow", "Fri 3pm, Dentist, the good one", "  Lunch  tomorrow ", "Team offsite Oct 14 to Oct 16"))
            assertEquals(t, When.parse(t, now), s(t)!!.moment)
        for (t in listOf("", "   ", "Dentist", "call the bank", "3 Dentists", "at", "🎉", "Launch Booklight 1.1")) assertNull(t, s(t))
    }

    @Test fun theLongestRunWinsWhereverItStands() {
        // "Friday" at the start is one word; "tomorrow at 7pm" in the middle is three.
        val x = s("Friday dinner with Sam tomorrow at 7pm to Team")!!
        assertEquals(at(6, 19), x.moment.start)
        assertEquals(listOf("Friday dinner with Sam", "to Team"), x.pieces.map { it.text })
        // As long as each other: the start, then the end, then the leftmost.
        assertEquals(at(6), s("tomorrow call friday")!!.moment.start)
        assertEquals(at(9), s("call tomorrow about friday")!!.moment.start)
        assertEquals(at(6), s("call tomorrow about friday maybe")!!.moment.start)
    }

    @Test fun theOtherHalfIsFoundWhereverItStands() {
        val x = s("Lunch on Friday with Sam at noon in the canteen")!!
        assertEquals(at(9, 12), x.moment.start)
        assertEquals(listOf("Lunch", "with Sam", "in the canteen"), x.pieces.map { it.text })
        assertEquals("on Friday at noon", x.said)
        // The time first and the day later; and a day with "all day" at the line's other end.
        assertEquals(at(9, 12), s("Sam at noon in the canteen on Friday please")!!.moment.start)
        val y = s("ganztägig Messe am Samstag in Halle 4")!!
        assertEquals(Moment(at(10), null, true, true, true, "Messe in Halle 4"), y.moment)
        // Only one of the two is there: the other is the guess, as at the ends.
        assertEquals(Moment(at(6), null, true, true, false, "call about the offer"), s("call tomorrow about the offer")!!.moment)
        assertEquals(Moment(at(5, 17), null, false, false, true, "call about the offer"), s("call at 5pm about the offer")!!.moment)
    }

    @Test fun theProbesSentencesHaveTheirDayAndTime() {
        assertEquals(Moment(at(6, 15, 30), null, false, true, true, "Schedule dentist appointment at Dr. Sam office"), s("Schedule dentist appointment next Tuesday at 3:30pm at Dr. Sam office")!!.moment)
        assertEquals(Moment(at(9, 12), null, false, true, true, "Lunch with Sam at Cafe Luna, add to Team calendar"), s("Lunch with Sam on Friday at noon at Cafe Luna, add to Team calendar")!!.moment)
        assertEquals(Moment(at(14), at(17), true, true, true, "Team offsite in Lisbon"), s("Team offsite Oct 14 to Oct 16 in Lisbon")!!.moment)
        assertEquals(Moment(at(5, 18), null, false, true, true, "Put yoga class in my Team calendar", repeats = true), s("Put yoga class every Monday 6pm in my Team calendar")!!.moment)
        assertEquals(Moment(at(12, 6, 45), null, false, true, true, "Flight to Frankfurt"), s("Flight to Frankfurt on the 12th at 6:45 in the morning")!!.moment)
        assertEquals(Moment(at(6, 9), at(6, 10), false, true, true, "meeting with the team about Q3 plan room 4B"), s("meeting with the team about Q3 plan tmrw 9-10am room 4B")!!.moment)
        assertEquals(Moment(at(6, 19), null, false, true, true, "Abendessen mit Sam in den Team-Kalender eintragen"), s("Abendessen mit Sam morgen um 19 Uhr in den Team-Kalender eintragen")!!.moment)
        assertEquals(Moment(at(9, 12), null, false, true, true, "Trag Mittagessen mit Sam im Café Luna in den Teamkalender ein"), s("Trag Mittagessen mit Sam am Freitag um 12 im Café Luna in den Teamkalender ein")!!.moment)
        assertEquals(Moment(at(14, 19, 30), null, false, true, true, "Elternabend in der Schule"), s("Elternabend am 14.10. um 19:30 in der Schule")!!.moment)
    }

    @Test fun whatIsFarMoreOftenSomethingElseIsNotReadInTheMiddle() {
        // A weekday's short form that is a word too.
        assertNull(s("the cat sat on the mat"))
        assertEquals("today", s("we sat in the sun today")!!.said)
        // Two bare numbers with a dash.
        assertNull(s("buy 2-3 gifts for Sam"))
        assertEquals("tomorrow", s("buy 2-3 gifts tomorrow")!!.said)
        // At an end both are what they have always been; a whole name, a led one and a marked one are read anywhere.
        assertEquals(at(10), s("sat dinner")!!.moment.start)
        assertEquals(at(6, 9), s("party 9-11")!!.moment.start)
        assertEquals(at(10), s("dinner Saturday with Sam")!!.moment.start)
        assertEquals(at(10), s("dinner on sat with Sam")!!.moment.start)
        assertEquals(at(6, 14), s("call 2-3pm with Sam")!!.moment.start)
    }

    @Test fun aLongLineIsReadAtItsEndsOnly() {
        val filler = "word ".repeat(70)
        assertNull(s(filler + "tomorrow " + filler))
        assertEquals(at(6), s("tomorrow $filler")!!.moment.start)
        assertEquals(at(5, 17), s(filler + "5pm")!!.moment.start)
        assertEquals(at(6, 17), s("tomorrow " + filler + "5pm")!!.moment.start)
    }

    @Test fun nothingThrows() {
        val bits = listOf("fri", "3pm", "in", "20m", "at", "um", "am", "on", "-", "9", "25", ":", "@", "uhr", "10/3", "3.10.", "oct", "9-9:30", "noon", "tomorrow", "x", "🎉",
            "to", "from", "bis", "von", "the", "12th", "every", "jeden", "all", "day", "ganztägig", "morning", "abends", "den", "14-16", "of", "tmrw", "add", "calendar", ",", "sat")
        val random = java.util.Random(11)
        repeat(20_000) {
            val text = List(1 + random.nextInt(9)) { bits[random.nextInt(bits.size)] }.joinToString(if (random.nextBoolean()) " " else "  ")
            val x = When.spot(text, now) ?: return@repeat
            // Every piece is a piece of the line, where it says it is; and the rest is the pieces.
            for (p in x.pieces) { assertFalse(text, p.text.isEmpty()); assertEquals(text, p.text, text.substring(p.start, p.start + p.text.length)) }
            assertEquals(text, x.pieces.joinToString(" ") { it.text }, x.moment.rest)
        }
    }
}
```

- [ ] **Step 2: The event's tests, in `core/src/test/kotlin/io/github/kuscher/booklight/core/JotTest.kt`.** One change: three tests before the reminder's.

Find:

```kotlin
    }

    // Reminder

    @Test fun inSoLongIsATimer() {
```

Make it:

```kotlin
    }

    @Test fun anEventsDayAndTimeMayStandInTheMiddle() {
        assertEquals(EventDraft("call about it", at(2), at(3), true, "", dayGiven = true, timeGiven = false), Jot.event("call tomorrow about it", now))
        assertEquals(EventDraft("Dinner with Sam at Luigi’s", at(2, 19), at(2, 20), false, "", dayGiven = true, timeGiven = true),
            Jot.event("Dinner with Sam tomorrow at 7pm at Luigi’s", now))
        assertEquals(EventDraft("Dinner with Sam", at(2, 19), at(2, 20), false, "Luigi’s", dayGiven = true, timeGiven = true),
            Jot.event("Dinner with Sam tomorrow at 7pm @ Luigi’s", now))
    }

    @Test fun anEventOverSeveralDaysAndOneThatIsAllDay() {
        // A range of days ends at the midnight after its last day: that is how calendars keep one.
        assertEquals(EventDraft("Team offsite", at(14), at(17), true, "Lisbon", dayGiven = true, timeGiven = true), Jot.event("Team offsite Oct 14 to Oct 16 @ Lisbon", now))
        assertEquals(EventDraft("Birthday party for Sam", LocalDateTime.of(2026, 11, 3, 0, 0), LocalDateTime.of(2026, 11, 4, 0, 0), true, "", dayGiven = true, timeGiven = true),
            Jot.event("Birthday party for Sam on November 3rd all day", now))
    }

    @Test fun whenAnEventIsSure() {
        // Sure: a day and a time that were both typed, a day with "all day", a range of days.
        for (t in listOf("Call mom Sunday 10am", "Geburtstag von Oma am Samstag ganztägig", "Team offsite Oct 14 to Oct 16", "Standup tomorrow from 9 to 10"))
            assertEquals(t, true, Jot.event(t, now).sure)
        // A guess: no day, no time, neither, or a repeat (the day that was read is only the next one).
        for (t in listOf("5pm Call", "Offsite tomorrow", "Dentist", "", "yoga class every Monday 6pm", "Inventory all day"))
            assertEquals(t, false, Jot.event(t, now).sure)
        assertEquals(true, Jot.event("yoga class every Monday 6pm", now).repeats)
    }

    // Reminder

    @Test fun inSoLongIsATimer() {
```

- [ ] **Step 3: Run them and see them fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SpotTest' --tests 'io.github.kuscher.booklight.core.JotTest' --console=plain
```

Expected: `BUILD FAILED` in `:core:compileTestKotlin`: `Unresolved reference 'spot'` in `SpotTest`, and `Unresolved reference 'sure'` and `'repeats'` in `JotTest`.

- [ ] **Step 4: A short weekday that is a word, in `core/src/main/kotlin/io/github/kuscher/booklight/core/WhenParts.kt`.** Two changes, in `Days`.

Find:

```kotlin
    /** Between two days: "Oct 14 to Oct 16", „14. bis 16. Oktober“. And what may stand before the first. */
```

Make it:

```kotlin
    /** A weekday's short form that is an ordinary word as often as a day ("the sun", "we sat"): alone in the middle of a sentence it is not read. */
    private val SLIGHT = setOf("mon", "tue", "tues", "wed", "weds", "thu", "thur", "thurs", "fri", "sat", "sun")
    /** Between two days: "Oct 14 to Oct 16", „14. bis 16. Oktober“. And what may stand before the first. */
```

Find:

```kotlin
    }

    /**
     * [dotted]: the words follow „am“ or „den“, where a number with a dot is a day of the month („am 12.“).
```

Make it:

```kotlin
    }

    /** True for a weekday's short form that is a word too ([SLIGHT]). */
    fun slight(word: String): Boolean = Matcher.fold(word) in SLIGHT

    /**
     * [dotted]: the words follow „am“ or „den“, where a number with a dot is a day of the month („am 12.“).
```

- [ ] **Step 5: `spot`, in `core/src/main/kotlin/io/github/kuscher/booklight/core/When.kt`.** Four changes: `Spot` and `Piece` above the header; a line of the header; two constants; and `spot` and `leads` above `findOne`.

Find:

```kotlin
    val dayGiven: Boolean, val timeGiven: Boolean, val rest: String, val repeats: Boolean = false)

/**
 * Reads when something is, in English and German: "Fri 3pm Dentist", "Standup tomorrow 9-9:30",
```

Make it:

```kotlin
    val dayGiven: Boolean, val timeGiven: Boolean, val rest: String, val repeats: Boolean = false)

/**
 * A day or a time found anywhere in a line ([When.spot]): what it says ([moment], whose rest is the
 * line without the expression), the line's [pieces] around the expression in their order and as they
 * were typed (none of them empty), and the expression itself as typed ([said]).
 */
class Spot(val moment: Moment, val pieces: List<Piece>, val said: String)

/** A piece of a line, as typed, and where it starts in the line. */
class Piece(val text: String, val start: Int)

/**
 * Reads when something is, in English and German: "Fri 3pm Dentist", "Standup tomorrow 9-9:30",
```

Find:

```kotlin
 *   wins; if both ends read equally long, the start. A day and a time may stand in either order.
```

Make it:

```kotlin
 *   wins; if both ends read equally long, the start. A day and a time may stand in either order.
 *   (An event's line is read in its middle too: [spot].)
```

Find:

```kotlin
    private val WHOLE = setOf("all day", "allday", "ganztagig", "ganztaegig", "ganztags", "den ganzen tag")

    /** What [text] says about when, or null when neither end of it is a day or a time. */
```

Make it:

```kotlin
    private val WHOLE = setOf("all day", "allday", "ganztagig", "ganztaegig", "ganztags", "den ganzen tag")
    /** The most words of a line whose middle is read ([spot]): a longer one is read at its ends alone. */
    private const val WORDS = 60
    /** Two numbers and a dash, with nothing that says they are times: "2-3". */
    private val BARE = Regex("[0-9]{1,2}[-–—][0-9]{1,2}")

    /** What [text] says about when, or null when neither end of it is a day or a time. */
```

Find:

```kotlin
    }

    private fun findOne(text: String, now: LocalDateTime): Hit? {
```

Make it:

```kotlin
    }

    /**
     * A day or a time anywhere in [text], for a line that is an event: the longest run of words that is
     * one expression; where two are as long, the one at the line's start, then the one at its end, then
     * the leftmost. Where that is only a day or only a time, the other half is looked for in what is
     * left, wherever it stands. So whatever [parse] reads is read the same, and "Add dinner tomorrow at
     * 7pm to the Team calendar" is read too. Two things are not read alone in the middle, where they
     * are far more often something else: a weekday's short form that is a word as well ("we sat"), and
     * two bare numbers with a dash ("2-3 eggs"). A line of more than [WORDS] words is read at its ends only.
     */
    fun spot(text: String, now: LocalDateTime): Spot? {
        val words = Words.of(text)
        val n = words.size
        if (n > WORDS) return find(text, now)?.let { h -> Spot(moment(h, h.rest), listOfNotNull(h.rest.takeIf { it.isNotEmpty() }?.let { Piece(it, text.indexOf(it).coerceAtLeast(0)) }), "") }
        val low = words.map { it.text.lowercase().trimEnd(',', ';') }
        class Run(val from: Int, val to: Int, val hit: Hit)
        /** The best run of the words from [a] up to [b] that is one expression and passes [fits]. */
        fun best(a: Int, b: Int, fits: (Hit) -> Boolean): Run? {
            for (k in minOf(b - a, SPAN) downTo 1) {
                // (The start, the end, then the middle from the left.)
                for (i in (sequenceOf(a, b - k) + (a + 1 until b - k)).distinct()) {
                    if (k == 1 && i > 0 && i + 1 < n && (Days.slight(low[i]) || BARE.matches(low[i]))) continue
                    val h = read(low.subList(i, i + k), now) ?: continue
                    if (fits(h)) return Run(i, i + k, h)
                }
            }
            return null
        }
        val one = best(0, n) { true } ?: return null
        var runs = listOf(one)
        var hit = one.hit
        if (hit.seconds == null && !(hit.day && hit.timed)) {
            // Only a day, or only a time: the other half, before it or after it. The longer of the two; after it, where they are as long.
            val other: (Hit) -> Boolean = { it.seconds == null && it.day != one.hit.day && it.timed != one.hit.timed }
            val before = best(0, one.from, other)
            val after = best(one.to, n, other)
            val two = if (before != null && (after == null || before.to - before.from > after.to - after.from)) before else after
            if (two != null) { hit = both(one.hit, two.hit); runs = listOf(one, two).sortedBy { it.from } }
        }
        val pieces = ArrayList<Piece>(3)
        var from = 0
        for (r in runs + Run(n, n, hit)) {
            if (r.from > from) Words.cut(text, words, from, r.from).trimEnd(',', ';').takeIf { it.isNotEmpty() }?.let { pieces.add(Piece(it, words[from].start)) }
            from = r.to
        }
        return Spot(moment(hit, pieces.joinToString(" ") { it.text }), pieces, runs.joinToString(" ") { Words.cut(text, words, it.from, it.to) })
    }

    /** True for a word that leads into a day or a time and means nothing without it ("on", "the", "at", „am“). */
    internal fun leads(word: String): Boolean = word.lowercase().let { it in LEADING || it == "the" || it == "from" || it == "von" || it == "vom" }

    private fun findOne(text: String, now: LocalDateTime): Hit? {
```

- [ ] **Step 6: The event reads by it, in `core/src/main/kotlin/io/github/kuscher/booklight/core/Jot.kt`.** Two changes: `EventDraft`, and `event` with `placed` and `draft`.

Find:

```kotlin
 * guessed (today, all day, one hour) and shown dimmer in the preview.
 */
data class EventDraft(val title: String, val start: LocalDateTime, val end: LocalDateTime, val allDay: Boolean,
    val place: String, val dayGiven: Boolean, val timeGiven: Boolean)
```

Make it:

```kotlin
 * guessed (today, all day, one hour) and shown dimmer in the preview. "All day" said outright, and a
 * range of days, count as the time given. [repeats]: the line said "every Monday", which this version
 * cannot carry: the day is the next such day, and no more than a guess.
 */
data class EventDraft(val title: String, val start: LocalDateTime, val end: LocalDateTime, val allDay: Boolean,
    val place: String, val dayGiven: Boolean, val timeGiven: Boolean, val repeats: Boolean = false) {
    /** The day and the time were both read from what was typed: nothing of when it is is a guess. */
    val sure: Boolean get() = dayGiven && timeGiven && !repeats
}
```

Find:

```kotlin
     * "Fri 3pm Dentist @ Main St": what follows the last " @ " is the place, a day or time at either
     * end is when ([When]), the rest is the title. No day: today. No time: all day. No end: one hour.
     */
    fun event(arg: String, now: LocalDateTime): EventDraft {
        var text = " " + arg.trim()
        var place = ""
        val at = text.lastIndexOf(" @ ")
        if (at >= 0) {
            place = text.substring(at + 3).trim()
            text = text.substring(0, at)
        } else if (text.endsWith(" @")) {
            text = text.dropLast(2)                       // the place is about to be typed
        }
        text = text.trim()
        val m = When.parse(text, now)
            ?: return now.toLocalDate().atStartOfDay().let { EventDraft(text, it, it.plusDays(1), true, place, dayGiven = false, timeGiven = false) }
        val end = m.end ?: if (m.allDay) m.start.plusDays(1) else m.start.plusHours(1)
        return EventDraft(m.rest, m.start, end, m.allDay, place, m.dayGiven, m.timeGiven)
```

Make it:

```kotlin
     * "Fri 3pm Dentist @ Main St": what follows the last " @ " is the place, a day or time anywhere in
     * the line is when ([When.spot]), the rest is the title. No day: today. No time: all day. No end:
     * one hour.
     */
    fun event(arg: String, now: LocalDateTime): EventDraft {
        val (text, place) = placed(arg)
        return draft(When.spot(text, now)?.moment, text, place, now)
    }

    /** [arg] without its place, and the place: what follows the last " @ ". A " @" at the very end is a place about to be typed. */
    internal fun placed(arg: String): Pair<String, String> {
        val text = " " + arg.trim()
        val at = text.lastIndexOf(" @ ")
        return when {
            at >= 0 -> text.substring(0, at).trim() to text.substring(at + 3).trim()
            text.endsWith(" @") -> text.dropLast(2).trim() to ""
            else -> text.trim() to ""
        }
    }

    /** The event that [m] and its rest make; with no day or time read, [text] is the title, today, all day. */
    internal fun draft(m: Moment?, text: String, place: String, now: LocalDateTime, title: String? = null): EventDraft {
        if (m == null) return now.toLocalDate().atStartOfDay().let { EventDraft(title ?: text, it, it.plusDays(1), true, place, dayGiven = false, timeGiven = false) }
        val end = m.end ?: if (m.allDay) m.start.plusDays(1) else m.start.plusHours(1)
        return EventDraft(title ?: m.rest, m.start, end, m.allDay, place, m.dayGiven, m.timeGiven, m.repeats)
```

- [ ] **Step 7: Run them and see them pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SpotTest' --tests 'io.github.kuscher.booklight.core.JotTest' --tests 'io.github.kuscher.booklight.core.WhenTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Run every core test, and build the app**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT` (the app builds an `EventDraft` nowhere itself, and reads its old fields only).

- [ ] **Step 9: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/Jot.kt core/src/main/kotlin/io/github/kuscher/booklight/core/When.kt core/src/main/kotlin/io/github/kuscher/booklight/core/WhenParts.kt core/src/test/kotlin/io/github/kuscher/booklight/core/JotTest.kt core/src/test/kotlin/io/github/kuscher/booklight/core/SpotTest.kt
git commit -m "An event from a sentence: an event's day and time are found anywhere in its line, not only at its ends (core, tested)"
```

### Task 3: A sentence read by rules

**Files:**
- Create: `core/src/main/kotlin/io/github/kuscher/booklight/core/Sentence.kt`
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/When.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/SentenceTest.kt`

**Interfaces:**
- Consumes: `When.spot`, `Spot`, `Piece` (Task 2); `Jot.placed`, `Jot.draft` (Task 2); `EventDraft`; `Words`, `Matcher.fold`; in `When.kt` the private `BARE` and `Days.slight` (Task 2).
- Produces, in the new `core/…/Sentence.kt`:
  - `data class Cal(val id: Long, val name: String, val color: Int = 0, val owner: String = "", val primary: Boolean = false, val writable: Boolean = true)`: one of the user's calendars, as the system lists it.
  - `data class EventReading(val draft: EventDraft, val calendar: Cal? = null, val rest: String? = null, val named: Boolean = false, val cue: Boolean = false, val loose: Boolean = false, val said: String = "")`.
  - `object Sentence`: `fun read(text: String, now: LocalDateTime, calendars: List<Cal> = emptyList()): EventReading`; `fun reads(r: EventReading): Boolean`; `fun put(text: String, name: String, word: String, now: LocalDateTime, calendars: List<Cal>): String`; `internal fun uncued(title: String): String`; `internal fun bare(name: String): Pair<String, Boolean>`.
  - `object Cals`: `fun named(folded: String, all: List<Cal>): Cal?`, `fun starting(typed: String, all: List<Cal>): Cal?`. (Task 5 adds to it.)
  - `internal fun When.slight(said: String): Boolean`.

(The class is `EventReading` because core has a `Reading` already, in `Verbs.kt`.)

This is the reading that is there in the frame of every key. Five rules, in this order:

1. **The cue.** A sentence that begins with "add", "schedule", "put", "new event" („trag“, „trage“, „neuer Termin“) is an event, and the word is dropped; „trag … ein“ loses its „ein“ too, and „… eintragen“ at the end is dropped whether a cue began the line or not. **"Book" and "plan" („plane“) are cues that stay in the title** ("Decided here", 3).
2. **The place**: what follows the last " @ ", as under the keyword (`Jot.placed`).
3. **The day and time**, wherever they stand (`When.spot`), and the line's pieces around them.
4. **The calendar.** At the end of a piece, the last piece first: "to", "in", "on" („im“, „zum“ …), then "the" or "my" („den“, „meinen“ …), then the name, with or without "calendar", „Kalender“ or „-Kalender“ joined to it. The name is matched against the list without regard to case and to an s at its end (`Cals.named`), and its phrase is cut from the piece, with an "add" or "put" that stands before it ("…, add to the Team calendar"). **Only at the very end of the typed text** the start of a name is enough, from two letters (`Cals.starting`): the reading then holds that calendar and `rest`, what is left of its name for the field to show in grey. There too a whole name with the word "calendar" („Kalender“) still being typed after it is that calendar ("… to the Team cal", „… im Teamkal“), with no `rest`: the row would otherwise lose its calendar at the "c" and have it again at the "r". A calendar named by the word that is none of the list (or the list is empty: not allowed) stays in the title, and the reading says `named`.
5. **The title**: the pieces that are left, joined. Where more than one is left the reading is `loose`: the title is put together, and one of its pieces may be a place. That is the only case in which Task 4 asks the model. (A piece that is wholly a calendar named by the word does not count: it is known what it is.)

`reads` says whether a line typed **without the keyword** is offered as an event: a cue, a day or a time, something left to call it, and not where the expression is one that is far more often something else ("add pictures of the sun", "add 2-3 eggs").

`put` writes a calendar chosen from the row's list into the sentence: in place of the calendar it names now (or of the letters a name began with), else after the sentence's own words and before a place or a closing „ein“.

The tests read every beginning of a sentence too, as the panel does at every key: none throws, none shows a word that was not typed, and the calendar stays from its name's last letter to the sentence's end.

The test's table is the sixteen sentences that were tried on a device, the people in them called Sam and the calendars Team: the rules alone have ten of them right from end to end, and the other six have their place in the title, loose.

- [ ] **Step 1: Write the failing tests `core/src/test/kotlin/io/github/kuscher/booklight/core/SentenceTest.kt`**

```kotlin
package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An event read from a sentence by rules ([Sentence.read]). The table is the sixteen sentences that were
 * tried on a device (docs/research/event-sentence.md), with the people in them called Sam and the
 * calendars Team.
 */
class SentenceTest {
    /** Monday 5 October 2026, twenty past four: the day those sentences were tried. */
    private val now = LocalDateTime.of(2026, 10, 5, 16, 20)
    private fun at(day: Int, hour: Int = 0, minute: Int = 0, month: Int = 10) = LocalDateTime.of(2026, month, day, hour, minute)

    private val sam = Cal(1, "Sam", 0xFF3F51B5.toInt(), "sam@example.com", primary = true)
    private val team = Cal(2, "Team", 0xFF0B8043.toInt(), "team@group.calendar.google.com")
    private val trips = Cal(3, "Team trips", 0xFFF4511E.toInt(), "trips@group.calendar.google.com")
    private val holidays = Cal(4, "Holidays", 0xFF7986CB.toInt(), "holidays@group.v.calendar.google.com", writable = false)
    private val mine = listOf(sam, team, trips, holidays)

    private fun r(text: String, calendars: List<Cal> = mine) = Sentence.read(text, now, calendars)

    /** One line of the table: the sentence, and what the rules make of it. */
    private class Row(val text: String, val title: String, val start: LocalDateTime, val end: LocalDateTime, val allDay: Boolean = false, val place: String = "",
        val calendar: Cal? = null, val sure: Boolean = true, val loose: Boolean = false, val cue: Boolean = false)

    private val table = listOf(
        Row("Add dinner with Sam tomorrow at 7pm to Team calendar", "dinner with Sam", at(6, 19), at(6, 20), calendar = team, cue = true),
        // A place without an @ stays in the title, and the reading is loose: the model may split it.
        Row("Schedule dentist appointment next Tuesday at 3:30pm at Dr. Sam office", "dentist appointment at Dr. Sam office", at(6, 15, 30), at(6, 16, 30), loose = true, cue = true),
        Row("Lunch with Sam on Friday at noon at Cafe Luna, add to Team calendar", "Lunch with Sam at Cafe Luna", at(9, 12), at(9, 13), calendar = team, loose = true),
        Row("Team offsite Oct 14 to Oct 16 in Lisbon", "Team offsite in Lisbon", at(14), at(17), allDay = true, loose = true),
        Row("Call mom Sunday 10am", "Call mom", at(11, 10), at(11, 11)),
        // "every Monday": the next one (today is a Monday, and six is still ahead), and a guess.
        Row("Put yoga class every Monday 6pm in my Team calendar", "yoga class", at(5, 18), at(5, 19), calendar = team, sure = false, cue = true),
        Row("Flight to Frankfurt on the 12th at 6:45 in the morning", "Flight to Frankfurt", at(12, 6, 45), at(12, 7, 45)),
        Row("Add Team bowling night to the Team calendar Saturday 8pm", "Team bowling night", at(10, 20), at(10, 21), calendar = team, cue = true),
        Row("meeting with the team about Q3 plan tmrw 9-10am room 4B", "meeting with the team about Q3 plan room 4B", at(6, 9), at(6, 10), loose = true),
        Row("Birthday party for Sam on November 3rd all day", "Birthday party for Sam", at(3, month = 11), at(4, month = 11), allDay = true),
        Row("Abendessen mit Sam morgen um 19 Uhr in den Team-Kalender eintragen", "Abendessen mit Sam", at(6, 19), at(6, 20), calendar = team),
        Row("Zahnarzt nächsten Dienstag 15:30 Uhr", "Zahnarzt", at(6, 15, 30), at(6, 16, 30)),
        Row("Trag Mittagessen mit Sam am Freitag um 12 im Café Luna in den Teamkalender ein", "Mittagessen mit Sam im Café Luna", at(9, 12), at(9, 13), calendar = team, loose = true, cue = true),
        Row("Elternabend am 14.10. um 19:30 in der Schule", "Elternabend in der Schule", at(14, 19, 30), at(14, 20, 30), loose = true),
        Row("Übermorgen 8 Uhr Friseur", "Friseur", at(7, 8), at(7, 9)),
        Row("Geburtstag von Oma am Samstag ganztägig", "Geburtstag von Oma", at(10), at(11), allDay = true),
    )

    @Test fun theSixteenSentences() {
        assertEquals(16, table.size)
        for (row in table) {
            val got = r(row.text)
            assertEquals(row.text, row.title, got.draft.title)
            assertEquals(row.text, row.start, got.draft.start)
            assertEquals(row.text, row.end, got.draft.end)
            assertEquals(row.text, row.allDay, got.draft.allDay)
            assertEquals(row.text, row.place, got.draft.place)
            assertEquals(row.text, row.calendar, got.calendar)
            assertEquals(row.text, row.sure, got.draft.sure)
            assertEquals(row.text, row.loose, got.loose)
            assertEquals(row.text, row.cue, got.cue)
            assertNull(row.text, got.rest)
            assertFalse(row.text, got.named)
        }
        // Ten of the sixteen are right from end to end by the rules alone; the other six have their place in the title.
        assertEquals(10, table.count { !it.loose })
    }

    @Test fun theCueIsDroppedOrKept() {
        assertEquals("dinner", r("Add dinner tomorrow 7pm").draft.title)
        assertEquals("dinner", r("add  dinner tomorrow 7pm").draft.title)
        assertEquals("dentist", r("Schedule dentist Friday 3pm").draft.title)
        assertEquals("yoga", r("Put yoga Friday 6pm").draft.title)
        assertEquals("dinner", r("New event dinner tomorrow 7pm").draft.title)
        assertEquals("Zahnarzt", r("Neuer Termin Zahnarzt morgen 15 Uhr").draft.title)
        assertEquals("Zahnarzt", r("Trage Zahnarzt morgen 15 Uhr ein").draft.title)
        assertEquals("Zahnarzt", r("Zahnarzt morgen 15 Uhr eintragen").draft.title)
        // "Book club" and "Plan review" are what such events are called: these two say it is an event and stay.
        for (t in listOf("Book club Friday 7pm" to "Book club", "Plan review tomorrow 10am" to "Plan review", "book dentist tomorrow 3pm" to "book dentist", "Plane Offsite am 14.10." to "Plane Offsite")) {
            assertEquals(t.first, t.second, r(t.first).draft.title)
            assertTrue(t.first, r(t.first).cue)
        }
        // Only at the start, only as a whole word, and „ein“ only where „trag“ began.
        assertEquals("Add-ons review", r("Add-ons review Friday 3pm").draft.title)
        assertFalse(r("Add-ons review Friday 3pm").cue)
        assertEquals("Addition lesson", r("Addition lesson tomorrow").draft.title)
        assertFalse(r("Addition lesson tomorrow").cue)
        assertEquals("Kaufe ein", r("Kaufe ein morgen 15 Uhr").draft.title)
        // The cue alone, and nothing at all.
        assertEquals("", r("Add").draft.title)
        assertTrue(r("Add").cue)
        assertEquals(EventDraft("", at(5), at(6), true, "", dayGiven = false, timeGiven = false), r("").draft)
    }

    @Test fun thePlaceFollowsAnAt() {
        val got = r("Add dinner with Sam tomorrow at 7pm to Team @ Cafe Luna")
        assertEquals("dinner with Sam", got.draft.title)
        assertEquals("Cafe Luna", got.draft.place)
        assertEquals(team, got.calendar)
        assertFalse(got.loose)
        assertEquals("Cafe Luna", r("Add dinner @ Cafe Luna").draft.place)
    }

    @Test fun aLineTypedWithoutTheKeywordIsOfferedAsAnEvent() {
        for (t in listOf("Add dinner with Sam tomorrow at 7pm", "add dinner 7pm", "Schedule dentist Friday", "Put yoga class every Monday 6pm in my Team calendar",
            "Book dentist tomorrow 3pm", "Plan offsite Oct 14 to Oct 16", "Trag Mittagessen am Freitag um 12 ein", "Neuer Termin Zahnarzt morgen", "Plane Offsite am 14.10.",
            "Add lunch on sat", "add call 2-3pm"))
            assertTrue(t, Sentence.reads(r(t)))
        for (t in listOf(
            // No cue: under the keyword it is an event, without it an ordinary line.
            "Lunch with Sam on Friday at noon", "Call mom Sunday 10am", "dinner tomorrow", "Zahnarzt morgen 15 Uhr",
            // No day and no time.
            "Add dinner with Sam", "add", "book a table", "schedule", "plan b",
            // Nothing left to call it.
            "Add tomorrow", "add 7pm", "schedule friday 3pm",
            // What is far more often something else: a short weekday alone, two bare numbers.
            "add pictures of the sun", "add 2-3 eggs", "book 2-3 nights", "put the cat out, sat",
            // A longer word that only begins like a cue.
            "Additional notes tomorrow", "Booking tomorrow 3pm", "planet tomorrow",
        )) assertFalse(t, Sentence.reads(r(t)))
    }

    @Test fun aCalendarIsFoundByItsName() {
        for (t in listOf("dinner tomorrow 7pm to Team", "dinner tomorrow 7pm to the Team calendar", "dinner tomorrow 7pm in team", "dinner tomorrow 7pm on my TEAM calendar",
            "dinner tomorrow 7pm to Teams", "dinner to Team tomorrow 7pm", "Abendessen morgen 19 Uhr in den Team-Kalender", "Abendessen morgen 19 Uhr im Teamkalender",
            "Abendessen morgen 19 Uhr in meinen Team Kalender", "dinner tomorrow 7pm, add to Team", "dinner tomorrow 7pm, put it in the Team calendar")) {
            assertEquals(t, team, r(t).calendar)
            assertEquals(t, if (t.startsWith("A")) "Abendessen" else "dinner", r(t).draft.title)
            assertFalse(t, r(t).loose)
        }
        // A name of several words; the whole name wins over a name that begins the same.
        assertEquals(trips, r("flight Friday 9am to Team trips").calendar)
        assertEquals("flight", r("flight Friday 9am to Team trips").draft.title)
        assertEquals(trips, r("flight Friday 9am to the team trips calendar").calendar)
        // The account's own calendar, by its name.
        assertEquals(sam, r("dentist Friday 9am in Sam").calendar)
    }

    @Test fun wordsThatAreNoCalendarStayInTheTitle() {
        // Not after "to", "in" or "on"; not in the middle of a piece; a word that only begins like the name, anywhere but at the very end.
        assertNull(r("dinner with the Team tomorrow 7pm").calendar)
        assertEquals("dinner with the Team", r("dinner with the Team tomorrow 7pm").draft.title)
        assertNull(r("Team dinner tomorrow 7pm").calendar)
        assertNull(r("send invites to Team members tomorrow 9am").calendar)
        assertEquals("send invites to Team members", r("send invites to Team members tomorrow 9am").draft.title)
        assertNull(r("move to Te tomorrow 9am").calendar)
        assertEquals("Flight to Frankfurt", r("Flight to Frankfurt on the 12th at 6:45 in the morning").draft.title)
    }

    /**
     * A calendar's name is an ordinary word too. After "to", "in" or "on" at the end of a piece it is the calendar, as the
     * design has it: the row says so (its Calendar slot, and the title without the word) before anything is opened or saved.
     */
    @Test fun aCalendarsNameThatIsAlsoAnOrdinaryWordIsTheCalendar() {
        val got = r("Add handover to team tomorrow 9am")
        assertEquals(team, got.calendar)
        assertEquals("handover", got.draft.title)
        // Without such a calendar the words stay where they are.
        val none = r("Add handover to team tomorrow 9am", listOf(sam))
        assertNull(none.calendar)
        assertEquals("handover to team", none.draft.title)
    }

    @Test fun aCalendarThatIsNoneOfYoursStaysInTheTitle() {
        // Named by the word: the reading says so, the name stays, and the model is not asked about a piece that is known.
        val unknown = r("Add dinner tomorrow at 7pm to the Garden calendar")
        assertNull(unknown.calendar)
        assertTrue(unknown.named)
        assertEquals("dinner to the Garden calendar", unknown.draft.title)
        assertFalse(unknown.loose)
        // The calendars are not known at all (not allowed): the same, for every calendar.
        val blind = r("Add dinner with Sam tomorrow at 7pm to Team calendar", emptyList())
        assertNull(blind.calendar)
        assertTrue(blind.named)
        assertEquals("dinner with Sam to Team calendar", blind.draft.title)
        assertFalse(blind.loose)
        val german = r("Abendessen mit Sam morgen um 19 Uhr in den Team-Kalender eintragen", emptyList())
        assertTrue(german.named)
        assertEquals("Abendessen mit Sam in den Team-Kalender", german.draft.title)
        // Without the word nothing says it is a calendar: an ordinary piece of the title.
        val plain = r("Add dinner tomorrow at 7pm to Garden")
        assertFalse(plain.named)
        assertTrue(plain.loose)
        // "the calendar" names none.
        assertFalse(r("Add dinner tomorrow at 7pm to the calendar").named)
    }

    @Test fun theStartOfACalendarsNameAtTheVeryEnd() {
        val got = r("Add dinner with Sam tomorrow at 7pm to tea")
        assertEquals(team, got.calendar)
        assertEquals("m", got.rest)
        assertEquals("dinner with Sam", got.draft.title)
        assertEquals("am", r("Add dinner tomorrow 7pm to the Te").rest)
        assertEquals("rips", r("flight Friday 9am to team t").rest)
        assertEquals(trips, r("flight Friday 9am to team t").calendar)
        assertEquals("lidays", r("Add trip tomorrow in ho", listOf(holidays)).rest)
        // Two letters at least, and the whole name has nothing left to complete.
        assertNull(r("Add dinner tomorrow 7pm to t").rest)
        assertNull(r("Add dinner tomorrow 7pm to t").calendar)
        assertNull(r("Add dinner tomorrow 7pm to Team").rest)
        // Only at the very end of the sentence: not before the day, not before a place, not before „ein“.
        assertNull(r("Add dinner to tea tomorrow 7pm").calendar)
        assertEquals("dinner to tea", r("Add dinner to tea tomorrow 7pm").draft.title)
        assertNull(r("Add dinner tomorrow 7pm to tea @ Cafe Luna").calendar)
        assertNull(r("Trag Essen morgen um 12 in tea ein").calendar)
        // And not with the word after it: "tea calendar" is a calendar that is none of yours.
        assertTrue(r("Add dinner tomorrow 7pm to tea calendar").named)
    }

    /** From the letter a calendar's name is whole, the row holds that calendar at every letter that follows: it never comes and goes while "calendar" is typed. */
    @Test fun theCalendarStaysWhileTheWordCalendarIsTyped() {
        class Typed(val whole: String, val name: String, val calendar: Cal, val title: String)
        for (t in listOf(
            Typed("Add dinner with Sam tomorrow at 7pm to the Team calendar", "Team", team, "dinner with Sam"),
            Typed("Abendessen morgen um 19 Uhr in den Team-Kalender", "Team", team, "Abendessen"),
            Typed("Abendessen morgen um 19 Uhr im Teamkalender", "Team", team, "Abendessen"),
            Typed("flight Friday 9am to Team trips calendar", "Team trips", trips, "flight"),
        )) {
            // Every beginning from the name's last letter on. (A space at the end is no letter: a sentence is read trimmed.)
            for (n in t.whole.indexOf(t.name) + t.name.length..t.whole.length) {
                val typed = t.whole.take(n)
                assertEquals(typed, t.calendar, r(typed).calendar)
                assertEquals(typed, t.title, r(typed).draft.title)
            }
        }
        // Only at the very end, and only the word: "Team c" before the day is no calendar, and "Team x" never.
        assertNull(r("Add dinner to Team c tomorrow 7pm").calendar)
        assertNull(r("Add dinner tomorrow 7pm to Team x").calendar)
    }

    @Test fun everyLetterOfEverySentenceIsRead() {
        // The row is made at every key: no beginning of any of the sixteen sentences throws, and none shows a word that was not typed.
        for (row in table) for (n in 0..row.text.length) {
            val typed = row.text.take(n)
            val got = r(typed)
            Sentence.reads(got)
            for (w in (got.draft.title + " " + got.draft.place).split(' ').filter { it.isNotEmpty() }) assertTrue("$typed: $w", w in typed)
        }
    }

    @Test fun aCalendarChosenFromTheListIsWrittenIntoTheSentence() {
        fun put(text: String, name: String = "Team", word: String = "to") = Sentence.put(text, name, word, now, mine)
        // None named: after the sentence's own words.
        assertEquals("Add dinner tomorrow 7pm to Team", put("Add dinner tomorrow 7pm"))
        assertEquals("dinner tomorrow 7pm to Team trips", put("dinner tomorrow 7pm ", "Team trips"))
        // Before a place, and before „ein“.
        assertEquals("Add dinner tomorrow 7pm to Team @ Cafe Luna", put("Add dinner tomorrow 7pm @ Cafe Luna"))
        assertEquals("Trag Essen morgen um 12 in Team ein", put("Trag Essen morgen um 12 ein", word = "in"))
        // One named, or begun: in its place.
        assertEquals("Add dinner tomorrow 7pm to Sam", put("Add dinner tomorrow 7pm to Team", "Sam"))
        assertEquals("Add dinner tomorrow 7pm to the Sam", put("Add dinner tomorrow 7pm to the Team calendar", "Sam"))
        assertEquals("Add dinner tomorrow 7pm to Team", put("Add dinner tomorrow 7pm to tea"))
        assertEquals("Add dinner to Sam tomorrow 7pm", put("Add dinner to Team tomorrow 7pm", "Sam"))
        assertEquals("Add dinner tomorrow 7pm to the Team", put("Add dinner tomorrow 7pm to the Garden calendar"))
        // What it makes is read as that calendar.
        for (t in listOf("Add dinner tomorrow 7pm", "Add dinner tomorrow 7pm @ Cafe Luna", "Add dinner tomorrow 7pm to tea", "dinner", ""))
            for (c in listOf(sam, team, trips)) assertEquals("$t + ${c.name}", c, r(put(t, c.name)).calendar)
    }

    @Test fun nothingThrowsAndNothingIsLost() {
        val bits = listOf("add", "Add", "trag", "ein", "eintragen", "new", "event", "book", "to", "in", "on", "the", "my", "den", "Team", "team", "tea", "te", "trips", "calendar", "Kalender",
            "Team-Kalender", "tomorrow", "7pm", "at", "@", "Cafe", "Luna", ",", "dinner", "Sam", "fri", "9-10", "🎉", "it", "put", "every", "Monday", "all", "day", "x")
        val random = java.util.Random(5)
        repeat(20_000) {
            val text = List(random.nextInt(10)) { bits[random.nextInt(bits.size)] }.joinToString(if (random.nextBoolean()) " " else "  ")
            val got = Sentence.read(text, now, mine)
            Sentence.reads(got)
            // Every word of the title and of the place was typed.
            for (w in (got.draft.title + " " + got.draft.place).split(' ').filter { it.isNotEmpty() }) assertTrue("$text: $w", w in text)
            // What is left of a calendar's name is the end of that calendar's name.
            got.rest?.let { assertTrue(text, got.calendar!!.name.endsWith(it)) }
            Sentence.put(text, "Team", "to", now, mine)
        }
    }
}
```

- [ ] **Step 2: Run them and see them fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SentenceTest' --console=plain
```

Expected: `BUILD FAILED` in `:core:compileTestKotlin`: `Unresolved reference 'Cal'` and `Unresolved reference 'Sentence'`.

- [ ] **Step 3: An expression that is far more often something else, in `core/src/main/kotlin/io/github/kuscher/booklight/core/When.kt`.** One change: `slight`, above `leads`.

Find:

```kotlin
    }

    /** True for a word that leads into a day or a time and means nothing without it ("on", "the", "at", „am“). */
```

Make it:

```kotlin
    }

    /** True for an expression that is far more often something else than a day or a time: a weekday's short form alone ("sun"), two bare numbers with a dash ("2-3"). */
    internal fun slight(said: String): Boolean = Days.slight(said) || BARE.matches(said.lowercase())

    /** True for a word that leads into a day or a time and means nothing without it ("on", "the", "at", „am“). */
```

- [ ] **Step 4: Write `core/src/main/kotlin/io/github/kuscher/booklight/core/Sentence.kt`**

```kotlin
package io.github.kuscher.booklight.core

import java.time.LocalDateTime

/**
 * One of the user's calendars, as the system lists it: its [name] and [color], the [id] the system has
 * for it on this device, and [owner], the address a calendar link names it by. [primary]: the account's
 * own calendar. [writable]: events can be added to it. Nothing of what is in it: Booklight never reads
 * an event.
 */
data class Cal(val id: Long, val name: String, val color: Int = 0, val owner: String = "", val primary: Boolean = false, val writable: Boolean = true)

/**
 * What a sentence says about an event, read by rules ([Sentence.read]): the event ([draft]), the
 * calendar it names ([calendar], one of the user's), and how sure the reading is.
 *
 * [rest]: only the start of that calendar's name is typed, at the very end of the sentence, and this is
 * what is left of it: the field shows it in grey. [named]: the sentence names a calendar by the word
 * ("… to the Team calendar") that is none of the user's, or their calendars are not known: the name
 * stays in the title. [cue]: the sentence begins with a word that says it is an event ("Add …"). [loose]:
 * words stand on more than one side of the day and time, so the title is put together from pieces, and
 * one of them may be a place: the model on the device may split it better. [said]: the day and time as
 * they were typed, empty where none was read.
 */
data class EventReading(
    val draft: EventDraft, val calendar: Cal? = null, val rest: String? = null, val named: Boolean = false,
    val cue: Boolean = false, val loose: Boolean = false, val said: String = "",
)

/**
 * An event the way it is said: "Add dinner with Sam tomorrow at 7pm to the Team calendar". Read by
 * rules, at every letter, in English and German:
 *
 * 1. A word at the start that says "this is an event" is the cue. "Add", "schedule", "put", "new event"
 *    („trag … ein“, „neuer Termin“) are dropped; "book" and "plan" („plane“) stay in the title, because
 *    "Book dentist" and "Plan review" are what such events are called. „… eintragen“ at the end is dropped.
 * 2. What follows the last " @ " is the place, as under the keyword.
 * 3. The day and time are found wherever they stand ([When.spot]).
 * 4. A calendar is found by its name: the words after "to", "in" or "on" („in den“, „im“) at the end of
 *    a piece of the sentence, with or without "calendar" or „-Kalender“, matched against the user's
 *    calendars without regard to case, to "the" and "my", and to a plural s. At the very end of the
 *    sentence the start of a name is enough, and so is a name with the word "calendar" still being typed.
 * 5. What is left is the title.
 *
 * Nothing here fails, and nothing is reckoned by anyone but [When].
 */
object Sentence {
    /** A sentence that begins with one of these is an event; the word is dropped. Nobody calls an event "Add dinner". */
    private val DROPPED = setOf("add", "schedule", "put", "trag", "trage")
    /** These begin an event's sentence too, and stay in its title. */
    private val KEPT = setOf("book", "plan", "plane")
    private val TWO = setOf("new event", "neuer termin", "neuen termin")
    /** „Trag … ein“: the verb's other half stands at the end. */
    private val SPLIT = setOf("trag", "trage")
    /** A calendar's name follows one of these, and may have one of [THE] before it. */
    private val INTO = setOf("to", "in", "into", "on", "onto", "im", "ins", "zum", "zu")
    private val THE = setOf("the", "my", "our", "den", "dem", "der", "die", "das", "mein", "meine", "meinen", "meinem", "unser", "unseren", "unserem")
    /** "…, add to the Team calendar": a second verb before the calendar goes with it. */
    private val ADDS = setOf("add", "put", "trag", "trage")
    /** The most words a calendar's name has. */
    private const val NAME = 5
    /** The word that may follow a calendar's name, folded. */
    private val WORDS = listOf("calendar", "kalender")

    /** What [text] says, read by the rules above. [calendars]: the user's, where they are known. */
    fun read(text: String, now: LocalDateTime, calendars: List<Cal> = emptyList()): EventReading = parsed(text, now, calendars).reading

    /**
     * Whether a line typed without the keyword is offered as an event: it begins with a cue, a day or
     * a time was read, and something is left to call it. Not for an expression that is far more often
     * something else: a weekday's short form alone ("add pictures of the sun"), two bare numbers with a
     * dash ("add 2-3 eggs").
     */
    fun reads(r: EventReading): Boolean = r.cue && r.draft.title.isNotBlank() && (r.draft.dayGiven || r.draft.timeGiven) && !When.slight(r.said)

    /**
     * [text] with the calendar [name] in it: in place of the calendar it names now (or of the letters a
     * name began with), else after the sentence's own words, led in by [word] ("to", „in“). What a
     * calendar chosen from the row's list writes into the field.
     */
    fun put(text: String, name: String, word: String, now: LocalDateTime, calendars: List<Cal>): String {
        val p = parsed(text, now, calendars)
        if (p.name != null) return p.text.substring(0, p.name.first) + name + p.text.substring(p.name.last + 1)
        return (p.text.substring(0, p.end).trimEnd() + " $word $name").trimStart() + p.text.substring(p.end)
    }

    /** [title] without a cue that is dropped at its start: the model on the device sometimes leaves the verb in. */
    internal fun uncued(title: String): String {
        val words = Words.of(title)
        val f = words.map { Matcher.fold(it.text) }
        val from = when {
            words.size >= 2 && "${f[0]} ${f[1]}" in TWO -> 2
            words.isNotEmpty() && f[0] in DROPPED -> 1
            else -> 0
        }
        return Words.cut(title, words, from, words.size)
    }

    /** A calendar's name as a sentence has it, folded, without the word "calendar" or „-Kalender“; and whether that word stood there. */
    internal fun bare(name: String): Pair<String, Boolean> {
        val f = Matcher.fold(name)
        return when {
            f.endsWith(" calendar") -> f.removeSuffix(" calendar") to true
            f.endsWith(" kalender") -> f.removeSuffix(" kalender") to true
            f.endsWith("kalender") && f.length > "kalender".length -> f.removeSuffix("kalender") to true
            else -> f to false
        }
    }

    /** The reading, the trimmed text it was read from, where a calendar's name stands in that text, and where the sentence's own words end (before a place, before „ein“). */
    private class Parsed(val reading: EventReading, val text: String, val name: IntRange?, val end: Int)

    /** A calendar found at the end of a piece: where its phrase and its name start in the piece. */
    private class Found(val cal: Cal?, val rest: String?, val phrase: Int, val name: Int)

    private fun parsed(input: String, now: LocalDateTime, calendars: List<Cal>): Parsed {
        val t = input.trim()
        val words = Words.of(t)
        val f = words.map { Matcher.fold(it.text) }
        // 1. The cue, and the German verb's other half.
        var from = 0
        var to = words.size
        var cue = false
        when {
            words.size >= 2 && "${f[0]} ${f[1]}" in TWO -> { from = 2; cue = true }
            words.isNotEmpty() && f[0] in DROPPED -> { from = 1; cue = true }
            words.isNotEmpty() && f[0] in KEPT -> cue = true
        }
        if (to > from && (f[to - 1] == "eintragen" || (f[to - 1] == "ein" && f[0] in SPLIT))) to--
        val start = if (from < to) words[from].start else t.length
        // 2. The place.
        val (main, place) = Jot.placed(if (from < to) t.substring(start, words[to - 1].end) else "")
        val end = start + main.length
        // 3. The day and time, and the sentence's pieces around them.
        val spot = When.spot(main, now)
        val pieces = (spot?.pieces ?: listOfNotNull(main.takeIf { it.isNotEmpty() }?.let { Piece(it, 0) })).toMutableList()
        // 4. The calendar: at the end of a piece, the last piece first. Only the sentence's very end may hold the start of a name.
        var calendar: Cal? = null
        var rest: String? = null
        var named = false
        var name: IntRange? = null
        var known = -1
        for (i in pieces.indices.reversed()) {
            val p = pieces[i]
            val last = i == pieces.lastIndex && p.start + p.text.length == main.length && end == t.length
            val found = phrase(p.text, calendars, last) ?: continue
            if (found.cal == null) {
                // Named by the word, and none of the user's: it stays where it is. (The first such from the end counts.)
                if (!named) { named = true; name = start + p.start + found.name until start + p.start + p.text.length; if (found.phrase == 0) known = i }
                continue
            }
            calendar = found.cal; rest = found.rest; named = false; known = -1
            name = start + p.start + found.name until start + p.start + p.text.length
            pieces[i] = Piece(p.text.substring(0, found.phrase).trimEnd().trimEnd(',', ';'), p.start)
            break
        }
        // 5. The title: what is left, in its order. Pieces on more than one side of the day and time make a loose one.
        val left = pieces.withIndex().filter { it.value.text.isNotEmpty() }
        val title = left.joinToString(" ") { it.value.text }
        val loose = left.count { it.index != known } > 1
        return Parsed(EventReading(Jot.draft(spot?.moment, main, place, now, title), calendar, rest, named, cue, loose, spot?.said.orEmpty()), t, name, end)
    }

    /**
     * The calendar that [piece] ends with: "… to the Team calendar", "… in Team", „… in den Team-Kalender“,
     * with "add" or "put" before it. A calendar of [calendars] by its whole name; with [last], also by the
     * start of its name. Named by the word "calendar" and none of them: found, with no calendar. Else null.
     */
    private fun phrase(piece: String, calendars: List<Cal>, last: Boolean): Found? {
        val w = Words.of(piece)
        val g = w.map { Matcher.fold(it.text) }
        var unknown: Found? = null
        for (j in w.lastIndex - 1 downTo maxOf(0, w.size - NAME - 3)) {
            if (g[j] !in INTO) continue
            var k = j + 1
            while (k < w.lastIndex && g[k] in THE) k++
            if (w.size - k > NAME) continue
            val typed = piece.substring(w[k].start)
            val (folded, byWord) = bare(typed)
            if (folded.isEmpty()) continue
            // ("…, add to", "put it in": the verb goes with the calendar.)
            val verb = if (j > 0 && g[j - 1] in ADDS) j - 1 else if (j > 1 && g[j - 1] == "it" && g[j - 2] in ADDS) j - 2 else j
            Cals.named(folded, calendars)?.let { return Found(it, null, w[verb].start, w[k].start) }
            if (byWord) { if (unknown == null) unknown = Found(null, null, w[verb].start, w[k].start); continue }
            if (!last) continue
            // At the very end of what is typed: the word "calendar" may be on its way after the name ("… to the Team cal"), and
            // the calendar must not come and go while it is typed. Else the start of a name is enough.
            WORDS.firstNotNullOfOrNull { word -> (1 until word.length).firstNotNullOfOrNull { n -> folded.takeIf { it.endsWith(word.take(n)) }?.dropLast(n)?.trimEnd()?.let { Cals.named(it, calendars) } } }
                ?.let { return Found(it, null, w[verb].start, w[k].start) }
            Cals.starting(typed, calendars)?.let { return Found(it, it.name.substring(typed.length), w[verb].start, w[k].start) }
        }
        return unknown
    }
}

/** The user's calendars: which of them a name means. */
object Cals {
    /**
     * The calendar called [folded] (a name as [Matcher.fold] leaves it): the same name, or the same but
     * for an s at the end of either ("Teams" for "Team", „Arbeitskalender“ for „Arbeit“). The first of [all].
     */
    fun named(folded: String, all: List<Cal>): Cal? {
        if (folded.isEmpty()) return null
        val names = all.map { Matcher.fold(it.name) }
        val exact = names.indexOf(folded)
        if (exact >= 0) return all[exact]
        return all.getOrNull(names.indexOfFirst { it.isNotEmpty() && (it + "s" == folded || folded + "s" == it) })
    }

    /** The calendar whose name starts with [typed] and goes on, letter for letter but for their case: the first of [all]. Two letters at least. */
    fun starting(typed: String, all: List<Cal>): Cal? =
        if (typed.length < START) null else all.firstOrNull { it.name.length > typed.length && it.name.startsWith(typed, ignoreCase = true) }

    /** The fewest letters that are taken for the start of a calendar's name. */
    private const val START = 2
}
```

- [ ] **Step 5: Run them and see them pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SentenceTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Run every core test, and build the app**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`.

- [ ] **Step 7: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/Sentence.kt core/src/main/kotlin/io/github/kuscher/booklight/core/When.kt core/src/test/kotlin/io/github/kuscher/booklight/core/SentenceTest.kt
git commit -m "An event from a sentence: a sentence is read by rules: the cue, the place, the day and time wherever they stand, a calendar by its name, and what is left as the title (core, tested)"
```

### Task 4: The model's answer, taken apart and merged

**Files:**
- Create: `core/src/main/kotlin/io/github/kuscher/booklight/core/Split.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/SplitTest.kt`

**Interfaces:**
- Consumes: `EventReading`, `Cal`, `Sentence.read`, `Sentence.uncued`, `Sentence.bare`, `Cals.named` (Task 3); `When.parse`, `When.leads` (Tasks 1 and 2); `Jot.draft` (Task 2); `Words`; core's own `internal object Json { fun parse(text: String): Any? }` (in `AirLabs.kt`: it throws on anything that is not JSON).
- Produces, in the new `core/…/Split.kt`:
  - `data class Split(val title: String, val said: String, val place: String, val calendar: String)`.
  - `object Splits`: `const val MAX = 200`; `fun asks(r: EventReading, text: String): Boolean`; `fun prompt(text: String): String`; `fun parse(answer: String): Split?`; `fun merge(own: EventReading, answer: String, text: String, now: LocalDateTime, calendars: List<Cal>): EventReading`, which returns `own` itself (the same object) wherever the answer is not taken.

The model on the device is good at one thing the rules are not: saying which words of a sentence are the place. It is asked for exactly that, and for nothing it could get wrong unseen. `prompt` is the instruction that was tried on a device, word for word, with the sentence after it; it asks for words, never for a date.

**When it is asked at all** (`asks`): only where the rules left the title in pieces (`loose`), and never about more than 200 characters. Of the sixteen sentences that is six.

**Its answer** (`parse`) comes inside a code fence, as one line of JSON. Whatever stands around the braces is not it. Two of the sixteen answers had three quotes where the last, empty value has two: that is mended (three quotes or more before the closing brace become two), and anything else that is not such an object, with a title and a "when" that are texts, is no answer.

**What is taken of it** (`merge`) is decided by checks, every one of which must pass; where one fails the rules' reading comes back as it is:
- every part is a piece of the typed text: its words stand there side by side, in that order, compared without their case and the marks at their ends. **Each part is then cut from the text as it was typed**: the model's own letters are never shown;
- the day and time are read by `When.parse` from the text's own words where the model's "when" stands, with a word before them that leads into a day ("on", "the", „am“: the model left "the" out of "the 12th"), all of them, and they say no less than the rules had read;
- a calendar it names is one of the user's, and no other than the one the rules found, if they found one;
- **no word is lost**: a word of the text that is in none of the parts must be one that only joins them ("to", "the", "add", "calendar"). One of the sixteen answers passed everything else and had dropped "for Sam" from the title.

Then the model's four faults of the sixteen are each handled by a rule, not by trust: a place that is the calendar's name is no place; a place taken out of the title is no place; a cue left in the title („Trag Mittagessen …“) is dropped by `Sentence.uncued`; and a place typed after an @ stays the place whatever the model says.

The test's fixtures are the model's sixteen answers as they came. With them every one of the sixteen sentences has its title, its place and its calendar: fifteen answers are taken, and the one that lost two words is dropped, where the rules were right anyway.

- [ ] **Step 1: Write the failing tests `core/src/test/kotlin/io/github/kuscher/booklight/core/SplitTest.kt`**

```kotlin
package io.github.kuscher.booklight.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The model on the device as a splitter ([Splits]): its sixteen answers as they came on a device
 * (docs/research/event-sentence.md), the people in them called Sam and the calendars Team, and what is
 * taken of each. Every answer came inside a code fence, on one line.
 */
class SplitTest {
    private val now = LocalDateTime.of(2026, 10, 5, 16, 20)
    private fun at(day: Int, hour: Int = 0, minute: Int = 0, month: Int = 10) = LocalDateTime.of(2026, month, day, hour, minute)

    private val sam = Cal(1, "Sam", 0xFF3F51B5.toInt(), "sam@example.com", primary = true)
    private val team = Cal(2, "Team", 0xFF0B8043.toInt(), "team@group.calendar.google.com")
    private val mine = listOf(sam, team)

    /** The code fence every answer came in: three backticks, and "json" after the first three. */
    private val fence = "`".repeat(3)
    private fun fenced(json: String) = "${fence}json\n$json\n$fence"
    private fun json(title: String, said: String, place: String = "", calendar: String = "") =
        fenced("""{"title":"$title","when":"$said","place":"$place","calendar":"$calendar"}""")
    /** The broken answer, as two of the sixteen came: three quotes where the last, empty value has two. */
    private fun broken(title: String, said: String, place: String = "") = "{\"title\":\"$title\",\"when\":\"$said\",\"place\":\"$place\",\"calendar\":\"\"\"}"

    /** One sentence, the model's answer to it, and what the row shows once the answer is in: title, place, calendar. [taken]: the answer passed every check. */
    private class Case(val text: String, val answer: String, val title: String, val place: String = "", val calendar: Cal? = null, val taken: Boolean = true)

    private val cases = listOf(
        Case("Add dinner with Sam tomorrow at 7pm to Team calendar", json("dinner with Sam", "tomorrow at 7pm", calendar = "Team"), "dinner with Sam", calendar = team),
        Case("Schedule dentist appointment next Tuesday at 3:30pm at Dr. Sam office", json("dentist appointment", "next Tuesday at 3:30pm", "Dr. Sam office"), "dentist appointment", "Dr. Sam office"),
        Case("Lunch with Sam on Friday at noon at Cafe Luna, add to Team calendar", json("Lunch with Sam", "Friday at noon", "Cafe Luna", "Team"), "Lunch with Sam", "Cafe Luna", team),
        Case("Team offsite Oct 14 to Oct 16 in Lisbon", json("Team offsite", "Oct 14 to Oct 16", "Lisbon"), "Team offsite", "Lisbon"),
        Case("Call mom Sunday 10am", json("Call mom", "Sunday 10am"), "Call mom"),
        Case("Put yoga class every Monday 6pm in my Team calendar", json("yoga class", "every Monday 6pm", calendar = "Team"), "yoga class", calendar = team),
        // Its fault: a place taken out of the title. No place.
        Case("Flight to Frankfurt on the 12th at 6:45 in the morning", json("Flight to Frankfurt", "12th at 6:45 in the morning", "Frankfurt"), "Flight to Frankfurt"),
        Case("Add Team bowling night to the Team calendar Saturday 8pm", json("Team bowling night", "Saturday 8pm", calendar = "Team"), "Team bowling night", calendar = team),
        Case("meeting with the team about Q3 plan tmrw 9-10am room 4B", json("meeting with the team about Q3 plan", "tmrw 9-10am", "room 4B"), "meeting with the team about Q3 plan", "room 4B"),
        // A broken answer (three quotes at its end), mended; and its fault: two words of the title dropped. A word lost: the rules' reading stands.
        Case("Birthday party for Sam on November 3rd all day", fenced(broken("Birthday party", "November 3rd all day")), "Birthday party for Sam", taken = false),
        // Its fault: the calendar given as the place too. No place.
        Case("Abendessen mit Sam morgen um 19 Uhr in den Team-Kalender eintragen", json("Abendessen mit Sam", "morgen um 19 Uhr", "Team-Kalender", "Team-Kalender"), "Abendessen mit Sam", calendar = team),
        Case("Zahnarzt nächsten Dienstag 15:30 Uhr", json("Zahnarzt", "nächsten Dienstag 15:30 Uhr"), "Zahnarzt"),
        // Its fault: the verb left in the title. Dropped by the rules' own rule.
        Case("Trag Mittagessen mit Sam am Freitag um 12 im Café Luna in den Teamkalender ein", json("Trag Mittagessen mit Sam", "Freitag um 12", "Café Luna", "Teamkalender"), "Mittagessen mit Sam", "Café Luna", team),
        // The other broken answer, mended. Its "when" lost a dot: the day and time are read from the text's own words.
        Case("Elternabend am 14.10. um 19:30 in der Schule", broken("Elternabend", "14.10 um 19:30", "in der Schule"), "Elternabend", "Schule"),
        Case("Übermorgen 8 Uhr Friseur", json("Friseur", "Übermorgen 8 Uhr"), "Friseur"),
        Case("Geburtstag von Oma am Samstag ganztägig", json("Geburtstag von Oma", "Samstag ganztägig"), "Geburtstag von Oma"),
    )

    @Test fun theSixteenAnswers() {
        assertEquals(16, cases.size)
        for (c in cases) {
            val own = Sentence.read(c.text, now, mine)
            val got = Splits.merge(own, c.answer, c.text, now, mine)
            assertEquals(c.text, c.title, got.draft.title)
            assertEquals(c.text, c.place, got.draft.place)
            assertEquals(c.text, c.calendar, got.calendar)
            // The day and time are the parser's, whoever split the sentence: never another moment than the rules read.
            assertEquals(c.text, own.draft.start, got.draft.start)
            assertEquals(c.text, own.draft.end, got.draft.end)
            assertEquals(c.text, own.draft.allDay, got.draft.allDay)
            assertEquals(c.text, own.draft.sure, got.draft.sure)
            assertEquals(c.text, own.cue, got.cue)
            if (c.taken) assertFalse(c.text, got.loose) else assertSame(c.text, own, got)
        }
        // With the model's split every one of the sixteen has its title, its place and its calendar; one of them by the rules alone.
        assertEquals(15, cases.count { it.taken })
    }

    @Test fun theModelIsAskedOnlyWhereWordsAreLeftOver() {
        val asked = cases.filter { Splits.asks(Sentence.read(it.text, now, mine), it.text) }.map { cases.indexOf(it) + 1 }
        assertEquals(listOf(2, 3, 4, 9, 13, 14), asked)
        // Never for a line without a day or a time, and never for a very long one.
        assertFalse(Splits.asks(Sentence.read("Add dinner with Sam at Cafe Luna", now, mine), "Add dinner with Sam at Cafe Luna"))
        val long = "Schedule dentist appointment next Tuesday at 3:30pm at " + "Hinterwaldkirchenstrasse ".repeat(8)
        assertTrue(Sentence.read(long, now, mine).loose)
        assertFalse(Splits.asks(Sentence.read(long, now, mine), long))
    }

    @Test fun theQuestionIsTheOneThatWasTried() {
        val q = Splits.prompt("  Add dinner with Sam tomorrow at 7pm  ")
        assertTrue(q.startsWith("Split the request into its parts. Answer with one line of JSON and nothing else:\n{\"title\":\"\",\"when\":\"\",\"place\":\"\",\"calendar\":\"\"}\nRules: copy the words exactly from the request."))
        assertTrue(q.endsWith("\nRequest: Add dinner with Sam tomorrow at 7pm"))
        // It asks for words, never for a date.
        assertFalse(q.contains("2026"))
    }

    @Test fun anAnswerIsTakenApart() {
        assertEquals(Split("dinner", "tomorrow at 7pm", "Cafe Luna", "Team"), Splits.parse(json("dinner", "tomorrow at 7pm", "Cafe Luna", "Team")))
        // Without the fence, with words around it, with spaces in it.
        assertEquals(Split("dinner", "7pm", "", ""), Splits.parse("""Here you go: { "title" : " dinner ", "when" : "7pm", "place" : "", "calendar" : "" } Anything else?"""))
        // A part that is missing or null is an empty one; the title and the "when" must be there.
        assertEquals(Split("dinner", "7pm", "", ""), Splits.parse("""{"title":"dinner","when":"7pm","place":null}"""))
        assertNull(Splits.parse("""{"title":"dinner","place":"","calendar":""}"""))
        assertNull(Splits.parse("""{"when":"7pm"}"""))
        // The broken last value, mended: three quotes or more before the closing brace.
        assertEquals(Split("Elternabend", "14.10 um 19:30", "in der Schule", ""), Splits.parse(broken("Elternabend", "14.10 um 19:30", "in der Schule")))
        assertEquals(Split("a", "b", "", ""), Splits.parse(fenced(broken("a", "b").dropLast(1) + "\" }")))
    }

    @Test fun whatIsNoAnswerIsDropped() {
        for (bad in listOf("", "   ", "I cannot help with that.", fenced(""), "{", "}{", "{}", "[]", """["title","when"]""", """{"title":"dinner","when":"7pm""", """{"title":dinner,"when":"7pm"}""",
            """{"title":"dinner","when":7}""", """{"title":{"a":1},"when":"7pm"}""", """{"title":"dinner","when":"7pm","place":["x"]}""", """{"title":"dinner" "when":"7pm"}""", "{".repeat(100) + "}".repeat(100)))
            assertNull(bad, Splits.parse(bad))
    }

    @Test fun nothingTheModelSaysIsShownUnlessItWasTyped() {
        val text = "Schedule dentist appointment next Tuesday at 3:30pm at Dr. Sam office"
        val own = Sentence.read(text, now, mine)
        fun kept(answer: String) = assertSame(answer, own, Splits.merge(own, answer, text, now, mine))
        // A word that was not typed, in any part.
        kept(json("dental appointment", "next Tuesday at 3:30pm", "Dr. Sam office"))
        kept(json("dentist appointment", "next Tuesday at 3:30pm", "Dr. Sam's practice"))
        kept(json("dentist appointment", "Tuesday 6 October at 3:30pm", "Dr. Sam office"))
        kept(json("dentist appointment", "next Tuesday at 3:30pm", "Dr. Sam office", "Sam's"))
        // Typed words, but not side by side or not in that order.
        kept(json("appointment dentist", "next Tuesday at 3:30pm", "Dr. Sam office"))
        kept(json("dentist office", "next Tuesday at 3:30pm"))
        // No title, no "when", or a "when" that is no day and time.
        kept(json("", "next Tuesday at 3:30pm", "Dr. Sam office"))
        kept(json("dentist appointment", "", "Dr. Sam office"))
        kept(json("dentist", "appointment next Tuesday at 3:30pm", "Dr. Sam office"))
        // A "when" that says less than the rules read: the time left in the title.
        kept(json("dentist appointment at 3:30pm", "next Tuesday", "Dr. Sam office"))
        // A word lost: "appointment" is in no part.
        kept(json("dentist", "next Tuesday at 3:30pm", "Dr. Sam office"))
        // A calendar that is none of the user's, though the word was typed.
        kept(json("dentist appointment", "next Tuesday at 3:30pm", "office", "Dr. Sam"))
        // No answer at all.
        kept("")
        kept(fenced("{\"title\":\"dentist appointment\""))
    }

    @Test fun whatTheModelSplitsIsTakenFromTheTextAsItWasTyped() {
        val text = "Schedule DENTIST appointment next Tuesday at 3:30pm at Dr. Sam Office, upstairs"
        val got = Splits.merge(Sentence.read(text, now, mine), json("dentist appointment", "next tuesday at 3:30PM", "dr. sam office, upstairs"), text, now, mine)
        assertEquals("DENTIST appointment", got.draft.title)
        assertEquals("Dr. Sam Office, upstairs", got.draft.place)
        assertEquals("next Tuesday at 3:30pm", got.said)
        assertEquals(at(6, 15, 30), got.draft.start)
    }

    @Test fun theRulesOwnCalendarAndPlaceStand() {
        // The rules found the calendar; an answer that names none, or another, is not taken.
        val text = "Lunch with Sam on Friday at noon at Cafe Luna, add to Team calendar"
        val own = Sentence.read(text, now, mine)
        assertSame(own, Splits.merge(own, json("Lunch with Sam", "Friday at noon", "Cafe Luna"), text, now, mine))
        assertSame(own, Splits.merge(own, json("Lunch", "Friday at noon", "Cafe Luna", "Sam"), text, now, mine))
        // A place typed after an @ is the place, whatever the model makes of it.
        val at = "Add dinner tomorrow at 7pm with Sam @ Cafe Luna"
        val typed = Sentence.read(at, now, mine)
        assertTrue(typed.loose)
        assertEquals("Cafe Luna", Splits.merge(typed, json("dinner with Sam", "tomorrow at 7pm", "Luna"), at, now, mine).draft.place)
    }

    @Test fun withoutTheCalendarsAnAnswerThatNamesOneIsNotTaken() {
        // Not allowed: no calendar can be matched, and the name stays in the title, as the rules' own reading has it.
        val text = "Lunch with Sam on Friday at noon at Cafe Luna, add to Team calendar"
        val own = Sentence.read(text, now, emptyList())
        assertSame(own, Splits.merge(own, json("Lunch with Sam", "Friday at noon", "Cafe Luna", "Team"), text, now, emptyList()))
        assertEquals("Lunch with Sam at Cafe Luna, add to Team calendar", own.draft.title)
        // One that names none is taken as on any day.
        val plain = "Schedule dentist appointment next Tuesday at 3:30pm at Dr. Sam office"
        assertEquals("Dr. Sam office", Splits.merge(Sentence.read(plain, now, emptyList()), json("dentist appointment", "next Tuesday at 3:30pm", "Dr. Sam office"), plain, now, emptyList()).draft.place)
    }

    @Test fun nothingThrows() {
        val bits = listOf("{", "}", "\"", ":", ",", "title", "when", "place", "calendar", "\"title\":\"dinner\"", "\"when\":\"tomorrow\"", "\"place\":\"\"", "null", fence, "json", "\n", "\\", "\\u12", "[", "]", "7", " ")
        val random = java.util.Random(3)
        val text = "Add dinner with Sam tomorrow at 7pm at Cafe Luna"
        val own = Sentence.read(text, now, mine)
        repeat(20_000) {
            val answer = List(random.nextInt(14)) { bits[random.nextInt(bits.size)] }.joinToString("")
            Splits.parse(answer)
            Splits.merge(own, answer, text, now, mine)
        }
    }
}
```

- [ ] **Step 2: Run them and see them fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SplitTest' --console=plain
```

Expected: `BUILD FAILED` in `:core:compileTestKotlin`: `Unresolved reference 'Splits'` and `Unresolved reference 'Split'`.

- [ ] **Step 3: Write `core/src/main/kotlin/io/github/kuscher/booklight/core/Split.kt`**

```kotlin
package io.github.kuscher.booklight.core

import java.time.LocalDateTime

/** A sentence as the model on the device split it, each part as the model gave it: nothing here has been checked. */
data class Split(val title: String, val said: String, val place: String, val calendar: String)

/**
 * The model on the device as a splitter: it is asked which words of an event's sentence are the title,
 * the day and time, the place and the calendar, and nothing else. It never reckons a date (that is
 * [When]'s), it is asked only where the rules leave a title in pieces ([asks]), and what it says is
 * taken only where every check of [merge] passes. Whatever fails, the rules' own reading stands.
 */
object Splits {
    /** The longest sentence the model is asked about: a longer one is no event typed into a launcher. */
    const val MAX = 200

    /** What the model is asked, word for word as it was tried on a device (docs/research/event-sentence.md); the sentence follows. */
    private const val INSTRUCTION = "Split the request into its parts. Answer with one line of JSON and nothing else:\n" +
        "{\"title\":\"\",\"when\":\"\",\"place\":\"\",\"calendar\":\"\"}\n" +
        "Rules: copy the words exactly from the request. \"when\" is every word about the day and the time. \"calendar\" is only the name of the calendar. " +
        "\"title\" is what the event is, without add, schedule or put, and without the time, the place and the calendar. Use \"\" for a part that is not there.\n" +
        "Request: "

    /** Words that join a sentence's parts and belong to none: the model may leave them out of every part. Any other word it leaves out is a word lost. */
    private val GLUE = setOf("add", "schedule", "put", "new", "event", "trag", "trage", "ein", "eintragen", "neuer", "neuen", "termin",
        "to", "in", "into", "on", "onto", "at", "im", "ins", "zum", "zu", "bei", "the", "my", "our", "den", "dem", "der", "die", "das", "mein", "meine", "meinen", "meinem",
        "calendar", "kalender", "and", "und", "please", "bitte", "it")
    /** Words that lead into a place and are not its name: „in der Schule“ is „Schule“. */
    private val AT = setOf("at", "in", "im", "bei", "auf", "an")
    private val ARTICLE = setOf("der", "dem", "den")

    /** Whether the model is asked about [text] at all: only where the rules left the title in pieces. */
    fun asks(r: EventReading, text: String): Boolean = r.loose && text.trim().length <= MAX

    /** The question for [text]. */
    fun prompt(text: String): String = INSTRUCTION + text.trim()

    /**
     * The model's answer, taken apart: one JSON object with a title and a "when", whatever stands
     * around it (it comes in a code fence). An answer whose last value has a quote too many is mended,
     * as two of sixteen had; anything else that is not such an object is no answer.
     */
    fun parse(answer: String): Split? {
        val from = answer.indexOf('{')
        val to = answer.lastIndexOf('}')
        if (from < 0 || to <= from) return null
        val line = answer.substring(from, to + 1)
        val map = read(line) ?: read(MENDS.replace(line, "\"\"}")) ?: return null
        fun part(key: String): String? = when (val v = map[key]) { is String -> v.trim(); null -> ""; else -> null }
        if (map["title"] !is String || map["when"] !is String) return null
        return Split(part("title") ?: return null, part("when") ?: return null, part("place") ?: return null, part("calendar") ?: return null)
    }

    private val MENDS = Regex("\"{3,}\\s*\\}$")
    private fun read(line: String): Map<*, *>? = try { Json.parse(line) as? Map<*, *> } catch (_: Exception) { null }

    /**
     * [own], the rules' reading of [text], with the model's [answer] where it helps. The answer is
     * taken only if all of this holds; else [own] comes back as it is:
     * - it is an answer ([parse]), with a title and a "when";
     * - every part is a piece of the text: its words stand there, side by side, in that order. Each part
     *   is then taken from the text as it was typed, never from the answer;
     * - the day and time are read by [When] from the text's own words where the model's "when" stands
     *   (with a word before them that leads into a day: "on", "the", „am“), all of them, and they say no
     *   less than the rules had read;
     * - a calendar it names is one of [calendars], and it names the one the rules found, if they found one;
     * - no word of the text is lost: a word in none of the parts must be one that only joins them ("to", "the", "add").
     * A place that is the calendar's name, or that is taken from the title, is no place. A cue the model
     * left in the title is dropped by the rules' own rule. A place typed after an @ stays the place.
     */
    fun merge(own: EventReading, answer: String, text: String, now: LocalDateTime, calendars: List<Cal>): EventReading {
        val s = parse(answer) ?: return own
        if (s.title.isEmpty() || s.said.isEmpty()) return own
        val words = Words.of(text)
        fun key(w: String) = w.lowercase().trim { !it.isLetterOrDigit() }
        // The words that are words (not a comma or an @ alone), and where each stands among all of them.
        val at = words.indices.filter { key(words[it].text).isNotEmpty() }
        val keys = at.map { key(words[it].text) }
        val taken = HashSet<Int>()
        /** Where [part]'s words stand in the text, as places among [keys]: free of what is [taken] where it can be, else anywhere. */
        fun run(part: String): IntRange? {
            val p = Words.of(part).map { key(it.text) }.filter { it.isNotEmpty() }
            if (p.isEmpty() || p.size > keys.size) return null
            val all = (0..keys.size - p.size).filter { i -> p.indices.all { keys[i + it] == p[it] } }.map { it until it + p.size }
            return all.firstOrNull { r -> r.none { it in taken } } ?: all.firstOrNull()
        }
        fun typed(r: IntRange) = Words.cut(text, words, at[r.first], at[r.last] + 1).trimEnd(',', ';')
        val title = run(s.title) ?: return own
        taken += title
        var said = run(s.said) ?: return own
        while (said.first > 0 && said.first - 1 !in taken && When.leads(keys[said.first - 1])) said = said.first - 1..said.last
        taken += said
        val place = if (s.place.isEmpty()) null else run(s.place) ?: return own
        val calendar = if (s.calendar.isEmpty()) null else run(s.calendar) ?: return own
        // The day and time: the parser's, from the text's own words.
        val m = When.parse(typed(said), now)?.takeIf { it.rest.isBlank() } ?: return own
        if ((own.draft.dayGiven && !m.dayGiven) || (own.draft.timeGiven && !m.timeGiven)) return own
        // The calendar: one of the user's, and no other than the rules' own.
        val cal = calendar?.let { Cals.named(Sentence.bare(typed(it)).first, calendars) ?: return own }
        if (own.calendar != null && cal != own.calendar) return own
        // No word lost.
        val parts = listOfNotNull(title, said, place, calendar)
        if (keys.indices.any { i -> parts.none { i in it } && keys[i] !in GLUE }) return own
        val name = Sentence.uncued(typed(title))
        if (name.isBlank()) return own
        val where = when {
            own.draft.place.isNotEmpty() -> own.draft.place
            place == null -> ""
            // The calendar's name given as the place too, or a place taken out of the title: no place.
            calendar != null && (place == calendar || Sentence.bare(typed(place)).first == Sentence.bare(typed(calendar)).first) -> ""
            place.all { it in title } -> ""
            else -> unled(typed(place))
        }
        return EventReading(Jot.draft(m, "", where, now, name), cal, cue = own.cue, said = typed(said))
    }

    /** A place without the words that lead into it: "at Cafe Luna", „in der Schule“. */
    private fun unled(place: String): String {
        val words = Words.of(place)
        var from = 0
        if (words.size > 1 && words[0].text.lowercase() in AT) from = 1
        if (from == 1 && words.size > 2 && words[1].text.lowercase() in ARTICLE) from = 2
        return Words.cut(place, words, from, words.size)
    }
}
```

- [ ] **Step 4: Run them and see them pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SplitTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run every core test, and build the app**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`.

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/Split.kt core/src/test/kotlin/io/github/kuscher/booklight/core/SplitTest.kt
git commit -m "An event from a sentence: the model's answer is taken apart, checked word for word against what was typed, and merged with the rules' reading; the date is always the parser's (core, tested)"
```

### Task 5: What Enter sends, what it writes, and that a guess is never saved

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/Sentence.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/CalsTest.kt`

**Interfaces:**
- Consumes: `Cal`, `EventReading`, `object Cals` with `named` and `starting` (Task 3); `EventDraft` with `sure` (Task 2).
- Produces, in `core/…/Sentence.kt`:
  - `data class EventWrite(val calendar: Long, val title: String, val start: Long, val end: Long, val allDay: Boolean, val zone: String, val place: String)`.
  - In `object Cals`: `const val TEMPLATE = "https://calendar.google.com/calendar/render?action=TEMPLATE"`; `fun target(named: Cal?, all: List<Cal>, chosen: String): Cal?`; `fun saves(r: EventReading, on: Boolean, target: Cal?): Boolean`; `fun link(e: EventDraft, cal: Cal?, zone: ZoneId): String`; `fun write(e: EventDraft, cal: Cal, zone: ZoneId): EventWrite`.

What the device said (`docs/research/event-sentence.md`): the Calendar app ignores a calendar's id in the insert request, and **honours a calendar link that names the calendar** by `src=`. So Enter sends a link: `…/calendar/render?action=TEMPLATE&text=…&dates=…&location=…&src=…`. `link` builds it: a moment in UTC as `yyyyMMdd'T'HHmmss'Z'`, each end converted by itself from the device's zone; an all-day event as its first day and the day after its last, `yyyyMMdd/yyyyMMdd`, **never converted**: the days are the days that were typed, wherever the device is. Every value is escaped, so nothing typed can add a parameter of its own; a part that is empty is left out; with no calendar none is named, and the Calendar app chooses.

`write` is what a direct save writes: exactly what the row shows, as the calendar provider keeps it. An all-day event is midnight to midnight in UTC and says so in its zone; anything else is its two moments and the device's zone.

`target` says where a saved event goes: into the calendar the sentence named; with none named, into the one chosen under "New events go to" (kept by its owner's address), else the account's own, else the first that takes events. A calendar that takes none is never the one: named, it is no target at all.

`saves` is §11's rule in one line: **the switch on, a calendar to write to, a title, and a day and a time that were both read** (`EventDraft.sure`). Never a day that was guessed, a time that was guessed, or the next day of a repeat.

- [ ] **Step 1: Write the failing tests `core/src/test/kotlin/io/github/kuscher/booklight/core/CalsTest.kt`**

```kotlin
package io.github.kuscher.booklight.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What Enter on an event's row sends or writes: the calendar link, the values for a direct save, the calendar it goes to, and when it is saved at all. */
class CalsTest {
    private val now = LocalDateTime.of(2026, 10, 5, 16, 20)
    private val berlin = ZoneId.of("Europe/Berlin")

    private val sam = Cal(1, "Sam", 0xFF3F51B5.toInt(), "sam@example.com", primary = true)
    private val team = Cal(2, "Team", 0xFF0B8043.toInt(), "team@group.calendar.google.com")
    private val holidays = Cal(4, "Holidays", 0xFF7986CB.toInt(), "holidays@group.v.calendar.google.com", writable = false)
    private val mine = listOf(team, sam, holidays)

    private fun r(text: String) = Sentence.read(text, now, mine)

    @Test fun theLinkCarriesTheTitleTheTimeInUtcAndTheCalendar() {
        val got = r("Add dinner with Sam tomorrow at 7pm to Team calendar")
        // Seven in the evening in Berlin's summer time is five in UTC.
        assertEquals("https://calendar.google.com/calendar/render?action=TEMPLATE&text=dinner%20with%20Sam&dates=20261006T170000Z/20261006T180000Z&src=team%40group.calendar.google.com",
            Cals.link(got.draft, got.calendar, berlin))
        // No calendar named: none is sent, and the Calendar app chooses.
        assertEquals("https://calendar.google.com/calendar/render?action=TEMPLATE&text=Call%20mom&dates=20261011T080000Z/20261011T090000Z", Cals.link(r("Call mom Sunday 10am").draft, null, berlin))
        // A place.
        assertEquals("https://calendar.google.com/calendar/render?action=TEMPLATE&text=Dentist&dates=20261009T130000Z/20261009T140000Z&location=Main%20St%2012",
            Cals.link(r("Fri 3pm Dentist @ Main St 12").draft, null, berlin))
    }

    @Test fun anAllDayEventIsItsFirstDayAndTheDayAfterItsLast() {
        assertEquals("${Cals.TEMPLATE}&text=Birthday%20party%20for%20Sam&dates=20261103/20261104", Cals.link(r("Birthday party for Sam on November 3rd all day").draft, null, berlin))
        assertEquals("${Cals.TEMPLATE}&text=Team%20offsite&dates=20261014/20261017&location=Lisbon", Cals.link(r("Team offsite Oct 14 to Oct 16 @ Lisbon").draft, null, berlin))
        // The days are the days that were typed, wherever the device is: no zone moves an all-day event to the day before.
        for (zone in listOf("America/Los_Angeles", "Pacific/Auckland", "UTC", "Asia/Kolkata"))
            assertEquals(zone, "${Cals.TEMPLATE}&text=Birthday%20party%20for%20Sam&dates=20261103/20261104", Cals.link(r("Birthday party for Sam on November 3rd all day").draft, null, ZoneId.of(zone)))
    }

    @Test fun aMomentIsTheSameMomentInEveryZone() {
        val e = r("Call mom Sunday 10am").draft
        // Ten in the morning in Los Angeles is five in the afternoon in UTC; in Auckland it is nine in the evening of the day before.
        assertTrue(Cals.link(e, null, ZoneId.of("America/Los_Angeles")).endsWith("&dates=20261011T170000Z/20261011T180000Z"))
        assertTrue(Cals.link(e, null, ZoneId.of("Pacific/Auckland")).endsWith("&dates=20261010T210000Z/20261010T220000Z"))
        // Over the night the clocks go back (25 October 2026 in Berlin): each end is its own moment.
        val night = EventDraft("Night shift", LocalDateTime.of(2026, 10, 24, 22, 0), LocalDateTime.of(2026, 10, 25, 3, 30), false, "", dayGiven = true, timeGiven = true)
        assertTrue(Cals.link(night, null, berlin).endsWith("&dates=20261024T200000Z/20261025T023000Z"))
        // And what is written is those same moments.
        val w = Cals.write(night, sam, berlin)
        assertEquals(Instant.parse("2026-10-24T20:00:00Z").toEpochMilli(), w.start)
        assertEquals(Instant.parse("2026-10-25T02:30:00Z").toEpochMilli(), w.end)
    }

    @Test fun whatATitleHoldsIsWrittenIntoTheLinkSafely() {
        val e = EventDraft("Q&A: 50% off? #1 + “more” für Jürgen 🎉", now, now.plusHours(1), false, "A&B / C=D", dayGiven = true, timeGiven = true)
        val link = Cals.link(e, Cal(9, "x", owner = "a+b&c=d@example.com"), berlin)
        assertEquals("${Cals.TEMPLATE}&text=Q%26A%3A%2050%25%20off%3F%20%231%20%2B%20%E2%80%9Cmore%E2%80%9D%20f%C3%BCr%20J%C3%BCrgen%20%F0%9F%8E%89&dates=20261005T142000Z/20261005T152000Z" +
            "&location=A%26B%20%2F%20C%3DD&src=a%2Bb%26c%3Dd%40example.com", link)
        // Nothing typed can add a parameter of its own: after the fixed start there are exactly these four.
        assertEquals(listOf("text", "dates", "location", "src"), link.removePrefix(Cals.TEMPLATE).split('&').filter { it.isNotEmpty() }.map { it.substringBefore('=') })
        // No title, no place, no owner: those parameters are not there.
        assertEquals("${Cals.TEMPLATE}&dates=20261005T142000Z/20261005T152000Z", Cals.link(e.copy(title = " ", place = ""), Cal(9, "x"), berlin))
    }

    @Test fun whatIsWrittenIsWhatTheRowShows() {
        val got = r("Add dinner with Sam tomorrow at 7pm to Team calendar @ Cafe Luna")
        assertEquals(EventWrite(2, "dinner with Sam", Instant.parse("2026-10-06T17:00:00Z").toEpochMilli(), Instant.parse("2026-10-06T18:00:00Z").toEpochMilli(), false, "Europe/Berlin", "Cafe Luna"),
            Cals.write(got.draft, got.calendar!!, berlin))
        // All day: midnight to midnight in UTC, said so, whatever the device's zone.
        val day = r("Birthday party for Sam on November 3rd all day").draft
        for (zone in listOf(berlin, ZoneId.of("America/Los_Angeles"), ZoneId.of("Pacific/Auckland")))
            assertEquals(EventWrite(1, "Birthday party for Sam", Instant.parse("2026-11-03T00:00:00Z").toEpochMilli(), Instant.parse("2026-11-04T00:00:00Z").toEpochMilli(), true, "UTC", ""), Cals.write(day, sam, zone))
    }

    @Test fun whereANewEventGoes() {
        // The calendar that was named.
        assertEquals(team, Cals.target(team, mine, ""))
        assertEquals(team, Cals.target(team, mine, sam.owner))
        // None named: the one chosen in the window; until one is chosen, the account's own; without one, the first that can be written.
        assertEquals(team, Cals.target(null, mine, team.owner))
        assertEquals(sam, Cals.target(null, mine, ""))
        assertEquals(team, Cals.target(null, listOf(holidays, team), ""))
        // The chosen one is gone from this device (another device's settings came with a backup): the account's own again.
        assertEquals(sam, Cals.target(null, mine, "gone@group.calendar.google.com"))
        // A calendar that cannot be written is never the one: not when named, not when chosen, not as the only one.
        assertNull(Cals.target(holidays, mine, ""))
        assertEquals(sam, Cals.target(null, mine, holidays.owner))
        assertNull(Cals.target(null, listOf(holidays), ""))
        assertNull(Cals.target(null, emptyList(), ""))
    }

    @Test fun aGuessIsNeverSaved() {
        fun saves(text: String, on: Boolean = true, target: Cal? = sam) = Cals.saves(r(text), on, target)
        // Saved: a day and a time that were both typed; a day with "all day"; a range of days.
        for (t in listOf("Add dinner with Sam tomorrow at 7pm", "Call mom Sunday 10am", "Birthday party for Sam on November 3rd all day", "Team offsite Oct 14 to Oct 16", "Standup tomorrow from 9 to 10"))
            assertTrue(t, saves(t))
        // Never: the day is a guess (only a time was typed), the time is (only a day), both are, or the day is the next of a repeat.
        for (t in listOf("Call mom 10am", "Offsite tomorrow", "Dentist", "Put yoga class every Monday 6pm in my Team calendar", "Inventory all day"))
            assertFalse(t, saves(t))
        // Never without the switch, without a calendar to write to, into one that cannot be written, or with nothing to call it.
        assertFalse(saves("Call mom Sunday 10am", on = false))
        assertFalse(saves("Call mom Sunday 10am", target = null))
        assertFalse(saves("Call mom Sunday 10am", target = holidays))
        assertFalse(saves("Sunday 10am"))
        assertFalse(saves("Add tomorrow at 7pm"))
    }
}
```

- [ ] **Step 2: Run them and see them fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.CalsTest' --console=plain
```

Expected: `BUILD FAILED` in `:core:compileTestKotlin`: `Unresolved reference 'link'`, `'TEMPLATE'`, `'write'`, `'EventWrite'`, `'target'`, `'saves'`.

- [ ] **Step 3: The link, the values, the target and the rule, in `core/src/main/kotlin/io/github/kuscher/booklight/core/Sentence.kt`.** Two changes: four imports; and `EventWrite` with the head of `Cals`, above its `named`.

Find:

```kotlin
import java.time.LocalDateTime
```

Make it:

```kotlin
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
```

Find:

```kotlin
/** The user's calendars: which of them a name means. */
object Cals {
```

Make it:

```kotlin
/**
 * An event as it is written into a calendar: the calendar's [calendar] id on this device, and the
 * event's own values. [start] and [end] are epoch milliseconds; an all-day event runs from midnight to
 * midnight in UTC, whatever the device's zone, and says so in [zone]: that is how calendars keep one.
 */
data class EventWrite(val calendar: Long, val title: String, val start: Long, val end: Long, val allDay: Boolean, val zone: String, val place: String)

/** The user's calendars: which of them a name means, which one a new event goes to, and what is sent or written for an event. */
object Cals {
    /** The web calendar's own address for a new event, filled in: the Calendar app takes it, and honours the calendar it names. */
    const val TEMPLATE = "https://calendar.google.com/calendar/render?action=TEMPLATE"
    private val DAY = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val INSTANT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    /**
     * The calendar a new event is written into when it is saved from the panel: the one the sentence
     * [named]; with none named, the one the user chose ([chosen]: its owner's address, empty for none),
     * else the account's own, else the first that can be written. Null where the named one cannot be
     * written (a calendar that is only subscribed to), and where there is none to write to.
     */
    fun target(named: Cal?, all: List<Cal>, chosen: String): Cal? {
        if (named != null) return named.takeIf { it.writable }
        val open = all.filter { it.writable }
        return open.firstOrNull { chosen.isNotEmpty() && it.owner == chosen } ?: open.firstOrNull { it.primary } ?: open.firstOrNull()
    }

    /**
     * Whether Enter saves the event itself: only with the switch [on], a calendar to write to, a
     * title, and a day and a time that were both read from the sentence (or a day with "all day", or a
     * range of days). A guess is never saved: not a day that was guessed, not a time, not a repeat.
     */
    fun saves(r: EventReading, on: Boolean, target: Cal?): Boolean = on && target != null && target.writable && r.draft.sure && r.draft.title.isNotBlank()

    /**
     * The calendar link for [e], which opens the Calendar app's editor filled in: the title, the start
     * and end in UTC (a moment in [zone], the device's), or for an all-day event its first day and the
     * day after its last, the place, and [cal] as the calendar to put it on. With no calendar none is
     * named, and the Calendar app chooses.
     */
    fun link(e: EventDraft, cal: Cal?, zone: ZoneId): String {
        fun q(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
        val dates = if (e.allDay) "${DAY.format(e.start)}/${DAY.format(e.end)}"
            else listOf(e.start, e.end).joinToString("/") { INSTANT.format(it.atZone(zone).withZoneSameInstant(ZoneOffset.UTC)) }
        return buildString {
            append(TEMPLATE)
            if (e.title.isNotBlank()) append("&text=").append(q(e.title))
            append("&dates=").append(dates)
            if (e.place.isNotBlank()) append("&location=").append(q(e.place))
            if (cal != null && cal.owner.isNotBlank()) append("&src=").append(q(cal.owner))
        }
    }

    /** What is written for [e] into [cal], with the device's [zone]: exactly what the row shows, and nothing else. */
    fun write(e: EventDraft, cal: Cal, zone: ZoneId): EventWrite {
        val at = if (e.allDay) ZoneOffset.UTC else zone
        return EventWrite(cal.id, e.title, e.start.atZone(at).toInstant().toEpochMilli(), e.end.atZone(at).toInstant().toEpochMilli(), e.allDay, if (e.allDay) "UTC" else zone.id, e.place)
    }

```

- [ ] **Step 4: Run them and see them pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.CalsTest' --tests 'io.github.kuscher.booklight.core.SentenceTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run every core test, and build the app**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`.

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/Sentence.kt core/src/test/kotlin/io/github/kuscher/booklight/core/CalsTest.kt
git commit -m "An event from a sentence: the calendar link that is sent, the values that are written, the calendar a new event goes to, and that a guess is never saved (core, tested)"
```

### Task 6: The row's place in the list, its Calendar stop, and what a row can now say

**Files:**
- Modify: `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/Model.kt`
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/SearchEngine.kt`
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/Stops.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/IconTest.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/SearchEngineTest.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/StopsTest.kt`

**Interfaces:**
- Consumes: in `Model.kt`, `Icon.Swatch(argb)`, `enum class Behind { ARROW, WINDOW }`, `Effect.InsertEvent(title, startMillis, endMillis, allDay, place)`, `Body.Slots(caption, slots, note)`, `Slot(label, value, state)`; `Stops.of`, `Stops.list`, `Stops.at`; `SearchEngine.search` with its private `isFallback`, `isGuess`, `letter`, `ways`, `under`, `out`.
- Produces:
  - `Icon.Swatch.symbol: String` (`dot:` and the colour in hex) and `Icon.Swatch.of(symbol: String): Icon.Swatch?`.
  - `enum class Behind { ARROW, WINDOW, CALENDAR }`.
  - `Effect.InsertEvent` has one more field, last: `val link: String = ""`.
  - `Body.Slots` has four more fields, last: `val more: List<Slot> = emptyList()`, `val completes: String? = null`, `val footer: String? = null`, `val ask: String? = null`.
  - `Slot` has one more field, last: `val dot: Int? = null`.
  - `Stops.of`: a line of any stop's own list, armed while that list is closed, stands in that stop (it was Window's alone).
  - `SearchEngine.SENTENCE = "sentence:"`: a row whose id begins with it stands below every row of the device and above every row of the web that ranks as a match.
  - `DebugReceiver`'s dump says `armed=calendar` on that stop (one `when` there lists every `Behind`).

Three small things the row needs of core before the app can build it, each with its test.

**Its place.** "One ranked list": a sentence's row must stand below anything of the device that matches and above the web. No score can say that: a weak match of an app scores 0.1, and the question for Gemini, which ranks as a match for a line of five words, 0.79. So its place is not a score's. `SearchEngine.search` takes the row whose id begins with `SENTENCE` out of the ranking and sets it down after every row that is not the web's and before every row that is; and keeps a seat for it, as it does for the web's row, so that it is never crowded out. Without such a row the order is exactly as it was (`withoutASentenceTheOrderIsAsItWas`).

**Its stop.** An event's row gets a stop, Calendar, that opens the user's calendars as a list under the row, as Window does on an app's row. `Behind` gains `CALENDAR`; `Stops.list` and `Stops.at` already go by the stop's own `OpenList`; `Stops.of` said "Window" where it meant "a stop with a list of its own", and now says that. A line of that list is marked by its calendar's colour: `Icon.Swatch` can be an action's symbol, as `Icon.App` can.

**What it says.** `Body.Slots` gains a second line of slots (`more`), what is left of a name it read by its start (`completes`), a line for the footer (`footer`) and the text the device's model may be asked about (`ask`: the row carries it itself, so an answer is only ever for the text the row was made from). A `Slot` can have a colour's `dot`. `Effect.InsertEvent` can carry the calendar `link`. All of them default to nothing: every row that is built today is built as before.

The two effects the row will run (the direct save, and a list's line that writes into the field) come with Task 8, together with the lines of `Executor` that take them: `Executor`'s `when` over the effects is exhaustive, and the app must build after every task. For the same reason this task adds one word to the dump's `when` over `Behind`.

- [ ] **Step 1: The failing test of the symbol, in `core/src/test/kotlin/io/github/kuscher/booklight/core/IconTest.kt`.** One change.

Find:

```kotlin
    }

    @Test fun booklightsOwnSymbolsNameNoApp() {
```

Make it:

```kotlin
    }

    @Test fun aColourAsAnActionsSymbol() {
        // A calendar's own colour marks its line in the list of calendars.
        for (argb in listOf(0xFF0B8043.toInt(), 0xFFFFFFFF.toInt(), 0x00000000, 0xFF000000.toInt(), 0x7F123456)) assertEquals(Icon.Swatch(argb), Icon.Swatch.of(Icon.Swatch(argb).symbol))
        assertEquals("dot:ff0b8043", Icon.Swatch(0xFF0B8043.toInt()).symbol)
        for (s in listOf("", "dot", "dot:", "dot:xyz", "dot:1ffffffff", "dot:-1", "dots:ff0b8043", "copy", "t:HEX", "app:a/b#1")) assertNull(s, Icon.Swatch.of(s))
        // And an app's icon is no colour.
        assertNull(Icon.Swatch.of(Icon.App("a", ".B").symbol))
        assertNull(Icon.App.of(Icon.Swatch(1).symbol))
    }

    @Test fun booklightsOwnSymbolsNameNoApp() {
```

- [ ] **Step 2: The failing tests of the stop, in `core/src/test/kotlin/io/github/kuscher/booklight/core/StopsTest.kt`.** One change, at the end.

Find:

```kotlin
        assertEquals(true, Stops.at(r, Behind.ARROW) in Stops.of(r, open = true))
    }
}
```

Make it:

```kotlin
        assertEquals(true, Stops.at(r, Behind.ARROW) in Stops.of(r, open = true))
    }

    /** An event's row: Create · Copy · Calendar, the user's calendars as the lines behind that stop; nothing behind an arrow. */
    private fun event(vararg calendars: String, armed: Int = 0): Result {
        val actions = listOf(a("create"), a("copy"), Action("calendar", "Calendar", Effect.OpenList(Behind.CALENDAR))) +
            calendars.map { Action("cal:$it", it, Effect.Internal(it), more = true, behind = Behind.CALENDAR) }
        return Result("sentence:event", "events", Kind.OTHER, "Event", icon = Icon.Symbol("event"), score = 0.5, actions = actions, armed = armed)
    }

    @Test fun anEventsRowStopsOnItsActionsAndOnCalendar() {
        val r = event("Sam", "Team")
        assertEquals(listOf("create", "copy", "calendar"), ids(r))
        // Calendar opens its own list, which is the row's only one: no arrow, and no stop for one.
        assertEquals(Behind.CALENDAR, Stops.list(r, 2))
        assertNull(Stops.list(r, 0))
        assertNull(Stops.list(r, r.actions.size))
        assertEquals(2, Stops.at(r, Behind.CALENDAR))
        assertEquals(listOf("create", "copy", "calendar"), ids(r, Stops.of(r, open = true)))
        // No calendars known: no such stop.
        assertEquals(listOf(0, 1), Stops.of(event().let { it.copy(actions = it.actions.take(2)) }))
    }

    @Test fun aLineOfAStopsOwnListStandsInThatStopAndInNoOther() {
        // A line of the Calendar list, armed while the list is closed, stands in Calendar's slot, as a typed place stands in Window's.
        val r = event("Sam", "Team", armed = 4)
        assertEquals(listOf("create", "copy", "cal:Team"), ids(r))
        // On an app's row a typed place still stands in Window's slot, and only there.
        assertEquals(listOf("open", "search", "play", "left", "more"), ids(app("left")))
    }
}
```

- [ ] **Step 3: The failing tests of the place, in `core/src/test/kotlin/io/github/kuscher/booklight/core/SearchEngineTest.kt`.** One change: two providers and three tests before `answersGoFirst`.

Find:

```kotlin
    }

    @Test fun answersGoFirst() = runTest {
```

Make it:

```kotlin
    }

    /** A line read as a sentence (an event typed the way it is said), and the question for Gemini that ranks as a match where the line is five words long. */
    private fun sentence(score: Double = 0.5) = object : Provider {
        override val id = "events"
        override suspend fun query(q: Query) = listOf(Result(SearchEngine.SENTENCE + "event", id, Kind.OTHER, "Event", icon = Icon.Symbol("event"), score = score,
            actions = listOf(Action("create", "Create", Effect.InsertEvent("x", 0, 1, false, ""))), learnable = false))
    }
    private val gemini = object : Provider {
        override val id = "gemini"
        override suspend fun query(q: Query) = listOf(Result("web:gemini", id, Kind.WEB, "Ask Gemini", icon = Icon.Symbol("spark"), score = 0.79,
            actions = listOf(Action("ask", "Ask", Effect.AskGemini(q.text))), learnable = false))
    }

    @Test fun aSentenceStandsUnderWhatTheDeviceMatchesAndOverTheWeb() = runTest {
        // Nothing of the device matches: the sentence's row is row one, over Gemini's question and the web search.
        val r = engine(History(), sentence(), gemini).search(Query("add dinner with sam tomorrow at 7pm"))
        assertEquals(listOf("sentence:event", "web:gemini", "web:search"), r.map { it.id })
        // Something of the device matches, however weakly, and however the sentence's row is scored: that comes first.
        for (score in listOf(0.01, 0.5, 1.0, 5.0)) {
            val both = engine(History(), sentence(score), gemini).search(Query("ca"))
            assertEquals("$score", listOf("app:Camera", "app:Canvas", "app:Calendar", "app:Calculator", "sentence:event", "web:gemini", "web:search"), both.map { it.id })
        }
        // An answer is the device's too.
        assertEquals(listOf("calc", "sentence:event", "web:gemini", "web:search"), engine(History(), sentence(), gemini).search(Query("2+2")).map { it.id })
    }

    @Test fun aSentencesRowIsNeverCrowdedOut() = runTest {
        // A list too short for everything: the device's rows give way, the sentence's row and the web's keep their places.
        val r = engine(History(), sentence(), gemini).search(Query("ca"), limit = 4)
        assertEquals(listOf("app:Camera", "app:Canvas", "sentence:event", "web:search"), r.map { it.id })
        assertEquals(listOf("sentence:event", "web:search"), engine(History(), sentence(), gemini).search(Query("ca"), limit = 2).map { it.id })
    }

    @Test fun withoutASentenceTheOrderIsAsItWas() = runTest {
        // Gemini's question for a line that reads as one ranks as a match: over a weaker local row, as before.
        val r = engine(History(), gemini).search(Query("clr"))
        assertEquals(listOf("web:gemini", "app:Calendar", "app:Calculator", "web:search"), r.map { it.id })
        // With one, those local rows stand over it, and Gemini's question under it.
        assertEquals(listOf("app:Calendar", "app:Calculator", "sentence:event", "web:gemini", "web:search"), engine(History(), sentence(), gemini).search(Query("clr")).map { it.id })
    }

    @Test fun answersGoFirst() = runTest {
```

- [ ] **Step 4: Run them and see them fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.IconTest' --tests 'io.github.kuscher.booklight.core.StopsTest' --tests 'io.github.kuscher.booklight.core.SearchEngineTest' --console=plain
```

Expected: `BUILD FAILED` in `:core:compileTestKotlin`: `Unresolved reference 'CALENDAR'`, `Unresolved reference 'SENTENCE'`, `Unresolved reference 'symbol'` and `Unresolved reference 'of'`.

- [ ] **Step 5: The data, in `core/src/main/kotlin/io/github/kuscher/booklight/core/Model.kt`.** Five changes: the swatch as a symbol; `Behind.CALENDAR`; the link of `InsertEvent`; the four fields of `Body.Slots`; the dot of `Slot`.

Find:

```kotlin
    data class Swatch(val argb: Int) : Icon
```

Make it:

```kotlin
    data class Swatch(val argb: Int) : Icon {
        /** This colour as an action's mark: a line of a row's list that is told from the others by its colour (a calendar's own) is marked by a dot of it. */
        val symbol: String get() = SYMBOL + Integer.toHexString(argb)

        companion object {
            private const val SYMBOL = "dot:"

            /** The colour a symbol names; null for any other symbol. */
            fun of(symbol: String): Swatch? = if (!symbol.startsWith(SYMBOL)) null else symbol.substring(SYMBOL.length).toLongOrNull(16)?.takeIf { it in 0..0xFFFFFFFFL }?.let { Swatch(it.toInt()) }
        }
    }
```

Find:

```kotlin
 * the arrow, or Window (where an app's window goes). Two lists at most, and never a list inside a list.
 */
enum class Behind { ARROW, WINDOW }
```

Make it:

```kotlin
 * the arrow, or a stop of the row's own: Window (where an app's window goes), Calendar (which of the
 * user's calendars an event goes to). A row has the arrow's list and at most one other: two lists at
 * most, and never a list inside a list.
 */
enum class Behind { ARROW, WINDOW, CALENDAR }
```

Find:

```kotlin
    /** The calendar's editor, filled in. Times are epoch milliseconds. */
    data class InsertEvent(val title: String, val startMillis: Long, val endMillis: Long, val allDay: Boolean, val place: String) : Effect
```

Make it:

```kotlin
    /**
     * The calendar's editor, filled in. Times are epoch milliseconds. [link]: a calendar link for the
     * same event ([Cals.link]), which names the calendar too: it is sent to the Calendar app first, and
     * the rest is what is sent where no Calendar app takes it. Nothing is saved: the user saves it there.
     */
    data class InsertEvent(val title: String, val startMillis: Long, val endMillis: Long, val allDay: Boolean, val place: String, val link: String = "") : Effect
```

Find:

```kotlin
    /** A preview of what was understood: labelled slots that fill as the argument is typed, and a line for the long part (a message). */
    data class Slots(val caption: String?, val slots: List<Slot>, val note: String? = null) : Body
```

Make it:

```kotlin
    /**
     * A preview of what was understood: labelled slots that fill as the argument is typed, and a line for the long part (a message).
     * [more]: a second line of slots, in the note's place (an event's place and calendar): the row is as high with it as without.
     * [completes]: what is left of a name the row read by its start ("tea" for the Team calendar): the field shows it in grey
     * after the text, and Right takes it. [footer]: what the footer says of the row while it is selected. [ask]: the typed
     * line this preview was made from, where the device's own model may be asked to split it better (an event's sentence
     * whose title the rules left in pieces); null where there is nothing to ask. The row carries it, so that an answer is
     * only ever for the text the row itself was made from.
     */
    data class Slots(val caption: String?, val slots: List<Slot>, val note: String? = null, val more: List<Slot> = emptyList(),
        val completes: String? = null, val footer: String? = null, val ask: String? = null) : Body
```

Find:

```kotlin
data class Slot(val label: String, val value: String, val state: SlotState)
```

Make it:

```kotlin
/** [dot]: a colour that belongs to the value (a calendar's own), drawn as a dot before it; null for none. */
data class Slot(val label: String, val value: String, val state: SlotState, val dot: Int? = null)
```

- [ ] **Step 6: A stop with a list of its own, in `core/src/main/kotlin/io/github/kuscher/booklight/core/Stops.kt`.** Two changes: the header, and `of`.

Find:

```kotlin
 * others as lines of a list: behind its arrow, and on an app's row also behind Window. Tab and the
 * arrows go along the stops; Enter on a stop that opens a list opens it. The places are indices into
 * the row's actions; the arrow, which is no action, is the number of actions.
```

Make it:

```kotlin
 * others as lines of a list: behind its arrow, and behind a stop of its own (Window on an app's row,
 * Calendar on an event's). Tab and the arrows go along the stops; Enter on a stop that opens a list
 * opens it. The places are indices into the row's actions; the arrow, which is no action, is the
 * number of actions.
```

Find:

```kotlin
        val place = typed?.takeIf { r.actions[it].behind == Behind.WINDOW }
        val shown = r.actions.indices.filter { !r.actions[it].more && !r.actions[it].off }.map { if (place != null && r.actions[it].effect is Effect.OpenList) place else it }
```

Make it:

```kotlin
        val place = typed?.takeIf { r.actions[it].behind != Behind.ARROW }
        val shown = r.actions.indices.filter { !r.actions[it].more && !r.actions[it].off }.map { if (place != null && (r.actions[it].effect as? Effect.OpenList)?.behind == r.actions[place].behind) place else it }
```

- [ ] **Step 7: A sentence's place, in `core/src/main/kotlin/io/github/kuscher/booklight/core/SearchEngine.kt`.** Four changes: the header; the end of `search`; `isSentence`; the constant.

Find:

```kotlin
 * local row, and suggestions from the search engine, when they arrive, go below it.
```

Make it:

```kotlin
 * local row, and suggestions from the search engine, when they arrive, go below it. A line read as
 * a whole sentence (an event typed the way it is said) stands below everything of the device that
 * matches and above everything of the web.
```

Find:

```kotlin
        return (ranked.filterNot { isFallback(it) || isGuess(it) || it in letter || it in under }.take((limit - out.size).coerceAtLeast(0)) + out).take(limit)
```

Make it:

```kotlin
        val room = (limit - out.size).coerceAtLeast(0)
        val rest = ranked.filterNot { isFallback(it) || isGuess(it) || it in letter || it in under }
        // A sentence read as a whole: its place is not a score's. Under every row of the device that matches, over every row
        // of the web that ranks as a match (the question for Gemini), and never crowded out by either.
        val sentence = rest.firstOrNull(::isSentence) ?: return (rest.take(room) + out).take(limit)
        val (web, local) = rest.filter { it !== sentence }.partition { it.kind == Kind.WEB }
        return ((local.take((room - 1).coerceAtLeast(0)) + sentence + web).take(room) + out).take(limit)
```

Find:

```kotlin
    private fun isGuess(r: Result) = r.score <= GUESS

    /** The row of a scope whose keyword is the one letter that was typed. */
```

Make it:

```kotlin
    private fun isGuess(r: Result) = r.score <= GUESS

    private fun isSentence(r: Result) = r.id.startsWith(SENTENCE)

    /** The row of a scope whose keyword is the one letter that was typed. */
```

Find:

```kotlin
        /** The id of the web's row: "Search Google for …". */
```

Make it:

```kotlin
        /**
         * How the id of a row starts that reads the whole typed line as a sentence, without a keyword ("Add dinner with Sam
         * tomorrow at 7pm" as an event): it stands below everything of the device that matches, and above the web.
         */
        const val SENTENCE = "sentence:"
        /** The id of the web's row: "Search Google for …". */
```

- [ ] **Step 8: The dump names the new stop, in `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`.** One change, in one long line of `dump`.

Find, in one long line:

```kotlin
io.github.kuscher.booklight.core.Behind.WINDOW -> "window"; null ->
```

Make it:

```kotlin
io.github.kuscher.booklight.core.Behind.WINDOW -> "window"; io.github.kuscher.booklight.core.Behind.CALENDAR -> "calendar"; null ->
```

- [ ] **Step 9: Run them and see them pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.IconTest' --tests 'io.github.kuscher.booklight.core.StopsTest' --tests 'io.github.kuscher.booklight.core.SearchEngineTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Run every core test, and build the app**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT` (without Step 8 the debug build fails: the dump's `when` must name every `Behind`).

- [ ] **Step 11: Commit**

```bash
git add app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt core/src/main/kotlin/io/github/kuscher/booklight/core/Model.kt core/src/main/kotlin/io/github/kuscher/booklight/core/SearchEngine.kt core/src/main/kotlin/io/github/kuscher/booklight/core/Stops.kt core/src/test/kotlin/io/github/kuscher/booklight/core/IconTest.kt core/src/test/kotlin/io/github/kuscher/booklight/core/SearchEngineTest.kt core/src/test/kotlin/io/github/kuscher/booklight/core/StopsTest.kt
git commit -m "An event from a sentence: a sentence's row stands below what the device matches and above the web, and an event's row has a Calendar stop with a list of its own (core, tested)"
```

### Task 7: The calendars, allowed in the window; the switch and the choice

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/Executor.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/data/Prefs.kt`
- Create: `app/src/main/java/io/github/kuscher/booklight/device/CalendarList.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/window/Controls.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/window/MainActivity.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/window/Pages.kt`
- Create: `app/src/main/res/values-de/strings_33.xml`
- Create: `app/src/main/res/values/strings_33.xml`

**Interfaces:**
- Consumes: core `Cal`, `Cals.target(named, all, chosen)` (Tasks 3 and 5). `data/Prefs.kt`: `Settings`, `Prefs.SCHEMA`, `migrate`, `Prefs.now`, `Prefs.update`. `BooklightApp`: `scope`, `prefs`, `onDevice`. `Executor.perform`'s `Effect.Grant` branch. `OverlayActivity.onCreate`. In the window: `Page`, `PageRow(page, key, title, subtitle, mark, onEnter, …, still, place, trailing)`, `Toggle(page, key, title, about, on, mark, place, onChange)`, `Pick(page, key, title, about, ids, names, chosen, mark, place, onPick)`, `menuWidth(names)`, `Word(text)`, `MarkIcon(name)`, `Group`, `Rise`, `LocalColumn`, `INSET`, `MARK`; `MainActivity.EXTRA_PAGE`, `PAGE_FLIGHTS`, `PAGE_SONGS`; `FLIGHT_KEY_ROW`, `SONG_KEY_ROW`; in the `Window` composable `resumed`, `picked`, `landing`, `go(to)`; `ResultsPage(page, app, s, arrive, from)`, `PrivacyPage(page, app, resumed, arrive, from)`.
- Produces:
  - The manifest's two permissions: `android.permission.READ_CALENDAR`, `android.permission.WRITE_CALENDAR`. Nothing else in the manifest changes.
  - `class CalendarList(context: Context, scope: CoroutineScope)` in `device/`: `val now: StateFlow<List<Cal>>`, `val known: List<Cal>`, `val allowed: Boolean`, `val writes: Boolean`, `val pretends: Boolean`, `fun refresh()`, `fun pretend(list: List<Cal>?)`.
  - `BooklightApp.calendars: CalendarList` and `fun BooklightApp.lookAtCalendars()`.
  - `Settings.saveEvents: Boolean = false`, `Settings.eventCalendar: String = ""`, `Settings.calendarsAsked: Boolean = false`; the settings' schema is 8.
  - `Executor` performs `Effect.Grant("calendars")`: the Booklight window, on that row.
  - `MainActivity.askCalendar(write: Boolean, then: (Boolean) -> Unit = {})`, `MainActivity.systemPage()`, `MainActivity.PAGE_CALENDARS = "calendars"`, `MainActivity.rowFor(goto: String?): Pair<Part, String>?`.
  - `const val CALENDARS_ROW = "calendars"` (in `Pages.kt`); `ResultsPage(page, app, s, resumed, arrive, from)`; `PrivacyPage(page, app, s, resumed, arrive, from)`.
  - `Pick(…, beside: Boolean? = null, onPick)` and `@Composable fun menuBeside(names: List<String>, mark: Boolean): Boolean`.
  - Strings, English and German, in the new `strings_33.xml`: `win_calendars`, `win_calendars_off`, `win_calendars_on`, `win_calendars_write`, `win_events`, `set_save_events`, `set_save_events_text`, `set_event_calendar`, `set_event_calendar_text`, `set_event_calendar_off`, `set_event_calendar_none`.

Two permissions, by Alex's word (design §11.3), **both inert until the user allows them in the Booklight window**. This task is everything about them, and nothing of the row: after it a build looks and behaves as before, with one more row on Privacy and one more group on Results.

**The list.** `CalendarList` is the one place that asks the system's calendar provider anything, and what it asks for is the list of calendars: seven columns of each (id, name, colour, owner, whether it is the account's own, how much may be done with it, the account), and never an event. Only the calendars of Google accounts ("Decided here", 15). It asks nothing unless the permission is there; `refresh()` reads off the main thread and keeps the list, and where the permission is gone it empties the list at once. `pretend` is for Task 10's hook.

**When it is read.** `BooklightApp.lookAtCalendars()`, called as a panel is made and when the window has the keys again: it reads the list again, and **it switches "Save events without opening Calendar" off where the permission for it is gone**, so that the switch is on only by the user's own press in the window (also after the permission was taken back and given again in the system's screens, and on a device the settings came to with a backup). Never while typing.

**The settings.** Three fields, and schema 8. The migration takes nothing of the three from a file from before: the switch is only ever turned on in the window.

**The window.** Privacy › What Booklight may use gets "Calendars" under Brightness, built like it: a mark, one line that says what is read ("Never an event"), and "Allow…" as its word. Enter asks Android's own question (`MainActivity.askCalendar`); allowed, the word is "Change…" and Enter opens Booklight's page in the system's Settings, where it is taken back. Results gets a group "Events" between "Show" and the other apps: the switch, which asks for both permissions when it is switched on without them and **stays off when the question is refused**; and "New events go to", a menu of the calendars that take events (the account's own until another is chosen), or, while the list is not allowed, a row that asks for it.

**Android's question** is asked through one launcher in `MainActivity`, by a press on a row of the window and by nothing else. Its answer is read there: the list is read again, `calendarsAsked` is noted, and whoever asked hears whether everything was given. Refused for good, the system shows no question and says no at once: the system's page opens instead ("Decided here", 21).

**The window's lines.** The new rows are `PageRow`, `Toggle` and `Pick`, so their marks, texts and controls stand on the lines every other row's do. Results now has two menus, and "the choices of one page all stand in the same place": `Pick` takes the page's decision (`beside`), which `ResultsPage` makes for both with `menuBeside`; a calendar's name longer than 28 letters is cut for the button, so that one long name does not send both menus under their text.

- [ ] **Step 1: Write `app/src/main/res/values/strings_33.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- An event from a sentence (docs/design/event-sentence/design.md): the calendars in the Booklight window, the event row's slots, actions and lines. -->
<resources>
    <!-- The window › Privacy › What Booklight may use: the list of calendars. Not allowed; allowed; allowed, and events are added too. -->
    <string name="win_calendars">Calendars</string>
    <string name="win_calendars_off">The names and colours of your calendars, to put an event on one by its name. Never an event. Not allowed yet.</string>
    <string name="win_calendars_on">Booklight reads the list of your calendars. It never reads an event.</string>
    <string name="win_calendars_write">Booklight reads the list of your calendars and adds the events you save from it. It never reads an event.</string>
    <!-- The window › Results: an event typed as a sentence. -->
    <string name="win_events">Events</string>
    <string name="set_save_events">Save events without opening Calendar</string>
    <string name="set_save_events_text">Enter saves an event at once where its day and its time were both read. Any other opens in Calendar, filled in.</string>
    <string name="set_event_calendar">New events go to</string>
    <string name="set_event_calendar_text">For a saved event that names no calendar.</string>
    <string name="set_event_calendar_off">Your own calendar. To choose another, allow Calendars.</string>
    <string name="set_event_calendar_none">No calendar here takes new events.</string>
</resources>
```

- [ ] **Step 2: Write `app/src/main/res/values-de/strings_33.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="win_calendars">Kalender</string>
    <string name="win_calendars_off">Die Namen und Farben deiner Kalender, um einen Termin mit dem Namen in einen davon zu legen. Nie ein Termin. Noch nicht erlaubt.</string>
    <string name="win_calendars_on">Booklight liest die Liste deiner Kalender. Es liest nie einen Termin.</string>
    <string name="win_calendars_write">Booklight liest die Liste deiner Kalender und trägt die Termine ein, die du daraus speicherst. Es liest nie einen Termin.</string>
    <string name="win_events">Termine</string>
    <string name="set_save_events">Termine speichern, ohne Kalender zu öffnen</string>
    <string name="set_save_events_text">Enter speichert einen Termin sofort, wenn Tag und Uhrzeit beide gelesen wurden. Jeder andere öffnet sich im Kalender, ausgefüllt.</string>
    <string name="set_event_calendar">Neue Termine kommen in</string>
    <string name="set_event_calendar_text">Für einen gespeicherten Termin, der keinen Kalender nennt.</string>
    <string name="set_event_calendar_off">Deinen eigenen Kalender. Für einen anderen: Kalender erlauben.</string>
    <string name="set_event_calendar_none">Hier nimmt kein Kalender neue Termine an.</string>
</resources>
```

- [ ] **Step 3: The two permissions, in `app/src/main/AndroidManifest.xml`.** One change.

Find:

```xml
    <!-- No runtime permissions, no accessibility service. -->
```

Make it:

```xml
    <!-- The list of the user's calendars (names, colours, ids): an event typed as a sentence can name one, and opens on
         it. Never an event: Booklight reads none. Asked for only in the Booklight window (Privacy › Calendars), by the
         user's own press; until then nothing is read and the system shows no question. -->
    <uses-permission android:name="android.permission.READ_CALENDAR" />
    <!-- To add the one event the row shows, on the user's Enter, and only with "Save events without opening
         Calendar" switched on in the Booklight window, which is where this is asked for. Booklight never changes or
         removes an event. Without the switch an event opens in the Calendar app, filled in, and is saved there. -->
    <uses-permission android:name="android.permission.WRITE_CALENDAR" />
    <!-- Two runtime permissions, both the Calendar's, both asked for in the Booklight window alone. No accessibility service. -->
```

- [ ] **Step 4: Write `app/src/main/java/io/github/kuscher/booklight/device/CalendarList.kt`**

```kotlin
package io.github.kuscher.booklight.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Calendars
import android.util.Log
import io.github.kuscher.booklight.BooklightApp
import io.github.kuscher.booklight.core.Cal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The user's calendars, as the system lists them: each one's name, its colour, the id the system has for
 * it, its owner's address (what a calendar link names it by), whether it is the account's own and whether
 * events can be added to it. **That list and nothing else: no event is ever read**, here or anywhere in
 * Booklight.
 *
 * Only with the Calendar permission, which Booklight asks for in its own window and nowhere else. Without
 * it the list is empty and the system is asked nothing. The list is read when a panel is made and when
 * the Booklight window has the keys again, off the main thread, and kept: typing reads what is kept,
 * never the system. Only the calendars of Google accounts are listed: those are the ones the Calendar
 * app on a Googlebook shows and a calendar link can name.
 */
class CalendarList(private val context: Context, private val scope: CoroutineScope) {
    private val read = MutableStateFlow<List<Cal>>(emptyList())
    /** Debug builds: a list to show in place of the device's (`./bl debug calendars pretend A,B`), to look at the row without the permission. */
    private val pretended = MutableStateFlow<List<Cal>?>(null)
    private val _now = MutableStateFlow<List<Cal>>(emptyList())

    /** The calendars as they were last read (or pretended), the account's own first. */
    val now: StateFlow<List<Cal>> = _now
    val known: List<Cal> get() = _now.value

    /** The list may be read: the user allowed it in the Booklight window. */
    val allowed: Boolean get() = granted(Manifest.permission.READ_CALENDAR)
    /** An event may be added: the user switched that on in the Booklight window, and the system agreed. */
    val writes: Boolean get() = granted(Manifest.permission.WRITE_CALENDAR)
    /** A list is pretended (debug builds): the row is as with the permission, and nothing is written. */
    val pretends: Boolean get() = pretended.value != null

    private fun granted(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    /** Reads the list again, if it may be read; taken back (in the system's settings), the list is empty at once. */
    fun refresh() {
        if (!allowed) { set(emptyList()); return }
        scope.launch(Dispatchers.IO) { set(query()) }
    }

    /** Debug builds: [list] in place of the device's calendars; null for the device's own again. */
    fun pretend(list: List<Cal>?) { pretended.value = list; _now.value = list ?: read.value }

    private fun set(list: List<Cal>) { read.value = list; _now.value = pretended.value ?: list }

    private fun query(): List<Cal> = try {
        val out = ArrayList<Cal>()
        context.contentResolver.query(Calendars.CONTENT_URI, COLUMNS, "${Calendars.ACCOUNT_TYPE} = ?", arrayOf(GOOGLE), null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1)?.trim().orEmpty()
                if (name.isEmpty()) continue
                val owner = c.getString(3).orEmpty()
                out += Cal(
                    id = c.getLong(0), name = name, color = c.getInt(2) or OPAQUE, owner = owner,
                    // (Where the system does not say which is the account's own: the one the account itself owns.)
                    primary = c.getInt(4) == 1 || (owner.isNotEmpty() && owner == c.getString(6)),
                    writable = c.getInt(5) >= Calendars.CAL_ACCESS_CONTRIBUTOR,
                )
            }
        }
        out.sortedWith(compareByDescending<Cal> { it.primary }.thenBy { it.name.lowercase() })
    } catch (e: Exception) {
        // Only the kind of failure: nothing of the list is logged.
        Log.w(BooklightApp.TAG, "calendars not read (${e.javaClass.simpleName})")
        emptyList()
    }

    private companion object {
        /** What is read of a calendar, and nothing more. */
        val COLUMNS = arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.CALENDAR_COLOR, Calendars.OWNER_ACCOUNT, Calendars.IS_PRIMARY, Calendars.CALENDAR_ACCESS_LEVEL, Calendars.ACCOUNT_NAME)
        const val GOOGLE = "com.google"
        const val OPAQUE = 0xFF000000.toInt()
    }
}
```

- [ ] **Step 5: The settings and their migration, in `app/src/main/java/io/github/kuscher/booklight/data/Prefs.kt`.** Four changes: the three fields; the schema's line; its number; the migration.

Find:

```kotlin
    /** The section the Booklight window was left on (`window/Nav.kt`): it opens there again. */
```

Make it:

```kotlin
    /**
     * An event typed as a sentence (core `Sentence`). [saveEvents]: Enter saves it into the calendar itself, where its day
     * and time were both read; off unless chosen in the Booklight window, and it counts only while the system's permission
     * to add events is there. [eventCalendar]: where a saved event goes that names no calendar, as that calendar's owner's
     * address; empty for the account's own. [calendarsAsked]: the system's question for the calendars has been answered
     * once, yes or no: the event row no longer points at it.
     */
    val saveEvents: Boolean = false,
    val eventCalendar: String = "",
    val calendarsAsked: Boolean = false,
    /** The section the Booklight window was left on (`window/Nav.kt`): it opens there again. */
```

Find:

```kotlin
    /** The shape of this file: 1 = Booklight 1.0, 2 = 1.1, 3 = 2.0, 4 = 2.2, 5 = ready-made prompts by reference, 6 = app commands of the user's own, 7 = first run. */
```

Make it:

```kotlin
    /** The shape of this file: 1 = Booklight 1.0, 2 = 1.1, 3 = 2.0, 4 = 2.2, 5 = ready-made prompts by reference, 6 = app commands of the user's own, 7 = first run, 8 = events saved from the panel. */
```

Find:

```kotlin
        const val SCHEMA = 7
```

Make it:

```kotlin
        const val SCHEMA = 8
```

Find:

```kotlin
        if (s.schema < 7) s = s.asUpdate()
```

Make it:

```kotlin
        if (s.schema < 7) s = s.asUpdate()
        // Schema 8: an event saved straight into a calendar. The switch is only ever turned on in the Booklight window, by
        // the user, with the system's question answered there: a file from before has none that was, so whatever it holds
        // of the three is not taken.
        if (s.schema < 8) s = s.copy(saveEvents = false, eventCalendar = "", calendarsAsked = false)
```

- [ ] **Step 6: The process has the list, in `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`.** Three changes: the property; `lookAtCalendars`; the list made in `onCreate`.

Find:

```kotlin
    /** What the pinned window shows, while there is one. */
```

Make it:

```kotlin
    /** The list of the user's calendars, once they allowed it in the window: names, colours, ids. Never an event. */
    lateinit var calendars: io.github.kuscher.booklight.device.CalendarList private set
    /** What the pinned window shows, while there is one. */
```

Find:

```kotlin
    private val ownDp: Float by lazy { getSystemService(android.view.WindowManager::class.java).maximumWindowMetrics.bounds.height() / resources.displayMetrics.density }

    /** Every source of results. A new ability is one more line here. */
```

Make it:

```kotlin
    private val ownDp: Float by lazy { getSystemService(android.view.WindowManager::class.java).maximumWindowMetrics.bounds.height() / resources.displayMetrics.density }

    /**
     * What the system allows of the calendars, looked at as a panel is made and when the Booklight window has the keys
     * again: the list is read again (or emptied, where it may no longer be read), and "Save events without opening
     * Calendar" goes off where its permission is gone. So the switch is on only by the user's own press in the window,
     * also after the permission was taken back and given again in the system's screens, and on a device the settings
     * came to with a backup. (Not while a list is pretended, in a debug build: there the switch is set by its hook, to
     * look at the row, and nothing can be written.)
     */
    fun lookAtCalendars() {
        calendars.refresh()
        if (prefs.now.saveEvents && !calendars.pretends && !(calendars.allowed && calendars.writes)) prefs.update { it.copy(saveEvents = false) }
    }

    /** Every source of results. A new ability is one more line here. */
```

Find:

```kotlin
        onDevice = OnDevice(scope)
```

Make it:

```kotlin
        onDevice = OnDevice(scope)
        calendars = io.github.kuscher.booklight.device.CalendarList(this, scope)
```

- [ ] **Step 7: Read as a panel is made, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** One change, in `onCreate`.

Find:

```kotlin
        val slowly = staged && FirstRun.slow(run, FirstRun.Overture.WELCOME)
```

Make it:

```kotlin
        val slowly = staged && FirstRun.slow(run, FirstRun.Overture.WELCOME)
        // The list of calendars is read as the panel is made, where the user allowed it, off the main thread: typing never reads it.
        app.lookAtCalendars()
```

- [ ] **Step 8: The way from a row of the panel to the window's row, in `app/src/main/java/io/github/kuscher/booklight/Executor.kt`.** One change, in the `Effect.Grant` branch.

Find:

```kotlin
                // With a note to add once the folder is chosen: "notes", a line break, the note.
```

Make it:

```kotlin
                // The calendars are allowed in the Booklight window and nowhere else: the window, on that row.
                "calendars" -> start(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_PAGE, MainActivity.PAGE_CALENDARS))
                // With a note to add once the folder is chosen: "notes", a line break, the note.
```

- [ ] **Step 9: Android's question, and the row a start may ask for, in `app/src/main/java/io/github/kuscher/booklight/window/MainActivity.kt`.** Eight changes: imports; the launcher, `askCalendar` and `systemPage`; the constants and `rowFor`; the section the window opens on; the list read when the window has the keys again; the landing on a row; and the two pages' new parameters.

Find:

```kotlin
import android.content.Intent
import android.os.Build
import android.os.Bundle
```

Make it:

```kotlin
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings as SystemSettings
```

Find:

```kotlin
import androidx.compose.animation.AnimatedContent
```

Make it:

```kotlin
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
```

Find:

```kotlin
    var guides = false
```

Make it:

```kotlin
    var guides = false

    /** Who asked the system for the Calendar permission, and when: the answer is handed on once. */
    private var answered: ((Boolean) -> Unit)? = null
    private var askedAt = 0L

    /**
     * The system's answer to its own question for the Calendar permission, read here and nowhere else: the list of
     * calendars is read again (or emptied), the settings note that the question has been answered once, and whoever
     * asked hears whether all that was asked for was given.
     */
    private val calendarQuestion = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { got ->
        val app = application as BooklightApp
        val given = got.isNotEmpty() && got.values.all { it }
        app.calendars.refresh()
        if (!app.prefs.now.calendarsAsked) app.prefs.update { it.copy(calendarsAsked = true) }
        // Refused for good, the system shows no question and says no at once: Booklight's page in the system's Settings
        // is then the one place where it can still be allowed, so that page opens.
        if (!given && SystemClock.uptimeMillis() - askedAt < NO_QUESTION_MS && got.keys.none { shouldShowRequestPermissionRationale(it) }) systemPage()
        answered?.invoke(given)
        answered = null
    }

    /**
     * Asks the system for the Calendar permission: to read the list of calendars and, with [write], to add an event too.
     * Only ever for a press on a row of this window: nothing else in Booklight asks. [then] hears whether all of it was given.
     */
    fun askCalendar(write: Boolean, then: (Boolean) -> Unit = {}) {
        answered = then
        askedAt = SystemClock.uptimeMillis()
        calendarQuestion.launch(if (write) arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR) else arrayOf(Manifest.permission.READ_CALENDAR))
    }

    /** Booklight's own page in the system's Settings: where a permission is taken back, and given after it was refused for good. */
    fun systemPage() { runCatching { startActivity(Intent(SystemSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) } }
```

Find:

```kotlin
        const val PAGE_SONGS = "songs"
```

Make it:

```kotlin
        const val PAGE_SONGS = "songs"
        /** Privacy, on the row where the calendars are allowed: where the event row's "Allow…" leads. */
        const val PAGE_CALENDARS = "calendars"
        /** An answer from the system sooner than this was no person's: it showed no question. */
        private const val NO_QUESTION_MS = 300L

        /** The section and the row a start of the window may ask for by one word. */
        fun rowFor(goto: String?): Pair<Part, String>? = when (goto) {
            PAGE_FLIGHTS -> Part.LABS to FLIGHT_KEY_ROW
            PAGE_SONGS -> Part.LABS to SONG_KEY_ROW
            PAGE_CALENDARS -> Part.PRIVACY to CALENDARS_ROW
            else -> null
        }
```

Find, in one long line:

```kotlin
if (goto == MainActivity.PAGE_FLIGHTS || goto == MainActivity.PAGE_SONGS) Part.LABS else
```

Make it:

```kotlin
MainActivity.rowFor(goto)?.first ?:
```

Find:

```kotlin
    /** "Open Keyboard shortcuts" was pressed: the line about the key says what to do next, whichever section was looked at in between. */
```

Make it:

```kotlin
    // The list of calendars is read again whenever the window has the keys again (the permission may have been given or
    // taken back in the system's own screens meanwhile). Where it is not allowed this asks the system nothing.
    LaunchedEffect(resumed) { app.lookAtCalendars() }
    /** "Open Keyboard shortcuts" was pressed: the line about the key says what to do next, whichever section was looked at in between. */
```

Find:

```kotlin
        if (goto == MainActivity.PAGE_FLIGHTS || goto == MainActivity.PAGE_SONGS) { picked[Part.LABS] = if (goto == MainActivity.PAGE_SONGS) SONG_KEY_ROW else FLIGHT_KEY_ROW; landing = true; go(Part.LABS); inNav = false; entering = true; byKeys = true; onGone(); return@LaunchedEffect }
```

Make it:

```kotlin
        // "Allow…" on an event's row: Privacy, on the row where the calendars are allowed.
        MainActivity.rowFor(goto)?.let { (to, row) -> picked[to] = row; landing = true; go(to); inNav = false; entering = true; byKeys = true; onGone(); return@LaunchedEffect }
```

Find:

```kotlin
                                        Part.RESULTS -> ResultsPage(page, app, s, arrive, from)
                                        Part.LABS -> LabsPage(page, app, arrive, from, onTyping = { editing = it; if (!it) runCatching { focus.requestFocus() } })
                                        Part.PRIVACY -> PrivacyPage(page, app, resumed, arrive, from)
```

Make it:

```kotlin
                                        Part.RESULTS -> ResultsPage(page, app, s, resumed, arrive, from)
                                        Part.LABS -> LabsPage(page, app, arrive, from, onTyping = { editing = it; if (!it) runCatching { focus.requestFocus() } })
                                        Part.PRIVACY -> PrivacyPage(page, app, s, resumed, arrive, from)
```

- [ ] **Step 10: A page decides where its menus stand, in `app/src/main/java/io/github/kuscher/booklight/window/Controls.kt`.** Three changes: `Pick`'s parameter and its use, and `menuBeside`.

Find:

```kotlin
    page: Page, key: String, title: String, about: String?, ids: List<String>, names: List<String>, chosen: String, mark: String? = null, place: Place? = null,
```

Make it:

```kotlin
    page: Page, key: String, title: String, about: String?, ids: List<String>, names: List<String>, chosen: String, mark: String? = null, place: Place? = null,
    /** Where the button stands, when the page decides it for all its menus together ([menuBeside]); else the row decides for itself. */
    beside: Boolean? = null,
```

Find:

```kotlin
    val width = menuWidth(names)
    val room = LocalColumn.current - INSET * 2 - (if (mark != null) MARK + 12.dp else 0.dp)
    // The button is narrow enough to stay at the row's trailing end down to the window's smallest width, on the line
    // the switches under it end on: the row's name keeps its one line beside it, and the line under the name wraps.
    val beside = room - 12.dp - width >= 120.dp
```

Make it:

```kotlin
    // The button is narrow enough to stay at the row's trailing end down to the window's smallest width, on the line
    // the switches under it end on: the row's name keeps its one line beside it, and the line under the name wraps.
    @Suppress("NAME_SHADOWING") val beside = beside ?: menuBeside(names, mark != null)
```

Find:

```kotlin
}

/** How wide the button of a menu of these names is: 16, the longest name, 8, the arrow (or the check) of 20, 16. */
```

Make it:

```kotlin
}

/**
 * Whether the button of a menu of these names has room at the trailing end of its row, beside the row's name. A page
 * with more than one menu asks this for all of them and puts them all in the same place, as it does for its choices.
 */
@Composable
fun menuBeside(names: List<String>, mark: Boolean): Boolean {
    val room = LocalColumn.current - INSET * 2 - (if (mark) MARK + 12.dp else 0.dp)
    return room - 12.dp - menuWidth(names) >= 120.dp
}

/** How wide the button of a menu of these names is: 16, the longest name, 8, the arrow (or the check) of 20, 16. */
```

- [ ] **Step 11: The grant, the switch and the choice, in `app/src/main/java/io/github/kuscher/booklight/window/Pages.kt`.** Seven changes: two imports; the head of `ResultsPage` (the calendars, and where the two menus stand); the group "Events", and the number of the block after it; two constants; `PrivacyPage`'s parameter; and the row "Calendars".

Find:

```kotlin
import androidx.activity.ComponentActivity
```

Make it:

```kotlin
import androidx.compose.runtime.collectAsState
import io.github.kuscher.booklight.core.Cals
import androidx.activity.ComponentActivity
```

Find:

```kotlin
fun ResultsPage(page: Page, app: BooklightApp, s: Settings, arrive: Animatable<Float, *>, from: Float) {
    fun set(change: (Settings) -> Settings) = app.prefs.update(change)
```

Make it:

```kotlin
fun ResultsPage(page: Page, app: BooklightApp, s: Settings, resumed: Int, arrive: Animatable<Float, *>, from: Float) {
    fun set(change: (Settings) -> Settings) = app.prefs.update(change)
    val window = LocalActivity.current as? MainActivity
    // The calendars an event can be added to, where the list is allowed. (A very long name is cut for the menu's button.)
    val calendars by app.calendars.now.collectAsState()
    val open = calendars.filter { it.writable }
    val names = open.map { if (it.name.length > CALENDAR_NAME) it.name.take(CALENDAR_NAME - 1).trimEnd() + "…" else it.name }
    // The page's two menus stand in the same place: both at their rows' trailing ends, or both under their text.
    val engines = Engines.all.map { it.name }
    val beside = menuBeside(engines, true) && (names.isEmpty() || menuBeside(names, true))
```

Find, in one long line:

```kotlin
Engines.all.map { e -> e.name }, s.engine, mark = "search", place = it
```

Make it:

```kotlin
engines, s.engine, mark = "search", place = it, beside = beside
```

Find:

```kotlin
    }
    Rise(arrive, 3, from) {
        val offers = app.commands.offers
```

Make it:

```kotlin
    }
    // An event typed as a sentence: whether Enter saves it, and where one goes that names no calendar.
    Rise(arrive, 3, from) {
        resumed     // (read: what the system allows is looked at again when the window has the keys again)
        val reads = app.calendars.allowed
        val may = reads && app.calendars.writes
        Group(stringResource(R.string.win_events)) {
            // On only while the system's permission to add events is there: refused, or taken back, the switch stands off.
            row("save-events") {
                Toggle(page, "save-events", stringResource(R.string.set_save_events), stringResource(R.string.set_save_events_text), s.saveEvents && may, mark = "event", place = it) { on ->
                    when {
                        !on -> set { st -> st.copy(saveEvents = false) }
                        may -> set { st -> st.copy(saveEvents = true) }
                        // The system's own question first: for the list of calendars and for adding an event. Refused, nothing changes.
                        else -> window?.askCalendar(write = true) { given -> if (given) set { st -> st.copy(saveEvents = true) } }
                    }
                }
            }
            row("event-calendar") { place ->
                when {
                    // Not allowed yet: the row asks. Allowed, and no calendar that takes events: it only says so.
                    !reads -> PageRow(page, "event-calendar", stringResource(R.string.set_event_calendar), stringResource(R.string.set_event_calendar_off), place = place, mark = { MarkIcon("list") },
                        onEnter = { window?.askCalendar(write = false) }) { Word(stringResource(R.string.action_allow)) }
                    open.isEmpty() -> PageRow(page, "event-calendar", stringResource(R.string.set_event_calendar), stringResource(R.string.set_event_calendar_none), place = place, mark = { MarkIcon("list") }, still = true)
                    else -> Pick(page, "event-calendar", stringResource(R.string.set_event_calendar), stringResource(R.string.set_event_calendar_text), open.map { c -> c.owner }, names,
                        Cals.target(null, open, s.eventCalendar)?.owner.orEmpty(), mark = "list", place = place, beside = beside) { v -> set { st -> st.copy(eventCalendar = v) } }
                }
            }
        }
    }
    Rise(arrive, 4, from) {
        val offers = app.commands.offers
```

Find:

```kotlin
}

/** An app's own icon as a row's mark. It fades in when it had to be loaded: nothing cuts in. */
```

Make it:

```kotlin
}

/** The most letters of a calendar's name on the button of "New events go to". */
private const val CALENDAR_NAME = 28

/** The row of the window where the list of calendars is allowed: "Allow…" on an event's row in the panel opens the window on it. */
const val CALENDARS_ROW = "calendars"

/** An app's own icon as a row's mark. It fades in when it had to be loaded: nothing cuts in. */
```

Find:

```kotlin
fun PrivacyPage(page: Page, app: BooklightApp, resumed: Int, arrive: Animatable<Float, *>, from: Float) {
```

Make it:

```kotlin
fun PrivacyPage(page: Page, app: BooklightApp, s: Settings, resumed: Int, arrive: Animatable<Float, *>, from: Float) {
```

Find:

```kotlin
                    mark = { MarkIcon("sun") }, onEnter = { app.executor.run(Effect.Grant("brightness"), activity) }) { Word(stringResource(if (bright) R.string.win_change else R.string.action_allow)) }
            }
        }
```

Make it:

```kotlin
                    mark = { MarkIcon("sun") }, onEnter = { app.executor.run(Effect.Grant("brightness"), activity) }) { Word(stringResource(if (bright) R.string.win_change else R.string.action_allow)) }
            }
            // The list of calendars: allowed by this row and by nothing else in Booklight. Allowed, the row leads to the
            // system's own page, where it is taken back. The line says what is read, and what is written only where the
            // switch for that is on and the system agreed.
            row(CALENDARS_ROW) { place ->
                val reads = app.calendars.allowed
                PageRow(page, CALENDARS_ROW, stringResource(R.string.win_calendars),
                    stringResource(if (!reads) R.string.win_calendars_off else if (s.saveEvents && app.calendars.writes) R.string.win_calendars_write else R.string.win_calendars_on), place = place,
                    mark = { MarkIcon("event") }, onEnter = { (activity as? MainActivity)?.let { w -> if (reads) w.systemPage() else w.askCalendar(write = false) } }) {
                    Word(stringResource(if (reads) R.string.win_change else R.string.action_allow))
                }
            }
        }
```

- [ ] **Step 12: Look for what must be so**

```bash
grep -c "uses-permission" app/src/main/AndroidManifest.xml
grep -n "android.permission.READ_CALENDAR\|android.permission.WRITE_CALENDAR" app/src/main/AndroidManifest.xml | wc -l
git diff -- app/src/main/AndroidManifest.xml | grep -c "^+.*<queries\|^+.*<package\|^+.*<intent"
grep -rn "CalendarContract" app/src/main --include='*.kt' | grep -v "device/CalendarList.kt\|Executor.kt" || echo CLEAN
grep -n "CalendarContract.Events\|Instances\|Attendees\|Reminders" app/src/main/java/io/github/kuscher/booklight/device/CalendarList.kt || echo CLEAN
for n in win_calendars win_calendars_off win_calendars_on win_calendars_write win_events set_save_events set_save_events_text set_event_calendar set_event_calendar_text set_event_calendar_off set_event_calendar_none; do echo "$n $(grep -c "name=\"$n\"" app/src/main/res/values/strings_33.xml) $(grep -c "name=\"$n\"" app/src/main/res/values-de/strings_33.xml)"; done
```

Expected: `6` (four permissions before, and these two); `2`; `0` (no line added to `<queries>`); `CLEAN` (the calendar provider is named in the list's file and, for the insert request that was always there, in `Executor`, and nowhere else); `CLEAN` (the list's file names no table but the calendars'); then eleven lines, each a name followed by `1 1`.

- [ ] **Step 13: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 14: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt app/src/main/java/io/github/kuscher/booklight/Executor.kt app/src/main/java/io/github/kuscher/booklight/data/Prefs.kt app/src/main/java/io/github/kuscher/booklight/device/CalendarList.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt app/src/main/java/io/github/kuscher/booklight/window/Controls.kt app/src/main/java/io/github/kuscher/booklight/window/MainActivity.kt app/src/main/java/io/github/kuscher/booklight/window/Pages.kt app/src/main/res/values-de/strings_33.xml app/src/main/res/values/strings_33.xml
git commit -m "An event from a sentence: the list of calendars is read where the user allows it in the window, with the switch that saves events and the choice of where they go"
```

### Task 8: The event's row, with the keyword and without

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/Executor.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/Guide.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Bodies.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`
- Create: `app/src/main/java/io/github/kuscher/booklight/providers/Events.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/scopes/Jot.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/scopes/Scopes.kt`
- Modify: `app/src/main/res/values-de/strings_11.xml`
- Modify: `app/src/main/res/values-de/strings_20.xml`
- Modify: `app/src/main/res/values-de/strings_33.xml`
- Modify: `app/src/main/res/values/strings_11.xml`
- Modify: `app/src/main/res/values/strings_20.xml`
- Modify: `app/src/main/res/values/strings_33.xml`
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/Model.kt`

**Interfaces:**
- Consumes: core `Sentence.read`, `Sentence.reads`, `Sentence.put`, `EventReading`, `Cal` (Task 3); `Cals.target`, `Cals.saves`, `Cals.link`, `Cals.write`, `Cals.TEMPLATE` (Task 5); `SearchEngine.SENTENCE`, `Behind.CALENDAR`, `Icon.Swatch.symbol`, `Icon.Swatch.of`, `Effect.InsertEvent.link`, `Body.Slots.more`, `completes`, `footer`, `Slot.dot` (Task 6). App: `BooklightApp.calendars` (`known`, `allowed`, `writes`, `pretends`), `Settings.saveEvents`, `eventCalendar`, `calendarsAsked`, `Effect.Grant("calendars")`, `strings_33.xml` (Task 7). `scopes/Jot.kt`: `JotScope`, `EventScope(context)`, the internal `day`, `clock`, `span(context, e)`, `JotScope.insert(e)`. `Scopes(…)`. In `OverlayModel`: `opened`, `shut()`, `filledAt`, `touched`, `search(keep)`, `enter`, `runRow`, `actionRows`. In `Rows.kt`: `ResultRow`, `ActionRow`. `SlotsBody`. `Footer`. `Executor.perform`, `start(intent)`. `OverlayActivity.run`. `Guide.used`.
- Produces:
  - In `Model.kt`: `data class Effect.SaveEvent(val calendar: Long, val title: String, val startMillis: Long, val endMillis: Long, val allDay: Boolean, val zone: String, val place: String)` and `data class Effect.Retype(val text: String)`.
  - `class Events(context: Context, prefs: Prefs, calendars: CalendarList) : Provider` in `providers/`: `override val id = Events.ID`; `suspend fun query(q: Query): List<Result>` (the row of a sentence without the keyword, or nothing); `fun row(text: String): Result` (the row under the keyword); `fun row(id: String, text: String, r: EventReading, now: LocalDateTime): Result`; `Events.ID = "events"`, `Events.SCOPE = "jot:event"`, `Events.SENTENCE = "sentence:event"`.
  - `BooklightApp.events: Events`, registered among the providers before the web's; `Scopes(…, events = events)`; `EventScope(context, events)`.
  - `internal fun insert(e: EventDraft, link: String = ""): Effect.InsertEvent` (top level, in `scopes/Jot.kt`).
  - `Executor`: `Effect.InsertEvent` sends its link to `Executor.CALENDAR` first; `Effect.SaveEvent` is `private fun save(e)`; `Effect.Retype` is the panel's.
  - `OverlayModel`: `private fun retype(text: String)`; Ctrl + digit never saves.
  - Strings: `slot_calendar`, `action_save_event`, `action_open_calendar`, `event_caption_save`, `done_saved_event`, `event_into`, `event_footer_allow`, `event_footer_repeats`, `event_footer_guess`, `event_footer_closed`; `event_hint` and the `guide` array's `event` line are reworded.

**One row, built in one place.** `providers/Events.kt` builds the event's row from core's reading. `EventScope` asks it for the row under the keyword (`jot:event`, as before); as a provider it offers the same row for a line typed without the keyword, where `Sentence.reads` says so (`sentence:event`, which the engine places: Task 6).

**The row** is the preview row it was, 92 dp, with a second line of slots: WHEN and TITLE, then WHERE and, where the calendars are known, CALENDAR with the calendar's colour as a dot. **Both lines always stand** ("Decided here", 1): `SlotsBody` draws `more` as a line of slots in the note's place. "When" is a step lighter while the event is not sure (a day or a time that was not typed, a repeat).

**Its actions**, by the state of the switch:
- off: **Create** · Copy · Calendar. Create opens the Calendar app's editor by the calendar link (core `Cals.link`), on the calendar the row shows.
- on, and the event may be saved (core `Cals.saves`): **Save** · Open in Calendar · Copy · Calendar. Save carries exactly what core's `Cals.write` says; the caption reads "New event. Enter saves it."; where no calendar was named the slot shows the one it goes to, a step lighter.
- on, and it may not (a guess, a repeat, a calendar that takes no events): **Open in Calendar** · Copy · Calendar, and the footer's quiet line says why.
- Where the sentence names a calendar by the word and the list is not allowed, "Allow…" is the last action and the footer says so, until Android's question has been answered once: it opens the window's row (Task 7).

**The Calendar stop** opens the calendars that take events as a list under the row (twelve at most), each with its colour's dot. Tab only moves; Enter opens. Enter on a line runs `Effect.Retype`: the panel writes that calendar into the sentence (core `Sentence.put`), closes the list, puts the pill back on the row, and **does not also run the row**: `retype` sets `filledAt`, so the Enter that chose the line cannot save.

**The executor.** The link goes to the Calendar app alone (`com.google.android.calendar`), and only the one address Booklight builds itself; where no such app takes it, the insert request, as before. `save` is the one place that writes: **it asks for the switch and for both permissions again, takes only a calendar of the list that was read and one that takes events**, writes the event and one reminder row that says "the calendar's own default", and reads nothing. `OverlayActivity.run` lets the panel go with a save whether Shift is held or not: a panel that stayed would save again on the next Enter. Ctrl + digit does not save (`runRow`).

- [ ] **Step 1: The two effects, in `core/src/main/kotlin/io/github/kuscher/booklight/core/Model.kt`.** Two changes.

Find:

```kotlin
    data class InsertEvent(val title: String, val startMillis: Long, val endMillis: Long, val allDay: Boolean, val place: String, val link: String = "") : Effect
```

Make it:

```kotlin
    data class InsertEvent(val title: String, val startMillis: Long, val endMillis: Long, val allDay: Boolean, val place: String, val link: String = "") : Effect
    /**
     * The event itself, written into the calendar with the id [calendar] on this device, with that
     * calendar's own default reminder. Times are epoch milliseconds; [zone] is the device's time zone
     * ("UTC" for an all-day event). Only ever what the row shows, on the user's own Enter, with the
     * option switched on and the permission given: the executor checks the last two again.
     */
    data class SaveEvent(val calendar: Long, val title: String, val startMillis: Long, val endMillis: Long, val allDay: Boolean, val zone: String, val place: String) : Effect
```

Find:

```kotlin
    /** Opens the place where the user allows something, once: `brightness`, `notes`. */
```

Make it:

```kotlin
    /**
     * What stands in the field becomes [text], at once: a line of a row's list that changes what the
     * row reads (a calendar chosen for an event is written into its sentence). The panel does this
     * itself; nothing runs, and the Enter that chose it does not also run the row.
     */
    data class Retype(val text: String) : Effect
    /** Opens the place where the user allows something, once: `brightness`, `notes`, `calendars` (the Booklight window, on that row). */
```

- [ ] **Step 2: The English strings, in `app/src/main/res/values/strings_33.xml`.** One change, at the end of the file.

Find:

```xml
</resources>
```

Make it:

```xml

    <!-- The event row. Its second line of slots: where (strings_11.xml) and on which calendar. -->
    <string name="slot_calendar">Calendar</string>
    <!-- With "Save events without opening Calendar" on: Enter saves; the second action opens the editor instead, and is the first for a guess. -->
    <string name="action_save_event">Save</string>
    <string name="action_open_calendar">Open in Calendar</string>
    <string name="event_caption_save">New event. Enter saves it.</string>
    <!-- The footer once it is saved. %1$s is the calendar's name. -->
    <string name="done_saved_event">Saved to %1$s</string>
    <!-- The word a calendar chosen from the row's list is written into the sentence with: "… to Team". -->
    <string name="event_into">to</string>
    <!-- The footer's quiet line under an event's row: the sentence names a calendar and the list is not allowed; "every Monday"; -->
    <!-- why Enter does not save although the switch is on. %1$s is a calendar's name. -->
    <string name="event_footer_allow">Allow Calendars to put it on a calendar by name</string>
    <string name="event_footer_repeats">A repeat is set in Calendar</string>
    <string name="event_footer_guess">Opens in Calendar: the day or the time is a guess</string>
    <string name="event_footer_closed">Opens in Calendar: %1$s takes no new events</string>
</resources>
```

- [ ] **Step 3: The German strings, in `app/src/main/res/values-de/strings_33.xml`.** One change.

Find:

```xml
</resources>
```

Make it:

```xml

    <string name="slot_calendar">Kalender</string>
    <string name="action_save_event">Speichern</string>
    <string name="action_open_calendar">Im Kalender öffnen</string>
    <string name="event_caption_save">Neuer Termin. Enter speichert ihn.</string>
    <string name="done_saved_event">Gespeichert in %1$s</string>
    <string name="event_into">in</string>
    <string name="event_footer_allow">Kalender erlauben, um ihn mit Namen in einen zu legen</string>
    <string name="event_footer_repeats">Eine Wiederholung stellst du im Kalender ein</string>
    <string name="event_footer_guess">Öffnet sich im Kalender: Tag oder Uhrzeit sind geraten</string>
    <string name="event_footer_closed">Öffnet sich im Kalender: %1$s nimmt keine neuen Termine an</string>
</resources>
```

- [ ] **Step 4: The placeholder, in `app/src/main/res/values/strings_11.xml`.** One change.

Find:

```xml
    <string name="event_hint">When, title @ place</string>
```

Make it:

```xml
    <string name="event_hint">What and when, the way you say it</string>
```

- [ ] **Step 5: The same in German, in `app/src/main/res/values-de/strings_11.xml`.** One change.

Find:

```xml
    <string name="event_hint">Wann, Titel @ Ort</string>
```

Make it:

```xml
    <string name="event_hint">Was und wann, so wie du es sagst</string>
```

- [ ] **Step 6: The line in the list of everything, in `app/src/main/res/values/strings_20.xml`.** One change: the `event` line of the `guide` array. Its example still begins with the keyword, so `./bl debug guide` finds the event's own row.

Find:

```xml
        <item>event|write|event|event|event Fri 3pm Dentist|Event|The calendar’s editor, filled in</item>
```

Make it:

```xml
        <item>event|write|event|event|event dinner with Sam tomorrow 7pm|Event|Typed the way you say it, also without the keyword: add dinner with Sam tomorrow at 7pm</item>
```

- [ ] **Step 7: The same line in German, in `app/src/main/res/values-de/strings_20.xml`.** One change.

Find:

```xml
        <item>event|write|event|event|termin Fr 15 Uhr Zahnarzt|Termin|Der Editor des Kalenders, ausgefüllt</item>
```

Make it:

```xml
        <item>event|write|event|event|termin Abendessen mit Sam morgen 19 Uhr|Termin|So getippt, wie du es sagst, auch ohne das Wort: trag Abendessen mit Sam morgen um 19 Uhr ein</item>
```

- [ ] **Step 8: Write `app/src/main/java/io/github/kuscher/booklight/providers/Events.kt`**

```kotlin
package io.github.kuscher.booklight.providers

import android.content.Context
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Behind
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Cals
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.EventReading
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Sentence
import io.github.kuscher.booklight.core.Slot
import io.github.kuscher.booklight.core.SlotState
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.device.CalendarList
import io.github.kuscher.booklight.scopes.insert
import io.github.kuscher.booklight.scopes.span
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * An event from a sentence: the event's row, the same one under the keyword `event` and for a line typed
 * the way it is said, without a keyword ("Add dinner with Sam tomorrow at 7pm to the Team calendar").
 *
 * The row shows what was understood, read by core's rules at every letter ([Sentence]): when and what
 * on its first line of slots, where and on which calendar on its second. Both lines stand from the first
 * letter, an empty slot as its label and a rule, so nothing in the row ever moves when a place or a
 * calendar is read.
 *
 * Enter opens the Calendar app's editor, filled in, on the calendar that was named (**Create**); the
 * user saves it there. With "Save events without opening Calendar" switched on in the Booklight window,
 * Enter is **Save**: the event is written into the calendar that was named, or the one chosen there, and
 * **Open in Calendar** is the second action. A guess is never saved (core `Cals.saves`): where the day or
 * the time was not read from the sentence, Enter opens the editor whatever the switch says, and the footer
 * says why. With the list of calendars allowed, a stop **Calendar** opens them as a list under the row;
 * Enter on one writes its name into the sentence.
 *
 * Without the keyword the row is offered only for a line that begins with a cue and holds a day or a time
 * (core `Sentence.reads`), and the engine puts it below everything of the device that matches and above
 * the web (its id begins with `SearchEngine.SENTENCE`).
 */
class Events(private val context: Context, private val prefs: Prefs, private val calendars: CalendarList) : Provider {
    override val id = ID

    override suspend fun query(q: Query): List<Result> {
        val now = LocalDateTime.now()
        val read = Sentence.read(q.text, now, calendars.known)
        return if (Sentence.reads(read)) listOf(row(SENTENCE, q.text, read, now)) else emptyList()
    }

    /** The row under the keyword, for whatever is typed there. */
    fun row(text: String): Result = LocalDateTime.now().let { now -> row(SCOPE, text, Sentence.read(text, now, calendars.known), now) }

    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)

    /** The row [id] for [text] as [r] reads it. */
    fun row(id: String, text: String, r: EventReading, now: LocalDateTime): Result {
        val s = prefs.now
        val all = calendars.known
        val e = r.draft
        val zone = ZoneId.systemDefault()
        // Saved from here only with the switch on and the system's leave for it. (While a list is pretended, in a debug build,
        // the row is as with that leave, to be looked at: the executor writes nothing then.)
        val on = s.saveEvents && (calendars.pretends || (calendars.allowed && calendars.writes))
        val target = Cals.target(r.calendar, all, s.eventCalendar)
        val saves = Cals.saves(r, on, target)
        // The calendar the row shows: the one that was named; else, where Enter saves, the one it is saved to. The editor opens on that one too.
        val shown = r.calendar ?: target.takeIf { saves }
        val open = insert(e, Cals.link(e, shown, zone))
        // The calendars an event can be added to, as the lines of the row's own list: each writes its name into the sentence.
        // (The first twelve: a list under a row is never longer than the list of an app's window places. Any other is named by typing.)
        val word = text(R.string.event_into)
        val lines = all.filter { it.writable }.take(LINES).map { c ->
            Action("cal:${c.id}", c.name, Effect.Retype(Sentence.put(text, c.name, word, now, all)), keepOpen = true, symbol = Icon.Swatch(c.color).symbol, more = true, behind = Behind.CALENDAR)
        }
        // The sentence names a calendar and the list is not allowed: the row points at the step, until the system's question has been answered once.
        val points = r.named && !calendars.allowed && !calendars.pretends && !s.calendarsAsked
        val actions = if (text.isBlank()) emptyList() else listOfNotNull(
            // What is written is core's to say (`Cals.write`): exactly what the row shows.
            target?.takeIf { saves }?.let { c ->
                val w = Cals.write(e, c, zone)
                Action("save", text(R.string.action_save_event), Effect.SaveEvent(w.calendar, w.title, w.start, w.end, w.allDay, w.zone, w.place), symbol = "check", done = text(R.string.done_saved_event, c.name))
            },
            // With the switch on this is "Open in Calendar", whether it is the second action or, for a guess, the first.
            if (on) Action("open", text(R.string.action_open_calendar), open, symbol = "open") else Action("create", text(R.string.action_create), open, symbol = "plus"),
            Action("copy", text(R.string.action_copy), Effect.CopyText(listOf(e.title, span(context, e), e.place).filter { it.isNotEmpty() }.joinToString(", "))),
            Action("calendar", text(R.string.slot_calendar), Effect.OpenList(Behind.CALENDAR), keepOpen = true, symbol = "list").takeIf { lines.isNotEmpty() },
            Action("allow", text(R.string.action_allow), Effect.Grant("calendars"), symbol = "lock").takeIf { points },
        ) + lines
        fun slot(label: Int, value: String, guessed: Boolean = false, dot: Int? = null) =
            Slot(text(label), value, if (value.isEmpty()) SlotState.EMPTY else if (guessed) SlotState.GUESSED else SlotState.TYPED, dot)
        val footer = when {
            text.isBlank() -> null
            points -> text(R.string.event_footer_allow)
            e.repeats -> text(R.string.event_footer_repeats)
            on && !saves && !e.sure -> text(R.string.event_footer_guess)
            on && !saves && target == null && r.calendar != null -> text(R.string.event_footer_closed, r.calendar!!.name)
            else -> null
        }
        return Result(
            id = id, provider = ID, kind = Kind.OTHER, title = text(R.string.event_name), icon = Icon.Symbol("event"), score = if (id == SCOPE) 1.0 else SCORE, learnable = false,
            body = Body.Slots(
                text(if (saves) R.string.event_caption_save else R.string.event_caption),
                listOf(slot(R.string.slot_when, span(context, e), guessed = !e.sure), slot(R.string.slot_title, e.title)),
                // (A calendar that was not typed, the one a saved event goes to, is a step lighter.)
                more = listOfNotNull(slot(R.string.slot_where, e.place), slot(R.string.slot_calendar, shown?.name.orEmpty(), guessed = r.calendar == null, dot = shown?.color).takeIf { all.isNotEmpty() }),
                completes = r.rest, footer = footer,
            ),
            actions = actions,
        )
    }

    companion object {
        const val ID = "events"
        /** The row under the keyword `event`. */
        const val SCOPE = "jot:event"
        /** The row of a line typed without the keyword: the engine places it by this id, not by its score. */
        const val SENTENCE = SearchEngine.SENTENCE + "event"
        private const val SCORE = 0.5
        /** The most calendars the row's list shows. */
        private const val LINES = 12
    }
}
```

- [ ] **Step 9: The keyword asks it, in `app/src/main/java/io/github/kuscher/booklight/scopes/Jot.kt`.** Four changes: a range of days in `span`; `insert` at the top level, with the link; `JotScope.insert`; and `EventScope`.

Find:

```kotlin
    e.allDay -> context.getString(R.string.jot_all_day, day(context, e.start))
```

Make it:

```kotlin
    // (Several days: the first and the last, "Wed 14 Oct – Fri 16 Oct, all day". The end that is kept is the midnight after the last day.)
    e.allDay -> context.getString(R.string.jot_all_day, if (e.end.isAfter(e.start.plusDays(1))) "${day(context, e.start)} – ${day(context, e.end.minusDays(1))}" else day(context, e.start))
```

Find:

```kotlin
}

/**
 * The scopes that jot something down: a mail, a note, an event, a reminder, a timer, an alarm,
```

Make it:

```kotlin
}

/**
 * The calendar's editor for [e], filled in; with [link], the calendar link that is sent to the Calendar app first (core `Cals.link`).
 * An all-day event is midnight to midnight in UTC, whatever the device's zone: that is how calendars store one.
 */
internal fun insert(e: EventDraft, link: String = ""): Effect.InsertEvent {
    val zone = if (e.allDay) ZoneOffset.UTC else ZoneId.systemDefault()
    return Effect.InsertEvent(e.title, e.start.atZone(zone).toInstant().toEpochMilli(), e.end.atZone(zone).toInstant().toEpochMilli(), e.allDay, e.place, link)
}

/**
 * The scopes that jot something down: a mail, a note, an event, a reminder, a timer, an alarm,
```

Find:

```kotlin
    protected fun insert(e: EventDraft): Effect {
        // An all-day event is midnight to midnight in UTC, whatever the device's zone: that is how calendars store one.
        val zone = if (e.allDay) ZoneOffset.UTC else ZoneId.systemDefault()
        return Effect.InsertEvent(e.title, e.start.atZone(zone).toInstant().toEpochMilli(), e.end.atZone(zone).toInstant().toEpochMilli(), e.allDay, e.place)
    }
```

Make it:

```kotlin
    protected fun insert(e: EventDraft): Effect = io.github.kuscher.booklight.scopes.insert(e)
```

Find:

```kotlin
/** `event Fri 3pm Dentist @ Main St`: the calendar's editor, filled in. */
class EventScope(context: Context) : JotScope(context, "event", R.string.event_keys, R.string.event_name, R.string.event_hint, R.string.event_about, "event") {
    override suspend fun rows(arg: String): List<Result> {
        val e = Jot.event(arg, LocalDateTime.now())
        val actions = if (arg.isBlank()) emptyList() else listOf(
            Action("create", text(R.string.action_create), insert(e), symbol = "plus"),
            copy(listOf(e.title, span(e), e.place).filter { it.isNotEmpty() }.joinToString(", ")),
        )
        return listOf(preview(
            text(R.string.event_caption),
            listOfNotNull(slot(R.string.slot_when, span(e), guessed = !e.dayGiven || !e.timeGiven), slot(R.string.slot_title, e.title), slot(R.string.slot_where, e.place).takeIf { e.place.isNotEmpty() }),
            null, actions,
        ))
    }
```

Make it:

```kotlin
/**
 * `event Fri 3pm Dentist @ Main St`, or the event the way it is said: the calendar's editor, filled in, or the event saved.
 * Its row is `providers/Events.kt`'s, the same one a sentence typed without the keyword gets.
 */
class EventScope(context: Context, private val events: io.github.kuscher.booklight.providers.Events) : JotScope(context, "event", R.string.event_keys, R.string.event_name, R.string.event_hint, R.string.event_about, "event") {
    override suspend fun rows(arg: String): List<Result> = listOf(events.row(arg))
```

- [ ] **Step 10: The registry hands it on, in `app/src/main/java/io/github/kuscher/booklight/scopes/Scopes.kt`.** Two changes.

Find:

```kotlin
    private val installed: (String) -> io.github.kuscher.booklight.providers.AppsProvider.Installed? = { null },
```

Make it:

```kotlin
    private val installed: (String) -> io.github.kuscher.booklight.providers.AppsProvider.Installed? = { null },
    /** An event's row, for the keyword `event`: the same one a sentence typed without the keyword gets. */
    events: io.github.kuscher.booklight.providers.Events,
```

Find:

```kotlin
        MailScope(context), NoteScope(context, notes), NotesScope(context, notes), todo, PinScope(context, pinned), EventScope(context), RemindScope(context), TimerScope(context), AlarmScope(context),
```

Make it:

```kotlin
        MailScope(context), NoteScope(context, notes), NotesScope(context, notes), todo, PinScope(context, pinned), EventScope(context, events), RemindScope(context), TimerScope(context), AlarmScope(context),
```

- [ ] **Step 11: The provider is registered, in `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`.** Two changes: the property; and `onCreate`, where it is made before the scopes and stands among the providers before the web's.

Find:

```kotlin
    /** What Spotify has by a name, for `play`. With a key of the user's own it asks Spotify: network code like [flights]'. */
```

Make it:

```kotlin
    /** An event typed as a sentence: its row under the keyword `event`, and for a line typed without it. */
    lateinit var events: io.github.kuscher.booklight.providers.Events private set
    /** What Spotify has by a name, for `play`. With a key of the user's own it asks Spotify: network code like [flights]'. */
```

Find:

```kotlin
        scopes = Scopes(this, prefs, dials, notes, { web.search(it) }, pages, keys, onDevice, guide, others = { commands.scopes(it) }, pinned = { pinned.value }, flights = flights, installed = { apps.installed(it) })
        providers = listOf(apps, CalcProvider(this, prefs), Answers(this), pages, CommandsProvider(this), dials, User(this, prefs, apps), commands, keys, flights, web)
```

Make it:

```kotlin
        events = io.github.kuscher.booklight.providers.Events(this, prefs, calendars)
        scopes = Scopes(this, prefs, dials, notes, { web.search(it) }, pages, keys, onDevice, guide, others = { commands.scopes(it) }, pinned = { pinned.value }, flights = flights, installed = { apps.installed(it) }, events = events)
        providers = listOf(apps, CalcProvider(this, prefs), Answers(this), pages, CommandsProvider(this), dials, User(this, prefs, apps), commands, keys, flights, events, web)
```

- [ ] **Step 12: A sentence's row is the `event` line's, in `app/src/main/java/io/github/kuscher/booklight/Guide.kt`.** One change, in `used`.

Find:

```kotlin
                "flights" -> "flight"
```

Make it:

```kotlin
                "flights" -> "flight"
                // An event typed as a sentence, without the keyword: the `event` line's all the same.
                io.github.kuscher.booklight.providers.Events.ID -> "event"
```

- [ ] **Step 13: The link and the save, in `app/src/main/java/io/github/kuscher/booklight/Executor.kt`.** Six changes: two imports; `InsertEvent` and `SaveEvent`; `Retype` among what the panel does itself; `save`; the Calendar app's package.

Find:

```kotlin
import android.content.Context
```

Make it:

```kotlin
import android.content.ContentValues
import android.content.Context
```

Find:

```kotlin
import io.github.kuscher.booklight.core.Effect
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.Cals
import io.github.kuscher.booklight.core.Effect
```

Find:

```kotlin
            is Effect.InsertEvent -> start(Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, effect.title)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, effect.startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, effect.endMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, effect.allDay)
                .apply { if (effect.place.isNotBlank()) putExtra(CalendarContract.Events.EVENT_LOCATION, effect.place) })
```

Make it:

```kotlin
            is Effect.InsertEvent -> {
                val insert = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, effect.title)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, effect.startMillis)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, effect.endMillis)
                    .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, effect.allDay)
                    .apply { if (effect.place.isNotBlank()) putExtra(CalendarContract.Events.EVENT_LOCATION, effect.place) }
                // A calendar link says which calendar, which the request above cannot (the Calendar app ignores a calendar's id
                // in it): the link goes to the Calendar app first, and to that app alone. Only the one address Booklight builds
                // itself (core `Cals.link`). Where no Calendar app takes it: the request, without a calendar.
                if (!effect.link.startsWith(Cals.TEMPLATE)) start(insert)
                else try { start(Intent(Intent.ACTION_VIEW, effect.link.toUri()).setPackage(CALENDAR)) } catch (_: ActivityNotFoundException) { start(insert) }
            }
            is Effect.SaveEvent -> return save(effect)
```

Find:

```kotlin
            is Effect.EnterScope, is Effect.OpenList, is Effect.Type, is Effect.Ask, is Effect.Unsuggest -> return false     // the panel does these itself
```

Make it:

```kotlin
            is Effect.EnterScope, is Effect.OpenList, is Effect.Type, is Effect.Retype, is Effect.Ask, is Effect.Unsuggest -> return false     // the panel does these itself
```

Find:

```kotlin
    }

    /**
     * One of an app's own pages in Settings: the notification page takes the package as an extra, the
```

Make it:

```kotlin
    }

    /**
     * Writes one event into a calendar: the event the row showed, on the user's Enter, and nothing else. Only with "Save
     * events without opening Calendar" switched on and the system's leave to add events, both asked again here whoever made
     * the effect; and only into a calendar of the list that was read, one that takes events. Two rows are written: the event
     * (its title, start and end, all day or not, the time zone, the place), and its reminder, which says "this calendar's
     * own default" in the provider's own words and no time of Booklight's. Nothing is read: not the event that was written,
     * and no other. Booklight keeps no trace of it, and never changes or removes an event.
     */
    private fun save(e: Effect.SaveEvent): Boolean {
        val calendars = app.calendars
        if (!app.prefs.now.saveEvents || !calendars.allowed || !calendars.writes || calendars.pretends) return false
        if (e.title.isBlank() || e.endMillis <= e.startMillis || calendars.known.none { it.id == e.calendar && it.writable }) return false
        val event = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, e.calendar)
            put(CalendarContract.Events.TITLE, e.title)
            put(CalendarContract.Events.DTSTART, e.startMillis)
            put(CalendarContract.Events.DTEND, e.endMillis)
            put(CalendarContract.Events.ALL_DAY, if (e.allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, e.zone)
            if (e.place.isNotBlank()) put(CalendarContract.Events.EVENT_LOCATION, e.place)
        }
        val id = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, event)?.lastPathSegment?.toLongOrNull() ?: return false
        // The reminder. Its failing takes nothing from the event, which is saved: the footer still says so.
        runCatching {
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, id)
                put(CalendarContract.Reminders.MINUTES, CalendarContract.Reminders.MINUTES_DEFAULT)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_DEFAULT)
            })
        }
        return true
    }

    /**
     * One of an app's own pages in Settings: the notification page takes the package as an extra, the
```

Find:

```kotlin
        /** A number as an address takes it: digits, with a + in front or without. Anything else is not put into one. */
```

Make it:

```kotlin
        /** The Calendar app: a calendar link is sent to it and to no other app. (No `<queries>` line: it is only ever started, never asked about.) */
        const val CALENDAR = "com.google.android.calendar"
        /** A number as an address takes it: digits, with a + in front or without. Anything else is not put into one. */
```

- [ ] **Step 14: A second line of slots, and the dot, in `app/src/main/java/io/github/kuscher/booklight/overlay/Bodies.kt`.** Four changes: an import; `SlotsBody`, `SlotLine` and `DOT`.

Find:

```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
```

Make it:

```kotlin
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
```

Find:

```kotlin
 * as you type, then a line for the long part (a message). What was typed is in full ink, what was
 * guessed a step lighter, and an empty slot is its label and a short rule. The values change in
 * the same frame as the typing: they mirror the field and are never animated.
```

Make it:

```kotlin
 * as you type, then a line for the long part (a message), or a second line of slots in its place
 * (an event's place and calendar). What was typed is in full ink, what was guessed a step lighter,
 * and an empty slot is its label and a short rule. The values change in the same frame as the
 * typing: they mirror the field and are never animated. A row's lines are the same lines whatever
 * its slots hold: when a value comes later (the device's model has split the sentence), it changes
 * where it stands and nothing moves.
```

Find:

```kotlin
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            b.slots.forEachIndexed { i, s ->
                // The first slots keep to their share; the last takes what is left, so a growing value never pushes its neighbour away.
                val last = i == b.slots.lastIndex
                Row(if (last) Modifier.weight(1f, fill = false) else Modifier.widthIn(max = if (i == 0) 240.dp else 170.dp)) {
                    // Label and value sit on one baseline.
                    Text(s.label.uppercase(), color = ink.copy(alpha = ink.alpha * SECOND), style = HINT, maxLines = 1, modifier = Modifier.alignByBaseline().padding(end = 7.dp))
                    if (s.state == SlotState.EMPTY) Text("–", color = ink.copy(alpha = ink.alpha * 0.40f), style = VALUE, modifier = Modifier.alignByBaseline())
                    else Text(s.value, color = ink.copy(alpha = ink.alpha * if (s.state == SlotState.GUESSED) SECOND else 1f), style = VALUE, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alignByBaseline())
```

Make it:

```kotlin
        SlotLine(b.slots, ink)
        if (b.more.isNotEmpty()) SlotLine(b.more, ink)
        b.note?.let { Text(it, color = ink, style = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/** One line of slots. A slot whose value has a colour of its own (a calendar's) shows it as a dot before the value: a sample, as a colour's swatch is. */
@Composable
private fun SlotLine(slots: List<io.github.kuscher.booklight.core.Slot>, ink: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        slots.forEachIndexed { i, s ->
            // The first slots keep to their share; the last takes what is left, so a growing value never pushes its neighbour away.
            val last = i == slots.lastIndex
            Row(if (last) Modifier.weight(1f, fill = false) else Modifier.widthIn(max = if (i == 0) 240.dp else 170.dp)) {
                // Label and value sit on one baseline.
                Text(s.label.uppercase(), color = ink.copy(alpha = ink.alpha * SECOND), style = HINT, maxLines = 1, modifier = Modifier.alignByBaseline().padding(end = 7.dp))
                if (s.state == SlotState.EMPTY) Text("–", color = ink.copy(alpha = ink.alpha * 0.40f), style = VALUE, modifier = Modifier.alignByBaseline())
                else {
                    s.dot?.let { Box(Modifier.align(Alignment.CenterVertically).padding(end = 6.dp).size(DOT).clip(CircleShape).background(Color(it))) }
                    Text(s.value, color = ink.copy(alpha = ink.alpha * if (s.state == SlotState.GUESSED) SECOND else 1f), style = VALUE, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alignByBaseline())
```

Find:

```kotlin
        }
        b.note?.let { Text(it, color = ink, style = SMALL, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}
```

Make it:

```kotlin
        }
    }
}

/** A calendar's own colour, as a dot: before its name in a slot, and in the icon column of its line in the list of calendars. */
internal val DOT = 10.dp
```

- [ ] **Step 15: A stop with a list of its own, in `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`.** Six changes: a list's line with a dot; and in `ResultRow`, wherever it said Window and meant such a stop.

Find:

```kotlin
        // The glyph sits in the icon column, the name on the title's edge.
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { Icon(Symbols.of((r.icon as? RowIcon.Symbol)?.name ?: "app"), null, Modifier.size(18.dp), tint = ink) }
```

Make it:

```kotlin
        // The glyph sits in the icon column, the name on the title's edge. (A line that is told by a colour, a calendar's, has a dot of it there.)
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
            val dot = r.icon as? RowIcon.Swatch
            if (dot != null) Box(Modifier.size(DOT).clip(CircleShape).background(Color(dot.argb)))
            else Icon(Symbols.of((r.icon as? RowIcon.Symbol)?.name ?: "app"), null, Modifier.size(18.dp), tint = ink)
        }
```

Find:

```kotlin
        val shown = r.actions.filter { !it.more }
```

Make it:

```kotlin
        // (An event's Calendar is such a stop too: what is said of Window here holds for any stop that opens a list of its own.)
        val shown = r.actions.filter { !it.more }
```

Find:

```kotlin
        val place = stops.firstOrNull { r.actions.getOrNull(it)?.let { a -> a.more && a.behind == Behind.WINDOW } == true }?.takeIf { window >= 0 }
```

Make it:

```kotlin
        val place = stops.firstOrNull { r.actions.getOrNull(it)?.let { a -> a.more && a.behind != Behind.ARROW } == true }?.takeIf { window >= 0 }
```

Find, in one long line:

```kotlin
if (opened == Behind.WINDOW) a.copy(label
```

Make it:

```kotlin
if (opened != null && opened != Behind.ARROW) a.copy(label
```

Find:

```kotlin
                    onArm = { onArm(full(it)) }, onRun = { if (more && tenth == null && it == shown.size && opened != Behind.WINDOW) onToggle() else onAction(full(it)) },
```

Make it:

```kotlin
                    onArm = { onArm(full(it)) }, onRun = { if (more && tenth == null && it == shown.size && (opened == null || opened == Behind.ARROW)) onToggle() else onAction(full(it)) },
```

Find, in one long line:

```kotlin
== Behind.WINDOW) Text(stringResource(R.string.action_window
```

Make it:

```kotlin
!= Behind.ARROW) Text(r.actions.firstOrNull { (it.effect as? Effect.OpenList)?.behind == opened }?.label.orEmpty(
```

- [ ] **Step 16: A line that writes into the field, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Three changes: the dot of a list's line in `actionRows`; `Retype` in `enter`, with `retype`; and `runRow`.

Find:

```kotlin
            id = "act:${r.id}:${a.id}", provider = r.provider, kind = Kind.ACTION, title = a.label, icon = io.github.kuscher.booklight.core.Icon.Symbol(a.symbol),
```

Make it:

```kotlin
            // (A line that is told from the others by a colour, a calendar's, is marked by a dot of it.)
            id = "act:${r.id}:${a.id}", provider = r.provider, kind = Kind.ACTION, title = a.label, icon = io.github.kuscher.booklight.core.Icon.Swatch.of(a.symbol) ?: io.github.kuscher.booklight.core.Icon.Symbol(a.symbol),
```

Find:

```kotlin
        // "Try it": Booklight types the example.
        (a.effect as? Effect.Type)?.let { typeOut(it.text); return }
        run(r, a)
    }

    private fun into(e: Effect.EnterScope, /** The row and the action that were run, where they are not the selected row's (Ctrl + digit). */ ran: Pair<Result, Action>? = chosen()) {
```

Make it:

```kotlin
        // "Try it": Booklight types the example.
        (a.effect as? Effect.Type)?.let { typeOut(it.text); return }
        // A line that changes what the row reads (a calendar chosen for an event): written into the field, and nothing runs.
        (a.effect as? Effect.Retype)?.let { retype(it.text); return }
        run(r, a)
    }

    /**
     * A line of a row's list wrote into the field: the list closes, the text is [text], and the pill is on the row the
     * list was opened under, with that row's own first action armed. The Enter that chose the line does not also run
     * the row: only a new press, a moment later (for an event whose Enter saves, that moment is what keeps one press
     * from being two).
     */
    private fun retype(text: String) {
        val row = opened
        shut()
        row?.let { id -> results.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { selected = it } }
        armed = current?.armed ?: 0
        filledAt = SystemClock.uptimeMillis()
        touched = false
        query = text
        search(keep = true)
    }

    private fun into(e: Effect.EnterScope, /** The row and the action that were run, where they are not the selected row's (Ctrl + digit). */ ran: Pair<Result, Action>? = chosen()) {
```

Find:

```kotlin
        if (a.effect is Effect.Unsuggest) return        // and so is "Don't suggest"
```

Make it:

```kotlin
        if (a.effect is Effect.Unsuggest) return        // and so is "Don't suggest"
        if (a.effect is Effect.SaveEvent) return        // and so is saving an event: written on Enter, on the row that is selected, and by nothing else
```

- [ ] **Step 17: The footer's quiet line, in `app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt`.** One change.

Find:

```kotlin
            val quiet = grid?.cells?.getOrNull(model.cell)?.name?.let { " $it" } ?: (r?.body as? Body.Flight)?.source?.let { " $it" }
```

Make it:

```kotlin
            // (Under an event's row: why Enter opens the calendar instead of saving, or what the row cannot do yet.)
            val quiet = grid?.cells?.getOrNull(model.cell)?.name?.let { " $it" } ?: (r?.body as? Body.Flight)?.source?.let { " $it" } ?: (r?.body as? Body.Slots)?.footer?.let { " $it" }
```

- [ ] **Step 18: A save takes the panel with it, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** One change, in `run`.

Find:

```kotlin
            a.keepOpen || keep -> model.refresh()
```

Make it:

```kotlin
            // (An event that was saved always takes the panel with it, Shift held or not: a panel that stayed would save it again on the next Enter.)
            a.keepOpen || (keep && a.effect !is Effect.SaveEvent) -> model.refresh()
```

- [ ] **Step 19: Look for what must be so**

```bash
grep -rn "CalendarContract" app/src core/src --include='*.kt' | grep -v "booklight/Executor.kt\|booklight/device/CalendarList.kt" || echo CLEAN
grep -c "contentResolver.insert" app/src/main/java/io/github/kuscher/booklight/Executor.kt
grep -rn "contentResolver.query\|contentResolver.update\|contentResolver.delete" app/src/main/java/io/github/kuscher/booklight/Executor.kt app/src/main/java/io/github/kuscher/booklight/providers/Events.kt || echo CLEAN
awk '/private fun save\(e: Effect.SaveEvent\)/ { f = 1 } f && /saveEvents/ { a = NR } f && /contentResolver.insert/ && !b { b = NR } END { print (a > 0 && a < b) ? "ASKED-FIRST" : "NOT-ASKED" }' app/src/main/java/io/github/kuscher/booklight/Executor.kt
for n in slot_calendar action_save_event action_open_calendar event_caption_save done_saved_event event_into event_footer_allow event_footer_repeats event_footer_guess event_footer_closed; do echo "$n $(grep -c "name=\"$n\"" app/src/main/res/values/strings_33.xml) $(grep -c "name=\"$n\"" app/src/main/res/values-de/strings_33.xml)"; done
grep -c "<item>event|write|event|event|" app/src/main/res/values/strings_20.xml app/src/main/res/values-de/strings_20.xml
```

Expected: `CLEAN` (the calendar provider is named in two files: the list's, which reads the calendars, and the executor); `2` (what the executor writes: the event, and its reminder); `CLEAN` (neither the executor nor the row's provider reads, changes or removes anything); `ASKED-FIRST` (the switch is asked for before anything is written); ten lines, each a name followed by `1 1`; two lines ending `:1`.

- [ ] **Step 20: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`.

- [ ] **Step 21: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt app/src/main/java/io/github/kuscher/booklight/Executor.kt app/src/main/java/io/github/kuscher/booklight/Guide.kt app/src/main/java/io/github/kuscher/booklight/overlay/Bodies.kt app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt app/src/main/java/io/github/kuscher/booklight/providers/Events.kt app/src/main/java/io/github/kuscher/booklight/scopes/Jot.kt app/src/main/java/io/github/kuscher/booklight/scopes/Scopes.kt app/src/main/res/values-de/strings_11.xml app/src/main/res/values-de/strings_20.xml app/src/main/res/values-de/strings_33.xml app/src/main/res/values/strings_11.xml app/src/main/res/values/strings_20.xml app/src/main/res/values/strings_33.xml core/src/main/kotlin/io/github/kuscher/booklight/core/Model.kt
git commit -m "An event from a sentence: the event's row, with the keyword and without: its place and its calendar on a second line, Create or Save and Open in Calendar, and the list of calendars under it"
```

### Task 9: The model splits what the rules could not; the calendar's name is completed; a reader hears the row

**Files:**
- Modify: `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/providers/Events.kt`

**Interfaces:**
- Consumes: core `Splits.asks`, `Splits.prompt`, `Splits.merge` (Task 4); `Body.Slots.ask`, `completes`, `more` (Task 6). `ai/OnDevice`: `state: StateFlow<State>`, `ready`, `suspend fun check(): State`, `fun warm()`, `fun ask(prompt: String): Flow<String>`. `providers/Events.kt` as Task 8 left it. In `OverlayModel`: `look()`, `lookUp(row, pause)`, `land(row)`, `answer(row, pause)`, `looked`, `onScreen`, `completion`, `entered(s)`, `search`, `type(text)`, `filledAt`, `onTell`. `Panel.keys`. `ResultRow`'s `described`.
- Produces:
  - `Events(context, prefs, calendars, ai: OnDevice)`; `fun asks(r: Result): Boolean`; `suspend fun answer(row: Result, pause: Boolean): Result?`; `fun forget()`; `fun row(id, text, r, now, asks: Boolean = false): Result`, which sets `Body.Slots.ask`; `Events.REST_MS = 700L`.
  - `OverlayModel.completion` is also the rest of a calendar's name on an event's row; `fun take(): Boolean`.
  - Right at the end of the text takes that rest (`Panel.keys`, and `./bl debug key right`).
  - A preview row's description for a screen reader holds its slots.

**The model, after a rest, through the lookup the panel has.** A flight's row is looked up when the list has landed and the typing has paused, and its answer lands in the row where it stands (`OverlayModel.look`, `lookUp`, `land`). An event's row whose sentence is loose goes the same way: `Events.row` writes the text into the row's `ask`; `look` finds such a row; `Events.answer` waits 0.7 s, asks the model once (`Splits.prompt`), keeps what it said for this text until the panel closes, and gives the row again with core's merged reading, or nothing where the answer did not pass. Every key cancels the lookup (`search` does, as for a flight), so the model is not asked while letters come, and an answer that was cut off is not kept. No model on the device, none ready: the first lookup learns it and no later row asks.

The row that comes back has the same id, the same height and the same lines: `land` puts it in its place, and the words in its slots change where they stand. **An Enter in that moment is not taken**: where the model changed the selected row, `lookUp` sets `filledAt`, the mark the panel already has for "the field was just filled for the user", and Enter counts again a third of a second later. So a press made for the row as it stood cannot save, or open, another reading. The row's slots are said to a screen reader again.

Entering the keyword `event` loads the model, as entering a prompt does; a line that reads as an event is not sent for search suggestions ("Decided here", 17).

**The completion.** `Body.Slots.completes` is what is left of the calendar's name the row read by its start. `OverlayModel.completion`, which the field draws in grey after the text, is that rest on an event's row (only for the rows of what the field holds now, and not after a space); `take()` types it, and Right at the end of the text calls `take()` before it goes along the row's actions.

**A reader.** A preview row said its name and its armed action. It now says its slots too, each with its label, so that what Enter hands over or saves is heard before Enter ("Decided here", 20).

- [ ] **Step 1: The row's provider asks, in `app/src/main/java/io/github/kuscher/booklight/providers/Events.kt`.** Eight changes: to its header, its head (the model, what was said, `query`, `row(text)`, `read`, `asks`, `answer`, `forget`), the signature of `row`, the row's `ask`, three imports and three constants.

Find:

```kotlin
import io.github.kuscher.booklight.core.Slot
import io.github.kuscher.booklight.core.SlotState
```

Make it:

```kotlin
import io.github.kuscher.booklight.ai.OnDevice
import io.github.kuscher.booklight.core.Slot
import io.github.kuscher.booklight.core.SlotState
import io.github.kuscher.booklight.core.Splits
```

Find:

```kotlin
import java.time.ZoneId

/**
 * An event from a sentence: the event's row, the same one under the keyword `event` and for a line typed
```

Make it:

```kotlin
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * An event from a sentence: the event's row, the same one under the keyword `event` and for a line typed
```

Find:

```kotlin
 */
class Events(private val context: Context, private val prefs: Prefs, private val calendars: CalendarList) : Provider {
    override val id = ID
```

Make it:

```kotlin
 *
 * **The device's model helps to split, and nothing waits for it.** The row is the rules' reading in the
 * frame of the key. Only where the rules leave the title in pieces (core `Splits.asks`), and only once
 * the typing has rested for [REST_MS], the model is asked which words are the title, the place and the
 * calendar: once for a text, never for a date. What it says is taken only where every check of core
 * `Splits.merge` passes (every part a piece of what was typed, the day and time read by the parser, no
 * word lost); then the row is given again with the same lines, and the words in its slots change where
 * they stand. No model here, no answer, a slow one: the row stays as the rules read it. What was asked
 * and answered is kept in memory until the panel closes ([forget]) and nowhere else.
 */
class Events(private val context: Context, private val prefs: Prefs, private val calendars: CalendarList, private val ai: OnDevice) : Provider {
    override val id = ID

    /** What the model said in this opening, by the text it was asked about: no text is asked twice. */
    private val said = ConcurrentHashMap<String, String>()
    @Volatile private var warmed = false
```

Find:

```kotlin
        val read = Sentence.read(q.text, now, calendars.known)
        return if (Sentence.reads(read)) listOf(row(SENTENCE, q.text, read, now)) else emptyList()
```

Make it:

```kotlin
        val read = read(q.text, now)
        // (A line that is no event has no row.)
        return if (Sentence.reads(read.second)) listOf(row(SENTENCE, q.text, read.second, now, asks(read, q.text))) else emptyList()
```

Find:

```kotlin
    fun row(text: String): Result = LocalDateTime.now().let { now -> row(SCOPE, text, Sentence.read(text, now, calendars.known), now) }
```

Make it:

```kotlin
    fun row(text: String): Result = LocalDateTime.now().let { now -> read(text, now).let { row(SCOPE, text, it.second, now, asks(it, text)) } }

    /** What is read of [text]: by the rules, and with the model's answer for this very text where one is kept and passes. First: whether the model has been asked about it. */
    private fun read(text: String, now: LocalDateTime): Pair<Boolean, EventReading> {
        val own = Sentence.read(text, now, calendars.known)
        val answer = said[text] ?: return false to own
        return true to Splits.merge(own, answer, text, now, calendars.known)
    }

    /**
     * Whether the model may be asked about [text]: not asked yet, the rules left its title in pieces (core `Splits.asks`),
     * and a model may be there (one the system has, or one it has not been asked about yet).
     */
    private fun asks(read: Pair<Boolean, EventReading>, text: String): Boolean =
        !read.first && Splits.asks(read.second, text) && ai.state.value.let { it == OnDevice.State.READY || it == OnDevice.State.UNKNOWN }

    /** [r] is an event's row whose sentence the device's model may split better: the row says so itself (`Body.Slots.ask`). */
    fun asks(r: Result): Boolean = r.provider == ID && (r.body as? Body.Slots)?.ask != null

    /**
     * Asks the device's model to split the sentence [row] was made from, after the typing has rested ([pause]), and
     * gives the row again with what it said. Null where nothing changes: no model, no answer, or an answer that did not
     * pass every check. Whoever calls this cancels it when a letter is typed: the model is never asked while the user
     * types, and an answer that was cut off is not kept.
     */
    suspend fun answer(row: Result, pause: Boolean): Result? {
        val text = (row.body as? Body.Slots)?.ask ?: return null
        // (Loaded while the typing rests, where the system is known to have it: the first answer of an opening comes sooner.)
        if (ai.ready && !warmed) { warmed = true; ai.warm() }
        if (pause) delay(REST_MS)
        val state = ai.state.value.let { if (it == OnDevice.State.UNKNOWN) ai.check() else it }
        if (state != OnDevice.State.READY) return null
        val answer = StringBuilder()
        withTimeoutOrNull(ANSWER_MS) { ai.ask(Splits.prompt(text)).collect { answer.append(it) } }
        if (said.size >= KEPT) said.clear()
        said[text] = answer.toString()
        val now = LocalDateTime.now()
        val own = Sentence.read(text, now, calendars.known)
        val merged = Splits.merge(own, answer.toString(), text, now, calendars.known)
        // (The same row, with nothing more to ask: its words change where they stand, or stay.)
        return if (merged === own) null else row(row.id, text, merged, now, asks = false)
    }

    /** The panel closed: nothing of what was typed or answered is kept. */
    fun forget() { said.clear(); warmed = false }
```

Find:

```kotlin
    /** The row [id] for [text] as [r] reads it. */
    fun row(id: String, text: String, r: EventReading, now: LocalDateTime): Result {
```

Make it:

```kotlin
    /** The row [id] for [text] as [r] reads it. [asks]: the device's model may be asked to split this text better. */
    fun row(id: String, text: String, r: EventReading, now: LocalDateTime, asks: Boolean = false): Result {
```

Find:

```kotlin
                completes = r.rest, footer = footer,
```

Make it:

```kotlin
                completes = r.rest, footer = footer, ask = text.takeIf { asks },
```

Find:

```kotlin
        /** The most calendars the row's list shows. */
```

Make it:

```kotlin
        /** How long the typing rests before the device's model is asked about a sentence. */
        const val REST_MS = 700L
        /** How long its answer may take: the first one of a process loads the model (about six seconds on a Googlebook). A later one is no use. */
        private const val ANSWER_MS = 20_000L
        /** How many answers are kept for one opening. */
        private const val KEPT = 32
        /** The most calendars the row's list shows. */
```

- [ ] **Step 2: It is given the model, in `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`.** One change.

Find:

```kotlin
        events = io.github.kuscher.booklight.providers.Events(this, prefs, calendars)
```

Make it:

```kotlin
        events = io.github.kuscher.booklight.providers.Events(this, prefs, calendars, onDevice)
```

- [ ] **Step 3: What was asked is forgotten with the panel, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** One change, in `onDestroy`.

Find:

```kotlin
            (application as BooklightApp).let { it.panelDp = null; it.scopes.forget(); it.onDevice.close() }
```

Make it:

```kotlin
            (application as BooklightApp).let { it.panelDp = null; it.scopes.forget(); it.events.forget(); it.onDevice.close() }
```

- [ ] **Step 4: The lookup, the landing and the completion, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Eight changes: an import; `completion` and `take`; the model loaded in `entered`; no suggestions for an event's line; `answer`; `look`; the landing in `lookUp`; a constant.

Find:

```kotlin
import io.github.kuscher.booklight.providers.FlightsProvider
```

Make it:

```kotlin
import io.github.kuscher.booklight.providers.Events
import io.github.kuscher.booklight.providers.FlightsProvider
```

Find:

```kotlin
    /** The rest of the selected row's name, shown grey after the typed text ("chr" + "ome"). */
    val completion: String? by derivedStateOf {
        val r = current
        // (A flight's row is named "LH 455 · Lufthansa": that is what the number is, not the rest of what was typed.)
        if (chip != null || r == null || r.answer != null || r.body != null || r.nudge != null || r.kind == Kind.WEB || r.kind == Kind.SUGGESTION || r.kind == Kind.SCOPE || r.provider == FlightsProvider.ID) null
        else Matcher.completion(query, r.title)
    }
```

Make it:

```kotlin
    /**
     * The rest of the selected row's name, shown grey after the typed text ("chr" + "ome"). On an event's row: the rest of
     * the calendar's name it read by its start at the very end of the sentence ("… to tea" + "m"), which Right takes ([take]).
     */
    val completion: String? by derivedStateOf {
        val r = current
        // (Only for the rows of what the field holds now, and not after a space: the word is finished then.)
        val name = (r?.body as? Body.Slots)?.completes?.takeIf { onScreen && opened == null && query.isNotEmpty() && !query.last().isWhitespace() }
        // (A flight's row is named "LH 455 · Lufthansa": that is what the number is, not the rest of what was typed.)
        if (name != null) name
        else if (chip != null || r == null || r.answer != null || r.body != null || r.nudge != null || r.kind == Kind.WEB || r.kind == Kind.SUGGESTION || r.kind == Kind.SCOPE || r.provider == FlightsProvider.ID) null
        else Matcher.completion(query, r.title)
    }

    /**
     * Right at the end of the text, on an event's row that shows the rest of a calendar's name in grey: the rest is typed,
     * as if by hand. False where there is nothing of that kind to take: Right then goes along the row's actions, as ever.
     */
    fun take(): Boolean {
        val rest = (current?.body as? Body.Slots)?.completes?.takeIf { it == completion } ?: return false
        type(query + rest)
        return true
    }
```

Find:

```kotlin
        // An app's chip shows only the app: a screen reader is told which action is armed, and what to type.
```

Make it:

```kotlin
        // An event: the model may be asked to split its sentence, so it is loaded as the keyword is entered. Nothing is asked by this.
        if (s.key == EVENT) scope.launch { if (app.onDevice.check() == OnDevice.State.READY) app.onDevice.warm() }
        // An app's chip shows only the app: a screen reader is told which action is armed, and what to type.
```

Find:

```kotlin
            if (local.any { app.engine.leads(it) || it.id == WebProvider.IN_APP }) return@launch
```

Make it:

```kotlin
            // Nor a line that reads as an event: it is the user's own appointment, and goes to the calendar.
            if (local.any { app.engine.leads(it) || it.id == WebProvider.IN_APP || it.id == Events.SENTENCE }) return@launch
```

Find:

```kotlin
    private suspend fun answer(row: Result, pause: Boolean): Result? = if (row.provider == Songs.PROVIDER) app.songs.answer(row.id, pause) else app.flights.answer(row.id, pause)
```

Make it:

```kotlin
    private suspend fun answer(row: Result, pause: Boolean): Result? = when (row.provider) {
        Songs.PROVIDER -> app.songs.answer(row.id, pause)
        // (An event's row: the device's model, asked to split the sentence. Nothing waits for it, and no Enter does.)
        Events.ID -> app.events.answer(row, pause)
        else -> app.flights.answer(row.id, pause)
    }
```

Find:

```kotlin
        (results.firstOrNull { app.flights.waits(it) } ?: results.firstOrNull()?.takeIf { app.songs.asks(it) })?.let { lookUp(it, pause = true) }
```

Make it:

```kotlin
        // An event's row whose sentence the rules left in pieces: the device's model is asked to split it, once the typing
        // has rested. Only here, where a list has landed: never because the user went to the row, and never without the rest.
        (results.firstOrNull { app.flights.waits(it) } ?: results.firstOrNull()?.takeIf { app.songs.asks(it) } ?: results.firstOrNull { app.events.asks(it) })?.let { lookUp(it, pause = true) }
```

Find:

```kotlin
            // What Spotify found is said once to a screen reader: the song, who it is by, and what Enter does with it.
```

Make it:

```kotlin
            // The device's model has changed what an event's row shows, with the pill on it: an Enter in that moment was
            // pressed for the row as it stood, and is not taken (a new press, a moment later, is). And the row is said
            // again to a screen reader, slot by slot.
            if (got.provider == Events.ID && (current?.id == got.id || opened == got.id)) {
                filledAt = SystemClock.uptimeMillis()
                (got.body as? Body.Slots)?.let { b -> onTell((b.slots + b.more).filter { it.value.isNotEmpty() }.joinToString(", ") { "${it.label} ${it.value}" }) }
            }
            // What Spotify found is said once to a screen reader: the song, who it is by, and what Enter does with it.
```

Find:

```kotlin
        /** While the system is still looking at a copy just made: how often Booklight looks again, and how long between. */
```

Make it:

```kotlin
        /** The key of the event scope: its model is loaded as it is entered. */
        private const val EVENT = "event"
        /** While the system is still looking at a copy just made: how often Booklight looks again, and how long between. */
```

- [ ] **Step 5: Right takes the rest of a name, in `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** One change, in `keys`.

Find:

```kotlin
                !atEnd || !bare -> false
```

Make it:

```kotlin
                !atEnd || !bare -> false
                // The rest of a calendar's name, grey after the text of an event's sentence: Right takes it.
                model.take() -> true
```

- [ ] **Step 6: The hook's Right does what the key does, in `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`.** One change.

Find:

```kotlin
                    "right" -> if (!m.moveCell(1, 0) && !m.nudge(1)) { if (m.tabEnters) m.fill() else if (m.opened == null) { if (m.onList != null) m.open() else m.arm(1, wrap = false) } }
```

Make it:

```kotlin
                    "right" -> if (!m.take() && !m.moveCell(1, 0) && !m.nudge(1)) { if (m.tabEnters) m.fill() else if (m.opened == null) { if (m.onList != null) m.open() else m.arm(1, wrap = false) } }
```

- [ ] **Step 7: A preview row says its slots, in `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`.** One change, in `ResultRow`.

Find:

```kotlin
    val described = if (body is Body.Switch) r.title
        else stringResource(R.string.a11y_selected, if (body is Body.Flight) listOfNotNull(r.title, body.headline.ifEmpty { null }, body.badge).joinToString(", ") else r.title, r.actions.getOrNull(armed)?.label ?: kind)
```

Make it:

```kotlin
    // (A preview is said with what it understood, slot by slot: what Enter hands over, or saves, is heard before Enter.)
    val described = if (body is Body.Switch) r.title
        else stringResource(R.string.a11y_selected, when (body) {
            is Body.Flight -> listOfNotNull(r.title, body.headline.ifEmpty { null }, body.badge).joinToString(", ")
            is Body.Slots -> (listOf(r.title) + (body.slots + body.more).filter { it.value.isNotEmpty() }.map { "${it.label} ${it.value}" }).joinToString(", ")
            else -> r.title
        }, r.actions.getOrNull(armed)?.label ?: kind)
```

- [ ] **Step 8: Look for what must be so**

```bash
grep -c "ai.ask(" app/src/main/java/io/github/kuscher/booklight/providers/Events.kt
awk '/suspend fun answer\(row: Result, pause: Boolean\)/ { f = 1 } f && /delay\(REST_MS\)/ && !d { d = NR } f && /ai.ask\(/ && !a { a = NR } END { print (d > 0 && d < a) ? "RESTS-FIRST" : "ASKS-AT-ONCE" }' app/src/main/java/io/github/kuscher/booklight/providers/Events.kt
grep -c "app.events.asks(it)" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
grep -n "URL(\|openConnection\|HttpURLConnection" app/src/main/java/io/github/kuscher/booklight/providers/Events.kt app/src/main/java/io/github/kuscher/booklight/device/CalendarList.kt || echo CLEAN
```

Expected: `1` (the model is asked in one place of this file, `answer`); `RESTS-FIRST` (the rest comes before the question); `1` (the model is asked from one place, where a list has landed); `CLEAN` (no connection).

- [ ] **Step 9: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`.

- [ ] **Step 10: Commit**

```bash
git add app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt app/src/main/java/io/github/kuscher/booklight/providers/Events.kt
git commit -m "An event from a sentence: the device's model splits what the rules could not, after a rest and never for a date; a calendar's name is completed in the field; and the row says what it read to a screen reader"
```

### Task 10: The hooks, and the checks for a device

**Files:**
- Modify: `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/providers/Events.kt`
- Modify: `bl`
- Create: `docs/research/event-sentence.md`

**Interfaces:**
- Consumes: `Events.row(id, text, r, now, asks)`, `Events.ID` (Tasks 8 and 9); `CalendarList.pretend(list)`, `known`, `allowed`, `writes`, `pretends` (Task 7); `Settings.saveEvents`, `eventCalendar`, `calendarsAsked`; core `Sentence.reads`, `Splits.asks`, `EventReading`, `Cal`; `OverlayModel.looked`, `completion`; `DebugReceiver`'s `pref`, `dump`, `describe`, its companion.
- Produces:
  - `Events.rules(text: String): EventReading` and `suspend fun Events.split(text: String): Triple<String, EventReading, EventReading>` (the model's answer as it came, the rules' reading, the reading with the answer: the same object as the rules' where the answer did not pass). For the hooks alone.
  - `./bl debug event TEXT`, `./bl debug event ask TEXT`, `./bl debug calendars`, `./bl debug calendars pretend NAME,NAME,…`, `./bl debug calendars pretend off`, `./bl debug pref save on|off`, `pref eventcal OWNER|none`, `pref calasked yes|no`.
  - `dump` says an event's row with both lines of slots, `rest=`, `footer=`, `+c` for a line of its Calendar list, and `looking` while the model is asked.
  - `docs/research/event-sentence.md`: what a device said before the build, and thirty checks, every answer "not run".

Nobody but the coordinator touches a device, and a check must never save an event into a calendar other people see. So the hooks say what would happen without doing it:

- **`event TEXT`** needs no panel. It prints what the rules read of the text (title, start and end, all day, a guess or not, place, calendar, what is left of a calendar's name, `named`, `cue`, `loose`, the day and time as typed), `offered=` (a line typed without the keyword would be an event's row), `asks=` (the model would be asked), `enter=` with what Enter would do, **said and not done**: "would send" and the calendar link, or "would write" and the values with the calendar's name, and then the row as the dump says it. Nothing is opened and nothing is written.
- **`event ask TEXT`**, with a panel open (the model answers only the app in front): the model is asked as the row asks it, at once; it prints the model's answer as it came, whether it was taken, and the reading with it. Nothing is kept.
- **`calendars`** says what the system allows, and what was read. **`calendars pretend Sam,Team,Holidays!`** puts a list in place of the device's, so the row can be looked at without the permission: the first name is the account's own calendar, a name that ends in `!` takes no new events. While a list is pretended the row offers Save where the switch is on, and `Executor.save` writes nothing (Task 8).
- **`pref save on|off`**, **`pref eventcal OWNER|none`**, **`pref calasked yes|no`** set the three new settings as they are stored.

**These hooks print the names of the user's own calendars, and a link's `src=` is an address. Neither is ever copied into a file of this repo**: a check's answer says "a calendar", "the account's own calendar", "three calendars". The checks' own examples are a pretended list, Sam and Team.

The document has the device's findings first (the core's tests are made from them, and core's comments point at it), then the checks in the form of `docs/research/first-run-last-pass.md`: each with what to type and what to look for, marked [C] for the coordinator over adb or [A] for Alex, every answer "not run". Check 24 is the one that saves: one event, into the account's own calendar, with a title that says it is a test, removed after.

- [ ] **Step 1: Two functions for the hooks, in `app/src/main/java/io/github/kuscher/booklight/providers/Events.kt`.** Two changes: `rules`, and `split`.

Find:

```kotlin
        !read.first && Splits.asks(read.second, text) && ai.state.value.let { it == OnDevice.State.READY || it == OnDevice.State.UNKNOWN }

    /** [r] is an event's row whose sentence the device's model may split better: the row says so itself (`Body.Slots.ask`). */
```

Make it:

```kotlin
        !read.first && Splits.asks(read.second, text) && ai.state.value.let { it == OnDevice.State.READY || it == OnDevice.State.UNKNOWN }

    /** What the rules read of [text], here and now: for the debug hooks. Nothing is noted, nothing asked. */
    fun rules(text: String): EventReading = Sentence.read(text, LocalDateTime.now(), calendars.known)

    /** [r] is an event's row whose sentence the device's model may split better: the row says so itself (`Body.Slots.ask`). */
```

Find:

```kotlin
    }

    /** The panel closed: nothing of what was typed or answered is kept. */
```

Make it:

```kotlin
    }

    /**
     * The model asked about [text] as the row asks it, at once and whatever the rules made of it: what it said, the rules'
     * reading, and the reading with its answer (the same object as the rules' where the answer did not pass). For the debug
     * hook, with a panel open; nothing of it is kept.
     */
    suspend fun split(text: String): Triple<String, EventReading, EventReading> {
        val answer = StringBuilder()
        if (ai.check() == OnDevice.State.READY) withTimeoutOrNull(ANSWER_MS) { ai.ask(Splits.prompt(text)).collect { answer.append(it) } }
        val now = LocalDateTime.now()
        val own = Sentence.read(text, now, calendars.known)
        return Triple(answer.toString(), own, Splits.merge(own, answer.toString(), text, now, calendars.known))
    }

    /** The panel closed: nothing of what was typed or answered is kept. */
```

- [ ] **Step 2: The hooks, in `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`.** Nine changes: the header; `pref`'s three words and what it prints; `event` and `calendars`; `looking` in the dump; `reading` and the slots in `describe`; a list's line marked `+c`; the colours of a pretended list.

Find:

```kotlin
 *   `key enter2` is Enter twice in one turn, as a quick double press; dump says `due=`, `coach=`, `pressed`, `choices`, `ended`.
```

Make it:

```kotlin
 *   `key enter2` is Enter twice in one turn, as a quick double press; dump says `due=`, `coach=`, `pressed`, `choices`, `ended`.
 *   event TEXT (an event from a sentence, without the panel: what the rules read of TEXT, whether a line typed without the keyword would be
 *   offered as an event, whether the model would be asked, the row's actions, and what Enter would do: the calendar link it would send, or the
 *   values it would write. Nothing is opened and nothing is written by it) |
 *   event ask TEXT (with a panel open: the device's model asked about TEXT as the row asks it, at once: its answer as it came, whether it passed
 *   every check, and the reading with it. Nothing is opened, written or kept) |
 *   calendars (what the system allows and how many calendars were read, each with its name: the names are the user's own, and are never copied
 *   into a file of the repo) | calendars pretend NAME,NAME,… (a list in place of the device's, to look at the row without the permission: the first
 *   is the account's own, a name that ends in ! takes no new events; nothing can be written while a list is pretended) | calendars pretend off |
 *   pref save on|off ("Save events without opening Calendar", as stored: the row offers Save only where the system also allows it, or a list is
 *   pretended) | pref eventcal OWNER|none ("New events go to", by the calendar's owner's address) | pref calasked yes|no (the system's question for
 *   the calendars has been answered once: the row no longer points at it); dump says an event's row with both lines of slots (● a colour's dot),
 *   `rest=` (what is left of a calendar's name), `footer=`, a line of its Calendar list as `+c`, `armed=calendar` on that stop, and `looking`
 *   while the model is asked.
```

Find:

```kotlin
                    "sums" -> app.prefs.update { it.copy(showSums = v == "on") }
```

Make it:

```kotlin
                    "sums" -> app.prefs.update { it.copy(showSums = v == "on") }
                    // "Save events without opening Calendar", as it is stored. The row offers Save only where the system allows it too, or a list is pretended.
                    "save" -> app.prefs.update { it.copy(saveEvents = v == "on") }
                    // "New events go to": a calendar by its owner's address (`calendars` does not print it: it is read from the system's own list).
                    "eventcal" -> app.prefs.update { it.copy(eventCalendar = if (v == "none") "" else v) }
                    "calasked" -> app.prefs.update { it.copy(calendarsAsked = v == "yes") }
```

Find, in one long line:

```kotlin
app.prefs.spotifyKey.value.isEmpty()) "none" else "set"}" })
```

Make it:

```kotlin
app.prefs.spotifyKey.value.isEmpty()) "none" else "set"} save=${it.saveEvents} eventcal=${if (it.eventCalendar.isEmpty()) "own" else "chosen"} calasked=${it.calendarsAsked}" })
```

Find:

```kotlin
            // `flight show NAME`: a sample flight's row in the open panel, in the phase its name says (`FlightSamples`: `friday`, `soon`, `air`, `air-late`,
```

Make it:

```kotlin
            // An event from a sentence. `event TEXT`: what the rules read of TEXT here and now, and what Enter on its row would do, said and not done:
            // the calendar link that would be sent, or the values that would be written. `event ask TEXT`: the device's model asked as the row asks
            // it, at once (it answers only while a panel is in front): its answer as it came, and the reading with it.
            "event" -> if (arg.startsWith("ask ")) {
                if (act == null) return out("no panel: the model answers only while the panel is in front")
                val text = arg.removePrefix("ask ").trim()
                app.scope.launch {
                    val t0 = android.os.SystemClock.uptimeMillis()
                    val (said, own, merged) = app.events.split(text)
                    out("asks=${io.github.kuscher.booklight.core.Splits.asks(own, text)} ai=${app.onDevice.state.value} ${android.os.SystemClock.uptimeMillis() - t0} ms | model=${said.replace('\n', ' ').ifEmpty { "(nothing)" }} | " +
                        "${if (merged === own) "not taken: the rules' reading stands" else "taken"} | ${reading(merged)}")
                }
            } else {
                val now = java.time.LocalDateTime.now()
                val read = app.events.rules(arg)
                val row = app.events.row("debug:event", arg, read, now)
                val zone = java.time.ZoneId.systemDefault()
                fun at(ms: Long, z: String = zone.id) = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.of(z)).toLocalDateTime().toString()
                val enter = row.actions.firstOrNull()?.let { a ->
                    when (val e = a.effect) {
                        is io.github.kuscher.booklight.core.Effect.SaveEvent -> "${a.id}: would write calendar=${app.calendars.known.firstOrNull { it.id == e.calendar }?.name} title='${e.title}' start=${at(e.startMillis, e.zone)} end=${at(e.endMillis, e.zone)} allDay=${e.allDay} zone=${e.zone} place='${e.place}' + the calendar's default reminder"
                        is io.github.kuscher.booklight.core.Effect.InsertEvent -> "${a.id}: would send ${e.link} (where no Calendar app takes it: the insert request, title='${e.title}' allDay=${e.allDay} place='${e.place}')"
                        else -> "${a.id}: ${e::class.simpleName}"
                    }
                } ?: "nothing (no actions)"
                out("offered=${io.github.kuscher.booklight.core.Sentence.reads(read)} asks=${io.github.kuscher.booklight.core.Splits.asks(read, arg)} | ${reading(read)} | enter=$enter | ${describe(row)}")
            }
            // What the system allows of the calendars and what was read; and a list in place of the device's, to look at the row without the
            // permission. The names it prints are the user's own: never copied into a file of the repo.
            "calendars" -> {
                val words = arg.split(' ', limit = 2)
                if (words[0] == "pretend") {
                    val names = words.getOrNull(1).orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    app.calendars.pretend(if (names.isEmpty() || names == listOf("off")) null else names.mapIndexed { i, n ->
                        io.github.kuscher.booklight.core.Cal(900L + i, n.removeSuffix("!"), PRETEND_COLORS[i % PRETEND_COLORS.size], "pretend-$i@example.com", primary = i == 0, writable = !n.endsWith("!"))
                    })
                }
                val c = app.calendars
                out("allowed=${c.allowed} writes=${c.writes} pretended=${c.pretends} asked=${app.prefs.now.calendarsAsked} save=${app.prefs.now.saveEvents} count=${c.known.size}: " +
                    c.known.joinToString(" | ") { "${it.name}${if (it.primary) " (own)" else ""}${if (!it.writable) " (no new events)" else ""}" })
            }
            // `flight show NAME`: a sample flight's row in the open panel, in the phase its name says (`FlightSamples`: `friday`, `soon`, `air`, `air-late`,
```

Find, in one long line:

```kotlin
thinking" else ""} selected=${m.selected}
```

Make it:

```kotlin
thinking" else ""}${if (m.looked) " looking" else ""} selected=${m.selected}
```

Find:

```kotlin
        /**
         * The frames the last `first trace N` took, each as its two clocks and the rest of its line. Kept by the receiver
```

Make it:

```kotlin
        /** The colours of a pretended list of calendars (`calendars pretend`): four that a calendar may have. */
        private val PRETEND_COLORS = listOf(0xFF3F51B5.toInt(), 0xFF0B8043.toInt(), 0xFFF4511E.toInt(), 0xFF8E24AA.toInt())
        /**
         * The frames the last `first trace N` took, each as its two clocks and the rest of its line. Kept by the receiver
```

Find:

```kotlin
    private fun describe(r: io.github.kuscher.booklight.core.Result): String {
```

Make it:

```kotlin
    /** An event's reading in a line: what the row's slots are made from. */
    private fun reading(r: io.github.kuscher.booklight.core.EventReading): String = r.draft.let { e ->
        "title='${e.title}' when=${e.start}..${e.end}${if (e.allDay) " all day" else ""}${if (e.sure) "" else " (a guess${if (e.repeats) ": repeats" else ""})"} place='${e.place}' calendar=${r.calendar?.name ?: "none"}" +
            "${r.rest?.let { " rest='$it'" } ?: ""}${if (r.named) " named" else ""}${if (r.cue) " cue" else ""}${if (r.loose) " loose" else ""} said='${r.said}'"
    }

    private fun describe(r: io.github.kuscher.booklight.core.Result): String {
```

Find:

```kotlin
            is io.github.kuscher.booklight.core.Body.Slots -> " {" + listOfNotNull(b.caption).plus(b.slots.map { "${it.label}=${it.value}${if (it.state == io.github.kuscher.booklight.core.SlotState.GUESSED) "?" else ""}" }).plus(listOfNotNull(b.note)).joinToString("; ") + "}"
```

Make it:

```kotlin
            // (A slot whose value has a colour's dot before it is marked ●; the second line of slots follows a bar; `rest=` is what the field shows in grey.)
            is io.github.kuscher.booklight.core.Body.Slots -> {
                fun slot(s: io.github.kuscher.booklight.core.Slot) = "${s.label}=${if (s.dot != null) "●" else ""}${s.value}${if (s.state == io.github.kuscher.booklight.core.SlotState.GUESSED) "?" else ""}"
                " {" + listOfNotNull(b.caption).plus(b.slots.map(::slot)).plus(if (b.more.isEmpty()) emptyList() else listOf("| " + b.more.joinToString("; ", transform = ::slot))).plus(listOfNotNull(b.note, b.completes?.let { "rest=$it" }, b.footer?.let { "footer=$it" })).joinToString("; ") + "}"
            }
```

Find:

```kotlin
        // (A line behind the arrow is marked +, one behind Window +w.)
        val acts = r.actions.mapIndexed { i, a -> (if (i == r.armed) "*" else "") + a.id + (if (!a.more) "" else if (a.behind == io.github.kuscher.booklight.core.Behind.WINDOW) "+w" else "+") + (if (a.off) "(off)" else "") }.joinToString(",")
```

Make it:

```kotlin
        // (A line behind the arrow is marked +, one behind Window +w, one behind an event's Calendar +c.)
        val acts = r.actions.mapIndexed { i, a -> (if (i == r.armed) "*" else "") + a.id + (if (!a.more) "" else when (a.behind) { io.github.kuscher.booklight.core.Behind.WINDOW -> "+w"; io.github.kuscher.booklight.core.Behind.CALENDAR -> "+c"; else -> "+" }) + (if (a.off) "(off)" else "") }.joinToString(",")
```

- [ ] **Step 3: The script's header, in `bl`.** One change.

Find:

```bash
#                            attached device takes: run it before a release)
```

Make it:

```bash
#                            attached device takes: run it before a release),
#                            event TEXT (an event from a sentence: what is read of TEXT and what Enter would
#                            send or write, said and not done), event ask TEXT (the device's model asked as
#                            the row asks it; with a panel open), calendars [pretend NAME,NAME,…|pretend off]
#                            (what the system allows and what was read; or a list in place of the device's),
#                            pref save on|off, pref eventcal OWNER|none, pref calasked yes|no
```

- [ ] **Step 4: Write `docs/research/event-sentence.md`**

```markdown
# An event from a sentence: what a device said, and the checks for one

*Two things in one place. First what was tried on a Googlebook before anything was built (5 October 2026), which
the core's tests are made from. Then the checks for the build
(`docs/superpowers/plans/2026-10-05-event-from-a-sentence.md`): whoever runs one writes its answer under it, with
the date and "the Lenovo Googlebook" or "the HP Googlebook". Until then an answer reads "not run".*

The design is `docs/design/event-sentence/design.md`; its §11, "Settled for the build", wins.

# What the device said

## The model as a splitter

Sixteen sentences, English and German, each sent to the model on the device through `OnDevice.ask` (temperature
0.2, top-k 10) after this instruction, which the build sends word for word (core `Splits.prompt`):

    Split the request into its parts. Answer with one line of JSON and nothing else:
    {"title":"","when":"","place":"","calendar":""}
    Rules: copy the words exactly from the request. "when" is every word about the day and the time. "calendar" is only the name of the calendar. "title" is what the event is, without add, schedule or put, and without the time, the place and the calendar. Use "" for a part that is not there.
    Request: <the sentence>

Every answer came inside a code fence, on one line. An answer took 1.1 to 2.3 s; the first one of a process 6.3 s
(the model loading). The model answers only while Booklight's panel is the window in front. "Now" was Monday 5
October 2026, about twenty past four. The people in the sentences are called Sam here and the calendars Team.

| # | The sentence | The parser of 3.1, under `event` | The model |
| --- | --- | --- | --- |
| 1 | Add dinner with Sam tomorrow at 7pm to Team calendar | nothing read (the whole sentence as the title) | title, when, calendar: right |
| 2 | Schedule dentist appointment next Tuesday at 3:30pm at Dr. Sam office | nothing read | right, with the place |
| 3 | Lunch with Sam on Friday at noon at Cafe Luna, add to Team calendar | nothing read | right, with the place and the calendar |
| 4 | Team offsite Oct 14 to Oct 16 in Lisbon | nothing read | right; the parser could not read a range of days |
| 5 | Call mom Sunday 10am | right | the same |
| 6 | Put yoga class every Monday 6pm in my Team calendar | nothing read | right; the parser could not read "every …" |
| 7 | Flight to Frankfurt on the 12th at 6:45 in the morning | nothing read | **a fault**: the place "Frankfurt", taken out of the title; the parser could not read "the 12th … in the morning" |
| 8 | Add Team bowling night to the Team calendar Saturday 8pm | the time right, the title the whole rest | right |
| 9 | meeting with the team about Q3 plan tmrw 9-10am room 4B | nothing read | right, with the place; the parser could not read "tmrw" |
| 10 | Birthday party for Sam on November 3rd all day | nothing read | **broken JSON** (three quotes at its end), and **a fault**: "for Sam" dropped from the title |
| 11 | Abendessen mit Sam morgen um 19 Uhr in den Team-Kalender eintragen | nothing read | **a fault**: the calendar given as the place too |
| 12 | Zahnarzt nächsten Dienstag 15:30 Uhr | right | the same |
| 13 | Trag Mittagessen mit Sam am Freitag um 12 im Café Luna in den Teamkalender ein | nothing read | **a fault**: the verb left in the title ("Trag Mittagessen mit Sam") |
| 14 | Elternabend am 14.10. um 19:30 in der Schule | nothing read | **broken JSON** (the same), the dot of the date dropped from its "when", the place with its "in der" |
| 15 | Übermorgen 8 Uhr Friseur | right | the same |
| 16 | Geburtstag von Oma am Samstag ganztägig | nothing read | right; the parser could not read „ganztägig“ |

In every well-formed answer each part was a piece of what was typed: nothing was invented. The parser of 3.1 alone
had 3 of 16 right; the model's split with that parser reading its "when", 9 of 16; what still failed was the
parser. In this build the rules alone have 10 of 16 right from end to end (core `SentenceTest.theSixteenSentences`),
and with the model's split all 16 have their title, place and calendar (`SplitTest.theSixteenAnswers`: 15 answers
are taken, and the one that lost two words is dropped, where the rules were right anyway). "every Monday" is read
as the next Monday and shown as a guess.

Structured output and a system prompt are not available on that model (`device-findings.md`, "The on-device
model"); `on-device-ai.md` has its allowance: it is never asked while the user types.

## The calendar on a Googlebook

- The Calendar app (`com.google.android.calendar`) takes both the insert request and a link to
  `https://calendar.google.com/calendar/render?…`, and opens the web calendar in a window of its own.
- The insert request (`ACTION_INSERT` on the events' address, as Booklight sent it up to 3.1): the title, the
  begin and the end arrive. **A calendar's id in it is ignored**: the editor opens on the account's own calendar.
- **A link that names the calendar is honoured**: `…/render?action=TEMPLATE&text=<title>&dates=<start>/<end>&src=<id>`,
  sent to the Calendar app, opened the full editor with the title, the time and that calendar chosen. `dates` in
  UTC as `yyyyMMdd'T'HHmmss'Z'` landed at the right moment; an all-day event is `yyyyMMdd/yyyyMMdd`, the end being
  the day after the last day; `location=` is the link's parameter for the place (documented for these links; not
  tried). `src` is what the system's calendar list calls the calendar's owner account.
- The system's list of calendars is there and in step: each has a name, a colour, an id, an owner, whether it is
  the account's own, and how much may be done with it.
- Starting an activity of an app that is not visible to Booklight still works; only questions about it are
  filtered. So no `<queries>` line is needed: the link is sent, and where nothing takes it the insert request is.

# The checks

**Who runs what.** A check marked **[C]** is the coordinator's: it is run on the test device by adb and the debug
hooks. A check marked **[A]** is Alex's own: it needs the system's own question answered by a hand, another app's
window, the HP Googlebook, or a person's judgement.

**Before the first check**

- Choose the device by its model (`adb devices -l`), never by its place in the list; set `ANDROID_SERIAL`.
- Close the Booklight window, then `./bl install`. `./bl idle` before every `./bl open`: the panel takes the keyboard.
- After `./bl stop` the hooks are not heard until a panel has been opened once: `./bl open stay` first.
- Never `uiautomator dump`. No sign-in is made for a check. A capture of the screen shows real windows: none is
  kept or committed. `./bl shot NAME` is the panel's own window, `./bl wshot NAME` the Booklight window's.
- **This file is public.** `./bl debug calendars`, `./bl debug event …` and the dump print the names of the
  user's own calendars, and a link's `src=` is an address: an answer says "a calendar", "the account's own
  calendar", "three calendars", never a name or an address. The examples are Sam and Team.
- **Nothing is saved into a calendar by a check, but for check 24**, which saves one event into the account's own
  calendar, with a title that says it is a test, and removes it after. Never into a calendar that is shared with
  other people. Everything else that has to do with saving is read from `./bl debug event TEXT`, which says what
  Enter would write and writes nothing, or is looked at with a pretended list, where nothing can be written.
- **The Calendar app's editor is another app's window.** Where a check opens it (7, 23), it is read from a
  capture cut to that window, nothing is typed or clicked in it, and it is closed without saving.
- **Real keys** go only into Booklight's own focused window (`./bl idle` says which has the focus), and never a
  letter and Enter back to back: with the switch on, Enter on an event's row writes.
- **The hooks.** `./bl debug event TEXT`: what the rules read of TEXT (`title=`, `when=`, `place=`, `calendar=`,
  `rest=` for the start of a calendar's name, `named`, `cue`, `loose`, `said=`), `offered=` (a line typed without
  the keyword would be an event's row), `asks=` (the model would be asked), `enter=` (what Enter would do: "would
  send" a link, or "would write" the values), and the row as the dump says it. It needs no panel.
  `./bl debug event ask TEXT`: the model asked as the row asks it, at once; with a panel open.
  `./bl debug calendars`: `allowed=`, `writes=`, `pretended=`, `asked=`, `save=`, `count=` and the calendars.
  `./bl debug calendars pretend Sam,Team,Team trips,Holidays!` puts that list in place of the device's (the first
  is the account's own; a name that ends in `!` takes no new events); `./bl debug calendars pretend off` ends it.
  `./bl debug pref save on|off`, `pref eventcal OWNER|none`, `pref calasked yes|no`. The dump says an event's row
  with both lines of slots (after a bar the second; `●` a colour's dot; `?` a guess), `rest=`, `footer=`, a line
  of its Calendar list as `+c`, `armed=calendar` on that stop, `completion=`, and `looking` while the model is asked.
- **The permission by adb**, where a check says so: `./bl sh pm grant io.github.kuscher.booklight
  android.permission.READ_CALENDAR` (and `WRITE_CALENDAR`), back with `pm revoke`, which also ends Booklight's
  process (`./bl open stay` again after it). The system's own question, answered by a hand, is check 20.
- Write down what `./bl debug pref` and `./bl debug calendars` say before the first check, and put everything
  back after the last one: `calendars pretend off`, `pref save off`, the two permissions as they were.
- **In German.** `adb shell cmd locale set-app-locales io.github.kuscher.booklight --user current --locales de`,
  and back with `--locales ""`.

## Someone who allows nothing

### 1. The panel still opens, and nothing is asked [C]

`./bl open stay`, `./bl debug dump`, `./bl shot plain`; again with `DARK=true`. `./bl debug calendars`. A few
ordinary lines by `./bl debug keys` (an app's first letters, a sum, `note x`). `./bl debug guide`. `./bl logs`.

The panel as before in both themes. `allowed=false writes=false pretended=false count=0`: nothing was asked of
the system, and the system showed no question. Every example of `./bl debug guide` is `ok`, the event's among
them (`event dinner with Sam tomorrow 7pm`). No crash.

Answer: not run

### 2. What the parser reads now, under the keyword [C]

`./bl debug event TEXT` for each of: `dinner with Sam tomorrow at 7pm at Cafe Luna` · `meeting tmrw 9-10am` ·
`Flight to Frankfurt on the 12th at 6:45 in the morning` · `Standup tomorrow from 9 to 10` · `Team offsite Oct 14
to Oct 16 @ Lisbon` · `Birthday party for Sam on November 3rd all day` · `yoga class every Monday 6pm` ·
`Abendessen morgen um 7 abends` · `Messe vom 14. bis 16. Oktober` · `Geburtstag von Oma am Samstag ganztägig` ·
`Dentist` · `2nd interview tomorrow`.

In this order: tomorrow 19:00 to 20:00, the title with "at Cafe Luna" in it and `loose`; tomorrow 9:00 to 10:00;
the 12th at 6:45, title "Flight to Frankfurt"; tomorrow 9:00 to 10:00; the 14th to the 17th at midnight, "all
day", place "Lisbon"; 3 November, "all day", not a guess; the next Monday at 18:00 and "(a guess: repeats)";
tomorrow 19:00; the 14th to the 17th, all day; Saturday, all day, not a guess; today, all day, a guess; tomorrow,
with "2nd interview" as the title.

Answer: not run

### 3. The row has its two lines from the first letter, and nothing in it moves [C]

`./bl open stay`, then `./bl debug keys "event dinner with Sam tomorrow at 7pm @ Cafe Luna"`, with `./bl debug
dump` and `./bl shot` after `event d`, after `tomorrow`, after `7pm` and at the end (four panels, typed anew each
time: `keys` types from an empty field).

In every one the row is 92 dp high (`window=` says the same height in all four). Its caption, then `WHEN` and
`TITLE` on one line, then `WHERE` on the line under it: the same three lines at the same places in all four
pictures. An empty slot is its label and a short rule. "When" is a step lighter while it is a guess (`?` in the
dump) and full once day and time are both read. No Calendar slot: the calendars are not allowed.

Answer: not run

### 4. A sentence without the keyword [C]

`./bl debug keys "Add dinner with Sam tomorrow at 7pm"`, with `./bl debug dump` after `Add dinner with Sam` and at
the end. Then `./bl debug find` for a line that is an event and finds something of the device too (the first
letters of an app's name after `add` and before a day: whatever this device has). Then `./bl debug event TEXT`
for: `add pictures of the sun` · `add 2-3 eggs` · `Book club Friday 7pm` · `Add dinner with Sam` · `Lunch with Sam
on Friday at noon`.

After `Add dinner with Sam`: no event's row (no day, no time). At the end: the row `sentence:event` is row one,
selected, `Create` armed, and under it the question for Gemini and the web's search. In the `find`: whatever of
the device matches stands over the event's row, the web's rows under it. The five: `offered=false` (a short
weekday alone) · `offered=false` (two bare numbers) · `offered=true`, title "Book club" · `offered=false` (no day
or time) · `offered=false` (no cue; under `event` it is read).

Answer: not run

### 5. What Enter would send [C]

`./bl debug event "Add dinner with Sam tomorrow at 7pm"`, and the same with `@ Cafe Luna` at its end.

`enter=create: would send https://calendar.google.com/calendar/render?action=TEMPLATE&text=dinner%20with%20Sam&dates=…Z/…Z`:
the two moments are tomorrow 19:00 and 20:00 of this device's zone, said in UTC. No `src=`. With the place:
`&location=Cafe%20Luna` before the end.

Answer: not run

### 6. `remind`, and a date that was copied, are as they were [C]

`./bl debug find "remind tomorrow 5pm call"`, `./bl debug find "remind all day sale"`. `./bl debug copy "Dinner
with Sam on Friday at 7pm. See you"`, `./bl open stay`, `./bl debug key tab`, `./bl debug dump`.

A reminder for tomorrow 17:00 as before; "all day sale" has no day and no time to ring at, as before. The copy's
rows have the date's row ("Fri …, 7–8 PM", "Dinner with Sam"), and its Enter goes on in the event's row.

Answer: not run

### 7. The editor opens, filled in, and is not saved [C] and [A]

`./bl debug keys "Add Booklight test dinner tomorrow at 7pm"`, a moment later `./bl debug key enter`.

The Calendar app's editor opens in a window of its own with the title "Booklight test dinner" and tomorrow 7 to 8
PM, on the calendar the Calendar app chooses. Booklight's panel has gone. The editor is closed without saving.

Answer: not run

## The model

### 8. The model splits what the rules could not [C]

`./bl open stay`, then `./bl debug event ask TEXT` for: `Schedule dentist appointment next Tuesday at 3:30pm at Dr.
Sam office` · `Team offsite Oct 14 to Oct 16 in Lisbon` · `Elternabend am 14.10. um 19:30 in der Schule` · `Flight to
Frankfurt on the 12th at 6:45 in the morning`.

Each: `ai=READY`, the model's own line of JSON, `taken`, and the reading with it: the place "Dr. Sam office" ·
"Lisbon" · "Schule" · and for the flight no place (the model's "Frankfurt" is in the title already). The day and
time of each are what `./bl debug event TEXT` says of the same text without the model: the model never moves a date.

Answer: not run

### 9. Its answer changes the slots where they stand [C]

`./bl open stay`, `./bl debug keys "Schedule dentist appointment next Tuesday at 3:30pm at Dr. Sam office"`, then
`./bl debug dump` and `./bl shot before` at once, the dump again after one second, and `./bl debug dump` and
`./bl shot after` once the dump no longer says `looking` (up to seven seconds the first time).

At once: the title "dentist appointment at Dr. Sam office", `WHERE` empty, no `looking`, no `thinking`: the row is
the rules' own, in the frame of the last key. About a second later: `looking` (the edge's light runs). Then: title
"dentist appointment", `WHERE` "Dr. Sam office". `window=` is the same before and after; the two pictures differ
in those two slots and in nothing else: no line has moved, the pill and the strip stand where they stood.

Answer: not run

### 10. It is asked after a rest, once, and never for a sentence the rules read whole [C]

`./bl debug keys "Add dinner with Sam tomorrow at 7pm"`, dumps for five seconds. Then the sentence of check 9
typed again in the same panel (`./bl debug keys …`), with a dump after half a second and after two seconds. Then
`./bl debug keys` with the same sentence and one more word, and at once one more letter by `./bl debug in`.

The first: never `looking`, nothing changes: nothing was left over. The second: the split is there within the
first dump, with no `looking`: this text was asked once and is not asked again. The third: no `looking` while
letters come; it begins only 0.7 s after the last one.

Answer: not run

### 11. No model here [C]

`./bl debug ai none`, `./bl open stay`, the sentence of check 9, dumps for three seconds; then `./bl debug ai real`.

The row is the rules' reading and stays it; never `looking`. Enter is `Create` as before.

Answer: not run

### 12. Enter in the moment the words change [A]

With the sentence of check 9 typed by hand: Enter pressed in the instant the slots change.

That press is not taken: the row stands, with its new words. A second press, a third of a second later or more,
runs it. (Nothing waits for the model: an Enter before its answer runs the row as the rules read it.)

Answer: not run

## The calendars, pretended

### 13. A calendar by its name, and the start of one completed [C]

`./bl debug calendars pretend Sam,Team,Team trips,Holidays!`, `./bl open stay`, `./bl debug keys "Add dinner with
Sam tomorrow at 7pm to tea"`, `./bl debug dump`, `./bl shot tea`; `./bl debug key right`, `./bl debug dump`;
then `./bl debug in` with the same text and a space at its end.

The row has `CALENDAR` on its second line, after `WHERE`. With `to tea`: `completion=m`, the field shows a grey
"m" after the text, the slot says `●Team` with the calendar's colour as a dot, the title is "dinner with Sam".
After Right: the field ends in "to Team", `completion=null`, the slot the same. With the space: no grey letter.
Then `./bl debug keys "Add dinner with Sam tomorrow at 7pm to Team cal"` and a dump: the slot says `●Team`, the
title is "dinner with Sam" and `completion=null` (the calendar does not leave the row while the word "calendar"
is typed after its name).

Answer: not run

### 14. The Calendar stop and its list [C]

In the panel of check 13: `./bl debug key tab` until the dump says `armed=calendar`; `./bl debug key enter`,
`./bl debug dump`, `./bl shot list`; `./bl debug key down`, `./bl debug key enter2`, `./bl debug dump`.

Tab only moves: nothing opens until Enter. Then `opened=sentence:event:calendar`, and under the row three lines
(`+c`), each a calendar's name with its colour's dot in the icon column: not the one that takes no new events.
The row's strip is gone but for the word "Calendar" and the arrow, turned over. After Down and Enter twice: the
list is closed, the field's sentence names the calendar of the second line in place of the one it named, the
pill is on the event's row with its first action armed, and **the second Enter did nothing**: the panel is open.

Answer: not run

### 15. What Enter would send with a calendar named [C]

`./bl debug event "Add dinner with Sam tomorrow at 7pm to Team"`.

`calendar=Team`, and the link ends `&src=pretend-1%40example.com`.

Answer: not run

### 16. Save, looked at with nothing to write into [C]

Still pretended: `./bl debug pref save on`. `./bl debug event TEXT` for: `Add dinner with Sam tomorrow at 7pm to
Team` · `Add dinner with Sam tomorrow at 7pm` · `Add dinner with Sam tomorrow` · `Put yoga class every Monday 6pm in
my Team calendar` · `Add trip tomorrow at 9am to Holidays`. Then in a panel: the first of them by `./bl debug
keys`, `./bl debug dump`, `./bl shot save`, `./bl debug key enter`, `./bl debug dump`. Then `./bl debug pref save off`.

The five: `enter=save: would write calendar=Team title='dinner with Sam' start=… end=… allDay=false zone=` this
device's zone, `+ the calendar's default reminder` · the same into `Sam`, the account's own, and the row's
Calendar slot says `●Sam?` (a step lighter: it was not typed) · `enter=open` and `footer=Opens in Calendar: the day
or the time is a guess` · `enter=open`, `footer=A repeat is set in Calendar` · `enter=open`, `footer=Opens in
Calendar: Holidays takes no new events`. In the panel: the strip is Save · Open in Calendar · Copy · Calendar,
Save armed, the caption "New event. Enter saves it." Enter: nothing is written (a pretended list has nothing to
write into), the footer says it could not be done, and the panel stays.

Answer: not run

### 17. The row points at the step, until the question has been answered [C]

`./bl debug calendars pretend off`, `./bl debug pref calasked no`, `./bl debug event "Add dinner tomorrow at 7pm to
the Team calendar"`; then `./bl debug pref calasked yes` and the same again.

First: `named`, the title "dinner to the Team calendar", an action `allow` after Copy, `footer=Allow Calendars to
put it on a calendar by name`. After: no `allow`, no footer; the title the same.

Answer: not run

### 18. Many calendars, a long name, the dark theme [C]

`./bl debug calendars pretend` with fourteen names, one of them forty letters long; a sentence that names the
long one; the list opened; `./bl shot` in both themes.

The list has twelve lines. The long name fades or is cut at the slot's end and pushes nothing. Every dot can be
told from the glass in both themes.

Answer: not run

## The window, and the system's permission

### 19. The grant, the switch and the choice [C]

`./bl window privacy`, `./bl wshot privacy`; `./bl window results`, `./bl wshot results`; each at three widths of
the window (narrow, with the rail, with the second pane).

Privacy › What Booklight may use: a row "Calendars" under Brightness, with a mark, one line of text that ends
"Not allowed yet.", and "Allow…" at the row's end, on the line the two rows above end on. Results: a group
"Events" between "Show" and the other apps: "Save events without opening Calendar" with its switch off, and "New
events go to" with "Allow…". Every mark starts 16 dp in, every text on one edge, every control ends 16 dp from
the row's trailing edge; nothing is centred.

Answer: not run

### 20. The system's question, answered by a hand [A]

Privacy › Calendars › Enter. "Don't allow". The same again, and "Don't allow" again. Then once more. Then allow
it in the system's own page. Then Results › "Save events without opening Calendar" › Enter, and "Don't allow";
again, and "Allow".

Each refusal leaves the row as it was. After the second, the system asks no more: the row opens Booklight's page
in the system's Settings instead. Allowed: the row says what Booklight reads, and "Change…" opens that page. The
switch stays off when its question is refused, and goes on when it is allowed.

Answer: not run

### 21. Allowed: the list is read, and only the list [C]

`./bl sh pm grant io.github.kuscher.booklight android.permission.READ_CALENDAR`, `./bl open stay`, `./bl debug
calendars`, `./bl debug close`, `./bl window results`, `./bl wshot results-allowed`, `./bl window privacy`.

`allowed=true writes=false count=` one or more (the answer says how many, and no name). Results: "New events go
to" is a menu button that shows the account's own calendar; it stands where the search engine's stands (both at
their rows' ends, or both under their text). Privacy: "Booklight reads the list of your calendars. It never reads
an event."

Answer: not run

### 22. The switch goes off with its permission [C]

`pm grant … WRITE_CALENDAR`, `./bl debug pref save on`, `./bl open stay`, `./bl debug pref`: `save=true`. Then
`pm revoke … WRITE_CALENDAR`, `./bl open stay`, `./bl debug pref`.

`save=false`: the switch went off with its permission, and comes back only by a press in the window.

Answer: not run

### 23. The editor opens on the calendar that was named [C] and [A]

With the list allowed and the switch off: a sentence that names one of this device's calendars other than the
account's own, by `./bl debug event` first (the link ends in `&src=` and an address), then typed in a panel and
Enter.

The Calendar app's editor opens with that calendar chosen. It is closed without saving.

Answer: not run

### 24. One event is saved, once [C]

Both permissions given, `./bl debug pref save on`, "New events go to" on the account's own calendar. `./bl debug
event "Add Booklight test event tomorrow at 7am"` first. Then `./bl debug keys` with that sentence, `./bl debug
dump`, `./bl debug key enter`, `./bl shot saved` within half a second.

The hook: `enter=save: would write` into the account's own calendar. In the panel: Save armed, the Calendar slot
shows that calendar a step lighter. Enter: the footer says "Saved to" and the calendar's name with the drawn
check, and the panel goes. The Calendar app shows one event of that name tomorrow from 7 to 8 in this device's
zone, with the calendar's own default reminder, and no second one. **The event is removed after the check.**

Answer: not run

### 25. Nothing saves twice, and nothing but Enter saves [C] and [A]

With the switch on and a pretended list (nothing can be written): a sentence that would be saved, then `./bl
debug key stay` (Enter with Shift held). By hand [A]: Ctrl + the row's digit on such a row.

Enter with Shift held does what Enter does: an event that was saved takes the panel with it (here: the footer says
it could not be done). Ctrl + digit does nothing on a row whose first action is Save.

Answer: not run

### 26. A line that reads as an event is not sent for suggestions [C]

`./bl debug pref suggestions on`, `./bl open stay`, `./bl debug keys "Add dinner with Sam"`, a dump a second
later; then `./bl debug keys "Add dinner with Sam tomorrow at 7pm"`, a dump a second later; `./bl debug pref
suggestions off`.

The first may have suggested searches under the web's row, as any typed line has. The second has none: once the
line reads as an event it is not sent.

Answer: not run

### 27. In German [C]

With the app's language German and a pretended list: `./bl debug keys "Trag Mittagessen mit Sam am Freitag um 12 in
den Team-Kalender ein"`, `./bl shot de`; the list opened and a calendar chosen; `./bl window results`, `./bl wshot
de-results`; `./bl window privacy`.

The slots WANN, TITEL, WO, KALENDER; "Erstellen" (with the switch on "Speichern" and "Im Kalender öffnen"). A
calendar chosen from the list is written as "in" and its name, before the „ein“. The window's rows in German,
each title on one line at the widest window and nothing cut at the narrowest.

Answer: not run

### 28. What a screen reader is told [C]

`./bl open stay`, the sentence of check 13, `./bl debug first says`.

The event's row says its name, then each slot that has a value with its label ("When …", "Title …", "Calendar
Team"), then the armed action. After the model's answer (check 9) the row's new slots are said once.

Answer: not run

### 29. Settings that were there before [C]

The build installed over a 3.1 that has been used: `./bl debug pref`, `./bl debug calendars`.

`save=false eventcal=own calasked=false`, `allowed=false`; everything else `./bl debug pref` says is as it was
under 3.1.

Answer: not run

### 30. The HP Googlebook, and what only a person can say [A]

Checks 1, 3, 9, 13, 14 and 19 on the HP Googlebook. And by eye: do two lines of slots read as one row; is "WHERE
–" on every event's row right, or should a slot stand only once it is filled; is the dot a help; is "Save" as the
armed action right for you with the switch on.

Answer: not run
```

- [ ] **Step 5: Look for what must be so**

```bash
grep -c "^Answer: not run$" docs/research/event-sentence.md
grep -c "^### [0-9]*\. " docs/research/event-sentence.md
grep -c "\[A\]$" docs/research/event-sentence.md
git diff -- app/src/debug | grep "^+" | grep -c "startActivity\|contentResolver\|executor.run"
grep -n '"event" ->\|"calendars" ->\|"save" ->\|"eventcal" ->\|"calasked" ->' app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt | wc -l
```

Expected: `30` (every check's answer reads "not run"); `30` (the checks); `6` (those that are Alex's, or his too: 7, 12, 20, 23, 25 and 30); `0` (no new hook starts an activity, touches a provider or runs an effect: they say, and do not do); `5` (the five new words).

- [ ] **Step 6: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`.

- [ ] **Step 7: Commit**

```bash
git add app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt app/src/main/java/io/github/kuscher/booklight/providers/Events.kt bl docs/research/event-sentence.md
git commit -m "An event from a sentence: the hooks that say what a sentence's row reads and what Enter would send or write, pretend a list of calendars and ask the model as the row does; and the checks for a device"
```

### Task 11: The documents say what it is

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `CLAUDE.md`
- Modify: `PRIVACY.md`
- Modify: `README.md`
- Modify: `docs/PICKING-UP.md`
- Modify: `docs/PLAN.md`
- Modify: `docs/design/design-system.md`
- Modify: `docs/design/ux-model.md`
- Modify: `store-submission/forms/data-safety.md`

**Interfaces:**
- Consumes: everything above, as built; `docs/research/event-sentence.md` (Task 10).
- Produces: the documents below. No code, no resource, no version.

A permission needs its line in `docs/PLAN.md` §4 and §6 (CLAUDE.md, at its top), in `PRIVACY.md` and in the store's form; the rule on the model changes as the design's §11 has it; and a session that picks the branch up must find what was built, what was decided on the way, and that nothing was run on a device.

- `PRIVACY.md`: a paragraph "Calendar events" before "Handing things to other apps", and the Calendar permission in "Permissions". It says what Android's one question covers and what Booklight asks for; that no event is read; when an event is written; what is kept; and that a line that reads as an event is not sent for suggestions.
- `docs/PLAN.md`: §4, "An event from a sentence: what was built", with Alex's words; §6, one row of the permission tiers.
- `store-submission/forms/data-safety.md`: "An event from a sentence: what changed for the form".
- `CHANGELOG.md`: the entry, under **"Unreleased"**. The version is not this plan's.
- `README.md`: the feature, a line of the table of what you can type, and the window's two sections.
- `docs/design/ux-model.md` §20 and `docs/design/design-system.md` §18 (and the one exception to "one coloured thing" in its §1).
- `CLAUDE.md`: the permissions line at its top; the layout (core's `Sentence.kt` and `Split.kt`, the app's `providers/Events.kt` and `device/CalendarList.kt`); the hooks; the rule on the model; that Save may be armed and is Enter's alone; the calendar's dot; four gotchas.
- `docs/PICKING-UP.md`: the newest entry.

- [ ] **Step 1: The privacy text, in `PRIVACY.md`.** Two changes.

Find:

```markdown
goes to Spotify only. Take it out in the same place and nothing is sent again. Booklight ships no key and
its developer sees neither yours nor your lookups.

**Handing things to other apps.** A web row opens in your browser. A mail, an event, a note for Keep, a
```

Make it:

```markdown
goes to Spotify only. Take it out in the same place and nothing is sent again. Booklight ships no key and
its developer sees neither yours nor your lookups.

**Calendar events.** An event you type (`event dinner tomorrow 7pm`, or a sentence that begins with add, schedule,
put, book or plan and holds a day or a time) is read on the device, by Booklight's own rules. Where those rules
cannot tell the title from the place, the model on the device (see "Answers on this device") is asked which words
are which, a moment after you stop typing. It works out no date, and what it says is shown only if every word of
it is one you typed. Enter opens your Calendar app's editor, filled in, and you save the event there.
**The list of your calendars is read only if you allow it** (the Booklight window › Privacy › Calendars): their
names, their colours and the ids the system has for them, so that you can name a calendar and the editor opens on
it. Booklight never reads an event. Nothing of the list leaves the device, and none of it is written to Booklight's
files. Android has one question for all of this and asks it about "your calendar" as a whole; what Booklight asks
the system for is the list of calendars and nothing else.
**Booklight saves an event itself only if you switch that on** ("Save events without opening Calendar", the
Booklight window › Results), which asks Android for the permission to add events. Then Enter writes the one event
the row shows into the calendar you named, or into the one chosen under "New events go to", with that calendar's
own default reminder: only where the day and the time were both read from what you typed. Any other event opens
in Calendar as before. Booklight never changes or removes an event and keeps no copy of one. What it keeps: the
switch, the calendar you chose (its address, as the system lists it), and that Android's question has been
answered. A line that reads as an event is not sent for search suggestions; before it does (no day or time typed
yet) it is a typed line like any other.

**Handing things to other apps.** A web row opens in your browser. A mail, an event, a note for Keep, a
```

Find:

```markdown
network is up. None of these shows a prompt. No accessibility service, no notification access, no access
to your contacts or location. Nothing of Booklight's own runs in the background; the library's usage
```

Make it:

```markdown
network is up. None of these shows a prompt. The Calendar permission does: Android asks you, and only when you
press Allow… in the Booklight window (to read the list of your calendars), or switch on "Save events without
opening Calendar" there (to add the event you save). Until then Booklight reads and writes nothing of it. No
accessibility service, no notification access, no access to your contacts or location. Nothing of Booklight's own runs in the background; the library's usage
```

- [ ] **Step 2: The plan, in `docs/PLAN.md`.** Two changes: §4, and a row of §6.

Find:

```markdown
and over. Still not built: the Gmail relay; the layer of extensions that answer while you type.

### After 1.0: plenty before any permission that makes review harder
```

Make it:

```markdown
and over. Still not built: the Gmail relay; the layer of extensions that answer while you type.

### An event from a sentence: what was built (5 October 2026)

Alex asked for a calendar entry "just from typing out freeform text", with the calendar permission "if that
helps so we can even better match calendars", and approved the design the same day: "Yes. Re 2 add save too
behind an option" (`design/event-sentence/design.md`; its §11 is what was settled). An event is typed the way it
is said, with the keyword or without; the rules read it at every letter, the model on the device helps to split
a sentence the rules cannot and never reckons a date; a calendar is named and completed; Enter opens the Calendar
app's editor on that calendar, or, with a switch, saves the event. **Two permissions are new, by his word, both
asked for in the Booklight window alone and inert until then (§6):** `READ_CALENDAR` for the list of calendars
(never an event), `WRITE_CALENDAR` for the one event the row shows, only with "Save events without opening
Calendar" on. The plan and what it decided on the way: `superpowers/plans/2026-10-05-event-from-a-sentence.md`.

### After 1.0: plenty before any permission that makes review harder
```

Find:

```markdown
| 0 (2.0) | Nothing at install: AICore's `BIND_SERVICE` and `ACCESS_NETWORK_STATE` come with Google's ML Kit library and are granted without a prompt | Prompts answered by the system's own model, on the device | The library reports usage to Google: data-safety entries (device or other IDs, diagnostics), and an audience of 18 and over |
```

Make it:

```markdown
| 0 (2.0) | Nothing at install: AICore's `BIND_SERVICE` and `ACCESS_NETWORK_STATE` come with Google's ML Kit library and are granted without a prompt | Prompts answered by the system's own model, on the device | The library reports usage to Google: data-safety entries (device or other IDs, diagnostics), and an audience of 18 and over |
| 1 (an event from a sentence) | The Calendar permission, Android's own question, asked only by a press in the Booklight window: `READ_CALENDAR` (Privacy › Calendars), and `WRITE_CALENDAR` only by switching on "Save events without opening Calendar" (Results) | The list of calendars (names, colours, ids; never an event): a calendar named in a sentence, completed in the field, and chosen in the editor that opens. With the switch: the one event the row shows is written, on Enter, never a guess | No declaration form for these two. Nothing is collected or shared: the list stays on the device. A release with a new permission is sent through the Play Console, not the API |
```

- [ ] **Step 3: The store's form, in `store-submission/forms/data-safety.md`.** One change: a section at the end.

Find:

```markdown
  sent to the search engine.
```

Make it:

```markdown
  sent to the search engine.

## An event from a sentence: what changed for the form

Nothing new leaves the device through Booklight, and nothing new is collected. New, all on the device or handed
to an app the user sees open:

- **Permissions, both runtime, both the Calendar group's, both asked for only by the user's own press in the
  Booklight window:** `android.permission.READ_CALENDAR` (Privacy › Calendars › Allow…) and
  `android.permission.WRITE_CALENDAR` (Results › "Save events without opening Calendar"). Neither has a Play
  declaration form. Until the user allows one, Booklight asks the system nothing of it. No `<queries>` line was
  added.
- **What is read with the first:** the system's list of calendars and nothing else: for each calendar of a Google
  account its id, name, colour, owner's address, whether it is the account's own and whether events can be added
  to it (`device/CalendarList.kt` names the seven columns). **No event is read, anywhere in the app.** The list is
  kept in memory while the app runs and is not written to storage.
- **What is written with the second:** one event, on the user's Enter, with the switch on: its title, start, end,
  all day or not, the device's time zone, the place, the calendar; and one reminder row that says "this
  calendar's default". Only where the day and the time were both read from the typed sentence (core `Cals.saves`,
  with tests). Booklight never updates or deletes an event and keeps no record of the ones it wrote.
- **Handed to the Calendar app by the user's Enter (the switch off, or the event a guess):** a link to
  `https://calendar.google.com/calendar/render?action=TEMPLATE` with the title, the dates, the place and the
  calendar's address (`src`), sent to `com.google.android.calendar` alone; where no such app takes it, the
  `INSERT` request of 1.1. An ordinary Android hand-over: the Calendar app opens its editor and the user saves
  there. Booklight itself opens no connection for it.
- **The model on the device** is asked to split an event's sentence into title, time, place and calendar, where
  Booklight's own rules leave the title in pieces: after 0.7 s without a key, once for a text. The sentence stays
  on the device, as every prompt does (see "2.0: what changed for the form" for what the library itself reports).
- **Stored in `files/settings.json`:** whether the switch is on, the chosen calendar's owner's address ("New
  events go to"; empty for the account's own), and that the system's question has been answered once.
- **Suggestions:** a line that reads as an event (it begins with add, schedule, put, book or plan and holds a day
  or a time) is not sent to the search engine, with suggestions on or off. Before it reads as one it is a typed
  line like any other.
- **For the data-safety answers:** no new data type. "Calendar events" is not collected and not shared: nothing of
  a calendar is transmitted off the device by Booklight.
```

- [ ] **Step 4: The changelog, in `CHANGELOG.md`.** One change: an entry under "Unreleased", above 3.1.

Find:

```markdown
# Changelog

## 3.1 (5 October 2026)
```

Make it:

```markdown
# Changelog

## Unreleased

**An event from a sentence** (`docs/design/event-sentence/design.md`).

- **Type an event the way you say it.** "Add dinner with Sam tomorrow at 7pm to the Team calendar": one row
  shows what Booklight understood (when, the title, where, the calendar), and Enter opens your Calendar app's
  editor, filled in, on that calendar. In any order. With the keyword `event`, or without it where the line
  begins with add, schedule, put, book or plan and holds a day or a time; such a row stands below anything of
  your device that matches and above the web.
- **The day and time are read anywhere in the line**, and more of them: `tmrw`, "on the 12th at 6:45 in the
  morning", "from 9 to 10", "Oct 14 to Oct 16", "all day"; in German „von 9 bis 10“, „am 12.“, „vom 14. bis 16.
  Oktober“, „ganztägig“, „7 abends“. "every Monday" is read as the next Monday and shown as a guess.
- **Your calendars, if you allow it.** Booklight window › Privacy › Calendars. Booklight then reads the list of
  your calendars (names and colours, never an event): name one in the sentence, its start is completed in grey
  and Right takes it, and a Calendar stop on the row lists them.
- **The model on your device helps to split.** Where the rules cannot tell a place from the title, the model is
  asked a moment after you stop typing, and the words in the row change where they stand. It works out no date,
  nothing waits for it, and nothing it says is shown unless you typed every word of it.
- **Save without opening Calendar, if you switch it on.** Booklight window › Results › "Save events without
  opening Calendar", and "New events go to". Enter then saves the event the row shows; "Open in Calendar" is the
  second action. A guess is never saved: where the day or the time was not read, Enter opens Calendar.
- Two new permissions, both asked for only in the Booklight window: to read the list of calendars, and (with the
  switch) to add events. `PRIVACY.md` says what each does.

## 3.1 (5 October 2026)
```

- [ ] **Step 5: The README, in `README.md`.** Three changes.

Find:

```markdown
- **Small controls.** `vol`, `brightness`, `pause`; an emoji grid, the letters of other languages (`abc danish`), colour values, QR codes, passwords.
```

Make it:

```markdown
- **An event, the way you say it.** "Add dinner with Sam tomorrow at 7pm to the Team calendar": the row shows
  what was understood, and Enter opens your calendar's editor on that calendar, filled in. If you allow it,
  Booklight reads the list of your calendars (never an event) to complete a calendar's name; if you switch it
  on, Enter saves the event without opening Calendar.
- **Small controls.** `vol`, `brightness`, `pause`; an emoji grid, the letters of other languages (`abc danish`), colour values, QR codes, passwords.
```

Find:

```markdown
| `note buy milk` · `event Fri 3pm Dentist` · `remind 5pm call bank` | A note, an event, a reminder |
```

Make it:

```markdown
| `note buy milk` · `event Fri 3pm Dentist` · `remind 5pm call bank` | A note, an event, a reminder |
| `add dinner with Sam tomorrow at 7pm to the Team calendar` | An event, typed the way you say it |
```

Find:

```markdown
kinds of rows show, other apps' commands), Labs (what needs a key of your own: Spotify, flight times) and Privacy (the notes folder, brightness,
```

Make it:

```markdown
kinds of rows show, whether an event is saved without opening Calendar and where it goes, other apps' commands), Labs (what needs a key of your own: Spotify, flight times) and Privacy (the notes folder, brightness, your calendars,
```

- [ ] **Step 6: How it behaves, in `docs/design/ux-model.md`.** One change: §20, at the end.

Find:

```markdown
| Esc | closes; the screen waits | closes; the choices are done | the key's step lands |
```

Make it:

```markdown
| Esc | closes; the screen waits | closes; the choices are done | the key's step lands |

## 20. An event from a sentence (5 October 2026)

`docs/design/event-sentence/design.md`; §11 there is what Alex settled.

- **One row, with the keyword or without.** `event dinner tomorrow 7pm` as before; and a line that begins with
  add, schedule, put, book or plan („trag … ein“, „plane“, „neuer Termin“) and holds a day or a time is the same
  row with no keyword typed. That row stands below everything of the device that matches and above everything of
  the web: if nothing of the device matches it is row one and Enter runs it. Never for a line without a day or a
  time, and not for one whose only "day" is a word that is far more often something else ("add pictures of the
  sun", "add 2-3 eggs").
- **What it reads, at every letter:** the day and time wherever they stand; the place after an @; a calendar by
  its name after "to", "in" or "on" at the end of the sentence or just before the day; the cue dropped ("add",
  "schedule", "put") or kept ("book", "plan": "Book club" is a title); the rest is the title.
- **The model on the device helps and is never waited for.** Only where the title is left in pieces, 0.7 s after
  the last key, once for a text. Every part it names must be words that were typed, side by side; the day and time
  are the parser's; no typed word may be lost. Then the slots change where they stand. An Enter in the moment
  they change is not taken; a new press is.
- **A calendar's name is completed** in grey at the very end of the sentence, from two letters on, and the row
  already reads that calendar; Right takes the rest. Once the name is whole the row keeps the calendar while the
  word "calendar" is typed after it.
- **Keys on the row.** Enter: Create (the Calendar app's editor, filled in, on the calendar named). Tab: Copy,
  then Calendar. Enter on Calendar opens the list of calendars under the row; Enter on a line writes that
  calendar into the sentence and closes the list, and does not also run the row.
- **Save is the user's choice, and never a guess.** With "Save events without opening Calendar" on, Enter is Save
  and "Open in Calendar" the second action. Where the day or the time was not read from the sentence, or the
  sentence said "every", Enter opens Calendar instead and the footer says why. Save is written by Enter on the
  selected row and by nothing else: not by Ctrl + digit, and never twice (the panel goes with it, Shift held or
  not). The footer says "Saved to" and the calendar.
- **Why Save may be the armed action.** The rule is that what removes or cannot be taken back is never armed
  first. Save adds one event the user has typed and sees whole in the row; it removes nothing, it is on only by
  the user's own switch, and the calendar keeps the event for the user to change or delete. What Booklight cannot
  do is take it back itself (it never changes or removes an event): that is said here so that nobody adds an Undo
  by reading an event.
- **Nothing is asked for in the panel.** Both permissions are given in the Booklight window. Where a sentence
  names a calendar by the word and the list is not allowed, the row has "Allow…" as its last action and the
  footer says so, until Android's question has been answered once; it leads to the window's row.
```

- [ ] **Step 7: How it looks, in `docs/design/design-system.md`.** Two changes: the exception in §1, and §18 at the end.

Find, in one long line:

```markdown
of a flight's row, green or amber, §14.)
```

Make it:

```markdown
of a flight's row, green or amber, §14. And one sample of a colour that is the user's own, as a swatch is: the dot of a calendar's colour, §18.)
```

Find:

```markdown
frame the pill arrives. Nothing of the two is drawn outside the glass, and the window is never resized in width.
```

Make it:

```markdown
frame the pill arrives. Nothing of the two is drawn outside the glass, and the window is never resized in width.

## 18. An event’s row (5 October 2026)

`docs/design/event-sentence/design.md`. The preview row of §3c, 92 dp, with nothing new drawn but a dot.

- **Two lines of slots, always.** The caption, then `WHEN` and `TITLE`, then `WHERE` and, where the list of
  calendars is known, `CALENDAR`. Both lines stand from the first letter; an empty slot is its label and the rule.
  So the row's three lines never change their places: when a place or a calendar is read, by the rules at a key
  or by the device's model a moment later, a value changes where it stands and nothing moves. (One line cannot
  hold four slots beside a strip of four actions: at 720 dp the third slot of the old row was already cut.)
- **The dot.** A calendar's own colour, 10 dp, round, before the calendar's name in its slot and in the icon
  column of its line in the list of calendars. It is a sample of something that is the user's, as a colour's
  swatch is, not a state: no word and no line of the row is coloured, and the dot never says anything a word
  does not say beside it.
- **A guess is a step lighter** (ink 0.80), as ever: "When" where the day or the time was not typed or the
  sentence said "every"; the calendar where it was not typed and is the one a saved event goes to.
- **The strip.** Create · Copy · Calendar. With "Save events without opening Calendar" on: Save · Open in Calendar
  · Copy · Calendar, and for a guess Open in Calendar first, with the footer's quiet line saying why. Save has the
  check, like every action that acts at once and says so in the footer; it is not a removal and not red.
- **Calendar** is a stop that opens a list under the row, as Window does on an app's row (§16): Tab only moves,
  Enter opens, the row keeps the word and the turned arrow while the pill is on a line. Twelve lines at most.
- **The field** shows what is left of a calendar's name in grey after the text, as it shows an app's; Right takes it.
- **The window.** "Calendars" is a row of Privacy › What Booklight may use, with "Allow…" as its word, like
  Brightness. Results has a group "Events": a switch, and a menu button for "New events go to", which stands
  where the search engine's stands (both at the rows' ends, or both under their text).
```

- [ ] **Step 8: The notes for working on the code, in `CLAUDE.md`.** Eight changes.

Find:

```markdown
AICore's `BIND_SERVICE` and `ACCESS_NETWORK_STATE`, and the library's usage reporting to Google. No runtime
permissions, no accessibility service, nothing of Booklight's own in the background (the user's rule: adding a
permission needs his say-so and a place in docs/PLAN.md §4/§6).
```

Make it:

```markdown
AICore's `BIND_SERVICE` and `ACCESS_NETWORK_STATE`, and the library's usage reporting to Google. Two runtime
permissions, both the Calendar's and both Alex's word of 5 October 2026, asked for in the Booklight window and
nowhere else, inert until then: `READ_CALENDAR` (the list of calendars: names, colours, ids; **never an event**)
and `WRITE_CALENDAR` (the one event the row shows, on Enter, only with "Save events without opening Calendar" on;
Booklight never changes or removes an event). No accessibility service, nothing of Booklight's own in the
background (the user's rule: adding a permission needs his say-so and a place in docs/PLAN.md §4/§6).
```

Find:

```markdown
    (dates and times, English and German), `Jot.kt` (mail, event, reminder, timer, new), `Colors.kt`,
```

Make it:

```markdown
    (dates and times, English and German; `When.spot` finds them anywhere in an event's line), `Jot.kt` (mail, event, reminder, timer, new),
    `Sentence.kt` (an event the way it is said: the cue, the place, the calendar by its name, `reads` for a line without the keyword, `put`; `Cal` and `Cals`: which calendar a name means, where a new event goes, the calendar link, the values of a direct save, and `saves`: never a guess),
    `Split.kt` (the device's model as a splitter: when it is asked, its question, its answer taken apart and merged with the rules' reading only where every part was typed; the date is always `When`'s), `Colors.kt`,
```

Find:

```markdown
    `FlightsProvider` is the row, `FlightScope` the keyword `flight`).
```

Make it:

```markdown
    `FlightsProvider` is the row, `FlightScope` the keyword `flight`). `Events.kt` (an event from a sentence: the event's row,
    the same under the keyword `event` and for a line typed without it; its slots on two lines, Create or Save and Open in
    Calendar, the Calendar stop's list; it asks the device's model to split after a rest and lands the answer in the row).
```

Find:

```markdown
    touches the system's clipboard and its text classifier) and `data/Notes.kt`.
```

Make it:

```markdown
    touches the system's clipboard and its text classifier; `CalendarList`, the one place that reads the system's
    list of calendars, and reads nothing else of it) and `data/Notes.kt`. An event is written in `Executor.save`
    and nowhere else.
```

Find:

```markdown
- First run's stored state (core `FirstRun`; `docs/design/first-run/`): `./bl debug first` shows the run, what is done, the screen that stands and its
```

Make it:

```markdown
- An event from a sentence (`docs/design/event-sentence/design.md`; the checks: `docs/research/event-sentence.md`):
  `./bl debug event TEXT` says what the rules read of TEXT and what Enter on its row would do, said and not done (the
  link it would send, or the values it would write); `./bl debug event ask TEXT` asks the device's model as the row
  does (with a panel open); `./bl debug calendars` says what the system allows and what was read, and `calendars
  pretend Sam,Team,Holidays!` puts a list in place of the device's (the first is the account's own; `!`: takes no
  new events; `pretend off` ends it; nothing can be written while one is pretended); `pref save on|off`, `pref
  eventcal OWNER|none`, `pref calasked yes|no`. `dump` says an event's row with both lines of slots (`●` a
  colour's dot, `?` a guess), `rest=`, `footer=`, a line of its Calendar list as `+c`, `armed=calendar`, and
  `looking` while the model is asked. **The hooks print the names of the user's calendars: never copy one into a
  file of this repo** (the examples are Sam and Team).
- First run's stored state (core `FirstRun`; `docs/design/first-run/`): `./bl debug first` shows the run, what is done, the screen that stands and its
```

Find:

```markdown
  never a coloured word or line on the glass, and never red: red is for what removes something.
```

Make it:

```markdown
  never a coloured word or line on the glass, and never red: red is for what removes something. (A calendar's own
  colour, as a 10 dp dot before its name, is a sample of something that is the user's, like a colour's swatch: not a
  state, and never without the name beside it.)
```

Find:

```markdown
- What the device's model says is only shown, copied, pinned or put back where the text came from: it never
  runs anything and never outranks a local match.
```

Make it:

```markdown
  Saving an event is not one (it adds what the row shows, by the user's own switch), so Save may be armed; but it
  is Enter's alone, on the selected row: never Ctrl + digit, never twice, never a guess.
- What the device's model says is only shown, copied, pinned or put back where the text came from: it never
  runs anything and never outranks a local match. One more thing is allowed since the event's row: the model may
  fill that row's slots, where every part it names was typed and the day and time are the parser's. It still runs
  nothing and saves nothing by itself: the user's Enter saves what the row shows, and every field that will be
  saved stands in the row before Enter. It is asked only after the typing has rested, never for a date or a sum.
```

Find:

```markdown
- While a lesson of first run stands (`OverlayModel.lesson`: also while its list is typed, when `stage` is null), nothing opens:
```

Make it:

```markdown
- An event's row has two lines of slots from its first letter (`Body.Slots.more`), empty ones included: so nothing in
  it moves when the model's answer lands. Do not hide a slot that is empty. The answer lands through
  `OverlayModel.lookUp` and `land`, as a flight's does; when it changes the selected row, Enter is not taken for a
  moment (`filledAt`), so a press made for the row as it stood saves nothing else. The model's ask is the lookup's
  job: any key cancels it (`search`), and `Events.answer` keeps an answer only once it is whole.
- The list of calendars is read where a panel is made and where the window has the keys again
  (`BooklightApp.lookAtCalendars`), never while typing; "Save events without opening Calendar" goes off there when its
  permission is gone. An event is saved by `Executor.save` alone, which asks for the switch and the permission again.
  Nothing in Booklight reads an event: a query of the calendar provider anywhere but `CalendarList` is a bug.
- A line of a row's list that writes into the field (`Effect.Retype`: a calendar chosen for an event) closes the list
  and sets `filledAt`: the Enter that chose it does not also run the row.
- While a lesson of first run stands (`OverlayModel.lesson`: also while its list is typed, when `stage` is null), nothing opens:
```

- [ ] **Step 9: The status, in `docs/PICKING-UP.md`.** One change: the newest entry.

Find:

```markdown
*Living status. Newest first.*

## 2026-10-05 (later): 3.1 is released: first steps
```

Make it:

```markdown
*Living status. Newest first.*

## 2026-10-05 (night): an event from a sentence is built (on branch `event-sentence`; not released)

Alex, on the design: "Yes. Re 2 add save too behind an option. Ship and ship also to production directly."
`docs/design/event-sentence/design.md`, §11.

- **What it is:** an event typed the way it is said, with the keyword `event` or without; one row shows what
  was read (when, title, where, calendar); Enter opens the Calendar app's editor on that calendar, or, with
  "Save events without opening Calendar" on, saves the event. The rules read at every letter; the model on the
  device helps to split and never reckons a date. `CHANGELOG.md`, under "Unreleased", says it for a user.
- **Two new permissions**, both asked for in the Booklight window alone: `READ_CALENDAR` (the list of calendars,
  never an event) and `WRITE_CALENDAR` (the one event the row shows, only with the switch). `docs/PLAN.md` §4 and
  §6, `PRIVACY.md`, `store-submission/forms/data-safety.md`.
- **How it was built:** `docs/superpowers/plans/2026-10-05-event-from-a-sentence.md`, twelve tasks, each one
  commit. Its "Decided here" is a table of what the design left open, each for Alex to overturn.
- **Not checked on a device.** `docs/research/event-sentence.md` has what a device said before the build, and
  thirty checks, every answer "not run". Check 1 first; check 24 is the one that saves an event (one, into the
  account's own calendar, removed after).
- **Before a release** (the version is still 3.1, code 9): the checks; Alex's answers to "Decided here"; the
  version, the changelog's heading and the release notes; the store's listing (a line for the feature, in
  English and German) and its data-safety answers; and because of the two permissions the release is sent
  through the Play Console, not the API.

## 2026-10-05 (later): 3.1 is released: first steps
```

- [ ] **Step 10: Look for what must be so**

```bash
grep -n "^## " CHANGELOG.md | head -2
grep -c "READ_CALENDAR" docs/PLAN.md store-submission/forms/data-safety.md CLAUDE.md docs/PICKING-UP.md
grep -c "Calendar permission" PRIVACY.md
grep -n "3\.2" CHANGELOG.md README.md PRIVACY.md CLAUDE.md docs/PICKING-UP.md docs/PLAN.md docs/design/ux-model.md docs/design/design-system.md store-submission/forms/data-safety.md || echo CLEAN
grep -n "versionCode\|versionName" app/build.gradle.kts
git diff --stat -- app core | grep . || echo CLEAN
```

Expected: `## Unreleased` on line 3 and `## 3.1 (5 October 2026)` after it; four lines, none ending `:0` (the permission has its line in the plan's §4 and §6, in the store's form, in CLAUDE.md and in the status); `1` or more (the privacy text says it in words, for a reader who is no developer); `CLEAN` (no document says that a version 3.2 is out); `versionCode = 9` and `versionName = "3.1"`; `CLEAN` (this task changes no code and no resource).

- [ ] **Step 11: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`.

- [ ] **Step 12: Commit**

```bash
git add CHANGELOG.md CLAUDE.md PRIVACY.md README.md docs/PICKING-UP.md docs/PLAN.md docs/design/design-system.md docs/design/ux-model.md store-submission/forms/data-safety.md
git commit -m "An event from a sentence: the documents say what it is: privacy and the two permissions, the plan, the store's form, the changelog, the README, the two design documents and the status"
```

### Task 12: Checked as a whole

**Files:** none are changed. If a step shows a change that is not committed, it belongs to the task that made it: say which, and stop.

**Interfaces:**
- Consumes: everything above.
- Produces: the report that the plan is whole, for the coordinator.

- [ ] **Step 1: Every core test, with its count**

```bash
./gradlew :core:test --console=plain && ./bl test && for c in WhenTest JotTest SpotTest SentenceTest SplitTest CalsTest StopsTest IconTest SearchEngineTest; do f=core/build/test-results/test/TEST-io.github.kuscher.booklight.core.$c.xml; echo "$c $(grep -o 'tests="[0-9]*"' "$f" | head -1) $(grep -o 'failures="[0-9]*"' "$f" | head -1) $(grep -o 'errors="[0-9]*"' "$f" | head -1)"; done
python3 -c "import glob,re; print(sum(int(re.search(r'tests=\"(\d+)\"', open(f).read()).group(1)) for f in glob.glob('core/build/test-results/test/TEST-*.xml')))"
```

Expected: `BUILD SUCCESSFUL`, no output from `./bl test`, then nine lines, each ending `failures="0" errors="0"`:

```text
WhenTest tests="39"
JotTest tests="34"
SpotTest tests="8"
SentenceTest tests="13"
SplitTest tests="10"
CalsTest tests="7"
StopsTest tests="10"
IconTest tests="3"
SearchEngineTest tests="32"
```

and then `783`: 55 tests are this plan's (eight more in `WhenTest`, three in `JotTest`, the eight of `SpotTest`, the thirteen of `SentenceTest`, the ten of `SplitTest`, the seven of `CalsTest`, two more in `StopsTest`, one in `IconTest`, three in `SearchEngineTest`), and 783 core tests in all (728 before).

- [ ] **Step 2: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 3: The rules hold**

```bash
grep -rn "^import android" core/src/main || echo CLEAN
git diff fdebfb0 -- app/src/main/AndroidManifest.xml | grep "^+" | grep -v "^+++" | grep "<[a-z]"
git diff fdebfb0 -- app/src/main/AndroidManifest.xml | grep "^-" | grep -v "^---"
git diff fdebfb0 -- app/src/main/AndroidManifest.xml app/src/debug/AndroidManifest.xml | grep -c "^+.*queries\|^+.*<package\|^+.*<intent\|^+.*<service\|^+.*<receiver\|^+.*<provider"
grep -c "<queries>" app/src/main/AndroidManifest.xml
git diff fdebfb0 -- app/src/main core/src/main | grep -E "^\+.*(HttpURLConnection|openConnection|URL\(|\.fetch\(|Socket\(|okhttp)" || echo CLEAN
grep -rn "CalendarContract" app/src core/src --include='*.kt' | grep -v "booklight/Executor.kt\|booklight/device/CalendarList.kt" || echo CLEAN
grep -n "contentResolver.query" app/src/main/java/io/github/kuscher/booklight/device/CalendarList.kt
grep -o "Calendars\.[A-Z_a-z]*" app/src/main/java/io/github/kuscher/booklight/device/CalendarList.kt | LC_ALL=C sort -u | tr '\n' ' '; echo
grep -n "contentResolver.query\|contentResolver.update\|contentResolver.delete\|\.query(" app/src/main/java/io/github/kuscher/booklight/Executor.kt || echo CLEAN
grep -rn "Effect.SaveEvent(" app/src/main --include='*.kt' | wc -l
grep -rn "requestPermissions\|RequestMultiplePermissions\|RequestPermission()" app/src/main --include='*.kt' | grep -v "window/MainActivity.kt" || echo CLEAN
grep -n "WorkManager\|JobScheduler\|AlarmManager\|startService\|startForegroundService" app/src/main/java/io/github/kuscher/booklight/providers/Events.kt app/src/main/java/io/github/kuscher/booklight/device/CalendarList.kt || echo CLEAN
grep -n "versionCode\|versionName" app/build.gradle.kts
git diff fdebfb0 --stat -- app/build.gradle.kts core/build.gradle.kts build.gradle.kts settings.gradle.kts gradle | grep . || echo CLEAN
git log fdebfb0..HEAD --format=%B | grep -inE "co-authored-by|claude|generated with|session" || echo CLEAN
git diff fdebfb0 -- . ':!docs/superpowers' | grep "^+" | grep -inE "co-authored-by|claude-session|generated with" || echo CLEAN
git diff fdebfb0 -- docs README.md PRIVACY.md CHANGELOG.md CLAUDE.md store-submission bl ':!docs/superpowers' | grep "^+" | grep -nE "emulator-|ANDROID_SERIAL=[A-Z0-9]|@gmail\.com|[a-z0-9]@group\.calendar\.google\.com" || echo CLEAN
git diff fdebfb0 -- app/src/main/res/values | grep -c "^+ *<string name"; git diff fdebfb0 -- app/src/main/res/values-de | grep -c "^+ *<string name"
grep -n '>[^<]*"[^<]*<' app/src/main/res/values-de/strings_33.xml || echo CLEAN
grep -c "^Answer: not run$" docs/research/event-sentence.md
git log --oneline fdebfb0..HEAD -- . ':!docs/superpowers' | wc -l
git status --short
```

(This plan's own file is left out of the three commands that name `':!docs/superpowers'`: it quotes the very words they look for, and its commit, if it stands on the branch, is none of a task's.)

Expected, in this order:
- `CLEAN` (no Android in core);
- two lines, the two `<uses-permission>` of the Calendar and nothing else of the manifest's elements; then one line, the comment "No runtime permissions, no accessibility service." that was replaced; `0` (no line of `<queries>`, no component); `1` (the one `<queries>` block that was there);
- `CLEAN` (no network call added);
- `CLEAN` (the calendar provider is named in two files); one line, the list's one query, of `Calendars.CONTENT_URI`; then what that file names of the calendars' table, and only this: `Calendars.ACCOUNT_NAME Calendars.ACCOUNT_TYPE Calendars.CALENDAR_ACCESS_LEVEL Calendars.CALENDAR_COLOR Calendars.CALENDAR_DISPLAY_NAME Calendars.CAL_ACCESS_CONTRIBUTOR Calendars.CONTENT_URI Calendars.IS_PRIMARY Calendars.OWNER_ACCOUNT Calendars._ID` (**no event is read anywhere**: nothing of the events' table is ever asked for); `CLEAN` (the executor reads, changes and removes nothing);
- `1` (one place makes the effect that saves: the event's row; `wc -l` may print spaces before its number);
- `CLEAN` (the system is asked for a permission in the window's activity and nowhere else); `CLEAN` (nothing in the background);
- `versionCode = 9` and `versionName = "3.1"`; `CLEAN` (no build file changed);
- twice `CLEAN` (no attribution in the commits or in what they add); `CLEAN` (no serial and no address in the documents: the examples are Sam and Team);
- `22` and `22` (the new and reworded strings, in both languages: eleven of the window, ten of the row, and the placeholder); `CLEAN` (no straight quotation mark in a German string);
- `30` (every check still reads "not run"); the number of commits since `fdebfb0` (`11`: one for each of Tasks 1 to 11, and one more for each fix a review asked for); and no line from `git status` (but this plan's own file, where it was not committed).

- [ ] **Step 4: Report.** The commits of Tasks 1 to 11 by their first lines, the results above, that the branch was not pushed and nothing was released (the version is 3.1, code 9), and that the thirty device checks in `docs/research/event-sentence.md` are all "not run": check 1 first (the panel still opens, nothing is asked of the system, and `./bl debug guide` still finds every example), and check 24 last (the one event that is saved, into the account's own calendar, and removed).

---

## What is left after this plan

Nothing of the feature is left to plan. What is left is to look, to decide and to release.

| What | Whose | Where it is written |
| --- | --- | --- |
| The checks on the test device marked [C]: 1 to 6, 8 to 11, 13 to 19, 21, 22, 24, 26 to 29, and the coordinator's half of 7, 23 and 25 | The coordinator | `docs/research/event-sentence.md` |
| The checks marked [A]: Android's own question answered by a hand (20), Enter in the moment the model's words change (12), the Calendar app's editor closed without saving (7, 23), Ctrl + digit on a row that saves (25), the HP Googlebook and what only a person can say (30) | Alex | The same file |
| What the device says must be tuned: the rest before the model is asked (`Events.REST_MS`), how long its answer may take, the third of a second an Enter is not taken after the row changed (`OverlayModel`'s `CONFIRM_GAP_MS`), the dot's size (`DOT`) | Whoever looks | The constants named |
| Two things only a device can say: whether the Calendar app shows the calendar's default reminder on a saved event (check 24), and whether the system's list of calendars is read without a `<queries>` line, as a system provider's should be (check 21) | The coordinator | The same file |
| Alex's answers to "Decided here", above all 1 (two lines of slots, always there), 3 ("book" and "plan" stay in the title), 10 (Save armed, and no Undo) and 14 (the row points at the calendars until Android's question was answered) | Alex | The table above |
| What this plan left out (row 23): repeats, guests, a reminder of one's own, reading events, Undo; and "new event …" typed in the panel (row 22) | Alex, to ask for or to leave | "Decided here" |
| The release: the version (3.2), the changelog's heading and the release notes, the store's listing (a line for the feature, English and German) and its data-safety answers, the tag, Play: because of the two permissions it is sent through the Play Console, not the API | The coordinator, on Alex's word | `docs/PICKING-UP.md`; `docs/RELEASING.md` |
| The branch: `event-sentence` is local and not pushed | Alex's word | `docs/PICKING-UP.md` |
