package com.orbit.app.feature.spacehome

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemoryItemRepository
import com.orbit.app.data.repository.InMemoryLastSpaceRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.defaultModules
import com.orbit.app.testing.FailingItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

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
    fun showsTheSpaceAndItsMembersInitials() = runTest {
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val id = spaces.createSpace("Flat 4B", accent = 0)

        val state = SpaceHomeViewModel(id, spaces, items, lastSpace).uiState.first { it.memberInitials.isNotEmpty() }

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

        val state = SpaceHomeViewModel(id, spaces, items, lastSpace).uiState.first { it.openTasks > 0 }

        assertEquals(2, state.openTasks)
    }

    @Test
    fun opensOnItemsAndSwitchesTab() = runTest {
        val viewModel = SpaceHomeViewModel("space-1", spaces, items, lastSpace)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(SpaceHomeTab.ITEMS, viewModel.uiState.value.tab)

        viewModel.selectTab(SpaceHomeTab.MEMBERS)

        assertEquals(SpaceHomeTab.MEMBERS, viewModel.uiState.value.tab)
    }

    @Test
    fun hasNoNameForASpaceItCannotSee() = runTest {
        val viewModel = SpaceHomeViewModel("missing", spaces, items, lastSpace)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals("", viewModel.uiState.value.name)
    }

    @Test
    fun remembersTheSpaceItOpensAsTheLastOne() {
        SpaceHomeViewModel("space-1", spaces, items, lastSpace)
        assertEquals("space-1", lastSpace.lastSpaceId())

        SpaceHomeViewModel("space-2", spaces, items, lastSpace)
        assertEquals("space-2", lastSpace.lastSpaceId())
    }

    @Test
    fun createsTheItemWithItsPresetDefaultsAndAsksToOpenIt() = runTest {
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val viewModel = collected(SpaceHomeViewModel("space-1", spaces, items, lastSpace))

        viewModel.openNewItem()
        viewModel.selectNewItemPreset(Preset.SHOPPING_LIST)
        viewModel.onNewItemTitleChange("  Groceries  ")
        viewModel.createItem()

        val created = items.observeItems("space-1").first().single()
        assertEquals("Groceries", created.title)
        assertEquals(Preset.SHOPPING_LIST, created.preset)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        assertEquals(Preset.SHOPPING_LIST.defaultModules(today), created.modules)
        val state = viewModel.uiState.value
        assertEquals(created.id, state.createdItemId)
        assertFalse(state.newItemOpen)
        assertEquals("", state.newItemTitle)
        assertEquals(Preset.TASK, state.newItemPreset)

        viewModel.onCreatedItemOpened()
        assertNull(viewModel.uiState.value.createdItemId)
    }

    @Test
    fun aFailedCreateKeepsTheDialogAndIsClearedByTheNextEdit() = runTest {
        val viewModel = collected(SpaceHomeViewModel("space-1", spaces, FailingItemRepository(), lastSpace))
        viewModel.openNewItem()
        viewModel.onNewItemTitleChange("Book plumber")

        viewModel.createItem()

        val failed = viewModel.uiState.value
        assertTrue(failed.createFailed)
        assertTrue(failed.newItemOpen)
        assertFalse(failed.creating)
        assertEquals("Book plumber", failed.newItemTitle)
        assertNull(failed.createdItemId)

        viewModel.onNewItemTitleChange("Book a plumber")
        assertFalse(viewModel.uiState.value.createFailed)
    }

    @Test
    fun cannotCreateWithoutAName() = runTest {
        val viewModel = collected(SpaceHomeViewModel("space-1", spaces, items, lastSpace))
        viewModel.openNewItem()
        viewModel.onNewItemTitleChange("   ")
        assertFalse(viewModel.uiState.value.canCreate)

        viewModel.createItem()

        assertFalse(viewModel.uiState.value.creating)
        assertTrue(items.observeItems("space-1").first().isEmpty())
    }

    @Test
    fun dismissingForgetsTheDialog() = runTest {
        val viewModel = collected(SpaceHomeViewModel("space-1", spaces, items, lastSpace))
        viewModel.openNewItem()
        viewModel.selectNewItemPreset(Preset.EVENT)
        viewModel.onNewItemTitleChange("Dinner")

        viewModel.dismissNewItem()

        val state = viewModel.uiState.value
        assertFalse(state.newItemOpen)
        assertEquals("", state.newItemTitle)
        assertEquals(Preset.TASK, state.newItemPreset)
    }

    private fun TestScope.collected(viewModel: SpaceHomeViewModel) = viewModel.also {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect {} }
    }
}
