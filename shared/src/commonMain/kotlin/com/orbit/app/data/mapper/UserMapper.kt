package com.orbit.app.data.mapper

import com.orbit.app.core.deviceLanguage
import com.orbit.app.domain.model.User
import kotlinx.datetime.TimeZone

internal fun mapUser(
    id: String,
    displayName: String?,
    photoUrl: String?,
    language: String?,
    timeZone: String?,
    fallbackDisplayName: String? = null,
    fallbackPhotoUrl: String? = null,
    email: String? = null,
): User {
    val resolvedDisplayName = displayName?.takeIf { it.isNotBlank() }
        ?: fallbackDisplayName?.takeIf { it.isNotBlank() }
        ?: ""
    val resolvedPhotoUrl = photoUrl ?: fallbackPhotoUrl
    val resolvedLanguage = language?.takeIf { it.isNotBlank() } ?: DEFAULT_LANGUAGE
    val resolvedTimeZone = timeZone?.let {
        try {
            TimeZone.of(it)
        } catch (_: Exception) {
            null
        }
    } ?: TimeZone.currentSystemDefault()

    return User(
        id = id,
        displayName = resolvedDisplayName,
        photoUrl = resolvedPhotoUrl,
        language = resolvedLanguage,
        timeZone = resolvedTimeZone,
        email = email,
    )
}

internal fun userProfileDocument(
    displayName: String,
    photoUrl: String? = null,
    language: String = deviceLanguage(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Map<String, Any?> = buildMap {
    put("displayName", displayName)
    if (photoUrl != null) put("photoUrl", photoUrl)
    put("language", language)
    put("timeZone", timeZone.id)
}

internal fun devicePreferencesDocument(
    language: String = deviceLanguage(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Map<String, String> = mapOf(
    "language" to language,
    "timeZone" to timeZone.id,
)

private const val DEFAULT_LANGUAGE = "en"

