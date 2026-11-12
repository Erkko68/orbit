package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.model.costErrors
import kotlinx.datetime.LocalDate

/**
 * The expense completing [item] adds on [date]: its planned cost as it is, once per occurrence (ADR 0014). Null with no
 * cost, or when this occurrence already added one. A cost that no longer holds (a member left) throws instead of
 * charging the wrong people.
 */
fun itemCostExpense(item: Item, memberIds: Collection<String>, date: LocalDate): NewExpense? {
    if (item.costExpenseId != null) return null
    val cost = item.modules.cost ?: return null
    require(costErrors(item.title, cost, memberIds).isEmpty()) { "Correct the item's cost before completing it" }
    return NewExpense(
        item.title, cost.paidBy, cost.split, date, itemId = item.id, customAllocation = cost.customAllocation,
    )
}
