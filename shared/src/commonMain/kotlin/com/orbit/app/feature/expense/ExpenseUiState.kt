package com.orbit.app.feature.expense

import com.orbit.app.core.EURO
import com.orbit.app.core.MoneyLocale
import com.orbit.app.core.euroBeforeAmount
import com.orbit.app.core.parseMoney
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.validateExpense
import com.orbit.app.domain.usecase.splitEqually
import com.orbit.app.domain.usecase.splitExactly
import com.orbit.app.feature.item.ItemFace
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

enum class ExpenseSplit { EQUAL, EXACT }

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
    val spaceName: String = "",
    val title: String = "",
    val amount: String = "",
    val paidBy: String = "",
    val date: LocalDate? = null,
    val mode: ExpenseSplit = ExpenseSplit.EQUAL,
    val selected: Set<String> = emptySet(),
    val exact: Map<String, String> = emptyMap(),
    /** The members, and anyone who left but is still in the expense being edited. */
    val people: List<ItemFace> = emptyList(),
    val locale: MoneyLocale = MoneyLocale(".", ","),
    val original: Expense? = null,
    val member: Member? = null,
    val submitting: Boolean = false,
    val failed: Boolean = false,
    val deleteOpen: Boolean = false,
    val pickingPayer: Boolean = false,
    val pickingDate: Boolean = false,
    val closed: Boolean = false,
) {
    val editing: Boolean get() = original != null

    /** The euro sign on the side of the amount the device writes it. */
    val currencyPrefix: String? get() = EURO.takeIf { euroBeforeAmount() }
    val currencySuffix: String? get() = EURO.takeUnless { euroBeforeAmount() }
    val payer: ItemFace? get() = people.firstOrNull { it.id == paidBy }
    val dateMillis: Long? get() = date?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()
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
    /** What the heading of the split says: how much of the total the shares cover, in either mode. */
    val splitTotals: ExpenseSplitProgress?
        get() = when (mode) {
            ExpenseSplit.EXACT -> splitProgress
            ExpenseSplit.EQUAL -> cents?.let { ExpenseSplitProgress(it, split?.values?.sum() ?: 0L) }
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
    /** What each person's row shows: their part of an equal split, what was typed for them, or nothing. */
    val shares: Map<String, Long>
        get() = selected.associateWith { id ->
            when (mode) {
                ExpenseSplit.EQUAL -> split?.get(id) ?: 0L
                ExpenseSplit.EXACT -> parseMoney(exact[id].orEmpty(), locale) ?: 0L
            }
        }
    val canSave: Boolean
        get() = !loading && !loadFailed && !missing && !submitting && !closed && member != null && date != null &&
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
