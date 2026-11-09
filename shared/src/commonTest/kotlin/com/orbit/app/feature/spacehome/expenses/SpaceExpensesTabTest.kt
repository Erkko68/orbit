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
        val vm = SpaceExpensesViewModel("space-1", ledger, spaces, auth)
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
        onNodeWithText("Paid by Ana", substring = true).performScrollTo().assertIsDisplayed()
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
            ledger.add("space-1", NewExpense("Food", "bea", mapOf("ana" to 1240L), LocalDate(2026, 11, 4)))
        }
        val controlled = ControlledExpenseRepository(ledger)
        val vm = SpaceExpensesViewModel("space-1", controlled, spaces, auth)
        setContent { OrbitTheme { OrbitScreen(scrollable = true) { SpaceExpensesTab("space-1", {}, viewModel = vm) } } }
        onNode(hasText("Ana") and hasText("Owes")).performScrollTo().assertIsDisplayed()
        onNode(hasText("Bea") and hasText("Gets back")).performScrollTo().assertIsDisplayed()
        onNodeWithText("You owe Bea").performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("Mark as paid: You owe Bea").performScrollTo().performClick()
        onNodeWithText("Record this payment?").assertIsDisplayed()
        runOnIdle { assertEquals(0, controlled.writes) }
        onNodeWithContentDescription("Close").performClick()
        onNodeWithText("Record this payment?").assertDoesNotExist()
        runOnIdle { assertEquals(0, controlled.writes) }
        onNodeWithContentDescription("Mark as paid: You owe Bea").performScrollTo().performClick()
        onAllNodesWithText("Mark as paid").onLast().performScrollTo().performClick()
        onNodeWithText("Record this payment?").assertDoesNotExist()
        onNode(hasText("Ana") and hasText("Settled up")).performScrollTo().assertIsDisplayed()
        onNode(hasText("Bea") and hasText("Settled up")).performScrollTo().assertIsDisplayed()
        onNodeWithText("Everyone is settled up.").performScrollTo().assertIsDisplayed()
        onNode(hasText("Ana paid Bea") and hasText("Reimbursement", substring = true)).performScrollTo().assertIsDisplayed()
        runOnIdle {
            val entry =
                runBlocking { ledger.observeExpenses("space-1").first().single { it.kind == ExpenseKind.SETTLEMENT } }
            assertEquals(1, controlled.writes)
            assertEquals("ana", entry.paidBy)
            assertEquals(mapOf("bea" to 1240L), entry.split)
            runBlocking { ledger.delete("space-1", entry.id) }
        }
        onNode(hasText("Bea") and hasText("Gets back")).performScrollTo().assertIsDisplayed()
        onNodeWithText("You owe Bea").performScrollTo().assertIsDisplayed()
        onNodeWithText("Ana paid Bea").assertDoesNotExist()
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
            ledger.add("space-1", NewExpense("Food", "bea", mapOf("ana" to 1240L), LocalDate(2026, 11, 4)))
        }
        val controlled = ControlledExpenseRepository(ledger).apply { failWrites = true }
        val vm = SpaceExpensesViewModel("space-1", controlled, spaces, auth)
        setContent { OrbitTheme { OrbitScreen(scrollable = true) { SpaceExpensesTab("space-1", {}, viewModel = vm) } } }
        onNodeWithContentDescription("Mark as paid: You owe Bea").performScrollTo().performClick()
        onAllNodesWithText("Mark as paid").onLast().performScrollTo().performClick()
        onNodeWithText("Could not save the change. Try again.").performScrollTo().assertIsDisplayed()
        onNodeWithText("Record this payment?").assertIsDisplayed()
        runOnIdle {
            assertTrue(vm.uiState.value.balances.payments.isNotEmpty())
            controlled.failWrites = false
        }
        onAllNodesWithText("Mark as paid").onLast().performScrollTo().performClick()
        onNodeWithText("Record this payment?").assertDoesNotExist()
        onNodeWithText("Everyone is settled up.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun emptyLedgerStillShowsCurrentMembersAtZero() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = InMemorySpaceRepository(auth)
        runBlocking {
            auth.register("ana", "secret", "Ana")
            spaces.createSpace("Flat", 0)
        }
        val vm = SpaceExpensesViewModel("space-1", InMemoryExpenseRepository(auth), spaces, auth)
        setContent { OrbitTheme { OrbitScreen(scrollable = true) { SpaceExpensesTab("space-1", {}, viewModel = vm) } } }
        onNode(hasText("Ana") and hasText("Settled up")).performScrollTo().assertIsDisplayed()
        onNodeWithText(formatEuro(0)).assertIsDisplayed()
        onNodeWithText("Everyone is settled up.").performScrollTo().assertIsDisplayed()
        onNodeWithText("No expenses yet").performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("Mark as paid", substring = true).assertDoesNotExist()
    }

    @Test
    fun moneyOwedToTheUserIsShownWithoutAnythingToConfirm() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = InMemorySpaceRepository(auth)
        val ledger = InMemoryExpenseRepository(auth)
        runBlocking {
            auth.register("ana", "secret", "Ana")
            spaces.createSpace("Flat", 0)
            spaces.addMember("space-1", Member("bea", "Bea", null, Role.MEMBER))
            ledger.add("space-1", NewExpense("Food", "ana", mapOf("bea" to 1240L), LocalDate(2026, 11, 4)))
        }
        val vm = SpaceExpensesViewModel("space-1", ledger, spaces, auth)
        setContent { OrbitTheme { OrbitScreen(scrollable = true) { SpaceExpensesTab("space-1", {}, viewModel = vm) } } }
        onNodeWithText("Bea owes you").performScrollTo().assertIsDisplayed()
        onNodeWithText("Pay").assertDoesNotExist()
        onNodeWithContentDescription("Mark as paid", substring = true).assertDoesNotExist()
        runOnIdle {
            vm.requestSettlement(vm.uiState.value.balances.payments.single())
            assertEquals(null, vm.uiState.value.pendingPayment)
        }
    }
}
