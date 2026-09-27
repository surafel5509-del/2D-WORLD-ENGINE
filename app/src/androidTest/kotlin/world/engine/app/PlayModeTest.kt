package world.engine.app

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class PlayModeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun realPlayPauseStepStopDoesNotOverwriteSavedScene() {
        val projects=File(compose.activity.filesDir,"2DWorldProjects")
        val before=projects.listFiles().orEmpty().map { it.name }.toSet()
        compose.onNodeWithText("New project").performClick();compose.onNodeWithText("Create",useUnmergedTree=true).performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("Systems",substring=false).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Systems").performScrollTo().performClick();compose.onNodeWithText("Scene",substring=false).performClick()
        compose.onNodeWithText("Add platformer test rig").performScrollTo().performClick()
        compose.onNodeWithText("Save",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("MyGame • saved").fetchSemanticsNodes().isNotEmpty()}
        val project=projects.listFiles().orEmpty().single { it.name !in before }
        val scene=File(project,"Scenes/Main.json");val saved=scene.readBytes()
        compose.onNodeWithText("Play",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("PLAY · step",substring=true).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Pause",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("PAUSED · step",substring=true).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Step",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Stop",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("MyGame • saved").fetchSemanticsNodes().isNotEmpty()}
        assertArrayEquals(saved,scene.readBytes())
        compose.onNodeWithText("Save",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("Save",substring=false).fetchSemanticsNodes().any { !it.config.contains(SemanticsProperties.Disabled) }}
        assertArrayEquals(saved,scene.readBytes())
    }
}
