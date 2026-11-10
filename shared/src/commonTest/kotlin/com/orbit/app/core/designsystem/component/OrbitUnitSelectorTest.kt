package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitUnitSelectorTest : ComposeTest() {
    @Test
    fun selectsTheVisibleSegmentsAndKeepsAnAccessibleTouchTarget() = runComposeUiTest {
        var percent by mutableStateOf(false)
        setContent {
            OrbitTheme {
                OrbitUnitSelector("€", "%", "Euros", "Percentages", percent, { percent = it })
            }
        }
        onNodeWithText("€").assertIsSelected().assertTouchHeightIsEqualTo(48.dp)
        onNodeWithContentDescription("Percentages").assertIsNotSelected().performClick()
        onNodeWithText("%").assertIsSelected()
        onNodeWithText("€").assertIsNotSelected().performClick()
        onNodeWithText("€").assertIsSelected()
    }

    @Test
    fun disabledSegmentsDoNotChangeSelection() = runComposeUiTest {
        var actions = 0
        setContent {
            OrbitTheme {
                OrbitUnitSelector("€", "%", "Euros", "Percentages", false, { actions++ }, enabled = false)
            }
        }
        onNodeWithText("%").assertIsNotEnabled().performClick()
        assertEquals(0, actions)
    }
}
