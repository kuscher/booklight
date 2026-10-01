# Releasing Booklight

A release is a tag. Pushing `v<version>` makes GitHub Actions build, sign and publish it
(`.github/workflows/release.yml`, the same system as the other Googlebook apps; see
`~/googlebook-tech/scripts/play/release-template/README.md`):

1. `build` (environment `release`): checks the notes and the version, runs the core tests and lint, builds
   and signs the APK and the Play bundle, checks both against the pinned certificate, publishes the GitHub
   release with `Booklight.apk` (the README's download link), `Booklight-<version>.apk` and `SHA256SUMS`.
2. `play` (environment `play`): `tools/play-upload.mjs` puts the bundle on Play's closed testing track as
   a **draft**. Sending it for review stays a button in the Play Console.

**The key.** `~/.config/booklight/keystore.jks` + `keystore.pass` on the Mac (alias `booklight`, RSA 4096,
SHA-256 `61:30:F1:F9:11:56:11:56:5D:20:60:08:48:59:02:2C:E8:12:A4:35:E7:BC:62:76:69:D5:C3:66:86:90:A7:F6`),
backed up in a private folder, and in the repo's
`release` environment as `SIGNING_KEYSTORE_B64` / `SIGNING_KEYSTORE_PASS` (set with googlebook-tech's
`setup-secrets.sh booklight ~/.config/booklight/keystore.jks ~/.config/booklight/keystore.pass`). It is
Play's app signing key and upload key too. Never committed.

## Steps
1. On `main`: bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`.
2. Add `docs/release-notes/<version>.md`, the same under a new heading in `CHANGELOG.md`, and
   `store-submission/listing/en-US/release-notes.txt` (500 characters at most).
3. Commit, push, then `git tag v<version> && git push origin v<version>`.
4. In the Play Console: Closed testing › the draft › Next › Save › Send for review.

"Run workflow" on the Actions tab (or `gh workflow run release.yml --ref main`) is a dry run: the same
signed build and checks, nothing published.

`tools/release.sh` does the build and the checks on the Mac (into `executables/`), for looking at a
release before tagging.

## Before tagging: on the HP
- `./bl install app/build/outputs/apk/release/app-release.apk`, then the shortcut: opens, closes on the
  same keys, Esc, a click outside.
- Cold and warm start (`am start -W`): about 120 ms and 65 ms for 1.0.
- A sum, a settings page, a keyword search, Tab on an app, suggestions on and off.
- `DARK=true ./bl open stay` for the dark theme. German: Settings › Languages for the app.
