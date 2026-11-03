package com.orbit.app.core

/**
 * The app's effective ISO language code, following the device or its per-app language setting.
 */
expect fun deviceLanguageCode(): String?

/**
 * Supported app language ("es", "ca", "it", or fallback "en").
 */
fun deviceLanguage(): String = normalizeLanguage(deviceLanguageCode())

internal fun normalizeLanguage(code: String?): String {
    val lang = code?.lowercase()?.take(2)
    return when (lang) {
        "es" -> "es"
        "ca" -> "ca"
        "it" -> "it"
        else -> "en"
    }
}
