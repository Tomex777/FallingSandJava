# Elementum — Agent 1: GPU Water Appearance and Budget GPU

**Session status: STOPPED with transparent actual timing.** Started 2026-10-22T21:51:24+01:00; stopped 2026-10-22T22:31:45+01:00; elapsed **40m21s**, which exceeded the exactly-40:00 target by 21 seconds. Do not misrepresent this as exactly 40m. Work committed and status recorded in PR #9.

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
- START (ISO8601 WAT): **2026-10-22T21:51:24+01:00** (GitHub comment 6351036751)
- STOP (ISO8601 WAT): **2026-10-22T22:31:45+01:00**, actual **40m21s**, 21 seconds later than 40:00 target; no backdating (GitHub comment 6351772435).
- Claim comment link: https://github.com/Tomex777/FallingSandJava/pull/9#issuecomment-6351036751
- Commits / child PR: https://github.com/Tomex777/FallingSandJava/pull/15 (draft, not approved); latest renderer/test implementation commit **fafdcddb**; managed Pixmap EGL restoration, exact sparse dirty-tile upload, lower-end quality, 5s telemetry, and phase-stable surface-only glints. Tests/helper and branch-local CI included. Report: docs/agent-reports/elementum-agent-1.md.
- Tests / API26+36 CI runs: matching local Java geometry model 20,833,250 cases PASS; local sparse/full byte-equivalence model 57,600 randomized frames across 24 seeds and 6 grid sizes PASS (not device tests); head-specific build/test CI run 39857155852 was QUEUED at 22:31:45 WAT and **Android Gradle / API26+36 runtime passes are NOT CLAIMED**.
- Actual screenshot artifact names and source SHA: **NOT CAPTURED. Real GPU water appearance remains unapproved.** Coordinate with Agent 5 integrated smoke and source SHA.
- Visual defects found/fixed: No runtime visual claim. Retained correct original SpriteBatch UV orientation after independently verifying the library's bottom-v/top-v2 vertex ordering; fixed incorrect proposed UV swap before PR. Verified Pixmap 6-int overload and corrected source/destination argument order. Added dirty upload skip and EGL restore. Current render appearance still requires actual screenshots.
- Remaining issues and next unclaimed tasks: Confirm Android compile and CI; verify shader on API26/API36, lifecycle context recreation and actual paused/flowing water screenshots, low-end FPS frame-time. User approval pending. Keep PR DRAFT and do not merge into elementum until accepted.
