package com.orbit.app.feature.expense

import com.orbit.app.core.MoneyLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExpenseSplitProgressTest {
    private val state = ExpenseUiState(
        mode = ExpenseSplit.EXACT,
        amount = "12,40",
        locale = MoneyLocale(",", "."),
        selected = setOf("ana", "bea"),
        exact = mapOf("ana" to "7,00", "bea" to "3,00"),
    )

    @Test
    fun reportsTheAmountStillToAssignAndTheExcessAndAnExactMatch() {
        val short = requireNotNull(state.splitProgress)
        assertEquals(1000L, short.assigned)
        assertEquals(240L, short.remaining)
        assertNull(short.excess)
        assertFalse(short.matched)
        val over = requireNotNull(state.copy(exact = mapOf("ana" to "7,00", "bea" to "6,00")).splitProgress)
        assertEquals(60L, over.excess)
        assertNull(over.remaining)
        val match = requireNotNull(state.copy(exact = mapOf("ana" to "7,00", "bea" to "5,40")).splitProgress)
        assertTrue(match.matched)
        assertNull(match.remaining)
        assertNull(match.excess)
    }

    @Test
    fun usesOnlySelectedPeopleAndRejectsMissingOrInvalidAmounts() {
        assertEquals(700L, state.copy(selected = setOf("ana")).splitProgress?.assigned)
        assertNull(state.copy(selected = emptySet()).splitProgress)
        assertNull(state.copy(mode = ExpenseSplit.EQUAL).splitProgress)
        assertNull(state.copy(amount = "").splitProgress)
        for (text in listOf("", "-1", "1,234")) {
            val invalid = state.copy(exact = state.exact + ("bea" to text))
            assertNull(invalid.splitProgress)
            assertEquals(setOf("bea"), invalid.invalidShareIds)
        }
        assertTrue(state.invalidShareIds.isEmpty())
    }

    @Test
    fun neverWrapsAnOverflowIntoAMisleadingRemainingAmount() {
        val large = state.copy(
            amount = "92233720368547758,07",
            exact = mapOf("ana" to "92233720368547758,07", "bea" to "0,01"),
        )
        assertNull(large.splitProgress)
        assertEquals(Long.MAX_VALUE, large.copy(exact = large.exact + ("bea" to "0")).splitProgress?.assigned)
    }
}
