package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitOutlinedButtonTest : ComposeTest() {
    @Test
    fun showsItsIconBeforeTheTextAndClicks() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme {
                OrbitOutlinedButton("Continue with Google", onClick = { clicks++ }, icon = { Box(Modifier.testTag("logo")) })
            }
        }
        val logo = onNodeWithTag("logo", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val text = onNodeWithText("Continue with Google", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(logo.right <= text.left)

        onNodeWithText("Continue with Google").assertHeightIsEqualTo(56.dp).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun ignoresClicksWhenDisabled() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme {
                OrbitOutlinedButton("Join space", onClick = { clicks++ }, enabled = false)
            }
        }
        onNodeWithText("Join space").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var text by mutableStateOf("Join space")
        var enabled by mutableStateOf(true)
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitOutlinedButton(
                    text = text,
                    onClick = {},
                    modifier = Modifier.testTag("button"),
                    enabled = enabled,
                    icon = { Box(Modifier) },
                )
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()

        text = "Joining"
        enabled = false
        onNodeWithTag("button").assertTextEquals("Joining").assertIsNotEnabled()
    }
}
