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
    @Test fun solidSpriteRendersAndContextCanBeRecreated() {
        repeat(2) {
            val display=eglGetDisplay(EGL_DEFAULT_DISPLAY)
            val version=IntArray(2)
            assertTrue(eglInitialize(display,version,0,version,1))
            val configs=arrayOfNulls<android.opengl.EGLConfig>(1); val count=IntArray(1)
            val attrs=intArrayOf(EGL_RENDERABLE_TYPE,0x40,EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE)
            assertTrue(eglChooseConfig(display,attrs,0,configs,0,1,count,0)); assertTrue(count[0]>0)
            val context=eglCreateContext(display,configs[0],EGL_NO_CONTEXT,intArrayOf(EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE),0)
            val surface=eglCreatePbufferSurface(display,configs[0],intArrayOf(EGL_WIDTH,64,EGL_HEIGHT,64,EGL_NONE),0)
            try {
                assertTrue(eglMakeCurrent(display,surface,surface,context))
                val errors=mutableListOf<String>(); val renderer=SceneRenderer { errors.add(it) }
                renderer.frame=RenderFrame(scene=Scene(nodes=listOf(Node(components=listOf(TransformComponent(),SpriteComponent(Vec2(32f,32f),Color(1f,0f,0f)))))))
                renderer.onSurfaceCreated(null,null); renderer.onSurfaceChanged(null,64,64); renderer.onDrawFrame(null)
                val pixel=ByteBuffer.allocateDirect(4)
                glReadPixels(32,32,1,1,GL_RGBA,GL_UNSIGNED_BYTE,pixel)
                assertEquals(errors.toString(),emptyList<String>(),errors)
                assertEquals(GL_NO_ERROR,glGetError())
                assertTrue((pixel.get(0).toInt() and 255)>240)
                assertTrue((pixel.get(1).toInt() and 255)<10)
            } finally {
                eglMakeCurrent(display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT)
                eglDestroySurface(display,surface); eglDestroyContext(display,context); eglTerminate(display)
            }
        }
    }
}
