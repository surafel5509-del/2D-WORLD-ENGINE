package world.engine.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import world.engine.core.*
import world.engine.math.Vec2
import world.engine.render.ShakePreset

@Composable internal fun PlayInputOverlay(scene: Scene,selected: String?,vm: EditorViewModel,modifier: Modifier=Modifier) {
    val actor=scene.nodes.find { it.id==selected && it.component<InputControllerComponent>()!=null } ?: scene.nodes.firstOrNull { it.component<InputControllerComponent>()!=null }
    val control=actor?.component<InputControllerComponent>() ?: InputControllerComponent()
    val actions=scene.inputMap.bindings.filter { it.source==InputSource.TOUCH }.map { it.action }.distinct()
    Row(modifier.padding(12.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.SpaceBetween) {
        if(control.horizontal in actions || control.vertical in actions)Joystick { vector -> vm.inputTouch(control.horizontal,vector.x);vm.inputTouch(control.vertical,vector.y) }
        Spacer(Modifier.weight(1f))
        Row(Modifier.weight(2f).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            actions.filterNot { it==control.horizontal || it==control.vertical }.forEach { action -> HeldButton(action){value -> vm.inputTouch(action,value)} }
        }
    }
    DisposableEffect(Unit){onDispose { vm.releaseInput() }}
}
@Composable private fun Joystick(move: (Vec2)->Unit) {
    var stick by remember { mutableStateOf(Offset.Zero) }
    val currentMove by rememberUpdatedState(move)
    Canvas(Modifier.size(132.dp).semantics { contentDescription="Virtual joystick. Drag to move; keyboard and gamepad mappings are also supported." }.pointerInput(Unit) {
        fun update(position: Offset) { val center=Offset(size.width/2f,size.height/2f);val delta=(position-center)/(size.width*.4f);val length=delta.getDistance();stick=if(length>1f)delta/length else delta;currentMove(Vec2(stick.x,-stick.y)) }
        detectDragGestures(onDragStart={update(it)},onDragEnd={stick=Offset.Zero;currentMove(Vec2())},onDragCancel={stick=Offset.Zero;currentMove(Vec2())},onDrag={change,_->change.consume();update(change.position)})
    }) {
        drawCircle(Color(0x99445566),size.minDimension*.47f)
        drawCircle(Color(0xcc88bbff),size.minDimension*.19f,center+stick*(size.minDimension*.3f))
    }
}
@Composable private fun HeldButton(label: String,change: (Float)->Unit) {
    var pressed by remember { mutableStateOf(false) };val scope=rememberCoroutineScope();val currentChange by rememberUpdatedState(change)
    Surface(color=if(pressed)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,shape=MaterialTheme.shapes.large,modifier=Modifier.sizeIn(minWidth=76.dp,minHeight=64.dp).semantics {
        role=Role.Button;contentDescription=label
        onClick { scope.launch { currentChange(1f);delay(80);currentChange(0f) };true }
    }.pointerInput(label) {
        awaitEachGesture {
            awaitFirstDown().consume();pressed=true;currentChange(1f)
            try { waitForUpOrCancellation() } finally { pressed=false;currentChange(0f) }
        }
    }) { Box(contentAlignment=Alignment.Center,modifier=Modifier.padding(12.dp)){Text(label)} }
    DisposableEffect(label){onDispose { currentChange(0f) }}
}
@Composable internal fun PlayStatus(s: EditorState,vm: EditorViewModel) {
    var preset by remember { mutableStateOf(ShakePreset.SMALL) }
    Column(Modifier.fillMaxWidth().padding(horizontal=8.dp)) {
        val frame=s.preview
        Text("${if(s.paused)"PAUSED" else "PLAY"} · step ${frame?.steps ?: 0} · bodies ${frame?.bodies ?: 0} · joints ${frame?.joints ?: 0} · dropped ${"%.3f".format(frame?.droppedSeconds ?: 0f)} s",style=MaterialTheme.typography.labelSmall)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            RuntimeChoice("Shake",preset.name,ShakePreset.entries.map { it.name }){preset=ShakePreset.valueOf(it)}
            TextButton(enabled=!s.paused,onClick={vm.shakeCamera(preset)}){Text("Shake camera")}
            TextButton(enabled=!s.paused,onClick={vm.focusCamera(null)}){Text("All cameras")}
            s.scene.nodes.filter { it.component<CameraComponent>()?.enabled==true }.forEach { camera -> TextButton(enabled=!s.paused,onClick={vm.focusCamera(camera.id)}){Text(camera.name)} }
        }
        Text(frame?.input?.entries?.joinToString(" · ") { "${it.key}=${"%.1f".format(it.value)}" } ?: "",style=MaterialTheme.typography.labelSmall)
    }
}
