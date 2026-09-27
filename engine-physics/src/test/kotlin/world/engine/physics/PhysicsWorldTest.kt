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
