package com.orbit.app.feature.spacehome

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Schedule
import com.orbit.app.testing.ControlledItemRepository
import com.orbit.app.testing.FlakySpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemoryLastSpaceRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/** The in-memory user's id is their email. */
private const val ANA = "ana@orbit.app"

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceHomeViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val items = InMemoryItemRepository(auth)
    private val lastSpace = InMemoryLastSpaceRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsTheSpaceItsColourAndTheUsersAvatar() = runTest {
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val id = spaces.createSpace("Flat 4B", accent = 2)

        val state = SpaceHomeViewModel(id, spaces, items, lastSpace, auth).uiState.first { it.name.isNotEmpty() }

        assertEquals("Flat 4B", state.name)
        assertEquals(2, state.accent)
        assertEquals("AL", state.userInitials)
    }

    private suspend fun dueItem(title: String, userId: String, due: LocalDate, spaceId: String) = items.create(
        spaceId,
        NewItem(
            title,
            modules = ItemModules(assignment = Assignment.ToMembers(setOf(userId)), schedule = Schedule(due)),
        ),
    )

    @Test
    fun aSpaceWithoutItemsSaysSo() = runTest {
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val viewModel = collected(SpaceHomeViewModel(id, spaces, items, lastSpace, auth))

        assertEquals(SpaceHeadline.Empty, viewModel.uiState.value.headline)
    }

    @Test
    fun countsWhatIsDueForTheUserTodayOrOverdue() = runTest {
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        dueItem("Today", ANA, today, id)
        dueItem("Overdue", ANA, today.minus(1, DateTimeUnit.DAY), id)
        dueItem("Tomorrow", ANA, today.plus(1, DateTimeUnit.DAY), id)
        dueItem("Not mine", "ben", today, id)
        val viewModel = collected(SpaceHomeViewModel(id, spaces, items, lastSpace, auth))

        assertEquals(SpaceHeadline.Due(2), viewModel.uiState.value.headline)
    }

    @Test
    fun allClearOnceNothingIsDue() = runTest {
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val item = dueItem("Today", ANA, today, id)
        val viewModel = collected(SpaceHomeViewModel(id, spaces, items, lastSpace, auth))

        items.complete(id, item, null)

        assertEquals(SpaceHeadline.AllClear, viewModel.uiState.value.headline)
    }

    @Test
    fun opensOnCalendarAndSwitchesDestination() = runTest {
        val viewModel = SpaceHomeViewModel("space-1", spaces, items, lastSpace, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(SpaceHomeTab.CALENDAR, viewModel.uiState.value.tab)

        viewModel.selectTab(SpaceHomeTab.EXPENSES)

        assertEquals(SpaceHomeTab.EXPENSES, viewModel.uiState.value.tab)
        assertFalse(viewModel.uiState.value.canAddItem)

        viewModel.selectTab(SpaceHomeTab.CALENDAR)
        assertTrue(viewModel.uiState.value.canAddItem)
    }

    @Test
    fun hasNoNameForASpaceItCannotSee() = runTest {
        val viewModel = SpaceHomeViewModel("missing", spaces, items, lastSpace, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals("", viewModel.uiState.value.name)
    }

    @Test
    fun remembersTheSpaceItOpensAsTheLastOne() {
        SpaceHomeViewModel("space-1", spaces, items, lastSpace, auth)
        assertEquals("space-1", lastSpace.lastSpaceId())

        SpaceHomeViewModel("space-2", spaces, items, lastSpace, auth)
        assertEquals("space-2", lastSpace.lastSpaceId())
    }

    private fun TestScope.collected(viewModel: SpaceHomeViewModel) = viewModel.also {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect {} }
    }

    @Test
    fun leavesTheHeaderEmptyWhenItsReadsFail() = runTest {
        val failingItems = ControlledItemRepository(items).apply { beforeRead = { error("Could not reach the server") } }
        val spaces = FlakySpaceRepository(spaceFailures = 1, memberFailures = 1)
        val viewModel = SpaceHomeViewModel("space-1", spaces, failingItems, lastSpace, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals("", state.name)
        assertNull(state.accent)
        assertNull(state.headline)
    }
}
