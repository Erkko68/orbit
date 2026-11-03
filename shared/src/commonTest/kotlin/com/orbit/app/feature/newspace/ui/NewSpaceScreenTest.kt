package com.orbit.app.feature.newspace.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.PendingInvitation
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.newspace.NewSpaceViewModel
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryInvitationRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class NewSpaceScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val invitations = InMemoryInvitationRepository(auth)
    private val spaces = InMemorySpaceRepository(auth, invitations)
    private val pending = PendingInvitation()

    private var opened: String? = null

    /** Ana owns Flat 4B and made an invitation to it. Ben is signed in, outside the space. Returns the code. */
    private fun invitationFromAnaWithBenSignedIn(): String = runBlocking {
        auth.signIn("ana@orbit.app", "secret")
        val code = invitations.createInvitation(spaces.createSpace("Flat 4B", accent = 0))
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        code
    }

    private fun ComposeUiTest.showNewSpace(repository: SpaceRepository = spaces) {
        val viewModel = NewSpaceViewModel(repository, pending)
        setContent {
            OrbitTheme {
                NewSpaceScreen(onClose = {}, onOpenSpace = { opened = it }, viewModel = viewModel)
            }
        }
    }

    @Test
    fun createsOnlyWithANameAndOpensTheSpace() = runComposeUiTest {
        runBlocking { auth.signIn("ana@orbit.app", "secret") }
        showNewSpace()
        onNodeWithText("Create space").performScrollTo().assertIsNotEnabled()

        onAllNodes(hasSetTextAction())[0].performTextInput("Flat 4B")
        onNodeWithContentDescription("Teal").performScrollTo().performClick()
        onNodeWithText("Create space").performScrollTo().assertIsEnabled().performClick()
        waitForIdle()

        val space = runBlocking { spaces.observeSpaces().first() }.single()
        assertEquals(space.id, opened)
        assertEquals(2, space.accent)
    }

    @Test
    fun saysWhenCreatingFailed() = runComposeUiTest {
        // This fake's createSpace always throws.
        showNewSpace(FakeSpaceRepository.withMembers())
        onAllNodes(hasSetTextAction())[0].performTextInput("Flat 4C")
        onNodeWithText("Create space").performScrollTo().performClick()

        onNodeWithText("Could not create the space. Try again.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun joiningWaitsForACodeToBeTyped() = runComposeUiTest {
        showNewSpace()

        onNodeWithText("Join space").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Invite code or link").performScrollTo().performTextInput("ABCD2345")

        onNodeWithText("Join space").performScrollTo().assertIsEnabled()
    }

    @Test
    fun joinsTheSpaceOfTheTypedCode() = runComposeUiTest {
        val code = invitationFromAnaWithBenSignedIn()
        showNewSpace()
        onNodeWithText("Invite code or link").performScrollTo().performTextInput(code)

        onNodeWithText("Join space").performScrollTo().performClick()
        waitForIdle()

        assertEquals(runBlocking { spaces.observeSpaces().first() }.single().id, opened)
    }

    @Test
    fun saysWhenTheCodeIsNotAnInvitation() = runComposeUiTest {
        invitationFromAnaWithBenSignedIn()
        showNewSpace()

        onNodeWithText("Invite code or link").performScrollTo().performTextInput("ABCD2345")
        onNodeWithText("Join space").performScrollTo().performClick()

        onNodeWithText("That is not an invitation code. Check it and try again.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saysWhenTheInvitationWasRevoked() = runComposeUiTest {
        val code = invitationFromAnaWithBenSignedIn()
        runBlocking { invitations.revokeInvitation(code) }
        showNewSpace()

        onNodeWithText("Invite code or link").performScrollTo().performTextInput(code)
        onNodeWithText("Join space").performScrollTo().performClick()

        onNodeWithText("This invitation was revoked. Ask for a new one.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saysWhenTheUserIsInTheSpaceAlready() = runComposeUiTest {
        val code = runBlocking {
            auth.signIn("ana@orbit.app", "secret")
            invitations.createInvitation(spaces.createSpace("Flat 4B", accent = 0))
        }
        showNewSpace()

        onNodeWithText("Invite code or link").performScrollTo().performTextInput(code)
        onNodeWithText("Join space").performScrollTo().performClick()

        onNodeWithText("You are already in this space.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun anOpenedLinkArrivesWithItsCodeReadyToJoin() = runComposeUiTest {
        pending.offer("orbit://join/ABCD2345")
        showNewSpace()

        onNodeWithText("ABCD2345").performScrollTo().assertIsDisplayed()
        onNodeWithText("Join space").performScrollTo().assertIsEnabled()
    }

    @Test
    fun saysWhenTheInvitationHasExpired() = runComposeUiTest {
        val longAgo = object : Clock {
            override fun now(): Instant = Clock.System.now() - 8.days
        }
        val oldInvitations = InMemoryInvitationRepository(auth, longAgo)
        val oldSpaces = InMemorySpaceRepository(auth, oldInvitations)
        val code = runBlocking {
            auth.signIn("ana@orbit.app", "secret")
            oldInvitations.createInvitation(oldSpaces.createSpace("Flat 4B", accent = 0))
        }
        runBlocking {
            auth.signOut()
            auth.signIn("ben@orbit.app", "secret")
        }
        showNewSpace(oldSpaces)

        onNodeWithText("Invite code or link").performScrollTo().performTextInput(code)
        onNodeWithText("Join space").performScrollTo().performClick()

        onNodeWithText("This invitation has expired. Ask for a new one.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saysWhenTheJoinFailsForAnyOtherReason() = runComposeUiTest {
        val code = invitationFromAnaWithBenSignedIn()
        val offline = object : SpaceRepository by spaces {
            override suspend fun joinSpace(code: String): String = error("Could not reach the server")
        }
        showNewSpace(offline)

        onNodeWithText("Invite code or link").performScrollTo().performTextInput(code)
        onNodeWithText("Join space").performScrollTo().performClick()

        onNodeWithText("Could not join the space. Check your connection and try again.").performScrollTo().assertIsDisplayed()
        onNodeWithText("Join space").performScrollTo().assertIsEnabled()
    }
}
