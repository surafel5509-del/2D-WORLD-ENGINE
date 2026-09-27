package world.engine.math

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MathTest {
    @Test fun vectorArithmetic() { assertEquals(Vec2(4f,6f),Vec2(1f,2f)+Vec2(3f,4f)); assertEquals(5f,Vec2(3f,4f).length()) }
    @Test fun affineRoundTrip() {
        val t=Transform(Vec2(7f,-3f),37f,Vec2(2f,.5f)); val p=Vec2(8f,9f)
        val result=t.inverseMap(t.matrix().map(p))
        assertEquals(p.x,result.x,.0001f); assertEquals(p.y,result.y,.0001f)
        assertEquals(t.matrix().map(p),(Mat3.Identity*t.matrix()).map(p))
    }
    @Test fun rectEdges() { val r=Rect(Vec2(),Vec2(10f,20f)); assertTrue(r.contains(Vec2(5f,-10f))); assertFalse(r.contains(Vec2(5.01f,0f))) }
}
