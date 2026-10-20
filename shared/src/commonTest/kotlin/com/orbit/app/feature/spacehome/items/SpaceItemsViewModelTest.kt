package com.orbit.app.feature.spacehome.items

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.ControlledItemRepository
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.item_type_event
import orbit.shared.generated.resources.item_type_list
import orbit.shared.generated.resources.item_type_other
import orbit.shared.generated.resources.item_type_task
import orbit.shared.generated.resources.item_unknown_member
import orbit.shared.generated.resources.module_assignment_anyone
import orbit.shared.generated.resources.module_assignment_unassigned


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

        val state = SpaceItemsViewModel(id, items, spaces).uiState.first { it.items.size == 4 }

        assertEquals(
            listOf(
                Res.string.item_type_task,
                Res.string.item_type_list,
                Res.string.item_type_event,
                Res.string.item_type_other
            ),
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
        val viewModel = SpaceItemsViewModel(id, items, spaces)
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
        val viewModel = SpaceItemsViewModel(id, controlled, spaces)
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
        val viewModel = SpaceItemsViewModel(id, items, spaces)
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
        val viewModel = SpaceItemsViewModel(id, items, spaces)
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
        val viewModel = SpaceItemsViewModel(id, controlled, spaces)
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
        val viewModel = SpaceItemsViewModel(id, controlled, spaces)
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
        val viewModel = SpaceItemsViewModel(id, controlled, spaces)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.complete(itemId)
        assertEquals(1, writeCalls)
        assertFalse(viewModel.uiState.value.submitting)
    }

    @Test
    fun mapsModulesAndUpdatesTheChecklistProgress() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val members = FakeSpaceRepository(
            Space("space-1", "Flat 4B", 0, listOf("ana")),
            listOf(Member("ana", "Ana López", null, Role.OWNER)),
        )
        val deadline = LocalDate(2026, 10, 26)
        val itemId = items.create(
            "space-1",
            NewItem(
                "Groceries",
                preset = Preset.SHOPPING_LIST,
                modules = ItemModules(
                    assignment = Assignment.ToMember("ana"),
                    schedule = Schedule(date = LocalDate(2026, 10, 24), endDate = deadline),
                    checklist = Checklist(
                        listOf(ChecklistEntry("milk", "Milk", checked = true), ChecklistEntry("bread", "Bread")),
                    ),
                ),
            ),
        )
        val viewModel = SpaceItemsViewModel("space-1", items, members)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val card = viewModel.uiState.value.items.single()
        assertEquals(Res.string.item_type_list, card.label)
        assertEquals("Ana López", card.assigneeName)
        assertEquals("AL", card.assigneeInitials)
        assertEquals(deadline, card.dueDate)
        assertTrue(card.hasFooter)
        assertEquals(SpaceItemChecklist(1, 2), card.checklist)
        assertEquals(0.5f, card.checklist?.progress)

        items.setEntryChecked("space-1", itemId, "bread", checked = true)
        assertEquals(SpaceItemChecklist(2, 2), viewModel.uiState.value.items.single().checklist)
        assertEquals(1f, viewModel.uiState.value.items.single().checklist?.progress)
    }

    @Test
    fun keepsMissingModulesAbsentAndAnEmptyChecklistAtZero() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        items.create("space-1", NewItem("Plain item"))
        items.create("space-1", NewItem("Empty list", modules = ItemModules(checklist = Checklist(emptyList()))))
        val state = SpaceItemsViewModel("space-1", items, spaces).uiState.first { it.items.size == 2 }

        val plain = state.items.first()
        assertNull(plain.assignmentLabel)
        assertNull(plain.assigneeName)
        assertNull(plain.assigneeInitials)
        assertNull(plain.dueDate)
        assertNull(plain.checklist)
        assertFalse(plain.hasFooter)
        assertEquals(SpaceItemChecklist(0, 0), state.items.last().checklist)
        assertEquals(0f, state.items.last().checklist?.progress)
    }

    @Test
    fun distinguishesAnyoneUnclaimedAndUnknownAssignees() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        for (assignment in listOf(Assignment.Anyone, Assignment.Claim(), Assignment.ToMember("missing"))) {
            items.create("space-1", NewItem("Assigned item", modules = ItemModules(assignment = assignment)))
        }
        val state = SpaceItemsViewModel("space-1", items, spaces).uiState.first { it.items.size == 3 }

        assertEquals(
            listOf(
                Res.string.module_assignment_anyone,
                Res.string.module_assignment_unassigned,
                Res.string.item_unknown_member,
            ),
            state.items.map { it.assignmentLabel },
        )
        assertTrue(state.items.all { it.hasFooter && it.assigneeName == null })
    }

    @Test
    fun updatesNamesAndFallsBackWhenAMemberDisappears() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val member = Member("ana", "Ana López", null, Role.OWNER)
        val memberFlow = MutableStateFlow(listOf(member))
        val members = object : SpaceRepository by spaces {
            override fun observeMembers(spaceId: String) = memberFlow
        }
        val dueDate = LocalDate(2026, 10, 26)
        items.create(
            "space-1",
            NewItem(
                "Book plumber",
                modules = ItemModules(assignment = Assignment.Claim("ana"), schedule = Schedule(dueDate)),
            ),
        )
        val viewModel = SpaceItemsViewModel("space-1", items, members)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals("Ana López", viewModel.uiState.value.items.single().assigneeName)
        assertEquals(dueDate, viewModel.uiState.value.items.single().dueDate)

        memberFlow.value = listOf(member.copy(displayName = "Ana García"))
        assertEquals("AG", viewModel.uiState.value.items.single().assigneeInitials)

        memberFlow.value = listOf(member.copy(displayName = " "))
        assertNull(viewModel.uiState.value.items.single().assigneeName)
        memberFlow.value = emptyList()
        assertEquals(Res.string.item_unknown_member, viewModel.uiState.value.items.single().assignmentLabel)
        assertNull(viewModel.uiState.value.items.single().assigneeInitials)
    }

    @Test
    fun stillListsItemsWhenOpeningTheMemberStreamFails() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        items.create("space-1", NewItem("Book plumber", modules = ItemModules(assignment = Assignment.ToMember("ana"))))
        val failingMembers = object : SpaceRepository by spaces {
            override fun observeMembers(spaceId: String): kotlinx.coroutines.flow.Flow<List<Member>> =
                error("Member read failed")
        }

        val viewModel = SpaceItemsViewModel("space-1", items, failingMembers)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val state = viewModel.uiState.value

        assertEquals("Book plumber", state.items.single().title)
        assertNull(state.items.single().assigneeName)
        assertEquals(Res.string.item_unknown_member, state.items.single().assignmentLabel)
    }
}
