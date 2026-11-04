package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitNumberWheelTest : ComposeTest() {
    private var weeks by mutableStateOf(2)

    @Test
    fun showsTheNumberAndTypingMovesIt() = runComposeUiTest {
        setContent { OrbitTheme { OrbitNumberWheel(weeks, { weeks = it }, description = "weeks") } }

        onNodeWithContentDescription("weeks").assertTextEquals("02")
        onNodeWithContentDescription("weeks").performTextInput("4")

        assertEquals(24, weeks)
    }
}
