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
    fun offersTheOwnerToCreateAnInvitation() = runComposeUiTest {
        showCard(ownedSpace(), InMemoryInvitationRepository(auth))

        onNodeWithContentDescription("Close").assertIsDisplayed()
        onNodeWithText("Create invitation").assertIsEnabled()
        onNodeWithText("Share").assertDoesNotExist()
    }

    @Test
    fun opensAtOnceButOffersToCreateOnlyOnceTheInvitationsArrive() = runComposeUiTest {
        showCard(ownedSpace(), FailingInvitationRepository(neverLoad = true))

        onNodeWithContentDescription("Close").assertIsDisplayed()
        onNodeWithText("Create invitation").assertIsNotEnabled()
    }

    @Test
    fun createsAnInvitationAndShowsItsCodeAndLink() = runComposeUiTest {
        val invitations = InMemoryInvitationRepository(auth)
        showCard(ownedSpace(), invitations)

        onNodeWithText("Create invitation").performClick()

        val invitation = runBlocking { invitations.observeInvitations("space-1").first().single() }
        onNodeWithText(invitation.code).assertIsDisplayed()
        onNodeWithText(invitation.link).assertIsDisplayed()
        onNodeWithText("Share").assertIsDisplayed()
        onNodeWithText("Revoke").assertIsDisplayed()
        onNodeWithText("Create invitation").assertDoesNotExist()
    }

    @Test
    fun sharesTheLinkAndTheCodeWithTheSpaceName() = runComposeUiTest {
        val invitations = InMemoryInvitationRepository(auth)
        showCard(ownedSpace(), invitations)
        onNodeWithText("Create invitation").performClick()
        val invitation = runBlocking { invitations.observeInvitations("space-1").first().single() }

        onNodeWithText("Share").performClick()

        val message = shared.single()
        assertTrue("Flat 4B" in message, message)
        assertTrue(invitation.link in message, message)
        assertTrue(message.endsWith("code ${invitation.code}."), message)
    }

    @Test
    fun revokingGoesBackToOfferingANewInvitation() = runComposeUiTest {
        showCard(ownedSpace(), InMemoryInvitationRepository(auth))
        onNodeWithText("Create invitation").performClick()

        onNodeWithText("Revoke").performClick()

        onNodeWithText("Create invitation").assertIsDisplayed()
        onNodeWithText("Share").assertDoesNotExist()
        assertEquals(emptyList(), shared)
    }

    @Test
    fun saysWhenTheInvitationCouldNotBeCreated() = runComposeUiTest {
        showCard(ownedSpace(), FailingInvitationRepository())

        onNodeWithText("Create invitation").performClick()

        onNodeWithText("Could not create the invitation. Try again.").assertIsDisplayed()
        onNodeWithText("Create invitation").assertIsEnabled()
    }

    @Test
    fun closingTheSheetForgetsAFailure() = runComposeUiTest {
        showCard(ownedSpace(), FailingInvitationRepository())
        onNodeWithText("Create invitation").performClick()

        onNodeWithContentDescription("Close").performClick()
        onNodeWithText("Create invitation").assertDoesNotExist()
        onNodeWithText("Invite people").performScrollTo().performClick()

        onNodeWithContentDescription("Close").assertIsDisplayed()
        onNodeWithText("Could not create the invitation. Try again.").assertDoesNotExist()
    }
}
