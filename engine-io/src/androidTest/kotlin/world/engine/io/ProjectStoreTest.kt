package world.engine.io

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import world.engine.core.*
import world.engine.math.Vec2

class ProjectStoreTest {
    @Test fun saveReopenRecoveryAndDiscard() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=ProjectStore(context)
        val scene=Scene(nodes=List(3) { Node(name="Sprite $it").moved(Vec2(it*33f,it*-17f)) })
        val project=store.create("Persistence test","com.example.persistence","Landscape","Empty",Scene())
        try {
            store.save(project,scene)
            assertEquals(scene,ProjectStore(context).open(project).saved)
            val changed=scene.replace(scene.nodes[0].moved(Vec2(999f,42f)))
            store.save(project,changed,true)
            val reopened=ProjectStore(context).open(project)
            assertEquals(scene,reopened.saved); assertEquals(changed,reopened.recovery)
            store.discardRecovery(project); assertNull(store.open(project).recovery)
            store.save(project,changed,true); store.save(project,changed)
            assertNull(store.open(project).recovery); assertEquals(changed,store.open(project).saved)
        } finally { project.directory.deleteRecursively() }
    }
    @Test fun corruptRecoveryDoesNotHideSavedScene() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext; val store=ProjectStore(context)
        val original=Scene(nodes=listOf(Node()))
        val project=store.create("Corrupt snapshot test","com.example.corrupt","Portrait","Empty",original)
        try {
            java.io.File(project.directory,".autosave/Main.json").writeText("invalid")
            val result=store.open(project)
            assertEquals(original,result.saved); assertNull(result.recovery); assertNotNull(result.recoveryIssue)
            store.discardRecovery(project); assertNull(store.open(project).recoveryIssue)
        } finally { project.directory.deleteRecursively() }
    }
    @Test fun atomicBackupIsRecovered() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext; val store=ProjectStore(context)
        val scene=Scene(nodes=listOf(Node()))
        val project=store.create("Atomic test","com.example.atomic","Portrait","Empty",scene)
        try {
            val file=java.io.File(project.directory,"Scenes/Main.json")
            file.copyTo(java.io.File(file.path+".bak")); file.writeText("truncated")
            assertEquals(scene,ProjectStore(context).open(project).saved)
        } finally { project.directory.deleteRecursively() }
    }
}
