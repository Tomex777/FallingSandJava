# Elementum — Agent 1: GPU Water Appearance and Budget GPU

**Session status: NOT STARTED.** This file is a concrete work order, not proof an agent is running. The agent must record actual start/stop timestamps, work uninterrupted for EXACTLY **40:00 real-time wall-clock minutes**, and write its handoff.

**Branch:** `agents/elementum-a1-gpu-water-40m-20261022`
**Target integration PR branch:** `feature/elementum-abr-visual-audio-20261022`
**Owned source files:** `core/src/com/gdx/cellular/effects/FluidSurfaceRenderer.java and new fluid-only helpers/tests`
**Canonical rules:** [ELEMENTUM_AGENTS_40MIN.md](https://github.com/Tomex777/FallingSandJava/blob/feature/elementum-abr-visual-audio-20261022/ELEMENTUM_AGENTS_40MIN.md) and `AGENTS.md`.
**Coordination:** GitHub Issues are disabled; post START, ownership/takeover, STOP comments directly to [PR #9](https://github.com/Tomex777/FallingSandJava/pull/9). Do not backdate.

## Exact 40-minute job
- 00:00–05:00: inspect code, baseline, last CI artifacts; immediately prepare concrete reproduction.
- 05:00–28:00: implement iteratively and test every completed section. No idle CI queue watching.
- 28:00–35:00: live Android emulator visual/function testing; capture actual screenshots and correct any defects.
- 35:00–40:00: refine, test, create PR to integration branch, and publish evidence/handoff. If primary work ends sooner than 40:00, claim **unowned/idle** work in PR comments and keep actively working to 40:00.

## Specific implementation checklist
- [ ] Profile water mask CPU scan, GPU upload, shader region, number of texture updates and frame pacing on API26/36 and 320x568.
- [ ] Fix actual UV/alpha/reflection and clipped/empty water; stabilize animations, reduce GPU draw work and maintain automatic low-end fallback.
- [ ] Capture actual emulator screenshots while water paused and flowing, identify the exact source SHA and graphics renderer; never pass synthetic images off as runtime screenshots.
- [ ] Benchmark with ElementumPerf and GPU logs; keep physics unaffected and work with Agent3 on narrow integration interface.

## Release/acceptance gate
Smooth visible water from an actual APK; no blank or inverted water, no crashing fallback, measurable budget-device profile.

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
