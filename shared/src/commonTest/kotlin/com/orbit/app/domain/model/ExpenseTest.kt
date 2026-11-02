package com.orbit.app.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ExpenseTest {
    private fun expense(id: String, date: LocalDate, addedAt: Long) = Expense(
        id = id,
        title = "Groceries",
        paidBy = "ana",
        split = mapOf("ben" to 100),
        date = date,
        createdBy = "ana",
        createdAt = Instant.fromEpochSeconds(addedAt),
    )

    @Test
    fun theLatestDateComesFirstAndTheLastAddedWithinADay() {
        val friday = LocalDate(2026, 10, 9)
        val sunday = LocalDate(2026, 10, 11)
        val ledger = listOf(expense("old", friday, 300), expense("first", sunday, 100), expense("second", sunday, 200))

        assertEquals(listOf("second", "first", "old"), ledger.newestFirst().map { it.id })
    }

    @Test
    fun theTotalIsTheSumOfTheShares() {
        val split = mapOf("ana" to 620L, "ben" to 621L)
        assertEquals(1241, expense("e1", LocalDate(2026, 10, 9), 100).copy(split = split).total)
    }
}
