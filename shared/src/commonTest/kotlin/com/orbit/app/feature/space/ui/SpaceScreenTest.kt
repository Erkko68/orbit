package com.orbit.app.feature.space.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.domain.model.Role
import com.orbit.app.feature.space.SpaceViewModel
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.startTestKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
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

    @Test
    fun showsACardPerSpaceAndSaysWhenCreatingFailed() = runComposeUiTest {
        // This fake has one space, "Flat 4B", and its createSpace always throws.
        val spaces = FakeSpaceRepository.withMembers("Ana" to Role.OWNER, "Ben" to Role.MEMBER)
        // The space cards get their view models from Koin.
        startTestKoin(InMemoryAuthRepository(), spaces)
        val viewModel = SpaceViewModel(spaces)
        setContent {
            OrbitTheme {
                SpaceScreen(onOpenSpace = {}, viewModel = viewModel)
            }
        }
        // The name field, not the card of the space, which is also called "Flat 4B".
        onNode(hasText("Flat 4B") and hasSetTextAction()).performScrollTo().performTextInput("Flat 4C")
        onNodeWithText("Create space").performScrollTo().performClick()
        onNodeWithText("Could not create the space. Try again.").performScrollTo().assertIsDisplayed()

        onNodeWithText("2 members", substring = true).performScrollTo().assertIsDisplayed()
    }

    @AfterTest
    fun stopDi() {
        stopKoin()
    }
}
