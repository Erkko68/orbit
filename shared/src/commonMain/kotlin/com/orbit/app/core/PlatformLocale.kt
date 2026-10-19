package com.orbit.app.core

/**
 * The device's current ISO language code (e.g. "en", "es", "ca"), or null if unavailable.
 */
expect fun deviceLanguageCode(): String?

/**
 * Supported app language per ADR 0002 ("es", "ca", or fallback "en").
 */
fun deviceLanguage(): String = normalizeLanguage(deviceLanguageCode())

internal fun normalizeLanguage(code: String?): String {
    val lang = code?.lowercase()?.take(2)
    return when (lang) {
        "es" -> "es"
        "ca" -> "ca"
        else -> "en"
    }
}
