# Elementum — Agent 4: Naturalistic Sound, Latency and Lifecycle

**Session status: ACTIVE (not yet verified as a full 40-minute run).** Actual START is recorded in PR #9. No STOP has been recorded; this document does not certify exact session duration.

**Branch:** `agents/elementum-a4-sound-40m-20261022`
**Target integration PR branch:** `feature/elementum-abr-visual-audio-20261022`
**Owned source files:** `core/src/com/gdx/cellular/audio/**; android/src/com/gdx/cellular/AndroidLauncher.java; audio tests`
**Canonical rules:** [ELEMENTUM_AGENTS_40MIN.md](https://github.com/Tomex777/FallingSandJava/blob/feature/elementum-abr-visual-audio-20261022/ELEMENTUM_AGENTS_40MIN.md) and `AGENTS.md`.
**Coordination:** GitHub Issues are disabled; post START, ownership/takeover, STOP comments directly to [PR #9](https://github.com/Tomex777/FallingSandJava/pull/9). Do not backdate.

## Exact 40-minute job
- 00:00–05:00: inspect code, baseline, last CI artifacts; immediately prepare concrete reproduction.
- 05:00–28:00: implement iteratively and test every completed section. No idle CI queue watching.
- 28:00–35:00: live Android emulator visual/function testing; capture actual screenshots and correct any defects.
- 35:00–40:00: refine, test, create PR to integration branch, and publish evidence/handoff. If primary work ends sooner than 40:00, claim **unowned/idle** work in PR comments and keep actively working to 40:00.

## Specific implementation checklist
- [x] Audit procedural WAV initialization/playback (not yet recorded natural audio); add appropriate water impact vs continuous movement, grain and fire with restrained volume.
- [x] Implement toggle/lifecycle, bound voice requests and no overlapping audio storm, click/pops, per-cell playback or render-thread blocks; maintain async audio.
- [x] Add PCM/WAV and limiter tests and local JVM validation; Android logs still pending; use Android emulator logs for callback correctness, clearly state -noaudio can't validate listening.
- [x] Provide a real-device audio listening QA checklist and disclose any unattainable evidence.

## Release/acceptance gate
Audible control behavior not fabricated; deterministic automated audio tests and safe lifecycle with no frame stalls.

**Never claim** 40 minutes elapsed from file creation or a GitHub CI run. User must see **actual in-app water screenshots** before approving visual shader output. minSdk26, no duplicated controls, exactly two horizontal-swipe rows, real release install/QA remain requirements.

## Actual work log (agent fills this while running)
- START (ISO8601 WAT): **2026-10-22T21:50:58+01:00** (PR #9 comment 6351028818)
- STOP (ISO8601 WAT): **NOT RECORDED / NOT COMPLETE**
- Claim comment link: https://github.com/Tomex777/FallingSandJava/pull/9#issuecomment-6351028818
- Commits / child PR: https://github.com/Tomex777/FallingSandJava/pull/11
- Tests / API26+36 CI runs: local JVM smoke PASS (not Android); agent audio GitHub Actions currently queued; release/emulator API 26+36 unverified
- Actual screenshot artifact names and source SHA: none from this audio-only branch; user GPU water visual approval pending Agent 1/E
- Visual defects found/fixed: UI Sound label does not reflect saved off state on restart; coordination comment on Agent B PR #12
- Remaining issues and next unclaimed tasks: Android JUnit/compile queued; require audio listening and lifecycle on physical phone; see docs/agent-reports/elementum-agent-4.md.
