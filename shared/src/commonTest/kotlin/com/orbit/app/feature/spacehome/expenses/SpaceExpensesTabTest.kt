package com.orbit.app.feature.spacehome.expenses

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.formatEuro
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

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
        setContent { OrbitTheme { SpaceExpensesTab("space-1", { opened = it }, viewModel = vm) } }
        onNodeWithText("${formatEuro(1240)} — Total").assertIsDisplayed()
        onNodeWithText("Paid by: Ana").assertIsDisplayed()
        onNodeWithText("Date:", substring = true).assertIsDisplayed()
        onNodeWithText("Food").assertIsDisplayed().performClick()
        assertEquals(id, opened)
    }
}
