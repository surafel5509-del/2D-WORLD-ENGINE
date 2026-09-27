package world.engine.render

import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import world.engine.core.*
import world.engine.math.*
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.floor

data class RenderFrame(val scene: Scene=Scene(), val selected: String?=null, val camera: Camera2D=Camera2D(), val directory: File?=null, val assetPaths: Map<String,String> = emptyMap(), val views: List<CameraView> = emptyList(), val debugLines: List<DebugLine> = emptyList(), val grid: Boolean=true)
/** The renderer only reads immutable snapshots; it never mutates editor entities. */
class SceneRenderer(private val report: (String)->Unit): GLSurfaceView.Renderer {
    @Volatile var frame=RenderFrame()
    private val batch=SpriteBatch()
    private val textures=TextureLoader(report)
    private var width=1; private var height=1
    private var root: File?=null
    private var ready=false
    private var assetPaths: Map<String,String> = emptyMap()
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        ready=false
        try {
            batch.initialize(); textures.initialize(); root=null
            glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA); ready=true
        } catch(e: Exception) { report("OpenGL initialization failed: ${e.message}") }
    }
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) { this.width=width; this.height=height; glViewport(0,0,width,height) }
    override fun onDrawFrame(gl: GL10?) {
        glClearColor(.065f,.075f,.09f,1f); glClear(GL_COLOR_BUFFER_BIT)
        if(!ready) return
        try {
            val f=frame
            if(root!=f.directory || assetPaths!=f.assetPaths) { textures.clear(); root=f.directory; assetPaths=f.assetPaths }
            val matrices=f.scene.worldMatrices()
            val selection=f.selected?.let { f.scene.descendants(it) }.orEmpty()
            val views=f.views.ifEmpty { listOf(CameraView("editor",f.camera)) }
            glEnable(GL_SCISSOR_TEST)
            views.forEach { view ->
                val bounds=view.viewport
                val w=(width*bounds.width).toInt().coerceAtLeast(1);val h=(height*bounds.height).toInt().coerceAtLeast(1)
                val x=(width*bounds.x).toInt();val y=height-(height*(bounds.y+bounds.height)).toInt()
                glViewport(x,y,w,h);glScissor(x,y,w,h);glClear(GL_COLOR_BUFFER_BIT)
                val camera=view.camera
                batch.begin(camera,w,h)
                if(f.grid) {
                    val corners=listOf(camera.screenToWorld(0f,0f,w,h),camera.screenToWorld(w.toFloat(),0f,w,h),camera.screenToWorld(0f,h.toFloat(),w,h),camera.screenToWorld(w.toFloat(),h.toFloat(),w,h))
                    val a=Vec2(corners.minOf { it.x },corners.minOf { it.y });val b=Vec2(corners.maxOf { it.x },corners.maxOf { it.y })
                    val spacing=if(camera.zoom<.5f)256f else 64f;val color=Color(.14f,.16f,.19f)
                    var gx=floor(a.x/spacing)*spacing
                    repeat(minOf(2000,((b.x-gx)/spacing).toInt().coerceAtLeast(0)+1)) { batch.line(textures.white,Vec2(gx,a.y),Vec2(gx,b.y),color);gx+=spacing }
                    var gy=floor(a.y/spacing)*spacing
                    repeat(minOf(2000,((b.y-gy)/spacing).toInt().coerceAtLeast(0)+1)) { batch.line(textures.white,Vec2(a.x,gy),Vec2(b.x,gy),color);gy+=spacing }
                }
                f.scene.nodes.forEach { n -> n.sprite?.let { sprite ->
                    var matrix=matrices.getValue(n.id)
                    n.component<ParallaxComponent>()?.let { layer ->
                        val values=matrix.values.toMutableList();values[2]+=camera.center.x*(1-layer.factor.x);values[5]+=camera.center.y*(1-layer.factor.y);matrix=Mat3(values)
                    }
                    val ref=sprite.assetId ?: sprite.asset?.removePrefix("Sprites/")?.removeSuffix(".png")
                    val path=ref?.let { f.assetPaths[it] } ?: sprite.asset ?: sprite.assetId?.let { "Sprites/$it-missing.png" }
                    if(n.id in selection)batch.drawMatrix(textures.white,matrix,sprite.size+Vec2(8/camera.zoom,8/camera.zoom),Color(1f,.75f,.15f))
                    batch.drawMatrix(textures.get(root,path),matrix,sprite.size,sprite.tint)
                } }
                f.debugLines.forEach { batch.line(textures.white,it.start,it.end,it.color) }
                batch.flush()
            }
            glDisable(GL_SCISSOR_TEST);glViewport(0,0,width,height)
        } catch(e: Exception) { ready=false; report("Rendering stopped: ${e.message}. Reopen the project to retry.") }
    }
}
