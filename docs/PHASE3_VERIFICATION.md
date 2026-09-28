# Phase 3 verification ledger

Final verification date: 2026-09-28 (user's local date). Status is evidence-based, not inferred from file presence.

## Executed checks

| Check | Result and scope |
|---|---|
| `validate_structure.py` | PASS: 21 modules, references, XML, wrapper integrity, permission policy, seven JVM test files and six instrumented test files |
| `check_asset_schema.py` | PASS: real host SQLite schema/reference/deletion constraints |
| Standalone compiler | PASS: math/core/assets/io/animation/physics/render/viewport compiled separately against Android 34 and actual JBox2D |
| Core smoke execution | PASS: six executable groups |
| Runtime smoke execution | PASS: ten executable groups against actual JBox2D |
| Full Gradle/Compose build | PASS at final code revision `053ec45`: debug app and test APKs compiled. Actual XML summary reports 31 JVM cases, zero failures and zero skipped. |
| Emulator tests | PASS at `053ec45`: 15 Android API 34 emulator cases, zero failures and zero skipped; app 6, assets 5, IO 3, physics 1. |
| Physical devices/performance | NOT RUN |

The source-only compiler diagnostic uses Kotlin 1.9.23 from the documented notebook compiler distribution. It is explicitly **not** the pinned Kotlin 1.9.24/Compose/Gradle app build. The full CI app build uses the pinned stack.

## CI evidence

- [36348507968](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36348507968): failed Android setup action; no app result claimed.
- [36348764239](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36348764239): real Gradle compilation exposed a nested Compose receiver access error; corrected by capturing the panel height in its owning scope.
- [36349040885](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36349040885), `916ee4d`: **compile-and-unit-test passed**, including debug APK and test APK assembly. Device job failed before test execution because its `sh` wrapper does not support `set -o pipefail`; the command now invokes Bash explicitly.
- [36349468726](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36349468726), `4e663af`: compile-and-unit-test passed; emulator outcome was not available at the last authenticated observation.
- [36349701716](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36349701716), `8c16575`: intermediate run; final outcome not retrieved.
- [36349870370](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36349870370), `3efe814`: **compile-and-unit-test passed**. XML notice: animation 5, core 15, math 3, physics 5, render 3; **31 total, zero failures, zero skipped**. Expanded emulator job `108706942591` was in progress at the last authenticated observation.
- **Final acceptance: [36350107741](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36350107741), `053ec459d4a06b22719130970669e12db50bf305`: SUCCESS.** Both `compile-and-unit-test` (job `108707019829`) and `device-tests` (job `108707744131`) completed successfully. Their actual XML-result annotations report:
  - JVM: animation 5, core 15, math 3, physics 5, render 3 — **31 passed, zero failures/skips**.
  - Android: app 6, assets 5, IO 3, physics 1 — **15 passed, zero failures/skips**.
  - Includes strengthened live-play step assertions, height-constrained joystick normalization, rotated split-camera framebuffer checks, GL lines, parallax, and the real Bitmap alpha test.

The earlier authentication interruption is resolved. Final acceptance is based on authenticated completed-job results and XML summaries, **not** the CLI watcher's exit code. All implementation/configuration/test files in the workspace were compared with the verified remote revision; only delivery documentation differed before the final documentation update.

### Genuine build artifacts

- [Debug APK, build log and JVM reports](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36350107741/artifacts/10942131622) — `android-build-and-unit-results`, 9,497,412-byte ZIP.
- [Android emulator reports](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36350107741/artifacts/10942730043) — `android-device-results`, 27,691-byte ZIP.

The debug APK is inside the first artifact under `app/build/outputs/apk/debug/`. GitHub authentication and artifact retention apply. The sandbox's earlier blob-host download attempt failed with EOF; no local APK was fabricated. Users can download the genuine artifact from GitHub. This is the editor debug APK, not a signed standalone-game export.

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

There are **31 authored JUnit Jupiter cases** in seven files and **15 authored Android cases** in six files. Execution counts must come from CI XML summaries, not from this inventory.

- Math: affine transforms and camera-independent coordinate math.
- Core: scenes/history/hierarchy, prefab safety, runtime serialization, input edges/disconnection, invalid controllers/animation/geometry checks.
- Animation: frame boundaries, long-frame event catch-up, reverse sprite events, interpolation, preset identity.
- Physics: actual contacts/grounding/query/raycast, all seven joints, compound shapes, tiny geometry rejection, hierarchy safety/hulls.
- Render: rotated camera mapping, viewport-aware limits, split views and transitions.
- Android assets: five real Bitmap/SQLite/import/split/reference cases.
- Android IO: three real persistence/recovery cases.
- Android physics: fitted alpha hull from a real transparent Bitmap.
- App: create/add/save flow, generated/split prefab asset flow, real EGL/GLES context recreation, rotated split-camera scissor/readback, parallax and GL line readback, and Play/Pause/Step/Stop without changing the saved scene bytes.

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
