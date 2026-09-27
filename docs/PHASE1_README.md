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
