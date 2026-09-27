# Phase 3 verification ledger

Delivery date: 2026-09-27 (user's local date). Status is evidence-based, not inferred from file presence.

## Executed checks

| Check | Result and scope |
|---|---|
| `validate_structure.py` | PASS: 21 modules, references, XML, wrapper integrity, permission policy, seven JVM test files and six instrumented test files |
| `check_asset_schema.py` | PASS: real host SQLite schema/reference/deletion constraints |
| Standalone compiler | PASS: math/core/assets/io/animation/physics/render/viewport compiled separately against Android 34 and actual JBox2D |
| Core smoke execution | PASS: six executable groups |
| Runtime smoke execution | PASS: ten executable groups against actual JBox2D |
| Full Gradle/Compose build | PASS at `916ee4d`: debug app and all requested test APKs compiled; JVM tasks passed. Latest hardening revision is undergoing the same CI gate. |
| Emulator tests | Pending final rerun; the earlier job failed in its shell wrapper before Gradle tests ran |
| Physical devices/performance | NOT RUN |

The source-only compiler diagnostic uses Kotlin 1.9.23 from the documented notebook compiler distribution. It is explicitly **not** the pinned Kotlin 1.9.24/Compose/Gradle app build. The full CI app build uses the pinned stack.

## CI evidence

- [36348507968](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36348507968): failed Android setup action; no app result claimed.
- [36348764239](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36348764239): real Gradle compilation exposed a nested Compose receiver access error; corrected by capturing the panel height in its owning scope.
- [36349040885](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36349040885), `916ee4d`: **compile-and-unit-test passed**, including debug APK and test APK assembly. Device job failed before test execution because its `sh` wrapper does not support `set -o pipefail`; the command now invokes Bash explicitly.
- The latest runtime safety/event changes are being rerun before final handoff. Final results replace this provisional row, rather than assuming an older green build covers later code.

Successful CI builds upload `android-build-and-unit-results` with the genuine debug APK and test reports. The sandbox could not download GitHub's blob-hosted artifact/log redirect (EOF); no local APK file is fabricated. Artifacts can be downloaded from the run page by the repository user.

## Runtime smoke groups that passed

1. 65 valid presets, stable unique IDs.
2. Once/loop/ping-pong event timing and seeking.
3. Transform/color/sprite sampling and JSON round-trip.
4. Mixed input sources, opposing keys, axis dead zones and consumable edges.
5. A real falling rigid body settles, reports grounding/contact, and supports ray/AABB queries.
6. Real sensor enter/exit with no collision response.
7. Seven actual joint implementations step; circles, polygons, capsules and hull construction work.
8. Fixed-step preview repeatability, grounded jump, and authored-scene isolation.
9. Rotated coordinate round-trip, dead zone/limits, split cameras and smooth transitions.
10. Prefab animation resources/internal joint identity survive instantiate/refresh.

## Conventional test inventory

There are **30 authored JUnit Jupiter cases** in seven files and **13 authored Android cases** in six files. Execution counts must come from CI XML summaries, not from this inventory.

- Math: affine transforms and camera-independent coordinate math.
- Core: scenes/history/hierarchy, prefab safety, runtime serialization, input edges/disconnection, invalid controllers/animation/geometry checks.
- Animation: frame boundaries, long-frame event catch-up, reverse sprite events, interpolation, preset identity.
- Physics: actual contacts/grounding/query/raycast, all seven joints, compound shapes, tiny geometry rejection, hierarchy safety/hulls.
- Render: rotated camera mapping, viewport-aware limits, split views and transitions.
- Android assets: five real Bitmap/SQLite/import/split/reference cases.
- Android IO: three real persistence/recovery cases.
- Android physics: fitted alpha hull from a real transparent Bitmap.
- App: create/add/save flow, generated/split prefab asset flow, real EGL/GLES pixel readback, and Play/Pause/Step/Stop without changing the saved scene bytes.

## Manual device acceptance still required

1. On a phone and a tablet, create/move/save/close/reopen a scene; verify orientation changes and process recovery, not just same-process navigation.
2. Add the test rig, watch contacts/pendulum, cross the sensor, jump only when grounded, and inspect collision lines. Compare authored transforms before/after Stop and relaunch.
3. Hold joystick + Jump simultaneously. Release outside a button, cancel a gesture, background, disconnect a gamepad and change focus; verify no stuck action.
4. Bind keyboard/gamepad buttons, mouse buttons and gamepad axes; verify signs/dead zones and both press/release behavior on actual hardware.
5. Author frame keys, FPS/loop/ping-pong, seek, trigger and sprite events. Verify reverse events at exact boundaries; verify edits undo and survive reopen.
6. Add/tune every joint's applicable parameters, test motors/limits, edit compound/per-part colliders and confirm undoable deletion/reference cleanup.
7. Generate alpha colliders from transparent assets, inspect approximation/shrinkage, and draw 3–8 convex-hull points. Ensure invalid geometry produces an error without scene loss.
8. Configure rotated follow cameras, limits, dead zones, split views, transitions, all six shake presets and different parallax factors. Check actual GLES clipping and framebuffer edges.
9. Exercise Play/Pause/Step/Stop through lifecycle changes and orientation; confirm no simulation writes to project/autosave files.
10. Measure memory/frame time on representative low/mid-range devices. No 60-fps-at-limit claim is made without these measurements.
