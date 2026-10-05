# First run: a plan (4 October 2026)

Alex: "a strong first run experience. Ideally in the style of the app ui (Not the settings ui) with some strong
visual design and animations and motions but ultimately helping the user walk through major setup (shortcut,
feature enablement) and top 3 functions and how to learn more. Research best practices, design, animations, and
have a designer, motion designer, PM and tlm make a plan."

A product manager, a designer, a motion designer and a tech lead worked on it, after research into what other
launchers do and into what Android allows for the shortcut. He approved it for building on the same day.

## The plan in ten lines

*As it stands after Alex's answers of 4 October. To the first plan: "Love it. Some adjustments. The suggested key
should be Action + Quick insert […]. Can you add a more visual beginning […]. It should wow at the very beginning
showcasing truly this app. Re 5, the other thing. Re 11 no row." To the opening: "Yes but have sort of a 'boot
screen' at the beginning where it says 'Welcome to Booklight' […] but maybe cuter and funnier. Also add flights
into the opening celebration." And: "make the welcome visually stunning […] Go all out for the boot welcome."*

1. **One thing must happen:** the user's own key opens the panel, and they run something with it.
2. **It begins with a welcome, then a show** (about ten seconds, Booklight's own). The welcome is the one place
   where the glass is a stage: it grows to 720 × 468 and goes to night, the caret comes back as Booklight's own
   lamp, and its light falls on "Welcome to Booklight", one funny line and a small Swiss Army knife of five tools
   whose handle says "Show off". Then Booklight uses itself in the real panel: the device's own apps in a
   cascade, a sum rolled into its answer, an example flight with its plane arriving, the emoji grid in its wave,
   one highlight travelling through all of it. The first key that types ends everything in the same frame and
   is typed.
3. **It lives in the panel**, under the field, where rows stand, and it is **one opening** from the first frame to
   the bare field: the key, three lessons, the question, two choices.
4. **The key is set in the system's Keyboard shortcuts dialog**, because Android offers no other way
   (`docs/research/shortcut-setup.md`). The panel stays alive behind that dialog, so the dialog opens on
   Booklight's own page, and that page carries five numbered steps in the system's own type.
5. **The suggested key is Action + Quick Insert** (the key where Caps Lock sits on other keyboards); Action + J
   on a keyboard without it.
6. **The reveal:** after "Set shortcut" the user presses the new keys; the system's dialog falls away by itself
   and the panel is there, its key held down, a check drawing in it, one lap of the white light.
7. **The lessons** are open an app, search inside an app (Tab), a sum. The panel stays through them. Where Enter
   opens an app, Booklight steps aside and is back by itself, in front of the app, on the next lesson (seen to
   work on a device; how the blink looks is filmed before it is built, and practice only is the fallback).
8. **The suggestions question stands alone**, nothing armed: "Agree" or "Not now". Then two rows: "Show your
   usual" and `?`. No row for the device's model.
9. **It ends as the bare field**, with "Type ? for everything Booklight does" for that one opening.
10. **"First steps"** replays it: a typed command and a row on the window's Start page.

## The files

- `first-run.html`: the page Alex is shown, with a prototype that plays the whole minute and steps through every
  screen (light and dark, English and German, at a quarter of the speed, with motion off).
- `00-brief.md`: his request and answers, what is fixed, the facts found on the way.
- `pm.md`: who it is for, the three functions, the steps, skips and returns, the words of the question (written
  before his answer; where it differs from this page, this page and `design.md` hold).
- `design.md`: the stage, every screen with its measures and its English and German words, the rows inside the
  system's dialog, every state, what must line up.
- `motion.md`: every transition in ms, the springs, what interrupts what, the stills, what to cut first.
- `eng-constraints.md`: what the code allows (written before the design).
- `eng.md`: the build plan: files, state, tests, hooks, the order of work with sizes, the device checks.
- Research: `docs/research/shortcut-setup.md`, `docs/research/first-run-practice.md`.

## Seen on a device, and not

Seen on the Lenovo Googlebook, with a throwaway stand-in for the panel (a window of its size and place that stays
open): the system's dialog opens on the app's own page; the five rows are drawn as designed, in English and
German; the panel's place lies wholly behind the dialog; closing the dialog gives the focus back; **the new key
closes the dialog by itself and the app is told**; Action + Quick Insert is taken by the dialog and works; a
window that opens an app is removed by the system and comes back in front when it starts itself again at once.

Not seen: the real panel behind the dialog, how the step aside and the return look, anything on the HP
Googlebook, the opening, any of the screens or the motion
(they exist as drawings and as the prototype). The build plan starts with a proof on a device (its milestone 0).

## Approved to build

Alex, 4 October, late night: "Go ahead and build it", with his last answers: the plain shaft of light, the words
A, the first opening at Slow, Action + M for a keyboard without Quick Insert, and nothing opens in first run (the
lessons are practice; Booklight never opens itself over another window). The settled list is in `00-brief.md`,
"Settled for the build", and wins where a paper says otherwise. The build's plan:
`docs/superpowers/plans/2026-10-04-first-run.md`.
