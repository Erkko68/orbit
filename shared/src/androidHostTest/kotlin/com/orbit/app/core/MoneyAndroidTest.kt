package com.orbit.app.core

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoneyAndroidTest {
    @Test
    fun usesNativeRegionalCurrencyFormattingWithoutLosingLargeCents() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALY)
            assertEquals(MoneyLocale(",", "."), deviceMoneyLocale())
            assertTrue(formatEuro(1240).contains("12,40"))
            assertTrue(formatEuro(1240).contains("€"))
            assertTrue(formatEuro(9007199254740993L).contains("90.071.992.547.409,93"))
            Locale.setDefault(Locale.US)
            assertEquals(MoneyLocale(".", ","), deviceMoneyLocale())
            assertTrue(formatEuro(1240).contains("12.40"))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
