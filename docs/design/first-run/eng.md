# First run: the build plan (tech lead, second pass)

*4 October 2026. The plan for `design.md` (stage (b), 252 and 324 dp) and for `motion.md`, which arrived while
this was being written. From the code on `first-run`: nothing built or run, no device touched.*

**Changed at night, after Alex had seen the opening: section 13 is new** (a welcome in a tall glass, an example
flight in the opening). The total is about 30 days, not 22 (corrected in sections 9 and 12.5).

**Changed in the evening, after Alex had seen the plan: section 12 is new.** The total was then 22 days, not 16
(corrected in section 9). Section 12 overrules these earlier lines: §3, the three-openings rule (it is per run
now); §4, the suggested key (Quick Insert where the keyboard has it); §5, what a lesson's Enter does (the panel
stays); §6, the model's status (there is no such row: `look()` and the privacy sentence are out); §10, check 6;
§11, decision 4 (answered).

Marks: **[C]** read in the code today (file · function). **[D]** seen on the Lenovo Googlebook with the stand-in
window (`shortcut-setup.md` §2, `design.md` §4). **[I]** inferred: a conclusion, or Android as I remember its
source. Days are my estimates unless said. Paths are under `app/src/main/java/io/github/kuscher/booklight/`
unless they start with `core/`.

## 1. The shape

- **Core decides, the app draws.** New `core/…/FirstRun.kt`: the steps (`KEY, OPEN, SEARCH, SUM, ASK, CHOICES`),
  the screens (`K1…K4, L2…L4, Q, C`), and pure functions from what is stored to the screen, the counter and what
  an answer changes. JUnit; the app module has no tests [C: `app/src` holds `main` and `debug`].
- **The stage takes the card's place.** `OverlayModel.card` [C: derived; null once something is typed or a chip
  is set] becomes `stage: FirstRun.Screen?`, derived the same way. K, L and Q are one body of `Panel`, `"first"`,
  drawn by a new `overlay/FirstRunBody.kt`. `Card`, `CardBody`, `cardChoice`, `OverlayActivity.card` go.
- **`Under.choose` does not change** [C: core `Zero.kt`; a card is first]. Its `card` argument is passed "a
  first-run screen is due". While one stands no copy's line, usual row or tip comes, as with a card today [C:
  `offerCopy`, `offerZero`, `offerTip` refuse while `card != null`].
- **C is a real list**: three `Result`s, `selected = -1`, as the usual rows stand at rest [C: `offerZero`].
  `Metrics.height` sums it as any list [C]: 68 + 8 + 92 + 56 + 56 + 8 + 36 = 324.

## 2. Files and functions

| File | Added or changed | Why |
| --- | --- | --- |
| `core/…/FirstRun.kt`, `FirstRunTest.kt` (new) | Section 8 | The part that must not go wrong |
| `core/…/Model.kt` | `Body.Switch(on, word)` | C's first row |
| `data/Prefs.kt` | Five fields, `SCHEMA = 7`, a step in `migrate`, `load()` for a new install | Section 3 |
| `overlay/OverlayModel.kt` | `stage`, `firstTab`, `firstEnter`, `keyLanded`, `offerChoices`, `coach`, `firstHint`, `lap`; a lesson marked in `used()`; "First steps" and the switch caught in `enter()` and `runRow()` | Sections 4 to 7 |
| `overlay/OverlayActivity.kt` | `hold`, `onProvideKeyboardShortcuts`, `onTopResumedActivityChanged`; `onNewIntent`, `byKey`, `onCreate`, `onWindowFocusChanged` | Section 4 |
| `overlay/FirstRunBody.kt` (new) | `FirstBody` (seat, counter, band, answers), `BigCap`, `YourKey`, `Recipe`, `QuestionBody` | `design.md` §3 to §6 |
| `overlay/SystemWords.kt` (new) | Two strings of the system UI, read off the main thread | Section 4 |
| `overlay/Panel.kt` | The body `"first"`; Enter and Tab on it; `lap()` with a trigger; Q's lines measured | |
| `overlay/Metrics.kt` | `height(m)` for a stage (184 or 256 dp under the field); `rowHeight` 92 for `Body.Switch` | It must match what `Panel` draws |
| `overlay/Footer.kt`, `Field.kt` | The coach line; C's Labs line and `⏎` Done. The placeholder asks `model.firstHint` first [C] | Section 5 |
| `overlay/Rows.kt`, `Bodies.kt` | `PanelSwitch` (44 × 24); a `Body.Switch` row shows it and no strip | Section 6 |
| `overlay/Motion.kt`; `ai/OnDevice.kt` | Section 7; `look()` | |
| `Strip.kt`, `Marks.kt`, `Glass.kt`, `Tips.kt` | None: `OptionStrip(lit = false)`, `DrawnCheck(stroke =)`, the shader's `run` and `glow` exist [C] | |
| `providers/Providers.kt` · `CommandsProvider` | "First steps" (`Effect.Internal("first")`), beside "Booklight settings" [C] | The replay |
| `Guide.kt`, `BooklightApp.kt` | A `guide` line; `suspend fun firstExample()` | Section 5 |
| `window/Pages.kt` · `StartPage` | The row "First steps"; Action + J where Action + K is drawn [C]; the key's two texts | `shortcut-setup.md` §3 |
| `window/MainActivity.kt` | `onProvideKeyboardShortcuts`: the same five rows first, when the Start page asked | The other way to the key |
| `AndroidManifest.xml` | `<package android:name="com.android.systemui" />` in `<queries>` | Section 4 |
| The rest | About 60 strings, English and German; `DebugReceiver.kt`, `bl`; `CLAUDE.md`, `ux-model.md`, `design-system.md`, `PRIVACY.md`, `README.md`, `data-safety.md`, `PICKING-UP.md`, `CHANGELOG.md` | |

## 3. State and storage

In `Settings` [C: `data/Prefs.kt`], each with the default a file from before must decode to:

| Field | Default | Means |
| --- | --- | --- |
| `first: Int` | 0 | 0 no run · 1 a new install's · 2 an update's (the key only) · 3 a replay |
| `firstDone: List<String>` | empty | Steps that are over: done, skipped or answered |
| `firstShown: Map<String, Int>` | empty | In how many openings each step's screen came |
| `firstHelper: Int` | 0 | How often the helper came for step 1: 0 K1, 1 K2, more K3 |
| `firstIcon: Boolean` | false | The icon's first click has shown the panel |

- **A new install** has no file; `load()` makes `Settings(schema = SCHEMA)` [C]. There, and only there,
  `first = 1`.
- **An update is any install whose file is there**, a restore too. `migrate` [C] gains
  `if (s.schema < 7) first = FirstRun.forUpdate(s.keySeen, "first:key" in s.tipsSeen)`: 2 without a key, else 0.
  An unreadable file [C: `load`, the `catch`] starts the same way.
- **The same answer twice.** An older build drops the five fields and writes schema 6 back [C:
  `ignoreUnknownKeys`; `update` writes the whole file]; this build then migrates again. `keySeen` survives the
  trip, so "has a key: nothing" repeats. "Said Not now to step 1" would not. So when step 1 ends, however it
  ends, the mark `first:key` is added to `tipsSeen`: a list every build with tips keeps whole and reads only for
  ids it knows [C: `Tips.next`, `Tips.seen`]. Only "Show the tips again" in the older build loses it [C:
  `StartPage`]. A run half done comes back as no run.
- **Replay** sets `first = 3` and empties `firstDone`, `firstShown`, `firstHelper`. It touches no switch, not
  `keySeen`, not `firstIcon`.
- **Three openings.** `firstShown[step]` goes up when the step's screen comes at the gate, once per panel. At
  the next panel's creation `FirstRun.due` gives up a step shown three times: a lesson is skipped; Q is over,
  `suggestions` untouched; step 1 without a key ends the run; K4 goes on.
- **Backup**: the fields travel in `settings.json` [C: `backup_rules.xml`, `data_extraction_rules.xml`]. A
  restored device starts no run unless the backup was in the middle of one.

## 4. Step 1 in the code

**The hold.** `private var hold` in `OverlayActivity`, in release builds too (`stay` is debug-only [C]). Enter on
"Open Keyboard shortcuts" sets it and calls `requestShowKeyboardShortcuts()`, with no `close()`.
`onWindowFocusChanged` returns early for `hold` as for `stay` [C]. The lost focus says the helper came: there
`firstHelper + 1` is stored and the stage turns to K2 or K3 (`motion.md` T2).

| Ends it | Signal | Then |
| --- | --- | --- |
| The helper is closed | `onWindowFocusChanged(true)` [D: the focus comes straight back] | The stage stands; a lost focus closes again, as every day |
| The key | `onNewIntent` | Below |
| Another app comes to the front | `onTopResumedActivityChanged(false)` [I: the helper, a dialog, does not cause it] | `close()` |
| A lock, a new desk | `onStop` [C], unchanged | Finish |
| The helper never comes | No lost focus 1 s after asking | `hold = false` |

Not seen on any device: the real panel (blur, `singleInstance`) under the helper, and whether the helper's
arrival causes `onStop` or the third signal. Milestone 0 answers that first.

**The rows.** `onProvideKeyboardShortcuts` on the panel adds one group, only while `hold`: the label
`first_helper_title` and five `KeyboardShortcutInfo(label, KEYCODE_J, META_META_ON)` [D: drawn as five]. The
fallback is one constant: `KEYCODE_1 + i`, no modifier, no number in the label [D].

**The system's own words.** `SystemWords.read()` is
`packageManager.getResourcesForApplication(info, a configuration with LocaleManager.systemLocales)`, then
`getIdentifier("shortcut_helper_customize_button_text", "string", "com.android.systemui")`, and the same for
`…_customize_dialog_set_shortcut_button_label`. The system's language, not the app's own [C:
`locales_config.xml`].

- **I believe an ordinary app may read them**, about four to one: it is the public call launchers use for other
  apps' labels, and it needs no permission, only a visible package. The `<queries>` line makes sure of that
  (platform-signed packages are visible anyway [I]). The names are on the device [D], but read from outside an
  app.
- **What can fail**: the package not found; a name gone or collapsed in another build; an empty or overlong
  value; an overlay this read misses. Each gives the fallback (`first_sys_customize`, `first_sys_set`), which
  misquotes in a third system language. Tens of ms [I]: off the main thread.
- **A device confirms it** with `./bl debug first words` (the receiver runs inside the app): the system in
  English, then German, against the helper's buttons.

**`onNewIntent`**, in this order:

1. `val key = byKey(intent)`, at once: inside this call the referrer is the new start's [D]; afterwards the
   first start's again [I].
2. `key && !keySeen`: `keySeen = true`. Today only `onCreate` does that [C].
3. From the icon: the window, as built [C].
4. `key` while the stage awaits it (K2, K3; K1 without a key): `keyLanded()` sets K4 with the cap down and calls
   `dismissKeyboardShortcutsHelper()` (the system closes it itself [D]). Never `close()`.
5. `hold`: return. Else as built: `turn`, an example, text, or the toggle [C].

A panel made by the key needs nothing new: `onCreate` sets `keySeen` [C], so the stage is K4 at the gate.

**`byKey`.** Today it only rules out [C]: any launcher start not from the store, the installers, Settings, adb
or Booklight is a key, so another app that starts Booklight would be told "Your key works". **Recommended: the
positive sign**: a key is the assistant's action, or a launcher start whose referrer is `android` or missing
(`FirstRun.byKey(host)`). What else carries `android` [I]: whatever the system starts itself, such as the
touchpad's three-finger tap set to open an app; each is a way the user chose. The cost: one device was seen [D],
and a build that starts the key's launch as someone else would never be believed. So it is checked on every
Googlebook within reach; `./bl debug first` prints the last start's referrer.

**The icon.** In `onCreate` a start from the icon [C: `fromIcon`] hands over, unless `first == 1`, step 1 is
open and `firstIcon` is false: then `firstIcon = true` and the panel stays. **K2 against K3** is
`FirstRun.screen`: `firstHelper` 1, or more.

## 5. The lessons in the code

- **Done.** `OverlayActivity.run` calls `model.used(r, a)` once the executor has said yes [C], and `Guide.used`
  names the line: `apps`, `places`, `appsearch`, `sums`, `settings` [C]. There `FirstRun.ran(state, line,
  effect)` marks OPEN (an app's line with `Effect.LaunchApp`), SEARCH (`appsearch`; `settings` where no app can
  be searched) or SUM (`sums` with `Effect.CopyText`). Decided before the effect, written in the same call
  before `close()`: no frame and no focus change comes between. Not written first: an app that failed to open
  must not count. A lesson's thing counts whichever lesson is on screen.
- **Next.** `Prefs.update` changes the state at once and writes the file later [C]; the next panel derives its
  stage from it, and Skip changes it in the open panel.
- **The example.** `BooklightApp.firstExample()` waits for both lists as `usual()` does [C], takes
  `Guide.appSearch()` [C] and asks the engine for the shortest start of the name, three or four letters, that
  puts the app first; else Settings. Not stored: the same apps give the same pick. If it is late the lesson
  does not come in that opening, and the opening is not counted.
- **The coach line** is a third source for the footer's left seat, after the feedback word [C: `Footer`]:
  counter, one `Keycap`, words, `fadeEnd`, a polite live region. `OverlayModel.coach` maps the selected row, the
  armed action and the chip to six cases; `FirstRun.coach` picks the words. **0.75 day, not 0.5**: the drawing
  is half a day, the mapping and its tests a quarter.
- **The placeholder** is `model.firstHint`: one per lesson, one each for the ending and for "Not now". Under the
  app's chip the chip's own wins [C: `OverlayModel.hint`].

## 6. The question and the choices

- **Nothing armed**: `OptionStrip(lit = false)` and the `tab` cap, as a tip has them [C: `TipBody`].
- **Consent is written in one place**: `OverlayModel.firstEnter()` on Q, from a new press of Enter with Agree
  armed or a click on Agree. One `update` sets `suggestions = true` and adds ASK. In `FirstRun.answer` only
  `AGREE` returns `suggestions = true`; every other way out leaves it untouched, and a test says so.
- **The old suggestions card goes.** It armed "Turn on" [C: `Panel.keys`, `cardChoice == 0`].
- **The switch** is `Body.Switch`, drawn by a new `PanelSwitch` after design system §3d. Enter is caught in
  `OverlayModel.enter()`, as "Don't suggest" is [C], and flips `zero`; the rows come from the next opening [C:
  their job starts at creation].
- **The model's status**: `OnDevice.look()` is `checkStatus()` alone. `check()` asks a "downloadable" model
  "Say OK." [C: `answers`]; `look()` never does. It honours `pretend`, and sets the state only while it is
  UNKNOWN, so that "Get" works [C: `download` wants DOWNLOADABLE]. C takes what is known when it is drawn. A
  device that calls a model it has "downloadable" [C: the comment in `checked`] is offered "Get". `move()`
  passes a row without an action.
- **The promise.** `PRIVACY.md` says "if you never use a prompt, the model is never asked" [C], and that holds.
  But `look()` makes the library's client for everyone who reaches C, and what the library reports then [I] is
  new for them. One sentence more there.

## 7. Motion hooks

| Hook | Where | Note |
| --- | --- | --- |
| `BigCap(label, pressed: () -> Float)` | `FirstRunBody.kt` | 56 dp; fill as `Keycap`, ring as the chip [C]. Drawn only: 0.96 to 1 on `pop`, + 0.08 ink. A cap that arrives is the "up" half |
| `YourKey` | The same | A blank cap of the suggestion's measured width; `DrawnCheck(shown, ink, 28 dp, 2.5 dp)`, 140 ms as built [C] |
| The landing | `OverlayModel.landed` | Down in `keyLanded()`; up after `KEY_HELD_MS` (T3) or `KEY_TAP_MS` (T4); the check 60 ms on, the lap 240. If the helper's exit varies, T3 counts from the focus coming back |
| One lap | `Panel`: `lap()` out of `LaunchedEffect(gate, leaving)` [C]; `LaunchedEffect(model.lap)` | T3, T4, T10: that opening's one lap. Not put out by the change that asked for it [C: the collector fades a lap when `results` change] |
| Set-down, the written recipe | `FirstBody` | G and B from `opened()` [C: `Panel`]; parts as `TipBody.part(i)` [C]; letters at `typeStep` [C]; whole when `firstShown` is not 0 |

`Motion.kt` gains `beat` (66 ms), `KEY_HELD_MS` (400), `KEY_TAP_MS` (160) and `held(ms)`: stretched by `slow`
and 0 with animations off, as `stagger` is [C]. (`hold()` keeps its time then [C]; `motion.md` §5 wants these
gone.) No new spring.

## 8. Tests, hooks, races, everyone else

**`FirstRun`**: `forUpdate`, `due`, `screen`, `count`, `shown`, `keyLanded`, `answer`, `ran`, `byKey`, `coach`,
`fits`.

| # | `FirstRunTest` | Expect |
| --- | --- | --- |
| 1 | `forUpdate`: a key · none · none and the mark | 0 · 2 · 0 |
| 2 | Not now on K1, the new fields dropped, `forUpdate` again | 0 |
| 3 | New: helper 0, 1, 2; the key; Go on; each lesson run or skipped | K1, K2, K3; K4; L2, L3, L4, Q; counts 1 to 5 of 5 |
| 4 | An update; Not now on any K | K screens only, no counter; no run, the mark |
| 5 | `answer`, every way out of Q | Only AGREE gives `suggestions = true` |
| 6 | Shown three times: a lesson · Q · K1 · K4 | Skipped · over, untouched · no run · L2 |
| 7 | `ran`: a sum while L2 stands · "App info" | SUM done · nothing |
| 8 | Replay with a key: suggestions on · off | K4, lessons, C · and Q |
| 9 | `byKey`: `android`, none, the store, another app | Yes, yes, no, no |
| 10 | `coach`, six cases; `fits` at 474 and 475 dp | `design.md` §5; no, yes |

**Debug hooks**

| `./bl debug …` | Does |
| --- | --- |
| `first` | The run, done, shown, the next screen and count, hold, the last start's referrer, the example |
| `first new\|update\|replay\|off` | The stored state of a fresh install, of 3.0 without a key, of a replay, of none |
| `first k1…k4\|open\|search\|sum\|ask\|choices` | That screen, in the open panel or the next: no fresh install needed |
| `first helper\|back\|press` | The helper with the hold; the helper closed; the key's start (`./bl open key` [C] is the real path) |
| `first words\|rows\|lap\|trace N` | What the system UI gave and how long it took; the five labels; one lap; N frames logged (height, the key's scale, the check, the lap), as `pill N` does [C] |
| `ai none\|downloadable\|downloading\|ready` [C]; `dump` | C's row in each state; `first=K2 2/5 armed=0 hold coach='…'` |

**Races**

| # | Race | Cause in the code | Answer |
| --- | --- | --- | --- |
| 1 | The key's start and the focus come in either order | Two messages of the system | K4 is set in `onNewIntent`; the cap comes up by its own clock |
| 2 | A held key puts the panel away at the reveal | Each repeat is a new start (`motion.md` §4); a start with the panel open is the toggle [C: `onNewIntent`] | Until the check is whole a second start does nothing |
| 3 | An early focus blip closes the panel under the helper | `onWindowFocusChanged` posts a close 250 ms on [C] | The posted close asks `hold` too |
| 4 | Enter too early, or twice | The stage is state at once and drawn later; the model's `settings` is a copy a collector feeds [C: `init`] and may lag [I] | Enter counts from the answers' first frame; stage changes read `prefs.now` |
| 5 | The key while the panel is leaving | `turn()` returns before the rest [C] | `byKey` is read first |
| 6 | The example, or the model's status, is late | Lists read by jobs; a call to the system | The screen waits or does without; nothing arrives late |
| 7 | "First steps" reaches the executor | An unknown `Internal` returns false; the footer says it failed [C] | Caught in `enter()` |
| 8 | The height is wrong for a frame | `Metrics.height` is asked before layout, and gives 68 between a letter and its list [C] | Q's lines are measured first; the stage's height is kept until the list lands |

**People who never see first run** (`first == 0`): no job, no look and no helper row runs for them. What changes:

1. The two old cards are gone: whoever never answered "Search suggestions are off" is no longer asked.
2. `keySeen` is also set by the key pressed with the panel open; with the positive sign, no longer by another
   app's start.
3. The Start page suggests Action + J and has the row "First steps"; the panel has that command; the window's
   page in the helper has five rows more. Schema 7. One `<queries>` line.

## 9. Order of work

| M | What | On a device at its end | Days |
| --- | --- | --- | --- |
| 0 | Proof: the real panel with a hold and throwaway rows; the referrer; the system's words; `look()` | The helper opens on the panel's page and the key drops it; the log says who started the panel, the two words, the status | 0.75 |
| 1 | `FirstRun` and its tests; fields, schema 7, migration; the state hooks | `./bl test`; `first k1…` prints each screen | 1.5 |
| 2 | The stage as stills: skeleton, caps, K and L screens, `Metrics`, Tab and Enter; the old cards out | Every K and L screen, both themes, 4x captures | 2.5 |
| 3 | Step 1 for real | A whole step 1 with the real helper | 2 |
| 4 | Lessons: example, placeholders, done, Skip, three openings, coach line | The key, K4, three things run for real | 2 |
| 5 | Q, C, the fold; `look()`; the switch; `PRIVACY.md` | A whole run to the bare field | 2 |
| 6 | Motion: `motion.md`'s ten transitions, its interruptions, `trace`, filming | Recordings stepped against its §7 | 2.5 |
| 7 | Replay, the window, Action + J everywhere, German, the screen reader's words, documents | "First steps" typed; the Start page | 1.25 |
| 8 | Device pass (section 10), the HP with Alex, fixes | | 1.5 |
| | **In all**, first round | | **16** |
| | In all, after the second round (the revised order is in section 12) | | 22 |
| | **In all, after the third round** (section 13) | | **29.75** |

All are mine; the coach line was the designer's 0.5 and is 0.75 here. M1, M2 and M7 are sure to a quarter day.
M3 is two days if milestone 0 goes as the stand-in did, four if the hold needs another signal. M6 is the least
sure: `motion.md` calls every number "a starting value until it has been filmed". Fairly: 14 to 20 days for the
first round's 16; 19 to 27 for the 22; 25 to 38 for the 29.75.

**To halve it** (the first round's 16 to about 8 days; what to cut first now is in section 12), in this order:

| Cut | Saves | Loses |
| --- | --- | --- |
| The model's row | 0.5 | One status line; no new privacy sentence |
| The system's words read; the window's rows; the icon's first click | 0.9 | Exact quotes in a third language; the Apps list leads to the window |
| The lessons become the tips that exist | 3.25 | "By doing": Booklight types the example. No counter. Opening an app has no tip [C: `tips`] |
| C: after Q, the fold with the `?` placeholder | 1 | "Your usual" stays in the window |
| Motion by its own list (`motion.md` §6, all six) | 1.25 | The written recipe and both laps. The key held down and the check stay |
| Their share of the device pass | 0.75 | |

What is left is step 1 whole, with its reveal, and the question.

## 10. Device checks before it is called done

**Answered by the stand-in [D]:** the panel's place lies wholly behind the helper; five rows with the same keys
are drawn as five, and digit caps too; labels wrap, English on one or two lines, German on two or three; the
helper's X and Esc give the focus back; the new key closes the helper and a launcher start arrives; keys that
are no shortcut yet do not reach the app. **Open:**

1. The real panel under the helper (M0), both Googlebooks: alive, with its page? Does the helper's arrival cause
   `onStop` or `onTopResumedActivityChanged`?
2. Under the hold: a click beside the helper, another window, another app's shortcut, the lock. Does the panel
   end each time? After X or Esc, does the field take the next letter?
3. The referrer of the key, the three-finger tap, "Open" in the store and the installer, the icon, the widget,
   the tile, the assistant key, on every Googlebook within reach. Does a held key repeat its start?
4. The reveal, filmed: the helper's exit in frames; the panel's first frame: K4 with the key down, or K2?
5. `first words`: read at all, how long, in English and German, equal to the buttons; with Booklight in another
   language than the system.
6. `look()`: its time; on the HP, "downloadable" for a model that is there?
7. A fresh install: "Open" in the store, the icon once, the icon again. Does the launcher's closing close the
   panel?
8. 3.0 with a key and without, updated to this build; the trip through 3.0 and back.
9. 252 and 324 dp growing at `slow=4`; the HP's first opening: a blurred rectangle round the glass?
10. A 56 dp cap, "your key" blank, and the switch, on glass over a white and a dark window, both themes.
11. German: L3's longest recipe beside „Überspringen“; Q with the longest engine name; the coach line beside
    „tab Aktionen“ and „esc Schließen“; an umlaut and a dead key typed in a lesson.
12. Every screen with the system's animations off; the screen reader through a whole run.
13. A release build from the first screen to the bare field, with a stopwatch (`pm.md` §2).

Dropped with the design's choice of stage: the larger glass and the dim (first pass, checks 7 and 8).

## 11. Decisions for Alex

| # | Decision | Recommended | If you say the other thing |
| --- | --- | --- | --- |
| 1 | What counts as the key | A start by the system itself, checked on each Googlebook | Today's rule: it survives a system that changes who starts the key's launch; another app starting Booklight is told "Your key works" |
| 2 | The system's own words | Read them: one `<queries>` line, no permission | Fixed words in English and German: misquotes in any other system language; 0.4 day less |
| 3 | The old suggestions card, for updates | Gone; the switch is in the window | Q once for whoever never answered it: a quarter day, and an update that asks something |
| 4 | The model's status on the last screen | Asked of the system, the model nothing; one sentence in `PRIVACY.md`. **Answered in the evening: no row** | No row: half a day less, no word about the model |
| 5 | A lesson's thing run out of turn | It counts | Only the lesson on screen can be done: a sum copied during step 2 is taught again |
| 6 | A proof on a device first (M0) | Yes, before any layout | Straight to the screens: the hold's risk is met in week two |

## 12. Second round (4 October, evening)

*Alex, to the first plan: the suggested key is Action + Quick Insert; a beginning that wows; "Re 5, the other
thing" (the panel stays open through the lessons); "Re 11 no row" (the model). His numbers are his page's, not
section 11's. Marks as above, and two more. **[J]**: read in the API 37 `android.jar` on this Mac, which says
what is public. **[R]**: from this repo's research notes, seen on a device earlier, as they say. [D] now
includes two results of this evening on the Lenovo Googlebook with the stand-in: the launch test below, and the
Quick Insert key (`shortcut-setup.md` §3).*

### 12.1 The panel stays through the lessons

**Seen [D]:** a see-through `singleInstance` activity like the panel opens the Calculator.

- The app takes the front and the focus within about 30 ms. The system then destroys the see-through activity
  by itself, 0.2 to 1.5 s later. Nobody called `finish()`.
- `AppTask.moveToFront()` 10 ms after the start does not bring it back.
- Starting itself again right after starting the app does (`NEW_TASK or REORDER_TO_FRONT`). At once: a new
  instance stands over the app, with the focus about 90 ms after the app's start. After 400 ms: the same
  instance comes back. After 1.5 s: too late. The background-start rules did not block it.

| Way | What the code and the platform say | Verdict |
| --- | --- | --- |
| **Open the app, then start the panel again at once** | Below | **Works [D]. Recommended** |
| Bring the panel's task back | `AppTask.moveToFront()` | Fails [D] |
| A launch option that leaves the caller in front | `ActivityOptions` has none in public [J]. `setLaunchBounds` and `setLaunchDisplayId`, in use [C: `Executor.launch`], say where a window opens, not who is in front. `makeTaskLaunchBehind()` is public [J] but is for a new document's task that is not shown [I] | No |
| The hold from step 1, alone | It stops Booklight's own closing [C: `onWindowFocusChanged`, `onStop`], not the system's destroying [D] | No |
| The pinned layer; picture-in-picture | The first was granted with `USE_PINNED_WINDOWING_LAYER` and has a caption [R: `device-findings.md`, "The pinned window"]; the manifest does not ask for that [C], so it is a new permission [I]. The second never has the keys [R] | No |
| Practice only: the row answers, nothing opens | Below | The fallback |

**The recommended way, in the code.**

- `OverlayActivity.run`, for a run that ends a lesson and opens a window: the executor says yes [C], the lesson is
  marked (section 5), then `again()`: `applicationContext.startActivity(Intent(…, OverlayActivity::class.java)
  .setAction(ACTION_PANEL).putExtra(EXTRA_AGAIN, true).addFlags(NEW_TASK or REORDER_TO_FRONT))`. No `close()`.
- The old instance must not fold away, as a lost focus makes it today [C: `onWindowFocusChanged` calls `close()`].
  It holds still; or, better, it is finished in the same turn without its transition [C: `finishNow`], so that
  it is always a new instance that answers [I: not tried].
- A new instance (`onCreate` with `EXTRA_AGAIN`) takes its stage from `Prefs`, as every opening does. It needs a
  third `Arrival`, `still`: glass, height and parts stand in the first frame. The opening set to Off is that
  path but for its fade [C: `placeWindow`; `Panel`: `gate = !arrival.unfold`]. It is not counted as an opening,
  and it is no key [C: `byKey` wants `ACTION_MAIN` or `ACTION_ASSIST`].
- The same instance (`onNewIntent` with `EXTRA_AGAIN`) [D: at 400 ms]: no toggle [C: today a start without text
  ends in `close()`]; the field is emptied and the next stage stands.
- A watch on the app's scope [C: `BooklightApp.scope`], not the activity's, which dies with it [C:
  `lifecycleScope`; the 1.5 s call died so, D]: 700 ms on, no panel with the focus: start it once more, never a
  third time. If no window of Booklight's is left by then, the background-start rules may refuse it [I].

**To the eye: not filmed.** The system fades a see-through task in over about 200 ms and animates its going; an
app can switch neither off [R: `device-findings.md`]. Expected: the app's window opens, the old glass goes, the
new glass comes up over 200 ms with the next lesson on it. At best one breath of the glass. At worst a blink of
0.1 to 0.3 s, or two glasses in one place (twice the tint and the shadow) until the old one is gone.

| Risk | Answer |
| --- | --- |
| The fades | Milestone 0's film decides. A blink Alex will not have: practice |
| The HP Googlebook | Not tried: the same film there |
| The system's destroying races the start: between 0 and 400 ms either instance may answer [D]; one that answers and is then destroyed leaves nothing in front [I] | Finish the old one first; the watch |
| An app that is slow to draw, or opens a second window, takes the front again [I]; the lesson's app comes from a table of well-known apps [C: `AppSearch.example`] | A new panel that loses the focus in its first 600 ms looks again 250 ms later before it closes [C: `EARLY_MS`]; the watch tries once. After that the user is in their app, and their key brings the next lesson |
| Letters typed in the gap, about 90 ms [D], go to the app [I] | Accepted |

**Cost: 1.5 days**, and a quarter day in milestone 0. **The proof, in one line:** film at 120 Hz Enter opening
the Calculator, the lesson's app, and an app that is already open, with the panel started again (a) at once,
(b) at once with the old one finished, (c) 400 ms later, on both Googlebooks; count the frames without glass and
the frames with two.

**Practice only.** `FirstRun.enters(lesson, effect)` says REAL or PRACTICE. PRACTICE is caught in
`OverlayModel.enter()` before anything runs, as "Don't suggest" is [C]; the footer says what Enter would have
done (`say` [C]) and the lesson is done. Nothing opens, nothing is learned [C: the engine learns only in
`OverlayActivity.run`]. Half a day, built in any case as that switch. Free for the designer with it: the glass
can start to leave and turn round, to say "here Booklight would go" [C: `close()`, `turn()`; `./bl debug turn`].

**Recommended: behind the glass**, sized here, decided by milestone 0's film. The mechanism is seen on one
device; the look is not. I am about two to one that it ships; practice is one switch away.

**State and resume, now that a run is one opening**

| | First plan | Now |
| --- | --- | --- |
| Three openings | Per step (`firstShown`) | Per run: `firstOpens: Int`, counted at the gate, never for a panel started again. After three, what is left waits on the Start page |
| Done | After the executor's yes, before `close()` | The same, before `again()`. Practice: at Enter |
| The sum | Copies; the panel closes 520 ms later [C: `run`] | `run(…, keep = true)` [C]: it copies, says so and stays; 520 ms later the field empties and the question stands |
| Between lessons | A new opening | A lock: from a lesson's Enter until the next stage stands, Enter does nothing. Today `settled` does that only on the way out [C] |
| Resume from `Prefs` | Every lesson | Esc, a lost focus, something else run for real, and every panel started again |
| The hold | Step 1 | Step 1, unchanged |

Races that go: the key pressed while the panel leaves after a lesson; the example late at a later opening; the
per-step counts. Races that come:

| Race | Cause | Answer |
| --- | --- | --- |
| Enter twice at a lesson's end runs the row for real | The lesson is done and its list still stands | The lock |
| Two instances for a moment | The new one is made before the old one is destroyed [D] | `current` is guarded by identity in `onDestroy` [C]: the old one's end closes nothing of the new one's |
| The panel's own start is taken for the key or the toggle | `onNewIntent` closes on a start without text [C] | An action and an extra of its own |
| A write is pending when the system destroys the old instance | | The settings are the app's, not the activity's [C: `Prefs(this, scope)`] |

### 12.2 The overture

**What is there [C].** `typeOut` types an example a letter at a time, 16 to 40 ms each, with one search at the
end; the user's own typing cancels it in the same call (`type`); Enter does nothing meanwhile (`enter`). And the
window's demo is already a script, written by hand, on a model made with `demo = true`: `type`, `delay`, `move`,
`arm` (`window/Stage.kt` · `Stages.Demo`). Such a model asks no network and learns nothing: `search`, `ask`,
`look`, `went`, `entered`, `used`, `enterKeyword` and `into` each ask `demo`.

| Part | What is built |
| --- | --- |
| `core/…/Overture.kt`, `OvertureTest` | The script as data: `Type(text)`, `Hold(ms)`, `Down`, `Arm`, `Clear`, `Lap`. No beat runs anything: there is none that could. Tests: 8 s at most; it ends on an empty field |
| `OverlayModel.perform(script)` | One job, as `Demo` has. A letter is `type(query + c)`, as `./bl debug keys` types [C], so every list in between is made and cascades; `typeOut` cannot do that [C]. Deleting: letters from the end, or one clear. A hand's pace (`Motion.perform()`, about 70 ms a letter); pauses are `hold()` [C]. `typeOut`, `Effect.Type` and `guided` gain nothing |
| `performing` | Joins `demo` in every guard above; `enter`, `runRow`, `nudge` and `tabCopy` refuse (a level's `nudge` runs its effect at once [C]). What it typed is nobody's last text [C: `keep` asks `shown`]. The stage is null while it plays |
| Rows and words | The real engine's rows [C: `search`]. One beat is this device's app with its icon (`firstExample()`; three rows at most: `limit` is a parameter [C]). The others are the same rows on every Googlebook: a sum, the emoji grid, a colour, a QR code. Their words are a string array in English and German (`12*3.5` is `12*3,5` in German [C: `guide`]); `./bl debug first overture check` runs each through the engine, as `guide` does [C] |
| Heights | `Metrics.height` once each list lands [C]. A sum's or a colour's row is 92 dp, the QR code's 208, a full grid 256 [C: `rowHeight`]: 376 dp in all at most. Under 468: free. A screen under 540 dp gets no overture |
| The user's first key | A letter: `endPerformance()` empties chip, text and rows in that call, and `Panel`'s `onChange` already keeps only what an edit added to a text the model no longer has [C]: the letter is the whole text, in that frame. Other keys reach `Panel.keys` first [C]: Esc ends it and K1 stands; Enter, Tab and the arrows end it and do nothing more. A click ends it |
| Once | `firstDone` gains `show` when it ends, or after 2 s of playing: a focus lost in the first second must not use it up [C: `EARLY_MS`]. A replay empties `firstDone`, so it plays again |
| Slow opening, dim, lap | `Arrival.of("slow", …)` for this one start [C: read once in `onCreate`]; no overture with animations off [C: `motion.on`]. The dim: `present()` sets it per frame when one was asked for at creation [C: `placeWindow`]: 0.3, times a factor that goes to 0 as K1 is set down (a quarter day; the bars dim too). The lap: section 7's trigger |

**It never** runs anything; sends anything (suggestions are off, and the guard does not rely on that); enters a
prompt (that asks the system about its model [C: `entered`]); reads the clipboard; changes a level; touches the
history or "your usual" (learned only in `OverlayActivity.run` [C]).

**Size: 3.5 days** (3 to 5): script and tests 0.5; the performer and its guards 1; keys, Esc, the slow opening,
the dim, the lap and the hand-over to K1 0.75; words and hooks 0.5; filming and tuning 0.75. The range is the
tuning: "wow" is judged by eye.

**The motion designer must not ask for:** an Enter, or anything seen to run; a list landing in a named frame,
or a letter faster than two frames (a search is a job and lands a frame or two after its letter [C: `search`]);
the same rows or height on every device for the app's beat; two lists dissolving into each other, a list
running backwards, a loop; the caret anywhere but at the end, or a selection; a height on anything but `place`
[C: `Panel`]; more than 468 dp, a width, a top edge, anything outside the glass; the user's key waiting for a
phrase to end.

### 12.3 The key

- **The code** `KeyEvent.KEYCODE_CONTEXTUAL_INSERT` is public in API 37 [J], which the app compiles against [C:
  `app/build.gradle`].
- **Per keyboard.** New `device/Keyboards.kt`: `InputDevice.hasKeys(KEYCODE_CONTEXTUAL_INSERT)` [J]. For the
  helper's rows it asks the keyboard the system names: `onProvideKeyboardShortcuts(…, deviceId)` [C: the
  signature]; which id comes after `requestShowKeyboardShortcuts()` is not known [I]: milestone 0. For the caps:
  the keyboard of the last key the panel saw (`Panel.keys` has the event [C]); none yet: the built-in one
  (`isExternal` false [J]). An external keyboard without the key, in front: Action + J. Chosen once, as K1 is
  set down, so that no cap changes its width under the reader. `FirstRun.suggest(hasKey)` is the pure part.
- **The cap's label.** The helper's "Quick Insert" [D] is the system UI's word. Its resource name is not in the
  research: it is read off the device as the others were, and then `SystemWords` reads it by the same call, at
  the same odds (section 4). No public call names a key for people [I]. The fallback `key_quick_insert` is
  "Quick Insert"; its German comes off a device, not from a desk.
- **For the designer:** the cap is about 196 dp wide against 56 for "J" [I: 13 dp a letter at 24 sp]. With Action
  and the "+" that is about 350 of the band's 404 dp: the tail no longer fits before the answers, and "your key"
  grows with the suggestion.
- **The helper's rows:** `KeyboardShortcutInfo(label, KEYCODE_CONTEXTUAL_INSERT, META_META_ON)` [D: drawn as the
  Action glyph and a cap "Quick Insert"]. Row 3 names the key by the same word; J's set stays for a keyboard
  without the key; `first_key_other` becomes "If it was taken, try J".
- **Elsewhere:** the README's line on the two shortcuts [C: line 56] and the Start page's two drawn suggestions,
  with `win_key_text` and `win_key_steps` [C: `StartPage`], show one suggestion, from `Keyboards`.
- **Cost:** half a day more in step 1's milestone.

### 12.4 No row for the model

Out of the plan: `OnDevice.look()`, and with it `ai/OnDevice.kt` in section 2; the sentence for `PRIVACY.md`,
which stays as it is; C's third row and its states; milestone 0's probe; device check 6; decision 4. C is two
rows, 68 + 8 + 92 + 56 + 8 + 36 = **268 dp**, always, so the height goes from 324 to 268 between the question and
the choices (`motion.md` T8 has that move). Half a day less.

### 12.5 The order of work, the total, the cuts, the decisions

| M | What | Days | Was |
| --- | --- | --- | --- |
| 0 | Proof on both Googlebooks: the real panel under the helper; the referrer; the system's words and the key's name; `hasKeys` and the helper's `deviceId`; **the film of 12.1**; one performed beat and its frame times | 1.25 | 0.75 |
| 1 | `FirstRun` (with `firstOpens`, `suggest`, `enters`), schema 7, the state hooks | 1.5 | 1.5 |
| 2 | The stage as stills, with the wider caps; the old cards out | 2.5 | 2.5 |
| 3 | Step 1 for real; the key per keyboard | 2.5 | 2 |
| 4 | The lessons in one opening: the lock, the sum that stays, the coach line, the practice switch; the panel started again | 3.5 | 2 |
| 5 | The question, the two choices, the fold | 1.5 | 2 |
| 6 | The overture | 3.5 | new |
| 7 | Motion, from `motion.md` as it is revised | 2.5 | 2.5 |
| 8 | Replay, the window, the README, German, the screen reader's words, documents | 1.25 | 1.25 |
| 9 | Device pass, the HP with Alex, fixes | 2 | 1.5 |
| | **In all** | **22** | 16 |

Fairly 19 to 27. The least sure are milestones 4 (the film), 6 and 7 (the eye). **After the third round: 29.75
days** (section 13.4).

**What to cut first now**

| Cut | Saves | Loses |
| --- | --- | --- |
| The dim that lifts | 0.25 | A quieter desk behind the overture |
| The overture at three beats: the app, a sum, the emoji grid | 0.5 | The colour and the QR code |
| Behind the glass | 1.5 | Nothing opens in first run: practice |
| The system's words and the key's name, read | 0.5 | Exact quotes outside English and German |
| The window's rows in the helper; the icon's first click | 0.5 | The Apps list leads to the window |
| Motion by its own list (`motion.md` §6) | 1.25 | The written recipe, both laps |

All six leave about 17.5 days. Half of 22 is reached only by also giving up the overture and the choices, which
is not what Alex asked for.

**Decisions for Alex, new or changed** (1 to 3, 5 and 6 of section 11 stand; 4 is answered)

| # | Decision | Recommended | If you say the other thing |
| --- | --- | --- | --- |
| 7 | Lessons 2 and 3 | The app opens behind the glass, if milestone 0's film shows no blink; else practice | Practice from the start: 1.5 days less, and nothing opens in first run |
| 8 | The panel that comes back over a lesson's app | It stands at once, at the lesson's height: the one exception to "the opening is always the field's height" | It unfolds again: 0.4 s more, twice, and it reads as three openings |
| 9 | An unfinished run | It comes back in two more openings, then waits on the Start page | Per step, as in the first plan: up to three openings for each step |
| 10 | The overture's rows | This device's real rows; the app's beat differs from device to device | A fixed list, the same everywhere: a day more, and it is a film about the product |
| 11 | The overture at a replay | It plays again; any key ends it | Only on the very first start |
| 12 | A keyboard without Quick Insert | Action + J | Action + Alt + Space: three keys at every opening |

## 13. Third round (4 October, night)

*Alex, to the opening: a welcome first ("visually stunning … in an expanded version of our panel (same width) …
animation, graphics, motions, and light. Go all out"), and a flight in the performance. Marks as in section 12.
The designer's pages for both were not there yet.*

### 13.1 The example flight

**What is there [C].** The release code can already answer a flight's row from a sample: `FlightsProvider.stage`,
`unstage`, `Kept.sample`. Only the samples are debug-only (`FlightSamples.kt`), and the saved replies are assets
of debug builds alone (`app/build.gradle`). **The opening does not use `stage()`**: it answers the real id
(`flight:LH455…`) with the real row, whose actions open the flight's page, copy, pin and add to a calendar, and
it writes `kept`, `pins` and `ahead` (`full`).

| Question | Answer |
| --- | --- |
| Where the data lives | New `core/…/FlightExample.kt`: a `Flight` built in code, as `FlightSamples.lh400` is [C], and the moment it is looked at. LH455, San Francisco to Frankfurt, the plan of the saved reply (14:40 to 10:25 the next day [C: `flight-LH455-in-the-air.json`]), left at 14:47, expected within 15 minutes of the plan, which reads "On time" [C: `FlightStatus.ON_TIME`] |
| Why not the saved reply | It reads "24 min early" (10:01 against 10:25) [C]; it would put the service's own data into the release APK; and it is parsed at run time, which can fail |
| The row | New `FlightsProvider.example(flight, now, today, label, source)`. The words of `full()` (line one, `headline`, `badge`, the two `stop`s) move into one private function that both call. `example` touches none of `kept`, `numbers`, `looking`, `pins`, `places`, `ahead` |
| Into the list | A new beat of the script, `Show(text, row)`: the letters are written without a search, as `typeOut` writes them [C], then `results = listOf(row)`. The engine is not asked: with no key it gives the 56 dp row that names the airline, and at a replay with a key in, a row that waits [C: `row`, `plain`, `full`] |
| One seat | The row keeps `provider = "flights"`: `RowSlots` gives every row of that provider one seat [C], and the row's layout asks the provider only for a row without a body [C: `ResultRow`, `guess`] |
| "Now" | Fixed with the example, so the plane stands at about 0.6 of its way and the headline always reads the same. `today` is passed in (the day it leaves), not taken from the device's zone as `full()` takes it [C]: the same row in every time zone. The clock's 12 or 24 hours and the language are the device's [C: `clock`, `flightDay`] |
| The minute's tick | None: `counts` is false, so `ResultsBody` sets no wake [C], and `told()` knows no such id [C] |
| The plane | Nothing to drive. A line that appears with a share brings its plane in at the start and flies it there once: 60 ms, then 300 + 300 × share ms, about half a second here [C: `FlightLine`, `Motion.flies`]. No "Looking it up" first: nothing is looked up |
| Heights | 136 dp by the body's type [C: `Metrics.rowHeight`]: the beat is 68 + 8 + 136 + 8 + 36 = **256 dp**. `maxRows` already keeps room for one such row [C] and is not touched |
| The word "Example" | The row's `label`. A row without actions shows its label at its right end even when it is selected (the strip comes only with actions [C: `ResultRow`]), in the band where a flight's strip stands [C]. The footer's left seat says a flight's `source` for the selected row [C: `Footer`]: a few plain words there |

**Guards**

| So that it can never | Because |
| --- | --- |
| Be run, copied, pinned, opened or put in a calendar | `actions = emptyList()`: Enter finds nothing [C: `chosen`]; `Effect.Pin("flight", …)` is made only in `full()` [C] |
| Be taken for the real LH455 | Its id is `example:flight`. `waits`, `gone`, `retry`, `told` and `answer` know only ids the provider made itself [C] |
| Reach the pinned window | `pins` is written only in `full()`, and in `again()` from the service's answer about a flight that is already pinned [C] |
| Reach the history or "your usual" | Both learn only in `OverlayActivity.run` [C]; `learnable = false`; `Zero.KINDS` has no such id [C] |
| Stay behind | It stands only in `results` while `performing`; ending the performance empties them (12.2). `example()` has one caller |
| Fetch | `example()` reads no file and opens no connection; `look()` and `went()` are behind the performance's guard (12.2) |

Tests: `FlightExampleTest` (the phase is in the air, the verdict on time, the share between 0.55 and 0.65, by the
same `FlightStatus.row` the real row uses [C]); `OvertureTest` (a `Show` row has no actions).
**Size: 1 day**: the example and its test a quarter; the provider's shared function, with a look at the debug
samples after it, a half; the beat, the words in English and German and the guards a quarter.

### 13.2 The welcome

**What the panel offers for drawing, and what each costs.** The Lenovo's screen draws 120 frames a second [R:
`device-findings.md`]: a frame is 8.3 ms, and "60 a second" is every other one. No frame time of a tall glass
redrawn every frame has been measured; `flights-row/eng.md` says of the blur behind it "not measured".

| Means | In the code today | Cost next to the window's blur |
| --- | --- | --- |
| `graphicsLayer`: alpha, scale, a move | Everywhere (the arrival, `TipBody`) | The cheapest: what is drawn once is only placed again |
| `Canvas` in the draw phase: paths, gradients, a stroke that draws itself | `DrawnCheck`, `FlightLine`, the strips' panes [C] | Cheap for tens of shapes; nothing is laid out [C: `FlightLine`] |
| An AGSL `RuntimeShader` | The glass itself, redrawn every frame while its light runs [C: `Glass.kt`, `Modifier.glass`] | By area and by length: 720 × 468 dp is about 0.76 million pixels on the Lenovo. One short pass beside the glass's own: likely fine [I]. Loops over samples, or a blur inside the glass: no |
| Animated vectors | None: the library is not among the app's [C: `app/build.gradle`] | Not needed: every mark is path data [C: `ui/Icons.kt`] |
| Type | The system's font, at fixed weights [C: `ui/Theme.kt`, `Fonts`] | Laid out once, then moved as layers. A weight or a roundness that changes per frame is a new layout every frame: no |

**What "the window is exactly the panel" means here.** The expanded panel is the same window made taller: the
height rides `place` and the window follows it frame by frame [C: `Panel`, `sizeWindow`]; the blur is the root
view framed to the glass, and the root view clips [C: `frameGlass`]. So: the opening is still the field's height
and the welcome's room comes at the gate; the top edge and the width stand; **nothing is drawn outside the
glass**, no glow past the outline, no mark that leaves. Light is either on the outline, where the reflection
runs today, or inside. To 468 dp it fits a screen from 655 dp. Taller is possible (a full list is 648 dp today),
with more pixels to blur and to shade, and a check per screen (`FirstRun.fits`). The field stays in the
composition and keeps the keys whatever is drawn over it [C: `KeysFirst`; `Panel.keys` sees every key first].

**Kept out of every other opening.** `overlay/Welcome.kt` is composed only while `model.welcome` is true: no
class of it is loaded, no shader compiled and nothing allocated on a daily opening [I: classes load on first
use]. Its shader is made when the welcome is due, as the glass's is made in the first composition today [C]. No
bitmap, no asset, no new library. When it ends it leaves the composition.

**Interrupted in one frame.** One clock: every mark is a pure function of the time t and the glass's size.
Ending it is one state change: the welcome is gone in the next frame, the letter is in the field in the same
one (12.2), and the height turns to its new target from where it is, with the speed it has [C:
`animateDpAsState`]. Nothing needs to finish. The same clock is what `slow=4` stretches [C: `Motion.slow`] and
what a hook can stop.

**Tested.** `./bl debug first welcome` plays it in an open panel; `first welcome at MS` stands it still at t, for
`./bl shot` [C] beside the prototype's frame of the same t; `first trace N` logs each frame's time. Filmed at
`slow=4` and stepped, light and dark, over the white and the dark test window (`./bl backdrop` [C]), in English
and German, on both Googlebooks.

**Matching the prototype.** Recommended: both draw with what both have: paths, gradients, alpha, blend modes,
the same clock and the same springs' numbers. Where light needs a shader, one short one, written twice (GLSL for
the page, AGSL for the app): the glass's own is 70 lines of that kind [C], and a port is half a day. A stack of
effects written only for the page cannot be matched.

**Size: 6 days, fairly 5 to 9.**

| Part | Days |
| --- | --- |
| The tall glass as a state: `Metrics.height`, the screen check, the field kept live, the hand-over to the first beat | 0.75 |
| The clock, the stills, `slow`, the hooks | 1 |
| Large type set down: measured once, the longest German version decides its size | 0.75 |
| Drawn graphics | 1 to 2 |
| Light: the reflection as built, and one pass of a shader | 0.75 to 1.5 |
| Frame times on both Googlebooks, both themes, both test windows | 0.75 |
| **Tuning by eye, with the designer and with Alex** | 1 to 3: open-ended |

**What I would hold the designer to:**

1. One clock, about four seconds. No state from frame to frame, nothing random without a fixed seed.
2. Inside the glass, at the panel's width. One height or two, on `place`. It opens at the field's height.
3. The kit above: type as layers, paths and gradients, one shader pass of about 60 lines, about 150 moving marks
   at most. No blur inside the glass, no bending of what is behind it ("Android blurs what is behind a window
   but doesn't let an app bend it" [C: `Glass.kt`]), no bitmap, no video, no animation file.
4. It reads over a white window and over a black one, in both themes: every mark carries its own contrast, and
   light is never the only carrier. A light added to white shows nothing [C: `Glass.kt`, the reflection in
   light theme].
5. The words fit at 720 dp in English and German, in every version offered.
6. An order of what goes first if a frame is late: 8.3 ms on the slower Googlebook is the limit.
7. The prototype is built from the same kit, on the same clock.

### 13.3 The opening at ten to eleven seconds

- **Interruption:** as in 12.2. During the welcome the field is empty, so a letter is simply the text. Esc ends
  welcome and performance alike and K1 stands.
- **The script's shape:** the welcome is no beat. `Overture` gains its head (`Welcome(version)`: the script
  waits for the welcome's clock) and `Show`. The words' versions are string arrays in English and German; the
  version is fixed in the code once Alex has chosen.
- **Seen once:** still marked after 2 s of playing, or at its end.
- **New: a screen reader.** With one on there is no performance: the welcome's words are said once and K1
  stands, as with animations off.
- **Milestone 0, one proof more, with no new code:** `./bl open stay`, type `?` (a full list), then `./bl debug
  think on` [C: `pretendThinking`]: the glass's shader then redraws every frame over a tall glass with the blur
  behind it. Count the late frames on both Googlebooks. That says what a second pass may cost before anything
  is designed on it. A quarter day.

### 13.4 The total, and what to cut first now

| | Days | After the second round |
| --- | --- | --- |
| 0 Proof | 1.5 | 1.25 |
| 1 to 5, as in 12.5 | 11.5 | 11.5 |
| 6 The overture, with the example flight | 4.5 | 3.5 |
| The welcome | 6 | new |
| 7 Motion | 2.5 | 2.5 |
| 8 Replay, the window, documents | 1.25 | 1.25 |
| 9 Device pass | 2.5 | 2 |
| **In all** | **29.75** | 22 |

Fairly 25 to 38: the welcome's and the overture's tuning are judged by eye.

| Cut first | Saves | Loses |
| --- | --- | --- |
| The welcome's shader pass: its light is the reflection that exists | 1 | Light inside the glass |
| The welcome as large type set down and one drawn mark | 2 more | "All out" |
| The dim that lifts | 0.25 | |
| The colour and the QR code in the performance | 0.5 | Two of six beats |
| Behind the glass | 1.5 | Nothing opens in first run |
| The system's words read; the window's rows; the icon's first click | 1 | As in 12.5 |
| Motion by its own list | 1.25 | As in 12.5 |

The flight stays: it is one day, and it was asked for by name.

**Decisions for Alex, new**

| # | Decision | Recommended | If you say the other thing |
| --- | --- | --- | --- |
| 13 | The example flight's row | It can do nothing: no Open, no Copy, no Pin; it says "Example" where its actions would stand | The real row's actions, on a flight that is not flying: an Enter in the wrong moment opens a page or pins an example |
| 14 | The welcome's limits | The kit of 13.2, and 8.3 ms a frame on the slower Googlebook | No limits: no size can be given, and the daily glass's rules (nothing outside it, nothing bent) still hold |
| 15 | With a screen reader on | No performance: the words are said, the key step stands | It plays, unheard |
