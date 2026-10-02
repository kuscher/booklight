# M1 · The copy: interaction review

Seat: interaction designer. Branch `m1-the-copy` at `5f49e0a`. Read against `plan.md` §4, `04-design.md` §3,
`ux-model.md` §15. Code read: `OverlayModel.kt`, `Panel.kt`, `CopyLine.kt`, `Footer.kt`, `scopes/Picks.kt`,
`scopes/Prompts.kt`, `ai/OnDevice.kt`, `device/Clipboard.kt`, `core/Clip.kt`, `core/Languages.kt`,
`core/Matcher.kt`, both `strings_23.xml`. Captures: the 14 light stills, both dark sheets, `shared1/2.png`, and
the films mA, mB, mC stepped frame by frame around each change (sheets in `reviews-m1/work/ux/`).
Nothing was run: findings from code are marked "code", findings seen in a capture name the frame.

## What holds

Every path of the behaviour table was walked. These do what the plan says:

| Path | Result |
| --- | --- |
| Enter on the line | Nothing (`enter()`: no row, `chosen()` is null) |
| Tab, Down, click on the line | `openCopy()`, first press only (`!again`) |
| A letter on the line | Line gone in the same frame (mC 160), stays away |
| Tab before the line has come | Opens the copy (`tabCopy()` looks itself) |
| Esc anywhere | Closes |
| Order after Tab | Link, event, phone, three prompts (02); translation first for the German copy (10) |
| Enter on a model's row | Asked in place, chip stays (mB 228 to 250); a second Enter waits for the whole answer, then copies |
| Held Enter, held Tab, Ctrl + digit | Never ask the model, never run twice |
| Backspace, one press each | Answer to rows (mB 436, pill back on "In English"), rows to line (mB 506) |
| Private copy | No line; `clip` says "What you copied is marked private" (14) |
| Booklight's own copy | No line (label, or within 1.5 s of its own copy) |
| Shared text | Same list, same answer in place (`shared1.png`, `shared2.png`) |

Footer in each state, read from the captures. All match the plan's tables.

| State | Footer |
| --- | --- |
| The line (01, 08, 09) | none |
| Rows, link or phone selected (02, 04) | `tab` Actions · `esc` Close |
| Event row (03, mA 256) | `tab` Fill in · `esc` Close |
| Thinking (mB 234 to 238) | `⌫` Leave · `esc` Close |
| Answered (07, 11, 12, 13) | `tab` Actions · `esc` Close |
| Private (14) | `⌫` Leave · `esc` Close |

## Findings

### 1. Must fix · The model's rows say "Gemini" and hand over for the first half second

Where: `TextScope.here()` and `model()` in `scopes/Picks.kt`; `OverlayModel.entered()`.

What: the device's model is only asked about once the copy has become the chip (`entered()` calls
`onDevice.check()`), and on the HP that check includes a trial question. Until it returns, the state is
UNKNOWN, `here()` is false, and every model's row is built with the label "Gemini" and one action,
"Ask Gemini". Film mA: the rows land at frame 170 and read "Gemini" three times until frame 202; in frame 203
all three read "On this device", changed in one frame (a pop of three labels). That is 33 frames, 0.55 s; the
trial may take up to 4 s (`PROBE_MS`). It happens on the first Tab of every process, and Booklight keeps
nothing in the background, so that is often.

Two things are wrong. The label says the wrong thing and then changes under the eye. And Enter in that window
runs "Ask Gemini": for a German copy row one is "In English", so "the key, Tab, Enter" between 0.35 s (the
guard after Tab) and 0.55 s leaves Booklight for the Gemini app. The story of M1 is exactly that sequence.

Fix, three parts:
1. `OverlayModel.offerCopy()`: when it has an offer, `scope.launch { app.onDevice.check() }`. The panel is in
   front, so the system answers. The state is then known before a hand reaches Tab.
2. `TextScope.here()`: count UNKNOWN as here:
   `ai?.state?.value.let { it == OnDevice.State.READY || it == OnDevice.State.UNKNOWN } && full.length <= most`.
   Same in `TextScope.instruction()` (it uses `here()`).
3. `OverlayModel.stream()`: at the top of the job, if the state is UNKNOWN, set `thinking = true`, await
   `app.onDevice.check()`, and if it is not READY `put(s.unanswered(row))` and return. Enter then never hands
   over unasked: the worst case is "No answer on this device this time" with "Ask Gemini ⏎".

### 2. Should fix · A held Backspace runs every step back, and drops shared text

Where: `Panel.kt`, `keys()`: `Key.Backspace -> field.text.isEmpty() && model.back()`. Code.

What: there is no `again` guard. `04-design.md` §3.4 M1-f says "one step each press". Hold Backspace to clear
a typed instruction ("pull out the tasks as a list"): when the field empties, the next repeat (about 30 ms
later) leaves the chip. For a copy that is the line again. For text another app handed over,
`leaveScope()` calls `scopes.forget()`: the text is gone and there is no way back to it.

Fix: `Key.Backspace -> field.text.isEmpty() && (again || model.back())`. A repeat on the empty field is
swallowed; a new press steps back.

### 3. Should fix · After typing, Tab on the empty field no longer opens the copy

Where: `OverlayModel.tabCopy()` and `offerCopy()`. Code.

What: `tabCopy()` goes through `offerCopy()`, which refuses once `typedYet` or `copyGone` is set.
`04-design.md` §3.5 says: "Once put away by typing, the line stays away... Tab on the empty field still opens
the copy." As built: type `c`, delete it, Tab does nothing. Worse: Tab, Down, Enter on the event row moves
into the Event scope; from there the copy's other rows (the phone number, the translation) cannot be reached
again in this opening except by typing `clip`.

Fix: `tabCopy()` looks for itself and leaves `offerCopy()` as it is (the line still stays away):
```
fun tabCopy(): Boolean {
    if (copy == null) copy = Clipboard.look(app)?.let { Clip.offer(it, settings.copyRow) } ?: return false
    return openCopy()
}
```
Could, on top: `into()` remembers a `TextScope` it came from, and Backspace on the empty field of the scope
entered from it goes back to that chip instead of turning into the word "event".

### 4. Should fix · Two letters of a row's name are taken for a language

Where: `TextScope.rows()` in `scopes/Picks.kt`: `if (into != null) return listOf(into) + hits…`. Code.

What: `Languages.find` takes a tag of two letters. Under the chip, typing "fix" shows row one as "Fix
spelling" (f), then "In Finnish" (fi), then "Fix spelling" (fix): row one changes twice under the selection,
and "fi", Enter translates into Finnish. Same for "no" (Norwegian above Note). Two rules of §15 contradict:
"typing narrows the rows by their names" and "a language's tag is the translation".

Fix: keep the scores, and let a tag give way to a name that starts with it:
```
val scored = named.map { (n, r) -> Matcher.score(filter, n) to r }.filter { it.first >= Matcher.INSIDE }.sortedByDescending { it.first }
val hits = scored.map { it.second }
val nameFirst = filter.length < 4 && (scored.firstOrNull()?.first ?: 0.0) >= Matcher.PREFIX
if (into != null) return if (nameFirst) hits.filter { it.id != into.id } + into else listOf(into) + hits.filter { it.id != into.id }
```
"en", "de", "da" still give the language (no row's name starts with them).

### 5. Should fix · "translate" and "in danish" do not find the translation

Where: `TextScope.rows()`, the `named` list. Code.

What: Alex's words were "hit tab and have translate options". After Tab, typing `translate` matches no row
(the row is called "In German"), so it becomes an instruction: the model is sent the bare word "translate"
with no language. Typing what the row itself says, `in danish` (or `to danish`, `auf dänisch`), is not a
language for `Languages.find` either and also becomes an instruction.

Fix: (a) name the translation by its verb too: in both places it is added to `named`, use
`"${translation.title} ${text(R.string.tr_name)}"`. (b) Before `Languages.find(filter)`, if the filter has
two or three words and every word before the last is in a new resource `tr_lead` (en: `in,into,to,translate`;
de: `auf,ins,in,nach,übersetze,übersetzen`), look up the last word.

### 6. Should fix · `tr` with a language it does not know is a dead end

Where: `TranslateScope.rows()`: `Languages.leading(arg) ?: return plain(R.string.tr_which)`. Code.

What: the plan (§4, item 4) says a language outside the table "hands over to Gemini". As built
`tr thai see you on Saturday` shows "Translate · Which language?" with no action, whatever follows. The
table has 25 languages, while the strings promise more: `tr_about` "Into any language, on this device", the
guide line "Into any language", the tip "Any language works".

Fix: when `leading()` is null and two words or more are typed, return one row: title `tr_name`, subtitle the
typed text, label "Gemini", action "Ask Gemini ⏎" with `Effect.AskGemini("Translate: $arg")`. One word alone
keeps "Which language?". And make the three strings true: "Into 25 languages on this device, others through
Gemini" (de: „In 25 Sprachen auf diesem Gerät, weitere über Gemini“).

### 7. Should fix · The same translation has two limits

Where: `TranslateScope.rows()`: `full.length <= PromptScope.MAX` (8,000). `TextScope`: `HERE` = 1,800. Code.

What: §15 says a translation of up to about 1,800 characters is answered in the row and a longer one hands
over, because a cut answer looks whole. `tr danish` with nothing typed takes the same copy at up to 8,000
characters and answers in the row, cut at 768 tokens. Two rules for one thing.

Fix: move `HERE` out of `TextScope`'s private companion (for example `Answering.HERE = 1800`) and use it in
`TranslateScope.rows()`. The same holds for a rewrite typed by its keyword (`PromptScope.MAX`): 1,800 for
every prompt but `p-sum`.

### 8. Could

- **Backspace is named nowhere on an answer.** Footer in 07, 11, 12: "tab Actions". The step back is the one
  new key of M1 after Tab. And "Leave" while thinking (mB 234) does not leave, it goes back to the rows. Fix:
  `OverlayModel`: `val stepped: Boolean get() = asked != null`; `Footer.kt`, first in `first`'s `when` after
  `confirming`: `model.stepped && model.query.isEmpty() -> "⌫" to stringResource(R.string.hint_back)`
  ("Back" / „Zurück“).
- **The age is said twice.** mB 150: "Copied just now"; back at mB 520: "Copied 10 s ago". §15: "The age is
  said once, as it was when the panel opened." And a copy that passes two minutes while its rows are read gets
  no line on the way back. Fix: `openCopy()` keeps the `Offer` it opened from; `leaveScope()` puts that one
  back instead of calling `offerCopy(back = true)`.
- **A link without a tracking tail has no Copy.** `thing()`, `Thing.LINK`: the second action exists only when
  `Links.clean(url)` is not null. Phone and mail rows always have Copy; `04-design.md` §3.2 says "otherwise
  Copy". Fix: always add it: label `action_copy_clean` when cleaned, else `action_copy_link`;
  `Effect.CopyText(Links.clean(url) ?: url)`.
- **Enter within 350 ms of Tab is dropped.** `openCopy()` sets `filledAt`. The rows are readable 12 frames
  (0.2 s) after Tab (mA 164 to 176). A practised "Tab, Enter" under 0.35 s needs a third press, against "four
  presses". Keep the guard (a queued Enter could open a link nobody saw), but 200 ms is enough here.
- **`tr danish`, half a second's pause: the copy is read and translated unasked,** in the middle of typing
  `tr danish see you…`, and the row does not say the text is the copy (a prompt's row says "From the
  clipboard"). Fix: in `TranslateScope.rows()` the caption for the clipboard's text is
  `"$into · ${prompt_clip}"`, and the copy is asked on Enter only.
- **The web row under `tr`** (13): "Search Google for “tr danish see you on Saturday”" is one Down, Enter from
  sending the text to Google with the keyword in it. Plan screen 4 has no such row. Fix: `TranslateScope`:
  `override val web = false`.
- **German.** „Kopiert gerade eben · Text“ is stiff. „Gerade kopiert“ needs the age first: `copy_line` de
  `%1$s kopiert` with ages „Gerade eben“, „Vor %d s“, „Vor 1 min“.
- **For Alex to decide:** with the switch "What you copied" off, Tab on the empty field does nothing either
  (`Clip.offer(…, on = false)`). Plan decision 2 says that without the line "Tab there opens the copy".

## Verdict

approved with the must-fixes
