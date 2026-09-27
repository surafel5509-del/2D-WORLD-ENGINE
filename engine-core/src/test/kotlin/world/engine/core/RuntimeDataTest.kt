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
    @Test fun dynamicPoseAnimationAndBadCollidersAreRejected() {
        val clip=AnimationClip(tracks=listOf(AnimationTrack(TrackProperty.POSITION,listOf(AnimationKey(0,listOf(0f,0f))))))
        val n=Node(components=listOf(TransformComponent(),RigidBodyComponent(),AnimatorComponent(clip.id)))
        assertThrows(IllegalArgumentException::class.java){Scene(nodes=listOf(n),clips=listOf(clip)).validated()}
        assertThrows(IllegalArgumentException::class.java){Collider(shape=ShapeKind.CAPSULE,size=Vec2(100f,20f)).validated()}
    }
}
