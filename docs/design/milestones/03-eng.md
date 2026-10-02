# Booklight after 2.1: how the four milestones are built

*Engineer's part, 1 October 2026. Planning only: nothing in the repo was changed, no device was touched, nothing
was built. It answers the PM's frame (`01-frame.md`) and uses its short names: **NF2** = `next-features-2.md`,
**S&C** = `screen-and-clipboard.md`, **DF** = `device-findings.md`, **LANG** = `languages.md`,
**NB** = `not-built-2.0.md`, **AI** = `on-device-ai.md`. Code is named by file. "Not verified" is said where the
research says it. Where I add an expectation of my own, I say that it is one.*

*File: `docs/design/milestones/03-eng.md`*

---

## 1. In short

1. **No milestone needs a new permission.** For the text recogniser (D6) that is no longer a guess: I read the
   manifests of the library and of everything it pulls in (§2.2). It asks for two permissions, and 2.1 already
   has both.
2. **Eight things in the frame cannot be built as worded** (§3). The two that matter: the selection menu can
   carry Booklight's ready-made prompts by name, but never a prompt you wrote yourself; and "in any app" is
   "in apps that show such entries".
3. **My sizes are larger than the frame's** (§13): M1 about 13 days, M2 about 14, M3 about 10, the groundwork 12,
   M4 about 17. They include what the frame's letters leave out: the shared pieces, the motion passes, the
   release-build check, both devices.
4. **Four changes to the frame, for engineering reasons** (§11): the copy's row is not a row of the list; six
   shared pieces are built once, in M1; `error` moves to M2; the groundwork is merged before M3 is started, and
   is better as a quiet release of its own.
5. **One device session before M1's design is final** (§12): thirteen checks, half a day, both devices. Seven
   more come before the later milestones. Most need a throwaway build.

**About the days.** They are the research's unit (S a day or less, M a few days, L a week or more; "working days
for Alex with Claude", LANG §2). `docs/PICKING-UP.md` dates every release from 0.1 to 2.1 on 1 October 2026, so
this project has run far ahead of such figures. Read them as sizes against each other, not as a calendar.

---

## 2. What this rests on

### 2.1 Read in the code

| Fact | Where | What it means here |
| --- | --- | --- |
| With nothing typed the model asks no one and shows no rows. The first-run card and the tip are two states of their own, drawn under the field, with nothing armed until Tab | `overlay/OverlayModel.kt` (`search`, `card`, `tip`, `tipArmed`), `overlay/Panel.kt` (`body`), `Metrics.height` | The copy's row has a ready-made home, and D4 ("Enter does nothing until Tab") is how a tip already behaves |
| "Text another app handed over" exists: the text is the chip, the rows are what to do with it, three prompts by name among them. It is not kept and never sent for suggestions | `scopes/Picks.kt` (`TextScope(fixed)`), `scopes/Scopes.kt` (`receive`, `forget`), `OverlayModel` (`foreign`) | Tab on the copy can open exactly this. One answer for the copy, the picture's text and the selection |
| A prompt chosen from that list gets the text typed into the field, on one line | `scopes/Picks.kt` (`EnterScope(PromptScope.key(p), one)`) | A copied page would be a field of 8,000 characters without its line breaks. The text has to travel beside the field (§4, piece 2) |
| A prompt reads at most 8,000 characters and answers at most 512 tokens, about 2,000 characters | `scopes/Prompts.kt` (`MAX`), `ai/OnDevice.kt` (`MAX_OUT`) | A translation of a longer copy comes back cut today, and nothing says so |
| Every failure of the model ends in one state: "No answer on this device this time", with the way to the Gemini app | `ai/OnDevice.kt` (`ask`), `scopes/Prompts.kt` (`unanswered`) | The one plain state the frame asks for exists. It does not know why |
| The five prompts are copied into the settings file once, in the language of that day | `data/Prefs.kt` (`started`) | New ready-made prompts never reach an existing installation, and a change of language never reaches the old ones |
| The engine gives a scope 150 ms for its rows | `core/SearchEngine.kt` (`budgetMs`) | Anything slow (the classifier, the recogniser) must be done before the list is shown, or fill a row that is already there |
| The panel closes when it loses focus and when it is stopped | `overlay/OverlayActivity.kt` (`onWindowFocusChanged`, `onStop`) | It cannot stay open behind the photo picker |
| A helper without a screen already opens a system picker and acts on the result | `entry/PickFolderActivity.kt` | The pattern for the photo picker |
| Selected text arrives with "may be replaced" or not; Replace hands the answer back. Several paragraphs are treated as not replaceable | `OverlayActivity.take`, `Executor.kt` (`Effect.Replace`) | M3's road exists. I found no note that Replace was ever tried on a device |
| The pin opens at the smallest size its manifest entry names, 280 × 118 dp; shapes between 1 : 2.39 and 2.39 : 1 | `AndroidManifest.xml`, `pin/Pinned.kt`, DF "The pinned window" | A pinned picture needs its own entry, or it opens tiny |
| The reader of dates looks only at the first or the last words of a line | `core/When.kt` (its rules) | A date in the middle of a copied sentence must be moved to the front before it is read |
| The clipboard is touched in five places | `scopes/Picks.kt`, `scopes/Prompts.kt`, `Executor.kt`, `pin/PinActivity.kt`, `device/QrImages.kt` | One helper first |
| Today's polish is in the working tree as it stands now: settings schema 4 is called 2.2, the white reflection waits 2.4 s and 0.7 s of quiet | `data/Prefs.kt`, `overlay/Panel.kt` | M1's settings schema is 5. The reflection can start in the very frame the copy's marks land (§5.1) |

### 2.2 Checked today without a device

**The text recogniser's manifests** (D6's condition; S&C §3.1 said "its manifest was not opened today"). I
fetched `com.google.mlkit:text-recognition:16.0.1` and the seventeen Google libraries it pulls in from Google's
Maven and read every manifest among them (sixteen; two are plain Java libraries without one). They are saved
beside this file in `eng-ocr-check/m/`.

- **Permissions in the whole tree: `ACCESS_NETWORK_STATE` and `INTERNET`**, both from the `datatransport`
  libraries. 2.1's merged manifest has both.
- **Components:** nothing of a new kind. `MlKitInitProvider`, `MlKitComponentDiscoveryService`,
  `GoogleApiActivity`, the Play services version entry and the three `datatransport` parts are all in 2.1's
  merged manifest already. New are two lines that name the recogniser's parts (`TextRegistrar`,
  `VisionCommonRegistrar`).
- **Size:** one native library, 10.8 MB for arm64 and 11.4 MB for x86_64 (4.3 and 4.4 MB compressed), plus
  1.45 MB of model files. The research's "about 4 MB" is the compressed download. On Play that is about 5 to 6 MB
  more per device, and about 13 MB more on the device.
- **The GitHub APK** is 3.5 MB today, holds four architectures, and stores native libraries uncompressed. With
  the recogniser and no filter it would be about 45 MB. Limited to arm64 and x86_64 (the HP and the Lenovo):
  about 27 MB. Packed compressed as well: about 13 MB.
- **16 KB pages:** the arm64 and x86_64 libraries are aligned for them (read from the files).
- **Keep rules** come with the library (native methods, generated fields). The rule 2.0 needed for ML Kit's
  parts covers the new ones.
- Not followed: the AndroidX libraries these depend on. The merged manifest of a real build is the last word,
  and the release build has to be run (the 2.0 lesson).

**The model's library** (`genai-prompt` 1.0.0-beta4, from the Gradle cache): it has `ImagePart(Bitmap)`, a
finish reason on every answer (`STOP`, `MAX_TOKENS`, `OTHER`) and `countTokens`. So "the answer was cut here"
can be said, and a picture goes in as a bitmap.

**The HP:** it reports `AICORE_QC_HAMOA`, "so it very likely has AICore too (inferred). That AICore is installed
does not prove it answers a third-party app" (AI §3.1). I cannot answer the PM's first question from a desk.
It is check 1.

---

## 3. Not possible as worded

| The frame says | What is true | What I would build |
| --- | --- | --- |
| M3: "Select text in any app" | Each app builds its own menu. Only apps that list other apps' text actions show Booklight. Which do on a Googlebook, and whether a right-click shows them, is **not verified** | "In apps that show such entries". Check 15 names them |
| M3: prompts in the menu "by name, switched on one by one" | A menu entry is declared when the app is built, and so is its name. Booklight's ready-made prompts can each have one. **A prompt you wrote cannot stand there under its name**, and a ready-made one you renamed keeps its built name | A fixed list of ready-made entries. Your own prompts stay behind the one "Booklight" entry, as today |
| M3: "A single word needs no model (`abc Koeln`)" | Without a word list it is a blind swap: "neue" becomes "neü", "Feuer" becomes "Feür" | A single word goes through the model and the same check as a sentence |
| M3: German "passes today's 221 tests unchanged" | The tests run with English and German both on. After the groundwork a reader has one language and English | The same cases, each with its language named |
| M1: "Copy as Markdown" | Booklight does not know a page's title and fetches no pages (the internet permission is for suggestions only) | `[example.com/page](address)` |
| M1: "Track the flight", "Open in Maps", "Call" | There is no request for flights: it is a web search for the number. `geo:` is answered only by Zoom on the HP (DF), so Maps is a web address. Whether anything answers `tel:` on a Googlebook is **not verified** | Search, a Maps address, and Call only where something answers |
| M1: Translate, Fix, Shorter on a copied text of any length | The answer is capped (§2.1). At 70 to 100 characters a second (DF), 3,000 characters take 30 to 40 seconds, and an answer is given up on after 45 (`OverlayModel`, `ANSWER_MS`) | On the device up to about 3,000 characters, with "cut here" said when it is. Longer: Summarise and Explain here, the rest in the Gemini app |
| M2: "the table you are typing from" stays on top | A pin opens at its smallest size and the system decides how large it may be dragged. A 1,200-pixel table in a pin 420 dp wide is shown at half size on the Lenovo. How large a pin may be is **not verified** | Its own, larger opening size. If the system caps it: word the promise for a small region |

Everything else in the frame can be built as it is written.

---

## 4. Shared engineering: built once

All six of M1's pieces are used again by M2, M3 or M4. Built later, each would be built twice.

| # | Piece | Files | Built in | Used by | Days |
| --- | --- | --- | --- | --- | --- |
| 1 | **One way to an answer.** Any scope can have a row answered by the model, not only a prompt's. The reason for no answer is kept (busy, the allowance, a refusal, too long), and "cut here" when the finish reason says so. The words for these are UX's; the state stays one | `OverlayModel.ask` (today tied to `PromptScope`), `ai/OnDevice.kt`, `core/Model.kt` (`Body.Stream`) | M1 | M2 (a picture's row), M3 (`abc`, `tr`) | 1 |
| 2 | **The material beside the field.** A prompt gets its text from what was typed, else from the text handed over, else from the copy, with its line breaks | `scopes/Prompts.kt`, `scopes/Scopes.kt`, `core/Prompts.kt` | M1 | `do`, a picture's text, the menu, replacing several paragraphs | 0.5 |
| 3 | **Ready-made prompts by reference.** The settings file keeps a ready-made prompt's id; its name, keyword and text come from the resources until you edit it. New ones are added once per version | `data/Prefs.kt` (schema 5), `scopes/Scopes.kt`, `window/Commands.kt` | M1 | `do`; M3's `formal`, `friendly`, `error`; M4 (a change of language reaches them) | 1 |
| 4 | **One clipboard helper:** look (the description only), read text, read a picture, set, and "this copy is mine" | `device/Clipboard.kt` (new), the five places of §2.1 | M1; the picture half in M2 | M1, M2 | 0.5 |
| 5 | **The language of a text,** and a translate prompt with a hole for the target | `device/` (the system's `detectLanguage`), `core/Prompts.kt` | M1 | M3's `tr`, M4 | in 5.3 |
| 6 | **Plain answers:** the model writes Markdown unless told not to (DF). One small cleaner | `core/Plain.kt` (new) | M1 | every answer | 0.5 |
| 7 | **The picture store:** a handed-over picture is copied into Booklight's cache, decoded off the main thread, and deleted when the panel or the pin closes | `device/Pictures.kt` (new) | M2 | Ask, the recogniser, the pin | in 6.1 |
| 8 | **Letters and their plain writings** in one table (ä: ae, a; ß: ss; ø: oe, o) | `core/Latin.kt` (new), from `core/Letters.kt` | M3 or the groundwork, whichever is first | the letters check, matching (LANG §7 item 3) | 0.5 |
| 9 | **Debug hooks:** set a copy, print its description, hand a picture over, read a picture's text, start as a menu entry | `app/src/debug/…/DebugReceiver.kt`, `bl` | with each | every device check | in each |

---

## 5. M1 · The copy

**Total: about 13 days** (11 to 15): 3.5 for the shared pieces, 7.5 for the four features, 2 to finish.

### 5.1 The copy, as a row

- **Built.** Not as a row of the list: as a third guest under the empty field, beside the card and the tip.
  `OverlayModel` gets `copy`, offered once the window has focus and the glass has opened; `type()` puts it
  away in the frame it puts a tip away; `offerTip()` stands back while it is there (D3). `Panel` gets one more
  state under the field and `Metrics.height` its height. Why not a list row: the list's rule is "row one
  selected, Enter runs it", and with it come the pill, the footer, the grey completion, Ctrl + 1 and learning.
  D4 wants none of them.
- The look is `getPrimaryClipDescription()` alone, when `onWindowFocusChanged(true)` arrives (the clipboard
  answers only the focused window: S&C §4.1). That is after the first frame, so the opening does not wait for
  it. The rule (younger than two minutes, not private, not Booklight's own, a known kind) is `Clip.offer` in
  `core/Clip.kt`, with tests.
- "Booklight's own": every copy Booklight makes goes through the helper, which remembers its time; the label
  "Booklight" that `Executor.copy` already sets covers a restart.
- **A row that changes while shown.** While the system is still classifying, the helper looks again every
  400 ms, five times at most. Only the marks change. The height and place do not, and nothing is selected, so
  the crash of 1.1 has no room here. The reflection's "quiet" test in `Panel` gets the copy's state too:
  today it could start at 2.4 s, just as late marks land.
- D1's switch: `Settings.copyRow`, one row in the window.
- **Size:** 2.5 days. A day of it is the arrival, the leaving and the marks landing, recorded and stepped
  through.
- **Check first:** 3, 4, 5. If the description cannot be read on the HP: no row there, and Tab on the empty
  field opens the copy (D1's "no" branch). The rest of M1 stands.
- **Asks for:** nothing. `PRIVACY.md` must change: it says the clipboard "is read only when you ask for it".
  New: when the panel opens, Booklight asks the system what kind of thing was copied and when; the content is
  read only on Tab, `clip` or a prompt. Data safety: no change.
- **Tests.** Core: the offer rule (age, private, own, kinds, a clock set back), the marks' order. Device:
  copies from Chrome, Gmail and a password manager; no message; late frames against 2.1 (`dumpsys gfxinfo`).
- **Risk:** frames at the opening; the timestamp is wall-clock time (S&C §4.1).

### 5.2 Tab: what is in it

- **Built.** Tab or Down reads the text (the system's message, once) and enters `TextScope` through
  `Scopes.receive`. `clip` opens the same list, so there is one list, not two.
- What is in the text: for a clip the system classified (up to 400 characters), the places it found come
  with the clip (`getTextLinks`, S&C §4.1). For a longer one Booklight asks the system's classifier itself,
  off the main thread, **before** the list is shown, with a cap of about 150 ms. Over the cap the list comes
  without those rows. Rows are never slipped in above the selection later.
- The order of offers is data from `core/Clip.kt`. `core/Links.kt` (new) cleans a link and writes it as
  Markdown.
- No new effect. A link: `OpenUrl`. A date: `EnterScope("event", …)` with the date moved to the front, so
  the Event row opens filled in and you see the day before Enter. Maps and a flight: `OpenUrl`. Call:
  `OpenUrl("tel:…")`.
- **I would not use the classifier's ready-made actions** (S&C §4.2, "not probed"). They are sealed requests
  of the system's: Booklight could not say what one does, test it, or keep "the executor is the only place
  that starts activities". The kinds of things found are enough. The PM's question 3 is then nice to know,
  not a blocker.
- **Size:** 2.5 days.
- **Check first:** 6, 7. Addresses not found: no Maps row. Nothing answers `tel:`: Copy number. The
  classifier too slow: only the system's own findings, so long texts get no such rows.
- **Asks for:** nothing.
- **Tests.** Core: twenty links cleaned, the offers and their order, a date moved and read (English, German).
  Device: DF's probe texts.
- **Risk:** `When` cannot read every date the classifier finds ("Friday, October 3rd"). The Event row then
  shows a guessed day, dimmer, as today.

### 5.3 What the model can do with it

- **Built.** The list's prompt rows and their order come from `Clip` too: Summarise only for a long text,
  Translate first when the text is not in your language. Enter on one enters that prompt with nothing typed;
  it takes the copy as its material (piece 2) and is asked at once, not after today's half-second pause.
- Direction: `detectLanguage` on the read text. A text in another language goes into the app's language. A
  text in yours goes into "the other one": German in an English Booklight, English otherwise, which is what
  today's `de` and `en` say. No setting in M1.
- The cap of §2.1: answers up to about 1,500 tokens for prompts that rewrite; "cut here" when the finish
  reason says so; over about 3,000 characters Translate, Fix and Shorter go to the Gemini app (§3).
- **Size:** 1.5 days, given pieces 1 to 3.
- **Check first:** 1 and 8. No model on the HP: these rows hand over to Gemini there, and 5.1 and 5.2 are
  untouched.
- **Asks for:** nothing.
- **Tests.** Core: the target (the text's language against the app's), which prompts for which length, a
  prompt with two holes. Device: the five prompts on copies of 50, 500 and 3,000 characters.
- **Risk:** several asked in a row run into the allowance, which has no published numbers (S&C §1.4).

### 5.4 `do …`

- **Built.** One more ready-made prompt with two holes: what is typed is the instruction, the material comes
  through piece 2. `Prompts.fill` gets a second argument. Nothing copied: a row with nothing to run.
- **The keyword.** `do` and a space would swallow "do not disturb", which is a settings page
  (`settings_pages.xml`). The engine's rescue for a keyword that begins an ordinary name covers apps and
  other apps' commands only (`SearchEngine.inScope`). Either the rescue learns settings pages (small, and
  right anyway), or UX picks another word. In German I see no clash in the code: "do" is Thursday only
  inside the date reader.
- **Size:** 1 day.
- **Check first:** ten instructions on three texts on the Lenovo: which kinds it follows.
- **Asks for:** nothing. **Tests:** core, the fill. `./bl debug guide` must set a copy before it runs `do`'s
  example.
- **Risk:** an instruction followed badly. It is only shown, copied or pinned.

### 5.5 To finish, the cheapest, the risk

- **To finish, 2 days:** about 25 strings in English and German, two guide lines, a tip, the privacy text,
  the release build installed and asked a prompt, both devices, what the reviews find.
- **Cheapest that keeps the promise, about 5 days:** the row with kind and age only; Tab opens today's `clip`
  list with Open the link and Add the event in front and the five prompts by name (a prompt with nothing
  typed already reads the copy); no direction, no `do`, no Call, Maps, flight or link tools. The shared
  pieces then fall to M2 and M3.
- **Riskiest:** the row, done to the standard of the opening. **Cut first:** Call, Maps and the flight; then
  the Markdown link.

---

## 6. M2 · The picture

**Total: about 14 days** with the recogniser (12 to 16), **about 10 without**.

### 6.1 A picture reaches Booklight

- **Built.** `device/Pictures.kt` (piece 7). A capture can show other people's mail: the panel's copy is
  deleted when the panel closes, leftovers at the next start, and the cache is in no backup. Core gets
  `Icon.Image` and `Body.Picture`. `scopes/Picture.kt` is the twin of the handed-over text: the picture is
  the chip, what is typed is the question.
- Three ways in, in the order I would build them:
  1. **Share.** One more intent filter on the panel (`SEND`, `image/*`); `OverlayActivity.take` reads the
     address. This is plain Android and works for any app that shares a picture. Whether the capture's
     preview offers Share is **not verified**.
  2. **The clipboard.** M1's look already tells a picture by its type; Tab reads its address. It works for
     anything that copies a picture (a browser's "Copy image"), whatever D5 says. **Not tried** (DF).
  3. **The photo picker.** A helper without a screen, like the notes folder's. **The panel cannot survive
     the picker**, and should not try: it closes, the picker shows, and the panel comes back with the
     picture as its chip.
- **Size:** 2.5 days for the store, the scope and Share; 1 for the clipboard; 1 for the picker.
- **Check first:** 9, 10, 11. If a capture is not copied and its preview has no Share, the picker is the
  only road for captures and is no longer the part to cut.
- **Asks for:** manifest: one intent filter and one activity without a screen. No permission; the photo
  picker needs none (S&C §2.2). Play: nothing, and the photo and video policy is not touched. `PRIVACY.md`: a
  paragraph "Pictures".
- **Tests.** Device: each way in by hand, Cancel in the picker, a 20 MB picture, an address that cannot be
  opened.
- **Risk:** three ways in are three flows to design and test.

### 6.2 Ask about it

- **Built.** `OnDevice.ask` takes a bitmap too. Piece 1 lets the picture's row be answered like a prompt's.
  The prompt ends "one or two sentences, plain text" (DF).
- **The size rule** (`core/Fit.kt`, with tests). With the recogniser: how high the lines are once the picture
  is shrunk to what the model sees (768 pixels a side, inferred: S&C §1.2). By my arithmetic from DF's
  pictures and S&C §3.1 the limit lies between about 6 and 13 pixels. Above it the picture is asked. Under it
  the picture's text is asked, and the caption says so. Without the recogniser: the longer side up to 1,600
  pixels, else "capture a smaller piece".
- **Size:** 2 days, half a day of it to find the limit.
- **Check first:** 1, 13. No model on the HP: there is no Ask there. The Gemini app is handed text today;
  whether it takes a picture this way is **not verified**.
- **Asks for:** nothing more.
- **Tests.** Core: the rule. Device: DF's three pictures and a whole screen, through Share.
- **Risk:** pictures near the limit get invented answers.

### 6.3 Copy its text (D6)

- **Built.** `ai/Reader.kt` around the bundled recogniser (bundled, not through Play services: it works at
  first use and has no download to design). `core/Lines.kt` turns the found lines and their boxes into text
  in reading order (tests: a table, a terminal, two columns). The row "Copy its text" stands from the start
  and says "Reading…" until it fills, so no row arrives late. Its actions: Copy; Use, where the text becomes
  the chip and gets everything M1 built; Pin. Small text is enlarged twice before reading (the HP: S&C §3.1).
- **Size:** 3.5 days.
- **Check first:** 12, in a release build.
- **Asks for:** no new permission (§2.2). Data safety: the two entries 2.0 added (Device or other IDs,
  Diagnostics) already cover ML Kit; the note becomes "two libraries". `PRIVACY.md`: one sentence. The size
  of §2.2.
- **Tests.** Core: `Lines`. Device: the exact characters of DF's three pictures.
- **Risk:** the size of the GitHub APK; small text on the HP.
- **If D6 is no:** no "Copy its text", nothing of M1 works on a picture, and a whole screen gets a plain
  "too large". M2 is then 6.1, 6.2 and 6.4.

### 6.4 Pin it

- **Built.** `Effect.Pin("picture", …)`; `Pinned.size` from the picture's shape, held inside the shapes the
  system takes; the pin keeps its own copy of the picture until it closes. A second manifest entry for a
  picture pin with a larger smallest size, because the smallest size is the opening size (DF). `pin` shows
  it with Unpin.
- **Size:** 2 days.
- **Check first:** 14.
- **Asks for:** manifest: one activity entry. Nothing else.
- **Tests:** device only.
- **Risk:** the system keeps one picture-in-picture window (DF: not checked), so a pinned picture and a
  video cannot both be up; and how readable a picture is at the size the system allows.

### 6.5 To finish, the cheapest, the risk

- **To finish, 2 days.** I would seed `error` here (§11).
- **Cheapest that keeps the promise, about 12 days:** Share only. Ask, the recogniser and the pin are each a
  third of the promise, so only the two other roads can go.
- **Riskiest:** the way in: three roads, two of them not verified. **Cut first:** the picker, unless
  checks 9 and 10 both fail.

---

## 7. M3 · The selection

**Total: about 10 days** (8 to 12).

### 7.1 Prompts in the selection menu

- **How several entries are declared.** One `activity-alias` per entry in the manifest: it points at the
  panel, takes selected text, has its own name and is off. The window switches one on or off with
  `PackageManager.setComponentEnabledSetting`. No permission. The panel sees which one was picked from the
  intent's component.
- **The limit:** §3. Names are fixed when the app is built.
- **The flow.** `OverlayActivity.take` sees the alias, receives the text and goes straight into that
  prompt, asked at once; the model is warmed as the panel is created (about 2 s the first time, DF). When
  the field can be edited, Replace is the first action (today Copy is: `PromptScope.answered`). With piece 2
  several paragraphs can be replaced as well.
- **How the answer goes back, and read-only:** both exist (§2.1).
- **Size:** 2.5 days, plus the list of switches in the window.
- **Check first:** 15, 16, 17. Replace is the whole promise and I found no note that it was tried: 16 comes
  first. If the menu shows one entry per app or folds the rest away: keep the one "Booklight" entry and put
  the two prompts first in its list. That is one key more for the same promise. If Replace does not land in
  some app: Copy is first there.
- **Asks for:** manifest: one alias per entry. Play: nothing.
- **Tests:** device only.
- **Risk:** the order in the menu is the other app's; an app may be slow to notice an entry switched on.

### 7.2 Put the letters back

- **Built.** `Letters.onlyLetters(typed, answer)` in `core/Letters.kt`: it walks both texts. A letter with a
  mark in the answer must stand where one of its plain writings stood (piece 8); every other character must
  be the same character. `abc` and more than one word that is not a letter search asks the model with a
  ready-made prompt and shows the answer only if the check passes. Otherwise: one plain line and the way to
  Gemini.
- **Size:** 2 days.
- **Check first:** 18. A language where fewer than about four of five sentences pass is not offered.
- **Asks for:** nothing.
- **Tests.** Core: about sixty pairs, accepted and refused. This is the one model feature a unit test can
  hold. Device: the fifty sentences.
- **Risk:** the model also mends a comma or a capital, the check refuses, and the row shows nothing too
  often. Whether the check may forgive that is a product call.

### 7.3 Translate into any language

- **Built.** A scope of Booklight's own (`tr`), on M1's hole: the first word is the language (a table in
  core: English and German names, the language's own name, its code), the rest is the text, or the material
  when nothing more is typed. A language that reads badly in the probe is not in the table; `tr` then offers
  Gemini.
- **Size:** 1.5 days. **Check first:** 18. **Asks for:** nothing. **Tests:** core, the table; device, the
  probe.

### 7.4 `formal`, `friendly`, `error`

- **Built.** Seeds, added once (piece 3). `error` carries a mark of its own: no Replace, and one more action,
  "Search the web for its first line".
- **Size:** 1 day; half if `error` came with M2.

### 7.5 To finish, the cheapest, the risk

- **Landing the groundwork under it:** 1 day. **To finish:** 2 days.
- **Cheapest that keeps the promise, about 5 days:** two entries (Fix spelling, Translate), Replace first,
  several paragraphs, and the letters under `abc` for German.
- **Riskiest:** the menu and Replace in the apps you write in. **Cut first:** `tr`'s list down to the
  languages that passed; then `formal` and `friendly`.

---

## 8. The groundwork (the PM's question 9)

- **Is ten days right?** I would plan **12** (10 to 14). LANG §7 sizes its eight items between about 6 and 19
  days. Item 1 is the one that can grow: it moves every word of the date reader. And German is checked on a
  device for the first time here (`docs/PICKING-UP.md`: "German was not checked on the device").
- **Can it run beside M2?** Yes, in three parts:
  - *A, half a day, between M1 and M2 with no other branch open:* the keyword lists, `guide`, `tips` and the
    prompt seeds move to their own file (item 4's move), and the language list is generated (item 7). Every
    feature adds strings; this move should not happen under one.
  - *B, beside M2:* items 1, 2, 3 and 5. They live in `core/` and in three date formats. M2 touches none of
    it, except a few lines of the countdown in `pin/PinActivity.kt`.
  - *C, after M2 is merged:* items 6 and 8. Item 8 changes `Strip`, `Field` and the tip's line in `Panel`;
    M2 changes `Field` (a picture in the chip).
- **Which items must be in before M3?** None, strictly. The letters check needs `Letters.kt`'s tables, which
  2.1 has, not item 3. Piece 8 makes them one table. Item 4's test should be in before M3 adds `tr`,
  `formal` and `friendly`: it is the test that catches a keyword in two places.
- **Where it ships.** Merged before M3 is started, not while it is built. Better still as a quiet release
  of its own to the testers: a mistake in "morgen 9 Uhr" should not hide among M3's features.
- **D13 as "the app's language, English, and German for you":** about 1 day. The reader takes a list of word
  sets after item 1, so a third is one setting. The cost is the collision test, which then runs for every
  pair.

---

## 9. M4 · Your language

**Total: about 17 days** (15 to 19), and the wait for six readers.

| Part | Days | Built |
| --- | --- | --- |
| Six languages to depth b | 11.5 | LANG §2: a word set and its test, about 460 strings, 50 keyword lists, the guide, the tips, 98 settings items, 86 shortcut items, an emoji file from CLDR |
| What M1 to M3 added, six times | 0.5 | About sixty strings, four keywords, three prompts. Piece 3 is why this is small |
| The model, per language | 2 | Five prompts on a fixed text on the Lenovo, the panel in front, with pauses for the allowance. A per-language list says where the device may answer; elsewhere the rows hand over to Gemini. The translate keyword per language (`en` or `de` is an everyday word in each of the six: LANG §3.4) |
| Picture books and what the readers find | 2 | LANG §9 |
| Store pages, release notes for every language | 1 | LANG §6 |

- **Check first:** 19, 20.
- **Asks for:** the language list in the manifest. If suggestions are asked for in the user's language
  (LANG §12.11, not in the frame's list), the language goes to the search engine: one clause in
  `PRIVACY.md` and in the data-safety note. My default: yes.
- **Tests:** LANG §10 as it stands.
- **To test in the probe:** for Danish, Swedish and Norwegian, the ready-made prompts with the instruction in
  English and "keep the text's language". It may read better than an instruction in a language Google lists
  nowhere.
- **Cheapest, about 8 days:** French, Spanish and Italian first (the model is on Google's lists for fixing
  and rewriting in all three: LANG §5). The Nordic three follow as a second release.
- **Riskiest:** keywords nobody would type; the model in the Nordic languages. **Cut first:** depth b down
  to screens only for a language without a reader.

---

## 10. The two other tracks

- **Today's polish** is in the working tree (§2.1). It changes `Strip`, `Rows`, `Glass`, `Bodies`. M1's row
  sits under the field and touches none of them; M1's list uses the new highlight as it is.
- **The window redesign.** M1 adds one switch and M3 one list of switches. Engineering needs only the
  window's page structure to be final before M3. A redesign that lands during M1 costs one line.

---

## 11. The order, and what I would move

1. Today's polish, released.
2. **The device session** (§12), with a throwaway build.
3. **M1,** with the six shared pieces.
4. Groundwork A.
5. **M2,** with groundwork B beside it. Then groundwork C.
6. The groundwork merged and on a device in German; if possible released on its own.
7. **M3.**
8. **M4:** three languages, then three.

**Moves:**
- **`error` from M3 to M2.** A picture of an error is M2's best test (DF read one correctly), and with the
  seed it is one key. It costs half a day there and saves it in M3.
- **The translate hole from M3 to M1.** M1's "Translate knows the direction" already needs it. `tr` in M3 is
  then a keyword and a table.
- **The letter table (piece 8) out of the groundwork's critical path.** M3 no longer waits for the
  groundwork.
- **The photo picker to the end of M2.**
- **If the HP has no model** (check 1): M2 before M3 is right, and D6 becomes a must, because Copy its text
  and Pin are then all of M2 on the HP. M3 could only be judged on the Lenovo.
- **If checks 9 and 10 both fail and 15 and 16 pass:** swap M2 and M3, as D19 allows. Nothing in M3 needs M2
  except `error` on a picture.

---

## 12. The device checks

**The first session, before M1's design is final:** checks 1 to 11, 15 and 16. Half a day, both devices. It
settles M1, the way a picture gets in, and whether M3's promise holds, so the order of the four is known
before the first is built. The other seven come before their own milestone.

Checks 1, 2, 7, 11 and 16 need only 2.1 as released. The rest need a throwaway build with hooks, on a branch,
as DF's did. The device rules of `CLAUDE.md` hold. The first half of check 9 is D5 and is Alex's own.

| # | Check | On | When | Decides |
| --- | --- | --- | --- | --- |
| 1 | 2.1's release build: `fix teh text` | HP | first | Whether any model row exists on the HP: 5.3, 5.4, 6.2, all of M3 |
| 2 | What 2.0 left open: the pin, the shadow, the key | HP | first | `PICKING-UP.md`, "Not done" |
| 3 | Copy, then open the panel 0.3, 1, 2 and 5 s later: is the classification complete? Texts of 50, 399, 401 and 5,000 characters; a picture | both | first | How long the row looks again; what it says for a long text |
| 4 | The "pasted" message: none on the look, one on Tab; where a Googlebook shows it | both | first | M1's first proof |
| 5 | A copy from a real password manager; a copy Booklight made in the panel, the pin, the window | Lenovo | first | The two "never a row" rules |
| 6 | The classifier on the read text: time for 400, 2,000 and 8,000 characters; what it finds in DF's texts, a German and an American address, a phone number | both | first | Rows for long texts; Maps; Call |
| 7 | What answers `tel:` and a Maps address | both | first | Call or Copy number |
| 8 | `detectLanguage`; a translation of 2,000 characters: how long, whole or cut | both | first | 5.3's limits |
| 9 | Capture a region, then Paste. "Copy image" in Chrome: what the description says, can the address be opened | both | first | The clipboard road |
| 10 | The capture's preview: is there Share, is Booklight in it | both | first | The share road for captures |
| 11 | The photo picker: is the newest capture first, and how soon | both | first | The picker road |
| 12 | The recogniser, release build: time for 1,600 × 900 and for a whole screen; small interface text on the HP as it is and enlarged twice; DF's three pictures | both | before M2 | D6 in practice |
| 13 | A dozen pictures with smaller and smaller text, asked of the model | Lenovo | before M2 | The size rule's limit |
| 14 | A pin without today's smallest size: how large it opens, how large it may be dragged, what a pinned video does to it | both | before M2 | 6.4's promise |
| 15 | Where the menu's entries show: Chrome (a page, a text field), Gmail, Docs, Keep, the text editor; by right-click and by touch; three, five and eight entries of one app; their order | both | first | M3's promise, D8 |
| 16 | Replace: does the answer land in the selection, in each of those; several paragraphs | both | first | M3's promise |
| 17 | An entry switched on and off: how soon the menus follow | Lenovo | before M3 | The window's list |
| 18 | Fifty sentences without their letters in German, Danish and French; `tr` into eight languages | Lenovo | before M3 | 7.2 and 7.3, per language |
| 19 | German on a device, on the new structure | both | after the groundwork | The groundwork is done |
| 20 | Per language: five prompts on one text; the Settings app's page names | Lenovo | before M4 | M4 |

---

## 13. Sizes side by side

| | The frame | Mine | Range | Cheapest that keeps the promise |
| --- | --- | --- | --- | --- |
| M1 · The copy | about 5 | 13 | 11 to 15 | 5 |
| M2 · The picture | 5 to 10 | 14 (10 without the recogniser) | 12 to 16 | 12 |
| M3 · The selection | about 5 | 10 | 8 to 12 | 5 |
| The groundwork | 10 | 12 | 10 to 14 | 12: it cannot be cut |
| M4 · Your language | 12.5 | 17, and the readers | 15 to 19 | 8, for three languages |
| **All** | **about 40** | **66** | | **42** |

What makes mine larger: the shared pieces (3.5 days, all in M1), two days at the end of each milestone, the
recorded motion passes, and the model probes in M4.

---

## 14. The PM's ten questions

1. **The HP:** not answerable from a desk. It very likely has the service (AI §3.1); that proves nothing.
   Check 1.
2. **The look's cost; a row that changes:** one call after the first frame, so nothing is added to the
   opening (to be measured against 2.1). The row is not in the list, only its marks change, and the list
   behind Tab is made from the text, not from the description (5.1, 5.2).
3. **The classifier:** not checked. I build on the kinds of things found, not on its ready-made actions, so
   only addresses and the time it takes can change the plan (check 6).
4. **M2's hand-over:** Share first (certain as Android), the clipboard second (not tried), the picker last.
   The panel does not survive the picker; it closes and comes back (6.1).
5. **The recogniser:** no new permission, bundled, sizes in §2.2, keep rules included, 16 KB pages fine. Its
   speed and small text on the HP: check 12.
6. **A picture in the pin:** the smallest size is the opening size; shapes between 1 : 2.39 and 2.39 : 1; a
   pin keeps its shape when you drag it larger, and the system does not reshape an open one for the app (DF,
   `pin/Pinned.kt`). So: its own manifest entry, and check 14.
7. **When the model will not answer:** the one state exists. Piece 1 keeps the reason; the words are UX's.
8. **M3's menu:** an alias per entry, switched by the window; ready-made prompts only; Replace and read-only
   exist; how many show is check 15. The letters check: 7.2.
9. **The groundwork:** §8.
10. **Per milestone:** §13 for the sizes; the riskiest piece and the first cut close §5, §6, §7 and §9.
