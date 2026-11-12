package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.*
import kotlinx.datetime.LocalDate
import kotlin.test.*
import kotlin.time.Instant

class ItemCostExpenseTest {
    private val members = listOf("ana", "bea", "cam")
    private val date = LocalDate(2026, 11, 12)
    private val cost = Cost("ana", mapOf("ana" to 334L, "bea" to 333L, "cam" to 333L))
    private val item = Item(
        "item-1", "Detergent", null, ItemStatus.Open, "ana", Instant.fromEpochSeconds(0), ItemModules(cost = cost),
    )

    @Test
    fun copiesThePlannedSharesAsTheyAreAndLinksTheItem() {
        val custom = cost.copy(split = mapOf("ana" to 700L, "bea" to 300L), customAllocation = mapOf("ana" to false, "bea" to true))
        val expense = assertNotNull(itemCostExpense(item.copy(modules = ItemModules(cost = custom)), members, date))
        assertEquals(NewExpense("Detergent", "ana", custom.split, date, itemId = "item-1", customAllocation = custom.customAllocation), expense)
    }

    @Test
    fun noCostOrAnOccurrenceAlreadyChargedAddsNothing() {
        assertNull(itemCostExpense(item.copy(modules = ItemModules()), members, date))
        assertNull(itemCostExpense(item.copy(costExpenseId = "expense-1"), members, date))
    }

    @Test
    fun aCostThatChargesSomeoneWhoLeftIsRefused() {
        for (left in listOf(cost.copy(paidBy = "dan"), cost.copy(split = mapOf("dan" to 1000L)), cost.copy(split = emptyMap()))) {
            assertTrue(validateItem(item.title, ItemModules(cost = left), members).contains(ItemError.InvalidCost))
            assertFailsWith<IllegalArgumentException> { itemCostExpense(item.copy(modules = ItemModules(cost = left)), members, date) }
        }
    }
}
