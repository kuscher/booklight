# First run, part 4: the welcome and the show. Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A new installation's very first opening runs at the Slow speed and begins with the welcome "Lights on" (the glass grows to 720 × 468 dp and goes to night, the field is a lamp's head, a plain shaft of light falls on "Welcome to Booklight", one line and a small Swiss Army knife whose handle says "Show off") and with the show (Booklight performs this device's apps, a sum, an example flight and the emoji grid in the real panel, running nothing), and lands in the key's step with nothing of it having been the user's to answer blind.

**Architecture:** Core decides and is tested: who gets the opening piece and who is only greeted (`FirstRun.overture`, `greets`), what a key does while it plays (`FirstRun.pressed`), and the show as data (`Show.script`: its steps and their times, with the calculator's own answers; the example flight as a `Flight` of its own). In the app, `OverlayModel` says where the piece is (`playing`) and performs the show by setting its own fields as a key of the user's would leave them, so the panel's real rows, pill, strip, answer, flight line and grid draw it; the rows are a cast made once (`BooklightApp.cast`) whose every action carries nothing to run. `overlay/Welcome.kt` draws the welcome on one clock, in a layer of the glass's own under the field and the rows, with every mark of that clock in `overlay/Motion.kt`; the glass's shader is told how far it is night. `OverlayActivity.onCreate` asks how the opening begins between the count and the model, and `Panel` gives every key and click to the piece first. The window is still exactly the panel: the welcome's 468 dp is one more height in `Metrics`.

**Tech Stack:** Kotlin 2.4, JUnit 4 in `core/` (plain JVM, no `android.*`), Jetpack Compose in `app/` (compile SDK 37, min SDK 34), the repo's `./bl` helper over adb.

**Spec:**
- `docs/design/first-run/00-brief.md`, **"Settled for the build"**: it wins over every other paper. For this part: point 2 (the welcome is "Lights on" with **the plain shaft**: no dust in the beam, no lap of light round the outline during the welcome; the lamp's strike, the core and the soft sides stay), point 3 (the words are **A**), point 4 (the **very first opening runs at Slow**, once), point 6 (nothing opens in first run) and point 7 (Booklight never opens itself over another window). And "Third round": what Alex said to the opening.
- `docs/design/first-run/BUILD.md`: what parts 1 to 3 built, the table "Decided on the way", and "Known, and left for a later part".
- `docs/design/first-run/design.md` §2 (heights), §3 (the overture: the welcome and the show, with their tables of measures), §10 (every state: the longest German lines, a screen too low, animations off, a screen reader).
- `docs/design/first-run/motion.md` §2 (the overture: the welcome's and the show's timelines, "The one highlight", "Interrupting it"), §6 (animations off), §8 (measuring). Every other transition of that paper is part 5's.
- `docs/design/first-run/eng.md` §12.2 (the overture), §13 (the example flight; the welcome's kit and its cost). `docs/design/first-run/pm.md` for what the opening is for. The prototype, `docs/design/first-run/first-run.html`: its welcome and its show are the reference a still is laid beside, with its light on "The shaft" and its words on A.
- `docs/research/first-run-proof.md`, check 12: a 468 dp glass redrawn every frame with a shader pass and a canvas pass holds the screen's frame rate on the Lenovo Googlebook.
- `docs/superpowers/plans/2026-10-05-first-run-3-the-lessons.md`: part 3, on which this builds.

**Who sees what in a build of this part alone.** A new installation, at its very first opening, on a screen of 655 dp of height or more, with the system's animations on and no screen reader: the opening at Slow, the welcome, the show, and then the key's step as part 2 has it; after that everything is as in part 3. The same installation with animations off or a screen reader on: the key's step at once, as today, with "Welcome to Booklight" as the field's placeholder. On a lower screen: the key's step at once, as today. An unfinished run at a later opening, an installation that was there before, and someone with no run: no change. There is no "First steps" command and no row for it in the window (part 5); the transitions between the screens of parts 2 and 3 are still stills (part 5).

## Global Constraints

- Branch `first-run`. Local only: never pushed, no tag, no release.
- No attribution lines of any kind in commits or code: no `Co-Authored-By`, no Claude lines, no session links.
- The repo is public: no device serials, no adb names, no build numbers, nothing about what is installed on a device, in any file or commit message. The show's first rows are apps of the device, and the lessons' example names one: they are shown on the glass and by the debug hooks, and are never written into a file or a commit message.
- `core/` has no `android.*` import.
- Every user-facing string is a resource, in English (`res/values`) and German (`res/values-de`). In German a quotation opens with „ (U+201E) and closes with “ (U+201C); an English apostrophe is ’ (U+2019).
- No new permission, and no new line in `<queries>`. No manifest is changed.
- No network: nothing in this part opens a connection. The show fetches nothing; its flight is an example of the app's own data, and says so on the glass.
- Comments in the surrounding code's own style and density: plain sentences that say why; a KDoc where the neighbours have one.
- Never run `uiautomator dump`. A device is chosen by its model (`adb devices -l`), never by its place in the list. Only the coordinator touches a device: the engineer builds, and runs no `./bl` command but `./bl test` and `./bl build`.
- **Every write of first run's state goes through `Prefs.firstRun { … }`** (`data/Prefs.kt`): the change is worked out inside the settings' own update, never from a state read before it. In `OverlayModel` that is `step { … }`, in `OverlayActivity` it is `first(app) { … }`.
- **The order for every panel** is: the key landing (if the key made the panel), then the opening counted, then how the opening begins (`FirstRun.overture`, with the same `plain` as the count), then the model, then what stands. Nothing asks `FirstRun.stage` or `FirstRun.overture` before `FirstRun.opening` was applied.
- **Nothing in first run opens another app or brings Booklight over another window. The show performs: it runs nothing.** Its rows carry core `Show.NOTHING` in place of what they would do; while the piece plays, every key and click goes to `OverlayModel.press` first (core `FirstRun.pressed`), and `OverlayActivity.run`, where everything that runs passes, returns at once. No task gives an action another way to the executor.
- **A key pressed during the welcome or the show never reaches the armed answer of the key's step unseen.** No press passes through the piece but a typed character; the key's step "comes" when it lands, and takes a key 350 ms later (`OverlayModel.seen`, part 3).
- **Someone who has no run sees, writes and pays nothing new at an opening**: no new job, no new read of the system, no write of the settings, no new part composed. What draws the piece is composed only where `OverlayModel.began` is true. **Someone whose piece is not played** (animations off, a screen reader, a screen lower than 655 dp) **lands in the key's step as today.**
- The panel's rules hold: the window is exactly the panel and is never resized in width; one height for each thing that stands, in `Metrics`, and `Metrics.height(model)` equals what `Panel` draws in every frame, of the welcome, the show and the landing too; the blur is the window's root view framed to the glass (`OverlayActivity.frameGlass`, which no task touches); nothing is drawn outside the glass; the opening is the field's height, and what is under the field comes at the gate; the field is live and typing never waits.
- The daily panel's design rules (flat glass, no glow, no illustration, plain copy) are lifted for the welcome alone, by the owner's word. They hold again from the key's step on, and for the show, which is the product itself.
- **All motion's times and curves live in `overlay/Motion.kt`.** The welcome's are `Lights` there. The one thing that is not there is core's: the show's script, which says what Booklight does and how long after the thing before (`Show.script`), as data with tests; the app stretches its waits with `Motion.hold`.
- **The glass's shader is compiled on the device, not by the build.** Where a task changes it, the changed lines must be exactly as written.
- The welcome is "Lights on" with the plain shaft and the words A. The suggested key is Action + Quick Insert; a keyboard without that key is offered Action + M. The key's step, the lessons, the question and the choices are parts 2 and 3's and are not changed here but where a task says so.
- Use a task's code as it is written. A "Find" is a text that stands in its file exactly once at the moment the task runs; "Find, in one long line" is a piece of one line, which also stands in the file exactly once. If a Find does not stand exactly once, stop and report it: do not guess the place.

**The commands of this repo, as every task uses them**
- All core tests: `./bl test` (quiet: no output and exit status 0 mean every test passed).
- One test class: `./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SomeTest' --console=plain`.
- A debug build with its check: `./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT` (the build is good only if this prints the one word `BUILT`).

**Where a late fix to part 3 would touch this plan.** It is written on `first-run` at `066c734`. Every "Find" below stands in its file exactly once there, or once the tasks before it have run. If a later fix changes one of these places, the task that finds it must read the place again: `core/…/FirstRun.kt` (`Overture`, `overture`, `slow`), `FirstRunStartTest` (the low screen's test, the Slow opening's), `OverlayModel.kt` (`keyword`, the lines from `carries` to the stage's section, `stage`, the heading of the lessons' section, `firstHint`, `offerTip`, `offerZero`, `offerCopy`, `type`, the empty field's branch of `search`, `went`, `enter`, `runRow`, `keep`, the companion's last value), `Metrics.kt` (`pad`, `height`), `Motion.kt` (`LEAVE_MS`, `LocalMotion`), `Panel.kt` (the three curves under `SEAM`, the imports, `onPresence`, the reflection's effect, `leftAnswer`, the start of `keys`, `radiusPx`, the glass's last parameters, the glass's `CompositionLocalProvider`, `onChange`'s first lines, the body's `transitionSpec`, the footer's line, `DIGITS`), `OverlayActivity.kt` (`handedOver`, `onCreate` from the count to `typeStep`, `var opening`, the panel's call, `placeWindow`'s dim, `present`, `run`'s first line), `Glass.kt` (the imports, the comment above the shader, the uniform `solid`, the outline's `base`, `Modifier.glass`), `Field.kt` (the imports, the mark's box, `cursorBrush`, `none`), `Footer.kt` (the word for Esc), `FirstStage.kt` (the announcement, the `strip`), `ui/Theme.kt` (`isDark`), `providers/AppsProvider.kt` (`count`), `providers/Flights.kt` (the comment above `time`, `ID`), `BooklightApp.kt` (the import of `SearchEngine`, `providers`, `SETTINGS`), `DebugReceiver.kt` (the header, the `key` case, `dump`, the `first` case), the last line of both `strings_32.xml`, `CLAUDE.md` (the `FirstRun.kt` line, the first-run paragraph of the dev loop, the rule on flat glass, the gotcha on first run's choices), and `bl`'s header.

**How this plan was checked.** Every task below was applied from this text, in order, to a scratch checkout of `066c734`: each "Find" stood exactly once when its turn came; the failing tests of Tasks 1 to 3 failed as said and passed after; after every task `./bl test` passed and the debug build printed `BUILT`; Task 13's commands gave what it expects. **Nothing was run on a device, and nobody has seen what this plan draws.** The welcome's drawing is a port, measure for measure, of the prototype's own (`first-run.html`, `welcomeAt` and `shaft`); how it looks and moves on glass is Task 12's list, every answer of which reads "not run". The one change to code that the build does not compile is the glass's shader (Task 8: one uniform, used in one line): the first device check is that the panel still opens.

## Review Focus

Five things the papers imply that would bite a real user and that nothing would exercise unless a test is written for it. Each has its test in the task that owns the code.

| # | The input or condition | What must hold | Its test |
| --- | --- | --- | --- |
| 1 | Enter pressed in the welcome ("Show off"), again in the show, again while the key's step is arriving; Enter held down through all of it; Esc, Tab, an arrow, a click at any moment | The key's step comes with "Open Keyboard shortcuts" armed. None of these presses is that answer's: the piece uses every press up but a typed character, and while the step is being set down nothing more happens | Task 2: `enterPressedAgainAndAgainNeverGoesFurtherThanTheLanding`, `noPressPassesThroughThePieceButATypedCharacter`, `whileTheKeysStepIsSetDownNothingMoreHappens` |
| 2 | The system's animations off; a screen reader on; both; a start that carries another app's text; a screen of 654 dp; a piece that was shown; an update's run; a run past the key; a run that waits; a replay with and without the piece | Only where the piece would have played but for animations or a reader is there a greeting, and nothing is used up by it. Nobody else is greeted, nobody on a low screen gets a show without its welcome or a Slow opening for nothing, and all of them land in the key's step as today | Task 1: `aScreenTooLowForTheWelcomeHasNoOpeningPiece`, `whereThePieceIsDueAndNotPlayedItsTitleGreets`, `nobodyElseIsGreeted` (and part 1's `askingUsesNothingUp`, `theVeryFirstOpeningIsSlowOnce`, which stay green) |
| 3 | The show's sum as it is typed, a character at a time; another sum; a language that writes 1,5 | The show never shows a number the product would not: every answer is the calculator's for the text that stands in the field then, and none stands before the text is a sum | Task 3: `everyAnswerIsTheCalculatorsForTheTextInTheField`, `anotherSumIsPlayedByTheSameRules` |
| 4 | The example flight on any day, at any hour, in any time zone, with a key in or without | It is always the same row: in the air, on time, 4 h 07 min to go, the plane 61 % of the way, by the rules every flight's row is said by; what Booklight types for it reads as that flight; and what its rows carry is nothing the executor knows | Task 3: `theExampleFlightIsInTheAirAndOnTime`, `itsNumberReadsAsAFlightNumber`, `theShowsRowsCarryNothingToRun` |
| 5 | A device whose app names begin with digits or signs; with two letters equally common; with none that begins with a letter; the show cut short at any step | The first letter is one the device's apps really begin with, or there is no show at all (the key's step lands); the script is typed over and never leaves the field empty; it ends on the landing and on nothing else | Task 3: `theFirstLetterIsTheOneMostAppsBeginWith`, `theTextIsTypedOverAndTheFieldIsNeverEmpty`, `itEndsOnTheLandingAndNothingComesAfter` |

Also pinned: a typed key ends the piece at every moment (Task 2, `aKeyThatTypesEndsThePieceAtEveryMomentAndIsTyped`); the script's marks are `motion.md`'s (Task 3, `theMarksAreThePapers`); a still of the show is the script up to its moment and never lands (Task 3, `aStillIsTheScriptUpToItsMoment`). No JVM test reaches the app: that the welcome is drawn inside the glass and at its measures, that there is one highlight in every frame, that the heights are what is drawn, that Enter three times over opens no dialog, and that the panel still opens with the changed shader are Task 12's checks 1, 3 to 9, 16, 18, 21 and 25 on a device.

## Decided here

Where the papers differ, or are silent, or could not be built as written. Each is Alex's to overturn.

| # | What | Why | What it costs if wrong |
| --- | --- | --- | --- |
| 1 | **A screen lower than 655 dp of height has no opening piece at all**: the key's step at once, as today. Part 1's `Overture.SHOW` (the show without its welcome, from 475 dp) is taken out | `design.md` §10 wants the show there, "its pill fading in on row one", but no paper times that beginning (the one highlight is born as the welcome's handle), and three papers name three least heights for it (475 in core, 540 in `eng.md` §12.2, 570 in `design.md` §2; the grid alone needs 540). No Googlebook is that low | A zoomed external screen sees no show. A second beginning for the show, an enum value and two tests back: about a task |
| 2 | **Where the piece is not played for animations or a screen reader, nothing is used up**: the title greets as the placeholder in each opening the key's step stands in, and the piece still plays once animations are on | Part 1's tested rule ("asking uses nothing up"); `design.md` says "for that opening" | The greeting stands in up to three openings; someone who turns animations on a day later is still welcomed. One line to mark it shown instead |
| 3 | **"The plain shaft" is the prototype's "The shaft"**: the fall-off, the penumbra and the core, twelve faint rays, one breath of 6 %, the strike, a little bloom round the lit letters; no dust, no lap. And **no grain of the shaft's own** | He chose it from the prototype's switch, which has all of these in that position; `motion.md` recommends the bloom. Grain needs a bitmap, which the tech lead's kit rules out; the glass's own grain lies under the light | Rays or bloom unwanted: one constant each. Banding of the fall-off over a plain window: a 128 px noise tile, about ten lines |
| 4 | **The light is drawn with gradients and blend modes in layers, as the prototype draws it, not as a second shader** | A shader is compiled on the device, where no engineer of this plan looks; this is code the build checks, and a port of the reference line for line | About a dozen fills over the stage per frame instead of one pass. If Task 12's check 11 shows late frames: the penumbra and the core go first; or the pass is ported to a shader in half a day (`eng.md` §13.2) |
| 5 | **The welcome's clock starts at the gate**, whatever the speed of the opening. At Slow, which the very first opening is, the whole piece is 258 ms later than `motion.md`'s column (written for Medium), and nothing in it is stretched. A debug build's `opening=` still wins over Slow, so that a film can be laid beside the paper | The papers were timed before "the first opening is Slow" was settled | One constant (`Lights.GATE`) and the gate's own time |
| 6 | **The key's step takes a key 350 ms after the landing began** (part 3's `seen`), not from `motion.md`'s T + 180; until then every press but a typed character is used up, a fresh Esc included | One rule for every screen of first run, tested in core, and the one this part must not weaken | A deliberate Enter in that third of a second is lost without a word |
| 7 | **The piece is marked as shown when it ends or has played for two seconds** (`eng.md` §12.2 and part 1's test); `design.md`'s table has "the key's step at the next opening" also for a panel that loses the focus in its first second | A focus lost while the launcher's own window is still closing must not use the welcome up | Someone who clicks away in the first two seconds is welcomed again |
| 8 | **Backspace during the piece sets the key's step down**, as Esc does; `design.md` lists it among the keys that type | It types nothing: the field would be empty, which is the key's step | One condition in `Panel`'s `pressOf` |
| 9 | **A typed key in the welcome: the handle fades with the rest of the stage** (`design.md`); `motion.md` would have it travel to row one of the user's list | That list comes a frame or two after its letter and fades in with its own pill: a hand-over between the two would lie them over each other | For 80 ms one highlight fades as another comes. About half a task |
| 10 | **The show's own motion is the product's own, with four things of `motion.md` §2.2 not built**: the pill condensing into the grid's first cell (the pill fades where it stands as the grid arrives with its square, as it does when a user types `emoji`); the cells leaving as a wave (they fade in 80 ms, as rows leave); the key's step arriving part by part and cap by cap at the landing (it fades in whole, in 140 ms: that is transition T1, which part 5 builds for every arrival of that step); Booklight's mark coming up 100 ms before the first letter (it comes with it). The lower edge waits 80 ms for the fade, not 160 for the wave | Each needs a part of the daily panel rebuilt for these seconds (the grid's cells, the row's slots, the stage's skeleton), and none can be judged without a device. What is built of "one highlight, never gone": the handle becomes the pill in one frame, and the highlight travels from the grid into the armed answer (Task 11) | For about 120 ms, as the grid comes, there is no highlight or two. Each of the four is a task of its own once a film says it is missed |
| 11 | **The landing is cued 776 ms after the light sets off**, by the script, not by where the light is | `motion.md`'s own figure for a lap that is cut; the light and the script are stretched alike by a debug build's slow motion | The light is not exactly on the lower edge as it draws in, on a glass of another height (fewer apps make no difference: the grid's glass is always 376 dp) |
| 12 | **The show's rows**: the engine's own rows for the letter, the first five that are apps, with their real strips; the sum's row as the calculator's, with Copy alone; the emoji scope's own grid. **The word "Example" is drawn as every row's kind is** (second ink), not in strong ink | "The product performs": the row's own code draws it | Strong ink for that one word: one condition in `ResultRow` |
| 13 | **Hooks**: `first welcome [at MS]`, `first show [at NAME]`, `first land`. `motion.md` §8 also asks for `first trace N` and `first press`; neither is built | A still at a named moment is surer than a trace; `first press` is the reveal's, which is part 5's | A trace of the frame's numbers is about twenty lines |
| 14 | **The desk dims only behind a panel that began with the piece**; a debug hook that plays the welcome in an ordinary panel shows it without | `FLAG_DIM_BEHIND` is asked for when the window is made | The dim is looked at in a real first opening (check 10) |
| 15 | **The welcome's type is sized in dp, not sp**, and the title is made smaller where it would be wider than 470 dp | It is a picture, and it must stay inside the light at any font scale | A user with a large font scale sees the welcome's words no larger |

## What each task hands the next

For every pair of tasks that share a file or a name: what the one produces and the other consumes. Where a later task's "Find" is text an earlier task wrote, the row says so.

| From | To | What |
| --- | --- | --- |
| 1 | 2 | `core/…/FirstRun.kt`: Task 1 changes `Overture` and `overture` and adds `greets` after it; Task 2 adds a section after `slow`, which Task 1 leaves as it was. They do not meet |
| 1 | 10 | `FirstRun.Overture { NONE, WELCOME }`, `FirstRun.overture(…)`, `FirstRun.greets(s, motion, reader, plain, screenDp)` |
| 1 | 12 | `FirstRun.greets`, for the debug hook `first` |
| 2 | 7 | `FirstRun.Playing`, `FirstRun.Press`, `FirstRun.Ends`, `FirstRun.pressed(playing, press)` |
| 2 | 8, 9, 10, 12 | `FirstRun.Playing` (what the field, the welcome and the panel ask of `OverlayModel.playing`); `FirstRun.Press` (what `Panel` and the debug hook pass to `OverlayModel.press`) |
| 3 | 6 | `Show.letter(names)`, `Show.APPS`, `Show.FLIGHT`, `Show.LOOKED`, `Show.NOTHING` |
| 3 | 7 | `Show.script(sum, keyword, comma)`, `Show.Step`, `Show.Cue`, `Show.Still`, `Show.until(script, still)` |
| 3 | 12 | `Show.Still`, for the debug hook `first show at NAME` |
| 4 | 6 | `R.string.first_show_example`, `first_show_flight` |
| 4 | 7 | `R.string.first_hello_title` (the greeting's placeholder) |
| 4 | 9, 10 | `R.string.first_hello_title`, `first_hello_line`, `first_hello_cue` (the welcome; the greeting said to a screen reader) |
| 5 | 7 | `Motion.LANDS_AFTER_MS` |
| 5 | 9 | `Lights`, `SEAM_GROWS`, `SEAM_DRAWS_IN`, `FOLDS`, `Motion.DESK_LIFTS_MS` |
| 5 | 10 | `overlay/Panel.kt`: Task 5 takes the three curves out from under `SEAM`; Task 10 changes imports, `keys`, the glass and its layers. They do not meet |
| 5 | 11 | `Motion.GLIDE_AFTER_MS`, `Motion.TAKES_OVER_MS` |
| 6 | 7 | `BooklightApp.cast(): Cast?`, `Cast.letter`, `apps`, `flight`, `scope`, `keyword`, `grid`, `comma`, `Cast.sum(text, answer)` |
| 7 | 8 | `OverlayModel.playing`, `began` |
| 7 | 9 | `OverlayModel.playing`, `rounds`, `standsAt`, `cast`, `cued`, `cueUp`, `show()`, `land()` |
| 7 | 10 | `OverlayModel.begin()`, `press(press, again)`, `greets`, `hold`, `laps`, `cueUp`, `began`, `playing`; `Metrics.welcome` |
| 7 | 11 | `overlay/OverlayModel.kt` as Task 7 leaves it: the line `private var unseen = false` with its comment, after which Task 11 adds the highlight's state; in `land`, the line that holds the height and `clear()` after it, between which Task 11 says where the highlight stood; in `typed`, the line that begins `playing = null; cued = false`, to which Task 11 adds |
| 7 | 12 | `OverlayModel.begin(at)`, `show(only)`, `land()`, `press(…)`, `playing`, `began`, `cast`, `laps` |
| 8 | 9 | `Night`, `NIGHT`, `NIGHT_VEIL`, `NIGHT_DESK`, `lightScheme(tint)` |
| 8 | 10 | `Modifier.glass(…, veil, rim)`, `Night` |
| 9 | 10 | `Piece(model, gate, ends, night, onDesk)`, `onHandle(x, y)` |
| 9 | 11 | `overlay/Welcome.kt` as Task 9 writes it: its import of `withFrameNanos`, after which Task 11 adds three; `Piece`'s last line, `Welcome(model, gate, ends, night, through, onDesk)` with the brace that closes `Piece`, after which Task 11 composes `Glide`; its private `WIDE` and `mix` |
| 10 | 11 | `overlay/FirstStage.kt`: Task 10 changes the announcement; Task 11 changes an import and the `strip`. They do not meet |
| 10 | 12 | The piece plays: what the checks check. `overlay/OverlayActivity.kt` is not touched again |
| 11 | 12 | `OverlayModel.gliding`, for the dump |

---

### Task 1: `FirstRun`: who gets the opening piece, and the greeting where it is not played

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunStartTest.kt`

**Interfaces:**
- Consumes (in `object FirstRun`, part 1): `State`, `Run`, `Step`, `SHOWN`, `fits(screenDp)`, `parked(s)`, `step(s)`, `enum class Overture { NONE, SHOW, WELCOME }`, `const val WELCOME_SCREEN_DP = 655f`, `fun overture(s: State, motion: Boolean, reader: Boolean, plain: Boolean, screenDp: Float): Overture`, `fun shown(s: State): State`, `fun slow(s: State, overture: Overture): Boolean`.
- Produces, in `object FirstRun`:
  - `enum class Overture { NONE, WELCOME }`: the value `SHOW` is gone. `overture(…)` answers `NONE` on a screen lower than `WELCOME_SCREEN_DP`.
  - `fun greets(s: State, motion: Boolean, reader: Boolean, plain: Boolean, screenDp: Float): Boolean`: the opening piece is this opening's but is not played, because the system's animations are off or a screen reader is on. The welcome's title is then the field's placeholder for this opening.
  - `shown` and `slow` keep their signatures.

Part 1 gave a screen between 475 and 655 dp of height the show without its welcome (`Overture.SHOW`). No paper says how such a show begins (the one highlight is born as the welcome's handle), and three papers name three least heights for it. This plan does not build it (decision 1 in the plan's head): a screen too low for the welcome gets the key's step at once, as it does today. So the value goes, and with it the chance that something asks for a piece nobody plays.

With the system's animations off, or with a screen reader on, there is no welcome and no show; the key's step stands in the first frame and the welcome's title is the field's placeholder (`design.md` §10, "System animations off"). `greets` says when. It uses nothing up, as `overture` uses nothing up: the piece is still to come at a later opening with animations on.

- [ ] **Step 1: Write the failing tests, in `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunStartTest.kt`.** Two changes: the low screen, with the greeting after it; and the Slow opening without the value that goes.

Find:

```kotlin
    @Test fun aLowerScreenHasTheShowWithoutTheWelcomeOrNothing() {
        assertEquals(Overture.WELCOME, begins(State(Run.NEW), screenDp = 655f))
        assertEquals(Overture.SHOW, begins(State(Run.NEW), screenDp = 654f))
        assertEquals(Overture.SHOW, begins(State(Run.NEW), screenDp = 475f))
        assertEquals(Overture.NONE, begins(State(Run.NEW), screenDp = 474f))
    }
```

Make it:

```kotlin
    /** The welcome's glass is 468 dp high: a screen too low for it has no opening piece at all, and the key's step stands at once. */
    @Test fun aScreenTooLowForTheWelcomeHasNoOpeningPiece() {
        assertEquals(Overture.WELCOME, begins(State(Run.NEW), screenDp = 655f))
        assertEquals(Overture.NONE, begins(State(Run.NEW), screenDp = 654f))
        assertEquals(Overture.NONE, begins(State(Run.NEW), screenDp = 475f))
        assertEquals(Overture.NONE, begins(State(Run.NEW), screenDp = 474f))
        // And such an opening is not the Slow one: there is nothing to be slow for.
        assertFalse(FirstRun.slow(State(Run.NEW), begins(State(Run.NEW), screenDp = 654f)))
        assertEquals(listOf(Overture.NONE, Overture.WELCOME), Overture.entries)
    }

    private fun greets(s: State, motion: Boolean = true, reader: Boolean = false, plain: Boolean = true, screenDp: Float = 1200f) =
        FirstRun.greets(s, motion, reader, plain, screenDp)

    /** Animations off, or a screen reader on: no piece is played, and the welcome's title is the field's placeholder for that opening. */
    @Test fun whereThePieceIsDueAndNotPlayedItsTitleGreets() {
        val first = State(Run.NEW)
        assertTrue(greets(first, motion = false))
        assertTrue(greets(first, reader = true))
        assertTrue(greets(first, motion = false, reader = true))
        // Where it is played, nothing greets in its place.
        assertFalse(greets(first))
        // It uses nothing up: the piece is still to come once animations are on.
        assertEquals(Overture.WELCOME, begins(first))
    }

    /** Nobody is greeted for whom the piece would not have played either. */
    @Test fun nobodyElseIsGreeted() {
        for (motion in listOf(true, false)) for (reader in listOf(true, false)) {
            // No run, an update's run, a run past the key, a piece that was shown, a run that waits.
            assertFalse(greets(State(), motion, reader))
            assertFalse(greets(State(key = true), motion, reader))
            assertFalse(greets(State(Run.UPDATE), motion, reader))
            assertFalse(greets(State(Run.NEW, done = listOf("key"), key = true), motion, reader))
            assertFalse(greets(FirstRun.shown(State(Run.NEW)), motion, reader))
            assertFalse(greets(State(Run.NEW, opens = FirstRun.MAX_OPENS + 1), motion, reader))
            // A panel that carries another app's text, and a screen too low for the welcome.
            assertFalse(greets(State(Run.NEW), motion, reader, plain = false))
            assertFalse(greets(State(Run.NEW), motion, reader, screenDp = 654f))
        }
        // A replay greets only where it asked for the piece.
        assertTrue(greets(FirstRun.replay(State(key = true), overture = true), motion = false))
        assertFalse(greets(FirstRun.replay(State(key = true), overture = false), motion = false))
    }
```

Find:

```kotlin
        assertTrue(FirstRun.slow(State(Run.NEW), Overture.WELCOME))
        assertTrue(FirstRun.slow(State(Run.NEW), Overture.SHOW))
        assertFalse(FirstRun.slow(State(Run.NEW), Overture.NONE))
```

Make it:

```kotlin
        assertTrue(FirstRun.slow(State(Run.NEW), Overture.WELCOME))
        assertFalse(FirstRun.slow(State(Run.NEW), Overture.NONE))
```

- [ ] **Step 2: Run them and see them fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunStartTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'greets'` among its `e:` lines.

- [ ] **Step 3: The piece and the greeting, in `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`.** Two changes: the kinds of opening; and `overture` with `greets` after it.

Find:

```kotlin
    /** How an opening begins. [SHOW]: Booklight uses itself, then the key's screen. [WELCOME]: the welcome first, then the show. */
    enum class Overture { NONE, SHOW, WELCOME }
```

Make it:

```kotlin
    /** How an opening begins. [WELCOME]: with the opening piece: the welcome, then the show in which Booklight uses itself, then the key's screen. */
    enum class Overture { NONE, WELCOME }
```

Find:

```kotlin
    fun overture(s: State, motion: Boolean, reader: Boolean, plain: Boolean, screenDp: Float): Overture = when {
        !motion || reader || !plain || !fits(screenDp) -> Overture.NONE
        s.run != Run.NEW && s.run != Run.REPLAY -> Overture.NONE
        SHOWN in s.done || parked(s) || step(s) != Step.KEY -> Overture.NONE
        screenDp >= WELCOME_SCREEN_DP -> Overture.WELCOME
        else -> Overture.SHOW
    }
```

Make it:

```kotlin
    fun overture(s: State, motion: Boolean, reader: Boolean, plain: Boolean, screenDp: Float): Overture = when {
        // (A screen too low for the welcome's glass has no piece at all: the show is not played without its welcome.)
        !motion || reader || !plain || screenDp < WELCOME_SCREEN_DP -> Overture.NONE
        s.run != Run.NEW && s.run != Run.REPLAY -> Overture.NONE
        SHOWN in s.done || parked(s) || step(s) != Step.KEY -> Overture.NONE
        else -> Overture.WELCOME
    }

    /**
     * The opening piece is this opening's and is not played, because the system's animations are off
     * or a screen reader is on: the key's screen stands at once, and the welcome's title greets as the
     * field's placeholder (and is said to a screen reader before the screen's own words). Like
     * [overture] it uses nothing up.
     */
    fun greets(s: State, motion: Boolean, reader: Boolean, plain: Boolean, screenDp: Float): Boolean =
        (!motion || reader) && overture(s, motion = true, reader = false, plain = plain, screenDp = screenDp) != Overture.NONE
```

- [ ] **Step 4: Run them and see them pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunStartTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run every core test, and build the app**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT` (the app's one user of `Overture`, the debug hook `first`, prints the value and names none).

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunStartTest.kt
git commit -m "First run: a screen too low for the welcome has no opening piece, and where the piece is not played its title greets (core, tested)"
```

### Task 2: `FirstRun`: what a key does while the opening piece plays

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunPlayingTest.kt`

**Interfaces:**
- Consumes: nothing but `object FirstRun` itself (Task 1 left `fun slow(s: State, overture: Overture): Boolean` as the last thing before `enum class Key`).
- Produces, in `object FirstRun`:
  - `enum class Playing { WELCOME, SHOW, LANDING }`: where the opening piece is while it plays.
  - `enum class Press { TYPES, ENTER, CUE, OTHER, MODIFIER }`: a key or a click, as the panel sees it.
  - `enum class Ends { NOTHING, BEGIN, LAND, TYPE }`: what it makes of the piece.
  - `fun pressed(playing: Playing, press: Press): Ends`.

The papers' two tables "During the welcome, the user" and "During the show, the user" (`design.md` §3) and `motion.md`'s "Interrupting it", as one function. A key that types ends the piece in that frame and is typed. Enter, and a click on the welcome's handle, start the show from the welcome: the cue is the one thing in the piece that is the user's to run. Esc, Tab, an arrow, Backspace and a click elsewhere on the glass set the key's step down, from the welcome and from the show. While it is being set down nothing more happens: a press there does not reach the screen that is arriving. A modifier alone is nothing.

Why it is core's: the key's screen comes with its first answer armed ("Open Keyboard shortcuts"). The Enter that says "Show off", an Enter that skips the show, and a key held down through both must never be that answer's Enter. Here no press but a typed character ever passes through the piece: every other one is used up by it.

- [ ] **Step 1: Write the failing test `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunPlayingTest.kt`**

```kotlin
package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Ends
import io.github.kuscher.booklight.core.FirstRun.Playing
import io.github.kuscher.booklight.core.FirstRun.Press
import org.junit.Assert.assertEquals
import org.junit.Test

class FirstRunPlayingTest {
    /** The cue is the one thing in the piece that is the user's to run: Enter, or a click on the handle that carries it. */
    @Test fun enterInTheWelcomeBeginsTheShow() {
        assertEquals(Ends.BEGIN, FirstRun.pressed(Playing.WELCOME, Press.ENTER))
        assertEquals(Ends.BEGIN, FirstRun.pressed(Playing.WELCOME, Press.CUE))
    }

    @Test fun escTabAnArrowAndAClickOnTheGlassSetTheKeysStepDown() {
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.WELCOME, Press.OTHER))
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.SHOW, Press.OTHER))
        // In the show Enter runs nothing: the highlighted row was Booklight's choice. It lands the key's step, as Esc does.
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.SHOW, Press.ENTER))
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.SHOW, Press.CUE))
    }

    @Test fun aKeyThatTypesEndsThePieceAtEveryMomentAndIsTyped() {
        for (playing in Playing.entries) assertEquals(Ends.TYPE, FirstRun.pressed(playing, Press.TYPES))
    }

    @Test fun aModifierAloneIsNothing() {
        for (playing in Playing.entries) assertEquals(Ends.NOTHING, FirstRun.pressed(playing, Press.MODIFIER))
    }

    /** One press, one step: while the key's step is being set down, nothing more happens. */
    @Test fun whileTheKeysStepIsSetDownNothingMoreHappens() {
        for (press in listOf(Press.ENTER, Press.CUE, Press.OTHER)) assertEquals(Ends.NOTHING, FirstRun.pressed(Playing.LANDING, press))
    }

    /**
     * The key's step comes with its first answer armed. No press reaches it through the piece: but for a typed
     * character, which goes to the field, every press at every moment is the piece's own, and is used up by it.
     */
    @Test fun noPressPassesThroughThePieceButATypedCharacter() {
        for (playing in Playing.entries) for (press in Press.entries)
            assertEquals("$playing $press", press == Press.TYPES, FirstRun.pressed(playing, press) == Ends.TYPE)
    }

    /** Enter held down from the cue to the key's step: it begins the show, lands the key's step, and then does nothing. Never a fourth thing. */
    @Test fun enterPressedAgainAndAgainNeverGoesFurtherThanTheLanding() {
        var playing: Playing? = Playing.WELCOME
        val did = ArrayList<Ends>()
        repeat(6) {
            val ends = FirstRun.pressed(playing ?: return@repeat, Press.ENTER)
            did += ends
            playing = when (ends) { Ends.BEGIN -> Playing.SHOW; Ends.LAND -> Playing.LANDING; Ends.TYPE -> null; Ends.NOTHING -> playing }
        }
        assertEquals(listOf(Ends.BEGIN, Ends.LAND, Ends.NOTHING, Ends.NOTHING, Ends.NOTHING, Ends.NOTHING), did)
    }
}
```

- [ ] **Step 2: Run it and see it fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunPlayingTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'Playing'` among its `e:` lines.

- [ ] **Step 3: The piece's keys, in `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`.** One change: a section after `slow`.

Find:

```kotlin
    /** The very first opening of a new installation, the one its opening piece begins, runs at the Slow speed whatever is set. */
    fun slow(s: State, overture: Overture): Boolean = s.run == Run.NEW && overture != Overture.NONE
```

Make it:

```kotlin
    /** The very first opening of a new installation, the one its opening piece begins, runs at the Slow speed whatever is set. */
    fun slow(s: State, overture: Overture): Boolean = s.run == Run.NEW && overture != Overture.NONE

    // ---- the opening piece, while it plays

    /**
     * Where the opening piece is. [WELCOME]: the welcome stands. [SHOW]: Booklight types into its own
     * field. [LANDING]: the key's screen is being set down, and has not been in view for long enough
     * to take a key.
     */
    enum class Playing { WELCOME, SHOW, LANDING }

    /**
     * A key or a click while the piece plays, as the panel sees it. [TYPES]: a key that puts a
     * character into the field. [ENTER]: Enter. [CUE]: a click on the welcome's handle, which carries
     * the cue. [OTHER]: Esc, Tab, an arrow, Backspace, any other key, and a click elsewhere on the
     * glass. [MODIFIER]: Shift, Ctrl, Alt or the Action key alone.
     */
    enum class Press { TYPES, ENTER, CUE, OTHER, MODIFIER }

    /**
     * What a press makes of the piece. [NOTHING]: it goes on. [BEGIN]: the welcome hands over to the
     * show. [LAND]: the piece ends and the key's screen is set down. [TYPE]: the piece ends in that
     * frame and the character is typed, as on any day.
     */
    enum class Ends { NOTHING, BEGIN, LAND, TYPE }

    /**
     * What [press] does while the piece is [playing]. A typed character always wins. The cue (Enter
     * in the welcome, a click on its handle) is the one thing in the piece that is the user's to run:
     * it begins the show. Every other press sets the key's screen down, once: while that happens,
     * nothing more does. Whatever the answer, a press that is not [Ends.TYPE] is used up here and
     * reaches nothing else: the key's screen comes with an answer armed, and no Enter pressed during
     * the piece may be that answer's.
     */
    fun pressed(playing: Playing, press: Press): Ends = when {
        press == Press.MODIFIER -> Ends.NOTHING
        press == Press.TYPES -> Ends.TYPE
        playing == Playing.LANDING -> Ends.NOTHING
        playing == Playing.WELCOME && (press == Press.ENTER || press == Press.CUE) -> Ends.BEGIN
        else -> Ends.LAND
    }
```

- [ ] **Step 4: Run it and see it pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunPlayingTest' --console=plain
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
git add core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunPlayingTest.kt
git commit -m "First run: what a key does while the opening piece plays, and that none of them reaches the key's step (core, tested)"
```

### Task 3: `Show`: the show's script as data, and its example flight

**Files:**
- Create: `core/src/main/kotlin/io/github/kuscher/booklight/core/Show.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/ShowTest.kt`

**Interfaces:**
- Consumes (core, as they are): `Calc.answer(input: String, comma: Boolean = false): String?`; `Effect.Internal(command: String)`; `Flight(number, airline, from, to, state, flownAs, timetable, loose, aircraft)`, `FlightEnd(code, city, airport, planned, expected, actual, offset, terminal, gate, belt)`, `FlightState.IN_AIR`; for the test: `FlightStatus.row(f: Flight, now: Instant): FlightRow`, `Flights.read(text: String, airlines: Airlines, today: LocalDate, sure: Boolean = false): FlightNumber?`, `Airlines(lines: Sequence<String>)`.
- Produces, in a new `object Show`:
  - `sealed interface Step` with `Letter`, `Typed(text: String)`, `Apps`, `Down`, `Tab`, `Sum(answer: String)`, `Flight`, `Grid`, `Cell(dx: Int, dy: Int)`, `Lap`, `Land`.
  - `data class Cue(val at: Int, val step: Step)`: a step, `at` ms after the show's first letter.
  - `fun script(sum: String, keyword: String, comma: Boolean = false): List<Cue>`.
  - `enum class Still { APPS, ROW, STOP, SUM, ANSWER, FLIGHT, GRID, CELL }` and `fun until(script: List<Cue>, still: Still): List<Cue>`: the script up to a named moment, for a picture.
  - `fun letter(names: List<String>): String?`, `const val APPS = 5`.
  - `val FLIGHT: Flight`, `val LOOKED: Instant`, `val NOTHING: Effect`.
  - `const val LETTER_MS`, `LANDS_MS`, `SPACE_MS`, `LANDED_MS`, `ROLLED_MS`, `LAP_TO_LAND_MS`.

The show is the product at work: Booklight types four things into its own field, and the panel answers with the rows it has every day (`design.md` §3, "The show"; `motion.md` §2.2). What happens, in which order and how long after the thing before, is data, here. The app performs it (Task 7) and can run nothing by it: no step is an Enter, and every row the app puts up for the show carries `Show.NOTHING` in place of what it would do.

The times are `motion.md`'s, counted from the show's first letter (the paper's 3,820 ms): the letter that makes the text a sum at 1,960 (5,780), the flight's row at 3,352 (7,172), the keyword's space at 4,840 (8,660), the light at 5,080 (8,900), the landing at 5,856 (9,676). They follow from a few gaps, so that another sum would still be typed by the same hand.

The sum's answers are not written down: the script asks `Calc` what stands for each text as it is typed, so the show can never show a number the product would not. The flight is held as a `Flight` of its own (`eng.md` §13.1): LH 455, San Francisco to Frankfurt, left at 14:47 against a plan of 14:40, expected as planned, looked at 4 h 07 min before it lands. Its number is what Booklight types; a flight number is no word.

- [ ] **Step 1: Write the failing test `core/src/test/kotlin/io/github/kuscher/booklight/core/ShowTest.kt`**

```kotlin
package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.Show.Cue
import io.github.kuscher.booklight.core.Show.Step
import io.github.kuscher.booklight.core.Show.Still
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class ShowTest {
    /** The script as the product plays it: the sum of lesson 4, the emoji keyword. */
    private val script = Show.script("150 + 20%", "emoji")

    private fun at(step: Step): Int = script.first { it.step == step }.at
    private fun typed(): List<Cue> = script.filter { it.step is Step.Typed }
    private fun text(c: Cue): String = (c.step as Step.Typed).text

    /** `motion.md` §2.2, counted from the show's first letter (its 3,820 ms). */
    @Test fun theMarksAreThePapers() {
        assertEquals(0, at(Step.Letter))
        assertEquals(0, at(Step.Apps))
        assertEquals(listOf(480, 780), script.filter { it.step == Step.Down }.map { it.at })
        assertEquals(listOf(1100, 1340), script.filter { it.step == Step.Tab }.map { it.at })
        assertEquals(1560, at(Step.Typed("1")))
        assertEquals(1960, at(Step.Typed("150 + 2")))
        assertEquals(2300, at(Step.Typed("150 + 20")))
        assertEquals(2600, at(Step.Typed("150 + 20%")))
        assertEquals(3080, at(Step.Typed("L")))
        assertEquals(3352, at(Step.Flight))
        assertEquals(4460, at(Step.Typed("e")))
        assertEquals(4840, at(Step.Grid))
        assertEquals(5080, at(Step.Lap))
        assertEquals(listOf(5240, 5350, 5460, 5620), script.filter { it.step is Step.Cell }.map { it.at })
        assertEquals(5856, at(Step.Land))
    }

    @Test fun itEndsOnTheLandingAndNothingComesAfter() {
        assertEquals(Step.Land, script.last().step)
        assertEquals(1, script.count { it.step == Step.Land })
        assertEquals(script.map { it.at }.sorted(), script.map { it.at })
        // Under seven seconds from the first letter: with the welcome's four, the whole piece stays under the thirteen it may take.
        assertTrue(script.last().at < 7000)
    }

    /** The show never shows a number the product would not: each answer is what the calculator says for the text that stands then. */
    @Test fun everyAnswerIsTheCalculatorsForTheTextInTheField() {
        val sums = script.filter { it.step is Step.Sum }
        assertEquals(listOf("152", "170", "180"), sums.map { (it.step as Step.Sum).answer })
        for (s in sums) {
            val field = text(typed().last { it.at <= s.at })
            assertEquals(field, Calc.answer(field), (s.step as Step.Sum).answer)
        }
        // And nothing answers before the text is a sum: the apps stand until then.
        assertEquals(1960, sums.first().at)
        for (c in typed().filter { it.at in 1560 until 1960 }) assertNull(text(c), Calc.answer(text(c)))
    }

    /** One movement, not four slides: the text is typed over, never cleared, and the field is never empty before the chip. */
    @Test fun theTextIsTypedOverAndTheFieldIsNeverEmpty() {
        var before = ""
        for (c in typed()) {
            val now = text(c)
            assertTrue(now, now.isNotEmpty())
            // A letter more, or the first letter of the next thing in place of all that stood there.
            assertTrue("$before -> $now", now.length == 1 || (now.length == before.length + 1 && now.startsWith(before)))
            before = now
        }
        assertEquals(listOf("1", "L", "e"), typed().map(::text).filter { it.length == 1 })
        // The whole of each thing is typed: the sum, the flight's number, the keyword.
        assertTrue(typed().any { text(it) == "150 + 20%" })
        assertTrue(typed().any { text(it) == Show.FLIGHT.number })
        assertEquals("emoji", text(typed().last()))
        // The keyword's space makes the chip: no text is typed after it.
        assertTrue(typed().all { it.at < at(Step.Grid) })
    }

    /** A hand, not a machine gun: no two letters closer than two frames of a 60 Hz screen, and the letter that lands a row a little later than the others. */
    @Test fun itIsTypedByAHand() {
        val gaps = typed().zipWithNext { a, b -> b.at - a.at }
        assertTrue(gaps.toString(), gaps.all { it >= 34 })
        assertEquals(Show.LETTER_MS, at(Step.Typed("15")) - at(Step.Typed("1")))
        assertEquals(Show.LANDS_MS, at(Step.Typed("150 + 2")) - at(Step.Typed("150 + ")))
        assertEquals(Show.LANDS_MS, at(Step.Flight) - at(Step.Typed("LH45")))
        assertEquals(Show.SPACE_MS, at(Step.Grid) - at(Step.Typed("emoji")))
        assertEquals(Show.LAP_TO_LAND_MS, at(Step.Land) - at(Step.Lap))
    }

    /** Booklight's moves in the first beat come before it types on, and its moves in the grid after the grid has come. */
    @Test fun theMovesBelongToTheirBeats() {
        val moves = script.filter { it.step == Step.Down || it.step == Step.Tab }
        assertTrue(moves.all { it.at > at(Step.Apps) && it.at < at(Step.Typed("1")) })
        val cells = script.filter { it.step is Step.Cell }
        assertEquals(listOf(Step.Cell(1, 0), Step.Cell(1, 0), Step.Cell(1, 0), Step.Cell(0, 1)), cells.map { it.step })
        assertTrue(cells.all { it.at > at(Step.Grid) && it.at < at(Step.Land) })
    }

    /** Another sum is typed by the same hand, and its answers are still the calculator's. */
    @Test fun anotherSumIsPlayedByTheSameRules() {
        val other = Show.script("12*3", "emoji")
        assertEquals(listOf("36"), other.filter { it.step is Step.Sum }.map { (it.step as Step.Sum).answer })
        assertEquals(Step.Land, other.last().step)
        // Where the user's language writes one and a half as 1,5 the answer is written so too.
        assertEquals(listOf("1,5"), Show.script("3/2", "emoji", comma = true).filter { it.step is Step.Sum }.map { (it.step as Step.Sum).answer })
    }

    @Test fun aStillIsTheScriptUpToItsMoment() {
        fun last(still: Still) = Show.until(script, still).last().step
        assertEquals(Step.Apps, last(Still.APPS))
        assertEquals(Step.Down, last(Still.ROW))
        assertEquals(Step.Tab, last(Still.STOP))
        assertEquals(Step.Sum("152"), last(Still.SUM))
        assertEquals(Step.Sum("180"), last(Still.ANSWER))
        assertEquals(Step.Flight, last(Still.FLIGHT))
        assertEquals(Step.Grid, last(Still.GRID))
        assertEquals(Step.Cell(0, 1), last(Still.CELL))
        // Each is the script from its beginning to that moment, without the light (a still has no motion), and none of them lands.
        for (still in Still.entries) {
            val part = Show.until(script, still)
            assertEquals(script.filter { it.at <= part.last().at && it.step != Step.Lap }, part)
            assertFalse(part.any { it.step == Step.Land || it.step == Step.Lap })
        }
        assertEquals(2, Show.until(script, Still.ROW).count { it.step == Step.Down })
        assertEquals(2, Show.until(script, Still.STOP).count { it.step == Step.Tab })
    }

    @Test fun theFirstLetterIsTheOneMostAppsBeginWith() {
        assertEquals("c", Show.letter(listOf("Calculator", "Calendar", "camera", "Maps", "Messages", "Chrome")))
        // Of two that begin as many, the earlier in the alphabet; a name is taken as it is shown, whatever its case.
        assertEquals("m", Show.letter(listOf("Zebra", "zoo", "Maps", "messages")))
        // A name that begins with a digit or a sign has no letter to give.
        assertEquals("p", Show.letter(listOf("1 Player", "2048", " Photos", "+Plus")))
        assertNull(Show.letter(listOf("1 Player", "2048")))
        assertNull(Show.letter(emptyList()))
        assertEquals("ä", Show.letter(listOf("Ärzte", "ärger", "Maps")))
    }

    /** The example is a flight in the air, on time, the plane 61 % of the way: said by the same rules as any flight's row. */
    @Test fun theExampleFlightIsInTheAirAndOnTime() {
        val row = FlightStatus.row(Show.FLIGHT, Show.LOOKED)
        assertEquals(FlightPhase.IN_AIR, row.phase)
        assertEquals(Headline(Heading.LANDS_IN, 247), row.headline)       // 4 h 07 min
        assertEquals(Badge(Verdict.ON_TIME), row.badge)
        assertEquals(Tone.GOOD, row.badge!!.tone)
        val share = row.share!!
        assertEquals(391.0 / 638.0, share, 1e-9)
        assertTrue(share > 0.60 && share < 0.62)
        // Under the line's ends: where it left, with the aircraft; where it lands, with the terminal. No gate, no belt.
        assertEquals(EndSays("SFO", LocalDateTime.parse("2026-10-01T14:47"), aircraft = "Boeing 747-8"), row.from)
        assertEquals(EndSays("FRA", LocalDateTime.parse("2026-10-02T10:25"), terminal = "1"), row.to)
        assertEquals("San Francisco", Show.FLIGHT.from.place)
        assertEquals("Frankfurt/Main", Show.FLIGHT.to.place)
    }

    /** What Booklight types for it reads as that flight, by the airline table the app ships. */
    @Test fun itsNumberReadsAsAFlightNumber() {
        val airlines = Airlines(File("../app/src/main/assets/airlines.tsv").readLines().asSequence())
        val n = Flights.read(Show.FLIGHT.number, airlines, LocalDate.parse("2026-10-01"))!!
        assertTrue(n.strong)
        assertEquals("LH 455", n.shown)
        assertEquals(Show.FLIGHT.airline, n.airline.name)
    }

    /** What the show's rows carry in place of what they would do is something the executor has no branch for. */
    @Test fun theShowsRowsCarryNothingToRun() {
        assertEquals(Effect.Internal("show"), Show.NOTHING)
    }
}
```

- [ ] **Step 2: Run it and see it fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.ShowTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'Show'` among its `e:` lines.

- [ ] **Step 3: Write `core/src/main/kotlin/io/github/kuscher/booklight/core/Show.kt`**

```kotlin
package io.github.kuscher.booklight.core

import java.time.Instant
import java.time.LocalDateTime

/**
 * First run's show: after the welcome Booklight types four things into its own field, and the panel
 * answers each with the rows it has every day: this device's apps, a sum, a flight, the emoji grid.
 * Then the key's screen is set down. What happens, in which order and how long after the thing
 * before, is said here as data; the app performs it. Nothing in it can be run: no step is an Enter,
 * and the rows the app puts up for it carry [NOTHING] in place of what they would do.
 * (docs/design/first-run/design.md §3, motion.md §2.2.)
 */
object Show {
    /** One thing Booklight does. */
    sealed interface Step {
        /** The first thing typed: one letter, the one most of this device's apps begin with ([letter]). */
        data object Letter : Step
        /** The field holds [text] now: a letter more, or the first letter of the next thing in place of what stood there. */
        data class Typed(val text: String) : Step
        /** The list is this device's apps for that letter, row one selected. */
        data object Apps : Step
        /** Down: the next row. */
        data object Down : Step
        /** Tab: the next stop of the selected row. */
        data object Tab : Step
        /** The list is one row: the answer of the sum that stands in the field. */
        data class Sum(val answer: String) : Step
        /** The list is one row: the example flight's. */
        data object Flight : Step
        /** The keyword's space: the keyword becomes the chip, the field is empty under it, and the list is its grid. */
        data object Grid : Step
        /** An arrow in the grid: [dx] cells across, [dy] lines down. */
        data class Cell(val dx: Int, val dy: Int) : Step
        /** The light sets off round the outline. */
        data object Lap : Step
        /** The key's screen is set down, and the show is over. */
        data object Land : Step
    }

    /** [step], [at] ms after the show's first letter. */
    data class Cue(val at: Int, val step: Step)

    /** Booklight's hand: a letter after the one before it. A hand, not a metronome, and never under two frames. */
    const val LETTER_MS = 64
    /** The letter that lands a row comes a little later than the others. */
    const val LANDS_MS = 80
    /** And the space that makes the chip later still. */
    const val SPACE_MS = 124
    /** After a sum's row has landed, and after its answer has rolled: before the next key. */
    const val LANDED_MS = 340
    const val ROLLED_MS = 300
    /** The landing waits for the light: it comes when the lap has turned onto the lower edge, this long after it set off. */
    const val LAP_TO_LAND_MS = 776

    // The first beat's moves, each after the thing before it: Down, Down, Tab, Tab.
    private const val FIRST_MOVE_MS = 480
    private const val DOWN_MS = 300
    private const val TAB_MS = 320
    private const val TAB_AGAIN_MS = 240
    // Before the first letter of the next thing: after the last Tab; after the answer has been read; after the plane has reached its place and rested.
    private const val TO_SUM_MS = 220
    private const val TO_FLIGHT_MS = 480
    private const val TO_GRID_MS = 1108
    // In the grid: the light sets off as the wave lands; then three quick steps, as a held key glides, and one settled.
    private const val TO_LAP_MS = 240
    private const val TO_CELLS_MS = 160
    private const val CELL_MS = 110
    private const val LAST_CELL_MS = 160

    /**
     * The show. [sum]: what is typed for the sum (lesson 4's own). [keyword]: the emoji grid's keyword.
     * [comma]: the user's language writes one and a half as 1,5. Each answer is what [Calc] says for
     * the text that stands in the field then, and comes with the letter that changes it.
     */
    fun script(sum: String, keyword: String, comma: Boolean = false): List<Cue> {
        val cues = ArrayList<Cue>()
        var at = 0
        fun cue(after: Int, step: Step) { at += after; cues += Cue(at, step) }

        // Apps: one letter, the device's apps, and Booklight's four moves in them.
        cue(0, Step.Letter); cue(0, Step.Apps)
        cue(FIRST_MOVE_MS, Step.Down); cue(DOWN_MS, Step.Down); cue(TAB_MS, Step.Tab); cue(TAB_AGAIN_MS, Step.Tab)

        // A sum, typed over the letter. The apps stand until the text is a sum; then its row, and the answer rolls as the sum goes on.
        var answer: String? = null
        var gap = TO_SUM_MS
        for (n in 1..sum.length) {
            val text = sum.take(n)
            val now = Calc.answer(text, comma)
            val changes = now != null && now != answer
            if (changes && answer == null) gap = maxOf(gap, LANDS_MS)
            cue(gap, Step.Typed(text))
            gap = LETTER_MS
            if (changes) { gap = if (answer == null) LANDED_MS else ROLLED_MS; answer = now; cue(0, Step.Sum(now)) }
        }

        // A flight, typed over the sum: its row lands with the last character of the number.
        val number = FLIGHT.number
        for (n in 1..number.length) cue(if (n == 1) TO_FLIGHT_MS else if (n == number.length) LANDS_MS else LETTER_MS, Step.Typed(number.take(n)))
        cue(0, Step.Flight)

        // The grid: the keyword typed over the number, its space, the light, four moves of the square.
        for (n in 1..keyword.length) cue(if (n == 1) TO_GRID_MS else LETTER_MS, Step.Typed(keyword.take(n)))
        cue(SPACE_MS, Step.Grid)
        cue(TO_LAP_MS, Step.Lap)
        val lap = at
        cue(TO_CELLS_MS, Step.Cell(1, 0)); cue(CELL_MS, Step.Cell(1, 0)); cue(CELL_MS, Step.Cell(1, 0)); cue(LAST_CELL_MS, Step.Cell(0, 1))

        // The landing, cued by the light.
        cue(maxOf(0, lap + LAP_TO_LAND_MS - at), Step.Land)
        return cues
    }

    /**
     * A moment of the show to stand still at, for a picture. [APPS]: the list has landed. [ROW]: after
     * Down, Down. [STOP]: after Tab, Tab. [SUM]: the sum's row has landed. [ANSWER]: its last answer
     * stands. [FLIGHT]: the flight's row. [GRID]: the grid, the square on its first cell. [CELL]: after
     * the square's four moves.
     */
    enum class Still { APPS, ROW, STOP, SUM, ANSWER, FLIGHT, GRID, CELL }

    /**
     * [script] up to [still], with everything that comes in the same moment: what to perform at once,
     * and then stand. Without the light: a still has no motion.
     */
    fun until(script: List<Cue>, still: Still): List<Cue> {
        val i = when (still) {
            Still.APPS -> script.indexOfFirst { it.step == Step.Apps }
            Still.ROW -> script.indexOfLast { it.step == Step.Down }
            Still.STOP -> script.indexOfLast { it.step == Step.Tab }
            Still.SUM -> script.indexOfFirst { it.step is Step.Sum }
            Still.ANSWER -> script.indexOfLast { it.step is Step.Sum }
            Still.FLIGHT -> script.indexOfFirst { it.step == Step.Flight }
            Still.GRID -> script.indexOfFirst { it.step == Step.Grid }
            Still.CELL -> script.indexOfLast { it.step is Step.Cell }
        }
        if (i < 0) return emptyList()
        return script.take(script.indexOfLast { it.at == script[i].at } + 1).filter { it.step != Step.Lap }
    }

    /** How many apps the first beat shows at most. */
    const val APPS = 5

    /**
     * The letter Booklight types first: the one that begins the most of [names], the device's apps;
     * of two that begin as many, the earlier one. Null where no name begins with a letter.
     */
    fun letter(names: List<String>): String? =
        names.mapNotNull { it.trim().firstOrNull()?.takeIf(Char::isLetter)?.lowercaseChar() }
            .groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<Char, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()?.key?.toString()

    /**
     * The example flight: LH 455 from San Francisco to Frankfurt, in the air and on time. Held here and
     * never asked of any service: its row says "Example". It left at 14:47 against a plan of 14:40, and
     * is expected as planned. Its number is what Booklight types for it.
     */
    val FLIGHT = Flight(
        number = "LH455", airline = "Lufthansa",
        from = FlightEnd("SFO", "San Francisco", "San Francisco International Airport", planned = LocalDateTime.parse("2026-10-01T14:40"), actual = LocalDateTime.parse("2026-10-01T14:47"), offset = -420),
        to = FlightEnd("FRA", "Frankfurt/Main", "Frankfurt Airport", planned = LocalDateTime.parse("2026-10-02T10:25"), expected = LocalDateTime.parse("2026-10-02T10:25"), offset = 120, terminal = "1"),
        state = FlightState.IN_AIR, aircraft = "Boeing 747-8",
    )

    /** The moment the example is looked at, whatever the clock says: 4 h 07 min before it lands, 391 of its 638 minutes in the air behind it. */
    val LOOKED: Instant = Instant.parse("2026-10-02T04:18:00Z")

    /** What every row of the show carries in place of what it would do: nothing the executor knows. */
    val NOTHING: Effect = Effect.Internal("show")
}
```

- [ ] **Step 4: Run it and see it pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.ShowTest' --console=plain
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
git add core/src/main/kotlin/io/github/kuscher/booklight/core/Show.kt core/src/test/kotlin/io/github/kuscher/booklight/core/ShowTest.kt
git commit -m "First run: the show's script as data, with the calculator's own answers, and its example flight (core, tested)"
```

### Task 4: The words of the welcome and of the example flight

**Files:**
- Modify: `app/src/main/res/values/strings_32.xml`
- Modify: `app/src/main/res/values-de/strings_32.xml`

**Interfaces:**
- Consumes: the last line of both files as part 3 left it (`a11y_first_done`, then `</resources>`). Strings that exist and are used again by later tasks without a change: `first_skip` ("Skip" · „Überspringen“: the footer's word for Esc while the show plays), `first_sum_example` (`150 + 20%`: the sum the show types), `action_copy`, `emoji_keys`.
- Produces: `R.string.first_hello_title`, `first_hello_line`, `first_hello_cue` (the welcome: Task 9; the title is also the greeting's placeholder: Task 7), `first_show_example`, `first_show_flight` (the example flight's row: Task 6).

The welcome's words are version A, as Alex settled them ("Settled for the build", point 3). They are the one place in Booklight where copy is not plain, by his word. The line is 52 characters in English and 53 in German: it stands on one line inside the light, which is 520 dp wide there (`design.md` §10), so neither may grow.

The flight in the show is an example and says so on the glass in two seats (`design.md` §3, "How the glass says it is an example"): the word where a row names its kind, and one line at the footer's left end, where a real flight's row names the source of its answer. It says "AirLabs key" and not "a key", because two beats later Booklight asks for a key of another kind.

- [ ] **Step 1: The English words, in `app/src/main/res/values/strings_32.xml`.** One change, at the file's end.

Find:

```xml
    <string name="a11y_first_done">First steps are done. Type question mark for everything.</string>
</resources>
```

Make it:

```xml
    <string name="a11y_first_done">First steps are done. Type question mark for everything.</string>

    <!-- First run · the welcome (design.md §3, words A): a title, one line and the cue on the knife's handle, in the lamp's light. The one place where copy is not plain (Alex, 4 October 2026). -->
    <!-- The title is also the field's placeholder for an opening in which the welcome is due and not played (animations off, a screen reader). -->
    <string name="first_hello_title">Welcome to Booklight</string>
    <!-- One line, 55 characters at most: it must fit the light. -->
    <string name="first_hello_line">Your Swiss Army knife launcher. Blades not included.</string>
    <string name="first_hello_cue">Show off</string>
    <!-- First run · the show's flight is an example, and says so: where a row names its kind, and at the footer's left end, where a flight's row names its source. -->
    <string name="first_show_example">Example</string>
    <string name="first_show_flight">An example. Live times need your own AirLabs key.</string>
</resources>
```

- [ ] **Step 2: The German words, in `app/src/main/res/values-de/strings_32.xml`.** One change, at the file's end.

Find:

```xml
    <string name="a11y_first_done">Die ersten Schritte sind fertig. Fragezeichen tippen für alles.</string>
</resources>
```

Make it:

```xml
    <string name="a11y_first_done">Die ersten Schritte sind fertig. Fragezeichen tippen für alles.</string>

    <string name="first_hello_title">Willkommen bei Booklight</string>
    <string name="first_hello_line">Dein Schweizer Taschenmesser zum Tippen. Ohne Klinge.</string>
    <string name="first_hello_cue">Kurz angeben</string>
    <string name="first_show_example">Beispiel</string>
    <string name="first_show_flight">Ein Beispiel. Live-Zeiten brauchen deinen eigenen AirLabs-Schlüssel.</string>
</resources>
```

- [ ] **Step 3: Both languages have the same five, and the line fits**

```bash
for f in app/src/main/res/values/strings_32.xml app/src/main/res/values-de/strings_32.xml; do grep -c 'name="first_hello_\|name="first_show_' "$f"; done
python3 - <<'PY'
import re
for f in ("app/src/main/res/values/strings_32.xml", "app/src/main/res/values-de/strings_32.xml"):
    line = re.search(r'name="first_hello_line">([^<]*)<', open(f, encoding="utf-8").read()).group(1)
    print(len(line), "FITS" if len(line) <= 55 else "TOO LONG")
PY
```

Expected: `5`, `5`, then `52 FITS` and `53 FITS`.

- [ ] **Step 4: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings_32.xml app/src/main/res/values-de/strings_32.xml
git commit -m "First run: the words of the welcome and of the example flight, English and German"
```

### Task 5: `Motion.kt`: the welcome's clock, its curves, and the landing's times

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Motion.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`

**Interfaces:**
- Consumes: `overlay/Motion.kt` as it is (`class Motion`, its companion with `OPENS`, the line `val LocalMotion = staticCompositionLocalOf { Motion(true) }` at the file's end); in `overlay/Panel.kt` the three private curves `SEAM_GROWS`, `SEAM_DRAWS_IN` and `FOLDS` under `private val SEAM = 6.dp`.
- Produces, in `overlay/Motion.kt`:
  - top-level `internal val SEAM_GROWS`, `SEAM_DRAWS_IN`, `FOLDS: CubicBezierEasing`, with the values they had in `Panel.kt` (which now uses these: same package, same names).
  - `object Lights`: the welcome's marks in ms after the gate, as `Int` constants: `GATE`, `NIGHT_MS`, `CARET_OFF`, `STRIKE`, `STRIKE_UP_MS`, `STRIKE_SAG_MS`, `STRIKE_ON_MS`, `FLOOD_AFTER_MS`, `FLOOD_MS`, `SEAM`, `SEAM_MS`, `OPEN`, `OPEN_MS`, `HANDLE`, `HANDLE_MS`, `TOOLS`, `BEAT_MS`, `GLINT_AFTER_MS`, `GLINT_MS`, `STANDS`, `HAND`, `PRESS_MS`, `RELEASE_MS`, `FOLD_EACH_MS`, `FOLD_MS`, `TURN_AFTER_MS`, `TURN_MS`, `FOOT_AFTER_MS`, `RISE_MS`, `LIFT_MS`, `CUE_OUT_AFTER_MS`, `CUE_OUT_MS`, `GO_AFTER_MS`, `WORDS_OUT_AFTER_MS`, `WORDS_OUT_MS`, `CLOSE_AFTER_MS`, `CLOSE_MS`, `HAND_MS`, `OUT_MS`, `DAWN_MS`, `WAITS_MS`.
  - in `Motion`'s companion: `const val LANDS_AFTER_MS = 80L`, `const val DESK_LIFTS_MS = 640`, `const val GLIDE_AFTER_MS = 32L`, `const val TAKES_OVER_MS = 120`.

Every time and every curve of the panel's motion lives in `Motion.kt`. This task puts the welcome's there, before anything draws it (Task 9), and moves the glass's own three curves there from `Panel.kt`, because the welcome's light makes the moves the glass made a second before: a seam first, then open to both sides, and at the end it folds as the glass folds.

The welcome runs on one clock (`motion.md` §2.1). The paper counts from the first frame of an opening at the Medium speed, whose gate (the glass 85 % open, the height leaving 68 dp) is at 257 ms. The very first opening runs at Slow, where the gate is at 515 ms: so the clock here starts at the gate, whatever the speed, and every mark is the paper's less 257. At Slow the whole welcome is then a quarter of a second later than the paper's column, and nothing in it is stretched (decision 5 in the plan's head).

Nothing uses the new values yet: a user sees no change.

- [ ] **Step 1: The curves and the marks, in `app/src/main/java/io/github/kuscher/booklight/overlay/Motion.kt`.** Two changes: four values at the companion's start, and the curves and `Lights` before `LocalMotion`.

Find:

```kotlin
        /** How long the panel takes to fade away when the opening is turned off (the unfold has its own time: [Arrival.leaveMs]). */
        const val LEAVE_MS = 110L
```

Make it:

```kotlin
        /** How long the panel takes to fade away when the opening is turned off (the unfold has its own time: [Arrival.leaveMs]). */
        const val LEAVE_MS = 110L

        // First run's show landing in the key's step (docs/design/first-run/motion.md §2.2, "B5").
        /** The lower edge waits this long while what was Booklight's fades where it stands, and only then draws in: it cuts no row and no cell. */
        const val LANDS_AFTER_MS = 80L
        /** The desk behind the panel, dimmed through the piece, lifts over this long once the key's step lands. */
        const val DESK_LIFTS_MS = 640
        /** The highlight leaves for the key's armed answer this long after the landing began: once what it stood on has begun to fade. */
        const val GLIDE_AFTER_MS = 32L
        /** Arrived, it is the answers' own highlight within this long: the time that highlight takes to come. */
        const val TAKES_OVER_MS = 120
```

Find:

```kotlin
val LocalMotion = staticCompositionLocalOf { Motion(true) }
```

Make it:

```kotlin
/** The seam the glass opens out of, growing to its length. */
internal val SEAM_GROWS = CubicBezierEasing(0.2f, 0f, 0f, 1f)
/** [SEAM_GROWS] run backwards. */
internal val SEAM_DRAWS_IN = CubicBezierEasing(1f, 0f, 0.8f, 1f)
/** The glass closing: the opening spring's way from the seam to full width, backwards (it lands softly on the seam; the bounce is left out). */
internal val FOLDS = CubicBezierEasing(0.45f, 0f, 0.4f, 1f)

/**
 * First run's welcome, "Lights on" (docs/design/first-run/motion.md §2.1): one clock, and every mark of it here, in
 * ms. The paper counts from the first frame of an opening at Medium, whose gate is at 257 ms; the clock here starts
 * at the gate, whatever the speed of the opening, so each mark is the paper's less [GATE]. A mark named `…_MS` is a
 * length of time; one named `…_AFTER_MS` is counted from the beginning of the hand-over; the others are moments.
 *
 * Its curves are the panel's own: fades on the standard curve, the lamp's light flooding the field and the seam of
 * light falling on [SEAM_GROWS], the light opening on [Motion.OPENS] (the glass's own unfold, a second time), the
 * shaft turning on [FOLDS], the head's light closing on [SEAM_DRAWS_IN], the handle on `place`, a tool on `pop`.
 */
object Lights {
    /** Where the paper's clock has the gate. */
    const val GATE = 257
    /** Night falls from the gate on, slower than the glass grows: the veil, the desk behind, the outline. */
    const val NIGHT_MS = 420
    /** The caret blinks off in the falling night, as a caret does. Where it would come back, the lamp does. */
    const val CARET_OFF = 560 - GATE
    /** The lamp strikes: 60 % after [STRIKE_UP_MS], back to 35 % at [STRIKE_SAG_MS] (it has not caught), 100 % at [STRIKE_ON_MS], fast at first. A fade would be a dimmer, not a lamp. */
    const val STRIKE = 800 - GATE
    const val STRIKE_UP_MS = 30
    const val STRIKE_SAG_MS = 70
    const val STRIKE_ON_MS = 200
    /** The light floods the field from the caret to both ends. */
    const val FLOOD_AFTER_MS = 40
    const val FLOOD_MS = 260
    /** A seam of light falls under the lamp, */
    const val SEAM = 960 - GATE
    const val SEAM_MS = 160
    /** and opens out of itself to both sides, to its shape. */
    const val OPEN = 1180 - GATE
    const val OPEN_MS = 360
    /** The handle rises into the foot of the light, the cue on it. */
    const val HANDLE = 1420 - GATE
    const val HANDLE_MS = 140
    /** The five tools flick out of it, left to right, a beat apart; a glint crosses each disc once as it opens. */
    const val TOOLS = 1560 - GATE
    const val BEAT_MS = 66
    const val GLINT_AFTER_MS = 90
    const val GLINT_MS = 160
    /** It stands, to be read: one breath of the light, from here to the hand-over. */
    const val STANDS = 1540 - GATE
    /** The hand-over: Booklight presses the handle, and everything goes back up to the field. One leads at a time, each beginning before the last has ended. */
    const val HAND = 3200 - GATE
    const val PRESS_MS = 80
    const val RELEASE_MS = 120
    /** The tools fold back into the handle, right to left. */
    const val FOLD_EACH_MS = 30
    const val FOLD_MS = 120
    /** The shaft turns: its upper edge slides to the caret, the foot follows and rises, the sides close. The night begins to lift with it. */
    const val TURN_AFTER_MS = 160
    const val TURN_MS = 300
    const val FOOT_AFTER_MS = 220
    const val RISE_MS = 360
    const val LIFT_MS = 400
    /** The cue fades inside the handle; the handle sets off for row one's seat; the unlit words fade where they stand. */
    const val CUE_OUT_AFTER_MS = 200
    const val CUE_OUT_MS = 80
    const val GO_AFTER_MS = 240
    const val WORDS_OUT_AFTER_MS = 380
    const val WORDS_OUT_MS = 80
    /** The head's light closes on the caret from both ends: then all that is left of the lamp is the caret. */
    const val CLOSE_AFTER_MS = 440
    const val CLOSE_MS = 160
    /** The hand-over's length: at its end the show's first letter lands on the caret. */
    const val HAND_MS = 620
    /** A key puts the lamp out in its frame: what stands fades where it stands over [OUT_MS], and the night lifts over [DAWN_MS]. */
    const val OUT_MS = 80
    const val DAWN_MS = 160
    /** If the show cannot begin when its cue is due (this device's apps are still being read), the welcome stands on for this long; then the key's step lands. */
    const val WAITS_MS = 1000
}

val LocalMotion = staticCompositionLocalOf { Motion(true) }
```

- [ ] **Step 2: The glass uses them from there, in `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** One change.

Find:

```kotlin
/** The seam the glass opens out of, as a width. */
private val SEAM = 6.dp
private val SEAM_GROWS = CubicBezierEasing(0.2f, 0f, 0f, 1f)
/** [SEAM_GROWS] run backwards. */
private val SEAM_DRAWS_IN = CubicBezierEasing(1f, 0f, 0.8f, 1f)
/** The glass closing: the opening spring's way from the seam to full width, backwards (it lands softly on the seam; the bounce is left out). */
private val FOLDS = CubicBezierEasing(0.45f, 0f, 0.4f, 1f)
```

Make it:

```kotlin
/** The seam the glass opens out of, as a width. (Its curves, `SEAM_GROWS`, `SEAM_DRAWS_IN` and `FOLDS`, are in `Motion.kt`: first run's welcome makes the same moves with its light.) */
private val SEAM = 6.dp
```

- [ ] **Step 3: Each curve is said once, and the opening still uses all three**

```bash
grep -c "val SEAM_GROWS\|val SEAM_DRAWS_IN\|val FOLDS" app/src/main/java/io/github/kuscher/booklight/overlay/Motion.kt app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt
grep -c "easing = SEAM_GROWS\|easing = SEAM_DRAWS_IN\|easing = FOLDS" app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt
```

Expected: `…/Motion.kt:3`, `…/Panel.kt:0`, then `3`.

- [ ] **Step 4: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/Motion.kt app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt
git commit -m "First run: the welcome's clock and the landing's times are Motion's, and the glass's three curves with them"
```

### Task 6: What the show needs of this device: the cast, and the example flight's row

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/providers/AppsProvider.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/providers/Flights.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`

**Interfaces:**
- Consumes: core `Show.letter(names)`, `Show.APPS`, `Show.FLIGHT`, `Show.LOOKED`, `Show.NOTHING` (Task 3); `R.string.first_show_example`, `first_show_flight` (Task 4), `action_copy`; in `FlightsProvider` its private `airlines`, `text(id, vararg args)`, `headline(h: Headline, f: Flight, today: LocalDate)`, `badge(b: Badge)`, `stop(e: EndSays, today: LocalDate)` and the companion's `const val ID = "flights"`; core `FlightStatus.row(f, now)`, `Flights.read(text, airlines, today)`; in `BooklightApp`: `apps.ready`, `commands.ready`, `engine.search(q: Query, limit: Int)`, `engine.scope(key)`, `icons`, `flights`, and `firstExample()` as the model of a job that is worked out off the main thread.
- Produces:
  - `AppsProvider.names(): List<String>`: every app's name, as it is shown.
  - `FlightsProvider.example(f: Flight, now: Instant, label: String, source: String): Result` and `FlightsProvider.EXAMPLE = "example:flight"`.
  - `class BooklightApp.Cast(val letter: String, val apps: List<Result>, val flight: Result, val scope: Scope, val keyword: String, val grid: Result, private val copy: String, val comma: Boolean)` with `fun sum(text: String, answer: String): Result`.
  - `suspend fun BooklightApp.cast(): Cast?`.

The show performs with the panel's real rows, from data of its own (`eng.md` §12.2, §13.1; the notes of part 3's review, point 4: "draw the show from its own data"). Its rows are made here, once, off the main thread, before the show begins:

- **This device's apps.** The letter is the one most app names here begin with (core `Show.letter`). The rows are the engine's own for that letter, the first five that are apps, each as typing would find it: its icon, its name, the same strip. But every action carries `Show.NOTHING` in place of what it would do, so nothing of the show can be run by any way. The icons are loaded before, so that no icon comes after its row. Nothing is asked of the engine that typing the letter would not ask, and nothing is run, sent, learned, logged or kept by it.
- **The example flight.** `FlightsProvider.example` says core's `Show.FLIGHT` with the words of a real flight's row (the same private functions `full()` uses), at the moment `Show.LOOKED`. It has no actions: nothing of an example can be opened, copied or pinned, and a row without actions shows its label, "Example", at its right end where its strip would stand. Its id is one the provider never made: none of `waits`, `gone`, `retry`, `told` or `answer` knows it, nothing of it is kept, no connection is opened, and with a key in or without it is the same row. Each end's time is said in its airport's own clock and without a day; the minutes do not count down while it shows.
- **The emoji grid** is the emoji scope's own row for an empty text, with nothing to run.
- **The sum's row** is made as the calculator's own is, for the text and the answer the script names.

Where the device has no app whose name begins with a letter, or the grid cannot be made, there is no cast, and no show: the welcome then lands in the key's step (Task 7).

There is no unit test for this task: the app module has no test source set. What it decides is core's (`ShowTest`); its check here is that it builds, and what the rows look like is Task 12's device checks 12 to 15.

- [ ] **Step 1: Every app's name, in `app/src/main/java/io/github/kuscher/booklight/providers/AppsProvider.kt`.** One change.

Find:

```kotlin
    val count: Int get() = index.size
```

Make it:

```kotlin
    val count: Int get() = index.size
    /** Every app's name, as it is shown: for what is counted over all of them (first run's show types the letter most of them begin with). */
    fun names(): List<String> = index.map { it.label }
```

- [ ] **Step 2: The example's row, in `app/src/main/java/io/github/kuscher/booklight/providers/Flights.kt`.** Two changes: the function after `full`, and its id in the companion.

Find:

```kotlin
    /** A time in its airport's own clock, with the day when it is not the user's today: "10:55 AM", "Fri 10:55 AM". */
```

Make it:

```kotlin
    /**
     * First run's example flight (core `Show.FLIGHT`): the row of a flight in the air, said with the words of a real
     * one, from a flight that is held in the app. Nothing is asked of the service for it, with a key in or without, and
     * nothing of it is kept: its id is none this provider made, so it is never looked up, said again, pinned or taken
     * for the real flight of that number. It has no actions: an example can be neither opened nor copied, and its
     * [label] ("Example") stands at its right end where a row's strip would. [source] is what the footer says while it
     * is selected. [now]: the moment it is looked at, the example's own, so the row is the same on every day and in
     * every time zone; each end's time is said without a day, and the minutes do not count down.
     */
    fun example(f: Flight, now: Instant, label: String, source: String): Result {
        val shown = Flights.read(f.number, airlines, f.from.time?.toLocalDate() ?: LocalDate.now())?.shown ?: f.number
        val caption = text(R.string.flight_who, text(R.string.flight_who, shown, f.airline), text(R.string.flight_route, f.from.place, f.to.place))
        val row = FlightStatus.row(f, now)
        fun end(e: EndSays) = stop(e, e.time?.toLocalDate() ?: LocalDate.now())
        return Result(
            id = EXAMPLE, provider = ID, kind = Kind.OTHER, title = caption, icon = Icon.Symbol("plane"), score = 1.0, learnable = false, label = label, actions = emptyList(),
            body = Body.Flight(
                caption, headline(row.headline, f, LocalDate.ofInstant(now, ZoneId.of("UTC"))), row.badge?.let(::badge), row.badge?.tone ?: Tone.PLAIN, row.share?.toFloat(),
                end(row.from), end(row.to), flight = EXAMPLE, answer = 1, counts = false, source = source, phase = row.phase,
            ),
        )
    }

    /** A time in its airport's own clock, with the day when it is not the user's today: "10:55 AM", "Fri 10:55 AM". */
```

Find:

```kotlin
        const val ID = "flights"
```

Make it:

```kotlin
        const val ID = "flights"
        /** The id of first run's example flight: none a flight number makes. */
        const val EXAMPLE = "example:flight"
```

- [ ] **Step 3: The cast, in `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`.** Three changes: two imports; the cast, before `providers`; and one number in the companion.

Find:

```kotlin
import io.github.kuscher.booklight.core.SearchEngine
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Show
```

Find:

```kotlin
    /** Every source of results. A new ability is one more line here. */
    lateinit var providers: List<Provider> private set
```

Make it:

```kotlin
    /**
     * What first run's show needs of this device (core `Show`): the [letter] Booklight types first and the [apps] it
     * finds, the example [flight]'s row, and the emoji grid ([scope], its [keyword], its row [grid]). Every row is the
     * panel's own, as typing would find it, but carries nothing to run. [comma]: the user's language writes 1,5.
     */
    class Cast(val letter: String, val apps: List<Result>, val flight: Result, val scope: Scope, val keyword: String, val grid: Result, private val copy: String, val comma: Boolean) {
        /** The answer's row for the sum [text], as the calculator's own row is, with nothing to run. */
        fun sum(text: String, answer: String) = Result(
            id = "calc", provider = "calc", kind = Kind.ANSWER, title = answer, subtitle = text.trim(), icon = io.github.kuscher.booklight.core.Icon.Symbol("calc"),
            score = 1.0, answer = answer, learnable = false, actions = listOf(io.github.kuscher.booklight.core.Action("copy", copy, Show.NOTHING)),
        )
    }

    /**
     * The cast for first run's show, worked out off the main thread once the app list and other apps' commands have
     * been read. The apps are the engine's own rows for the letter most app names here begin with, the first five
     * that are apps, with their icons loaded: nothing is asked that typing that letter would not ask, and nothing is
     * run, sent, learned, logged or kept by it. Every action of every row carries `Show.NOTHING` in place of what it
     * would do. Null where this device has nothing to show: no app whose name begins with a letter, or no emoji grid.
     */
    suspend fun cast(): Cast? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        apps.ready.await(); commands.ready.await()
        fun idle(r: Result) = r.copy(actions = r.actions.map { it.copy(effect = Show.NOTHING, done = null) }, nudge = null)
        val letter = Show.letter(apps.names()) ?: return@withContext null
        val found = engine.search(Query(letter), CAST_ROWS).filter { it.provider == AppsProvider.ID && it.kind == Kind.APP }.take(Show.APPS).map(::idle)
        if (found.isEmpty()) return@withContext null
        val scope = engine.scope("emoji") ?: return@withContext null
        val keyword = scope.keywords.firstOrNull() ?: return@withContext null
        val grid = scope.rows("").firstOrNull() ?: return@withContext null
        val flight = flights.example(Show.FLIGHT, Show.LOOKED, getString(R.string.first_show_example), getString(R.string.first_show_flight))
        val cache = icons ?: io.github.kuscher.booklight.ui.AppIcons(this@BooklightApp).also { icons = it }
        val px = (48 * resources.displayMetrics.density).toInt()
        for (r in found) (r.icon as? io.github.kuscher.booklight.core.Icon.App)?.let { cache.load(it, px) }
        val comma = java.text.DecimalFormatSymbols.getInstance(resources.configuration.locales[0]).decimalSeparator == ','
        Cast(letter, found, flight, scope, keyword, idle(grid), getString(R.string.action_copy), comma)
    }

    /** Every source of results. A new ability is one more line here. */
    lateinit var providers: List<Provider> private set
```

Find:

```kotlin
        private const val SETTINGS = "com.android.settings"
```

Make it:

```kotlin
        private const val SETTINGS = "com.android.settings"
        /** How many rows the engine is asked for to find the show's five apps among them: a letter also finds settings pages and keywords. */
        private const val CAST_ROWS = 40
```

- [ ] **Step 4: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/providers/AppsProvider.kt app/src/main/java/io/github/kuscher/booklight/providers/Flights.kt app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt
git commit -m "First run: the show's cast: this device's apps with nothing to run, the example flight's row, the emoji grid (nothing plays them yet)"
```

### Task 7: The model plays the opening piece: its state, the show's performer, its keys

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt`

**Interfaces:**
- Consumes: core `FirstRun.Playing`, `Press`, `Ends`, `pressed(playing, press)` (Task 2), `FirstRun.shown(s)` (part 1); core `Show.script(sum, keyword, comma)`, `Show.Step`, `Show.Still`, `Show.until(script, still)` (Task 3); `R.string.first_hello_title` (Task 4), `first_sum_example`; `Motion.LANDS_AFTER_MS` (Task 5); `BooklightApp.cast(): Cast?` and `Cast` (Task 6). In `OverlayModel` (parts 2 and 3): `due`, `stage`, `step { … }`, `first()`, `rearm`, `cameAt`, `seen`, `SEEN_MS`, `resuggest()`, `stops(r)`, `current`, the private `For(key, text, act)` and `resultsFor`, `typist`, `filledAt`.
- Produces, in `OverlayModel`:
  - `var playing: FirstRun.Playing?` (the setter is the model's own); `var began: Boolean` (its setter too); `var greets: Boolean`; `var cast: BooklightApp.Cast?`; `var cued: Boolean`; `var cueUp: Boolean`; `var laps: Int`; `var rounds: Int`; `var standsAt: Float?`; `var heldHeight: Dp?`; `var hold: (Long) -> Long`.
  - `fun begin(at: Float? = null)`: the opening piece begins with this panel.
  - `fun press(press: FirstRun.Press, again: Boolean = false): Boolean`: a key or a click while it plays; true: used up.
  - `fun show(only: Show.Still? = null)`: the welcome has handed over; Booklight performs.
  - `fun land()`: the key's step is set down.
  - `stage` is null while the welcome or the show plays; `firstHint` is empty in the welcome and the welcome's title where `greets`; `keyword` is null while the piece plays.
  - `Metrics.welcome` (400 dp) and `Metrics.height(m)` for the welcome and for a height that is held.

The model says where the opening piece is (`playing`) and performs the show. Nothing draws the welcome yet and nothing begins the piece: until Task 10 calls `begin()`, `playing` is null in every panel and no user sees a change.

**The show performs, it runs nothing.** A cue of core's script sets the field's text, the list, the selection, the arming or the grid's cell, exactly as a key of the user's would leave them, and the panel's own parts draw what they are given: the cascade, the travelling pill, the strip, the answer rolling, the plane flying to its place, the grid's wave. The rows are the cast's (Task 6): none carries anything to run. And the model takes no key for them: `enter`, `runRow` and `went` return at once while the piece plays, the list is said to be for `first:show` (never for what the field holds), and what Booklight typed is nobody's last text. The engine is asked nothing, so nothing is looked up and no suggestion is fetched.

**The keys** are core's (`FirstRun.pressed`). `press` returns true where the piece used the press up. A typed character ends the piece in that call and returns false: the character then goes to the field as on any day, and because the model's text is empty by then, it is the whole text.

**A typed character** ends the piece in `typed`: what was Booklight's goes in the same change, so no row of the show ever stands under a field the user is typing into, and no key can be pressed on one.

**The landing.** `land()` empties the field and the list in one change and says `LANDING`; the screen that is due (the key's) then stands, because `stage` is no longer held back. It "came" at that moment: `cameAt` is set, so its keys count 350 ms later (`seen`, part 3), and for exactly that long the piece is still `LANDING` and uses every press up. A key pressed during the welcome or the show therefore never reaches the armed answer of the key's step, and neither does one pressed while that step is still arriving. Where the piece ended for a typed character, the key's step comes into view only when the field is empty again: `cameAt` is set then.

**Seen once.** `FirstRun.shown` is written when the piece ends (the landing, a typed character) or has played for two seconds: a panel that loses the focus in its first moment has not used it up (`eng.md` §12.2).

**The height.** The welcome is 468 dp: `Metrics.welcome` (400) under the field. During the show the height is the list's own, as every day. At the landing the height is held for 80 ms (`Motion.LANDS_AFTER_MS`) while what was Booklight's fades, and then goes to the key's 252 dp on `place`.

There is no unit test for this task: the app module has no test source set. What it decides is core's and is tested there (`FirstRunPlayingTest`, `ShowTest`); its check here is that it builds, and Task 12's device checks 16 to 23.

- [ ] **Step 1: The heights, in `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt`.** Three changes: the welcome's height; `height` asking for it and for a height that is held; and the bracket that closes what `height` now wraps.

Find:

```kotlin
    val pad = 8.dp
    val footer = 36.dp
```

Make it:

```kotlin
    /**
     * First run's welcome under the field: with the field the glass is 468 dp high, the most that fits every Googlebook
     * (core `FirstRun.WELCOME_SCREEN_DP` is reckoned from it, so a change here needs a change there; docs/design/first-run/design.md §2 and §3).
     */
    val welcome = 400.dp
    val pad = 8.dp
    val footer = 36.dp
```

Find:

```kotlin
    fun height(m: OverlayModel): Dp = field + when {
        m.results.isNotEmpty() -> pad + listHeight(m.results) + pad + footer
```

Make it:

```kotlin
    fun height(m: OverlayModel): Dp = m.heldHeight ?: (field + when {
        // First run's welcome: the glass is a stage. (Its show is the list's own height, below; at its landing the height is held for a moment.)
        m.playing == FirstRun.Playing.WELCOME -> welcome
        m.results.isNotEmpty() -> pad + listHeight(m.results) + pad + footer
```

Find:

```kotlin
        m.otherAct != null -> pad + row + pad
        else -> 0.dp
    }
```

Make it:

```kotlin
        m.otherAct != null -> pad + row + pad
        else -> 0.dp
    })
```

- [ ] **Step 2: The piece's state, and what waits for it, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Sixteen changes, in the file's order: an import; `keyword`; the piece's state before the stage's; `stage`; the piece's functions before the lessons; `firstHint`; the tip, the usual rows and the copy's line stay away; `type`; the empty field's branch of `search`; `went`; `enter`; `runRow`; `keep`; two values in the companion.

Find:

```kotlin
import io.github.kuscher.booklight.core.SearchEngine
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Show
```

Find:

```kotlin
    val keyword: Scope? by derivedStateOf { if (chip != null || touched || opened != null) null else app.engine.keywordScope(query) }
```

Make it:

```kotlin
    // (Not while first run's show types: a keyword Booklight types is not the user's to make a chip of.)
    val keyword: Scope? by derivedStateOf { if (playing != null || chip != null || touched || opened != null) null else app.engine.keywordScope(query) }
```

Find:

```kotlin
    var carries by mutableStateOf(false)

    // ---- first run: its stage under the empty field
```

Make it:

```kotlin
    var carries by mutableStateOf(false)

    // ---- first run: the opening piece (the welcome, then the show)

    // (Its state stands here, before the stage's: the stage asks it, and is first asked while this object is made.)
    /** Where the opening piece is while it plays (core `FirstRun.Playing`); null: it does not play, or is over. */
    var playing by mutableStateOf<FirstRun.Playing?>(null); private set
    /** This panel began with the opening piece: what draws the piece is composed in this panel, and in no other. */
    var began by mutableStateOf(false); private set
    /**
     * The opening piece is this opening's and is not played (the system's animations are off, or a screen reader is on:
     * core `FirstRun.greets`): the welcome's title greets as the field's placeholder. Set by the activity.
     */
    var greets by mutableStateOf(false)
    /** What the show needs of this device (`BooklightApp.cast`); null until it is worked out, and where the device has nothing to show. */
    var cast by mutableStateOf<BooklightApp.Cast?>(null); private set
    private var casting: Job? = null
    private var performer: Job? = null
    /** The cue was given (Enter in the welcome, a click on its handle): the welcome hands over as soon as the show can begin. */
    var cued by mutableStateOf(false); private set
    /** The welcome's handle stands and carries the cue: a click on it is the cue. Set by the welcome. */
    var cueUp by mutableStateOf(false)
    /** How often the show has asked for the light to set off round the outline: the panel runs one lap for each. */
    var laps by mutableIntStateOf(0); private set
    /** How often the piece has begun in this panel: once, but for a debug hook that plays it again. The welcome's clock starts anew with each. */
    var rounds by mutableIntStateOf(0); private set
    /** Debug builds: the welcome's clock stands at this many ms of the paper's own clock (`./bl debug first welcome at MS`); null: it runs. */
    var standsAt by mutableStateOf<Float?>(null); private set
    /** The height the glass keeps for a moment while what was Booklight's fades at the landing: the lower edge draws in after it (`Metrics.height`). */
    var heldHeight by mutableStateOf<Dp?>(null); private set
    /** How long a wait of the piece really is (`Motion.hold`: a debug build's slow motion stretches it). Set by the activity. */
    var hold: (Long) -> Long = { it }
    /** The piece ended for a typed character: the key's step has not been in view yet, and comes when the field is empty again. */
    private var unseen = false

    // ---- first run: its stage under the empty field
```

Find:

```kotlin
    val stage: FirstRun.Screen? by derivedStateOf { due?.takeIf { it != FirstRun.Screen.C && query.isEmpty() && chip == null } }
```

Make it:

```kotlin
    // (Not while first run's welcome or its show plays: the stage is what they land in.)
    val stage: FirstRun.Screen? by derivedStateOf {
        due?.takeIf { it != FirstRun.Screen.C && query.isEmpty() && chip == null && (playing == null || playing == FirstRun.Playing.LANDING) }
    }
```

Find:

```kotlin
    // ---- first run: the lessons
```

Make it:

```kotlin
    // ---- first run: the opening piece, played

    /**
     * The opening piece begins with this panel (core `FirstRun.overture` said so): said by the activity before anything
     * asks what stands. The welcome stands first; what the show needs of this device is worked out meanwhile, off the
     * main thread. [at]: debug builds, the welcome's clock stands at that moment and nothing follows.
     */
    fun begin(at: Float? = null) {
        performer?.cancel(); performer = null
        if (playing == FirstRun.Playing.SHOW) clear()
        began = true; playing = FirstRun.Playing.WELCOME; cued = false; cueUp = false; standsAt = at; unseen = false; heldHeight = null
        rounds++
        if (cast == null && casting == null) casting = scope.launch { cast = app.cast() }
        // It does not come again once it has played for two seconds, or has ended: a panel that loses the focus in its first moment has not used it up.
        if (at == null) scope.launch { delay(SHOWN_AFTER_MS); if (playing != null) shown() }
    }

    /** The piece has been shown: it does not come again in this run (core `FirstRun.shown`). Where there is no run, nothing is written. */
    private fun shown() { if (first().run != FirstRun.Run.NONE) step { FirstRun.shown(it) } }

    /** What was Booklight's goes from the field and from the list, in one change. */
    private fun clear() {
        chip = null; word = null; via = null; origin = null; act = null; held = null; touched = false
        query = ""; results = emptyList(); selected = 0; armed = 0; cell = 0
        resultsFor = For(null, ""); whenReady = null
    }

    /**
     * A key or a click while the piece plays (core `FirstRun.pressed`). True: the piece has used it up, and it reaches
     * nothing else. False: nothing plays; or it is a typed character, the piece is over, and the character goes to the
     * field as on any day. [again]: a held key repeating: one press, one step.
     */
    fun press(press: FirstRun.Press, again: Boolean = false): Boolean {
        val on = playing ?: return false
        if (again && press != FirstRun.Press.TYPES) return true
        when (FirstRun.pressed(on, press)) {
            FirstRun.Ends.NOTHING -> {}
            FirstRun.Ends.BEGIN -> cued = true
            FirstRun.Ends.LAND -> land()
            FirstRun.Ends.TYPE -> { typed(on); return false }
        }
        return true
    }

    /**
     * The piece ends in this call for a typed character. What was Booklight's goes from the field and from the list (its
     * rows fade where they stand, as rows do), so the character is the whole text and its list lands as any list. The
     * key's step comes when the field is empty again. Should no character follow after all (a key that only begins
     * one), the field is empty now and the key's step stands at once: it came in this moment, and its keys count a
     * moment later ([seen]).
     */
    private fun typed(was: FirstRun.Playing) {
        performer?.cancel(); performer = null
        playing = null; cued = false; cueUp = false; standsAt = null; heldHeight = null
        if (was == FirstRun.Playing.SHOW) clear()
        unseen = true
        cameAt = SystemClock.uptimeMillis()
        shown()
    }

    /**
     * The welcome has handed over: Booklight performs the show from core's script (`Show.script`), a cue at a time.
     * Nothing in it is run, looked up or fetched: the rows are the cast's, which carry nothing to run, and the engine is
     * not asked. Without a cast there is no show: the key's step lands. [only]: debug builds, the show up to that moment
     * at once, and then it stands.
     */
    fun show(only: Show.Still? = null) {
        if (playing != FirstRun.Playing.WELCOME) return
        val c = cast
        if (c == null) {
            // (Only a debug hook asks before the cast is there: the welcome itself waits for it.)
            if (casting?.isActive == true) scope.launch { casting?.join(); show(only) } else land()
            return
        }
        playing = FirstRun.Playing.SHOW; cued = false; cueUp = false; standsAt = null
        val script = Show.script(app.getString(R.string.first_sum_example), c.keyword, c.comma)
        if (only != null) { Show.until(script, only).forEach { perform(it.step, c) }; return }
        performer = scope.launch {
            var at = 0
            for (cue in script) {
                if (cue.at > at) { delay(hold((cue.at - at).toLong())); at = cue.at }
                perform(cue.step, c)
            }
        }
    }

    /** One step of the show: the field, the list, the selection, the arming or the grid's cell as a key of the user's would leave them. */
    private fun perform(step: Show.Step, c: BooklightApp.Cast) {
        fun list(rows: List<Result>) { results = rows; selected = 0; armed = 0; cell = 0; resultsFor = For(SHOWS, ""); whenReady = null }
        when (step) {
            Show.Step.Letter -> query = c.letter
            is Show.Step.Typed -> query = step.text
            Show.Step.Apps -> list(c.apps)
            Show.Step.Down -> if (selected < results.lastIndex) { selected++; armed = current?.armed ?: 0 }
            Show.Step.Tab -> current?.let { r -> stops(r).let { st -> if (st.size > 1) armed = st[(st.indexOf(armed).coerceAtLeast(0) + 1) % st.size] } }
            is Show.Step.Sum -> list(listOf(c.sum(query, step.answer)))
            Show.Step.Flight -> list(listOf(c.flight))
            // The keyword's space: the keyword is the chip, the field is empty under it, and the list is its grid.
            Show.Step.Grid -> { chip = c.scope; word = c.keyword; query = ""; list(listOf(c.grid)) }
            is Show.Step.Cell -> (current?.body as? Body.Grid)?.let { g ->
                val to = cell + step.dx + step.dy * g.columns
                if (to in 0 until minOf(g.cells.size, g.columns * 5)) cell = to
            }
            Show.Step.Lap -> laps++
            Show.Step.Land -> land()
        }
    }

    /**
     * The key's step is set down, from wherever the piece is: the show's own last step, or a press that is not a typed
     * character. What was Booklight's goes from the field and the list in one change, and the screen that is due stands.
     * It came now, not when the stored state first said so: its keys count once it has been in view for a moment
     * ([seen]), and for that long the piece is still [FirstRun.Playing.LANDING] and uses every press up.
     */
    fun land() {
        val was = playing ?: return
        if (was == FirstRun.Playing.LANDING) return
        performer?.cancel(); performer = null
        // The lower edge waits while what was Booklight's fades where it stands: it cuts no row and no cell.
        if (results.isNotEmpty()) { heldHeight = Metrics.height(this); scope.launch { delay(hold(Motion.LANDS_AFTER_MS)); heldHeight = null } }
        clear()
        playing = FirstRun.Playing.LANDING; cued = false; cueUp = false; standsAt = null; unseen = false
        resuggest()
        cameAt = SystemClock.uptimeMillis()
        shown()
        scope.launch { delay(SEEN_MS); if (playing == FirstRun.Playing.LANDING) playing = null }
    }

    // ---- first run: the lessons
```

Find:

```kotlin
        FirstRun.Screen.L4 -> app.getString(R.string.first_sum_example)
        else -> if (ended) app.getString(R.string.first_end_hint) else null
    }
```

Make it:

```kotlin
        FirstRun.Screen.L4 -> app.getString(R.string.first_sum_example)
        else -> when {
            // The welcome has no placeholder: the field is the lamp's head, and nothing is written on it.
            playing == FirstRun.Playing.WELCOME -> ""
            ended -> app.getString(R.string.first_end_hint)
            // Where the opening piece is this opening's and is not played, its title greets here, over the key's step.
            greets && stage?.step == FirstRun.Step.KEY -> app.getString(R.string.first_hello_title)
            else -> null
        }
    }
```

Find:

```kotlin
        if (demo || guided || typedYet || tip != null || copy != null || query.isNotEmpty() || chip != null || results.isNotEmpty() || !settings.tips || stage != null) return
```

Make it:

```kotlin
        if (demo || guided || typedYet || tip != null || copy != null || query.isNotEmpty() || chip != null || results.isNotEmpty() || !settings.tips || stage != null || playing != null) return
```

Find:

```kotlin
        if (Under.choose(untouched, stage != null, fresh, settings.zero, zeroSeats, tip = false, last) != Under.What.USUAL) return false
```

Make it:

```kotlin
        // (First run's opening piece has the place as a stage has it.)
        if (Under.choose(untouched, stage != null || playing != null, fresh, settings.zero, zeroSeats, tip = false, last) != Under.What.USUAL) return false
```

Find:

```kotlin
        if (demo || guided || copyGone || (typedYet && !back) || tip != null || query.isNotEmpty() || chip != null || results.isNotEmpty() || stage != null) return false
```

Make it:

```kotlin
        if (demo || guided || copyGone || (typedYet && !back) || tip != null || query.isNotEmpty() || chip != null || results.isNotEmpty() || stage != null || playing != null) return false
```

Find:

```kotlin
    fun type(text: String) {
        if (text == query) return
```

Make it:

```kotlin
    fun type(text: String) {
        // First run's opening piece ends for a typed character, by whichever way it comes (a key, the input method, a debug hook).
        if (playing != null) press(FirstRun.Press.TYPES)
        if (text == query) return
```

Find:

```kotlin
            // First run's choices, where their turn has come (a lesson's list has just given way; or it came while something
            // was typed): they stand now that the field is empty.
            offerChoices()
```

Make it:

```kotlin
            // The key's step, where first run's opening piece ended for a typed character: it comes into view now, with the
            // empty field, and takes a key a moment later ([seen]).
            if (unseen) { unseen = false; cameAt = SystemClock.uptimeMillis() }
            // First run's choices, where their turn has come (a lesson's list has just given way; or it came while something
            // was typed): they stand now that the field is empty.
            offerChoices()
```

Find:

```kotlin
        if (demo || !onScreen) return
```

Make it:

```kotlin
        if (demo || playing != null || !onScreen) return
```

Find:

```kotlin
        if (typist != null) return          // Booklight is still typing: there is nothing to run yet
```

Make it:

```kotlin
        if (playing != null) return         // first run's opening piece: nothing of it is the user's to run but its cue ([press])
        if (typist != null) return          // Booklight is still typing: there is nothing to run yet
```

Find:

```kotlin
    fun runRow(n: Int, run: (Result, Action) -> Unit) {
        if (!onScreen) return
```

Make it:

```kotlin
    fun runRow(n: Int, run: (Result, Action) -> Unit) {
        if (!onScreen || playing != null) return
```

Find:

```kotlin
    fun keep() {
        hideTip()
```

Make it:

```kotlin
    fun keep() {
        hideTip()
        if (playing != null) return         // what Booklight typed in first run's show is nobody's last text
```

Find:

```kotlin
        /** What the switch of first run's choices does: caught by the panel before anything runs. */
        private val USUAL = Effect.Internal("usual")
```

Make it:

```kotlin
        /** What the switch of first run's choices does: caught by the panel before anything runs. */
        private val USUAL = Effect.Internal("usual")
        /** First run's opening piece does not come again once it has played for this long, or has ended. */
        private const val SHOWN_AFTER_MS = 2000L
        /** What the list is said to be for while first run's show performs: never what the field holds, so nothing takes its rows for the rows of what is typed. */
        private const val SHOWS = "first:show"
```

- [ ] **Step 3: Nothing begins the piece yet, and the stage's first reader comes after the piece's state**

```bash
grep -n "begin(" app/src/main/java/io/github/kuscher/booklight/overlay/*.kt | grep -v "fun begin" || echo NOT-BEGUN
awk '/var playing by mutableStateOf/ { p = NR } /val stage: FirstRun.Screen\? by derivedStateOf/ { s = NR } /if \(!demo && stage != null\) resuggest\(\)/ { i = NR } END { print (p < s && s < i) ? "IN-ORDER" : "OUT-OF-ORDER" }' app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
```

Expected: `NOT-BEGUN`, then `IN-ORDER` (a property that is read while the object is made must be made before its reader: `stage` reads `playing`, and the model's own `init` reads `stage`).

- [ ] **Step 4: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt
git commit -m "First run: the model plays the opening piece: where it is, the show performed from its script with nothing to run, its keys, the landing in the key's step (nothing begins it yet)"
```

### Task 8: The glass at night, and the field and the footer while the piece plays

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Glass.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/ui/Theme.kt`

**Interfaces:**
- Consumes: `OverlayModel.playing`, `began` (Task 7); core `FirstRun.Playing`; `R.string.first_skip` (part 3); `Symbols.booklight`; in `Glass.kt` the shader `GLASS` and `fun Modifier.glass(tint, radiusPx, density, run, glow, tail, dark, solid)`; in `ui/Theme.kt` the private `OwnLight`.
- Produces:
  - `Glass.kt`: `val NIGHT: Color` (#0A0C12), `const val NIGHT_VEIL = 0.80f`, `const val NIGHT_DESK = 0.50f`; `class Night { var veil: Float; var rim: Float }`; `Modifier.glass(…, veil: () -> Float = { 0f }, rim: () -> Float = { 0f })`: two more parameters at the end, with defaults, so its one caller compiles unchanged until Task 10.
  - `Field.kt`: while the welcome plays the field shows no mark, no placeholder (the model says so), no caret of its own and no `esc` cap of its own (the welcome draws its own caret and cap, in the lamp's colours: Task 9); while the show plays the mark is Booklight's own.
  - `Footer.kt`: while the piece plays the word beside the `esc` cap is "Skip".
  - `ui/Theme.kt`: `@Composable fun lightScheme(tint: Boolean): ColorScheme`.

**The night** (`design.md` §3, the table's rows "The night" and "The outline"). One night for both themes: the veil goes to #0A0C12 at 0.80 (by day it is white at 0.42 or near-black at 0.50), and the outline, which is the glass shader's own line, goes to full white (by day 0.80 or 0.44). Both are the glass's, so the glass is told: `veil` is how far the veil has gone to night, `rim` how far the outline has, each 0 to 1, read where the glass is drawn, so a frame of the night lays nothing out and composes nothing. They are two numbers because a typed key puts the lamp out in its frame: the veil is then the theme's own at once (the field must show the typed character in the theme's ink), while the outline relaxes with the rest of the night.

The shader gains one uniform, `night`, and one use of it, in the line that says how bright the outline is. **The shader is compiled on the device, not by the build: the two changed lines must be exactly as written here.** A mistake in them would stop every panel from opening, and only Task 12's first check would find it.

**The field** (`design.md` §3: "the head", "the caret", "the `esc` cap", "the mark's seat"). In the welcome the field is the lamp's head and the whole panel is the mark: the seat is empty, and no placeholder is written (Task 7's `firstHint` is empty then). The field's own caret and `esc` cap are in the theme's colours, which are wrong on the night and on the lit head, so they step back and the welcome draws both in its own. From the show's first letter the seat holds Booklight's mark, which says who is typing; when the chip takes the seat and when the key's step lands, the seat changes as it does every day.

In a panel that did not begin with the piece (`began` is false: every panel of every other day) the mark is drawn as before, by the same code.

There is no unit test for this task: the app module has no test source set. Its check here is that it builds; what it looks like is Task 12's device checks 1 and 3 to 9.

- [ ] **Step 1: The night, in `app/src/main/java/io/github/kuscher/booklight/overlay/Glass.kt`.** Five changes: the imports; the night's values and the two numbers the glass is told, before the shader's comment; the uniform; the outline's line; and the modifier.

Find:

```kotlin
import android.graphics.RuntimeShader
import androidx.compose.runtime.remember
```

Make it:

```kotlin
import android.graphics.RuntimeShader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
```

Find:

```kotlin
/**
 * The panel's glass, as a shader over the system's window blur: a flat veil (white in light theme,
```

Make it:

```kotlin
/** First run's welcome is one night in both themes: the veil's colour then, and how much of it lies over the blur (by day: white at 0.42, near-black at 0.50). */
val NIGHT = Color(0xFF0A0C12)
const val NIGHT_VEIL = 0.80f
/** How far the desk behind the panel dims in that night. */
const val NIGHT_DESK = 0.50f

/**
 * What first run's welcome asks of the glass, written for each frame and read where the glass is drawn: how far the
 * [veil] has gone to night and how far the outline has ([rim]: full white at 1), each 0 to 1. Two numbers: a typed key
 * puts the lamp out in its frame, and the veil is the theme's own at once while the outline relaxes with the rest.
 */
class Night {
    var veil by mutableFloatStateOf(0f)
    var rim by mutableFloatStateOf(0f)
}

/**
 * The panel's glass, as a shader over the system's window blur: a flat veil (white in light theme,
```

Find:

```kotlin
uniform float solid;      // 1: no blur behind, so stay opaque
```

Make it:

```kotlin
uniform float solid;      // 1: no blur behind, so stay opaque
uniform float night;      // first run's welcome: 0 by day, 1 at night, when the outline is full white
```

Find:

```kotlin
    float base = mix(0.80, 0.44, dark);
```

Make it:

```kotlin
    float base = mix(mix(0.80, 0.44, dark), 1.0, night);
```

Find:

```kotlin
fun Modifier.glass(tint: Color, radiusPx: Float, density: Float, run: () -> Float, glow: () -> Float, tail: () -> Float, dark: Boolean, solid: Boolean): Modifier = composed {
    val shader = remember { RuntimeShader(GLASS) }
    drawWithCache {
        val brush = ShaderBrush(shader)
        onDrawBehind {
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("radius", minOf(radiusPx, size.width / 2f, size.height / 2f))
            shader.setFloatUniform("density", density)
            shader.setFloatUniform("tint", tint.red, tint.green, tint.blue, tint.alpha)
```

Make it:

```kotlin
fun Modifier.glass(
    tint: Color, radiusPx: Float, density: Float, run: () -> Float, glow: () -> Float, tail: () -> Float, dark: Boolean, solid: Boolean,
    /** First run's welcome: how far the veil has gone to night, and how far the outline has gone to full white, each 0 to 1 ([Night]). */
    veil: () -> Float = { 0f }, rim: () -> Float = { 0f },
): Modifier = composed {
    val shader = remember { RuntimeShader(GLASS) }
    drawWithCache {
        val brush = ShaderBrush(shader)
        onDrawBehind {
            // The veil as it stands in this frame: the theme's own by day, on its way to the night's while first run's welcome says so.
            val dusk = veil().coerceIn(0f, 1f)
            val veiled = if (dusk > 0f) lerp(tint, NIGHT.copy(alpha = NIGHT_VEIL), dusk) else tint
            shader.setFloatUniform("night", rim().coerceIn(0f, 1f))
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("radius", minOf(radiusPx, size.width / 2f, size.height / 2f))
            shader.setFloatUniform("density", density)
            shader.setFloatUniform("tint", veiled.red, veiled.green, veiled.blue, veiled.alpha)
```

- [ ] **Step 2: The shader's two lines are as written, and its one new uniform is set once**

```bash
grep -c "^uniform float night;      // first run's welcome: 0 by day, 1 at night, when the outline is full white$" app/src/main/java/io/github/kuscher/booklight/overlay/Glass.kt
grep -c "^    float base = mix(mix(0.80, 0.44, dark), 1.0, night);$" app/src/main/java/io/github/kuscher/booklight/overlay/Glass.kt
grep -c 'setFloatUniform("night"' app/src/main/java/io/github/kuscher/booklight/overlay/Glass.kt
grep -c "^uniform " app/src/main/java/io/github/kuscher/booklight/overlay/Glass.kt
grep -c "setFloatUniform(" app/src/main/java/io/github/kuscher/booklight/overlay/Glass.kt
```

Expected: `1`, `1`, `1`, then `10` and `10`: ten uniforms are declared and ten are set, each once (a uniform that is declared and never set draws with 0; one that is set and not declared stops the panel).

- [ ] **Step 3: The light scheme, in `app/src/main/java/io/github/kuscher/booklight/ui/Theme.kt`.** Two changes: an import, and the function after `isDark`.

Find:

```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
```

Make it:

```kotlin
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
```

Find:

```kotlin
fun isDark(theme: String): Boolean = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
```

Make it:

```kotlin
fun isDark(theme: String): Boolean = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }

/**
 * The light scheme, whatever the theme is: first run's welcome is one night in both themes, and the one coloured
 * surface in it, the knife's handle, is the selection as light theme has it. The device's own colours when [tint] is
 * on, else Booklight's own.
 */
@Composable
fun lightScheme(tint: Boolean): ColorScheme = if (tint) dynamicLightColorScheme(LocalContext.current) else OwnLight
```

- [ ] **Step 4: The field, in `app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt`.** Five changes: two imports; the mark's seat; the caret; the `esc` cap.

Find:

```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
```

Make it:

```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
```

Find:

```kotlin
import io.github.kuscher.booklight.core.AppChip
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.AppChip
import io.github.kuscher.booklight.core.FirstRun
```

Find:

```kotlin
            else Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                if (model.settings.engine == "google") Text("G", color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.round, fontSize = 24.sp, fontWeight = FontWeight(600)))
                else Icon(Symbols.search, null, Modifier.size(22.dp), tint = scheme.onSurface.copy(alpha = SECOND))
            }
```

Make it:

```kotlin
            else Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                val engine: @Composable () -> Unit = {
                    if (model.settings.engine == "google") Text("G", color = scheme.onSurface, style = TextStyle(fontFamily = Fonts.round, fontSize = 24.sp, fontWeight = FontWeight(600)))
                    else Icon(Symbols.search, null, Modifier.size(22.dp), tint = scheme.onSurface.copy(alpha = SECOND))
                }
                // A panel that began with first run's opening piece: no mark in the welcome, where the whole panel is the mark;
                // Booklight's own while Booklight types, which says who is typing; the engine's again as the key's step lands.
                // Each comes up as a mark does here. In every other panel the mark is drawn as it always was.
                if (!model.began) engine()
                else AnimatedContent(model.playing?.takeIf { it != FirstRun.Playing.LANDING }, transitionSpec = { (scaleIn(motion.pop(), 0.6f) + fadeIn(motion.fade(110))) togetherWith fadeOut(motion.fade(80)) }, contentAlignment = Alignment.Center, label = "who") { who ->
                    when (who) {
                        FirstRun.Playing.WELCOME -> Spacer(Modifier.size(22.dp))
                        FirstRun.Playing.SHOW -> Icon(Symbols.booklight, null, Modifier.size(22.dp), tint = scheme.onSurface)
                        else -> engine()
                    }
                }
            }
```

Find:

```kotlin
                cursorBrush = SolidColor(scheme.primary),
```

Make it:

```kotlin
                // (In first run's welcome the caret is the lamp's switch, and the welcome draws it in the lamp's colours.)
                cursorBrush = SolidColor(if (model.playing == FirstRun.Playing.WELCOME) Color.Transparent else scheme.primary),
```

Find:

```kotlin
        val none = model.results.isEmpty()
```

Make it:

```kotlin
        // (Nor in first run's welcome: there the cap is drawn in the night's ink, and in the lamp's once the head is lit.)
        val none = model.results.isEmpty() && model.playing != FirstRun.Playing.WELCOME
```

- [ ] **Step 5: The footer's word for Esc, in `app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt`.** One change.

Find:

```kotlin
            // What Escape does now: cancel a waiting confirmation, go back from an answer to the rows it was asked from, or close.
            AnimatedContent(if (model.confirming) R.string.hint_cancel else if (model.escapeLeaves) R.string.hint_back else R.string.hint_close,
```

Make it:

```kotlin
            // What Escape does now: cancel a waiting confirmation, go back from an answer to the rows it was asked from, or close.
            // While first run's show plays it skips to the key's step.
            AnimatedContent(if (model.confirming) R.string.hint_cancel else if (model.escapeLeaves) R.string.hint_back else if (model.playing != null) R.string.first_skip else R.string.hint_close,
```

- [ ] **Step 6: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/Glass.kt app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt app/src/main/java/io/github/kuscher/booklight/ui/Theme.kt
git commit -m "First run: the glass can go to night, and the field and the footer step back for the opening piece (nothing begins it yet)"
```

### Task 9: The welcome, "Lights on": `overlay/Welcome.kt`

**Files:**
- Create: `app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt`

**Interfaces:**
- Consumes: `Lights`, `SEAM_GROWS`, `SEAM_DRAWS_IN`, `FOLDS`, `Motion.OPENS`, `Motion.DESK_LIFTS_MS`, `Motion(true).place()` and `.pop()`, `LocalMotion` (`slow`, `fade`) (Task 5); `OverlayModel.playing`, `rounds`, `standsAt`, `cast`, `cued`, `cueUp`, `show()`, `land()`, `settings.tint` (Task 7); `Night`, `NIGHT`, `NIGHT_VEIL`, `NIGHT_DESK`, `Look.dimDark`, `Look.dimLight`, `selectionFill(scheme, dark, glass)` (Task 8, `Glass.kt`); `lightScheme(tint)` (Task 8, `ui/Theme.kt`); `R.string.first_hello_title`, `first_hello_line`, `first_hello_cue` (Task 4); `Keycap("esc", wide = true)`, `Metrics.field`, `LocalDark`, `LocalGlass`, `Symbols.of(name)`, `Symbols.enter`, `Fonts.round`, `Fonts.text`.
- Produces: `@Composable fun Piece(model: OverlayModel, gate: Boolean, ends: () -> Float, night: Night, onDesk: (Float) -> Unit)`: what of the opening piece is drawn under the field and the rows, to be put in a layer as wide as the panel and 468 dp high (Task 10 does); it writes `night.veil` and `night.rim` for each frame and calls `onDesk` with how far the desk behind is to dim, 0 to 1. Also `internal fun onHandle(x: Float, y: Float): Boolean`: whether a point of the glass, in dp of a panel 720 wide, lies on the handle (a click there is the cue). Private to the file: `Welcome`, the frame's numbers `Lit`, the light `Shaft`, and the drawing helpers. Task 11 adds to `Piece`: its last line, `    Welcome(model, gate, ends, night, through, onDesk)`, is that task's "Find".

Nothing composes `Piece` yet: a user sees no change.

**What is drawn, and where** (`design.md` §3, "Lights on, in full", whose table this is; x from the panel's left edge, y from its top, in dp of a panel 720 wide; the file's constants carry the same numbers):

| Part | Place | Look |
| --- | --- | --- |
| The night | The whole glass, 720 × 468 | The veil to #0A0C12 at 0.80 and the outline to full white are the glass's own (Task 8); here the night is 0.6 deeper towards the corners |
| The head | The field's band, y 0 to 68, corners 32 | White at 0.92, spreading from the caret to both ends, its front 30 dp soft |
| The caret | x 72 to 74, y 20 to 48 | The theme's until the night, then white; off for a blink before the lamp; the night's colour in the lit head |
| The `esc` cap | The field's own place, its right edge at 700 | White in the night; the night's ink once the lit head is under it |
| The light | Upper edge y = 92, x 129 to 591; foot y = 448, x 32 to 688 | White, 0.34 under the lamp falling to 0.10 at the foot; sides and foot soft over 20 dp; a penumbra outside (0.05 to 0.02) and a core along the axis (0.16 to 0.03); twelve faint rays; it breathes once, by 6 % |
| The title | Centred on x = 360, box y 124 to 172; the rounded cut at 40 / 600, smaller where it would be wider than 470 | White at 0.38 where no light is, white at 1.0 with a little bloom where the light falls |
| The line | Centred on x = 360, box y 188 to 210; 17 / 500, one line | The same two layers |
| The tools | Five discs of 36 on stems of 64 from pivots at x = 288, 324, 360, 396, 432 on the handle's upper edge; open at 40° and 20° left of upright, upright, 20° and 40° right: their centres at (247, 323), (302, 312), (360, 308), (418, 312), (473, 323) | Disc white at 0.16 with a ring at 0.30, stem white at 0.60 and 4 wide, symbol white at 20 dp: `app`, `calc`, `plane`, `smile`, `key` |
| The handle | x 252 to 468, y 372 to 420, radius 24 | The light scheme's `secondaryContainer` at 0.92 with a white rim at 0.55: the one coloured surface |
| The cue | On the handle, centred on (360, 396): the words at 17 / 600, 8 dp, the Enter mark at 16 | The light scheme's `onSecondaryContainer` |

It is the plain shaft, as Alex settled it: no dust in the beam, and no lap of light round the outline while the welcome stands. The strike, the core and the soft sides stay: they are the shaft.

**When** (`motion.md` §2.1; the paper's ms from the first frame of an opening at Medium, and in brackets the clock's own, from the gate): the night falls from 257 (0) for 420 ms; the caret blinks off at 560 (303); the lamp strikes at 800 (543) and floods the head by 1,100; a seam of light falls from 960 (703) and opens from 1,180 (923) to its shape by 1,540; the handle rises at 1,420 (1,163); the tools flick out from 1,560 (1,303), 66 ms apart; it stands, to be read; at 3,200 (2,943) Booklight presses the handle and everything goes back up: the tools fold, the shaft turns into the caret, the handle travels to row one's seat (x 8 to 712, y 76 to 132), the head's light closes on the caret; 620 ms later the show's first letter lands.

**How it is drawn.** With paths, gradients and blend modes, on one canvas: not with a second shader. The light is the prototype's own recipe (`first-run.html`, `shaft`): a layer that holds the fall-off down the shaft and the rays, of which only what lies inside the light's four soft edges is kept (`BlendMode.DstIn` under a gradient across each edge). The lit words are a layer of white type of which the same edges keep their part, so the light opens across the letters. A shader would be one pass instead of a dozen fills, but it is compiled on the device, where no engineer of this plan looks, and this is code the build checks (decision 4 in the plan's head).

**Its clock** starts when the glass has opened (`gate`) and counts ms, stretched by a debug build's slow motion. Per frame it reads the model, never the other way round but for three calls: `model.show()` when the hand-over is done, `model.land()` when the show cannot begin (no apps were ready a second after the cue was due), and `model.cueUp`. The cue (`model.cued`: Enter, or a click on the handle) begins the hand-over in that frame, from wherever the parts are: every part is its arrival's progress times what the hand-over has taken back of it, so nothing jumps. A key that ends the welcome (`model.playing` is no longer `WELCOME`) puts the lamp out in that frame: the light and the head are gone, what stands fades where it stands in 80 ms, and the night lifts in 160.

There is no unit test for this task: the app module has no test source set. Its check here is that it builds. What it looks like is Task 12's device checks 3 to 9, each a still at a named moment, and 10 and 11 in motion.

- [ ] **Step 1: Write `app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt`**

```kotlin
package io.github.kuscher.booklight.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import io.github.kuscher.booklight.ui.lightScheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// First run's welcome, "Lights on" (docs/design/first-run/design.md §3, motion.md §2.1). For four seconds the glass is a
// stage: it grows to 720 × 468 dp and goes to night, the field lights up from its caret and is a lamp's head, a shaft
// of light falls from it, the words stand in the light, and a small Swiss Army knife of five tools flicks out of a
// handle that carries the cue. Then the light folds back into the caret, and the handle rises to row one's seat and is
// the list's highlight.
//
// It is drawn on one clock. Every number that is drawn is a function of that clock's time and of two moments (when
// the hand-over began, when a key put the lamp out): nothing is kept from frame to frame. A frame composes nothing and
// lays nothing out: the type is laid out once, and everything is drawn in the draw phase. Nothing is drawn outside
// the glass, which clips it. This file is used only by a panel that began with the opening piece.

// The stage's measures, in dp of a panel 720 wide, from the panel's left and top edges (design.md §3, "Lights on, in full").
/** The panel's width, and the glass's height in the welcome. */
private const val WIDE = 720f
private const val HIGH = 468f
/** The seam's axis: everything of the welcome is centred on it. */
private const val AXIS = 360f
/** The lamp's head is the field's band, 0 to 68, its corners the glass's own. */
private const val HEAD = 68f
private const val CORNER = 32f
/** The caret, x 72 to 74, y 20 to 48: where the lamp strikes, and what its light folds back into. */
private const val CARET = 73f
private const val CARET_TOP = 20f
private const val CARET_HIGH = 28f
/** The light at rest: its upper edge on y = 92, from x 129 to 591 (0.64 of the head's width); its foot on y = 448, from 32 to 688 (0.91). */
private const val TOP = 92f
private const val FOOT = 448f
private const val TOP_HALF = 231f
private const val FOOT_HALF = 328f
/** As a seam it is 6 dp wide. */
private const val SEAM_HALF = 3f
/** Its edges: hard as a seam, soft once it is open; its penumbra and its core are softer still. */
private const val HARD = 1.5f
private const val SOFT = 20f
private const val HALO_SOFT = 60f
private const val CORE_SOFT = 50f
/** How far the light has spread through the head when it has reached its right end, from the caret. */
private const val FLOOD = WIDE - CARET
/** The title's box, y 124 to 172, and the line's, y 188 to 210; both centred on the axis. */
private const val TITLE_TOP = 124f
private const val TITLE_HIGH = 48f
private const val LINE_TOP = 188f
private const val LINE_HIGH = 22f
/** The title is set at 40, and smaller where it would be wider than this: it stays inside the light, which is 492 wide there. */
private const val TITLE = 40f
private const val TITLE_ROOM = 470f
private const val LINE = 17f
/** The handle, 216 × 48 with a radius of 24: left, top, right, bottom. And row one's seat, where it becomes the list's pill. */
private val HANDLE = floatArrayOf(252f, 372f, 468f, 420f)
private val SEAT = floatArrayOf(8f, 76f, 712f, 132f)
private const val ROUND = 24f
/** It rises this far into the light, as a row rises. */
private const val RISES = 12f
/** The five tools: pivots on the handle's upper edge from x = 288, 36 apart; a stem of 64 to the middle of a disc of 36; a symbol of 20 in it. */
private const val PIVOT = 288f
private const val PIVOTS = 36f
private const val STEM = 64f
private const val DISC = 18f
private const val SYMBOL = 20f
/** Folded, a tool lies along the handle and behind it: this far from upright, the left three to the right and the right two to the left. Open, they stand 40° and 20° to the left of upright, upright, 20° and 40° to the right. */
private const val FOLDED = 112f
private const val FAN = 20f
/** Left to right: the four beats of the show, and the key's step. */
private val TOOLS = listOf("app", "calc", "plane", "smile", "key")

/** Whether a point of the glass, in the stage's own measures, lies on the handle as it stands: a click there is the cue. */
internal fun onHandle(x: Float, y: Float): Boolean = x in HANDLE[0]..HANDLE[2] && y in HANDLE[1]..HANDLE[3]

// The light's strength, as white over the night (motion.md §2.1, "The light, as one pass").
/** Down the shaft: brightest under the lamp, not one even ramp. */
private val FALL = floatArrayOf(0.34f, 0.24f, 0.17f, 0.125f, 0.10f)
/** Its penumbra, outside its edges, and its core, along its axis. */
private val HALO = floatArrayOf(0.05f, 0.02f)
private val CORE = floatArrayOf(0.16f, 0.07f, 0.03f)
/** Twelve rays: where across the shaft (−1 to 1) a little more light lies, and how wide. They overlap, so no edge of one is seen. */
private val RAYS = floatArrayOf(-0.84f, 0.10f, -0.66f, 0.05f, -0.52f, 0.13f, -0.33f, 0.07f, -0.20f, 0.16f, -0.04f, 0.06f, 0.10f, 0.12f, 0.27f, 0.05f, 0.40f, 0.15f, 0.57f, 0.08f, 0.70f, 0.12f, 0.86f, 0.07f)
private val RAY_WIDTHS = floatArrayOf(1f, 0.6f, 0.3f)
private const val RAY = 0.016f
/** The lit head, the words where no light falls, a tool's stem and disc, the disc's ring, a glint; and the breath of the light while it stands. */
private const val HEAD_LIGHT = 0.92f
private const val UNLIT = 0.38f
private const val STEM_INK = 0.60f
private const val DISC_INK = 0.16f
private const val RING_INK = 0.30f
private const val GLINT = 0.70f
private const val BREATH = 0.06f
/** The handle is the selection as light theme has it, a little denser on the night, with the selection's white rim. Pressed, it gains a little ink. */
private const val HANDLE_FILL = 0.92f
private const val HANDLE_RIM = 0.55f
private const val PRESSED = 0.08f

private const val NEVER = Float.POSITIVE_INFINITY
private val CLEAR = Color.White.copy(alpha = 0f)

private fun mix(a: Float, b: Float, x: Float) = a + (b - a) * x
/** How far something that began at [from] and takes [ms] has come at [t], 0 to 1, on [curve]. Something that never began ([from] is [NEVER]) has not come at all. */
private fun tw(t: Float, from: Float, ms: Int, curve: Easing = FastOutSlowInEasing): Float = curve.transform(((t - from) / ms).coerceIn(0f, 1f))
private fun soften(a: Float, b: Float, x: Float): Float { val t = ((x - a) / (b - a)).coerceIn(0f, 1f); return t * t * (3f - 2f * t) }

/** One of the panel's springs as a function of the welcome's clock: how far it has come, 0 to a little past 1, [at] ms after it set off. */
private class Sprung(spec: FiniteAnimationSpec<Float>) {
    private val spring = spec.vectorize(Float.VectorConverter)
    private val zero = AnimationVector1D(0f)
    private val one = AnimationVector1D(1f)
    fun at(ms: Float): Float = if (!(ms > 0f)) 0f else if (ms > 4000f) 1f else spring.getValueFromNanos((ms * 1_000_000f).toLong(), zero, one, zero).value
}
/** (The clock is already as slow as a debug build asks: the springs are asked at their own speed.) */
private val PLACE = Sprung(Motion(true).place())
private val POP = Sprung(Motion(true).pop())

/** How dark the night is, 0 to 1: it falls from the gate on, and lifts as the shaft turns back into the caret, or once a key has put the lamp out. */
private fun nightAt(t: Float, hand: Float, off: Float): Float {
    fun alive(at: Float) = tw(at, 0f, Lights.NIGHT_MS) * (1f - tw(at, hand + Lights.TURN_AFTER_MS, Lights.LIFT_MS))
    return if (t >= off) alive(off) * (1f - (t - off) / Lights.DAWN_MS).coerceIn(0f, 1f) else alive(t)
}

/** How bright the lamp is, 0 to 1: it strikes, sags (it has not caught), and catches, fast at first. */
private fun lampAt(t: Float): Float {
    val s = t - Lights.STRIKE
    return when {
        s < 0f -> 0f
        s < Lights.STRIKE_UP_MS -> 0.6f * s / Lights.STRIKE_UP_MS
        s < Lights.STRIKE_SAG_MS -> mix(0.6f, 0.35f, (s - Lights.STRIKE_UP_MS) / (Lights.STRIKE_SAG_MS - Lights.STRIKE_UP_MS))
        else -> ((s - Lights.STRIKE_SAG_MS) / (Lights.STRIKE_ON_MS - Lights.STRIKE_SAG_MS)).coerceIn(0f, 1f).let { mix(0.35f, 1f, 1f - (1f - it) * (1f - it)) }
    }
}

/** How far the head's light has closed on the caret again, 0 to 1. */
private fun closedAt(t: Float, hand: Float) = tw(t, hand + Lights.CLOSE_AFTER_MS, Lights.CLOSE_MS, SEAM_DRAWS_IN)
/** Where the front of the head's light stands on its way from the caret to the field's right end. */
private fun frontAt(t: Float) = CARET + FLOOD * tw(t, (Lights.STRIKE + Lights.FLOOD_AFTER_MS).toFloat(), Lights.FLOOD_MS, SEAM_GROWS)

/** The light as a shape: where its middle stands at its upper edge and at its foot, half its width at each, how far it has fallen, and how soft its edges are. */
private class Shaft(val top: Float, val topHalf: Float, val foot: Float, val footHalf: Float, val fallen: Float, val soft: Float)

/**
 * The welcome at one moment of its clock: every number that is drawn. [t]: ms after the gate. [hand]: when the
 * hand-over began ([NEVER]: not yet). [off]: when a key put the lamp out ([NEVER]: none has).
 */
private class Lit(val t: Float, val hand: Float, off: Float) {
    /** A key has put the lamp out: the light and the head are gone at once, and what stands fades where it stands. */
    val dead = t >= off
    val fades = if (dead) (1f - (t - off) / Lights.OUT_MS).coerceIn(0f, 1f) else 1f
    val night = nightAt(t, hand, off)
    val lamp = if (dead) 0f else lampAt(t)
    val struck = t - Lights.STRIKE
    private val closed = closedAt(t, hand)
    /** The head's light, from the caret to both ends, and closing on the caret again at the end. */
    val headLeft = mix((CARET - (frontAt(t) - CARET)).coerceAtLeast(0f), CARET - 1f, closed)
    val headRight = mix(frontAt(t).coerceAtMost(WIDE), CARET + 1f, closed)
    val head = lamp > 0f && closed < 1f

    private val turn = tw(t, hand + Lights.TURN_AFTER_MS, Lights.TURN_MS, FOLDS)
    /** How far the light is open out of its seam. */
    val open = tw(t, Lights.OPEN.toFloat(), Lights.OPEN_MS, Motion.OPENS) * (1f - turn)
    private val fallen = TOP + (FOOT - TOP) * tw(t, Lights.SEAM.toFloat(), Lights.SEAM_MS, SEAM_GROWS) * (1f - tw(t, hand + Lights.TURN_AFTER_MS, Lights.RISE_MS, FastOutLinearInEasing))
    private val topAt = mix(AXIS, CARET, turn)
    private val footAt = mix(AXIS, CARET, tw(t, hand + Lights.FOOT_AFTER_MS, Lights.TURN_MS, FOLDS))
    private val topHalf = mix(SEAM_HALF, TOP_HALF, open)
    private val footHalf = mix(SEAM_HALF, FOOT_HALF, open)
    val shaft = Shaft(topAt, topHalf, footAt, footHalf, fallen, mix(HARD, SOFT, open))
    val halo = Shaft(topAt, topHalf + 14f * open, footAt, footHalf + 24f * open, fallen, mix(HARD, HALO_SOFT, open))
    val core = Shaft(topAt, topHalf / 2f, footAt, footHalf / 2f, fallen, mix(HARD, CORE_SOFT, open))
    val shines = !dead && fallen > TOP + 1f
    /** As a seam it is full white; it relaxes to its fall-off as it opens. And it breathes once while it stands. */
    val relaxed = soften(0f, 0.6f, open)
    val bright = lamp.coerceAtMost(1f) * (1f + BREATH * sin(PI.toFloat() * ((t - Lights.STANDS) / (Lights.HAND - Lights.STANDS)).coerceIn(0f, 1f)))

    /** The words in the lamp's room: there with the lamp, gone as the light leaves them. */
    val ambient: Float = (lampAt(minOf(t, off)).coerceAtMost(1f) * (1f - tw(minOf(t, off), hand + Lights.WORDS_OUT_AFTER_MS, Lights.WORDS_OUT_MS))) * fades

    /** The handle: born in the light's last third (at once, where the cue came before that), and from the hand-over on on its way to row one's seat. */
    private val born = minOf(Lights.HANDLE.toFloat(), hand)
    val there = (if (t >= born && !(dead && off < born)) tw(t, born, Lights.HANDLE_MS) else 0f) * fades
    val rise = RISES * (1f - PLACE.at(t - born))
    private val gone = PLACE.at(t - (hand + Lights.GO_AFTER_MS))
    val box = FloatArray(4) { mix(HANDLE[it] + if (it % 2 == 1) rise else 0f, SEAT[it], gone) }
    val pressed = tw(t, hand, Lights.PRESS_MS) * (1f - tw(t, hand + Lights.PRESS_MS, Lights.RELEASE_MS))
    /** How much of the night's own colour the handle has: all of it until the hand-over, then as much as there is night. */
    val handleNight = if (t < hand) 1f else night
    val cue = there * (1f - tw(t, hand + Lights.CUE_OUT_AFTER_MS, Lights.CUE_OUT_MS))
}

/**
 * What of first run's opening piece is drawn under the field and the rows, in a layer of the glass's own that is as
 * wide as the panel and as high as the welcome's glass: the welcome. And the desk behind the panel: dimmed by half in
 * the welcome's night (the welcome says how far, frame by frame), a little through the show, and lifting once the
 * key's step lands. [gate]: the glass has opened far enough for what is under the field to come. [ends]: how much of
 * the field's two ends shows while the glass opens. [onDesk]: how far the desk is to dim, 0 to 1, for this frame.
 */
@Composable
fun Piece(model: OverlayModel, gate: Boolean, ends: () -> Float, night: Night, onDesk: (Float) -> Unit) {
    val motion = LocalMotion.current
    /** The show's own dim: what "Dim the desktop" would be. */
    val through = if (LocalDark.current) Look.dimDark else Look.dimLight
    val desk = remember { Animatable(0f) }
    val phase = when (model.playing) { FirstRun.Playing.WELCOME -> 0; FirstRun.Playing.SHOW -> 1; else -> 2 }
    LaunchedEffect(phase) { if (phase == 1) desk.snapTo(through) else if (phase == 2) desk.animateTo(0f, motion.fade(Motion.DESK_LIFTS_MS)) }
    LaunchedEffect(Unit) { snapshotFlow { desk.value }.collect { if (model.playing != FirstRun.Playing.WELCOME) onDesk(it) } }
    Welcome(model, gate, ends, night, through, onDesk)
}

@Composable
private fun Welcome(model: OverlayModel, gate: Boolean, ends: () -> Float, night: Night, through: Float, onDesk: (Float) -> Unit) {
    val motion = LocalMotion.current
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDark.current
    val density = LocalDensity.current
    // The clock: ms after the gate. Before the gate there is nothing but the caret.
    val clock = remember { mutableFloatStateOf(0f) }
    var hand by remember { mutableFloatStateOf(NEVER) }
    var off by remember { mutableFloatStateOf(NEVER) }
    var over by remember { mutableStateOf(model.playing != FirstRun.Playing.WELCOME) }

    // One loop for the whole welcome. It ends in one of two ways: the hand-over is done and the show begins, or a key
    // has put the lamp out and the night has lifted. (A debug hook can begin it again in the open panel: `rounds`.)
    LaunchedEffect(gate, model.rounds) {
        if (!gate || model.playing != FirstRun.Playing.WELCOME) return@LaunchedEffect
        hand = NEVER; off = NEVER; clock.floatValue = 0f; over = false
        var t = 0f
        var before = 0L
        var late = false
        while (true) {
            val now = withFrameNanos { it }
            val stands = model.standsAt
            // (A debug build's slow motion stretches the clock, and a hook can stand it still at a moment of the paper's own clock.)
            t = if (stands != null) stands - Lights.GATE else t + (if (before == 0L) 0f else (now - before) / 1_000_000f / motion.slow)
            before = now
            if (model.playing == FirstRun.Playing.WELCOME) {
                if (hand == NEVER) {
                    val ready = model.cast != null
                    when {
                        stands != null -> if (t >= Lights.HAND) hand = Lights.HAND.toFloat()
                        // The cue: the user's Enter, or a click on the handle, begins the hand-over in this frame, from wherever the parts are.
                        model.cued && ready -> hand = t
                        // Or Booklight presses the handle itself. If this device's apps are not ready when that is due, the welcome
                        // stands on; after one more second the key's step lands, without a show.
                        t >= Lights.HAND -> if (ready) hand = if (late) t else Lights.HAND.toFloat() else { late = true; if (t >= Lights.HAND + Lights.WAITS_MS) model.land() }
                    }
                } else if (stands == null && t >= hand + Lights.HAND_MS) {
                    // The hand-over is done: the show's first letter lands, and its list comes with the pill in the seat the
                    // handle has reached. The handle is not drawn again: never two highlights in one frame.
                    model.show()
                    if (model.playing != FirstRun.Playing.WELCOME) break
                }
                // The handle carries the cue once it stands, and until it is pressed.
                model.cueUp = hand == NEVER && t >= Lights.HANDLE + Lights.HANDLE_MS
            } else {
                // The show has begun: nothing of the welcome is left. Or a key ended it: the lamp is out in this frame.
                if (model.playing == FirstRun.Playing.SHOW) break
                if (off == NEVER) off = t
                if (t >= off + Lights.DAWN_MS) break
            }
            clock.floatValue = t
            val n = nightAt(t, hand, off)
            // A key that types puts the lamp out as a lamp goes out: the veil is the theme's own at once, so that the typed
            // character stands in the theme's ink; the outline, and what is left of the night under the field, relax.
            night.rim = n; night.veil = if (t >= off) 0f else n
            onDesk(mix(if (t >= hand && t < off) through else 0f, NIGHT_DESK, n))
        }
        night.rim = 0f; night.veil = 0f; over = true
    }
    if (over) return

    val light = lightScheme(model.settings.tint)
    val sel = selectionFill(scheme, dark, LocalGlass.current)
    val handle = light.secondaryContainer.copy(alpha = HANDLE_FILL)
    val ink = light.onSecondaryContainer
    val rim = if (dark) 0.30f else 0.55f
    val caret = scheme.primary
    val marks = TOOLS.map { rememberVectorPainter(Symbols.of(it)) }
    val enter = rememberVectorPainter(Symbols.enter)
    // The type is laid out once. Its sizes are the design's dp, whatever the system's font scale is: it is a picture,
    // and it must stay inside the light.
    val measurer = rememberTextMeasurer()
    val title = stringResource(R.string.first_hello_title)
    val line = stringResource(R.string.first_hello_line)
    val cue = stringResource(R.string.first_hello_cue)
    val laid = remember(title, line, cue, density) {
        with(density) {
            fun lay(text: String, family: FontFamily, weight: Int, size: Float) =
                measurer.measure(text, TextStyle(fontFamily = family, fontWeight = FontWeight(weight), fontSize = size.dp.toSp()), maxLines = 1, softWrap = false)
            val wide = lay(title, Fonts.round, 600, TITLE).size.width / 1.dp.toPx()
            listOf(lay(title, Fonts.round, 600, if (wide > TITLE_ROOM) TITLE * TITLE_ROOM / wide else TITLE), lay(line, Fonts.text, 500, LINE), lay(cue, Fonts.text, 600, LINE))
        }
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            // The design's own units: `u` px to a dp of a panel 720 wide.
            val u = size.width / WIDE
            val f = Lit(clock.floatValue, hand, off)
            val t = f.t
            scale(u, u, Offset.Zero) {
                // The night is deeper towards the corners. Once a key has put the lamp out, what is left of it lifts under the field.
                if (f.dead) { if (f.night > 0f) drawRect(NIGHT.copy(alpha = NIGHT_VEIL * f.night), Offset(0f, HEAD), Size(WIDE, HIGH - HEAD)) }
                else if (f.night > 0f) drawRect(Brush.radialGradient(0f to NIGHT.copy(alpha = 0f), 0.38f to NIGHT.copy(alpha = 0f), 1f to NIGHT.copy(alpha = 0.6f), center = Offset(AXIS, 230f), radius = 520f), Offset.Zero, Size(WIDE, HIGH), alpha = f.night)
                if (f.head) {
                    // The strike: for a fifth of a second a halo stands round the caret, brightest at the lamp's first flare.
                    if (f.struck < Lights.STRIKE_ON_MS) drawRect(
                        Brush.radialGradient(0f to Color.White.copy(alpha = 0.9f), 1f to CLEAR, center = Offset(CARET, HEAD / 2f), radius = 80f), Offset.Zero, Size(160f, 120f),
                        alpha = (if (f.struck < Lights.STRIKE_UP_MS) f.struck / Lights.STRIKE_UP_MS else 1f - (f.struck - Lights.STRIKE_UP_MS) / (Lights.STRIKE_ON_MS - Lights.STRIKE_UP_MS)).coerceIn(0f, 1f),
                    )
                    // The head: the field's band, lit from the caret to both ends, the light's front soft.
                    val edge = 30f
                    val q = edge / (f.headRight - f.headLeft + 2f * edge)
                    clipPath(Path().apply { addRoundRect(RoundRect(0f, 0f, WIDE, HEAD, CornerRadius(CORNER))) }) {
                        drawRect(Brush.horizontalGradient(0f to CLEAR, q to Color.White, 1f - q to Color.White, 1f to CLEAR, startX = f.headLeft - edge, endX = f.headRight + edge), Offset.Zero, Size(WIDE, HEAD), alpha = (HEAD_LIGHT * f.lamp).coerceIn(0f, 1f))
                    }
                }
                if (f.shines) {
                    // The light: a penumbra outside its edges, the shaft with its fall-off and its rays, a core along its axis.
                    if (f.relaxed > 0.02f) light(f.halo, FloatArray(HALO.size) { HALO[it] * f.relaxed }, f.bright, 0f)
                    light(f.shaft, FloatArray(FALL.size) { mix(1f, FALL[it], f.relaxed) }, f.bright, f.relaxed)
                    if (f.relaxed > 0.02f) light(f.core, FloatArray(CORE.size) { CORE[it] * f.relaxed }, f.bright, 0f)
                }
            }
            // The words: dim in the lamp's room, white where the light falls, with a little bloom. The lit layer is masked by
            // the light's own soft edges, so the light opens across the letters.
            if (f.ambient > 0f) {
                val titleAt = Offset((size.width - laid[0].size.width) / 2f, TITLE_TOP * u + (TITLE_HIGH * u - laid[0].size.height) / 2f)
                val lineAt = Offset((size.width - laid[1].size.width) / 2f, LINE_TOP * u + (LINE_HIGH * u - laid[1].size.height) / 2f)
                drawText(laid[0], Color.White, titleAt, alpha = UNLIT * f.ambient)
                drawText(laid[1], Color.White, lineAt, alpha = UNLIT * f.ambient)
                if (f.shines) {
                    val area = Rect(0f, TOP, WIDE, 240f)
                    drawIntoCanvas { it.saveLayer(Rect(0f, area.top * u, size.width, area.bottom * u), Paint().apply { alpha = f.lamp.coerceIn(0f, 1f) }) }
                    val bloom = Shadow(Color.White.copy(alpha = 0.45f), Offset.Zero, 9f * u)
                    drawText(laid[0], Color.White, titleAt, shadow = bloom)
                    drawText(laid[1], Color.White, lineAt, shadow = bloom)
                    scale(u, u, Offset.Zero) { edges(f.shaft, area, upper = false) }
                    drawIntoCanvas { it.restore() }
                }
            }
            scale(u, u, Offset.Zero) {
                if (f.there > 0f) {
                    // The knife: five tools flick out from behind the handle, left to right, a beat apart, each once past its
                    // angle and back; a glint crosses each disc as it opens. At the hand-over they fold back, right to left.
                    for (i in TOOLS.indices) {
                        val since = t - Lights.TOOLS - Lights.BEAT_MS * i
                        val back = if (f.dead) 0f else tw(t, hand + Lights.FOLD_EACH_MS * (TOOLS.lastIndex - i), Lights.FOLD_MS, FastOutLinearInEasing)
                        if (since < 0f || back >= 1f) continue
                        val folded = if (i < 3) FOLDED else -FOLDED
                        val angle = Math.toRadians(mix(mix(folded, FAN * (i - 2), POP.at(since)), folded, back).toDouble())
                        val sx = sin(angle).toFloat()
                        val cx = cos(angle).toFloat()
                        val px = PIVOT + PIVOTS * i
                        val py = HANDLE[1] + f.rise
                        val at = Offset(px + STEM * sx, py - STEM * cx)
                        drawLine(Color.White.copy(alpha = STEM_INK * f.there), Offset(px, py), Offset(at.x - DISC * sx, at.y + DISC * cx), 4f, StrokeCap.Round)
                        drawCircle(Color.White.copy(alpha = DISC_INK * f.there), DISC, at)
                        drawCircle(Color.White.copy(alpha = RING_INK * f.there), DISC, at, style = Stroke(1f))
                        translate(at.x - SYMBOL / 2f, at.y - SYMBOL / 2f) { with(marks[i]) { draw(Size(SYMBOL, SYMBOL), alpha = f.there, colorFilter = ColorFilter.tint(Color.White)) } }
                        if (since > Lights.GLINT_AFTER_MS && since < Lights.GLINT_AFTER_MS + Lights.GLINT_MS) {
                            val gx = at.x + mix(-30f, 30f, (since - Lights.GLINT_AFTER_MS) / Lights.GLINT_MS)
                            clipPath(Path().apply { addOval(Rect(at, DISC)) }) {
                                rotate(45f, at) { drawRect(Brush.horizontalGradient(0f to CLEAR, 0.5f to Color.White.copy(alpha = GLINT), 1f to CLEAR, startX = gx - 7f, endX = gx + 7f), Offset(gx - 7f, at.y - 26f), Size(14f, 52f), alpha = f.there) }
                            }
                        }
                    }
                    // The handle, over the folded tools: the one coloured surface. It is the selection: as light theme has it
                    // while it is night, the theme's own as the night lifts. From the hand-over on it travels to row one's
                    // seat, and there it is the list's pill.
                    val fill = lerp(sel, handle, f.handleNight).let { lerp(it, ink.copy(alpha = it.alpha), PRESSED * f.pressed) }
                    val b = f.box
                    drawRoundRect(fill.copy(alpha = fill.alpha * f.there), Offset(b[0], b[1]), Size(b[2] - b[0], b[3] - b[1]), CornerRadius(ROUND))
                    drawRoundRect(Color.White.copy(alpha = mix(rim, HANDLE_RIM, f.handleNight) * f.there), Offset(b[0] + 0.5f / u, b[1] + 0.5f / u), Size(b[2] - b[0] - 1f / u, b[3] - b[1] - 1f / u), CornerRadius(ROUND), style = Stroke(1f / u))
                }
                // The caret is the switch: the theme's own until the night falls, white in the night, off for a blink before
                // the lamp strikes, and in the lit head the dark slit of Booklight's mark. At the end it is all that is left of
                // the lamp. (Once a key has ended the welcome the field's own caret is back.)
                if (!f.dead && !(t >= Lights.CARET_OFF && t < Lights.STRIKE))
                    drawRect(if (f.head && f.headRight > CARET + 2f) NIGHT else lerp(caret, Color.White, f.night), Offset(CARET - 1f, CARET_TOP), Size(2f, CARET_HIGH), alpha = ends())
            }
            // The cue, on the handle wherever the handle is: the words, 8 dp, the Enter mark at 16.
            if (f.cue > 0f) {
                val cx = (f.box[0] + f.box[2]) / 2f * u
                val cy = (f.box[1] + f.box[3]) / 2f * u
                val wide = laid[2].size.width + 24f * u
                drawText(laid[2], ink, Offset(cx - wide / 2f, cy - laid[2].size.height / 2f), alpha = f.cue)
                translate(cx + wide / 2f - 16f * u, cy - 8f * u) { with(enter) { draw(Size(16f * u, 16f * u), alpha = f.cue, colorFilter = ColorFilter.tint(ink)) } }
            }
        }
        // The field's `esc` cap, where the field has it (its right edge at 700): white in the night, and in the night's ink
        // once the lit head is under it.
        if (model.playing == FirstRun.Playing.WELCOME) {
            val dusk by remember { derivedStateOf { night.rim > 0.5f } }
            val lit by remember { derivedStateOf { clock.floatValue.let { it < off && lampAt(it) > 0f && frontAt(it) > 700f && closedAt(it, hand) < 0.05f } } }
            Box(Modifier.align(Alignment.TopEnd).padding(end = 20.dp).height(Metrics.field).graphicsLayer { alpha = ends() }, contentAlignment = Alignment.Center) {
                MaterialTheme(colorScheme = scheme.copy(onSurface = if (lit) NIGHT else if (dusk) Color.White else scheme.onSurface)) {
                    CompositionLocalProvider(LocalDark provides (if (lit) false else if (dusk) true else dark)) { Keycap("esc", wide = true) }
                }
            }
        }
    }
}

/** The light as one pass: its fall-off down the shaft ([stops], from its upper edge to its foot), a few rays, and its four soft edges. */
private fun DrawScope.light(s: Shaft, stops: FloatArray, bright: Float, rays: Float) {
    val area = Rect(0f, HEAD - 8f, WIDE, HIGH + 12f)
    drawIntoCanvas { it.saveLayer(area, Paint()) }
    val fall = Array(stops.size) { it / (stops.size - 1f) to Color.White.copy(alpha = (stops[it] * bright).coerceIn(0f, 1f)) }
    drawRect(Brush.verticalGradient(*fall, startY = TOP, endY = maxOf(TOP + 1f, s.fallen)), area.topLeft, area.size)
    if (rays > 0f) {
        val ray = Path()
        val ink = Color.White.copy(alpha = (RAY * rays * bright).coerceIn(0f, 1f))
        for (i in RAYS.indices step 2) for (w in RAY_WIDTHS) {
            val at = RAYS[i]
            val d = RAYS[i + 1] * w
            ray.rewind()
            ray.moveTo(s.top + (at - d) * s.topHalf, TOP); ray.lineTo(s.top + (at + d) * s.topHalf, TOP)
            ray.lineTo(s.foot + (at + d) * s.footHalf, s.fallen); ray.lineTo(s.foot + (at - d) * s.footHalf, s.fallen)
            ray.close()
            drawPath(ray, ink)
        }
    }
    edges(s, area, upper = true)
    drawIntoCanvas { it.restore() }
}

/** The light's soft edges, as what is kept of everything drawn since the layer began: inside its two sides, above its foot, and ([upper]) under its upper edge. */
private fun DrawScope.edges(s: Shaft, area: Rect, upper: Boolean) {
    side(s.top - s.topHalf, s.foot - s.footHalf, s.fallen, 1f, s.soft, area)
    side(s.top + s.topHalf, s.foot + s.footHalf, s.fallen, -1f, s.soft, area)
    if (upper) keep(0f, TOP, 0f, 1f, -8f, 6f, area)
    keep(0f, s.fallen, 0f, -1f, -s.soft, 0.3f * s.soft, area)
}

/** One side of the light, a line from ([x1], the upper edge) to ([x2], [y2]): what lies on its inner side is kept. [sign]: 1 for the left side, −1 for the right. */
private fun DrawScope.side(x1: Float, x2: Float, y2: Float, sign: Float, soft: Float, area: Rect) {
    val dx = x2 - x1
    val dy = y2 - TOP
    val long = hypot(dx, dy).takeIf { it > 0f } ?: 1f
    keep(x1, TOP, sign * dy / long, -sign * dx / long, -soft, 0.3f * soft, area)
}

/** Keeps what lies beyond the line through ([px], [py]) in the direction ([nx], [ny]): nothing of it at [from] along that direction, all of it at [to], and softly between. */
private fun DrawScope.keep(px: Float, py: Float, nx: Float, ny: Float, from: Float, to: Float, area: Rect) {
    drawRect(
        Brush.linearGradient(
            0f to Color.Black.copy(alpha = 0f), 0.25f to Color.Black.copy(alpha = 0.16f), 0.5f to Color.Black.copy(alpha = 0.5f), 0.75f to Color.Black.copy(alpha = 0.84f), 1f to Color.Black,
            start = Offset(px + nx * from, py + ny * from), end = Offset(px + nx * to, py + ny * to),
        ),
        area.topLeft, area.size, blendMode = BlendMode.DstIn,
    )
}
```

- [ ] **Step 2: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 3: Nothing of it is outside the stage, and it brings nothing with it**

```bash
grep -c "RuntimeShader\|Bitmap\|ImageBitmap\|Random\|R.drawable\|R.raw" app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt
grep -n "Piece(" app/src/main/java/io/github/kuscher/booklight/overlay/*.kt | grep -v "fun Piece" || echo NOT-COMPOSED
```

Expected: `0` (no second shader, no bitmap, nothing random, no asset), then `NOT-COMPOSED`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt
git commit -m "First run: the welcome, Lights on: the night, the lamp, the shaft, the words in its light, the knife, and the hand-over (nothing composes it yet)"
```

### Task 10: The panel plays it: the first opening, its keys and clicks, the light at the grid

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`

**Interfaces:**
- Consumes: core `FirstRun.overture(s, motion, reader, plain, screenDp)`, `FirstRun.greets(…)` (Task 1), `FirstRun.slow(s, overture)` (part 1), `FirstRun.Press`, `FirstRun.Playing` (Task 2); `OverlayModel.begin()`, `press(press, again)`, `playing`, `began`, `greets`, `cueUp`, `laps`, `hold`, `due` (Task 7; `due` part 3); `Night`, `Modifier.glass(…, veil, rim)` (Task 8); `Piece(model, gate, ends, night, onDesk)`, `onHandle(x, y)` (Task 9); `Metrics.welcome` (Task 7); `R.string.first_hello_title` (Task 4). In `Panel`: the local `gate`, `ends()`, `shown()`, `glassTop()`, `stir`, `lapping`, `flourish`, `run`, `laps` (its coroutine scope), `outlineDp()`. In `OverlayActivity`: `first(app) { … }`, `motion`, `dim`, `present(dimmed, focused)`, `placeWindow()`, `run(r, a, keep)`.
- Produces: the opening piece plays. `Panel(…)` has one more parameter, `onDesk: (Float) -> Unit`, after `onPresence`. In `OverlayActivity`: `private var staged`, `private fun desk(amount: Float)`. A new installation's very first opening, with the system's animations on, no screen reader, a plain start and a screen of 655 dp or more: at Slow, the welcome, the show, the key's step.

This task switches the piece on. Everything it needs is built and tested or built and inert; here it is wired, in four places.

**1. Who gets it** (`OverlayActivity.onCreate`). The order for every panel stays: the key landing, then the opening counted, then, new, how the opening begins, then the model. `FirstRun.overture` is asked with the same `plain` as the count. The system is asked whether a screen reader is on only where the piece would otherwise play: someone who has no run, or whose piece was shown, pays nothing new. Where it plays, `model.begin()` is said before anything asks the model what stands, and for a new installation the opening is the Slow one (`FirstRun.slow`), whatever is set. Where it is this opening's and is not played (animations off, a screen reader on), the key's step stands in the first frame as it does today, and `model.greets` makes the welcome's title the field's placeholder; a screen reader is told the title before the step's own words. On a screen lower than 655 dp nothing of this happens: the key's step, as today.

**2. The window.** It is still exactly the panel: the welcome's 468 dp is `Metrics.height(model)` (Task 7), the height rides `place` from the gate on, and the window follows it frame by frame as it does for any list (`sizeWindow`); its width never changes; the blur is still the root view framed to the glass (`frameGlass`, untouched). The opening itself is still the field's height: the welcome's room comes at the gate. What is new is the desk's dim: a panel that begins with the piece asks for `FLAG_DIM_BEHIND` as "Dim the desktop" does, and the piece says how far for each frame (`desk`): 0.50 in the night, the theme's own dim (0.10 light, 0.22 dark) through the show, none once the key's step has landed. It comes and goes with the panel, as the dim of the setting does.

**3. The glass's layers** (`Panel`). The piece's layer is the first child of the glass, under the field and the rows: 720 × 468 dp, laid out once, placed as the contents are (top centre, moved up while the glass is still a seam), cut by the glass's own clip, and it fades with the contents when the panel leaves. So nothing of it is ever outside the glass. While the piece plays a second layer lies over everything and takes every click: on the welcome's handle a click is the cue, anywhere else it sets the key's step down; no row of the show can be hovered or clicked.

**4. The keys** (`Panel.keys`). The piece is asked first, for every key (`model.press`). Only a typed character goes on, to the field; the piece has ended by then, so the character is the whole text. A key the piece has used up does nothing more for as long as it is held (`spent`): one press, one step, and a held Esc that skipped the show does not go on to close the panel. `OverlayActivity.run`, where everything that runs passes, returns at once while the piece plays.

**The light.** A panel that began with the piece has no lap of its own: none while the welcome stands (the plain shaft), and afterwards the one the show asks for as the grid's wave lands (`model.laps`), which the landing is cued by. Booklight's own typing and moves do not put it out; any key of the user's does, in 160 ms, as built.

**The show's first list comes without a fade.** The welcome's handle has travelled to row one's seat by then, in the theme's own selection colour. The list's pill stands in that seat from its first frame, and the handle is no longer drawn from that frame on: one highlight, never two and never none. Only the rows rise.

There is no unit test for this task: the app module has no test source set. What it decides is core's and is tested there (`FirstRunStartTest`, `FirstRunGateTest`, `FirstRunPlayingTest`); its check here is that it builds, and Task 12's device checks, all of them.

- [ ] **Step 1: The panel, in `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** Sixteen changes, in the file's order: four imports; the new parameter; no lap of its own; the show's lap; `spent`; the keys; `night`; the glass told of it; the piece's layer; a typed character by whichever way; the show's first list; the layer that takes the clicks; and `pressOf` at the file's end.

Find:

```kotlin
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
```

Make it:

```kotlin
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
```

Find:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
```

Make it:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
```

Find:

```kotlin
import androidx.compose.foundation.layout.requiredWidth
```

Make it:

```kotlin
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
```

Find:

```kotlin
import androidx.compose.ui.input.key.type
```

Make it:

```kotlin
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
```

Find:

```kotlin
    onPresence: (dim: Float, blur: Float) -> Unit,
```

Make it:

```kotlin
    onPresence: (dim: Float, blur: Float) -> Unit,
    /** First run's opening piece: how far the desk behind the panel is to dim for this frame, 0 to 1. Called only in a panel that began with the piece. */
    onDesk: (Float) -> Unit,
```

Find:

```kotlin
        if (!motion.on || !gate || leaving) return@LaunchedEffect
        val since = SystemClock.uptimeMillis()
```

Make it:

```kotlin
        if (!motion.on || !gate || leaving) return@LaunchedEffect
        // A panel that began with first run's opening piece has no lap of its own: none while the welcome stands, and the one
        // the show asks for afterwards (below).
        if (model.began) return@LaunchedEffect
        val since = SystemClock.uptimeMillis()
```

Find:

```kotlin
    LaunchedEffect(leaving) { present.animateTo(if (leaving) 0f else 1f, motion.fade(if (leaving) 90 else 160)) }
```

Make it:

```kotlin
    // First run's show asks for the light itself, as the grid's wave lands (`OverlayModel.laps`): one lap, as built, which the
    // show's landing is cued by. Booklight's own typing and moves do not put it out; any key of the user's does, in 160 ms.
    if (model.began) {
        LaunchedEffect(Unit) {
            snapshotFlow { model.laps }.collect { asked ->
                if (asked == 0 || !motion.on || lapping) return@collect
                laps.launch {
                    lapping = true
                    try {
                        flourish.snapTo(1f); run.snapTo(0f)
                        val ms = (outlineDp() * Motion.REFLECTION_MS_PER_DP).toInt().coerceAtMost(Motion.REFLECTION_MAX_MS)
                        run.animateTo(1f, motion.fade(ms, easing = Motion.REFLECTS))
                        run.snapTo(0f)
                    } finally { lapping = false }
                }
            }
        }
        LaunchedEffect(Unit) { snapshotFlow { stir }.collect { if (lapping) flourish.animateTo(0f, motion.fade(160)) } }
    }
    LaunchedEffect(leaving) { present.animateTo(if (leaving) 0f else 1f, motion.fade(if (leaving) 90 else 160)) }
```

Find:

```kotlin
    /** The Escape that is down went back from an answer: its repeats do nothing more. */
    val leftAnswer = remember { booleanArrayOf(false) }
```

Make it:

```kotlin
    /** The Escape that is down went back from an answer: its repeats do nothing more. */
    val leftAnswer = remember { booleanArrayOf(false) }
    /** The key that is down was used up by first run's opening piece (its key code; 0: none): its repeats do nothing more. */
    val spent = remember { intArrayOf(0) }
```

Find:

```kotlin
        model.keyboard = e.nativeKeyEvent.deviceId
        val stage = model.stage
        val atEnd = field.selection.collapsed && field.selection.end == field.text.length
        val enter = e.key == Key.Enter || e.key == Key.NumPadEnter
        // A key that is held repeats. Whatever runs something takes one press for one run: a held Enter must not confirm its own delete.
        val again = e.nativeKeyEvent.repeatCount > 0
```

Make it:

```kotlin
        model.keyboard = e.nativeKeyEvent.deviceId
        // A key that is held repeats. Whatever runs something takes one press for one run: a held Enter must not confirm its own delete.
        val again = e.nativeKeyEvent.repeatCount > 0
        // First run's opening piece takes every key first (core `FirstRun.pressed`): only a typed character goes on, to the
        // field, and the piece is over by then. A key the piece has used up does nothing more for as long as it is held: one
        // press, one step, and a held Esc that skipped the show does not go on to close the panel.
        if (!again) spent[0] = 0
        if (again && spent[0] == e.nativeKeyEvent.keyCode) return true
        if (model.playing != null && model.press(pressOf(e), again)) { spent[0] = e.nativeKeyEvent.keyCode; return true }
        val stage = model.stage
        val atEnd = field.selection.collapsed && field.selection.end == field.text.length
        val enter = e.key == Key.Enter || e.key == Key.NumPadEnter
```

Find:

```kotlin
    val radiusPx = with(density) { Metrics.radius.toPx() }
```

Make it:

```kotlin
    val radiusPx = with(density) { Metrics.radius.toPx() }
    /** First run's welcome: how far the glass has gone to night, written by the welcome for each frame and read where the glass is drawn. */
    val night = remember { Night() }
```

Find:

```kotlin
                    dark = dark, solid = !glass,
                )
```

Make it:

```kotlin
                    dark = dark, solid = !glass, veil = { night.veil }, rim = { night.rim },
                )
```

Find:

```kotlin
            CompositionLocalProvider(LocalDark provides dark, LocalGlass provides glass) {
                // Laid out once at the panel's width and only uncovered: it never moves on screen while the glass grows.
```

Make it:

```kotlin
            CompositionLocalProvider(LocalDark provides dark, LocalGlass provides glass) {
                // First run's opening piece draws under the field and the rows, in a layer of the glass's own: laid out once, as
                // wide as the panel and as high as the welcome's glass, placed as the contents are, uncovered by the glass as it
                // grows and cut by its edge: nothing of it is outside the glass. It lets go with the contents when the panel
                // leaves. Composed only in a panel that began with the piece.
                if (model.began) Box(
                    Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).requiredSize(full, Metrics.field + Metrics.welcome)
                        .offset { IntOffset(0, -glassTop()) }
                        .graphicsLayer { alpha = shown() },
                ) { Piece(model, gate, ::ends, night, onDesk) }
                // Laid out once at the panel's width and only uncovered: it never moves on screen while the glass grows.
```

Find:

```kotlin
                        val text = model.query
                        val now = when {
```

Make it:

```kotlin
                        // First run's opening piece ends for a typed character by whichever way it comes: a key has ended it in
                        // `keys` already; text the input method puts together arrives only here. Booklight's own text is then
                        // gone from the model, and what the edit added is the whole text.
                        if (model.playing != null) model.press(FirstRun.Press.TYPES)
                        val text = model.query
                        val now = when {
```

Find:

```kotlin
                            transitionSpec = { (fadeIn(motion.fade(140)) togetherWith fadeOut(motion.fade(70))).using(null) },
                            contentAlignment = Alignment.TopStart,
                            label = "body",
```

Make it:

```kotlin
                            // (First run's show begins with the welcome's handle in row one's seat: its list comes at once, the pill
                            // in that seat from its first frame, and only the rows rise. Nothing fades in over the handle.)
                            transitionSpec = { ((if (model.playing == FirstRun.Playing.SHOW && initialState == "none") fadeIn(snap()) else fadeIn(motion.fade(140))) togetherWith fadeOut(motion.fade(70))).using(null) },
                            contentAlignment = Alignment.TopStart,
                            label = "body",
```

Find:

```kotlin
                    if (rows || foot.value > 0f) Box(Modifier.graphicsLayer { alpha = foot.value * edges() * band() }) { Footer(model) }
                }
```

Make it:

```kotlin
                    if (rows || foot.value > 0f) Box(Modifier.graphicsLayer { alpha = foot.value * edges() * band() }) { Footer(model) }
                }
                // While first run's opening piece plays, the glass takes every click itself, and no row is hovered or clicked: on
                // the welcome's handle a click is the cue, anywhere else it sets the key's step down (core `FirstRun.pressed`).
                if (model.playing != null) Box(Modifier.matchParentSize().pointerInput(Unit) {
                    detectTapGestures { at ->
                        // (In the stage's own measures: dp of a panel 720 wide.)
                        val unit = size.width / Metrics.width.value
                        val cue = model.playing == FirstRun.Playing.WELCOME && model.cueUp && onHandle(at.x / unit, at.y / unit)
                        model.press(if (cue) FirstRun.Press.CUE else FirstRun.Press.OTHER)
                    }
                })
```

Find:

```kotlin
private val DIGITS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)
```

Make it:

```kotlin
private val DIGITS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)

/**
 * A key as first run's opening piece sees it (core `FirstRun.Press`). A key that types is one that puts a character into
 * the field as it is pressed now: not Enter, Tab, Esc or Backspace, and not with Ctrl or Alt held. A modifier alone is
 * nothing, and neither is a key pressed with the Action key: that is the system's.
 */
private fun pressOf(e: KeyEvent): FirstRun.Press = when {
    android.view.KeyEvent.isModifierKey(e.nativeKeyEvent.keyCode) || e.isMetaPressed -> FirstRun.Press.MODIFIER
    e.key == Key.Enter || e.key == Key.NumPadEnter -> FirstRun.Press.ENTER
    !e.isCtrlPressed && !e.isAltPressed && e.key != Key.Tab && e.key != Key.Escape && e.key != Key.Backspace && e.nativeKeyEvent.unicodeChar != 0 -> FirstRun.Press.TYPES
    else -> FirstRun.Press.OTHER
}
```

- [ ] **Step 2: The activity, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Seven changes, in the file's order: `staged`; how the opening begins; the Slow opening; the piece's call from the panel; the window's dim; `present` and `desk`; `run`.

Find:

```kotlin
    /** This start was a click on the icon: there is no panel, only a hand-over to the Booklight window. */
    private var handedOver = false
```

Make it:

```kotlin
    /** This start was a click on the icon: there is no panel, only a hand-over to the Booklight window. */
    private var handedOver = false
    /** This panel begins with first run's opening piece (core `FirstRun.overture`): for it alone the desk behind dims as the piece asks ([desk]). */
    private var staged = false
```

Find:

```kotlin
        first(app) { FirstRun.opening(it, screenDp, plain) }
        model = OverlayModel(app, lifecycleScope, limit = Metrics.maxRows(screenDp), screenDp = screenDp)
        // Said before anything asks the model what stands (the words to read, the window's first height).
        if (app.example != null) model.guided = true
        // The system's own words for its dialog are read before the stage that quotes them comes: the key's.
        if (model.stage?.step == FirstRun.Step.KEY) SystemWords.load(this, app.scope)
        model.typeStep = motion::typeStep
```

Make it:

```kotlin
        first(app) { FirstRun.opening(it, screenDp, plain) }
        // How this opening begins (core `FirstRun.overture`): asked once the opening is counted, with the same `plain`. The
        // system is asked for a screen reader only where the opening piece is this opening's but for animations and a
        // reader: on every other day nothing here is read from it.
        val run = app.prefs.now.firstRun()
        val due = FirstRun.overture(run, motion = true, reader = false, plain = plain, screenDp = screenDp) != FirstRun.Overture.NONE
        val reader = due && getSystemService(android.view.accessibility.AccessibilityManager::class.java)?.isTouchExplorationEnabled == true
        staged = due && FirstRun.overture(run, motion.on, reader, plain, screenDp) != FirstRun.Overture.NONE
        // The very first opening of a new installation, the one its opening piece begins, runs at Slow whatever is set.
        val slowly = staged && FirstRun.slow(run, FirstRun.Overture.WELCOME)
        model = OverlayModel(app, lifecycleScope, limit = Metrics.maxRows(screenDp), screenDp = screenDp)
        // Said before anything asks the model what stands (the words to read, the window's first height).
        if (app.example != null) model.guided = true
        // The piece begins with this panel: the welcome, then the show, then the key's step. Where it is this opening's and
        // is not played (animations off, a screen reader), the key's step stands at once, as on any day, and the welcome's
        // title greets as the field's placeholder.
        if (staged) model.begin() else if (due) model.greets = FirstRun.greets(run, motion.on, reader, plain, screenDp)
        // The system's own words for its dialog are read before the stage that quotes them comes: the key's. (Asked of what
        // is due, not of what stands: while the piece plays nothing stands, and the key's step is what it lands in.)
        if (model.due?.step == FirstRun.Step.KEY) SystemWords.load(this, app.scope)
        model.typeStep = motion::typeStep
        model.hold = motion::hold
```

Find:

```kotlin
        var opening = settings.opening
```

Make it:

```kotlin
        var opening = if (slowly) "slow" else settings.opening
```

Find:

```kotlin
                        onHeight = ::sizeWindow, onPresence = ::present, frame = frame,
```

Make it:

```kotlin
                        onHeight = ::sizeWindow, onPresence = ::present, onDesk = ::desk, frame = frame,
```

Find:

```kotlin
        if (dim > 0f) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
```

Make it:

```kotlin
        // (A panel that begins with first run's opening piece dims the desk for the piece, whatever is set: `desk`.)
        if (dim > 0f || staged) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
```

Find:

```kotlin
        if (dim > 0f) window.setDimAmount(dim * dimmed)
    }
```

Make it:

```kotlin
        arrived = dimmed
        if (dim > 0f || staged) window.setDimAmount(maxOf(dim, desk) * dimmed)
    }

    /** How far the desk behind is dimmed for first run's opening piece, 0 to 1; and how far the panel has arrived, with which every dim comes and goes. */
    private var desk = 0f
    private var arrived = 0f

    /**
     * First run's opening piece asks for the desk behind the panel to dim: by half in the welcome's night, as "Dim the
     * desktop" would through the show, not at all once the key's step has landed. Never less than the setting asks for.
     */
    private fun desk(amount: Float) {
        if (!staged || amount == desk) return
        desk = amount
        window.setDimAmount(maxOf(dim, desk) * arrived)
    }
```

Find:

```kotlin
        if (leaving || settled) return   // a second Enter or click while the panel is on its way out
```

Make it:

```kotlin
        // (Nor while first run's opening piece plays: it performs, and nothing of it is run.)
        if (leaving || settled || model.playing != null) return   // a second Enter or click while the panel is on its way out
```

- [ ] **Step 3: The greeting for a screen reader, in `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`.** One change.

Find:

```kotlin
    LaunchedEffect(on) { view.announceForAccessibility(told) }
```

Make it:

```kotlin
    // Where first run's opening piece is this opening's and is not played, its title is said once, before the first screen's own words.
    val hello = stringResource(R.string.first_hello_title)
    val greeted = remember { booleanArrayOf(false) }
    LaunchedEffect(on) {
        val first = model.greets && !greeted[0]
        greeted[0] = true
        view.announceForAccessibility(if (first) "$hello. $told" else told)
    }
```

- [ ] **Step 4: The order of `onCreate` holds, and the piece has one beginning**

```bash
awk '/FirstRun.keyLanded\(it\)/ && !k { k = NR } /FirstRun.opening\(it, screenDp, plain\)/ { o = NR } /staged = due && FirstRun.overture/ { v = NR } /model = OverlayModel\(/ { m = NR } /if \(staged\) model.begin\(\)/ { b = NR } /arrival = Arrival.of\(/ { a = NR } END { print (k < o && o < v && v < m && m < b && b < a) ? "IN-ORDER" : "OUT-OF-ORDER" }' app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
grep -c "model.begin()" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
grep -c "Piece(model" app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt
```

Expected: `IN-ORDER` (the key, the count, how it begins, the model, the piece begun, the arrival), then `1` and `1`.

- [ ] **Step 5: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt
git commit -m "First run: the very first opening is the Slow one and begins with the welcome and the show; every key and click is the piece's first, and nothing of it is run"
```

### Task 11: The one highlight travels into the key's step

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`

**Interfaces:**
- Consumes: `OverlayModel.land()`, `clear()`, `current`, `results`, `selected`, `cell`, `stage`, `due`, `first()` (Task 7 and before); `Metrics.field`, `pad`, `width`, `cell`, `tops(rows)`, `rowHeight(r)`; core `FirstRun.armed(s)`; `Motion.GLIDE_AFTER_MS`, `Motion.TAKES_OVER_MS`, `motion.pillLead()`, `motion.pillTrail(far)`, `motion.place()`, `motion.fade(ms)` (Task 5 and before); `Piece` in `overlay/Welcome.kt`, whose last line is `    Welcome(model, gate, ends, night, through, onDesk)` (Task 9), and that file's private `WIDE` and `mix`; `selectionFill(scheme, dark, glass)`; in `FirstStage.kt` the `strip` that draws the answers with `OptionStrip(…, vertical = true, lit = armed >= 0)`.
- Produces: in `OverlayModel`: `var glideFrom: FloatArray?` (its setter is the model's own), `val gliding: Boolean`, `var answerAt: FloatArray?`, `fun glided()`. In `Welcome.kt`: the private composable `Glide`, composed by `Piece`. In `FirstStage.kt`: the answers say where the armed one stands, and are not lit while the highlight is on its way to them.

"One highlight, never gone" (`design.md` §3, the show's third rule; on its list of what is never cut). It is born as the welcome's handle and is the list's pill from the show's first frame (Tasks 9 and 10). This task carries it over the last gap: from the grid's square, or from the pill where a key ended the show sooner, into the armed answer of the key's step ("Open Keyboard shortcuts"; in a replay with a key that works, "Go on").

**How.** At the landing the list is emptied, so the square and the pill are not drawn from that frame on (a row that is leaving has no highlight). In the same frame `Glide` draws the highlight where it stood, in the piece's layer, which lies under the field and the rows: the words of the answers stand over it. 32 ms later it leaves: its leading edge on `pillLead`, the other two frames behind on `pillTrail(far)`, top, bottom and corner on `place`, to where the stage says its armed answer stands (`answerAt`). Meanwhile the stage's own answers are not lit. When it has arrived the model is told (`glided`): the answers' own highlight then comes, in its usual fade of 120 ms, and `Glide` goes as that one comes, at the strength that makes the two together one highlight of the selection's strength throughout. In the frame it rests it is the strip's own.

**From where** (dp from the glass's corner; the same sums the rows and the grid draw by): the pill of the selected row is x 8 to 712, from 76 plus that row's top to its height below, radius 24. The square on a grid's cell is 44 dp inside its 48 dp cell: for column `c` and line `l` of fourteen columns at a pitch of (720 − 28) / 14 = 49.43, x from 14 + c × 49.43 + 2.71 to 14 + (c + 1) × 49.43 − 2.71, y from 84 + l × 48 + 2 to 84 + (l + 1) × 48 − 2, radius 14. After the show's four moves that is the fourth cell of the second line: x 165 to 209, y 134 to 178, as `design.md` has it.

**Where none travels.** Out of the welcome (Esc before the show): the handle fades with the rest of the stage, and the key's step comes with its own highlight. Where the screen that lands arms no answer ("Now press your keys"). And where a typed character ends the piece: the list for that character has its own pill.

There is no unit test for this task: the app module has no test source set. Its check here is that it builds; Task 12's device checks 17 and 18 look at it, frame by frame.

- [ ] **Step 1: Where the highlight sets off, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Three changes: its state after the piece's; `land` saying where it stood; and a typed character ending it.

Find:

```kotlin
    /** The piece ended for a typed character: the key's step has not been in view yet, and comes when the field is empty again. */
    private var unseen = false
```

Make it:

```kotlin
    /** The piece ended for a typed character: the key's step has not been in view yet, and comes when the field is empty again. */
    private var unseen = false
    /**
     * The one highlight on its way from where the show left it into the armed answer of the screen that lands: where it
     * stood when the landing began, in dp from the glass's corner (left, top, right, bottom, corner radius). Null: none
     * travels, or it has arrived.
     */
    var glideFrom by mutableStateOf<FloatArray?>(null); private set
    /** While it is on its way the stage's own answers are not lit: one highlight, never two. */
    val gliding: Boolean get() = glideFrom != null
    /** Where the stage's armed answer stands, in px from the glass's corner (left, top, right, bottom), as the stage last said while the highlight was on its way. */
    var answerAt by mutableStateOf<FloatArray?>(null)
    /** It has arrived: from now on the highlight is the answers' own. */
    fun glided() { glideFrom = null }

    /**
     * Where the highlight of what stands now is drawn, in dp from the glass's corner: the square on a grid's cell (44 dp
     * in its 48 dp cell, radius 14), else the pill on the selected row (radius 24). By the sums the rows and the grid
     * draw by. Null: nothing is selected.
     */
    private fun highlight(): FloatArray? {
        val r = current ?: return null
        val top = Metrics.field.value + Metrics.pad.value + Metrics.tops(results)[selected].value
        val grid = r.body as? Body.Grid
            ?: return floatArrayOf(Metrics.pad.value, top, Metrics.width.value - Metrics.pad.value, top + Metrics.rowHeight(r).value, 24f)
        val pitch = (Metrics.width.value - 28f) / grid.columns
        val inset = (pitch - Metrics.cell.value) / 2f + 2f
        val left = 14f + (cell % grid.columns) * pitch
        val up = top + 8f + (cell / grid.columns) * Metrics.cell.value
        return floatArrayOf(left + inset, up + 2f, left + pitch - inset, up + Metrics.cell.value - 2f, 14f)
    }
```

Find:

```kotlin
        if (results.isNotEmpty()) { heldHeight = Metrics.height(this); scope.launch { delay(hold(Motion.LANDS_AFTER_MS)); heldHeight = null } }
        clear()
```

Make it:

```kotlin
        if (results.isNotEmpty()) { heldHeight = Metrics.height(this); scope.launch { delay(hold(Motion.LANDS_AFTER_MS)); heldHeight = null } }
        // The one highlight goes on: from the grid's square, or from the list's pill, into the armed answer of the screen that
        // lands. Where that screen arms none, or nothing was highlighted (the welcome), none travels.
        answerAt = null
        glideFrom = highlight()?.takeIf { due != null && FirstRun.armed(first()) >= 0 }
        clear()
```

Find:

```kotlin
        playing = null; cued = false; cueUp = false; standsAt = null; heldHeight = null
        if (was == FirstRun.Playing.SHOW) clear()
```

Make it:

```kotlin
        playing = null; cued = false; cueUp = false; standsAt = null; heldHeight = null; glideFrom = null
        if (was == FirstRun.Playing.SHOW) clear()
```

- [ ] **Step 2: The highlight on its way, in `app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt`.** Two changes: three imports; and `Piece` composing `Glide`, which stands after it.

Find:

```kotlin
import androidx.compose.runtime.withFrameNanos
```

Make it:

```kotlin
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
```

Find:

```kotlin
    Welcome(model, gate, ends, night, through, onDesk)
}
```

Make it:

```kotlin
    Welcome(model, gate, ends, night, through, onDesk)
    Glide(model)
}

/**
 * The one highlight on its way into the key's step (docs/design/first-run/design.md §3, "The landing in K1"): from
 * where the show left it (the grid's square, or the list's pill) into the armed answer of the screen that lands. It is
 * drawn in the piece's layer, under the field and the rows, so the answers' words stand over it. Its leading edge goes
 * first, the other follows two frames later, and top, bottom and corner go on `place`. Arrived, it is the answers' own
 * highlight: that one comes in its usual fade, and this one goes as that one comes, at the strength that makes the two
 * together one highlight of the selection's own strength throughout.
 */
@Composable
private fun Glide(model: OverlayModel) {
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val fill = selectionFill(MaterialTheme.colorScheme, dark, LocalGlass.current)
    val rim = Color.White.copy(alpha = if (dark) 0.30f else 0.55f)
    // Where it set off: kept here while it hands over, after the model has let it go.
    var from by remember { mutableStateOf<FloatArray?>(null) }
    model.glideFrom?.let { if (from !== it) from = it }
    val lead = remember { Animatable(0f) }
    val trail = remember { Animatable(0f) }
    val place = remember { Animatable(0f) }
    val given = remember { Animatable(0f) }
    LaunchedEffect(from) {
        if (from == null) return@LaunchedEffect
        lead.snapTo(0f); trail.snapTo(0f); place.snapTo(0f); given.snapTo(0f)
        // It leaves once what it stood on has begun to fade.
        delay(motion.hold(Motion.GLIDE_AFTER_MS))
        coroutineScope {
            launch { lead.animateTo(1f, motion.pillLead()) }
            launch { withFrameNanos { }; withFrameNanos { }; trail.animateTo(1f, motion.pillTrail(far = true)) }
            launch { place.animateTo(1f, motion.place()) }
        }
        model.glided()
        given.animateTo(1f, motion.fade(Motion.TAKES_OVER_MS))
        from = null
    }
    // (A typed character takes the stage away under it: then it is simply gone.)
    val start = from?.takeIf { model.stage != null } ?: return
    Canvas(Modifier.fillMaxSize()) {
        val u = size.width / WIDE
        val to = model.answerAt
        fun edge(i: Int, by: Float) = if (to == null) start[i] * u else mix(start[i] * u, to[i], by)
        // To the right the right edge leads, to the left the left one.
        val right = to == null || to[2] >= start[2] * u
        val l = edge(0, if (right) trail.value else lead.value)
        val r = edge(2, if (right) lead.value else trail.value)
        val t = edge(1, place.value)
        val b = edge(3, place.value)
        val corner = CornerRadius(mix(start[4], 16f, place.value.coerceIn(0f, 1f)) * u)
        // The answers' own highlight is there at `given` of its strength: this one is what is missing of one whole highlight.
        val a = fill.alpha
        val g = given.value.coerceIn(0f, 1f)
        val own = if (g <= 0f) 1f else if (a >= 0.99f) 1f - g else ((1f - (1f - a) / (1f - a * g)) / a).coerceIn(0f, 1f)
        if (r - l > 1f && b - t > 1f) {
            drawRoundRect(fill.copy(alpha = a * own), Offset(l, t), Size(r - l, b - t), corner)
            drawRoundRect(rim.copy(alpha = rim.alpha * own), Offset(l + 0.5f, t + 0.5f), Size(r - l - 1f, b - t - 1f), corner, style = Stroke(1f))
        }
    }
}
```

- [ ] **Step 3: The answers say where the armed one stands, in `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`.** Two changes: two imports, and the strip.

Find:

```kotlin
import androidx.compose.ui.graphics.graphicsLayer
```

Make it:

```kotlin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
```

Find:

```kotlin
    val strip: @Composable () -> Unit = {
        OptionStrip(labels, armed.coerceIn(0, (labels.size - 1).coerceAtLeast(0)), onChoose = { model.stageArm(it) },
            // (A click counts from the moment the keys do: "Agree" is consent, by whichever way it is given.)
            onRun = { if (model.seen) { model.stageArm(it); answers.getOrNull(it)?.let(onAnswer) } }, vertical = true, lit = armed >= 0)
    }
```

Make it:

```kotlin
    val density = LocalDensity.current
    val strip: @Composable () -> Unit = {
        OptionStrip(labels, armed.coerceIn(0, (labels.size - 1).coerceAtLeast(0)), onChoose = { model.stageArm(it) },
            // (A click counts from the moment the keys do: "Agree" is consent, by whichever way it is given.)
            onRun = { if (model.seen) { model.stageArm(it); answers.getOrNull(it)?.let(onAnswer) } },
            // First run's show hands its one highlight over to the armed answer (`Glide`): while it is on its way the answers
            // say where the armed one stands (slots of 32 dp, 4 apart), and are not lit themselves.
            modifier = Modifier.onGloballyPositioned { c ->
                if (model.gliding) with(density) {
                    val at = c.positionInRoot()
                    val top = at.y + armed.coerceAtLeast(0) * 36.dp.toPx()
                    model.answerAt = floatArrayOf(at.x, top, at.x + c.size.width, top + 32.dp.toPx())
                }
            },
            vertical = true, lit = armed >= 0 && !model.gliding)
    }
```

- [ ] **Step 4: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt
git commit -m "First run: the one highlight travels from the show into the armed answer of the key's step, and is that answer's own when it rests"
```

### Task 12: The hooks, and the checks on a device

**Files:**
- Modify: `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`
- Modify: `bl`
- Modify: `CLAUDE.md`
- Create: `docs/research/first-run-welcome.md`

**Interfaces:**
- Consumes: `OverlayModel.begin(at: Float?)`, `show(only: Show.Still?)`, `land()`, `press(press, again)`, `playing`, `began`, `cast`, `laps` (Task 7), `gliding` (Task 11); core `Show.Still`, `FirstRun.Press`, `FirstRun.greets(…)`; the `first`, `key` and `dump` cases of `DebugReceiver` as part 3 left them. Hooks that exist and need no change: `./bl debug first` (it already prints `overture=` and `slow=`), `first new|update|off|shown|replay [show]`, `./bl debug keys TEXT`, `type TEXT`, `close`, `pref key seen|no`, `./bl open stay [slow=4] [opening=…]`, `./bl shot NAME`, `./bl backdrop`, `./bl idle`.
- Produces: `./bl debug first welcome [at MS]` (the welcome in the open panel, or standing still at a moment of the paper's clock), `first show [at apps|row|stop|sum|answer|flight|grid|cell]` (the show, or standing still at a named moment), `first land` (the key's step set down); `./bl debug key …` is taken by the piece first, as the keys are; `./bl debug dump` says `playing=`, `cast=`, `laps=` and `gliding`; `./bl debug first` says `greets=`. The list of checks for the coordinator, with a place for each answer.

**The engineer builds and does not touch a device: the coordinator runs the checks and writes the answers into the file.**

Nobody of this plan has seen what it draws. So the checks lean on stills: the welcome can be stood still at any moment of `motion.md`'s own clock, and the show at eight named moments, and each check says what must be where in that picture. Motion is filmed and stepped through, with the frames named in which to look.

The hooks play the piece in an open panel whatever the stored state is. They write nothing of first run's state but what playing the piece writes anyway (that it was shown, where a run stands). The dump says whether the show's rows are worked out, never what they are: they name apps of the device.

The file of checks is public, like the repo: it has no serial and no build number, and it never names an app of the device or the letter Booklight types for them.

There is no unit test for this task: the app module has no test source set. Its check here is that it builds.

- [ ] **Step 1: The hooks, in `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`.** Six changes; three are a piece of one long line. In the header: what is new. In the `key` case: the piece takes a key first. In `dump`: where the piece is. In the `first` case: the piece's own words, the usage, and `greets=`.

Find:

```kotlin
 *   first answer NAME | first ran open|search|sum | first at SCREEN (core `FirstRun`: only first run's own fields, but for `key` and `answer agree`)
```

Make it:

```kotlin
 *   first answer NAME | first ran open|search|sum | first at SCREEN (core `FirstRun`: only first run's own fields, but for `key` and `answer agree`)
 *   first welcome [at MS] (first run's welcome in the open panel; with `at`, standing still at that ms of the paper's clock, motion.md §2.1) |
 *   first show [at apps|row|stop|sum|answer|flight|grid|cell] (its show; with `at`, up to that moment at once, and then it stands) | first land
 *   While the piece plays, `key …` is taken by it first, as the keys are; dump says `playing=`, `cast=ready|none`, `laps=`, `gliding`.
```

Find:

```kotlin
                val m = act?.model ?: return@post out("no panel")
                when (arg) {
                    "down" -> m.down(false)
```

Make it:

```kotlin
                val m = act?.model ?: return@post out("no panel")
                // First run's opening piece takes a key first, as the panel's keys do (core `FirstRun.pressed`): Enter is the cue in
                // the welcome; any other of these sets the key's step down. Nothing of the piece is run.
                if (m.playing != null && m.press(if (arg.startsWith("enter") || arg == "stay") FirstRun.Press.ENTER else FirstRun.Press.OTHER)) {
                    // (Twice in one turn, as a quick double press: the second one finds the piece where the first one left it.)
                    if (arg == "enter2") m.press(FirstRun.Press.ENTER)
                    return@post out("the opening piece took it: playing=${m.playing?.name?.lowercase() ?: "none"}")
                }
                when (arg) {
                    "down" -> m.down(false)
```

Find, in one long line:

```kotlin
 due=${m.due?.name ?: "none"}${m.coach
```

Make it:

```kotlin
 playing=${m.playing?.name?.lowercase() ?: "none"}${if (m.began) " cast=" + (if (m.cast != null) "ready" else "none") + " laps=" + m.laps else ""}${if (m.gliding) " gliding" else ""} due=${m.due?.name ?: "none"}${m.coach
```

Find:

```kotlin
                    "shown" -> app.prefs.firstRun { FirstRun.shown(it) }
```

Make it:

```kotlin
                    "shown" -> app.prefs.firstRun { FirstRun.shown(it) }
                    // The opening piece in the open panel, whatever the stored state is: `welcome` plays it from the gate, `welcome at MS`
                    // stands it still at that moment of the paper's own clock (motion.md §2.1), `show` skips the welcome, `show at NAME`
                    // performs the show up to a named moment at once and stands, `land` sets the key's step down. Nothing of first
                    // run's state is written by them but what playing the piece writes: that it was shown, where a run stands.
                    "welcome", "show", "land" -> {
                        val m = act?.model ?: return out("no panel")
                        val at = words.getOrNull(2).takeIf { words.getOrNull(1) == "at" }
                        val still = io.github.kuscher.booklight.core.Show.Still.entries.firstOrNull { it.name.equals(at, ignoreCase = true) }
                        if (words[0] == "show" && at != null && still == null) return out(io.github.kuscher.booklight.core.Show.Still.entries.joinToString(" ") { it.name.lowercase() })
                        if (words[0] == "welcome" && words.size > 1 && at?.toFloatOrNull() == null) return out("welcome | welcome at MS")
                        main.post {
                            if (words[0] == "land") { m.land(); return@post }
                            // The field is emptied first, as for any hook that types: the piece plays over the empty field.
                            while (m.leaveScope()) {}
                            m.type("")
                            m.begin(if (words[0] == "welcome") at?.toFloatOrNull() else null)
                            if (words[0] == "show") m.show(still)
                        }
                        return out(if (words[0] == "land") "landing" else "${words[0]}${at?.let { " stands at $it" } ?: " plays"} (`dump` says where it is; a key ends it)")
                    }
```

Find, in one long line:

```kotlin
| shown | answer NAME | ran open|search|sum | at SCREEN | older")
```

Make it:

```kotlin
| shown | answer NAME | ran open|search|sum | at SCREEN | older | welcome [at MS] | show [at NAME] | land")
```

Find, in one long line:

```kotlin
slow=${FirstRun.slow(f, overture)} fits=
```

Make it:

```kotlin
slow=${FirstRun.slow(f, overture)} greets=${FirstRun.greets(f, motion, reader, plain = true, screenDp = dp)} fits=
```

- [ ] **Step 2: Say them in `bl`'s header.** One change.

Find:

```bash
#   ./bl debug first answer not_now|go_on|change_key|skip|agree|done | ran open|search|sum | at k1|k2|k3|k4|l2|l3|l4|q|c
```

Make it:

```bash
#   ./bl debug first answer not_now|go_on|change_key|skip|agree|done | ran open|search|sum | at k1|k2|k3|k4|l2|l3|l4|q|c
#   ./bl debug first welcome [at MS]     first run's welcome in the open panel; with `at`, standing still at that ms of the paper's clock
#   ./bl debug first show [at apps|row|stop|sum|answer|flight|grid|cell] | land     its show, or the show up to that moment and standing; the key's step set down
# While the piece plays, `./bl debug key enter|esc|tab|…` is taken by it first, as the keys are, and `dump` says `playing=`, `cast=`, `laps=`.
```

- [ ] **Step 3: Say in `CLAUDE.md` what is drawn now, how to drive it, and what to mind.** Four changes: the `FirstRun.kt` line of the layout (a piece of one long line), the first-run paragraph of the dev loop, the rule on flat glass, and three gotchas after the one on first run's choices.

Find, in one long line:

```markdown
the choices are a list of two rows, and the welcome and the show are to come),
```

Make it:

```markdown
the choices are a list of two rows; `Show.kt` is the opening's show as data, its beats, their times and its example flight; `overlay/Welcome.kt` draws the welcome on one clock, whose marks are `Lights` in `overlay/Motion.kt`, and the highlight's way into the key's step),
```

Find:

```markdown
  into a file of this repo, it names an app of the device). The checks for a device: `docs/research/first-run-key.md` (the key's step) and
  `docs/research/first-run-lessons.md` (the lessons, the question, the choices, the ending).
```

Make it:

```markdown
  into a file of this repo, it names an app of the device). The checks for a device: `docs/research/first-run-key.md` (the key's step),
  `docs/research/first-run-lessons.md` (the lessons, the question, the choices, the ending) and `docs/research/first-run-welcome.md` (the
  welcome and the show).
  The opening piece (the welcome, then the show) begins a new installation's very first opening, which runs at Slow. In an open panel:
  `./bl debug first welcome` plays the welcome, `first welcome at MS` stands it still at that ms of `motion.md`'s own clock (for `./bl shot`
  beside the prototype's frame of the same moment), `first show` plays the show, `first show at apps|row|stop|sum|answer|flight|grid|cell`
  stands it still there, `first land` sets the key's step down. While it plays `./bl debug key …` is taken by it first, as the keys are;
  `dump` says `playing=welcome|show|landing|none`, `cast=ready|none`, `laps=` and `gliding`, and `./bl debug first` says `overture=`,
  `slow=` and `greets=`. The show's first rows are apps of the device: never copy them from a dump into a file of this repo.
```

Find:

```markdown
  sculpted highlights (Alex: "not too 3D esp the highlights. I do like the white outline").
```

Make it:

```markdown
  sculpted highlights (Alex: "not too 3D esp the highlights. I do like the white outline"). Lifted for first run's welcome alone, by
  his word (night, a lamp's light, large type, a drawn knife: `overlay/Welcome.kt`); it holds again from the key's step on.
```

Find:

```markdown
  is either). They are left, and the run is over, by Enter at rest, a typed letter, their second row, or the panel closing.
```

Make it:

```markdown
  is either). They are left, and the run is over, by Enter at rest, a typed letter, their second row, or the panel closing.
- While first run's opening piece plays (`OverlayModel.playing`), the show performs with the panel's real rows and runs nothing: its
  rows are the cast's (`BooklightApp.cast`), whose every action carries core `Show.NOTHING`; `stage` is null; `Panel.keys` and a layer
  over the glass give every key and click to `OverlayModel.press` first (core `FirstRun.pressed`), and `enter`, `runRow`, `went` and
  `OverlayActivity.run` return at once. Only a typed character passes, and the piece is over by then. The key's step it lands in takes a
  key 350 ms after the landing (`cameAt`, `seen`): the piece is `LANDING` for that long. `OverlayModel.playing` is declared before
  `stage`, which reads it while the model is made: keep that order.
- The welcome's height is `Metrics.welcome` under the field (468 dp in all), from which core `FirstRun.WELCOME_SCREEN_DP` is reckoned.
  Its layer is the first child of the glass, 720 × 468 dp, cut by the glass: nothing of it may be drawn outside that layer.
- The glass's shader (`Glass.kt`, `GLASS`) is compiled on the device, not by the build: a mistake in it stops every panel from opening.
  After a change to it, open the panel on a device before anything else.
```

- [ ] **Step 4: Write `docs/research/first-run-welcome.md`**

```markdown
# First run: the welcome and the show on a device (part 4)

*The checks for part 4 of the first-run plan (`docs/superpowers/plans/2026-10-05-first-run-4-the-welcome.md`).
Whoever runs a check writes its answer under it, with the date and "the Lenovo Googlebook" or "the HP Googlebook".
Until then an answer reads "not run".*

**Before the first check**

- Choose the device by its model (`adb devices -l`), never by its place in the list; set `ANDROID_SERIAL`.
- Close the Booklight window, then `./bl install`. `./bl idle` before every `./bl open`: the panel takes the keyboard.
- Never `uiautomator dump`. No sign-in is made for a check, and no other app is opened by one. A capture or a film
  of the screen shows real windows: none is kept or committed. `./bl shot NAME` is the panel's own window, and
  `./bl backdrop` puts a window of test content (a white page, a dark terminal) behind the panel.
- This file is public: no serial, no build number, nothing about what is installed. The show's first beat lists
  apps of the device, and `./bl debug dump` names them after `rows=`: an answer says "the device's apps" and how
  many, never a name, and never the letter Booklight types for them.
- Write down what `./bl debug pref` says before the first check (`opening=`, `dim=`, `theme=`, `suggestions=`) and
  put them back after the last one. Check 1 comes first: the glass's shader changed, and it is compiled on the
  device.
- **A real first opening**: `./bl debug close`, `./bl debug first new`, `./bl debug pref key no`, then
  `./bl open stay` (a plain start; `stay` keeps the panel up when it loses the focus). `./bl debug first` before it
  says `overture=WELCOME slow=true`. The piece is marked as shown once it has played for two seconds or has ended:
  `./bl debug first new` before the next one.
- **A still of the welcome**: with a panel open and its field empty, `./bl debug first welcome at MS`. MS is the
  paper's own clock (`docs/design/first-run/motion.md` §2.1: ms from the first frame of an opening at Medium). The
  welcome stands at that moment until a key ends it or another hook begins it again. The prototype's frame of the
  same moment is in `docs/design/first-run/first-run.html`, with its light on "The shaft" and its words on A.
  `./bl debug first welcome` plays it from the gate. In a panel that did not begin with the piece the desk behind
  does not dim: that is looked at in a real first opening.
- **A still of the show**: `./bl debug first show at apps|row|stop|sum|answer|flight|grid|cell`. `./bl debug first
  show` plays it from its first letter; `./bl debug first land` sets the key's step down.
- `./bl debug dump` says `playing=welcome|show|landing|none`, `cast=ready|none` (whether this device's rows for the
  show are worked out), `laps=` (how often the show asked for the light), `gliding`, `first=`, `due=`, the
  placeholder as `hint=`, and the window's size in px (divide by the screen's density for dp).
- While the piece plays, `./bl debug key enter|esc|tab|down` are taken by it as the keys are (the answer says so);
  `./bl debug keys TEXT` types a letter at a time, and the first letter ends the piece.
- `./bl open stay slow=4` stretches the piece's clock and every spring four times; `DARK=true ./bl open stay` is
  the dark theme. A film of the screen is cut to the panel's rectangle, looked at frame by frame, and deleted.
- Lengths below are dp from the panel's left and top edges.

## 1. The panel still opens

`./bl debug first off`, `./bl debug pref key seen`, `./bl open stay`, `./bl debug dump`, `./bl shot plain`; again
with `DARK=true`. `./bl logs`: no crash, no line about a shader.

The glass as it was: the veil, the white outline at its everyday strength (not full white), the grain. `playing=none`.

Answer: not run

## 2. Who gets it

`./bl debug first new`, `./bl debug pref key no`, `./bl debug first`: `overture=WELCOME slow=true greets=false`.
`./bl debug first shown`: `overture=NONE slow=false`. `./bl debug first update`: `NONE`. `./bl debug pref key
seen`, `./bl debug first replay show`: `overture=WELCOME slow=false`; `./bl debug first replay`: `NONE`.

Answer: not run

## 3. Night (the paper's 700)

`./bl open stay`, `./bl debug first welcome at 700`, `./bl debug dump`, `./bl shot night`. Light and dark theme;
over the backdrop's white page and over its dark one.

The window 468 dp high and as wide as before. The veil near black in both themes, a tenth of the desk still
showing through; the outline full white, the brightest line there is; a little darker towards the corners. No mark
in the field, no placeholder, no caret (it has blinked off); the `esc` cap white, its right edge at 700. Nothing
else. Is the night too dark, or not a night, in light theme over the white page?

Answer: not run

## 4. The strike (850)

`./bl debug first welcome at 850`, `./bl shot strike`.

The field's band is lit from the caret outwards: to its left end, and part of the way to the right, the front
soft; not yet at its full brightness (the lamp has not caught); a halo round the caret; the caret a dark slit on
x = 72 to 74. Nothing below the field yet.

Answer: not run

## 5. The head lit, the seam (1090)

`./bl debug first welcome at 1090`, `./bl shot seam`.

The whole band white, corners as the glass's own; the `esc` cap in the night's ink now, readable on the white. A
seam of light 6 dp wide on x = 360 from y = 92 down to about 440. The title and the line show dimly.

Answer: not run

## 6. The light opens (1330)

`./bl debug first welcome at 1330`, `./bl shot opening`.

The light is partly open, the same to both sides of x = 360, its edges soft. The title is white in its middle and
dim at its ends: the light's edge crosses the letters as a gradient, not as a cut. No handle yet.

Answer: not run

## 7. The knife (1750)

`./bl debug first welcome at 1750`, `./bl shot knife`.

The light at its shape. The handle stands at x 252 to 468, y 372 to 420, in the selection's colour, "Show off" and
the Enter mark centred on it. The tools come left to right: the first two stand at their angles (40° and 20° left
of upright) or a little past them, the third is on its way out, the fourth and the fifth are not there yet. A tool
that is folded lies behind the handle and does not show.

Answer: not run

## 8. It stands (2900)

`./bl debug first welcome at 2900`, `./bl shot stands`; light and dark; white page and dark; German.

Measure in the shot, at four times the size. The light: upper edge on y = 92 from x 129 to 591, foot on y = 448
from 32 to 688, 24 dp of night between the head and the light, brightest under the lamp. The title centred on 360
in the box y 124 to 172; the line centred on 360 in y 188 to 210, on one line. Five discs of 36 dp, centred on
(247, 323), (302, 312), (360, 308), (418, 312), (473, 323), with `app`, `calc`, `plane`, `smile`, `key` in them,
on stems from the handle's upper edge. The handle and its cue as in check 7. Everything the same to both sides of
x = 360. The words are A: "Welcome to Booklight" · "Your Swiss Army knife launcher. Blades not included." · "Show
off"; German: „Willkommen bei Booklight“ · „Dein Schweizer Taschenmesser zum Tippen. Ohne Klinge.“ · „Kurz
angeben“. In German: is the title inside the light, and is the line whole? No dust in the beam; no light running
round the outline. Is the shaft seen as light over the white page? Are the words read in the time they stand?

Answer: not run

## 9. The hand-over (3470 and 3660)

`./bl debug first welcome at 3470`, `./bl shot hand-1`; `./bl debug first welcome at 3660`, `./bl shot hand-2`.

At 3470: the tools are folded away; the shaft is narrower and its upper edge has slid towards the caret, the foot
a little behind it; the cue has all but faded; the handle has just set off for row one's seat; the night has begun
to lift. At 3660: all that is left of the shaft is a thin seam under the caret, rising; the handle rests, or all
but rests, on x 8 to 712, y 76 to 132, in the theme's own selection colour; the head's light has begun to close on
the caret from both ends; the theme's own glass is nearly back.

Answer: not run

## 10. The welcome, in motion

A real first opening, filmed; again with `slow=4`.

The gate, the strike, the seam of light, the light open, the first tool, the hand-over, the first letter: at Slow
(which the very first opening is) each is 258 ms later than the paper's 257 · 800 · 960 · 1,540 · 1,560 · 3,200 ·
3,820; within two frames each? The lower edge falls from 68 to 468 in one move, and the night follows it. In no
frame is anything drawn outside the glass, or cut by its lower edge while it falls. The top 68 dp are the same
pixels throughout but for the light, the caret and the cap. The window's width and its top edge never move. The
desk behind dims to about half and comes back with the hand-over. Does the lamp read as struck, not faded in? Do
the shaft into the caret and the handle to row one read as one move up?

Answer: not run

## 11. Frame times

`./bl sh dumpsys gfxinfo io.github.kuscher.booklight reset`, a real first opening left to play to the key's step,
then `./bl sh dumpsys gfxinfo io.github.kuscher.booklight`.

How many frames, how many of them janky, the 90th and 99th percentile in ms: a frame of this screen is 8.3 ms. If
frames are late in the welcome, the order of what goes is `motion.md`'s: the bloom round the lit letters, the
rays, the penumbra and the core.

Answer: not run

## 12. The show: apps

`./bl debug first show at apps`, `./bl debug dump`, `./bl shot apps`; then `at row`, `at stop`.

`playing=show cast=ready`. The field holds one letter, the rest of row one's name grey after it, Booklight's mark
in the mark's seat. Up to five rows, all of them apps of the device, each with its icon; the pill on row one with
its strip; the footer's right end `tab` Actions and `esc` Skip. The window is 400 dp with five rows (344 with
four, 288 with three). `at row`: the pill on row three. `at stop`: the pane on that row's third stop, its name
unrolled. How many rows: write the number, not the apps.

Answer: not run

## 13. The show: the sum

`./bl debug first show at sum`, `./bl shot sum`; `./bl debug first show at answer`, `./bl shot answer`.

The field holds `150 + 2`, the row's small line the same, its answer 152, "Copy" with the Enter mark; 212 dp. Then
`150 + 20%` and 180.

Answer: not run

## 14. The show: the example flight

`./bl debug first show at flight`, `./bl debug dump`, `./bl shot flight`; German; with a flight key in
(`./bl debug pref flightkey` as it was) and without; with the device offline.

256 dp. Line one "LH 455 · Lufthansa · San Francisco → Frankfurt/Main"; the headline "Lands in 4 h 07 min" with
one green badge "On time"; at the row's right end "Example", and no strip; the line from x = 72 to 700 with the
plane's tail near x = 443, solid behind it and dots ahead; under its ends "SFO", the time it left and "Boeing
747-8", and "Terminal 1", the time it lands and "FRA", in the device's 12 or 24 hours and with no day beside
them. The footer's left end: "An example. Live times need your own AirLabs key." German: „Landung in 4 Std. 07
Min.“, „Pünktlich“, „Beispiel“, „Ein Beispiel. Live-Zeiten brauchen deinen eigenen AirLabs-Schlüssel.“ The dump:
`phase=in_air`, `share=0.61`, and no action between the row's `<` and `>`. It is the same row with a key in,
without one and offline, and a minute later the headline has not counted down.

Answer: not run

## 15. The show: the grid

`./bl debug first show at grid`, `./bl shot grid`; `./bl debug first show at cell`, `./bl shot cell`.

The chip "Emoji" where the mark was, the scope's placeholder, five lines of fourteen cells, 376 dp; the square on
the first cell. `at cell`: the square on the fourth cell of the second line (x 165 to 209, y 134 to 178), that
cell's name at the footer's left end, `⏎` Copy, the arrows' cap Move and `esc` Skip at its right.

Answer: not run

## 16. The show, in motion

A real first opening, filmed from the hand-over to the key's step; again with `slow=4`.

Counted from the first letter: the sum's row at 1,960 ms, the flight's at 3,352, the plane at its place about half
a second later, the keyword's space at 4,840, the light setting off at 5,080, the landing at 5,856; within two
frames each? The field is never empty before the chip, and each new text replaces the old one in one frame. The
lower edge goes 400 (or less, check 12), 212, 256, 376, each in one move and never back to 68. **The frame in
which the first list comes**: the handle is gone and the pill stands in row one's seat in the same frame; no frame
with two highlights there, none with none. From then on: exactly one highlight in every frame but while the pill
gives way to the grid's square (the product's own change: how many frames have none, or two?). The light runs once
round the outline from the grid on, and the user's keys were not needed for any of it. One movement, or five
slides? Eleven seconds: a pleasure or a wait?

Answer: not run

## 17. The landing

`./bl debug first new`, `./bl debug pref key no`, `./bl open stay`, `./bl debug first show at cell`, then
`./bl debug first land` and at once `./bl debug dump`; a moment later `./bl debug dump`, `./bl shot landed`.

At once: `playing=landing`, `first=K1`, `gliding`, the window still 376 dp. Later: `playing=none`, `first=K1
armed=0 1/5`, 252 dp, no `gliding`; the key's step exactly as `docs/research/first-run-key.md` has it, "Open
Keyboard shortcuts" lit, the engine's mark back, the ordinary placeholder, the field's `esc` cap.

Answer: not run

## 18. The highlight's way into the key's step

As check 17 with `./bl open stay slow=4`, filmed.

The square leaves its cell in the frame the cells begin to fade, travels almost level to the right, its right edge
first, and rests on "Open Keyboard shortcuts" (y 154 to 186, its right edge at 700), where the answer's own
highlight takes its place. In every frame one highlight and one only, of one strength: no second one fading in at
the answer while the first is on its way, no flash and no dip as the one becomes the other. The lower edge waits
for the cells to fade and then draws in to 252; it cuts no cell. From a list's row instead (`./bl debug first show
at row`, then `land`): the same from the pill; is a denser patch seen on the row for the first frames?

Answer: not run

## 19. Keys in the welcome

Each from `./bl debug first welcome` in an open panel, a second in: `./bl debug key enter`: the hand-over begins
at once from wherever the parts are, and the show follows (`playing=show`). `./bl debug key esc`: `playing=landing`,
the lamp out, the key's step at 252 dp; the panel is still open; `./bl debug key esc` a second later closes it.
`./bl debug key tab` and `./bl debug key down`: as the first Esc. `./bl debug keys x`: the lamp is out in that
frame, the field's band is the theme's own glass with the x in it, the list for x lands; `playing=none`;
`./bl debug type` with nothing after it: the key's step stands. With real keys: Shift alone does nothing.

Answer: not run

## 20. Keys in the show

Each from `./bl debug first show`, during a beat: `./bl debug key enter`: `playing=landing`; `./bl idle`: the
focused window is still Booklight's panel, nothing has opened and nothing was copied. `./bl debug key esc`: the
same, and the panel is still open. `./bl debug keys x`: the field holds x alone (not Booklight's text and an x),
no chip, the list is the real one for x. With real keys: Esc held down for two seconds lands the key's step and
does not go on to close the panel.

Answer: not run

## 21. No press reaches the key's armed answer

`./bl debug first new`, `./bl debug pref key no`, `./bl open stay`, `./bl debug first welcome`, and a second in
`./bl debug key enter2`: both Enters are the cue (the hand-over is under way; `playing=welcome`, then `show`).
During the show `./bl debug key enter2`, then at once `./bl debug key enter`.

Of those three the first lands the key's step and the other two do nothing: no Keyboard shortcuts dialog comes,
`./bl debug first` says `hold=NONE` and `helper=0`, and the dump `first=K1 armed=0`. With real keys: Enter pressed
again and again as fast as a hand can, from the welcome to the key's step, and Enter held down through all of it:
no dialog. Half a second after the key's step stands, Enter is that step's own again (it asks for the dialog, as
part 2 has it).

Answer: not run

## 22. Clicks

In the welcome, once the handle stands: a click on the handle begins the show; a click elsewhere on the glass sets
the key's step down. In the show: a click on a row sets the key's step down, and nothing is opened; the pointer
moved over the rows moves no highlight. A click outside the glass closes the panel, as always.

Answer: not run

## 23. Seen once

A real first opening, and `./bl debug close` within its first second: `./bl debug first` has no `show` in `done=`,
and the next opening plays the piece again. A real first opening left for three seconds, then closed: `show` is in
`done=`; the next opening shows the key's step at once, at the speed that is set (`slow=false`), with the
ordinary placeholder.

Answer: not run

## 24. Where it is not played

With the system's animations off, `./bl debug first new`, `./bl debug pref key no`, `./bl open stay`:
`./bl debug first` said `overture=NONE greets=true`; the key's step stands whole in the first frame, 252 dp; the
dump's `hint=` is "Welcome to Booklight"; `done=` has no `show`. With a screen reader on: the same, and the title
is said before the step's own words. Animations on again: the piece plays at the next real first opening.

Answer: not run

## 25. Heights, and nothing outside the glass

From the dumps of checks 3 to 17, in dp: the welcome 468; apps 400, 344 or 288; the sum 212; the flight 256; the
grid 376; the key's step 252. In each shot: is anything cut at the lower edge, or is there glass with nothing on
it under the last line? Over the backdrop, filmed while the lower edge moves: is there a strip of blur without
glass under the panel, or anything of the welcome beside or below the glass?

Answer: not run

## 26. A replay

`./bl debug pref key seen`, `./bl debug first replay show`, `./bl open stay`.

The piece plays at the speed that is set, not at Slow. It lands in "Your key works", and the highlight travels
into "Go on".

Answer: not run

## 27. Nobody else sees a change

`./bl debug first off`, `./bl debug pref key seen`; ten openings by the keys: the opening at the speed that is
set, the placeholder, the mark, the caret and the `esc` cap, the lap of light a while after the opening, the tips,
the copy's line and "your usual" as before; the desk behind is not dimmed unless "Dim the desktop" is on;
`playing=none` and no `cast=` in the dump; `./bl debug pref` before and after says the same.

Answer: not run

## 28. The HP Googlebook, with Alex

Checks 1, 8, 10, 11, 14, 16 and 18 again there.

Answer: not run
```

- [ ] **Step 5: The file of checks holds nothing of a device**

```bash
grep -c "^Answer: not run$" docs/research/first-run-welcome.md
grep -nE "com\.[a-z]+\.|[0-9A-Z]{8,}|build [0-9]|emulator-" docs/research/first-run-welcome.md | grep -v "io.github.kuscher.booklight" || echo CLEAN
```

Expected: `28`, then `CLEAN` (no package name but Booklight's own, no serial, no build number).

- [ ] **Step 6: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 7: Commit**

```bash
git add app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt bl CLAUDE.md docs/research/first-run-welcome.md
git commit -m "First run: the hooks that stand the welcome and the show still, and the checks to run on a device"
```

- [ ] **Step 8: Hand over.** Report to the coordinator: the commit, that the debug APK is at `app/build/outputs/apk/debug/app-debug.apk`, and that the twenty-eight checks are in `docs/research/first-run-welcome.md`, the first of which comes before all others (the glass's shader changed). Do not install it and do not run a check.

### Task 13: Part 4, checked as a whole

**Files:** none are changed. If a step shows a change that is not committed, it belongs to the task that made it: say which, and stop.

**Interfaces:**
- Consumes: everything above.
- Produces: the report that part 4 is whole.

- [ ] **Step 1: Every core test, with its count**

```bash
./gradlew :core:test --console=plain && ./bl test && for f in core/build/test-results/test/TEST-io.github.kuscher.booklight.core.FirstRun*.xml core/build/test-results/test/TEST-io.github.kuscher.booklight.core.ShowTest.xml; do n=${f##*core.}; echo "${n%.xml} $(grep -o 'tests="[0-9]*"' "$f" | head -1) $(grep -o 'failures="[0-9]*"' "$f" | head -1) $(grep -o 'errors="[0-9]*"' "$f" | head -1)"; done
python3 -c "import glob,re; print(sum(int(re.search(r'tests=\"(\d+)\"', open(f).read()).group(1)) for f in glob.glob('core/build/test-results/test/TEST-*.xml')))"
```

Expected: `BUILD SUCCESSFUL`, no output from `./bl test`, then thirteen lines, each ending `failures="0" errors="0"`:

```text
FirstRunAnswersTest tests="14"
FirstRunGateTest tests="7"
FirstRunHoldTest tests="12"
FirstRunKeyStepTest tests="8"
FirstRunLessonsTest tests="10"
FirstRunOpeningsTest tests="9"
FirstRunPlayingTest tests="7"
FirstRunRecipeTest tests="11"
FirstRunStageTest tests="11"
FirstRunStartTest tests="12"
FirstRunTest tests="13"
FirstRunUpdateTest tests="10"
ShowTest tests="12"
```

and then `688`: 124 first-run tests in twelve classes, 9 of them this part's (`FirstRunPlayingTest`'s seven and two more in `FirstRunStartTest`), the show's twelve, and 688 core tests in all.

- [ ] **Step 2: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 3: What must be gone is gone**

```bash
grep -rn "Overture.SHOW" core/src app/src || echo CLEAN
grep -n "private val SEAM_GROWS\|private val SEAM_DRAWS_IN\|private val FOLDS" app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt || echo CLEAN
grep -n "the welcome and the show are to come" CLAUDE.md || echo CLEAN
grep -n "model.stage?.step == FirstRun.Step.KEY) SystemWords.load" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt || echo CLEAN
```

Expected: four times `CLEAN` (the show without its welcome; the glass's curves outside `Motion.kt`; the layout's "to come"; the system's words asked of what stands and not of what is due).

- [ ] **Step 4: The rules hold**

```bash
grep -rn "^import android" core/src/main || echo CLEAN
grep -rn "withFirstRun(" app/src | grep -v "fun Settings.withFirstRun\|fun Prefs.firstRun\|fun Settings.asUpdate\|^app/src/debug" || echo CLEAN
grep -rn "suggestions = true" app/src/main/java core/src/main | grep -v "core/FirstRun.kt" || echo CLEAN
grep -rn "executor.run(" app/src/main/java/io/github/kuscher/booklight/overlay | grep -v "OverlayActivity.kt.*app.executor.run(a.effect, this)\|OverlayModel.kt.*app.executor.run(if (dir < 0) n.down else n.up)" || echo CLEAN
git diff 066c734 -- app/src/main/AndroidManifest.xml app/src/debug/AndroidManifest.xml | grep . || echo CLEAN
git diff 066c734 -- app/src/main core/src/main | grep -E "^\+.*(HttpURLConnection|openConnection|URL\(|\.fetch\(|Socket\(|okhttp)" || echo CLEAN
grep -rn "FlightSamples\|src/test/resources\|flight-LH455" app/src/main core/src/main || echo CLEAN
grep -c "RuntimeShader\|Bitmap\|ImageBitmap\|Random\|R.drawable\|R.raw" app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt
grep -rnE "[^a-zA-Z](delay|tween)\([0-9]" app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt || echo CLEAN
grep -c "Show.NOTHING" app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt
grep -c "if (model.began)" app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt
grep -c "^uniform " app/src/main/java/io/github/kuscher/booklight/overlay/Glass.kt; grep -c "setFloatUniform(" app/src/main/java/io/github/kuscher/booklight/overlay/Glass.kt
git log 066c734..HEAD --format=%B | grep -inE "co-authored-by|claude|generated with|session" || echo CLEAN
grep -rnE "emulator-|ANDROID_SERIAL=[A-Z0-9]" docs/research/first-run-welcome.md || echo CLEAN
git log --oneline 066c734..HEAD | wc -l
git status --short
```

Expected, in this order: seven times `CLEAN` (no Android in core; no write of first run's state in the app but through `Prefs.firstRun`; suggestions switched on in core's `answer` alone; no way to the executor from the panel but `OverlayActivity.run`'s and a level's nudge; no change to a manifest, so no new permission and no `<queries>` line; no network call added; nothing of the debug flight samples or of the test resources in `main`); `0` (the welcome has no shader of its own, no bitmap, nothing random, no asset); `CLEAN` (no time written into the welcome: they are `Motion.kt`'s); `3` (the show's rows carry nothing to run: said once, and used for the sum's action and for every action of every other row); `3` (the piece is composed, and its light asked for, only in a panel that began with it); `10` and `10` (the glass's uniforms: each declared once and set once); twice `CLEAN` (no attribution in the commits of this part; no serial in the file of checks); the number of commits since `066c734` (`12`, and one more for each fix a review asked for); and no line from `git status`.

- [ ] **Step 5: Report.** The commits of Tasks 1 to 12 by their first lines, the results above, that the branch was not pushed, and that the twenty-eight device checks in `docs/research/first-run-welcome.md` are all "not run", the first of which (the panel still opens: the glass's shader changed) comes before every other.

---

## The parts that follow

| Part | What it builds | What it needs from this part |
| --- | --- | --- |
| **5. Motion, the replay, the documents, the device pass** | `motion.md`'s transitions between the screens of parts 2 and 3: T1 (the key's step set down part by part, its caps coming up a beat apart: also at the landing of the show, where this part lets it fade in whole), T2 to T4 (the hand-off, the reveal, the large cap pressed and released, the reveal's lap), T5 (a lesson set down, the recipe written a letter at a time), T7, T8 (the sum, the question rising), T9 (the question to the choices, the switch), T10 (the fold, with the lap of light); the stage's answers drawn pressed. What this part left of the show's own motion, if the films ask for it: the cells leaving as a wave, the pill condensing into the grid's first cell, a typed key's list taking the welcome's handle as its pill, Booklight's mark coming up as the head's light passes its seat. "First steps" as a command and as a row on the window's Start page (the row plays the piece: `FirstRun.replay(s, overture = true)`), with `first_later_hint`, the word "Not now" leaves on the key's step. Action + Quick Insert on the Start page and in the README. The documents: `store-submission/forms/data-safety.md`, `PRIVACY.md`, `ux-model.md`, `design-system.md` (the welcome as the one place where the daily panel's rules are lifted; `Lights`), `PICKING-UP.md`, `CHANGELOG.md`, `docs/design/first-run/BUILD.md`. The screen reader pass. Every open device check | `OverlayModel.playing`, `began`, `begin()`, `press()`, `land()` and the order of `OverlayActivity.onCreate` (the key, the count, how the opening begins, the model, the piece begun), for a replay that asks for the piece; `FirstRun.greets` and `OverlayModel.greets` for the screen reader pass; `Lights` and `Welcome.kt`'s `Lit` as the one place the welcome's motion is tuned; `Glide` and `OverlayModel.glideFrom`, `answerAt`, `gliding` as the model for a highlight handed between two parts (T1's armed answer, T8); `heldHeight` for a lower edge that waits; `OverlayModel.laps` as the trigger for a lap that Booklight asks for itself (the reveal's, the fold's); the hooks `first welcome [at MS]`, `first show [at NAME]`, `first land`; `docs/research/first-run-welcome.md`'s answers, and what they say must be tuned |
