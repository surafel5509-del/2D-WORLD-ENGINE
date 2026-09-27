# Phase 1 verification protocol

Status: tests are provided but Android build/device runs are pending. Run on an isolated test device/emulator; the UI smoke test creates a MyGame project each run and does not erase user data.

## Automated tests

```sh
python3 tools/validate_structure.py
./gradlew :engine-math:test :engine-core:test
./gradlew :app:assembleDebug
./gradlew :engine-io:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

- Math: vector arithmetic, affine inverse round trip, identity multiplication, rectangle boundaries.
- Core: complete JSON round trip with stable IDs and texture reference, duplicate IDs/path traversal/unsupported version rejection, transformed reverse-painter picking, history branching/no-op/capacity, component queries.
- Storage (instrumented): create/save/reinstantiate store/reopen equality, recovery distinct from explicit save, discard, save replacing recovery, AtomicFile backup repair, malformed recovery fallback.
- Compose (instrumented): launch dashboard, wizard, add three sprites, save, return to dashboard. This is a smoke test, not a touch-gesture or process-death test.
- GL (instrumented): create real GLES 3 pbuffer, run the production renderer, read back a red sprite pixel, check GL errors; repeat with a new context. This does not substitute for viewport lifecycle tests on physical GPUs.

JUnit reports: each tested module's `build/reports/tests/test/` for JVM tests; Android instrumented reports under `build/reports/androidTests/connected/` (AGP may include device/flavor subdirectories).

## Acceptance A — exact save/reopen

1. Launch 2D WORLD. New project → name **MyGame**, package **com.example.mygame**, Landscape, Empty → Create.
2. Tap **Add sprite** three times. Overlapping sprites are expected initially. In Hierarchy select each entity and use Inspector → Apply properties:
   - Sprite 1: X `-140`, Y `80`, Rotation `15`, Scale X `1.2`, Scale Y `1`.
   - Sprite 2: X `0`, Y `-50`, Rotation `0`, Scale X `1`, Scale Y `1`.
   - Sprite 3: X `150`, Y `90`, Rotation `-20`, Scale X `0.8`, Scale Y `1.3`.
3. Drag each sprite to a new position. Record the resulting numeric properties and stable ID from Inspector. Select a hierarchy entry to disambiguate overlaps.
4. Tap Save. Wait for `MyGame • saved` and console `Scene saved`.
5. Pull the scene JSON as described below; record its SHA-256 or keep a copy.
6. Force-stop the editor (Settings → Apps → 2D WORLD → Force stop, or the adb command below). Do not clear storage.
7. Relaunch, tap MyGame. All three sprites must have identical IDs, positions, rotations, scales, names and ordering. Compare JSON before/after: it must be byte-for-byte equal if no new edit/save was made.
8. Reset view if needed; camera/selection are not saved scene properties.

```sh
adb shell am force-stop world.engine.editor
adb shell am start -n world.engine.editor/world.engine.app.MainActivity
adb shell run-as world.engine.editor ls files/2DWorldProjects
# Replace PROJECT_UUID with the folder printed by the preceding command:
adb exec-out run-as world.engine.editor cat files/2DWorldProjects/PROJECT_UUID/Scenes/Main.json > scene.json
# Full debug backup; store outside the repo if it contains large user images:
adb exec-out run-as world.engine.editor tar -cf - files/2DWorldProjects > projects-backup.tar
```

`run-as` requires the debuggable development APK; it is not a production sharing feature.

## Acceptance B — history and gestures

1. Move a sprite in one continuous drag; Undo must restore its pre-drag position in one step; Redo must restore its final position.
2. Undo then make a different edit: Redo must be disabled.
3. Tap empty space: selection clears. Drag empty space: camera pans without editing the scene.
4. Pinch: zoom must remain anchored near the gesture focus, within 0.15–8×. Adding a second finger ends any active move command.
5. Long press a sprite: delete confirmation opens. Cancel leaves the entity intact; Delete removes it; Undo restores it with the same ID.
6. Apply a negative scale to mirror a sprite, a rotation, and fractional positions; picking must still work in its transformed bounds.
7. Enter invalid text/zero scale in Inspector. Apply must show an error without altering the scene.
8. Rotate the device and background/foreground it. Scene edits must survive activity recreation; renderer must recreate textures when its GL context is lost. Repeat with “Don't keep activities” enabled on a test device.

## Acceptance C — autosave and crash recovery

1. Save a baseline scene. Move a sprite, then wait until Console says **Recovery snapshot saved** (30 seconds maximum between autosave checks while the app is active).
2. Force-stop without saving; relaunch and open the project.
3. Recovery prompt must appear. Restore must recover the move and show dirty state; Save must make it durable and remove the snapshot.
4. Repeat but choose Keep saved: the baseline must remain; no prompt should appear on the next open.
5. Confirm explicit Save immediately followed by force-stop does not resurrect an older autosave.
6. With the app stopped, write invalid JSON into `.autosave/Main.json` via `run-as` on a disposable test project. Reopen: explain the corrupt snapshot and allow **Keep saved**; do not hide or overwrite the saved scene.
7. Corrupt `Scenes/Main.json` only in a backed-up disposable test project. Opening must show an error and not overwrite the file. Restore your backup manually afterward.

## Acceptance D — imported assets and offline behavior

1. Choose Import image and select a locally stored PNG/JPEG/WebP from the system picker; cancel once to verify no entity is added.
2. Import successfully, move the image, save, force-stop, reopen. Its copied PNG reference must still resolve even after the original source file is moved or removed.
3. Enable airplane mode, create a project and repeat add/move/save/reopen. No network should be required. Cloud document providers themselves may need connectivity; choose a local image.
4. On a disposable project remove a referenced copied PNG using `run-as`; reopen. A magenta/dark checkerboard and actionable console error must appear, not a silent white replacement or a crash.
5. Test portrait phone and landscape tablet layouts. Toolbar scrolls on narrow screens; Viewport tab hides lower panels; wide screens show hierarchy and inspector at the sides.

## Report format

Record Android version, device/GPU or emulator configuration, orientation, build command/output, test reports and the first failing step. Include `adb logcat` output for a crash. Do not claim Phase 1 accepted until build, installation and Acceptance A pass; run the remaining checks before relying on recovery or imported assets for real work.
