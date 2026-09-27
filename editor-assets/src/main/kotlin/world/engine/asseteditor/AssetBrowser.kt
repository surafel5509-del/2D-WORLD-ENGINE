package world.engine.asseteditor

import android.content.ClipData
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import world.engine.assets.*
import java.io.File

@Composable fun AssetBrowser(assets: List<AssetRecord>,selected: String?,frames: List<SpriteFrame>,root: File,busy: Boolean,progress: String,actions: AssetActions,modifier: Modifier=Modifier,allowExpand: Boolean=true) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var favorite by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(true) }
    var kind by remember { mutableIntStateOf(0) }
    var inspect by remember { mutableStateOf(false) }
    var generate by remember { mutableStateOf(false) }
    val chosen=assets.find { it.id==selected }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(actions::importAsset) }
    val filtered=assets.filter { a -> (!favorite || a.favorite) && (kind==0 || (kind==1 && a.kind==AssetKind.IMAGE) || (kind==2 && a.kind==AssetKind.PREFAB)) && (a.name+" "+a.folder+" "+a.tags.joinToString(" ")).contains(query,true) }
    Column(modifier.padding(8.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            if(allowExpand)TextButton(onClick={expanded=true}) { Text("Expand") }
            TextButton(enabled=!busy,onClick={importer.launch(arrayOf("image/*"))}) { Text("Import") }
            TextButton(enabled=!busy,onClick={generate=true}) { Text("Generate 335") }
            TextButton(enabled=chosen!=null && !busy,onClick={inspect=true}) { Text("Inspect / edit") }
            TextButton(enabled=chosen!=null && !busy,onClick={chosen?.let { actions.useAsset(it.id) }}) { Text("Use selected") }
            TextButton(onClick={grid=!grid}) { Text(if(grid) "List view" else "Grid view") }
            TextButton(onClick={favorite=!favorite}) { Text(if(favorite) "★ Favorites" else "Favorites off") }
            TextButton(onClick={kind=(kind+1)%3}) { Text(listOf("All types","Images","Prefabs")[kind]) }
        }
        OutlinedTextField(query,{query=it},singleLine=true,label={Text("Search names, folders, tags (${filtered.size})")},modifier=Modifier.fillMaxWidth())
        if(progress.isNotEmpty())Text(progress,style=MaterialTheme.typography.labelSmall)
        if(filtered.isEmpty())Text("No matching assets. Import a local image or generate the procedural library.")
        else if(grid) LazyVerticalGrid(columns=GridCells.Adaptive(100.dp),modifier=Modifier.weight(1f)) {
            items(filtered,key={it.id}) { a -> AssetTile(a,root,a.id==selected,busy,actions) }
        } else LazyColumn(Modifier.weight(1f)) { items(filtered,key={it.id}) { a -> AssetTile(a,root,a.id==selected,busy,actions,true) } }
    }
    if(expanded)Dialog(onDismissRequest={if(!busy)expanded=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column { TextButton(enabled=!busy,onClick={expanded=false}){Text("Return to scene")}; AssetBrowser(assets,selected,frames,root,busy,progress,actions,Modifier.weight(1f),false) }
        }
    }
    if(inspect && chosen!=null) Dialog(onDismissRequest={if(!busy)inspect=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            AssetInspector(chosen,frames,root,busy,actions){inspect=false}
        }
    }
    if(generate) AlertDialog(onDismissRequest={generate=false},title={Text("Generate original procedural assets?")},text={Text("Creates 335 small PNG images locally: 30 characters, 30 enemies, 20 NPCs, 20 animals, 25 vehicles, 40 buildings, 30 nature items, 40 props and 100 tileable textures. Re-running skips existing recipes. These are geometric pixel-art variants, not hand-drawn game packs.")},confirmButton={TextButton(onClick={generate=false;actions.generateLibrary()}){Text("Generate")}},dismissButton={TextButton(onClick={generate=false}){Text("Cancel")}})
}

@Composable private fun AssetTile(a: AssetRecord,root: File,selected: Boolean,busy: Boolean,actions: AssetActions,list: Boolean=false) {
    val view=LocalView.current
    val drag=Modifier.pointerInput(a.id,busy) {
        if(!busy) detectDragGesturesAfterLongPress(onDragStart={
            actions.selectAsset(a.id)
            val shadow=object: View.DragShadowBuilder() {
                override fun onProvideShadowMetrics(size: Point,touch: Point) { size.set(96,96); touch.set(48,48) }
                override fun onDrawShadow(canvas: Canvas) { canvas.drawRect(0f,0f,96f,96f,Paint().apply { color=0xff4488dd.toInt() }) }
            }
            view.startDragAndDrop(ClipData.newPlainText("2DWorldAsset",a.id),shadow,null,0)
        },onDrag={change,_->change.consume()})
    }
    OutlinedCard(onClick={actions.selectAsset(a.id)},enabled=!busy,modifier=Modifier.padding(3.dp).then(drag)) {
        if(list) Row(Modifier.padding(8.dp)) { Preview(a,root,Modifier.size(40.dp)); Text((if(selected) "● " else "")+a.name+" · "+a.folder,Modifier.padding(start=8.dp)) }
        else Column(Modifier.padding(6.dp)) { Preview(a,root,Modifier.fillMaxWidth().height(52.dp)); Text((if(selected) "● " else "")+(if(a.favorite) "★ " else "")+a.name,maxLines=2,style=MaterialTheme.typography.labelSmall) }
    }
}
@Composable internal fun Preview(a: AssetRecord,root: File,modifier: Modifier) {
    if(a.kind==AssetKind.IMAGE) AsyncImage(model=File(root,a.path),contentDescription=a.name,contentScale=ContentScale.Fit,modifier=modifier)
    else Box(modifier) { Text("▦ Prefab",Modifier.padding(8.dp)) }
}
