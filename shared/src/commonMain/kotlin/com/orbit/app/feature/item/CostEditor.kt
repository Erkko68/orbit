package com.orbit.app.feature.item

import com.orbit.app.core.EURO
import com.orbit.app.core.MoneyLocale
import com.orbit.app.core.euroBeforeAmount
import com.orbit.app.core.parseMoney
import com.orbit.app.domain.model.Cost
import com.orbit.app.domain.model.costErrors

/** Text stays a draft until Save. Only exact cents can become a module. */
data class CostEditor(
    val amount: String = "",
    val paidBy: String,
    val selected: Set<String>,
    val locale: MoneyLocale,
) {
    val cents: Long? get() = parseMoney(amount, locale)?.takeIf { it > 0 }
    val amountInvalid: Boolean get() = amount.isNotEmpty() && cents == null
    val placeholder: String get() = "0${locale.decimal}00"
    val currencyPrefix: String? get() = EURO.takeIf { euroBeforeAmount() }
    val currencySuffix: String? get() = EURO.takeUnless { euroBeforeAmount() }
    val candidate: Cost? get() = cents?.let { Cost(it, paidBy, selected) }
    fun validFor(memberIds: Collection<String>): Boolean =
        candidate?.let { costErrors(it, memberIds).isEmpty() } == true
}
