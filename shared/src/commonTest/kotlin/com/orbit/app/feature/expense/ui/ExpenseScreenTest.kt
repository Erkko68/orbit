package com.orbit.app.feature.expense.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.deviceMoneyLocale
import com.orbit.app.core.formatEuro
import com.orbit.app.core.moneyInput
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.model.Role
import com.orbit.app.feature.expense.ExpenseViewModel
import com.orbit.app.testing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class ExpenseScreenTest : ComposeTest() {
    @Test
    fun narrowScreenKeepsNamesAndActionsReadableAtNormalSize() = verifyNarrowRows(1f)

    @Test
    fun narrowScreenKeepsNamesAndActionsReadableWithLargerText() = verifyNarrowRows(1.5f)

    private fun verifyNarrowRows(fontScale: Float) = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val spaces = FakeSpaceRepository(
            Space("space-1", "Flat", 0, listOf("ana", "bea")), listOf(
                Member("ana", "Valerio Bianchi", null, Role.OWNER),
                Member("bea", "Alessandra Maria", null, Role.MEMBER),
            )
        )
        val ledger = InMemoryExpenseRepository(auth)
        val id = runBlocking {
            auth.register("ana", "secret", "Valerio Bianchi")
            ledger.add(
                "space-1", NewExpense(
                    "Food", "ana", mapOf("ana" to 1600L, "bea" to 2400L),
                    LocalDate(2026, 11, 1), customAllocation = mapOf("ana" to false, "bea" to true)
                )
            )
        }
        val vm = ExpenseViewModel("space-1", id, ledger, spaces, auth)
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                OrbitTheme {
                    // A narrow phone viewport, using existing tokens rather than changing the app's typography.
                    Box(Modifier.width(OrbitSpacing.xl * 10)) { ExpenseScreen("space-1", id, {}, vm) }
                }
            }
        }
        // Main's one-line row: the undo icon sits inside the field, and the field stays on screen.
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val field = onNodeWithContentDescription("Share for Valerio Bianchi").fetchSemanticsNode().boundsInRoot
        val reset = onNodeWithContentDescription("Make Valerio Bianchi automatic").fetchSemanticsNode().boundsInRoot
        assertTrue(reset.left >= field.left && reset.right <= field.right, "The undo icon is inside the field")
        assertTrue(field.right <= root.right)
        onNodeWithContentDescription("Automatic share for Alessandra Maria").assertExists()
    }

    @Test
    fun automaticRowsStayEditableAndTheExplicitActionReleasesAManualShare() = runComposeUiTest {
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
        onNodeWithText("Custom amounts").performClick()
        onNodeWithContentDescription("Automatic share for ana").performScrollTo().performClick()
        assertEquals(setOf("ana", "bea", "marc"), vm.uiState.value.automatic)
        onNodeWithContentDescription("Automatic share for ana").performTextInput("16")
        onNodeWithContentDescription("Make ana automatic").assertExists()
        assertEquals(mapOf("ana" to 1600L, "bea" to 1200L, "marc" to 1200L), vm.uiState.value.split)
        onNodeWithContentDescription("Automatic share for bea").performScrollTo().performTextInput("10")
        onNodeWithContentDescription("Make ana automatic").performScrollTo().performClick()
        assertEquals(setOf("ana", "marc"), vm.uiState.value.automatic)
        onNodeWithContentDescription("Share for bea").performScrollTo().performTextInput("0")
        // Typing again in the same row retains focus and text while automatic neighbours update.
        assertTrue("bea" !in vm.uiState.value.automatic)
        onNodeWithContentDescription("Share for bea").assertIsFocused()
        onNodeWithContentDescription("Share for bea").performTextReplacement("10")
        // Save is in the fixed bottom action, outside the scrolling form.
        onNodeWithText("Save").assertIsDisplayed().performClick()
        val saved = runBlocking { ledger.observeExpenses("space-1").first().single() }
        assertEquals(mapOf("ana" to true, "bea" to false, "marc" to true), saved.customAllocation)
    }

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
