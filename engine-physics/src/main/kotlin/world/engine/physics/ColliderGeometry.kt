package world.engine.physics

import android.graphics.Bitmap
import world.engine.core.*
import world.engine.math.*
import kotlin.math.*

/** Convex geometry is also used by the editor before a Box2D world exists. */
object ColliderGeometry {
    fun hull(points: List<Vec2>,limit: Int=8): List<Vec2> {
        require(limit in 3..8 && points.size<=20000 && points.all { it.x.isFinite() && it.y.isFinite() })
        val sorted=points.distinct().sortedWith(compareBy<Vec2> { it.x }.thenBy { it.y })
        fun cross(a: Vec2,b: Vec2,c: Vec2)=(b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x)
        fun half(input: List<Vec2>): List<Vec2> {
            val result=mutableListOf<Vec2>()
            input.forEach { p -> while(result.size>=2 && cross(result[result.lastIndex-1],result.last(),p)<=.001f)result.removeAt(result.lastIndex);result.add(p) }
            return result.dropLast(1)
        }
        val hull=(half(sorted)+half(sorted.asReversed())).toMutableList()
        require(hull.size>=3) { "Need at least three non-collinear points" }
        while(hull.size>limit) {
            val index=hull.indices.minBy { i -> abs(cross(hull[(i-1+hull.size)%hull.size],hull[i],hull[(i+1)%hull.size])) }
            hull.removeAt(index)
        }
        return hull
    }
    /** Alpha hull approximation: concavities/holes are filled; at most eight hull vertices. */
    fun alpha(bitmap: Bitmap,size: Vec2,threshold: Int=16): Collider {
        require(bitmap.width<=2048 && bitmap.height<=2048 && threshold in 1..255)
        val row=IntArray(bitmap.width); val points=mutableListOf<Vec2>()
        fun local(x: Int,y: Int)=Vec2((x.toFloat()/bitmap.width-.5f)*size.x,(.5f-y.toFloat()/bitmap.height)*size.y)
        for(y in 0 until bitmap.height) {
            bitmap.getPixels(row,0,bitmap.width,0,y,bitmap.width,1)
            val left=row.indexOfFirst { (it ushr 24)>=threshold };val right=row.indexOfLast { (it ushr 24)>=threshold }
            if(left>=0) { points.add(local(left,y));points.add(local(right+1,y));points.add(local(left,y+1));points.add(local(right+1,y+1)) }
        }
        return Collider(shape=ShapeKind.POLYGON,size=size,vertices=hull(points)).validated()
    }
    fun polygons(c: Collider): List<List<Vec2>> {
        fun rect(w: Float,h: Float)=listOf(Vec2(-w/2,-h/2),Vec2(w/2,-h/2),Vec2(w/2,h/2),Vec2(-w/2,h/2))
        fun circle(center: Vec2,r: Float)=List(24) { i -> val a=i*2*PI/24;center+Vec2(cos(a).toFloat()*r,sin(a).toFloat()*r) }
        val raw=when(c.shape) {
            ShapeKind.RECTANGLE -> listOf(rect(c.size.x,c.size.y))
            ShapeKind.POLYGON -> listOf(c.vertices)
            ShapeKind.CIRCLE -> listOf(circle(Vec2(),c.radius))
            ShapeKind.CAPSULE -> {
                val r=c.size.x/2;val d=(c.size.y-c.size.x)/2
                listOfNotNull(if(d>.001f)rect(c.size.x,d*2) else null,circle(Vec2(0f,d),r),if(d>.001f)circle(Vec2(0f,-d),r) else null)
            }
        }
        return raw.map { vertices -> vertices.map { it+c.offset } }
    }
    fun outlines(scene: Scene): List<DebugLine> {
        val matrices=scene.worldMatrices()
        return scene.nodes.flatMap { n -> n.component<RigidBodyComponent>()?.colliders.orEmpty().flatMap { c ->
            val color=if(c.sensor)Color(.2f,.8f,1f) else Color(.3f,1f,.4f)
            polygons(c).flatMap { polygon -> polygon.indices.map { i -> DebugLine(matrices.getValue(n.id).map(polygon[i]),matrices.getValue(n.id).map(polygon[(i+1)%polygon.size]),color) } }
        } }
    }
}
