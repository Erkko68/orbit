package com.orbit.app.feature.spacehome.calendar

import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Schedule
import com.orbit.app.testing.ControlledItemRepository
import com.orbit.app.testing.FailingItemRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
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
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceCalendarViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val items = InMemoryItemRepository(auth)
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val firstOfMonth = LocalDate(today.year, today.month, 1)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun space(): String {
        auth.signIn("ana@orbit.app", "secret")
        return spaces.createSpace("Flat 4B", accent = 0)
    }

    private suspend fun dated(spaceId: String, title: String, date: LocalDate, endDate: LocalDate? = null) =
        items.create(spaceId, NewItem(title, modules = ItemModules(schedule = Schedule(date, endDate = endDate))))

    private fun TestScope.watch(viewModel: SpaceCalendarViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

    @Test
    fun opensOnTheMonthWithTodayPickedAndItsDaysMarked() = runTest {
        val id = space()
        dated(id, "Bins", today)
        dated(id, "Trip", firstOfMonth, endDate = firstOfMonth.plus(1, DateTimeUnit.DAY))
        items.create(id, NewItem("Undated"))
        val viewModel = SpaceCalendarViewModel(id, items, spaces)
        watch(viewModel)

        val state = viewModel.uiState.first { !it.loading }
        assertEquals(CalendarView.MONTH, state.view)
        assertEquals(firstOfMonth, state.month)
        assertEquals(today, state.selected)
        assertEquals(setOf(today, firstOfMonth, firstOfMonth.plus(1, DateTimeUnit.DAY)), state.marked)
        assertTrue("Bins" in state.selectedItems.map { it.title })
    }

    @Test
    fun pickingADayListsWhatFallsOnIt() = runTest {
        val id = space()
        val later = today.plus(40, DateTimeUnit.DAY)
        dated(id, "Dentist", later)
        val viewModel = SpaceCalendarViewModel(id, items, spaces)
        watch(viewModel)

        // The month is loaded one at a time: the day is marked once its month shows.
        while (viewModel.uiState.value.month != LocalDate(later.year, later.month, 1)) viewModel.nextMonth()
        assertTrue(later in viewModel.uiState.value.marked)

        viewModel.selectDay(later)
        assertEquals(listOf("Dentist"), viewModel.uiState.value.selectedItems.map { it.title })
    }

    @Test
    fun changingTheMonthPicksItsFirstDayOrToday() = runTest {
        val viewModel = SpaceCalendarViewModel(space(), items, spaces)
        watch(viewModel)

        viewModel.nextMonth()
        val next = firstOfMonth.plus(1, DateTimeUnit.MONTH)
        assertEquals(next, viewModel.uiState.value.month)
        assertEquals(next, viewModel.uiState.value.selected)

        viewModel.previousMonth()
        assertEquals(today, viewModel.uiState.value.selected)
        viewModel.previousMonth()
        assertEquals(firstOfMonth.minus(1, DateTimeUnit.MONTH), viewModel.uiState.value.selected)
    }

    @Test
    fun theListGoesByDayOverdueFirst() = runTest {
        val id = space()
        dated(id, "Late", today.minus(3, DateTimeUnit.DAY))
        dated(id, "Now", today)
        val viewModel = SpaceCalendarViewModel(id, items, spaces)
        watch(viewModel)

        viewModel.selectView(CalendarView.LIST)

        val state = viewModel.uiState.value
        assertEquals(CalendarView.LIST, state.view)
        assertEquals(listOf("Late"), state.overdue.map { it.title })
        assertEquals(listOf(today to listOf("Now")), state.days.map { day -> day.date to day.items.map { it.title } })
        assertFalse(state.agendaEmpty)
    }

    @Test
    fun completingAnItemTakesItOffTheCalendar() = runTest {
        val id = space()
        dated(id, "Bins", today)
        val viewModel = SpaceCalendarViewModel(id, items, spaces)
        watch(viewModel)
        val bins = viewModel.uiState.first { it.selectedItems.isNotEmpty() }.selectedItems.single().id

        viewModel.complete(bins)

        val state = viewModel.uiState.first { it.selectedItems.isEmpty() }
        assertTrue(state.marked.isEmpty())
        assertFalse(state.failed)
    }

    @Test
    fun aFailedCompleteIsFlagged() = runTest {
        val id = space()
        dated(id, "Bins", today)
        val stored = items.observeItems(id).first()
        val viewModel = SpaceCalendarViewModel(id, FailingItemRepository(items = stored), spaces)
        watch(viewModel)

        viewModel.complete(stored.single().id)

        assertTrue(viewModel.uiState.value.failed)
        assertFalse(viewModel.uiState.value.submitting)
    }

    @Test
    fun saysWhenTheItemsFailToLoadAndLoadsThemOnRetry() = runTest {
        val id = space()
        dated(id, "Bins", today)
        val controlled = ControlledItemRepository(items).apply { beforeRead = { error("Could not reach the server") } }
        val viewModel = SpaceCalendarViewModel(id, controlled, spaces)
        watch(viewModel)
        assertTrue(viewModel.uiState.value.loadFailed)

        controlled.beforeRead = {}
        viewModel.retry()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals(listOf("Bins"), viewModel.uiState.value.selectedItems.map { it.title })
    }
}
