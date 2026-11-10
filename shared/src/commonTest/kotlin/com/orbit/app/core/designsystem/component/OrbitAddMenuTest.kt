package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_sparkle
import orbit.shared.generated.resources.ic_task
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitAddMenuTest : ComposeTest() {
    @Test
    fun opensItsActionsAndClosesOnAPickOrTheCloseButton() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        val picked = mutableListOf<String>()
        setContent {
            OrbitTheme {
                // Where the screen puts it: bottom right, so the menu opens up and to the left.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                    OrbitAddMenu(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        contentDescription = "Add",
                        closeDescription = "Close",
                        actions = listOf(
                            OrbitAddMenuAction("New task", Res.drawable.ic_task) { picked += "task" },
                            OrbitAddMenuAction("Plan with the assistant", Res.drawable.ic_sparkle) { picked += "assistant" },
                        ),
                    )
                }
            }
        }
        onNodeWithText("New task").assertDoesNotExist()

        onNodeWithContentDescription("Add").performClick()
        onNodeWithText("Plan with the assistant").assertIsDisplayed().performClick()
        onNodeWithText("New task").assertDoesNotExist()

        onNodeWithContentDescription("Add").performClick()
        onNodeWithContentDescription("Close").performClick()
        onNodeWithText("New task").assertDoesNotExist()

        assertEquals(listOf("assistant"), picked)
    }
}
