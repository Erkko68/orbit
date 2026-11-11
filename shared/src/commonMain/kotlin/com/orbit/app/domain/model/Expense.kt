package com.orbit.app.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

enum class ExpenseKind { EXPENSE, SETTLEMENT }

/**
 * An entry of a space's ledger, as read from Firestore. Amounts are in cents, in euros until the space has a
 * currency (#111). [split] says what each member owes and is the only amount stored (ADR 0003). A settlement is
 * an entry too: paid by the debtor and owed by the creditor.
 */
data class Expense(
    val id: String,
    val title: String,
    val paidBy: String,
    val split: Map<String, Long>,
    val date: LocalDate,
    val createdBy: String,
    val createdAt: Instant,
    val kind: ExpenseKind = ExpenseKind.EXPENSE,
    /** The item whose Cost module made this expense (#58). */
    val itemId: String? = null,
    /** Selected people, including zero shares: true is automatic, false is manual. Never a monetary source. */
    val customAllocation: Map<String, Boolean>? = null,
) {
    val total: Long get() = split.values.sum()
}

/** What a person or an item's Cost module fills in. The repository adds id, creator and timestamps. */
data class NewExpense(
    val title: String,
    val paidBy: String,
    val split: Map<String, Long>,
    val date: LocalDate,
    val kind: ExpenseKind = ExpenseKind.EXPENSE,
    val itemId: String? = null,
    val customAllocation: Map<String, Boolean>? = null,
)

/** By date, the latest first, and the entry added last first within a day. */
fun List<Expense>.newestFirst(): List<Expense> =
    sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAt })
