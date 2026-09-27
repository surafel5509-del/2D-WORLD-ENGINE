package world.engine.physics

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import world.engine.math.Vec2

class AlphaColliderTest {
    @Test fun transparentImageProducesFittedLocalPixelHull() {
        val bitmap=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888)
        try {
            for(y in 3..12)for(x in 2..13)bitmap.setPixel(x,y,Color.WHITE)
            val hull=ColliderGeometry.alpha(bitmap,Vec2(64f,64f));assertEquals(4,hull.vertices.size)
            assertEquals(-24f,hull.vertices.minOf { it.x },.01f);assertEquals(24f,hull.vertices.maxOf { it.x },.01f)
            assertEquals(-20f,hull.vertices.minOf { it.y },.01f);assertEquals(20f,hull.vertices.maxOf { it.y },.01f)
        } finally { bitmap.recycle() }
    }
}
