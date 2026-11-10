package com.orbit.app.core

import com.orbit.app.domain.usecase.ShareDivision
import com.orbit.app.domain.usecase.divideShare
import java.math.BigInteger
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShareDivisionTest {
    @Test
    fun fullLongRangeMatchesAnIndependentBigIntegerCalculation() {
        val random = Random(55)
        val cases = listOf(
            Triple(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE),
            Triple(Long.MAX_VALUE, 10_000L, 3L),
            Triple(0L, Long.MAX_VALUE, 1L),
            Triple(1L, 0L, 1L),
        ) + List(1000) {
            Triple(
                random.nextLong(0, Long.MAX_VALUE),
                random.nextLong(0, Long.MAX_VALUE),
                random.nextLong(1, Long.MAX_VALUE)
            )
        }
        for ((value, numerator, denominator) in cases) {
            val reference = BigInteger.valueOf(value).multiply(BigInteger.valueOf(numerator))
                .divideAndRemainder(BigInteger.valueOf(denominator))
            val expected = if (reference[0] > BigInteger.valueOf(Long.MAX_VALUE)) null else ShareDivision(
                reference[0].toLong(),
                reference[1].toLong()
            )
            assertEquals(expected, divideShare(value, numerator, denominator), "$value * $numerator / $denominator")
        }
        assertNull(divideShare(-1, 1, 1))
        assertNull(divideShare(1, -1, 1))
        assertNull(divideShare(1, 1, 0))
    }
}
