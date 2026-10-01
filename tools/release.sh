#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Builds a signed release on this machine and checks it, for a look before tagging. Publishing is the
# tag workflow's job (docs/RELEASING.md).
#   tools/release.sh    build + verify into executables/release-<version>/
set -euo pipefail
cd "$(dirname "$(readlink -f "$0")")/.."
VERSION=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)
NOTES=docs/release-notes/$VERSION.md
[ -f "$NOTES" ] || { echo "missing $NOTES"; exit 1; }
[ -f ~/.config/booklight/keystore.jks ] || { echo "no release key in ~/.config/booklight"; exit 1; }
test "$(wc -m < store-submission/listing/en-US/release-notes.txt)" -le 500 || { echo "the Play release notes are over 500 characters"; exit 1; }
./gradlew :core:test :app:lintRelease :app:assembleRelease :app:bundleRelease --console=plain -q
OUT=executables/release-$VERSION
mkdir -p "$OUT"
cp app/build/outputs/apk/release/app-release.apk "$OUT/Booklight.apk"
cp "$OUT/Booklight.apk" "$OUT/Booklight-$VERSION.apk"
cp app/build/outputs/bundle/release/app-release.aab "$OUT/Booklight-$VERSION.aab"
SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
BT=$(ls -d "$SDK"/build-tools/*/ | sort -V | tail -1)
CERT=$("$BT/apksigner" verify --print-certs "$OUT/Booklight.apk" | sed -n 's/.*certificate SHA-256 digest: //p' | head -1)
EXPECT=6130f1f9115611565d2060084859022ce812a435e7bc627669d5c3668690a7f6
[ "$CERT" = "$EXPECT" ] || { echo "wrong signing certificate: $CERT"; exit 1; }
(cd "$OUT" && shasum -a 256 Booklight.apk "Booklight-$VERSION.apk" > SHA256SUMS)
ls -la "$OUT"; cat "$OUT/SHA256SUMS"
