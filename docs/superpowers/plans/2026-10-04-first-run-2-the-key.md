# First run, part 2: the key. Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A new installation, and one that was there before and has no key, is asked for its key under the empty field, is led through the system's Keyboard shortcuts dialog with the panel alive behind it, and sees "Your key works" the moment its own keys uncover the panel.

**Architecture:** Core `FirstRun` gains everything the key's step decides: what stands on a screen of a given height and in which order a panel asks, the answers and which is armed, the caption, the words to quote, what a signal makes of the hold, what counts as the key, the icon's first click. The app draws it in one new file, `overlay/FirstStage.kt`, where the two old cards stood, reads the system's own words in `device/SystemWords.kt`, and keeps the hold, the dialog's rows and the key's landing in `OverlayActivity`. Every write of first run's state goes through `Prefs.firstRun { … }`.

**Tech Stack:** Kotlin 2.4, JUnit 4 in `core/` (plain JVM, no `android.*`), Jetpack Compose in `app/` (compile SDK 37, min SDK 34), the repo's `./bl` helper over adb.

**Spec:**
- `docs/design/first-run/00-brief.md`, **"Settled for the build"**: it wins over every other paper.
- `docs/design/first-run/design.md` §4 (the skeleton, the large key cap, "your key"), §5 (K1 to K4, the dialog's rows, coming back), §9, §10 (states), §11 (the icon).
- `docs/research/first-run-proof.md`: what the device answered, and its last section, "What the answers decided".
- `docs/design/first-run/eng.md` §4 and §12.3; `docs/superpowers/plans/2026-10-04-first-run.md` (part 1: what exists).

**Who sees what in a build of this part alone.** A new installation, and an installation that was there before and has no key: the key's step (K1 to K4) under the empty field. After "Go on" or "Not now": the bare field, as every day; tips, the copy's line and "your usual" follow as they always did. The two old cards are gone for everyone. The lessons, the question and the choices are not drawn until part 3 (a new installation's run waits at its first lesson, uncounted, and part 3 takes it up there); there is no welcome and no show until part 4; "First steps" as a command and as a row in the window comes with part 5, so "Not now" leaves no word about where first run waits. Nobody is asked about search suggestions by this build: the switch is in the window. Someone with a key and no run sees no change but the cards' absence.

**Decided here, where the papers differ or are silent:**
1. *What counts as the key* is the positive sign alone: a start of the launcher activity by the system itself (or by nobody who is named) with no icon's place and nothing carried, or the assistant key. Today's list of senders who are *not* a key goes: none of them is the system.
2. *An opening is counted when its panel is made*, before the model is built, not when the stage comes at the gate (the review's ruling: `keyLanded`, then `opening`, then what stands). A panel typed into at once is counted too.
3. *The suggested key is chosen when the stage is set down and again at the Enter that asks for the dialog*, for the keyboard that Enter came from. It never changes while the caps are in view.
4. *The cap shows the system's name for the key only where it fits*; else Booklight's own word. The caption and the dialog's rows always use the system's name.
5. *No line in `<queries>`*: the system UI's package was visible without one. The proof's line in the debug manifest goes with the proof; a device check reads the words without it.
6. *A held panel goes when another window has had the front for 300 ms* (the key's own start has it for about 3 ms), and the hold gives up 1 s after asking if no dialog came.
7. *`keyLanded` brings a waiting run back only for a first key* (the review's optional point 5, taken).
8. *The cards' fields stay in `Settings`* (`shortcutCard`, `suggestionsCard`): builds from before first run read them. Nothing here reads them any more. Of their strings, `card_shortcut_text`, `card_suggest_title`, `card_suggest_text` and `card_suggest_on` go; `card_shortcut_title`, `card_shortcut_action` and `card_not_now` are the stage's; `card_done` waits for part 3's last screen.

## Global Constraints

- Branch `first-run`. Local only: never pushed, no tag, no release.
- No attribution lines of any kind in commits or code: no `Co-Authored-By`, no Claude lines, no session links.
- The repo is public: no device serials, no adb names, no build numbers, nothing about what is installed on a device, in any file or commit message.
- `core/` has no `android.*` import.
- Every user-facing string is a resource, in English (`res/values`) and German (`res/values-de`). In German a quotation opens with „ (U+201E) and closes with “ (U+201C); an English apostrophe is ’ (U+2019).
- No new permission, and no new line in `<queries>`.
- Comments in the surrounding code's own style and density: plain sentences that say why; a KDoc where the neighbours have one.
- Never run `uiautomator dump`. A device is chosen by its model (`adb devices -l`), never by its place in the list. Only the coordinator touches a device: the engineer builds.
- **Every write of first run's state goes through `Prefs.firstRun { … }`** (`data/Prefs.kt`): the change is worked out inside the settings' own update, never from a state read before it. `Settings.withFirstRun` also writes `keySeen` and `suggestions`, the consent switch.
- **The order for every panel** is: the key landing (if the key made the panel), then the opening counted, then what stands. Nothing asks `FirstRun.stage` or `FirstRun.overture` before `FirstRun.opening` was applied.
- The suggested key is Action + Quick Insert; a keyboard without that key is offered Action + M (not J).
- Nothing opens in first run; Booklight never starts itself again over another window; first run asks the system nothing about its on-device model.
- Motion is part 5: this part draws stills. Where the design says a thing rolls or fades and a helper for it exists (`Motion.roll()`, `Motion.fade()`, `DrawnCheck`), the helper is used; nothing new is animated.
- The panel's rules hold: the window is exactly the panel, one fixed height for each screen in `Metrics`, nothing outside the glass, the field is never covered and typing never waits.

**The commands of this repo, as every task uses them**
- All core tests: `./bl test` (quiet: no output and exit status 0 mean every test passed).
- One test class: `./gradlew :core:test --tests 'io.github.kuscher.booklight.core.SomeTest' --console=plain`.
- A debug build with its check: `./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT` (the build is good only if this prints the one word `BUILT`).

**Where a late fix to part 1 would touch this plan.** It is written on `first-run` as the review's fix wave left it (`Prefs.firstRun` exists; every proof line in `OverlayActivity.kt` carries `THE PROOF`). Every "Find" below stands in its file exactly once there. If a later fix changes one of these places, the task that finds it must read the place again: `OverlayActivity.kt` (`onCreate`, `onNewIntent`, `fromIcon`, `byKey`, `onWindowFocusChanged`, `card`, the companion), `OverlayModel.kt` (the constructor, `card`, `stage`, `offerTip`, `offerZero`, `offerCopy`, `bare`), `Panel.kt` (`keys`, the body, `CardBody`), `Metrics.kt` (`height`), `DebugReceiver.kt` (the `first` and `key` cases, `dump`, `pref`), `FirstRun.kt` (`Answer`, `keyLanded`, the file's last line), and the last line of both `strings_32.xml`.

## Review Focus

Five things the papers and the device's answers imply that would bite a real user and that nothing would exercise unless a test is written for it. Each has its test in the task that owns the code.

| # | The input or condition | What must hold | Its test |
| --- | --- | --- | --- |
| 1 | A fourth opening of an unfinished run; a screen under 475 dp | Neither a screen nor the opening piece comes, and nothing is counted on a screen too low. Asked in the wrong order, the piece would still begin | Task 1: `atTheFourthOpeningNeitherAScreenNorTheOpeningPieceComes`, `askedBeforeItIsCountedTheFourthOpeningWouldStillBegin`, `onAScreenTooLowNothingStandsAndNothingIsCounted` |
| 2 | Under the dialog the panel is told only that it lost the focus; then another window takes the front for good, or the system shows no dialog | The panel stays while the dialog is over it and never lingers after it: it goes when the front is gone for good, and a lost focus closes it as every day once the hold is over | Task 5: `theDialogComesAndThePanelStays`, `theKeyLandsUnderTheDialog`, `anotherWindowTakesTheFrontForGood`, `theSystemShowsNoDialog`, `withoutAHoldALostFocusClosesThePanelAsEveryDay` |
| 3 | A start that is not the key: the icon, "Open" in the store or in Settings, another app, a start that carries text | It is never greeted with "Your key works" and never makes Booklight believe it has a key | Task 5: `aClickOnTheIconIsNoKey`, `openInTheStoreAndAnotherAppsStartAreNoKey`, `aStartByTheSystemItselfIsTheKey` |
| 4 | The system's words cannot be read, a name is gone in another build, a word is empty or is no button's name | Booklight's own word stands in, each word by itself | Task 4: `whereAWordCannotBeHadBooklightsOwnStandsIn`, `theSystemsOwnWordsAreQuotedWhereTheyWereRead` |
| 5 | A keyboard without Quick Insert | Action + M on the caps; no word about where a key is; after it no other key to try. (No device has shown such a keyboard yet: Task 10's check 12 comes before this is trusted) | Task 4: `onlyQuickInsertNeedsSayingWhereItIs`, `theOtherKeysToTryAreNamedOnlyAfterQuickInsert` |

Also pinned: Enter does nothing where no answer is armed (Task 4, `enterHasAnAnswerButWhereTheKeyIsTheThingToPress`); the icon's first click shows the panel once, only for a new installation that still asks for its key, and only where the key's step will stand in the panel that click makes (asked after the opening is counted; Task 5, `theIconsFirstClickShowsThePanelOnce`, `everyOtherClickOnTheIconOpensTheWindow`). No JVM test reaches `Settings.withFirstRun` or `asUpdate`: Task 10's check 16 keeps `./bl debug first older` on a device.

---

### Task 1: The gate: what stands, and in which order a panel asks

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunGateTest.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`

**Interfaces:**
- Consumes (core, part 1, in `object FirstRun`): `State`, `Run`, `Step`, `Screen`, `Overture`, `SHOWN`, `CHANGE`, `MAX_OPENS`, `fits(screenDp: Float): Boolean`, `screen(s: State): Screen?`, `opened(s: State): State`, `parked(s: State): Boolean`, `overture(s, motion, reader, plain, screenDp): Overture`, `keyLanded(s: State): State`. App: `fun Prefs.firstRun(change: (FirstRun.State) -> FirstRun.State)` and `fun Settings.firstRun(): FirstRun.State` (both in package `io.github.kuscher.booklight.data`, one import: `io.github.kuscher.booklight.data.firstRun`); `OverlayModel.stage` (part 1's derived value); `OverlayActivity.byKey(intent)`.
- Produces: `fun stage(s: State, screenDp: Float): Screen?` and `fun opening(s: State, screenDp: Float): State` in `object FirstRun`; `keyLanded` resets `opens` only for a first key. `OverlayModel` takes `screenDp: Float = Float.MAX_VALUE` as its last constructor parameter and its `stage` is `FirstRun.stage(…)`. In `OverlayActivity`: `private fun first(app: BooklightApp, change: (FirstRun.State) -> FirstRun.State)` (a change written only where it changes something), and `onCreate` applies the key and the opening before it builds the model.

Until now `OverlayModel.stage` named a screen on a display too low for it, and nothing counted an opening: asked on a fourth opening, `FirstRun.overture` would still say the piece plays, and it would end on an empty field. This task makes one order for every panel and pins it. Nothing is drawn yet.

- [ ] **Step 1: Write the failing test** `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunGateTest.kt`

```kotlin
package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Overture
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Screen
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunGateTest {
    private fun begins(s: State, screenDp: Float = 1200f) = FirstRun.overture(s, motion = true, reader = false, plain = true, screenDp = screenDp)

    /** The order for each panel: the opening is counted, then it is asked what stands and how it begins. */
    @Test fun atTheFourthOpeningNeitherAScreenNorTheOpeningPieceComes() {
        var s = State(Run.NEW)
        repeat(FirstRun.MAX_OPENS) {
            s = FirstRun.opening(s, 1200f)
            assertEquals(Screen.K1, FirstRun.stage(s, 1200f))
            assertEquals(Overture.WELCOME, begins(s))
        }
        s = FirstRun.opening(s, 1200f)
        assertNull(FirstRun.stage(s, 1200f))
        assertEquals(Overture.NONE, begins(s))
    }

    /** Why the order matters: asked before it is counted, the fourth opening still says the piece plays, and nothing would stand after it. */
    @Test fun askedBeforeItIsCountedTheFourthOpeningWouldStillBegin() {
        val third = State(Run.NEW, opens = FirstRun.MAX_OPENS)
        assertEquals(Overture.WELCOME, begins(third))
        assertEquals(Overture.NONE, begins(FirstRun.opening(third, 1200f)))
    }

    @Test fun onAScreenTooLowNothingStandsAndNothingIsCounted() {
        assertNull(FirstRun.stage(State(Run.NEW), 474f))
        assertEquals(State(Run.NEW), FirstRun.opening(State(Run.NEW), 474f))
        assertEquals(Screen.K1, FirstRun.stage(State(Run.NEW), 475f))
        assertEquals(1, FirstRun.opening(State(Run.NEW), 475f).opens)
    }

    @Test fun whereThereIsNoRunThePanelIsNotCounted() {
        assertEquals(State(key = true), FirstRun.opening(State(key = true), 1200f))
        assertNull(FirstRun.stage(State(key = true), 1200f))
    }

    /** The key first, then the count: the first press of the user's own key brings a run back that waited for it, and that opening is its first again. */
    @Test fun theKeyThatMadeThePanelIsLandedBeforeTheOpeningIsCounted() {
        val waiting = State(Run.NEW, helper = 1, opens = FirstRun.MAX_OPENS + 1)
        val s = FirstRun.opening(FirstRun.keyLanded(waiting), 1200f)
        assertEquals(Screen.K4, FirstRun.stage(s, 1200f))
        assertEquals(1, s.opens)
    }

    /** A replay that had asked for another key and then waited is not brought back by a key that was known: only a first key is news. */
    @Test fun aKeyThatWasKnownDoesNotBringAWaitingRunBack() {
        val waiting = State(Run.REPLAY, done = listOf(FirstRun.SHOWN, FirstRun.CHANGE), helper = 1, opens = FirstRun.MAX_OPENS + 1, key = true)
        val s = FirstRun.keyLanded(waiting)
        assertTrue(FirstRun.parked(s))
        assertTrue(FirstRun.CHANGE !in s.done)
    }
}
```

- [ ] **Step 2: Run it and see it fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunGateTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'opening'` among its `e:` lines.

- [ ] **Step 3: Write the implementation.** In `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`, directly above the file's last line (the `}` that closes `object FirstRun`), add, with one empty line before it:

```kotlin
    // ---- the gate

    /** What stands in the panel on a screen [screenDp] high: [screen], or nothing where first run does not fit. */
    fun stage(s: State, screenDp: Float): Screen? = if (fits(screenDp)) screen(s) else null

    /**
     * A panel is made. An unfinished run stands in [MAX_OPENS] openings, so this one is counted here,
     * before anything asks what stands or how the opening begins: asked first, a fourth opening would
     * still begin with the opening piece and end on an empty field. The order for every panel is
     * [keyLanded] (where the key made it), then this, then [overture] and [stage]. On a screen too
     * low for first run nothing is counted.
     */
    fun opening(s: State, screenDp: Float): State = if (fits(screenDp)) opened(s) else s
```

And a first key alone brings a waiting run back. In the same file:

Find:

```kotlin
    /**
     * The key opened the panel, or uncovered it under the system's dialog. A key that was known
     * already changes nothing, unless the run was asking for another one. A run that waited comes
     * back: the first press of the user's own key is what first run is for.
     */
    fun keyLanded(s: State): State {
        if (s.key && CHANGE !in s.done) return s
        val t = s.copy(key = true)
        if (s.run == Run.NONE || over(s, Step.KEY)) return t
        return t.copy(done = t.done - CHANGE, helper = 0, opens = 0)
    }
```

Make it:

```kotlin
    /**
     * The key opened the panel, or uncovered it under the system's dialog. A key that was known
     * already changes nothing, unless the run was asking for another one. A run that waited for its
     * first key comes back: the first press of the user's own key is what first run is for. (A key
     * that was known is no such news: a replay that asked for another one and then waited goes on waiting.)
     */
    fun keyLanded(s: State): State {
        if (s.key && CHANGE !in s.done) return s
        val t = s.copy(key = true)
        if (s.run == Run.NONE || over(s, Step.KEY)) return t
        return t.copy(done = t.done - CHANGE, helper = 0, opens = if (s.key) s.opens else 0)
    }
```

- [ ] **Step 4: Run it and see it pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunGateTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: The model asks the gate.** In `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`, two changes; each "Find" stands in the file exactly once.

Find:

```kotlin
    private val demo: Boolean = false,
) {
```

Make it:

```kotlin
    private val demo: Boolean = false,
    /** The screen's height in dp: on a screen too low for it first run does not stand in the panel (core `FirstRun.fits`). */
    private val screenDp: Float = Float.MAX_VALUE,
) {
```

Find:

```kotlin
if (demo || guided || query.isNotEmpty() || chip != null) null else FirstRun.screen(settings.firstRun()) }
```

Make it:

```kotlin
if (demo || guided || query.isNotEmpty() || chip != null) null else FirstRun.stage(settings.firstRun(), screenDp) }
```

- [ ] **Step 6: The panel's order, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Five changes; each "Find" stands in the file exactly once.

*The import of first run.* Find:

```kotlin
import io.github.kuscher.booklight.core.Effect
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.FirstRun
```

*The import of its state in the settings.* Find:

```kotlin
import io.github.kuscher.booklight.core.Result
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.data.firstRun
```

*The key, then the opening, then the model.* Find:

```kotlin
        val screen = windowManager.maximumWindowMetrics.bounds
        model = OverlayModel(app, lifecycleScope, limit = Metrics.maxRows(screen.height() / resources.displayMetrics.density))
```

Make it:

```kotlin
        val screenDp = windowManager.maximumWindowMetrics.bounds.height() / resources.displayMetrics.density
        // First run, in this order for every panel (core `FirstRun.opening`): the key, if it made this panel (a keyboard shortcut
        // starts the launcher activity; the assistant key asks for assistance); then this opening is counted for an unfinished
        // run; only then is the model built, which asks what stands. (Until the lessons are drawn only the key's step stands: a
        // run that is past it is not counted.)
        if (byKey(intent)) first(app) { FirstRun.keyLanded(it) }
        first(app) { if (FirstRun.screen(it)?.step == FirstRun.Step.KEY) FirstRun.opening(it, screenDp) else it }
        model = OverlayModel(app, lifecycleScope, limit = Metrics.maxRows(screenDp), screenDp = screenDp)
```

*The key is no longer written further down.* Delete (it stands in the file exactly once):

```kotlin
        // Opened the way a key opens it (a keyboard shortcut starts the launcher activity; the assistant key asks for
        // assistance): Booklight has a key, and need not ask for one.
        if (!settings.keySeen && byKey(intent)) app.prefs.update { it.copy(keySeen = true) }
```

*A change that is written only where it is news.* Find:

```kotlin
    private fun dp(v: Float) = v * resources.displayMetrics.density
```

Make it:

```kotlin
    private fun dp(v: Float) = v * resources.displayMetrics.density

    /**
     * A change of first run's state, worked out inside the settings' own update (`Prefs.firstRun`), and written only where
     * it changes something: on every day but the first few this costs no write.
     */
    private fun first(app: BooklightApp, change: (FirstRun.State) -> FirstRun.State) {
        val was = app.prefs.now.firstRun()
        if (change(was) != was) app.prefs.firstRun(change)
    }
```

- [ ] **Step 7: Run every core test**

```bash
./bl test
```

Expected: no output, exit status 0.

- [ ] **Step 8: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 9: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunGateTest.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
git commit -m "First run: one order for every panel (the key, the opening counted, then what stands), and nothing on a screen too low"
```

### Task 2: The proof goes

**Files:**
- Delete: `app/src/main/java/io/github/kuscher/booklight/overlay/Proof.kt`, `app/src/main/java/io/github/kuscher/booklight/overlay/ProofWords.kt`, `app/src/main/java/io/github/kuscher/booklight/overlay/ProofGlass.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`, `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt`, `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`, `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`, `app/src/debug/AndroidManifest.xml`, `bl`

**Interfaces:**
- Consumes: nothing of the proof is used by a later task. What it found is in `docs/research/first-run-proof.md`, and a copy of its debug build is kept outside git for the checks that are still open.
- Produces: a tree without `THE PROOF`. `OverlayActivity` no longer overrides `onProvideKeyboardShortcuts`, `onTopResumedActivityChanged`, `onPause` or `onResume` (Task 8 brings the first two back for real), and its debug flag `stay` again lets a new start close a held panel.

There is no unit test for this task: the app module has no test source set (`app/src` holds `main` and `debug`). What it decides is core's and is tested there; its check here is that it builds, and what it does is seen in nothing: it only takes away.

- [ ] **Step 1: Delete the three files**

```bash
git rm -q app/src/main/java/io/github/kuscher/booklight/overlay/Proof.kt app/src/main/java/io/github/kuscher/booklight/overlay/ProofWords.kt app/src/main/java/io/github/kuscher/booklight/overlay/ProofGlass.kt
```

- [ ] **Step 2: Its lines in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Seven changes; each stands in the file exactly once. The overrides that only call `super` go with their proof lines, and so do the two imports only they used.

Find:

```kotlin
        super.onCreate(savedInstanceState)
        // THE PROOF (first run, milestone 0; debug builds; goes in part 2): who started the panel.
        if (BuildConfig.DEBUG) Proof.started("onCreate", intent, referrer, byKey(intent), fromIcon(intent))
        if (fromIcon(intent)) { handOver(); return }
```

Make it:

```kotlin
        super.onCreate(savedInstanceState)
        if (fromIcon(intent)) { handOver(); return }
```

Find:

```kotlin
        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_STAY, false)) { stay = true; return }
        // THE PROOF (goes in part 2): who started it this time; and a held panel stays, so that "the key was pressed" can be seen.
        if (BuildConfig.DEBUG) Proof.started("onNewIntent", intent, referrer, byKey(intent), fromIcon(intent))
        if (BuildConfig.DEBUG && stay) return // THE PROOF
```

Make it:

```kotlin
        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_STAY, false)) { stay = true; return }
```

Delete, with the empty line before it (it stands in the file exactly once):

```kotlin
    /** THE PROOF (first run, milestone 0; debug builds; goes in part 2): a held panel's own page in the system's Keyboard shortcuts dialog. */
    override fun onProvideKeyboardShortcuts(data: MutableList<KeyboardShortcutGroup>, menu: Menu?, deviceId: Int) {
        super.onProvideKeyboardShortcuts(data, menu, deviceId)
        if (BuildConfig.DEBUG && stay) Proof.rows(this, data, deviceId)
    }

    // THE PROOF (goes in part 2): which signals a held panel is given while the system's dialog is over it, and when it goes.
    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        if (BuildConfig.DEBUG && stay) Proof.note("topResumed=$isTopResumedActivity")
    }

    override fun onPause() {
        super.onPause()
        if (BuildConfig.DEBUG && stay) Proof.note("pause")
    }

    override fun onResume() {
        super.onResume()
        if (BuildConfig.DEBUG && stay) Proof.note("resume")
    }
```

Find:

```kotlin
        super.onWindowFocusChanged(hasFocus)
        if (BuildConfig.DEBUG && stay) Proof.note("focus=$hasFocus") // THE PROOF
```

Make it:

```kotlin
        super.onWindowFocusChanged(hasFocus)
```

Find:

```kotlin
        super.onStop()
        if (BuildConfig.DEBUG && stay) Proof.note("stop") // THE PROOF
```

Make it:

```kotlin
        super.onStop()
```

Find:

```kotlin
    override fun onDestroy() {
        if (BuildConfig.DEBUG && stay) Proof.note("destroy") // THE PROOF
```

Make it:

```kotlin
    override fun onDestroy() {
```

Find:

```kotlin
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.Menu
```

Make it:

```kotlin
import android.view.KeyEvent
```

- [ ] **Step 3: Its lines in `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt` and `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** Two changes in each; each stands in its file exactly once.

In `Metrics.kt`:

Find:

```kotlin
import androidx.compose.ui.unit.dp
import io.github.kuscher.booklight.BuildConfig
```

Make it:

```kotlin
import androidx.compose.ui.unit.dp
```

Delete (it stands in the file exactly once):

```kotlin
        // THE PROOF (first run, milestone 0; debug builds; goes in part 2): the glass at the welcome's height while a mode of it runs.
        BuildConfig.DEBUG && ProofGlass.mode != null -> ProofGlass.TALL - field
```

In `Panel.kt`:

Find:

```kotlin
import io.github.kuscher.booklight.BuildConfig
import io.github.kuscher.booklight.R
```

Make it:

```kotlin
import io.github.kuscher.booklight.R
```

Delete (it stands in the file exactly once):

```kotlin
            // THE PROOF (first run, milestone 0; debug builds; goes in part 2): its drawing, over the whole glass.
            if (BuildConfig.DEBUG) ProofLayer()
```

- [ ] **Step 4: Its commands in `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`.** Two changes: the six `proof-…` cases of `onReceive`'s `when`, and their three lines in the KDoc at the top of the file.

Delete (it stands in the file exactly once):

```kotlin
            // THE PROOF (first run, milestone 0; goes in part 2). `./bl open stay` first: the panel is then held. `proof-helper`: the held panel asks
            // for the system's Keyboard shortcuts dialog; `proof-helper close`: it asks for it to go. `proof-rows quick|m|auto`: which key its rows carry.
            "proof-helper" -> main.post {
                if (arg == "close") act?.dismissKeyboardShortcutsHelper() else act?.requestShowKeyboardShortcuts()
                out(if (act == null) "no panel" else if (arg == "close") "asked for the helper to go" else "asked for the helper")
            }
            "proof-rows" -> {
                if (arg in setOf("quick", "m", "auto")) io.github.kuscher.booklight.overlay.Proof.key = arg
                out("rows=${io.github.kuscher.booklight.overlay.Proof.key}")
            }
            // THE PROOF (goes in part 2). `proof-words [NAME…]`: the system UI's own words for its Keyboard shortcuts dialog, read as an ordinary app
            // reads them. `proof-find TEXT`: the names of the system's strings that say TEXT (slow: `TRIES=120 ./bl debug proof-find Quick Insert`).
            // `proof-keys`: what the system says each attached keyboard has.
            "proof-words" -> app.scope.launch { out(io.github.kuscher.booklight.overlay.ProofWords.words(context, arg.split(' ').filter { it.isNotEmpty() })) }
            "proof-find" -> app.scope.launch {
                val t0 = android.os.SystemClock.uptimeMillis()
                val found = listOf(io.github.kuscher.booklight.overlay.ProofWords.SYSTEM_UI, "android").joinToString(" | ") { p ->
                    "$p: " + io.github.kuscher.booklight.overlay.ProofWords.find(context, p, arg).joinToString(",").ifEmpty { "-" }
                }
                out("'$arg' is $found took=${android.os.SystemClock.uptimeMillis() - t0}ms")
            }
            "proof-keys" -> out(io.github.kuscher.booklight.overlay.ProofWords.keyboards().joinToString(" | "))
            // THE PROOF (goes in part 2). `proof-glass tall|light|shader|canvas|both`: the glass at 468 dp, redrawn every frame for four seconds
            // in that way, with what the window's frames took as one line in the log.
            "proof-glass" -> main.post { out(act?.let { io.github.kuscher.booklight.overlay.ProofGlass.run(it, arg) } ?: "no panel") }
```

Delete (it stands in the file exactly once):

```kotlin
 *   THE PROOF for first run (milestone 0; it goes in part 2), with the panel held by `./bl open stay`: proof-helper [close] | proof-rows quick|m|auto
 *   proof-glass tall|light|shader|canvas|both (THE PROOF: the glass at 468 dp, redrawn every frame for four seconds; its frames' times in the log)
 *   proof-words [NAME…] | proof-find TEXT | proof-keys (THE PROOF: the system's own words, where a word comes from, the keyboards)
```

- [ ] **Step 5: Its line in the debug manifest.** In `app/src/debug/AndroidManifest.xml`:

Delete (it stands in the file exactly once):

```xml
    <!-- THE PROOF (first run, milestone 0; it goes in part 2): the system UI's own words for its Keyboard shortcuts dialog are
         read (./bl debug proof-words). If they can be, part 2 moves this line into the main manifest. No permission. -->
    <queries>
        <package android:name="com.android.systemui" />
    </queries>
```

- [ ] **Step 6: Its lines in `bl`'s header.** Delete these seven lines (they stand together, directly above `set -euo pipefail`):

```bash
# The proof for first run (milestone 0; debug builds; it goes when step 1 is built). `./bl open stay` holds the panel, then:
#   ./bl debug proof-helper [close]      the held panel asks for the system's Keyboard shortcuts dialog (or for it to go)
#   ./bl debug proof-rows quick|m|auto   which key the panel's rows in that dialog carry
#   ./bl debug proof-words [NAME…]       the system UI's own words for that dialog, as an ordinary app reads them
#   TRIES=120 ./bl debug proof-find TEXT   the names of the system's strings that say TEXT (slow: hence the longer wait)
#   ./bl debug proof-keys                what the system says each attached keyboard has
#   ./bl debug proof-glass tall|light|shader|canvas|both   the glass at 468 dp, redrawn every frame for 4 s; the frames' times in ./bl logs
```

- [ ] **Step 7: Nothing of it is left**

```bash
grep -rn "THE PROOF\|Proof\.\|ProofGlass\|ProofWords\|ProofLayer\|proof-" app/src bl || echo CLEAN
```

Expected: `CLEAN`.

- [ ] **Step 8: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt app/src/debug/AndroidManifest.xml bl
git commit -m "First run: the proof goes (what it found is in docs/research/first-run-proof.md)"
```

### Task 3: The words of the key's step

**Files:**
- Modify: `app/src/main/res/values/strings_32.xml`
- Modify: `app/src/main/res/values-de/strings_32.xml`

**Interfaces:**
- Consumes: the two files as they are (the dialog's rows and the system's words, with their German: `first_helper_title`, `first_helper_1` to `first_helper_5`, `first_sys_customize`, `first_sys_set`, `first_key_quick`, `first_key_letter`).
- Produces (string resources, in both languages): `first_key_text`; `first_key_where` (one argument: the Quick Insert key's name); `first_key_any`; `first_key_press`; `first_key_press_text`; `first_key_yours`; `first_key_retry`; `first_key_retry_text` (one argument: the system's word for its Customize button); `first_key_try` (one argument: the other key, M); `first_key_works_text`; `first_next`; `first_key_change`; `first_step` (two numbers: "1 of 5"); and for a screen reader `a11y_first_keys` (two arguments: the two suggested keys), `a11y_first_opens`, `a11y_first_tab_opens`, `a11y_first_goes_on`. Titles and answers that exist are used as they are: `card_shortcut_title`, `card_shortcut_action`, `card_not_now`, `key_works`, `key_action`.

These are design.md §5's words, with `first_key_try` taking the key as an argument where the design wrote J (the settled list: M). There is no unit test for a resource file: Step 3 checks the characters, Step 4 is the build.

- [ ] **Step 1: The English words.** In `app/src/main/res/values/strings_32.xml`:

Find:

```xml
    <string name="first_key_letter" translatable="false">M</string>
</resources>
```

Make it:

```xml
    <string name="first_key_letter" translatable="false">M</string>

    <!-- First run · the key's step under the empty field (design.md §4 and §5): four screens on one skeleton. -->
    <string name="first_key_text">One press, from any app, and Booklight is there.</string>
    <!-- Under the caps. %1$s is the Quick Insert key's name, as the system says it. -->
    <string name="first_key_where">%1$s is left of A</string>
    <string name="first_key_any">or any keys you like</string>
    <!-- Back from the system's dialog without a key. -->
    <string name="first_key_press">Now press your keys</string>
    <string name="first_key_press_text">Nothing happens? Open Keyboard shortcuts once more.</string>
    <string name="first_key_yours">your keys</string>
    <!-- Back again. %1$s is the system's own word for its Customize button. -->
    <string name="first_key_retry">Didn’t work?</string>
    <string name="first_key_retry_text">Click %1$s first. Hold Action, then press the second key.</string>
    <!-- %1$s is the other key to try: M. -->
    <string name="first_key_try">Or try Action + %1$s</string>
    <string name="first_key_works_text">The same keys put Booklight away.</string>
    <string name="first_next">Go on</string>
    <string name="first_key_change">Change the key</string>
    <!-- Where a screen is in first run: "1 of 5". -->
    <string name="first_step">%1$d of %2$d</string>
    <!-- For a screen reader, once when a screen comes. %1$s and %2$s are the two suggested keys. -->
    <string name="a11y_first_keys">Suggested keys: %1$s and %2$s</string>
    <string name="a11y_first_opens">Enter opens Keyboard shortcuts. Tab for Not now.</string>
    <string name="a11y_first_tab_opens">Tab, then Enter opens Keyboard shortcuts.</string>
    <string name="a11y_first_goes_on">Enter goes on.</string>
</resources>
```

- [ ] **Step 2: The German words.** In `app/src/main/res/values-de/strings_32.xml`:

Find:

```xml
    <string name="first_key_quick">Schnelles Einfügen</string>
</resources>
```

Make it:

```xml
    <string name="first_key_quick">Schnelles Einfügen</string>

    <string name="first_key_text">Ein Druck, aus jeder App, und Booklight ist da.</string>
    <string name="first_key_where">%1$s liegt links neben A</string>
    <string name="first_key_any">oder Tasten deiner Wahl</string>
    <string name="first_key_press">Jetzt deine Tasten drücken</string>
    <string name="first_key_press_text">Nichts passiert? Öffne die Tastenkürzel noch einmal.</string>
    <string name="first_key_yours">deine Tasten</string>
    <string name="first_key_retry">Hat nicht geklappt?</string>
    <string name="first_key_retry_text">Zuerst „%1$s“ klicken. Aktion halten, dann die zweite Taste drücken.</string>
    <string name="first_key_try">Oder Aktion + %1$s versuchen</string>
    <string name="first_key_works_text">Dieselben Tasten schließen Booklight wieder.</string>
    <string name="first_next">Weiter</string>
    <string name="first_key_change">Taste ändern</string>
    <string name="first_step">%1$d von %2$d</string>
    <string name="a11y_first_keys">Vorschlag: %1$s und %2$s</string>
    <string name="a11y_first_opens">Enter öffnet die Tastenkürzel. Tab für „Nicht jetzt“.</string>
    <string name="a11y_first_tab_opens">Tab, dann Enter öffnet die Tastenkürzel.</string>
    <string name="a11y_first_goes_on">Enter geht weiter.</string>
</resources>
```

- [ ] **Step 3: Check the characters**

```bash
python3 - <<'EOF'
en = open('app/src/main/res/values/strings_32.xml', encoding='utf-8').read()
de = open('app/src/main/res/values-de/strings_32.xml', encoding='utf-8').read()
assert '”' not in de and '&#8221;' not in de, 'a German quotation closes with U+201C'
assert de.count('„') + de.count('&#8222;') == de.count('“') + de.count('&#8220;') == 4, 'four German quotations, each opened and closed'
assert 'Didn’t work?' in en and "\\'" not in en, 'the English apostrophe is U+2019'
import re
names = lambda t: set(re.findall(r'<string name="([a-z0-9_]+)"', t))
assert names(en) - names(de) == {'first_key_letter'} and not names(de) - names(en), 'the same strings in both, but for the letter'
print('OK', len(names(en)), 'English,', len(names(de)), 'German')
EOF
```

Expected: `OK 27 English, 26 German`.

- [ ] **Step 4: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings_32.xml app/src/main/res/values-de/strings_32.xml
git commit -m "First run: the words of the key's step, English and German"
```

### Task 4: `FirstRun`: the key's step as it stands (answers, what is armed, the caption, the words)

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunKeyStepTest.kt`

**Interfaces:**
- Consumes (in `object FirstRun`): `State`, `Run`, `Screen`, `Key`, `screen(s: State): Screen?`, `other(suggested: Key): Key?`, `answer(s: State, answer: Answer): State`, and the enum `Answer { NOT_NOW, GO_ON, CHANGE_KEY, SKIP, AGREE, DONE }`.
- Produces, in `object FirstRun`:
  - `Answer` gains `OPEN_HELPER` as its first value (it changes nothing that is kept)
  - `fun answers(s: State): List<Answer>`, `fun armed(s: State): Int` (-1: none), `fun tab(armed: Int, count: Int, back: Boolean): Int`
  - `enum class Caption { WHERE_AND_ANY, ANY, YOURS, TRY_OTHER, NONE }`, `fun caption(s: State, suggested: Key): Caption`
  - `data class Read(val customize: String? = null, val set: String? = null, val quick: String? = null)`, `data class Words(val customize: String, val set: String, val quick: String)`, `const val MOST_WORD = 40`, `fun words(read: Read?, ours: Words): Words`

- [ ] **Step 1: Write the failing test** `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunKeyStepTest.kt`

```kotlin
package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Answer
import io.github.kuscher.booklight.core.FirstRun.Caption
import io.github.kuscher.booklight.core.FirstRun.Key
import io.github.kuscher.booklight.core.FirstRun.Read
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.State
import io.github.kuscher.booklight.core.FirstRun.Words
import org.junit.Assert.assertEquals
import org.junit.Test

class FirstRunKeyStepTest {
    private val k1 = State(Run.NEW)
    private val k2 = State(Run.NEW, helper = 1)
    private val k3 = State(Run.NEW, helper = 2)
    private val k4 = State(Run.NEW, key = true)
    private val lesson = State(Run.NEW, done = listOf("key"), key = true)

    @Test fun theKeysScreensOfferTheDialogOrNotNow() {
        for (s in listOf(k1, k2, k3)) assertEquals(listOf(Answer.OPEN_HELPER, Answer.NOT_NOW), FirstRun.answers(s))
        assertEquals(listOf(Answer.GO_ON), FirstRun.answers(k4))
        assertEquals(listOf(Answer.GO_ON, Answer.CHANGE_KEY), FirstRun.answers(State(Run.REPLAY, key = true)))
        assertEquals(emptyList<Answer>(), FirstRun.answers(State()))
        assertEquals(emptyList<Answer>(), FirstRun.answers(lesson))
    }

    @Test fun enterHasAnAnswerButWhereTheKeyIsTheThingToPress() {
        assertEquals(listOf(0, -1, 0, 0), listOf(k1, k2, k3, k4).map { FirstRun.armed(it) })
        assertEquals(-1, FirstRun.armed(State()))
    }

    @Test fun askingForTheDialogChangesNothingThatIsKept() {
        for (s in listOf(k1, k2, k3)) assertEquals(s, FirstRun.answer(s, Answer.OPEN_HELPER))
    }

    @Test fun tabGoesRoundAndTheFirstPressArmsTheFirst() {
        assertEquals(0, FirstRun.tab(-1, 2, back = false))
        assertEquals(0, FirstRun.tab(-1, 2, back = true))
        assertEquals(1, FirstRun.tab(0, 2, back = false))
        assertEquals(0, FirstRun.tab(1, 2, back = false))
        assertEquals(1, FirstRun.tab(0, 2, back = true))
        assertEquals(0, FirstRun.tab(1, 2, back = true))
        // One answer: it stays armed. None: nothing can be.
        assertEquals(0, FirstRun.tab(0, 1, back = false))
        assertEquals(0, FirstRun.tab(0, 1, back = true))
        assertEquals(-1, FirstRun.tab(0, 0, back = false))
        // An arming that is out of range (the screen changed under it) starts at the first.
        assertEquals(0, FirstRun.tab(5, 2, back = false))
    }

    @Test fun onlyQuickInsertNeedsSayingWhereItIs() {
        assertEquals(Caption.WHERE_AND_ANY, FirstRun.caption(k1, Key.QUICK_INSERT))
        assertEquals(Caption.ANY, FirstRun.caption(k1, Key.M))
        assertEquals(Caption.YOURS, FirstRun.caption(k2, Key.QUICK_INSERT))
        assertEquals(Caption.YOURS, FirstRun.caption(k2, Key.M))
    }

    /** A keyboard without Quick Insert was offered Action + M: there is no other key to try after it. */
    @Test fun theOtherKeysToTryAreNamedOnlyAfterQuickInsert() {
        assertEquals(Caption.TRY_OTHER, FirstRun.caption(k3, Key.QUICK_INSERT))
        assertEquals(Caption.ANY, FirstRun.caption(k3, Key.M))
        assertEquals(Caption.NONE, FirstRun.caption(k4, Key.QUICK_INSERT))
        assertEquals(Caption.NONE, FirstRun.caption(lesson, Key.QUICK_INSERT))
    }

    private val ours = Words("Customize", "Set shortcut", "Quick Insert")

    @Test fun theSystemsOwnWordsAreQuotedWhereTheyWereRead() {
        assertEquals(Words("Anpassen", "Speichern", "Schnelles Einfügen"), FirstRun.words(Read("Anpassen", "Speichern", "Schnelles Einfügen"), ours))
        assertEquals(Words("Anpassen", "Set shortcut", "Quick Insert"), FirstRun.words(Read(customize = " Anpassen "), ours))
    }

    /** The system's package cannot be read, a name is gone in another build, a word is empty or is no button's name: Booklight's own word. */
    @Test fun whereAWordCannotBeHadBooklightsOwnStandsIn() {
        assertEquals(ours, FirstRun.words(null, ours))
        assertEquals(ours, FirstRun.words(Read(), ours))
        assertEquals(ours, FirstRun.words(Read("", "   ", "\n"), ours))
        assertEquals(ours, FirstRun.words(Read("x".repeat(FirstRun.MOST_WORD + 1), "two\nlines", null), ours))
        assertEquals("x".repeat(FirstRun.MOST_WORD), FirstRun.words(Read("x".repeat(FirstRun.MOST_WORD)), ours).customize)
    }
}
```

- [ ] **Step 2: Run it and see it fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunKeyStepTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'Caption'` among its `e:` lines.

- [ ] **Step 3: Write the implementation.** First the new answer. In `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`:

Find:

```kotlin
    /** What can be answered. [DONE]: the choices were left, however they were left. */
    enum class Answer { NOT_NOW, GO_ON, CHANGE_KEY, SKIP, AGREE, DONE }
```

Make it:

```kotlin
    /**
     * What can be answered. [OPEN_HELPER]: "Open Keyboard shortcuts"; it changes nothing that is kept
     * (the panel asks the system for its dialog and holds on). [DONE]: the choices were left, however
     * they were left.
     */
    enum class Answer { OPEN_HELPER, NOT_NOW, GO_ON, CHANGE_KEY, SKIP, AGREE, DONE }
```

Then: in `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`, directly above the file's last line (the `}` that closes `object FirstRun`), add, with one empty line before it:

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

    /**
     * Tab among [count] answers of which [armed] is armed (-1: none): the next one, with [back] the
     * one before, round and round. The first press arms the first, whichever way it goes.
     */
    fun tab(armed: Int, count: Int, back: Boolean): Int = when {
        count <= 0 -> -1
        armed !in 0 until count -> 0
        else -> (armed + (if (back) count - 1 else 1)) % count
    }

    /** What stands under the caps: where the suggested key is and that any keys will do; "your keys"; the other keys to try; nothing. */
    enum class Caption { WHERE_AND_ANY, ANY, YOURS, TRY_OTHER, NONE }

    /** The caption of the screen that stands, for the key that is [suggested]. Only Quick Insert needs saying where it is; only after it is there another key to try. */
    fun caption(s: State, suggested: Key): Caption = when (screen(s)) {
        Screen.K1 -> if (suggested == Key.QUICK_INSERT) Caption.WHERE_AND_ANY else Caption.ANY
        Screen.K2 -> Caption.YOURS
        Screen.K3 -> if (other(suggested) != null) Caption.TRY_OTHER else Caption.ANY
        else -> Caption.NONE
    }

    /** The system's own words for its Keyboard shortcuts dialog as they were read from it; null: that one could not be read. */
    data class Read(val customize: String? = null, val set: String? = null, val quick: String? = null)

    /** The words Booklight quotes: the dialog's Customize button, its Set shortcut button, and the Quick Insert key's name. */
    data class Words(val customize: String, val set: String, val quick: String)

    /** A word of the system's longer than this is not quoted: it is no button's name. */
    const val MOST_WORD = 40

    private fun usable(read: String?, ours: String): String = read?.trim()?.takeIf { it.isNotEmpty() && it.length <= MOST_WORD && '\n' !in it } ?: ours

    /** The words to quote: each of the system's own where it was read and can be a button's name, else [ours]. */
    fun words(read: Read?, ours: Words): Words = Words(usable(read?.customize, ours.customize), usable(read?.set, ours.set), usable(read?.quick, ours.quick))
```

- [ ] **Step 4: Run it and see it pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunKeyStepTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run every core test**

```bash
./bl test
```

Expected: no output, exit status 0.

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunKeyStepTest.kt
git commit -m "First run: the key's step as it stands: its answers, what Enter runs, the caption, the words to quote (core, tested)"
```

### Task 5: `FirstRun`: the hold, what counts as the key, the icon's first click

**Files:**
- Modify: `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`
- Create (test): `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunHoldTest.kt`

**Interfaces:**
- Consumes (in `object FirstRun`): `State`, `Run`, `Screen`, `MAX_OPENS`, `fits(screenDp: Float): Boolean`, `screen(s: State): Screen?`.
- Produces, in `object FirstRun`:
  - `enum class Hold { NONE, ASKED, UNDER }`, `enum class Signal { ASK, FOCUS_LOST, FOCUS_BACK, KEY, GONE, NO_DIALOG }`, `data class Held(val hold: Hold, val close: Boolean = false, val came: Boolean = false)`, `fun hold(h: Hold, signal: Signal): Held`
  - `data class Start(val launcher: Boolean, val assist: Boolean = false, val referrer: String? = null, val bounds: Boolean = false, val extras: Boolean = false, val home: String? = null)`, `const val SYSTEM = "android"`, `fun fromIcon(s: Start): Boolean`, `fun byKey(s: Start): Boolean`
  - `fun iconShowsPanel(s: State, screenDp: Float): Boolean`, `fun iconClicked(s: State): State`

What the device said, and what this is built on (`docs/research/first-run-proof.md`, checks 2, 3, 6 and 7): under the dialog the panel is told one thing, `focus=false`; Esc, the dialog's X and a dismissal give `focus=true` back; a key that lands shows as its start first and `focus=true` 45 ms later, and its start is the launcher activity, sent by the system, with no icon's place and nothing carried.

- [ ] **Step 1: Write the failing test** `core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunHoldTest.kt`

```kotlin
package io.github.kuscher.booklight.core

import io.github.kuscher.booklight.core.FirstRun.Hold
import io.github.kuscher.booklight.core.FirstRun.Held
import io.github.kuscher.booklight.core.FirstRun.Run
import io.github.kuscher.booklight.core.FirstRun.Signal
import io.github.kuscher.booklight.core.FirstRun.Start
import io.github.kuscher.booklight.core.FirstRun.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunHoldTest {
    /** The signals in their order: the hold at the end, whether the panel was closed on the way, and how often the dialog came. */
    private fun after(vararg signals: Signal): Triple<Hold, Boolean, Int> {
        var hold = Hold.NONE; var closed = false; var came = 0
        for (s in signals) { val h = FirstRun.hold(hold, s); hold = h.hold; closed = closed || h.close; if (h.came) came++ }
        return Triple(hold, closed, came)
    }

    @Test fun withoutAHoldALostFocusClosesThePanelAsEveryDay() {
        assertEquals(Held(Hold.NONE, close = true), FirstRun.hold(Hold.NONE, Signal.FOCUS_LOST))
        for (s in listOf(Signal.FOCUS_BACK, Signal.KEY, Signal.GONE, Signal.NO_DIALOG)) assertEquals(Held(Hold.NONE), FirstRun.hold(Hold.NONE, s))
    }

    /** As the device said it: under the dialog the panel is told one thing, that it lost the focus. */
    @Test fun theDialogComesAndThePanelStays() {
        assertEquals(Triple(Hold.UNDER, false, 1), after(Signal.ASK, Signal.FOCUS_LOST))
        // The second that was allowed for it to come has passed: nothing changes.
        assertEquals(Triple(Hold.UNDER, false, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.NO_DIALOG))
    }

    @Test fun theDialogIsClosedByHandAndTheHoldIsOver() {
        assertEquals(Triple(Hold.NONE, false, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.FOCUS_BACK))
        // From here a lost focus closes, as every day.
        assertEquals(Triple(Hold.NONE, true, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.FOCUS_BACK, Signal.FOCUS_LOST))
    }

    /** The key lands while the dialog is open: its start comes first, the focus 45 ms later. The panel never closes. */
    @Test fun theKeyLandsUnderTheDialog() {
        assertEquals(Triple(Hold.UNDER, false, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.KEY))
        assertEquals(Triple(Hold.NONE, false, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.KEY, Signal.FOCUS_BACK))
    }

    /** A click on another window, another app's shortcut: the front is gone for good, and so is the panel. */
    @Test fun anotherWindowTakesTheFrontForGood() {
        assertEquals(Triple(Hold.NONE, true, 1), after(Signal.ASK, Signal.FOCUS_LOST, Signal.GONE))
        assertEquals(Triple(Hold.NONE, true, 0), after(Signal.ASK, Signal.GONE))
    }

    @Test fun theSystemShowsNoDialog() {
        assertEquals(Triple(Hold.NONE, false, 0), after(Signal.ASK, Signal.NO_DIALOG))
        assertEquals(Triple(Hold.NONE, true, 0), after(Signal.ASK, Signal.NO_DIALOG, Signal.FOCUS_LOST))
    }

    @Test fun askedTwiceItIsCountedOnce() {
        assertEquals(Triple(Hold.UNDER, false, 1), after(Signal.ASK, Signal.ASK, Signal.FOCUS_LOST, Signal.FOCUS_LOST))
    }

    private val home = "com.example.home"
    /** A keyboard shortcut's start, as it was seen on a device: the launcher activity, sent by the system, nothing else. */
    private val key = Start(launcher = true, referrer = FirstRun.SYSTEM, home = home)

    @Test fun aStartByTheSystemItselfIsTheKey() {
        assertTrue(FirstRun.byKey(key))
        assertFalse(FirstRun.fromIcon(key))
        assertTrue(FirstRun.byKey(Start(launcher = false, assist = true)))
        // Nobody is named: as before, a key.
        assertTrue(FirstRun.byKey(Start(launcher = true, home = home)))
    }

    @Test fun aClickOnTheIconIsNoKey() {
        val withBounds = key.copy(bounds = true)
        val fromHome = Start(launcher = true, referrer = home, home = home)
        for (s in listOf(withBounds, fromHome)) { assertTrue(FirstRun.fromIcon(s)); assertFalse(FirstRun.byKey(s)) }
    }

    @Test fun openInTheStoreAndAnotherAppsStartAreNoKey() {
        for (who in listOf("com.android.vending", "com.android.settings", "com.android.shell", "com.example.other"))
            assertFalse(who, FirstRun.byKey(Start(launcher = true, referrer = who, home = home)))
        // A start that carries something, or does not ask for the launcher activity (the widget, the tile, text handed over).
        assertFalse(FirstRun.byKey(key.copy(extras = true)))
        assertFalse(FirstRun.byKey(Start(launcher = false, referrer = FirstRun.SYSTEM)))
        assertFalse(FirstRun.fromIcon(Start(launcher = false, bounds = true)))
    }

    @Test fun theIconsFirstClickShowsThePanelOnce() {
        val fresh = State(Run.NEW)
        assertTrue(FirstRun.iconShowsPanel(fresh, 1200f))
        assertTrue(FirstRun.iconShowsPanel(fresh.copy(helper = 2), 1200f))
        assertTrue(FirstRun.iconShowsPanel(fresh.copy(opens = FirstRun.MAX_OPENS - 1), 1200f))        // its panel is the third opening
        assertFalse(FirstRun.iconShowsPanel(FirstRun.iconClicked(fresh), 1200f))
    }

    @Test fun everyOtherClickOnTheIconOpensTheWindow() {
        assertFalse(FirstRun.iconShowsPanel(State(), 1200f))                              // no run
        assertFalse(FirstRun.iconShowsPanel(State(Run.UPDATE), 1200f))                    // an installation that was there before knows the window
        assertFalse(FirstRun.iconShowsPanel(State(Run.NEW, key = true), 1200f))           // the key works: nothing to ask for
        // Asked of the state as this opening leaves it: a click that is the fourth opening would show an empty field.
        assertFalse(FirstRun.iconShowsPanel(State(Run.NEW, opens = FirstRun.MAX_OPENS), 1200f))
        assertFalse(FirstRun.iconShowsPanel(State(Run.NEW, opens = FirstRun.MAX_OPENS + 1), 1200f))   // the run waits
        assertFalse(FirstRun.iconShowsPanel(State(Run.NEW), 474f))                        // a screen too low for first run
    }
}
```

- [ ] **Step 2: Run it and see it fail**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunHoldTest' --console=plain
```

Expected: `FAILURE: Build failed`, from `:core:compileTestKotlin`, with `Unresolved reference 'Hold'` among its `e:` lines.

- [ ] **Step 3: Write the implementation.** In `core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt`, directly above the file's last line (the `}` that closes `object FirstRun`), add, with one empty line before it:

```kotlin
    // ---- the hold, and the key landing

    /**
     * The panel kept alive behind the system's Keyboard shortcuts dialog. [ASKED]: the dialog was
     * asked for and has not come. [UNDER]: it is over the panel, which has lost the focus to it.
     */
    enum class Hold { NONE, ASKED, UNDER }

    /**
     * What the panel is told. [ASK]: Enter on "Open Keyboard shortcuts". [FOCUS_LOST], [FOCUS_BACK]:
     * its window's focus. [KEY]: a start by the user's key. [GONE]: another window has had the front
     * for longer than the key's own start takes it. [NO_DIALOG]: a second after asking.
     */
    enum class Signal { ASK, FOCUS_LOST, FOCUS_BACK, KEY, GONE, NO_DIALOG }

    /** The hold after a signal. [close]: the panel goes, as it does on any lost focus. [came]: the dialog has come over the panel, once more. */
    data class Held(val hold: Hold, val close: Boolean = false, val came: Boolean = false)

    /**
     * What a signal makes of the hold. Under the dialog the panel is told one thing, that it lost the
     * focus; so the first lost focus after asking is the dialog, and the focus coming back is the
     * dialog gone (closed by hand, or by the key landing: its start comes first, the focus after).
     * Without a hold a lost focus closes the panel, as every day.
     */
    fun hold(h: Hold, signal: Signal): Held = when (h) {
        Hold.NONE -> when (signal) {
            Signal.ASK -> Held(Hold.ASKED)
            Signal.FOCUS_LOST -> Held(Hold.NONE, close = true)
            else -> Held(Hold.NONE)
        }
        Hold.ASKED -> when (signal) {
            Signal.FOCUS_LOST -> Held(Hold.UNDER, came = true)
            Signal.NO_DIALOG -> Held(Hold.NONE)
            Signal.GONE -> Held(Hold.NONE, close = true)
            else -> Held(Hold.ASKED)
        }
        Hold.UNDER -> when (signal) {
            Signal.FOCUS_BACK -> Held(Hold.NONE)
            Signal.GONE -> Held(Hold.NONE, close = true)
            else -> Held(Hold.UNDER)
        }
    }

    /**
     * A start of the panel as the system describes it. [launcher]: the launcher activity was asked
     * for (what the icon, a keyboard shortcut and "Open" in the store all do). [assist]: the
     * assistant key. [referrer]: who the system says sent it, as a package's name; [SYSTEM] for the
     * system itself. [bounds]: it says where on screen an icon was. [extras]: it carries anything.
     * [home]: the home app's package.
     */
    data class Start(val launcher: Boolean, val assist: Boolean = false, val referrer: String? = null, val bounds: Boolean = false, val extras: Boolean = false, val home: String? = null)

    /** Who the system itself is, as a referrer. */
    const val SYSTEM = "android"

    /** A click on the app's icon: the home app sent it, or it says where the icon was. */
    fun fromIcon(s: Start): Boolean = s.launcher && (s.bounds || (s.home != null && s.referrer == s.home))

    /**
     * The user's key: the assistant key, or a start of the launcher activity by the system itself
     * (or by nobody who is named) that says nothing about an icon and carries nothing. That is what
     * a keyboard shortcut's start was seen to be; "Open" in the store, in an installer or in
     * Settings names its sender, and so does another app.
     */
    fun byKey(s: Start): Boolean = when {
        s.assist -> true
        !s.launcher || fromIcon(s) || s.extras -> false
        else -> s.referrer == null || s.referrer == SYSTEM
    }

    /**
     * Whether a click on the icon shows the panel instead of the Booklight window: once, for a new
     * installation that still asks for its key, and only where the key's step will stand in the
     * panel this click makes. So it is asked of the state as this [opening] leaves it: a click that
     * is the fourth opening would show an empty field. [iconClicked] uses it up.
     */
    fun iconShowsPanel(s: State, screenDp: Float): Boolean =
        !s.icon && s.run == Run.NEW &&
            stage(opening(s, screenDp), screenDp).let { it == Screen.K1 || it == Screen.K2 || it == Screen.K3 }

    fun iconClicked(s: State): State = s.copy(icon = true)
```

- [ ] **Step 4: Run it and see it pass**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRunHoldTest' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run every core test**

```bash
./bl test
```

Expected: no output, exit status 0.

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/io/github/kuscher/booklight/core/FirstRun.kt core/src/test/kotlin/io/github/kuscher/booklight/core/FirstRunHoldTest.kt
git commit -m "First run: what a signal makes of the hold, what counts as the key, the icon's first click (core, tested)"
```

### Task 6: The system's own words, and the dialog's rows

**Files:**
- Create: `app/src/main/java/io/github/kuscher/booklight/device/SystemWords.kt`

**Interfaces:**
- Consumes (core, Task 4, and part 1): `FirstRun.Read`, `FirstRun.Words`, `FirstRun.words(read: Read?, ours: Words): Words`, `FirstRun.Key`. Strings: `first_sys_customize`, `first_sys_set`, `first_key_quick`, `first_key_letter`, `first_helper_title`, `first_helper_1` to `first_helper_5`.
- Produces: `object SystemWords` in package `io.github.kuscher.booklight.device` with `var read: FirstRun.Read?` (snapshot state, set only inside), `fun load(context: Context, scope: CoroutineScope)`, `fun words(context: Context): FirstRun.Words`, `fun name(context: Context, key: FirstRun.Key): String`, `fun rows(context: Context, key: FirstRun.Key): KeyboardShortcutGroup`.

The names are the system UI's own, read on a device: `shortcut_helper_customize_button_text`, `shortcut_helper_customize_dialog_set_shortcut_button_label`, `keyboard_key_quick_insert`. The read took 3 ms there and needed no line in `<queries>`; it still runs off the main thread, and it is made in the system's languages by a configuration of its own (`Resources.getSystem()` would not take that override). Which word is used when one cannot be had is core's (Task 4, tested).

There is no unit test for this task: the app module has no test source set (`app/src` holds `main` and `debug`). What it decides is core's and is tested there; its check here is that it builds, and what it does is seen in Task 10's device checks.

- [ ] **Step 1: Write `SystemWords.kt`**

```kotlin
package io.github.kuscher.booklight.device

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.FirstRun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The system's own words for its Keyboard shortcuts dialog, so that first run can quote the buttons as the user
 * sees them: "Customize", "Set shortcut", and the name of the Quick Insert key. They are read from the system
 * UI's resources as any app reads another app's label, in the system's own language (Booklight can be set to
 * another one: hence the configuration passed here; `Resources.getSystem()` takes no such override), off the main
 * thread, and kept while the process lives. No permission, and no line in `<queries>`: the system UI's package was
 * visible to an ordinary app without one (docs/research/first-run-proof.md, check 9). Where a word cannot be had,
 * Booklight's own stands in (core `FirstRun.words`).
 */
object SystemWords {
    private const val SYSTEM_UI = "com.android.systemui"

    /** What was read; null until it has been, and where the system UI's resources could not be had. Snapshot state: what quotes it follows it. */
    var read by mutableStateOf<FirstRun.Read?>(null)
        private set

    /** Reads them, once in a process. Asked for when a screen of the key's step is due. */
    fun load(context: Context, scope: CoroutineScope) {
        if (read != null) return
        val app = context.applicationContext
        scope.launch(Dispatchers.Default) {
            val got = fetch(app)
            withContext(Dispatchers.Main) { read = got }
        }
    }

    private fun fetch(context: Context): FirstRun.Read? = runCatching {
        val pm = context.packageManager
        val system = context.getSystemService(LocaleManager::class.java).systemLocales
        val res = pm.getResourcesForApplication(pm.getApplicationInfo(SYSTEM_UI, 0), Configuration(context.resources.configuration).apply { setLocales(system) })
        fun word(name: String): String? = res.getIdentifier(name, "string", SYSTEM_UI).takeIf { it != 0 }?.let { runCatching { res.getString(it) }.getOrNull() }
        FirstRun.Read(word("shortcut_helper_customize_button_text"), word("shortcut_helper_customize_dialog_set_shortcut_button_label"), word("keyboard_key_quick_insert"))
    }.getOrNull()

    /** The words to quote now: the system's where they were read, else Booklight's own. */
    fun words(context: Context): FirstRun.Words = FirstRun.words(
        read, FirstRun.Words(context.getString(R.string.first_sys_customize), context.getString(R.string.first_sys_set), context.getString(R.string.first_key_quick)),
    )

    /** What the key that is suggested beside Action is called. */
    fun name(context: Context, key: FirstRun.Key): String = if (key == FirstRun.Key.QUICK_INSERT) words(context).quick else context.getString(R.string.first_key_letter)

    /**
     * Booklight's page in the system's Keyboard shortcuts dialog while the panel waits for its key: five numbered
     * steps, each with the suggested keys at its end (docs/design/first-run/design.md §5). No label and no heading
     * holds the app's name: the dialog's search would then stay on this page.
     */
    fun rows(context: Context, key: FirstRun.Key): KeyboardShortcutGroup {
        val words = words(context)
        val code = if (key == FirstRun.Key.QUICK_INSERT) KeyEvent.KEYCODE_CONTEXTUAL_INSERT else KeyEvent.KEYCODE_M
        val labels = listOf(
            context.getString(R.string.first_helper_1, words.customize),
            context.getString(R.string.first_helper_2),
            context.getString(R.string.first_helper_3, name(context, key)),
            context.getString(R.string.first_helper_4, words.set),
            context.getString(R.string.first_helper_5),
        )
        return KeyboardShortcutGroup(context.getString(R.string.first_helper_title), labels.map { KeyboardShortcutInfo(it, code, KeyEvent.META_META_ON) })
    }
}
```

- [ ] **Step 2: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/device/SystemWords.kt
git commit -m "First run: the system's own words for its dialog, read as any app reads a label, and the five rows of Booklight's page in it"
```

### Task 7: The stage: the model's state, its height, and its drawing

**Files:**
- Create: `app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt`

**Interfaces:**
- Consumes: core (Tasks 1, 4, part 1): `FirstRun.stage(s, screenDp)`, `answers(s)`, `armed(s)`, `tab(armed, count, back)`, `caption(s, suggested)`, `count(s)`, `answer(s, answer)`, `helperCame(s)`, `keyLanded(s)`, `suggest(quickInsert)`, `hasQuickInsert(keyboards, last)`. App: `Prefs.firstRun { … }`, `Settings.firstRun()`, `Keyboards.attached()`, `SystemWords.words(context)` and `SystemWords.name(context, key)` (Task 6), Task 3's strings, and from the panel `OptionStrip(options, chosen, onChoose, onRun, vertical = true, lit = …)`, `Keycap("tab")`, `DrawnCheck(shown, color, modifier, stroke)`, `Modifier.fadeEnd()`, `SMALL`, `SECOND`, `LocalDark`, `LocalMotion`, `Metrics.row`, `Metrics.pad`. In `OverlayModel`: `settings` (its setter is the model's own), `screenDp` (Task 1), and part 1's `stage`.
- Produces: in `OverlayModel`: `val stage: FirstRun.Screen?` (now the key's step only), `var stageArmed: Int`, `var suggested: FirstRun.Key`, `var keyboard: Int?`, `val awaitsKey: Boolean`, `fun resuggest()`, `fun stageTab(back: Boolean)`, `fun stageArm(index: Int)`, `fun stageEnter(): FirstRun.Answer?`, `fun answerStage(answer: FirstRun.Answer)`, `fun helperCame()`, `fun keyLanded()`, `fun firstChanged()`. `Metrics.stage` (184 dp). The composable `FirstStage(model: OverlayModel, on: FirstRun.Screen, onAnswer: (FirstRun.Answer) -> Unit)`.

Nothing calls `FirstStage` yet: Task 9 puts it where the cards stood. The measures are design.md §4's: the seat is a row's (56 dp) from y = 76, its mark on x = 38, its words from x = 72, the counter ending at x = 700; the band is 112 dp with the caps (56 dp) on its centre line, y = 188, the caption centred on y = 230, the answers ending at x = 700. Title and line roll, the band and the caption fade, the check draws itself, all by helpers that exist; the cap is a picture and takes no pointer.

There is no unit test for this task: the app module has no test source set (`app/src` holds `main` and `debug`). What it decides is core's and is tested there; its check here is that it builds, and what it does is seen in Task 10's device checks.

- [ ] **Step 1: The model's state, in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Two changes; each "Find" stands in the file exactly once. The second replaces part 1's `stage` with the whole of the stage's state.

Find:

```kotlin
import io.github.kuscher.booklight.data.firstRun
```

Make it:

```kotlin
import io.github.kuscher.booklight.data.firstRun
import io.github.kuscher.booklight.device.Keyboards
```

Find:

```kotlin
    /**
     * The screen of first run that has the place under the empty field (core `FirstRun`); null: none. Like the card, only
     * while nothing is typed. Nothing draws it yet: `./bl debug first` and `dump` say it.
     */
    val stage: FirstRun.Screen? by derivedStateOf { if (demo || guided || query.isNotEmpty() || chip != null) null else FirstRun.stage(settings.firstRun(), screenDp) }
```

Make it:

```kotlin
    // ---- first run: its stage under the empty field

    /**
     * The screen of first run that stands under the empty field (core `FirstRun.stage`); null: none. Like a tip, only while
     * nothing is typed. Until the lessons, the question and the choices are drawn, only the key's step is: a run that is
     * past it shows nothing.
     */
    val stage: FirstRun.Screen? by derivedStateOf {
        if (demo || guided || query.isNotEmpty() || chip != null) null
        else FirstRun.stage(settings.firstRun(), screenDp)?.takeIf { it.step == FirstRun.Step.KEY }
    }
    /** Which of the stage's answers Enter runs; -1: none (Enter then does nothing). */
    var stageArmed by mutableIntStateOf(FirstRun.armed(app.prefs.now.firstRun())); private set
    /** The key suggested beside Action, for the keyboard in front ([resuggest]). */
    var suggested by mutableStateOf(FirstRun.Key.QUICK_INSERT); private set
    /** The keyboard the last key came from, by its id; null before any key. Set by the panel. */
    var keyboard: Int? = null
    /** The key's step waits for the key: none has opened the panel yet, or another one is asked for. */
    val awaitsKey: Boolean get() = stage.let { it == FirstRun.Screen.K1 || it == FirstRun.Screen.K2 || it == FirstRun.Screen.K3 }

    // (The keyboards are asked only where a stage is due: on every other day nothing here runs.)
    init { if (!demo && stage != null) resuggest() }

    private fun first() = app.prefs.now.firstRun()

    /**
     * A step of first run, worked out inside the settings' own update (`Prefs.firstRun`); what stands is read from the
     * settings in the same call (they reach [settings] by themselves only a moment later). Nothing is written where
     * nothing changes.
     */
    private fun step(f: (FirstRun.State) -> FirstRun.State) {
        val was = first()
        if (f(was) == was) return
        val before = stage
        app.prefs.firstRun(f)
        settings = app.prefs.now
        if (stage != before) stageArmed = FirstRun.armed(first())
    }

    /**
     * The key is suggested for the keyboard in front: the one the last key came from; before any key, the device's own.
     * Asked when the stage is set down and when the system's dialog is asked for, never while the caps are being read.
     */
    fun resuggest() { suggested = FirstRun.suggest(FirstRun.hasQuickInsert(Keyboards.attached(), keyboard)) }

    /** Tab on the stage: the next answer is armed, with [back] the one before; the first press arms the first. */
    fun stageTab(back: Boolean) { if (stage != null) stageArmed = FirstRun.tab(stageArmed, FirstRun.answers(first()).size, back) }
    fun stageArm(index: Int) { if (stage != null && index in FirstRun.answers(first()).indices) stageArmed = index }
    /** Enter on the stage: the armed answer; null where none is armed (Enter then does nothing, and is not kept). */
    fun stageEnter(): FirstRun.Answer? = if (stage == null) null else FirstRun.answers(first()).getOrNull(stageArmed)
    /** An answer was given on the stage. ("Open Keyboard shortcuts" changes nothing here: it is the window's to do.) */
    fun answerStage(answer: FirstRun.Answer) = step { FirstRun.answer(it, answer) }
    /** The system's Keyboard shortcuts dialog has come over the panel: under it the stage says what to do next. */
    fun helperCame() = step { FirstRun.helperCame(it) }
    /** The user's key has opened the panel, or uncovered it. Where its step waited for it, "Your key works" stands. */
    fun keyLanded() = step { FirstRun.keyLanded(it) }
    /** Debug builds: the stored state was set from outside (`./bl debug first …`). What stands follows it at once. */
    fun firstChanged() {
        val before = stage
        settings = app.prefs.now
        if (stage != null) resuggest()
        if (stage != before) stageArmed = FirstRun.armed(first())
    }
```

- [ ] **Step 2: The stage's height, in `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt`**

Find:

```kotlin
    val card = 96.dp
```

Make it:

```kotlin
    val card = 96.dp
    /** First run's stage under the field: 8 of air, a row's seat (56), a band of two rows for the keys (112), 8 of air (docs/design/first-run/design.md §4). With the field the panel is 252 dp. */
    val stage = 184.dp
```

- [ ] **Step 3: Write `FirstStage.kt`**

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

/**
 * First run's stage under the empty field, for the key's step (docs/design/first-run/design.md §4 and §5): a seat
 * like a row's (a mark, a title and a line, the counter where a row's kind stands), and under it a band with the
 * keys as large caps, one line of caption, and the answers at the right end. One skeleton for its four screens;
 * only the words and the band change, and they change where they stand. It is not a row: nothing of it can be
 * selected, and the field above it is live.
 */
@Composable
fun FirstStage(model: OverlayModel, on: FirstRun.Screen, onAnswer: (FirstRun.Answer) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val context = LocalContext.current
    val state = model.settings.firstRun()
    val words = SystemWords.words(context)
    val key = model.suggested
    val name = SystemWords.name(context, key)
    val action = stringResource(R.string.key_action)
    val second = scheme.onSurface.copy(alpha = SECOND)

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
    val answers = FirstRun.answers(state)
    val labels = answers.map {
        stringResource(when (it) {
            FirstRun.Answer.OPEN_HELPER -> R.string.card_shortcut_action
            FirstRun.Answer.GO_ON -> R.string.first_next
            FirstRun.Answer.CHANGE_KEY -> R.string.first_key_change
            else -> R.string.card_not_now
        })
    }
    val count = FirstRun.count(state)
    val counter = count?.let { stringResource(R.string.first_step, it.first, it.second) }

    // A screen reader is told each screen once, when it comes: where it is in the run, what it says, the keys, and how to act.
    val view = LocalView.current
    val keys = stringResource(R.string.a11y_first_keys, action, name)
    val act = stringResource(when (on) {
        FirstRun.Screen.K2 -> R.string.a11y_first_tab_opens
        FirstRun.Screen.K4 -> R.string.a11y_first_goes_on
        else -> R.string.a11y_first_opens
    })
    val said = listOfNotNull(counter, title, line, keys.takeIf { on == FirstRun.Screen.K1 }, caption.ifEmpty { null }, act).joinToString(". ") { it.trimEnd('.') } + "."
    LaunchedEffect(on) { view.announceForAccessibility(said) }

    Column(Modifier.fillMaxWidth().height(Metrics.stage).padding(vertical = Metrics.pad)) {
        // The seat: where a row would be. Its words roll when the screen changes, as a counter's do.
        Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(Metrics.row), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = if (dark) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
                Icon(Symbols.of("key"), null, Modifier.size(20.dp), tint = second)
            }
            Column(Modifier.weight(1f).padding(start = 16.dp, end = 16.dp)) {
                AnimatedContent(title, transitionSpec = { motion.roll() }, contentAlignment = Alignment.TopStart, label = "title") {
                    Text(it, color = scheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadeEnd())
                }
                AnimatedContent(line, transitionSpec = { motion.roll() }, contentAlignment = Alignment.TopStart, label = "line") {
                    Text(it, color = second, style = SMALL, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadeEnd())
                }
            }
            if (counter != null) Text(counter, color = second, style = SMALL, maxLines = 1, softWrap = false)
        }
        // The band: the keys from the titles' edge, the caption under them, the answers at the right end on the caps' centre line.
        val armed = model.stageArmed
        Row(Modifier.padding(start = 72.dp, end = 20.dp).fillMaxWidth().height(BAND), verticalAlignment = Alignment.CenterVertically) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                // The system's name for the key is used on the cap only where the cap then has room: a cap never shrinks or wraps.
                val measurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val room = with(density) { maxWidth.toPx() }
                val fallback = stringResource(if (key == FirstRun.Key.QUICK_INSERT) R.string.first_key_quick else R.string.first_key_letter)
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
                // Nothing is armed until Tab on "Now press your keys": a `tab` cap says so, 10 dp before the answers, and goes once one is.
                // (It lies over the band's free end and takes no room: the suggestion's caps have all of it.)
                val cap by animateFloatAsState(if (armed < 0) 1f else 0f, motion.fade(120), label = "cap")
                Box(Modifier.align(Alignment.CenterEnd).offset(x = 14.dp).graphicsLayer { alpha = cap }) { Keycap("tab") }
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(CAPTION_BAND), contentAlignment = Alignment.CenterStart) {
                    AnimatedContent(caption, transitionSpec = { (fadeIn(motion.fade(120)) togetherWith fadeOut(motion.fade(80))).using(null) }, contentAlignment = Alignment.CenterStart, label = "caption") {
                        Text(it, color = second, style = CAPTION, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadeEnd())
                    }
                }
            }
            Spacer(Modifier.width(24.dp))
            OptionStrip(labels, armed.coerceIn(0, (labels.size - 1).coerceAtLeast(0)), onChoose = { model.stageArm(it) },
                onRun = { model.stageArm(it); answers.getOrNull(it)?.let(onAnswer) }, vertical = true, lit = armed >= 0)
        }
    }
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

- [ ] **Step 4: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/FirstStage.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt
git commit -m "First run: the stage for the key's step: its state in the model, its height, its drawing (nothing shows it yet)"
```

### Task 8: The hold, the dialog's rows, the key landing, the icon's first click

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`

**Interfaces:**
- Consumes: core (Task 5): `FirstRun.Hold`, `Signal`, `hold(h, signal): Held`, `Start`, `fromIcon(s)`, `byKey(s)`, `iconShowsPanel(s, screenDp)`, `iconClicked(s)`, `Answer.OPEN_HELPER`. Model (Task 7): `stage`, `awaitsKey`, `suggested`, `resuggest()`, `answerStage(answer)`, `helperCame()`, `keyLanded()`. `SystemWords.load(context, scope)` and `SystemWords.rows(context, key)` (Task 6). In the activity: `first(app) { … }` and `screenDp` (Task 1), `stay`, `leaving`, `turn(intent)`, `close()`, `take(intent)`, `handOver()`, `created`, `EARLY_MS`.
- Produces: `fun stage(answer: FirstRun.Answer)` (public: the debug hooks call it; Task 9 hands it to the panel), `val held: String` (the hold's name, for the hooks), `OverlayActivity.lastStart: String` (companion, for the hooks). `fromIcon` and `byKey` keep their names and signatures and are now core's rules.

What changes for someone who never sees first run: a start is called a key by the positive sign instead of by a list of who is not one, and a key pressed with the panel open is noted before it puts the panel away. Nothing else: with no hold a lost focus closes the panel exactly as before, and nothing is written where nothing changes.

There is no unit test for this task: the app module has no test source set (`app/src` holds `main` and `debug`). What it decides is core's and is tested there; its check here is that it builds, and what it does is seen in Task 10's device checks.

- [ ] **Step 1: Eleven changes in `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Each "Find" stands in the file exactly once.

*The two imports.* Find:

```kotlin
import android.view.KeyEvent
```

Make it:

```kotlin
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.Menu
```

*The import of the words.* Find:

```kotlin
import io.github.kuscher.booklight.data.firstRun
```

Make it:

```kotlin
import io.github.kuscher.booklight.data.firstRun
import io.github.kuscher.booklight.device.SystemWords
```

*The hold's state.* Find:

```kotlin
    private var leaveJob: Job? = null
```

Make it:

```kotlin
    private var leaveJob: Job? = null
    /** First run's hold: the panel kept alive behind the system's Keyboard shortcuts dialog while it waits for its key (core `FirstRun.hold`). */
    private var hold = FirstRun.Hold.NONE
    /** The hold, by its name: for the debug hooks. */
    val held: String get() = hold.name
    /** This activity is the one in front. False while another has its place, if only for the moment the key's own start takes. */
    private var topResumed = true
    /** The start that was described last, and what was made of it ([start]). */
    private var described: Pair<Intent, FirstRun.Start>? = null
```

*The icon's first click, at the top of `onCreate`.* Find:

```kotlin
        super.onCreate(savedInstanceState)
        if (fromIcon(intent)) { handOver(); return }
        current = WeakReference(this)
        val app = application as BooklightApp
```

Make it:

```kotlin
        super.onCreate(savedInstanceState)
        val app = application as BooklightApp
        val screenDp = windowManager.maximumWindowMetrics.bounds.height() / resources.displayMetrics.density
        lastStart = said(intent)
        // A click on the icon opens the Booklight window. But the first one of a new installation that still asks for its key
        // shows the panel, once: whoever starts from the Apps list meets the key's step, not the settings.
        if (fromIcon(intent)) {
            if (!FirstRun.iconShowsPanel(app.prefs.now.firstRun(), screenDp)) { handOver(); return }
            first(app) { FirstRun.iconClicked(it) }
        }
        current = WeakReference(this)
```

*The screen's height is known earlier now.* Find:

```kotlin
        val screenDp = windowManager.maximumWindowMetrics.bounds.height() / resources.displayMetrics.density
        // First run, in this order
```

Make it:

```kotlin
        // First run, in this order
```

*The words are read when a stage is due.* Find:

```kotlin
        model = OverlayModel(app, lifecycleScope, limit = Metrics.maxRows(screenDp), screenDp = screenDp)
```

Make it:

```kotlin
        model = OverlayModel(app, lifecycleScope, limit = Metrics.maxRows(screenDp), screenDp = screenDp)
        // The system's own words for its dialog are read before the stage that quotes them comes.
        if (model.stage != null) SystemWords.load(this, app.scope)
```

*A new start while the panel is open.* Find:

```kotlin
        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_STAY, false)) { stay = true; return }
        if (fromIcon(intent)) { startActivity(Intent(this, MainActivity::class.java)); close(); return }
        if (leaving) { turn(intent); return }
```

Make it:

```kotlin
        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_STAY, false)) { stay = true; return }
        lastStart = said(intent)
        // (Asked now: inside this call the system says who sent this start; afterwards it says who sent the first one again.)
        val key = byKey(intent)
        if (fromIcon(intent)) { startActivity(Intent(this, MainActivity::class.java)); close(); return }
        // The user's key while first run waits for it, under the system's dialog or in view after it: it has landed, and the
        // panel stays. (Pressed while the panel was on its way out, it turns round first.)
        if (key && model.awaitsKey) { if (leaving) turn(intent); landed(); return }
        // A key nobody knew of, pressed with the panel open: it is known now. It puts the panel away, as every day.
        if (key) model.keyLanded()
        // Under the dialog nothing else puts the panel away.
        if (hold != FirstRun.Hold.NONE) return
        if (leaving) { turn(intent); return }
```

*What a start is: the icon, the key.* Find:

```kotlin
    /**
     * Was this start a click on the app's icon? Then the Booklight window is wanted, not the panel. The
     * launcher, the taskbar and the Apps list all belong to the home app, and they say where on screen the
     * icon was (source bounds). A keyboard shortcut, the assistant key, the widget, the tile and adb do neither.
     */
    private fun fromIcon(intent: Intent): Boolean {
        if (intent.action != Intent.ACTION_MAIN || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)) return false
        if (intent.sourceBounds != null) return true
        val home = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        return home != null && referrer?.host == home
    }

    /**
     * Was this start a keyboard shortcut or the assistant key? A shortcut starts the launcher activity, as "Open" in
     * the store, in the installer and in Settings' App info do: those say who they are, and they are not a key.
     */
    private fun byKey(intent: Intent): Boolean {
        if (BuildConfig.DEBUG && intent.getBooleanExtra("key", false)) return true
        if (intent.action == Intent.ACTION_ASSIST) return true
        if (intent.action != Intent.ACTION_MAIN || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)) return false
        val from = referrer?.host ?: return true
        val installer = runCatching { packageManager.getInstallSourceInfo(packageName).installingPackageName }.getOrNull()
        return from != installer && from != packageName && from !in NOT_A_KEY
    }
```

Make it:

```kotlin
    /**
     * This start as the system describes it (core `FirstRun.Start`): whether the launcher activity was asked for, who the
     * system says sent it, whether it says where on screen an icon was, whether it carries anything. Inside `onNewIntent`
     * the sender is that start's; outside it, the first start's. Worked out once for each start: who the home app is, is a
     * question to the system, and every opening passes here.
     */
    private fun start(intent: Intent): FirstRun.Start {
        described?.takeIf { it.first === intent }?.let { return it.second }
        val launcher = intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        // (Asked only where the start says nothing of an icon's place, as before.)
        val home = if (!launcher || intent.sourceBounds != null) null else packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        return FirstRun.Start(launcher, intent.action == Intent.ACTION_ASSIST, referrer?.host, intent.sourceBounds != null, intent.extras?.isEmpty == false, home).also { described = intent to it }
    }

    /**
     * Was this start a click on the app's icon? Then the Booklight window is wanted, not the panel. The
     * launcher, the taskbar and the Apps list all belong to the home app, and they say where on screen the
     * icon was (source bounds). A keyboard shortcut, the assistant key, the widget, the tile and adb do neither.
     */
    private fun fromIcon(intent: Intent): Boolean = FirstRun.fromIcon(start(intent))

    /**
     * Was this start a keyboard shortcut or the assistant key? A shortcut's start of the launcher activity comes from the
     * system itself and carries nothing (docs/research/first-run-proof.md, check 6); "Open" in the store, in the installer
     * and in Settings' App info, and another app, say who they are.
     */
    private fun byKey(intent: Intent): Boolean = (BuildConfig.DEBUG && intent.getBooleanExtra("key", false)) || FirstRun.byKey(start(intent))

    /** A start in a few words, for the debug hooks: what it asks for and who the system says sent it. Nothing of what it carries. */
    private fun said(intent: Intent) = "${intent.action?.substringAfterLast('.')} referrer=${referrer?.host} bounds=${intent.sourceBounds != null} extras=${intent.extras?.isEmpty == false} key=${byKey(intent)} icon=${fromIcon(intent)}"
```

*The focus, the hold's one place, the dialog's rows, the front gone for good.* Find:

```kotlin
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) model.focused = true
        if (hasFocus || stay || handedOver) return
        // Whatever started the panel (the taskbar, a widget, the Apps list) may still be closing and
        // take focus for a moment: early on, only close if focus is still gone a little later.
        if (SystemClock.uptimeMillis() - created < EARLY_MS) window.decorView.postDelayed({ if (!hasWindowFocus() && !isFinishing) close() }, 250)
        else close()
    }
```

Make it:

```kotlin
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (handedOver) return
        // The focus is back: the system's dialog has gone (closed by hand, or by the key that landed), and the hold is over.
        if (hasFocus) { model.focused = true; signal(FirstRun.Signal.FOCUS_BACK); return }
        if (stay) return
        // Behind the system's Keyboard shortcuts dialog the panel stays (first run's hold): the first focus lost after asking
        // for the dialog is the dialog. Without a hold a lost focus closes the panel, as every day.
        if (!signal(FirstRun.Signal.FOCUS_LOST)) return
        // Whatever started the panel (the taskbar, a widget, the Apps list) may still be closing and
        // take focus for a moment: early on, only close if focus is still gone a little later.
        if (SystemClock.uptimeMillis() - created < EARLY_MS) window.decorView.postDelayed({ if (!hasWindowFocus() && !isFinishing && hold == FirstRun.Hold.NONE) close() }, 250)
        else close()
    }

    /** The hold's one place: what a signal makes of it (core `FirstRun.hold`). True: the panel is to go. */
    private fun signal(s: FirstRun.Signal): Boolean {
        val held = FirstRun.hold(hold, s)
        hold = held.hold
        if (held.came) model.helperCame()
        return held.close
    }

    /**
     * While the panel is held, the system's Keyboard shortcuts dialog opens on a page of the panel's own: the five steps
     * to a key, with the suggested keys at each row's end (docs/design/first-run/design.md §5).
     */
    override fun onProvideKeyboardShortcuts(data: MutableList<KeyboardShortcutGroup>, menu: Menu?, deviceId: Int) {
        super.onProvideKeyboardShortcuts(data, menu, deviceId)
        if (hold != FirstRun.Hold.NONE) data.add(SystemWords.rows(this, model.suggested))
    }

    /**
     * Under the dialog the panel is told nothing but that it lost the focus. If another window then takes the front for
     * good (a click beside the dialog, another app's shortcut), nothing would ever end the hold: this does. The key's own
     * start also takes the front, for a few milliseconds: hence the wait.
     */
    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        topResumed = isTopResumedActivity
        if (!isTopResumedActivity && hold != FirstRun.Hold.NONE) window.decorView.postDelayed({ if (!topResumed && !isFinishing && signal(FirstRun.Signal.GONE)) close() }, GONE_MS)
    }
```

*An answer on the stage, and the key landing (after the old card's function, which Task 9 removes).* Find:

```kotlin
    /** A button on the first-run card. Either way the step is done and doesn't come back. */
    private fun card(card: Card, primary: Boolean) {
        if (leaving) return
        when (card) {
            Card.SHORTCUT -> {
                model.change { it.copy(shortcutCard = false) }
                if (primary) { requestShowKeyboardShortcuts(); close() }
            }
            Card.SUGGESTIONS -> model.change { it.copy(suggestionsCard = false, suggestions = primary) }
        }
    }
```

Make it:

```kotlin
    /** A button on the first-run card. Either way the step is done and doesn't come back. */
    private fun card(card: Card, primary: Boolean) {
        if (leaving) return
        when (card) {
            Card.SHORTCUT -> {
                model.change { it.copy(shortcutCard = false) }
                if (primary) { requestShowKeyboardShortcuts(); close() }
            }
            Card.SUGGESTIONS -> model.change { it.copy(suggestionsCard = false, suggestions = primary) }
        }
    }

    /**
     * An answer on first run's stage. "Open Keyboard shortcuts" is the window's to do: the system is asked for its dialog
     * and the panel holds on behind it, so that the dialog opens on the panel's own page and the panel is there, changed,
     * when the dialog goes. Every other answer only changes what is kept.
     */
    fun stage(answer: FirstRun.Answer) {
        if (leaving) return
        if (answer != FirstRun.Answer.OPEN_HELPER) { model.answerStage(answer); return }
        // The key is suggested for the keyboard this Enter came from: the dialog's rows carry it.
        model.resuggest()
        signal(FirstRun.Signal.ASK)
        requestShowKeyboardShortcuts()
        // No dialog a second later (the system showed none): the hold is over, and a lost focus closes the panel as every day.
        window.decorView.postDelayed({ signal(FirstRun.Signal.NO_DIALOG) }, NO_DIALOG_MS)
    }

    /**
     * The user's key has opened the panel while its step waited for it: "Your key works". The system closes its dialog by
     * itself when the new key is pressed; it is asked to as well, for a build that does not.
     */
    private fun landed() {
        model.keyLanded()
        dismissKeyboardShortcutsHelper()
        signal(FirstRun.Signal.KEY)
    }
```

*The companion: the old list goes, two times and the last start come.* Find:

```kotlin
        /** Who starts the launcher activity with a button of their own that says "Open". */
        private val NOT_A_KEY = setOf("com.android.vending", "com.google.android.packageinstaller", "com.android.packageinstaller", "com.android.settings", "com.android.shell")
```

Make it:

```kotlin
        /** How long the system has to show its Keyboard shortcuts dialog before the hold gives up waiting for it. */
        private const val NO_DIALOG_MS = 1000L
        /** How long another window must have had the front before a held panel goes: the key's own start has it for a few ms. */
        private const val GONE_MS = 300L
        /** The last start of the panel, in a few words ([said]): for the debug hooks. */
        var lastStart = ""
```

- [ ] **Step 2: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt
git commit -m "First run: the panel holds on behind the system's dialog, gives it its five rows, and knows its key when it lands"
```

### Task 9: The stage takes the cards' place

**Files:**
- Modify: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`, `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt`, `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`, `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`, `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml`

**Interfaces:**
- Consumes: `FirstStage(model, on, onAnswer)`, `Metrics.stage`, the model's `stage`, `stageEnter()`, `stageTab(back)`, `keyboard` (Task 7); `OverlayActivity.stage(answer)` (Task 8).
- Produces: `Panel(…, onRun, onStage: (FirstRun.Answer) -> Unit, onClose)` in place of `onCard`. Gone: `enum class Card`, `OverlayModel.card`, `CardBody`, `OverlayActivity.card`, the debug hooks `pref cards` and `pref nocards`, and four strings. The panel is 252 dp while a screen of the key's step stands (`Metrics.height`).

From this commit on the key's step is what a new installation sees. Keys on it: a letter is typed in the same frame and the list takes the stage's place (the field emptied, the stage is back); Enter runs the armed answer and does nothing where none is armed; Tab and Shift + Tab arm the next and the one before; Esc and a click outside close; the pointer arms an answer by moving onto it and runs it by a click.

There is no unit test for this task: the app module has no test source set (`app/src` holds `main` and `debug`). What it decides is core's and is tested there; its check here is that it builds, and what it does is seen in Task 10's device checks.

- [ ] **Step 1: `app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt`.** Ten changes; each stands in the file exactly once. The last removes `CardBody`, the file's last function.

Find:

```kotlin
import io.github.kuscher.booklight.core.Body
```

Make it:

```kotlin
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.FirstRun
```

Find:

```kotlin
    onCard: (Card, primary: Boolean) -> Unit,
```

Make it:

```kotlin
    /** An answer on first run's stage. */
    onStage: (FirstRun.Answer) -> Unit,
```

Delete (it stands in the file exactly once):

```kotlin
    var cardChoice by remember { mutableIntStateOf(0) }
```

Delete (it stands in the file exactly once):

```kotlin
    LaunchedEffect(model.card) { cardChoice = 0 }
```

Find:

```kotlin
        val card = model.card
```

Make it:

```kotlin
        // (Which keyboard the keys come from says which key first run suggests.)
        model.keyboard = e.nativeKeyEvent.deviceId
        val stage = model.stage
```

Find:

```kotlin
if (!again) { if (card != null) onCard(card, cardChoice == 0) else if (model.tip != null)
```

Make it:

```kotlin
if (!again) { if (stage != null) model.stageEnter()?.let(onStage) else if (model.tip != null)
```

Find:

```kotlin
                    card != null -> if (!again) cardChoice = 1 - cardChoice
```

Make it:

```kotlin
                    stage != null -> if (!again) model.stageTab(e.isShiftPressed)
```

Find:

```kotlin
                        model.card != null -> "card:${model.card}"
```

Make it:

```kotlin
                        model.stage != null -> "first"
```

Find:

```kotlin
                                state.startsWith("card") -> model.card?.let { CardBody(it, model, cardChoice, onChoice = { c -> cardChoice = c }, onCard = onCard) }
```

Make it:

```kotlin
                                state == "first" -> model.stage?.let { FirstStage(model, it, onStage) }
```

Delete, with the empty line before it (it stands in the file exactly once):

```kotlin
/** The first-run card: one small step under the field, only while nothing is typed. Its two answers are one option strip. */
@Composable
private fun CardBody(card: Card, model: OverlayModel, choice: Int, onChoice: (Int) -> Unit, onCard: (Card, Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val engine = model.settings.engine()
    val (title, text, yes, no) = when (card) {
        Card.SHORTCUT -> listOf(stringResource(R.string.card_shortcut_title), stringResource(R.string.card_shortcut_text),
            stringResource(R.string.card_shortcut_action), stringResource(R.string.card_done))
        Card.SUGGESTIONS -> listOf(stringResource(R.string.card_suggest_title), stringResource(R.string.card_suggest_text, engine.name),
            stringResource(R.string.card_suggest_on), stringResource(R.string.card_not_now))
    }
    Row(
        Modifier.padding(horizontal = Metrics.pad).fillMaxWidth().height(Metrics.card)
            // No plate of its own: a box inside the glass would read as a second rim.
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Symbols.booklight, null, Modifier.size(20.dp), tint = scheme.primary)
        }
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 14.dp)) {
            Text(title, color = scheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight(600)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text, color = scheme.onSurface.copy(alpha = SECOND), style = TextStyle(fontFamily = Fonts.text, fontSize = 13.sp, fontWeight = FontWeight(500), lineHeight = 17.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        OptionStrip(listOf(yes, no), choice, onChoose = onChoice, onRun = { onCard(card, it == 0) }, vertical = true)
    }
}
```

- [ ] **Step 2: `app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt`**

Find:

```kotlin
        m.card != null || m.tip != null -> card + pad
```

Make it:

```kotlin
        m.stage != null -> stage
        m.tip != null -> card + pad
```

- [ ] **Step 3: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt`.** Six changes: the enum and the derived `card` go, and the four places that asked `card` ask `stage`.

Delete, with the empty line after it (it stands in the file exactly once):

```kotlin
/** The small first-run card under the field: one step at a time, only while nothing is typed. */
enum class Card { SHORTCUT, SUGGESTIONS }
```

Delete, with the empty line after it (it stands in the file exactly once):

```kotlin
    /** The first-run step to show, if any is left and nothing is typed. */
    val card: Card? by derivedStateOf {
        when {
            demo || guided || query.isNotEmpty() || chip != null -> null
            // "Give Booklight a key": not for someone whose key has already opened the panel.
            settings.shortcutCard && !settings.keySeen -> Card.SHORTCUT
            settings.suggestionsCard && !settings.suggestions -> Card.SUGGESTIONS
            else -> null
        }
    }
```

Find:

```kotlin
|| results.isNotEmpty() || !settings.tips || card != null) return
```

Make it:

```kotlin
|| results.isNotEmpty() || !settings.tips || stage != null) return
```

Find:

```kotlin
if (Under.choose(untouched, card != null, fresh,
```

Make it:

```kotlin
if (Under.choose(untouched, stage != null, fresh,
```

Find:

```kotlin
|| chip != null || results.isNotEmpty() || card != null) return false
```

Make it:

```kotlin
|| chip != null || results.isNotEmpty() || stage != null) return false
```

Find:

```kotlin
results.isEmpty() && card == null && tip == null
```

Make it:

```kotlin
results.isEmpty() && stage == null && tip == null
```

- [ ] **Step 4: `app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt`.** Two changes: the panel is handed `stage`, and the card's function goes.

Find:

```kotlin
onRun = ::run, onCard = ::card, onClose = ::close,
```

Make it:

```kotlin
onRun = ::run, onStage = ::stage, onClose = ::close,
```

Delete, with the empty line after it (it stands in the file exactly once):

```kotlin
    /** A button on the first-run card. Either way the step is done and doesn't come back. */
    private fun card(card: Card, primary: Boolean) {
        if (leaving) return
        when (card) {
            Card.SHORTCUT -> {
                model.change { it.copy(shortcutCard = false) }
                if (primary) { requestShowKeyboardShortcuts(); close() }
            }
            Card.SUGGESTIONS -> model.change { it.copy(suggestionsCard = false, suggestions = primary) }
        }
    }
```

- [ ] **Step 5: `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`.** Six changes: the two hooks for the cards and their mentions go, and `dump` says what is armed on the stage.

Find:

```kotlin
if (v.isNotEmpty() || k == "cards" || k == "nocards") when (k) {
```

Make it:

```kotlin
if (v.isNotEmpty()) when (k) {
```

Delete (it stands in the file exactly once):

```kotlin
                    "cards" -> app.prefs.update { it.copy(shortcutCard = true, suggestionsCard = true, suggestions = false, keySeen = false) }
```

Delete (it stands in the file exactly once):

```kotlin
                    "nocards" -> app.prefs.update { it.copy(shortcutCard = false, suggestionsCard = false) }
```

Delete this piece of its line (it stands in the file exactly once; the rest of the line stays):

```kotlin
 cards=${it.shortcutCard},${it.suggestionsCard}
```

Delete this piece of its line (it stands in the file exactly once; the rest of the line stays):

```kotlin
 | pref cards (show the first-run cards again) | pref nocards
```

Find:

```kotlin
card=${m.card} first=${m.stage?.name ?: "none"}
```

Make it:

```kotlin
first=${m.stage?.name ?: "none"}${if (m.stage != null) " armed=" + m.stageArmed else ""}
```

- [ ] **Step 6: The cards' strings.** In `app/src/main/res/values/strings.xml` and in `app/src/main/res/values-de/strings.xml`, delete the four lines that begin

```xml
    <string name="card_shortcut_text">
    <string name="card_suggest_title">
    <string name="card_suggest_text">
    <string name="card_suggest_on">
```

(`card_shortcut_title`, `card_shortcut_action` and `card_not_now` are the stage's now; `card_done` stays for part 3.) Then:

```bash
grep -rnE "card_shortcut_text|card_suggest_|cardChoice|CardBody|Card\.SHORTCUT|model\.card([^A-Za-z]|$)|[^a-z]m\.card([^A-Za-z]|$)" app/src || echo CLEAN
grep -rn "onCard" app/src/main/java/io/github/kuscher/booklight/overlay || echo CLEAN
```

Expected: `CLEAN`, twice. (The settings window has an `onCard` of its own, in `window/Controls.kt`: it is another thing and stays.)

- [ ] **Step 7: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/io/github/kuscher/booklight/overlay/Panel.kt app/src/main/java/io/github/kuscher/booklight/overlay/Metrics.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayModel.kt app/src/main/java/io/github/kuscher/booklight/overlay/OverlayActivity.kt app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "First run: the key's step stands under the empty field, where the two cards stood"
```

### Task 10: The hooks, and the checks on a device

**Files:**
- Modify: `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`
- Modify: `CLAUDE.md`
- Create: `docs/research/first-run-key.md`

**Interfaces:**
- Consumes: `OverlayModel.stage`, `stageArmed`, `stageTab(back)`, `stageEnter()`, `firstChanged()` (Task 7); `OverlayActivity.stage(answer)`, `held`, `OverlayActivity.lastStart` (Task 8); `SystemWords.read` and `SystemWords.load(context, scope)` (Task 6); the `first` case of `DebugReceiver` as part 1 left it.
- Produces: `./bl debug first …` moves an open panel at once and also prints `words=`, `hold=` and the last start; `./bl debug key tab|backtab|enter` act on the stage as the keys do. With these every K screen and both ways back from the dialog are reached without a new installation and without the dialog: `first at k1|k2|k3`, `first helper` (back without a key), `first key` (the key landed), `./bl open key` (a start by the key, into an open panel or a new one). The list of checks for the coordinator, with a place for each answer.

**The engineer builds and does not touch a device: the coordinator runs the checks and writes the answers into the file.**

There is no unit test for this task: the app module has no test source set (`app/src` holds `main` and `debug`). What it decides is core's and is tested there; its check here is that it builds.

- [ ] **Step 1: The hooks, in `app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt`.** Six changes; each "Find" stands in the file exactly once.

Find:

```kotlin
                    else -> return out("new | update | off | replay [show] | resume | key | helper | opened | shown | answer NAME | ran open|search|sum | at SCREEN | older")
                }
```

Make it:

```kotlin
                    else -> return out("new | update | off | replay [show] | resume | key | helper | opened | shown | answer NAME | ran open|search|sum | at SCREEN | older")
                }
                // An open panel follows at once: the screen that now stands is drawn, without a new opening.
                main.post { act?.model?.firstChanged() }
```

Find:

```kotlin
.ifEmpty { "none" }}")
            }
```

Make it:

```kotlin
.ifEmpty { "none" }} | " +
                    "words=${io.github.kuscher.booklight.device.SystemWords.read ?: "not read"} | hold=${act?.held ?: "no panel"} | last start: ${OverlayActivity.lastStart.ifEmpty { "none in this process" }}")
            }
```

Find:

```kotlin
"tab" -> if (m.tip != null) m.tipTab()
```

Make it:

```kotlin
"tab" -> if (m.stage != null) m.stageTab(false) else if (m.tip != null) m.tipTab()
```

Find:

```kotlin
"backtab" -> if (m.opened != null) m.step(-1)
```

Make it:

```kotlin
"backtab" -> if (m.stage != null) m.stageTab(true) else if (m.opened != null) m.step(-1)
```

Find:

```kotlin
"enter" -> if (m.tip != null) m.tipEnter() else m.enter { r, a -> act.run(r, a) }
```

Make it:

```kotlin
"enter" -> if (m.stage != null) m.stageEnter()?.let { act.stage(it) } else if (m.tip != null) m.tipEnter() else m.enter { r, a -> act.run(r, a) }
```

Find:

```kotlin
                main.post { act?.model?.firstChanged() }
```

Make it:

```kotlin
                main.post { act?.model?.firstChanged(); io.github.kuscher.booklight.device.SystemWords.load(context, app.scope) }
```

- [ ] **Step 2: Say in `CLAUDE.md` that the key's step is drawn**

Find:

```markdown
who gets a run at all; nothing of it is drawn yet),
```

Make it:

```markdown
who gets a run at all; the key's step is drawn by `overlay/FirstStage.kt`, and the rest is to come),
```

- [ ] **Step 3: Write `docs/research/first-run-key.md`**

```markdown
# First run: the key's step on a device (part 2)

*The checks for part 2 of the first-run plan (`docs/superpowers/plans/2026-10-04-first-run-2-the-key.md`). Whoever
runs a check writes its answer under it, with the date and "the Lenovo Googlebook" or "the HP Googlebook". Until
then an answer reads "not run".*

**Before the first check**

- Choose the device by its model (`adb devices -l`), never by its place in the list; set `ANDROID_SERIAL`.
- Close the Booklight window, then `./bl install`. `./bl idle` before every `./bl open`: the panel takes the keyboard.
- Never `uiautomator dump`. No sign-in is made for a check. A capture of the screen shows real windows: none is
  kept or committed; `./bl shot NAME` is the panel's own window.
- This file is public: no serial, no build number, nothing about what is installed. A referrer is written as its
  kind (the system, the home app, the store, Settings, adb), not as another app's package name.
- `./bl debug first` prints first run's stored state, the screen that stands, the suggested key, the system's
  words as they were read, the hold, and the last start. `./bl debug pref key no` makes Booklight forget that it
  has a key (the device's own shortcut stays as it is); `./bl debug pref key seen` puts that back.

## 1. K1, as a still

`./bl debug first new`, `./bl debug pref key no`, `./bl open stay`, `./bl debug dump`, `./bl shot k1`; again
with `DARK=true ./bl open stay`.

Look, at four times the size: the panel is 252 dp high; the seat's mark on the field's mark's line (x = 38); the
title, the line, the first cap and the caption on one left edge (x = 72); the counter "1 of 5", the `esc` cap and
the answers on one right edge (x = 700); the caps `Action` + `Quick Insert` flat, with their white ring, over a
white window and a dark one; "Open Keyboard shortcuts" armed. In the dump: `first=K1 armed=0 1/5`.

Answer: not run

## 2. K2, K3, K4, by the hooks

With the panel open: `./bl debug first at k2`, `./bl shot k2`; `first at k3`, `shot k3`; `./bl debug first key`,
`shot k4`. K2: "your key" blank, nothing armed, a `tab` cap before the answers. K3: "Open Keyboard shortcuts"
armed, the caption "Or try Action + M". K4: the check in the key, "Go on" armed, no caption. Does "your key"
read as a key that waits, or as an empty box? Does anything move that should stand (the answers' right edge, the
seat)?

Answer: not run

## 3. Keys on the stage

On K2: `./bl debug key enter` (nothing; the dump is unchanged), `key tab` (`armed=0`), `key tab` (`armed=1`),
`key backtab` (`armed=0`). `./bl debug keys ch`: the list comes in the stage's place and the letters are there
in the same frame; `./bl debug type` with nothing after it: the stage is back. Esc and a click outside close;
the next opening shows the same screen.

Answer: not run

## 4. The system's words, read without a line in `<queries>`

`./bl debug first`: `words=Read(customize=…, set=…, quick=…)`. Are all three there, and the system's own? With
the system in German (if it can be set): „Anpassen“, „Speichern“, „Schnelles Einfügen“? With Booklight set to
the other language than the system: still the system's? If `words=not read` on a device, a line for
`com.android.systemui` goes into the main manifest's `<queries>`, and this check is run again.

Answer: not run

## 5. The real dialog, and back without a key

On K1, Enter. Does the system's dialog open on Booklight's page, with five rows that quote "Customize" and "Set
shortcut" as the dialog's own buttons say them and end in the suggested keys? `./bl debug first`: `hold=UNDER`,
`helper=1`. Esc: K2 stands, `hold=NONE`, a typed letter lands. Enter by Tab on "Open Keyboard shortcuts", Esc
again: K3, `helper=2`.

Answer: not run

## 6. The key lands under the dialog

From K1: Enter, then in the dialog its five steps (or, where the device already has Booklight's shortcut, just
its keys). Does the dialog go by itself, and is the panel there with "Your key works", the check, "Go on"
armed? Which screen is in the first frame that shows: K4, or K2 for a frame? `./bl debug first`: `key=true`,
`screen=K4`, `hold=NONE`, and the last start: `MAIN referrer=android bounds=false extras=false key=true
icon=false`.

Answer: not run

## 7. The key in view, and a key held

`./bl debug pref key no`, `./bl debug first at k2`, the panel open and in front: press the keys. K4, and the
panel stays. Hold the keys for a second on K4: does the panel go once (as every day), or flicker?

Answer: not run

## 8. "Go on" and "Not now"

On K4, Enter: the bare field at 68 dp; `./bl debug first` says `done=[key]`, `screen=L2`, and the dump
`first=none 2/5` (the lessons are not drawn yet: nothing stands). The next opening: nothing of first run; a
tip comes as it always did. Then `first new`, `pref key no`, open, Tab, Enter on "Not now": the bare field;
`run=NONE mark=true`.

Answer: not run

## 9. What ends the hold

With the dialog over the held panel, each from a fresh K1: a click beside the dialog; a click on another
window; another app's keyboard shortcut; the lock screen. Each time: is the panel gone afterwards (`./bl debug
dump` says `no panel`) or in front with K2, and never left on the desk without the keys? In `./bl logs`, is
anything said twice?

Answer: not run

## 10. The icon's first click

`./bl debug first new`, `./bl debug pref key no`, no panel open. Click Booklight's icon: the panel with K1.
Click it again: the Booklight window. `./bl debug first`: `icon=true`. Does the launcher's own closing close
the panel in its first second?

Answer: not run

## 11. Other starts (the proof's check 8)

Each once, then `./bl debug first` for the line `last start:`: "Open" on Booklight's App info page in Settings;
the home screen's widget and the Quick Settings tile, if they are placed; the keys with no panel open. Is the
key the only one that says `key=true`?

Answer: not run

## 12. A keyboard without Quick Insert

With an external keyboard attached: `./bl debug first`, `keyboards=`: does the external one lack `+quick`? Open
the panel on K1 and press Tab on the external keyboard, then Enter on "Open Keyboard shortcuts": do the dialog's
rows end in Action + M, and does row 3 say M? `./bl debug first at k3`: is the caption "or any keys you like"?
(Until this is seen, "Action + M for a keyboard without the key" rests on tests alone.)

Answer: not run

## 13. Is Quick Insert the key left of A?

By eye, on the keyboard, for Alex to confirm: K1's caption says so.

Answer: not run

## 14. German

Booklight in German: K1 to K4, nothing cut, the caption „… liegt links neben A · oder Tasten deiner Wahl“ in
its room, the answers „Tastenkürzel öffnen“ and „Nicht jetzt“. With the system in German: does the cap read
„Schnelles Einfügen“ and fit, or does it fall back; the dialog's rows in German, all five in view?

Answer: not run

## 15. An installation that was there before

`./bl debug first update`, `./bl debug pref key no`, open: K1 with no counter. Its "Go on" after the key ends
the run (`run=NONE`).

Answer: not run

## 16. The same answer twice

`./bl debug first update`, `./bl debug first answer not_now`, `./bl debug first older`: `run=NONE mark=true`
both times. `./bl debug first update`, `./bl debug first helper`, `./bl debug first older`: `run=UPDATE
helper=0`, K1.

Answer: not run

## 17. Animations off, and a screen reader

With the system's animations off: every K screen whole in one frame, K4 with its check. With a screen reader on:
each screen said once when it comes (the counter, the title, the line, the keys, the caption, how to act).

Answer: not run

## 18. Nobody else sees a change

`./bl debug first off`, `./bl debug pref key seen`: open and close by the keys ten times; a tip, the copy's
line and "your usual" come as before; no card ever comes; `./bl debug pref` before and after says the same but
for what was set here.

Answer: not run

## 19. The HP Googlebook, with Alex

Checks 1, 5, 6, 9 and 10 again there.

Answer: not run
```

- [ ] **Step 4: Run every core test**

```bash
./bl test
```

Expected: no output, exit status 0.

- [ ] **Step 5: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 6: Commit**

```bash
git add app/src/debug/java/io/github/kuscher/booklight/DebugReceiver.kt CLAUDE.md docs/research/first-run-key.md
git commit -m "First run: the hooks reach every screen of the key's step, and the checks to run on a device"
```

- [ ] **Step 7: Hand over.** Report to the coordinator: the commit, that the debug APK is at `app/build/outputs/apk/debug/app-debug.apk`, and that the nineteen checks are in `docs/research/first-run-key.md`. Do not install it and do not run a check.

### Task 11: Part 2, checked as a whole

**Files:** none are changed. If a step shows a change that is not committed, it belongs to the task that made it: say which, and stop.

**Interfaces:**
- Consumes: everything above.
- Produces: the report that part 2 is whole.

- [ ] **Step 1: Every core test, with its count**

```bash
./gradlew :core:test --tests 'io.github.kuscher.booklight.core.FirstRun*' --console=plain && ./bl test && grep -h -o 'tests="[0-9]*"' core/build/test-results/test/TEST-io.github.kuscher.booklight.core.FirstRun*.xml
```

Expected: `BUILD SUCCESSFUL`, no output from `./bl test`, then nine lines, one for each test class in the order of their names (Answers, Gate, Hold, KeyStep, Lessons, Openings, Start, `FirstRunTest`, Update): `tests="14"`, `tests="6"`, `tests="12"`, `tests="8"`, `tests="10"`, `tests="9"`, `tests="10"`, `tests="13"`, `tests="10"`. 92 in all, 26 of them this part's.

- [ ] **Step 2: Build, and look for both kinds of failure**

```bash
./bl build > /tmp/booklight-build.log 2>&1; grep -nE "FAILURE|e: " /tmp/booklight-build.log || echo BUILT
```

Expected: the one word `BUILT`. Any line with `FAILURE` or `e: ` is a broken build (a resource error has no `e:` line, and the old APK would install).

- [ ] **Step 3: What must be gone is gone, and the rules hold**

```bash
grep -rn "THE PROOF\|Proof\.\|proof-\|cardChoice\|CardBody\|enum class Card\|NOT_A_KEY" app/src bl || echo CLEAN
grep -rn "^import android" core/src/main || echo CLEAN
grep -rn "withFirstRun(" app/src | grep -v "fun Settings.withFirstRun\|fun Prefs.firstRun\|fun Settings.asUpdate\|^app/src/debug" || echo CLEAN
grep -n "com.android.systemui" app/src/main/AndroidManifest.xml app/src/debug/AndroidManifest.xml || echo CLEAN
git log -n 10 --format=%B | grep -inE "co-authored-by|claude-session|generated with" || echo CLEAN
git status --short
```

Expected: five times `CLEAN` (no proof and no card; no Android in core; no write of first run's state in the app but through `Prefs.firstRun`; no line for the system UI in a manifest; no attribution), and no line from `git status`.

- [ ] **Step 4: Report.** The commits of Tasks 1 to 10 by their first lines, the results above, and that the branch was not pushed.

---

## The parts that follow

| Part | What it builds | What it needs from this part |
| --- | --- | --- |
| **3. The lessons, the question, the choices, the fold** | L2 to L4 on the same skeleton, with practice Enters, the sum that stays, the coach line and the placeholder for each step; Q with nothing armed; C with the panel's switch; the fold. It takes the limit "only the key's step stands" out of `OverlayModel.stage` and out of `OverlayActivity.onCreate`'s count | `FirstStage`'s seat, band, caps and answers to build on; the model's `stageArmed`, `stageTab`, `stageEnter`, `answerStage`; `Metrics.stage`; the order of `onCreate`; `card_done` |
| **4. The welcome and the show** | The welcome in a 468 dp glass, the performed show with the example flight, the first opening at Slow, the landing in K1 | `FirstRun.opening` already applied before the model is built: `FirstRun.overture` and `slow` are asked after it, in `onCreate`; `FirstStage` to land in |
| **5. Motion, the replay, the documents, the device pass** | `motion.md`'s transitions (the large cap pressed and released, the reveal's lap of light); "First steps" as a command and as a row on the window's Start page, with the word "Not now" leaves behind; Action + Quick Insert on the Start page and in the README; the documents; every open device check | `FirstRun.answers` with `CHANGE_KEY` for a replay (already drawn); the hold and `landed()` as the reveal's trigger; `docs/research/first-run-key.md`'s answers |
