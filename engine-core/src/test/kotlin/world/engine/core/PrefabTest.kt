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
