package world.engine.io

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import world.engine.core.Scene
import java.io.File
import java.util.UUID

@Serializable data class ProjectManifest(val id: String = UUID.randomUUID().toString(), val name: String, val packageId: String, val orientation: String, val template: String, val version: Int = 1)
data class Project(val manifest: ProjectManifest, val directory: File)
data class OpenedProject(val project: Project, val saved: Scene, val recovery: Scene?, val recoveryIssue: String? = null)
data class ProjectListing(val projects: List<Project>, val errors: List<String>)

/** App-owned projects. Writes are serialized and AtomicFile retains the previous good file on failure. */
class ProjectStore(context: Context) {
    private val context = context.applicationContext
    val root = File(context.filesDir, "2DWorldProjects").apply { mkdirs() }
    private val lock = Mutex()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = false }
    private val folders = listOf("Scenes","Scripts","Sprites","Textures","Animations","Tilesets","Tilemaps","Materials","Particles","Audio/Music","Audio/SFX","UI","Prefabs","Physics","Fonts","Shaders","Data","Resources","Plugins","Build","Exports",".autosave")
    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { lock.withLock { block() } }
    private fun read(file: File): String {
        val atomic=AtomicFile(file)
        atomic.openRead().use { input ->
            val data=input.readBytesLimited(8 * 1024 * 1024)
            return data.toString(Charsets.UTF_8)
        }
    }
    private fun java.io.InputStream.readBytesLimited(max: Int): ByteArray {
        val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
        while(true) { val count=read(buffer); if(count<0) break; require(out.size()+count<=max) { "File exceeds size limit" }; out.write(buffer,0,count) }
        return out.toByteArray()
    }
    private fun write(file: File, text: String) {
        file.parentFile?.mkdirs(); val atomic=AtomicFile(file); val stream=atomic.startWrite()
        try { stream.write(text.toByteArray()); atomic.finishWrite(stream) }
        catch(e: Exception) { atomic.failWrite(stream); throw e }
    }
    private fun scene(file: File) = json.decodeFromString<Scene>(read(file)).validated()
    suspend fun list(): ProjectListing = io {
        val errors=mutableListOf<String>()
        val projects=root.listFiles().orEmpty().filter { it.isDirectory }.mapNotNull { dir ->
            try {
                val m=json.decodeFromString<ProjectManifest>(read(File(dir,"project.json")))
                require(m.version==1 && m.id==dir.name) { "Invalid project manifest" }
                Project(m,dir)
            } catch(e: Exception) { errors.add("${dir.name}: ${e.message}"); null }
        }.sortedByDescending { File(it.directory,"Scenes/Main.json").lastModified() }
        ProjectListing(projects,errors)
    }
    suspend fun create(name: String, packageId: String, orientation: String, template: String, initial: Scene): Project = io {
        require(name.trim().length in 1..64) { "Use a project name of 1–64 characters" }
        require(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+").matches(packageId)) { "Use a lowercase package such as com.example.mygame" }
        require(orientation in listOf("Portrait","Landscape"))
        require(template in listOf("Empty","Three sprites"))
        val m=ProjectManifest(name=name.trim(),packageId=packageId,orientation=orientation,template=template)
        val dir=File(root,m.id)
        try {
            folders.forEach { require(File(dir,it).mkdirs()) { "Cannot create project directory" } }
            write(File(dir,"Scenes/Main.json"),json.encodeToString(initial.validated()))
            write(File(dir,"project.json"),json.encodeToString(m))
            Project(m,dir)
        } catch(e: Exception) { dir.deleteRecursively(); throw e }
    }
    suspend fun open(project: Project): OpenedProject = io {
        val saved=scene(File(project.directory,"Scenes/Main.json"))
        val recoveryFile=File(project.directory,".autosave/Main.json")
        var issue: String?=null
        val recovery=try {
            if(recoveryFile.exists() || File(recoveryFile.path+".bak").exists()) scene(recoveryFile).takeIf { it!=saved } else null
        } catch(e: Exception) { issue="The recovery snapshot cannot be read: ${e.message}. Your explicit save is intact."; null }
        OpenedProject(project,saved,recovery,issue)
    }
    suspend fun save(project: Project, scene: Scene, autosave: Boolean = false) = io {
        write(File(project.directory,if(autosave) ".autosave/Main.json" else "Scenes/Main.json"),json.encodeToString(scene.validated()))
        if(!autosave) AtomicFile(File(project.directory,".autosave/Main.json")).delete()
    }
    suspend fun discardRecovery(project: Project) = io { AtomicFile(File(project.directory,".autosave/Main.json")).delete() }
    /** Decode bounded images then normalize to PNG; external URI permissions need not survive a restart. */
    suspend fun importSprite(project: Project, uri: Uri): String = io {
        val bytes=context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(16*1024*1024) } ?: error("Cannot read image")
        val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
        require(options.outWidth>0 && options.outHeight>0) { "Unsupported image format" }
        options.inSampleSize=1
        while((options.outWidth.toLong()+options.inSampleSize-1)/options.inSampleSize>2048 || (options.outHeight.toLong()+options.inSampleSize-1)/options.inSampleSize>2048) options.inSampleSize*=2
        options.inJustDecodeBounds=false
        val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: error("Image decode failed")
        val path="Sprites/${UUID.randomUUID()}.png"
        val target=File(project.directory,path)
        try { target.outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }; path }
        catch(e: Exception) { target.delete(); throw e }
        finally { bitmap.recycle() }
    }
}
