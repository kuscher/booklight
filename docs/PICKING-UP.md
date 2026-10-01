# Picking up

*Living status. Newest first.*

## 2026-10-01: 0.1, plan and design proposed

**Where it stands.** 0.1 runs on the HP Googlebook 14: the glass panel, apps, settings pages, sums,
web search, learning, and the Action + K shortcut (bound by hand in Keyboard shortcuts). The plan,
the 1.0 design and an interactive prototype are written. Nothing beyond 0.1 is approved.

**Waiting on Alex:** the fourteen questions in `docs/PLAN.md` §11 (name, key, empty state, Esc,
web, sums, extensions, advanced tiers, repo visibility, signing key, reach, languages, the test
shortcut on his HP, the BentoBar fix).

**Next, once answered:** write the 0.2 implementation plan (`docs/superpowers/plans/`), then build
0.2 "Feels right" (PLAN.md §10).

**Known gaps in 0.1 against the 1.0 design**
- No completion text in the field, no suggestion strip, no first-run row, no keyword searches.
- Selection highlight is per row (colour fade), not one gliding highlight.
- Only Open and App info as app actions.
- No preferences (search engine is fixed to Google), no icon beyond a placeholder.
- Debug build only: no R8, no baseline profile, no release key.
- Not checked: TalkBack, dark theme on the device, blur off, a work-profile app, the Acer.

**Measured on the HP (debug build):** cold 257–339 ms, warm 43–91 ms, a search 5–16 ms. See
`docs/research/device-findings.md`.
