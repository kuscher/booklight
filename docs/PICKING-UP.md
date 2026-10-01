# Picking up

*Living status. Newest first.*

## 2026-10-01 (night): 2.0

**What 2.0 is.** Alex ticked seven things in `research/next-features.md` and added the rest in his own words
(spec §1 and §2): other apps' commands as rows; a pinned window; the system's shortcuts as answers; more
places, nine icons and a list under the row; `?`; notes that grow; prompts; `s` and `k`; tips; a window with
a column of sections and a list of commands; a shadow around the panel; answers from the device's own model.
203 core tests; release build and lint pass; the release build opens in about 110 ms on the Lenovo.

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
- The calculator still has no decimal comma (the German examples use a point).
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
  before 1.0 could be put back). When it is online: `ANDROID_SERIAL=adb-HP-SERIAL-… ./bl install` with the
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
4972005003444967962, Fika Labs) with app signing set to our key, the backup done and closed testing
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
