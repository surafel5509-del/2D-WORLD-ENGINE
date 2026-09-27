package world.engine.assets

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import world.engine.core.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Per-project operations are serialized on IO; database commits publish already-durable payloads. */
class AssetRepository(private val context: Context, val root: File) {
    private val mutex=Mutex()
    private val json=Json { prettyPrint=true }
    private suspend fun <T> work(block: (SQLiteDatabase)->T): T = withContext(Dispatchers.IO) {
        mutex.withLock { File(root,"Resources").mkdirs(); AssetDatabase(context,root).use { block(it.writableDatabase) } }
    }
    private fun <T> transaction(db: SQLiteDatabase, block: ()->T): T {
        db.beginTransaction()
        try { val result=block(); db.setTransactionSuccessful(); return result } finally { db.endTransaction() }
    }
    fun file(asset: AssetRecord): File {
        val file=File(root,asset.path)
        require(file.canonicalPath.startsWith(root.canonicalPath+File.separator)) { "Unsafe asset path" }
        return file
    }
    private fun get(db: SQLiteDatabase,id: String) = Catalog.list(db).singleOrNull { it.id==id } ?: error("Asset no longer exists: $id")
    suspend fun list(): List<AssetRecord> = work { db ->
        // Phase 1 UUID PNGs remain valid without modifying existing scene JSON.
        val deleted=db.rawQuery("SELECT id FROM tombstones",null).use { c -> buildSet { while(c.moveToNext())add(c.getString(0)) } }
        val known=Catalog.list(db).map { it.id }.toSet()+deleted
        File(root,"Sprites").listFiles().orEmpty().filter { it.extension=="png" && Scene.isAssetId(it.nameWithoutExtension) && it.nameWithoutExtension !in known }.forEach { f ->
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }; BitmapFactory.decodeFile(f.path,bounds)
            if(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048) Catalog.put(db,AssetRecord(f.nameWithoutExtension,"Imported ${f.nameWithoutExtension.take(8)}","Imported",AssetKind.IMAGE,"Sprites/${f.name}",width=bounds.outWidth,height=bounds.outHeight))
        }
        Catalog.list(db)
    }
    private fun validateMetadata(name: String,folder: String,tags: List<String>) {
        require(name.isNotBlank() && name.length<=96) { "Name must be 1–96 characters" }
        require(folder.length<=128 && folder.split('/').none { it==".." || it=="." } && !folder.startsWith('/')) { "Use a relative catalog folder" }
        require(tags.size<=32 && tags.all { it.length in 1..32 }) { "Use up to 32 tags of 1–32 characters" }
    }
    suspend fun metadata(id: String,name: String,folder: String,tags: List<String>,favorite: Boolean) = work { db ->
        validateMetadata(name,folder,tags)
        val a=get(db,id); Catalog.put(db,a.copy(name=name.trim(),folder=folder.trim('/'),tags=tags.distinct(),favorite=favorite))
    }
    private fun writeBitmap(db: SQLiteDatabase,bitmap: Bitmap,name: String,folder: String,prior: AssetRecord?=null,recipe: String?=null): AssetRecord {
        require(bitmap.width in 1..2048 && bitmap.height in 1..2048)
        validateMetadata(name,folder,emptyList())
        val id=prior?.id ?: UUID.randomUUID().toString()
        val path="Sprites/$id-${UUID.randomUUID()}.png"
        val result=prior?.copy(path=path,revision=prior.revision+1,width=bitmap.width,height=bitmap.height,recipe=null)
            ?: AssetRecord(id,name,folder,AssetKind.IMAGE,path,width=bitmap.width,height=bitmap.height,recipe=recipe)
        val target=file(result); target.parentFile?.mkdirs()
        try {
            FileOutputStream(target).use { require(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)); it.fd.sync() }
            transaction(db) { Catalog.put(db,result); if(prior!=null)db.delete("sheets","owner=?",arrayOf(id)) }; return result
        } catch(e: Exception) { target.delete(); throw e }
    }
    suspend fun importImage(uri: Uri,replaceId: String?=null): AssetRecord = work { db ->
        val prior=replaceId?.let { get(db,it).also { a -> require(a.kind==AssetKind.IMAGE) } }
        val bitmap=context.contentResolver.openInputStream(uri)?.use { ImageOps.decode(it) } ?: error("Cannot open chosen image")
        try { writeBitmap(db,bitmap,"Imported image","Imported",prior) } finally { bitmap.recycle() }
    }
    suspend fun duplicate(id: String): AssetRecord = work { db ->
        val a=get(db,id)
        if(a.kind==AssetKind.PREFAB) writePrefab(db,readPrefab(a),"${a.name.take(80)} copy",null)
        else {
            val bitmap=file(a).inputStream().use { ImageOps.decode(it) }
            try { writeBitmap(db,bitmap,"${a.name.take(80)} copy",a.folder) } finally { bitmap.recycle() }
        }
    }
    suspend fun editImage(id: String,edit: TextureEdit): AssetRecord = work { db ->
        val a=get(db,id); require(a.kind==AssetKind.IMAGE)
        val input=file(a).inputStream().use { ImageOps.decode(it) }
        try {
            val result=ImageOps.apply(input,edit)
            try { writeBitmap(db,result,"${a.name.take(72)} edited",a.folder) } finally { if(result!==input)result.recycle() }
        } finally { input.recycle() }
    }
    private fun readPrefab(a: AssetRecord): PrefabDefinition {
        require(a.kind==AssetKind.PREFAB && file(a).length()<=8*1024*1024)
        return json.decodeFromString<PrefabDefinition>(file(a).readText()).validated()
    }
    suspend fun definitions(): Map<String,PrefabDefinition> = work { db -> Catalog.list(db).filter { it.kind==AssetKind.PREFAB }.associate { it.id to readPrefab(it) } }
    private fun writePrefab(db: SQLiteDatabase,definition: PrefabDefinition,name: String,prior: AssetRecord?): AssetRecord {
        definition.validated(); validateMetadata(name,"Prefabs",emptyList())
        val id=prior?.id ?: UUID.randomUUID().toString()
        val a=AssetRecord(id,name,"Prefabs",AssetKind.PREFAB,"Prefabs/$id-${UUID.randomUUID()}.json",revision=(prior?.revision ?: 0)+1,tags=prior?.tags.orEmpty(),favorite=prior?.favorite ?: false)
        val target=file(a); target.parentFile?.mkdirs()
        try {
            FileOutputStream(target).use { it.write(json.encodeToString(definition).toByteArray()); it.fd.sync() }
            transaction(db) { Catalog.put(db,a); Catalog.setDependencies(db,id,definition.dependencies()) }
            return a
        } catch(e: Exception) { target.delete(); throw e }
    }
    suspend fun savePrefab(definition: PrefabDefinition,name: String,replaceId: String?=null): AssetRecord = work { db ->
        val prior=replaceId?.let { get(db,it).also { a -> require(a.kind==AssetKind.PREFAB) } }
        writePrefab(db,definition,name,prior)
    }
    suspend fun delete(id: String,retainedScenes: List<Scene>) = work { db ->
        val a=get(db,id)
        val dependents=db.rawQuery("SELECT a.name FROM edges e JOIN assets a ON e.owner=a.id WHERE e.target=?",arrayOf(id)).use { c -> buildList { while(c.moveToNext())add(c.getString(0)) } }
        require(dependents.isEmpty()) { "Asset is used by prefabs: ${dependents.joinToString()}" }
        val diskScenes=listOf("Scenes",".autosave").flatMap { File(root,it).listFiles().orEmpty().filter { f -> f.name.endsWith(".json") || f.name.endsWith(".json.bak") } }.map { f ->
            require(f.length()<=8*1024*1024) { "Cannot check references: oversized ${f.name}" }
            json.decodeFromString<Scene>(f.readText()).validated()
        }
        require((retainedScenes+diskScenes).none { scene -> id in scene.assetReferences() || scene.nodes.any { it.sprite?.asset==a.path } }) { "Asset is used by a scene, recovery snapshot, or undo history. Remove references, save, then close/reopen the project before deleting." }
        transaction(db) {
            db.delete("assets","id=?",arrayOf(id))
            db.execSQL("INSERT OR IGNORE INTO tombstones(id) VALUES(?)",arrayOf(id))
        }
        // Retain immutable payload revisions for crash safety; explicit garbage collection is separate.
    }
    suspend fun dependents(id: String): List<String> = work { db ->
        db.rawQuery("SELECT a.name FROM edges e JOIN assets a ON e.owner=a.id WHERE e.target=?",arrayOf(id)).use { c -> buildList { while(c.moveToNext()) add(c.getString(0)) } }
    }
    suspend fun export(id: String,uri: Uri) = work { db ->
        val a=get(db,id)
        require(a.kind==AssetKind.IMAGE) { "Use project backup for prefabs and their dependencies" }
        context.contentResolver.openOutputStream(uri,"wt")?.use { out -> file(a).inputStream().use { it.copyTo(out) } } ?: error("Cannot write destination")
    }
    suspend fun slice(id: String,frames: List<SpriteFrame>): List<AssetRecord> = work { db ->
        val a=get(db,id); require(frames.size in 1..256)
        val bitmap=file(a).inputStream().use { ImageOps.decode(it) }
        try {
            frames.forEach { it.validate(bitmap.width,bitmap.height) }
            storeSheet(db,id,frames)
            frames.map { frame ->
                val part=Bitmap.createBitmap(bitmap,frame.x,frame.y,frame.width,frame.height)
                try { writeBitmap(db,part,frame.name,"${a.folder}/Frames".trim('/')) } finally { if(part!==bitmap) part.recycle() }
            }
        } finally { bitmap.recycle() }
    }
    suspend fun autoFrames(id: String): List<SpriteFrame> = work { db ->
        val bitmap=file(get(db,id)).inputStream().use { ImageOps.decode(it) }
        try { SpriteSlicer.auto(bitmap) } finally { bitmap.recycle() }
    }
    suspend fun split(id: String,frames: List<SpriteFrame>): AssetRecord = work { db ->
        val a=get(db,id)
        val layers=a.recipe?.let { ProceduralAssets.layers(it) }
        val bitmap=if(layers==null) file(a).inputStream().use { ImageOps.decode(it) } else null
        try {
            val rootNode=Node(name=a.name,components=listOf(TransformComponent()))
            val children=if(layers!=null) layers.map { part ->
                val asset=writeBitmap(db,part.bitmap,part.name,"Parts/${a.name.take(80)}")
                part.node(asset.id,rootNode.id,a.width,a.height)
            } else {
                require(frames.size in 1..256) { "Choose manual, grid or automatic regions first" }
                val image=requireNotNull(bitmap)
                frames.forEach { it.validate(image.width,image.height) }
                frames.map { frame ->
                    val crop=Bitmap.createBitmap(image,frame.x,frame.y,frame.width,frame.height)
                    try {
                        val asset=writeBitmap(db,crop,frame.name,"Parts/${a.name.take(80)}")
                        ProceduralPart(frame.name,crop,frame.x,frame.y).node(asset.id,rootNode.id,image.width,image.height)
                    } finally { if(crop!==bitmap)crop.recycle() }
                }
            }
            writePrefab(db,PrefabDefinition(nodes=listOf(rootNode)+children),"${a.name.take(78)} split",null)
        } finally { bitmap?.recycle(); layers?.forEach { it.bitmap.recycle() } }
    }
    private fun storeSheet(db: SQLiteDatabase,id: String,frames: List<SpriteFrame>) {
        val asset=get(db,id); require(asset.kind==AssetKind.IMAGE && frames.size<=256)
        frames.forEach { it.validate(asset.width,asset.height) }
        db.execSQL("INSERT OR REPLACE INTO sheets(owner,layout) VALUES(?,?)",arrayOf(id,json.encodeToString(frames)))
    }
    suspend fun saveSheet(id: String,frames: List<SpriteFrame>) = work { db -> storeSheet(db,id,frames) }
    suspend fun sheet(id: String): List<SpriteFrame> = work { db ->
        db.rawQuery("SELECT layout FROM sheets WHERE owner=?",arrayOf(id)).use { c ->
            if(c.moveToFirst()) json.decodeFromString<List<SpriteFrame>>(c.getString(0)) else emptyList()
        }
    }
    suspend fun exportFrames(id: String,frames: List<SpriteFrame>,uri: Uri) = work { db ->
        require(frames.isNotEmpty()); val a=get(db,id)
        val image=file(a).inputStream().use { ImageOps.decode(it) }
        try {
            frames.forEach { it.validate(image.width,image.height) }
            storeSheet(db,id,frames)
            val stream=context.contentResolver.openOutputStream(uri,"wt") ?: error("Cannot write destination")
            java.util.zip.ZipOutputStream(stream).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("frames.json")); zip.write(json.encodeToString(frames).toByteArray()); zip.closeEntry()
                frames.forEachIndexed { index,frame ->
                    zip.putNextEntry(java.util.zip.ZipEntry("frame-${index+1}.png"))
                    val part=Bitmap.createBitmap(image,frame.x,frame.y,frame.width,frame.height)
                    try { require(part.compress(Bitmap.CompressFormat.PNG,100,zip)) } finally { if(part!==image)part.recycle() }
                    zip.closeEntry()
                }
            }
        } finally { image.recycle() }
    }
    /** Idempotent generation: each recipe is unique in SQLite, allowing interrupted runs to resume. */
    suspend fun generateLibrary(progress: (Int,Int)->Unit) {
        val job=currentCoroutineContext().job
        work { db ->
            File(root,"Resources/GENERATED_ASSETS_LICENSE.txt").writeText("Only original engine-generated procedural artwork is dedicated to the public domain under CC0 1.0: https://creativecommons.org/publicdomain/zero/1.0/ . No external artwork is bundled. User-imported images are NOT covered by this dedication. No warranty is provided.")
            val recipes=ProceduralAssets.recipes(); val existing=Catalog.list(db).mapNotNull { it.recipe }.toSet()
            recipes.forEachIndexed { index,recipe ->
                job.ensureActive()
                if(recipe !in existing) {
                    val bitmap=ProceduralAssets.render(recipe)
                    try { writeBitmap(db,bitmap,recipe.replace(':',' '),"Generated/${recipe.substringBefore(':')}",recipe=recipe) } finally { bitmap.recycle() }
                }
                progress(index+1,recipes.size)
            }
        }
    }
}
