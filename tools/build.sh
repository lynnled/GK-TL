#!/usr/bin/env bash
# Build GK-TL without Gradle (aapt2 + javac + d8 + apksigner).
# Tested on Windows 10 + Git Bash with JDK 17 and Android SDK build-tools 36.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-${LOCALAPPDATA}/Android/Sdk}}"
BT="${BUILD_TOOLS:-$SDK/build-tools/36.0.0}"
AJAR="${ANDROID_JAR:-$SDK/platforms/android-36/android.jar}"
OUT="$ROOT/out"
BUILD="$ROOT/build"

if [ -z "${JAVA_HOME:-}" ] && [ -d "/c/Program Files/Java/jdk-17" ]; then
  JAVA_HOME="/c/Program Files/Java/jdk-17"
fi
JAR="${JAVA_HOME:+$JAVA_HOME/bin/jar}"
JAR="${JAR:-jar}"
KEYTOOL="${JAVA_HOME:+$JAVA_HOME/bin/keytool}"
KEYTOOL="${KEYTOOL:-keytool}"
PY="${PYTHON:-python}"
command -v "$PY" >/dev/null 2>&1 || PY=python3

AAPT2="$BT/aapt2"
[ -f "$BT/aapt2.exe" ] && AAPT2="$BT/aapt2.exe"
D8="$BT/d8"
[ -f "$BT/d8.bat" ] && D8="$BT/d8.bat"
ZIPALIGN="$BT/zipalign"
[ -f "$BT/zipalign.exe" ] && ZIPALIGN="$BT/zipalign.exe"
APKSIGNER="$BT/apksigner"
[ -f "$BT/apksigner.bat" ] && APKSIGNER="$BT/apksigner.bat"

mkdir -p "$BUILD/classes" "$BUILD/dex" "$BUILD/gen" "$OUT"
rm -rf "$BUILD/classes"/* "$BUILD/dex"/* "$BUILD/gen"/* "$OUT"/*.apk

echo "[1/6] aapt2 compile"
"$AAPT2" compile --dir "$ROOT/app/res" -o "$BUILD/res.zip"

echo "[2/6] aapt2 link"
if [ -d "$ROOT/app/assets" ]; then
  "$AAPT2" link -o "$BUILD/base.apk" \
    --manifest "$ROOT/app/AndroidManifest.xml" \
    -I "$AJAR" \
    --min-sdk-version 26 --target-sdk-version 34 \
    --java "$BUILD/gen" \
    -A "$ROOT/app/assets" \
    "$BUILD/res.zip"
else
  "$AAPT2" link -o "$BUILD/base.apk" \
    --manifest "$ROOT/app/AndroidManifest.xml" \
    -I "$AJAR" \
    --min-sdk-version 26 --target-sdk-version 34 \
    --java "$BUILD/gen" \
    "$BUILD/res.zip"
fi

echo "[3/6] javac"
cd "$ROOT"
find app/src build/gen -name '*.java' > build/sources.txt
javac -encoding UTF-8 -source 11 -target 11 -Xlint:-options \
  -cp "$AJAR" -d build/classes \
  @build/sources.txt

echo "[4/6] d8"
rm -f "$BUILD/classes.jar"
"$JAR" cf "$BUILD/classes.jar" -C "$BUILD/classes" .
"$D8" --lib "$AJAR" --min-api 26 --output "$BUILD/dex" "$BUILD/classes.jar"

echo "[5/6] package + zipalign"
cp "$BUILD/base.apk" "$BUILD/unsigned.apk"
"$PY" - "$BUILD/unsigned.apk" "$BUILD/dex/classes.dex" <<'PYEOF'
import sys, zipfile
apk, dex = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(apk, 'a', zipfile.ZIP_DEFLATED) as z:
    z.write(dex, 'classes.dex')
PYEOF
rm -f "$BUILD/aligned.apk"
"$ZIPALIGN" -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"

echo "[6/6] sign"
KS="$ROOT/keystore/gk-tl.jks"
if [ ! -f "$KS" ]; then
  mkdir -p "$ROOT/keystore"
  "$KEYTOOL" -genkeypair -keystore "$KS" -alias gktl -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass gktl123 -keypass gktl123 \
    -dname "CN=GK-TL, OU=Android, O=lynnled, L=Local, ST=Local, C=CN" >/dev/null
fi
"$APKSIGNER" sign --ks "$KS" --ks-pass pass:gktl123 --key-pass pass:gktl123 \
  --out "$OUT/GK-TL.apk" "$BUILD/aligned.apk"
"$APKSIGNER" verify --print-certs "$OUT/GK-TL.apk" | head -3
ls -lh "$OUT/GK-TL.apk"
echo DONE
