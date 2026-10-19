package com.orbit.app.feature.spacehome.items

import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
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
            listOf(ItemType.TASK, ItemType.LIST, ItemType.EVENT, ItemType.OTHER),
            state.items.map { it.type },
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
                ItemFilter.CHORES to emptyList(),
                ItemFilter.BILLS to emptyList(),
                ItemFilter.LISTS to listOf("Groceries"),
                ItemFilter.EVENTS to listOf("Dinner at 20:00"),
            ),
            titlesByFilter,
        )
    }
}
