package com.orbit.app.feature.spacehome.expenses

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.formatEuro
import com.orbit.app.domain.model.ExpenseKind
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.model.Role
import com.orbit.app.testing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SpaceExpensesTabTest : ComposeTest() {
    @Test
    fun showsTheLedgerWithTitleTotalPayerAndDateAndOpensTheSelectedEntry() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = InMemorySpaceRepository(auth)
        val ledger = InMemoryExpenseRepository(auth)
        val id = runBlocking {
            auth.register("ana", "secret", "Ana")
            spaces.createSpace("Flat", 0)
            ledger.add("space-1", NewExpense("Food", "ana", mapOf("ana" to 1240L), LocalDate(2026, 11, 4)))
        }
        val vm = SpaceExpensesViewModel("space-1", ledger, spaces)
        var opened = ""
        setContent {
            OrbitTheme {
                OrbitScreen(scrollable = true) {
                    SpaceExpensesTab(
                        "space-1",
                        { opened = it },
                        viewModel = vm
                    )
                }
            }
        }
        onNodeWithText(formatEuro(1240)).performScrollTo().assertIsDisplayed()
        onNodeWithText("Paid by: Ana").performScrollTo().assertIsDisplayed()
        onNodeWithText("2026", substring = true).performScrollTo().assertIsDisplayed()
        onNodeWithText("Food").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(id, opened)
    }

    @Test
    fun confirmsAPaymentAndUpdatesBalancesSuggestionsAndLedgerThenDeletionRestoresDebt() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = InMemorySpaceRepository(auth)
        val ledger = InMemoryExpenseRepository(auth)
        runBlocking {
            auth.register("ana", "secret", "Ana")
            spaces.createSpace("Flat", 0)
            spaces.addMember("space-1", Member("bea", "Bea", null, Role.MEMBER))
            ledger.add("space-1", NewExpense("Food", "ana", mapOf("bea" to 1240L), LocalDate(2026, 11, 4)))
        }
        val controlled = ControlledExpenseRepository(ledger)
        val vm = SpaceExpensesViewModel("space-1", controlled, spaces)
        setContent { OrbitTheme { OrbitScreen(scrollable = true) { SpaceExpensesTab("space-1", {}, viewModel = vm) } } }
        onNode(hasText("Ana") and hasText("To receive")).performScrollTo().assertIsDisplayed()
        onNode(hasText("Bea") and hasText("To pay")).performScrollTo().assertIsDisplayed()
        onNode(hasText("Bea") and hasText("Ana") and hasText("Who pays")).performScrollTo().assertIsDisplayed()
        onNodeWithText("Mark as paid").performScrollTo().performClick()
        onNodeWithText("Record this payment?").assertIsDisplayed()
        runOnIdle { assertEquals(0, controlled.writes) }
        onNodeWithContentDescription("Close").performClick()
        onNodeWithText("Record this payment?").assertDoesNotExist()
        runOnIdle { assertEquals(0, controlled.writes) }
        onNodeWithText("Mark as paid").performScrollTo().performClick()
        onAllNodesWithText("Mark as paid").onLast().performScrollTo().performClick()
        onNodeWithText("Record this payment?").assertDoesNotExist()
        onNode(hasText("Ana") and hasText("Settled up")).performScrollTo().assertIsDisplayed()
        onNode(hasText("Bea") and hasText("Settled up")).performScrollTo().assertIsDisplayed()
        onNodeWithText("No payments needed.").performScrollTo().assertIsDisplayed()
        onNodeWithText("Reimbursement").performScrollTo().assertIsDisplayed()
        runOnIdle {
            val entry =
                runBlocking { ledger.observeExpenses("space-1").first().single { it.kind == ExpenseKind.SETTLEMENT } }
            assertEquals(1, controlled.writes)
            assertEquals("bea", entry.paidBy)
            assertEquals(mapOf("ana" to 1240L), entry.split)
            runBlocking { ledger.delete("space-1", entry.id) }
        }
        onNode(hasText("Bea") and hasText("To pay")).performScrollTo().assertIsDisplayed()
        onNode(hasText("Bea") and hasText("Ana") and hasText("Who pays")).performScrollTo().assertIsDisplayed()
        onNodeWithText("Reimbursement").assertDoesNotExist()
    }

    @Test
    fun failedPaymentStaysOpenAndCanBeRetried() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = InMemorySpaceRepository(auth)
        val ledger = InMemoryExpenseRepository(auth)
        runBlocking {
            auth.register("ana", "secret", "Ana")
            spaces.createSpace("Flat", 0)
            spaces.addMember("space-1", Member("bea", "Bea", null, Role.MEMBER))
            ledger.add("space-1", NewExpense("Food", "ana", mapOf("bea" to 1240L), LocalDate(2026, 11, 4)))
        }
        val controlled = ControlledExpenseRepository(ledger).apply { failWrites = true }
        val vm = SpaceExpensesViewModel("space-1", controlled, spaces)
        setContent { OrbitTheme { OrbitScreen(scrollable = true) { SpaceExpensesTab("space-1", {}, viewModel = vm) } } }
        onNodeWithText("Mark as paid").performScrollTo().performClick()
        onAllNodesWithText("Mark as paid").onLast().performScrollTo().performClick()
        onNodeWithText("Could not save the change. Try again.").performScrollTo().assertIsDisplayed()
        onNodeWithText("Record this payment?").assertIsDisplayed()
        runOnIdle {
            assertTrue(vm.uiState.value.balances.payments.isNotEmpty())
            controlled.failWrites = false
        }
        onAllNodesWithText("Mark as paid").onLast().performScrollTo().performClick()
        onNodeWithText("Record this payment?").assertDoesNotExist()
        onNodeWithText("No payments needed.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun emptyLedgerStillShowsCurrentMembersAtZero() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = InMemorySpaceRepository(auth)
        runBlocking {
            auth.register("ana", "secret", "Ana")
            spaces.createSpace("Flat", 0)
        }
        val vm = SpaceExpensesViewModel("space-1", InMemoryExpenseRepository(auth), spaces)
        setContent { OrbitTheme { OrbitScreen(scrollable = true) { SpaceExpensesTab("space-1", {}, viewModel = vm) } } }
        onNode(hasText("Ana") and hasText("Settled up")).performScrollTo().assertIsDisplayed()
        onNodeWithText(formatEuro(0)).assertIsDisplayed()
        onNodeWithText("No payments needed.").performScrollTo().assertIsDisplayed()
        onNodeWithText("No expenses yet").performScrollTo().assertIsDisplayed()
        onNodeWithText("Mark as paid").assertDoesNotExist()
    }
}
