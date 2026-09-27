package world.engine.assets

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable enum class AssetKind { IMAGE, PREFAB }
@Serializable data class AssetRecord(
    val id: String, val name: String, val folder: String, val kind: AssetKind,
    val path: String, val revision: Int=1, val width: Int=0, val height: Int=0,
    val tags: List<String> = emptyList(), val favorite: Boolean=false, val recipe: String?=null
)

/** SQLite owns metadata and UUID dependency edges; immutable files hold payload revisions. */
internal class AssetDatabase(context: Context, root: File): SQLiteOpenHelper(context,File(root,"Resources/assets.sqlite").absolutePath,null,1) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE assets(id TEXT PRIMARY KEY, name TEXT NOT NULL, folder TEXT NOT NULL, kind TEXT NOT NULL, path TEXT NOT NULL, revision INTEGER NOT NULL, width INTEGER NOT NULL, height INTEGER NOT NULL, tags TEXT NOT NULL, favorite INTEGER NOT NULL, recipe TEXT)")
        db.execSQL("CREATE TABLE tombstones(id TEXT PRIMARY KEY)")
        db.execSQL("CREATE UNIQUE INDEX asset_recipe ON assets(recipe) WHERE recipe IS NOT NULL")
        db.execSQL("CREATE TABLE edges(owner TEXT NOT NULL REFERENCES assets(id) ON DELETE CASCADE, target TEXT NOT NULL REFERENCES assets(id) ON DELETE RESTRICT, PRIMARY KEY(owner,target))")
        db.execSQL("CREATE TABLE sheets(owner TEXT PRIMARY KEY REFERENCES assets(id) ON DELETE CASCADE, layout TEXT NOT NULL)")
        db.execSQL("CREATE INDEX edges_target ON edges(target)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Unsupported asset database upgrade: $oldVersion → $newVersion. Back up this project.") }
}

internal object Catalog {
    private val json=Json
    fun list(db: SQLiteDatabase): List<AssetRecord> = db.rawQuery("SELECT id,name,folder,kind,path,revision,width,height,tags,favorite,recipe FROM assets ORDER BY folder,name,id",null).use { c ->
        buildList { while(c.moveToNext()) add(AssetRecord(c.getString(0),c.getString(1),c.getString(2),AssetKind.valueOf(c.getString(3)),c.getString(4),c.getInt(5),c.getInt(6),c.getInt(7),json.decodeFromString<List<String>>(c.getString(8)),c.getInt(9)!=0,if(c.isNull(10)) null else c.getString(10))) }
    }
    fun put(db: SQLiteDatabase, a: AssetRecord) {
        val values=ContentValues().apply {
            put("id",a.id); put("name",a.name); put("folder",a.folder); put("kind",a.kind.name); put("path",a.path)
            put("revision",a.revision); put("width",a.width); put("height",a.height); put("tags",json.encodeToString(a.tags)); put("favorite",if(a.favorite)1 else 0); put("recipe",a.recipe)
        }
        if(db.update("assets",values,"id=?",arrayOf(a.id))==0) db.insertOrThrow("assets",null,values)
    }
    fun dependencies(db: SQLiteDatabase, owner: String): Set<String> = db.rawQuery("SELECT target FROM edges WHERE owner=?",arrayOf(owner)).use { c -> buildSet { while(c.moveToNext()) add(c.getString(0)) } }
    fun setDependencies(db: SQLiteDatabase, owner: String, targets: Set<String>) {
        fun reaches(start: String): Boolean {
            val pending=ArrayDeque<String>(); pending.add(start); val seen=mutableSetOf<String>()
            while(pending.isNotEmpty()) { val current=pending.removeFirst(); if(current==owner) return true; if(seen.add(current)) dependencies(db,current).forEach { pending.add(it) } }
            return false
        }
        require(targets.none { reaches(it) }) { "Prefab dependency cycle rejected" }
        db.delete("edges","owner=?",arrayOf(owner))
        targets.forEach { target -> db.execSQL("INSERT INTO edges(owner,target) VALUES(?,?)",arrayOf(owner,target)) }
    }
}
