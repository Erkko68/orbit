package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
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
class OrbitButtonTest : ComposeTest() {
    @Test
    fun clicksWithA48dpTouchTarget() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme {
                OrbitButton("Sign in", onClick = { clicks++ })
            }
        }
        onNodeWithText("Sign in").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun ignoresClicksWhenDisabled() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme {
                OrbitButton("Sign in", onClick = { clicks++ }, enabled = false)
            }
        }
        onNodeWithText("Sign in").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var text by mutableStateOf("Sign in")
        var enabled by mutableStateOf(true)
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitButton(text, onClick = {}, modifier = Modifier.testTag("button"), enabled = enabled)
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()

        text = "Signing in"
        enabled = false
        onNodeWithTag("button").assertTextEquals("Signing in").assertIsNotEnabled()
    }
}
