package com.orbit.app.feature.item

import androidx.lifecycle.ViewModelStore
import com.orbit.app.core.moneyInput
import com.orbit.app.domain.model.*
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.testing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ItemCostViewModelTest {
    @Test
    fun alreadyChargedOccurrenceCanBeCompletedWithoutRedistributingDepartedMembers() {
        val state = ItemUiState(
            loading = false,
            costAlreadyRecorded = true,
            modules = listOf(Cost(1000, "left", setOf("left"))),
        )
        assertTrue(state.canComplete)
        assertFalse(state.costInvalid)
    }

    private val auth = InMemoryAuthRepository()
    private val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER, "bea" to Role.MEMBER)
    private val ledger = InMemoryExpenseRepository(auth)
    private val items = InMemoryItemRepository(auth, ledger, spaces)
    private val viewModels = ViewModelStore()
    private var opened = 0

    @BeforeTest
    fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun teardown() {
        viewModels.clear()
        Dispatchers.resetMain()
    }

    private fun TestScope.open(id: String, repository: ItemRepository = items): ItemViewModel {
        val vm = ItemViewModel("space-1", id, repository, spaces, auth)
        viewModels.put("cost-${opened++}", vm)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    private fun fill(vm: ItemViewModel, cents: Long = 1000) {
        vm.editCost()
        vm.onCostAmountChange(moneyInput(cents, vm.uiState.value.costEditor!!.locale))
    }

    @Test
    fun newItemKeepsCostLocalUntilCreateAndCancelDiscardsOnlyTheEditor() = runTest {
        auth.signIn("ana", "secret")
        val vm = open("")
        vm.onTitleChange("Detergent")
        fill(vm)
        vm.saveCost()
        assertTrue(items.observeItems("space-1").first().isEmpty())
        fill(vm, 1200)
        vm.dismissCost()
        assertEquals(1000L, vm.uiState.value.cost?.cents)
        vm.create()
        assertEquals(1000L, items.observeItems("space-1").first().single().modules.cost?.cents)
        assertTrue(ledger.observeExpenses("space-1").first().isEmpty())
    }

    @Test
    fun costSaveFailureKeepsInputAndRetryWritesOnlyTheModule() = runTest {
        auth.signIn("ana", "secret")
        val id = items.create("space-1", NewItem("Detergent"))
        var fail = true
        val failing = object : ItemRepository by items {
            override suspend fun setModule(spaceId: String, itemId: String, module: Module) {
                if (fail) error("Offline refusal")
                items.setModule(spaceId, itemId, module)
            }
        }
        val vm = open(id, failing)
        fill(vm)
        vm.saveCost()
        assertTrue(vm.uiState.value.failed)
        assertEquals(1000L, vm.uiState.value.costEditor?.cents)
        fail = false
        vm.saveCost()
        assertNull(vm.uiState.value.costEditor)
        assertEquals(1000L, vm.uiState.value.cost?.cents)
        assertTrue(ledger.observeExpenses("space-1").first().isEmpty())
        vm.editCost()
        vm.removeCost()
        assertNull(vm.uiState.value.cost)
    }

    @Test
    fun reopeningAndChangingTheCostNeverRewritesOrDuplicatesThePastExpense() = runTest {
        auth.signIn("ana", "secret")
        val id = items.create("space-1", NewItem("Detergent"))
        val vm = open(id)
        fill(vm)
        vm.saveCost()
        vm.complete()
        vm.complete()
        vm.reopen()
        fill(vm, 1200)
        vm.saveCost()
        vm.complete()
        val expense = ledger.observeExpenses("space-1").first().single()
        assertEquals(1000L, expense.total)
        assertEquals(id, expense.itemId)
    }

    @Test
    fun lastChecklistTickCreatesTheExpenseAndUncheckingDoesNotDeleteIt() = runTest {
        auth.signIn("ana", "secret")
        val id = items.create(
            "space-1", NewItem(
                "Shopping", modules = ItemModules(
                    cost = Cost(1000, "ana", setOf("ana", "bea")),
                    checklist = Checklist(listOf(ChecklistEntry("e1", "Milk")))
                )
            )
        )
        val vm = open(id)
        vm.toggleChecklistEntry("e1")
        assertTrue(vm.uiState.value.isDone)
        vm.toggleChecklistEntry("e1")
        assertFalse(vm.uiState.value.isDone)
        vm.toggleChecklistEntry("e1")
        assertEquals(1, ledger.observeExpenses("space-1").first().size)
    }

    @Test
    fun invalidInputAndNoParticipantsCannotSave() = runTest {
        auth.signIn("ana", "secret")
        val vm = open("")
        fill(vm)
        vm.onCostAmountChange("0")
        assertFalse(vm.uiState.value.canSaveCost)
        vm.onCostAmountChange("9223372036854775808")
        assertFalse(vm.uiState.value.canSaveCost)
        fill(vm)
        vm.toggleCostParticipant("ana")
        vm.toggleCostParticipant("bea")
        assertFalse(vm.uiState.value.canSaveCost)
        vm.pickCostPayer("outsider")
        assertEquals("ana", vm.uiState.value.costEditor?.paidBy)
    }

    @Test
    fun recurringCompletionsChargeTheUpdatedCostOnlyForTheNextOccurrence() = runTest {
        auth.signIn("ana", "secret")
        val id = items.create(
            "space-1", NewItem(
                "Detergent", modules = ItemModules(
                    schedule = Schedule(kotlinx.datetime.LocalDate(2026, 11, 1)), recurrence = Recurrence.Days(1),
                    cost = Cost(1000, "ana", setOf("ana", "bea"))
                )
            )
        )
        val vm = open(id)
        vm.complete()
        assertFalse(vm.uiState.value.isDone)
        fill(vm, 1200)
        vm.saveCost()
        vm.complete()
        assertEquals(listOf(1000L, 1200L), ledger.observeExpenses("space-1").first().map { it.total }.sorted())
    }

    @Test
    fun pendingSaveBlocksDuplicateSubmissionAndRemovalFailureKeepsTheEditor() = runTest {
        auth.signIn("ana", "secret")
        val id = items.create("space-1", NewItem("Detergent"))
        val gate = CompletableDeferred<Unit>()
        var saves = 0
        val slow = object : ItemRepository by items {
            override suspend fun setModule(spaceId: String, itemId: String, module: Module) {
                saves++
                gate.await()
                items.setModule(spaceId, itemId, module)
            }

            override suspend fun removeModule(spaceId: String, itemId: String, type: ModuleType) = error("Denied")
        }
        val vm = open(id, slow)
        fill(vm)
        vm.saveCost()
        vm.saveCost()
        assertEquals(1, saves)
        assertTrue(vm.uiState.value.submitting)
        vm.dismissCost()
        assertNotNull(vm.uiState.value.costEditor)
        gate.complete(Unit)
        vm.editCost()
        vm.removeCost()
        assertTrue(vm.uiState.value.failed)
        assertNotNull(vm.uiState.value.costEditor)
        assertEquals(1000L, vm.uiState.value.cost?.cents)
    }

    @Test
    fun departedParticipantCanBeExplicitlyRemovedFromTheCostDraft() = runTest {
        auth.signIn("ana", "secret")
        val id = items.create(
            "space-1",
            NewItem("Detergent", modules = ItemModules(cost = Cost(1000, "ana", setOf("ana", "left"))))
        )
        val vm = open(id)
        assertFalse(vm.uiState.value.canComplete)
        vm.editCost()
        assertTrue(vm.uiState.value.costParticipantFaces.any { it.id == "left" })
        assertFalse(vm.uiState.value.canSaveCost)
        vm.toggleCostParticipant("left")
        assertTrue(vm.uiState.value.canSaveCost)
        vm.saveCost()
        assertEquals(setOf("ana"), vm.uiState.value.cost?.memberIds)
        assertTrue(vm.uiState.value.canComplete)
    }

    @Test
    fun failedFinalTickKeepsTheListOpenAndCanBeRetriedWithoutAnotherCharge() = runTest {
        auth.signIn("ana", "secret")
        var fail = true
        val refusing = object : com.orbit.app.domain.repository.ExpenseRepository by ledger {
            override suspend fun add(spaceId: String, expense: NewExpense): String {
                if (fail) error("Expense refused")
                return ledger.add(spaceId, expense)
            }
        }
        val repository = InMemoryItemRepository(auth, refusing, spaces)
        val id = repository.create(
            "space-1", NewItem(
                "Shopping", modules = ItemModules(
                    cost = Cost(1000, "ana", setOf("ana", "bea")),
                    checklist = Checklist(listOf(ChecklistEntry("e1", "Milk")))
                )
            )
        )
        val vm = open(id, repository)
        vm.toggleChecklistEntry("e1")
        assertTrue(vm.uiState.value.failed)
        assertFalse(vm.uiState.value.isDone)
        assertEquals(0, vm.uiState.value.checkedCount)
        assertTrue(ledger.observeExpenses("space-1").first().isEmpty())
        fail = false
        vm.toggleChecklistEntry("e1")
        assertTrue(vm.uiState.value.isDone)
        assertEquals(1, ledger.observeExpenses("space-1").first().size)
    }
}
