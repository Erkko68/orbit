package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitBadgeTest : ComposeTest() {
    @Test
    fun showsALabelWithoutAnInteractiveAction() = runComposeUiTest {
        setContent { OrbitTheme { OrbitBadge("Task") } }
        onNodeWithText("Task").assertIsDisplayed().assertHasNoClickAction()
    }
}
