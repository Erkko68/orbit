package com.orbit.app.feature.expense.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.deviceMoneyLocale
import com.orbit.app.core.formatEuro
import com.orbit.app.core.moneyInput
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.model.Role
import com.orbit.app.feature.expense.ExpenseViewModel
import com.orbit.app.testing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ExpenseScreenTest : ComposeTest() {
    @Test
    fun percentagesAndWeightsShowWhatEachPersonPays() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER, "bea" to Role.MEMBER, "marc" to Role.MEMBER)
        val ledger = InMemoryExpenseRepository(auth)
        val locale = deviceMoneyLocale()
        runBlocking { auth.register("ana", "secret", "Ana") }
        val vm = ExpenseViewModel("space-1", "", ledger, spaces, auth)
        setContent { OrbitTheme { ExpenseScreen("space-1", "", {}, vm) } }
        onNode(hasSetTextAction() and hasContentDescription("Amount")).performScrollTo().performTextInput(moneyInput(4000, locale))
        onNode(hasSetTextAction() and hasContentDescription("Title")).performTextInput("Food")
        onNodeWithText("Split").performScrollTo().performClick()
        onNodeWithText("Whole numbers, such as 2 : 1 : 1.").assertIsDisplayed()
        onNodeWithText("Weights").performClick()
        onNodeWithContentDescription("Weight for ana").performScrollTo().performTextReplacement("2")
        onNodeWithText(formatEuro(2000)).performScrollTo().assertIsDisplayed()
        onNodeWithText("Split").performScrollTo().performClick()
        onNodeWithText("Percentages").performClick()
        onNodeWithContentDescription("Share for marc").performScrollTo().performTextReplacement("24")
        onNodeWithText("Still to assign: ${moneyInput(100, locale)}%").performScrollTo().assertIsDisplayed()
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithContentDescription("Share for marc").performScrollTo().performTextReplacement("25")
        onNodeWithText("Save").assertIsEnabled().performClick()
        assertEquals(
            mapOf("ana" to 2000L, "bea" to 1000L, "marc" to 1000L),
            runBlocking { ledger.observeExpenses("space-1").first().single().split },
        )
    }

    @Test
    fun customSharesExplainMissingAndExcessAmountsBeforeSaving() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER, "bea" to Role.MEMBER)
        val ledger = InMemoryExpenseRepository(auth)
        val locale = deviceMoneyLocale()
        val id = runBlocking {
            auth.register("ana", "secret", "Ana")
            ledger.add(
                "space-1",
                NewExpense("Food", "ana", mapOf("ana" to 620L, "bea" to 620L), LocalDate(2026, 11, 4)),
            )
        }
        val vm = ExpenseViewModel("space-1", id, ledger, spaces, auth)
        setContent { OrbitTheme { ExpenseScreen("space-1", id, {}, vm) } }
        onNodeWithText("Custom amounts").performScrollTo().assertIsDisplayed()
        onNodeWithText("Assigned ${formatEuro(1240)} of ${formatEuro(1240)}").performScrollTo().assertIsDisplayed()
        onAllNodes(hasSetTextAction() and hasText(moneyInput(620, locale))).onFirst()
            .performScrollTo().performTextReplacement(moneyInput(700, locale))
        onNodeWithText("Over the total by ${formatEuro(80)}. Reduce the shares.").performScrollTo().assertIsDisplayed()
        onNodeWithText("Save").assertIsNotEnabled()
        onNode(hasSetTextAction() and hasText(moneyInput(620, locale)))
            .performScrollTo().performTextReplacement(moneyInput(300, locale))
        onNodeWithText("Assigned ${formatEuro(1000)} of ${formatEuro(1240)}").performScrollTo().assertIsDisplayed()
        onNodeWithText("Still to assign: ${formatEuro(240)}").performScrollTo().assertIsDisplayed()
        onNodeWithText("Save").assertIsNotEnabled()
        onNode(hasSetTextAction() and hasText(moneyInput(300, locale)))
            .performScrollTo().performTextReplacement(moneyInput(540, locale))
        onNodeWithText("Assigned ${formatEuro(1240)} of ${formatEuro(1240)}").performScrollTo().assertIsDisplayed()
        onNodeWithText("Save").assertIsEnabled().performClick()
        assertEquals(
            mapOf("ana" to 700L, "bea" to 540L),
            runBlocking { ledger.observeExpenses("space-1").first().single().split },
        )
    }

    @Test
    fun editsExactSharesAndDeletesOnlyAfterConfirming() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = InMemorySpaceRepository(auth)
        val ledger = InMemoryExpenseRepository(auth)
        val id = runBlocking {
            auth.register("ana", "secret", "Ana")
            spaces.createSpace("Flat", 0)
            ledger.add("space-1", NewExpense("Food", "ana", mapOf("ana" to 1240L), LocalDate(2026, 11, 4)))
        }
        val vm = ExpenseViewModel("space-1", id, ledger, spaces, auth)
        setContent { OrbitTheme { ExpenseScreen("space-1", id, {}, vm) } }
        onNode(hasSetTextAction() and hasContentDescription("Title")).performTextReplacement("Dinner")
        onNodeWithText("Custom amounts").performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("Delete").performClick()
        onNodeWithText("Delete this entry?").assertIsDisplayed()
        assertEquals(1, runBlocking { ledger.observeExpenses("space-1").first().size })
        onNodeWithText("Delete").performScrollTo().performClick()
        waitForIdle()
        assertTrue(runBlocking { ledger.observeExpenses("space-1").first().isEmpty() })
    }

    @Test
    fun addsAnExpenseOnlyAfterSaveAndReturnsToTheLedger() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = InMemorySpaceRepository(auth)
        val ledger = InMemoryExpenseRepository(auth)
        runBlocking { auth.register("ana", "secret", "Ana"); spaces.createSpace("Flat", 0) }
        val vm = ExpenseViewModel("space-1", "", ledger, spaces, auth)
        var closed = false
        setContent { OrbitTheme { ExpenseScreen("space-1", "", { closed = true }, vm) } }
        onNode(hasSetTextAction() and hasContentDescription("Amount")).performScrollTo()
            .performTextInput(moneyInput(1240, deviceMoneyLocale()))
        onNode(hasSetTextAction() and hasContentDescription("Title")).performTextInput("Food")
        assertTrue(runBlocking { ledger.observeExpenses("space-1").first().isEmpty() })
        onNodeWithText("Save").performClick()
        waitForIdle()
        assertTrue(closed)
        val saved = runBlocking { ledger.observeExpenses("space-1").first().single() }
        assertEquals("Food", saved.title)
        assertEquals(1240L, saved.total)
    }

    @Test
    fun showsEqualSharesUnderParticipantsAndUpdatesThemWhenSomeoneIsExcluded() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER, "bea" to Role.MEMBER, "marc" to Role.MEMBER)
        val ledger = InMemoryExpenseRepository(auth)
        runBlocking { auth.register("ana", "secret", "Ana") }
        val vm = ExpenseViewModel("space-1", "", ledger, spaces, auth)
        setContent { OrbitTheme { ExpenseScreen("space-1", "", {}, vm) } }
        onNode(hasSetTextAction() and hasContentDescription("Amount")).performScrollTo()
            .performTextInput(moneyInput(1000, deviceMoneyLocale()))
        onNodeWithText(formatEuro(334)).performScrollTo().assertIsDisplayed()
        onAllNodesWithText(formatEuro(333)).onFirst().performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("marc").performScrollTo().performClick()
        onAllNodesWithText(formatEuro(500)).onFirst().performScrollTo().assertIsDisplayed()
        onNodeWithText(formatEuro(0)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun picksThePayerAndTheDateFromTheirSheets() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER, "bea" to Role.MEMBER)
        val ledger = InMemoryExpenseRepository(auth)
        runBlocking { auth.register("ana", "secret", "Ana") }
        val vm = ExpenseViewModel("space-1", "", ledger, spaces, auth)
        setContent { OrbitTheme { ExpenseScreen("space-1", "", {}, vm) } }
        onNode(hasText("Paid by") and hasText("ana")).performScrollTo().performClick()
        onAllNodesWithText("bea").onLast().performClick()
        waitForIdle()
        assertEquals("bea", vm.uiState.value.paidBy)
        onNodeWithText("Date").performScrollTo().performClick()
        assertTrue(vm.uiState.value.pickingDate)
        onNodeWithContentDescription("Close").performClick()
        waitForIdle()
        assertTrue(!vm.uiState.value.pickingDate)
    }

    @Test
    fun failedSaveKeepsTheFormAndShowsAnError() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = InMemorySpaceRepository(auth)
        runBlocking { auth.register("ana", "secret", "Ana"); spaces.createSpace("Flat", 0) }
        val ledger = ControlledExpenseRepository(InMemoryExpenseRepository(auth)).apply { failWrites = true }
        val vm = ExpenseViewModel("space-1", "", ledger, spaces, auth)
        setContent { OrbitTheme { ExpenseScreen("space-1", "", {}, vm) } }
        onNode(hasSetTextAction() and hasContentDescription("Amount")).performScrollTo()
            .performTextInput(moneyInput(1240, deviceMoneyLocale()))
        onNode(hasSetTextAction() and hasContentDescription("Title")).performTextInput("Food")
        onNodeWithText("Save").performClick()
        onNodeWithText("Could not save the change. Try again.").performScrollTo().assertIsDisplayed()
        assertEquals("Food", vm.uiState.value.title)
    }
}
