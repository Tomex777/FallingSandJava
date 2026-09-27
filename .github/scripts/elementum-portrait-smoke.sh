#!/usr/bin/env bash
set -euo pipefail

evidence=android/build/runtime-smoke
mkdir -p "$evidence"
trap 'adb logcat -d > "$evidence/logcat.txt" || true' EXIT

capture() { adb exec-out screencap -p > "$evidence/elementum-$1.png"; }
tap() { adb shell input tap "$1" "$2"; sleep 1; }
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
test "$(stat -c%s "$evidence/elementum-startup.png")" -gt 5000
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

tap 160 765
capture paused
tap 160 765
tap 235 765
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
