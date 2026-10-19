package com.orbit.app.data.mapper

import com.orbit.app.core.deviceLanguage
import com.orbit.app.domain.model.User
import kotlinx.datetime.TimeZone

/** The user from their profile, with the sign-in account's name and photo where the profile has none. */
internal fun mapUser(
    id: String,
    displayName: String?,
    photoUrl: String?,
    fallbackDisplayName: String? = null,
    fallbackPhotoUrl: String? = null,
    email: String? = null,
) = User(
    id = id,
    displayName = displayName?.ifBlank { null } ?: fallbackDisplayName?.ifBlank { null }.orEmpty(),
    photoUrl = photoUrl ?: fallbackPhotoUrl,
    email = email,
)

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

