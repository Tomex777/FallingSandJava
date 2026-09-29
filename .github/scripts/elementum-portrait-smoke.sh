#!/usr/bin/env bash
set -euo pipefail

evidence=android/build/runtime-smoke
mkdir -p "$evidence"
trap 'adb logcat -d > "$evidence/logcat.txt" || true' EXIT

DESIGN_W=360
DESIGN_H=800
SCREEN_W=$DESIGN_W
SCREEN_H=$DESIGN_H

capture() { adb exec-out screencap -p > "$evidence/elementum-$1.png"; }

# The smoke workflow applies a 360x800 logical wm override before launch.
# Android 8 may still return a 1080x1920 physical framebuffer from screencap,
# but input injection remains in the logical display coordinate space. Keep
# touch coordinates logical so API 26 and API 36 exercise the same controls.
tap() {
  adb shell input tap "$1" "$2"
  sleep 1
}
tap_fast() {
  adb shell input tap "$1" "$2"
}
swipe() {
  adb shell input swipe "$1" "$2" "$3" "$4" "$5"
}

assert_foreground() {
  local label="$1"
  local state="$evidence/activity-$label.txt"
  adb shell dumpsys activity activities > "$state" || true
  if ! grep -Eq 'mResumedActivity.*com\.tomex\.elementum|ResumedActivity.*com\.tomex\.elementum' "$state"; then
    echo "Elementum lost foreground during $label" >&2
    adb shell dumpsys activity top >> "$state" || true
    return 1
  fi
}


dismiss_system_fullscreen_confirmation() {
  # Android SystemUI can intermittently show its first-use immersive-mode
  # confirmation even after immersive_mode_confirmations is pre-set. That
  # overlay consumes app taps/Back presses, so dismiss the system-owned prompt
  # before collecting product evidence. Do not change Elementum to satisfy it.
  local xml="$evidence/system-ui-fullscreen.xml"
  local dismiss_x dismiss_y
  for attempt in 1 2 3; do
    adb shell uiautomator dump /sdcard/elementum-window.xml >/dev/null 2>&1 || break
    adb shell cat /sdcard/elementum-window.xml > "$xml" 2>/dev/null || break
    if read -r dismiss_x dismiss_y < <(python3 - "$xml" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
for node in root.iter("node"):
    text = (node.attrib.get("text") or "").strip().lower()
    desc = (node.attrib.get("content-desc") or "").strip().lower()
    if text not in {"got it", "ok"} and desc not in {"got it", "ok"}:
        continue
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
    if not match:
        continue
    left, top, right, bottom = map(int, match.groups())
    print((left + right) // 2, (top + bottom) // 2)
    raise SystemExit(0)
raise SystemExit(1)
PY
    ); then
      adb shell input tap "$dismiss_x" "$dismiss_y"
      sleep 1
    else
      break
    fi
  done
}

# Compare only the sandbox viewport, not transient button pressed/checked state.
# Android screencap PNG output is decoded with Python's standard library so CI
# does not depend on Pillow/ImageMagick being preinstalled on the runner.
same_canvas_pixels() {
  python3 - "$1" "$2" <<'PY'
import struct
import sys
import zlib

PNG = b"\x89PNG\r\n\x1a\n"

def paeth(a, b, c):
    p = a + b - c
    pa = abs(p - a)
    pb = abs(p - b)
    pc = abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c

def decode(path):
    data = open(path, "rb").read()
    if not data.startswith(PNG):
        raise SystemExit(f"Not a PNG: {path}")
    pos = len(PNG)
    width = height = bit_depth = color_type = interlace = None
    chunks = []
    while pos + 12 <= len(data):
        size = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        payload = data[pos + 8:pos + 8 + size]
        pos += 12 + size
        if kind == b"IHDR":
            width, height, bit_depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", payload)
        elif kind == b"IDAT":
            chunks.append(payload)
        elif kind == b"IEND":
            break
    if bit_depth != 8 or color_type not in (2, 6) or interlace != 0:
        raise SystemExit(f"Unsupported screencap PNG layout in {path}: bit={bit_depth} color={color_type} interlace={interlace}")
    bpp = 3 if color_type == 2 else 4
    stride = width * bpp
    raw = zlib.decompress(b"".join(chunks))
    expected = height * (stride + 1)
    if len(raw) != expected:
        raise SystemExit(f"Unexpected PNG payload size in {path}: {len(raw)} != {expected}")
    rows = []
    offset = 0
    previous = bytearray(stride)
    for _ in range(height):
        filter_type = raw[offset]
        offset += 1
        row = bytearray(raw[offset:offset + stride])
        offset += stride
        for i in range(stride):
            left = row[i - bpp] if i >= bpp else 0
            up = previous[i]
            upper_left = previous[i - bpp] if i >= bpp else 0
            if filter_type == 1:
                row[i] = (row[i] + left) & 0xff
            elif filter_type == 2:
                row[i] = (row[i] + up) & 0xff
            elif filter_type == 3:
                row[i] = (row[i] + ((left + up) // 2)) & 0xff
            elif filter_type == 4:
                row[i] = (row[i] + paeth(left, up, upper_left)) & 0xff
            elif filter_type != 0:
                raise SystemExit(f"Unsupported PNG filter {filter_type} in {path}")
        rows.append(bytes(row))
        previous = row
    return width, height, bpp, rows

left = decode(sys.argv[1])
right = decode(sys.argv[2])
if left[:3] != right[:3]:
    raise SystemExit(1)

width, height, bpp = left[:3]
top = min(height - 1, max(1, round(height * 40 / 800)))
bottom = min(height, max(top + 1, round(height * 700 / 800)))
for y in range(top, bottom):
    if left[3][y][:width * bpp] != right[3][y][:width * bpp]:
        raise SystemExit(1)
PY
}
multitouch_pan() {
  # sendevent needs a rooted adbd on the emulator. adb root restarts adbd and
  # can transiently return "closed" even though the emulator remains healthy.
  # Treat that transport restart as harness setup, then require a working shell
  # before touching the app or raw input device.
  adb root >/dev/null 2>&1 || true
  adb wait-for-device
  local adb_ready=false
  for _ in {1..20}; do
    if adb shell true >/dev/null 2>&1; then
      adb_ready=true
      break
    fi
    sleep 1
  done
  if [ "$adb_ready" != true ]; then
    echo "ADB shell did not recover after enabling emulator root" >&2
    return 1
  fi
  dismiss_system_fullscreen_confirmation
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
# Portrait coordinates for the compact mobile dock/sheets. These helpers keep
# the runtime proof readable while the simulation itself remains untouched.
open_more() { tap 335 765; }
tool_draw() { open_more; tap 48 490; }
tool_heat() { open_more; tap 114 490; }
tool_cool() { open_more; tap 180 490; }
tool_erase() { open_more; tap 246 490; }
open_save_sheet() { open_more; tap 48 580; }
open_load_sheet() { open_more; tap 114 580; }
open_clear_sheet() { open_more; tap 180 580; }
open_help_sheet() { open_more; tap 312 580; }

adb shell wm size 360x800
adb shell wm density 160
adb install -r android/build/outputs/apk/debug/android-debug.apk
# Clearing logcat is useful for evidence isolation but is not a product check.
# Older/slow emulator images can reject the clear even while adb/app execution
# is healthy, so recover and continue into the actual Elementum validation.
if ! adb logcat -c; then
  echo "CI emulator could not clear logcat; continuing with tagged product assertions" >&2
  adb wait-for-device
fi
adb shell settings put secure immersive_mode_confirmations confirmed || true
adb shell am force-stop com.tomex.elementum
adb shell am start -W -n com.tomex.elementum/com.gdx.cellular.AndroidLauncher
sleep 6
dismiss_system_fullscreen_confirmation
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
read -r SCREEN_W SCREEN_H < <(python3 - "$evidence/elementum-startup.png" <<'PY'
import struct,sys
with open(sys.argv[1],'rb') as f:
    header=f.read(24)
w,h=struct.unpack('>II',header[16:24])
assert h > w, f'Expected portrait output, got {w}x{h}'
assert w >= 320 and h >= 568, f'Unexpectedly small Android surface: {w}x{h}'
print(w, h)
PY
)
{
  printf 'screencap=%sx%s design=%sx%s\n' "$SCREEN_W" "$SCREEN_H" "$DESIGN_W" "$DESIGN_H"
  adb shell wm size || true
} > "$evidence/screen-size.txt"

open_more
capture more-sheet
adb shell input keyevent KEYCODE_BACK
sleep 1
capture more-back-dismissed
assert_foreground more-back-dismiss
test -n "$(adb shell pidof com.tomex.elementum)"

# Help is now a compact in-game bottom sheet instead of a desktop dialog.
open_help_sheet
sleep 1
capture help-sheet
adb shell input keyevent KEYCODE_BACK
sleep 1
capture help-back-dismissed
assert_foreground help-back-dismiss
test -n "$(adb shell pidof com.tomex.elementum)"

tool_heat
capture tool-heat
tool_draw
capture tool-draw

tap 310 715
capture material-picker-solids
adb shell input keyevent KEYCODE_BACK
sleep 1
capture material-picker-back-dismissed
assert_foreground material-picker-back-dismiss
test -n "$(adb shell pidof com.tomex.elementum)"
tap 310 715

# Copper is available directly from the compact Solids grid. Exercise the
# reversible thermal metal while paused so phase changes remain observable at
# fixed coordinates.
tap 210 545
tap 240 765
tap 40 765
swipe 70 630 150 630 400
sleep 1
capture copper-solid
tool_heat
swipe 70 630 150 630 400
sleep 1
capture copper-molten
tool_cool
swipe 70 630 150 630 400
sleep 1
capture copper-refrozen

# Copper is also thermally conductive, not only electrically conductive.
# Create a local cold spot while paused, then resume briefly so adjacent copper
# cells equalize that gradient through bounded nearest-neighbour transfer.
tool_cool
tap 70 630
capture copper-thermal-gradient-before
tap 240 765
sleep 2
capture copper-thermal-gradient-after
tap 240 765

tool_heat
# The preceding conduction pass can equalize the strip above a fresh Copper's
# 500-point melt resistance. Two bounded heat strokes guarantee this quench
# fixture is actually molten instead of depending on conduction timing.
swipe 70 630 150 630 400
swipe 70 630 150 630 400
sleep 1
capture copper-remelted
tap 90 715
swipe 70 624 150 624 400
sleep 1
capture molten-copper-water-before
tap 240 765
sleep 2
capture molten-copper-water-after

# Copper also acts as a conductor. Set up a fresh stationary strip plus an
# adjacent lightning trace while paused, then resume to prove bounded transfer.
tap 240 765
tap 310 715
tap 210 545
swipe 70 590 150 590 400
sleep 1
tap 210 715
swipe 70 584 150 584 400
sleep 1
capture copper-lightning-before
tap 240 765
sleep 2
capture copper-lightning-after
tap 90 765
tap 40 715

# Re-open and prove the compact category tabs expose every material family
# without forcing desktop-style nested menus or a long scrolling sheet.
tap 310 715
tap 130 500
capture material-picker-liquids
tap 210 500
capture material-picker-gases
tap 290 500
capture material-picker-energy
tap 310 715
tap 145 715
swipe 190 230 190 340 500
sleep 1
capture petrol
tap 210 715
swipe 190 230 190 340 500
sleep 2
capture interactions

# Rapidly switch among the four touch-first materials and leave overlapping
# strokes in the same busy area.
tap 40 715
swipe 80 250 130 300 300
tap 90 715
swipe 120 250 170 300 300
tap 145 715
swipe 160 250 210 300 300
tap 210 715
swipe 200 250 245 300 300
sleep 2
capture rapid-material-switching

# Increase the brush materially rather than only exercising the +/- buttons.
# Use raw taps here so the stress case does not spend a second per increment.
for _ in {1..8}; do tap_fast 90 765; done
sleep 1
tap 40 715
swipe 70 360 250 390 550
sleep 2
capture large-brush-stroke
for _ in {1..8}; do tap_fast 40 765; done
sleep 1

# Paused drawing should edit the world without advancing the simulation.
tap 240 765
capture pause-draw-before
tap 90 715
swipe 110 410 220 430 450
sleep 1
capture pause-draw-after
if same_canvas_pixels "$evidence/elementum-pause-draw-before.png" "$evidence/elementum-pause-draw-after.png"; then
  echo "Drawing while paused produced no visible world edit" >&2
  exit 1
fi
tap 240 765
sleep 2
capture pause-draw-resumed

tap 40 715
swipe 100 260 130 290 500
sleep 1
capture brush-circle-stroke

tap 160 765
capture brush-rect
swipe 150 260 180 290 500
sleep 1
capture brush-rect-stroke
tap 160 765
capture brush-square
swipe 200 260 230 290 500
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
if same_canvas_pixels "$evidence/elementum-navigation-before.png" "$evidence/elementum-navigation-after.png"; then
  echo "Two-pointer navigation produced no visible camera change" >&2
  exit 1
fi
printf '%s\n' "device=$device rangeX=$x_min..$x_max rangeY=$y_min..$y_max injected=two-pointer-pan-pinch" > "$evidence/navigation-input.txt"
tap 240 765

open_save_sheet
capture save-sheet
tap 210 401
sleep 1
adb shell run-as com.tomex.elementum ls -l files/save > "$evidence/saves.txt"
adb shell "run-as com.tomex.elementum sh -c '[ -s files/save/scene_1.ser ]'"
adb shell run-as com.tomex.elementum cat files/save/scene_1.ser | head -c 3 > "$evidence/save-format.txt"
grep -q '^V3' "$evidence/save-format.txt"

# A truncated V2 file can still contain individually valid tokens. Keep the
# current world paused and prove dimension validation rejects it before clearAll.
tap 240 765
capture invalid-load-before
adb shell "run-as com.tomex.elementum sh -c 'printf \"V2\\nSAND\\n\" > files/save/elementum_corrupt.ser'"
sleep 1
open_load_sheet
capture corrupt-load-browser
sleep 1
# Fixed slots stay first; the injected legacy/corrupt save is the fifth row.
tap 210 585
sleep 1
capture invalid-load-after
if ! same_canvas_pixels "$evidence/elementum-invalid-load-before.png" "$evidence/elementum-invalid-load-after.png"; then
  echo "Rejected V2 load changed the live paused sandbox" >&2
  exit 1
fi
adb shell run-as com.tomex.elementum rm files/save/elementum_corrupt.ser

# Saving the same name again must use the safe overwrite path rather than
# deleting the existing valid scene before the replacement is ready.
open_save_sheet
capture overwrite-save-sheet
tap 210 401
sleep 1
adb shell "run-as com.tomex.elementum sh -c '[ -s files/save/scene_1.ser ]'"
if adb shell "run-as com.tomex.elementum sh -c '[ -e files/save/scene_1.ser.tmp ]'"; then
  echo "Atomic save overwrite left a temporary file behind" >&2
  exit 1
fi

capture paused

# Clear is deliberately destructive on mobile. First cancel it while the
# simulation is paused and prove the live sandbox is bit-for-bit unchanged.
open_clear_sheet
sleep 1
capture clear-confirm-cancel
tap 112 640
sleep 1
capture clear-cancelled
if ! same_canvas_pixels "$evidence/elementum-paused.png" "$evidence/elementum-clear-cancelled.png"; then
  echo "Cancelling Clear changed the paused sandbox" >&2
  exit 1
fi

# Then resume and prove the affirmative path really clears the active world.
tap 240 765
open_clear_sheet
sleep 1
capture clear-confirm
tap 244 640
sleep 1
capture cleared
tap 40 715
swipe 90 300 160 340 400
sleep 1
capture after-clear-redraw
if same_canvas_pixels "$evidence/elementum-cleared.png" "$evidence/elementum-after-clear-redraw.png"; then
  echo "Drawing immediately after Clear produced no visible world edit" >&2
  exit 1
fi
open_load_sheet
capture load-browser
sleep 1
adb shell input keyevent KEYCODE_BACK
sleep 1
capture load-browser-back-dismissed
assert_foreground load-browser-back-dismiss
test -n "$(adb shell pidof com.tomex.elementum)"
open_load_sheet
capture load-browser-reopened
sleep 1
# Android Load is a touch-first in-game slot browser.
tap 210 401
sleep 2
capture loaded
adb shell input keyevent KEYCODE_HOME
sleep 2
adb shell am start -W -n com.tomex.elementum/com.gdx.cellular.AndroidLauncher
sleep 3
dismiss_system_fullscreen_confirmation
capture resumed

# Local erase is a first-class material-strip action.
tap 270 715
capture erase-selected
swipe 160 300 205 330 450
sleep 1
capture erase-stroke

# Prove the thermal phase change while paused so the water cannot flow away
# between drawing and cooling. Thermal tools are on the second top-right row.
tap 90 715
tap 240 765
swipe 125 410 205 410 400
sleep 1
capture cooling-water-before
tool_cool
swipe 125 410 205 410 400
sleep 1
capture cooling-water-after

# While still paused, heat the frozen strip once to melt Ice -> Water, then a
# second time to boil Water -> Steam. Cool that stationary steam back into
# Water to prove the reverse gas/liquid phase transition without movement
# hiding the result.
tool_heat
swipe 125 410 205 410 400
sleep 1
capture melting-ice-after
swipe 125 410 205 410 400
sleep 1
capture evaporation-steam-after
tool_cool
swipe 125 410 205 410 400
sleep 1
capture condensation-water-after
tap 240 765

# Heat is paired with Cool. Ignite a Petrol strip, then a material shortcut
# must restore SPAWN before the session continues.
tap 145 715
swipe 115 445 220 445 450
sleep 1
capture heat-petrol-before
tool_heat
swipe 115 445 220 445 450
sleep 2
capture heat-petrol-after
tap 40 715

# Build a stationary Petrol + Lightning contact while paused, then resume it.
# Brush size 3 is one matrix cell for a circle, so the strike sits directly
# above rather than replacing the Petrol cells it must ignite.
tap 240 765
tap 40 765
tap 145 715
swipe 110 500 230 500 450
sleep 1
tap 210 715
swipe 110 494 230 494 450
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
swipe 110 545 230 545 450
sleep 1
tap 210 715
swipe 110 539 230 539 450
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
grep -q 'ElementumInput.*help=open' "$evidence/logcat.txt"
grep -q 'ElementumInput.*material-picker=back-closed' "$evidence/logcat.txt"
grep -q 'ElementumInput.*more-sheet=open' "$evidence/logcat.txt"
grep -q 'ElementumInput.*back-dismiss=mobile-overlay' "$evidence/logcat.txt"
clear_cancel_line=$(grep 'ElementumInput: clear-cancelled before=' "$evidence/logcat.txt" | tail -1)
python3 - "$clear_cancel_line" <<'PY'
import re,sys
line=sys.argv[1]
m=re.search(r'before=(\d+) after=(\d+).*dimensionsPreserved=(true|false).*pausedBefore=(true|false) pausedAfter=(true|false)', line)
assert m, f'Could not parse clear-cancel evidence: {line}'
before,after=map(int,m.group(1,2))
assert before > 0, f'Clear cancel fixture had no live cells: {line}'
assert before == after, f'Clear cancel changed live-cell count: {line}'
assert m.group(3) == 'true', f'Clear cancel changed world dimensions: {line}'
assert m.group(4) == 'true' and m.group(5) == 'true', f'Clear cancel changed paused state: {line}'
PY
clear_confirm_line=$(grep 'ElementumInput: clear-confirmed before=' "$evidence/logcat.txt" | tail -1)
python3 - "$clear_confirm_line" <<'PY'
import re,sys
line=sys.argv[1]
m=re.search(r'before=(\d+) remaining=(\d+) removed=(-?\d+).*dimensionsPreserved=(true|false).*pausedBefore=(true|false) pausedAfter=(true|false)', line)
assert m, f'Could not parse clear-confirm evidence: {line}'
before,remaining,removed=map(int,m.group(1,2,3))
assert before > 0, f'Clear confirm fixture had no live cells: {line}'
assert remaining == 0, f'Clear confirm left live cells behind: {line}'
assert removed == before, f'Clear confirm removed-count mismatch: {line}'
assert m.group(4) == 'true', f'Clear confirm changed world dimensions: {line}'
assert m.group(5) == 'false' and m.group(6) == 'false', f'Clear confirm changed running state: {line}'
PY
grep -q 'ElementumInput: clear-settled remaining=0 .*paused=false' "$evidence/logcat.txt"
grep -q 'ElementumSaveLoad.*saved=scene_1' "$evidence/logcat.txt"
save_count=$(grep -c 'ElementumSaveLoad.*saved=scene_1.*atomic=true' "$evidence/logcat.txt" || true)
if [ "$save_count" -lt 2 ]; then
  echo "Expected initial save plus atomic overwrite, got atomic save count=$save_count" >&2
  exit 1
fi
grep -Eq 'ElementumSaveLoad.*browser-scenes=[1-9][0-9]*' "$evidence/logcat.txt"
grep -q 'ElementumSaveLoad.*browser-selected=scene_1' "$evidence/logcat.txt"
grep -q 'ElementumSaveLoad.*loaded=scene_1.*format=V3' "$evidence/logcat.txt"
grep -Eq 'ElementumSaveLoad.*restored-stateful=[0-9]+.*restored-ignited=[0-9]+.*transactional=true' "$evidence/logcat.txt"
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
grep -q 'ElementumLifecycle.*autosave=__autosave.*atomic=true' "$evidence/logcat.txt"

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
dismiss_system_fullscreen_confirmation
capture process-restart
test -n "$(adb shell pidof com.tomex.elementum)"
adb shell "run-as com.tomex.elementum sh -c '[ -s files/save/scene_1.ser ]'"

open_load_sheet
capture process-restart-load-browser
sleep 1
tap 210 401
sleep 2
capture process-restart-loaded
adb logcat -d > "$evidence/logcat-after-process-restart.txt"
grep -q 'ElementumSaveLoad.*browser-selected=scene_1' "$evidence/logcat-after-process-restart.txt"
grep -q 'ElementumSaveLoad.*loaded=scene_1.*format=V3' "$evidence/logcat-after-process-restart.txt"
grep -q 'ElementumSaveLoad.*transactional=true' "$evidence/logcat-after-process-restart.txt"
grep -q 'ElementumLifecycle.*autosave-restored format=V3 transactional=true' "$evidence/logcat-after-process-restart.txt"
test -n "$(adb shell pidof com.tomex.elementum)"
