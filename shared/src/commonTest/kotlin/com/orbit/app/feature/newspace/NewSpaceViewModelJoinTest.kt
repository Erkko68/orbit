package com.orbit.app.feature.newspace

import com.orbit.app.core.PendingInvitation
import com.orbit.app.domain.model.JoinError
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryInvitationRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NewSpaceViewModelJoinTest {
    private val auth = InMemoryAuthRepository()
    private val invitations = InMemoryInvitationRepository(auth)
    private val spaces = InMemorySpaceRepository(auth, invitations)
    private val pending = PendingInvitation()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newSpaceViewModel(repository: SpaceRepository = spaces) = NewSpaceViewModel(repository, pending)

    /** Ana owns Flat 4B and made an invitation to it. Ben is signed in, outside the space. */
    private suspend fun invitationFromAna(): Pair<String, String> {
        auth.signIn("ana@orbit.app", "secret")
        val spaceId = spaces.createSpace("Flat 4B", accent = 0)
        val code = invitations.createInvitation(spaceId)
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        return spaceId to code
    }

    @Test
    fun joinsWithTheTypedCodeAndOpensTheSpace() = runTest {
        val (spaceId, code) = invitationFromAna()
        val viewModel = newSpaceViewModel()
        viewModel.onJoinCodeChange(code)

        viewModel.joinSpace()

        val state = viewModel.uiState.value
        assertEquals(spaceId, state.openSpaceId)
        assertFalse(state.joining)
        assertNull(state.joinError)
    }

    @Test
    fun joinsWithAPastedLinkInAnyCase() = runTest {
        val (spaceId, code) = invitationFromAna()
        val viewModel = newSpaceViewModel()

        viewModel.onJoinCodeChange("  orbit://join/${code.lowercase()}  ")
        viewModel.joinSpace()

        assertEquals(spaceId, viewModel.uiState.value.openSpaceId)
    }

    @Test
    fun saysWhenTheTextIsNotACodeAndAsksNothing() = runTest {
        invitationFromAna()
        val viewModel = newSpaceViewModel()

        viewModel.onJoinCodeChange("not a code")
        viewModel.joinSpace()

        assertEquals(JoinError.INVALID, viewModel.uiState.value.joinError)
        assertNull(viewModel.uiState.value.openSpaceId)
    }

    @Test
    fun saysWhenNoInvitationHasTheCode() = runTest {
        invitationFromAna()
        val viewModel = newSpaceViewModel()

        viewModel.onJoinCodeChange("ABCD2345")
        viewModel.joinSpace()

        assertEquals(JoinError.INVALID, viewModel.uiState.value.joinError)
        assertFalse(viewModel.uiState.value.joining)
    }

    @Test
    fun saysWhenTheInvitationWasRevoked() = runTest {
        val (_, code) = invitationFromAna()
        auth.signOut()
        auth.signIn("ana@orbit.app", "secret")
        invitations.revokeInvitation(code)
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        val viewModel = newSpaceViewModel()

        viewModel.onJoinCodeChange(code)
        viewModel.joinSpace()

        assertEquals(JoinError.REVOKED, viewModel.uiState.value.joinError)
        assertNull(viewModel.uiState.value.openSpaceId)
    }

    @Test
    fun saysWhenTheInvitationHasExpired() = runTest {
        val longAgo = object : Clock {
            override fun now(): Instant = Clock.System.now() - 8.days
        }
        val oldInvitations = InMemoryInvitationRepository(auth, longAgo)
        val repository = InMemorySpaceRepository(auth, oldInvitations)
        auth.signIn("ana@orbit.app", "secret")
        val code = oldInvitations.createInvitation(repository.createSpace("Flat 4B", accent = 0))
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        val viewModel = newSpaceViewModel(repository)

        viewModel.onJoinCodeChange(code)
        viewModel.joinSpace()

        assertEquals(JoinError.EXPIRED, viewModel.uiState.value.joinError)
    }

    @Test
    fun saysWhenTheUserIsInTheSpaceAlready() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val code = invitations.createInvitation(spaces.createSpace("Flat 4B", accent = 0))
        val viewModel = newSpaceViewModel()

        viewModel.onJoinCodeChange(code)
        viewModel.joinSpace()

        assertEquals(JoinError.ALREADY_MEMBER, viewModel.uiState.value.joinError)
        assertNull(viewModel.uiState.value.openSpaceId)
    }

    @Test
    fun saysSoAndKeepsTheCodeWhenTheJoinFailsForAnyOtherReason() = runTest {
        val (_, code) = invitationFromAna()
        val offline = object : SpaceRepository by spaces {
            override suspend fun joinSpace(code: String): String = error("Could not reach the server")
        }
        val viewModel = newSpaceViewModel(offline)
        viewModel.onJoinCodeChange(code)

        viewModel.joinSpace()

        val state = viewModel.uiState.value
        assertEquals(JoinError.FAILED, state.joinError)
        assertFalse(state.joining)
        assertEquals(code, state.joinCode)
    }

    @Test
    fun editingTheCodeClearsTheError() = runTest {
        invitationFromAna()
        val viewModel = newSpaceViewModel()
        viewModel.onJoinCodeChange("ABCD2345")
        viewModel.joinSpace()

        viewModel.onJoinCodeChange("ABCD234")

        assertNull(viewModel.uiState.value.joinError)
    }

    @Test
    fun cannotJoinWithNothingTyped() = runTest {
        invitationFromAna()
        val viewModel = newSpaceViewModel()

        assertFalse(viewModel.uiState.value.canJoin)
        viewModel.onJoinCodeChange("   ")
        assertFalse(viewModel.uiState.value.canJoin)
        viewModel.joinSpace()

        assertNull(viewModel.uiState.value.joinError)
    }

    @Test
    fun anOpenedLinkComesWithItsCodeAndIsTaken() = runTest {
        pending.offer("orbit://join/ABCD2345")

        val viewModel = newSpaceViewModel()

        assertEquals("ABCD2345", viewModel.uiState.value.joinCode)
        assertNull(pending.code.value)
    }
}
