package world.engine.asseteditor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import world.engine.assets.*
import java.io.File
import kotlin.math.*

@Composable internal fun AssetInspector(a: AssetRecord,frames: List<SpriteFrame>,root: File,busy: Boolean,actions: AssetActions,close: ()->Unit) {
    var tab by remember(a.id) { mutableStateOf("Details") }
    var name by remember(a) { mutableStateOf(a.name) }
    var folder by remember(a) { mutableStateOf(a.folder) }
    var tags by remember(a) { mutableStateOf(a.tags.joinToString(", ")) }
    var favorite by remember(a) { mutableStateOf(a.favorite) }
    var delete by remember { mutableStateOf(false) }
    var replace by remember { mutableStateOf(false) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { actions.replaceAsset(a.id,it) } }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri -> uri?.let { actions.exportAsset(a.id,it) } }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(enabled=!busy,onClick=close){Text("Close")}
            TextButton(enabled=!busy,onClick={actions.useAsset(a.id);close()}){Text("Use in scene")}
            TextButton(enabled=!busy,onClick={actions.duplicateAsset(a.id)}){Text("Duplicate")}
            if(a.kind==AssetKind.IMAGE)TextButton(enabled=!busy,onClick={export.launch("${a.name}.png")}){Text("Export PNG")}
        }
        Text(a.name,style=MaterialTheme.typography.titleLarge)
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        Row { (if(a.kind==AssetKind.IMAGE)listOf("Details","Texture","Sheet / split") else listOf("Details")).forEach { value -> TextButton(onClick={tab=value}){Text(if(tab==value)"[$value]" else value)} } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if(a.kind==AssetKind.IMAGE) RegionPreview(a,root,if(tab=="Sheet / split")frames else emptyList(),tab=="Sheet / split" && !busy,actions::appendFrame)
            Text("${a.kind} · ${a.width}×${a.height} · revision ${a.revision}",style=MaterialTheme.typography.labelMedium)
            when(tab) {
                "Details" -> {
                    Input("Name",name){name=it}; Input("Catalog folder (move)",folder){folder=it}; Input("Tags, comma separated",tags){tags=it}
                    Row { Checkbox(favorite,{favorite=it}); Text("Favorite",Modifier.padding(top=12.dp)) }
                    Button(enabled=!busy,onClick={actions.metadata(a.id,name,folder,tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },favorite)}){Text("Save metadata")}
                    Text("UUID: ${a.id}",style=MaterialTheme.typography.labelSmall)
                    Text("${a.path}\nCatalog moves and renames never change the UUID.",style=MaterialTheme.typography.labelSmall)
                    if(a.kind==AssetKind.IMAGE)TextButton(enabled=!busy,onClick={replace=true}){Text("Replace image, keep references")}
                    if(a.kind==AssetKind.PREFAB)Text("Use creates an editable hierarchy. Edit its parts in the scene; Update prefab saves the source. Refresh instance applies source changes while retaining property overrides.")
                    TextButton(enabled=!busy,onClick={actions.prefabFromAsset(a.id)}){Text("Create prefab")}
                    TextButton(enabled=!busy,onClick={delete=true}){Text("Delete from catalog")}
                }
                "Texture" -> TextureControls(a,busy,actions)
                else -> SheetControls(a,frames,busy,actions)
            }
        }
    }
    if(delete)AlertDialog(onDismissRequest={delete=false},title={Text("Delete ${a.name}?")},text={Text("Referenced assets are protected, including saved scenes, recovery, undo history and prefab dependencies. Unreferenced payload revisions remain on disk for safety.")},confirmButton={TextButton(enabled=!busy,onClick={delete=false;actions.deleteAsset(a.id)}){Text("Delete")}},dismissButton={TextButton(onClick={delete=false}){Text("Cancel")}})
    if(replace)AlertDialog(onDismissRequest={replace=false},title={Text("Replace image for every reference?")},text={Text("The UUID stays the same. All scenes and prefabs referencing it will use the new pixels. This changes the asset, not the scene undo history.")},confirmButton={TextButton(enabled=!busy,onClick={replace=false;picker.launch(arrayOf("image/*"))}){Text("Choose image")}},dismissButton={TextButton(onClick={replace=false}){Text("Cancel")}})
}

@Composable private fun TextureControls(a: AssetRecord,busy: Boolean,actions: AssetActions) {
    var x by remember(a.id){mutableStateOf("0")}; var y by remember(a.id){mutableStateOf("0")}
    var width by remember(a){mutableStateOf(a.width.toString())}; var height by remember(a){mutableStateOf(a.height.toString())}
    var color by remember { mutableStateOf("#FFFFFFFF") }
    Text("Operations create a new image; the original and its references are unchanged.")
    Input("Crop X",x){x=it}; Input("Crop Y",y){y=it}; Input("Width",width){width=it}; Input("Height",height){height=it}; Input("Color multiplier #AARRGGBB",color){color=it}
    listOf("Crop","Resize","Rotate 90°","Flip X","Flip Y","Tile 2×2","Colorize").forEach { operation ->
        TextButton(enabled=!busy,onClick={
            try {
                actions.editTexture(a.id,TextureEdit(operation,x.toInt(),y.toInt(),width.toInt(),height.toInt(),android.graphics.Color.parseColor(color)))
            } catch(e: IllegalArgumentException) { actions.assetError("Use integer dimensions and a valid #AARRGGBB color.") }
        }){Text(operation)}
    }
}

@Composable private fun SheetControls(a: AssetRecord,frames: List<SpriteFrame>,busy: Boolean,actions: AssetActions) {
    val recipe=a.recipe
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> uri?.let { actions.exportFrames(a.id,it) } }
    var width by remember(a.id){mutableStateOf("32")}; var height by remember(a.id){mutableStateOf("32")}
    Text("Draw rectangles on the preview for manual regions. Grid replaces the list. Alpha slicing detects connected opaque islands, not semantic object parts.")
    Input("Grid cell width",width){width=it}; Input("Grid cell height",height){height=it}
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        TextButton(enabled=!busy,onClick={try { actions.setFrames(SpriteSlicer.grid(a.width,a.height,width.toInt(),height.toInt())) } catch(e: IllegalArgumentException){actions.assetError(e.message ?: "Invalid grid")}}){Text("Grid slice")}
        TextButton(enabled=!busy,onClick={actions.autoSlice(a.id)}){Text("Auto alpha")}
        TextButton(enabled=!busy,onClick={actions.setFrames(listOf(SpriteFrame("Whole image",0,0,a.width,a.height)))}){Text("Full frame")}
        TextButton(enabled=!busy,onClick={actions.setFrames(emptyList())}){Text("Clear regions")}
    }
    Text("${frames.size} named regions · top-left pixel coordinates")
    frames.forEachIndexed { index,frame ->
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(frame.name,{value -> actions.setFrames(frames.toMutableList().also { it[index]=frame.copy(name=value) })},label={Text("${index+1}: ${frame.x},${frame.y} ${frame.width}×${frame.height}")},singleLine=true,modifier=Modifier.weight(1f),enabled=!busy)
            TextButton(enabled=!busy && index>0,onClick={actions.setFrames(frames.toMutableList().also { java.util.Collections.swap(it,index,index-1) })}){Text("↑")}
            TextButton(enabled=!busy && index<frames.lastIndex,onClick={actions.setFrames(frames.toMutableList().also { java.util.Collections.swap(it,index,index+1) })}){Text("↓")}
            TextButton(enabled=!busy,onClick={actions.setFrames(frames.filterIndexed { i,_->i!=index })}){Text("×")}
        }
    }
    Button(enabled=!busy,onClick={actions.saveSheet(a.id)}){Text("Save named layout")}
    Button(enabled=!busy && frames.isNotEmpty(),onClick={export.launch("${a.name}-frames.zip")}){Text("Export frames ZIP + manifest")}
    Button(enabled=!busy && frames.isNotEmpty(),onClick={actions.extractFrames(a.id)}){Text("Extract frames as PNG assets")}
    Button(enabled=!busy && (frames.isNotEmpty() || (recipe!=null && !recipe.startsWith("Texture:"))),onClick={actions.splitAsset(a.id)}){Text("Split into named prefab parts")}
    if(recipe!=null && !recipe.startsWith("Texture:"))Text("Generated composites use their original transparent layers, not rectangular crops. Regions are ignored for these assets.")
}
@Composable private fun Input(label: String,value: String,change: (String)->Unit) { OutlinedTextField(value,change,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth()) }

@Composable private fun RegionPreview(a: AssetRecord,root: File,frames: List<SpriteFrame>,editable: Boolean,append: (SpriteFrame)->Unit) {
    var area by remember { mutableStateOf(IntSize(1,1)) }
    var start by remember { mutableStateOf<Offset?>(null) }
    var end by remember { mutableStateOf<Offset?>(null) }
    val currentAppend by rememberUpdatedState(append)
    fun pixel(point: Offset): Offset {
        val scale=min(area.width.toFloat()/a.width,area.height.toFloat()/a.height)
        val origin=Offset((area.width-a.width*scale)/2,(area.height-a.height*scale)/2)
        return Offset(((point.x-origin.x)/scale).coerceIn(0f,a.width.toFloat()),((point.y-origin.y)/scale).coerceIn(0f,a.height.toFloat()))
    }
    Box(Modifier.size(240.dp).onSizeChanged { area=it }.pointerInput(a.id,editable,area) {
        if(editable)detectDragGestures(onDragStart={start=pixel(it);end=start},onDragEnd={
            val p=start;val q=end
            if(p!=null && q!=null) {
                val left=floor(min(p.x,q.x)).toInt();val top=floor(min(p.y,q.y)).toInt()
                val right=ceil(max(p.x,q.x)).toInt();val bottom=ceil(max(p.y,q.y)).toInt()
                if(right>left && bottom>top)currentAppend(SpriteFrame("Region ${System.currentTimeMillis()%10000}",left,top,right-left,bottom-top))
            }
            start=null;end=null
        },onDragCancel={start=null;end=null},onDrag={change,_->end=pixel(change.position);change.consume()})
    }) {
        Preview(a,root,Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val scale=min(size.width/a.width,size.height/a.height);val ox=(size.width-a.width*scale)/2;val oy=(size.height-a.height*scale)/2
            frames.forEach { f -> drawRect(Color.Yellow,Offset(ox+f.x*scale,oy+f.y*scale),androidx.compose.ui.geometry.Size(f.width*scale,f.height*scale),style=Stroke(2f)) }
            val p=start;val q=end
            if(p!=null && q!=null)drawRect(Color.Cyan,Offset(ox+min(p.x,q.x)*scale,oy+min(p.y,q.y)*scale),androidx.compose.ui.geometry.Size(abs(p.x-q.x)*scale,abs(p.y-q.y)*scale),style=Stroke(2f))
        }
    }
}
