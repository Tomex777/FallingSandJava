# Elementum — Agent 3: Material Physics and Smooth Simulations

**Session status: NOT STARTED.** This file is a concrete work order, not proof an agent is running. The agent must record actual start/stop timestamps, work uninterrupted for EXACTLY **40:00 real-time wall-clock minutes**, and write its handoff.

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
- START (ISO8601 WAT): **NOT STARTED**
- STOP (ISO8601 WAT): **NOT STARTED**
- Claim comment link: —
- Commits / child PR: —
- Tests / API26+36 CI runs: —
- Actual screenshot artifact names and source SHA: —
- Visual defects found/fixed: —
- Remaining issues and next unclaimed tasks: —
