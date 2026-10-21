package com.orbit.app.feature.space

import com.orbit.app.core.PendingInvitation
import com.orbit.app.domain.model.JoinError
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryInvitationRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceViewModelJoinTest {
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

    private fun TestScope.spaceViewModel(repository: SpaceRepository = spaces) =
        SpaceViewModel(repository, auth, pending).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        }

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
    fun joinsWithTheTypedCodeAndClosesTheDialog() = runTest {
        val (spaceId, code) = invitationFromAna()
        val viewModel = spaceViewModel()
        viewModel.openCreate()
        viewModel.onJoinCodeChange(code)

        viewModel.joinSpace()

        val state = viewModel.uiState.value
        assertEquals(listOf(spaceId), state.spaceIds)
        assertFalse(state.createOpen)
        assertFalse(state.joining)
        assertNull(state.joinError)
        assertEquals("", state.joinCode)
    }

    @Test
    fun joinsWithAPastedLinkInAnyCase() = runTest {
        val (spaceId, code) = invitationFromAna()
        val viewModel = spaceViewModel()

        viewModel.onJoinCodeChange("  orbit://join/${code.lowercase()}  ")
        viewModel.joinSpace()

        assertEquals(listOf(spaceId), viewModel.uiState.value.spaceIds)
    }

    @Test
    fun saysWhenTheTextIsNotACodeAndAsksNothing() = runTest {
        invitationFromAna()
        val viewModel = spaceViewModel()
        viewModel.openCreate()

        viewModel.onJoinCodeChange("not a code")
        viewModel.joinSpace()

        assertEquals(JoinError.INVALID, viewModel.uiState.value.joinError)
        assertTrue(viewModel.uiState.value.createOpen)
        assertTrue(viewModel.uiState.value.spaceIds.isEmpty())
    }

    @Test
    fun saysWhenNoInvitationHasTheCode() = runTest {
        invitationFromAna()
        val viewModel = spaceViewModel()

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
        val viewModel = spaceViewModel()

        viewModel.onJoinCodeChange(code)
        viewModel.joinSpace()

        assertEquals(JoinError.REVOKED, viewModel.uiState.value.joinError)
        assertTrue(viewModel.uiState.value.spaceIds.isEmpty())
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
        val viewModel = spaceViewModel(repository)

        viewModel.onJoinCodeChange(code)
        viewModel.joinSpace()

        assertEquals(JoinError.EXPIRED, viewModel.uiState.value.joinError)
    }

    @Test
    fun saysWhenTheUserIsInTheSpaceAlready() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val code = invitations.createInvitation(spaces.createSpace("Flat 4B", accent = 0))
        val viewModel = spaceViewModel()

        viewModel.onJoinCodeChange(code)
        viewModel.joinSpace()

        assertEquals(JoinError.ALREADY_MEMBER, viewModel.uiState.value.joinError)
        assertEquals(1, viewModel.uiState.value.spaceIds.size)
    }

    @Test
    fun saysSoAndKeepsTheDialogWhenTheJoinFailsForAnyOtherReason() = runTest {
        val (_, code) = invitationFromAna()
        val offline = object : SpaceRepository by spaces {
            override suspend fun joinSpace(code: String): String = error("Could not reach the server")
        }
        val viewModel = spaceViewModel(offline)
        viewModel.openCreate()
        viewModel.onJoinCodeChange(code)

        viewModel.joinSpace()

        val state = viewModel.uiState.value
        assertEquals(JoinError.FAILED, state.joinError)
        assertFalse(state.joining)
        assertTrue(state.createOpen)
        assertEquals(code, state.joinCode)
    }

    @Test
    fun editingTheCodeClearsTheError() = runTest {
        invitationFromAna()
        val viewModel = spaceViewModel()
        viewModel.onJoinCodeChange("ABCD2345")
        viewModel.joinSpace()

        viewModel.onJoinCodeChange("ABCD234")

        assertNull(viewModel.uiState.value.joinError)
    }

    @Test
    fun cannotJoinWithNothingTyped() = runTest {
        invitationFromAna()
        val viewModel = spaceViewModel()

        assertFalse(viewModel.uiState.value.canJoin)
        viewModel.onJoinCodeChange("   ")
        assertFalse(viewModel.uiState.value.canJoin)
        viewModel.joinSpace()

        assertNull(viewModel.uiState.value.joinError)
    }

    @Test
    fun anOpenedLinkWaitsUntilTheScreenTakesItAndThenBringsUpTheDialog() = runTest {
        val viewModel = spaceViewModel()
        assertFalse(viewModel.uiState.value.invitationWaiting)

        pending.offer("orbit://join/ABCD2345")

        assertTrue(viewModel.uiState.value.invitationWaiting)
        assertFalse(viewModel.uiState.value.createOpen)

        viewModel.openPendingInvitation()

        val state = viewModel.uiState.value
        assertTrue(state.createOpen)
        assertEquals("ABCD2345", state.joinCode)
        assertFalse(state.invitationWaiting)
        assertNull(pending.code.value)
    }

    @Test
    fun aLinkOpenedBeforeTheSpacesAreShownWaitsForThem() = runTest {
        pending.offer("orbit://join/ABCD2345")

        val viewModel = spaceViewModel()
        assertTrue(viewModel.uiState.value.invitationWaiting)
        viewModel.openPendingInvitation()

        assertTrue(viewModel.uiState.value.createOpen)
        assertEquals("ABCD2345", viewModel.uiState.value.joinCode)
    }

    @Test
    fun takingAnInvitationWhenNoneWaitsDoesNothing() = runTest {
        val viewModel = spaceViewModel()

        viewModel.openPendingInvitation()

        assertFalse(viewModel.uiState.value.createOpen)
        assertEquals("", viewModel.uiState.value.joinCode)
    }
}
