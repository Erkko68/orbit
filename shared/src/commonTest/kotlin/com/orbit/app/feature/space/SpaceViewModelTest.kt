package com.orbit.app.feature.space

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.testing.FakeSpaceRepository
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
    fun createsASpaceForTheSignedInUser() = runTest {
        val auth = InMemoryAuthRepository()
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth))

        viewModel.onNewSpaceNameChange("  Flat 4B ")
        viewModel.createSpace()

        val state = viewModel.uiState.first { it.spaceIds.isNotEmpty() }
        assertEquals(1, state.spaceIds.size)
        // The form is ready for the next space.
        assertEquals("", state.newSpaceName)
        assertFalse(state.canCreate)
    }

    @Test
    fun createsOnlyWithAName() = runTest {
        val auth = InMemoryAuthRepository()
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onNewSpaceNameChange("   ")
        viewModel.createSpace()

        assertFalse(viewModel.uiState.value.canCreate)
        assertEquals(emptyList(), viewModel.uiState.value.spaceIds)
    }

    @Test
    fun aFailedCreateKeepsTheNameAndIsFlaggedUntilTheNextEdit() = runTest {
        // This fake's createSpace always throws.
        val viewModel = SpaceViewModel(FakeSpaceRepository.withMembers())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onNewSpaceNameChange("Flat 4B")
        viewModel.createSpace()

        val failed = viewModel.uiState.value
        assertEquals("Flat 4B", failed.newSpaceName)
        assertTrue(failed.createFailed)
        assertTrue(failed.canCreate)

        viewModel.onNewSpaceNameChange("Flat 4C")
        assertFalse(viewModel.uiState.value.createFailed)
    }
}
