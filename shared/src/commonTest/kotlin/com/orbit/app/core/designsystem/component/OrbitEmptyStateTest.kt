package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitEmptyStateTest : ComposeTest() {
    @Test
    fun saysWhatIsMissingAndWhatWillFillIt() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitEmptyState("No items yet", "Items you add will show up here.")
            }
        }
        onNodeWithText("No items yet").assertIsDisplayed()
        onNodeWithText("Items you add will show up here.").assertIsDisplayed()
    }
}
