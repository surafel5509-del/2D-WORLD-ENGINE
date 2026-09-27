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
