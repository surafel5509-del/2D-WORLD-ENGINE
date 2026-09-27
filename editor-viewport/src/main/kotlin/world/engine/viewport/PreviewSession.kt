package world.engine.viewport

import world.engine.core.*
import world.engine.math.*
import world.engine.animation.*
import world.engine.physics.*
import world.engine.render.*
import java.util.concurrent.ConcurrentLinkedQueue

/** Nondestructive play session. Only advance() touches the Box2D world, on one worker thread. */
data class PreviewFrame(val scene: Scene,val views: List<CameraView>,val lines: List<DebugLine>,val steps: Long,val droppedSeconds: Float,val bodies: Int,val joints: Int,val input: Map<String,Float>)
class PreviewSession(private val authored: Scene,private val event: (String)->Unit) {
    val input=InputRouter(authored.inputMap)
    private val cameras=CameraRig(authored.activeCamera)
    private val commands=ConcurrentLinkedQueue<()->Unit>()
    private val physics=PhysicsWorld(authored) { event("${if(it.sensor)"Trigger" else "Collision"} ${if(it.entered)"enter" else "exit"}: ${it.a} ↔ ${it.b}") }
    private val animators=authored.nodes.mapNotNull { n -> n.component<AnimatorComponent>()?.let { n.id to AnimationPlayer(authored.clips.single { c -> c.id==it.clipId },it.speed) } }.toMap()
    private val bases=authored.nodes.associateBy { it.id }
    private var current=authored
    private var accumulator=0.0
    private var steps=0L
    private var dropped=0f
    private var inputValues=emptyMap<String,Float>()
    @Volatile private var dimensions=800 to 600
    fun resize(width: Int,height: Int) { dimensions=width.coerceAtLeast(1) to height.coerceAtLeast(1) }
    fun focusCamera(id: String?) { commands.add { cameras.focus(id) } }
    fun shake(preset: ShakePreset) { commands.add { cameras.shake(preset) } }
    fun advance(seconds: Float): PreviewFrame {
        require(seconds.isFinite() && seconds>=0)
        while(true) { val command=commands.poll() ?: break;command() }
        val accepted=seconds.coerceAtMost(.25f);dropped+=(seconds-accepted)
        accumulator+=accepted
        var count=0
        while(accumulator+1e-9>=PhysicsWorld.STEP && count<8) {
            val controls=input.snapshot();inputValues=controls.values
            controls.pressed.forEach { event("Input pressed: $it") }
            current=current.copy(nodes=current.nodes.map { n ->
                animators[n.id]?.let { player ->
                    player.advance(PhysicsWorld.STEP) { e -> event("Animation ${e.name} (${e.kind}) on ${n.name}, frame ${e.frame}") }
                    AnimationSampler.sample(bases.getValue(n.id),player.clip,player.frame,n)
                } ?: n
            })
            current=current.copy(nodes=current.nodes.map { n ->
                val controller=n.component<InputControllerComponent>() ?: return@map n
                val x=controls.value(controller.horizontal);val y=controls.value(controller.vertical)
                val movement=Vec2(x,y);val direction=if(movement.length()>1f)movement*(1f/movement.length()) else movement
                val body=n.component<RigidBodyComponent>()
                if(body!=null && body.kind!=BodyKind.STATIC) {
                    val old=physics.velocity(n.id)
                    val vy=if(controller.kind==ControllerKind.TOP_DOWN)direction.y*controller.speed else if(controller.jump in controls.pressed && physics.isGrounded(n.id))controller.jumpSpeed else old.y
                    physics.setVelocity(n.id,Vec2(if(controller.kind==ControllerKind.TOP_DOWN)direction.x*controller.speed else x*controller.speed,vy));n
                } else if(body==null)n.moved(n.transform.position+direction*(controller.speed*PhysicsWorld.STEP)) else n
            })
            current=physics.step(current)
            val (width,height)=dimensions
            cameras.update(current,PhysicsWorld.STEP,width,height)
            steps++;count++;accumulator-=PhysicsWorld.STEP
        }
        if(accumulator>=PhysicsWorld.STEP) { val remainder=accumulator%PhysicsWorld.STEP;dropped+=(accumulator-remainder).toFloat();accumulator=remainder }
        val (width,height)=dimensions
        return PreviewFrame(current,cameras.update(current,0f,width,height),physics.debugLines(),steps,dropped,physics.bodyCount,physics.jointCount,inputValues)
    }
}
