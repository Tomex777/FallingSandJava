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

### Confirmed by source review: menu subcategories change only on pointer hover

- `CreatorMenu.createAccessSublistButton` switches the visible submenu in `enter`, which is a mouse hover callback. It has no click/touch handler.
- If the creator menu is opened from a touchscreen, tapping “Mouse Modes”, “Weather”, or “Body Type” does not select that submenu. This compounds the missing mobile entry point and must be exercised after adding touch access.

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

### Confirmed by source review: the creator menu is not drawn while paused

- The early return in the paused branch draws the simulation and mobile bar but skips `inputManager.drawMenu()`.
- If the creator menu is opened while paused, input is redirected to its stage but the menu itself is invisible until the simulation resumes.

### Confirmed on the CI device: status labels are positioned off-screen

- `InputManager` positions the status actor at fixed `screenHeight - 23` (777 px), while its `Stage()` uses a screen-sized viewport.
- The API 36 workflow device is 640×320 in landscape. The labels are absent in runs #44 and #45 because their y-position exceeds the viewport. This is device-size-specific and has not yet been checked on a taller handset profile.

### Confirmed: Android save/load prompts prefill “File Name” as actual text

- Save/load are bound only to hardware K/L keys and native text prompts in `InputManager`; there are no visible touch controls.
- Runs #47–48 captured the Android save dialog and verified a file is written after submitting it. The text field begins with `File Name`; typing `elementum_qa` without first deleting it creates `File_Nameelementum_qa.ser`.
- The save/load actions are also bound only to hardware K/L keys, with no visible touch control. The native prompts are reachable with a keyboard, so the file-prefix issue is separate from the touch-access gap.
- Fix the initial text so the prompt shows an empty field with “File Name” as a hint, and add visible touch access if it can be done without disturbing the existing quickbar layout.

### Confirmed by source review: loading saved particles or boids can crash

- `InputManager.save` serializes every cell by class name, including `Particle` and `Boid`.
- `InputManager.load` reconstructs each class through `ElementType.createElementByMatrix`, but the `PARTICLE` and `BOID` enum factories intentionally throw because these types require velocity/source data.
- Saving a scene that contains particles or boids can therefore make the later load throw on the game thread. A targeted runtime case is pending.

### Confirmed intermittent: mobile controls and material picker do not render

- Runs #43 and #49 captured no mobile controls at startup; tapping All in those runs did not open the picker. Runs #44, #45, #47, and #48 showed the controls and picker.
- Run #49 did not render the bottom bar or picker in any capture, though the app remained foregrounded. The Petrol interaction screenshot was consequently empty, so the Petrol/Lightning case needs a valid UI run.
- This repeats under the same API 36 emulator workflow without an app-code change. Treat it as an intermittent app initialization/viewport issue, not a single bad screenshot.

### Confirmed: loading a saved scene after pause/resume triggers an Android ANR

- Run #49 saved a populated scene, cleared it, and attempted to load it after a pause/resume cycle.
- `elementum-load-dialog.png` shows Android’s “Elementum isn’t responding” prompt. `logcat.txt` records an input dispatch timeout for `KEYCODE_L` and an ANR in `com.tomex.elementum`; the foreground process check alone missed it.
- `CellularAutomaton.render` disables `useChunks` on the paused path and never restores it. The ANR follows pause/resume in the same test; this is a likely contributor and needs a retest after restoring the previous stepping state.
- The workflow returned success because its smoke assertion checks only PID and fatal exceptions. Add an explicit ANR assertion and require successful save/load evidence.

## Other audit checks still pending

- Complete a populated save/clear/load round-trip using the current prompt's prefixed filename; after the fix, assert the clean filename and same restored scene.
- Verify save/load behavior when the grid contains a particle or boid.
- Repeat UI, pause/resume, save/load, and Petrol/Lightning interactions after fixing the intermittent controls and ANR; add assertions so the workflow cannot pass on an ANR or missing toolbar.
- Exercise each quick material and representative picker categories; verify brush decrement/increment; test clear/pause/resume behavior and chunk mode after resume.
- Exercise touch access to settings and lifecycle/background-resume behavior.
- Target Petrol ignition and Lightning propagation/conduction/expiry; check for crashes and scene corruption.
- Review the recorded source-level findings for scope and regressions before starting app fixes.

## Change log

- No app fixes have been made during this audit pass.
- The workflow change that added pause/clear/save-load evidence capture is test-only.
- The Lightning lifetime change is part of the existing baseline being audited, not a change made during this audit pass.
