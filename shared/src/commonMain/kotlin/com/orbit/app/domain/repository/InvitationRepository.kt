package com.orbit.app.domain.repository

import com.orbit.app.domain.model.Invitation
import kotlinx.coroutines.flow.Flow

/** The invitations of a space. Only its owner and admins can create, list and revoke them. */
interface InvitationRepository {
    /** The invitations of the space, newest first, revoked and expired ones included. */
    fun observeInvitations(spaceId: String): Flow<List<Invitation>>

    /** Makes an invitation to the space, valid for a week, and returns its code. */
    suspend fun createInvitation(spaceId: String): String

    /** Revokes the invitation for good. */
    suspend fun revokeInvitation(code: String)
}
