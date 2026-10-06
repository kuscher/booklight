# Privacy

Booklight has no account and no ads, and its developer receives nothing from it. Since 2.0 it contains one
library of Google's, which reports how it is used to Google (see "Answers on this device").

**On your device.** Booklight remembers what you pick for the text you type, so your usual choice comes
first, and it keeps your settings, your links, snippets and recipes, and the emoji you picked lately.
These are files in the app's own storage, and they are part of your own Android backup and device transfer. With what it learns it also keeps, for an app whose own search
row you picked after typing its name and some words (`netflix severance`), that this app comes before the web
for such a text. The Booklight window › What Booklight keeps › Forget
everything clears what it has learned. If you turn on Show your usual, the empty panel shows the two things you
run most from Booklight and the one you ran last. This uses the same record; the only new things kept are the
switch, which two things had the first two rows last time, and the list of things you asked not to be suggested.
Booklight also keeps where its first steps stand: which of them are done, in how many openings they have stood,
how often the system's Keyboard shortcuts dialog was opened from them, and whether the app's icon has shown the
panel once.

**Notes** you jot down go into files (Notes.md, Todo.md, a file per day if you ask for one) in a folder
you choose once. Booklight reads and writes in that folder only, and reads it only when you search your
notes or your tasks.

**Other apps' commands.** Booklight reads what your installed apps declare they offer (their shortcuts) from
their own packages. It starts one only when you press Enter on its row, and those apps never see what you type.

**Search suggestions are off until you turn them on.** If you turn them on, the text you type is sent
over HTTPS to the search engine you chose (Google by default; DuckDuckGo, Bing, Brave Search or Ecosia
if you change it) to get suggested searches. Sums, web addresses, an app's own address, and anything
typed after a keyword or with an app in the field (a note, a mail, a search of one site or of one app, a song
to play) are not sent. Nor is a line that reads as an event (see "Calendar events"). An app's name followed by other words, typed in one go (`netflix severance`), is a
web search like any other and is sent, until you have picked that app's own row for such a text: from then
on the app comes first for words after its name, and they are not sent (picking the web's row twice running
makes it a web search again). No cookies or identifiers are added; like any
internet request it shows the search engine your IP address. Booklight asks in its first steps ("Suggest
searches as you type?"), whenever they come to that question while suggestions are off (an installation that was
there before and is only shown the step for the key is never asked), and only "Agree" there, or the switch
in the Booklight window, turns them on: Enter alone, Esc and waiting agree to nothing. Turn suggestions off again
in the Booklight window › Web search.

**The first steps** send nothing and open nothing. In the welcome Booklight types into its own field and shows
your own apps for one letter, a sum, a flight and the emoji grid: nothing of that is opened, looked up, sent or
kept, and the flight is an example that Booklight carries and that says so. In the three things to try, an app's
row and a search inside an app are practice and start nothing; the sum's answer is copied, as on any day. To quote
the two buttons of the system's Keyboard shortcuts dialog and the name of the key it suggests, Booklight reads
those words from the system's own resources, as any app reads a label, and it asks which keyboards are attached
and whether they have the Quick Insert key. Neither needs a permission, and nothing of either is kept or sent.

**Flights: nothing is sent unless you put in a key of your own.** A flight number you type (`LH455`) is
read on the device: Booklight carries a table of airlines, names the airline, and Enter opens the
flight's page in your browser. If you put a key of your own for AirLabs (airlabs.co, a flight data
service) into the Booklight window › Labs › Flights, the row also shows the flight's times, gate
and status. For that, the flight number and your key are sent over HTTPS to airlabs.co, and nothing
else is: no cookies, no identifiers; like any internet request it shows AirLabs your IP address, and
AirLabs can tie the lookups to the account the key belongs to. When: once, a moment after you stop
typing, for text that reads as a flight number and as little else; for text that is usually something
else (`ps5`, `ms 365`) only when you move to its row; for a flight number found in text you copied or
another app handed over, likewise only when you move to its row; after the keyword `flight`; and, for a flight you
pinned, again while its small window is open, until it has landed: every half hour from three hours before
it leaves, every three hours before that, sooner after a lookup that got no answer, and not at all while it
is only a plan from the timetable more than ten hours off. The answer is kept
in memory for two minutes (that a number is not known, for an hour) and written nowhere. The key is a file in the app's own storage; it is not
part of your backup and goes to AirLabs only. Take it out in the same place and nothing is sent again.
Booklight ships no key and its developer sees neither yours nor your lookups.

**Spotify: nothing is sent unless you put in a key of your own.** Without a key, Spotify's row has Search:
Enter hands your text to the Spotify app on your device, which shows its search results; Booklight itself
sends nothing. If you put
a key of your own for Spotify's Web API (a client ID and its secret, from developer.spotify.com) into the
Booklight window › Labs, Spotify's row also has Play, and Booklight looks the name up so that Spotify can play it. For that,
the text you type while Spotify stands in the field and Play is armed (without the app's name) and the country your device is set to are sent
over HTTPS to api.spotify.com, with a token Booklight gets by sending your key to accounts.spotify.com, and
nothing else is: no cookies, no identifiers; like any internet request it shows Spotify your IP address, and
Spotify can tie the lookups to the account the key belongs to. The token is the key's, not yours as a
listener: nothing of your Spotify account (your playlists, what you listen to) is read. When: each
time you pause typing there for about half a second, the text as it stands then (so a slow hand
sends the start of a name before the whole of it), while Play is armed and Spotify is in the field because you chose
it (you pressed Enter on Play on Spotify's own row; or you typed `play` and named Spotify, or played in it last, or it is
the only music app that plays). Nothing is sent while Search is armed, while you are typing without a pause, or for text
another app handed over. Where `play` puts Spotify in the field merely because it comes first of several, for a single
letter, and while another row stands above Spotify's (`play store` finds the Play Store first), nothing is sent
until you press Enter on Play. Where Spotify's row stands under another music app's after `play`, it is asked when
you move to that row. An album takes a second request (for its first song), and so does an artist (for a song of their
own). What Spotify found is kept in memory for five minutes, and the token until it runs
out (an hour); neither is written anywhere. If you make a recipe step of a song that was found, the song's
link is kept with the recipe. The key is a file in the app's own storage; it is not part of your backup and
goes to Spotify only. Take it out in the same place and nothing is sent again. Booklight ships no key and
its developer sees neither yours nor your lookups.

**Calendar events.** An event you type (`event dinner tomorrow 7pm`, or a sentence that begins with add, schedule,
put, book or plan, in German with „trag … ein“, „plane“ or „neuer Termin“, and holds a day or a time; both, after
schedule, put, book and plan, which as often begin a search) is read on the device, by Booklight's own rules. Where those rules cannot tell the title from the place, the model on the
device (see "Answers on this device") is asked which words are which, a moment after you stop typing. It works out
no date, and what it says is shown only if every word of it is one you typed.
**Enter hands the event to a calendar app**: its title, its time and its place. Where the event names no
calendar, that is Android's own request to add an event, as Booklight has always sent it, to whichever calendar
app there is. Where there is a calendar to name, it is a calendar link
(`https://calendar.google.com/calendar/render?…`) with that calendar's address in it (for most calendars an
e-mail address), given to Google's Calendar app and to no other: for the calendar you named; and, with "Save
events without opening Calendar" on, also for the calendar new events go to, where the row shows it and you
choose "Open". Where no such app takes the link, the same event goes as Android's request, without a calendar.
The calendar app opens its editor, filled in, and you save the event there, or you don't.
**The list of your calendars is read only if you allow it**, in one of three places in the Booklight window:
Privacy › Calendars; "New events go to" under Results; or the switch "Save events without opening Calendar", which
asks for both, to read the list and to add events. What is read is the list of the calendars of your Google
accounts and nothing else: for each its name, its colour, the id the system has for it, its owner's address, the
account it belongs to, whether it is that account's own, and whether events can be added to it. Booklight never
reads an event. Nothing of the list leaves the device through Booklight, and the list itself is kept in memory
only: it is not written to Booklight's files. Android has one question for all of this and asks it about "your
calendar" as a whole; what Booklight asks the system for is the list of calendars and nothing else. (Where you
have already allowed the list, Android does not ask again when you switch saving on: its one question was for
the calendar as a whole. The switch stays off until you press it.)
**Booklight saves an event itself only if you switch that on** ("Save events without opening Calendar", the
Booklight window › Results). Then Enter, or a click, on Save writes the one event the row shows, with its
calendar's own default reminder: into the calendar you named; with none named, into the one chosen under "New
events go to"; with none chosen, into your account's own calendar, or, where that takes no events, the first of
your calendars that does. The row shows which before you press Enter. Only where nothing of when it is is a
guess: the day and the time were both read from what you typed (the time with its half of the day: "7pm",
"19:30", „9 Uhr“, "7 in the evening"), or the day with "all day", or a range of days, which are saved as all-day
events that leave the day free. Never a line that asks for a repeat, one that says "next Tuesday" (people mean
two days by it), one with a date that had just passed and would land a year ahead, one that names a calendar
which is none of yours, or one typed without the keyword that begins with schedule, put, book or plan. Any
other event opens in a calendar app, filled in, as above. An event
that was saved is an ordinary event of your calendar: the account the calendar belongs to syncs it, as it does
every event, under its own terms. Booklight sends it nowhere, never changes or removes an event, and keeps no
copy of one.
**What Booklight keeps of this**, in its settings: whether the switch is on; that Android's question has been
answered on this device; and, once you choose a calendar under "New events go to", that calendar's owner's
address (for most calendars an e-mail address). The settings are in your own Android backup and device transfer,
and so that address is too. The switch and the answer are believed only on the device they were set on: settings
that arrive with a backup never have saving on.
A line that reads as an event is not sent for search suggestions; before it does (no day or time typed yet) it is
a typed line like any other.

**Handing things to other apps.** A web row opens in your browser. A mail, an event, a note for Keep, a
timer or a question for Gemini is handed to the app that opens: your mail app, the Calendar app (see "Calendar
events" for what it is given), the Clock, Gemini. Booklight sends none of it anywhere itself, and a mail or a Gemini question is only filled in:
you send it there, or you don't.

**Answers on this device.** A prompt (`fix …`, `sum`, one of your own) is answered by the system's own
model, Gemini Nano, which Android keeps and runs on the device; Booklight reaches it through Google's
ML Kit library. Your text and the answer stay on the device: they are not sent to Booklight's developer,
and the system's service does not keep them. That library, like all of ML Kit, **reports how it is used
to Google**: the device model and Android version, Booklight's name and version, an identifier made for
this installation, which feature ran, how long it took, how large the input and the answer were, and
error codes. Not the text. It sends this in the background when the network allows. Booklight cannot
switch that off in this version; if you never use a prompt, the model is never asked. (The row that plays
in a music app has one such question behind its arrow, "Which song is this?": the model is given the name you typed
there when you press Enter on it, and only then.) Where the device has
no such model, a prompt opens the Gemini app with your text in its prompt instead, and you send it there
or you don't.

**The pinned window** shows what you pinned until you close it; a pinned line is kept nowhere else.

**The clipboard.** When you open Booklight within two minutes of copying something, it asks Android what
kind of thing was copied and how old it is (text, and whether the system found a link, a date, a phone number
or a mail address in it) and shows one line under the empty field. What was copied is not read for that, and
Android shows no "pasted" message. It is read when you press Tab or Down on that line or click it (Tab on the
empty field also opens a fresh copy when the line is switched off), or when you ask for it (`clip`, `tr` or a
prompt with nothing typed, or a link of yours that uses `{clipboard}`). It is given to the model on the device
when you press Enter on one of the model's rows, or a moment after you stop typing under a prompt's keyword or
`tr` with no text of your own. Something a password manager marked as private is never read and gets no line.
The line can be turned off in the Booklight window › What you copied.
**Text from another app** (its selection menu, its share sheet) is shown in the panel and forgotten when
the panel closes.

**Permissions.** Internet, for suggestions, for flight and Spotify lookups with your own keys, and for the library's usage reports. Asking Android to
uninstall an app (Android asks you before it does). Setting alarms and timers in the Clock app. Changing
the brightness, which does nothing until you switch on "Modify system settings" for Booklight yourself.
Since 2.0, from Google's library: connecting to the system's on-device AI service, and seeing whether the
network is up. None of these shows a prompt. The Calendar permission does: Android asks you, and only when you
press Allow… in the Booklight window (on Privacy › Calendars or on "New events go to": to read the list of your
calendars), or switch on "Save events without opening Calendar" there (to read that list and to add the event
you save). Until then Booklight reads and writes nothing of it. No
accessibility service, no notification access, no access to your contacts or location. Nothing of Booklight's own runs in the background; the library's usage
report is a short job the system runs. (A pinned flight's window is on your screen, not in the background:
it asks only while it is there.)

The exact addresses and details are in [store-submission/forms/data-safety.md](store-submission/forms/data-safety.md).
Questions: kuscher.projects@gmail.com.
