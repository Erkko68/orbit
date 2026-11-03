package com.orbit.app.feature.space

import com.orbit.app.core.PendingInvitation
import com.orbit.app.testing.FlakySpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsLoadingThenShowsTheSpaces() = runTest {
        val auth = InMemoryAuthRepository()
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth), auth, PendingInvitation())
        assertTrue(viewModel.uiState.value.loading)

        val loaded = viewModel.uiState.first { !it.loading }

        assertFalse(loaded.loadFailed)
        assertEquals(emptyList(), loaded.spaceIds)
    }

    @Test
    fun flagsAFailedLoadAndRetriesIt() = runTest {
        val viewModel = SpaceViewModel(
            FlakySpaceRepository(spaceFailures = 1),
            InMemoryAuthRepository(),
            PendingInvitation(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertTrue(viewModel.uiState.value.loadFailed)
        assertFalse(viewModel.uiState.value.loading)

        viewModel.retry()

        val loaded = viewModel.uiState.value
        assertFalse(loaded.loadFailed)
        assertFalse(loaded.loading)
        assertEquals(listOf("space-1"), loaded.spaceIds)
    }

    @Test
    fun aFailedLoadKeepsTheAvatar() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val viewModel = SpaceViewModel(FlakySpaceRepository(spaceFailures = 1), auth, PendingInvitation())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val failed = viewModel.uiState.value
        assertTrue(failed.loadFailed)
        assertEquals("AL", failed.userInitials)
    }

    @Test
    fun saysWhileAnOpenedLinkWaitsToBeTaken() = runTest {
        val pending = PendingInvitation()
        val auth = InMemoryAuthRepository()
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth), auth, pending)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertFalse(viewModel.uiState.value.invitationWaiting)

        pending.offer("orbit://join/ABCD2345")
        assertTrue(viewModel.uiState.value.invitationWaiting)

        pending.consume()
        assertFalse(viewModel.uiState.value.invitationWaiting)
    }
}
