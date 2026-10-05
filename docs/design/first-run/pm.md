# First run: the product paper

*PM seat, 4 October 2026. Read: the brief and what it points to, the `guide` and `tips` tables (`strings_20.xml`), and the code named below. Nothing was changed, no device was touched. A number called a target is ours to meet, not a finding.*

> **Second round, 4 October (evening).** Alex answered the first plan: the suggested key is Action + Quick Insert
> (not Action + J, which stays the suggestion for a keyboard without that key); first run begins with an opening in
> which Booklight performs; the panel stays through the three lessons (decision 4 here, the other answer); and the
> last screen has no row for the device's model (decision 8, the other answer). This paper is as it was written
> before that: where it differs, `README.md`, `design.md`, `motion.md` and `eng.md` §12 hold.

## 1. Who this is for, in minute one

A Googlebook owner with a keyboard under their hands. They found Booklight on Play (Googlebooks only) or took the APK from GitHub (`README.md`), and have just pressed **Open** in the store or the installer, or clicked the new icon. They give it about a minute.

| They know | They do not know |
| --- | --- |
| The promise: press a key, type, Enter | That there is no key yet, and that only they can set one, in a dialog of the system's (`shortcut-setup.md` §1) |
| Spotlight or Alfred, perhaps | That Tab goes along a row; that `?` lists everything |

Today, by the code, Open shows the panel with a card and the icon shows the window (`OverlayActivity.fromIcon`). The card is done once its button is pressed, key or no key (`OverlayActivity.card`): whoever fails in the helper is not asked again.

## 2. The one thing, and how we would know

**Their own key opens the panel, and they run one thing with it.** Without a key Booklight is an icon that opens settings.

Booklight has no analytics and gets none. What can be checked by hand:

| Check | How | Target |
| --- | --- | --- |
| The key works | The window's Start page says "Your key works" (`key_works`, from `Settings.keySeen`) | 4 of 5 people, unaided |
| Time | A stopwatch, first screen to bare field | 60 s for the middle person; the key in 30 s |
| Nobody is stranded | After step 2 has opened an app: is the key pressed again, untold? | 4 of 5 |
| It stuck | Ten minutes later: "search that app for something else" | 4 of 5 |
| The question was understood | "What is sent, and to whom?" | All name their typing and the search engine |

The hallway test: five people new to Booklight, a fresh install, no help; note where each stalls in the helper. By hand on a device: Esc at every step, then open again; the model in each state (`./bl debug ai …`); an update with and without a key (`./bl debug pref key seen|no`); German. How many real users finish, we will never know.

## 3. The three functions

| Function | No key, account or setup | Safe for real | Shows | Habit |
| --- | --- | --- | --- | --- |
| **1. Open an app**: a few letters, Enter | Every device | An app opens | Going | Many times a day (`app-structure/pm.md` §1) |
| **2. Search inside an app**: the app, Tab, Enter, a word, Enter | The app is chosen per device, as the tip's is (`Guide.appSearch()`): one that needs no sign-in. None here: Settings, by `s`, Tab, `wifi` | It opens on its results; nothing leaves before that Enter (`CHANGELOG.md`, 3.0) | Doing: Tab moves, Enter does | Daily (same table) |
| **3. A sum**: `150 + 20%` | Every device | Enter copies | An answer; nothing opens | Often |

Go, do, answer: one of each kind. The second is what 3.0 was built around, and Tab is the one key nobody guesses.

| Left out | Why |
| --- | --- |
| Play by name; a flight's times | Each needs the user's own key (Labs) |
| A prompt (`fix teh text`) | The model can be absent or on its way (`OnDevice.State`); the Gemini app then opens instead |
| An app in a place (`chrome left`) | Only for an app that is not open yet (the guide's own line), and the user's may be |
| Timer, note | A timer rings later in the Clock; a note asks for a folder first (`notes_choose`) |
| Web search | The last row of every list already offers it |

## 4. The steps

Each step stands under the empty field, where the card stands today (first in `Under.choose`, core `Zero.kt`). **Enter does what it does every day**: the app opens, the panel goes, the key brings the next step. So the key is pressed four times in the first minute. No step has a clock; none moves on by itself.

| # | Step | The user does | Done when | Budget | Skip |
| --- | --- | --- | --- | --- | --- |
| 1 | Your key | Enter on "Open Keyboard shortcuts". There: Customize, types Booklight, +, the keys, Set shortcut. Then presses the keys | The key opens the panel (`keySeen`) | 25 s | "Not now" ends first run (§6) |
| 2 | Open an app | Types the first letters of any app, Enter | An app's Open ran | 6 s | The next step, at once |
| 3 | Search inside an app | The key. The app's letters, Tab, Enter, a word, Enter | An app's Search ran | 12 s | The same |
| 4 | A sum | The key. Types a sum, Enter | Its answer was copied | 6 s | The same |
| 5 | Your choices | The key. Answers the suggestions question; switches what they like; types `?` | The question is answered; then any key | 10 s | "Not now" is an answer |

59 s when nobody stalls. If the test runs long, the sum goes first: a tip teaches it too.

**Step 1.** One reason, one button named for what it opens, the suggested keys as key caps. The surface that asks stays in front, so the helper opens on Booklight's page and that page carries the path (`shortcut-setup.md` §2); which surface, the tech lead says. Opening the helper is not "done". A key pressed while that surface is in front is success; today that press closes the panel (`OverlayActivity.onNewIntent`).

**Steps 2 to 4.** The real field, the real list; any app and any sum counts. One line of guidance stays while the user types (the placeholder and the footer are free, `eng-constraints.md` §8) and ends on the key caps: "for the next one".

**Step 5 is two screens**: the store's rule wants the suggestions disclosure alone (§8). First the question. Then "Your usual", a switch, off, whose line says nothing changes until two things were run twice each (`set_usual_text`); the model's row; and `?` (§7).

| `OnDevice.State` | The model's row |
| --- | --- |
| READY | "Answers on this device", with an example for later. No control |
| DOWNLOADABLE | "Get the on-device model" (`model_get`), with its size: about 1.5 GB (`eng-constraints.md` §6). Never armed |
| DOWNLOADING | "The system is fetching it" (`model_waiting`); it goes on after the panel closes |
| NONE, or not known when the screen is drawn | No row. Nothing arrives late |

First run may ask the system for the status, and the model nothing: `OnDevice.check()` tries a "downloadable" model with a question, and `PRIVACY.md` promises "if you never use a prompt, the model is never asked".

## 5. The key to suggest

**Action + J, "or any keys you like".**

- Android 17's source leaves D, J, O, R, T, X, Y, Z alone with Action; only J, O and R mean nothing with a second modifier either (`shortcut-setup.md` §3).
- Two keys, for something pressed all day, and first pressed inside a system dialog: Action + Alt + Space without the Alt reads "Key combination already in use" there (`device-findings.md`, "The keyboard shortcut").
- J is in one place on English and German keyboards (Y and Z are not); O sits beside L, which locks the screen (`keys_table.xml`).

The README, the Start page (`Pages.kt` draws Action + K today) and `win_key_text` take the same suggestion.

## 6. Skips, quits and returns

| Who | Gets |
| --- | --- |
| Skips at once ("Not now" on step 1) | First run is over: no key, no loop to practise. Suggestions off, tips as today. The key's status and the replay wait on the window's Start page |
| Presses Esc at any step | The panel closes, as always. The next opening shows the same step |
| Comes back from the helper without a key | Step 1 again: "Now press your keys" (`key_press`), the button, and "Didn't work? Try other keys" |
| Set a key, never pressed it | Booklight cannot know (`shortcut-setup.md` §2). The first press, days later, is greeted, and step 2 follows |
| Ignores a step in three openings | It counts as skipped; the question, as "Not now". No step nags |
| Updates from 3.0 with a key | Nothing. The replay is there |
| Updates from 3.0 without a key | Step 1 only, once: the old card sent these people the long way |
| Restores a backup on another Googlebook | `keySeen` comes along, the shortcut does not (`eng-constraints.md` §8). Nothing shows by itself; the replay's step 1 offers the key again |

For the tech lead: an update must be told from a new install (a new setting that defaults to "not done" would send everyone through).

## 7. Learn more, and the replay

- **The last screen says one thing: type `?`.** The list of everything, what is new to the user first, each line with an example Booklight types (`scopes/Help.kt`). Typing it ends first run inside the product; Enter and Esc end it too.
- One quiet line: "Play by name and flight times need a key of your own: Booklight's window › Labs."
- **Tips** go on as built and pass over what first run had the user do (`Tips.next` reads `Settings.used`).
- **The window's Commands page** is the last row of `?` (`help_all`). **The README** serves those who read before installing.
- **The replay is "First steps" · „Erste Schritte“**: a typed command beside "Booklight settings" (`CommandsProvider`; also found by tour, welcome, intro), a line in `guide`, and a row on Start beside "Show the tips again". With a key that works it shows step 1 as done, with "Change the key", and starts at step 2. It changes no switch, and asks the question only while suggestions are off.

## 8. The suggestions question

The store's rules (`first-run-practice.md`, "Consent wording"): in the app, in normal use; the data, the purpose, who gets it; apart from other notices; an action that says yes; nothing sent before it; a way to say no, and yes later. So:

- **Alone on its screen.**
- **Nothing is armed.** Enter does nothing until Tab or a click chose an answer, as on a tip (`ux-model.md` §14); today's card arms "Turn on", so a bare Enter consents (`eng-constraints.md` §6). Esc, a lost focus and waiting are no answer.
- It adds what today's card lacks: when, and the IP address. `store-submission/forms/data-safety.md` quotes the card and takes the new words.

| | English | German |
| --- | --- | --- |
| Title | Suggest searches as you type? | Beim Tippen Suchen vorschlagen? |
| Text | Booklight sends what you type to %1$s while you type, to suggest searches. %1$s also sees your IP address. Not sent: sums, web addresses, and anything after a keyword or with an app in the field. | Booklight sendet, was du tippst, schon beim Tippen an %1$s, um Suchen vorzuschlagen. %1$s sieht dabei auch deine IP-Adresse. Nicht gesendet werden Berechnungen, Webadressen und alles nach einem Stichwort oder mit einer App im Feld. |
| Small line | Off until you agree. Change it in Booklight's window › Results. | Aus, bis du zustimmst. Änderbar in Booklights Fenster › Ergebnisse. |
| Answers | Agree · Not now | Zustimmen · Nicht jetzt |

## 9. Left out, and where it lives

| Not in first run | Lives |
| --- | --- |
| Theme, glass, shadow, opening | Window › Look. Nobody can judge a look before use (`first-run-practice.md`, "Things to avoid") |
| The search engine | Window › Results; the question names the one that is set |
| Spotify and flight keys | Labs, named once on the last screen |
| The assistant key, the tile, the widget | Window › Start, as built |
| The notes folder, the brightness switch | Asked where first needed (`notes_choose`, `dial_needs_grant`) |
| Places, pin, timer, prompts and the rest | Tips and `?` |

## 10. Decisions for Alex

| # | Decision | Recommended | If you say the other thing |
| --- | --- | --- | --- |
| 1 | The three functions | Open an app · Search inside an app · A sum | "An app in a place" for the sum: more of a show, and it fails for an app that is already open |
| 2 | The key | Action + J | Action + Alt + Space: known from Spotlight, no letter to lose to a later build; a third key at every opening |
| 3 | The very first start from the icon | First run in the panel, once; after that the window, with a row on Start that goes on with first run | The window, always: whoever starts from the Apps list meets the settings pages first |
| 4 | Enter in steps 2 to 4 | What it always does; the key brings the next step | The panel stays open through all three (`eng-constraints.md` §7): shorter, nobody stranded, but an Enter that exists nowhere else, and the key pressed once |
| 5 | The switches | The question alone, then one screen for the other two and `?` | One screen for all three: a disclosure among other choices, which the store's rule speaks against |
| 6 | The question at rest | Nothing armed | "Agree" armed: more say yes, some by a stray Enter |
| 7 | "Your usual" for a new install | Offered, off | On: their apps are on screen at every opening, and the tips end sooner |
| 8 | The model in first run | The status only, a row only where there is something to say; one sentence more in `PRIVACY.md` | No look: no row here; the model is met at the first prompt, as today |
| 9 | Updates from 3.0 | With a key nothing; without one, step 1 once | Nothing for anyone: those who failed in the helper stay without a key |
| 10 | The replay's name | First steps · Erste Schritte | Tour or Welcome: shorter, less plain |
