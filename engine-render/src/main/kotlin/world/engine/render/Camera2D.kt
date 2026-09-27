package world.engine.render

import world.engine.math.Vec2
import kotlin.math.*

/** Immutable view transform. Rotation is degrees counter-clockwise in world space. */
data class Camera2D(val center: Vec2=Vec2(),val zoom: Float=1f,val rotation: Float=0f) {
    fun screenToWorld(x: Float,y: Float,width: Int,height: Int): Vec2 {
        val dx=(x-width/2f)/zoom;val dy=(height/2f-y)/zoom;val a=rotation*PI/180
        return center+Vec2((cos(a)*dx-sin(a)*dy).toFloat(),(sin(a)*dx+cos(a)*dy).toFloat())
    }
    fun worldToClip(p: Vec2,width: Int,height: Int): Vec2 {
        val d=p-center;val a=-rotation*PI/180
        return Vec2((cos(a)*d.x-sin(a)*d.y).toFloat()*zoom*2/width.coerceAtLeast(1),(sin(a)*d.x+cos(a)*d.y).toFloat()*zoom*2/height.coerceAtLeast(1))
    }
}
