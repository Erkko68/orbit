package com.orbit.app.feature.newspace

import com.orbit.app.core.PendingInvitation
import com.orbit.app.testing.FakeConnectivity
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
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
    fun theAssistantWaitsForAManualWriteToFinish() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = NewSpaceViewModel(FakeSpaceRepository.withMembers(), PendingInvitation(), FakeConnectivity())
        assertTrue(viewModel.uiState.value.canOpenAssistant)

        viewModel.onNameChange("Flat 4B")
        viewModel.createSpace()
        assertFalse(viewModel.uiState.value.canOpenAssistant)
        runCurrent()
        assertTrue(viewModel.uiState.value.createFailed)
        assertTrue(viewModel.uiState.value.canOpenAssistant)

        viewModel.onJoinCodeChange("ABCD2345")
        viewModel.joinSpace()
        assertFalse(viewModel.uiState.value.canOpenAssistant)
        runCurrent()
        assertTrue(viewModel.uiState.value.canOpenAssistant)
    }

    @Test
    fun disablesTheAssistantOfflineAndRestoresItWithTheConnection() = runTest {
        val auth = InMemoryAuthRepository()
        auth.signIn("ana@orbit.app", "secret")
        val spaces = InMemorySpaceRepository(auth)
        val connectivity = FakeConnectivity(online = false)
        val viewModel = NewSpaceViewModel(spaces, PendingInvitation(), connectivity)

        assertFalse(viewModel.uiState.value.online)
        assertFalse(viewModel.uiState.value.canOpenAssistant)

        viewModel.onNameChange("Flat 4B")
        viewModel.createSpace()

        val created = spaces.observeSpaces().first().single()
        assertEquals("Flat 4B", created.name)
        assertEquals(created.id, viewModel.uiState.value.openSpaceId)
        assertFalse(viewModel.uiState.value.canOpenAssistant)

        connectivity.setOnline(true)
        assertTrue(viewModel.uiState.value.online)
        assertTrue(viewModel.uiState.value.canOpenAssistant)

        val broken = NewSpaceViewModel(
            FakeSpaceRepository.withMembers(),
            PendingInvitation(),
            connectivity = { flow { error("Broken observer") } },
        )
        assertTrue(broken.uiState.value.online)
        assertTrue(broken.uiState.value.canOpenAssistant)
    }

    @Test
    fun createsASpaceWithTheNextBadgeAndOpensIt() = runTest {
        val auth = InMemoryAuthRepository()
        auth.signIn("ana@orbit.app", "secret")
        val spaces = InMemorySpaceRepository(auth)
        spaces.createSpace("Studio", accent = 0)
        val viewModel = NewSpaceViewModel(spaces, PendingInvitation(), FakeConnectivity())

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
        val viewModel = NewSpaceViewModel(spaces, PendingInvitation(), FakeConnectivity())

        viewModel.onNameChange("   ")
        viewModel.createSpace()

        assertFalse(viewModel.uiState.value.canCreate)
        assertEquals(emptyList(), spaces.observeSpaces().first())
    }

    @Test
    fun aFailedCreateKeepsTheNameAndIsFlaggedUntilTheNextEdit() = runTest {
        // This fake's createSpace always throws.
        val viewModel = NewSpaceViewModel(FakeSpaceRepository.withMembers(), PendingInvitation(), FakeConnectivity())

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
