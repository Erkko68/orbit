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
class OrbitScreenHeaderTest : ComposeTest() {
    @Test
    fun showsTheAppNameThenTheTitleAndSubtitle() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitScreenHeader("Welcome back", "Sign in to continue.")
            }
        }
        val name = onNodeWithText("Orbit").assertIsDisplayed().getUnclippedBoundsInRoot()
        val title = onNodeWithText("Welcome back").assertIsDisplayed().getUnclippedBoundsInRoot()
        onNodeWithText("Sign in to continue.").assertIsDisplayed()
        assertTrue(title.top >= name.bottom)
    }
}
