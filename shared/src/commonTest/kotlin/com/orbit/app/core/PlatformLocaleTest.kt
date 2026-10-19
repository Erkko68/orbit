package com.orbit.app.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformLocaleTest {

    @Test
    fun normalizesSpanishVariants() {
        assertEquals("es", normalizeLanguage("es"))
        assertEquals("es", normalizeLanguage("ES"))
        assertEquals("es", normalizeLanguage("es-ES"))
        assertEquals("es", normalizeLanguage("es-419"))
        assertEquals("es", normalizeLanguage("es_AR"))
    }

    @Test
    fun normalizesCatalanVariants() {
        assertEquals("ca", normalizeLanguage("ca"))
        assertEquals("ca", normalizeLanguage("CA"))
        assertEquals("ca", normalizeLanguage("ca-ES"))
    }

    @Test
    fun normalizesEnglishVariants() {
        assertEquals("en", normalizeLanguage("en"))
        assertEquals("en", normalizeLanguage("EN"))
        assertEquals("en", normalizeLanguage("en-US"))
        assertEquals("en", normalizeLanguage("en-GB"))
    }

    @Test
    fun fallsBackToEnglishForUnsupportedOrMissingLanguages() {
        assertEquals("en", normalizeLanguage("fr"))
        assertEquals("en", normalizeLanguage("de"))
        assertEquals("en", normalizeLanguage("it"))
        assertEquals("en", normalizeLanguage(null))
        assertEquals("en", normalizeLanguage(""))
    }

    @Test
    fun deviceLanguageReturnsSupportedLanguage() {
        val lang = deviceLanguage()
        assertTrue(lang in setOf("es", "ca", "en"), "Expected supported language but got $lang")
    }
}
