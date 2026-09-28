# Phase 3 — complete new/changed source

Generated from the actual repository files. Includes implementation, tests, configuration, licensing and documents in full; it does not recursively embed this appendix itself. The unchanged source remains in its original repository paths.

- `.github/workflows/android.yml` (CHANGED)
- `.gitignore` (CHANGED)
- `README.md` (CHANGED)
- `app/build.gradle.kts` (CHANGED)
- `app/src/androidTest/kotlin/world/engine/app/GlRenderTest.kt` (CHANGED)
- `app/src/androidTest/kotlin/world/engine/app/PlayModeTest.kt` (NEW)
- `app/src/main/assets/licenses/JBOX2D.txt` (NEW)
- `docs/PHASE2_README.md` (NEW)
- `docs/PHASE3.md` (NEW)
- `docs/PHASE3_BASELINE.json` (NEW)
- `docs/PHASE3_FILES.md` (NEW)
- `docs/PHASE3_VERIFICATION.md` (NEW)
- `editor-animation/README.md` (CHANGED)
- `editor-animation/build.gradle.kts` (CHANGED)
- `editor-animation/src/main/kotlin/world/engine/animationeditor/TimelineEditor.kt` (NEW)
- `editor-ui/build.gradle.kts` (CHANGED)
- `editor-ui/src/main/kotlin/world/engine/editor/EditorViewModel.kt` (CHANGED)
- `editor-ui/src/main/kotlin/world/engine/editor/PlayControls.kt` (NEW)
- `editor-ui/src/main/kotlin/world/engine/editor/RuntimePanels.kt` (NEW)
- `editor-ui/src/main/kotlin/world/engine/editor/WorldEditorApp.kt` (CHANGED)
- `editor-viewport/build.gradle.kts` (CHANGED)
- `editor-viewport/src/main/kotlin/world/engine/viewport/PreviewSession.kt` (NEW)
- `editor-viewport/src/main/kotlin/world/engine/viewport/WorldViewport.kt` (CHANGED)
- `engine-animation/README.md` (CHANGED)
- `engine-animation/build.gradle.kts` (CHANGED)
- `engine-animation/src/main/kotlin/world/engine/animation/AnimationPlayer.kt` (NEW)
- `engine-animation/src/main/kotlin/world/engine/animation/AnimationPresets.kt` (NEW)
- `engine-animation/src/test/kotlin/world/engine/animation/AnimationPlayerTest.kt` (NEW)
- `engine-core/src/main/kotlin/world/engine/core/InputRouter.kt` (NEW)
- `engine-core/src/main/kotlin/world/engine/core/Prefab.kt` (CHANGED)
- `engine-core/src/main/kotlin/world/engine/core/RuntimeData.kt` (NEW)
- `engine-core/src/main/kotlin/world/engine/core/Scene.kt` (CHANGED)
- `engine-core/src/test/kotlin/world/engine/core/RuntimeDataTest.kt` (NEW)
- `engine-physics/README.md` (CHANGED)
- `engine-physics/build.gradle.kts` (CHANGED)
- `engine-physics/src/androidTest/kotlin/world/engine/physics/AlphaColliderTest.kt` (NEW)
- `engine-physics/src/main/kotlin/world/engine/physics/ColliderGeometry.kt` (NEW)
- `engine-physics/src/main/kotlin/world/engine/physics/PhysicsWorld.kt` (NEW)
- `engine-physics/src/test/kotlin/world/engine/physics/PhysicsWorldTest.kt` (NEW)
- `engine-render/build.gradle.kts` (CHANGED)
- `engine-render/src/main/kotlin/world/engine/render/Camera2D.kt` (CHANGED)
- `engine-render/src/main/kotlin/world/engine/render/CameraRig.kt` (NEW)
- `engine-render/src/main/kotlin/world/engine/render/SceneRenderer.kt` (CHANGED)
- `engine-render/src/main/kotlin/world/engine/render/SpriteBatch.kt` (CHANGED)
- `engine-render/src/test/kotlin/world/engine/render/CameraRigTest.kt` (NEW)
- `tools/RuntimeSmoke.kt` (NEW)
- `tools/annotate_ci_failure.py` (NEW)
- `tools/check_engine_standalone.py` (CHANGED)
- `tools/report_ci_results.py` (NEW)
- `tools/validate_structure.py` (CHANGED)
- `tools/write_phase3_delivery.py` (NEW)

## .github/workflows/android.yml

````yaml
name: Android build and runtime verification
on:
  workflow_dispatch:
  pull_request:
  push:
    branches: [arena/01a0e43a-2d-world-engine]
concurrency:
  group: android-${{ github.ref }}
  cancel-in-progress: true
permissions:
  contents: read
jobs:
  compile-and-unit-test:
    timeout-minutes: 20
    runs-on: ubuntu-24.04
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4
      - name: Install pinned Android SDK packages
        run: |
          export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
          printf 'y\n%.0s' {1..100} | sdkmanager --licenses >/dev/null
          sdkmanager 'platforms;android-34' 'build-tools;34.0.0'
      - run: python3 tools/validate_structure.py
      - name: Compile app and execute JVM tests
        run: |
          set -o pipefail
          ./gradlew :engine-math:test :engine-core:test :engine-animation:testDebugUnitTest :engine-physics:testDebugUnitTest :engine-render:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :engine-assets:assembleDebugAndroidTest :engine-io:assembleDebugAndroidTest :engine-physics:assembleDebugAndroidTest --console=plain --stacktrace 2>&1 | tee ci-build.log
      - name: Report unit results
        if: always()
        run: python3 tools/report_ci_results.py unit
      - name: Annotate build failures
        if: failure()
        run: python3 tools/annotate_ci_failure.py ci-build.log
      - uses: actions/upload-artifact@v5
        if: always()
        with:
          name: android-build-and-unit-results
          path: |
            ci-build.log
            app/build/outputs/apk/debug/*.apk
            **/build/reports/tests/
  device-tests:
    timeout-minutes: 25
    runs-on: ubuntu-24.04
    needs: compile-and-unit-test
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '17'
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
          script: bash -eo pipefail -c './gradlew :engine-physics:connectedDebugAndroidTest :engine-assets:connectedDebugAndroidTest :engine-io:connectedDebugAndroidTest :app:connectedDebugAndroidTest --console=plain --stacktrace 2>&1 | tee ci-device.log'
      - name: Report device results
        if: always()
        run: python3 tools/report_ci_results.py device
      - name: Annotate device failures
        if: failure()
        run: python3 tools/annotate_ci_failure.py ci-device.log
      - uses: actions/upload-artifact@v5
        if: always()
        with:
          name: android-device-results
          path: |
            ci-device.log
            **/build/reports/androidTests/
````

## .gitignore

````text
*.iml
.gradle/
**/build/
local.properties
.idea/
*.apk
*.aab

.venv/

ci-build.log
ci-device.log
````

## README.md

````markdown
# 2D WORLD Engine

Native Android 2D scene editor, written in Kotlin with Jetpack Compose and OpenGL ES 3.

## Current status

**Phase 3 runtime and authoring source is implemented. The full Android build, 31 JVM tests and 15 API 34 emulator tests passed in CI. Physical-device acceptance and cross-phase sound/particle/script event integrations remain outstanding; see the Phase 3 guide.** Do not treat this as a fully verified or finished Unity-class engine.

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
- Full pinned-toolchain Gradle/Compose build, debug APK/test APK assembly, and **31 JVM tests with zero failures/skips**: passed in CI at final code revision `053ec45`; exact evidence and genuine APK artifacts are in the verification ledger.
- Host SQLite and repository/module/XML/wrapper checks: passed.
- **15 API 34 emulator tests passed**, including live play, real GLES framebuffer checks, assets and persistence. Physical phone/tablet/gamepad and sustained-performance checks remain unverified.

## Data safety

Projects use app-private storage: `files/2DWorldProjects/<UUID>/`. No broad storage or internet permission is requested. Uninstalling/clearing app data deletes projects; back up important projects first. Catalog folders are virtual; immutable payload filenames do not change when assets are renamed or moved in the catalog. Asset edits create revisions or new assets, not scene undo commands. Old payload revisions are retained; automatic cleanup is not implemented.

**Phase gate:** verify Phase 3 and confirm before Phase 4 (tilemaps, particles, audio and runtime UI).
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
        versionCode = 3
        versionName = "0.3.0"
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
    private fun withGl(block: (SceneRenderer)->Unit) {
        val display=eglGetDisplay(EGL_DEFAULT_DISPLAY);val version=IntArray(2)
        assertTrue(eglInitialize(display,version,0,version,1))
        val configs=arrayOfNulls<android.opengl.EGLConfig>(1);val count=IntArray(1)
        val attrs=intArrayOf(EGL_RENDERABLE_TYPE,0x40,EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE)
        assertTrue(eglChooseConfig(display,attrs,0,configs,0,1,count,0));assertTrue(count[0]>0)
        val context=eglCreateContext(display,configs[0],EGL_NO_CONTEXT,intArrayOf(EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE),0)
        val surface=eglCreatePbufferSurface(display,configs[0],intArrayOf(EGL_WIDTH,64,EGL_HEIGHT,64,EGL_NONE),0)
        try {
            assertTrue(eglMakeCurrent(display,surface,surface,context))
            val renderer=SceneRenderer { fail(it) };renderer.onSurfaceCreated(null,null);renderer.onSurfaceChanged(null,64,64)
            block(renderer);assertEquals(GL_NO_ERROR,glGetError())
        } finally {
            eglMakeCurrent(display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT)
            eglDestroySurface(display,surface);eglDestroyContext(display,context);eglTerminate(display)
        }
    }
    private fun pixel(x: Int,y: Int): List<Int> {
        val bytes=ByteBuffer.allocateDirect(4);glReadPixels(x,y,1,1,GL_RGBA,GL_UNSIGNED_BYTE,bytes)
        return (0..3).map { bytes.get(it).toInt() and 255 }
    }
    @Test fun solidSpriteRendersAndContextCanBeRecreated() {
        repeat(2) { withGl { renderer ->
            renderer.frame=RenderFrame(scene=Scene(nodes=listOf(Node(components=listOf(TransformComponent(),SpriteComponent(Vec2(32f,32f),Color(1f,0f,0f)))))))
            renderer.onDrawFrame(null);val p=pixel(32,32);assertTrue(p[0]>240);assertTrue(p[1]<10)
        } }
    }
    @Test fun rotatedSplitViewsKeepTheirOwnScissoredPixels() = withGl { renderer ->
        val red=Node(components=listOf(TransformComponent(Transform(Vec2(-60f,0f))),SpriteComponent(Vec2(16f,32f),Color(1f,0f,0f))))
        val blue=Node(components=listOf(TransformComponent(Transform(Vec2(60f,0f))),SpriteComponent(Vec2(16f,32f),Color(0f,0f,1f))))
        val views=listOf(CameraView("left",Camera2D(Vec2(-60f,0f),rotation=90f),CameraViewport(width=.5f)),CameraView("right",Camera2D(Vec2(60f,0f)),CameraViewport(x=.5f,width=.5f)))
        renderer.frame=RenderFrame(scene=Scene(nodes=listOf(red,blue)),views=views,grid=false);renderer.onDrawFrame(null)
        assertTrue(pixel(16,32)[0]>240);assertTrue("Rotated rectangle should span the left viewport horizontally",pixel(28,32)[0]>240)
        assertTrue(pixel(48,32)[2]>240);assertTrue(pixel(60,32)[2]<80)
    }
    @Test fun parallaxAndActualGlLinesRenderIntoFramebuffer() = withGl { renderer ->
        val node=Node(components=listOf(TransformComponent(),SpriteComponent(Vec2(16f,16f),Color(1f,0f,1f)),ParallaxComponent(Vec2())))
        renderer.frame=RenderFrame(scene=Scene(nodes=listOf(node)),camera=Camera2D(Vec2(100f,0f)),debugLines=listOf(DebugLine(Vec2(76f,-12f),Vec2(124f,-12f),Color(0f,1f,0f))),grid=false)
        renderer.onDrawFrame(null);assertTrue(pixel(32,32)[0]>240);assertTrue(pixel(32,32)[2]>240)
        assertTrue("Expected real green GL line pixels",(19..21).any { y -> (30..33).any { x -> val p=pixel(x,y);p[1]>240 && p[0]<10 && p[2]<10 } })
        renderer.frame=renderer.frame.copy(camera=Camera2D(Vec2(200f,0f)),debugLines=emptyList());renderer.onDrawFrame(null)
        assertTrue("Zero-factor parallax must remain screen-fixed",pixel(32,32)[0]>240)
    }
}
````

## app/src/androidTest/kotlin/world/engine/app/PlayModeTest.kt

````kotlin
package world.engine.app

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class PlayModeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun step(label: String): Long? {
        val node=compose.onAllNodesWithText("$label · step",substring=true).fetchSemanticsNodes().firstOrNull() ?: return null
        val text=node.config[SemanticsProperties.Text].joinToString { it.text }
        return Regex("step ([0-9]+)").find(text)?.groupValues?.get(1)?.toLongOrNull()
    }
    @Test fun realPlayPauseStepStopDoesNotOverwriteSavedScene() {
        val projects=File(compose.activity.filesDir,"2DWorldProjects")
        val before=projects.listFiles().orEmpty().map { it.name }.toSet()
        compose.onNodeWithText("New project").performClick();compose.onNodeWithText("Create",useUnmergedTree=true).performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("Systems",substring=false).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Systems").performScrollTo().performClick();compose.onNodeWithText("Scene",substring=false).performClick()
        compose.onNodeWithText("Add platformer test rig").performScrollTo().performClick()
        compose.onNodeWithText("Save",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("MyGame • saved").fetchSemanticsNodes().isNotEmpty()}
        val project=projects.listFiles().orEmpty().single { it.name !in before }
        val scene=File(project,"Scenes/Main.json");val saved=scene.readBytes()
        compose.onNodeWithText("Play",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000){(step("PLAY") ?: 0)>=12}
        compose.onNodeWithText("bodies 7 · joints 1",substring=true).assertExists()
        compose.onNodeWithText("Pause",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("PAUSED · step",substring=true).fetchSemanticsNodes().isNotEmpty()}
        val pausedStep=requireNotNull(step("PAUSED"))
        compose.onNodeWithText("Step",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000){(step("PAUSED") ?: 0)>pausedStep}
        compose.onNodeWithText("Stop",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("MyGame • saved").fetchSemanticsNodes().isNotEmpty()}
        assertArrayEquals(saved,scene.readBytes())
        compose.onNodeWithText("Save",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("Save",substring=false).fetchSemanticsNodes().any { !it.config.contains(SemanticsProperties.Disabled) }}
        assertArrayEquals(saved,scene.readBytes())
    }
}
````

## app/src/main/assets/licenses/JBOX2D.txt

````text
Copyright (c) 2013, Daniel Murphy
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:
  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.
  * Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
````

## docs/PHASE2_README.md

````markdown
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
````

## docs/PHASE3.md

````markdown
# Phase 3 — animation, physics, input and cameras

## 1. ARCHITECTURE DECISIONS

Phase 3 adds executable runtime systems and their authoring tools to the existing native Android editor. It does **not** implement Phase 4 or claim that untested hardware workflows have passed.

- **21-module boundary preserved.** `engine-core` owns serializable runtime components and input action data. `engine-animation`, `engine-physics` and `engine-render` implement animation, JBox2D and camera/render behavior. `editor-animation` supplies the timeline. `editor-viewport/PreviewSession` coordinates the runtime; the editor ViewModel owns lifecycle and authoring commands.
- **Real simulation, not a playback mock.** JBox2D 2.2.1.1 runs at 60 Hz, 100 pixels/metre, eight velocity and three position iterations. A preview accepts at most 0.25 seconds of elapsed time and eight catch-up steps per update; discarded time is displayed.
- **Nondestructive play.** Play creates a separate scene/world. Pause releases held input, Step advances one fixed step, and Stop discards the preview. Save/autosave retain authored data. Backgrounding pauses; returning does not silently resume. Physics is confined to one worker; GL resources remain on the GLSurfaceView render thread.
- **Animation model.** Frame-indexed position, rotation, scale, RGBA and asset-UUID tracks; linear/step sampling; FPS, once/loop/ping-pong; transient scrub; actor binding. Frame zero and crossed-frame events are dispatched in order. Sprite-change events retain their result until the next sprite event, including during reverse playback, and take precedence over sprite tracks. Scrub/seek reconstructs forward-frame event state without dispatching side effects.
- **65 built-ins.** Five archetypes × 13 actions (idle, walk, run, jump, attack, hurt, death, roll, dash, shoot, reload, climb, swim). These are editable procedural **transform/color clips**, not hand-drawn sprite-frame artwork. Actual frame animation is authored with imported/generated image UUIDs and SPRITE keys.
- **Honest phase boundary.** TRIGGER events appear in the real preview event stream; SET_SPRITE changes the rendered image. Sound, particle and script event consumers cannot work before their Phase 4/5 systems exist, so they are not exposed as inactive choices. This is a remaining cross-phase requirement, not a claim of completed sound/particle/script integration.
- **Physics/editor tools.** Static/dynamic/kinematic bodies; rectangle, circle, convex polygon, capsule and compound fixtures; density/friction/restitution; sensors, contact aggregation, grounded checks, raycast/AABB APIs; fixed, distance, spring, revolute, prismatic, wheel and rope joints. The editor supports local shape fields, compound parts, viewport polygon drawing, real bitmap-alpha hull generation and per-node/per-prefab-part bodies. GL_LINES show fixture outlines and joint connections.
- **Body/animation ownership.** Dynamic bodies own their pose. Body scale tracks and physical ancestor scale animation are rejected. “Create physics parent + visual child” provides an authorable separation. Platformer controllers require dynamic bodies; top-down movement supports dynamic/kinematic bodies or an unphysical entity. Static bodies cannot accept movement controllers.
- **Input.** Action mappings combine keyboard, mouse buttons, gamepad buttons/axes and touch. Axis dead zones, signed scales, key listening, edits/removal, joystick and held touch buttons are real. Short taps are buffered between ticks; focus loss, pause and device disconnection cancel input appropriately. Attack is an action/event, **not** a promised combat implementation.
- **Cameras.** Follow/smoothing, rotated dead zones, zoom, rotation, viewport-aware world limits, six shake presets, multiple/split views, transitions and per-sprite parallax. Runtime camera commands are queued to the simulation worker. GLES uses per-view viewport/scissor and rotation-aware coordinate mapping.
- **Persistence and prefabs.** Existing scene JSON remains readable through defaulted fields. Clips, joints, input and cameras serialize with the scene. Prefab bundles carry animation resources and remap internal joint/camera references; runtime overrides survive refresh. Removing bodies/entities cleans connected references. Authoring edits use undo history.
- **Safety and licensing.** Validation bounds resources and rejects invalid convex geometry before JBox2D can substitute fallback shapes. Physics failure returns to the authored scene with an error. JBox2D's BSD notice ships in `app/src/main/assets/licenses/JBOX2D.txt`. No new copyrighted artwork or network-dependent core feature is introduced.

## 2. FILE TREE

[PHASE3_FILES.md](PHASE3_FILES.md) lists **every** repository file as NEW, CHANGED or UNCHANGED against the captured end-of-Phase-2 baseline, with hashes for non-self-indexing files.

Principal additions:

```text
engine-core/.../RuntimeData.kt, InputRouter.kt
engine-animation/.../AnimationPlayer.kt, AnimationPresets.kt
engine-physics/.../PhysicsWorld.kt, ColliderGeometry.kt
engine-render/.../CameraRig.kt
editor-animation/.../TimelineEditor.kt
editor-viewport/.../PreviewSession.kt
editor-ui/.../RuntimePanels.kt, PlayControls.kt
engine-*/src/test/...                    runtime JUnit suites
engine-physics/src/androidTest/...      real Bitmap alpha test
app/src/androidTest/.../PlayModeTest.kt  nondestructive play UI test
app/src/main/assets/licenses/JBOX2D.txt
tools/RuntimeSmoke.kt                   executable host integration checks
```

Changed integration files include `Scene`, `Prefab`, `Camera2D`, `SceneRenderer`, `SpriteBatch`, `WorldViewport`, `EditorViewModel`, `WorldEditorApp`, module Gradle files and Android CI. The exact paths and complete unchanged set are in the inventory, rather than a shortened tree presented as exhaustive.

## 3. COMPLETE SOURCE

[PHASE3_SOURCE.md](PHASE3_SOURCE.md) contains the **complete text of every new/changed file**, including implementation, tests, Gradle/workflow configuration, licenses and delivery documents. It does not recursively embed itself. Files also remain individually editable in the repository; the appendix is not a replacement project skeleton.

`tools/write_phase3_delivery.py` regenerates the inventory and appendix from `docs/PHASE3_BASELINE.json`. `--check` detects stale delivery snapshots. Phase 1/2 appendices remain historical records; current module files are authoritative.

## 4. GRADLE CONFIG

The locked app stack remains:

| Item | Version |
|---|---|
| Gradle / Android Gradle plugin | 8.7 / 8.5.2 |
| Kotlin / JDK | 1.9.24 / 17 |
| Compose compiler / BOM | 1.5.14 / 2024.06.00 |
| kotlinx.serialization / coroutines | 1.6.3 / 1.8.1 |
| Android min / target / compile SDK | 24 / 34 / 34 |
| JBox2D | 2.2.1.1 |
| JUnit Jupiter | 5.10.3 |
| Editor APK version | 0.3.0, code 3 |

`engine-physics` adds `org.jbox2d:jbox2d-library:2.2.1.1`; viewport depends on animation/physics/render. Animation/render/physics local unit tasks use JUnit Platform. The alpha-collider Android test uses the real Android Bitmap API. Full changed Gradle files are in the source appendix.

GitHub Actions builds the APK/test APKs, runs JVM suites, then runs API 34 emulator tests with KVM and software GLES. It uploads debug APKs, build/test reports and logs. Build-time dependency downloads require connectivity once; that is separate from the offline operation of installed projects.

## 5. BUILD & RUN

### Android Studio

1. Open the repository root in Android Studio Koala or a compatible newer release.
2. Set **Gradle JDK to 17**. Install Android SDK Platform 34 and Build Tools 34.0.0; accept SDK licenses.
3. Sync and run the `app` configuration on an API 24+ GLES 3 device, or an API 34 GLES 3 emulator.
4. Keep network permission disabled: project creation, assets, animation, physics and editing are local.

### Command line

```bash
./gradlew :engine-math:test :engine-core:test \
  :engine-animation:testDebugUnitTest :engine-physics:testDebugUnitTest \
  :engine-render:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest \
  :engine-assets:assembleDebugAndroidTest :engine-io:assembleDebugAndroidTest \
  :engine-physics:assembleDebugAndroidTest

adb install -r app/build/outputs/apk/debug/app-debug.apk
./gradlew :engine-physics:connectedDebugAndroidTest \
  :engine-assets:connectedDebugAndroidTest :engine-io:connectedDebugAndroidTest \
  :app:connectedDebugAndroidTest
```

`ANDROID_HOME` must point to the SDK (or set `sdk.dir` in your local, untracked `local.properties`). After the pinned dependencies are cached, Gradle supports `--offline`. Do not confuse the source-only host diagnostic with these Gradle/app commands.

### First real physics scene

1. Create an Empty project. Open **Systems → Scene → Add platformer test rig**. This adds scene entities plus a pendulum joint and resets gravity/input to defaults, as the UI explains.
2. Enable **Colliders on**, Save, then Play. The player, ball, capsule and pendulum simulate; the sensor is non-solid and the camera follows the player.
3. Move with the virtual joystick, A/D, arrows or gamepad X. Jump with the touch action, Space or gamepad A after landing. J, gamepad B or mouse primary produces the mapped Attack input event; no combat is claimed.
4. Pause, Step, Resume and Stop. The saved authoring scene must remain unchanged. Camera shake and camera-transition controls operate only during active play.
5. In Systems → Actor, tune materials or add compound parts. Draw a polygon with 3–8 viewport taps and Apply; alpha hulls require a textured sprite. Select split-prefab children to author per-part bodies.

### Animation, rebinding and cameras

- Select a sprite, open **Animation**, create a clip or add the 65 presets, and assign it to the actor. Scrub/Seek with the viewport visible. Set keys at the current integer frame; setting the same property/frame replaces that key. RGBA includes alpha. Set SPRITE keys to image UUIDs for actual frame animation.
- Use a visual child for transform animation on a physical character. The helper preserves the root transform and moves its sprite/animator to a child with an identity local transform.
- Add named TRIGGER or SET_SPRITE events at the selected frame, then Play. The event console shows dispatches; sprite events alter the image.
- **Systems → Input** adds/updates/removes mappings and listens for keyboard/gamepad button codes. Axis codes use Android MotionEvent axis IDs; touch binds by action name. UI fields are shown only for applicable source/shape/joint types.
- Create cameras in **Systems → Scene**, configure them under Actor, and choose Full/Left/Right/Top/Bottom viewports. “Render all enabled cameras” enables split-view authoring. Runtime camera buttons transition between enabled cameras; six shake presets and parallax affect real rendering.

Projects retain the previously explained app-private layout under `filesDir/2DWorldProjects`. This does not claim unrestricted `/storage/emulated/0/2DWorldProjects` access on scoped-storage Android versions.

## 6. VERIFICATION TESTS

**Final CI run [36350107741](https://github.com/surafel5509-del/2D-WORLD-ENGINE/actions/runs/36350107741) passed at code revision `053ec45`: full Android/Compose build, 31 JVM tests and 15 API 34 emulator tests, with zero failures or skips.** The genuine editor debug APK and reports are linked in the verification ledger. Physical-device checks remain outstanding.

[PHASE3_VERIFICATION.md](PHASE3_VERIFICATION.md) is the dated evidence ledger: exact CI revisions/runs, passed host checks, authored test inventory, hardware gaps and manual acceptance steps. A test's source existing is not recorded as a test pass.

Host checks:

```bash
python3 tools/validate_structure.py
python3 tools/check_asset_schema.py
python3 tools/write_phase3_delivery.py --check
```

Optional standalone compiler diagnostic (not an APK build): install the tool versions documented in `tools/check_engine_standalone.py`, then provide genuine Android 34 and JBox2D 2.2.1.1 JARs:

```bash
python tools/check_engine_standalone.py \
  --android-jar /path/to/android-34/android.jar \
  --jbox2d /path/to/jbox2d-library-2.2.1.1.jar
```

It separately compiles eight modules and executes six core plus ten runtime smoke groups. JVM JUnit suites additionally cover short input taps/disconnection, strict convexity, tiny-fixture rejection, reverse sprite-event timing and camera limits/transitions.

## 7. KNOWN LIMITATIONS

- Read the verification ledger before relying on a build/device claim. Physical phones/tablets, controllers, OEM lifecycle behavior and sustained frame rate require hardware acceptance; host math tests do not prove GLES rendering.
- Sound, particle and script event consumers are deferred to their real Phase 4/5 systems. Trigger and sprite events are functional now. Therefore the cross-phase event-integration portion of the original specification is **not yet complete**.
- The 65 built-ins are procedural transform/color motion, not 65 hand-drawn frame sequences. Frame sequences are editable with real image assets; there is no skeletal rigging or animation blending/state machine in this phase.
- Convex alpha hulls fill holes/concavities and reduce to at most eight vertices, potentially shrinking the outline. There is no automatic concave decomposition. Capsules use a rectangle plus round ends; nonuniform circles use octagons.
- Physics ancestor transforms must be uniform, non-reflected and unskewed. Dynamic pose/physical scale animation is rejected; animate a visual child. Body centres are bounded to ±1,000,000 pixels; tiny/degenerate fixtures and excessive values are rejected with errors rather than silently substituted geometry.
- Limits: 512 preview bodies, 4096 collider definitions, 32 per body, 1000 joints, 512 clips, 256 distinct input bindings and eight catch-up steps. They are safety ceilings, **not** benchmarked mobile performance promises.
- Controllers provide a grounded platformer or top-down movement foundation, not a finished game/combat system. All devices feed a shared action map; per-player device ownership/input contexts are not implemented. Mouse support here is button mapping plus existing editor pointer gestures, not arbitrary pointer-position gameplay scripts.
- Camera transitions interpolate center/zoom/rotation over 0.4 seconds; changing split layouts can change viewport rectangles immediately. Overlapping camera viewports draw in scene order. “All cameras” means all enabled cameras; no enabled camera uses the default view.
- Joint/component/entity removal and authoring edits are undoable. Save retains authored state; there is deliberately no “apply physics result back to scene” control.
- This delivers editor/runtime source and its debug build path, not the signed standalone game APK export pipeline or five finished games reserved for Phase 7.

## 8. NEXT PHASE PREVIEW + CONFIRMATION

Phase 4 would implement tile sets/maps and brushes, collision generation from tile data, the particle editor/runtime, actual offline audio playback/authoring, and runtime UI. It would also connect animation sound/particle events to those real systems. Script consumers remain Phase 5.

**Stop here. Confirm Phase 3 acceptance and explicitly authorize Phase 4 before implementation continues.**
````

## docs/PHASE3_BASELINE.json

````json
{"README.md": "3708ece381fcec2e27c7ce78cc0e96fb1903187dd977f3f2b02cec1bcd834cf3", ".gitignore": "9c4dfa06b4927b2bc0e2bc139b3db30ab0491b3e11755dccd2d94c24e77166eb", "settings.gradle.kts": "febdf83ff285b42d48a3be287756d04f91acaa86fa61a1fa10c46abef7f5be4a", "build.gradle.kts": "8e4d6736f6e5a5dabc5c8f1faf837f0bc03d859760f1cedd4e2ab0301717daef", "gradle.properties": "e43a00491832254a35cf5a979ef6de17f78c4e159d446be39319d2e3d3e46325", "gradlew": "bf2ca3f9d7c42b831380b47ab9e7594ddc33054b3e09253a0c4862353d5d604b", "gradlew.bat": "e54283c0f86074fa408c1161d08ef8f63992323a8c940c66f3626e8abc5bd8cc", "engine-math/build.gradle.kts": "08590c5230e93208a74d3eecdb41d14eb217e1bdc5b5bba8e2b7dbad969473e7", "engine-math/src/main/kotlin/world/engine/math/Math.kt": "92bf644a6c65322c2b8376d77fb6b5eec057667ef440e89f7dbe635937bc709f", "engine-math/src/test/kotlin/world/engine/math/MathTest.kt": "851df9fa94b2a32099feab9df8d362858e2ca13906432f4eab829df09817ac2e", "engine-core/build.gradle.kts": "2e26d4eee1270e12462b194eed513363186e3b9cccad5beba0684ae4914be53b", "engine-core/src/main/kotlin/world/engine/core/Scene.kt": "250c1854e47c73dad49d1120b768f9237aba55fefa31fdac651b3abd75b2d866", "engine-core/src/main/kotlin/world/engine/core/Prefab.kt": "f6c6c94cc85d02910664125511b9aa379a3604fe71f4eaa3974ec8d27a7180f7", "engine-core/src/test/kotlin/world/engine/core/SceneTest.kt": "33f5002a1fd607090c875a75c0a95f87ec5e1ad936d93887fb4cbf2b10fbbbcb", "engine-core/src/test/kotlin/world/engine/core/PrefabTest.kt": "6546ff2c977229c864add8e57e9210ac57dbf0ceae31aca1c01bc24a6c65e6d0", "engine-render/build.gradle.kts": "cd0c7b21a32d4a7b733040f5d1eae8d557b1b0e1816bc2c2d0c3a494a863cf5f", "engine-render/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "engine-render/src/main/kotlin/world/engine/render/Camera2D.kt": "f30b1e282c7a1fcc8b310b3404b06c7e359d8a9e637f450c4567464c218b1b3f", "engine-render/src/main/kotlin/world/engine/render/SpriteBatch.kt": "5dd1b43870ff634126965913a2ebc3a0cd9fd7b2ee8f973b8251f44a14893b71", "engine-render/src/main/kotlin/world/engine/render/TextureLoader.kt": "c800e18ebc4b351f4d494c5f60596469849dcde4ed05f3dadbe2574b8be2c764", "engine-render/src/main/kotlin/world/engine/render/SceneRenderer.kt": "eb5c6bb3a0f74391d43cf3e185b941dd4874362b2cd53bf4165bd436bcb589da", "engine-physics/build.gradle.kts": "748d9dd8488d57c9454ee182b9c5fe187b0fff3a2cf542c941a04b3700b39cad", "engine-physics/README.md": "60c72e76071174e2e9b3b0d5e05f58f2498c596b103e0aad4c9718278ad32842", "engine-physics/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "engine-audio/build.gradle.kts": "bfaf836f9202cbb32531411e53d1c7b77a52fed759602310a759a9b1a07963d9", "engine-audio/README.md": "45e07672906786267572864ed1a51e75543b1b56e7a5b22662d711026259a985", "engine-audio/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "engine-animation/build.gradle.kts": "ed6028707eb566d81646dae608bd77b69cd054500537e7363740f4f4ed414e20", "engine-animation/README.md": "19af94e7684719b4f8cd91cff9e5f711a98830977527d693024a7d454dcbbcc9", "engine-animation/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "engine-assets/build.gradle.kts": "34b950ffb92ea29d890354737af2de1cb387cf0245e67af54f8238297758eed9", "engine-assets/README.md": "2eefbcc36a6f92d04d79a5c2e44defac207f759c8bda01bba4dd38a1255ad6b0", "engine-assets/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "engine-assets/src/main/kotlin/world/engine/assets/AssetDatabase.kt": "da1d7007331293b82e1c96f624f2814d709738cc6a45bb15f6832f555233555f", "engine-assets/src/main/kotlin/world/engine/assets/AssetRepository.kt": "d635ea2ac521369f3d10f5ce31e184864edf421feecbf5a6d01c80c7c5eb4c20", "engine-assets/src/main/kotlin/world/engine/assets/ImageOps.kt": "d68f467d46fa5d043bafcf0834b66b6f2318ba779dad3aa9d43257e9b245c2d2", "engine-assets/src/main/kotlin/world/engine/assets/SpriteSlicer.kt": "093d47c5338fa638c8820e23a02ee7d07a9a08795c597c2123224d866a66f9fa", "engine-assets/src/main/kotlin/world/engine/assets/ProceduralAssets.kt": "7e527f2aebb5dcb713417795fbd1397ee215a4a8ff284dcba1fd0282b970b449", "engine-assets/src/androidTest/kotlin/world/engine/assets/AssetPipelineTest.kt": "75a90542a103e2b1deec133fc5bfd411680a227f5fd958b045407d7ba20c4eea", "engine-scripting/build.gradle.kts": "c3d70a1b157af64a52aa31b747964cb3659101ef3dc1e07dd16feae4247ea3fd", "engine-scripting/README.md": "8d1ee12919c3d6c2b4e19dcac25970bad8908795de77795c782f7d2eaae36bd6", "engine-scripting/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "engine-ai/build.gradle.kts": "0cc2cbbb56bfbb31534606e27d5a7f348eadbd3ff36908c842b1d5e823f3dca6", "engine-ai/README.md": "26e6b320882124636ca3ad49d20443cd370463c6c690fd3561b4472db6f6f1d9", "engine-ai/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "engine-tilemap/build.gradle.kts": "2327a5c407399562c83e2ecee0070bd08da8212d917ce30d9da74d18879945ac", "engine-tilemap/README.md": "bbf3362e806d6c910871aaf5f2a1191ea986f8cf62ba555b7db061fd1e258235", "engine-tilemap/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "engine-particles/build.gradle.kts": "76286a1a1892cc9003acf2ea4e6b157f5b6fd5e4f3951300020731f17a97e63e", "engine-particles/README.md": "9bb1ea34ca3bc03c62d5ee953582560ba67d556242d59b176ecc3c9e20a9e920", "engine-particles/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "engine-ui/build.gradle.kts": "c627743a1af0a7fff5e6c495b6b983df6e2f84c844954c542c8cce31393d7aa7", "engine-ui/README.md": "202ca8258b36a0f325f8bcf457a3c421559844bf47f2a0c1519718afc52a3cef", "engine-ui/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "engine-io/build.gradle.kts": "73c42c113f5886640f5872cea8d44ec1f13b737556304a2631931e0b8fc539f0", "engine-io/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "engine-io/src/main/kotlin/world/engine/io/ProjectStore.kt": "e38a8fdb0727a6d007c1646a8d83a75d6eb156c7478a3456d8aa0d4646265fbd", "engine-io/src/androidTest/kotlin/world/engine/io/ProjectStoreTest.kt": "f4d6c61497d700ac13894956421f0e5fc3a4992bb460f945e318419f14e3983b", "engine-build/build.gradle.kts": "17aeae60c6b623b92286ed9387aa7c0813ff8bb1c9adb3b647653c5bc46087a9", "engine-build/README.md": "294564e7dc15de88695978fac950cecc8bcf6a6b850a448aec5a6e911048ed34", "engine-build/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "editor-ui/build.gradle.kts": "d131cffd9a16e0cfad4629df6e6c4c260515a4fbad778d1d55d42d40d508686f", "editor-ui/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "editor-ui/src/main/kotlin/world/engine/editor/EditorViewModel.kt": "6119e05f87a86dab808440098d1fdd92fcb47de18127da2d34a095b1f63b0063", "editor-ui/src/main/kotlin/world/engine/editor/WorldEditorApp.kt": "28fab52dfab47bd6297b90420669ff238dcc9287af5a88cfc1dc996a9b6dc15d", "editor-viewport/build.gradle.kts": "38b49fcc87423a8dee297e5b5328c53db3fc3214f731764fbb4f3bd06537ac90", "editor-viewport/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "editor-viewport/src/main/kotlin/world/engine/viewport/WorldViewport.kt": "aa0bce0e07571f2495fdefc708652a05e4d345e93521ae72b6bb77d4a6a561a0", "editor-assets/build.gradle.kts": "9d9c555168e76f85c200c92021d4ac76e2dc60268acdbc10aca4f0236c765684", "editor-assets/README.md": "bb3f12d6b140bcfa25009be11e2f74049a4854d0aeaba2fe337e48c0a53554ad", "editor-assets/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "editor-assets/src/main/kotlin/world/engine/asseteditor/AssetActions.kt": "0e2d581adf2945faf9652fed312194d6de0e7010a673f440f0baac31a8fc2ddb", "editor-assets/src/main/kotlin/world/engine/asseteditor/AssetBrowser.kt": "3c17a99264329a698d0ae28071f7657e6d46c7d12a13fb252ecc90f35c1a4cb9", "editor-assets/src/main/kotlin/world/engine/asseteditor/AssetInspector.kt": "ecc19797583e3854eb3b7e0ad36395b78bb12dc1a9c9c0b2d68f5a55bd2eaa6c", "editor-animation/build.gradle.kts": "14584230368434b08f3ec82beac4ed64ed95e90f4d08d61d91e253e7d016efb1", "editor-animation/README.md": "f8653ceaf4d7d58db65c305c548cf69ff2a39789c6a9fa534609ddac4fd73d0a", "editor-animation/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "editor-scripting/build.gradle.kts": "9fdf536ae1d612b622e9c0dccde9635f331a82ea0d942028600775e36c7987e9", "editor-scripting/README.md": "30a2a9b01b12d0e38ea1206f6c6021661412efb16298e8960e187770e2d5bbe3", "editor-scripting/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "editor-build/build.gradle.kts": "f0de30cc9e38f5d712c0f75349b6f28c88e1b2e9e8cb6ad794449fee06cfc7b7", "editor-build/README.md": "aef5e1d8d67cb41c8462164cb7db549080ed34094391265a02ff650771f2accd", "editor-build/src/main/AndroidManifest.xml": "571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f", "app/build.gradle.kts": "2c2f3743dbe8291b4046b4f814cca17ec425b7114a2507dbc64babd909bef281", "app/src/main/AndroidManifest.xml": "bc257b5b75ce0167c9550e824a96b3235b0ca6ee311522bc6546f0eda7536b2a", "app/src/main/kotlin/world/engine/app/MainActivity.kt": "389e5d7bcbcdaa28d5328a44336ef53b0e8780d471ae3f0eee9aad53520ea3e2", "app/src/main/res/xml/file_paths.xml": "7e771275d2ffdb130bdefac582c138d1c37f715bc274aa47c1dc3c709ac8b1a1", "app/src/main/res/values/styles.xml": "04eb151f059d6054eaa700f0783ef7a78cc1f3211819a3f3a0da64f0c3dd713b", "app/src/androidTest/kotlin/world/engine/app/EditorSmokeTest.kt": "13d7226c36de268fa9faaf91b0da01b6879579fc6d5aa8bf018cb4dac6286660", "app/src/androidTest/kotlin/world/engine/app/GlRenderTest.kt": "61d4760e68f1cc24d5d11bd2cde0ba839bc966daee8db906a4a937a9ba25830b", "docs/MASTER_SPEC.md": "84158e302455aa36b4bf358afa7ce11c8effebce3e4864e0fe9b6276be285257", "docs/VERIFICATION.md": "89640d834bb29c13eb4aa2a4e9f60ce2a8e91e29ff7a02283ec5a1984491d1b3", "docs/PHASE1_SOURCE.md": "36d30533e3d3e62abb5ae9b68f32f2fce6c7c27fde7654f0eb8b6ac070f3d22e", "docs/PHASE1_README.md": "d89d29e603dc315ac5c585bfb683fb441785353579d9ae57350256c939c8d968", "docs/PHASE2.md": "27af686a36d3e824c7a89e7b1e10248ec73a3c7b4717f7656ff44fb4465b6547", "docs/PHASE2_VERIFICATION.md": "5acb33e5ab16a77814b77af378b732be0b4ebd083cee6bf2eba673fa4da92f53", "docs/PHASE2_FILES.md": "91296954de236ccd59955c96ec1b558e47a57fbe3c884bd4aa832e07b77cd531", "docs/PHASE2_SOURCE.md": "976579e6a43ca675496de14e1352db829d02a3e57718ef30fc70498f3737352f", "gradle/wrapper/gradle-wrapper.jar": "cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8", "gradle/wrapper/gradle-wrapper.properties": "712d9bc912d4b0d42b0faf8c6afeed13ae31ced75ca28463b1dfc4ced1a4bc9a", "tools/validate_structure.py": "c52708ccb32655c40c1a55b7bb6dca4094459a6b8b9987bf49e41c2a7edc7349", "tools/CoreSmoke.kt": "00e65dc98ad5ad2cd4420b4bce91a455917f1fab8ef72098b5782339af07cb8f", "tools/check_engine_standalone.py": "24180b81b241aeb00de5f5899ee7c84c701b0620bc68285d4a688503d88fcc24", "tools/check_asset_schema.py": "2df9a654e48f4c27b14f5d844a91582eb72bd445daafd2f8b5b24552660bdb39", ".github/workflows/android.yml": "bf68c813f76137937a726d64b7ebfa73fd29d5dd774af71db72c3c84c54ee820"}
````

## docs/PHASE3_FILES.md

````markdown
# Phase 3 file inventory

Compared with the captured end-of-Phase-2 SHA-256 baseline. Generated inventory/source files do not hash themselves.

## NEW — 28 files

| Path | SHA-256 |
|---|---|
| `app/src/androidTest/kotlin/world/engine/app/PlayModeTest.kt` | `dd390594c76505d060da4588330a63609ef21915fd9028baaa808206da6716be` |
| `app/src/main/assets/licenses/JBOX2D.txt` | `2895b20e161e2fcf8ae9cdda59f86cc4e4db9136d5fc453b809ce843bd1d0cd9` |
| `docs/PHASE2_README.md` | `3708ece381fcec2e27c7ce78cc0e96fb1903187dd977f3f2b02cec1bcd834cf3` |
| `docs/PHASE3.md` | `5c540ee656adb0878e2eced515c34648cdc830c084e2b0e60c512fadb97e837c` |
| `docs/PHASE3_BASELINE.json` | `d19f72593a83976aa20dee09180932f51a16ac38c119ef699f9dd3df1f49d7c8` |
| `docs/PHASE3_FILES.md` | `generated; not self-hashed` |
| `docs/PHASE3_SOURCE.md` | `generated; not self-hashed` |
| `docs/PHASE3_VERIFICATION.md` | `0d69c6cc450cf8b2b712da5555ace3374606c9582ebbb26254e5f833e4494d7a` |
| `editor-animation/src/main/kotlin/world/engine/animationeditor/TimelineEditor.kt` | `9f37aa4d209b30d1a7a950bd3f9b2fb8130d68b83d5a2712745bf80e6c34ae5f` |
| `editor-ui/src/main/kotlin/world/engine/editor/PlayControls.kt` | `06d2aaf3307ecd1cd0473fd74f6e0eaee912f4539fb347247a5c59027b9a2d1d` |
| `editor-ui/src/main/kotlin/world/engine/editor/RuntimePanels.kt` | `6ecc7fd603996c83994ec3043e70a454fe08e15e05e22aebeebd61cd14ef5aa3` |
| `editor-viewport/src/main/kotlin/world/engine/viewport/PreviewSession.kt` | `bfa9d9be2664bf90d365c5d790b2d5720d862ee60997001c213bc16b176e660d` |
| `engine-animation/src/main/kotlin/world/engine/animation/AnimationPlayer.kt` | `3d4483a085b482b9b13990f17ea57e7f79735c9a8244ada550dcb551c5ee0af0` |
| `engine-animation/src/main/kotlin/world/engine/animation/AnimationPresets.kt` | `b9c840818e767b2db18b687fa5a34fffbfdfa00a88967b7f101c0bf1e2771c6b` |
| `engine-animation/src/test/kotlin/world/engine/animation/AnimationPlayerTest.kt` | `da0237cf0b68fab64fe1ffdb7b8b069515991faa670d58bc8e189dfd933b19da` |
| `engine-core/src/main/kotlin/world/engine/core/InputRouter.kt` | `44acc42c4347477f5b456e4f3dad384911123dac3198c8b654aec89602deffe4` |
| `engine-core/src/main/kotlin/world/engine/core/RuntimeData.kt` | `8f55b705242d78cff21e6eaa3ba64fbb6a80b5632ee82cdbe8d1c7da41e47849` |
| `engine-core/src/test/kotlin/world/engine/core/RuntimeDataTest.kt` | `84f985707b6c93cb552233b7821e5e621d98b533854516d89636ddaff8faf656` |
| `engine-physics/src/androidTest/kotlin/world/engine/physics/AlphaColliderTest.kt` | `ec8526159da11ebb4855bfad39760176b56b59364bd9a2acfe0474a0d8a683c5` |
| `engine-physics/src/main/kotlin/world/engine/physics/ColliderGeometry.kt` | `c33b1557e2337b1035412449ff63bf8a96ff6525d95f020b586b044d58b3072d` |
| `engine-physics/src/main/kotlin/world/engine/physics/PhysicsWorld.kt` | `7484b6e64a71f0d1de62fe4f8a003361d2c8964139c312217aa9191ec9d65264` |
| `engine-physics/src/test/kotlin/world/engine/physics/PhysicsWorldTest.kt` | `c5b6ec7db3b65ffb11400de34df8436a6ed79d4ae083100d6565c6e4570bf148` |
| `engine-render/src/main/kotlin/world/engine/render/CameraRig.kt` | `f58a3549a6058b4ef48d8f8ab868c3374588ca7401a3d852237b6ef3a4e59b73` |
| `engine-render/src/test/kotlin/world/engine/render/CameraRigTest.kt` | `875b659a14262891c777ceabed3e72320671527453883ac5f083637deff52274` |
| `tools/RuntimeSmoke.kt` | `295b53a980fd580ab8f836d19cf20fd6c3f23618abb9bc3ea4f4077546902652` |
| `tools/annotate_ci_failure.py` | `4c1cfaeb9d46a1adc082b3f411ffa7a12c190c5485c6fe03b9f5e0823e1551e0` |
| `tools/report_ci_results.py` | `c6f19722e022fa015736ef72e1b89153b793f9399f92ab32ce346ee695ca74f9` |
| `tools/write_phase3_delivery.py` | `da9857724f5677de99644db3cd672b100dc9cf6bc59920b9bdd4dfd73d1257b2` |

## CHANGED — 24 files

| Path | SHA-256 |
|---|---|
| `.github/workflows/android.yml` | `da600dd32a6cb798fcfda2a1cebe9b4185ccac1cbae3c74d0578403ab088c06c` |
| `.gitignore` | `8dc14c72d0dda33f5b0b71b55bbfd8ebb5d62ac868713d3b0bc1c048c676931b` |
| `README.md` | `890b7436246864f6981834481400fa5723e1e6d41771afa7742a3b3460a2dca0` |
| `app/build.gradle.kts` | `1f61fb5f336d157f8b89945f6509c0101584771bee513ad4c3126e2e52527a04` |
| `app/src/androidTest/kotlin/world/engine/app/GlRenderTest.kt` | `def61e6bcf6dd93a949cdaa5908f580a4aae90b449d0e9612ee1b1f0270c0159` |
| `editor-animation/README.md` | `75e26f1bb73acb29809ab472624578f4ed5525a4e509eb7b8edda5f0889c3853` |
| `editor-animation/build.gradle.kts` | `6b729348d583a0b0a1534b42b4aeed5604370ed00693b26eb4355b6e50c8d5dd` |
| `editor-ui/build.gradle.kts` | `3d0e1b82f39a5edbefb303c3e5afcf8984967cee6d1c88e6a7a63f6913676151` |
| `editor-ui/src/main/kotlin/world/engine/editor/EditorViewModel.kt` | `beed11516312f606ba6f958c121636894e4e3668e0bac28c119c18b027053acb` |
| `editor-ui/src/main/kotlin/world/engine/editor/WorldEditorApp.kt` | `68ab0cb3763742e1474eda8dfc577a5ff8940634c5eec1b311d6241ae3e0daf2` |
| `editor-viewport/build.gradle.kts` | `9880d02f4ef9cb68a7f37eaefd15323449720d3942e6afb9127a2682cfce4fbd` |
| `editor-viewport/src/main/kotlin/world/engine/viewport/WorldViewport.kt` | `0b4c723ca45766b89742d93564063068035b81e997e40c32ce906dd8b7d89fe7` |
| `engine-animation/README.md` | `91c36ac1c643a3862a6718bfde333210f2a82bd53af607b136d51cb94fe84673` |
| `engine-animation/build.gradle.kts` | `e8162fb615a52266eac67fdfca2e472326adc9a019343ea4e1c51a8cfaf80b5f` |
| `engine-core/src/main/kotlin/world/engine/core/Prefab.kt` | `7b96dd66022e6b57303fb940452ff7e0cfe12b73bf7140f9c80320a60bb57da3` |
| `engine-core/src/main/kotlin/world/engine/core/Scene.kt` | `a895596d70b5d9a57635a8ba659b7950252ebe4c4eba4758cbcc28512f475c07` |
| `engine-physics/README.md` | `d027250c7ca7c7eb49f288fe8a617d06f011d9db565a51ecfac6b048f6912855` |
| `engine-physics/build.gradle.kts` | `a2bff2b6e0517c78ea0964540e12f6b0485c6749a3cf6b0f37a4ce6d639725aa` |
| `engine-render/build.gradle.kts` | `a249a279bd93dee8a6c187bab1d0b5b0ceb237e54fb0d217518ee91989a63401` |
| `engine-render/src/main/kotlin/world/engine/render/Camera2D.kt` | `7d6a4d59d8132cab7156e0b91cee9e1eda2aeeb5c9db0238f6deb32e8636f2ec` |
| `engine-render/src/main/kotlin/world/engine/render/SceneRenderer.kt` | `080e77c5fc67a76bf69381842ffaf1b7f0d0f3ee579a1efbb96c09cd79430238` |
| `engine-render/src/main/kotlin/world/engine/render/SpriteBatch.kt` | `4d2d7075b8d805ebca6a01c31e8ea60f8f53e76b3df949fd5d6032fe1fa5afa6` |
| `tools/check_engine_standalone.py` | `b29def0b98ae5f366b43b5796899f1675c6d47beb3e46e3c152278fe79207b25` |
| `tools/validate_structure.py` | `a8369eedb0732fb76ffe6c6790e463a4c5c7386626e116b9f9e1da1957a23cfd` |

## UNCHANGED — 81 files

| Path | SHA-256 |
|---|---|
| `app/src/androidTest/kotlin/world/engine/app/EditorSmokeTest.kt` | `13d7226c36de268fa9faaf91b0da01b6879579fc6d5aa8bf018cb4dac6286660` |
| `app/src/main/AndroidManifest.xml` | `bc257b5b75ce0167c9550e824a96b3235b0ca6ee311522bc6546f0eda7536b2a` |
| `app/src/main/kotlin/world/engine/app/MainActivity.kt` | `389e5d7bcbcdaa28d5328a44336ef53b0e8780d471ae3f0eee9aad53520ea3e2` |
| `app/src/main/res/values/styles.xml` | `04eb151f059d6054eaa700f0783ef7a78cc1f3211819a3f3a0da64f0c3dd713b` |
| `app/src/main/res/xml/file_paths.xml` | `7e771275d2ffdb130bdefac582c138d1c37f715bc274aa47c1dc3c709ac8b1a1` |
| `build.gradle.kts` | `8e4d6736f6e5a5dabc5c8f1faf837f0bc03d859760f1cedd4e2ab0301717daef` |
| `docs/MASTER_SPEC.md` | `84158e302455aa36b4bf358afa7ce11c8effebce3e4864e0fe9b6276be285257` |
| `docs/PHASE1_README.md` | `d89d29e603dc315ac5c585bfb683fb441785353579d9ae57350256c939c8d968` |
| `docs/PHASE1_SOURCE.md` | `36d30533e3d3e62abb5ae9b68f32f2fce6c7c27fde7654f0eb8b6ac070f3d22e` |
| `docs/PHASE2.md` | `27af686a36d3e824c7a89e7b1e10248ec73a3c7b4717f7656ff44fb4465b6547` |
| `docs/PHASE2_FILES.md` | `91296954de236ccd59955c96ec1b558e47a57fbe3c884bd4aa832e07b77cd531` |
| `docs/PHASE2_SOURCE.md` | `976579e6a43ca675496de14e1352db829d02a3e57718ef30fc70498f3737352f` |
| `docs/PHASE2_VERIFICATION.md` | `5acb33e5ab16a77814b77af378b732be0b4ebd083cee6bf2eba673fa4da92f53` |
| `docs/VERIFICATION.md` | `89640d834bb29c13eb4aa2a4e9f60ce2a8e91e29ff7a02283ec5a1984491d1b3` |
| `editor-animation/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `editor-assets/README.md` | `bb3f12d6b140bcfa25009be11e2f74049a4854d0aeaba2fe337e48c0a53554ad` |
| `editor-assets/build.gradle.kts` | `9d9c555168e76f85c200c92021d4ac76e2dc60268acdbc10aca4f0236c765684` |
| `editor-assets/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `editor-assets/src/main/kotlin/world/engine/asseteditor/AssetActions.kt` | `0e2d581adf2945faf9652fed312194d6de0e7010a673f440f0baac31a8fc2ddb` |
| `editor-assets/src/main/kotlin/world/engine/asseteditor/AssetBrowser.kt` | `3c17a99264329a698d0ae28071f7657e6d46c7d12a13fb252ecc90f35c1a4cb9` |
| `editor-assets/src/main/kotlin/world/engine/asseteditor/AssetInspector.kt` | `ecc19797583e3854eb3b7e0ad36395b78bb12dc1a9c9c0b2d68f5a55bd2eaa6c` |
| `editor-build/README.md` | `aef5e1d8d67cb41c8462164cb7db549080ed34094391265a02ff650771f2accd` |
| `editor-build/build.gradle.kts` | `f0de30cc9e38f5d712c0f75349b6f28c88e1b2e9e8cb6ad794449fee06cfc7b7` |
| `editor-build/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `editor-scripting/README.md` | `30a2a9b01b12d0e38ea1206f6c6021661412efb16298e8960e187770e2d5bbe3` |
| `editor-scripting/build.gradle.kts` | `9fdf536ae1d612b622e9c0dccde9635f331a82ea0d942028600775e36c7987e9` |
| `editor-scripting/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `editor-ui/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `editor-viewport/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-ai/README.md` | `26e6b320882124636ca3ad49d20443cd370463c6c690fd3561b4472db6f6f1d9` |
| `engine-ai/build.gradle.kts` | `0cc2cbbb56bfbb31534606e27d5a7f348eadbd3ff36908c842b1d5e823f3dca6` |
| `engine-ai/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-animation/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-assets/README.md` | `2eefbcc36a6f92d04d79a5c2e44defac207f759c8bda01bba4dd38a1255ad6b0` |
| `engine-assets/build.gradle.kts` | `34b950ffb92ea29d890354737af2de1cb387cf0245e67af54f8238297758eed9` |
| `engine-assets/src/androidTest/kotlin/world/engine/assets/AssetPipelineTest.kt` | `75a90542a103e2b1deec133fc5bfd411680a227f5fd958b045407d7ba20c4eea` |
| `engine-assets/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-assets/src/main/kotlin/world/engine/assets/AssetDatabase.kt` | `da1d7007331293b82e1c96f624f2814d709738cc6a45bb15f6832f555233555f` |
| `engine-assets/src/main/kotlin/world/engine/assets/AssetRepository.kt` | `d635ea2ac521369f3d10f5ce31e184864edf421feecbf5a6d01c80c7c5eb4c20` |
| `engine-assets/src/main/kotlin/world/engine/assets/ImageOps.kt` | `d68f467d46fa5d043bafcf0834b66b6f2318ba779dad3aa9d43257e9b245c2d2` |
| `engine-assets/src/main/kotlin/world/engine/assets/ProceduralAssets.kt` | `7e527f2aebb5dcb713417795fbd1397ee215a4a8ff284dcba1fd0282b970b449` |
| `engine-assets/src/main/kotlin/world/engine/assets/SpriteSlicer.kt` | `093d47c5338fa638c8820e23a02ee7d07a9a08795c597c2123224d866a66f9fa` |
| `engine-audio/README.md` | `45e07672906786267572864ed1a51e75543b1b56e7a5b22662d711026259a985` |
| `engine-audio/build.gradle.kts` | `bfaf836f9202cbb32531411e53d1c7b77a52fed759602310a759a9b1a07963d9` |
| `engine-audio/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-build/README.md` | `294564e7dc15de88695978fac950cecc8bcf6a6b850a448aec5a6e911048ed34` |
| `engine-build/build.gradle.kts` | `17aeae60c6b623b92286ed9387aa7c0813ff8bb1c9adb3b647653c5bc46087a9` |
| `engine-build/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-core/build.gradle.kts` | `2e26d4eee1270e12462b194eed513363186e3b9cccad5beba0684ae4914be53b` |
| `engine-core/src/test/kotlin/world/engine/core/PrefabTest.kt` | `6546ff2c977229c864add8e57e9210ac57dbf0ceae31aca1c01bc24a6c65e6d0` |
| `engine-core/src/test/kotlin/world/engine/core/SceneTest.kt` | `33f5002a1fd607090c875a75c0a95f87ec5e1ad936d93887fb4cbf2b10fbbbcb` |
| `engine-io/build.gradle.kts` | `73c42c113f5886640f5872cea8d44ec1f13b737556304a2631931e0b8fc539f0` |
| `engine-io/src/androidTest/kotlin/world/engine/io/ProjectStoreTest.kt` | `f4d6c61497d700ac13894956421f0e5fc3a4992bb460f945e318419f14e3983b` |
| `engine-io/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-io/src/main/kotlin/world/engine/io/ProjectStore.kt` | `e38a8fdb0727a6d007c1646a8d83a75d6eb156c7478a3456d8aa0d4646265fbd` |
| `engine-math/build.gradle.kts` | `08590c5230e93208a74d3eecdb41d14eb217e1bdc5b5bba8e2b7dbad969473e7` |
| `engine-math/src/main/kotlin/world/engine/math/Math.kt` | `92bf644a6c65322c2b8376d77fb6b5eec057667ef440e89f7dbe635937bc709f` |
| `engine-math/src/test/kotlin/world/engine/math/MathTest.kt` | `851df9fa94b2a32099feab9df8d362858e2ca13906432f4eab829df09817ac2e` |
| `engine-particles/README.md` | `9bb1ea34ca3bc03c62d5ee953582560ba67d556242d59b176ecc3c9e20a9e920` |
| `engine-particles/build.gradle.kts` | `76286a1a1892cc9003acf2ea4e6b157f5b6fd5e4f3951300020731f17a97e63e` |
| `engine-particles/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-physics/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-render/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-render/src/main/kotlin/world/engine/render/TextureLoader.kt` | `c800e18ebc4b351f4d494c5f60596469849dcde4ed05f3dadbe2574b8be2c764` |
| `engine-scripting/README.md` | `8d1ee12919c3d6c2b4e19dcac25970bad8908795de77795c782f7d2eaae36bd6` |
| `engine-scripting/build.gradle.kts` | `c3d70a1b157af64a52aa31b747964cb3659101ef3dc1e07dd16feae4247ea3fd` |
| `engine-scripting/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-tilemap/README.md` | `bbf3362e806d6c910871aaf5f2a1191ea986f8cf62ba555b7db061fd1e258235` |
| `engine-tilemap/build.gradle.kts` | `2327a5c407399562c83e2ecee0070bd08da8212d917ce30d9da74d18879945ac` |
| `engine-tilemap/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `engine-ui/README.md` | `202ca8258b36a0f325f8bcf457a3c421559844bf47f2a0c1519718afc52a3cef` |
| `engine-ui/build.gradle.kts` | `c627743a1af0a7fff5e6c495b6b983df6e2f84c844954c542c8cce31393d7aa7` |
| `engine-ui/src/main/AndroidManifest.xml` | `571f2735faf5e857752ee057ac8ef63425576a721616c60b86ee5d4c0a2d682f` |
| `gradle.properties` | `e43a00491832254a35cf5a979ef6de17f78c4e159d446be39319d2e3d3e46325` |
| `gradle/wrapper/gradle-wrapper.jar` | `cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8` |
| `gradle/wrapper/gradle-wrapper.properties` | `712d9bc912d4b0d42b0faf8c6afeed13ae31ced75ca28463b1dfc4ced1a4bc9a` |
| `gradlew` | `bf2ca3f9d7c42b831380b47ab9e7594ddc33054b3e09253a0c4862353d5d604b` |
| `gradlew.bat` | `e54283c0f86074fa408c1161d08ef8f63992323a8c940c66f3626e8abc5bd8cc` |
| `settings.gradle.kts` | `febdf83ff285b42d48a3be287756d04f91acaa86fa61a1fa10c46abef7f5be4a` |
| `tools/CoreSmoke.kt` | `00e65dc98ad5ad2cd4420b4bce91a455917f1fab8ef72098b5782339af07cb8f` |
| `tools/check_asset_schema.py` | `2df9a654e48f4c27b14f5d844a91582eb72bd445daafd2f8b5b24552660bdb39` |
````

## docs/PHASE3_VERIFICATION.md

````markdown
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
````

## editor-animation/README.md

````markdown
# editor-animation — Phase 3

Compose frame timeline with clip naming/FPS/length, looping and ping-pong, exact-frame seek and GL viewport scrubbing, actor assignment, editable transform/color/sprite keys, linear/step interpolation, frame events, and a 65-clip preset library.

The editor emits `AnimationActions` to the editor ViewModel. Authoring is undoable; scrubbing is transient and not persisted. The timeline is not a video exporter or skeletal animation system. Full source and verification results are in `docs/PHASE3.md` and its linked appendices.
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
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}
dependencies {
    api(project(":engine-animation"))
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.material3:material3")
}
````

## editor-animation/src/main/kotlin/world/engine/animationeditor/TimelineEditor.kt

````kotlin
package world.engine.animationeditor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import world.engine.core.*

interface AnimationActions {
    fun createClip()
    fun addAnimationPresets()
    fun selectClip(id: String)
    fun updateClip(clip: AnimationClip)
    fun assignClip(id: String?)
    fun scrubAnimation(frame: Float)
    fun clearScrub()
    fun animationError(message: String)
}
/** An editable frame timeline: real keys, interpolation, events, frame scrubbing and actor binding. */
@Composable fun TimelineEditor(scene: Scene,selected: String?,clipId: String?,frame: Float,images: Map<String,String>,actions: AnimationActions,modifier: Modifier=Modifier) {
    val clip=scene.clips.find { it.id==clipId }
    val node=scene.nodes.find { it.id==selected }
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("ANIMATION",style=MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(onClick=actions::createClip){Text("New clip")}
            TextButton(onClick=actions::addAnimationPresets){Text("Add 65 presets")}
            TextButton(onClick=actions::clearScrub){Text("Exit scrub")}
        }
        Picker("Clip",clip?.name ?: "Select",scene.clips.associate { it.id to it.name },actions::selectClip)
        if(clip==null) { Text("Create a clip or add the transform/color preset library. Select a scene entity to bind it.");return@Column }
        key(clip.id,clip.name,clip.fps,clip.frames) {
            var name by remember { mutableStateOf(clip.name) };var fps by remember { mutableStateOf(clip.fps.toString()) };var frames by remember { mutableStateOf(clip.frames.toString()) }
            Field("Name",name){name=it};Field("FPS 1–120",fps){fps=it};Field("Frame count",frames){frames=it}
            TextButton(onClick={guard(actions){actions.updateClip(clip.copy(name=name,fps=fps.toInt(),frames=frames.toInt()))}}){Text("Apply clip settings")}
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(onClick={actions.updateClip(clip.copy(loop=LoopMode.entries[(clip.loop.ordinal+1)%LoopMode.entries.size]))}){Text("Mode: ${clip.loop}")}
            TextButton(onClick={actions.updateClip(clip.copy(relative=!clip.relative))}){Text(if(clip.relative)"Relative transforms" else "Absolute transforms")}
        }
        Text("Frame ${frame.toInt()} / ${clip.frames-1}")
        Slider(value=frame.coerceIn(0f,(clip.frames-1).toFloat()),onValueChange=actions::scrubAnimation,valueRange=0f..maxOf(1,clip.frames-1).toFloat())
        var exact by remember(frame.toInt()) { mutableStateOf(frame.toInt().toString()) }
        Field("Exact frame",exact){exact=it}
        TextButton(onClick={guard(actions){actions.scrubAnimation(exact.toInt().toFloat())}}){Text("Seek")}
        Canvas(Modifier.fillMaxWidth().height(84.dp).background(Color(0xff121921))) {
            val denominator=maxOf(1,clip.frames-1).toFloat()
            clip.tracks.forEachIndexed { row,track -> track.keys.forEach { key -> drawCircle(Color.Cyan,4f,Offset(key.frame/denominator*size.width,(row+1)*size.height/6)) } }
            clip.events.forEach { drawCircle(Color.Yellow,4f,Offset(it.frame/denominator*size.width,size.height-5)) }
            drawLine(Color.White,Offset(frame/denominator*size.width,0f),Offset(frame/denominator*size.width,size.height),2f)
        }
        if(node!=null) {
            Text("Actor: ${node.name}")
            Row { TextButton(onClick={actions.assignClip(clip.id)}){Text("Assign to actor")};TextButton(enabled=node.component<AnimatorComponent>()!=null,onClick={actions.assignClip(null)}){Text("Detach animator")} }
        } else Text("Select a hierarchy entity to assign or scrub this clip.")
        KeyEditor(clip,frame.toInt(),images,actions)
        clip.tracks.forEach { track ->
            Text("${track.property} · ${track.keys.size} keys")
            if(track.property!=TrackProperty.SPRITE)TextButton(onClick={actions.updateClip(clip.copy(tracks=clip.tracks.map { if(it==track)it.copy(interpolation=if(it.interpolation==Interpolation.LINEAR)Interpolation.STEP else Interpolation.LINEAR) else it }))}){Text("Interpolation: ${track.interpolation}")}
            track.keys.sortedBy { it.frame }.forEach { key -> Row {
                TextButton(onClick={actions.scrubAnimation(key.frame.toFloat())}){Text("${key.frame}: ${key.assetId?.let { images[it] } ?: key.values.joinToString()}")}
                TextButton(onClick={actions.updateClip(clip.copy(tracks=clip.tracks.mapNotNull { t -> if(t!=track)t else t.copy(keys=t.keys-key).takeIf { it.keys.isNotEmpty() } }))}){Text("Remove")}
            } }
        }
        EventEditor(clip,frame.toInt(),images,actions)
        clip.events.forEach { e -> Row { Text("${e.frame}: ${e.name} (${e.kind})",Modifier.weight(1f));TextButton(onClick={actions.updateClip(clip.copy(events=clip.events-e))}){Text("Remove")} } }
        Text("Yellow markers are events. Trigger events are delivered to the preview event stream. Sprite events change the real asset reference. Sound, particle and script consumers belong to their later engine phases.",style=MaterialTheme.typography.bodySmall)
    }
}
@Composable private fun KeyEditor(clip: AnimationClip,frame: Int,images: Map<String,String>,actions: AnimationActions) {
    var property by remember { mutableStateOf(TrackProperty.POSITION) }
    var values by remember(property) { mutableStateOf(when(property){TrackProperty.ROTATION->"0";TrackProperty.SCALE->"1,1";TrackProperty.COLOR->"1,1,1,1";else->"0,0"}) }
    var asset by remember { mutableStateOf<String?>(null) }
    Picker("Property",property.name,TrackProperty.entries.associate { it.name to it.name }){property=TrackProperty.valueOf(it)}
    if(property==TrackProperty.SPRITE)Picker("Image",images[asset] ?: "Select",images){asset=it} else Field("Values (comma-separated, RGBA for color)",values){values=it}
    Button(onClick={guard(actions){
        val key=if(property==TrackProperty.SPRITE)AnimationKey(frame,assetId=asset) else AnimationKey(frame,values.split(',').map { it.trim().toFloat() })
        val old=clip.tracks.find { it.property==property }
        val track=(old ?: AnimationTrack(property,emptyList())).copy(keys=(old?.keys.orEmpty().filterNot { it.frame==frame }+key).sortedBy { it.frame })
        actions.updateClip(clip.copy(tracks=clip.tracks.filterNot { it.property==property }+track))
    }}){Text("Set key at frame $frame")}
}
@Composable private fun EventEditor(clip: AnimationClip,frame: Int,images: Map<String,String>,actions: AnimationActions) {
    var name by remember { mutableStateOf("trigger") };var kind by remember { mutableStateOf(AnimationEventKind.TRIGGER) };var asset by remember { mutableStateOf<String?>(null) }
    Text("Frame events",style=MaterialTheme.typography.titleSmall)
    Field("Event name",name){name=it}
    Picker("Kind",kind.name,AnimationEventKind.entries.associate { it.name to it.name }){kind=AnimationEventKind.valueOf(it)}
    if(kind==AnimationEventKind.SET_SPRITE)Picker("Image",images[asset] ?: "Select",images){asset=it}
    TextButton(onClick={actions.updateClip(clip.copy(events=clip.events+AnimationEvent(frame,name,kind,if(kind==AnimationEventKind.SET_SPRITE)asset else null)))}){Text("Add event at frame $frame")}
}
@Composable private fun Field(label: String,value: String,change: (String)->Unit) { OutlinedTextField(value,change,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth()) }
@Composable private fun Picker(label: String,value: String,options: Map<String,String>,select: (String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { TextButton(onClick={expanded=true}){Text("$label: $value")};DropdownMenu(expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=320.dp)){ options.forEach { (id,name)->DropdownMenuItem(text={Text(name)},onClick={expanded=false;select(id)}) } } }
}
private fun guard(actions: AnimationActions,block: ()->Unit) { try { block() } catch(e: IllegalArgumentException) { actions.animationError(e.message ?: "Invalid animation value") } }
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
    api(project(":editor-animation"))
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import world.engine.core.*
import world.engine.io.*
import world.engine.math.*
import world.engine.assets.*
import world.engine.asseteditor.AssetActions
import world.engine.animationeditor.AnimationActions
import world.engine.animation.*
import world.engine.physics.*
import world.engine.render.*
import world.engine.viewport.*
import java.util.concurrent.atomic.AtomicInteger

data class EditorState(
    val projects: List<Project> = emptyList(), val project: Project? = null,
    val scene: Scene = Scene(), val saved: Scene = Scene(), val selected: String? = null,
    val canUndo: Boolean = false, val canRedo: Boolean = false,
    val busy: Boolean = false, val error: String? = null, val recovery: Scene? = null, val recoveryIssue: String? = null,
    val assets: List<AssetRecord> = emptyList(), val selectedAsset: String? = null, val frames: List<SpriteFrame> = emptyList(), val assetProgress: String = "",
    val selectedClip: String?=null, val animationFrame: Float=0f, val scrubScene: Scene?=null,
    val playId: Long?=null, val paused: Boolean=false, val preview: PreviewFrame?=null,
    val drawCollider: Boolean=false, val colliderPoints: List<Vec2> = emptyList(), val removeRuntimeType: String?=null,
    val log: List<String> = emptyList()
) { val dirty get() = scene != saved }

/** Main-thread editor state; disk operations use IO dispatching and serialized persistence. */
class EditorViewModel(application: Application): AndroidViewModel(application), AssetActions, AnimationActions {
    @Volatile private var session: PreviewSession?=null
    private var playJob: Job?=null
    private val stepRequests=AtomicInteger()
    private var playSerial=0L
    private var viewportSize=800 to 600
    private var foreground=true
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
    private fun change(block: (EditorState)->EditorState) { mutable.update(block) }
    fun report(message: String) { change { it.copy(error=message,log=(it.log+message).takeLast(100)) } }
    fun dismissError() { change { it.copy(error=null) } }
    private fun note(message: String) { change { it.copy(log=(it.log+message).takeLast(100)) } }
    private fun task(block: suspend ()->Unit) {
        if(mutable.value.busy || mutable.value.playId!=null) return
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
        change { it.copy(project=open.project,scene=open.saved,saved=open.saved,selected=null,canUndo=false,canRedo=false,recovery=open.recovery,recoveryIssue=open.recoveryIssue,assets=catalog,selectedAsset=null,frames=emptyList(),assetProgress="",selectedClip=null,animationFrame=0f,scrubScene=null,drawCollider=false,colliderPoints=emptyList(),log=listOf("Opened ${open.project.manifest.name}")) }
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
    private fun sync(scene: Scene) { change { it.copy(scene=scene,scrubScene=null,canUndo=history.canUndo,canRedo=history.canRedo,selected=it.selected?.takeIf { id -> scene.nodes.any { n -> n.id==id } }) } }
    private fun edit(scene: Scene) {
        if(mutable.value.recovery!=null || mutable.value.recoveryIssue!=null || mutable.value.playId!=null) return
        try { finishDrag(); sync(history.commit(mutable.value.scene,scene)) }
        catch(e: IllegalArgumentException) { report("Edit rejected: ${e.message ?: "Invalid scene"}") }
    }
    fun select(id: String?) { if(mutable.value.playId==null)change { it.copy(selected=id,scrubScene=null,drawCollider=false,colliderPoints=emptyList()) } }
    fun addSprite() {
        if(mutable.value.busy || mutable.value.playId!=null) return
        val s=mutable.value; val node=sprite(s.scene.nodes.size+1,Vec2())
        edit(s.scene.copy(nodes=s.scene.nodes+node)); select(node.id)
    }
    fun importSprite(uri: Uri) = importAsset(uri)
    fun move(id: String, p: Vec2, end: Boolean) {
        if(mutable.value.busy || mutable.value.playId!=null || mutable.value.recovery!=null || mutable.value.recoveryIssue!=null) return
        val s=mutable.value; val node=s.scene.nodes.find { it.id==id } ?: return
        if(dragStart==null) dragStart=s.scene
        change { it.copy(scene=it.scene.replace(Prefabs.override(node.moved(p),"transform"))) }
        if(end) finishDrag()
    }
    fun updateNode(node: Node) {
        val old=mutable.value.scene.nodes.find { it.id==node.id } ?: return
        val fields=buildList { if(old.transform!=node.transform)add("transform"); if(old.name!=node.name)add("name"); if(old.sprite!=node.sprite)add("sprite");if(old.components.filterNot { it is TransformComponent || it is SpriteComponent }!=node.components.filterNot { it is TransformComponent || it is SpriteComponent })add("runtime") }
        edit(mutable.value.scene.replace(Prefabs.override(node,*fields.toTypedArray())))
    }
    fun deleteSelected() { val s=mutable.value; val removed=s.selected?.let { s.scene.descendants(it) }.orEmpty(); edit(s.scene.copy(nodes=s.scene.nodes.filterNot { it.id in removed }.map { n -> n.copy(components=n.components.map { c -> if(c is CameraComponent && c.follow in removed)c.copy(follow=null) else c }) },joints=s.scene.joints.filterNot { it.bodyA in removed || it.bodyB in removed },activeCamera=s.scene.activeCamera?.takeUnless { it in removed })) }
    fun undo() { if(mutable.value.playId!=null)return;finishDrag(); sync(history.undo(mutable.value.scene)) }
    fun redo() { if(mutable.value.playId!=null)return;finishDrag(); sync(history.redo(mutable.value.scene)) }
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
        val bundle=if(a.kind==AssetKind.PREFAB) { val definitions=assets().definitions(); withContext(Dispatchers.Default) { Prefabs.bundle(id,definitions,position) } } else Scene(nodes=listOf(
            Node(name=a.name,components=listOf(TransformComponent(Transform(position)),SpriteComponent(size=Vec2(a.width.toFloat(),a.height.toFloat()),assetId=id)))))
        val current=mutable.value.scene
        edit(current.copy(nodes=current.nodes+bundle.nodes,clips=mergeClips(current.clips,bundle.clips),joints=current.joints+bundle.joints)); select(bundle.nodes.first { it.parent==null }.id)
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
        val bundle=withContext(Dispatchers.Default) { Prefabs.bundle(binding.assetId,definitions,root.transform.position,s.scene.nodes.filter { it.id in ids },preserve,s.scene.clips) };val replacement=bundle.nodes
        val at=s.scene.nodes.indexOfFirst { it.id in ids }
        val remaining=s.scene.nodes.filterNot { it.id in ids }.toMutableList()
        remaining.addAll(at.coerceAtMost(remaining.size),replacement)
        edit(s.scene.copy(nodes=remaining,clips=mergeClips(s.scene.clips,bundle.clips),joints=s.scene.joints.filterNot { it.bodyA in ids && it.bodyB in ids }+bundle.joints)); select(replacement.first { it.parent==null }.id)
    }

    private fun mergeClips(old: List<AnimationClip>,incoming: List<AnimationClip>): List<AnimationClip> {
        val map=old.associateBy { it.id }.toMutableMap()
        incoming.forEach { require(map[it.id]==null || map[it.id]==it) { "Conflicting animation UUID ${it.name}" };map[it.id]=it }
        return map.values.toList()
    }
    fun parseRuntime(block: ()->Unit) { try { block() } catch(e: IllegalArgumentException) { report(e.message ?: "Invalid runtime properties") } }
    fun editRuntimeScene(scene: Scene) { if(!mutable.value.busy)edit(scene) }
    override fun animationError(message: String)=report(message)
    override fun createClip() { val clip=AnimationClip();edit(mutable.value.scene.copy(clips=mutable.value.scene.clips+clip));selectClip(clip.id) }
    override fun addAnimationPresets() {
        val s=mutable.value;val ids=s.scene.clips.map { it.id }.toSet();val presets=AnimationPresets.all().filterNot { it.id in ids }
        edit(s.scene.copy(clips=s.scene.clips+presets));if(s.selectedClip==null)selectClip((presets.firstOrNull() ?: s.scene.clips.first()).id)
    }
    override fun selectClip(id: String) { change { it.copy(selectedClip=id,animationFrame=0f,scrubScene=null) } }
    override fun updateClip(clip: AnimationClip) {
        parseRuntime { clip.validated();edit(mutable.value.scene.copy(clips=mutable.value.scene.clips.map { if(it.id==clip.id)clip else it }));change { it.copy(animationFrame=it.animationFrame.coerceAtMost((clip.frames-1).toFloat()),scrubScene=null) } }
    }
    override fun assignClip(id: String?) {
        val node=mutable.value.scene.nodes.find { it.id==mutable.value.selected } ?: return report("Select an actor first")
        updateNode(if(id==null)node.copy(components=node.components.filterNot { it is AnimatorComponent }) else node.withComponent(AnimatorComponent(id)))
    }
    override fun scrubAnimation(frame: Float) {
        parseRuntime {
            require(frame.isFinite());val s=mutable.value;val clip=s.scene.clips.find { it.id==s.selectedClip } ?: return@parseRuntime
            val f=frame.coerceIn(0f,(clip.frames-1).toFloat());val actor=s.scene.nodes.find { it.id==s.selected }
            val preview=actor?.let { s.scene.replace(AnimationSampler.sample(it,clip,f)) }
            change { it.copy(animationFrame=f,scrubScene=preview) }
        }
    }
    override fun clearScrub() { change { it.copy(scrubScene=null) } }
    fun addCamera() { val node=Node(name="Camera",components=listOf(TransformComponent(),CameraComponent(follow=mutable.value.selected)));edit(mutable.value.scene.copy(nodes=mutable.value.scene.nodes+node));select(node.id) }
    fun removeRuntimeComponent(type: Class<out Component>) { change { it.copy(removeRuntimeType=type.simpleName) } }
    fun cancelRuntimeRemoval() { change { it.copy(removeRuntimeType=null) } }
    fun confirmRuntimeRemoval() {
        val s=mutable.value;val node=s.scene.nodes.find { it.id==s.selected } ?: return
        val type=s.removeRuntimeType;cancelRuntimeRemoval()
        val updated=Prefabs.override(node.copy(components=node.components.filterNot { it.javaClass.simpleName==type || (type==RigidBodyComponent::class.java.simpleName && it is InputControllerComponent) }),"runtime")
        edit(s.scene.replace(updated).copy(joints=if(type==RigidBodyComponent::class.java.simpleName)s.scene.joints.filterNot { it.bodyA==node.id || it.bodyB==node.id } else s.scene.joints,activeCamera=if(type==CameraComponent::class.java.simpleName && s.scene.activeCamera==node.id)null else s.scene.activeCamera))
    }
    fun separateAnimatedVisual() {
        val s=mutable.value;val n=s.scene.nodes.find { it.id==s.selected } ?: return;val sprite=n.sprite ?: return
        val visual=Node(name="${n.name} visual",parent=n.id,components=listOf(TransformComponent(),sprite)+n.components.filter { it is AnimatorComponent })
        val root=n.copy(components=n.components.filterNot { it is SpriteComponent || it is AnimatorComponent }).withComponent(n.component<RigidBodyComponent>() ?: RigidBodyComponent(colliders=listOf(Collider(size=sprite.size)),fixedRotation=true))
        edit(s.scene.replace(Prefabs.override(root,"sprite","runtime")).copy(nodes=s.scene.replace(Prefabs.override(root,"sprite","runtime")).nodes+visual))
    }
    fun beginColliderDraw() { change { it.copy(drawCollider=true,colliderPoints=emptyList(),scrubScene=null) } }
    fun addColliderPoint(world: Vec2) {
        val s=mutable.value;if(!s.drawCollider || s.colliderPoints.size>=8)return
        val id=s.selected ?: return;val local=s.scene.worldMatrices().getValue(id).inverse().map(world)
        change { it.copy(colliderPoints=it.colliderPoints+local) }
    }
    fun cancelColliderDraw() { change { it.copy(drawCollider=false,colliderPoints=emptyList()) } }
    fun clearColliderPoints() { change { it.copy(colliderPoints=emptyList()) } }
    fun applyColliderDraw() {
        parseRuntime {
            val s=mutable.value;val n=s.scene.nodes.single { it.id==s.selected };val polygon=Collider(shape=ShapeKind.POLYGON,vertices=ColliderGeometry.hull(s.colliderPoints))
            val body=n.component<RigidBodyComponent>() ?: RigidBodyComponent()
            updateNode(n.withComponent(body.copy(colliders=listOf(polygon))));cancelColliderDraw()
        }
    }
    fun alphaCollider()=task {
        val s=mutable.value;val n=s.scene.nodes.single { it.id==s.selected };val sprite=n.sprite ?: error("Selected entity has no image")
        val project=s.project ?: return@task
        val ref=sprite.assetId ?: sprite.asset?.removePrefix("Sprites/")?.removeSuffix(".png")
        val record=s.assets.find { it.id==ref };val file=record?.let { assets().file(it) } ?: sprite.asset?.let { java.io.File(project.directory,it) } ?: error("Choose a textured sprite; a solid color has no alpha image")
        val collider=withContext(Dispatchers.IO) {
            val bitmap=file.inputStream().use { ImageOps.decode(it) }
            try { ColliderGeometry.alpha(bitmap,sprite.size) } finally { bitmap.recycle() }
        }
        val body=n.component<RigidBodyComponent>() ?: RigidBodyComponent()
        updateNode(n.withComponent(body.copy(colliders=listOf(collider))));note("Created an eight-vertex maximum convex alpha approximation")
    }
    fun addPhysicsTestRig() {
        val s=mutable.value
        fun body(name: String,position: Vec2,size: Vec2,kind: BodyKind,color: Color,shape: ShapeKind=ShapeKind.RECTANGLE)=Node(name=name,components=listOf(TransformComponent(Transform(position)),SpriteComponent(size,color),RigidBodyComponent(kind=kind,colliders=listOf(Collider(shape=shape,size=size,radius=size.x/2)),fixedRotation=name=="Player")))
        val ground=body("Ground",Vec2(0f,-180f),Vec2(1200f,30f),BodyKind.STATIC,Color(.3f,.4f,.45f))
        val player=body("Player",Vec2(-160f,0f),Vec2(48f,64f),BodyKind.DYNAMIC,Color(.3f,.7f,1f)).withComponent(InputControllerComponent())
        val ball=body("Ball",Vec2(100f,80f),Vec2(40f,40f),BodyKind.DYNAMIC,Color(1f,.5f,.2f),ShapeKind.CIRCLE)
        val capsule=body("Capsule",Vec2(30f,40f),Vec2(32f,70f),BodyKind.DYNAMIC,Color(.5f,.9f,.4f),ShapeKind.CAPSULE)
        val anchor=body("Pendulum anchor",Vec2(240f,180f),Vec2(12f,12f),BodyKind.STATIC,Color(1f,1f,.3f),ShapeKind.CIRCLE)
        val bob=body("Pendulum bob",Vec2(300f,60f),Vec2(40f,40f),BodyKind.DYNAMIC,Color(1f,.3f,.5f),ShapeKind.CIRCLE)
        val sensor=body("Finish sensor",Vec2(420f,-110f),Vec2(50f,100f),BodyKind.STATIC,Color(.2f,.9f,.8f,.35f)).withComponent(RigidBodyComponent(kind=BodyKind.STATIC,colliders=listOf(Collider(size=Vec2(50f,100f),sensor=true))))
        val camera=Node(name="Follow camera",components=listOf(TransformComponent(),CameraComponent(follow=player.id)))
        edit(s.scene.copy(nodes=s.scene.nodes+listOf(ground,player,ball,capsule,anchor,bob,sensor,camera),joints=s.scene.joints+JointSpec(kind=JointKind.DISTANCE,bodyA=anchor.id,bodyB=bob.id,length=134.16f),gravity=Vec2(0f,-980f),inputMap=InputMap(),activeCamera=camera.id));select(player.id)
    }
    fun play() {
        if(mutable.value.playId!=null) { resumePlay();return }
        task {
            finishDrag();cancelColliderDraw();clearScrub();val authored=mutable.value.scene
            val next=withContext(Dispatchers.Default) { PreviewSession(authored,::note) }
            next.resize(viewportSize.first,viewportSize.second)
            val first=withContext(Dispatchers.Default) { next.advance(0f) };val token=++playSerial;session=next;stepRequests.set(0)
            change { it.copy(playId=token,paused=!foreground,preview=first) }
            playJob=viewModelScope.launch(Dispatchers.Default) {
                var time=System.nanoTime();var wasPaused=true
                try {
                    while(isActive && mutable.value.playId==token) {
                        val now=System.nanoTime();val elapsed=(now-time)/1_000_000_000f;time=now
                        val paused=mutable.value.paused;val requested=stepRequests.getAndSet(0)
                        var frame: PreviewFrame?=null
                        if(!paused)frame=next.advance(if(wasPaused)0f else elapsed)
                        else repeat(requested.coerceAtMost(8)) { frame=next.advance(PhysicsWorld.STEP) }
                        frame?.let { result -> change { if(it.playId==token)it.copy(preview=result) else it } }
                        wasPaused=paused;delay(if(paused)100 else 16)
                    }
                } catch(e: CancellationException) { throw e }
                catch(e: Exception) { if(session===next)session=null;change { if(it.playId==token)it.copy(playId=null,preview=null,paused=false) else it };report("Play stopped: ${e.message}. Your authored scene is unchanged.") }
            }
        }
    }
    fun pausePlay() { session?.input?.releaseAll();if(mutable.value.playId!=null)change { it.copy(paused=true) } }
    fun resumePlay() { if(foreground)change { it.copy(paused=false) } }
    fun stopPlay() { playSerial++;playJob?.cancel();playJob=null;session?.input?.releaseAll();session=null;stepRequests.set(0);change { it.copy(playId=null,preview=null,paused=false) } }
    fun stepPlay() { if(mutable.value.paused)stepRequests.updateAndGet { (it+1).coerceAtMost(8) } }
    fun editorForeground(active: Boolean) { foreground=active;if(!active)pausePlay() }
    fun viewportResize(width: Int,height: Int) { viewportSize=width to height;session?.resize(width,height) }
    fun inputPhysical(source: InputSource,code: Int,value: Float,device: Int) { if(!mutable.value.paused || value==0f)session?.input?.physical(source,code,value,device) }
    fun inputTouch(action: String,value: Float) { if(!mutable.value.paused || value==0f)session?.input?.touch(action,value) }
    fun releaseDeviceInput(device: Int) { session?.input?.releaseDevice(device) }
    fun releaseInput() { session?.input?.releaseAll() }
    fun shakeCamera(preset: ShakePreset) { session?.shake(preset) }
    fun focusCamera(id: String?) { session?.focusCamera(id) }
}
````

## editor-ui/src/main/kotlin/world/engine/editor/PlayControls.kt

````kotlin
package world.engine.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import world.engine.core.*
import world.engine.math.Vec2
import world.engine.render.ShakePreset

@Composable internal fun PlayInputOverlay(scene: Scene,selected: String?,vm: EditorViewModel,modifier: Modifier=Modifier) {
    val actor=scene.nodes.find { it.id==selected && it.component<InputControllerComponent>()!=null } ?: scene.nodes.firstOrNull { it.component<InputControllerComponent>()!=null }
    val control=actor?.component<InputControllerComponent>() ?: InputControllerComponent()
    val actions=scene.inputMap.bindings.filter { it.source==InputSource.TOUCH }.map { it.action }.distinct()
    Row(modifier.padding(12.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.SpaceBetween) {
        if(control.horizontal in actions || control.vertical in actions)Joystick { vector -> vm.inputTouch(control.horizontal,vector.x);vm.inputTouch(control.vertical,vector.y) }
        Spacer(Modifier.weight(1f))
        Row(Modifier.weight(2f).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            actions.filterNot { it==control.horizontal || it==control.vertical }.forEach { action -> HeldButton(action){value -> vm.inputTouch(action,value)} }
        }
    }
    DisposableEffect(Unit){onDispose { vm.releaseInput() }}
}
@Composable private fun Joystick(move: (Vec2)->Unit) {
    var stick by remember { mutableStateOf(Offset.Zero) }
    val currentMove by rememberUpdatedState(move)
    Canvas(Modifier.size(132.dp).semantics { contentDescription="Virtual joystick. Drag to move; keyboard and gamepad mappings are also supported." }.pointerInput(Unit) {
        fun update(position: Offset) { val center=Offset(size.width/2f,size.height/2f);val delta=(position-center)/(minOf(size.width,size.height)*.4f);val length=delta.getDistance();stick=if(length>1f)delta/length else delta;currentMove(Vec2(stick.x,-stick.y)) }
        detectDragGestures(onDragStart={update(it)},onDragEnd={stick=Offset.Zero;currentMove(Vec2())},onDragCancel={stick=Offset.Zero;currentMove(Vec2())},onDrag={change,_->change.consume();update(change.position)})
    }) {
        drawCircle(Color(0x99445566),size.minDimension*.47f)
        drawCircle(Color(0xcc88bbff),size.minDimension*.19f,center+stick*(size.minDimension*.3f))
    }
}
@Composable private fun HeldButton(label: String,change: (Float)->Unit) {
    var pressed by remember { mutableStateOf(false) };val scope=rememberCoroutineScope();val currentChange by rememberUpdatedState(change)
    Surface(color=if(pressed)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,shape=MaterialTheme.shapes.large,modifier=Modifier.sizeIn(minWidth=76.dp,minHeight=64.dp).semantics {
        role=Role.Button;contentDescription=label
        onClick { scope.launch { currentChange(1f);delay(80);currentChange(0f) };true }
    }.pointerInput(label) {
        awaitEachGesture {
            awaitFirstDown().consume();pressed=true;currentChange(1f)
            try { waitForUpOrCancellation() } finally { pressed=false;currentChange(0f) }
        }
    }) { Box(contentAlignment=Alignment.Center,modifier=Modifier.padding(12.dp)){Text(label)} }
    DisposableEffect(label){onDispose { currentChange(0f) }}
}
@Composable internal fun PlayStatus(s: EditorState,vm: EditorViewModel) {
    var preset by remember { mutableStateOf(ShakePreset.SMALL) }
    Column(Modifier.fillMaxWidth().padding(horizontal=8.dp)) {
        val frame=s.preview
        Text("${if(s.paused)"PAUSED" else "PLAY"} · step ${frame?.steps ?: 0} · bodies ${frame?.bodies ?: 0} · joints ${frame?.joints ?: 0} · dropped ${"%.3f".format(frame?.droppedSeconds ?: 0f)} s",style=MaterialTheme.typography.labelSmall)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            RuntimeChoice("Shake",preset.name,ShakePreset.entries.map { it.name }){preset=ShakePreset.valueOf(it)}
            TextButton(enabled=!s.paused,onClick={vm.shakeCamera(preset)}){Text("Shake camera")}
            TextButton(enabled=!s.paused,onClick={vm.focusCamera(null)}){Text("All cameras")}
            s.scene.nodes.filter { it.component<CameraComponent>()?.enabled==true }.forEach { camera -> TextButton(enabled=!s.paused,onClick={vm.focusCamera(camera.id)}){Text(camera.name)} }
        }
        Text(frame?.input?.entries?.joinToString(" · ") { "${it.key}=${"%.1f".format(it.value)}" } ?: "",style=MaterialTheme.typography.labelSmall)
    }
}
````

## editor-ui/src/main/kotlin/world/engine/editor/RuntimePanels.kt

````kotlin
package world.engine.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import world.engine.core.*
import world.engine.math.*

/** Touch-first authoring forms. Every Apply routes through scene validation and undo history. */
@Composable internal fun RuntimeInspector(scene: Scene,node: Node?,vm: EditorViewModel,draw: ()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("PHYSICS / CAMERA / INPUT ACTOR",style=MaterialTheme.typography.titleMedium)
        if(node==null) { Text("Select an entity in the hierarchy first.");return@Column }
        Text(node.name,style=MaterialTheme.typography.titleLarge)
        val body=node.component<RigidBodyComponent>()
        if(body==null) {
            Button(onClick={vm.updateNode(node.withComponent(RigidBodyComponent(colliders=listOf(Collider(size=node.sprite?.size ?: Vec2(96f,96f))))))}){Text("Add rigid body")}
            if(node.sprite!=null)TextButton(onClick=vm::separateAnimatedVisual){Text("Create physics parent + visual child")}
        } else BodyEditor(node,body,vm,draw)
        val controller=node.component<InputControllerComponent>()
        if(controller==null)TextButton(enabled=body?.kind!=BodyKind.STATIC,onClick={vm.updateNode(node.withComponent(InputControllerComponent(kind=if(body?.kind==BodyKind.DYNAMIC)ControllerKind.PLATFORMER else ControllerKind.TOP_DOWN)))}){Text("Add input controller")}
        else key(node.id,controller) {
            var speed by remember { mutableStateOf(controller.speed.toString()) };var jump by remember { mutableStateOf(controller.jumpSpeed.toString()) }
            var horizontal by remember { mutableStateOf(controller.horizontal) };var vertical by remember { mutableStateOf(controller.vertical) };var jumpAction by remember { mutableStateOf(controller.jump) }
            Text("Input controller")
            RuntimeChoice("Mode",controller.kind.name,(if(body?.kind==BodyKind.DYNAMIC)ControllerKind.entries else listOf(ControllerKind.TOP_DOWN)).map { it.name }){vm.updateNode(node.withComponent(controller.copy(kind=ControllerKind.valueOf(it))))}
            RuntimeField("Speed (pixels/second)",speed){speed=it};if(controller.kind==ControllerKind.PLATFORMER)RuntimeField("Jump speed",jump){jump=it}
            RuntimeField("Horizontal action",horizontal){horizontal=it};if(controller.kind==ControllerKind.TOP_DOWN)RuntimeField("Vertical action",vertical){vertical=it};if(controller.kind==ControllerKind.PLATFORMER)RuntimeField("Jump action",jumpAction){jumpAction=it}
            TextButton(onClick={vm.parseRuntime { vm.updateNode(node.withComponent(controller.copy(speed=speed.toFloat(),jumpSpeed=jump.toFloat(),horizontal=horizontal,vertical=vertical,jump=jumpAction))) }}){Text("Apply controller")}
            Text("Platformer jump requires a touching, non-sensor support contact. For top-down rigid bodies set gravity scale to zero.",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={vm.removeRuntimeComponent(InputControllerComponent::class.java)}){Text("Remove controller")}
        }
        val camera=node.component<CameraComponent>()
        if(camera==null)TextButton(onClick={vm.updateNode(node.withComponent(CameraComponent()))}){Text("Add camera")}
        else CameraEditor(scene,node,camera,vm)
        val parallax=node.component<ParallaxComponent>()
        if(node.sprite!=null && body==null) {
            if(parallax==null)TextButton(onClick={vm.updateNode(node.withComponent(ParallaxComponent()))}){Text("Add parallax layer")}
            else key(node.id,parallax) {
                var x by remember { mutableStateOf(parallax.factor.x.toString()) };var y by remember { mutableStateOf(parallax.factor.y.toString()) }
                RuntimeField("Parallax X (0 screen-fixed, 1 world)",x){x=it};RuntimeField("Parallax Y",y){y=it}
                TextButton(onClick={vm.parseRuntime { vm.updateNode(node.withComponent(ParallaxComponent(Vec2(x.toFloat(),y.toFloat())))) }}){Text("Apply parallax")}
                TextButton(onClick={vm.removeRuntimeComponent(ParallaxComponent::class.java)}){Text("Remove parallax")}
            }
        }
        node.component<AnimatorComponent>()?.let { animator -> key(node.id,animator) {
            var speed by remember { mutableStateOf(animator.speed.toString()) };RuntimeField("Animator speed 0.01–10",speed){speed=it}
            TextButton(onClick={vm.parseRuntime { vm.updateNode(node.withComponent(animator.copy(speed=speed.toFloat()))) }}){Text("Apply animator speed")}
        } }
    }
}
@Composable private fun BodyEditor(node: Node,body: RigidBodyComponent,vm: EditorViewModel,draw: ()->Unit) {
    key(node.id,body) {
        var gravity by remember { mutableStateOf(body.gravityScale.toString()) };var damping by remember { mutableStateOf(body.linearDamping.toString()) }
        var vx by remember { mutableStateOf(body.velocity.x.toString()) };var vy by remember { mutableStateOf(body.velocity.y.toString()) };var angular by remember { mutableStateOf(body.angularVelocity.toString()) }
        RuntimeChoice("Body",body.kind.name,BodyKind.entries.map { it.name }){vm.updateNode(node.withComponent(body.copy(kind=BodyKind.valueOf(it))))}
        if(body.kind==BodyKind.DYNAMIC) {
            Toggle("Fixed rotation",body.fixedRotation){vm.updateNode(node.withComponent(body.copy(fixedRotation=it)))}
            Toggle("Bullet / continuous collision",body.bullet){vm.updateNode(node.withComponent(body.copy(bullet=it)))}
            RuntimeField("Gravity scale",gravity){gravity=it};RuntimeField("Linear damping",damping){damping=it}
        }
        if(body.kind!=BodyKind.STATIC) {
            RuntimeField("Initial velocity X",vx){vx=it};RuntimeField("Initial velocity Y",vy){vy=it};RuntimeField("Angular velocity degrees/sec",angular){angular=it}
        }
        if(body.kind!=BodyKind.STATIC)TextButton(onClick={vm.parseRuntime { vm.updateNode(node.withComponent(body.copy(gravityScale=gravity.toFloat(),linearDamping=damping.toFloat(),velocity=Vec2(vx.toFloat(),vy.toFloat()),angularVelocity=angular.toFloat()))) }}){Text("Apply body settings")}
        body.colliders.forEachIndexed { index,collider -> ColliderEditor(index,collider,body.colliders.size>1,{ updated -> vm.updateNode(node.withComponent(body.copy(colliders=body.colliders.mapIndexed { i,c -> if(i==index)updated else c }))) },{vm.updateNode(node.withComponent(body.copy(colliders=body.colliders.filterIndexed { i,_->i!=index })))},vm) }
        TextButton(enabled=body.colliders.size<32,onClick={vm.updateNode(node.withComponent(body.copy(colliders=body.colliders+Collider(size=node.sprite?.size ?: Vec2(48f,48f)))))}){Text("Add compound part")}
        TextButton(onClick=draw){Text("Draw convex polygon in viewport")}
        TextButton(enabled=node.sprite!=null,onClick=vm::alphaCollider){Text("Replace colliders with alpha hull")}
        TextButton(onClick={vm.removeRuntimeComponent(RigidBodyComponent::class.java)}){Text("Remove body, controller and joints")}
    }
}
@Composable private fun ColliderEditor(index: Int,c: Collider,removable: Boolean,apply: (Collider)->Unit,remove: ()->Unit,vm: EditorViewModel) {
    key(index,c) {
        var shape by remember { mutableStateOf(c.shape) };var width by remember { mutableStateOf(c.size.x.toString()) };var height by remember { mutableStateOf(c.size.y.toString()) };var radius by remember { mutableStateOf(c.radius.toString()) }
        var x by remember { mutableStateOf(c.offset.x.toString()) };var y by remember { mutableStateOf(c.offset.y.toString()) };var density by remember { mutableStateOf(c.density.toString()) };var friction by remember { mutableStateOf(c.friction.toString()) };var bounce by remember { mutableStateOf(c.restitution.toString()) }
        var vertices by remember { mutableStateOf(c.vertices.joinToString("; ") { "${it.x},${it.y}" }) };var sensor by remember { mutableStateOf(c.sensor) }
        Text("Collider ${index+1}",style=MaterialTheme.typography.titleMedium)
        RuntimeChoice("Shape",shape.name,ShapeKind.entries.map { it.name }){shape=ShapeKind.valueOf(it)}
        if(shape==ShapeKind.RECTANGLE || shape==ShapeKind.CAPSULE){RuntimeField("Width",width){width=it};RuntimeField("Height (capsule ≥ width)",height){height=it}};if(shape==ShapeKind.CIRCLE)RuntimeField("Circle radius",radius){radius=it}
        RuntimeField("Offset X",x){x=it};RuntimeField("Offset Y",y){y=it}
        if(shape==ShapeKind.POLYGON)RuntimeField("Convex vertices: x,y; x,y; x,y",vertices){vertices=it}
        RuntimeField("Density kg/m²",density){density=it};RuntimeField("Friction",friction){friction=it};RuntimeField("Restitution 0–1",bounce){bounce=it};Toggle("Sensor (no collision response)",sensor){sensor=it}
        TextButton(onClick={vm.parseRuntime {
            val points=if(shape==ShapeKind.POLYGON)vertices.split(';').map { pair -> val v=pair.trim().split(',');require(v.size==2);Vec2(v[0].trim().toFloat(),v[1].trim().toFloat()) } else emptyList()
            apply(c.copy(shape=shape,size=if(shape==ShapeKind.RECTANGLE || shape==ShapeKind.CAPSULE)Vec2(width.toFloat(),height.toFloat()) else c.size,radius=if(shape==ShapeKind.CIRCLE)radius.toFloat() else c.radius,offset=Vec2(x.toFloat(),y.toFloat()),density=density.toFloat(),friction=friction.toFloat(),restitution=bounce.toFloat(),sensor=sensor,vertices=points))
        }}){Text("Apply collider")}
        TextButton(enabled=removable,onClick=remove){Text("Remove collider")}
    }
}
@Composable private fun CameraEditor(scene: Scene,node: Node,c: CameraComponent,vm: EditorViewModel) {
    key(node.id,c) {
        var follow by remember { mutableStateOf(c.follow) };var smoothing by remember { mutableStateOf(c.smoothing.toString()) };var zoom by remember { mutableStateOf(c.zoom.toString()) };var rotation by remember { mutableStateOf(c.rotation.toString()) }
        var dx by remember { mutableStateOf(c.deadZone.x.toString()) };var dy by remember { mutableStateOf(c.deadZone.y.toString()) }
        var minX by remember { mutableStateOf(c.limitMin?.x?.toString() ?: "") };var minY by remember { mutableStateOf(c.limitMin?.y?.toString() ?: "") };var maxX by remember { mutableStateOf(c.limitMax?.x?.toString() ?: "") };var maxY by remember { mutableStateOf(c.limitMax?.y?.toString() ?: "") }
        var viewport by remember { mutableStateOf(c.viewport) }
        Text("Camera")
        EntityChoice("Follow",follow,scene.nodes,true){follow=it}
        RuntimeField("Smoothing seconds (0 instant)",smoothing){smoothing=it};RuntimeField("Zoom",zoom){zoom=it};RuntimeField("Additional rotation degrees",rotation){rotation=it}
        if(follow!=null){RuntimeField("Dead-zone width",dx){dx=it};RuntimeField("Dead-zone height",dy){dy=it}}
        RuntimeField("Limit minimum X (empty = no limits)",minX){minX=it};RuntimeField("Limit minimum Y",minY){minY=it};RuntimeField("Limit maximum X",maxX){maxX=it};RuntimeField("Limit maximum Y",maxY){maxY=it}
        RuntimeChoice("Viewport","${viewport.x},${viewport.y},${viewport.width},${viewport.height}",listOf("Full","Left half","Right half","Top half","Bottom half")){viewport=when(it){"Left half"->CameraViewport(width=.5f);"Right half"->CameraViewport(x=.5f,width=.5f);"Top half"->CameraViewport(height=.5f);"Bottom half"->CameraViewport(y=.5f,height=.5f);else->CameraViewport()}}
        Toggle("Enabled",c.enabled){vm.updateNode(node.withComponent(c.copy(enabled=it)))}
        TextButton(onClick={vm.parseRuntime { val limited=listOf(minX,minY,maxX,maxY).any { it.isNotBlank() };vm.updateNode(node.withComponent(c.copy(follow=follow,smoothing=smoothing.toFloat(),zoom=zoom.toFloat(),rotation=rotation.toFloat(),deadZone=Vec2(dx.toFloat(),dy.toFloat()),limitMin=if(limited)Vec2(minX.toFloat(),minY.toFloat()) else null,limitMax=if(limited)Vec2(maxX.toFloat(),maxY.toFloat()) else null,viewport=viewport))) }}){Text("Apply camera")}
        TextButton(onClick={vm.editRuntimeScene(scene.copy(activeCamera=node.id))}){Text("Use as active camera")}
        TextButton(onClick={vm.editRuntimeScene(scene.copy(activeCamera=null))}){Text("Render all enabled cameras")}
        TextButton(onClick={vm.removeRuntimeComponent(CameraComponent::class.java)}){Text("Remove camera")}
    }
}
@Composable internal fun InputMapEditor(scene: Scene,vm: EditorViewModel) {
    var selected by remember { mutableIntStateOf(-1) };var action by remember { mutableStateOf("MoveX") };var source by remember { mutableStateOf(InputSource.KEY) };var code by remember { mutableStateOf("29") };var scale by remember { mutableStateOf("-1") };var dead by remember { mutableStateOf("0.15") }
    var listening by remember { mutableStateOf(false) };val focus=remember { FocusRequester() }
    LaunchedEffect(listening){if(listening)focus.requestFocus()}
    Column(Modifier.fillMaxSize().focusRequester(focus).onPreviewKeyEvent { e ->
        if(listening && e.nativeKeyEvent.action==android.view.KeyEvent.ACTION_DOWN) { code=e.nativeKeyEvent.keyCode.toString();source=InputSource.KEY;listening=false;true } else false
    }.focusable().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("INPUT MAP",style=MaterialTheme.typography.titleMedium)
        RuntimeField("Action name",action){action=it};RuntimeChoice("Source",source.name,InputSource.entries.map { it.name }){source=InputSource.valueOf(it)}
        if(source!=InputSource.TOUCH)RuntimeField("Android code / axis / mouse button",code){code=it};RuntimeField("Scale (-1 reverses)",scale){scale=it};if(source==InputSource.GAMEPAD_AXIS)RuntimeField("Axis dead zone 0–0.95",dead){dead=it}
        Text("Gamepad X=0, Y=1, hat X=15/Y=16. Mouse primary=1, secondary=2, middle=4. Touch uses the action name.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={listening=!listening}){Text(if(listening)"Listening… press a key (tap to cancel)" else "Listen for key / gamepad button")}
        TextButton(onClick={vm.parseRuntime {
            val binding=InputBinding(action,source,if(source==InputSource.TOUCH)0 else code.toInt(),scale.toFloat(),if(source==InputSource.GAMEPAD_AXIS)dead.toFloat() else .15f)
            val bindings=scene.inputMap.bindings.toMutableList();if(selected in bindings.indices)bindings[selected]=binding else bindings.add(binding)
            vm.editRuntimeScene(scene.copy(inputMap=InputMap(bindings)));selected=-1
        }}){Text(if(selected>=0)"Update binding" else "Add binding")}
        if(selected>=0)TextButton(onClick={selected=-1}){Text("Cancel binding edit")}
        scene.inputMap.bindings.forEachIndexed { index,b -> Row {
            TextButton(modifier=Modifier.weight(1f),onClick={selected=index;action=b.action;source=b.source;code=b.code.toString();scale=b.scale.toString();dead=b.deadZone.toString()}){Text("${b.action}: ${b.source} ${b.code} × ${b.scale}")}
            TextButton(onClick={selected=-1;vm.editRuntimeScene(scene.copy(inputMap=InputMap(scene.inputMap.bindings.filterIndexed { i,_->i!=index })))}){Text("Remove")}
        } }
    }
}
@Composable internal fun JointEditor(scene: Scene,vm: EditorViewModel) {
    val bodies=scene.nodes.filter { it.component<RigidBodyComponent>()!=null }
    var selected by remember { mutableStateOf<String?>(null) };var kind by remember { mutableStateOf(JointKind.DISTANCE) };var a by remember { mutableStateOf<String?>(null) };var b by remember { mutableStateOf<String?>(null) }
    var ax by remember { mutableStateOf("0") };var ay by remember { mutableStateOf("0") };var bx by remember { mutableStateOf("0") };var by by remember { mutableStateOf("0") };var axisX by remember { mutableStateOf("1") };var axisY by remember { mutableStateOf("0") }
    var length by remember { mutableStateOf("100") };var frequency by remember { mutableStateOf("4") };var damping by remember { mutableStateOf("0.7") };var motor by remember { mutableStateOf(false) };var speed by remember { mutableStateOf("0") };var force by remember { mutableStateOf("100") };var limit by remember { mutableStateOf(false) };var lower by remember { mutableStateOf("-45") };var upper by remember { mutableStateOf("45") };var collide by remember { mutableStateOf(false) }
    var removal by remember { mutableStateOf<JointSpec?>(null) }
    removal?.let { joint -> AlertDialog(onDismissRequest={removal=null},title={Text("Remove joint?")},text={Text("Remove this ${joint.kind} constraint? This is undoable.")},confirmButton={TextButton(onClick={selected=null;vm.editRuntimeScene(scene.copy(joints=scene.joints.filterNot { it.id==joint.id }));removal=null}){Text("Remove")}},dismissButton={TextButton(onClick={removal=null}){Text("Cancel")}}) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("JOINTS",style=MaterialTheme.typography.titleMedium)
        RuntimeChoice("Type",kind.name,JointKind.entries.map { it.name }){kind=JointKind.valueOf(it)}
        EntityChoice("Body A",a,bodies,false){a=it};EntityChoice("Body B",b,bodies,false){b=it}
        RuntimeField("Local anchor A X",ax){ax=it};RuntimeField("Local anchor A Y",ay){ay=it};RuntimeField("Local anchor B X",bx){bx=it};RuntimeField("Local anchor B Y",by){by=it}
        if(kind in listOf(JointKind.PRISMATIC,JointKind.WHEEL)){RuntimeField("Axis X (prismatic/wheel)",axisX){axisX=it};RuntimeField("Axis Y",axisY){axisY=it}}
        if(kind in listOf(JointKind.DISTANCE,JointKind.SPRING,JointKind.ROPE))RuntimeField("Length / rope max (pixels)",length){length=it};if(kind in listOf(JointKind.SPRING,JointKind.WHEEL)){RuntimeField("Spring / wheel frequency Hz",frequency){frequency=it};RuntimeField("Damping ratio",damping){damping=it}}
        Toggle("Collide connected",collide){collide=it}
        if(kind in listOf(JointKind.REVOLUTE,JointKind.PRISMATIC,JointKind.WHEEL)) {
            Toggle("Motor",motor){motor=it};RuntimeField("Motor speed (degrees/sec, prismatic pixels/sec)",speed){speed=it};RuntimeField("Max motor force/torque (SI)",force){force=it}
        }
        if(kind in listOf(JointKind.REVOLUTE,JointKind.PRISMATIC)) { Toggle("Limits",limit){limit=it};RuntimeField("Lower (degrees, prismatic pixels)",lower){lower=it};RuntimeField("Upper",upper){upper=it} }
        TextButton(onClick={vm.parseRuntime {
            val joint=JointSpec(id=selected ?: java.util.UUID.randomUUID().toString(),kind=kind,bodyA=requireNotNull(a){"Choose body A"},bodyB=requireNotNull(b){"Choose body B"},anchorA=Vec2(ax.toFloat(),ay.toFloat()),anchorB=Vec2(bx.toFloat(),by.toFloat()),axis=Vec2(axisX.toFloat(),axisY.toFloat()),length=length.toFloat(),frequency=frequency.toFloat(),damping=damping.toFloat(),collideConnected=collide,motor=motor,motorSpeed=speed.toFloat(),maxMotorForce=force.toFloat(),limit=limit,lower=lower.toFloat(),upper=upper.toFloat())
            vm.editRuntimeScene(scene.copy(joints=scene.joints.filterNot { it.id==joint.id }+joint));selected=null
        }}){Text(if(selected==null)"Create joint" else "Update joint")}
        if(selected!=null)TextButton(onClick={selected=null}){Text("Cancel edit")}
        scene.joints.forEach { j -> Row {
            TextButton(modifier=Modifier.weight(1f),onClick={selected=j.id;kind=j.kind;a=j.bodyA;b=j.bodyB;ax=j.anchorA.x.toString();ay=j.anchorA.y.toString();bx=j.anchorB.x.toString();by=j.anchorB.y.toString();axisX=j.axis.x.toString();axisY=j.axis.y.toString();length=j.length.toString();frequency=j.frequency.toString();damping=j.damping.toString();motor=j.motor;speed=j.motorSpeed.toString();force=j.maxMotorForce.toString();limit=j.limit;lower=j.lower.toString();upper=j.upper.toString();collide=j.collideConnected}){Text("${j.kind}: ${bodies.find { it.id==j.bodyA }?.name} ↔ ${bodies.find { it.id==j.bodyB }?.name}")}
            TextButton(onClick={removal=j}){Text("Remove")}
        } }
    }
}
@Composable internal fun RuntimeField(label: String,value: String,change: (String)->Unit) { OutlinedTextField(value,change,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth()) }
@Composable internal fun RuntimeChoice(label: String,value: String,choices: List<String>,choose: (String)->Unit) {
    var open by remember { mutableStateOf(false) }
    Box { TextButton(onClick={open=true}){Text("$label: $value")};DropdownMenu(open,onDismissRequest={open=false},modifier=Modifier.heightIn(max=320.dp)){choices.forEach { item -> DropdownMenuItem(text={Text(item)},onClick={open=false;choose(item)})}} }
}
@Composable private fun EntityChoice(label: String,value: String?,nodes: List<Node>,allowNone: Boolean,choose: (String?)->Unit) {
    var open by remember { mutableStateOf(false) }
    Box { TextButton(onClick={open=true}){Text("$label: ${nodes.find { it.id==value }?.name ?: "None"}")};DropdownMenu(open,onDismissRequest={open=false},modifier=Modifier.heightIn(max=320.dp)) {
        if(allowNone)DropdownMenuItem(text={Text("None")},onClick={open=false;choose(null)})
        nodes.forEach { n -> DropdownMenuItem(text={Text(n.name)},onClick={open=false;choose(n.id)}) }
    } }
}
@Composable private fun Toggle(label: String,value: Boolean,change: (Boolean)->Unit) { Row { Checkbox(value,change);Text(label,Modifier.padding(top=12.dp)) } }
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import world.engine.physics.ColliderGeometry
import world.engine.animationeditor.TimelineEditor
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
        val observer=LifecycleEventObserver { _,event -> when(event) { Lifecycle.Event.ON_STOP -> vm.backgroundSave();Lifecycle.Event.ON_PAUSE -> vm.editorForeground(false);Lifecycle.Event.ON_RESUME -> vm.editorForeground(true);else -> Unit } }
        owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
    }
    BackHandler(s.project!=null && !s.busy) { if(s.playId!=null)vm.stopPlay() else if(s.drawCollider)vm.cancelColliderDraw() else if(s.recovery==null && s.recoveryIssue==null)exit=true }
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
        s.removeRuntimeType?.let { type -> AlertDialog(onDismissRequest=vm::cancelRuntimeRemoval,title={Text("Remove $type?")},text={Text("Removing a body also removes its connected joints and movement controller. This scene edit can be undone.")},confirmButton={TextButton(onClick=vm::confirmRuntimeRemoval){Text("Remove")}},dismissButton={TextButton(onClick=vm::cancelRuntimeRemoval){Text("Cancel")}}) }
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

@Composable private fun EditorShell(s: EditorState,vm: EditorViewModel,exit: ()->Unit) {
    val project=s.project ?: return
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    val viewport=remember(project.manifest.id){WorldViewport(context)}
    var delete by remember { mutableStateOf(false) };var panel by remember { mutableStateOf("Hierarchy") }
    var reset by remember { mutableIntStateOf(0) };var systems by remember { mutableStateOf(false) };var debug by remember { mutableStateOf(false) }
    val playing=s.playId!=null;val editable=!s.busy && !playing && !s.drawCollider
    val displayed=s.preview?.scene ?: s.scrubScene ?: s.scene
    val paths=remember(s.assets){s.assets.filter { it.kind==world.engine.assets.AssetKind.IMAGE }.associate { it.id to it.path }}
    val images=remember(s.assets){s.assets.filter { it.kind==world.engine.assets.AssetKind.IMAGE }.associate { it.id to it.name }}
    val lines=remember(displayed,debug,s.preview,s.colliderPoints,s.drawCollider,s.selected) {
        val result=if(debug) (s.preview?.lines ?: ColliderGeometry.outlines(displayed)).toMutableList() else mutableListOf()
        if(s.drawCollider && s.selected!=null) {
            val matrix=s.scene.worldMatrices().getValue(s.selected)
            val points=s.colliderPoints.map(matrix::map)
            points.forEach { point -> result.add(DebugLine(point-Vec2(4f,0f),point+Vec2(4f,0f),world.engine.math.Color(1f,1f,0f)));result.add(DebugLine(point-Vec2(0f,4f),point+Vec2(0f,4f),world.engine.math.Color(1f,1f,0f))) }
            points.zipWithNext().forEach { (a,b)->result.add(DebugLine(a,b,world.engine.math.Color(1f,1f,0f))) }
        }
        result.toList()
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(vm::importSprite)}
    DisposableEffect(viewport,owner) {
        val observer=LifecycleEventObserver { _,event -> when(event){Lifecycle.Event.ON_RESUME->viewport.onResume();Lifecycle.Event.ON_PAUSE->viewport.onPause();else->Unit} }
        owner.lifecycle.addObserver(observer);if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))viewport.onResume()
        onDispose { owner.lifecycle.removeObserver(observer);viewport.onPause();vm.releaseInput() }
    }
    LaunchedEffect(reset){viewport.resetCamera()}
    Column(Modifier.fillMaxSize()) {
        Text(project.manifest.name+if(playing)" • nondestructive play" else if(s.dirty)" • unsaved" else " • saved",Modifier.padding(horizontal=12.dp,vertical=4.dp),style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            if(!playing)TextButton(enabled=editable,onClick=vm::play){Text("Play")}
            else {
                TextButton(onClick={if(s.paused)vm.resumePlay() else vm.pausePlay()}){Text(if(s.paused)"Resume" else "Pause")}
                TextButton(enabled=s.paused,onClick=vm::stepPlay){Text("Step")};TextButton(onClick=vm::stopPlay){Text("Stop")}
            }
            TextButton(enabled=editable,onClick=exit){Text("Projects")};TextButton(enabled=editable,onClick=vm::save){Text("Save")}
            TextButton(enabled=editable && s.canUndo,onClick=vm::undo){Text("Undo")};TextButton(enabled=editable && s.canRedo,onClick=vm::redo){Text("Redo")}
            TextButton(enabled=editable,onClick=vm::addSprite){Text("Add sprite")};TextButton(enabled=editable,onClick={picker.launch(arrayOf("image/*"))}){Text("Import image")}
            TextButton(enabled=editable,onClick={systems=true}){Text("Systems")};TextButton(enabled=editable,onClick={panel="Animation"}){Text("Animation")}
            TextButton(enabled=editable,onClick={panel="Assets"}){Text("Assets")};TextButton(onClick={debug=!debug}){Text(if(debug)"Colliders on" else "Colliders off")}
            TextButton(enabled=editable,onClick={reset++}){Text("Reset view")}
        }
        if(s.drawCollider)Row(Modifier.horizontalScroll(rememberScrollState())) {
            Text("Polygon ${s.colliderPoints.size}/8",Modifier.padding(12.dp))
            TextButton(enabled=s.colliderPoints.size>=3,onClick=vm::applyColliderDraw){Text("Apply polygon")}
            TextButton(onClick=vm::clearColliderPoints){Text("Clear points")};TextButton(onClick=vm::cancelColliderDraw){Text("Cancel drawing")}
        }
        if(playing)PlayStatus(s,vm)
        BoxWithConstraints(Modifier.weight(1f)) {
            val wide=maxWidth>=840.dp
            val panelHeight=(maxHeight*.5f).coerceAtMost(360.dp)
            Column {
                Row(Modifier.weight(1f)) {
                    if(wide && !playing)Box(Modifier.width(190.dp).fillMaxHeight()){Hierarchy(s,vm)}
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        AndroidView(factory={viewport},modifier=Modifier.fillMaxSize(),update={view ->
                            view.onSelect=vm::select;view.onMove=vm::move;view.onError=vm::report;view.onAssetDrop=vm::placeAsset
                            view.onColliderPoint=vm::addColliderPoint;view.onViewportSize=vm::viewportResize;view.onPhysicalInput=vm::inputPhysical;view.onReleaseInput=vm::releaseInput;view.onDeviceRemoved=vm::releaseDeviceInput
                            view.onContext={id -> vm.select(id);if(id!=null){panel="Inspector";delete=true}}
                            view.isEnabled=!s.busy && s.recovery==null && s.recoveryIssue==null
                            view.update(displayed,s.selected,project.directory,paths,playing,s.drawCollider,s.preview?.views.orEmpty(),lines)
                        })
                        if(playing && !s.paused)PlayInputOverlay(s.scene,s.selected,vm,Modifier.align(Alignment.BottomCenter).fillMaxWidth())
                        if(playing && s.paused)Text("Paused · Step = 1/60 second",Modifier.align(Alignment.BottomCenter).background(MaterialTheme.colorScheme.surface).padding(12.dp))
                    }
                    if(wide && !playing)Box(Modifier.width(250.dp).fillMaxHeight()){Inspector(s,vm){delete=true}}
                }
                if(!playing) {
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        (if(wide)listOf("Assets","Animation","Console","Viewport") else listOf("Hierarchy","Inspector","Assets","Animation","Console","Viewport")).forEach { label -> TextButton(enabled=!s.drawCollider,onClick={panel=label}){Text(if(panel==label)"[$label]" else label)} }
                    }
                    if(panel!="Viewport" && !s.drawCollider)Box(Modifier.fillMaxWidth().height(panelHeight)) {
                        when(panel) {
                            "Hierarchy" -> if(wide)Console(s,Modifier.fillMaxSize()) else Hierarchy(s,vm)
                            "Inspector" -> if(wide)Console(s,Modifier.fillMaxSize()) else Inspector(s,vm){delete=true}
                            "Assets" -> AssetBrowser(s.assets,s.selectedAsset,s.frames,project.directory,s.busy,s.assetProgress,vm,Modifier.fillMaxSize())
                            "Animation" -> TimelineEditor(s.scene,s.selected,s.selectedClip,s.animationFrame,images,vm,Modifier.fillMaxSize())
                            else -> Console(s,Modifier.fillMaxSize())
                        }
                    }
                } else Console(s,Modifier.fillMaxWidth().height(64.dp))
            }
        }
        if(!playing)Text(if(s.drawCollider)"Tap 3–8 points. Apply creates a convex collider; concavities are filled." else "Drag: move/pan · Pinch: zoom · Hold: actions",Modifier.padding(6.dp),style=MaterialTheme.typography.labelSmall)
    }
    if(systems && !playing)Dialog(onDismissRequest={if(!s.busy)systems=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            SystemsPanel(s,vm,{systems=false},{vm.beginColliderDraw();systems=false})
        }
    }
    if(delete && s.selected!=null)AlertDialog(onDismissRequest={delete=false},title={Text("Delete entity and children?")},text={Text("Connected joints and camera follow references are removed. This can be undone. Asset files are kept.")},confirmButton={TextButton(enabled=editable,onClick={delete=false;vm.deleteSelected()}){Text("Delete")}},dismissButton={TextButton(onClick={delete=false}){Text("Cancel")}})
}

@Composable private fun SystemsPanel(s: EditorState,vm: EditorViewModel,close: ()->Unit,draw: ()->Unit) {
    var tab by remember { mutableStateOf("Actor") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(enabled=!s.busy,onClick=close){Text("Close systems")}
            listOf("Actor","Joints","Input","Scene").forEach { name -> TextButton(onClick={tab=name}){Text(if(tab==name)"[$name]" else name)} }
        }
        if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        Box(Modifier.weight(1f)) {
            when(tab) {
                "Actor" -> RuntimeInspector(s.scene,s.scene.nodes.find { it.id==s.selected },vm,draw)
                "Joints" -> JointEditor(s.scene,vm)
                "Input" -> InputMapEditor(s.scene,vm)
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    Text("SCENE RUNTIME",style=MaterialTheme.typography.titleMedium)
                    key(s.scene.gravity) {
                        var x by remember { mutableStateOf(s.scene.gravity.x.toString()) };var y by remember { mutableStateOf(s.scene.gravity.y.toString()) }
                        RuntimeField("Gravity X pixels/sec²",x){x=it};RuntimeField("Gravity Y pixels/sec²",y){y=it}
                        TextButton(onClick={vm.parseRuntime { vm.editRuntimeScene(s.scene.copy(gravity=Vec2(x.toFloat(),y.toFloat()))) }}){Text("Apply gravity")}
                    }
                    TextButton(onClick={vm.addCamera();tab="Actor"}){Text("Create camera entity")}
                    TextButton(onClick={vm.editRuntimeScene(s.scene.copy(activeCamera=null))}){Text("Use all enabled cameras")}
                    Text("Test rig adds a player, ground, ball, capsule, pendulum, sensor and follow camera, and resets gravity/input to the defaults. It is one undoable scene edit.")
                    Button(onClick={vm.addPhysicsTestRig();close()}){Text("Add platformer test rig")}
                    Text("Play works on a disposable scene copy. Pause and single-step inspect simulation; Stop returns to the unchanged authored scene. Physics runs at 60 Hz with at most eight catch-up steps. Dynamic bodies own their pose: use a visual child for transform animation.")
                }
            }
        }
    }
}

@Composable private fun Hierarchy(s: EditorState, vm: EditorViewModel) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Text("HIERARCHY · ${s.scene.nodes.size}",style=MaterialTheme.typography.labelLarge)
        OutlinedTextField(query,{query=it},label={Text("Find entity")},singleLine=true,modifier=Modifier.fillMaxWidth())
        LazyColumn { items(s.scene.nodes.filter { it.name.contains(query,true) },key={it.id}) { node -> TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={vm.select(node.id)},modifier=Modifier.fillMaxWidth()){Text((if(node.parent!=null) "↳ " else "")+(if(node.id==s.selected) "● " else "○ ")+node.name)} } }
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
            Button(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={
                val values=listOf(x,y,rotation,sx,sy).map { it.toFloatOrNull() }
                if(name.isBlank() || values.any { it==null || !it.isFinite() } || kotlin.math.abs(values[3] ?: 0f)<.001f || kotlin.math.abs(values[4] ?: 0f)<.001f) vm.report("Enter a name, finite numeric properties, and non-zero scales.")
                else {
                    val t=Transform(Vec2(values[0]!!,values[1]!!),values[2]!!,Vec2(values[3]!!,values[4]!!))
                    vm.updateNode(node.copy(name=name,components=node.components.map { if(it is TransformComponent) TransformComponent(t) else it }))
                }
            }){Text("Apply properties")}
            Text("ID: ${node.id}",style=MaterialTheme.typography.labelSmall)
            Text(node.sprite?.assetId ?: node.sprite?.asset ?: if(node.sprite==null) "Hierarchy group" else "Built-in solid sprite",style=MaterialTheme.typography.labelSmall)
            TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick=vm::createPrefabFromSelection){Text("Create prefab")}
            node.prefab?.let { binding ->
                Text("Prefab overrides: ${binding.overrides.joinToString().ifEmpty { "none" }}",style=MaterialTheme.typography.labelSmall)
                TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={prefabAction="Refresh instance"}){Text("Refresh instance")}
                TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={prefabAction="Revert overrides"}){Text("Revert overrides")}
                TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={prefabAction="Update prefab source"}){Text("Update prefab source")}
            }
            TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick=delete){Text("Delete entity")}
        }
    }
    prefabAction?.let { action -> AlertDialog(onDismissRequest={prefabAction=null},title={Text("$action?")},text={Text(when(action) { "Revert overrides" -> "Reset this instance from its source, including its children. This scene edit can be undone."; "Refresh instance" -> "Rebuild this subtree from its prefab source while preserving property overrides. Locally added or removed children are not structural overrides and may be discarded or restored. This scene edit can be undone."; else -> "Replace the prefab source with this instance. Other instances receive changes when refreshed; source updates are not scene undo commands." })},confirmButton={TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={prefabAction=null;when(action) { "Revert overrides" -> vm.refreshPrefab(false); "Refresh instance" -> vm.refreshPrefab(true); else -> vm.updatePrefabSource() }}){Text("Confirm")}},dismissButton={TextButton(onClick={prefabAction=null}){Text("Cancel")}}) }
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
    api(project(":engine-animation"))
    api(project(":engine-physics"))
    api(project(":engine-render"))
}
````

## editor-viewport/src/main/kotlin/world/engine/viewport/PreviewSession.kt

````kotlin
package world.engine.viewport

import world.engine.core.*
import world.engine.math.*
import world.engine.animation.*
import world.engine.physics.*
import world.engine.render.*
import java.util.concurrent.ConcurrentLinkedQueue

/** Nondestructive play session. Only advance() touches the Box2D world, on one worker thread. */
data class PreviewFrame(val scene: Scene,val views: List<CameraView>,val lines: List<DebugLine>,val steps: Long,val droppedSeconds: Float,val bodies: Int,val joints: Int,val input: Map<String,Float>)
class PreviewSession(private val authored: Scene,private val event: (String)->Unit) {
    val input=InputRouter(authored.inputMap)
    private val cameras=CameraRig(authored.activeCamera)
    private val commands=ConcurrentLinkedQueue<()->Unit>()
    private val physics=PhysicsWorld(authored) { event("${if(it.sensor)"Trigger" else "Collision"} ${if(it.entered)"enter" else "exit"}: ${it.a} ↔ ${it.b}") }
    private val animators=authored.nodes.mapNotNull { n -> n.component<AnimatorComponent>()?.let { n.id to AnimationPlayer(authored.clips.single { c -> c.id==it.clipId },it.speed) } }.toMap()
    private val bases=authored.nodes.associateBy { it.id }
    private var current=authored
    private var accumulator=0.0
    private var steps=0L
    private var dropped=0f
    private var inputValues=emptyMap<String,Float>()
    @Volatile private var dimensions=800 to 600
    fun resize(width: Int,height: Int) { dimensions=width.coerceAtLeast(1) to height.coerceAtLeast(1) }
    fun focusCamera(id: String?) { commands.add { cameras.focus(id) } }
    fun shake(preset: ShakePreset) { commands.add { cameras.shake(preset) } }
    fun advance(seconds: Float): PreviewFrame {
        require(seconds.isFinite() && seconds>=0)
        while(true) { val command=commands.poll() ?: break;command() }
        val accepted=seconds.coerceAtMost(.25f);dropped+=(seconds-accepted)
        accumulator+=accepted
        var count=0
        while(accumulator+1e-9>=PhysicsWorld.STEP && count<8) {
            val controls=input.snapshot();inputValues=controls.values
            controls.pressed.forEach { event("Input pressed: $it") }
            current=current.copy(nodes=current.nodes.map { n ->
                animators[n.id]?.let { player ->
                    player.advance(PhysicsWorld.STEP) { e -> event("Animation ${e.name} (${e.kind}) on ${n.name}, frame ${e.frame}") }
                    AnimationSampler.sample(bases.getValue(n.id),player.clip,player.frame,n,player.eventSprite)
                } ?: n
            })
            current=current.copy(nodes=current.nodes.map { n ->
                val controller=n.component<InputControllerComponent>() ?: return@map n
                val x=controls.value(controller.horizontal);val y=controls.value(controller.vertical)
                val movement=Vec2(x,y);val direction=if(movement.length()>1f)movement*(1f/movement.length()) else movement
                val body=n.component<RigidBodyComponent>()
                if(body!=null && body.kind!=BodyKind.STATIC) {
                    val old=physics.velocity(n.id)
                    val vy=if(controller.kind==ControllerKind.TOP_DOWN)direction.y*controller.speed else if(controller.jump in controls.pressed && physics.isGrounded(n.id))controller.jumpSpeed else old.y
                    physics.setVelocity(n.id,Vec2(if(controller.kind==ControllerKind.TOP_DOWN)direction.x*controller.speed else x*controller.speed,vy));n
                } else if(body==null)n.moved(n.transform.position+direction*(controller.speed*PhysicsWorld.STEP)) else n
            })
            current=physics.step(current)
            val (width,height)=dimensions
            cameras.update(current,PhysicsWorld.STEP,width,height)
            steps++;count++;accumulator-=PhysicsWorld.STEP
        }
        if(accumulator>=PhysicsWorld.STEP) { val remainder=accumulator%PhysicsWorld.STEP;dropped+=(accumulator-remainder).toFloat();accumulator=remainder }
        val (width,height)=dimensions
        return PreviewFrame(current,cameras.update(current,0f,width,height),physics.debugLines(),steps,dropped,physics.bodyCount,physics.jointCount,inputValues)
    }
}
````

## editor-viewport/src/main/kotlin/world/engine/viewport/WorldViewport.kt

````kotlin
package world.engine.viewport

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.DragEvent
import android.view.ScaleGestureDetector
import android.view.KeyEvent
import android.view.InputDevice
import world.engine.core.InputSource
import world.engine.core.DebugLine
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
    var onColliderPoint: (Vec2)->Unit = {}
    var onPhysicalInput: (InputSource,Int,Float,Int)->Unit = { _,_,_,_-> }
    var onReleaseInput: ()->Unit = {}
    var onDeviceRemoved: (Int)->Unit = {}
    private val inputs=context.getSystemService(Context.INPUT_SERVICE) as InputManager
    private val devices=object: InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int)=Unit
        override fun onInputDeviceChanged(deviceId: Int) { onDeviceRemoved(deviceId) }
        override fun onInputDeviceRemoved(deviceId: Int) { onDeviceRemoved(deviceId) }
    }
    var onViewportSize: (Int,Int)->Unit = { _,_-> }
    private var playing=false
    private var drawing=false
    private var views: List<CameraView> = emptyList()
    private var debugLines: List<DebugLine> = emptyList()
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
        isFocusableInTouchMode=true
        setEGLContextClientVersion(3); preserveEGLContextOnPause=true; setRenderer(renderer); renderMode=RENDERMODE_WHEN_DIRTY
        setOnDragListener { _, event ->
            when(event.action) {
                DragEvent.ACTION_DRAG_STARTED -> isEnabled && !playing && !drawing && event.clipDescription?.label=="2DWorldAsset"
                DragEvent.ACTION_DROP -> {
                    if(isEnabled && !playing && !drawing) event.clipData?.getItemAt(0)?.text?.toString()?.let { onAssetDrop(it,world(event.x,event.y)) }
                    true
                }
                else -> true
            }
        }
        contentDescription="Scene viewport. Drag sprites to move; drag empty space to pan; pinch to zoom; long press for actions."
    }
    fun update(scene: Scene, selected: String?, directory: File?, assetPaths: Map<String,String> = emptyMap(), playing: Boolean=false, drawing: Boolean=false, views: List<CameraView> = emptyList(), debugLines: List<DebugLine> = emptyList()) {
        if(playing && (!this.playing || !hasFocus()) && isEnabled)requestFocus()
        this.playing=playing;this.drawing=drawing;this.views=views;this.debugLines=debugLines
        this.assetPaths=assetPaths
        if(this.directory!=directory) camera=Camera2D()
        this.scene=scene; this.selected=selected; this.directory=directory; publish()
    }
    fun resetCamera() { camera=Camera2D(); publish() }
    private fun publish() { renderer.frame=RenderFrame(scene,if(playing)null else selected,camera,directory,assetPaths,views,debugLines,!playing); requestRender() }
    private fun world(x: Float,y: Float)=camera.screenToWorld(x,y,width,height)
    private fun finish() {
        dragging?.let { id -> scene.nodes.find { it.id==id }?.let { onMove(id,it.transform.position,true) } }; dragging=null
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(!isEnabled) return true
        parent?.requestDisallowInterceptTouchEvent(true)
        if(playing) {
            if(e.actionMasked==MotionEvent.ACTION_DOWN)requestFocus()
            if(e.isFromSource(InputDevice.SOURCE_MOUSE))mouse(e)
            return true
        }
        if(drawing) { if(e.actionMasked==MotionEvent.ACTION_UP)onColliderPoint(world(e.x,e.y));return true }
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

    override fun onAttachedToWindow() { super.onAttachedToWindow();inputs.registerInputDeviceListener(devices,Handler(Looper.getMainLooper())) }
    override fun onDetachedFromWindow() { inputs.unregisterInputDeviceListener(devices);onReleaseInput();super.onDetachedFromWindow() }
    override fun onSizeChanged(w: Int,h: Int,oldw: Int,oldh: Int) { super.onSizeChanged(w,h,oldw,oldh);onViewportSize(w,h) }
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) { super.onWindowFocusChanged(hasWindowFocus);if(!hasWindowFocus)onReleaseInput() }
    override fun onKeyDown(keyCode: Int,event: KeyEvent): Boolean {
        if(playing && keyCode!=KeyEvent.KEYCODE_BACK) { onPhysicalInput(InputSource.KEY,keyCode,1f,event.deviceId);return true }
        return super.onKeyDown(keyCode,event)
    }
    override fun onKeyUp(keyCode: Int,event: KeyEvent): Boolean {
        if(playing && keyCode!=KeyEvent.KEYCODE_BACK) { onPhysicalInput(InputSource.KEY,keyCode,0f,event.deviceId);return true }
        return super.onKeyUp(keyCode,event)
    }
    private fun mouse(e: MotionEvent) { listOf(1,2,4,8,16).forEach { onPhysicalInput(InputSource.MOUSE_BUTTON,it,if(e.buttonState and it!=0)1f else 0f,e.deviceId) } }
    override fun onGenericMotionEvent(e: MotionEvent): Boolean {
        if(playing && e.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
            scene.inputMap.bindings.filter { it.source==InputSource.GAMEPAD_AXIS }.map { it.code }.distinct().forEach { onPhysicalInput(InputSource.GAMEPAD_AXIS,it,e.getAxisValue(it),e.deviceId) }
            return true
        }
        if(playing && e.isFromSource(InputDevice.SOURCE_MOUSE)) { mouse(e);return true }
        return super.onGenericMotionEvent(e)
    }
}
````

## engine-animation/README.md

````markdown
# engine-animation — Phase 3

`AnimationPlayer` advances deterministic frame time with ONCE/LOOP/PING_PONG playback and crossed-frame event dispatch. `AnimationSampler` applies position, rotation, scale, RGBA and asset-UUID tracks without modifying authored nodes. `AnimationPresets` provides 65 deterministic transform/color clips: 13 actions × five archetypes. These are not new sprite-sheet artwork.

The runtime exposes real TRIGGER and SET_SPRITE events. Sound/particle/script consumers are intentionally not exposed before their later-phase modules exist. See `docs/PHASE3.md` for runtime integration and `docs/PHASE3_VERIFICATION.md` for executed versus pending tests.
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
    testOptions { unitTests.all { it.useJUnitPlatform() } }
}
dependencies {
    api(project(":engine-core"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}
````

## engine-animation/src/main/kotlin/world/engine/animation/AnimationPlayer.kt

````kotlin
package world.engine.animation

import world.engine.core.*
import world.engine.math.*
import kotlin.math.floor

/** Deterministic frame clock. Crossing a boundary dispatches that frame's events exactly once. */
class AnimationPlayer(val clip: AnimationClip, private val speed: Float=1f) {
    private var ticks=0.0
    private var started=false
    /** Last dispatched sprite event. It changes only at real event boundaries, including reverse playback. */
    var eventSprite: String?=null
        private set
    init { clip.validated(); require(speed in .01f..10f) }
    val finished get() = clip.loop==LoopMode.ONCE && ticks>=clip.frames
    val frame: Float get() = frameAt(ticks).toFloat()
    /** Seeks in forward frame space without emitting events. */
    fun seek(frame: Float) { require(frame.isFinite()); ticks=frame.coerceIn(0f,(clip.frames-1).toFloat()).toDouble();eventSprite=latestSpriteAt(clip,ticks.toFloat());started=true }
    /** Advances bounded elapsed time, delivering frame-zero and every crossed frame in order. */
    fun advance(seconds: Float, emit: (AnimationEvent)->Unit = {}) {
        require(seconds.isFinite() && seconds in 0f..10f)
        if(!started) { emitAt(0,emit); started=true }
        if(finished)return
        val end=if(clip.loop==LoopMode.ONCE) minOf(ticks+seconds*clip.fps*speed,clip.frames.toDouble()) else ticks+seconds*clip.fps*speed
        val first=floor(ticks).toLong()+1; val last=floor(end+1e-9).toLong()
        require(last-first<20000) { "Animation event catch-up limit exceeded" }
        for(step in first..last) {
            if(clip.loop==LoopMode.ONCE && step>=clip.frames)break
            emitAt(frameAt(step.toDouble()).toInt(),emit)
        }
        ticks=end
        // Keep long-running loops numerically stable without losing a boundary.
        if(ticks>1_000_000 && clip.loop!=LoopMode.ONCE)ticks%=period()
    }
    private fun emitAt(frame: Int,emit: (AnimationEvent)->Unit) { clip.events.filter { it.frame==frame }.forEach { if(it.kind==AnimationEventKind.SET_SPRITE)eventSprite=it.assetId;emit(it) } }
    private fun period() = if(clip.loop==LoopMode.PING_PONG) maxOf(1,2*(clip.frames-1)).toDouble() else clip.frames.toDouble()
    private fun frameAt(t: Double): Double = when(clip.loop) {
        LoopMode.ONCE -> t.coerceIn(0.0,(clip.frames-1).toDouble())
        LoopMode.LOOP -> (t%period()).coerceAtMost((clip.frames-1).toDouble())
        LoopMode.PING_PONG -> { val p=t%period(); if(p<=clip.frames-1)p else period()-p }
    }
}

object AnimationSampler {
    /** A pure sampler used both by timeline scrubbing and the runtime player. */
    fun sample(base: Node,clip: AnimationClip,frame: Float,current: Node=base,eventSprite: String?=latestSpriteAt(clip,frame)): Node {
        require(frame.isFinite())
        var transform=current.transform; var sprite=current.sprite
        val f=frame.coerceIn(0f,(clip.frames-1).toFloat())
        clip.tracks.forEach { track ->
            val keys=track.keys.sortedBy { it.frame }
            val a=keys.lastOrNull { it.frame<=f } ?: keys.first()
            val b=keys.firstOrNull { it.frame>f } ?: a
            val t=if(track.interpolation==Interpolation.STEP || a.frame==b.frame)0f else ((f-a.frame)/(b.frame-a.frame)).coerceIn(0f,1f)
            val values=a.values.indices.map { i -> a.values[i]+(b.values[i]-a.values[i])*t }
            when(track.property) {
                TrackProperty.POSITION -> { val v=Vec2(values[0],values[1]); transform=transform.copy(position=if(clip.relative)base.transform.position+v else v) }
                TrackProperty.ROTATION -> transform=transform.copy(rotation=values[0]+if(clip.relative)base.transform.rotation else 0f)
                TrackProperty.SCALE -> { val v=Vec2(values[0],values[1]); transform=transform.copy(scale=if(clip.relative)Vec2(base.transform.scale.x*v.x,base.transform.scale.y*v.y) else v) }
                TrackProperty.COLOR -> sprite=sprite?.copy(tint=Color(values[0],values[1],values[2],values[3]))
                TrackProperty.SPRITE -> sprite=sprite?.copy(asset=null,assetId=a.assetId)
            }
        }
        eventSprite?.let { sprite=sprite?.copy(asset=null,assetId=it) }
        return current.copy(components=current.components.map { when(it) { is TransformComponent -> TransformComponent(transform); is SpriteComponent -> sprite ?: it; else -> it } })
    }
}

private fun latestSpriteAt(clip: AnimationClip,frame: Float): String? = clip.events.withIndex().filter { it.value.kind==AnimationEventKind.SET_SPRITE && it.value.frame<=frame }.maxWithOrNull(compareBy<IndexedValue<AnimationEvent>> { it.value.frame }.thenBy { it.index })?.value?.assetId
````

## engine-animation/src/main/kotlin/world/engine/animation/AnimationPresets.kt

````kotlin
package world.engine.animation

import world.engine.core.*
import java.util.UUID

/** 65 editable transform/color clips. These do not pretend to be hand-drawn sprite-frame artwork. */
object AnimationPresets {
    private val actions=listOf("Idle","Walk","Run","Jump","Attack","Hurt","Death","Roll","Dash","Shoot","Reload","Climb","Swim")
    fun all(): List<AnimationClip> = listOf("Humanoid","Robot","Animal","Vehicle","Creature").flatMapIndexed { archetype,type ->
        actions.map { action ->
            val amount=1f+archetype*.15f
            fun track(p: TrackProperty,vararg values: List<Float>) = AnimationTrack(p,values.mapIndexed { i,v -> AnimationKey(i*11/(values.size-1),v) })
            val tracks=when(action) {
                "Idle" -> listOf(track(TrackProperty.SCALE,listOf(1f,1f),listOf(1f,1.025f*amount.coerceAtMost(1.1f)),listOf(1f,1f)))
                "Walk","Run" -> listOf(track(TrackProperty.ROTATION,listOf(-4f*amount),listOf(4f*amount),listOf(-4f*amount)),track(TrackProperty.POSITION,listOf(0f,0f),listOf(0f,if(action=="Run")8f else 3f),listOf(0f,0f)))
                "Jump" -> listOf(track(TrackProperty.POSITION,listOf(0f,0f),listOf(0f,36f*amount),listOf(0f,0f)))
                "Attack" -> listOf(track(TrackProperty.ROTATION,listOf(0f),listOf(-25f*amount),listOf(30f*amount),listOf(0f)))
                "Hurt" -> listOf(track(TrackProperty.COLOR,listOf(1f,1f,1f,1f),listOf(1f,.15f,.15f,1f),listOf(1f,1f,1f,1f)))
                "Death" -> listOf(track(TrackProperty.ROTATION,listOf(0f),listOf(90f)),track(TrackProperty.COLOR,listOf(1f,1f,1f,1f),listOf(1f,1f,1f,0f)))
                "Roll" -> listOf(track(TrackProperty.ROTATION,listOf(0f),listOf(360f)))
                "Dash" -> listOf(track(TrackProperty.POSITION,listOf(0f,0f),listOf(80f*amount,0f)))
                "Shoot" -> listOf(track(TrackProperty.POSITION,listOf(0f,0f),listOf(-6f*amount,0f),listOf(0f,0f)))
                "Reload" -> listOf(track(TrackProperty.ROTATION,listOf(0f),listOf(20f),listOf(0f)))
                "Climb" -> listOf(track(TrackProperty.POSITION,listOf(0f,0f),listOf(0f,16f*amount)))
                else -> listOf(track(TrackProperty.ROTATION,listOf(-10f),listOf(10f),listOf(-10f)))
            }
            AnimationClip(id=UUID.nameUUIDFromBytes("world.preset.$type.$action.v1".toByteArray()).toString(),name="$type $action",fps=if(action=="Run")24 else 12,frames=12,loop=if(action in listOf("Idle","Walk","Run","Swim","Climb"))LoopMode.LOOP else LoopMode.ONCE,tracks=tracks,
                events=if(action in listOf("Attack","Shoot","Jump"))listOf(AnimationEvent(5,action.lowercase())) else emptyList()).validated()
        }
    }
}
````

## engine-animation/src/test/kotlin/world/engine/animation/AnimationPlayerTest.kt

````kotlin
package world.engine.animation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import world.engine.core.*
import world.engine.math.*

class AnimationPlayerTest {
    @Test fun pingPongBoundariesAndSeekDoNotDuplicateEvents() {
        val player=AnimationPlayer(AnimationClip(frames=3,fps=1,loop=LoopMode.PING_PONG,events=(0..2).map { AnimationEvent(it,"f$it") }))
        val events=mutableListOf<Int>();repeat(5){player.advance(1f){events.add(it.frame)}}
        assertEquals(listOf(0,1,2,1,0,1),events)
        player.seek(1f);val next=mutableListOf<Int>();player.advance(1f){next.add(it.frame)};assertEquals(listOf(2),next)
    }
    @Test fun multipleLoopEventsAreNotSkippedByLongFrames() {
        val clip=AnimationClip(frames=3,fps=10,events=(0..2).map { AnimationEvent(it,"f$it") });val player=AnimationPlayer(clip);val events=mutableListOf<Int>()
        player.advance(.65f){events.add(it.frame)};assertEquals(listOf(0,1,2,0,1,2,0),events)
    }
    @Test fun reversePlaybackChangesSpriteOnlyWhenItsEventIsCrossed() {
        val a=java.util.UUID.randomUUID().toString();val b=java.util.UUID.randomUUID().toString()
        val clip=AnimationClip(frames=3,fps=1,loop=LoopMode.PING_PONG,events=listOf(AnimationEvent(1,"a",AnimationEventKind.SET_SPRITE,a),AnimationEvent(2,"b",AnimationEventKind.SET_SPRITE,b)))
        val player=AnimationPlayer(clip);player.advance(2f);assertEquals(b,player.eventSprite)
        player.advance(.5f);assertEquals(1.5f,player.frame);assertEquals(b,player.eventSprite)
        player.advance(.5f);assertEquals(a,player.eventSprite)
        player.seek(0f);assertNull(player.eventSprite)
    }
    @Test fun relativePositionAndStepInterpolation() {
        val node=Node().moved(Vec2(100f,50f))
        val track=AnimationTrack(TrackProperty.POSITION,listOf(AnimationKey(0,listOf(0f,0f)),AnimationKey(10,listOf(20f,40f))))
        val clip=AnimationClip(frames=11,tracks=listOf(track))
        assertEquals(Vec2(110f,70f),AnimationSampler.sample(node,clip,5f).transform.position)
        assertEquals(node.transform.position,AnimationSampler.sample(node,clip.copy(tracks=listOf(track.copy(interpolation=Interpolation.STEP))),5f).transform.position)
    }
    @Test fun libraryContains65StableValidatedClips() { val library=AnimationPresets.all();assertEquals(65,library.size);assertEquals(65,library.map { it.id }.distinct().size);assertEquals(library,AnimationPresets.all());library.forEach { it.validated() } }
}
````

## engine-core/src/main/kotlin/world/engine/core/InputRouter.kt

````kotlin
package world.engine.core

/** Source values are combined per action; snapshots consume buffered edges, not held state. */
data class InputSnapshot(val values: Map<String,Float>,val pressed: Set<String>,val released: Set<String>) {
    fun value(action: String) = values[action] ?: 0f
}
class InputRouter(private val mapping: InputMap) {
    private val physical=mutableMapOf<Triple<InputSource,Int,Int>,Float>()
    private val touch=mutableMapOf<String,Float>()
    private var active=emptySet<String>()
    private val pressed=mutableSetOf<String>()
    private val released=mutableSetOf<String>()
    private var values=emptyMap<String,Float>()
    @Synchronized fun physical(source: InputSource,code: Int,value: Float,device: Int=0) {
        if(!value.isFinite())return
        val key=Triple(source,code,device)
        if(value==0f)physical.remove(key) else physical[key]=value.coerceIn(-1f,1f)
        resolve()
    }
    @Synchronized fun touch(action: String,value: Float) {
        if(!value.isFinite() || mapping.bindings.none { it.source==InputSource.TOUCH && it.action==action })return
        if(value==0f)touch.remove(action) else touch[action]=value.coerceIn(-1f,1f)
        resolve()
    }
    /** Focus/pause cancellation releases held sources and cannot inject a stale press on resume. */
    @Synchronized fun releaseAll() { physical.clear();touch.clear();pressed.clear();resolve() }
    /** Device disconnect cancels only that device, preserving other held controls. */
    @Synchronized fun releaseDevice(device: Int) {
        val removed=physical.keys.filter { it.third==device }.toSet()
        val affected=mapping.bindings.filter { b -> removed.any { it.first==b.source && it.second==b.code } }.map { it.action }.toSet()
        physical.keys.removeAll(removed);resolve();pressed.removeAll(affected-active)
    }
    private fun resolve() {
        val next=mutableMapOf<String,Float>()
        mapping.bindings.forEach { b ->
            val raw=if(b.source==InputSource.TOUCH)touch[b.action] ?: 0f else physical.filterKeys { it.first==b.source && it.second==b.code }.values.sum()
            val filtered=if(b.source==InputSource.GAMEPAD_AXIS) { val a=kotlin.math.abs(raw);if(a<=b.deadZone)0f else kotlin.math.sign(raw)*(a-b.deadZone)/(1f-b.deadZone) } else raw
            next[b.action]=(next[b.action] ?: 0f)+filtered*b.scale
        }
        next.replaceAll { _,v -> v.coerceIn(-1f,1f) }
        val held=next.filterValues { kotlin.math.abs(it)>.5f }.keys.toSet()
        pressed.addAll(held-active);released.addAll(active-held);active=held;values=next.toMap()
    }
    @Synchronized fun snapshot(): InputSnapshot {
        val result=InputSnapshot(values,pressed.toSet(),released.toSet());pressed.clear();released.clear();return result
    }
}
````

## engine-core/src/main/kotlin/world/engine/core/Prefab.kt

````kotlin
package world.engine.core

import kotlinx.serialization.Serializable
import world.engine.math.*
import java.util.UUID

/** A binding keeps instance identity and per-property overrides independent of asset paths. */
@Serializable data class PrefabBinding(val assetId: String, val sourcePath: String, val instanceRoot: String, val overrides: Set<String> = emptySet())
@Serializable data class PrefabDefinition(val version: Int=1, val nodes: List<Node>, val clips: List<AnimationClip> = emptyList(), val joints: List<JointSpec> = emptyList()) {
    fun validated(): PrefabDefinition {
        require(version==1 && nodes.count { it.parent==null }==1) { "Prefab needs one root" }
        Scene(nodes=nodes,clips=clips,joints=joints).validated()
        require(nodes.all { it.prefab==null }) { "Definitions cannot contain runtime bindings" }
        nodes.forEach { require(it.nestedPrefab==null || Scene.isAssetId(it.nestedPrefab)) }
        return this
    }
    fun dependencies(): Set<String> = nodes.flatMap { listOfNotNull(it.sprite?.assetId,it.sprite?.asset?.removePrefix("Sprites/")?.removeSuffix(".png")?.takeIf { id -> Scene.isAssetId(id) },it.nestedPrefab) }.toSet()+clips.flatMap { it.assets() }
}

/** Nested definitions expand to ordinary editable ECS entities, bounded against recursion/bombs. */
object Prefabs {
    fun instantiate(assetId: String, definitions: Map<String,PrefabDefinition>, position: Vec2=Vec2(), previous: List<Node> = emptyList(), preserveOverrides: Boolean=true): List<Node> = bundle(assetId,definitions,position,previous,preserveOverrides).nodes
    /** Complete expansion includes clip resources and remapped internal joints. */
    fun bundle(assetId: String, definitions: Map<String,PrefabDefinition>, position: Vec2=Vec2(), previous: List<Node> = emptyList(), preserveOverrides: Boolean=true, extraClips: List<AnimationClip> = emptyList()): Scene {
        val old=previous.associateBy { it.prefab?.sourcePath }
        val output=mutableListOf<Node>()
        val clips=extraClips.associateBy { it.id }.toMutableMap()
        val joints=mutableListOf<JointSpec>()
        var instanceRoot=""
        fun expand(id: String, parent: String?, prefix: String, stack: Set<String>) {
            require(id !in stack && stack.size<16) { "Cyclic or excessively nested prefab" }
            val definition=definitions[id]?.validated() ?: error("Missing prefab $id")
            definition.clips.forEach { clip -> require(clips[clip.id]==null || clips[clip.id]==clip) { "Conflicting animation UUID" };clips[clip.id]=clip }
            val root=definition.nodes.single { it.parent==null }
            val ids=definition.nodes.associate { n -> n.id to (old[prefix+n.id]?.id ?: UUID.randomUUID().toString()) }
            if(instanceRoot.isEmpty()) instanceRoot=ids.getValue(root.id)
            definition.joints.forEach { joint -> joints.add(joint.copy(id=UUID.nameUUIDFromBytes("$instanceRoot/$prefix/${joint.id}".toByteArray()).toString(),bodyA=ids.getValue(joint.bodyA),bodyB=ids.getValue(joint.bodyB))) }
            definition.nodes.forEach { source ->
                require(output.size<5000) { "Expanded prefab exceeds 5000 entities" }
                val path=prefix+source.id; val prior=old[path]
                val flags=if(preserveOverrides) prior?.prefab?.overrides.orEmpty() else emptySet()
                var node=source.copy(id=ids.getValue(source.id),parent=source.parent?.let { ids.getValue(it) } ?: parent,
                    prefab=PrefabBinding(assetId,path,instanceRoot,flags))
                node=node.copy(components=node.components.map { c -> if(c is CameraComponent)c.copy(follow=c.follow?.let { ids.getValue(it) }) else c })
                if(prior!=null) {
                    if("name" in flags) node=node.copy(name=prior.name)
                    node=node.copy(components=node.components.map { c -> when {
                        c is TransformComponent && "transform" in flags -> TransformComponent(prior.transform)
                        c is SpriteComponent && "sprite" in flags -> prior.sprite ?: c
                        else -> c
                    } })
                }
                if(prior!=null && "runtime" in flags)node=node.copy(components=node.components.filter { it is TransformComponent || it is SpriteComponent }+prior.components.filterNot { it is TransformComponent || it is SpriteComponent })
                if(node.id==instanceRoot) node=node.moved(position)
                output.add(node)
                source.nestedPrefab?.let { expand(it,node.id,"$path/",stack+id) }
            }
        }
        expand(assetId,null,"",emptySet())
        return Scene(nodes=output,clips=clips.values.toList(),joints=joints).validated()
    }
    /** Capture a subtree, preserving nested references instead of duplicating their expansion. */
    fun capture(scene: Scene, rootId: String): PrefabDefinition {
        val root=scene.nodes.single { it.id==rootId }
        val excluded=scene.nodes.filter { it.nestedPrefab!=null && it.id in scene.descendants(rootId) }
            .flatMap { scene.descendants(it.id)-it.id }.toSet()
        val selected=scene.nodes.filter { it.id in scene.descendants(rootId) && it.id !in excluded }
        val ids=selected.associate { it.id to (if(root.prefab?.instanceRoot==root.id) it.prefab?.sourcePath ?: it.id else it.id) }
        val nodes=selected.map {
            val components=it.components.map { c -> if(c is CameraComponent)c.copy(follow=c.follow?.let { target -> ids[target] ?: error("Camera follows a target outside this prefab subtree") }) else c }
            val clean=it.copy(id=ids.getValue(it.id),parent=it.parent?.let { p -> ids[p] },prefab=null,components=components)
            if(it.id==root.id) clean.copy(parent=null).moved(Vec2()) else clean
        }
        val subtree=scene.descendants(rootId)
        val related=scene.joints.filter { it.bodyA in subtree || it.bodyB in subtree }
        require(related.all { it.bodyA in ids && it.bodyB in ids }) { "Cannot capture joints across an external or nested prefab boundary" }
        val joints=related.map { it.copy(bodyA=ids.getValue(it.bodyA),bodyB=ids.getValue(it.bodyB)) }
        val clipIds=nodes.mapNotNull { it.component<AnimatorComponent>()?.clipId }.toSet()
        return PrefabDefinition(nodes=nodes,clips=scene.clips.filter { it.id in clipIds },joints=joints).validated()
    }
    fun override(node: Node, vararg fields: String): Node = node.copy(prefab=node.prefab?.let { it.copy(overrides=it.overrides+fields) })
}
````

## engine-core/src/main/kotlin/world/engine/core/RuntimeData.kt

````kotlin
package world.engine.core

import kotlinx.serialization.Serializable
import world.engine.math.*
import java.util.UUID
import kotlin.math.abs

@Serializable enum class TrackProperty { POSITION, ROTATION, SCALE, COLOR, SPRITE }
@Serializable enum class Interpolation { LINEAR, STEP }
@Serializable enum class LoopMode { ONCE, LOOP, PING_PONG }
@Serializable data class AnimationKey(val frame: Int, val values: List<Float> = emptyList(), val assetId: String? = null)
@Serializable data class AnimationTrack(val property: TrackProperty, val keys: List<AnimationKey>, val interpolation: Interpolation=Interpolation.LINEAR)
@Serializable enum class AnimationEventKind { TRIGGER, SET_SPRITE }
@Serializable data class AnimationEvent(val frame: Int, val name: String, val kind: AnimationEventKind=AnimationEventKind.TRIGGER, val assetId: String?=null)
/** Frame indices are zero-based; the last frame is held for one frame in looping/once clips. */
@Serializable data class AnimationClip(val id: String=UUID.randomUUID().toString(), val name: String="Animation", val fps: Int=12, val frames: Int=24, val loop: LoopMode=LoopMode.LOOP, val relative: Boolean=true, val tracks: List<AnimationTrack> = emptyList(), val events: List<AnimationEvent> = emptyList()) {
    fun validated(): AnimationClip {
        require(id.isNotBlank() && name.isNotBlank() && fps in 1..120 && frames in 1..36000)
        require(tracks.size<=5 && tracks.map { it.property }.distinct().size==tracks.size)
        tracks.forEach { track ->
            require(track.keys.isNotEmpty() && track.keys.size<=36000 && track.keys.map { it.frame }.distinct().size==track.keys.size)
            track.keys.forEach { key ->
                require(key.frame in 0 until frames && key.values.all { it.isFinite() })
                val count=when(track.property) { TrackProperty.POSITION,TrackProperty.SCALE -> 2; TrackProperty.ROTATION -> 1; TrackProperty.COLOR -> 4; TrackProperty.SPRITE -> 0 }
                require(key.values.size==count)
                if(track.property==TrackProperty.SPRITE)require(key.assetId?.let(Scene::isAssetId)==true)
                if(track.property==TrackProperty.SCALE)require(key.values.all { it>=.001f }) { "Scale animation keys must be positive" }
                if(track.property==TrackProperty.COLOR)require(key.values.all { it in 0f..1f })
            }
        }
        require(events.size<=4096)
        events.forEach { require(it.frame in 0 until frames && it.name.isNotBlank()); if(it.kind==AnimationEventKind.SET_SPRITE)require(it.assetId?.let(Scene::isAssetId)==true) }
        return this
    }
    fun assets(): Set<String> = (tracks.flatMap { it.keys.mapNotNull { k -> k.assetId } }+events.mapNotNull { it.assetId }).toSet()
}
@Serializable data class AnimatorComponent(val clipId: String, val speed: Float=1f) : Component()

@Serializable enum class BodyKind { STATIC, DYNAMIC, KINEMATIC }
@Serializable enum class ShapeKind { RECTANGLE, CIRCLE, POLYGON, CAPSULE }
/** Multiple colliders on a body form a compound. Sizes/offsets are local scene pixels. */
@Serializable data class Collider(val shape: ShapeKind=ShapeKind.RECTANGLE, val size: Vec2=Vec2(96f,96f), val offset: Vec2=Vec2(), val radius: Float=48f, val vertices: List<Vec2> = emptyList(), val sensor: Boolean=false, val density: Float=1f, val friction: Float=.3f, val restitution: Float=.1f) {
    fun validated(): Collider {
        require(listOf(size.x,size.y,offset.x,offset.y,radius,density,friction,restitution).all { it.isFinite() })
        require(size.x in .1f..100000f && size.y in .1f..100000f && radius in .05f..50000f)
        require(density in .001f..1000f && friction in 0f..10f && restitution in 0f..1f)
        if(shape==ShapeKind.CAPSULE)require(size.y>=size.x) { "Capsules are vertical; rotate the entity for horizontal capsules" }
        if(shape==ShapeKind.POLYGON) {
            require(vertices.size in 3..8 && vertices.all { it.x in -1000000f..1000000f && it.y in -1000000f..1000000f }) { "Use a convex polygon with 3–8 vertices" }
            val turns=vertices.indices.flatMap { i -> val a=vertices[i];val next=(i+1)%vertices.size;val b=vertices[next];vertices.indices.filter { it!=i && it!=next }.map { k -> val c=vertices[k];(b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x) } }
            require(turns.all { it>0.001f } || turns.all { it<-.001f }) { "Polygon must be strictly convex, ordered, and nondegenerate" }
        }
        return this
    }
}
@Serializable data class RigidBodyComponent(val kind: BodyKind=BodyKind.DYNAMIC, val colliders: List<Collider> = listOf(Collider()), val gravityScale: Float=1f, val fixedRotation: Boolean=false, val bullet: Boolean=false, val linearDamping: Float=0f, val velocity: Vec2=Vec2(), val angularVelocity: Float=0f) : Component()
@Serializable enum class JointKind { FIXED, DISTANCE, REVOLUTE, PRISMATIC, SPRING, WHEEL, ROPE }
/** Anchors are local to each body's entity origin, in pixels; axes are local to body A. */
@Serializable data class JointSpec(val id: String=UUID.randomUUID().toString(), val kind: JointKind=JointKind.DISTANCE, val bodyA: String, val bodyB: String, val anchorA: Vec2=Vec2(), val anchorB: Vec2=Vec2(), val axis: Vec2=Vec2(1f,0f), val length: Float=100f, val frequency: Float=4f, val damping: Float=.7f, val collideConnected: Boolean=false, val motor: Boolean=false, val motorSpeed: Float=0f, val maxMotorForce: Float=100f, val limit: Boolean=false, val lower: Float=-45f, val upper: Float=45f)

@Serializable enum class InputSource { KEY, GAMEPAD_AXIS, MOUSE_BUTTON, TOUCH }
@Serializable data class InputBinding(val action: String, val source: InputSource, val code: Int=0, val scale: Float=1f, val deadZone: Float=.15f)
@Serializable data class InputMap(val bindings: List<InputBinding> = defaultBindings()) {
    companion object {
        fun defaultBindings() = listOf(
            InputBinding("MoveX",InputSource.KEY,29,-1f),InputBinding("MoveX",InputSource.KEY,32),
            InputBinding("MoveX",InputSource.KEY,21,-1f),InputBinding("MoveX",InputSource.KEY,22),
            InputBinding("MoveY",InputSource.KEY,51),InputBinding("MoveY",InputSource.KEY,47,-1f),
            InputBinding("MoveY",InputSource.KEY,19),InputBinding("MoveY",InputSource.KEY,20,-1f),
            InputBinding("Jump",InputSource.KEY,62),InputBinding("Jump",InputSource.KEY,96),
            InputBinding("Attack",InputSource.KEY,38),InputBinding("Attack",InputSource.KEY,97),InputBinding("Attack",InputSource.MOUSE_BUTTON,1),
            InputBinding("MoveX",InputSource.GAMEPAD_AXIS,0),InputBinding("MoveY",InputSource.GAMEPAD_AXIS,1,-1f),
            InputBinding("MoveX",InputSource.TOUCH),InputBinding("MoveY",InputSource.TOUCH),
            InputBinding("Jump",InputSource.TOUCH),InputBinding("Attack",InputSource.TOUCH))
    }
}
@Serializable enum class ControllerKind { PLATFORMER, TOP_DOWN }
@Serializable data class InputControllerComponent(val kind: ControllerKind=ControllerKind.PLATFORMER, val speed: Float=240f, val jumpSpeed: Float=520f, val horizontal: String="MoveX", val vertical: String="MoveY", val jump: String="Jump") : Component()
@Serializable data class CameraViewport(val x: Float=0f,val y: Float=0f,val width: Float=1f,val height: Float=1f)
@Serializable data class CameraComponent(val follow: String?=null, val smoothing: Float=.15f, val deadZone: Vec2=Vec2(60f,40f), val zoom: Float=1f, val rotation: Float=0f, val limitMin: Vec2?=null, val limitMax: Vec2?=null, val viewport: CameraViewport=CameraViewport(), val enabled: Boolean=true) : Component()
@Serializable data class ParallaxComponent(val factor: Vec2=Vec2(.5f,.5f)) : Component()
data class DebugLine(val start: Vec2,val end: Vec2,val color: Color=Color(.3f,1f,.4f))

inline fun <reified T: Component> Node.component(): T? = components.filterIsInstance<T>().singleOrNull()
fun Node.withComponent(component: Component): Node = copy(components=components.filterNot { it::class==component::class }+component)

/** Shared persistence checks; runtime additionally checks engine-specific body hierarchy constraints. */
fun Scene.validateRuntimeData() {
    require(clips.size<=512 && clips.map { it.id }.distinct().size==clips.size)
    clips.forEach { it.validated() }; val byClip=clips.associateBy { it.id }; val byNode=nodes.associateBy { it.id }
    require(gravity.x.isFinite() && gravity.y.isFinite() && abs(gravity.x)<=100000 && abs(gravity.y)<=100000)
    require(inputMap.bindings.size<=256 && inputMap.bindings.distinct().size==inputMap.bindings.size) { "Use at most 256 distinct input bindings" }
    inputMap.bindings.forEach { require(it.action.isNotBlank() && it.action.length<=64 && it.scale.isFinite() && abs(it.scale)<=10 && it.deadZone in 0f..0.95f && it.code>=0) }
    nodes.forEach { node ->
        require(node.components.map { it::class }.distinct().size==node.components.size) { "Duplicate component on ${node.name}" }
        node.component<AnimatorComponent>()?.let { a ->
            val clip=requireNotNull(byClip[a.clipId]) { "Missing animation ${a.clipId} on ${node.name}" }
            require(a.speed.isFinite() && a.speed in .01f..10f)
            val body=node.component<RigidBodyComponent>()
            if(body!=null) {
                require(clip.tracks.none { it.property==TrackProperty.SCALE }) { "Animate a visual child, not physics body scale" }
                if(body.kind==BodyKind.DYNAMIC) require(clip.tracks.none { it.property in listOf(TrackProperty.POSITION,TrackProperty.ROTATION) }) { "Dynamic bodies own their pose; animate a visual child instead" }
            }
        }
        node.component<RigidBodyComponent>()?.let { body ->
            require(body.colliders.size in 1..32); body.colliders.forEach { it.validated() }
            require(listOf(body.gravityScale,body.linearDamping,body.velocity.x,body.velocity.y,body.angularVelocity).all { it.isFinite() })
            require(abs(body.velocity.x)<=100000f && abs(body.velocity.y)<=100000f && abs(body.angularVelocity)<=36000f) { "Reduce initial body velocity" }
            require(body.gravityScale in -10f..10f && body.linearDamping in 0f..100f)
            require(node.component<ParallaxComponent>()==null) { "Physics bodies cannot use visual parallax" }
        }
        node.component<InputControllerComponent>()?.let {
            val kind=node.component<RigidBodyComponent>()?.kind
            require(kind!=BodyKind.STATIC) { "Movement controllers cannot move static bodies" }
            require(it.kind!=ControllerKind.PLATFORMER || kind==BodyKind.DYNAMIC) { "Platformer controllers require a dynamic rigid body" }
            require(listOf(it.horizontal,it.vertical,it.jump).all { name -> name.isNotBlank() && name.length<=64 });require(it.speed.isFinite() && it.speed in 0f..5000f && it.jumpSpeed.isFinite() && it.jumpSpeed in 0f..5000f) }
        node.component<ParallaxComponent>()?.let { require(it.factor.x.isFinite() && it.factor.y.isFinite()) }
        node.component<CameraComponent>()?.let { c ->
            require(c.follow==null || c.follow in byNode) { "Missing camera follow target" }
            require(c.smoothing in 0f..10f && c.zoom in .05f..20f && c.rotation.isFinite() && c.deadZone.x>=0 && c.deadZone.y>=0 && c.deadZone.x.isFinite() && c.deadZone.y.isFinite())
            require((c.limitMin==null)==(c.limitMax==null))
            if(c.limitMin!=null && c.limitMax!=null)require(c.limitMin.x<c.limitMax.x && c.limitMin.y<c.limitMax.y && listOf(c.limitMin.x,c.limitMin.y,c.limitMax.x,c.limitMax.y).all { it.isFinite() })
            val v=c.viewport
            require(listOf(v.x,v.y,v.width,v.height).all { it.isFinite() } && v.x>=0 && v.y>=0 && v.width>0 && v.height>0 && v.x+v.width<=1.001f && v.y+v.height<=1.001f)
        }
    }
    require(activeCamera==null || byNode[activeCamera]?.component<CameraComponent>()!=null)
    require(joints.size<=1000 && joints.map { it.id }.distinct().size==joints.size)
    joints.forEach { j ->
        require(j.bodyA!=j.bodyB && byNode[j.bodyA]?.component<RigidBodyComponent>()!=null && byNode[j.bodyB]?.component<RigidBodyComponent>()!=null) { "Joint endpoints must be distinct rigid bodies" }
        require(listOf(j.anchorA.x,j.anchorA.y,j.anchorB.x,j.anchorB.y,j.axis.x,j.axis.y,j.length,j.frequency,j.damping,j.motorSpeed,j.maxMotorForce,j.lower,j.upper).all { it.isFinite() })
        require(j.length in .1f..100000f && j.frequency in 0f..100f && j.damping in 0f..1f && j.axis.length()>.001f && j.maxMotorForce in 0f..1000000000f && abs(j.motorSpeed)<=100000f && abs(j.axis.x)<=1000000f && abs(j.axis.y)<=1000000f && j.lower<=j.upper)
    }
}
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
@Serializable data class SpriteComponent(val size: Vec2 = Vec2(96f,96f), val tint: Color = Color(), val asset: String? = null, val assetId: String? = null) : Component()
/** Stable entity identity. Children store local transforms; list order controls painting. */
@Serializable data class Node(val id: String = UUID.randomUUID().toString(), val name: String = "Sprite", val parent: String? = null, val prefab: PrefabBinding? = null, val nestedPrefab: String? = null, val components: List<Component> = listOf(TransformComponent(), SpriteComponent())) {
    val transform get() = components.filterIsInstance<TransformComponent>().single().value
    val sprite get() = components.filterIsInstance<SpriteComponent>().singleOrNull()
    fun moved(p: Vec2) = copy(components=components.map { if (it is TransformComponent) it.copy(value=it.value.copy(position=p)) else it })
}
@Serializable data class Scene(val version: Int = 1, val name: String = "Main", val nodes: List<Node> = emptyList(), val clips: List<AnimationClip> = emptyList(), val joints: List<JointSpec> = emptyList(), val inputMap: InputMap=InputMap(), val gravity: Vec2=Vec2(0f,-980f), val activeCamera: String?=null) {
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
                require(it.sourcePath.isNotBlank() && it.sourcePath.length<=4096 && it.overrides.all { field -> field in setOf("name","transform","sprite","runtime") })
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
        validateRuntimeData()
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
    fun assetReferences(): Set<String> = nodes.flatMap { listOfNotNull(it.sprite?.assetId,it.sprite?.asset?.removePrefix("Sprites/")?.removeSuffix(".png")?.takeIf { id -> isAssetId(id) },it.prefab?.assetId,it.nestedPrefab) }.toSet()+clips.flatMap { it.assets() }
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
````

## engine-core/src/test/kotlin/world/engine/core/RuntimeDataTest.kt

````kotlin
package world.engine.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import world.engine.math.*

class RuntimeDataTest {
    @Test fun inputEdgesAndOpposingKeys() {
        val router=InputRouter(InputMap());router.physical(InputSource.KEY,62,1f)
        assertTrue("Jump" in router.snapshot().pressed);assertTrue(router.snapshot().pressed.isEmpty());router.releaseAll();assertTrue("Jump" in router.snapshot().released)
        router.physical(InputSource.KEY,29,1f);router.physical(InputSource.KEY,32,1f);assertEquals(0f,router.snapshot().value("MoveX"))
    }
    @Test fun quickTapBetweenTicksIsNotLostAndPauseCancelsPendingPress() {
        val router=InputRouter(InputMap());router.touch("Jump",1f);router.touch("Jump",0f)
        val tap=router.snapshot();assertTrue("Jump" in tap.pressed);assertTrue("Jump" in tap.released);assertEquals(0f,tap.value("Jump"))
        router.touch("Jump",1f);router.releaseAll();assertFalse("Jump" in router.snapshot().pressed)
    }
    @Test fun unpluggingOneDevicePreservesAnother() {
        val router=InputRouter(InputMap());router.physical(InputSource.KEY,62,1f,1);router.physical(InputSource.KEY,62,1f,2);router.snapshot()
        router.releaseDevice(1);assertEquals(1f,router.snapshot().value("Jump"));router.releaseDevice(2);assertTrue("Jump" in router.snapshot().released)
    }
    @Test fun runtimeComponentsAndInternalPrefabJointRoundTrip() {
        val root=Node(components=listOf(TransformComponent()))
        val a=Node(parent=root.id,components=listOf(TransformComponent(),RigidBodyComponent(kind=BodyKind.STATIC)))
        val b=Node(parent=root.id,components=listOf(TransformComponent(Transform(Vec2(0f,-100f))),RigidBodyComponent(),InputControllerComponent()))
        val scene=Scene(nodes=listOf(root,a,b),joints=listOf(JointSpec(bodyA=a.id,bodyB=b.id))).validated()
        assertEquals(scene,Json.decodeFromString<Scene>(Json.encodeToString(scene)).validated())
        val id=java.util.UUID.randomUUID().toString();val definition=Prefabs.capture(scene,root.id);val bundle=Prefabs.bundle(id,mapOf(id to definition))
        assertEquals(1,bundle.joints.size);assertNotEquals(scene.joints.single().bodyA,bundle.joints.single().bodyA);bundle.validated()
    }
    @Test fun selfIntersectingStarIsNotAConvexPolygon() {
        val points=(0..4).map { val a=it*4*Math.PI/5;Vec2((kotlin.math.cos(a)*50).toFloat(),(kotlin.math.sin(a)*50).toFloat()) }
        assertThrows(IllegalArgumentException::class.java){Collider(shape=ShapeKind.POLYGON,vertices=points).validated()}
    }
    @Test fun dynamicPoseAnimationAndBadCollidersAreRejected() {
        val clip=AnimationClip(tracks=listOf(AnimationTrack(TrackProperty.POSITION,listOf(AnimationKey(0,listOf(0f,0f))))))
        val n=Node(components=listOf(TransformComponent(),RigidBodyComponent(),AnimatorComponent(clip.id)))
        assertThrows(IllegalArgumentException::class.java){Scene(nodes=listOf(n),clips=listOf(clip)).validated()}
        assertThrows(IllegalArgumentException::class.java){Collider(shape=ShapeKind.CAPSULE,size=Vec2(100f,20f)).validated()}
    }
}
````

## engine-physics/README.md

````markdown
# engine-physics — Phase 3

Actual JBox2D 2.2.1.1, 100 pixels/metre, 60 fixed steps/second, eight velocity and three position iterations. Provides static/dynamic/kinematic bodies; rectangle/circle/convex polygon/capsule/compound fixtures; materials, sensors, aggregated contact events, grounding, AABB queries, raycasts and seven joint kinds. GL debug data contains fixture boundaries and joint anchors.

`ColliderGeometry` constructs convex hulls and real Bitmap-alpha approximations. Alpha holes/concavities are filled; reduction is limited to eight vertices and may shrink the hull. Physics ancestors need uniform non-reflected scale. Nonuniform circles become octagons. Tiny fixtures are rejected before JBox2D can substitute fallback geometry.

Preview limits are 512 bodies and 4096 collider definitions. Scene validation caps each body at 32 collider definitions and scenes at 1000 joints. Compound capsules create multiple fixtures. These are safety ceilings, not verified mobile performance targets. Body centres must remain within ±1,000,000 pixels. See `docs/PHASE3.md`.
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
    testOptions { unitTests.all { it.useJUnitPlatform() } }
}
dependencies {
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    api(project(":engine-core"))
    implementation("org.jbox2d:jbox2d-library:2.2.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}
````

## engine-physics/src/androidTest/kotlin/world/engine/physics/AlphaColliderTest.kt

````kotlin
package world.engine.physics

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import world.engine.math.Vec2

class AlphaColliderTest {
    @Test fun transparentImageProducesFittedLocalPixelHull() {
        val bitmap=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888)
        try {
            for(y in 3..12)for(x in 2..13)bitmap.setPixel(x,y,Color.WHITE)
            val hull=ColliderGeometry.alpha(bitmap,Vec2(64f,64f));assertEquals(4,hull.vertices.size)
            assertEquals(-24f,hull.vertices.minOf { it.x },.01f);assertEquals(24f,hull.vertices.maxOf { it.x },.01f)
            assertEquals(-20f,hull.vertices.minOf { it.y },.01f);assertEquals(20f,hull.vertices.maxOf { it.y },.01f)
        } finally { bitmap.recycle() }
    }
}
````

## engine-physics/src/main/kotlin/world/engine/physics/ColliderGeometry.kt

````kotlin
package world.engine.physics

import android.graphics.Bitmap
import world.engine.core.*
import world.engine.math.*
import kotlin.math.*

/** Convex geometry is also used by the editor before a Box2D world exists. */
object ColliderGeometry {
    fun hull(points: List<Vec2>,limit: Int=8): List<Vec2> {
        require(limit in 3..8 && points.size<=20000 && points.all { it.x.isFinite() && it.y.isFinite() })
        val sorted=points.distinct().sortedWith(compareBy<Vec2> { it.x }.thenBy { it.y })
        fun cross(a: Vec2,b: Vec2,c: Vec2)=(b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x)
        fun half(input: List<Vec2>): List<Vec2> {
            val result=mutableListOf<Vec2>()
            input.forEach { p -> while(result.size>=2 && cross(result[result.lastIndex-1],result.last(),p)<=.001f)result.removeAt(result.lastIndex);result.add(p) }
            return result.dropLast(1)
        }
        val hull=(half(sorted)+half(sorted.asReversed())).toMutableList()
        require(hull.size>=3) { "Need at least three non-collinear points" }
        while(hull.size>limit) {
            val index=hull.indices.minBy { i -> abs(cross(hull[(i-1+hull.size)%hull.size],hull[i],hull[(i+1)%hull.size])) }
            hull.removeAt(index)
        }
        return hull
    }
    /** Alpha hull approximation: concavities/holes are filled; at most eight hull vertices. */
    fun alpha(bitmap: Bitmap,size: Vec2,threshold: Int=16): Collider {
        require(bitmap.width<=2048 && bitmap.height<=2048 && threshold in 1..255)
        val row=IntArray(bitmap.width); val points=mutableListOf<Vec2>()
        fun local(x: Int,y: Int)=Vec2((x.toFloat()/bitmap.width-.5f)*size.x,(.5f-y.toFloat()/bitmap.height)*size.y)
        for(y in 0 until bitmap.height) {
            bitmap.getPixels(row,0,bitmap.width,0,y,bitmap.width,1)
            val left=row.indexOfFirst { (it ushr 24)>=threshold };val right=row.indexOfLast { (it ushr 24)>=threshold }
            if(left>=0) { points.add(local(left,y));points.add(local(right+1,y));points.add(local(left,y+1));points.add(local(right+1,y+1)) }
        }
        return Collider(shape=ShapeKind.POLYGON,size=size,vertices=hull(points)).validated()
    }
    fun polygons(c: Collider): List<List<Vec2>> {
        fun rect(w: Float,h: Float)=listOf(Vec2(-w/2,-h/2),Vec2(w/2,-h/2),Vec2(w/2,h/2),Vec2(-w/2,h/2))
        fun circle(center: Vec2,r: Float)=List(24) { i -> val a=i*2*PI/24;center+Vec2(cos(a).toFloat()*r,sin(a).toFloat()*r) }
        val raw=when(c.shape) {
            ShapeKind.RECTANGLE -> listOf(rect(c.size.x,c.size.y))
            ShapeKind.POLYGON -> listOf(c.vertices)
            ShapeKind.CIRCLE -> listOf(circle(Vec2(),c.radius))
            ShapeKind.CAPSULE -> {
                val r=c.size.x/2;val d=(c.size.y-c.size.x)/2
                listOfNotNull(if(d>.001f)rect(c.size.x,d*2) else null,circle(Vec2(0f,d),r),if(d>.001f)circle(Vec2(0f,-d),r) else null)
            }
        }
        return raw.map { vertices -> vertices.map { it+c.offset } }
    }
    fun outlines(scene: Scene): List<DebugLine> {
        val matrices=scene.worldMatrices()
        return scene.nodes.flatMap { n -> n.component<RigidBodyComponent>()?.colliders.orEmpty().flatMap { c ->
            val color=if(c.sensor)Color(.2f,.8f,1f) else Color(.3f,1f,.4f)
            polygons(c).flatMap { polygon -> polygon.indices.map { i -> DebugLine(matrices.getValue(n.id).map(polygon[i]),matrices.getValue(n.id).map(polygon[(i+1)%polygon.size]),color) } }
        } }
    }
}
````

## engine-physics/src/main/kotlin/world/engine/physics/PhysicsWorld.kt

````kotlin
package world.engine.physics

import org.jbox2d.callbacks.*
import org.jbox2d.collision.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.dynamics.*
import org.jbox2d.dynamics.contacts.Contact
import org.jbox2d.dynamics.joints.*
import org.jbox2d.common.Vec2 as BoxVec
import world.engine.core.*
import world.engine.math.*
import kotlin.math.*

data class CollisionEvent(val a: String,val b: String,val sensor: Boolean,val entered: Boolean)
data class RayHit(val entity: String,val point: Vec2,val normal: Vec2,val fraction: Float)
/** Single-thread-owned JBox2D world. Engine pixels are converted at 100 pixels per metre. */
class PhysicsWorld(initial: Scene,private val onContact: (CollisionEvent)->Unit = {}) {
    companion object { const val PIXELS_PER_METRE=100f; const val STEP=1f/60f }
    private val world=World(box(initial.gravity))
    private val bodies=linkedMapOf<String,Body>()
    private val contactCounts=mutableMapOf<Triple<String,String,Boolean>,Int>()
    private val events=ArrayDeque<CollisionEvent>()
    private var grounded=emptySet<String>()
    val bodyCount get() = world.bodyCount
    val jointCount get() = world.jointCount
    init {
        initial.validated()
        require(initial.nodes.count { it.component<RigidBodyComponent>()!=null }<=512) { "Preview supports at most 512 rigid bodies" }
        require(initial.nodes.sumOf { it.component<RigidBodyComponent>()?.colliders?.size ?: 0 }<=4096) { "Too many colliders for preview" }
        val matrices=initial.worldMatrices()
        initial.nodes.forEach { n -> n.component<RigidBodyComponent>()?.let { config ->
            var parent=n.parent
            while(parent!=null) {
                val ancestor=initial.nodes.single { it.id==parent }; val m=matrices.getValue(ancestor.id).values
                val x=hypot(m[0],m[3]);val y=hypot(m[1],m[4]);val dot=m[0]*m[1]+m[3]*m[4]
                require(abs(x-y)<.001f*maxOf(1f,x) && abs(dot)<.001f*maxOf(1f,x*y) && m[0]*m[4]-m[1]*m[3]>0) { "Body ${n.name}: ancestors need uniform, non-reflected scale" }
                ancestor.component<AnimatorComponent>()?.let { a -> require(initial.clips.single { it.id==a.clipId }.tracks.none { it.property==TrackProperty.SCALE }) { "Cannot animate physics ancestor scale" } }
                parent=ancestor.parent
            }
            val matrix=matrices.getValue(n.id); val parentAngle=n.parent?.let { angle(matrices.getValue(it)) } ?: 0f
            val rotation=parentAngle+radians(n.transform.rotation)
            val definition=BodyDef().apply {
                type=when(config.kind) { BodyKind.STATIC->BodyType.STATIC;BodyKind.DYNAMIC->BodyType.DYNAMIC;BodyKind.KINEMATIC->BodyType.KINEMATIC }
                position.set(box(matrix.map(Vec2())));this.angle=rotation;gravityScale=config.gravityScale;fixedRotation=config.fixedRotation;bullet=config.bullet;linearDamping=config.linearDamping
                linearVelocity.set(box(config.velocity));angularVelocity=radians(config.angularVelocity)
            }
            checkPose(definition.position,definition.angle)
            val body=world.createBody(definition);body.userData=n.id;bodies[n.id]=body
            config.colliders.forEachIndexed { index,c ->
                fun point(p: Vec2): BoxVec = body.getLocalPoint(box(matrix.map(p))).also { require(it.x in -10000f..10000f && it.y in -10000f..10000f) { "Collider on ${n.name} is too large or offset too far" } }
                fun polygon(vertices: List<Vec2>) {
                    val points=vertices.map(::point)
                    require(points.indices.all { a -> (a+1 until points.size).all { b -> points[a].sub(points[b]).lengthSquared()>=.000025f } }) { "Collider on ${n.name} has vertices less than 0.5 world pixels apart; increase its size" }
                    val twiceArea=points.indices.sumOf { i -> val a=points[i];val b=points[(i+1)%points.size];(a.x*b.y-a.y*b.x).toDouble() }
                    require(abs(twiceArea)>.000002) { "Collider on ${n.name} has negligible area; draw a larger convex polygon" }
                    val shape=PolygonShape();shape.set(points.toTypedArray(),points.size);fixture(body,shape,c,index)
                }
                fun circle(center: Vec2,radius: Float) {
                    val values=matrix.values;val sx=hypot(values[0],values[3]);val sy=hypot(values[1],values[4])
                    if(abs(sx-sy)<.0001f*maxOf(1f,sx)) {
                        val shape=CircleShape();shape.m_p.set(point(center));shape.m_radius=radius*sx/PIXELS_PER_METRE;require(shape.m_radius in .005f..10000f) { "Circle radius must be 0.5–1000000 world pixels" };fixture(body,shape,c,index)
                    } else polygon(List(8) { i -> val a=i*2*PI/8; center+Vec2(cos(a).toFloat()*radius,sin(a).toFloat()*radius) })
                }
                when(c.shape) {
                    ShapeKind.CIRCLE -> circle(c.offset,c.radius)
                    ShapeKind.CAPSULE -> {
                        val radius=c.size.x/2;val half=(c.size.y-c.size.x)/2
                        if(half>.001f) polygon(listOf(Vec2(-radius,-half),Vec2(radius,-half),Vec2(radius,half),Vec2(-radius,half)).map { it+c.offset })
                        circle(c.offset+Vec2(0f,half),radius);if(half>.001f)circle(c.offset+Vec2(0f,-half),radius)
                    }
                    else -> polygon(ColliderGeometry.polygons(c).single())
                }
            }
        } }
        initial.joints.forEach { createJoint(it,matrices) }
        world.setContactListener(object: ContactListener {
            override fun beginContact(contact: Contact) = queue(contact,true)
            override fun endContact(contact: Contact) = queue(contact,false)
            override fun preSolve(contact: Contact,oldManifold: Manifold) = Unit
            override fun postSolve(contact: Contact,impulse: ContactImpulse) = Unit
        })
    }
    private fun fixture(body: Body,shape: Shape,c: Collider,index: Int) {
        body.createFixture(FixtureDef().apply { this.shape=shape;density=c.density;friction=c.friction;restitution=c.restitution;isSensor=c.sensor;userData=index })
    }
    private fun queue(contact: Contact,begin: Boolean) {
        val ids=listOf(contact.fixtureA.body.userData as String,contact.fixtureB.body.userData as String).sorted()
        val key=Triple(ids[0],ids[1],contact.fixtureA.isSensor || contact.fixtureB.isSensor)
        val old=contactCounts[key] ?: 0;val count=(old+if(begin)1 else -1).coerceAtLeast(0)
        if(count==0)contactCounts.remove(key) else contactCounts[key]=count
        if((begin && old==0) || (!begin && old>0 && count==0))events.add(CollisionEvent(key.first,key.second,key.third,begin))
    }
    private fun createJoint(j: JointSpec,matrices: Map<String,Mat3>) {
        val a=bodies.getValue(j.bodyA);val b=bodies.getValue(j.bodyB)
        val pa=box(matrices.getValue(j.bodyA).map(j.anchorA));val pb=box(matrices.getValue(j.bodyB).map(j.anchorB))
        val axis=a.getWorldVector(BoxVec(j.axis.x,j.axis.y));axis.normalize()
        val definition: JointDef=when(j.kind) {
            JointKind.FIXED -> WeldJointDef().apply { initialize(a,b,pa);localAnchorB.set(b.getLocalPoint(pb)) }
            JointKind.DISTANCE,JointKind.SPRING -> DistanceJointDef().apply { initialize(a,b,pa,pb);length=j.length/PIXELS_PER_METRE;frequencyHz=if(j.kind==JointKind.SPRING)j.frequency else 0f;dampingRatio=j.damping }
            JointKind.REVOLUTE -> RevoluteJointDef().apply { initialize(a,b,pa);localAnchorB.set(b.getLocalPoint(pb));enableMotor=j.motor;motorSpeed=radians(j.motorSpeed);maxMotorTorque=j.maxMotorForce;enableLimit=j.limit;lowerAngle=radians(j.lower);upperAngle=radians(j.upper) }
            JointKind.PRISMATIC -> PrismaticJointDef().apply { initialize(a,b,pa,axis);localAnchorB.set(b.getLocalPoint(pb));enableMotor=j.motor;motorSpeed=j.motorSpeed/PIXELS_PER_METRE;maxMotorForce=j.maxMotorForce;enableLimit=j.limit;lowerTranslation=j.lower/PIXELS_PER_METRE;upperTranslation=j.upper/PIXELS_PER_METRE }
            JointKind.WHEEL -> WheelJointDef().apply { initialize(a,b,pa,axis);localAnchorB.set(b.getLocalPoint(pb));enableMotor=j.motor;motorSpeed=radians(j.motorSpeed);maxMotorTorque=j.maxMotorForce;frequencyHz=j.frequency;dampingRatio=j.damping }
            JointKind.ROPE -> RopeJointDef().apply { bodyA=a;bodyB=b;localAnchorA.set(a.getLocalPoint(pa));localAnchorB.set(b.getLocalPoint(pb));maxLength=j.length/PIXELS_PER_METRE }
        }
        definition.collideConnected=j.collideConnected;world.createJoint(definition)
    }
    /** Current world velocity in pixels per second; missing bodies return zero. */
    fun velocity(id: String): Vec2 = bodies[id]?.linearVelocity?.let(::pixels) ?: Vec2()
    /** Changes a body velocity in world pixels per second and wakes it. */
    fun setVelocity(id: String,velocity: Vec2) { require(velocity.x.isFinite() && velocity.y.isFinite());bodies[id]?.let { it.linearVelocity=box(velocity);it.isAwake=true } }
    /** Applies a world impulse in kilogram-pixels per second at the centre of mass. */
    fun impulse(id: String,impulse: Vec2) { require(impulse.x.isFinite() && impulse.y.isFinite());bodies[id]?.let { it.applyLinearImpulse(box(impulse),it.worldCenter) } }
    /** True after the latest step when a solid contact supports the body from below. */
    fun isGrounded(id: String) = id in grounded
    /** Steps 1/60 second, then returns new local transforms without mutating the supplied scene. */
    fun step(scene: Scene): Scene {
        val matrices=scene.worldMatrices()
        scene.nodes.forEach { n -> bodies[n.id]?.let { body ->
            if(body.type!=BodyType.DYNAMIC) {
                val position=box(matrices.getValue(n.id).map(Vec2()));val rotation=radians(n.transform.rotation)+(n.parent?.let { angle(matrices.getValue(it)) } ?: 0f)
                checkPose(position,rotation)
                val animated=generateSequence(n) { current -> current.parent?.let { id -> scene.nodes.find { it.id==id } } }.any { it.component<AnimatorComponent>()!=null }
                if(body.type==BodyType.STATIC)body.setTransform(position,rotation)
                else if(animated) { body.linearVelocity=position.sub(body.position).mul(1f/STEP);body.angularVelocity=(rotation-body.angle)/STEP }
            }
        } }
        world.step(STEP,8,3)
        val supported=mutableSetOf<String>();var contact=world.contactList
        while(contact!=null) {
            if(contact.isTouching && contact.isEnabled && !contact.fixtureA.isSensor && !contact.fixtureB.isSensor) {
                val manifold=WorldManifold();contact.getWorldManifold(manifold)
                if(manifold.normal.y<-.4f)supported.add(contact.fixtureA.body.userData as String)
                if(manifold.normal.y>.4f)supported.add(contact.fixtureB.body.userData as String)
            }
            contact=contact.next
        }
        grounded=supported
        while(events.isNotEmpty())onContact(events.removeFirst())
        val changed=scene.nodes.associateBy { it.id }.toMutableMap();val worlds=mutableMapOf<String,Mat3>()
        fun update(id: String): Mat3 = worlds.getOrPut(id) {
            var node=changed.getValue(id);val parent=node.parent?.let(::update) ?: Mat3.Identity
            bodies[id]?.let { body ->
                checkPose(body.position,body.angle)
                node=node.withComponent(TransformComponent(node.transform.copy(position=parent.inverse().map(pixels(body.position)),rotation=degrees(body.angle-angle(parent)))))
                changed[id]=node
            }
            parent*node.transform.matrix()
        }
        scene.nodes.forEach { update(it.id) }
        return scene.copy(nodes=scene.nodes.map { changed.getValue(it.id) })
    }
    /** World-space fixture outlines and joint anchor connectors for the GL debug pass. */
    fun debugLines(): List<DebugLine> = bodies.values.flatMap { body ->
        val lines=mutableListOf<DebugLine>();var fixture=body.fixtureList
        while(fixture!=null) {
            val color=if(fixture.isSensor)Color(.2f,.8f,1f) else if(body.type==BodyType.STATIC)Color(.8f,.8f,.8f) else Color(.3f,1f,.4f)
            val vertices=when(val shape=fixture.shape) {
                is PolygonShape -> (0 until shape.m_count).map { pixels(body.getWorldPoint(shape.m_vertices[it])) }
                is CircleShape -> List(24) { i -> val a=i*2*PI/24;pixels(body.getWorldPoint(shape.m_p.add(BoxVec(cos(a).toFloat()*shape.m_radius,sin(a).toFloat()*shape.m_radius)))) }
                else -> emptyList()
            }
            vertices.indices.forEach { i -> lines.add(DebugLine(vertices[i],vertices[(i+1)%vertices.size],color)) };fixture=fixture.next
        };lines
    } + buildList {
        var joint=world.jointList
        while(joint!=null) { val a=BoxVec();val b=BoxVec();joint.getAnchorA(a);joint.getAnchorB(b);add(DebugLine(pixels(a),pixels(b),Color(1f,.8f,.2f)));joint=joint.next }
    }
    /** Broad-phase AABB query; callers needing precise overlap should narrow the returned set. */
    fun query(rect: Rect): Set<String> {
        require(listOf(rect.center.x,rect.center.y,rect.size.x,rect.size.y).all { it.isFinite() } && rect.size.x>0 && rect.size.y>0)
        val hits=mutableSetOf<String>();val half=rect.size*.5f
        world.queryAABB(QueryCallback { fixture -> hits.add(fixture.body.userData as String);true },AABB(box(rect.center-half),box(rect.center+half)))
        return hits
    }
    /** Closest intersection along a nonzero segment, excluding sensors unless requested. */
    fun raycast(from: Vec2,to: Vec2,includeSensors: Boolean=false): RayHit? {
        require(listOf(from.x,from.y,to.x,to.y).all { it.isFinite() } && (to-from).length()>.001f) { "Ray must have positive length" }
        var result: RayHit?=null
        world.raycast(RayCastCallback { fixture,point,normal,fraction ->
            if(fixture.isSensor && !includeSensors)-1f else { result=RayHit(fixture.body.userData as String,pixels(point),Vec2(normal.x,normal.y),fraction);fraction }
        },box(from),box(to))
        return result
    }
    private fun checkPose(position: BoxVec,angle: Float) {
        require(position.x in -10000f..10000f && position.y in -10000f..10000f && angle.isFinite()) { "Physics left the safe ±1000000 pixel world range; reduce velocities, forces or scene scale" }
    }
    private fun box(v: Vec2)=BoxVec(v.x/PIXELS_PER_METRE,v.y/PIXELS_PER_METRE)
    private fun pixels(v: BoxVec)=Vec2(v.x*PIXELS_PER_METRE,v.y*PIXELS_PER_METRE)
    private fun angle(m: Mat3)=atan2(m.values[3],m.values[0])
    private fun radians(degrees: Float)=degrees*(PI.toFloat()/180f)
    private fun degrees(radians: Float)=radians*(180f/PI.toFloat())
}
````

## engine-physics/src/test/kotlin/world/engine/physics/PhysicsWorldTest.kt

````kotlin
package world.engine.physics

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import world.engine.core.*
import world.engine.math.*

class PhysicsWorldTest {
    private fun body(p: Vec2,kind: BodyKind=BodyKind.DYNAMIC,sensor: Boolean=false)=Node(components=listOf(TransformComponent(Transform(p)),RigidBodyComponent(kind=kind,colliders=listOf(Collider(size=Vec2(40f,40f),sensor=sensor)),fixedRotation=true)))
    @Test fun realBodyFallsAndContactsGround() {
        val floor=body(Vec2(0f,-50f),BodyKind.STATIC);val actor=body(Vec2(0f,100f));var scene=Scene(nodes=listOf(floor,actor))
        val contacts=mutableListOf<CollisionEvent>();val world=PhysicsWorld(scene,contacts::add)
        repeat(300){scene=world.step(scene)}
        assertTrue(world.isGrounded(actor.id));assertTrue(contacts.any { it.entered && !it.sensor })
        assertTrue(scene.nodes.last().transform.position.y in -12f..-6f)
        assertEquals(actor.id,world.raycast(Vec2(0f,200f),Vec2(0f,-100f))?.entity)
        assertTrue(world.query(Rect(Vec2(),Vec2(100f,100f))).contains(actor.id))
    }
    @Test fun sevenJointImplementationsCreateAndStep() {
        JointKind.entries.forEach { kind ->
            val a=body(Vec2(),BodyKind.STATIC);val b=body(Vec2(0f,-100f));var scene=Scene(nodes=listOf(a,b),joints=listOf(JointSpec(kind=kind,bodyA=a.id,bodyB=b.id)))
            val world=PhysicsWorld(scene);repeat(30){scene=world.step(scene)};assertEquals(1,world.jointCount);assertTrue(scene.nodes.last().transform.position.y.isFinite())
        }
    }
    @Test fun everyShapeAndCompoundCreatesRealFixtures() {
        val colliders=listOf(Collider(),Collider(shape=ShapeKind.CIRCLE),Collider(shape=ShapeKind.CAPSULE,size=Vec2(20f,60f)),Collider(shape=ShapeKind.POLYGON,vertices=listOf(Vec2(-10f,-10f),Vec2(10f,-10f),Vec2(0f,10f))))
        val node=Node(components=listOf(TransformComponent(),RigidBodyComponent(colliders=colliders)))
        val scene=Scene(nodes=listOf(node));val world=PhysicsWorld(scene);assertEquals(1,world.bodyCount);assertTrue(world.debugLines().size>20);world.step(scene)
    }
    @Test fun tinyFixturesAreRejectedInsteadOfSilentlyReplacedByBox2D() {
        val node=Node(components=listOf(TransformComponent(),RigidBodyComponent(colliders=listOf(Collider(size=Vec2(.1f,.1f))))))
        assertThrows(IllegalArgumentException::class.java){PhysicsWorld(Scene(nodes=listOf(node)))}
    }
    @Test fun unsafeBodyHierarchyIsRejectedAndConvexHullIsBounded() {
        val parent=Node(components=listOf(TransformComponent(Transform(scale=Vec2(2f,1f)))))
        val child=body(Vec2()).copy(parent=parent.id)
        assertThrows(IllegalArgumentException::class.java){PhysicsWorld(Scene(nodes=listOf(parent,child)))}
        val points=(0..31).map { val angle=it*Math.PI/16;Vec2((kotlin.math.cos(angle)*20).toFloat(),(kotlin.math.sin(angle)*20).toFloat()) }
        val hull=ColliderGeometry.hull(points);assertEquals(8,hull.size);Collider(shape=ShapeKind.POLYGON,vertices=hull).validated()
    }
}
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
    testOptions { unitTests.all { it.useJUnitPlatform() } }
}
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    api(project(":engine-core"))
}
````

## engine-render/src/main/kotlin/world/engine/render/Camera2D.kt

````kotlin
package world.engine.render

import world.engine.math.Vec2
import kotlin.math.*

/** Immutable view transform. Rotation is degrees counter-clockwise in world space. */
data class Camera2D(val center: Vec2=Vec2(),val zoom: Float=1f,val rotation: Float=0f) {
    fun screenToWorld(x: Float,y: Float,width: Int,height: Int): Vec2 {
        val dx=(x-width/2f)/zoom;val dy=(height/2f-y)/zoom;val a=rotation*PI/180
        return center+Vec2((cos(a)*dx-sin(a)*dy).toFloat(),(sin(a)*dx+cos(a)*dy).toFloat())
    }
    fun worldToClip(p: Vec2,width: Int,height: Int): Vec2 {
        val d=p-center;val a=-rotation*PI/180
        return Vec2((cos(a)*d.x-sin(a)*d.y).toFloat()*zoom*2/width.coerceAtLeast(1),(sin(a)*d.x+cos(a)*d.y).toFloat()*zoom*2/height.coerceAtLeast(1))
    }
}
````

## engine-render/src/main/kotlin/world/engine/render/CameraRig.kt

````kotlin
package world.engine.render

import world.engine.core.*
import world.engine.math.*
import kotlin.math.*

data class CameraView(val id: String,val camera: Camera2D,val viewport: CameraViewport=CameraViewport())
enum class ShakePreset(val amplitude: Float,val seconds: Float) {
    SMALL(3f,.15f),MEDIUM(8f,.3f),LARGE(18f,.5f),EXPLOSION(28f,.6f),DAMAGE(10f,.2f),EARTHQUAKE(12f,2f)
}
/** A single-thread camera system: dead zone, exponential follow, split views, transitions and shake. */
class CameraRig(initialActive: String?=null) {
    private val states=mutableMapOf<String,Camera2D>()
    private var focus=initialActive
    private var previous: Camera2D?=null
    private var transition=0f
    private var transitionDuration=.4f
    private var last=emptyList<CameraView>()
    private var shake: ShakePreset?=null
    private var shakeTime=0f
    fun focus(id: String?,seconds: Float=.4f) { require(seconds.isFinite() && seconds in 0f..10f);previous=last.firstOrNull()?.camera;focus=id;transition=0f;transitionDuration=seconds }
    fun shake(preset: ShakePreset) { shake=preset;shakeTime=0f }
    fun update(scene: Scene,dt: Float,width: Int,height: Int): List<CameraView> {
        require(dt.isFinite() && dt>=0f)
        val matrices=scene.worldMatrices();val cameras=scene.nodes.filter { it.component<CameraComponent>()?.enabled==true }
        val visible=if(focus==null)cameras else cameras.filter { it.id==focus }
        transition+=dt;shakeTime+=dt
        val result=visible.map { n ->
            val config=n.component<CameraComponent>()!!;val m=matrices.getValue(n.id)
            val authored=Camera2D(m.map(Vec2()),config.zoom,config.rotation+atan2(m.values[3],m.values[0])*180f/PI.toFloat())
            val old=states[n.id] ?: authored;val target=config.follow?.let { matrices[it]?.map(Vec2()) }
            var desired=old.center
            if(target!=null) {
                val delta=target-old.center;val r=-authored.rotation*PI/180
                val local=Vec2((cos(r)*delta.x-sin(r)*delta.y).toFloat(),(sin(r)*delta.x+cos(r)*delta.y).toFloat())
                val correction=Vec2(local.x-local.x.coerceIn(-config.deadZone.x/2,config.deadZone.x/2),local.y-local.y.coerceIn(-config.deadZone.y/2,config.deadZone.y/2))
                desired+=Vec2((cos(-r)*correction.x-sin(-r)*correction.y).toFloat(),(sin(-r)*correction.x+cos(-r)*correction.y).toFloat())
            } else desired=authored.center
            val alpha=if(config.smoothing==0f || n.id !in states)1f else 1f-exp(-dt/config.smoothing)
            var camera=constrain(authored.copy(center=old.center+(desired-old.center)*alpha),config,width,height)
            states[n.id]=camera
            previous?.let { from ->
                val t=if(transitionDuration==0f)1f else (transition/transitionDuration).coerceIn(0f,1f);val eased=t*t*(3-2*t)
                val angle=((camera.rotation-from.rotation+540f)%360f)-180f
                camera=Camera2D(from.center+(camera.center-from.center)*eased,from.zoom+(camera.zoom-from.zoom)*eased,from.rotation+angle*eased)
            }
            shake?.let { s -> if(shakeTime<s.seconds) {
                val amplitude=s.amplitude*(1f-shakeTime/s.seconds);val seed=(n.id.hashCode() and 255)*.1f
                camera=camera.copy(center=camera.center+Vec2(sin(shakeTime*137f+seed)*amplitude,cos(shakeTime*173f+seed)*amplitude))
            } }
            camera=constrain(camera,config,width,height)
            CameraView(n.id,camera,config.viewport)
        }
        if(transition>=transitionDuration)previous=null
        if(shakeTime>=(shake?.seconds ?: 0f))shake=null
        last=if(result.isEmpty())listOf(CameraView("editor-default",Camera2D())) else result
        return last
    }
    private fun constrain(camera: Camera2D,config: CameraComponent,width: Int,height: Int): Camera2D {
        val low=config.limitMin ?: return camera;val high=config.limitMax ?: return camera
        val r=camera.rotation*PI/180;val halfX=width*config.viewport.width/(2*camera.zoom);val halfY=height*config.viewport.height/(2*camera.zoom)
        val extentX=(abs(cos(r))*halfX+abs(sin(r))*halfY).toFloat();val extentY=(abs(sin(r))*halfX+abs(cos(r))*halfY).toFloat()
        fun clamp(value: Float,min: Float,max: Float,extent: Float)=if(max-min<2*extent)(min+max)/2 else value.coerceIn(min+extent,max-extent)
        return camera.copy(center=Vec2(clamp(camera.center.x,low.x,high.x,extentX),clamp(camera.center.y,low.y,high.y,extentY)))
    }
}
````

## engine-render/src/main/kotlin/world/engine/render/SceneRenderer.kt

````kotlin
package world.engine.render

import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import world.engine.core.*
import world.engine.math.*
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.floor

data class RenderFrame(val scene: Scene=Scene(), val selected: String?=null, val camera: Camera2D=Camera2D(), val directory: File?=null, val assetPaths: Map<String,String> = emptyMap(), val views: List<CameraView> = emptyList(), val debugLines: List<DebugLine> = emptyList(), val grid: Boolean=true)
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
            val matrices=f.scene.worldMatrices()
            val selection=f.selected?.let { f.scene.descendants(it) }.orEmpty()
            val views=f.views.ifEmpty { listOf(CameraView("editor",f.camera)) }
            glEnable(GL_SCISSOR_TEST)
            views.forEach { view ->
                val bounds=view.viewport
                val w=(width*bounds.width).toInt().coerceAtLeast(1);val h=(height*bounds.height).toInt().coerceAtLeast(1)
                val x=(width*bounds.x).toInt();val y=height-(height*(bounds.y+bounds.height)).toInt()
                glViewport(x,y,w,h);glScissor(x,y,w,h);glClear(GL_COLOR_BUFFER_BIT)
                val camera=view.camera
                batch.begin(camera,w,h)
                if(f.grid) {
                    val corners=listOf(camera.screenToWorld(0f,0f,w,h),camera.screenToWorld(w.toFloat(),0f,w,h),camera.screenToWorld(0f,h.toFloat(),w,h),camera.screenToWorld(w.toFloat(),h.toFloat(),w,h))
                    val a=Vec2(corners.minOf { it.x },corners.minOf { it.y });val b=Vec2(corners.maxOf { it.x },corners.maxOf { it.y })
                    val spacing=if(camera.zoom<.5f)256f else 64f;val color=Color(.14f,.16f,.19f)
                    var gx=floor(a.x/spacing)*spacing
                    repeat(minOf(2000,((b.x-gx)/spacing).toInt().coerceAtLeast(0)+1)) { batch.line(textures.white,Vec2(gx,a.y),Vec2(gx,b.y),color);gx+=spacing }
                    var gy=floor(a.y/spacing)*spacing
                    repeat(minOf(2000,((b.y-gy)/spacing).toInt().coerceAtLeast(0)+1)) { batch.line(textures.white,Vec2(a.x,gy),Vec2(b.x,gy),color);gy+=spacing }
                }
                f.scene.nodes.forEach { n -> n.sprite?.let { sprite ->
                    var matrix=matrices.getValue(n.id)
                    n.component<ParallaxComponent>()?.let { layer ->
                        val values=matrix.values.toMutableList();values[2]+=camera.center.x*(1-layer.factor.x);values[5]+=camera.center.y*(1-layer.factor.y);matrix=Mat3(values)
                    }
                    val ref=sprite.assetId ?: sprite.asset?.removePrefix("Sprites/")?.removeSuffix(".png")
                    val path=ref?.let { f.assetPaths[it] } ?: sprite.asset ?: sprite.assetId?.let { "Sprites/$it-missing.png" }
                    if(n.id in selection)batch.drawMatrix(textures.white,matrix,sprite.size+Vec2(8/camera.zoom,8/camera.zoom),Color(1f,.75f,.15f))
                    batch.drawMatrix(textures.get(root,path),matrix,sprite.size,sprite.tint)
                } }
                f.debugLines.forEach { batch.line(textures.white,it.start,it.end,it.color) }
                batch.flush()
            }
            glDisable(GL_SCISSOR_TEST);glViewport(0,0,width,height)
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
    private var primitive=GL_TRIANGLES
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
        if(texture!=id || data.remaining()<48 || primitive!=GL_TRIANGLES) flush()
        primitive=GL_TRIANGLES;texture=id
        val corners=listOf(Vec2(-size.x/2,-size.y/2),Vec2(size.x/2,-size.y/2),Vec2(size.x/2,size.y/2),Vec2(-size.x/2,size.y/2))
        val uv=listOf(Vec2(0f,1f),Vec2(1f,1f),Vec2(1f,0f),Vec2(0f,0f))
        for(i in intArrayOf(0,1,2,0,2,3)) {
            val p=camera.worldToClip(matrix.map(corners[i]),width,height)
            data.put(p.x).put(p.y).put(uv[i].x).put(uv[i].y).put(color.r).put(color.g).put(color.b).put(color.a); vertices++
        }
    }
    /** Physics overlays use actual GL_LINES, sharing the shader/VBO with sprites. */
    fun line(id: Int,start: Vec2,end: Vec2,color: Color) {
        if(texture!=id || data.remaining()<16 || primitive!=GL_LINES)flush()
        texture=id;primitive=GL_LINES
        listOf(start,end).forEach { point -> val p=camera.worldToClip(point,width,height)
            data.put(p.x).put(p.y).put(0f).put(0f).put(color.r).put(color.g).put(color.b).put(color.a);vertices++
        }
    }
    fun flush() {
        if(vertices==0) return
        data.flip(); glBindTexture(GL_TEXTURE_2D,texture); glBindBuffer(GL_ARRAY_BUFFER,vbo)
        glBufferSubData(GL_ARRAY_BUFFER,0,data.remaining()*4,data); glDrawArrays(primitive,0,vertices)
        data.clear(); vertices=0
    }
}
````

## engine-render/src/test/kotlin/world/engine/render/CameraRigTest.kt

````kotlin
package world.engine.render

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import world.engine.core.*
import world.engine.math.*

class CameraRigTest {
    @Test fun rotatedViewRoundTrip() {
        val c=Camera2D(Vec2(80f,40f),2f,37f);val p=Vec2(120f,70f);val clip=c.worldToClip(p,640,480)
        val restored=c.screenToWorld((clip.x+1)*320,(1-clip.y)*240,640,480)
        assertEquals(p.x,restored.x,.001f);assertEquals(p.y,restored.y,.001f)
    }
    @Test fun followDoesNotAccumulateHiddenOvershootBeyondLimits() {
        val target=Node().moved(Vec2(300f,0f));val camera=Node(components=listOf(TransformComponent(),CameraComponent(follow=target.id,smoothing=.1f,deadZone=Vec2(),limitMin=Vec2(-100f,-100f),limitMax=Vec2(100f,100f))))
        val rig=CameraRig();val scene=Scene(nodes=listOf(target,camera));assertEquals(50f,rig.update(scene,.1f,100,100).single().camera.center.x,.001f)
        val returned=rig.update(scene.replace(target.moved(Vec2())),.1f,100,100).single().camera.center.x
        assertTrue(returned in 0f..49f,"Camera must immediately follow a target returning inside its limits")
    }
    @Test fun limitsIncludeVisibleViewportAndMultipleViewsTransition() {
        val target=Node().moved(Vec2(300f,0f))
        val camera=Node(components=listOf(TransformComponent(),CameraComponent(follow=target.id,smoothing=0f,deadZone=Vec2(),limitMin=Vec2(-100f,-100f),limitMax=Vec2(100f,100f))))
        val view=CameraRig().update(Scene(nodes=listOf(target,camera)),.1f,100,100).single();assertEquals(50f,view.camera.center.x,.001f)
        val left=Node(components=listOf(TransformComponent(),CameraComponent(viewport=CameraViewport(width=.5f))))
        val right=Node(components=listOf(TransformComponent(Transform(Vec2(100f,0f))),CameraComponent(viewport=CameraViewport(x=.5f,width=.5f))))
        val scene=Scene(nodes=listOf(left,right));val rig=CameraRig();assertEquals(2,rig.update(scene,0f,800,600).size)
        rig.focus(right.id,.5f);assertEquals(50f,rig.update(scene,.25f,800,600).single().camera.center.x,.001f)
    }
}
````

## tools/RuntimeSmoke.kt

````kotlin
import world.engine.core.*
import world.engine.math.*
import world.engine.animation.*
import world.engine.physics.*
import world.engine.render.*
import world.engine.viewport.PreviewSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.abs

/** Real JVM/JBox2D runtime checks; no mock physics, platform calls or timing sleeps. */
fun main() {
    var count=0
    fun test(name: String,block: ()->Unit) { block();count++;println("PASS: $name") }
    fun near(a: Float,b: Float,tolerance: Float=.01f) { check(abs(a-b)<tolerance){"$a != $b"} }
    fun body(name: String,p: Vec2,kind: BodyKind=BodyKind.DYNAMIC,collider: Collider=Collider(size=Vec2(20f,20f)),velocity: Vec2=Vec2())=Node(name=name,components=listOf(TransformComponent(Transform(p)),SpriteComponent(collider.size),RigidBodyComponent(kind=kind,colliders=listOf(collider),velocity=velocity,fixedRotation=true)))
    test("65 valid editable animation presets with unique stable IDs") {
        val clips=AnimationPresets.all();check(clips.size==65 && clips.map { it.id }.distinct().size==65);clips.forEach { it.validated() };check(clips==AnimationPresets.all())
    }
    test("loop, ping-pong and once dispatch every crossed frame exactly once") {
        val events=(0..2).map { AnimationEvent(it,"$it") }
        val ping=AnimationPlayer(AnimationClip(fps=1,frames=3,loop=LoopMode.PING_PONG,events=events));val fired=mutableListOf<Int>()
        repeat(5){ping.advance(1f){fired.add(it.frame)}};check(fired==listOf(0,1,2,1,0,1)){fired.toString()}
        val loop=AnimationPlayer(AnimationClip(fps=10,frames=3,events=events));val loopEvents=mutableListOf<Int>()
        loop.advance(.65f){loopEvents.add(it.frame)};check(loopEvents==listOf(0,1,2,0,1,2,0)){loopEvents.toString()}
        val once=AnimationPlayer(AnimationClip(fps=10,frames=3,loop=LoopMode.ONCE,events=events));val onceEvents=mutableListOf<Int>()
        repeat(4){once.advance(.25f){onceEvents.add(it.frame)}};check(once.finished && onceEvents==listOf(0,1,2))
        once.seek(1f);val afterSeek=mutableListOf<Int>();once.advance(.1f){afterSeek.add(it.frame)};check(afterSeek==listOf(2))
    }
    test("relative transform, color and sprite sampling plus scene serialization") {
        val asset=UUID.randomUUID().toString()
        val clip=AnimationClip(frames=11,tracks=listOf(AnimationTrack(TrackProperty.POSITION,listOf(AnimationKey(0,listOf(0f,0f)),AnimationKey(10,listOf(20f,40f)))),AnimationTrack(TrackProperty.COLOR,listOf(AnimationKey(0,listOf(1f,0f,0f,1f)),AnimationKey(10,listOf(0f,0f,1f,0f))))),events=listOf(AnimationEvent(5,"swap",AnimationEventKind.SET_SPRITE,asset)))
        val base=Node().moved(Vec2(100f,50f));val sampled=AnimationSampler.sample(base,clip,5f)
        check(sampled.transform.position==Vec2(110f,70f));near(sampled.sprite!!.tint.a,.5f);check(sampled.sprite!!.assetId==asset)
        val scene=Scene(nodes=listOf(base.withComponent(AnimatorComponent(clip.id))),clips=listOf(clip)).validated()
        check(Json.decodeFromString<Scene>(Json.encodeToString(scene)).validated()==scene);check(asset in scene.assetReferences())
    }
    test("input combines sources, dead-zones axes, cancels opposite keys and consumes edges") {
        val input=InputRouter(InputMap());input.physical(InputSource.KEY,29,1f);near(input.snapshot().value("MoveX"),-1f)
        input.physical(InputSource.KEY,32,1f);near(input.snapshot().value("MoveX"),0f)
        input.physical(InputSource.KEY,62,1f);check("Jump" in input.snapshot().pressed);check("Jump" !in input.snapshot().pressed)
        input.releaseAll();check("Jump" in input.snapshot().released)
        input.physical(InputSource.GAMEPAD_AXIS,0,.1f);near(input.snapshot().value("MoveX"),0f)
        input.physical(InputSource.GAMEPAD_AXIS,0,1f);near(input.snapshot().value("MoveX"),1f)
        input.touch("Jump",1f);check("Jump" in input.snapshot().pressed)
        input.releaseAll();input.snapshot();input.touch("Jump",1f);input.touch("Jump",0f);check("Jump" in input.snapshot().pressed)
    }
    test("real JBox2D falling body rests on ground and supports ray/AABB queries") {
        val floor=body("ground",Vec2(0f,-50f),BodyKind.STATIC,Collider(size=Vec2(600f,20f)))
        val actor=body("actor",Vec2(0f,100f));var scene=Scene(nodes=listOf(floor,actor));val contacts=mutableListOf<CollisionEvent>()
        val world=PhysicsWorld(scene,contacts::add);repeat(300){scene=world.step(scene)}
        val y=scene.nodes.single { it.id==actor.id }.transform.position.y
        check(y in -32f..-26f){"rest y=$y"};check(world.isGrounded(actor.id));check(contacts.any { it.entered && !it.sensor })
        check(world.query(Rect(Vec2(0f,-30f),Vec2(60f,60f))).contains(actor.id))
        check(world.raycast(Vec2(0f,200f),Vec2(0f,-100f))?.entity==actor.id)
        check(world.debugLines().isNotEmpty())
    }
    test("sensor enter/exit is real and has no collision response") {
        val sensor=body("sensor",Vec2(),BodyKind.STATIC,Collider(size=Vec2(30f,60f),sensor=true))
        val mover=body("mover",Vec2(-100f,0f),velocity=Vec2(120f,0f));var scene=Scene(nodes=listOf(sensor,mover),gravity=Vec2())
        val events=mutableListOf<CollisionEvent>();val world=PhysicsWorld(scene,events::add)
        repeat(120){scene=world.step(scene)}
        check(events.map { it.entered }==listOf(true,false)){events.toString()};check(events.all { it.sensor });check(scene.nodes.last().transform.position.x>130f)
    }
    test("all seven real joint types, circles, convex polygons and capsules construct and step") {
        JointKind.entries.forEach { kind ->
            val a=body("anchor",Vec2(),BodyKind.STATIC,Collider(shape=ShapeKind.CIRCLE,radius=8f))
            val b=body("bob",Vec2(0f,-100f),collider=Collider(shape=ShapeKind.CAPSULE,size=Vec2(20f,50f)))
            var scene=Scene(nodes=listOf(a,b),joints=listOf(JointSpec(kind=kind,bodyA=a.id,bodyB=b.id,length=100f)))
            val world=PhysicsWorld(scene);repeat(60){scene=world.step(scene)};check(world.jointCount==1);check(scene.nodes.last().transform.position.x.isFinite())
        }
        val hull=ColliderGeometry.hull(listOf(Vec2(-10f,-10f),Vec2(0f,0f),Vec2(10f,-10f),Vec2(10f,10f),Vec2(-10f,10f)))
        check(hull.size==4);Collider(shape=ShapeKind.POLYGON,vertices=hull).validated()
    }
    test("fixed-step preview is deterministic, supports grounded jump and leaves authoring untouched") {
        val floor=body("ground",Vec2(0f,-50f),BodyKind.STATIC,Collider(size=Vec2(600f,20f)))
        val player=body("player",Vec2(0f,50f)).withComponent(InputControllerComponent())
        val scene=Scene(nodes=listOf(floor,player));val first=PreviewSession(scene){};val second=PreviewSession(scene){}
        var one=first.advance(0f);var two=second.advance(0f)
        repeat(120){one=first.advance(1f/60)};repeat(60){two=second.advance(1f/30)}
        check(one.steps==120L && two.steps==120L);near(one.scene.nodes.last().transform.position.y,two.scene.nodes.last().transform.position.y)
        first.input.physical(InputSource.KEY,62,1f);val jumped=first.advance(1f/60);check(jumped.scene.nodes.last().transform.position.y>one.scene.nodes.last().transform.position.y)
        check(scene.nodes.last().transform.position==Vec2(0f,50f));check(first.advance(2f).droppedSeconds>1.8f)
    }
    test("camera rotation round trip, dead zone, limits, split views and smooth transitions") {
        val camera=Camera2D(Vec2(80f,40f),2f,37f);val point=Vec2(120f,70f);val clip=camera.worldToClip(point,640,480)
        val reconstructed=camera.screenToWorld((clip.x+1)*320,(1-clip.y)*240,640,480);near(point.x,reconstructed.x);near(point.y,reconstructed.y)
        val target=Node().moved(Vec2(300f,0f));val c=Node(components=listOf(TransformComponent(),CameraComponent(follow=target.id,smoothing=0f,deadZone=Vec2(),limitMin=Vec2(-100f,-100f),limitMax=Vec2(100f,100f))))
        val rig=CameraRig();val limited=rig.update(Scene(nodes=listOf(target,c)),.1f,100,100).single().camera;near(limited.center.x,50f)
        val left=Node(components=listOf(TransformComponent(),CameraComponent(smoothing=0f,viewport=CameraViewport(width=.5f))))
        val right=Node(components=listOf(TransformComponent(Transform(Vec2(100f,0f))),CameraComponent(smoothing=0f,viewport=CameraViewport(x=.5f,width=.5f))))
        val split=Scene(nodes=listOf(left,right));val multi=CameraRig();check(multi.update(split,0f,800,600).size==2);multi.focus(right.id,.5f)
        val halfway=multi.update(split,.25f,800,600).single();near(halfway.camera.center.x,50f)
        multi.shake(ShakePreset.SMALL);check(multi.update(split,.1f,800,600).single().camera.center.y!=0f)
    }
    test("prefabs retain animation resources and internal joint identity across refresh") {
        val root=Node(name="root",components=listOf(TransformComponent()))
        val clip=AnimationPresets.all().first { it.name=="Humanoid Hurt" }
        val a=body("a",Vec2(),BodyKind.STATIC).copy(parent=root.id)
        val b=body("b",Vec2(0f,-100f)).copy(parent=root.id).withComponent(AnimatorComponent(clip.id))
        val scene=Scene(nodes=listOf(root,a,b),clips=listOf(clip),joints=listOf(JointSpec(bodyA=a.id,bodyB=b.id)))
        val definition=Prefabs.capture(scene,root.id);val id=UUID.randomUUID().toString()
        val instance=Prefabs.bundle(id,mapOf(id to definition));val refreshed=Prefabs.bundle(id,mapOf(id to definition),previous=instance.nodes)
        check(instance.joints==refreshed.joints);check(instance.clips==listOf(clip));check(instance.nodes.map { it.id }==refreshed.nodes.map { it.id });instance.validated()
    }
    println("$count runtime smoke checks passed using actual JBox2D; Android UI/GLES/audio/device tests are not covered.")
}
````

## tools/annotate_ci_failure.py

````python
#!/usr/bin/env python3
"""Expose bounded Gradle/test diagnostics in check annotations as well as log artifacts."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

path = Path(sys.argv[1])
if path.is_file():
    lines = path.read_text(errors='replace').splitlines()
    selected = [line for line in lines if line.startswith('e: ') or ' FAILED' in line]
    for index, line in enumerate(lines):
        if line.startswith('* What went wrong:'):
            selected.extend(lines[index:index + 20])
    for result in Path('.').glob('*/build/test-results/**/TEST-*.xml'):
        try:
            for failure in ET.parse(result).iter('failure'):
                selected.append(f'{result}: {failure.get("message", "")} {(failure.text or "")[:1200]}')
        except ET.ParseError:
            continue
    message = '\n'.join(selected or lines[-45:])[:18000]
    escaped = message.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    print('::error title=Build or test diagnostics::' + escaped)
else:
    print('::warning::Build log unavailable: toolchain setup failed before Gradle execution.')
````

## tools/check_engine_standalone.py

````python
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
import shutil
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--android-jar', type=Path, required=True)
parser.add_argument('--keep-output', type=Path, help='Optional external cache directory for diagnostic JARs')
parser.add_argument('--jbox2d', type=Path, required=True, help='JBox2D library 2.2.1.1 JAR (Maven or diagnostic cache)')
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
assert fat.is_file() and args.android_jar.is_file() and args.jbox2d.is_file()
jars = sorted(p for p in args.jars.glob('*.jar') if p != fat) + [fat]
classpath = os.pathsep.join(map(str, jars))
java = str(args.java_home / 'bin/java')
modules = ('engine-math', 'engine-core', 'engine-assets', 'engine-io', 'engine-animation', 'engine-physics', 'engine-render', 'editor-viewport')
with tempfile.TemporaryDirectory(prefix='world-engine-check-') as directory:
    work = Path(directory)
    # Load only the genuine serialization plugin from the bundled compiler;
    # avoid registering the notebook scripting plugin a second time.
    plugin = work / 'serialization-plugin.jar'
    with zipfile.ZipFile(plugin, 'w') as archive:
        archive.writestr('META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar', 'org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar\n')
        archive.writestr('META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor', 'org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginOptions\n')
    compiler = [java, '-cp', classpath, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-Xplugin=' + str(plugin)]
    artifacts = [str(args.jbox2d)]
    for module in modules:
        output = work / (module + '.jar')
        sources = sorted(str(p) for p in (root / module / 'src/main').rglob('*.kt'))
        target_classpath = os.pathsep.join([classpath, str(args.android_jar), *artifacts])
        subprocess.run([*compiler, '-classpath', target_classpath, '-d', str(output), *sources], check=True)
        artifacts.append(str(output))
        if args.keep_output:
            args.keep_output.mkdir(parents=True, exist_ok=True)
            shutil.copy2(output, args.keep_output / output.name)
        print('PASS: separate-module compile ' + module, flush=True)
    smoke = work / 'core-smoke.jar'
    runtime_classpath = os.pathsep.join([*artifacts, classpath])
    subprocess.run([*compiler, '-classpath', runtime_classpath, '-d', str(smoke), str(root / 'tools/CoreSmoke.kt'), str(root / 'tools/RuntimeSmoke.kt')], check=True)
    subprocess.run([java, '-cp', str(smoke) + os.pathsep + runtime_classpath, 'CoreSmokeKt'], check=True)
    subprocess.run([java, '-ea', '-cp', str(smoke) + os.pathsep + runtime_classpath + os.pathsep + str(args.android_jar), 'RuntimeSmokeKt'], check=True)
print('PASS: standalone engine compilation. This is NOT a Compose/app build or an APK test.')
````

## tools/report_ci_results.py

````python
#!/usr/bin/env python3
"""Summarize actual JUnit XML in GitHub check annotations, without downloading artifacts."""
from collections import defaultdict
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

kind = sys.argv[1]
assert kind in {'unit', 'device'}
pattern = '*/build/test-results/**/TEST-*.xml' if kind == 'unit' else '*/build/outputs/androidTest-results/**/TEST-*.xml'
counts = defaultdict(lambda: [0, 0, 0])
for path in sorted(Path('.').glob(pattern)):
    root = ET.parse(path).getroot()
    cases = list(root.iter('testcase'))
    counts[path.parts[0]][0] += len(cases)
    counts[path.parts[0]][1] += sum(case.find('failure') is not None or case.find('error') is not None for case in cases)
    counts[path.parts[0]][2] += sum(case.find('skipped') is not None for case in cases)
message = '; '.join(f'{module}: {total} tests, {failed} failures, {skipped} skipped' for module, (total, failed, skipped) in counts.items())
print(f'::notice title=Executed {kind} tests::{message or "No JUnit XML produced"}')
assert sum(c[0] for c in counts.values()) > 0, 'No test cases were discovered in result XML'
assert all(c[1] == 0 for c in counts.values()), 'Test failures were recorded'
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
assert len(list(root.glob('*/src/test/**/*.kt'))) == 7
assert len(list(root.glob('*/src/androidTest/**/*.kt'))) == 6
asset_build=(root / 'engine-assets/build.gradle.kts').read_text()
assert 'kotlin("plugin.serialization")' in asset_build
assert 'api(project(":engine-assets"))' in (root / 'editor-assets/build.gradle.kts').read_text()
assert 'api(project(":editor-assets"))' in (root / 'editor-ui/build.gradle.kts').read_text()
generator=(root / 'engine-assets/src/main/kotlin/world/engine/assets/ProceduralAssets.kt').read_text()
counts=re.findall(r'"(?:Character|Enemy|NPC|Animal|Vehicle|Building|Nature|Prop|Texture)" to (\d+)', generator)
assert sum(map(int,counts))==335
assert 'org.jbox2d:jbox2d-library:2.2.1.1' in (root / 'engine-physics/build.gradle.kts').read_text()
assert 'api(project(":engine-animation"))' in (root / 'editor-viewport/build.gradle.kts').read_text()
print(f'PASS: {len(modules)} modules, project references, XML, official wrapper integrity, permission policy, and test-source presence.')
print('Android compilation, JUnit execution, Compose tests and GLES device tests are NOT covered by this check.')
````

## tools/write_phase3_delivery.py

````python
#!/usr/bin/env python3
"""Generate/check complete Phase 3 source and file inventory against the Phase 2 snapshot."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--check', action='store_true')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
baseline = json.loads((root / 'docs/PHASE3_BASELINE.json').read_text())
generated = {'docs/PHASE3_FILES.md', 'docs/PHASE3_SOURCE.md'}
tracked = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=root).decode().split('\0')
paths = sorted({p for p in tracked if p and (root / p).is_file()} | generated)
hashes = {p: hashlib.sha256((root / p).read_bytes()).hexdigest() for p in paths if p not in generated}
status = {p: 'NEW' if p not in baseline else 'UNCHANGED' if hashes.get(p) == baseline[p] else 'CHANGED' for p in paths}
removed = sorted(set(baseline) - set(paths))
assert not removed, f'Unexpected removed baseline files: {removed}'
inventory = '# Phase 3 file inventory\n\nCompared with the captured end-of-Phase-2 SHA-256 baseline. Generated inventory/source files do not hash themselves.\n\n'
for group in ('NEW', 'CHANGED', 'UNCHANGED'):
    items = [p for p in paths if status[p] == group]
    inventory += f'## {group} — {len(items)} files\n\n| Path | SHA-256 |\n|---|---|\n'
    inventory += ''.join(f'| `{p}` | `{hashes.get(p, "generated; not self-hashed")}` |\n' for p in items) + '\n'
changed = [p for p in paths if status[p] != 'UNCHANGED' and p != 'docs/PHASE3_SOURCE.md']
source = '# Phase 3 — complete new/changed source\n\nGenerated from the actual repository files. Includes implementation, tests, configuration, licensing and documents in full; it does not recursively embed this appendix itself. The unchanged source remains in its original repository paths.\n\n'
source += ''.join(f'- `{p}` ({status[p]})\n' for p in changed) + '\n'
for p in changed:
    text = inventory if p == 'docs/PHASE3_FILES.md' else (root / p).read_text()
    language = 'kotlin' if p.endswith(('.kt', '.kts')) else {'.py': 'python', '.json': 'json', '.yml': 'yaml', '.xml': 'xml', '.md': 'markdown'}.get(Path(p).suffix, 'text')
    fence = '`' * max(4, max((len(line) + 1 for line in text.splitlines() if line and not line.strip('`')), default=4))
    source += f'## {p}\n\n{fence}{language}\n{text.rstrip()}\n{fence}\n\n'
for name, contents in (('PHASE3_FILES.md', inventory), ('PHASE3_SOURCE.md', source)):
    path = root / 'docs' / name
    if args.check:
        assert path.is_file() and path.read_text() == contents, f'{name} is stale; regenerate delivery documents'
    else:
        path.write_text(contents)
print(f'PASS: Phase 3 inventory and complete source snapshots ({len(changed)} full files plus the appendix itself).')
````

