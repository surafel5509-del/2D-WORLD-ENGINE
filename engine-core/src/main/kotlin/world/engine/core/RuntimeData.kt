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
