package com.orbit.app.domain.repository

import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.NewExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * The ledger of a space. Callers check `validateExpense` first. Acts as the signed-in user. Writes return once
 * applied locally, without waiting for the server, so they work offline. They throw on failure.
 */
interface ExpenseRepository {
    /** Every entry of the ledger, newest first. Balances are derived from it (ADR 0003). */
    fun observeExpenses(spaceId: String): Flow<List<Expense>>

    /** Returns the new expense's id. */
    suspend fun add(spaceId: String, expense: NewExpense): String

    /** Replaces what a person can edit. The split goes whole, so a member taken out of it leaves no share. */
    suspend fun edit(
        spaceId: String,
        expenseId: String,
        title: String,
        paidBy: String,
        split: Map<String, Long>,
        date: LocalDate,
    )

    suspend fun delete(spaceId: String, expenseId: String)
}
