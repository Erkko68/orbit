package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitDialogTest : ComposeTest() {
    @Test
    fun showsItsContentAndRunsItsActions() = runComposeUiTest {
        var confirmed = 0
        var dismissed = 0
        setContent {
            OrbitTheme {
                OrbitDialog("New space", "Cancel", { dismissed++ }, confirmText = "Create", onConfirm = { confirmed++ }) {
                    Text("Name it")
                }
            }
        }
        onNodeWithText("New space").assertIsDisplayed()
        onNodeWithText("Name it").assertIsDisplayed()

        onNodeWithText("Create").performClick()
        onNodeWithText("Cancel").performClick()

        assertEquals(1, confirmed)
        assertEquals(1, dismissed)
    }

    @Test
    fun canDisableItsConfirmAction() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitDialog("New space", "Cancel", {}, confirmText = "Create", confirmEnabled = false) {}
            }
        }
        onNodeWithText("Create").assertIsNotEnabled()
    }

    @Test
    fun hasOnlyAWayOutWithoutAConfirmAction() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitDialog("New space", "Cancel", {}) { Text("Name it") }
            }
        }
        onNodeWithText("Cancel").assertIsDisplayed()
        onNodeWithText("Name it").assertIsDisplayed()
    }
}
