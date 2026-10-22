# Elementum — Agent D / Agent 4 audio work report

**Updated (WAT):** 2026-10-22T22:03:49+01:00
**Current status:** IN PROGRESS, not complete; **do not count queued CI time as active implementation**.
**Actual START:** 2026-10-22T21:50:58+01:00, PR #9 comment ID 6351028818.
**Actual STOP:** NOT RECORDED — the 40:00 uninterrupted-session requirement has NOT yet been satisfied or certified.
**Scope branch:** `agents/elementum-a4-sound-40m-20261022`
**Integration:** draft PR #11 into PR #9 branch. Never merge this work straight into `elementum`.

## Findings and changes already committed

1. The original audio generated four short WAVs, trusted cached files if their lengths were merely >=44 bytes, and used a single shared 155 ms cooldown; no stored Sound preference, stop-on-mute, or Android lifecycle hook existed.
2. Added `AudioEventGate` with one allocation-free global limiter (110 ms) and distinct material cooldowns (water 175 ms, grains 165 ms, heat 235 ms, electric 285 ms). The event callback is once per brush operation, not per painted cell.
3. Added `SynthesizedWav`: versioned 22,050 Hz 16-bit little-endian mono WAV generation, deterministic noise, gentle start/end fade to avoid clicks, more subdued water/grain/fire/electric textures, strict cached WAV header verification and file-length gate before reading. No network or third-party clips.
4. `MaterialSoundscape` now reuses four cached clips, limits event admission, makes small pitch changes, requests stop on toggle-off and Android pause, resumes with a reset event gate, persists the enabled state in libGDX Preferences, and releases/unregisters lifecycle callbacks. Android launcher additionally caps SoundPool streams at 4; actual audio delivery remains unverified.
5. Investigated exact libGDX 1.14.2 AsynchronousSound source: its play() returns a *queued operation token*, not an audible SoundPool stream confirmation. Corrected runtime logs to say `paint-kind=N queued=true` instead of asserting playback success, and avoided queueing asynchronous stop commands immediately before synchronous sound disposal.
6. Narrow, shared-comment-claimed `InputManager.java` audio change: rectangle brush used to blip while outlining with **zero rectangle cells placed**. Moved its sound emission to finger release, and ordinary paint/particle callbacks after spawn calls. Did not change physics or input selection.

## Verification completed so far

- Recompiled `AudioEventGate` + `SynthesizedWav` on local Java 21 with `-source 8 -target 8`; executed a JVM smoke of all four WAV headers, deterministic re-generation, zero attack/release endpoints, event cooldown thresholds and reset. **PASS**; this is *not* Android SDK compilation.
- Each generated WAV passes Python's built-in `wave` reader. Synthesized samples: water 13,274 B (0.30 s, RMS -27.5 dBFS); grains 7,100 B (0.16 s, RMS -24.4 dBFS); heat 10,628 B (0.24 s, RMS -22.4 dBFS); electric 7,982 B (0.18 s, RMS -23.5 dBFS). Those RMS levels are **pre-SoundPool level scaling** and do not prove audibility.
- Focused Gradle/JUnit + Android debug compilation runs are queued in GitHub Actions; no success claimed. Runs observed queued include #39853157900 and latest at time of this report #39853842781.
- **No** Android API26/API36 emulator screenshots, device logs, real-device listening QA or physical-speaker inspection has yet passed for this branch. Agent E owns integrated visual screenshots.
- Agent B's More-sheet Sound label initially hard-codes “Sound on”, despite preference persistence. Reported compatibility fix directly on PR #12 in comment 6351240821; did **not** modify Agent B's UI file.

## Necessary real-device listening acceptance

Use the integrated APK on both API26 and API36 as well as an actual lower-end phone. Use moderate speaker volume first, then headphones at sensible levels. Check isolated droplets, long water dragging, sand/grains, lava/fire, electric bolts, switching materials rapidly, rectangle preview versus finger release, and drawing repeatedly for one minute. Confirm no harsh crackle, per-frame chatter, overloaded overlapping voices, disappearing toggle state or unexpectedly loud electrical hits. Toggle Sound off while a clip is playing (clip must stop immediately), background/resume while dragging, force-stop/relaunch and confirm the saved toggle matches audio and visible label. Capture `ElementumAudio` logs and frame-time metrics. Do **not** claim emulator listening on a `-noaudio` runner.

## Pending before merge

- Current-head `:core:test`, `:android:assembleDebug`, real API26/API36 launch and release APK installation.
- Pixel screenshots from the APK, user water approval, and physical-device audio listening; do not fabricate any.
- Agent B Sound label fix integration; coalesce audio change without overwriting other agents.
- This branch has a prepared `AGENT_TASK.md` also present on other agent branches; coordinator must **exclude or relocate** that duplicate top-level file when merging separate PRs.
