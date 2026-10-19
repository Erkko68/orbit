package com.orbit.app.feature.spacehome.items

import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
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
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.item_type_event
import orbit.shared.generated.resources.item_type_list
import orbit.shared.generated.resources.item_type_other
import orbit.shared.generated.resources.item_type_task
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceItemsViewModelTest {
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
    fun listsItemsByType() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        items.create(id, NewItem("Book plumber", preset = Preset.TASK))
        items.create(id, NewItem("Groceries", preset = Preset.SHOPPING_LIST))
        items.create(id, NewItem("Dinner at 20:00", preset = Preset.EVENT))
        items.create(id, NewItem("Take out recycling"))

        val state = SpaceItemsViewModel(id, items).uiState.first { it.items.size == 4 }

        assertEquals(
            listOf(Res.string.item_type_task, Res.string.item_type_list, Res.string.item_type_event, Res.string.item_type_other),
            state.items.map { it.label },
        )
    }

    @Test
    fun filtersItemsWithTheSelectedChip() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        items.create(id, NewItem("Book plumber", preset = Preset.TASK))
        items.create(id, NewItem("Groceries", preset = Preset.SHOPPING_LIST))
        items.create(id, NewItem("Dinner at 20:00", preset = Preset.EVENT))
        val viewModel = SpaceItemsViewModel(id, items)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val titlesByFilter = ItemFilter.entries.associateWith { filter ->
            viewModel.selectFilter(filter)
            viewModel.uiState.value.also { assertEquals(filter, it.filter) }.items.map { it.title }
        }

        assertEquals(
            mapOf(
                ItemFilter.ALL to listOf("Book plumber", "Groceries", "Dinner at 20:00"),
                ItemFilter.TASKS to listOf("Book plumber"),
                ItemFilter.LISTS to listOf("Groceries"),
                ItemFilter.EVENTS to listOf("Dinner at 20:00"),
            ),
            titlesByFilter,
        )
    }

    @Test
    fun saysWhenTheItemsFailToLoadAndLoadsThemOnRetry() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        items.create(id, NewItem("Book plumber", preset = Preset.TASK))
        val controlled = ControlledItemRepository(items).apply { beforeRead = { error("Could not reach the server") } }
        val viewModel = SpaceItemsViewModel(id, controlled)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.loadFailed)

        controlled.beforeRead = {}
        viewModel.retry()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals(listOf("Book plumber"), viewModel.uiState.value.items.map { it.title })
    }

    @Test
    fun completesAndReopensItem() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val itemId = items.create(id, NewItem("Book plumber", preset = Preset.TASK))
        val viewModel = SpaceItemsViewModel(id, items)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val initial = viewModel.uiState.first { it.items.isNotEmpty() }
        assertFalse(initial.items.single().done)

        viewModel.complete(itemId)
        val completed = viewModel.uiState.first { it.items.single().done }
        assertTrue(completed.items.single().done)

        viewModel.reopen(itemId)
        val reopened = viewModel.uiState.first { !it.items.single().done }
        assertFalse(reopened.items.single().done)
    }

    @Test
    fun movesDoneItemsUnderOpenItems() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val firstId = items.create(id, NewItem("First task", preset = Preset.TASK))
        items.create(id, NewItem("Second task", preset = Preset.TASK))
        items.create(id, NewItem("Third task", preset = Preset.TASK))
        val viewModel = SpaceItemsViewModel(id, items)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.complete(firstId)
        val state = viewModel.uiState.first { it.items.size == 3 && it.items.last().done }

        assertEquals(listOf("Second task", "Third task", "First task"), state.items.map { it.title })
        assertEquals(listOf(false, false, true), state.items.map { it.done })
    }

    @Test
    fun flagsFailureWhenCompleteFails() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val itemId = items.create(id, NewItem("Book plumber", preset = Preset.TASK))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val viewModel = SpaceItemsViewModel(id, controlled)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.complete(itemId)
        val state = viewModel.uiState.first { it.failed }
        assertTrue(state.failed)
        assertFalse(state.submitting)
    }

    @Test
    fun flagsFailureWhenReopenFails() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val itemId = items.create(id, NewItem("Book plumber", preset = Preset.TASK))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val viewModel = SpaceItemsViewModel(id, controlled)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.reopen(itemId)
        val state = viewModel.uiState.first { it.failed }
        assertTrue(state.failed)
        assertFalse(state.submitting)
    }

    @Test
    fun cannotSubmitWhileAlreadySubmitting() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val itemId = items.create(id, NewItem("Book plumber", preset = Preset.TASK))
        var writeCalls = 0
        val controlled = ControlledItemRepository(items).apply {
            beforeWrite = { writeCalls++ }
        }
        val viewModel = SpaceItemsViewModel(id, controlled)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.complete(itemId)
        assertEquals(1, writeCalls)
        assertFalse(viewModel.uiState.value.submitting)
    }
}
