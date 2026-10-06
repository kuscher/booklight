# An event from a sentence

*Approved by Alex on 5 October 2026: "Yes. Re 2 add save too behind an option. Ship and ship also to production
directly." §11 says what that settles; it wins over the sections before it where they differ.*

His words: "could we be using it to add a calendar entry just from typing out freeform text … and have it intent
into calendar"; then "design the event from a sentence to calendar. Get the calendar permission too if that helps so
we can even better match calendars like we can autocomplete even?"

## 1. What it is

Type an event the way you would say it, in any order, and one row shows what Booklight understood: the title, the
day and time, the place, the calendar. Enter opens the calendar's editor with all of it filled in, on that
calendar. You save it there.

It builds on the `event` row that exists (`event dinner tomorrow 7pm` works today). Three things are new: the
sentence may be in any order and needs no keyword; a calendar can be named and is completed as you type; and the
model on the device helps to split a sentence our own rules cannot.

## 2. What the device said (the Lenovo Googlebook, 5 October 2026)

- **The calendar.** On a Googlebook the Calendar app opens the web calendar in a window of its own. The Android
  intent that inserts an event carries the title and the time, and **ignores a calendar's id**: the editor opens on
  the primary calendar. **A calendar link that names the calendar is honoured**
  (`…/calendar/render?action=TEMPLATE&text=…&dates=…&location=…&src=<the calendar's id>`, sent to the Calendar app):
  the full editor opened with the title, the time and a second calendar chosen. Times sent in UTC land right.
- **The model as a splitter.** Sixteen sentences in English and German, one instruction ("one line of JSON: title,
  when, place, calendar; copy the words exactly"): 1.1 to 2.3 s each once loaded, about 6 s the first time. Fourteen
  answers were well-formed and two had a broken last value that can be repaired. In every well-formed answer each
  part was a piece of what was typed: nothing was invented. Its faults: a calendar's name also given as the place
  once, a place taken from the title once, two words of a title dropped once, a German verb left in a title once.
- **The two together.** The model's split, with our own parser reading the day and time from the model's "when":
  9 events of 16 right from end to end. Our parser alone, with the keyword `event` in front: 3 of 16. What still
  failed was our parser, not the model (§6).

## 3. The row

The event row as it is, with one more slot. Nothing floats, nothing new is drawn.

```
  Add dinner with Sam tomorrow at 7pm to tea|m calendar                               esc
 ┌──────────────────────────────────────────────────────────────────────────────────────┐
 │ (+)  New event. Opens in your calendar to save.                                      │
 │      Title     Dinner with Sam                                                       │
 │      When      Tue 6 Oct, 7–8 PM                                 [ + Create ⏎ ]  ⧉   │
 │      Calendar  ● Team                                                                │
 └──────────────────────────────────────────────────────────────────────────────────────┘
      Search Google for “Add dinner with Sam …”                                      Web
```

- **Slots**: Title, When, Where (only where a place was read), Calendar (only where one was named, with the
  calendar's own colour as a dot). A slot that is a guess is drawn as a guess, as "When" is today.
- **Actions**: Create (Enter), Copy. With calendars allowed, a third stop, **Calendar**: Tab to it, and a list of
  your calendars opens under the row, as an app's Window list does; Enter on one sets it. One list, never a list
  inside a list.
- **The row never jumps.** It has its height from the first letter. Our own reading is there at once; if the model
  then reads it better, the words in the slots change where they stand.
- **Completion.** After `to`, `in` or `on` (German `in`, `in den`, `im`) at the end of the sentence, the letters of
  a calendar's name are completed in grey in the field, as an app's name is today, and Right takes it. "tea"
  completes to "Team"; the word "calendar" after it is not needed.

## 4. How a sentence is read

**First our own rules, at every letter** (core, tested, instant):
1. The day and time are found **anywhere** in the sentence, not only at its ends (the parser of today, widened: §6).
2. A calendar is found by its name: the words after `to`/`in`/`on` at the end, with or without "calendar" /
   „-Kalender“, matched against your calendars without regard to case, to "the" and to a plural "s".
3. A leading `add`, `schedule`, `put`, `book`, `plan`, `new event` (German `trag … ein`, `plane`, `neuer Termin`)
   is dropped. What is left is the title; `@ place` is the place, as today.

**Then the model, only where it can help**: where words are left that our rules cannot place (a place without `@`,
an odd order), once the typing has rested for 0.7 s, once for a text. It is asked to split, never to reckon.
- Every part it returns must be a piece of what was typed, or the answer is dropped.
- The day and time are always read by our parser from the model's "when". If the parser cannot read it, our own
  reading of the sentence stands.
- A calendar it names must match one of yours; a place that is also the calendar's name, or part of the title, is
  dropped.
- No answer, a slow answer, no model on the device: the row shows our own reading. Nothing waits for the model,
  and typing is never held up.

**Without the keyword** the row is offered where the sentence begins with one of the words of rule 3 and holds a
day or a time. It stands below anything of the device that matches (an app, a setting), above the web.

## 5. The calendars

- **One new permission: "Calendar", to read.** Booklight reads the **list of your calendars** (name, colour, id)
  and nothing else: no event is ever read, and nothing of the list leaves the device. It is off until you allow it:
  a row "Calendars" in the Booklight window › Privacy, with "Allow…", as Brightness has. The first time a sentence
  names a calendar and the list is not allowed, the row says so in its footer and offers the same step.
- **Without it everything else works**: the sentence, the day, the place. The editor opens on the calendar the
  Calendar app chooses, and a name you typed stays in the title.
- **Enter** sends the calendar link of §2 to the Calendar app. Where no Calendar app takes it, the Android intent
  of today is used, without a calendar.
- **No calendar named**: no calendar is sent, and the Calendar app chooses, as today.

## 6. What our parser must learn

From the sixteen sentences. All of it is core, with tests, English and German:
- a day and time in the middle of a sentence;
- a range of hours (`9-10am`, `from 9 to 10`, „von 9 bis 10“) and of days (`Oct 14 to Oct 16`, all day);
- `the 12th`, `on the 12th at 6:45 in the morning`, `tmrw`, `all day` / „ganztägig“;
- left for later, and said as a guess meanwhile: `every Monday` (the link can carry a repeat; not in this version).

## 7. What it changes

- **A permission** (`READ_CALENDAR`): Alex's word of 5 October. Inert until allowed in the window. `docs/PLAN.md`
  §4 and §6, `PRIVACY.md` and the store's form get its line ("reads the names of your calendars, on the device").
  A release with a new permission is sent through the Play Console, not the API.
- **The rule on the model** (CLAUDE.md, "What the device's model says is only shown, copied, pinned or put back"):
  one more thing is allowed. The model may fill the slots of a row whose Enter opens another app's editor, where
  the user sees everything and saves it himself. It still runs nothing, saves nothing and outranks nothing.
- Nothing in the background, no new connection.

## 8. Not in this version

- **Writing the event straight into the calendar** (a second permission, no Calendar window, "Added · Undo"). It
  is the faster way, and a wrong split would then be saved unseen. Later, behind a switch, once the split has
  proven itself.
- Repeats, guests, reminders, a video call.
- Reading your events (for "what is next", "am I free").

## 9. Decisions for Alex

Each has a recommendation; "yes" to the whole takes them all.

| # | Decision | Recommended |
| --- | --- | --- |
| 1 | The permission is "read" alone, asked in the window, and Booklight reads only the list of calendars | Yes |
| 2 | Enter opens the Calendar editor filled in; you save there. Writing directly comes later, if at all | Yes |
| 3 | No keyword is needed where a sentence begins with add, schedule, put, book, plan (and their German words) and holds a day or a time | Yes |
| 4 | The model is asked only where our rules leave words over, after 0.7 s of rest; the row never waits for it | Yes |
| 5 | With no calendar named, the Calendar app chooses (Booklight sends none) | Yes |
| 6 | The model may fill a row that opens an editor (the rule of §7) | Yes |
| 7 | It goes out as 3.2, to closed testing first | Yes |

## 10. How it will be checked

- Core: the sixteen sentences of the probe and their like as a table of tests (the reading, the calendar's match,
  the link that is built), in English and German; the model's answers of the probe as fixtures for the checks of §4
  (a broken answer, an invented word, a calendar as a place).
- On a device: the row at every letter of the example sentence; the completion; the list of calendars; the editor
  that opens, on the right calendar, never saved by a check; with the permission refused; with no model.

## 11. Settled for the build (Alex, 5 October 2026)

1. **All seven decisions of §9: yes.**
2. **Saving directly is built too, behind an option** (his words: "add save too behind an option"). §8's first
   point is therefore in this version:
   - A switch in the Booklight window, **"Save events without opening Calendar"**, off unless chosen. Turning it
     on asks for the Calendar permission to write (and to read, where that was not allowed yet). Refused: the
     switch stays off.
   - With it on, the event row's Enter, or a click on it, is **Save**: the event is written into the calendar
     that was named, or into the one chosen under "New events go to" in the window (the primary calendar until
     another is chosen). The footer says "Saved to" and the calendar's name, and the panel goes, as it does
     after Copy. The row's second action, **Open in Calendar**, opens the editor filled in, as Enter does with
     the switch off; Copy stays. (In the row its label is the short word, "Open": on a Googlebook the long one,
     unrolled, cut the title away. The row's caption says where it opens, and says "Enter saves it" only while
     Save is the armed action.)
   - **A guess is never saved.** Where the day or the time is a guess (the slot is drawn as one), Enter is "Open
     in Calendar" even with the switch on. Saving needs a day and a time that were read from the sentence, or a
     day with "all day".
   - What is saved is exactly what the row shows: title, start and end (or all day), place, calendar; the
     calendar's own default reminder; the device's time zone. No guests, no repeat.
   - The model's rule (§7) then reads: the model may fill the slots of the event row. It runs nothing and saves
     nothing by itself; the user's Enter or click saves what the row shows, and every field that will be saved
     stands in the row before it.
   - As built, after the reviews of 6 October (each for Alex to overturn): a guess is also a time that does not
     say its half of the day ("at 7", "7:30", „um 7“; "7pm", "19:30", „19 Uhr“ and "7 in the evening" say it), a
     line that asks for a repeat by any word, a line that holds a second day or time, and a calendar read by the
     first letters of its name until the name is whole. And an Enter pressed before the row was on the glass
     never saves: the row stands, and the next Enter does.
   - Ruled in the last round of fixes, 6 October (each for Alex to overturn):
     - **"next Tuesday" is never today, and always a guess.** Typed on a Tuesday it is a week on; on any other
       day the coming Tuesday, as before. People mean two different days by it, so Enter opens the editor.
     - **Schedule, put, book, plan and „plane“ are everyday searches too.** Without the keyword, a line that
       begins with one of them is an event's row only with a day and a time of day, both („plane“ only where
       the day or the time was said in German), and that row never has Save: with the switch on, "book flight
       to boston friday 9am" and Enter opens the editor. "Add", "new event", „trag … ein“ and „neuer Termin“
       need a day or a time and are saved, as before; so is any line under the keyword `event`.
     - **Under the keyword `event`, what 3.1 read is read.** Two bare numbers at one end of the line are the
       time of a day at the other ("event 9-10 standup tomorrow"), a guess at the half of the day; and no first
       word is dropped but "new event" ("event put out bins thu 7am" keeps its "put"). Without the keyword two
       bare numbers are no time ("add 1-2 eggs tomorrow").
     - **With no calendar to name, Enter sends what 3.1 sent**: the request to add an event, to whichever
       calendar app there is. The calendar link, which goes to Google's Calendar app, is for an event that names
       a calendar (or, with saving on, shows the one it would be saved to). Someone who allows nothing gets
       3.1's editor.
     - **„9 Uhr“ and "09:30" say their half of the day**: „N Uhr“ and an hour with a zero before it are read
       by the 24-hour clock. A bare "at 7", "7:30", „um 7“, "9-5" and "at 12" stay guesses. (Which „N Uhr“ are
       sure was ruled again in the third round, below.)
     - **A date without a year that had passed is a guess** where it lands more than 300 days ahead ("Oct 5"
       typed on 6 October); "Jan 15" typed in October is next January, and sure. The row's When says the year
       wherever it is not this year's. A year that was typed is as typed.
     - **The switch is this device's own.** "Save events without opening Calendar" is believed only beside a
       mark that no backup holds, as "asked" is: settings that arrive with a restore never have saving on.
   - Ruled in the third round of fixes, 6 October (each for Alex to overturn):
     - **A net under every rule: a number standing alone, or a word of time nobody read, left in the title
       makes the reading a guess** ("Zahnarzt 30", "2 hour workshop", "dinner next week", "7pm EST"; not a
       number inside a word, "Q3", and not the place), so what no list of sentences foresees opens the editor;
       its known cost is a title with a number of its own ("Sprint 12 planning tomorrow 9am"). A title's own
       "night" or "morning" is no such word where the time that was read is in that half of the day ("movie night
       Friday 8pm" is saved; „Essen Freitag Abend 9 Uhr“ is a guess), and "1:1" is no time.
     - **„9 Uhr“ to „12 Uhr“ say their half of the day, „1 Uhr“ to „8 Uhr“ do not**: with no zero before
       them („08 Uhr“ says it) and no word for the half („7 Uhr abends“) they are read by the 24-hour clock,
       as before, and are a guess, alone and as the start of a span („2-3 Uhr“, „von 1 bis 3 Uhr“; „von 9
       bis 11 Uhr“ is sure): people say „um 7 Uhr“ for the evening.
     - **After "on" and "in" a calendar is its name as the calendar has it, or has the word "calendar" after
       it**: "… 9am on Teams" is where the standup is held, and stays in the title; the plural and singular
       forms name a calendar only after "to", "into" and "onto" or with the word ("… to Teams", "… on the
       Teams calendar").
3. **Two permissions**, both inert until the user allows them in the window: `READ_CALENDAR` (the list of
   calendars: names, colours, ids; never an event) and `WRITE_CALENDAR` (only with the switch of point 2, and only
   to add the event the row shows). Booklight never reads, changes or removes an event.
4. **It goes out as 3.2**: GitHub, Play's closed testing, and production (his words: "Ship and ship also to
   production directly").
