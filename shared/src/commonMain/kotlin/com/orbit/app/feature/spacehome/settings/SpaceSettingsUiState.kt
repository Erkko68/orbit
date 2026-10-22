package com.orbit.app.feature.spacehome.settings

/** The settings of a space: its name and colour, edited in a dialog by an owner or admin. */
data class SpaceSettingsUiState(
    /** The user owns or admins the space, so the settings are theirs to change. Nobody else sees them. */
    val canEdit: Boolean = false,
    /** The dialog is showing. */
    val open: Boolean = false,
    /** The name being typed: the current one when the dialog opened. */
    val name: String = "",
    /** The colour being picked, an index into the accent palette: the current one when the dialog opened. */
    val accent: Int = 0,
    val saving: Boolean = false,
    /** The last save failed. Cleared by the next edit or attempt. */
    val failed: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && !saving
}
