package com.orbit.app.feature.space

import com.orbit.app.testing.FakeSpaceRepository
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
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth), auth)
        assertTrue(viewModel.uiState.value.loading)

        val loaded = viewModel.uiState.first { !it.loading }

        assertFalse(loaded.loadFailed)
        assertEquals(emptyList(), loaded.spaceIds)
    }

    @Test
    fun flagsAFailedLoadAndRetriesIt() = runTest {
        val viewModel = SpaceViewModel(FlakySpaceRepository(spaceFailures = 1), InMemoryAuthRepository())
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
    fun aFailedLoadKeepsTheAvatarAndTheOpenDialog() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val viewModel = SpaceViewModel(FlakySpaceRepository(spaceFailures = 1), auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.openCreate()

        val failed = viewModel.uiState.value
        assertTrue(failed.loadFailed)
        assertEquals("AL", failed.userInitials)
        assertTrue(failed.createOpen)
    }

    @Test
    fun createsASpaceForTheSignedInUser() = runTest {
        val auth = InMemoryAuthRepository()
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth), auth)

        viewModel.openCreate()
        viewModel.onNewSpaceNameChange("  Flat 4B ")
        viewModel.createSpace()

        val state = viewModel.uiState.first { it.spaceIds.isNotEmpty() }
        assertEquals(1, state.spaceIds.size)
        assertEquals("A", state.userInitials)
        // The dialog is closed and ready for the next space.
        assertFalse(state.createOpen)
        assertEquals("", state.newSpaceName)
        assertFalse(state.canCreate)
    }

    @Test
    fun opensTheCreateDialogAndForgetsItsNameWhenDismissed() = runTest {
        val auth = InMemoryAuthRepository()
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth), auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.openCreate()
        viewModel.onNewSpaceNameChange("Flat 4B")
        assertTrue(viewModel.uiState.value.createOpen)

        viewModel.dismissCreate()

        assertEquals(SpaceUiState(), viewModel.uiState.value)
    }

    @Test
    fun createsOnlyWithAName() = runTest {
        val auth = InMemoryAuthRepository()
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth), auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onNewSpaceNameChange("   ")
        viewModel.createSpace()

        assertFalse(viewModel.uiState.value.canCreate)
        assertEquals(emptyList(), viewModel.uiState.value.spaceIds)
    }

    @Test
    fun aFailedCreateKeepsTheNameAndIsFlaggedUntilTheNextEdit() = runTest {
        // This fake's createSpace always throws.
        val viewModel = SpaceViewModel(FakeSpaceRepository.withMembers(), InMemoryAuthRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.openCreate()
        viewModel.onNewSpaceNameChange("Flat 4B")
        viewModel.createSpace()

        val failed = viewModel.uiState.value
        assertTrue(failed.createOpen)
        assertEquals("Flat 4B", failed.newSpaceName)
        assertTrue(failed.createFailed)
        assertTrue(failed.canCreate)

        viewModel.onNewSpaceNameChange("Flat 4C")
        assertFalse(viewModel.uiState.value.createFailed)
    }
}
