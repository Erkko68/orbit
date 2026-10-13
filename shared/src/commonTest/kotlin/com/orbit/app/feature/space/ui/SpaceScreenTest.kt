package com.orbit.app.feature.space.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
                SpaceScreen(onOpenSpace = {}, viewModel = viewModel)
            }
        }
        onNodeWithText("Create or join a space").assertIsDisplayed()
        onNodeWithText("No spaces yet").assertIsDisplayed()
    }

    @Test
    fun createsOnlyWithANameAndJoiningIsAPlaceholder() = runComposeUiTest {
        val viewModel = SpaceViewModel(InMemorySpaceRepository(InMemoryAuthRepository()))
        setContent {
            OrbitTheme {
                SpaceScreen(onOpenSpace = {}, viewModel = viewModel)
            }
        }
        onNodeWithText("Create space").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Flat 4B").performTextInput("Flat 4B")
        onNodeWithText("Create space").assertIsEnabled()

        onNodeWithText("Invite code or link").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Join space").performScrollTo().assertIsNotEnabled()
    }
}
