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
