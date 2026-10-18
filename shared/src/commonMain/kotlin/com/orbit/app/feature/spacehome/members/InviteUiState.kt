package com.orbit.app.feature.spacehome.members

import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.invite_create_failed
import orbit.shared.generated.resources.invite_revoke_failed
import org.jetbrains.compose.resources.StringResource

/** The invitation card of the members tab. It only shows for an owner or admin of the space. */
data class InviteUiState(
    val canInvite: Boolean = false,
    val spaceName: String = "",
    /** The invitations have not arrived yet, so the card cannot say whether there is one to share. */
    val loading: Boolean = false,
    /** The invitation that works now, if there is one. */
    val invitation: ActiveInvitation? = null,
    /** An invitation is being created or revoked. */
    val busy: Boolean = false,
    /** The last create or revoke failed. Cleared by the next attempt. */
    val failure: InviteFailure? = null,
)

data class ActiveInvitation(val code: String, val link: String)

enum class InviteFailure(val message: StringResource) {
    CREATE(Res.string.invite_create_failed),
    REVOKE(Res.string.invite_revoke_failed),
}
