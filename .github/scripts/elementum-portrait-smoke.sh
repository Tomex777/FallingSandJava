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
  add_event 1 330 1; add_event 1 325 1
  add_event 3 47 0; add_event 3 57 1; add_event 3 53 "$x0"; add_event 3 54 "$y0"; add_event 3 48 5
  add_event 0 0 0
  add_event 3 47 1; add_event 3 57 2; add_event 3 53 "$x1"; add_event 3 54 "$y0"; add_event 3 48 5
  add_event 0 0 0
  add_event 3 47 0; add_event 3 53 "$x0m"; add_event 3 54 "$ym"
  add_event 3 47 1; add_event 3 53 "$x1m"; add_event 3 54 "$ym"; add_event 0 0 0
  add_event 3 47 0; add_event 3 57 -1
  add_event 3 47 1; add_event 3 57 -1; add_event 1 330 0; add_event 1 325 0; add_event 0 0 0
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

tap 278 715
capture material-picker-solids
adb shell input swipe 190 610 190 430 500
sleep 1
capture material-picker-liquids
adb shell input swipe 190 610 190 430 500
sleep 1
capture material-picker-gases-energy
tap 278 715
tap 145 715
adb shell input swipe 190 230 190 340 500
sleep 1
capture petrol
tap 210 715
adb shell input swipe 190 230 190 340 500
sleep 2
capture interactions

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

tap 240 765
capture paused
tap 240 765
tap 315 765
capture cleared
tap 326 29
tap 220 250
capture load-dialog
dialog Load
adb shell input text elementum_qa
dialog Load
tap_ok Load
sleep 2
capture loaded
adb shell input keyevent KEYCODE_HOME
sleep 2
adb shell am start -W -n com.tomex.elementum/com.gdx.cellular.AndroidLauncher
sleep 3
capture resumed

adb logcat -d > "$evidence/logcat.txt"
test -n "$(adb shell pidof com.tomex.elementum)"
! grep -E 'FATAL EXCEPTION|Process: com\.tomex\.elementum.*has died' "$evidence/logcat.txt"
! grep -E 'ANR in com\.tomex\.elementum|Input dispatching timed out.*com\.tomex\.elementum' "$evidence/logcat.txt"
