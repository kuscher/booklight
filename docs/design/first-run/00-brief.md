# First run: the brief

*4 October 2026. For the four seats that plan it: PM, designer, motion designer, tech lead. Nothing is built until
Alex has seen the plan and the designs and approved them.*

## What Alex asked for

> "Second task, and this is bigger, is a strong first run experience. Ideally in the style of the app ui (Not the
> settings ui) with some strong visual design and animations and motions but ultimately helping the user walk
> through major setup (shortcut, feature enablement) and top 3 functions and how to learn more. Research best
> practices, design, animations, and have a designer, motion designer, PM and tlm make a plan."

And on the shortcut: "Right now users have a hard time setting it themselves."

## What he answered (4 October)

| Question | His answer |
| --- | --- |
| Which three functions? | **The PM proposes** three with reasons; he approves or changes them in the plan |
| Which features does first run offer to switch on? | **Search suggestions**, **Your usual**, **the device's model**. Flight and Spotify keys stay in Labs and are only named under "learn more" |
| How much may it ask of a new user? | **Short, by doing**: about 45 to 60 seconds; the shortcut first, then the three functions each tried once for real, then the switches and "learn more"; Esc leaves at any point |
| Who gets it? (my default, not objected to) | New installs; every step can be skipped; it can be replayed from the Booklight window |

## What is fixed

- **The panel's own language.** Flat glass, visibly see-through, a crisp white outline, no bevels or glows; one
  coloured surface, the selection; motion everywhere from `Motion.kt`, nothing pops, sizes known before anything
  moves, typing never waits (`CLAUDE.md`, "Design rules"; `docs/design/design-system.md`). Not the window's
  Material pages.
- **Alex judges alignment first**, then motion. "Keep the high motion and design quality. This is paramount."
- Every string is a resource, **English and German**. Plain copy.
- **No new permission.** Nothing in the background. What the device's model says is only shown.
- The repo is public: no device serials or build numbers, nothing about what is installed on anyone's device.
  Say "the Lenovo Googlebook", "the HP Googlebook".
- Approvals: a design is shown before it is built; a release only on his word.

## What is known (read these)

1. `docs/research/shortcut-setup.md`: how the system's Keyboard shortcuts helper really behaves, the shortest
   path (Customize, type Booklight, +, the keys, Set shortcut), that the helper opens on **Booklight's own page of
   rows when a Booklight surface stays in front**, that Booklight only learns of success when its key opens the
   panel, and that Action + K must no longer be suggested.
2. `docs/research/first-run-practice.md`: what other launchers and keyboard-first tools do, the evidence, motion
   numbers, the store's consent rules.
3. `CLAUDE.md` (layout, design rules, gotchas), `docs/design/design-system.md` (tokens, components, §4 the motion
   table), `docs/design/ux-model.md` (keys, scopes), `docs/design/zero-state.md` §3 ("one rule for the place under
   the empty field") and core `Zero.kt` / `Under`.
4. What first run is today, in the code: `overlay/OverlayModel.kt` (`Card`, `card`, the tips: `offerTip`,
   `tipEnter`, `Effect.Type`), `overlay/Panel.kt` (the card, line 600 on), `overlay/OverlayActivity.kt` (`byKey`,
   `keySeen`, the card's buttons), `window/Pages.kt` (Start: the key's status row, the demo `Stage`),
   `window/Stage.kt` (the panel drawn on a desk, with a looping demo), `Tips.kt`, `data/Prefs.kt`.

## Hard facts the design must live with

- **The panel's window is exactly the panel** and is never resized in width (it is neither smooth nor symmetric);
  height may change, the top edge never moves; nothing may be drawn outside the glass. The opening is always the
  field's height; what is under the field comes after it.
- **The panel closes when it loses the focus.** Today the card asks for the helper and closes; the helper then
  opens on *System*. To open on Booklight's page the surface that asks must stay in front.
- The helper is the system's dialog: Booklight cannot draw in it or over it, cannot prefill it, cannot tell that a
  shortcut was set. It dims everything behind it. Only the rows of Booklight's own page in it are Booklight's.
- Success is known the moment the key opens the panel (`keySeen`); a start from the icon opens the window, not
  the panel.
- The device's model: present, downloadable, downloading or absent, per device; it answers only the app in front.
- Search suggestions send what is typed to the search engine: a disclosure of its own, an affirmative choice,
  never bundled, never by Esc or by waiting.
- "Your usual" needs a history to show anything: on a first day it has nothing to show.
- Typed keys reach the field before the input method; a held key repeats; Esc must always leave.

## Since the first pass (facts for the designer and the motion designer)

- The PM's paper (`pm.md`) and the tech lead's memo (`eng-constraints.md`) are in. Read both before anything else.
  The PM's choices stand unless you have a design reason to differ; if you do, say so in a section of its own.
- **The helper covers the panel.** On the Lenovo Googlebook (2880 × 1800 px at 1.5 px a dp) the helper's dialog
  is 1488 × 1432 px, centred, from 284 px down; the panel is 1080 px wide, centred, from 360 px down. So while the
  helper is up the panel is not dimmed beside it: it is **behind it, unseen**. It only has to stay alive, so that
  the helper opens on Booklight's page and the panel is there, changed, when the helper closes.
- **The helper's rows wrap.** The system's own long labels ("Cycle focus between system windows in reverse order")
  stand on three lines of about 25 characters, uncut, with the keys at the row's right end. The page shows about
  ten one-line rows without scrolling. Its title is the app's name beside the app's icon. Rows are a label and key
  caps in the system's own type; a key is drawn as its name ("Ctrl", "F6") or its letter.
- The helper's search field has the focus when it opens and after *Customize*; typed before *Customize* the app's
  name finds nothing ("No search results"); after it, the app with a **+**.
- **Tried with a stand-in for the panel** (`docs/research/shortcut-setup.md` §2, "A stand-in for the panel"): a
  window that stays open gets its own page in the helper, headed by the group's label (free text); rows wrap,
  uncut, in English and German; a row with the app's name in it stops the search's jump; closing the helper gives
  the focus back to the window that stayed.
- **The new key closes the helper by itself.** Right after *Set shortcut*, pressing the new keys makes the system
  close the helper and bring the app's window to the front, and the app is told (a launcher start arrives, its
  referrer `android-app://android`). The path ends "… Set shortcut, then press your keys": no *Done*, no closing.
  The first press of the user's own key is the moment the system's dialog falls away and the panel is there.
- A combination that is not a shortcut yet does not reach the app: no "try your keys here first".
- The helper's own words exist as named strings of the system UI (`shortcut_helper_customize_button_text` and
  others; the research file lists them). Reading them would let the instructions quote the buttons in the user's
  own language; whether an app may read them is for the tech lead's second pass.

## Second round: what Alex said to the first plan (4 October, evening)

> "Love it. Some adjustments. The suggested key should be Action + Quick insert (the key which shares itself with
> caps lock). Can you add a more visual beginning with more motion and more animation and polish. It should wow at
> the very beginning showcasing truly this app. Re 5, the other thing. Re 11 no row."

Everything else on his page stands at its recommended answer. What changes:

1. **The suggested key is Action + Quick Insert.** It works (`docs/research/shortcut-setup.md` §3): the system's
   helper draws the key as a cap that reads "Quick Insert" after the Action glyph, its capture dialog accepts it,
   and pressing it starts the app and closes the helper. The key's code is `KEYCODE_CONTEXTUAL_INSERT`; it sits
   where Caps Lock sits on other keyboards. A keyboard without it needs another suggestion (Action + J).
2. **A beginning that wows.** His words: "more visual", "more motion and more animation and polish", "showcasing
   truly this app". The lead's direction for the team, to be bettered by it:
   - **The product performs; nothing is a film about it.** In the real panel, with real rows, real components
     and the motion of `Motion.kt`: Booklight types into its own field and shows a few of the things it does,
     one flowing into the next, the glass breathing in height with each, the highlight travelling, a list
     cascading, the light running its lap at the peak. Then it settles into "Give Booklight a key".
   - **Only what is true on every Googlebook on day one**: no flight (it needs the user's key), no play by name,
     no prompt (the model may be absent). Apps this device really has, a sum, and Booklight's most visual pieces
     that need nothing (the emoji grid, a colour, a QR code, a level).
   - **Short and never in the way**: about 6 to 8 seconds; the user's first key ends it in the same frame and is
     typed; Esc skips to the key step; nothing in it asks for anything. With the system's animations off there
     is no performance: the key step stands at once.
   - It may use what the first plan left on the table for this one moment: more height (to 468 dp is free), a
     dim behind that lifts as it settles (up to half a day), the opening at its slow, full speed.
   - It is shown once, on the very first start; the replay may offer it again.
3. **Decision 5, the other thing: the panel stays open through the three lessons.** Enter in a lesson does not
   put the panel away. First run is then one unbroken sequence in one opening: the key, the reveal, three
   lessons, the question, the choices, the fold. The key is pressed once in it. To settle: what Enter does in each
   lesson when the panel stays (the sum copies and stays; a search inside an app makes the app the chip, as built;
   opening an app: does the app open behind the glass while the panel stays in front, seen through it, or is it
   practice only?), and how a lesson says that on every other day Enter puts Booklight away.
4. **Decision 11: no row for the device's model.** The last screen has "Show your usual" and "Everything
   Booklight does"; first run asks the system nothing about the model and the privacy text stays as it is.

**Tried on the Lenovo Googlebook for point 3** (the stand-in window, opening another app from it):
- A see-through window that starts another app does not stay in front. The app takes the front and the focus at
  once, and the system then removes the window that started it, unasked (destroyed 0.2 s after the start in one
  run, still there at 0.4 s in another, gone before 1.5 s in a third).
- Asking for the window's task to come to the front did not bring it back.
- Starting the window again right after starting the app did: it stood in front of the app's window, with the
  focus, about 0.1 s after the app's start (0.43 s when it waited 0.4 s; at 1.5 s it was too late). It came back
  as a newly made window in one run and as the same one in another.
- Not filmed: what the eye sees in between (the system fades a see-through window out and in).
- So: "Enter opens the app; Booklight steps aside for a blink and is back by itself, in front of the app, on the
  next lesson" is real. A panel that never moves while an app opens is not.

## Third round: what Alex said to the opening (4 October, night)

> "Yes but have sort of a 'boot screen' at the beginning where it says 'Welcome to Booklight' and next line 'Your
> Swiss Army Knife launcher. Let me show you what I can do before we dive in ... show off now' but maybe cuter and
> funnier. Also add flights into the opening celebration."

So the opening stands, with two additions:

1. **A welcome before the performance.** Booklight greets, in the first person, this once: "Welcome to Booklight",
   then a line in the spirit of "Your Swiss Army knife launcher. Let me show you what I can do before we dive in",
   and a cue into the show ("show off now"). He wants it **cuter and funnier** than his draft. This one moment may
   leave "copy is plain": it is his word. It stays the panel's own glass and type: a boot screen in feeling, not a
   splash screen with a logo. He should get a few versions of the words to choose from, in English and German.
2. **A flight in the opening.** He loves the flight's row (a line from take-off to landing, the plane on it, one
   badge: `docs/design/flights-row/design.md`), and it is the most beautiful row Booklight has. On day one there is
   no key, and Booklight must not fetch anything or pretend: the flight in the opening is **an example**, said so
   on the glass in a few plain words, and the place that says flight times need a key of the user's own (the last
   screen's footer line about Labs) stays. LH455 is the flight he asked for when flights were first built.

A few minutes later he added:

> "And make the welcome visually stunning. It should be in an expanded version of our panel (same width) but you
> can play inside of it with animation, graphics, motions, and light. Go all out for the boot welcome."

So the welcome is not a quiet title. It is the one place in Booklight where the glass is a stage:

- **An expanded panel, the same width**: 720 dp wide, taller (to 468 dp is free, to the screen's ceiling half a
  day more). The window is still exactly the glass, nothing is drawn outside it, and it still opens as the panel
  opens (the seam, the glass at the field's height) before it grows.
- **Inside it, everything is allowed for these seconds**: drawn graphics, animation, light, large type. "Flat
  glass, no glows, no illustration, plain copy" are the rules of the daily panel; his word lifts them for the
  welcome. It must still be Booklight's and nobody else's: its name is a reading light, its mark is a beam, its
  outline already carries a lap of white light, and his own line is "Swiss Army knife". Start from those.
- It must hold up over any desk (the glass is see-through), in light and dark, and at 60 frames a second on a
  Googlebook.

The lead's limits for this round: the welcome may take up to about four seconds and the whole opening up to about
thirteen; the first key that types still ends it in the same frame and is typed; Esc still skips to the key step;
with the system's animations off there is no performance (a still greeting may stand).

## Settled for the build (Alex, 4 October, late night)

> "Re 2 the plain shaft (the other thing). 3. A. 9. The other thing. First round is slow. 12. Action M. 13. yeah
> dont open in first run. And in general no need to open booklight on top of other windows I think. Go ahead and
> build it"

This list wins over anything in `pm.md`, `design.md`, `motion.md` and `eng.md` that says otherwise. Everything
they say that is not named here stands at its recommended answer.

1. **Build it.** The route for the key is the system's dialog, guided; the suggested key is **Action + Quick
   Insert**.
2. **The welcome is "Lights on" with the plain shaft**: no dust in the beam and no lap of light round the outline
   during the welcome. (The lamp's strike, the core and the soft sides of the shaft stay; they are the shaft.)
3. **The welcome's words are A**: "Welcome to Booklight" · "Your Swiss Army knife launcher. Blades not included."
   · "Show off" (German: „Willkommen bei Booklight“ · „Dein Schweizer Taschenmesser zum Tippen. Ohne Klinge.“ ·
   „Kurz angeben“). B and C are not built.
4. **The very first opening runs at the Slow speed**, once, whatever is set. Every later opening as set.
5. **A keyboard without Quick Insert is offered Action + M** (not J). K3's "other keys to try" follows: with
   Quick Insert it suggests Action + M; without it, nothing more than "or any keys you like".
6. **Nothing opens in first run: the lessons are practice.** Enter on an app's row in lesson 2 and on the word in
   lesson 3 starts nothing (`design.md` §6, "Practice only"): the slot is pressed, the footer says with its check
   "That opens %1$s" or "That searches %1$s", and 520 ms later the list gives way to the next lesson; 252 stays.
   The caption under the recipe is "Practice: nothing opens. Enter also puts Booklight away." The coach line's
   second half is "practice: nothing opens" (German: „zum Üben: nichts öffnet sich“) in steps 2 and 3. The sum
   copies for real and the panel stays; Search on an app's row makes the app the chip for real.
7. **Booklight never opens itself over another window.** No "started again at once" after an app, anywhere. That
   whole mechanism (`eng.md` §12.1, `motion.md`'s step aside, the film in milestone 0) is out of the build.
8. Still his to confirm, at the first look on a device: that Quick Insert is the key left of A (the caption under
   the caps says so).

## The seats, and what each hands over

All files in this folder. Write for a reader who was not in the room: plain words, tables where they help, every
number with its unit, every claim about the code with the file it is in. Where you choose between ways, say which
you chose and what the other costs. Collect what only Alex can decide in a table **"Decisions for Alex"**
(the decision, the recommended answer, "if you say the other thing") and go ahead on the recommended answers.

| Seat | File | What it must settle |
| --- | --- | --- |
| **PM** | `pm.md` | Who this is for and what they know on minute one; the one thing first run must achieve and how we would know; the three functions and why these; the steps in order with what the user does in each (do, not watch) and the time each may take; what is left out and where it goes instead; what "learn more" is; what a user who skips, quits half-way, or comes back gets; the words for the consent step; which key to suggest |
| **Tech lead**, first pass | `eng-constraints.md` | What the code allows and forbids for a first run in the panel: which surface can stay in front while the helper is open; what a taller or larger glass costs; what exists to reuse (cards, tips, self-typing, the demo stage, the pin); where state would live and how a run resumes; what cannot be known or done; the checks only a device can answer |
| **Designer** | `design.md` | The experience, screen by screen, in the panel's language: the stage (two or three ways, one recommended), every step's layout with measures in dp on the panel's own grid, its English and German words, every state (waiting for the key, back without it, skipped, the model absent, the longest German line), the rows of Booklight's page in the system's helper, the replay and the way back in; what must line up |
| **Motion designer** | `motion.md` | The choreography: the arrival, each step's entry and exit, the hand-off to the helper and the return, the key press that lands, the typed demos, the ending that folds into the bare field; every time in ms and every spring by its name in `Motion.kt` or as new values with a reason; what typing or Esc interrupts and how; the still for reduced motion; what to cut first if a device shows it too busy |
| **Tech lead**, second pass | `eng.md` | The build plan for the chosen design: files and functions, the state and its storage, the pure functions and their tests, debug hooks, races, what changes for people who never see first run, the order of work with sizes in days, the device checks before it is called done |

Then the plan (`README.md`), a prototype that moves (`first-run.html`), and Alex's review.
