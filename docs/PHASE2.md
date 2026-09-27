# Phase 2 — Asset system and sprite pipeline

## 1. Architecture decisions

This delivery implements Phase 2 workflows on top of the existing foundation. It does **not** combine Phase 3 into the same unverified release. The original stop/confirm phase gate remains in force.

### Storage and identity

`engine-assets` owns a per-project SQLite catalog at `Resources/assets.sqlite`. Stable UUIDs are stored in `SpriteComponent.assetId`. Names, virtual folders, tags and favorite status are metadata; renaming/moving them does not rewrite scene references. A replacement preserves the UUID but publishes a new immutable PNG revision. Existing world-space sprite dimensions remain unchanged.

Payload files are written/fsynced before database transactions publish them. Foreign-key dependency edges protect images/nested definitions referenced by prefabs. Deletion additionally checks in-memory/current/saved/recovery/undo scenes and on-disk Scenes/autosave JSON, failing closed if those files cannot be read. Tombstones prevent legacy Phase 1 PNGs being reindexed after deletion. Old revisions remain on disk: this favors safety over automatic space reclamation.

Phase 1 UUID PNGs are indexed without rewriting their scene JSON. The renderer can resolve those legacy UUID paths through the new catalog, including after image replacement. App-private scoped storage and system document-picker import remain unchanged.

Sheet layouts are stored in SQLite with ordered frame names/rectangles. Explicit Save named layout persists them; extraction and ZIP export also save them. Replacing an image clears its layout to avoid stale pixel coordinates.

### Engine and editor boundaries

- `engine-core`: parent/local transforms; affine hierarchy matrices including shear; root/subtree operations; nested prefab definitions, stable source paths and instance UUIDs; property overrides for name/transform/sprite; cycle/depth/size validation.
- `engine-assets`: catalog/files, bounded image decoding/operations, slicers, original procedural generator, prefab persistence and dependency graph.
- `editor-assets`: Compose browser, previews via Coil, inspectors and sheet/texture tools. `AssetActions` keeps it independent of `editor-ui`.
- `editor-ui`: action orchestration and editor state. Disk operations dispatch to IO; prefab expansion dispatches to Default.
- `engine-render`/`editor-viewport`: parent-aware drawing/picking/dragging; UUID-to-revision resolution; texture cache invalidation on catalog path changes; native Android drag/drop into the GL viewport.

### Implemented UI workflows

| Area | Actions |
|---|---|
| Browser | Grid/list, search over name/folder/tags, favorites toggle, image/prefab type filter, file previews, import, generate, select, use, inspect, expand |
| Placement | Long-press drag from embedded browser to viewport; explicit Use from either browser mode |
| Asset inspector | Name/folder/tags/favorite editing, dimensions/UUID/path/revision, duplicate, replace preserving UUID, create prefab, protected catalog deletion, PNG export |
| Texture editor | Crop, nearest-neighbor resize, 90° rotate, flip X/Y, 2×2 tiling, RGBA multiply/colorize; each creates a new image |
| Sheet editor | Manual preview rectangles, exact-dividing grid, four-connected alpha islands, frame rename/reorder/remove, saved layouts, frame extraction, ordered PNG+JSON ZIP export |
| Modular split | Original transparent layers for generated composites; named rectangular crops for arbitrary imported images; output is a prefab hierarchy |
| Scene inspector | Create prefab from selection, source update with confirmation, instance refresh preserving overrides, revert overrides with confirmation; all entity properties remain editable |

### Procedural assets

Generate 335 creates actual 64×64 PNGs from 335 unique recipes: 30 characters, 30 enemies, 20 NPCs, 20 animals, 25 vehicles, 40 buildings, 30 nature objects, 40 props and 100 tileable textures. Vehicle variants include cars, trucks, tanks and motorcycles; building variants include houses, towers, roads and bridges. Colors/geometries vary deterministically.

These are intentionally **simple geometric pixel-art variants**, not a polished hand-authored character/game pack. No third-party artwork is bundled. Only original generated outputs are dedicated under CC0; imported assets are excluded. A generated-output license notice is written into the project. Re-running skips existing recipe records; derived/duplicated/replaced assets can make the total catalog larger than 335.

Cars split into named body/interior/windows/doors/four wheels/lights/mirrors/bumpers/spoiler layers. No dummy collision component is added: collision is Phase 3. Generic image splitting does not infer hidden or occluded semantic parts.

## 2. File tree

See [PHASE2_FILES.md](PHASE2_FILES.md) for the exact new/changed/unchanged inventory relative to the previous turn, including docs and build files.

Principal new implementation files:

- `engine-core/src/main/kotlin/world/engine/core/Prefab.kt`
- `engine-assets/src/main/kotlin/world/engine/assets/AssetDatabase.kt`
- `engine-assets/src/main/kotlin/world/engine/assets/AssetRepository.kt`
- `engine-assets/src/main/kotlin/world/engine/assets/ImageOps.kt`
- `engine-assets/src/main/kotlin/world/engine/assets/SpriteSlicer.kt`
- `engine-assets/src/main/kotlin/world/engine/assets/ProceduralAssets.kt`
- `editor-assets/src/main/kotlin/world/engine/asseteditor/AssetActions.kt`
- `editor-assets/src/main/kotlin/world/engine/asseteditor/AssetBrowser.kt`
- `editor-assets/src/main/kotlin/world/engine/asseteditor/AssetInspector.kt`
- `engine-core/src/test/kotlin/world/engine/core/PrefabTest.kt`
- `engine-assets/src/androidTest/kotlin/world/engine/assets/AssetPipelineTest.kt`
- `tools/CoreSmoke.kt`, `tools/check_engine_standalone.py`, `tools/check_asset_schema.py`
- `.github/workflows/android.yml`

Existing math/core/IO/render/viewport/editor sources, editor-assets/engine-assets build files, editor-ui dependency wiring, Compose smoke test and validation script are updated. Phase 3 modules remain reserved and unchanged.

## 3. Complete source

[PHASE2_SOURCE.md](PHASE2_SOURCE.md) contains every new/changed text file in full, except that appendix itself. Source is also directly editable in the repository. Existing historical Phase 1 source snapshots are preserved, not silently rewritten.

## 4. Gradle configuration

The existing Gradle 8.7 / AGP 8.5.2 / Kotlin 1.9.24 / Java 17 / SDK 34 setup remains.

- `engine-assets`: Android library; Kotlin serialization plugin; API dependency on engine-core; serialization JSON 1.6.3; coroutines Android 1.8.1; Android test runner and JUnit extension.
- `editor-assets`: Android Compose library, Compose compiler 1.5.14, BOM 2024.06.00; API dependency on engine-assets; Material 3, activity-compose 1.9.1, Coil Compose 2.6.0.
- `editor-ui`: API dependency on editor-assets.
- No Phase 3 JBox2D/animation/gameplay dependencies or controls are added prematurely.

## 5. Build & run

Open the repo in Android Studio Koala or newer. Configure JDK 17 and Android SDK 34/Build Tools 34.0.0. Sync, select app and Run on GLES 3-capable Android 7+ hardware/emulator.

```sh
./gradlew :engine-math:test :engine-core:test :app:assembleDebug
./gradlew :app:installDebug
./gradlew :engine-assets:connectedDebugAndroidTest :engine-io:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Successful builds output the real debug editor APK under `app/build/outputs/apk/debug/`. No editor APK or exported game APK is fabricated or supplied here. The CI workflow performs unit/build checks and then emulator tests once run on GitHub; it has not been executed in this session.

### Optional standalone diagnostic

This reproduces the narrower engine checks when Gradle distribution access is blocked. It does not compile Compose or package an Android app.

```sh
python3 -m venv .venv
.venv/bin/pip install jdk4py==17.0.9.2 kotlin-jupyter-kernel==0.12.0.322
.venv/bin/python tools/check_engine_standalone.py --android-jar "$ANDROID_HOME/platforms/android-34/android.jar"
python3 tools/check_asset_schema.py
python3 tools/validate_structure.py
```

The diagnostic uses the genuine Kotlin 1.9.23 compiler/serialization plugin bundled with the pinned notebook package, serialization runtime 1.6.3, an Android API JAR, and its bundled coroutines classes. It does not prove compatibility with all pinned app dependencies. The sandbox obtained the API JAR from the Sable Android-platform mirror via configured GitHub access; a normal SDK installation should supply it instead. Downloaded validation tools are outside Git and are not required by the app.

## 6. Verification tests

Actual results in this session:

| Check | Result |
|---|---|
| Standalone Kotlin compile: math/core/assets/IO/render/viewport + serialization | PASS (diagnostic Kotlin 1.9.23, Android API 34) |
| Six executable core checks | PASS: affine inverse, hierarchy picking, nested refresh/overrides, stable identities, cycle rejection, JSON/legacy refs, history |
| Host SQLite production-schema checks | PASS |
| Repository/module/XML/wrapper integrity checks | PASS |
| Gradle wrapper distribution bootstrap with Java 17 runtime | BLOCKED: SSL handshake terminated by remote/network path |
| Full Gradle/Compose application build | NOT RUN successfully |
| JUnit and Android bitmap/SQLite/Compose/GL tests | NOT RUN |
| CI jobs and APK installation | NOT RUN |

The failed UI compiler diagnostic lacked Compose/AndroidX dependencies; it is **not** counted as a successful compile. No physical-device gesture or rendering claims are made.

See [PHASE2_VERIFICATION.md](PHASE2_VERIFICATION.md) for automated suites and reproducible device checks.

## 7. Known limitations

1. Full Android app build/device acceptance is still open. This is an implemented source delivery, **not a “fully built and verified” Phase 2 or Phase 3**.
2. The editor still has one Main scene per project. Core prefab definitions can instantiate into multiple Scene values; a multi-scene editor remains later work.
3. Prefab refresh/source application is explicit, not live propagation. Property overrides cover name, whole transform and sprite component. Deleted/added child entities are not structural overrides: refresh reconstructs the definition subtree and may restore deleted children or discard additions. An explicit confirmation now warns before that refresh.
4. Nested instances expand with unique stable paths and support scene-local property overrides. Baking nested-child overrides into an outer prefab is rejected with an explanatory error; edit the nested asset as a separate instance instead. Arbitrary visual prefab-tree authoring/reparenting is not implemented.
5. There is no multi-selection/grouping tool. “Create prefab from selection” captures one selected subtree; split composites naturally provide grouped hierarchies. Selecting an existing instance creates a nested wrapper prefab.
6. Manual regions are rectangular; alpha slicing detects connected visible islands. There is no semantic image segmentation or reconstruction of occluded parts. Generated originals have genuine transparent component layers; generic crops may contain overlapping visible content.
7. Sheets export ordered PNG files plus `frames.json`, not a repacked atlas. Unsaved frame-layout edits are session state until Save layout, extraction or export. Texture edits make new assets; a replacement clears the old sheet layout and preserves world sprite sizes. EXIF orientation is not automatically applied; use a rotated copy if necessary.
8. Import copies at most 16 MiB encoded data and downsamples to at most 2048 pixels per dimension. Generated assets are 64×64. Up to 256 regions, 5000 scene entities, hierarchy depth 64 and nested prefab depth 16 are accepted. GL textures retain the Phase 1 64 MiB budget.
9. Catalog folders are virtual, not arbitrary physical moves. Asset operations are not in the scene undo stack. Revision rollback UI, physical garbage collection, transactional bulk library/split undo and portable prefab ZIP export are not implemented. Interrupted batch operations can leave usable partial assets or unreferenced payload files; generation can resume.
10. Corrupt catalog/definition files produce errors; automatic database repair is not implemented. Back up the complete project, including SQLite and payload files, while the app is stopped. Corrupt unrelated definitions can currently block prefab expansion because definitions are loaded together.
11. Generated art is a functional procedural library, not production-quality hand-painted art. Counts do not imply unique behavior, rigging, collisions or animation clips.
12. Expanded asset browsing uses Use to place assets; drag into the viewport uses the embedded browser. On very small screens, Expand gives a usable full-screen grid. Device-specific drag/drop and nested scrolling still need testing.
13. Phase 1 limitations unrelated to this phase remain: private project storage, no universal no-crash guarantee, best-effort autosave, no APK game build pipeline.

## 8. Next phase preview + confirmation gate

Phase 3 will implement animation playback/timelines, JBox2D bodies/shapes/joints and collision authoring, input mapping/devices/virtual controls, and camera behaviors. None of these is claimed in this delivery.

**Please run the Phase 2 build and acceptance checks and confirm the result before authorizing Phase 3. If any build or test fails, share the first error and we should fix it before adding more engine systems.**
