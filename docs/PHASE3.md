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
