package com.orbit.app.feature.spacehome

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemoryItemRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.role_admin
import orbit.shared.generated.resources.role_member
import orbit.shared.generated.resources.role_owner
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
    fun showsTheSpaceAndItsOwner() = runTest {
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val id = spaces.createSpace("Flat 4B", accent = 0)

        val state = SpaceHomeViewModel(id, spaces, items).uiState.first { it.members.isNotEmpty() }

        assertEquals("Flat 4B", state.name)
        assertEquals(listOf(SpaceMember("ana@orbit.app", "Ana López", "AL", Res.string.role_owner)), state.members)
    }

    @Test
    fun labelsEveryRole() = runTest {
        val repository = FakeSpaceRepository.withMembers("Ana" to Role.OWNER, "Ben" to Role.ADMIN, "Cy" to Role.MEMBER)

        val state = SpaceHomeViewModel("space-1", repository, items).uiState.first { it.members.isNotEmpty() }

        assertEquals(
            listOf(Res.string.role_owner, Res.string.role_admin, Res.string.role_member),
            state.members.map { it.role },
        )
    }

    @Test
    fun listsItemsByTypeAndCountsOpenTasks() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        items.create(id, NewItem("Book plumber", preset = Preset.TASK))
        items.create(id, NewItem("Groceries", preset = Preset.SHOPPING_LIST))
        items.create(id, NewItem("Dinner at 20:00", preset = Preset.EVENT))
        val done = items.create(id, NewItem("Take out recycling"))
        items.complete(id, done)

        val state = SpaceHomeViewModel(id, spaces, items).uiState.first { it.items.size == 4 }

        assertEquals(
            listOf(ItemType.TASK, ItemType.LIST, ItemType.EVENT, ItemType.OTHER),
            state.items.map { it.type },
        )
        assertEquals(1, state.openTasks)
    }

    @Test
    fun filtersItemsWithTheSelectedChip() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        items.create(id, NewItem("Book plumber", preset = Preset.TASK))
        items.create(id, NewItem("Groceries", preset = Preset.SHOPPING_LIST))
        items.create(id, NewItem("Dinner at 20:00", preset = Preset.EVENT))
        val viewModel = SpaceHomeViewModel(id, spaces, items)

        val titlesByFilter = ItemFilter.entries.associateWith { filter ->
            viewModel.select(filter)
            viewModel.uiState.first { it.filter == filter && it.name.isNotEmpty() }.items.map { it.title }
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

    @Test
    fun hasNoNameForASpaceItCannotSee() = runTest {
        val viewModel = SpaceHomeViewModel("missing", spaces, items)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals("", viewModel.uiState.value.name)
    }

    @Test
    fun initialsTakeTheFirstLettersOfUpToTwoWords() {
        assertEquals("AL", initialsOf("Ana López"))
        assertEquals("AL", initialsOf("  ana  lópez  garcía "))
        assertEquals("Y", initialsOf("you"))
        assertEquals("", initialsOf(""))
    }
}
