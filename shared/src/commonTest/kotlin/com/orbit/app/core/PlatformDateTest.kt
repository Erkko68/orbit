package com.orbit.app.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformDateTest {
    private val date = LocalDate(2026, 1, 12)

    @Test
    fun usesTheRegionsDateOrderEvenWithinOneLanguage() {
        val us = formatDate(date, "en-US")
        val gb = formatDate(date, "en-GB")
        assertTrue(us.indexOf("Jan") < us.indexOf("12"), us)
        assertTrue(gb.indexOf("12") < gb.indexOf("Jan"), gb)
    }

    @Test
    fun usesTheSupportedLanguagesMonthNames() {
        for ((tag, month) in listOf("es-ES" to "ene", "ca-ES" to "gen", "it-IT" to "gen")) {
            val label = formatDate(date, tag)
            assertTrue(label.contains(month), "$tag: $label")
            assertTrue(label.contains("12") && label.contains("2026"), "$tag: $label")
            assertTrue(label.indexOf("12") < label.indexOf(month), "$tag: $label")
        }
    }

    @Test
    fun keepsTheWallClockDateIncludingLeapDay() {
        for (tag in listOf("en-US", "es-ES", "ca-ES", "it-IT")) {
            val label = formatDate(LocalDate(2024, 2, 29), tag)
            assertTrue(label.contains("29") && label.contains("2024"), "$tag: $label")
        }
    }
}
