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
    fun editsFixOnlyTheirOwnShareAndMakeAutomaticReleasesIt() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.onAmountChange(moneyInput(4000, locale))
        vm.selectSplit(ExpenseSplit.EXACT)
        assertEquals(setOf("ana", "bea", "marc"), vm.uiState.value.automatic)
        vm.onShareChange("ana", moneyInput(1600, locale))
        assertEquals(mapOf("ana" to 1600L, "bea" to 1200L, "marc" to 1200L), vm.uiState.value.split)
        vm.onShareChange("bea", moneyInput(1000, locale))
        assertEquals(setOf("marc"), vm.uiState.value.automatic)
        vm.makeAutomatic("ana")
        assertEquals(mapOf("ana" to 1500L, "bea" to 1000L, "marc" to 1500L), vm.uiState.value.split)
        vm.onAmountChange(moneyInput(7000, locale))
        assertEquals(mapOf("ana" to 3000L, "bea" to 1000L, "marc" to 3000L), vm.uiState.value.split)
        assertEquals(0, controlled.writes)
    }

    @Test
    fun emptyManualInputAndExcessRemainDraftsUntilExplicitlyReleased() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.selectSplit(ExpenseSplit.EXACT)
        vm.onShareChange("ana", "1")
        vm.onShareChange("ana", "")
        assertFalse("ana" in vm.uiState.value.automatic)
        assertEquals("", vm.uiState.value.exact["ana"])
        assertNull(vm.uiState.value.split)
        assertEquals("", vm.uiState.value.shareInputs["bea"])
        vm.selectSplit(ExpenseSplit.PERCENTAGES)
        assertEquals(ExpenseSplit.EXACT, vm.uiState.value.mode)
        assertTrue(vm.uiState.value.conversionFailed)
        vm.onShareChange("ana", moneyInput(1100, locale))
        assertEquals(100L, vm.uiState.value.splitProgress?.excess)
        assertFalse(vm.uiState.value.canSave)
        vm.makeAutomatic("ana")
        assertTrue(vm.uiState.value.canSave)
        vm.onShareChange("ana", "0")
        assertFalse("ana" in vm.uiState.value.automatic)
        assertEquals(mapOf("bea" to 500L, "marc" to 500L), vm.uiState.value.split)
    }

    @Test
    fun selectionRestoresManualDraftAndNewPeopleStartAutomatic() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.selectSplit(ExpenseSplit.EXACT)
        vm.onShareChange("ana", moneyInput(400, locale))
        vm.toggleMember("ana")
        assertEquals(mapOf("bea" to 500L, "marc" to 500L), vm.uiState.value.split)
        vm.toggleMember("ana")
        assertFalse("ana" in vm.uiState.value.automatic)
        assertEquals(400L, vm.uiState.value.split?.get("ana"))
        vm.selectSplit(ExpenseSplit.EQUAL)
        vm.toggleMember("marc")
        vm.selectSplit(ExpenseSplit.PERCENTAGES)
        vm.toggleMember("marc")
        assertEquals(setOf("ana", "bea", "marc"), vm.uiState.value.automatic)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun percentagesKeepManualProportionsAndAutomaticThirdsAreComplete() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.selectSplit(ExpenseSplit.PERCENTAGES)
        assertEquals(10000L, vm.uiState.value.splitProgress?.assigned)
        assertTrue(vm.uiState.value.canSave)
        vm.onShareChange("ana", "50")
        vm.onAmountChange(moneyInput(8000, locale))
        assertEquals(mapOf("ana" to 4000L, "bea" to 2000L, "marc" to 2000L), vm.uiState.value.split)
        vm.openSplitPicker()
        vm.selectSplit(ExpenseSplit.PERCENTAGES)
        assertFalse(vm.uiState.value.pickingSplit)
        assertFalse("ana" in vm.uiState.value.automatic)
        vm.selectSplit(ExpenseSplit.EXACT)
        assertEquals(moneyInput(4000, locale), vm.uiState.value.exact["ana"])
        assertEquals(setOf("bea", "marc"), vm.uiState.value.automatic)
    }

    @Test
    fun aDeselectedDraftComesBackInItsModeAndIsDroppedOnAModeChange() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.selectSplit(ExpenseSplit.EXACT)
        vm.onShareChange("ana", moneyInput(400, locale))
        vm.toggleMember("ana")
        vm.toggleMember("ana")
        assertEquals(moneyInput(400, locale), vm.uiState.value.exact["ana"])
        assertFalse("ana" in vm.uiState.value.automatic)
        vm.toggleMember("ana")
        vm.selectSplit(ExpenseSplit.PERCENTAGES)
        vm.toggleMember("ana")
        assertTrue("ana" in vm.uiState.value.automatic)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun simpleModeConversionAcceptsRoundingWithoutRestoringHiddenRatios() = runTest {
        signIn()
        val id = ledger.add(
            "space-1", NewExpense(
                "Old", "ana", mapOf("ana" to 1L, "bea" to 99999L),
                LocalDate(2026, 11, 1)
            )
        )
        val vm = open(id)
        vm.selectSplit(ExpenseSplit.PERCENTAGES)
        assertEquals(mapOf("ana" to 10L, "bea" to 99990L), vm.uiState.value.split)
        vm.selectSplit(ExpenseSplit.EXACT)
        assertEquals(mapOf("ana" to 10L, "bea" to 99990L), vm.uiState.value.split)
        assertTrue(vm.uiState.value.automatic.isEmpty())
    }

    @Test
    fun savedZeroSelectionAndStatusesSurviveReopenAndNonAllocationEdits() = runTest {
        signIn()
        val allocation = mapOf("ana" to false, "bea" to true, "marc" to true)
        // These saved automatic amounts deliberately differ: opening must not silently redistribute them.
        val id = ledger.add(
            "space-1", NewExpense(
                "Old", "ana", mapOf("bea" to 4L, "marc" to 6L),
                LocalDate(2026, 11, 1), customAllocation = allocation
            )
        )
        val vm = open(id)
        assertEquals(allocation.keys, vm.uiState.value.selected)
        assertEquals(setOf("bea", "marc"), vm.uiState.value.automatic)
        vm.onTitleChange("Renamed")
        vm.selectPayer("bea")
        vm.pickDate(LocalDate(2026, 11, 5).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
        vm.save()
        val saved = ledger.observeExpenses("space-1").first().single()
        assertEquals(mapOf("bea" to 4L, "marc" to 6L), saved.split)
        assertEquals(allocation, saved.customAllocation)
        val reopened = open(id)
        reopened.onAmountChange(moneyInput(20, locale))
        assertEquals(mapOf("bea" to 10L, "marc" to 10L), reopened.uiState.value.split)
    }

    @Test
    fun metadataIsClearedByEqualAndWeightsAndInvalidContextFallsBackToManual() = runTest {
        signIn()
        val id = ledger.add(
            "space-1", NewExpense(
                "Old", "ana", mapOf("ana" to 10L),
                LocalDate(2026, 11, 1), customAllocation = mapOf("ana" to true, "unknown" to true)
            )
        )
        val vm = open(id)
        assertEquals(setOf("ana"), vm.uiState.value.selected)
        assertTrue(vm.uiState.value.automatic.isEmpty())
        vm.selectSplit(ExpenseSplit.EQUAL)
        vm.save()
        assertNull(ledger.observeExpenses("space-1").first().single().customAllocation)
        val weights = open(id)
        weights.selectSplit(ExpenseSplit.WEIGHTS)
        weights.selectSplit(ExpenseSplit.EXACT)
        assertTrue(weights.uiState.value.automatic.isEmpty())
        weights.selectSplit(ExpenseSplit.WEIGHTS)
        weights.save()
        assertNull(ledger.observeExpenses("space-1").first().single().customAllocation)
    }

    @Test
    fun formerZeroParticipantsAreLoadedFromMetadata() = runTest {
        signIn()
        val id = ledger.add(
            "space-1", NewExpense(
                "Old", "ana", mapOf("ana" to 1L), LocalDate(2026, 11, 1),
                customAllocation = mapOf("ana" to true, "former" to false)
            )
        )
        val spaces = FakeSpaceRepository(
            Space("space-1", "Flat", 0, listOf("ana"), mapOf("former" to "Former")),
            listOf(Member("ana", "Ana", null, Role.OWNER))
        )
        val vm = open(id, spaces)
        assertEquals("Former", vm.uiState.value.people.first { it.id == "former" }.name)
        assertEquals(moneyInput(0, locale), vm.uiState.value.exact["former"])
        assertFalse("former" in vm.uiState.value.automatic)
        assertTrue(vm.uiState.value.canSave)
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
    fun moneyFieldsTakeOnlyDigitsAndSeparatorsAndWeightsOnlyDigits() = runTest {
        signIn()
        val vm = open()
        vm.onAmountChange("12,50")
        for (typed in listOf("12,50a", "-12", "12 ", "€12", "1e3")) vm.onAmountChange(typed)
        assertEquals("12,50", vm.uiState.value.amount)
        vm.onAmountChange("1.234,5")
        assertEquals("1.234,5", vm.uiState.value.amount)
        vm.selectSplit(ExpenseSplit.EXACT)
        vm.onShareChange("ana", "x")
        assertFalse("x" in vm.uiState.value.exact["ana"].orEmpty())
        vm.selectSplit(ExpenseSplit.WEIGHTS)
        vm.onShareChange("ana", "1,5")
        vm.onShareChange("ana", "-2")
        assertEquals("1", vm.uiState.value.shareInputs["ana"])
        vm.onShareChange("ana", "3")
        assertEquals("3", vm.uiState.value.shareInputs["ana"])
    }

    @Test
    fun percentagesStartFromTheSplitAndMustAddUpToOneHundred() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.toggleMember("marc")
        vm.selectSplit(ExpenseSplit.PERCENTAGES)
        assertEquals(moneyInput(5000, locale), vm.uiState.value.automaticInputs["ana"])
        assertTrue(vm.uiState.value.canSave)
        vm.onShareChange("ana", "60")
        assertTrue(vm.uiState.value.canSave)
        vm.onShareChange("bea", "50")
        assertEquals(1000L, vm.uiState.value.splitProgress?.excess)
        assertFalse(vm.uiState.value.canSave)
        vm.onShareChange("bea", "")
        assertEquals(setOf("bea"), vm.uiState.value.invalidShareIds)
        vm.onShareChange("bea", "40")
        assertEquals(mapOf("ana" to 600L, "bea" to 400L), vm.uiState.value.shares)
        vm.selectSplit(ExpenseSplit.EXACT)
        assertEquals(moneyInput(600, locale), vm.uiState.value.exact["ana"])
        vm.save()
        assertEquals(mapOf("ana" to 600L, "bea" to 400L), ledger.observeExpenses("space-1").first().single().split)
    }

    @Test
    fun weightsStartAtOneAndAreKeptAcrossModes() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.openSplitPicker()
        vm.selectSplit(ExpenseSplit.EQUAL)
        assertFalse(vm.uiState.value.pickingSplit)
        vm.openSplitPicker()
        assertTrue(vm.uiState.value.pickingSplit)
        vm.dismissSplitPicker()
        assertFalse(vm.uiState.value.pickingSplit)
        vm.openSplitPicker()
        vm.selectSplit(ExpenseSplit.WEIGHTS)
        assertFalse(vm.uiState.value.pickingSplit)
        assertEquals(mapOf("ana" to 334L, "bea" to 333L, "marc" to 333L), vm.uiState.value.shares)
        vm.onShareChange("ana", "2")
        assertEquals(mapOf("ana" to 500L, "bea" to 250L, "marc" to 250L), vm.uiState.value.shares)
        vm.onShareChange("bea", "")
        assertEquals(setOf("bea"), vm.uiState.value.invalidShareIds)
        assertFalse(vm.uiState.value.canSave)
        vm.onShareChange("bea", "0")
        vm.selectSplit(ExpenseSplit.EQUAL)
        vm.onShareChange("ana", "9")
        vm.selectSplit(ExpenseSplit.WEIGHTS)
        assertEquals(mapOf("ana" to "2", "bea" to "0", "marc" to "1"), vm.uiState.value.shareInputs)
        vm.save()
        assertEquals(mapOf("ana" to 667L, "marc" to 333L), ledger.observeExpenses("space-1").first().single().split)
    }

    @Test
    fun exactAmountsMustMatchAndSwitchingModesKeepsTheEqualPreview() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.selectSplit(ExpenseSplit.EXACT)
        assertEquals(moneyInput(334, locale), vm.uiState.value.automaticInputs["ana"])
        assertTrue(vm.uiState.value.canSave)
        vm.onShareChange("ana", moneyInput(400, locale))
        assertTrue(vm.uiState.value.canSave)
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
        vm.onShareChange("ana", "1")
        vm.onShareChange("ana", "")
        assertEquals(mapOf("ana" to 0L, "bea" to 0L), vm.uiState.value.shares)
    }

    @Test
    fun invalidFieldsAndAnEmptySelectionNeverWrite() = runTest {
        signIn()
        val vm = open()
        vm.fill()
        vm.onTitleChange("x".repeat(121))
        assertFalse(vm.uiState.value.canSave)
        vm.onTitleChange("Food")
        vm.onAmountChange("0")
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
