package com.orbit.app.feature.newspace

import com.orbit.app.core.PendingInvitation
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalCoroutinesApi::class)
class NewSpaceViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createsASpaceWithTheNextBadgeAndOpensIt() = runTest {
        val auth = InMemoryAuthRepository()
        auth.signIn("ana@orbit.app", "secret")
        val spaces = InMemorySpaceRepository(auth)
        spaces.createSpace("Studio", accent = 0)
        val viewModel = NewSpaceViewModel(spaces, PendingInvitation())

        viewModel.onNameChange("  Flat 4B ")
        viewModel.createSpace()

        val created = spaces.observeSpaces().first().single { it.name != "Studio" }
        assertEquals("Flat 4B", created.name)
        assertEquals(1, created.accent)
        assertEquals(created.id, viewModel.uiState.value.openSpaceId)
        assertFalse(viewModel.uiState.value.creating)
    }

    @Test
    fun createsOnlyWithAName() = runTest {
        val auth = InMemoryAuthRepository()
        auth.signIn("ana@orbit.app", "secret")
        val spaces = InMemorySpaceRepository(auth)
        val viewModel = NewSpaceViewModel(spaces, PendingInvitation())

        viewModel.onNameChange("   ")
        viewModel.createSpace()

        assertFalse(viewModel.uiState.value.canCreate)
        assertEquals(emptyList(), spaces.observeSpaces().first())
    }

    @Test
    fun aFailedCreateKeepsTheNameAndIsFlaggedUntilTheNextEdit() = runTest {
        // This fake's createSpace always throws.
        val viewModel = NewSpaceViewModel(FakeSpaceRepository.withMembers(), PendingInvitation())

        viewModel.onNameChange("Flat 4B")
        viewModel.createSpace()

        val failed = viewModel.uiState.value
        assertEquals("Flat 4B", failed.name)
        assertTrue(failed.createFailed)
        assertTrue(failed.canCreate)
        assertNull(failed.openSpaceId)

        viewModel.onNameChange("Flat 4C")
        assertFalse(viewModel.uiState.value.createFailed)
    }
}
