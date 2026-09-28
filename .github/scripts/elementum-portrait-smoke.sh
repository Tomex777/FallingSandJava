#!/usr/bin/env bash
set -euo pipefail

evidence=android/build/runtime-smoke
mkdir -p "$evidence"
trap 'adb logcat -d > "$evidence/logcat.txt" || true' EXIT

capture() { adb exec-out screencap -p > "$evidence/elementum-$1.png"; }
tap() { adb shell input tap "$1" "$2"; sleep 1; }
multitouch_pan() {
  adb root >/dev/null
  adb wait-for-device
  adb shell getevent -lp > "$evidence/input-devices.txt"
  read -r device x_min x_max y_min y_max < <(python3 - "$evidence/input-devices.txt" <<'PY'
import re,sys
data=open(sys.argv[1]).read()
for block in re.split(r'add device \d+:\s*', data)[1:]:
    path=block.splitlines()[0].strip()
    x=re.search(r'ABS_MT_POSITION_X\s*:.*?min\s+(-?\d+),\s*max\s+(-?\d+)', block)
    y=re.search(r'ABS_MT_POSITION_Y\s*:.*?min\s+(-?\d+),\s*max\s+(-?\d+)', block)
    if x and y:
        print(path, x.group(1), x.group(2), y.group(1), y.group(2))
        break
else:
    raise SystemExit('No multi-touch input device found')
PY
  )
  raw_x() { python3 - "$1" "$x_min" "$x_max" <<'PY'
import sys
screen,low,high=map(int,sys.argv[1:])
print(round(low + screen * (high-low) / 359))
PY
  }
  raw_y() { python3 - "$1" "$y_min" "$y_max" <<'PY'
import sys
screen,low,high=map(int,sys.argv[1:])
print(round(low + screen * (high-low) / 799))
PY
  }
  local x0 y0 x1 y1 x0m x1m ym
  x0=$(raw_x 120); x1=$(raw_x 240); y0=$(raw_y 330)
  x0m=$(raw_x 115); x1m=$(raw_x 255); ym=$(raw_y 340)
  local events=""
  add_event() { events+="sendevent $device $1 $2 $3; "; }
  add_event 1 330 1
  add_event 3 47 0; add_event 3 57 1; add_event 3 53 "$x0"; add_event 3 54 "$y0"; add_event 3 48 5; add_event 3 58 512
  add_event 0 0 0
  add_event 3 47 1; add_event 3 57 2; add_event 3 53 "$x1"; add_event 3 54 "$y0"; add_event 3 48 5; add_event 3 58 512
  add_event 0 0 0
  add_event 3 47 0; add_event 3 53 "$x0m"; add_event 3 54 "$ym"
  add_event 3 47 1; add_event 3 53 "$x1m"; add_event 3 54 "$ym"; add_event 0 0 0
  add_event 3 47 0; add_event 3 57 -1
  add_event 3 47 1; add_event 3 57 -1; add_event 1 330 0; add_event 0 0 0
  adb shell "$events"
}
dialog() {
  adb shell uiautomator dump /sdcard/elementum-window.xml >/dev/null
  adb shell cat /sdcard/elementum-window.xml > "$evidence/$1-window.xml"
  grep -q "text=\"$1 Level\"" "$evidence/$1-window.xml"
}
tap_ok() {
  local bounds
  bounds=$(python3 - "$evidence/$1-window.xml" <<'PY'
import re,sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
node=next(n for n in root.iter('node') if n.get('text')=='OK')
x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
print((x1+x2)//2,(y1+y2)//2)
PY
)
  adb shell input tap $bounds
}

adb shell wm size 360x800
adb shell wm density 160
adb install -r android/build/outputs/apk/debug/android-debug.apk
adb logcat -c
adb shell settings put secure immersive_mode_confirmations confirmed || true
adb shell am force-stop com.tomex.elementum
adb shell am start -W -n com.tomex.elementum/com.gdx.cellular.AndroidLauncher
sleep 6
capture startup
for attempt in {1..10}; do
  if [ "$(stat -c%s "$evidence/elementum-startup.png")" -gt 5000 ]; then break; fi
  mv "$evidence/elementum-startup.png" "$evidence/elementum-startup-wait-$attempt.png"
  sleep 3
  capture startup
done
if [ "$(stat -c%s "$evidence/elementum-startup.png")" -le 5000 ]; then
  app_pid=$(adb shell pidof com.tomex.elementum || true)
  adb root >/dev/null || true
  adb wait-for-device
  if [ -n "$app_pid" ]; then adb shell kill -3 "$app_pid" || true; fi
  adb shell ls -l /data/anr > "$evidence/startup-anr-files.txt" 2>&1 || true
  adb shell cat /data/anr/traces.txt > "$evidence/startup-traces.txt" 2>&1 || true
  if [ -n "$app_pid" ]; then
    adb shell debuggerd -b "$app_pid" > "$evidence/startup-native-traces.txt" 2>&1 || true
    adb shell ps -T -p "$app_pid" -o PID,TID,STAT,NAME > "$evidence/startup-threads.txt" 2>&1 || true
  fi
  adb shell dumpsys gfxinfo com.tomex.elementum > "$evidence/startup-gfxinfo.txt" || true
  adb shell dumpsys activity top > "$evidence/startup-activity.txt" || true
  sleep 2
  exit 1
fi
python3 - "$evidence/elementum-startup.png" <<'PY'
import struct,sys
with open(sys.argv[1],'rb') as f: header=f.read(24)
w,h=struct.unpack('>II',header[16:24])
assert (w,h)==(360,800),f'Expected portrait 360x800, got {w}x{h}'
PY

tap 326 29
capture tools-menu
adb shell input keyevent KEYCODE_BACK
sleep 1
capture tools-back-dismissed
test -n "$(adb shell pidof com.tomex.elementum)"
tap 326 29
tap 100 250
capture tools-dismissed
tap 326 29
tap 220 170
capture tools-mouse-modes
tap 100 190
capture mouse-mode-heat
tap 326 29
tap 220 170
tap 100 130
capture mouse-mode-spawn

tap 310 715
capture material-picker-solids
adb shell input keyevent KEYCODE_BACK
sleep 1
capture material-picker-back-dismissed
test -n "$(adb shell pidof com.tomex.elementum)"
tap 310 715

# Copper is the second-row left solid after alphabetical material sorting.
# Exercise the new reversible thermal metal and a local water quench while the
# scene is paused so phase changes are observable at fixed coordinates.
tap 88 473
tap 240 765
tap 40 765
adb shell input swipe 70 630 150 630 400
sleep 1
capture copper-solid
tap 255 74
adb shell input swipe 70 630 150 630 400
sleep 1
capture copper-molten
tap 318 74
adb shell input swipe 70 630 150 630 400
sleep 1
capture copper-refrozen

# Copper is also thermally conductive, not only electrically conductive.
# Create a local cold spot while paused, then resume briefly so adjacent copper
# cells equalize that gradient through bounded nearest-neighbour transfer.
tap 318 74
tap 70 630
capture copper-thermal-gradient-before
tap 240 765
sleep 2
capture copper-thermal-gradient-after
tap 240 765

tap 255 74
# The preceding conduction pass can equalize the strip above a fresh Copper's
# 500-point melt resistance. Two bounded heat strokes guarantee this quench
# fixture is actually molten instead of depending on conduction timing.
adb shell input swipe 70 630 150 630 400
adb shell input swipe 70 630 150 630 400
sleep 1
capture copper-remelted
tap 90 715
adb shell input swipe 70 624 150 624 400
sleep 1
capture molten-copper-water-before
tap 240 765
sleep 2
capture molten-copper-water-after

# Copper also acts as a conductor. Set up a fresh stationary strip plus an
# adjacent lightning trace while paused, then resume to prove bounded transfer.
tap 240 765
tap 310 715
tap 88 473
adb shell input swipe 70 590 150 590 400
sleep 1
tap 210 715
adb shell input swipe 70 584 150 584 400
sleep 1
capture copper-lightning-before
tap 240 765
sleep 2
capture copper-lightning-after
tap 90 765
tap 40 715

# Re-open at the top and continue proving the full picker can scroll through
# liquids, gases and energy after the new material rows were added.
tap 310 715
adb shell input swipe 190 610 190 430 500
sleep 1
capture material-picker-liquids
adb shell input swipe 190 610 190 430 500
sleep 1
capture material-picker-gases-energy
tap 310 715
tap 145 715
adb shell input swipe 190 230 190 340 500
sleep 1
capture petrol
tap 210 715
adb shell input swipe 190 230 190 340 500
sleep 2
capture interactions

# Rapidly switch among the four touch-first materials and leave overlapping
# strokes in the same busy area.
tap 40 715
adb shell input swipe 80 250 130 300 300
tap 90 715
adb shell input swipe 120 250 170 300 300
tap 145 715
adb shell input swipe 160 250 210 300 300
tap 210 715
adb shell input swipe 200 250 245 300 300
sleep 2
capture rapid-material-switching

# Increase the brush materially rather than only exercising the +/- buttons.
# Use raw taps here so the stress case does not spend a second per increment.
for _ in {1..8}; do adb shell input tap 90 765; done
sleep 1
tap 40 715
adb shell input swipe 70 360 250 390 550
sleep 2
capture large-brush-stroke
for _ in {1..8}; do adb shell input tap 40 765; done
sleep 1

# Paused drawing should edit the world without advancing the simulation.
tap 240 765
capture pause-draw-before
tap 90 715
adb shell input swipe 110 410 220 430 450
sleep 1
capture pause-draw-after
if cmp -s "$evidence/elementum-pause-draw-before.png" "$evidence/elementum-pause-draw-after.png"; then
  echo "Drawing while paused produced no visible world edit" >&2
  exit 1
fi
tap 240 765
sleep 2
capture pause-draw-resumed

tap 40 715
adb shell input swipe 100 260 130 290 500
sleep 1
capture brush-circle-stroke

tap 160 765
capture brush-rect
adb shell input swipe 150 260 180 290 500
sleep 1
capture brush-rect-stroke
tap 160 765
capture brush-square
adb shell input swipe 200 260 230 290 500
sleep 1
capture brush-square-stroke
tap 160 765
capture brush-circle

# Freeze the scene and prove that a genuine two-pointer pan/pinch reaches the
# Android input stack. The paired captures retain visual proof for review.
tap 240 765
tap 160 330
capture navigation-before
multitouch_pan
sleep 1
capture navigation-after
if cmp -s "$evidence/elementum-navigation-before.png" "$evidence/elementum-navigation-after.png"; then
  echo "Two-pointer navigation produced no visible camera change" >&2
  exit 1
fi
printf '%s\n' "device=$device rangeX=$x_min..$x_max rangeY=$y_min..$y_max injected=two-pointer-pan-pinch" > "$evidence/navigation-input.txt"
tap 240 765

tap 326 29
tap 220 230
capture save-dialog
dialog Save
adb shell input text elementum_qa
dialog Save
tap_ok Save
sleep 1
adb shell run-as com.tomex.elementum ls -l files/save > "$evidence/saves.txt"
adb shell run-as com.tomex.elementum test -s files/save/elementum_qa.ser
adb shell run-as com.tomex.elementum cat files/save/elementum_qa.ser | head -c 3 > "$evidence/save-format.txt"
grep -q '^V3' "$evidence/save-format.txt"

# A truncated V2 file can still contain individually valid tokens. Keep the
# current world paused and prove dimension validation rejects it before clearAll.
tap 240 765
capture invalid-load-before
adb shell "run-as com.tomex.elementum sh -c 'printf \"V2\\nSAND\\n\" > files/save/elementum_corrupt.ser'"
sleep 1
tap 326 29
tap 220 250
capture corrupt-load-browser
sleep 1
tap 120 255
sleep 1
capture invalid-load-after
if ! cmp -s "$evidence/elementum-invalid-load-before.png" "$evidence/elementum-invalid-load-after.png"; then
  echo "Rejected V2 load changed the live paused sandbox" >&2
  exit 1
fi
adb shell run-as com.tomex.elementum rm files/save/elementum_corrupt.ser

# Saving the same name again must use the safe overwrite path rather than
# deleting the existing valid scene before the replacement is ready.
tap 326 29
tap 220 230
capture overwrite-save-dialog
dialog Save
adb shell input text elementum_qa
dialog Save
tap_ok Save
sleep 1
adb shell run-as com.tomex.elementum test -s files/save/elementum_qa.ser
if adb shell run-as com.tomex.elementum test -e files/save/elementum_qa.ser.tmp; then
  echo "Atomic save overwrite left a temporary file behind" >&2
  exit 1
fi

capture paused
tap 240 765
tap 315 765
capture cleared
tap 40 715
adb shell input swipe 90 300 160 340 400
sleep 1
capture after-clear-redraw
tap 326 29
tap 220 250
capture load-browser
sleep 1
adb shell input keyevent KEYCODE_BACK
sleep 1
capture load-browser-back-dismissed
test -n "$(adb shell pidof com.tomex.elementum)"
tap 326 29
tap 220 250
capture load-browser-reopened
sleep 1
# Android Load is a touch-first scene browser. The newest saved scene is the
# first large row in the centered dialog; no exact filename entry is required.
tap 120 255
sleep 2
capture loaded
adb shell input keyevent KEYCODE_HOME
sleep 2
adb shell am start -W -n com.tomex.elementum/com.gdx.cellular.AndroidLauncher
sleep 3
capture resumed

# Local erase is a first-class touch action. It sits immediately left of Tools,
# so the long-standing Tools target at x=326 remains unchanged.
tap 255 29
capture erase-selected
adb shell input swipe 160 300 205 330 450
sleep 1
capture erase-stroke

# Prove the thermal phase change while paused so the water cannot flow away
# between drawing and cooling. Thermal tools are on the second top-right row.
tap 90 715
tap 240 765
adb shell input swipe 125 410 205 410 400
sleep 1
capture cooling-water-before
tap 318 74
adb shell input swipe 125 410 205 410 400
sleep 1
capture cooling-water-after

# While still paused, heat the frozen strip once to melt Ice -> Water, then a
# second time to boil Water -> Steam. Cool that stationary steam back into
# Water to prove the reverse gas/liquid phase transition without movement
# hiding the result.
tap 255 74
adb shell input swipe 125 410 205 410 400
sleep 1
capture melting-ice-after
adb shell input swipe 125 410 205 410 400
sleep 1
capture evaporation-steam-after
tap 318 74
adb shell input swipe 125 410 205 410 400
sleep 1
capture condensation-water-after
tap 240 765

# Heat is paired with Cool. Ignite a Petrol strip, then a material shortcut
# must restore SPAWN before the session continues.
tap 145 715
adb shell input swipe 115 445 220 445 450
sleep 1
capture heat-petrol-before
tap 255 74
adb shell input swipe 115 445 220 445 450
sleep 2
capture heat-petrol-after
tap 40 715

# Build a stationary Petrol + Lightning contact while paused, then resume it.
# Brush size 3 is one matrix cell for a circle, so the strike sits directly
# above rather than replacing the Petrol cells it must ignite.
tap 240 765
tap 40 765
tap 145 715
adb shell input swipe 110 500 230 500 450
sleep 1
tap 210 715
adb shell input swipe 110 494 230 494 450
sleep 1
capture petrol-lightning-before
tap 240 765
sleep 2
capture petrol-lightning-after

# Water is a local conductor, not a world-wide electrical flood. With the same
# one-cell brush, place a water strip and a Lightning trace directly above it.
# The strike must cross a water contact using the existing child budget.
tap 240 765
tap 90 715
adb shell input swipe 110 545 230 545 450
sleep 1
tap 210 715
adb shell input swipe 110 539 230 539 450
sleep 1
capture water-lightning-before
tap 240 765
sleep 2
capture water-lightning-after

tap 90 765
tap 40 715

# Keep the busy world active long enough to expose delayed allocation,
# render-thread or reaction-traversal problems. Sample memory, graphics and
# worker-thread state during the soak.
for sample in 1 2 3 4; do
  sleep 5
  app_pid=$(adb shell pidof com.tomex.elementum)
  test -n "$app_pid"
  capture "soak-$sample"
  adb shell dumpsys meminfo com.tomex.elementum > "$evidence/meminfo-$sample.txt" || true
  adb shell dumpsys gfxinfo com.tomex.elementum > "$evidence/gfxinfo-$sample.txt" || true
  adb shell ps -T -p "$app_pid" -o PID,TID,STAT,NAME > "$evidence/threads-$sample.txt" || true
done
capture long-session-settled
test -n "$(adb shell pidof com.tomex.elementum)"

# Android's toybox ps NAME column reports the process name for every thread on
# API 36, so it cannot prove executor thread names. Use the app's one-shot
# ThreadFactory creation logs instead: a fixed pool must create at least one
# worker and must never exceed the hard six-worker cap during this soak.
adb logcat -d > "$evidence/logcat.txt"
sim_threads=$(grep -c 'ElementumWorker: created=ElementumSim-' "$evidence/logcat.txt" || true)
pool_configs=$(grep -c 'ElementumWorker: pool-size=' "$evidence/logcat.txt" || true)
if [ "$pool_configs" -lt 1 ] || [ "$sim_threads" -lt 1 ] || [ "$sim_threads" -gt 6 ]; then
  echo "Expected one bounded persistent simulation pool (1..6 workers), got configs=$pool_configs workers=$sim_threads" >&2
  grep 'ElementumWorker' "$evidence/logcat.txt" >&2 || true
  exit 1
fi

pss_first=$(awk '/TOTAL PSS:/ {print $3; exit}' "$evidence/meminfo-1.txt")
pss_last=$(awk '/TOTAL PSS:/ {print $3; exit}' "$evidence/meminfo-4.txt")
pss_growth=$((pss_last - pss_first))
if [ "$pss_growth" -gt 32768 ]; then
  echo "Elementum PSS grew by more than 32 MiB during the 20-second busy-world soak: ${pss_growth} KiB" >&2
  exit 1
fi
perf_samples=$(grep -c 'ElementumPerf: frames=' "$evidence/logcat.txt" || true)
if [ "$perf_samples" -lt 2 ]; then
  echo "Expected repeated in-engine performance samples, got $perf_samples" >&2
  grep 'ElementumPerf' "$evidence/logcat.txt" >&2 || true
  exit 1
fi
latest_perf=$(grep 'ElementumPerf: frames=' "$evidence/logcat.txt" | tail -1 | sed 's/^.*ElementumPerf: //')
printf '%s\n' \
  "persistent_sim_workers=$sim_threads" \
  "pool_configurations=$pool_configs" \
  "pss_first_kib=$pss_first" \
  "pss_last_kib=$pss_last" \
  "pss_growth_kib=$pss_growth" \
  "perf_samples=$perf_samples" \
  "latest_perf=$latest_perf" > "$evidence/performance-summary.txt"

test -n "$(adb shell pidof com.tomex.elementum)"
! grep -E 'FATAL EXCEPTION|Process: com\.tomex\.elementum.*has died|OutOfMemoryError|Fatal signal' "$evidence/logcat.txt"
! grep -E 'ANR in com\.tomex\.elementum|Input dispatching timed out.*com\.tomex\.elementum' "$evidence/logcat.txt"
grep -q 'ElementumInput.*material=WATER' "$evidence/logcat.txt"
grep -q 'ElementumInput.*material=PETROL' "$evidence/logcat.txt"
grep -q 'ElementumInput.*material=LIGHTNING' "$evidence/logcat.txt"
grep -q 'ElementumInput.*material=EMPTYCELL' "$evidence/logcat.txt"
grep -q 'ElementumInput.*mode=HEAT' "$evidence/logcat.txt"
grep -q 'ElementumInput.*mode=COOL' "$evidence/logcat.txt"
grep -q 'ElementumInput.*mode=SPAWN' "$evidence/logcat.txt"
grep -q 'ElementumInput.*material-picker=open' "$evidence/logcat.txt"
grep -q 'ElementumInput.*material-picker=closed' "$evidence/logcat.txt"
grep -q 'ElementumInput.*material-picker=back-closed' "$evidence/logcat.txt"
grep -q 'ElementumInput.*back-dismiss=creator-overlay' "$evidence/logcat.txt"
grep -q 'ElementumInput.*back-dismiss=mobile-overlay' "$evidence/logcat.txt"
grep -q 'ElementumSaveLoad.*saved=elementum_qa' "$evidence/logcat.txt"
save_count=$(grep -c 'ElementumSaveLoad.*saved=elementum_qa.*atomic=true' "$evidence/logcat.txt" || true)
if [ "$save_count" -lt 2 ]; then
  echo "Expected initial save plus atomic overwrite, got atomic save count=$save_count" >&2
  exit 1
fi
grep -q 'ElementumSaveLoad.*browser-scenes=1' "$evidence/logcat.txt"
grep -q 'ElementumSaveLoad.*browser-selected=elementum_qa' "$evidence/logcat.txt"
grep -q 'ElementumSaveLoad.*loaded=elementum_qa.*format=V3' "$evidence/logcat.txt"
grep -Eq 'ElementumSaveLoad.*restored-stateful=[1-9][0-9]*' "$evidence/logcat.txt"
grep -q 'ElementumSaveLoad.*load-invalid=elementum_corrupt' "$evidence/logcat.txt"
grep -q 'ElementumReaction.*water-to-ice' "$evidence/logcat.txt"
grep -q 'ElementumReaction.*ice-to-water' "$evidence/logcat.txt"
grep -q 'ElementumReaction.*water-to-steam' "$evidence/logcat.txt"
grep -q 'ElementumReaction.*steam-to-water' "$evidence/logcat.txt"
grep -q 'ElementumReaction.*copper-to-molten-copper' "$evidence/logcat.txt"
grep -q 'ElementumReaction.*molten-copper-to-copper' "$evidence/logcat.txt"
grep -q 'ElementumReaction.*water-molten-copper-steam' "$evidence/logcat.txt"
grep -q 'ElementumReaction.*copper-thermal-conduction' "$evidence/logcat.txt"
grep -q 'ElementumReaction.*lightning-conducted-copper' "$evidence/logcat.txt"
grep -q 'ElementumReaction.*lightning-conducted-water' "$evidence/logcat.txt"
grep -q 'ElementumReaction.*lightning-ignited-petrol' "$evidence/logcat.txt"
grep -q 'ElementumPerf.*avgFrameUs=' "$evidence/logcat.txt"

# With no overlay active, Android Back should finish the activity rather than
# being swallowed by libGDX. Then prove a true process restart can still reopen
# and load the durable V3 scene from app storage.
adb shell input keyevent KEYCODE_BACK
sleep 2
adb shell dumpsys activity activities > "$evidence/root-back-activity.txt"
if grep -q 'mResumedActivity.*com\.tomex\.elementum' "$evidence/root-back-activity.txt"; then
  echo "Root Back left Elementum resumed" >&2
  exit 1
fi
adb logcat -d > "$evidence/logcat-after-root-back.txt"
grep -q 'ElementumLifecycle.*back-finish' "$evidence/logcat-after-root-back.txt"

adb shell am force-stop com.tomex.elementum
sleep 1
adb shell am start -W -n com.tomex.elementum/com.gdx.cellular.AndroidLauncher
sleep 4
capture process-restart
test -n "$(adb shell pidof com.tomex.elementum)"
adb shell run-as com.tomex.elementum test -s files/save/elementum_qa.ser

tap 326 29
tap 220 250
capture process-restart-load-browser
sleep 1
tap 120 255
sleep 2
capture process-restart-loaded
adb logcat -d > "$evidence/logcat-after-process-restart.txt"
grep -q 'ElementumSaveLoad.*browser-selected=elementum_qa' "$evidence/logcat-after-process-restart.txt"
grep -q 'ElementumSaveLoad.*loaded=elementum_qa.*format=V3' "$evidence/logcat-after-process-restart.txt"
test -n "$(adb shell pidof com.tomex.elementum)"
