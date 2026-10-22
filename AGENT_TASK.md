# Elementum — Agent 3: Material Physics and Smooth Simulations

**Session status: IN PROGRESS as of 2026-10-22T21:50:37+01:00.** Actual start is recorded in PR #9 comment 6351025068; the full 40:00 session cannot be claimed until its end at 22:30:37 WAT. Do not claim a complete 40-minute session prematurely.

**Branch:** `agents/elementum-a3-physics-40m-20261022`
**Target integration PR branch:** `feature/elementum-abr-visual-audio-20261022`
**Owned source files:** `core/src/com/gdx/cellular/CellularMatrix.java; elements/**; focused tests; CellularAutomaton only after agreement`
**Canonical rules:** [ELEMENTUM_AGENTS_40MIN.md](https://github.com/Tomex777/FallingSandJava/blob/feature/elementum-abr-visual-audio-20261022/ELEMENTUM_AGENTS_40MIN.md) and `AGENTS.md`.
**Coordination:** GitHub Issues are disabled; post START, ownership/takeover, STOP comments directly to [PR #9](https://github.com/Tomex777/FallingSandJava/pull/9). Do not backdate.

## Exact 40-minute job
- 00:00–05:00: inspect code, baseline, last CI artifacts; immediately prepare concrete reproduction.
- 05:00–28:00: implement iteratively and test every completed section. No idle CI queue watching.
- 28:00–35:00: live Android emulator visual/function testing; capture actual screenshots and correct any defects.
- 35:00–40:00: refine, test, create PR to integration branch, and publish evidence/handoff. If primary work ends sooner than 40:00, claim **unowned/idle** work in PR comments and keep actively working to 40:00.

## Specific implementation checklist
- [ ] Audit water collisions, flow/settling, material reactions, freeze/heat and conservation without rewriting cell-grid simulation.
- [ ] Implement minimal physically justified liquid-motion and thermal fixes with unit regressions, preserve stable save file compatibility.
- [ ] Profile CPU / multi-thread stepping; avoid allocations in the inner loop and investigate visually demonstrable stutters.
- [ ] Test under paused and unpaused motions with real screenshots and recorded fps on API26/36.

## Release/acceptance gate
No regression to materials, water movement coherent, physics unit suite green, screen capture corroborates result.

**Never claim** 40 minutes elapsed from file creation or a GitHub CI run. User must see **actual in-app water screenshots** before approving visual shader output. minSdk26, no duplicated controls, exactly two horizontal-swipe rows, real release install/QA remain requirements.

## Actual work log (agent fills this while running)
- START (ISO8601 WAT): **2026-10-22T21:50:37+01:00**
- STOP (ISO8601 WAT): **NOT YET REACHED**
- Claim comment link: https://github.com/Tomex777/FallingSandJava/pull/9#issuecomment-6351025068
- Commits / child PR: https://github.com/Tomex777/FallingSandJava/pull/10 (draft). Water boundary conservation, fluid-layer collision, CPU hot-loop allocation, opposing thermal state; JUnit tests and dedicated physics workflow.
- Tests / API26+36 CI runs: Dedicated :core:test run(s) queued; results **unverified**. Main PR #9 Android API26/API36 acceptance belongs to integrated QA and is not yet proven by Agent C.
- Actual screenshot artifact names and source SHA: **NONE YET**. Do not claim screenshots or visual approval.
- Visual defects found/fixed: —
- Remaining issues and next unclaimed tasks: Verify latest test run on actual head; examine Android 26/36 runtime screenshots and frame-time; identify cross-column worker races without changing Agent 1 owned renderer.
