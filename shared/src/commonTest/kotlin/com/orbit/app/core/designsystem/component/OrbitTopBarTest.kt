package com.orbit.app.core.designsystem.component

import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitTopBarTest : ComposeTest() {
    @Test
    fun showsItsTitlesAndButtons() = runComposeUiTest {
        var backs = 0
        var edits = 0
        setContent {
            OrbitTheme {
                OrbitTopBar(
                    title = "Task",
                    modifier = Modifier.testTag("bar"),
                    subtitle = "Flat 4B",
                    navigationIcon = { IconButton(onClick = { backs++ }) { Text("Back") } },
                    actions = { IconButton(onClick = { edits++ }) { Text("Edit") } },
                )
            }
        }
        onNodeWithTag("bar").assertHeightIsAtLeast(64.dp)
        onNodeWithText("Task").assertIsDisplayed()
        onNodeWithText("Flat 4B").assertIsDisplayed()

        onNodeWithText("Back").performClick()
        onNodeWithText("Edit").performClick()
        assertEquals(1, backs)
        assertEquals(1, edits)
    }

    @Test
    fun showsATitleAlone() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitTopBar("Orbit")
            }
        }
        onNodeWithText("Orbit").assertIsDisplayed()
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var title by mutableStateOf("Items")
        var subtitle by mutableStateOf<String?>("Flat 4B")
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitTopBar(
                    title = title,
                    modifier = Modifier.testTag("bar"),
                    subtitle = subtitle,
                    navigationIcon = { Text("Back") },
                    actions = { Text("Edit") },
                )
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()

        title = "Task"
        subtitle = null
        onNodeWithText("Task").assertIsDisplayed()
        onNodeWithText("Flat 4B").assertDoesNotExist()
    }
}
