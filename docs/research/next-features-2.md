# Booklight: what to build after 2.1

*1 October 2026, after 2.1. It answers Alex's request: "look for all the features we didn't implement and
brainstorm 10 more yourself which would be high value adds", with his own three thoughts: a screenshot taken when
Booklight is engaged and asked about with Gemini Nano, other good Nano features, and acting on what was copied
("hit tab and have translate options", perhaps as a suggestion when nothing is typed).*

**What it rests on.**
- [not-built-2.0.md](not-built-2.0.md): every idea that was written down and is not in the app: 143 ideas, 9
  built, 134 not, each with its old ratings and what was decided about it.
- [screen-and-clipboard.md](screen-and-clipboard.md): what the on-device model can do today, ten ways to a
  picture of the screen and what each asks of the user and of Play, how other products do it, Android's
  clipboard rules, and fifteen candidate features. Desk research with sources.
- [device-findings.md](device-findings.md), last section: what was **tried on the Lenovo** for this list.
- [languages.md](languages.md): the plan for more languages (§6 below has the summary).

**Tried on the Lenovo, so no longer a guess**
- The model answers about a picture. First word after 0.7 s. A picture costs about 256 tokens whatever its size.
- A picture of **a region or one window** is read correctly: the lowest price in a table, a build error with its
  file and line, the numbers in a terminal.
- A picture of **the whole screen** is not: asked what the terminal says, it reported errors that are not there.
  It gets the layout right and makes up the text.
- The clipboard tells its type, its age and whether it holds a link, a date, a phone number or a flight
  **without being read**, so without the system's "pasted from your clipboard" message.

**Effort:** S a day or less · M a few days · L a week or more. **Asks for:** what the user or Play has to grant;
"nothing" means no permission, no prompt, no form.

---

## 1. The list, in order

"Yours" = from Alex's message. "New" = one of the ten of §3. A code (C1, L8…) = carried from the old catalogue
([not-built-2.0.md](not-built-2.0.md)).

| # | Idea | From | What you do, and what you get | Effort | Asks for |
| --- | --- | --- | --- | --- | --- |
| 1 | **The copy, as a row** | Yours, with F6 | Copy something, open Booklight: one quiet row says "Copied 20 s ago: a link and a date". Tab opens what can be done with it: Open the link, Add the event, Call, then Translate, Fix, Shorter, Summarise, Explain | M | Nothing. Whether a row may show with nothing typed is your call (§5) |
| 2 | **Ask about a picture** | Yours | Capture a region or a window, open Booklight, ask: "what is this error", "which is cheapest". Also Copy its text, Add the event in it, Pin it | M; L with whole screens | Nothing for a picture you hand over. Whole screens need a text-recognition library (§5) |
| 3 | **Tell it what to do with the copy** | New | `do make this a bullet list`, `do answer politely that Friday works`: what you type is the instruction, what you copied or selected is the material | S | Nothing |
| 4 | **Put the letters back** | New | `ä Gruesse aus Koeln, ich komme spaeter` gives "Grüße aus Köln, ich komme später". French, Danish and the rest too. Only letters may change, so it cannot rewrite you | S | Nothing |
| 5 | **Prompts in the selection menu** | New | Select text in any app, right-click: "Fix spelling", "Translate", "Shorter" stand there by name, and the answer replaces the selection | S to M | Nothing. One device check |
| 6 | **More ready-made prompts** | New, AI1, AI4 | `error …` (what it means, the likely cause, and a web search of its first line beside it), `commit` (a message from the diff you copied), `reply`, `formal`, `friendly`, `define` | S | Nothing |
| 7 | **Say it plainly** | I4 | "lunch with Anna next Thursday at one at Borchardt" becomes the Event row, filled in. The model only sorts the words; Booklight's own date reader decides the day | M to L | Nothing |
| 8 | **Pick a colour from the screen** | C1 | `pick colour`, click a pixel, get HEX, RGB, HSL, OKLCH in the colour row | S | Nothing (new in Android 17). One device check |
| 9 | **A guess when nothing matches** | New | Three or more words that find nothing: after a pause a row offers "Did you mean: Timer, 10 min" or the settings page for "make the screen less blue at night". It never runs by itself | M | Nothing |
| 10 | **Text tools, and a clean link** | D1, New | `b64`, `jwt`, `sha256`, `epoch`, `json`, `0xff`; and a copied link without its tracking tail, or as a Markdown link | S | Nothing |
| 11 | **A key per command** | L8 | Action + E opens the emoji grid, Action + N a note: each a thing the system's shortcut settings can bind | S to M | Nothing. One device check |
| 12 | **Pin a picture** | New | A captured region stays on top in the small window that 2.0 built: the table you are typing from, the design you are matching | S to M | Nothing |
| 13 | **Units, money, time zones** | X1 | `5 ft in cm`, `120 usd in eur`, `3pm berlin in sf`, answered by Summa's engine | M | Nothing |
| 14 | **What other apps can do with a thing** | F3, F4, F5 | A file, link or text gets "Send to PDF Toolbox", "Edit in Canvas", another app's "Translate", from what those apps already declare | M | Nothing |
| 15 | **Stop what is ringing** | New | `stop` ends the timer or alarm that is going off; `timers` shows them. Booklight starts timers and cannot stop one today | S | Nothing |
| 16 | **`abc` by keystrokes and by word** | New | `abc "u` is ü, `abc 'e` é, `abc /o` ø (the compose sequences many people know); `abc Koeln` gives Köln | S | Nothing |
| 17 | **Tasks out of a text** | New | Copy the notes of a meeting, `tasks`: each thing to do is a row; Enter adds it to Todo.md | S | Nothing |
| 18 | **Emoji by description** | New | `emoji we shipped it` finds 🚀 🎉 when no emoji has that name: the model picks from Booklight's own table | S | Nothing |
| 19 | **Your people** | P1 | `anna` gives Mail, Meet, Copy address, from a short list kept in Booklight | M | Nothing |
| 20 | **Translate into any language** | I5 | `tr danish see you on Saturday`; today only one fixed direction is seeded | S | Nothing. Which languages the model writes well is unknown (§6) |
| 21 | **Packs** | A1 | Links, snippets, recipes and prompts as files in the notes folder: edit them in an editor, share them | M | Nothing |
| 22 | **More languages** | Yours | French, Spanish, Italian, Danish, Swedish, Norwegian; then Polish; then Japanese, Korean, Chinese | L: about four weeks for the first six | Nothing. Thirteen decisions ([languages.md](languages.md) §12) |

**Not on the list, on purpose.** Still waiting for the "power pack" and its accessibility service: paste in
place, open windows by title, lock and screenshot, notifications as rows, clipboard history. Still "not yet":
sending mail without the compose window. A picture of the screen taken silently when the panel opens: §2.2.

---

## 2. Alex's three ideas, as they would be built

### 2.1 The copy, as a row

**Today.** `clip` shows what was copied with Note, Mail, QR code, UPPERCASE, a count, and the prompts by name;
a prompt with nothing typed uses the copy. All of it is behind a keyword.

**What changes.**
1. *A row when nothing is typed.* If something was copied in the last two minutes, the empty panel shows one row:
   the clip's kind and age, and what the system found in it. Booklight reads only the clip's description for
   this (tried on the Lenovo): the text itself is not touched and the system shows no "pasted" message. A clip
   an app marked as sensitive (a password manager's) never gets a row; nor does Booklight's own copy.
2. *Tab opens it.* Now the text is read. First what is in it, with no model and at once: Open the link, Add the
   event (the date goes through Booklight's own reader), Call, Open in Maps, Track the flight. Then what the
   model does, on Enter: Translate, Fix, Shorter, Summarise (only for a long text), Explain.
3. *Translate knows the direction.* The system says what language the text is in (tried: German, 0.998). A text
   in your language is translated out of it, any other into it.
4. *A picture on the clipboard* gets the rows of §2.2 in the same place.

**Why first.** It is the idea with the most reach for the least: everything except the row and the list of
options exists, most of the options need no model, and every comparable tool leads with exactly this (Gboard's
strip after a copy, PopClip, Raycast, PowerToys "Paste with AI": summarise and translate are Microsoft's first
two examples).

**The catch.** PLAN §11.3 says "Nothing typed: nothing at all". Tips already bend that. This would be the second
exception, and the first that depends on what you did in another app. §5 asks.

### 2.2 Ask about a picture

**What the device said.** The model reads a region or a window well and a whole screen badly. So the feature is
"ask about what you framed", not "ask about everything on screen".

**How the picture gets to Booklight**, lightest first (all need no permission):
1. *From the clipboard*, if the Googlebook's capture tool copies what it captures (ChromeOS does; **not checked
   on the Googlebook**: take a region screenshot, then look whether Paste offers it). Then it is: capture,
   open Booklight, ask.
2. *Shared to Booklight* from the capture's preview: about three steps. Booklight takes text this way already.
3. *Picked* in the system's photo picker, where the newest capture is the first tile.

**The rows for a picture.** Ask (type the question; the answer is written into the row, as a prompt's is);
Copy its text; Add the event in it; Read its QR code; Pin it (idea 12); Save it.

**Whole screens** need the text read first by a text recogniser (Google's ML Kit one, on the device, about 4 MB,
and one more Google library with the usage reporting 2.0 already accepted for the model's library). With it,
"Copy the text from this picture" works without any model, and a whole-screen question is answered from the
text. Without it, the feature is honest only for regions and windows. Suggested: ship regions first, decide on
the recogniser after using it.

**Taking the picture silently when the panel opens** is possible in two ways only, and both are a different kind
of app:
- *As the device's digital assistant.* The system then hands Booklight a picture of the screen and the text of
  every visible window, with no dialog. It replaces Gemini on its key, keeps a service of Booklight's running
  always, and the picture is of the whole screen: the kind the model reads badly, so it would lean on the
  windows' text. Chrome hands over the page's address and text this way.
- *An accessibility service.* Play's declaration, an in-app consent, a service always running.

Neither is recommended now. If regions prove useful, the assistant route is the one to look at, because its text
of every window is better material than any picture.

### 2.3 What else the model is good for

From the fifteen candidates in [screen-and-clipboard.md](screen-and-clipboard.md) §5, by how they suit a small
model that runs only while the panel is open and may be wrong:

| Good fit | Why |
| --- | --- |
| Rewriting what you give it: fix, shorter, tone, translate, put the letters back (ideas 3, 4, 6) | Short in, short out; you see the result before you use it |
| Choosing from a closed list: which command, which settings page, which emoji (ideas 9, 18) | It cannot invent an answer that is not on the list |
| Sorting words into slots: a sentence into title, day, time, place (idea 7) | Booklight's own code checks every slot |
| Reading a framed picture (idea 2) | Tried on the device |
| Explaining a short text: an error, a word (idea 6) | Useful when right, and the web search stands beside it |

| Poor fit | Why |
| --- | --- |
| Answering as you type | A quota, and one to two seconds each time |
| Sums, conversions, dates done by the model | Wrong numbers that look right. The model may name the parts; code computes |
| Reading a whole screen, exact numbers from a chart | Tried: it invents |
| Anything that runs by itself | A model's row never runs anything: 2.0's rule, kept |
| Long documents | About 3,000 words in, and a long answer takes 10 seconds and more |

---

## 3. Ten new ideas

Each is new to the catalogue. Numbers are the rows of §1.

**3 · Tell it what to do with the copy.** The five prompts are fixed instructions; this is the open one.
`do turn this into a table`, `do pull out the dates`, `do write a polite no`. What is typed is the instruction;
the material is what you copied, or the text you selected and handed over. It is one more seeded prompt whose
text has two holes instead of one, so it costs almost nothing, and it is what PowerToys calls "Paste with AI".
Risk: an instruction the model follows badly. The answer is only ever shown, copied or put back.

**4 · Put the letters back.** For writing German, Danish or French on a keyboard without their letters, which
is what `abc` was asked for, one letter at a time. Type the whole sentence the way people do without the keys
("Gruesse aus Koeln", "ca va tres bien", "jeg kommer pa lordag") and get it back with its letters. The model
does the language part (it knows "Wasser" keeps its ss and "Strasse" does not). Booklight then checks that
nothing but letters changed (the answer, with its marks taken off and ä read as ae, must equal what was typed),
and shows nothing if the model changed a word. That check makes it the one model feature that cannot say
something you did not.

**5 · Prompts in the selection menu.** Booklight already stands in the menu that appears on selected text, as
one entry that opens the panel. Each prompt can be an entry of its own ("Fix spelling", "In German"), switched on
one by one in the window so the menu does not fill up. Select, right-click, pick: the panel opens with the
answer already being written, and Enter replaces the selection where the field allows it. No permission: it is
the same door 1.1 opened, with more name plates. To check on a device: that the Googlebook's menu shows several
entries of one app, and how many before it folds them away.

**9 · A guess when nothing matches.** Today three words that match nothing give a web search. After a pause the
model is asked one question: which of Booklight's own commands and settings pages, if any, is this? It answers
with one line from a list it was given, so it cannot invent a command, and the row says what it understood
("Timer · 10 min", "Night Light · Settings"). Enter runs that row as if you had typed it, through the same
parsers. Before the model, the cheap half: more everyday words per settings page and per command, which is
instant and needs no model. It also answers "how do I …" with the matching line of the list of everything.

**10 · A clean link.** A copied link often carries a tail that tracks (`utm_…`, `fbclid`, `gclid`, `si`). On a
link, the copy row (idea 1) offers "Copy clean" and "Copy as Markdown". No model, no network, a few lines of
Kotlin and a list of parameter names. It sits with the old catalogue's text tools (D1: `b64`, `jwt`, `sha256`,
`epoch`, `json`), which are the same kind of work and were not chosen for 2.0.

**12 · Pin a picture.** 2.0's small window on top shows text, an answer, a colour, a QR code or a countdown. A
picture is the obvious sixth: the region you captured stays visible while you type from it or build next to it.
Tools that do only this are popular on other desktops. It needs the picture hand-over of idea 2 and little else;
the window itself can already be dragged and resized by the system.

**15 · Stop what is ringing.** Booklight can start a timer and an alarm through the Clock app and has no word
for ending one. Android has had the matching requests for years: dismiss the expired timer, dismiss or snooze
the alarm, show the timers. `stop` when something rings; `timers` otherwise. Found the hard way: six test timers
rang on the Lenovo for two hours because nothing in Booklight could stop them.

**16 · `abc` by keystrokes and by word.** Two small additions to 2.1's letters. The compose sequences people
know from other systems: a mark and a letter (`"u` ü, `'e` é, `` `a `` à, `^o` ô, `~n` ñ, `/o` ø, `,c` ç,
`oa` å, `ss` ß). And a whole word typed without its letters, for the languages where that is unambiguous
(`abc Koeln`, `abc Malmoe`), which is idea 4 without the model for a single word.

**17 · Tasks out of a text.** Copy the notes of a meeting or a long mail; `tasks` lists what somebody has to do,
one short row each. Enter adds a row to Todo.md (the `todo` list 2.0 built), Shift + Enter adds it and stays.
The model proposes; you choose which become tasks. It joins the two things 2.0 added that do not know each
other yet: the model and the notes.

**18 · Emoji by description.** The emoji grid finds by name and keyword. "we shipped it", "I am sorry", "good
luck with the exam" have no emoji by that name. When the name search comes back empty, the model picks up to ten
from Booklight's own table (it is given the names, it returns names), and the grid shows them. A closed list, a
harmless result, and the most-used picker gets better.

Two more that came out of the research and ride on idea 6: **"what is this error"** (copy or select an error,
get its meaning and likely cause in two lines, with "Search the web for its first line" as the next action) and
**a commit message from the diff you copied**.

---

## 4. From the old list: what is still worth doing

Of the 134 unbuilt ideas, these keep their place (their rows in §1): pick a colour (C1), a key per command (L8),
say it plainly (I4), text tools (D1), Summa's answers (X1), what other apps can do with a thing (F3, F4, F5),
your people (P1), translate into any language (I5), packs (A1). What the text is decides the rows (F6) is now
the first half of idea 1.

Worth a look after those, all small and asking for nothing: Share… on every row (P6, partly built), settings
rows that land on the switch (S1, partly built), answers about the device (`battery`, `storage`, `ip`: S2),
sound output (S5), large type (C3), what you ran (`!!`: L10), several emoji in one go (DR8), copy with the sum
(DR7), snippets with the date or the copy filled in (PL11).

Waiting on his other apps, each a small release there: StudioSnap's capture modes (X4), PDF Toolbox's tools
(X5), BentoBar's timers and its lock and screenshot (X2, X3). With X4, "capture a region and ask about it"
(idea 2) becomes one command.

The full inventory, with each idea's old ratings and what was decided: [not-built-2.0.md](not-built-2.0.md).

---

## 5. Decisions for Alex

1. **A row with nothing typed** (idea 1): yes, for a copy younger than two minutes? Or keep "nothing typed,
   nothing shown" and put the copy behind Tab on the empty field?
2. **The text recogniser** (idea 2): add Google's on-device library (about 4 MB, the same usage reporting as the
   model's library), or ship regions and windows only?
3. **Does your Googlebook's capture tool copy the picture?** Capture a region, then try Paste somewhere. It
   decides whether idea 2 is "capture, ask" or "capture, share, ask".
4. **Silent capture** as the digital assistant: look into it later, or rule it out?
5. **Which of 3 to 21** go into the next release. Recommended as one release: 1, 3, 4, 5, 6, 10, 15, 16. All
   small, all without a new permission, and together they are "Booklight works on what you copied or selected".
   Then 2 with 12 as the release after.

---

## 6. Languages, in short

Full plan: [languages.md](languages.md).

**Translating the screens is the easy fifth; the hard part is that Booklight is typed.** German was done by
adding words to 50 keyword lists and 15 word tables in the code, by hand, and all of it assumes spaces, Latin
letters and English date order.

| Language | How hard | Why | The device's model | Days |
| --- | --- | --- | --- | --- |
| French, Spanish, Italian | Easy | Day-first dates, "15h30", "en" and "de" are everyday words | On Google's lists for fixing and rewriting | 2 each |
| Danish, Swedish, Norwegian | Easy | ø and æ are not matched by o and ae today; "kl. 15.30" | On no list: to be tried | 2, 2, 1.5 |
| Polish | Medium | Three plural forms, day names that change with grammar, `w` is a word | On no list: to be tried | 3 |
| Korean | Medium to hard | Half-typed syllables, particles | On all lists | 4.5 |
| Japanese | Hard | An input method, no spaces, kana and kanji | On all lists | 7 |
| Chinese (both scripts) | Hard | Pinyin matching, no spaces | On no list: to be tried | 6 |

About **10 days of one-time work** come before any language (the typed words out of the code and into a file per
language, numbers and dates by locale, a test that checks every language's keywords for collisions and limits),
and **9 more** before Japanese, Korean or Chinese (matching by kana, Hangul and pinyin; the field under an input
method; a date reader that needs no spaces). The six western European languages together: about four weeks.
All eleven: about fifty working days. The figures are estimates from counts (463 strings, 692 units, 2,934
words per language), not from having done one.

The lasting cost is not the first translation: every new keyword, tip and release note is then written twelve
times and checked by somebody who reads the language. German already sits at three limits (12-character place
names, 90-character tips, 3,995 of Play's 4,000).
