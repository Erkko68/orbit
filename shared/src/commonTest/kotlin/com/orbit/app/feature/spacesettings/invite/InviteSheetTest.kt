package com.orbit.app.feature.spacesettings.invite

import androidx.compose.ui.test.ComposeUiTest
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
import com.orbit.app.core.share.TextSharer
import com.orbit.app.feature.member.SpaceMembersViewModel
import com.orbit.app.feature.spacesettings.SpaceSettingsViewModel
import com.orbit.app.feature.spacesettings.ui.SpaceSettingsScreen
import com.orbit.app.domain.repository.InvitationRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FailingInvitationRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryInvitationRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class InviteSheetTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val shared = mutableListOf<String>()

    /** Signs in as ana, who owns Flat 4B. */
    private fun ownedSpace(): SpaceRepository = runBlocking {
        auth.signIn("ana@orbit.app", "secret")
        InMemorySpaceRepository(auth).also { it.createSpace("Flat 4B", accent = 0) }
    }

    /** Shows the settings of the space and opens the invitation sheet from them. */
    private fun ComposeUiTest.showCard(spaces: SpaceRepository, invitations: InvitationRepository) {
        setContent {
            OrbitTheme {
                SpaceSettingsScreen(
                    spaceId = "space-1",
                    onBack = {},
                    onOpenMember = {},
                    onGone = {},
                    viewModel = SpaceSettingsViewModel("space-1", spaces, auth),
                    membersViewModel = SpaceMembersViewModel("space-1", spaces, auth),
                    inviteViewModel = InviteViewModel("space-1", invitations, spaces, auth),
                    sharer = TextSharer { shared += it },
                )
            }
        }
        onNodeWithText("Invite people").performScrollTo().performClick()
    }

    @Test
    fun opensOnACodeMadeForItAndShowsIt() = runComposeUiTest {
        val invitations = InMemoryInvitationRepository(auth)
        showCard(ownedSpace(), invitations)
        waitForIdle()

        val invitation = runBlocking { invitations.observeInvitations("space-1").first().single() }
        onNodeWithContentDescription(invitation.code).assertIsDisplayed()
        onNodeWithText("Share").assertIsDisplayed()
        onNodeWithText("New code").assertIsEnabled()
    }

    @Test
    fun reopeningKeepsTheSameCode() = runComposeUiTest {
        val invitations = InMemoryInvitationRepository(auth)
        showCard(ownedSpace(), invitations)
        waitForIdle()

        onNodeWithContentDescription("Close").performClick()
        onNodeWithText("Invite people").performScrollTo().performClick()
        waitForIdle()

        assertEquals(1, runBlocking { invitations.observeInvitations("space-1").first() }.size)
    }

    @Test
    fun waitsForTheInvitationsBeforeMakingOne() = runComposeUiTest {
        showCard(ownedSpace(), FailingInvitationRepository(neverLoad = true))

        onNodeWithContentDescription("Close").assertIsDisplayed()
        onNodeWithText("Share").assertDoesNotExist()
        onNodeWithText("Try again").assertDoesNotExist()
    }

    @Test
    fun sharesTheLinkAndTheCodeWithTheSpaceName() = runComposeUiTest {
        val invitations = InMemoryInvitationRepository(auth)
        showCard(ownedSpace(), invitations)
        waitForIdle()
        val invitation = runBlocking { invitations.observeInvitations("space-1").first().single() }

        onNodeWithText("Share").performClick()

        val message = shared.single()
        assertTrue("Flat 4B" in message, message)
        assertTrue(invitation.link in message, message)
        assertTrue(message.endsWith("code ${invitation.code}."), message)
    }

    @Test
    fun aNewCodeReplacesTheOldOne() = runComposeUiTest {
        val invitations = InMemoryInvitationRepository(auth)
        showCard(ownedSpace(), invitations)
        waitForIdle()
        val old = runBlocking { invitations.observeInvitations("space-1").first().single() }

        onNodeWithText("New code").performClick()
        waitForIdle()

        val all = runBlocking { invitations.observeInvitations("space-1").first() }
        assertTrue(all.single { it.code == old.code }.revoked)
        val fresh = all.single { !it.revoked }
        onNodeWithContentDescription(fresh.code).assertIsDisplayed()
        assertEquals(emptyList(), shared)
    }

    @Test
    fun saysWhenTheCodeCouldNotBeMadeAndOffersToTryAgain() = runComposeUiTest {
        showCard(ownedSpace(), FailingInvitationRepository())
        waitForIdle()

        onNodeWithText("Could not create the invitation. Try again.").assertIsDisplayed()
        onNodeWithText("Try again").assertIsEnabled()
        onNodeWithText("Share").assertDoesNotExist()
    }
}
