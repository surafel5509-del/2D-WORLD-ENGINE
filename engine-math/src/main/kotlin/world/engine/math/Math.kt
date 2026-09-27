package world.engine.math

import kotlinx.serialization.Serializable
import kotlin.math.*

/** Immutable world-space vector; positive Y points up. */
@Serializable data class Vec2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(v: Vec2) = Vec2(x + v.x, y + v.y)
    operator fun minus(v: Vec2) = Vec2(x - v.x, y - v.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    fun length() = hypot(x, y)
}
/** Row-major affine matrix operating on column vectors. */
data class Mat3(val values: List<Float>) {
    init { require(values.size == 9) }
    operator fun times(b: Mat3) = Mat3(List(9) { i ->
        (0..2).sumOf { k -> (values[(i / 3) * 3 + k] * b.values[k * 3 + i % 3]).toDouble() }.toFloat()
    })
    fun map(v: Vec2) = Vec2(values[0]*v.x + values[1]*v.y + values[2], values[3]*v.x + values[4]*v.y + values[5])
    /** Inverse of a non-singular affine matrix, including inherited shear. */
    fun inverse(): Mat3 {
        val a=values[0]; val b=values[1]; val c=values[3]; val d=values[4]
        val determinant=a*d-b*c
        require(determinant.isFinite() && determinant != 0f) { "Singular transform" }
        val tx=values[2]; val ty=values[5]
        return Mat3(listOf(d/determinant,-b/determinant,(b*ty-d*tx)/determinant,
            -c/determinant,a/determinant,(c*tx-a*ty)/determinant,0f,0f,1f))
    }
    companion object { val Identity = Mat3(listOf(1f,0f,0f, 0f,1f,0f, 0f,0f,1f)) }
}
@Serializable data class Transform(val position: Vec2 = Vec2(), val rotation: Float = 0f, val scale: Vec2 = Vec2(1f, 1f)) {
    fun matrix(): Mat3 {
        val r = Math.toRadians(rotation.toDouble()); val c = cos(r).toFloat(); val s = sin(r).toFloat()
        return Mat3(listOf(c*scale.x,-s*scale.y,position.x, s*scale.x,c*scale.y,position.y, 0f,0f,1f))
    }
    fun inverseMap(p: Vec2): Vec2 {
        val d=p-position; val r=Math.toRadians(-rotation.toDouble()); val c=cos(r).toFloat(); val s=sin(r).toFloat()
        return Vec2((c*d.x-s*d.y)/scale.x, (s*d.x+c*d.y)/scale.y)
    }
}
@Serializable data class Rect(val center: Vec2, val size: Vec2) {
    fun contains(p: Vec2) = abs(p.x-center.x)<=size.x/2 && abs(p.y-center.y)<=size.y/2
}
@Serializable data class Color(val r: Float=1f, val g: Float=1f, val b: Float=1f, val a: Float=1f)
