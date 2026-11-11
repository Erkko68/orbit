package com.orbit.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ExpenseValidationTest {
    @Test
    fun allocationIncludesEveryDebtAndOnlyKnownSelectedPeople() {
        val split = mapOf("ana" to 1L)
        assertEquals(
            emptyList(), validateExpense(
                "Food", "ana", split, 1, listOf("ana", "ben"),
                mapOf("ana" to true, "ben" to false)
            )
        )
        for (invalid in listOf(emptyMap(), mapOf("ben" to true), mapOf("ana" to true, "unknown" to false))) {
            assertEquals(
                listOf(ExpenseError.InvalidAllocation),
                validateExpense("Food", "ana", split, 1, listOf("ana", "ben"), invalid)
            )
        }
    }


    private val members = listOf("ana", "ben")

    private fun errors(
        title: String = "Groceries",
        paidBy: String = "ana",
        split: Map<String, Long> = mapOf("ana" to 620, "ben" to 620),
        total: Long = 1240,
    ) = validateExpense(title, paidBy, split, total, members)

    @Test
    fun validExpenseHasNoErrors() {
        assertEquals(emptyList(), errors())
    }

    @Test
    fun titleIsTrimmedBeforeChecking() {
        assertEquals(listOf(ExpenseError.BlankTitle), errors(title = "   "))
        assertEquals(emptyList(), errors(title = " " + "a".repeat(120) + " "))
        assertEquals(listOf(ExpenseError.TitleTooLong), errors(title = "a".repeat(121)))
    }

    @Test
    fun thePayerIsAMember() {
        assertEquals(listOf(ExpenseError.PayerNotMember), errors(paidBy = "eve"))
    }

    @Test
    fun anExpenseNeedsAShare() {
        assertEquals(listOf(ExpenseError.NoShares), errors(split = emptyMap()))
    }

    @Test
    fun everyShareIsAboveZero() {
        val split = mapOf("ana" to 1240L, "ben" to 0L)
        assertEquals(listOf(ExpenseError.NonPositiveShare("ben")), errors(split = split))
    }

    @Test
    fun everyShareIsOwedByAMember() {
        val split = mapOf("ana" to 620L, "eve" to 620L)
        assertEquals(listOf(ExpenseError.ShareNotMember("eve")), errors(split = split))
    }

    @Test
    fun theSharesAddUpToTheTotal() {
        assertEquals(listOf(ExpenseError.SharesDoNotAddUp), errors(total = 1241))
    }

    @Test
    fun aSettlementIsPaidByTheDebtorAndOwedByTheCreditor() {
        assertEquals(emptyList(), errors(paidBy = "ben", split = mapOf("ana" to 1240)))
    }
}
