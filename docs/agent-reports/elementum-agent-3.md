# Elementum Agent C / 3 — Physics and material-state audit

> **Status: STOPPED at the measured 40:00 boundary.** All entries are based on
> this session's actual work and GitHub commits, not on a future agent run.

## Session and coordination
- Start: **2026-10-22T21:50:37+01:00 (WAT)**, recorded in integration [PR #9 comment 6351025068](https://github.com/Tomex777/FallingSandJava/pull/9#issuecomment-6351025068).
- Measured STOP: **2026-10-22T22:30:37+01:00 (WAT)**; 40:00 elapsed. Actual boundary and GitHub handoff: [PR #9 comment 6351755239](https://github.com/Tomex777/FallingSandJava/pull/9#issuecomment-6351755239).
- Work branch: `agents/elementum-a3-physics-40m-20261022`.
- Draft child PR: [#10](https://github.com/Tomex777/FallingSandJava/pull/10), targeting `feature/elementum-abr-visual-audio-20261022`, *not* master.
- Scope: material dynamics and JVM regression tests. Agent A granted isolated `CellularAutomaton.java` stepping ownership in [PR #9 comment 6351340171](https://github.com/Tomex777/FallingSandJava/pull/9#issuecomment-6351340171). Agent C has **not** touched GPU renderer, UI, audio, or QA scripts.
- Checkpoints: collision/worker race audit [6351292574](https://github.com/Tomex777/FallingSandJava/pull/9#issuecomment-6351292574); cross-agent-safe fallback checkpoint [6351604232](https://github.com/Tomex777/FallingSandJava/pull/9#issuecomment-6351604232).

## Changes in PR #10
1. **World-edge conservation:** `Liquid.step` no longer deletes Water/Oil/Petrol/Lava/etc. on encountering the bottom or side border. Fluids settle at the last valid cell, damp side collisions, and can creep along a dry floor without crossing walls.
2. **Collision integrity:** horizontal liquid traversal cannot jump across a continuous layer of liquid. `Element.interactWith` stops collision movement if a material has already died/transformed during a reaction (preventing resurrection of stale Lava/Acid/etc.).
3. **Phase consistency:** partial heating first neutralizes accumulated cold, partial cooling first neutralizes accumulated warmth. Existing `Water` two-integer save state is preserved. Cooling Lava to Stone now transforms only the Lava cell, never surrounding unrelated liquid materials. Repeated cooling a dead lava object is ignored.
4. **CPU/GC:** replace per-contact HashMap/temporary ReactionKey lookups with direct enum-ordinal-indexed Reaction[][]; remove temporary liquid collision vectors; reuse two movement scratch vectors per simulation thread; eliminate some redundant coordinate vectors.
5. **Sleep/wake correctness:** exposed still-water edges keep their chunk active until the pool is enclosed/settled; enclosed calm fluid can sleep.
6. **Safer worker schedule:** default narrow Android worlds of 53, 60 and capped 120 columns (12 partitions) do not run adjacent-write-risk parity worker groups. They run physics serially. The former serial path drew the whole world twice per frame; serial stepping is now **simulation-only** and MatrixActor performs the only draw.
7. **Focused JUnit:** water floor containment, side splashes, sealed Stone tank (32 cells), oil boundary, thermal cycles and old save format, water↔steam and water↔ice phase chains, Lava/water isolation and stale-material resurrection, sleep/wake, partition clearance cases. `PhysicsTestGdx` sets a headless files stub before libGDX material colors initialize.

## Verification status — truthful release gates
- **Independent pure Java 8 check:** locally compiled `SimulationPartitions` with `javac --release 8`; six 320px/360px/720px and stripe boundary assertions passed. The thermal budget algorithm also passed 15,496 paired hot/cold conservation cases in an independent Python state model. **Neither is an integrated Gradle or Android test.**
- **JVM tests:** added and the focused GitHub Actions workflow `.github/workflows/elementum-agent-c-physics.yml` triggers for PR #10. Many superseded queued runs were auto-cancelled while development commits advanced. **Do not claim green until the current SHA's run finishes and JUnit reports are inspected.**
- **Android API26 + API36 install and visual tests:** **NOT VERIFIED by Agent C**. Canonical integrated visual/release evidence belongs to Agent E's integration QA, not source code review.
- **GPU water visual approval:** **NOT OBTAINED**. Actual user-visible APK water screenshots are required before merge/approval. Shader initialization logs are insufficient.
- **Frame-time / low-end regression:** **UNVERIFIED** until device/emulator avg/max simulation and total frame-time numbers are compared at the same fixture. The narrow-grid serial fallback prioritizes avoiding data races and one duplicate draw; neither that nor fewer allocations proves faster FPS.

## Known limitations and handoff
- The worker stripe check models **ordinary 8-cell lateral dispersion** and is necessary, not a formal guarantee of thread safety for high-speed impulses or manually chosen wider partitions. Default phone settings take the serial path; larger/overridden grids need a fully isolated/atomic stepping design before parity worker safety can be certified.
- Do not merge PR #10 just because the code compiles; block on current-head `:core:test`, real API26/API36 APK install, water movement screenshots, and performance comparison.
- If CI stays queued, record that status and link the **latest actual run**, not a cancelled superseded run.
- No real screenshot artifacts or frame-time data exist **yet for this agent's code**. Do not fabricate them.
