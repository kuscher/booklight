# First run, part 5: the last part. Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** First run can be asked for again ("First steps": a command in the panel, a row on the window's Start page) and is whole on an installation that has been lived in; its screens are set down part by part and show what was pressed; what the device said of the welcome is mended; a screen reader is told each thing once; and the documents, the window and the README say what first run is now, with one list of every check that is still to be run on a device.

**Architecture:** Core decides and is tested: what "First steps" does to a run and what the window's row offers (`FirstRun.again`, `againHere`, `offer`, `later`), which steps a run has where sums are switched off, what the cue does where there is nothing to show (`FirstRun.pressed`, `handOver`), when a screen is in view (`FirstRun.inView`), and **when each part of a stage comes, for every way a screen can come** (`SetDown.marks`: a schedule as data, with its times handed in). In the app, `overlay/Motion.kt` holds every time and curve of that schedule (`PACE`); `OverlayModel` works a screen's marks out once, in the moment it comes, and counts a key by them; `overlay/FirstStage.kt` draws every part of the stage by its mark on one clock, by graphics layer only, so that no part's place in the layout ever changes and a still at any moment is whole. "First steps" is one command (`CommandsProvider`, caught in `OverlayActivity.run` before anything is run) and one row (`window/Pages.kt`), both writing through `Prefs.firstRun`. The window is still exactly the panel; nothing new is asked of the system.

**Tech Stack:** Kotlin 2.4, JUnit 4 in `core/` (plain JVM, no `android.*`), Jetpack Compose in `app/` (compile SDK 37, min SDK 34), the repo's `./bl` helper over adb.

**Spec:**
- `docs/design/first-run/00-brief.md`, **"Settled for the build"**: it wins over every other paper. For this part: point 1 (the suggested key is Action + Quick Insert) and point 5 (a keyboard without that key is offered Action + M), which the window's Start page and the README now tell too; point 6 (nothing opens in first run: the lessons are practice), which is why `motion.md`'s T6, "the step aside", is not built; point 7 (Booklight never opens itself over another window), which binds what "First steps" may do.
- `docs/design/first-run/BUILD.md`: what parts 1 to 4 built, the table "Decided on the way", "Known, and left for a later part", and "Still open on a device". Its part 5 is the coordinator's to write: no task touches that file.
- `docs/design/first-run/motion.md` §3 ("The other transitions": T1 to T10, and "Clocks"), §4 (the large cap, pressed), §5 (interruption), §6 (system animations off), §7 (what to cut first), §8 (measuring: the hooks); and §2.2 for what part 4 left of the show's own motion.
- `docs/design/first-run/design.md` §4 to §8 (the skeleton, the key with `first_later_hint`, the lessons, the question and the ending, the strong moments), §9 (progress and leaving), §10 (every state: animations off, a screen reader, the longest German lines), §11 (the replay: "First steps" in the panel and in the window) and §14, row 17 (the window's row plays the opening piece, the typed command does not).
- `docs/design/first-run/pm.md` §6 (skips, quits and returns) and §7 (the replay); `eng.md` §2 (where "First steps" lives in the code), §7 (motion hooks) and §8 (tests, hooks, races).
- The prototype, `docs/design/first-run/first-run.html`: the reference a still is laid beside.
- `docs/research/first-run-key.md`, `first-run-lessons.md`, `first-run-welcome.md`: what was checked on a device in parts 2 to 4, and what each left open. `first-run-welcome.md` says what was seen in the welcome that this part mends.
- `docs/superpowers/plans/2026-10-05-first-run-4-the-welcome.md` and `2026-10-05-first-run-3-the-lessons.md`: parts 4 and 3, on which this builds.

**Who sees what in a build of this part alone.** Someone with no run, who types nothing of it: nothing new at an opening, nothing written, nothing composed. The same person finds one more command when typing for it ("First steps", also `tour`, `welcome`, `intro`), one more line in the list of everything, one more row on the window's Start page, and on that page the way to a key as first run tells it. A new installation: the welcome and the show as in part 4, with three things mended (no ring as the night lifts, no words before the lamp's light reaches them, no empty highlight before the first rows) and Booklight's mark up before it types; then every screen of the run set down part by part, an answer shown as pressed, the key that lands held down on the glass and checked, and a lap of light where the key works and where the run folds to the bare field. Someone who says "Not now" to the key, or whose run has stood in its three openings: one placeholder, once, that the first steps wait in Booklight's window. With the system's animations off: every screen whole in its first frame, as before. A screen reader: each screen once, the greeting once, a switch as a switch. Nothing is released by this part: the version stays 3.0, code 8.

## Global Constraints

- Branch `first-run`. Local only: never pushed, no tag, no release.
- **Nothing is released by this part.** No version name or version code is changed, and no file says that a version with first run is out: the changelog's entry is headed "Unreleased", the status says "built, on a branch".
- No attribution lines of any kind in commits or code: no `Co-Authored-By`, no Claude lines, no session links.
- The repo is public: no device serials, no adb names, no build numbers, nothing about what is installed on a device, in any file or commit message. The lessons' example and the show's first rows are apps of the device: they are shown on the glass and by the debug hooks, and are never written into a file or a commit message.
- `core/` has no `android.*` import.
- Every user-facing string is a resource, in English (`res/values`) and German (`res/values-de`). In German a quotation opens with „ (U+201E) and closes with “ (U+201C); an English apostrophe is ’ (U+2019). German says „Aktion“ for the Action key.
- No new permission, and no new line in `<queries>`. No manifest is changed.
- No network: nothing in this part opens a connection.
- Comments in the surrounding code's own style and density: plain sentences that say why; a KDoc where the neighbours have one.
- Never run `uiautomator dump`. A device is chosen by its model (`adb devices -l`), never by its place in the list. Only the coordinator touches a device: the engineer builds, and runs no `./bl` command but `./bl test` and `./bl build`.
- **Every write of first run's state goes through `Prefs.firstRun { … }`** (`data/Prefs.kt`): the change is worked out inside the settings' own update, never from a state read before it. In `OverlayModel` that is `step { … }`, in `OverlayActivity` it is `first(app) { … }`, in the window it is `app.prefs.firstRun { … }`.
- **The order for every panel** is: the key landing (if the key made the panel), then the opening counted, then how the opening begins (`FirstRun.overture`, with the same `plain` as the count), then the model, then what stands. Nothing asks `FirstRun.stage` or `FirstRun.overture` before `FirstRun.opening` was applied.
- **Nothing in first run opens another app or brings Booklight over another window.** The lessons are practice and the show performs: neither runs anything. "First steps" typed in the panel stays in that panel; the window's row starts the panel from Booklight's own window, by the user's click on it, as the Commands page's rows do for an example. No task gives an action another way to the executor, and no task starts an activity from the panel.
- **A screen of first run takes a key or a click only once the answer Enter would run has been in view for 350 ms** (`OverlayModel.seen`, core `FirstRun.inView` with `FirstRun.SEEN_MS`). No motion weakens that: a screen that is set down part by part counts from when that answer begins to show, never from sooner, and no task hands a key to a stage another way. A key pressed during the welcome or the show never reaches the armed answer of the key's step.
- **Consent is never armed and never first**: on the question nothing is armed, "Not now" stands before "Agree", and no motion, no press shown and no hook arms or runs "Agree" for the user.
- **Typing is never held up by motion.** A typed letter puts a stage away in that frame, whatever of it has come.
- **With the system's animations off nothing moves and everything stands**: every part of every screen is in its place in the first frame, every span of motion is nothing (`Motion.held`, `Motion.hold` and the stage's clock say so), and a key still counts 350 ms after the screen came.
- **Someone who has no run sees, writes and pays nothing new at an opening**: no new job, no new read of the system, no write of the settings, no new part composed. The two exceptions are asked for by the user: the "First steps" command where it is typed for, and its row in the Booklight window.
- The panel's rules hold: the window is exactly the panel and is never resized in width; one height for each thing that stands, in `Metrics`, and `Metrics.height(model)` equals what `Panel` draws in every frame; the blur is the window's root view framed to the glass (`OverlayActivity.frameGlass`, which no task touches); nothing is drawn outside the glass; the opening is the field's height, and what is under the field comes at the gate; the field is live.
- **Sizes are known before anything moves.** A part of the stage arrives by its graphics layer (alpha, a shift, a scale about its own centre): its place and size in the layout are final from the first frame, and nothing is measured again while it comes. One highlight per level: the answers have one, the list has one, and there are never two on the stage.
- **All motion's times and curves live in `overlay/Motion.kt`.** The stage's are its `PACE` and the constants beside it; the welcome's are `Lights`. What is core's is the order: `SetDown.marks` says which part comes after which, with the times handed in, and `Show.script` says what Booklight does in the show; both are data with tests.
- **No task changes the glass's shader** (`overlay/Glass.kt`): it is compiled on the device, not by the build, and nothing in this part needs it changed.
- The daily panel's design rules (flat glass, no glow, no illustration, plain copy) are lifted for the welcome alone. They hold for the stage: its motion is the panel's own springs and fades, and nothing on it glows.
- The suggested key is Action + Quick Insert; a keyboard without that key is offered Action + M. The welcome is "Lights on" with the plain shaft and the words A.
- Use a task's code as it is written. A "Find" is a text that stands in its file exactly once at the moment the task runs; "Find, in one long line" is a piece of one line, which also stands in the file exactly once. If a Find does not stand exactly once, stop and report it: do not guess the place.

**The commands of this repo, as every task uses them**
- All core tests: `./bl test` (quiet: no output and exit status 0 mean every test passed).
- One test class: `./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SomeTest' --console=plain`.
- A debug build with its check: `./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT` (the build is good only if this prints the one word `BUILT`).

**Where a late fix to part 4 would touch this plan.** It is written on `first-run` at `5edba67`. Every "Find" below stands in its file exactly once there, or once the tasks before it have run. If a later fix changes one of these places, the task that finds it must read the place again: core `FirstRun.kt` (the header, `State`, `steps`, `again`, `pressed`, `caption`, `starts`, `recipe`, the line above `caption`), `Show.kt` (`script`), the last test of `FirstRunTest`, `FirstRunPlayingTest`, `ShowTest`, `FirstRunStageTest`, `FirstRunOpeningsTest` and `FirstRunRecipeTest`; `Motion.kt` (`hold`, the companion's last constants, `LocalMotion`, the end of `Lights`); `Welcome.kt` (`Sprung` and its two springs, `Lit`, the night's shade, `frontAt`, the words' light, `Glide`, the hand-over in `Welcome`'s loop); `OverlayModel.kt` (the piece's section from `began` to `land`, `stage`, `rearm`, `seen`, `answerStage`, `keyLanded`, `fold`, `closing`, `turned`, `firstHint`, `type`, the landing of a list, `enter`, `runRow`, `flipUsual`, the companion's last constants); `OverlayActivity.kt` (`onCreate` from the key to the arrival, `onNewIntent`, `signal`, `run`'s first lines, `stage`, `landed`, `onDestroy`); `Panel.kt` (`Arrival`, the lap's two effects, the keys for Enter and Ctrl + digit, the body's `transitionSpec` and its `first` branch, the line that composes `ChoicesSaid`); the whole of `FirstStage.kt`; `Strip.kt` (`OptionStrip`); `Rows.kt` (`Slot`, `RowSlots.sync`, `SlotRow`, `RowFrame`, the switch's description); `Field.kt` (the mark, the `esc` cap); `Providers.kt` (`CommandsProvider`), `Guide.kt` (`entries`, `line`), `BooklightApp.kt` (the line above `providers`), `Prefs.kt` (`Settings.firstRun`); `window/Pages.kt` (the Start page's key group), `window/MainActivity.kt` (`onProvideKeyboardShortcuts`); the last lines of `strings_32.xml`, the `guide` array's `settings` line in `strings_20.xml`, `win_key_text` to `win_key_steps` in `strings_11.xml`; `DebugReceiver.kt` (the header, `dump`, the `first`, `pref` and `turn` cases); `CLAUDE.md` (the layout's `FirstRun.kt` line, the first-run paragraph of the dev loop, the gotchas on first run); `bl`'s header; and in the documents the passages Task 16 finds.

**How this plan was checked.** Every task below was applied from this text, in order, to a scratch checkout of `5edba67`: each "Find" stood exactly once when its turn came; the failing tests of Tasks 1 to 4 and 14 failed as said and passed after; after every task `./bl test` passed, the debug build printed `BUILT`, and the tree was the one the code had been developed to; Task 17's commands gave what it expects. **Nothing was run on a device, and nobody has seen what this plan draws.** The stage's motion is built from `motion.md`'s figures and the prototype; how it looks and moves on glass is Task 15's list, every answer of which reads "not run".

## Review Focus

Five things the papers imply that would bite a real user and that nothing would exercise unless a test is written for it. Each has its test in the task that owns the code.

| # | The input or condition | What must hold | Its test |
| --- | --- | --- | --- |
| 1 | Enter, Tab or a click while a screen is still being set down; the Enter that answered the screen before, pressed again at once; a key whose press was made before the screen's answers showed and is handled a long frame later | Nothing is answered: a screen counts from when the answer Enter would run began to show, plus 350 ms, by the key's own time. A screen that comes later than the opening counts from its own coming, one that comes with the opening from the opening | Task 3: `aScreenThatIsStillArrivingIsNotInViewSooner`, `keysPressedWhileAScreenIsSetDownDoNothing` |
| 2 | Every way a screen can come (set down, come back to, in another's place, after a list, at the landing, whole), for every screen: the key's three caps, one cap, a recipe of caps and typed words, the question, a screen without a caption | The answers never come before the band they answer; the lead is the armed answer's mark and never negative; what two screens share does not move in a turn; a screen come back to writes nothing twice; whole means everything there at once | Task 4: `theAnswersComeLastAndTheLeadIsTheArmedAnswers`, `aScreenThatTakesAnothersPlaceMovesOnlyWhatChanges`, `aScreenComeBackToArrivesAsRowsDo`, `aScreenThatStandsWholeHasNothingToCome` |
| 3 | "First steps" asked for where a run is unfinished, where one is finished, where none ever was; with suggestions on; with sums off; with a key known and without | It touches no switch. An unfinished run goes on where it stopped, with what was done kept; a finished one is whole again; with suggestions on there is no question, with sums off no sum's lesson, and the counter counts what is left. The typed command never plays the opening piece, the window's row does | Task 2: `askedAgainItTouchesNoSwitchAndNoKey`, `anUnfinishedRunGoesOnWhereItStopped`, `theTypedCommandStartsWithoutThePiece`, `theWindowsRowPlaysTheWholeOfItAgainThePieceFirst`; Task 1: `withSumsSwitchedOffTheSumsLessonIsNoStep`, `aRunWithSumsSwitchedOffIsWholeWithoutTheSum` |
| 4 | "Not now" on each of the key's screens; a run in its third and fourth opening; a run that was finished; a run that has waited for a week | The bare field says where the steps wait in exactly one opening: the one in which they began to wait. Never after a finished run, never at a later opening, never after "Not now" to the question | Task 2: `notNowOnTheKeyLeavesAWordBehind`, `aRunThatUsedUpItsOpeningsLeavesTheSameWordOnce`, `noOtherAnswerLeavesThatWord` |
| 5 | A device with nothing to show (no app whose name begins with a letter); its rows still being read when the cue is given; an app whose name begins with an emoji, a flag, a letter with a combining mark | The cue never does nothing: it begins the show or sets the key's step down. No recipe shows half a character | Task 1: `theCueWithNothingToShowSetsTheKeysStepDown`, `theHandOverWaitsOnlyForRowsThatAreStillComing`; Task 14: `aNameIsNeverCutInsideACharacter` |

Also pinned: the show leaves the sum's beat out where sums are off and is still a show (Task 1, `whereSumsAreSwitchedOffTheShowLeavesTheSumOut`); a later opening's screen comes back (Task 3, `inALaterOpeningAScreenComesBack`); the paper's own marks for the key's step and for a lesson's recipe (Task 4, `theKeysStepIsSetDownAsThePaperHasIt`, `aLessonsRecipeIsWrittenAsThePaperHasIt`). No JVM test reaches the app: that the stage draws by its marks, that no part's place changes while it comes, that the heights are what is drawn, that a pressed answer runs once, and that the welcome's three mends are mended are Task 15's checks on a device.

## Decided here

Where the papers differ, or are silent, or could not be built as written. Each is Alex's to overturn. Everything of this part's scope that was left out is in rows 24 to 28.

| # | What | Why | What it costs if wrong |
| --- | --- | --- | --- |
| 1 | **"First steps", typed, begins in the panel that is open and never plays the opening piece**; that panel is the first of the run's three openings. **The window's row plays the whole of it in a new plain panel**, started by the click on the row, the opening piece first | The coordinator's note: a piece begun in an open panel is not whole (no dimmed desk, the ordinary lap armed, what stood under the field). And the user who types is at work; the one in the window asked to be shown | A typed "First steps" that should play the piece: the command would close the panel and start a new one, about half a task |
| 2 | **Where a run is unfinished, both go on where it stopped**, with what was done kept, and the row says so ("Go on with the first steps", with the counter). The opening piece is then not played again if it was shown | `pm.md` §7; core's `again` from part 1 | Someone who wants the whole run again must finish or leave the one they have: "Not now" on the key, or the choices |
| 3 | **The command and the row are not offered on a screen lower than 475 dp**, and the list of everything leaves the line out | First run does not stand in the panel there (`FirstRun.fits`): a command that does nothing | A line and a row that lead nowhere on a zoomed external screen |
| 4 | **Asked for again with "Show sums" off: no sum's lesson, and the show leaves the sum out.** The counter says "of 4" (or "of 3" where suggestions are on too) | The notes: lesson 4's example finds the web's row there and could only be skipped; "the show shows nothing that is switched off". `State.sums` is read, never written | Someone with sums off is not shown that Booklight can add. One line to show the lesson with its Skip instead |
| 5 | **The show's apps stay ranked as the engine ranks them for this user**: on an installation that has been lived in, the user's own most used first | "The product performs"; a second ranking for the show would be a show of something else | The rows of the show name what this person uses, on their own glass. Nothing leaves it |
| 6 | **A cue where there is nothing to show sets the key's step down at once.** The notes speak of the emoji scope switched off: the code has no such switch; there is no show only where no app's name begins with a letter, or no emoji grid can be made | The notes: Enter on "Show off" did nothing and the step landed a second late | Nothing: it is a mend |
| 7 | **The word that the steps wait in the window is said once, in the opening they began to wait in**: after "Not now" on the key, and in the first opening after an unfinished run's three. `design.md` §5 has it after "Not now" only | Someone whose run ran out of openings was told nothing, and the run is as hidden for them | One placeholder too many, once. One condition in `FirstRun.later` |
| 8 | **The Start page shows one suggestion with first run's own caption** (Action + Quick Insert, "the key left of A" and "or any keys you like"; Action + M on a keyboard without the key), not two side by side; and **the dialog it opens begins with the five steps** on the window's own page, as the panel's does | One way to a key, told the same in the three places that tell it | The Start page no longer shows that a second combination is free |
| 9 | **A screen takes a key 350 ms after the answer Enter would run began to show.** `motion.md` §5 has Enter run as soon as that answer has begun to show, has Tab during a set-down finish it and arm the first answer, and has "Go on" run at once while the key's landing is shown. Here **a key during a set-down does nothing**, and "Go on" waits its 350 ms like every answer | The rule this part must not weaken, and one rule for every screen, tested in core; a Tab that arms what is not drawn yet is a key answered blind | A screen is deaf for longer than the paper has it: on the key's first screen Enter counts about 0.73 s after the gate at Medium (the answers at 383 ms, then 350 in view; the paper: 0.38 s), and on lesson 3 the keys that skip count about 0.94 s after its band began (its recipe takes 592 ms to write). Typing is never part of this. `SEEN_MS` is one constant in core; hurrying a set-down on Tab is about half a task |
| 10 | **When each part comes is core's (`SetDown`), with the times from `Motion.kt`** | Five ways to come times six kinds of screen is where a schedule goes wrong, and the app has no tests | Two files to read for one transition |
| 11 | **Every part arrives by its graphics layer; the answers never move at a landing** (they fade in where they stand) | The highlight that travels from the grid into the armed answer reads that answer's place from the layout, once | The answers do not rise 12 dp at the landing as the other parts do |
| 12 | **A stage that gives way fades as it stood**: what of it had not come never comes, and it is not finished first | `motion.md` §5: typing is never held up, and nothing is drawn for a screen that is leaving | A half-set-down screen fades half-set-down, for 80 ms |
| 13 | **T2 to T4 are built as `motion.md` has them** (the pressed slot; the stage turned under the dialog 300 ms after the focus was lost; "your key" down for 160 ms in view and 400 under the dialog, the check 60 ms after it comes up, the lap 240 ms after; a held key's repeats doing nothing until the check is whole), **and cannot be looked at with a new key on the test device**: the hook `first press [under]` lands a key as the dialog's would | That device has a shortcut for Booklight already, whose row is never removed | What a real first press under the real dialog looks like is check 38, Alex's own |
| 14 | **T6, "the step aside", is not built**, and neither is its `AGAIN_AFTER_MS` | Settled points 6 and 7: the lessons are practice, nothing opens, and Booklight never opens itself again | Nothing: it is settled |
| 15 | **A panel that opens on a screen of first run has no ordinary lap of light**; first run asks for its own two (the key works; the fold) | Two laps within a second, or a lap as a reward for nothing | In the three openings of a run the glass has no idle lap |
| 16 | **The landing of the opening piece lasts until the key's step is in view** (its armed answer's mark and 350 ms: about 530 ms), and the glass keeps its height for 80 ms also when the welcome is put out while it is tall | Until then every press but a typed character is the piece's, so no Tab moves the armed answer while the highlight is still on its way to it; and a welcome cut from 468 to 252 dp was cut through its words | A deliberate Enter in that half second is lost without a word |
| 17 | **The ring as the night lifts**: the night's deeper shade at the corners is a gradient with no edge (it grows with the square of the distance), and is there only above half night | The still at 3660 ms showed the edge of a two-stop gradient as it faded | The corners of the night are a little less dark between half night and none |
| 18 | **The welcome's words show dimly only as the lamp's light floods the field** (from 40 ms after the strike, over 260 ms), and fully once the shaft has opened on them; at the strike itself nothing stands under the field | `motion.md` §2.1; the still at 850 ms showed them at the strike, with the lamp's first flicker. (The prototype dims them from the strike too) | If the paper means nothing at all until the shaft opens: one factor in `Lit.ambient` |
| 19 | **The show's first rows stand in the frame the handle becomes the pill**: the list has no fade of its own there and row one is whole | The still that showed the highlight empty in row one's seat | Rows two to five still rise a beat apart; row one does not |
| 20 | **Booklight's mark comes up 520 ms after the hand-over began**, about 100 ms before the show's first letter (`motion.md` §2.2) | One of the four pieces part 4 left, and the one that costs a line | One constant in `Lights` |
| 21 | **A recipe too wide for its room is drawn smaller as a whole, from its left end**; it is never cut and never wraps | The band is 112 dp high and holds one line; lesson 3 in German is the tight case | At a large font scale a long recipe is smaller than the caps beside it elsewhere |
| 22 | **A screen reader switched on while the piece plays ends it**: the key's step lands and says itself. The welcome has no spoken way to skip because it is not there to be read | The notes: about 25 changes of the field and rows that announce | Someone who switches a reader on in those eleven seconds misses the show |
| 23 | **Hooks**: `first again`, `first stage [as HOW] [at MS]`, `first press [under]`, `first says`, `first trace N`, `pref sums on` (or `off`), `turn MS enter` | `motion.md` §8; the coordinator's notes: a still at a named moment is surer than a film, a log line surer than a film's clock, and a reader cannot be driven on the test device | Debug code only |
| 24 | **Left out: the pill condensing into the grid's first cell, and the cells leaving as a wave** (`motion.md` §2.2) | Each rebuilds a part of the daily panel (the grid's cells) for those seconds, and neither can be judged without a device | For about 120 ms as the grid comes there is no highlight or two; the cells fade in 80 ms. A task each |
| 25 | **Left out: the highlight's way into the key's step starting from where it was drawn in the middle of a move** | It starts from where the highlight was going; the difference is a few dp for a few frames | Seen only when the piece is ended while the square moves. About half a task |
| 26 | **Left out: a pointer's click on the stage or on the choices counting by the click's own time** | Keys do (Enter, Tab, Ctrl + digit); a click is handled in the frame it is made | A click made before a screen was in view and handled after is taken. One parameter through `Panel` |
| 27 | **Left out: a leave turned round on the choices** still shows the two rows with the run already over (part 3's ledger) | The rows work as on any day; it needs the choices' ending told apart from their leaving | A user who closes on the choices and presses the key again within the fold sees them once more. A few lines in `turned` |
| 28 | **Left out, as not first run's or not this part's**: the old command "Set up the keyboard shortcut", which is left as it is, and the Play listing's paragraph on the key, which still tells the old way (it is named in `PICKING-UP.md` under "Before a release"); the resource names `card_*` and the two settings fields the cards used, which are kept so that no stored setting changes its meaning | The brief: nothing that is not first run, no listing | One text that tells the old way to a key until a release is prepared |
| 29 | **Where the coordinator's notes and the code differ, the code was followed**: `Zero.kt` no longer speaks of a card (`Under.What.STAGE` since part 3), so only core's header and the documents are mended; the night's deeper shade is drawn in `Welcome.kt`, not by the glass's shader, so the ring is mended there and no shader changes; the notes' "emoji scope switched off" has no switch in the code (row 6) | The notes say the code wins where they differ | Nothing |

## What each task hands the next

For every pair of tasks that share a file or a name: what the one produces and the other consumes. Where a later task's "Find" is text an earlier task wrote, the row says so.

| From | To | What |
| --- | --- | --- |
| 1 | 2, 3, 4, 14 | `core/…/FirstRun.kt`: Task 1 changes `State`, `steps`, `pressed` and adds `Cast`, `handOver`; Task 2 changes the header and `again` and adds after it; Task 3 adds above `caption`; Task 4 changes `caption`; Task 14 changes `starts` and `recipe`. They do not meet |
| 1 | 9 | `FirstRun.State.sums`, `FirstRun.Cast`, `FirstRun.pressed(playing, press, cast)`, `FirstRun.handOver(cast, waited)`, `Show.script(sum: String?, …)` |
| 2 | 6 | `FirstRun.AGAIN`, `FirstRun.againHere(s)`, `FirstRun.later(before, after)` |
| 2 | 7 | `FirstRun.offer(s)`, `FirstRun.Offer`, `FirstRun.again(s, overture)` |
| 3 | 10 | `FirstRun.inView(press, opened, came, lead)`, `FirstRun.SEEN_MS`, `FirstRun.comesBack(s)` |
| 4 | 5 | `SetDown.Pace` |
| 4 | 10 | `SetDown.Comes`, `SetDown.Marks`, `SetDown.WHOLE`, `SetDown.THERE`, `SetDown.marks(…)`, `SetDown.piece`, `SetDown.letter`, `SetDown.sameBand`, `SetDown.sameCaption` |
| 5 | 8 | `Lights.MARK_AFTER_MS`, `Lights.DEEPENS_FROM`; `overlay/Welcome.kt` without `Sprung` and its two springs, which are `Motion.kt`'s now |
| 5 | 10 | `PACE`, `PLACE`, `POP`, `Motion.held(ms)`, `Motion.RISE_MS`, `CAP_MS`, `CAP_SMALL`, `PART_MS`, `BAND_AFTER_MS`, `GATE_TO_REST_MS` |
| 5 | 11 | `Motion.PRESS_MS`, `RELEASE_MS`, `KEY_TAP_MS`, `KEY_HELD_MS`, `CHECK_AFTER_MS`, `CHECK_MS`, `LAP_AFTER_MS`, `HELPER_AFTER_MS` |
| 5 | 12 | `Motion.ASK_LEAVES_MS`, `FOLD_CAP_MS`, `FOLD_LAP_MS`, `COACH_AFTER_MS` |
| 6 | 7 | `BooklightApp.firstFits`; `R.string.first_name`, `first_row_text`, `first_row_go_on` |
| 6 | 9 | `overlay/OverlayModel.kt`: Task 6 adds the section "asked for again" above the piece's; Task 9 changes the piece's section. They do not meet |
| 6 | 10 | `OverlayModel.again()`, in which Task 10 says how the screen comes (`came`) in place of the line that set `cameAt` |
| 6 | 11, 12 | `OverlayModel.answerStage` as a block, to which Task 11 adds the pressed slot and Task 12 the kept height |
| 6 | 13 | `OverlayModel.later`; `R.string.a11y_first_later`, `a11y_first_goes_on_change` |
| 6 | 15 | `OverlayModel.again()`, `later`, for the hooks |
| 8 | 9 | `OverlayModel.marked`, which `begin` clears: Task 9 finds that line as Task 8 left it |
| 8 | 12 | `RowSlots.sync(results, motion, held)`, to which Task 12 adds `late`; `Slot.whole` |
| 9 | 10 | `OverlayModel.stop()`, `land()` and `closing()` as Task 9 leaves them: Task 10 finds their `cameAt = SystemClock.uptimeMillis()` lines |
| 10 | 11 | `OverlayModel.lasts`, `marks`, `cameAt`; `OptionStrip(…, shown)`, to which Task 11 adds `pressed`; in `FirstStage.kt`: `Modifier.comesUp`, `BigCap`, `Keys`, the `strip` |
| 10 | 12 | `OverlayModel.seen(made)`, `lasts`; `overlay/FirstStage.kt`'s `CoachLine` |
| 10 | 13 | `OverlayModel.cameAt`, which the stage's announcement is keyed by |
| 10 | 14 | `Written`, the recipe's row in `Keys` |
| 10 | 15 | `OverlayModel.stageAt`, `comes`, `marks`, `seen`, and the private `came(how)`, which Task 15's `stageAs` calls |
| 11 | 12 | `OverlayModel.answerStage` with the pressed slot, to which Task 12 adds the kept height; `laps` asked for by first run, which `Panel` now composes for |
| 11 | 15 | `OverlayModel.keyLanded(under)`, `keyDown`, `landing`, `stagePressed`, for the hooks and the dump |
| 13 | 15 | `OverlayModel.said`, for `first says` |
| 15 | 16 | `docs/research/first-run-last-pass.md`, which the status names |

---

### Task 1: `FirstRun` and `Show`: a run on an installation that has been lived in, and a cue with nothing to show

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/Show.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunTest.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunPlayingTest.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/ShowTest.kt`

**Interfaces:**
- Consumes (in `object FirstRun`, parts 1 to 4): `data class State(run, done, opens, helper, icon, key, suggestions, mark)`, `enum class Step { KEY, OPEN, SEARCH, SUM, ASK, CHOOSE }` with `id`, `fun steps(s: State): List<Step>`, `private fun over(s: State, step: Step)`, `enum class Playing { WELCOME, SHOW, LANDING }`, `enum class Press { ENTER, CUE, TYPES, MODIFIER, OTHER }`, `enum class Ends { NOTHING, BEGIN, LAND, TYPE }`, `fun pressed(playing: Playing, press: Press): Ends`. In `object Show`: `fun script(sum: String, keyword: String, comma: Boolean = false): List<Cue>`, `Step.Typed`, `Step.Sum`, `Step.Flight`, `FLIGHT`, the private times `TO_SUM_MS`, `TO_FLIGHT_MS`.
- Produces:
  - `FirstRun.State` has one more field, last: `val sums: Boolean = true` (sums are answered: "Show sums" is on). Only read, never changed by anything in `FirstRun`.
  - `FirstRun.steps(s)`: `Step.SUM` is a step only while `s.sums`, or once it is over in this run.
  - `enum class FirstRun.Cast { WAITS, READY, NONE }`: what the show has to show on this device.
  - `fun FirstRun.pressed(playing: Playing, press: Press, cast: Cast = Cast.READY): Ends`: the cue in the welcome with `Cast.NONE` answers `Ends.LAND`.
  - `fun FirstRun.handOver(cast: Cast, waited: Boolean): Ends`: `READY` begins, `NONE` lands, `WAITS` does nothing until `waited`, then lands.
  - `fun Show.script(sum: String?, keyword: String, comma: Boolean = false): List<Cue>`: with `sum == null` the sum's beat is left out.

"First steps" (Task 2) asks for the run on an installation that has been lived in, where things may be switched off that a new installation has on. Two of them bite. With "Show sums" off, lesson 4's example ("4 × 18") finds the web's row, not an answer: the lesson could only be skipped, and the show would play a sum the product would not answer. So the sum's lesson is no step there, and the show leaves that beat out: it shows nothing that is switched off. The state says whether sums are answered; nothing here changes it.

And the cue. In the welcome, Enter (or a click on the handle) is the user's own: it begins the show. Where the device has nothing to show (no app whose name begins with a letter), part 4 let that Enter do nothing, and the key's step landed a second after the welcome had stood. A key that does nothing is the worst answer: now the cue sets the key's step down, and the welcome's own hand-over asks the same question of core (`handOver`), so that there is one rule for "what happens when the welcome ends".

- [ ] **Step 1: Write the failing tests for the steps, in `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunTest.kt`.** One change: two tests after the last one.

Find:

```kotlin
    }

    @Test fun theRunOfAnInstallationThatWasThereBeforeIsTheKeyAlone() {
```

Make it:

```kotlin
    }

    /**
     * "Show sums" switched off, as an installation that has been lived in may have it when the run is asked for again:
     * there is no sum to practise, so its lesson is no step. (With the lesson left in, its example found the web's row
     * and the lesson could only be skipped.)
     */
    @Test fun withSumsSwitchedOffTheSumsLessonIsNoStep() {
        val s = State(Run.REPLAY, done = done(Step.KEY, Step.OPEN, Step.SEARCH), key = true, sums = false)
        assertEquals(Screen.Q, FirstRun.screen(s))
        assertEquals(Screen.C, FirstRun.screen(s.copy(suggestions = true)))
        assertFalse(Step.SUM in FirstRun.steps(s))
        // The counter counts the steps this run has: two lessons and the question after the key.
        assertEquals(4 to 4, FirstRun.count(s))
        assertEquals(2 to 4, FirstRun.count(State(Run.REPLAY, done = done(Step.KEY), key = true, sums = false)))
        // Done in this run before the switch went off, it stays a step that is over: the count does not change under the reader.
        assertEquals(6, FirstRun.steps(State(Run.NEW, done = lessons, key = true, sums = false)).size)
        // A new installation's run is no other: whoever switches sums off before the lesson has no lesson.
        assertEquals(Screen.Q, FirstRun.screen(State(Run.NEW, done = done(Step.KEY, Step.OPEN, Step.SEARCH), key = true, sums = false)))
        // The lesson cannot be reached by name either (the debug hooks), and with sums on everything is as it was.
        assertNull(FirstRun.at(State(key = true, sums = false), Screen.L4))
        assertEquals(Screen.L4, FirstRun.screen(s.copy(sums = true)))
    }

    /** A whole run with sums switched off: every screen but the sum's stands once, in its order, and the run ends. */
    @Test fun aRunWithSumsSwitchedOffIsWholeWithoutTheSum() {
        var s = FirstRun.replay(State(key = true, sums = false), overture = false)
        val stood = ArrayList<Screen>()
        while (true) {
            val on = FirstRun.screen(s) ?: break
            stood += on
            s = FirstRun.answer(s, when (on) { Screen.K4 -> FirstRun.Answer.GO_ON; Screen.Q -> FirstRun.Answer.NOT_NOW; Screen.C -> FirstRun.Answer.DONE; else -> FirstRun.Answer.SKIP })
        }
        assertEquals(listOf(Screen.K4, Screen.L2, Screen.L3, Screen.Q, Screen.C), stood)
        assertEquals(Run.NONE, s.run)
        // No switch was touched by any of it.
        assertFalse(s.sums)
        assertFalse(s.suggestions)
    }

    @Test fun theRunOfAnInstallationThatWasThereBeforeIsTheKeyAlone() {
```

- [ ] **Step 2: Write the failing tests for the cue, in `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunPlayingTest.kt`.** Two changes: an import, and two tests after the last one.

Find:

```kotlin
import io.github.kuscher.booklight.core.FirstRun.Ends
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.FirstRun.Cast
import io.github.kuscher.booklight.core.FirstRun.Ends
```

Find:

```kotlin
    }

    /** Enter held down from the cue to the key's step: it begins the show, lands the key's step, and then does nothing. Never a fourth thing. */
```

Make it:

```kotlin
    }

    /**
     * A device with nothing to show (no app whose name begins with a letter): the cue has no show to begin, and sets
     * the key's step down. It never does nothing: "Show off" with Enter beside it must answer Enter.
     */
    @Test fun theCueWithNothingToShowSetsTheKeysStepDown() {
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.WELCOME, Press.ENTER, Cast.NONE))
        assertEquals(Ends.LAND, FirstRun.pressed(Playing.WELCOME, Press.CUE, Cast.NONE))
        // While this device's rows are still being worked out the cue is kept: the show begins once they are there.
        assertEquals(Ends.BEGIN, FirstRun.pressed(Playing.WELCOME, Press.ENTER, Cast.WAITS))
        assertEquals(Ends.BEGIN, FirstRun.pressed(Playing.WELCOME, Press.ENTER, Cast.READY))
        // Everything else is as it is with a show: a typed character types, a modifier is nothing, the landing takes nothing more.
        for (playing in Playing.entries) for (press in Press.entries) for (cast in Cast.entries)
            if (!(playing == Playing.WELCOME && (press == Press.ENTER || press == Press.CUE)))
                assertEquals("$playing $press $cast", FirstRun.pressed(playing, press), FirstRun.pressed(playing, press, cast))
    }

    /**
     * Booklight presses the handle itself once the welcome has stood. With a show it begins; with none the key's step
     * lands in that moment; while this device's rows are still being read it waits for them, a second at most.
     */
    @Test fun theHandOverWaitsOnlyForRowsThatAreStillComing() {
        assertEquals(Ends.BEGIN, FirstRun.handOver(Cast.READY, waited = false))
        assertEquals(Ends.BEGIN, FirstRun.handOver(Cast.READY, waited = true))
        assertEquals(Ends.LAND, FirstRun.handOver(Cast.NONE, waited = false))
        assertEquals(Ends.NOTHING, FirstRun.handOver(Cast.WAITS, waited = false))
        assertEquals(Ends.LAND, FirstRun.handOver(Cast.WAITS, waited = true))
    }

    /** Enter held down from the cue to the key's step: it begins the show, lands the key's step, and then does nothing. Never a fourth thing. */
```

- [ ] **Step 3: Write the failing test for the show, in `core/src/test/kotlin/io/github/kuscher/booklight/core/ShowTest.kt`.** One change: a test after the last one.

Find:

```kotlin
    }

    @Test fun itEndsOnTheLandingAndNothingComesAfter() {
```

Make it:

```kotlin
    }

    /**
     * "Show sums" switched off (a replay on an installation that has been lived in): the show shows nothing this
     * installation has switched off. The sum's beat is left out, and the flight is typed over the letter.
     */
    @Test fun whereSumsAreSwitchedOffTheShowLeavesTheSumOut() {
        val bare = Show.script(null, "emoji")
        assertTrue(bare.none { it.step is Step.Sum })
        // Nothing that is typed is a sum: every text is the flight's number or the keyword, a letter at a time.
        val texts = bare.mapNotNull { (it.step as? Step.Typed)?.text }
        assertEquals((1..5).map { "LH455".take(it) } + (1..5).map { "emoji".take(it) }, texts)
        assertTrue(texts.none { Calc.answer(it) != null })
        // The flight's first letter comes after the last Tab as the sum's first would have: 220 ms.
        assertEquals(bare.last { it.step == Step.Tab }.at + 220, bare.first { it.step is Step.Typed }.at)
        // What is left is in the same order, and ends on the landing, cued by the light as before.
        assertEquals(script.filter { it.step !is Step.Sum && !(it.step is Step.Typed && text(it).startsWith("1")) }.map { it.step }, bare.map { it.step })
        assertEquals(bare.map { it.at }.sorted(), bare.map { it.at })
        assertEquals(Step.Land, bare.last().step)
        assertEquals(bare.first { it.step == Step.Lap }.at + Show.LAP_TO_LAND_MS, bare.last().at)
        // A still of a beat that is not played is nothing to perform; the others are as they were.
        assertEquals(emptyList<Cue>(), Show.until(bare, Still.SUM))
        assertEquals(Step.Flight, Show.until(bare, Still.FLIGHT).last().step)
    }

    @Test fun itEndsOnTheLandingAndNothingComesAfter() {
```

- [ ] **Step 4: Run them and see them fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunTest' --tests 'io.github.kuscher.booklight.core.FirstRunPlayingTest' --tests 'io.github.kuscher.booklight.core.ShowTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'Cast'` and `No parameter with name 'sums' found` among its `e:` lines.

- [ ] **Step 5: The steps and the cue, in `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`.** Seven changes, in the file's order: the state's comment and its new field; `steps`; `Cast`; `pressed`; `handOver`.

Find:

```kotlin
     * [mark]: step 1 is over on this installation, however it ended.
```

Make it:

```kotlin
     * [mark]: step 1 is over on this installation, however it ended. [sums]: sums are answered
     * ("Show sums" is on); only read here, never changed.
```

Find:

```kotlin
        val mark: Boolean = false,
```

Make it:

```kotlin
        val mark: Boolean = false,
        val sums: Boolean = true,
```

Find:

```kotlin
     * The question is a step only while suggestions are off, or once it was answered in this run.
```

Make it:

```kotlin
     * The question is a step only while suggestions are off, or once it was answered in this run.
     * The sum's lesson is a step only while sums are answered, or once it is over in this run: an
     * installation that has been lived in may have them switched off when the run is asked for again,
     * and a lesson whose example finds nothing to copy could only be skipped.
```

Find:

```kotlin
        Run.NEW, Run.REPLAY -> Step.entries.filter { it != Step.ASK || !s.suggestions || over(s, Step.ASK) }
```

Make it:

```kotlin
        Run.NEW, Run.REPLAY -> Step.entries.filter { step ->
            when (step) {
                Step.ASK -> !s.suggestions || over(s, Step.ASK)
                Step.SUM -> s.sums || over(s, Step.SUM)
                else -> true
            }
        }
```

Find:

```kotlin
    enum class Ends { NOTHING, BEGIN, LAND, TYPE }

    /**
     * What [press] does while the piece is [playing]. A typed character always wins. The cue (Enter
```

Make it:

```kotlin
    enum class Ends { NOTHING, BEGIN, LAND, TYPE }

    /**
     * What the show has to show on this device. [WAITS]: its rows are still being worked out.
     * [READY]: they are there. [NONE]: this device has nothing to show (no app whose name begins with
     * a letter): there is no show.
     */
    enum class Cast { WAITS, READY, NONE }

    /**
     * What [press] does while the piece is [playing]. A typed character always wins. The cue (Enter
```

Find:

```kotlin
     * it begins the show. Every other press sets the key's screen down, once: while that happens,
     * nothing more does. Whatever the answer, a press that is not [Ends.TYPE] is used up here and
     * reaches nothing else: the key's screen comes with an answer armed, and no Enter pressed during
     * the piece may be that answer's.
     */
    fun pressed(playing: Playing, press: Press): Ends = when {
```

Make it:

```kotlin
     * it begins the show; where the device has nothing to show ([cast]), it sets the key's screen
     * down instead, and never does nothing. Every other press sets the key's screen down, once: while
     * that happens, nothing more does. Whatever the answer, a press that is not [Ends.TYPE] is used
     * up here and reaches nothing else: the key's screen comes with an answer armed, and no Enter
     * pressed during the piece may be that answer's.
     */
    fun pressed(playing: Playing, press: Press, cast: Cast = Cast.READY): Ends = when {
```

Find:

```kotlin
        playing == Playing.WELCOME && (press == Press.ENTER || press == Press.CUE) -> Ends.BEGIN
        else -> Ends.LAND
    }
```

Make it:

```kotlin
        playing == Playing.WELCOME && (press == Press.ENTER || press == Press.CUE) -> if (cast == Cast.NONE) Ends.LAND else Ends.BEGIN
        else -> Ends.LAND
    }

    /**
     * The welcome has stood and Booklight presses the handle itself, or the cue was given: what
     * happens now. With a show it begins. With none the key's screen is set down in that moment.
     * While this device's rows are still being worked out the welcome stands on; once it has
     * [waited] its second for them, the key's screen is set down without a show.
     */
    fun handOver(cast: Cast, waited: Boolean): Ends = when (cast) {
        Cast.READY -> Ends.BEGIN
        Cast.NONE -> Ends.LAND
        Cast.WAITS -> if (waited) Ends.LAND else Ends.NOTHING
    }
```

- [ ] **Step 6: The show without the sum, in `core/src/main/kotlin/io/github/kuscher/booklight/core/Show.kt`.** Three changes: the script's comment and signature; the sum's loop; the flight's first wait.

Find:

```kotlin
     * The show. [sum]: what is typed for the sum (lesson 4's own). [keyword]: the emoji grid's keyword.
     * [comma]: the user's language writes one and a half as 1,5. Each answer is what [Calc] says for
     * the text that stands in the field then, and comes with the letter that changes it.
     */
    fun script(sum: String, keyword: String, comma: Boolean = false): List<Cue> {
```

Make it:

```kotlin
     * The show. [sum]: what is typed for the sum (lesson 4's own); null where sums are switched off
     * on this installation: the show shows nothing that is switched off, and that beat is left out.
     * [keyword]: the emoji grid's keyword. [comma]: the user's language writes one and a half as 1,5.
     * Each answer is what [Calc] says for the text that stands in the field then, and comes with the
     * letter that changes it.
     */
    fun script(sum: String?, keyword: String, comma: Boolean = false): List<Cue> {
```

Find:

```kotlin
        for (n in 1..sum.length) {
```

Make it:

```kotlin
        if (sum != null) for (n in 1..sum.length) {
```

Find:

```kotlin
        // A flight, typed over the sum: its row lands with the last character of the number.
        val number = FLIGHT.number
        for (n in 1..number.length) cue(if (n == 1) TO_FLIGHT_MS else if (n == number.length) LANDS_MS else LETTER_MS, Step.Typed(number.take(n)))
```

Make it:

```kotlin
        // A flight, typed over the sum: its row lands with the last character of the number. (Without the sum's beat it is
        // typed over the letter, as long after the last Tab as the sum's first character would have come.)
        val number = FLIGHT.number
        val toFlight = if (sum != null) TO_FLIGHT_MS else TO_SUM_MS
        for (n in 1..number.length) cue(if (n == 1) toFlight else if (n == number.length) LANDS_MS else LETTER_MS, Step.Typed(number.take(n)))
```

- [ ] **Step 7: Run them and see them pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunTest' --tests 'io.github.kuscher.booklight.core.FirstRunPlayingTest' --tests 'io.github.kuscher.booklight.core.ShowTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Run every core test, and build the app**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT` (the app still calls `pressed` with two arguments and `script` with a text: both still compile).

- [ ] **Step 9: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/main/kotlin/io/github/kuscher/booklight/core/Show.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunTest.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunPlayingTest.kt core/src/test/kotlin/io/github/kuscher/booklight/core/ShowTest.kt
git commit -m "First run: asked for again where sums are switched off, the run and the show leave the sum out; a cue with nothing to show sets the key's step down (core, tested)"
```

### Task 2: `FirstRun`: "First steps" asks for the run again, the window's row, and the word that the steps wait

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunAgainTest.kt`

**Interfaces:**
- Consumes (in `object FirstRun`, part 1): `State`, `Run { NONE, NEW, UPDATE, REPLAY }`, `Screen` (`K1` to `K4`, `L2` to `L4`, `Q`, `C`), `Answer`, `SHOWN`, `MAX_OPENS`, `fun screen(s: State): Screen?`, `fun count(s: State): Pair<Int, Int>?`, `fun parked(s: State): Boolean`, `fun opened(s: State): State`, `fun resume(s: State): State`, `fun replay(s: State, overture: Boolean): State`, `fun again(s: State, overture: Boolean): State`, `fun answer(s: State, answer: Answer): State`, `fun overture(…)`; `State.sums` (Task 1); core `Effect.Internal(command: String)`.
- Produces, in `object FirstRun`:
  - `val AGAIN: Effect = Effect.Internal("first")`: what the "First steps" command carries in place of something to run.
  - `fun againHere(s: State): State`: `again(s, overture = false)`, and the panel it was asked in counted as the run's first opening.
  - `data class Offer(val goesOn: Boolean, val count: Pair<Int, Int>?)` and `fun offer(s: State): Offer`: what the window's row offers.
  - `fun later(before: State, after: State): Boolean`: the steps began to wait in the Booklight window in the change from `before` to `after`.
  - `again` keeps its signature; its comment says who asks with which `overture`. The file's header no longer speaks of a card.

Part 1 gave core `replay` and `again` and tested them; nothing called them. This task adds what the two callers need, so that neither works anything out for itself.

**The command** is typed in a panel that is already open. No new panel is made for it (Booklight never opens itself over another window, and a panel started from a panel would be exactly that), so no opening is counted by `OverlayActivity.onCreate`: `againHere` counts this one. And it never plays the opening piece: a piece begun in an open panel is not whole (the desk is not dimmed, the ordinary lap of light is already armed), and the user who typed is at work.

**The row** in the window writes `again(s, overture = true)` and starts a new plain panel, which plays the piece if this run has not shown it. What the row says is `offer`: "First steps", or "Go on with the first steps" with the counter of where the run stopped, also while it waits.

**The word.** A run that ends by "Not now" on the key, or that has stood in its three openings, waits in the window. `design.md` §5 gives the first case one placeholder ("First steps wait in Booklight’s window"); this plan gives it to both (decision 7), and only once: `later` is true for the one change of state in which the waiting began.

- [ ] **Step 1: Write the failing test `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunAgainTest.kt`**

```kotlin
package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Answer
import io.github.kuscher.booklight.core.FirstRun.Overture
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "First steps": the run asked for again, from the Booklight window's row or by its command in the panel; and where the steps wait until then. */
class FirstRunAgainTest {
    /** An installation that has been lived in: a key, suggestions on, the icon clicked, step 1 over long ago, sums switched off. */
    private val lived = State(icon = true, key = true, suggestions = true, mark = true, sums = false)
    /** A run left half-way: the key and the first lesson are over. */
    private val halfWay = State(Run.NEW, done = listOf("show", "key", "open"), opens = 2, key = true)

    private fun begins(s: State) = FirstRun.overture(s, motion = true, reader = false, plain = true, screenDp = 1200f)

    /** The window's row plays the whole of it, the opening piece first, at the speed that is set; it lands in "Your key works". */
    @Test fun theWindowsRowPlaysTheWholeOfItAgainThePieceFirst() {
        val s = FirstRun.again(State(key = true, mark = true), overture = true)
        assertEquals(Run.REPLAY, s.run)
        assertEquals(Overture.WELCOME, begins(FirstRun.opening(s, 1200f, plain = true)))
        assertFalse(FirstRun.slow(s, Overture.WELCOME))
        assertEquals(Screen.K4, FirstRun.screen(s))
        assertEquals(listOf(Answer.GO_ON, Answer.CHANGE_KEY), FirstRun.answers(s))
    }

    /** The typed command does not: the user is at work in the panel. The run starts on "Your key works", in the panel that is open. */
    @Test fun theTypedCommandStartsWithoutThePiece() {
        val s = FirstRun.again(State(key = true, mark = true), overture = false)
        assertEquals(Run.REPLAY, s.run)
        assertEquals(Overture.NONE, begins(s))
        assertEquals(Screen.K4, FirstRun.screen(s))
        assertEquals(0, FirstRun.armed(s))
        // Nor at the next opening, if the panel is closed on it: the piece was not asked for.
        assertEquals(Overture.NONE, begins(FirstRun.opening(s, 1200f, plain = true)))
    }

    /** Asked for by its command, the run stands in the panel that is open: that panel is its first opening, and it has two more. */
    @Test fun thePanelTheCommandIsTypedInIsTheRunsOpening() {
        val here = FirstRun.againHere(State(key = true, mark = true))
        assertEquals(FirstRun.again(State(key = true, mark = true), overture = false).copy(opens = 1), here)
        assertEquals(Screen.K4, FirstRun.screen(here))
        var s = here
        repeat(FirstRun.MAX_OPENS - 1) { s = FirstRun.opening(s, 1200f, plain = true); assertEquals(Screen.K4, FirstRun.screen(s)) }
        assertNull(FirstRun.screen(FirstRun.opening(s, 1200f, plain = true)))
        // An unfinished run asked for by the command: where it stopped, counted anew from this panel.
        assertEquals(halfWay.copy(opens = 1), FirstRun.againHere(halfWay.copy(opens = FirstRun.MAX_OPENS + 1)))
    }

    /** Without a key the run asked for again begins where a new one does: on the key's first screen. */
    @Test fun withoutAKeyItBeginsOnTheKeysFirstScreen() {
        for (overture in listOf(true, false)) {
            val s = FirstRun.again(State(mark = true), overture)
            assertEquals(Screen.K1, FirstRun.screen(s))
            assertEquals(1 to 5, FirstRun.count(s))
        }
    }

    /** A run that is not finished goes on where it stopped, from either place, and the piece does not come with it. */
    @Test fun anUnfinishedRunGoesOnWhereItStopped() {
        var waiting = halfWay
        repeat(FirstRun.MAX_OPENS) { waiting = FirstRun.opened(waiting) }
        assertNull(FirstRun.screen(waiting))
        for (overture in listOf(true, false)) for (s in listOf(halfWay, waiting)) {
            val on = FirstRun.again(s, overture)
            assertEquals(Run.NEW, on.run)
            assertEquals(Screen.L3, FirstRun.screen(on))
            assertEquals(halfWay.done, on.done)
            assertEquals(0, on.opens)
            assertEquals(Overture.NONE, begins(FirstRun.opening(on, 1200f, plain = true)))
        }
        // The run of an installation that was there before, left on its key: the key's step again, and nothing more.
        assertEquals(Screen.K1, FirstRun.screen(FirstRun.again(State(Run.UPDATE, opens = FirstRun.MAX_OPENS + 1), overture = true)))
    }

    /** Asked for again, it changes no switch and forgets no key, whichever way it is asked and wherever it stood. */
    @Test fun askedAgainItTouchesNoSwitchAndNoKey() {
        for (overture in listOf(true, false)) for (s in listOf(lived, lived.copy(run = Run.NEW, done = listOf("show", "key")), State(), halfWay)) {
            val on = FirstRun.again(s, overture)
            assertEquals("$s", listOf(s.key, s.suggestions, s.sums, s.icon, s.mark), listOf(on.key, on.suggestions, on.sums, on.icon, on.mark))
        }
        // With suggestions on the question is not asked again, and with sums off there is no sum to practise.
        assertEquals(listOf(FirstRun.Step.KEY, FirstRun.Step.OPEN, FirstRun.Step.SEARCH, FirstRun.Step.CHOICES), FirstRun.steps(FirstRun.again(lived, overture = true)))
    }

    /** What the window's row says: "First steps"; while a run is unfinished, "Go on with the first steps" and where it stopped. */
    @Test fun theRowSaysGoOnWhileARunIsUnfinished() {
        assertEquals(FirstRun.Offer(goesOn = false, count = null), FirstRun.offer(State()))
        assertEquals(FirstRun.Offer(goesOn = false, count = null), FirstRun.offer(lived))
        assertEquals(FirstRun.Offer(goesOn = true, count = 3 to 5), FirstRun.offer(halfWay))
        // A run that waits says where it stopped, not nothing.
        assertEquals(FirstRun.Offer(goesOn = true, count = 3 to 5), FirstRun.offer(halfWay.copy(opens = FirstRun.MAX_OPENS + 1)))
        // The run of one step has no counter, and the choices have none.
        assertEquals(FirstRun.Offer(goesOn = true, count = null), FirstRun.offer(State(Run.UPDATE)))
        assertEquals(FirstRun.Offer(goesOn = true, count = null), FirstRun.offer(State(Run.NEW, done = listOf("key", "open", "search", "sum", "ask"), key = true)))
    }

    /** "Not now" on the key's step ends the run: the steps wait in the Booklight window, and the bare field says so for that opening. */
    @Test fun notNowOnTheKeyLeavesAWordBehind() {
        for (helper in 0..2) for (run in listOf(Run.NEW, Run.UPDATE)) {
            val before = State(run, helper = helper)
            assertTrue("$run $helper", FirstRun.later(before, FirstRun.answer(before, Answer.NOT_NOW)))
        }
        // A replay that asks for another key, left by "Not now": the same.
        val change = State(Run.REPLAY, done = listOf(FirstRun.SHOWN, FirstRun.CHANGE), key = true)
        assertTrue(FirstRun.later(change, FirstRun.answer(change, Answer.NOT_NOW)))
    }

    /** Nothing else leaves it: a run that goes on, a run that is finished, an answer that changes nothing. */
    @Test fun noOtherAnswerLeavesThatWord() {
        val k1 = State(Run.NEW)
        assertFalse(FirstRun.later(k1, FirstRun.answer(k1, Answer.OPEN_HELPER)))
        assertFalse(FirstRun.later(k1, FirstRun.helperCame(k1)))
        assertFalse(FirstRun.later(k1, FirstRun.keyLanded(k1)))
        // "Go on" in the run of one step ends that run as finished: there is nothing left to wait.
        val k4 = State(Run.UPDATE, key = true)
        assertEquals(Run.NONE, FirstRun.answer(k4, Answer.GO_ON).run)
        assertFalse(FirstRun.later(k4, FirstRun.answer(k4, Answer.GO_ON)))
        // The question's "Not now" is an answer like another, and the choices' end is the run's own end.
        val q = State(Run.NEW, done = listOf("key", "open", "search", "sum"), key = true)
        assertFalse(FirstRun.later(q, FirstRun.answer(q, Answer.NOT_NOW)))
        val c = q.copy(done = q.done + "ask")
        assertEquals(Run.NONE, FirstRun.answer(c, Answer.DONE).run)
        assertFalse(FirstRun.later(c, FirstRun.answer(c, Answer.DONE)))
        // Nobody without a run is told anything.
        assertFalse(FirstRun.later(State(), State()))
        assertFalse(FirstRun.later(lived, FirstRun.opening(lived, 1200f, plain = true)))
    }

    /** An unfinished run stands in three openings. In the fourth nothing stands, and that opening alone says where the steps wait. */
    @Test fun aRunThatUsedUpItsOpeningsLeavesTheSameWordOnce() {
        var s = State(Run.NEW, done = listOf("show", "key", "open"), key = true)
        val said = ArrayList<Boolean>()
        repeat(6) {
            val after = FirstRun.opening(s, 1200f, plain = true)
            said += FirstRun.later(s, after)
            s = after
        }
        assertEquals(listOf(false, false, false, true, false, false), said)
        // A panel that is no opening of the run (it carries another app's text) says nothing, and uses nothing up.
        val third = State(Run.NEW, done = listOf("show", "key"), opens = FirstRun.MAX_OPENS, key = true)
        assertFalse(FirstRun.later(third, FirstRun.opening(third, 1200f, plain = false)))
    }

    /** The command carries nothing the executor knows, and nothing a lesson would take for practice: the panel catches it first. */
    @Test fun theCommandCarriesNothingToRun() {
        assertEquals(Effect.Internal("first"), FirstRun.AGAIN)
        assertFalse(FirstRun.AGAIN == Show.NOTHING)
        val lesson = State(Run.NEW, done = listOf("key"), key = true)
        assertEquals(FirstRun.Enter.AS_ALWAYS, FirstRun.enters(lesson, null, FirstRun.AGAIN, searchable = true))
    }
}
```

- [ ] **Step 2: Run it and see it fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunAgainTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'againHere'`, `Unresolved reference 'offer'` and `Unresolved reference 'later'` among its `e:` lines.

- [ ] **Step 3: The run again, in `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`.** Three changes: the header; `again`'s comment, with `AGAIN`, `againHere`, `Offer`, `offer` and `later` after it.

Find:

```kotlin
 * two choices are offered, under the empty field where a card stood. Everything that is decided
 * about it is decided here: which screen stands at the next opening, what an answer changes, who
 * gets a run at all. The app keeps a [State] in its settings and draws what [screen] says; nothing
 * here knows Android.
```

Make it:

```kotlin
 * two choices are offered, on a stage under the empty field. Everything that is decided about it
 * is decided here: which screen stands at the next opening, what an answer changes, who gets a
 * run at all. The app keeps a [State] in its settings and draws what [screen] says; nothing here
 * knows Android.
```

Find:

```kotlin
 *    window, with what was done kept, until it is asked for ([resume]).
```

Make it:

```kotlin
 *    window, with what was done kept, until it is asked for ([resume]).
 *  - **"First steps"** asks for it again ([again]): the Booklight window's row, with the opening
 *    piece first, and the command typed in the panel ([AGAIN]), without. Where the steps wait is
 *    said once, in the opening they began to wait in ([later]).
```

Find:

```kotlin
    /** "First steps" was asked for: an unfinished run goes on where it stopped, a finished one is played again. */
    fun again(s: State, overture: Boolean): State = if (s.run != Run.NONE) resume(s) else replay(s, overture)
```

Make it:

```kotlin
    /**
     * "First steps" was asked for: an unfinished run goes on where it stopped, a finished one is
     * played again. [overture]: asked for by the Booklight window's row, which plays the whole of it,
     * the opening piece first; false for the command typed in the panel, where the user is at work
     * and the run begins in the panel that is open.
     */
    fun again(s: State, overture: Boolean): State = if (s.run != Run.NONE) resume(s) else replay(s, overture)

    /** What the "First steps" command carries in place of something to run: the panel catches it before anything is run, and asks [againHere]. */
    val AGAIN: Effect = Effect.Internal("first")

    /**
     * "First steps" was asked for by its command, in a panel that is open: [again] without the
     * opening piece, and the run stands in that very panel, which is therefore the first of its
     * [MAX_OPENS] openings (no new panel is made that could count itself).
     */
    fun againHere(s: State): State = opened(again(s, overture = false))

    /**
     * What the Booklight window's row for the first steps offers. [goesOn]: a run is unfinished, and
     * the row goes on with it where it stopped; else it plays the whole of it again. [count]: where
     * an unfinished run stopped, as its counter says it ("3 of 5"), also while it waits; null where
     * no counter stands.
     */
    data class Offer(val goesOn: Boolean, val count: Pair<Int, Int>?)

    fun offer(s: State): Offer = if (s.run == Run.NONE) Offer(goesOn = false, count = null) else Offer(goesOn = true, count = count(resume(s)))

    /**
     * The steps have begun to wait in the Booklight window in the change from [before] to [after],
     * and the bare field says so for the rest of that opening: "Not now" was answered on the key's
     * step, which ends the run; or an unfinished run has stood in its [MAX_OPENS] openings and this
     * one is the first in which nothing stands. Once: a run that waits says nothing at the openings
     * after that, and a run that was finished has nothing waiting.
     */
    fun later(before: State, after: State): Boolean {
        val asked = screen(before).let { it == Screen.K1 || it == Screen.K2 || it == Screen.K3 }
        return (asked && after.run == Run.NONE) || (before.run != Run.NONE && !parked(before) && parked(after))
    }
```

- [ ] **Step 4: Run it and see it pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunAgainTest' --console=plain
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
git add core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunAgainTest.kt
git commit -m "First run: \"First steps\" asks for the run again, the window's row says where it stopped, and the steps that wait say so once (core, tested)"
```

### Task 3: `FirstRun`: when a screen is in view, and a screen come back to

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunStageTest.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunOpeningsTest.kt`

**Interfaces:**
- Consumes (in `object FirstRun`): `State`, `fun screen(s: State): Screen?`, `fun opening(s: State, screenDp: Float, plain: Boolean): State`, `fun armed(s: State): Int`, `fun tabbed(s: State, armed: Int, back: Boolean, arrived: Boolean): Int`, `fun entered(s: State, armed: Int, arrived: Boolean): Answer?` (part 3); `fun againHere(s: State): State` (Task 2).
- Produces, in `object FirstRun`:
  - `const val SEEN_MS = 350L`: how long a screen must have been in view before it takes a key or a click.
  - `fun inView(press: Long, opened: Long, came: Long, lead: Long): Boolean`: `press - (maxOf(opened, came) + maxOf(lead, 0L)) >= SEEN_MS`. All four are ms of one clock; `lead` is how long after it came the screen's answers begin to show.
  - `fun comesBack(s: State): Boolean`: a screen stands, and this run has stood in an opening before this one (`s.opens > 1`).

Until now a screen stood whole in the frame it came, and took a key 350 ms later (`OverlayModel.seen`, with the constant in the app). From Task 10 on a screen is set down part by part, and its answers come last. If the 350 ms still ran from the screen's coming, someone could answer a question whose answers are not drawn yet. So the rule moves to core with one more term: **a screen is in view once the answer Enter would run has begun to show**, and takes a key `SEEN_MS` after that. Nothing can make it count sooner than a screen that stands whole (a negative lead is nothing).

`comesBack` says which of two arrivals a screen has when a panel opens on it: set down part by part in the run's first opening, or as rows arrive in a later one, where nothing is written a second time (`motion.md` §3, T1: "the second time is quiet").

- [ ] **Step 1: Write the failing tests for the view, in `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunStageTest.kt`.** One change: two tests after the last one.

Find:

```kotlin
    }

    /** Through a whole run, whatever else is pressed or left undone, suggestions are on only after Agree. */
```

Make it:

```kotlin
    }

    /**
     * A screen takes a key only once it has been in view for a moment, and a screen whose parts are set down one after
     * the other is in view only once its answers have begun to show. A press made while they are still coming answers
     * nothing, however long ago the screen "came".
     */
    @Test fun aScreenThatIsStillArrivingIsNotInViewSooner() {
        val opened = 1_000L
        val came = 5_000L
        // Whole at once: 350 ms after it came.
        assertFalse(FirstRun.inView(press = came + 349, opened = opened, came = came, lead = 0))
        assertTrue(FirstRun.inView(press = came + 350, opened = opened, came = came, lead = 0))
        // Its answers begin to show 383 ms after it: 350 ms after them, not after the screen.
        assertFalse(FirstRun.inView(press = came + 350, opened = opened, came = came, lead = 383))
        assertFalse(FirstRun.inView(press = came + 383 + 349, opened = opened, came = came, lead = 383))
        assertTrue(FirstRun.inView(press = came + 383 + 350, opened = opened, came = came, lead = 383))
        // A screen the panel opens on is in view from the glass's opening, not from the moment the panel was made.
        assertFalse(FirstRun.inView(press = 1_000 + 120 + 349, opened = 1_000, came = 400, lead = 120))
        assertTrue(FirstRun.inView(press = 1_000 + 120 + 350, opened = 1_000, came = 400, lead = 120))
        // Nothing makes a screen count sooner than one that stands whole.
        assertFalse(FirstRun.inView(press = came + 349, opened = opened, came = came, lead = -500))
        assertEquals(350L, FirstRun.SEEN_MS)
    }

    /**
     * The keys that skip (Tab, then Enter), pressed while a screen is being set down, choose nothing and answer
     * nothing: on the question they do not even reach "Not now", and on the key's screen Enter is not the armed
     * answer's until that answer has been in view.
     */
    @Test fun keysPressedWhileAScreenIsSetDownDoNothing() {
        val came = 10_000L
        val lead = 142L       // the question's answers are the last of its parts
        for (press in listOf(came, came + lead, came + lead + 349)) {
            val seen = FirstRun.inView(press, opened = 0, came = came, lead = lead)
            val armed = FirstRun.tabbed(q, FirstRun.armed(q), back = false, arrived = seen)
            assertEquals(-1, armed)
            assertNull(FirstRun.entered(q, armed, arrived = seen))
        }
        val seen = FirstRun.inView(came + lead + 350, opened = 0, came = came, lead = lead)
        assertEquals(Answer.NOT_NOW, FirstRun.entered(q, FirstRun.tabbed(q, -1, back = false, arrived = seen), arrived = seen))
        assertNull(FirstRun.entered(k1, FirstRun.armed(k1), arrived = FirstRun.inView(came + 383 + 100, opened = 0, came = came, lead = 383)))
        assertEquals(Answer.OPEN_HELPER, FirstRun.entered(k1, FirstRun.armed(k1), arrived = FirstRun.inView(came + 383 + 350, opened = 0, came = came, lead = 383)))
    }

    /** Through a whole run, whatever else is pressed or left undone, suggestions are on only after Agree. */
```

- [ ] **Step 2: Write the failing test for a screen come back to, in `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunOpeningsTest.kt`.** One change: a test after the last one.

Find:

```kotlin
    }

    @Test fun theUsersOwnKeyBringsARunBackThatWaitedForIt() {
```

Make it:

```kotlin
    }

    /** A screen the run has stood on in an earlier opening comes back: its parts arrive as rows do, and nothing of it is written again. */
    @Test fun inALaterOpeningAScreenComesBack() {
        var s = FirstRun.opening(halfWay, 1200f, plain = true)
        assertFalse(FirstRun.comesBack(s))
        s = FirstRun.opening(s, 1200f, plain = true)
        assertTrue(FirstRun.comesBack(s))
        // Asked for again, the run's next opening is its first once more; so is the one a first key makes.
        assertFalse(FirstRun.comesBack(FirstRun.opening(FirstRun.again(s, overture = true), 1200f, plain = true)))
        val waiting = State(Run.NEW, helper = 1, opens = FirstRun.MAX_OPENS)
        assertFalse(FirstRun.comesBack(FirstRun.opening(FirstRun.keyLanded(waiting), 1200f, plain = true)))
        // The panel the command is typed in is the run's first opening: only the next one comes back.
        val here = FirstRun.againHere(State(key = true))
        assertFalse(FirstRun.comesBack(here))
        assertTrue(FirstRun.comesBack(FirstRun.opening(here, 1200f, plain = true)))
        // Nothing comes back where nothing stands.
        assertFalse(FirstRun.comesBack(State()))
        assertFalse(FirstRun.comesBack(State(Run.NEW, opens = FirstRun.MAX_OPENS + 1)))
    }

    @Test fun theUsersOwnKeyBringsARunBackThatWaitedForIt() {
```

- [ ] **Step 3: Run them and see them fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunStageTest' --tests 'io.github.kuscher.booklight.core.FirstRunOpeningsTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'inView'`, `Unresolved reference 'SEEN_MS'` and `Unresolved reference 'comesBack'` among its `e:` lines.

- [ ] **Step 4: The rule, in `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`.** One change: a section above `caption`.

Find:

```kotlin
    }

    /** What stands under the caps: where the suggested key is and that any keys will do; "your keys"; the other keys to try; nothing. */
```

Make it:

```kotlin
    }

    // ---- in view

    /** How long a screen of first run must have been in view before it takes a key or a click. */
    const val SEEN_MS = 350L

    /**
     * Whether the screen that stands takes a key pressed at [press] (a clock's ms). [opened]: when
     * the glass had opened far enough for the stage to be seen. [came]: when the screen came.
     * [lead]: how long after that its answers begin to show, where its parts are set down one after
     * the other; 0 where it stands whole at once. A screen is in view once its answers are, and takes
     * a key [SEEN_MS] after that: the Enter that skipped a lesson, pressed again at once, must not
     * answer what took the lesson's place, and nothing is answered that has not been set down yet.
     */
    fun inView(press: Long, opened: Long, came: Long, lead: Long): Boolean = press - (maxOf(opened, came) + maxOf(lead, 0L)) >= SEEN_MS

    /**
     * The screen that stands is one come back to: this run has stood in an opening before this one.
     * Its parts then arrive as rows do, and nothing of it is written again; in a run's first opening
     * a screen is set down part by part. (Counted for the run, not for each screen: where the run
     * went on to another screen in the last opening and was left at once, that screen comes as one
     * come back to.)
     */
    fun comesBack(s: State): Boolean = screen(s) != null && s.opens > 1

    /** What stands under the caps: where the suggested key is and that any keys will do; "your keys"; the other keys to try; nothing. */
```

- [ ] **Step 5: Run them and see them pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunStageTest' --tests 'io.github.kuscher.booklight.core.FirstRunOpeningsTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Run every core test, and build the app**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`.

- [ ] **Step 7: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunStageTest.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunOpeningsTest.kt
git commit -m "First run: a screen is in view once its answers have begun to show, and one come back to is not set down again (core, tested)"
```

### Task 4: `SetDown`: when each part of a stage comes, for every way a screen can come, as data

**Files:**
- Create: `core/src/main/kotlin/io/github/kuscher/booklight/core/SetDown.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/SetDownTest.kt`
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`

**Interfaces:**
- Consumes (in `object FirstRun`): `Screen` (`K1` to `K4`, `L2` to `L4`, `Q`, `C`), `enum class Key { QUICK_INSERT, M }`, `enum class Caption`, `fun caption(s: State, suggested: Key): Caption`, `fun screen(s: State): Screen?`.
- Produces:
  - In `object FirstRun`: `fun caption(on: Screen?, suggested: Key): Caption`, the caption of a screen by itself; `caption(s, suggested)` answers with it for `screen(s)`.
  - A new `object SetDown` (core, no Android):
    - `enum class Comes { WHOLE, SET_DOWN, BACK, TURN, AFTER_LIST, LANDS }`: how a screen comes onto the glass.
    - `data class Pace(val part: Int, val beat: Int, val letter: Int, val band: Int, val turn: Int, val afterList: Int, val press: Int, val askText: Int, val askNote: Int, val askAnswers: Int, val landsDisc: Int, val landsWords: Int, val landsCounter: Int, val landsArmed: Int, val landsBand: Int, val rest: Int)`: the times, in ms, which the app hands in.
    - `const val THERE = -100_000`: the mark of a part that does not come: it is there.
    - `data class Marks(val disc: Int, val words: Int, val counter: Int, val pieces: List<Int>, val written: Boolean, val caption: Int, val answers: Int, val armed: Int, val rises: Boolean, val end: Int)` with `val lead: Int` (`maxOf(0, armed)`): when each part comes, in ms after the screen came.
    - `val WHOLE: Marks`: nothing comes, everything is there.
    - `fun piece(m: Marks, k: Int): Int`, `fun letter(m: Marks, pace: Pace, k: Int, i: Int): Int`.
    - `fun marks(comes: Comes, pace: Pace, letters: List<Int>, ask: Boolean, caption: Boolean, bandAfter: Int = pace.band, bandChanges: Boolean = true, captionChanges: Boolean = true, answersChange: Boolean = true): Marks`.
    - `fun sameBand(a: FirstRun.Screen?, b: FirstRun.Screen): Boolean`, `fun sameCaption(a: FirstRun.Screen?, b: FirstRun.Screen, suggested: FirstRun.Key): Boolean`.

`motion.md` §3 sets a screen of first run down in parts: the seat's disc, words and counter 22 ms apart; then the band, whose caps come up a beat apart and whose typed words are written a letter at a time; the caption; the answers last. That is one way a screen comes. There are five more: back in a later opening (as rows, nothing written twice), in another screen's place (what the two share does not move), after a list that gave way, at the landing of the opening piece (the armed answer first, inside the highlight that travels to it), and whole at once. Six ways times six kinds of screen is where a schedule goes wrong, and the app has no tests. So the schedule is data, here, with its tests; the app draws by it and counts keys by it (`Marks.lead`, Task 3's `inView`).

The times are not core's: all motion's times live in `overlay/Motion.kt` (Task 5 makes the `Pace`). The tests use the paper's own figures, so that a mark here can be laid beside a line of `motion.md`.

Three things the tests pin that an eye would miss: the answers never come before the last piece of the band they answer; `lead` is the armed answer's mark and never negative; and a part whose mark is `THERE` is drawn as it is, with no motion at all.

- [ ] **Step 1: Write the failing test `core/src/test/kotlin/io/github/kuscher/booklight/core/SetDownTest.kt`**

```kotlin
package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.SetDown.Comes
import io.github.kuscher.booklight.core.SetDown.THERE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When each part of first run's stage comes (docs/design/first-run/motion.md §3): the paper's own tables, with the paper's own times. */
class SetDownTest {
    /** The paper's times: a part 22 ms after the one before, a beat of 66, a letter 40. */
    private val pace = SetDown.Pace(
        part = 22, beat = 66, letter = 40, band = 120, turn = 120, afterList = 60, press = 80,
        askText = 66, askNote = 120, askAnswers = 142,
        landsDisc = 80, landsWords = 150, landsCounter = 190, landsArmed = 180, landsBand = 260, rest = 170,
    )
    /** The band's pieces, as how many letters each typed part has (0: a cap or a sign): the suggested keys, "your key", and the three recipes. */
    private val caps = listOf(0, 0, 0)
    private val yours = listOf(0)
    private val l2 = listOf(4, 0)
    private val l3 = listOf(4, 0, 0, 4, 0)
    private val l4 = listOf(9, 0)

    private fun marks(comes: Comes, letters: List<Int> = caps, ask: Boolean = false, caption: Boolean = true, bandAfter: Int = pace.band, bandChanges: Boolean = true, captionChanges: Boolean = true, answersChange: Boolean = true) =
        SetDown.marks(comes, pace, letters, ask, caption, bandAfter, bandChanges, captionChanges, answersChange)

    /** T1: the key's step at the gate of a first opening at Medium, where the band begins 163 ms after the gate. */
    @Test fun theKeysStepIsSetDownAsThePaperHasIt() {
        val m = marks(Comes.SET_DOWN, caps, bandAfter = 163)
        assertEquals(listOf(0, 22, 44), listOf(m.disc, m.words, m.counter))
        // `Action`, "+", `Quick Insert`: B, B + 66, B + 132. The caption at B + 198, the answers at B + 220, at rest B + 390.
        assertEquals(listOf(163, 229, 295), m.pieces)
        assertEquals(163 + 198, m.caption)
        assertEquals(163 + 220, m.answers)
        assertEquals(163 + 390, m.end)
        // The armed answer comes with the others, and that is where the screen's keys begin to count from.
        assertEquals(m.answers, m.armed)
        assertEquals(383, m.lead)
        assertTrue(m.rises)
        // At Fast, and with the opening switched off, the band is 120 ms after the gate.
        assertEquals(listOf(120, 186, 252), marks(Comes.SET_DOWN, caps).pieces)
    }

    /** T5: a recipe is written at one pace: a letter 40 ms after the one before, a cap or a new typed part a beat after the piece before it. */
    @Test fun aLessonsRecipeIsWrittenAsThePaperHasIt() {
        fun written(letters: List<Int>) = marks(Comes.SET_DOWN, letters, bandAfter = 0)
        // `yout` `⏎`: letters 0 to 120, the cap at 186; the caption 252, Skip 274; at rest 445 (444 here).
        written(l2).let { assertEquals(listOf(0, 186), it.pieces); assertEquals(252, it.caption); assertEquals(274, it.answers); assertEquals(444, it.end) }
        // `yout` `Tab` `⏎` `lofi` `⏎`: 0 to 120, 186, 252, 318 to 438, 504; 570 and 592; at rest 760 (762 here).
        written(l3).let { assertEquals(listOf(0, 186, 252, 318, 504), it.pieces); assertEquals(570, it.caption); assertEquals(592, it.answers); assertEquals(762, it.end) }
        // `150 + 20%` `⏎`: 0 to 320, 386; 452 and 474; at rest 645 (644 here).
        written(l4).let { assertEquals(listOf(0, 386), it.pieces); assertEquals(452, it.caption); assertEquals(474, it.answers); assertEquals(644, it.end) }
        // A typed part's letters follow its mark 40 ms apart.
        assertEquals(listOf(318, 358, 398, 438), (0 until 4).map { SetDown.letter(written(l3), pace, 3, it) })
        // A lesson whose example is not worked out yet has no recipe: its caption and Skip do not wait for one.
        written(emptyList()).let { assertEquals(emptyList<Int>(), it.pieces); assertEquals(0, it.caption); assertEquals(22, it.answers) }
    }

    /** "Your key works" in a panel the keys have just opened: the key comes up at B, "Go on" at B + 66. There is no caption. */
    @Test fun aScreenWithoutACaptionHasItsAnswersABeatAfterTheBand() {
        val m = marks(Comes.SET_DOWN, yours, caption = false, bandAfter = 163)
        assertEquals(listOf(163), m.pieces)
        assertEquals(THERE, m.caption)
        assertEquals(163 + 66, m.answers)
    }

    /** A screen come back to is not set down again at that pace: its parts arrive as rows do, 22 ms apart, and nothing is written twice. */
    @Test fun aScreenComeBackToArrivesAsRowsDo() {
        for (letters in listOf(caps, yours, l2, l3, l4)) {
            val m = marks(Comes.BACK, letters, bandAfter = 163)
            assertEquals(listOf(0, 22, 44), listOf(m.disc, m.words, m.counter))
            assertEquals(List(letters.size) { 66 }, m.pieces)
            assertEquals(88, m.caption)
            assertEquals(110, m.answers)
            // Its typed parts stand whole: every letter with its piece.
            for (k in letters.indices) for (i in 0 until letters[k]) assertEquals(66, SetDown.letter(m, pace, k, i))
        }
    }

    /** T5, in the same panel: the seat stays and its words roll; the new band is written from 120 ms; what stays does not move. */
    @Test fun aScreenThatTakesAnothersPlaceMovesOnlyWhatChanges() {
        // "Go on" on "Your key works": lesson 2's recipe is written from 120, then its caption, then Skip in place of "Go on".
        val goOn = marks(Comes.TURN, l2)
        assertEquals(listOf(THERE, THERE, THERE), listOf(goOn.disc, goOn.words, goOn.counter))
        assertEquals(listOf(120, 306), goOn.pieces)
        assertEquals(372, goOn.caption)
        assertEquals(394, goOn.answers)
        // Skip on a lesson: the next lesson's Skip is the same answer, and its caption says the same: neither moves.
        val skip = marks(Comes.TURN, l3, captionChanges = false, answersChange = false)
        assertEquals(THERE, skip.answers)
        assertEquals(THERE, skip.caption)
        assertEquals(0, skip.lead)
        assertEquals(listOf(120, 306, 372, 438, 624), skip.pieces)
        // From lesson 3 to the sum's lesson the caption says something else: it comes once the recipe is written.
        assertEquals(120 + 386 + 66, marks(Comes.TURN, l4, answersChange = false).caption)
        // The key lands with its screen in view: "your key" stays where it is, and "Go on" comes once the old answers have faded.
        val lands = marks(Comes.TURN, yours, caption = false, bandChanges = false)
        assertEquals(listOf(THERE), lands.pieces)
        assertEquals(80, lands.answers)
        assertEquals(80, lands.lead)
        // Back from the system's dialog: the caps give way to "your key", the caption changes after it, the two answers stay.
        val back = marks(Comes.TURN, yours, answersChange = false)
        assertEquals(listOf(120), back.pieces)
        assertEquals(186, back.caption)
        assertEquals(THERE, back.answers)
    }

    /** T8, and a lesson after a lesson's Enter: a stage that follows a list is set down 60 ms after the list gave way. */
    @Test fun afterAListTheStageWaitsForItsRowsToFade() {
        val m = marks(Comes.AFTER_LIST, l3)
        assertEquals(listOf(60, 82, 104), listOf(m.disc, m.words, m.counter))
        assertEquals(126, m.pieces.first())
        assertEquals(marks(Comes.SET_DOWN, l3, bandAfter = 66).pieces.map { it + 60 }, m.pieces)
        assertEquals(marks(Comes.SET_DOWN, l3, bandAfter = 66).answers + 60, m.answers)
    }

    /** The question's parts in reading order (T8: 580, 602, 624, 646, 700 and 722 for a list that gave way at 520). */
    @Test fun theQuestionComesInReadingOrder() {
        val after = marks(Comes.AFTER_LIST, emptyList(), ask = true, caption = false)
        assertEquals(listOf(580, 602, 624, 646, 700, 722), listOf(after.disc, after.words, after.counter, after.pieces[0], after.pieces[1], after.answers).map { it + 520 })
        assertEquals(722 - 520 + 170, after.end)
        // Come back to at a gate: the same offsets from the gate. Nothing of it is armed: its keys count from its answers.
        for (comes in listOf(Comes.SET_DOWN, Comes.BACK)) {
            val m = marks(comes, emptyList(), ask = true, caption = false, bandAfter = 163)
            assertEquals(listOf(0, 22, 44, 66, 120, 142), listOf(m.disc, m.words, m.counter, m.pieces[0], m.pieces[1], m.answers))
            assertEquals(142, m.lead)
        }
        // Where it takes a lesson's place (Skip): its text once the recipe has faded, then as above.
        val turn = marks(Comes.TURN, emptyList(), ask = true, caption = false)
        assertEquals(listOf(THERE, 120, 174, 196), listOf(turn.disc, turn.pieces[0], turn.pieces[1], turn.answers))
        assertEquals(196, turn.lead)
    }

    /** B5: the opening piece lands. The highlight travels into the armed answer, whose words fade in inside it at T + 180; the rest follows. */
    @Test fun atALandingTheArmedAnswerComesInsideTheHighlight() {
        val m = marks(Comes.LANDS, caps)
        assertEquals(listOf(80, 150, 190), listOf(m.disc, m.words, m.counter))
        assertEquals(180, m.armed)
        assertEquals(listOf(260, 326, 392), m.pieces)
        assertEquals(458, m.caption)
        assertEquals(480, m.answers)
        assertEquals(180, m.lead)
        // The answers do not rise there: the highlight is on its way to where they stand.
        assertFalse(m.rises)
        // A replay lands in "Your key works": the key at 260, "Change the key" a beat later.
        val again = marks(Comes.LANDS, yours, caption = false)
        assertEquals(listOf(260), again.pieces)
        assertEquals(326, again.answers)
        assertEquals(180, again.armed)
    }

    /** A screen that stands whole (the field emptied again, animations off): nothing comes, everything is there. */
    @Test fun aScreenThatStandsWholeHasNothingToCome() {
        for (letters in listOf(caps, l3, emptyList())) for (ask in listOf(true, false)) {
            val m = marks(Comes.WHOLE, letters, ask = ask)
            assertEquals(SetDown.WHOLE, m)
            assertEquals(0, m.end)
            assertEquals(0, m.lead)
            assertEquals(THERE, SetDown.piece(m, 3))
        }
    }

    /**
     * Whatever comes, in every way a screen can come: the answers are the last of its parts to begin (but for the
     * armed one's words at a landing), nothing begins after the end, and the lead is when the answer Enter would run
     * begins to show, never before the screen came.
     */
    @Test fun theAnswersComeLastAndTheLeadIsTheArmedAnswers() {
        for (comes in Comes.entries) for (letters in listOf(caps, yours, l2, l3, l4, emptyList())) for (ask in listOf(false, true))
            for (caption in listOf(true, false)) for (band in listOf(true, false)) for (answers in listOf(true, false)) for (after in listOf(120, 163, 324)) {
                val m = marks(comes, letters, ask, caption, after, band, captionChanges = band, answersChange = answers)
                val name = "$comes $letters ask=$ask caption=$caption band=$band answers=$answers"
                val parts = listOf(m.disc, m.words, m.counter, m.caption) + m.pieces
                if (m.answers != THERE) assertTrue(name, parts.all { it <= m.answers })
                assertTrue(name, (parts + m.answers + m.armed).all { it == THERE || it in 0..m.end })
                assertEquals(name, maxOf(0, m.armed), m.lead)
                if (comes != Comes.LANDS) assertEquals(name, m.answers, m.armed)
                // A piece asked for that the band does not have is the last one's, never a crash.
                assertEquals(name, m.pieces.lastOrNull() ?: THERE, SetDown.piece(m, 99))
            }
    }

    /** Which screens share a band: the key's last three are all "your key"; every other screen has its own. */
    @Test fun whichScreensShareABand() {
        for (a in Screen.entries) for (b in Screen.entries) {
            val yours = setOf(Screen.K2, Screen.K3, Screen.K4)
            assertEquals("$a $b", a == b || (a in yours && b in yours), SetDown.sameBand(a, b))
        }
        assertFalse(SetDown.sameBand(null, Screen.K1))
    }

    /** Which screens say the same under their band: lessons 2 and 3; and the key's first and third screen where no other key is left to try. */
    @Test fun whichScreensShareACaption() {
        val quick = FirstRun.Key.QUICK_INSERT
        assertTrue(SetDown.sameCaption(Screen.L2, Screen.L3, quick))
        assertFalse(SetDown.sameCaption(Screen.L3, Screen.L4, quick))
        assertFalse(SetDown.sameCaption(Screen.K4, Screen.L2, quick))
        // With Quick Insert suggested every screen of the key's step says something else; "Your key works" and the question say nothing.
        for (a in listOf(Screen.K1, Screen.K2, Screen.K3, Screen.K4)) for (b in listOf(Screen.K1, Screen.K2, Screen.K3, Screen.K4)) assertEquals("$a $b", a == b, SetDown.sameCaption(a, b, quick))
        // With Action + M suggested there is no other key to try: the third screen says what the first said.
        assertTrue(SetDown.sameCaption(Screen.K1, Screen.K3, FirstRun.Key.M))
        assertFalse(SetDown.sameCaption(Screen.K1, Screen.K2, FirstRun.Key.M))
        assertTrue(SetDown.sameCaption(Screen.K4, Screen.Q, quick))
        assertFalse(SetDown.sameCaption(null, Screen.K1, quick))
        assertTrue(SetDown.sameCaption(null, Screen.K4, quick))
    }
}
```

- [ ] **Step 2: Run it and see it fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SetDownTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'SetDown'` among its `e:` lines.

- [ ] **Step 3: Write `core/src/main/kotlin/io/github/kuscher/booklight/core/SetDown.kt`**

```kotlin
package io.github.kuscher.booklight.core

/**
 * First run's stage is not there in one frame: a screen is set down part by part, comes back as rows
 * do, or takes another screen's place where that stood. When each of its parts comes is said here, as
 * data (docs/design/first-run/motion.md §3: T1, T5, T8, and §2.2 for the landing of the opening
 * piece). The app draws the stage by these marks on one clock, and counts by them too: a screen takes
 * a key only once the answer Enter would run has begun to show ([Marks.lead], `FirstRun.inView`).
 * The times themselves are the app's, as all motion's are (`overlay/Motion.kt`): it hands them in as
 * a [Pace].
 */
object SetDown {
    /** How a screen comes onto the glass. */
    enum class Comes {
        /** It stands whole at once: the field was emptied again, a panel was turned round, the system's animations are off. */
        WHOLE,
        /** Set down part by part at the gate of a run's first opening: the seat, then the caps a beat apart or a recipe written a letter at a time, the caption, the answers (T1, T5). */
        SET_DOWN,
        /** At the gate of a later opening: its parts arrive as rows do, and nothing is written again. */
        BACK,
        /** Where another screen stood: the seat stays and its words roll; what changes in the band is written anew; what stays does not move (T5 in the same panel; the key's screens). */
        TURN,
        /** After a list that gave way (a lesson's Enter, the sum copied, "First steps" asked for): set down once its rows have begun to fade (T8). */
        AFTER_LIST,
        /** At the landing of the opening piece, behind the highlight that travels into its armed answer (§2.2, B5). */
        LANDS,
    }

    /**
     * The times, in ms. [part]: between the seat's parts, and between parts that come as rows do.
     * [beat]: between two pieces of a band. [letter]: between two letters of a typed part. [band]: how
     * long after the gate the band of a screen set down at an opening begins, at the least. [turn]:
     * how long after its words began to roll a screen's new band is written. [afterList]: how long
     * after a list gave way its stage begins. [press]: how long what gives way takes to fade.
     * [askText], [askNote], [askAnswers]: the question's parts, counted from its seat. [landsDisc] to
     * [landsBand]: the parts at a landing, counted from its beginning. [rest]: from a stage's last
     * part beginning to its being at rest.
     */
    data class Pace(
        val part: Int, val beat: Int, val letter: Int, val band: Int, val turn: Int, val afterList: Int, val press: Int,
        val askText: Int, val askNote: Int, val askAnswers: Int,
        val landsDisc: Int, val landsWords: Int, val landsCounter: Int, val landsArmed: Int, val landsBand: Int, val rest: Int,
    )

    /** The mark of a part that does not come: it is there. */
    const val THERE = -100_000

    /**
     * When each part comes, in ms after the screen came (for a screen the panel opens on: after the
     * gate). [disc], [words], [counter]: the seat. [pieces]: the band's pieces from left to right
     * (the question's: its text, its note); [written]: a typed part's letters follow its piece one
     * after the other ([letter]), else they stand with it. [caption]: the line under the band.
     * [answers]: the answers with their `tab` cap; [armed]: the words of the answer Enter would run,
     * which at a landing come first, inside the highlight. [rises]: the answers rise as they come;
     * not at a landing, where the highlight is on its way to where they stand. [end]: everything is
     * at rest.
     */
    data class Marks(
        val disc: Int, val words: Int, val counter: Int, val pieces: List<Int>, val written: Boolean,
        val caption: Int, val answers: Int, val armed: Int, val rises: Boolean, val end: Int,
    ) {
        /** How long after the screen came the answer Enter would run begins to show: from then the screen is in view. */
        val lead: Int get() = maxOf(0, armed)
    }

    /** Nothing comes: everything is there. */
    val WHOLE = Marks(THERE, THERE, THERE, emptyList(), written = false, caption = THERE, answers = THERE, armed = THERE, rises = false, end = 0)

    /** When piece [k] of the band begins; a band asked for a piece it has not got answers with its last. */
    fun piece(m: Marks, k: Int): Int = if (m.pieces.isEmpty()) THERE else m.pieces[k.coerceIn(0, m.pieces.lastIndex)]

    /** When letter [i] of the typed part that is piece [k] comes. */
    fun letter(m: Marks, pace: Pace, k: Int, i: Int): Int {
        val at = piece(m, k)
        return if (at == THERE || !m.written) at else at + pace.letter * maxOf(0, i)
    }

    /**
     * The marks of a screen that comes as [comes] says. [letters]: the band's pieces from left to
     * right: for a typed part of a recipe how many letters it has, 0 for a cap or a sign. [ask]: the
     * question, whose band is its text and its note. [caption]: a line stands under the band.
     * [bandAfter]: how long after the gate the band of a screen set down at an opening begins (the
     * glass is at rest by then). [bandChanges], [captionChanges], [answersChange]: for a screen that
     * takes another's place, whether its band, its caption and its answers are other than the ones
     * that stood: what says the same does not move.
     */
    fun marks(
        comes: Comes, pace: Pace, letters: List<Int>, ask: Boolean, caption: Boolean, bandAfter: Int = pace.band,
        bandChanges: Boolean = true, captionChanges: Boolean = true, answersChange: Boolean = true,
    ): Marks {
        if (comes == Comes.WHOLE) return WHOLE
        val p = pace.part
        val base = if (comes == Comes.AFTER_LIST) pace.afterList else 0
        // The seat's three parts. Where a screen takes another's place they are there already, and their words roll.
        val seat = when (comes) {
            Comes.TURN -> listOf(THERE, THERE, THERE)
            Comes.LANDS -> listOf(pace.landsDisc, pace.landsWords, pace.landsCounter)
            else -> listOf(base, base + p, base + 2 * p)
        }
        if (ask) {
            // The question: its text, its note and its answers in reading order, also where it comes back.
            val from = when (comes) { Comes.TURN -> pace.turn - pace.askText; Comes.LANDS -> pace.landsBand - pace.askText; else -> base }
            val answers = from + pace.askAnswers
            return Marks(seat[0], seat[1], seat[2], listOf(from + pace.askText, from + pace.askNote), written = false, caption = THERE, answers = answers, armed = answers, rises = comes != Comes.LANDS, end = answers + pace.rest)
        }
        // The band. A screen come back to has it whole, as a row; one that keeps the band of the screen before it does not move it.
        val whole = comes == Comes.BACK
        val from = when (comes) {
            Comes.SET_DOWN -> bandAfter
            Comes.BACK -> 3 * p
            Comes.TURN -> if (bandChanges) pace.turn else THERE
            Comes.LANDS -> pace.landsBand
            else -> base + 3 * p
        }
        val pieces = ArrayList<Int>(letters.size)
        var last = from
        for (k in letters.indices) {
            val at = if (whole || from == THERE || k == 0) from else last + pace.beat
            pieces += at
            last = if (whole || from == THERE) from else at + pace.letter * maxOf(0, letters[k] - 1)
        }
        // What follows the band: the caption a beat after its last piece, the answers one part after that.
        val after = when {
            whole -> from + p
            from == THERE -> pace.press
            letters.isEmpty() -> from
            else -> last + pace.beat
        }
        val captionAt = if (!caption || (comes == Comes.TURN && !captionChanges)) THERE else after
        val rest = if (caption) after + p else after
        val answers = if (comes == Comes.TURN && !answersChange) THERE else rest
        // (At a landing the armed answer's words fade in inside the highlight that has come for them, before the band.)
        val armed = if (comes == Comes.LANDS) pace.landsArmed else answers
        return Marks(seat[0], seat[1], seat[2], pieces, written = !whole, caption = captionAt, answers = answers, armed = armed, rises = comes != Comes.LANDS, end = maxOf(0, last, captionAt, answers, armed, seat[2]) + pace.rest)
    }

    /**
     * Whether two screens show the same thing in their band, so that it stays where it is when the
     * one takes the other's place: the key's screens after the first all show "your key". Every other
     * screen has a band of its own.
     */
    fun sameBand(a: FirstRun.Screen?, b: FirstRun.Screen): Boolean {
        val yours = setOf(FirstRun.Screen.K2, FirstRun.Screen.K3, FirstRun.Screen.K4)
        return a == b || (a in yours && b in yours)
    }

    /**
     * Whether the caption under two screens' bands says the same, so that it stays where it is when
     * the one takes the other's place: lessons 2 and 3 share theirs ("Practice: nothing opens…"), and
     * two of the key's screens do where the suggested key leaves them the same words. [suggested]:
     * the key that is suggested, which the key's captions are worded for.
     */
    fun sameCaption(a: FirstRun.Screen?, b: FirstRun.Screen, suggested: FirstRun.Key): Boolean {
        fun words(on: FirstRun.Screen?): String = when (on) {
            null, FirstRun.Screen.Q, FirstRun.Screen.C -> "none"
            FirstRun.Screen.L2, FirstRun.Screen.L3 -> "practice"
            FirstRun.Screen.L4 -> "stays"
            else -> FirstRun.caption(on, suggested).name.lowercase()
        }
        return words(a) == words(b)
    }
}
```

- [ ] **Step 4: A screen's caption by itself, in `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`.** One change: `caption` for a state answers with a new `caption` for a screen, which `SetDown.sameCaption` asks.

Find:

```kotlin
    fun caption(s: State, suggested: Key): Caption = when (screen(s)) {
```

Make it:

```kotlin
    fun caption(s: State, suggested: Key): Caption = caption(screen(s), suggested)

    /** The caption of the screen [on], whichever state it stands in. */
    fun caption(on: Screen?, suggested: Key): Caption = when (on) {
```

- [ ] **Step 5: Run it and see it pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SetDownTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Run every core test, and build the app**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`.

- [ ] **Step 7: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/SetDown.kt core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/test/kotlin/io/github/kuscher/booklight/core/SetDownTest.kt
git commit -m "First run: when each part of a stage comes, for every way a screen can come, as data (core, tested)"
```

### Task 5: `Motion.kt`: the stage's times, the springs it is drawn by, and the spans a key is held

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Motion.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt`

**Interfaces:**
- Consumes: core `SetDown.Pace` (Task 4); in `Motion.kt`: `class Motion(val on: Boolean, val slow: Float = 1f)` with `fun hold(ms: Long): Long`, `place()`, `pop()`, `fade(ms, delay)`, `stagger(i)`, its companion's constants, `object Lights`; in `Welcome.kt`: `private class Sprung`, `private val PLACE`, `private val POP`.
- Produces, in `Motion.kt`:
  - `fun Motion.held(ms: Long): Long`: a span of motion that is neither a spring nor a tween; stretched by a debug build's slow motion, and 0 with the system's animations off. (`hold` stays for a pause that is there to be read.)
  - In `Motion`'s companion, all `const val`: `BEAT_MS = 66`, `LETTER_MS = 40`, `PART_MS = 22`, `RISE_MS = 140`, `CAP_MS = 110`, `CAP_SMALL = 0.96f`, `BAND_AFTER_MS = 120`, `GATE_TO_REST_MS = 81`, `TURN_MS = 120`, `AFTER_LIST_MS = 60`, `ASK_TEXT_MS = 66`, `ASK_NOTE_MS = 120`, `ASK_ANSWERS_MS = 142`, `LANDS_DISC_MS = 80`, `LANDS_WORDS_MS = 150`, `LANDS_COUNTER_MS = 190`, `LANDS_ARMED_MS = 180`, `LANDS_BAND_MS = 260`, `REST_MS = 170`, `PRESS_MS = 80`, `RELEASE_MS = 120`, `KEY_TAP_MS = 160L`, `KEY_HELD_MS = 400L`, `CHECK_AFTER_MS = 60L`, `CHECK_MS = 140L`, `LAP_AFTER_MS = 240L`, `HELPER_AFTER_MS = 300L`, `ASK_LEAVES_MS = 60L`, `FOLD_CAP_MS = 120`, `FOLD_LAP_MS = 360L`, `COACH_AFTER_MS = 80`.
  - Top level: `internal class Sprung(spec: FiniteAnimationSpec<Float>)` with `fun at(ms: Float): Float`; `internal val PLACE`, `internal val POP` (the panel's `place` and `pop` springs as functions of time); `val PACE: SetDown.Pace`, made of the constants above.
  - In `object Lights`: `const val MARK_AFTER_MS = 520`, `const val DEEPENS_FROM = 0.5f`.
  - `Welcome.kt` no longer has `Sprung`, `PLACE` and `POP` of its own: it uses these.

All motion's times and curves live in `Motion.kt`. This task puts every time of this part there, before anything uses it, so that the tasks that draw (8, 10, 11, 12) write no number of their own. The stage is drawn on a clock of its own, as the welcome is (a part is where its mark and that clock say, and nothing is kept from frame to frame): so the welcome's two springs as functions of time move here, where the stage can use them too.

Nothing changes on the glass by this task.

- [ ] **Step 1: The times and the springs, in `app/src/main/java/io/github/kuscher/booklight/overlay/Motion.kt`.** Seven changes, in the file's order: three imports; `held`; the stage's constants at the end of the companion; `Sprung`, `PLACE`, `POP` and `PACE` above `LocalMotion`; two constants at the end of `Lights`.

Find:

```kotlin
import androidx.compose.animation.core.CubicBezierEasing
```

Make it:

```kotlin
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
```

Find:

```kotlin
import androidx.compose.animation.core.snap
```

Make it:

```kotlin
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
```

Find:

```kotlin
import kotlin.math.exp
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.SetDown
import kotlin.math.exp
```

Find:

```kotlin
    fun hold(ms: Long): Long = (ms * slow).toLong()
```

Make it:

```kotlin
    fun hold(ms: Long): Long = (ms * slow).toLong()

    /**
     * A span of motion that is neither a spring nor a tween (how long a key on the glass is held down, how long a part
     * waits its turn): stretched as they are, and nothing with the system's animations off. A pause that is there to be
     * read is [hold], which stays.
     */
    fun held(ms: Long): Long = if (on) (ms * slow).toLong() else 0L
```

Find:

```kotlin
        /** How far the glass is open (0 to 1) before the panel may grow past the field's height: what is under the field arrives after the opening, never as part of it. */
```

Make it:

```kotlin

        // First run's stage, and what moves between its screens (docs/design/first-run/motion.md §3 and §4). The stage is
        // drawn on a clock of its own, by core's schedule (`SetDown`, which takes these as [PACE]): that clock's lengths, in ms.
        /** One key after another: a cap, or a new typed part of a recipe, comes this long after the piece before it (three steps of [stagger]). */
        const val BEAT_MS = 66
        /** A recipe's typed part is written a letter at a time, each this long after the one before ([typeStep] at these lengths). */
        const val LETTER_MS = 40
        /** The seat's parts, and whatever comes "as rows do", come this far apart ([stagger]'s step). */
        const val PART_MS = 22
        /** A part rises 12 dp on `place` and fades in over this long, as a row does; the caption fades in over the same. A cap comes up over [CAP_MS], from [CAP_SMALL] of its size, on `pop`. */
        const val RISE_MS = 140
        const val CAP_MS = 110
        const val CAP_SMALL = 0.96f
        /** The band of a screen set down at an opening begins when the glass is at rest, and this long after the gate at the least. */
        const val BAND_AFTER_MS = 120
        /** From the gate to the glass at rest at Fast (motion.md §3, "Clocks": 129 to 210); times the opening's speed. */
        const val GATE_TO_REST_MS = 81
        /** A screen that takes another's place where it stands: its band is written this long after its words began to roll, once the old band has faded ([PRESS_MS]). */
        const val TURN_MS = 120
        /** A stage that follows a list (a lesson's Enter, the sum copied, "First steps" asked for) begins this long after the list gave way: its rows have begun to fade by then. */
        const val AFTER_LIST_MS = 60
        /** The question's parts after its seat: its text, its note, its answers (motion.md T8: 646, 700 and 722 for a seat at 580). */
        const val ASK_TEXT_MS = 66
        const val ASK_NOTE_MS = 120
        const val ASK_ANSWERS_MS = 142
        /** At the landing of the opening piece (motion.md §2.2, B5, counted from its T): the disc, the words, the counter; the armed answer's words inside the highlight that travels there; the band's first piece. What follows the band comes as everywhere. */
        const val LANDS_DISC_MS = 80
        const val LANDS_WORDS_MS = 150
        const val LANDS_COUNTER_MS = 190
        const val LANDS_ARMED_MS = 180
        const val LANDS_BAND_MS = 260
        /** A stage is at rest this long after its last part began to come. */
        const val REST_MS = 170
        /** Pressed: ink over the slot or the cap, this long in and this long out (the design system's pressed token). */
        const val PRESS_MS = 80
        const val RELEASE_MS = 120
        /**
         * "Your key" on the glass is held down as the user's own key is: this long where the keys were pressed with the panel
         * in view, and this long where they uncovered it under the system's dialog (the dialog's own fade, about 240 ms, and
         * 160 ms in view). Then it comes up on `pop`; the check draws [CHECK_AFTER_MS] later, over [CHECK_MS]; the light sets
         * off [LAP_AFTER_MS] after the key came up.
         */
        const val KEY_TAP_MS = 160L
        const val KEY_HELD_MS = 400L
        const val CHECK_AFTER_MS = 60L
        const val CHECK_MS = 140L
        const val LAP_AFTER_MS = 240L
        /** Under the system's dialog the stage turns to "Now press your keys" this long after the panel lost the focus to it: the dialog covers the panel by then, and nothing is seen to pop beside it. */
        const val HELPER_AFTER_MS = 300L
        /** The question gives way to the choices: the lower edge rises only after this long, so that it cuts no word that is still fading. */
        const val ASK_LEAVES_MS = 60L
        /** The fold: the field's `esc` cap fades back this long after the choices were left, and the light sets off round the bare field this long after. */
        const val FOLD_CAP_MS = 120
        const val FOLD_LAP_MS = 360L
        /** A coach line that takes the seat of a word that just happened ("Copied") waits for that word's fade. */
        const val COACH_AFTER_MS = 80
        /** How far the glass is open (0 to 1) before the panel may grow past the field's height: what is under the field arrives after the opening, never as part of it. */
```

Find:

```kotlin
internal val FOLDS = CubicBezierEasing(0.45f, 0f, 0.4f, 1f)

/**
 * First run's welcome, "Lights on" (docs/design/first-run/motion.md §2.1): one clock, and every mark of it here, in
```

Make it:

```kotlin
internal val FOLDS = CubicBezierEasing(0.45f, 0f, 0.4f, 1f)

/**
 * One of the panel's springs as a function of time: how far it has come, 0 to a little past 1, [ms] after it set off.
 * For what is drawn on a clock of its own and keeps nothing from frame to frame: first run's welcome, and its stage.
 */
internal class Sprung(spec: FiniteAnimationSpec<Float>) {
    private val spring = spec.vectorize(Float.VectorConverter)
    private val zero = AnimationVector1D(0f)
    private val one = AnimationVector1D(1f)
    fun at(ms: Float): Float = if (!(ms > 0f)) 0f else if (ms > 4000f) 1f else spring.getValueFromNanos((ms * 1_000_000f).toLong(), zero, one, zero).value
}
/** (Such a clock is already as slow as a debug build asks: the springs are asked at their own speed.) */
internal val PLACE = Sprung(Motion(true).place())
internal val POP = Sprung(Motion(true).pop())

/**
 * The times of first run's stage as core's schedule takes them (`SetDown.marks`: when each part of a screen comes, for
 * every way a screen can come). The stage is drawn on a clock of its own by that schedule, and the model counts a
 * screen as in view by the same marks.
 */
val PACE = SetDown.Pace(
    part = Motion.PART_MS, beat = Motion.BEAT_MS, letter = Motion.LETTER_MS, band = Motion.BAND_AFTER_MS, turn = Motion.TURN_MS,
    afterList = Motion.AFTER_LIST_MS, press = Motion.PRESS_MS,
    askText = Motion.ASK_TEXT_MS, askNote = Motion.ASK_NOTE_MS, askAnswers = Motion.ASK_ANSWERS_MS,
    landsDisc = Motion.LANDS_DISC_MS, landsWords = Motion.LANDS_WORDS_MS, landsCounter = Motion.LANDS_COUNTER_MS,
    landsArmed = Motion.LANDS_ARMED_MS, landsBand = Motion.LANDS_BAND_MS, rest = Motion.REST_MS,
)

/**
 * First run's welcome, "Lights on" (docs/design/first-run/motion.md §2.1): one clock, and every mark of it here, in
```

Find:

```kotlin
    /** The hand-over's length: at its end the show's first letter lands on the caret. */
```

Make it:

```kotlin
    /** Booklight's mark comes up in the field's seat as that light passes it, before the show's first letter: it says who is about to type. */
    const val MARK_AFTER_MS = 520
    /**
     * The night is deeper towards the glass's corners, but only while it is night: that shade begins once the night has
     * fallen this far (0 to 1) and is whole at full night, and it leaves the same way before the theme's own glass is
     * back. (Under a glass that is half day its edge showed as a faint ring.)
     */
    const val DEEPENS_FROM = 0.5f
    /** The hand-over's length: at its end the show's first letter lands on the caret. */
```

- [ ] **Step 2: The welcome uses them, in `app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt`.** Three changes: three imports go; its own `Sprung` and springs go.

Find:

```kotlin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
```

Make it:

```kotlin
import androidx.compose.animation.core.Animatable
```

Find:

```kotlin
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
```

Make it:

```kotlin
import androidx.compose.animation.core.FastOutSlowInEasing
```

Find:

```kotlin
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
```

Make it:

```kotlin
// (The panel's springs as functions of the welcome's clock, `PLACE` and `POP`, are `Motion.kt`'s: first run's stage is drawn by them too.)
```

- [ ] **Step 3: Every time of the stage is in one place**

```bash
grep -c "private class Sprung\|private val PLACE\|private val POP" app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt
grep -c "^val PACE = SetDown.Pace(" app/src/main/java/io/github/kuscher/booklight/overlay/Motion.kt
```

Expected: `0`, then `1`.

- [ ] **Step 4: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/Motion.kt app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt
git commit -m "First run: the stage's times are Motion's, with the springs it is drawn by and the spans a key is held (nothing uses them yet)"
```

### Task 6: "First steps" in the panel: the command, and the word that the steps wait

**Files:**
- Modify: `app/src/main/res/values/strings_32.xml`
- Modify: `app/src/main/res/values-de/strings_32.xml`
- Modify: `app/src/main/res/values/strings_20.xml`
- Modify: `app/src/main/res/values-de/strings_20.xml`
- Modify: `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/providers/Providers.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/Guide.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`

**Interfaces:**
- Consumes: core `FirstRun.AGAIN`, `FirstRun.againHere(s)`, `FirstRun.later(before, after)` (Task 2), `FirstRun.fits(screenDp: Float)`, `FirstRun.opening(s, screenDp, plain)`, `FirstRun.answer(s, answer)`. In `OverlayModel`: `step { … }` (the one way it writes first run's state), `first()`, `due`, `stage`, `demo`, `playing`, `guided`, `carries`, `zeroStood`, `zeroUp`, `ended`, `taken`, `flash`, `resuggest()`, `exemplify()`, `empty()`, `cameAt`, `used(r, a)`, `fun answerStage(answer: FirstRun.Answer)`, `firstHint`. In `OverlayActivity`: `first(app) { … }`, `run(r: Result, a: Action)`, the local `run` of `onCreate` (the state after the opening was counted), `SystemWords.load(context, scope)`. In `Providers.kt`: `CommandsProvider`'s private `Command(key, title, subtitle, words, effect, symbol, asks)`. In `Guide.kt`: `entries()`, the line of a row. The `guide` string array, whose lines are `id|group|mark|scope|example|name|line`.
- Produces:
  - Strings, English and German: `first_name`, `first_words`, `first_row_text`, `first_row_go_on`, `first_later_hint`, `a11y_first_later`, `a11y_first_goes_on_change`; and one line more in the `guide` array (`first|booklight|again||first steps|…`, German example `erste schritte`).
  - `BooklightApp.firstFits: Boolean` (lazy): whether first run stands in the panel on this device's screen.
  - The command "First steps" (`command:first`, mark `again`), only where `firstFits`; found by its name and by `first_words`.
  - `OverlayModel.later: Boolean` (state): the bare field says where the steps wait, for the rest of this opening. `OverlayModel.again()`: the run asked for again in this panel. `OverlayModel.answerStage(answer)` sets `later` where the answer made the steps wait.
  - `OverlayActivity.run` catches `FirstRun.AGAIN` before anything is run; `onCreate` sets `model.later` where this opening is the first in which an unfinished run waits.

**The command.** "First steps" is a row of the commands' own kind, beside "Booklight settings". What it carries is `FirstRun.AGAIN`, an `Effect.Internal` the executor does not know: it never gets there. `OverlayActivity.run` is where everything that runs passes (Enter, a click, Ctrl + digit), and it catches the effect before the lessons' rule and before the executor: nothing is run, the panel stays, `model.again()` writes the run through `step` and empties the field in one change, and the screen that is due stands under it. That screen came in this moment: the Enter that asked for it, pressed again at once, answers nothing on it (part 3's `seen`). No opening piece: `againHere` asks for none, and `begin()` is not called.

It is not offered on a screen too low for first run to stand in the panel (`firstFits`), and the list of everything leaves its line out there, so that `./bl debug guide` has no example that finds nothing. `firstFits` asks the system once, lazily, and only where a command list or the guide is built: someone who opens the panel and types nothing pays nothing for it.

**The word.** After "Not now" on the key's step, and in the first opening after a run's three, the bare field's placeholder says "First steps wait in Booklight’s window", for the rest of that opening (`firstHint`). The activity reads the state before it counts the opening and compares (`FirstRun.later`): one read of settings that are in memory, and only a comparison for someone with no run.

- [ ] **Step 1: The English strings, in `app/src/main/res/values/strings_32.xml`.** One change, at the end of the file.

Find:

```xml
</resources>
```

Make it:

```xml

    <!-- First run · "First steps": the run asked for again (design.md §11). A command in the panel, found by its name and by these other words; a row on the Booklight window's Start page. -->
    <string name="first_name">First steps</string>
    <string name="first_words">tour,welcome,intro</string>
    <!-- The row in the window: what it plays; and its name while a run is not finished. -->
    <string name="first_row_text">The key, three things to try, your choices. About a minute.</string>
    <string name="first_row_go_on">Go on with the first steps</string>
    <!-- The field's placeholder for the rest of an opening in which the steps began to wait in the window: "Not now" on the key's step, or a run that has stood in its three openings. -->
    <string name="first_later_hint">First steps wait in Booklight’s window</string>
    <!-- For a screen reader: the same, as a sentence; and what "Your key works" offers where the run was asked for again. -->
    <string name="a11y_first_later">The first steps wait in Booklight’s window, on its Start page.</string>
    <string name="a11y_first_goes_on_change">Enter goes on. Tab for Change the key.</string>
</resources>
```

- [ ] **Step 2: The German strings, in `app/src/main/res/values-de/strings_32.xml`.** One change, at the end of the file.

Find:

```xml
</resources>
```

Make it:

```xml

    <string name="first_name">Erste Schritte</string>
    <string name="first_words">tour,willkommen,intro,einführung</string>
    <string name="first_row_text">Die Taste, drei Dinge zum Ausprobieren, deine Auswahl. Etwa eine Minute.</string>
    <string name="first_row_go_on">Erste Schritte fortsetzen</string>
    <string name="first_later_hint">Die ersten Schritte warten in Booklights Fenster</string>
    <string name="a11y_first_later">Die ersten Schritte warten in Booklights Fenster, auf der Seite „Start“.</string>
    <string name="a11y_first_goes_on_change">Enter geht weiter. Tab für „Taste ändern“.</string>
</resources>
```

- [ ] **Step 3: The line in the list of everything, in `app/src/main/res/values/strings_20.xml`.** One change: a line at the end of the `guide` array.

Find:

```xml
    </string-array>

    <!-- Tips -->
```

Make it:

```xml
        <item>first|booklight|again||first steps|First steps|The key, three things to try and your choices, once more, here in the panel</item>
    </string-array>

    <!-- Tips -->
```

- [ ] **Step 4: The same line in German, in `app/src/main/res/values-de/strings_20.xml`.** One change.

Find:

```xml
    </string-array>

    <!-- Tipps -->
```

Make it:

```xml
        <item>first|booklight|again||erste schritte|Erste Schritte|Die Taste, drei Dinge zum Ausprobieren und deine Auswahl, noch einmal, hier im Panel</item>
    </string-array>

    <!-- Tipps -->
```

- [ ] **Step 5: Whether first run fits this screen, in `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`.** One change: `firstFits`, above `providers`.

Find:

```kotlin
    }

    /** Every source of results. A new ability is one more line here. */
```

Make it:

```kotlin
    }

    /**
     * Whether first run stands in the panel on this device's screen (core `FirstRun.fits`): on a lower one "First steps"
     * is not offered, neither as a command nor in the list of everything. Asked of the system once, when it is first needed.
     */
    val firstFits: Boolean by lazy {
        FirstRun.fits(getSystemService(android.view.WindowManager::class.java).maximumWindowMetrics.bounds.height() / resources.displayMetrics.density)
    }

    /** Every source of results. A new ability is one more line here. */
```

- [ ] **Step 6: The command, in `app/src/main/java/io/github/kuscher/booklight/providers/Providers.kt`.** Three changes: an import, the provider's comment, the command in its list (which is now `listOfNotNull`).

Find:

```kotlin
import io.github.kuscher.booklight.core.Icon
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.core.Icon
```

Find:

```kotlin
/** Booklight's own pages, findable like anything else; and the Clock's lists of alarms and timers. */
```

Make it:

```kotlin
/** Booklight's own pages, findable like anything else; the Clock's lists of alarms and timers; and "First steps", first run asked for again. */
```

Find:

```kotlin
        listOf(
            Command("settings", R.string.cmd_settings, null, R.string.cmd_settings_words, Effect.Internal("settings")),
```

Make it:

```kotlin
        listOfNotNull(
            Command("settings", R.string.cmd_settings, null, R.string.cmd_settings_words, Effect.Internal("settings")),
            // First run, once more, in the panel that is open (core `FirstRun.AGAIN`: the panel catches it, nothing is run). Not
            // on a screen too low for first run to stand in the panel.
            Command("first", R.string.first_name, null, R.string.first_words, FirstRun.AGAIN, "again").takeIf { (context.applicationContext as io.github.kuscher.booklight.BooklightApp).firstFits },
```

- [ ] **Step 7: The list of everything, in `app/src/main/java/io/github/kuscher/booklight/Guide.kt`.** Two changes: the line is left out where the command is; and a row of that command is the line `first`.

Find:

```kotlin
            if (f.size < 7 || f[3] in gone || (f[0] == "alarms" && !clock)) return@mapNotNull null
```

Make it:

```kotlin
            // (Nor "First steps" on a screen too low for first run to stand in the panel: its command is not there.)
            if (f.size < 7 || f[3] in gone || (f[0] == "alarms" && !clock) || (f[0] == "first" && !(context.applicationContext as BooklightApp).firstFits)) return@mapNotNull null
```

Find:

```kotlin
                "commands" -> if (row.id == "command:alarms" || row.id == "command:timers") "alarms" else null
```

Make it:

```kotlin
                "commands" -> if (row.id == "command:alarms" || row.id == "command:timers") "alarms" else if (row.id == "command:first") "first" else null
```

- [ ] **Step 8: Asked for again, and where the steps wait, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Three changes: `answerStage` says where "Not now" left the steps; a new section with `later` and `again()`; the placeholder.

Find:

```kotlin
    fun answerStage(answer: FirstRun.Answer) = step { FirstRun.answer(it, answer) }
```

Make it:

```kotlin
    fun answerStage(answer: FirstRun.Answer) {
        val was = first()
        step { FirstRun.answer(it, answer) }
        // "Not now" on the key's step ends the run: the steps wait in the Booklight window, and the bare field says so.
        if (FirstRun.later(was, first())) later = true
    }
```

Find:

```kotlin
    }

    // ---- first run: the opening piece, played
```

Make it:

```kotlin
    }

    // ---- first run: asked for again, and where the steps wait

    /**
     * The steps have begun to wait in the Booklight window in this opening (core `FirstRun.later`): "Not now" was
     * answered on the key's step, or an unfinished run has stood in its three openings (the activity says that one). The
     * bare field's placeholder says where they are, for the rest of this opening.
     */
    var later by mutableStateOf(false)

    /**
     * "First steps", asked for by its command in this panel (core `FirstRun.againHere`): an unfinished run goes on where
     * it stopped, a finished one is played again from its first screen, without the opening piece: the user is at work
     * here. The field is emptied in one change and the screen that is due stands under it, whatever this panel was opened
     * for (an example typed into it, another app's text) and whatever stood under its empty field before. The screen came
     * in this moment: the Enter that asked for it, pressed again at once, answers nothing on it ([seen]).
     */
    fun again() {
        if (demo || playing != null) return
        guided = false; carries = false
        step { FirstRun.againHere(it) }
        // (What stood under the empty field in this opening gives its place to the stage: `Under`'s order.)
        zeroStood = false; zeroUp = false; ended = false; later = false; taken = null; flash = null
        resuggest()
        exemplify()
        empty()
        cameAt = SystemClock.uptimeMillis()
    }

    // ---- first run: the opening piece, played
```

Find:

```kotlin
            // Where the opening piece is this opening's and is not played, its title greets here, over the key's step.
```

Make it:

```kotlin
            // The steps began to wait in the Booklight window in this opening: where they are.
            later -> app.getString(R.string.first_later_hint)
            // Where the opening piece is this opening's and is not played, its title greets here, over the key's step.
```

- [ ] **Step 9: The panel catches the command, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Three changes in `onCreate` and `run`: the state before the opening is counted; `later` for a run that has used up its openings; the command caught where everything that runs passes.

Find:

```kotlin
        val plain = app.example == null && handed(intent) == null
```

Make it:

```kotlin
        val plain = app.example == null && handed(intent) == null
        val counted = app.prefs.now.firstRun()
```

Find:

```kotlin
        // The piece begins with this panel: the welcome, then the show, then the key's step. Where it is this opening's and
```

Make it:

```kotlin
        // An unfinished run that has stood in its three openings waits in the Booklight window from this one on: the bare
        // field says so, in this opening alone (core `FirstRun.later`).
        if (FirstRun.later(counted, run)) model.later = true
        // The piece begins with this panel: the welcome, then the show, then the key's step. Where it is this opening's and
```

Find:

```kotlin
        // First run: while a lesson stands nothing opens (core `FirstRun.enters`). An app's Open and a search inside an app are
```

Make it:

```kotlin
        // "First steps" is the panel's own (core `FirstRun.AGAIN`): nothing is run and the panel stays; the run stands under
        // the emptied field. Caught here, where everything that runs passes (Enter, a click, Ctrl + digit).
        if (a.effect == FirstRun.AGAIN) {
            model.used(r, a)
            model.again()
            // (The system's own words for its dialog are read before the key's step quotes them, as at an opening.)
            if (model.due?.step == FirstRun.Step.KEY) SystemWords.load(this, app.scope)
            return
        }
        // First run: while a lesson stands nothing opens (core `FirstRun.enters`). An app's Open and a search inside an app are
```

- [ ] **Step 10: The command is caught before anything is run, and the strings are in both languages**

```bash
awk '/if \(a.effect == FirstRun.AGAIN\)/ { c = NR } /app.executor.run\(a.effect, this\)/ { x = NR } END { print (c > 0 && c < x) ? "IN-ORDER" : "OUT-OF-ORDER" }' app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
for n in first_name first_words first_row_text first_row_go_on first_later_hint a11y_first_later a11y_first_goes_on_change; do echo "$n $(grep -c "name=\"$n\"" app/src/main/res/values/strings_32.xml) $(grep -c "name=\"$n\"" app/src/main/res/values-de/strings_32.xml)"; done
grep -c "<item>first|booklight|again||" app/src/main/res/values/strings_20.xml app/src/main/res/values-de/strings_20.xml
grep -n "[\"']Taste ändern[\"']" app/src/main/res/values-de/strings_32.xml || echo CLEAN
```

Expected: `IN-ORDER`; seven lines, each a name followed by `1 1`; two lines ending `:1`; `CLEAN` (the German quotation is „…“).

- [ ] **Step 11: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 12: Commit**

```bash
git add app/src/main/res/values/strings_32.xml app/src/main/res/values-de/strings_32.xml app/src/main/res/values/strings_20.xml app/src/main/res/values-de/strings_20.xml app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt app/src/main/java/io/github/kuscher/booklight/providers/Providers.kt app/src/main/java/io/github/kuscher/booklight/Guide.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
git commit -m "First run: \"First steps\" is a command in the panel, and the bare field says where the steps wait"
```

### Task 7: "First steps" in the window, and the way to a key on its Start page

**Files:**
- Modify: `app/src/main/res/values/strings_11.xml`
- Modify: `app/src/main/res/values-de/strings_11.xml`
- Modify: `app/src/main/java/io/github/kuscher/booklight/window/Pages.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/window/MainActivity.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/window/Page.kt`

**Interfaces:**
- Consumes: core `FirstRun.offer(s): Offer` with `goesOn`, `count` (Task 2), `FirstRun.again(s, overture)`, `FirstRun.suggest(quickInsert: Boolean): Key`, `FirstRun.hasQuickInsert(keyboards: List<Keyboard>, last: Int?): Boolean`, `FirstRun.Key`; `BooklightApp.firstFits`, `R.string.first_name`, `first_row_text`, `first_row_go_on` (Task 6); `R.string.first_step` ("%1$d of %2$d"), `first_key_where`, `first_key_any`, `key_action` (part 2); `Prefs.firstRun { … }`, `Settings.firstRun()` (`data/Prefs.kt`); `device/Keyboards.attached()`, `device/SystemWords.load(context, scope)`, `SystemWords.name(context, key)`, `SystemWords.words(context)` with `customize` and `set`, `SystemWords.rows(context, key): KeyboardShortcutGroup` (part 2: the five steps as a group of the system's dialog); `OverlayActivity.ACTION_PANEL`; in `Pages.kt`: the Start page's `row(id) { place -> … }`, `PageRow(…)`, `Keys(…)`, `MarkIcon(name)`, `Opens()`, the local `s` (the settings), `resumed`, `asked`.
- Produces:
  - Strings, English and German: `win_key_text` and `win_key_steps` say the new way (`win_key_steps` takes the dialog's two buttons: `%1$s` Customize, `%2$s` Set shortcut); `win_key_or` is gone.
  - The Start page's key group shows the suggested keys as first run suggests them, with first run's caption, and its row's second line tells the five steps.
  - The Start page has the row `first`: "First steps", or "Go on with the first steps" with the counter. Only where `app.firstFits`.
  - `MainActivity.guides: Boolean`: once the Start page has asked for the system's dialog, the window's page in it begins with the five steps.

**The row.** It writes `FirstRun.again(it, overture = true)` through `Prefs.firstRun` and starts the panel with `ACTION_PANEL`, as the Commands page's rows start it for an example: by the user's click, from Booklight's own window. That panel is an ordinary new panel: `OverlayActivity.onCreate` counts the opening and asks how it begins, and a run that has not shown its opening piece plays it (the welcome, the show), whole, with the desk dimmed. Nothing here calls `begin()`. Where a run is unfinished the row goes on with it (`offer.goesOn`) and says where it stopped.

**The way to a key.** The page drew Action + Alt + Space "or" Action + K, and told a route through "App shortcuts" that the system's dialog no longer has. It now shows what first run shows: one suggestion (Action + Quick Insert where the keyboard has that key, else Action + M), under it first run's own caption, and the steps in the system's own words for its two buttons. And the dialog it opens begins, on the window's page, with the same five steps the panel shows while it waits for its key.

- [ ] **Step 1: The English words, in `app/src/main/res/values/strings_11.xml`.** One change: `win_key_text` and `win_key_steps`; `win_key_or` goes.

Find:

```xml
    <string name="win_key_text">Booklight opens when its keyboard shortcut is pressed, and the same keys put it away. The system asks for the Action key in every shortcut; these two are free.</string>
    <string name="win_key_or">or</string>
    <string name="win_key_steps">Choose App shortcuts, then Add shortcut, then Booklight</string>
```

Make it:

```xml
    <string name="win_key_text">Booklight opens when its keyboard shortcut is pressed, and the same keys put it away. The system asks for the Action key in every shortcut.</string>
    <!-- %1$s and %2$s are the system's own words for the two buttons of its Keyboard shortcuts dialog: Customize, Set shortcut. -->
    <string name="win_key_steps">Click %1$s, type Booklight, click +, press your keys, click %2$s. Then press your keys once more.</string>
```

- [ ] **Step 2: The German words, in `app/src/main/res/values-de/strings_11.xml`.** One change.

Find:

```xml
    <string name="win_key_text">Booklight öffnet sich, wenn sein Tastenkürzel gedrückt wird; dieselben Tasten schließen es wieder. Das System verlangt die Aktionstaste in jedem Kürzel; diese beiden sind frei.</string>
    <string name="win_key_or">oder</string>
    <string name="win_key_steps">App-Verknüpfungen wählen, dann Tastenkürzel hinzufügen, dann Booklight</string>
```

Make it:

```xml
    <string name="win_key_text">Booklight öffnet sich, wenn sein Tastenkürzel gedrückt wird; dieselben Tasten schließen es wieder. Das System verlangt die Taste „Aktion“ in jedem Kürzel.</string>
    <string name="win_key_steps">Auf „%1$s“ klicken, Booklight tippen, auf + klicken, deine Tasten drücken, auf „%2$s“ klicken. Dann die Tasten noch einmal drücken.</string>
```

- [ ] **Step 3: The Start page, in `app/src/main/java/io/github/kuscher/booklight/window/Pages.kt`.** Six changes, in the file's order: four imports; the suggested key and the system's words; the caps and their caption; the key's row; the row "First steps".

Find:

```kotlin
import io.github.kuscher.booklight.entry.PickFolderActivity
```

Make it:

```kotlin
import io.github.kuscher.booklight.data.firstRun
import io.github.kuscher.booklight.device.Keyboards
import io.github.kuscher.booklight.device.SystemWords
import io.github.kuscher.booklight.entry.PickFolderActivity
```

Find:

```kotlin
import io.github.kuscher.booklight.ui.AppIcons
```

Make it:

```kotlin
import io.github.kuscher.booklight.overlay.OverlayActivity
import io.github.kuscher.booklight.ui.AppIcons
```

Find:

```kotlin
        Group(stringResource(R.string.win_open_title)) {
            // What is so: no key yet (and then the two shortcuts the system has free), or the key works. Not a row to press.
```

Make it:

```kotlin
        // The keys that are suggested, as first run suggests them (core `FirstRun.suggest`): Action + Quick Insert where the
        // keyboard has that key, Action + M where it has not. The key's name and the dialog's two buttons are quoted in
        // the system's own words where they can be read (`SystemWords`: no permission), else in Booklight's.
        LaunchedEffect(Unit) { SystemWords.load(activity, app.scope) }
        val key = remember(resumed) { FirstRun.suggest(FirstRun.hasQuickInsert(Keyboards.attached(), null)) }
        val name = SystemWords.name(activity, key)
        val words = SystemWords.words(activity)
        val where = if (key == FirstRun.Key.QUICK_INSERT) stringResource(R.string.first_key_where, name) + " · " + stringResource(R.string.first_key_any) else stringResource(R.string.first_key_any)
        Group(stringResource(R.string.win_open_title)) {
            // What is so: no key yet (and then the keys that are suggested, and where the second one is), or the key works. Not a row to press.
```

Find:

```kotlin
                            Row(Modifier.padding(top = 8.dp, bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Keys(stringResource(R.string.key_action), "Alt", stringResource(R.string.key_space), ink = scheme.onSurfaceVariant)
                                Text(stringResource(R.string.win_key_or), color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                Keys(stringResource(R.string.key_action), "K", ink = scheme.onSurfaceVariant)
```

Make it:

```kotlin
                            // The caps on the text's edge, and under them what first run's own caption says: one edge for all three lines.
                            Column(Modifier.padding(top = 8.dp, bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Keys(stringResource(R.string.key_action), name, ink = scheme.onSurfaceVariant)
                                Text(where, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
```

Find:

```kotlin
                PageRow(page, "key", stringResource(R.string.set_shortcut_button), stringResource(R.string.win_key_steps), place = place,
                    mark = { MarkIcon("key") }, onEnter = { asked = true; activity.requestShowKeyboardShortcuts() }) { Opens() }
```

Make it:

```kotlin
                // The dialog then opens on the window's own page, which begins with the five steps (`MainActivity.guides`).
                PageRow(page, "key", stringResource(R.string.set_shortcut_button), stringResource(R.string.win_key_steps, words.customize, words.set), place = place,
                    mark = { MarkIcon("key") }, onEnter = { asked = true; (activity as? MainActivity)?.guides = true; activity.requestShowKeyboardShortcuts() }) { Opens() }
```

Find:

```kotlin
                    }
                }
            }
        }
    }
    // What else can stand under the empty field. Tips stay where they were; "Your usual" follows them, because it takes
```

Make it:

```kotlin
                    }
                }
            }
            // "First steps": first run, once more. The row plays the whole of it in a new panel, the opening piece first; while
            // a run is unfinished it goes on where that stopped, and says where (core `FirstRun.offer`, `again`). The panel is
            // started as a row of the Commands page starts it for an example: by this click, over Booklight's own window.
            // Not on a screen too low for first run to stand in the panel.
            if (app.firstFits) row("first") { place ->
                val offer = FirstRun.offer(s.firstRun())
                PageRow(page, "first", stringResource(if (offer.goesOn) R.string.first_row_go_on else R.string.first_name), stringResource(R.string.first_row_text), place = place, roll = true,
                    mark = { MarkIcon("again") },
                    onEnter = {
                        app.prefs.firstRun { FirstRun.again(it, overture = true) }
                        activity.startActivity(Intent(activity, OverlayActivity::class.java).setAction(OverlayActivity.ACTION_PANEL))
                    }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        offer.count?.let { Text(stringResource(R.string.first_step, it.first, it.second), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                        Opens()
                    }
                }
            }
        }
    }
    // What else can stand under the empty field. Tips stay where they were; "Your usual" follows them, because it takes
```

- [ ] **Step 4: The dialog's page, in `app/src/main/java/io/github/kuscher/booklight/window/MainActivity.kt`.** Three changes: three imports; `guides`; the five steps first in what the window tells the dialog.

Find:

```kotlin
import io.github.kuscher.booklight.data.Settings
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.data.Settings
import io.github.kuscher.booklight.device.Keyboards
import io.github.kuscher.booklight.device.SystemWords
```

Find:

```kotlin
    var asked = 0
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
```

Make it:

```kotlin
    var asked = 0
        private set
    /**
     * The Start page has asked for the system's Keyboard shortcuts dialog to give Booklight a key: from then on this
     * window's page in it begins with the five steps to a key, as the panel's page does while it waits for its key
     * (`SystemWords.rows`).
     */
    var guides = false

    override fun onCreate(savedInstanceState: Bundle?) {
```

Find:

```kotlin
        val ctrl = KeyEvent.META_CTRL_ON
```

Make it:

```kotlin
        // (The keys are suggested for the keyboard the dialog was asked from, where the system says which that is.)
        if (guides) data.add(SystemWords.rows(this, FirstRun.suggest(FirstRun.hasQuickInsert(Keyboards.attached(), deviceId))))
        val ctrl = KeyEvent.META_CTRL_ON
```

- [ ] **Step 5: A comment that named the old key, in `app/src/main/java/io/github/kuscher/booklight/window/Page.kt`.** One change.

Find:

```kotlin
/** A few key caps in a row, with plus signs between them: Action + K. */
```

Make it:

```kotlin
/** A few key caps in a row, with plus signs between them: Action + M. */
```

- [ ] **Step 6: The old way is gone, and the row writes through `Prefs.firstRun`**

```bash
grep -rn "win_key_or" app/src || echo GONE
grep -n '"Alt"' app/src/main/java/io/github/kuscher/booklight/window/Pages.kt || echo GONE
grep -c "app.prefs.firstRun { FirstRun.again(it, overture = true) }" app/src/main/java/io/github/kuscher/booklight/window/Pages.kt
grep -c "model.begin()\|\.begin(" app/src/main/java/io/github/kuscher/booklight/window/Pages.kt
```

Expected: `GONE`, `GONE`, `1`, `0`.

- [ ] **Step 7: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/res/values/strings_11.xml app/src/main/res/values-de/strings_11.xml app/src/main/java/io/github/kuscher/booklight/window/Pages.kt app/src/main/java/io/github/kuscher/booklight/window/MainActivity.kt app/src/main/java/io/github/kuscher/booklight/window/Page.kt
git commit -m "First run: \"First steps\" is a row on the window's Start page, and the page tells the way to a key as first run does: Action + Quick Insert, or Action + M"
```

### Task 8: The welcome and the show, mended: the ring, the words, the first rows, and Booklight's mark

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt`

**Interfaces:**
- Consumes: `Lights.MARK_AFTER_MS`, `Lights.DEEPENS_FROM` (Task 5), `Lights.STRIKE`, `FLOOD_AFTER_MS`, `FLOOD_MS`; in `Welcome.kt`: `NIGHT`, `AXIS`, `WIDE`, `HIGH`, `CARET`, `FLOOD`, `NEVER`, `tw(…)`, `soften(a, b, x)`, `lampAt(t)`, `SEAM_GROWS`, `private class Lit(val t: Float, val hand: Float, off: Float)` with `night`, `ambient`, `fades`, the welcome's frame loop with its `hand`; in `Rows.kt`: `private class Slot`, `RowSlots.sync(results: List<Result>, motion: Motion): List<Slot>`, `SlotRow(top, leaving, delayMs, onGone, wait, content)`, `ResultsBody`; in `Panel.kt`: the body's `AnimatedContent` and its `transitionSpec`; in `OverlayModel`: `playing`, `begin(at)`; in `Field.kt`: the mark's `AnimatedContent` (label `who`).
- Produces:
  - `OverlayModel.marked: Boolean` (state, set by the welcome, cleared by `begin`): the hand-over has come as far as the field's mark.
  - In `Rows.kt`: `Slot.whole`; `RowSlots.sync(results: List<Result>, motion: Motion, held: Boolean = false): List<Slot>`; `SlotRow(…, wait, whole: Boolean = false, content)`.
  - In `Welcome.kt`: `private const val DEEP`, `private val DEEPER: Brush`, `private fun floodAt(t: Float)`, `Lit.deep`.
  - On the glass: no ring as the night falls and lifts; no words under the field at the lamp's strike; the show's first rows in the frame the handle becomes the pill; Booklight's mark in the field 100 ms before its first letter.

Three things the device showed (`docs/research/first-run-welcome.md`), each mended where it is drawn, and one piece of the show's own motion that part 4 left.

1. **A faint large ring round the middle of the glass as the night lifts.** The corners' deeper shade was a radial gradient with nothing up to 38 % of its radius and the night's colour at its end: a gradient with a corner in it, whose corner shows as a ring once the glass under it is no longer dark. It is now a gradient with no corner anywhere (seventeen stops on the square of the distance), and it is there only above half night (`Lit.deep`), so it has gone before the theme's glass is back.
2. **The title and the line showed dimly at the lamp's strike.** Their dim light followed the lamp alone, which is at 60 % thirty ms after the strike. It now follows the lamp times how far the lamp's light has flooded the field (`floodAt`): nothing at the strike, there as the head lights up.
3. **The highlight stood empty in row one's seat for a frame or two.** The list came by `fadeIn(snap())`: a transition that cuts still needs a frame to begin, at alpha 0. The show's first list now comes with `EnterTransition.None`, and its first row does not rise: it is whole in its first frame (`Slot.whole`), inside the pill that the handle has just become. The other rows rise as always.
4. **Booklight's mark** takes the engine's place in the field 520 ms after the hand-over began, about 100 ms before the first letter (`motion.md` §2.2): it says who is about to type.

No engineer can see any of these. Use the code as written; the coordinator looks on the device (Task 15's checks 11 to 14).

- [ ] **Step 1: The night's shade, the words' light and the mark, in `app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt`.** Six changes, in the file's order: the shade without an edge; `floodAt`, which `frontAt` now uses; `Lit.deep`; `Lit.ambient`; the mark, in the frame loop; the shade drawn by `deep`. The last is a piece of one long line.

Find:

```kotlin
private const val PRESSED = 0.08f

private const val NEVER = Float.POSITIVE_INFINITY
```

Make it:

```kotlin
private const val PRESSED = 0.08f

/**
 * The night's deeper shade towards the corners: none in the middle of the glass, [DEEP] of the night's colour 520 dp
 * out. It grows with the square of the distance, in steps too fine to see, so it has no edge anywhere: a shade that
 * began at a ring 200 dp out showed that ring once the glass under it was no longer dark.
 */
private const val DEEP = 0.6f
private val DEEPER: Brush = Brush.radialGradient(*Array(17) { it / 16f to NIGHT.copy(alpha = DEEP * (it / 16f) * (it / 16f)) }, center = Offset(AXIS, 230f), radius = 520f)

private const val NEVER = Float.POSITIVE_INFINITY
```

Find:

```kotlin
/** Where the front of the head's light stands on its way from the caret to the field's right end. */
private fun frontAt(t: Float) = CARET + FLOOD * tw(t, (Lights.STRIKE + Lights.FLOOD_AFTER_MS).toFloat(), Lights.FLOOD_MS, SEAM_GROWS)
```

Make it:

```kotlin
/** How far the lamp's light has flooded the head, 0 to 1: from the caret to both ends. */
private fun floodAt(t: Float) = tw(t, (Lights.STRIKE + Lights.FLOOD_AFTER_MS).toFloat(), Lights.FLOOD_MS, SEAM_GROWS)
/** Where the front of the head's light stands on its way from the caret to the field's right end. */
private fun frontAt(t: Float) = CARET + FLOOD * floodAt(t)
```

Find:

```kotlin
    val lamp = if (dead) 0f else lampAt(t)
```

Make it:

```kotlin
    /** How much of the corners' deeper shade there is: none until the night has half fallen, all of it at full night, and gone again before the glass is the theme's own. */
    val deep = soften(Lights.DEEPENS_FROM, 1f, night)
    val lamp = if (dead) 0f else lampAt(t)
```

Find:

```kotlin
    /** The words in the lamp's room: there with the lamp, gone as the light leaves them. */
    val ambient: Float = (lampAt(minOf(t, off)).coerceAtMost(1f) * (1f - tw(minOf(t, off), hand + Lights.WORDS_OUT_AFTER_MS, Lights.WORDS_OUT_MS))) * fades
```

Make it:

```kotlin
    /**
     * The words in the lamp's room: they show dimly as its light floods the head (the lamp lights the room; at the strike
     * itself there is nothing under the field yet), and are gone as the light leaves them.
     */
    val ambient: Float = (lampAt(minOf(t, off)).coerceAtMost(1f) * floodAt(minOf(t, off)) * (1f - tw(minOf(t, off), hand + Lights.WORDS_OUT_AFTER_MS, Lights.WORDS_OUT_MS))) * fades
```

Find:

```kotlin
                model.cueUp = hand == NEVER && t >= Lights.HANDLE + Lights.HANDLE_MS
```

Make it:

```kotlin
                model.cueUp = hand == NEVER && t >= Lights.HANDLE + Lights.HANDLE_MS
                // Booklight's mark comes up in the field's seat as the head's light closes past it, before the first letter.
                model.marked = hand != NEVER && t >= hand + Lights.MARK_AFTER_MS
```

Find, in one long line:

```kotlin
night > 0f) drawRect(Brush.radialGradient(0f to NIGHT.copy(alpha = 0f), 0.38f to NIGHT.copy(alpha = 0f), 1f to NIGHT.copy(alpha = 0.6f), center = Offset(AXIS, 230f), radius = 520f), Offset.Zero, Size(WIDE, HIGH), alpha = f.night
```

Make it:

```kotlin
deep > 0f) drawRect(DEEPER, Offset.Zero, Size(WIDE, HIGH), alpha = f.deep
```

- [ ] **Step 2: A first row that is whole at once, in `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`.** Seven changes: `Slot.whole`; `sync` with `held`; the body says when the highlight is held; `SlotRow` with `whole`, and the row handed it.

Find:

```kotlin
import io.github.kuscher.booklight.core.Icon as RowIcon
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.core.Icon as RowIcon
```

Find:

```kotlin
    var delay = 0
```

Make it:

```kotlin
    var delay = 0
    /** It stands whole from its first frame and does not rise: row one of first run's show, which comes into the seat the highlight already has. */
    var whole = false
```

Find:

```kotlin
    fun sync(results: List<Result>, motion: Motion): List<Slot> {
```

Make it:

```kotlin
    /** [held]: the highlight stands in row one's seat already (first run's show, whose pill is the welcome's handle): a list that comes from nothing has that row whole at once. */
    fun sync(results: List<Result>, motion: Motion, held: Boolean = false): List<Slot> {
```

Find:

```kotlin
            val s = live[seat(r)] ?: Slot(next++, r).also { it.delay = motion.stagger(if (fromNothing) i else fresh++) }
```

Make it:

```kotlin
            val s = live[seat(r)] ?: Slot(next++, r).also { it.delay = motion.stagger(if (fromNothing) i else fresh++); it.whole = held && fromNothing && i == 0 }
```

Find:

```kotlin
    val rows = slots.sync(model.results, motion)
```

Make it:

```kotlin
    val rows = slots.sync(model.results, motion, held = model.playing == FirstRun.Playing.SHOW)
```

Find:

```kotlin
            SlotRow(slot.top, slot.leaving, if (under) 0 else slot.delay, onGone = { slots.remove(slot) }, wait = {
```

Make it:

```kotlin
            SlotRow(slot.top, slot.leaving, if (under) 0 else slot.delay, whole = slot.whole, onGone = { slots.remove(slot) }, wait = {
```

Find:

```kotlin
private fun SlotRow(top: Dp, leaving: Boolean, delayMs: Int, onGone: () -> Unit, wait: () -> Boolean, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    val y by animateDpAsState(top, motion.place(), label = "row")
    val here = remember { Animatable(if (motion.on) 0f else 1f) }
```

Make it:

```kotlin
private fun SlotRow(top: Dp, leaving: Boolean, delayMs: Int, onGone: () -> Unit, wait: () -> Boolean, /** It does not arrive: it is there, whole, from its first frame. */ whole: Boolean = false, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    val y by animateDpAsState(top, motion.place(), label = "row")
    val here = remember { Animatable(if (motion.on && !whole) 0f else 1f) }
```

- [ ] **Step 3: The show's first list comes with no transition, in `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** Three changes: an import comes, one goes; the body's `transitionSpec`.

Find:

```kotlin
import androidx.compose.animation.core.Animatable
```

Make it:

```kotlin
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
```

Find:

```kotlin
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
```

Make it:

```kotlin
import androidx.compose.animation.core.animateDpAsState
```

Find:

```kotlin
                            // (First run's show begins with the welcome's handle in row one's seat: its list comes at once, the pill
                            // in that seat from its first frame, and only the rows rise. Nothing fades in over the handle.)
                            transitionSpec = { ((if (model.playing == FirstRun.Playing.SHOW && initialState == "none") fadeIn(snap()) else fadeIn(motion.fade(140))) togetherWith fadeOut(motion.fade(70))).using(null) },
```

Make it:

```kotlin
                            // (First run's show begins with the welcome's handle in row one's seat: its list is there in the very frame
                            // the handle is drawn no more, the pill in that seat with row one in it, and the other rows rise. It comes
                            // with no transition at all: even one that cuts needs a frame to begin, and in that frame the seat stood
                            // empty, or without a highlight.)
                            transitionSpec = { ((if (model.playing == FirstRun.Playing.SHOW && initialState == "none") EnterTransition.None else fadeIn(motion.fade(140))) togetherWith fadeOut(motion.fade(70))).using(null) },
```

- [ ] **Step 4: `marked`, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Two changes: the state; `begin` clears it.

Find:

```kotlin
    /** How often the show has asked for the light to set off round the outline: the panel runs one lap for each. */
```

Make it:

```kotlin
    /** The welcome's hand-over has come as far as the field's mark: Booklight's own stands in its seat, a moment before Booklight types. Set by the welcome. */
    var marked by mutableStateOf(false)
    /** How often the show has asked for the light to set off round the outline: the panel runs one lap for each. */
```

Find:

```kotlin
        began = true; playing = FirstRun.Playing.WELCOME; cued = false; cueUp = false; standsAt = at; unseen = false; heldHeight = null
```

Make it:

```kotlin
        began = true; playing = FirstRun.Playing.WELCOME; cued = false; cueUp = false; marked = false; standsAt = at; unseen = false; heldHeight = null
```

- [ ] **Step 5: The field's mark, in `app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt`.** Two changes: the comment, and whose mark stands; the second is a piece of one long line.

Find:

```kotlin
                // Booklight's own while Booklight types, which says who is typing; the engine's again as the key's step lands.
```

Make it:

```kotlin
                // Booklight's own while Booklight types, which says who is typing, and from the moment the welcome's light has
                // closed past its seat, just before the first letter; the engine's again as the key's step lands.
```

Find, in one long line:

```kotlin
else AnimatedContent(model.playing?.takeIf
```

Make it:

```kotlin
else AnimatedContent(if (model.playing == FirstRun.Playing.WELCOME && model.marked) FirstRun.Playing.SHOW else model.playing?.takeIf
```

- [ ] **Step 6: The old drawing is gone**

```bash
grep -n "fadeIn(snap())" app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt || echo GONE
grep -n "0.38f to NIGHT" app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt || echo GONE
grep -c "floodAt(" app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt
```

Expected: `GONE`, `GONE`, `3` (its definition, the field's front, the words' light).

- [ ] **Step 7: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt
git commit -m "First run: what the device said of the welcome: no ring as the night lifts, no words before the lamp's light reaches them, no empty highlight before the show's first rows; and Booklight's mark comes up before it types"
```

### Task 9: The opening piece ends in one place, and a run asked for again is whole

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/data/Prefs.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt`

**Interfaces:**
- Consumes: core `FirstRun.State.sums`, `FirstRun.Cast`, `FirstRun.pressed(playing, press, cast)`, `FirstRun.handOver(cast, waited)`, `Show.script(sum: String?, keyword, comma)` (Task 1); `Settings.showSums`; in `OverlayModel`: `cast`, `casting` (the job that reads it), `performer`, `playing`, `cued`, `cueUp`, `marked` (Task 8), `standsAt`, `heldHeight`, `glideFrom`, `clear()`, `begin(at)`, `press(press, again)`, `private fun typed(was: FirstRun.Playing)`, `show(only)`, `land()`, `closing()`, `highlight()`, `hold(ms)`; in `Welcome.kt`: `Glide(model)` with its `from`, the welcome's frame loop with `hand`, `late`, `Lights.HAND`, `Lights.WAITS_MS`.
- Produces:
  - `Settings.firstRun()` hands core whether sums are answered (`sums = showSums`).
  - `OverlayModel.casts: FirstRun.Cast`: the show's rows are there, still being worked out, or this device has none.
  - `private fun OverlayModel.stop(): FirstRun.Playing?`: the one place the piece stops performing; answers where it was. `typed()` takes no argument now. `begin`, `typed`, `land` and `closing` all end through it.
  - `land()` keeps the glass's height also where the welcome is put out while its glass is tall.
  - The show plays no sum where "Show sums" is off; the cue with nothing to show lands the key's step; the highlight's travel into the key's step is forgotten once a letter was typed over it.

**One ending.** The piece ended in four places (a typed character, the landing, a panel that closes, a hook that begins it anew), each with its own copy of the same lines, and they had drifted: one cleared the travel's start, one did not. "First steps" brings more panels that begin with the piece. `stop()` is now the one place: Booklight's hand comes off the field, nothing of the welcome's is left to press, and what the show had put into the field and the list goes in one change. What follows is each caller's own.

**Whole on an installation that has been lived in.** The run's state now says whether sums are answered, so core leaves the sum's lesson out where they are not (Task 1), and the show is scripted without its sum. Where the device has nothing to show, the cue and the welcome's own hand-over ask core (`pressed` with `casts`, `handOver`): the key's step lands at once, and no Enter does nothing.

**Two small things from part 4's ledger.** A welcome put out while its glass is 468 dp high (Esc in the welcome) went to 252 dp with no hold, and the lower edge cut through its words: `land()` now keeps the height for those 80 ms, as it does after the show. And a travel of the highlight that was cut by a typed letter went on from where it was if the field was emptied within 400 ms: `Glide` forgets it.

- [ ] **Step 1: Whether sums are answered, in `app/src/main/java/io/github/kuscher/booklight/data/Prefs.kt`.** One change: `Settings.firstRun()`.

Find:

```kotlin
/** What first run needs of the settings, as the core's own state. That step 1 is over is read from the list the mark is in. */
fun Settings.firstRun(): FirstRun.State = FirstRun.State(FirstRun.Run.of(first), firstDone, firstOpens, firstHelper, firstIcon, keySeen, suggestions, FirstRun.MARK in tipsSeen)
```

Make it:

```kotlin
/**
 * What first run needs of the settings, as the core's own state. That step 1 is over is read from the list the mark is in.
 * Whether sums are answered ("Show sums") is only read: where they are switched off the sum's lesson is no step.
 */
fun Settings.firstRun(): FirstRun.State = FirstRun.State(FirstRun.Run.of(first), firstDone, firstOpens, firstHelper, firstIcon, keySeen, suggestions, FirstRun.MARK in tipsSeen, sums = showSums)
```

- [ ] **Step 2: One ending, and what there is to show, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Nine changes, in the file's order: `casts`; `begin` ends what played through `stop`; `stop` itself; `press` asks with `casts`, and a typed character ends through `typed()`; `typed`; the show's script; `land`; `closing`.

Find:

```kotlin
    private var performer: Job? = null
```

Make it:

```kotlin
    /** Whether the show has something to show here (core `FirstRun.Cast`): its rows are there, still being worked out, or this device has none. */
    val casts: FirstRun.Cast get() = if (cast != null) FirstRun.Cast.READY else if (casting?.isCompleted == true) FirstRun.Cast.NONE else FirstRun.Cast.WAITS
    private var performer: Job? = null
```

Find:

```kotlin
        performer?.cancel(); performer = null
        // (A search still on its way must not land among the show's rows: the show types the same words a user might have.)
        job?.cancel()
        if (playing == FirstRun.Playing.SHOW) clear()
        began = true; playing = FirstRun.Playing.WELCOME; cued = false; cueUp = false; marked = false; standsAt = at; unseen = false; heldHeight = null
        rounds++
        glideFrom = null
```

Make it:

```kotlin
        // (A search still on its way must not land among the show's rows: the show types the same words a user might have.)
        job?.cancel()
        stop()
        began = true; playing = FirstRun.Playing.WELCOME; standsAt = at; unseen = false
        rounds++
```

Find:

```kotlin
    private fun shown() { if (first().run != FirstRun.Run.NONE) step { FirstRun.shown(it) } }

    /** What was Booklight's goes from the field and from the list, in one change. */
```

Make it:

```kotlin
    private fun shown() { if (first().run != FirstRun.Run.NONE) step { FirstRun.shown(it) } }

    /**
     * The piece stops performing: the one place for every way it ends or begins anew (a typed character, the landing, a
     * panel that closes, a hook that plays it again). Booklight's hand is off the field, nothing of the welcome's is
     * left to press, and what the show had put into the field and the list goes in one change, so that none of it is
     * taken for the user's. Answers where the piece was. What follows is the caller's: where [playing] goes, and when the
     * screen that stands came.
     */
    private fun stop(): FirstRun.Playing? {
        val was = playing
        performer?.cancel(); performer = null
        cued = false; cueUp = false; marked = false; standsAt = null; heldHeight = null; glideFrom = null
        if (was == FirstRun.Playing.SHOW) clear()
        return was
    }

    /** What was Booklight's goes from the field and from the list, in one change. */
```

Find:

```kotlin
        when (FirstRun.pressed(on, press)) {
```

Make it:

```kotlin
        // (Where this device has nothing to show, the cue has no show to begin: it sets the key's step down.)
        when (FirstRun.pressed(on, press, casts)) {
```

Find:

```kotlin
            FirstRun.Ends.TYPE -> { typed(on); return false }
```

Make it:

```kotlin
            FirstRun.Ends.TYPE -> { typed(); return false }
```

Find:

```kotlin
    private fun typed(was: FirstRun.Playing) {
        performer?.cancel(); performer = null
        playing = null; cued = false; cueUp = false; standsAt = null; heldHeight = null; glideFrom = null
        if (was == FirstRun.Playing.SHOW) clear()
```

Make it:

```kotlin
    private fun typed() {
        stop()
        playing = null
```

Find:

```kotlin
        val script = Show.script(app.getString(R.string.first_sum_example), c.keyword, c.comma)
```

Make it:

```kotlin
        // (The show shows nothing this installation has switched off: with "Show sums" off the sum's beat is left out.)
        val script = Show.script(app.getString(R.string.first_sum_example).takeIf { settings.showSums }, c.keyword, c.comma)
```

Find:

```kotlin
        performer?.cancel(); performer = null
        // The lower edge waits while what was Booklight's fades where it stands: it cuts no row and no cell.
        if (results.isNotEmpty()) { heldHeight = Metrics.height(this); scope.launch { delay(hold(Motion.LANDS_AFTER_MS)); heldHeight = null } }
        // The one highlight goes on: from the grid's square, or from the list's pill, into the armed answer of the screen that
        // lands. Where that screen arms none, or nothing was highlighted (the welcome), none travels.
        answerAt = null
        glideFrom = highlight()?.takeIf { due != null && FirstRun.armed(first()) >= 0 }
        clear()
        playing = FirstRun.Playing.LANDING; cued = false; cueUp = false; standsAt = null; unseen = false
```

Make it:

```kotlin
        // The lower edge waits while what was Booklight's fades where it stands: it cuts no row and no cell, and nothing of
        // a welcome that is put out while its glass is tall.
        val held = if (results.isNotEmpty() || was == FirstRun.Playing.WELCOME) Metrics.height(this) else null
        // The one highlight goes on: from the grid's square, or from the list's pill, into the armed answer of the screen that
        // lands. Where that screen arms none, or nothing was highlighted (the welcome), none travels.
        val from = highlight()?.takeIf { due != null && FirstRun.armed(first()) >= 0 }
        // (After a show `stop` has taken Booklight's text and rows away; under a welcome the field holds nothing of anyone's.)
        if (stop() != FirstRun.Playing.SHOW) clear()
        if (held != null) { heldHeight = held; scope.launch { delay(hold(Motion.LANDS_AFTER_MS)); heldHeight = null } }
        answerAt = null
        glideFrom = from
        playing = FirstRun.Playing.LANDING; unseen = false
```

Find:

```kotlin
        playing?.let { was ->
            performer?.cancel(); performer = null
            playing = null; cued = false; cueUp = false; standsAt = null; heldHeight = null; glideFrom = null
            if (was == FirstRun.Playing.SHOW) clear()
```

Make it:

```kotlin
        if (stop() != null) {
            playing = null
```

- [ ] **Step 3: The hand-over asks core, and a travel that was typed over is forgotten, in `app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt`.** Two changes: `Glide`; the welcome's frame loop.

Find:

```kotlin
    // (A typed character takes the stage away under it: then it is simply gone.)
    val start = from?.takeIf { model.stage != null } ?: return
```

Make it:

```kotlin
    // (A typed character takes the stage away under it: then it is simply gone, and it does not go on from where it was
    // should the field be emptied before it would have arrived.)
    val typedOver = model.stage == null
    LaunchedEffect(typedOver) { if (typedOver) from = null }
    val start = from?.takeIf { !typedOver } ?: return
```

Find:

```kotlin
                    val ready = model.cast != null
                    when {
                        stands != null -> if (t >= Lights.HAND) hand = Lights.HAND.toFloat()
                        // The cue: the user's Enter, or a click on the handle, begins the hand-over in this frame, from wherever the parts are.
                        model.cued && ready -> hand = t
                        // Or Booklight presses the handle itself. If this device's apps are not ready when that is due, the welcome
                        // stands on; after one more second the key's step lands, without a show.
                        t >= Lights.HAND -> if (ready) hand = if (late) t else Lights.HAND.toFloat() else { late = true; if (t >= Lights.HAND + Lights.WAITS_MS) model.land() }
```

Make it:

```kotlin
                    when {
                        stands != null -> if (t >= Lights.HAND) hand = Lights.HAND.toFloat()
                        // The cue (the user's Enter, a click on the handle) begins the hand-over in this frame, from wherever the
                        // parts are; or Booklight presses the handle itself, once the welcome has stood. What then happens is core's
                        // to say (`FirstRun.handOver`): with a show it begins; where this device has nothing to show the key's step
                        // lands at once; while its apps are still being read the welcome stands on, a second at most.
                        model.cued || t >= Lights.HAND -> when (FirstRun.handOver(model.casts, waited = t >= Lights.HAND + Lights.WAITS_MS)) {
                            FirstRun.Ends.BEGIN -> hand = if (model.cued || late) t else Lights.HAND.toFloat()
                            FirstRun.Ends.LAND -> model.land()
                            else -> if (t >= Lights.HAND) late = true
                        }
```

- [ ] **Step 4: The piece ends in one place**

```bash
grep -c "performer?.cancel(); performer = null" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
grep -c "stop()" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
grep -n "model.cast != null" app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt || echo GONE
```

Expected: `1` (in `stop` alone), `5` (its definition and its four callers: `begin`, `typed`, `land`, `closing`), `GONE`.

- [ ] **Step 5: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/data/Prefs.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt
git commit -m "First run: the opening piece ends in one place, shows nothing that is switched off, and lands at once where it has nothing to show"
```

### Task 10: The stage is set down: every part by its mark, on one clock

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Strip.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`

**Interfaces:**
- Consumes: core `SetDown.Comes`, `SetDown.Marks`, `SetDown.WHOLE`, `SetDown.THERE`, `SetDown.marks(…)`, `SetDown.piece(m, k)`, `SetDown.letter(m, pace, k, i)`, `SetDown.sameBand`, `SetDown.sameCaption` (Task 4); `FirstRun.inView(press, opened, came, lead)`, `FirstRun.SEEN_MS`, `FirstRun.comesBack(s)` (Task 3); `FirstRun.screen(s)`, `FirstRun.answers(s)`, `FirstRun.lesson(s)`, `FirstRun.recipe(s, example, sum)`, `FirstRun.example(…)`, `FirstRun.Part` (`Typed`, `Key`); `PACE`, `PLACE`, `POP`, `Motion.held(ms)`, `Motion.RISE_MS`, `CAP_MS`, `CAP_SMALL`, `PART_MS`, `PRESS_MS`, `BAND_AFTER_MS`, `GATE_TO_REST_MS` (Task 5). In `OverlayModel`: `first()`, `stage`, `due`, `arrived`, `arrivedAt`, `suggested`, `example`, `exemplify()`, `private var cameAt`, `private fun seen(made: Long): Boolean`, `seen`, `unseen`, the private `rearm` with `armedFor`, `again()` (Task 6), `land()`, `closing()` (Task 9), `turned()`, `type(…)`, the companion's `SEEN_MS`. In `Strip.kt`: `OptionStrip(…)`. In `Panel.kt`: `data class Arrival(val unfold: Boolean, val slow: Float)`, the body's `first` branch. In `OverlayActivity`: `motion`, `arrival`.
- Produces, in `OverlayModel`:
  - `var comes: SetDown.Comes` and `var marks: SetDown.Marks` (state, set privately): how the screen that stands came, and when each of its parts comes.
  - `var cameAt: Long` (state, set privately; it was a private field): when the screen that stands came. `val viewFrom: Long`: the zero of the stage's clock (`maxOf(arrivedAt, cameAt)`).
  - `var lasts: (Long) -> Long`: how long a span of motion really is; the activity sets it to `motion::held`.
  - `var stageAt: Float?` (state): debug builds stand the stage's clock still here.
  - `fun recipe(f: FirstRun.State = first()): List<FirstRun.Part>`: the recipe of the lesson that stands, on this device.
  - `fun opening(bandAfter: Int)`: the activity says how long after the gate a band begins that is set down as this panel opens.
  - `private fun came(how: SetDown.Comes, was: Pair<FirstRun.Screen?, List<FirstRun.Answer>>? = null)`: the screen that stands came in this moment, in this way. Every place that set `cameAt` calls it.
  - `seen(made)` counts by core's `inView` with the marks' lead. The companion's `SEEN_MS` is gone (core's `FirstRun.SEEN_MS`).
- Produces elsewhere:
  - `OptionStrip(…, shown: (Int) -> Float = { 1f })`: how far each option is there, read as it is drawn.
  - `Arrival.bandAfter: Int`.
  - In `FirstStage.kt` (all private but `FirstStage` itself, whose signature stays `FirstStage(model: OverlayModel, on: FirstRun.Screen, onAnswer: (FirstRun.Answer) -> Unit)`): `stageClock(model): () -> Float`; `come`, `below`; `Modifier.rises(now, at, rise)`, `Modifier.fadesIn(now, at, ms)`, `Modifier.comesUp(now, at)`; `class Own(val now: () -> Float, val marks: SetDown.Marks)` and `own(now, marks, stands)`; `late(value, ms)`; `data class Under`, `data class Answers`, `sealed interface Picture { Caps, Yours, Recipe }`; `Seat(symbol, title, line, counter, now, marks, turn, rise)`; `Band(model, under, armed, o, answers)`; `BoxWithConstraintsScope.Keys(model, under, o)` (it was `KeyCaps`); `Written(text, now, at, written, ink)`; `AskBand(under, more, armed, o, rise, answers)`. The old `Recipe` composable is gone.

This is the part's centre: `motion.md` §3's transitions T1, T5, T7 (the stage's side), T8 and T9 (the stage's side), and the landing of the opening piece, in one mechanism.

**How it works.** The model works a screen's marks out once, in the moment it comes (`came`): which way it comes is known there (a panel opens on it: set down, or come back to; it takes the place of a screen that stood: a turn; it follows a list; the opening piece lands on it; or it stands whole), and core's `SetDown.marks` says when each part comes that way. The stage has one clock (`stageClock`): ms since the screen came into view, as slow as a debug build asks, standing at its end with the system's animations off. Every part is drawn by a graphics layer from that clock and its own mark. Nothing is kept from frame to frame, so a still at any moment is whole and a hook can stand the clock anywhere (Task 15).

**What must not break, and how the code keeps it.**
- *A key counts only once the answers are in view.* `seen(made)` is `FirstRun.inView(press, arrivedAt, cameAt, lasts(marks.lead))`. A screen that is set down counts later than one that stands whole, never sooner. A letter typed while a screen is set down makes it whole (what has not come never comes) and, where its answers had not been in view, it is `unseen`: it comes into view when the field is empty again.
- *Sizes are known before anything moves.* Every part is laid out at its final place from the first frame; `rises`, `fadesIn` and `comesUp` only draw. The highlight that travels from the grid into the armed answer (part 4's `Glide`) reads that answer's place from the layout once: at a landing the answers therefore do not rise (`Marks.rises`), they fade in where they stand.
- *What gives way fades as it stood.* Inside an `AnimatedContent` the content that leaves is drawn by the clock and marks of its last moment (`own`), not by those of the screen that takes its place. And where the model has no stage any more (a letter typed over it, the choices in the question's place), `Panel` draws the one that stood while it fades.
- *One clock, read where things are drawn.* The clock is a state read inside `graphicsLayer` and `drawBehind` blocks only: a frame of it composes nothing.
- *Animations off.* The clock is at rest in the first frame, `late` hands its value through, and `lasts` is 0: everything stands, and a key counts 350 ms after the screen came.

**A turn** (one screen in another's place: the key's four screens, Skip from lesson to lesson, the question after the sum). The seat stays: its title, its line and its counter roll one after the other (`late`, 22 ms apart). The band's old pieces fade in 80 ms and the new ones are written from 120 ms. What the two screens share does not move at all: "your key" on the key's later screens (`sameBand`), the caption lessons 2 and 3 share (`sameCaption`), answers that are the same.

No engineer can see any of this. Use the code as written; Task 15's checks 16 to 26 and 32 are how the coordinator looks.

- [ ] **Step 1: How a screen comes, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Fifteen changes, in the file's order: an import; the new section (`comes`, `marks`, `cameAt`, `viewFrom`, `bandAfter`, `lasts`, `stageAt`); `recipe`, `came` and `opening`; the model's making; `rearm`; the old `cameAt` goes; `seen`; `again`; the three places of the piece and `turned`; a lesson's list that gave way; a letter typed during a set-down; the field emptied again; the companion.

Find:

```kotlin
import io.github.kuscher.booklight.core.Show
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.SetDown
import io.github.kuscher.booklight.core.Show
```

Find:

```kotlin
    val awaitsKey: Boolean get() = stage.let { it == FirstRun.Screen.K1 || it == FirstRun.Screen.K2 || it == FirstRun.Screen.K3 }

    /**
     * What the recipes of lessons 2 and 3 show to type on this device (core `FirstRun.Example`); null until it is worked
```

Make it:

```kotlin
    val awaitsKey: Boolean get() = stage.let { it == FirstRun.Screen.K1 || it == FirstRun.Screen.K2 || it == FirstRun.Screen.K3 }

    // ---- first run: how the screen that stands comes onto the glass

    /**
     * How the screen that stands came (core `SetDown.Comes`: set down part by part, come back to, in another's place,
     * after a list, at the landing of the opening piece, or whole at once), and when each of its parts comes by that
     * ([marks], core `SetDown.marks` with `Motion.kt`'s times). Worked out once, in the moment it comes ([came]). The
     * stage draws by these marks on one clock; [seen] counts by them.
     */
    var comes by mutableStateOf(SetDown.Comes.WHOLE); private set
    var marks by mutableStateOf(SetDown.WHOLE); private set
    /** When the screen of first run that stands came (the model's making, for the one it was made with), by the clock keys are timed by. */
    var cameAt by mutableLongStateOf(SystemClock.uptimeMillis()); private set
    /** The zero of the stage's clock: when the screen came, and not before the glass had opened far enough for it to be seen. */
    val viewFrom: Long get() = maxOf(arrivedAt, cameAt)
    /** How long after the gate the band of a screen begins that is set down as this panel opens (`Arrival.bandAfter`): said by the activity ([opening]). */
    private var bandAfter = Motion.BAND_AFTER_MS
    /** How long a span of motion really is (`Motion.held`: a debug build's slow motion stretches it, and with the system's animations off it is nothing). Set by the activity. */
    var lasts: (Long) -> Long = { it }
    /** Debug builds: the stage's clock stands at this many ms (`./bl debug first stage at MS`); null: it runs. */
    var stageAt by mutableStateOf<Float?>(null)

    /**
     * What the recipes of lessons 2 and 3 show to type on this device (core `FirstRun.Example`); null until it is worked
```

Find:

```kotlin
    private var exampling: Job? = null

    /** The example is worked out, once for this panel, off the main thread (`BooklightApp.firstExample`). */
```

Make it:

```kotlin
    private var exampling: Job? = null

    /** The recipe of the lesson that stands, on this device (core `FirstRun.recipe`). Lessons 2 and 3 have none until their example is worked out. */
    fun recipe(f: FirstRun.State = first()): List<FirstRun.Part> =
        if (example == null && FirstRun.lesson(f) != FirstRun.Screen.L4) emptyList()
        else FirstRun.recipe(f, example ?: FirstRun.example(null, "", null, "", ""), app.getString(R.string.first_sum_example))

    /**
     * The screen that stands came in this moment, in the way [how] says. Its marks are worked out (for a screen that
     * takes the place of [was], the screen and the answers that stood: what they share with it does not move), and its
     * keys count from when its answers have begun to show ([seen]).
     */
    private fun came(how: SetDown.Comes, was: Pair<FirstRun.Screen?, List<FirstRun.Answer>>? = null) {
        val f = first()
        val on = FirstRun.screen(f)
        // The band's pieces as core's schedule counts them: how many letters each typed part has, 0 for a cap or a sign.
        val letters = when (on?.step) {
            FirstRun.Step.KEY -> if (on == FirstRun.Screen.K1) listOf(0, 0, 0) else listOf(0)
            FirstRun.Step.OPEN, FirstRun.Step.SEARCH, FirstRun.Step.SUM -> recipe(f).map { (it as? FirstRun.Part.Typed)?.text?.length ?: 0 }
            else -> emptyList()
        }
        comes = how
        // (The choices are a list of their own: nothing of a stage comes for them.)
        marks = if (on == null || on == FirstRun.Screen.C) SetDown.WHOLE else SetDown.marks(
            how, PACE, letters, ask = on == FirstRun.Screen.Q,
            // (Under "Your key works" and under the question's answers stands no caption.)
            caption = on != FirstRun.Screen.K4 && on != FirstRun.Screen.Q, bandAfter = bandAfter,
            bandChanges = was == null || !SetDown.sameBand(was.first, on), captionChanges = was == null || !SetDown.sameCaption(was.first, on, suggested),
            answersChange = was == null || was.second != FirstRun.answers(f),
        )
        cameAt = SystemClock.uptimeMillis()
    }

    /** The activity says how this panel opens: how long after the gate the band of a screen begins that is set down in it. Said before the glass opens. */
    fun opening(bandAfter: Int) {
        this.bandAfter = bandAfter
        if (comes == SetDown.Comes.SET_DOWN) came(SetDown.Comes.SET_DOWN)
    }

    /** The example is worked out, once for this panel, off the main thread (`BooklightApp.firstExample`). */
```

Find:

```kotlin
        if (!demo && due != null) exemplify()
```

Make it:

```kotlin
        // (A screen the panel opens on is set down at its gate part by part; one the run has stood on before comes back as rows do.)
        if (!demo && due != null) { exemplify(); came(if (FirstRun.comesBack(first())) SetDown.Comes.BACK else SetDown.Comes.SET_DOWN) }
```

Find:

```kotlin
        if (on != armedFor) { armedFor = on; stageArmed = FirstRun.armed(first); cameAt = SystemClock.uptimeMillis() }
    }

    /** When the screen of first run that stands came (the model's making, for the one it was made with). */
    private var cameAt = SystemClock.uptimeMillis()

    /**
     * The screen that stands has been in view for a moment: the glass is open, and neither it nor the screen came just
     * now. A screen's keys and clicks count from then, each screen for itself: the Enter that skipped a lesson, pressed
     * again at once, must not answer the question that took the lesson's place, and nothing is answered on a screen that
     * is still under the lower edge of a glass that grows.
```

Make it:

```kotlin
        if (on != armedFor) {
            val was = armedFor
            armedFor = on; stageArmed = FirstRun.armed(first)
            // It takes the place of a screen that stood on the glass: its words roll and its band is written where that one's
            // was. Where none stood there (a list is typed; the choices; nothing), it stands whole whenever it shows.
            val turns = stage != null && was.first != null && was.first != FirstRun.Screen.C
            came(if (turns) SetDown.Comes.TURN else SetDown.Comes.WHOLE, was)
        }
    }

    /**
     * The screen that stands has been in view for a moment (core `FirstRun.inView`): the glass is open, and neither it
     * nor the screen came just now, and where the screen is set down part by part its answers have begun to show
     * ([marks]). A screen's keys and clicks count from then, each screen for itself: the Enter that skipped a lesson,
     * pressed again at once, must not answer the question that took the lesson's place, and nothing is answered on a
     * screen that is still under the lower edge of a glass that grows, or whose answers are still to come.
```

Find:

```kotlin
    private fun seen(made: Long): Boolean = arrived && (if (made > 0L) made else SystemClock.uptimeMillis()) - maxOf(arrivedAt, cameAt) >= SEEN_MS
```

Make it:

```kotlin
    private fun seen(made: Long): Boolean = arrived && FirstRun.inView(if (made > 0L) made else SystemClock.uptimeMillis(), arrivedAt, cameAt, lasts(marks.lead.toLong()))
```

Find:

```kotlin
        empty()
        cameAt = SystemClock.uptimeMillis()
```

Make it:

```kotlin
        // (It follows a list: it is set down once the rows have begun to fade.)
        came(SetDown.Comes.AFTER_LIST)
        empty()
```

Find:

```kotlin
        unseen = true
        cameAt = SystemClock.uptimeMillis()
```

Make it:

```kotlin
        unseen = true
        came(SetDown.Comes.WHOLE)
```

Find:

```kotlin
        cameAt = SystemClock.uptimeMillis()
        shown()
        scope.launch { delay(SEEN_MS); if (playing == FirstRun.Playing.LANDING) playing = null }
```

Make it:

```kotlin
        came(SetDown.Comes.LANDS)
        shown()
        // (The landing lasts until the key's step is in view: until then every press but a typed character is the piece's, and used up.)
        scope.launch { delay(lasts(marks.lead.toLong()) + FirstRun.SEEN_MS); if (playing == FirstRun.Playing.LANDING) playing = null }
```

Find:

```kotlin
            cameAt = SystemClock.uptimeMillis()
```

Make it:

```kotlin
            came(SetDown.Comes.WHOLE)
```

Find:

```kotlin
    fun turned() { cameAt = SystemClock.uptimeMillis() }
```

Make it:

```kotlin
    fun turned() { came(SetDown.Comes.WHOLE) }
```

Find:

```kotlin
        flash = null
```

Make it:

```kotlin
        flash = null
        // What stands next follows this list: it is set down once the rows have begun to fade.
        came(SetDown.Comes.AFTER_LIST)
```

Find:

```kotlin
        if (text == query) return
```

Make it:

```kotlin
        if (text == query) return
        // A letter typed while a screen of first run is being set down: what of it has not come never comes. When the field
        // is empty again the screen stands whole, and nothing is written twice. Where its answers had not been in view yet,
        // it comes into view only then, and takes a key a moment after that.
        if (stage != null && comes != SetDown.Comes.WHOLE) {
            if (!seen) unseen = true
            comes = SetDown.Comes.WHOLE; marks = SetDown.WHOLE
        }
```

Find:

```kotlin
            if (unseen && text.isEmpty()) { unseen = false; cameAt = SystemClock.uptimeMillis() }
```

Make it:

```kotlin
            if (unseen && text.isEmpty()) { unseen = false; came(SetDown.Comes.WHOLE) }
```

Find:

```kotlin
        /** How long a screen of first run is in view before its keys and clicks count ([seen]). */
        private const val SEEN_MS = 350L
```

Make it:

```kotlin

```

- [ ] **Step 2: Answers that come a moment after their band, in `app/src/main/java/io/github/kuscher/booklight/overlay/Strip.kt`.** Four changes in `OptionStrip`: the parameter `shown`; the highlight, the labels and the marks drawn as far as their option is there.

Find:

```kotlin
    lit: Boolean = true,
```

Make it:

```kotlin
    lit: Boolean = true,
    /** How far each option is there, 0 to 1, read as it is drawn: first run's stage sets its answers down a moment after its band. The highlight is as far there as the option it is on. Nothing moves by it. */
    shown: (Int) -> Float = { 1f },
```

Find:

```kotlin
                if (n == 0 || glow <= 0f) return@drawBehind
```

Make it:

```kotlin
                val there = if (n == 0) 0f else glow * shown(chosen.coerceIn(0, n - 1))
                if (there <= 0f) return@drawBehind
```

Find:

```kotlin
                if (hair.alpha > 0f) drawRoundRect(hair.copy(alpha = hair.alpha * glow), o - Offset(0.5f, 0.5f), Size(s.width + 1f, s.height + 1f), CornerRadius(r.x + 0.5f), style = Stroke(1f))
                drawRoundRect(fill.copy(alpha = fill.alpha * glow), o, s, r)
                drawRoundRect(rim.copy(alpha = rim.alpha * glow), o + Offset(0.5f, 0.5f), Size(s.width - 1f, s.height - 1f), r, style = Stroke(1f))
```

Make it:

```kotlin
                if (hair.alpha > 0f) drawRoundRect(hair.copy(alpha = hair.alpha * there), o - Offset(0.5f, 0.5f), Size(s.width + 1f, s.height + 1f), CornerRadius(r.x + 0.5f), style = Stroke(1f))
                drawRoundRect(fill.copy(alpha = fill.alpha * there), o, s, r)
                drawRoundRect(rim.copy(alpha = rim.alpha * there), o + Offset(0.5f, 0.5f), Size(s.width - 1f, s.height - 1f), r, style = Stroke(1f))
```

Find:

```kotlin
                    modifier = Modifier.graphicsLayer { alpha = SECOND + (1f - SECOND) * on(a.value, k) * glow })
                Icon(mark, null, Modifier.size(14.dp).graphicsLayer { alpha = on(a.value, k) * glow }, tint = ink ?: scheme.onSurface)
```

Make it:

```kotlin
                    modifier = Modifier.graphicsLayer { alpha = (SECOND + (1f - SECOND) * on(a.value, k) * glow) * shown(k) })
                Icon(mark, null, Modifier.size(14.dp).graphicsLayer { alpha = on(a.value, k) * glow * shown(k) }, tint = ink ?: scheme.onSurface)
```

- [ ] **Step 3: The stage, in `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`.** Twenty-five changes, in the file's order: the imports; the clock and what draws by it (`RISE`, `AT_REST`, `stageClock`, `come`, `below`, `rises`, `fadesIn`, `comesUp`, `Own`, `own`, `late`, `Under`, `Answers`, `Picture`); `FirstStage` itself; `Seat`; `Band`; `Keys`; `Written` in the place of `Recipe`; `AskBand`.

Find:

```kotlin
import androidx.compose.animation.AnimatedContent
```

Make it:

```kotlin
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
```

Find:

```kotlin
import androidx.compose.animation.togetherWith
```

Make it:

```kotlin
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
```

Find:

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
```

Make it:

```kotlin
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
```

Find:

```kotlin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
```

Make it:

```kotlin
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
```

Find:

```kotlin
import io.github.kuscher.booklight.data.firstRun
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.SetDown
import io.github.kuscher.booklight.data.firstRun
```

Find:

```kotlin
import io.github.kuscher.booklight.ui.Symbols

/** A large cap's label, and a typed part of a recipe: the field's own type. */
```

Make it:

```kotlin
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.delay

/** A large cap's label, and a typed part of a recipe: the field's own type. */
```

Find:

```kotlin
)

/**
 * First run's stage under the empty field (docs/design/first-run/design.md §4 to §7): a seat like a row's (a mark, a
```

Make it:

```kotlin
)

/** How far a part of the stage rises into its place, as a row does. */
private val RISE = 12.dp

/** Where the stage's clock stands once everything is at rest: with the system's animations off it is there from the first frame. */
private const val AT_REST = 1_000_000f

/**
 * The stage's clock, in ms. It starts when the screen that stands comes into view (`OverlayModel.viewFrom`: when it
 * came, and not before the gate), runs as slowly as a debug build asks, and stops at the end of the screen's marks.
 * Every part of the stage is drawn as a function of this clock and of its own mark (`OverlayModel.marks`, core
 * `SetDown`): nothing is kept from frame to frame, a part that has not come has no say, and a debug hook can stand the
 * clock still (`OverlayModel.stageAt`). With the system's animations off it is at rest in the first frame. It is read
 * where things are drawn, never where they are composed: a frame of it composes nothing.
 */
@Composable
private fun stageClock(model: OverlayModel): () -> Float {
    val motion = LocalMotion.current
    val came = model.cameAt
    val arrived = model.arrived
    val stands = model.stageAt
    fun at(now: Long): Float = when {
        stands != null -> stands
        !motion.on -> AT_REST
        !arrived -> 0f
        else -> (now - model.viewFrom) / motion.slow
    }
    val clock = remember(came, arrived, stands, motion.on) { mutableFloatStateOf(at(SystemClock.uptimeMillis())) }
    LaunchedEffect(clock) {
        if (stands != null || !motion.on || !arrived) return@LaunchedEffect
        val end = model.marks.end
        // (A letter typed while it runs takes the stage away: what has not come by then never comes.)
        while (clock.floatValue < end && model.stage != null) clock.floatValue = at(withFrameNanos { it / 1_000_000 })
    }
    return remember(clock) { { clock.floatValue } }
}

/** How far something has come that begins at [at] ms of the stage's clock and takes [ms]: 0 to 1, on the standard curve. A part whose mark is `SetDown.THERE` is there. */
private fun come(t: Float, at: Int, ms: Int): Float = if (at == SetDown.THERE) 1f else FastOutSlowInEasing.transform(((t - at) / ms).coerceIn(0f, 1f))

/** How far below its place something still is that rises there from [at] on, [rise] px on `place`. */
private fun below(t: Float, at: Int, rise: Float): Float = if (at == SetDown.THERE) 0f else (1f - PLACE.at(t - at)) * rise

/** A part that rises into its place as a row does: 12 dp up on `place`, fading in over 140 ms (motion.md §3, "rises"). Drawn so; its place in the layout never changes. */
private fun Modifier.rises(now: () -> Float, at: Int, rise: Float): Modifier = graphicsLayer {
    val t = now()
    alpha = come(t, at, Motion.RISE_MS)
    translationY = below(t, at, rise)
}

/** A part that fades in where it stands, over [ms]. */
private fun Modifier.fadesIn(now: () -> Float, at: Int, ms: Int = Motion.RISE_MS): Modifier = graphicsLayer { alpha = come(now(), at, ms) }

/** A cap that comes up: it fades in over 110 ms and grows from 0.96 of its size on `pop`, about its centre. The "up" half of a key that is pressed: every cap on the stage has moved the way the user's key will (motion.md §4). */
private fun Modifier.comesUp(now: () -> Float, at: Int): Modifier = graphicsLayer {
    val t = now()
    alpha = come(t, at, Motion.CAP_MS)
    val size = if (at == SetDown.THERE) 1f else Motion.CAP_SMALL + (1f - Motion.CAP_SMALL) * POP.at(t - at)
    scaleX = size; scaleY = size
}

/**
 * What one content of a transition is drawn by: the stage's clock and marks for as long as it is the content that
 * stands, and those of its last moment once it gives way to another. So what goes fades as it stood: a part of it that
 * had not come never comes, and nothing of it follows the clock of the screen that takes its place.
 */
private class Own(val now: () -> Float, val marks: SetDown.Marks)

@Composable
private fun own(now: () -> Float, marks: SetDown.Marks, stands: Boolean): Own {
    val last = remember { floatArrayOf(0f) }
    val kept = remember { arrayOf(marks) }
    val live = rememberUpdatedState(stands)
    val clock = rememberUpdatedState(now)
    if (stands) kept[0] = marks
    val mine = remember { { if (live.value) clock.value().also { last[0] = it } else last[0] } }
    return Own(mine, kept[0])
}

/**
 * [value], a moment late where it changes: so that words which change together roll one after the other (the title,
 * then its line, then the counter: motion.md T5). With no wait, and with the system's animations off, it is simply [value].
 */
@Composable
private fun <T> late(value: T, ms: Int): T {
    val motion = LocalMotion.current
    var shown by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (shown != value) { if (ms > 0) delay(motion.held(ms.toLong())); shown = value } }
    return if (ms <= 0 || !motion.on) value else shown
}

/** What stands under the seat, with everything it draws: where the question takes a lesson's place, the lesson's band still draws what it showed while it fades. */
private data class Under(
    val on: FirstRun.Screen, val parts: List<FirstRun.Part>, val caption: String, val text: String, val note: String,
    val labels: List<String>, val answers: List<FirstRun.Answer>,
) {
    val ask: Boolean get() = on.step == FirstRun.Step.ASK
}

/** The answers, as one thing that gives way to other answers. */
private data class Answers(val labels: List<String>, val answers: List<FirstRun.Answer>)

/** What a band shows: the suggested keys as two large caps, "your key", or a lesson's recipe. */
private sealed interface Picture {
    data class Caps(val action: String, val name: String) : Picture
    data object Yours : Picture
    data class Recipe(val parts: List<FirstRun.Part>) : Picture
}

/**
 * First run's stage under the empty field (docs/design/first-run/design.md §4 to §7): a seat like a row's (a mark, a
```

Find:

```kotlin
 * selected, and the field above it is live.
```

Make it:

```kotlin
 * selected, and the field above it is live.
 *
 * It is not there in one frame (motion.md §3): a screen is set down part by part, comes back as rows do, or takes
 * another's place where that stood. When each part comes is the model's to say (`OverlayModel.marks`, core `SetDown`,
 * with `Motion.kt`'s times); here every part is drawn by its mark on one clock ([stageClock]), as a layer over a layout
 * that never changes: sizes are known before anything moves, and the place of the answers is the same in every frame.
```

Find:

```kotlin
    val state = model.settings.firstRun()
```

Make it:

```kotlin
    val motion = LocalMotion.current
    val state = model.settings.firstRun()
```

Find:

```kotlin
    val example = model.example
    val parts = if (example == null && on != FirstRun.Screen.L4) emptyList()
        else FirstRun.recipe(state, example ?: FirstRun.example(null, "", null, "", ""), stringResource(R.string.first_sum_example))
```

Make it:

```kotlin
    val parts = model.recipe(state)
```

Find:

```kotlin
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
    Column(Modifier.fillMaxWidth().height(Metrics.stage(on, model.askMore)).padding(vertical = Metrics.pad)) {
        Seat(said.symbol, said.title, said.line, counter)
        if (on.step == FirstRun.Step.ASK) AskBand(text, note, model.askMore, armed, strip)
        else Band(armed, said.caption, strip) {
            if (on.step == FirstRun.Step.KEY) KeyCaps(model, on) else Recipe(parts, Modifier.align(Alignment.CenterStart).clearAndSetSemantics { })
```

Make it:

```kotlin
    // The clock and the marks this stage is drawn by. Once it is no longer the stage that stands (a letter was typed over
    // it; the choices took the question's place) and is only fading where it stood, they are those of its last moment.
    val whole = own(stageClock(model), model.marks, stands = model.stage != null)
    val now = whole.now
    val marks = whole.marks
    // The screen took another's place where that stood: its seat stays, and its words roll.
    val turn = model.comes == SetDown.Comes.TURN
    val rise = with(density) { RISE.toPx() }
    // The answers, at the band's right end. Where a screen has other answers than the one before it, the old ones fade
    // where they stand and the new ones come by their mark; answers that stay (a lesson's Skip) do not move.
    val strip: @Composable (Under, Own) -> Unit = { under, outer ->
        AnimatedContent(Answers(under.labels, under.answers), transitionSpec = { (EnterTransition.None togetherWith fadeOut(motion.fade(Motion.PRESS_MS))).using(null) }, contentAlignment = Alignment.CenterEnd, label = "answers") { a ->
            val o = own(outer.now, outer.marks, transition.targetState == EnterExitState.Visible)
            val m = o.marks
            // (They rise as they come, the armed one's highlight with them. Not at the landing of the opening piece: there
            // the highlight is on its way to where the armed answer stands, and nothing of them moves.)
            Box(Modifier.graphicsLayer { if (m.rises) translationY = below(o.now(), m.answers, rise) }) {
                OptionStrip(a.labels, armed.coerceIn(0, (a.labels.size - 1).coerceAtLeast(0)), onChoose = { model.stageArm(it) },
                    // (A click counts from the moment the keys do: "Agree" is consent, by whichever way it is given.)
                    onRun = { if (model.seen) { model.stageArm(it); a.answers.getOrNull(it)?.let(onAnswer) } },
                    // First run's show hands its one highlight over to the armed answer (`Glide`): while it is on its way the answers
                    // say where the armed one stands (slots of 32 dp, 4 apart), and are not lit themselves.
                    modifier = Modifier.onGloballyPositioned { c ->
                        if (model.gliding) with(density) {
                            val at = c.positionInRoot()
                            val top = at.y + armed.coerceAtLeast(0) * 36.dp.toPx()
                            model.answerAt = floatArrayOf(at.x, top, at.x + c.size.width, top + 32.dp.toPx())
                        }
                    },
                    vertical = true, lit = armed >= 0 && !model.gliding,
                    shown = { k -> come(o.now(), if (k == armed) m.armed else m.answers, if (m.rises) Motion.RISE_MS else Motion.CAP_MS) })
            }
        }
    }
    Column(Modifier.fillMaxWidth().height(Metrics.stage(on, model.askMore)).padding(vertical = Metrics.pad)) {
        Seat(said.symbol, said.title, said.line, counter, now, marks, turn, rise)
        // Under the seat: the question's disclosure and its answers, or a band. Where the one takes the other's place (Skip
        // on the sum's lesson), what stood fades where it stands and what comes is set down by its marks.
        AnimatedContent(Under(on, parts, said.caption, text, note, labels, answers), contentKey = { it.ask },
            transitionSpec = { (EnterTransition.None togetherWith fadeOut(motion.fade(Motion.PRESS_MS))).using(null) }, contentAlignment = Alignment.TopStart, label = "under") { under ->
            val o = own(now, marks, transition.targetState == EnterExitState.Visible)
            if (under.ask) AskBand(under, model.askMore, armed, o, rise) { strip(under, it) }
            else Band(model, under, armed, o) { strip(under, it) }
```

Find:

```kotlin
 * title alone), and the counter where a row's kind stands. Its words roll when the screen changes, as a counter's do.
 */
@Composable
private fun Seat(symbol: String, title: String, line: String, counter: String?) {
```

Make it:

```kotlin
 * title alone), and the counter where a row's kind stands. Where a screen is set down its three parts rise one after
 * the other ([marks]); where it takes another's place ([turn]) they stay, the mark's symbol gives way to the new one,
 * and the words roll as a counter's do: the title, the line 22 ms later, the counter's 44.
 */
@Composable
private fun Seat(symbol: String, title: String, line: String, counter: String?, now: () -> Float, marks: SetDown.Marks, turn: Boolean, rise: Float) {
```

Find:

```kotlin
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(Metrics.row), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = if (dark) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            AnimatedContent(symbol, transitionSpec = { (fadeIn(motion.fade(110)) togetherWith fadeOut(motion.fade(80))).using(null) }, label = "mark") {
```

Make it:

```kotlin
    val lineLate = late(line, if (turn) Motion.PART_MS else 0)
    // (A screen without a line under its title, the question, has none from its first frame: its title stands alone on the seat's centre line.)
    val lineNow = if (line.isEmpty()) "" else lineLate
    val counterNow = late(counter, if (turn) 2 * Motion.PART_MS else 0) ?: counter
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(Metrics.row), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.rises(now, marks.disc, rise).size(36.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = if (dark) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            AnimatedContent(symbol, transitionSpec = { ((scaleIn(motion.pop(), 0.6f) + fadeIn(motion.fade(110))) togetherWith (scaleOut(motion.fade(Motion.PRESS_MS), 0.6f) + fadeOut(motion.fade(Motion.PRESS_MS)))).using(null) }, contentAlignment = Alignment.Center, label = "mark") {
```

Find:

```kotlin
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 16.dp)) {
```

Make it:

```kotlin
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 16.dp).rises(now, marks.words, rise)) {
```

Find:

```kotlin
            if (line.isNotEmpty()) AnimatedContent(line, transitionSpec = { motion.roll() }, contentAlignment = Alignment.TopStart, label = "line") {
```

Make it:

```kotlin
            if (lineNow.isNotEmpty()) AnimatedContent(lineNow, transitionSpec = { motion.roll() }, contentAlignment = Alignment.TopStart, label = "line") {
```

Find:

```kotlin
        if (counter != null) AnimatedContent(counter, transitionSpec = { motion.roll() }, contentAlignment = Alignment.CenterEnd, label = "counter") {
```

Make it:

```kotlin
        if (counterNow != null) AnimatedContent(counterNow, Modifier.rises(now, marks.counter, rise), transitionSpec = { motion.roll() }, contentAlignment = Alignment.CenterEnd, label = "counter") {
```

Find:

```kotlin
 * centre line ([keys]); the caption under it, on the line a footer's words stand on; the answers at the right end, on
 * the same centre line.
 */
@Composable
private fun Band(armed: Int, caption: String, strip: @Composable () -> Unit, keys: @Composable BoxWithConstraintsScope.() -> Unit) {
```

Make it:

```kotlin
 * centre line ([Keys]); the caption under it, on the line a footer's words stand on; the answers at the right end, on
 * the same centre line. Each comes by its mark ([o]): the keys a beat apart or the recipe written, then the caption,
 * then the answers. Where the screen took another's place, a caption that says the same does not move (its mark says
 * it is there), and one that says something else comes once the band has been written, while the old one fades.
 */
@Composable
private fun Band(model: OverlayModel, under: Under, armed: Int, o: Own, answers: @Composable (Own) -> Unit) {
```

Find:

```kotlin
    Row(Modifier.padding(start = LEFT, end = RIGHT).fillMaxWidth().height(BAND), verticalAlignment = Alignment.CenterVertically) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            keys()
            // Nothing is armed until Tab (on "Now press your keys", in a lesson): a `tab` cap says so, 10 dp before the answers, and goes once one is.
            // (It lies over the band's free end and takes no room: the caps have all of it.)
            val cap by animateFloatAsState(if (armed < 0) 1f else 0f, motion.fade(120), label = "cap")
            Box(Modifier.align(Alignment.CenterEnd).offset(x = 14.dp).graphicsLayer { alpha = cap }) { Keycap("tab") }
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(CAPTION_BAND), contentAlignment = Alignment.CenterStart) {
                AnimatedContent(caption, transitionSpec = { (fadeIn(motion.fade(120)) togetherWith fadeOut(motion.fade(80))).using(null) }, contentAlignment = Alignment.CenterStart, label = "caption") {
                    Text(it, color = second, style = CAPTION, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadeEnd())
```

Make it:

```kotlin
    val now = o.now
    val marks = o.marks
    Row(Modifier.padding(start = LEFT, end = RIGHT).fillMaxWidth().height(BAND), verticalAlignment = Alignment.CenterVertically) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            Keys(model, under, o)
            // Nothing is armed until Tab (on "Now press your keys", in a lesson): a `tab` cap says so, 10 dp before the answers, and goes once one is.
            // (It lies over the band's free end and takes no room: the caps have all of it. It comes with the answers.)
            val cap by animateFloatAsState(if (armed < 0) 1f else 0f, motion.fade(120), label = "cap")
            Box(Modifier.align(Alignment.CenterEnd).offset(x = 14.dp).graphicsLayer { alpha = cap * come(now(), marks.answers, Motion.RISE_MS) }) { Keycap("tab") }
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(CAPTION_BAND), contentAlignment = Alignment.CenterStart) {
                AnimatedContent(under.caption, transitionSpec = { (EnterTransition.None togetherWith fadeOut(motion.fade(Motion.PRESS_MS))).using(null) }, contentAlignment = Alignment.CenterStart, label = "caption") {
                    val mine = own(now, marks, transition.targetState == EnterExitState.Visible)
                    Text(it, color = second, style = CAPTION, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadesIn(mine.now, mine.marks.caption).fadeEnd())
```

Find:

```kotlin
        Spacer(Modifier.width(24.dp))
        strip()
```

Make it:

```kotlin
        Spacer(Modifier.width(24.dp))
        answers(o)
```

Find:

```kotlin
 * The key's step in the band: on its first screen the suggested keys, Action and the key beside it, as two large caps;
 * on the others "your key", one blank cap that takes the check once the key works.
 */
@Composable
private fun BoxWithConstraintsScope.KeyCaps(model: OverlayModel, on: FirstRun.Screen) {
```

Make it:

```kotlin
 * What the band shows, from the titles' edge on its centre line. The key's step: on its first screen the suggested
 * keys, Action and the key beside it, as two large caps that come up a beat apart; on the others "your key", one blank
 * cap that takes the check once the key works. A lesson: its recipe, what to type in the field's own type as it will
 * stand in the field, written a letter at a time, and the keys to press as large caps (Enter is the drawn mark); 12 dp
 * between two caps, 16 between a cap and letters. A picture: it takes no pointer and no key. Where one picture takes
 * another's place the old one fades where it stands and the new one comes by its marks.
 */
@Composable
private fun BoxWithConstraintsScope.Keys(model: OverlayModel, under: Under, o: Own) {
```

Find:

```kotlin
    val key = model.suggested
    val name = SystemWords.name(context, key)
    val action = stringResource(R.string.key_action)
```

Make it:

```kotlin
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val room = with(density) { maxWidth.toPx() }
    val key = model.suggested
    val name = SystemWords.name(context, key)
    val action = stringResource(R.string.key_action)
```

Find:

```kotlin
    // The system's name for the key is used on the cap only where the cap then has room: a cap never shrinks or wraps.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val room = with(density) { maxWidth.toPx() }
    // (Its stand-in on the cap is a word known to fit, the same in every language: the caption and the rows keep the name.)
    val fallback = stringResource(if (key == FirstRun.Key.QUICK_INSERT) R.string.first_key_quick_cap else R.string.first_key_letter)
    val shown = remember(action, name, fallback, room) {
```

Make it:

```kotlin
    // (Its stand-in on the cap is a word known to fit, the same in every language: the caption and the rows keep the name.)
    val fallback = stringResource(if (key == FirstRun.Key.QUICK_INSERT) R.string.first_key_quick_cap else R.string.first_key_letter)
    // The system's name for the key is used on the cap only where the cap then has room: a cap never shrinks or wraps.
    val fits = remember(action, name, fallback, room) {
```

Find:

```kotlin
    AnimatedContent(on == FirstRun.Screen.K1, Modifier.align(Alignment.CenterStart).clearAndSetSemantics { },
        transitionSpec = { (fadeIn(motion.fade(110, 40)) togetherWith fadeOut(motion.fade(80))).using(null) }, contentAlignment = Alignment.CenterStart, label = "keys") { suggestion ->
        if (suggestion) Row(verticalAlignment = Alignment.CenterVertically) {
            BigCap { Text(action, color = scheme.onSurface, style = CAP, maxLines = 1, softWrap = false) }
            Text("+", color = second, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), modifier = Modifier.width(PLUS), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            BigCap { Text(shown, color = scheme.onSurface, style = CAP, maxLines = 1, softWrap = false) }
        } else BigCap(Modifier.width(YOUR_KEY)) {
            // "Your key": blank, because Booklight cannot know the keys; the one check Booklight has is drawn in it once it works.
            DrawnCheck(model.stage == FirstRun.Screen.K4, scheme.onSurface, Modifier.size(28.dp), stroke = 2.5.dp)
        }
    }
}

/**
 * A lesson's recipe: what to type, in the field's own type, as it will stand in the field; and the keys to press, as
 * large caps (Enter is the drawn mark). Left to right on one centre line: 12 dp between two caps, 16 between a cap and
 * letters. A picture: it takes no pointer and no key. It changes with the lesson, where it stands.
 */
@Composable
private fun Recipe(parts: List<FirstRun.Part>, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    AnimatedContent(parts, modifier, transitionSpec = { (fadeIn(motion.fade(110, 40)) togetherWith fadeOut(motion.fade(80))).using(null) }, contentAlignment = Alignment.CenterStart, label = "recipe") { pieces ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            pieces.forEachIndexed { i, part ->
                if (i > 0) Spacer(Modifier.width(if (part is FirstRun.Part.Key && pieces[i - 1] is FirstRun.Part.Key) 12.dp else 16.dp))
                when (part) {
                    is FirstRun.Part.Typed -> Text(part.text, color = scheme.onSurface, style = CAP, maxLines = 1, softWrap = false)
                    FirstRun.Part.Key.TAB -> BigCap { Text(stringResource(R.string.first_key_tab), color = scheme.onSurface, style = CAP, maxLines = 1, softWrap = false) }
                    FirstRun.Part.Key.ENTER -> BigCap { Icon(Symbols.enter, null, Modifier.size(24.dp), tint = scheme.onSurface) }
```

Make it:

```kotlin
    val picture: Picture = when {
        under.on.step != FirstRun.Step.KEY -> Picture.Recipe(under.parts)
        under.on == FirstRun.Screen.K1 -> Picture.Caps(action, fits)
        else -> Picture.Yours
    }
    AnimatedContent(picture, Modifier.align(Alignment.CenterStart).clearAndSetSemantics { },
        // (A recipe that was no part of what was set down, because this device's example was worked out a moment late, fades in where it belongs.)
        transitionSpec = { ((if (o.marks.pieces.isEmpty()) fadeIn(motion.fade(Motion.CAP_MS, 40)) else EnterTransition.None) togetherWith fadeOut(motion.fade(Motion.PRESS_MS))).using(null) },
        contentAlignment = Alignment.CenterStart, label = "keys") { what ->
        val mine = own(o.now, o.marks, transition.targetState == EnterExitState.Visible)
        val now = mine.now
        val m = mine.marks
        when (what) {
            is Picture.Caps -> Row(verticalAlignment = Alignment.CenterVertically) {
                BigCap(Modifier.comesUp(now, SetDown.piece(m, 0))) { Text(what.action, color = scheme.onSurface, style = CAP, maxLines = 1, softWrap = false) }
                Text("+", color = second, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), modifier = Modifier.width(PLUS).fadesIn(now, SetDown.piece(m, 1), Motion.CAP_MS), textAlign = TextAlign.Center)
                BigCap(Modifier.comesUp(now, SetDown.piece(m, 2))) { Text(what.name, color = scheme.onSurface, style = CAP, maxLines = 1, softWrap = false) }
            }
            // "Your key": blank, because Booklight cannot know the keys; the one check Booklight has is drawn in it once it works,
            // a moment after the cap has come up.
            Picture.Yours -> {
                val checked by remember(now, m) { derivedStateOf { now() >= SetDown.piece(m, 0) + Motion.CHECK_AFTER_MS } }
                BigCap(Modifier.comesUp(now, SetDown.piece(m, 0)).width(YOUR_KEY)) {
                    DrawnCheck(model.stage == FirstRun.Screen.K4 && checked, scheme.onSurface, Modifier.size(28.dp), stroke = 2.5.dp)
                }
            }
            is Picture.Recipe -> Row(verticalAlignment = Alignment.CenterVertically) {
                what.parts.forEachIndexed { i, part ->
                    if (i > 0) Spacer(Modifier.width(if (part is FirstRun.Part.Key && what.parts[i - 1] is FirstRun.Part.Key) 12.dp else 16.dp))
                    when (part) {
                        is FirstRun.Part.Typed -> Written(part.text, now, SetDown.piece(m, i), m.written, scheme.onSurface)
                        FirstRun.Part.Key.TAB -> BigCap(Modifier.comesUp(now, SetDown.piece(m, i))) { Text(stringResource(R.string.first_key_tab), color = scheme.onSurface, style = CAP, maxLines = 1, softWrap = false) }
                        FirstRun.Part.Key.ENTER -> BigCap(Modifier.comesUp(now, SetDown.piece(m, i))) { Icon(Symbols.enter, null, Modifier.size(24.dp), tint = scheme.onSurface) }
                    }
```

Find:

```kotlin
}

/**
 * The question's band (design.md §7): the disclosure, whole and in strong ink, from the titles' edge to the right
```

Make it:

```kotlin
}

/**
 * A typed part of a recipe, in the field's own type. It is laid out whole, once: its width never changes. Where the
 * recipe is written its letters are cut in one after the other, 40 ms apart ([written]; motion.md §3, "written": in
 * the stage, never in the field); where the screen comes back as a row does they fade in together, with their piece.
 */
@Composable
private fun Written(text: String, now: () -> Float, at: Int, written: Boolean, ink: Color) {
    val n = text.length
    val shown by remember(text, at, written, now) {
        derivedStateOf {
            if (at == SetDown.THERE || !written) n
            else now().let { t -> if (t < at) 0 else (((t - at) / Motion.LETTER_MS).toInt() + 1).coerceAtMost(n) }
        }
    }
    // (Never between the two halves of one character.)
    val cut = if (shown in 1 until n && Character.isLowSurrogate(text[shown])) shown + 1 else shown
    Text(
        if (cut >= n) AnnotatedString(text) else buildAnnotatedString { append(text); addStyle(SpanStyle(color = Color.Transparent), cut, n) },
        color = ink, style = CAP, maxLines = 1, softWrap = false,
        modifier = if (written) Modifier else Modifier.fadesIn(now, at, Motion.CAP_MS),
    )
}

/**
 * The question's band (design.md §7): the disclosure, whole and in strong ink, from the titles' edge to the right
```

Find:

```kotlin
 * lines; the stage is that much taller, and the text is never cut.
 */
@Composable
private fun AskBand(text: String, note: String, more: Dp, armed: Int, strip: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    Column(Modifier.padding(start = LEFT, end = RIGHT).fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(ASK_TEXT + more)) { Text(text, color = scheme.onSurface, style = ASK) }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().height(ASK_ANSWERS), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                Text(note, color = scheme.onSurface.copy(alpha = SECOND), style = SMALL, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = ASK_NOTE))
            }
            val cap by animateFloatAsState(if (armed < 0) 1f else 0f, motion.fade(120), label = "cap")
            Box(Modifier.graphicsLayer { alpha = cap }) { Keycap("tab") }
            Spacer(Modifier.width(10.dp))
            strip()
```

Make it:

```kotlin
 * lines; the stage is that much taller, and the text is never cut. Its parts rise in reading order: the text, the
 * note, then the answers (motion.md T8).
 */
@Composable
private fun AskBand(under: Under, more: Dp, armed: Int, o: Own, rise: Float, answers: @Composable (Own) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val now = o.now
    val marks = o.marks
    Column(Modifier.padding(start = LEFT, end = RIGHT).fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(ASK_TEXT + more).rises(now, SetDown.piece(marks, 0), rise)) { Text(under.text, color = scheme.onSurface, style = ASK) }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().height(ASK_ANSWERS), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).rises(now, SetDown.piece(marks, 1), rise)) {
                Text(under.note, color = scheme.onSurface.copy(alpha = SECOND), style = SMALL, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = ASK_NOTE))
            }
            val cap by animateFloatAsState(if (armed < 0) 1f else 0f, motion.fade(120), label = "cap")
            Box(Modifier.graphicsLayer { alpha = cap * come(now(), marks.answers, Motion.RISE_MS) }) { Keycap("tab") }
            Spacer(Modifier.width(10.dp))
            answers(o)
```

- [ ] **Step 4: When the band begins, and a stage that fades as it stood, in `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** Three changes: `Arrival.bandAfter`; the stage that stood; the body's `first` branch.

Find:

```kotlin
    val leaveMs: Long get() = if (unfold) ((FOLD_MS + LEAVE_WAIT_MS) * leave).toLong() + 20 else Motion.LEAVE_MS
```

Make it:

```kotlin
    val leaveMs: Long get() = if (unfold) ((FOLD_MS + LEAVE_WAIT_MS) * leave).toLong() + 20 else Motion.LEAVE_MS
    /**
     * How long after the gate the band of a screen of first run begins that is set down as the panel opens: when the
     * glass is at rest, and 120 ms after the gate at the least (docs/design/first-run/motion.md §3, "Clocks": 120 ms at
     * Fast, about 163 at Medium, 325 at Slow).
     */
    val bandAfter: Int get() = if (unfold) maxOf(Motion.BAND_AFTER_MS, (Motion.GATE_TO_REST_MS * slow).toInt()) else Motion.BAND_AFTER_MS
```

Find:

```kotlin
                    val footerPx = with(density) { (Metrics.footer + Metrics.pad).roundToPx() }
```

Make it:

```kotlin
                    // The stage that stood is drawn on while it fades where it stood (a letter typed over it, the choices in the
                    // question's place, "Not now"): the model has none by then.
                    val stood = remember { arrayOfNulls<FirstRun.Screen>(1) }
                    model.stage?.let { stood[0] = it }
                    val footerPx = with(density) { (Metrics.footer + Metrics.pad).roundToPx() }
```

Find:

```kotlin
                                state == "first" -> model.stage?.let { FirstStage(model, it, onStage) }
```

Make it:

```kotlin
                                state == "first" -> (model.stage ?: stood[0])?.let { FirstStage(model, it, onStage) }
```

- [ ] **Step 5: The activity says how long motion lasts and how the panel opens, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Two changes in `onCreate`.

Find:

```kotlin
        model.hold = motion::hold
```

Make it:

```kotlin
        model.hold = motion::hold
        model.lasts = motion::held
```

Find:

```kotlin
        arrival = Arrival.of(opening, motion)
```

Make it:

```kotlin
        arrival = Arrival.of(opening, motion)
        // (First run's stage, where one is set down as this panel opens, begins its band when the glass is at rest.)
        model.opening(arrival.bandAfter)
```

- [ ] **Step 6: One place says when a screen came, and no time is written into the stage**

```bash
grep -c "cameAt = SystemClock.uptimeMillis()" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
grep -n "private const val SEEN_MS" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt || echo GONE
grep -c "FirstRun.inView(" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
grep -nE "[^a-zA-Z](delay|tween)\([0-9]" app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt || echo CLEAN
grep -n "fun KeyCaps\|private fun Recipe(" app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt || echo GONE
awk '/model.lasts = motion::held/ { l = NR } /model.opening\(arrival.bandAfter\)/ { o = NR } /model = OverlayModel\(/ { m = NR } END { print (m < l && l < o) ? "IN-ORDER" : "OUT-OF-ORDER" }' app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
```

Expected: `1` (in `came` alone), `GONE`, `1` (in `seen`), `CLEAN`, `GONE`, `IN-ORDER`.

- [ ] **Step 7: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Strip.kt app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
git commit -m "First run: a screen is set down part by part, comes back as rows do, or takes another's place where that stood, and takes a key once its answers have begun to show"
```

### Task 11: Pressed: an answer on the stage, and the key that lands

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Strip.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`

**Interfaces:**
- Consumes: `Motion.PRESS_MS`, `RELEASE_MS`, `KEY_TAP_MS`, `KEY_HELD_MS`, `CHECK_AFTER_MS`, `CHECK_MS`, `LAP_AFTER_MS`, `HELPER_AFTER_MS`, `CAP_SMALL` (Task 5); `OverlayModel.lasts`, `marks`, `OptionStrip(…, shown)`, and in `FirstStage.kt` `Modifier.comesUp(now, at)`, `BigCap(modifier, content)`, `Keys`, the `strip` (Task 10); core `FirstRun.keyLanded(s)`, `FirstRun.answers(s)`, `FirstRun.Hold`, `SetDown.piece(m, k)`. In `OverlayModel`: `answerStage(answer)` (as Task 6 left it), `fun keyLanded()`, `awaitsKey`, `helperCame()`, `laps`, `due`, `stage`, `arrived`, `began`. In `OverlayActivity`: `byKey(intent)`, `first(app) { … }`, `onNewIntent` with its `key`, `signal()` with `after.came`, `stage(answer)`, `landed()`, `hold`, `motion.hold(ms)`. In `Panel.kt`: the lap's two effects. `DrawnCheck(shown: Boolean, color: Color, modifier: Modifier = Modifier, stroke: Dp = 2.dp)` (`overlay/Marks.kt`).
- Produces, in `OverlayModel`:
  - `var stagePressed: Int` (state, set privately): the place among the stage's answers of the one that was just given, -1 for none.
  - `var keyDown`, `keyUncovered`, `keyChecked`, `landing`: Boolean states, set privately: "your key" on the glass is held down; it went down unseen, under the dialog; its check may draw; the landing is still being shown.
  - `fun keyLanded(under: Boolean = false)` (it took no argument): also shows the landing where the key's step stood on the glass.
  - `fun keyOpened()`: the user's key has made this panel, and "Your key works" is set down in it for the first time: the lap of light.
- Produces elsewhere:
  - `OptionStrip(…, shown, pressed: Int = -1)`: the option that shows as pressed.
  - In `FirstStage.kt`: `Modifier.comesUp(now, at, down: () -> Float = { 0f })`, `BigCap(modifier, pressed: () -> Float = { 0f }, content)`.
  - `OverlayActivity`: `onCreate` tells the model where the key that made the panel is news; `onNewIntent` returns while a landing is shown; `helperCame` is told 300 ms after the dialog came; `stage()` shows "Open Keyboard shortcuts" as pressed; `landed()` says whether the panel was under the dialog.
  - `Panel`: no ordinary lap of light in a panel that opens on a screen of first run; the asked-for lap is composed where first run has asked for one.

`motion.md` §4: a key that is pressed goes down and comes up. Three things on the stage are pressed.

**An answer.** Given by Enter or by a click, its slot shows as pressed for 80 ms (the design system's pressed token: ink over the slot), in the frame the answer is taken; what the answer does is not held up by it. The model says which (`stagePressed`); the strip draws it. On the question nothing is armed and nothing is pressed for the user: only an answer the user gave is shown so. "Open Keyboard shortcuts" changes nothing that is kept, so the activity's `stage()` now tells the model of it too, for the slot alone.

**The hand-off (T2).** The stage turned to "Now press your keys" in the frame the panel lost the focus, beside the arriving dialog. It now turns 300 ms later: the dialog covers the panel by then.

**The key that lands (T3, T4).** Where the key's step stood on the glass and the user's key lands, "your key" is held down as the user's is: 160 ms where the panel was in view, 400 ms where it was under the system's dialog and is only now uncovered (the first frame that shows it shows it down). Then it comes up, the check draws 60 ms later, and 240 ms after the key came up the light runs one lap. A key that is held repeats, and each repeat is a new start of the panel: while the landing is shown (`landing`) none of them puts the panel away. Where the keys made a new panel (the dialog was closed first), "Your key works" is set down as any screen is, and the same lap follows (`keyOpened`).

**The lap.** A panel that opens on a screen of first run has no ordinary lap of light any more: there the lap is what first run asks for itself, when the key works and when the run folds (Task 12). Someone with no run: `model.due` is null, and everything is as it was.

- [ ] **Step 1: An option that shows as pressed, in `app/src/main/java/io/github/kuscher/booklight/overlay/Strip.kt`.** Three changes in `OptionStrip`: the parameter; the press as a value; the ink over the slot.

Find:

```kotlin
    shown: (Int) -> Float = { 1f },
```

Make it:

```kotlin
    shown: (Int) -> Float = { 1f },
    /** The option that was just run shows as pressed (first run's answers): its index, -1 for none. */
    pressed: Int = -1,
```

Find:

```kotlin
    val glow by animateFloatAsState(if (lit) 1f else 0f, motion.fade(120), label = "lit")
```

Make it:

```kotlin
    val glow by animateFloatAsState(if (lit) 1f else 0f, motion.fade(120), label = "lit")
    // Pressed: the slot gains a little ink, the design system's pressed token (0.08, 80 ms in and 120 out). No colour.
    val press by animateFloatAsState(if (pressed >= 0) 1f else 0f, motion.fade(if (pressed >= 0) Motion.PRESS_MS else Motion.RELEASE_MS), label = "press")
    val pressInk = scheme.onSurface.copy(alpha = 0.08f)
    // (Which slot it was is kept while the ink leaves.)
    val pressedSlot = remember { intArrayOf(0) }
    if (pressed >= 0) pressedSlot[0] = pressed
```

Find:

```kotlin
                val there = if (n == 0) 0f else glow * shown(chosen.coerceIn(0, n - 1))
```

Make it:

```kotlin
                if (n > 0 && press > 0f) {
                    val k = pressedSlot[0].coerceIn(0, n - 1)
                    val at = if (vertical) Offset(0f, slots.x[k].toFloat()) else Offset(slots.x[k].toFloat(), 0f)
                    val box = if (vertical) Size(size.width, slots.w[k].toFloat()) else Size(slots.w[k].toFloat(), size.height)
                    drawRoundRect(pressInk.copy(alpha = pressInk.alpha * press * shown(k)), at, box, CornerRadius(SLOT.toPx() / 2))
                }
                val there = if (n == 0) 0f else glow * shown(chosen.coerceIn(0, n - 1))
```

- [ ] **Step 2: What is pressed, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Two changes: `stagePressed` with `answerStage`; `keyLanded(under)` with its states, and `keyOpened`.

Find:

```kotlin
    /** An answer was given on the stage. ("Open Keyboard shortcuts" changes nothing here: it is the window's to do.) */
    fun answerStage(answer: FirstRun.Answer) {
        val was = first()
```

Make it:

```kotlin
    /** The answer of the stage that was just given shows as pressed, for as long as the pressed token takes to come: its place among the answers, -1 for none. */
    var stagePressed by mutableIntStateOf(-1); private set
    private var pressing: Job? = null

    /** An answer was given on the stage: its slot shows as pressed. ("Open Keyboard shortcuts" changes nothing else here: the rest is the window's to do.) */
    fun answerStage(answer: FirstRun.Answer) {
        val was = first()
        FirstRun.answers(was).indexOf(answer).takeIf { it >= 0 }?.let { at ->
            stagePressed = at
            pressing?.cancel()
            pressing = scope.launch { delay(lasts(Motion.PRESS_MS.toLong())); stagePressed = -1 }
        }
```

Find:

```kotlin
    /** The user's key has opened the panel, or uncovered it. Where its step waited for it, "Your key works" stands. */
    fun keyLanded() = step { FirstRun.keyLanded(it) }
```

Make it:

```kotlin
    /** "Your key" on the glass is held down, as the user's own key is in this moment (docs/design/first-run/motion.md §4). */
    var keyDown by mutableStateOf(false); private set
    /** It went down under the system's dialog, unseen: the first frame that shows the panel shows it down, with no way there. */
    var keyUncovered by mutableStateOf(false); private set
    /** The check may draw in "your key": always, but while a landing is shown, where it waits for the key to come up. */
    var keyChecked by mutableStateOf(true); private set
    /**
     * The key's landing is still being shown, until its check is whole. The user's key, held, repeats, and each repeat is
     * a new start of the panel: while this holds none of them puts the panel away.
     */
    var landing by mutableStateOf(false); private set
    private var landed: Job? = null

    /**
     * The user's key has opened the panel, or uncovered it. Where its step waited for it, "Your key works" stands; and
     * where that step stood on the glass, the landing is shown (motion.md T3 and T4): "your key" is down as the user's is,
     * for 160 ms in view, for 400 where the panel was [under] the system's dialog and is only now uncovered; it comes
     * up; 60 ms later its check draws; 240 ms after the key came up the light runs its one lap. With the system's
     * animations off all of it stands in the first frame.
     */
    fun keyLanded(under: Boolean = false) {
        val waited = awaitsKey
        step { FirstRun.keyLanded(it) }
        if (!waited || stage != FirstRun.Screen.K4) return
        landed?.cancel()
        keyUncovered = under; keyDown = true; keyChecked = false; landing = true
        landed = scope.launch {
            delay(lasts(if (under) Motion.KEY_HELD_MS else Motion.KEY_TAP_MS))
            keyDown = false
            delay(lasts(Motion.CHECK_AFTER_MS))
            keyChecked = true
            delay(lasts(Motion.CHECK_MS))
            landing = false
            delay(lasts(Motion.LAP_AFTER_MS - Motion.CHECK_AFTER_MS - Motion.CHECK_MS))
            if (stage == FirstRun.Screen.K4) laps++
        }
    }

    /**
     * The user's key has made this panel, and "Your key works" is set down in it for the first time (the system's dialog
     * was closed before the keys were pressed): the same lap of light, 240 ms after "your key" has come up at its mark.
     * Said by the activity.
     */
    fun keyOpened() {
        if (due != FirstRun.Screen.K4) return
        scope.launch {
            androidx.compose.runtime.snapshotFlow { arrived }.first { it }
            delay(lasts(maxOf(0, SetDown.piece(marks, 0)).toLong() + Motion.LAP_AFTER_MS))
            if (stage == FirstRun.Screen.K4) laps++
        }
    }
```

- [ ] **Step 3: The caps go down and come up, in `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`.** Six changes: `comesUp` with `down`; the strip handed what is pressed; "your key" held down, and its check; `BigCap` with `pressed`.

Find:

```kotlin
import androidx.compose.ui.graphics.Color
```

Make it:

```kotlin
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
```

Find:

```kotlin
/** A cap that comes up: it fades in over 110 ms and grows from 0.96 of its size on `pop`, about its centre. The "up" half of a key that is pressed: every cap on the stage has moved the way the user's key will (motion.md §4). */
private fun Modifier.comesUp(now: () -> Float, at: Int): Modifier = graphicsLayer {
    val t = now()
    alpha = come(t, at, Motion.CAP_MS)
    val size = if (at == SetDown.THERE) 1f else Motion.CAP_SMALL + (1f - Motion.CAP_SMALL) * POP.at(t - at)
```

Make it:

```kotlin
/**
 * A cap that comes up: it fades in over 110 ms and grows from 0.96 of its size on `pop`, about its centre. The "up" half
 * of a key that is pressed: every cap on the stage has moved the way the user's key will (motion.md §4). [down]: how
 * far it is held down just now, 0 to 1 (a little under 0 as it springs back up): "your key", as the user's key lands.
 */
private fun Modifier.comesUp(now: () -> Float, at: Int, down: () -> Float = { 0f }): Modifier = graphicsLayer {
    val t = now()
    alpha = come(t, at, Motion.CAP_MS)
    val size = (if (at == SetDown.THERE) 1f else Motion.CAP_SMALL + (1f - Motion.CAP_SMALL) * POP.at(t - at)) - (1f - Motion.CAP_SMALL) * down()
```

Find:

```kotlin
                    shown = { k -> come(o.now(), if (k == armed) m.armed else m.answers, if (m.rises) Motion.RISE_MS else Motion.CAP_MS) })
```

Make it:

```kotlin
                    shown = { k -> come(o.now(), if (k == armed) m.armed else m.answers, if (m.rises) Motion.RISE_MS else Motion.CAP_MS) },
                    // (The answer that was just given shows as pressed, also on answers that fade for it.)
                    pressed = model.stagePressed)
```

Find:

```kotlin
            // a moment after the cap has come up.
            Picture.Yours -> {
                val checked by remember(now, m) { derivedStateOf { now() >= SetDown.piece(m, 0) + Motion.CHECK_AFTER_MS } }
                BigCap(Modifier.comesUp(now, SetDown.piece(m, 0)).width(YOUR_KEY)) {
                    DrawnCheck(model.stage == FirstRun.Screen.K4 && checked, scheme.onSurface, Modifier.size(28.dp), stroke = 2.5.dp)
```

Make it:

```kotlin
            // a moment after the cap has come up. As the user's key lands it is held down as theirs is (0.96 of its size, a
            // little ink), comes up on `pop`, and only then takes its check (motion.md §4, T3 and T4): under the system's
            // dialog it is down at once, so that the first frame that shows the panel shows it down.
            Picture.Yours -> {
                val checked by remember(now, m) { derivedStateOf { now() >= SetDown.piece(m, 0) + Motion.CHECK_AFTER_MS } }
                val held = model.keyDown
                val goes = if (model.keyUncovered) 0 else Motion.PRESS_MS
                val sunk by animateFloatAsState(if (held) 1f else 0f, if (held) motion.fade(goes) else motion.pop(), label = "key")
                val ink by animateFloatAsState(if (held) 1f else 0f, motion.fade(if (held) goes else Motion.RELEASE_MS), label = "ink")
                BigCap(Modifier.comesUp(now, SetDown.piece(m, 0), down = { sunk }).width(YOUR_KEY), pressed = { ink }) {
                    DrawnCheck(model.stage == FirstRun.Screen.K4 && checked && model.keyChecked, scheme.onSurface, Modifier.size(28.dp), stroke = 2.5.dp)
```

Find:

```kotlin
 * picture, not a button: it takes no pointer and no key.
 */
@Composable
private fun BigCap(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDark.current
```

Make it:

```kotlin
 * picture, not a button: it takes no pointer and no key. [pressed]: how far it is pressed, 0 to 1: its fill gains
 * ink 0.08, the design system's pressed token, and no colour.
 */
@Composable
private fun BigCap(modifier: Modifier = Modifier, pressed: () -> Float = { 0f }, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDark.current
    val ink = scheme.onSurface
```

Find:

```kotlin
            .background(scheme.onSurface.copy(alpha = if (dark) 0.14f else 0.10f))
```

Make it:

```kotlin
            .background(scheme.onSurface.copy(alpha = if (dark) 0.14f else 0.10f))
            .drawBehind { pressed().let { if (it > 0f) drawRect(ink.copy(alpha = 0.08f * it.coerceAtMost(1f))) } }
```

- [ ] **Step 4: The activity tells the model, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Six changes, in the file's order: whether the key that made the panel is news; `keyOpened`; a held key's repeats; the turn under the dialog, late; the pressed slot; where the key landed.

Find:

```kotlin
        if (byKey(intent)) first(app) { FirstRun.keyLanded(it) }
```

Make it:

```kotlin
        val keyed = byKey(intent)
        // (Whether this key is news: a first key, or the other one a replay asked for. Then "Your key works" is greeted below.)
        val news = keyed && app.prefs.now.firstRun().let { FirstRun.keyLanded(it) != it }
        if (keyed) first(app) { FirstRun.keyLanded(it) }
```

Find:

```kotlin
        // The piece begins with this panel: the welcome, then the show, then the key's step. Where it is this opening's and
```

Make it:

```kotlin
        // The key that made this panel is the one its step waited for: "Your key works" is set down, and the light runs its lap.
        if (news) model.keyOpened()
        // The piece begins with this panel: the welcome, then the show, then the key's step. Where it is this opening's and
```

Find:

```kotlin
        // A key nobody knew of, pressed with the panel open: it is known now. It puts the panel away, as every day.
```

Make it:

```kotlin
        // The user's key, held, repeats, and each repeat is a new start: while its landing is still being shown (the key on
        // the glass down and up, its check drawing) none of them puts the panel away.
        if (key && model.landing) return
        // A key nobody knew of, pressed with the panel open: it is known now. It puts the panel away, as every day.
```

Find:

```kotlin
        if (after.came) model.helperCame()
```

Make it:

```kotlin
        // (Under the dialog the stage turns to "Now press your keys" a moment after the panel lost the focus to it: the dialog
        // covers the panel by then, and nothing is seen to change beside it. The wait is there to be kept, also with animations off.)
        if (after.came) lifecycleScope.launch { delay(motion.hold(Motion.HELPER_AFTER_MS)); model.helperCame() }
```

Find:

```kotlin
        // The key is suggested for the keyboard this Enter came from: the dialog's rows carry it.
```

Make it:

```kotlin
        // (The slot shows as pressed in the frame the dialog is asked for; nothing that is kept changes by this answer.)
        model.answerStage(answer)
        // The key is suggested for the keyboard this Enter came from: the dialog's rows carry it.
```

Find:

```kotlin
        model.keyLanded()
```

Make it:

```kotlin
        // (Under the dialog "your key" is held down until the dialog has gone; in view, for a tap's length.)
        model.keyLanded(under = hold == FirstRun.Hold.UNDER)
```

- [ ] **Step 5: First run's own laps, in `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** Two changes: no ordinary lap in a panel that opens on a screen of first run; the asked-for lap composed where one was asked for.

Find:

```kotlin
        // the show asks for afterwards (below).
        if (model.began) return@LaunchedEffect
```

Make it:

```kotlin
        // the show asks for afterwards (below). Nor has a panel that opens on a screen of first run: there the lap is the
        // reward that first run asks for itself, when the key lands and when the run folds to the bare field.
        if (model.began || model.due != null) return@LaunchedEffect
```

Find:

```kotlin
    // First run's show asks for the light itself, as the grid's wave lands (`OverlayModel.laps`): one lap, as built, which the
    // show's landing is cued by. Booklight's own typing and moves do not put it out; any key of the user's does, in 160 ms.
    if (model.began) {
```

Make it:

```kotlin
    // First run asks for the light itself (`OverlayModel.laps`): its show as the grid's wave lands, which the show's landing
    // is cued by; "Your key works" once the key on the glass has come up; the fold to the bare field. One lap each, as built.
    // Booklight's own typing and moves do not put it out; any key of the user's does, in 160 ms. Composed only in a panel
    // that began with the piece, or in which first run has asked for a lap.
    if (model.began || model.laps > 0) {
```

- [ ] **Step 6: Nobody else pays, and the key still lands in order**

```bash
grep -c "if (model.began || model.due != null) return@LaunchedEffect" app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt
grep -c "if (model.began || model.laps > 0) {" app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt
awk '/val keyed = byKey\(intent\)/ { k = NR } /val news = keyed/ { n = NR } /if \(keyed\) first\(app\) \{ FirstRun.keyLanded\(it\) \}/ { l = NR } /FirstRun.opening\(it, screenDp, plain\)/ { o = NR } /model = OverlayModel\(/ { m = NR } /if \(news\) model.keyOpened\(\)/ { q = NR } END { print (k < n && n < l && l < o && o < m && m < q) ? "IN-ORDER" : "OUT-OF-ORDER" }' app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
```

Expected: `1`, `1`, `IN-ORDER` (the key, whether it is news, the key landing, the opening counted, the model, the lap asked for).

- [ ] **Step 7: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/Strip.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt
git commit -m "First run: an answer shows as pressed; the key that lands is held down on the glass, comes up and takes its check, and the light runs its lap"
```

### Task 12: The lower edge, the fold and its lap, and the choices' keys

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`

**Interfaces:**
- Consumes: `Motion.ASK_LEAVES_MS`, `FOLD_CAP_MS`, `FOLD_LAP_MS`, `COACH_AFTER_MS`, `Motion.held(ms)` (Task 5); `OverlayModel.lasts`, `seen(made)` (Task 10), `answerStage(answer)` (Task 11), `laps`, `heldHeight`, `choicesUp`, `ended`, `bare`, `stage`, `playing`, `fold()`, `type(…)`, the place where a typed list lands, `fun enter(run: (Result, Action) -> Unit)`, `fun runRow(n: Int, run: (Result, Action) -> Unit)`; `Metrics.height(model)`; `RowSlots.sync(results, motion, held)` (Task 8); in `Panel.kt`: the keys for Enter and for Ctrl + digit; in `Field.kt`: the `esc` cap's `AnimatedVisibility`; in `FirstStage.kt`: `CoachLine(model, shown)`.
- Produces, in `OverlayModel`:
  - `private fun keep(high: Dp, ms: Long)`: the glass keeps a height for a moment at the most, then goes to its own. (The model already has a `fun keep()` without arguments, which is another thing and is not touched.)
  - `fun enter(made: Long, run: (Result, Action) -> Unit)`; `enter(run)` calls it with 0. `fun runRow(n: Int, made: Long = 0L, run: (Result, Action) -> Unit)`. `made` is when the key was pressed (uptime; 0: now).
  - `fold()` asks for one lap of light `FOLD_LAP_MS` after the fold, unless a letter was typed meanwhile.
  - The companion's `private const val LIST_LANDS_MS = 300L`.
- Produces elsewhere: `RowSlots.sync(results, motion, held, late: Int = 0)`; the field's `esc` cap comes back `FOLD_CAP_MS` late at the fold; a coach line that comes new waits `COACH_AFTER_MS`.

What `motion.md` §3 asks of the glass's lower edge and of the ending, and one mend from the last look at part 4.

**T7, typing on a stage.** A first letter typed on a stage took the stage away in that frame, and the list for it lands a frame or two later: in between, the glass set off from 252 dp for the bare field's 68 before the rows grew it again. Now the glass keeps the stage's height until the list lands (300 ms at the most), and goes from there to the list's height in one move. Typing is not held up: the stage is gone and the field has the letter in the same frame as before; only the lower edge waits.

**T9, the question gives way to the choices.** The lower edge rises 60 ms after the answer, once the question's words have begun to fade, so that it cuts none of them; the choices' first row waits the same 60 ms.

**T10, the fold.** After the choices the glass folds to the bare field. The field's `esc` cap, which a list hides, fades back 120 ms later, and 360 ms after the fold one lap of light runs round the bare field: the run's last word. Not where a letter was typed meanwhile: the light does not come over a list.

**A word and a line in one seat.** In the footer, for about 80 ms, the word that just happened ("Copied") and the coach line that takes its seat were drawn over each other (part 3's ledger). A coach line that comes new now waits for that fade.

**The choices' keys count by when they were pressed.** The stage's Enter and Tab are judged by the key event's own time (`seen(made)`); the choices' Enter and Ctrl + digit were judged by when they were handled, so a key pressed before the choices were in view and handled a long frame later could count. They now carry their time too.

- [ ] **Step 1: The lower edge, the fold's lap, and keys by their own time, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Eleven changes, in the file's order: `keep`; the question's height kept in `answerStage`; `fold` and `typedSince`; a letter typed on a stage; the typed list landed; `enter` with `made`; `runRow` with `made`; the companion.

Find:

```kotlin
    /** The piece ended for a typed character: the key's step has not been in view yet, and comes when the field is empty again. */
```

Make it:

```kotlin
    private var keeping: Job? = null

    /**
     * The glass keeps the height [high] for [ms] at the most, and then goes to its own: what stood in it is still fading
     * (the question as the choices come), or what comes has not landed yet (the list for a letter typed on a stage: the
     * lower edge then turns to that list's height from where it is, and never sets off for the bare field's first).
     */
    private fun keep(high: Dp, ms: Long) {
        heldHeight = high
        keeping?.cancel()
        keeping = scope.launch { delay(ms); heldHeight = null }
    }
    /** The piece ended for a typed character: the key's step has not been in view yet, and comes when the field is empty again. */
```

Find:

```kotlin
        val was = first()
        FirstRun.answers(was).indexOf(answer).takeIf { it >= 0 }?.let { at ->
```

Make it:

```kotlin
        val was = first()
        val asked = stage == FirstRun.Screen.Q
        val high = Metrics.height(this)
        FirstRun.answers(was).indexOf(answer).takeIf { it >= 0 }?.let { at ->
```

Find:

```kotlin
        // "Not now" on the key's step ends the run: the steps wait in the Booklight window, and the bare field says so.
```

Make it:

```kotlin
        // The question gives way to the choices: the lower edge rises only once the question's words have begun to fade, so
        // that it cuts none of them (motion.md T9).
        if (asked && choicesUp) keep(high, lasts(Motion.ASK_LEAVES_MS))
        // "Not now" on the key's step ends the run: the steps wait in the Booklight window, and the bare field says so.
```

Find:

```kotlin
    /** Enter on the choices with nothing selected: "Done". The rows go, and what is left is the bare field. */
```

Make it:

```kotlin
    /**
     * Enter on the choices with nothing selected: "Done". The rows go, and what is left is the bare field; once the glass
     * has folded to it the light runs its one lap round it (motion.md T10). Not where a letter was typed meanwhile.
     */
```

Find:

```kotlin
        resultsFor = For(null, query)
    }
```

Make it:

```kotlin
        resultsFor = For(null, query)
        scope.launch { delay(lasts(Motion.FOLD_LAP_MS)); if (ended && bare && !typedSince) laps++ }
        typedSince = false
    }
    /** Something was typed since the choices were folded away: the fold's light does not come over a list. */
    private var typedSince = false
```

Find:

```kotlin
        }
        leaveChoices()                      // a typed letter takes the place of first run's choices: they are done
```

Make it:

```kotlin
        }
        // And the lower edge turns to the typed list's height from where it is (motion.md T7): for the frame or two until
        // that list lands the glass keeps the stage's height.
        if (stage != null && text.isNotEmpty()) keep(Metrics.height(this), LIST_LANDS_MS)
        typedSince = true
        leaveChoices()                      // a typed letter takes the place of first run's choices: they are done
```

Find:

```kotlin
                val same = if (keep) local.indexOfFirst { it.id == was } else -1
```

Make it:

```kotlin
                // (The list for a letter typed on first run's stage has landed: the glass goes to its height from the stage's.)
                if (playing == null && heldHeight != null) { keeping?.cancel(); heldHeight = null }
                val same = if (keep) local.indexOfFirst { it.id == was } else -1
```

Find:

```kotlin
    fun enter(run: (Result, Action) -> Unit) {
```

Make it:

```kotlin
    fun enter(run: (Result, Action) -> Unit) = enter(0L, run)

    /** [made]: when the key was pressed (uptime; 0: now). First run's choices count a press by when it was made, as its stages do. */
    fun enter(made: Long, run: (Result, Action) -> Unit) {
```

Find:

```kotlin
        // First run's choices, like a stage, take a key only once they have been in view for a moment.
        if (choicesUp && !seen) return
```

Make it:

```kotlin
        // First run's choices, like a stage, take a key only once they have been in view for a moment: a key pressed before
        // that, and handled a long frame later, is still one pressed before that.
        if (choicesUp && !seen(made)) return
```

Find:

```kotlin
    fun runRow(n: Int, run: (Result, Action) -> Unit) {
        if (!onScreen || playing != null) return
        if (choicesUp && !seen) return      // first run's choices take a key only once they are in view, as Enter has it
```

Make it:

```kotlin
    fun runRow(n: Int, made: Long = 0L, run: (Result, Action) -> Unit) {
        if (!onScreen || playing != null) return
        if (choicesUp && !seen(made)) return      // first run's choices take a key only once they are in view, as Enter has it
```

Find:

```kotlin
        /** What the list is said to be for while first run's show performs: never what the field holds, so nothing takes its rows for the rows of what is typed. */
```

Make it:

```kotlin
        /** A list lands a frame or two after its letter: the glass waits no longer than this for one before it goes to its own height. */
        private const val LIST_LANDS_MS = 300L
        /** What the list is said to be for while first run's show performs: never what the field holds, so nothing takes its rows for the rows of what is typed. */
```

- [ ] **Step 2: The choices' rows wait for the question's words, in `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`.** Three changes: `sync` with `late`, and the body's call.

Find:

```kotlin
    /** [held]: the highlight stands in row one's seat already (first run's show, whose pill is the welcome's handle): a list that comes from nothing has that row whole at once. */
    fun sync(results: List<Result>, motion: Motion, held: Boolean = false): List<Slot> {
```

Make it:

```kotlin
    /**
     * [held]: the highlight stands in row one's seat already (first run's show, whose pill is the welcome's handle): a list
     * that comes from nothing has that row whole at once. [late]: a list that comes from nothing waits this long before
     * its first row rises (first run's choices, which come as the question's words fade: motion.md T9).
     */
    fun sync(results: List<Result>, motion: Motion, held: Boolean = false, late: Int = 0): List<Slot> {
```

Find:

```kotlin
            val s = live[seat(r)] ?: Slot(next++, r).also { it.delay = motion.stagger(if (fromNothing) i else fresh++); it.whole = held && fromNothing && i == 0 }
```

Make it:

```kotlin
            val s = live[seat(r)] ?: Slot(next++, r).also { it.delay = motion.stagger(if (fromNothing) i else fresh++) + (if (fromNothing) late else 0); it.whole = held && fromNothing && i == 0 }
```

Find:

```kotlin
    val rows = slots.sync(model.results, motion, held = model.playing == FirstRun.Playing.SHOW)
```

Make it:

```kotlin
    val rows = slots.sync(model.results, motion, held = model.playing == FirstRun.Playing.SHOW, late = if (model.choicesUp) motion.held(Motion.ASK_LEAVES_MS).toInt() else 0)
```

- [ ] **Step 3: Enter and Ctrl + digit carry their time, in `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** Two changes: the choices' Enter, and Ctrl + digit; one is a piece of one long line.

Find, in one long line:

```kotlin
model.tipEnter() else model.enter { row,
```

Make it:

```kotlin
model.tipEnter() else model.enter(e.nativeKeyEvent.eventTime) { row,
```

Find:

```kotlin
                    if (!again) model.runRow(n) { row, a -> go(row, a, false) }
```

Make it:

```kotlin
                    if (!again) model.runRow(n, e.nativeKeyEvent.eventTime) { row, a -> go(row, a, false) }
```

- [ ] **Step 4: The `esc` cap at the fold, in `app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt`.** One change.

Find:

```kotlin
        AnimatedVisibility(none, Modifier.graphicsLayer { alpha = minOf(ends(), most) }, enter = fadeIn(motion.fade(120)), exit = fadeOut(motion.fade(120))) { Keycap("esc", wide = true) }
```

Make it:

```kotlin
        // (At first run's fold it fades back a moment after the choices' rows have gone: motion.md T10.)
        AnimatedVisibility(none, Modifier.graphicsLayer { alpha = minOf(ends(), most) }, enter = fadeIn(motion.fade(120, if (model.ended) Motion.FOLD_CAP_MS else 0)), exit = fadeOut(motion.fade(120))) { Keycap("esc", wide = true) }
```

- [ ] **Step 5: A coach line that comes new waits for the word before it, in `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`.** One change, in `CoachLine`.

Find:

```kotlin
    AnimatedContent(line, transitionSpec = { (fadeIn(motion.fade(120)) togetherWith fadeOut(motion.fade(80))).using(null) }, contentAlignment = Alignment.CenterStart, label = "coach") { said ->
```

Make it:

```kotlin
    // (Where it takes the seat of a word that just happened, or comes with a new list, it waits for what stood there to
    // fade: the two are never drawn over each other. Its own words change where they stand, 80 out and 120 in.)
    AnimatedContent(line, transitionSpec = { (fadeIn(motion.fade(120, if (initialState == null) Motion.COACH_AFTER_MS else 0)) togetherWith fadeOut(motion.fade(80))).using(null) }, contentAlignment = Alignment.CenterStart, label = "coach") { said ->
```

- [ ] **Step 6: The height that is kept is always let go**

```bash
grep -c "keep(high, lasts(Motion.ASK_LEAVES_MS))\|keep(Metrics.height(this), LIST_LANDS_MS)" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
grep -c "keeping = scope.launch { delay(ms); heldHeight = null }" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
grep -c "e.nativeKeyEvent.eventTime" app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt
```

Expected: `2` (the two places a height is kept: the question's answer, a letter typed on a stage), `1` (whatever is kept is let go after its time), `3` (the stage's Enter, the choices' Enter, Ctrl + digit).

- [ ] **Step 7: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt
git commit -m "First run: the lower edge goes from a stage to what follows it without a visit to the bare field, the fold ends in one lap of light, and the choices' keys count by when they were pressed"
```

### Task 13: What a screen reader is told: each thing once, a switch as a switch

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`

**Interfaces:**
- Consumes: `OverlayModel.cameAt` (Task 10), `later` (Task 6), `greets`, `stage`, `choicesUp`, `ended`, `results`, `var onTell: (String) -> Unit` (set by the activity to the window's `announceForAccessibility`), `flipUsual()`, `land()`; `R.string.a11y_first_later`, `a11y_first_goes_on_change` (Task 6), `a11y_first_goes_on`, `a11y_selected` ("%1$s, %2$s"), `first_hello_title`; core `FirstRun.answers(s)`, `Body.Switch(on, word)`. In `FirstStage.kt`: the stage's announcement (a `LaunchedEffect(on)` with `greeted` kept in the composition), `said.keys`, `keySaid(model, on, state)`, `ChoicesSaid(model)`. In `Rows.kt`: `RowFrame(height, selected, label, actions, bare, onHover, onClick, onAction, calm, content)`, `ResultRow`'s `described`. In `OverlayActivity`: `staged` (this panel began with the opening piece), `onDestroy`.
- Produces, in `OverlayModel`:
  - `var said: List<String>` (set privately): the last sentences first run told a screen reader in this panel. `fun tell(text: String)`: says one, and keeps it.
  - `fun says(on: FirstRun.Screen, recipe: Boolean): Boolean`: true once for each coming of a screen. `fun saysRecipe(on: FirstRun.Screen): Boolean`: the recipe of that screen is still to be said; true once. `fun greeting(): Boolean`: the welcome's title is to be said first; true once in a panel.
  - `flipUsual()` tells the switch's new state.
- Produces elsewhere: `RowFrame(…, calm, on: Boolean? = null, content)`: a row that is a switch says so (`Role.Switch`, its state); `ChoicesSaid` also tells where the steps wait; `OverlayActivity` lands the key's step when a screen reader is switched on while the opening piece plays.

Five things a screen reader got wrong, from the reviews of parts 3 and 4. A reader cannot be driven on the test device, so what is said is kept by the model (`said`), where Task 15's hook can print it.

1. **The greeting, and a whole screen, could be said twice.** "Said once" was kept in the stage's composition memory, which is gone when a letter is typed over the stage and back when the field is emptied. The model keeps it now: a screen is said once for each coming (`says`, keyed by the screen and the moment it came), the greeting once in a panel (`greeting`).
2. **A lesson's recipe was not said if this device's example arrived after the lesson was first drawn.** It is said then, by itself (`saysRecipe`).
3. **The switch's row was a button with its state in its name**, and its new state was not said after a flip. It is a switch now (`Role.Switch`, `toggleableState`), named by its title alone, and the flip tells the new state.
4. **"Your key works" with two answers** (a run asked for again offers "Change the key" too) said only "Enter goes on".
5. **A reader switched on in the middle of the opening piece** met a field and rows that change about 25 times and a welcome with nothing to read. The piece ends there: the key's step lands and says itself. Listened for only in a panel that began with the piece.

And the new placeholder (the steps wait in the window, Task 6) is said as a sentence.

- [ ] **Step 1: What was said, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Three changes: a new section (`said`, `tell`, `says`, `saysRecipe`, `greeting`); the flip tells its state; the companion.

Find:

```kotlin
    }

    // ---- first run: asked for again, and where the steps wait
```

Make it:

```kotlin
    }

    // ---- first run: what a screen reader is told

    /** What first run has told a screen reader in this panel, the last few sentences as they were said: for the debug hook that prints them (`./bl debug first says`). */
    var said: List<String> = emptyList(); private set
    /** A sentence of first run's for a screen reader: said, and kept for that hook. */
    fun tell(text: String) { said = (said + text).takeLast(SAID); onTell(text) }
    /** The screen that was last said, with the moment it came; whether its recipe was said with it; and whether the greeting was said in this panel. */
    private var saidFor: Pair<FirstRun.Screen, Long>? = null
    private var recipeSaid = false
    private var greeted = false

    /**
     * The screen [on] is to be said now: true once for each coming of a screen. A stage that is only back, after something
     * was typed over it and deleted, is not said again. [recipe]: what is said includes its recipe.
     */
    fun says(on: FirstRun.Screen, recipe: Boolean): Boolean {
        val key = on to cameAt
        if (saidFor == key) return false
        saidFor = key; recipeSaid = recipe
        return true
    }
    /** The recipe of [on] is still to be said: this device's example was worked out after its lesson was said. True once. */
    fun saysRecipe(on: FirstRun.Screen): Boolean = (saidFor?.first == on && !recipeSaid).also { if (it) recipeSaid = true }
    /** The welcome's title is to be said before the first screen's words, where the opening piece is this opening's and is not played: once in a panel, whatever is typed and deleted in it. */
    fun greeting(): Boolean = (greets && !greeted).also { greeted = true }

    // ---- first run: asked for again, and where the steps wait
```

Find:

```kotlin
        selected = results.indexOfFirst { it.id == on }
    }
```

Make it:

```kotlin
        selected = results.indexOfFirst { it.id == on }
        // (A screen reader is told the switch's new state: the row does not change its place, and may not have the reader's focus.)
        results.firstOrNull { it.body is Body.Switch }?.let { tell(app.getString(R.string.a11y_selected, it.title, (it.body as Body.Switch).word)) }
    }
```

Find:

```kotlin
        /** A list lands a frame or two after its letter: the glass waits no longer than this for one before it goes to its own height. */
```

Make it:

```kotlin
        /** How many of first run's sentences for a screen reader are kept for the debug hook. */
        private const val SAID = 8
        /** A list lands a frame or two after its letter: the glass waits no longer than this for one before it goes to its own height. */
```

- [ ] **Step 2: The stage tells through the model, in `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`.** Seven changes: an import goes; the screen's announcement, and the recipe's; "Your key works" with two answers; `ChoicesSaid`.

Find:

```kotlin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
```

Make it:

```kotlin
import androidx.compose.ui.platform.LocalDensity
```

Find:

```kotlin
    // A screen reader is told each screen once, when it comes: where it is in the run, what it says, the keys, and how to act.
    val view = LocalView.current
```

Make it:

```kotlin
    // A screen reader is told each screen once, when it comes: where it is in the run, what it says, the keys, and how to act.
    // (Once for each coming: the model keeps what was said, so a stage that is only back after something was typed over
    // it and deleted is not said again, and neither is the greeting.)
```

Find:

```kotlin
    val greeted = remember { booleanArrayOf(false) }
    LaunchedEffect(on) {
        val first = model.greets && !greeted[0]
        greeted[0] = true
        view.announceForAccessibility(if (first) "$hello. $told" else told)
    }
```

Make it:

```kotlin
    val cameAt = model.cameAt
    LaunchedEffect(on, cameAt) {
        if (model.stage == on && model.says(on, recipe = said.keys != null)) model.tell(if (model.greeting()) "$hello. $told" else told)
    }
    // A lesson's recipe is this device's, and can be worked out a moment after the lesson was said: it is said then, by itself.
    LaunchedEffect(on, said.keys) { said.keys?.let { if (model.stage == on && model.saysRecipe(on)) model.tell(it) } }
```

Find:

```kotlin
        FirstRun.Screen.K4 -> R.string.a11y_first_goes_on
```

Make it:

```kotlin
        // (Asked for again, "Your key works" has a second answer.)
        FirstRun.Screen.K4 -> if (FirstRun.answers(state).size > 1) R.string.a11y_first_goes_on_change else R.string.a11y_first_goes_on
```

Find:

```kotlin
 * First run's choices arrive as a list with nothing selected, and its ending is the bare field with a placeholder: a
 * screen reader is told of each once (design.md §10). It draws nothing.
```

Make it:

```kotlin
 * First run's choices arrive as a list with nothing selected, and its ending is the bare field with a placeholder; so
 * is the opening in which the steps began to wait in the Booklight window. A screen reader is told of each once
 * (design.md §10). It draws nothing.
```

Find:

```kotlin
fun ChoicesSaid(model: OverlayModel) {
    val view = LocalView.current
```

Make it:

```kotlin
fun ChoicesSaid(model: OverlayModel) {
```

Find:

```kotlin
    LaunchedEffect(model.choicesUp, model.ended) {
        if (model.choicesUp) view.announceForAccessibility("$rows. $choices") else if (model.ended) view.announceForAccessibility(done)
    }
```

Make it:

```kotlin
    val later = stringResource(R.string.a11y_first_later)
    LaunchedEffect(model.choicesUp, model.ended) {
        if (model.choicesUp) model.tell("$rows. $choices") else if (model.ended) model.tell(done)
    }
    LaunchedEffect(model.later) { if (model.later) model.tell(later) }
```

- [ ] **Step 3: Where the steps wait is said too, in `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** One change.

Find:

```kotlin
    // So do first run's choices, and its ending is only a placeholder: a screen reader is told of each, once.
    if (model.choicesUp || model.ended) ChoicesSaid(model)
```

Make it:

```kotlin
    // So do first run's choices; its ending is only a placeholder, and so is the word that the steps wait in the window: a
    // screen reader is told of each, once.
    if (model.choicesUp || model.ended || model.later) ChoicesSaid(model)
```

- [ ] **Step 4: A switch is a switch, in `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`.** Five changes: two imports; `RowFrame` with `on`; its role; the switch's row named by its title, and handed its state. One is a piece of one long line.

Find:

```kotlin
import androidx.compose.ui.text.TextStyle
```

Make it:

```kotlin
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
```

Find, in one long line:

```kotlin
Boolean = false, content: @Composable RowScope.(Dp
```

Make it:

```kotlin
Boolean = false, /** The row is a switch, and whether it is on: said so to a screen reader. */ on: Boolean? = null, content: @Composable RowScope.(Dp
```

Find:

```kotlin
                this.selected = selected; role = Role.Button; contentDescription = label
```

Make it:

```kotlin
                this.selected = selected; contentDescription = label
                // (A row that holds a switch is one to a reader: it says "on" or "off" itself, also when it is flipped.)
                if (on != null) { role = Role.Switch; toggleableState = ToggleableState(on) } else role = Role.Button
```

Find:

```kotlin
    // (And a switch's row with its state: "Show your usual, Off".)
    val described = if (body is Body.Switch) "${r.title}, ${body.word}"
```

Make it:

```kotlin
    // (A switch's row is said by its name: its state is the switch's own to say.)
    val described = if (body is Body.Switch) r.title
```

Find:

```kotlin
    RowFrame(Metrics.rowHeight(r), selected, described, r.actions, bare = body is Body.Grid, onHover, onClick, onCustom, calm) { tall ->
```

Make it:

```kotlin
    RowFrame(Metrics.rowHeight(r), selected, described, r.actions, bare = body is Body.Grid, onHover, onClick, onCustom, calm, on = (body as? Body.Switch)?.on) { tall ->
```

- [ ] **Step 5: A reader that comes in the middle of the piece, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Three changes: the listener; added in `onCreate` where the panel began with the piece; removed in `onDestroy`.

Find:

```kotlin
    private var described: Pair<Intent, FirstRun.Start>? = null
```

Make it:

```kotlin
    private var described: Pair<Intent, FirstRun.Start>? = null
    /**
     * A screen reader is switched on while first run's opening piece plays: the piece is a picture and a performance, with
     * nothing in it to read, so it ends there and the key's step stands, which says itself. Listened for only in a panel
     * that began with the piece, and until that panel goes.
     */
    private val readerCame = android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener { on -> if (on) model.land() }
```

Find:

```kotlin
        // The system's own words for its dialog are read before the stage that quotes them comes: the key's. (Asked of what
```

Make it:

```kotlin
        if (staged) getSystemService(android.view.accessibility.AccessibilityManager::class.java)?.addTouchExplorationStateChangeListener(readerCame)
        // The system's own words for its dialog are read before the stage that quotes them comes: the key's. (Asked of what
```

Find:

```kotlin
    override fun onDestroy() {
```

Make it:

```kotlin
    override fun onDestroy() {
        if (staged) getSystemService(android.view.accessibility.AccessibilityManager::class.java)?.removeTouchExplorationStateChangeListener(readerCame)
```

- [ ] **Step 6: The stage says nothing behind the model's back, and nobody else listens**

```bash
grep -n "announceForAccessibility\|LocalView" app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt || echo GONE
grep -c "if (staged) getSystemService(android.view.accessibility.AccessibilityManager::class.java)" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
grep -c "Role.Switch" app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt
```

Expected: `GONE`, `2` (the listener is added and removed only in a panel that began with the piece), `1`.

- [ ] **Step 7: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
git commit -m "First run: a screen reader is told each screen once, the greeting once, a late recipe by itself, and a switch as a switch; one switched on in the opening piece ends it"
```

### Task 14: A recipe has its room, and a name is never cut inside a character

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunRecipeTest.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`

**Interfaces:**
- Consumes (in `object FirstRun`, part 3): `const val LEAST_LETTERS = 3`, `MOST_LETTERS = 4`, `RECIPE_WORD = 8`, `fun starts(name: String): List<String>`, `fun recipe(s: State, example: Example, sum: String): List<Part>`, `Part.Typed`, `Example`. In `FirstStage.kt` (Task 10): the recipe's `Row` in `Keys`. In `OverlayModel`: the placeholder of lesson 3, which cuts the example's word.
- Produces:
  - `fun FirstRun.head(text: String, n: Int): String`: the first `n` characters of `text` as a reader counts them, never half of one. `starts` and `recipe` cut by it.
  - In `FirstStage.kt`: `private val RECIPE_END = 16.dp`, `private fun Modifier.fitsWidth(end: Dp): Modifier`: what is wider than its room is drawn smaller as a whole, from its left end.

Two small things part 3 left.

**A name cut in half.** The lessons' recipe shows the first letters of an app's name, and `take(n)` counts the text's units: a name that begins with an emoji was cut in the middle of it, and the glass showed a broken character. `head` counts what a reader counts: a character of two units is kept whole or left out, and what belongs to a character stays with it (a mark over a letter, a skin tone, a variation selector, whatever a joiner joins, the second half of a flag). It uses only `java.lang.Character`, which core may.

**A recipe too wide for its room.** The band holds one line. At a larger font scale, with the longest words (lesson 3 in German is the tight case), a recipe could run under the answers. It is now measured at its own width and, where that is more than its room less 16 dp, drawn smaller as a whole from its left end: never cut, never wrapped, never over the answers. What fits is drawn as it was. Its size in the layout is known before anything moves: the parts inside still arrive by their marks.

- [ ] **Step 1: Write the failing test, in `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunRecipeTest.kt`.** One change: a test after the last one. Its texts hold emoji, a joiner and a combining mark: use them exactly as they are written.

Find:

```kotlin
    }

    @Test fun noRecipeWhereNoLessonStands() {
```

Make it:

```kotlin
    }

    /**
     * A name that begins with an emoji, or holds a letter that takes two of the text's units: a recipe never shows half
     * of one, and what belongs to a character stays with it.
     */
    @Test fun aNameIsNeverCutInsideACharacter() {
        val grin = "😀"                                          // one character, two units
        assertEquals(grin + "ab", FirstRun.head(grin + "abc", 3))
        assertEquals(grin.repeat(3), FirstRun.head(grin.repeat(5), 3))
        // A skin tone, a variation selector, what a joiner joins, a mark over a letter, the second half of a flag.
        val thumb = "👍🏽"
        assertEquals(thumb + "a", FirstRun.head(thumb + "ab", 2))
        val heart = "❤️"
        assertEquals(heart, FirstRun.head(heart + "abc", 1))
        val family = "👨‍👩‍👧"
        assertEquals(family, FirstRun.head(family + "x", 1))
        assertEquals("ét", FirstRun.head("été", 2))
        val flags = "🇩🇪🇫🇷"
        assertEquals(flags.substring(0, 4), FirstRun.head(flags, 1))
        assertEquals(flags, FirstRun.head(flags + "x", 2))
        // Plain letters are cut as they were, and nothing is made longer.
        assertEquals("yout", FirstRun.head("youtube", 4))
        assertEquals("tv", FirstRun.head("tv", 4))
        assertEquals("", FirstRun.head("abc", 0))
        assertEquals("", FirstRun.head("", 3))
        // The starts of a name and a recipe's typed parts are cut by it.
        assertEquals(listOf(grin + "mu", grin + "mus"), FirstRun.starts(grin + "Music"))
        assertEquals(listOf(grin + grin), FirstRun.starts(grin + grin))
        val e = Example(grin.repeat(6), grin.repeat(6), enter = true, word = grin.repeat(12))
        assertEquals(Part.Typed(grin.repeat(4)), FirstRun.recipe(l2, e, sum).first())
        assertEquals(Part.Typed(grin.repeat(8)), FirstRun.recipe(l3, e, sum)[3])
    }

    @Test fun noRecipeWhereNoLessonStands() {
```

- [ ] **Step 2: Run it and see it fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunRecipeTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'head'` among its `e:` lines.

- [ ] **Step 3: Whole characters, in `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`.** Three changes: `head` above `starts`; `starts`; a recipe's typed part.

Find:

```kotlin
    const val RECIPE_WORD = 8

    /**
     * The starts of an app's [name] that a recipe may show, the shortest first: of its first word (a
```

Make it:

```kotlin
    const val RECIPE_WORD = 8

    /**
     * The first [n] characters of [text] as a reader counts them, never half of one. A character that
     * takes two of the text's units (an emoji, a letter outside the common ones) is kept whole or left
     * out, and what belongs to a character stays with it: a mark over a letter, a skin tone, a
     * variation selector, whatever a joiner joins to it, the second half of a flag. (An app's name may
     * begin with an emoji; cut by units, its first letters ended in half of one.)
     */
    fun head(text: String, n: Int): String {
        var end = 0
        var count = 0
        var half = false      // the character before was the first half of a flag
        while (end < text.length) {
            val cp = text.codePointAt(end)
            val flag = cp in 0x1F1E6..0x1F1FF
            val joined = end > 0 && text.codePointBefore(end) == JOINER
            val belongs = joined || cp == JOINER || cp in 0xFE00..0xFE0F || cp in 0x1F3FB..0x1F3FF || (flag && half) ||
                Character.getType(cp).let { it == Character.NON_SPACING_MARK.toInt() || it == Character.ENCLOSING_MARK.toInt() || it == Character.COMBINING_SPACING_MARK.toInt() }
            if (!belongs) {
                if (count == n) break
                count++
                half = flag
            } else if (flag) half = false
            end += Character.charCount(cp)
        }
        return text.substring(0, end)
    }

    /** What joins two characters into one (a family, a flag with a sign on it). */
    private const val JOINER = 0x200D

    /**
     * The starts of an app's [name] that a recipe may show, the shortest first: of its first word (a
```

Find:

```kotlin
            word.length <= LEAST_LETTERS -> listOf(word)
            else -> (LEAST_LETTERS..minOf(MOST_LETTERS, word.length)).map { word.take(it) }
```

Make it:

```kotlin
            head(word, LEAST_LETTERS) == word -> listOf(word)
            else -> (LEAST_LETTERS..MOST_LETTERS).map { head(word, it) }.distinct()
```

Find:

```kotlin
        fun typed(text: String, most: Int): Part? = text.take(most).takeIf { it.isNotEmpty() }?.let { Part.Typed(it) }
```

Make it:

```kotlin
        fun typed(text: String, most: Int): Part? = head(text, most).takeIf { it.isNotEmpty() }?.let { Part.Typed(it) }
```

- [ ] **Step 4: Run it and see it pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunRecipeTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: A recipe that fits its room, in `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`.** Three changes: two imports; the recipe's row; `RECIPE_END` and `fitsWidth` after `Keys`.

Find:

```kotlin
import androidx.compose.ui.graphics.graphicsLayer
```

Make it:

```kotlin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
```

Find:

```kotlin
            is Picture.Recipe -> Row(verticalAlignment = Alignment.CenterVertically) {
```

Make it:

```kotlin
            is Picture.Recipe -> Row(Modifier.fitsWidth(RECIPE_END), verticalAlignment = Alignment.CenterVertically) {
```

Find:

```kotlin
}

/**
 * A typed part of a recipe, in the field's own type. It is laid out whole, once: its width never changes. Where the
```

Make it:

```kotlin
}

/** A recipe ends this far before its room does: the `tab` cap lies over the band's free end. */
private val RECIPE_END = 16.dp

/**
 * What is wider than its room is drawn smaller, as a whole, from its left end and about its own centre line: a recipe
 * is one line, and is never cut, never wrapped and never laid over the answers (a larger type size; lesson 3 in German
 * is the tight case). What fits is drawn as it is. [end]: how much of the room's end is kept free.
 */
private fun Modifier.fitsWidth(end: Dp): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
    val room = if (constraints.hasBoundedWidth) (constraints.maxWidth - end.roundToPx()).coerceAtLeast(1) else p.width
    val small = if (p.width > room) room.toFloat() / p.width else 1f
    layout(minOf(p.width, room), p.height) {
        p.placeWithLayer(0, 0) { scaleX = small; scaleY = small; transformOrigin = TransformOrigin(0f, 0.5f) }
    }
}

/**
 * A typed part of a recipe, in the field's own type. It is laid out whole, once: its width never changes. Where the
```

- [ ] **Step 6: The placeholder cuts as the recipe does, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** One change.

Find:

```kotlin
        FirstRun.Screen.L3 -> example?.search?.take(FirstRun.MOST_LETTERS)?.takeIf { it.isNotEmpty() }?.let { app.getString(R.string.first_search_hint, it) }
```

Make it:

```kotlin
        FirstRun.Screen.L3 -> example?.search?.let { FirstRun.head(it, FirstRun.MOST_LETTERS) }?.takeIf { it.isNotEmpty() }?.let { app.getString(R.string.first_search_hint, it) }
```

- [ ] **Step 7: Nothing of first run cuts a name by units any more**

```bash
grep -n "\.take(most)\|word.take(it)\|\.take(FirstRun.MOST_LETTERS)" core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt || echo GONE
grep -rn "^import android" core/src/main || echo CLEAN
```

Expected: `GONE`, `CLEAN`.

- [ ] **Step 8: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 9: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunRecipeTest.kt app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
git commit -m "First run: a recipe never shows half a character of a name, and one too wide for its room is drawn smaller, whole"
```

### Task 15: The hooks, and the last pass's checks for a device

**Files:**
- Modify: `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `bl`
- Modify: `CLAUDE.md`
- Create: `docs/research/first-run-last-pass.md`

**Interfaces:**
- Consumes: `OverlayModel.again()`, `later` (Task 6); `comes`, `marks`, `stageAt`, `seen`, the private `came(how)` (Task 10); `keyLanded(under)`, `keyDown`, `landing`, `stagePressed` (Task 11); `said` (Task 13); `laps`, `stage`, `playing`; core `FirstRun.again(s, overture)`, `SetDown.Comes`; `Prefs.firstRun { … }`; in `DebugReceiver.kt`: its header comment, `dump`, the `first`, `pref` and `turn` cases, `out(text)`, the panel it reaches (`OverlayActivity.current`), the settings' `showSums`.
- Produces:
  - `fun OverlayModel.stageAs(how: SetDown.Comes?, at: Float?)`: debug builds: the screen that stands comes again in the way `how` says (null: as it came), and its clock stands at `at` ms, or runs (null). Nothing that is kept changes.
  - `./bl debug first again`: writes what the window's "First steps" row writes (`FirstRun.again(it, overture = true)` through `Prefs.firstRun`); the next `./bl open stay` is the panel that row starts.
  - `./bl debug first stage at MS`, `first stage`, `first stage as whole|set_down|back|turn|after_list|lands [at MS]`: the stage's clock stands still, runs, or the screen comes again that way.
  - `./bl debug first press [under]`: the user's key lands on the screen that waits for it, in view or as under the system's dialog.
  - `./bl debug first says`: what first run told a screen reader in this panel, and what every node of the panel says to one (read from the semantics the app builds; no accessibility service is asked or changed).
  - `./bl debug first trace N`: one log line for each of the next N frames (ms, the window's height, the stage's clock, `seen`, the key on the glass, laps).
  - `./bl debug pref sums on|off`; `./bl debug turn MS enter` (a closing panel turned round after MS, with Enter in the same moment).
  - `./bl debug dump` also says `comes=`, `lead=`, `end=`, `seen`, `later`, `keydown`, `landing`, and `laps=` once a lap was asked for.
  - `docs/research/first-run-last-pass.md`: sixty-one checks, every answer "not run".

No engineer has seen what this part draws, and none will: the checks are the coordinator's, on the test device, by adb. This task gives the checks what they need, and writes them down.

**The hooks.** A film cannot be judged from stills, and a film's own times are lost when it is cut: so a set-down can be stood still at a named ms (`first stage at MS`), a screen can be made to come again in any of the six ways without walking the run there (`first stage as …`), and a time that must be measured is a log line (`first trace N`). The key that lands needs the system's dialog and a real key: `first press` lands it as the dialog would. A screen reader cannot be driven on the test device without changing its accessibility settings: `first says` prints what the model told one and walks the semantics tree of the panel's own compose view, in the app's own process.

**The checks.** `first-run-last-pass.md` has this part's checks (1 to 37) and then every check the three earlier lists left open (38 to 61), each with what to type and what to look for, and each marked **[C]** (the coordinator's, by adb and the hooks) or **[A]** (Alex's own: a click on the launcher's icon or another app's window, another keyboard, the system's language or font scale, a shortcut set where Booklight has none yet, a screen reader's voice, the HP Googlebook, a person's judgement). `./bl debug guide` is in check 2. The two things `first-run-welcome.md` still has open are checks 57 (a space that comes by the input method) and 58 (Enter in the third of a second after a closing panel is turned round, which no key sent from outside can make: the hook `turn MS enter` makes it from inside).

Do not install the build and do not run a check.

- [ ] **Step 1: The hooks, in `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`.** Eleven changes, in the file's order: the header; `dump`; the `first` case (`again`, `stage`, `press`, `says`, `trace`); `pref sums`; `turn MS enter`; `spoken`. Three are a piece of one long line.

Find:

```kotlin
import io.github.kuscher.booklight.core.FirstRun
```

Make it:

```kotlin
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import io.github.kuscher.booklight.core.FirstRun
```

Find:

```kotlin
 *   think on|off (the light of the model at work, without the model) | turn MS (close, and the key again MS later)
```

Make it:

```kotlin
 *   think on|off (the light of the model at work, without the model) | turn MS (close, and the key again MS later) |
 *   turn MS enter (and Enter in the same moment: what it answered on first run's stage)
```

Find:

```kotlin
 *   While the piece plays, `key …` is taken by it first, as the keys are; dump says `playing=`, `cast=ready|none`, `laps=`, `gliding`.
```

Make it:

```kotlin
 *   While the piece plays, `key …` is taken by it first, as the keys are; dump says `playing=`, `cast=ready|none`, `laps=`, `gliding`.
 *   first again (as the window's "First steps" row writes it: the next plain panel plays the whole of it, or goes on with an unfinished run) |
 *   first stage at MS (the stage's clock stands at that ms after the screen came: a set-down as a still) | first stage (it runs again) |
 *   first stage as whole|set_down|back|turn|after_list|lands [at MS] (the screen that stands comes again that way, now) |
 *   first press [under] (the user's key lands on the screen that waits for it, in view or as under the system's dialog) |
 *   first says (what first run told a screen reader in this panel, and what every node of the panel says to one) |
 *   first trace N (the next N frames to the log: ms, the window's height, the stage's clock, whether it takes a key, the key on the glass, laps) |
 *   pref sums on|off ("Show sums": for a run asked for again where they are off); dump says `comes=`, `lead=`, `end=`, `seen`, `later`, `keydown`, `landing`.
```

Find:

```kotlin
                    "zero" -> app.prefs.update { it.copy(zero = v == "on") }
```

Make it:

```kotlin
                    "zero" -> app.prefs.update { it.copy(zero = v == "on") }
                    "sums" -> app.prefs.update { it.copy(showSums = v == "on") }
```

Find, in one long line:

```kotlin
zero=${it.zero} hidden=${it.zeroHidden.size
```

Make it:

```kotlin
zero=${it.zero} sums=${it.showSums} hidden=${it.zeroHidden.size
```

Find:

```kotlin
            // The panel is put away and the key comes again MS later: the turn, at a moment adb could never hit.
            "turn" -> main.post { act?.let { a -> a.close(); main.postDelayed({ a.turn(Intent()) }, arg.toLongOrNull() ?: 60L) }; out("ok") }
```

Make it:

```kotlin
            // The panel is put away and the key comes again MS later: the turn, at a moment adb could never hit. `turn MS enter`: and
            // Enter in that same moment, as a hand does that pressed it with the key; what it answered on the stage is said (nothing,
            // if a screen takes a key only once it has been in view).
            "turn" -> main.post {
                val words = arg.split(' ')
                val enter = words.getOrNull(1) == "enter"
                act?.let { a ->
                    a.close()
                    main.postDelayed({
                        a.turn(Intent())
                        if (enter) out("turned; Enter in the same moment answered ${a.model.stageEnter()?.also { a.stage(it) } ?: "nothing"}")
                    }, words[0].toLongOrNull() ?: 60L)
                }
                if (!enter || act == null) out("ok")
            }
```

Find, in one long line:

```kotlin
""}${if (m.gliding) " gliding" else ""} due=${m.due?.name ?: "none
```

Make it:

```kotlin
if (m.laps > 0) " laps=" + m.laps else ""}${if (m.gliding) " gliding" else ""} due=${m.due?.name ?: "none"}${if (m.stage != null) " comes=" + m.comes.name.lowercase() + " lead=" + m.marks.lead + " end=" + m.marks.end + (if (m.seen) " seen" else "") else ""}${if (m.later) " later" else ""}${if (m.keyDown) " keydown" else ""}${if (m.landing) " landing" else "
```

Find:

```kotlin
                    // The opening piece in the open panel, whatever the stored state is: `welcome` plays it from the gate, `welcome at MS`
```

Make it:

```kotlin
                    // As the Booklight window's "First steps" row writes it: a finished run is asked for again with its opening piece,
                    // an unfinished one goes on where it stopped. The next plain panel (`./bl open stay`) then begins as the row's does.
                    "again" -> app.prefs.firstRun { FirstRun.again(it, overture = true) }
                    // The stage's own clock (docs/design/first-run/motion.md §3): `stage at MS` stands it still at that ms after the
                    // screen came, `stage` lets it run again, `stage as HOW [at MS]` has the screen that stands come again in that way
                    // (whole, set_down, back, turn, after_list, lands), in this moment. Nothing that is kept changes.
                    "stage" -> {
                        val m = act?.model ?: return out("no panel")
                        val how = io.github.kuscher.booklight.core.SetDown.Comes.entries.firstOrNull { words.getOrNull(1) == "as" && it.name.equals(words.getOrNull(2), ignoreCase = true) }
                        if (words.getOrNull(1) == "as" && how == null) return out(io.github.kuscher.booklight.core.SetDown.Comes.entries.joinToString(" ") { it.name.lowercase() })
                        val at = words.indexOf("at").takeIf { it >= 0 }?.let { words.getOrNull(it + 1)?.toFloatOrNull() ?: return out("stage at MS | stage | stage as HOW [at MS]") }
                        main.post { m.stageAs(how, at) }
                        return out("stage=${m.stage?.name ?: "none"}${how?.let { " comes as " + it.name.lowercase() } ?: ""}${at?.let { " stands at $it" } ?: " runs"}")
                    }
                    // The user's key lands on the screen that waits for it, in the open panel: in view, or (`press under`) as it does
                    // under the system's dialog, where "your key" is held down until the dialog has gone. `key seen` only: no dialog.
                    "press" -> {
                        val m = act?.model ?: return out("no panel")
                        if (!m.awaitsKey) return out("no screen waits for the key here: `first at k2` first")
                        main.post { m.keyLanded(under = words.getOrNull(1) == "under") }
                        return out("the key lands${if (words.getOrNull(1) == "under") ", as under the dialog" else ""}")
                    }
                    // What first run told a screen reader in this panel (the last sentences, as they were said), and what the panel
                    // says to one now: every node of its semantics, merged as a reader meets them, a line each. Rows are among them:
                    // they name apps of the device, and are not for any file of this repo.
                    "says" -> {
                        val a = act ?: return out("no panel")
                        main.post {
                            val lines = ArrayList<String>()
                            fun walk(v: android.view.View) {
                                (v as? androidx.compose.ui.node.RootForTest)?.let { root -> root.semanticsOwner.getAllSemanticsNodes(mergingEnabled = true).mapNotNullTo(lines, ::spoken) }
                                (v as? android.view.ViewGroup)?.let { g -> for (i in 0 until g.childCount) walk(g.getChildAt(i)) }
                            }
                            walk(a.window.decorView)
                            a.model.said.forEach { Log.i(BooklightApp.TAG, "says told: $it") }
                            lines.forEach { Log.i(BooklightApp.TAG, "says node: $it") }
                            out(("told=" + a.model.said.joinToString(" | ").ifEmpty { "nothing" } + " || nodes=" + lines.joinToString(" | ")).let { if (it.length <= 3500) it else it.take(3500) + " … (./bl logs has every line)" })
                        }
                        return
                    }
                    // `trace N`: the next N frames, a log line each (`./bl logs`): ms since the first of them, the window's height in px,
                    // what plays and what stands, the stage's clock in ms since its screen came into view, whether it takes a key yet,
                    // the key on the glass, the laps asked for, a height that is held. For times a film cannot give.
                    "trace" -> {
                        val a = act ?: return out("no panel")
                        val n = (words.getOrNull(1)?.toIntOrNull() ?: 120).coerceIn(1, 1200)
                        val frames = android.view.Choreographer.getInstance()
                        var left = n
                        var first = 0L
                        main.post {
                            frames.postFrameCallback(object : android.view.Choreographer.FrameCallback {
                                override fun doFrame(t: Long) {
                                    val m = a.model
                                    if (first == 0L) first = t
                                    Log.i(BooklightApp.TAG, "first t=${(t - first) / 1_000_000} h=${a.window.decorView.height} playing=${m.playing?.name?.lowercase() ?: "none"} stage=${m.stage?.name ?: "none"} " +
                                        "comes=${m.comes.name.lowercase()} clock=${t / 1_000_000 - m.viewFrom} lead=${m.marks.lead} seen=${m.seen} keydown=${m.keyDown} checked=${m.keyChecked} laps=${m.laps} held=${m.heldHeight?.value?.toInt() ?: "-"} rows=${m.results.size}")
                                    if (--left > 0 && !a.isFinishing) frames.postFrameCallback(this)
                                }
                            })
                        }
                        return out("tracing $n frames (./bl logs)")
                    }
                    // The opening piece in the open panel, whatever the stored state is: `welcome` plays it from the gate, `welcome at MS`
```

Find:

```kotlin
                        FirstRun.at(app.prefs.now.firstRun(), screen) ?: return out("${screen.name} cannot stand: K4 needs a key (`first key`), Q needs suggestions off")
```

Make it:

```kotlin
                        FirstRun.at(app.prefs.now.firstRun(), screen) ?: return out("${screen.name} cannot stand: K4 needs a key (`first key`), L4 needs sums on (`pref sums on`), Q needs suggestions off")
```

Find, in one long line:

```kotlin
resume | key | helper | opened | shown | answer NAME | ran open|search|sum | at SCREEN | older | welcome [at MS] | show [at NAME] | land
```

Make it:

```kotlin
again | resume | key | helper | opened | shown | answer NAME | ran open|search|sum | at SCREEN | older | welcome [at MS] | show [at NAME] | land | stage [as HOW] [at MS] | press [under] | says | trace N
```

Find:

```kotlin
    }

    /** A row in one line: its title, kind, what it holds, and its actions with the armed one marked. */
```

Make it:

```kotlin
    }

    /**
     * One node of the panel as a screen reader meets it: its role, what it says (a description, else its text), its state,
     * whether it is announced when it changes, and what it offers besides a click. Null for a node that says nothing.
     */
    private fun spoken(n: androidx.compose.ui.semantics.SemanticsNode): String? {
        val c = n.config
        val words = c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)?.joinToString(" ")
            ?: c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.joinToString(" ") { it.text }
            ?: c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.EditableText)?.text
        val role = c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Role)?.toString()?.lowercase()
        if (words.isNullOrBlank() && role == null) return null
        return listOfNotNull(
            role, words?.let { "'$it'" },
            c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ToggleableState)?.name?.lowercase(),
            "selected".takeIf { c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Selected) == true },
            "live".takeIf { c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.LiveRegion) != null },
            c.getOrNull(androidx.compose.ui.semantics.SemanticsActions.CustomActions)?.joinToString(",", "+[", "]") { it.label },
        ).joinToString(" ")
    }

    /** A row in one line: its title, kind, what it holds, and its actions with the armed one marked. */
```

- [ ] **Step 2: A screen that comes again for a picture, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** One change: `stageAs`, after `opening`.

Find:

```kotlin
    }

    /** The activity says how this panel opens: how long after the gate the band of a screen begins that is set down in it. Said before the glass opens. */
```

Make it:

```kotlin
    }

    /**
     * Debug builds (`./bl debug first stage …`): the screen that stands comes again in the way [how] says, as if in this
     * moment (null: as it came); and its clock stands at [at] ms, or runs (null). For a picture of a set-down at a named
     * moment. Nothing that is kept changes.
     */
    fun stageAs(how: SetDown.Comes?, at: Float?) {
        stageAt = at
        if (how != null) came(how)
    }

    /** The activity says how this panel opens: how long after the gate the band of a screen begins that is set down in it. Said before the glass opens. */
```

- [ ] **Step 3: Say them in `bl`'s header.** One change.

Find:

```bash
# While a lesson stands (l2, l3, l4), `./bl debug key enter` on an app's row is practice, as the key is: nothing opens.
```

Make it:

```bash
#   ./bl debug first again               as the window's "First steps" row writes it (then `./bl open stay` is the panel that row starts)
#   ./bl debug first stage at MS         the stage's clock stands at that ms after its screen came (a set-down as a still); `first stage` lets it run
#   ./bl debug first stage as whole|set_down|back|turn|after_list|lands [at MS]     the screen that stands comes again that way, now
#   ./bl debug first press [under]       the user's key lands on the screen that waits for it: in view, or as under the system's dialog
#   ./bl debug first says                what first run told a screen reader in this panel, and what every node of the panel says to one
#   ./bl debug first trace N             the next N frames in `./bl logs`: ms, the window's height, the stage's clock, `seen`, the key on the glass, laps
#   ./bl debug pref sums on|off          "Show sums" (a run asked for again leaves the sum out where they are off)
# `dump` also says `comes=`, `lead=`, `end=` and `seen` for the stage that stands (how it came, when its answers begin to show, when it
# is at rest, whether it takes a key yet), `later` (the bare field says where the steps wait), `keydown` and `landing`.
# While a lesson stands (l2, l3, l4), `./bl debug key enter` on an app's row is practice, as the key is: nothing opens.
```

- [ ] **Step 4: Say in `CLAUDE.md` what there is now, how to drive it, and what to mind.** Four changes: the layout's lines for core (a piece of one long line), the first-run paragraph of the dev loop, and the gotchas.

Find, in one long line:

```markdown
the highlight's way into the key's step),
```

Make it:

```markdown
the highlight's way into the key's step; `SetDown.kt` says when each part of a stage comes, for every way a screen can come (set down part by part, come back to, in another's place, after a list, at the piece's landing), and the stage is drawn by those marks on one clock; "First steps" asks for the run again, as a command in the panel and as a row on the window's Start page),
```

Find:

```markdown
- Pictures: captures in `docs/design/captures/` → `tools/store_scenes.py` (a drawn desktop behind them) →
```

Make it:

```markdown
  The stage between its screens (`motion.md` §3): `./bl debug first stage at MS` stands the stage's clock still at that ms after its
  screen came, `first stage` lets it run, and `first stage as whole|set_down|back|turn|after_list|lands [at MS]` has the screen that stands
  come again in that way; `first at SCREEN` in an open panel is a real turn from the screen that stands. `first press [under]` lands the
  user's key on the screen that waits for it (in view, or as under the system's dialog); `first again` writes what the window's "First
  steps" row writes; `first says` prints what first run told a screen reader and what every node of the panel says to one; `first trace N`
  logs the next N frames (ms, the window's height, the stage's clock, whether it takes a key, the key on the glass, laps); `pref sums
  on|off` is "Show sums". `dump` says `comes=`, `lead=`, `end=` and `seen` for the stage that stands, `later`, `keydown` and `landing`.
  The last pass on a device, with every check the three earlier lists left open: `docs/research/first-run-last-pass.md`.
- Pictures: captures in `docs/design/captures/` → `tools/store_scenes.py` (a drawn desktop behind them) →
```

Find:

```markdown
- A screen of first run takes a key or a click only once it has been in view for 350 ms (`OverlayModel.seen`: the glass open,
  and the screen not just come): the Enter that skipped a lesson, pressed again at once, answers nothing on the screen that took
  its place. On the question the first answer is "Not now" (core `FirstRun.answers`): Tab then Enter, the keys that skip, agree to
  nothing. A hook that changes the screen and a key hook need that moment between them.
```

Make it:

```markdown
- A screen of first run takes a key or a click only once it has been in view for 350 ms (`OverlayModel.seen`, core `FirstRun.inView`:
  the glass open, the screen not just come, and the answer Enter would run begun to show): the Enter that skipped a lesson, pressed
  again at once, answers nothing on the screen that took its place, and nothing is answered that is still being set down. On the
  question the first answer is "Not now" (core `FirstRun.answers`): Tab then Enter, the keys that skip, agree to nothing. A hook that
  changes the screen and a key hook need that moment between them.
- A screen of first run comes onto the glass by marks (core `SetDown.marks` with `Motion.kt`'s times, as `PACE`): the model works them
  out in the moment the screen comes (`OverlayModel.came`: at its making, in `rearm`, `giveWay`, `again`, `land`, `turned`, `closing`)
  and counts by them; `overlay/FirstStage.kt` draws every part by its mark on one clock that starts at `OverlayModel.viewFrom`. Whatever
  makes a screen come must say so through `came`, or the screen is drawn and counted by the marks of the one before. The parts move as
  layers over a layout that never changes: nothing may move the answers' place (the highlight's way into the armed answer reads it once),
  and what gives way is drawn by the clock and marks of its last moment (`own`).
- "First steps" is the panel's own: its command carries core `FirstRun.AGAIN`, which `OverlayActivity.run` catches before the executor
  (`OverlayModel.again`: the run stands in the panel that is open, without the opening piece). The window's row writes the state and
  starts a plain panel by `ACTION_PANEL`, as a row of the Commands page does for an example: Booklight starts its panel in no other
  way, and never over another app's window.
```

Find:

```markdown
  key 350 ms after the landing (`cameAt`, `seen`): the piece is `LANDING` for that long. A panel that closes ends its piece
```

Make it:

```markdown
  key 350 ms after its armed answer has begun to show (`seen`): the piece is `LANDING` until then. The piece stops performing in one
  place (`stop`), whichever way it ends. A panel that closes ends its piece
```

- [ ] **Step 5: Write `docs/research/first-run-last-pass.md`**

```markdown
# First run: the last pass on a device (part 5)

*The checks for part 5 of the first-run plan (`docs/superpowers/plans/2026-10-05-first-run-5-the-last-part.md`), and
after them every check the three earlier lists left open (`first-run-key.md`, `first-run-lessons.md`,
`first-run-welcome.md`), gathered into one. Whoever runs a check writes its answer under it, with the date and "the
Lenovo Googlebook" or "the HP Googlebook". Until then an answer reads "not run".*

**Who runs what.** A check marked **[C]** is the coordinator's: it is run on the test device by adb and the debug
hooks. A check marked **[A]** is Alex's own: it needs a click on the launcher's icon or on another app's window,
another keyboard, the system's language or font scale, the network switched off, a shortcut set on a Googlebook
where Booklight has none yet, a screen reader's voice, the HP Googlebook, or a person's judgement.

**Before the first check**

- Choose the device by its model (`adb devices -l`), never by its place in the list; set `ANDROID_SERIAL`.
- Close the Booklight window, then `./bl install`. `./bl idle` before every `./bl open`: the panel takes the keyboard.
- Never `uiautomator dump`. No sign-in is made for a check, and no other app is opened by one. A capture or a film
  of the screen shows real windows: none is kept or committed. `./bl shot NAME` is the panel's own window, and
  `./bl backdrop` puts a window of test content (a white page, a dark terminal) behind the panel.
- This file is public: no serial, no build number, nothing about what is installed. The lessons' example and the
  show's first rows are apps of the device, and `./bl debug dump`, `./bl debug first` and `./bl debug first says`
  name them: an answer says "the device's example" or "the device's apps", never a name or its letters.
- Write down what `./bl debug pref` says before the first check (`opening=`, `dim=`, `theme=`, `suggestions=`,
  `zero=`, `sums=`) and put them back after the last one. The shortcut the test device already has for Booklight
  stays as it is: its row in the system's dialog is never removed.
- Check 1 comes first: the welcome's drawing changed, and the stage is drawn in a new way.
- **The stage's clock.** A screen of first run is not there in one frame: its parts come by marks, in ms after the
  screen came (`docs/design/first-run/motion.md` §3). `./bl debug first stage at MS` stands that clock still at MS;
  `./bl debug first stage` lets it run again; `./bl debug first stage as set_down|back|turn|after_list|lands|whole
  [at MS]` has the screen that stands come again in that way, in this moment. `./bl debug dump` says for the stage
  that stands `comes=` (how it came), `lead=` (when the answer Enter would run begins to show), `end=` (when it is at
  rest) and `seen` (it takes a key now: 350 ms after `lead`). A still of a **turn** (one screen in another's place):
  stand the clock first (`first stage at MS`), then make the turn (`first at SCREEN`, or `first answer NAME`); what
  gives way fades in real time and is not in such a still.
- **The marks, for a screen set down as a panel opens**, depend on the speed of the opening, because the band waits
  for the glass to be at rest: it begins 120 ms after the gate at Fast, 162 at Medium, 324 at Slow. The numbers below
  are for Medium; at another speed add the difference to everything from the band on.
- **A key hook after a screen came** is taken only once the dump says `seen` for that screen (350 ms after its
  answers began to show): wait for it, or the hook presses a key that does not count, as a hand's would not.
- **Times.** `./bl debug first trace N` logs the next N frames (`./bl logs`): `t=` ms since the first of them, `h=`
  the window's height in px, `playing=`, `stage=`, `comes=`, `clock=` the stage's clock in ms (divide by the slow
  motion's factor), `lead=`, `seen=`, `keydown=`, `checked=`, `laps=`, `held=` a height that is being kept, `rows=`.
  Start it, then give the key in the same breath; read the times from the log, not from a film.
- `./bl open stay slow=4` stretches every spring, every tween and the stage's clock four times; `DARK=true ./bl open
  stay` is the dark theme. A film of the screen is cut to the panel's rectangle, looked at frame by frame, and deleted.
- `./bl debug first says` prints what first run told a screen reader in the open panel, and what every node of the
  panel says to one (its role, its words, its state). It needs no screen reader to be on.
- Lengths below are dp from the panel's left and top edges; the window's height in the dump is px (divide by the
  screen's density).

# This part's checks

## 1. The panel still opens, and nobody else sees a change [C]

`./bl debug first off`, `./bl debug pref key seen`, `./bl open stay`, `./bl debug dump`, `./bl shot plain`; again with
`DARK=true`. `./bl logs`: no crash. Then ten openings by the keys, typing nothing, and `./bl debug pref` before and
after.

The glass, the placeholder, the mark, the `esc` cap, the tips, the copy's line and "your usual" as before; the lap
of light a while after the opening, as before. The dump: `first=none`, `playing=none`, no `comes=`, no `later`, no
`laps=`. `./bl debug pref` says the same before and after: nothing was written. `./bl debug keys chrome` (or any
letters that find apps): the list is as it was, and no row called "First steps" is in it.

Answer: not run

## 2. "First steps", typed [C]

`./bl debug first off`, `./bl debug pref key seen`, `./bl open stay`, `./bl debug keys "first steps"`, `./bl debug
dump`, `./bl shot steps-row`; then `./bl debug key enter`, a moment later `./bl debug dump`, `./bl shot steps-k4`,
`./bl debug first`. In German: the same with `erste schritte`. Also `./bl debug keys tour`, `welcome`, `intro`.
Then `./bl debug guide`, which tries every example of the list of everything.

The row "First steps" is row one, with the `again` mark and "Booklight" where a row's kind stands. Enter: the panel
stays, the field is emptied, the list fades and "Your key works" stands under it with "Go on" lit and "Change the
key" under it; 252 dp; no welcome and no show. `./bl debug first`: `run=REPLAY`, `done=[show]`, `opens=1`,
`screen=K4`, and `suggestions=`, `sums=`, `key=` as they were. The dump: `first=K4 armed=0`, `comes=after_list`.
With `./bl debug pref key no` before it: the key's first screen instead, "1 of 5" (or "of 4" with suggestions on).
`./bl debug guide`: every entry of its answer begins `ok`, none `BAD`, and `ok first 'first steps'` is among them
(in German `'erste schritte'`).

Answer: not run

## 3. The Enter that asked for it answers nothing [C]

As check 2, with `./bl debug key enter2` in place of `key enter` on the row; then `./bl debug first`. And with real
keys: the words typed, Enter pressed three times as fast as a hand can.

`screen=K4` still: the second Enter was not "Go on". Half a second later Enter is "Go on".

Answer: not run

## 4. "First steps" where a run is unfinished [C]

`./bl debug first new`, `./bl debug first shown`, `./bl debug pref key seen`, `./bl debug first at l3`, `./bl open
stay`; `./bl debug keys "first steps"`, `./bl debug key enter`, `./bl debug first`, `./bl debug dump`. Then with the
usual rows standing (`./bl debug pref zero on`, an installation with a history) and with a tip standing.

Lesson 3 stands again where it stopped, with its counter: `run=NEW`, `screen=L3`, `opens=1`; nothing that was done
is undone. Where the usual rows or a tip stood under the empty field, the stage has their place and they do not
come back in this opening.

Answer: not run

## 5. "First steps" from the window [C]

`./bl debug first off`, `./bl debug pref key seen`, `./bl window start`, `./bl wshot start`, `./bl debug window`.
With the keys in the window, go to the row "First steps" in the Tips group and press Enter (the keys are sent into
Booklight's own window only). Then `./bl debug first`, `./bl debug dump`. Close the panel with `./bl debug close`
three seconds in, `./bl wshot start-2`.

The row stands after "Show the tips again": the `again` mark at 16 dp, "First steps" and "The key, three things to
try, your choices. About a minute." from 52 dp, the "opens" mark ending 16 dp from the row's end. Enter: a panel
opens over Booklight's own window and plays the whole of it, the welcome first, at the speed that is set (not at
Slow): `run=REPLAY`, `overture=WELCOME` before it began, `playing=welcome`; the last start is no key (`key=false
icon=false`). It lands in "Your key works". Back in the window, with the run unfinished: the row is called "Go on
with the first steps" and "1 of 5" (or "of 4") stands before its mark; Enter on it then opens the panel on the
screen the run stopped at, without the welcome.

Answer: not run

## 6. The same row, as its hook writes it [C]

`./bl debug first off`, `./bl debug pref key seen`, `./bl debug first again`, `./bl debug first`, `./bl open stay`.

`run=REPLAY done=[] opens=0`, `overture=WELCOME slow=false`; the panel plays the piece. `./bl debug first again`
while a run is unfinished changes nothing but `opens=0`.

Answer: not run

## 7. Asked for again on an installation that has been lived in [C]

`./bl debug pref sums off`, `./bl debug pref suggestions on`, `./bl debug pref key seen`, `./bl debug first again`,
`./bl open stay`; let it play; `./bl debug dump` during the show; then Enter on "Go on", and Tab, Enter through the
lessons, with `./bl debug first` after each. Put `sums` and `suggestions` back.

The show goes from the apps straight to the example flight: no sum is typed and no answer row comes; the flight's
first letter follows the last Tab by about 220 ms. It lands in "Your key works", and the highlight travels into "Go
on". The counter says "1 of 3": the key, two lessons, no sum's lesson and no question; after lesson 3 the choices
come. `suggestions=true` and `sums=false` throughout: no switch was touched.

Answer: not run

## 8. The word "Not now" leaves behind [C]

`./bl debug first new`, `./bl debug first shown`, `./bl debug pref key no`, `./bl open stay`; once the dump says
`seen`, `./bl debug key tab`, `./bl debug key enter`; `./bl debug dump`, `./bl shot later`, `./bl debug first`. In German.
Then `./bl debug keys x`, `./bl debug type` with nothing after it, `./bl debug dump`. Close; open again.

The stage folds to the bare field, 68 dp, and its placeholder is "First steps wait in Booklight’s window" (German:
„Die ersten Schritte warten in Booklights Fenster“); the dump says `later`; `run=NONE mark=true`. Typed over and
emptied, the placeholder is the same again. At the next opening it is the ordinary one.

Answer: not run

## 9. A run that has used up its openings says the same, once [C]

`./bl debug first new`, `./bl debug first shown`, `./bl debug pref key seen`, `./bl debug first at l2`; then four
times: `./bl open stay`, `./bl debug dump`, `./bl debug close`. A fifth time.

Openings one to three show lesson 2. The fourth shows the bare field with "First steps wait in Booklight’s window"
and `later`; `./bl debug first`: `opens=4 parked=true`. The fifth: the ordinary placeholder, no `later`. In the
window the row says "Go on with the first steps" and "2 of 5".

Answer: not run

## 10. The Start page tells the way to a key as first run does [C]

`./bl debug pref key no`, `./bl window start`, `./bl wshot key`, `./bl debug window`; in German. Then, with the keys
in the window, Enter on "Open Keyboard shortcuts"; a capture cut to the dialog's own bounds; Esc.

Under "No key yet": two caps, `Action` and the Quick Insert key by the system's own name, on the text's edge; under
them "… is left of A · or any keys you like". No "Alt", no "Space", no "K", no "or". The row "Open Keyboard
shortcuts" says "Click Customize, type Booklight, click +, press your keys, click Set shortcut. Then press your keys
once more." with the dialog's own two words. The dialog opens on Booklight's page, which begins with "A shortcut for
this app, in five steps" and its five rows, each ending in the suggested keys, and the window's own keys after them.

Answer: not run

## 11. The welcome: no ring as the night falls and lifts [C]

A panel that began with the piece and was landed with Esc (a real first opening, then `./bl debug key esc`); in it
`./bl debug first welcome at MS` and `./bl shot ring-MS` for MS = 350, 450, 550, 700 (the night falling) and 3460,
3520, 3580, 3660, 3740 (lifting). Light and dark theme; over the backdrop's white page and over its dark one.

In no picture a ring, an arc or an edge round the middle of the glass. At 700 the glass is a little darker towards
its corners, with no line where that begins. From 3580 on, and before 500, there is no darker shade at the corners
at all: the glass is one even veil on its way between the night and the theme's own.

Answer: not run

## 12. The welcome: no words before the lamp's light reaches them [C]

`./bl debug first welcome at 850`, `./bl shot strike`; `at 900`, `at 1000`, `at 1090`.

At 850 nothing stands under the field: no title, no line. At 900 and 1000 they show more and more, dimly, as the
light floods the field from the caret; at 1090 they show dimly, whole, and the seam of light stands on the middle.

Answer: not run

## 13. The show's first rows come with their highlight [C]

A real first opening with `./bl debug first trace 400` started just before the welcome's end, and filmed with
`slow=4`; `./bl debug first show at apps`, `./bl shot apps`.

In the frames where the handle has reached row one's seat and the first letter lands: in every frame exactly one
highlight there, and from the first frame in which it is the list's own pill, row one's icon and name stand in it,
whole. No frame shows the pill empty, and none shows the seat without a highlight. Rows two to five rise after it,
22 ms apart. The trace: `playing=show` and `rows=5` (or fewer) begin in the same line.

Answer: not run

## 14. Booklight's mark comes up before the first letter [C]

`./bl debug first welcome at 3660`, `./bl shot mark-1`; `at 3740`, `./bl shot mark-2`; `at 3800`.

At 3660 the mark's seat (centred on x = 38, y = 34) is empty. At 3740 Booklight's mark stands in it or is coming
up; at 3800 it stands, and the head's light has closed to the caret. In a film: the mark is there before the first
letter, not with it.

Answer: not run

## 15. A cue with nothing to show [A]

On a device where no app's name begins with a letter (none is known): Enter on "Show off" sets the key's step down
at once; left alone, the welcome hands over to the key's step when it has stood, without a second's wait. By
reading: core `FirstRun.pressed` and `handOver`, with their tests.

Answer: not run

## 16. The key's step is set down part by part [C]

`./bl debug first new`, `./bl debug first shown`, `./bl debug pref key no`, `./bl open stay` (the piece counts as
shown: no welcome); `./bl debug dump` at once. Then `./bl debug first stage as set_down at MS`, `./bl shot t1-MS`
for MS = 0, 120, 250, 340, 420, 600. In motion: `./bl debug first stage as set_down`, filmed with `slow=4`. Once
with `./bl debug first update` in place of `new` and `shown` (the run of an installation that was there before).

The dump: `first=K1 armed=0 1/5 … comes=set_down lead=382 end=552` (Medium). At 0: the glass at 252 dp with nothing
under the field. At 120: the disc with the key's mark there, the title and its line nearly, the counter coming,
each rising into its place; nothing in the band. At 250: `Action` has all but come up, the "+" is coming, no second
cap. At 340: `Action` and the "+", the second cap coming up; no caption, no answers. At 420: both caps; the caption
is fading in, the two answers are rising with the highlight on the first. At 600: the screen exactly as
`first-run-key.md`'s check 1 has it. An update's run is the same without a counter. In no picture is anything cut by the
lower edge, and nothing ever stands lower or further right than it does at rest but the 12 dp a part rises from.
In motion: left to right and top to bottom, one voice at a time; a cap comes up as a key that is let go.

Answer: not run

## 17. It takes a key only once its answers are in view [C]

In the panel of check 16: `./bl debug first trace 200`, `./bl debug first stage as set_down`, at once `./bl debug
key enter2`; `./bl debug first`. Then real keys: Enter pressed again and again from the key that opens the panel
until the screen stands.

No dialog comes: `hold=NONE helper=0`. In the trace `seen=false` until `clock` has passed `lead` + 350 (732 at
Medium), and `seen=true` after. With real keys: the first Enter that does anything is one pressed after the answers
have stood for a third of a second; none is kept.

Answer: not run

## 18. A screen come back to arrives as rows do [C]

`./bl debug first new`, `./bl debug first shown`, `./bl debug pref key no`, `./bl open stay`, `./bl debug close`,
`./bl open stay`, `./bl debug dump` at once; `./bl debug first stage as back at MS`, `./bl shot back-MS` for MS =
40, 90, 150, 300. The same on lesson 3 (`./bl debug first at l3` before the two openings).

`comes=back lead=110`. At 40: the disc coming, hardly anything else. At 90: the seat nearly whole; the caps (or the
whole recipe) beginning to come, all together: nothing is written a letter at a time. At 150: the band there, the
caption and the answers coming. At 300: at rest.

Answer: not run

## 19. A lesson is set down, and its recipe is written [C]

On "Your key works" (`./bl debug pref key seen`, `./bl debug first at k4`, `./bl open stay`), half a second in:
`./bl debug first stage at MS`, then `./bl debug key enter` ("Go on"), `./bl shot go-MS`, for MS = 100, 200, 320,
420, 600; between two of them `./bl debug first at k4` and `./bl debug first stage`. In motion with `slow=4`.

`first=L2 comes=turn lead=394`. In every picture the seat's disc stands where it stood and the title, the line and
the counter are lesson 2's. At 100: nothing in the band yet ("your key" and its check have gone). At 200: the
recipe's first three letters (one every 40 ms from 120). At 320: its letters whole, the Enter cap beginning to come
up. At 420: the cap up, the caption fading in, Skip and its `tab` cap beginning to rise. At 600: lesson 2 as
`first-run-lessons.md`'s check 1 has it. In motion: the words roll one after the other (title, line, counter), the mark's symbol gives way
to the new one, "Go on" fades where it stood with a darker slot for a moment, and the recipe is typed out.

Answer: not run

## 20. Skip: what stays does not move [C]

On lesson 2: `./bl debug key tab`, half a second later `./bl debug key enter`; filmed with `slow=4`; `./bl debug
dump`.

`first=L3 comes=turn lead=0`. "Skip" and the caption stay exactly where they stand and do not fade; Skip's
highlight fades and the `tab` cap comes back; the old recipe fades and lesson 3's is written from the left.

Answer: not run

## 21. After a lesson's Enter the next lesson is set down [C]

On lesson 2, type the device's example (`./bl debug first` says it; never write it down), `./bl debug first trace
200`, `./bl debug key enter`; `./bl debug dump` a second later. For stills: `./bl debug first stage at MS` before
the Enter, MS = 60, 130, 300.

The slot shows as pressed and the footer says "That opens …"; 520 ms later the list gives way and lesson 3 is set
down: `comes=after_list`. In the trace `h=` goes from the list's height to 252 dp without a line at 68 dp's height.
At 60: nothing of the stage yet (the rows are fading). At 130: the disc and the title coming. At 300: the seat
whole and the recipe's letters; its caps not yet.

Answer: not run

## 22. Typing on a stage: the lower edge never visits the bare field [C]

On lesson 2: `./bl debug first trace 120`, then `./bl debug keys x`; and on the question (`./bl debug first at q`):
the same. Filmed with `slow=4`.

In the trace `h=` goes from the stage's height (252 dp, 324 for the question) to the list's in one move; no line
has the bare field's 68 dp, and `held=` shows the kept height for the frame or two before `rows=` is more than 0.
In the film the stage fades where it stands and the rows rise over it; the letter is in the field in the frame of
the key. `./bl debug type` with nothing after it: the stage is back whole, in one fade, and nothing is written
again.

Answer: not run

## 23. A letter typed while a screen is being set down [C]

`./bl debug first stage as set_down`, 150 ms later `./bl debug keys x`, then `./bl debug type` with nothing after
it; `./bl debug dump`; at once `./bl debug key enter`. Filmed with `slow=4`.

The caps that had not come when the letter was typed never come while the stage fades. Emptied, the screen stands
whole: `comes=whole`. The Enter pressed at once answers nothing (no dialog); a third of a second later it does.

Answer: not run

## 24. The sum is copied, and the question rises [C]

On lesson 4 (`./bl debug first at l4`): `./bl debug keys "150 + 20%"`, `./bl debug first trace 240`, `./bl debug
key enter`. For stills: `./bl debug first stage at MS` before the Enter, MS = 60, 110, 150, 210, 400. `slow=4`.

"Copied" with its check takes the coach line's seat, and the two are never drawn over each other. 520 ms later the
field is empty, the rows fade, and `h=` goes from the list's height to the question's 324 dp without 68. The
question's parts rise in reading order: at 60 nothing yet, at 110 the disc and the title coming, at 150 the counter
and the first of the text too, at 210 the text there and the note and the answers beginning with their `tab` cap,
at 400 the whole question, nothing lit. `comes=after_list lead=202`.

Answer: not run

## 25. The question's keys count from its answers [C]

On lesson 4's stage: `./bl debug key tab`, half a second later `./bl debug key enter` (Skip), and at once `./bl
debug key tab`, `./bl debug key enter`; `./bl debug first`. Then with real keys: Tab, Enter pressed four times over
from lesson 2, as fast as a hand can.

The question stands (`comes=turn lead=196`) and has no answer: `screen=Q`, `suggestions=false`. With real keys the
question is reached and stands, or "Not now" is given; suggestions are never on.

Answer: not run

## 26. The question gives way to the choices [C]

On the question: `./bl debug key tab`, half a second later `./bl debug first trace 120`, `./bl debug key enter`
("Not now"). Filmed with `slow=4`.

The slot shows as pressed; the question's words fade where they stand; `held=` keeps 324 dp for 60 ms, and only
then `h=` goes to 268 dp: no word is cut by the lower edge. The two rows rise 60 and 82 ms in, and the footer fades
in on the lower edge. Then `./bl debug key down`, `./bl debug key enter`: the switch's thumb goes across with a
small overshoot inside its track, the track fills, "Off" rolls to "On", and the panel stays.

Answer: not run

## 27. The fold, and its lap of light [C]

On the choices, nothing selected: `./bl debug first trace 300`, `./bl debug key enter`; `./bl debug dump` two
seconds later. Filmed. Then again, with `./bl debug keys x` a quarter of a second after the Enter.

The rows and the footer fade; `h=` goes from 268 dp to 68 without stopping; the placeholder is "Type ? for
everything Booklight does"; the `esc` cap fades back a moment after. About 360 ms after the Enter `laps=` goes up by
one, and one white light runs once round the bare field and is gone before it stops. With the letter typed: no lap
(`laps=` does not change), and the list for the letter stands.

Answer: not run

## 28. An answer shows as pressed [C]

`./bl open stay slow=4`; on the key's first screen `./bl debug key enter` (the system's dialog comes: Esc it away);
on "Your key works" Enter; on a lesson Tab, Enter; on the question Tab, Enter. Filmed.

For about a third of a second at this speed (80 ms at the speed of every day) the slot that was run is a little
darker, with no colour of its own; on answers that give way it is darker while they fade.

Answer: not run

## 29. The key lands in view [C]

`./bl debug pref key no`, `./bl debug first new`, `./bl debug first shown`, `./bl open stay`, `./bl debug first at
k2`; half a second later `./bl debug first trace 200`, `./bl debug first press`; `./bl debug dump` at once and
again a second later. Filmed with `slow=4`. Then `./bl debug first at k3` and the same.

At once: `first=K4 … keydown landing`. In the trace: `keydown=true` for 160 ms, then `checked=true` 60 ms after
it went false, `laps=` up by one 240 ms after it went false. In the film: "your key" goes down (a little smaller,
a little darker), the title rolls to "Your key works" and its line after it, the caption goes, the two answers fade
and "Go on" comes lit; the key comes up with a small spring, the check draws in it, and one lap of light runs round
the 252 dp outline. The key never grows past its size by more than a hair.

Answer: not run

## 30. The key lands under the dialog [C]

As check 29 with `./bl debug first press under`. Then for real: on the key's first screen Enter (the dialog comes),
`./bl debug first` half a second later, and the device's own shortcut for Booklight pressed with the dialog up;
filmed, and with `./bl debug first trace 300` started before the keys.

By the hook: `keydown=true` from the first line, for 400 ms; no way down is seen. For real: half a second after the
dialog came `helper=1` and the stage under it says "Now press your keys" (`hold=UNDER`); the keys: the dialog falls
away, and the first frame that shows the panel shows "Your key works" with the key held down; it comes up, the
check draws, the light runs. `key=true`, `screen=K4`, `hold=NONE`. The keys held down for a second: the panel stays
(until the check is whole no repeat puts it away).

Answer: not run

## 31. The keys open a new panel on "Your key works" [C]

`./bl debug pref key no`, `./bl debug first new`, `./bl debug first shown`, `./bl debug close`; the device's own
shortcut for Booklight, with `./bl debug first trace 300` not possible before it: so `./bl debug dump` at once and
two seconds later, and a film.

The panel opens as every day; "Your key works" is set down at the gate, "your key" comes up, its check draws a
moment later, "Go on" rises lit, and one lap of light runs. `laps=1`.

Answer: not run

## 32. The opening piece lands part by part [C]

A real first opening; `./bl debug first show at cell`, `./bl debug first stage at MS`, `./bl debug first land`,
`./bl shot land-MS` for MS = 100, 200, 300, 420, 520, 700 (a fresh `show at cell` before each). A real first
opening filmed with `slow=4` and with `./bl debug first trace 600`.

The hook's answer: `playing=landing first=K1 gliding`. At 100: the disc beginning; no words yet. At 200: the disc,
the title and its line coming, and "Open Keyboard shortcuts" beginning to fade in where the highlight is arriving.
At 300: the seat whole, the armed answer's words there, `Action` coming up. At 420: `Action` and the "+", the
second cap beginning. At 520: both caps, the caption and "Not now" coming. At 700: the key's first screen at rest.
The highlight ends exactly on the armed answer: its place is the same in every picture. In the trace: `seen=false`
until `clock` has passed 530, `playing=landing` until then.

Answer: not run

## 33. With the system's animations off, everything stands [A]

With the system's animations off (a setting of the device: Alex's, or set for this check and put back, as in part
4): every check above that has a still, once each.

Every screen stands whole in its first frame: the caps, the recipe, the caption, the answers; "Your key works" has
its check and "Go on" lit at once, the key is never drawn down; the choices' switch and its word change in one
frame; no lap runs. The waits that are there to be read stay: 520 ms after a lesson's Enter, 300 ms before "Now
press your keys" under the dialog. A screen still takes a key only 350 ms after it came.

Answer: not run

## 34. What a screen reader is told, by the hook [C]

On each screen in turn (`./bl debug first at k1 … q`, the choices, the ending, the word left behind): `./bl debug
first says`. Also: on lesson 2 right after the panel is made; after `./bl debug keys x` and `./bl debug type` with
nothing after it; after flipping the switch on the choices.

`told=` holds each screen's sentence once: the counter, the title, the line, the keys or the recipe ("For example:
…", the app's letters spelled), the caption, and how to act. For "Your key works" in a run asked for again: "Enter
goes on. Tab for Change the key." Typed over and emptied, a screen is not said a second time, and "Welcome to
Booklight" is said once in a panel at the most. Where the recipe came a moment after the lesson was said, it is
said by itself. After the flip: "Show your usual, On". The word left behind: "The first steps wait in Booklight’s
window, on its Start page." Among `nodes=`: the switch's row as `switch 'Show your usual' off` (or `on`), the coach
line as `live`, no node for a large cap or a recipe's letters (they are a picture), and the field.

Answer: not run

## 35. A screen reader's own voice [A]

With TalkBack on: a new installation's first opening (no welcome: the key's step at once, the title said before
it); each screen in turn; the choices; the ending. And TalkBack switched on in the middle of the welcome, and in
the middle of the show.

Each screen is said once, in the order of the hook's sentence; the switch is said as a switch with its state, and
its new state after a flip. Switched on in the piece: the piece ends at once and the key's step stands and is said.

Answer: not run

## 36. A recipe has its room [A]

With the system's font scale above the usual, in German, on lesson 3 (the widest recipe): is the recipe one line,
whole, a little smaller, ending before the `tab` cap, on the caps' own centre line? And on a device whose example's
name begins with an emoji (none is known): no half character in the recipe. By reading: core `FirstRun.head`, with
its test.

Answer: not run

## 37. Heights, and nothing outside the glass [C]

From the traces and dumps above, in dp: the key's step and the lessons 252, the question 324 (or more for a longer
text), the choices 268, the bare field 68, the welcome 468. In every picture: is anything cut at the lower edge, or
is there glass with nothing on it under the last line? The window's width and its top edge never change in any
trace (`./bl debug dump`'s `window=` before and after).

Answer: not run

# What the three earlier lists left open

Each with the list and the check it comes from. Where this part built something that answers it, that is said.

## 38. "Set shortcut" and the first press of a new key, with the panel held [A]

*`first-run-key.md` 21, and the proof's check 6.* On a Googlebook where Booklight has no shortcut yet: from the
key's first screen Enter; in the dialog Customize, the app's name, +, the keys, Set shortcut, the keys again. Does
the dialog go by itself, and does the panel show "Your key works" with the key held down, then up, the check and
the lap (check 30)?

Answer: not run

## 39. A real click on the icon, and the other starts [A]

*`first-run-key.md` 10 and 11.* A new installation: the first click on the launcher's icon shows the panel with the
opening piece; every later one the Booklight window. App info's "Open", the widget and the tile: the panel, not
counted as the key.

Answer: not run

## 40. A keyboard without the Quick Insert key [A]

*`first-run-key.md` 12.* With an external keyboard attached and used: the caps are `Action` and `M`, the caption is
"or any keys you like", the dialog's rows carry Action + M; on the window's Start page the same two caps (check 10).

Answer: not run

## 41. Is Quick Insert the key left of A? [A]

*`first-run-key.md` 13; "Settled for the build", point 8.* The caption under the caps, and now the Start page, say so.

Answer: not run

## 42. The system in German [A]

*`first-run-key.md` 4 and 14.* With the system's language German: the dialog's own two words and the key's name as
the caps, the caption, the dialog's rows and the Start page quote them; the large cap's fallback where the name
has no room.

Answer: not run

## 43. A larger font scale [A]

*`first-run-key.md`, its last list.* Every screen of first run, the Start page's key rows and the row "First steps"
at the largest font scale: nothing cut, the recipe drawn smaller (check 36), the question taller.

Answer: not run

## 44. Other windows, shortcuts and the lock screen under the hold [A]

*`first-run-key.md` 9.* With the dialog over the held panel: a click on another window, another app's keyboard
shortcut, the lock screen. Is the panel gone afterwards, and never left on the desk without the keys?

Answer: not run

## 45. A click on the stage, and a click outside the glass [C] and [A]

*`first-run-key.md` 3; `first-run-lessons.md` 14; `first-run-welcome.md` 22.* [C], taps inside the panel's own
window: a click on an answer runs it (and shows it pressed); a click on the glass beside the answers does nothing;
a click on an answer in the third of a second after its screen came does nothing; in the welcome a click on the
handle begins the show and a click elsewhere on the glass sets the key's step down; in the show a click on a row
sets the key's step down and opens nothing. [A]: a click outside the glass closes the panel, as always.

Answer: not run

## 46. A pointer that rests where the answers come [A]

*`first-run-lessons.md` 28.* The pointer left where "Go on" or "Agree" will stand, the screen changed by the keys:
is an answer lit without the pointer having moved?

Answer: not run

## 47. Settings standing in for an app that can be searched [A]

*`first-run-lessons.md` 6 and 27.* On a device where no app can be searched (none is known): lesson 2 names the
Settings app, lesson 3 goes by the Settings keyword to a page, and Enter there is practice.

Answer: not run

## 48. A settings page, and everything else, in a lesson [A]

*`first-run-lessons.md` 11.* In a lesson, a row that is not the lesson's own (a settings page, the web's row,
another app's command) runs as every day and opens what it opens: is that what a new user expects under "Practice:
nothing opens"? (It opens another app: not run by the coordinator.)

Answer: not run

## 49. A question of more than four lines [C]

*`first-run-lessons.md` 15.* `./bl debug pref engine brave` (the longest name), in German, on the question: is the
stage taller by what the text needs, the text whole, the answers under it? Put the engine back.

Answer: not run

## 50. The dark theme on the lessons and the choices [C]

*`first-run-lessons.md` 1 and 16.* `DARK=true ./bl open stay` on lessons 2 to 4 and on the choices, over the
backdrop's white page and its dark one: the caps, the recipe, the switch and the footer's line readable.

Answer: not run

## 51. The whole run by hand, timed and watched [A]

*`first-run-lessons.md` 20; `pm.md` §4.* A new installation, real keys, no hooks: from the first opening to the
bare field. How long does it take (45 to 60 seconds were asked for)? Where does a hand stall?

Answer: not run

## 52. The night and the shaft over a white page [C]

*`first-run-welcome.md` 3, 8 and 29.* A real first opening over the backdrop's white page, in light theme: is the
night a night, the shaft seen as light, the footer's "Skip" readable from the show's first frame?

Answer: not run

## 53. The piece's times, measured [C]

*`first-run-welcome.md` 10, 16 and 18.* A real first opening with `./bl debug first trace 1500` started as it
opens, and again with `slow=4` filmed. From the trace: when `playing=show` begins (the hand-over's end), when
`rows=` changes for the sum, the flight and the grid, when `laps=` goes up, when `playing=landing` begins: against
the script's 1,960 · 3,352 · 4,840 · 5,080 · 5,856 ms from the first letter, within two frames each. In the film at
a quarter of the speed: the highlight's way from the grid's square into the armed answer, and from a row.

Answer: not run

## 54. The example flight in German, later, with a key, offline [C] and [A]

*`first-run-welcome.md` 14.* [C]: `./bl debug first show at flight` in German („Landung in 4 Std. 07 Min.“,
„Pünktlich“, „Beispiel“); again a minute later: the headline has not counted down. With a flight key in and
without, if one is at hand: the same row. [A]: with the network off, the same row.

Answer: not run

## 55. Esc held, and the second Esc [C]

*`first-run-welcome.md` 19 and 20.* With real keys: Esc held for two seconds in the show lands the key's step and
does not go on to close the panel; a second Esc, pressed once the key's step has stood for half a second, closes it.

Answer: not run

## 56. The lock keys, Quick Insert alone, and the system's own keys in the piece [A]

*`first-run-welcome.md`, "Still open".* During the welcome and the show: Caps Lock or the Quick Insert key alone
does nothing; the volume and brightness keys do what they do and the piece plays on.

Answer: not run

## 57. A space that comes by the input method [A]

*`first-run-welcome.md`, "Still open".* With an input method that hands a space over as text and not as a key:
during the welcome it is the cue, during the show it sets the key's step down, and the field stays empty.

Answer: not run

## 58. Enter in the moment a closing panel is turned round [C]

*`first-run-welcome.md`, "Looked at again": not driven by a key from outside.* On the key's first screen, and one
second into the welcome: `./bl debug turn 60 enter`.

The hook's answer: "turned; Enter in the same moment answered nothing". No dialog comes (`hold=NONE helper=0`), and
half a second later Enter is the screen's own.

Answer: not run

## 59. Not verified by reading or on a device [A]

*`docs/design/first-run/BUILD.md`, "Known, and left for a later part".* A panel whose first start carried a
referrer of its own and is later reached by the key; a process that dies under the dialog after the icon's first
click; whether a screen reader says "Now press your keys" while the dialog is over it.

Answer: not run

## 60. The HP Googlebook [A]

*All three lists.* Checks 1, 5, 10, 11, 13, 16, 19, 24, 27, 30 and 32 again there, and the frame times of a real
first opening (`first-run-welcome.md` 11).

Answer: not run

## 61. What only a person can say [A]

*`first-run-welcome.md`; `design.md` §15.* Does the lamp read as struck? Do the shaft into the caret and the handle
to row one read as one move up? Is the show one movement or five slides, and are eleven seconds a pleasure or a
wait? Is "your key", blank, read as a key waiting? Do the set-downs feel like a hand setting keys down, or like a
delay before Enter works? Is the key left of A?

Answer: not run
```

- [ ] **Step 6: The file of checks is whole and holds nothing of a device**

```bash
grep -c "^Answer: not run$" docs/research/first-run-last-pass.md
grep -c "^## [0-9]* *\. " docs/research/first-run-last-pass.md
grep -c "debug guide" docs/research/first-run-last-pass.md
grep -nE "com\.[a-z]+\.|[0-9A-Z]{8,}|build [0-9]|emulator-" docs/research/first-run-last-pass.md | grep -v "io.github.kuscher.booklight" || echo CLEAN
```

Expected: `61`, `61`, `2`, then `CLEAN` (no package name but Booklight's own, no serial, no build number).

- [ ] **Step 7: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 8: Commit**

```bash
git add app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt bl CLAUDE.md docs/research/first-run-last-pass.md
git commit -m "First run: the hooks that stand the stage's clock still, land the key, print what a reader is told and trace the frames; and the last pass's checks for a device"
```

- [ ] **Step 9: Hand over.** Report to the coordinator: the commit, that the debug APK is at `app/build/outputs/apk/debug/app-debug.apk`, and that the sixty-one checks are in `docs/research/first-run-last-pass.md`, check 1 first (the welcome's drawing changed, and the stage is drawn in a new way). Do not install it and do not run a check.

### Task 16: The documents say what first run is now

**Files:**
- Modify: `README.md`
- Modify: `PRIVACY.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/PICKING-UP.md`
- Modify: `docs/design/ux-model.md`
- Modify: `docs/design/design-system.md`
- Modify: `store-submission/forms/data-safety.md`
- Modify: `store-submission/README.md`

**Interfaces:**
- Consumes: what Tasks 1 to 15 built, and `docs/research/first-run-last-pass.md` (Task 15), which the status names.
- Produces: the README tells the way to a key as first run does and has a section "First steps"; the privacy text and the store's data-safety form say what first run keeps, sends and asks (nothing new), in the question's own words; the changelog has an entry headed "Unreleased"; `docs/PICKING-UP.md` has a newest entry that says where first run stands; `docs/design/ux-model.md` has §19 "First run" and `docs/design/design-system.md` has §17 "First run", and neither speaks of a first-run card any more.

No code changes. Every edit is a "Find" that stands in its file exactly once, as in the tasks before.

**Nothing is released.** The changelog's entry is headed "Unreleased" and opens with "On the branch `first-run`: built, and in no release." The status says the same. No version name and no version code is written anywhere new, and none is changed: it stays 3.0, code 8.

**`docs/design/first-run/BUILD.md` is not touched**: its part 5 is the coordinator's to write, from what the device says.

**What the documents must get right**, because a store reviewer and a user read them:
- The question is quoted as the app asks it ("Suggest searches as you type?", "Not now" / "Agree"), and it is said that nothing is chosen for the user and that Enter alone, Esc and waiting agree to nothing.
- The welcome's show sends nothing, opens nothing and keeps nothing; its flight is an example the app carries.
- The system's own words for its dialog and the question which keyboards are attached need no permission, and nothing of either is kept or sent.
- What is kept of first run: which steps are done, and in how many openings they stood.
- The design system says that the welcome is the one place where the daily panel's rules are lifted, and where its times live (`Lights`); the stage's motion is a table by the ways a screen comes, with the mark from which Enter counts.

The store's listing texts (`store-submission/listing/`) still tell the old way to a key. They are not this part's: the status names them under "Before a release".

- [ ] **Step 1: The way to a key, and "First steps", in `README.md`.** Two changes.

Find:

```markdown
Booklight opens whenever you start it, so a keyboard shortcut for the app is all it needs:

1. Open **Keyboard shortcuts** (Action + /).
2. Choose **App shortcuts**, then **Add shortcut**.
3. Find **Booklight**, choose **+**, press your keys, then **Set shortcut**.

The system wants the Action key in every shortcut. **Action + Alt + Space** and **Action + K** are both
free. The same keys close the panel again. Booklight shows these steps the first time you open it.
```

Make it:

```markdown
Booklight opens whenever you start it, so a keyboard shortcut for the app is all it needs. The first time you
open it, Booklight takes you there: it suggests **Action + Quick Insert** (Quick Insert is the key left of A),
opens the system's Keyboard shortcuts with the steps on its page, and says so when your key works. By hand:

1. Open **Keyboard shortcuts** (Action + /) and click **Customize**.
2. Type **Booklight** and click the **+** beside it.
3. Hold Action and press your second key, then click **Set shortcut**.
4. Press your keys once more: Booklight opens.

The system wants the Action key in every shortcut. On a keyboard without the Quick Insert key, **Action + M** is
a good one. The same keys close the panel again.

## First steps

After the key come three things to try once each (open an app, search inside one, a sum), one question (may
searches be suggested as you type: nothing is sent until you agree) and two choices. About a minute. Nothing
opens while you practise, and Esc leaves at any point. To see them again, type `first steps` in the panel, or
choose First steps on the Booklight window's Start page, which plays the welcome too.
```

Find:

```markdown
six sections: Start (what Booklight is, your key, tips, your usual, what you copied), Commands (everything it
```

Make it:

```markdown
six sections: Start (what Booklight is, your key, tips and the first steps again, your usual, what you copied), Commands (everything it
```

- [ ] **Step 2: What is kept, asked and sent, in `PRIVACY.md`.** Two changes.

Find:

```markdown
switch, which two things had the first two rows last time, and the list of things you asked not to be suggested.
```

Make it:

```markdown
switch, which two things had the first two rows last time, and the list of things you asked not to be suggested.
Booklight also keeps where its first steps stand: which of them are done, in how many openings they have stood,
how often the system's Keyboard shortcuts dialog was opened from them, and whether the app's icon has shown the
panel once.
```

Find:

```markdown
internet request it shows the search engine your IP address. Turn suggestions off again in the
Booklight window › Web search.
```

Make it:

```markdown
internet request it shows the search engine your IP address. Booklight asks once, in its first steps ("Suggest
searches as you type?"), and only "Agree" there, or the switch in the Booklight window, turns them on: Enter
alone, Esc and waiting agree to nothing. Turn suggestions off again in the
Booklight window › Web search.

**The first steps** send nothing and open nothing. In the welcome Booklight types into its own field and shows
your own apps for one letter, a sum, a flight and the emoji grid: nothing of that is opened, looked up, sent or
kept, and the flight is an example that Booklight carries and that says so. In the three things to try, an app's
row and a search inside an app are practice and start nothing; the sum's answer is copied, as on any day. To quote
the two buttons of the system's Keyboard shortcuts dialog and the name of the key it suggests, Booklight reads
those words from the system's own resources, as any app reads a label, and it asks which keyboards are attached
and whether they have the Quick Insert key. Neither needs a permission, and nothing of either is kept or sent.
```

- [ ] **Step 3: An entry headed "Unreleased", in `CHANGELOG.md`.** One change, at the head of the file.

Find:

```markdown
# Changelog

## 3.0 (2 October 2026)
```

Make it:

```markdown
# Changelog

## Unreleased

On the branch `first-run`: built, and in no release.

**First steps** (`docs/design/first-run/`; `BUILD.md` there says what was built and what was decided on the way).

- **A welcome, once.** A new installation's very first opening is a slow one: the glass grows tall and goes to
  night, a lamp strikes at the caret, and "Welcome to Booklight" stands in its light with a small Swiss Army
  knife. Then Booklight shows off with its own panel: your apps, a sum, an example flight, the emoji grid. It
  runs nothing and sends nothing, any key ends it, and a typed letter is simply typed. Not played with the
  system's animations off or a screen reader on.
- **Give Booklight a key.** Under the empty field, where a card stood: Action + Quick Insert is suggested
  (Action + M on a keyboard without that key), "Open Keyboard shortcuts" opens the system's dialog on a page of
  Booklight's own with the five steps, and the panel waits behind it. Press your new keys: the dialog goes, and
  "Your key works".
- **Three things to try**, each once and for practice: open an app, search inside one, a sum. Nothing opens; the
  footer says what Enter would have done, and the sum is copied.
- **One question**: may searches be suggested as you type? Nothing is armed, "Not now" comes first, and only
  "Agree" switches suggestions on. Then two choices: "Show your usual", and the list of everything.
- **It never nags.** Esc leaves at any point; an unfinished run stands in three openings and then waits in the
  Booklight window, and the bare field says so once. "Not now" on the key ends it.
- **First steps, again**: type `first steps` in the panel, or choose First steps on the window's Start page,
  which plays the welcome too. It changes no switch.
- **The screens are set down, not shown**: caps come up a beat apart, a recipe is written a letter at a time,
  an answer shows as pressed, the key that lands is held down on the glass and comes up with its check, and the
  run ends in one lap of light round the bare field. A screen takes a key only once its answers have been in
  view for a moment. With the system's animations off everything stands at once.
- The window's Start page and the README tell the same way to a key. The two cards under the empty field are gone.
- Nothing new is asked of the system: no new permission, nothing in the background, no new connection.

## 3.0 (2 October 2026)
```

- [ ] **Step 4: Where first run stands, in `docs/PICKING-UP.md`.** One change: a newest entry.

Find:

```markdown
*Living status. Newest first.*

## 2026-10-03: after 3.0, where the next session starts
```

Make it:

```markdown
*Living status. Newest first.*

## 2026-10-05: first run is built, on branch `first-run` (local, nothing pushed, nothing released)

Alex, on the plan and its designs: "Go ahead and build it". The papers are in `docs/design/first-run/`; its
`00-brief.md`, "Settled for the build", wins over the others.

- **What it is:** a new installation's first opening plays a welcome and a show, then the key's step (Action +
  Quick Insert, the system's dialog guided from the panel held behind it), three lessons that are practice, the
  question about suggestions, two choices, and the fold to the bare field. "First steps" plays it again: a
  command in the panel, a row on the window's Start page. `CHANGELOG.md`, "Unreleased", says it for a user.
- **Where it is:** branch `first-run`, on top of main (3.0). Not merged, not tagged, not pushed; the version is
  still 3.0 / code 8. It was built in five parts, each from a plan in `docs/superpowers/plans/`
  (`2026-10-04-first-run-…` and `2026-10-05-first-run-…`); `docs/design/first-run/BUILD.md` is the record of
  what each part built and of everything that was decided on the way, for Alex to overturn.
- **Where the rules are:** core `FirstRun.kt` (who gets a run, which screen stands, what an answer changes, what
  Enter does in a lesson, what a key does while the piece plays), `Show.kt` (the show as data) and `SetDown.kt`
  (when each part of a stage comes), each with its tests. `CLAUDE.md` has the hooks (`./bl debug first …`) and
  the gotchas.
- **Checked on the Lenovo Googlebook:** parts 2 to 4, in `docs/research/first-run-key.md`,
  `first-run-lessons.md` and `first-run-welcome.md`. **The last pass** is `docs/research/first-run-last-pass.md`:
  part 5's own checks, and every check the three earlier lists left open, each marked as one for adb or one for
  Alex. An answer there says whether it was run.
- **Not seen on a device until that pass is run:** the stage's motion between its screens, the key held down on
  the glass, "First steps" from the window, the three mends to the welcome.
- **Before a release:** the last pass, the HP Googlebook with Alex, his answers to `BUILD.md`'s table, the store
  listing's text about the key (it still tells the old way), and his word.

## 2026-10-03: after 3.0, where the next session starts
```

- [ ] **Step 5: How first run behaves, in `docs/design/ux-model.md`.** Three changes: two lines that spoke of a first-run card; a new §19 at the end.

Find:

```markdown
  off. A typed letter takes the line away, and it stays away for that opening. A first-run
  card has the place before it; it has the place before a tip. No line for a copy marked private, for a copy
```

Make it:

```markdown
  off. A typed letter takes the line away, and it stays away for that opening. First run's
  stage has the place before it; it has the place before a tip. No line for a copy marked private, for a copy
```

Find:

```markdown
- **What stands under the empty field** is one thing at most, in this order: a first-run card, the line for a
```

Make it:

```markdown
- **What stands under the empty field** is one thing at most, in this order: first run's stage (§19), the line for a
```

Find:

```markdown
| Backspace on empty | – | back to where Enter was pressed | the text's |
```

Make it:

```markdown
| Backspace on empty | – | back to where Enter was pressed | the text's |

## 19. First run (5 October 2026)

The papers: `first-run/` (`00-brief.md`, "Settled for the build", wins over the others; `BUILD.md` says what was
built and what was decided on the way). As built:

- **A stage under the empty field**, where two cards stood: one screen at a time, in this order: the key (four
  screens), three lessons (open an app, search inside an app, a sum), one question (search suggestions), two
  choices. It has the place under the empty field before everything else (core `Under`). It is not a row: nothing
  of it is selected, the field above it is live, and a typed letter puts it away in that frame; it is back, whole,
  when the field is empty again.
- **The stage's keys.** Tab and Shift + Tab arm its answers, round and round; Enter runs the armed one, and with
  none armed does nothing; Esc closes, and the same screen stands at the next opening. A screen takes a key or a
  click only once the answer Enter would run has been in view for 350 ms: nothing is answered blind, and nothing
  by the Enter that answered the screen before.
- **The key.** Action + Quick Insert is suggested, Action + M on a keyboard without that key. "Open Keyboard
  shortcuts" asks the system for its dialog; the panel holds on behind it, and the dialog opens on Booklight's own
  page: five numbered steps in the system's own words. The key that lands is told to the panel: "Your key works".
  "Not now" ends first run.
- **The lessons are practice.** Nothing opens: Enter on an app's row, and on a search inside an app, shows its
  slot pressed and the footer says "That opens …"; half a second later the next lesson stands. A sum is copied for
  real, and the panel stays. While a lesson's list is typed, the field's placeholder says what to type and the
  footer's left end says the next key and what it does today.
- **The question is consent**: alone on its screen, nothing armed, "Not now" first; only "Agree" switches
  suggestions on. It is not asked while they are on.
- **The choices are a list** of two rows with nothing selected: "Show your usual" with a switch, and the list of
  everything. Enter with nothing selected is "Done": first run is over and the glass folds to the bare field.
  Closing the panel on them ends first run too. A letter typed over them does not: they are back when the field
  is empty again.
- **An unfinished run** stands in three openings, then waits in the Booklight window. The opening in which it
  begins to wait, and the one in which "Not now" was said to the key, say so in the field's placeholder.
- **"First steps"** asks for it again. Typed in the panel (also found by tour, welcome, intro), the run stands in
  that panel, from "Your key works" where a key is known, without the opening piece. The row on the window's
  Start page opens a panel that plays the whole of it. It changes no switch: where suggestions are on the question
  is not asked, and where sums are off there is no sum's lesson and no sum in the show.
- **The opening piece**, once, at a new installation's very first opening, which runs at Slow: the welcome, then a
  show in which Booklight types into its own field (this device's apps, a sum, an example flight, the emoji grid)
  and runs nothing. Every key is the piece's first: Enter and Space are the cue in the welcome and set the key's
  step down in the show; Esc, Tab, an arrow and Backspace set it down; a typed character ends it and is typed. It
  is not played with the system's animations off, with a screen reader on, on a screen lower than 655 dp or in a
  right-to-left layout: the key's step stands at once. With animations off or a screen reader on, the welcome's
  title greets as the field's placeholder.
- **Booklight never opens itself over another window**, and nothing in first run opens another app.
- **Screen reader**: each screen is said once when it comes, with its keys or its recipe and how to act; so are
  the choices, the ending and the word that the steps wait in the window. The switch is a switch, and its new
  state is said.

| Key | On a stage | On the choices | While the opening piece plays |
| --- | --- | --- | --- |
| A letter | typed; the stage gives way to its list, and is back when the field is empty | typed; the choices are back when the field is empty | ends the piece, and is typed |
| Enter | the armed answer; none armed: nothing | nothing selected: Done, the fold; on a row: that row | the cue in the welcome; in the show the key's step lands |
| Tab · Shift + Tab | arms the next · the previous answer | as Down | the key's step lands |
| ↓ ↑ | – | the highlight comes to a row, and goes | the key's step lands |
| Esc | closes; the screen waits | closes; the choices are done | the key's step lands |
```

- [ ] **Step 6: How first run is drawn and moves, in `docs/design/design-system.md`.** Five changes: four lines that spoke of a first-run card (one is a piece of one long line); a new §17 at the end.

Find:

```markdown
- **First-run card.** The same strip, vertical: two 32 dp slots 4 dp apart, both as wide as the wider label plus the mark home, flush right, `word`. One highlight in the pill recipe.
```

Make it:

```markdown
- **First run's answers** (§17). The same strip, vertical: two 32 dp slots 4 dp apart, both as wide as the wider label plus the mark home, flush right, `word`. One highlight in the pill recipe.
```

Find, in one long line:

```markdown
18 | First-run card's options | One highlight:
```

Make it:

```markdown
18 | First run's answers (§17) | One highlight:
```

Find:

```markdown
- **The opening** (§4): always at the field's height. What is under the field (a card, a tip, rows for handed
```

Make it:

```markdown
- **The opening** (§4): always at the field's height. What is under the field (first run's stage, a tip, rows for handed
```

Find:

```markdown
- **The tip card**: the first-run card's measure. Nothing armed at rest; a `tab` cap beside the two answers.
```

Make it:

```markdown
- **The tip card**: 96 dp, two answers as on first run's stage (§17). Nothing armed at rest; a `tab` cap beside the two answers.
```

Find:

```markdown
  symbol for what it does); the play row has the app's icon as its mark and no icon inside its action.
```

Make it:

```markdown
  symbol for what it does); the play row has the app's icon as its mark and no icon inside its action.

## 17. First run (5 October 2026)

Drawn in `first-run/first-run.html`; the papers are `first-run/design.md` and `first-run/motion.md`, and
`first-run/00-brief.md`, "Settled for the build", wins over both. The interaction is `ux-model.md` §19. Every time
and curve below is in `overlay/Motion.kt`; when each part of a stage comes is core's `SetDown`, with tests.

**The stage** stands where a card stood: under the empty field, on the glass, with no frame of its own.

| Part | Measure |
| --- | --- |
| The stage | 184 dp: 8 of air, a seat (56), a band (112), 8 of air. With the field the panel is 252 dp |
| The seat | a row's own: a 36 dp disc with a 20 dp symbol at x = 20, a title and a line as a row's, a counter ("1 of 6") in the kind's place |
| The band | the keys or a recipe on its centre line, a caption (13 sp, weight 500) on the line a footer's words stand on |
| A large cap | 56 dp high and at least as wide, 16 dp corners, 20 dp inside each end, the field's type (24 sp, weight 500); ink at 10 % (14 % in dark) under a hairline of white |
| A recipe | large caps and typed words in the field's type, 12 dp between two caps, 16 dp otherwise; one too wide for its room is drawn smaller as a whole, from its left end, never cut |
| The answers | the strip, vertical, flush right, 20 dp from the edge: one or two 32 dp slots, 4 dp apart; a `tab` cap beside them while none is armed |
| The question's stage | 256 dp: the seat, the disclosure on lines of 20 in strong ink (four lines, and taller by as much as it needs more), a note, the two answers. With the field 324 dp |
| The choices | two rows of the list's own kind: a switch's row (92 dp) and a row |

**It is set down, not shown.** One clock runs from the moment a screen comes; every part has a mark on it, and
draws by where the clock stands, so a still at any moment is whole.

| A screen comes | Its parts, in order | Enter counts |
| --- | --- | --- |
| Set down, as a run's first opening opens on it | the disc, the words, the counter, 22 ms apart from the gate, each rising 12 dp as a row does; the band once the glass is at rest (120 ms after the gate at the least): its caps a beat (66 ms) apart, each fading in over 110 ms and growing from 0.96 on `pop`, a recipe's typed words a letter at a time (40 ms); the caption a beat after the last piece; the answers 22 ms after the caption | 350 ms after the answers began to show |
| Back, as a later opening opens on it | as rows come: the seat's three parts, the band whole, the caption and the answers, 22 ms apart. Nothing is written twice | the same |
| In another's place (an answer, Skip, a key that landed) | the seat stays and its title, line and counter roll 22 ms apart; the band's old pieces fade in 80 ms and the new ones are written from 120 ms on; what the two screens share does not move | the same |
| After a list that gave way (a lesson's Enter, the sum copied, "First steps" typed) | as set down, from 60 ms after the list gave way | the same |
| At the landing of the opening piece | the disc at 80 ms, the words at 150, the counter at 190; the armed answer's words at 180, inside the highlight that has travelled there; the band from 260. The answers do not rise: they fade in where they stand | 350 ms after the armed answer's words began to show |
| Whole (the field emptied again, a panel turned round, animations off) | everything in one frame | 350 ms after it came |

- **Pressed.** An answer that was given shows as pressed: 8 % ink over its slot, in over 80 ms and out over 120.
  What the answer does is not held up by it. A large cap that is held down goes to 0.96 of its size under the same
  ink in 80 ms, and comes back up on `pop`.
- **The key lands.** "Your key" on the glass is held down as the user's own key is: for 160 ms where the panel
  was in view, for 400 where it was under the system's dialog and is only now uncovered. It comes up, the check
  draws itself 60 ms later (one 140 ms stroke), and 240 ms after the key came up the edge light runs one lap.
  Under the dialog the stage turns to "Now press your keys" 300 ms after the panel lost the focus to it.
- **The question** rises: its text at 66 ms, the note at 120, the answers at 142. Nothing is armed and nothing
  is pressed for the user.
- **The fold.** After the choices the glass folds to the field on `place`, the field's `esc` cap fades back
  120 ms later, and one lap of light runs 360 ms after the fold, unless a letter was typed first.
- **Interrupted.** A typed letter puts the stage away in that frame and is never held up. A key during a set-down
  answers nothing. A stage that gives way fades as it stood: it is not finished first.
- **Animations off.** Every mark is at its end from the first frame: nothing moves, everything stands, and a key
  counts 350 ms after the screen came.

**The welcome and the show** are the one place the daily panel's rules are lifted: the glass grows to 468 dp for
something that is not a result, the veil goes to night, the desk behind is dimmed, the title is 40 dp type, and a
piece of light is drawn that is no highlight. It happens once, at a new installation's very first opening, which
runs at Slow. Its times are `Lights` (in `Motion.kt`), on one clock from the gate: night falls over 420 ms; the
lamp strikes at the caret (60 %, back to 35 %, on) and floods the field; a seam of light falls and opens to its
shape; the words stand in it once it has reached them, and not before; the handle rises with the cue on it, and
five tools flick out a beat apart. At the hand-over the handle is pressed, the tools fold, the shaft turns up to
the caret and the night lifts without an edge; the handle travels to row one's seat and becomes the list's
pill. The show then types on `Show`'s script (core) with the panel's own motion, and its first rows stand in the
frame the pill arrives. Nothing of the two is drawn outside the glass, and the window is never resized in width.
```

- [ ] **Step 7: The question in its own words, in `store-submission/forms/data-safety.md`.** Two changes.

Find:

```markdown
  show, which first-run cards were seen, and (1.1) what the user made: links, snippets, recipes, the emoji
```

Make it:

```markdown
  show, where the first steps stand (which are done, in how many openings they stood, how often the system's
  Keyboard shortcuts dialog was opened from them, whether the app's icon has shown the panel once), and (1.1)
  what the user made: links, snippets, recipes, the emoji
```

Find:

```markdown
- Turned on only by the user: the first-run card under the search field ("Search suggestions are off.
  Turn them on and what you type is sent to Google to suggest searches. Sums and web addresses are not
  sent." with **Turn on** / **Not now**), or Settings › Web search › Search suggestions. Turned off in
  the same setting at any time.
```

Make it:

```markdown
- Turned on only by the user: the question in the first steps, alone under the search field ("Suggest
  searches as you type? Booklight sends what you type to Google while you type, to suggest searches. Google
  also sees your IP address. Not sent: sums, web addresses, and anything after a keyword or with an app in
  the field." with **Not now** / **Agree**; nothing is chosen for the user, and Enter alone, Esc and waiting
  agree to nothing), or Settings › Web search › Search suggestions. Turned off in the same setting at any
  time.
```

- [ ] **Step 8: The note beside it, in `store-submission/README.md`.** One change.

Find:

```markdown
  privacy page (googlebook.studio/privacy/booklight) too: the app's first-run card, that file and the
  page must say the same thing.
```

Make it:

```markdown
  privacy page (googlebook.studio/privacy/booklight) too: the question in the app's first steps, that file
  and the page must say the same thing.
```

- [ ] **Step 9: Nothing claims a release, the old words are gone, and nothing private came in**

```bash
grep -n "versionCode\|versionName" app/build.gradle.kts
sed -n 3p CHANGELOG.md; grep -c "^## 3.0 (2 October 2026)$" CHANGELOG.md
grep -rniE "first-run card" README.md PRIVACY.md docs/design/design-system.md docs/design/ux-model.md store-submission/README.md store-submission/forms || echo GONE
grep -nE "Action \+ K\b|Alt \+ Space|App shortcuts" README.md || echo GONE
grep -c "Action + Quick Insert" README.md
grep -n "^## 19\. First run\|^## 17\. First run" docs/design/ux-model.md docs/design/design-system.md | wc -l
git diff --stat -- docs/design/first-run/BUILD.md store-submission/listing | grep . || echo UNTOUCHED
git diff | grep -nE "^\+.*(emulator-|ANDROID_SERIAL=[A-Z0-9]|[0-9A-Z]{10,})" || echo CLEAN
git diff --check || true
```

Expected: `versionCode = 8` and `versionName = "3.0"`; `## Unreleased`, then `1`; `GONE`; `GONE`; `1`; `2`; `UNTOUCHED`; `CLEAN`; and no line from the last command.

- [ ] **Step 10: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`.

- [ ] **Step 11: Commit**

```bash
git add README.md PRIVACY.md CHANGELOG.md docs/PICKING-UP.md docs/design/ux-model.md docs/design/design-system.md store-submission/forms/data-safety.md store-submission/README.md
git commit -m "First run: the documents say what first run is: the README and the privacy text, the changelog and the status, the two design documents and the store's form"
```

### Task 17: Part 5, checked as a whole

**Files:** none are changed. If a step shows a change that is not committed, it belongs to the task that made it: say which, and stop.

**Interfaces:**
- Consumes: everything above.
- Produces: the report that part 5 is whole.

- [ ] **Step 1: Every core test, with its count**

```bash
./gradlew :core:test --console=plain && ./bl test && for f in core/build/test-results/test/TEST-io.github.kuscher.booklight.core.FirstRun*.xml core/build/test-results/test/TEST-io.github.kuscher.booklight.core.SetDownTest.xml core/build/test-results/test/TEST-io.github.kuscher.booklight.core.ShowTest.xml; do n=${f##*core.}; echo "${n%.xml} $(grep -o 'tests="[0-9]*"' "$f" | head -1) $(grep -o 'failures="[0-9]*"' "$f" | head -1) $(grep -o 'errors="[0-9]*"' "$f" | head -1)"; done
python3 -c "import glob,re; print(sum(int(re.search(r'tests=\"(\d+)\"', open(f).read()).group(1)) for f in glob.glob('core/build/test-results/test/TEST-*.xml')))"
```

Expected: `BUILD SUCCESSFUL`, no output from `./bl test`, then fifteen lines, each ending `failures="0" errors="0"`:

```text
FirstRunAgainTest tests="11"
FirstRunAnswersTest tests="14"
FirstRunGateTest tests="7"
FirstRunHoldTest tests="12"
FirstRunKeyStepTest tests="8"
FirstRunLessonsTest tests="10"
FirstRunOpeningsTest tests="10"
FirstRunPlayingTest tests="9"
FirstRunRecipeTest tests="12"
FirstRunStageTest tests="13"
FirstRunStartTest tests="12"
FirstRunTest tests="15"
FirstRunUpdateTest tests="10"
SetDownTest tests="12"
ShowTest tests="13"
```

and then `720`: 143 first-run tests in thirteen classes, the schedule's twelve and the show's thirteen; 32 of them are this part's (`FirstRunAgainTest`'s eleven, `SetDownTest`'s twelve, two more each in `FirstRunTest`, `FirstRunPlayingTest` and `FirstRunStageTest`, one more each in `FirstRunOpeningsTest`, `FirstRunRecipeTest` and `ShowTest`), and 720 core tests in all (688 before this part).

- [ ] **Step 2: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 3: What must be gone is gone**

```bash
grep -rn "win_key_or" app/src || echo CLEAN
grep -n '"Alt"' app/src/main/java/io/github/kuscher/booklight/window/Pages.kt || echo CLEAN
grep -nE "Action \+ K\b|Alt \+ Space|App shortcuts" README.md || echo CLEAN
grep -n "fadeIn(snap())" app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt || echo CLEAN
grep -n "0.38f to NIGHT" app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt || echo CLEAN
grep -n "private const val SEEN_MS" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt || echo CLEAN
grep -n "private class Sprung\|model.cast != null" app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt || echo CLEAN
grep -n "announceForAccessibility\|LocalView\|fun KeyCaps\|private fun Recipe(" app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt || echo CLEAN
grep -rn "where a card stood" core/src || echo CLEAN
grep -rniE "first-run card" README.md PRIVACY.md CLAUDE.md docs/design/design-system.md docs/design/ux-model.md store-submission/README.md store-submission/forms core/src app/src || echo CLEAN
```

Expected: ten times `CLEAN` (the Start page's "or"; its Alt cap; the old way to a key in the README; the list that needed a frame to begin; the night's shade with an edge; the 350 ms outside core; the welcome's own springs and its own question whether there is a show; the stage speaking behind the model's back, and its two old parts; the core's header and the documents speaking of a card).

- [ ] **Step 4: The rules hold**

```bash
grep -rn "^import android" core/src/main || echo CLEAN
grep -rn "withFirstRun(" app/src | grep -v "fun Settings.withFirstRun\|fun Prefs.firstRun\|fun Settings.asUpdate\|^app/src/debug" || echo CLEAN
grep -rn "suggestions = true" app/src/main/java core/src/main | grep -v "core/FirstRun.kt" || echo CLEAN
grep -rn "executor.run(" app/src/main/java/io/github/kuscher/booklight/overlay | grep -v "OverlayActivity.kt.*app.executor.run(a.effect, this)\|OverlayModel.kt.*app.executor.run(if (dir < 0) n.down else n.up)" || echo CLEAN
grep -rn "startActivity\|startActivities" app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt app/src/main/java/io/github/kuscher/booklight/overlay/Welcome.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt || echo CLEAN
git diff 5edba67 -- app/src/main/AndroidManifest.xml app/src/debug/AndroidManifest.xml | grep . || echo CLEAN
git diff 5edba67 -- app/src/main core/src/main | grep -E "^\+.*(HttpURLConnection|openConnection|URL\(|\.fetch\(|Socket\(|okhttp)" || echo CLEAN
git diff 5edba67 -- app/build.gradle.kts core/build.gradle.kts build.gradle.kts settings.gradle.kts gradle | grep . || echo CLEAN
grep -n "versionCode\|versionName" app/build.gradle.kts
grep -rnE "[^a-zA-Z](delay|tween)\([0-9]" app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt || echo CLEAN
grep -c "cameAt = SystemClock.uptimeMillis()" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
grep -c "performer?.cancel(); performer = null" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
grep -c "a.effect == FirstRun.AGAIN" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt; grep -c "R.string.first_words, FirstRun.AGAIN" app/src/main/java/io/github/kuscher/booklight/providers/Providers.kt
git diff 5edba67 -- app/src/main/res/values | grep -c "^+ *<string"; git diff 5edba67 -- app/src/main/res/values-de | grep -c "^+ *<string"
git log 5edba67..HEAD --format=%B | grep -inE "co-authored-by|claude|generated with|session" || echo CLEAN
grep -rnE "emulator-|ANDROID_SERIAL=[A-Z0-9]" docs/research/first-run-last-pass.md docs/PICKING-UP.md CHANGELOG.md README.md || echo CLEAN
grep -c "^Answer: not run$" docs/research/first-run-last-pass.md
awk '/^## 2\. /{f=1} /^## 3\. /{f=0} f && /debug guide/{n++} END{print n+0}' docs/research/first-run-last-pass.md
git diff 5edba67 --stat -- docs/design/first-run/BUILD.md app/src/main/java/io/github/kuscher/booklight/overlay/Glass.kt | grep . || echo CLEAN
git log --oneline 5edba67..HEAD | wc -l
git status --short
```

Expected, in this order: eight times `CLEAN` (no Android in core; no write of first run's state in the app but through `Prefs.firstRun`; suggestions switched on in core's `answer` alone; no way to the executor from the panel but `OverlayActivity.run`'s and a level's nudge; no activity started by the stage, the welcome or the model; no change to a manifest, so no new permission and no `<queries>` line; no network call added; no change to a build file); `versionCode = 8` and `versionName = "3.0"` (no version bump); `CLEAN` (no time written into the stage: they are `Motion.kt`'s); `1` (one place says when a screen came: `came`); `1` (one place ends the piece: `stop`); `1` and `1` (the command is caught in one place and made in one); `9` and `9` (the new and changed strings, in both languages); twice `CLEAN` (no attribution in the commits of this part; no serial in the documents); `61` (every check still reads "not run"); `2` (`./bl debug guide` stands in check 2 of the device list: what to run, and what it must answer); `CLEAN` (`BUILD.md` is the coordinator's, and the glass's shader is not changed); the number of commits since `5edba67` (`16`, and one more for each fix a review asked for); and no line from `git status`.

- [ ] **Step 5: Report.** The commits of Tasks 1 to 16 by their first lines, the results above, that the branch was not pushed and nothing was released, and that the sixty-one device checks in `docs/research/first-run-last-pass.md` are all "not run": check 1 first (the panel still opens, and nobody else sees a change), and `./bl debug guide` in check 2 (every example of the list of everything still finds its row, "first steps" among them).


---

## What is left after part 5

Nothing of first run is left to plan. What is left is to look, to decide and to release.

| What | Whose | Where it is written |
| --- | --- | --- |
| The last pass on the test device: checks 1 to 37 of this part, and the earlier lists' open checks marked [C] | The coordinator | `docs/research/first-run-last-pass.md` |
| The checks marked [A]: a real click on the icon, a keyboard without Quick Insert, the system in German, a larger font scale, "Set shortcut" where Booklight has no key yet, a screen reader's own voice, the HP Googlebook, and what only a person can say (is the key left of A; does a set-down feel like keys being set down or like a delay) | Alex | The same file |
| What the device says must be tuned: every time is one constant in `overlay/Motion.kt` (`PACE`'s, `Lights`), and `SEEN_MS` in core | Whoever looks | `motion.md` beside the films |
| What this plan left out (rows 24 to 28 of "Decided here"): the pill condensing into the grid's first cell and the cells leaving as a wave; the highlight's way from the middle of a move; a click's own time; a leave turned round on the choices; the old shortcut command and the Play listing's paragraph on the key | Alex, to ask for or to leave | "Decided here" above |
| `docs/design/first-run/BUILD.md`, part 5: what was built, what was decided on the way, what the device said | The coordinator | That file |
| Alex's answers to `BUILD.md`'s table "Decided on the way" and to this plan's "Decided here" | Alex | Both tables |
| The branch: `first-run` is local and not pushed. Merging it, the version, the store listing's text about the key, the release | Alex's word | `docs/PICKING-UP.md`, "Before a release"; `docs/RELEASING.md` |
