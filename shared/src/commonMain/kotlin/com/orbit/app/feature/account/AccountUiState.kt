package com.orbit.app.feature.account

import com.orbit.app.core.initialsOf
import com.orbit.app.core.designsystem.component.OrbitSelectionOption
import com.orbit.app.domain.model.User
import kotlinx.datetime.TimeZone

data class AccountUiState(
    val user: User? = null,
    val displayName: String = "",
    val language: String = "en",
    val timeZone: String = "",
    val timeZonePickerOpen: Boolean = false,
    val timeZoneQuery: String = "",
    val saving: Boolean = false,
    val signingOut: Boolean = false,
    val saveFailed: Boolean = false,
    val signOutFailed: Boolean = false,
    val saved: Boolean = false,
) {
    val userInitials: String get() = initialsOf(user?.displayName.orEmpty())

    val timeZoneOptions: List<OrbitSelectionOption>
        get() {
            val selectedId = parsedTimeZone?.id
            return (listOfNotNull(selectedId) + availableTimeZoneIds).distinct()
                .filter { it.contains(timeZoneQuery.trim(), ignoreCase = true) }
                .map { OrbitSelectionOption(value = it, selected = it == selectedId) }
        }

    val parsedTimeZone: TimeZone?
        get() = try {
            TimeZone.of(timeZone.trim())
        } catch (_: IllegalArgumentException) {
            null
        }

    val timeZoneInvalid: Boolean get() = timeZone.isNotBlank() && parsedTimeZone == null

    val busy: Boolean get() = saving || signingOut

    val canEdit: Boolean get() = user != null && !busy

    val hasChanges: Boolean
        get() = user != null && (
                displayName.trim() != user.displayName || language != user.language || parsedTimeZone != user.timeZone
                )

    val canSave: Boolean
        get() = canEdit && hasChanges && displayName.isNotBlank() &&
                language in listOf("es", "ca", "en") && parsedTimeZone != null
}

private val availableTimeZoneIds: List<String> = TimeZone.availableZoneIds.sorted()
