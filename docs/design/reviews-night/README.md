# The night of 2 October 2026: the reviews of what was built, and what was done

Three features were built in one night on the branch `m1-the-copy`: the copy (M1), flights (M5) and your usual
(the zero state). Two reviews followed. (The copy had been reviewed once already: `../reviews-m1/`.)

## The design review

An interaction designer ([ux.md](ux.md)), a visual designer ([visual.md](visual.md)) and a motion designer
([motion.md](motion.md)) looked at flights and your usual, and at the copy again, from stills in light and dark
and from films taken on the Lenovo Googlebook over the test backdrop. Their verdicts: the copy approved (two
of three with must-fixes of its transitions, done below); flights approved with the must-fixes; your usual
approved.

| Finding | Done |
| --- | --- |
| A flight's row under a copy said "Looking it up" while nothing was asked (UX 1) | Under a text's chip the row is the plain one that names the airline; going to it (Down, Tab) looks it up. Nothing is sent for it unasked, also under `clip` |
| The footer's "AirLabs · 1:46 AM" was uncovered letter by letter (motion 1); it was a size larger than the hints beside it (visual 3) | No size animation of its box; the hints' size |
| The caret ran through the copy's chip on Tab (motion 2, UX 10) | The chip takes its room in one step, once the placeholder it replaces has faded where it stood |
| A two-line answer in a four-line row, again (motion 3) | The answer is trimmed as it arrives, and a last empty line is not counted |
| The footer was cut by the glass's lower edge as the usual rows arrive (motion 4) | It shows once three quarters of its band are there |
| Rows two and three were cut by a hard line while they rise (motion 5) | The list's lower edge is soft while the panel is still growing, as under an opened row |
| A flight's answer landed as a cut (motion 6) | Its lines fade out and in where they stand; digits typed still change at once |
| Each digit of a flight number made a new row (motion 7, UX 11) | A flight's full row keeps its seat while its number is typed |
| A strip that leaves was cut in one frame (motion 9) | The strip's state is equal by its key, so its fade runs; strip to strip, the new one waits for the old |
| The line's glyph and row one's glyph lay over each other (motion 13) | The glyph goes with the words; the disc stays |
| "Don't suggest" was line one of a seated app's list, so arrow, Enter, Enter no longer gave "Left third" (UX 6) | It is the last line before Uninstall; every other line keeps its place |
| "The two things you run most" is not true while links and recipes are passed over (UX 7) | The switch's text says so |
| Shift + Enter in an opened list of the usual rows dropped the highlight (UX 8) | The highlight stays on the row whose list closed |
| `tr guten morgen` offered "In guten" (UX 9) | No language is named that Booklight does not know: Gemini gets the whole text and is told how to read it |
| "ins Englische" was not a language (UX 12) | The German name's form after "ins" is found |
| Flights: the key's text contradicted itself; a landed flight offered "Add to calendar" first; "Get times" and "key" read wrongly; a typed day took the row away; the empty row repeated the placeholder; no second try after "No connection"; "LANDS" moves on a 24-hour clock (UX 2, 4, 5, 14, 15, 16; visual 1) | With the agent that built flights; see its commits after this file |

Left, and why: second ink (kind labels, footer hints, key caps, slot labels) measures 2.2 to 3.2 : 1 on glass
over a window of the other brightness, and the visual designer asks for full ink there in every list (visual
2). That changes the look of every list, so it is a question for Alex, not a fix of the night. The thin arrow
in a flight's caption (visual 4). The placeholder at x = 74 over titles at 72 (visual 5: leave). The hint's two
labels over each other for one frame (motion 10); the answer's first word over the fading question (motion 11);
rows that come back inside a pill that is still shrinking (motion 12). A weak flight guess is the last local
row, not the last row, when suggestions are on (UX 13).

## The code review

Six reviewers read the whole change by area (the copy, flights, your usual, races, consistency, privacy); a
second reader then tried to refute each finding. Fifty were confirmed (several are one fault seen from two
areas), two refuted, one left unverified.

| Severity | Area | Finding | File |
| --- | --- | --- | --- |
| high | consistency | `clip` sends a flight number found in the clipboard to AirLabs unasked; the code's own comments and PRIVACY.md say it never is | `OverlayModel.kt` |
| high | flights | A pinned flight whose landing was not seen never stops asking: it becomes the next day's flight, or a past plan at "0 min" | `PinnedFlight.kt` |
| medium | consistency | A prompt typed by its keyword still takes 8,000 characters while answers stop at 768 tokens: the rewrite comes back cut, against CHANGELOG, ux-model §15 and OnDevice's comment | `Prompts.kt` |
| medium | consistency | "Is the model here" is decided two ways, and the new `trusted` rule is applied to PromptScope, which never trusts: its "Get the on-device model" row no longer appears | `OverlayModel.kt` |
| medium | consistency | PRIVACY.md says the copy reaches the model only on Enter; `tr <language>` and a prompt keyword with nothing typed ask after a 500 ms pause | `PRIVACY.md` |
| medium | consistency | The copy's line says "a flight" from the system's classifier, the row after Tab comes from Booklight's own reader: the line can promise a row that is not there | `Clipboard.kt` |
| medium | consistency | Documents are behind the tip commit: "a flight in a copy" is called not built, and no as-built text names it | `PICKING-UP.md` |
| medium | copy | German: rows under a copy are found by the copied text, through the web-search row's name | `Picks.kt` |
| medium | copy | A prompt typed by its keyword lets in 8,000 characters; the answer is capped at 768 tokens and shown as whole | `Prompts.kt` |
| medium | copy | When the classifier is slow the copy's chip stands over no rows, and its link, date and language are never read for that text | `Picks.kt` |
| medium | copy | A flight that is row one under a copy says "Looking it up" and is never looked up | `Flights.kt` |
| medium | flights | A pinned flight that is still hours or days off costs 2 or 3 requests every half hour for an answer that cannot change | `PinnedFlight.kt` |
| medium | flights | A cancelled next flight is answered as "Planned · from the timetable" | `FlightStatus.kt` |
| medium | flights | "The next flight" from the timetable is the first line that flies within a week, not the soonest | `AirLabs.kt` |
| medium | privacy | `clip` sends a flight number out of the clipboard to AirLabs unasked (wired in by 380d6a2, which landed during this review) | `OverlayModel.kt` |
| medium | privacy | A copied link's scheme is never checked: any `x://` goes to ACTION_VIEW, the row's title hides the scheme, and a failed open writes the address to the log | `Picks.kt` |
| medium | races | A probe of the "downloadable" model that the panel's closing cuts short counts as "tried in vain" for a minute | `OnDevice.kt` |
| medium | races | TextScope.read marks the text as read before the classifier has answered: a timeout or a second search loses the found things for the life of the chip, and the list can land empty | `Picks.kt` |
| medium | races | A flight's row under the copy's (or handed-over) chip waits for an answer nobody is fetching: Copy, Pin and Add to calendar do nothing | `OverlayModel.kt` |
| medium | races | Under the keyword `clip` a flight number found in the clipboard is sent to AirLabs unasked (outside my area; came with commit 380d6a2 during this review) | `OverlayModel.kt` |
| medium | usual | A seat holder that is gone for good freezes zeroHeld, so seat two is no longer held | `Zero.kt` |
| low | consistency | PRIVACY.md: "the only new things kept are the switch and the list of things you asked not to be suggested" leaves out zeroHeld | `PRIVACY.md` |
| low | consistency | `tr` and the translation under a copy's chip do one job three different ways | `Picks.kt` |
| low | consistency | TextScope.answered keeps code and a comment for something it no longer does | `Picks.kt` |
| low | consistency | Left behind by the branch: an unreferenced string, ten unused imports, two unused constants, one unused parameter | `strings_11.xml` |
| low | consistency | Three stated numbers or rules differ from the code or the findings | `Picks.kt` |
| low | copy | `clip` sends a flight number found in the clipboard to the service unasked | `OverlayModel.kt` |
| low | copy | A probe cut short by closing the panel counts as tried in vain: for a minute the HP's rows hand over to Gemini | `OnDevice.kt` |
| low | copy | The new keyword `tr` makes a user's own text-taking link `tr` unreachable | `Scopes.kt` |
| low | copy | A typed prompt on a fresh process no longer gets its "Get the on-device model" row when the system answers | `OverlayModel.kt` |
| low | copy | An answer given under the copy's chip stops the same question from being asked by keyword | `OverlayModel.kt` |
| low | copy | On a device whose language `tr` does not know, an English copy gets "In English" as its first row | `Picks.kt` |
| low | copy | German lead word `ins` never leads to a language: "ins Englische" is taken as an instruction | `strings_23.xml` |
| low | copy | The line says "a flight" from the system's classifier, which the row's own reader contradicts | `Clipboard.kt` |
| low | flights | A day's flight that has already flown is shown as "Planned · from the timetable" | `AirLabs.kt` |
| low | flights | A number the service does not know costs two lookups, and is forgotten after two minutes | `AirLabs.kt` |
| low | flights | A timetable flight dated after a clock change is an hour out | `AirLabs.kt` |
| low | flights | A date more than six days ahead is shown, copied and pinned as a weekday alone | `Flights.kt` |
| low | flights | The count of lookups left stays the previous key's after the key is replaced | `FlightsGroup.kt` |
| low | privacy | PRIVACY.md and two setting texts say things about the clipboard and what is kept that the code does not do | `PRIVACY.md` |
| low | privacy | intents.md shows by implication which apps are on the test Googlebook | `intents.md` |
| low | races | Tab or Down on the copy's line reads the clipboard's content on the main thread, without a cap | `OverlayModel.kt` |
| low | races | `thinking` is written by both the flight lookup and the model's answer: Enter on an Ask row is dropped while a slow lookup's light is on | `OverlayModel.kt` |
| low | usual | Ctrl + digit on the "Don't suggest" line of an opened list says "Couldn't do that" | `OverlayModel.kt` |
| low | usual | The screen reader's "Don't suggest" action on an app's row opens the app; at rest every custom action does nothing | `Rows.kt` |
| low | usual | AppsProvider.ready is assigned after the init block that starts the job which completes it | `AppsProvider.kt` |
| low | usual | With the switch off, the 320 ms mark still calls offerZero and looks at the clipboard once more | `OverlayModel.kt` |
| low | usual | With the switch off: after App info, Edit, Delete or Uninstall, Up brings back a text from before that run | `OverlayActivity.kt` |
| low | usual | Tab in the frames between a letter and its list landing moves the highlight in the old usual rows and takes the keyword's Tab away | `Panel.kt` |
| low | usual | Debug hooks the device pass needs are missing: pref prints no zero= or tips=, dump has no copy state, no log line per opening | `DebugReceiver.kt` |

Done on the night, in the commit "After the code review" and the ones after it: all of the copy's, your
usual's, the races', the consistency and the privacy findings, except these, which are left: `tr` and the
translation under a copy's chip still do one job in two places (consistency); the debug `dump` does not say
whether a copy was fresh, and there is no log line per opening for the usual rows (usual). The flights findings
are with the agent that built flights.

The most important ones, in plain words: with a key in, `clip` sent a flight number found in the clipboard to
the service unasked (it came with the commit that put a flight's row under a copy, an hour before the review
found it; now nothing is asked until the user goes to that row); a pinned flight could go on asking for ever;
a prompt typed by its keyword took 8,000 characters and gave back a cut answer; a copied link of any scheme was
handed to the system to open; and the first look at what is in a copied text could be lost for good if the list
was made anew while it ran.
