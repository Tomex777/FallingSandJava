# Elementum Agent E — Android QA, visual proof, release gating

**Status:** ACTIVE. Start: **2026-10-22T21:54:02+01:00**. Stop: **not yet reached**; post actual STOP in coordination PR #9. Never claim 40:00 before elapsed.

**Code:** [draft PR #13](https://github.com/Tomex777/FallingSandJava/pull/13) targeting the **integration** branch `feature/elementum-abr-visual-audio-20261022`; do not merge PR #9 or approve GPU-water visuals yet.

## Real baseline and evidence

- Android 8/API26 and Android16/API36 `assembleDebug`, `adb install`, launch **passed** on earlier integration SHA `d8261461d09ca13974063e6a4dd4a8d96207ef5f`; **full end-to-end interaction smoke FAILED** at Help. Evidence: [Actions run 39845584911](https://github.com/Tomex777/FallingSandJava/actions/runs/39845584911), artifacts `elementum-runtime-api-26` (12456308042) and `elementum-runtime-api-36` (12457076206).
- Real screenshot `elementum-help-sheet.png` on BOTH Android levels visibly clips last line under lower material row. Logs: `ElementumUI: help-content-fits=false preferred=332 available=320`. Agent B's draft fix PR #12 adds scrollable Help and hides the shelf under modal. **Not verified on emulator yet**.
- Real `elementum-startup.png`, `elementum-material-shelf-scrolled.png`, `elementum-material-shelf-restored.png` show exactly TWO stacked horizontally browsable material rows. Local Pillow pixel comparisons of central canvas viewport: **ZERO changes on both API26 and API36 during both shelf swipes**, while material shelf content really changes. An imperfect return scroll offset on API26 is not proof of a touch bug.
- The previous run's API26 final `ElementumPerf` sample: fps42 / avg frame 8122us / max frame 19252us. API36 final sample: fps37 / avg 23777us / max71374us. These are **software GPU emulator samples before any water**, not physical-phone or water-performance benchmarks.
- Logs `ElementumFluid: smooth-water-shader=enabled` and `ElementumAudio: sfx-ready clips=4` prove only **initialized features**; no acceptable fluid screenshot or listening proof exists from the baseline.

## New isolation and strict real-device runner assertions

1. Preserve **first failure** exact shell command/line, screenshot, gfxinfo, and useful Help geometry. Keep Help accessibility failure a hard gate.
2. Independently assemble **universal** and **ARM64** APKs and AAB; CI-sign and check package ABIs, SHA256 and actual API26/API36 foreground installation even when full UI interaction fails. Exercise **real CI-signed release** water brush, screenshot it, assert nonzero GPU water log and input receipt. CI keys are ephemeral and **not** production release signing.
3. Execute a stand-alone true Android GPU-water scenario on API26 and API36 under SwiftShader, no mock pictures: select Water, paint paused water pool, capture paused and moving 360×800 and resized 320×568 screens; log genuine water cell count, GPU frame metrics, package memory and crash/ANR/fallback checks.
4. Decode captured Android PNGs using Python 3 stdlib and **require visible blue pixels in the canvas** (not blue material chip colors); test actual image presence both on normal and compact displays. Verified detector with both blank real-emulator baseline screens as **negative** cases and a separately labeled synthetic positive-control fixture; positive control is never presented as a real screenshot.
5. Fail full interaction smoke if horizontal two-row quick shelf or full categorized picker swipes alter untouched sandbox canvas; enforce real compact 320×568 Help accessibility, screenshot before/after scroll, and check Close position.
6. Collect runtime, water and signed-release-install evidence from both API levels into one provenance-indexed `elementum-integrated-qa-evidence` CI artifact, even when component smoke jobs fail. Separate actual release APK artifacts remain available.
7. Independently compiled Agent A's pure `FluidMaskGeometry` helper with Java17 and checked **591,500 deterministic randomized property cases** on 1–130px grids: row inversion, UV boundary, dirty tile coverage/index bounds and full-upload thresholds: **PASS**. Geometry math is not proof of Android GL upload correctness, water appearance or FPS.

## Collaboration and safety

- Agent B owns `MobileControls.java` Help/ABR fixes; coordination comments in PR #12. Agent A owns GPU renderer/performance; PR #15 comment warns all CI emulators force **low-detail SwiftShader** output, so full-quality rendering still needs separate inspection.
- Agent C owns physics and Agent D owns audio. No source-file ownership overlap was introduced by E.
- Both Android API-level runners and release installation must pass on the **integrated** commit. No dummy mock screenshots, no false green builds. App remains minSdk **26** by source inspection.
- User explicitly must **see real GPU-water screenshots** before saying the shader look is approved. No physical-phone GPU, audio-listening, sustained-FPS, owner signing or user visual acceptance has yet been verified.

## Acceptance gates still outstanding

**Current head CI status:** workflow run for PR #13 pending/queued at the latest observation; no green claim; no new water screenshots yet. [CI runs](https://github.com/Tomex777/FallingSandJava/actions/workflows/elementum-final-qa.yml).

**Do not merge** PR #9 just because branch/Gradle compiles. Collect actual still/moving water images from both APIs and user feedback; integrate other owner PRs carefully without overwriting agent-specific `AGENT_TASK.md`; then verify full runtime tests, signed release installs, device budget performance and real audio.
