package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitLoadingStateTest : ComposeTest() {
    @Test
    fun saysWhatIsLoading() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitLoadingState("Loading your spaces", Modifier.testTag("loading"))
            }
        }
        onNodeWithTag("loading").assertIsDisplayed()
        onNodeWithText("Loading your spaces").assertIsDisplayed()
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var label by mutableStateOf("Loading your spaces")
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitLoadingState(label)
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()

        label = "Loading items"
        onNodeWithText("Loading items").assertIsDisplayed()
    }
}
