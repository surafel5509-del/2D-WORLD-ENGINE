package world.engine.render

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import world.engine.core.*
import world.engine.math.*

class CameraRigTest {
    @Test fun rotatedViewRoundTrip() {
        val c=Camera2D(Vec2(80f,40f),2f,37f);val p=Vec2(120f,70f);val clip=c.worldToClip(p,640,480)
        val restored=c.screenToWorld((clip.x+1)*320,(1-clip.y)*240,640,480)
        assertEquals(p.x,restored.x,.001f);assertEquals(p.y,restored.y,.001f)
    }
    @Test fun limitsIncludeVisibleViewportAndMultipleViewsTransition() {
        val target=Node().moved(Vec2(300f,0f))
        val camera=Node(components=listOf(TransformComponent(),CameraComponent(follow=target.id,smoothing=0f,deadZone=Vec2(),limitMin=Vec2(-100f,-100f),limitMax=Vec2(100f,100f))))
        val view=CameraRig().update(Scene(nodes=listOf(target,camera)),.1f,100,100).single();assertEquals(50f,view.camera.center.x,.001f)
        val left=Node(components=listOf(TransformComponent(),CameraComponent(viewport=CameraViewport(width=.5f))))
        val right=Node(components=listOf(TransformComponent(Transform(Vec2(100f,0f))),CameraComponent(viewport=CameraViewport(x=.5f,width=.5f))))
        val scene=Scene(nodes=listOf(left,right));val rig=CameraRig();assertEquals(2,rig.update(scene,0f,800,600).size)
        rig.focus(right.id,.5f);assertEquals(50f,rig.update(scene,.25f,800,600).single().camera.center.x,.001f)
    }
}
