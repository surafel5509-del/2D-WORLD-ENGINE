package world.engine.app

import android.opengl.EGL14.*
import android.opengl.GLES30.*
import org.junit.Assert.*
import org.junit.Test
import world.engine.core.*
import world.engine.math.*
import world.engine.render.*
import java.nio.ByteBuffer

/** Real offscreen GLES 3 render/readback; requires an ES 3-capable device or emulator. */
class GlRenderTest {
    private fun withGl(block: (SceneRenderer)->Unit) {
        val display=eglGetDisplay(EGL_DEFAULT_DISPLAY);val version=IntArray(2)
        assertTrue(eglInitialize(display,version,0,version,1))
        val configs=arrayOfNulls<android.opengl.EGLConfig>(1);val count=IntArray(1)
        val attrs=intArrayOf(EGL_RENDERABLE_TYPE,0x40,EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE)
        assertTrue(eglChooseConfig(display,attrs,0,configs,0,1,count,0));assertTrue(count[0]>0)
        val context=eglCreateContext(display,configs[0],EGL_NO_CONTEXT,intArrayOf(EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE),0)
        val surface=eglCreatePbufferSurface(display,configs[0],intArrayOf(EGL_WIDTH,64,EGL_HEIGHT,64,EGL_NONE),0)
        try {
            assertTrue(eglMakeCurrent(display,surface,surface,context))
            val renderer=SceneRenderer { fail(it) };renderer.onSurfaceCreated(null,null);renderer.onSurfaceChanged(null,64,64)
            block(renderer);assertEquals(GL_NO_ERROR,glGetError())
        } finally {
            eglMakeCurrent(display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT)
            eglDestroySurface(display,surface);eglDestroyContext(display,context);eglTerminate(display)
        }
    }
    private fun pixel(x: Int,y: Int): List<Int> {
        val bytes=ByteBuffer.allocateDirect(4);glReadPixels(x,y,1,1,GL_RGBA,GL_UNSIGNED_BYTE,bytes)
        return (0..3).map { bytes.get(it).toInt() and 255 }
    }
    @Test fun solidSpriteRendersAndContextCanBeRecreated() {
        repeat(2) { withGl { renderer ->
            renderer.frame=RenderFrame(scene=Scene(nodes=listOf(Node(components=listOf(TransformComponent(),SpriteComponent(Vec2(32f,32f),Color(1f,0f,0f)))))))
            renderer.onDrawFrame(null);val p=pixel(32,32);assertTrue(p[0]>240);assertTrue(p[1]<10)
        } }
    }
    @Test fun rotatedSplitViewsKeepTheirOwnScissoredPixels() = withGl { renderer ->
        val red=Node(components=listOf(TransformComponent(Transform(Vec2(-60f,0f))),SpriteComponent(Vec2(16f,32f),Color(1f,0f,0f))))
        val blue=Node(components=listOf(TransformComponent(Transform(Vec2(60f,0f))),SpriteComponent(Vec2(16f,32f),Color(0f,0f,1f))))
        val views=listOf(CameraView("left",Camera2D(Vec2(-60f,0f),rotation=90f),CameraViewport(width=.5f)),CameraView("right",Camera2D(Vec2(60f,0f)),CameraViewport(x=.5f,width=.5f)))
        renderer.frame=RenderFrame(scene=Scene(nodes=listOf(red,blue)),views=views,grid=false);renderer.onDrawFrame(null)
        assertTrue(pixel(16,32)[0]>240);assertTrue("Rotated rectangle should span the left viewport horizontally",pixel(28,32)[0]>240)
        assertTrue(pixel(48,32)[2]>240);assertTrue(pixel(60,32)[2]<80)
    }
    @Test fun parallaxAndActualGlLinesRenderIntoFramebuffer() = withGl { renderer ->
        val node=Node(components=listOf(TransformComponent(),SpriteComponent(Vec2(16f,16f),Color(1f,0f,1f)),ParallaxComponent(Vec2())))
        renderer.frame=RenderFrame(scene=Scene(nodes=listOf(node)),camera=Camera2D(Vec2(100f,0f)),debugLines=listOf(DebugLine(Vec2(76f,-12f),Vec2(124f,-12f),Color(0f,1f,0f))),grid=false)
        renderer.onDrawFrame(null);assertTrue(pixel(32,32)[0]>240);assertTrue(pixel(32,32)[2]>240)
        assertTrue("Expected real green GL line pixels",(19..21).any { y -> (30..33).any { x -> val p=pixel(x,y);p[1]>240 && p[0]<10 && p[2]<10 } })
        renderer.frame=renderer.frame.copy(camera=Camera2D(Vec2(200f,0f)),debugLines=emptyList());renderer.onDrawFrame(null)
        assertTrue("Zero-factor parallax must remain screen-fixed",pixel(32,32)[0]>240)
    }
}
