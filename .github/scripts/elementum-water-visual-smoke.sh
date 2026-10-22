#!/usr/bin/env bash
# Independent real-APK water visual acceptance. Do not substitute a mock image.
set -euo pipefail

evidence="${ELEMENTUM_WATER_EVIDENCE:-android/build/gpu-water-qa}"
app="com.tomex.elementum"
activity="$app/com.gdx.cellular.AndroidLauncher"
mkdir -p "$evidence"
printf 'source_sha=%s\nrun_id=%s\napi=%s\n'   "${GITHUB_SHA:-local-unknown}" "${GITHUB_RUN_ID:-local}"   "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" > "$evidence/provenance.txt"

on_exit() {
  local status=$?
  trap - EXIT
  adb logcat -d > "$evidence/logcat.txt" 2>/dev/null || true
  adb shell dumpsys gfxinfo "$app" framestats > "$evidence/gfxinfo-framestats.txt" 2>/dev/null || true
  adb shell dumpsys meminfo "$app" > "$evidence/meminfo.txt" 2>/dev/null || true
  adb shell wm size > "$evidence/final-screen-size.txt" 2>/dev/null || true
  adb shell wm size 360x800 >/dev/null 2>&1 || true
  if [ "$status" -ne 0 ]; then
    echo "GPU water visual QA FAILED (exit $status); inspecting $evidence/logcat.txt" >&2
    grep -E 'ElementumFluid|ElementumPerf|ElementumInput|FATAL EXCEPTION|ANR in com.tomex.elementum' "$evidence/logcat.txt" | tail -50 >&2 || true
  fi
  exit "$status"
}
trap on_exit EXIT

tap() { adb shell input tap "$1" "$2"; sleep 0.18; }
swipe() { adb shell input swipe "$1" "$2" "$3" "$4" 220; }
capture() {
  adb exec-out screencap -p > "$evidence/elementum-water-$1.png"
  # Never accept empty/headless/screensaver capture bytes as screenshots.
  local bytes
  bytes="$(stat -c%s "$evidence/elementum-water-$1.png")"
  if [ "$bytes" -le 5000 ]; then
    echo "Invalid $1 screenshot ($bytes bytes)" >&2
    return 1
  fi
}
verify_running() {
  test -n "$(adb shell pidof "$app" | tr -d '\r')"
}

adb shell wm size 360x800
adb shell wm density 160
adb install -r android/build/outputs/apk/debug/android-debug.apk
adb shell settings put secure immersive_mode_confirmations confirmed || true
adb logcat -c || true
adb shell am force-stop "$app" || true
# Keep this scenario isolated from app state left by earlier smoke tests.
adb shell pm clear "$app"
adb shell am start -W -n "$activity" > "$evidence/activity-launch.txt"
sleep 5
verify_running
capture startup-360x800
adb shell dumpsys activity activities > "$evidence/activity-foreground.txt"
grep -Eq 'mResumedActivity.*com\.tomex\.elementum|ResumedActivity.*com\.tomex\.elementum' "$evidence/activity-foreground.txt" || {
  echo "Water QA did not reach the Elementum foreground activity" >&2; exit 1;
}

# The two-row shelf is above the dock at 360x800. Pause the actual
# simulation, choose Water, enlarge brush, paint a staggered pool.
tap 239 765  # Pause
tap 89 724   # Water (lower material row, not the upper shelf)
for _ in 1 2 3 4 5 6 7 8; do tap 80 765; done
for y in 225 243 261 279 297 315 333 351; do
  swipe 72 "$y" 284 "$y"
done
sleep 6
capture paused-360x800
adb logcat -d > "$evidence/logcat-paused.txt"
if ! grep -q 'ElementumFluid.*smooth-water-shader=enabled' "$evidence/logcat-paused.txt"; then
  echo "GPU water shader did not initialize; screenshots are not GPU-water acceptance" >&2
  exit 1
fi
if ! grep -Eq 'ElementumFluid.*water-cells=[1-9][0-9]*' "$evidence/logcat-paused.txt"; then
  echo "No actual water cells reported; the touch fixture may have painted nothing" >&2
  exit 1
fi
if grep -E 'ElementumFluid.*(Shader fallback|disabled after error)' "$evidence/logcat-paused.txt"; then
  echo "Shader silently fell back; screenshot cannot be presented as GPU effect" >&2
  exit 1
fi

# Capture motion soon after unpausing (water otherwise falls out of frame).
tap 239 765
sleep 0.35
capture moving-early-360x800
sleep 0.45
capture moving-later-360x800
# A successful shader compilation or water-cell log does not guarantee a
# visible result. Exclude all material-chip colors and require actual
# blue water pixels in the sandbox on both paused and moving screenshots.
python3 .github/scripts/elementum-assert-water-pixels.py \
  "$evidence/elementum-water-paused-360x800.png" \
  "$evidence/elementum-water-moving-early-360x800.png"
verify_running

# Draw a second paused reference so compact-screen screenshots contain water
# even after the first fluid pool has already moved under gravity.
tap 239 765
for y in 200 222 244 266 288 310 332; do
  swipe 88 "$y" 268 "$y"
done
adb shell wm size 320x568
sleep 3
capture paused-320x568
# Compact dock: pause/resume stays the fourth bottom control.
tap 223 536
sleep 0.35
capture moving-320x568
verify_running

adb logcat -d > "$evidence/logcat-final.txt"
grep -Eq 'ElementumPerf.*avgFrameUs=' "$evidence/logcat-final.txt" || {
  echo "Performance telemetry missing from GPU-water QA" >&2; exit 1;
}
grep -Eq 'ElementumFluid.*water-cells=[1-9][0-9]*' "$evidence/logcat-final.txt" || {
  echo "No water was ever rendered" >&2; exit 1;
}
if grep -E 'ElementumFluid.*(Shader fallback|disabled after error)|FATAL EXCEPTION|ANR in com.tomex.elementum' "$evidence/logcat-final.txt"; then
  echo "Runtime crash, ANR, or water shader fallback" >&2
  exit 1
fi
grep 'ElementumPerf.*avgFrameUs=' "$evidence/logcat-final.txt" | tail -6 > "$evidence/frame-time-samples.txt"
echo "Real GPU-water screenshots and frame telemetry captured from $app"
