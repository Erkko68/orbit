package com.orbit.app.feature.spacehome.expenses

import com.orbit.app.domain.model.NewExpense
import com.orbit.app.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceExpensesViewModelTest {
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
}
