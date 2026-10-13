package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitLabeledDividerTest : ComposeTest() {
    @Test
    fun showsItsLabel() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitLabeledDivider("or")
            }
        }
        onNodeWithText("or").assertIsDisplayed()
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var label by mutableStateOf("or")
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitLabeledDivider(label, Modifier.testTag("divider"))
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()

        label = "and"
        onNodeWithText("and").assertIsDisplayed()
    }
}
