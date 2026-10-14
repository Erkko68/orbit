package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitSelectionFieldTest : ComposeTest() {
    @Test
    fun exposesTheSelectedValueAndOpensItsChoices() = runComposeUiTest {
        var opens = 0
        setContent { OrbitTheme { OrbitSelectionField("Europe/Rome", "Time zone", onClick = { opens++ }) } }
        onNodeWithContentDescription("Time zone").assertTextContains("Europe/Rome")
            .assertHasClickAction().performClick()
        assertEquals(1, opens)
    }

    @Test
    fun preventsOpeningWhileDisabled() = runComposeUiTest {
        var opens = 0
        setContent {
            OrbitTheme { OrbitSelectionField("Europe/Rome", "Time zone", onClick = { opens++ }, enabled = false) }
        }
        onNodeWithContentDescription("Time zone").assertIsNotEnabled().performClick()
        assertEquals(0, opens)
    }
}
