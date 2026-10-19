package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitAssistChipTest : ComposeTest() {
    @Test
    fun exposesAnActionWithoutFilterSelectionSemantics() = runComposeUiTest {
        var clicks = 0
        setContent { OrbitTheme { OrbitAssistChip("Add checklist", { clicks++ }) } }

        onNodeWithText("Add checklist")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
            .performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun disabledActionCannotRunAndCanBeEnabledAgain() = runComposeUiTest {
        var enabled by mutableStateOf(false)
        var clicks = 0
        setContent {
            OrbitTheme(darkTheme = true) { OrbitAssistChip("Add schedule", { clicks++ }, enabled = enabled) }
        }

        onNodeWithText("Add schedule").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
        runOnIdle { enabled = true }
        onNodeWithText("Add schedule").assertIsEnabled().performClick()
        assertEquals(1, clicks)
    }
}
