package com.orbit.app.feature.spacehome.expenses

import com.orbit.app.domain.model.NewExpense
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.domain.model.ExpenseKind
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.MemberBalance
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.model.SuggestedPayment
import com.orbit.app.testing.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceExpensesViewModelTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val ledger = InMemoryExpenseRepository(auth)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observesAddsEditsAndDeletesInDateOrderAndKeepsSpacesSeparate() = runTest {
        auth.register("ana", "secret", "Ana")
        val spaces = FakeSpaceRepository.withMembers("ana" to com.orbit.app.domain.model.Role.OWNER)
        val vm = SpaceExpensesViewModel("space-1", ledger, spaces)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        assertTrue(vm.uiState.value.entries.isEmpty())
        val older = ledger.add("space-1", NewExpense("Old", "ana", mapOf("ana" to 10L), LocalDate(2026, 11, 1)))
        val newer = ledger.add("space-1", NewExpense("New", "ana", mapOf("ana" to 20L), LocalDate(2026, 11, 2)))
        ledger.add("other", NewExpense("Other", "ana", mapOf("ana" to 99L), LocalDate(2026, 11, 3)))
        assertEquals(listOf(newer, older), vm.uiState.value.entries.map { it.id })
        assertEquals("ana", vm.uiState.value.names["ana"])
        ledger.edit("space-1", older, "Edited", "ana", mapOf("ana" to 30L), LocalDate(2026, 11, 4))
        assertEquals("Edited", vm.uiState.value.entries.first().title)
        ledger.delete("space-1", older)
        assertEquals(listOf(newer), vm.uiState.value.entries.map { it.id })
    }

    @Test
    fun ledgerAndMemberAndSpaceReadsAllExposeFailureAndRetry() = runTest {
        val controlled = ControlledExpenseRepository(ledger).apply { readFailures = 1 }
        val cases = listOf(
            controlled to FlakySpaceRepository(),
            ledger to FlakySpaceRepository(memberFailures = 1),
            ledger to FlakySpaceRepository(spaceFailures = 1),
        )
        for ((expenses, spaces) in cases) {
            val vm = SpaceExpensesViewModel("space-1", expenses, spaces)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            assertTrue(vm.uiState.value.loadFailed)
            assertFalse(vm.uiState.value.loading)
            vm.retry()
            assertFalse(vm.uiState.value.loadFailed)
            assertEquals("ana", vm.uiState.value.names["ana"])
        }
    }

    @Test
    fun balancesFollowAddsEditsAndDeletesAndIncludeCurrentZeroBalances() = runTest {
        auth.register("ana", "secret", "Ana")
        val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER, "bea" to Role.MEMBER, "cam" to Role.MEMBER)
        val vm = SpaceExpensesViewModel("space-1", ledger, spaces)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        assertEquals(listOf(0L, 0L, 0L), vm.uiState.value.balances.members.map { it.cents })
        val id = ledger.add(
            "space-1",
            NewExpense("Food", "ana", mapOf("ana" to 1000L, "bea" to 1000L), LocalDate(2026, 11, 4))
        )
        assertEquals(
            listOf(MemberBalance("ana", 1000), MemberBalance("bea", -1000), MemberBalance("cam", 0)),
            vm.uiState.value.balances.members
        )
        assertEquals(listOf(SuggestedPayment("bea", "ana", 1000)), vm.uiState.value.balances.payments)
        ledger.add("other", NewExpense("Elsewhere", "bea", mapOf("ana" to 999L), LocalDate(2026, 11, 4)))
        assertEquals(1000L, vm.uiState.value.balances.members.first().cents)
        ledger.edit("space-1", id, "Food", "ana", mapOf("bea" to 500L), LocalDate(2026, 11, 4))
        assertEquals(listOf(SuggestedPayment("bea", "ana", 500)), vm.uiState.value.balances.payments)
        ledger.delete("space-1", id)
        assertTrue(vm.uiState.value.balances.payments.isEmpty())
        assertTrue(vm.uiState.value.balances.members.all { it.cents == 0L })
    }

    @Test
    fun onlyConfirmationWritesTheExactSettlementAndDeletingItRestoresTheDebt() = runTest {
        auth.register("ana", "secret", "Ana")
        val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER, "bea" to Role.MEMBER)
        ledger.add("space-1", NewExpense("Food", "ana", mapOf("bea" to 1240L), LocalDate(2026, 11, 4)))
        val controlled = ControlledExpenseRepository(ledger)
        val vm = SpaceExpensesViewModel("space-1", controlled, spaces)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        val payment = vm.uiState.value.balances.payments.single()
        vm.confirmSettlement()
        vm.requestSettlement(payment.copy(cents = 1))
        assertNull(vm.uiState.value.pendingPayment)
        vm.requestSettlement(payment)
        assertTrue(vm.uiState.value.canConfirm)
        assertEquals(0, controlled.writes)
        vm.dismissSettlement()
        vm.confirmSettlement()
        assertEquals(0, controlled.writes)
        vm.requestSettlement(payment)
        val before = Clock.System.todayIn(TimeZone.currentSystemDefault())
        vm.confirmSettlement()
        advanceUntilIdle()
        val after = Clock.System.todayIn(TimeZone.currentSystemDefault())
        assertEquals(1, controlled.writes)
        assertNull(vm.uiState.value.pendingPayment)
        assertFalse(vm.uiState.value.submitting)
        assertFalse(vm.uiState.value.failed)
        val entry = vm.uiState.value.entries.single { it.kind == ExpenseKind.SETTLEMENT }
        assertEquals("bea", entry.paidBy)
        assertEquals(mapOf("ana" to 1240L), entry.split)
        assertEquals("ana", entry.createdBy)
        assertTrue(entry.title.isNotBlank())
        assertTrue(entry.date == before || entry.date == after)
        assertNull(entry.itemId)
        assertTrue(vm.uiState.value.balances.payments.isEmpty())
        assertTrue(vm.uiState.value.balances.members.all { it.cents == 0L })
        ledger.delete("space-1", entry.id)
        assertEquals(listOf(payment), vm.uiState.value.balances.payments)
    }

    @Test
    fun failedWriteKeepsConfirmationAndRetryPreventsDoubleSubmission() = runTest {
        auth.register("ana", "secret", "Ana")
        val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER, "bea" to Role.MEMBER)
        ledger.add("space-1", NewExpense("Food", "ana", mapOf("bea" to 1240L), LocalDate(2026, 11, 4)))
        val controlled = ControlledExpenseRepository(ledger).apply { failWrites = true }
        val vm = SpaceExpensesViewModel("space-1", controlled, spaces)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        val payment = vm.uiState.value.balances.payments.single()
        vm.requestSettlement(payment)
        vm.confirmSettlement()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.failed)
        assertTrue(vm.uiState.value.canConfirm)
        assertEquals(payment, vm.uiState.value.pendingPayment)
        assertEquals(listOf(payment), vm.uiState.value.balances.payments)
        controlled.failWrites = false
        controlled.gate = CompletableDeferred()
        vm.confirmSettlement()
        runCurrent()
        assertTrue(vm.uiState.value.submitting)
        assertFalse(vm.uiState.value.failed)
        assertFalse(vm.uiState.value.canConfirm)
        vm.confirmSettlement()
        vm.dismissSettlement()
        vm.requestSettlement(payment)
        assertEquals(payment, vm.uiState.value.pendingPayment)
        assertEquals(2, controlled.writes)
        controlled.gate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, controlled.writes)
        assertEquals(1, vm.uiState.value.entries.count { it.kind == ExpenseKind.SETTLEMENT })
        assertNull(vm.uiState.value.pendingPayment)
    }

    @Test
    fun changedLedgerDisablesTheOldConfirmationAndRequiresANewSelection() = runTest {
        auth.register("ana", "secret", "Ana")
        val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER, "bea" to Role.MEMBER)
        val id = ledger.add("space-1", NewExpense("Food", "ana", mapOf("bea" to 1240L), LocalDate(2026, 11, 4)))
        val controlled = ControlledExpenseRepository(ledger)
        val vm = SpaceExpensesViewModel("space-1", controlled, spaces)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.requestSettlement(vm.uiState.value.balances.payments.single())
        ledger.edit("space-1", id, "Food", "ana", mapOf("bea" to 500L), LocalDate(2026, 11, 4))
        assertTrue(vm.uiState.value.paymentChanged)
        assertFalse(vm.uiState.value.canConfirm)
        vm.confirmSettlement()
        assertEquals(0, controlled.writes)
        vm.dismissSettlement()
        assertNull(vm.uiState.value.pendingPayment)
        vm.requestSettlement(vm.uiState.value.balances.payments.single())
        assertTrue(vm.uiState.value.canConfirm)
        vm.confirmSettlement()
        advanceUntilIdle()
        assertEquals(mapOf("ana" to 500L), vm.uiState.value.entries.single { it.kind == ExpenseKind.SETTLEMENT }.split)
    }

    @Test
    fun formerMembersKeepTheirNamesAndCanBeSettledWithoutAMemberDocument() = runTest {
        auth.register("ana", "secret", "Ana")
        val spaces = FakeSpaceRepository(
            Space("space-1", "Flat", 0, listOf("ana"), mapOf("bea" to "Beatrice")),
            listOf(Member("ana", "Ana", null, Role.OWNER)),
        )
        ledger.add("space-1", NewExpense("Food", "ana", mapOf("bea" to 100L), LocalDate(2026, 11, 4)))
        val vm = SpaceExpensesViewModel("space-1", ledger, spaces)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        assertEquals("Beatrice", vm.uiState.value.names["bea"])
        vm.requestSettlement(vm.uiState.value.balances.payments.single())
        vm.confirmSettlement()
        advanceUntilIdle()
        assertEquals(listOf(MemberBalance("ana", 0)), vm.uiState.value.balances.members)
    }

    @Test
    fun calculationFailureIsShownAsLoadFailureAndCanBeRetriedAfterLedgerCorrection() = runTest {
        auth.register("ana", "secret", "Ana")
        val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER, "bea" to Role.MEMBER)
        ledger.add("space-1", NewExpense("First", "ana", mapOf("bea" to Long.MAX_VALUE), LocalDate(2026, 11, 4)))
        val invalid = ledger.add("space-1", NewExpense("Second", "ana", mapOf("bea" to 1L), LocalDate(2026, 11, 4)))
        val vm = SpaceExpensesViewModel("space-1", ledger, spaces)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        assertTrue(vm.uiState.value.loadFailed)
        assertFalse(vm.uiState.value.canConfirm)
        vm.requestSettlement(SuggestedPayment("bea", "ana", 1))
        assertNull(vm.uiState.value.pendingPayment)
        ledger.delete("space-1", invalid)
        vm.retry()
        assertFalse(vm.uiState.value.loadFailed)
        assertEquals(Long.MAX_VALUE, vm.uiState.value.balances.payments.single().cents)
    }
}
