# Elementum — five independent 40-minute coding-agent work orders

**Canonical product:** Elementum, the Android falling-sand/materials simulation at `Tomex777/FallingSandJava`.

**Repository limitation:** GitHub Issues are disabled here (GitHub returned HTTP 410). Do not try to record assignments in Issues. Use **PR #9 Conversation comments** for shared START/STOP and takeover claims, plus each agent's own `AGENT_TASK.md` and `docs/agent-reports/elementum-agent-N.md` for durable details.

**Shared source:** PR #9 `feature/elementum-abr-visual-audio-20261022` (builds upon two-row shelf PR #8). Each agent works on **its own branch created from this shared PR head**, and opens a small PR targeting that shared integration branch, **not** `master`/`elementum`. Never silently overwrite a teammate's files. Maintain this file and `AGENTS.md` as permanent contributor instructions.

## Exactly 40 minutes — meaning and enforcement
1. **Each agent runs one uninterrupted, individually timed 40:00-minute wall-clock session.** Record actual local/WAT start and stop timestamps (ISO 8601) and a time-ordered log in PR #9 conversation comments or own branch's `docs/agent-reports/elementum-agent-N.md`. The session is **not** done after a build, push, PR, plan, or a short 7-minute implementation; actively code, reproduce, test, inspect, revise, and retest.
2. **Minutes 00–05:** inspect the current source/PR and last failures, record baseline and an actionable plan; **05–28:** implement multiple coherent incremental changes; **28–35:** install/run on emulator, capture actual visuals/logs and iterate; **35–40:** fix last known issues, write dated PR #9 comment / branch handoff and screenshots. **This split is guidance, not permission to stop active work in any phase.** Whenever any discrete change lands, perform a visual/functional test immediately before the next section if possible.
3. **If the owned scope is complete before 40:00, do not stop.** Inspect the PR #9 coordination thread, select a concrete **unclaimed/idle** follow-on task, mark ownership in a PR #9 comment and report, and continue implementing/visually testing until the full 40 minutes have elapsed. Never make simultaneous edits to a claimed file or duplicate a running agent's work.
4. **If CI/emulator is queued, keep working** on reproduction, deterministic unit tests, code review, screenshot analysis or a non-conflicting unclaimed fix. Waiting, idle monitoring, or repeatedly reporting "building" does **not** count as development. Do not falsely claim physical-device runs.
5. Each agent must finish with: commit hashes; branch/PR links; exact work done; tests and build run IDs; real screenshots with device size/Android API and source SHA; before/after observations; FPS/frame-time evidence where applicable; remaining known failures; exact 40-minute timing record. If a blocker is real, spend remaining time on other non-conflicting work, not the same failed retries.
6. **Do not fabricate** active sessions, durations, screenshots, test passes, or results. Creating the issue/branch is *preparation*, not launching a runtime coding-agent session. To perform the session, an actual execution-capable agent must open and claim the work order and start its own 40:00 clock.

## ABR release gates — all five agents
- Keep minSdk **26**, test API **26 and 36**, real 360×800 and 320×568 portrait, and at least one representative lower-end phone profile/emulator. Build + install + exercise; a green Gradle compile is not completion.
- Respect one horizontally scrolling material shelf comprising **two vertically stacked rows**, and a two-row, horizontally swiping full picker. No redundant controls, default Android inputs, useless copy, status bar in fullscreen, or "AI vibe-coded" UI.
- Never hide broken shader/audio behind fake success. On unsupported hardware preserve a **working** low-cost cellular fallback; avoid GPU readback, unnecessary full-screen passes, per-frame Java object churn, and blocking audio.
- World physics, temperatures, reactions, save/load, eraser, pause, zoom/gestures and touch coordinates must remain correct. Neither water visuals nor audio qualifies as physical realism merely because the shader compiles.
- **Visual approval gate:** user specifically requested actual running-game GPU-water images **before** deciding approval. Screenshots or a real frame sequence must be from the APK at a named GitHub commit/API. A synthetic reference is *not* proof. Do not merge a water-visual redesign as "approved."
- Present one canonical integrated Android app and a single QA artifact collection, not five independent mini-previews.
- If an agent must edit another agent's owned file, first explicitly claim that scope through the issue and coordinate a cherry-pick. Prefer separate modules, tests, and PRs.

## Current confirmed starting condition — 2026-10-22
- PR #8 two-row shelf previously passed API26 and API36 **runtime** tests; that does not prove all later changes pass.
- PR #9 previous run **39829042784 failed runtime on both API26 and API36**, though unit test and debug APK build completed.
- Failure artifacts in that run show grotesquely **oversized font / clipped labels** in More and Help, and the smoke script exits after the help-screen assertion. Font source size was 40 pixels, and libGDX Label fontScale overrides the attempted 0.52 factor. The shared PR has an immediate fix reducing font source size to **20**, but that fix still needs fresh emulator screenshot verification.
- Logs from failed run reported `ElementumFluid: smooth-water-shader=enabled` and `ElementumAudio: sfx-ready clips=4`. Those only prove initialization. **No accepted visual confirmation** of real GPU water, sound quality, or budget GPU frame-time yet.
- Baseline emulator artifacts: https://github.com/Tomex777/FallingSandJava/actions/runs/39829042784
- Historical false confidence problem: passing gradle does **not** confirm usability. Inspect screenshots carefully; if text overlaps, the change fails ABR even when CI says otherwise.

---

## Agent 1 — GPU water appearance + efficient shader

**Branch:** `agents/elementum-a1-gpu-water-40m-20261022`

**Exclusive files:** `core/src/com/gdx/cellular/effects/FluidSurfaceRenderer.java`, GPU-only helper classes under `core/src/com/gdx/cellular/effects/fluid/`, dedicated GPU tests. Coordinate `CellularAutomaton` changes with Agent 3 before editing.

**40-minute work:**
- Inspect existing mask+shader: a CPU full-grid scan/pixmap upload still happens every frame; masked GPU pass only shades a bounding rectangle. Find actual high-cost paths, memory leaks, texture UV orientation, shader compilation and CPU/GPU sync costs.
- Preserve correct fluid bounds and top/bottom orientation, accurate water surface, polished but not cartoon/neon highlights. Improve smoothness, animation cadence, pooling, water alpha/reflection without introducing heavyweight particle-fluid solvers.
- Add hardware-aware quality modes internally (e.g. low-cost / richer) with **automatic default** and stable downgrade/fallback; **do not** introduce an unnecessary duplicate user quality slider or disable effects silently.
- Test moving/paused water with visually different pool heights and against adjacent solids; check edges on 320×568. Measure frame times using `ElementumPerf`, GPU-region logs and screenshots; do not claim performance based on shader instruction count alone.
- Deliver actual `gpu-water-*-api26.png` and `gpu-water-*-api36.png` screenshots and annotated observed defects. User approval remains pending.

**Acceptance:** Actual water visibly smoother than raw cells on supported GPU; no holes/inverted UV, no flicker, no blank water on fallback; finite texture allocations, safe surface recreation and no dramatic frame-time regressions.

## Agent 2 — ABR visual UI / typography / responsiveness

**Branch:** `agents/elementum-a2-abr-ui-40m-20261022`

**Exclusive files:** `core/src/com/gdx/cellular/ui/MobileControls.java` and related UI-only helpers and UI tests. Coordinate with Agent 4 on Sound button; existing sound control remains functional.

**40-minute work:**
- **Highest priority:** fix concrete oversized text/overlap visible in 39829042784 screenshots of Help, More, shelf and dock. Verify whether the 20px source-font correction is sufficient. Check all buttons remain readable and tappable, with padding and baseline alignment, not merely fit detection logs.
- Restore appearance to purposeful premium flat UI, no default Android controls, no duplicated tools; keep actual 2x horizontal shelf working. Show selected-material state clearly, no opaque sheet/shelf collision.
- Fix 360×800, 320×568 and one taller/wider screen. Test portrait rotation/lifecycle overlay dismissal and the picker scroll vs canvas painting.
- Save screenshot comparisons `a2-ui-{startup,shelf,more,help,picker}-api*.png`. Review `scene-close-in-bounds` and `help-content-fits` assertions *and* actual pixels.

**Acceptance:** No label bleeding/clipping/overlap; no obscured Close, Save/Load, selected material, bottom dock, or hidden buttons that intercept input. Correct two-row swipe navigation.

## Agent 3 — Material physics / correctness / smoothness

**Branch:** `agents/elementum-a3-physics-40m-20261022`

**Exclusive files:** `core/src/com/gdx/cellular/CellularMatrix.java`, `elements/**`, physics and reaction unit tests, optionally `CellularAutomaton.java` after Agent 1 agrees.

**40-minute work:**
- Maintain true cell-based simulation; separate dynamics from visual smoothing. Investigate water settling, liquid displacement, edge pooling, material temperature/reactions and meaningful splash movement. Incremental physically coherent fixes only; avoid a full rewrite without benchmarks.
- Audit Android worker-thread safety, grid chunk redraw and pause/resume. Preserve deterministic enough behavior for tests and save-state compatibility.
- Add unit/regression tests for water conservation/edge cases, solid/liquid interactions, heat, cooling and clear/load. Verify no expensive repeated Java object allocations in inner render/physics loop.
- If fluid renderer needs a new snapshot/hook, define a small stable API with Agent 1 rather than both editing the renderer.
- Record before/after actual physics screenshots and crash/perf evidence.

**Acceptance:** Water still obeys material collisions and conservation expectations, no runaway CPU spikes or broken initial/clear/save states, tests actually run.

## Agent 4 — Naturalistic sound, audio lifecycle + latency

**Branch:** `agents/elementum-a4-sound-40m-20261022`

**Exclusive files:** `core/src/com/gdx/cellular/audio/**`, `android/src/com/gdx/cellular/AndroidLauncher.java`, audio-specific tests; coordinate with Agent 2 before changing UI.

**40-minute work:**
- Audit generated `MaterialSoundscape` WAV: procedural sounds are placeholders, not actual recorded realistic water. Improve non-annoying dynamic volume/pitch/material mapping, splashing/continuous water vs one clip per stroke, repeated brush gestures without crackles. Avoid playing one sound every simulation cell/frame.
- Verify on Android 8 and 16 audio driver, app pause/resume and sound enable/disable persisted if feasible. If emulator uses `-noaudio`, do not claim listening QA; inspect log events and write reproducible manual real-device listening checklist.
- Keep async playback, bounded voice count, caching, memory safety, no external network/assets without rights.
- Add a focused test of WAV valid PCM duration/endianness and audio event rate limits where possible.
- Report exactly what is synthesized vs recorded.

**Acceptance:** No render-thread stalls, duplicate rapid blips, resource leaks, missing toggle, or misleading "realistic field recording" claims.

## Agent 5 — Integration, failure diagnosis, screenshot QA + release

**Branch:** `agents/elementum-a5-qa-release-40m-20261022`

**Exclusive files:** `.github/scripts/elementum-portrait-smoke.sh`, `.github/scripts/elementum-release-install-smoke.sh`, `.github/workflows/elementum-final-qa.yml`, `docs/qa/**`, isolated CI/test helpers.

**40-minute work:**
- Diagnose failed run 39829042784 **from logs and screenshot evidence**. Start with actual Help/More font failure. Re-run on the new PR head, identify exact first failing assertion; distinguish genuine UI defect from stale coordinate assumption.
- Make smoke robust but strict: assert visually useful help/materials/water; don't simply weaken assertions to get green. The recently appended clean-water fixture must actually place water after `pm clear` and produce an identifiable screenshot (the earlier fixture's Pause/+/Water taps are coordinate-sensitive).
- Ensure actual **running APK GPU water** screenshots at 360×800 + 320×568 on both API levels. Generate named artifact with commit SHA and FPS/frame-time/gfxinfo; retain source captures. Compare with original water visual.
- Build release, CI-sign and **install** actual APK on API26 and API36; ensure arm64-only and universal bundles and include failure screenshots/logcat. True production signing/physical phone remains explicitly unverified.
- Check PRs #8/#9 against their **current heads**; do not merge PR9 / approve water visuals until actual screenshot has been shown to the user.
- If tests are queued, work on independent test fixtures and screenshots available from last runs; don't sit idle.

**Acceptance:** Both Android versions compile/install, strict smoke run, truthfully recorded screenshots and performance, separate release install checks, documented blockers and no false claims.

---

## Shared time-stamped coordination + pickup register
At actual start, every agent posts a PR #9 conversation comment: `START <ISO8601 WAT> — Agent N; exact end = start + 40m; files claimed: ...`. At 40:00, add `STOP <ISO8601 WAT> — commits ... tests ... screenshot paths ... remaining ...`. Do not backdate.

**Pickup rule:** Check whether the other task owner has logged START and not STOP in PR #9 comments. If claimed and active, do not alter their files. If unclaimed/finished and a gap remains in own 40 minutes, post a PR #9 comment to claim a clearly bounded unfinished subtask and work under a separate commit, then hand back with screenshot evidence.

**Coordinator merge order:** Independent small PRs → inspect diffs and tests → cherry-pick/rebase to PR #9 without losing Agent 1/3 cross-file changes → rerun current-head API26 and API36 plus ARM64/release installation → provide **real water screenshots to user for approval** → only then consider landing the integrated update into `elementum`.
