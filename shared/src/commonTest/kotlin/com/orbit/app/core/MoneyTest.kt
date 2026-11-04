package com.orbit.app.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {
    private val comma = MoneyLocale(",", ".")
    private val point = MoneyLocale(".", ",")

    @Test
    fun readsRegionalSeparatorsAndExactCents() {
        assertEquals(123456L, parseMoney("1.234,56", comma))
        assertEquals(123456L, parseMoney("1,234.56", point))
        assertEquals(123450L, parseMoney("1234,5", comma))
        assertEquals(0L, parseMoney("0", comma))
        assertEquals(1L, parseMoney("0,01", comma))
        assertEquals(12L, parseMoney("0,12", MoneyLocale(",", "\u202f")))
        assertEquals(123450L, parseMoney("1\u202f234,50", MoneyLocale(",", "\u202f")))
        assertEquals(1240L, parseMoney("١٢٫٤٠", MoneyLocale("٫", "٬")))
    }

    @Test
    fun rejectsRoundingMalformedGroupingAndOverflow() {
        for (text in listOf("", "-1", "+1", "1e2", "NaN", "€12", "12,345", "12.34,50", "1..234", "1,2,3")) {
            assertNull(parseMoney(text, comma), text)
        }
        assertEquals(Long.MAX_VALUE, parseMoney("92233720368547758,07", comma))
        assertNull(parseMoney("92233720368547758,08", comma))
    }

    @Test
    fun editingValuesRoundTripWithoutFloatingPoint() {
        for (locale in listOf(comma, point)) {
            for (cents in listOf(0L, 1L, 10L, 199L, 9007199254740993L, Long.MAX_VALUE)) {
                assertEquals(cents, parseMoney(moneyInput(cents, locale), locale))
            }
        }
    }
}
