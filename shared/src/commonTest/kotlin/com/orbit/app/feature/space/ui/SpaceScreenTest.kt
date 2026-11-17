package com.orbit.app.feature.space.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.Role
import com.orbit.app.feature.space.SpaceViewModel
import com.orbit.app.core.PendingInvitation
import com.orbit.app.testing.FakeConnectivity
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.FlakySpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.testing.startTestKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SpaceScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()

    @AfterTest
    fun stopDi() {
        stopKoin()
    }

    @Test
    fun showsTheEmptyStateAndTheWayToGetASpace() = runComposeUiTest {
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth), auth, PendingInvitation(), FakeConnectivity())
        var assistantOpened = 0
        var manualOpened = 0
        setContent {
            OrbitTheme {
                SpaceScreen(
                    onOpenSpace = {},
                    onOpenAccount = {},
                    onNewSpace = { manualOpened++ },
                    onOpenAssistant = { assistantOpened++ },
                    viewModel = viewModel
                )
            }
        }
        onNodeWithText("Orbit").assertIsDisplayed()
        onNodeWithText("No spaces yet").assertIsDisplayed()
        onNodeWithContentDescription("New space").assertIsDisplayed()
        onNodeWithText("Set up with the assistant").performScrollTo().performClick()
        assertEquals(1, assistantOpened)
        onNodeWithText("New space").performScrollTo().performClick()
        assertEquals(1, manualOpened)
    }

    @Test
    fun disablesTheAssistantOfflineAndRestoresItWithTheConnection() = runComposeUiTest {
        val connectivity = FakeConnectivity(online = false)
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth), auth, PendingInvitation(), connectivity)
        var assistantOpened = 0
        var manualOpened = 0
        setContent {
            OrbitTheme {
                SpaceScreen(
                    onOpenSpace = {},
                    onOpenAccount = {},
                    onNewSpace = { manualOpened++ },
                    onOpenAssistant = { assistantOpened++ },
                    viewModel = viewModel,
                )
            }
        }

        onNodeWithText("Set up with the assistant").performScrollTo().assertIsNotEnabled().performClick()
        onNodeWithText("The assistant needs a connection. You can still add items by hand.")
            .performScrollTo()
            .assertIsDisplayed()
        onNodeWithText("New space").performScrollTo().assertIsEnabled().performClick()
        assertEquals(0, assistantOpened)
        assertEquals(1, manualOpened)

        connectivity.setOnline(true)
        waitForIdle()

        onNodeWithText("The assistant needs a connection. You can still add items by hand.").assertDoesNotExist()
        onNodeWithText("Set up with the assistant").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, assistantOpened)
    }

    @Test
    fun saysSoWhenTheSpacesCouldNotLoadAndLoadsThemOnRetry() = runComposeUiTest {
        val spaces = FlakySpaceRepository(spaceFailures = 1)
        // The space cards get their view models from Koin.
        startTestKoin(auth, spaces)
        val viewModel = SpaceViewModel(spaces, auth, PendingInvitation(), FakeConnectivity())
        setContent {
            OrbitTheme {
                SpaceScreen(
                    onOpenSpace = {},
                    onOpenAccount = {},
                    onNewSpace = {},
                    onOpenAssistant = {},
                    viewModel = viewModel
                )
            }
        }
        onNodeWithText("Could not load your spaces").assertIsDisplayed()
        onNodeWithText("No spaces yet").assertDoesNotExist()
        onNodeWithText("Set up with the assistant").assertDoesNotExist()

        onNodeWithText("Try again").performScrollTo().performClick()

        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("Could not load your spaces").assertDoesNotExist()
    }

    @Test
    fun theAddButtonAndAnOpenedLinkBothAskForTheNewSpaceScreen() = runComposeUiTest {
        val pending = PendingInvitation()
        var asked = 0
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth), auth, pending, FakeConnectivity())
        setContent {
            OrbitTheme {
                SpaceScreen(
                    onOpenSpace = {},
                    onOpenAccount = {},
                    onNewSpace = { asked++ },
                    onOpenAssistant = {},
                    viewModel = viewModel
                )
            }
        }
        onNodeWithContentDescription("New space").performClick()
        assertEquals(1, asked)

        pending.offer("orbit://join/ABCD2345")
        waitForIdle()

        assertEquals(2, asked)
    }

    @Test
    fun showsACardPerSpace() = runComposeUiTest {
        val spaces = FakeSpaceRepository.withMembers(
            "Ana" to Role.OWNER,
            "Ben" to Role.MEMBER,
            "Cy" to Role.MEMBER,
            "Di" to Role.MEMBER,
            "Ed" to Role.MEMBER,
        )
        // The space cards get their view models from Koin.
        startTestKoin(auth, spaces)
        val viewModel = SpaceViewModel(spaces, auth, PendingInvitation(), FakeConnectivity())
        setContent {
            OrbitTheme {
                SpaceScreen(
                    onOpenSpace = {},
                    onOpenAccount = {},
                    onNewSpace = {},
                    onOpenAssistant = {},
                    viewModel = viewModel
                )
            }
        }
        onNodeWithText("Your spaces").assertIsDisplayed()
        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("All clear").assertIsDisplayed()
        onNodeWithText("Set up with the assistant").assertDoesNotExist()
    }
}
