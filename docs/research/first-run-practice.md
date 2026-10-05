# First run: what others do, and what the evidence says

*4 October 2026. Desk research for the first-run plan (`docs/design/first-run/`). Marks: **[V n]** on a page
fetched that day (n is the source number); **[Repo]** read in this repo; **[GK]** general knowledge, not verified
that day; **[Inf]** an inference for Booklight.*

**Where Booklight stands** [Repo]: a card under the empty field ("Give Booklight a key", "Open Keyboard
shortcuts") shows until a key has opened the panel (`settings.keySeen`). A suggestions card follows ("Turn on" /
"Not now"), then tips that type their own example. Detection, consent copy and self-typing exist; the sequence,
the three functions and the motion do not.

## Patterns worth using

1. **The hotkey step ends when the key is pressed.** Flow Launcher: "Flow Launcher starts with the hotkey below,
   go ahead and try it out now" [V1]. Superhuman "required users to hit 'enter' to get started"; clicked buttons
   "jiggle helplessly" [V8]. Fit: strong. The panel opening on its own key is proof and reward at once.
2. **Hand-off: say the path, open it, notice the return.** Alfred and Raycast can only list the System Settings
   path and what else may hold the key [V4][V3]. Ueli on Wayland has Booklight's exact limit and gives one
   sentence [V5]. Android's own keyboard setup wizard polls the setting every 200 ms and brings itself back at the
   right step [V6]. On macOS, PermissionFlow's panel "follows the System Settings window" [V7]. Design a "back,
   key not pressed yet" state and a "didn't work?" line. PowerToys shows "Shortcut conflict detection" in first
   run [V2]; Booklight cannot detect a conflict.
3. **Prime with one reason and one button named for what it opens.** A reason made users 12% more likely to grant
   (Tan et al., 15 apps); best against worst reason was an 81% lift [V15]. Apple: "Include only one button and
   make it clear that it opens the system alert" [V21]. The present card does this [Repo].
4. **Teach by doing, in the real panel.** Apple: "Teach through interactivity" [V19]. NN/g: deck-of-cards
   tutorials "didn't improve task performance" [V14]. Superhuman uses a synthetic inbox [V8]; Flow says "Try the
   queries below in Flow Launcher" [V1]; Things builds a tutorial project in the real app [V10]. No sandbox is
   needed if the three are reversible (a sum, opening an app) [Inf].
5. **Pick the three by what builds the habit, and measure.** Superhuman swapped j/k for "e" and "h": usage
   "increased by 50%", self-serve activation "from 40% to 50%" [V8]. Material: "up to three benefits" [V30].
   Alfred starts with app, file, web search [V4]. Raycast starts with search, the Action Panel and a quicklink,
   and leaves the rest for "the next few days" [V3].
6. **Five steps or fewer, skippable, replayable.** Vendor data (Chameleon 2025): "Tours beyond five steps lose
   attention from more than half of users"; self-triggered tours "double the engagement" [V35]. Flow: "You can
   skip this if you wish", and "Open Welcome Window" later [V1]. Things: Help, Create Tutorial Project [V10].
   Raycast: a Walkthrough command with tasks and progress [V3]. Replay as a typed command suits a launcher.
7. **The empty state carries the sequence and keeps its place.** NN/g: empty states "Provide direct pathways for
   getting started with key tasks" [V17]. It is Booklight's own rule ("one thing that may stand under the empty
   field") [Repo]. The panel closes on every Esc, so each step must resume [Inf].
8. **Defaults first, one screen of choices, consent apart.** Apple: "Postpone nonessential setup"; "Provide
   reasonable default settings" [V19]. Material Self-Select: one screen, "fewer than ten choices" [V30]. NN/g:
   "Don't show all permission requests at once" [V15].
9. **One surface that becomes the product.** Carbon: "shared elements across screens" make "a graceful
   transition" [V27]. Arc's "unboxing" has one through-line and a closing keepsake [V11]; take the through-line,
   not the colour blast. The field never moves, steps live where rows live, the last step folds to the bare
   field [Inf].
10. **Help lives where the user is.** Raycast: "@manual" in its AI chat [V3]. Android: the Keyboard Shortcuts
    Helper on Action + / [V34]. Apple on tips: one or two sentences, only for people who have not used the
    feature, about one a day [V22]. Booklight's tips already fit [Repo].

## Things to avoid

1. **A feature tour before use.** Tutorials "interrupt users, don't necessarily improve task performance, and are
   quickly forgotten"; "users frequently skip them" [V13]. NN/g gives no skip rate.
2. **Several things at once.** Short-term memory "fades in about 20 seconds" [V16].
3. **Bundled consent.** A disclosure "Cannot be included with other disclosures unrelated to personal and
   sensitive user data collection" [V31].
4. **Look-and-feel choices in first run.** People cannot judge before use [V14, paraphrased].
5. **Expression that breaks the familiar pattern.** Where it did, "usability scores suffered"; "removing text
   labels" hurt too [V25]. Key caps keep their labels.
6. **Motion that makes a key wait.** "Let people cancel motion" [V20].
7. **Steps that advance or expire by themselves,** above all consent [V31].

## What this means for motion

- **Springs.** Material: spatial springs (position, size) may overshoot; effects springs (colour, opacity)
  "should not be overshot" [V24]. Values from the AndroidX source [V23], stiffness as fast / default / slow:

  | Scheme | Material's use for it | Spatial damping | Spatial stiffness |
  | --- | --- | --- | --- |
  | Standard | "utilitarian UI elements and recurring interactions" | 0.9 | 1400 / 700 / 300 |
  | Expressive | "prominent UI elements and hero interactions" | 0.6 / 0.8 / 0.8 | 800 / 380 / 200 |

  Effects springs are the same in both: damping 1.0, stiffness 3800 / 1600 / 800 [V23].
- **Apple on bounce.** 0 by default, about 0.15 for some life, caution above 0.4; a retargeted spring keeps its
  velocity [V26].
- **For Booklight** [Inf]: expressive only for what moves inside the glass (a key cap, a check). The edges stay on
  `open` = spring(0.9, 800), which may not pass the final edge [Repo].
- **Durations.** Feedback about 100 ms; large changes 200 to 300 ms; 400 ms is "very slow" [V18]. Material:
  container transform 300 in, 250 out; fade 150 in, 75 out [V24].
- **Stagger.** 20 ms an item, the whole "within 500 ms" (Carbon) [V27]. Booklight's 22 ms cascade agrees [Repo].
- **Demos.** No research was found on self-typing text or pressing key caps [GK]. WCAG 2.2.2: motion that starts
  by itself and runs over five seconds needs a stop [V29]. So keep each demo under 5 s, never looping, ended in
  the same frame by any key ("typing owns the frame" [Repo]).
- **Reduced motion.** Motion is never "the only way to communicate important information" [V20]; WCAG 2.3.3
  [V29]. Every step must read as a still.
- **Where to spend it.** A command menu's animation is "cognitive burden after seeing the same animation for the
  hundredth time" [V28]. First run is seen once; the daily opening stays as built [Inf].
- **Sequence or slides.** No study comparing them was found; pattern 9 is the argument [Inf].

## Consent wording (search suggestions)

Play's policy applies where sharing "may not be within the reasonable expectation of the user" [V31]. Whether
typed launcher text counts is not spelled out, so treat it as yes [Inf].

- **Disclosure.** In the app, "in the normal usage of the app", not in settings or only the privacy policy. It
  must "describe the data" and "explain how the data will be used and/or shared", and not be mixed with other
  notices [V31].
- **Consent.** An "affirmative user action". "Navigation away" (Esc, focus lost) is not consent. No
  "auto-dismissing or expiring messages". Granted before anything is sent [V31].
- **Play's format.** "[This app] collects/transmits/syncs/stores [type of data] to enable ["feature"], [in what
  scenario]." [V31]
- **Words.** "Agree" rather than "Allow access" or "Got it"; a way to decline and grant later; name the third
  party [V32].
- **Data safety form.** It must match the step: "In-app search history" is a listed type [V33].

Today's card already names data, recipient, purpose and exceptions [Repo]. To add [Inf]: when it happens (as you
type), and that the engine sees the IP address (`PRIVACY.md` says so).

## Not verified

- Raycast's in-app first-run screens (a gallery confirms 12 screens, with the text behind a sign-up) [V3].
- Linear: a command-menu step before the workspace and a checklist after, per a teardown [V9].
- Spotlight: Apple's guide documents its keys and no first-run flow [V12].
- No usable source: Notion Calendar, CleanShot X, Rewind, Screen Studio, Albert, the ChromeOS launcher.
- Material's motion pages render only with JavaScript, so the numbers come from AndroidX source.

## Sources (fetched 4 October 2026)

1. Flow Launcher wizard strings: https://raw.githubusercontent.com/Flow-Launcher/Flow.Launcher/dev/Flow.Launcher/Languages/en.xaml
2. PowerToys first-run strings: https://raw.githubusercontent.com/microsoft/PowerToys/main/src/settings-ui/Settings.UI/Strings/en-us/Resources.resw · https://learn.microsoft.com/en-us/windows/powertoys/run
3. Raycast: https://www.raycast.com/changelog/1-26-0 · https://manual.raycast.com/quickstart.md · https://manual.raycast.com/v1/hotkey · https://lazyweb.com/flow/raycast/onboarding
4. Alfred: https://www.alfredapp.com/help/getting-started/first-5-minutes/ · https://www.alfredapp.com/help/troubleshooting/cmd-space/
5. Ueli README: https://raw.githubusercontent.com/oliverschwendener/ueli/main/README.md
6. AOSP keyboard `SetupWizardActivity.java`: https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/refs/heads/main/java/src/com/android/inputmethod/latin/setup/SetupWizardActivity.java
7. PermissionFlow: https://github.com/jaywcjlove/PermissionFlow
8. Superhuman, First Round Review: https://review.firstround.com/superhuman-onboarding-playbook · https://www.gainsight.com/blog/5-lessons-i-learned-from-superhumans-onboarding/
9. Linear teardown: https://supademo.com/user-flow-examples/linear
10. Things: https://culturedcode.com/things/support/articles/2803553/
11. Arc: https://www.inverse.com/input/design/the-browser-company-arc-design-interview
12. Apple, Spotlight shortcuts: https://support.apple.com/guide/mac-help/mh26783
13. NN/g, tutorials: https://www.nngroup.com/articles/onboarding-tutorials/
14. NN/g, mobile-app onboarding: https://www.nngroup.com/articles/mobile-app-onboarding/
15. NN/g, permission requests: https://www.nngroup.com/articles/permission-requests/
16. NN/g, instructional overlays: https://www.nngroup.com/articles/mobile-instructional-overlay/
17. NN/g, empty states: https://www.nngroup.com/articles/empty-state-interface-design/
18. NN/g, animation: https://www.nngroup.com/articles/animation-duration/ · https://www.nngroup.com/articles/animation-purpose-ux/
19. Apple HIG, Onboarding: https://developer.apple.com/design/human-interface-guidelines/onboarding
20. Apple HIG, Motion: https://developer.apple.com/design/human-interface-guidelines/motion
21. Apple HIG, Privacy: https://developer.apple.com/design/human-interface-guidelines/privacy
22. Apple HIG, Offering help: https://developer.apple.com/design/human-interface-guidelines/offering-help
23. AndroidX `MotionScheme.kt` and its tokens: https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/MotionScheme.kt
24. Material Components for Android, Motion: https://raw.githubusercontent.com/material-components/material-components-android/master/docs/theming/Motion.md
25. Google Design, M3 Expressive research: https://design.google/library/expressive-material-design-google-research
26. Apple, "Animate with springs" (WWDC23): https://developer.apple.com/videos/play/wwdc2023/10158/
27. Carbon, choreography: https://carbondesignsystem.com/elements/motion/choreography/
28. Emil Kowalski: https://emilkowal.ski/ui/great-animations · Rauno Freiberg: https://rauno.me/craft/interaction-design
29. WCAG 2.2: https://www.w3.org/WAI/WCAG22/Understanding/pause-stop-hide.html · https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions.html
30. Material 1, Onboarding: https://m1.material.io/growth-communications/onboarding.html
31. Google Play, User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
32. Google Play, prominent disclosure best practices: https://support.google.com/googleplay/android-developer/answer/11150561
33. Google Play, Data safety form: https://support.google.com/googleplay/android-developer/answer/10787469
34. Android, Keyboard Shortcuts Helper: https://developer.android.com/develop/ui/compose/touch-input/keyboard-input/keyboard-shortcuts-helper
35. Chameleon Benchmark Report 2025 (vendor data): https://www.chameleon.io/benchmark-report
