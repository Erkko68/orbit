package com.orbit.app.feature.spacehome.items

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.ControlledItemRepository
import com.orbit.app.testing.FailingItemRepository
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime


/** The in-memory user's id is their email. */
private const val ANA = "ana@orbit.app"

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
    fun tasksAndListsShareToDoAndDoneFoldsAway() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        items.create(id, NewItem("Book plumber"))
        items.create(id, NewItem("Groceries", modules = ItemModules(checklist = Checklist(listOf(ChecklistEntry("m", "Milk"))))))
        val dinner = items.create(id, NewItem("Dinner at 20:00"))
        items.complete(id, dinner, null)
        val viewModel = SpaceItemsViewModel(id, items, spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.first { it.items.size == 3 }
        assertEquals(listOf("Book plumber", "Groceries"), state.todo.map { it.title })
        assertEquals(listOf("Dinner at 20:00"), state.done.map { it.title })
        assertFalse(state.showingDone)

        viewModel.toggleDoneSection()
        assertTrue(viewModel.uiState.value.showingDone)
    }

    @Test
    fun filtersItemsWithTheSelectedChip() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        items.create(id, NewItem("Book plumber"))
        items.create(id, NewItem("Groceries", modules = ItemModules(checklist = Checklist(listOf(ChecklistEntry("m", "Milk"))))))
        items.create(id, NewItem("Empty list", modules = ItemModules(checklist = Checklist(emptyList()))))
        val viewModel = SpaceItemsViewModel(id, items, spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val titlesByFilter = ItemFilter.entries.associateWith { filter ->
            viewModel.selectFilter(filter)
            viewModel.uiState.value.also { assertEquals(filter, it.filter) }.items.map { it.title }
        }

        assertEquals(
            mapOf(
                ItemFilter.ALL to listOf("Book plumber", "Groceries", "Empty list"),
                ItemFilter.MINE to emptyList(),
                ItemFilter.LISTS to listOf("Groceries"),
            ),
            titlesByFilter,
        )
    }

    @Test
    fun saysWhenTheItemsFailToLoadAndLoadsThemOnRetry() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        items.create(id, NewItem("Book plumber"))
        val controlled = ControlledItemRepository(items).apply { beforeRead = { error("Could not reach the server") } }
        val viewModel = SpaceItemsViewModel(id, controlled, spaces, auth)
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
        val itemId = items.create(id, NewItem("Book plumber"))
        val viewModel = SpaceItemsViewModel(id, items, spaces, auth)
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
    fun completingARecurringItemMovesItToItsNextDate() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val modules = ItemModules(schedule = Schedule(LocalDate(2030, 1, 7)), recurrence = Recurrence.Days(3))
        val itemId = items.create(id, NewItem("Water the plants", modules = modules))
        val viewModel = SpaceItemsViewModel(id, items, spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.items.isNotEmpty() || it.turn != null }

        viewModel.complete(itemId)

        val item = items.observeItems(id).first { list -> list.single().modules.schedule?.date == LocalDate(2030, 1, 10) }.single()
        assertEquals(com.orbit.app.domain.model.ItemStatus.Open, item.status)
    }

    @Test
    fun aCompletedItemMovesToDone() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val firstId = items.create(id, NewItem("First task"))
        items.create(id, NewItem("Second task"))
        items.create(id, NewItem("Third task"))
        val viewModel = SpaceItemsViewModel(id, items, spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.complete(firstId)
        val state = viewModel.uiState.first { it.done.isNotEmpty() }

        assertEquals(listOf("Second task", "Third task"), state.todo.map { it.title })
        assertEquals(listOf("First task"), state.done.map { it.title })
    }

    @Test
    fun flagsFailureWhenCompleteFails() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        val itemId = items.create(id, NewItem("Book plumber"))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val viewModel = SpaceItemsViewModel(id, controlled, spaces, auth)
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
        val itemId = items.create(id, NewItem("Book plumber"))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val viewModel = SpaceItemsViewModel(id, controlled, spaces, auth)
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
        val itemId = items.create(id, NewItem("Book plumber"))
        var writeCalls = 0
        val controlled = ControlledItemRepository(items).apply {
            beforeWrite = { writeCalls++ }
        }
        val viewModel = SpaceItemsViewModel(id, controlled, spaces, auth)
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
                modules = ItemModules(
                    assignment = Assignment.ToMembers(setOf("ana")),
                    schedule = Schedule(date = LocalDate(2026, 10, 24), endDate = deadline),
                    checklist = Checklist(
                        listOf(ChecklistEntry("milk", "Milk", checked = true), ChecklistEntry("bread", "Bread")),
                    ),
                ),
            ),
        )
        val viewModel = SpaceItemsViewModel("space-1", items, members, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val card = viewModel.uiState.value.items.single()
        assertEquals(listOf("Ana López"), card.faces.map { it.name })
        assertEquals(deadline, card.dueDate)
        assertEquals(SpaceItemChecklist(1, 2), card.checklist)

        items.setEntryChecked("space-1", itemId, "bread", checked = true)
        assertEquals(SpaceItemChecklist(2, 2), viewModel.uiState.value.items.single().checklist)
    }

    @Test
    fun keepsMissingModulesAbsentAndAnEmptyChecklistIsNoList() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        items.create("space-1", NewItem("Plain item"))
        items.create("space-1", NewItem("Empty list", modules = ItemModules(checklist = Checklist(emptyList()))))
        val state = SpaceItemsViewModel("space-1", items, spaces, auth).uiState.first { it.items.size == 2 }

        val plain = state.items.first()
        assertTrue(plain.faces.isEmpty())
        assertNull(plain.summary)
        assertNull(plain.dueDate)
        assertNull(plain.checklist)
        assertNull(state.items.last().checklist)
    }

    @Test
    fun anyonesItemShowsEveryoneAndAnUnknownAssigneeNobody() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val everyone = object : SpaceRepository by spaces {
            override fun observeMembers(spaceId: String) = flowOf(listOf(Member("ana", "Ana", null, Role.OWNER), Member("mika", "Mika", null, Role.MEMBER)))
        }
        for (assignment in listOf(Assignment.Anyone, Assignment.ToMembers(setOf("missing")))) {
            items.create("space-1", NewItem("Assigned item", modules = ItemModules(assignment = assignment)))
        }
        val state = SpaceItemsViewModel("space-1", items, everyone, auth).uiState.first { it.items.size == 2 && it.items.first().faces.isNotEmpty() }

        assertEquals(listOf(listOf("Ana", "Mika"), emptyList()), state.items.map { card -> card.faces.map { it.name } })
    }

    @Test
    fun aCardCarriesTheSummaryAndTheTime() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val time = LocalTime(18, 0)
        items.create("space-1", NewItem("Task", "Sink leaking", ItemModules(schedule = Schedule(LocalDate(2026, 10, 26), time = time))))

        val card = SpaceItemsViewModel("space-1", items, spaces, auth).uiState.first { it.items.isNotEmpty() }.items.single()

        assertEquals("Sink leaking", card.summary)
        assertEquals(time, card.dueTime)
    }

    @Test
    fun followsTheMembersWhenTheyChangeOrLeave() = runTest {
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
                modules = ItemModules(assignment = Assignment.ToMembers(setOf("ana")), schedule = Schedule(dueDate)),
            ),
        )
        val viewModel = SpaceItemsViewModel("space-1", items, members, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(listOf("Ana López"), viewModel.uiState.value.items.single().faces.map { it.name })
        assertEquals(dueDate, viewModel.uiState.value.items.single().dueDate)

        memberFlow.value = listOf(member.copy(displayName = "Ana García"))
        assertEquals(listOf("Ana García"), viewModel.uiState.value.items.single().faces.map { it.name })

        memberFlow.value = emptyList()
        assertTrue(viewModel.uiState.value.items.single().faces.isEmpty())
    }

    @Test
    fun stillListsItemsWhenOpeningTheMemberStreamFails() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        items.create("space-1", NewItem("Book plumber", modules = ItemModules(assignment = Assignment.ToMembers(setOf("ana")))))
        val failingMembers = object : SpaceRepository by spaces {
            override fun observeMembers(spaceId: String): kotlinx.coroutines.flow.Flow<List<Member>> =
                error("Member read failed")
        }

        val viewModel = SpaceItemsViewModel("space-1", items, failingMembers, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val state = viewModel.uiState.value

        assertEquals("Book plumber", state.items.single().title)
        assertTrue(state.items.single().faces.isEmpty())
    }

    private suspend fun assignedTask(title: String, userId: String, due: LocalDate? = null) = items.create(
        "space-1",
        NewItem(
            title,
            modules = ItemModules(assignment = Assignment.ToMembers(setOf(userId)), schedule = due?.let { Schedule(it) }),
        ),
    )

    @Test
    fun theMineFilterKeepsWhatIsAssignedToTheUser() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        assignedTask("Mine", ANA)
        assignedTask("Theirs", "ben")
        val viewModel = SpaceItemsViewModel("space-1", items, spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.selectFilter(ItemFilter.MINE)

        assertEquals("Mine", viewModel.uiState.value.turn?.title)
        assertEquals(emptyList(), viewModel.uiState.value.items)
    }

    @Test
    fun theTurnIsTheOpenItemOfTheUserThatIsDueFirstAndLeavesTheRows() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        assignedTask("No date", ANA)
        assignedTask("Later", ANA, LocalDate(2026, 11, 20))
        assignedTask("Soon", ANA, LocalDate(2026, 11, 3))
        assignedTask("Not mine", "ben", LocalDate(2026, 11, 1))
        val viewModel = SpaceItemsViewModel("space-1", items, spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value

        assertEquals("Soon", state.turn?.title)
        assertEquals(listOf("No date", "Later", "Not mine"), state.items.map { it.title })
    }

    @Test
    fun aListIsNeverATurn() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        items.create(
            "space-1",
            NewItem(
                "Groceries",
                modules = ItemModules(
                    assignment = Assignment.ToMembers(setOf(ANA)),
                    checklist = Checklist(listOf(ChecklistEntry("m", "Milk"))),
                ),
            ),
        )
        val viewModel = SpaceItemsViewModel("space-1", items, spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertNull(viewModel.uiState.value.turn)
    }

    @Test
    fun finishingTheTurnMovesTheCardOnToTheNext() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        assignedTask("First", ANA, LocalDate(2026, 11, 3))
        assignedTask("Next", ANA, LocalDate(2026, 11, 4))
        val viewModel = SpaceItemsViewModel("space-1", items, spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.finishTurn()

        assertEquals("Next", viewModel.uiState.value.turn?.title)
        assertTrue(items.observeItems("space-1").first().single { it.title == "First" }.status is ItemStatus.Done)
    }

    @Test
    fun aFailedTurnStaysOpenAndSaysSo() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        assignedTask("First", ANA)
        val open = items.observeItems("space-1").first()
        val viewModel = SpaceItemsViewModel("space-1", FailingItemRepository(items = open), spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.finishTurn()

        assertEquals(false, viewModel.uiState.value.turn?.done)
        assertTrue(viewModel.uiState.value.failed)
    }

    @Test
    fun givesEachAssigneeTheirPlaceAmongTheMembers() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        spaces.addMember(id, Member("ben", "Ben", photoUrl = null, role = Role.MEMBER))
        items.create(id, NewItem("Theirs", modules = ItemModules(assignment = Assignment.ToMembers(setOf("ben")))))
        items.create(id, NewItem("Everyone's", modules = ItemModules(assignment = Assignment.Anyone)))
        items.create(id, NewItem("Nobody's"))
        val viewModel = SpaceItemsViewModel(id, items, spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val faces = viewModel.uiState.value.items.associate { it.title to it.faces.map { face -> face.index } }
        assertEquals(mapOf("Theirs" to listOf(1), "Everyone's" to listOf(0, 1), "Nobody's" to emptyList()), faces)
    }

    @Test
    fun aRowShowsTheFirstLineOfTheDescription() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        items.create("space-1", NewItem("Groceries", description = "\nFrom the market\nNot the corner shop"))
        val viewModel = SpaceItemsViewModel("space-1", items, spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals("From the market", viewModel.uiState.value.items.single().summary)
    }
}
