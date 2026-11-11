package com.orbit.app.feature.expense

import com.orbit.app.core.EURO
import com.orbit.app.core.MoneyLocale
import com.orbit.app.core.euroBeforeAmount
import com.orbit.app.core.parseMoney
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.validateExpense
import com.orbit.app.domain.usecase.HUNDRED_PERCENT
import com.orbit.app.domain.usecase.splitByPercentages
import com.orbit.app.domain.usecase.splitByWeights
import com.orbit.app.domain.usecase.splitEqually
import com.orbit.app.domain.usecase.splitExactly
import com.orbit.app.feature.item.ItemFace
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

enum class ExpenseSplit { EQUAL, EXACT, PERCENTAGES, WEIGHTS }

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
    val spaceName: String = "",
    val title: String = "",
    val amount: String = "",
    val paidBy: String = "",
    val date: LocalDate? = null,
    val mode: ExpenseSplit = ExpenseSplit.EQUAL,
    val selected: Set<String> = emptySet(),
    val exact: Map<String, String> = emptyMap(),
    /** Typed percentages and weights. Neither is stored: they only become cents, as the custom amounts do. */
    val percentages: Map<String, String> = emptyMap(),
    val weights: Map<String, String> = emptyMap(),
    /** The members, and anyone who left but is still in the expense being edited. */
    val people: List<ItemFace> = emptyList(),
    val locale: MoneyLocale = MoneyLocale(".", ","),
    val original: Expense? = null,
    val member: Member? = null,
    val submitting: Boolean = false,
    val failed: Boolean = false,
    val deleteOpen: Boolean = false,
    val pickingPayer: Boolean = false,
    val pickingSplit: Boolean = false,
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

    /** What the empty amount shows: nothing yet, written the way the device writes money. */
    val amountPlaceholder: String get() = "0${locale.decimal}00"

    /** Something is typed that is not an amount above zero. */
    val amountInvalid: Boolean get() = amount.isNotEmpty() && cents == null

    /** What each person's field shows in the current mode. A weight left alone is 1. */
    val shareInputs: Map<String, String>
        get() = selected.associateWith { id ->
            when (mode) {
                ExpenseSplit.EQUAL -> ""
                ExpenseSplit.EXACT -> exact[id].orEmpty()
                ExpenseSplit.PERCENTAGES -> percentages[id].orEmpty()
                ExpenseSplit.WEIGHTS -> weights[id] ?: "1"
            }
        }

    /** A typed field as a number: cents, hundredths of a percent or a whole weight. Null when it is not valid. */
    private fun typed(id: String): Long? {
        val text = shareInputs[id] ?: return null
        return when (mode) {
            ExpenseSplit.EQUAL -> null
            ExpenseSplit.EXACT -> parseMoney(text, locale)
            ExpenseSplit.PERCENTAGES -> parseMoney(text, locale)?.takeIf { it <= HUNDRED_PERCENT }
            ExpenseSplit.WEIGHTS -> text.trim().toLongOrNull()?.takeIf { it >= 0 }
        }
    }

    val invalidShareIds: Set<String>
        get() = if (mode == ExpenseSplit.EQUAL) emptySet() else selected.filter { typed(it) == null }.toSet()

    /** How much of the whole the typed fields cover: the total in cents, or 100 % in hundredths. */
    val splitProgress: ExpenseSplitProgress?
        get() {
            if (selected.isEmpty()) return null
            val whole = when (mode) {
                ExpenseSplit.EXACT -> cents ?: return null
                ExpenseSplit.PERCENTAGES -> HUNDRED_PERCENT
                else -> return null
            }
            var assigned = 0L
            for (id in selected) {
                val share = typed(id) ?: return null
                if (share > Long.MAX_VALUE - assigned) return null
                assigned += share
            }
            return ExpenseSplitProgress(whole, assigned)
        }
    /** What the heading of the split says: how much of the total the shares cover, in any mode. */
    val splitTotals: ExpenseSplitProgress?
        get() = if (mode == ExpenseSplit.EXACT) {
            splitProgress
        } else {
            cents?.let { ExpenseSplitProgress(it, split?.values?.sum() ?: 0L) }
        }
    val split: Map<String, Long>?
        get() {
            val total = cents ?: return null
            if (selected.isEmpty() || selected.any { id -> people.none { it.id == id } }) return null
            if (mode == ExpenseSplit.EQUAL) return splitEqually(total, selected)
            val values = selected.associateWith { typed(it) ?: return null }
            return when (mode) {
                ExpenseSplit.EQUAL, ExpenseSplit.EXACT -> splitExactly(total, values)
                ExpenseSplit.PERCENTAGES -> splitByPercentages(total, values)
                ExpenseSplit.WEIGHTS -> splitByWeights(total, values)
            }
        }
    /** What each person's row shows: what was typed with custom amounts, else their part of the split, or nothing. */
    val shares: Map<String, Long>
        get() = selected.associateWith { id ->
            if (mode == ExpenseSplit.EXACT) typed(id) ?: 0L else split?.get(id) ?: 0L
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
