package com.orbit.app.core

/**
 * The app's effective ISO language code, following the device or its per-app language setting.
 */
expect fun deviceLanguageCode(): String?

private val supportedLanguages = setOf("es", "ca", "it")

/**
 * Supported app language ("es", "ca", "it", or fallback "en").
 */
fun deviceLanguage(): String = normalizeLanguage(deviceLanguageCode())

internal fun normalizeLanguage(code: String?): String =
    code?.take(2)?.lowercase()?.takeIf { it in supportedLanguages } ?: "en"
