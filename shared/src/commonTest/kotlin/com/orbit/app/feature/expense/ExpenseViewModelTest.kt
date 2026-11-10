package com.orbit.app.feature.expense

import com.orbit.app.core.deviceMoneyLocale
import com.orbit.app.core.moneyInput
import com.orbit.app.domain.model.*
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {
    @Test
    fun aTemporarilyInvalidTotalKeepsDraftsAndDisablesUnitConversion() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.selectSplit(ExpenseSplit.EXACT)
        val amounts = vm.uiState.value.exact
        val shares = vm.uiState.value.split
        vm.onAmountChange("")
        assertFalse(vm.uiState.value.canChangeUnit)
        vm.selectUnit(ExpenseUnit.PERCENT)
        assertEquals(ExpenseUnit.MONEY, vm.uiState.value.unit)
        assertEquals(amounts, vm.uiState.value.exact)
        vm.onAmountChange(moneyInput(1000, locale))
        vm.selectUnit(ExpenseUnit.PERCENT)
        val percentages = vm.uiState.value.shareInputs
        vm.onAmountChange("")
        vm.selectUnit(ExpenseUnit.MONEY)
        assertEquals(ExpenseUnit.PERCENT, vm.uiState.value.unit)
        assertEquals(percentages, vm.uiState.value.shareInputs)
        vm.onAmountChange(moneyInput(1000, locale))
        vm.selectUnit(ExpenseUnit.MONEY)
        assertEquals(shares, vm.uiState.value.split)
    }

    @Test
    fun unitSwitchesKeepEveryCentAndTheSelectedUnitControlsTotalChanges() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.selectSplit(ExpenseSplit.EXACT)
        val original = vm.uiState.value.split
        repeat(5) {
            vm.selectUnit(ExpenseUnit.PERCENT)
            assertEquals(original, vm.uiState.value.split)
            vm.selectUnit(ExpenseUnit.MONEY)
            assertEquals(original, vm.uiState.value.split)
        }
        vm.onAmountChange(moneyInput(2000, locale))
        assertFalse(vm.uiState.value.canSave)
        assertEquals(1000L, vm.uiState.value.splitProgress?.assigned)
        vm.onAmountChange(moneyInput(1000, locale))
        vm.selectUnit(ExpenseUnit.PERCENT)
        vm.onAmountChange(moneyInput(2000, locale))
        assertEquals(mapOf("ana" to 668L, "bea" to 666L, "marc" to 666L), vm.uiState.value.split)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun percentagesNeedExactlyOneHundredAndPersistOnlyCentsThenReopenAsEuros() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.onAmountChange(moneyInput(4000, locale))
        vm.selectSplit(ExpenseSplit.EXACT)
        vm.selectUnit(ExpenseUnit.PERCENT)
        vm.onShareChange("ana", "50")
        vm.onShareChange("bea", "25")
        vm.onShareChange("marc", "24")
        assertFalse(vm.uiState.value.canSave)
        vm.onShareChange("marc", "26")
        assertTrue(vm.uiState.value.percentageExcess)
        vm.onShareChange("marc", "25")
        assertEquals(mapOf("ana" to 2000L, "bea" to 1000L, "marc" to 1000L), vm.uiState.value.split)
        controlled.failWrites = true
        vm.save()
        assertTrue(vm.uiState.value.failed)
        assertEquals("25", vm.uiState.value.shareInputs["marc"])
        controlled.failWrites = false
        vm.save()
        val saved = ledger.observeExpenses("space-1").first().single()
        val reopened = open(saved.id)
        assertEquals(ExpenseSplit.EXACT, reopened.uiState.value.mode)
        assertEquals(ExpenseUnit.MONEY, reopened.uiState.value.unit)
        assertEquals(saved.split, reopened.uiState.value.split)
    }

    @Test
    fun weightsAndSelectionKeepDraftValuesWithoutNormalizingPercentages() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.onAmountChange(moneyInput(4000, locale))
        vm.selectSplit(ExpenseSplit.WEIGHTS)
        vm.onShareChange("ana", "2")
        assertEquals(mapOf("ana" to 2000L, "bea" to 1000L, "marc" to 1000L), vm.uiState.value.split)
        vm.onShareChange("bea", "0")
        assertFalse(vm.uiState.value.canSave)
        vm.onShareChange("bea", "1")
        vm.toggleMember("ana")
        assertEquals(mapOf("bea" to 2000L, "marc" to 2000L), vm.uiState.value.split)
        vm.toggleMember("ana")
        assertEquals("2", vm.uiState.value.shareInputs["ana"])
        vm.selectSplit(ExpenseSplit.EXACT)
        vm.selectUnit(ExpenseUnit.PERCENT)
        vm.toggleMember("ana")
        assertFalse(vm.uiState.value.canSave)
        vm.toggleMember("ana")
        assertTrue(vm.uiState.value.canSave)
        vm.onShareChange("ana", "")
        assertEquals(setOf("ana"), vm.uiState.value.invalidShareIds)
        vm.selectUnit(ExpenseUnit.MONEY)
        vm.selectUnit(ExpenseUnit.PERCENT)
        assertEquals("", vm.uiState.value.shareInputs["ana"])
        assertEquals("25" + locale.decimal + "00", vm.uiState.value.shareInputs["bea"])
        vm.selectSplit(ExpenseSplit.WEIGHTS)
        vm.save()
        assertEquals(
            mapOf("ana" to 2000L, "bea" to 1000L, "marc" to 1000L),
            ledger.observeExpenses("space-1").first().single().split
        )
    }

    private val auth = InMemoryAuthRepository()
    private val ledger = InMemoryExpenseRepository(auth)
    private val controlled = ControlledExpenseRepository(ledger)
    private val locale = deviceMoneyLocale()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private suspend fun signIn() = auth.register("ana", "secret", "Ana")
    private fun people(role: Role = Role.OWNER) =
        FakeSpaceRepository.withMembers("ana" to role, "bea" to Role.MEMBER, "marc" to Role.MEMBER)

    private fun TestScope.open(id: String = "", spaces: SpaceRepository = people()): ExpenseViewModel {
        val vm = ExpenseViewModel("space-1", id, controlled, spaces, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        return vm
    }

    private fun ExpenseViewModel.fill() {
        onTitleChange("  Food  ")
        onAmountChange(moneyInput(1000, locale))
        pickDate(LocalDate(2026, 11, 4).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
    }

    private suspend fun stored(creator: String = "ana"): String {
        auth.register(creator, "secret", creator)
        return ledger.add(
            "space-1",
            NewExpense("Old", "ana", mapOf("ana" to 333L, "bea" to 667L), LocalDate(2026, 11, 1))
        )
    }

    @Test
    fun defaultsToTheSignedInPayerAndEveryoneAndWritesOnlyOnSave() = runTest {
        signIn()
        val vm = open()
        assertEquals("ana", vm.uiState.value.paidBy)
        assertEquals(setOf("ana", "bea", "marc"), vm.uiState.value.selected)
        vm.fill()
        assertTrue(vm.uiState.value.canSave)
        assertEquals(0, controlled.writes)
        vm.save()
        val saved = ledger.observeExpenses("space-1").first().single()
        assertEquals("Food", saved.title)
        assertEquals(mapOf("ana" to 334L, "bea" to 333L, "marc" to 333L), saved.split)
        assertEquals(LocalDate(2026, 11, 4), saved.date)
        assertTrue(vm.uiState.value.closed)
    }

    @Test
    fun exactAmountsMustMatchAndSwitchingModesKeepsTheEqualPreview() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.selectSplit(ExpenseSplit.EXACT)
        assertEquals(moneyInput(334, locale), vm.uiState.value.exact["ana"])
        assertTrue(vm.uiState.value.canSave)
        vm.onShareChange("ana", moneyInput(400, locale))
        assertFalse(vm.uiState.value.canSave)
        vm.onShareChange("bea", moneyInput(600, locale))
        vm.onShareChange("marc", "0")
        vm.selectPayer("bea")
        vm.save()
        val saved = ledger.observeExpenses("space-1").first().single()
        assertEquals("bea", saved.paidBy)
        assertEquals(mapOf("ana" to 400L, "bea" to 600L), saved.split)
    }

    @Test
    fun payerAndDateArePickedInTheirSheetsWhichCloseOnPick() = runTest {
        signIn()
        val vm = open()
        vm.openPayerPicker()
        assertTrue(vm.uiState.value.pickingPayer)
        vm.selectPayer("bea")
        assertFalse(vm.uiState.value.pickingPayer)
        assertEquals("bea", vm.uiState.value.payer?.name)
        vm.openPayerPicker()
        vm.dismissPayerPicker()
        assertFalse(vm.uiState.value.pickingPayer)
        assertEquals("bea", vm.uiState.value.paidBy)
        vm.openDatePicker()
        assertTrue(vm.uiState.value.pickingDate)
        val millis = LocalDate(2026, 3, 1).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        vm.pickDate(millis)
        assertFalse(vm.uiState.value.pickingDate)
        assertEquals(LocalDate(2026, 3, 1), vm.uiState.value.date)
        assertEquals(millis, vm.uiState.value.dateMillis)
        vm.openDatePicker()
        vm.dismissDatePicker()
        assertFalse(vm.uiState.value.pickingDate)
    }

    @Test
    fun equalSharesFollowTheSelectionAndTypedSharesShowAsTyped() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        assertEquals(mapOf("ana" to 334L, "bea" to 333L, "marc" to 333L), vm.uiState.value.shares)
        vm.toggleMember("marc")
        assertEquals(mapOf("ana" to 500L, "bea" to 500L), vm.uiState.value.shares)
        vm.selectSplit(ExpenseSplit.EXACT)
        vm.onShareChange("ana", "x")
        assertEquals(mapOf("ana" to 0L, "bea" to 500L), vm.uiState.value.shares)
    }

    @Test
    fun invalidFieldsAndAnEmptySelectionNeverWrite() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.onTitleChange("x".repeat(121))
        assertFalse(vm.uiState.value.canSave)
        vm.onTitleChange("Food")
        vm.onAmountChange("-1")
        assertFalse(vm.uiState.value.canSave)
        vm.onAmountChange(moneyInput(1000, locale))
        for (id in listOf("ana", "bea", "marc")) vm.toggleMember(id)
        vm.save()
        assertFalse(vm.uiState.value.canSave)
        assertEquals(0, controlled.writes)
    }

    @Test
    fun failedAddKeepsInputsAndCanBeRetriedAndCannotRunTwice() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        controlled.gate = CompletableDeferred()
        controlled.failWrites = true
        vm.save()
        vm.save()
        assertTrue(vm.uiState.value.submitting)
        assertFalse(vm.uiState.value.canSave)
        vm.onTitleChange("Ignored while saving")
        assertEquals("  Food  ", vm.uiState.value.title)
        assertEquals(1, controlled.writes)
        controlled.gate!!.complete(Unit)
        runCurrent()
        assertTrue(vm.uiState.value.failed)
        assertFalse(vm.uiState.value.closed)
        assertTrue(vm.uiState.value.canSave)
        vm.onTitleChange("Retry")
        assertFalse(vm.uiState.value.failed)
        controlled.failWrites = false
        vm.save()
        assertTrue(vm.uiState.value.closed)
    }

    @Test
    fun editPreservesStoredSharesAndReportsAFailedEdit() = runTest {
        val id = stored()
        val vm = open(id)
        assertEquals(ExpenseSplit.EXACT, vm.uiState.value.mode)
        assertEquals(mapOf("ana" to 333L, "bea" to 667L), vm.uiState.value.split)
        vm.onTitleChange("Edited")
        controlled.failWrites = true
        vm.save()
        assertTrue(vm.uiState.value.failed)
        assertEquals("Old", ledger.observeExpenses("space-1").first().single().title)
        controlled.failWrites = false
        vm.save()
        assertEquals("Edited", ledger.observeExpenses("space-1").first().single().title)
        assertEquals(mapOf("ana" to 333L, "bea" to 667L), ledger.observeExpenses("space-1").first().single().split)
    }

    @Test
    fun deleteRequiresConfirmationHandlesFailureAndThenRemovesTheEntry() = runTest {
        val id = stored()
        val vm = open(id)
        vm.delete()
        assertEquals(0, controlled.writes)
        vm.requestDelete()
        vm.dismissDelete()
        assertFalse(vm.uiState.value.deleteOpen)
        vm.requestDelete()
        controlled.failWrites = true
        vm.delete()
        assertTrue(vm.uiState.value.failed)
        assertTrue(vm.uiState.value.deleteOpen)
        assertFalse(vm.uiState.value.closed)
        controlled.failWrites = false
        vm.delete()
        assertTrue(ledger.observeExpenses("space-1").first().isEmpty())
        assertTrue(vm.uiState.value.closed)
    }

    @Test
    fun membersEditAnyEntryButDeleteOnlyTheirOwnWhileManagersDeleteAny() = runTest {
        val id = stored("bea")
        signIn()
        val vm = open(id, people(Role.MEMBER))
        assertTrue(vm.uiState.value.canSave)
        assertFalse(vm.uiState.value.canDelete)
        vm.requestDelete()
        vm.delete()
        assertEquals(0, controlled.writes)
        val admin = open(id, people(Role.ADMIN))
        assertTrue(admin.uiState.value.canDelete)
        val ownId = stored()
        assertTrue(open(ownId, people(Role.MEMBER)).uiState.value.canDelete)
    }

    @Test
    fun keepsFormerParticipantsWhenEditingAndUsesTheirSavedNames() = runTest {
        signIn()
        val id =
            ledger.add("space-1", NewExpense("Old", "former", mapOf("former" to 7, "ana" to 3), LocalDate(2026, 11, 1)))
        val spaces = FakeSpaceRepository(
            Space("space-1", "Flat", 0, listOf("ana"), mapOf("former" to "Former name")),
            listOf(Member("ana", "Ana", null, Role.OWNER)),
        )
        val vm = open(id, spaces)
        assertEquals("Former name", vm.uiState.value.people.first { it.id == "former" }.name)
        assertTrue(vm.uiState.value.canSave)
        vm.save()
        assertEquals("former", ledger.observeExpenses("space-1").first().single().paidBy)
    }

    @Test
    fun everyReadFailureShowsRetryAndPreservesTypedFields() = runTest {
        signIn()
        val id = stored()
        controlled.readFailures = 1
        val vm = open(id)
        assertTrue(vm.uiState.value.loadFailed)
        assertFalse(vm.uiState.value.canSave)
        vm.retry()
        assertFalse(vm.uiState.value.loadFailed)
        assertTrue(vm.uiState.value.canSave)
        for (spaces in listOf(FlakySpaceRepository(spaceFailures = 1), FlakySpaceRepository(memberFailures = 1))) {
            val form = open(spaces = spaces)
            assertTrue(form.uiState.value.loadFailed)
            form.retry()
            assertFalse(form.uiState.value.loadFailed)
            form.fill()
            assertTrue(form.uiState.value.canSave)
        }
        val brokenAuth = object : com.orbit.app.domain.repository.AuthRepository by auth {
            override val session = flow<User?> { error("Auth read failed") }
        }
        val form = ExpenseViewModel("space-1", "", controlled, people(), brokenAuth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { form.uiState.collect() }
        assertTrue(form.uiState.value.loadFailed)
    }

    @Test
    fun missingExpenseAndSignedOutSessionCannotWrite() = runTest {
        signIn()
        val vm = open("missing")
        assertTrue(vm.uiState.value.missing)
        assertFalse(vm.uiState.value.canSave)
        auth.signOut()
        val new = open()
        assertTrue(new.uiState.value.missing)
        new.fill()
        new.save()
        assertEquals(0, controlled.writes)
    }
}
