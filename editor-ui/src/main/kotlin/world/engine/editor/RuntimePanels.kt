package world.engine.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import world.engine.core.*
import world.engine.math.*

/** Touch-first authoring forms. Every Apply routes through scene validation and undo history. */
@Composable internal fun RuntimeInspector(scene: Scene,node: Node?,vm: EditorViewModel,draw: ()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("PHYSICS / CAMERA / INPUT ACTOR",style=MaterialTheme.typography.titleMedium)
        if(node==null) { Text("Select an entity in the hierarchy first.");return@Column }
        Text(node.name,style=MaterialTheme.typography.titleLarge)
        val body=node.component<RigidBodyComponent>()
        if(body==null) {
            Button(onClick={vm.updateNode(node.withComponent(RigidBodyComponent(colliders=listOf(Collider(size=node.sprite?.size ?: Vec2(96f,96f))))))}){Text("Add rigid body")}
            if(node.sprite!=null)TextButton(onClick=vm::separateAnimatedVisual){Text("Create physics parent + visual child")}
        } else BodyEditor(node,body,vm,draw)
        val controller=node.component<InputControllerComponent>()
        if(controller==null)TextButton(onClick={vm.updateNode(node.withComponent(InputControllerComponent()))}){Text("Add input controller")}
        else key(node.id,controller) {
            var speed by remember { mutableStateOf(controller.speed.toString()) };var jump by remember { mutableStateOf(controller.jumpSpeed.toString()) }
            var horizontal by remember { mutableStateOf(controller.horizontal) };var vertical by remember { mutableStateOf(controller.vertical) };var jumpAction by remember { mutableStateOf(controller.jump) }
            Text("Input controller")
            RuntimeChoice("Mode",controller.kind.name,ControllerKind.entries.map { it.name }){vm.updateNode(node.withComponent(controller.copy(kind=ControllerKind.valueOf(it))))}
            RuntimeField("Speed (pixels/second)",speed){speed=it};RuntimeField("Jump speed",jump){jump=it}
            RuntimeField("Horizontal action",horizontal){horizontal=it};RuntimeField("Vertical action",vertical){vertical=it};RuntimeField("Jump action",jumpAction){jumpAction=it}
            TextButton(onClick={vm.parseRuntime { vm.updateNode(node.withComponent(controller.copy(speed=speed.toFloat(),jumpSpeed=jump.toFloat(),horizontal=horizontal,vertical=vertical,jump=jumpAction))) }}){Text("Apply controller")}
            Text("Platformer jump requires a touching, non-sensor support contact. For top-down rigid bodies set gravity scale to zero.",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={vm.removeRuntimeComponent(InputControllerComponent::class.java)}){Text("Remove controller")}
        }
        val camera=node.component<CameraComponent>()
        if(camera==null)TextButton(onClick={vm.updateNode(node.withComponent(CameraComponent()))}){Text("Add camera")}
        else CameraEditor(scene,node,camera,vm)
        val parallax=node.component<ParallaxComponent>()
        if(node.sprite!=null && body==null) {
            if(parallax==null)TextButton(onClick={vm.updateNode(node.withComponent(ParallaxComponent()))}){Text("Add parallax layer")}
            else key(node.id,parallax) {
                var x by remember { mutableStateOf(parallax.factor.x.toString()) };var y by remember { mutableStateOf(parallax.factor.y.toString()) }
                RuntimeField("Parallax X (0 screen-fixed, 1 world)",x){x=it};RuntimeField("Parallax Y",y){y=it}
                TextButton(onClick={vm.parseRuntime { vm.updateNode(node.withComponent(ParallaxComponent(Vec2(x.toFloat(),y.toFloat())))) }}){Text("Apply parallax")}
                TextButton(onClick={vm.removeRuntimeComponent(ParallaxComponent::class.java)}){Text("Remove parallax")}
            }
        }
        node.component<AnimatorComponent>()?.let { animator -> key(node.id,animator) {
            var speed by remember { mutableStateOf(animator.speed.toString()) };RuntimeField("Animator speed 0.01–10",speed){speed=it}
            TextButton(onClick={vm.parseRuntime { vm.updateNode(node.withComponent(animator.copy(speed=speed.toFloat()))) }}){Text("Apply animator speed")}
        } }
    }
}
@Composable private fun BodyEditor(node: Node,body: RigidBodyComponent,vm: EditorViewModel,draw: ()->Unit) {
    key(node.id,body) {
        var gravity by remember { mutableStateOf(body.gravityScale.toString()) };var damping by remember { mutableStateOf(body.linearDamping.toString()) }
        var vx by remember { mutableStateOf(body.velocity.x.toString()) };var vy by remember { mutableStateOf(body.velocity.y.toString()) };var angular by remember { mutableStateOf(body.angularVelocity.toString()) }
        RuntimeChoice("Body",body.kind.name,BodyKind.entries.map { it.name }){vm.updateNode(node.withComponent(body.copy(kind=BodyKind.valueOf(it))))}
        Toggle("Fixed rotation",body.fixedRotation){vm.updateNode(node.withComponent(body.copy(fixedRotation=it)))}
        Toggle("Bullet / continuous collision",body.bullet){vm.updateNode(node.withComponent(body.copy(bullet=it)))}
        RuntimeField("Gravity scale",gravity){gravity=it};RuntimeField("Linear damping",damping){damping=it}
        RuntimeField("Initial velocity X",vx){vx=it};RuntimeField("Initial velocity Y",vy){vy=it};RuntimeField("Angular velocity degrees/sec",angular){angular=it}
        TextButton(onClick={vm.parseRuntime { vm.updateNode(node.withComponent(body.copy(gravityScale=gravity.toFloat(),linearDamping=damping.toFloat(),velocity=Vec2(vx.toFloat(),vy.toFloat()),angularVelocity=angular.toFloat()))) }}){Text("Apply body settings")}
        body.colliders.forEachIndexed { index,collider -> ColliderEditor(index,collider,body.colliders.size>1,{ updated -> vm.updateNode(node.withComponent(body.copy(colliders=body.colliders.mapIndexed { i,c -> if(i==index)updated else c }))) },{vm.updateNode(node.withComponent(body.copy(colliders=body.colliders.filterIndexed { i,_->i!=index })))},vm) }
        TextButton(enabled=body.colliders.size<32,onClick={vm.updateNode(node.withComponent(body.copy(colliders=body.colliders+Collider(size=node.sprite?.size ?: Vec2(48f,48f)))))}){Text("Add compound part")}
        TextButton(onClick=draw){Text("Draw convex polygon in viewport")}
        TextButton(enabled=node.sprite!=null,onClick=vm::alphaCollider){Text("Replace colliders with alpha hull")}
        TextButton(onClick={vm.removeRuntimeComponent(RigidBodyComponent::class.java)}){Text("Remove body and connected joints")}
    }
}
@Composable private fun ColliderEditor(index: Int,c: Collider,removable: Boolean,apply: (Collider)->Unit,remove: ()->Unit,vm: EditorViewModel) {
    key(index,c) {
        var shape by remember { mutableStateOf(c.shape) };var width by remember { mutableStateOf(c.size.x.toString()) };var height by remember { mutableStateOf(c.size.y.toString()) };var radius by remember { mutableStateOf(c.radius.toString()) }
        var x by remember { mutableStateOf(c.offset.x.toString()) };var y by remember { mutableStateOf(c.offset.y.toString()) };var density by remember { mutableStateOf(c.density.toString()) };var friction by remember { mutableStateOf(c.friction.toString()) };var bounce by remember { mutableStateOf(c.restitution.toString()) }
        var vertices by remember { mutableStateOf(c.vertices.joinToString("; ") { "${it.x},${it.y}" }) };var sensor by remember { mutableStateOf(c.sensor) }
        Text("Collider ${index+1}",style=MaterialTheme.typography.titleMedium)
        RuntimeChoice("Shape",shape.name,ShapeKind.entries.map { it.name }){shape=ShapeKind.valueOf(it)}
        RuntimeField("Width",width){width=it};RuntimeField("Height (capsule ≥ width)",height){height=it};RuntimeField("Circle radius",radius){radius=it}
        RuntimeField("Offset X",x){x=it};RuntimeField("Offset Y",y){y=it}
        if(shape==ShapeKind.POLYGON)RuntimeField("Convex vertices: x,y; x,y; x,y",vertices){vertices=it}
        RuntimeField("Density kg/m²",density){density=it};RuntimeField("Friction",friction){friction=it};RuntimeField("Restitution 0–1",bounce){bounce=it};Toggle("Sensor (no collision response)",sensor){sensor=it}
        TextButton(onClick={vm.parseRuntime {
            val points=if(shape==ShapeKind.POLYGON)vertices.split(';').map { pair -> val v=pair.trim().split(',');require(v.size==2);Vec2(v[0].trim().toFloat(),v[1].trim().toFloat()) } else emptyList()
            apply(c.copy(shape=shape,size=Vec2(width.toFloat(),height.toFloat()),radius=radius.toFloat(),offset=Vec2(x.toFloat(),y.toFloat()),density=density.toFloat(),friction=friction.toFloat(),restitution=bounce.toFloat(),sensor=sensor,vertices=points))
        }}){Text("Apply collider")}
        TextButton(enabled=removable,onClick=remove){Text("Remove collider")}
    }
}
@Composable private fun CameraEditor(scene: Scene,node: Node,c: CameraComponent,vm: EditorViewModel) {
    key(node.id,c) {
        var follow by remember { mutableStateOf(c.follow) };var smoothing by remember { mutableStateOf(c.smoothing.toString()) };var zoom by remember { mutableStateOf(c.zoom.toString()) };var rotation by remember { mutableStateOf(c.rotation.toString()) }
        var dx by remember { mutableStateOf(c.deadZone.x.toString()) };var dy by remember { mutableStateOf(c.deadZone.y.toString()) }
        var minX by remember { mutableStateOf(c.limitMin?.x?.toString() ?: "") };var minY by remember { mutableStateOf(c.limitMin?.y?.toString() ?: "") };var maxX by remember { mutableStateOf(c.limitMax?.x?.toString() ?: "") };var maxY by remember { mutableStateOf(c.limitMax?.y?.toString() ?: "") }
        var viewport by remember { mutableStateOf(c.viewport) }
        Text("Camera")
        EntityChoice("Follow",follow,scene.nodes,true){follow=it}
        RuntimeField("Smoothing seconds (0 instant)",smoothing){smoothing=it};RuntimeField("Zoom",zoom){zoom=it};RuntimeField("Additional rotation degrees",rotation){rotation=it}
        RuntimeField("Dead-zone width",dx){dx=it};RuntimeField("Dead-zone height",dy){dy=it}
        RuntimeField("Limit minimum X (empty = no limits)",minX){minX=it};RuntimeField("Limit minimum Y",minY){minY=it};RuntimeField("Limit maximum X",maxX){maxX=it};RuntimeField("Limit maximum Y",maxY){maxY=it}
        RuntimeChoice("Viewport","${viewport.x},${viewport.y},${viewport.width},${viewport.height}",listOf("Full","Left half","Right half","Top half","Bottom half")){viewport=when(it){"Left half"->CameraViewport(width=.5f);"Right half"->CameraViewport(x=.5f,width=.5f);"Top half"->CameraViewport(height=.5f);"Bottom half"->CameraViewport(y=.5f,height=.5f);else->CameraViewport()}}
        Toggle("Enabled",c.enabled){vm.updateNode(node.withComponent(c.copy(enabled=it)))}
        TextButton(onClick={vm.parseRuntime { val limited=listOf(minX,minY,maxX,maxY).any { it.isNotBlank() };vm.updateNode(node.withComponent(c.copy(follow=follow,smoothing=smoothing.toFloat(),zoom=zoom.toFloat(),rotation=rotation.toFloat(),deadZone=Vec2(dx.toFloat(),dy.toFloat()),limitMin=if(limited)Vec2(minX.toFloat(),minY.toFloat()) else null,limitMax=if(limited)Vec2(maxX.toFloat(),maxY.toFloat()) else null,viewport=viewport))) }}){Text("Apply camera")}
        TextButton(onClick={vm.editRuntimeScene(scene.copy(activeCamera=node.id))}){Text("Use as active camera")}
        TextButton(onClick={vm.editRuntimeScene(scene.copy(activeCamera=null))}){Text("Render all enabled cameras")}
        TextButton(onClick={vm.removeRuntimeComponent(CameraComponent::class.java)}){Text("Remove camera")}
    }
}
@Composable internal fun InputMapEditor(scene: Scene,vm: EditorViewModel) {
    var selected by remember { mutableIntStateOf(-1) };var action by remember { mutableStateOf("MoveX") };var source by remember { mutableStateOf(InputSource.KEY) };var code by remember { mutableStateOf("29") };var scale by remember { mutableStateOf("-1") };var dead by remember { mutableStateOf("0.15") }
    var listening by remember { mutableStateOf(false) };val focus=remember { FocusRequester() }
    LaunchedEffect(listening){if(listening)focus.requestFocus()}
    Column(Modifier.fillMaxSize().focusRequester(focus).onPreviewKeyEvent { e ->
        if(listening && e.nativeKeyEvent.action==android.view.KeyEvent.ACTION_DOWN) { code=e.nativeKeyEvent.keyCode.toString();source=InputSource.KEY;listening=false;true } else false
    }.focusable().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("INPUT MAP",style=MaterialTheme.typography.titleMedium)
        RuntimeField("Action name",action){action=it};RuntimeChoice("Source",source.name,InputSource.entries.map { it.name }){source=InputSource.valueOf(it)}
        RuntimeField("Android code / axis / mouse button",code){code=it};RuntimeField("Scale (-1 reverses)",scale){scale=it};RuntimeField("Axis dead zone 0–0.95",dead){dead=it}
        Text("Gamepad X=0, Y=1, hat X=15/Y=16. Mouse primary=1, secondary=2, middle=4. Touch uses the action name.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={listening=!listening}){Text(if(listening)"Listening… press a key (tap to cancel)" else "Listen for key / gamepad button")}
        TextButton(onClick={vm.parseRuntime {
            val binding=InputBinding(action,source,code.toInt(),scale.toFloat(),dead.toFloat())
            val bindings=scene.inputMap.bindings.toMutableList();if(selected in bindings.indices)bindings[selected]=binding else bindings.add(binding)
            vm.editRuntimeScene(scene.copy(inputMap=InputMap(bindings)));selected=-1
        }}){Text(if(selected>=0)"Update binding" else "Add binding")}
        if(selected>=0)TextButton(onClick={selected=-1}){Text("Cancel binding edit")}
        scene.inputMap.bindings.forEachIndexed { index,b -> Row {
            TextButton(modifier=Modifier.weight(1f),onClick={selected=index;action=b.action;source=b.source;code=b.code.toString();scale=b.scale.toString();dead=b.deadZone.toString()}){Text("${b.action}: ${b.source} ${b.code} × ${b.scale}")}
            TextButton(onClick={selected=-1;vm.editRuntimeScene(scene.copy(inputMap=InputMap(scene.inputMap.bindings.filterIndexed { i,_->i!=index })))}){Text("Remove")}
        } }
    }
}
@Composable internal fun JointEditor(scene: Scene,vm: EditorViewModel) {
    val bodies=scene.nodes.filter { it.component<RigidBodyComponent>()!=null }
    var selected by remember { mutableStateOf<String?>(null) };var kind by remember { mutableStateOf(JointKind.DISTANCE) };var a by remember { mutableStateOf<String?>(null) };var b by remember { mutableStateOf<String?>(null) }
    var ax by remember { mutableStateOf("0") };var ay by remember { mutableStateOf("0") };var bx by remember { mutableStateOf("0") };var by by remember { mutableStateOf("0") };var axisX by remember { mutableStateOf("1") };var axisY by remember { mutableStateOf("0") }
    var length by remember { mutableStateOf("100") };var frequency by remember { mutableStateOf("4") };var damping by remember { mutableStateOf("0.7") };var motor by remember { mutableStateOf(false) };var speed by remember { mutableStateOf("0") };var force by remember { mutableStateOf("100") };var limit by remember { mutableStateOf(false) };var lower by remember { mutableStateOf("-45") };var upper by remember { mutableStateOf("45") };var collide by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("JOINTS",style=MaterialTheme.typography.titleMedium)
        RuntimeChoice("Type",kind.name,JointKind.entries.map { it.name }){kind=JointKind.valueOf(it)}
        EntityChoice("Body A",a,bodies,false){a=it};EntityChoice("Body B",b,bodies,false){b=it}
        RuntimeField("Local anchor A X",ax){ax=it};RuntimeField("Local anchor A Y",ay){ay=it};RuntimeField("Local anchor B X",bx){bx=it};RuntimeField("Local anchor B Y",by){by=it}
        RuntimeField("Axis X (prismatic/wheel)",axisX){axisX=it};RuntimeField("Axis Y",axisY){axisY=it}
        RuntimeField("Length / rope max (pixels)",length){length=it};RuntimeField("Spring / wheel frequency Hz",frequency){frequency=it};RuntimeField("Damping ratio",damping){damping=it}
        Toggle("Collide connected",collide){collide=it}
        if(kind in listOf(JointKind.REVOLUTE,JointKind.PRISMATIC,JointKind.WHEEL)) {
            Toggle("Motor",motor){motor=it};RuntimeField("Motor speed (degrees/sec, prismatic pixels/sec)",speed){speed=it};RuntimeField("Max motor force/torque (SI)",force){force=it}
        }
        if(kind in listOf(JointKind.REVOLUTE,JointKind.PRISMATIC)) { Toggle("Limits",limit){limit=it};RuntimeField("Lower (degrees, prismatic pixels)",lower){lower=it};RuntimeField("Upper",upper){upper=it} }
        TextButton(onClick={vm.parseRuntime {
            val joint=JointSpec(id=selected ?: java.util.UUID.randomUUID().toString(),kind=kind,bodyA=requireNotNull(a){"Choose body A"},bodyB=requireNotNull(b){"Choose body B"},anchorA=Vec2(ax.toFloat(),ay.toFloat()),anchorB=Vec2(bx.toFloat(),by.toFloat()),axis=Vec2(axisX.toFloat(),axisY.toFloat()),length=length.toFloat(),frequency=frequency.toFloat(),damping=damping.toFloat(),collideConnected=collide,motor=motor,motorSpeed=speed.toFloat(),maxMotorForce=force.toFloat(),limit=limit,lower=lower.toFloat(),upper=upper.toFloat())
            vm.editRuntimeScene(scene.copy(joints=scene.joints.filterNot { it.id==joint.id }+joint));selected=null
        }}){Text(if(selected==null)"Create joint" else "Update joint")}
        if(selected!=null)TextButton(onClick={selected=null}){Text("Cancel edit")}
        scene.joints.forEach { j -> Row {
            TextButton(modifier=Modifier.weight(1f),onClick={selected=j.id;kind=j.kind;a=j.bodyA;b=j.bodyB;ax=j.anchorA.x.toString();ay=j.anchorA.y.toString();bx=j.anchorB.x.toString();by=j.anchorB.y.toString();axisX=j.axis.x.toString();axisY=j.axis.y.toString();length=j.length.toString();frequency=j.frequency.toString();damping=j.damping.toString();motor=j.motor;speed=j.motorSpeed.toString();force=j.maxMotorForce.toString();limit=j.limit;lower=j.lower.toString();upper=j.upper.toString();collide=j.collideConnected}){Text("${j.kind}: ${bodies.find { it.id==j.bodyA }?.name} ↔ ${bodies.find { it.id==j.bodyB }?.name}")}
            TextButton(onClick={selected=null;vm.editRuntimeScene(scene.copy(joints=scene.joints-j))}){Text("Remove")}
        } }
    }
}
@Composable internal fun RuntimeField(label: String,value: String,change: (String)->Unit) { OutlinedTextField(value,change,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth()) }
@Composable internal fun RuntimeChoice(label: String,value: String,choices: List<String>,choose: (String)->Unit) {
    var open by remember { mutableStateOf(false) }
    Box { TextButton(onClick={open=true}){Text("$label: $value")};DropdownMenu(open,onDismissRequest={open=false},modifier=Modifier.heightIn(max=320.dp)){choices.forEach { item -> DropdownMenuItem(text={Text(item)},onClick={open=false;choose(item)})}} }
}
@Composable private fun EntityChoice(label: String,value: String?,nodes: List<Node>,allowNone: Boolean,choose: (String?)->Unit) {
    var open by remember { mutableStateOf(false) }
    Box { TextButton(onClick={open=true}){Text("$label: ${nodes.find { it.id==value }?.name ?: "None"}")};DropdownMenu(open,onDismissRequest={open=false},modifier=Modifier.heightIn(max=320.dp)) {
        if(allowNone)DropdownMenuItem(text={Text("None")},onClick={open=false;choose(null)})
        nodes.forEach { n -> DropdownMenuItem(text={Text(n.name)},onClick={open=false;choose(n.id)}) }
    } }
}
@Composable private fun Toggle(label: String,value: Boolean,change: (Boolean)->Unit) { Row { Checkbox(value,change);Text(label,Modifier.padding(top=12.dp)) } }
