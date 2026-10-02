# Booklight after 2.1: the plan in four milestones

> **Adjusted later the same day.** Alex chose M1 for the main panel and added a new M5, flights. The plan in force
> is `plan-next.md`, beside this file. M2, M3 and M4 below are parked, unchanged; M1 below is still M1.

*The plan the team stands behind, 1 October 2026: product manager, UX designer, visual and motion designer,
engineer. Planning and design only. Nothing was built, no device was touched, nothing in the repo was changed.
Nothing is built before you approve it.*

*File: `docs/design/milestones/plan.md`.
The same plan with its eleven screens drawn at real size is the page
`docs/design/booklight-milestones.html` (made by `tools/design-milestones/gen.py`); the two agree. The papers behind it are
in the same folder: `01-frame.md` (the product manager's first draft), `02-ux.md` (**UX**), `03-eng.md`
(**ENG**), `04-design.md` (**DES**). The research is named as there: **NF2** = `next-features-2.md`, **S&C** =
`screen-and-clipboard.md`, **DF** = `device-findings.md`, **LANG** = `languages.md`, **NB** = `not-built-2.0.md`.
"Check n" is a line of the engineer's device session (ENG §12). "Not verified" is said wherever the research
says it.*

*Last pass, after two reviews of the page (design and product): the typed language and `tr` moved from M3 to
M1; `error` left the four; six things the team had "settled" became decisions 9 to 14; the screens are
numbered 1 to 11 in the order of the page (1 to 4 are M1, 5 to 7 M2, 8 and 9 M3, 10 and 11 M4).*

---

## 1. The four, at a glance

| # | Name | The promise, in one sentence | Size | New permission |
| --- | --- | --- | --- | --- |
| M1 | **The copy** | Copy something, open Booklight, and one line offers what can be done with it. | About 14 days | None |
| M2 | **The picture** | Capture a piece of the screen, open Booklight, and copy its text, ask about it or keep it on top. | About 14 days | None. One more Google library (decision 4) |
| M3 | **The selection** | Select text where you are writing, right-click, and Booklight rewrites it in place. | About 8 days | None |
| M4 | **Your language** | Booklight speaks six more languages: its screens, its keywords and its dates. | About 17 days and the wait for readers, after 12 days of groundwork | None |

**One idea carries the first three.** Something you brought (a copy, a picture, a selection) becomes the chip
in the field, and the rows under it are what can be done with it. 2.0 already does this for text another app
hands over (UX §2, DES §2). M1 builds it once; M2 and M3 each add one new door to it.

**Three of the four are your own ideas:** M1, M2 and M4. M3 is not one of your three. It is the team's
proposal for the step after the copy, from the research's ideas 5, 4 and 6 (NF2 §1).

**The sizes are the engineer's** (ENG §13), made before the team's last changes. M1 and M2 were not re-sized
after their cuts, the choice "the answer where it stands" was never sized, and one rule (dates read in the
text's own language) has no size yet. Two things moved in the last review, with the engineer's own figures:
the typed language and `tr` went from M3 to M1 (1.5 days, ENG §7.3), and `error` left the four (half a day,
ENG §7.4). That makes about 65 days with the groundwork (the engineer's table says 66), without the settings
window and today's polish. The cheapest forms that still keep each promise add up to 42, with three languages
in M4, not six. Read the days as sizes against each other, not as a calendar: this project has run far ahead
of such figures before (ENG §1).

---

## 2. Where the team disagreed, and what was chosen

"First draft" is the product manager's first draft (`01-frame.md`).

| # | Question | What each said | Chosen |
| --- | --- | --- | --- |
| 1 | The chip when a model's row is run | UX: the chip stays the thing, the row is answered where it stands. DES and ENG: the chip becomes the prompt, the field stays empty | UX's. The picture needs it anyway (there is no prompt to turn its chip into), and ENG's shared piece 1 is its base. ENG's form is the fallback if it proves too dear in M1 |
| 2 | A keyword for "say what to do with it" | First draft: `do`. ENG: `do` and a space would swallow "do not disturb". UX: no keyword | None. Under the chip, what you type is the instruction. From the empty field: `clip` and the instruction |
| 3 | The quiet line's layout | UX: the words, the age at the right end, a `tab` cap. DES: one text with the age in it, one cap | DES's layout (one text has one baseline), UX's words |
| 4 | The line after the field is emptied again | UX: it comes back. DES: it stays away, or it flickers while you edit | It stays away. It returns only by Backspace out of its own chip, which is Tab backwards |
| 5 | The rows after Tab | DES: eight, with Explain and the web row. UX: at most three things found, your first three prompts (the translation among them), Summary for a long text | UX's. The rest is found by typing its name |
| 6 | The right end of a model's row | DES: bare. UX: "On this device", or "Gemini" | UX's. It is how you see before Enter that a row hands over. Three such labels stack in screen 2; it is the first thing to thin out in a "fewer things" pass |
| 7 | The things found in a copy | First draft: link, date, phone, address, flight, and Copy as Markdown. ENG: a flight is only a web search, Maps only a web address, addresses were not found in the Lenovo test, a Markdown link has no page title | Link, date, phone number, mail address. Copy clean stays. Flights, addresses and the Markdown link are out |
| 8 | The rows under a picture | DES: three rows, the answer beside the picture. UX: one row (the picture and its text), the answer as a row above it. ENG: a "Copy its text" row from the start | UX's rows on DES's measures. Enter on a picture copies its text, and the answer gets a full-width row |
| 9 | An answer's caption about a picture | UX: "On this device · from the picture". DES: the source first, and "on this device" is the part that drops when both do not fit | DES's order: "From the picture · on this device" |
| 10 | The doors for a picture | UX: clipboard, Share, the photo picker, and a keyword `pic`. ENG: three doors are three flows; the picker last and cut first | Two doors. The picker and `pic` are built only if both doors fail on the device |
| 11 | `error` | First draft: M3. ENG: M2, a picture of an error is M2's best test. The last review: its own example is a copy, and on a picture typing the question does it | Not in these four. You can add it under Yours (§10) |
| 12 | Where "translate into any language" goes | First draft and ENG: M3. The last review: it answers your M1 words, "have translate options", and the engineer had already moved its base to M1 (ENG §11) | M1. Its 1.5 days move with it |
| 13 | One word without its letters (`abc Koeln`) | First draft: no model. DES: a large computed answer. ENG: a blind swap turns "neue" into "neü" | Through the model and the same check as a sentence, in an ordinary answer row |
| 14 | The chip of `tr danish …` | DES: it changes to the language's name. UX: it stays "Translate" and the row says the direction | UX's. The row says "In Danish", the one form used everywhere |
| 15 | The names in the selection menu | First draft: any prompt, "In German". ENG: a name is fixed when the app is built, so only ready-made prompts | "Translate". Your own prompts stay behind "Booklight" |
| 16 | Where the language groundwork ships | First draft: inside M3. ENG: merged before M3 is started, better as a quiet release | ENG's |
| 17 | The picture before the selection | UX: the selection is the nearer step, but no swap asked. ENG: the picture second is right if the HP has no model | The picture second, with one swap rule (§8) |
| 18 | Six languages at once | First draft: yes. ENG: French, Spanish and Italian first, the Nordic three after their model test | One milestone, two steps |
| 19 | Dates in a thing you brought | First draft: the app's language and English. UX: a German mail copied into an English Booklight would lose its event | The text's own language for a copy, a selection or a picture's text. Not sized by the engineer; the reader takes a list of word sets (ENG §8) |
| 20 | The longest name of an action | UX: 16 characters, as today. DES: 20 | 16 |
| 21 | The sizes | First draft: about 40 days. ENG: 66 | ENG's, less what the last review moved out: about 65 |

---

## 3. What holds for all four

**Rules, not repeated below.** No new permission. A model's row never runs anything by itself. Nothing in the
background. Every string in English and German (the words are in UX §10). Every "not verified" gets its device
check before the feature is promised. The release build is installed and asked a prompt before every tag.

The model is asked on Enter, or when you pick its name in another app's menu; never after a pause in typing.
Ctrl + 1…9 never asks it. Esc closes at every step.

**One check stands before all four.** Your HP has never run 2.0 (`PICKING-UP.md`), and its model has never
been tried. If it does not answer there, every model row on the HP hands over to Gemini, and M1's model rows,
M2's Ask and all of M3 can only be judged on the Lenovo. It is check 1 of the first device session.

**The keys in the new states** (UX §2.4 has the whole table).

| State | Enter | Tab | Down | Typing | Backspace on the empty field |
| --- | --- | --- | --- | --- | --- |
| The quiet line | Nothing | Opens it | Opens it | Puts it away in the same frame | Nothing |
| A thing's rows | Runs the armed action. On a model's row: asks | The selected row's next action | Next row | Narrows the rows by name. What matches no row is the instruction, or the question about a picture | The chip goes; the line is back |
| A row being answered | Waits for the whole answer, then does the armed action | Next action | To the picture's row, if there is one | Takes the answer back | The rows are back |

**One state when the model does not answer** (UX §2.6). The caption says it in plain words, the thing's first
line stays, and the way to the Gemini app is the action. No colour, no warning mark. A device with no model:
the row's label is "Gemini" from the start. A text too long to rewrite here (over about 3,000 characters): the
label is "Gemini" before Enter. An answer that was cut says "cut off here", and Replace is never first on it.

**Two lights, one outline** (`design-system.md` §12, today). Both are white and run along the panel's edge.
The *reflection* runs once, a few seconds after an idle panel has opened: it is for fun and means nothing. The
*slow lap* runs round and round, dimmer, while the model has been asked and has said nothing yet: it means
"working". This plan adds no light of its own. In light theme over a white window neither can be seen
(accepted today); what a row being answered shows then is itself: the caption without "on this device" and
"Ask" in place of Copy. Whether that is enough is one of the things to judge on a device (§11).

**The lines every design is drawn on** (DES §1, from `Metrics.kt`, `Rows.kt`, `Field.kt`, `Strip.kt`). An
earlier mock-up was rejected as "really misaligned", so every screen uses these and nothing else.

| What | Value |
| --- | --- |
| Panel | 720 dp wide, radius 32 |
| Field | 68 high. Mark or chip from x = 20. Text from x = 72 |
| Rows | 56 · tall 92 · an answer grown to four lines 136 · list padding 8 above and below · footer 36 |
| Icon column | 36 dp disc centred on x = 38. Every chip's icon is centred there too |
| Strip | Flush right at x = 700. Slots at a 32 dp pitch, icons 18 |
| Chip | 36 high, from x = 20: 8 · icon 20 · 6 · name 15/600 · 14 |
| Type | Field 24/500 · title 17/500 · small 14/500 · hint 13/500 |
| Ink | 1.0 · 0.80 · 0.60 of `onSurface` |
| The quiet line | Row one's seat: y = 76 to 132, so the panel is 68 + 8 + 56 + 8 = 140 dp. Disc on x = 38. One text from x = 72 in the small type; the age at 0.80, the rest at 1.0 (both 1.0 in dark). Its `tab` cap ends at x = 700 |
| An answer's row | 2.0's, unchanged: 92 dp, grown once to 136. Caption small at 0.80. Text 17/500 on 22 dp lines, from x = 72 to 16 dp before the strip |
| A picture's row | 136 dp from its first frame. The picture hangs from x = 24, y = 16, fitted into 176 × 104 in its own shape; radius 12, a 1 px ring of ink at 0.20, no shadow. From x = 220: the picture's size, then the read text in the small type at 0.80 on 19 dp lines, four at most, the last one fading |
| A picture as a mark | In the chip: a 28 dp circle centred on x = 38. In a 56 dp row: 36 dp like every mark (edges at 20 and 56), radius 11, cut from the picture's top-left |
| A pinned picture | Edge to edge in the small window: 280 × 140 for a 2 : 1 picture (measured); no margin, caption, button or ring |

Take five things from the code, not from the 2.0 design page: the chip's padding; the strip after today's
polish (three placements and an app's own shortcuts); the dark highlight at the app's value of this afternoon
(`secondaryContainer` #33456F at 0.42 on glass: `design-system.md` §12); the edge light in white; small type
at 14 and hints at 13. The footer follows the design system's rule: "tab Actions" only with several actions,
"⌫ Leave" only under a chip with an empty field.

**The motion in the page's panels that play** follows §12's numbers at Medium: the seam for 160 ms, the glass
from 60 ms for 360 ms on cubic-bezier(0.55, 0, 0.1, 1), at rest 420 ms after the key; what stands under the
field may arrive when the glass is 85 % open (257 ms); the light 1.75 dp wide in dark and 2.0 in light, a
front of 40 dp and a tail of 36 to 140 dp, no glow; the reflection 2.4 s after the panel has opened, 1 ms for
each dp of outline; the slow lap at 1.5 ms for each dp and 75 % as bright. A browser draws them, so they are a
close copy, not the app.

**Nothing new in the look** beyond four elements, each built from parts the app has: the quiet line; a chip
with a picture in it; a row with a picture; a picture in the pinned window. No new colour, type size or row
height (DES §0, §8). One move is new and the motion designer has not drawn it yet: the picture riding down
when a question is typed (screen 5).

---

## 4. M1 · The copy

**Promise.** Copy something, open Booklight, and one line offers what can be done with it.

**Your idea, in your words:** "a function to grab the copy contents and act on them, like if there is text in
the copy buffer hit tab and have translate options. Maybe that's a suggestion for zero state if zero state is
coming and there was a recent copy".

**The story.** A mail in German is open in Gmail. You select a paragraph, press Ctrl + C and press the
Booklight key. Under the empty field one quiet line says "Copied just now · text". Tab opens it and the first
row is "In English"; Enter, Enter, and the translation is on your clipboard. Four presses, no typing. Want it
in Danish instead? After Tab, type `danish`.

### What is in it

1. **One quiet line for a fresh copy.** If you copied something in the last two minutes, the empty panel shows
   one line after it has opened. Booklight reads only the system's description of the copy for this (its kind,
   its age, what the system found), not the text. Android's documentation says the system then shows no
   "pasted" message (S&C §4.1). Reading the description was tried on the Lenovo (DF); the missing message for
   another app's copy is check 4, **not verified**. The system looks only at copies of up to 400 characters
   (S&C §4.1); a longer one always says "text". A copy marked private (a password manager's) never gets a
   line, if the app marks it (check 5), nor does Booklight's own copy. Enter does nothing on the line.
   *Example:* Ctrl + C on a chat message, then the key: "Copied just now · a link and a date".
2. **Tab: what is in it.** Tab or Down makes the copy the chip. Now the text is read, and the system shows
   "pasted" once. First come the things that need no model, three rows at most: a link (Open, Copy clean
   without its tracking tail), a date (the Event row, filled in, so you see the day before the calendar
   opens), a phone number (Enter calls if an app answers it, else copies it) and a mail address (Enter opens
   the compose window). What answers a phone number is check 7, **not verified**; no test text held a mail
   address, so that row is not tried. *Example:* Tab, Enter opens `example.com/table`. Tab, Down, Enter, Enter
   puts "Dinner with Anna, Fri 2 Oct, 19:00" into the calendar's editor.
3. **What the model can do with it, answered in the row.** Your first three prompts stand there by name; the
   translation is one of them (UX §3.3). Summary too for a long text. The translation knows its direction: a
   text not in your language comes into it and stands first; a text in your language goes into your other one
   (German in an English Booklight, English otherwise). Enter asks, the row grows into the answer, and the
   chip stays the copy. *Example:* the story above: the key, Tab, Enter, Enter.
4. **Into any language.** Your words were "translate options". Under the copy's chip, a typed language is the
   translation into it. From the empty field the same is `tr`, then the language, then the text. `de` and `en`
   stay. A language that read badly in the device test hands over to Gemini. *Example:* Tab, then `danish`,
   Enter, Enter. From the empty field: `tr danish see you on Saturday`.
5. **Say what to do with it.** Under the copy's chip, what you type is the instruction, unless it is a row's
   name or a language. There is no keyword for it: `do` and a space would swallow "do not disturb". *Example:*
   Tab, then type `pull out the tasks as a list`, Enter. From the empty field the same is `clip pull out the
   tasks as a list`.

Two changes to `clip` come with it. It opens the same list at any age, with fewer rows than today
(decision 10). And it no longer reads a copy marked private; it says "What you copied is marked private".

**This changes one thing in the released app** (decision 11): shared text behaves the new way too. Today Enter
on "Fix spelling" turns the chip into the prompt and types the whole text into the field. After M1 the chip
stays the text and the row is answered in its place. The selection handed over by today's "Booklight" menu
entry gets the same list at no extra cost, because it is the same code. Nothing more is promised for it
before M3.

### Deliberately out

Flights and addresses (a web search and a web address; addresses were not found in the Lenovo test, DF). Copy
as Markdown (Booklight does not know a page's title, so the link would hold its address twice: ENG §3). The
system classifier's ready-made actions (ENG §5.2: Booklight could not say what one does). A history of copies
and pasting in place (both need the power pack: NB, F9 and F10). A picture on the clipboard (M2). A line for a
copy older than two minutes. A setting for the two minutes.

### The designs (drawn on the page)

**Screen 1 · "Open, and the copy is there."** Plays. Light and dark, English and German.

| Part | Content | Layout (DES §3.1) |
| --- | --- | --- |
| Field | The G · placeholder "Search apps, settings and the web" · the caret · the `esc` cap | 68 dp |
| The line | Clipboard mark · "Copied 20 s ago · a link and a date" · a `tab` cap | §3's row "The quiet line". No highlight, no fill, no outline. In dark both parts of the text are full ink |
| Footer | None | The panel ends 8 dp under the line |
| Other states | "Copied just now · text" · "Copied 1 min ago · a link, a date and more" · „Kopiert vor 20 s · ein Link und ein Datum“ | Too long: it ends in the 24 dp fade, never an ellipsis. At most 80 characters |
| It plays | The opening at Medium as always, at 68 dp. Then the height goes to 140 and the mark, the text and the cap rise 12 dp and fade in, 22 ms apart. A typed letter takes the line away in the same frame | It waits 320 ms after the glass is 85 % open: the tip's hold. Never part of the opening. The page also plays it at 200 ms, to compare |

Beside it, a timeline of the first five seconds: the key, the glass at rest at 0.42 s, the line from 0.58 to
0.94 s, stillness, then the white reflection from 2.8 s for about 1.7 s. The reflection never starts while
anything under the field is arriving, while the line's words settle, or while an answer is being written
(DES §3.5; §12: 2.4 s after the panel has opened and 0.7 s after the last change of the list).

Over a white window dark glass turns mid-grey, and small text on it measures about 3.4 : 1 on the page; the
design system asks 4.5 : 1 for text that is said nowhere else (§8). So the line in dark over a white window is
a device check before this design is final.

**Screen 2 · "Tab."** Plays (Tab from screen 1). Light and dark. The copy: "Dinner with Anna on Friday at
7pm. Call +49 30 5550 1234 or book at https://example.com/table?utm_source=chat". Today is Thursday 1 October
2026.

| Part | Content |
| --- | --- |
| Field | `[clip · Dinner with Anna on…]` · placeholder "What to do with it" |
| ● 1 | Globe · example.com/table · Open ⏎ · (Copy clean: a link with a spark, not scissors, which read as Cut) |
| ○ 2 | Event mark · Fri 2 Oct, 19:00–20:00 · second line "Dinner with Anna" · label "Event" |
| ○ 3 | Phone mark · +49 30 5550 1234 · label "Phone" |
| ○ 4 | Spark · Fix spelling · label "On this device" |
| ○ 5 | Spark · Shorter · label "On this device" |
| ○ 6 | Spark · In German · label "On this device" |
| Footer | `tab` Actions · `esc` Close |

Layout: ordinary 56 dp rows; every mark on x = 38, every title from x = 72 on one baseline. A found thing's
title is the thing itself, and a word at the right says what it is. The model's rows all carry the spark, which
makes them a family without a heading. No gap and no line between the two kinds. Eight rows at most. On Tab
the line's seat becomes row one's: the highlight appears in place, it never flies in. A title never changes
after its row is drawn.

**Screen 3 · "The answer, where it stands."** Light, one in dark. Two moments, and two small states.

| Part | Thinking | Answered |
| --- | --- | --- |
| Field | `[clip · Am Freitag treffen wir…]`, empty, "What to do with it" | The same |
| ● 1, caption | In English | In English · on this device |
| ● 1, text | "Am Freitag treffen wir uns um 15 Uhr im Büro, bitte bringt die…", dimmer. The slow lap runs on the edge | "On Friday we meet at 3 pm in the office. Please bring the signed forms and your laptop." Two lines |
| ● 1, actions | Ask ⏎, at rest | Copy ⏎ · (Pin) · (Open in Gemini) |
| Other rows | Fading where they are | None |
| Footer | `⌫` Leave · `esc` Close | `tab` Actions · `esc` Close |

This copy is German, so its translation stands first (decision 9); in screen 2 the copy was English and "In
German" stood last. Small states: an instruction (field: `[clip · Notes from Monday…] pull out the tasks as a
list`; the row: caption "With what you copied", the copy's first two lines dimmer, Ask ⏎), and "No answer on
this device this time" with Ask Gemini ⏎.

**Screen 4 · "Into any language."** Light.

| Part | Content |
| --- | --- |
| Under the copy's chip | Field `[clip · Am Freitag treffen wir…]` danish · row one is "In Danish", Ask ⏎ · footer `esc` Close |
| The keyword | Field `[spark · Translate]` · placeholder "A language, then the text" |
| A language, then the text | Field `[spark · Translate]` danish see you on Saturday · caption "In Danish · on this device" · "Vi ses på lørdag" (a draft) · Copy ⏎ · (Pin) · (Open in Gemini) |

The row names its direction in one form everywhere: "In Danish", as "In English". `tr` is also the keyword M4
needs: "en" or "de" is an everyday word in each of its six languages (LANG §3.4).

### How it is built, and its size

The line is built as a third thing that can stand under the empty field, beside the first-run card and the tip
(`OverlayModel`, `Panel`, `Metrics.height`), so it brings no highlight, no footer and no stray Enter. When the
window has focus, after the first frame, Booklight asks the system for the copy's description; the rule
(younger than two minutes, not private, not Booklight's own, a known kind) is plain code with tests. Tab reads
the text and opens the list that `clip` opens, so there is one list. What is in the text comes from the
system's findings, looked up before the list is shown and capped at about 150 ms; nothing is slipped in above
the selection later. `tr` is a keyword and a table of languages on the translation (ENG §7.3). Six shared
pieces are built here once and used again by M2, M3 and M4: one way to an answer for any row, the material
travelling beside the field, ready-made prompts kept by reference, one clipboard helper, the language of a
text, and plain answers (ENG §4). The reflection's test for "quiet" learns about the line. `PRIVACY.md`
changes: today it says the clipboard is read only when you ask.

**About 14 days:** the engineer's 13 (11 to 15: 3.5 for the shared pieces, 7.5 for the features, 2 to finish)
and 1.5 for the typed language and `tr`, which came from M3. The 13 were made before the cuts above and
without the answer where it stands as its own line: not re-sized. The cheapest form that keeps the promise is
about 5 days. Riskiest: the line, done to the standard of the opening.

### To check on a device first

| Check | What | If it goes badly |
| --- | --- | --- |
| 1 | Does the model answer on the HP at all? 2.0 has never run there (`PICKING-UP.md`) | The model's rows there say "Gemini" and hand over. The line, the link and the date still work |
| 4 | The "pasted" message: none on the look, one on Tab | No line at all. Tab on the empty field opens the copy |
| 3 | How soon the system has looked at a copy; what "not looked at" means | The line says "text" more often |
| 5 | A password manager's copy; Booklight's own copy | A password's copy would get a line that says "text". Its content is still not read before Tab |
| 6, 7 | What the system finds, and how fast; what answers a phone number | A phone number gets Copy only |
| 8 | The language of a text; a translation of 2,000 characters | The limits of feature 3 move |
| 18, its second half | A translation into eight languages. It moves to the first session with `tr` | A language that reads badly is not in the table; typing it hands over to Gemini |
| In dark | The line over a white window: can it be read? | The line is designed again for dark: larger type, or a ground of its own |

All of these are **not verified** today, and so is the HP as a whole.

### For you to decide

| Decision | Recommended | If you say the other thing |
| --- | --- | --- |
| **2 · A line with nothing typed.** PLAN §11.3 says "Nothing typed: nothing at all". This is the first line that depends on what you did in another app, and it changes one sentence of the privacy text | Yes, with a switch in the window ("What you copied") | The empty panel stays empty, Tab there opens the copy, and the rest of M1 is the same |
| **9 · The translation first** for a text not in your language, above what was found in the copy. It puts a model's row above local rows; the 2.0 design says a model's rows "never outrank a local match", and `ux-model.md` §2 says the order is fixed | Yes, this one exception: it is what makes "four presses, no typing" true | It stands after the found things; the story is Tab, Down up to three times, Enter, Enter |
| **10 · `clip` shows fewer rows.** Note, Mail, QR code, UPPERCASE, lowercase, Title Case, On one line, Counted, URL-encoded, Search and Ask Gemini are found by typing their name | Yes | Those eleven rows stay in the list, under the new ones |
| **11 · Shared text behaves the new way** in the released app | Yes | Shared text keeps today's way, and the same rows behave in two ways |

Settled by the team; say so if you disagree: two minutes, fixed. The copy wins over a tip, and a first-run
card wins over both. Enter does nothing on the line.

### How we will know it worked

- Copy a line with a link and a date, press the key: the line is there after the opening, and the system
  showed no "pasted" message. After Tab it shows once.
- No line for a password manager's copy, for Booklight's own, or for one older than two minutes.
- From Ctrl + C to a translation on the clipboard: four presses, no typing.
- The panel opens as fast as 2.1, and late frames stay at about 1 %.
- After a week on your HP the switch is still on.

---

## 5. M2 · The picture

**Promise.** Capture a piece of the screen, open Booklight, and copy its text, ask about it or keep it on top.

**Your idea, in your words:** "features which are based on taking a screenshot when booklight is engaged and
then analyzing and asking about it (with Gemini nano)".

**Not as you worded it.** Booklight can take the picture itself only as the device's assistant or with an
accessibility service (NF2 §2.2), and a whole screen is the picture the model read wrongly on the Lenovo (DF).
So M2 is: you frame it, then open Booklight. The nearest thing to your wording without a new role is "capture
from Booklight" through StudioSnap (NF2 §4), placed after M2. The silent capture is decision 5.

**The story.** A build fails in a terminal window. You capture that corner of the screen and open Booklight.
The picture is the chip, and beside it stands the text Booklight read in it; Enter copies those characters.
Or you type "what is the error, and where" and the answer is written above the picture, or you pin the
picture on top while you type.

### What is in it

1. **A picture reaches Booklight, by two doors.** From the clipboard, as M1's line: "Copied just now · a
   picture". Whether the Googlebook's capture tool copies what it captures is **not verified** (NF2 §2.2). Or
   shared to Booklight, which opens the panel with the picture as its chip. Whether the capture's preview
   offers Share is **not verified** too; Android's own preview offers Share and Edit, not a copy (S&C §2.2,
   also not verified). So every example has both forms. *Example:* capture a region, the key, Tab. Or, if a
   capture is not copied: capture, Share, Booklight.
2. **Copy its text.** A text recogniser on the device reads the exact characters, with no model (Google's
   documentation; not run on either device: check 12), and shows them beside the picture before anything is
   copied. It is the first action, so Enter on a picture does the one thing that cannot be invented. "Use
   text" makes that text the chip, with everything M1 built: the link and the date in it, the translation,
   your prompts, an instruction. *Example:* capture, the key, Tab, Enter. Or: capture, Share, Booklight, Enter.
3. **Ask about it.** What you type is the question; the answer is a row above the picture. The promise is for
   a region or one window: a table's cheapest row, a build error with its file and line and the numbers in a
   terminal were read correctly on the Lenovo; a whole screen was not, the model made text up (DF). A picture
   too large for the model is asked through its text, and the caption says so. *Example:* with the picture as
   the chip, type `what is the error, and where`, Enter.
4. **Pin it.** The picture stays on top in the small window 2.0 built and never takes the keys. *Example:*
   with the picture as the chip: Tab, Tab, Enter. To take it down: `pin`, Enter.

### Deliberately out

The photo picker and a keyword `pic` (built only if both doors fail: checks 9 and 10). Taking the picture
silently when the panel opens (decision 5). Starting the capture from Booklight (a small release of
StudioSnap, after M2). Saving the picture. A table as a grid, a spoken description, the QR code in a picture.
Picking a colour. Several pictures at once (the first is taken, and the row says so). A way from a picture to
the Gemini app, until someone has checked that the app takes one. A ready-made prompt `error`: with a picture,
typing the question does it.

### The designs (drawn on the page)

**Screen 5 · "Ask about what you framed."** Plays (the answer being written). Light and dark, dark twice: the
highlight on the picture's row, and the highlight moved off it so the bright picture stands on bare dark glass.

| Part | Nothing typed | A question answered |
| --- | --- | --- |
| Field | `[thumbnail · Picture]` · placeholder "A question about this picture" | `[thumbnail · Picture]` what is the error, and where |
| Row with the answer | None | ● 1 · caption "From the picture · on this device" · "Panel.kt uses a name, Metrics, that it cannot find. It is in line 42, column 17. An import is probably missing." · Copy ⏎ · (Pin) |
| The picture's row | ● 1 · left: the capture of a terminal, whole · right: caption "1600 × 900", then small and dimmer, wrapping, "e: Panel.kt:42:17 Unresolved reference 'Metrics'" · "FAILURE: Build failed with an exception." · … · Copy text ⏎ · (Use text) · (Pin) | ○ 2 · the same, at rest · label "Picture" |
| Footer | `tab` Actions · `esc` Close | The same. While it is being answered: `esc` Close alone |

Layout (DES §4.2, §4.3, and §3's rows above): the picture's row is 136 dp from its first frame. The picture
is never cropped, never padded, never larger than it is. The read text is small ("Reading…" until it is
there), so the answer is the only title-size text; "Picture" is said by the chip and by the label at the
right, and the caption says only the size. The answer's row is the ordinary answer row. The first letter of a
question makes room above the picture in one move; the picture rides down on the list's spring and nothing
fades and comes back. The motion designer has not yet drawn this move: it is the part of this screen to review
first. The picture never shimmers or dims while the model works: the slow lap is the one sign of work (and see
§3 for light theme over a white window).

**Screen 6 · "Every shape hangs from the same corner."** Light. The picture's row five times: 3 : 1
(176 × 59), 2 : 1 (DF's table, 176 × 88), 16 : 10 (166 × 104), 1 : 1 (104 × 104), 9 : 16 (58 × 104). A flat
picture leaves air under it and a narrow one leaves air before the text: the price of two edges that never
move, accepted. Three more states: a whole screen, which is not asked as a picture (that is where the model
made text up) but through its text, with the caption "From the picture's text · on this device" and the read
text beside the answer; "No text in this picture" (Copy text and Use text dim, Pin armed, footer `⌫` Leave ·
`esc` Close); and "This picture could not be read", which has nothing to run and so no highlight: one plain
line, as the quiet line is drawn, with `⌫` Leave in the footer. This screen is the proof of alignment.

**Screen 7 · "Kept on top."** Light; one in dark. A drawn desktop with a text editor in front and the small
window on top: the same capture, whole, edge to edge, no caption, no buttons, at 2 : 1 and at 16 : 10, with
2.0's pins beside it for scale. One pin of a shape the system will not take, centred with bands. Beside it the
panel after `pin`:

| Part | Content |
| --- | --- |
| Field | `[pin · Pin]` · placeholder "A line to keep on top" |
| ● 1 | The picture as a 36 dp mark (radius 11, the ring, cut from its top-left, on the chip's left edge) · Picture · second line "Stays on top of your windows" · Unpin ⏎ · (Copy image) |

If 176 × 104 is too loud on dark glass on the device, the fallbacks are, in order: the picture at most 60 dp
high in a 92 dp row (the text still starts at x = 220, which leaves a wide gap: accepted for a fallback); then
the 36 dp mark, with the picture seen large only in the pin (DES §4.7).

### How it is built, and its size

A handed-over picture is copied into Booklight's cache, decoded off the main thread and deleted when the panel
or the pin closes; the cache is in no backup, because a capture can show other people's mail. Share is one
more intent filter on the panel. The clipboard door is M1's line with one more kind. The model's library
already takes a picture. A size rule decides whether the picture or its text is asked (its limit is check 13).
The model can still misread the recogniser's text (S&C §3.1; ENG §6.2); that is why the read text stands
beside the answer. The recogniser is Google's on-device library, bundled so that it works at first use. The
engineer read the library's manifest and those of the seventeen Google libraries it pulls in: two permissions,
and 2.1 already has both. The AndroidX ones were not followed, and a real build's merged manifest is the last
word (ENG §2.2). It adds about 5 to 6 MB to the Play download and about 13 MB on the device; the GitHub APK
grows from 3.5 MB to about 13 MB if it is limited to the two Googlebook architectures and packed compressed. A
pinned picture needs a manifest entry of its own, or it opens tiny. `PRIVACY.md` gets a paragraph "Pictures".

**About 14 days** (12 to 16); about 10 without the recogniser. Sized with three doors; the picker's day is
out now: not re-sized. The cheapest form is about 12 days: every one of Copy text, Ask and Pin is a third of
the promise. Riskiest: the way in, with two doors not verified.

### To check on a device first

| Check | What | If it goes badly |
| --- | --- | --- |
| 9 | Capture a region, then Paste: does the picture come? The first half is yours (decision 3) | No line for captures; Share is the door |
| 10 | Does the capture's preview offer Share, and is Booklight in it? | With 9 failing as well: build the picker and `pic`, and swap M2 and M3 (§8) |
| 12 | The recogniser in a release build: its speed, small interface text on the HP | Copy text may not deserve to be first on the HP |
| 13 | A dozen pictures with smaller and smaller text, asked of the model | The size rule's limit moves |
| 14 | How large a pin of a picture opens, and how far it can be dragged | "The table you are typing from" becomes "a small region" |
| 1 | The model on the HP | No Ask there. Copy text and Pin are then all of M2 on the HP, and the recogniser is a must |

### For you to decide

| Decision | Recommended | If you say the other thing |
| --- | --- | --- |
| **3 · Your own check:** capture a region on your Googlebook, then try Paste somewhere. Does the picture come? Look at the same time whether the capture's preview has Share | Do it now. It also decides whether screen 1 has a picture state | Not copied: Share is the door. Neither: the picker is built and M3 goes before M2 |
| **4 · The text recogniser.** One more Google library, with the usage reporting you accepted for the model's library. No new permission | Yes. Without it nothing M1 built works on a picture, and a whole screen gets only "too large" | No Copy text, about 10 days. The research suggested regions first and deciding later (NF2 §2.2) |
| **5 · Silent capture as the device's assistant.** It replaces Gemini on its key and keeps a service of Booklight's running always | Not in these four, and not ruled out. Look again after M2 has been used for a month | Not sized. It breaks "nothing in the background". It is also the only road to Chrome's "New tab" (DF: read in the source, not tried) |

### How we will know it worked

- DF's three test pictures (a table, a build error, a piece of a terminal) are answered correctly through the
  real flow, not a debug hook.
- A whole screen is not asked as a picture: it is answered from its text, with the read text beside the
  answer, or the row says plainly that it cannot read it.
- Copy text gives the exact characters of those three pictures, in three presses after the capture.
- A pinned picture stays on top and never takes the keyboard.
- The manifest has no new permission, and the release build does all of the above.

---

## 6. M3 · The selection

**Promise.** Select text where you are writing, right-click, and Booklight rewrites it in place.

**Not one of your three ideas.** The team proposes it as the step after the copy: the same rows, opened from
where you write, with the answer put back there. It is the research's ideas 5, 4 and 6 (NF2 §1).

**The story.** You are writing a mail and the sentence is not right: a typo, the wrong tone, no umlauts. You
select it, right-click and pick "Fix spelling". The panel opens with the answer already being written. Enter
puts it where the sentence stood: two clicks and one key.

"Where you are writing" means: in apps that show other apps' entries in their selection menu. Which do on a
Googlebook is **not verified** (check 15). Replace itself has never been tried on a device either (check 16).
If it lands nowhere, M3 has no promise. Both checks are in the first device session, before M1.

### What is in it

1. **Ready-made prompts in the selection menu, by name.** "Fix spelling" and "Translate" stand in the menu
   from the start; Shorter, Formal, Friendly, Put the letters back and Explain can be switched on in the
   window. The model is asked the moment you pick. Replace is the first action in a field you can type in,
   and it is built to work for several paragraphs with their line breaks; that is **not verified**. On a page
   you can only read there is no Replace and Copy is first. Nothing is ever put back without your Enter.
   *Example:* select "i think we shoud meet on friday becuase the room is free", right-click, Fix spelling,
   Enter.
2. **Put the letters back.** A sentence typed without its umlauts or accents gets them back, in place.
   Booklight checks that only letters changed and shows "Could not do it without changing your words"
   otherwise, so it cannot rewrite you. Typed, it lives under `abc`; a language may go first: `abc danish jeg
   kommer pa lordag`. *Example:* select "Gruesse aus Koeln, ich komme spaeter" in your mail, right-click,
   Booklight, "Put the letters back", Enter (UX §5.4).
3. **Two more ready-made prompts:** `formal` and `friendly` (German `förmlich`, `freundlich`). *Example:*
   select, right-click, Booklight, type `formal`, Enter, Enter.

The menu's "Translate" is M1's translation behind a new door: it knows its direction, and a typed language
changes it.

### Deliberately out

A prompt you wrote under its own name in the menu (not possible: ENG §3). "In German" as a menu name. Pasting
into a field that does not take the answer back (power pack). Snippets that expand as you type (ruled out: NB,
F11). `reply`, `commit`, `define`, `error` (you can add them under Yours). `abc` by keystrokes. A one-word
swap without the model.

### The designs (drawn on the page)

**Screen 8 · "Right-click, Fix spelling."** Plays. Light; the panel also in dark. A drawn mail window with one
sentence selected and the system's menu: Cut · Copy · Paste · Select all · Fix spelling · Translate ·
Booklight. Marked "the system draws this; order and icons not verified". Then the panel over it:

| Part | Content |
| --- | --- |
| Field | `[text · i think we shoud meet…]`, empty, "What to do with it" |
| ● 1 | Caption "Fix spelling · on this device" · "I think we should meet on Friday because the room is free." · Replace ⏎ · (Copy) · (Pin) · (Open in Gemini) |
| Footer | While it is being answered: `⌫` Leave · `esc` Close. Answered: `tab` Actions · `esc` Close. After Enter: "Replaced" with its check |

Layout and motion (DES §5): the strip is about 215 dp, the text column x = 72 to 469. The panel opens at 68 dp
as always, with the chip in the field from the first frame. The answer's row comes only when the glass is
85 % open, even if the answer is already there; the slow lap starts then, not before. The row shows your
sentence first, dimmer, then the answer written over it.

**Screen 9 · "Put the letters back."** Light.

| Part | Content |
| --- | --- |
| From a selection | Field `[text · Gruesse aus Koeln, ich…]` · caption "Put the letters back · on this device" · "Grüße aus Köln, ich komme später" · Replace ⏎ · (Copy) · (Pin) |
| Typed | Field `[letters · Letters]` Gruesse aus Koeln, ich komme spaeter · caption "With its letters · on this device" · the same answer · Copy ⏎ · (Pin) |
| Refused | Caption "Could not do it without changing your words" · your text stays · Ask Gemini ⏎, as every row has when there is no answer here (ENG §7.2) |
| One word | `abc Koeln` gives "Köln" in the same row |

The answer is not shown while it arrives: it is checked first, then written in. A wrong one is never shown,
not for one frame.

### How it is built, and its size

Each menu entry is declared in the manifest, pointing at the panel, with its own fixed name, and off. The
window switches one on or off; no permission is involved. The panel sees which entry was picked, takes the
text, and asks that prompt at once; the model is warmed as the panel is created. Replace and "this text cannot
be edited" exist today; Replace moves to the front, and several paragraphs become replaceable through the
shared piece that carries the material beside the field. The letters check walks both texts: a letter with a
mark must stand where one of its plain writings stood, and every other character must be the same. It is the
one model feature a unit test can hold, with about sixty pairs.

**About 8 days:** the engineer's 10 (8 to 12), less 1.5 for the typed language and `tr`, which went to M1,
and less half a day for `error`. The cheapest form is about 5 days: two entries, Replace first, several
paragraphs, and the letters for German. Riskiest: the menu and Replace in the apps you write in. The engineer
found no note that Replace was ever tried on a device.

### To check on a device first

| Check | What | If it goes badly |
| --- | --- | --- |
| 16 | Replace: does the answer land in the selection, in Chrome, Gmail, Docs, Keep and the text editor; with several paragraphs | Replace also copies, or Copy is first in that app. If it lands nowhere, M3 has no promise |
| 15 | Where the entries show; how many of one app before the menu folds them; their order; by right-click and by touch | One entry, "Booklight", with the two prompts first in its rows: one key more for the same promise |
| 17 | How soon the menus follow a switch | An entry may show late after its switch, and the window has to say so |
| 18, its first half | Fifty sentences without their letters in German, Danish and French | A language that passes fewer than about four in five is not offered |

Checks 15 and 16 belong to the first session, before M1: they decide whether this milestone holds at all.
Also **not verified**: that the other app's Ctrl + Z undoes a Replace in one step.

### For you to decide

| Decision | Recommended | If you say the other thing |
| --- | --- | --- |
| **6 · How many entries stand in the menu from the start** | Two: Fix spelling and Translate. The rest are off until you switch them on | One ("Booklight" only, as today, and every prompt is one key more) or all seven |

Settled by the team: "put the letters back" lives under `abc`. `formal` and `friendly` come ready-made;
`reply`, `commit`, `define` and `error` do not.

### How we will know it worked

- In Chrome, in a mail being written and in the text editor: select, right-click, "Fix spelling" is there by
  name, and Enter replaces the selection. Three paragraphs keep their line breaks.
- On a page that cannot be edited the answer is shown and copied, and no Replace is offered.
- Fifty test sentences in German, Danish and French: each comes back with the right letters or not at all.
  Never with a changed word.
- The menu holds exactly the entries that are switched on.

---

## 7. M4 · Your language

**Promise.** Booklight speaks six more languages: its screens, its keywords and its dates.

**Your idea, in your words:** "language support for e.g. more languages including French, Spanish, Italian,
Japanese, Korean, mandarin, danish, swedish, Norwegian, Polish". **Six of your ten.** Polish, Japanese, Korean
and Mandarin are not in these four (decision 7).

**The story.** Your Googlebook is set to French. Booklight's screens are in French, `ven 15h dentiste` makes
an event, and the keywords are words you would type. The English keywords still work. Where the model on the
device writes your language well it answers in the row; where it does not, the row hands over to Gemini and
says so before Enter.

### What is in it

It is one thing, six times: French, Spanish and Italian first, then Danish, Swedish and Norwegian. These are
its parts.

1. **The six languages as deep as German is today:** the screens and the typed words (keywords, verbs, dates
   and times, the words for settings pages and shortcuts). *Example:* `ven 15h dentiste`; in Danish `fre kl.
   15 tandlæge`.
2. **Dates and numbers the way the language writes them:** "dans 20 min", "kl. 15.30", 1 234,5.
3. **The prompts where the model passed a test on the device,** and a hand-over to the Gemini app where it
   did not. The row looks like any other row; its right end says "Gemini" where it would say "On this device".
   *Example:* in a Danish Booklight where the model failed: copy "jeg kommer pa lordag", the key, Tab. "Ret
   stavning" stands there with "Gemini" at its right; Enter opens the Gemini app with the prompt and the
   text, not sent.
4. **One choice in the window, "Translate into":** your other language, used when a text is already in yours.
   Until M4 it is fixed (German in an English Booklight, English otherwise), and that guess is wrong for most
   people in these six. *Example:* in a French Booklight, set "Translate into" to Spanish; a copied French
   sentence then gets the row "En espagnol", where it said "En anglais".
5. **The store page** in each language, written to fit Play's limits, not translated.

### Deliberately out

Polish (three plural forms, day names that change with grammar, `w` is a word: its own release next).
Japanese, Korean and Chinese (a second block: about nine days of one-time work and about eighteen for the
languages, and a different kind of work: LANG §2, §7, §11). Store pictures per language. Smaller type or a
second line to make a long word fit: the word is chosen to fit, and a test fails the build when it does not.

### The designs (drawn on the page)

**Screen 10 · "The same panel, in your language."** Light. The line and the list of screens 1 and 2 in
English, German and French, on one left edge, and the same event (Friday, three to four) typed the way each
language writes it. The French and Danish are drafts for the native readers.

| | English | German | French (draft) |
| --- | --- | --- | --- |
| The line | Copied 20 s ago · a link and a date | Kopiert vor 20 s · ein Link und ein Datum | Copié il y a 20 s · un lien et une date |
| The model's rows | Fix spelling · Shorter · In German | Korrigieren · Kürzer · Auf Englisch | Corriger · Plus court · En anglais |
| An event, typed | As today | As today | ven 15h dentiste (in Danish: fre kl. 15 tandlæge) |

**Screen 11 · "Where long words go."** Light. One sheet: each component at its limit, English above, the
longest language under it.

| Component | Limit | Drawn with |
| --- | --- | --- |
| A place's name in the armed slot | 12 characters | The three placements today's polish leaves on the row |
| Any other action's name | 16 characters | "Copier sans suivi" is 17: it must be written shorter |
| A tip | 90 characters; its two answers 18 each | `chrome gauche` "ouvre Chrome à gauche s'il n'est pas encore ouvert. Aussi : plein écran", with "Essayer" and "Désactiver les astuces" |
| A chip's name | 24 characters | The longest prompt name |
| The quiet line | 80 characters; things are dropped from the end, never cut mid-word | "Copié il y a 20 s · un lien, une date et plus" |
| An answer's caption | First part 32 characters; "on this device" goes when both do not fit | Beside a picture |
| A footer hint | 28 characters | The footer stays flush right at 700; its caps move left as the words grow, and "échap" is a wider cap than "esc" |
| A menu entry | Short forms: "Corriger", not "Corriger l'orthographe" | |
| A prompt the model failed in | The model's three rows of a list, English and Danish: "Ret stavning" with Spørg Gemini ⏎, "Kortere" and "På engelsk" with the label "Gemini" where English says "On this device" | They must look like normal rows, not like errors |

No new element and no new motion. The face has every letter the six need by Google Fonts' list, so no row
changes height (LANG §4); the device's own file is **not verified** (LANG §14).

The picture book for the native readers is made from captures of the real app, not from drawings: a drawing
would hide the clipping the reader is there to find. It has about fifty screens: the research's forty and
about ten for M1 to M3.

### How it is built, and its size

Before M4 the groundwork is done (§9): the typed words live in one file per language, numbers and dates follow
the locale, and a test fails the build when a language's keyword list is missing or collides. M4 is then the
languages themselves: a word set and its test, about 460 strings, 50 keyword lists, the list of everything,
the tips, the settings and shortcut words, per language. What M1 to M3 added is small here (about sixty
strings, three keywords, two prompts), because ready-made prompts are kept by reference since M1. The model
is tested per language on the Lenovo with five prompts on a fixed text, and a list says where the device may
answer. `tr` is the translation's keyword in all six: "en" or "de" is an everyday word in each (LANG §3.4).

**About 17 days** (15 to 19) and the wait for six readers: 11.5 for the six languages, 2 for the model tests,
2 for the picture books and what the readers find, 1 for store pages and notes, half a day for M1 to M3's
words. The first step (French, Spanish, Italian) is about 8 days. The day figures are estimates from counts,
"not from having done one" (LANG §14). Riskiest: keywords nobody would type, and the model in the Nordic
languages.

### To check on a device first

| Check | What | If it goes badly |
| --- | --- | --- |
| 19 | German on a device on the new structure. German was never checked on a device before | The groundwork is not done |
| 20 | Per language: five prompts on one text; the Settings app's page names | That language's prompts hand over to Gemini |
| None yet | Ctrl + 1…9 on a French keyboard, where digits need Shift | For M4's device pass |
| None yet | Whether the system tells Danish from Norwegian | The worst case is a translation offered into the language the text is in. The row names its direction |

**Not verified** besides: the model in any of the six (only German was tried on the Lenovo; Danish, Swedish
and Norwegian are on none of Google's lists: LANG §5, §14); Play's language codes; where Googlebooks are sold,
which should set the order.

### For you to decide

| Decision | Recommended | If you say the other thing |
| --- | --- | --- |
| **7 · Which languages, and how.** You named ten | These six, in two steps. Polish next as its own release. Japanese, Korean and Chinese as a later block | Polish in M4 adds about 3 days (LANG §2). Japanese, Korean and Chinese: about 9 days of one-time work and about 18 for the languages |
| **8 · German words in an English Booklight.** After the groundwork the date reader knows the app's language and English, so "morgen 9 Uhr" would stop working in an English Booklight. Your own examples are German | Keep German for you: one setting, about a day (ENG §8) | "morgen 9 Uhr" stops in an English Booklight. It saves a day |
| **12 · Who reads each language.** A machine draft, then one native reader per language (LANG §12.9) | A reader you know where you have one; paid, about USD 260 a language, where you do not | Unread drafts ship. A wrong keyword here is a broken feature, not a typo (LANG §13) |
| **13 · Suggestions in the user's language.** The language goes to the search engine with the text, and the privacy text gains a clause (LANG §12.11) | Yes | Suggestions stay as they are today, in every language |
| **14 · Your rule "every string in English and German".** After M4 the two stay the rule for building; the other six may trail by one release (LANG §12.13) | Yes | Every release waits for all eight languages and their readers |

Standing at the research's recommendation: the model is offered in a language only where the device test
reads well to that language's reader. English pictures under translated store text.

### How we will know it worked

- In every language each example of the list of everything runs.
- One native reader per language has seen the picture book and said what he would type for twenty things.
- Nothing is cut off: every limit of screen 11 holds, and the store text is within 4,000 characters.
- There is a table of the five prompts per language from the Lenovo; where a language failed, its rows hand
  over to Gemini.
- English and German behave as before.

---

## 8. The order, and why

1. **Today's polish**, released on its own. M1's line and list are drawn on its softer highlight and shorter
   strip, not on 2.1's.
2. **One device session,** half a day, both devices: checks 1 to 11, 15, 16 and the translation half of 18.
   Five need only 2.1 as released; the rest need a throwaway build on a branch. It settles M1, the way a
   picture gets in, and whether M3's promise holds, so the order of the four is known before the first is
   built. It needs your HP awake and attached.
3. **M1 · The copy.** It is useful on the first day without learning a keyword, most of it needs no model,
   and it builds what M2 and M3 stand on: a line for something you did in another app, the chip and its rows,
   an answer where it stands, the translation and its direction.
4. **M2 · The picture,** with the middle of the groundwork built beside it; they touch different code. It is
   your own first idea. And if the HP has no model, the picture is the milestone that still works there: Copy
   text and Pin need none.
5. **The groundwork, as a quiet release.** German on the new structure, on a device, before anything is built
   on it.
6. **M3 · The selection.** Its new keywords are then born in the per-language files.
7. **M4 · Your language.** Last, because afterwards every new keyword, tip and release note is written once
   per language and read by someone who speaks it. Before M4, everything M1 to M3 added is translated once.

**Beside these seven: the settings window.** It ships alone as soon as you have approved it and it is built:
any time, and with M2 at the latest. Its page structure must be final before M3. "Stop what is ringing" (a
repair: Booklight starts timers and cannot stop one) has no owner yet; the proposal is that it rides in that
release.

**One swap rule.** If a capture is not copied and its preview has no Share (checks 9 and 10 both fail), and
the menu and Replace work (checks 15 and 16 pass): M3 goes before M2. Nothing in M3 needs M2.

| From | It hands on | To |
| --- | --- | --- |
| Today's polish | The quieter highlight; the shorter strip | M1's line and list |
| M1 | The line; the chip and its rows; an answer where it stands; the translation, its direction and `tr`; the six shared pieces | M2 (a picture in the same line, its text in the same list), M3 (the same rows behind a new door) |
| M2 | Pictures handed to Booklight; the recogniser | Later ideas (a colour from the screen, capture from Booklight) |
| The groundwork | Words per language; the keyword test | M3 (its new keywords), M4 |
| M3 | The first test of the model's letters in other languages; the final list of prompts and keywords | M4 (translated once) |

---

## 9. The tracks beside the four

**The settings window.** It is being redesigned by a separate designer as a desktop-ready Material 3
Expressive window with its navigation at the left edge, and you approve it separately. It is a visible flaw in
the released app, not a feature waiting for a story, so it does not wait for a milestone (its place in the
order is in §8). It changes the window and the milestones change the panel, so neither waits for the other.
Every page must be final before M4's picture book is made. What the milestones add to it is small, and each is
"a title, one line, a switch", a row every version of the window has:

| From | It adds to the window |
| --- | --- |
| M1 | One switch: "What you copied" |
| M3 | One switch per ready-made prompt: "In the selection menu" |
| M4 | One choice: "Translate into". And the setting for German words, if you say yes to decision 8 |

**Languages.** Two parts.

- *The groundwork: about 12 days* (10 to 14), and nothing changes for the user. The typed words move out of
  the code into one file per language, numbers and dates follow the locale, ø æ ł ß are matched, a test checks
  every keyword list (LANG §7, items 1 to 8). It is built in three parts so that it never collides with a
  milestone (ENG §8): half a day between M1 and M2 to move the word lists; the date reader and the number
  formats beside M2; the parts that touch the panel's strip and field after M2. It cannot be cut. With it
  comes the rule of §2, line 19, which has no size yet: a date in a thing you brought is read in the text's
  own language.
- *The languages themselves* are M4. After it: Polish, then the block of Japanese, Korean and Chinese.

**Today's polish** is not planned here. This plan takes it as given: medium opening speed, the later white
reflection, three placements by default, an app's own shortcuts as actions, the softer highlight in dark. None
of the new elements uses the highlight's colour. Screens 2, 5 and 8 show it in dark under the new rows.

---

## 10. Not in these four, and why

| Idea | Why not now |
| --- | --- |
| **Say it plainly, a guess when nothing matches, settings in plain words, emoji by description** (NF2 ideas 7, 9, 18) | One real story and the best candidate for a fifth milestone. It is the largest model work, and it takes one to two seconds for something that looks instant (NF2 §2.3) |
| **Pick a colour from the screen** (idea 8) | Close to M2's story but another mechanism, with one device check open. The first thing to add after M2 |
| **Capture from Booklight** (`shot area` through StudioSnap) | The nearest thing to "a screenshot when Booklight is engaged" without a new role. A small release of StudioSnap, in its repo, after M2 |
| **Text tools** `b64`, `jwt`, `sha256`, `epoch`, `json` (idea 10) | Useful and small, but they share nothing with the four |
| **Stop what is ringing** (idea 15) | A repair, not a feature, and nobody owns it yet. Proposed: in the settings window's release (§8) |
| **Tasks out of a text** (idea 17) | Typing "pull out the tasks" under a copy does the reading. Adding them to Todo.md is a notes story |
| **`error`, `reply`, `commit`, `define`; `abc` by keystrokes** | You can add the prompts under Yours. The lists stay short. `error`'s own example was a copied line, and on a picture typing the question does it |
| **Units, money, time zones** (idea 13) | It needs Summa's engine inside Booklight. A milestone of its own |
| **A key per command** (idea 11) | One device check is open, and it is about opening Booklight, not about the four stories |
| **What other apps can do with a thing; packs; your people** (ideas 14, 21, 19) | Each is the start of another story |
| **Polish; Japanese, Korean, Chinese** | After M4 (decision 7) |
| **Silent capture; the power pack; the Gmail relay** | Yours to decide (decision 5); deferred; "not yet" |
| **The other unbuilt ideas** | They stay in `not-built-2.0.md` with their ratings |

What each milestone cut is in its own "Deliberately out".

---

## 11. Your decisions, in one list

Each can be answered in a word. The recommended answer is what happens if you say only "go".

| # | Milestone | Decision | Recommended | If you say the other thing |
| --- | --- | --- | --- | --- |
| 1 | All | These four, in this order, starting with the half-day device session on both devices | Yes | Name the order. The device session still comes first, and it needs your HP awake and attached for half a day |
| 2 | M1 | A line under the empty field for something you just copied, with a switch to turn it off | Yes | The empty panel stays empty, and Tab there opens the copy |
| 3 | M2 | A task more than a decision: capture a region on your Googlebook, then Paste somewhere. Does the picture come? Look at the same time whether the capture's preview has Share | Tell us yes or no | Yes: capture, the key, Tab. No: Share is the door. Neither: the picker is built and M3 goes before M2. If you say only "go", check 9 answers it |
| 4 | M2 | The text recogniser: one more Google library, no new permission, about 5 to 6 MB more on Play | Yes | No Copy text, nothing of M1 works on a picture, a whole screen gets only "too large", and M2 is about 10 days. The research suggested shipping regions first and deciding later (NF2 §2.2) |
| 5 | M2 | Silent capture as the device's assistant | No, not in these four. Look again after M2 has been used for a month | Not sized. It replaces Gemini on its key and keeps a service of Booklight's running always, which breaks "nothing in the background". It is also the only road to Chrome's "New tab", which you asked for today (DF: read in the source, not tried) |
| 6 | M3 | Entries in the selection menu from the start | Two, Fix spelling and Translate | One ("Booklight" only, as today) or all seven |
| 7 | M4 | Six languages in two steps: French, Spanish, Italian, then Danish, Swedish, Norwegian. Polish next. Japanese, Korean, Chinese later | Yes | Polish in M4 adds about 3 days (LANG §2). Japanese, Korean and Chinese are about 9 days of one-time work and about 18 for the languages |
| 8 | M4 | Keep German words ("morgen 9 Uhr") working in an English Booklight | Yes, one setting, about a day | "morgen 9 Uhr" stops working in an English Booklight. It saves a day |

**Six smaller ones.** The team settled these, but each changes something that is yours: a rule of yours, the
released app, or the privacy text.

| # | Milestone | Decision | Recommended | If you say the other thing |
| --- | --- | --- | --- | --- |
| 9 | M1 | The translation stands first for a text not in your language, above what was found in the copy | Yes, as the one exception | It bends two rules: the 2.0 design's "a model's rows never outrank a local match", and "the order is fixed". If no: it stands after the found things, and the story is Tab, Down up to three times, Enter, Enter |
| 10 | M1 | `clip` shows fewer rows. Note, Mail, QR code, UPPERCASE, lowercase, Title Case, On one line, Counted, URL-encoded, Search and Ask Gemini are found by typing their name | Yes | Those eleven rows stay in the list, under the new ones |
| 11 | M1 | Shared text behaves the new way in the released app: the chip stays the text and the row is answered in its place | Yes | Shared text keeps today's way (the chip becomes the prompt), and the same rows behave in two ways |
| 12 | M4 | Who reads each language: a machine draft, then one native reader per language | A reader you know where you have one; paid, about USD 260 a language, where you do not | Unread drafts ship. A wrong keyword here is a broken feature, not a typo (LANG §13) |
| 13 | M4 | Suggestions are asked for in the user's language. The language goes to the search engine with the text, and the privacy text gains a clause | Yes | Suggestions stay as they are today, in every language |
| 14 | M4 | Your rule "every string in English and German": the two stay the rule for building; the other six may trail by one release | Yes | Every release waits for all eight languages and their readers |

Everything else in this plan was settled by the team and is listed under its milestone as "settled". Say so if
you disagree with any of it.

**After "go"** comes the device session and its results, and the designs made final on them. Still to be drawn
then: the one new move, the picture riding down when a question is typed (screen 5), by the motion designer.

**Four things only you can judge, on a device,** before the designs are final. The line at 320 ms and at
200 ms after the opening: compare them first in the playing panel of screen 1 on the page, then in the
throwaway build of the device session, which carries both. The picture at 176 × 104 on dark glass. The line
and a picture's read text in dark over a white window. And whether, in light theme over a white window, a row
that is being answered says clearly enough that it is working, since the light cannot be seen there.
