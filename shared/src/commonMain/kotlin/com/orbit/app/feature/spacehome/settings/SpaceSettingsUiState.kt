package com.orbit.app.feature.spacehome.settings

import com.orbit.app.domain.model.Role

/** What the settings of a space show while they are open. */
enum class SpaceSettingsPage { MENU, IDENTITY, MEMBERS, MEMBER }

/** Every step of the one sheet of the settings, including the ones other view models hold open. */
internal enum class SpaceSettingsStep { MENU, IDENTITY, MEMBERS, MEMBER, INVITE, EXIT }

/**
 * The settings of a space, in a sheet that holds everything the user can do with it: an owner or admin invites and
 * edits its name and colour, anyone sees its members and each one's page, the owner deletes it and everyone else leaves it.
 */
data class SpaceSettingsUiState(
    /** The user's role in the space. Null until the members load, and for someone outside it: nothing shows. */
    val role: Role? = null,
    /** What is showing, null while the settings are closed. */
    val page: SpaceSettingsPage? = null,
    /** The member whose page is showing, on [SpaceSettingsPage.MEMBER]. */
    val memberId: String? = null,
    /** The space's name, for the title of the sheet. */
    val spaceName: String = "",
    /** The name being typed: the current one when the settings opened. */
    val name: String = "",
    /** The colour being picked, an index into the accent palette: the current one when the settings opened. */
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
