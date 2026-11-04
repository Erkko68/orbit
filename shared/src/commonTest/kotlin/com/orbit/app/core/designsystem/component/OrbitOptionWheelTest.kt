package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitOptionWheelTest : ComposeTest() {
    private var selected by mutableStateOf(1)

    @Test
    fun showsThePickedOptionAndTappingAnotherGoesToIt() = runComposeUiTest {
        setContent {
            OrbitTheme { OrbitOptionWheel(listOf("days", "weeks", "months"), selected, { selected = it }) }
        }

        onNodeWithText("weeks").assertIsDisplayed()
        onNodeWithText("months").performClick()
        waitForIdle()

        assertEquals(2, selected)
    }
}
