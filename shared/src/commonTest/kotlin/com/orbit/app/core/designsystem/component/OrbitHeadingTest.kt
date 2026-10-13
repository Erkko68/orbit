package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitHeadingTest : ComposeTest() {
    @Test
    fun showsItsTitleOverItsSubtitle() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitHeading("Join a space", "Use the invite you received.")
            }
        }
        val title = onNodeWithText("Join a space").assertIsDisplayed().getUnclippedBoundsInRoot()
        val subtitle = onNodeWithText("Use the invite you received.").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(subtitle.top >= title.bottom)
    }
}
