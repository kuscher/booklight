# First run: what is built, and what was decided on the way

*Kept by whoever coordinates the build. The plan's parts are in `docs/superpowers/plans/` (`2026-10-04-first-run…`).
All five parts are built. Nothing of this branch (`first-run`) is pushed or released.*

## Part 1: the proof and the logic (done, 5 October 2026)

- **The proof on a device** (`docs/research/first-run-proof.md`): the real panel, held, has its own page in the
  system's Keyboard shortcuts dialog with the five rows; it gets the focus and the keys back when the dialog goes;
  a key that lands is told to it and the dialog goes by itself; the system's own words and the key's name can be
  read; a tall glass redrawn every frame holds 120 frames a second on the Lenovo Googlebook. The proof's code is
  debug-only, marked `THE PROOF`, and goes in part 2; a copy of its build is kept outside git.
- **The logic**: core `FirstRun.kt` with six test classes (66 tests): the steps and the screen that stands, what
  an answer changes (suggestions go on by "Agree" on the question and by nothing else), the key landing, an
  unfinished run, a replay, who counts as an update, practice lessons, the opening shown once, the key by keyboard.
- **The stored state**: five fields in `Settings`, schema 7, and a migration that reads only what every build
  keeps, so it gives the same answer after an older build wrote the file. Every write of first run's state goes
  through `Prefs.firstRun { … }`, never from a snapshot.
- **Hooks**: `./bl debug first …` shows and sets the state; `OverlayModel.stage` says which screen stands.
  Nothing draws it yet: a user sees no change.
- Each of the 14 tasks was reviewed; a review of the whole part found nothing critical and asked for six small
  fixes, which are in.

## Part 2: the key's step (done, 5 October 2026)

- **What a user sees now**: a new installation, and an update that has no key yet, find the key's step under the
  empty field where the two cards stood: K1 suggests Action + Quick Insert and offers "Open Keyboard shortcuts"
  and "Not now"; behind the system's dialog the panel holds on and says "Now press your keys"; a key that lands
  shows "Your key works" with its check; "Go on" leaves the bare field. The lessons, the question, the choices,
  the welcome and the motion are not built yet. Someone who has a key and no run sees no change.
- **The dialog carries the instructions**: while the panel holds, the system's Keyboard shortcuts dialog opens on
  Booklight's own page with five rows that quote the system's own words for its two buttons and end in the
  suggested keys. The words are read from the system as any app reads a label: no permission, no `<queries>` line.
- **The rules are core's**, tested (92 first-run tests in nine classes): what stands at an opening, the answers
  and which one Enter runs, the caption, the hold and what ends it, what counts as the key, the icon's first click.
- **The proof is gone** from the code; what it found is in `docs/research/first-run-proof.md`. The two cards, their
  hooks and four of their strings are gone.
- **Hooks**: `./bl debug first …` moves an open panel at once and says the words read, the hold and the last
  start; `./bl debug key tab|backtab|enter` act on the stage.
- **On the Lenovo Googlebook** (`docs/research/first-run-key.md`, twenty-one checks): the four screens in both themes
  and in German, the keys on the stage, the real dialog and back without a key, the key landing under the dialog
  and in view, "Go on" and "Not now", an update's run, the same answer twice, animations off, ten openings with no
  run, and the dialog's own steps (Customize, the name, the dialog for the keys) over the held panel. One fault was
  found there and fixed (a panel opened to stay did not hear of the dialog).
- Each of the 10 tasks was reviewed, and the fixes those reviews asked for are in. A review of the whole part found
  nothing critical; what it asked for is in too: a panel in which no screen can stand (it carries another app's
  text, or an example is typed into it) no longer uses up one of an unfinished run's three openings, and the large
  cap has a short word of its own to fall back on.

## Part 3: the lessons, the question, the choices, the fold (done, 5 October 2026)

- **What a user sees now**: after "Go on" a new installation goes on in the same opening: three lessons that are
  practice (open an app, search inside an app, a sum), then the question whether searches may be suggested (only
  while suggestions are off), then two choices ("Show your usual" with a switch, and the list of everything), then
  the bare field. An unfinished run comes back where it stopped, in three openings in all, then waits. An
  installation that was there before gets the key's step alone, as in part 2. There is no welcome, no show and no
  new motion yet (parts 4 and 5), and no "First steps" to run it again (part 5).
- **Nothing opens in a lesson.** Enter on an app's row and on a search inside an app is practice: the slot shows as
  pressed, the footer says "That opens …" or "That searches …", and half a second later the next lesson stands. The
  sum is copied for real and the panel stays. It is decided in one place, `OverlayActivity.run`, by core's tested
  `FirstRun.enters`, before the executor is asked.
- **A lesson teaches by this device's own example**: an app that can be searched, found by the first three or four
  letters of its name; where there is none, Settings stands in; else the keys alone. While the user types, the
  lesson's stage gives way to the real list, and the lesson goes on as the placeholder and as a coach line at the
  footer's left end.
- **The question is consent**: nothing is armed, Enter alone does nothing, and suggestions go on by "Agree" and by
  nothing else (core, tested). A screen takes a key or a click only once it has been in view for a moment, and the
  question's first answer is "Not now": Tab, then Enter, the keys that skip a lesson, agree to nothing.
- **The choices are a real list** of two rows with nothing selected; however they are left, the run is done and the
  glass folds to the bare field.
- **The rules are core's**, tested: 115 first-run tests in eleven classes (667 core tests in all).
- **On the Lenovo Googlebook** (`docs/research/first-run-lessons.md`, twenty-nine checks): every screen in English
  and German, the practice Enters with nothing opened, the sum, two Enters in one turn, typing on, the question's
  keys and both answers, keys pressed blind, the choices and their switch, the fold, one opening from "Your key
  works" to the bare field, three openings and then waiting, animations off, and nobody else seeing a change.
- Each of the 13 tasks was reviewed, and what the reviews asked for is in. A review of the whole part found
  nothing critical and one thing that mattered: Tab, Enter pressed four times over went skip, skip, skip, "Agree".
  The two changes named above answer it, with a test in core and a check with real keys on the device.

## Part 4: the welcome and the show (done, 5 October 2026)

- **What a user sees now**: a new installation's very first opening runs at Slow, once, and begins with the opening
  piece. The glass grows to 468 dp and goes to night; a lamp strikes at the caret; a seam of light opens into the
  plain shaft; "Welcome to Booklight" and its line stand in the light; a knife opens five tools (an app, a sum, a
  plane, an emoji, a key) and its handle says "Show off" with the Enter mark. Enter, or Space, and the handle
  becomes row one's highlight and Booklight performs with its own panel: one letter and five apps of the device,
  the highlight down to the third row and along its stops, a sum that grows to 180, an example flight, the emoji
  grid. Then the one highlight travels into "Open Keyboard shortcuts" and the key's step stands, as part 2 built
  it. There is no new motion between the screens after that yet, and no "First steps" (part 5).
- **The show runs nothing.** Its rows are the engine's own rows for the letter, the calculator's row and the emoji
  scope's grid, each carrying an action that does nothing; the flight is data of its own, marked "Example", and its
  footer says so. Nothing is opened, looked up or fetched, and nothing of the device is kept or written.
- **Every key is the piece's first.** Enter and Space are the cue in the welcome and set the key's step down in the
  show; Esc, Tab, the arrows and Backspace set it down; a typed character ends the piece and is the whole text;
  Shift, the lock keys and Quick Insert alone do nothing; the system's own keys pass. A click on the handle is the
  cue, a click elsewhere on the glass sets the key's step down (clicks were not run on a device). The key's step
  takes a key 350 ms after it landed, so no press made for the piece asks for the system's dialog.
- **It is shown once**: marked when it ends or has played for two seconds from the glass being open. A panel that
  closes sooner plays it again; a panel that closes ends its piece.
- **Where it does not play** (animations off, a screen reader, a screen lower than 655 dp), the title greets as the
  placeholder over the key's step, and the piece is not used up. In a right-to-left layout it does not play either.
- **The glass's rules are lifted for the welcome alone** (night, light, large type, a drawn knife), by his word;
  they hold again from the show's first frame. Still true throughout: the window is never resized in width, and
  nothing is drawn outside the glass.
- **The rules are core's**, tested: what the opening is, what a press does while it plays, the show's script as
  data (124 first-run tests in twelve classes, the show's twelve, 688 core tests in all).
- **Hooks**: `./bl debug first welcome [at MS]`, `first show [at NAME]`, `first land`; `dump` says `playing=`,
  `cast=`, `laps=`, `gliding`.
- **On the Lenovo Googlebook** (`docs/research/first-run-welcome.md`, thirty checks): eight stills of the welcome
  against the paper's moments in both themes and in German, a real first opening filmed and its frames timed (1 %
  late in a debug build), the show's eight stills, the landing, every key in the welcome and in the show with real
  keys, Enter pressed six and eight times over and held, closing early and late, animations off, a replay, and
  nobody else seeing a change. One fault was found there and fixed (a search still on its way when the piece began
  landed among the show's rows).
- Each of the 12 tasks was reviewed, and what the reviews asked for is in. A review of the whole part found nothing
  critical and three things that mattered, all answered: the input method's report of the caret could end the show
  at its first letter; a piece ended before the glass was open left the welcome's caret on the glass; and Space,
  the key people press at "continue", typed a blank and left a bare glass. The second look at those fixes found one
  more, answered and looked at on the device: a panel closed in the piece and turned round by the key counted its
  key's step as seen at once.

## Part 5: the last part (done, 5 October 2026)

- **"First steps" runs it again.** Typed in the panel (also `tour`, `welcome`, `intro`; „Erste Schritte“), it
  starts the run in the panel that is open, without the opening piece; on the Booklight window's Start page a row
  in the Tips group plays the whole of it in a new panel, the opening piece first. An unfinished run goes on
  where it stopped, from either. It changes no switch. The command is offered only where it was typed for (three
  letters of its name, or one of its other words in full), and neither is offered on a screen too low for a stage.
- **A run on an installation that has been lived in is whole**: with "Show sums" off there is no sum's lesson and
  no sum in the show; with suggestions on there is no question; a cue with nothing to show sets the key's step
  down at once.
- **Every screen is set down part by part**, on one clock, from a schedule core works out (`SetDown`; the times
  are `Motion.kt`'s `PACE`): the seat, the caps a beat apart or the recipe written a letter at a time, the
  caption, the answers. A screen come back to in a later opening arrives as rows do; one that takes another's
  place moves only what changes; with the system's animations off everything stands in the first frame. Parts
  come by their graphics layer only: no place in the layout ever changes.
- **A screen takes a key 350 ms after the answer that key would run began to show**, by the key's own time. Tab,
  the pointer and a click count from the last answer to show. A key during a set-down does nothing and is not
  kept. On the question nothing is armed and "Not now" is first, as before.
- **Round the system's dialog**: the stage turns to "Now press your keys" a moment after the dialog has the
  front; the key that lands is held down on the glass, comes up, takes its check, and one lap of light runs; a
  held key's repeats do not put the panel away for 0.7 s after the last of them.
- **Answers show as pressed**; the lower edge goes from a stage to what follows without a visit to the bare
  field; the fold ends in one lap of light; the word "Not now" leaves behind is on the bare field once ("First
  steps wait in Booklight’s window"), also where a run has used up its three openings.
- **The welcome, mended from what the device showed**: no ring as the night lifts, no words before the lamp's
  light reaches them, the show's first row whole in the frame its letter lands, Booklight's mark up before it
  types.
- **A screen reader** is told each screen once, the greeting once, a late recipe by itself, the switch as a
  switch; one switched on in the opening piece ends it. `./bl debug first says` prints what is told.
- **The window's Start page and the README tell the way to a key as first run does**: Action + Quick Insert
  (Action + M on a keyboard without that key), and the system's dialog opens from the window on a page that
  begins with the five steps.
- **The documents**: README, the privacy text, the changelog ("Unreleased"), the status, `ux-model.md` §19,
  `design-system.md` §17, the store's form. Nothing is released: the version is 3.0, and the README, the privacy
  text and the form describe first run before a release has it (`PICKING-UP.md`, "Before a release").
- **The rules are core's**, tested: what "First steps" does to a run, a run with sums off, when a screen is in view (also under the
  system's dialog), where the command is offered, the schedule of every way a screen can come (148 first-run
  tests in thirteen classes, the schedule's fifteen, the show's thirteen; 728 core tests in all).
- **On the Lenovo Googlebook** (`docs/research/first-run-last-pass.md`, sixty-one checks, of which the ones a
  person must run are marked): forty answered in whole or in part, among them "First steps" typed and
  from the window, the word left behind, the welcome's mends in stills of both themes and a slowed film of the
  hand-over, every way a screen is set down, keys pressed while a screen comes and as fast as key events come,
  the question never agreed to, the choices and the fold, the key landing in view and under the real dialog, a
  held key's repeats, the landing, what a reader is told, animations off. Two things were found there and
  mended: "First steps" stood in everyday lists for one or two letters, and a sentence said to a reader ended "?.".
- The sixteen tasks' code is the plan author's own, applied from the plan's text; each task was reviewed, two
  rounds of fixes went in with their re-reviews, and a review of the whole part found nothing critical.
  What mattered among the findings, all answered: Tab then Enter at the landing reached "Not now"
  50 ms after it showed; a screen that came while the field held text counted as seen in its first frame; a
  screen after a list lost its set-down for an early typist; a held key's repeat would have closed "Your key
  works"; the press could not be seen; and, with animations off, the glass could dip to the bare field at a
  typed letter..

## Decided on the way, for Alex to overturn

| Decision | Why | If it is wrong |
| --- | --- | --- |
| An unfinished run is counted per run, not per step: it stands in three openings, then waits in the window | The tech lead's later paper over the two earlier ones | It waits sooner or later than wanted; one function |
| Practice holds in every lesson, also out of turn: while a lesson stands, an app's Open and a search inside an app start nothing | "Nothing opens in first run", read broadly | A condition to narrow |
| In a lesson, everything else (a settings page, the web, an app command, Play, a recipe) runs as every day and the panel goes | The design's own table; "nothing opens" read as "the lessons open nothing" | More effects to make practice |
| "Slow, once" belongs to the opening the welcome begins | So it is slow exactly as often as the welcome is shown | A first opening without a welcome (animations off) is not slow; one flag |
| The mark that the key's step is over lives in the tips' list as `first:key` | An older build keeps that list whole when it writes the settings | A stray id in a list; harmless |
| Someone who dismissed 3.0's shortcut card and still has no key is asked once more, by the key's step | "Updates from 3.0: without a key, step 1 once" | One unwanted step for them |
| An update with a key who never answered 3.0's suggestions card is not asked the question when the old cards go | The switch is in the window | A quarter day to ask it once |
| With suggestions already on, the question is not a step (the counter then says "of 4") | The replay asks it only while they are off | One condition |
| *Part 2.* What counts as "the key was pressed" is the positive sign alone: a start of the launcher by the system itself that carries nothing and no icon's place, or the assistant key | The Lenovo showed the system as the sender; the old list of senders who are not a key is gone | A start that is no key is greeted as one, or a key on another build is not known; one function with tests |
| An opening is counted when its panel is made, also one that is typed into at once; a panel that carries another app's text or has an example typed into it is not counted | So that a fourth opening does not begin a piece and end on an empty field; and so that openings in which nothing can stand do not use the run up | A run waits an opening sooner or later; one function |
| The suggested key is chosen when the stage is set down and again at the Enter that asks for the dialog, for the keyboard that Enter came from; it does not change while the caps are in view | A cap that changed under the eye would be worse than a stale one | A keyboard attached in between is offered the other key one Enter late |
| The large cap shows the system's name for the key only where it fits; else "Quick Insert", in every language. The caption and the dialog's rows always use the system's name | A cap never shrinks or wraps | In German the cap and the caption may name the key in two ways |
| No `<queries>` line for the system UI: its words were readable without one on the Lenovo; where they are not, Booklight's own words stand in | No change to the manifest without need | On another build the rows quote Booklight's words, not the dialog's own |
| The held panel goes when another window has had the front for 300 ms, and the hold gives up 1 s after asking if no dialog came | So that the panel is never left on the desk without the keys | It lingers, or goes too soon; two numbers |
| A key that lands brings a run that was waiting back only if it is a first key | A key that was known is no news | A waiting replay stays where it is |
| "Not now" on the key's step ends the run (the steps come again from the window, part 5) | The plan's reading of "Not now" | Someone who only wanted to skip the key never sees the lessons unasked |
| The old cards' two fields stay in the settings (nothing reads them but the migration) | An older build would otherwise show its cards again after a downgrade | Two unused fields |
| At the key's landing "Now press your keys" is seen for the frames of the dialog's fade and then turns into "Your key works" | It reads as an answer to the press; motion is part 5's | K4 could stand whole in the first frame instead |
| On the Lenovo nothing but Booklight and the dialog it opens was clicked or typed into: no click on the launcher's icon, no other app's window or shortcut, no lock screen, no change of the system's language or font scale | His rules for the devices; a font scale change re-lays every app out | Those checks are open (below) until he runs them or says to |
| *Part 3.* **The question's answers stand as "Not now", then "Agree"** (the papers and the prototype had "Agree" first) | Tab, then Enter skips a lesson; the same keys on the question must not be consent. A pause alone stops only the fast case | One line in core's `answers` and two tests |
| A screen of first run takes a key or a click only once it has been in view for 350 ms (the glass open, the screen not just come) | The Enter that skipped a lesson, pressed again at once, must answer nothing; and nothing is answered under the lower edge of a glass that still grows | A deliberate press in the first third of a second is lost without a word; one number |
| While a lesson's list is typed the lesson's stage gives way to the real list; the lesson goes on as the placeholder and as the coach line in the footer | `design.md` §6 and the prototype | A seat above the list is about one task; the model already holds `lesson` apart from `stage` |
| After a lesson's Enter its list stands for 520 ms and then gives way, but not if the user has typed on; until then a second Enter does nothing | "520 ms later the next lesson"; typing never waits | Enter is dead for half a second after a lesson's Enter |
| What a lesson calls practice is an app's Open, a search inside an app, and (where Settings stands in for an app that can be searched, or the example is not worked out yet) a settings page under its keyword. A sum is copied for real. **Everything else runs as every day, also where it opens another app**: the web's row for a name no app has, Play armed by a second Tab in lesson 3, App info, another app's command | The table above ("nothing opens" read as "the lessons open nothing"), with the settings page erring on the side of nothing opening | A new user who strays from the recipe sees a window open though the caption says "Practice: nothing opens": more effects to make practice |
| The lessons' example is the app the list of everything uses for "search inside an app", by the shortest start of its name (three or four letters) that puts its row first and offers Search; else Settings by its keyword; else the keys alone. It is worked out for each panel, off the main thread, and never kept or written | So that the recipe never sends the user to a row that is not there | On a device whose apps share their first four letters Settings stands in; on a cold start the recipe comes a moment after the seat |
| The question is 324 dp and taller by what its text needs beyond four lines, measured | "Never cut"; also covers a larger type size | On a very low screen the panel ends nearer the edge than 56 dp |
| The choices are a list of two rows with nothing selected; they are left, and the run is over, also when the panel closes, but not if they were never in view. Enter within 350 ms of their coming is not taken | "C is done when it is left, however it is left" | Someone who closes the panel on the choices is not offered "Show your usual" again unasked |
| Someone who keeps pressing Tab, Enter after the question selects "Show your usual" and flips it, a pair at a time | A local setting in plain view, not consent | The choices could ask for Down before Tab selects |
| With the switch's row selected the footer has no word for what Enter does there; "Done" gives way only to a row's own hint | Not in the papers | One string |
| A panel that carries another app's text, or that an example is typed into, shows nothing of first run and is no opening of a run | It is not a place for a lesson | Text handed over in the middle of a run is just that, and the run goes on at the next opening |
| German: „Aktion“ wherever first run names the key; „Vorgeschlagene Tasten: …“ for a screen reader. Left as the papers have them, though a reviewer queried them: „Beim Tippen Suchen vorschlagen?“, „bis zwei Dinge je zweimal liefen“, „Sonst schließt Enter auch Booklight“ | The papers' words are his to change | Strings |
| The word "Not now" leaves on the key's step, the "First steps" command and the window's row are part 5's; the store form still quotes the old card until then | Part 5 builds where first run waits | The form is stale, as it has been since part 2 |
| *Part 4.* The plan's fifteen decisions stand as built; its table says each one's cost (`docs/superpowers/plans/2026-10-05-first-run-4-the-welcome.md`, "Decided here"). The ones he would notice: a screen lower than 655 dp has no piece at all; where the piece does not play nothing is used up; "the plain shaft" keeps the prototype's rays, breath and bloom, without dust or a lap; the welcome's clock starts when the glass is open, so at Slow everything is a quarter second later than the paper; four pieces of the show's motion are not built (below) | Each where the papers differ, are silent, or could not be built as written | Each is a constant, a condition or a task; the table says which |
| **Space is the cue**, as Enter is: "Show off" in the welcome, the key's step in the show. It types nothing while the piece plays | The papers count it among the keys that type. It is the key people press at "continue", and a typed blank left a bare glass on the very first opening | Someone who starts a text with a space loses that space; one line in `Panel`'s `pressOf` |
| The lock keys and the Quick Insert key alone do nothing during the piece; the system's own keys (volume, brightness, media) pass by it | A touch of the key the next screen is about must not end the show; the volume must still change | One list |
| The two seconds after which the piece counts as shown run from the glass being open, not from the panel's making | At Slow the glass opens half a second late: the knife was not out when the piece was used up | Someone who closes in the first two and a half seconds is welcomed again |
| A panel that closes ends its piece; if the key turns the leave round, an ordinary panel with the key's step stands, and that step takes a key 350 ms later | The piece must not play on over a glass that folds, or write over text another app hands in | The rest of the piece is not seen in that opening; it plays at the next if two seconds had not passed |
| No piece in a right-to-left layout: the key's step at once, without the greeting | The welcome is drawn from the left edge (the caret, the lamp) and would not mirror | A user of such a language is not welcomed until the picture mirrors: about a task |
| The highlight's way into the key's step starts where the highlight was drawn, and the list's pill is not drawn while it travels | "One highlight, never two" | None known |
| A press counts by when the key was pressed, not by when it is handled | A long first frame of a screen must not make a key pressed before it count | None known |
| *Part 5.* The plan's twenty-nine decisions stand as built but where a row below says otherwise; its table says each one's cost (`docs/superpowers/plans/2026-10-05-first-run-5-the-last-part.md`, "Decided here") | Each where the papers differ, are silent, or could not be built as written | Each is a constant, a condition or a task; the table says which |
| **A key during a set-down does nothing, and every answer waits 350 ms from when it began to show.** `motion.md` §5 has Enter count as soon as the armed answer shows and Tab finish a set-down | One rule for every screen, tested in core; the system's dialog and consent must not be answered blind | A screen is deaf for longer than the paper has it, without a sign: the key's first screen about 0.73 s after the glass is open at Medium (the paper: 0.38 s); after the opening piece 0.53 s for Enter and 0.83 s for Tab and a click; lesson 3 after a lesson's Enter about 1.6 s before Tab counts. `SEEN_MS` is one constant; hurrying a set-down on Tab about half a task |
| "First steps" typed begins in the open panel and never plays the opening piece; the window's row plays the whole in a new panel; an unfinished run goes on where it stopped from either | The one who types is at work, the one in the window asked to be shown; a piece begun in an open panel is not whole | A typed "First steps" that should play the piece: about half a task |
| **"First steps" is offered only where it was typed for**: three letters of its name, or `tour`, `welcome`, `intro` in full. (As first built it stood in the list for "f", "fi" and "w" for everyone) | Someone who has no run meets nothing new unasked | Someone who types "fi" for it types one more letter; one condition |
| With "Show sums" off there is no sum's lesson and no sum in the show; the counter counts what is left | The lesson could only be skipped there; the show shows nothing that is switched off | Someone with sums off is not shown that Booklight can add; one line |
| The word that the steps wait in the window is said once, also where a run has used up its three openings (the paper: after "Not now" only) | That user was told nothing | One placeholder too many, once |
| The Start page shows one suggestion with first run's own caption, not two side by side, and the dialog it opens begins with the five steps | One way to a key, told the same in three places | The page no longer shows that a second combination is free |
| A panel that opens on a screen of first run has no ordinary lap of light; first run asks for its own two (the key works; the fold) | Two laps within a second, or a lap as a reward for nothing | In a run's openings the glass has no idle lap |
| The landing of the opening piece lasts until the key's step is in view (about half a second) | Until then every press but a typed character is the piece's | A deliberate Enter in that half second is lost without a word |
| The user's key, landed: no start by that key does anything for 0.7 s after the last one, in real time | A held shortcut repeats, and a repeat must not put away the panel that greets it. Whether a held shortcut's repeats are new starts at all is not known | A second press within 0.7 s does not close the panel; where the keyboard's repeat delay is longer than 0.7 s the guard is too short. One constant |
| A screen of the key's step that came while the system's dialog was over the panel takes a key 350 ms after the panel is uncovered | "In view" | Enter is deaf for a third of a second after the dialog goes |
| The press is shown without holding the answer back: the slot is darker from the frame of the press, on what the leaving screen showed, and lets go | A delayed answer could run twice or after typing | On a screen that changes, the press is a tenth of a second on answers that fade; whether it reads as a press is his eye's |
| A screen reader switched on while the opening piece plays ends it; the welcome has no spoken way to skip | It is not there to be read | Someone who switches a reader on in those seconds misses the show |
| A recipe too wide for its room is drawn smaller as a whole; a name that begins with something a keyboard cannot type is no example (Settings stands in) | Never cut, never wrapped; never told to type an emoji | At a large font scale a long recipe is smaller than the caps elsewhere |
| Left out, each said in the plan's table: the pill condensing into the grid's first cell and the cells leaving as a wave (`motion.md` §2.2); the highlight's way starting from the middle of a move; a pointer's click counting by its own time; a leave turned round on the choices; `motion.md`'s T6 (the lessons open nothing, so nothing steps aside); the old command "Set up the keyboard shortcut" and the Play listing's paragraph on the key, which still tell the old way | Each rebuilds a part of the daily panel or is not first run's | A task each, once a film or he says it is missed |
| The sixteen tasks' commits were made by the plan's author from the plan's own text and applied as they were, not typed again by sixteen engineers; every task was still reviewed on its own | The text carried the whole code and had been applied in order with green tests | A difference between a task's text and its commit would have been the reviewer's to find |

## Still open on a device

The one list is `docs/research/first-run-last-pass.md`: this part's checks, and after them everything the three
earlier lists left open, each marked as the coordinator's (run on the test device) or as his own. In short, his
own: "Set shortcut" and the first press of a new key under the real dialog, on a Googlebook where Booklight has
no shortcut yet (the feature's main moment, never seen); the user's keys held down; a real click on the icon and
two clicks on the window's row; a keyboard without Quick Insert; whether Quick Insert is the key left of A; the
system in German; a larger font scale; a screen reader's voice; the whole run by hand, timed; everything on the
HP Googlebook; and what only an eye can say: whether the lamp reads as struck, the show as one movement, a press
as a press, the handle's rest in row one's seat as a breath.

## Known, and left

- Four pieces of motion are not built (the table above): the two of the show's grid, the highlight's way from
  the middle of a move, and T6.
- A replay's opening piece that is closed in its first two seconds plays again at the next ordinary opening,
  wherever that is (part 4's rule, now reachable from the window's row).
- Space on a stage hides it (a field that holds one blank) until Backspace; in the opening piece Space is the cue.
- A late recipe (the device's example arriving after its lesson was set down) is written a letter at a time only
  where its band had not begun; later it fades in whole.
- The falling night's gradient shows bands of one grey level each when a still's contrast is stretched to the
  limit; none was seen at ordinary contrast.
- A pointer that rests where the answers come could light one without moving (not driven on a device).
- Not verified by reading or on a device: a panel whose first start carried a referrer of its own judges a later
  key by that first sender; a process that dies under the dialog after an icon's first click opens the window at
  the new key's first press.
- The Play listing's paragraph on the key and the command "Set up the keyboard shortcut" still tell the old way.
