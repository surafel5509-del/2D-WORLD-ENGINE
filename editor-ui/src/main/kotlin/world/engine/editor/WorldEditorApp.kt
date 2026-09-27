package world.engine.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import world.engine.physics.ColliderGeometry
import world.engine.animationeditor.TimelineEditor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import world.engine.core.*
import world.engine.io.Project
import world.engine.math.Transform
import world.engine.math.Vec2
import world.engine.viewport.WorldViewport
import world.engine.asseteditor.AssetBrowser

@Composable fun WorldEditorApp(vm: EditorViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var wizard by remember { mutableStateOf(false) }
    var exit by remember { mutableStateOf(false) }
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer=LifecycleEventObserver { _,event -> when(event) { Lifecycle.Event.ON_STOP -> vm.backgroundSave();Lifecycle.Event.ON_PAUSE -> vm.editorForeground(false);Lifecycle.Event.ON_RESUME -> vm.editorForeground(true);else -> Unit } }
        owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
    }
    BackHandler(s.project!=null && !s.busy) { if(s.playId!=null)vm.stopPlay() else if(s.drawCollider)vm.cancelColliderDraw() else if(s.recovery==null && s.recoveryIssue==null)exit=true }
    MaterialTheme(colorScheme=darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                if(s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(s.project==null) Dashboard(s.projects,s.busy,{wizard=true},vm::open,vm::refresh)
                else EditorShell(s,vm) { exit=true }
            }
        }
        if(wizard && s.project==null) ProjectWizard(s.busy,{wizard=false}) { name,pkg,orientation,template -> vm.create(name,pkg,orientation,template) }
        LaunchedEffect(s.project) { if(s.project!=null) wizard=false }
        s.error?.let { message -> AlertDialog(onDismissRequest=vm::dismissError,title={Text("Action needs attention")},text={Text(message)},confirmButton={TextButton(onClick=vm::dismissError){Text("OK")}}) }
        if(s.recovery!=null || s.recoveryIssue!=null) AlertDialog(onDismissRequest={},title={Text("Recover unsaved scene?")},text={Text(s.recoveryIssue ?: "A recovery snapshot differs from the saved scene. Restore it, or keep the last explicit save. Restoring does not overwrite your saved file until you save.")},confirmButton={TextButton(enabled=!s.busy && s.recovery!=null,onClick={vm.recover(true)}){Text("Restore")}},dismissButton={TextButton(enabled=!s.busy,onClick={vm.recover(false)}){Text("Keep saved")}})
        s.removeRuntimeType?.let { type -> AlertDialog(onDismissRequest=vm::cancelRuntimeRemoval,title={Text("Remove $type?")},text={Text("Removing a body also removes its connected joints. This scene edit can be undone.")},confirmButton={TextButton(onClick=vm::confirmRuntimeRemoval){Text("Remove")}},dismissButton={TextButton(onClick=vm::cancelRuntimeRemoval){Text("Cancel")}}) }
        if(exit) AlertDialog(onDismissRequest={exit=false},title={Text("Return to projects?")},text={Text(if(s.dirty) "Your scene has unsaved changes." else "The scene is saved.")},confirmButton={TextButton(enabled=!s.busy,onClick={exit=false;vm.close(false)}){Text("Save & close")}},dismissButton={Row { TextButton(onClick={exit=false}){Text("Cancel")}; TextButton(enabled=!s.busy,onClick={exit=false;vm.close(true)}){Text("Discard & close")} }})
    }
}

@Composable private fun Dashboard(projects: List<Project>, busy: Boolean, create: ()->Unit, open: (Project)->Unit, refresh: ()->Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("2D WORLD",style=MaterialTheme.typography.headlineMedium)
        Text("Projects · Asset & scene editor",style=MaterialTheme.typography.bodyMedium)
        Row { Button(enabled=!busy,onClick=create){Text("New project")}; TextButton(enabled=!busy,onClick=refresh){Text("Refresh")} }
        if(projects.isEmpty()) Text("Create your first project. Your scenes and images stay on this device.",Modifier.padding(vertical=24.dp))
        LazyVerticalGrid(columns=GridCells.Adaptive(220.dp),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            items(projects,key={it.manifest.id}) { project ->
                OutlinedCard(onClick={open(project)},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) { Text(project.manifest.name,style=MaterialTheme.typography.titleLarge); Text(project.manifest.packageId); Text(project.manifest.orientation); Text("Open scene →",Modifier.padding(top=12.dp)) }
                }
            }
        }
    }
}

@Composable private fun ProjectWizard(busy: Boolean, dismiss: ()->Unit, create: (String,String,String,String)->Unit) {
    var name by remember { mutableStateOf("MyGame") }
    var pkg by remember { mutableStateOf("com.example.mygame") }
    var orientation by remember { mutableStateOf("Landscape") }
    var template by remember { mutableStateOf("Empty") }
    AlertDialog(onDismissRequest={if(!busy)dismiss()},title={Text("Create project")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(name,{name=it},label={Text("Project name")},singleLine=true)
        OutlinedTextField(pkg,{pkg=it},label={Text("Package ID")},singleLine=true)
        Text("Game orientation",Modifier.padding(top=12.dp))
        Row { listOf("Portrait","Landscape").forEach { value -> FilterChip(selected=orientation==value,onClick={orientation=value},label={Text(value)}) } }
        Text("Template")
        Row { listOf("Empty","Three sprites").forEach { value -> FilterChip(selected=template==value,onClick={template=value},label={Text(value)}) } }
        Text("Stored privately on this device. No internet or storage permission needed.")
    }},confirmButton={TextButton(enabled=!busy,onClick={create(name,pkg,orientation,template)}){Text("Create")}},dismissButton={TextButton(enabled=!busy,onClick=dismiss){Text("Cancel")}})
}

@Composable private fun EditorShell(s: EditorState,vm: EditorViewModel,exit: ()->Unit) {
    val project=s.project ?: return
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    val viewport=remember(project.manifest.id){WorldViewport(context)}
    var delete by remember { mutableStateOf(false) };var panel by remember { mutableStateOf("Hierarchy") }
    var reset by remember { mutableIntStateOf(0) };var systems by remember { mutableStateOf(false) };var debug by remember { mutableStateOf(false) }
    val playing=s.playId!=null;val editable=!s.busy && !playing && !s.drawCollider
    val displayed=s.preview?.scene ?: s.scrubScene ?: s.scene
    val paths=remember(s.assets){s.assets.filter { it.kind==world.engine.assets.AssetKind.IMAGE }.associate { it.id to it.path }}
    val images=remember(s.assets){s.assets.filter { it.kind==world.engine.assets.AssetKind.IMAGE }.associate { it.id to it.name }}
    val lines=remember(displayed,debug,s.preview,s.colliderPoints,s.drawCollider,s.selected) {
        val result=if(debug) (s.preview?.lines ?: ColliderGeometry.outlines(displayed)).toMutableList() else mutableListOf()
        if(s.drawCollider && s.selected!=null) {
            val matrix=s.scene.worldMatrices().getValue(s.selected)
            val points=s.colliderPoints.map(matrix::map)
            points.forEach { point -> result.add(DebugLine(point-Vec2(4f,0f),point+Vec2(4f,0f),world.engine.math.Color(1f,1f,0f)));result.add(DebugLine(point-Vec2(0f,4f),point+Vec2(0f,4f),world.engine.math.Color(1f,1f,0f))) }
            points.zipWithNext().forEach { (a,b)->result.add(DebugLine(a,b,world.engine.math.Color(1f,1f,0f))) }
        }
        result.toList()
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(vm::importSprite)}
    DisposableEffect(viewport,owner) {
        val observer=LifecycleEventObserver { _,event -> when(event){Lifecycle.Event.ON_RESUME->viewport.onResume();Lifecycle.Event.ON_PAUSE->viewport.onPause();else->Unit} }
        owner.lifecycle.addObserver(observer);if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))viewport.onResume()
        onDispose { owner.lifecycle.removeObserver(observer);viewport.onPause();vm.releaseInput() }
    }
    LaunchedEffect(reset){viewport.resetCamera()}
    Column(Modifier.fillMaxSize()) {
        Text(project.manifest.name+if(playing)" • nondestructive play" else if(s.dirty)" • unsaved" else " • saved",Modifier.padding(horizontal=12.dp,vertical=4.dp),style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            if(!playing)TextButton(enabled=editable,onClick=vm::play){Text("Play")}
            else {
                TextButton(onClick={if(s.paused)vm.resumePlay() else vm.pausePlay()}){Text(if(s.paused)"Resume" else "Pause")}
                TextButton(enabled=s.paused,onClick=vm::stepPlay){Text("Step")};TextButton(onClick=vm::stopPlay){Text("Stop")}
            }
            TextButton(enabled=editable,onClick=exit){Text("Projects")};TextButton(enabled=editable,onClick=vm::save){Text("Save")}
            TextButton(enabled=editable && s.canUndo,onClick=vm::undo){Text("Undo")};TextButton(enabled=editable && s.canRedo,onClick=vm::redo){Text("Redo")}
            TextButton(enabled=editable,onClick=vm::addSprite){Text("Add sprite")};TextButton(enabled=editable,onClick={picker.launch(arrayOf("image/*"))}){Text("Import image")}
            TextButton(enabled=editable,onClick={systems=true}){Text("Systems")};TextButton(enabled=editable,onClick={panel="Animation"}){Text("Animation")}
            TextButton(enabled=editable,onClick={panel="Assets"}){Text("Assets")};TextButton(onClick={debug=!debug}){Text(if(debug)"Colliders on" else "Colliders off")}
            TextButton(enabled=editable,onClick={reset++}){Text("Reset view")}
        }
        if(s.drawCollider)Row(Modifier.horizontalScroll(rememberScrollState())) {
            Text("Polygon ${s.colliderPoints.size}/8",Modifier.padding(12.dp))
            TextButton(enabled=s.colliderPoints.size>=3,onClick=vm::applyColliderDraw){Text("Apply polygon")}
            TextButton(onClick=vm::clearColliderPoints){Text("Clear points")};TextButton(onClick=vm::cancelColliderDraw){Text("Cancel drawing")}
        }
        if(playing)PlayStatus(s,vm)
        BoxWithConstraints(Modifier.weight(1f)) {
            val wide=maxWidth>=840.dp
            Column {
                Row(Modifier.weight(1f)) {
                    if(wide && !playing)Box(Modifier.width(190.dp).fillMaxHeight()){Hierarchy(s,vm)}
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        AndroidView(factory={viewport},modifier=Modifier.fillMaxSize(),update={view ->
                            view.onSelect=vm::select;view.onMove=vm::move;view.onError=vm::report;view.onAssetDrop=vm::placeAsset
                            view.onColliderPoint=vm::addColliderPoint;view.onViewportSize=vm::viewportResize;view.onPhysicalInput=vm::inputPhysical;view.onReleaseInput=vm::releaseInput
                            view.onContext={id -> vm.select(id);if(id!=null){panel="Inspector";delete=true}}
                            view.isEnabled=!s.busy && s.recovery==null && s.recoveryIssue==null
                            view.update(displayed,s.selected,project.directory,paths,playing,s.drawCollider,s.preview?.views.orEmpty(),lines)
                        })
                        if(playing && !s.paused)PlayInputOverlay(s.scene,s.selected,vm,Modifier.align(Alignment.BottomCenter).fillMaxWidth())
                        if(playing && s.paused)Text("Paused · Step = 1/60 second",Modifier.align(Alignment.BottomCenter).background(MaterialTheme.colorScheme.surface).padding(12.dp))
                    }
                    if(wide && !playing)Box(Modifier.width(250.dp).fillMaxHeight()){Inspector(s,vm){delete=true}}
                }
                if(!playing) {
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        (if(wide)listOf("Assets","Animation","Console","Viewport") else listOf("Hierarchy","Inspector","Assets","Animation","Console","Viewport")).forEach { label -> TextButton(enabled=!s.drawCollider,onClick={panel=label}){Text(if(panel==label)"[$label]" else label)} }
                    }
                    if(panel!="Viewport" && !s.drawCollider)Box(Modifier.fillMaxWidth().height((maxHeight*.5f).coerceAtMost(360.dp))) {
                        when(panel) {
                            "Hierarchy" -> if(wide)Console(s,Modifier.fillMaxSize()) else Hierarchy(s,vm)
                            "Inspector" -> if(wide)Console(s,Modifier.fillMaxSize()) else Inspector(s,vm){delete=true}
                            "Assets" -> AssetBrowser(s.assets,s.selectedAsset,s.frames,project.directory,s.busy,s.assetProgress,vm,Modifier.fillMaxSize())
                            "Animation" -> TimelineEditor(s.scene,s.selected,s.selectedClip,s.animationFrame,images,vm,Modifier.fillMaxSize())
                            else -> Console(s,Modifier.fillMaxSize())
                        }
                    }
                } else Console(s,Modifier.fillMaxWidth().height(64.dp))
            }
        }
        if(!playing)Text(if(s.drawCollider)"Tap 3–8 points. Apply creates a convex collider; concavities are filled." else "Drag: move/pan · Pinch: zoom · Hold: actions",Modifier.padding(6.dp),style=MaterialTheme.typography.labelSmall)
    }
    if(systems && !playing)Dialog(onDismissRequest={if(!s.busy)systems=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            SystemsPanel(s,vm,{systems=false},{vm.beginColliderDraw();systems=false})
        }
    }
    if(delete && s.selected!=null)AlertDialog(onDismissRequest={delete=false},title={Text("Delete entity and children?")},text={Text("Connected joints and camera follow references are removed. This can be undone. Asset files are kept.")},confirmButton={TextButton(enabled=editable,onClick={delete=false;vm.deleteSelected()}){Text("Delete")}},dismissButton={TextButton(onClick={delete=false}){Text("Cancel")}})
}

@Composable private fun SystemsPanel(s: EditorState,vm: EditorViewModel,close: ()->Unit,draw: ()->Unit) {
    var tab by remember { mutableStateOf("Actor") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(enabled=!s.busy,onClick=close){Text("Close systems")}
            listOf("Actor","Joints","Input","Scene").forEach { name -> TextButton(onClick={tab=name}){Text(if(tab==name)"[$name]" else name)} }
        }
        if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        Box(Modifier.weight(1f)) {
            when(tab) {
                "Actor" -> RuntimeInspector(s.scene,s.scene.nodes.find { it.id==s.selected },vm,draw)
                "Joints" -> JointEditor(s.scene,vm)
                "Input" -> InputMapEditor(s.scene,vm)
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    Text("SCENE RUNTIME",style=MaterialTheme.typography.titleMedium)
                    key(s.scene.gravity) {
                        var x by remember { mutableStateOf(s.scene.gravity.x.toString()) };var y by remember { mutableStateOf(s.scene.gravity.y.toString()) }
                        RuntimeField("Gravity X pixels/sec²",x){x=it};RuntimeField("Gravity Y pixels/sec²",y){y=it}
                        TextButton(onClick={vm.parseRuntime { vm.editRuntimeScene(s.scene.copy(gravity=Vec2(x.toFloat(),y.toFloat()))) }}){Text("Apply gravity")}
                    }
                    TextButton(onClick={vm.addCamera();tab="Actor"}){Text("Create camera entity")}
                    TextButton(onClick={vm.editRuntimeScene(s.scene.copy(activeCamera=null))}){Text("Use all enabled cameras")}
                    Text("Test rig adds a player, ground, ball, capsule, pendulum, sensor and follow camera, and resets gravity/input to the defaults. It is one undoable scene edit.")
                    Button(onClick={vm.addPhysicsTestRig();close()}){Text("Add platformer test rig")}
                    Text("Play works on a disposable scene copy. Pause and single-step inspect simulation; Stop returns to the unchanged authored scene. Physics runs at 60 Hz with at most eight catch-up steps. Dynamic bodies own their pose: use a visual child for transform animation.")
                }
            }
        }
    }
}

@Composable private fun Hierarchy(s: EditorState, vm: EditorViewModel) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Text("HIERARCHY · ${s.scene.nodes.size}",style=MaterialTheme.typography.labelLarge)
        OutlinedTextField(query,{query=it},label={Text("Find entity")},singleLine=true,modifier=Modifier.fillMaxWidth())
        LazyColumn { items(s.scene.nodes.filter { it.name.contains(query,true) },key={it.id}) { node -> TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={vm.select(node.id)},modifier=Modifier.fillMaxWidth()){Text((if(node.parent!=null) "↳ " else "")+(if(node.id==s.selected) "● " else "○ ")+node.name)} } }
        if(s.scene.nodes.isEmpty()) Text("Use Add sprite or Import image.")
    }
}

@Composable private fun Inspector(s: EditorState, vm: EditorViewModel, delete: ()->Unit) {
    val node=s.scene.nodes.find { it.id==s.selected }
    if(node==null) { Text("Select a sprite to edit its properties.",Modifier.padding(12.dp)); return }
    var prefabAction by remember(node.id) { mutableStateOf<String?>(null) }
    key(node) {
        var name by remember { mutableStateOf(node.name) }
        var x by remember { mutableStateOf(node.transform.position.x.toString()) }
        var y by remember { mutableStateOf(node.transform.position.y.toString()) }
        var rotation by remember { mutableStateOf(node.transform.rotation.toString()) }
        var sx by remember { mutableStateOf(node.transform.scale.x.toString()) }
        var sy by remember { mutableStateOf(node.transform.scale.y.toString()) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
            Text("INSPECTOR",style=MaterialTheme.typography.labelLarge)
            Property("Name",name){name=it}; Property("X",x){x=it}; Property("Y",y){y=it}
            Property("Rotation (degrees)",rotation){rotation=it}; Property("Scale X",sx){sx=it}; Property("Scale Y",sy){sy=it}
            Button(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={
                val values=listOf(x,y,rotation,sx,sy).map { it.toFloatOrNull() }
                if(name.isBlank() || values.any { it==null || !it.isFinite() } || kotlin.math.abs(values[3] ?: 0f)<.001f || kotlin.math.abs(values[4] ?: 0f)<.001f) vm.report("Enter a name, finite numeric properties, and non-zero scales.")
                else {
                    val t=Transform(Vec2(values[0]!!,values[1]!!),values[2]!!,Vec2(values[3]!!,values[4]!!))
                    vm.updateNode(node.copy(name=name,components=node.components.map { if(it is TransformComponent) TransformComponent(t) else it }))
                }
            }){Text("Apply properties")}
            Text("ID: ${node.id}",style=MaterialTheme.typography.labelSmall)
            Text(node.sprite?.assetId ?: node.sprite?.asset ?: if(node.sprite==null) "Hierarchy group" else "Built-in solid sprite",style=MaterialTheme.typography.labelSmall)
            TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick=vm::createPrefabFromSelection){Text("Create prefab")}
            node.prefab?.let { binding ->
                Text("Prefab overrides: ${binding.overrides.joinToString().ifEmpty { "none" }}",style=MaterialTheme.typography.labelSmall)
                TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={prefabAction="Refresh instance"}){Text("Refresh instance")}
                TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={prefabAction="Revert overrides"}){Text("Revert overrides")}
                TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={prefabAction="Update prefab source"}){Text("Update prefab source")}
            }
            TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick=delete){Text("Delete entity")}
        }
    }
    prefabAction?.let { action -> AlertDialog(onDismissRequest={prefabAction=null},title={Text("$action?")},text={Text(when(action) { "Revert overrides" -> "Reset this instance from its source, including its children. This scene edit can be undone."; "Refresh instance" -> "Rebuild this subtree from its prefab source while preserving property overrides. Locally added or removed children are not structural overrides and may be discarded or restored. This scene edit can be undone."; else -> "Replace the prefab source with this instance. Other instances receive changes when refreshed; source updates are not scene undo commands." })},confirmButton={TextButton(enabled=!s.busy && s.playId==null && !s.drawCollider,onClick={prefabAction=null;when(action) { "Revert overrides" -> vm.refreshPrefab(false); "Refresh instance" -> vm.refreshPrefab(true); else -> vm.updatePrefabSource() }}){Text("Confirm")}},dismissButton={TextButton(onClick={prefabAction=null}){Text("Cancel")}}) }
}
@Composable private fun Property(label: String,value: String,set: (String)->Unit) { OutlinedTextField(value,set,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth()) }
@Composable private fun Console(s: EditorState, modifier: Modifier) {
    Column(modifier.padding(8.dp)) { Text("CONSOLE",style=MaterialTheme.typography.labelLarge); LazyColumn { items(s.log.asReversed()) { Text(it,style=MaterialTheme.typography.bodySmall) } } }
}
