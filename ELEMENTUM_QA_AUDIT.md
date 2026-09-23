# Elementum QA audit

Status: audit in progress. App fixes are intentionally deferred until the exploratory pass is complete.

## Scope

- Android app on the existing API 36 emulator workflow.
- Keep the existing particle appearance, movement presets, and UI layout intact.
- Exercise startup, material selection, drawing, brush controls, pause/resume, clear, save/load, modes, weather, Android lifecycle, and the Petrol/Lightning reactions.
- Record reproducible issues and evidence before changing app behavior.

## Findings so far

### Candidate: Android quick controls and picker do not render consistently

- Workflow run #42 (`dd33c75`) captured the quick-control bar at startup and the material picker after tapping All.
- Workflow run #43 (`289164f`) captured the simulation with no quick-control bar or picker in any of its three screenshots. The tap intended to open All appears to land on the simulation instead.
- The app process stayed foregrounded and the workflow passed because it checks only for a running PID and absence of `FATAL EXCEPTION`; it does not assert that controls appeared or that a material was selected.
- These runs differ in app code only by the Lightning lifetime. That change does not run during startup, so causality is not established. Repeat the same flow to determine whether this is an app rendering/input race or emulator flakiness.
- Severity/status: suspected user-facing rendering/input issue; not yet confirmed.

## Test gaps

- Existing CI smoke does not assert toolbar/picker visibility, selected material changes, pause state, clear result, or Petrol/Lightning outcomes.
- Lightning lifetime/branching has not been checked by a targeted emulator scenario.
- Save/load, mode changes, weather, brush resizing, and pause/background/resume have not yet been exercised end to end.

## Change log

- No app fixes have been made during this audit pass.
