# Picking up

*Living status. Newest first.*

## 2026-10-01 (night): 1.1 built, not released

**Where it stands.** Alex answered the 18 questions (spec §12) and asked for one big 1.1. It is built on
`main` (versionName 1.1, versionCode 2): the action row, scopes, previews, controls, grids, the unfold
arrival, the Booklight window, Jot, Dials, Recipes, Switches. 167 core tests pass; the release build and
lint pass; cold start about 110 ms, warm about 30 ms on the Lenovo.

**Tested on the Lenovo Googlebook 15** (the HP was offline with its lid closed): every kind of row through
`./bl debug`; the opening, the edge light, the arming and the first-run card frame by frame; mail, event,
new file, Ask Gemini, a timer, left half, right half and new window for real; German and dark; text handed
over by another app; real key presses on the release build.

**Not tested, needs Alex's own clicks:** the app icon opening the Booklight window; the notes-folder picker;
the brightness switch; adding the widget and the tile; Uninstall's system dialog; a bound key on the Lenovo.

**Reviewed.** A visual design review (captures against the design system) and a code review of the whole
1.1 diff, both independent; every must-fix and should-fix is in (the selected row's ink, the pane's one rim,
icons, the grid on the mark column; a crash when a row lost its actions while being typed, held-Enter
repeats, the typed keyword of a scope, the clipboard's rows, migration, the window's editors). Left on
purpose: a monospace face for passwords (none on the device; the panel's own face with a slashed zero is
used), fade-outs instead of ellipses, the site's own icon in a link's chip.

**Release.** Store screenshots (`store-submission/graphics/screens`, eight, made from panel-only captures
on a drawn desk), the listing (en-US, de-DE), `PRIVACY.md` and the data-safety facts are final for 1.1. Alex
asked the Play session (~/googlebook-tech) to release Booklight to testers; it pushes the tag `v1.1` itself
once CI on main is green, then does the Console side (listing, screenshots, privacy page, forms, closed-testing
rollout, send for review). This session does not tag. Check `git tag` and the Actions tab for where that stands.

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
