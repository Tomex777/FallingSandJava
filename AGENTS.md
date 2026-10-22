# Elementum contributor rules

Elementum is the Android falling-sand **simulation** in this repository, not a static scene editor. Preserve material interactions, simulation frame performance, and real save/load behavior.

## Android Building Rules (ABR)
- Minimum supported Android SDK remains **26**; validate both API 26 and API 36.
- No redundant controls doing the same job. No stock Android text prompts in the app UX; use the current custom in-game dialogs.
- Keep the sandbox dominant, controls reachable in portrait, and UI icons and text legible on compact devices.
- A successful compile alone does **not** mean the job is done: install and exercise the APK; examine screenshots, logcat, physics tests, ANRs and releases.
- Verify builds on the current PR head before merge, not an older green run. Preserve existing working functionality and save compatibility.

## Materials UX
- The always-visible bottom materials shelf is **exactly two stacked rows**, not two vertical columns or one long column.
- Both rows move **horizontally as one unit** under finger swipe; material taps select, not paint.
- The full categorized material picker is also two rows with horizontal scrolling (no tall vertical material grid).
- Keep useful one-tap Sand, Water, Petrol, Lightning, Erase and category-browser affordances readily available without offscreen controls.
- Retain finger-sized touch targets and show checked/selected states. When opening a modal sheet, do not allow hidden shelf buttons to intercept taps.

## Verification
- `.github/scripts/elementum-portrait-smoke.sh` exercises the scrollable shelf, categorized picker, drawing, thermal and electric interactions, source placement, save/load, clear and compact viewport.
- `.github/workflows/elementum-final-qa.yml` must pass Android API 26/36 runtime checks, screenshot capture, release building, and release installation. Inspect artifacts visually before closing any PR.
- Production owner signing and testing on a physical Android phone must be reported separately rather than implied by CI-signing.

## Five-agent 40-minute collaboration contract
For delegated work, **read [ELEMENTUM_AGENTS_40MIN.md](ELEMENTUM_AGENTS_40MIN.md) before touching code**. It defines five non-overlapping specialist workstreams (GPU water, responsive ABR UI, simulation physics, sound, release/visual QA), individual source ownership, exact **40:00 active wall-clock minutes**, time-stamped issue start/stop logs, mandatory per-section visual testing, early-finish takeover only from unclaimed/idle work, and real screenshot-based user acceptance. Never claim an agent has run merely because a branch/issue exists.

Current open integration PR #9 is **not yet accepted**; latest earlier API26/API36 smoke failed with UI font overflow and real GPU-water visual evidence remains pending. Do not merge until the user has seen true running-game water screenshots and the fresh head passes install/QA.
