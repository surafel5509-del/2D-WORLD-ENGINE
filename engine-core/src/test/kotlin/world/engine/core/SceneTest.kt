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
