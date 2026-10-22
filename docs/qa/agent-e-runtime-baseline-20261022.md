# Agent E — Android QA baseline (22 Oct 2026, WAT)

## Verified baseline — actual APK, not a design mock

* **Integration PR:** #9 at commit `d8261461d09ca13974063e6a4dd4a8d96207ef5f`.
* **Most recent downloaded runtime artifacts:** [GitHub Actions run 39845584911](https://github.com/Tomex777/FallingSandJava/actions/runs/39845584911).
* **API 26:** runtime job `120057875783`, artifact `12456308042`; 360×800 logical with 1080×1920 physical screencap. Debug APK assembled/installed/launched. Smoke **FAIL**.
* **API 36:** runtime job `120057875288`, artifact `12457076206`; real 360×800 screencap. Debug APK assembled/installed/launched. Smoke **FAIL**.
* **Exact first failure on both APIs:** `ElementumUI: help-content-fits=false preferred=332 available=320` after opening Help. That is 12 logical screen units of overflow. Actual screenshot shows the lower Help text clipped/covered by the materials shelf. The source-font reduction from 40px to 20px improved legibility but did **not** make the bottom sheet fit. **Agent 2 owns MobileControls UI corrections**; Agent E retains strict fit assertions and must not alter layout just to make CI green.
* The actual startup capture shows two horizontally arranged stacked shelf rows above the six bottom actions; the Help sheet overlaps part of this shelf. Initial blank sandbox appearance was also observed, but no gameplay water screenshots exist in this failed run. Screenshots should not be misrepresented as water visual acceptance.
* Both APIs logged `ElementumFluid: smooth-water-shader=enabled` and `ElementumAudio: sfx-ready clips=4`. These **prove initialization only**, not visual quality, audible output, or lower-end GPU frame-time.
* The previous run [39829042784](https://github.com/Tomex777/FallingSandJava/actions/runs/39829042784) also failed both runtime variants. **No current-head release build/install evidence** is present because `build-release` was unnecessarily gated on `runtime` success.

## QA changes made on Agent E's isolated branch

1. Preserve a first-failure screenshot, gfxinfo and exact failing shell command; explain genuine Help geometry regression directly instead of exiting silently.
2. Make release assembly independent of UI runtime gate: generate universal/ARM64 artifacts even when Help fails, then independently install-test CI-signed universal APK on API26 and API36.
3. Add an isolated real-APK GPU-water visual smoke for both API26 and API36. It paints a paused water pool, captures resumed motion and 320×568 compact shots, collects performance/gfxinfo and log proof for true nonzero water cells. It is separate from the long interaction smoke so the user's water screenshot is not indefinitely blocked by Help layout.
4. Enable agent/integration branch CI conditions without overwriting canonical QA pointers from the QA-only branch.

## Acceptance and unresolved requirements

The added water CI scenario has **not yet been confirmed green** on this branch. Do not confuse code committed with executed passing smoke. Manually inspect its uploaded in-app screenshots for genuine water, no inverted UV/holes/flicker, and water moving against gravity/collision as expected. Test sound using an audio-capable physical phone: SwiftShader emulators deliberately use `-noaudio`. Profile on a lower-end real Android device and confirm production-owner signing before claiming production readiness. Do **not** merge shader design before showing actual water to user for visual approval. Verify exact minSdk 26 and leave two stacked horizontal swiping rows intact.
