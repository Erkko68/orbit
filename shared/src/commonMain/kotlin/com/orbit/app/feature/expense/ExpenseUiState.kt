package com.orbit.app.feature.expense

import com.orbit.app.core.EURO
import com.orbit.app.core.MoneyLocale
import com.orbit.app.core.euroBeforeAmount
import com.orbit.app.core.parseMoney
import com.orbit.app.core.moneyInput
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.validateExpense
import com.orbit.app.domain.usecase.splitEqually
import com.orbit.app.domain.usecase.splitExactly
import com.orbit.app.domain.usecase.ExpensePercentages
import com.orbit.app.domain.usecase.ShareDivision
import com.orbit.app.domain.usecase.divideShare
import com.orbit.app.domain.usecase.splitByWeights
import com.orbit.app.feature.item.ItemFace
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

enum class ExpenseSplit { EQUAL, EXACT, WEIGHTS }

enum class ExpenseUnit { MONEY, PERCENT }

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
    val unit: ExpenseUnit = ExpenseUnit.MONEY,
    val weights: Map<String, String> = emptyMap(),
    val percentReference: ExpensePercentages? = null,
    val percentages: Map<String, String> = emptyMap(),
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
    val canChangeUnit: Boolean get() = mode == ExpenseSplit.EXACT && cents != null && !submitting
    val percentageValues: ExpensePercentages?
        get() {
            val reference = percentReference ?: return null
            val entered = percentages.filterKeys { it in selected }.mapValues {
                parseMoney(it.value, locale.copy(grouping = ""))?.takeIf { it <= 10_000 } ?: return null
            }
            return reference.copy(hundredths = entered)
        }
    val percentageProgress: ShareDivision? get() = percentageValues?.progress(selected)
    val percentageDifference: Long? get() = percentageProgress?.let { kotlin.math.abs(10_000 - it.whole) }
    val percentageDifferenceSmall: Boolean
        get() = percentageProgress?.let { it.remainder != 0L && it.whole in 9999L..10_000L } == true
    val percentagesMatched: Boolean get() = percentageProgress == ShareDivision(10_000, 0)
    val percentageExcess: Boolean
        get() = percentageProgress?.let { it.whole > 10_000 || it.whole == 10_000L && it.remainder > 0 } == true
    val shareInputs: Map<String, String>
        get() = selected.associateWith { id ->
            when {
                mode == ExpenseSplit.WEIGHTS -> weights[id] ?: "1"
                unit == ExpenseUnit.MONEY -> exact[id].orEmpty()
                else -> percentages[id] ?: percentReference?.percentage(id)?.let { moneyInput(it.whole, locale) }
                    .orEmpty()
            }
        }
    val previewShares: Map<String, Long>
        get() = when {
            mode == ExpenseSplit.EQUAL || mode == ExpenseSplit.WEIGHTS -> completePreview(split)
            unit == ExpenseUnit.PERCENT -> split?.let { completePreview(it) } ?: selected.mapNotNull { id ->
                val total = cents ?: return@mapNotNull null
                val reference = percentReference ?: return@mapNotNull null
                val typed = percentages[id]
                val entered = if (typed == null) emptyMap() else mapOf(
                    id to (
                            parseMoney(typed, locale.copy(grouping = ""))?.takeIf { it <= 10_000 }
                                ?: return@mapNotNull null))
                reference.copy(hundredths = entered).preview(total, setOf(id))?.get(id)?.let { id to it }
            }.toMap()

            else -> selected.mapNotNull { id -> parseMoney(exact[id].orEmpty(), locale)?.let { id to it } }.toMap()
        }

    private fun completePreview(shares: Map<String, Long>?): Map<String, Long> =
        if (shares == null) emptyMap() else selected.associateWith { shares[it] ?: 0L }

    fun percentageLabel(id: String): Pair<String, Boolean>? {
        val value = if (unit == ExpenseUnit.PERCENT && mode == ExpenseSplit.EXACT) {
            percentageValues?.percentage(id)
        } else {
            val total = cents ?: return null
            val share = parseMoney(exact[id].orEmpty(), locale) ?: return null
            divideShare(share, 10_000, total)
        } ?: return null
        return moneyInput(value.whole, locale) to (value.remainder != 0L)
    }

    val invalidShareIds: Set<String>
        get() = selected.filter { id ->
            when {
                mode == ExpenseSplit.WEIGHTS -> parseWeight(weights[id] ?: "1") == null
                unit == ExpenseUnit.PERCENT -> {
                    val text = percentages[id]
                    if (text == null) percentReference?.percentage(id) == null
                    else parseMoney(text, locale.copy(grouping = ""))?.let { it <= 10_000 } != true
                }

                else -> parseMoney(exact[id].orEmpty(), locale) == null
            }
        }.toSet()
    val splitProgress: ExpenseSplitProgress?
        get() {
            if (mode != ExpenseSplit.EXACT || selected.isEmpty()) return null
            val total = cents ?: return null
            if (unit == ExpenseUnit.PERCENT) {
                val shares = split ?: percentageValues?.preview(total, selected) ?: return null
                var assigned = 0L
                for (share in shares.values) {
                    if (share > Long.MAX_VALUE - assigned) return null
                    assigned += share
                }
                return ExpenseSplitProgress(total, assigned)
            }
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
            ExpenseSplit.EQUAL, ExpenseSplit.WEIGHTS -> cents?.let {
                ExpenseSplitProgress(
                    it,
                    split?.values?.sum() ?: 0L
                )
            }
        }
    val split: Map<String, Long>?
        get() {
            val total = cents ?: return null
            if (selected.isEmpty() || selected.any { id -> people.none { it.id == id } }) return null
            return when (mode) {
                ExpenseSplit.EQUAL -> splitEqually(total, selected)
                ExpenseSplit.EXACT -> {
                    if (unit == ExpenseUnit.PERCENT) return percentageValues?.split(total, selected)
                    val shares = selected.associateWith { parseMoney(exact[it].orEmpty(), locale) ?: return null }
                    splitExactly(total, shares)
                }

                ExpenseSplit.WEIGHTS -> splitByWeights(
                    total,
                    selected.associateWith { parseWeight(weights[it] ?: "1") ?: return null })
            }
        }

    /** What each person's row shows: their part of an equal split, what was typed for them, or nothing. */
    val shares: Map<String, Long>
        get() {
            val preview = previewShares
            return selected.associateWith { preview[it] ?: 0L }
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

internal fun parseWeight(text: String): Long? =
    text.trim().takeIf { it.isNotEmpty() && it.all { char -> char.digitToIntOrNull() != null } }
        ?.map { it.digitToInt() }?.joinToString("")?.toLongOrNull()?.takeIf { it > 0 }
