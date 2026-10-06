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
as the next Monday and shown as a guess. So is the one that says an hour without its half of the day („um 12“:
read as noon, as before), and the two with "next Tuesday" and „nächsten Dienstag“ (the coming Tuesday, as before:
people mean two different days by it): none of them is saved without the editor. Since the third round of fixes
one more of the sixteen is a guess: „8 Uhr“ is read as the 24-hour clock's eight in the morning, as before, but an
hour up to eight with „Uhr“ is said for the evening as often. ("Team bowling night" at 8pm is no guess: "night" is
the title's own word where the time that was read is in the night.)

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
window, the HP Googlebook, a window dragged to another width, or a person's judgement.

**Before the first check**

- Choose the device by its model (`adb devices -l`), never by its place in the list; set `ANDROID_SERIAL`.
- Close the Booklight window, then `./bl install`. `./bl idle` before every `./bl open`: the panel takes the keyboard.
- After `./bl stop` the hooks are not heard until a panel has been opened once: `./bl open stay` first.
- Never `uiautomator dump`. No sign-in is made for a check. A capture of the screen shows real windows: none is
  kept or committed. `./bl shot NAME` is the panel's own window, `./bl wshot NAME` the Booklight window's.
- **This file is public.** `./bl debug calendars`, `./bl debug event …` and the dump print the names of the
  user's own calendars, and a link's `src=` is an address; a dump and `./bl debug find` also name apps of the
  device. An answer says "a calendar", "the account's own calendar", "three calendars", "an app", never a name
  or an address. The examples are Sam and Team.
- **The state a check starts from is its first line**: whether the calendars are pretended and whether saving is
  on. It is set by the commands named there and read back before anything else: `./bl debug calendars` says
  `pretended=true` or `pretended=false`, and `save=true` or `save=false` (other words stand between them in its
  line: `allowed=`, `writes=`, `asked=`, `count=`). A check whose state does not read back as its first line says
  is not run.
- **One event is saved by these checks, once: check 24.** Into the account's own calendar, never one that is
  shared with other people, with the title "Booklight test (remove me)"; it is verified and removed over adb, by
  the two commands that check gives, and never by looking into the Calendar app. **No other check can save**:
  every check before 24 runs with saving off, or with a pretended list; check 22, the one that has saving on with
  the real list, types nothing and presses no Enter; and every check after 24 begins with `./bl debug pref save
  off` or with a pretended list (`./bl debug calendars pretend …`, `pretended=true` read back before saving is
  switched on). While a list is pretended nothing can be written, whatever the switch and the permissions say
  (`Executor.save` refuses first of all): Enter on a Save then says "Couldn’t do that" (`flash=` in the dump).
- **A pretended list lives in Booklight's process, and an Enter that opens another app ends that process on a
  Googlebook** (the panel's task is removed): the list is then the device's own again. So `./bl debug calendars`
  is read, and says `pretended=true`, after every Enter that opened something, and always directly before `./bl
  debug pref save on`. Where it says `pretended=false`: `./bl debug pref save off` first, then the list again.
- **The Calendar app's editor is another app's window.** Where a check opens it (7, 23, 31), it is read from a
  capture cut to that window, nothing is typed or clicked in it, and it is closed by its window's own close
  button, without saving.
- **Real keys** go only into Booklight's own focused window (`./bl idle` says which has the focus), and never a
  letter and Enter back to back: with the switch on, Enter on an event's row writes. (An Enter that comes before
  its list is what `./bl debug rush` is for, under a pretended list: check 31.)
- **The hooks.** `./bl debug event TEXT`: what the rules read of TEXT (`title=`, `when=`, `place=`, `calendar=`,
  `rest=` for the start of a calendar's name, `named`, `cue`, `everyday`, `loose`, `said=`; after `when=`, where
  the reading is a guess, why: `(a guess: no day, no time, half of the day not said, repeats, a second day or
  time, the date has passed, “next” means two days to people, a weekday by two letters, which number is the
  month was not said, which night was not said, a word beside the day or the time changes it, a number or a word
  of time is left in the title, it ends where it starts, it ends on the next day, more than twelve hours on, this
  week or the next was not said)`), `offered=` (a line typed without the
  keyword would be an event's row), `asks=` (the model would be asked), `enter=` (what Enter would do: "would
  send" a link where there is a calendar to name, "would send the insert request" where there is none, or
  "would write" the values; with a pretended list "would write" ends in `(pretended: Enter writes nothing)`),
  and the row as the dump says it. TEXT is read as the bare field reads it: `./bl debug event "event 9-10
  standup tomorrow"` is the keyword's line (the hook says `keyword`, and `offered=keyword`), `./bl debug event
  "Add dinner tomorrow 7pm"` a sentence typed without one. It needs no panel.
  `./bl debug event ask TEXT`: the model asked as the row asks it, at once; with a panel open.
  `./bl debug calendars`: `allowed=`, `writes=`, `pretended=`, `asked=`, `save=`, `count=` and the calendars.
  `./bl debug calendars pretend Sam,Team,Team trips,Holidays!` puts that list in place of the device's (the first
  is the account's own; a name that ends in `!` takes no new events); `./bl debug calendars pretend off` ends it.
  `./bl debug pref save on|off`, `pref eventcal OWNER|none`, `pref calasked yes|no`. `./bl debug key calendar`
  arms an event's Calendar stop. `./bl debug rush TEXT` sets TEXT and presses Enter in the same turn, before the
  list for it can have landed; it refuses (`not an early Enter: …`) where the field holds TEXT already, and
  where saving is on with the device's own calendars. `./bl debug calendars` marks a calendar `(not offered)`
  whose name would not read back as itself (the second of two with one name, a name of signs alone): it is no
  line of the row's list. The dump says an event's row with both lines of slots (after a bar the second; `●` a
  colour's dot; `?` a guess), its caption and, after `while save:`, `while copy:` and `while calendar:`, what the
  caption says while that action is armed, `rest=`, `footer=`, a line of its Calendar list as `+c`,
  `armed=calendar` on that stop, `completion=`, `flash=` (the footer's word of the moment: "Saved to …",
  "Couldn’t do that"), and `looking` while the model is asked.
- **A weekday that is the day a check is run on** is a guess of its own since the third round of fixes (`this week
  or the next was not said`: check 38). A check's line with such a weekday in it has that reason too, and where the
  check says "no guess" of it, it is one, with `enter=open`: read that line with this in mind, or run the check on
  another day. ("Tomorrow", "today" and a date are never that guess.)
- **The permission by adb**, where a check says so: `./bl sh pm grant --user current io.github.kuscher.booklight
  android.permission.READ_CALENDAR` (and `WRITE_CALENDAR`), back with `./bl sh pm revoke --user current
  io.github.kuscher.booklight android.permission.READ_CALENDAR` (and `WRITE_CALENDAR`), which also ends
  Booklight's process (`./bl open stay` again after it). The system's own question, answered by a hand, is check 20.
- Write down what `./bl debug pref` and `./bl debug calendars` say before the first check. "After the last check",
  at the end of this list, puts everything back.
- **In German.** `adb shell cmd locale set-app-locales io.github.kuscher.booklight --user 10 --locales de` (the
  user by its number, the one `adb shell am get-current-user` says: `current` is not taken here), and back with
  `--locales ""` **at the end of every check that asked for German**: the checks after it expect English.
- The days in the expectations are those of October 2026, when the list was written (the 14th is ahead). Run
  later, a date without a year is its next one.

## Someone who allows nothing

### 1. The panel still opens, and nothing is asked [C]

*State: nothing allowed, not pretended, saving off.*

`./bl open stay`, `./bl debug dump`, `./bl shot plain`; again in the dark theme, opened by `DARK=true ./bl open
stay` (the theme is read by `./bl open`, not by `./bl shot`). `./bl debug calendars`. A few ordinary lines by
`./bl debug keys` (an app's first letters, a sum, `note x`). `./bl debug guide`. `./bl logs`.

The panel as before in both themes. `./bl debug calendars` says `allowed=false`, `writes=false`,
`pretended=false`, `save=false` and `count=0`: nothing was asked of the system, and the system showed no
question. Every example of `./bl debug guide` is `ok`, the event's among them (`event dinner with Sam tomorrow
7pm`). No crash.

Answer (the Lenovo Googlebook, 6 October 2026): yes. The plain panel in both themes as before; `allowed=false writes=false pretended=false save=false count=0`, the system showed no question; the guide's examples are `ok`, the event's among them (one `BAD`, `clip`, is the test device's empty clipboard and not this build's). An app's letters, a sum and `note x` as ever. No crash.

### 2. What the parser reads now, under the keyword [C]

*State: as check 1. No panel is needed.*

`./bl debug event "event TEXT"` for each of (the keyword and a space before it: the keyword's line, and the hook
says `keyword`):

1. `dinner with Sam tomorrow at 7pm at Cafe Luna`
2. `meeting tmrw 9-10am`
3. `Flight to Frankfurt on the 12th at 6:45 in the morning`
4. `Standup tomorrow from 9am to 10am` · `Standup tomorrow from 9 to 10`
5. `Dinner tomorrow at 7` · `Abendessen morgen um 7 Uhr abends` · `Party Saturday 12 at night`
6. `Team offsite Oct 14 to Oct 16 @ Lisbon` · `Messe vom 14. bis 16. Oktober`
7. `Birthday party for Sam on November 3rd all day` · `Geburtstag von Oma am Samstag ganztägig`
8. `yoga class every Monday 6pm` · `weekly sync Friday 9am`
9. `Dentist` · `2nd interview tomorrow` · `I worked all day`
10. `dinner at 5 Main Street tomorrow` · `Sprint Oct 5 to Dec 20` · `Mon 3pm Tue 4pm`

In this order:

1. Tomorrow 19:00 to 20:00, the title with "at Cafe Luna" in it, and `loose`.
2. Tomorrow 9:00 to 10:00.
3. The 12th at 6:45, title "Flight to Frankfurt".
4. Tomorrow 9:00 to 10:00, twice; the second with `(a guess: half of the day not said)`.
5. Tomorrow 19:00 with `(a guess: half of the day not said)` · tomorrow 19:00 and no guess („7 Uhr abends“ is
   seven in the evening) · Saturday at 00:00 with `(a guess: which night was not said)`.
6. The 14th to the 17th at midnight, "all day", no guess; the first with the place "Lisbon".
7. 3 November, "all day", no guess · Saturday, "all day", no guess.
8. The next Monday at 18:00 and `(a guess: repeats)` · Friday at 9:00 and `(a guess: repeats)`.
9. Today, all day, `(a guess: no day, no time)` · tomorrow, title "2nd interview", `(a guess: no time, a number or
   a word of time is left in the title)` · today, the whole line as the title, `(a guess: no day, no time)`: "all
   day" with no day is neither a day nor a time.
10. Tomorrow, all day, the title "dinner at 5 Main Street" with its 5 in it (which the reasons name: `a number or a
    word of time is left in the title`) · 20 December, the title "Sprint Oct 5 to", `(a guess: no time, a second
    day or time, a number or a word of time is left in the title)`: no range of December's days is made of it ·
    Monday 15:00, the title "Tue 4pm", `(a guess: a second day or time, a number or a word of time is left in the
    title)`.

Answer (the Lenovo Googlebook, 6 October 2026): yes, each as written. And under the keyword, as 3.1 read it: `event 9-10 standup tomorrow` is 09:00 to 10:00, a guess for the half of the day.

### 3. The row has its two lines from the first letter, and nothing in it moves [C]

*State: as check 1.*

`./bl open stay`, then `./bl debug keys "event dinner with Sam tomorrow at 7pm @ Cafe Luna"`, with `./bl debug
dump` and `./bl shot` after `event d`, after `tomorrow`, after `7pm` and at the end (four panels, typed anew each
time: `keys` types from an empty field).

`window=` says the same height, in pixels, in all four dumps (the row is 92 dp: the capture's height in pixels,
less the field's and the footer's, divided by the device's density, says so once). Its caption, then `WHEN` and
`WHERE` on one line, then `TITLE` on the line under it: the same three lines at the same places in all four
pictures. An empty slot is its label and a short rule. "When" is a step lighter while it is a guess (`?` in the
dump) and full once day and time are both read. No Calendar slot: the calendars are not allowed.

Answer (the Lenovo Googlebook, 6 October 2026): yes. `window=1080x402` in all four dumps (268 dp; the row 92 dp). The caption, `WHEN` and `WHERE`, then `TITLE`: the same three lines in the four pictures, an empty slot its label and a short rule, "all day" a step lighter while it is a guess. No Calendar slot.

### 4. A sentence without the keyword [C]

*State: as check 1.*

`./bl debug keys "Add dinner with Sam tomorrow at 7pm"`, with `./bl debug dump` after `Add dinner with Sam` and at
the end. Then `./bl debug find` for a line that is an event and finds something of the device too (the first
letters of an app's name after `add` and before a day: whatever this device has; the answer names no app; a
device may have nothing that matches such a line, and then the order is core's test, `SearchEngineTest`). Then
`./bl debug event TEXT` for: `add pictures of the sun` · `add 2-3 eggs` · `add 1/2 cup sugar` · `plan tomorrow` ·
`Add dinner with Sam` · `Lunch with Sam on Friday at noon` · `Book club Friday 7pm` · `add 1-2 eggs tomorrow`.

After `Add dinner with Sam`: no event's row (no day, no time). At the end: the row `sentence:event` is row one,
selected, `create` armed, and under it the question for Gemini and the web's search. In the `find`: whatever of
the device matches stands over the event's row, the web's rows under it. The eight: `offered=false` (a short
weekday alone) · `offered=false` (two bare numbers) · `offered=false` (a fraction is no date) · `offered=false`
(a cue that stays in the title is no title by itself) · `offered=false` (no day or time) · `offered=false` (no
cue; under `event` it is read) · `offered=true`, title "Book club" · `offered=true`, tomorrow, all day, the title
"1-2 eggs": the two numbers are no time.

Answer (the Lenovo Googlebook, 6 October 2026): yes for the dumps and the eight (and `add milk tomorrow` is offered, `I worked all day` is not). After `Add dinner with Sam`: no event's row. At the end: the event's row is row one, selected, `create` armed, the question for Gemini and the web's search under it. The `find`: this device has nothing that matches such a line (a dozen tried), so the order is core's test.

### 5. What Enter would send [C]

*State: as check 1.*

`./bl debug event "Add dinner with Sam tomorrow at 7pm"`, and the same with `@ Cafe Luna` at its end.

`enter=create: would send the insert request (no calendar to name: as 3.1 did), title='dinner with Sam' start=…
end=… allDay=false place=''`: tomorrow 19:00 and 20:00 of this device's zone. No link: with no calendar to name,
Enter sends what 3.1 sent. With the place: `place='Cafe Luna'`. (A link is sent where the sentence names a
calendar: check 15.)

Answer (the Lenovo Googlebook, 6 October 2026): yes. No calendar to name: `would send the insert request (no calendar to name: as 3.1 did)`, tomorrow 19:00 to 20:00, and with the place `place='Cafe Luna'`.

### 6. `remind`, and a date that was copied, are as they were [C]

*State: as check 1.*

`./bl debug find "remind tomorrow 5pm call"`, `./bl debug find "remind call at 7"`, `./bl debug find "remind all
day sale"`. `./bl debug copy "Dinner with Sam on Friday at 7pm. See you"`, `./bl open stay`, `./bl debug key
tab`, `./bl debug dump`.

A reminder for tomorrow 17:00 as before; "at 7" is this evening's alarm (or tomorrow's, after seven), as before:
a reminder knows no guess; "all day sale" has no day and no time to ring at, as before. The copy's rows have the
date's row ("Fri …, 7–8 PM", "Dinner with Sam"), and its Enter goes on in the event's row.

Answer (the Lenovo Googlebook, 6 October 2026): yes. Tomorrow 17:00 as a calendar event, "at 7" as this evening's alarm, "all day sale" asks for a time. The copy's rows begin with "Fri 9 Oct, 7–8 PM (Dinner with Sam)", and its Enter goes on in the event's row.

### 7. The editor opens, filled in, and is not saved [C] and [A]

*State: not pretended, saving off (`save=false` read back: this check presses Enter on an event's row).*

`./bl open stay`, `./bl debug keys "Add Booklight test dinner tomorrow at 7pm"`, `./bl debug dump` (the event's
row is the selected one, `create` armed), a moment later `./bl debug key enter`.

The Calendar app's editor opens in a window of its own with the title "Booklight test dinner" and tomorrow 7 to 8
PM, on the calendar the Calendar app chooses. Booklight's panel has gone. The editor is closed by its window's
own close button, without saving.

Answer (the Lenovo Googlebook, 6 October 2026): yes. With nothing allowed the insert request went out: Calendar's window came to the front with its quick-create bubble on 7 October and the title "Booklight test dinner", as 3.1 opened it; Booklight's panel had gone. Closed by the window's own close button, unsaved; the provider held no such event. [A]: by hand, open.

## The model

### 8. The model splits what the rules could not [C]

*State: not pretended, saving off.*

`./bl open stay`, then `./bl debug event ask TEXT` for: `Schedule dentist appointment next Tuesday at 3:30pm at Dr.
Sam office` · `Team offsite Oct 14 to Oct 16 in Lisbon` · `Elternabend am 14.10. um 19:30 in der Schule` · `Flight to
Frankfurt on the 12th at 6:45 in the morning`.

Each: `ai=READY`, the model's own line of JSON, `taken`, and the reading with it: the place "Dr. Sam office" ·
"Lisbon" · "Schule" · and for the flight no place (the model's "Frankfurt" is in the title already). The day and
time of each are what `./bl debug event TEXT` says of the same text without the model: the model's "when" must
be the very words the rules read, so it never moves a date. (Where the model answers otherwise than it did when
these were tried, `not taken: the rules' reading stands` is a right answer too; the answer says which it was.)

Answer (the Lenovo Googlebook, 6 October 2026): yes. All four `ai=READY`, 1.7 to 2.3 s, `taken`: the places "Dr. Sam office", "Lisbon", "Schule"; the flight has no place and is not asked for by the row (`asks=false`). The day and time of each are the rules' own.

### 9. Its answer changes the slots where they stand [C]

*State: not pretended, saving off.*

`./bl open stay`, `./bl debug keys "Schedule dentist appointment next Tuesday at 3:30pm at Dr. Sam office"`, then
`./bl debug dump` and `./bl shot before` at once, the dump again after one second, and `./bl debug dump` and
`./bl shot after` once the dump no longer says `looking` (up to seven seconds the first time).

At once: the title "dentist appointment at Dr. Sam office", `WHERE` empty, no `looking`, no `thinking`: the row is
the rules' own, in the frame of the last key. About a second later: `looking` (the edge's light runs). Then: title
"dentist appointment", `WHERE` "Dr. Sam office". `window=` is the same before and after; the two pictures differ
in those two slots and in nothing else: no line has moved, the pill and the strip stand where they stood.

Answer (the Lenovo Googlebook, 6 October 2026): yes. At once the rules' reading (the place in the title, `WHERE` empty, no `looking`); within a second `looking`; then the title without the place and `WHERE` filled. `window=` the same; the two pictures differ in those two slots and in nothing else.

### 10. It is asked after a rest, once, and never for a sentence the rules read whole [C]

*State: not pretended, saving off.*

`./bl debug keys "Add dinner with Sam tomorrow at 7pm"`, dumps for five seconds. Then the sentence of check 9
typed again in the same panel (`./bl debug keys …`), with a dump after half a second and after two seconds. Then
`./bl debug keys` with the same sentence and one more word, and at once one more letter by `./bl debug in`.

The first: never `looking`, nothing changes: nothing was left over. The second: the split is there within the
first dump, with no `looking`: this text was asked once and is not asked again. The third: no `looking` while
letters come; it begins only 0.7 s after the last one.

Answer (the Lenovo Googlebook, 6 October 2026): yes. The sentence the rules read whole: never `looking` in five seconds. The same sentence typed again: the split at once, no `looking`. One more word and a letter: no `looking` right after the letter, then within the second.

### 11. No model here [C]

*State: not pretended, saving off.*

`./bl debug ai none`, `./bl open stay`, the sentence of check 9, dumps for three seconds; then `./bl debug ai real`.

The row is the rules' reading and stays it; never `looking`. Enter is `create` as before.

Answer (the Lenovo Googlebook, 6 October 2026): yes. The rules' reading for three seconds, never `looking`; `create` as before; `ai real` gave `READY` back.

### 12. Enter in the moment the words change [A]

*State: not pretended, saving off.*

With the sentence of check 9 typed by hand: Enter pressed in the instant the slots change.

That press is not taken: the row stands, with its new words. A second press, a third of a second later or more,
runs it. (Nothing waits for the model: an Enter before its answer runs the row as the rules read it. And an
answer that splits a sentence just as the rules had changes nothing in the row, and holds no Enter.)

Answer (the Lenovo Googlebook, 6 October 2026): open: by hand.

## The calendars, pretended

### 13. A calendar by its name, and the start of one completed [C]

*State: `./bl debug calendars pretend Sam,Team,Team trips,Holidays!` (`pretended=true` read back), saving off.*

`./bl open stay`, `./bl debug keys "Add dinner with Sam tomorrow at 7pm to tea"`, `./bl debug dump`, `./bl shot
tea`; `./bl debug key right`, `./bl debug dump`; then `./bl debug in` with the same text and a space at its end.
Then, each with a dump: `./bl debug keys "Add dinner with Sam tomorrow at 7pm to Team cal"` · `… to the` · `… to
TE` and `./bl debug key right` · `./bl debug keys "Add trip tomorrow at 9am to ho"`.

The row has `CALENDAR` on its second line, after `TITLE`. With `to tea`: `completion=m`, the field shows a grey
"m" after the text, the slot says `●Team?` with the calendar's colour as a dot (a step lighter: its name is not
whole yet), the title is "dinner with Sam". After Right: the field ends in "to Team", `completion=null`, the slot
`●Team`. With the space: no grey letter. `to Team cal`: the slot says `●Team`, the title is "dinner with Sam" and
`completion=null` (from its third letter the word "calendar" after a whole name is the word on its way; with one
letter or two, `to Team c`, the row has no calendar for that moment: it is as likely a name going on). `to
the`: no calendar and no grey letter (an article alone begins no name), the words in the title. `to TE` and
Right: the field ends in "to Team", the name as the calendar has it. `to ho`: no grey letter and no calendar: a
start is never completed into a calendar that takes no new events (its whole name, `to Holidays`, is read).

Answer (the Lenovo Googlebook, 6 October 2026): yes, each as written; also "to the tea" completes to Team and "in team t" to Team trips.

### 14. The Calendar stop and its list [C] and [A]

*State: as check 13 (pretended, saving off), in a panel with `Add dinner with Sam tomorrow at 7pm to Team` typed.*

`./bl debug key calendar` (it arms the Calendar stop at once), `./bl debug dump`, **and `armed=calendar` read in
that dump before any Enter**: Tab goes round (Create, Copy, Calendar, and Create again), and an Enter on Create
opens the Calendar app. Then `./bl debug key enter`, `./bl debug dump`, `./bl shot list`; `./bl debug key down`
twice, `./bl debug key enter2`, `./bl debug dump`. By hand [A]: the list opened again, and Ctrl + the digit of
one of its lines.

Tab only moves: nothing opens until Enter. Then `opened=sentence:event:calendar`, the pill on the first of three
lines under the row (`+c`), each a calendar's name with its colour's dot in the icon column: not the one that
takes no new events. The row's strip is gone but for the word "Calendar" and the arrow, turned over. After Down
twice and Enter twice: the list is closed, the field's sentence names the calendar of the third line ("… to Team
trips") in place of the one it named, the pill is on the event's row with its first action armed, and **the
second Enter did nothing**: the panel is open. Ctrl + digit on a line chooses that calendar as Enter does, and
says nothing in the footer.

Answer (the Lenovo Googlebook, 6 October 2026): yes for [C]. `key calendar` arms the stop and nothing opens; Enter: `opened=sentence:event:calendar`, the pill on the first of three lines, each with its colour's dot, not the one that takes no events; Down Down Enter Enter: the list closed, the field names the third line's calendar, the event's row selected with its first action armed, the panel open. [A]: open.

### 15. What Enter would send with a calendar named [C]

*State: as check 13 (pretended, saving off).*

`./bl debug event "Add dinner with Sam tomorrow at 7pm to Team"`.

`calendar=Team`, and the link ends `&src=pretend-1%40example.com`.

Answer (the Lenovo Googlebook, 6 October 2026): yes: `calendar=Team`, the link ends `&src=pretend-1%40example.com`.

### 16. Save, looked at with nothing to write into [C]

*State: as check 13: `pretended=true` read back first, and only then `./bl debug pref save on` (`save=true`).*

`./bl debug event TEXT` for:

1. `Add dinner with Sam tomorrow at 7pm to Team`
2. `Add dinner with Sam tomorrow at 7pm`
3. `Add dinner with Sam tomorrow`
4. `Add dinner with Sam tomorrow at 7`
5. `Put yoga class every Monday 6pm in my Team calendar` · `Add weekly sync Friday 9am`
6. `Add trip tomorrow at 9am to Holidays`
7. `Add dinner with Sam tomorrow at 7pm to tea`

Then in a panel: the first of them by `./bl debug keys`, `./bl debug dump`, `./bl shot save`; `./bl debug key
tab`, `./bl debug dump`, `./bl shot open`; `./bl debug key backtab`, `./bl debug key enter`, and a dump within a
second (the footer's word stands for 1.6 s). Then the fourth by `./bl debug keys`, `./bl debug dump`, `./bl shot
guess`. Then `./bl debug pref save off`.

The seven:

1. `enter=save: would write calendar=Team title='dinner with Sam' start=… end=… allDay=false zone=` this device's
   zone, `+ the calendar's default reminder (pretended: Enter writes nothing)`.
2. The same into `Sam`, the account's own, and the row's Calendar slot says `●Sam?` (a step lighter: it was not
   typed).
3. `enter=open`, `(a guess: no time)`, `footer=Opens in Calendar: the day or the time is a guess`.
4. `enter=open`, `(a guess: half of the day not said)`, the same footer: "at 7" is read as the evening, and
   nobody typed the evening.
5. `enter=open`, `(a guess: repeats)`, `footer=A repeat is set in Calendar`, both.
6. `enter=open`, `footer=Opens in Calendar: Holidays takes no new events`.
7. `enter=open`, `rest=m`, the slot `●Team?`, `footer=Opens in Calendar: the calendar’s name is not finished`.

In the panel, the first sentence: the strip is Save · Open · Copy · Calendar, `armed=save`; the dump's row begins
`{New event. Opens in your calendar to save.; while save: New event. Enter saves it.; …`, and the picture's
caption reads "New event. Enter saves it."; TITLE shows "dinner with Sam" whole. After Tab: `armed=open`, the
strip unrolls "Open" with its mark, the caption reads "New event. Opens in your calendar to save.", and TITLE
still shows "dinner with Sam" whole: the short word leaves it its room. After Enter on Save: nothing is written
(a pretended list has nothing to write into), the dump says `flash=Couldn’t do that`, and the panel stays. The
fourth sentence: the strip is Open · Copy · Calendar, `armed=open`, "When" a step lighter, the same caption, the
footer's quiet line, and TITLE whole.

Answer (the Lenovo Googlebook, 6 October 2026): yes, the seven and the panel as written: Save · Open · Copy · Calendar with `armed=save` and "New event. Enter saves it."; after Tab "Open" and "New event. Opens in your calendar to save."; Enter on Save with a pretended list: `flash=Couldn’t do that`, the panel stays, nothing written. The fourth: Open first, "When" a step lighter, the footer's quiet line. The title stands whole in all of them.

### 17. The row points at the step, until the question has been answered [C]

*State: `./bl debug calendars pretend off` (`pretended=false`, `allowed=false`), saving off.*

`./bl debug pref calasked no`, `./bl debug event "Add dinner tomorrow at 7pm to the Team calendar"`; then `./bl
debug pref calasked yes` and the same again.

First: `named`, the title "dinner to the Team calendar", an action `allow` after Copy, `footer=Allow Calendars to
put it on a calendar by name`. After: no `allow`, no footer; the title the same.

Answer (the Lenovo Googlebook, 6 October 2026): yes: first `named`, the `allow` action and the footer's line; after `calasked yes` neither, the title the same.

### 18. Many calendars, a long name, the dark theme [C]

*State: `./bl debug calendars pretend` with fourteen names, one of them forty letters long and one that can be
named (letters and digits, no word for a day or a time in it, no ` @ `: `./bl debug calendars` does not mark it
`(not offered)`), `pretended=true`, saving off.*

A sentence that names the long one; the list opened; `./bl shot` in both themes. Then `./bl debug calendars
pretend off`, and `pretended=false` read back.

The list has twelve lines (fewer on a screen that has no room for twelve under the row: `window=` in the dump is
never taller than the screen leaves under the panel's top). The long name fades or is cut at the slot's end and
pushes nothing. Every dot can be told from the glass in both themes.

Answer (the Lenovo Googlebook, 6 October 2026): yes. Fourteen pretended: twelve lines in the list (`window=1080x1038`); a name of 46 letters is cut with an ellipsis at its slot's end and pushes nothing; every dot can be told from the glass in both themes.

## The window, and the system's permission

### 19. The grant, the switch and the choice [C] and [A]

*State: nothing allowed, not pretended, saving off.*

`./bl window privacy`, `./bl wshot privacy`; `./bl window results`, `./bl wshot results`, at the width the window
opens at. By hand [A]: the same two pages with the window dragged narrow, with the rail, and with the second pane.

Privacy › What Booklight may use: a row "Calendars" under Brightness, with a mark, one line of text that ends
"Not allowed yet.", and "Allow…" at the row's end, on the line the two rows above end on. Results: a group
"Events" between "Show" and the other apps: "Save events without opening Calendar" with its switch off, and "New
events go to" with "Allow…". Every mark starts 16 dp in, every text on one edge, every control ends 16 dp from
the row's trailing edge; nothing is centred. The switch's text ("… both sure: “tomorrow at 7pm”, not “at 7”. …")
is not cut at any width.

Answer (the Lenovo Googlebook, 6 October 2026): yes for [C], at 420 dp wide: Calendars under Brightness with "Allow…" on the line of the rows above; the group Events between "Show in the list" and "Other apps", the switch off and its text whole, "New events go to" with "Allow…". [A]: the other widths, open.

### 20. The system's question, answered by a hand [A]

*State: nothing allowed, not pretended, saving off; the system has never asked for the calendars on this device
(a new installation: `./bl uninstall`, `./bl install`).*

Privacy › Calendars › Enter, and the question put away without an answer, where the system lets one (Esc, a click
beside it). Enter again, "Don't allow". Enter again, "Don't allow" again. Then Enter once more. Then allow it in
the system's own page. Then Results › "Save events without opening Calendar" › Enter, and "Don't allow"; again,
and "Allow". **Last of all `./bl debug pref save off`, and `save=false` read back**: the last "Allow" switched
saving on with the device's own calendars.

A question that is put away, and each of the two refusals, leaves the row as it was and opens nothing: each
answered a question that was shown. After the second refusal the system asks no more: the next Enter gets its
"no" at once, and only that one opens Booklight's page in the system's Settings. Allowed: the row says what
Booklight reads, and "Change…" opens that page. The switch stays off when its question is refused, and goes on
when it is allowed.

Answer (the Lenovo Googlebook, 6 October 2026): in part, with the system's own question answered by taps. Privacy
› Calendars › Enter: "Allow Booklight to access your calendar?" with Allow and Don't allow. "Don't allow": the row
as it was, nothing opened, `asked=true`. Enter again: the question again; "Allow": `allowed=true`, the list read,
and the row says "Booklight reads the list of your calendars. It never reads an event." with "Change…" (on the
first build of that day it went on saying "Not allowed yet" until the window was opened again: fixed). Results ›
"New events go to" › Enter › Allow: the row is the menu with the account's own calendar. **With the list
allowed, the switch "Save events without opening Calendar" goes on with no second question**: Android gives the
second permission of the same group by itself. Open: the question put away without an answer, the second refusal
and the page in Settings after it.

### 21. Allowed: the list is read, and only the list [C]

*State: `./bl debug calendars pretend off` (`pretended=false`), `./bl debug pref save off` (`save=false`); and, where
check 20 was run before it, `./bl sh pm revoke --user current io.github.kuscher.booklight
android.permission.WRITE_CALENDAR` first (its last "Allow" gave both).*

`./bl sh pm grant --user current io.github.kuscher.booklight android.permission.READ_CALENDAR`, `./bl open stay`,
`./bl debug calendars`, `./bl debug close`, `./bl window results`, `./bl wshot results-allowed`, `./bl window
privacy`.

`allowed=true`, `writes=false`, `pretended=false`, and `count=` one or more (the answer says how many, and no
name). Results: "New events go to" is a menu button that shows the account's own calendar; it stands where the
search engine's stands (both at their rows' ends, or both under their text). Privacy: "Booklight reads the list
of your calendars. It never reads an event."

Answer (the Lenovo Googlebook, 6 October 2026): yes: `allowed=true writes=false pretended=false`, and a `count=` of more than one. "New events go to" is a menu button with the account's own calendar; Privacy says "Booklight reads the list of your calendars. It never reads an event." with "Change…".

### 22. The switch goes off with its permission [C]

*State: not pretended, the list allowed (check 21's grant stands). **Saving is on with the real list in this
check: nothing is typed into a panel and no Enter is pressed.***

`./bl sh pm grant --user current io.github.kuscher.booklight android.permission.WRITE_CALENDAR`, `./bl debug pref
save on`, `./bl open stay`, `./bl debug pref`: `save=true`. `./bl debug close`. Then `./bl stop`, `./bl sh
'run-as io.github.kuscher.booklight --user $(am get-current-user) rm files/calendars.asked'`, `./bl open stay`,
`./bl debug pref`; `./bl debug pref save on` again, `./bl debug pref`, `./bl debug close`. Then `./bl sh pm
revoke --user current io.github.kuscher.booklight android.permission.WRITE_CALENDAR`, `./bl open stay`, `./bl
debug pref`.

After the mark was removed: `save=false`, with both permissions still given: settings that say "save" without
this device's own mark beside them (as they would arrive with a backup) are not believed. Switched on again:
`save=true`. After the revoke: `save=false`: the switch went off with its permission, and comes back only by a
press in the window.

Answer (the Lenovo Googlebook, 6 October 2026): yes, both halves: with the write permission revoked the next panel says `save=false`; and with both permissions still given but this device's mark removed (`files/calendars.asked`), `save=false` too.

### 23. The editor opens on the calendar that was named [C] and [A]

*State: not pretended, the list allowed, saving off (`save=false` read back: this check presses Enter).*

A sentence that names one of this device's calendars other than the account's own, by `./bl debug event` first
(the link ends in `&src=` and an address), then typed in a panel and Enter.

The Calendar app's editor opens with that calendar chosen. It is closed by its window's own close button,
without saving.

Answer (the Lenovo Googlebook, 6 October 2026): yes. A sentence naming one of this device's calendars other than the account's own, by a word of its name with "calendar" after it: the link ends in `&src=` and its address, and the editor opened with that calendar chosen, the title and the day right. Closed by its window's close button, unsaved. [A]: by hand, open.

### 24. One event is saved, once [C]

*State: `./bl debug calendars pretend off` (`pretended=false`); both permissions given (`pm grant --user current`
for `READ_CALENDAR` and `WRITE_CALENDAR`: `allowed=true`, `writes=true`); `./bl debug pref eventcal none` ("New
events go to" is the account's own calendar); and, last, `./bl debug pref save on` (`save=true`). This is the one
check that saves. Enter is pressed once.*

1. `./bl debug event "Add Booklight test (remove me) tomorrow at 7am"`.
2. That no such event is there yet. The user is named by its number, `U` below (`./bl sh am get-current-user`
   says it: 10 on these devices; `content` refuses `--user current`), because the calendars are the current
   user's and not user 0's; and `--where` is an argument of its own to adb, quoted so that the device's shell
   hands it on whole:
   `adb shell content query --user U --uri content://com.android.calendar/events --projection _id:title:calendar_id:dtstart:dtend:allDay:eventLocation:eventTimezone:availability:deleted:dirty:_sync_id --where "title=\'Booklight\ test\ \(remove\ me\)\'"`
   (this form worked on the test device).
3. `./bl open stay`, `./bl debug keys "Add Booklight test (remove me) tomorrow at 7am"`, `./bl debug dump`.
4. `./bl debug key enter`, once; `./bl debug dump` at once and again a second later.
5. The query of step 2 again; and with the `_id` it says:
   `adb shell content query --user U --uri content://com.android.calendar/reminders --projection event_id:minutes:method --where "event_id=ID"`.
6. `adb shell content delete --user U --uri content://com.android.calendar/events/ID`, and the query of step 2
   once more, and again a minute later.
7. `./bl debug pref save off`, and `save=false` read back; and right after it `./bl sh pm revoke --user current
   io.github.kuscher.booklight android.permission.WRITE_CALENDAR` and the same for `READ_CALENDAR`, then `./bl
   open stay` and `./bl debug calendars`: `allowed=false`, `writes=false`, `save=false`. (With the permissions
   gone no later check can write, whatever becomes of a pretended list or of Booklight's process; a pretended
   list still shows Save, to be looked at. Nothing is typed and no Enter is pressed between step 4 and here.)

Step 1: `enter=save: would write` into the account's own calendar (one that `./bl debug calendars` marks `(own)`:
with more than one account several are, and the one this line names is the one), `title='Booklight test (remove
me)'`, tomorrow 07:00 to 08:00, `allDay=false`, this device's zone, `+ the calendar's default reminder`, and no
`(pretended: …)`. Step 2: `No result found.` Step 3: the event's row is the selected one (if a row of the device
stands over it, `./bl debug key down` until it is) with `armed=save`, its caption says Enter saves it, and the
Calendar slot shows the account's own calendar a step lighter. Step 4: the first dump says `flash=Saved to` and
the calendar's name, or already `no panel`, or neither yet (the event is being written: then the dump again);
the second says `no panel`. Step 5: exactly one row: that title, the `calendar_id` of the calendar that step 1's
`enter=` named, `dtstart` and `dtend` an hour apart at tomorrow 07:00 of this device's zone (in milliseconds),
`allDay=0`, `eventLocation=NULL`, `eventTimezone` this device's zone, `availability=0` (a timed event is as the
provider makes it; an all-day one is written as free, `availability=1`, and no check saves one), `deleted=0`;
and one reminder of that event, with the provider's own words for "the calendar's default" (`minutes=-1`,
`method=0`), or with the minutes the calendar's own default has. `dirty=1` and no `_sync_id` at first, `dirty=0`
and a `_sync_id` once the calendar's account has taken the event (within a minute on the test device). Step 6:
the row says `deleted=1`, and is gone (`No result found.`) once the account has removed it; or it is gone at
once. **Nothing of this is read in the Calendar app, and no picture is taken of the footer.** If step 5 finds no
row, or two: stop, write down what the query said, remove what is there by its `_id`, and run nothing more with
saving on.

Answer (the Lenovo Googlebook, 6 October 2026): yes, twice, each one event that was then removed. A timed one: exactly one row, the account's own calendar, `dtstart` and `dtend` tomorrow 07:00 and 08:00 of the device's zone, `allDay=0`, `eventLocation=NULL`, one reminder `minutes=-1, method=0`; `flash=Saved to` and the calendar's name, then `no panel`. The account took it within a minute (`dirty=0`, a `_sync_id`), and a minute after `content delete` the row was gone. An all-day one on the last build: `allDay=1`, `eventTimezone=UTC`, the two midnights, `availability=1` (free); deleted before it was synced. Saving off and both permissions revoked right after each.

### 25. Nothing saves twice, and nothing but Enter saves [C] and [A]

*State: `./bl debug calendars pretend Sam,Team,Holidays!` (`pretended=true` read back), and only then `./bl debug
pref save on`.*

`./bl open stay`, `./bl debug keys "Add dinner with Sam tomorrow at 7pm to Team"`, `./bl debug dump` (`armed=save`),
`./bl debug key stay` (Enter with Shift held), a dump a moment later. By hand [A], in the same state: Ctrl + the
row's digit on such a row. Then `./bl debug pref save off`.

Enter with Shift held does what Enter does: an event that was saved takes the panel with it whatever Shift says
(here nothing can be written: the dump says `flash=Couldn’t do that`, and the panel stays). Ctrl + digit does
nothing on a row whose first action is Save.

Answer (the Lenovo Googlebook, 6 October 2026): yes for [C]: Enter with Shift held on Save, with a pretended list: `flash=Couldn’t do that`, the panel stays. [A]: open.

### 26. A line that reads as an event is not sent for suggestions [C]

*State: `./bl debug pref save off` (`save=false`), not pretended.*

`./bl debug pref suggestions on`, `./bl open stay`, `./bl debug keys "Add dinner with Sam"`, a dump a second
later; then `./bl debug keys "Add dinner with Sam tomorrow at 7pm"`, a dump a second later; `./bl debug pref
suggestions off`.

The first may have suggested searches under the web's row, as any typed line has. The second has none: once the
line reads as an event it is not sent.

Answer (the Lenovo Googlebook, 6 October 2026): yes: "dinner recipes for", "book flights to", "plan a trip to" have suggested searches; "Add dinner with Sam tomorrow at 7pm" and "Add dinner recipes for tomorrow" have the event's row and none.

### 27. In German [C] and [A]

*State: `./bl debug calendars pretend Sam,Team,Holidays!` (`pretended=true` read back), saving off; the app's
language German.*

`./bl debug keys "Trag Mittagessen mit Sam am Freitag um 12 Uhr mittags in den Team-Kalender ein"`, `./bl shot
de`; the list opened (`./bl debug key calendar`, `armed=calendar` read, `./bl debug key enter`) and a calendar chosen; then, still pretended, `./bl debug pref save on`, the same sentence
again, `./bl shot de-save`, `./bl debug key tab`, `./bl shot de-open`, and the sentence with „um 12“ in place of
„um 12 Uhr mittags“, `./bl shot de-guess`; `./bl debug pref save off`. `./bl window results`, `./bl wshot
de-results`; `./bl window privacy`. Then the app's language back (`--locales ""`). By hand [A]: the window's two
pages at its narrowest and its widest.

The slots WANN, TITEL, WO, KALENDER; „Erstellen“. A calendar chosen from the list is written as „in“ and its
name, before the „ein“. With the switch on: „Speichern“ armed and the caption „Neuer Termin. Enter speichert
ihn.“; after Tab „Öffnen“ and the caption „Neuer Termin. Öffnet sich im Kalender zum Speichern.“, the title whole
in both. With „um 12“: „Öffnen“ first, WANN a step lighter, and the footer's line „Öffnet sich im Kalender: Tag
oder Uhrzeit sind geraten“ (which twelve was not said). The window's rows in German, each title on one line at
the widest window and nothing cut at the narrowest.

Answer (the Lenovo Googlebook, 6 October 2026): yes for [C]. WANN, WO, TITEL, KALENDER; „Erstellen“; with the switch on „Speichern“ and „Neuer Termin. Enter speichert ihn.“, after Tab „Öffnen“ and „Neuer Termin. Öffnet sich im Kalender zum Speichern.“; with „um 12“ „Öffnen“ first, WANN a step lighter and „Öffnet sich im Kalender: Tag oder Uhrzeit sind geraten“. A calendar chosen from the list is written after „in den“ and reads back. The window's Events group in German with every row's name on one line at 420 dp. The language was set back. [A]: open.

### 28. What a screen reader is told [C]

*State: `./bl debug calendars pretend Sam,Team,Holidays!` (`pretended=true`), saving off; then on, still pretended.*

`./bl open stay`, `./bl debug keys "Add dinner with Sam tomorrow at 7 to Team"`, `./bl debug first says`. Then
`./bl debug pref save on`, `./bl debug keys "Add dinner with Sam tomorrow at 7pm to Team"`, `./bl debug first
says`; `./bl debug pref save off`.

The event's row says its name, then each slot that has a value with its label ("When …", "Title …", "Calendar
Team"), a guess as one ("When …, a guess"), then the armed action. With saving on: after the slots also what
Enter does on the armed action ("New event. Enter saves it."), and the footer's line where the row has one.
After the model's answer (check 9) the row's new slots are said once, and only where the answer changed one.

Answer (the Lenovo Googlebook, 6 October 2026): yes: "Event, When Wed 7 Oct, 7–8 PM, a guess, Title dinner with Sam, Calendar Team, Create"; with saving on "Event, When …, Title …, Where Cafe Luna, Calendar Team, New event. Enter saves it., Save".

### 29. Settings that were there before [C]

*State: a 3.1 that has been used, and this build installed over it.*

`./bl debug pref`, `./bl debug calendars`.

`save=false eventcal=own calasked=false`, `allowed=false`; everything else `./bl debug pref` says is as it was
under 3.1.

Answer (the Lenovo Googlebook, 6 October 2026): yes: on a device that had 3.1 and its settings, `save=false eventcal=own calasked=false`, `allowed=false`, and everything else `./bl debug pref` says as it was.

### 30. The HP Googlebook, and what only a person can say [A]

*State: on the HP Googlebook, a pretended list wherever a check asks for one; saving off but under a pretended list.*

Checks 1, 3, 9, 13, 14, 16 and 19 on the HP Googlebook. And by eye: do two lines of slots read as one row; is
"WHERE –" on every event's row right, or should a slot stand only once it is filled; is the dot a help; is "Save"
as the armed action right for you with the switch on; does the caption's change read well when Tab moves from
Save to Open (it rolls into place); is "Opens in Calendar" too often the answer now that "at 7" is a guess.

Answer (the Lenovo Googlebook, 6 October 2026): open: the HP Googlebook, and his eye.

## What the reviews found

Each begins with a pretended list: nothing can be written in any of them.

### 31. An Enter that comes before its list never saves [C] and [A]

*State: `./bl debug calendars pretend Sam,Team,Holidays!` (`pretended=true` read back), then `./bl debug pref
save on`.*

`./bl open stay`, `./bl debug rush "Add dinner with Sam tomorrow at 7pm to Team"`, `./bl debug dump` a moment
later; then `./bl debug key enter` and a dump within a second. Then the same `rush` once more, with the text
still in the field. Then `./bl debug pref save off`, `./bl debug close`, `./bl open stay`, and the same `rush`
again. By hand [A], with the pretended list and saving on: the sentence typed fast and Enter with its last
letter.

With saving on: after `rush` the row stands, selected, with `armed=save`, `flash=null`, and the panel is open:
the Enter that came before the row was on the glass was dropped, not run (had it run, the dump would say
`flash=Couldn’t do that`). (The early Enter is row one's: if check 35 showed a row of the device over the event's
row for this sentence, another title is taken here, one that nothing of the device matches.) The Enter after it is taken, and says so (`flash=Couldn’t do that`: the list is pretended). The
`rush` with the text the field holds already answers `not an early Enter: the field holds this text already`
and presses nothing (it would be an Enter on a row that has landed); so does a `rush` with saving on and the
device's own calendars (`not an early Enter: saving is on …`), which this check never has. With saving
off the same early Enter is kept and runs Create when the list lands, as an Enter before its list always has:
the Calendar app's editor opens (closed by its own close button, without saving; and `./bl debug calendars`
read afterwards: the process, and the pretended list with it, went when the editor came).

Answer (the Lenovo Googlebook, 6 October 2026): yes for [C]: after `rush` with saving on the row stands with `armed=save`, `flash=null`, the panel open; the next Enter says `flash=Couldn’t do that`. With saving off the early Enter ran Create when the list landed (the editor, closed unsaved). [A]: open.

### 32. The calendars are read while the row stands [C] and [A]

*State: `./bl debug calendars pretend Sam,Team` (`pretended=true` read back), then `./bl debug pref save on`.*

`./bl open stay`, `./bl debug keys "Add dinner with Sam tomorrow at 7pm to Garden"`, `./bl debug dump`; then
`./bl debug calendars pretend Sam,Team,Garden` (the list changes under the open panel, and stays pretended) and
a dump at once, with no key in between. Then `./bl debug pref save off`. By hand [A]: Enter in the instant of
the second command.

First: the title "dinner with Sam to Garden", the Calendar slot `●Sam?`. After the list changed: the title
"dinner with Sam" and the slot `●Garden`, with no key typed: the row is made anew when the list of calendars
lands. An Enter in that instant is not taken (the row changed under it); one a third of a second later is.

Answer (the Lenovo Googlebook, 6 October 2026): yes for [C]: first the title "dinner with Sam to Garden" and `●Sam?`; after the list changed, with no key, "dinner with Sam" and `●Garden`. [A]: open.

### 33. In a lesson of first run nothing is saved [C]

*State: `./bl debug calendars pretend Sam,Team` (`pretended=true` read back), then `./bl debug pref save on`;
first run's state written down (`./bl debug first`).*

`./bl debug first at l2`, `./bl open stay`, `./bl debug keys "Add dinner with Sam tomorrow at 7pm"`, `./bl debug
dump`, `./bl debug key enter`, a dump within a second. Then `./bl debug first off` (or first run's state as it
was), `./bl debug pref save off`.

The lesson stands (`due=L2`), the event's row is there, selected (`./bl debug key down` to it where a row of the
device stands over it), with `armed=save`. Enter: `flash=That saves the event`,
nothing else, the panel open and the lesson still standing: practice opens nothing and saves nothing. (Not
`flash=Couldn’t do that`: the executor was not asked at all.)

Answer (the Lenovo Googlebook, 6 October 2026): yes: `due=L2`, the event's row selected with `armed=save`; Enter: `flash=That saves the event`, the panel open, the lesson standing.

### 34. "Asked" that came with a backup is not believed [C]

*State: not pretended, saving off, the list not allowed (`allowed=false`).*

`./bl debug pref calasked yes`, `./bl debug pref` (`calasked=true`). `./bl stop`, then `./bl sh 'run-as
io.github.kuscher.booklight --user $(am get-current-user) rm files/calendars.asked'`, `./bl open stay`, `./bl
debug pref`, and `./bl debug event "Add dinner tomorrow at 7pm to the Team calendar"`.

`calasked=false`: the settings said "asked", this device's own mark was gone (as on a device the settings came to
with a backup), and they were not believed. The row points at the step again (`allow`, and the footer's line).

Answer (the Lenovo Googlebook, 6 October 2026): yes: `calasked=false` after the mark was removed, and the row has `allow` and the footer's line again.

### 35. The example, a letter at a time, looked at at every word [C]

*State: `./bl debug calendars pretend Sam,Team,Team trips,Holidays!` (`pretended=true` read back), saving off.*

`./bl open stay`. Then, for each beginning of `Add dinner with Sam tomorrow at 7pm to the Team calendar` that
ends with a word (eleven of them: `Add` · `Add dinner` · … · the whole sentence): `./bl debug keys "BEGINNING"`,
which types it from an empty field one letter at a time, and `./bl debug dump`. `./bl shot` at `tomorrow`, `7pm`,
`the` and at the end.

| Typed up to | The event's row |
| --- | --- |
| `Add`, `dinner`, `with`, `Sam` | none: no day and no time yet |
| `tomorrow` | there: When tomorrow, all day, a guess (`?`); Title "dinner with Sam"; `WHERE` and `CALENDAR` empty |
| `at` | the same, the title "dinner with Sam at" |
| `7pm` | When tomorrow 7 to 8 PM, no `?`; Title "dinner with Sam" |
| `to` | the title "dinner with Sam to" |
| `the` | the title "dinner with Sam to the"; no calendar, `completion=null` |
| `Team` | `CALENDAR` `●Team`; Title "dinner with Sam" |
| `calendar` | the same |

From `tomorrow` on the row is `sentence:event`, under whatever of the device matches (the answer names no app)
and over the web's rows, and `window=` says the same height for the row in every dump: its two lines of slots
stand from its first moment, and only the words in them change. With saving off its first action is `create`
throughout.

Answer (the Lenovo Googlebook, 6 October 2026): yes, as the table: no event's row up to `Sam`; from `tomorrow` on it is row one with `window=1080x486` in every dump, and only the words in its slots change.

## What the second round of reviews and the first pass on a device found

Each begins with a pretended list or with saving off: nothing can be written in any of them. The days in the
expectations are those of a run on Tuesday 6 October 2026; on another day "tomorrow" and the weekdays move.

### 36. "When" stands whole [C] and [A]

*State: `./bl debug calendars pretend Sam,Team` (`pretended=true` read back), then `./bl debug pref save on`: the
row's strip is then at its widest (Save · Open · Copy · Calendar), and its text at its narrowest.*

`./bl open stay`, then, each with `./bl debug dump` and `./bl shot`: `./bl debug keys "event standup tomorrow
11:30am-12:30pm"` · `./bl debug keys "event Offsite Oct 14 to Oct 16"` · `./bl debug keys "event standup
Wednesday 28 October 10:30am-12:30pm"` · `./bl debug keys "event Offsite Oct 28 to Nov 2"` · `./bl debug keys
"event review Jan 15 10:30am-12:30pm"`. The same five with the app's language German, and the language back afterwards. Then `./bl debug pref save
off`. By eye [A]: is the title's room enough (it has the second line).

`WHEN` is whole in every picture, and never ends in an ellipsis: "Wed 7 Oct, 11:30 AM–12:30 PM" · "Wed 14 – Fri
16 Oct, all day" (both weekdays, the month once, and "all day" on the glass) · "Wed 28 Oct, 10:30 AM–12:30 PM" ·
"28 Oct – 2 Nov, all day" (across months: no weekdays) · "15 Jan 2027, 10:30 AM–12:30 PM" (the year, and for
its room no weekday). The dump's `When=` says the same words as the picture. `WHERE`, beside it, is the slot that gives
way; `TITLE` has the second line, and all of it but the calendar's share (a title of some thirty letters stands
whole there beside a calendar and the widest strip). In German, on a device that shows the 24-hour clock:
"Mi. 7 Okt., 11:30–12:30" · "Mi. 14 – Fr. 16 Okt., ganztägig" · "Mi. 28 Okt., 10:30–12:30" · "28 Okt. – 2 Nov.,
ganztägig" · "Fr. 15 Jan. 2027, 10:30–12:30"; on one that shows AM and PM the times are as in English, and the
last is "15 Jan. 2027, 10:30 AM–12:30 PM". `window=` says the same height in every dump.

Answer (the Lenovo Googlebook, 6 October 2026): yes, in English and German with the widest strip: `WHEN` whole in all five ("Wed 7 Oct, 11:30 AM–12:30 PM", "Wed 14 – Fri 16 Oct, all day", "Wed 28 Oct, 10:30 AM–12:30 PM", "28 Oct – 2 Nov, all day", "15 Jan 2027, 10:30 AM–12:30 PM"; „Mi. 7 Okt., 11:30–12:30“, „Mi. 14 – Fr. 16 Okt., ganztägig“, „Mi. 28 Okt., 10:30–12:30“, „28 Okt. – 2 Nov., ganztägig“, „Fr. 15 Jan. 2027, 10:30–12:30“), `window=` the same. With the title on the second line a title of thirty letters stands whole beside a calendar. [A]: his eye, open.

### 37. The year [C]

*State: as check 1 (nothing allowed, not pretended, saving off). No panel is needed but for the picture.*

`./bl debug event TEXT` for: `event dentist Oct 5 3pm` (a date that has just passed: run on 6 October or later;
on another day, yesterday's date) · `event review Jan 15 9am` · `event review 3.10.2020 15:00`. Then `./bl open
stay`, `./bl debug keys "event dentist Oct 5 3pm"`, `./bl shot year`.

The first: `when=2027-10-05T15:00..`, `(a guess: the date has passed)`, and the row's `When=Tue 5 Oct 2027, 3–4
PM?`: the year is on the glass, a step lighter with the rest of When. The second: next January, with its year
in `When=` and no guess. The third: 2020, as typed, with its year in `When=` and no guess. A date of this year
shows none.

Answer (the Lenovo Googlebook, 6 October 2026): yes: `2027-10-05`, `(a guess: the date has passed)`; next January with its year and no guess; the row says the year.

### 38. "next Tuesday" is never today [C]

*State: `./bl debug calendars pretend Sam,Team` (`pretended=true` read back), then `./bl debug pref save on`.*

`./bl debug event "Add dinner next Tuesday 7pm"` and `./bl debug event "Add dinner Tuesday 7pm"` (on any day; the
weekday of the day the check is run in place of Tuesday shows the difference). Then `./bl debug pref save off`.

With "next" and today's weekday: a week on, never today, whatever the time. With another weekday: the coming
one. Both `(a guess: “next” means two days to people)`, `enter=open`, and the footer's line "Opens in Calendar:
the day or the time is a guess". Without "next", today's weekday with a time still ahead is today, and since the
third round of fixes a guess of its own, `(a guess: this week or the next was not said)`, `enter=open`; any other
weekday is the coming one, no guess, `enter=save`.

Answer (the Lenovo Googlebook, 6 October 2026): yes, run on a Tuesday: "next Tuesday" is the 13th and a guess, "next Friday" the 9th and a guess, both `enter=open` with the footer's line; "Tuesday 7pm" is today and, since the third round of fixes, a guess too (`this week or the next was not said`).

### 39. A calendar called "Family Calendar", there and back [C]

*State: `./bl debug calendars pretend "Sam,Family Calendar,Team,Team"` (`pretended=true` read back; the second
"Team" is marked `(not offered)`), saving off.*

`./bl debug event "Add dinner tomorrow 7pm to the Family Calendar"`. Then `./bl open stay`, `./bl debug keys "Add
dinner tomorrow 7pm to fam"`, `./bl debug dump`; `./bl debug key right`, `./bl debug dump`. Then `./bl debug key
calendar`, `armed=calendar` read in the dump, `./bl debug key enter`, `./bl debug dump`; `./bl debug key down`
to the line "Team", `./bl debug key enter`, `./bl debug dump`; the list opened again the same way and the line
"Family Calendar" chosen, `./bl debug dump`.

The hook: `calendar=Family Calendar`, the title "dinner", no `named`. `to fam`: `completion=ily Calendar`, the
slot `●Family Calendar?`. After Right: the field ends "to Family Calendar", `completion=null`, the slot `●Family
Calendar`, the title "dinner". The list has three lines (Sam, Family Calendar, Team: one "Team", not two). After
"Team": the field ends "to Team", the slot `●Team`. After "Family Calendar": the field ends "to Family
Calendar", the slot `●Family Calendar`: what the list writes reads back as the calendar that was chosen.

Answer (the Lenovo Googlebook, 6 October 2026): yes: `calendar=Family Calendar`; "to fam" completes it; after Right the field ends "to Family Calendar" and reads it; the list has Sam, Family Calendar and one Team (the second is `(not offered)`); each line that was chosen reads back as itself.

### 40. A line that begins like an everyday search [C]

*State: `./bl debug calendars pretend Sam,Team` (`pretended=true` read back), then `./bl debug pref save on`.*

`./bl debug event TEXT` for: `plane crash today` · `book the midnight library` · `schedule nfl sunday` · `plan
trip to rome oct 14 to oct 16` · `book flight to boston friday 9am` · `event book flight to boston friday 9am` ·
`plane Treffen morgen um 15 Uhr`. `./bl debug find "plane crash today"` and `./bl debug find "book the midnight
library"`. Then `./bl open stay`, `./bl debug keys "book flight to boston friday 9am"`, `./bl debug dump`, `./bl
shot everyday` (no Enter is pressed). Then `./bl debug pref save off`.

The first four: `offered=false` (a day alone, a time alone, a range of days: each is as often a search), and
the two `find`s have no event's row. The fifth: `offered=true`, `everyday`, the reading sure (no `(a guess: …)`),
and all the same `enter=open: would send the insert request …`: the row's actions are `open,copy,calendar` and
its lines, with no `save`; `footer=Opens in Calendar: “book” often begins a search`, and the caption "An event,
or a search? Opens in your calendar to save." The sixth, under the keyword: `keyword`, no `everyday`,
`enter=save: would write … (pretended: Enter writes nothing)`. The seventh: `offered=true` („plane“ with a day
and a time said in German), `everyday`, `enter=open`. In the panel: the strip is Open · Copy · Calendar,
`armed=open`, the caption and the footer's line as the hook said them.

Answer (the Lenovo Googlebook, 6 October 2026): yes: no event's row for "plane crash today" and "book the midnight library"; "book flight to boston friday 9am" is offered, `everyday`, `enter=open`, with no Save among its actions, the caption "An event, or a search? Opens in your calendar to save." and the footer "Opens in Calendar: “book” often begins a search"; under the keyword `enter=save` (pretended); „plane Treffen morgen um 15 Uhr“ offered, `everyday`.

### 41. Under the keyword, as 3.1 read it [C]

*State: as check 1. No panel is needed.*

`./bl debug event TEXT` for: `event 9-10 standup tomorrow` · `event tomorrow standup 9-10` · `event put out bins
thu 7am` · `event Fr 15 Uhr Zahnarzt` · `Add 1-2 eggs tomorrow`.

`keyword`, the title "standup", tomorrow 09:00 to 10:00, `(a guess: half of the day not said)`, twice · the
title "put out bins" (no word of the line is dropped under the keyword), Thursday 07:00, no guess · Friday 15:00,
the title "Zahnarzt", `(a guess: a weekday by two letters)` · and without the keyword: tomorrow, all day, the
title "1-2 eggs": there two bare numbers are no time.

Answer (the Lenovo Googlebook, 6 October 2026): yes, each as written.

### 42. The window's two menus stand in one place [C] and [A]

*State: not pretended, saving off, the list allowed (`./bl sh pm grant --user current
io.github.kuscher.booklight android.permission.READ_CALENDAR`; `allowed=true`).*

`./bl window results`, `./bl debug window` (its width in dp), `./bl wshot results-menus`. By hand [A]: the same
page with the window dragged to its narrowest (420 dp), and to a wide one; the calendar's menu opened at the
narrowest (a capture cut to the window's bounds: a menu is a window of its own). Afterwards `./bl sh pm revoke
--user current io.github.kuscher.booklight android.permission.READ_CALENDAR`.

"Search engine" and "New events go to": both menu buttons end 16 dp from their rows' trailing edge, on the line
the switches end on, at every width; neither stands under its text. Where the calendar's name is longer than
the room its place has, it is cut inside its button with an ellipsis, and the button never pushes the row's name
off its line; the opened menu is as wide as the button, its names cut the same way.

Answer (the Lenovo Googlebook, 6 October 2026): yes for [C], at 420 dp in English and German: both menu buttons at their rows' ends on the line the switches end on, the calendar's name cut inside its button, each row's name on one line. [A]: the other widths and the opened menu, open.

## After the last check

`./bl debug calendars pretend off`; `./bl debug pref save off`; `./bl sh pm revoke --user current
io.github.kuscher.booklight android.permission.WRITE_CALENDAR` and the same for `READ_CALENDAR` (both, whatever
they were: a check granted them), then `./bl open stay` and `./bl debug calendars`: `allowed=false`,
`writes=false`, `pretended=false`, `save=false`. `./bl debug pref calasked`, `pref eventcal`, `pref suggestions`,
first run's state and the app's language as they were written down before the first check. The query of check
24, step 2, once more: `No result found.`
