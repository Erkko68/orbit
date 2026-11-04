package com.orbit.app.feature.spacesettings.invite

import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.invite_create_failed
import orbit.shared.generated.resources.invite_revoke_failed
import org.jetbrains.compose.resources.StringResource

/** The invitation sheet of the settings of a space. It only opens for an owner or admin. */
data class InviteUiState(
    val canInvite: Boolean = false,
    /** The invitation sheet is open. */
    val open: Boolean = false,
    val spaceName: String = "",
    /** The invitations have not arrived yet, so the card cannot say whether there is one to share. */
    val loading: Boolean = false,
    /** The invitation that works now, if there is one. */
    val invitation: ActiveInvitation? = null,
    /** An invitation is being created or revoked. */
    val busy: Boolean = false,
    /** The last create or revoke failed. Cleared by the next attempt or by closing the dialog. */
    val failure: InviteFailure? = null,
)

data class ActiveInvitation(val code: String, val link: String)

enum class InviteFailure(val message: StringResource) {
    CREATE(Res.string.invite_create_failed),
    REVOKE(Res.string.invite_revoke_failed),
}
