# First run, part 3: the lessons. Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A new installation whose key works goes on, in the same opening, through three lessons that are practice (open an app, search inside an app, a sum), is asked once, alone and with nothing armed, whether searches may be suggested, is offered "Show your usual" and the list of everything, and is left with the bare field and its run marked done.

**Architecture:** Core `FirstRun` gains what the screens after the key decide: every screen's answers, that the stage's keys count only once the stage can be seen, the coach line, this device's example and each lesson's recipe. In the app, `OverlayModel` says three things where it said one: which screen is `due` in this panel whatever the field holds, whose `stage` has the place under the empty field, and which `lesson` stands, also while its list is typed. `overlay/FirstStage.kt` draws the lessons and the question on the key's skeleton, each at a fixed height from `Metrics`. Practice is decided in one place, `OverlayActivity.run`, where everything that runs passes, before the executor is asked. The choices are a real list of two rows with nothing selected, as the usual rows stand at rest. Every write of first run's state goes through `Prefs.firstRun { … }`.

**Tech Stack:** Kotlin 2.4, JUnit 4 in `core/` (plain JVM, no `android.*`), Jetpack Compose in `app/` (compile SDK 37, min SDK 34), the repo's `./bl` helper over adb.

**Spec:**
- `docs/design/first-run/00-brief.md`, **"Settled for the build"**: it wins over every other paper. For this part: point 6 (nothing opens in first run: the lessons are practice) and point 7 (Booklight never opens itself over another window).
- `docs/design/first-run/BUILD.md`: what parts 1 and 2 built, the table "Decided on the way", and "Known, and left for a later part".
- `docs/design/first-run/design.md` §2 (heights), §4 (the skeleton), §6 (the lessons, with "Practice only" and "While the user types"), §7 (the question, the choices, the ending), §10 (every state).
- `docs/design/first-run/pm.md` §3, §4, §7, §8 (what each step is for; the question's words).
- `docs/design/first-run/eng.md` §5, §6, §12.1 ("Practice only", "State and resume"). `docs/design/first-run/motion.md` only for what is *not* built here: its transitions are part 5's.
- `docs/superpowers/plans/2026-10-04-first-run-2-the-key.md`: part 2, on which this builds.

**Who sees what in a build of this part alone.** A new installation: the key's step as in part 2; after "Go on" the three lessons, the question (only while suggestions are off) and the choices, in the panel that is open, and then the bare field. An unfinished run comes back at the next opening where it stopped, in three openings in all, then waits. An installation that was there before and has no key: the key's step alone, as in part 2. Someone with a key and no run: no change. There is no welcome and no show (part 4); nothing is animated that was not before, there is no "First steps" command and no row for it in the window, and "Not now" on the key's step still leaves no word about where first run waits (part 5).

## Global Constraints

- Branch `first-run`. Local only: never pushed, no tag, no release.
- No attribution lines of any kind in commits or code: no `Co-Authored-By`, no Claude lines, no session links.
- The repo is public: no device serials, no adb names, no build numbers, nothing about what is installed on a device, in any file or commit message. The lessons' example names an app of the device: it is shown on the glass and by `./bl debug first`, and is never written into a file or a commit message.
- `core/` has no `android.*` import.
- Every user-facing string is a resource, in English (`res/values`) and German (`res/values-de`). In German a quotation opens with „ (U+201E) and closes with “ (U+201C); an English apostrophe is ’ (U+2019).
- No new permission, and no new line in `<queries>`.
- Comments in the surrounding code's own style and density: plain sentences that say why; a KDoc where the neighbours have one.
- Never run `uiautomator dump`. A device is chosen by its model (`adb devices -l`), never by its place in the list. Only the coordinator touches a device: the engineer builds, and runs no `./bl` command but `./bl test` and `./bl build`.
- **Every write of first run's state goes through `Prefs.firstRun { … }`** (`data/Prefs.kt`): the change is worked out inside the settings' own update, never from a state read before it. In `OverlayModel` that is `step { … }`, in `OverlayActivity` it is `first(app) { … }`. `Settings.withFirstRun` also writes `keySeen` and `suggestions`, the consent switch.
- **Suggestions go on by "Agree" on the question and by nothing else**: core `FirstRun.answer(…, Answer.AGREE)`, written through `Prefs.firstRun`. No other code of first run sets `suggestions`.
- **The order for every panel** is: the key landing (if the key made the panel), then the opening counted, then what stands. Nothing asks `FirstRun.stage` or `FirstRun.overture` before `FirstRun.opening` was applied.
- **Nothing opens while a lesson stands.** An app's Open and a search inside an app are practice; a sum is copied for real and the panel stays; everything else runs as every day. That is decided by core `FirstRun.enters`, asked in `OverlayActivity.run` before the executor: everything that runs passes there. No task gives an action another way to the executor. Booklight never starts itself again over another window; first run asks the system nothing about its on-device model.
- **Someone who has no run sees, writes and pays nothing new at an opening.** What this part adds is asked only where `OverlayModel.due` is not null: no new job, no new read of the system, no write of the settings for anyone else.
- The panel's rules hold: the window is exactly the panel, one fixed height for each screen in `Metrics`, and `Metrics.height(model)` equals what `Panel` draws; nothing outside the glass; the field is never covered and typing never waits.
- Motion is part 5: this part draws stills. Where the design says a thing rolls or fades and a helper for it exists (`Motion.roll()`, `Motion.fade()`, `Motion.pop()`, `DrawnCheck`), the helper is used; no spring and no time is added to `Motion.kt`.
- The suggested key is Action + Quick Insert; a keyboard without that key is offered Action + M (not J). The key's step is part 2's and is not changed here but where a task says so.
- Use a task's code as it is written. A "Find" is a text that stands in its file exactly once at the moment the task runs; "Find, in one long line" is a piece of one line, which also stands in the file exactly once. If a Find does not stand exactly once, stop and report it: do not guess the place.

**The commands of this repo, as every task uses them**
- All core tests: `./bl test` (quiet: no output and exit status 0 mean every test passed).
- One test class: `./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SomeTest' --console=plain`.
- A debug build with its check: `./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT` (the build is good only if this prints the one word `BUILT`).

**Where a late fix to part 2 would touch this plan.** It is written on `first-run` at `f17ebbe`. Every "Find" below stands in its file exactly once there, or once the tasks before it have run. If a later fix changes one of these places, the task that finds it must read the place again: `core/…/FirstRun.kt` (`answers`, `armed`, the end of `ran`), `core/…/Zero.kt` (`Under`), `OverlayModel.kt` (`stage`, the `init` after `awaitsKey`, `step`, `stageTab` to `stageEnter`, `firstChanged`, `up`, `type`, `enterScope`, `typeOut`, the empty field's branch of `search`, `enter`, `runRow`, the companion's last line), `OverlayActivity.kt` (`settled`, `onCreate` from the count to `placeWindow`, `run`, `close`, `onStop`, the companion), `Panel.kt` (`keys`' Tab, the announcement of the usual rows, `val full = maxWidth`), `Metrics.kt` (`stage`, `rowHeight`, `height`), `Rows.kt` (`ResultsBody`'s call of `ResultRow`, `ResultRow`'s parameters, `described`, the strip), `Strip.kt` (`ActionStrip`'s parameters and its `drawBehind`), `Footer.kt` (the left seat, `first`), `Field.kt` (the placeholder), `DebugReceiver.kt` (the header, the `key` case, `dump`, the `first` case's last line, `describe`), the last line of both `strings_32.xml`, the manifest's comment on `INTERNET`, `CLAUDE.md` (the `FirstRun.kt` line, the first-run paragraph of the dev loop, the gotcha on hover), and `bl`'s header.

**How this plan was checked.** Every task below was applied from this text, in order, to a scratch checkout of `f17ebbe`: each "Find" stood exactly once when its turn came; the failing tests of Tasks 1 to 3 failed as said and passed after; after every task `./bl test` passed and the debug build printed `BUILT`; Task 14's commands gave what it expects. Nothing was run on a device: what the screens look like and how they behave there is Task 13's list, every answer of which reads "not run".

## Review Focus

Five things the papers imply that would bite a real user and that nothing would exercise unless a test is written for it. Each has its test in the task that owns the code.

| # | The input or condition | What must hold | Its test |
| --- | --- | --- | --- |
| 1 | On the question: Enter alone; Tab and Enter pressed blind while the panel is still opening; an answer the question does not offer; three openings without an answer; "Not now"; the choices left | Suggestions are on after "Agree" and after nothing else, and "Agree" can only be run by an Enter that follows a Tab (or a pointer) the user has seen arm it | Task 2: `enterGivesTheQuestionNoAnswerUntilOneIsChosen`, `keysPressedBeforeTheStageIsThereDoNothing`, `nothingButAgreeSwitchesSuggestionsOnThroughAWholeRun` |
| 2 | In lesson 3 on a device where no app can be searched: the recipe shows the Settings keyword and a page; elsewhere a settings page is as every day | What the recipe shows is what Enter then practises: the page under the keyword starts nothing where the example says Settings stands in, and opens as every day where the example is an app. (That an app's Open, a place and a search inside an app start nothing in a lesson, and that nothing is taken from Enter where no lesson stands, is pinned by part 1's `FirstRunLessonsTest`, which stays green unchanged) | Task 3: `whereNoAppCanBeTheExampleSettingsStandsIn`; part 1: `nothingOpensInALesson`, `outsideTheLessonsEnterIsAsEveryDay` |
| 3 | A device where the first letters of the example's name do not put its row first; an app whose name has one short word; no app that can be searched; no Settings app either | The recipe never sends the user to a row that is not there: letters are shown only where they lead, three or four of them; else Settings by its keyword, with no Enter after Tab; else the keys alone | Task 3: `theLettersShownAreTheShortestThatPutTheAppFirst`, `anAppsLettersAreTheStartOfItsFirstWordThreeAtLeastFourAtMost`, `aRecipeShowsNoMoreThanItHasRoomFor` |
| 4 | The selected row is not the lesson's own (a sum in lesson 2, an app without Search in lesson 3, the web's row, App info, nothing selected); no lesson stands (no run, the key's step, the question, a run that waits) | The coach line never promises what Enter will not do: the lesson's title stands there instead; and where no lesson stands there is no coach line at all, on any day | Task 3: `whereTheSelectedRowIsNotTheLessonsItsTitleStands`, `whereNoLessonStandsThereIsNoCoachLine`, `theSettingsPageThatStandsInIsSaidToOpen` |
| 5 | A run left at a lesson, at the question or at the choices and opened again and again; a whole run done in one opening | Every screen is counted now, not only the key's: three openings wherever the run stopped, then it waits with what was done kept and nothing answered for the user; what follows in the same panel is not counted again; the choices left, nothing of the run is kept | Task 2: `aRunPastTheKeyStandsInThreeOpeningsThenWaits`, `whatFollowsInTheSamePanelIsNotCountedAgain` (they pin what Task 12 switches on in `OverlayActivity.onCreate`) |

Also pinned: Enter on a lesson's empty field does nothing and Tab, Enter skips (Task 2, `tabThenEnterSkipsALesson`); a lesson offers Skip, the question Agree then Not now, the choices no answers (Task 2, `aLessonOffersSkipAndTheQuestionAgreeThenNotNow`); lesson 3's recipe builds on lesson 2's app (Task 3, `lessonThreeBuildsOnLessonTwosApp`). No JVM test reaches the app: that a second Enter does nothing while a lesson's list waits to give way, that typing on keeps the list, that the heights are what is drawn, and that the question's text is never cut are Task 13's checks 8, 9, 22 and 15 on a device.

## Decided here

Where the papers differ, or are silent. Each is Alex's to overturn.

| # | What | Why | What it costs if wrong |
| --- | --- | --- | --- |
| 1 | **While a lesson's list is typed, the lesson's stage gives way to the real list**, whose row one stands where the seat stood; the lesson goes on standing as the coach line at the footer's left end and as what Enter does. The brief for this plan said "its seat stays above the typed list" | `design.md` §6, "While the user types" ("No line is added under the field: it would push row one off y = 76"), `motion.md` T7 and the prototype all have it so, and the papers win over a summary of them | A seat above the list is one composable above `ResultsBody` in `Panel` and 56 dp in `Metrics.height`: about one task. The model already says `lesson` apart from `stage`, which is what that would need |
| 2 | **The stage's keys (Tab, Enter, the pointer's arming) count only once the glass has opened**, on every screen, not on the question alone | One rule in core, tested; `motion.md` T1 and §5 say Enter counts from the answers' first frame | On K1 an Enter pressed during the opening no longer asks for the dialog, which part 2 let it do. To narrow it to the question: one condition in `FirstRun.entered` and `tabbed` |
| 3 | **After a lesson's Enter the list stands for 520 ms and then gives way to whatever stands then**, also for a lesson's thing done out of turn (the same lesson stands again); **not if the user has typed on**: then it comes when the field is empty again | "520 ms later the list gives way to the next lesson"; typing never waits, and nobody's text is emptied under their hands | One function (`OverlayModel.giveWay`) |
| 4 | **Practice is decided in `OverlayActivity.run`**, not in `OverlayModel.enter` as `eng.md` §12.1 has it | It is the one place everything that runs passes: Enter, a click, Ctrl + digit, an Enter that waited for its list or for an answer. `enter` is not | None known. `eng.md`'s place would leave Ctrl + digit opening an app in a lesson |
| 5 | **Until the list gives way a second Enter does nothing**, by the activity's flag `settled`, which already keeps a second Enter from running a row while the panel waits to close | `eng.md` §12.1, "the lock": after the last lesson its list still stands, and core then says "as always" | Enter is dead for half a second after a lesson's Enter, also for something typed in that half second |
| 6 | **A practice Enter marks its line of the list of everything as used** (tips pass over it), **and teaches the ranking nothing** | `pm.md` §7: tips pass over what first run had the user do; `eng.md` §12.1: nothing is learned | A tip for "search inside an app" comes although it was practised, or does not |
| 7 | **The example**: the app `Guide.appSearch()` picks, by the shortest start of its name, three or four letters, for which the engine's row one is that app *and offers Search*; the word to look for is the one the list of everything uses (`guide_link_text`, "lofi"). If no start leads, or no app can be searched: lesson 2 names the Settings app by the letters of its name on this device, lesson 3 goes by the Settings keyword to the page `wifi`, with no Enter after Tab. With no Settings app either: the keys alone | `design.md` §6 and §10 (letters cut to four); the check for Search is added so that lesson 3's Tab has somewhere to go | A device whose apps share their first four letters gets the Settings stand-in although an app could be searched |
| 8 | **The example is worked out for each panel, off the main thread, only where a screen is due; until it is there lessons 2 and 3 show no recipe** and the ordinary placeholder, and the lesson stands all the same | `eng.md` §5 would not show the lesson in that opening and not count it; since part 2 the opening is counted before the model exists | On a cold start straight onto lesson 2 or 3 the recipe comes a moment after the seat |
| 9 | **Whether a settings page stands in for lesson 3 is what the example says** (`Example.enter`), not asked of the device again at Enter | What the recipe shows is what Enter practises (Review Focus 2) | With the example not yet worked out a settings page opens as every day |
| 10 | **The question is 324 dp, and taller by what its text needs beyond four lines**, measured when the question becomes due | `design.md` §10: five lines make it 344, "never cut". Measuring also covers a larger type size | `FirstRun.MIN_SCREEN_DP` (475) still counts on 324: on a screen under about 500 dp with a five-line text the panel ends nearer the screen's edge than 56 dp |
| 11 | **The choices are `results` with nothing selected, not a stage**; their two rows are the panel's own (no provider). The switch's action is `Effect.Internal("usual")`, caught by the panel before anything runs, for Enter, a click and Ctrl + digit. The second row is a scope's row with its key (`?`) as its label: Enter and Tab both enter the list of everything. Core gains `Body.Switch` | `eng.md` §1 and §6 ("C is a real list"; the switch is caught "as Don't suggest is") | Another way to hold the two rows; the drawing would stay |
| 12 | **The choices are left, and the run is over, also when the panel closes or is covered** (Esc, a lost focus, the key again, the lock screen); and Up at rest does nothing there | `design.md` §7: "C is done when it is left, however it is left". Up would bring the last text back, which leaves them | Someone who closes the panel on the choices by mistake is not offered "Show your usual" again unasked; it is in the window |
| 13 | **The Enter that answered the question does not also end the choices**: an Enter within 350 ms of their coming is not taken | The same guard the panel has for a field it filled itself | A quick second Enter is lost once |
| 14 | **In the choices' footer "Done" gives way only to a selected row's own hint**; the Labs line ends in an ellipsis where it has no room | `design.md` §10 has "Done" give way before the line is cut. By estimate German has about 35 dp to spare | A measuring step in `Footer`; Task 13's check 23 looks at it |
| 15 | **The ending's placeholder also stands** where the choices were left by a typed letter and the field is empty again in the same opening | "For this one opening the placeholder is …" | One condition |
| 16 | **`first_later_hint`, the word "Not now" leaves on the key's step, is not built here** | There is nothing in the window yet for first run to wait in: part 5 builds both | None: part 5's |
| 17 | **German**: „Aktion“ wherever first run names the key (the dialog's row 3 said „Aktionstaste“), and `a11y_first_keys` reads „Vorgeschlagene Tasten: …“ | The system's own word for the key is „Aktion“; „Vorschlag:“ before two keys read as one suggestion of something else | Two strings |
| 18 | **The pressed slot** is drawn in the row's own strip with the design system's pressed token (ink 0.08, 80 ms in, 120 out). The stage's own answers (Skip, Go on, Agree) are not drawn pressed here | "The slot shows as pressed" is this part's; `motion.md`'s pressed answers are part 5's | One parameter of `ActionStrip` |
| 19 | **The screen reader's words for this part's screens are built here**: each stage is said once when it comes, the coach line is a polite live region, the choices are said with each row's state, the ending is said | Part 2 did the same for the key's step; `eng.md` had them in a later milestone | Five strings |
| 20 | **The system's words for the dialog are read at an opening only where the key's step stands** | No lesson quotes them | A replay that goes from a lesson back to the key reads them a moment late (the debug hook loads them itself) |
| 21 | **Where Settings stands in for lesson 3, Enter on its page is said to open it**: the coach line reads "opens it" and the footer's word "That opens …" with the page's name, not "searches" | It is what that Enter would do | Two conditions |
| 22 | **`store-submission/forms/data-safety.md` still quotes the old card, and `PRIVACY.md` is not touched**; the manifest's comment on `INTERNET` is brought up to date | The documents are part 5's; `PRIVACY.md` stays true ("off until you turn them on") | The store form is stale until part 5, as it has been since part 2 |

## What each task hands the next

For every pair of tasks that share a file or a name: what the one produces and the other consumes. Where a later task's "Find" is text an earlier task wrote, the row says so.

| From | To | What |
| --- | --- | --- |
| 1 | 11 | `Under.choose(untouched, stage, copy, zero, seats, tip, last)` and `Under.What.STAGE`. The app's one caller, `OverlayModel.offerZero`, passes by position: no task changes that call |
| 2 | 3 | `core/…/FirstRun.kt`: Task 2 rewrites the section "the stage, as it stands" near the file's end; Task 3 adds after `ran`, in the section "the lessons". They do not meet |
| 2 | 6 | `FirstRun.tabbed(s, armed, back, arrived): Int`, `FirstRun.entered(s, armed, arrived): Answer?` |
| 2 | 7 | `FirstRun.answers(s)` with `SKIP` for a lesson and `AGREE`, `NOT_NOW` for the question; `FirstRun.armed(s)` is -1 for both |
| 2 | 11, 12 | Pinned for them: `FirstRun.answer(s, Answer.DONE)` on the choices ends the run, and `FirstRun.opening` counts every screen |
| 3 | 5 | `FirstRun.Example`, `FirstRun.example(app, word, settings, keyword, page)`, `inline fun FirstRun.letters(name, leads)` |
| 3 | 7 | `FirstRun.Part` (`Typed(text)`, `Key.TAB`, `Key.ENTER`), `FirstRun.recipe(s, example, sum)` |
| 3 | 8 | Part 1's `FirstRun.enters(s, line, effect, searchable)` and `FirstRun.ran(s, enter)` get their first callers; `Example.enter` is what is passed as `searchable` |
| 3 | 9 | `FirstRun.Coach`, `FirstRun.coach(s, enter, effect, search)`, `FirstRun.MOST_LETTERS` |
| 4 | 5 | `R.string.first_search_page` |
| 4 | 7 | The lessons' and the question's words, `first_skip`, `first_agree`, `first_key_tab`, `first_key_enter`, `first_sum_example`, `a11y_first_example`, `a11y_first_skips`, `a11y_first_asks` |
| 4 | 8 | `first_practice_open`, `first_practice_search` |
| 4 | 9 | `first_open_hint`, `first_search_hint`, `first_coach_open`, `first_coach_tab`, `first_coach_into`, `first_coach_search`, `first_coach_sum` |
| 4 | 11 | `first_usual_text`, `first_on`, `first_off`, `first_labs`, `first_end_hint`, `a11y_first_choices`, `a11y_first_done` |
| 5 | 7 | `OverlayModel.due` (the panel asks `model.due == FirstRun.Screen.Q`), `stage`, `example`. In `OverlayModel.kt`, the comment above the `init` that asks the keyboards, as Task 5 words it: Task 7's "Find", above which it puts `askMore` |
| 5 | 8, 9 | `OverlayModel.lesson`, `stage`, `example` |
| 5 | 11 | `OverlayModel.due`; `firstChanged` as Task 5 leaves it (its line `if (due != null) exemplify()` is part of Task 11's "Find") |
| 5 | 12 | `due` with the key's limit, and its comment: Task 12's "Find" |
| 5 | 13 | `OverlayModel.due`, `example` |
| 6 | 13 | `OverlayModel.stageTab(back)`, `stageArm(index)`, `stageEnter()` do nothing until `arrived`: the debug hooks call them as the keys do |
| 7 | 9, 11 | `overlay/FirstStage.kt` as it is written anew: its private `CAPTION`, its imports, and the comment `/** The "+" of a chord, with 12 dp of air on either side. */`, above which Tasks 9 and 11 each add a composable |
| 7 | 10 | `overlay/Metrics.kt`: Task 7 adds an import and the stage's heights; Task 10 changes one line of `rowHeight`. They do not meet |
| 7 | 11 | `overlay/Panel.kt`: Task 7 adds two lines under `val full = maxWidth`; Task 11 changes `keys`' Tab and adds a line under the usual rows' announcement. They do not meet |
| 8 | 9 | `OverlayModel.enters(r, a): FirstRun.Enter`: Task 9's "Find" is its last line and the comment under it, between which `coach` and `firstHint` go |
| 8 | 10, 11 | `overlay/Rows.kt`: Task 8 adds `pressed` to `ResultRow` and to its call in `ResultsBody`; Task 10 changes `described`, the place before the strip and the scope's key cap; Task 11 changes `calm =` in that call. No two change the same line |
| 8 | 11 | `OverlayModel.giveWay()` and `empty()`: they end in the empty field's branch of `search`, where Task 11 sets the choices down. `overlay/OverlayActivity.kt`: Task 8 changes `settled`, `run` and the companion and adds `lessonOver`; Task 11 changes `onCreate`, `close` and `onStop` |
| 8 | 13 | `OverlayModel.pressed` |
| 9 | 11 | `OverlayModel.firstHint`, which Task 11's "Find" holds whole and whose last branch it changes; `Footer`'s `val quiet`, to which Task 11 adds |
| 9 | 13 | `OverlayModel.coach`, `firstHint` |
| 10 | 11 | Core `Body.Switch(on, word)`; a row with it is 92 dp and shows `PanelSwitch`; a row of kind `SCOPE` with a `label` shows that label as its key |
| 10 | 13 | `DebugReceiver.kt`: Task 10 adds a branch to `describe`, so a dump says a switch's row as `{on}` or `{off}`; Task 13 changes the header, the `key` case, `dump` and the `first` case |
| 11 | 12 | `OverlayActivity.onCreate` calls `model.offerChoices()` after `take(intent)`; Task 12 changes lines above that. `OverlayModel.kt`: Task 12 changes `due` only |
| 11 | 13 | `OverlayModel.choicesUp`, `ended`, `atRest` |
| 12 | 13 | Every screen is due and counted: what the checks check |

---

### Task 1: `Under`: the place under the empty field is the stage's

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/Zero.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/UnderTest.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunTest.kt`

**Interfaces:**
- Consumes (core, in `Zero.kt`): `object Under` with `enum class What { CARD, COPY, USUAL, TIP, NOTHING, WAIT }` and `fun choose(untouched: Boolean, card: Boolean, copy: Boolean?, zero: Boolean, seats: Int?, tip: Boolean, last: Boolean): What`.
- Produces: `Under.What.STAGE` in place of `CARD`, and the parameter `stage` in place of `card`: `fun choose(untouched: Boolean, stage: Boolean, copy: Boolean?, zero: Boolean, seats: Int?, tip: Boolean, last: Boolean): What`. Nothing else changes. The app's one caller, `OverlayModel.offerZero`, passes its arguments by position and compares the answer with `What.USUAL` only: it needs no change.

The two cards went in part 2; core still called the stage's place by their name. This task renames it, and nothing more.

- [ ] **Step 1: Say "stage" in `core/src/test/kotlin/io/github/kuscher/booklight/core/UnderTest.kt`.** Two changes.

Find:

```kotlin
    private fun at(untouched: Boolean = true, card: Boolean = false, copy: Boolean? = false, zero: Boolean = false, seats: Int? = 0, tip: Boolean = false, last: Boolean = false) =
        Under.choose(untouched, card, copy, zero, seats, tip, last)

    @Test fun onceTouchedNothingComes() {
        assertEquals(What.NOTHING, at(untouched = false, card = true, copy = true, zero = true, seats = 3, tip = true))
```

Make it:

```kotlin
    private fun at(untouched: Boolean = true, stage: Boolean = false, copy: Boolean? = false, zero: Boolean = false, seats: Int? = 0, tip: Boolean = false, last: Boolean = false) =
        Under.choose(untouched, stage, copy, zero, seats, tip, last)

    @Test fun onceTouchedNothingComes() {
        assertEquals(What.NOTHING, at(untouched = false, stage = true, copy = true, zero = true, seats = 3, tip = true))
```

Find:

```kotlin
    @Test fun aCardComesFirstAtEitherMoment() {
        assertEquals(What.CARD, at(card = true, copy = true, zero = true, seats = 3, tip = true))
        assertEquals(What.CARD, at(card = true, copy = true, zero = true, seats = 3, tip = true, last = true))
```

Make it:

```kotlin
    @Test fun firstRunsStageComesFirstAtEitherMoment() {
        assertEquals(What.STAGE, at(stage = true, copy = true, zero = true, seats = 3, tip = true))
        assertEquals(What.STAGE, at(stage = true, copy = true, zero = true, seats = 3, tip = true, last = true))
```

- [ ] **Step 2: Say "stage" in `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunTest.kt`.** One change.

Find:

```kotlin
    /** How first run meets the one rule for the place under the empty field: a screen that is due is passed where a card was. */
    @Test fun aScreenThatIsDueHasThePlaceACardHad() {
        fun under(s: State, last: Boolean) = Under.choose(untouched = true, card = FirstRun.screen(s) != null, copy = true, zero = true, seats = 3, tip = true, last = last)
        assertEquals(Under.What.CARD, under(State(Run.NEW), last = false))
        assertEquals(Under.What.CARD, under(State(Run.NEW, done = lessons, key = true), last = true))
```

Make it:

```kotlin
    /** How first run meets the one rule for the place under the empty field: a screen that is due has the place first. */
    @Test fun aScreenThatIsDueHasThePlaceFirst() {
        fun under(s: State, last: Boolean) = Under.choose(untouched = true, stage = FirstRun.screen(s) != null, copy = true, zero = true, seats = 3, tip = true, last = last)
        assertEquals(Under.What.STAGE, under(State(Run.NEW), last = false))
        assertEquals(Under.What.STAGE, under(State(Run.NEW, done = lessons, key = true), last = true))
```

- [ ] **Step 3: Run them and see them fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.UnderTest' --tests 'io.github.kuscher.booklight.core.FirstRunTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'STAGE'` and `No parameter with name 'stage' found` among its `e:` lines.

- [ ] **Step 4: Rename it in `core/src/main/kotlin/io/github/kuscher/booklight/core/Zero.kt`.** One change, from the comment above `object Under` down to the second branch of `choose`.

Find:

```kotlin
 * What stands under the untouched empty field. At most one thing, chosen in one order: a first-run
 * card (or the screen of first run that is due, [FirstRun.screen], which is passed as the card), the
 * line for a fresh copy, the usual rows, a tip. The card comes at once; the others wait
 * for the last moment ([last]: a moment after the panel has opened), except that the usual rows may
 * come before it once it is known that no copy is fresh.
 */
object Under {
    enum class What { CARD, COPY, USUAL, TIP, NOTHING, WAIT }

    /**
     * [untouched]: nothing typed, no chip, no rows, nothing under the field yet. [copy]: a copy is
     * fresh, or null while the system has not said. [zero]: the switch for the usual rows.
     * [seats]: how many of them there are, or null while they are not worked out. [tip]: tips are
     * on and one is left. At the [last] moment nothing waits any longer, and "not known" counts as no.
     */
    fun choose(untouched: Boolean, card: Boolean, copy: Boolean?, zero: Boolean, seats: Int?, tip: Boolean, last: Boolean): What = when {
        !untouched -> What.NOTHING
        card -> What.CARD
```

Make it:

```kotlin
 * What stands under the untouched empty field. At most one thing, chosen in one order: first run's
 * stage (the screen of first run that is due, [FirstRun.stage]), the line for a fresh copy, the
 * usual rows, a tip. The stage comes at once; the others wait for the last moment ([last]: a
 * moment after the panel has opened), except that the usual rows may come before it once it is
 * known that no copy is fresh.
 */
object Under {
    enum class What { STAGE, COPY, USUAL, TIP, NOTHING, WAIT }

    /**
     * [untouched]: nothing typed, no chip, no rows, nothing under the field yet. [stage]: a screen of
     * first run is due. [copy]: a copy is fresh, or null while the system has not said. [zero]: the
     * switch for the usual rows. [seats]: how many of them there are, or null while they are not
     * worked out. [tip]: tips are on and one is left. At the [last] moment nothing waits any longer,
     * and "not known" counts as no.
     */
    fun choose(untouched: Boolean, stage: Boolean, copy: Boolean?, zero: Boolean, seats: Int?, tip: Boolean, last: Boolean): What = when {
        !untouched -> What.NOTHING
        stage -> What.STAGE
```

- [ ] **Step 5: Run them and see them pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.UnderTest' --tests 'io.github.kuscher.booklight.core.FirstRunTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Run every core test, and build the app**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT` (the app's caller passes by position: it compiles unchanged).

- [ ] **Step 7: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/Zero.kt core/src/test/kotlin/io/github/kuscher/booklight/core/UnderTest.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunTest.kt
git commit -m "First run: the place under the empty field is the stage's, by that name (core, tested)"
```

### Task 2: `FirstRun`: every screen's answers, and when the stage's keys count

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunStageTest.kt`
- Modify (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunKeyStepTest.kt`

**Interfaces:**
- Consumes (in `object FirstRun`, parts 1 and 2): `State`, `Run`, `Screen` (`K1…K4, L2, L3, L4, Q, C`), `Answer { OPEN_HELPER, NOT_NOW, GO_ON, CHANGE_KEY, SKIP, AGREE, DONE }`, `Enter { AS_ALWAYS, PRACTICE_OPEN, PRACTICE_SEARCH, COPY_STAYS }`, `MAX_OPENS`, `fun screen(s: State): Screen?`, `fun answer(s: State, answer: Answer): State`, `fun ran(s: State, enter: Enter): State`, `fun keyLanded(s: State): State`, `fun stage(s: State, screenDp: Float): Screen?`, `fun opening(s: State, screenDp: Float, plain: Boolean): State`, `fun answers(s: State): List<Answer>` (the key's screens only), `fun armed(s: State): Int`, `fun tab(armed: Int, count: Int, back: Boolean): Int`.
- Produces, in `object FirstRun`:
  - `answers(s)` also answers for the other screens: `listOf(Answer.SKIP)` on `L2`, `L3`, `L4`; `listOf(Answer.AGREE, Answer.NOT_NOW)` on `Q`; empty on `C` and where nothing stands.
  - `armed(s)` returns what it did (-1 on every screen but `K1`, `K3`, `K4`); its comment says why for a lesson and for the question.
  - `fun tabbed(s: State, armed: Int, back: Boolean, arrived: Boolean): Int`
  - `fun entered(s: State, armed: Int, arrived: Boolean): Answer?`

Part 2's `answers` and `armed` knew only the key's screens, and the model's keys acted from the panel's first moment, before anything was drawn. On the question an answer is consent: this task puts the rule "a key counts once the stage can be seen" into core, where it is tested, and pins that nothing but Agree switches suggestions on, and that every screen is counted at an opening (which Task 12 switches on in the activity).

- [ ] **Step 1: Write the failing test `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunStageTest.kt`**

```kotlin
package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Answer
import io.github.kuscher.booklight.core.FirstRun.Enter
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunStageTest {
    private val steps = listOf("key", "open", "search", "sum")
    private val k1 = State(Run.NEW)
    private val l2 = State(Run.NEW, done = steps.take(1), key = true)
    private val l3 = State(Run.NEW, done = steps.take(2), key = true)
    private val l4 = State(Run.NEW, done = steps.take(3), key = true)
    private val q = State(Run.NEW, done = steps, key = true)
    private val c = State(Run.NEW, done = steps + "ask", key = true)

    @Test fun theStatesOfThisTestStandOnTheirScreens() {
        assertEquals(listOf(Screen.K1, Screen.L2, Screen.L3, Screen.L4, Screen.Q, Screen.C), listOf(k1, l2, l3, l4, q, c).map { FirstRun.screen(it) })
    }

    @Test fun aLessonOffersSkipAndTheQuestionAgreeThenNotNow() {
        for (s in listOf(l2, l3, l4)) assertEquals(listOf(Answer.SKIP), FirstRun.answers(s))
        assertEquals(listOf(Answer.AGREE, Answer.NOT_NOW), FirstRun.answers(q))
        // The choices are a list of their own: no answers stand beside them.
        assertEquals(emptyList<Answer>(), FirstRun.answers(c))
    }

    @Test fun nothingIsArmedInALessonNorOnTheQuestion() {
        for (s in listOf(l2, l3, l4, q, c)) assertEquals(-1, FirstRun.armed(s))
    }

    /** Enter on a lesson's empty field does nothing, as every day. Tab, then Enter skips the lesson. */
    @Test fun tabThenEnterSkipsALesson() {
        assertNull(FirstRun.entered(l2, FirstRun.armed(l2), arrived = true))
        val armed = FirstRun.tabbed(l2, FirstRun.armed(l2), back = false, arrived = true)
        assertEquals(Answer.SKIP, FirstRun.entered(l2, armed, arrived = true))
        assertEquals(Screen.L3, FirstRun.screen(FirstRun.answer(l2, Answer.SKIP)))
    }

    /** The consent. Enter alone gives the question no answer; Tab chooses Agree, then Not now, and round again. */
    @Test fun enterGivesTheQuestionNoAnswerUntilOneIsChosen() {
        assertNull(FirstRun.entered(q, FirstRun.armed(q), arrived = true))
        val first = FirstRun.tabbed(q, FirstRun.armed(q), back = false, arrived = true)
        val second = FirstRun.tabbed(q, first, back = false, arrived = true)
        assertEquals(Answer.AGREE, FirstRun.entered(q, first, arrived = true))
        assertEquals(Answer.NOT_NOW, FirstRun.entered(q, second, arrived = true))
        assertEquals(first, FirstRun.tabbed(q, second, back = false, arrived = true))
    }

    /** Keys pressed blind, while the panel is still opening and nothing of the stage can be seen, choose nothing and answer nothing. */
    @Test fun keysPressedBeforeTheStageIsThereDoNothing() {
        for (s in listOf(k1, l2, q)) {
            assertEquals(FirstRun.armed(s), FirstRun.tabbed(s, FirstRun.armed(s), back = false, arrived = false))
            for (armed in -1..1) assertNull(FirstRun.entered(s, armed, arrived = false))
        }
        // (Once it is there, a key's screen answers Enter at once: its first answer is armed.)
        assertEquals(Answer.OPEN_HELPER, FirstRun.entered(k1, FirstRun.armed(k1), arrived = true))
    }

    /** Through a whole run, whatever else is pressed or left undone, suggestions are on only after Agree. */
    @Test fun nothingButAgreeSwitchesSuggestionsOnThroughAWholeRun() {
        var s = FirstRun.answer(FirstRun.keyLanded(State(Run.NEW)), Answer.GO_ON)
        for (enter in listOf(Enter.PRACTICE_OPEN, Enter.PRACTICE_SEARCH, Enter.COPY_STAYS)) s = FirstRun.ran(s, enter)
        assertEquals(Screen.Q, FirstRun.screen(s))
        assertFalse(s.suggestions)
        // Enter at rest, and Tab and Enter pressed blind: no answer at all.
        assertNull(FirstRun.entered(s, FirstRun.armed(s), arrived = true))
        assertNull(FirstRun.entered(s, FirstRun.tabbed(s, FirstRun.armed(s), back = false, arrived = false), arrived = false))
        // An answer the question does not offer changes nothing.
        for (a in Answer.entries - Answer.AGREE - Answer.NOT_NOW) assertEquals(s, FirstRun.answer(s, a))
        // Left unanswered in three openings, the run waits: nothing was answered for the user.
        var left = s
        repeat(FirstRun.MAX_OPENS + 1) { left = FirstRun.opening(left, 1200f, plain = true) }
        assertNull(FirstRun.stage(left, 1200f))
        assertFalse(left.suggestions)
        // Not now, and then the choices left: still off.
        val no = FirstRun.answer(s, Answer.NOT_NOW)
        assertEquals(Screen.C, FirstRun.screen(no))
        assertFalse(FirstRun.answer(no, Answer.DONE).suggestions)
        assertTrue(FirstRun.answer(s, Answer.AGREE).suggestions)
    }

    /** Every screen is counted, not only the key's: an unfinished run stands in three openings wherever it stopped, then waits with what was done kept. */
    @Test fun aRunPastTheKeyStandsInThreeOpeningsThenWaits() {
        for (start in listOf(l2, l3, l4, q, c)) {
            var s = start
            repeat(FirstRun.MAX_OPENS) {
                s = FirstRun.opening(s, 1200f, plain = true)
                assertEquals(FirstRun.screen(start), FirstRun.stage(s, 1200f))
            }
            s = FirstRun.opening(s, 1200f, plain = true)
            assertNull(FirstRun.stage(s, 1200f))
            assertEquals(start.done, s.done)
            assertEquals(start.run, s.run)
        }
    }

    /** One opening for the whole of it: the lessons, the question and the choices follow one another in the panel that is open, and none of them is counted again. */
    @Test fun whatFollowsInTheSamePanelIsNotCountedAgain() {
        var s = FirstRun.opening(l2, 1200f, plain = true)
        for (enter in listOf(Enter.PRACTICE_OPEN, Enter.PRACTICE_SEARCH, Enter.COPY_STAYS)) s = FirstRun.ran(s, enter)
        s = FirstRun.answer(s, Answer.NOT_NOW)
        assertEquals(Screen.C, FirstRun.stage(s, 1200f))
        assertEquals(1, s.opens)
        // The choices left, the run is over: nothing of it is kept but that the key's step has ended here.
        assertEquals(State(key = true, mark = true), FirstRun.answer(s, Answer.DONE))
    }
}
```

- [ ] **Step 2: A lesson now has an answer: take the line that said it had none out of `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunKeyStepTest.kt`.** One change (what a lesson offers is said in the new test).

Find:

```kotlin
        assertEquals(emptyList<Answer>(), FirstRun.answers(State()))
        assertEquals(emptyList<Answer>(), FirstRun.answers(lesson))
```

Make it:

```kotlin
        assertEquals(emptyList<Answer>(), FirstRun.answers(State()))
```

- [ ] **Step 3: Run it and see it fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunStageTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'entered'` and `Unresolved reference 'tabbed'` among its `e:` lines.

- [ ] **Step 4: Implement it in `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`.** One change: the section that began "the key's step, as it stands", from its heading down to the comment that opens `tab`. `answers` gains three lines, `armed` a longer comment, and `tabbed` and `entered` are new.

Find:

```kotlin
    // ---- the key's step, as it stands

    /** The answers the screen that stands offers, in their order; none where nothing of the key's step stands. */
    fun answers(s: State): List<Answer> = when (screen(s)) {
        Screen.K1, Screen.K2, Screen.K3 -> listOf(Answer.OPEN_HELPER, Answer.NOT_NOW)
        Screen.K4 -> if (s.run == Run.REPLAY) listOf(Answer.GO_ON, Answer.CHANGE_KEY) else listOf(Answer.GO_ON)
        else -> emptyList()
    }

    /**
     * Which of them Enter runs when the screen comes: the first. On "Now press your keys" none: the
     * thing to press there is the key, and Enter does nothing until Tab. -1: none.
     */
    fun armed(s: State): Int = when (screen(s)) {
        Screen.K1, Screen.K3, Screen.K4 -> 0
        else -> -1
    }
```

Make it:

```kotlin
    // ---- the stage, as it stands

    /**
     * The answers the screen that stands offers, in their order. A lesson offers Skip; the question,
     * Agree and then Not now. None on the choices, which are a list of their own, and none where
     * nothing stands.
     */
    fun answers(s: State): List<Answer> = when (screen(s)) {
        Screen.K1, Screen.K2, Screen.K3 -> listOf(Answer.OPEN_HELPER, Answer.NOT_NOW)
        Screen.K4 -> if (s.run == Run.REPLAY) listOf(Answer.GO_ON, Answer.CHANGE_KEY) else listOf(Answer.GO_ON)
        Screen.L2, Screen.L3, Screen.L4 -> listOf(Answer.SKIP)
        Screen.Q -> listOf(Answer.AGREE, Answer.NOT_NOW)
        Screen.C, null -> emptyList()
    }

    /**
     * Which of them Enter runs when the screen comes: on the key's screens the first. On "Now press
     * your keys" none: the thing to press there is the key. In a lesson none: the thing to do there
     * is to type, and Enter on the empty field does nothing, as every day. On the question none: an
     * answer there is consent, and Enter gives none until Tab or the pointer has chosen one. -1: none.
     */
    fun armed(s: State): Int = when (screen(s)) {
        Screen.K1, Screen.K3, Screen.K4 -> 0
        else -> -1
    }

    /**
     * Tab on the stage, with [armed] armed: the next answer, with [back] the one before ([tab]).
     * [arrived]: the glass has opened and the stage can be seen. Until then a key is pressed blind,
     * and changes nothing.
     */
    fun tabbed(s: State, armed: Int, back: Boolean, arrived: Boolean): Int = if (arrived) tab(armed, answers(s).size, back) else armed

    /**
     * Enter on the stage: the answer that is armed. Null, and Enter does nothing, where none is
     * armed and before the stage has [arrived]: Tab and Enter pressed while the panel was still
     * opening must not answer a question nobody has read.
     */
    fun entered(s: State, armed: Int, arrived: Boolean): Answer? = if (arrived) answers(s).getOrNull(armed) else null
```

- [ ] **Step 5: Run it and see it pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunStageTest' --tests 'io.github.kuscher.booklight.core.FirstRunKeyStepTest' --console=plain
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
git add core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunStageTest.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunKeyStepTest.kt
git commit -m "First run: every screen's answers, and the stage's keys count only once it is there (core, tested)"
```

### Task 3: `FirstRun`: the lessons as they stand (the coach line, this device's example, the recipe)

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunRecipeTest.kt`

**Interfaces:**
- Consumes (in `object FirstRun`): `State`, `Run`, `Screen`, `Enter`, `MAX_OPENS`, `fun screen(s: State): Screen?`, `fun enters(s: State, line: String?, effect: Effect, searchable: Boolean): Enter`. Core `Effect` (`LaunchApp`, `EnterScope(key, text, act)`, `Open`, `CopyText`, `OpenSettings`, `OpenUrl`, `AppInfo`) and `Act.SEARCH`, in the same package.
- Produces, in `object FirstRun`:
  - `fun lesson(s: State): Screen?` (`L2`, `L3`, `L4`, or null)
  - `enum class Coach { OPENS, TO_SEARCH, INTO_APP, SEARCHES, COPIES, TITLE }`, `fun coach(s: State, enter: Enter, effect: Effect?, search: Boolean): Coach?`
  - `const val LEAST_LETTERS = 3`, `const val MOST_LETTERS = 4`, `const val RECIPE_WORD = 8`
  - `fun starts(name: String): List<String>`, `inline fun letters(name: String, leads: (String) -> Boolean): String?`
  - `data class Example(val open: String, val search: String, val enter: Boolean, val word: String)`, `fun example(app: String?, word: String, settings: String?, keyword: String, page: String): Example`
  - `sealed interface Part { data class Typed(val text: String) : Part; enum class Key : Part { TAB, ENTER } }`, `fun recipe(s: State, example: Example, sum: String): List<Part>`

What a lesson shows and says is decided here. The coach line is asked with what `enters` says Enter would do on the selected row, so it cannot promise what Enter will not do. `letters` is `inline` because the app's answer to "do these letters put the app first?" comes from its engine, which suspends.

- [ ] **Step 1: Write the failing test `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunRecipeTest.kt`**

```kotlin
package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Coach
import io.github.kuscher.booklight.core.FirstRun.Enter
import io.github.kuscher.booklight.core.FirstRun.Example
import io.github.kuscher.booklight.core.FirstRun.Part
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirstRunRecipeTest {
    private val l2 = State(Run.NEW, done = listOf("key"), key = true)
    private val l3 = State(Run.NEW, done = listOf("key", "open"), key = true)
    private val l4 = State(Run.NEW, done = listOf("key", "open", "search"), key = true)
    private val q = State(Run.NEW, done = listOf("key", "open", "search", "sum"), key = true)
    private val noLesson = listOf(State(), State(Run.NEW), State(Run.NEW, key = true), q, l2.copy(opens = FirstRun.MAX_OPENS + 1))

    private val open = Effect.LaunchApp("com.example.videos", "Main")
    private val intoApp = Effect.EnterScope("appsearch:com.example.videos", act = Act.SEARCH)
    private val search = Effect.Open("com.example.videos", "intent:#Intent;action=android.intent.action.SEARCH;end")
    private val copy = Effect.CopyText("180")
    private val page = Effect.OpenSettings("android.settings.WIFI_SETTINGS")
    private val sum = "150 + 20%"

    /** The coach line for what Enter would do on the selected row now: asked the way the panel asks, through [FirstRun.enters]. */
    private fun coach(s: State, line: String?, effect: Effect, searchable: Boolean = true, search: Boolean = true) =
        FirstRun.coach(s, FirstRun.enters(s, line, effect, searchable), effect, search)

    @Test fun theLessonThatStands() {
        assertEquals(listOf(Screen.L2, Screen.L3, Screen.L4), listOf(l2, l3, l4).map { FirstRun.lesson(it) })
        for (s in noLesson) assertNull(FirstRun.lesson(s))
    }

    @Test fun theCoachLineSaysTheNextKeyAndWhatItDoesToday() {
        assertEquals(Coach.OPENS, coach(l2, "apps", open))
        assertEquals(Coach.TO_SEARCH, coach(l3, "apps", open))
        assertEquals(Coach.INTO_APP, coach(l3, "apps", intoApp))
        assertEquals(Coach.SEARCHES, coach(l3, "appsearch", search))
        assertEquals(Coach.COPIES, coach(l4, "sums", copy))
    }

    /** It never promises what Enter will not do: where the selected row is not the lesson's, the lesson's own title stands there. */
    @Test fun whereTheSelectedRowIsNotTheLessonsItsTitleStands() {
        // Lesson 3 on an app that cannot be searched: its row has no Search to move to.
        assertEquals(Coach.TITLE, coach(l3, "apps", open, search = false))
        // A sum while lesson 2 stands, an app while lesson 4 stands, the web, App info.
        assertEquals(Coach.TITLE, coach(l2, "sums", copy))
        assertEquals(Coach.TITLE, coach(l4, "apps", open))
        assertEquals(Coach.TITLE, coach(l2, "web", Effect.OpenUrl("https://example.com")))
        assertEquals(Coach.TITLE, coach(l3, "apps", Effect.AppInfo("com.example.videos", "Main")))
        // Making the app the chip is lesson 3's step, not lesson 2's.
        assertEquals(Coach.TITLE, coach(l2, "apps", intoApp))
        // Nothing is selected.
        assertEquals(Coach.TITLE, FirstRun.coach(l2, Enter.AS_ALWAYS, null, search = false))
    }

    /** On every other day, and on every other screen of first run, the footer's left end is as it was. */
    @Test fun whereNoLessonStandsThereIsNoCoachLine() {
        for (s in noLesson) for (enter in Enter.entries) assertNull(FirstRun.coach(s, enter, open, search = true))
    }

    @Test fun theSettingsPageThatStandsInIsSaidToOpen() {
        assertEquals(Coach.OPENS, coach(l3, "settings", page, searchable = false))
        // Where an app can be searched a settings page is as every day, and nothing is promised of it.
        assertEquals(Coach.TITLE, coach(l3, "settings", page, searchable = true))
    }

    @Test fun anAppsLettersAreTheStartOfItsFirstWordThreeAtLeastFourAtMost() {
        assertEquals(listOf("you", "yout"), FirstRun.starts("YouTube"))
        assertEquals(listOf("pla", "play"), FirstRun.starts(" Play Store "))
        assertEquals(listOf("map"), FirstRun.starts("Map"))
        assertEquals(listOf("tv"), FirstRun.starts("TV"))
        assertEquals(emptyList<String>(), FirstRun.starts("  "))
    }

    /** The letters a recipe shows must put the app's row first: Enter is pressed on row one. */
    @Test fun theLettersShownAreTheShortestThatPutTheAppFirst() {
        assertEquals("you", FirstRun.letters("YouTube") { true })
        assertEquals("yout", FirstRun.letters("YouTube") { it == "yout" })
        // No start of it leads its list: this app is not the example.
        assertNull(FirstRun.letters("YouTube") { false })
    }

    @Test fun lessonThreeBuildsOnLessonTwosApp() {
        val e = FirstRun.example(app = "yout", word = "lofi", settings = null, keyword = "s", page = "wifi")
        assertEquals(Example("yout", "yout", enter = true, word = "lofi"), e)
        assertEquals(listOf(Part.Typed("yout"), Part.Key.ENTER), FirstRun.recipe(l2, e, sum))
        assertEquals(listOf(Part.Typed("yout"), Part.Key.TAB, Part.Key.ENTER, Part.Typed("lofi"), Part.Key.ENTER), FirstRun.recipe(l3, e, sum))
        assertEquals(listOf(Part.Typed(sum), Part.Key.ENTER), FirstRun.recipe(l4, e, sum))
    }

    /** No app of this device can be searched, or none leads its list: Settings stands in, by its keyword, and the page found there is lesson 3's thing. */
    @Test fun whereNoAppCanBeTheExampleSettingsStandsIn() {
        val e = FirstRun.example(app = null, word = "lofi", settings = "sett", keyword = "s", page = "wifi")
        assertEquals(listOf(Part.Typed("sett"), Part.Key.ENTER), FirstRun.recipe(l2, e, sum))
        // The keyword's Tab makes Settings the chip by itself: no Enter follows it.
        assertEquals(listOf(Part.Typed("s"), Part.Key.TAB, Part.Typed("wifi"), Part.Key.ENTER), FirstRun.recipe(l3, e, sum))
        // What the recipe shows is what Enter then practises: asked with what the example says.
        assertEquals(Enter.PRACTICE_SEARCH, FirstRun.enters(l3, "settings", page, searchable = e.enter))
        assertEquals(Enter.AS_ALWAYS, FirstRun.enters(l3, "settings", page, searchable = FirstRun.example("yout", "lofi", null, "s", "wifi").enter))
    }

    @Test fun aRecipeShowsNoMoreThanItHasRoomFor() {
        val long = Example("youtube", "youtube", enter = true, word = "a long word to look for")
        assertEquals(Part.Typed("yout"), FirstRun.recipe(l2, long, sum).first())
        assertEquals(Part.Typed("a long w"), FirstRun.recipe(l3, long, sum)[3])
        // Nothing to name (no app that can be the example, and no Settings app either): the keys alone.
        val none = FirstRun.example(app = null, word = "lofi", settings = null, keyword = "", page = "wifi")
        assertEquals(listOf<Part>(Part.Key.ENTER), FirstRun.recipe(l2, none, sum))
        assertEquals(listOf(Part.Key.TAB, Part.Typed("wifi"), Part.Key.ENTER), FirstRun.recipe(l3, none, sum))
    }

    @Test fun noRecipeWhereNoLessonStands() {
        val e = FirstRun.example("yout", "lofi", null, "s", "wifi")
        for (s in noLesson) assertEquals(emptyList<Part>(), FirstRun.recipe(s, e, sum))
    }
}
```

- [ ] **Step 2: Run it and see it fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunRecipeTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'Coach'` among its `e:` lines.

- [ ] **Step 3: Implement it in `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`.** One change: everything new goes in after `ran`, the last function of the section "the lessons", and before the section "the start".

Find:

```kotlin
    }

    // ---- the start
```

Make it:

```kotlin
    }

    /** The lesson that stands (the second, third or fourth step's screen); null: none does. */
    fun lesson(s: State): Screen? = screen(s)?.takeIf { it == Screen.L2 || it == Screen.L3 || it == Screen.L4 }

    /**
     * What the footer's left end says while a lesson's list is typed: the next key, and what it does
     * today. [OPENS]: Enter opens what is selected, for practice. [TO_SEARCH]: Tab moves to Search.
     * [INTO_APP]: Enter makes the app the chip. [SEARCHES]: Enter searches inside the app, for
     * practice. [COPIES]: Enter copies the answer, and the panel stays. [TITLE]: none of these: the
     * lesson's own title stands there.
     */
    enum class Coach { OPENS, TO_SEARCH, INTO_APP, SEARCHES, COPIES, TITLE }

    /**
     * The coach line for the lesson that stands and the row that is selected; null where no lesson
     * stands. [enter]: what Enter would do on that row now ([enters]). [effect]: the armed action's;
     * null with nothing selected. [search]: the row also offers Search (lesson 3's Tab has somewhere
     * to go). It never promises what Enter will not do: a row that is not the lesson's own gets the
     * lesson's title.
     */
    fun coach(s: State, enter: Enter, effect: Effect?, search: Boolean): Coach? = when (lesson(s)) {
        Screen.L2 -> if (enter == Enter.PRACTICE_OPEN) Coach.OPENS else Coach.TITLE
        Screen.L3 -> when {
            // (Where no app can be searched a settings page stands in, and Enter opens it.)
            enter == Enter.PRACTICE_SEARCH -> if (effect is Effect.OpenSettings) Coach.OPENS else Coach.SEARCHES
            effect is Effect.EnterScope && effect.act == Act.SEARCH -> Coach.INTO_APP
            enter == Enter.PRACTICE_OPEN && search -> Coach.TO_SEARCH
            else -> Coach.TITLE
        }
        Screen.L4 -> if (enter == Enter.COPY_STAYS) Coach.COPIES else Coach.TITLE
        else -> null
    }

    /** How many letters of an app's name a recipe shows as what to type: three at least, four at most (the recipe has room for no more). */
    const val LEAST_LETTERS = 3
    const val MOST_LETTERS = 4
    /** The longest word to look for that a recipe shows; a longer one is cut. */
    const val RECIPE_WORD = 8

    /**
     * The starts of an app's [name] that a recipe may show, the shortest first: of its first word (a
     * space would end the letters), in small letters. A name of three letters or fewer is typed whole.
     */
    fun starts(name: String): List<String> {
        val word = name.trim().lowercase().substringBefore(' ')
        return when {
            word.isEmpty() -> emptyList()
            word.length <= LEAST_LETTERS -> listOf(word)
            else -> (LEAST_LETTERS..minOf(MOST_LETTERS, word.length)).map { word.take(it) }
        }
    }

    /**
     * The letters to show for the app called [name]: its shortest start that puts its row first
     * ([leads]: typed, these letters have that app's row as row one). Null: none does, and this app
     * cannot be the example: Enter is pressed on row one.
     */
    inline fun letters(name: String, leads: (String) -> Boolean): String? = starts(name).firstOrNull(leads)

    /**
     * What the recipes of lessons 2 and 3 show, on this device. [open]: the letters lesson 2 types;
     * empty where there is no app to name. [search]: what lesson 3 types before Tab. [enter]: Enter
     * follows that Tab: on an app's row Tab moves to Search and Enter makes the app the chip; false
     * where the Settings keyword stands in, which Tab alone makes the chip. [word]: what is then
     * looked for.
     */
    data class Example(val open: String, val search: String, val enter: Boolean, val word: String)

    /**
     * The example for this device. [app]: the letters of an app that can be searched ([letters]);
     * lesson 3 then builds on lesson 2's app and looks for [word]. Null where no app here can be
     * searched, or none leads its list: lesson 2 then names the Settings app ([settings]: its
     * letters, null where it has none), and lesson 3 goes by the Settings [keyword] to the page [page].
     */
    fun example(app: String?, word: String, settings: String?, keyword: String, page: String): Example =
        if (app != null) Example(app, app, enter = true, word = word) else Example(settings.orEmpty(), keyword, enter = false, word = page)

    /** One piece of a lesson's recipe: letters to type, shown as they will stand in the field, or a key to press. */
    sealed interface Part {
        data class Typed(val text: String) : Part
        enum class Key : Part { TAB, ENTER }
    }

    /**
     * The recipe of the lesson that stands, left to right: what to type and which keys to press.
     * [sum]: what lesson 4 types. Empty where no lesson stands. Letters are cut to what the band
     * has room for, and where there is nothing to type that piece is left out.
     */
    fun recipe(s: State, example: Example, sum: String): List<Part> {
        fun typed(text: String, most: Int): Part? = text.take(most).takeIf { it.isNotEmpty() }?.let { Part.Typed(it) }
        return when (lesson(s)) {
            Screen.L2 -> listOfNotNull(typed(example.open, MOST_LETTERS), Part.Key.ENTER)
            Screen.L3 -> listOfNotNull(typed(example.search, MOST_LETTERS), Part.Key.TAB, Part.Key.ENTER.takeIf { example.enter }, typed(example.word, RECIPE_WORD), Part.Key.ENTER)
            Screen.L4 -> listOf(Part.Typed(sum), Part.Key.ENTER)
            else -> emptyList()
        }
    }

    // ---- the start
```

- [ ] **Step 4: Run it and see it pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunRecipeTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run every core test**

```bash
./bl test
```

Expected: no output, exit status 0.

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunRecipeTest.kt
git commit -m "First run: the lessons as they stand: the coach line, this device's example, the recipe (core, tested)"
```

### Task 4: The words of the lessons, the question, the choices and the ending

**Files:**
- Modify: `app/src/main/res/values/strings_32.xml`
- Modify: `app/src/main/res/values-de/strings_32.xml`

**Interfaces:**
- Consumes: both files as part 2 left them (each ends with `a11y_first_goes_on` and `</resources>`). Strings that exist and are used again by later tasks, not added here: `card_not_now`, `card_done`, `set_usual_show`, `help_title`, `help_about`, `action_open`, `guide_link_text` ("lofi"), `first_step`, `copied`.
- Produces (English and German unless said): the lessons: `first_open_title`, `first_open_text`, `first_open_hint`, `first_search_title`, `first_search_text`, `first_search_hint` (`%1$s`: the letters), `first_sum_title`, `first_sum_text`, `first_practice`, `first_enter_stays`, `first_skip`, `first_practice_open` and `first_practice_search` (`%1$s`: the app), `first_coach_open`, `first_coach_tab`, `first_coach_into`, `first_coach_search`, `first_coach_sum`; not translated: `first_sum_example` (`150 + 20%`), `first_search_page` (`wifi`), `first_key_tab`, `first_key_enter`. The question: `first_suggest_title`, `first_suggest_text` (`%1$s`: the search engine, twice), `first_suggest_note`, `first_agree`. The choices and the ending: `first_usual_text`, `first_off`, `first_on`, `first_labs`, `first_end_hint`. For a screen reader: `a11y_first_example` (`%1$s`: the recipe), `a11y_first_skips`, `a11y_first_asks`, `a11y_first_choices`, `a11y_first_done`. Two German strings of part 2 are reworded: `first_helper_3`, `a11y_first_keys`.

The words are `design.md` §6 and §7's and `pm.md` §8's, with "Settled for the build", point 6: the caption under a lesson's recipe is "Practice: nothing opens. Enter also puts Booklight away.", and the coach line's second half in lessons 2 and 3 is "practice: nothing opens" („zum Üben: nichts öffnet sich“). Nothing uses them yet. `first_sum_example` holds a per cent sign and takes no arguments: hence `formatted="false"`.

- [ ] **Step 1: The English words, in `app/src/main/res/values/strings_32.xml`.** One change: they go in before the file's last line.

Find:

```xml
</resources>
```

Make it:

```xml

    <!-- First run · the three lessons (design.md §6): the same skeleton, with a recipe where the key caps stood. Nothing opens in first run: -->
    <!-- Enter on an app's row and on a search inside an app is practice. -->
    <string name="first_open_title">Open an app</string>
    <string name="first_open_text">Type its first letters, then Enter.</string>
    <!-- The field's placeholder while the lesson stands. -->
    <string name="first_open_hint">An app’s first letters</string>
    <string name="first_search_title">Search inside an app</string>
    <string name="first_search_text">Tab moves along the row, Enter does. Then type what to look for.</string>
    <!-- %1$s is what to type first: the letters of an app of this device. -->
    <string name="first_search_hint">%1$s, then Tab</string>
    <string name="first_sum_title">A sum</string>
    <string name="first_sum_text">The answer stands there as you type. Enter copies it.</string>
    <!-- What lesson 4 shows as what to type, and its placeholder. A sum is no word. -->
    <string name="first_sum_example" translatable="false" formatted="false">150 + 20%</string>
    <!-- What lesson 3 looks for where no app of the device can be searched: a page of Settings, found by this word in every language. -->
    <string name="first_search_page" translatable="false">wifi</string>
    <!-- Two keys as a recipe's caps name them, and as a screen reader says them. -->
    <string name="first_key_tab" translatable="false">Tab</string>
    <string name="first_key_enter" translatable="false">Enter</string>
    <!-- Under the recipe: what other days are like. -->
    <string name="first_practice">Practice: nothing opens. Enter also puts Booklight away.</string>
    <string name="first_enter_stays">Enter also puts Booklight away. Today it stays.</string>
    <string name="first_skip">Skip</string>
    <!-- In the footer, with its check, after Enter in a lesson. %1$s is the app, or the settings page. -->
    <string name="first_practice_open">That opens %1$s</string>
    <string name="first_practice_search">That searches %1$s</string>
    <!-- The coach line, at the footer's left end while a lesson's list is typed: after the counter and a key cap (Enter or tab), what that key does today. -->
    <string name="first_coach_open">opens it · practice: nothing opens</string>
    <string name="first_coach_tab">moves to Search</string>
    <string name="first_coach_into">goes into the app</string>
    <string name="first_coach_search">searches there · practice: nothing opens</string>
    <string name="first_coach_sum">copies the answer · Booklight stays today only</string>

    <!-- First run · the question (design.md §7, pm.md §8): alone on its screen, nothing armed. %1$s is the search engine. -->
    <string name="first_suggest_title">Suggest searches as you type?</string>
    <string name="first_suggest_text">Booklight sends what you type to %1$s while you type, to suggest searches. %1$s also sees your IP address. Not sent: sums, web addresses, and anything after a keyword or with an app in the field.</string>
    <string name="first_suggest_note">Off until you agree. Change it in Booklight’s window › Results.</string>
    <string name="first_agree">Agree</string>

    <!-- First run · the choices (design.md §7): a list of two rows, and the placeholder the ending leaves behind. -->
    <string name="first_usual_text">With nothing typed, the two things you run most stand under the field. Nothing changes until you have run two things twice each.</string>
    <string name="first_off">Off</string>
    <string name="first_on">On</string>
    <string name="first_labs">Play by name and flight times need your own key: window › Labs</string>
    <string name="first_end_hint">Type ? for everything Booklight does</string>

    <!-- For a screen reader. %1$s is a lesson's recipe, a piece at a time. -->
    <string name="a11y_first_example">For example: %1$s</string>
    <string name="a11y_first_skips">Tab, then Enter skips.</string>
    <string name="a11y_first_asks">Nothing is chosen. Tab for Agree or Not now.</string>
    <string name="a11y_first_choices">Down arrow to choose. Enter when done.</string>
    <string name="a11y_first_done">First steps are done. Type question mark for everything.</string>
</resources>
```

- [ ] **Step 2: The German words, in `app/src/main/res/values-de/strings_32.xml`.** Three changes. The first two settle the wording part 2 left open: the key is „Aktion“ wherever first run names it, as the system names it, and a screen reader hears „Vorgeschlagene Tasten“ before the two keys. The third adds this part's words before the file's last line.

Find:

```xml
    <string name="first_helper_3">3. Aktionstaste halten und %1$s drücken, oder Tasten deiner Wahl</string>
```

Make it:

```xml
    <string name="first_helper_3">3. Aktion halten und %1$s drücken, oder Tasten deiner Wahl</string>
```

Find:

```xml
    <string name="a11y_first_keys">Vorschlag: %1$s und %2$s</string>
```

Make it:

```xml
    <string name="a11y_first_keys">Vorgeschlagene Tasten: %1$s und %2$s</string>
```

Find:

```xml
</resources>
```

Make it:

```xml

    <string name="first_open_title">Eine App öffnen</string>
    <string name="first_open_text">Die ersten Buchstaben tippen, dann Enter.</string>
    <string name="first_open_hint">Die ersten Buchstaben einer App</string>
    <string name="first_search_title">In einer App suchen</string>
    <string name="first_search_text">Tab geht die Zeile entlang, Enter führt aus. Dann tippen, was du suchst.</string>
    <string name="first_search_hint">%1$s, dann Tab</string>
    <string name="first_sum_title">Eine Rechnung</string>
    <string name="first_sum_text">Das Ergebnis steht da, während du tippst. Enter kopiert es.</string>
    <string name="first_practice">Zum Üben: nichts öffnet sich. Sonst schließt Enter auch Booklight.</string>
    <string name="first_enter_stays">Sonst schließt Enter auch Booklight. Heute bleibt es.</string>
    <string name="first_skip">Überspringen</string>
    <string name="first_practice_open">Das öffnet %1$s</string>
    <string name="first_practice_search">Das durchsucht %1$s</string>
    <string name="first_coach_open">öffnet sie · zum Üben: nichts öffnet sich</string>
    <string name="first_coach_tab">geht zu „Suchen“</string>
    <string name="first_coach_into">geht in die App</string>
    <string name="first_coach_search">sucht dort · zum Üben: nichts öffnet sich</string>
    <string name="first_coach_sum">kopiert das Ergebnis · Booklight bleibt nur heute</string>

    <string name="first_suggest_title">Beim Tippen Suchen vorschlagen?</string>
    <string name="first_suggest_text">Booklight sendet, was du tippst, schon beim Tippen an %1$s, um Suchen vorzuschlagen. %1$s sieht dabei auch deine IP-Adresse. Nicht gesendet werden Berechnungen, Webadressen und alles nach einem Stichwort oder mit einer App im Feld.</string>
    <string name="first_suggest_note">Aus, bis du zustimmst. Änderbar in Booklights Fenster › Ergebnisse.</string>
    <string name="first_agree">Zustimmen</string>

    <string name="first_usual_text">Ohne Eingabe stehen deine zwei meistgenutzten Dinge unter dem Feld. Nichts ändert sich, bis zwei Dinge je zweimal liefen.</string>
    <string name="first_off">Aus</string>
    <string name="first_on">An</string>
    <string name="first_labs">Musik nach Namen, Flugzeiten: eigener Schlüssel, im Fenster › Labs</string>
    <string name="first_end_hint">Tippe ? für alles, was Booklight kann</string>

    <string name="a11y_first_example">Zum Beispiel: %1$s</string>
    <string name="a11y_first_skips">Tab, dann Enter überspringt.</string>
    <string name="a11y_first_asks">Nichts ist gewählt. Tab für „Zustimmen“ oder „Nicht jetzt“.</string>
    <string name="a11y_first_choices">Pfeil nach unten zum Auswählen. Enter zum Beenden.</string>
    <string name="a11y_first_done">Die ersten Schritte sind fertig. Fragezeichen tippen für alles.</string>
</resources>
```

- [ ] **Step 3: Every new name stands in both files, but for the ones that are not translated**

```bash
for f in app/src/main/res/values/strings_32.xml app/src/main/res/values-de/strings_32.xml; do grep -c "<string name=" "$f"; done
grep -o 'name="[a-z0-9_]*"' app/src/main/res/values/strings_32.xml | sort > /tmp/booklight-en.txt
grep -o 'name="[a-z0-9_]*"' app/src/main/res/values-de/strings_32.xml | sort > /tmp/booklight-de.txt
comm -23 /tmp/booklight-en.txt /tmp/booklight-de.txt
```

Expected: `64`, then `58`; then the six names English has alone, each marked `translatable="false"` there (four are this task's, two are part 2's): `first_key_enter`, `first_key_letter`, `first_key_quick_cap`, `first_key_tab`, `first_search_page`, `first_sum_example`.

- [ ] **Step 4: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings_32.xml app/src/main/res/values-de/strings_32.xml
git commit -m "First run: the words of the lessons, the question, the choices and the ending, English and German"
```

### Task 5: The model says what is due, which lesson stands, and what this device's example is

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`

**Interfaces:**
- Consumes: core (Task 3) `FirstRun.Example`, `FirstRun.example(app: String?, word: String, settings: String?, keyword: String, page: String): Example`, `inline fun FirstRun.letters(name: String, leads: (String) -> Boolean): String?`; core `FirstRun.stage(s: State, screenDp: Float): Screen?`, `Query(raw)`, `Kind.APP`, `Effect.EnterScope(key, text, act)`, `Act.SEARCH`. In `BooklightApp`: `apps.ready` and `commands.ready` (each a `CompletableDeferred<Unit>`), `engine.search(q: Query): List<Result>` (suspends), `engine.scope(key: String): Scope?`, `guide.appSearch(): String?` (the name, in small letters, of an app of this device that can be searched; null: none), `apps.installed(pkg: String): AppsProvider.Installed?` (with `label`). Strings: `guide_link_text`, `first_search_page` (Task 4). In `OverlayModel` (part 2): `val stage: FirstRun.Screen? by derivedStateOf { … }`, `settings`, `guided`, `demo`, `screenDp`, `scope`, `resuggest()`, `firstChanged()`.
- Produces:
  - `suspend fun BooklightApp.firstExample(): FirstRun.Example`
  - In `OverlayModel`: `val due: FirstRun.Screen?` (the screen that is due in this panel whatever the field holds; until Task 12 only a screen of the key's step), `val stage: FirstRun.Screen?` (the screen whose stage has the place under the empty field: `due`, but for the choices, while nothing is typed and no chip stands), `val lesson: FirstRun.Screen?` (`due` where it is `L2`, `L3` or `L4`), `var example: FirstRun.Example?` (null until worked out; its setter is the model's own).

Until now `stage` meant two things at once: a screen of first run is due, and its stage has the place under the empty field. A lesson needs them apart: it goes on standing while its list is typed, when `stage` is null. Every asker of `stage != null` today (`offerTip`, `offerCopy`, `offerZero`, `bare`, the panel's keys, the debug hooks) means "the stage has the place", and keeps asking `stage`. The key's limit moves from `stage` to `due` and stays until Task 12, so nothing a user sees changes in this task.

The example is this device's and is worked out only where a screen is due: on every other day nothing here runs.

There is no unit test for this task: the app module has no test source set (`app/src` holds `main` and `debug`). What it decides is core's and is tested there; its check here is that it builds.

- [ ] **Step 1: The example, in `app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt`.** Three changes: the imports, `firstExample` above the list of providers, and the Settings app's package in the companion.

Find:

```kotlin
import io.github.kuscher.booklight.core.Zero
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.Act
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Zero
```

Find:

```kotlin
    }

    /** Every source of results. A new ability is one more line here. */
```

Make it:

```kotlin
    }

    /**
     * First run's example for this device (core `FirstRun.example`): what the recipes of lessons 2 and 3 show to type. An
     * app that can be searched, by the shortest start of its name that puts its row first, so that lesson 3 builds on
     * lesson 2's app; where there is none, the Settings app and the Settings keyword. Worked out off the main thread, once
     * the app list and other apps' commands have been read, by asking the engine what those letters find: nothing is run,
     * sent or learned by it. Not kept: the same apps give the same example.
     */
    suspend fun firstExample(): FirstRun.Example = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        apps.ready.await(); commands.ready.await()
        // Row one for these letters is the app called [name]; with [search], its row also offers Search (lesson 3's Tab goes there).
        suspend fun leads(letters: String, name: String, search: Boolean): Boolean {
            val first = engine.search(Query(letters)).firstOrNull() ?: return false
            return first.kind == Kind.APP && first.title.equals(name, ignoreCase = true) && (!search || first.actions.any { (it.effect as? Effect.EnterScope)?.act == Act.SEARCH })
        }
        val app = guide.appSearch()?.let { name -> FirstRun.letters(name) { leads(it, name, search = true) } }
        val settings = if (app != null) null else apps.installed(SETTINGS)?.label?.let { name -> FirstRun.letters(name) { leads(it, name, search = false) } }
        FirstRun.example(app, getString(R.string.guide_link_text), settings, engine.scope("settings")?.keywords?.firstOrNull().orEmpty(), getString(R.string.first_search_page))
    }

    /** Every source of results. A new ability is one more line here. */
```

Find:

```kotlin
        const val TAG = "Booklight"
```

Make it:

```kotlin
        const val TAG = "Booklight"
        /** The Settings app, which every Googlebook has: first run's example where no app can be searched. (It is named in the manifest's `<queries>` already, for its pages.) */
        private const val SETTINGS = "com.android.settings"
```

- [ ] **Step 2: What is due, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Three changes: `stage` becomes `due`, `stage` and `lesson`; the example and its job go in above the `init` that asks the keyboards, which now also asks for the example; and `firstChanged` asks for it where a debug hook has made a screen due.

Find:

```kotlin
     * The screen of first run that stands under the empty field (core `FirstRun.stage`); null: none. Like a tip, only
     * while nothing is typed. Until the lessons, the question and the choices are drawn, only the key's step is: a run that is
     * past it shows nothing.
     */
    val stage: FirstRun.Screen? by derivedStateOf {
        if (demo || guided || query.isNotEmpty() || chip != null) null
        else FirstRun.stage(settings.firstRun(), screenDp)?.takeIf { it.step == FirstRun.Step.KEY }
    }
```

Make it:

```kotlin
     * The screen of first run that is due in this panel (core `FirstRun.stage`), whatever the field holds; null: none.
     * Until the lessons, the question and the choices have their place too, only the key's step is due: a run that is past
     * it shows nothing.
     */
    val due: FirstRun.Screen? by derivedStateOf {
        if (demo || guided) null else FirstRun.stage(settings.firstRun(), screenDp)?.takeIf { it.step == FirstRun.Step.KEY }
    }
    /**
     * The screen whose stage has the place under the empty field; null: none. Like a tip, only while nothing is typed and
     * no chip stands. The choices are no stage: they are a list of their own.
     */
    val stage: FirstRun.Screen? by derivedStateOf { due?.takeIf { it != FirstRun.Screen.C && query.isEmpty() && chip == null } }
    /**
     * The lesson that stands in this panel (the second, third or fourth step's screen): first its stage under the empty
     * field, then the list that is typed for it. It goes on standing while that list is on screen, though its stage does not.
     */
    val lesson: FirstRun.Screen? by derivedStateOf { due?.takeIf { it.step == FirstRun.Step.OPEN || it.step == FirstRun.Step.SEARCH || it.step == FirstRun.Step.SUM } }
```

Find:

```kotlin
    // (The keyboards are asked only where a stage is due: on every other day nothing here runs.)
    init {
        if (!demo && stage != null) resuggest()
```

Make it:

```kotlin
    /**
     * What the recipes of lessons 2 and 3 show to type on this device (core `FirstRun.Example`); null until it is worked
     * out. That is asked only where a screen of first run is due.
     */
    var example by mutableStateOf<FirstRun.Example?>(null); private set
    private var exampling: Job? = null

    /** The example is worked out, once for this panel, off the main thread (`BooklightApp.firstExample`). */
    private fun exemplify() {
        if (example == null && exampling == null) exampling = scope.launch { example = app.firstExample() }
    }

    // (The keyboards are asked, and the example worked out, only where a screen is due: on every other day nothing here runs.)
    init {
        if (!demo && stage != null) resuggest()
        if (!demo && due != null) exemplify()
```

Find:

```kotlin
        if (stage != null) resuggest()
```

Make it:

```kotlin
        if (stage != null) resuggest()
        if (due != null) exemplify()
```

- [ ] **Step 3: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/BooklightApp.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
git commit -m "First run: the model says what is due whatever the field holds, which lesson stands, and what this device's example is"
```

### Task 6: The stage's keys count once the panel has opened

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`

**Interfaces:**
- Consumes: core (Task 2) `FirstRun.tabbed(s: State, armed: Int, back: Boolean, arrived: Boolean): Int`, `FirstRun.entered(s: State, armed: Int, arrived: Boolean): Answer?`, `FirstRun.answers(s)`. In `OverlayModel`: `var arrived: Boolean` (set by the panel once the glass is 85 % open, `Motion.GATE`; at once where the opening is switched off), `stage`, `stageArmed`, `private fun first(): FirstRun.State`.
- Produces: `OverlayModel.stageTab(back: Boolean)`, `stageArm(index: Int)` and `stageEnter(): FirstRun.Answer?` with the signatures they had; before `arrived` the first two change nothing and the third returns null.

Part 2 left this known: "the stage's keys act from the panel's first moment, before the glass has opened". On K1 a blind Enter asked for the system's dialog; on the question a blind Tab and Enter would be consent. From this task on a key counts once the stage can be seen, on every screen (decision 2 in this plan's head). The panel's keys and the debug hooks call these three functions and need no change.

There is no unit test for this task: the app module has no test source set. The rule is core's (`FirstRunStageTest.keysPressedBeforeTheStageIsThereDoNothing`); its check here is that it builds, and Task 13's check 14 on a device.

- [ ] **Step 1: The three functions, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** One change.

Find:

```kotlin
    /** Tab on the stage: the next answer is armed, with [back] the one before; the first press arms the first. */
    fun stageTab(back: Boolean) { if (stage != null) stageArmed = FirstRun.tab(stageArmed, FirstRun.answers(first()).size, back) }
    fun stageArm(index: Int) { if (stage != null && index in FirstRun.answers(first()).indices) stageArmed = index }
    /** Enter on the stage: the armed answer; null where none is armed (Enter then does nothing, and is not kept). */
    fun stageEnter(): FirstRun.Answer? = if (stage == null) null else FirstRun.answers(first()).getOrNull(stageArmed)
```

Make it:

```kotlin
    // The stage's keys count only once the glass has opened and the stage can be seen ([arrived]; core `FirstRun.tabbed` and
    // `entered`): Tab and Enter pressed while the panel was still opening must not answer a question nobody has read.
    /** Tab on the stage: the next answer is armed, with [back] the one before; the first press arms the first. */
    fun stageTab(back: Boolean) { if (stage != null) stageArmed = FirstRun.tabbed(first(), stageArmed, back, arrived) }
    /** The pointer is on an answer: it is armed. */
    fun stageArm(index: Int) { if (stage != null && arrived && index in FirstRun.answers(first()).indices) stageArmed = index }
    /** Enter on the stage: the armed answer; null where none is armed (Enter then does nothing, and is not kept). */
    fun stageEnter(): FirstRun.Answer? = if (stage == null) null else FirstRun.entered(first(), stageArmed, arrived)
```

- [ ] **Step 2: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt
git commit -m "First run: the stage's keys count once the panel has opened, not before"
```

### Task 7: The stage draws the lessons and the question

**Files:**
- Modify (written anew): `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`

**Interfaces:**
- Consumes: core `FirstRun.answers(s)` (Task 2), `FirstRun.count(s): Pair<Int, Int>?`, `FirstRun.caption(s, suggested)`, `FirstRun.Part`, `FirstRun.recipe(s, example, sum)`, `FirstRun.example(…)` (Task 3). App: `OverlayModel.stage`, `due`, `example` (Task 5), `stageArmed`, `stageArm(index)`, `suggested`, `settings` (`settings.firstRun()`, `settings.engine().name`); `SystemWords.words(context)`, `SystemWords.name(context, key)`; from the panel `OptionStrip(options, chosen, onChoose, onRun, vertical = true, lit = …)`, `Keycap("tab")`, `DrawnCheck(shown, color, modifier, stroke)`, `Modifier.fadeEnd()`, `SMALL` (14 sp / 500), `SECOND`, `LocalDark`, `LocalMotion` (`roll()`, `fade(ms, delay)`), `Symbols.of(name)`, `Symbols.enter`, `Metrics.row`, `Metrics.pad`, `Metrics.stage` (184 dp). Task 4's strings.
- Produces:
  - `FirstStage(model: OverlayModel, on: FirstRun.Screen, onAnswer: (FirstRun.Answer) -> Unit)`: the same signature, now for the key's screens, the three lessons and the question.
  - `AskRoom(model: OverlayModel, width: Dp)`: draws nothing; measures the question's text and tells the model.
  - `Metrics.ask` (256 dp), `fun Metrics.stage(on: FirstRun.Screen?, more: Dp): Dp`; `Metrics.height(m)` asks it.
  - In `OverlayModel`: `var askMore: Dp` (its setter is the model's own), `fun askRoom(more: Dp)`.
  - In `FirstStage.kt`, private to the file: the type `CAPTION`, which Task 9's coach line uses again; the edges `LEFT` (72 dp) and `RIGHT` (20 dp); the composables `Seat`, `Band`, `KeyCaps`, `Recipe`, `AskBand`, `BigCap`.

`FirstStage.kt` is written anew: its whole new text is below, and nothing of the old file stays that is not in it. The key's four screens are drawn as part 2 drew them, with the same measures; what is new is the split the next screens need: the words of a screen (`Said`), the seat, the band for keys or a recipe, and the question's own band. The system's words and the suggested key are read only where the key's step stands.

The measures are `design.md`'s. A lesson (§6) is the key's skeleton, 252 dp with the field: in the band its recipe from x = 72 on the caps' centre line (letters in the field's type, keys as large caps, Enter as the drawn mark, 12 dp between two caps and 16 between a cap and letters), its caption on y = 230, and "Skip" at the right end, not lit, with a `tab` cap before it. The question (§7) is 324 dp: the title alone in the seat; the text from y = 140 on four lines of 20, x = 72 to 700, in strong ink; the note and the two answers in the 68 dp from y = 240. Where the text needs more than four lines, the stage is that much taller (decision 10 in this plan's head).

Nothing shows the new screens yet: until Task 12, `stage` is only ever a screen of the key's step.

There is no unit test for this task: the app module has no test source set. What it decides is core's and is tested there; its check here is that it builds, and what it draws is seen in Task 13's device checks 1, 2, 12 and 15.

- [ ] **Step 1: The heights, in `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt`.** Four changes: an import, the question's height beside the stage's, `stage(on, more)` above `height`, and `height` asking it.

Find:

```kotlin
import io.github.kuscher.booklight.core.Kind
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.core.Kind
```

Find:

```kotlin
    /** First run's stage under the field: 8 of air, a row's seat (56), a band of two rows for the keys (112), 8 of air (docs/design/first-run/design.md §4). With the field the panel is 252 dp. */
    val stage = 184.dp
```

Make it:

```kotlin
    /** First run's stage under the field, for the key's step and for a lesson: 8 of air, a row's seat (56), a band of two rows for the keys (112), 8 of air (docs/design/first-run/design.md §4). With the field the panel is 252 dp. */
    val stage = 184.dp
    /** The question's stage: 8 of air, the seat (56), then 8, its text on four lines of 20, 20, its two answers (68), 8; and 8 of air (design.md §7). With the field the panel is 324 dp. */
    val ask = 256.dp
```

Find:

```kotlin
    fun listHeight(rows: List<Result>): Dp = rows.fold(0.dp) { h, r -> h + gap(r) + rowHeight(r) }

    /** The panel's height for what the model is showing. Must match what [Panel] draws. */
```

Make it:

```kotlin
    fun listHeight(rows: List<Result>): Dp = rows.fold(0.dp) { h, r -> h + gap(r) + rowHeight(r) }

    /**
     * How tall first run's stage is for the screen [on]: one fixed height for each. [more]: what the question's text needs
     * beyond its four lines (a long name of a search engine, a large type size): its disclosure is never cut.
     */
    fun stage(on: FirstRun.Screen?, more: Dp): Dp = if (on == FirstRun.Screen.Q) ask + more else stage

    /** The panel's height for what the model is showing. Must match what [Panel] draws. */
```

Find:

```kotlin
        m.stage != null -> stage
```

Make it:

```kotlin
        m.stage != null -> stage(m.stage, m.askMore)
```

- [ ] **Step 2: The room the question's text needs, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Two changes: two imports, and `askMore` above the `init` that asks the keyboards.

Find:

```kotlin
import io.github.kuscher.booklight.R
```

Make it:

```kotlin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.R
```

Find:

```kotlin
    }

    // (The keyboards are asked, and the example worked out, only where a screen is due: on every other day nothing here runs.)
```

Make it:

```kotlin
    }

    /** What the question's text needs beyond its four lines; the stage is that much taller (`Metrics.stage`). Measured by the panel before the question stands. */
    var askMore by mutableStateOf(0.dp); private set
    fun askRoom(more: Dp) { if (more != askMore) askMore = more }

    // (The keyboards are asked, and the example worked out, only where a screen is due: on every other day nothing here runs.)
```

- [ ] **Step 3: Write `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt` anew**

```kotlin
package io.github.kuscher.booklight.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.data.firstRun
import io.github.kuscher.booklight.device.SystemWords
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols

/** A large cap's label, and a typed part of a recipe: the field's own type. */
private val CAP = TextStyle(fontFamily = Fonts.text, fontSize = 24.sp, fontWeight = FontWeight(500))
/** The line under the caps: the footer's type. */
private val CAPTION = TextStyle(fontFamily = Fonts.text, fontSize = 13.sp, fontWeight = FontWeight(500), letterSpacing = 0.1.sp)
private val CAP_SHAPE = RoundedCornerShape(16.dp)
/** Inside each end of a large cap. */
private val CAP_PAD = 20.dp
/** "Your key": two rows' heights wide, whatever was suggested. */
private val YOUR_KEY = 112.dp
/** The band under the seat: the caps on its centre line, the caption on the line a footer's words stand on. */
private val BAND = 112.dp
private val CAPTION_BAND = 28.dp
/** The question's disclosure: a row's small type on lines of 20, in strong ink. */
private val ASK = SMALL.copy(lineHeight = 20.sp)
/** The room the disclosure has: four lines. Where it needs more, the stage is that much taller ([AskRoom]). */
private val ASK_TEXT = 80.dp
/** The two answers under it, 32 dp each and 4 apart; the note beside them ends well before them. */
private val ASK_ANSWERS = 68.dp
private val ASK_NOTE = 442.dp
/** Where a stage's words begin, and where its answers end: the titles' edge and the panel's right margin. */
private val LEFT = 72.dp
private val RIGHT = 20.dp

/** What a screen says: in its seat, under its band, and to a screen reader. */
private class Said(
    val symbol: String, val title: String, val line: String, val caption: String,
    /** For a screen reader: the keys or the recipe as words, null for none; and how to act. */
    val keys: String?, val act: String,
)

/**
 * First run's stage under the empty field (docs/design/first-run/design.md §4 to §7): a seat like a row's (a mark, a
 * title and a line, the counter where a row's kind stands), and under it a band. For the key's step the band holds the
 * keys as large caps; for a lesson, its recipe: what to type and which keys to press; in both, one line of caption and
 * the answers at the right end. The question's band is its disclosure, whole, with a note and two answers under it. One
 * skeleton for every screen: the words and the band change where they stand. It is not a row: nothing of it can be
 * selected, and the field above it is live.
 */
@Composable
fun FirstStage(model: OverlayModel, on: FirstRun.Screen, onAnswer: (FirstRun.Answer) -> Unit) {
    val state = model.settings.firstRun()
    val answers = FirstRun.answers(state)
    val labels = answers.map {
        stringResource(when (it) {
            FirstRun.Answer.OPEN_HELPER -> R.string.card_shortcut_action
            FirstRun.Answer.GO_ON -> R.string.first_next
            FirstRun.Answer.CHANGE_KEY -> R.string.first_key_change
            FirstRun.Answer.SKIP -> R.string.first_skip
            FirstRun.Answer.AGREE -> R.string.first_agree
            else -> R.string.card_not_now
        })
    }
    val count = FirstRun.count(state)
    val counter = count?.let { stringResource(R.string.first_step, it.first, it.second) }
    // A lesson's recipe is this device's: until its example is worked out, lessons 2 and 3 show none.
    val example = model.example
    val parts = if (example == null && on != FirstRun.Screen.L4) emptyList()
        else FirstRun.recipe(state, example ?: FirstRun.example(null, "", null, "", ""), stringResource(R.string.first_sum_example))
    val said = when (on.step) {
        FirstRun.Step.KEY -> keySaid(model, on, state)
        FirstRun.Step.ASK -> askSaid()
        else -> lessonSaid(on, parts)
    }
    val text = if (on == FirstRun.Screen.Q) stringResource(R.string.first_suggest_text, model.settings.engine().name) else ""
    val note = if (on == FirstRun.Screen.Q) stringResource(R.string.first_suggest_note) else ""

    // A screen reader is told each screen once, when it comes: where it is in the run, what it says, the keys, and how to act.
    val view = LocalView.current
    val told = listOfNotNull(counter, said.title, said.line.ifEmpty { null }, text.ifEmpty { null }, note.ifEmpty { null }, said.keys, said.caption.ifEmpty { null }, said.act)
        .joinToString(". ") { it.trimEnd('.') } + "."
    LaunchedEffect(on) { view.announceForAccessibility(told) }

    val armed = model.stageArmed
    val strip: @Composable () -> Unit = {
        OptionStrip(labels, armed.coerceIn(0, (labels.size - 1).coerceAtLeast(0)), onChoose = { model.stageArm(it) },
            onRun = { model.stageArm(it); answers.getOrNull(it)?.let(onAnswer) }, vertical = true, lit = armed >= 0)
    }
    Column(Modifier.fillMaxWidth().height(Metrics.stage(on, model.askMore)).padding(vertical = Metrics.pad)) {
        Seat(said.symbol, said.title, said.line, counter)
        if (on.step == FirstRun.Step.ASK) AskBand(text, note, model.askMore, armed, strip)
        else Band(armed, said.caption, strip) {
            if (on.step == FirstRun.Step.KEY) KeyCaps(model, on) else Recipe(parts, Modifier.align(Alignment.CenterStart).clearAndSetSemantics { })
        }
    }
}

/** The key's step: four screens. Only here are the system's own words and the suggested key read. */
@Composable
private fun keySaid(model: OverlayModel, on: FirstRun.Screen, state: FirstRun.State): Said {
    val context = LocalContext.current
    val words = SystemWords.words(context)
    val key = model.suggested
    val name = SystemWords.name(context, key)
    val title = stringResource(when (on) {
        FirstRun.Screen.K2 -> R.string.first_key_press
        FirstRun.Screen.K3 -> R.string.first_key_retry
        FirstRun.Screen.K4 -> R.string.key_works
        else -> R.string.card_shortcut_title
    })
    val line = when (on) {
        FirstRun.Screen.K2 -> stringResource(R.string.first_key_press_text)
        FirstRun.Screen.K3 -> stringResource(R.string.first_key_retry_text, words.customize)
        FirstRun.Screen.K4 -> stringResource(R.string.first_key_works_text)
        else -> stringResource(R.string.first_key_text)
    }
    val caption = when (FirstRun.caption(state, key)) {
        FirstRun.Caption.WHERE_AND_ANY -> stringResource(R.string.first_key_where, name) + " · " + stringResource(R.string.first_key_any)
        FirstRun.Caption.ANY -> stringResource(R.string.first_key_any)
        FirstRun.Caption.YOURS -> stringResource(R.string.first_key_yours)
        FirstRun.Caption.TRY_OTHER -> stringResource(R.string.first_key_try, stringResource(R.string.first_key_letter))
        FirstRun.Caption.NONE -> ""
    }
    val keys = stringResource(R.string.a11y_first_keys, stringResource(R.string.key_action), name)
    val act = stringResource(when (on) {
        FirstRun.Screen.K2 -> R.string.a11y_first_tab_opens
        FirstRun.Screen.K4 -> R.string.a11y_first_goes_on
        else -> R.string.a11y_first_opens
    })
    return Said("key", title, line, caption, keys.takeIf { on == FirstRun.Screen.K1 }, act)
}

/** A lesson: its mark is what it teaches, its caption says what other days are like. [parts]: its recipe, said to a screen reader a piece at a time. */
@Composable
private fun lessonSaid(on: FirstRun.Screen, parts: List<FirstRun.Part>): Said {
    val tab = stringResource(R.string.first_key_tab)
    val enter = stringResource(R.string.first_key_enter)
    // (An app's first letters are spelled, so that a reader does not make a word of them; a sum is said as it is; keys go by their names.)
    val recipe = parts.joinToString(", ") {
        when (it) {
            is FirstRun.Part.Typed -> if (it.text.all(Char::isLetter)) it.text.toList().joinToString(" ") else it.text
            FirstRun.Part.Key.TAB -> tab
            FirstRun.Part.Key.ENTER -> enter
        }
    }
    val keys = if (parts.isEmpty()) null else stringResource(R.string.a11y_first_example, recipe)
    val act = stringResource(R.string.a11y_first_skips)
    return when (on) {
        FirstRun.Screen.L3 -> Said("search", stringResource(R.string.first_search_title), stringResource(R.string.first_search_text), stringResource(R.string.first_practice), keys, act)
        FirstRun.Screen.L4 -> Said("calc", stringResource(R.string.first_sum_title), stringResource(R.string.first_sum_text), stringResource(R.string.first_enter_stays), keys, act)
        else -> Said("open", stringResource(R.string.first_open_title), stringResource(R.string.first_open_text), stringResource(R.string.first_practice), keys, act)
    }
}

/** The question: its title alone in the seat. Its text and its note are the band's. */
@Composable
private fun askSaid(): Said = Said("globe", stringResource(R.string.first_suggest_title), "", "", null, stringResource(R.string.a11y_first_asks))

/**
 * The seat: where a row would be. A disc with the step's mark, the title and one line under it (the question has the
 * title alone), and the counter where a row's kind stands. Its words roll when the screen changes, as a counter's do.
 */
@Composable
private fun Seat(symbol: String, title: String, line: String, counter: String?) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val second = scheme.onSurface.copy(alpha = SECOND)
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(Metrics.row), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = if (dark) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            AnimatedContent(symbol, transitionSpec = { (fadeIn(motion.fade(110)) togetherWith fadeOut(motion.fade(80))).using(null) }, label = "mark") {
                Icon(Symbols.of(it), null, Modifier.size(20.dp), tint = second)
            }
        }
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 16.dp)) {
            AnimatedContent(title, transitionSpec = { motion.roll() }, contentAlignment = Alignment.TopStart, label = "title") {
                Text(it, color = scheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadeEnd())
            }
            if (line.isNotEmpty()) AnimatedContent(line, transitionSpec = { motion.roll() }, contentAlignment = Alignment.TopStart, label = "line") {
                Text(it, color = second, style = SMALL, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadeEnd())
            }
        }
        if (counter != null) AnimatedContent(counter, transitionSpec = { motion.roll() }, contentAlignment = Alignment.CenterEnd, label = "counter") {
            Text(it, color = second, style = SMALL, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * The band under the seat, for the key's step and for a lesson: what to press, from the titles' edge on the band's
 * centre line ([keys]); the caption under it, on the line a footer's words stand on; the answers at the right end, on
 * the same centre line.
 */
@Composable
private fun Band(armed: Int, caption: String, strip: @Composable () -> Unit, keys: @Composable BoxWithConstraintsScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val second = scheme.onSurface.copy(alpha = SECOND)
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
                }
            }
        }
        Spacer(Modifier.width(24.dp))
        strip()
    }
}

/**
 * The key's step in the band: on its first screen the suggested keys, Action and the key beside it, as two large caps;
 * on the others "your key", one blank cap that takes the check once the key works.
 */
@Composable
private fun BoxWithConstraintsScope.KeyCaps(model: OverlayModel, on: FirstRun.Screen) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val context = LocalContext.current
    val second = scheme.onSurface.copy(alpha = SECOND)
    val key = model.suggested
    val name = SystemWords.name(context, key)
    val action = stringResource(R.string.key_action)
    // The system's name for the key is used on the cap only where the cap then has room: a cap never shrinks or wraps.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val room = with(density) { maxWidth.toPx() }
    // (Its stand-in on the cap is a word known to fit, the same in every language: the caption and the rows keep the name.)
    val fallback = stringResource(if (key == FirstRun.Key.QUICK_INSERT) R.string.first_key_quick_cap else R.string.first_key_letter)
    val shown = remember(action, name, fallback, room) {
        fun wide(s: String) = measurer.measure(s, CAP, maxLines = 1, softWrap = false).size.width
        val fixed = with(density) { (CAP_PAD * 4 + PLUS).toPx() }
        if (wide(action) + wide(name) + fixed <= room) name else fallback
    }
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
                }
            }
        }
    }
}

/**
 * The question's band (design.md §7): the disclosure, whole and in strong ink, from the titles' edge to the right
 * margin; under it a note, and at the right end the two answers. Neither is lit until Tab or the pointer has chosen
 * one, so Enter alone gives no answer: a `tab` cap says how to get to them. [more]: what the text needs beyond its four
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
        }
    }
}

/**
 * The room the question's disclosure needs beyond its four lines, told to the model as soon as the question is due,
 * before its screen stands: the panel's height is then known before anything moves, and the text is never cut (a long
 * name of a search engine, a large type size). It draws nothing. [width]: the panel's.
 */
@Composable
fun AskRoom(model: OverlayModel, width: Dp) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val text = stringResource(R.string.first_suggest_text, model.settings.engine().name)
    val more = remember(text, width, density) {
        with(density) {
            val high = measurer.measure(text, ASK, constraints = Constraints(maxWidth = (width - LEFT - RIGHT).roundToPx())).size.height
            (high.toDp() - ASK_TEXT).coerceAtLeast(0.dp)
        }
    }
    SideEffect { model.askRoom(more) }
}

/** The "+" of a chord, with 12 dp of air on either side. */
private val PLUS = 34.dp

/**
 * A key as large as a row: flat, the key cap's fill, a white ring inside its edge as the scope's chip has. A
 * picture, not a button: it takes no pointer and no key.
 */
@Composable
private fun BigCap(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDark.current
    Box(
        modifier.height(Metrics.row).defaultMinSize(minWidth = Metrics.row).clip(CAP_SHAPE)
            .background(scheme.onSurface.copy(alpha = if (dark) 0.14f else 0.10f))
            .border(with(LocalDensity.current) { 1f.toDp() }, Color.White.copy(alpha = if (dark) 0.30f else 0.55f), CAP_SHAPE)
            .padding(horizontal = CAP_PAD),
        contentAlignment = Alignment.Center,
    ) { content() }
}
```

- [ ] **Step 4: Measure the question's text as soon as it is due, in `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** One change.

Find:

```kotlin
        // The window as it stands in this frame. It follows the height's spring a frame behind (a window is resized through
```

Make it:

```kotlin
        // First run's question: how much room its text needs is measured as soon as it is due, before its screen stands.
        if (model.due == FirstRun.Screen.Q) AskRoom(model, full)
        // The window as it stands in this frame. It follows the height's spring a frame behind (a window is resized through
```

- [ ] **Step 5: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt
git commit -m "First run: the stage draws the lessons and the question on the key's skeleton, each at its own height (nothing shows them yet)"
```

### Task 8: In a lesson Enter is practice

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Strip.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`

**Interfaces:**
- Consumes: core (part 1) `FirstRun.enters(s: State, line: String?, effect: Effect, searchable: Boolean): Enter` and `FirstRun.ran(s: State, enter: Enter): State`, which get their first callers here; `SearchEngine.IN_APP` (the start of the id of a search inside an app). In `OverlayModel`: `lesson`, `example` (Task 5), `private fun first()`, `private fun step(f)`, `chip`, `query`, `act`, `current`, `armed`, `opened`, `private var closed`, `private data class For(key, text, act)`, `flash`, `private fun search()`, `fun used(r, a)`; `app.guide.used(chip, row, action): String?` (the line of the list of everything a run belongs to: `apps`, `places`, `appsearch`, `sums`, `settings`…). In `OverlayActivity`: `fun run(r: Result, a: Action, keep: Boolean = false)`, `private var settled`, `private var ran`, `private fun say(word, bad)`, `motion.hold(ms)`. Strings `first_practice_open`, `first_practice_search` (Task 4).
- Produces:
  - In `OverlayModel`: `fun enters(r: Result, a: Action): FirstRun.Enter`, `val pressed: Boolean`, `fun ran(enter: FirstRun.Enter)`, `fun giveWay()`, `private fun empty()`, `fun named(r: Result): String`.
  - `OverlayActivity.run` asks `model.enters` first; `private fun lessonOver(enter: FirstRun.Enter)`; `LESSON_MS` (520).
  - `ActionStrip(…, pressed: Boolean = false)` and `ResultRow(…, pressed: Boolean = false)`, each as its last parameter.

"Settled for the build", point 6: nothing opens in first run. While a lesson stands, Enter on an app's Open (also on a place behind Window) and on a search inside an app starts nothing: the armed slot shows as pressed, the footer says with its check "That opens …" or "That searches …", the lesson is over at once, and 520 ms later the list gives way to what stands next. A sum is copied for real, the footer says "Copied", and the panel stays in the same way. Making an app the chip (Search on its row) is done for real. Everything else runs as every day and the panel goes (`BUILD.md`'s table).

It is decided in `OverlayActivity.run`, before the executor is asked: Enter, a click, Ctrl + digit and an Enter that waited for its list or its answer all pass there (decision 4 in this plan's head). Until the list has given way `settled` keeps a second Enter from running anything: the lesson is over then, and core would say "as always" for a list that still stands (decision 5 in this plan's head). The list gives way only if nothing was typed since that Enter (decision 3 in this plan's head).

While `OverlayModel.lesson` is null, `enters` answers `AS_ALWAYS` without asking anything: on every other day `run` is as it was. Until Task 12 `lesson` is always null.

There is no unit test for this task: the app module has no test source set. What Enter does in a lesson is core's and is tested there (`FirstRunLessonsTest`); its check here is that it builds, and Task 13's checks 5 to 11 on a device.

- [ ] **Step 1: The lesson's Enter, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Two changes: an import, and a new section above the tips'.

Find:

```kotlin
import io.github.kuscher.booklight.core.Stops
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Stops
```

Find:

```kotlin
    }

    /** The tip that is on screen under the empty field; null = none. */
```

Make it:

```kotlin
    }

    // ---- first run: the lessons

    /**
     * What Enter does with [a] on [r] now (core `FirstRun.enters`). While a lesson stands nothing opens: an app's Open and
     * a search inside an app are practice, and a sum is copied for real with the panel staying. On every other day, and
     * for everything else, as always.
     */
    fun enters(r: Result, a: Action): FirstRun.Enter =
        if (lesson == null) FirstRun.Enter.AS_ALWAYS else FirstRun.enters(first(), app.guide.used(chip, r, a), a.effect, searchable = example?.enter ?: true)

    /** A lesson's Enter that was taken: what the field held, and the row and the action it was pressed on. */
    private data class Taken(val on: For, val row: String?, val armed: Int)
    private var taken by mutableStateOf<Taken?>(null)

    /**
     * The armed slot of the selected row shows as pressed: a lesson's Enter was taken there, nothing was run (or a sum was
     * copied and the panel stays), and its list is about to give way.
     */
    val pressed: Boolean get() = taken.let { it != null && it == Taken(For(chip?.key, query, act), current?.id, armed) }

    /** A lesson's Enter did [enter]: that lesson is over at once (core `FirstRun.ran`), while its list still stands for a moment ([giveWay]). */
    fun ran(enter: FirstRun.Enter) {
        step { FirstRun.ran(it, enter) }
        taken = Taken(For(chip?.key, query, act), current?.id, armed)
    }

    /**
     * The list of a lesson that is over gives way to what stands next: the field is emptied in one change, and the next
     * screen's stage is there. Not where the user has typed on since that Enter: then it comes when the field is empty again.
     */
    fun giveWay() {
        val was = taken ?: return
        taken = null
        if (was.on != For(chip?.key, query, act)) return
        flash = null
        empty()
    }

    /** Booklight empties the field itself: chip, text and rows go in one change, and nothing of it counts as typed. */
    private fun empty() {
        typist?.cancel(); typist = null
        answering?.cancel(); answering = null; thinking = false; whenAnswered = null; whenReady = null
        chip = null; word = null; via = null; origin = null; act = null; held = null; foreign = false; touched = false
        query = ""
        search()
    }

    /** What [r] is about, by its name, for the footer's word: the app, also for a line of one of its row's two lists and for a search inside it. */
    fun named(r: Result): String = when {
        r.kind == Kind.ACTION -> closed.firstOrNull { it.id == opened }?.title ?: r.title
        r.id.startsWith(SearchEngine.IN_APP) -> r.label ?: (chip as? AppChip)?.name ?: r.title
        else -> r.title
    }

    /** The tip that is on screen under the empty field; null = none. */
```

- [ ] **Step 2: Practice, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Six changes: what `settled` is for; `run` asks first what Enter does in a lesson; a lesson's sum does not count as "something ran"; the sum's branch of `run`'s last `when`; `lessonOver`; and its time.

Find:

```kotlin
    /** Something ran and the panel is only waiting to close: a second Enter in that moment must not run it again. */
```

Make it:

```kotlin
    /**
     * Something ran and the panel is only waiting to close: a second Enter in that moment must not run it again. Also while
     * the list of one of first run's lessons waits to give way: its lesson is over, and a second Enter must not run for real
     * what the first one only practised.
     */
```

Find:

```kotlin
        // A pinned text exists nowhere else: when another pin takes its place, the footer says which one went.
```

Make it:

```kotlin
        // First run: while a lesson stands nothing opens (core `FirstRun.enters`). An app's Open and a search inside an app are
        // practice: nothing is started, the footer says what Enter would have done, and the lesson is over. Asked here, where
        // everything that runs passes (Enter, a click, Ctrl + digit, an Enter that waited for its list or its answer).
        val lesson = model.enters(r, a)
        if (lesson == FirstRun.Enter.PRACTICE_OPEN || lesson == FirstRun.Enter.PRACTICE_SEARCH) {
            model.used(r, a)
            // (Where no app can be searched a settings page stands in for the search: Enter would open it.)
            val searches = lesson == FirstRun.Enter.PRACTICE_SEARCH && a.effect !is Effect.OpenSettings
            say(getString(if (searches) R.string.first_practice_search else R.string.first_practice_open, model.named(r)))
            lessonOver(lesson)
            return
        }
        // A pinned text exists nowhere else: when another pin takes its place, the footer says which one went.
```

Find:

```kotlin
        ran = a.effect !is Effect.Grant
```

Make it:

```kotlin
        // (Nor does a lesson's sum: it was copied, and the panel stays for what is typed next.)
        ran = a.effect !is Effect.Grant && lesson != FirstRun.Enter.COPY_STAYS
```

Find:

```kotlin
        when {
            a.keepOpen || keep -> model.refresh()
```

Make it:

```kotlin
        when {
            // First run: a lesson's sum is copied for real, the footer says so, and the panel stays.
            lesson == FirstRun.Enter.COPY_STAYS -> lessonOver(lesson)
            a.keepOpen || keep -> model.refresh()
```

Find:

```kotlin
    }

    /** A word in the footer for a moment. */
```

Make it:

```kotlin
    }

    /**
     * A lesson's Enter has had its answer (practice, or its sum copied). The lesson is over at once; its list stands for as
     * long as the panel would on any other day take to go, the slot pressed, and then gives way to what stands next. Until
     * then Enter does nothing more.
     */
    private fun lessonOver(enter: FirstRun.Enter) {
        settled = true
        model.ran(enter)
        lifecycleScope.launch { delay(motion.hold(LESSON_MS)); settled = false; model.giveWay() }
    }

    /** A word in the footer for a moment. */
```

Find:

```kotlin
        /** Asked for by name (the widget, the tile): the panel, whoever started it. */
```

Make it:

```kotlin
        /** How long a lesson's list stands after its Enter before it gives way: as long as the panel takes to go once a word was said. */
        private const val LESSON_MS = 520L
        /** Asked for by name (the widget, the tile): the panel, whoever started it. */
```

- [ ] **Step 3: The pressed slot, in `app/src/main/java/io/github/kuscher/booklight/overlay/Strip.kt`.** Three changes, all in `ActionStrip`: the parameter, how far it is pressed, and the ink drawn over the pane.

Find:

```kotlin
    icons: AppIcons? = null,
```

Make it:

```kotlin
    icons: AppIcons? = null,
    /** The armed slot shows as pressed: Enter was taken there and the row is about to go (first run's practice). */
    pressed: Boolean = false,
```

Find:

```kotlin
    val drain = scheme.onErrorContainer
```

Make it:

```kotlin
    val drain = scheme.onErrorContainer
    // Pressed: the pane gains a little ink, the design system's pressed token (0.08, 80 ms in and 120 out). No colour.
    val press by animateFloatAsState(if (pressed) 1f else 0f, motion.fade(if (pressed) 80 else 120), label = "press")
    val pressInk = scheme.onSurface.copy(alpha = 0.08f)
```

Find:

```kotlin
                pane(x0, x1, lerp(pane, alarm, (red / sum).coerceIn(0f, 1f)), rim)
```

Make it:

```kotlin
                pane(x0, x1, lerp(pane, alarm, (red / sum).coerceIn(0f, 1f)), rim)
                if (press > 0f) drawRoundRect(pressInk.copy(alpha = pressInk.alpha * press), Offset(x0, 0f), Size(x1 - x0, size.height), CornerRadius(size.height / 2))
```

- [ ] **Step 4: The row passes it on, in `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`.** Three changes: `ResultsBody` says which row is pressed, `ResultRow` takes it, and hands it to its strip.

Find:

```kotlin
                    onGrow = { model.grow(r.id) },
```

Make it:

```kotlin
                    onGrow = { model.grow(r.id) },
                    pressed = selected && model.pressed,
```

Find:

```kotlin
    onGrow: () -> Unit = {},
```

Make it:

```kotlin
    onGrow: () -> Unit = {},
    /** Its armed action shows as pressed (first run's practice). */
    pressed: Boolean = false,
```

Find:

```kotlin
                    moreLabel = stringResource(R.string.action_more), lessLabel = less, turn = { arrow.value }, icons = icons)
```

Make it:

```kotlin
                    moreLabel = stringResource(R.string.action_more), lessLabel = less, turn = { arrow.value }, icons = icons, pressed = pressed)
```

- [ ] **Step 5: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 6: Nothing else reaches the executor**

```bash
grep -rn "executor.run(" app/src/main/java/io/github/kuscher/booklight/overlay
```

Expected: two lines, as before this task: `OverlayActivity.run`'s (after the lesson's question) and `OverlayModel.nudge`'s (a level's Left and Right, which open nothing).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt app/src/main/java/io/github/kuscher/booklight/overlay/Strip.kt app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt
git commit -m "First run: in a lesson Enter is practice: nothing opens, the slot is pressed, the footer says what it would have done, and the list gives way to what stands next"
```

### Task 9: The placeholder and the coach line

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt`

**Interfaces:**
- Consumes: core (Task 3) `FirstRun.Coach`, `FirstRun.coach(s: State, enter: Enter, effect: Effect?, search: Boolean): Coach?`, `FirstRun.MOST_LETTERS`; `FirstRun.count(s)`. In `OverlayModel`: `lesson`, `stage`, `example` (Task 5), `enters(r, a)` (Task 8), `chosen(): Pair<Result, Action>?`, `current`, `results`, `hint`. In `FirstStage.kt` (Task 7): `CAPTION`, and its imports. `Keycap(label)` (`Footer.kt`). Strings `first_open_hint`, `first_search_hint`, `first_sum_example`, `first_coach_open`, `first_coach_tab`, `first_coach_into`, `first_coach_search`, `first_coach_sum`, `first_open_title`, `first_search_title`, `first_sum_title`, `first_step`, `first_key_enter`, `first_key_tab` (Task 4).
- Produces: in `OverlayModel`: `val coach: FirstRun.Coach?`, `val firstHint: String?`. In `FirstStage.kt`: `CoachLine(model: OverlayModel, shown: Boolean)`. In `Footer`: `val quiet` (what a row says of itself at the footer's left end), which Task 11 adds to. `Field`'s placeholder asks `model.firstHint` after the chip's own and before a tip's.

While the user types, a lesson's guidance has the two free seats (`design.md` §6): the placeholder says what to type before the first letter ("An app’s first letters", "LETTERS, then Tab", "150 + 20%"; under an app's chip the chip's own stands), and the footer's left end says the next key and what it does today: the counter, a dot, one key cap, the words. Where the selected row is not the lesson's own, the lesson's title stands there. A word that just happened ("Copied", "That opens …") has the seat for its time, and so has a cell's name.

`CoachLine` is composed only while a lesson stands: on every other day the footer is as it was.

There is no unit test for this task: the app module has no test source set. Which line stands is core's and is tested there (`FirstRunRecipeTest`); its check here is that it builds, and Task 13's checks 5, 6, 7 and 23 on a device.

- [ ] **Step 1: The two answers, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** One change: `coach` and `firstHint` go in above `Taken`, in the section Task 8 made.

Find:

```kotlin
        if (lesson == null) FirstRun.Enter.AS_ALWAYS else FirstRun.enters(first(), app.guide.used(chip, r, a), a.effect, searchable = example?.enter ?: true)

    /** A lesson's Enter that was taken: what the field held, and the row and the action it was pressed on. */
```

Make it:

```kotlin
        if (lesson == null) FirstRun.Enter.AS_ALWAYS else FirstRun.enters(first(), app.guide.used(chip, r, a), a.effect, searchable = example?.enter ?: true)

    /**
     * The coach line for the footer's left end (core `FirstRun.coach`): while a lesson's list is typed, the next key and
     * what it does today, by the row that is selected and the action that is armed. Null where no lesson stands, and
     * where no list does.
     */
    val coach: FirstRun.Coach? get() {
        if (lesson == null || results.isEmpty()) return null
        val picked = chosen()
        val enter = picked?.let { (r, a) -> enters(r, a) } ?: FirstRun.Enter.AS_ALWAYS
        return FirstRun.coach(first(), enter, picked?.second?.effect, search = current?.actions?.any { (it.effect as? Effect.EnterScope)?.act == Act.SEARCH } == true)
    }

    /**
     * The field's placeholder where first run has something to say there: what to type, while a lesson's stage stands.
     * Null: the ordinary one. (Under a chip the chip's own stands, [hint].)
     */
    val firstHint: String? get() = when (stage) {
        FirstRun.Screen.L2 -> app.getString(R.string.first_open_hint)
        FirstRun.Screen.L3 -> example?.search?.take(FirstRun.MOST_LETTERS)?.takeIf { it.isNotEmpty() }?.let { app.getString(R.string.first_search_hint, it) }
        FirstRun.Screen.L4 -> app.getString(R.string.first_sum_example)
        else -> null
    }

    /** A lesson's Enter that was taken: what the field held, and the row and the action it was pressed on. */
```

- [ ] **Step 2: The coach line, in `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`.** Two changes: four imports, and `CoachLine` above the "+" of a chord.

Find:

```kotlin
import androidx.compose.ui.semantics.clearAndSetSemantics
```

Make it:

```kotlin
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
```

Find:

```kotlin
}

/** The "+" of a chord, with 12 dp of air on either side. */
```

Make it:

```kotlin
}

/**
 * First run's coach line, at the footer's left end while a lesson's list is typed (design.md §6): where the lesson is
 * in its run, one key cap, and what that key does today. Where the selected row is not the lesson's own, the lesson's
 * title stands there. Its words change where they stand, in a fade; where the line has no room, the counter goes first.
 * [shown]: false while a word that just happened ("Copied") has the seat.
 */
@Composable
fun CoachLine(model: OverlayModel, shown: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val ink = scheme.onSurface.copy(alpha = SECOND)
    val counter = FirstRun.count(model.settings.firstRun())?.let { stringResource(R.string.first_step, it.first, it.second) }
    val title = stringResource(when (model.lesson) {
        FirstRun.Screen.L3 -> R.string.first_search_title
        FirstRun.Screen.L4 -> R.string.first_sum_title
        else -> R.string.first_open_title
    })
    // The key, as the footer's caps name it, and what it does.
    val line: Pair<String, String>? = when (model.coach.takeIf { shown }) {
        FirstRun.Coach.OPENS -> "⏎" to stringResource(R.string.first_coach_open)
        FirstRun.Coach.TO_SEARCH -> "tab" to stringResource(R.string.first_coach_tab)
        FirstRun.Coach.INTO_APP -> "⏎" to stringResource(R.string.first_coach_into)
        FirstRun.Coach.SEARCHES -> "⏎" to stringResource(R.string.first_coach_search)
        FirstRun.Coach.COPIES -> "⏎" to stringResource(R.string.first_coach_sum)
        FirstRun.Coach.TITLE -> "" to title
        null -> null
    }
    val enter = stringResource(R.string.first_key_enter)
    val tab = stringResource(R.string.first_key_tab)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    AnimatedContent(line, transitionSpec = { (fadeIn(motion.fade(120)) togetherWith fadeOut(motion.fade(80))).using(null) }, contentAlignment = Alignment.CenterStart, label = "coach") { said ->
        if (said != null) BoxWithConstraints(Modifier.fillMaxWidth()) {
            val (cap, words) = said
            val before = counter?.let { "$it · " }.orEmpty()
            // The counter stands before the cap only where all of the line then fits.
            val room = with(density) { maxWidth.toPx() }
            val whole = remember(before, cap, words, room) {
                fun wide(s: String) = measurer.measure(s, CAPTION, maxLines = 1, softWrap = false).size.width
                val key = if (cap.isEmpty()) 0f else with(density) { (if (cap == "⏎") 27.dp else 14.dp).toPx() + (if (cap == "⏎") 0 else wide(cap)) + 12.dp.toPx() }
                wide(before) + key + wide(words) <= room
            }
            // A screen reader is told the line when it changes, the key by its name.
            val told = listOfNotNull(counter, (if (cap == "⏎") enter else if (cap.isEmpty()) null else tab)?.let { "$it $words" } ?: words).joinToString(". ")
            Row(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite; contentDescription = told }, verticalAlignment = Alignment.CenterVertically) {
                if (whole && before.isNotEmpty()) Text(before, color = ink, style = CAPTION, maxLines = 1, softWrap = false)
                if (cap.isNotEmpty()) { Keycap(cap); Spacer(Modifier.width(6.dp)) }
                Text(words, color = ink, style = CAPTION, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The "+" of a chord, with 12 dp of air on either side. */
```

- [ ] **Step 3: Its seat, in `app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt`.** Two changes, both in the left `Box`: it centres what stands in it, and what a row says of itself there gets a name, `quiet`; and the coach line is drawn after it.

Find:

```kotlin
        Box(Modifier.weight(1f)) {
            val word = model.flash
            // (Under an answer that came from somewhere else: who gave it and when, said as quietly as a cell's name.)
            AnimatedContent(word ?: grid?.cells?.getOrNull(model.cell)?.name?.let { " $it" } ?: (r?.body as? Body.Flight)?.source?.let { " $it" }, transitionSpec = {
```

Make it:

```kotlin
        // (What stands in this seat stands on its centre line, also for the frames in which one thing gives way to another.)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            val word = model.flash
            // (Under an answer that came from somewhere else: who gave it and when, said as quietly as a cell's name.)
            val quiet = grid?.cells?.getOrNull(model.cell)?.name?.let { " $it" } ?: (r?.body as? Body.Flight)?.source?.let { " $it" }
            AnimatedContent(word ?: quiet, transitionSpec = {
```

Find:

```kotlin
                }
            }
        }
        // (key, what it does) for the selected row; empty key = nothing to say.
```

Make it:

```kotlin
                }
            }
            // First run: while a lesson's list is typed, its coach line has this seat. A word that just happened has it first,
            // and so has what a row says here of itself (a cell's name).
            if (model.lesson != null) CoachLine(model, shown = word == null && quiet == null)
        }
        // (key, what it does) for the selected row; empty key = nothing to say.
```

- [ ] **Step 4: The placeholder, in `app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt`.** One change.

Find:

```kotlin
                AnimatedContent(model.hint ?: model.tip?.takeIf { !model.tipOff }?.example?.trim() ?: stringResource(R.string.search_hint), transitionSpec = {
```

Make it:

```kotlin
                // (While a lesson of first run stands: what that lesson types.)
                AnimatedContent(model.hint ?: model.firstHint ?: model.tip?.takeIf { !model.tipOff }?.example?.trim() ?: stringResource(R.string.search_hint), transitionSpec = {
```

- [ ] **Step 5: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt app/src/main/java/io/github/kuscher/booklight/overlay/Field.kt
git commit -m "First run: while a lesson stands the placeholder says what to type, and the footer's left end says the next key and what it does today"
```

### Task 10: The panel's switch, and a row that shows it

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/Model.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Bodies.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`
- Modify: `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`

**Interfaces:**
- Consumes: core `sealed interface Body` (`Model.kt`), `Result.label`, `Kind.SCOPE`. App: `Metrics.rowHeight(r)` and `Metrics.tall` (92 dp); in `Bodies.kt`: `SMALL`, and its imports (`AnimatedContent`, `animateFloatAsState`, `drawBehind`, `CornerRadius`, `Offset`, `Box`, `size`, `padding`, `Text`, `MaterialTheme`, `Alignment`, `Modifier`, `getValue`, `dp`); `LocalMotion` (`pop()`, `fade(ms)`, `roll()`), `LocalDark`, `SECOND`; in `Rows.kt`: `ResultRow` (its `described`, its `RowFrame { … }`, the last `when` of its strip), `Keycap(label, strong)`.
- Produces: core `data class Body.Switch(val on: Boolean, val word: String) : Body`. App: `PanelSwitch(b: Body.Switch)`; a row whose body is a `Body.Switch` is 92 dp, shows the switch at its end, selected or not, and has no strip; a row of kind `SCOPE` whose `label` is not null shows that label as a key cap at its end where it showed `tab`; `./bl debug dump` says a switch's row as `{on}` or `{off}`.

The design system specifies a switch (`docs/design/design-system.md` §3: an 18 dp thumb in a 44 × 24 dp track, its state as a word before it; on: the track at full ink and the thumb in the ground's colour; off: the track at rest and the thumb in ink), and the panel has built none yet (`design.md` §7 says so). The choices' first row needs it. Its thumb travels on `pop`, its track's fill changes on `fade(120)` and its word rolls: the design system's own motion (§4, row 11), with helpers that exist. It is a picture of the state: the row takes the key and the click.

The second change is for the choices' other row: the list of everything is found by typing `?`, and its row there shows that key where a scope's row in a typed list shows `tab`. The engine's own scope rows have no label and show `tab` as before.

Core's part is one data class with no logic, so no test comes first; `./bl test` must still pass. `DebugReceiver.describe` has a `when` over every `Body`: without its new branch the debug build does not compile.

- [ ] **Step 1: The body, in `core/src/main/kotlin/io/github/kuscher/booklight/core/Model.kt`.** One change.

Find:

```kotlin
    /**
     * A text under a caption, on up to four lines: what a prompt will be asked about, and then the
```

Make it:

```kotlin
    /** A switch at the row's end, [on] or off, with its state as a [word] before it ("On", "Off"). The row has no strip: its one action flips the switch. */
    data class Switch(val on: Boolean, val word: String) : Body
    /**
     * A text under a caption, on up to four lines: what a prompt will be asked about, and then the
```

- [ ] **Step 2: Its row's height, in `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt`.** One change.

Find:

```kotlin
        is Body.Slots, is Body.Mono -> tall
```

Make it:

```kotlin
        // (A switch's row has two lines under its name: what the switch does, and when.)
        is Body.Slots, is Body.Mono, is Body.Switch -> tall
```

- [ ] **Step 3: Its drawing, in `app/src/main/java/io/github/kuscher/booklight/overlay/Bodies.kt`.** One change: `PanelSwitch` goes in above `Swatch`.

Find:

```kotlin
}.getOrNull()

/** A swatch of one colour, with a hairline so white and black still have an edge. */
```

Make it:

```kotlin
}.getOrNull()

/**
 * A switch at a row's end (docs/design/design-system.md §3): its state as a word, 12 dp before an 18 dp thumb in a
 * 44 × 24 dp track. On: the track at full ink, the thumb in the ground's colour, at the right. Off: the track at rest,
 * the thumb in ink, at the left. The thumb travels on a spring, the track's fill changes in a fade, the word rolls. It
 * shows the state and takes no pointer of its own: the row has the key and the click.
 */
@Composable
fun PanelSwitch(b: Body.Switch) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val rest = scheme.onSurface.copy(alpha = if (LocalDark.current) 0.24f else 0.20f)
    val at by animateFloatAsState(if (b.on) 1f else 0f, motion.pop(), label = "thumb")
    val filled by animateFloatAsState(if (b.on) 1f else 0f, motion.fade(120), label = "track")
    AnimatedContent(b.word, transitionSpec = { motion.roll() }, contentAlignment = Alignment.CenterEnd, label = "state") {
        Text(it, color = scheme.onSurface.copy(alpha = SECOND), style = SMALL, maxLines = 1, softWrap = false)
    }
    Box(Modifier.padding(start = 12.dp).size(44.dp, 24.dp).drawBehind {
        drawRoundRect(androidx.compose.ui.graphics.lerp(rest, scheme.onSurface, filled), cornerRadius = CornerRadius(size.height / 2))
        // (3 dp inside the track at either end: the spring's small overshoot stays inside it.)
        val r = 9.dp.toPx()
        val from = 3.dp.toPx() + r
        drawCircle(androidx.compose.ui.graphics.lerp(scheme.onSurface, scheme.surfaceContainerLowest, filled), r, Offset(from + (size.width - 2 * from) * at, size.height / 2))
    })
}

/** A swatch of one colour, with a hairline so white and black still have an edge. */
```

- [ ] **Step 4: The row, in `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`.** Three changes, all in `ResultRow`: what a screen reader is told of a switch's row; the switch in place of the strip; and a scope's row that names its own key.

Find:

```kotlin
    val described = stringResource(R.string.a11y_selected, if (body is Body.Flight) listOfNotNull(r.title, body.headline.ifEmpty { null }, body.badge).joinToString(", ") else r.title, r.actions.getOrNull(armed)?.label ?: kind)
```

Make it:

```kotlin
    // (And a switch's row with its state: "Show your usual, Off".)
    val described = if (body is Body.Switch) "${r.title}, ${body.word}"
        else stringResource(R.string.a11y_selected, if (body is Body.Flight) listOfNotNull(r.title, body.headline.ifEmpty { null }, body.badge).joinToString(", ") else r.title, r.actions.getOrNull(armed)?.label ?: kind)
```

Find:

```kotlin
        }

        // What the row can do arrives on the selected row; the others say what kind of thing they are.
```

Make it:

```kotlin
        }

        // A switch is the row's own control: it stands at the row's end, selected or not, and the row has no strip.
        if (body is Body.Switch) { PanelSwitch(body); return@RowFrame }

        // What the row can do arrives on the selected row; the others say what kind of thing they are.
```

Find:

```kotlin
                r.kind == Kind.SCOPE -> Keycap("tab")
```

Make it:

```kotlin
                // (A scope's row that names its own key shows that key, as an answer's caps: `?` for the list of everything.)
                r.kind == Kind.SCOPE -> Keycap(r.label ?: "tab", strong = r.label != null)
```

- [ ] **Step 5: The dump, in `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`.** One change, in `describe`.

Find:

```kotlin
            is io.github.kuscher.booklight.core.Body.Task -> if (b.done) " {done}" else " {open}"
```

Make it:

```kotlin
            is io.github.kuscher.booklight.core.Body.Task -> if (b.done) " {done}" else " {open}"
            is io.github.kuscher.booklight.core.Body.Switch -> if (b.on) " {on}" else " {off}"
```

- [ ] **Step 6: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 7: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/Model.kt app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt app/src/main/java/io/github/kuscher/booklight/overlay/Bodies.kt app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt
git commit -m "First run: a switch at a row's end, and a row that shows one (nothing uses it yet)"
```

### Task 11: The choices, and the fold

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`

**Interfaces:**
- Consumes: core `Body.Switch(on, word)` (Task 10), `FirstRun.answer(s, Answer.DONE)` (on the choices it ends the run), `Effect.Internal(command)`, `Effect.EnterScope(key)`, `Kind.OTHER`, `Kind.SCOPE`. In `OverlayModel`: `due`, `stage` (Task 5), `giveWay()`, `empty()` (Task 8), `firstHint` (Task 9), `private fun step(f)`, `firstChanged()`, `zeroUp`, `results`, `selected`, `armed`, `cell`, `current`, `opened`, `query`, `chip`, `private var resultsFor`, `private var whenReady`, `private var filledAt`, `settings`, `fun change(f)`, the companion's `HELP` ("?") and `CONFIRM_GAP_MS` (350), `up`, `type`, `enterScope`, `typeOut`, `search`, `enter`, `runRow`. `Footer`'s `val quiet` (Task 9). In `FirstStage.kt` its imports (Task 7, Task 9). Strings `set_usual_show`, `help_title`, `help_about`, `action_open`, `card_done` (there), `first_usual_text`, `first_on`, `first_off`, `first_labs`, `first_end_hint`, `a11y_first_choices`, `a11y_first_done` (Task 4).
- Produces: in `OverlayModel`: `var choicesUp: Boolean`, `var ended: Boolean` (their setters are the model's own), `val atRest: Boolean`, `fun offerChoices(): Boolean`, `fun closing()`; private: `choiceRows()`, `flipUsual()`, `leaveChoices()`, `fold()`, the companion's `FIRST` and `USUAL`. `firstHint` says the ending's placeholder once `ended`. In `FirstStage.kt`: `ChoicesSaid(model: OverlayModel)`. `OverlayActivity` sets the choices down in `onCreate` and tells the model when the panel closes.

`design.md` §7: the choices are a real list of two rows, 268 dp with the field (68 + 8 + 92 + 56 + 8 + 36), nothing selected. "Show your usual" with its two lines and the panel's switch: Enter flips it and the panel stays. "Everything Booklight does" with the `?` key at its end: Enter (or Tab) opens the list of everything, as typing `?` does. There is no row for the device's model. The footer's left end says where the two things live that need a key of the user's own; its right end says `⏎` Done and `esc` Close, and with a row selected "Done" gives way to that row's own hint. Down, Tab or the pointer brings the pill; Up from row one puts it away again.

The choices are done when they are left, however they are left: Enter with nothing selected (the fold: the rows go and the field is bare, with the placeholder "Type ? for everything Booklight does" for the rest of this opening), a typed letter, the second row, or the panel closing (decision 12 in this plan's head). Then the run is over (`FirstRun.answer(…, DONE)`).

They are a list, not a stage: `results` with `selected = -1`, as the usual rows stand at rest (`zeroUp`). `atRest` is true for either. They are set down where they are due and the field is empty with nothing under it: by `step` when the question is answered in the open panel, by the empty field's branch of `search` when a lesson's list has given way, and by the activity for a panel that opens on them, once it is known that the panel carries no text.

Until Task 12 the choices are never due: nothing a user sees changes in this task.

There is no unit test for this task: the app module has no test source set. That leaving the choices ends the run is core's and is tested there; its check here is that it builds, and Task 13's checks 16 to 19 on a device.

- [ ] **Step 1: The choices, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Eleven changes, in the file's order: `step` sets them down where their turn has come; `firstChanged` takes them away or sets them down as a debug hook says; `firstHint` says the ending's placeholder, and under it the new section "the choices, and the ending"; `up` treats them as it treats the usual rows, but for the last text; `type`, `enterScope` and `typeOut` leave them; the empty field's branch of `search` keeps them while they stand, and sets them down where their turn has come; `enter` folds them at rest, and flips the switch; `runRow` flips it too; and the companion names their provider and the switch's action.

Find:

```kotlin
        settings = app.prefs.now
        rearm(first())
    }
```

Make it:

```kotlin
        settings = app.prefs.now
        rearm(first())
        offerChoices()
    }
```

Find:

```kotlin
        if (due != null) exemplify()
        rearm(first())
    }
```

Make it:

```kotlin
        if (due != null) exemplify()
        rearm(first())
        // (The choices are a list: it goes where another screen is due now, and comes where they are.)
        if (choicesUp && due != FirstRun.Screen.C) { choicesUp = false; results = emptyList(); selected = 0; resultsFor = For(null, query) }
        offerChoices()
    }
```

Find:

```kotlin
     * The field's placeholder where first run has something to say there: what to type, while a lesson's stage stands.
     * Null: the ordinary one. (Under a chip the chip's own stands, [hint].)
     */
    val firstHint: String? get() = when (stage) {
        FirstRun.Screen.L2 -> app.getString(R.string.first_open_hint)
        FirstRun.Screen.L3 -> example?.search?.take(FirstRun.MOST_LETTERS)?.takeIf { it.isNotEmpty() }?.let { app.getString(R.string.first_search_hint, it) }
        FirstRun.Screen.L4 -> app.getString(R.string.first_sum_example)
        else -> null
    }
```

Make it:

```kotlin
     * The field's placeholder where first run has something to say there: what to type, while a lesson's stage stands;
     * and once the choices were left, for the rest of this opening, how to learn more. Null: the ordinary one. (Under a
     * chip the chip's own stands, [hint].)
     */
    val firstHint: String? get() = when (stage) {
        FirstRun.Screen.L2 -> app.getString(R.string.first_open_hint)
        FirstRun.Screen.L3 -> example?.search?.take(FirstRun.MOST_LETTERS)?.takeIf { it.isNotEmpty() }?.let { app.getString(R.string.first_search_hint, it) }
        FirstRun.Screen.L4 -> app.getString(R.string.first_sum_example)
        else -> if (ended) app.getString(R.string.first_end_hint) else null
    }

    // ---- first run: the choices, and the ending

    /**
     * First run's choices are the list (docs/design/first-run/design.md §7): two rows under the empty field with none
     * selected, as the usual rows stand at rest. "Show your usual" with the panel's switch, and the list of everything.
     */
    var choicesUp by mutableStateOf(false); private set
    /** The choices were left in this opening, and first run with them: the field's placeholder says how to learn more. */
    var ended by mutableStateOf(false); private set
    /** A list stands under the empty field with nothing selected (the usual rows, first run's choices): Down, Tab or the pointer brings the highlight. */
    val atRest: Boolean get() = (zeroUp || choicesUp) && current == null

    /**
     * The choices come, where they are what is due and the field is empty with nothing under it: when their turn comes in
     * the open panel, and for a panel that opens on them. True if they stand.
     */
    fun offerChoices(): Boolean {
        if (choicesUp) return true
        if (due != FirstRun.Screen.C || query.isNotEmpty() || chip != null || results.isNotEmpty() || opened != null) return false
        results = choiceRows(); selected = -1; armed = 0; cell = 0
        choicesUp = true
        resultsFor = For(null, query); whenReady = null
        // The Enter that answered the question must not also be the one that ends the choices: only a new press, a moment later.
        filledAt = SystemClock.uptimeMillis()
        return true
    }

    private fun choiceRows(): List<Result> = listOf(
        Result(
            id = "first:usual", provider = FIRST, kind = Kind.OTHER, title = app.getString(R.string.set_usual_show), subtitle = app.getString(R.string.first_usual_text),
            icon = io.github.kuscher.booklight.core.Icon.Symbol("list"), score = 1.0, learnable = false,
            actions = listOf(Action("usual", app.getString(R.string.set_usual_show), USUAL, keepOpen = true, symbol = "list")),
            body = Body.Switch(settings.zero, app.getString(if (settings.zero) R.string.first_on else R.string.first_off)),
        ),
        // The list of everything, as its own row is everywhere (a scope's row: Enter or Tab enters it), with its key at the row's end.
        Result(
            id = "first:help", provider = FIRST, kind = Kind.SCOPE, title = app.getString(R.string.help_title), subtitle = app.getString(R.string.help_about),
            icon = io.github.kuscher.booklight.core.Icon.Symbol("booklight"), score = 1.0, learnable = false, label = HELP,
            actions = listOf(Action("enter", app.getString(R.string.action_open), Effect.EnterScope("help"), keepOpen = true, symbol = "list")),
        ),
    )

    /** Enter on "Show your usual": the switch flips where it stands, and the panel stays. The rows themselves come from the next opening on. */
    private fun flipUsual() {
        change { it.copy(zero = !it.zero) }
        settings = app.prefs.now
        val on = current?.id
        results = choiceRows()
        selected = results.indexOfFirst { it.id == on }
    }

    /**
     * The choices are left, however they are left (Enter with nothing selected, a typed letter, the row that leads to the
     * list of everything): first run is over (core `FirstRun.answer`).
     */
    private fun leaveChoices() {
        if (!choicesUp) return
        choicesUp = false; ended = true
        step { FirstRun.answer(it, FirstRun.Answer.DONE) }
    }

    /** Enter on the choices with nothing selected: "Done". The rows go, and what is left is the bare field. */
    private fun fold() {
        leaveChoices()
        results = emptyList(); selected = 0; armed = 0; cell = 0
        resultsFor = For(null, query)
    }

    /** The panel is closing: choices that stand are left by that too. (The list itself stays as it is: it goes with the glass.) */
    fun closing() { if (choicesUp) step { FirstRun.answer(it, FirstRun.Answer.DONE) } }
```

Find:

```kotlin
        if (zeroUp) {
            when {
                selected > 0 -> move(-1)
                selected == 0 && opened == null -> if (!again) { moved(); selected = -1; armed = 0; cell = 0 }
                selected < 0 -> if (!again) restoreLast()
```

Make it:

```kotlin
        if (zeroUp || choicesUp) {
            when {
                selected > 0 -> move(-1)
                selected == 0 && opened == null -> if (!again) { moved(); selected = -1; armed = 0; cell = 0 }
                // (From first run's choices at rest Up goes nowhere: the last text would take their place, and end them.)
                selected < 0 -> if (!again && zeroUp) restoreLast()
```

Find:

```kotlin
        if (text == query) return
```

Make it:

```kotlin
        if (text == query) return
        leaveChoices()                      // a typed letter takes the place of first run's choices: they are done
```

Find:

```kotlin
    fun enterScope(s: Scope, text: String = "", word: String? = null, act: Act? = null) {
```

Make it:

```kotlin
    fun enterScope(s: Scope, text: String = "", word: String? = null, act: Act? = null) {
        leaveChoices()
```

Find:

```kotlin
    fun typeOut(text: String) {
```

Make it:

```kotlin
    fun typeOut(text: String) {
        leaveChoices()
```

Find:

```kotlin
            results = if (zeroStood) usualRows() else emptyList()
            zeroUp = results.isNotEmpty()
            selected = if (zeroUp) results.indexOfFirst { it.id == was } else 0
            armed = current?.armed ?: 0; cell = 0; resultsFor = For(null, text); whenReady = null
            // Back out of an app's chip that was entered from one of the usual rows: that row again, the action that was left armed.
            back?.takeIf { zeroUp }?.place(results, ::stops)?.let { (row, action) -> selected = row; armed = action }
```

Make it:

```kotlin
            // (First run's choices stand until they are left: a list made anew for the empty field is theirs again.)
            results = if (choicesUp) choiceRows() else if (zeroStood) usualRows() else emptyList()
            zeroUp = !choicesUp && results.isNotEmpty()
            selected = if (zeroUp || choicesUp) results.indexOfFirst { it.id == was } else 0
            armed = current?.armed ?: 0; cell = 0; resultsFor = For(null, text); whenReady = null
            // Back out of an app's chip that was entered from one of the usual rows: that row again, the action that was left armed.
            back?.takeIf { zeroUp }?.place(results, ::stops)?.let { (row, action) -> selected = row; armed = action }
            // First run's choices, where their turn has come (a lesson's list has just given way; or it came while something
            // was typed): they stand now that the field is empty.
            offerChoices()
```

Find:

```kotlin
        // On Window or on the row's arrow: Enter opens that stop's lines as a list, or closes them again.
        onList?.let { if (opened != null && list == it) close() else open(it); return }
        val (r, a) = chosen() ?: return
```

Make it:

```kotlin
        // First run's choices with nothing selected: Enter is "Done", and the list folds to the bare field.
        if (choicesUp && current == null) { fold(); return }
        // On Window or on the row's arrow: Enter opens that stop's lines as a list, or closes them again.
        onList?.let { if (opened != null && list == it) close() else open(it); return }
        val (r, a) = chosen() ?: return
        // The switch of first run's choices is the panel's own: it flips where it stands, and nothing runs.
        if (a.effect == USUAL) { flipUsual(); return }
```

Find:

```kotlin
        if (a.effect is Effect.Unsuggest) return        // and so is "Don't suggest"
```

Make it:

```kotlin
        if (a.effect is Effect.Unsuggest) return        // and so is "Don't suggest"
        if (a.effect == USUAL) { flipUsual(); return }  // the switch of first run's choices flips where it stands
```

Find:

```kotlin
        const val TIP_OFF_MS = 1600L
```

Make it:

```kotlin
        const val TIP_OFF_MS = 1600L
        /** The rows of first run's choices are the panel's own: no provider made them. */
        private const val FIRST = "first"
        /** What the switch of first run's choices does: caught by the panel before anything runs. */
        private val USUAL = Effect.Internal("usual")
```

- [ ] **Step 2: The panel sets them down and says when it closes, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Three changes: `onCreate`, `close`, `onStop`.

Find:

```kotlin
        take(intent)
        placeWindow()
```

Make it:

```kotlin
        take(intent)
        // First run's choices are a list, not a stage: set down here, once it is known that the panel carries no text.
        model.offerChoices()
        placeWindow()
```

Find:

```kotlin
        if (!ran) model.keep()
```

Make it:

```kotlin
        if (!ran) model.keep()
        model.closing()
```

Find:

```kotlin
        if (!stay) { if (!ran && !leaving) model.keep(); finishNow() }
```

Make it:

```kotlin
        if (!stay) { if (!ran && !leaving) model.keep(); model.closing(); finishNow() }
```

- [ ] **Step 3: Tab at rest, and what a screen reader is told, in `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** Two changes.

Find:

```kotlin
    // The seam the glass grows out of, and draws back into, lies on the field's centre line whatever the panel's height.
```

Make it:

```kotlin
    // So do first run's choices, and its ending is only a placeholder: a screen reader is told of each, once.
    if (model.choicesUp || model.ended) ChoicesSaid(model)
    // The seam the glass grows out of, and draws back into, lies on the field's centre line whatever the panel's height.
```

Find:

```kotlin
                    model.zeroUp && model.current == null && model.chip == null && model.query.isBlank() -> if (!again && !e.isShiftPressed) model.down(false)
```

Make it:

```kotlin
                    // (So do first run's choices.)
                    model.atRest && model.chip == null && model.query.isBlank() -> if (!again && !e.isShiftPressed) model.down(false)
```

- [ ] **Step 4: The pointer at rest, in `app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`.** One change, in `ResultsBody`'s call of `ResultRow`.

Find:

```kotlin
                    onHover = { if (!slot.leaving) model.select(slot.index, passing = true) }, calm = model.zeroUp && model.current == null,
```

Make it:

```kotlin
                    onHover = { if (!slot.leaving) model.select(slot.index, passing = true) }, calm = model.atRest,
```

- [ ] **Step 5: The footer, in `app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt`.** Two changes: the Labs line at the left end, and `⏎` Done at rest.

Find:

```kotlin
            val quiet = grid?.cells?.getOrNull(model.cell)?.name?.let { " $it" } ?: (r?.body as? Body.Flight)?.source?.let { " $it" }
```

Make it:

```kotlin
            // (Under first run's choices: where the two things live that need a key of the user's own.)
            val quiet = grid?.cells?.getOrNull(model.cell)?.name?.let { " $it" } ?: (r?.body as? Body.Flight)?.source?.let { " $it" }
                ?: if (model.choicesUp) " " + stringResource(R.string.first_labs) else null
```

Find:

```kotlin
            // The usual rows at rest: nothing is selected, and Down is how to get in.
```

Make it:

```kotlin
            // First run's choices at rest: Enter is "Done". With a row selected it gives way to that row's own hint.
            model.choicesUp && r == null -> "⏎" to stringResource(R.string.card_done)
            // The usual rows at rest: nothing is selected, and Down is how to get in.
```

- [ ] **Step 6: What a screen reader is told, in `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`.** Two changes: an import, and `ChoicesSaid` above the "+" of a chord.

Find:

```kotlin
import io.github.kuscher.booklight.core.FirstRun
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.FirstRun
```

Find:

```kotlin
}

/** The "+" of a chord, with 12 dp of air on either side. */
```

Make it:

```kotlin
}

/**
 * First run's choices arrive as a list with nothing selected, and its ending is the bare field with a placeholder: a
 * screen reader is told of each once (design.md §10). It draws nothing.
 */
@Composable
fun ChoicesSaid(model: OverlayModel) {
    val view = LocalView.current
    val rows = model.results.takeIf { model.choicesUp }.orEmpty().joinToString(". ") { r -> listOfNotNull(r.title, (r.body as? Body.Switch)?.word).joinToString(", ") }
    val choices = stringResource(R.string.a11y_first_choices)
    val done = stringResource(R.string.a11y_first_done)
    LaunchedEffect(model.choicesUp, model.ended) {
        if (model.choicesUp) view.announceForAccessibility("$rows. $choices") else if (model.ended) view.announceForAccessibility(done)
    }
}

/** The "+" of a chord, with 12 dp of air on either side. */
```

- [ ] **Step 7: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt app/src/main/java/io/github/kuscher/booklight/overlay/Footer.kt app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt
git commit -m "First run: the choices are a list of two rows with the panel's switch, and leaving them folds to the bare field and ends the run"
```

### Task 12: Every screen stands and is counted

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: everything Tasks 5 to 11 built behind the key's limit: `OverlayModel.due`, `stage`, `lesson`, `example`; `FirstStage` for every screen; practice; the coach line and the placeholder; the choices. Core `FirstRun.opening(s: State, screenDp: Float, plain: Boolean): State`, which counts an opening for every screen (pinned by Task 2's `aRunPastTheKeyStandsInThreeOpeningsThenWaits`).
- Produces: `OverlayModel.due` is `FirstRun.stage(settings.firstRun(), screenDp)`, without `takeIf { it.step == FirstRun.Step.KEY }`; `OverlayActivity.onCreate` counts every opening of an unfinished run; the system's words are read at an opening only where the key's step stands. From this commit on a user sees the lessons, the question and the choices.

The limit "only the key's step stands" sat in two places that must go together (`BUILD.md`, "Known, and left for a later part"): the `takeIf` that Task 5 moved from `stage` to `due`, and the condition round the count in `OverlayActivity.onCreate`. Both go here, in one commit: with only the first gone a lesson would stand in every opening and never wait; with only the second a run would be counted that shows nothing.

The manifest's comment on `INTERNET` said suggestions go on by the user's own switch in the settings. From this commit "Agree" on the question turns them on as well: the comment says so. It is a comment only: no permission and no `<queries>` line changes.

There is no unit test for this task: the app module has no test source set. Its check here is that it builds; what it switches on is Task 13's whole list.

- [ ] **Step 1: Every screen is due, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** One change.

Find:

```kotlin
     * Until the lessons, the question and the choices have their place too, only the key's step is due: a run that is past
     * it shows nothing.
     */
    val due: FirstRun.Screen? by derivedStateOf {
        if (demo || guided) null else FirstRun.stage(settings.firstRun(), screenDp)?.takeIf { it.step == FirstRun.Step.KEY }
    }
```

Make it:

```kotlin
     * Not in the Booklight window's demo, and not in a panel that was opened to have an example typed into it.
     */
    val due: FirstRun.Screen? by derivedStateOf { if (demo || guided) null else FirstRun.stage(settings.firstRun(), screenDp) }
```

- [ ] **Step 2: Every opening is counted, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Three changes, all in `onCreate`: the comment, the count, and where the system's words are read.

Find:

```kotlin
        // run; only then is the model built, which asks what stands. (Until the lessons are drawn only the key's step stands: a
        // run that is past it is not counted.)
```

Make it:

```kotlin
        // run, wherever it stopped; only then is the model built, which asks what stands.
```

Find:

```kotlin
        first(app) { if (FirstRun.screen(it)?.step == FirstRun.Step.KEY) FirstRun.opening(it, screenDp, plain) else it }
```

Make it:

```kotlin
        first(app) { FirstRun.opening(it, screenDp, plain) }
```

Find:

```kotlin
        // The system's own words for its dialog are read before the stage that quotes them comes.
        if (model.stage != null) SystemWords.load(this, app.scope)
```

Make it:

```kotlin
        // The system's own words for its dialog are read before the stage that quotes them comes: the key's.
        if (model.stage?.step == FirstRun.Step.KEY) SystemWords.load(this, app.scope)
```

- [ ] **Step 3: The manifest's comment, in `app/src/main/AndroidManifest.xml`.** One change.

Find:

```xml
         on (the user's own switch in the settings): the typed text then goes to the chosen search engine. And a
```

Make it:

```xml
         on (the user's own switch in the settings, or "Agree" to the question first run asks, alone on its
         screen and with nothing armed): the typed text then goes to the chosen search engine. And a
```

- [ ] **Step 4: The limit is gone from both places, and the manifest asks for nothing new**

```bash
grep -n "step == FirstRun.Step.KEY" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
git diff -- app/src/main/AndroidManifest.xml | grep -E "^[+-] *<(uses-permission|package|intent|queries)" || echo CLEAN
```

Expected: one line from the first command, `OverlayActivity.kt`'s `if (model.stage?.step == FirstRun.Step.KEY) SystemWords.load(this, app.scope)` (the system's words are the key's step's); then `CLEAN`.

- [ ] **Step 5: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt app/src/main/AndroidManifest.xml
git commit -m "First run: every screen stands and is counted: the lessons, the question and the choices follow the key's step"
```

### Task 13: The hooks, and the checks on a device

**Files:**
- Modify: `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`
- Modify: `bl`
- Modify: `CLAUDE.md`
- Create: `docs/research/first-run-lessons.md`

**Interfaces:**
- Consumes: `OverlayModel.due`, `example` (Task 5), `pressed` (Task 8), `coach`, `firstHint` (Task 9), `choicesUp`, `ended`, `atRest` (Task 11), `stage`, `stageArmed`, `hint`, `enter(run)`; `OverlayActivity.run(r, a)`; the `first`, `key` and `dump` cases of `DebugReceiver` as part 2 left them. Hooks that exist and need no change: `./bl debug first new|update|off|key|at SCREEN|answer NAME|ran open|search|sum` (an open panel follows at once), `./bl debug keys TEXT`, `in TEXT`, `type TEXT`, `key tab|backtab|enter|down|up|esc`, `close`, `pref suggestions on|off`, `pref zero on|off`, `pref engine ID`, `pref key seen|no`, `./bl open stay [slow=4] [opening=slow]`, `./bl shot NAME`, `./bl idle`.
- Produces: `./bl debug dump` says the placeholder first run puts in the field (`hint=`), `due=`, `coach=`, `pressed`, `choices` and `ended`; `./bl debug first` says `example=`; `./bl debug key enter2` is Enter twice in one turn; `./bl debug key tab` on the choices at rest is Down, as the key is. The list of checks for the coordinator, with a place for each answer.

**The engineer builds and does not touch a device: the coordinator runs the checks and writes the answers into the file.**

With the hooks that exist every screen of this part is reached without a new installation: `./bl debug first new`, `./bl debug first key`, `./bl debug first at l2|l3|l4|q|c`. What is new lets a check read what it cannot see (`due=` and `coach=` while a list is typed, `pressed` in the half second after a lesson's Enter) and do what adb is too slow for (Enter twice in one turn).

The file of checks is public, like the repo: it has no serial and no build number, and it never names the example's app or its letters.

There is no unit test for this task: the app module has no test source set. Its check here is that it builds.

- [ ] **Step 1: The hooks, in `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`.** Seven changes; four are a piece of one long line. In the header: `enter2` among the keys, and two lines on what is new. In the `key` case: Tab on a list at rest, and `enter2`. In `dump`: the placeholder, and what first run adds. In the `first` case: the example.

Find:

```kotlin
 *   ping | dump | type TEXT | key up|down|left|right|tab|backtab|esc|enter|stay|back|more|window | close | shot [NAME]
```

Make it:

```kotlin
 *   ping | dump | type TEXT | key up|down|left|right|tab|backtab|esc|enter|enter2|stay|back|more|window | close | shot [NAME]
```

Find:

```kotlin
 *   first answer NAME | first ran open|search|sum | first at SCREEN (core `FirstRun`: only first run's own fields, but for `key` and `answer agree`)
```

Make it:

```kotlin
 *   first answer NAME | first ran open|search|sum | first at SCREEN (core `FirstRun`: only first run's own fields, but for `key` and `answer agree`)
 *   While a lesson of first run stands, `key enter` on an app's Open and on a search inside an app is practice, as the key is: nothing opens.
 *   `key enter2` is Enter twice in one turn, as a quick double press; dump says `due=`, `coach=`, `pressed`, `choices`, `ended`.
```

Find, in one long line:

```kotlin
(m.zeroUp && m.current == null && m.chip
```

Make it:

```kotlin
(m.atRest && m.chip
```

Find:

```kotlin
                    "enter" -> if (m.stage != null) m.stageEnter()?.let { act.stage(it) } else if (m.tip != null) m.tipEnter() else m.enter { r, a -> act.run(r, a) }
```

Make it:

```kotlin
                    "enter" -> if (m.stage != null) m.stageEnter()?.let { act.stage(it) } else if (m.tip != null) m.tipEnter() else m.enter { r, a -> act.run(r, a) }
                    // Enter twice in one turn, as a quick double press does: after a lesson's Enter the second one must do nothing.
                    "enter2" -> repeat(2) { m.enter { r, a -> act.run(r, a) } }
```

Find, in one long line:

```kotlin
?: ""} hint='${m.hint.orEmpty()}' query='${m.query
```

Make it:

```kotlin
?: ""} hint='${m.hint ?: m.firstHint.orEmpty()}' query='${m.query
```

Find, in one long line:

```kotlin
it.second}" } ?: ""} zero=${if (m.zeroUp)
```

Make it:

```kotlin
it.second}" } ?: ""} due=${m.due?.name ?: "none"}${m.coach?.let { " coach=" + it.name.lowercase() } ?: ""}${if (m.pressed) " pressed" else ""}${if (m.choicesUp) " choices" else ""}${if (m.ended) " ended" else ""} zero=${if (m.zeroUp)
```

Find, in one long line:

```kotlin
"no panel"} | last start: ${OverlayActivity.lastStart.ifEmpty
```

Make it:

```kotlin
"no panel"} | example=${act?.model?.example ?: "none (no panel, or not worked out)"} | last start: ${OverlayActivity.lastStart.ifEmpty
```

- [ ] **Step 2: Say them in `bl`'s header.** One change.

Find:

```bash
#   ./bl debug first answer not_now|go_on|change_key|skip|agree|done | ran open|search|sum | at k1|k2|k3|k4|l2|l3|l4|q|c
```

Make it:

```bash
#   ./bl debug first answer not_now|go_on|change_key|skip|agree|done | ran open|search|sum | at k1|k2|k3|k4|l2|l3|l4|q|c
# While a lesson stands (l2, l3, l4), `./bl debug key enter` on an app's row is practice, as the key is: nothing opens.
# `./bl debug in TEXT` types under the chip that is there (lesson 3's word); `key enter2` is Enter twice in one turn.
# `./bl debug dump` says `due=` (the screen that is due whatever the field holds), `coach=` (the footer's coach line),
# `pressed`, `choices` and `ended`; `./bl debug first` says `example=` (what the lessons' recipes show on this device).
```

- [ ] **Step 3: Say in `CLAUDE.md` what is drawn now, how to drive it, and what to mind.** Three changes: the `FirstRun.kt` line of the layout (a piece of one long line), the first-run paragraph of the dev loop, and two gotchas after the one on hover.

Find, in one long line:

```markdown
; the key's step is drawn by `overlay/FirstStage.kt`, and the rest is
```

Make it:

```markdown
, what Enter does while a lesson stands, the coach line, the recipes and this device's example; `overlay/FirstStage.kt` draws the key's step, the three lessons and the question on one skeleton, the choices are a list of two rows, and the welcome and the show are
```

Find:

```markdown
  `first answer agree` (suggestions on). While a screen of the key's step stands, `./bl debug key tab|backtab|enter` act on it as the keys do.
  The checks for a device: `docs/research/first-run-key.md`.
```

Make it:

```markdown
  `first answer agree` (suggestions on). While a stage stands (the key's step, a lesson, the question), `./bl debug key tab|backtab|enter` act
  on it as the keys do; on a lesson's typed list `key enter` is practice, as the key is, and `key enter2` is Enter twice in one turn.
  `dump` says `first=` (the stage under the empty field), `due=` (the screen that is due whatever the field holds), `coach=` (the footer's coach
  line), `pressed`, `choices` and `ended`; `./bl debug first` says `example=` (what the lessons' recipes show on this device: never copy it
  into a file of this repo, it names an app of the device). The checks for a device: `docs/research/first-run-key.md` (the key's step) and
  `docs/research/first-run-lessons.md` (the lessons, the question, the choices, the ending).
```

Find:

```markdown
- A constant alone is not a sum (`e`, `pi`): otherwise typing "e" shows 2.718 instead of apps.
```

Make it:

```markdown
- While a lesson of first run stands (`OverlayModel.lesson`: also while its list is typed, when `stage` is null), nothing opens:
  `OverlayActivity.run`, where everything that runs passes, asks core `FirstRun.enters` before the executor. An app's Open and a
  search inside an app are practice; a sum is copied and the panel stays; 520 ms later the list gives way to what stands next
  (`lessonOver`, `OverlayModel.giveWay`), and until then `settled` keeps a second Enter from running anything. Anything that runs
  an action by another way than `OverlayActivity.run` would open things in a lesson.
- First run's choices are no stage: they are `results` with nothing selected, as the usual rows stand at rest (`choicesUp`; `atRest`
  is either). They are left, and the run is over, by Enter at rest, a typed letter, their second row, or the panel closing.
- A constant alone is not a sum (`e`, `pi`): otherwise typing "e" shows 2.718 instead of apps.
```

- [ ] **Step 4: Write `docs/research/first-run-lessons.md`**

```markdown
# First run: the lessons, the question, the choices and the ending on a device (part 3)

*The checks for part 3 of the first-run plan (`docs/superpowers/plans/2026-10-05-first-run-3-the-lessons.md`).
Whoever runs a check writes its answer under it, with the date and "the Lenovo Googlebook" or "the HP Googlebook".
Until then an answer reads "not run".*

**Before the first check**

- Choose the device by its model (`adb devices -l`), never by its place in the list; set `ANDROID_SERIAL`.
- Close the Booklight window, then `./bl install`. `./bl idle` before every `./bl open`: the panel takes the keyboard.
- Never `uiautomator dump`. No sign-in is made for a check. A capture of the screen shows real windows: none is
  kept or committed; `./bl shot NAME` is the panel's own window.
- This file is public: no serial, no build number, nothing about what is installed. The lessons' example is an app
  of the device: an answer says "the example's app" and "its letters", never the name or the letters themselves.
- Write down what `./bl debug pref` says before the first check (`suggestions=`, `zero=`, `engine=`, `opening=`)
  and put all four back after the last one. The question only stands while suggestions are off:
  `./bl debug pref suggestions off` before a check that needs it.
- `./bl debug first` prints first run's stored state, the screen that stands and, with a panel open, `example=`:
  what the lessons' recipes show on this device (`open` and `search` are the letters, `enter=true` where lesson 3
  goes by an app's row, `enter=false` where Settings stands in). Below, LETTERS stands for those letters.
- `./bl debug dump` says `first=` (the stage under the empty field, its armed answer, the counter), `due=` (the
  screen that is due whatever the field holds), `coach=`, `pressed`, `choices`, `ended`, the placeholder as
  `hint=`, the footer's word as `flash=`, and the window's size in px (divide by the screen's density for dp).
- `./bl debug keys TEXT` types into the empty field a letter at a time; `./bl debug in TEXT` types under the chip
  that is there; `./bl debug type` with nothing after it empties the field.
- A lesson is reached without the key's step: `./bl debug first new`, `./bl debug first key`, then
  `./bl debug first at l2` (or `l3`, `l4`, `q`, `c`). An open panel follows at once.
- After a lesson's Enter its list stands for half a second. With `./bl open stay slow=4` that is two seconds:
  time enough for a dump or a shot in between.

## 1. Lesson 2, as a still

`./bl debug first new`, `./bl debug first key`, `./bl debug first at l2`, `./bl open stay`, `./bl debug dump`,
`./bl shot l2`; again with `DARK=true ./bl open stay`.

In the dump: `first=L2 armed=-1 2/5 due=L2`, `hint='An app’s first letters'`, and the window 252 dp high. Look,
at four times the size: the seat's mark (the `open` symbol) on the field's mark's line; "Open an app" and its line,
the recipe's letters and the caption on one left edge (x = 72); the recipe's letters in the field's type beside
one large cap with the Enter mark, on the caps' centre line; the caption "Practice: nothing opens. Enter also puts
Booklight away."; the counter "2 of 5", the `esc` cap and "Skip" on one right edge (x = 700); "Skip" not lit, a
`tab` cap before it.

Answer: not run

## 2. Lessons 3 and 4, as stills

With the panel open: `./bl debug first at l3`, `./bl shot l3`; `./bl debug first at l4`, `./bl shot l4`.

Lesson 3: the mark is `search`; the recipe reads letters, `Tab`, Enter, a word, Enter (with `enter=false`:
letters, `Tab`, a word, Enter) and ends before the `tab` cap; the placeholder is "LETTERS, then Tab". Lesson 4:
the mark is `calc`; the recipe is `150 + 20%` and Enter; the caption ends "Today it stays."; the placeholder is
`150 + 20%`. Each at 252 dp. Going from one to the next: does anything move that should stand (the seat, the
answers' right edge)?

Answer: not run

## 3. The example leads its list

`./bl debug first` for `example=`. Then, on lesson 2, `./bl debug keys LETTERS` and `./bl debug dump`.

Is the first row the example's app, of the kind App, with `*open` armed and `search` among its actions (with
`enter=false`: is it the Settings app)? Are the letters three or four? Write "yes" or what differs, not the app.

Answer: not run

## 4. Keys on a lesson's stage

On lesson 2 with the field empty: `./bl debug key enter` (nothing: the dump is unchanged), `./bl debug key tab`
(`armed=0`, "Skip" lit), `./bl debug key enter`: `first=L3 armed=-1 3/5`, still 252 dp. `./bl debug first`:
`done=[key, show, open]` or the like, with `open` in it.

Answer: not run

## 5. Lesson 2: Enter is practice

`./bl debug first at l2`; `./bl debug keys LETTERS`; `./bl debug dump`: `due=L2 coach=opens`, and `first=none`
(the stage has given way to the list). `./bl shot l2-list`: the list stands in its real seats, row one at the
height where the seat stood; the footer's left end reads "2 of 5 · ⏎ opens it · practice: nothing opens", its
right end `tab` Actions and `esc` Close.

`./bl debug key enter`, and at once `./bl debug dump` (open the panel with `slow=4` for this): `flash=That opens
…` with the app's name, `pressed`, `due=L3`. `./bl shot l2-pressed` in the same two seconds: the armed slot a
little darker, the footer's word with its check. `./bl idle`: is the focused window still Booklight's panel, and
has no app opened? Then `./bl debug dump`: `first=L3 armed=-1 3/5`, `query=''`, the window 252 dp.

Answer: not run

## 6. Lesson 3, from the letters to the word

`./bl debug first at l3`; `./bl debug keys LETTERS`; dump: `coach=to_search`. `./bl debug key tab`:
`armed=search`, `coach=into_app`. `./bl debug key enter`: `chip=appsearch:…:search`, `hint='Search …'`,
`due=L3`, `first=none`; nothing has opened. `./bl debug in lofi`; dump: `coach=searches`; the footer reads "3 of 5 ·
⏎ searches there · practice: nothing opens". `./bl debug key enter`: `flash=That searches …`; `./bl idle`: no app
has opened. A second later: `first=L4 armed=-1 4/5`, no chip.

Where `example=` says `enter=false`: `./bl debug keys s`, `./bl debug key tab` (the Settings chip),
`./bl debug in wifi`, dump: `coach=opens`; `./bl debug key enter`: `flash=That opens …`, and Settings has not
opened.

Answer: not run

## 7. Lesson 4: the sum is copied, and the panel stays

`./bl debug first at l4`, `./bl debug pref suggestions off`; `./bl debug keys 150 + 20%`; dump: `coach=copies`,
the row's answer 180. `./bl debug key enter`: `flash=Copied`, `pressed`. A second later: the panel is still
there; `first=Q armed=-1 5/5`; the window 324 dp. Ctrl + V in the field: 180. Filmed or stepped with
`./bl open stay slow=4`: does the lower edge go from the list's height straight to 324, without a visit to 68?

Answer: not run

## 8. Enter twice

`./bl debug first new`, `./bl debug first key`, `./bl debug first at l2`, `./bl debug first ran search`,
`./bl debug first ran sum` (lesson 2 is the last lesson left), `./bl debug pref suggestions off`; open;
`./bl debug keys LETTERS`; `./bl debug key enter2`.

`./bl idle`: no app has opened (the second Enter found the lesson over and its list still standing, and did
nothing). A second later: `first=Q`.

Answer: not run

## 9. Typed on before the list gives way

`./bl debug first at l2`; `./bl open stay slow=4` (the half second is two seconds then); `./bl debug keys
LETTERS`; `./bl debug key enter`, and within those two seconds `./bl debug in x`.

Three seconds later: the field still holds the x, with its list (nobody emptied it under the typing hand);
`due=L3`. `./bl debug type` with nothing after it: lesson 3's stage stands.

Answer: not run

## 10. A lesson's thing out of turn

`./bl debug first at l2`; `./bl debug keys 150 + 20%`; dump: `coach=title` (the footer reads "2 of 5 · Open an
app"). `./bl debug key enter`: `flash=Copied`; a second later lesson 2 stands again, and `./bl debug first` has
`sum` in `done=`. After lessons 2 and 3 the question follows, or the choices: lesson 4 is not asked for.

Answer: not run

## 11. Everything else runs as every day

On lesson 2: `./bl debug keys wifi`, `./bl debug key down` to the settings page if it is not row one,
`./bl debug key enter`. The page opens and the panel goes, as every day. (This opens a window of another app:
run it where that is allowed, and say who closed the window.) At the next opening lesson 2 stands again, and
`./bl debug first` says `opens=` one higher.

Answer: not run

## 12. The question, as a still, and its keys

`./bl debug pref suggestions off`, `./bl debug first at q`, `./bl open stay`, `./bl debug dump`, `./bl shot q`;
again dark.

`first=Q armed=-1 5/5`, 324 dp. Look: the `globe` mark; the title alone in the seat, on its centre line; the
text from x = 72 to the right margin, in strong ink, whole; the note under it in second ink; "Agree" and "Not
now" at the right edge, neither lit, a `tab` cap before them. `./bl debug key enter`: nothing, and
`./bl debug pref` still says `suggestions=false`. `./bl debug key tab`: `armed=0`; again: `armed=1`;
`./bl debug key backtab`: `armed=0`. `./bl debug close`, open again: the question stands as it did, nothing lit,
`suggestions=false`.

Answer: not run

## 13. Agree, and Not now

On the question: `./bl debug key tab`, `./bl debug key enter`. `./bl debug pref`: `suggestions=true`. The dump:
`choices`, two rows, the window 268 dp. Then `./bl debug pref suggestions off`, `./bl debug first at q`,
`./bl debug key tab` twice, `./bl debug key enter`: `suggestions=false`, and the choices stand all the same.

Answer: not run

## 14. No answer is given blind

`./bl debug pref suggestions off`, `./bl debug first at q`, no panel open. `./bl open stay opening=slow slow=4`
and at once, while the glass is still opening, `./bl debug key tab` and `./bl debug key enter`.

Once it has opened: `first=Q armed=-1`, and `./bl debug pref` says `suggestions=false`. The same on lesson 2
(`first=L2 armed=-1`, not skipped) and on K1 (`./bl debug first new`, `./bl debug pref key no`: no dialog comes).

Answer: not run

## 15. The question is never cut

Booklight in German; the search engine with the longest name (`./bl debug pref engine ID`, then put it back);
`./bl shot q-de`. How many lines has the text: three, four? Is its last word there, and is the note on two lines
at most? If the dump's window is taller than 324 dp: by how much, and is the text then whole?

Answer: not run

## 16. The choices, as a still

`./bl debug first at c`, `./bl open stay`, `./bl debug dump`, `./bl shot c`; again dark.

`choices`, `selected=-1`, `first=none due=C`, 268 dp; the rows `Show your usual (…) [OTHER] {off} <*usual>` and
`Everything Booklight does (…) [?] <*enter>`. Look: row tops at 76 and 168; marks on x = 38, titles on 72; the
first row's text on two lines; at its right end "Off" and the switch, the switch ending at x = 700; at the second
row's right end a `?` cap; no row for the device's model; the footer's left end "Play by name and flight times
need your own key: window › Labs", its right end `⏎` Done and `esc` Close.

Answer: not run

## 17. The choices' keys

`./bl debug key down`: `selected=0`, the pill on row one. `./bl debug key enter`: `{on}`, the thumb at the
right, "On"; `./bl debug pref` says `zero=true`; the panel stays. `./bl debug key enter` again: off. (Leave the
switch as the device had it.) `./bl debug key up`: `selected=-1`; again: nothing. `./bl debug key tab`:
`selected=0`. `./bl debug key down`: row two, the footer `tab` Fill in. `./bl debug key enter`: `chip=help`, the
list of everything; `./bl debug first`: `run=NONE mark=true`.

Answer: not run

## 18. The fold

`./bl debug first at c`, open, a moment, `./bl debug key enter`.

The rows go and the window is 68 dp; `ended`; `hint='Type ? for everything Booklight does'`; the field's `esc`
cap is back. `./bl debug first`: `run=NONE done=[] mark=true`. Close, open: the ordinary placeholder, and
whatever stood under the empty field before first run stands there again. Filmed or stepped with `slow=4`: does
the height go from 268 to 68 in one move?

Answer: not run

## 19. The choices are done however they are left

Each from `./bl debug first at c` with the panel open: `./bl debug keys a` (the typed list takes their place);
`./bl debug key esc`; `./bl debug close`. After each, `./bl debug first`: `run=NONE`.

Answer: not run

## 20. One opening, from "Your key works" to the bare field

`./bl debug first new`, `./bl debug pref key no`, `./bl debug pref suggestions off`; open; `./bl debug first
key` ("Your key works"); then with the hooks, or by hand: Go on, the three lessons each done, an answer to the
question, Enter on the choices.

Did the panel stay open from the first screen to the last? `./bl debug first` on the way: `opens=1` throughout.
Did any height change look like a jump, or cut a row or a line at the lower edge? By hand, with a stopwatch,
from "Go on" to the bare field: how many seconds (the product paper's target for the whole of first run is 60)?

Answer: not run

## 21. Three openings, then the run waits

`./bl debug first at l3`; `./bl open`, `./bl debug dump`, `./bl debug close`, three times: lesson 3 each time.
A fourth: the bare field. `./bl debug first`: `opens=4 parked=true`, `done=` as it was, `suggestions=` as it was.

Answer: not run

## 22. Heights, screen by screen

From the dump's window size, in dp: K4 252, each lesson 252, a lesson's list as any list of those rows, the
question 324 (or more, check 15), the choices 268, the ending 68. In `./bl shot` of each: is anything cut at the
lower edge, or is there glass with nothing on it under the last line?

Answer: not run

## 23. German

Booklight in German, every screen of this part. The longest lines: lesson 3's line under its title; the caption
„Zum Üben: nichts öffnet sich. Sonst schließt Enter auch Booklight.“ beside „Überspringen“; the coach line „4
von 5 · ⏎ kopiert das Ergebnis · Booklight bleibt nur heute“ beside `tab` Aktionen and `esc` Schließen (does the
counter go where the line has no room?); the question's note; the choices' two lines of text; the Labs line
beside `⏎` Fertig. Is anything cut in the middle of a word?

Answer: not run

## 24. Animations off, and a screen reader

With the system's animations off: every screen whole in one frame; a lesson's list still gives way half a
second after its Enter; the switch and its word change in one frame. With a screen reader on: each stage said
once when it comes (the counter, the title, the line, the recipe a piece at a time, the caption, how to act);
the coach line when it changes; the choices with each row's state; "First steps are done" at the fold.

Answer: not run

## 25. Nobody else sees a change

`./bl debug first off`, `./bl debug pref key seen`; open; `./bl debug keys 150 + 20%`; dump: `due=none`, no
`coach=`; `./bl debug key enter`: "Copied", and the panel closes, as every day. Ten openings by the keys: the
placeholder, the tips, the copy's line and "your usual" as before; `./bl debug pref` before and after says the
same but for what was set here.

Answer: not run

## 26. The HP Googlebook, with Alex

Checks 1, 5, 6, 7, 12, 16, 18 and 20 again there.

Answer: not run
```

- [ ] **Step 5: The file of checks holds nothing of a device**

```bash
grep -c "^Answer: not run$" docs/research/first-run-lessons.md
grep -nE "com\.[a-z]+\.|[0-9A-Z]{8,}|build [0-9]|emulator-" docs/research/first-run-lessons.md || echo CLEAN
```

Expected: `26`, then `CLEAN` (no package name, no serial, no build number).

- [ ] **Step 6: Run every core test, and build**

```bash
./bl test
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: no output from `./bl test` and exit status 0; then the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 7: Commit**

```bash
git add app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt bl CLAUDE.md docs/research/first-run-lessons.md
git commit -m "First run: the hooks for the lessons, the question and the choices, and the checks to run on a device"
```

- [ ] **Step 8: Hand over.** Report to the coordinator: the commit, that the debug APK is at `app/build/outputs/apk/debug/app-debug.apk`, and that the twenty-six checks are in `docs/research/first-run-lessons.md`. Do not install it and do not run a check.

### Task 14: Part 3, checked as a whole

**Files:** none are changed. If a step shows a change that is not committed, it belongs to the task that made it: say which, and stop.

**Interfaces:**
- Consumes: everything above.
- Produces: the report that part 3 is whole.

- [ ] **Step 1: Every core test, with its count**

```bash
./gradlew :core:test --console=plain && ./bl test && for f in core/build/test-results/test/TEST-io.github.kuscher.booklight.core.FirstRun*.xml core/build/test-results/test/TEST-io.github.kuscher.booklight.core.UnderTest.xml; do n=${f##*core.}; echo "${n%.xml} $(grep -o 'tests="[0-9]*"' "$f" | head -1) $(grep -o 'failures="[0-9]*"' "$f" | head -1) $(grep -o 'errors="[0-9]*"' "$f" | head -1)"; done
```

Expected: `BUILD SUCCESSFUL`, no output from `./bl test`, then twelve lines, each ending `failures="0" errors="0"`:

```text
FirstRunAnswersTest tests="14"
FirstRunGateTest tests="7"
FirstRunHoldTest tests="12"
FirstRunKeyStepTest tests="8"
FirstRunLessonsTest tests="10"
FirstRunOpeningsTest tests="9"
FirstRunRecipeTest tests="11"
FirstRunStageTest tests="9"
FirstRunStartTest tests="10"
FirstRunTest tests="13"
FirstRunUpdateTest tests="10"
UnderTest tests="7"
```

113 first-run tests in eleven classes, 20 of them this part's (`FirstRunRecipeTest` and `FirstRunStageTest`), and `UnderTest`'s seven.

- [ ] **Step 2: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 3: What must be gone is gone**

```bash
grep -rn "What\.CARD\|card: Boolean\|card = true" core/src || echo CLEAN
grep -n "takeIf { it.step == FirstRun.Step.KEY }\|FirstRun.screen(it)?.step == FirstRun.Step.KEY" app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt || echo CLEAN
grep -rn "Aktionstaste\|Vorschlag:" app/src/main/res/values-de/strings_32.xml || echo CLEAN
```

Expected: three times `CLEAN` (the cards' name in core; the key's limit in the model and round the activity's count; the two German wordings).

- [ ] **Step 4: The rules hold**

```bash
grep -rn "^import android" core/src/main || echo CLEAN
grep -rn "withFirstRun(" app/src | grep -v "fun Settings.withFirstRun\|fun Prefs.firstRun\|fun Settings.asUpdate\|^app/src/debug" || echo CLEAN
grep -rn "suggestions = true" app/src/main/java core/src/main | grep -v "core/FirstRun.kt" || echo CLEAN
grep -rn "executor.run(" app/src/main/java/io/github/kuscher/booklight/overlay | grep -v "OverlayActivity.kt.*app.executor.run(a.effect, this)\|OverlayModel.kt.*app.executor.run(if (dir < 0) n.down else n.up)" || echo CLEAN
git diff f17ebbe -- app/src/main/AndroidManifest.xml app/src/debug/AndroidManifest.xml | grep -E "^[+-] *<(uses-permission|package|intent|queries|action|category|data)" || echo CLEAN
git log f17ebbe..HEAD --format=%B | grep -inE "co-authored-by|claude|generated with|session" || echo CLEAN
grep -rnE "emulator-|ANDROID_SERIAL=[A-Z0-9]" docs/research/first-run-lessons.md || echo CLEAN
git log --oneline f17ebbe..HEAD | wc -l
git status --short
```

Expected: seven times `CLEAN` (no Android in core; no write of first run's state in the app but through `Prefs.firstRun`; suggestions switched on in core's `answer` alone; no way to the executor from the panel but `OverlayActivity.run`'s and a level's nudge; no new permission and no `<queries>` line in a manifest; no attribution in the commits of this part; no serial in the file of checks), then the number of commits since `f17ebbe` (`13`, and one more for each fix a review asked for), and no line from `git status`.

- [ ] **Step 5: Report.** The commits of Tasks 1 to 13 by their first lines, the results above, that the branch was not pushed, and that the twenty-six device checks in `docs/research/first-run-lessons.md` are all "not run".

---

## The parts that follow

| Part | What it builds | What it needs from this part |
| --- | --- | --- |
| **4. The welcome and the show** | The welcome in a 468 dp glass ("Lights on", the plain shaft, the words A), the performed show with the example flight, the very first opening at Slow, the landing in K1 | `OverlayModel.due` and `stage`, so that the opening piece can ask what it lands in; the order of `OverlayActivity.onCreate` (the key, the count, the model, then `take(intent)` and `offerChoices()`); `FirstStage`'s seat and band to land in; `BooklightApp.firstExample()` and the apps it waits for, for the show's first beat |
| **5. Motion, the replay, the documents, the device pass** | `motion.md`'s transitions: T5 (a lesson set down, the recipe written a letter at a time), T7 (typing in a lesson), T8 (the sum, the question rising), T9 (the question to the choices, the switch), T10 (the fold, with the lap of light); the stage's answers drawn pressed; the reveal's lap. "First steps" as a command and as a row on the window's Start page, with `first_later_hint`, the word "Not now" leaves on the key's step. Action + Quick Insert on the Start page and in the README. The documents: `store-submission/forms/data-safety.md` takes the question's words in place of the old card's, `PRIVACY.md`, `ux-model.md`, `design-system.md`, `PICKING-UP.md`, `CHANGELOG.md`. Every open device check | `FirstStage.kt`'s parts as the places motion acts on (`Seat`, `Band`, `Recipe`, `AskBand`, `BigCap`); `OverlayModel.pressed`, `giveWay()` and `OverlayActivity.lessonOver` as T8's trigger; `choicesUp`, `ended` and `fold()` as T9's and T10's; `FirstRun.answers` with `CHANGE_KEY` for a replay (drawn since part 2); `FirstRun.again` and `replay` (part 1) for "First steps"; `docs/research/first-run-lessons.md`'s answers |
