package com.orbit.app.feature.space.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.domain.model.Role
import com.orbit.app.feature.space.SpaceViewModel
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.FlakySpaceRepository
import com.orbit.app.testing.startTestKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SpaceScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()

    @AfterTest
    fun stopDi() {
        stopKoin()
    }

    @Test
    fun showsTheEmptyStateAndTheWayToGetASpace() = runComposeUiTest {
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth), auth)
        setContent {
            OrbitTheme {
                SpaceScreen(onOpenSpace = {}, viewModel = viewModel)
            }
        }
        onNodeWithText("Orbit").assertIsDisplayed()
        onNodeWithText("No spaces yet").assertIsDisplayed()
        onNodeWithText("New space").assertIsDisplayed()
    }

    @Test
    fun saysSoWhenTheSpacesCouldNotLoadAndLoadsThemOnRetry() = runComposeUiTest {
        val spaces = FlakySpaceRepository(failures = 1)
        // The space cards get their view models from Koin.
        startTestKoin(auth, spaces)
        val viewModel = SpaceViewModel(spaces, auth)
        setContent {
            OrbitTheme {
                SpaceScreen(onOpenSpace = {}, viewModel = viewModel)
            }
        }
        onNodeWithText("Could not load your spaces").assertIsDisplayed()
        onNodeWithText("No spaces yet").assertDoesNotExist()

        onNodeWithText("Try again").performClick()

        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("Could not load your spaces").assertDoesNotExist()
    }

    @Test
    fun newSpaceOpensADialogThatCreatesOnlyWithAName() = runComposeUiTest {
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth), auth)
        setContent {
            OrbitTheme {
                SpaceScreen(onOpenSpace = {}, viewModel = viewModel)
            }
        }
        onNodeWithText("Create space").assertDoesNotExist()

        onNodeWithText("New space").performClick()

        onNodeWithText("Create space").assertIsNotEnabled()
        onNode(hasSetTextAction() and isEnabled()).performTextInput("Flat 4B")
        onNodeWithText("Create space").assertIsEnabled()
        // Joining is a placeholder until invites land.
        onNodeWithText("Invite code or link").assertIsNotEnabled()
        onNodeWithText("Join space").assertIsNotEnabled()

        onNodeWithText("Cancel").performClick()
        onNodeWithText("Create space").assertDoesNotExist()
    }

    @Test
    fun showsACardPerSpaceAndSaysWhenCreatingFailed() = runComposeUiTest {
        // This fake has one space, "Flat 4B", and its createSpace always throws.
        val spaces = FakeSpaceRepository.withMembers(
            "Ana" to Role.OWNER,
            "Ben" to Role.MEMBER,
            "Cy" to Role.MEMBER,
            "Di" to Role.MEMBER,
            "Ed" to Role.MEMBER,
        )
        // The space cards get their view models from Koin.
        startTestKoin(auth, spaces)
        val viewModel = SpaceViewModel(spaces, auth)
        setContent {
            OrbitTheme {
                SpaceScreen(onOpenSpace = {}, viewModel = viewModel)
            }
        }
        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("5 members").assertIsDisplayed()
        // Three avatars, then the rest as a count.
        onNodeWithText("C").assertIsDisplayed()
        onNodeWithText("D").assertDoesNotExist()
        onNodeWithText("+2").assertIsDisplayed()

        onNodeWithText("New space").performClick()
        onNode(hasSetTextAction() and isEnabled()).performTextInput("Flat 4C")
        onNodeWithText("Create space").performClick()
        onNodeWithText("Could not create the space. Try again.").assertIsDisplayed()
    }
}
