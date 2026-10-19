package com.orbit.app.feature.spacehome.members

import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryInvitationRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.domain.model.Invitation
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.InvitationRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FailingInvitationRepository
import com.orbit.app.testing.FakeSpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class InviteViewModelTest {
    private val auth = InMemoryAuthRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.inviteViewModel(
        spaces: SpaceRepository,
        invitations: InvitationRepository,
    ) = InviteViewModel("space-1", invitations, spaces, auth).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    /** Signs in as ana, who owns Flat 4B. */
    private suspend fun ownedSpace(): InMemorySpaceRepository {
        auth.signIn("ana@orbit.app", "secret")
        return InMemorySpaceRepository(auth).also { it.createSpace("Flat 4B", accent = 0) }
    }

    private fun invitation(code: String, expiresAt: Instant, revoked: Boolean = false) =
        Invitation(code, "space-1", "ana@orbit.app", Clock.System.now(), expiresAt, revoked)

    @Test
    fun theOwnerCanInviteAndSeesNoInvitationYet() = runTest {
        val spaces = ownedSpace()

        val state = inviteViewModel(spaces, InMemoryInvitationRepository(auth)).uiState.value

        assertTrue(state.canInvite)
        assertEquals("Flat 4B", state.spaceName)
        assertNull(state.invitation)
    }

    @Test
    fun anAdminCanInviteAndAPlainMemberCannot() = runTest {
        val spaces = FakeSpaceRepository.withMembers(
            "ana@orbit.app" to Role.ADMIN,
            "ben@orbit.app" to Role.MEMBER,
            "cleo@orbit.app" to Role.OWNER,
        )
        auth.signIn("ana@orbit.app", "secret")
        assertTrue(inviteViewModel(spaces, InMemoryInvitationRepository(auth)).uiState.value.canInvite)

        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        assertFalse(inviteViewModel(spaces, InMemoryInvitationRepository(auth)).uiState.value.canInvite)
    }

    @Test
    fun aPlainMemberNeverAsksForTheInvitations() = runTest {
        val spaces = FakeSpaceRepository.withMembers("ben@orbit.app" to Role.MEMBER, "cleo@orbit.app" to Role.OWNER)
        auth.signIn("ben@orbit.app", "secret")

        val state = inviteViewModel(spaces, FailingInvitationRepository(failToLoad = true)).uiState.value

        assertFalse(state.canInvite)
        assertNull(state.failure)
    }

    @Test
    fun createsAnInvitationWithItsLinkAndCode() = runTest {
        val spaces = ownedSpace()
        val viewModel = inviteViewModel(spaces, InMemoryInvitationRepository(auth))

        viewModel.createInvitation()

        val invitation = assertNotNull(viewModel.uiState.value.invitation)
        assertEquals(8, invitation.code.length)
        assertEquals("orbit://join/${invitation.code}", invitation.link)
        assertFalse(viewModel.uiState.value.busy)
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun revokingTheInvitationLeavesNoneToShare() = runTest {
        val spaces = ownedSpace()
        val invitations = InMemoryInvitationRepository(auth)
        val viewModel = inviteViewModel(spaces, invitations)
        viewModel.createInvitation()

        viewModel.revokeInvitation()

        assertNull(viewModel.uiState.value.invitation)
        assertTrue(invitations.observeInvitations("space-1").first().single().revoked)
    }

    @Test
    fun anExpiredOrRevokedInvitationIsNotShown() = runTest {
        val spaces = ownedSpace()
        val now = Clock.System.now()
        val invitations = FailingInvitationRepository(
            listOf(
                invitation("REVOKED2", expiresAt = now + 3.days, revoked = true),
                invitation("EXPIRED2", expiresAt = now - 1.days),
            ),
        )

        assertNull(inviteViewModel(spaces, invitations).uiState.value.invitation)
    }

    @Test
    fun showsTheNewestInvitationThatStillWorks() = runTest {
        val spaces = ownedSpace()
        val now = Clock.System.now()
        val invitations = FailingInvitationRepository(
            listOf(
                invitation("EXPIRED2", expiresAt = now - 1.days),
                invitation("WORKS234", expiresAt = now + 3.days),
                invitation("WORKS567", expiresAt = now + 5.days),
            ),
        )

        assertEquals("WORKS234", inviteViewModel(spaces, invitations).uiState.value.invitation?.code)
    }

    @Test
    fun saysWhenTheInvitationCouldNotBeCreatedAndClearsItOnTheNextAttempt() = runTest {
        val spaces = ownedSpace()
        val viewModel = inviteViewModel(spaces, FailingInvitationRepository())

        viewModel.createInvitation()

        assertEquals(InviteFailure.CREATE, viewModel.uiState.value.failure)
        assertFalse(viewModel.uiState.value.busy)
        assertNull(viewModel.uiState.value.invitation)
    }

    @Test
    fun saysWhenTheInvitationCouldNotBeRevoked() = runTest {
        val spaces = ownedSpace()
        val works = invitation("WORKS234", expiresAt = Clock.System.now() + 3.days)
        val viewModel = inviteViewModel(spaces, FailingInvitationRepository(listOf(works)))

        viewModel.revokeInvitation()

        assertEquals(InviteFailure.REVOKE, viewModel.uiState.value.failure)
        assertFalse(viewModel.uiState.value.busy)
        assertEquals("WORKS234", viewModel.uiState.value.invitation?.code)
    }

    @Test
    fun keepsWorkingWhenTheInvitationsCannotBeLoaded() = runTest {
        val spaces = ownedSpace()

        val state = inviteViewModel(spaces, FailingInvitationRepository(failToLoad = true)).uiState.value

        assertTrue(state.canInvite)
        assertNull(state.invitation)
        assertNull(state.failure)
    }

    @Test
    fun showsTheCardAtOnceAndWaitsForTheInvitationsToArrive() = runTest {
        val spaces = ownedSpace()

        val state = inviteViewModel(spaces, FailingInvitationRepository(neverLoad = true)).uiState.value

        assertTrue(state.canInvite)
        assertTrue(state.loading)
        assertNull(state.invitation)
    }

    @Test
    fun stopsLoadingOnceTheInvitationsArrive() = runTest {
        val spaces = ownedSpace()

        assertFalse(inviteViewModel(spaces, InMemoryInvitationRepository(auth)).uiState.value.loading)
    }

    @Test
    fun offersToCreateWhenTheInvitationsEndWithoutLoading() = runTest {
        val spaces = ownedSpace()

        val state = inviteViewModel(spaces, FailingInvitationRepository(endWithoutLoading = true)).uiState.value

        assertTrue(state.canInvite)
        assertFalse(state.loading)
        assertNull(state.invitation)
    }

    @Test
    fun revokingWithNothingToRevokeDoesNothing() = runTest {
        val spaces = ownedSpace()
        val viewModel = inviteViewModel(spaces, FailingInvitationRepository())

        viewModel.revokeInvitation()

        assertNull(viewModel.uiState.value.failure)
    }
}
