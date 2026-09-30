#!/usr/bin/env bash
set -euo pipefail

evidence="${ELEMENTUM_RELEASE_EVIDENCE:-android/build/release-install-smoke}"
apk="${ELEMENTUM_RELEASE_APK:-android/build/final-artifacts/elementum-universal-release-ci-signed.apk}"
package_name="com.tomex.elementum"
activity="${package_name}/com.gdx.cellular.AndroidLauncher"

mkdir -p "$evidence"
trap 'adb logcat -d > "$evidence/logcat.txt" 2>/dev/null || true' EXIT

if ! adb logcat -c; then
  echo "Release smoke could not clear logcat; continuing with launch validation" >&2
  adb wait-for-device
fi

adb install -r "$apk"
adb shell settings put secure immersive_mode_confirmations confirmed || true
adb shell am force-stop "$package_name"

adb shell am start -W -n "$activity" > "$evidence/activity-start.txt"
cat "$evidence/activity-start.txt"

sleep 6

pid="$(adb shell pidof "$package_name" | tr -d '\r')"
if [ -z "$pid" ]; then
  echo "Elementum release process is not running after launch" >&2
  exit 1
fi
printf 'pid=%s\n' "$pid" > "$evidence/process.txt"

adb shell dumpsys activity activities > "$evidence/activity-state.txt" || true
if ! grep -Eq 'mResumedActivity.*com\.tomex\.elementum|ResumedActivity.*com\.tomex\.elementum' "$evidence/activity-state.txt"; then
  echo "Elementum release is running but not resumed in the foreground" >&2
  adb shell dumpsys activity top >> "$evidence/activity-state.txt" || true
  exit 1
fi

adb exec-out screencap -p > "$evidence/elementum-release-startup.png"
screenshot_bytes="$(stat -c%s "$evidence/elementum-release-startup.png")"
if [ "$screenshot_bytes" -le 5000 ]; then
  echo "Elementum release screenshot is unexpectedly small: ${screenshot_bytes} bytes" >&2
  exit 1
fi
printf 'screenshot_bytes=%s\n' "$screenshot_bytes" > "$evidence/screenshot-summary.txt"

adb logcat -d > "$evidence/logcat.txt"
if ! grep -Eq 'ElementumUI.*logical=360x[0-9]+' "$evidence/logcat.txt"; then
  echo "Elementum release did not report the expected 360-unit mobile UI width" >&2
  grep 'ElementumUI' "$evidence/logcat.txt" >&2 || true
  exit 1
fi
if grep -E 'FATAL EXCEPTION|Process: com\.tomex\.elementum.*has died|OutOfMemoryError|Fatal signal' "$evidence/logcat.txt"; then
  echo "Fatal Elementum release failure found in logcat" >&2
  exit 1
fi
if grep -E 'ANR in com\.tomex\.elementum|Input dispatching timed out.*com\.tomex\.elementum' "$evidence/logcat.txt"; then
  echo "Elementum release ANR found in logcat" >&2
  exit 1
fi

echo "Elementum CI-signed release install and foreground launch verified."
