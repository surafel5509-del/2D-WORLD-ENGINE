# Phase 2 — complete new/changed file listing

Full source snapshot for this delivery. Files on disk are authoritative. Includes every new/changed text file except this self-referential appendix. No Phase 3 implementation is included. See PHASE2.md for verification status and limitations.

## .github/workflows/android.yml

`````yaml
name: Android foundation and assets
on:
  workflow_dispatch:
  pull_request:
  push:
    branches: [arena/01a0e43a-2d-world-engine]
permissions:
  contents: read
jobs:
  compile-and-unit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: android-actions/setup-android@v3
      - uses: gradle/actions/setup-gradle@v4
      - run: sdkmanager 'platforms;android-34' 'build-tools;34.0.0'
      - run: python3 tools/validate_structure.py
      - run: ./gradlew :engine-math:test :engine-core:test :app:assembleDebug :app:assembleDebugAndroidTest :engine-assets:assembleDebugAndroidTest :engine-io:assembleDebugAndroidTest
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: android-build-and-unit-results
          path: |
            app/build/outputs/apk/debug/*.apk
            **/build/reports/tests/
  device-tests:
    runs-on: ubuntu-latest
    needs: compile-and-unit-test
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: android-actions/setup-android@v3
      - uses: gradle/actions/setup-gradle@v4
      - name: Enable KVM
        run: |
          echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' | sudo tee /etc/udev/rules.d/99-kvm4all.rules
          sudo udevadm control --reload-rules
          sudo udevadm trigger --name-match=kvm
      - uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          arch: x86_64
          profile: pixel_2
          disable-animations: true
          emulator-options: -no-window -gpu swiftshader_indirect -no-snapshot -noaudio -no-boot-anim
          script: ./gradlew :engine-assets:connectedDebugAndroidTest :engine-io:connectedDebugAndroidTest :app:connectedDebugAndroidTest
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: android-device-results
          path: '**/build/reports/androidTests/'

`````

## .gitignore

`````text
*.iml
.gradle/
**/build/
local.properties
.idea/
*.apk
*.aab

.venv/

`````

## README.md

`````markdown
# 2D WORLD Engine

Native Android 2D scene editor, written in Kotlin with Jetpack Compose and OpenGL ES 3.

## Current status

**Phase 2 source is implemented for the workflows listed below. Full Android app build and device acceptance are still pending. Phase 3 has not started.** Do not treat this as a fully verified or finished Unity-class engine.

- Phase 1: dashboard, project wizard, scene editing, touch viewport, image import, atomic JSON saves, undo/redo and recovery snapshots.
- Phase 2: SQLite UUID asset catalog, grid/list browser, metadata/tags/favorites/search/type filters, drag-to-viewport placement, texture operations, named sprite-sheet slicing and ZIP export, 335 original procedural recipes, modular prefab splitting, hierarchical transforms, nested prefab instances and property overrides.
- No inactive physics, animation, gameplay or APK-export controls are exposed.

### Delivery guides and full source

- [Phase 2 architecture, file changes, exact scope, instructions and limitations](docs/PHASE2.md)
- [Phase 2 complete new/changed file listing](docs/PHASE2_SOURCE.md)
- [Phase 2 acceptance walkthrough](docs/PHASE2_VERIFICATION.md)
- [Original master specification](docs/MASTER_SPEC.md)
- [Historical Phase 1 guide](docs/PHASE1_README.md) and [source snapshot](docs/PHASE1_SOURCE.md)

The code files in their modules are authoritative; source appendices are historical delivery snapshots.

## Build and run

Install **JDK 17**, Android SDK **34**, and Build Tools **34.0.0**. Open the repository in Android Studio (Koala or newer), select Gradle JDK 17, configure the Android SDK location, sync and run `app` on an Android 7+ device with GLES 3 support.

```sh
./gradlew :engine-math:test :engine-core:test :app:assembleDebug
./gradlew :app:installDebug
./gradlew :engine-assets:connectedDebugAndroidTest :engine-io:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24; minimum SDK 24, target/compile SDK 34. Initial development-tool/dependency downloads need connectivity; editor operation is offline. On Windows use `gradlew.bat`.

Successful Gradle packaging produces `app/build/outputs/apk/debug/app-debug.apk`. **No APK was produced in this session.** The Android CI workflow is supplied but has not been run here.

## Validation actually performed

- Standalone Kotlin **1.9.23** compilation of math, core, assets, IO, renderer and viewport against Android API 34, with serialization generation: **passed**. This diagnostic compiler is not the pinned Gradle/Kotlin 1.9.24 app toolchain.
- Six executable core smoke checks, including nested overrides and JSON serialization: **passed**.
- Host SQLite checks executing the production DDL: **passed**.
- Repository/module/XML/wrapper/source-presence checks: **passed**.
- Full Gradle build: **blocked by Gradle distribution TLS/download failure**, after obtaining a temporary Java 17 runtime. Android SDK installation and Compose/AndroidX build dependencies are unavailable in this sandbox.
- JUnit, Android bitmap/SQLite tests, Compose tests, physical touch/drop tests and GL device tests: **not run**.

## Data safety

Projects use app-private storage: `files/2DWorldProjects/<UUID>/`. No broad storage or internet permission is requested. Uninstalling/clearing app data deletes projects; back up important projects first. Catalog folders are virtual; immutable payload filenames do not change when assets are renamed or moved in the catalog. Asset edits create revisions or new assets, not scene undo commands. Old payload revisions are retained; automatic cleanup is not implemented.

**Phase gate:** verify Phase 2 and confirm before Phase 3 (animation, physics, input and cameras).

`````

## app/src/androidTest/kotlin/world/engine/app/EditorSmokeTest.kt

`````kotlin
package world.engine.app

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class EditorSmokeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun createAddSaveAndReturnToDashboard() {
        compose.onNodeWithText("New project").performClick()
        compose.onNodeWithText("Create",useUnmergedTree=true).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Add sprite").fetchSemanticsNodes().isNotEmpty() }
        repeat(3) { compose.onNodeWithText("Add sprite").performScrollTo().performClick() }
        compose.onNodeWithText("Save",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("MyGame • saved").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Projects",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Save & close").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("New project").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun generateSplitPlaceAndSaveVehicle() {
        compose.onNodeWithText("New project").performClick()
        compose.onNodeWithText("Create",useUnmergedTree=true).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Assets",substring=false).fetchSemanticsNodes().isNotEmpty() }
        // Toolbar Assets exists even when the phone tab is outside the visible scroll range.
        compose.onAllNodesWithText("Assets",substring=false)[0].performScrollTo().performClick()
        compose.onNodeWithText("Generate 335").performScrollTo().performClick()
        compose.onNodeWithText("Generate",substring=false).performClick()
        compose.waitUntil(90000) { compose.onAllNodesWithText("Search names, folders, tags (335)").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Expand").performScrollTo().performClick()
        compose.onAllNodesWithText("Search names, folders, tags (335)").onLast().performTextInput("Vehicle 1")
        compose.onAllNodesWithText("Vehicle 1",substring=false).onLast().performScrollTo().performClick()
        compose.onAllNodesWithText("Inspect / edit").onLast().performScrollTo().performClick()
        compose.onNodeWithText("Sheet / split").performClick()
        compose.onNodeWithText("Split into named prefab parts").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Vehicle 1 split",substring=false).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Use in scene").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Return to scene").fetchSemanticsNodes().any { !it.config.contains(SemanticsProperties.Disabled) } }
        compose.onNodeWithText("Return to scene").performClick()
        compose.onNodeWithText("Save",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("MyGame • saved").fetchSemanticsNodes().isNotEmpty() }
    }
}

`````

## docs/PHASE1_README.md

`````markdown
# 2D WORLD Engine — Phase 1 foundation

Native Android scene editor source, implemented in Kotlin, Jetpack Compose and OpenGL ES 3.0. **Android compilation and device execution have not been verified in this sandbox.** This is a foundation implementation for review, not a claim of a finished Unity-class engine.

## 1. Architecture decisions

- **21 Gradle module boundaries** follow the master specification. Phase 1 implements `engine-math`, `engine-core`, `engine-render`, `engine-io`, `editor-viewport`, `editor-ui`, and `app`. Other modules contain build configuration only, explicitly reserved for later phases; they are not wired into the app.
- **Immutable ECS snapshots:** stable entity UUIDs, typed serializable transform/sprite components, component queries, painter-ordered flat scenes, inverse-transform picking, validated scene versions and IDs. No parent/child transforms yet.
- **MVVM:** one activity, lifecycle-retained view model, Flow state, immutable snapshots sent to the GL thread. Compose does not mutate GL resources.
- **GLES 3:** shader-based textured sprite batching, procedural solid sprites, background grid, selection outline, texture caching, context recreation, alpha blending, nearest-neighbor filtering. A 64 MiB texture budget produces a checkerboard and actionable console error instead of unlimited allocation.
- **Touch:** one finger selects/moves a sprite or pans empty space; pinch zooms around the gesture focus; long press selects and opens a delete confirmation. Inspector provides precise position, rotation and scale. One drag is one history command.
- **Persistence:** JSON manifest and scene, atomic file writes, explicit save, 30-second dirty-scene autosave, best-effort background snapshot. Disk work uses IO coroutines and a mutex. Recovery never silently overwrites the explicit save.
- **Android storage:** internal app-owned `files/2DWorldProjects/<UUID>/` replaces unrestricted shared-storage paths. Display names live in manifests. Android scoped storage does not allow arbitrary shared-root writes without inappropriate broad permissions. Image import uses the system document picker and copies normalized PNGs into the project; no persistent URI grant is needed.
- **Permission minimization:** no internet, storage or install permissions. Non-exported FileProvider is restricted to a dedicated cache export directory and reserved for later real sharing flows. Only the launcher activity is exported.
- **Honest UI:** only Phase 1 actions are visible; no Play, Build, scripting, physics or other inactive controls. Portrait/landscape in the wizard is game-project metadata, not a forced editor orientation.

## 2. File tree

**Changed:** `README.md` (this guide).

**Preserved content:** original README copied verbatim to `docs/MASTER_SPEC.md`.

**New build infrastructure:** `.gitignore`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.{jar,properties}`, and a build file in every module.

**Implemented modules and source files:**

| Module | Source files (under `src/main/kotlin/world/engine/`) |
|---|---|
| engine-math | `math/Math.kt` |
| engine-core | `core/Scene.kt` |
| engine-render | `render/Camera2D.kt`, `SpriteBatch.kt`, `TextureLoader.kt`, `SceneRenderer.kt` |
| engine-io | `io/ProjectStore.kt` |
| editor-viewport | `viewport/WorldViewport.kt` |
| editor-ui | `editor/EditorViewModel.kt`, `WorldEditorApp.kt` |
| app | `app/MainActivity.kt` |

All Android modules have `src/main/AndroidManifest.xml`. App additionally has `src/main/res/values/styles.xml` and `src/main/res/xml/file_paths.xml`.

**Reserved module boundaries:** `engine-physics`, `engine-audio`, `engine-animation`, `engine-assets`, `engine-scripting`, `engine-ai`, `engine-tilemap`, `engine-particles`, `engine-ui`, `engine-build`, `editor-assets`, `editor-animation`, `editor-scripting`, `editor-build`. Each has a build file, minimal Android manifest and a README describing its unimplemented status.

**New tests:**
- `engine-math/src/test/kotlin/world/engine/math/MathTest.kt`
- `engine-core/src/test/kotlin/world/engine/core/SceneTest.kt`
- `engine-io/src/androidTest/kotlin/world/engine/io/ProjectStoreTest.kt`
- `app/src/androidTest/kotlin/world/engine/app/EditorSmokeTest.kt`
- `app/src/androidTest/kotlin/world/engine/app/GlRenderTest.kt`

**New documentation/tooling:** `docs/VERIFICATION.md`, `docs/PHASE1_SOURCE.md`, `tools/validate_structure.py`.

**Unchanged tracked files:** none; the original repository contained only README.md. Git metadata is untouched.

## 3. Complete source

The actual source files are in the repository, not pseudocode. [Full source listing](docs/PHASE1_SOURCE.md) contains all Kotlin files, Gradle configuration, manifests/resources, tests, validation script and wrapper scripts with paths, without omitted sections. It also includes the wrapper JAR as base64 with its SHA-256. Documentation is included too; the appendix does not recursively embed itself.

## 4. Gradle configuration

- Gradle **8.7** official wrapper; Android Gradle Plugin **8.5.2**.
- JDK **17**, Kotlin **1.9.24**, Compose compiler **1.5.14**, Compose BOM **2024.06.00**.
- Compile SDK **34**, application target SDK **34**, minimum SDK **24**; GLES **3.0 required**.
- kotlinx.serialization JSON **1.6.3**, coroutines **1.8.1**, JUnit Jupiter **5.10.3** for JVM tests; AndroidX Compose UI Test/JUnit4 for instrumented tests.
- JBox2D, audio, image-browser libraries and other later-phase dependencies are intentionally not downloaded or wired before their implementation phases.
- Wrapper JAR from the official `gradle/gradle` `v8.7.0` repository. SHA-256: `cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8`.

## 5. Build & run

### Android Studio

1. Install Android Studio supporting AGP 8.5 (Koala or newer), SDK Platform 34 and Build Tools 34.0.0.
2. Open this repository as the Gradle project; set the Gradle JVM to **JDK 17**.
3. Set the SDK path through Android Studio or untracked `local.properties` (`sdk.dir=/your/Android/Sdk`).
4. Sync Gradle. First-time SDK/dependency downloads require internet. The editor itself has no network dependency or internet permission.
5. Select `app`, connect an Android 7+ GLES 3 device or GLES 3-capable emulator, then Run.

### Command line

Set `JAVA_HOME` to JDK 17 and `ANDROID_HOME` to your installed Android SDK. Accept SDK licenses with the SDK manager before building.

```sh
./gradlew --version
./gradlew :engine-math:test :engine-core:test :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -n world.engine.editor/world.engine.app.MainActivity
./gradlew :engine-io:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

On Windows use `gradlew.bat`. The real Gradle build, when successful, outputs `app/build/outputs/apk/debug/app-debug.apk`. **No APK was generated in this sandbox.**

After dependencies and SDK tools are cached, use `./gradlew --offline :app:assembleDebug`. Offline editor operation does not imply a first build without downloaded development tools.

Projects contain the complete folder layout in the specification, including `project.json`, `Scenes/Main.json`, normalized `Sprites/<UUID>.png`, and `.autosave/Main.json` when dirty. Project files survive process death and reboot, but **uninstalling/clearing app data deletes them**. Project ZIP export/SAF workspace selection is not implemented in Phase 1; back up debug projects using the steps in the verification guide.

## 6. Verification tests

See [reproducible device walkthrough and test matrix](docs/VERIFICATION.md).

This sandbox's actual results:

| Check | Result |
|---|---|
| `python3 tools/validate_structure.py` | PASS: 21 module boundaries/references, XML, wrapper integrity, permissions, test-source presence |
| `unzip -t gradle/wrapper/gradle-wrapper.jar` | PASS |
| `git diff --check` | PASS |
| `./gradlew --version` | BLOCKED: no Java / JAVA_HOME |
| SDK, Gradle distribution and Maven direct downloads | BLOCKED by network connectivity |
| Kotlin/Android compilation, JUnit, Compose, GL device tests | NOT RUN |
| APK install, force-stop persistence and recovery walkthrough | NOT RUN |

Structural checks do **not** prove compilation or runtime correctness. The Phase 1 acceptance gate remains open until Android builds and device tests pass.

## 7. Known limitations

- No on-device verification or successful Android build in this environment; compilation/runtime issues may remain.
- One flat scene (`Main`) per project. No parent-child hierarchy, prefabs, multi-scene workflow, gameplay, physics, audio, scripting or APK exporter yet.
- Later-phase modules are reserved configuration, not implementations.
- Responsive fixed panels: three columns on wide tablets; hierarchy/inspector/console tabs or full viewport on phones. Docking, resizing, accessibility transform handles and advanced editor tools belong to later phases.
- Long press currently offers deletion, not a general context-action menu. Pinch zoom and one-finger empty-space pan are supported; simultaneous two-finger pan is not.
- Camera position/zoom and selection are editor session state, not scene properties. Reset view returns to origin. Numeric transform values and imported image references persist.
- Assets are copied locally using UUID filenames. A full asset database, move/rename dependency management, replacement UI, atlas generation and import previews are Phase 2 work. Missing images show a checkerboard; re-import creates a new sprite rather than repairing the old reference.
- Imports accept up to 16 MiB encoded data, normalized to a maximum 2048 pixels per dimension; textures have a 64 MiB per-project cache budget; scene JSON is limited to 8 MiB, entities to 5000, undo history to 100 edits. These are safeguards, not measured performance guarantees.
- Deleted entities do not delete their image files; undo remains valid. Unused asset cleanup is not implemented.
- Force-stop can lose unsaved work since the last successful snapshot (up to 30 seconds during active editing). Background autosave is best effort, not a guarantee against OS termination. Explicit Save is the durability boundary.
- A malformed recovery snapshot can be discarded through the recovery prompt without blocking the saved scene. Corrupt explicit scene files or unsupported versions produce errors rather than silently replacing data; debug backup/manual repair may be required. No universal “never crash” guarantee is asserted.
- App-private projects are not visible in the shared-storage root. Full SAF workspace selection/export is deferred. No install or storage permission is requested merely to satisfy a permissions checklist.
- Only procedural colored sprites and user-selected images are included; no third-party art is bundled. No broad asset library or performance claims.

## 8. Next phase preview and confirmation gate

Phase 2 introduces the asset database, stable reference management, searchable asset browser, sprite-sheet/texture editing, modular splitting and prefabs. It has **not** started.

**Please build and run Phase 1, complete the MyGame save/force-stop/reopen test, and confirm success (or share the build/test failures) before authorizing Phase 2.**

`````

## docs/PHASE2.md

`````markdown
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

`````

## docs/PHASE2_FILES.md

`````markdown
# Phase 2 file inventory

Relative to the preceding Phase 1 delivery, not to the original README-only Git commit. No repository files were deleted.

## New (20)

```text
.github/workflows/android.yml
docs/PHASE1_README.md
docs/PHASE2.md
docs/PHASE2_FILES.md
docs/PHASE2_SOURCE.md
docs/PHASE2_VERIFICATION.md
editor-assets/src/main/kotlin/world/engine/asseteditor/AssetActions.kt
editor-assets/src/main/kotlin/world/engine/asseteditor/AssetBrowser.kt
editor-assets/src/main/kotlin/world/engine/asseteditor/AssetInspector.kt
engine-assets/src/androidTest/kotlin/world/engine/assets/AssetPipelineTest.kt
engine-assets/src/main/kotlin/world/engine/assets/AssetDatabase.kt
engine-assets/src/main/kotlin/world/engine/assets/AssetRepository.kt
engine-assets/src/main/kotlin/world/engine/assets/ImageOps.kt
engine-assets/src/main/kotlin/world/engine/assets/ProceduralAssets.kt
engine-assets/src/main/kotlin/world/engine/assets/SpriteSlicer.kt
engine-core/src/main/kotlin/world/engine/core/Prefab.kt
engine-core/src/test/kotlin/world/engine/core/PrefabTest.kt
tools/CoreSmoke.kt
tools/check_asset_schema.py
tools/check_engine_standalone.py
```

## Changed (17)

```text
.gitignore
README.md
app/src/androidTest/kotlin/world/engine/app/EditorSmokeTest.kt
editor-assets/README.md
editor-assets/build.gradle.kts
editor-ui/build.gradle.kts
editor-ui/src/main/kotlin/world/engine/editor/EditorViewModel.kt
editor-ui/src/main/kotlin/world/engine/editor/WorldEditorApp.kt
editor-viewport/src/main/kotlin/world/engine/viewport/WorldViewport.kt
engine-assets/README.md
engine-assets/build.gradle.kts
engine-core/src/main/kotlin/world/engine/core/Scene.kt
engine-io/src/main/kotlin/world/engine/io/ProjectStore.kt
engine-math/src/main/kotlin/world/engine/math/Math.kt
engine-render/src/main/kotlin/world/engine/render/SceneRenderer.kt
engine-render/src/main/kotlin/world/engine/render/SpriteBatch.kt
tools/validate_structure.py
```

## Unchanged (68)

```text
app/build.gradle.kts
app/src/androidTest/kotlin/world/engine/app/GlRenderTest.kt
app/src/main/AndroidManifest.xml
app/src/main/kotlin/world/engine/app/MainActivity.kt
app/src/main/res/values/styles.xml
app/src/main/res/xml/file_paths.xml
build.gradle.kts
docs/MASTER_SPEC.md
docs/PHASE1_SOURCE.md
docs/VERIFICATION.md
editor-animation/README.md
editor-animation/build.gradle.kts
editor-animation/src/main/AndroidManifest.xml
editor-assets/src/main/AndroidManifest.xml
editor-build/README.md
editor-build/build.gradle.kts
editor-build/src/main/AndroidManifest.xml
editor-scripting/README.md
editor-scripting/build.gradle.kts
editor-scripting/src/main/AndroidManifest.xml
editor-ui/src/main/AndroidManifest.xml
editor-viewport/build.gradle.kts
editor-viewport/src/main/AndroidManifest.xml
engine-ai/README.md
engine-ai/build.gradle.kts
engine-ai/src/main/AndroidManifest.xml
engine-animation/README.md
engine-animation/build.gradle.kts
engine-animation/src/main/AndroidManifest.xml
engine-assets/src/main/AndroidManifest.xml
engine-audio/README.md
engine-audio/build.gradle.kts
engine-audio/src/main/AndroidManifest.xml
engine-build/README.md
engine-build/build.gradle.kts
engine-build/src/main/AndroidManifest.xml
engine-core/build.gradle.kts
engine-core/src/test/kotlin/world/engine/core/SceneTest.kt
engine-io/build.gradle.kts
engine-io/src/androidTest/kotlin/world/engine/io/ProjectStoreTest.kt
engine-io/src/main/AndroidManifest.xml
engine-math/build.gradle.kts
engine-math/src/test/kotlin/world/engine/math/MathTest.kt
engine-particles/README.md
engine-particles/build.gradle.kts
engine-particles/src/main/AndroidManifest.xml
engine-physics/README.md
engine-physics/build.gradle.kts
engine-physics/src/main/AndroidManifest.xml
engine-render/build.gradle.kts
engine-render/src/main/AndroidManifest.xml
engine-render/src/main/kotlin/world/engine/render/Camera2D.kt
engine-render/src/main/kotlin/world/engine/render/TextureLoader.kt
engine-scripting/README.md
engine-scripting/build.gradle.kts
engine-scripting/src/main/AndroidManifest.xml
engine-tilemap/README.md
engine-tilemap/build.gradle.kts
engine-tilemap/src/main/AndroidManifest.xml
engine-ui/README.md
engine-ui/build.gradle.kts
engine-ui/src/main/AndroidManifest.xml
gradle.properties
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
gradlew
gradlew.bat
settings.gradle.kts
```

## Removed (0)

None.

`````

## docs/PHASE2_VERIFICATION.md

`````markdown
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

`````

## editor-assets/README.md

`````markdown
# editor-assets — Phase 2 implementation

Compose asset grid/list with search, tags/favorites/type filters, Coil file previews, local drag-and-drop payloads, expandable browsing, asset metadata/actions, non-destructive texture tools, and manual/grid/alpha sprite-sheet region editing.

The `AssetActions` interface connects UI actions to the editor view model without introducing a dependency back to editor-ui. Sheet layouts are explicitly saved. Long-press dragging works from the embedded browser into the viewport; the expanded browser uses the explicit Use action.

No Phase 3 controls are exposed. Full app/Compose compilation and physical-device gesture verification remain pending.

`````

## editor-assets/build.gradle.kts

`````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.editorassets"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}
dependencies {
    api(project(":engine-assets"))
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("io.coil-kt:coil-compose:2.6.0")
}

`````

## editor-assets/src/main/kotlin/world/engine/asseteditor/AssetActions.kt

`````kotlin
package world.engine.asseteditor

import android.net.Uri
import world.engine.assets.*

/** Editor-facing actions. All disk-backed implementations dispatch away from the UI thread. */
interface AssetActions {
    fun selectAsset(id: String)
    fun useAsset(id: String)
    fun importAsset(uri: Uri)
    fun replaceAsset(id: String,uri: Uri)
    fun metadata(id: String,name: String,folder: String,tags: List<String>,favorite: Boolean)
    fun prefabFromAsset(id: String)
    fun duplicateAsset(id: String)
    fun deleteAsset(id: String)
    fun editTexture(id: String,edit: TextureEdit)
    fun autoSlice(id: String)
    fun setFrames(frames: List<SpriteFrame>)
    fun appendFrame(frame: SpriteFrame)
    fun saveSheet(id: String)
    fun exportFrames(id: String,uri: Uri)
    fun extractFrames(id: String)
    fun splitAsset(id: String)
    fun exportAsset(id: String,uri: Uri)
    fun generateLibrary()
    fun assetError(message: String)
}

`````

## editor-assets/src/main/kotlin/world/engine/asseteditor/AssetBrowser.kt

`````kotlin
package world.engine.asseteditor

import android.content.ClipData
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import world.engine.assets.*
import java.io.File

@Composable fun AssetBrowser(assets: List<AssetRecord>,selected: String?,frames: List<SpriteFrame>,root: File,busy: Boolean,progress: String,actions: AssetActions,modifier: Modifier=Modifier,allowExpand: Boolean=true) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var favorite by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(true) }
    var kind by remember { mutableIntStateOf(0) }
    var inspect by remember { mutableStateOf(false) }
    var generate by remember { mutableStateOf(false) }
    val chosen=assets.find { it.id==selected }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(actions::importAsset) }
    val filtered=assets.filter { a -> (!favorite || a.favorite) && (kind==0 || (kind==1 && a.kind==AssetKind.IMAGE) || (kind==2 && a.kind==AssetKind.PREFAB)) && (a.name+" "+a.folder+" "+a.tags.joinToString(" ")).contains(query,true) }
    Column(modifier.padding(8.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            if(allowExpand)TextButton(onClick={expanded=true}) { Text("Expand") }
            TextButton(enabled=!busy,onClick={importer.launch(arrayOf("image/*"))}) { Text("Import") }
            TextButton(enabled=!busy,onClick={generate=true}) { Text("Generate 335") }
            TextButton(enabled=chosen!=null && !busy,onClick={inspect=true}) { Text("Inspect / edit") }
            TextButton(enabled=chosen!=null && !busy,onClick={chosen?.let { actions.useAsset(it.id) }}) { Text("Use selected") }
            TextButton(onClick={grid=!grid}) { Text(if(grid) "List view" else "Grid view") }
            TextButton(onClick={favorite=!favorite}) { Text(if(favorite) "★ Favorites" else "Favorites off") }
            TextButton(onClick={kind=(kind+1)%3}) { Text(listOf("All types","Images","Prefabs")[kind]) }
        }
        OutlinedTextField(query,{query=it},singleLine=true,label={Text("Search names, folders, tags (${filtered.size})")},modifier=Modifier.fillMaxWidth())
        if(progress.isNotEmpty())Text(progress,style=MaterialTheme.typography.labelSmall)
        if(filtered.isEmpty())Text("No matching assets. Import a local image or generate the procedural library.")
        else if(grid) LazyVerticalGrid(columns=GridCells.Adaptive(100.dp),modifier=Modifier.weight(1f)) {
            items(filtered,key={it.id}) { a -> AssetTile(a,root,a.id==selected,busy,actions) }
        } else LazyColumn(Modifier.weight(1f)) { items(filtered,key={it.id}) { a -> AssetTile(a,root,a.id==selected,busy,actions,true) } }
    }
    if(expanded)Dialog(onDismissRequest={if(!busy)expanded=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column { TextButton(enabled=!busy,onClick={expanded=false}){Text("Return to scene")}; AssetBrowser(assets,selected,frames,root,busy,progress,actions,Modifier.weight(1f),false) }
        }
    }
    if(inspect && chosen!=null) Dialog(onDismissRequest={if(!busy)inspect=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            AssetInspector(chosen,frames,root,busy,actions){inspect=false}
        }
    }
    if(generate) AlertDialog(onDismissRequest={generate=false},title={Text("Generate original procedural assets?")},text={Text("Creates 335 small PNG images locally: 30 characters, 30 enemies, 20 NPCs, 20 animals, 25 vehicles, 40 buildings, 30 nature items, 40 props and 100 tileable textures. Re-running skips existing recipes. These are geometric pixel-art variants, not hand-drawn game packs.")},confirmButton={TextButton(onClick={generate=false;actions.generateLibrary()}){Text("Generate")}},dismissButton={TextButton(onClick={generate=false}){Text("Cancel")}})
}

@Composable private fun AssetTile(a: AssetRecord,root: File,selected: Boolean,busy: Boolean,actions: AssetActions,list: Boolean=false) {
    val view=LocalView.current
    val drag=Modifier.pointerInput(a.id,busy) {
        if(!busy) detectDragGesturesAfterLongPress(onDragStart={
            actions.selectAsset(a.id)
            val shadow=object: View.DragShadowBuilder() {
                override fun onProvideShadowMetrics(size: Point,touch: Point) { size.set(96,96); touch.set(48,48) }
                override fun onDrawShadow(canvas: Canvas) { canvas.drawRect(0f,0f,96f,96f,Paint().apply { color=0xff4488dd.toInt() }) }
            }
            view.startDragAndDrop(ClipData.newPlainText("2DWorldAsset",a.id),shadow,null,0)
        },onDrag={change,_->change.consume()})
    }
    OutlinedCard(onClick={actions.selectAsset(a.id)},enabled=!busy,modifier=Modifier.padding(3.dp).then(drag)) {
        if(list) Row(Modifier.padding(8.dp)) { Preview(a,root,Modifier.size(40.dp)); Text((if(selected) "● " else "")+a.name+" · "+a.folder,Modifier.padding(start=8.dp)) }
        else Column(Modifier.padding(6.dp)) { Preview(a,root,Modifier.fillMaxWidth().height(52.dp)); Text((if(selected) "● " else "")+(if(a.favorite) "★ " else "")+a.name,maxLines=2,style=MaterialTheme.typography.labelSmall) }
    }
}
@Composable internal fun Preview(a: AssetRecord,root: File,modifier: Modifier) {
    if(a.kind==AssetKind.IMAGE) AsyncImage(model=File(root,a.path),contentDescription=a.name,contentScale=ContentScale.Fit,modifier=modifier)
    else Box(modifier) { Text("▦ Prefab",Modifier.padding(8.dp)) }
}

`````

## editor-assets/src/main/kotlin/world/engine/asseteditor/AssetInspector.kt

`````kotlin
package world.engine.asseteditor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import world.engine.assets.*
import java.io.File
import kotlin.math.*

@Composable internal fun AssetInspector(a: AssetRecord,frames: List<SpriteFrame>,root: File,busy: Boolean,actions: AssetActions,close: ()->Unit) {
    var tab by remember(a.id) { mutableStateOf("Details") }
    var name by remember(a) { mutableStateOf(a.name) }
    var folder by remember(a) { mutableStateOf(a.folder) }
    var tags by remember(a) { mutableStateOf(a.tags.joinToString(", ")) }
    var favorite by remember(a) { mutableStateOf(a.favorite) }
    var delete by remember { mutableStateOf(false) }
    var replace by remember { mutableStateOf(false) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { actions.replaceAsset(a.id,it) } }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri -> uri?.let { actions.exportAsset(a.id,it) } }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(enabled=!busy,onClick=close){Text("Close")}
            TextButton(enabled=!busy,onClick={actions.useAsset(a.id);close()}){Text("Use in scene")}
            TextButton(enabled=!busy,onClick={actions.duplicateAsset(a.id)}){Text("Duplicate")}
            if(a.kind==AssetKind.IMAGE)TextButton(enabled=!busy,onClick={export.launch("${a.name}.png")}){Text("Export PNG")}
        }
        Text(a.name,style=MaterialTheme.typography.titleLarge)
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        Row { (if(a.kind==AssetKind.IMAGE)listOf("Details","Texture","Sheet / split") else listOf("Details")).forEach { value -> TextButton(onClick={tab=value}){Text(if(tab==value)"[$value]" else value)} } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if(a.kind==AssetKind.IMAGE) RegionPreview(a,root,if(tab=="Sheet / split")frames else emptyList(),tab=="Sheet / split" && !busy,actions::appendFrame)
            Text("${a.kind} · ${a.width}×${a.height} · revision ${a.revision}",style=MaterialTheme.typography.labelMedium)
            when(tab) {
                "Details" -> {
                    Input("Name",name){name=it}; Input("Catalog folder (move)",folder){folder=it}; Input("Tags, comma separated",tags){tags=it}
                    Row { Checkbox(favorite,{favorite=it}); Text("Favorite",Modifier.padding(top=12.dp)) }
                    Button(enabled=!busy,onClick={actions.metadata(a.id,name,folder,tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },favorite)}){Text("Save metadata")}
                    Text("UUID: ${a.id}",style=MaterialTheme.typography.labelSmall)
                    Text("${a.path}\nCatalog moves and renames never change the UUID.",style=MaterialTheme.typography.labelSmall)
                    if(a.kind==AssetKind.IMAGE)TextButton(enabled=!busy,onClick={replace=true}){Text("Replace image, keep references")}
                    if(a.kind==AssetKind.PREFAB)Text("Use creates an editable hierarchy. Edit its parts in the scene; Update prefab saves the source. Refresh instance applies source changes while retaining property overrides.")
                    TextButton(enabled=!busy,onClick={actions.prefabFromAsset(a.id)}){Text("Create prefab")}
                    TextButton(enabled=!busy,onClick={delete=true}){Text("Delete from catalog")}
                }
                "Texture" -> TextureControls(a,busy,actions)
                else -> SheetControls(a,frames,busy,actions)
            }
        }
    }
    if(delete)AlertDialog(onDismissRequest={delete=false},title={Text("Delete ${a.name}?")},text={Text("Referenced assets are protected, including saved scenes, recovery, undo history and prefab dependencies. Unreferenced payload revisions remain on disk for safety.")},confirmButton={TextButton(enabled=!busy,onClick={delete=false;actions.deleteAsset(a.id)}){Text("Delete")}},dismissButton={TextButton(onClick={delete=false}){Text("Cancel")}})
    if(replace)AlertDialog(onDismissRequest={replace=false},title={Text("Replace image for every reference?")},text={Text("The UUID stays the same. All scenes and prefabs referencing it will use the new pixels. This changes the asset, not the scene undo history.")},confirmButton={TextButton(enabled=!busy,onClick={replace=false;picker.launch(arrayOf("image/*"))}){Text("Choose image")}},dismissButton={TextButton(onClick={replace=false}){Text("Cancel")}})
}

@Composable private fun TextureControls(a: AssetRecord,busy: Boolean,actions: AssetActions) {
    var x by remember(a.id){mutableStateOf("0")}; var y by remember(a.id){mutableStateOf("0")}
    var width by remember(a){mutableStateOf(a.width.toString())}; var height by remember(a){mutableStateOf(a.height.toString())}
    var color by remember { mutableStateOf("#FFFFFFFF") }
    Text("Operations create a new image; the original and its references are unchanged.")
    Input("Crop X",x){x=it}; Input("Crop Y",y){y=it}; Input("Width",width){width=it}; Input("Height",height){height=it}; Input("Color multiplier #AARRGGBB",color){color=it}
    listOf("Crop","Resize","Rotate 90°","Flip X","Flip Y","Tile 2×2","Colorize").forEach { operation ->
        TextButton(enabled=!busy,onClick={
            try {
                actions.editTexture(a.id,TextureEdit(operation,x.toInt(),y.toInt(),width.toInt(),height.toInt(),android.graphics.Color.parseColor(color)))
            } catch(e: IllegalArgumentException) { actions.assetError("Use integer dimensions and a valid #AARRGGBB color.") }
        }){Text(operation)}
    }
}

@Composable private fun SheetControls(a: AssetRecord,frames: List<SpriteFrame>,busy: Boolean,actions: AssetActions) {
    val recipe=a.recipe
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> uri?.let { actions.exportFrames(a.id,it) } }
    var width by remember(a.id){mutableStateOf("32")}; var height by remember(a.id){mutableStateOf("32")}
    Text("Draw rectangles on the preview for manual regions. Grid replaces the list. Alpha slicing detects connected opaque islands, not semantic object parts.")
    Input("Grid cell width",width){width=it}; Input("Grid cell height",height){height=it}
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        TextButton(enabled=!busy,onClick={try { actions.setFrames(SpriteSlicer.grid(a.width,a.height,width.toInt(),height.toInt())) } catch(e: IllegalArgumentException){actions.assetError(e.message ?: "Invalid grid")}}){Text("Grid slice")}
        TextButton(enabled=!busy,onClick={actions.autoSlice(a.id)}){Text("Auto alpha")}
        TextButton(enabled=!busy,onClick={actions.setFrames(listOf(SpriteFrame("Whole image",0,0,a.width,a.height)))}){Text("Full frame")}
        TextButton(enabled=!busy,onClick={actions.setFrames(emptyList())}){Text("Clear regions")}
    }
    Text("${frames.size} named regions · top-left pixel coordinates")
    frames.forEachIndexed { index,frame ->
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(frame.name,{value -> actions.setFrames(frames.toMutableList().also { it[index]=frame.copy(name=value) })},label={Text("${index+1}: ${frame.x},${frame.y} ${frame.width}×${frame.height}")},singleLine=true,modifier=Modifier.weight(1f),enabled=!busy)
            TextButton(enabled=!busy && index>0,onClick={actions.setFrames(frames.toMutableList().also { java.util.Collections.swap(it,index,index-1) })}){Text("↑")}
            TextButton(enabled=!busy && index<frames.lastIndex,onClick={actions.setFrames(frames.toMutableList().also { java.util.Collections.swap(it,index,index+1) })}){Text("↓")}
            TextButton(enabled=!busy,onClick={actions.setFrames(frames.filterIndexed { i,_->i!=index })}){Text("×")}
        }
    }
    Button(enabled=!busy,onClick={actions.saveSheet(a.id)}){Text("Save named layout")}
    Button(enabled=!busy && frames.isNotEmpty(),onClick={export.launch("${a.name}-frames.zip")}){Text("Export frames ZIP + manifest")}
    Button(enabled=!busy && frames.isNotEmpty(),onClick={actions.extractFrames(a.id)}){Text("Extract frames as PNG assets")}
    Button(enabled=!busy && (frames.isNotEmpty() || (recipe!=null && !recipe.startsWith("Texture:"))),onClick={actions.splitAsset(a.id)}){Text("Split into named prefab parts")}
    if(recipe!=null && !recipe.startsWith("Texture:"))Text("Generated composites use their original transparent layers, not rectangular crops. Regions are ignored for these assets.")
}
@Composable private fun Input(label: String,value: String,change: (String)->Unit) { OutlinedTextField(value,change,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth()) }

@Composable private fun RegionPreview(a: AssetRecord,root: File,frames: List<SpriteFrame>,editable: Boolean,append: (SpriteFrame)->Unit) {
    var area by remember { mutableStateOf(IntSize(1,1)) }
    var start by remember { mutableStateOf<Offset?>(null) }
    var end by remember { mutableStateOf<Offset?>(null) }
    val currentAppend by rememberUpdatedState(append)
    fun pixel(point: Offset): Offset {
        val scale=min(area.width.toFloat()/a.width,area.height.toFloat()/a.height)
        val origin=Offset((area.width-a.width*scale)/2,(area.height-a.height*scale)/2)
        return Offset(((point.x-origin.x)/scale).coerceIn(0f,a.width.toFloat()),((point.y-origin.y)/scale).coerceIn(0f,a.height.toFloat()))
    }
    Box(Modifier.size(240.dp).onSizeChanged { area=it }.pointerInput(a.id,editable,area) {
        if(editable)detectDragGestures(onDragStart={start=pixel(it);end=start},onDragEnd={
            val p=start;val q=end
            if(p!=null && q!=null) {
                val left=floor(min(p.x,q.x)).toInt();val top=floor(min(p.y,q.y)).toInt()
                val right=ceil(max(p.x,q.x)).toInt();val bottom=ceil(max(p.y,q.y)).toInt()
                if(right>left && bottom>top)currentAppend(SpriteFrame("Region ${System.currentTimeMillis()%10000}",left,top,right-left,bottom-top))
            }
            start=null;end=null
        },onDragCancel={start=null;end=null},onDrag={change,_->end=pixel(change.position);change.consume()})
    }) {
        Preview(a,root,Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val scale=min(size.width/a.width,size.height/a.height);val ox=(size.width-a.width*scale)/2;val oy=(size.height-a.height*scale)/2
            frames.forEach { f -> drawRect(Color.Yellow,Offset(ox+f.x*scale,oy+f.y*scale),androidx.compose.ui.geometry.Size(f.width*scale,f.height*scale),style=Stroke(2f)) }
            val p=start;val q=end
            if(p!=null && q!=null)drawRect(Color.Cyan,Offset(ox+min(p.x,q.x)*scale,oy+min(p.y,q.y)*scale),androidx.compose.ui.geometry.Size(abs(p.x-q.x)*scale,abs(p.y-q.y)*scale),style=Stroke(2f))
        }
    }
}

`````

## editor-ui/build.gradle.kts

`````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.editorui"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}
dependencies {
    api(project(":editor-assets"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    api(project(":editor-viewport"))
    api(project(":engine-io"))
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.core:core-ktx:1.13.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

`````

## editor-ui/src/main/kotlin/world/engine/editor/EditorViewModel.kt

`````kotlin
package world.engine.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import world.engine.core.*
import world.engine.io.*
import world.engine.math.*
import world.engine.assets.*
import world.engine.asseteditor.AssetActions

data class EditorState(
    val projects: List<Project> = emptyList(), val project: Project? = null,
    val scene: Scene = Scene(), val saved: Scene = Scene(), val selected: String? = null,
    val canUndo: Boolean = false, val canRedo: Boolean = false,
    val busy: Boolean = false, val error: String? = null, val recovery: Scene? = null, val recoveryIssue: String? = null,
    val assets: List<AssetRecord> = emptyList(), val selectedAsset: String? = null, val frames: List<SpriteFrame> = emptyList(), val assetProgress: String = "",
    val log: List<String> = emptyList()
) { val dirty get() = scene != saved }

/** Main-thread editor state; disk operations use IO dispatching and serialized persistence. */
class EditorViewModel(application: Application): AndroidViewModel(application), AssetActions {
    private var repository: AssetRepository?=null
    private var sheetLoad: Job?=null
    private var frameRevision=0L
    private fun assets() = repository ?: error("Open a project first")
    private val store=ProjectStore(application)
    private val mutable=MutableStateFlow(EditorState())
    val state=mutable.asStateFlow()
    private val history=CommandStack()
    private var dragStart: Scene?=null
    private val persistence=Mutex()
    init {
        refresh()
        viewModelScope.launch { while(isActive) { delay(30_000); autosave() } }
    }
    private fun change(block: (EditorState)->EditorState) { mutable.value=block(mutable.value) }
    fun report(message: String) { change { it.copy(error=message,log=(it.log+message).takeLast(100)) } }
    fun dismissError() { change { it.copy(error=null) } }
    private fun note(message: String) { change { it.copy(log=(it.log+message).takeLast(100)) } }
    private fun task(block: suspend ()->Unit) {
        if(mutable.value.busy) return
        change { it.copy(busy=true) }
        viewModelScope.launch {
            try { block() } catch(e: CancellationException) { throw e }
            catch(e: Exception) { report(e.message ?: e.javaClass.simpleName) }
            finally { change { it.copy(busy=false) } }
        }
    }
    fun refresh() = task {
        val listing=store.list(); change { it.copy(projects=listing.projects) }
        if(listing.errors.isNotEmpty()) report("Some projects could not be opened: " + listing.errors.joinToString("; "))
    }
    private suspend fun enter(open: OpenedProject) {
        val next=AssetRepository(getApplication(),open.project.directory)
        val catalog=next.list()
        repository=next
        history.clear(); dragStart=null
        change { it.copy(project=open.project,scene=open.saved,saved=open.saved,selected=null,canUndo=false,canRedo=false,recovery=open.recovery,recoveryIssue=open.recoveryIssue,assets=catalog,selectedAsset=null,frames=emptyList(),assetProgress="",log=listOf("Opened ${open.project.manifest.name}")) }
    }
    fun open(project: Project)=task { enter(store.open(project)) }
    fun create(name: String, packageId: String, orientation: String, template: String)=task {
        val initial=if(template=="Empty") Scene() else Scene(nodes=listOf(
            sprite(1,Vec2(-130f,0f)),sprite(2,Vec2()),sprite(3,Vec2(130f,0f))))
        val project=store.create(name,packageId,orientation,template,initial)
        enter(OpenedProject(project,initial,null))
    }
    private fun sprite(index: Int, position: Vec2, asset: String?=null): Node {
        val colors=listOf(Color(.25f,.7f,1f),Color(1f,.5f,.3f),Color(.5f,.9f,.4f))
        return Node(name="Sprite $index",components=listOf(TransformComponent(Transform(position)),SpriteComponent(tint=if(asset==null) colors[(index-1)%3] else Color(),asset=asset)))
    }
    private fun finishDrag() {
        val before=dragStart ?: return; dragStart=null
        val s=mutable.value; history.commit(before,s.scene); sync(s.scene)
    }
    private fun sync(scene: Scene) { change { it.copy(scene=scene,canUndo=history.canUndo,canRedo=history.canRedo,selected=it.selected?.takeIf { id -> scene.nodes.any { n -> n.id==id } }) } }
    private fun edit(scene: Scene) {
        if(mutable.value.recovery!=null || mutable.value.recoveryIssue!=null) return
        try { finishDrag(); sync(history.commit(mutable.value.scene,scene)) }
        catch(e: IllegalArgumentException) { report("Edit rejected: ${e.message ?: "Invalid scene"}") }
    }
    fun select(id: String?) { change { it.copy(selected=id) } }
    fun addSprite() {
        if(mutable.value.busy) return
        val s=mutable.value; val node=sprite(s.scene.nodes.size+1,Vec2())
        edit(s.scene.copy(nodes=s.scene.nodes+node)); select(node.id)
    }
    fun importSprite(uri: Uri) = importAsset(uri)
    fun move(id: String, p: Vec2, end: Boolean) {
        if(mutable.value.busy || mutable.value.recovery!=null || mutable.value.recoveryIssue!=null) return
        val s=mutable.value; val node=s.scene.nodes.find { it.id==id } ?: return
        if(dragStart==null) dragStart=s.scene
        change { it.copy(scene=it.scene.replace(Prefabs.override(node.moved(p),"transform"))) }
        if(end) finishDrag()
    }
    fun updateNode(node: Node) {
        val old=mutable.value.scene.nodes.find { it.id==node.id } ?: return
        val fields=buildList { if(old.transform!=node.transform)add("transform"); if(old.name!=node.name)add("name"); if(old.sprite!=node.sprite)add("sprite") }
        edit(mutable.value.scene.replace(Prefabs.override(node,*fields.toTypedArray())))
    }
    fun deleteSelected() { val s=mutable.value; val removed=s.selected?.let { s.scene.descendants(it) }.orEmpty(); edit(s.scene.copy(nodes=s.scene.nodes.filterNot { it.id in removed })) }
    fun undo() { finishDrag(); sync(history.undo(mutable.value.scene)) }
    fun redo() { finishDrag(); sync(history.redo(mutable.value.scene)) }
    fun save()=task {
        persistence.withLock {
            finishDrag(); val s=mutable.value; val project=s.project ?: return@withLock
            store.save(project,s.scene); change { it.copy(saved=s.scene) }; note("Scene saved")
        }
    }
    private suspend fun autosave() = persistence.withLock {
        val s=mutable.value
        if(s.project==null || !s.dirty || s.busy || s.recovery!=null || s.recoveryIssue!=null) return@withLock
        try { store.save(s.project,s.scene,true); note("Recovery snapshot saved") }
        catch(e: CancellationException) { throw e }
        catch(e: Exception) { report("Autosave failed: ${e.message}. Use Save to retry.") }
    }
    fun backgroundSave() { viewModelScope.launch { autosave() } }
    fun recover(restore: Boolean)=task {
        val s=mutable.value; val project=s.project ?: return@task
        if(restore) {
            val recovered=s.recovery ?: return@task
            sync(history.commit(s.scene,recovered)); change { it.copy(recovery=null,recoveryIssue=null) }; note("Recovered snapshot; Save to keep it")
        } else { store.discardRecovery(project); change { it.copy(recovery=null,recoveryIssue=null) } }
    }
    fun close(discard: Boolean)=task {
        persistence.withLock {
            finishDrag(); val s=mutable.value
            s.project?.let { if(discard) store.discardRecovery(it) else store.save(it,s.scene) }
            history.clear(); sheetLoad?.cancel(); repository=null; change { EditorState(busy=true) }
            val listing=store.list(); change { it.copy(projects=listing.projects) }
            if(listing.errors.isNotEmpty()) report(listing.errors.joinToString("; "))
        }
    }

    private suspend fun reloadAssets(selected: String?=mutable.value.selectedAsset) {
        val catalog=assets().list()
        change { it.copy(assets=catalog,selectedAsset=selected?.takeIf { id -> catalog.any { a -> a.id==id } }) }
    }
    override fun assetError(message: String) = report(message)
    override fun selectAsset(id: String) {
        if(mutable.value.busy || mutable.value.selectedAsset==id)return
        sheetLoad?.cancel(); frameRevision++
        val revision=frameRevision
        change { it.copy(selectedAsset=id,frames=emptyList()) }
        sheetLoad=viewModelScope.launch {
            try {
                val frames=assets().sheet(id)
                if(mutable.value.selectedAsset==id && frameRevision==revision)change { it.copy(frames=frames) }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { report("Cannot load sheet layout: ${e.message}") }
        }
    }
    override fun useAsset(id: String) { placeAsset(id,Vec2()) }
    fun placeAsset(id: String,position: Vec2)=task {
        val a=mutable.value.assets.singleOrNull { it.id==id } ?: error("Asset is not in this project")
        val nodes=if(a.kind==AssetKind.PREFAB) { val definitions=assets().definitions(); withContext(Dispatchers.Default) { Prefabs.instantiate(id,definitions,position) } } else listOf(
            Node(name=a.name,components=listOf(TransformComponent(Transform(position)),SpriteComponent(size=Vec2(a.width.toFloat(),a.height.toFloat()),assetId=id))))
        edit(mutable.value.scene.copy(nodes=mutable.value.scene.nodes+nodes)); select(nodes.first { it.parent==null }.id)
    }
    override fun importAsset(uri: Uri)=task {
        val a=assets().importImage(uri); reloadAssets(a.id)
        val n=Node(name=a.name,components=listOf(TransformComponent(),SpriteComponent(size=Vec2(a.width.toFloat(),a.height.toFloat()),assetId=a.id)))
        edit(mutable.value.scene.copy(nodes=mutable.value.scene.nodes+n)); select(n.id); note("Image imported with stable UUID ${a.id}")
    }
    override fun replaceAsset(id: String,uri: Uri)=task { assets().importImage(uri,id); reloadAssets(id); change { it.copy(frames=emptyList()) }; note("Asset replaced; UUID retained. Existing sprite dimensions are preserved.") }
    override fun metadata(id: String,name: String,folder: String,tags: List<String>,favorite: Boolean)=task { assets().metadata(id,name,folder,tags,favorite); reloadAssets(id) }
    override fun prefabFromAsset(id: String)=task {
        val a=mutable.value.assets.single { it.id==id }
        val group=Node(name=a.name,components=listOf(TransformComponent()))
        val child=if(a.kind==AssetKind.PREFAB) Node(name=a.name,parent=group.id,nestedPrefab=a.id,components=listOf(TransformComponent()))
            else Node(name=a.name,parent=group.id,components=listOf(TransformComponent(),SpriteComponent(size=Vec2(a.width.toFloat(),a.height.toFloat()),assetId=a.id)))
        val prefab=assets().savePrefab(PrefabDefinition(nodes=listOf(group,child)),"${a.name.take(78)} prefab")
        reloadAssets(prefab.id)
    }
    override fun duplicateAsset(id: String)=task { val a=assets().duplicate(id); reloadAssets(a.id) }
    override fun deleteAsset(id: String)=task {
        val s=mutable.value
        assets().delete(id,listOfNotNull(s.scene,s.saved,s.recovery)+history.retainedScenes())
        reloadAssets(null); note("Removed unreferenced asset from catalog")
    }
    override fun editTexture(id: String,edit: TextureEdit)=task { val a=assets().editImage(id,edit); reloadAssets(a.id); change { it.copy(frames=emptyList()) }; note("Created edited image; original unchanged") }
    override fun autoSlice(id: String)=task { val frames=assets().autoFrames(id); change { it.copy(frames=frames) } }
    override fun setFrames(frames: List<SpriteFrame>) {
        frameRevision++
        if(frames.size>256) { report("Use at most 256 frames"); return }
        change { it.copy(frames=frames) }
    }
    override fun appendFrame(frame: SpriteFrame) { setFrames(mutable.value.frames+frame) }
    override fun saveSheet(id: String)=task { assets().saveSheet(id,mutable.value.frames); note("Named sheet layout saved") }
    override fun exportFrames(id: String,uri: Uri)=task { assets().exportFrames(id,mutable.value.frames,uri); note("Exported ordered PNG frames and frames.json") }
    override fun extractFrames(id: String)=task {
        try {
            val extracted=assets().slice(id,mutable.value.frames)
            note("Extracted ${extracted.size} PNG assets in frame-list order. Export individual PNGs from their inspector.")
        } finally { reloadAssets() }
    }
    override fun splitAsset(id: String)=task {
        var selected=mutable.value.selectedAsset
        try {
            val prefab=assets().split(id,mutable.value.frames); selected=prefab.id; change { it.copy(frames=emptyList()) }
            note("Created ${prefab.name}. Use it to place editable child parts.")
        } finally { reloadAssets(selected) }
    }
    override fun exportAsset(id: String,uri: Uri)=task { assets().export(id,uri); note("PNG exported to chosen document") }
    override fun generateLibrary()=task {
        try {
            assets().generateLibrary { count,total -> viewModelScope.launch { change { it.copy(assetProgress="Generating $count / $total") } } }
        } finally { reloadAssets(); change { it.copy(assetProgress="") } }
        note("Procedural library is ready: 335 original recipes. Existing recipes were reused.")
    }
    fun createPrefabFromSelection()=task {
        val s=mutable.value;val root=s.scene.nodes.singleOrNull { it.id==s.selected } ?: error("Select an entity first")
        val binding=root.prefab
        val definition=if(binding?.instanceRoot==root.id) {
            val group=Node(name="${root.name} group",components=listOf(TransformComponent()))
            val nested=Node(name=root.name,parent=group.id,nestedPrefab=binding.assetId,components=listOf(TransformComponent(root.transform.copy(position=Vec2()))))
            PrefabDefinition(nodes=listOf(group,nested))
        } else Prefabs.capture(s.scene,root.id)
        val a=assets().savePrefab(definition,"${root.name.take(80)} prefab"); reloadAssets(a.id); note("Prefab captured; original scene unchanged")
    }
    fun updatePrefabSource()=task {
        val s=mutable.value; val selected=s.scene.nodes.single { it.id==s.selected }
        val binding=selected.prefab ?: error("Select a prefab instance")
        val root=s.scene.nodes.single { it.id==binding.instanceRoot }
        val record=s.assets.single { it.id==binding.assetId }
        require(s.scene.nodes.none { it.prefab?.let { link -> link.instanceRoot==root.id && link.sourcePath.contains('/') && link.overrides.isNotEmpty() } == true }) {
            "Nested-child overrides are scene-local. Edit the nested prefab as a separate instance to update its source, or revert those overrides before updating the outer prefab."
        }
        assets().savePrefab(Prefabs.capture(s.scene,root.id),record.name,record.id)
        reloadAssets(record.id); note("Prefab source updated. Refresh other instances to apply it.")
    }
    fun refreshPrefab(preserve: Boolean)=task {
        val s=mutable.value; val selected=s.scene.nodes.single { it.id==s.selected }
        val binding=selected.prefab ?: error("Select a prefab instance")
        val root=s.scene.nodes.single { it.id==binding.instanceRoot }; val ids=s.scene.descendants(root.id)
        val definitions=assets().definitions()
        val replacement=withContext(Dispatchers.Default) { Prefabs.instantiate(binding.assetId,definitions,root.transform.position,s.scene.nodes.filter { it.id in ids },preserve) }
        val at=s.scene.nodes.indexOfFirst { it.id in ids }
        val remaining=s.scene.nodes.filterNot { it.id in ids }.toMutableList()
        remaining.addAll(at.coerceAtMost(remaining.size),replacement)
        edit(s.scene.copy(nodes=remaining)); select(replacement.first { it.parent==null }.id)
    }
}

`````

## editor-ui/src/main/kotlin/world/engine/editor/WorldEditorApp.kt

`````kotlin
package world.engine.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import world.engine.core.*
import world.engine.io.Project
import world.engine.math.Transform
import world.engine.math.Vec2
import world.engine.viewport.WorldViewport
import world.engine.asseteditor.AssetBrowser

@Composable fun WorldEditorApp(vm: EditorViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var wizard by remember { mutableStateOf(false) }
    var exit by remember { mutableStateOf(false) }
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_STOP) vm.backgroundSave() }
        owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
    }
    BackHandler(s.project!=null && !s.busy) { if(s.recovery==null && s.recoveryIssue==null) exit=true }
    MaterialTheme(colorScheme=darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                if(s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(s.project==null) Dashboard(s.projects,s.busy,{wizard=true},vm::open,vm::refresh)
                else EditorShell(s,vm) { exit=true }
            }
        }
        if(wizard && s.project==null) ProjectWizard(s.busy,{wizard=false}) { name,pkg,orientation,template -> vm.create(name,pkg,orientation,template) }
        LaunchedEffect(s.project) { if(s.project!=null) wizard=false }
        s.error?.let { message -> AlertDialog(onDismissRequest=vm::dismissError,title={Text("Action needs attention")},text={Text(message)},confirmButton={TextButton(onClick=vm::dismissError){Text("OK")}}) }
        if(s.recovery!=null || s.recoveryIssue!=null) AlertDialog(onDismissRequest={},title={Text("Recover unsaved scene?")},text={Text(s.recoveryIssue ?: "A recovery snapshot differs from the saved scene. Restore it, or keep the last explicit save. Restoring does not overwrite your saved file until you save.")},confirmButton={TextButton(enabled=!s.busy && s.recovery!=null,onClick={vm.recover(true)}){Text("Restore")}},dismissButton={TextButton(enabled=!s.busy,onClick={vm.recover(false)}){Text("Keep saved")}})
        if(exit) AlertDialog(onDismissRequest={exit=false},title={Text("Return to projects?")},text={Text(if(s.dirty) "Your scene has unsaved changes." else "The scene is saved.")},confirmButton={TextButton(enabled=!s.busy,onClick={exit=false;vm.close(false)}){Text("Save & close")}},dismissButton={Row { TextButton(onClick={exit=false}){Text("Cancel")}; TextButton(enabled=!s.busy,onClick={exit=false;vm.close(true)}){Text("Discard & close")} }})
    }
}

@Composable private fun Dashboard(projects: List<Project>, busy: Boolean, create: ()->Unit, open: (Project)->Unit, refresh: ()->Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("2D WORLD",style=MaterialTheme.typography.headlineMedium)
        Text("Projects · Asset & scene editor",style=MaterialTheme.typography.bodyMedium)
        Row { Button(enabled=!busy,onClick=create){Text("New project")}; TextButton(enabled=!busy,onClick=refresh){Text("Refresh")} }
        if(projects.isEmpty()) Text("Create your first project. Your scenes and images stay on this device.",Modifier.padding(vertical=24.dp))
        LazyVerticalGrid(columns=GridCells.Adaptive(220.dp),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            items(projects,key={it.manifest.id}) { project ->
                OutlinedCard(onClick={open(project)},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) { Text(project.manifest.name,style=MaterialTheme.typography.titleLarge); Text(project.manifest.packageId); Text(project.manifest.orientation); Text("Open scene →",Modifier.padding(top=12.dp)) }
                }
            }
        }
    }
}

@Composable private fun ProjectWizard(busy: Boolean, dismiss: ()->Unit, create: (String,String,String,String)->Unit) {
    var name by remember { mutableStateOf("MyGame") }
    var pkg by remember { mutableStateOf("com.example.mygame") }
    var orientation by remember { mutableStateOf("Landscape") }
    var template by remember { mutableStateOf("Empty") }
    AlertDialog(onDismissRequest={if(!busy)dismiss()},title={Text("Create project")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(name,{name=it},label={Text("Project name")},singleLine=true)
        OutlinedTextField(pkg,{pkg=it},label={Text("Package ID")},singleLine=true)
        Text("Game orientation",Modifier.padding(top=12.dp))
        Row { listOf("Portrait","Landscape").forEach { value -> FilterChip(selected=orientation==value,onClick={orientation=value},label={Text(value)}) } }
        Text("Template")
        Row { listOf("Empty","Three sprites").forEach { value -> FilterChip(selected=template==value,onClick={template=value},label={Text(value)}) } }
        Text("Stored privately on this device. No internet or storage permission needed.")
    }},confirmButton={TextButton(enabled=!busy,onClick={create(name,pkg,orientation,template)}){Text("Create")}},dismissButton={TextButton(enabled=!busy,onClick=dismiss){Text("Cancel")}})
}

@Composable private fun EditorShell(s: EditorState, vm: EditorViewModel, exit: ()->Unit) {
    val project=s.project ?: return
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val viewport=remember(project.manifest.id) { WorldViewport(context) }
    var delete by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf("Hierarchy") }
    var reset by remember { mutableIntStateOf(0) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) vm.importSprite(uri) }
    DisposableEffect(viewport,owner) {
        val observer=LifecycleEventObserver { _,event -> when(event) {
            Lifecycle.Event.ON_RESUME -> viewport.onResume()
            Lifecycle.Event.ON_PAUSE -> viewport.onPause()
            else -> Unit
        } }
        owner.lifecycle.addObserver(observer)
        if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) viewport.onResume()
        onDispose { owner.lifecycle.removeObserver(observer); viewport.onPause() }
    }
    LaunchedEffect(reset) { viewport.resetCamera() }
    Column(Modifier.fillMaxSize()) {
        Text(project.manifest.name + if(s.dirty) " • unsaved" else " • saved",Modifier.padding(horizontal=12.dp,vertical=4.dp),style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            TextButton(enabled=!s.busy,onClick=exit){Text("Projects")}
            TextButton(enabled=!s.busy,onClick=vm::save){Text("Save")}
            TextButton(enabled=s.canUndo && !s.busy,onClick=vm::undo){Text("Undo")}
            TextButton(enabled=s.canRedo && !s.busy,onClick=vm::redo){Text("Redo")}
            TextButton(enabled=!s.busy,onClick=vm::addSprite){Text("Add sprite")}
            TextButton(enabled=!s.busy,onClick={picker.launch(arrayOf("image/*"))}){Text("Import image")}
            TextButton(onClick={panel="Assets"}){Text("Assets")}
            TextButton(onClick={reset++}){Text("Reset view")}
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            val wide=maxWidth>=840.dp
            Column {
                Row(Modifier.weight(1f)) {
                    if(wide) Box(Modifier.width(200.dp).fillMaxHeight()) { Hierarchy(s,vm) }
                    AndroidView(factory={viewport},modifier=Modifier.weight(1f).fillMaxHeight(),update={ view ->
                        view.onSelect=vm::select; view.onMove=vm::move; view.onError=vm::report; view.onAssetDrop=vm::placeAsset
                        view.onContext={id -> vm.select(id); if(id!=null) { panel="Inspector"; delete=true } }
                        view.isEnabled=!s.busy && s.recovery==null && s.recoveryIssue==null
                        view.update(s.scene,s.selected,project.directory,s.assets.filter { it.kind==world.engine.assets.AssetKind.IMAGE }.associate { it.id to it.path })
                    })
                    if(wide) Box(Modifier.width(250.dp).fillMaxHeight()) { Inspector(s,vm){delete=true} }
                }
                if(wide) {
                    Row { TextButton(onClick={panel="Assets"}){Text("Assets")}; TextButton(onClick={panel="Console"}){Text("Console")} }
                    if(panel=="Assets") AssetBrowser(s.assets,s.selectedAsset,s.frames,project.directory,s.busy,s.assetProgress,vm,Modifier.fillMaxWidth().height((maxHeight*.4f).coerceAtMost(320.dp)))
                    else Console(s,Modifier.fillMaxWidth().height(96.dp))
                } else {
                    Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("Hierarchy","Inspector","Assets","Console","Viewport").forEach { label -> TextButton(onClick={panel=label}){Text(if(panel==label) "[$label]" else label)} } }
                    if(panel!="Viewport") Box(Modifier.fillMaxWidth().height((maxHeight*.5f).coerceAtMost(320.dp))) { when(panel) { "Hierarchy" -> Hierarchy(s,vm); "Inspector" -> Inspector(s,vm){delete=true}; "Assets" -> AssetBrowser(s.assets,s.selectedAsset,s.frames,project.directory,s.busy,s.assetProgress,vm,Modifier.fillMaxSize()); else -> Console(s,Modifier.fillMaxSize()) } }
                }
            }
        }
        Text("Drag: move / pan   •   Pinch: zoom   •   Hold: actions",Modifier.padding(6.dp),style=MaterialTheme.typography.labelSmall)
    }
    if(delete && s.selected!=null) AlertDialog(onDismissRequest={delete=false},title={Text("Delete entity and its children?")},text={Text("This edit can be undone. Asset files are kept.")},confirmButton={TextButton(enabled=!s.busy,onClick={delete=false;vm.deleteSelected()}){Text("Delete")}},dismissButton={TextButton(onClick={delete=false}){Text("Cancel")}})
}

@Composable private fun Hierarchy(s: EditorState, vm: EditorViewModel) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Text("HIERARCHY · ${s.scene.nodes.size}",style=MaterialTheme.typography.labelLarge)
        OutlinedTextField(query,{query=it},label={Text("Find entity")},singleLine=true,modifier=Modifier.fillMaxWidth())
        LazyColumn { items(s.scene.nodes.filter { it.name.contains(query,true) },key={it.id}) { node -> TextButton(enabled=!s.busy,onClick={vm.select(node.id)},modifier=Modifier.fillMaxWidth()){Text((if(node.parent!=null) "↳ " else "")+(if(node.id==s.selected) "● " else "○ ")+node.name)} } }
        if(s.scene.nodes.isEmpty()) Text("Use Add sprite or Import image.")
    }
}

@Composable private fun Inspector(s: EditorState, vm: EditorViewModel, delete: ()->Unit) {
    val node=s.scene.nodes.find { it.id==s.selected }
    if(node==null) { Text("Select a sprite to edit its properties.",Modifier.padding(12.dp)); return }
    var prefabAction by remember(node.id) { mutableStateOf<String?>(null) }
    key(node) {
        var name by remember { mutableStateOf(node.name) }
        var x by remember { mutableStateOf(node.transform.position.x.toString()) }
        var y by remember { mutableStateOf(node.transform.position.y.toString()) }
        var rotation by remember { mutableStateOf(node.transform.rotation.toString()) }
        var sx by remember { mutableStateOf(node.transform.scale.x.toString()) }
        var sy by remember { mutableStateOf(node.transform.scale.y.toString()) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
            Text("INSPECTOR",style=MaterialTheme.typography.labelLarge)
            Property("Name",name){name=it}; Property("X",x){x=it}; Property("Y",y){y=it}
            Property("Rotation (degrees)",rotation){rotation=it}; Property("Scale X",sx){sx=it}; Property("Scale Y",sy){sy=it}
            Button(enabled=!s.busy,onClick={
                val values=listOf(x,y,rotation,sx,sy).map { it.toFloatOrNull() }
                if(name.isBlank() || values.any { it==null || !it.isFinite() } || kotlin.math.abs(values[3] ?: 0f)<.001f || kotlin.math.abs(values[4] ?: 0f)<.001f) vm.report("Enter a name, finite numeric properties, and non-zero scales.")
                else {
                    val t=Transform(Vec2(values[0]!!,values[1]!!),values[2]!!,Vec2(values[3]!!,values[4]!!))
                    vm.updateNode(node.copy(name=name,components=node.components.map { if(it is TransformComponent) TransformComponent(t) else it }))
                }
            }){Text("Apply properties")}
            Text("ID: ${node.id}",style=MaterialTheme.typography.labelSmall)
            Text(node.sprite?.assetId ?: node.sprite?.asset ?: if(node.sprite==null) "Hierarchy group" else "Built-in solid sprite",style=MaterialTheme.typography.labelSmall)
            TextButton(enabled=!s.busy,onClick=vm::createPrefabFromSelection){Text("Create prefab")}
            node.prefab?.let { binding ->
                Text("Prefab overrides: ${binding.overrides.joinToString().ifEmpty { "none" }}",style=MaterialTheme.typography.labelSmall)
                TextButton(enabled=!s.busy,onClick={prefabAction="Refresh instance"}){Text("Refresh instance")}
                TextButton(enabled=!s.busy,onClick={prefabAction="Revert overrides"}){Text("Revert overrides")}
                TextButton(enabled=!s.busy,onClick={prefabAction="Update prefab source"}){Text("Update prefab source")}
            }
            TextButton(enabled=!s.busy,onClick=delete){Text("Delete entity")}
        }
    }
    prefabAction?.let { action -> AlertDialog(onDismissRequest={prefabAction=null},title={Text("$action?")},text={Text(when(action) { "Revert overrides" -> "Reset this instance from its source, including its children. This scene edit can be undone."; "Refresh instance" -> "Rebuild this subtree from its prefab source while preserving property overrides. Locally added or removed children are not structural overrides and may be discarded or restored. This scene edit can be undone."; else -> "Replace the prefab source with this instance. Other instances receive changes when refreshed; source updates are not scene undo commands." })},confirmButton={TextButton(enabled=!s.busy,onClick={prefabAction=null;when(action) { "Revert overrides" -> vm.refreshPrefab(false); "Refresh instance" -> vm.refreshPrefab(true); else -> vm.updatePrefabSource() }}){Text("Confirm")}},dismissButton={TextButton(onClick={prefabAction=null}){Text("Cancel")}}) }
}
@Composable private fun Property(label: String,value: String,set: (String)->Unit) { OutlinedTextField(value,set,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth()) }
@Composable private fun Console(s: EditorState, modifier: Modifier) {
    Column(modifier.padding(8.dp)) { Text("CONSOLE",style=MaterialTheme.typography.labelLarge); LazyColumn { items(s.log.asReversed()) { Text(it,style=MaterialTheme.typography.bodySmall) } } }
}

`````

## editor-viewport/src/main/kotlin/world/engine/viewport/WorldViewport.kt

`````kotlin
package world.engine.viewport

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.DragEvent
import android.view.ScaleGestureDetector
import world.engine.core.Scene
import world.engine.math.Vec2
import world.engine.render.*
import java.io.File

/** One-finger sprite drag or empty-space pan; two-finger focal-point zoom; long-press selection. */
class WorldViewport(context: Context): GLSurfaceView(context) {
    var onSelect: (String?)->Unit = {}
    var onMove: (String,Vec2,Boolean)->Unit = { _,_,_-> }
    var onContext: (String?)->Unit = {}
    var onError: (String)->Unit = {}
    var onAssetDrop: (String,Vec2)->Unit = { _,_ -> }
    private var assetPaths: Map<String,String> = emptyMap()
    private val renderer=SceneRenderer { message -> post { onError(message) } }
    private var scene=Scene()
    private var selected: String?=null
    private var directory: File?=null
    private var camera=Camera2D()
    private var dragging: String?=null
    private var offset=Vec2()
    private var last=Vec2()
    private var multiple=false
    private var longPressed=false
    private val scale=ScaleGestureDetector(context,object: ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val before=world(detector.focusX,detector.focusY)
            camera=camera.copy(zoom=(camera.zoom*detector.scaleFactor).coerceIn(.15f,8f))
            camera=camera.copy(center=camera.center+before-world(detector.focusX,detector.focusY)); publish(); return true
        }
    })
    private val gestures=GestureDetector(context,object: GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent)=true
        override fun onLongPress(e: MotionEvent) {
            if(!multiple) { finish(); longPressed=true; onContext(scene.pick(world(e.x,e.y))?.id) }
        }
    })
    init {
        setEGLContextClientVersion(3); preserveEGLContextOnPause=true; setRenderer(renderer); renderMode=RENDERMODE_WHEN_DIRTY
        setOnDragListener { _, event ->
            when(event.action) {
                DragEvent.ACTION_DRAG_STARTED -> isEnabled && event.clipDescription?.label=="2DWorldAsset"
                DragEvent.ACTION_DROP -> {
                    if(isEnabled) event.clipData?.getItemAt(0)?.text?.toString()?.let { onAssetDrop(it,world(event.x,event.y)) }
                    true
                }
                else -> true
            }
        }
        contentDescription="Scene viewport. Drag sprites to move; drag empty space to pan; pinch to zoom; long press for actions."
    }
    fun update(scene: Scene, selected: String?, directory: File?, assetPaths: Map<String,String> = emptyMap()) {
        this.assetPaths=assetPaths
        if(this.directory!=directory) camera=Camera2D()
        this.scene=scene; this.selected=selected; this.directory=directory; publish()
    }
    fun resetCamera() { camera=Camera2D(); publish() }
    private fun publish() { renderer.frame=RenderFrame(scene,selected,camera,directory,assetPaths); requestRender() }
    private fun world(x: Float,y: Float)=camera.screenToWorld(x,y,width,height)
    private fun finish() {
        dragging?.let { id -> scene.nodes.find { it.id==id }?.let { onMove(id,it.transform.position,true) } }; dragging=null
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(!isEnabled) return true
        parent?.requestDisallowInterceptTouchEvent(true)
        scale.onTouchEvent(e); gestures.onTouchEvent(e)
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                multiple=false; longPressed=false; last=Vec2(e.x,e.y)
                val point=world(e.x,e.y); val node=scene.pick(point)
                dragging=node?.id; offset=point-(node?.let { scene.worldMatrices().getValue(it.id).map(Vec2()) } ?: point); onSelect(node?.id)
            }
            MotionEvent.ACTION_POINTER_DOWN -> { finish(); multiple=true }
            MotionEvent.ACTION_MOVE -> if(!multiple && !longPressed) {
                val id=dragging
                if(id!=null) {
                    val worldPosition=world(e.x,e.y)-offset
                    val node=scene.nodes.find { it.id==id }
                    val p=node?.parent?.let { scene.worldMatrices().getValue(it).inverse().map(worldPosition) } ?: worldPosition
                    scene.nodes.find { it.id==id }?.let { scene=scene.replace(it.moved(p)) }; publish(); onMove(id,p,false)
                } else { camera=camera.copy(center=camera.center+Vec2((last.x-e.x)/camera.zoom,(e.y-last.y)/camera.zoom)); publish() }
                last=Vec2(e.x,e.y)
            }
            MotionEvent.ACTION_UP -> { finish(); performClick() }
            MotionEvent.ACTION_CANCEL -> finish()
        }
        return true
    }
}

`````

## engine-assets/README.md

`````markdown
# engine-assets — Phase 2 implementation

SQLite UUID catalog, immutable PNG/prefab revisions, dependency protection, phase-1 PNG indexing, bitmap operations, named sprite-sheet layouts, PNG/ZIP export, modular splitting, and a 335-recipe original procedural generator.

All repository operations dispatch to IO and serialize through a per-project mutex. A payload is written and fsynced before a transaction publishes its path. Old payload revisions remain on disk; deletion removes only unreferenced catalog records and leaves tombstones for legacy images. Full asset-operation undo and garbage collection are not implemented.

See `docs/PHASE2.md` for exact scope, tests and limitations. Standalone compilation is not Android runtime verification.

`````

## engine-assets/build.gradle.kts

`````kotlin
plugins { id("com.android.library"); kotlin("android"); kotlin("plugin.serialization") }
android {
    namespace = "world.engine.engineassets"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    api(project(":engine-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

`````

## engine-assets/src/androidTest/kotlin/world/engine/assets/AssetPipelineTest.kt

`````kotlin
package world.engine.assets

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import world.engine.core.*
import java.io.File
import java.util.UUID

class AssetPipelineTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun workspace()=File(context.cacheDir,"asset-test-${UUID.randomUUID()}").apply { mkdirs() }
    private fun png(root: File,name: String,color: Int): File {
        val bitmap=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888); bitmap.eraseColor(color)
        return File(root,name).also { file -> try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } } finally { bitmap.recycle() } }
    }
    @Test fun stableUuidRenameMoveReplaceDuplicateAndReferenceProtection() = runBlocking {
        val root=workspace()
        try {
            val repo=AssetRepository(context,root)
            val original=repo.importImage(Uri.fromFile(png(root,"input.png",Color.RED)))
            repo.metadata(original.id,"Hero","Actors/Players",listOf("player","red"),true)
            val moved=repo.list().single(); assertEquals(original.id,moved.id); assertEquals(original.path,moved.path)
            assertEquals("Hero",moved.name); assertTrue(moved.favorite)
            repo.saveSheet(moved.id,listOf(SpriteFrame("whole",0,0,16,16)))
            val replaced=repo.importImage(Uri.fromFile(png(root,"new.png",Color.BLUE)),original.id)
            assertEquals(original.id,replaced.id); assertNotEquals(original.path,replaced.path); assertEquals(2,replaced.revision)
            assertTrue(repo.file(original).exists()); assertTrue(repo.sheet(moved.id).isEmpty())
            val duplicate=repo.duplicate(original.id); assertNotEquals(original.id,duplicate.id)
            val scene=Scene(nodes=listOf(Node(components=listOf(TransformComponent(),SpriteComponent(assetId=original.id)))))
            try { repo.delete(original.id,listOf(scene)); fail("Referenced image deleted") } catch(expected: IllegalArgumentException) { assertTrue(expected.message!!.contains("used")) }
            val prefab=repo.savePrefab(Prefabs.capture(scene,scene.nodes[0].id),"Hero prefab")
            try { repo.delete(original.id,emptyList()); fail("Prefab dependency deleted") } catch(expected: IllegalArgumentException) { assertTrue(expected.message!!.contains("prefabs")) }
            repo.delete(prefab.id,emptyList()); repo.delete(original.id,emptyList())
            assertFalse(AssetRepository(context,root).list().any { it.id==original.id })
        } finally { root.deleteRecursively() }
    }
    @Test fun pixelOperationsAndSlicingPreserveContent() {
        val image=Bitmap.createBitmap(8,4,Bitmap.Config.ARGB_8888)
        image.setPixel(1,1,Color.RED); image.setPixel(6,2,Color.BLUE)
        try {
            val frames=SpriteSlicer.auto(image); assertEquals(2,frames.size)
            assertEquals(SpriteFrame("Part 1",1,1,1,1),frames[0])
            assertEquals(4,SpriteSlicer.grid(8,4,4,2).size)
            val flip=ImageOps.apply(image,TextureEdit("Flip X"))
            try { assertEquals(Color.RED,flip.getPixel(6,1)) } finally { flip.recycle() }
            val crop=ImageOps.apply(image,TextureEdit("Crop",1,1,1,1))
            try { assertEquals(Color.RED,crop.getPixel(0,0)) } finally { crop.recycle() }
        } finally { image.recycle() }
    }
    @Test fun generationCountIdempotenceAndTrueModularVehicleSplit() = runBlocking {
        val root=workspace()
        try {
            val repo=AssetRepository(context,root); repo.generateLibrary { _,_-> }
            val catalog=repo.list(); assertEquals(335,catalog.size)
            repo.generateLibrary { _,_-> }; assertEquals(catalog.map { it.id }.toSet(),repo.list().map { it.id }.toSet())
            val car=catalog.single { it.recipe=="Vehicle:1" }
            val split=repo.split(car.id,emptyList())
            val definition=repo.definitions().getValue(split.id)
            assertEquals(18,definition.nodes.size)
            assertEquals(4,definition.nodes.count { it.name.startsWith("Wheel") })
            assertTrue(definition.nodes.any { it.name=="Interior" })
            val nodes=Prefabs.instantiate(split.id,repo.definitions()); Scene(nodes=nodes).validated()
            assertEquals(17,nodes.count { it.parent!=null })
        } finally { root.deleteRecursively() }
    }
    @Test fun layoutsPersistOrderingAndRejectedCyclesRollback() = runBlocking {
        val root=workspace()
        try {
            val repo=AssetRepository(context,root); val image=repo.importImage(Uri.fromFile(png(root,"source.png",Color.WHITE)))
            val frames=listOf(SpriteFrame("Second",8,0,8,16),SpriteFrame("First",0,0,8,16))
            repo.saveSheet(image.id,frames); assertEquals(frames,AssetRepository(context,root).sheet(image.id))
            val exported=File(root,"frames.zip")
            repo.exportFrames(image.id,frames,Uri.fromFile(exported))
            java.util.zip.ZipFile(exported).use { zip ->
                assertEquals(3,zip.size()); assertNotNull(zip.getEntry("frame-1.png")); assertNotNull(zip.getEntry("frame-2.png"))
                val manifest=zip.getInputStream(zip.getEntry("frames.json")).bufferedReader().use { it.readText() }
                assertTrue(manifest.indexOf("Second")<manifest.indexOf("First"))
            }
            val node=Node(name="Base"); val base=repo.savePrefab(PrefabDefinition(nodes=listOf(node)),"Base")
            val nested=repo.savePrefab(PrefabDefinition(nodes=listOf(Node(nestedPrefab=base.id,components=listOf(TransformComponent())))),"Nested")
            try { repo.savePrefab(PrefabDefinition(nodes=listOf(Node(nestedPrefab=nested.id,components=listOf(TransformComponent())))),"Base",base.id); fail("Cycle allowed") } catch(expected: IllegalArgumentException) { assertTrue(expected.message!!.contains("cycle")) }
            assertEquals(base.path,repo.list().single { it.id==base.id }.path)
        } finally { root.deleteRecursively() }
    }
    @Test fun generatedTextureEdgesMatch() {
        ProceduralAssets.recipes().filter { it.startsWith("Texture:") }.forEach { recipe ->
            val image=ProceduralAssets.render(recipe)
            try { repeat(64) { p -> assertEquals(image.getPixel(0,p),image.getPixel(63,p)); assertEquals(image.getPixel(p,0),image.getPixel(p,63)) } } finally { image.recycle() }
        }
    }
}

`````

## engine-assets/src/main/kotlin/world/engine/assets/AssetDatabase.kt

`````kotlin
package world.engine.assets

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable enum class AssetKind { IMAGE, PREFAB }
@Serializable data class AssetRecord(
    val id: String, val name: String, val folder: String, val kind: AssetKind,
    val path: String, val revision: Int=1, val width: Int=0, val height: Int=0,
    val tags: List<String> = emptyList(), val favorite: Boolean=false, val recipe: String?=null
)

/** SQLite owns metadata and UUID dependency edges; immutable files hold payload revisions. */
internal class AssetDatabase(context: Context, root: File): SQLiteOpenHelper(context,File(root,"Resources/assets.sqlite").absolutePath,null,1) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE assets(id TEXT PRIMARY KEY, name TEXT NOT NULL, folder TEXT NOT NULL, kind TEXT NOT NULL, path TEXT NOT NULL, revision INTEGER NOT NULL, width INTEGER NOT NULL, height INTEGER NOT NULL, tags TEXT NOT NULL, favorite INTEGER NOT NULL, recipe TEXT)")
        db.execSQL("CREATE TABLE tombstones(id TEXT PRIMARY KEY)")
        db.execSQL("CREATE UNIQUE INDEX asset_recipe ON assets(recipe) WHERE recipe IS NOT NULL")
        db.execSQL("CREATE TABLE edges(owner TEXT NOT NULL REFERENCES assets(id) ON DELETE CASCADE, target TEXT NOT NULL REFERENCES assets(id) ON DELETE RESTRICT, PRIMARY KEY(owner,target))")
        db.execSQL("CREATE TABLE sheets(owner TEXT PRIMARY KEY REFERENCES assets(id) ON DELETE CASCADE, layout TEXT NOT NULL)")
        db.execSQL("CREATE INDEX edges_target ON edges(target)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Unsupported asset database upgrade: $oldVersion → $newVersion. Back up this project.") }
}

internal object Catalog {
    private val json=Json
    fun list(db: SQLiteDatabase): List<AssetRecord> = db.rawQuery("SELECT id,name,folder,kind,path,revision,width,height,tags,favorite,recipe FROM assets ORDER BY folder,name,id",null).use { c ->
        buildList { while(c.moveToNext()) add(AssetRecord(c.getString(0),c.getString(1),c.getString(2),AssetKind.valueOf(c.getString(3)),c.getString(4),c.getInt(5),c.getInt(6),c.getInt(7),json.decodeFromString<List<String>>(c.getString(8)),c.getInt(9)!=0,if(c.isNull(10)) null else c.getString(10))) }
    }
    fun put(db: SQLiteDatabase, a: AssetRecord) {
        val values=ContentValues().apply {
            put("id",a.id); put("name",a.name); put("folder",a.folder); put("kind",a.kind.name); put("path",a.path)
            put("revision",a.revision); put("width",a.width); put("height",a.height); put("tags",json.encodeToString(a.tags)); put("favorite",if(a.favorite)1 else 0); put("recipe",a.recipe)
        }
        if(db.update("assets",values,"id=?",arrayOf(a.id))==0) db.insertOrThrow("assets",null,values)
    }
    fun dependencies(db: SQLiteDatabase, owner: String): Set<String> = db.rawQuery("SELECT target FROM edges WHERE owner=?",arrayOf(owner)).use { c -> buildSet { while(c.moveToNext()) add(c.getString(0)) } }
    fun setDependencies(db: SQLiteDatabase, owner: String, targets: Set<String>) {
        fun reaches(start: String): Boolean {
            val pending=ArrayDeque<String>(); pending.add(start); val seen=mutableSetOf<String>()
            while(pending.isNotEmpty()) { val current=pending.removeFirst(); if(current==owner) return true; if(seen.add(current)) dependencies(db,current).forEach { pending.add(it) } }
            return false
        }
        require(targets.none { reaches(it) }) { "Prefab dependency cycle rejected" }
        db.delete("edges","owner=?",arrayOf(owner))
        targets.forEach { target -> db.execSQL("INSERT INTO edges(owner,target) VALUES(?,?)",arrayOf(owner,target)) }
    }
}

`````

## engine-assets/src/main/kotlin/world/engine/assets/AssetRepository.kt

`````kotlin
package world.engine.assets

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import world.engine.core.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Per-project operations are serialized on IO; database commits publish already-durable payloads. */
class AssetRepository(private val context: Context, val root: File) {
    private val mutex=Mutex()
    private val json=Json { prettyPrint=true }
    private suspend fun <T> work(block: (SQLiteDatabase)->T): T = withContext(Dispatchers.IO) {
        mutex.withLock { File(root,"Resources").mkdirs(); AssetDatabase(context,root).use { block(it.writableDatabase) } }
    }
    private fun <T> transaction(db: SQLiteDatabase, block: ()->T): T {
        db.beginTransaction()
        try { val result=block(); db.setTransactionSuccessful(); return result } finally { db.endTransaction() }
    }
    fun file(asset: AssetRecord): File {
        val file=File(root,asset.path)
        require(file.canonicalPath.startsWith(root.canonicalPath+File.separator)) { "Unsafe asset path" }
        return file
    }
    private fun get(db: SQLiteDatabase,id: String) = Catalog.list(db).singleOrNull { it.id==id } ?: error("Asset no longer exists: $id")
    suspend fun list(): List<AssetRecord> = work { db ->
        // Phase 1 UUID PNGs remain valid without modifying existing scene JSON.
        val deleted=db.rawQuery("SELECT id FROM tombstones",null).use { c -> buildSet { while(c.moveToNext())add(c.getString(0)) } }
        val known=Catalog.list(db).map { it.id }.toSet()+deleted
        File(root,"Sprites").listFiles().orEmpty().filter { it.extension=="png" && Scene.isAssetId(it.nameWithoutExtension) && it.nameWithoutExtension !in known }.forEach { f ->
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }; BitmapFactory.decodeFile(f.path,bounds)
            if(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048) Catalog.put(db,AssetRecord(f.nameWithoutExtension,"Imported ${f.nameWithoutExtension.take(8)}","Imported",AssetKind.IMAGE,"Sprites/${f.name}",width=bounds.outWidth,height=bounds.outHeight))
        }
        Catalog.list(db)
    }
    private fun validateMetadata(name: String,folder: String,tags: List<String>) {
        require(name.isNotBlank() && name.length<=96) { "Name must be 1–96 characters" }
        require(folder.length<=128 && folder.split('/').none { it==".." || it=="." } && !folder.startsWith('/')) { "Use a relative catalog folder" }
        require(tags.size<=32 && tags.all { it.length in 1..32 }) { "Use up to 32 tags of 1–32 characters" }
    }
    suspend fun metadata(id: String,name: String,folder: String,tags: List<String>,favorite: Boolean) = work { db ->
        validateMetadata(name,folder,tags)
        val a=get(db,id); Catalog.put(db,a.copy(name=name.trim(),folder=folder.trim('/'),tags=tags.distinct(),favorite=favorite))
    }
    private fun writeBitmap(db: SQLiteDatabase,bitmap: Bitmap,name: String,folder: String,prior: AssetRecord?=null,recipe: String?=null): AssetRecord {
        require(bitmap.width in 1..2048 && bitmap.height in 1..2048)
        validateMetadata(name,folder,emptyList())
        val id=prior?.id ?: UUID.randomUUID().toString()
        val path="Sprites/$id-${UUID.randomUUID()}.png"
        val result=prior?.copy(path=path,revision=prior.revision+1,width=bitmap.width,height=bitmap.height,recipe=null)
            ?: AssetRecord(id,name,folder,AssetKind.IMAGE,path,width=bitmap.width,height=bitmap.height,recipe=recipe)
        val target=file(result); target.parentFile?.mkdirs()
        try {
            FileOutputStream(target).use { require(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)); it.fd.sync() }
            transaction(db) { Catalog.put(db,result); if(prior!=null)db.delete("sheets","owner=?",arrayOf(id)) }; return result
        } catch(e: Exception) { target.delete(); throw e }
    }
    suspend fun importImage(uri: Uri,replaceId: String?=null): AssetRecord = work { db ->
        val prior=replaceId?.let { get(db,it).also { a -> require(a.kind==AssetKind.IMAGE) } }
        val bitmap=context.contentResolver.openInputStream(uri)?.use { ImageOps.decode(it) } ?: error("Cannot open chosen image")
        try { writeBitmap(db,bitmap,"Imported image","Imported",prior) } finally { bitmap.recycle() }
    }
    suspend fun duplicate(id: String): AssetRecord = work { db ->
        val a=get(db,id)
        if(a.kind==AssetKind.PREFAB) writePrefab(db,readPrefab(a),"${a.name.take(80)} copy",null)
        else {
            val bitmap=file(a).inputStream().use { ImageOps.decode(it) }
            try { writeBitmap(db,bitmap,"${a.name.take(80)} copy",a.folder) } finally { bitmap.recycle() }
        }
    }
    suspend fun editImage(id: String,edit: TextureEdit): AssetRecord = work { db ->
        val a=get(db,id); require(a.kind==AssetKind.IMAGE)
        val input=file(a).inputStream().use { ImageOps.decode(it) }
        try {
            val result=ImageOps.apply(input,edit)
            try { writeBitmap(db,result,"${a.name.take(72)} edited",a.folder) } finally { if(result!==input)result.recycle() }
        } finally { input.recycle() }
    }
    private fun readPrefab(a: AssetRecord): PrefabDefinition {
        require(a.kind==AssetKind.PREFAB && file(a).length()<=8*1024*1024)
        return json.decodeFromString<PrefabDefinition>(file(a).readText()).validated()
    }
    suspend fun definitions(): Map<String,PrefabDefinition> = work { db -> Catalog.list(db).filter { it.kind==AssetKind.PREFAB }.associate { it.id to readPrefab(it) } }
    private fun writePrefab(db: SQLiteDatabase,definition: PrefabDefinition,name: String,prior: AssetRecord?): AssetRecord {
        definition.validated(); validateMetadata(name,"Prefabs",emptyList())
        val id=prior?.id ?: UUID.randomUUID().toString()
        val a=AssetRecord(id,name,"Prefabs",AssetKind.PREFAB,"Prefabs/$id-${UUID.randomUUID()}.json",revision=(prior?.revision ?: 0)+1,tags=prior?.tags.orEmpty(),favorite=prior?.favorite ?: false)
        val target=file(a); target.parentFile?.mkdirs()
        try {
            FileOutputStream(target).use { it.write(json.encodeToString(definition).toByteArray()); it.fd.sync() }
            transaction(db) { Catalog.put(db,a); Catalog.setDependencies(db,id,definition.dependencies()) }
            return a
        } catch(e: Exception) { target.delete(); throw e }
    }
    suspend fun savePrefab(definition: PrefabDefinition,name: String,replaceId: String?=null): AssetRecord = work { db ->
        val prior=replaceId?.let { get(db,it).also { a -> require(a.kind==AssetKind.PREFAB) } }
        writePrefab(db,definition,name,prior)
    }
    suspend fun delete(id: String,retainedScenes: List<Scene>) = work { db ->
        val a=get(db,id)
        val dependents=db.rawQuery("SELECT a.name FROM edges e JOIN assets a ON e.owner=a.id WHERE e.target=?",arrayOf(id)).use { c -> buildList { while(c.moveToNext())add(c.getString(0)) } }
        require(dependents.isEmpty()) { "Asset is used by prefabs: ${dependents.joinToString()}" }
        val diskScenes=listOf("Scenes",".autosave").flatMap { File(root,it).listFiles().orEmpty().filter { f -> f.name.endsWith(".json") || f.name.endsWith(".json.bak") } }.map { f ->
            require(f.length()<=8*1024*1024) { "Cannot check references: oversized ${f.name}" }
            json.decodeFromString<Scene>(f.readText()).validated()
        }
        require((retainedScenes+diskScenes).none { scene -> id in scene.assetReferences() || scene.nodes.any { it.sprite?.asset==a.path } }) { "Asset is used by a scene, recovery snapshot, or undo history. Remove references, save, then close/reopen the project before deleting." }
        transaction(db) {
            db.delete("assets","id=?",arrayOf(id))
            db.execSQL("INSERT OR IGNORE INTO tombstones(id) VALUES(?)",arrayOf(id))
        }
        // Retain immutable payload revisions for crash safety; explicit garbage collection is separate.
    }
    suspend fun dependents(id: String): List<String> = work { db ->
        db.rawQuery("SELECT a.name FROM edges e JOIN assets a ON e.owner=a.id WHERE e.target=?",arrayOf(id)).use { c -> buildList { while(c.moveToNext()) add(c.getString(0)) } }
    }
    suspend fun export(id: String,uri: Uri) = work { db ->
        val a=get(db,id)
        require(a.kind==AssetKind.IMAGE) { "Use project backup for prefabs and their dependencies" }
        context.contentResolver.openOutputStream(uri,"wt")?.use { out -> file(a).inputStream().use { it.copyTo(out) } } ?: error("Cannot write destination")
    }
    suspend fun slice(id: String,frames: List<SpriteFrame>): List<AssetRecord> = work { db ->
        val a=get(db,id); require(frames.size in 1..256)
        val bitmap=file(a).inputStream().use { ImageOps.decode(it) }
        try {
            frames.forEach { it.validate(bitmap.width,bitmap.height) }
            storeSheet(db,id,frames)
            frames.map { frame ->
                val part=Bitmap.createBitmap(bitmap,frame.x,frame.y,frame.width,frame.height)
                try { writeBitmap(db,part,frame.name,"${a.folder}/Frames".trim('/')) } finally { if(part!==bitmap) part.recycle() }
            }
        } finally { bitmap.recycle() }
    }
    suspend fun autoFrames(id: String): List<SpriteFrame> = work { db ->
        val bitmap=file(get(db,id)).inputStream().use { ImageOps.decode(it) }
        try { SpriteSlicer.auto(bitmap) } finally { bitmap.recycle() }
    }
    suspend fun split(id: String,frames: List<SpriteFrame>): AssetRecord = work { db ->
        val a=get(db,id)
        val layers=a.recipe?.let { ProceduralAssets.layers(it) }
        val bitmap=if(layers==null) file(a).inputStream().use { ImageOps.decode(it) } else null
        try {
            val rootNode=Node(name=a.name,components=listOf(TransformComponent()))
            val children=if(layers!=null) layers.map { part ->
                val asset=writeBitmap(db,part.bitmap,part.name,"Parts/${a.name.take(80)}")
                part.node(asset.id,rootNode.id,a.width,a.height)
            } else {
                require(frames.size in 1..256) { "Choose manual, grid or automatic regions first" }
                val image=requireNotNull(bitmap)
                frames.forEach { it.validate(image.width,image.height) }
                frames.map { frame ->
                    val crop=Bitmap.createBitmap(image,frame.x,frame.y,frame.width,frame.height)
                    try {
                        val asset=writeBitmap(db,crop,frame.name,"Parts/${a.name.take(80)}")
                        ProceduralPart(frame.name,crop,frame.x,frame.y).node(asset.id,rootNode.id,image.width,image.height)
                    } finally { if(crop!==bitmap)crop.recycle() }
                }
            }
            writePrefab(db,PrefabDefinition(nodes=listOf(rootNode)+children),"${a.name.take(78)} split",null)
        } finally { bitmap?.recycle(); layers?.forEach { it.bitmap.recycle() } }
    }
    private fun storeSheet(db: SQLiteDatabase,id: String,frames: List<SpriteFrame>) {
        val asset=get(db,id); require(asset.kind==AssetKind.IMAGE && frames.size<=256)
        frames.forEach { it.validate(asset.width,asset.height) }
        db.execSQL("INSERT OR REPLACE INTO sheets(owner,layout) VALUES(?,?)",arrayOf(id,json.encodeToString(frames)))
    }
    suspend fun saveSheet(id: String,frames: List<SpriteFrame>) = work { db -> storeSheet(db,id,frames) }
    suspend fun sheet(id: String): List<SpriteFrame> = work { db ->
        db.rawQuery("SELECT layout FROM sheets WHERE owner=?",arrayOf(id)).use { c ->
            if(c.moveToFirst()) json.decodeFromString<List<SpriteFrame>>(c.getString(0)) else emptyList()
        }
    }
    suspend fun exportFrames(id: String,frames: List<SpriteFrame>,uri: Uri) = work { db ->
        require(frames.isNotEmpty()); val a=get(db,id)
        val image=file(a).inputStream().use { ImageOps.decode(it) }
        try {
            frames.forEach { it.validate(image.width,image.height) }
            storeSheet(db,id,frames)
            val stream=context.contentResolver.openOutputStream(uri,"wt") ?: error("Cannot write destination")
            java.util.zip.ZipOutputStream(stream).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("frames.json")); zip.write(json.encodeToString(frames).toByteArray()); zip.closeEntry()
                frames.forEachIndexed { index,frame ->
                    zip.putNextEntry(java.util.zip.ZipEntry("frame-${index+1}.png"))
                    val part=Bitmap.createBitmap(image,frame.x,frame.y,frame.width,frame.height)
                    try { require(part.compress(Bitmap.CompressFormat.PNG,100,zip)) } finally { if(part!==image)part.recycle() }
                    zip.closeEntry()
                }
            }
        } finally { image.recycle() }
    }
    /** Idempotent generation: each recipe is unique in SQLite, allowing interrupted runs to resume. */
    suspend fun generateLibrary(progress: (Int,Int)->Unit) {
        val job=currentCoroutineContext().job
        work { db ->
            File(root,"Resources/GENERATED_ASSETS_LICENSE.txt").writeText("Only original engine-generated procedural artwork is dedicated to the public domain under CC0 1.0: https://creativecommons.org/publicdomain/zero/1.0/ . No external artwork is bundled. User-imported images are NOT covered by this dedication. No warranty is provided.")
            val recipes=ProceduralAssets.recipes(); val existing=Catalog.list(db).mapNotNull { it.recipe }.toSet()
            recipes.forEachIndexed { index,recipe ->
                job.ensureActive()
                if(recipe !in existing) {
                    val bitmap=ProceduralAssets.render(recipe)
                    try { writeBitmap(db,bitmap,recipe.replace(':',' '),"Generated/${recipe.substringBefore(':')}",recipe=recipe) } finally { bitmap.recycle() }
                }
                progress(index+1,recipes.size)
            }
        }
    }
}

`````

## engine-assets/src/main/kotlin/world/engine/assets/ImageOps.kt

`````kotlin
package world.engine.assets

import android.graphics.*
import java.io.InputStream

/** Bounded, deterministic bitmap operations. Source bitmaps are never mutated. */
data class TextureEdit(val operation: String, val x: Int=0, val y: Int=0, val width: Int=64, val height: Int=64, val color: Int=android.graphics.Color.WHITE)
object ImageOps {
    fun decode(input: InputStream): Bitmap {
        val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
        while(true) { val n=input.read(buffer); if(n<0)break; require(out.size()+n<=16*1024*1024) { "Image exceeds 16 MiB" }; out.write(buffer,0,n) }
        val bytes=out.toByteArray(); val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
        require(options.outWidth>0 && options.outHeight>0) { "Unsupported or corrupt image" }
        options.inSampleSize=1
        while((options.outWidth.toLong()+options.inSampleSize-1)/options.inSampleSize>2048 || (options.outHeight.toLong()+options.inSampleSize-1)/options.inSampleSize>2048) options.inSampleSize*=2
        options.inJustDecodeBounds=false
        return BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: error("Image decode failed")
    }
    fun apply(source: Bitmap,edit: TextureEdit): Bitmap = when(edit.operation) {
        "Crop" -> {
            SpriteFrame("Crop",edit.x,edit.y,edit.width,edit.height).validate(source.width,source.height)
            Bitmap.createBitmap(source,edit.x,edit.y,edit.width,edit.height)
        }
        "Resize" -> { bounds(edit.width,edit.height); Bitmap.createScaledBitmap(source,edit.width,edit.height,false) }
        "Rotate 90°" -> Bitmap.createBitmap(source,0,0,source.width,source.height,Matrix().apply { postRotate(90f) },false)
        "Flip X", "Flip Y" -> Bitmap.createBitmap(source,0,0,source.width,source.height,Matrix().apply { postScale(if(edit.operation=="Flip X")-1f else 1f,if(edit.operation=="Flip Y")-1f else 1f) },false)
        "Tile 2×2" -> {
            bounds(source.width*2,source.height*2)
            Bitmap.createBitmap(source.width*2,source.height*2,Bitmap.Config.ARGB_8888).also { target ->
                val canvas=Canvas(target); repeat(2) { y -> repeat(2) { x -> canvas.drawBitmap(source,(x*source.width).toFloat(),(y*source.height).toFloat(),null) } }
            }
        }
        "Colorize" -> {
            val red=Color.red(edit.color)/255f; val green=Color.green(edit.color)/255f; val blue=Color.blue(edit.color)/255f; val alpha=Color.alpha(edit.color)/255f
            Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888).also { target ->
                val paint=Paint().apply { colorFilter=ColorMatrixColorFilter(ColorMatrix(floatArrayOf(red,0f,0f,0f,0f,0f,green,0f,0f,0f,0f,0f,blue,0f,0f,0f,0f,0f,alpha,0f))) }
                Canvas(target).drawBitmap(source,0f,0f,paint)
            }
        }
        else -> error("Unknown image operation")
    }
    private fun bounds(width: Int,height: Int) { require(width in 1..2048 && height in 1..2048) { "Use dimensions from 1 to 2048" } }
}

`````

## engine-assets/src/main/kotlin/world/engine/assets/ProceduralAssets.kt

`````kotlin
package world.engine.assets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import world.engine.core.*
import world.engine.math.Transform
import world.engine.math.Vec2
import kotlin.math.*

/** Original geometric pixel-art layers. Runtime output is dedicated to CC0; no external art. */
data class ProceduralPart(val name: String,val bitmap: Bitmap,val x: Int,val y: Int) {
    fun node(assetId: String,parent: String,width: Int,height: Int) = Node(name=name,parent=parent,components=listOf(
        TransformComponent(Transform(Vec2(x+bitmap.width/2f-width/2f,height/2f-y-bitmap.height/2f))),
        SpriteComponent(size=Vec2(bitmap.width.toFloat(),bitmap.height.toFloat()),assetId=assetId)))
}
object ProceduralAssets {
    val categories=linkedMapOf("Character" to 30,"Enemy" to 30,"NPC" to 20,"Animal" to 20,"Vehicle" to 25,"Building" to 40,"Nature" to 30,"Prop" to 40,"Texture" to 100)
    fun recipes() = categories.flatMap { (category,count) -> (1..count).map { "$category:$it" } }
    fun layers(recipe: String): List<ProceduralPart>? {
        val category=recipe.substringBefore(':'); val index=recipe.substringAfter(':').toInt()
        require(index in 1..(categories[category] ?: 0)) { "Unknown procedural recipe" }
        if(category=="Texture")return null
        val parts=mutableListOf<ProceduralPart>()
        val main=Color.HSVToColor(floatArrayOf(((index*37+(category.hashCode() and 255))%360).toFloat(),.55f,.8f))
        val dark=Color.rgb(25,32,45); val light=Color.rgb(190,224,238)
        fun part(name: String,x: Int,y: Int,w: Int,h: Int,color: Int,oval: Boolean=false) {
            val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
            val canvas=Canvas(bitmap); val paint=Paint().apply { this.color=color; isAntiAlias=false }
            if(oval)canvas.drawOval(0f,0f,w.toFloat(),h.toFloat(),paint) else canvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),paint)
            parts.add(ProceduralPart(name,bitmap,x,y))
        }
        when(category) {
            "Vehicle" -> when(index%4) {
                0 -> {
                    part("Rear wheel",25,44,14,19,dark,true); part("Front wheel",25,1,14,19,dark,true)
                    part("Frame",28,16,8,34,main); part("Fuel tank",22,20,20,17,main,true)
                    part("Seat",25,36,14,13,dark); part("Handlebars",13,15,38,4,light)
                    part("Headlight",28,10,8,7,Color.YELLOW); part("Footrest",17,40,30,3,light)
                }
                2 -> {
                    part("Cargo bed",13,25,38,35,main); part("Cab",16,3,32,24,main)
                    part("Windshield",20,7,24,9,light); part("Door left",14,17,5,9,dark); part("Door right",45,17,5,9,dark)
                    for(y in listOf(9,34,48)) { part("Wheel left $y",8,y,6,12,dark); part("Wheel right $y",50,y,6,12,dark) }
                    part("Cargo",18,31,28,22,Color.rgb(125,89,42)); part("Bumper",14,0,36,3,light)
                    part("Light left",17,2,5,3,Color.YELLOW); part("Light right",42,2,5,3,Color.YELLOW)
                }
                3 -> {
                    part("Track left",7,10,12,48,dark); part("Track right",45,10,12,48,dark)
                    part("Hull",16,10,32,46,main); part("Turret",21,21,22,24,main,true)
                    part("Gun barrel",29,0,6,30,light); part("Hatch",26,29,12,10,dark,true)
                    for(y in 12..52 step 8) { part("Tread left $y",7,y,12,2,light); part("Tread right $y",45,y,12,2,light) }
                }
                else -> {
                val width=26+index%5*2
                part("Body",32-width/2,8,width,48,main)
                part("Interior",24,22,16,23,dark)
                part("Front window",24,18,16,8,light); part("Rear window",24,39,16,7,light)
                part("Door left",17,27,5,15,main); part("Door right",42,27,5,15,main)
                part("Wheel front left",12,17,7,12,dark); part("Wheel front right",45,17,7,12,dark)
                part("Wheel rear left",12,40,7,12,dark); part("Wheel rear right",45,40,7,12,dark)
                part("Headlight left",21,8,6,4,Color.YELLOW); part("Headlight right",37,8,6,4,Color.YELLOW)
                part("Mirror left",10,28,7,3,light); part("Mirror right",47,28,7,3,light)
                part("Front bumper",19,5,26,3,dark); part("Rear bumper",19,56,26,3,dark)
                part("Spoiler",17,51,30,3,light)
                }
            }
            "Character","Enemy","NPC" -> {
                val offset=index%4
                part("Left leg",22,41,8,17,dark); part("Right leg",34,41,8,17,dark)
                part("Torso",20-offset,23,24+offset*2,22,main)
                part("Left arm",12-offset,24,8,20,main); part("Right arm",44+offset,24,8,20,main)
                part("Head",22,5,20,19,if(category=="Enemy")main else Color.rgb(170+index%7*10,120+index%9*7,90+index%8*8),category=="Enemy")
                part("Hair or helmet",20,3,24,6,dark)
                part("Left eye",26,13,3,3,light); part("Right eye",35,13,3,3,light)
                if(index%2==0)part("Belt",19,37,26,3,light)
                if(index%3==0)part("Tool",52,32,4,25,light)
            }
            "Animal" -> {
                part("Body",11,24,40,22,main,true)
                part("Head",39,13,19,23,main,true); part("Ear",42,5,8,17,dark,true)
                part("Front leg",40,40,6,17,dark); part("Rear leg",16,40,6,17,dark)
                part("Tail",3,17+index%8,8,23,main); part("Eye",49,21,3,3,light)
            }
            "Building" -> when(index%5) {
                2 -> {
                    part("Road",0,0,64,64,dark); part("Sidewalk left",0,0,10,64,light); part("Sidewalk right",54,0,10,64,light)
                    for(y in 2..56 step 18)part("Lane marking $y",30,y,4,10,Color.YELLOW)
                }
                3 -> {
                    part("Water",0,0,64,64,Color.rgb(35,110,180)); part("Bridge deck",12,0,40,64,main)
                    part("Rail left",10,0,4,64,light); part("Rail right",50,0,4,64,light)
                    for(y in 0..56 step 8)part("Plank $y",14,y,36,2,dark)
                }
                4 -> {
                    part("Tower",10,0,44,64,main)
                    for(y in 5..45 step 13)for(x in listOf(16,28,40))part("Window $x $y",x,y,7,8,light)
                    part("Entry",26,53,12,11,dark)
                }
                else -> {
                part("Wall",8,24,48,36,main)
                val roof=Bitmap.createBitmap(60,25,Bitmap.Config.ARGB_8888)
                val path=android.graphics.Path().apply { moveTo(0f,25f); lineTo(30f,(index%5).toFloat()); lineTo(60f,25f); close() }
                Canvas(roof).drawPath(path,Paint().apply { color=dark }); parts.add(ProceduralPart("Roof",roof,2,0))
                part("Door",26,39,12,21,dark); part("Window left",12,32,10,12,light); part("Window right",42,32,10,12,light)
                if(index%2==0)part("Chimney",45,4,7,20,dark)
                }
            }
            "Nature" -> {
                if(index%3==0) { part("Rock",8,20,48,37,main,true); part("Highlight",16,23,22,8,light,true) }
                else { part("Trunk",27,28,10,34,Color.rgb(106,72,39)); part("Canopy",6,3,52,43,Color.rgb(30+index*3,100+index*3,40),true); part("Leaves",14,1,31,27,main,true) }
            }
            "Prop" -> {
                when(index%4) {
                    0 -> { part("Crate",8,8,48,48,main); part("Brace top",8,10,48,6,dark); part("Brace bottom",8,48,48,6,dark); part("Brace middle",27,8,8,48,light) }
                    1 -> { part("Barrel",14,7,36,50,main,true); part("Band top",15,17,34,5,dark); part("Band bottom",15,42,34,5,dark) }
                    2 -> { part("Table",6,18,52,10,main); part("Leg left",10,28,6,30,dark); part("Leg right",48,28,6,30,dark) }
                    else -> { part("Handle",27,35,8,24,main); part("Tool head",12,12,40,25,light); part("Fastener",29,19,5,5,dark) }
                }
            }
        }
        return parts
    }
    fun render(recipe: String): Bitmap {
        val bitmap=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888)
        val layers=layers(recipe)
        if(layers!=null) {
            val canvas=Canvas(bitmap)
            try { layers.forEach { canvas.drawBitmap(it.bitmap,it.x.toFloat(),it.y.toFloat(),null) } } finally { layers.forEach { it.bitmap.recycle() } }
        } else {
            // Integer-frequency periodic fields guarantee tileable patterns; mirrored edge samples match.
            val index=recipe.substringAfter(':').toInt(); val hue=index*43%360
            val pixels=IntArray(4096) { i ->
                val x=i%64; val y=i/64
                val value=(.5+.2*cos(2*PI*(index%5+1)*x/63)+.2*cos(2*PI*(index%7+1)*y/63)).toFloat()
                Color.HSVToColor(floatArrayOf(hue.toFloat(),.2f+(index%6)*.1f,value.coerceIn(.1f,1f)))
            }
            bitmap.setPixels(pixels,0,64,0,0,64,64)
        }
        return bitmap
    }
}

`````

## engine-assets/src/main/kotlin/world/engine/assets/SpriteSlicer.kt

`````kotlin
package world.engine.assets

import android.graphics.Bitmap
import kotlinx.serialization.Serializable

@Serializable data class SpriteFrame(val name: String,val x: Int,val y: Int,val width: Int,val height: Int) {
    fun validate(imageWidth: Int,imageHeight: Int) {
        require(name.isNotBlank() && name.length<=96)
        require(x>=0 && y>=0 && width>0 && height>0 && x.toLong()+width<=imageWidth && y.toLong()+height<=imageHeight) { "Frame must be inside the image" }
    }
}
object SpriteSlicer {
    /** Full cells only; reject grids that would discard edge pixels. */
    fun grid(width: Int,height: Int,cellWidth: Int,cellHeight: Int): List<SpriteFrame> {
        require(cellWidth>0 && cellHeight>0 && width%cellWidth==0 && height%cellHeight==0) { "Cell dimensions must divide the image exactly" }
        require((width/cellWidth).toLong()*(height/cellHeight) in 1..256) { "Use 1–256 frames" }
        return (0 until height step cellHeight).flatMap { y -> (0 until width step cellWidth).map { x -> SpriteFrame("Frame ${y/cellHeight*(width/cellWidth)+x/cellWidth+1}",x,y,cellWidth,cellHeight) } }
    }
    /** Four-connected nontransparent islands; opaque sheets require grid/manual regions. */
    fun auto(bitmap: Bitmap): List<SpriteFrame> {
        val w=bitmap.width; val h=bitmap.height
        val pixels=IntArray(w*h); bitmap.getPixels(pixels,0,w,0,0,w,h)
        val seen=BooleanArray(pixels.size); val queue=IntArray(pixels.size); val result=mutableListOf<SpriteFrame>()
        for(start in pixels.indices) {
            if(seen[start] || (pixels[start] ushr 24)==0) continue
            require(result.size<256) { "More than 256 islands; use grid/manual slicing" }
            var head=0; var tail=0; queue[tail++]=start; seen[start]=true
            var left=w; var right=0; var top=h; var bottom=0
            fun enqueue(i: Int) { if(!seen[i] && (pixels[i] ushr 24)!=0) { seen[i]=true; queue[tail++]=i } }
            while(head<tail) {
                val i=queue[head++]; val x=i%w; val y=i/w
                left=minOf(left,x); right=maxOf(right,x); top=minOf(top,y); bottom=maxOf(bottom,y)
                if(x>0)enqueue(i-1); if(x<w-1)enqueue(i+1); if(y>0)enqueue(i-w); if(y<h-1)enqueue(i+w)
            }
            result.add(SpriteFrame("Part ${result.size+1}",left,top,right-left+1,bottom-top+1))
        }
        return result.sortedWith(compareBy<SpriteFrame> { it.y }.thenBy { it.x })
    }
}

`````

## engine-core/src/main/kotlin/world/engine/core/Prefab.kt

`````kotlin
package world.engine.core

import kotlinx.serialization.Serializable
import world.engine.math.*
import java.util.UUID

/** A binding keeps instance identity and per-property overrides independent of asset paths. */
@Serializable data class PrefabBinding(val assetId: String, val sourcePath: String, val instanceRoot: String, val overrides: Set<String> = emptySet())
@Serializable data class PrefabDefinition(val version: Int=1, val nodes: List<Node>) {
    fun validated(): PrefabDefinition {
        require(version==1 && nodes.count { it.parent==null }==1) { "Prefab needs one root" }
        Scene(nodes=nodes).validated()
        require(nodes.all { it.prefab==null }) { "Definitions cannot contain runtime bindings" }
        nodes.forEach { require(it.nestedPrefab==null || Scene.isAssetId(it.nestedPrefab)) }
        return this
    }
    fun dependencies(): Set<String> = nodes.flatMap { listOfNotNull(it.sprite?.assetId,it.sprite?.asset?.removePrefix("Sprites/")?.removeSuffix(".png")?.takeIf { id -> Scene.isAssetId(id) },it.nestedPrefab) }.toSet()
}

/** Nested definitions expand to ordinary editable ECS entities, bounded against recursion/bombs. */
object Prefabs {
    fun instantiate(assetId: String, definitions: Map<String,PrefabDefinition>, position: Vec2=Vec2(), previous: List<Node> = emptyList(), preserveOverrides: Boolean=true): List<Node> {
        val old=previous.associateBy { it.prefab?.sourcePath }
        val output=mutableListOf<Node>()
        var instanceRoot=""
        fun expand(id: String, parent: String?, prefix: String, stack: Set<String>) {
            require(id !in stack && stack.size<16) { "Cyclic or excessively nested prefab" }
            val definition=definitions[id]?.validated() ?: error("Missing prefab $id")
            val root=definition.nodes.single { it.parent==null }
            val ids=definition.nodes.associate { n -> n.id to (old[prefix+n.id]?.id ?: UUID.randomUUID().toString()) }
            if(instanceRoot.isEmpty()) instanceRoot=ids.getValue(root.id)
            definition.nodes.forEach { source ->
                require(output.size<5000) { "Expanded prefab exceeds 5000 entities" }
                val path=prefix+source.id; val prior=old[path]
                val flags=if(preserveOverrides) prior?.prefab?.overrides.orEmpty() else emptySet()
                var node=source.copy(id=ids.getValue(source.id),parent=source.parent?.let { ids.getValue(it) } ?: parent,
                    prefab=PrefabBinding(assetId,path,instanceRoot,flags))
                if(prior!=null) {
                    if("name" in flags) node=node.copy(name=prior.name)
                    node=node.copy(components=node.components.map { c -> when {
                        c is TransformComponent && "transform" in flags -> TransformComponent(prior.transform)
                        c is SpriteComponent && "sprite" in flags -> prior.sprite ?: c
                        else -> c
                    } })
                }
                if(node.id==instanceRoot) node=node.moved(position)
                output.add(node)
                source.nestedPrefab?.let { expand(it,node.id,"$path/",stack+id) }
            }
        }
        expand(assetId,null,"",emptySet())
        Scene(nodes=output).validated()
        return output
    }
    /** Capture a subtree, preserving nested references instead of duplicating their expansion. */
    fun capture(scene: Scene, rootId: String): PrefabDefinition {
        val root=scene.nodes.single { it.id==rootId }
        val excluded=scene.nodes.filter { it.nestedPrefab!=null && it.id in scene.descendants(rootId) }
            .flatMap { scene.descendants(it.id)-it.id }.toSet()
        val selected=scene.nodes.filter { it.id in scene.descendants(rootId) && it.id !in excluded }
        val ids=selected.associate { it.id to (if(root.prefab?.instanceRoot==root.id) it.prefab?.sourcePath ?: it.id else it.id) }
        val nodes=selected.map {
            val clean=it.copy(id=ids.getValue(it.id),parent=it.parent?.let { p -> ids[p] },prefab=null)
            if(it.id==root.id) clean.copy(parent=null).moved(Vec2()) else clean
        }
        return PrefabDefinition(nodes=nodes).validated()
    }
    fun override(node: Node, vararg fields: String): Node = node.copy(prefab=node.prefab?.let { it.copy(overrides=it.overrides+fields) })
}

`````

## engine-core/src/main/kotlin/world/engine/core/Scene.kt

`````kotlin
package world.engine.core

import kotlinx.serialization.Serializable
import world.engine.math.*
import java.util.UUID

/** Components are serializable values. Systems consume immutable scene snapshots. */
@Serializable sealed class Component
@Serializable data class TransformComponent(val value: Transform = Transform()) : Component()
@Serializable data class SpriteComponent(val size: Vec2 = Vec2(96f,96f), val tint: Color = Color(), val asset: String? = null, val assetId: String? = null) : Component()
/** Stable entity identity. Children store local transforms; list order controls painting. */
@Serializable data class Node(val id: String = UUID.randomUUID().toString(), val name: String = "Sprite", val parent: String? = null, val prefab: PrefabBinding? = null, val nestedPrefab: String? = null, val components: List<Component> = listOf(TransformComponent(), SpriteComponent())) {
    val transform get() = components.filterIsInstance<TransformComponent>().single().value
    val sprite get() = components.filterIsInstance<SpriteComponent>().singleOrNull()
    fun moved(p: Vec2) = copy(components=components.map { if (it is TransformComponent) it.copy(value=it.value.copy(position=p)) else it })
}
@Serializable data class Scene(val version: Int = 1, val name: String = "Main", val nodes: List<Node> = emptyList()) {
    /** Reject ambiguous identities, unsupported versions and unsafe asset references before use. */
    fun validated(): Scene {
        require(version == 1) { "Unsupported scene version $version" }
        require(nodes.size <= 5000) { "Scene exceeds 5000 entities" }
        require(nodes.map { it.id }.toSet().size == nodes.size) { "Duplicate entity IDs" }
        val byId=nodes.associateBy { it.id }
        nodes.forEach { n ->
            require(n.parent == null || n.parent in byId) { "Missing parent for ${n.name}" }
            val ancestors=mutableSetOf(n.id); var parent=n.parent
            while(parent!=null) {
                require(ancestors.add(parent) && ancestors.size<=64) { "Hierarchy cycle or depth over 64" }
                parent=byId[parent]?.parent
            }
            n.prefab?.let {
                require(it.instanceRoot in byId && isAssetId(it.assetId)) { "Invalid prefab instance binding" }
                require(it.sourcePath.isNotBlank() && it.sourcePath.length<=4096 && it.overrides.all { field -> field in setOf("name","transform","sprite") })
            }
            require(n.nestedPrefab==null || isAssetId(n.nestedPrefab)) { "Invalid nested prefab UUID" }

            require(n.id.isNotBlank() && n.name.isNotBlank()) { "Empty entity identity or name" }
            require(n.components.count { it is TransformComponent } == 1)
            require(n.components.count { it is SpriteComponent } <= 1)
            val t=n.transform
            require(listOf(t.position.x,t.position.y,t.rotation,t.scale.x,t.scale.y).all { it.isFinite() })
            require(kotlin.math.abs(t.scale.x) >= .001f && kotlin.math.abs(t.scale.y) >= .001f)
            n.sprite?.let { s ->
                require(s.size.x.isFinite() && s.size.y.isFinite() && s.size.x>0 && s.size.y>0)
                require(listOf(s.tint.r,s.tint.g,s.tint.b,s.tint.a).all { it.isFinite() && it in 0f..1f })
                require(s.assetId == null || isAssetId(s.assetId)) { "Invalid asset UUID" }
                require(s.asset == null || Regex("Sprites/[a-f0-9-]+\\.png").matches(s.asset)) { "Unsafe asset path" }
            }
        }
        worldMatrices().values.forEach { matrix ->
            require(matrix.values.all { it.isFinite() } && matrix.inverse().values.all { it.isFinite() }) { "Transform hierarchy is numerically unstable" }
        }
        return this
    }
    fun replace(node: Node) = copy(nodes=nodes.map { if(it.id==node.id) node else it })
    inline fun <reified T: Component> query(): List<Pair<Node,T>> = nodes.flatMap { n -> n.components.filterIsInstance<T>().map { n to it } }
    fun worldMatrices(): Map<String,Mat3> {
        val byId=nodes.associateBy { it.id }; val result=mutableMapOf<String,Mat3>()
        fun visit(id: String): Mat3 = result.getOrPut(id) {
            val n=byId.getValue(id)
            (n.parent?.let { visit(it) } ?: Mat3.Identity)*n.transform.matrix()
        }
        nodes.forEach { visit(it.id) }; return result
    }
    fun pick(point: Vec2): Node? {
        val matrices=worldMatrices()
        return nodes.asReversed().firstOrNull { n -> n.sprite?.let { Rect(Vec2(),it.size).contains(matrices.getValue(n.id).inverse().map(point)) } ?: false }
    }
    fun descendants(id: String): Set<String> {
        val result=mutableSetOf(id)
        var old: Int
        do { old=result.size; nodes.filter { it.parent in result }.forEach { result.add(it.id) } } while(result.size!=old)
        return result
    }
    fun assetReferences(): Set<String> = nodes.flatMap { listOfNotNull(it.sprite?.assetId,it.sprite?.asset?.removePrefix("Sprites/")?.removeSuffix(".png")?.takeIf { id -> isAssetId(id) },it.prefab?.assetId,it.nestedPrefab) }.toSet()
    companion object { fun isAssetId(id: String) = Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}").matches(id) }

}
/** Snapshot commands combine an entire drag into one undoable edit. */
class CommandStack(private val capacity: Int = 100) {
    init { require(capacity > 0) }
    private val undo = ArrayDeque<Scene>()
    private val redo = ArrayDeque<Scene>()
    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()
    fun commit(before: Scene, after: Scene): Scene {
        after.validated()
        if(before!=after) { undo.addLast(before); if(undo.size>capacity) undo.removeFirst(); redo.clear() }
        return after
    }
    fun undo(current: Scene): Scene { if(undo.isEmpty()) return current; redo.addLast(current); return undo.removeLast() }
    fun redo(current: Scene): Scene { if(redo.isEmpty()) return current; undo.addLast(current); return redo.removeLast() }
    fun retainedScenes(): List<Scene> = undo.toList()+redo.toList()
    fun clear() { undo.clear(); redo.clear() }
}

`````

## engine-core/src/test/kotlin/world/engine/core/PrefabTest.kt

`````kotlin
package world.engine.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import world.engine.math.*
import java.util.UUID

class PrefabTest {
    @Test fun hierarchyMatrixAndInversePicking() {
        val parent=Node(components=listOf(TransformComponent(Transform(Vec2(100f,40f),30f,Vec2(2f,.5f)))))
        val child=Node(parent=parent.id,components=listOf(TransformComponent(Transform(Vec2(10f,8f),20f)),SpriteComponent()))
        val scene=Scene(nodes=listOf(child,parent)).validated()
        val p=scene.worldMatrices().getValue(child.id).map(Vec2())
        assertEquals(child,scene.pick(p))
        assertEquals(setOf(parent.id,child.id),scene.descendants(parent.id))
        assertThrows(IllegalArgumentException::class.java) { Scene(nodes=listOf(parent.copy(parent=child.id),child)).validated() }
    }
    @Test fun nestedInstancesStableIdentityOverridesAndSerialization() {
        val sourceId=UUID.randomUUID().toString(); val outerId=UUID.randomUUID().toString()
        val leaf=Node(name="Wheel"); val leafDef=PrefabDefinition(nodes=listOf(leaf))
        val group=Node(name="Car",components=listOf(TransformComponent()))
        val nested=Node(parent=group.id,nestedPrefab=sourceId,components=listOf(TransformComponent()))
        val definitions=mapOf(sourceId to leafDef,outerId to PrefabDefinition(nodes=listOf(group,nested)))
        val original=Prefabs.instantiate(outerId,definitions,Vec2(200f,50f))
        val edited=original.map { if(it.name=="Wheel") Prefabs.override(it.copy(name="Custom wheel").moved(Vec2(8f,4f)),"name","transform") else it }
        val updated=definitions+(sourceId to leafDef.copy(nodes=listOf(leaf.copy(name="New wheel"))))
        val refreshed=Prefabs.instantiate(outerId,updated,Vec2(200f,50f),edited)
        assertEquals(original.map { it.id },refreshed.map { it.id })
        assertTrue(refreshed.any { it.name=="Custom wheel" && it.transform.position==Vec2(8f,4f) })
        val reverted=Prefabs.instantiate(outerId,updated,Vec2(200f,50f),edited,false)
        assertTrue(reverted.any { it.name=="New wheel" })
        val scene=Scene(nodes=refreshed).validated()
        assertEquals(scene,Json.decodeFromString<Scene>(Json.encodeToString(scene)).validated())
        val captured=Prefabs.capture(scene,refreshed.first().id)
        assertEquals(2,captured.nodes.size); assertTrue(captured.nodes.any { it.nestedPrefab==sourceId })
    }
    @Test fun cyclesMissingDefinitionsAndInvalidParentAreRejected() {
        val id=UUID.randomUUID().toString()
        val definition=PrefabDefinition(nodes=listOf(Node(nestedPrefab=id,components=listOf(TransformComponent()))))
        assertThrows(IllegalArgumentException::class.java) { Prefabs.instantiate(id,mapOf(id to definition)) }
        assertThrows(IllegalStateException::class.java) { Prefabs.instantiate(id,emptyMap()) }
        assertThrows(IllegalArgumentException::class.java) { Scene(nodes=listOf(Node(parent="missing"))).validated() }
    }
}

`````

## engine-io/src/main/kotlin/world/engine/io/ProjectStore.kt

`````kotlin
package world.engine.io

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import world.engine.core.Scene
import java.io.File
import java.util.UUID

@Serializable data class ProjectManifest(val id: String = UUID.randomUUID().toString(), val name: String, val packageId: String, val orientation: String, val template: String, val version: Int = 1)
data class Project(val manifest: ProjectManifest, val directory: File)
data class OpenedProject(val project: Project, val saved: Scene, val recovery: Scene?, val recoveryIssue: String? = null)
data class ProjectListing(val projects: List<Project>, val errors: List<String>)

/** App-owned projects. Writes are serialized and AtomicFile retains the previous good file on failure. */
class ProjectStore(context: Context) {
    private val context = context.applicationContext
    val root = File(context.filesDir, "2DWorldProjects").apply { mkdirs() }
    private val lock = Mutex()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = false }
    private val folders = listOf("Scenes","Scripts","Sprites","Textures","Animations","Tilesets","Tilemaps","Materials","Particles","Audio/Music","Audio/SFX","UI","Prefabs","Physics","Fonts","Shaders","Data","Resources","Plugins","Build","Exports",".autosave")
    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { lock.withLock { block() } }
    private fun read(file: File): String {
        val atomic=AtomicFile(file)
        atomic.openRead().use { input ->
            val data=input.readBytesLimited(8 * 1024 * 1024)
            return data.toString(Charsets.UTF_8)
        }
    }
    private fun java.io.InputStream.readBytesLimited(max: Int): ByteArray {
        val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
        while(true) { val count=read(buffer); if(count<0) break; require(out.size()+count<=max) { "File exceeds size limit" }; out.write(buffer,0,count) }
        return out.toByteArray()
    }
    private fun write(file: File, text: String) {
        file.parentFile?.mkdirs(); val atomic=AtomicFile(file); val stream=atomic.startWrite()
        try { stream.write(text.toByteArray()); atomic.finishWrite(stream) }
        catch(e: Exception) { atomic.failWrite(stream); throw e }
    }
    private fun scene(file: File) = json.decodeFromString<Scene>(read(file)).validated()
    suspend fun list(): ProjectListing = io {
        val errors=mutableListOf<String>()
        val projects=root.listFiles().orEmpty().filter { it.isDirectory }.mapNotNull { dir ->
            try {
                val m=json.decodeFromString<ProjectManifest>(read(File(dir,"project.json")))
                require(m.version==1 && m.id==dir.name) { "Invalid project manifest" }
                Project(m,dir)
            } catch(e: Exception) { errors.add("${dir.name}: ${e.message}"); null }
        }.sortedByDescending { File(it.directory,"Scenes/Main.json").lastModified() }
        ProjectListing(projects,errors)
    }
    suspend fun create(name: String, packageId: String, orientation: String, template: String, initial: Scene): Project = io {
        require(name.trim().length in 1..64) { "Use a project name of 1–64 characters" }
        require(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+").matches(packageId)) { "Use a lowercase package such as com.example.mygame" }
        require(orientation in listOf("Portrait","Landscape"))
        require(template in listOf("Empty","Three sprites"))
        val m=ProjectManifest(name=name.trim(),packageId=packageId,orientation=orientation,template=template)
        val dir=File(root,m.id)
        try {
            folders.forEach { require(File(dir,it).mkdirs()) { "Cannot create project directory" } }
            write(File(dir,"Scenes/Main.json"),json.encodeToString(initial.validated()))
            write(File(dir,"project.json"),json.encodeToString(m))
            Project(m,dir)
        } catch(e: Exception) { dir.deleteRecursively(); throw e }
    }
    suspend fun open(project: Project): OpenedProject = io {
        val saved=scene(File(project.directory,"Scenes/Main.json"))
        val recoveryFile=File(project.directory,".autosave/Main.json")
        var issue: String?=null
        val recovery=try {
            if(recoveryFile.exists() || File(recoveryFile.path+".bak").exists()) scene(recoveryFile).takeIf { it!=saved } else null
        } catch(e: Exception) { issue="The recovery snapshot cannot be read: ${e.message}. Your explicit save is intact."; null }
        OpenedProject(project,saved,recovery,issue)
    }
    suspend fun save(project: Project, scene: Scene, autosave: Boolean = false) = io {
        write(File(project.directory,if(autosave) ".autosave/Main.json" else "Scenes/Main.json"),json.encodeToString(scene.validated()))
        if(!autosave) AtomicFile(File(project.directory,".autosave/Main.json")).delete()
    }
    suspend fun discardRecovery(project: Project) = io { AtomicFile(File(project.directory,".autosave/Main.json")).delete() }
    /** Decode bounded images then normalize to PNG; external URI permissions need not survive a restart. */
    suspend fun importSprite(project: Project, uri: Uri): String = io {
        val bytes=context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(16*1024*1024) } ?: error("Cannot read image")
        val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
        require(options.outWidth>0 && options.outHeight>0) { "Unsupported image format" }
        options.inSampleSize=1
        while((options.outWidth.toLong()+options.inSampleSize-1)/options.inSampleSize>2048 || (options.outHeight.toLong()+options.inSampleSize-1)/options.inSampleSize>2048) options.inSampleSize*=2
        options.inJustDecodeBounds=false
        val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: error("Image decode failed")
        val path="Sprites/${UUID.randomUUID()}.png"
        val target=File(project.directory,path)
        try { target.outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }; path }
        catch(e: Exception) { target.delete(); throw e }
        finally { bitmap.recycle() }
    }
}

`````

## engine-math/src/main/kotlin/world/engine/math/Math.kt

`````kotlin
package world.engine.math

import kotlinx.serialization.Serializable
import kotlin.math.*

/** Immutable world-space vector; positive Y points up. */
@Serializable data class Vec2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(v: Vec2) = Vec2(x + v.x, y + v.y)
    operator fun minus(v: Vec2) = Vec2(x - v.x, y - v.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    fun length() = hypot(x, y)
}
/** Row-major affine matrix operating on column vectors. */
data class Mat3(val values: List<Float>) {
    init { require(values.size == 9) }
    operator fun times(b: Mat3) = Mat3(List(9) { i ->
        (0..2).sumOf { k -> (values[(i / 3) * 3 + k] * b.values[k * 3 + i % 3]).toDouble() }.toFloat()
    })
    fun map(v: Vec2) = Vec2(values[0]*v.x + values[1]*v.y + values[2], values[3]*v.x + values[4]*v.y + values[5])
    /** Inverse of a non-singular affine matrix, including inherited shear. */
    fun inverse(): Mat3 {
        val a=values[0]; val b=values[1]; val c=values[3]; val d=values[4]
        val determinant=a*d-b*c
        require(determinant.isFinite() && determinant != 0f) { "Singular transform" }
        val tx=values[2]; val ty=values[5]
        return Mat3(listOf(d/determinant,-b/determinant,(b*ty-d*tx)/determinant,
            -c/determinant,a/determinant,(c*tx-a*ty)/determinant,0f,0f,1f))
    }
    companion object { val Identity = Mat3(listOf(1f,0f,0f, 0f,1f,0f, 0f,0f,1f)) }
}
@Serializable data class Transform(val position: Vec2 = Vec2(), val rotation: Float = 0f, val scale: Vec2 = Vec2(1f, 1f)) {
    fun matrix(): Mat3 {
        val r = Math.toRadians(rotation.toDouble()); val c = cos(r).toFloat(); val s = sin(r).toFloat()
        return Mat3(listOf(c*scale.x,-s*scale.y,position.x, s*scale.x,c*scale.y,position.y, 0f,0f,1f))
    }
    fun inverseMap(p: Vec2): Vec2 {
        val d=p-position; val r=Math.toRadians(-rotation.toDouble()); val c=cos(r).toFloat(); val s=sin(r).toFloat()
        return Vec2((c*d.x-s*d.y)/scale.x, (s*d.x+c*d.y)/scale.y)
    }
}
@Serializable data class Rect(val center: Vec2, val size: Vec2) {
    fun contains(p: Vec2) = abs(p.x-center.x)<=size.x/2 && abs(p.y-center.y)<=size.y/2
}
@Serializable data class Color(val r: Float=1f, val g: Float=1f, val b: Float=1f, val a: Float=1f)

`````

## engine-render/src/main/kotlin/world/engine/render/SceneRenderer.kt

`````kotlin
package world.engine.render

import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import world.engine.core.Scene
import world.engine.math.*
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.floor

data class RenderFrame(val scene: Scene=Scene(), val selected: String?=null, val camera: Camera2D=Camera2D(), val directory: File?=null, val assetPaths: Map<String,String> = emptyMap())
/** The renderer only reads immutable snapshots; it never mutates editor entities. */
class SceneRenderer(private val report: (String)->Unit): GLSurfaceView.Renderer {
    @Volatile var frame=RenderFrame()
    private val batch=SpriteBatch()
    private val textures=TextureLoader(report)
    private var width=1; private var height=1
    private var root: File?=null
    private var ready=false
    private var assetPaths: Map<String,String> = emptyMap()
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        ready=false
        try {
            batch.initialize(); textures.initialize(); root=null
            glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA); ready=true
        } catch(e: Exception) { report("OpenGL initialization failed: ${e.message}") }
    }
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) { this.width=width; this.height=height; glViewport(0,0,width,height) }
    override fun onDrawFrame(gl: GL10?) {
        glClearColor(.065f,.075f,.09f,1f); glClear(GL_COLOR_BUFFER_BIT)
        if(!ready) return
        try {
            val f=frame
            if(root!=f.directory || assetPaths!=f.assetPaths) { textures.clear(); root=f.directory; assetPaths=f.assetPaths }
            batch.begin(f.camera,width,height)
            val a=f.camera.screenToWorld(0f,height.toFloat(),width,height)
            val b=f.camera.screenToWorld(width.toFloat(),0f,width,height)
            val spacing=if(f.camera.zoom<.5f) 256f else 64f
            val grid=Color(.14f,.16f,.19f)
            var x=floor(a.x/spacing)*spacing
            while(x<=b.x) { batch.draw(textures.white,Transform(Vec2(x,(a.y+b.y)/2)),Vec2(1/f.camera.zoom,b.y-a.y),grid); x+=spacing }
            var y=floor(a.y/spacing)*spacing
            while(y<=b.y) { batch.draw(textures.white,Transform(Vec2((a.x+b.x)/2,y)),Vec2(b.x-a.x,1/f.camera.zoom),grid); y+=spacing }
            val matrices=f.scene.worldMatrices()
            val selection=f.selected?.let { f.scene.descendants(it) }.orEmpty()
            f.scene.nodes.forEach { n -> n.sprite?.let { sprite ->
                val matrix=matrices.getValue(n.id)
                val ref=sprite.assetId ?: sprite.asset?.removePrefix("Sprites/")?.removeSuffix(".png")
                val path=ref?.let { f.assetPaths[it] } ?: sprite.asset ?: sprite.assetId?.let { "Sprites/$it-missing.png" }

                if(n.id in selection) batch.drawMatrix(textures.white,matrix,sprite.size+Vec2(8/f.camera.zoom,8/f.camera.zoom),Color(1f,.75f,.15f))
                batch.drawMatrix(textures.get(root,path),matrix,sprite.size,sprite.tint)
            } }
            batch.flush()
        } catch(e: Exception) { ready=false; report("Rendering stopped: ${e.message}. Reopen the project to retry.") }
    }
}

`````

## engine-render/src/main/kotlin/world/engine/render/SpriteBatch.kt

`````kotlin
package world.engine.render

import android.opengl.GLES30.*
import world.engine.math.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** GLES 3 streaming batch. Consecutive sprites sharing a texture use one draw call. GL-thread only. */
class SpriteBatch {
    private val data=ByteBuffer.allocateDirect(6*8*4*1024).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var program=0
    private var vao=0
    private var vbo=0
    private var texture=0
    private var vertices=0
    private lateinit var camera: Camera2D
    private var width=1
    private var height=1
    fun initialize() {
        val vertex=compile(GL_VERTEX_SHADER,"""#version 300 es
            layout(location=0) in vec2 position;
            layout(location=1) in vec2 uv;
            layout(location=2) in vec4 color;
            out vec2 texCoord;
            out vec4 tint;
            void main(){ gl_Position=vec4(position,0.0,1.0); texCoord=uv; tint=color; }
        """.trimIndent())
        val fragment=compile(GL_FRAGMENT_SHADER,"""#version 300 es
            precision mediump float;
            in vec2 texCoord;
            in vec4 tint;
            uniform sampler2D image;
            out vec4 outputColor;
            void main(){ outputColor=texture(image,texCoord)*tint; }
        """.trimIndent())
        program=glCreateProgram(); glAttachShader(program,vertex); glAttachShader(program,fragment); glLinkProgram(program)
        val ok=IntArray(1); glGetProgramiv(program,GL_LINK_STATUS,ok,0)
        check(ok[0]!=0) { glGetProgramInfoLog(program) }
        glDeleteShader(vertex); glDeleteShader(fragment)
        val ids=IntArray(1); glGenVertexArrays(1,ids,0); vao=ids[0]; glGenBuffers(1,ids,0); vbo=ids[0]
        glBindVertexArray(vao); glBindBuffer(GL_ARRAY_BUFFER,vbo)
        glBufferData(GL_ARRAY_BUFFER,data.capacity()*4,null,GL_DYNAMIC_DRAW)
        glEnableVertexAttribArray(0); glVertexAttribPointer(0,2,GL_FLOAT,false,32,0)
        glEnableVertexAttribArray(1); glVertexAttribPointer(1,2,GL_FLOAT,false,32,8)
        glEnableVertexAttribArray(2); glVertexAttribPointer(2,4,GL_FLOAT,false,32,16)
    }
    private fun compile(type: Int, source: String): Int {
        val shader=glCreateShader(type); glShaderSource(shader,source); glCompileShader(shader)
        val ok=IntArray(1); glGetShaderiv(shader,GL_COMPILE_STATUS,ok,0)
        check(ok[0]!=0) { glGetShaderInfoLog(shader) }; return shader
    }
    fun begin(camera: Camera2D, width: Int, height: Int) {
        this.camera=camera; this.width=width; this.height=height; vertices=0; data.clear()
        glUseProgram(program); glBindVertexArray(vao); glActiveTexture(GL_TEXTURE0)
        glUniform1i(glGetUniformLocation(program,"image"),0)
    }
    fun draw(id: Int, transform: Transform, size: Vec2, color: Color) = drawMatrix(id,transform.matrix(),size,color)
    fun drawMatrix(id: Int, matrix: Mat3, size: Vec2, color: Color) {
        if(texture!=id || data.remaining()<48) flush()
        texture=id
        val corners=listOf(Vec2(-size.x/2,-size.y/2),Vec2(size.x/2,-size.y/2),Vec2(size.x/2,size.y/2),Vec2(-size.x/2,size.y/2))
        val uv=listOf(Vec2(0f,1f),Vec2(1f,1f),Vec2(1f,0f),Vec2(0f,0f))
        for(i in intArrayOf(0,1,2,0,2,3)) {
            val p=camera.worldToClip(matrix.map(corners[i]),width,height)
            data.put(p.x).put(p.y).put(uv[i].x).put(uv[i].y).put(color.r).put(color.g).put(color.b).put(color.a); vertices++
        }
    }
    fun flush() {
        if(vertices==0) return
        data.flip(); glBindTexture(GL_TEXTURE_2D,texture); glBindBuffer(GL_ARRAY_BUFFER,vbo)
        glBufferSubData(GL_ARRAY_BUFFER,0,data.remaining()*4,data); glDrawArrays(GL_TRIANGLES,0,vertices)
        data.clear(); vertices=0
    }
}

`````

## tools/CoreSmoke.kt

`````kotlin
import world.engine.core.*
import world.engine.math.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.abs

/** Dependency-light executable checks supplement (not replace) the Gradle/JUnit/device suites. */
fun main() {
    var passed=0
    fun test(name: String,action: ()->Unit) { action(); passed++; println("PASS: $name") }
    fun rejects(action: ()->Unit) { var rejected=false; try { action() } catch(e: IllegalArgumentException) { rejected=true }; check(rejected) }
    test("affine hierarchy including nonuniform scale and shear") {
        val a=Transform(Vec2(10f,-4f),35f,Vec2(2f,.5f)).matrix()
        val b=Transform(Vec2(3f,7f),75f).matrix()
        val p=Vec2(8f,9f); val actual=(a*b).inverse().map((a*b).map(p))
        check(abs(actual.x-p.x)<.0001f && abs(actual.y-p.y)<.0001f)
    }
    test("parent order independent picking and recursive deletion membership") {
        val root=Node(components=listOf(TransformComponent(Transform(Vec2(120f,40f),35f,Vec2(2f,.5f)))))
        val child=Node(parent=root.id,components=listOf(TransformComponent(Transform(Vec2(10f,8f),20f)),SpriteComponent()))
        val scene=Scene(nodes=listOf(child,root)).validated()
        val center=scene.worldMatrices().getValue(child.id).map(Vec2())
        check(scene.pick(center)==child); check(scene.descendants(root.id)==setOf(root.id,child.id))
        rejects { Scene(nodes=listOf(root.copy(parent=child.id),child)).validated() }
    }
    test("nested prefabs expand, preserve overrides and retain UUIDs on refresh") {
        val leafId=UUID.randomUUID().toString(); val outerId=UUID.randomUUID().toString()
        val wheel=Node(name="Wheel")
        val root=Node(name="Car",components=listOf(TransformComponent()))
        val anchor=Node(parent=root.id,nestedPrefab=leafId,components=listOf(TransformComponent()))
        val definitions=mapOf(leafId to PrefabDefinition(nodes=listOf(wheel)),outerId to PrefabDefinition(nodes=listOf(root,anchor)))
        val original=Prefabs.instantiate(outerId,definitions,Vec2(50f,60f))
        val edited=original.map { if(it.name=="Wheel") Prefabs.override(it.copy(name="Custom").moved(Vec2(7f,9f)),"name","transform") else it }
        val changed=definitions+(leafId to PrefabDefinition(nodes=listOf(wheel.copy(name="Source update"))))
        val refreshed=Prefabs.instantiate(outerId,changed,Vec2(50f,60f),edited)
        check(refreshed.map { it.id }==original.map { it.id })
        check(refreshed.single { it.name=="Custom" }.transform.position==Vec2(7f,9f))
        check(Prefabs.instantiate(outerId,changed,Vec2(),edited,false).any { it.name=="Source update" })
        val captured=Prefabs.capture(Scene(nodes=refreshed),original.first().id)
        check(captured.nodes.size==2 && captured.dependencies()==setOf(leafId))
        check(captured.nodes.first().id==root.id)
    }
    test("cyclic definitions and duplicate identities are rejected") {
        val id=UUID.randomUUID().toString()
        rejects { Prefabs.instantiate(id,mapOf(id to PrefabDefinition(nodes=listOf(Node(nestedPrefab=id))))) }
        val n=Node(); rejects { Scene(nodes=listOf(n,n)).validated() }
        rejects { Scene(nodes=listOf(n.copy(parent="absent"))).validated() }
    }
    test("serialized hierarchy references and legacy references round-trip") {
        val id=UUID.randomUUID().toString()
        val root=Node(components=listOf(TransformComponent()))
        val child=Node(parent=root.id,components=listOf(TransformComponent(),SpriteComponent(assetId=id)))
        val scene=Scene(nodes=listOf(root,child)).validated()
        check(Json.decodeFromString<Scene>(Json.encodeToString(scene)).validated()==scene)
        check(scene.assetReferences()==setOf(id))
        val legacy=Scene(nodes=listOf(Node(components=listOf(TransformComponent(),SpriteComponent(asset="Sprites/$id.png")))))
        check(legacy.assetReferences()==setOf(id))
        check(Prefabs.capture(legacy,legacy.nodes.first().id).dependencies()==setOf(id))
    }
    test("hierarchy edits remain one undo command and branch clears redo") {
        val root=Node(components=listOf(TransformComponent())); val child=Node(parent=root.id)
        val before=Scene(nodes=listOf(root,child)); val after=before.replace(root.moved(Vec2(30f,40f)))
        val history=CommandStack(); history.commit(before,after)
        check(history.undo(after)==before); check(history.redo(before)==after)
        history.undo(after); history.commit(before,Scene()); check(!history.canRedo)
        check(history.retainedScenes().isNotEmpty())
    }
    println("$passed core smoke checks passed; Android UI, bitmap, SQLite and GLES execution not tested here.")
}

`````

## tools/check_asset_schema.py

`````python
#!/usr/bin/env python3
"""Execute the production schema with host SQLite; not an Android SQLite/device test."""
from pathlib import Path
import re
import sqlite3

root = Path(__file__).resolve().parents[1]
source = (root / 'engine-assets/src/main/kotlin/world/engine/assets/AssetDatabase.kt').read_text()
statements = re.findall(r'db\.execSQL\("(CREATE [^"]+)"\)', source)
assert len(statements) == 6
connection = sqlite3.connect(':memory:')
connection.execute('PRAGMA foreign_keys=ON')
for statement in statements:
    connection.execute(statement)

def add(identity, kind='IMAGE', recipe=None):
    connection.execute('INSERT INTO assets VALUES(?,?,?,?,?,?,?,?,?,?,?)', (identity, 'name', 'folder', kind, identity + '.png', 1, 64, 64, '[]', 0, recipe))

add('image', recipe='Vehicle:1')
add('prefab', 'PREFAB')
connection.execute('INSERT INTO edges VALUES(?,?)', ('prefab', 'image'))
connection.execute('INSERT INTO sheets VALUES(?,?)', ('image', '[]'))
connection.execute('UPDATE assets SET name=?,folder=?,favorite=? WHERE id=?', ('Renamed', 'Moved/Nested', 1, 'image'))
assert connection.execute('SELECT target FROM edges WHERE owner=?', ('prefab',)).fetchone() == ('image',)
connection.commit()
try:
    connection.execute('DELETE FROM assets WHERE id=?', ('image',))
except sqlite3.IntegrityError:
    connection.rollback()
else:
    raise AssertionError('Referenced asset deletion was not blocked')
try:
    add('duplicate-recipe', recipe='Vehicle:1')
except sqlite3.IntegrityError:
    connection.rollback()
else:
    raise AssertionError('Recipe uniqueness was not enforced')
connection.execute('DELETE FROM assets WHERE id=?', ('prefab',))
assert connection.execute('SELECT count(*) FROM edges').fetchone()[0] == 0
connection.execute('DELETE FROM assets WHERE id=?', ('image',))
assert connection.execute('SELECT count(*) FROM sheets').fetchone()[0] == 0
connection.execute('INSERT OR IGNORE INTO tombstones VALUES(?)', ('image',))
connection.execute('INSERT OR IGNORE INTO tombstones VALUES(?)', ('image',))
assert connection.execute('SELECT count(*) FROM tombstones').fetchone()[0] == 1
print('PASS: actual schema creation, stable UUID edges on metadata moves, reference-restricted deletion, recipe uniqueness, cascading cleanup and tombstones (host SQLite).')

`````

## tools/check_engine_standalone.py

`````python
#!/usr/bin/env python3
"""Compile engine modules and execute core checks without Gradle/Compose.

Diagnostic only: uses Kotlin 1.9.23 from kotlin-jupyter-kernel 0.12.0.322,
not the app's pinned Gradle/Kotlin 1.9.24 toolchain. No Android runtime tests.
Install optional tools with: pip install jdk4py==17.0.9.2 kotlin-jupyter-kernel==0.12.0.322
Provide an actual Android API 34 android.jar with --android-jar.
"""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--android-jar', type=Path, required=True)
parser.add_argument('--java-home', type=Path)
parser.add_argument('--jars', type=Path)
args = parser.parse_args()
if args.java_home is None:
    import jdk4py
    args.java_home = Path(jdk4py.JAVA_HOME)
if args.jars is None:
    import importlib.util
    spec = importlib.util.find_spec('run_kotlin_kernel')
    if spec is None:
        raise SystemExit('Install the documented kotlin-jupyter-kernel version or provide --jars')
    args.jars = Path(next(iter(spec.submodule_search_locations))) / 'jars'
root = Path(__file__).resolve().parents[1]
fat = args.jars / 'kotlin-jupyter-kernel-0.12.0-322-all.jar'
assert fat.is_file() and args.android_jar.is_file()
jars = sorted(p for p in args.jars.glob('*.jar') if p != fat) + [fat]
classpath = os.pathsep.join(map(str, jars))
java = str(args.java_home / 'bin/java')
modules = ('engine-math', 'engine-core', 'engine-assets', 'engine-io', 'engine-render', 'editor-viewport')
with tempfile.TemporaryDirectory(prefix='world-engine-check-') as directory:
    work = Path(directory)
    # Load only the genuine serialization plugin from the bundled compiler;
    # avoid registering the notebook scripting plugin a second time.
    plugin = work / 'serialization-plugin.jar'
    with zipfile.ZipFile(plugin, 'w') as archive:
        archive.writestr('META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar', 'org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar\n')
        archive.writestr('META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor', 'org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginOptions\n')
    compiler = [java, '-cp', classpath, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-Xplugin=' + str(plugin)]
    artifacts = []
    for module in modules:
        output = work / (module + '.jar')
        sources = sorted(str(p) for p in (root / module / 'src/main').rglob('*.kt'))
        target_classpath = os.pathsep.join([classpath, str(args.android_jar), *artifacts])
        subprocess.run([*compiler, '-classpath', target_classpath, '-d', str(output), *sources], check=True)
        artifacts.append(str(output))
        print('PASS: separate-module compile ' + module, flush=True)
    smoke = work / 'core-smoke.jar'
    runtime_classpath = os.pathsep.join([*artifacts, classpath])
    subprocess.run([*compiler, '-classpath', runtime_classpath, '-d', str(smoke), str(root / 'tools/CoreSmoke.kt')], check=True)
    subprocess.run([java, '-cp', str(smoke) + os.pathsep + runtime_classpath, 'CoreSmokeKt'], check=True)
print('PASS: standalone engine compilation. This is NOT a Compose/app build or an APK test.')

`````

## tools/validate_structure.py

`````python
#!/usr/bin/env python3
"""Dependency-free repository sanity checks; not an Android compiler or device test."""
from pathlib import Path
import hashlib
import re
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parents[1]
settings = (root / 'settings.gradle.kts').read_text()
modules = re.findall(r'include\(":([\w-]+)"\)', settings)
assert len(modules) == len(set(modules)) == 21
for module in modules:
    build = root / module / 'build.gradle.kts'
    assert build.is_file(), module
    for dependency in re.findall(r'project\(":([\w-]+)"\)', build.read_text()):
        assert dependency in modules, dependency
for xml in root.glob('*/src/**/*.xml'):
    ET.parse(xml)
wrapper = root / 'gradle/wrapper/gradle-wrapper.jar'
assert hashlib.sha256(wrapper.read_bytes()).hexdigest() == 'cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8'
with zipfile.ZipFile(wrapper) as archive:
    assert archive.testzip() is None
    assert 'org/gradle/wrapper/GradleWrapperMain.class' in archive.namelist()
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
assert 'uses-permission' not in manifest
assert '0x00030000' in manifest
for source in root.glob('*/src/**/*.kt'):
    text = source.read_text()
    assert 'TODO' not in text and 'NotImplementedError' not in text, source
assert len(list(root.glob('*/src/test/**/*.kt'))) == 3
assert len(list(root.glob('*/src/androidTest/**/*.kt'))) == 4
asset_build=(root / 'engine-assets/build.gradle.kts').read_text()
assert 'kotlin("plugin.serialization")' in asset_build
assert 'api(project(":engine-assets"))' in (root / 'editor-assets/build.gradle.kts').read_text()
assert 'api(project(":editor-assets"))' in (root / 'editor-ui/build.gradle.kts').read_text()
generator=(root / 'engine-assets/src/main/kotlin/world/engine/assets/ProceduralAssets.kt').read_text()
counts=re.findall(r'"(?:Character|Enemy|NPC|Animal|Vehicle|Building|Nature|Prop|Texture)" to (\d+)', generator)
assert sum(map(int,counts))==335
print(f'PASS: {len(modules)} modules, project references, XML, official wrapper integrity, permission policy, and test-source presence.')
print('Android compilation, JUnit execution, Compose tests and GLES device tests are NOT covered by this check.')

`````
