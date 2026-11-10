package com.orbit.app.feature.expense

import com.orbit.app.feature.item.ItemFace
import com.orbit.app.core.MoneyLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExpenseSplitProgressTest {
    @Test
    fun zeroCentSharesStayVisibleButAreNotStored() {
        val tiny = ExpenseUiState(
            amount = "0.01", mode = ExpenseSplit.WEIGHTS,
            selected = setOf("a", "b"), people = listOf(ItemFace("a", "A", 0, null), ItemFace("b", "B", 1, null)),
        )
        assertEquals(mapOf("a" to 1L), tiny.split)
        assertEquals(mapOf("a" to 1L, "b" to 0L), tiny.previewShares)
        val percent = tiny.copy(
            mode = ExpenseSplit.EXACT, unit = ExpenseUnit.PERCENT,
            percentReference = com.orbit.app.domain.usecase.ExpensePercentages(
                10_000,
                mapOf("a" to 5000L, "b" to 5000L)
            )
        )
        assertEquals(tiny.previewShares, percent.previewShares)
    }

    @Test
    fun percentagesFollowRegionalSeparatorsAndRejectSymbolsPrecisionAndOverflow() {
        val percent = ExpenseUiState(
            mode = ExpenseSplit.EXACT, unit = ExpenseUnit.PERCENT,
            amount = "40,00", locale = MoneyLocale(",", "."),
            selected = setOf("a", "b"), people = listOf(ItemFace("a", "A", 0, null), ItemFace("b", "B", 1, null)),
            percentReference = com.orbit.app.domain.usecase.ExpensePercentages(4000, mapOf("a" to 2000L, "b" to 2000L)),
            percentages = mapOf("a" to "25,50", "b" to "74,50"),
        )
        assertEquals(mapOf("a" to 1020L, "b" to 2980L), percent.split)
        assertTrue(percent.percentagesMatched)
        for (text in listOf("25.50", "25,501", "25%", "-1", "101", "", "9223372036854775808")) {
            val invalid = percent.copy(percentages = percent.percentages + ("a" to text))
            assertNull(invalid.split)
            assertTrue("a" in invalid.invalidShareIds)
        }
        val english = percent.copy(
            amount = "40.00",
            locale = MoneyLocale(".", ","),
            percentages = mapOf("a" to "25.50", "b" to "74.50")
        )
        assertEquals(percent.split, english.split)
        val excluded = percent.copy(selected = setOf("b"), percentages = percent.percentages + ("a" to "invalid"))
        assertTrue(excluded.invalidShareIds.isEmpty())
        assertEquals(7450L, excluded.percentageProgress?.whole)
    }

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
