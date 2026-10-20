package com.orbit.app.feature.item

import androidx.lifecycle.ViewModelStore
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.model.validateItem
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
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
    fun loadsItemAndComputesAvailableModules() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val spaceId = "space-1"
        val schedule = Schedule(date = LocalDate(2026, 10, 26))
        val itemId = items.create(
            spaceId,
            NewItem(
                title = "Book plumber",
                description = "Kitchen leak",
                preset = Preset.TASK,
                modules = ItemModules(schedule = schedule),
            ),
        )

        val viewModel = track(ItemViewModel(spaceId, itemId, items, spaces, auth))
        val state = viewModel.uiState.first { !it.loading }

        assertEquals("Book plumber", state.title)
        assertEquals("Kitchen leak", state.description)
        assertEquals(Preset.TASK, state.preset)
        assertEquals(listOf(schedule), state.modules)
        // With schedule present, Reminder is available along with Assignment and Checklist
        assertEquals(
            listOf(ModuleType.ASSIGNMENT, ModuleType.CHECKLIST, ModuleType.REMINDER),
            state.availableModules,
        )
    }

    @Test
    fun hidesReminderWhenScheduleAbsent() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val spaceId = "space-1"
        val itemId = items.create(
            spaceId,
            NewItem(
                title = "Groceries",
                preset = Preset.SHOPPING_LIST,
                modules = ItemModules(checklist = Checklist(emptyList())),
            ),
        )

        val viewModel = track(ItemViewModel(spaceId, itemId, items, spaces, auth))
        val state = viewModel.uiState.first { !it.loading }

        // Reminder requires schedule, so without schedule Reminder is not offered
        assertEquals(
            listOf(ModuleType.SCHEDULE, ModuleType.ASSIGNMENT),
            state.availableModules,
        )
    }

    @Test
    fun addModuleSetsModuleOnRepository() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val spaceId = "space-1"
        val itemId = items.create(spaceId, NewItem(title = "Task without modules"))
        val viewModel = track(ItemViewModel(spaceId, itemId, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.addModule(ModuleType.CHECKLIST)

        val updated = items.observeItems(spaceId).first().first { it.id == itemId }
        assertTrue(updated.modules.checklist != null)
    }

    @Test
    fun addingAReminderAsksForNotificationsUntilAnswered() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val scheduled = ItemModules(schedule = Schedule(LocalDate(2026, 10, 26)))
        val itemId = items.create("space-1", NewItem(title = "Dinner", modules = scheduled))
        val viewModel = track(ItemViewModel("space-1", itemId, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.addModule(ModuleType.CHECKLIST)
        assertFalse(viewModel.uiState.value.askNotifications)

        viewModel.addModule(ModuleType.REMINDER)
        assertTrue(viewModel.uiState.value.askNotifications)

        viewModel.onNotificationsAnswered()
        assertFalse(viewModel.uiState.value.askNotifications)
    }

    @Test
    fun removeModuleRemovesModuleFromRepository() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val spaceId = "space-1"
        val itemId = items.create(
            spaceId,
            NewItem(
                title = "Task with checklist",
                modules = ItemModules(checklist = Checklist(emptyList())),
            ),
        )
        val viewModel = track(ItemViewModel(spaceId, itemId, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.removeModule(ModuleType.CHECKLIST)

        val updated = items.observeItems(spaceId).first().first { it.id == itemId }
        assertNull(updated.modules.checklist)
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

        viewModel.removeModule(ModuleType.SCHEDULE)

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
                    preset = null,
                    createdBy = "user-1",
                    createdAt = Instant.fromEpochMilliseconds(0),
                ),
            ),
        )
        val viewModel = track(ItemViewModel("space-1", "item-1", failingRepo, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertFalse(viewModel.uiState.value.failed)
        viewModel.addModule(ModuleType.SCHEDULE)
        assertTrue(viewModel.uiState.value.failed)
    }

    @Test
    fun missingItemEndsLoadingAndCannotBeChanged() = runTest {
        val controlled = ControlledItemRepository(items)
        val viewModel = track(ItemViewModel("space-1", "missing", controlled, spaces, auth))
        val state = viewModel.uiState.first { !it.loading }
        assertTrue(state.notFound)
        assertFalse(state.loadFailed)
        viewModel.addModule(ModuleType.CHECKLIST)
        viewModel.removeModule(ModuleType.SCHEDULE)
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
        viewModel.addModule(ModuleType.SCHEDULE)
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
    fun eachModuleStartsWithValidDefaultsAndExistingModulesAreNotOverwritten() = runTest {
        val id = createItem()
        val controlled = ControlledItemRepository(items)
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.addModule(ModuleType.REMINDER)
        viewModel.removeModule(ModuleType.CHECKLIST)
        assertTrue(controlled.writes.isEmpty())
        viewModel.addModule(ModuleType.SCHEDULE)
        viewModel.addModule(ModuleType.ASSIGNMENT)
        viewModel.addModule(ModuleType.CHECKLIST)
        viewModel.addModule(ModuleType.REMINDER)
        val loaded = items.observeItems("space-1").first().single()
        assertEquals(Assignment.Anyone, loaded.modules.assignment)
        assertEquals(emptyList(), loaded.modules.checklist?.entries)
        assertEquals(listOf(Duration.ZERO), loaded.modules.reminder?.offsets)
        assertTrue(validateItem(loaded.title, loaded.modules, emptyList()).isEmpty())
        viewModel.addModule(ModuleType.SCHEDULE)
        assertEquals(4, controlled.writes.size)
    }

    @Test
    fun removeFailurePreservesTheModuleAndFlagsTheError() = runTest {
        val id = createItem(ItemModules(checklist = Checklist(emptyList())))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write denied") } }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.removeModule(ModuleType.CHECKLIST)
        assertTrue(viewModel.uiState.value.failed)
        assertFalse(viewModel.uiState.value.submitting)
        assertEquals(1, viewModel.uiState.value.modules.size)
        assertEquals(listOf("remove:CHECKLIST"), controlled.writes)
    }

    @Test
    fun nextWriteClearsTheErrorAndCanSucceed() = runTest {
        val id = createItem()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write denied") } }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.addModule(ModuleType.CHECKLIST)
        assertTrue(viewModel.uiState.value.failed)
        controlled.beforeWrite = {}
        viewModel.addModule(ModuleType.CHECKLIST)
        assertFalse(viewModel.uiState.value.failed)
        assertEquals(ModuleType.CHECKLIST, viewModel.uiState.value.modules.single().type)
    }

    @Test
    fun pendingWriteBlocksOtherActionsUntilItFinishes() = runTest {
        val id = createItem(ItemModules(schedule = Schedule(LocalDate(2026, 10, 26))))
        val gate = CompletableDeferred<Unit>()
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { gate.await() } }
        val viewModel = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.addModule(ModuleType.CHECKLIST)
        assertTrue(viewModel.uiState.value.submitting)
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.addModule(ModuleType.ASSIGNMENT)
        viewModel.removeModule(ModuleType.SCHEDULE)
        assertEquals(listOf("set:CHECKLIST"), controlled.writes)
        gate.complete(Unit)
        assertFalse(viewModel.uiState.value.submitting)
        assertTrue(viewModel.uiState.value.canSubmit)
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
        viewModel.removeModule(ModuleType.SCHEDULE)
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
        viewModel.removeModule(ModuleType.SCHEDULE)
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
        viewModel.addModule(ModuleType.CHECKLIST)
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
        assertEquals(ScheduleEditor(date = LocalDate(2026, 10, 26)), model.uiState.value.scheduleEditor)
        model.pickScheduleDate(ScheduleDate.START)
        model.onScheduleDatePicked(1_793_059_200_000) // 2026-10-27, UTC midnight
        model.onScheduleTimeChange("18:30")
        model.onScheduleDurationChange("45")
        model.saveSchedule()

        val saved = items.observeItems("space-1").first().single { it.id == id }.modules.schedule
        assertEquals(Schedule(LocalDate(2026, 10, 27), time = LocalTime(18, 30), duration = 45.minutes), saved)
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
    fun anInvalidScheduleIsNeverWritten() = runTest {
        val schedule = Schedule(LocalDate(2026, 10, 26))
        val id = createItem(ItemModules(schedule = schedule))
        val controlled = ControlledItemRepository(items)
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.editSchedule()
        model.onScheduleTimeChange("25:00")
        assertFalse(model.uiState.value.canSaveSchedule)
        model.saveSchedule()

        assertTrue(controlled.writes.isEmpty())
        assertTrue(model.uiState.value.scheduleEditor!!.timeInvalid)
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
                            preset = null,
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
        model.onScheduleTimeChange("08:00")
        model.saveSchedule()

        val failed = model.uiState.value
        assertTrue(failed.failed)
        assertFalse(failed.submitting)
        assertEquals("08:00", failed.scheduleEditor?.time)

        model.onScheduleTimeChange("08:15")
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
        model.onScheduleTimeChange("08:00")
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
        val id = createItem(ItemModules(assignment = Assignment.ToMember("mika@orbit.app")))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.assignToAnyone()
        val updated = model.uiState.first { it.assignment == Assignment.Anyone }
        assertEquals(Assignment.Anyone, updated.assignment)
        assertEquals(Assignment.Anyone, items.observeItems("space-1").first().single { it.id == id }.modules.assignment)
    }

    @Test
    fun assignToClaimUpdatesModule() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Anyone))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.assignToClaim()
        val updated = model.uiState.first { it.assignment is Assignment.Claim }
        assertEquals(Assignment.Claim(null), updated.assignment)
        assertTrue(updated.canClaim)
        assertFalse(updated.canUnclaim)
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
    fun assignToMemberSetsModuleAndDismissesPicker() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Anyone))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.openMemberPicker()
        assertTrue(model.uiState.value.pickingMember)

        model.assignToMember("mika@orbit.app")
        val updated = model.uiState.first { it.assignment == Assignment.ToMember("mika@orbit.app") }
        assertEquals(Assignment.ToMember("mika@orbit.app"), updated.assignment)
        assertFalse(updated.pickingMember)
    }

    @Test
    fun assignToUnknownMemberIsIgnored() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Anyone))
        val controlled = ControlledItemRepository(items)
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.assignToMember("stranger@orbit.app")
        assertTrue(controlled.writes.isEmpty())
    }

    @Test
    fun claimAssignsCurrentUserWhenUnclaimed() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Claim(null)))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        assertTrue(model.uiState.value.canClaim)
        assertFalse(model.uiState.value.canUnclaim)

        model.claim()
        val updated = model.uiState.first { it.assignment == Assignment.Claim("ana@orbit.app") }
        assertEquals(Assignment.Claim("ana@orbit.app"), updated.assignment)
        assertFalse(updated.canClaim)
        assertTrue(updated.canUnclaim)
    }

    @Test
    fun claimDoesNothingWhenAlreadyClaimed() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Claim("mika@orbit.app")))
        val controlled = ControlledItemRepository(items)
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        assertFalse(model.uiState.value.canClaim)
        model.claim()
        assertTrue(controlled.writes.isEmpty())
    }

    @Test
    fun unclaimResetsClaimToNullWhenCallerIsClaimer() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Claim("ana@orbit.app")))
        val model = track(ItemViewModel("space-1", id, items, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        assertTrue(model.uiState.value.canUnclaim)
        model.unclaim()

        val updated = model.uiState.first { it.assignment == Assignment.Claim(null) }
        assertEquals(Assignment.Claim(null), updated.assignment)
        assertTrue(updated.canClaim)
        assertFalse(updated.canUnclaim)
    }

    @Test
    fun unclaimDoesNothingWhenCallerIsNotClaimer() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Claim("mika@orbit.app")))
        val controlled = ControlledItemRepository(items)
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        assertFalse(model.uiState.value.canUnclaim)
        model.unclaim()
        assertTrue(controlled.writes.isEmpty())
    }

    @Test
    fun assignToAnyoneFailureFlagsState() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.ToMember("mika@orbit.app")))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.assignToAnyone()
        val state = model.uiState.first { it.failed }
        assertTrue(state.failed)
        assertFalse(state.submitting)
    }

    @Test
    fun assignToClaimFailureFlagsState() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Anyone))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.assignToClaim()
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
        model.assignToMember("mika@orbit.app")
        val state = model.uiState.first { it.failed }
        assertTrue(state.failed)
        assertFalse(state.submitting)
        assertTrue(state.pickingMember)
    }

    @Test
    fun claimFailureFlagsState() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Claim(null)))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.claim()
        val state = model.uiState.first { it.failed }
        assertTrue(state.failed)
        assertFalse(state.submitting)
    }

    @Test
    fun unclaimFailureFlagsState() = runTest {
        val id = createItem(ItemModules(assignment = Assignment.Claim("ana@orbit.app")))
        val controlled = ControlledItemRepository(items).apply { beforeWrite = { error("Write failed") } }
        val model = track(ItemViewModel("space-1", id, controlled, spaces, auth))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.uiState.first { !it.loading }

        model.unclaim()
        val state = model.uiState.first { it.failed }
        assertTrue(state.failed)
        assertFalse(state.submitting)
    }

    private fun track(viewModel: ItemViewModel): ItemViewModel = viewModel.also { viewModels.put("item", it) }

    private suspend fun createItem(modules: ItemModules = ItemModules()): String {
        auth.signIn("ana@orbit.app", "secret")
        return items.create("space-1", NewItem(title = "Test item", modules = modules))
    }
}
