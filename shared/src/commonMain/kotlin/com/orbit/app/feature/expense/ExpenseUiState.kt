package com.orbit.app.feature.expense

import com.orbit.app.core.MoneyLocale
import com.orbit.app.core.parseMoney
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.validateExpense
import com.orbit.app.domain.usecase.splitEqually
import com.orbit.app.domain.usecase.splitExactly
import kotlinx.datetime.LocalDate

enum class ExpenseSplit { EQUAL, EXACT }

data class ExpensePerson(val id: String, val name: String)

/** A preview of the user's exact shares. These amounts are never persisted separately. */
data class ExpenseSplitProgress(val total: Long, val assigned: Long) {
    val remaining: Long? get() = (total - assigned).takeIf { it > 0 }
    val excess: Long? get() = (assigned - total).takeIf { it > 0 }
    val matched: Boolean get() = assigned == total
}

data class ExpenseUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val missing: Boolean = false,
    val accent: Int? = null,
    val title: String = "",
    val amount: String = "",
    val paidBy: String = "",
    val date: String = "",
    val mode: ExpenseSplit = ExpenseSplit.EQUAL,
    val selected: Set<String> = emptySet(),
    val exact: Map<String, String> = emptyMap(),
    val people: List<ExpensePerson> = emptyList(),
    val locale: MoneyLocale = MoneyLocale(".", ","),
    val original: Expense? = null,
    val member: Member? = null,
    val submitting: Boolean = false,
    val failed: Boolean = false,
    val deleteOpen: Boolean = false,
    val closed: Boolean = false,
) {
    val editing: Boolean get() = original != null
    val parsedDate: LocalDate? get() = runCatching { LocalDate.parse(date) }.getOrNull()
    val cents: Long? get() = parseMoney(amount, locale)?.takeIf { it > 0 }
    val invalidShareIds: Set<String>
        get() = selected.filter { parseMoney(exact[it].orEmpty(), locale) == null }.toSet()
    val splitProgress: ExpenseSplitProgress?
        get() {
            if (mode != ExpenseSplit.EXACT || selected.isEmpty()) return null
            val total = cents ?: return null
            var assigned = 0L
            for (id in selected) {
                val share = parseMoney(exact[id].orEmpty(), locale) ?: return null
                if (share > Long.MAX_VALUE - assigned) return null
                assigned += share
            }
            return ExpenseSplitProgress(total, assigned)
        }
    val split: Map<String, Long>?
        get() {
            val total = cents ?: return null
            if (selected.isEmpty() || selected.any { id -> people.none { it.id == id } }) return null
            return when (mode) {
                ExpenseSplit.EQUAL -> splitEqually(total, selected)
                ExpenseSplit.EXACT -> {
                    val shares = selected.associateWith { parseMoney(exact[it].orEmpty(), locale) ?: return null }
                    splitExactly(total, shares)
                }
            }
        }
    val canSave: Boolean
        get() = !loading && !loadFailed && !missing && !submitting && !closed && member != null && parsedDate != null &&
                split?.let {
                    validateExpense(
                        title,
                        paidBy,
                        it,
                        cents ?: 0,
                        people.map { person -> person.id }).isEmpty()
                } == true
    val canDelete: Boolean
        get() = original?.let { member != null && (it.createdBy == member.userId || member.role.managesSpace) } == true &&
                !loading && !loadFailed && !missing && !submitting && !closed
}
