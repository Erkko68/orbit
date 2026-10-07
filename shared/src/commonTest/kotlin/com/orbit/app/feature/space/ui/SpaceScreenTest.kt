package com.orbit.app.feature.space.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.feature.space.SpaceViewModel
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SpaceScreenTest : ComposeTest() {
    @Test
    fun showsTheEmptyMessageWithoutSpaces() = runComposeUiTest {
        val viewModel = SpaceViewModel(InMemorySpaceRepository(InMemoryAuthRepository()))
        setContent {
            OrbitTheme {
                SpaceScreen(viewModel)
            }
        }
        onNodeWithText("Orbit").assertIsDisplayed()
        onNodeWithText("No spaces yet").assertIsDisplayed()
    }
}
