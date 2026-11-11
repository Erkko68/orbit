package com.orbit.app.testing

import com.orbit.app.data.repository.requireUser
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.model.newestFirst
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/** Ledgers in memory, with ids in creation order across spaces. */
class InMemoryExpenseRepository(private val auth: AuthRepository) : ExpenseRepository {
    /** The expenses of each space, by its id. */
    private val expenses = MutableStateFlow(emptyMap<String, List<Expense>>())
    private var created = 0

    override fun observeExpenses(spaceId: String): Flow<List<Expense>> =
        expenses.map { it[spaceId].orEmpty().newestFirst() }

    override suspend fun add(spaceId: String, expense: NewExpense): String {
        val id = "expense-${++created}"
        val stored = Expense(
            id = id,
            title = expense.title,
            paidBy = expense.paidBy,
            split = expense.split,
            date = expense.date,
            createdBy = auth.requireUser().id,
            createdAt = Clock.System.now(),
            kind = expense.kind,
            itemId = expense.itemId,
            customAllocation = expense.customAllocation,
        )
        expenses.update { it + (spaceId to it[spaceId].orEmpty() + stored) }
        return id
    }

    override suspend fun edit(
        spaceId: String,
        expenseId: String,
        title: String,
        paidBy: String,
        split: Map<String, Long>,
        date: LocalDate,
        customAllocation: Map<String, Boolean>?,
    ) {
        val edited = expenses.value[spaceId].orEmpty().map {
            if (it.id == expenseId) it.copy(
                title = title, paidBy = paidBy, split = split, date = date,
                customAllocation = customAllocation
            ) else it
        }
        expenses.update { it + (spaceId to edited) }
    }

    override suspend fun delete(spaceId: String, expenseId: String) {
        expenses.update { it + (spaceId to it[spaceId].orEmpty().filterNot { expense -> expense.id == expenseId }) }
    }
}
