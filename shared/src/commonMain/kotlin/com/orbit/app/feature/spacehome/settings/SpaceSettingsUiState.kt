package com.orbit.app.feature.spacehome.settings

import com.orbit.app.domain.model.Role

/**
 * The settings of a space, in a dialog: an owner or admin edits its name and colour, the owner deletes it and
 * everyone else leaves it.
 */
data class SpaceSettingsUiState(
    /** The user's role in the space. Null until the members load, and for someone outside it: nothing shows. */
    val role: Role? = null,
    /** The dialog is showing. */
    val open: Boolean = false,
    /** The name being typed: the current one when the dialog opened. */
    val name: String = "",
    /** The colour being picked, an index into the accent palette: the current one when the dialog opened. */
    val accent: Int = 0,
    val saving: Boolean = false,
    /** The last save failed. Cleared by the next edit or attempt. */
    val failed: Boolean = false,
    /** The dialog asks to confirm leaving the space, or deleting it for the owner. */
    val confirmingExit: Boolean = false,
    /** Leaving or deleting is under way. */
    val exiting: Boolean = false,
    /** The last attempt to leave or delete failed. Cleared by the next attempt or by closing the dialog. */
    val exitFailed: Boolean = false,
    /** The user left or deleted the space, so the screen goes back to the list of spaces. */
    val gone: Boolean = false,
) {
    /** Members see the settings; someone outside the space, or before the members load, does not. */
    val visible: Boolean get() = role != null

    /** An owner or admin edits the name and colour. */
    val canEdit: Boolean get() = role?.managesSpace == true

    /** The owner's way out is deleting the space for everyone; everyone else leaves it. */
    val deletes: Boolean get() = role == Role.OWNER

    val canSave: Boolean get() = name.isNotBlank() && !saving
}
