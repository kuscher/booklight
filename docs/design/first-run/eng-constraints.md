# First run: what the code allows (tech lead, first pass)

*4 October 2026. For the designer and the motion designer, before they design. No design, no build plan.*

Marks: **[C]** read in the code (file · function). **[R]** from this repo's research notes: seen on a device or
read in Android's source, as they say. **[I]** inferred, not checked. Days are estimates [I]. Paths are under
`app/src/main/java/io/github/kuscher/booklight/`.

## 1. Which surface stays in front of the helper

| Surface | Today | Verdict |
| --- | --- | --- |
| The Booklight window (`window/MainActivity`) | Stays in front; gives the helper one group (`onProvideKeyboardShortcuts`) [C]. The helper lists a "Booklight" page; `dismissKeyboardShortcutsHelper()` closes it and the keys come back [R] | Works now. It is the Material window, not the panel |
| The panel (`overlay/OverlayActivity`) | Closes when it loses the focus; gives the helper no rows [C] | Can be made to. Never seen on a device |

**The panel today** [C]: `card()` asks for the helper, then `close()`. Without that `close()`,
`onWindowFocusChanged(false)` closes it. And it has no `onProvideKeyboardShortcuts`: with no rows the helper opens
on *System* [R], even if the panel stayed. `stay` is a debug-only flag (`EXTRA_STAY`) that skips the close in
`onWindowFocusChanged` and the finish in `onStop`; a release build has nothing like it.

**What must change** (about 1.5 days):
1. A hold: while first run waits for the key, losing the focus does not close the panel. It ends when another
   *activity* comes to the front (`onTopResumedActivityChanged`; the helper is a dialog) [I].
2. `OverlayActivity.onProvideKeyboardShortcuts`: one group of rows. A row is a label and at least one key, without
   "Booklight" in the label [R, `shortcut-setup.md` §2]. Plain text in the system's type: no layout, colour or
   motion of ours.
3. `onNewIntent` (section 2).

**Under the helper**: it dims everything behind it; a pinned Booklight window fell from 233 to 93 of 255 [R]. The
panel would be dimmed the same and have no keys [I], and the helper may cover it: its size against the panel's
place was never measured. **Design the panel under the helper as not readable and not reachable.** The steps
there are the rows of the helper's page.

**When the helper closes** [I]: the focus returns to the same panel, as it was left
(`onWindowFocusChanged(true)`). That is the only signal. It means "the helper is gone", never "a shortcut was
set": the state on return is "back, key not pressed yet".

Recommended: the panel, if checks 1 to 4 of section 9 pass. The window is the fallback that works today.

## 2. The key pressed while the panel is open

Today [C], `OverlayActivity.onNewIntent`: a second start without text ends in `close()` (the toggle), and
`keySeen` is set only in `onCreate`. So **a key pressed while the panel is open closes it and is not counted.**

First run needs (0.5 day): in `onNewIntent`, while the key step waits and `byKey(intent)` says yes: set `keySeen`,
mark the step done, stay open, and call `dismissKeyboardShortcutsHelper()` in case the helper is still up.
`fromIcon` already reads the referrer there [C]; that `byKey` works there too is [I].

- `byKey` is a rule of exclusion [C]: a launcher start that is not the store, the installer, Settings or adb. The
  icon is ruled out before it, by `fromIcon`. It cannot say which keys were pressed.
- If the panel had closed, the key makes a new one (`onCreate`): "done" plays after that panel's own arrival.
- Ways in [C, `fromIcon`, `handOver`]: the icon opens the *window*; "Open" in the store or the installer, the
  widget and the tile open the *panel*. Sending a first icon click to the panel is a small change there (0.5 day).
  The window can open the panel (`window/Commands.kt`, `ACTION_PANEL`).

## 3. What a bigger stage costs

Measures [C, `overlay/Metrics.kt`, `OverlayActivity.placeWindow`]: 720 dp wide; top edge at 20 % of the screen;
field 68 dp; today's card makes 172 dp in all. `maxRows` keeps 56 dp free below, so a panel *h* high fits a screen
of at least (*h* + 56) / 0.8. 468 dp in all (400 under the field) fits from 655 dp; today's tallest list, 648 dp,
from 880 dp. The ceiling is 797 dp on the HP (1067 dp high) and 904 dp on the Lenovo (1200 dp) [R].

| Stage | Verdict | Rests on |
| --- | --- | --- |
| The panel's size, 96 dp under the field | **Free** | `Panel.kt` `CardBody`, `Metrics.card` |
| Same width, taller, to 468 dp in all | **Free** to move; the layout is the work. One fixed height per step in `Metrics.height`, never wrap-content | `Panel` (height spring), `sizeWindow`; smooth [R] |
| Taller than 468 dp | **+0.5 day**: capped per screen as `maxRows` is, plus a lower layout | `Metrics.maxRows` |
| Tall from the first frame | **Not allowed**: it opens 68 dp high and grows after `Motion.GATE` | design rule |
| Glass smaller than the panel (a seam, a pill that grows) | **Free** | `Panel.glassBox`, `frameGlass` |
| A larger or wider glass that shrinks into the panel, one window | **4 to 5 days, and Alex's word**: that opening's window is larger and stays so, the glass framed inside. It breaks "the window is exactly the panel" | `frameGlass` holds within 0.5 px during the arrival [R], never tried at rest. `Panel` takes the window's width for the panel's [C]; clicks may land shifted while framed [I] |
| The same in a window of its own, handed over | **Not possible** without a seam | The system fades every see-through task in and out, about 200 ms; an app cannot turn that off [R] |
| The window's width changing, its top edge moving | **Not possible** | Width: tried twice [R]. The top: a rule, and the same fault is expected [I] |
| A dim behind (the whole screen, bars too) | **Free to 0.5 day** | `placeWindow`, `present`: `FLAG_DIM_BEHIND`, per frame; a setting today |
| A drawn backdrop, anything beside the glass | **Not possible** in the panel's window. In a second window behind: 5 days or more, risky, don't | The root view is framed to the glass and clips; nothing draws in the shadow's room [R]. Two see-through tasks: order and focus unknown [I] |
| The panel on a drawn desk | **Free**, in the window only: a picture | `window/Stage.kt` |

## 4. What exists to reuse

| Piece | Where | Can | Cannot |
| --- | --- | --- | --- |
| The two cards | `Panel.kt` `CardBody`, `OverlayModel.card` | A mark, a title, three lines, two answers; back at every opening until answered | Stand while anything is typed (`card` is null once `query` is not empty); a third answer; key caps |
| Tips | `Tips.kt`, `Panel.kt` `TipBody` | Three parts rise 22 ms apart; nothing armed at rest; "Try it" types | Come before 320 ms of quiet; run anything |
| Self-typing | `OverlayModel.typeOut`, `Effect.Type`, `Motion.typeStep`; from the window `BooklightApp.example`, `guided` | A keyword becomes its chip as its space lands; the user's first key takes over; Enter waits 350 ms | Set its pace (16 to 40 ms a letter); pause; delete |
| Key caps | `Footer.kt` `Keycap`, `KeyCaps`; `Body.Keys` | 22 or 24 dp caps, "+" between, arrows drawn | A large cap, a pressed state: none exists |
| Drawn check | `Marks.kt` `DrawnCheck` | One stroke in 140 ms, any size, undoes itself | |
| Level, grid | `Bodies.kt` `LevelTrack`, `GridBody` | Drawn from a value, real or made up | |
| Footer | `Footer.kt` | A word with the check (`say`, 1.6 s), a key hint, esc | Show without rows |
| Demo stage | `window/Stage.kt` `Stage`, `Stages.Demo` | This device's real list at up to 0.75 size; a typed loop | Take keys, run anything, show a card, the arrival or the edge light; leave the window |
| Pinned window | `pin/PinActivity.kt` | Stay on top beside the helper, without the keys [R]; six lines | Be glass; arrive our way; keep the user's own pin; escape the helper's dim [R] |
| Edge light | `Glass.kt`, driven in `Panel` | One lap; steady while `model.working` | A new trigger needs a hook (0.5 day). It means "working" today |
| Motion | `Motion.kt` | `place`, `lead`, `trail`, `pop`, `arm`, `fade`, `stagger`, `roll`; all cut with system animations off: the still comes free | The docs say `open` is spring(0.9, 800); the code says 1.0, 1000, for the turn only |
| Unfold | `Panel.kt` `Arrival` | 210 ms, 420 at Medium (the default). Folding back to the bare field is the height spring: free | Escape the system's 200 ms fade [R] |

## 5. State

- **Where** [C]: `data/Prefs.kt` `Settings`, in `files/settings.json`. The panel and its `OverlayModel` are made
  anew at every opening (`finishNow`): nothing else survives a close.
- **Schema** [C]: `SCHEMA` is 6. Add 7 and a step in `migrate` that marks existing installs; a new field's
  default alone would send everyone through. A new install has no file and skips `migrate` (`load`).
- **The gotcha** [C]: `ignoreUnknownKeys`, and `update` writes the whole file. An older build drops the new fields
  and writes schema 6 back; the migration then runs again, so it must give the same answer twice.
- **Resume**: a step is written when it is done; the next opening shows the next one after the gate, as `card`
  does. Not when `guided`, not under a chip.
- **Replay**: reset those fields and open the panel from the window. Precedents: "Show tips again"
  (`window/Pages.kt`), `./bl debug pref cards`. It must not touch `suggestions`.
- **An update from 3.0** [C]: what there is to judge by: `keySeen`, `shortcutCard`, `suggestionsCard`, `tipsSeen`,
  `used`, `history.json`. For installs older than 2.0, `keySeen` is itself a guess: "answered the card, or has
  picked anything" (`migrate`, schema 3).

## 6. The three switches

| Switch | Read and set today [C] | For first run |
| --- | --- | --- |
| Search suggestions (`Settings.suggestions`, off) | Read in `OverlayModel.search`, then `SuggestProvider.fetch`. Set by the card (`OverlayActivity.card`) and the window's switch | **The card arms "Turn on": a bare Enter consents** (`Panel.keys`, `cardChoice`). A tip's strip arms nothing at rest (`lit = false`): use that. A failed request looks like no suggestions |
| Show your usual (`Settings.zero`, off) | `OverlayModel.init` works the rows out once per opening; `offerZero`; core `Zero.pick`. Set in the window | Switched on in an open panel it shows nothing until the next opening. With no history, nothing: two things, each run twice. Never while a card stands (`Under.choose`) |
| The device's model | `ai/OnDevice.kt` `state`: UNKNOWN, NONE, DOWNLOADABLE, DOWNLOADING, READY. **No switch exists**; "Get" on a prompt's row calls `download()` | READY: nothing to switch. DOWNLOADABLE: "Get" starts the system's fetch, about 1.5 GB [R], shown in MB. NONE: say so, or leave it out. An "off" is a new setting (1 day) |

The model's times [R, the Lenovo; the HP unchecked]: about 2 s to load the first time, a first word 0.3 s later,
70 to 100 characters a second. It answers only the app in front [R]. `check()` asks a "downloadable" model a
question and waits up to 4 s (`answers`, `PROBE_MS`) [C]. A look that asks the model nothing is new (0.5 day);
on a device that calls a model it has "downloadable" it would offer "Get" for a model that is there [I, from the
comment in `OnDevice.checked`].

## 7. Trying things for real

`OverlayActivity.run` [C]: `keepOpen`, or Shift held: the panel stays. A word to say ("Copied"): it locks
(`settled`) and closes after 520 ms. Anything else: it closes at once. Whatever opens a window takes the focus
too.

| Try | What happens | To come back |
| --- | --- | --- |
| Open an app; search inside one | The panel closes | The user presses the key: a new panel, first run goes on from `Prefs`. Without a key there is only the icon |
| A sum | The answer shows as it is typed. Enter copies and closes | First run can pass `keep = true`, as Shift does: it copies, says so, stays (0.5 day) |
| Volume | Left and Right set it at once (`OverlayModel.nudge`) | Nothing: the panel stays |
| A prompt | The answer is written into the row (`Effect.Ask`) | Nothing: the panel stays |

Also `keepOpen` [C]: a keyword's row, an app's Search and Play (the app becomes the chip), `?` and its "try",
media keys, Window, a to-do's tick, "Get", "Don't suggest". Write a step as done *before* its effect runs. A real
run is learned (`OverlayModel.learn`): it seeds the history and "your usual".

## 8. Do not design against these

- Knowing that a shortcut exists, which keys it has, or that it was just set. Only: the key opened the panel.
- Opening the helper on Customize, on its search or on "+"; prefilling it; drawing in or over it [R].
- Anything to read or click on the panel while the helper is up.
- Naming the keys the user chose; telling the key from another app starting Booklight (`byKey`).
- A restored device: `keySeen` and the history travel with the backup (`res/xml/backup_rules.xml`) [C], the
  shortcut does not [I].
- A change of width, a moving top edge, anything outside the glass, an opening taller than the field.
- **A line that stays under the field while text is typed, or under a chip**: rows take that place and `card` is
  gone. It is new (1 day). Free seats today: the field's placeholder (`Field.kt`), the footer's word.
- The model's state the moment a screen is drawn; a first answer under 2 s; a right answer every time
  ("Googlebooks" became "Google Books" [R]).
- "Your usual" with real rows on day one.
- Coming back by itself after an app was opened.
- A key that waits: typing owns the frame, Esc and a click outside always close, a held key repeats.

## 9. Checks only a device can answer

1. `./bl open stay`, then Action + /: is the panel still drawn, how dark, does the helper cover it? Light and dark.
2. The same with a throwaway `onProvideKeyboardShortcuts` on the panel: does the helper open on Booklight's page?
   How long a label before it wraps, in German too?
3. Close the helper (Esc, Done, a click beside it): does the panel have the focus, and the field the next letter?
4. Helper open, press the new key: does `onNewIntent` arrive, does `byKey` say yes, does
   `dismissKeyboardShortcutsHelper()` close the helper in Customize?
5. Before any shortcut exists, press Action + J in the panel: does the panel see the key?
6. A step 400 dp high, `./bl open stay slow=4`: the lower edge as it grows, both devices, the Slow opening too.
7. For a larger glass: a window 240 dp wider, the glass framed at rest: a blurred band? Does a click land on the
   row under the pointer?
8. `./bl open stay dim=0.3`: a dim for first run, light and dark.
9. `./bl debug ai none|downloadable|downloading|real`: each state; and the time from opening to `check()`'s answer.
10. An umlaut and a dead key in a typed step, on a German layout.
11. A fresh install: "Open" in the store against the icon: the panel or the window?
12. System animations off: does every step read as a still?
13. The HP: the first opening of any first-run build: a blurred rectangle round the growing glass?
