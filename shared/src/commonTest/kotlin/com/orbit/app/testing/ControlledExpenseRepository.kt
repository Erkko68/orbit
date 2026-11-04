package com.orbit.app.testing

import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate

/** Reuses a ledger while independently failing reads/writes or holding a write in flight. */
class ControlledExpenseRepository(private val delegate: ExpenseRepository) : ExpenseRepository {
    var readFailures = 0
    var failWrites = false
    var gate: CompletableDeferred<Unit>? = null
    var writes = 0

    override fun observeExpenses(spaceId: String): Flow<List<Expense>> = flow {
        if (readFailures > 0) {
            readFailures--; error("Ledger unavailable")
        }
        emitAll(delegate.observeExpenses(spaceId))
    }

    private suspend fun beforeWrite() {
        writes++
        gate?.await()
        check(!failWrites) { "Write failed" }
    }

    override suspend fun add(spaceId: String, expense: NewExpense): String {
        beforeWrite()
        return delegate.add(spaceId, expense)
    }

    override suspend fun edit(
        spaceId: String,
        expenseId: String,
        title: String,
        paidBy: String,
        split: Map<String, Long>,
        date: LocalDate
    ) {
        beforeWrite()
        delegate.edit(spaceId, expenseId, title, paidBy, split, date)
    }

    override suspend fun delete(spaceId: String, expenseId: String) {
        beforeWrite()
        delegate.delete(spaceId, expenseId)
    }
}
