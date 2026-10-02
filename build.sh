#!/bin/bash
set -euo pipefail
: "${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK containing platform android-35 and build-tools 34.0.0}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
ROOT="$(cd "$(dirname "$0")" && pwd)"
BUILD="${BUILD_DIR:-$ROOT/../../work/build}"
KEY="${SIGNING_KEY:-$ROOT/../../work/app-atlas-test.jks}"
BT="$ANDROID_HOME/build-tools/34.0.0"
JAR="$ANDROID_HOME/platforms/android-35/android.jar"
mkdir -p "$BUILD/classes" "$BUILD/dex" "$BUILD/gen"
"$BT/aapt2" compile --dir "$ROOT/android/res" -o "$BUILD/resources.zip"
"$BT/aapt2" link -o "$BUILD/base.apk" -I "$JAR" --manifest "$ROOT/android/AndroidManifest.xml" -A "$ROOT/android/assets" --java "$BUILD/gen" "$BUILD/resources.zip"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 8 -target 8 -classpath "$JAR" -d "$BUILD/classes" "$ROOT/android/src/local/appatlas/"*.java
"$JAVA_HOME/bin/jar" cf "$BUILD/classes.jar" -C "$BUILD/classes" .
JAVA_HOME="$JAVA_HOME" "$BT/d8" --lib "$JAR" --min-api 30 --output "$BUILD/dex" "$BUILD/classes.jar"
cp "$BUILD/base.apk" "$BUILD/unsigned.apk"
(cd "$BUILD/dex" && zip -q "$BUILD/unsigned.apk" classes.dex)
"$BT/zipalign" -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
if [ ! -f "$KEY" ]; then
 "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$KEY" -storepass android -keypass android -alias app-atlas-test -dname "CN=App Atlas Local Test" -keyalg RSA -keysize 2048 -validity 10000 -noprompt
fi
JAVA_HOME="$JAVA_HOME" "$BT/apksigner" sign --ks "$KEY" --ks-pass pass:android --ks-key-alias app-atlas-test --out "$ROOT/app-atlas-v0.9.0.apk" "$BUILD/aligned.apk"
JAVA_HOME="$JAVA_HOME" "$BT/apksigner" verify --verbose "$ROOT/app-atlas-v0.9.0.apk"
"$BT/aapt2" dump permissions "$ROOT/app-atlas-v0.9.0.apk"
