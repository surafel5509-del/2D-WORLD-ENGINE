package world.engine.viewport

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.DragEvent
import android.view.ScaleGestureDetector
import android.view.KeyEvent
import android.view.InputDevice
import world.engine.core.InputSource
import world.engine.core.DebugLine
import world.engine.core.Scene
import world.engine.math.Vec2
import world.engine.render.*
import java.io.File

/** One-finger sprite drag or empty-space pan; two-finger focal-point zoom; long-press selection. */
class WorldViewport(context: Context): GLSurfaceView(context) {
    var onSelect: (String?)->Unit = {}
    var onMove: (String,Vec2,Boolean)->Unit = { _,_,_-> }
    var onContext: (String?)->Unit = {}
    var onError: (String)->Unit = {}
    var onAssetDrop: (String,Vec2)->Unit = { _,_ -> }
    var onColliderPoint: (Vec2)->Unit = {}
    var onPhysicalInput: (InputSource,Int,Float,Int)->Unit = { _,_,_,_-> }
    var onReleaseInput: ()->Unit = {}
    var onViewportSize: (Int,Int)->Unit = { _,_-> }
    private var playing=false
    private var drawing=false
    private var views: List<CameraView> = emptyList()
    private var debugLines: List<DebugLine> = emptyList()
    private var assetPaths: Map<String,String> = emptyMap()
    private val renderer=SceneRenderer { message -> post { onError(message) } }
    private var scene=Scene()
    private var selected: String?=null
    private var directory: File?=null
    private var camera=Camera2D()
    private var dragging: String?=null
    private var offset=Vec2()
    private var last=Vec2()
    private var multiple=false
    private var longPressed=false
    private val scale=ScaleGestureDetector(context,object: ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val before=world(detector.focusX,detector.focusY)
            camera=camera.copy(zoom=(camera.zoom*detector.scaleFactor).coerceIn(.15f,8f))
            camera=camera.copy(center=camera.center+before-world(detector.focusX,detector.focusY)); publish(); return true
        }
    })
    private val gestures=GestureDetector(context,object: GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent)=true
        override fun onLongPress(e: MotionEvent) {
            if(!multiple) { finish(); longPressed=true; onContext(scene.pick(world(e.x,e.y))?.id) }
        }
    })
    init {
        isFocusableInTouchMode=true
        setEGLContextClientVersion(3); preserveEGLContextOnPause=true; setRenderer(renderer); renderMode=RENDERMODE_WHEN_DIRTY
        setOnDragListener { _, event ->
            when(event.action) {
                DragEvent.ACTION_DRAG_STARTED -> isEnabled && !playing && !drawing && event.clipDescription?.label=="2DWorldAsset"
                DragEvent.ACTION_DROP -> {
                    if(isEnabled && !playing && !drawing) event.clipData?.getItemAt(0)?.text?.toString()?.let { onAssetDrop(it,world(event.x,event.y)) }
                    true
                }
                else -> true
            }
        }
        contentDescription="Scene viewport. Drag sprites to move; drag empty space to pan; pinch to zoom; long press for actions."
    }
    fun update(scene: Scene, selected: String?, directory: File?, assetPaths: Map<String,String> = emptyMap(), playing: Boolean=false, drawing: Boolean=false, views: List<CameraView> = emptyList(), debugLines: List<DebugLine> = emptyList()) {
        if(playing && (!this.playing || !hasFocus()) && isEnabled)requestFocus()
        this.playing=playing;this.drawing=drawing;this.views=views;this.debugLines=debugLines
        this.assetPaths=assetPaths
        if(this.directory!=directory) camera=Camera2D()
        this.scene=scene; this.selected=selected; this.directory=directory; publish()
    }
    fun resetCamera() { camera=Camera2D(); publish() }
    private fun publish() { renderer.frame=RenderFrame(scene,if(playing)null else selected,camera,directory,assetPaths,views,debugLines,!playing); requestRender() }
    private fun world(x: Float,y: Float)=camera.screenToWorld(x,y,width,height)
    private fun finish() {
        dragging?.let { id -> scene.nodes.find { it.id==id }?.let { onMove(id,it.transform.position,true) } }; dragging=null
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(!isEnabled) return true
        parent?.requestDisallowInterceptTouchEvent(true)
        if(playing) {
            if(e.actionMasked==MotionEvent.ACTION_DOWN)requestFocus()
            if(e.isFromSource(InputDevice.SOURCE_MOUSE))mouse(e)
            return true
        }
        if(drawing) { if(e.actionMasked==MotionEvent.ACTION_UP)onColliderPoint(world(e.x,e.y));return true }
        scale.onTouchEvent(e); gestures.onTouchEvent(e)
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                multiple=false; longPressed=false; last=Vec2(e.x,e.y)
                val point=world(e.x,e.y); val node=scene.pick(point)
                dragging=node?.id; offset=point-(node?.let { scene.worldMatrices().getValue(it.id).map(Vec2()) } ?: point); onSelect(node?.id)
            }
            MotionEvent.ACTION_POINTER_DOWN -> { finish(); multiple=true }
            MotionEvent.ACTION_MOVE -> if(!multiple && !longPressed) {
                val id=dragging
                if(id!=null) {
                    val worldPosition=world(e.x,e.y)-offset
                    val node=scene.nodes.find { it.id==id }
                    val p=node?.parent?.let { scene.worldMatrices().getValue(it).inverse().map(worldPosition) } ?: worldPosition
                    scene.nodes.find { it.id==id }?.let { scene=scene.replace(it.moved(p)) }; publish(); onMove(id,p,false)
                } else { camera=camera.copy(center=camera.center+Vec2((last.x-e.x)/camera.zoom,(e.y-last.y)/camera.zoom)); publish() }
                last=Vec2(e.x,e.y)
            }
            MotionEvent.ACTION_UP -> { finish(); performClick() }
            MotionEvent.ACTION_CANCEL -> finish()
        }
        return true
    }

    override fun onSizeChanged(w: Int,h: Int,oldw: Int,oldh: Int) { super.onSizeChanged(w,h,oldw,oldh);onViewportSize(w,h) }
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) { super.onWindowFocusChanged(hasWindowFocus);if(!hasWindowFocus)onReleaseInput() }
    override fun onKeyDown(keyCode: Int,event: KeyEvent): Boolean {
        if(playing && keyCode!=KeyEvent.KEYCODE_BACK) { onPhysicalInput(InputSource.KEY,keyCode,1f,event.deviceId);return true }
        return super.onKeyDown(keyCode,event)
    }
    override fun onKeyUp(keyCode: Int,event: KeyEvent): Boolean {
        if(playing && keyCode!=KeyEvent.KEYCODE_BACK) { onPhysicalInput(InputSource.KEY,keyCode,0f,event.deviceId);return true }
        return super.onKeyUp(keyCode,event)
    }
    private fun mouse(e: MotionEvent) { listOf(1,2,4,8,16).forEach { onPhysicalInput(InputSource.MOUSE_BUTTON,it,if(e.buttonState and it!=0)1f else 0f,e.deviceId) } }
    override fun onGenericMotionEvent(e: MotionEvent): Boolean {
        if(playing && e.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
            scene.inputMap.bindings.filter { it.source==InputSource.GAMEPAD_AXIS }.map { it.code }.distinct().forEach { onPhysicalInput(InputSource.GAMEPAD_AXIS,it,e.getAxisValue(it),e.deviceId) }
            return true
        }
        if(playing && e.isFromSource(InputDevice.SOURCE_MOUSE)) { mouse(e);return true }
        return super.onGenericMotionEvent(e)
    }
}
