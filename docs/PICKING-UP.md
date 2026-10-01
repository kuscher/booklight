# Picking up

*Living status. Newest first.*

## 2026-10-01 (later): 1.0 built, reviewed, tagged

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

**Next**
1. After the tag's workflow is green: tell the Play session (it files the listing and forms from
   `store-submission/` and sends for review), make the repo public (it then appears as a draft on
   googlebook.studio), and check the README's download link.
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
