package com.orbit.app.feature.spacesettings

import com.orbit.app.domain.model.Role

/**
 * The settings screen of a space: an owner or admin renames it in a sheet and invites people, anyone sees its members,
 * the owner deletes it and everyone else leaves it.
 */
data class SpaceSettingsUiState(
    /** The user's role in the space. Null until the members load, and for someone outside it. */
    val role: Role? = null,
    /** The space's name as saved. */
    val spaceName: String = "",
    /** The rename sheet is open. */
    val renaming: Boolean = false,
    /** The name being typed in it, until it is saved. */
    val nameDraft: String? = null,
    val saving: Boolean = false,
    /** The last save failed. Cleared by the next edit or attempt. */
    val failed: Boolean = false,
    /** The dialog asks to confirm leaving the space, or deleting it for the owner. */
    val confirmingExit: Boolean = false,
    /** Leaving or deleting is under way. */
    val exiting: Boolean = false,
    /** The last attempt to leave or delete failed. Cleared by the next attempt or by closing the dialog. */
    val exitFailed: Boolean = false,
    /** The user left or deleted the space, so the app goes back to the list of spaces. */
    val gone: Boolean = false,
) {
    /** An owner or admin edits the name. */
    val canEdit: Boolean get() = role?.managesSpace == true

    /** The owner's way out is deleting the space for everyone; everyone else leaves it. */
    val deletes: Boolean get() = role == Role.OWNER

    val name: String get() = nameDraft ?: spaceName

    /** Something differs from what is saved, so Save can go. */
    val changed: Boolean
        get() = nameDraft != null && nameDraft.trim() != spaceName

    val canSave: Boolean get() = canEdit && changed && name.isNotBlank() && !saving
}
