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
        val updated=Prefabs.override(node.copy(components=node.components.filterNot { it.javaClass.simpleName==type }),"runtime")
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
    fun releaseInput() { session?.input?.releaseAll() }
    fun shakeCamera(preset: ShakePreset) { session?.shake(preset) }
    fun focusCamera(id: String?) { session?.focusCamera(id) }
}
