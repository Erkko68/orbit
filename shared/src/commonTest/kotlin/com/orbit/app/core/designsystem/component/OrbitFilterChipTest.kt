package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitFilterChipTest : ComposeTest() {
    @Test
    fun disabledChipDoesNotInvokeItsActionAndCanBeEnabledAgain() = runComposeUiTest {
        var enabled by mutableStateOf(false)
        var clicks = 0
        setContent {
            OrbitTheme { OrbitFilterChip("Add checklist", false, { clicks++ }, enabled = enabled) }
        }
        onNodeWithText("Add checklist").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
        enabled = true
        onNodeWithText("Add checklist").assertIsEnabled().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun selectsOnClickWithA48dpTouchTarget() = runComposeUiTest {
        var selected by mutableStateOf(false)
        setContent {
            OrbitTheme {
                OrbitFilterChip("Tasks", selected = selected, onClick = { selected = !selected })
            }
        }
        onNodeWithText("Tasks").assertIsNotSelected().assertTouchHeightIsEqualTo(48.dp).performClick()
        onNodeWithText("Tasks").assertIsSelected()
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var label by mutableStateOf("Tasks")
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitFilterChip(label, selected = true, onClick = {}, modifier = Modifier.testTag("chip"))
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()

        label = "Lists"
        onNodeWithText("Lists").assertIsSelected()
    }
}
