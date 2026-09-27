import world.engine.core.*
import world.engine.math.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.abs

/** Dependency-light executable checks supplement (not replace) the Gradle/JUnit/device suites. */
fun main() {
    var passed=0
    fun test(name: String,action: ()->Unit) { action(); passed++; println("PASS: $name") }
    fun rejects(action: ()->Unit) { var rejected=false; try { action() } catch(e: IllegalArgumentException) { rejected=true }; check(rejected) }
    test("affine hierarchy including nonuniform scale and shear") {
        val a=Transform(Vec2(10f,-4f),35f,Vec2(2f,.5f)).matrix()
        val b=Transform(Vec2(3f,7f),75f).matrix()
        val p=Vec2(8f,9f); val actual=(a*b).inverse().map((a*b).map(p))
        check(abs(actual.x-p.x)<.0001f && abs(actual.y-p.y)<.0001f)
    }
    test("parent order independent picking and recursive deletion membership") {
        val root=Node(components=listOf(TransformComponent(Transform(Vec2(120f,40f),35f,Vec2(2f,.5f)))))
        val child=Node(parent=root.id,components=listOf(TransformComponent(Transform(Vec2(10f,8f),20f)),SpriteComponent()))
        val scene=Scene(nodes=listOf(child,root)).validated()
        val center=scene.worldMatrices().getValue(child.id).map(Vec2())
        check(scene.pick(center)==child); check(scene.descendants(root.id)==setOf(root.id,child.id))
        rejects { Scene(nodes=listOf(root.copy(parent=child.id),child)).validated() }
    }
    test("nested prefabs expand, preserve overrides and retain UUIDs on refresh") {
        val leafId=UUID.randomUUID().toString(); val outerId=UUID.randomUUID().toString()
        val wheel=Node(name="Wheel")
        val root=Node(name="Car",components=listOf(TransformComponent()))
        val anchor=Node(parent=root.id,nestedPrefab=leafId,components=listOf(TransformComponent()))
        val definitions=mapOf(leafId to PrefabDefinition(nodes=listOf(wheel)),outerId to PrefabDefinition(nodes=listOf(root,anchor)))
        val original=Prefabs.instantiate(outerId,definitions,Vec2(50f,60f))
        val edited=original.map { if(it.name=="Wheel") Prefabs.override(it.copy(name="Custom").moved(Vec2(7f,9f)),"name","transform") else it }
        val changed=definitions+(leafId to PrefabDefinition(nodes=listOf(wheel.copy(name="Source update"))))
        val refreshed=Prefabs.instantiate(outerId,changed,Vec2(50f,60f),edited)
        check(refreshed.map { it.id }==original.map { it.id })
        check(refreshed.single { it.name=="Custom" }.transform.position==Vec2(7f,9f))
        check(Prefabs.instantiate(outerId,changed,Vec2(),edited,false).any { it.name=="Source update" })
        val captured=Prefabs.capture(Scene(nodes=refreshed),original.first().id)
        check(captured.nodes.size==2 && captured.dependencies()==setOf(leafId))
        check(captured.nodes.first().id==root.id)
    }
    test("cyclic definitions and duplicate identities are rejected") {
        val id=UUID.randomUUID().toString()
        rejects { Prefabs.instantiate(id,mapOf(id to PrefabDefinition(nodes=listOf(Node(nestedPrefab=id))))) }
        val n=Node(); rejects { Scene(nodes=listOf(n,n)).validated() }
        rejects { Scene(nodes=listOf(n.copy(parent="absent"))).validated() }
    }
    test("serialized hierarchy references and legacy references round-trip") {
        val id=UUID.randomUUID().toString()
        val root=Node(components=listOf(TransformComponent()))
        val child=Node(parent=root.id,components=listOf(TransformComponent(),SpriteComponent(assetId=id)))
        val scene=Scene(nodes=listOf(root,child)).validated()
        check(Json.decodeFromString<Scene>(Json.encodeToString(scene)).validated()==scene)
        check(scene.assetReferences()==setOf(id))
        val legacy=Scene(nodes=listOf(Node(components=listOf(TransformComponent(),SpriteComponent(asset="Sprites/$id.png")))))
        check(legacy.assetReferences()==setOf(id))
        check(Prefabs.capture(legacy,legacy.nodes.first().id).dependencies()==setOf(id))
    }
    test("hierarchy edits remain one undo command and branch clears redo") {
        val root=Node(components=listOf(TransformComponent())); val child=Node(parent=root.id)
        val before=Scene(nodes=listOf(root,child)); val after=before.replace(root.moved(Vec2(30f,40f)))
        val history=CommandStack(); history.commit(before,after)
        check(history.undo(after)==before); check(history.redo(before)==after)
        history.undo(after); history.commit(before,Scene()); check(!history.canRedo)
        check(history.retainedScenes().isNotEmpty())
    }
    println("$passed core smoke checks passed; Android UI, bitmap, SQLite and GLES execution not tested here.")
}
