# Phase 2 acceptance checks

These steps are a **pending acceptance protocol**, not a record of completed Android/device tests. Use a disposable project first; asset source changes are not scene undo commands.

## Automated suites

```sh
python3 tools/validate_structure.py
python3 tools/check_asset_schema.py
./gradlew :engine-math:test :engine-core:test :app:assembleDebug
./gradlew :engine-assets:connectedDebugAndroidTest :engine-io:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

- `PrefabTest`: parent matrix/picking, cycles, nested expansion, identity preservation, property override refresh/revert, JSON round trip and capture of nested references.
- `AssetPipelineTest`: UUID move/rename/replacement, duplicate identity, revision retention, scene/prefab delete guards, bounded pixel operations, alpha/grid slicing, 335 recipes/idempotence, 17-layer car split, persistent frame ordering, ZIP entries/manifest, cycle rollback and tileable texture edges.
- `EditorSmokeTest`: foundation flow plus generate → browse → split car → use prefab → save. Requires functioning Compose/Android device execution.
- Existing project-save/recovery and GLES readback tests must continue to pass.
- `.github/workflows/android.yml` supplies a real build/unit job and API 34 emulator job. It has not been run in this session and no CI badge implies success.

The optional standalone diagnostic is documented in PHASE2.md. It compiles engine source and runs six actual core smoke checks; it cannot exercise Android bitmap/SQLite/GLES or Compose behavior.

## A. Backward compatibility and asset references

1. Back up a Phase 1 project with a copied image. Install the updated debug build over the same application ID without clearing storage.
2. Open it: the image should render and appear in Assets. Existing saved positions/names/UUIDs must remain.
3. Open Assets → select that image → Inspect/edit → Details. Rename to `Hero`, set folder `Actors/Players`, tags `player, test`, Favorite, Save metadata.
4. Search `player`, toggle Favorites and type filters, switch grid/list. Each control should change the actual result set.
5. Save the scene; force-stop and reopen. The same image must render. Record the asset UUID before and after the metadata move: unchanged.
6. Details → Replace image → confirm → choose a different local image. UUID remains the same, revision/path changes, all references now show new pixels. Existing world sprite dimensions remain. Any old sheet layout is cleared.
7. Duplicate: duplicate UUID differs. Rename/move/replace the duplicate; the original is unaffected.
8. Delete a referenced asset: expect an actionable refusal. Check both a saved scene reference and a prefab dependency. Remove unused entities, save, close/reopen to clear history, then delete only an actually unreferenced asset.

## B. Generate and split an actual modular vehicle

1. Create an Empty project. Assets → Generate 335 → confirm. Wait for completion; no network should be needed.
2. Search/filter must show 335 base asset records. Repeat Generate: base recipe UUIDs/count stay the same.
3. Search `Vehicle 1`, select the exact `Vehicle 1` record (not Vehicle 10), Inspect/edit → Sheet/split → Split into named prefab parts. Generated composite recipes use their original layers, not sheet crop regions.
4. The selected asset becomes `Vehicle 1 split`. Use in scene. Its hierarchy contains a group plus 17 parts: body, interior, windows, doors, four wheels, headlights, mirrors, bumpers and spoiler.
5. Select `Wheel front left` in the hierarchy. Change local X/Y, rotation and scale; Apply. Other wheels remain unchanged. Undo restores the edit in one step.
6. Select the parent group and change its position/rotation/scale through Inspector. Children follow using parent/local transforms; nonuniform parent scale plus child rotation must render/pick correctly.
7. Save, force-stop, reopen. Verify every node ID, parent reference, local property and asset UUID is unchanged.
8. Try Vehicle 2 (truck), Vehicle 3 (tank), Vehicle 4 (motorcycle), Building 2 (road), Building 3 (bridge), Building 4 (tower), and several Texture recipes. Generated counts are variants, not a claim of hand-crafted production art.

## C. Browser placement and small screens

1. From the embedded browser, long-press an image tile and drag it onto an empty viewport location. A real entity should appear at the camera-correct world point. Undo removes that placement; Redo restores it.
2. Repeat with a prefab. All its children should be placed under a new root with distinct instance IDs.
3. Pinch/pan the camera and repeat the drop; placement must follow the new view transform.
4. On a narrow phone, use Expand for a full-screen browser. The explicit Use action places at world origin; Return to scene closes the expanded browser.
5. Verify import/replace/export picker cancellation leaves the asset unchanged. Use a local source document while in airplane mode.

## D. Texture and sheet editing

1. Import a local image/sheet. Inspect → Texture. Test Crop, Resize, Rotate 90°, Flip X/Y, Tile 2×2 and Colorize. Each operation must create a new asset with a new UUID; original pixels/references stay intact.
2. Enter an invalid crop/size/color: expect an error, not silent correction or source overwrite.
3. For a 64×64 image, Sheet/split → set cells 32×32 → Grid slice: four regions in row-major order. Non-dividing dimensions must be rejected instead of silently dropping edge pixels.
4. Rename frames and move them up/down. Save named layout; close the inspector/project, reopen the asset: names and order persist.
5. Export frames ZIP. Inspect the document: `frames.json` plus `frame-1.png`, `frame-2.png`, etc. The JSON order and pixel crop sizes must match the editor.
6. Extract frames as PNG assets. Search their names; use/export a frame independently.
7. Import an image with two disconnected opaque islands. Auto alpha must detect two bounds. An opaque photograph normally gives one island; that is expected, not semantic segmentation.
8. Clear regions, drag manual rectangles on the preview, rename and reorder them, Split. The result must be a prefab with independent named crop children positioned to reconstruct the source bounds. Overlapping crop regions may duplicate visible content.

## E. Prefab reuse, nesting and overrides

1. Use a split prefab twice. Instance entity IDs differ; asset references are shared.
2. Change a child's name/transform in instance A. Inspector must list its overrides.
3. Change a different property in instance B; select B and Update prefab source → confirm.
4. Select A and Refresh instance → read/confirm the warning. Non-overridden source properties update; A's property overrides and matching entity IDs stay unchanged.
5. Revert overrides → confirm must restore source properties and be undoable as a scene edit. Root placement remains instance placement.
6. Create prefab from an existing instance root: a new wrapper definition must reference the original as a nested prefab rather than copying all its pixels. Use the wrapper and verify nested children remain editable.
7. Edit a nested child. Its override persists with the saved scene; updating the outer source must reject baking this nested override rather than silently lose it. Edit the nested asset as its own instance to modify its source.
8. Delete a local child, then Refresh: the confirmation explains that structural overrides are not supported. Cancel must leave the scene intact. Confirm reconstructs the source subtree; Undo restores the previous scene.
9. Try deleting an image or nested prefab referenced by the outer definition: deletion must be blocked.

## F. Failure, restart and storage

- Force-stop partway through procedural generation. Reopen and re-run: completed recipes are reused; missing ones are generated. The scene file is not replaced by generation.
- Import a corrupt/oversized image: error is shown; the existing catalog/scene still opens.
- Remove a referenced payload only in a disposable backed-up project: the renderer shows its missing-texture fallback and logs an error rather than pretending the texture loaded.
- For read-only/full-disk failures, failed operations must report errors. Batch splitting/extraction can leave already-created asset records; they must remain inspectable, not a fabricated success state.
- Back up while the app is stopped, including `project.json`, scenes, SQLite catalog and all Sprites/Prefabs payload revisions. Never back up only scene JSON if it references UUID assets.

## Reporting

Record device/API/GPU, build command and first error, relevant test report, failing step, and whether the project originated in Phase 1. Phase 2 is accepted only after the full build and the asset/split/save-reopen workflows pass. Do not begin Phase 3 on the assumption that standalone compilation proves Android UI correctness.
