package com.orbit.app.feature.spacehome

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemoryItemRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
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
class SpaceHomeViewModelTest {
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
    fun showsTheSpaceAndItsMembersInitials() = runTest {
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val id = spaces.createSpace("Flat 4B", accent = 0)

        val state = SpaceHomeViewModel(id, spaces, items).uiState.first { it.memberInitials.isNotEmpty() }

        assertEquals("Flat 4B", state.name)
        assertEquals(listOf("AL"), state.memberInitials)
    }

    @Test
    fun countsOpenTasks() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        items.create(id, NewItem("Book plumber", preset = Preset.TASK))
        items.create(id, NewItem("Call the landlord"))
        items.create(id, NewItem("Groceries", preset = Preset.SHOPPING_LIST))
        val done = items.create(id, NewItem("Take out recycling"))
        items.complete(id, done)

        val state = SpaceHomeViewModel(id, spaces, items).uiState.first { it.openTasks > 0 }

        assertEquals(2, state.openTasks)
    }

    @Test
    fun opensOnItemsAndSwitchesTab() = runTest {
        val viewModel = SpaceHomeViewModel("space-1", spaces, items)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(SpaceHomeTab.ITEMS, viewModel.uiState.value.tab)

        viewModel.selectTab(SpaceHomeTab.MEMBERS)

        assertEquals(SpaceHomeTab.MEMBERS, viewModel.uiState.value.tab)
    }

    @Test
    fun hasNoNameForASpaceItCannotSee() = runTest {
        val viewModel = SpaceHomeViewModel("missing", spaces, items)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals("", viewModel.uiState.value.name)
    }
}
