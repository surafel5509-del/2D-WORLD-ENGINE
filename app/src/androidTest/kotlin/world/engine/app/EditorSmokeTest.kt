package world.engine.app

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class EditorSmokeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun createAddSaveAndReturnToDashboard() {
        compose.onNodeWithText("New project").performClick()
        compose.onNodeWithText("Create",useUnmergedTree=true).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Add sprite").fetchSemanticsNodes().isNotEmpty() }
        repeat(3) { compose.onNodeWithText("Add sprite").performScrollTo().performClick() }
        compose.onNodeWithText("Save",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("MyGame • saved").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Projects",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Save & close").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("New project").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun generateSplitPlaceAndSaveVehicle() {
        compose.onNodeWithText("New project").performClick()
        compose.onNodeWithText("Create",useUnmergedTree=true).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Assets",substring=false).fetchSemanticsNodes().isNotEmpty() }
        // Toolbar Assets exists even when the phone tab is outside the visible scroll range.
        compose.onAllNodesWithText("Assets",substring=false)[0].performScrollTo().performClick()
        compose.onNodeWithText("Generate 335").performScrollTo().performClick()
        compose.onNodeWithText("Generate",substring=false).performClick()
        compose.waitUntil(90000) { compose.onAllNodesWithText("Search names, folders, tags (335)").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Expand").performScrollTo().performClick()
        compose.onAllNodesWithText("Search names, folders, tags (335)").onLast().performTextInput("Vehicle 1")
        compose.onAllNodesWithText("Vehicle 1",substring=false).onLast().performScrollTo().performClick()
        compose.onAllNodesWithText("Inspect / edit").onLast().performScrollTo().performClick()
        compose.onNodeWithText("Sheet / split").performClick()
        compose.onNodeWithText("Split into named prefab parts").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Vehicle 1 split",substring=false).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Use in scene").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Return to scene").fetchSemanticsNodes().any { !it.config.contains(SemanticsProperties.Disabled) } }
        compose.onNodeWithText("Return to scene").performClick()
        compose.onNodeWithText("Save",substring=false).performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("MyGame • saved").fetchSemanticsNodes().isNotEmpty() }
    }
}
