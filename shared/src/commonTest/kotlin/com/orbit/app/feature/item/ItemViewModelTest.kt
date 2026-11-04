package com.orbit.app.feature.item

import androidx.lifecycle.ViewModelStore
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.testing.ControlledItemRepository
import com.orbit.app.testing.FailingItemRepository
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ItemViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val items = InMemoryItemRepository(auth)
    private val spaces = FakeSpaceRepository.withMembers("ana@orbit.app" to Role.OWNER, "mika@orbit.app" to Role.MEMBER)
    private val viewModels = ViewModelStore()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        viewModels.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun resolvesSpaceAndMemberNames() = runTest {
        val id = createItem()
        val context = FakeSpaceRepository(
            com.orbit.app.domain.model.Space("space-1", "Flat 4B", 0, listOf("ana@orbit.app")),
            listOf(com.orbit.app.domain.model.Member("ana@orbit.app", "Ana", null, Role.OWNER)),
        )
        val model = track(ItemViewModel("space-1", id, items, context, auth))
        val state = model.uiState.first { !it.loading && it.spaceName != null }
        assertEquals("Flat 4B", state.spaceName)
        assertEquals("Ana", state.memberNames["ana@orbit.app"])
    }

    @Test
    fun failedMemberReadDoesNotHideTheItemOrDisableItsActions() = runTest {
        val id = createItem()
        val failing = object : com.orbit.app.domain.repository.SpaceRepository by spaces {
            override fun observeMembers(spaceId: String): Flow<List<com.orbit.app.domain.model.Member>> =
                kotlinx.coroutines.flow.flow { error("Read denied") }
        }
        val model = track(ItemViewModel("space-1", id, items, failing, auth))
        val state = model.uiState.first { !it.loading }
        assertEquals("Test item", state.title)
        assertTrue(state.canSubmit)
        assertFalse(state.loadFailed)
        assertTrue(state.memberNames.isEmpty())
    }

    @Test
    fun addModuleSetsModuleOnRepository() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val spaceId = "space-1"
        val itemId = items.create(spaceId, NewItem(title = "Task without modules"))
        val viewModel = track(ItemViewModel(spaceId, itemId, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.toggleAnyone()

        val updated = items.observeItems(spaceId).first().first { it.id == itemId }
        assertEquals(Assignment.Anyone, updated.modules.assignment)
    }

    @Test
    fun removeModuleRemovesModuleFromRepository() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val spaceId = "space-1"
        val itemId = items.create(
            spaceId,
            NewItem(
                title = "Anyone's task",
                modules = ItemModules(assignment = Assignment.Anyone),
            ),
        )
        val viewModel = track(ItemViewModel(spaceId, itemId, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.toggleAnyone()

        val updated = items.observeItems(spaceId).first().first { it.id == itemId }
        assertNull(updated.modules.assignment)
    }

    @Test
    fun removingScheduleAlsoRemovesReminder() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val spaceId = "space-1"
        val schedule = Schedule(date = LocalDate(2026, 10, 26))
        val reminder = Reminder(listOf(Duration.ZERO))
        val itemId = items.create(
            spaceId,
            NewItem(
                title = "Event",
                modules = ItemModules(schedule = schedule, reminder = reminder),
            ),
        )
        val viewModel = track(ItemViewModel(spaceId, itemId, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.removeSchedule()

        val updated = items.observeItems(spaceId).first().first { it.id == itemId }
        assertNull(updated.modules.schedule)
        assertNull(updated.modules.reminder)
    }

    @Test
    fun writeFailureFlagsFailedState() = runTest {
        val failingRepo = FailingItemRepository(
            items = listOf(
                Item(
                    id = "item-1",
                    title = "Test Item",
                    description = null,
                    status = ItemStatus.Open,
                    createdBy = "user-1",
                    createdAt = Instant.fromEpochMilliseconds(0),
                ),
            ),
        )
        val viewModel = track(ItemViewModel("space-1", "item-1", failingRepo, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertFalse(viewModel.uiState.value.failed)
        viewModel.toggleAnyone()
        assertTrue(viewModel.uiState.value.failed)
    }

    @Test
    fun missingItemEndsLoadingAndCannotBeChanged() = runTest {
        val controlled = ControlledItemRepository(items)
        val viewModel = track(ItemViewModel("space-1", "missing", controlled, spaces, auth))
        val state = viewModel.uiState.first { !it.loading }
        assertTrue(state.notFound)
        assertFalse(state.loadFailed)
        viewModel.toggleAnyone()
        viewModel.removeSchedule()
        assertTrue(controlled.writes.isEmpty())
    }

    @Test
    fun loadingItemCannotBeChanged() = runTest {
        val controlled = ControlledItemRepository(object : ItemRepository by items {
            override fun observeItems(spaceId: String) = emptyFlow<List<Item>>()
        })
        val viewModel = track(ItemViewModel("space-1", "item-1", controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.loading)
        viewModel.toggleAnyone()
        assertTrue(controlled.writes.isEmpty())
    }

    @Test
    fun readFailureIsDistinctFromMissingAndRetryLoadsTheItem() = runTest {
        val id = createItem()
        val controlled = ControlledItemRepository(items).apply {
            beforeRead = { error("Read denied") }
        }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.loadFailed)
        assertFalse(viewModel.uiState.value.loading)
        assertFalse(viewModel.uiState.value.notFound)
        assertTrue(controlled.writes.isEmpty())
        controlled.beforeRead = {}
        viewModel.retry()
        assertEquals("Test item", viewModel.uiState.value.title)
        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals(2, controlled.readAttempts)
        viewModel.retry()
        assertEquals(2, controlled.readAttempts)
    }

    @Test
    fun removeFailurePreservesTheModuleAndFlagsTheError() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Anyone))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write denied") } }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.toggleAnyone()
        assertTrue(viewModel.uiState.value.failed)
        assertFalse(viewModel.uiState.value.submitting)
        assertEquals(1, viewModel.uiState.value.modules.size)
        assertEquals(listOf("remove:ASSIGNMENT"), controlled.writes)
    }

    @Test
    fun nextWriteClearsTheErrorAndCanSucceed() = runTest {
        val id = createItem()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write denied") } }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.toggleAnyone()
        assertTrue(viewModel.uiState.value.failed)
        controlled.beforeWrite = {}
        viewModel.toggleAnyone()
        assertFalse(viewModel.uiState.value.failed)
        assertEquals(ModuleType.ASSIGNMENT, viewModel.uiState.value.modules.single().type)
    }

    @Test
    fun reminderRemovalFailureDoesNotRemoveItsSchedule() = runTest {
        val id = createItem(
            ItemModules(
                schedule = Schedule(LocalDate(2026, 10, 26)), reminder = Reminder(listOf(10.minutes)),
            )
        )
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write denied") } }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.removeSchedule()
        assertTrue(viewModel.uiState.value.failed)
        assertEquals(listOf("remove:REMINDER"), controlled.writes)
        assertEquals(2, viewModel.uiState.value.modules.size)
    }

    @Test
    fun scheduleRemovalFailureAfterReminderRemovalKeepsAValidItem() = runTest {
        val id = createItem(
            ItemModules(
                schedule = Schedule(LocalDate(2026, 10, 26)), reminder = Reminder(listOf(Duration.ZERO)),
            )
        )
        val controlled = ControlledItemRepository(items).apply {
            beforeWrite = { if (it == "remove:SCHEDULE") error("Write denied") }
        }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.removeSchedule()
        assertTrue(viewModel.uiState.value.failed)
        assertEquals(listOf(ModuleType.SCHEDULE), viewModel.uiState.value.modules.map { it.type })
        assertEquals(listOf("remove:REMINDER", "remove:SCHEDULE"), controlled.writes)
    }

    @Test
    fun cancellationIsNotReportedAsAWriteFailure() = runTest {
        val id = createItem()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { throw CancellationException() } }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.toggleAnyone()
        assertFalse(viewModel.uiState.value.failed)
        assertFalse(viewModel.uiState.value.submitting)
        assertTrue(viewModel.uiState.value.modules.isEmpty())
    }

    @Test
    fun editsTheScheduleAndClosesTheEditor() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26))))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.editSchedule()
        assertEquals(LocalDate(2026, 10, 26), model.uiState.value.scheduleEditor?.date)
        model.pickScheduleDate(ScheduleDate.START)
        model.onScheduleDatePicked(1_793_059_200_000) // 2026-10-27, UTC midnight
        model.onScheduleTimePicked(18, 30)
        model.onDurationHoursChange(1)
        model.onDurationMinutesChange(45)
        model.saveSchedule()

        val saved = items.observeItems("space-1").first().single { it.id == id }.modules.schedule
        assertEquals(Schedule(LocalDate(2026, 10, 27), time = LocalTime(18, 30), duration = 105.minutes), saved)
        assertNull(model.uiState.value.scheduleEditor)
    }

    @Test
    fun savesAWindowWithItsPickedEnd() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26))))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.editSchedule()
        model.setScheduleWindow(true)
        model.pickScheduleDate(ScheduleDate.END)
        model.onScheduleDatePicked(1_793_318_400_000) // 2026-10-30
        model.saveSchedule()

        val saved = items.observeItems("space-1").first().single { it.id == id }.modules.schedule
        assertEquals(Schedule(LocalDate(2026, 10, 26), endDate = LocalDate(2026, 10, 30)), saved)
    }

    @Test
    fun aFailedSaveKeepsTheEditorUntilTheNextEdit() = runTest {
        val schedule = Schedule(LocalDate(2026, 10, 26))
        val model = track(
            ItemViewModel(
                "space-1",
                "item-1",
                FailingItemRepository(
                    items = listOf(
                        Item(
                            id = "item-1",
                            title = "Test Item",
                            description = null,
                            status = ItemStatus.Open,
                            createdBy = "user-1",
                            createdAt = Instant.fromEpochMilliseconds(0),
                            modules = ItemModules(schedule = schedule),
                        ),
                    ),
                ),
                spaces,
                auth,
            ),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.editSchedule()
        model.onScheduleTimePicked(8, 0)
        model.saveSchedule()

        val failed = model.uiState.value
        assertTrue(failed.failed)
        assertFalse(failed.submitting)
        assertEquals("08:00", failed.scheduleEditor?.time)

        model.onScheduleTimePicked(8, 15)
        assertFalse(model.uiState.value.failed)
    }

    @Test
    fun dismissingTheEditorWritesNothing() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26))))
        val controlled = ControlledItemRepository(items)
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.editSchedule()
        model.onScheduleTimePicked(8, 0)
        model.dismissScheduleEditor()

        assertNull(model.uiState.value.scheduleEditor)
        assertTrue(controlled.writes.isEmpty())
    }

    @Test
    fun completesAndReopensTheItem() = runTest {
        val id = createItem()
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        assertFalse(model.uiState.value.isDone)

        model.complete()
        val completed = model.uiState.first { it.isDone }
        assertTrue(completed.isDone)

        model.reopen()
        val reopened = model.uiState.first { !it.isDone }
        assertFalse(reopened.isDone)
    }

    @Test
    fun completeFailureFlagsState() = runTest {
        val id = createItem()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.complete()
        val state = model.uiState.first { it.failed }
        assertTrue(state.failed)
        assertFalse(state.submitting)
    }

    @Test
    fun reopenFailureFlagsState() = runTest {
        val id = createItem()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.reopen()
        val state = model.uiState.first { it.failed }
        assertTrue(state.failed)
        assertFalse(state.submitting)
    }

    @Test
    fun assignToAnyoneUpdatesModule() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.ToMembers(setOf("mika@orbit.app"))))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.toggleAnyone()
        val updated = model.uiState.first { it.assignment == Assignment.Anyone }
        assertEquals(Assignment.Anyone, updated.assignment)
        assertEquals(Assignment.Anyone, items.observeItems("space-1").first().single { it.id == id }.modules.assignment)
    }

    @Test
    fun openAndDismissMemberPicker() = runTest {
        val id = createItem()
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        assertFalse(model.uiState.value.pickingMember)
        model.openMemberPicker()
        assertTrue(model.uiState.value.pickingMember)
        model.dismissMemberPicker()
        assertFalse(model.uiState.value.pickingMember)
    }

    @Test
    fun assignToUnknownMemberIsIgnored() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Anyone))
        val controlled = ControlledItemRepository(items)
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.toggleAssignee("stranger@orbit.app")
        assertTrue(controlled.writes.isEmpty())
    }

    @Test
    fun assignToAnyoneFailureFlagsState() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.ToMembers(setOf("mika@orbit.app"))))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.toggleAnyone()
        val state = model.uiState.first { it.failed }
        assertTrue(state.failed)
        assertFalse(state.submitting)
    }

    @Test
    fun assignToMemberFailureFlagsStateAndKeepsPickerOpen() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Anyone))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.openMemberPicker()
        model.toggleAssignee("mika@orbit.app")
        val state = model.uiState.first { it.failed }
        assertTrue(state.failed)
        assertFalse(state.submitting)
        assertTrue(state.pickingMember)
    }

    @Test
    fun loadsItem() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val schedule = Schedule(date = LocalDate(2026, 10, 26))
        val modules = ItemModules(schedule = schedule)
        val itemId = items.create("space-1", NewItem("Book plumber", "Kitchen leak", modules))

        val viewModel = track(ItemViewModel("space-1", itemId, items, spaces, auth))
        val state = viewModel.uiState.first { !it.loading }

        assertEquals("Book plumber", state.title)
        assertEquals("Kitchen leak", state.shownDescription)
        assertEquals(listOf(schedule), state.modules)
        assertFalse(state.isNew)
    }

    @Test
    fun aPendingWriteHoldsBackWritesThatMustNotRunTwice() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26))))
        val gate = CompletableDeferred<Unit>()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { gate.await() } }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.complete()
        assertTrue(viewModel.uiState.value.submitting)
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.complete()
        viewModel.reopen()
        assertEquals(listOf("complete"), controlled.writes)
        gate.complete(Unit)
        assertFalse(viewModel.uiState.value.submitting)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun tapsInARowAreNotHeldBackByAWriteTheServerHasNotConfirmed() = runTest {
        val entries = listOf(ChecklistEntry("milk", "Milk"), ChecklistEntry("bread", "Bread"))
        val id = createItem(ItemModules(checklist = Checklist(entries)))
        val gate = CompletableDeferred<Unit>()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { gate.await() } }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.toggleChecklistEntry("milk")
        viewModel.toggleChecklistEntry("bread")
        viewModel.toggleAssignee("mika@orbit.app")
        viewModel.toggleAssignee("ana@orbit.app")

        assertEquals(listOf("checkEntry", "checkEntry", "set:ASSIGNMENT", "set:ASSIGNMENT"), controlled.writes)
        assertFalse(viewModel.uiState.value.submitting)
        gate.complete(Unit)
    }

    @Test
    fun aTickedEntryStaysAMomentThenMoves() = runTest {
        val id = createItem(ItemModules(checklist = Checklist(listOf(ChecklistEntry("milk", "Milk")))))
        val viewModel = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.toggleChecklistEntry("milk")

        val ticked = viewModel.uiState.first { it.checkedCount == 1 }
        assertEquals("milk", ticked.shownEntries.first().id)
        assertTrue(ticked.shownEntries.first().checked)

        testScheduler.advanceTimeBy(1.seconds)

        assertEquals("milk", viewModel.uiState.value.shownEntries.last().id)
    }

    @Test
    fun aFailedTickKeepsTheEntryAndSaysSo() = runTest {
        val id = createItem(ItemModules(checklist = Checklist(listOf(ChecklistEntry("milk", "Milk")))))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write denied") } }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.toggleChecklistEntry("milk")

        assertTrue(viewModel.uiState.value.failed)
        assertEquals(0, viewModel.uiState.value.checkedCount)
    }

    @Test
    fun entriesAreAddedEditedAndRemovedWithoutChangingOthers() = runTest {
        val id = createItem(ItemModules(checklist = Checklist(listOf(ChecklistEntry("first", "Milk")))))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.onNewEntryChange(" Bread ")
        model.addNewEntry()
        val added = model.uiState.value.modules.filterIsInstance<Checklist>().single().entries.last()
        assertEquals("Bread", added.text)

        model.editChecklistEntry(added.id)
        model.onChecklistTextChange("Rolls")
        model.onChecklistQuantityChange("2")
        model.saveChecklistEntry()
        assertNull(model.uiState.value.checklistEditor)
        val changed = model.uiState.value.modules.filterIsInstance<Checklist>().single().entries
        assertEquals(ChecklistEntry(added.id, "Rolls", quantity = 2, order = 1), changed.last())
        assertEquals(ChecklistEntry("first", "Milk"), changed.first())

        model.editChecklistEntry(added.id)
        model.removeChecklistEntry()
        assertEquals(listOf(ChecklistEntry("first", "Milk")), model.uiState.value.modules.filterIsInstance<Checklist>().single().entries)
    }

    @Test
    fun checklistEditorRejectsBlankNamesAndInvalidQuantities() = runTest {
        val id = createItem(ItemModules(checklist = Checklist(listOf(ChecklistEntry("first", "Milk")))))
        val controlled = ControlledItemRepository(items)
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.editChecklistEntry("first")
        model.onChecklistTextChange("  ")
        model.saveChecklistEntry()
        model.onChecklistTextChange("Milk")
        for (quantity in listOf("0", "-2", "abc", "999999999999")) {
            model.onChecklistQuantityChange(quantity)
            assertTrue(model.uiState.value.checklistEditor?.quantityInvalid == true)
            model.saveChecklistEntry()
        }
        assertTrue(controlled.writes.isEmpty())
        model.onChecklistQuantityChange("")
        assertTrue(model.uiState.value.canSaveChecklist)
    }

    @Test
    fun checklistWriteFailuresKeepTheEditorAvailableForRetry() = runTest {
        val id = createItem(ItemModules(checklist = Checklist(listOf(ChecklistEntry("first", "Milk")))))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write denied") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.editChecklistEntry("first")
        model.onChecklistTextChange("Oat milk")
        model.saveChecklistEntry()
        assertTrue(model.uiState.value.failed)
        assertEquals("Oat milk", model.uiState.value.checklistEditor?.text)

        model.removeChecklistEntry()
        assertTrue(model.uiState.value.failed)
        assertEquals("first", model.uiState.value.checklistEditor?.entryId)
        assertEquals(listOf("updateEntry", "removeEntry"), controlled.writes)
        assertEquals(listOf(ChecklistEntry("first", "Milk")), model.uiState.value.modules.filterIsInstance<Checklist>().single().entries)
    }

    @Test
    fun severalMembersCanHaveTheItemAndThePickerStaysOpen() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Anyone))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.members.isNotEmpty() }

        model.openMemberPicker()
        model.toggleAssignee("mika@orbit.app")
        model.toggleAssignee("ana@orbit.app")

        val both = model.uiState.first { it.assigneeIds.size == 2 }
        assertEquals(setOf("mika@orbit.app", "ana@orbit.app"), both.assigneeIds)
        assertTrue(both.pickingMember)
        assertFalse(both.isAnyones)

        model.toggleAssignee("ana@orbit.app")
        assertEquals(setOf("mika@orbit.app"), model.uiState.first { it.assigneeIds.size == 1 }.assigneeIds)

        model.toggleAssignee("mika@orbit.app")
        assertNull(model.uiState.first { it.assignment == null }.assignment)
    }

    @Test
    fun anyoneTogglesOffAndReplacesWhoWasPicked() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.ToMembers(setOf("mika@orbit.app"))))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.assignment != null }

        model.toggleAnyone()
        assertTrue(model.uiState.first { it.isAnyones }.assigneeIds.isEmpty())

        model.toggleAnyone()
        assertNull(model.uiState.first { it.assignment == null }.assignment)
    }

    @Test
    fun theDescriptionIsSavedAndClearedWhenLeft() = runTest {
        val id = createItem()
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.onDescriptionChange("  Kitchen leak ")
        model.saveDrafts()
        val saved = model.uiState.first { it.description == "Kitchen leak" }
        assertNull(saved.descriptionDraft)

        model.onDescriptionChange("")
        model.saveDrafts()
        assertNull(model.uiState.first { it.description == null }.description)
    }

    @Test
    fun aFailedDescriptionKeepsWhatWasTyped() = runTest {
        val id = createItem()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.onDescriptionChange("Kitchen leak")
        model.saveDrafts()

        assertEquals("Kitchen leak", model.uiState.first { it.failed }.shownDescription)
    }

    @Test
    fun aNewItemFillsInAnyOrderAndWritesNothingUntilCreated() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val model = track(ItemViewModel("space-1", "", items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }

        val state = model.uiState.value
        assertTrue(state.isNew)
        assertTrue(state.canEdit)
        assertFalse(state.canCreate)

        model.toggleAnyone()
        model.onNewEntryChange("Milk")
        model.addNewEntry()
        model.editSchedule()
        model.saveSchedule()
        model.onTitleChange("Groceries")
        model.saveDrafts()

        val filled = model.uiState.value
        assertEquals(Assignment.Anyone, filled.assignment)
        assertEquals(listOf("Milk"), filled.shownEntries.map { it.text })
        assertTrue(filled.schedule != null)
        assertTrue(filled.canCreate)
        assertTrue(items.observeItems("space-1").first().isEmpty())
    }

    @Test
    fun createWritesEverythingInOneGoThenTheScreenCloses() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val controlled = ControlledItemRepository(items)
        val model = track(ItemViewModel("space-1", "", controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.onTitleChange(" Groceries ")
        model.onDescriptionChange("From the market")
        model.toggleAssignee("mika@orbit.app")
        model.onNewEntryChange("Milk")
        model.addNewEntry()
        model.onNewEntryChange("Bread")
        model.addNewEntry()
        model.toggleChecklistEntry(model.uiState.value.shownEntries.first().id)

        model.create()

        assertTrue(model.uiState.value.created)
        assertTrue(controlled.writes.isEmpty())
        val saved = items.observeItems("space-1").first().single()
        assertEquals("Groceries", saved.title)
        assertEquals("From the market", saved.description)
        assertEquals(setOf("mika@orbit.app"), saved.modules.assignment?.assigneeIds)
        assertEquals(listOf("Milk" to true, "Bread" to false), saved.modules.checklist?.entries?.map { it.text to it.checked })
    }

    @Test
    fun aNewItemNeedsANameToBeCreated() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val model = track(ItemViewModel("space-1", "", items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }

        model.onTitleChange("   ")
        model.create()

        assertFalse(model.uiState.value.created)
        assertTrue(items.observeItems("space-1").first().isEmpty())
    }

    @Test
    fun anEntryOfANewItemIsEditedAndRemovedInPlace() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val model = track(ItemViewModel("space-1", "", items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.onNewEntryChange("Milk")
        model.addNewEntry()
        val id = model.uiState.value.shownEntries.single().id

        model.editChecklistEntry(id)
        model.onChecklistTextChange("Oat milk")
        model.onChecklistQuantityChange("2")
        model.saveChecklistEntry()
        assertEquals(ChecklistEntry(id, "Oat milk", quantity = 2, order = 0), model.uiState.value.shownEntries.single())
        assertNull(model.uiState.value.checklistEditor)

        model.editChecklistEntry(id)
        model.removeChecklistEntry()
        assertFalse(model.uiState.value.hasEntries)
    }

    @Test
    fun aFailedCreateKeepsEverythingForTheNextTry() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val failing = object : ItemRepository by items {
            override suspend fun create(spaceId: String, item: NewItem): String = error("Write denied")
        }
        val model = track(ItemViewModel("space-1", "", failing, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.onTitleChange("Groceries")
        model.toggleAnyone()

        model.create()

        val state = model.uiState.first { it.failed }
        assertEquals("Groceries", state.shownTitle)
        assertEquals(Assignment.Anyone, state.assignment)
        assertTrue(state.isNew)
        assertFalse(state.created)
        assertFalse(state.submitting)
    }

    @Test
    fun theDeleteActionStaysAvailableWhileSomethingIsWritten() = runTest {
        val id = createItem()
        val gate = CompletableDeferred<Unit>()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { gate.await() } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.canDelete }

        model.complete()

        assertTrue(model.uiState.value.submitting)
        assertTrue(model.uiState.value.canDelete)
        gate.complete(Unit)
    }

    @Test
    fun aReminderAsksForNotificationsUntilAnswered() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26))))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.schedule != null }

        model.toggleAnyone()
        assertFalse(model.uiState.value.askNotifications)

        model.toggleReminderOffset(Duration.ZERO)
        assertTrue(model.uiState.first { it.askNotifications }.askNotifications)

        model.onNotificationsAnswered()
        assertFalse(model.uiState.value.askNotifications)
    }

    @Test
    fun aMomentTypedByHandJoinsTheOthers() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26)), reminder = Reminder(listOf(10.minutes))))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.reminder != null }

        model.openReminder()
        model.toggleCustomReminder()
        assertEquals(DurationUnit.MINUTES, model.uiState.first { it.customReminder != null }.customReminder?.unit)
        model.addCustomReminder()
        assertEquals(listOf(10.minutes), model.uiState.value.reminderOffsets)

        model.onCustomReminderAmountChange("3a0000")
        assertEquals("300", model.uiState.value.customReminder?.amount)
        model.onCustomReminderAmountChange("2")
        model.selectCustomReminderUnit(DurationUnit.DAYS)
        model.addCustomReminder()

        val saved = model.uiState.first { it.reminderOffsets.size == 2 }
        assertEquals(listOf(10.minutes, 2.days), saved.reminderOffsets)
        assertEquals(2.days, saved.reminderChoices.last())
        assertNull(saved.customReminder)
    }

    @Test
    fun aZeroAmountIsNotAMoment() {
        assertNull(CustomDuration(amount = "0").length)
        assertNull(CustomDuration(amount = "").length)
        assertEquals(90.minutes, CustomDuration("90", DurationUnit.MINUTES).length)
    }

    @Test
    fun noReminderTakesEveryMomentOff() = runTest {
        val id = createItem(
            ItemModules(schedule = Schedule(LocalDate(2026, 10, 26)), reminder = Reminder(listOf(Duration.ZERO, 10.minutes))),
        )
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.reminder != null }

        model.openReminder()
        model.clearReminder()

        val state = model.uiState.first { it.reminder == null }
        assertTrue(state.editingReminder)
        assertTrue(state.reminderOffsets.isEmpty())
    }

    private fun track(viewModel: ItemViewModel): ItemViewModel = viewModel.also { viewModels.put("item", it) }

    private suspend fun createItem(modules: ItemModules = ItemModules()): String {
        auth.signIn("ana@orbit.app", "secret")
        return items.create("space-1", NewItem(title = "Test item", modules = modules))
    }

    @Test
    fun picksATimeAndClearsIt() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26))))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }
        model.editSchedule()

        model.pickScheduleTime()
        assertTrue(model.uiState.value.scheduleEditor!!.pickingTime)
        assertEquals(LocalTime(9, 0), model.uiState.value.scheduleEditor!!.pickerTime)

        model.onScheduleTimePicked(7, 5)
        assertFalse(model.uiState.value.scheduleEditor!!.pickingTime)
        assertEquals("07:05", model.uiState.value.scheduleEditor!!.time)

        model.clearScheduleTime()
        assertEquals("", model.uiState.value.scheduleEditor!!.time)
    }

    @Test
    fun aLengthGoesOnTheWheelsAndZeroTakesItOff() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26), duration = 45.minutes)))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }
        model.editSchedule()

        assertEquals(0, model.uiState.value.scheduleEditor?.durationHours)
        assertEquals(45, model.uiState.value.scheduleEditor?.durationMinutes)

        model.onDurationMinutesChange(0)
        model.saveSchedule()

        assertNull(items.observeItems("space-1").first().single { it.id == id }.modules.schedule?.duration)
    }

    @Test
    fun addsToAListAndClearsTheField() = runTest {
        val id = createItem(ItemModules(checklist = Checklist(emptyList())))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }
        assertFalse(model.uiState.value.canAddEntry)

        model.onNewEntryChange("  Milk  ")
        assertTrue(model.uiState.value.canAddEntry)
        model.addNewEntry()

        assertEquals("", model.uiState.value.newEntry)
        assertEquals(listOf("Milk"), model.uiState.value.shownEntries.map { it.text })
    }

    @Test
    fun aFailedAddKeepsWhatWasTyped() = runTest {
        val item = Item(
            id = "item-1",
            title = "Groceries",
            description = null,
            status = ItemStatus.Open,
            createdBy = "ana@orbit.app",
            createdAt = Instant.fromEpochMilliseconds(0),
            modules = ItemModules(checklist = Checklist(emptyList())),
        )
        val model = track(ItemViewModel("space-1", "item-1", FailingItemRepository(items = listOf(item)), spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.onNewEntryChange("Milk")
        model.addNewEntry()

        assertTrue(model.uiState.value.failed)
        assertEquals("Milk", model.uiState.value.newEntry)

        model.onNewEntryChange("Milk!")
        assertFalse(model.uiState.value.failed)
    }

    @Test
    fun savingTheTitleRenamesAndDropsTheDraft() = runTest {
        val id = createItem()
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.onTitleChange("  Call the plumber ")
        assertEquals("  Call the plumber ", model.uiState.value.shownTitle)
        model.saveDrafts()

        val state = model.uiState.first { it.title == "Call the plumber" }
        assertNull(state.titleDraft)
    }

    @Test
    fun aBlankTitleGoesBackWithoutWriting() = runTest {
        val id = createItem()
        val controlled = ControlledItemRepository(items)
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        val title = model.uiState.first { !it.loading }.title

        model.onTitleChange("   ")
        model.saveDrafts()

        assertEquals(title, model.uiState.value.shownTitle)
        assertTrue(controlled.writes.isEmpty())
    }

    @Test
    fun aFailedRenameKeepsWhatWasTyped() = runTest {
        val id = createItem()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.onTitleChange("Call the plumber")
        model.saveDrafts()

        val state = model.uiState.first { it.failed }
        assertEquals("Call the plumber", state.shownTitle)
    }

    @Test
    fun deletingMarksTheItemGone() = runTest {
        val id = createItem()
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.canDelete }

        model.openDelete()
        assertTrue(model.uiState.value.confirmingDelete)
        model.deleteItem()

        assertTrue(model.uiState.first { it.deleted }.deleted)
    }

    @Test
    fun dismissingDeleteKeepsTheItem() = runTest {
        val id = createItem()
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.canDelete }

        model.openDelete()
        model.dismissDelete()

        assertFalse(model.uiState.value.confirmingDelete)
    }

    @Test
    fun aFailedDeleteKeepsTheSheetOpen() = runTest {
        val id = createItem()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.canDelete }

        model.openDelete()
        model.deleteItem()

        val state = model.uiState.first { it.failed }
        assertTrue(state.confirmingDelete)
        assertFalse(state.deleted)
    }

    @Test
    fun aNewItemHasNothingToDeleteYet() {
        val owner = Member("ana", "Ana", null, Role.OWNER)
        val state = ItemUiState(loading = false, isNew = true, currentUserId = "ana", members = listOf(owner))

        assertFalse(state.canDelete)
        assertTrue(state.copy(isNew = false).canDelete)
    }

    @Test
    fun onlyTheCreatorOrAManagerCanDelete() {
        val member = Member("mika", "Mika", null, Role.MEMBER)
        val admin = Member("rui", "Rui", null, Role.ADMIN)
        val state = ItemUiState(loading = false, createdBy = "ana", members = listOf(member, admin))

        assertTrue(state.copy(currentUserId = "ana").canDelete)
        assertTrue(state.copy(currentUserId = "rui").canDelete)
        assertFalse(state.copy(currentUserId = "mika").canDelete)
        assertFalse(state.canDelete)
    }

    @Test
    fun theHeaderShowsWhoHasTheItem() {
        val members = listOf(Member("ana", "Ana", null, Role.OWNER), Member("mika", "Mika", null, Role.MEMBER))
        val state = ItemUiState(loading = false, members = members)

        assertTrue(state.assigneeFaces.isEmpty())
        assertEquals(2, state.copy(modules = listOf(Assignment.Anyone)).assigneeFaces.size)
        assertEquals(listOf("mika"), state.copy(modules = listOf(Assignment.ToMembers(setOf("mika")))).assigneeFaces.map { it.id })
    }

    @Test
    fun theFirstReminderMomentAsksForNotificationsAndTheLastTakesItOff() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26))))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.schedule != null }

        model.openReminder()
        assertTrue(model.uiState.value.editingReminder)
        model.toggleReminderOffset(10.minutes)
        assertTrue(model.uiState.first { it.reminder != null }.askNotifications)

        model.toggleReminderOffset(10.minutes)
        assertNull(model.uiState.first { it.reminder == null }.reminder)
    }

    @Test
    fun aReminderNeedsASchedule() = runTest {
        val id = createItem()
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.openReminder()

        assertFalse(model.uiState.value.editingReminder)
    }

    @Test
    fun aFailedReminderKeepsTheSheetOpen() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26))))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.schedule != null }

        model.openReminder()
        model.toggleReminderOffset(Duration.ZERO)

        val state = model.uiState.first { it.failed }
        assertTrue(state.editingReminder)
    }

    @Test
    fun anItemWithoutAScheduleEditsOneFromToday() = runTest {
        val id = createItem()
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.editSchedule()
        assertTrue(model.uiState.value.scheduleEditor != null)
        model.saveSchedule()

        assertTrue(model.uiState.first { it.schedule != null }.scheduleEditor == null)
    }

    @Test
    fun removingTheScheduleClosesItsSheet() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26))))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { it.schedule != null }

        model.editSchedule()
        model.removeSchedule()

        val state = model.uiState.first { it.schedule == null }
        assertNull(state.scheduleEditor)
    }

    private suspend fun storedStatus(id: String) = items.observeItems("space-1").first().single { it.id == id }.status

    private fun TestScope.listModel(vararg entries: ChecklistEntry): Pair<String, ItemViewModel> {
        val id = runBlocking { items.create("space-1", NewItem("Groceries", modules = ItemModules(checklist = Checklist(entries.toList())))) }
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        return id to model
    }

    @Test
    fun tickingTheLastEntryCompletesTheItemAndUntickingOneOpensIt() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val (id, model) = listModel(ChecklistEntry("milk", "Milk", checked = true), ChecklistEntry("bread", "Bread"))
        model.uiState.first { it.hasEntries }

        model.toggleChecklistEntry("bread")
        model.uiState.first { it.isDone }
        assertTrue(storedStatus(id) is ItemStatus.Done)

        model.toggleChecklistEntry("milk")
        model.uiState.first { !it.isDone }
        assertTrue(storedStatus(id) is ItemStatus.Open)
    }

    @Test
    fun addingToAFinishedListOpensItAgain() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val (id, model) = listModel(ChecklistEntry("milk", "Milk", checked = true))
        model.uiState.first { it.hasEntries }
        items.complete("space-1", id)
        model.uiState.first { it.isDone }

        model.onNewEntryChange("Eggs")
        model.addNewEntry()

        model.uiState.first { !it.isDone }
        assertEquals(2, model.uiState.value.entryCount)
    }

    @Test
    fun removingTheOnlyEntryLeftOpenFinishesTheList() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val (_, model) = listModel(ChecklistEntry("milk", "Milk", checked = true), ChecklistEntry("bread", "Bread"))
        model.uiState.first { it.hasEntries }

        model.editChecklistEntry("bread")
        model.removeChecklistEntry()

        model.uiState.first { it.isDone }
        assertEquals(1, model.uiState.value.entryCount)
    }
}
