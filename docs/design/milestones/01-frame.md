# Booklight after 2.1: four milestones (first frame)

*Product manager's frame, 1 October 2026. Planning only: nothing was built, no device was touched, nothing in
the repo was changed. It rests on today's research in `docs/research/`; each claim names the file and section it
comes from. "Not verified" is said wherever the research says it.*

Short names for the sources: **NF2** = `next-features-2.md`, **S&C** = `screen-and-clipboard.md`,
**DF** = `device-findings.md` (its last two sections), **LANG** = `languages.md`, **NB** = `not-built-2.0.md`,
**PLAN** = `docs/PLAN.md`.

---

## 1. The four, at a glance

| # | Name | The promise, in one sentence | Size (rough) | New permission |
| --- | --- | --- | --- | --- |
| M1 | **The copy** | Copy something, open Booklight, and it already knows what you can do with it. | About a week | None |
| M2 | **The picture** | Capture a piece of the screen, open Booklight, and ask about it, copy its text or keep it on top. | One to two weeks | None. One library, if you say yes (D6) |
| M3 | **The selection** | Select text in any app, right-click, and Booklight rewrites it where it stands. | About a week | None |
| M4 | **Your language** | Booklight speaks six more languages: its screens, its keywords and its dates. | About two and a half weeks, after ten days of groundwork done earlier | None |

Sizes are the research's effort letters added up (NF2 §1; LANG §7 and §11). They are estimates, not
measurements. The engineer should correct them.

**How they were cut.** The research recommended one next release of eight ideas, "Booklight works on what you
copied or selected", then the picture (NF2 §5.5). Eight is a bag. I split it by *where the thing you are
working on is*: on the clipboard (M1), in a picture (M2), in a selection (M3). Each of the three has one way in,
one row and one list of options, and each later one reuses what the one before built. Languages are your third
idea and close the plan (§6 says why last).

**Three places where this frame differs from the research, so you can overrule them:**
1. The eight are two milestones (M1 and M3), and two of the eight are left out (§8).
2. The picture comes second, before the selection. It is your own idea, and M3 and M4 belong next to each
   other (§6). M2 and M3 do not need each other: they can swap if the picture's device checks go badly.
3. I recommend the text recogniser in M2. The research suggested deciding after using regions (NF2 §2.2). My
   reason is in D6.

---

## 2. What runs beside the four

| Track | What it is | Where it goes | Why there |
| --- | --- | --- | --- |
| **Today's polish** | Medium opening speed, the later white reflection, three placements by default, an app's own shortcuts as actions, a softer highlight in dark | Its own small release, **before M1**. Not one of the four | It changes the row's action strip and the highlight. M1's new row and list are designed on top of those, not on 2.1's |
| **The window redesign** | The Booklight window as a desktop-ready Material 3 Expressive window, navigation at the left edge. Designed by a separate designer, approved by you separately | Its own release **as soon as it is approved and built**; at the latest together with M2 | It is a visible flaw in the released app, not a feature waiting for a story. It changes the window, the milestones change the panel, so they do not wait for each other. M1 adds one switch to the window and M3 a short list, and M4 shows every page of it to native readers: the window must be final before M3 and M4 |
| **Language groundwork** | Ten days of one-time work: the typed words move out of the code into one file per language, numbers and dates follow the locale, ø æ ł ß are matched, a test checks every keyword list (LANG §7, items 1 to 8). Nothing changes for the user | Built **beside M2**, shipped **inside M3** | M2 hardly touches what it rewrites (the parsers and the word lists); the engineer should confirm. M3 needs one piece of it (the letter table). After it, M3's new keywords are born in the per-language files, and M4 is only the languages |

One check stands before all four: **the HP has not run 2.0 at all, and the model has never been tried on it**
(`docs/PICKING-UP.md`, "Not done"; LANG §5.6). M1, M2 and M3 lean on the model. If the HP has no model, every
model row there hands over to the Gemini app, and you would be judging these milestones on the Lenovo only.

Rules that hold for every milestone (not repeated below): no new permission; a model's row never runs anything;
nothing in the background; every string in English and German; every "not verified" gets its device check before
the feature is promised; the release build is installed and asked a prompt before every tag.

---

## 3. M1 · The copy

**Promise.** Copy something, open Booklight, and it already knows what you can do with it.

**Who, and the moment.** Anyone who copies a thing in order to use it somewhere else: a date from a mail, a
paragraph in another language, a link from a chat. The moment is right after Ctrl + C, when the next step today
is "open another app, paste, do the thing".

**What is in it** (four features, one row, one list)

1. **The copy, as a row.** If you copied something in the last two minutes, the empty panel shows one quiet row
   after it has opened: "Copied 20 s ago: a link and a date". Booklight reads only the clip's description for
   this, not the text, so the system shows no "pasted" message. A clip marked sensitive (a password manager's)
   never gets a row; nor does Booklight's own copy (NF2 §2.1; tried on the Lenovo, DF).
2. **Tab: what is in it.** Tab opens the copy. Now the text is read. First come the things that need no model
   and are there at once: Open the link, Add the event, Call, Open in Maps, Track the flight. A link also gets
   Copy clean (without its tracking tail) and Copy as Markdown (NF2 §2.1 and §3, idea 10).
3. **Then: what the model can do with it.** Translate, Fix, Shorter, Explain, and Summarise for a long text. These
   prompts exist since 2.0; here they stand in the list by name and run on Enter. Translate knows the
   direction: a text in your language goes out of it, any other comes into it (the system named German at
   0.998 on the Lenovo, DF).
4. **`do …`: tell it what to do with the copy.** `do make this a bullet list`, `do answer politely that Friday
   works`. What you type is the instruction, what you copied is the material. The answer is shown, copied or
   pinned, like any prompt's (NF2 §3, idea 3).

**Deliberately out.** A history of copies (it needs the power pack: NB, F9). Pasting in place (power pack:
NB, F10). A picture on the clipboard (that is M2). The text tools `b64`, `jwt`, `sha256` (no story here: §8).
Tasks out of a text (`do` covers the reading half: §8). A row for an older copy.

**Why first.** It is the smallest and safest of the four. Everything except the row and the list exists
already, most of the options need no model, there is no new library and no new permission (NF2 §2.1, "Why
first"). It is useful on the first day without learning a keyword. And it builds the three things M2 and M3
reuse: a row that appears for something you did in another app, a list of "what is in it" and "what the model
can do", and the direction of a translation.

**Needs decided**

| # | Decision | Recommended |
| --- | --- | --- |
| D1 | **A row with nothing typed.** PLAN §11.3 says "Nothing typed: nothing at all". Tips already bend it. This would be the first row that depends on what you did in another app (NF2 §5.1) | Yes, with a switch in the window to turn it off. If no: the empty panel stays empty and Tab on the empty field opens the copy; the rest of M1 is unchanged |
| D2 | **How fresh.** The system keeps a clip for an hour (S&C §4.1) | Two minutes, as the research proposes. A fixed number, no setting |
| D3 | **The copy and the tip.** Both want the space under the empty field | The copy wins: no tip while a fresh copy is shown |
| D4 | **Enter on the empty panel** when the copy's row is there | Enter does nothing until you press Tab or Down. A row you did not ask for must not run on a stray Enter |

**Not verified, to check on a device before the design is final:** which ready-made actions the Googlebook's
text classifier returns (S&C §4.2: "has not been probed"); addresses (the one German address in the probe was
not found: DF); only texts up to 400 characters are classified, and the description tells neither length nor
language (S&C §4.1), so for a long text the row can only say "text"; the HP.

**How we would know it worked.** Booklight collects nothing, so the proof is checks and your own use.
- Copy a line with a link and a date in another app, press the key: the row is there after the opening, and
  the system showed no "pasted" message. After Tab the message appears once, which is expected.
- No row for a password manager's copy, for Booklight's own copy, or for a copy older than two minutes.
- From Ctrl + C to a translation on the clipboard: at most five key presses and no typing.
- The panel opens as fast as in 2.1, and late frames stay at about 1 % (the 2.0 measure, `PICKING-UP.md`).
- After a week on your HP the switch is still on.

---

## 4. M2 · The picture

**Promise.** Capture a piece of the screen, open Booklight, and ask about it, copy its text or keep it on top.

**Who, and the moment.** Someone looking at something that cannot be selected: an error in a terminal window, a
table in a PDF, a design, a frame of a video. The moment is "what does this say", "what does this mean", or
"I need this next to me while I type".

**What is in it** (four features, one hand-over, the row M1 built)

1. **A picture reaches Booklight.** Lightest first: from the clipboard, if the Googlebook's capture tool copies
   what it captures (**not verified**: NF2 §2.2, S&C §2.3); shared to Booklight from the capture's preview;
   picked in the system's photo picker, where the newest capture is the first tile. It appears as the same
   quiet row as a copied text: "Copied 10 s ago: a picture".
2. **Ask about it.** Type the question; the answer is written into the row, as a prompt's is. The promise is
   for a region or one window. Tried on the Lenovo: the cheapest row of a table, a build error with its file and
   line, the numbers in a terminal were all read correctly; a whole screen was not, the model made text up (DF).
3. **Copy its text.** The exact characters, read by a text recogniser on the device, with no model (D6). The
   text then gets everything M1 built: the link and the date in it, Translate, Fix, `do …`. A picture too
   large for the model to read is asked through its text, and the row says so.
4. **Pin it.** The picture stays on top in the small window 2.0 built: the table you are typing from, the
   design you are matching (NF2 §3, idea 12).

**Deliberately out.** Taking the picture silently when the panel opens (D7). Starting the capture from
Booklight (it needs a small release of StudioSnap: §8). A table as a grid, a spoken description (S&C §5,
candidates 3 and 4). Picking a colour from the screen (§8). Several pictures at once.

**Why second.** It stands on M1: the picture arrives in M1's row, opens M1's list, and its text is handled as a
copy is. It is your own first idea. And the groundwork for languages can be built beside it, because M2 hardly
touches the parsers and word lists that the groundwork rewrites.

**Needs decided**

| # | Decision | Recommended |
| --- | --- | --- |
| D5 | **A check only you can make** (NF2 §5.3): capture a region on your Googlebook, then try Paste somewhere. Does the picture come? | Do it before M2 is designed. Yes means "capture, open, ask". No means "capture, share, ask": about three steps |
| D6 | **The text recogniser** (NF2 §5.2): Google's on-device library, about 4 MB, and one more Google library with the usage reporting you accepted for the model's library | **Yes, in M2.** "Copy its text" is the one part that cannot invent anything, Google's own advice is to read the text first (S&C §1.2), and without it nothing M1 built works on a picture. Condition: the engineer opens its manifest first (not opened in the research, S&C §3.1); a new permission means no. If no: M2 is features 1, 2 and 4, for regions and windows only |
| D7 | **Silent capture as the device's assistant** (NF2 §5.4). It replaces Gemini on its key and keeps a service of Booklight's running always | Not in these four, and not ruled out. Look again after M2 has been used for a month |

**Not verified:** the capture tool and the clipboard (D5); whether the capture's preview offers Share and
Booklight appears there (S&C §2.3); a picture on the clipboard was not tried (DF); the recogniser on a
Googlebook, where small interface text is near its 16-pixel floor (S&C §3.1); the HP.

**How we would know it worked.**
- The three test pictures of DF (a table, a build error, a piece of a terminal) are answered correctly through
  the real flow, not a debug hook.
- A whole screen never gets an invented answer: it is answered from its text, or the row says plainly that it
  cannot read it.
- "Copy its text" gives the exact characters for those three pictures.
- A pinned picture stays on top and never takes the keyboard.
- The manifest has no new permission, and the release build does all of the above.

---

## 5. M3 · The selection

**Promise.** Select text in any app, right-click, and Booklight rewrites it where it stands.

**Who, and the moment.** Someone writing in another app's text field: a mail, a chat, a document. The sentence
is written and is not right yet: a typo, the wrong tone, the wrong language, or no umlauts because the keyboard
has none.

**What is in it** (four features; all of them are prompts, so each shows up in every place M1 and M2 built)

1. **Prompts in the selection menu, by name.** Booklight already stands in that menu as one entry. Now "Fix
   spelling", "In German", "Shorter" can stand there themselves, switched on one by one in the window. Pick
   one: the panel opens with the answer already being written, and Enter replaces the selection where the
   field allows it (NF2 §3, idea 5).
2. **Put the letters back.** "Gruesse aus Koeln, ich komme spaeter" becomes "Grüße aus Köln, ich komme
   später"; French and Danish too. Booklight checks that only letters changed and shows nothing if the model
   changed a word, so it cannot rewrite you. A single word needs no model (`abc Koeln`) (NF2 §3, ideas 4 and 16).
3. **Translate into any language.** `tr danish see you on Saturday`. Today there is one fixed direction (NF2
   §1, idea 20). It reuses M1's sense of direction.
4. **Three more ready-made prompts:** `formal`, `friendly`, and `error` (what an error means and its likely
   cause, with a web search of its first line beside it). `error` is shown, not put back (NF2 §1, idea 6).

**Deliberately out.** Pasting into a field that does not take the answer back (power pack). Snippets that expand
as you type (ruled out by the research: NB, F11). `reply` ("Gmail and Messages already do it where it matters":
NB, AI4), `commit` and `define` (you can add them yourself under Yours; the lists stay short). `abc` by
keystrokes (§8).

**Why third.** It stands on M1 (the list of prompts, the direction of a translation) and uses M2 (an `error`
on a picture of an error). It is small and has no library. And it leads into M4: the letter table of feature 2
is the same one the language groundwork adds for matching, and the question "which languages does the model
write well" is asked here for three languages and in M4 for six.

**Needs decided**

| # | Decision | Recommended |
| --- | --- | --- |
| D8 | **Which prompts stand in the menu from the start** | Two: Fix spelling and the translation. The rest are off until you switch them on. To check first: how many entries of one app the Googlebook's menu shows before it folds them away (NF2 §3, idea 5: **not verified**) |
| D9 | **Which new prompts are seeded** | `formal`, `friendly`, `error`. Not `reply`, `commit`, `define` |
| D10 | **The word for "put the letters back".** The research's example keyword is `ä`: the one letter this user cannot type | It lives under `abc`: `abc` and a whole sentence. The UX designer should confirm or propose better |
| D11 | **The translate keywords** (LANG §12.5): `tr` and a language; does English keep `de` ("In German") and German keep `en`? | Yes, both stay; `tr` is added |

**Not verified:** the menu (D8); which languages the model writes well, beyond German and English (LANG §5:
Danish is on none of Google's lists, French is on two); the HP.

**How we would know it worked.**
- In Chrome, in a mail being written and in the text editor: select, right-click, "Fix spelling" is there by
  name, and the answer replaces the selection. In a place that cannot be edited the answer is shown and copied.
- Fifty test sentences in German, Danish and French: each comes back with the right letters or not at all.
  Never with a changed word.
- The menu holds exactly the entries that are switched on.
- German on the new language structure still passes today's 221 tests unchanged (the groundwork ships here).

---

## 6. M4 · Your language

**Promise.** Booklight speaks six more languages: its screens, its keywords and its dates.

**Who, and the moment.** People whose Googlebook is set to French, Spanish, Italian, Danish, Swedish or
Norwegian. The moment is every time they open it, and the store page before that.

**What is in it** (one thing, six times; these are its parts, not separate features)

1. **The six languages, as deep as German is today:** the screens, and the typed words: keywords, verbs, dates
   and times, the words for settings pages and shortcuts (LANG §12.2, depth b). The English keywords always
   work too.
2. **Dates and numbers the way the language writes them:** "ven 15h dentiste", "kl. 15.30", "dans 20 min",
   1 234,5 (LANG §2, §7 item 2).
3. **The prompts in that language where the model passed a test on the device,** and a hand-over to the
   Gemini app where it did not (LANG §5).
4. **The store page** in each language, written to fit Play's limits, not translated (LANG §6).

**Deliberately out.** Polish (D12). Japanese, Korean and Chinese: a second block of about nine days of one-time
work and about eighteen for the languages, and a different kind of work: input methods, no spaces, matching by
kana, Hangul and pinyin (LANG §7, §11). Store pictures per language.

**Why languages are a milestone, with a track before it, and why last.**
- *Not part of a feature milestone:* they are weeks of work and add no feature. Inside M1 to M3 they would
  hide the story of each.
- *Not only a track:* for a French user it is the one release that matters, and it deserves its own name,
  notes and store page.
- *Split in two:* the ten days of groundwork change nothing for the user, so they are a track (§2). What is
  left for M4 is the languages themselves: about eleven and a half days for six (LANG §2), plus the wait for
  readers.
- *Last:* the lasting cost is that every new keyword, tip and release note is then written once per language
  and checked by someone who reads it (LANG §1, §13). M1 to M3 add a handful of keywords and a few dozen
  texts. After M4 each would be written eight times; before M4 they are translated once, with everything else.

**Needs decided.** The research lists thirteen (LANG §12). These are the ones that shape M4; for the rest the
research's own recommendation stands unless you say otherwise.

| # | Decision | Recommended |
| --- | --- | --- |
| D12 | **Which languages.** You named ten | The six in M4. Polish next, as its own release (three plural forms, day names that change with grammar, `w` is a word: LANG §2). Japanese, Korean, Chinese as a later block. Where Googlebooks are sold should set the order, and that is **not verified** (LANG §12.8) |
| D13 | **Which words the date reader knows** (LANG §12.3) | The app's language and English. The cost: "morgen" stops working in an English Booklight. If you type German into an English Booklight yourself, say so: then it should be "the app's language, English, and German for you" and the engineer has to size that |
| D14 | **The model where Google lists no support** (Danish, Swedish, Norwegian: LANG §12.6) | Offer it if the test on the Lenovo reads well to a native reader; otherwise those languages get the Gemini app only |
| D15 | **Who reads each language** (LANG §12.9) | A machine draft, then one native reader per language who looks at a picture book of about forty screens and says what he would type for twenty things. Paid translation is about USD 260 a language if you want it (LANG §9) |
| D16 | **The rule "every string in English and German"** (LANG §12.13) | English and German stay the rule for building. The other six may trail by one release. A test fails the build when a language's keyword list is missing or collides |
| D17 | **Store pictures** (LANG §12.10) | English pictures under translated text, as German today |

**Not verified:** the model in Danish, Swedish and Norwegian; the system Settings app's page names in each
language on a Googlebook; Play's folder names; the day figures, which are estimates from counts, "not from
having done one" (LANG §14).

**How we would know it worked.**
- In every language each example of the list of everything runs (`./bl debug guide`, LANG §10).
- One native reader per language has seen the picture book and answered the three questions.
- Nothing is cut off: tips within 90 characters, place names within 12, the store text within 4,000.
- There is a table of the five prompts per language from the Lenovo; where a language failed, its rows hand
  over to Gemini.
- English and German behave as before.

---

## 7. What each milestone hands to the next

| From | It builds | Used by |
| --- | --- | --- |
| Today's polish | The quieter highlight; the shorter action strip | M1's row and list |
| M1 | A row for something you did in another app; the list "what is in it" and "what the model can do"; the direction of a translation; `do` (an instruction and a material) | M2 (a picture in the same row, its text in the same list, a question about a large picture is `do` with the picture's text), M3 (the same prompts, now in the selection menu) |
| M2 | Pictures handed to Booklight; the recogniser | M3 (`error` on a picture) |
| Groundwork | Words per language; the letter table; the keyword test | M3 (the letters check; new keywords born in the right place), M4 |
| M3 | The first test of the model in other languages; the final list of prompts and keywords | M4 (translated once) |

---

## 8. Not in these four, and why

| Idea | Why not now |
| --- | --- |
| **Say it plainly, a guess when nothing matches, settings in plain words, emoji by description** (NF2 ideas 7, 9, 18) | One real story ("say it your way"), and the best candidate for a fifth milestone. Left out because it is the largest model work (M to L), it takes one to two seconds for something that looks instant (NF2 §2.3), and it is not one of your three. Its cheap half, more everyday words per settings page, needs no model and can ride in any release |
| **Pick a colour from the screen** (idea 8) | Close to M2's story but a different mechanism, and one device check is open. Not ticked for 2.0 (NB, C1). The first thing to add to M2 if you want a fifth |
| **Text tools** `b64`, `jwt`, `sha256`, `epoch`, `json` (idea 10, first half) | Useful and small, but they share nothing with the four. Not ticked for 2.0 (NB, D1) |
| **Stop what is ringing** (idea 15) | A repair, not a feature: Booklight starts timers and cannot stop one. It belongs in a small release beside the polish |
| **`abc` by keystrokes** (`abc "u`: idea 16, first half) | Tiny, for people who know compose sequences. It can ride with M3 if you want it; it is not part of M3's promise |
| **Tasks out of a text** (idea 17) | `do pull out the tasks` in M1 does the reading. Adding them to Todo.md is a notes story |
| **`reply`, `commit`, `define`** (idea 6, the rest) | You can add them under Yours. Kept out so the menu and the lists stay short |
| **Units, money, time zones** (idea 13) | It needs Summa's engine inside Booklight or the layer of extensions that answer while you type. A milestone of its own |
| **A key per command** (idea 11) | One device check is open (does each get its own shortcut: NB, L8), and it is about opening Booklight, not about the four stories |
| **What other apps can do with a thing; packs; your people** (ideas 14, 21, 19) | Each is the start of another story (extensions, sharing, people) |
| **Capture from Booklight** (`shot area` through StudioSnap: NF2 §4) | A small release of StudioSnap, in its repo. It would make M2 one command. After M2 |
| **Polish; Japanese, Korean, Chinese** | D12 |
| **Silent capture; the power pack; the Gmail relay** | Yours to decide (D7); deferred; "not yet" |
| **The other unbuilt ideas** | They stay in `not-built-2.0.md` with their ratings |

---

## 9. All decisions, in one list

| # | Milestone | Decision | Recommended |
| --- | --- | --- | --- |
| D1 | M1 | A row with nothing typed | Yes, with an off switch |
| D2 | M1 | How fresh the copy must be | Two minutes |
| D3 | M1 | The copy or the tip | The copy |
| D4 | M1 | Enter on the empty panel with the copy's row | Nothing until Tab or Down |
| D5 | M2 | Your check: does a capture land on the clipboard | Do it before M2 is designed |
| D6 | M2 | The text recogniser | Yes, if it brings no permission |
| D7 | M2 | Silent capture as the assistant | Not in these four; not ruled out |
| D8 | M3 | Prompts in the menu from the start | Two |
| D9 | M3 | New ready-made prompts | `formal`, `friendly`, `error` |
| D10 | M3 | The word for "put the letters back" | Under `abc` |
| D11 | M3 | Translate keywords | Keep `de` and `en`, add `tr` |
| D12 | M4 | Which languages | Six; Polish next; the three Asian languages later |
| D13 | M4 | Words the date reader knows | The app's language and English |
| D14 | M4 | The model in unlisted languages | Only where the device test reads well |
| D15 | M4 | Readers | Machine draft, one native reader each |
| D16 | M4 | The English-and-German rule | It stays; other languages may trail by one release |
| D17 | M4 | Store pictures | English pictures, translated text |
| D18 | Tracks | The window redesign's release | Alone, as soon as it is ready; with M2 at the latest |
| D19 | Order | The picture before the selection | Yes; swap them if D5 and the checks under M2 go badly |

The research's five questions (NF2 §5) are D1, D6, D5, D7, and this whole frame (which ideas go next).

---

## 10. Questions for the team

### For the UX designer

1. **The empty panel now has three guests:** the copy's row, a tip, and the white reflection that runs round
   the edge a few seconds after opening (today's polish). Who shows, in what order, and what never shows
   together? My default: the copy replaces the tip (D3).
2. **Is the copy's row selected?** The rule is "row one selected, Enter runs it". A row nobody asked for must
   not run on a stray Enter (D4). What do Enter, Tab, Down and typing each do while it is there? Typing should
   put it away in the same frame, as it does a tip.
3. **What Tab opens.** 2.0 already has "text another app handed over": the text is the chip, the rows are
   what to do with it. Is the copy the same thing (Tab makes the copy the chip), or the list under a row?
   One answer for the copy, the picture and the selection, please.
4. **What the row says**, in plain words, for: a link; a date; several things; plain text; a text too long to
   be classified (over 400 characters, where the row cannot know more than "text"); a picture. It can never
   show the text itself, because it has not read it.
5. **`do`:** where the instruction and the material show; what it says when nothing is copied; how a copy that
   is too long is cut and said. And its German keyword: `do` is also short for Donnerstag.
6. **M2:** three ways in (clipboard, share, picker): one flow or three? Where is the question typed, and what
   stands in the field's chip for a picture? What does a whole-screen picture get? How is "this may be wrong"
   said without a warning on every row?
7. **M3:** the names in the menu; what happens between the pick and the replacement; what the user sees when
   the field cannot be edited.
8. **D10:** the word for putting the letters back.
9. **Count the keys** for the main path of each milestone, from "I copied" or "I selected" to "it is where I
   need it". M1's target is five presses and no typing.
10. **M4:** what happens to tips (90 characters), place names (12) and the tip's word order in a longer
    language, and which one-letter keywords survive.

### For the visual designer (and the motion designer)

1. **A quiet row.** How does a row that came by itself look different from a result, without a second
   highlight and without a new component? Is it a row at all, or the measure of the tip card?
2. **The marks for what was found** (link, date, phone, flight, picture) in one 56 dp row: their size, their
   order, their alignment with the row's icon column and text baseline. Please draw on the real numbers in
   `Metrics` (720 dp panel, 68 dp field, 56 / 92 / 208 dp rows): an earlier mock-up was rejected as "really
   misaligned".
3. **A picture on glass.** A screenshot is bright and opaque; the panel is see-through. How large is the
   thumbnail, what corners, which row height, what keeps it from looking pasted on? The same for a picture in
   the pinned window.
4. **The list after Tab** has two kinds of entries: instant ones and ones the model answers. How is that shown
   without groups or headers ("no groups, no tabs")?
5. **Dark theme** with today's softer highlight: do the new row, the marks and the thumbnail still read?
6. **Motion.** The copy's row arrives after the opening at medium speed (the rule: what is under the field
   comes once the glass is open). How does it arrive, how does it leave when a key is typed, and how do it and
   the edge reflection keep out of each other's way? How does a picture arrive in a row and in the pin? Nothing
   may pop.
7. **M4:** which components break with text 20 to 30 % longer.

### For the engineer

1. **The HP.** Does the model answer there at all? Everything in M1 to M3 depends on the answer.
2. **Reading the clip's description when the panel opens:** what does it cost against today's opening time?
   The classification may still be running two seconds after a copy (DF): what happens to a row whose content
   changes while it is shown? (A row changing under the selection hid a crash in 1.1.)
3. **The classifier on a Googlebook:** which entities and which ready-made actions come back; the German
   address that was not found; the 400-character limit.
4. **M2's hand-over:** which of the three routes can be confirmed on a device, and in what order to build
   them. The photo picker is another window and the panel closes on a click outside: can the panel survive it?
5. **The recogniser (D6):** its manifest, its permissions and reporting; bundled (about 4 MB) or through Play
   services (about 260 KB); its speed; whether small interface text on the HP's screen clears its 16-pixel
   floor; the keep rules a release build needs.
6. **A picture in the pin:** the smallest size, the shape, what the system does when the user resizes it.
7. **When the model will not answer** (busy, the battery quota, a policy refusal: S&C §1.4): one plain state
   for all rows, including the new ones.
8. **M3's menu:** how several entries of one app are declared and switched on and off; how many the Googlebook
   shows (D8); how the answer goes back; how a read-only selection is told apart. And the letters check: how
   it compares, and what counts as "only letters changed" in German, Danish and French.
9. **The groundwork:** can its ten days run beside M2 without colliding with it (one of its items makes room
   for longer text in the panel's strip and field)? Which of its eight items must be in before M3 is built?
   Is ten days right? If D13 becomes "and German for you", what does that cost?
10. **For each milestone:** your own size, the riskiest piece, and what you would cut first.
