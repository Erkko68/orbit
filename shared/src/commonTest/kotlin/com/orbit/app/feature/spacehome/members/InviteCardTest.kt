package com.orbit.app.feature.spacehome.members

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.share.TextSharer
import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemoryInvitationRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.InvitationRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FailingInvitationRepository
import com.orbit.app.testing.FakeSpaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class InviteCardTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val shared = mutableListOf<String>()

    /** Signs in as ana, who owns Flat 4B. */
    private fun ownedSpace(): SpaceRepository = runBlocking {
        auth.signIn("ana@orbit.app", "secret")
        InMemorySpaceRepository(auth).also { it.createSpace("Flat 4B", accent = 0) }
    }

    private fun ComposeUiTest.showCard(spaces: SpaceRepository, invitations: InvitationRepository) = setContent {
        OrbitTheme {
            InviteCard(
                spaceId = "space-1",
                viewModel = InviteViewModel("space-1", invitations, spaces, auth),
                sharer = TextSharer { shared += it },
            )
        }
    }

    @Test
    fun showsNothingToAPlainMember() = runComposeUiTest {
        runBlocking { auth.signIn("ben@orbit.app", "secret") }
        val spaces = FakeSpaceRepository.withMembers("ben@orbit.app" to Role.MEMBER, "cleo@orbit.app" to Role.OWNER)

        showCard(spaces, InMemoryInvitationRepository(auth))

        onNodeWithText("Invite people").assertDoesNotExist()
    }

    @Test
    fun offersTheOwnerToCreateAnInvitation() = runComposeUiTest {
        showCard(ownedSpace(), InMemoryInvitationRepository(auth))

        onNodeWithText("Invite people").assertIsDisplayed()
        onNodeWithText("Create invitation").assertIsEnabled()
        onNodeWithText("Share").assertDoesNotExist()
    }

    @Test
    fun showsTheCardAtOnceButOffersToCreateOnlyOnceTheInvitationsArrive() = runComposeUiTest {
        showCard(ownedSpace(), FailingInvitationRepository(neverLoad = true))

        onNodeWithText("Invite people").assertIsDisplayed()
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
}
