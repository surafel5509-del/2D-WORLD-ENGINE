package world.engine.render

import world.engine.core.*
import world.engine.math.*
import kotlin.math.*

data class CameraView(val id: String,val camera: Camera2D,val viewport: CameraViewport=CameraViewport())
enum class ShakePreset(val amplitude: Float,val seconds: Float) {
    SMALL(3f,.15f),MEDIUM(8f,.3f),LARGE(18f,.5f),EXPLOSION(28f,.6f),DAMAGE(10f,.2f),EARTHQUAKE(12f,2f)
}
/** A single-thread camera system: dead zone, exponential follow, split views, transitions and shake. */
class CameraRig(initialActive: String?=null) {
    private val states=mutableMapOf<String,Camera2D>()
    private var focus=initialActive
    private var previous: Camera2D?=null
    private var transition=0f
    private var transitionDuration=.4f
    private var last=emptyList<CameraView>()
    private var shake: ShakePreset?=null
    private var shakeTime=0f
    fun focus(id: String?,seconds: Float=.4f) { require(seconds.isFinite() && seconds in 0f..10f);previous=last.firstOrNull()?.camera;focus=id;transition=0f;transitionDuration=seconds }
    fun shake(preset: ShakePreset) { shake=preset;shakeTime=0f }
    fun update(scene: Scene,dt: Float,width: Int,height: Int): List<CameraView> {
        require(dt.isFinite() && dt>=0f)
        val matrices=scene.worldMatrices();val cameras=scene.nodes.filter { it.component<CameraComponent>()?.enabled==true }
        val visible=if(focus==null)cameras else cameras.filter { it.id==focus }
        transition+=dt;shakeTime+=dt
        val result=visible.map { n ->
            val config=n.component<CameraComponent>()!!;val m=matrices.getValue(n.id)
            val authored=Camera2D(m.map(Vec2()),config.zoom,config.rotation+atan2(m.values[3],m.values[0])*180f/PI.toFloat())
            val old=states[n.id] ?: authored;val target=config.follow?.let { matrices[it]?.map(Vec2()) }
            var desired=old.center
            if(target!=null) {
                val delta=target-old.center;val r=-authored.rotation*PI/180
                val local=Vec2((cos(r)*delta.x-sin(r)*delta.y).toFloat(),(sin(r)*delta.x+cos(r)*delta.y).toFloat())
                val correction=Vec2(local.x-local.x.coerceIn(-config.deadZone.x/2,config.deadZone.x/2),local.y-local.y.coerceIn(-config.deadZone.y/2,config.deadZone.y/2))
                desired+=Vec2((cos(-r)*correction.x-sin(-r)*correction.y).toFloat(),(sin(-r)*correction.x+cos(-r)*correction.y).toFloat())
            } else desired=authored.center
            val alpha=if(config.smoothing==0f || n.id !in states)1f else 1f-exp(-dt/config.smoothing)
            var camera=constrain(authored.copy(center=old.center+(desired-old.center)*alpha),config,width,height)
            states[n.id]=camera
            previous?.let { from ->
                val t=if(transitionDuration==0f)1f else (transition/transitionDuration).coerceIn(0f,1f);val eased=t*t*(3-2*t)
                val angle=((camera.rotation-from.rotation+540f)%360f)-180f
                camera=Camera2D(from.center+(camera.center-from.center)*eased,from.zoom+(camera.zoom-from.zoom)*eased,from.rotation+angle*eased)
            }
            shake?.let { s -> if(shakeTime<s.seconds) {
                val amplitude=s.amplitude*(1f-shakeTime/s.seconds);val seed=(n.id.hashCode() and 255)*.1f
                camera=camera.copy(center=camera.center+Vec2(sin(shakeTime*137f+seed)*amplitude,cos(shakeTime*173f+seed)*amplitude))
            } }
            camera=constrain(camera,config,width,height)
            CameraView(n.id,camera,config.viewport)
        }
        if(transition>=transitionDuration)previous=null
        if(shakeTime>=(shake?.seconds ?: 0f))shake=null
        last=if(result.isEmpty())listOf(CameraView("editor-default",Camera2D())) else result
        return last
    }
    private fun constrain(camera: Camera2D,config: CameraComponent,width: Int,height: Int): Camera2D {
        val low=config.limitMin ?: return camera;val high=config.limitMax ?: return camera
        val r=camera.rotation*PI/180;val halfX=width*config.viewport.width/(2*camera.zoom);val halfY=height*config.viewport.height/(2*camera.zoom)
        val extentX=(abs(cos(r))*halfX+abs(sin(r))*halfY).toFloat();val extentY=(abs(sin(r))*halfX+abs(cos(r))*halfY).toFloat()
        fun clamp(value: Float,min: Float,max: Float,extent: Float)=if(max-min<2*extent)(min+max)/2 else value.coerceIn(min+extent,max-extent)
        return camera.copy(center=Vec2(clamp(camera.center.x,low.x,high.x,extentX),clamp(camera.center.y,low.y,high.y,extentY)))
    }
}
