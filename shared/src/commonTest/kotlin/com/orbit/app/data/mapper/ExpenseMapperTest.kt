package com.orbit.app.data.mapper

import com.orbit.app.domain.model.ExpenseKind
import com.orbit.app.domain.model.NewExpense
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class ExpenseMapperTest {
    private val createdAt = Instant.fromEpochSeconds(1_760_000_000)

    private val stored = StoredExpense(
        title = "Groceries",
        paidBy = "ana",
        split = mapOf("ana" to 620, "ben" to 620),
        date = "2026-10-09",
        createdBy = "ana",
        createdAt = createdAt,
        kind = "expense",
    )

    @Test
    fun aNewExpenseIsMadeByItsCreator() {
        val expense = NewExpense(
            title = "Groceries",
            paidBy = "ben",
            split = mapOf("ana" to 620, "ben" to 620),
            date = LocalDate(2026, 10, 9),
            itemId = "item-1",
        )

        val document = expenseDocument("space-1", expense, "ana")

        assertEquals("space-1", document.spaceId)
        assertEquals("Groceries", document.title)
        assertEquals("ben", document.paidBy)
        assertEquals(mapOf("ana" to 620L, "ben" to 620L), document.split)
        assertEquals("2026-10-09", document.date)
        assertEquals("ana", document.createdBy)
        assertEquals(Timestamp.ServerTimestamp, document.createdAt)
        assertEquals("expense", document.kind)
        assertEquals("item-1", document.itemId)
    }

    @Test
    fun aSettlementKeepsItsKind() {
        val expense = NewExpense(
            title = "Settle up",
            paidBy = "ben",
            split = mapOf("ana" to 620),
            date = LocalDate(2026, 10, 9),
            kind = ExpenseKind.SETTLEMENT,
        )

        val document = expenseDocument("space-1", expense, "ben")

        assertNull(document.itemId)
        assertEquals("settlement", document.kind)
        assertEquals(ExpenseKind.SETTLEMENT, mapExpense("e1", stored.copy(kind = document.kind))?.kind)
    }

    @Test
    fun storedFieldsReadBack() {
        val expense = assertNotNull(mapExpense("e1", stored.copy(itemId = "item-1")))

        assertEquals("e1", expense.id)
        assertEquals("Groceries", expense.title)
        assertEquals("ana", expense.paidBy)
        assertEquals(mapOf("ana" to 620L, "ben" to 620L), expense.split)
        assertEquals(1240, expense.total)
        assertEquals(LocalDate(2026, 10, 9), expense.date)
        assertEquals("ana", expense.createdBy)
        assertEquals(createdAt, expense.createdAt)
        assertEquals(ExpenseKind.EXPENSE, expense.kind)
        assertEquals("item-1", expense.itemId)
    }

    @Test
    fun aMissingRequiredFieldSkipsTheExpense() {
        listOf(
            stored.copy(title = null),
            stored.copy(paidBy = null),
            stored.copy(split = null),
            stored.copy(date = null),
            stored.copy(createdBy = null),
            stored.copy(createdAt = null),
        ).forEach { assertNull(mapExpense("e1", it)) }
    }

    @Test
    fun anEmptySplitOrAMalformedDateSkipsTheExpense() {
        assertNull(mapExpense("e1", stored.copy(split = emptyMap())))
        assertNull(mapExpense("e1", stored.copy(date = "tomorrow")))
    }

    @Test
    fun anUnknownKindStillCountsAsAnExpense() {
        assertEquals(ExpenseKind.EXPENSE, mapExpense("e1", stored.copy(kind = "refund"))?.kind)
    }
}
