package com.orbit.app.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitTaskRowTest : ComposeTest() {
    private val ana = OrbitFace("ana", "Ana", "A", Color.Cyan, OrbitShape.Cookie)

    @Test
    fun theTitleComesFirstAndTheRestFollows() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitTaskRow("Book the plumber", false, {}, "Complete", {}, summary = "Sink leaking", label = "TODAY", faces = listOf(ana))
            }
        }

        val title = onNodeWithText("Book the plumber", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val summary = onNodeWithText("Sink leaking", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val label = onNodeWithText("TODAY", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(title.bottom <= summary.top)
        assertTrue(summary.bottom <= label.top)
        onNodeWithContentDescription("Ana", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aRowWithOnlyATitleIsCompact() = runComposeUiTest {
        setContent { OrbitTheme { OrbitTaskRow("Take out the bins", false, {}, "Complete", {}) } }

        onNodeWithText("Take out the bins").assertIsDisplayed()
        val height = onNodeWithContentDescription("Complete", useUnmergedTree = true).getUnclippedBoundsInRoot().height
        assertTrue(height <= 96.dp)
    }

    @Test
    fun tappingOpensItAndTheCheckCompletesIt() = runComposeUiTest {
        var opens = 0
        val changes = mutableListOf<Boolean>()
        setContent { OrbitTheme { OrbitTaskRow("Task", false, { changes += it }, "Complete", { opens++ }) } }

        onNodeWithText("Task").performClick()
        onNodeWithContentDescription("Complete").performClick()

        assertEquals(1, opens)
        assertEquals(listOf(true), changes)
    }

    @Test
    fun theCheckWaitsWhileOff() = runComposeUiTest {
        setContent { OrbitTheme { OrbitTaskRow("Task", true, {}, "Reopen", {}, enabled = false) } }

        onNodeWithContentDescription("Reopen").assertIsNotEnabled()
    }
}
