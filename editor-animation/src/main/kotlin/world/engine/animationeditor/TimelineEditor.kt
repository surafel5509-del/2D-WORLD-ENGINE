package world.engine.animationeditor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import world.engine.core.*

interface AnimationActions {
    fun createClip()
    fun addAnimationPresets()
    fun selectClip(id: String)
    fun updateClip(clip: AnimationClip)
    fun assignClip(id: String?)
    fun scrubAnimation(frame: Float)
    fun clearScrub()
    fun animationError(message: String)
}
/** An editable frame timeline: real keys, interpolation, events, frame scrubbing and actor binding. */
@Composable fun TimelineEditor(scene: Scene,selected: String?,clipId: String?,frame: Float,images: Map<String,String>,actions: AnimationActions,modifier: Modifier=Modifier) {
    val clip=scene.clips.find { it.id==clipId }
    val node=scene.nodes.find { it.id==selected }
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("ANIMATION",style=MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(onClick=actions::createClip){Text("New clip")}
            TextButton(onClick=actions::addAnimationPresets){Text("Add 65 presets")}
            TextButton(onClick=actions::clearScrub){Text("Exit scrub")}
        }
        Picker("Clip",clip?.name ?: "Select",scene.clips.associate { it.id to it.name },actions::selectClip)
        if(clip==null) { Text("Create a clip or add the transform/color preset library. Select a scene entity to bind it.");return@Column }
        key(clip.id,clip.name,clip.fps,clip.frames) {
            var name by remember { mutableStateOf(clip.name) };var fps by remember { mutableStateOf(clip.fps.toString()) };var frames by remember { mutableStateOf(clip.frames.toString()) }
            Field("Name",name){name=it};Field("FPS 1–120",fps){fps=it};Field("Frame count",frames){frames=it}
            TextButton(onClick={guard(actions){actions.updateClip(clip.copy(name=name,fps=fps.toInt(),frames=frames.toInt()))}}){Text("Apply clip settings")}
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(onClick={actions.updateClip(clip.copy(loop=LoopMode.entries[(clip.loop.ordinal+1)%LoopMode.entries.size]))}){Text("Mode: ${clip.loop}")}
            TextButton(onClick={actions.updateClip(clip.copy(relative=!clip.relative))}){Text(if(clip.relative)"Relative transforms" else "Absolute transforms")}
        }
        Text("Frame ${frame.toInt()} / ${clip.frames-1}")
        Slider(value=frame.coerceIn(0f,(clip.frames-1).toFloat()),onValueChange=actions::scrubAnimation,valueRange=0f..maxOf(1,clip.frames-1).toFloat())
        var exact by remember(frame.toInt()) { mutableStateOf(frame.toInt().toString()) }
        Field("Exact frame",exact){exact=it}
        TextButton(onClick={guard(actions){actions.scrubAnimation(exact.toInt().toFloat())}}){Text("Seek")}
        Canvas(Modifier.fillMaxWidth().height(84.dp).background(Color(0xff121921))) {
            val denominator=maxOf(1,clip.frames-1).toFloat()
            clip.tracks.forEachIndexed { row,track -> track.keys.forEach { key -> drawCircle(Color.Cyan,4f,Offset(key.frame/denominator*size.width,(row+1)*size.height/6)) } }
            clip.events.forEach { drawCircle(Color.Yellow,4f,Offset(it.frame/denominator*size.width,size.height-5)) }
            drawLine(Color.White,Offset(frame/denominator*size.width,0f),Offset(frame/denominator*size.width,size.height),2f)
        }
        if(node!=null) {
            Text("Actor: ${node.name}")
            Row { TextButton(onClick={actions.assignClip(clip.id)}){Text("Assign to actor")};TextButton(enabled=node.component<AnimatorComponent>()!=null,onClick={actions.assignClip(null)}){Text("Detach animator")} }
        } else Text("Select a hierarchy entity to assign or scrub this clip.")
        KeyEditor(clip,frame.toInt(),images,actions)
        clip.tracks.forEach { track ->
            Text("${track.property} · ${track.keys.size} keys")
            if(track.property!=TrackProperty.SPRITE)TextButton(onClick={actions.updateClip(clip.copy(tracks=clip.tracks.map { if(it==track)it.copy(interpolation=if(it.interpolation==Interpolation.LINEAR)Interpolation.STEP else Interpolation.LINEAR) else it }))}){Text("Interpolation: ${track.interpolation}")}
            track.keys.sortedBy { it.frame }.forEach { key -> Row {
                TextButton(onClick={actions.scrubAnimation(key.frame.toFloat())}){Text("${key.frame}: ${key.assetId?.let { images[it] } ?: key.values.joinToString()}")}
                TextButton(onClick={actions.updateClip(clip.copy(tracks=clip.tracks.mapNotNull { t -> if(t!=track)t else t.copy(keys=t.keys-key).takeIf { it.keys.isNotEmpty() } }))}){Text("Remove")}
            } }
        }
        EventEditor(clip,frame.toInt(),images,actions)
        clip.events.forEach { e -> Row { Text("${e.frame}: ${e.name} (${e.kind})",Modifier.weight(1f));TextButton(onClick={actions.updateClip(clip.copy(events=clip.events-e))}){Text("Remove")} } }
        Text("Yellow markers are events. Trigger events are delivered to the preview event stream. Sprite events change the real asset reference. Sound, particle and script consumers belong to their later engine phases.",style=MaterialTheme.typography.bodySmall)
    }
}
@Composable private fun KeyEditor(clip: AnimationClip,frame: Int,images: Map<String,String>,actions: AnimationActions) {
    var property by remember { mutableStateOf(TrackProperty.POSITION) }
    var values by remember(property) { mutableStateOf(when(property){TrackProperty.ROTATION->"0";TrackProperty.SCALE->"1,1";TrackProperty.COLOR->"1,1,1,1";else->"0,0"}) }
    var asset by remember { mutableStateOf<String?>(null) }
    Picker("Property",property.name,TrackProperty.entries.associate { it.name to it.name }){property=TrackProperty.valueOf(it)}
    if(property==TrackProperty.SPRITE)Picker("Image",images[asset] ?: "Select",images){asset=it} else Field("Values (comma-separated, RGBA for color)",values){values=it}
    Button(onClick={guard(actions){
        val key=if(property==TrackProperty.SPRITE)AnimationKey(frame,assetId=asset) else AnimationKey(frame,values.split(',').map { it.trim().toFloat() })
        val old=clip.tracks.find { it.property==property }
        val track=(old ?: AnimationTrack(property,emptyList())).copy(keys=(old?.keys.orEmpty().filterNot { it.frame==frame }+key).sortedBy { it.frame })
        actions.updateClip(clip.copy(tracks=clip.tracks.filterNot { it.property==property }+track))
    }}){Text("Set key at frame $frame")}
}
@Composable private fun EventEditor(clip: AnimationClip,frame: Int,images: Map<String,String>,actions: AnimationActions) {
    var name by remember { mutableStateOf("trigger") };var kind by remember { mutableStateOf(AnimationEventKind.TRIGGER) };var asset by remember { mutableStateOf<String?>(null) }
    Text("Frame events",style=MaterialTheme.typography.titleSmall)
    Field("Event name",name){name=it}
    Picker("Kind",kind.name,AnimationEventKind.entries.associate { it.name to it.name }){kind=AnimationEventKind.valueOf(it)}
    if(kind==AnimationEventKind.SET_SPRITE)Picker("Image",images[asset] ?: "Select",images){asset=it}
    TextButton(onClick={actions.updateClip(clip.copy(events=clip.events+AnimationEvent(frame,name,kind,if(kind==AnimationEventKind.SET_SPRITE)asset else null)))}){Text("Add event at frame $frame")}
}
@Composable private fun Field(label: String,value: String,change: (String)->Unit) { OutlinedTextField(value,change,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth()) }
@Composable private fun Picker(label: String,value: String,options: Map<String,String>,select: (String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { TextButton(onClick={expanded=true}){Text("$label: $value")};DropdownMenu(expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=320.dp)){ options.forEach { (id,name)->DropdownMenuItem(text={Text(name)},onClick={expanded=false;select(id)}) } } }
}
private fun guard(actions: AnimationActions,block: ()->Unit) { try { block() } catch(e: IllegalArgumentException) { actions.animationError(e.message ?: "Invalid animation value") } }
