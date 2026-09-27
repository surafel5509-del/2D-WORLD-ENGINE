# 2D WORLD Engine

Native Android 2D scene editor, written in Kotlin with Jetpack Compose and OpenGL ES 3.

## Current status

**Phase 3 runtime and authoring source is now implemented. Full Android app build and device acceptance are being checked; see the Phase 3 delivery guide for final status.** Do not treat this as a fully verified or finished Unity-class engine.

- Phase 1: dashboard, project wizard, scene editing, touch viewport, image import, atomic JSON saves, undo/redo and recovery snapshots.
- Phase 2: SQLite UUID asset catalog, grid/list browser, metadata/tags/favorites/search/type filters, drag-to-viewport placement, texture operations, named sprite-sheet slicing and ZIP export, 335 original procedural recipes, modular prefab splitting, hierarchical transforms, nested prefab instances and property overrides.
- Phase 3: animation tracks/events and 65 presets, real JBox2D physics, collider/joint editors, input mapping and virtual controls, camera behaviors, GL debug lines, and nondestructive Play/Pause/Step/Stop.
- No inactive sound, particles, script-event consumers or APK-export controls are exposed.

### Delivery guides and full source

- [Phase 3 implementation and verification status](docs/PHASE3.md)

- [Phase 2 architecture, file changes, exact scope, instructions and limitations](docs/PHASE2.md)
- [Phase 2 complete new/changed file listing](docs/PHASE2_SOURCE.md)
- [Phase 2 acceptance walkthrough](docs/PHASE2_VERIFICATION.md)
- [Original master specification](docs/MASTER_SPEC.md)
- [Historical Phase 1 guide](docs/PHASE1_README.md) and [source snapshot](docs/PHASE1_SOURCE.md)

The code files in their modules are authoritative; source appendices are historical delivery snapshots.

## Build and run

Install **JDK 17**, Android SDK **34**, and Build Tools **34.0.0**. Open the repository in Android Studio (Koala or newer), select Gradle JDK 17, configure the Android SDK location, sync and run `app` on an Android 7+ device with GLES 3 support.

```sh
./gradlew :engine-math:test :engine-core:test :engine-animation:testDebugUnitTest :engine-physics:testDebugUnitTest :engine-render:testDebugUnitTest :app:assembleDebug
./gradlew :app:installDebug
./gradlew :engine-physics:connectedDebugAndroidTest :engine-assets:connectedDebugAndroidTest :engine-io:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24; minimum SDK 24, target/compile SDK 34. Initial development-tool/dependency downloads need connectivity; editor operation is offline. On Windows use `gradlew.bat`.

Successful Gradle packaging produces `app/build/outputs/apk/debug/app-debug.apk`. **The full Gradle/Compose build has passed in GitHub Actions and produced a real debug APK artifact.** See the [verification ledger](docs/PHASE3_VERIFICATION.md) for exact revisions, test results and artifact links; no signed standalone-game export is claimed.

## Validation actually performed

- Eight separately compiled engine/runtime modules and **16 executable core/runtime smoke groups**: passed, using actual JBox2D and Android 34 APIs.
- Full pinned-toolchain Gradle/Compose build, debug APK/test APK assembly, and JVM unit tasks: passed in CI; exact revisions/results are recorded in the verification ledger.
- Host SQLite and repository/module/XML/wrapper checks: passed.
- Emulator acceptance is recorded separately in the ledger. Physical phone/tablet/gamepad and sustained-performance checks remain unverified.

## Data safety

Projects use app-private storage: `files/2DWorldProjects/<UUID>/`. No broad storage or internet permission is requested. Uninstalling/clearing app data deletes projects; back up important projects first. Catalog folders are virtual; immutable payload filenames do not change when assets are renamed or moved in the catalog. Asset edits create revisions or new assets, not scene undo commands. Old payload revisions are retained; automatic cleanup is not implemented.

**Phase gate:** verify Phase 3 and confirm before Phase 4 (tilemaps, particles, audio and runtime UI).
