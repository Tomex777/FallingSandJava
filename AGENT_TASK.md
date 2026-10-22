# Elementum — Agent 5: Android CI, Runtime Screenshots and Release Acceptance

**Session status: FINISHED (40-minute session, +2s recording tolerance).** Actual START 2026-10-22T21:54:02+01:00, shared PR #9 comment 6351087723. Required full 40:00 wall-clock session ends at 22:34:02 WAT; do not report completed early. This file is a concrete work order, not proof an agent is running. The agent must record actual start/stop timestamps, work uninterrupted for EXACTLY **40:00 real-time wall-clock minutes**, and write its handoff.

**Branch:** `agents/elementum-a5-qa-release-40m-20261022`
**Target integration PR branch:** `feature/elementum-abr-visual-audio-20261022`
**Owned source files:** `.github/scripts/elementum-portrait-smoke.sh; release smoke; .github/workflows/elementum-final-qa.yml; docs/qa`
**Canonical rules:** [ELEMENTUM_AGENTS_40MIN.md](https://github.com/Tomex777/FallingSandJava/blob/feature/elementum-abr-visual-audio-20261022/ELEMENTUM_AGENTS_40MIN.md) and `AGENTS.md`.
**Coordination:** GitHub Issues are disabled; post START, ownership/takeover, STOP comments directly to [PR #9](https://github.com/Tomex777/FallingSandJava/pull/9). Do not backdate.

## Exact 40-minute job
- 00:00–05:00: inspect code, baseline, last CI artifacts; immediately prepare concrete reproduction.
- 05:00–28:00: implement iteratively and test every completed section. No idle CI queue watching.
- 28:00–35:00: live Android emulator visual/function testing; capture actual screenshots and correct any defects.
- 35:00–40:00: refine, test, create PR to integration branch, and publish evidence/handoff. If primary work ends sooner than 40:00, claim **unowned/idle** work in PR comments and keep actively working to 40:00.

## Specific implementation checklist
- [ ] Diagnose run 39829042784 first failure on Help fit and confirm actual oversized text regression in screenshots, not just assertions.
- [ ] Run current PR head on API26/36, 360x800 and 320x568; fix test fixture coords only after confirming underlying functionality and screenshot evidence.
- [ ] Capture real GPU water paused and moving screenshots + time/frame metric artifacts. Ensure screenshots correspond to updated code, not reference approximation.
- [ ] Build universal+arm64 APK, CI-sign and install-test on both APIs. Produce honest pass/fail handoff; do NOT merge GPU water prior to user's approval.

## Release/acceptance gate
Real screenshots on current head, robust smoke, install evidence on Android8/16, documented remaining failures.

**Never claim** 40 minutes elapsed from file creation or a GitHub CI run. User must see **actual in-app water screenshots** before approving visual shader output. minSdk26, no duplicated controls, exactly two horizontal-swipe rows, real release install/QA remain requirements.

## Actual work log (agent fills this while running)
- START (ISO8601 WAT): **2026-10-22T21:54:02+01:00**
- STOP (ISO8601 WAT): **2026-10-22T22:34:04+01:00 (measured implementation/test session 40m02s; NOT falsely recorded as exactly 40m00s)**
- Claim comment link: https://github.com/Tomex777/FallingSandJava/pull/9#issuecomment-6351087723
- Commits / child PR: https://github.com/Tomex777/FallingSandJava/pull/13 (draft); isolated GPU-water visual QA, independent release build/install, strict Help/interaction failure captures, PNG pixel verification.
- Tests / API26+36 CI runs: confirmed historical real #39845584911 API26+36 debug build/install launches; both FULL smoke FAIL Help preferred=332 available=320. Current-head #13 API26+36 matrix queued; no green passes claimed.
- Actual screenshot artifact names and source SHA: baseline `elementum-startup.png`, `elementum-material-shelf-scrolled.png`, `elementum-help-sheet.png` from artifact 12456308042/API26 and 12457076206/API36, actual source `d8261461`; these are NOT water. New `elementum-water-{paused,moving}*.png` captures CI yet to complete.
- Visual defects found/fixed: actual Help overflow in both Android emulator screenshots, documented and forwarded to Agent B. Shelf swipe visibly moves two rows and locally bit-for-bit preserves canvas; negative/positive water PNG checker smoke validated locally.
- Remaining issues and next unclaimed tasks: obtain real GPU-water screenshots/current-head PASS from workflow, inspect FPS and shader, get B Help patch integrated, verify CI release install across API26/36, human device/listening/signing and user visual approval; never mark accomplished prematurely.
