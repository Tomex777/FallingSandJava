# Elementum Agent A — GPU Water / Budget Android

**Scope:** one-texel-per-cell water presentation only. This report is evidence of in-progress code review/implementation, not the acceptance of water visuals.

## Recorded actual session
- START: 2026-10-22T21:51:24+01:00 WAT, PR #9 coordination comment https://github.com/Tomex777/FallingSandJava/pull/9#issuecomment-6351036751.
- STOP actually recorded: **2026-10-22T22:31:45+01:00 WAT** in PR #9 comment https://github.com/Tomex777/FallingSandJava/pull/9#issuecomment-6351772435. Elapsed **40 minutes 21 seconds** after START, exceeding the exact 40:00 target by **21 seconds**. This was not backdated and should **not** be called exactly 40 minutes.
- Work branch: `agents/elementum-a1-gpu-water-40m-20261022`.
- Draft integration child PR: https://github.com/Tomex777/FallingSandJava/pull/15.

## Implemented changes (source)
- Exact previous-water occupancy cache. Update Pixmap only for changed water/empty cells; no redundant texture upload for a paused or stationary pool.
- Track **sparse** changed 32×32 tiles independently. Upload no more than three reusable edge-aware tile Pixmaps; fallback to one full-mask upload for scattered changes or when full upload is cheaper. Clear old occupied texels after water deletion and world reset.
- Retain **managed** PixmapTextureData as GL mask backing source to survive Android EGL re-creation with no guessed texture handle identity. Upload partial changes with OpenGL ES 2.0 `glTexSubImage2D`; LibGDX disallows `Texture.draw` on managed textures. The source Pixmap remains alive until renderer disposal.
- Preserve the correct original SpriteBatch flipped-V convention (bottom-vertex v; top-vertex v2) and add pure-Java geometry/JUnit regressions. Fix temporary misordered Pixmap 6-int drawPixmap call before accepting child PR.
- Low-cost shader detail on software/older GPUs or sustained poor frame pacing, with automatic per-cell fallback when GPU shader fails. Only surface water edges have moving glints; do not redraw whole interior specular wave.
- Added 5-second log window with actual CPU mask scan/upload average/peak times, frame water counts, texture uploads, transferred KiB, shaded region and FPS. These are **measurements available once running**, not already measured GPU speedups.
- Agent-local Actions workflow `.github/workflows/elementum-agent-a-water.yml` runs source-branch `core:test` and `android:assembleDebug`; integrated API26/API36 runs remain Agent E's responsibility.

## Source-independent tests actually performed
- Java-only matching geometry verifier on 20,833,250 cropped-world-row/tile-coordinate combinations: PASS. No Android/GPU used.
- Earlier randomized full-vs-partial texture-byte model: 1,750 frames across multiple sizes including complete clear: PASS.
- Expanded sparse-tile upload model (2026-10-22): **2,400 frames across 6 grid sizes; 65,534 changed texels; 819 full and 1,389 partial uploads; exact CPU source/GPU target byte equivalence every frame; simulated 292.132 MiB avoided transfer relative to indiscriminate full uploads.** Extended the same model to **24 distinct randomized seeds = 57,600 verified frames** with different edit positions. These are workload model results, **not real device upload bandwidth, GPU time or memory allocation proof**.
- New project JUnit tests validate geometry, reverse UV mapping, 32px edge tiles, dirty region coverage, distant droplets and sparse tile IDs. **Current-head CI result pending at report writing.**

## Historical real runtime QA inspected
- Run 39829042784 API36 artifact shows the SAME six Quickstep OS ANR overlay captures; shader enabled log alone is not water proof.
- API26 old artifacts show oversized text overlap, no source-stamped visible water at startup. Agent B owns responsive UI corrections; Agent E owns actual water visual screenshots.

## Must pass before this can be accepted
1. New source head `core:test`, Android APK compilation, actual install on API26 and API36 at 360×800 and 320×568; real water placement and still/flowing pixel screenshots, named source SHA/device driver.
2. Scene pause, save/load, clear/erase and Android lifecycle home/pause/resume with water retained and no blank shader output. If GPU unsupported, the cellular fallback must render correctly.
3. Record true FPS/frame-time/gfxinfo and mask scan/upload microseconds on software/representative low-end renderer; inspect actual visual appearance with user before considering approval.
4. Keep UI 2 horizontally swipeable stacked material rows and minSdk26; do not merge child draft PR directly to `elementum`.
5. **Exclude root `AGENT_TASK.md`** when combining different agents' branches to avoid ownership-log collisions.

## Handoff
Agent C explicitly owns concurrent simulation races/CellularAutomaton, Agent B owns MobileControls typography/shelf, Agent D owns material sound, Agent E owns independent source-stamped water-visual release smoke. PR #9 remains the canonical integrated target. No user water approval claimed.
