package com.orbit.app.domain.usecase

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SplitExpenseTest {
    @Test
    fun leftoversGoInUidOrderAndZeroSharesAreNotStored() {
        assertEquals(
            mapOf("ana" to 334L, "bea" to 333L, "marc" to 333L),
            splitEqually(1000, listOf("marc", "bea", "ana"))
        )
        assertEquals(mapOf("ana" to 1L), splitEqually(1, listOf("marc", "ana")))
        assertEquals(mapOf("ana" to 100L), splitEqually(100, listOf("ana", "ana")))
        assertFailsWith<IllegalArgumentException> { splitEqually(0, listOf("ana")) }
        assertFailsWith<IllegalArgumentException> { splitEqually(100, emptyList()) }
    }

    @Test
    fun equalSplitsConserveCentsAndDoNotDependOnInputOrder() {
        val random = Random(54)
        repeat(500) {
            val ids = (0 until random.nextInt(1, 9)).map { "member-$it" }
            val total = random.nextLong(1, Long.MAX_VALUE)
            val split = splitEqually(total, ids.shuffled(random))
            assertEquals(total, split.values.sum())
            assertEquals(splitEqually(total, ids), split)
            assertTrue(split.values.all { it > 0 })
            assertTrue(split.values.max() - split.values.min() <= 1)
        }
        assertEquals(Long.MAX_VALUE, splitEqually(Long.MAX_VALUE, listOf("a", "b")).values.sum())
    }

    @Test
    fun exactSplitsRejectWrongTotalsNegativesAndOverflow() {
        assertEquals(mapOf("a" to 5L, "b" to 7L), splitExactly(12, mapOf("b" to 7, "a" to 5, "c" to 0)))
        assertNull(splitExactly(12, mapOf("a" to 11)))
        assertNull(splitExactly(12, mapOf("a" to 13)))
        assertNull(splitExactly(12, mapOf("a" to -1, "b" to 13)))
        assertNull(splitExactly(0, emptyMap()))
        assertNull(splitExactly(Long.MAX_VALUE, mapOf("a" to Long.MAX_VALUE, "b" to Long.MAX_VALUE, "c" to 1)))
        assertEquals(mapOf("a" to Long.MAX_VALUE), splitExactly(Long.MAX_VALUE, mapOf("a" to Long.MAX_VALUE)))
    }
}
