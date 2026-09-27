package world.engine.assets

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import world.engine.core.*
import java.io.File
import java.util.UUID

class AssetPipelineTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun workspace()=File(context.cacheDir,"asset-test-${UUID.randomUUID()}").apply { mkdirs() }
    private fun png(root: File,name: String,color: Int): File {
        val bitmap=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888); bitmap.eraseColor(color)
        return File(root,name).also { file -> try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } } finally { bitmap.recycle() } }
    }
    @Test fun stableUuidRenameMoveReplaceDuplicateAndReferenceProtection() = runBlocking {
        val root=workspace()
        try {
            val repo=AssetRepository(context,root)
            val original=repo.importImage(Uri.fromFile(png(root,"input.png",Color.RED)))
            repo.metadata(original.id,"Hero","Actors/Players",listOf("player","red"),true)
            val moved=repo.list().single(); assertEquals(original.id,moved.id); assertEquals(original.path,moved.path)
            assertEquals("Hero",moved.name); assertTrue(moved.favorite)
            repo.saveSheet(moved.id,listOf(SpriteFrame("whole",0,0,16,16)))
            val replaced=repo.importImage(Uri.fromFile(png(root,"new.png",Color.BLUE)),original.id)
            assertEquals(original.id,replaced.id); assertNotEquals(original.path,replaced.path); assertEquals(2,replaced.revision)
            assertTrue(repo.file(original).exists()); assertTrue(repo.sheet(moved.id).isEmpty())
            val duplicate=repo.duplicate(original.id); assertNotEquals(original.id,duplicate.id)
            val scene=Scene(nodes=listOf(Node(components=listOf(TransformComponent(),SpriteComponent(assetId=original.id)))))
            try { repo.delete(original.id,listOf(scene)); fail("Referenced image deleted") } catch(expected: IllegalArgumentException) { assertTrue(expected.message!!.contains("used")) }
            val prefab=repo.savePrefab(Prefabs.capture(scene,scene.nodes[0].id),"Hero prefab")
            try { repo.delete(original.id,emptyList()); fail("Prefab dependency deleted") } catch(expected: IllegalArgumentException) { assertTrue(expected.message!!.contains("prefabs")) }
            repo.delete(prefab.id,emptyList()); repo.delete(original.id,emptyList())
            assertFalse(AssetRepository(context,root).list().any { it.id==original.id })
        } finally { root.deleteRecursively() }
    }
    @Test fun pixelOperationsAndSlicingPreserveContent() {
        val image=Bitmap.createBitmap(8,4,Bitmap.Config.ARGB_8888)
        image.setPixel(1,1,Color.RED); image.setPixel(6,2,Color.BLUE)
        try {
            val frames=SpriteSlicer.auto(image); assertEquals(2,frames.size)
            assertEquals(SpriteFrame("Part 1",1,1,1,1),frames[0])
            assertEquals(4,SpriteSlicer.grid(8,4,4,2).size)
            val flip=ImageOps.apply(image,TextureEdit("Flip X"))
            try { assertEquals(Color.RED,flip.getPixel(6,1)) } finally { flip.recycle() }
            val crop=ImageOps.apply(image,TextureEdit("Crop",1,1,1,1))
            try { assertEquals(Color.RED,crop.getPixel(0,0)) } finally { crop.recycle() }
        } finally { image.recycle() }
    }
    @Test fun generationCountIdempotenceAndTrueModularVehicleSplit() = runBlocking {
        val root=workspace()
        try {
            val repo=AssetRepository(context,root); repo.generateLibrary { _,_-> }
            val catalog=repo.list(); assertEquals(335,catalog.size)
            repo.generateLibrary { _,_-> }; assertEquals(catalog.map { it.id }.toSet(),repo.list().map { it.id }.toSet())
            val car=catalog.single { it.recipe=="Vehicle:1" }
            val split=repo.split(car.id,emptyList())
            val definition=repo.definitions().getValue(split.id)
            assertEquals(18,definition.nodes.size)
            assertEquals(4,definition.nodes.count { it.name.startsWith("Wheel") })
            assertTrue(definition.nodes.any { it.name=="Interior" })
            val nodes=Prefabs.instantiate(split.id,repo.definitions()); Scene(nodes=nodes).validated()
            assertEquals(17,nodes.count { it.parent!=null })
        } finally { root.deleteRecursively() }
    }
    @Test fun layoutsPersistOrderingAndRejectedCyclesRollback() = runBlocking {
        val root=workspace()
        try {
            val repo=AssetRepository(context,root); val image=repo.importImage(Uri.fromFile(png(root,"source.png",Color.WHITE)))
            val frames=listOf(SpriteFrame("Second",8,0,8,16),SpriteFrame("First",0,0,8,16))
            repo.saveSheet(image.id,frames); assertEquals(frames,AssetRepository(context,root).sheet(image.id))
            val exported=File(root,"frames.zip")
            repo.exportFrames(image.id,frames,Uri.fromFile(exported))
            java.util.zip.ZipFile(exported).use { zip ->
                assertEquals(3,zip.size()); assertNotNull(zip.getEntry("frame-1.png")); assertNotNull(zip.getEntry("frame-2.png"))
                val manifest=zip.getInputStream(zip.getEntry("frames.json")).bufferedReader().use { it.readText() }
                assertTrue(manifest.indexOf("Second")<manifest.indexOf("First"))
            }
            val node=Node(name="Base"); val base=repo.savePrefab(PrefabDefinition(nodes=listOf(node)),"Base")
            val nested=repo.savePrefab(PrefabDefinition(nodes=listOf(Node(nestedPrefab=base.id,components=listOf(TransformComponent())))),"Nested")
            try { repo.savePrefab(PrefabDefinition(nodes=listOf(Node(nestedPrefab=nested.id,components=listOf(TransformComponent())))),"Base",base.id); fail("Cycle allowed") } catch(expected: IllegalArgumentException) { assertTrue(expected.message!!.contains("cycle")) }
            assertEquals(base.path,repo.list().single { it.id==base.id }.path)
        } finally { root.deleteRecursively() }
    }
    @Test fun generatedTextureEdgesMatch() {
        ProceduralAssets.recipes().filter { it.startsWith("Texture:") }.forEach { recipe ->
            val image=ProceduralAssets.render(recipe)
            try { repeat(64) { p -> assertEquals(image.getPixel(0,p),image.getPixel(63,p)); assertEquals(image.getPixel(p,0),image.getPixel(p,63)) } } finally { image.recycle() }
        }
    }
}
