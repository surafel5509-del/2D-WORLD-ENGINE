import world.engine.core.*
import world.engine.math.*
import world.engine.animation.*
import world.engine.physics.*
import world.engine.render.*
import world.engine.viewport.PreviewSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.abs

/** Real JVM/JBox2D runtime checks; no mock physics, platform calls or timing sleeps. */
fun main() {
    var count=0
    fun test(name: String,block: ()->Unit) { block();count++;println("PASS: $name") }
    fun near(a: Float,b: Float,tolerance: Float=.01f) { check(abs(a-b)<tolerance){"$a != $b"} }
    fun body(name: String,p: Vec2,kind: BodyKind=BodyKind.DYNAMIC,collider: Collider=Collider(size=Vec2(20f,20f)),velocity: Vec2=Vec2())=Node(name=name,components=listOf(TransformComponent(Transform(p)),SpriteComponent(collider.size),RigidBodyComponent(kind=kind,colliders=listOf(collider),velocity=velocity,fixedRotation=true)))
    test("65 valid editable animation presets with unique stable IDs") {
        val clips=AnimationPresets.all();check(clips.size==65 && clips.map { it.id }.distinct().size==65);clips.forEach { it.validated() };check(clips==AnimationPresets.all())
    }
    test("loop, ping-pong and once dispatch every crossed frame exactly once") {
        val events=(0..2).map { AnimationEvent(it,"$it") }
        val ping=AnimationPlayer(AnimationClip(fps=1,frames=3,loop=LoopMode.PING_PONG,events=events));val fired=mutableListOf<Int>()
        repeat(5){ping.advance(1f){fired.add(it.frame)}};check(fired==listOf(0,1,2,1,0,1)){fired.toString()}
        val loop=AnimationPlayer(AnimationClip(fps=10,frames=3,events=events));val loopEvents=mutableListOf<Int>()
        loop.advance(.65f){loopEvents.add(it.frame)};check(loopEvents==listOf(0,1,2,0,1,2,0)){loopEvents.toString()}
        val once=AnimationPlayer(AnimationClip(fps=10,frames=3,loop=LoopMode.ONCE,events=events));val onceEvents=mutableListOf<Int>()
        repeat(4){once.advance(.25f){onceEvents.add(it.frame)}};check(once.finished && onceEvents==listOf(0,1,2))
        once.seek(1f);val afterSeek=mutableListOf<Int>();once.advance(.1f){afterSeek.add(it.frame)};check(afterSeek==listOf(2))
    }
    test("relative transform, color and sprite sampling plus scene serialization") {
        val asset=UUID.randomUUID().toString()
        val clip=AnimationClip(frames=11,tracks=listOf(AnimationTrack(TrackProperty.POSITION,listOf(AnimationKey(0,listOf(0f,0f)),AnimationKey(10,listOf(20f,40f)))),AnimationTrack(TrackProperty.COLOR,listOf(AnimationKey(0,listOf(1f,0f,0f,1f)),AnimationKey(10,listOf(0f,0f,1f,0f))))),events=listOf(AnimationEvent(5,"swap",AnimationEventKind.SET_SPRITE,asset)))
        val base=Node().moved(Vec2(100f,50f));val sampled=AnimationSampler.sample(base,clip,5f)
        check(sampled.transform.position==Vec2(110f,70f));near(sampled.sprite!!.tint.a,.5f);check(sampled.sprite!!.assetId==asset)
        val scene=Scene(nodes=listOf(base.withComponent(AnimatorComponent(clip.id))),clips=listOf(clip)).validated()
        check(Json.decodeFromString<Scene>(Json.encodeToString(scene)).validated()==scene);check(asset in scene.assetReferences())
    }
    test("input combines sources, dead-zones axes, cancels opposite keys and consumes edges") {
        val input=InputRouter(InputMap());input.physical(InputSource.KEY,29,1f);near(input.snapshot().value("MoveX"),-1f)
        input.physical(InputSource.KEY,32,1f);near(input.snapshot().value("MoveX"),0f)
        input.physical(InputSource.KEY,62,1f);check("Jump" in input.snapshot().pressed);check("Jump" !in input.snapshot().pressed)
        input.releaseAll();check("Jump" in input.snapshot().released)
        input.physical(InputSource.GAMEPAD_AXIS,0,.1f);near(input.snapshot().value("MoveX"),0f)
        input.physical(InputSource.GAMEPAD_AXIS,0,1f);near(input.snapshot().value("MoveX"),1f)
        input.touch("Jump",1f);check("Jump" in input.snapshot().pressed)
        input.releaseAll();input.snapshot();input.touch("Jump",1f);input.touch("Jump",0f);check("Jump" in input.snapshot().pressed)
    }
    test("real JBox2D falling body rests on ground and supports ray/AABB queries") {
        val floor=body("ground",Vec2(0f,-50f),BodyKind.STATIC,Collider(size=Vec2(600f,20f)))
        val actor=body("actor",Vec2(0f,100f));var scene=Scene(nodes=listOf(floor,actor));val contacts=mutableListOf<CollisionEvent>()
        val world=PhysicsWorld(scene,contacts::add);repeat(300){scene=world.step(scene)}
        val y=scene.nodes.single { it.id==actor.id }.transform.position.y
        check(y in -32f..-26f){"rest y=$y"};check(world.isGrounded(actor.id));check(contacts.any { it.entered && !it.sensor })
        check(world.query(Rect(Vec2(0f,-30f),Vec2(60f,60f))).contains(actor.id))
        check(world.raycast(Vec2(0f,200f),Vec2(0f,-100f))?.entity==actor.id)
        check(world.debugLines().isNotEmpty())
    }
    test("sensor enter/exit is real and has no collision response") {
        val sensor=body("sensor",Vec2(),BodyKind.STATIC,Collider(size=Vec2(30f,60f),sensor=true))
        val mover=body("mover",Vec2(-100f,0f),velocity=Vec2(120f,0f));var scene=Scene(nodes=listOf(sensor,mover),gravity=Vec2())
        val events=mutableListOf<CollisionEvent>();val world=PhysicsWorld(scene,events::add)
        repeat(120){scene=world.step(scene)}
        check(events.map { it.entered }==listOf(true,false)){events.toString()};check(events.all { it.sensor });check(scene.nodes.last().transform.position.x>130f)
    }
    test("all seven real joint types, circles, convex polygons and capsules construct and step") {
        JointKind.entries.forEach { kind ->
            val a=body("anchor",Vec2(),BodyKind.STATIC,Collider(shape=ShapeKind.CIRCLE,radius=8f))
            val b=body("bob",Vec2(0f,-100f),collider=Collider(shape=ShapeKind.CAPSULE,size=Vec2(20f,50f)))
            var scene=Scene(nodes=listOf(a,b),joints=listOf(JointSpec(kind=kind,bodyA=a.id,bodyB=b.id,length=100f)))
            val world=PhysicsWorld(scene);repeat(60){scene=world.step(scene)};check(world.jointCount==1);check(scene.nodes.last().transform.position.x.isFinite())
        }
        val hull=ColliderGeometry.hull(listOf(Vec2(-10f,-10f),Vec2(0f,0f),Vec2(10f,-10f),Vec2(10f,10f),Vec2(-10f,10f)))
        check(hull.size==4);Collider(shape=ShapeKind.POLYGON,vertices=hull).validated()
    }
    test("fixed-step preview is deterministic, supports grounded jump and leaves authoring untouched") {
        val floor=body("ground",Vec2(0f,-50f),BodyKind.STATIC,Collider(size=Vec2(600f,20f)))
        val player=body("player",Vec2(0f,50f)).withComponent(InputControllerComponent())
        val scene=Scene(nodes=listOf(floor,player));val first=PreviewSession(scene){};val second=PreviewSession(scene){}
        var one=first.advance(0f);var two=second.advance(0f)
        repeat(120){one=first.advance(1f/60)};repeat(60){two=second.advance(1f/30)}
        check(one.steps==120L && two.steps==120L);near(one.scene.nodes.last().transform.position.y,two.scene.nodes.last().transform.position.y)
        first.input.physical(InputSource.KEY,62,1f);val jumped=first.advance(1f/60);check(jumped.scene.nodes.last().transform.position.y>one.scene.nodes.last().transform.position.y)
        check(scene.nodes.last().transform.position==Vec2(0f,50f));check(first.advance(2f).droppedSeconds>1.8f)
    }
    test("camera rotation round trip, dead zone, limits, split views and smooth transitions") {
        val camera=Camera2D(Vec2(80f,40f),2f,37f);val point=Vec2(120f,70f);val clip=camera.worldToClip(point,640,480)
        val reconstructed=camera.screenToWorld((clip.x+1)*320,(1-clip.y)*240,640,480);near(point.x,reconstructed.x);near(point.y,reconstructed.y)
        val target=Node().moved(Vec2(300f,0f));val c=Node(components=listOf(TransformComponent(),CameraComponent(follow=target.id,smoothing=0f,deadZone=Vec2(),limitMin=Vec2(-100f,-100f),limitMax=Vec2(100f,100f))))
        val rig=CameraRig();val limited=rig.update(Scene(nodes=listOf(target,c)),.1f,100,100).single().camera;near(limited.center.x,50f)
        val left=Node(components=listOf(TransformComponent(),CameraComponent(smoothing=0f,viewport=CameraViewport(width=.5f))))
        val right=Node(components=listOf(TransformComponent(Transform(Vec2(100f,0f))),CameraComponent(smoothing=0f,viewport=CameraViewport(x=.5f,width=.5f))))
        val split=Scene(nodes=listOf(left,right));val multi=CameraRig();check(multi.update(split,0f,800,600).size==2);multi.focus(right.id,.5f)
        val halfway=multi.update(split,.25f,800,600).single();near(halfway.camera.center.x,50f)
        multi.shake(ShakePreset.SMALL);check(multi.update(split,.1f,800,600).single().camera.center.y!=0f)
    }
    test("prefabs retain animation resources and internal joint identity across refresh") {
        val root=Node(name="root",components=listOf(TransformComponent()))
        val clip=AnimationPresets.all().first { it.name=="Humanoid Hurt" }
        val a=body("a",Vec2(),BodyKind.STATIC).copy(parent=root.id)
        val b=body("b",Vec2(0f,-100f)).copy(parent=root.id).withComponent(AnimatorComponent(clip.id))
        val scene=Scene(nodes=listOf(root,a,b),clips=listOf(clip),joints=listOf(JointSpec(bodyA=a.id,bodyB=b.id)))
        val definition=Prefabs.capture(scene,root.id);val id=UUID.randomUUID().toString()
        val instance=Prefabs.bundle(id,mapOf(id to definition));val refreshed=Prefabs.bundle(id,mapOf(id to definition),previous=instance.nodes)
        check(instance.joints==refreshed.joints);check(instance.clips==listOf(clip));check(instance.nodes.map { it.id }==refreshed.nodes.map { it.id });instance.validated()
    }
    println("$count runtime smoke checks passed using actual JBox2D; Android UI/GLES/audio/device tests are not covered.")
}
