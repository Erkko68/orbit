package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.*
import kotlinx.datetime.LocalDate
import kotlin.test.*
import kotlin.time.Instant

class ItemCostExpenseTest {
    private val members = listOf("ana", "bea", "cam")
    private val date = LocalDate(2026, 11, 12)
    private val item = Item(
        "item-1", "Detergent", null, ItemStatus.Open, "ana", Instant.fromEpochSeconds(0),
        ItemModules(cost = Cost(1000L, "ana", members.toSet()))
    )

    @Test
    fun usesExactCentsAndLinksTheCompletionDayToTheItem() {
        val expense = assertNotNull(itemCostExpense(item, members, date))
        assertEquals(mapOf("ana" to 334L, "bea" to 333L, "cam" to 333L), expense.split)
        assertEquals("item-1", expense.itemId)
        assertEquals(date, expense.date)
        assertEquals("Detergent", expense.title)
        assertEquals(ExpenseKind.EXPENSE, expense.kind)
    }

    @Test
    fun missingCostAndAlreadyChargedOccurrenceDoNotCreateAnotherExpense() {
        assertNull(itemCostExpense(item.copy(modules = ItemModules()), members, date))
        val changed =
            item.copy(costExpenseId = "existing", modules = ItemModules(cost = Cost(1200, "ana", members.toSet())))
        assertNull(itemCostExpense(changed, members, date))
    }

    @Test
    fun theNextOccurrenceUsesTheNewCostAndCanChargeAgain() {
        val next = item.copy(costExpenseId = null, modules = ItemModules(cost = Cost(1200, "ana", members.toSet())))
        assertEquals(1200L, itemCostExpense(next, members, date)?.split?.values?.sum())
    }

    @Test
    fun invalidCostsAreRejectedBeforeWritingAnything() {
        for (cost in listOf(
            Cost(0, "ana", setOf("ana")), Cost(-1, "ana", setOf("ana")),
            Cost(100, "outsider", setOf("ana")), Cost(100, "ana", emptySet()), Cost(100, "ana", setOf("outsider"))
        )) {
            assertTrue(validateItem(item.title, ItemModules(cost = cost), members).isNotEmpty())
            assertFailsWith<IllegalArgumentException> {
                itemCostExpense(
                    item.copy(modules = ItemModules(cost = cost)),
                    members,
                    date
                )
            }
        }
    }

    @Test
    fun smallAndMaximumAmountsStayExact() {
        for (cents in listOf(1L, Long.MAX_VALUE)) {
            val expense = assertNotNull(
                itemCostExpense(
                    item.copy(
                        modules = ItemModules(
                            cost = Cost(
                                cents,
                                "ana",
                                members.toSet()
                            )
                        )
                    ), members, date
                )
            )
            assertEquals(cents, expense.split.values.sum())
            assertTrue(expense.split.values.all { it > 0 })
        }
    }
}
