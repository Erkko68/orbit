package com.orbit.app.feature.spacesettings

import com.orbit.app.domain.model.Role

/**
 * The settings screen of a space: an owner or admin edits its name and colour, anyone sees its members, the owner
 * deletes it and everyone else leaves it.
 */
data class SpaceSettingsUiState(
    /** The user's role in the space. Null until the members load, and for someone outside it. */
    val role: Role? = null,
    /** The space's name as saved. */
    val spaceName: String = "",
    /** The space's colour as saved, null until it has loaded. */
    val accent: Int? = null,
    /** The name being typed, until it is saved. */
    val nameDraft: String? = null,
    /** The colour being picked, until it is saved. */
    val accentDraft: Int? = null,
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
    /** An owner or admin edits the name and colour. */
    val canEdit: Boolean get() = role?.managesSpace == true

    /** The owner's way out is deleting the space for everyone; everyone else leaves it. */
    val deletes: Boolean get() = role == Role.OWNER

    val name: String get() = nameDraft ?: spaceName

    /** The colour the screen shows: the one being picked, or the saved one. */
    val shownAccent: Int? get() = accentDraft ?: accent

    /** Something differs from what is saved, so Save shows. */
    val changed: Boolean
        get() = (nameDraft != null && nameDraft.trim() != spaceName) || (accentDraft != null && accentDraft != accent)

    val canSave: Boolean get() = canEdit && changed && name.isNotBlank() && !saving
}
