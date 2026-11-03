package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
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
}
