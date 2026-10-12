package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitCardTest : ComposeTest() {
    @Test
    fun showsItsContentWithoutBeingClickable() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitCard { Text("Create a new space") }
            }
        }
        onNodeWithText("Create a new space").assertIsDisplayed().assertHasNoClickAction()
    }

    @Test
    fun clicksWhenItHasAnAction() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme {
                OrbitCard(onClick = { clicks++ }) { Text("Electricity") }
            }
        }
        onNodeWithText("Electricity").assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun tellsItsContentItIsOnACard() = runComposeUiTest {
        var outside: Boolean? = null
        var inside: Boolean? = null
        setContent {
            OrbitTheme {
                outside = LocalInsideCard.current
                OrbitCard { inside = LocalInsideCard.current }
            }
        }
        waitForIdle()
        assertEquals(false, outside)
        assertEquals(true, inside)
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var clicks = 0
        var label by mutableStateOf("Electricity")
        var onClick by mutableStateOf<(() -> Unit)?>(null)
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitCard(
                    modifier = Modifier.testTag("card"),
                    onClick = onClick,
                    verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
                ) {
                    Text(label)
                }
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()
        onNodeWithText("Electricity").assertHasNoClickAction()

        label = "Water"
        onClick = { clicks++ }
        onNodeWithText("Water").performClick()
        assertEquals(1, clicks)
    }
}
