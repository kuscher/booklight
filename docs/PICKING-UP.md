# Picking up

*Living status. Newest first.*

## 2026-10-02 (night): the copy, flights and your usual are built on a branch; nothing is released

Alex's word for the night: "go" on M1, then "change out the air api with Airlabs ... have a setting for users to
add their own ... finish the flights work", "a great zero state experience as in issue kuscher/booklight#1 ...
behind an option to show it or the tips for now defaulting to the tips", a clean-up and review, and a plan for
commands that control other apps.

- **Where it is.** Branch `m1-the-copy` holds all three, on top of main (2.2). Not merged, not tagged, not
  pushed; the version is still 2.2 / code 6. `CHANGELOG.md` "Unreleased" says what is in it. Branch
  `window-on-the-copy` (a git worktree) is the same with the rebuilt settings window on top.
- **The copy (M1)**: built, seen working on both Googlebooks, reviewed twice by an interaction, a visual and a
  motion designer (`docs/design/reviews-m1/`, `docs/design/reviews-night/`). As built: `ux-model.md` §15,
  `design-system.md` §13. Ready-made prompts are kept by reference (settings schema 5).
- **Flights (M5)**: built by an agent without a device, then tried on both Googlebooks with Alex's own AirLabs
  key (which is in no file of the repo: it lives in the app's own storage, and for tests in the private config
  folder). The source is AirLabs, not AeroDataBox: `docs/research/flights.md` §10 has what the service really
  answers. A flight number in what was copied gets its row under the copy's chip; nothing is asked for it until
  the user goes to that row. Not built: the usual route without a key (the letter to that table's host is not
  sent). The drawn page still names the old service.
- **Your usual (issue 1)**: designed by a group of five and two critics (`docs/design/zero-state.md`, with
  fourteen decisions for Alex), built with the cuts its head names, behind the switch "Show your usual", off.
  Tried on the Lenovo only.
- **The reviews** (`docs/design/reviews-night/README.md`): six code reviewers with a second reader each (fifty
  findings confirmed, fixed but for three small ones named there), and three designers on the built features.
- **Found on the devices** (`docs/research/device-findings.md`): the HP's model answers although the system
  calls it "downloadable" (Booklight now tries it); a text of 1,900 characters is translated whole in 36 s; the
  Lenovo calls a postcode a phone number.
- **Next five** (`docs/research/next-five.md`, from `docs/research/intents.md`): commands for other apps.
  A plan, nothing built. What a player does when asked to play by name is still not verified.
- **The settings window** is finished and reviewed on its own branch (`worktree-agent-…`, the reviewed state)
  and, with the night's three new settings carried into it (Start: "Your usual", "What you copied"; Results:
  "Flights"), on `window-on-the-copy`. Not merged: Alex has not seen it.
- **The release build** with all three features was tried on the Lenovo with real keys: the line, Tab, an
  answer in the row, the usual rows, a flight with its answer. About 1 % of frames late in each step.
- **Not done, or not verified:** the release build on the HP (the HP was not available for the rest of the
  night; an earlier release build there handed a model's row to Gemini, and what was changed for that is
  untried there); your usual and flights on the HP at all; the screen reader; a second display; the pointer.
- **Open for Alex**, beside the fourteen decisions of the zero state: merging and releasing; whether second ink
  becomes full ink in every list (the visual review's finding 2); whether a typed prompt should wait for Enter
  as a row under a copy does; the eleven decisions of `next-five.md`.

## 2026-10-01 (night, later): 2.2 released; the plan is M1 and a new M5; the window is being built

- **2.2 (versionCode 6) is released** on Alex's word ("Yes, release 2.2 now"): everything that was "on main after
  2.1" (the entry below). Before the tag the release build was installed on the Lenovo: a prompt was answered
  on the device, an app's row has its six icons, and the opening drew every frame 8.3 ms apart. Not looked at
  for this release: the HP, German on a device. On Play it is a draft on closed testing; sending it for review
  is the Play session's, on Alex's word. Play's production is empty on purpose.
- **Answered by Alex** (four questions): M1 then M5; 2.2 now; a flight's times through a key of his own; the
  service is asked after a short pause. The other decisions of `plan-next.md` §6 are open at their recommended
  answers.
- **Seen during the release check, not a fault:** `adb shell input text` with a keyword and its text in one go
  leaves the keyword in the field beside its chip; typed key by key it does not. And a typed prompt is asked
  after a pause in typing (2.0's behaviour), which the M1 plan's rule "the model is asked on Enter, never after
  a pause" would change: to be settled when M1's designs are made final.

- **The plan in force is `docs/design/milestones/plan-next.md`** (page: `docs/design/booklight-milestones.html`,
  https://claude.ai/artifact/YWPmPVwvSFiY1keZCbyCYt). Alex: "adjust the plan for the main ux by doing M1. Let's add a
  new M5 which handles flights. I want to type LH455 and get info about the next flight." So: 2.2 (what is on
  main), a half-day device session, M1 the copy, M5 flights. M2, M3 and M4 are parked. Thirteen decisions and
  three questions wait for his answer; nothing of M1 or M5 is built.
- **M5's hard fact** (`docs/research/flights.md`): no free source gives a flight's times without a key. The row
  names the airline and opens the flight's page for everyone; the times, gate and status need the user's own
  free key (AeroDataBox through RapidAPI). No keyed service was called: there was no key.
- **The settings window is being built** on a branch in a git worktree of its own, on his word ("kick off the
  settings window redesign"), with the nine recommended answers of `docs/design/window-redesign.md` §10. His
  note while it ran: "the search engine settings ux is really misaligned". Not merged, not released.
- **The repo's history was rewritten on 1 October** by the Play session (device identifiers and private project
  names taken out; see "This repo is public" in `CLAUDE.md`). Commit ids named in older entries below are from
  before it. No tag until Alex says so: a tag's upload also sends what is waiting unsent on Play.

## 2026-10-01 (late night): on main after 2.1 (released as 2.2 later that night)

Alex used 2.1 and asked for small improvements on main, each to go through a visual designer and a motion
designer (his words are at the top of `docs/design/reviews-after-2.1/plan.md`). Built, reviewed over one visual
round and its confirmation and four motion rounds of recordings on the Lenovo, approved by both, committed.
**Not tagged: no release was asked for.** The version is still 2.1 / code 5; a release needs 2.2, code 6 and notes.

- What changed: `docs/design/design-system.md` §12 (the opening at Medium and its curve, the leaving's time,
  the turn, the white reflection, the dark selection, six icons on an app's row). `CHANGELOG.md` has the short form.
- **A fault found on the way, older than this work:** since 2.0's shadow the seam the panel opens out of was
  never on screen (the root view shows nothing while its outline is empty). Fixed in `PanelOutline.caster`.
- **Chrome's "New tab" cannot be offered** by a plain app: `device-findings.md`, last section. The digital
  assistant may start such shortcuts (read in the Android 17 source, not tried).
- **The blur follows the glass** (Alex's next note: the blur "just pops in"). The window's root view is framed
  to the glass before each frame (`OverlayActivity.frameGlass`), so the glass is frosted from the seam on and
  through the fold; the window still never moves. How the blur's region works, the system's own fade over the
  first 200 ms, and the window resize that was tried and thrown away (Alex saw it open "one side first" on the
  Lenovo): `device-findings.md`, "The blur follows the glass". Motion review five approved it with nothing
  outstanding; the release build draws the whole opening at 8.3 ms a frame.
- Settings schema 4: a stored `fast` becomes `medium` once.
- Debug hooks added: `./bl debug activity PKG/CLASS`, `think on|off`, `turn MS`.
- Release build checked on the Lenovo (a prompt answers; frame times: the opening 56 frames with only the
  window's first one to three late, the reflection's lap one late in 249). The Lenovo has the release build
  with the blur change (its opening speed is set to Slow there, not by this work; left as found).
- Not done: nothing of it on the HP; German was not looked at on the device for the two renamed places.

**Two plans wait for Alex** (nothing of either is built):
- `docs/design/window-redesign.md` and its page (`docs/design/booklight-window.html`,
  https://claude.ai/artifact/Qp9U7LtwivskDF1dNTmouf): the Booklight window as a desktop window, navigation on
  the leading edge, five sections, Material 3 Expressive parts; nine decisions for him; about nine days.
- The four milestones (the copy, the picture, the selection, your language): `docs/design/milestones/plan.md`
  (with the team's four papers beside it) and its page (`docs/design/booklight-milestones.html`, made by
  `tools/design-milestones/gen.py`, https://claude.ai/artifact/YWPmPVwvSFiY1keZCbyCYt): eleven screens drawn at
  real size, fourteen decisions for him (eight large, six small), about 65 days with the language groundwork.
  It starts with a half-day device session on both Googlebooks, before M1.

**Device mishaps of this session, both put right:** six test timers had been ringing in Clock for two hours
(stopped; rule in `device-findings.md`); a recording helper resized another app's window on the Lenovo by mistake
(put back to its exact bounds; a task is now resized only after its line is checked to be Booklight's).

## 2026-10-01 (night): 2.1

Alex, after 2.0: "Fix the German decimal point. Add a new function to the app which lets me ask for e.g. German
or danish letters like umlauts or ß and have them in my copy paste. Make a release."

- **Letters** (`abc`, `core/Letters.kt`, `LettersScope` in `scopes/Picks.kt`): sixteen languages' letters in the
  picker's grid, by language, plain letter, the two letters it is written with, or its mark. `abc` is the only
  keyword (it is not a word anyone searches for); `umlaut`, `accent`, `letters` find its row without entering it.
  Recents in `Settings.lettersRecent`. A new language is one line in `LANGUAGES`.
- **Decimal comma** (`Calc.answer(text, comma)`): the language's decimal sign decides only the one ambiguous
  case (one sign, once, followed by exactly three digits); everything else is read by what it can mean.
- 221 core tests. Tried on the Lenovo in English and in German (`cmd locale set-app-locales`), debug and release
  build; the release build still answers a prompt on the device.
- Version code 5. 2.0 (code 4) was sent for Play review by the Play session about 17:25 PDT; 2.1 is a draft
  beside it until the Play session sends it.
- **Released:** tag `v2.1`, workflow green, GitHub release "Booklight 2.1"; the published APK was checked against
  its sums and is on the Lenovo.
- **On Play** (the Play session, on Alex's word to it): 2.0 was approved at 17:54 PDT and 2.1 is live to the closed
  testers since 18:56 PDT. Production is prepared there and not sent: Alex decides. The store's eight screenshots
  still show 1.1; nothing of 2.0 or 2.1 is in them.

**What to build next: research, nothing decided.** Alex asked for the unbuilt ideas, ten new ones, his own
(a screenshot asked about with the device's model; acting on the copy; a suggestion with nothing typed) and a
plan for more languages. All in `docs/research/`:
- `next-features-2.md`: the list in order (22 rows), the ten new ideas explained, five decisions for him.
  Recommended next release: the copy as a row, `do …`, put the letters back, prompts in the selection menu,
  more prompts, a clean link, stop what is ringing, `abc` by keystrokes.
- `screen-and-clipboard.md` (desk research with sources), `not-built-2.0.md` (143 ideas, 134 not built),
  `languages.md` (about four weeks for six European languages; Japanese, Korean, Chinese are a second block).
- Tried on the Lenovo with a throwaway hook (`device-findings.md`, last section): the model reads a picture of a
  region or a window correctly and invents text on a whole screen; the clipboard's description gives type, age
  and entity scores without the clip being read.
- Found on the way: six test timers had been ringing in Clock on the Lenovo for two hours. Stopped; the rule is
  in `device-findings.md` ("Rules for testing").
- Not checked: whether the Googlebook's capture tool copies the picture to the clipboard (it decides how a
  picture reaches Booklight).

## 2026-10-01 (night): 2.0

**Released.** Tag `v2.0` at 321fd59, pushed 23:55 UTC. The workflow was green on its first run: GitHub release
"Booklight 2.0" with `Booklight.apk`, `Booklight-2.0.apk` and `SHA256SUMS`; the bundle (version code 4) is a
**draft** on Play's closed-testing track beside 1.1.1. The published APK was downloaded, checked against its
sums, installed on the Lenovo and asked a prompt: it answers on the device. Sending the draft for review, and
the Console's forms, are the Play session's (below).

**What 2.0 is.** Alex ticked seven things in `research/next-features.md` and added the rest in his own words
(spec §1 and §2): other apps' commands as rows; a pinned window; the system's shortcuts as answers; more
places, nine icons and a list under the row; `?`; notes that grow; prompts; `s` and `k`; tips; a window with
a column of sections and a list of commands; a shadow around the panel; answers from the device's own model.
204 core tests; release build and lint pass; the release build opens in about 110 ms on the Lenovo.

**Reviewed, as built.** Four independent reviews of the built app: interaction, visual, motion (from frame
sheets recorded on the Lenovo at 120 fps) and code. Every must-fix is in, and the should-fixes but the ones
listed under "Not done". What the motion review changed: the answer is laid out once and uncovered in draw;
typing over an opened list brings the new rows at once; the window's Commands page is composed eight rows a
frame; the pin is never see-through from our side; one turning arrow; rows return as a closing list's edge
passes them. Two flaws older than 2.0 were found on the way and fixed: a shrinking panel left a strip of blur
with no glass under it for a frame (the glass is now sized from the window as it stands, `Panel`'s
`windowPx`), and an emptied list was cut in one frame instead of fading (`ResultsBody`'s `tall`).
**Measured on the release build** (Lenovo, `dumpsys gfxinfo`, keys typed into the panel): about 1 % of
frames late in the panel (3 of 320), under 1 % over twelve changes of section in the window, median 6 ms.
The debug build is three to five times slower in Compose: judge pacing on the release build only.

**What the devices decided** (all in `research/device-findings.md`):
- **The pin is a picture-in-picture window.** A window in Android 17's pinned layer takes the keyboard when it
  opens and again each time the panel closes; an app cannot hand it back. Picture-in-picture never has the keys,
  opens at the size the manifest names as its smallest, and needs no permission. So: no caption, no buttons of
  its own; Copy and Unpin are on the row `pin` shows. Its arrival is the system's (about half a second).
- **The shadow is the system's own**, in room the window's blur does not reach; the window's background clears
  the glass's shape first, so nothing of it is under the glass.
- **The on-device model answers Booklight** (Lenovo, `nano-v3`): first word about 0.3 s once loaded, 70 to 100
  characters a second. **The release build needs a keep rule** for ML Kit's registrars or every prompt falls
  back to Gemini: found only by installing the release build. Do that before every tag.
- The desktop gives a new single-task window of Booklight's the bounds of whichever Booklight window is in
  front; `FLAG_ACTIVITY_MULTIPLE_TASK` gets the bounds asked for.

**Alex's decisions for 2.0** beyond the spec's list: Google's ML Kit library as shipped (its usage reporting
stays: PRIVACY.md and the data-safety note say so); Play audience 18 and over; tips on from the start, Tab
then Enter, one pass; the shadow in low, medium and high.

**For the Play session** (it files these; this session does not touch the Console): the audience change to 18
and over; data safety gains Device or other IDs and Diagnostics, collected, for analytics, through Google's
ML Kit (`store-submission/forms/data-safety.md`, "2.0"); the privacy page needs `PRIVACY.md`'s new text; the
store's release notes are in `store-submission/listing/*/release-notes.txt`.

**Not done, known**
- The HP was offline the whole time: nothing of 2.0 has run on it, the on-device model least of all. Install
  it there and try `fix …`, a pin, the shadow, and the key.
- A second display ("… on display 2") was not tried.
- Not as specified, on purpose or for time: the keyword does not travel into the chip (it grows where the mark
  was); the pill returns to a closing row on `lead` and `trail`, not rigid; a pin has no Copy of its own.
- At the speed `adb shell input text` types, a letter right after a keyword's Space can be lost (seen once:
  "fix this" became Fix + "his"). `./bl debug keys` (40 ms a letter) never loses one. Worth a look in `Panel`'s
  `onChange` if a fast typist reports it.
- From the motion review, left: when a row's list opens, its first action is drawn over the row that is
  fading under it for about 50 ms, and the pill's lower edge is ahead of the uncovering edge for as long
  (the fix is rows riding the edge; it wants its own recording session). A strip whose actions change (an
  answer lands: Ask becomes Copy, Pin) is exchanged in a fade, not slot by slot. Kind labels blink along a
  held Down; app icons cut in.
- TalkBack was not run.
- Not built: the Gmail relay (Alex: not yet); extensions that answer while you type; "say it plainly" through
  the model (proposed for the release after).

## 2026-10-01 (late): 1.1.1 released to testers

Alex asked the Play session to cut 1.1.1 with the close animation: versionCode 3, tag `v1.1.1`, release notes in
English and German, closed testing on Play. 1.1 had been approved at 12:12 PDT (live to testers, with the new
title). Nothing else is in 1.1.1.

## 2026-10-01 (late): after 1.1

- **Closing is the opening backwards** (Alex's request; released in 1.1.1). `Panel`'s
  one `LaunchedEffect(leaving)` runs both ways; `Arrival.leaveMs` is how long the activity waits. Stepped
  through frame by frame on the Lenovo at fast and slow, with the key pressed again mid-close, and with the
  opening off. design-system.md §4 has the times.
- **"No key after installing"** (Alex's note) is not an APK matter: no app can give itself a key on a
  Googlebook; the user sets it in Keyboard shortcuts → App shortcuts, from Play as from an APK
  (device-findings.md). The HP kept Action + K across reinstalls because the system remembers the entry.
- **Research for what comes next:** `docs/research/next-features.md` (95 ideas by effort, value, permission
  tier and how Googlebook-only they are; the extensions contract in three layers; a device checklist; twelve
  questions for Alex). Not yet decided on.
- **Play title** is "Booklight: Delightful Launcher" (the Play session, on Alex's request).

## 2026-10-01 (night): 1.1 released to testers

**Released.** Tag `v1.1` at 7fadfbe (pushed by the Play session in ~/googlebook-tech on Alex's request to it).
The workflow was green on its first run: GitHub release "Booklight 1.1" with `Booklight.apk`,
`Booklight-1.1.apk` and `SHA256SUMS`; the bundle (version code 2) on Play closed testing, sent for review at
about 18:40 UTC. 1.0 stays live for testers until the review passes. Production is Alex's call.

**What Play said about the new permissions:** nothing. One warning (no debug symbols for native code), no
declaration form for `REQUEST_DELETE_PACKAGES`, `SET_ALARM` or `WRITE_SETTINGS`; App content "all caught up";
still offered to exactly the five Googlebooks. The listing is updated in English and now exists in German;
eight new screenshots, feature graphic and icon; googlebook.studio/privacy/booklight carries the 1.1 text
from `PRIVACY.md`; the data-safety form is as filed for 1.0.

**What 1.1 is.** Alex answered the 18 questions (spec §12) and asked for one big 1.1: the action row, scopes,
previews, controls, grids, the unfold arrival, the Booklight window, Jot, Dials, Recipes, Switches. 167 core
tests; release build and lint pass; cold start about 110 ms, warm about 30 ms on the Lenovo.

**Tested on the Lenovo Googlebook 15** (the HP was offline with its lid closed): every kind of row through
`./bl debug` (and every example line typed one character at a time); the opening, the edge light, the arming
and the first-run card frame by frame; event, new file, Ask Gemini, a timer, left half, right half and new
window for real; German and dark; text handed over by another app; real key presses on the release build.

**Reviewed.** A visual design review and a code review of the whole diff, both independent; every must-fix
and should-fix is in. Left on purpose: a monospace face for passwords, fade-outs instead of ellipses, the
site's own icon in a link's chip.

**Not tested, needs Alex's own clicks:** the app icon opening the Booklight window; the notes-folder picker;
the brightness switch; adding the widget and the tile; Uninstall's system dialog; a bound key on the Lenovo.
If the icon routing guesses wrong the panel opens, as in 1.0.

**Next**
1. When the review passes: confirm with the Play session that testers get 1.1.
2. Alex's first impressions of 1.1 on a device; fix what he finds as 1.1.1.
3. Then PLAN.md §4: 2.0 Extensions. The Gmail relay (2.1) is explicitly not started.

**Loose ends**
- The HP still has a throwaway spike build that draws a blurred band around the panel (it went offline
  before 1.0 could be put back). When it is online: `ANDROID_SERIAL=<the HP> ./bl install` with the
  1.1 build, or the 1.0 APK from the GitHub release.
- On the Lenovo: a test file `Documents/booklight-test.md`, a Text editor window, a Gemini window with a test
  question typed in (not sent) and two Chrome custom tabs are left from the tests; a 45-minute test timer was
  started and the Clock app then force-stopped.
- The spike branch `spike/unfold` (local) can go.

## 2026-10-01 (evening): 1.1 to 1.4 planned, waiting for Alex's answers

**Asked for:** build 1.1 Jot, 1.2 Dials, 1.3 Recipes and 2.2 Switches (not the Gmail relay); a way of acting
on a row (actions inside the row, Tab or arrows, typed verbs like "chrome uninstall"); keyword searches as a
chip after Tab; an opening animation with an off switch; a Booklight window (what it is, settings) behind the
app icon; Ask Gemini; fix the janky two-option switching on the first-run card. Plan first, then questions.

**Done, nothing built into the app yet:**
- `docs/design/ux-model.md` (the interaction model), `docs/design/design-system.md` (tokens, components,
  motion table; §4 carries a note on what the devices changed), `docs/research/permissions.md`.
- The page with a prototype and 18 questions: `docs/design/booklight-next.html`, published at
  https://claude.ai/artifact/Nc98FHZosXidEaHqMpU1me (publish the same file to update it).
- Device checks: `docs/research/device-findings.md`, the last two sections before the testing rules.
- A throwaway spike on the local branch `spike/unfold` (not pushed): the opening animations on a real device.
  Its debug build is on the Lenovo and opens with the unfold.

**Next:** Alex answers the questions (or "Default"). Then: write the spec in `docs/superpowers/specs/`, have
him read it, write the plan, build in the order on the page (foundation, Jot, Dials, Recipes, Switches).

**Loose ends**
- The HP went offline (lid closed) while it had a spike build installed that draws a blurred band around
  the panel. Put 1.0 back as soon as it is online: `./bl install` with the release APK from the GitHub release.
- Uninstall needs `REQUEST_DELETE_PACKAGES` (install-time, no prompt, Play asks nothing): Alex has to say yes.

## 2026-10-01 (later): 1.0 released

**Where it stands.** Booklight 1.0 (versionCode 1) is built, signed with the Booklight release key and
installed on the HP (release build; Action + K is bound to it). The Play app exists (id
4972005003444967962, Fika Labs) with app signing set to our key, the key backed up and closed testing
configured; the session in ~/googlebook-tech ("googlebookstudiowebsite") does the Play Console work and
sends it for review, with Alex's OK given there. `v1.0` is the tag that publishes the GitHub release and
puts the bundle on closed testing as a draft.

**Alex's answers and direction** are in `docs/PLAN.md` §11. In short: Booklight; Action + Alt + Space or
Action + K (the system needs the Action key, one shortcut per app); nothing shown when nothing is typed;
suggestions (off until turned on); English and German; flat glass, visibly see-through, white outline, no
3D highlights; motion throughout.

**What was built after 0.1:** `docs/superpowers/plans/2026-10-01-booklight-1.0.md`, and the spec's §8.
Reviews: a design pass and an independent visual review of device captures (glass, inks, grid, pill,
outline), and a code review (the privacy rule for suggestions, double-run and stale-result Enter, a
locale bug in the calculator, debug flags, file writes). All findings marked must or should were fixed.

**Released.** `v1.0` (commit 2a6c40d) went through the tag workflow: the GitHub release "Booklight 1.0" has
`Booklight.apk`, and Play's closed testing track has the bundle as a draft. The repo is public. On Play the app
requires `android.hardware.type.pc`, so it is offered to Googlebooks only (PLAN.md §11.11). The privacy page is
live at googlebook.studio/privacy/booklight.

**In review.** The Play session sent 1.0 for review on closed testing on 1 October (listing, forms, data
safety, content rating Everyone, testers googlebook-studio-testers, 178 countries). Tester link once approved:
https://play.google.com/apps/testing/io.github.kuscher.booklight. Production is Alex's call and not part of it.

**Next**
1. The next version must carry the in-app privacy policy link (Play asks for it): it is on `main` in
   Settings › What Booklight keeps, unreleased (CHANGELOG "Unreleased"). After the review: check with the Play
   session that Play offers the app to the five Googlebooks only.
2. **1.1 "Jot"** (PLAN.md §4): quick email (`mail …` → Gmail compose filled in), quick note, calendar
   event, timer, new file and folder. Alex asked for quick email and notes by name. Needs a spec and plan
   first; `docs/research/use-cases.md` has the intents and what to verify on the HP.
3. The German Play listing (`store-submission/listing/de-DE/`) once the app is in review.

**Not done, known**
- TalkBack was not run on the device (rows have roles, selection and labels in code).
- The Acer Googlebook and plain Android phones are untested; German was not checked on the device.
- No baseline profile (cold start is 121 ms without one).
- Decimal commas (`2,5*4`) aren't understood by the calculator; tiny float leftovers show as `5.55e-17`.
- The plan page's prototype is a browser copy; the app is the reference.
- BentoBar's unbind fix (its commit 94a952c) is unreleased and untested on a device.

**Measured on the HP (release build):** cold 121 ms, warm 61–81 ms, a search 6–15 ms. Details and every
device rule: `docs/research/device-findings.md`.

## 2026-10-01: 0.1, plan and design proposed

0.1 proved a zero-permission hotkey and overlay on the HP. The plan, the 1.0 design and an interactive
prototype were written and Alex answered the fourteen questions the same day.
