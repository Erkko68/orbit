package com.orbit.app.feature.space.card

import com.orbit.app.testing.FlakySpaceRepository
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Schedule
import com.orbit.app.testing.ControlledItemRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceCardViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val items = InMemoryItemRepository(auth)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsItsSpace() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        spaces.createSpace("Garage", accent = 1)
        val id = spaces.createSpace("Flat 4B", accent = 2)

        val state = SpaceCardViewModel(id, spaces, items, auth).uiState.first { it.name.isNotEmpty() }

        assertEquals("Flat 4B", state.name)
        assertEquals(2, state.accent)
        assertEquals(1, state.memberCount)
        assertEquals(listOf("A"), state.faces.map { it.initials })
    }

    @Test
    fun staysEmptyForASpaceItCannotSee() = runTest {
        val viewModel = SpaceCardViewModel("missing", spaces, items, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(SpaceCardUiState(), viewModel.uiState.value)
    }

    @Test
    fun showsItsSpaceWithoutAvatarsWhenTheMembersFailToLoad() = runTest {
        val viewModel = SpaceCardViewModel("space-1", FlakySpaceRepository(memberFailures = 1), items, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals("Flat 4B", viewModel.uiState.value.name)
        assertEquals(emptyList(), viewModel.uiState.value.faces.map { it.initials })
    }

    @Test
    fun saysWhatIsDueForTheUserAndWhatIsOpen() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        items.create(
            id,
            NewItem(
                "Mine today",
                modules = ItemModules(assignment = Assignment.ToMembers(setOf("ana@orbit.app")), schedule = Schedule(today)),
            ),
        )
        items.create(id, NewItem("Nobody's"))
        val viewModel = SpaceCardViewModel(id, spaces, items, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(1, viewModel.uiState.value.dueToday)
        assertEquals(2, viewModel.uiState.value.openCount)
    }

    @Test
    fun saysNothingAboutTheItemsWhenTheyCannotBeRead() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val failing = ControlledItemRepository(items).apply { beforeRead = { error("Could not reach the server") } }
        val viewModel = SpaceCardViewModel(id, spaces, failing, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals("Flat 4B", viewModel.uiState.value.name)
        assertNull(viewModel.uiState.value.dueToday)
        assertNull(viewModel.uiState.value.openCount)
    }
}
