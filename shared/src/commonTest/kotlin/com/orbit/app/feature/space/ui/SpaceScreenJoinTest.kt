package com.orbit.app.feature.space.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.PendingInvitation
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.repository.InvitationRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.space.SpaceViewModel
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryInvitationRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.testing.startTestKoin
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** The join half of the New space dialog. */
@OptIn(ExperimentalTestApi::class)
class SpaceScreenJoinTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val invitations = InMemoryInvitationRepository(auth)
    private val spaces = InMemorySpaceRepository(auth, invitations)
    private val pending = PendingInvitation()

    // The space cards get their view models from Koin.
    @AfterTest
    fun stopDi() {
        stopKoin()
    }

    /** Ana owns Flat 4B and made an invitation to it. Ben is signed in, outside the space. Returns the code. */
    private fun invitationFromAnaWithBenSignedIn(): String = runBlocking {
        auth.signIn("ana@orbit.app", "secret")
        val code = invitations.createInvitation(spaces.createSpace("Flat 4B", accent = 0))
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        code
    }

    private fun ComposeUiTest.showSpaces(
        repository: SpaceRepository = spaces,
        invites: InvitationRepository = invitations,
    ) {
        startTestKoin(auth, repository, invitations = invites)
        val viewModel = SpaceViewModel(repository, auth, pending)
        setContent {
            OrbitTheme {
                SpaceScreen(onOpenSpace = {}, onOpenAccount = {}, viewModel = viewModel)
            }
        }
    }

    @Test
    fun joiningWaitsForACodeToBeTyped() = runComposeUiTest {
        showSpaces()
        onNodeWithText("New space").performClick()

        onNodeWithText("Join space").assertIsNotEnabled()
        onNodeWithText("Invite code or link").performTextInput("ABCD2345")

        onNodeWithText("Join space").assertIsEnabled()
    }

    @Test
    fun joinsTheSpaceOfTheTypedCode() = runComposeUiTest {
        val code = invitationFromAnaWithBenSignedIn()
        showSpaces()
        onNodeWithText("New space").performClick()
        onNodeWithText("Invite code or link").performTextInput(code)

        onNodeWithText("Join space").performClick()

        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("Create space").assertDoesNotExist()
    }

    @Test
    fun saysWhenTheCodeIsNotAnInvitation() = runComposeUiTest {
        invitationFromAnaWithBenSignedIn()
        showSpaces()
        onNodeWithText("New space").performClick()

        onNodeWithText("Invite code or link").performTextInput("ABCD2345")
        onNodeWithText("Join space").performClick()

        onNodeWithText("That is not an invitation code. Check it and try again.").assertIsDisplayed()
        onNodeWithText("Create space").assertIsDisplayed()
    }

    @Test
    fun saysWhenTheInvitationWasRevoked() = runComposeUiTest {
        val code = invitationFromAnaWithBenSignedIn()
        runBlocking { invitations.revokeInvitation(code) }
        showSpaces()
        onNodeWithText("New space").performClick()

        onNodeWithText("Invite code or link").performTextInput(code)
        onNodeWithText("Join space").performClick()

        onNodeWithText("This invitation was revoked. Ask for a new one.").assertIsDisplayed()
    }

    @Test
    fun saysWhenTheUserIsInTheSpaceAlready() = runComposeUiTest {
        val code = runBlocking {
            auth.signIn("ana@orbit.app", "secret")
            invitations.createInvitation(spaces.createSpace("Flat 4B", accent = 0))
        }
        showSpaces()
        onNodeWithText("New space").performClick()

        onNodeWithText("Invite code or link").performTextInput(code)
        onNodeWithText("Join space").performClick()

        onNodeWithText("You are already in this space.").assertIsDisplayed()
    }

    @Test
    fun anOpenedLinkShowsTheDialogWithItsCodeReadyToJoin() = runComposeUiTest {
        showSpaces()
        onNodeWithText("Create space").assertDoesNotExist()

        pending.offer("orbit://join/ABCD2345")
        waitForIdle()

        onNodeWithText("Create space").assertIsDisplayed()
        onNodeWithText("ABCD2345").assertIsDisplayed()
        onNodeWithText("Join space").assertIsEnabled()
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
        showSpaces(oldSpaces, oldInvitations)
        onNodeWithText("New space").performClick()

        onNodeWithText("Invite code or link").performTextInput(code)
        onNodeWithText("Join space").performClick()

        onNodeWithText("This invitation has expired. Ask for a new one.").assertIsDisplayed()
    }

    @Test
    fun saysWhenTheJoinFailsForAnyOtherReason() = runComposeUiTest {
        val code = invitationFromAnaWithBenSignedIn()
        val offline = object : SpaceRepository by spaces {
            override suspend fun joinSpace(code: String): String = error("Could not reach the server")
        }
        showSpaces(offline)
        onNodeWithText("New space").performClick()

        onNodeWithText("Invite code or link").performTextInput(code)
        onNodeWithText("Join space").performClick()

        onNodeWithText("Could not join the space. Check your connection and try again.").assertIsDisplayed()
        onNodeWithText("Join space").assertIsEnabled()
    }
}
