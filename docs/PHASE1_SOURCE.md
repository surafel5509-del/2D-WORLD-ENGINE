# Phase 1 — complete file listing

This appendix is a delivery snapshot. Files on disk are authoritative. It contains every delivered file except this self-referential appendix. The wrapper JAR is encoded as base64. Android compilation and device verification remain pending; see README.md.

## File inventory

```text
.gitignore
README.md
app/build.gradle.kts
app/src/androidTest/kotlin/world/engine/app/EditorSmokeTest.kt
app/src/androidTest/kotlin/world/engine/app/GlRenderTest.kt
app/src/main/AndroidManifest.xml
app/src/main/kotlin/world/engine/app/MainActivity.kt
app/src/main/res/values/styles.xml
app/src/main/res/xml/file_paths.xml
build.gradle.kts
docs/MASTER_SPEC.md
docs/VERIFICATION.md
editor-animation/README.md
editor-animation/build.gradle.kts
editor-animation/src/main/AndroidManifest.xml
editor-assets/README.md
editor-assets/build.gradle.kts
editor-assets/src/main/AndroidManifest.xml
editor-build/README.md
editor-build/build.gradle.kts
editor-build/src/main/AndroidManifest.xml
editor-scripting/README.md
editor-scripting/build.gradle.kts
editor-scripting/src/main/AndroidManifest.xml
editor-ui/build.gradle.kts
editor-ui/src/main/AndroidManifest.xml
editor-ui/src/main/kotlin/world/engine/editor/EditorViewModel.kt
editor-ui/src/main/kotlin/world/engine/editor/WorldEditorApp.kt
editor-viewport/build.gradle.kts
editor-viewport/src/main/AndroidManifest.xml
editor-viewport/src/main/kotlin/world/engine/viewport/WorldViewport.kt
engine-ai/README.md
engine-ai/build.gradle.kts
engine-ai/src/main/AndroidManifest.xml
engine-animation/README.md
engine-animation/build.gradle.kts
engine-animation/src/main/AndroidManifest.xml
engine-assets/README.md
engine-assets/build.gradle.kts
engine-assets/src/main/AndroidManifest.xml
engine-audio/README.md
engine-audio/build.gradle.kts
engine-audio/src/main/AndroidManifest.xml
engine-build/README.md
engine-build/build.gradle.kts
engine-build/src/main/AndroidManifest.xml
engine-core/build.gradle.kts
engine-core/src/main/kotlin/world/engine/core/Scene.kt
engine-core/src/test/kotlin/world/engine/core/SceneTest.kt
engine-io/build.gradle.kts
engine-io/src/androidTest/kotlin/world/engine/io/ProjectStoreTest.kt
engine-io/src/main/AndroidManifest.xml
engine-io/src/main/kotlin/world/engine/io/ProjectStore.kt
engine-math/build.gradle.kts
engine-math/src/main/kotlin/world/engine/math/Math.kt
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
engine-render/src/main/kotlin/world/engine/render/SceneRenderer.kt
engine-render/src/main/kotlin/world/engine/render/SpriteBatch.kt
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
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
gradle.properties
gradlew
gradlew.bat
settings.gradle.kts
tools/validate_structure.py
```

## .gitignore

````text
*.iml
.gradle/
**/build/
local.properties
.idea/
*.apk
*.aab

````

## README.md

````markdown
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

````

## app/build.gradle.kts

````kotlin
plugins { id("com.android.application"); kotlin("android") }
android {
    namespace = "world.engine.app"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationId = "world.engine.editor"
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}
dependencies {
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    implementation(project(":editor-ui"))
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

````

## app/src/androidTest/kotlin/world/engine/app/EditorSmokeTest.kt

````kotlin
package world.engine.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class EditorSmokeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun createAddSaveAndReturnToDashboard() {
        compose.onNodeWithText("New project").performClick()
        compose.onNodeWithText("Create",useUnmergedTree=true).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Add sprite").fetchSemanticsNodes().isNotEmpty() }
        repeat(3) { compose.onNodeWithText("Add sprite").performClick() }
        compose.onNodeWithText("Save",substring=false).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("MyGame • saved").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Projects",substring=false).performClick()
        compose.onNodeWithText("Save & close").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("New project").fetchSemanticsNodes().isNotEmpty() }
    }
}

````

## app/src/androidTest/kotlin/world/engine/app/GlRenderTest.kt

````kotlin
package world.engine.app

import android.opengl.EGL14.*
import android.opengl.GLES30.*
import org.junit.Assert.*
import org.junit.Test
import world.engine.core.*
import world.engine.math.*
import world.engine.render.*
import java.nio.ByteBuffer

/** Real offscreen GLES 3 render/readback; requires an ES 3-capable device or emulator. */
class GlRenderTest {
    @Test fun solidSpriteRendersAndContextCanBeRecreated() {
        repeat(2) {
            val display=eglGetDisplay(EGL_DEFAULT_DISPLAY)
            val version=IntArray(2)
            assertTrue(eglInitialize(display,version,0,version,1))
            val configs=arrayOfNulls<android.opengl.EGLConfig>(1); val count=IntArray(1)
            val attrs=intArrayOf(EGL_RENDERABLE_TYPE,0x40,EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE)
            assertTrue(eglChooseConfig(display,attrs,0,configs,0,1,count,0)); assertTrue(count[0]>0)
            val context=eglCreateContext(display,configs[0],EGL_NO_CONTEXT,intArrayOf(EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE),0)
            val surface=eglCreatePbufferSurface(display,configs[0],intArrayOf(EGL_WIDTH,64,EGL_HEIGHT,64,EGL_NONE),0)
            try {
                assertTrue(eglMakeCurrent(display,surface,surface,context))
                val errors=mutableListOf<String>(); val renderer=SceneRenderer { errors.add(it) }
                renderer.frame=RenderFrame(scene=Scene(nodes=listOf(Node(components=listOf(TransformComponent(),SpriteComponent(Vec2(32f,32f),Color(1f,0f,0f)))))))
                renderer.onSurfaceCreated(null,null); renderer.onSurfaceChanged(null,64,64); renderer.onDrawFrame(null)
                val pixel=ByteBuffer.allocateDirect(4)
                glReadPixels(32,32,1,1,GL_RGBA,GL_UNSIGNED_BYTE,pixel)
                assertEquals(errors.toString(),emptyList<String>(),errors)
                assertEquals(GL_NO_ERROR,glGetError())
                assertTrue((pixel.get(0).toInt() and 255)>240)
                assertTrue((pixel.get(1).toInt() and 255)<10)
            } finally {
                eglMakeCurrent(display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT)
                eglDestroySurface(display,surface); eglDestroyContext(display,context); eglTerminate(display)
            }
        }
    }
}

````

## app/src/main/AndroidManifest.xml

````xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-feature android:glEsVersion="0x00030000" android:required="true" />
    <application android:theme="@style/Theme.WorldEngine" android:label="2D WORLD" android:allowBackup="false" android:supportsRtl="true">
        <activity android:name="world.engine.app.MainActivity" android:exported="true" android:windowSoftInputMode="adjustResize">
            <intent-filter><action android:name="android.intent.action.MAIN" /><category android:name="android.intent.category.LAUNCHER" /></intent-filter>
        </activity>
        <provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.files" android:exported="false" android:grantUriPermissions="true">
            <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>

````

## app/src/main/kotlin/world/engine/app/MainActivity.kt

````kotlin
package world.engine.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import world.engine.editor.WorldEditorApp

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { WorldEditorApp() } }
}

````

## app/src/main/res/values/styles.xml

````xml
<resources>
    <style name="Theme.WorldEngine" parent="android:style/Theme.Material.NoActionBar">
        <item name="android:fontFamily">sans</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:statusBarColor">#101318</item>
        <item name="android:navigationBarColor">#101318</item>
        <item name="android:windowActionModeOverlay">true</item>
        <item name="android:colorAccent">#80C7FF</item>
    </style>
</resources>

````

## app/src/main/res/xml/file_paths.xml

````xml
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <cache-path name="shared_exports" path="shared_exports/" />
</paths>

````

## build.gradle.kts

````kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    kotlin("android") version "1.9.24" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
}

````

## docs/MASTER_SPEC.md

````markdown
# 2D-WORLD-ENGINE

╔══════════════════════════════════════════════════════════════════════╗
║  2D WORLD ENGINE — PROFESSIONAL UNITY-GRADE MOBILE 2D GAME ENGINE   ║
║  Master Build Prompt v3.0 (Phased, Realistic, Production-Ready)     ║
╚══════════════════════════════════════════════════════════════════════╝

═══════════════════════════════════════════════════════════════════════
§0. ROLE & IDENTITY
═══════════════════════════════════════════════════════════════════════
You are the lead architect of "2D WORLD Engine" — a professional,
Unity-class 2D game engine and IDE built natively for Android.
You have deep expertise in:
  • Kotlin + Jetpack Compose
  • OpenGL ES 3.x / Vulkan 2D rendering pipelines
  • ECS (Entity-Component-System) architecture
  • Box2D / JBox2D physics integration
  • Android build tooling (Gradle, aapt2, d8, apksigner)
  • Editor UX design for touchscreens
  • Real-time game engine internals

You build software like a senior engineer at Unity, Epic, or Godot.
You never ship stubs. You never say "Coming Soon". You never fake builds.

═══════════════════════════════════════════════════════════════════════
§1. PRODUCT VISION
═══════════════════════════════════════════════════════════════════════
2D WORLD Engine is a complete, offline-first, mobile-native 2D game
development environment. A user on a phone or tablet can:

  → Create a project from a template
  → Build scenes visually with touch
  → Import / generate / split assets modularly
  → Animate sprites with a timeline editor
  → Add physics, collision, particles, audio, UI
  → Write scripts (text + visual nodes)
  → Test the game instantly with Play mode
  → Debug with profiler + console
  → Build a real, installable Android APK
  → Inspect and modify 5 complete example games

It must feel like "Unity for Android" — but optimized for touch,
offline, and small screens. Productivity over decoration.
Stability over flashiness. Real features over fake UI.

═══════════════════════════════════════════════════════════════════════
§2. NON-NEGOTIABLE RULES
═══════════════════════════════════════════════════════════════════════
1.  NO FAKE FEATURES. Every visible UI control must do something real.
2.  NO "COMING SOON" labels. If not implemented, do not show it.
3.  NO static mock screens. Every screen is functional.
4.  NO fake APK builds. Either real build or documented limitation.
5.  NO copyrighted assets. Only CC0 / public domain / procedural.
6.  OFFLINE-FIRST. Internet is optional, never required for core work.
7.  BUILD IN PHASES. Never attempt the whole engine in one response.
8.  After each phase, STOP and ask the user to confirm before continuing.
9.  Every code block must compile and run on a real Android device.
10. If a platform limitation exists, state it clearly and give the
    closest real, working alternative.

═══════════════════════════════════════════════════════════════════════
§3. TECHNICAL STACK (LOCKED)
═══════════════════════════════════════════════════════════════════════
Language        : Kotlin (100%, no Java unless required by a library)
UI Framework    : Jetpack Compose (Material 3, dark theme default)
Viewport        : GLSurfaceView + OpenGL ES 3.0 with render thread
Physics         : JBox2D (pure Java Box2D port — no NDK needed)
Audio           : SoundPool (SFX) + ExoPlayer (music)
Scripting       : Custom Kotlin DSL interpreter + visual node graph
Serialization   : kotlinx.serialization (JSON) for project/scene files
Async           : Kotlin Coroutines + Flow
Image loading   : Custom OpenGL texture loader + Coil for UI previews
Architecture    : MVVM for editor, ECS for runtime
Min SDK         : 24  (Android 7.0)
Target SDK      : 34  (Android 14)
Compile SDK     : 34
Build system    : Gradle 8.x with Kotlin DSL
Testing         : JUnit5 + Compose UI Test + instrumented GL tests

═══════════════════════════════════════════════════════════════════════
§4. MODULAR GRADLE ARCHITECTURE
═══════════════════════════════════════════════════════════════════════
:engine-math        — Vec2, Mat3, Transform, Rect, Color, MathUtils
:engine-core        — ECS, Scene, Node, Component, Prefab, Resource
:engine-render      — SpriteBatch, Camera2D, Shader, Texture, Atlas
:engine-physics     — JBox2D wrapper, Body, Shape, Joint, Query
:engine-audio       — AudioManager, SFX, Music, Mixer, Spatial2D
:engine-animation   — Timeline, Keyframe, AnimationClip, Player
:engine-assets      — AssetDatabase, Importer, DependencyGraph
:engine-scripting   — Lexer, Parser, Interpreter, VM, NodeGraph
:engine-ai          — StateMachine, Behavior, Pathfinder, A*
:engine-tilemap     — TileSet, TileMap, AutoTile, Layer
:engine-particles   — ParticleSystem, Emitter, Preset library
:engine-ui          — Runtime UI system (not editor UI)
:engine-io          — ProjectIO, SaveSystem, ImportExport
:engine-build       — APK builder orchestration + aapt2/d8 wrapper

:editor-ui          — Compose panels (Hierarchy, Inspector, Console)
:editor-viewport    — GL viewport + touch transform gizmos
:editor-assets      — Asset browser, sprite editor, tilemap editor
:editor-animation    — Timeline UI
:editor-scripting   — Code editor + visual node editor
:editor-build       — Build wizard UI

:app                — Dashboard + wires all modules together

═══════════════════════════════════════════════════════════════════════
§5. ON-DEVICE PROJECT STRUCTURE
═══════════════════════════════════════════════════════════════════════
/storage/emulated/0/2DWorldProjects/<ProjectName>/
  project.json              ← project manifest
  /Scenes
  /Scripts
  /Sprites
  /Textures
  /Animations
  /Tilesets
  /Tilemaps
  /Materials
  /Particles
  /Audio/Music
  /Audio/SFX
  /UI
  /Prefabs
  /Physics
  /Fonts
  /Shaders
  /Data
  /Resources
  /Plugins
  /Build
  /Exports
  .autosave/                ← crash recovery snapshots

═══════════════════════════════════════════════════════════════════════
§6. DEVELOPMENT PHASES (STRICT ORDER)
═══════════════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────────────┐
│ PHASE 1 — FOUNDATION (MVP, RUNNABLE)                                │
├─────────────────────────────────────────────────────────────────────┤
│ Goal: An actually working editor where you can create a project,    │
│ build a scene, add sprites, move them with touch, save, reopen,     │
│ and see the exact same scene.                                       │
│                                                                     │
│ Deliverables:                                                       │
│  1. Multi-module Gradle project skeleton                            │
│  2. engine-math: Vec2, Mat3, Transform, Rect, Color                 │
│  3. engine-core: ECS + Scene + Node + Component + serialization     │
│  4. engine-render: SpriteBatch, Camera2D, Texture loader (GL ES 3)  │
│  5. editor-ui: Dashboard, Editor shell (top/left/right/bottom)      │
│  6. editor-viewport: GL viewport with touch drag + pinch zoom       │
│  7. Project creation wizard (name, package, orientation, template)  │
│  8. Project dashboard with recent projects grid                     │
│  9. Undo/redo command stack                                         │
│ 10. Auto-save every 30 seconds + crash recovery prompt              │
│ 11. Full Android manifest, permissions, file provider              │
│ 12. Build & run instructions                                        │
│                                                                     │
│ Verification: User creates project "MyGame", adds 3 sprites, moves  │
│ them, saves, force-closes app, reopens → identical scene.          │
│                                                                     │
│ STOP. Ask user to confirm before Phase 2.                           │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ PHASE 2 — ASSET SYSTEM & SPRITE PIPELINE                            │
├─────────────────────────────────────────────────────────────────────┤
│  1. engine-assets: AssetDatabase (SQLite + files), UUID system,     │
│     dependency graph, reference integrity (moves never break refs)  │
│  2. Asset Browser UI: grid/list, tags, favorites, search, filters   │
│  3. Drag-and-drop from browser into scene                           │
│  4. Asset Inspector: preview, metadata, actions (Use/Edit/Split/    │
│     Duplicate/Rename/Replace/Delete/Favorite/CreatePrefab)          │
│  5. Sprite Sheet Editor: import, auto-slice, grid-slice, manual,    │
│     rename/reorder frames, export                                    │
│  6. Texture Editor: crop, resize, rotate, flip, tile, colorize      │
│  7. Procedural asset generators (so we have 300+ real assets):      │
│       • 30+ characters (procedural pixel art, CC0-generated)        │
│       • 30+ enemies, 20+ NPCs, 20+ animals                          │
│       • 25+ vehicles (cars, trucks, tanks, bikes — modular parts)   │
│       • 40+ buildings, houses, roads, bridges                       │
│       • 30+ nature (trees, rocks, water, grass tiles)               │
│       • 40+ props (barrels, crates, weapons, furniture)             │
│       • 100+ textures (tileable, seamless, procedurally generated)  │
│  8. MODULAR SPLIT SYSTEM (critical):                                │
│       • Any composite asset (car, character, house) can be split    │
│       • Split produces a Prefab with named child nodes              │
│       • Each child is independently editable/animateable            │
│       • Example: Car → Body, 4 Wheels, Windows, Lights, Doors,      │
│         Bumper, Mirrors, Spoiler, Interior, Collision               │
│  9. Prefab system: create from selection, instance across scenes,   │
│     overrides, nested prefabs                                        │
│                                                                     │
│ STOP. Confirm before Phase 3.                                       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ PHASE 3 — ANIMATION, PHYSICS, INPUT, CAMERA                          │
├─────────────────────────────────────────────────────────────────────┤
│  1. engine-animation:                                               │
│       • Timeline with keyframes                                     │
│       • Sprite frame animation, position/rotation/scale/color/alpha │
│       • Events: play sound, spawn particle, call script, change     │
│         sprite, trigger (frame-precise)                              │
│       • Loop, ping-pong, FPS control                                │
│       • 50+ built-in animations (idle/walk/run/jump/attack/hurt/    │
│         death/roll/dash/shoot/reload/climb/swim per archetype)      │
│  2. engine-physics (JBox2D):                                        │
│       • Static, Dynamic, Kinematic bodies                           │
│       • Shapes: Circle, Rect, Polygon, Capsule, Compound            │
│       • Sensors / triggers                                          │
│       • Joints: Fixed, Distance, Revolute, Prismatic, Spring,       │
│         Wheel, Rope                                                  │
│       • Materials: friction, restitution, density                   │
│       • Visual debug overlay (GL lines)                             │
│  3. Physics Editor:                                                 │
│       • Auto-generate colliders from sprite alpha                   │
│       • Manual shape drawing on viewport                            │
│       • Per-part colliders on split prefabs                         │
│  4. Input System:                                                   │
│       • Input Map UI (Move Up/Down/Left/Right/Jump/Attack/etc.)     │
│       • Touch, keyboard, mouse, gamepad, virtual joystick           │
│       • Rebinding                                                    │
│  5. Camera System:                                                  │
│       • Follow target, smooth, dead zone, zoom, shake presets       │
│         (Small/Medium/Large/Explosion/Damage/Earthquake)            │
│       • Limits, rotation, multi-camera, transitions                 │
│       • Parallax layers                                              │
│                                                                     │
│ STOP. Confirm before Phase 4.                                       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ PHASE 4 — TILEMAP, PARTICLES, AUDIO, UI                             │
├─────────────────────────────────────────────────────────────────────┤
│  1. engine-tilemap: TileSet editor, TileMap editor, auto-tiling,    │
│     terrain rules, brushes (brush/rect/line/fill/eraser/stamp/      │
│     random), tile rotate/flip, collision gen, animated tiles,       │
│     isometric mode                                                   │
│  2. engine-particles: emitter, 100+ presets (fire, smoke, dust,     │
│     rain, snow, sparks, explosion, magic, energy, lightning,        │
│     impact, splash, leaves, confetti, fog, steam, engine smoke,     │
│     rocket, hits, healing, teleport). Live preview in editor.       │
│  3. engine-audio: 30+ CC0 SFX (UI click/hover, jump, footstep, hit, │
│     explosion, gunshot, vehicle, engine, brake, door, coin, powerup,│
│     damage, death, menu, notification, success, failure, ambient).  │
│     Editor: volume, pitch, loop, pan, 2D spatial, fade in/out.     │
│  4. engine-ui (runtime): Panel, Button, Label, Image, ProgressBar,  │
│     Slider, Checkbox, Toggle, InputField, ScrollView, List, Grid,   │
│     Tabs, Menu, Dialog, HUD, VirtualJoystick, TouchButton.          │
│     Editor: anchors, margins, alignment, fonts, colors, shadows,    │
│     rounded corners, transparency.                                   │
│                                                                     │
│ STOP. Confirm before Phase 5.                                       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ PHASE 5 — SCRIPTING, VISUAL SCRIPTING, AI, PATHFINDING              │
├─────────────────────────────────────────────────────────────────────┤
│  1. Text scripting:                                                 │
│       • Kotlin-flavored DSL: player.speed = 200                     │
│       • Blocks: on_start, on_update, on_collision(enemy), on_input │
│       • Syntax highlighting, autocomplete, error markers, line      │
│         numbers, search/replace, format, templates, doc popup       │
│       • Sandboxed interpreter (no file/network unless allowed)      │
│  2. Visual scripting: node graph editor with categories:            │
│       Event, Condition, Compare, Math, Variable, Set/Get, Move,     │
│       Rotate, Spawn, Destroy, PlayAnimation, PlaySound, Wait,       │
│       Timer, Collision, Input, Camera, Scene, UI, Physics, Particle │
│       Drag → Connect → Configure → Test loop.                       │
│  3. AI: Patrol, Chase, Follow, Flee, Attack, Guard, Wander,         │
│     Target selection, Line of sight, State machine editor.          │
│  4. Pathfinding: A* on grid, nav regions, obstacles, dynamic targets│
│                                                                     │
│ STOP. Confirm before Phase 6.                                       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ PHASE 6 — EDITOR POLISH, DEBUGGER, PROFILER, DOCS                   │
├─────────────────────────────────────────────────────────────────────┤
│  1. Full editor UX: dockable/resizable/collapsible panels,          │
│     multi-scene tabs, context menus, tooltips, empty/loading/error  │
│     states, confirmation dialogs.                                   │
│  2. Multi-scene workflow: open multiple scenes as tabs, save all.   │
│  3. Debugger: console (info/warn/error), breakpoints, variable      │
│     inspector, collision viz, FPS, draw calls, memory, bodies,      │
│     particles, audio sources.                                        │
│  4. Profiler: FPS, frame time, CPU/GPU/physics/render time, memory, │
│     texture memory, object count, draw calls + optimization tips.   │
│  5. Themes: Dark (default), Light, High Contrast.                   │
│  6. Localization: English, Amharic, Spanish, French, Arabic,        │
│     Portuguese (i18n-ready architecture, English fully done first). │
│  7. Built-in docs: Getting Started, Projects, Scenes, Assets,       │
│     Sprites, Animation, Physics, TileMaps, UI, Audio, Particles,    │
│     Scripting, Visual Scripting, Building APK, Optimization.        │
│  8. Beginner Mode / Pro Mode toggle (simplified vs full editor).    │
│  9. Professional color picker (RGB/HSV/HEX/Alpha + eyedropper).     │
│ 10. Grid system + snap (grid/pixel/angle/object), iso grid.         │
│ 11. Transform tools: Select/Move/Rotate/Scale/Pivot/Rect/Multi.     │
│ 12. Shader editor (beginner): Grayscale, Glow, Flash, Outline,      │
│     Dissolve, Water, Pixel, ColorReplace + presets.                 │
│ 13. Materials: basic, textured, transparent, blended, modulated.    │
│ 14. Lighting 2D: point, directional, ambient, shadows, light masks, │
│     glow, color, intensity, radius + day/night example.             │
│ 15. Save/Load API: save("key", value), load("key") + slots + auto.  │
│ 16. Game State: MainMenu, Loading, Gameplay, Pause, GameOver,       │
│     Victory, Settings, SaveLoad.                                     │
│                                                                     │
│ STOP. Confirm before Phase 7.                                       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ PHASE 7 — BUILD SYSTEM, EXAMPLE GAMES, OPTIMIZATION                 │
├─────────────────────────────────────────────────────────────────────┤
│  1. APK BUILD SYSTEM (real, not fake):                              │
│     Approach: Bundle a minimal aapt2/d8/zipalign/apksigner toolchain │
│     for arm64 (from AOSP, Apache-2.0) inside the app's assets.       │
│     On-device build pipeline:                                        │
│       a) Prepare project tree (convert scene JSON to compiled form)  │
│       b) Copy template APK skeleton (pre-signed with debug key)      │
│       c) Compile user's assets into resources.arsc via aapt2         │
│       d) Dex user scripts into classes.dex via d8                    │
│       e) Package with zipalign                                        │
│       f) Sign with apksigner (debug keystore generated on first run) │
│       g) Output to /Exports/<app>.apk                                │
│     Build config UI: app name, package ID, version, version code,    │
│       icon, splash, orientation, min/target SDK, permissions, FPS    │
│       target, graphics quality.                                       │
│     Buttons: BUILD DEBUG APK / BUILD RELEASE APK / EXPORT PROJECT    │
│     Progress: Preparing → Compiling → Processing assets → Building   │
│       resources → Packaging → Signing → Finalizing → SUCCESS        │
│     Success screen: APK path, size, version, type, package ID +      │
│       Install / Share / Export buttons (via FileProvider + Intent)   │
│     If Android blocks direct install on this device, provide clear   │
│       "Save to Downloads" + "Open with Package Installer" flow.      │
│     Fallback: export a full Android Studio project as .zip that the  │
│       user can open on PC and build with one command.                │
│                                                                     │
│  2. FIVE COMPLETE EXAMPLE GAMES (real, playable, inspectable):       │
│     • Platformer  — player, jump, enemies, coins, UI, camera,       │
│       physics, animation, sound, levels.                             │
│     • Top-Down RPG — player, NPCs, enemies, dialogue, inventory,    │
│       health, weapons, tilemap, save system.                         │
│     • Top-Down Shooter — weapons, enemies, projectiles, particles,  │
│       AI, waves, HUD, level flow.                                    │
│     • 2D Racing — modular cars, wheels, physics, tracks,            │
│       checkpoints, laps, speed UI, engine sound, drift particles.   │
│     • Adventure (product-level) — multi-scene, progression, NPCs,   │
│       boss, inventory, quests, save/load, settings, audio, UI,      │
│       menus, pause, game over, victory.                              │
│     Each example has an Inspector screen with buttons:              │
│       Open Project | Play | Explore Scene | Explore Scripts |       │
│       Explore Assets | Explore Animations | Explore Physics |       │
│       Explore UI | Explore Project Structure.                        │
│                                                                     │
│  3. Performance:                                                     │
│     • Object pooling (bullets, particles)                            │
│     • Texture atlas batching                                        │
│     • Lazy asset loading                                             │
│     • Background indexing                                            │
│     • Frustum culling                                                │
│     • Draw call merging                                              │
│     • Memory budget per scene                                        │
│                                                                     │
│  4. Error handling: Never crash on missing asset / corrupt scene /  │
│     invalid script / broken ref / failed build. Show actionable      │
│     error UI with Locate/Replace/Ignore/Open actions.                │
│                                                                     │
│  5. Security: Sandboxed scripts, no dangerous commands from imported │
│     projects, safe file parsing, permission minimization.            │
│                                                                     │
│  6. Testing checklist before "complete":                             │
│     [✓] Create project      [✓] Open project                        │
│     [✓] Create scene        [✓] Import asset                        │
│     [✓] Place sprite        [✓] Transform                           │
│     [✓] Split prefab        [✓] Add physics                         │
│     [✓] Add collision       [✓] Create animation                    │
│     [✓] Add particles       [✓] Add sound                           │
│     [✓] Add UI              [✓] Add script                          │
│     [✓] Configure input     [✓] Play test                           │
│     [✓] Debug               [✓] Save/reopen                         │
│     [✓] Build APK           [✓] Export                              │
│     [✓] Install & run       [✓] Open 5 examples                     │
│     [✓] Modify example      [✓] Rebuild example                     │
└─────────────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════════
§7. EDITOR LAYOUT (Unity-inspired, touch-optimized)
═══════════════════════════════════════════════════════════════════════
Top bar    : Project name | Save | Undo | Redo | Play | Pause | Stop |
             Build | Export | Search | Settings | Mode toggle (Beginner/Pro)
Left panel : Scene Hierarchy | Create Object menu | Layers | Prefabs
Center     : GL viewport with transform gizmos, grid, snap, rulers
Right panel: Inspector (components, properties, add component)
Bottom     : Tabs → Assets | Console | Animation | Timeline | Debugger |
             Output | Profiler | Build log
Panels are dockable, resizable, collapsible. On small phones, panels
collapse to icons; viewport can go full-screen. Long-press = context
menu. All controls reachable in 5" portrait and 10" landscape.

═══════════════════════════════════════════════════════════════════════
§8. OUTPUT FORMAT FOR EVERY PHASE RESPONSE
═══════════════════════════════════════════════════════════════════════
When delivering a phase, structure the response EXACTLY as:

  1. ARCHITECTURE DECISIONS for this phase (why these choices)
  2. FILE TREE (new / changed / unchanged files listed clearly)
  3. COMPLETE SOURCE for every new/changed file
     — no "..." placeholders
     — no "// TODO: implement"
     — no "rest of code omitted"
  4. GRADLE CONFIG for new modules
  5. BUILD & RUN steps (Android Studio + command line)
  6. VERIFICATION TESTS (specific, reproducible)
  7. KNOWN LIMITATIONS (honest list)
  8. NEXT PHASE PREVIEW + explicit request for user confirmation

═══════════════════════════════════════════════════════════════════════
§9. BEHAVIORAL CONTRACT WITH THE USER
═══════════════════════════════════════════════════════════════════════
• If the user asks for the entire engine in one response, REFUSE politely.
  Explain that a Unity-class engine is 3–8 person-years of work and
  cannot be built in one message. Propose the phased plan above.
• If the user asks for a feature that is impossible on Android (e.g.,
  silent APK install without user interaction), explain the limitation
  and provide the closest real workflow (e.g., FileProvider + Intent).
• If the user asks for a copyrighted asset, refuse and generate a
  procedural CC0 replacement instead.
• Always be honest about what is working and what is not.
• Always prioritize: functionality > stability > performance > visuals.

═══════════════════════════════════════════════════════════════════════
§10. QUALITY BAR
═══════════════════════════════════════════════════════════════════════
The final product must be usable by:
  • A beginner on a phone → can create a simple game offline.
  • An advanced developer on a tablet → can build a complex 2D game
    with scenes, scripts, physics, animation, tilemaps, particles,
    audio, UI, AI, camera, lighting, prefabs, debugging, profiling,
    and Android APK export.

Code quality:
  • Clean architecture, single-responsibility modules
  • Unit tests for engine-core, engine-physics, engine-scripting
  • No God-classes, no monolithic files
  • Documented public APIs (KDoc)
  • Consistent naming, formatting, and error handling
  • Never crash — degrade gracefully with actionable errors

UX quality:
  • Consistent iconography and typography
  • Touch feedback on every interactive element
  • Smooth transitions, no jank
  • Empty/loading/error states everywhere
  • Confirmation dialogs for destructive actions
  • Search available on every major panel

═══════════════════════════════════════════════════════════════════════
§11. START COMMAND
═══════════════════════════════════════════════════════════════════════
Begin with PHASE 1 — FOUNDATION.

Deliver:
  • The complete multi-module Gradle project skeleton
  • All Phase 1 source files, fully implemented
  • Dashboard, project wizard, editor shell, GL viewport
  • Touch controls (drag, pinch, pan, long-press)
  • Save/load scene as JSON with reference integrity
  • Undo/redo stack
  • Auto-save + crash recovery
  • Build and run instructions
  • Verification test walkthrough

After delivering Phase 1, STOP.
Ask the user to confirm success before moving to Phase 2.

═══════════════════════════════════════════════════════════════════════
END OF MASTER PROMPT — 2D WORLD ENGINE v3.0
═══════════════════════════════════════════════════════════════════════

````

## docs/VERIFICATION.md

````markdown
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

````

## editor-animation/README.md

````markdown
# editor-animation

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## editor-animation/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.editoranimation"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## editor-animation/src/main/AndroidManifest.xml

````xml
<manifest />

````

## editor-assets/README.md

````markdown
# editor-assets

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## editor-assets/build.gradle.kts

````kotlin
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
}
dependencies {
}

````

## editor-assets/src/main/AndroidManifest.xml

````xml
<manifest />

````

## editor-build/README.md

````markdown
# editor-build

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## editor-build/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.editorbuild"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## editor-build/src/main/AndroidManifest.xml

````xml
<manifest />

````

## editor-scripting/README.md

````markdown
# editor-scripting

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## editor-scripting/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.editorscripting"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## editor-scripting/src/main/AndroidManifest.xml

````xml
<manifest />

````

## editor-ui/build.gradle.kts

````kotlin
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

````

## editor-ui/src/main/AndroidManifest.xml

````xml
<manifest />

````

## editor-ui/src/main/kotlin/world/engine/editor/EditorViewModel.kt

````kotlin
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

data class EditorState(
    val projects: List<Project> = emptyList(), val project: Project? = null,
    val scene: Scene = Scene(), val saved: Scene = Scene(), val selected: String? = null,
    val canUndo: Boolean = false, val canRedo: Boolean = false,
    val busy: Boolean = false, val error: String? = null, val recovery: Scene? = null, val recoveryIssue: String? = null,
    val log: List<String> = emptyList()
) { val dirty get() = scene != saved }

/** Main-thread editor state; disk operations use IO dispatching and serialized persistence. */
class EditorViewModel(application: Application): AndroidViewModel(application) {
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
    private fun enter(open: OpenedProject) {
        history.clear(); dragStart=null
        change { it.copy(project=open.project,scene=open.saved,saved=open.saved,selected=null,canUndo=false,canRedo=false,recovery=open.recovery,recoveryIssue=open.recoveryIssue,log=listOf("Opened ${open.project.manifest.name}")) }
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
    fun importSprite(uri: Uri)=task {
        val project=mutable.value.project ?: return@task
        val asset=store.importSprite(project,uri)
        val s=mutable.value; val node=sprite(s.scene.nodes.size+1,Vec2(),asset)
        edit(s.scene.copy(nodes=s.scene.nodes+node)); select(node.id); note("Imported $asset")
    }
    fun move(id: String, p: Vec2, end: Boolean) {
        if(mutable.value.busy || mutable.value.recovery!=null || mutable.value.recoveryIssue!=null) return
        val s=mutable.value; val node=s.scene.nodes.find { it.id==id } ?: return
        if(dragStart==null) dragStart=s.scene
        change { it.copy(scene=it.scene.replace(node.moved(p))) }
        if(end) finishDrag()
    }
    fun updateNode(node: Node) {
        try { edit(mutable.value.scene.replace(node)) } catch(e: IllegalArgumentException) { report("Invalid properties: ${e.message}") }
    }
    fun deleteSelected() { val s=mutable.value; edit(s.scene.copy(nodes=s.scene.nodes.filterNot { it.id==s.selected })) }
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
            history.clear(); change { EditorState(busy=true) }
            val listing=store.list(); change { it.copy(projects=listing.projects) }
            if(listing.errors.isNotEmpty()) report(listing.errors.joinToString("; "))
        }
    }
}

````

## editor-ui/src/main/kotlin/world/engine/editor/WorldEditorApp.kt

````kotlin
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
        Text("Projects · Foundation editor",style=MaterialTheme.typography.bodyMedium)
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
            TextButton(onClick={reset++}){Text("Reset view")}
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            val wide=maxWidth>=840.dp
            Column {
                Row(Modifier.weight(1f)) {
                    if(wide) Box(Modifier.width(200.dp).fillMaxHeight()) { Hierarchy(s,vm) }
                    AndroidView(factory={viewport},modifier=Modifier.weight(1f).fillMaxHeight(),update={ view ->
                        view.onSelect=vm::select; view.onMove=vm::move; view.onError=vm::report
                        view.onContext={id -> vm.select(id); if(id!=null) { panel="Inspector"; delete=true } }
                        view.isEnabled=!s.busy && s.recovery==null && s.recoveryIssue==null
                        view.update(s.scene,s.selected,project.directory)
                    })
                    if(wide) Box(Modifier.width(250.dp).fillMaxHeight()) { Inspector(s,vm){delete=true} }
                }
                if(wide) Console(s,Modifier.fillMaxWidth().height(96.dp))
                else {
                    Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("Hierarchy","Inspector","Console","Viewport").forEach { label -> TextButton(onClick={panel=label}){Text(if(panel==label) "[$label]" else label)} } }
                    if(panel!="Viewport") Box(Modifier.fillMaxWidth().heightIn(max=220.dp).height(200.dp)) { when(panel) { "Hierarchy" -> Hierarchy(s,vm); "Inspector" -> Inspector(s,vm){delete=true}; else -> Console(s,Modifier.fillMaxSize()) } }
                }
            }
        }
        Text("Drag: move / pan   •   Pinch: zoom   •   Hold: actions",Modifier.padding(6.dp),style=MaterialTheme.typography.labelSmall)
    }
    if(delete && s.selected!=null) AlertDialog(onDismissRequest={delete=false},title={Text("Delete selected sprite?")},text={Text("This edit can be undone. Imported image files are kept.")},confirmButton={TextButton(enabled=!s.busy,onClick={delete=false;vm.deleteSelected()}){Text("Delete")}},dismissButton={TextButton(onClick={delete=false}){Text("Cancel")}})
}

@Composable private fun Hierarchy(s: EditorState, vm: EditorViewModel) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Text("HIERARCHY · ${s.scene.nodes.size}",style=MaterialTheme.typography.labelLarge)
        OutlinedTextField(query,{query=it},label={Text("Find sprite")},singleLine=true,modifier=Modifier.fillMaxWidth())
        LazyColumn { items(s.scene.nodes.filter { it.name.contains(query,true) },key={it.id}) { node -> TextButton(enabled=!s.busy,onClick={vm.select(node.id)},modifier=Modifier.fillMaxWidth()){Text((if(node.id==s.selected) "● " else "○ ")+node.name)} } }
        if(s.scene.nodes.isEmpty()) Text("Use Add sprite or Import image.")
    }
}

@Composable private fun Inspector(s: EditorState, vm: EditorViewModel, delete: ()->Unit) {
    val node=s.scene.nodes.find { it.id==s.selected }
    if(node==null) { Text("Select a sprite to edit its properties.",Modifier.padding(12.dp)); return }
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
            Text(node.sprite?.asset ?: "Built-in solid sprite",style=MaterialTheme.typography.labelSmall)
            TextButton(enabled=!s.busy,onClick=delete){Text("Delete sprite")}
        }
    }
}
@Composable private fun Property(label: String,value: String,set: (String)->Unit) { OutlinedTextField(value,set,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth()) }
@Composable private fun Console(s: EditorState, modifier: Modifier) {
    Column(modifier.padding(8.dp)) { Text("CONSOLE",style=MaterialTheme.typography.labelLarge); LazyColumn { items(s.log.asReversed()) { Text(it,style=MaterialTheme.typography.bodySmall) } } }
}

````

## editor-viewport/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.editorviewport"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    api(project(":engine-render"))
}

````

## editor-viewport/src/main/AndroidManifest.xml

````xml
<manifest />

````

## editor-viewport/src/main/kotlin/world/engine/viewport/WorldViewport.kt

````kotlin
package world.engine.viewport

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
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
        contentDescription="Scene viewport. Drag sprites to move; drag empty space to pan; pinch to zoom; long press for actions."
    }
    fun update(scene: Scene, selected: String?, directory: File?) {
        if(this.directory!=directory) camera=Camera2D()
        this.scene=scene; this.selected=selected; this.directory=directory; publish()
    }
    fun resetCamera() { camera=Camera2D(); publish() }
    private fun publish() { renderer.frame=RenderFrame(scene,selected,camera,directory); requestRender() }
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
                dragging=node?.id; offset=point-(node?.transform?.position ?: point); onSelect(node?.id)
            }
            MotionEvent.ACTION_POINTER_DOWN -> { finish(); multiple=true }
            MotionEvent.ACTION_MOVE -> if(!multiple && !longPressed) {
                val id=dragging
                if(id!=null) {
                    val p=world(e.x,e.y)-offset
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

````

## engine-ai/README.md

````markdown
# engine-ai

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## engine-ai/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.engineai"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## engine-ai/src/main/AndroidManifest.xml

````xml
<manifest />

````

## engine-animation/README.md

````markdown
# engine-animation

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## engine-animation/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.engineanimation"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## engine-animation/src/main/AndroidManifest.xml

````xml
<manifest />

````

## engine-assets/README.md

````markdown
# engine-assets

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## engine-assets/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
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
}

````

## engine-assets/src/main/AndroidManifest.xml

````xml
<manifest />

````

## engine-audio/README.md

````markdown
# engine-audio

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## engine-audio/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.engineaudio"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## engine-audio/src/main/AndroidManifest.xml

````xml
<manifest />

````

## engine-build/README.md

````markdown
# engine-build

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## engine-build/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.enginebuild"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## engine-build/src/main/AndroidManifest.xml

````xml
<manifest />

````

## engine-core/build.gradle.kts

````kotlin
plugins { `java-library`; kotlin("jvm"); kotlin("plugin.serialization") }
kotlin { jvmToolchain(17) }
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    api(project(":engine-math"))
}
tasks.test { useJUnitPlatform() }

````

## engine-core/src/main/kotlin/world/engine/core/Scene.kt

````kotlin
package world.engine.core

import kotlinx.serialization.Serializable
import world.engine.math.*
import java.util.UUID

/** Components are serializable values. Systems consume immutable scene snapshots. */
@Serializable sealed class Component
@Serializable data class TransformComponent(val value: Transform = Transform()) : Component()
@Serializable data class SpriteComponent(val size: Vec2 = Vec2(96f,96f), val tint: Color = Color(), val asset: String? = null) : Component()
/** Stable entity identity. Ordered nodes determine painter order; Phase 1 is a flat scene. */
@Serializable data class Node(val id: String = UUID.randomUUID().toString(), val name: String = "Sprite", val components: List<Component> = listOf(TransformComponent(), SpriteComponent())) {
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
        nodes.forEach { n ->
            require(n.id.isNotBlank() && n.name.isNotBlank()) { "Empty entity identity or name" }
            require(n.components.count { it is TransformComponent } == 1)
            require(n.components.count { it is SpriteComponent } <= 1)
            val t=n.transform
            require(listOf(t.position.x,t.position.y,t.rotation,t.scale.x,t.scale.y).all { it.isFinite() })
            require(kotlin.math.abs(t.scale.x) >= .001f && kotlin.math.abs(t.scale.y) >= .001f)
            n.sprite?.let { s ->
                require(s.size.x.isFinite() && s.size.y.isFinite() && s.size.x>0 && s.size.y>0)
                require(listOf(s.tint.r,s.tint.g,s.tint.b,s.tint.a).all { it.isFinite() && it in 0f..1f })
                require(s.asset == null || Regex("Sprites/[a-f0-9-]+\\.png").matches(s.asset)) { "Unsafe asset path" }
            }
        }
        return this
    }
    fun replace(node: Node) = copy(nodes=nodes.map { if(it.id==node.id) node else it })
    inline fun <reified T: Component> query(): List<Pair<Node,T>> = nodes.flatMap { n -> n.components.filterIsInstance<T>().map { n to it } }
    fun pick(point: Vec2): Node? = nodes.asReversed().firstOrNull { n -> n.sprite?.let { Rect(Vec2(),it.size).contains(n.transform.inverseMap(point)) } ?: false }
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
    fun clear() { undo.clear(); redo.clear() }
}

````

## engine-core/src/test/kotlin/world/engine/core/SceneTest.kt

````kotlin
package world.engine.core

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import world.engine.math.*

class SceneTest {
    @Test fun jsonRoundTripPreservesIdentitiesAndAllProperties() {
        val node=Node(name="Player",components=listOf(TransformComponent(Transform(Vec2(12.5f,-8f),45f,Vec2(2f,.5f))),SpriteComponent(asset="Sprites/abcdef-123.png",tint=Color(.2f,.3f,.4f,.5f))))
        val scene=Scene(nodes=listOf(node))
        assertEquals(scene,Json.decodeFromString<Scene>(Json.encodeToString(scene)).validated())
    }
    @Test fun ambiguousAndUnsafeScenesFail() {
        val n=Node(); assertThrows(IllegalArgumentException::class.java) { Scene(nodes=listOf(n,n)).validated() }
        assertThrows(IllegalArgumentException::class.java) { Scene(nodes=listOf(n.copy(components=listOf(TransformComponent(),SpriteComponent(asset="../outside.png"))))).validated() }
        assertThrows(IllegalArgumentException::class.java) { Scene(version=2).validated() }
        assertThrows(IllegalArgumentException::class.java) { Scene(nodes=listOf(n.copy(components=emptyList()))).validated() }
    }
    @Test fun pickingUsesInverseTransformAndPainterOrder() {
        val a=Node(components=listOf(TransformComponent(Transform(Vec2(10f,20f),90f)),SpriteComponent(Vec2(100f,20f))))
        val b=a.copy(id="other")
        val scene=Scene(nodes=listOf(a,b)); assertEquals(b,scene.pick(Vec2(10f,60f))); assertNull(scene.pick(Vec2(60f,20f)))
    }
    @Test fun undoRedoBranchAndDragCoalescing() {
        val initial=Scene(nodes=listOf(Node())); val moved=initial.replace(initial.nodes[0].moved(Vec2(4f,8f)))
        val stack=CommandStack(); stack.commit(initial,moved)
        assertEquals(initial,stack.undo(moved)); assertEquals(moved,stack.redo(initial))
        stack.undo(moved); stack.commit(initial,Scene()); assertFalse(stack.canRedo)
    }
    @Test fun historyBoundedAndNoOpsIgnored() {
        val stack=CommandStack(2); val a=Scene(name="a"); val b=Scene(name="b"); val c=Scene(name="c"); val d=Scene(name="d")
        stack.commit(a,a); assertFalse(stack.canUndo)
        stack.commit(a,b); stack.commit(b,c); stack.commit(c,d)
        assertEquals(c,stack.undo(d)); assertEquals(b,stack.undo(c)); assertFalse(stack.canUndo)
    }
    @Test fun typedComponentQuery() { val s=Scene(nodes=listOf(Node(),Node())); assertEquals(2,s.query<SpriteComponent>().size) }
}

````

## engine-io/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android"); kotlin("plugin.serialization") }
android {
    namespace = "world.engine.engineio"
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
    implementation("androidx.core:core-ktx:1.13.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

````

## engine-io/src/androidTest/kotlin/world/engine/io/ProjectStoreTest.kt

````kotlin
package world.engine.io

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import world.engine.core.*
import world.engine.math.Vec2

class ProjectStoreTest {
    @Test fun saveReopenRecoveryAndDiscard() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=ProjectStore(context)
        val scene=Scene(nodes=List(3) { Node(name="Sprite $it").moved(Vec2(it*33f,it*-17f)) })
        val project=store.create("Persistence test","com.example.persistence","Landscape","Empty",Scene())
        try {
            store.save(project,scene)
            assertEquals(scene,ProjectStore(context).open(project).saved)
            val changed=scene.replace(scene.nodes[0].moved(Vec2(999f,42f)))
            store.save(project,changed,true)
            val reopened=ProjectStore(context).open(project)
            assertEquals(scene,reopened.saved); assertEquals(changed,reopened.recovery)
            store.discardRecovery(project); assertNull(store.open(project).recovery)
            store.save(project,changed,true); store.save(project,changed)
            assertNull(store.open(project).recovery); assertEquals(changed,store.open(project).saved)
        } finally { project.directory.deleteRecursively() }
    }
    @Test fun corruptRecoveryDoesNotHideSavedScene() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext; val store=ProjectStore(context)
        val original=Scene(nodes=listOf(Node()))
        val project=store.create("Corrupt snapshot test","com.example.corrupt","Portrait","Empty",original)
        try {
            java.io.File(project.directory,".autosave/Main.json").writeText("invalid")
            val result=store.open(project)
            assertEquals(original,result.saved); assertNull(result.recovery); assertNotNull(result.recoveryIssue)
            store.discardRecovery(project); assertNull(store.open(project).recoveryIssue)
        } finally { project.directory.deleteRecursively() }
    }
    @Test fun atomicBackupIsRecovered() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext; val store=ProjectStore(context)
        val scene=Scene(nodes=listOf(Node()))
        val project=store.create("Atomic test","com.example.atomic","Portrait","Empty",scene)
        try {
            val file=java.io.File(project.directory,"Scenes/Main.json")
            file.copyTo(java.io.File(file.path+".bak")); file.writeText("truncated")
            assertEquals(scene,ProjectStore(context).open(project).saved)
        } finally { project.directory.deleteRecursively() }
    }
}

````

## engine-io/src/main/AndroidManifest.xml

````xml
<manifest />

````

## engine-io/src/main/kotlin/world/engine/io/ProjectStore.kt

````kotlin
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
        while(options.outWidth/options.inSampleSize>2048 || options.outHeight/options.inSampleSize>2048) options.inSampleSize*=2
        options.inJustDecodeBounds=false
        val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: error("Image decode failed")
        val path="Sprites/${UUID.randomUUID()}.png"
        val target=File(project.directory,path)
        try { target.outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }; path }
        catch(e: Exception) { target.delete(); throw e }
        finally { bitmap.recycle() }
    }
}

````

## engine-math/build.gradle.kts

````kotlin
plugins { `java-library`; kotlin("jvm"); kotlin("plugin.serialization") }
kotlin { jvmToolchain(17) }
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}
tasks.test { useJUnitPlatform() }

````

## engine-math/src/main/kotlin/world/engine/math/Math.kt

````kotlin
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

````

## engine-math/src/test/kotlin/world/engine/math/MathTest.kt

````kotlin
package world.engine.math

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MathTest {
    @Test fun vectorArithmetic() { assertEquals(Vec2(4f,6f),Vec2(1f,2f)+Vec2(3f,4f)); assertEquals(5f,Vec2(3f,4f).length()) }
    @Test fun affineRoundTrip() {
        val t=Transform(Vec2(7f,-3f),37f,Vec2(2f,.5f)); val p=Vec2(8f,9f)
        val result=t.inverseMap(t.matrix().map(p))
        assertEquals(p.x,result.x,.0001f); assertEquals(p.y,result.y,.0001f)
        assertEquals(t.matrix().map(p),(Mat3.Identity*t.matrix()).map(p))
    }
    @Test fun rectEdges() { val r=Rect(Vec2(),Vec2(10f,20f)); assertTrue(r.contains(Vec2(5f,-10f))); assertFalse(r.contains(Vec2(5.01f,0f))) }
}

````

## engine-particles/README.md

````markdown
# engine-particles

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## engine-particles/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.engineparticles"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## engine-particles/src/main/AndroidManifest.xml

````xml
<manifest />

````

## engine-physics/README.md

````markdown
# engine-physics

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## engine-physics/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.enginephysics"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## engine-physics/src/main/AndroidManifest.xml

````xml
<manifest />

````

## engine-render/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.enginerender"
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
}

````

## engine-render/src/main/AndroidManifest.xml

````xml
<manifest />

````

## engine-render/src/main/kotlin/world/engine/render/Camera2D.kt

````kotlin
package world.engine.render

import world.engine.math.Vec2

/** Immutable view transform shared safely between the UI and GL threads. */
data class Camera2D(val center: Vec2 = Vec2(), val zoom: Float = 1f) {
    fun screenToWorld(x: Float, y: Float, width: Int, height: Int) = Vec2((x-width/2f)/zoom+center.x, (height/2f-y)/zoom+center.y)
    fun worldToClip(p: Vec2, width: Int, height: Int) = Vec2((p.x-center.x)*zoom*2/width.coerceAtLeast(1), (p.y-center.y)*zoom*2/height.coerceAtLeast(1))
}

````

## engine-render/src/main/kotlin/world/engine/render/SceneRenderer.kt

````kotlin
package world.engine.render

import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import world.engine.core.Scene
import world.engine.math.*
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.floor

data class RenderFrame(val scene: Scene=Scene(), val selected: String?=null, val camera: Camera2D=Camera2D(), val directory: File?=null)
/** The renderer only reads immutable snapshots; it never mutates editor entities. */
class SceneRenderer(private val report: (String)->Unit): GLSurfaceView.Renderer {
    @Volatile var frame=RenderFrame()
    private val batch=SpriteBatch()
    private val textures=TextureLoader(report)
    private var width=1; private var height=1
    private var root: File?=null
    private var ready=false
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
            if(root!=f.directory) { textures.clear(); root=f.directory }
            batch.begin(f.camera,width,height)
            val a=f.camera.screenToWorld(0f,height.toFloat(),width,height)
            val b=f.camera.screenToWorld(width.toFloat(),0f,width,height)
            val spacing=if(f.camera.zoom<.5f) 256f else 64f
            val grid=Color(.14f,.16f,.19f)
            var x=floor(a.x/spacing)*spacing
            while(x<=b.x) { batch.draw(textures.white,Transform(Vec2(x,(a.y+b.y)/2)),Vec2(1/f.camera.zoom,b.y-a.y),grid); x+=spacing }
            var y=floor(a.y/spacing)*spacing
            while(y<=b.y) { batch.draw(textures.white,Transform(Vec2((a.x+b.x)/2,y)),Vec2(b.x-a.x,1/f.camera.zoom),grid); y+=spacing }
            f.scene.nodes.forEach { n -> n.sprite?.let { sprite ->
                if(n.id==f.selected) batch.draw(textures.white,n.transform,sprite.size+Vec2(8/f.camera.zoom,8/f.camera.zoom),Color(1f,.75f,.15f))
                batch.draw(textures.get(root,sprite.asset),n.transform,sprite.size,sprite.tint)
            } }
            batch.flush()
        } catch(e: Exception) { ready=false; report("Rendering stopped: ${e.message}. Reopen the project to retry.") }
    }
}

````

## engine-render/src/main/kotlin/world/engine/render/SpriteBatch.kt

````kotlin
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
    fun draw(id: Int, transform: Transform, size: Vec2, color: Color) {
        if(texture!=id || data.remaining()<48) flush()
        texture=id
        val matrix=transform.matrix()
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

````

## engine-render/src/main/kotlin/world/engine/render/TextureLoader.kt

````kotlin
package world.engine.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30.*
import android.opengl.GLUtils
import java.io.File

/** Textures belong to one GL context. All calls must run on its render thread. */
class TextureLoader(private val report: (String) -> Unit) {
    private val cache=mutableMapOf<String,Int>()
    var white=0; private set
    private var missing=0
    private var bytesUsed=0L
    fun initialize() {
        cache.clear(); bytesUsed=0
        val solid=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888); solid.eraseColor(-1)
        white=upload(solid); solid.recycle()
        val check=Bitmap.createBitmap(intArrayOf(0xffff00ff.toInt(),0xff202020.toInt(),0xff202020.toInt(),0xffff00ff.toInt()),2,2,Bitmap.Config.ARGB_8888)
        missing=upload(check); check.recycle()
    }
    fun clear() { cache.values.filter { it!=missing }.distinct().forEach { glDeleteTextures(1,intArrayOf(it),0) }; cache.clear(); bytesUsed=0 }
    fun get(root: File?, path: String?): Int {
        if(path==null) return white
        return cache.getOrPut(path) {
            try {
                require(root!=null)
                val file=File(root,path)
                require(file.canonicalPath.startsWith(root.canonicalPath+File.separator))
                val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                BitmapFactory.decodeFile(file.path,bounds)
                require(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048) { "Missing or oversized texture: $path" }
                val bytes=bounds.outWidth.toLong()*bounds.outHeight*4
                require(bytesUsed+bytes<=64L*1024*1024) { "64 MiB texture budget exhausted; use smaller images" }
                val bitmap=BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inPremultiplied=false }) ?: error("Cannot decode $path")
                try { upload(bitmap).also { bytesUsed+=bytes } } finally { bitmap.recycle() }
            } catch(e: Exception) { report("Texture $path: ${e.message}. Showing checkerboard; re-import the image."); missing }
        }
    }
    private fun upload(bitmap: Bitmap): Int {
        val id=IntArray(1); glGenTextures(1,id,0); glBindTexture(GL_TEXTURE_2D,id[0])
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GL_TEXTURE_2D,0,bitmap,0)
        return id[0]
    }
}

````

## engine-scripting/README.md

````markdown
# engine-scripting

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## engine-scripting/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.enginescripting"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## engine-scripting/src/main/AndroidManifest.xml

````xml
<manifest />

````

## engine-tilemap/README.md

````markdown
# engine-tilemap

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## engine-tilemap/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.enginetilemap"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## engine-tilemap/src/main/AndroidManifest.xml

````xml
<manifest />

````

## engine-ui/README.md

````markdown
# engine-ui

Reserved module boundary for later phases. No implementation or exposed editor controls in Phase 1.

````

## engine-ui/build.gradle.kts

````kotlin
plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.engineui"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
}

````

## engine-ui/src/main/AndroidManifest.xml

````xml
<manifest />

````

## gradle/wrapper/gradle-wrapper.jar

Binary file; SHA-256: `cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8`. Decode the following base64 to recover the file.

````base64
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
WxoeYDmReKhhDvNJtMLAqK8qQ3qbJoKhkPy2V13PFOoNN12BdTIoS3M5hWLiHEXFxLcBwqgmjfZV
Oi0iRQso1FqtCxRWz1Gsn2P2CzCg0HujxD2kKQKatTMUS+U+7nzAwjcstWpvS+UL6GeY7eNRH5VP
KI/g2p/wZ+Iy1GnP0He4UoNyUr8AUEsHCBbX6RwNAgAAQwMAAFBLAwQUAAgICAAAACEAAAAAAAAA
AAAAAAAAMgAJAG9yZy9ncmFkbGUvY2xpL0NvbW1hbmRMaW5lUGFyc2VyJFBhcnNlclN0YXRlLmNs
YXNzVVQFAAEAAAAAhVHLSgMxFL2xtdVqtb5XLhxctNJx8LGoD1woCkJRseLCXWbmdhrNJCUzLYjo
h/gXrgQXfoAfJd7U+oKCA5Nzc8/Jucm9b+8vrwCwDgsMHh8ezmt3js+DG1Shs+0ETafqBDpuC8lT
oZUb6xApb1AiT5DIFk/coIXBTdKJE2e7yWWCVacduTFvu8J6oL+1GfobpDW1r/PNjpSUSFrcXbMS
FQmFaISKKNtFk1AtytdWN1Zrbohd534EGINCQ3dMgEdCIoOqNpEXGR5K9AIpvAMdx1yFdXI64yZB
s/wJjZSnmIcsg9I173JPchV5p/41Bmkecgxyu0KJdI9Bply5HIcRGC1AHgoMsuXjymUBhm1civmt
j2Rl0tO27QSDuXL9x6+R2svvVK4YFLX6o7saoBtwsv7vcz4Nfz1qh8GYVidafZXaH3Sl/43/Wpa0
+iU5VCF14oDGxmDSJk46sY/mgvsSs0vUnDzYLwfMdo7WOdpNETLC4ZVnGHuyfMnS4316kXCoTxct
zWC+70ExDWoCJqE3MHKyOA0zPdXsd4Vib09/z53CDK1DkPkAUEsHCJDJyYmnAQAAzgIAAFBLAwQU
AAgICAAAACEAAAAAAAAAAAAAAAAAPwAJAG9yZy9ncmFkbGUvY2xpL0NvbW1hbmRMaW5lUGFyc2Vy
JFVua25vd25PcHRpb25QYXJzZXJTdGF0ZS5jbGFzc1VUBQABAAAAAJVT7U4TURA9l7YWygqWbxUU
V9S2dLt8mFjAmCCJ0diAEcVATMzt7mVZ2I/m7hY1Rh7EZ/CHJgUTf/gAPpRxbinSIEnDn517Z+ac
OTOz9/efn78AzGGG4cvBwcvyJ73KrT0R2Pqibm3rRd0K/Zrr8dgNA8MPbUF+KTzBI0HBHR4Z1o6w
9qK6H+mL29yLRFGvOYbPa4arOER14b5dnadcWT7Bb9c9jxzRDjdmVUrguIEQ0g0c8u4LGVEt8pdL
86WyYYt9/XM3GENmPaxLSzxxPcGwEErHdCS3PWFanmuuhL7PA7tCTC+4jISceh3sBeH7YK2mpB/7
1mMeizSSDHMd4efgLjGkInVkKFU6ErRBlxgSXDoMA5Vdvs9NjweOuR6rjinUa51iGfSzzE0eu42f
IJceuoEbP2IQuf8ZOxNcTHx+gyGZe5bf0NCHKxmkkdWQQW8PUhjUoOGyOg1r6EaPOo0y9Dkifsqj
ZenUfRHE1H4uv0XuMCBKGa+KD/GymsdMLn/RQWbCgFJqnoiFhgmMZ6jijab7tNrjc6Zy4UKTncaY
hk4tUSuSn5SOGIaPS9dj1zOXpeQfK24UL2mYwp0e3MZdhsFzEtLIqX/EtomgXfxadVdY8VJ+S0MB
0xnkUaRlrNA7YuhXIlbrflXIV7zqCczSKNL0mhPIql3QKav21LS0JbIpkGL007dEt0m6J8kOFTbf
Jn5gYPoQQ8VDjBiHGPsONHFXca2V3UeWke1Kfm3FrmO8Fcu2YqnCEW5+a4Uncast3HU2PPEP/YAU
K/RoYXOzgZHnDVLUwL13RzDeNDCmAAxmU0KC2qHOiW2wCUooQUj8BVBLBwhLz5agcwIAAMcEAABQ
SwMEFAAICAgAAAAhAAAAAAAAAAAAAAAAACYACQBvcmcvZ3JhZGxlL2NsaS9Db21tYW5kTGluZVBh
cnNlci5jbGFzc1VUBQABAAAAAI1VW3cTVRT+DglMGqLQcDNY6BjBpmnTCEXphYshFKltk9IUamix
nsycJkMnM2Fm0osIDyyfXYtH+ugLryrYoizRZ1988Cf4P8R9Jr3ZC8s8ZM7s85199v723t/88c8v
rwCcRZ1h6eHDsZ778RLXZoWlx/vi2ky8M67Z1Zphcs+wrVTV1gXZHWEK7grarHA3pVWENuvWq268
b4abruiM18qpKq+lDOlDlHrP6aVuwjo9a+dn6qZJBrfCU2ckxCoblhCOYZXJOiccl+4ie09Xd1dP
Shdz8QchMIZwwa47mrhmmIJBtZ1yuuxw3RRpzTTSWbta5ZY+TJ5GueMKR0GQ4eBdPsfTJrfK6Xzp
rtA8BfsYDuVHxwfzuelcZmRgejQzPj4wlmOIDfvgumeYaUeUxUJ6lHuecKx+OnGKu+RTkuBeNVxe
MoXOwG4zHLBrvvXKYsGTGRB2k5/r3K2M8Jr0wE3Tnr9pzVr2vJVvnGHYd8GwDO8SQyDRfiuCAzgY
hoJmhuZtPhQcCuMwmiOI4K0m7MVRhtAFSr3h4MBGplmTglUQYziqC9dwhJ5ZC77gca/u+tfdjuBd
tIRxHCciCGO/dNnK0JKYuvz1VO1+xrTq1QeT66vU9J1kewjvMRzbhSYF7zMojXahAqUSwxshNbjp
b9+V4ghO44MwTqEtghCaZDDtRE+DXIbzicmdvO3eAw2Giff9mm153LDcIbHIcGRzUI2O6JdMpNAl
yU1TTVMhnPlP4zRuU9BNHeh63PHcCcOrbPG1FhL5+ggfh3EO54mMKvdoOhyG7s3YbIU7BXGvLixN
7EDJSOMQUdKLPklJ/w6cr4IUXFy/xo3gsizoJXzCEN+4btA0RZmbGadcrwrLG1jQhE+OgisMk1lu
Wbancl1XG2SrbafdNpW7KrfWLJpcWuaiusqlys1ahVNX0MxqqkbpcI2q6NJMqm2pNv8x3dYVwlUq
4YztUHwMvTvQNblDNbajIriGTyWl13ch3Z+cz8LIYoih739mJDF+OdV5KqeMmwIeYWjNbzpk0CHT
EVxfVHUxQ32lEyj/RvXJr5J7Y62J/KplHIcv0lAWiBHuDhsuMXI6sXv+/iEJo+xv4lYY45ggEUls
3W3kXgxjDCRGir0mLFtFqCCkpyncaSLkF9v0hbYVfEmCYlAduWdTyx5NbA5lcNVOTkrQwuAg/Ytu
31cwQ2HQZyEnFrwIKmjZjzIMhqBFBobDifbtOUcwC1PiqqRMtTrBenaY0zf3yrorGzU5yvfoyix9
bkgZZVVy9WpJOONSuHGGxEWhj14QMak1wMGYFECyNEttpSfD2/57gFakyfTv0ttJ/x2IJosriL7E
4eLQCo4kf8KxHyB/Ibyzjm3FHh97KLp3GSeDj55BjcZfIPEMyQb4MTrQuQr+iwLaS8+VjlcXA5dO
tHyHr5IdJ872BZ/jWCy4jA+XcCMWjJ5dRs8S0j8iKY0XlpF5gqZvAuzp6z9fIlsM/gqlOBSIBQvR
geQLDK5g+Lct9twu9tEN+1ixONLxAp+vYPI5ppchhjt+xl2GJziepBVp8e84l6PAUp3LcCaevv67
83ufMY/+w5T1t7R+7GcfIMseBP4FUEsHCB2MmemvBAAAYwgAAFBLAwQUAAgICAAAACEAAAAAAAAA
AAAAAAAAJgAJAG9yZy9ncmFkbGUvY2xpL1BhcnNlZENvbW1hbmRMaW5lLmNsYXNzVVQFAAEAAAAA
jVXbdhNVGP52k3bS6VhooFBAJERK2xwaewDTE9jWItCkRaLUQD1MZnbSaSczcWbSBcslywfwBeQF
uMW1agNmqVx54fIFvPRFrP/OARKTpfYi/98/33/a37d3fvvrx58BTMNkePL48d3kV+Gcqu1xSw/P
h7V8OBbW7GLJMFXPsK140dY5xR1uctXl9OWO6sa1Ha7tueWiG57Pq6bLY+FSIV5US3FD1OC5uVk9
N0NYJ9nMz5dNkwLujhqfEhCrYFicO4ZVoOg+d1zqRfHk5MxkMq7z/fDXATAGOWOXHY3fMEzOELKd
QqLgqLrJE5ppJO6ojsv1VbtYVC09RfUk+BmO76r7asJUrUJiM7fLNU9CH8MxuyTWcVceZTzRleFE
qgYse4aZuKm6O2m1tMAwWHK4yy1vsw7vhGW4J2AOL9r7XH8FG+QPPUdddgrlImVTYLglb9lx1Ecp
wxWZfYuGZXjXGE6Nd6k8cY/BNz5xT8ExDMmQEGQY6phTwkkZwwgqCKC/H7043YGiYhLOyDgrUDIG
BOpNBUrde4v26jKehJCMiyLjDQwK3NsMAcPjjurZjph4omXkW434goJRXBadxhiCnd9LmGCQSDUb
dES17e4riCI2gAjiDH6rFj7ZrN1CHFVO4B2Bm+okv4X2OgkSZhgu/5dEmtgrMq6Kw5UL/DXXw20L
NjlRkMScjFnMt4mrriMJi7RTqUwrJMc7N+iMdF3zGq4LQt9jUL4s2x5ftvTbtmExTLeKZDnnksY0
b9U2Tcqjmduq1QciiZ3+Z2ylbJg6Jybel7Emtg6+RtRoypl0dz4YwE3BYU8sFMBtUqpaKtGjwBAf
7+zS2bjRhLZJIS36bDCwsQDukIY8u3nr2nluFFNwFxmR8pGCFazKpDy6BzONKzsfGnVjofb7VY+1
X0IRC+ATGjxvO0WVGJnrMviDf6fk1UT38UDGErapXH0OhsWu5/D/FEe0+EhqpNAuKumqiS+gCk3k
GMItbBHzBdVsnsPaQ403BE08jdRbhcZG3bGQZXshnedpAH0ygLwQd5fpaw/NjgwOg67iKr3VmKLT
l+j3wY8h8byQNyQekJpVGpaehxqC3lUcp889+u9byuol+000ks1uV3CiiuFsqoJT0R8wUsVZ4Z8j
/3yLf6GKi8IPk3/pEOOp6AtMMnyHJXKmGV5itoqr2XQF7x5igQAb8Trg6I9IvIFYmvcfYOSMP3aI
5a2nR39+D/HXL4TUmCxLk/rIpiNVrGXXK7jhX3yBWwzpWKPd1LlYs1rqCeRIcP0Qm1u0R/BD4UTF
R931LT49+j1yiI+f1doMCeU22izQ+n6yCco7wPnn2Fo/wCUyqQNcIJPu+wlSdnvDF8n4o5neWCaY
jT/Hp81Cn+HzRqErVKiH7ESEFqPe2ks6g/Vf0Rt5VgXP+kWZdV80EyxEKL+C3V9qJVhtyR74/gZQ
SwcI6zJ3jToEAADhBwAAUEsDBBQACAgIAAAAIQAAAAAAAAAAAAAAAAAsAAkAb3JnL2dyYWRsZS9j
bGkvUGFyc2VkQ29tbWFuZExpbmVPcHRpb24uY2xhc3NVVAUAAQAAAABtkM1Kw0AUhc8YbdoYbW11
6yILUTGGqov4gyAFNxYUBcHlNLlNx06SMpMURPRBfAsXIij4AD6UOCm4c3O595xvzp2Z75+PLwB7
WGV4eX6+Dh+9AY/GlMXekRcNvR0vytOJkLwQeeaneUxGVySJazLmiGs/GlE01mWqvaMhl5p2vEni
p3ziiyqDBocH8WDfsCr8Oz8spTSCHnG/WyFZIjIiJbLEqFNS2uwyeri7vxv6MU29pzoYg3OTlyqi
cyGJYSNXSZAoHksKIimCK640xb08TXkW903e5aS6so15htY9n/JA8iwJLgf3FBU2agy1KZclaYa1
/swvCyGDM6X4Q1/o4tgAJyITxSmDtbl168LBogMbLkPnH97GsoMmXBd1NBpYwArDfM+8F10z2OaP
GVYqb9axKs3UjpnWYZkOaG/fvWPpE827i3e0tt/QfgVmtGXqHKxfUEsHCFrddm1UAQAArAEAAFBL
AwQUAAgICAAAACEAAAAAAAAAAAAAAAAAMwAJAG9yZy9ncmFkbGUvaW50ZXJuYWwvZmlsZS9QYXRo
VHJhdmVyc2FsQ2hlY2tlci5jbGFzc1VUBQABAAAAAHVTa3PTRhQ9G5tIMW4BAaEtpSjikUTEEhBo
nUehYMLTw8s8hseXtbyWBXqY3XXSTKf5H/UPaL92+sFlygDf+VEMV2KYAA2a0evce889d/fsm7f/
vQJwEk2G4cbG7fpvTpsHT0XacRadoOvMOUGW9KOY6yhLa0nWEYRLEQuuBAV7XNWCngieqkGinMUu
j5WYc/phLeH9WpRziPbCqU57nnJl/UN9dxDHBKger53IU9IwSoWQURoSuiqkol6E1715r17riFXn
dxOModLKBjIQF6NYMNQyGfqh5J1Y+FGqhUx57Hcp5N/kundH8pyHx41cnJAGygw7n/BV7sc8Df0b
7Sci0AbGGaqKd0Vec50nxHt0prmZ1tK5qKXZ/0OfsL3HDFQYjEitJH29zlCamX1YRRVfVbAdXzMw
38ROGkJpLrW6H+kew96tmlGVhd151R6qemxikmHM80x8w2AGWap5lCqG/R/XNnpctsSzgUgDUTB8
h/05w/ek43Fe+wPV0qYWfauw3/NPUdTzqMMh+vDztCMFQkqnqfWiiVmaKFNeSktj4tinQ68rLRID
NYbtodA3ZdYXUq9X4WOiAg/HP2QPdBT7zSzgsTBwkma522Kwmp/Hlqo4hdMTmMePxKizZrYmZINs
trknH2dvsSdV1LGQz7VIqteitJOtKRPLDM5m6pU4FiGPz8lwkIhUr/waiH5ubQNnGLzpI2rajpSd
Ztrmdm4Mm8ugF60Km5Llup1Ju09WsfMFoeX6hWG8m8mEa4aFLfbyUfNzy22t+zwaue4LRLccpZE+
8wVr3KviIi5VcBaXGcoNOk0MO5p0eK4PkraQd3g7FuUpbIOB/GKYgEk3w1X6+5PwMXpvuCPsGCJw
rV0j7B3ioWvtKz5uuda3IxwYYvwvTLvWwRGcIZZd63ABzrvW0QJxXWumQKZci6gO/IFJa+45TvyD
n0ZYsn4uYtvcv1/g7IPySxgPmiW3ZZ079hwr/+LK60LXNXpOkh5yGVl1jPSVcBtlhAVWougYSu8A
UEsHCPkkTxT/AgAAnAQAAFBLAwQUAAgICAAAACEAAAAAAAAAAAAAAAAAQQAJAG9yZy9ncmFkbGUv
aW50ZXJuYWwvZmlsZS9sb2NraW5nL0V4Y2x1c2l2ZUZpbGVBY2Nlc3NNYW5hZ2VyLmNsYXNzVVQF
AAEAAAAAZVBNT9tAEH1bEkxCUkhp+QHuBSKMxcchBVSpQuVEVbVI9LxeT5wl63W0a0egqvyQ/oGe
OaFy4MiBH1V1bIF66B5mNO+9eTM7j3/u7gHsYl3g5/X119H3MJFqSjYND0I1DrdCVeQzbWSpCxvl
RUqMOzIkPTE5kT5SE1JTX+U+PBhL42krnGVRLmeRrj0oebefJnusdaPn/nFlDAN+IqOdWmIzbYmc
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
JM6Q8RddL0ucIeOvut6QOEPG33K9J3GGjB90uyXOkHCqhk1UGTejHDy7nEPVtR1ubEQevdY99ETM
x7VUYW+GirupYn4EjWgKxWp4RTcmiCgmuhowyXUOilzrUOzagBLXRSh1OZjsPg1l7rMxxb0GU93r
Mc19IXzuTVKOW1Zx9/8AUEsHCCKYkR7DCwAAuhUAAFBLAwQUAAgICAAAACEAAAAAAAAAAAAAAAAA
LQAJAG9yZy9ncmFkbGUvd3JhcHBlci9JbnN0YWxsJEluc3RhbGxDaGVjay5jbGFzc1VUBQABAAAA
AGWRy0oDMRSG/1i1WsdqvW3cjYJWOw5eFvWCG0EUFEFBcJnOnE6jmQvJtC5EH8S3cCGCCx/AhxLP
VEVEDuSc8+c7f0Ly/vH6BmADcwKPDw/nzTu3JYMbSkJ3xw3absMN0jhTWuYqTbw4DYl1Q5qkJd7s
SOsFHQpubDe27k5baksNN4u8WGaeKjyotb0VtjaZNc2f+XZXaxZsR3rrBZJEKiEyKolY7ZGxfBbr
zbXNtaYXUs+9H4EQqFykXRPQodIksJSayI+MDDX5t0ZmGRn/OLG51HrxOx8UFytjUGDyWvakr2US
+WetawryMobZ72v8KI3Zr3rSZ1TqF/67LLSl0l1Dp2StjJiYOvl1uciL2zI1vKcSle8LLCz/NfgP
1y8FSsv1SwcOqhWUMeFgBKOjGELNQQVjRTUtMHjAr4R1bsr8MwOoFRRXtYLhLDgcjPM6y908ShzA
xMrV1QsmV58x1XjGzBPQR0t9i9InUEsHCKoEk0pqAQAA5wEAAFBLAwQUAAgICAAAACEAAAAAAAAA
AAAAAAAAIAAJAG9yZy9ncmFkbGUvd3JhcHBlci9JbnN0YWxsLmNsYXNzVVQFAAEAAAAApVgLfFPn
dT8HSZZ8EQ/bGCIeiWJwkCXL5hEwmITUNiQxFo9iCFUgIdfStX1Bute99wpwstBuI9vabuvapg9o
G7Jsi7Mt7coGslNa2KvJlnXtunXtnt3WZN3Wvbru/Ujp/3ySjGzLpNv48dO53/ed73znO+d/Hp9f
/c6nrxHRJs4xXTh79uC2J1qG9MxJw8q2dLdkhlvaWzJ2fszM6Z5pW8m8nTUw7xg5Q3cNLI7qbjIz
amROuoW829I9rOdco71lbCSZ18eSpsgwhrbfnR3aDF5nW2X/cCGXw4Q7qic3Cos1YlqG4ZjWCGZP
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
AAAATUVUQS1JTkYvTElDRU5TRVVUBQABAAAAAFBLAQIUABQACAgIAAAAIQBtsT49QAAAAD8AAAAU
AAkAAAAAAAAAAAAAADAOAABNRVRBLUlORi9NQU5JRkVTVC5NRlVUBQABAAAAAFBLAQIUABQACAgI
AAAAIQCTYHpYIQEAAHABAAAxAAkAAAAAAAAAAAAAALsOAABvcmcvZ3JhZGxlL2NsaS9Db21tYW5k
TGluZUFyZ3VtZW50RXhjZXB0aW9uLmNsYXNzVVQFAAEAAAAAUEsBAhQAFAAICAgAAAAhAMOXEplu
AgAAswMAACYACQAAAAAAAAAAAAAARBAAAG9yZy9ncmFkbGUvY2xpL0NvbW1hbmRMaW5lT3B0aW9u
LmNsYXNzVVQFAAEAAAAAUEsBAhQAFAAICAgAAAAhAGSivSBaAgAAtgQAADMACQAAAAAAAAAAAAAA
DxMAAG9yZy9ncmFkbGUvY2xpL0NvbW1hbmRMaW5lUGFyc2VyJEFmdGVyT3B0aW9ucy5jbGFzc1VU
BQABAAAAAFBLAQIUABQACAgIAAAAIQCL4zEXLAMAAF0HAAA8AAkAAAAAAAAAAAAAANMVAABvcmcv
Z3JhZGxlL2NsaS9Db21tYW5kTGluZVBhcnNlciRCZWZvcmVGaXJzdFN1YkNvbW1hbmQuY2xhc3NV
VAUAAQAAAABQSwECFAAUAAgICAAAACEA6doPs98GAABiDgAAPQAJAAAAAAAAAAAAAAByGQAAb3Jn
L2dyYWRsZS9jbGkvQ29tbWFuZExpbmVQYXJzZXIkS25vd25PcHRpb25QYXJzZXJTdGF0ZS5jbGFz
c1VUBQABAAAAAFBLAQIUABQACAgIAAAAIQBDJ3yiTAIAAJcEAAA8AAkAAAAAAAAAAAAAAMUgAABv
cmcvZ3JhZGxlL2NsaS9Db21tYW5kTGluZVBhcnNlciRNaXNzaW5nT3B0aW9uQXJnU3RhdGUuY2xh
c3NVVAUAAQAAAABQSwECFAAUAAgICAAAACEAtJRbo9cCAABKBQAAPQAJAAAAAAAAAAAAAACEIwAA
b3JnL2dyYWRsZS9jbGkvQ29tbWFuZExpbmVQYXJzZXIkT3B0aW9uQXdhcmVQYXJzZXJTdGF0ZS5j
bGFzc1VUBQABAAAAAFBLAQIUABQACAgIAAAAIQB1W3o/ogEAAH0CAAA4AAkAAAAAAAAAAAAAAM8m
AABvcmcvZ3JhZGxlL2NsaS9Db21tYW5kTGluZVBhcnNlciRPcHRpb25QYXJzZXJTdGF0ZS5jbGFz
c1VUBQABAAAAAFBLAQIUABQACAgIAAAAIQAW1+kcDQIAAEMDAAAzAAkAAAAAAAAAAAAAAOAoAABv
cmcvZ3JhZGxlL2NsaS9Db21tYW5kTGluZVBhcnNlciRPcHRpb25TdHJpbmcuY2xhc3NVVAUAAQAA
AABQSwECFAAUAAgICAAAACEAkMnJiacBAADOAgAAMgAJAAAAAAAAAAAAAABXKwAAb3JnL2dyYWRs
ZS9jbGkvQ29tbWFuZExpbmVQYXJzZXIkUGFyc2VyU3RhdGUuY2xhc3NVVAUAAQAAAABQSwECFAAU
AAgICAAAACEAS8+WoHMCAADHBAAAPwAJAAAAAAAAAAAAAABnLQAAb3JnL2dyYWRsZS9jbGkvQ29t
bWFuZExpbmVQYXJzZXIkVW5rbm93bk9wdGlvblBhcnNlclN0YXRlLmNsYXNzVVQFAAEAAAAAUEsB
AhQAFAAICAgAAAAhAB2MmemvBAAAYwgAACYACQAAAAAAAAAAAAAAUDAAAG9yZy9ncmFkbGUvY2xp
L0NvbW1hbmRMaW5lUGFyc2VyLmNsYXNzVVQFAAEAAAAAUEsBAhQAFAAICAgAAAAhAOsyd406BAAA
4QcAACYACQAAAAAAAAAAAAAAXDUAAG9yZy9ncmFkbGUvY2xpL1BhcnNlZENvbW1hbmRMaW5lLmNs
YXNzVVQFAAEAAAAAUEsBAhQAFAAICAgAAAAhAFrddm1UAQAArAEAACwACQAAAAAAAAAAAAAA8zkA
AG9yZy9ncmFkbGUvY2xpL1BhcnNlZENvbW1hbmRMaW5lT3B0aW9uLmNsYXNzVVQFAAEAAAAAUEsB
AhQAFAAICAgAAAAhAPkkTxT/AgAAnAQAADMACQAAAAAAAAAAAAAAqjsAAG9yZy9ncmFkbGUvaW50
ZXJuYWwvZmlsZS9QYXRoVHJhdmVyc2FsQ2hlY2tlci5jbGFzc1VUBQABAAAAAFBLAQIUABQACAgI
AAAAIQB5tXfKhwEAAAMCAABBAAkAAAAAAAAAAAAAABM/AABvcmcvZ3JhZGxlL2ludGVybmFsL2Zp
bGUvbG9ja2luZy9FeGNsdXNpdmVGaWxlQWNjZXNzTWFuYWdlci5jbGFzc1VUBQABAAAAAFBLAQIU
ABQACAgIAAAAIQBipwaKwQEAAKMCAAA+AAkAAAAAAAAAAAAAABJBAABvcmcvZ3JhZGxlL3V0aWwv
aW50ZXJuYWwvV3JhcHBlckRpc3RyaWJ1dGlvblVybENvbnZlcnRlci5jbGFzc1VUBQABAAAAAFBL
AQIUABQACAgIAAAAIQCcRdKajgEAAB4CAAAvAAkAAAAAAAAAAAAAAEhDAABvcmcvZ3JhZGxlL3dy
YXBwZXIvQm9vdHN0cmFwTWFpblN0YXJ0ZXIkMS5jbGFzc1VUBQABAAAAAFBLAQIUABQACAgIAAAA
IQCA0yUGKQMAAOQEAABBAAkAAAAAAAAAAAAAADxFAABvcmcvZ3JhZGxlL3dyYXBwZXIvRG93bmxv
YWQkRGVmYXVsdERvd25sb2FkUHJvZ3Jlc3NMaXN0ZW5lci5jbGFzc1VUBQABAAAAAFBLAQIUABQA
CAgIAAAAIQB3o7Ym5QIAABEFAAA0AAkAAAAAAAAAAAAAAN1IAABvcmcvZ3JhZGxlL3dyYXBwZXIv
RG93bmxvYWQkUHJveHlBdXRoZW50aWNhdG9yLmNsYXNzVVQFAAEAAAAAUEsBAhQAFAAICAgAAAAh
ABgEsAxlCQAAKhIAACEACQAAAAAAAAAAAAAALUwAAG9yZy9ncmFkbGUvd3JhcHBlci9Eb3dubG9h
ZC5jbGFzc1VUBQABAAAAAFBLAQIUABQACAgIAAAAIQBBcxcJ2QEAALICAAAtAAkAAAAAAAAAAAAA
AOpVAABvcmcvZ3JhZGxlL3dyYXBwZXIvR3JhZGxlVXNlckhvbWVMb29rdXAuY2xhc3NVVAUAAQAA
AABQSwECFAAUAAgICAAAACEA3RS7hw0VAACpKQAAKgAJAAAAAAAAAAAAAAAnWAAAb3JnL2dyYWRs
ZS93cmFwcGVyL0dyYWRsZVdyYXBwZXJNYWluLmNsYXNzVVQFAAEAAAAAUEsBAhQAFAAICAgAAAAh
ACKYkR7DCwAAuhUAACIACQAAAAAAAAAAAAAAlW0AAG9yZy9ncmFkbGUvd3JhcHBlci9JbnN0YWxs
JDEuY2xhc3NVVAUAAQAAAABQSwECFAAUAAgICAAAACEAqgSTSmoBAADnAQAALQAJAAAAAAAAAAAA
AACxeQAAb3JnL2dyYWRsZS93cmFwcGVyL0luc3RhbGwkSW5zdGFsbENoZWNrLmNsYXNzVVQFAAEA
AAAAUEsBAhQAFAAICAgAAAAhAD8+qWeoDgAA2hsAACAACQAAAAAAAAAAAAAAf3sAAG9yZy9ncmFk
bGUvd3JhcHBlci9JbnN0YWxsLmNsYXNzVVQFAAEAAAAAUEsBAhQAFAAICAgAAAAhAF32tW87AgAA
HgQAAB8ACQAAAAAAAAAAAAAAfooAAG9yZy9ncmFkbGUvd3JhcHBlci9Mb2dnZXIuY2xhc3NVVAUA
AQAAAABQSwECFAAUAAgICAAAACEA6imTPiQBAABqAQAAJgAJAAAAAAAAAAAAAAAPjQAAb3JnL2dy
YWRsZS93cmFwcGVyL1BhdGhBc3NlbWJsZXIuY2xhc3NVVAUAAQAAAABQSwECFAAUAAgICAAAACEA
1uMlrJoDAABOBgAAMAAJAAAAAAAAAAAAAACQjgAAb3JnL2dyYWRsZS93cmFwcGVyL1N5c3RlbVBy
b3BlcnRpZXNIYW5kbGVyLmNsYXNzVVQFAAEAAAAAUEsBAhQAFAAICAgAAAAhAOYRBMnuAgAAUAYA
AC0ACQAAAAAAAAAAAAAAkZIAAG9yZy9ncmFkbGUvd3JhcHBlci9XcmFwcGVyQ29uZmlndXJhdGlv
bi5jbGFzc1VUBQABAAAAAFBLAQIUABQACAgIAAAAIQARWWHoUwYAAMUMAAAoAAkAAAAAAAAAAAAA
AOOVAABvcmcvZ3JhZGxlL3dyYXBwZXIvV3JhcHBlckV4ZWN1dG9yLmNsYXNzVVQFAAEAAAAAUEsF
BgAAAAAhACEAEg0AAJWcAAAAAA==
````

## gradle/wrapper/gradle-wrapper.properties

````properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists

````

## gradle.properties

````properties
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official

````

## gradlew

````text
#!/bin/sh

#
# Copyright © 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
#
#   Gradle start up script for POSIX generated by Gradle.
#
#   Important for running:
#
#   (1) You need a POSIX-compliant shell to run this script. If your /bin/sh is
#       noncompliant, but you have some other compliant shell such as ksh or
#       bash, then to run this script, type that shell name before the whole
#       command line, like:
#
#           ksh Gradle
#
#       Busybox and similar reduced shells will NOT work, because this script
#       requires all of these POSIX shell features:
#         * functions;
#         * expansions «$var», «${var}», «${var:-default}», «${var+SET}»,
#           «${var#prefix}», «${var%suffix}», and «$( cmd )»;
#         * compound commands having a testable exit status, especially «case»;
#         * various built-in commands including «command», «set», and «ulimit».
#
#   Important for patching:
#
#   (2) This script targets any POSIX shell, so it avoids extensions provided
#       by Bash, Ksh, etc; in particular arrays are avoided.
#
#       The "traditional" practice of packing multiple parameters into a
#       space-separated string is a well documented source of bugs and security
#       problems, so this is (mostly) avoided, by progressively accumulating
#       options in "$@", and eventually passing that to Java.
#
#       Where the inherited environment variables (DEFAULT_JVM_OPTS, JAVA_OPTS,
#       and GRADLE_OPTS) rely on word-splitting, this is performed explicitly;
#       see the in-line comments for details.
#
#       There are tweaks for specific operating systems such as AIX, CygWin,
#       Darwin, MinGW, and NonStop.
#
#   (3) This script is generated from the Groovy template
#       https://github.com/gradle/gradle/blob/HEAD/subprojects/plugins/src/main/resources/org/gradle/api/internal/plugins/unixStartScript.txt
#       within the Gradle project.
#
#       You can find Gradle at https://github.com/gradle/gradle/.
#
##############################################################################

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
      /*)   app_path=$link ;; #(
      *)    app_path=$APP_HOME$link ;;
    esac
done

# This is normally unused
# shellcheck disable=SC2034
APP_BASE_NAME=${0##*/}
# Discard cd standard output in case $CDPATH is set (https://github.com/gradle/gradle/issues/25036)
APP_HOME=$( cd "${APP_HOME:-./}" > /dev/null && pwd -P ) || exit

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in                #(
  CYGWIN* )         cygwin=true  ;; #(
  Darwin* )         darwin=true  ;; #(
  MSYS* | MINGW* )  msys=true    ;; #(
  NONSTOP* )        nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar


# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
fi

# Increase the maximum file descriptors if we can.
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in #(
      max*)
        # In POSIX sh, ulimit -H is undefined. That's why the result is checked to see if it worked.
        # shellcheck disable=SC2039,SC3045
        MAX_FD=$( ulimit -H -n ) ||
            warn "Could not query maximum file descriptor limit"
    esac
    case $MAX_FD in  #(
      '' | soft) :;; #(
      *)
        # In POSIX sh, ulimit -n is undefined. That's why the result is checked to see if it worked.
        # shellcheck disable=SC2039,SC3045
        ulimit -n "$MAX_FD" ||
            warn "Could not set maximum file descriptor limit to $MAX_FD"
    esac
fi

# Collect all arguments for the java command, stacking in reverse order:
#   * args from the command line
#   * the main class name
#   * -classpath
#   * -D...appname settings
#   * --module-path (only if needed)
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, and GRADLE_OPTS environment variables.

# For Cygwin or MSYS, switch paths to Windows format before running java
if "$cygwin" || "$msys" ; then
    APP_HOME=$( cygpath --path --mixed "$APP_HOME" )
    CLASSPATH=$( cygpath --path --mixed "$CLASSPATH" )

    JAVACMD=$( cygpath --unix "$JAVACMD" )

    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    for arg do
        if
            case $arg in                                #(
              -*)   false ;;                            # don't mess with options #(
              /?*)  t=${arg#/} t=/${t%%/*}              # looks like a POSIX filepath
                    [ -e "$t" ] ;;                      #(
              *)    false ;;
            esac
        then
            arg=$( cygpath --path --ignore --mixed "$arg" )
        fi
        # Roll the args list around exactly as many times as the number of
        # args, so each arg winds up back in the position where it started, but
        # possibly modified.
        #
        # NB: a `for` loop captures its iteration list before it begins, so
        # changing the positional parameters here affects neither the number of
        # iterations, nor the values presented in `arg`.
        shift                   # remove old arg
        set -- "$@" "$arg"      # push replacement arg
    done
fi


# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='-Dfile.encoding=UTF-8 "-Xmx64m" "-Xms64m"'

# Collect all arguments for the java command:
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, JAVA_OPTS, and optsEnvironmentVar are not allowed to contain shell fragments,
#     and any embedded shellness will be escaped.
#   * For example: A user cannot expect ${Hostname} to be expanded, as it is an environment variable and will be
#     treated as '${Hostname}' itself on the command line.

set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$CLASSPATH" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"

# Stop when "xargs" is not available.
if ! command -v xargs >/dev/null 2>&1
then
    die "xargs is not available"
fi

# Use "xargs" to parse quoted args.
#
# With -n1 it outputs one arg per line, with the quotes and backslashes removed.
#
# In Bash we could simply go:
#
#   readarray ARGS < <( xargs -n1 <<<"$var" ) &&
#   set -- "${ARGS[@]}" "$@"
#
# but POSIX shell has neither arrays nor command substitution, so instead we
# post-process each arg (as a line of input to sed) to backslash-escape any
# character that might be a shell metacharacter, then use eval to reverse
# that process (while maintaining the separation between arguments), and wrap
# the whole thing up as a single "set" statement.
#
# This will of course break if any of these variables contains a newline or
# an unmatched quote.
#

eval "set -- $(
        printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
        xargs -n1 |
        sed ' s~[^-[:alnum:]+,./:=@_]~\\&~g; ' |
        tr '\n' ' '
    )" '"$@"'

exec "$JAVACMD" "$@"

````

## gradlew.bat

````bat
@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=-Dfile.encoding=UTF-8 "-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar


@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega

````

## settings.gradle.kts

````kotlin
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "2DWorldEngine"
include(":engine-math")
include(":engine-core")
include(":engine-render")
include(":engine-physics")
include(":engine-audio")
include(":engine-animation")
include(":engine-assets")
include(":engine-scripting")
include(":engine-ai")
include(":engine-tilemap")
include(":engine-particles")
include(":engine-ui")
include(":engine-io")
include(":engine-build")
include(":editor-ui")
include(":editor-viewport")
include(":editor-assets")
include(":editor-animation")
include(":editor-scripting")
include(":editor-build")
include(":app")

````

## tools/validate_structure.py

````python
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
assert len(list(root.glob('*/src/test/**/*.kt'))) == 2
assert len(list(root.glob('*/src/androidTest/**/*.kt'))) == 3
print(f'PASS: {len(modules)} modules, project references, XML, official wrapper integrity, permission policy, and test-source presence.')
print('Android compilation, JUnit execution, Compose tests and GLES device tests are NOT covered by this check.')

````
