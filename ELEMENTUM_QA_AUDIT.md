# Elementum QA audit

Status: exploratory audit in progress. App fixes remain deferred until this pass is complete.

## Scope

- Android app on the existing API 36 emulator workflow.
- Preserve the existing particle appearance, movement presets, and UI layout.
- Exercise startup, material selection, drawing, brush controls, pause/resume, clear, save/load, modes, weather, Android lifecycle, and the Petrol/Lightning reactions.
- Record reproducible issues and evidence before changing app behavior.

## Findings recorded before fixes

### Confirmed: brush decrement control has no visible label

- `MobileControls` uses U+2212 (`−`). The bundled bitmap font does not render it in the Android emulator; the gray decrement button appears blank in runs #44 and #45, while `+` renders.
- The action still runs when tapped, but users cannot tell what the blank button does.

### Confirmed: touch users cannot open the creator settings menu

- `CreatorInputProcessor.touchDown` opens the creator menu only for `Input.Buttons.RIGHT`.
- On the Android touchscreen flow, taps are left-button input and there is no alternate mobile control for mouse mode, weather, or Box2D body type. The mobile UI offers material selection, brush size, pause, and clear only.
- The Android menu path is therefore unavailable from touch; the menu remains available through a right-click capable pointer.

### Confirmed: heat propagation skips matrix cell (0, 0), not the source cell

- `Element.applyHeatToNeighborsIfIgnited` loops over the source's neighborhood but excludes only `x == 0 && y == 0`.
- This includes the source cell and omits the world-origin cell for every other source position. The exclusion must be based on the current element's own matrix coordinates.

### Confirmed: explosion input also applies heat

- In `InputManager.spawnElementByInput`, `MouseMode.EXPLOSION` falls through into `MouseMode.HEAT` because the explosion case has no `break` or `return` after adding the explosion.
- Selecting the explosion mode also performs the heat-brush action at the cursor.

### Confirmed: rectangle brush omits its final row

- `InputManager.spawnRectangle` loops `y < yEnd` while the x loop includes its endpoint.
- A one-cell-tall rectangle spawns no cells; taller rectangles omit the top/end row.

### Confirmed: resuming after pause leaves chunk stepping disabled

- The paused branch in `CellularAutomaton.render` assigns `useChunks = false` before returning.
- The next resumed frame uses that same false value, with no path restoring the previous setting. Pause/resume therefore permanently disables chunk stepping for the rest of the app session, increasing work and changing scheduling.
- Run #45 confirmed the pause control changes to “Play”; the performance/state consequence is established by source inspection and still needs a post-resume runtime assertion.

### Confirmed on the CI device: status labels are positioned off-screen

- `InputManager` positions the status actor at fixed `screenHeight - 23` (777 px), while its `Stage()` uses a screen-sized viewport.
- The API 36 workflow device is 640×320 in landscape. The labels are absent in runs #44 and #45 because their y-position exceeds the viewport. This is device-size-specific and has not yet been checked on a taller handset profile.

### Candidate: save/load is not usable from the Android touch UI

- Save/load are bound only to hardware K/L keys and native text prompts in `InputManager`; there are no visible touch controls.
- The run #45 key-injection sequence produced no file in `files/save`, and the subsequent load did not restore a scene. The captured final window remained the game activity, but the workflow did not capture the dialog itself, so this is not yet evidence of a save implementation failure. It is still a confirmed touch-access gap and the key flow needs a better targeted check.

### Confirmed by source review: loading saved particles or boids can crash

- `InputManager.save` serializes every cell by class name, including `Particle` and `Boid`.
- `InputManager.load` reconstructs each class through `ElementType.createElementByMatrix`, but the `PARTICLE` and `BOID` enum factories intentionally throw because these types require velocity/source data.
- Saving a scene that contains particles or boids can therefore make the later load throw on the game thread. A targeted runtime case is pending.

### Closed as not reproduced: missing mobile controls/picker capture

- Run #43 captured none of the mobile controls. Runs #44 and #45 showed the controls at startup and the material picker after tapping All, so the missing-control result has not reproduced and is treated as a one-off emulator capture discrepancy for now.
- The original smoke test checked process survival only. The test-only workflow extension now captures picker, interaction, pause, clear, and load states; these screenshots are evidence, not assertions yet.

## Other audit checks still pending

- Verify save/load by capturing the text prompt and checking the saved filename/content and restored material grid.
- Verify save/load behavior when the grid contains a particle or boid.
- Exercise each quick material and representative picker categories; verify brush decrement/increment; test clear/pause/resume behavior and chunk mode after resume.
- Exercise touch access to settings and lifecycle/background-resume behavior.
- Target Petrol ignition and Lightning propagation/conduction/expiry; check for crashes and scene corruption.
- Review the recorded source-level findings for scope and regressions before starting app fixes.

## Change log

- No app fixes have been made during this audit pass.
- The workflow change that added pause/clear/save-load evidence capture is test-only.
- The Lightning lifetime change is part of the existing baseline being audited, not a change made during this audit pass.
