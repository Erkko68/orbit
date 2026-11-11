package com.orbit.app.data.mapper

import co.touchlab.kermit.Logger
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.ExpenseKind
import com.orbit.app.domain.model.NewExpense
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.LocalDate

/** The value of [kind] in the `kind` field. */
private fun kindValue(kind: ExpenseKind): String = kind.name.lowercase()

/** The new expense, made by [creatorId]. */
internal fun expenseDocument(spaceId: String, expense: NewExpense, creatorId: String) = ExpenseDocument(
    spaceId = spaceId,
    title = expense.title,
    paidBy = expense.paidBy,
    split = expense.split,
    date = expense.date.toString(),
    createdBy = creatorId,
    createdAt = Timestamp.ServerTimestamp,
    kind = kindValue(expense.kind),
    itemId = expense.itemId,
    customAllocation = expense.customAllocation,
)

/**
 * An expense from its stored fields, or null when a required one is missing or its split is empty: a malformed
 * document is skipped by the caller. An unknown kind still moves money, so it reads as an expense.
 */
internal fun mapExpense(id: String, stored: StoredExpense): Expense? {
    val title = stored.title ?: return null
    val paidBy = stored.paidBy ?: return null
    val split = stored.split?.takeIf { it.isNotEmpty() } ?: return null
    val date = dateOf(stored.date) ?: return null
    val createdBy = stored.createdBy ?: return null
    val createdAt = stored.createdAt ?: return null
    return Expense(
        id = id,
        title = title,
        paidBy = paidBy,
        split = split,
        date = date,
        createdBy = createdBy,
        createdAt = createdAt,
        kind = kindOf(id, stored.kind),
        itemId = stored.itemId,
        customAllocation = stored.customAllocation?.takeIf {
            it.isNotEmpty() && it.keys.all(String::isNotBlank) && it.keys.containsAll(split.keys)
        },
    )
}

private fun dateOf(date: String?): LocalDate? = try {
    date?.let(LocalDate::parse)
} catch (_: IllegalArgumentException) {
    null
}

private fun kindOf(id: String, kind: String?): ExpenseKind {
    val known = ExpenseKind.entries.firstOrNull { kindValue(it) == kind }
    if (known == null) Logger.w { "Expense $id has the unknown kind $kind, read as an expense" }
    return known ?: ExpenseKind.EXPENSE
}
