package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.model.costErrors
import com.orbit.app.domain.model.validateExpense
import kotlinx.datetime.LocalDate

/** A past expense is never rewritten by a change to the item's cost or by reopening it. */
fun itemCostExpense(item: Item, memberIds: Collection<String>, date: LocalDate): NewExpense? {
    if (item.costExpenseId != null) return null
    val cost = item.modules.cost ?: return null
    require(costErrors(cost, memberIds).isEmpty()) { "Correct the item's cost before completing it" }
    val split = splitEqually(cost.cents, cost.memberIds)
    require(validateExpense(item.title, cost.paidBy, split, cost.cents, memberIds).isEmpty())
    return NewExpense(item.title, cost.paidBy, split, date, itemId = item.id)
}
