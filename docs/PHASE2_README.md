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
